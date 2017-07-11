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
package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.ai.model.L2SummonAI;
import net.sf.l2j.gameserver.model.L2CharPosition;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SiegeSummonInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SummonInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.ActionFailed;
import net.sf.l2j.gameserver.network.client.game_to_client.NpcSay;
import net.sf.l2j.gameserver.util.Util;

public final class RequestActionUse extends L2GameClientPacket
{
	private static final int[] PASSIVE_SUMMONS =
	{
		12564,
		12621,
		14702,
		14703,
		14704,
		14705,
		14706,
		14707,
		14708,
		14709,
		14710,
		14711,
		14712,
		14713,
		14714,
		14715,
		14716,
		14717,
		14718,
		14719,
		14720,
		14721,
		14722,
		14723,
		14724,
		14725,
		14726,
		14727,
		14728,
		14729,
		14730,
		14731,
		14732,
		14733,
		14734,
		14735,
		14736
	};
	
	private static final int SIN_EATER_ID = 12564;
	private static final String[] SIN_EATER_ACTIONS_STRINGS =
	{
		"special skill? Abuses in this kind of place, can turn blood Knots...!",
		"Hey! Brother! What do you anticipate to me?",
		"shouts ha! Flap! Flap! Response?",
		", has not hit...!"
	};
	
	private int _actionId;
	private boolean _ctrlPressed;
	private boolean _shiftPressed;
	
