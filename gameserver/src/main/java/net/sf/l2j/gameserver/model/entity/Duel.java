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
package net.sf.l2j.gameserver.model.entity;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.instancemanager.DuelManager;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ExDuelEnd;
import net.sf.l2j.gameserver.network.serverpackets.ExDuelReady;
import net.sf.l2j.gameserver.network.serverpackets.ExDuelStart;
import net.sf.l2j.gameserver.network.serverpackets.ExDuelUpdateUserInfo;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public class Duel
{
	public static enum DuelState
	{
		NO_DUEL,
		DUELLING,
		DEAD,
		WINNER,
		INTERRUPTED
	}
	
	private static final PlaySound B04_S01 = new PlaySound(1, "B04_S01", 0, 0, 0, 0, 0);
	
	private static enum DuelResult
	{
		CONTINUE,
		TEAM_1_WIN,
		TEAM_2_WIN,
		TEAM_1_SURRENDER,
		TEAM_2_SURRENDER,
		CANCELED,
		TIMEOUT
	}
	
	private final int _duelId;
	private final boolean _partyDuel;
	private final Calendar _duelEndTime;
	private final L2PcInstance _playerA;
	private final L2PcInstance _playerB;
	private final List<PlayerCondition> _playerConditions = new CopyOnWriteArrayList<>();
	
	private int _surrenderRequest;
	
	protected Future<?> _startTask = null;
	protected Future<?> _checkTask = null;
	protected int _countdown = 5;
	
	public Duel(L2PcInstance playerA, L2PcInstance playerB, int partyDuel, int duelId)
	{
		_duelId = duelId;
		_playerA = playerA;
		_playerB = playerB;
		_partyDuel = (partyDuel == 1) ? true : false;
		
		_duelEndTime = Calendar.getInstance();
		_duelEndTime.add(Calendar.SECOND, 120);
		
		if (_partyDuel)
		{
			_countdown = 35;
			
			// Inform players that they will be ported shortly.
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.IN_A_MOMENT_YOU_WILL_BE_TRANSPORTED_TO_THE_SITE_WHERE_THE_DUEL_WILL_TAKE_PLACE);
			broadcastToTeam1(sm);
			broadcastToTeam2(sm);
		}
		
		savePlayerConditions();
		
		// Start task, used for countdowns and startDuel method call. Can be shutdowned if the check task commands it.
		_startTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new StartTask(), 1000, 1000);
		
		// Check task, used to verify if duel is disturbed.
		_checkTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new CheckTask(), 1000, 1000);
	}
	
	/**
	 * This class hold important player informations, which will be restored on duel end.
	 */
	private static class PlayerCondition
	{
		private L2PcInstance _player;
		
		private double _hp;
		private double _mp;
		private double _cp;
		
		private int _x;
		private int _y;
		private int _z;
		
		private List<L2Effect> _debuffs;
		
		public PlayerCondition(L2PcInstance player, boolean partyDuel)
		{
			if (player == null)
				return;
			
			_player = player;
			_hp = _player.getCurrentHp();
			_mp = _player.getCurrentMp();
			_cp = _player.getCurrentCp();
			
			if (partyDuel)
			{
				_x = _player.getX();
				_y = _player.getY();
				_z = _player.getZ();
			}
		}
		
		public void restoreCondition(boolean abnormalEnd)
		{
			teleportBack();
			
			if (abnormalEnd)
				return;
			
			_player.setCurrentHp(_hp);
			_player.setCurrentMp(_mp);
			_player.setCurrentCp(_cp);
			
			if (_debuffs != null)
			{
				for (L2Effect skill : _debuffs)
					if (skill != null)
						skill.exit();
			}
		}
		
		public void registerDebuff(L2Effect debuff)
		{
			if (_debuffs == null)
				_debuffs = new CopyOnWriteArrayList<>();
			
			_debuffs.add(debuff);
		}
		
		public void teleportBack()
		{
			if (_x != 0 && _y != 0)
				_player.teleToLocation(_x, _y, _z, 0);
		}
		
		public L2PcInstance getPlayer()
		{
			return _player;
		}
	}
	
	/**
	 * This task makes the countdown, both for party and 1vs1 cases.
	 * <ul>
	 * <li>For 1vs1, the timer begins to 5 (messages then start duel process).</li>
	 * <li>For party duel, the timer begins to 35 (2sec break, teleport parties, 3sec break, messages then start duel process).</li>
	 * </ul>
	 * The task is running until countdown reaches -1 (0 being startDuel).
	 */
	private class StartTask implements Runnable
	{
		public StartTask()
		{
			
		}
		
		@Override
		public void run()
		{
			// Schedule anew, until time reaches 0.
			if (_countdown < 0)
			{
				_startTask.cancel(true);
				_startTask = null;
			}
			
			switch (_countdown)
			{
				case 33:
					teleportPlayers(-83760, -238825, -3331);
					break;
				
				case 30:
				case 20:
				case 15:
				case 10:
				case 5:
				case 4:
				case 3:
				case 2:
				case 1:
					SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.THE_DUEL_WILL_BEGIN_IN_S1_SECONDS).addNumber(_countdown);
					broadcastToTeam1(sm);
					broadcastToTeam2(sm);
					break;
				
				case 0:
					sm = SystemMessage.getSystemMessage(SystemMessageId.LET_THE_DUEL_BEGIN);
					broadcastToTeam1(sm);
					broadcastToTeam2(sm);
					
					startDuel();
					break;
			}
			
			// Decrease timer.
			_countdown--;
		}
	}
	
	/**
	 * This task listens the different ways to disturb the duel. Two cases are possible :
	 * <ul>
	 * <li>DuelResult is under CONTINUE state, nothing happens. The task will continue to run every second.</li>
	 * <li>DuelResult is anything except CONTINUE, then the duel ends. Animations are played on any duel end cases, except CANCELED.</li>
	 * </ul>
	 */
	private class CheckTask implements Runnable
	{
		public CheckTask()
		{
			
		}
		
		@Override
		public void run()
		{
			final DuelResult status = checkEndDuelCondition();
			
			if (status != DuelResult.CONTINUE)
			{
				// Abort the start task if it was currently running. Interrupt it, even if it was on a loop.
				if (_startTask != null)
				{
					_startTask.cancel(true);
					_startTask = null;
				}
				
				// Abort the check task. Let this last loop alive.
				if (_checkTask != null)
				{
					_checkTask.cancel(false);
					_checkTask = null;
				}
				
				stopFighting();
				
				if (status != DuelResult.CANCELED)
					playAnimations();
				
				endDuel(status);
			}
		}
	}
	
	/**
	 * Stops all players from attacking. Used for duel timeout / interrupt.
	 */
	protected void stopFighting()
	{
		if (_partyDuel)
		{
			for (L2PcInstance partyPlayer : _playerA.getParty().getPartyMembers())
			{
				partyPlayer.abortCast();
				partyPlayer.getAI().setIntention(CtrlIntention.ACTIVE);
				partyPlayer.setTarget(null);
				partyPlayer.sendPacket(ActionFailed.STATIC_PACKET);
			}
			
			for (L2PcInstance partyPlayer : _playerB.getParty().getPartyMembers())
			{
				partyPlayer.abortCast();
				partyPlayer.getAI().setIntention(CtrlIntention.ACTIVE);
				partyPlayer.setTarget(null);
				partyPlayer.sendPacket(ActionFailed.STATIC_PACKET);
			}
		}
		else
		{
			_playerA.abortCast();
			_playerB.abortCast();
			_playerA.getAI().setIntention(CtrlIntention.ACTIVE);
			_playerA.setTarget(null);
			_playerB.getAI().setIntention(CtrlIntention.ACTIVE);
			_playerB.setTarget(null);
			_playerA.sendPacket(ActionFailed.STATIC_PACKET);
			_playerB.sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
	
	/**
	 * Starts the duel.<br>
	 * Save players conditions, cancel active trade, set the team color and all duel start packets.<br>
	 * Handle the duel task, which checks if the duel ends in one way or another.
	 */
	protected void startDuel()
	{
		if (_partyDuel)
		{
			for (L2PcInstance partyPlayer : _playerA.getParty().getPartyMembers())
			{
				partyPlayer.cancelActiveTrade();
				partyPlayer.setInDuel(_duelId);
				partyPlayer.setTeam(1);
				partyPlayer.broadcastUserInfo();
				// TODO Servitors getTeam update
				broadcastToTeam2(new ExDuelUpdateUserInfo(partyPlayer));
			}
			
			for (L2PcInstance partyPlayer : _playerB.getParty().getPartyMembers())
			{
				partyPlayer.cancelActiveTrade();
				partyPlayer.setInDuel(_duelId);
				partyPlayer.setTeam(2);
				partyPlayer.broadcastUserInfo();
				// TODO Servitors getTeam update
				broadcastToTeam1(new ExDuelUpdateUserInfo(partyPlayer));
			}
			
			// Send duel Start packets.
			ExDuelReady ready = new ExDuelReady(1);
			ExDuelStart start = new ExDuelStart(1);
			
			broadcastToTeam1(ready);
			broadcastToTeam2(ready);
			broadcastToTeam1(start);
			broadcastToTeam2(start);
		}
		else
		{
			// Set states.
			_playerA.setInDuel(_duelId);
			_playerA.setTeam(1);
			_playerB.setInDuel(_duelId);
			_playerB.setTeam(2);
			
			// Send duel Start packets.
			ExDuelReady ready = new ExDuelReady(0);
			ExDuelStart start = new ExDuelStart(0);
			
			broadcastToTeam1(ready);
			broadcastToTeam2(ready);
			broadcastToTeam1(start);
			broadcastToTeam2(start);
			
			broadcastToTeam1(new ExDuelUpdateUserInfo(_playerB));
			broadcastToTeam2(new ExDuelUpdateUserInfo(_playerA));
			
			_playerA.broadcastUserInfo();
			// TODO Servitors getTeam update
			_playerB.broadcastUserInfo();
			// TODO Servitors getTeam update
		}
		
		// Play sound.
		broadcastToTeam1(B04_S01);
		broadcastToTeam2(B04_S01);
	}
	
	/**
	 * Save the current player condition: hp, mp, cp, location
	 */
	private void savePlayerConditions()
	{
		if (_partyDuel)
		{
			for (L2PcInstance partyPlayer : _playerA.getParty().getPartyMembers())
				_playerConditions.add(new PlayerCondition(partyPlayer, _partyDuel));
			
			for (L2PcInstance partyPlayer : _playerB.getParty().getPartyMembers())
				_playerConditions.add(new PlayerCondition(partyPlayer, _partyDuel));
		}
		else
		{
			_playerConditions.add(new PlayerCondition(_playerA, _partyDuel));
			_playerConditions.add(new PlayerCondition(_playerB, _partyDuel));
		}
	}
	
	/**
	 * Restore player conditions.
	 * @param abnormalEnd : true if the duel was canceled.
	 */
	private void restorePlayerConditions(boolean abnormalEnd)
	{
		if (_partyDuel)
		{
			for (L2PcInstance partyPlayer : _playerA.getParty().getPartyMembers())
			{
				partyPlayer.setInDuel(0);
				partyPlayer.setTeam(0);
				partyPlayer.broadcastUserInfo();
				// TODO Servitors getTeam update
			}
			
			for (L2PcInstance partyPlayer : _playerB.getParty().getPartyMembers())
			{
				partyPlayer.setInDuel(0);
				partyPlayer.setTeam(0);
				partyPlayer.broadcastUserInfo();
				// TODO Servitors getTeam update
			}
		}
		else
		{
			_playerA.setInDuel(0);
			_playerA.setTeam(0);
			_playerA.broadcastUserInfo();
			// TODO Servitors getTeam update
			
			_playerB.setInDuel(0);
			_playerB.setTeam(0);
			_playerB.broadcastUserInfo();
			// TODO Servitors getTeam update
		}
		
		// Restore player conditions, but only for party duel (no matter the end) && 1vs1 which ends normally.
		if ((!_partyDuel && !abnormalEnd) || _partyDuel)
		{
			for (PlayerCondition cond : _playerConditions)
				cond.restoreCondition(abnormalEnd);
		}
	}
	
	/**
	 * @return the duel id.
	 */
	public int getId()
	{
		return _duelId;
	}
	
	/**
	 * @return the remaining time.
	 */
	public int getRemainingTime()
	{
		return (int) (_duelEndTime.getTimeInMillis() - Calendar.getInstance().getTimeInMillis());
	}
	
	/**
	 * @return the player that requested the duel.
	 */
	public L2PcInstance getPlayerA()
	{
		return _playerA;
	}
	
	/**
	 * @return the player that was challenged.
	 */
	public L2PcInstance getPlayerB()
	{
		return _playerB;
	}
	
	/**
	 * @return true if the duel was a party duel, false otherwise.
	 */
	public boolean isPartyDuel()
	{
		return _partyDuel;
	}
	
	/**
	 * Teleport all players to the given coordinates. Used by party duel only.
	 * @param x
	 * @param y
	 * @param z
	 */
	protected void teleportPlayers(int x, int y, int z)
	{
		// TODO: adjust the values if needed... or implement something better (especially using more then 1 arena)
		if (!_partyDuel)
			return;
		
		int offset = 0;
		
		for (L2PcInstance partyPlayer : _playerA.getParty().getPartyMembers())
		{
			partyPlayer.teleToLocation(x + offset - 180, y - 150, z, 0);
			offset += 40;
		}
		
		offset = 0;
		for (L2PcInstance partyPlayer : _playerB.getParty().getPartyMembers())
		{
			partyPlayer.teleToLocation(x + offset - 180, y + 150, z, 0);
			offset += 40;
		}
	}
	
	/**
	 * Broadcast a packet to the challenger team.
	 * @param packet : The packet to send.
	 */
	public void broadcastToTeam1(L2GameServerPacket packet)
	{
		if (_partyDuel && _playerA.getParty() != null)
		{
			for (L2PcInstance partyPlayer : _playerA.getParty().getPartyMembers())
				partyPlayer.sendPacket(packet);
		}
		else
			_playerA.sendPacket(packet);
	}
	
	/**
	 * Broadcast a packet to the challenged team.
	 * @param packet : The packet to send.
	 */
	public void broadcastToTeam2(L2GameServerPacket packet)
	{
		if (_partyDuel && _playerB.getParty() != null)
		{
			for (L2PcInstance partyPlayer : _playerB.getParty().getPartyMembers())
				partyPlayer.sendPacket(packet);
		}
		else
			_playerB.sendPacket(packet);
	}
	
	/**
	 * Playback the bow animation for loosers, victory pose for winners.<br>
	 * The method works even if other side is null or offline.
	 */
	protected void playAnimations()
	{
		if (_playerA.isOnline())
		{
			if (_playerA.getDuelState() == DuelState.WINNER)
			{
				if (_partyDuel && _playerA.getParty() != null)
				{
					for (L2PcInstance partyPlayer : _playerA.getParty().getPartyMembers())
						partyPlayer.broadcastPacket(new SocialAction(partyPlayer, 3));
				}
				else
					_playerA.broadcastPacket(new SocialAction(_playerA, 3));
			}
			else if (_playerA.getDuelState() == DuelState.DEAD)
			{
				if (_partyDuel && _playerA.getParty() != null)
				{
					for (L2PcInstance partyPlayer : _playerA.getParty().getPartyMembers())
						partyPlayer.broadcastPacket(new SocialAction(partyPlayer, 7));
				}
				else
					_playerA.broadcastPacket(new SocialAction(_playerA, 7));
			}
		}
		
		if (_playerB.isOnline())
		{
			if (_playerB.getDuelState() == DuelState.WINNER)
			{
				if (_partyDuel && _playerB.getParty() != null)
				{
					for (L2PcInstance partyPlayer : _playerB.getParty().getPartyMembers())
						partyPlayer.broadcastPacket(new SocialAction(partyPlayer, 3));
				}
				else
					_playerB.broadcastPacket(new SocialAction(_playerB, 3));
			}
			else if (_playerB.getDuelState() == DuelState.DEAD)
			{
				if (_partyDuel && _playerB.getParty() != null)
				{
					for (L2PcInstance partyPlayer : _playerB.getParty().getPartyMembers())
						partyPlayer.broadcastPacket(new SocialAction(partyPlayer, 7));
				}
				else
					_playerB.broadcastPacket(new SocialAction(_playerB, 7));
			}
		}
	}
	
	/**
	 * This method ends a duel, sending messages to each team, end duel packet, cleaning player conditions and then removing duel from manager.
	 * @param result : The duel result.
	 */
	protected void endDuel(DuelResult result)
	{
		SystemMessage sm = null;
		switch (result)
		{
			case TEAM_2_SURRENDER:
				sm = SystemMessage.getSystemMessage((_partyDuel) ? SystemMessageId.SINCE_S1_PARTY_WITHDREW_FROM_THE_DUEL_S2_PARTY_HAS_WON : SystemMessageId.SINCE_S1_WITHDREW_FROM_THE_DUEL_S2_HAS_WON).addString(_playerB.getName()).addString(_playerA.getName());
				broadcastToTeam1(sm);
				broadcastToTeam2(sm);
			case TEAM_1_WIN:
				sm = SystemMessage.getSystemMessage((_partyDuel) ? SystemMessageId.S1_PARTY_HAS_WON_THE_DUEL : SystemMessageId.S1_HAS_WON_THE_DUEL).addString(_playerA.getName());
				break;
			
			case TEAM_1_SURRENDER:
				sm = SystemMessage.getSystemMessage((_partyDuel) ? SystemMessageId.SINCE_S1_PARTY_WITHDREW_FROM_THE_DUEL_S2_PARTY_HAS_WON : SystemMessageId.SINCE_S1_WITHDREW_FROM_THE_DUEL_S2_HAS_WON).addString(_playerA.getName()).addString(_playerB.getName());
				broadcastToTeam1(sm);
				broadcastToTeam2(sm);
			case TEAM_2_WIN:
				sm = SystemMessage.getSystemMessage((_partyDuel) ? SystemMessageId.S1_PARTY_HAS_WON_THE_DUEL : SystemMessageId.S1_HAS_WON_THE_DUEL).addString(_playerB.getName());
				break;
			
			case CANCELED:
				sm = SystemMessage.getSystemMessage(SystemMessageId.THE_DUEL_HAS_ENDED_IN_A_TIE);
				break;
			
			case TIMEOUT:
				sm = SystemMessage.getSystemMessage(SystemMessageId.THE_DUEL_HAS_ENDED_IN_A_TIE);
				break;
		}
		
		broadcastToTeam1(sm);
		broadcastToTeam2(sm);
		
		restorePlayerConditions(result == DuelResult.CANCELED);
		
		// Send end duel packet.
		ExDuelEnd duelEnd = new ExDuelEnd((_partyDuel) ? 1 : 0);
		
		broadcastToTeam1(duelEnd);
		broadcastToTeam2(duelEnd);
		
		// Cleanup.
		_playerConditions.clear();
		DuelManager.getInstance().removeDuel(_duelId);
	}
	
	/**
	 * This method checks all possible scenari which can disturb a duel, and return the appropriate status.
	 * @return DuelResult : The duel status.
	 */
	protected DuelResult checkEndDuelCondition()
	{
		// Both players are offline.
		if (!_playerA.isOnline() && !_playerB.isOnline())
			return DuelResult.CANCELED;
		
		// Player A is offline.
		if (!_playerA.isOnline())
		{
			onPlayerDefeat(_playerA);
			return DuelResult.TEAM_1_SURRENDER;
		}
		
		// Player B is offline.
		if (!_playerB.isOnline())
		{
			onPlayerDefeat(_playerB);
			return DuelResult.TEAM_2_SURRENDER;
		}
		
		// Duel surrender request.
		if (_surrenderRequest != 0)
			return (_surrenderRequest == 1) ? DuelResult.TEAM_1_SURRENDER : DuelResult.TEAM_2_SURRENDER;
		
		// Duel timed out.
		if (getRemainingTime() <= 0)
			return DuelResult.TIMEOUT;
		
		// One of the players is declared winner.
		if (_playerA.getDuelState() == DuelState.WINNER)
			return DuelResult.TEAM_1_WIN;
		
		if (_playerB.getDuelState() == DuelState.WINNER)
			return DuelResult.TEAM_2_WIN;
		
		if (!_partyDuel)
		{
			// Duel was interrupted e.g.: player was attacked by mobs / other players
			if (_playerA.getDuelState() == DuelState.INTERRUPTED || _playerB.getDuelState() == DuelState.INTERRUPTED)
				return DuelResult.CANCELED;
			
			// Players are too far apart.
			if (!_playerA.isInsideRadius(_playerB, 2000, false, false))
				return DuelResult.CANCELED;
			
			// One of the players is engaged in PvP.
			if (_playerA.getPvpFlag() != 0 || _playerB.getPvpFlag() != 0)
				return DuelResult.CANCELED;
			
			// One of the players is in a Siege, Peace or PvP zone.
			if (_playerA.isInsideZone(ZoneId.PEACE) || _playerB.isInsideZone(ZoneId.PEACE) || _playerA.isInsideZone(ZoneId.SIEGE) || _playerB.isInsideZone(ZoneId.SIEGE) || _playerA.isInsideZone(ZoneId.PVP) || _playerB.isInsideZone(ZoneId.PVP))
				return DuelResult.CANCELED;
		}
		else
		{
			if (_playerA.getParty() != null)
			{
				for (L2PcInstance partyMember : _playerA.getParty().getPartyMembers())
				{
					// Duel was interrupted e.g.: player was attacked by mobs / other players
					if (partyMember.getDuelState() == DuelState.INTERRUPTED)
						return DuelResult.CANCELED;
					
					// Players are too far apart.
					if (!partyMember.isInsideRadius(_playerB, 2000, false, false))
						return DuelResult.CANCELED;
					
					// One of the players is engaged in PvP.
					if (partyMember.getPvpFlag() != 0)
						return DuelResult.CANCELED;
					
					// One of the players is in a Siege, Peace or PvP zone.
					if (partyMember.isInsideZone(ZoneId.PEACE) || partyMember.isInsideZone(ZoneId.PEACE) || partyMember.isInsideZone(ZoneId.SIEGE))
						return DuelResult.CANCELED;
				}
			}
			
			if (_playerB.getParty() != null)
			{
				for (L2PcInstance partyMember : _playerB.getParty().getPartyMembers())
				{
					// Duel was interrupted e.g.: player was attacked by mobs / other players
					if (partyMember.getDuelState() == DuelState.INTERRUPTED)
						return DuelResult.CANCELED;
					
					// Players are too far apart.
					if (!partyMember.isInsideRadius(_playerA, 2000, false, false))
						return DuelResult.CANCELED;
					
					// One of the players is engaged in PvP.
					if (partyMember.getPvpFlag() != 0)
						return DuelResult.CANCELED;
					
					// One of the players is in a Siege, Peace or PvP zone.
					if (partyMember.isInsideZone(ZoneId.PEACE) || partyMember.isInsideZone(ZoneId.PEACE) || partyMember.isInsideZone(ZoneId.SIEGE))
						return DuelResult.CANCELED;
				}
			}
		}
		
		return DuelResult.CONTINUE;
	}
	
	/**
	 * Register a surrender request. It updates DuelState of players.
	 * @param player : The player who surrenders.
	 */
	public void doSurrender(L2PcInstance player)
	{
		// A surrender request is already under process, return.
		if (_surrenderRequest != 0)
			return;
		
		// TODO: Can every party member cancel a party duel? or only the party leaders?
		if (_partyDuel)
		{
			if (_playerA.getParty().getPartyMembers().contains(player))
			{
				_surrenderRequest = 1;
				
				for (L2PcInstance partyPlayer : _playerA.getParty().getPartyMembers())
					partyPlayer.setDuelState(DuelState.DEAD);
				
				for (L2PcInstance partyPlayer : _playerB.getParty().getPartyMembers())
					partyPlayer.setDuelState(DuelState.WINNER);
			}
			else if (_playerB.getParty().getPartyMembers().contains(player))
			{
				_surrenderRequest = 2;
				
				for (L2PcInstance partyPlayer : _playerB.getParty().getPartyMembers())
					partyPlayer.setDuelState(DuelState.DEAD);
				
				for (L2PcInstance partyPlayer : _playerA.getParty().getPartyMembers())
					partyPlayer.setDuelState(DuelState.WINNER);
			}
		}
		else
		{
			if (player == _playerA)
			{
				_surrenderRequest = 1;
				
				_playerA.setDuelState(DuelState.DEAD);
				_playerB.setDuelState(DuelState.WINNER);
			}
			else if (player == _playerB)
			{
				_surrenderRequest = 2;
				
				_playerB.setDuelState(DuelState.DEAD);
				_playerA.setDuelState(DuelState.WINNER);
			}
		}
	}
	
	/**
	 * This method is called whenever a player was defeated in a duel. It updates DuelState of players.
	 * @param player : The defeated player.
	 */
	public void onPlayerDefeat(L2PcInstance player)
	{
		// Set player as defeated.
		player.setDuelState(DuelState.DEAD);
		
		if (_partyDuel)
		{
			boolean teamDefeated = true;
			for (L2PcInstance partyPlayer : player.getParty().getPartyMembers())
			{
				if (partyPlayer.getDuelState() == DuelState.DUELLING)
				{
					teamDefeated = false;
					break;
				}
			}
			
			if (teamDefeated)
			{
				L2PcInstance winner = _playerA;
				if (_playerA.getParty().getPartyMembers().contains(player))
					winner = _playerB;
				
				for (L2PcInstance partyPlayer : winner.getParty().getPartyMembers())
					partyPlayer.setDuelState(DuelState.WINNER);
			}
		}
		else
		{
			if (_playerA == player)
				_playerB.setDuelState(DuelState.WINNER);
			else
				_playerA.setDuelState(DuelState.WINNER);
		}
	}
	
	/**
	 * This method is called when a player left a party.
	 * <ul>
	 * <li>The player was a party leader : the duel is canceled.</li>
	 * <li>The player was a regular member : the duel continues, but all infos regarding this player are dropped from duel.</li>
	 * </ul>
	 * @param player : The player who left.
	 */
	public void onRemoveFromParty(L2PcInstance player)
	{
		// The duel isn't a party duel, ignore it.
		if (!_partyDuel)
			return;
		
		// The player is leaving his party during party duel ; if he's playerA or playerB cancel the duel and port the players back.
		if (player == _playerA || player == _playerB)
		{
			for (PlayerCondition cond : _playerConditions)
			{
				cond.teleportBack();
				cond.getPlayer().setInDuel(0);
			}
			endDuel(DuelResult.CANCELED);
		}
		// Otherwise, teleport the player back & delete his PlayerCondition record. The duel continues.
		else
		{
			for (PlayerCondition cond : _playerConditions)
			{
				if (cond.getPlayer() == player)
				{
					cond.teleportBack();
					_playerConditions.remove(cond);
					break;
				}
			}
			player.setInDuel(0);
		}
	}
	
	/**
	 * This method is called to register an effect.
	 * @param player : The player condition to affect.
	 * @param effect : The effect to register.
	 */
	public void onBuff(L2PcInstance player, L2Effect effect)
	{
		for (PlayerCondition cond : _playerConditions)
		{
			if (cond.getPlayer() == player)
			{
				cond.registerDebuff(effect);
				return;
			}
		}
	}
}