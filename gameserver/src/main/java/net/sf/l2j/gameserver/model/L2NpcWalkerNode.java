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
package net.sf.l2j.gameserver.model;

/**
 * @author Rayan RPG, JIV
 */
public class L2NpcWalkerNode
{
	private final int _routeId;
	private String _chatText;
	private final int _moveX, _moveY, _moveZ;
	private final int _delay;
	private final boolean _running;
	
	public L2NpcWalkerNode(int routeId, int moveX, int moveY, int moveZ, boolean running, int delay, String chatText)
	{
		super();
		_routeId = routeId;
		_chatText = chatText;
		
		if (_chatText.trim().isEmpty())
			_chatText = null;
		
		_moveX = moveX;
		_moveY = moveY;
		_moveZ = moveZ;
		_running = running;
		_delay = delay;
	}
	
	public int getRouteId()
	{
		return _routeId;
	}
	
	public String getChatText()
	{
		return _chatText;
	}
	
	public int getMoveX()
	{
		return _moveX;
	}
	
	public int getMoveY()
	{
		return _moveY;
	}
	
	public int getMoveZ()
	{
		return _moveZ;
	}
	
	public int getDelay()
	{
		return _delay;
	}
	
	public boolean getRunning()
	{
		return _running;
	}
}