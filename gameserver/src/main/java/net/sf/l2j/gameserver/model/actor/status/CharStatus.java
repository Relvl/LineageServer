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
package net.sf.l2j.gameserver.model.actor.status;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.stat.CharStat;
import net.sf.l2j.gameserver.skills.Formulas;

public class CharStatus
{
	protected static final Logger _log = Logger.getLogger(CharStatus.class.getName());
	
	private final L2Character _activeChar;
	
	private final Set<L2Character> _statusListener = ConcurrentHashMap.newKeySet();
	
	protected static final byte REGEN_FLAG_CP = 4;
	private static final byte REGEN_FLAG_HP = 1;
	private static final byte REGEN_FLAG_MP = 2;
	
	private double _currentHp = 0;
	private double _currentMp = 0;
	
	private Future<?> _regTask;
	protected byte _flagsRegenActive = 0;
	
	public CharStatus(L2Character activeChar)
	{
		_activeChar = activeChar;
	}
	
	/**
	 * Add the object to the list of L2Character that must be informed of HP/MP updates of this L2Character.
	 * @param object : L2Character to add to the listener.
	 */
	public final void addStatusListener(L2Character object)
	{
		if (object == getActiveChar())
			return;
		
		_statusListener.add(object);
	}
	
	/**
	 * Remove the object from the list of L2Character that must be informed of HP/MP updates of this L2Character.
	 * @param object : L2Character to remove to the listener.
	 */
	public final void removeStatusListener(L2Character object)
	{
		_statusListener.remove(object);
	}
	
	/**
	 * @return The list of L2Character to inform, or null if empty.
	 */
	public final Set<L2Character> getStatusListener()
	{
		return _statusListener;
	}
	
	public void reduceCp(int value)
	{
	}
	
	/**
	 * Reduce the current HP of the L2Character and launch the doDie Task if necessary.
	 * @param value : The amount of removed HPs.
	 * @param attacker : The L2Character who attacks.
	 */
	public void reduceHp(double value, L2Character attacker)
	{
		reduceHp(value, attacker, true, false, false);
	}
	
	public void reduceHp(double value, L2Character attacker, boolean isHpConsumption)
	{
		reduceHp(value, attacker, true, false, isHpConsumption);
	}
	
	public void reduceHp(double value, L2Character attacker, boolean awake, boolean isDOT, boolean isHPConsumption)
	{
		if (getActiveChar().isDead())
			return;
		
		// invul handling
		if (getActiveChar().isInvul())
		{
			// other chars can't damage
			if (attacker != getActiveChar())
				return;
			
			// only DOT and HP consumption allowed for damage self
			if (!isDOT && !isHPConsumption)
				return;
		}
		
		if (attacker != null)
		{
			final L2PcInstance attackerPlayer = attacker.getActingPlayer();
			if (attackerPlayer != null && attackerPlayer.isGM() && !attackerPlayer.getAccessLevel().canGiveDamage())
				return;
		}
		
		if (!isDOT && !isHPConsumption)
		{
			getActiveChar().stopEffectsOnDamage(awake);
			
			if (getActiveChar().isStunned() && Rnd.get(10) == 0)
				getActiveChar().stopStunning(true);
			
			if (getActiveChar().isImmobileUntilAttacked())
				getActiveChar().stopImmobileUntilAttacked(null);
		}
		
		if (value > 0) // Reduce Hp if any
			setCurrentHp(Math.max(getCurrentHp() - value, 0));
		
		// Die if character is mortal
		if (getActiveChar().getCurrentHp() < 0.5 && getActiveChar().isMortal())
		{
			getActiveChar().abortAttack();
			getActiveChar().abortCast();
			
			getActiveChar().doDie(attacker);
		}
	}
	
	public void reduceMp(double value)
	{
		setCurrentMp(Math.max(getCurrentMp() - value, 0));
	}
	
