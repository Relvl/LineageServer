/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.skills.l2skills;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.ShotType;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2CubicInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.templates.StatsSet;

public class L2SkillDrain extends L2Skill
{
	private final float _absorbPart;
	private final int _absorbAbs;
	
	public L2SkillDrain(StatsSet set)
	{
		super(set);
		
		_absorbPart = set.getFloat("absorbPart", 0.f);
		_absorbAbs = set.getInteger("absorbAbs", 0);
	}
	
	@Override
	public void useSkill(L2Character activeChar, L2Object[] targets)
	{
		if (activeChar.isAlikeDead())
			return;
		
		final boolean sps = activeChar.isChargedShot(ShotType.SPIRITSHOT);
		final boolean bsps = activeChar.isChargedShot(ShotType.BLESSED_SPIRITSHOT);
		final boolean isPlayable = activeChar instanceof L2Playable;
		
		for (L2Object obj : targets)
		{
			if (!(obj instanceof L2Character))
				continue;
			
			final L2Character target = ((L2Character) obj);
			if (target.isAlikeDead() && getTargetType() != SkillTargetType.TARGET_CORPSE_MOB)
				continue;
			
			if (activeChar != target && target.isInvul())
				continue; // No effect on invulnerable chars unless they cast it themselves.
				
			final boolean mcrit = Formulas.calcMCrit(activeChar.getMCriticalHit(target, this));
			final byte shld = Formulas.calcShldUse(activeChar, target, this);
			final int damage = (int) Formulas.calcMagicDam(activeChar, target, this, shld, sps, bsps, mcrit);
			
			if (damage > 0)
			{
				int _drain = 0;
				int _cp = (int) target.getCurrentCp();
				int _hp = (int) target.getCurrentHp();
				
				// Drain system is different for L2Playable and monsters.
				// When playables attack CP of enemies, monsters don't bother about it.
				if (isPlayable && _cp > 0)
				{
					if (damage < _cp)
						_drain = 0;
					else
						_drain = damage - _cp;
				}
				else if (damage > _hp)
					_drain = _hp;
				else
					_drain = damage;
				
				final double hpAdd = _absorbAbs + _absorbPart * _drain;
				if (hpAdd > 0)
				{
					final double hp = ((activeChar.getCurrentHp() + hpAdd) > activeChar.getMaxHp() ? activeChar.getMaxHp() : (activeChar.getCurrentHp() + hpAdd));
					
					activeChar.setCurrentHp(hp);
					
					StatusUpdate suhp = new StatusUpdate(activeChar);
					suhp.addAttribute(StatusUpdate.CUR_HP, (int) hp);
					activeChar.sendPacket(suhp);
				}
				
				// That section is launched for drain skills made on ALIVE targets.
				if (!target.isDead() || getTargetType() != SkillTargetType.TARGET_CORPSE_MOB)
				{
					// Manage cast break of the target (calculating rate, sending message...)
					Formulas.calcCastBreak(target, damage);
					
					activeChar.sendDamageMessage(target, damage, mcrit, false, false);
					
					if (hasEffects() && getTargetType() != SkillTargetType.TARGET_CORPSE_MOB)
					{
						// ignoring vengance-like reflections
						if ((Formulas.calcSkillReflect(target, this) & Formulas.SKILL_REFLECT_SUCCEED) > 0)
						{
							activeChar.stopSkillEffects(getId());
							getEffects(target, activeChar);
							activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT).addSkillName(getId()));
						}
						else
						{
							// activate attacked effects, if any
							target.stopSkillEffects(getId());
							if (Formulas.calcSkillSuccess(activeChar, target, this, shld, bsps))
								getEffects(activeChar, target);
							else
								activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_RESISTED_YOUR_S2).addCharName(target).addSkillName(getId()));
						}
					}
					target.reduceCurrentHp(damage, activeChar, this);
				}
			}
		}
		
		if (hasSelfEffects())
		{
			final L2Effect effect = activeChar.getFirstEffect(getId());
			if (effect != null && effect.isSelfEffect())
				effect.exit();
			
			getEffectsSelf(activeChar);
		}
		
		activeChar.setChargedShot(bsps ? ShotType.BLESSED_SPIRITSHOT : ShotType.SPIRITSHOT, isStaticReuse());
	}
	
	public void useCubicSkill(L2CubicInstance activeCubic, L2Object[] targets)
	{
		if (Config.DEBUG)
			_log.info("L2SkillDrain: useCubicSkill()");
		
		for (L2Object obj : targets)
		{
			if (!(obj instanceof L2Character))
				continue;
			
			final L2Character target = ((L2Character) obj);
			if (target.isAlikeDead() && getTargetType() != SkillTargetType.TARGET_CORPSE_MOB)
				continue;
			
			final boolean mcrit = Formulas.calcMCrit(activeCubic.getMCriticalHit(target, this));
			final byte shld = Formulas.calcShldUse(activeCubic.getOwner(), target, this);
			final int damage = (int) Formulas.calcMagicDam(activeCubic, target, this, mcrit, shld);
			
			// Check to see if we should damage the target
			if (damage > 0)
			{
				final L2PcInstance owner = activeCubic.getOwner();
				final double hpAdd = _absorbAbs + _absorbPart * damage;
				if (hpAdd > 0)
				{
					final double hp = ((owner.getCurrentHp() + hpAdd) > owner.getMaxHp() ? owner.getMaxHp() : (owner.getCurrentHp() + hpAdd));
					
					owner.setCurrentHp(hp);
					
					StatusUpdate suhp = new StatusUpdate(owner);
					suhp.addAttribute(StatusUpdate.CUR_HP, (int) hp);
					owner.sendPacket(suhp);
				}
				
				// That section is launched for drain skills made on ALIVE targets.
				if (!target.isDead() || getTargetType() != SkillTargetType.TARGET_CORPSE_MOB)
				{
					target.reduceCurrentHp(damage, activeCubic.getOwner(), this);
					
					// Manage cast break of the target (calculating rate, sending message...)
					Formulas.calcCastBreak(target, damage);
					
					owner.sendDamageMessage(target, damage, mcrit, false, false);
				}
			}
		}
	}
}