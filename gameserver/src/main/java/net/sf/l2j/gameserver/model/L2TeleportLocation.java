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

public class L2TeleportLocation
{
	private int _teleId;
	private int _locX;
	private int _locY;
	private int _locZ;
	private int _price;
	private boolean _forNoble;
	
	public void setTeleId(int id)
	{
		_teleId = id;
	}
	
	public int getTeleId()
	{
		return _teleId;
	}
	
	public void setLocX(int locX)
	{
		_locX = locX;
	}
	
	public int getLocX()
	{
		return _locX;
	}
	
	public void setLocY(int locY)
	{
		_locY = locY;
	}
	
	public int getLocY()
	{
		return _locY;
	}
	
	public void setLocZ(int locZ)
	{
		_locZ = locZ;
	}
	
	public int getLocZ()
	{
		return _locZ;
	}
	
	public void setPrice(int price)
	{
		_price = price;
	}
	
	public int getPrice()
	{
		return _price;
	}
	
	public void setIsForNoble(boolean val)
	{
		_forNoble = val;
	}
	
	public boolean getIsForNoble()
	{
		return _forNoble;
	}
}