	/**
	 * Start the HP/MP/CP Regeneration task.
	 */
	public final synchronized void startHpMpRegeneration()
	{
		if (_regTask == null && !getActiveChar().isDead())
		{
			// Get the regeneration period.
			final int period = Formulas.getRegeneratePeriod(getActiveChar());
			
			// Create the HP/MP/CP regeneration task.
			_regTask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new RegenTask(), period, period);
		}
	}
	
	/**
	 * Stop the HP/MP/CP Regeneration task.
	 */
	public final synchronized void stopHpMpRegeneration()
	{
		if (_regTask != null)
		{
			// Stop the HP/MP/CP regeneration task.
			_regTask.cancel(false);
			_regTask = null;
			
			// Set the RegenActive flag to false.
			_flagsRegenActive = 0;
		}
	}
	
	public double getCurrentCp()
	{
		return 0;
	}
	
	public void setCurrentCp(double newCp)
	{
	}
	
	public final double getCurrentHp()
	{
		return _currentHp;
	}
	
	public final void setCurrentHp(double newHp)
	{
		setCurrentHp(newHp, true);
	}
	
	public void setCurrentHp(double newHp, boolean broadcastPacket)
	{
		final double maxHp = getActiveChar().getMaxHp();
		
		synchronized (this)
		{
			if (getActiveChar().isDead())
				return;
			
			if (newHp >= maxHp)
			{
				// Set the RegenActive flag to false
				_currentHp = maxHp;
				_flagsRegenActive &= ~REGEN_FLAG_HP;
				
				// Stop the HP/MP/CP Regeneration task
				if (_flagsRegenActive == 0)
					stopHpMpRegeneration();
			}
			else
			{
				// Set the RegenActive flag to true
				_currentHp = newHp;
				_flagsRegenActive |= REGEN_FLAG_HP;
				
				// Start the HP/MP/CP Regeneration task with Medium priority
				startHpMpRegeneration();
			}
		}
		
		if (broadcastPacket)
			getActiveChar().broadcastStatusUpdate();
	}
	
	public final void setCurrentHpMp(double newHp, double newMp)
	{
		setCurrentHp(newHp, false);
		setCurrentMp(newMp, true);
	}
	
	public final double getCurrentMp()
	{
		return _currentMp;
	}
	
	public final void setCurrentMp(double newMp)
	{
		setCurrentMp(newMp, true);
	}
	
	public final void setCurrentMp(double newMp, boolean broadcastPacket)
	{
		final int maxMp = getActiveChar().getStat().getMaxMp();
		
		synchronized (this)
		{
			if (getActiveChar().isDead())
				return;
			
			if (newMp >= maxMp)
			{
				// Set the RegenActive flag to false
				_currentMp = maxMp;
				_flagsRegenActive &= ~REGEN_FLAG_MP;
				
				// Stop the HP/MP/CP Regeneration task
				if (_flagsRegenActive == 0)
					stopHpMpRegeneration();
			}
			else
			{
				// Set the RegenActive flag to true
				_currentMp = newMp;
				_flagsRegenActive |= REGEN_FLAG_MP;
				
				// Start the HP/MP/CP Regeneration task with Medium priority
				startHpMpRegeneration();
			}
		}
		
		if (broadcastPacket)
			getActiveChar().broadcastStatusUpdate();
	}
	
	protected void doRegeneration()
	{
		final CharStat charstat = getActiveChar().getStat();
		
		// Modify the current HP of the L2Character.
		if (getCurrentHp() < charstat.getMaxHp())
			setCurrentHp(getCurrentHp() + Formulas.calcHpRegen(getActiveChar()), false);
		
		// Modify the current MP of the L2Character.
		if (getCurrentMp() < charstat.getMaxMp())
			setCurrentMp(getCurrentMp() + Formulas.calcMpRegen(getActiveChar()), false);
		
		// Send the StatusUpdate packet.
		getActiveChar().broadcastStatusUpdate();
	}
	
	/** Task of HP/MP regeneration */
	class RegenTask implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				doRegeneration();
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "", e);
			}
		}
	}
	
	public L2Character getActiveChar()
	{
		return _activeChar;
	}
}