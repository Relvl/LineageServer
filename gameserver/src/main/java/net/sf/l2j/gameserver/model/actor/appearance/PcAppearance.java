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
package net.sf.l2j.gameserver.model.actor.appearance;

public class PcAppearance
{
	private byte _face;
	private byte _hairColor;
	private byte _hairStyle;
	private boolean _sex; // Female true
	private boolean _invisible = false;
	private int _nameColor = 0xFFFFFF;
	private int _titleColor = 0xFFFF77;
	
	public PcAppearance(byte Face, byte HColor, byte HStyle, boolean Sex)
	{
		_face = Face;
		_hairColor = HColor;
		_hairStyle = HStyle;
		_sex = Sex;
	}
	
	public final byte getFace()
	{
		return _face;
	}
	
	public final void setFace(int value)
	{
		_face = (byte) value;
	}
	
	public final byte getHairColor()
	{
		return _hairColor;
	}
	
	public final void setHairColor(int value)
	{
		_hairColor = (byte) value;
	}
	
	public final byte getHairStyle()
	{
		return _hairStyle;
	}
	
	public final void setHairStyle(int value)
	{
		_hairStyle = (byte) value;
	}
	
	public final boolean getSex()
	{
		return _sex;
	}
	
	public final void setSex(boolean isfemale)
	{
		_sex = isfemale;
	}
	
	public boolean getInvisible()
	{
		return _invisible;
	}
	
	public void setInvisible()
	{
		_invisible = true;
	}
	
	public void setVisible()
	{
		_invisible = false;
	}
	
	public int getNameColor()
	{
		return _nameColor;
	}
	
	public void setNameColor(int nameColor)
	{
		_nameColor = nameColor;
	}
	
	public void setNameColor(int red, int green, int blue)
	{
		_nameColor = (red & 0xFF) + ((green & 0xFF) << 8) + ((blue & 0xFF) << 16);
	}
	
	public int getTitleColor()
	{
		return _titleColor;
	}
	
	public void setTitleColor(int titleColor)
	{
		_titleColor = titleColor;
	}
	
	public void setTitleColor(int red, int green, int blue)
	{
		_titleColor = (red & 0xFF) + ((green & 0xFF) << 8) + ((blue & 0xFF) << 16);
	}
}