	@Override
	protected void readImpl()
	{
		_actionId = readD();
		_ctrlPressed = (readD() == 1);
		_shiftPressed = (readC() == 1);
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;
		
		// Dont do anything if player is dead, or use fakedeath using another action than sit.
		if ((activeChar.isFakeDeath() && _actionId != 0) || activeChar.isDead() || activeChar.isOutOfControl())
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final L2Summon pet = activeChar.getPet();
		final L2Object target = activeChar.getTarget();
		
		switch (_actionId)
		{
			case 0:
				activeChar.tryToSitOrStand(target, activeChar.isSitting());
				break;
			
			case 1:
				if (activeChar.isRunning())
					activeChar.setWalking();
				else
					activeChar.setRunning();
				break;
			
			case 10: // Private Store - Sell
				activeChar.tryOpenPrivateSellStore(false);
				break;
			
			case 28: // Private Store - Buy
				activeChar.tryOpenPrivateBuyStore();
				break;
			
			case 15:
			case 21: // Change Movement Mode (pet follow/stop)
				if (pet == null)
					return;
				
				// You can't order anymore your pet to stop if distance is superior to 2000.
				if (pet.getFollowStatus() && Util.calculateDistance(activeChar, pet, true) > 2000)
					return;
				
				if (pet.isOutOfControl())
				{
					activeChar.sendPacket(SystemMessageId.PET_REFUSING_ORDER);
					return;
				}
				
				((L2SummonAI) pet.getAI()).notifyFollowStatusChange();
				break;
			
			case 16:
			case 22: // Attack (pet attack)
				if (!(target instanceof L2Character) || pet == null || pet == target || activeChar == target)
					return;
				
				// Sin eater, Big Boom, Wyvern can't attack with attack button.
				if (Util.contains(PASSIVE_SUMMONS, pet.getNpcId()))
					return;
				
				if (pet.isOutOfControl())
				{
					activeChar.sendPacket(SystemMessageId.PET_REFUSING_ORDER);
					return;
				}
				
				if (pet.isAttackingDisabled())
				{
					if (pet.getAttackEndTime() <= System.currentTimeMillis())
						return;
					
					pet.getAI().setIntention(CtrlIntention.ATTACK, target);
				}
				
				if (pet instanceof L2PetInstance && (pet.getLevel() - activeChar.getLevel() > 20))
				{
					activeChar.sendPacket(SystemMessageId.PET_TOO_HIGH_TO_CONTROL);
					return;
				}
				
				if (activeChar.isInOlympiadMode() && !activeChar.isOlympiadStart())
					return;
				
				pet.setTarget(target);
				
				// Summons can attack NPCs even when the owner cannot.
				if (!target.isAutoAttackable(activeChar) && !_ctrlPressed && (!(target instanceof L2NpcInstance)))
				{
					pet.setFollowStatus(false);
					pet.getAI().setIntention(CtrlIntention.FOLLOW, target);
					activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
					return;
				}
				
				if (target instanceof L2DoorInstance)
				{
					if (((L2DoorInstance) target).isAttackable(activeChar) && pet.getNpcId() != L2SiegeSummonInstance.SWOOP_CANNON_ID)
						pet.getAI().setIntention(CtrlIntention.ATTACK, target);
				}
				// siege golem AI doesn't support attacking other than doors at the moment
				else if (pet.getNpcId() != L2SiegeSummonInstance.SIEGE_GOLEM_ID)
				{
					if (L2Character.isInsidePeaceZone(pet, target))
					{
						pet.setFollowStatus(false);
						pet.getAI().setIntention(CtrlIntention.FOLLOW, target);
					}
					else
						pet.getAI().setIntention(CtrlIntention.ATTACK, target);
				}
				break;
			
			case 17:
			case 23: // Stop (pet - cancel action)
				if (pet == null)
					return;
				
				if (pet.isOutOfControl())
				{
					activeChar.sendPacket(SystemMessageId.PET_REFUSING_ORDER);
					return;
				}
				
				pet.getAI().setIntention(CtrlIntention.ACTIVE, null);
				break;
			
			case 19: // Returns pet to control item
				if (pet == null || !(pet instanceof L2PetInstance))
					return;
				
				if (pet.isDead())
					activeChar.sendPacket(SystemMessageId.DEAD_PET_CANNOT_BE_RETURNED);
				else if (pet.isOutOfControl())
					activeChar.sendPacket(SystemMessageId.PET_REFUSING_ORDER);
				else if (pet.isAttackingNow() || pet.isInCombat())
					activeChar.sendPacket(SystemMessageId.PET_CANNOT_SENT_BACK_DURING_BATTLE);
				else if (((L2PetInstance) pet).isHungry())
					activeChar.sendPacket(SystemMessageId.YOU_CANNOT_RESTORE_HUNGRY_PETS);
				else
					pet.unSummon(activeChar);
				break;
			
			case 38: // pet mount/dismount
				activeChar.mountPlayer(pet);
				break;
			
			case 32: // Wild Hog Cannon - Mode Change
				// useSkill(4230);
				break;
			
			case 36: // Soulless - Toxic Smoke
				useSkill(4259, target);
				break;
			
			case 37: // Dwarven Manufacture
				activeChar.tryOpenWorkshop(true);
				break;
			
			case 39: // Soulless - Parasite Burst
				useSkill(4138, target);
				break;
			
			case 41: // Wild Hog Cannon - Attack
				if (!(target instanceof L2DoorInstance))
				{
					activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
					return;
				}
				
				useSkill(4230, target);
				break;
			
			case 42: // Kai the Cat - Self Damage Shield
				useSkill(4378, activeChar);
				break;
			
			case 43: // Unicorn Merrow - Hydro Screw
				useSkill(4137, target);
				break;
			
			case 44: // Big Boom - Boom Attack
				useSkill(4139, target);
				break;
			
			case 45: // Unicorn Boxer - Master Recharge
				useSkill(4025, activeChar);
				break;
			
			case 46: // Mew the Cat - Mega Storm Strike
				useSkill(4261, target);
				break;
			
			case 47: // Silhouette - Steal Blood
				useSkill(4260, target);
				break;
			
			case 48: // Mechanic Golem - Mech. Cannon
				useSkill(4068, target);
				break;
			
			case 51: // General Manufacture
				activeChar.tryOpenWorkshop(false);
				break;
			
			case 52: // Unsummon a servitor
				if (pet == null || !(pet instanceof L2SummonInstance))
					return;
				
				if (pet.isDead())
					activeChar.sendPacket(SystemMessageId.DEAD_PET_CANNOT_BE_RETURNED);
				else if (pet.isOutOfControl())
					activeChar.sendPacket(SystemMessageId.PET_REFUSING_ORDER);
				else if (pet.isAttackingNow() || pet.isInCombat())
					activeChar.sendPacket(SystemMessageId.PET_CANNOT_SENT_BACK_DURING_BATTLE);
				else
					pet.unSummon(activeChar);
				break;
			
			case 53: // move to target
			case 54: // move to target hatch/strider
				if (target == null || pet == null || pet == target)
					return;
				
				if (pet.isOutOfControl())
				{
					activeChar.sendPacket(SystemMessageId.PET_REFUSING_ORDER);
					return;
				}
				
				pet.setFollowStatus(false);
				pet.getAI().setIntention(CtrlIntention.MOVE_TO, new L2CharPosition(target.getX(), target.getY(), target.getZ(), 0));
				break;
			
			case 61: // Private Store Package Sell
				activeChar.tryOpenPrivateSellStore(true);
				break;
			
			case 1000: // Siege Golem - Siege Hammer
				if (!(target instanceof L2DoorInstance))
				{
					activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
					return;
				}
				
				useSkill(4079, target);
				break;
			
			case 1001: // Sin Eater - Ultimate Bombastic Buster
				if (useSkill(4139, pet) && pet.getNpcId() == SIN_EATER_ID && Rnd.get(100) < 10)
					pet.broadcastPacket(new NpcSay(pet.getObjectId(), Say2.ALL, pet.getNpcId(), SIN_EATER_ACTIONS_STRINGS[Rnd.get(SIN_EATER_ACTIONS_STRINGS.length)]));
				break;
			
			case 1003: // Wind Hatchling/Strider - Wild Stun
				useSkill(4710, target);
				break;
			
			case 1004: // Wind Hatchling/Strider - Wild Defense
				useSkill(4711, activeChar);
				break;
			
			case 1005: // Star Hatchling/Strider - Bright Burst
				useSkill(4712, target);
				break;
			
			case 1006: // Star Hatchling/Strider - Bright Heal
				useSkill(4713, activeChar);
				break;
			
			case 1007: // Cat Queen - Blessing of Queen
				useSkill(4699, activeChar);
				break;
			
			case 1008: // Cat Queen - Gift of Queen
				useSkill(4700, activeChar);
				break;
			
			case 1009: // Cat Queen - Cure of Queen
				useSkill(4701, target);
				break;
			
			case 1010: // Unicorn Seraphim - Blessing of Seraphim
				useSkill(4702, activeChar);
				break;
			
			case 1011: // Unicorn Seraphim - Gift of Seraphim
				useSkill(4703, activeChar);
				break;
			
			case 1012: // Unicorn Seraphim - Cure of Seraphim
				useSkill(4704, target);
				break;
			
			case 1013: // Nightshade - Curse of Shade
				useSkill(4705, target);
				break;
			
			case 1014: // Nightshade - Mass Curse of Shade
				useSkill(4706, activeChar);
				break;
			
			case 1015: // Nightshade - Shade Sacrifice
				useSkill(4707, target);
				break;
			
			case 1016: // Cursed Man - Cursed Blow
				useSkill(4709, target);
				break;
			
			case 1017: // Cursed Man - Cursed Strike/Stun
				useSkill(4708, target);
				break;
			
			case 1031: // Feline King - Slash
				useSkill(5135, target);
				break;
			
			case 1032: // Feline King - Spinning Slash
				useSkill(5136, target);
				break;
			
			case 1033: // Feline King - Grip of the Cat
				useSkill(5137, target);
				break;
			
			case 1034: // Magnus the Unicorn - Whiplash
				useSkill(5138, target);
				break;
			
			case 1035: // Magnus the Unicorn - Tridal Wave
				useSkill(5139, target);
				break;
			
			case 1036: // Spectral Lord - Corpse Kaboom
				useSkill(5142, target);
				break;
			
			case 1037: // Spectral Lord - Dicing Death
				useSkill(5141, target);
				break;
			
			case 1038: // Spectral Lord - Force Curse
				useSkill(5140, target);
				break;
			
			case 1039: // Swoop Cannon - Cannon Fodder
				if (target instanceof L2DoorInstance)
				{
					activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
					return;
				}
				
				useSkill(5110, target);
				break;
			
			case 1040: // Swoop Cannon - Big Bang
				if (target instanceof L2DoorInstance)
				{
					activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
					return;
				}
				
				useSkill(5111, target);
				break;
			
			default:
				_log.warning(activeChar.getName() + ": unhandled action type " + _actionId);
		}
	}
	
