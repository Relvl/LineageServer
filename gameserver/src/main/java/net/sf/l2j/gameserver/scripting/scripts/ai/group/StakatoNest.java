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
package net.sf.l2j.gameserver.scripting.scripts.ai.group;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.scripting.scripts.ai.AbstractNpcAI;

/**
 * This AI handles following behaviors :
 * <ul>
 * <li>Cannibalistic Stakato Leader : try to eat a Follower, if any around, at low HPs.</li>
 * <li>Female Spiked Stakato : when Male dies, summons 3 Spiked Stakato Guards.</li>
 * <li>Male Spiked Stakato : when Female dies, transforms in stronger form.</li>
 * <li>Spiked Stakato Baby : when Spiked Stakato Nurse dies, her baby summons 3 Spiked Stakato Captains.</li>
 * <li>Spiked Stakato Nurse : when Spiked Stakato Baby dies, transforms in stronger form.</li>
 * </ul>
 * As NCSoft implemented it on postIL, but skills exist since IL, I decided to implemented that script to "honor" the idea (which is kinda funny).
 */
public class StakatoNest extends AbstractNpcAI
{
	private static final int SpikedStakatoGuard = 22107;
	private static final int FemaleSpikedStakato = 22108;
	private static final int MaleSpikedStakato1 = 22109;
	private static final int MaleSpikedStakato2 = 22110;
	
	private static final int StakatoFollower = 22112;
	private static final int CannibalisticStakatoLeader1 = 22113;
	private static final int CannibalisticStakatoLeader2 = 22114;
	
	private static final int SpikedStakatoCaptain = 22117;
	private static final int SpikedStakatoNurse1 = 22118;
	private static final int SpikedStakatoNurse2 = 22119;
	private static final int SpikedStakatoBaby = 22120;
	
	public StakatoNest()
	{
		super("ai/group");
		
		addAttackId(CannibalisticStakatoLeader1, CannibalisticStakatoLeader2);
		addKillId(MaleSpikedStakato1, FemaleSpikedStakato, SpikedStakatoNurse1, SpikedStakatoBaby);
	}
	
	@Override
	public String onAttack(L2Npc npc, L2PcInstance player, int damage, boolean isPet)
	{
		if (npc.getCurrentHp() / npc.getMaxHp() < 0.3 && Rnd.get(100) < 5)
		{
			for (L2MonsterInstance follower : npc.getKnownList().getKnownTypeInRadius(L2MonsterInstance.class, 400))
			{
				if (follower.getNpcId() == StakatoFollower && !follower.isDead())
				{
					npc.setIsCastingNow(true);
					npc.broadcastPacket(new MagicSkillUse(npc, follower, (npc.getNpcId() == CannibalisticStakatoLeader2) ? 4072 : 4073, 1, 3000, 0));
					ThreadPoolManager.getInstance().scheduleGeneral(new EatTask(npc, follower), 3000L);
					break;
				}
			}
		}
		return super.onAttack(npc, player, damage, isPet);
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet)
	{
		switch (npc.getNpcId())
		{
			case MaleSpikedStakato1:
				for (L2MonsterInstance angryFemale : npc.getKnownList().getKnownTypeInRadius(L2MonsterInstance.class, 400))
				{
					if (angryFemale.getNpcId() == FemaleSpikedStakato && !angryFemale.isDead())
					{
						for (int i = 0; i < 3; i++)
						{
							final L2Npc guard = addSpawn(SpikedStakatoGuard, angryFemale, true, 0, false);
							attack(((L2Attackable) guard), killer);
						}
					}
				}
				break;
			
			case FemaleSpikedStakato:
				for (L2MonsterInstance morphingMale : npc.getKnownList().getKnownTypeInRadius(L2MonsterInstance.class, 400))
				{
					if (morphingMale.getNpcId() == MaleSpikedStakato1 && !morphingMale.isDead())
					{
						final L2Npc newForm = addSpawn(MaleSpikedStakato2, morphingMale, true, 0, false);
						attack(((L2Attackable) newForm), killer);
						
						morphingMale.deleteMe();
					}
				}
				break;
			
			case SpikedStakatoNurse1:
				for (L2MonsterInstance baby : npc.getKnownList().getKnownTypeInRadius(L2MonsterInstance.class, 400))
				{
					if (baby.getNpcId() == SpikedStakatoBaby && !baby.isDead())
					{
						for (int i = 0; i < 3; i++)
						{
							final L2Npc captain = addSpawn(SpikedStakatoCaptain, baby, true, 0, false);
							attack(((L2Attackable) captain), killer);
						}
					}
				}
				break;
			
			case SpikedStakatoBaby:
				for (L2MonsterInstance morphingNurse : npc.getKnownList().getKnownTypeInRadius(L2MonsterInstance.class, 400))
				{
					if (morphingNurse.getNpcId() == SpikedStakatoNurse1 && !morphingNurse.isDead())
					{
						final L2Npc newForm = addSpawn(SpikedStakatoNurse2, morphingNurse, true, 0, false);
						attack(((L2Attackable) newForm), killer);
						
						morphingNurse.deleteMe();
					}
				}
				break;
		}
		return super.onKill(npc, killer, isPet);
	}
	
	private class EatTask implements Runnable
	{
		private final L2Npc _npc;
		private final L2Npc _follower;
		
		public EatTask(L2Npc npc, L2Npc follower)
		{
			_npc = npc;
			_follower = follower;
		}
		
		@Override
		public void run()
		{
			if (_npc.isDead())
				return;
			
			if (_follower == null || _follower.isDead())
			{
				_npc.setIsCastingNow(false);
				return;
			}
			
			_npc.setCurrentHp(_npc.getCurrentHp() + (_follower.getCurrentHp() / 2));
			_follower.doDie(_follower);
			_npc.setIsCastingNow(false);
		}
	}
}