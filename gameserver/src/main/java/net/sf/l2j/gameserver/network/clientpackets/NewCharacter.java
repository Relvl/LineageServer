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
package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.datatables.CharTemplateTable;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.network.serverpackets.CharTemplates;

public final class NewCharacter extends L2GameClientPacket
{
	@Override
	protected void readImpl()
	{
	}
	
	@Override
	protected void runImpl()
	{
		CharTemplates ct = new CharTemplates();
		
		ct.addChar(CharTemplateTable.getInstance().getTemplate(0));
		ct.addChar(CharTemplateTable.getInstance().getTemplate(ClassId.fighter));
		ct.addChar(CharTemplateTable.getInstance().getTemplate(ClassId.mage));
		ct.addChar(CharTemplateTable.getInstance().getTemplate(ClassId.elvenFighter));
		ct.addChar(CharTemplateTable.getInstance().getTemplate(ClassId.elvenMage));
		ct.addChar(CharTemplateTable.getInstance().getTemplate(ClassId.darkFighter));
		ct.addChar(CharTemplateTable.getInstance().getTemplate(ClassId.darkMage));
		ct.addChar(CharTemplateTable.getInstance().getTemplate(ClassId.orcFighter));
		ct.addChar(CharTemplateTable.getInstance().getTemplate(ClassId.orcMage));
		ct.addChar(CharTemplateTable.getInstance().getTemplate(ClassId.dwarvenFighter));
		
		sendPacket(ct);
	}
}