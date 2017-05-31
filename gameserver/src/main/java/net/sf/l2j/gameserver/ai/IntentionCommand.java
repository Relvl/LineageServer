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
package net.sf.l2j.gameserver.ai;

public class IntentionCommand
{
	private final CtrlIntention _crtlIntention;
	private final Object _arg0, _arg1;
	
	public IntentionCommand(CtrlIntention pIntention, Object pArg0, Object pArg1)
	{
		_crtlIntention = pIntention;
		_arg0 = pArg0;
		_arg1 = pArg1;
	}
	
	public CtrlIntention getCtrlIntention()
	{
		return _crtlIntention;
	}
	
	public Object getFirstArgument()
	{
		return _arg0;
	}
	
	public Object getSecondArgument()
	{
		return _arg1;
	}
}