	/**
	 * Cast a skill for active pet/servitor.
	 * @param skillId The id of the skill to launch.
	 * @param target The target is specified as a parameter but can be overwrited or ignored depending on skill type.
	 * @return true if you can use the skill, false otherwise.
	 */
	private boolean useSkill(int skillId, L2Object target)
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		
		// No owner, or owner in shop mode.
		if (activeChar == null || activeChar.isInStoreMode())
			return false;
		
		final L2Summon activeSummon = activeChar.getPet();
		if (activeSummon == null)
			return false;
		
		// Pet which is 20 levels higher than owner.
		if (activeSummon instanceof L2PetInstance && activeSummon.getLevel() - activeChar.getLevel() > 20)
		{
			activeChar.sendPacket(SystemMessageId.PET_TOO_HIGH_TO_CONTROL);
			return false;
		}
		
		// Out of control pet.
		if (activeSummon.isOutOfControl())
		{
			activeChar.sendPacket(SystemMessageId.PET_REFUSING_ORDER);
			return false;
		}
		
		// Verify if the launched skill is mastered by the summon.
		final L2Skill skill = activeSummon.getTemplate().getSkills().get(skillId);
		if (skill == null)
			return false;
		
		// Can't launch offensive skills on owner.
		if (skill.isOffensive() && activeChar == target)
			return false;
		
		activeSummon.setTarget(target);
		return activeSummon.useMagic(skill, _ctrlPressed, _shiftPressed);
	}
}