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
package net.sf.l2j.gameserver.model.actor.instance;

import java.util.StringTokenizer;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.BuyListTable;
import net.sf.l2j.gameserver.datatables.MultisellData;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.buylist.NpcBuyList;
import net.sf.l2j.gameserver.network.serverpackets.BuyList;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SellList;
import net.sf.l2j.gameserver.network.serverpackets.ShopPreviewList;

/**
 * L2Merchant type, it got buy/sell methods && bypasses.<br>
 * It is used as extends for classes such as L2Fisherman, L2CastleChamberlain, etc.
 */
public class L2MerchantInstance extends L2NpcInstance
{
	public L2MerchantInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String filename = "";
		
		if (val == 0)
			filename = "" + npcId;
		else
			filename = npcId + "-" + val;
		
		return "data/html/merchant/" + filename + ".htm";
	}
	
	private final void showWearWindow(L2PcInstance player, int val)
	{
		final NpcBuyList buyList = BuyListTable.getInstance().getBuyList(val);
		if (buyList == null || !buyList.isNpcAllowed(getNpcId()))
			return;
		
		player.tempInventoryDisable();
		player.sendPacket(new ShopPreviewList(buyList, player.getAdena(), player.getExpertiseIndex()));
	}
	
	protected final void showBuyWindow(L2PcInstance player, int val)
	{
		final NpcBuyList buyList = BuyListTable.getInstance().getBuyList(val);
		if (buyList == null || !buyList.isNpcAllowed(getNpcId()))
			return;
		
		player.tempInventoryDisable();
		player.sendPacket(new BuyList(buyList, player.getAdena(), (getIsInTown()) ? getCastle().getTaxRate() : 0));
	}
	
	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		StringTokenizer st = new StringTokenizer(command, " ");
		String actualCommand = st.nextToken(); // Get actual command
		
		if (actualCommand.equalsIgnoreCase("Buy"))
		{
			if (st.countTokens() < 1)
				return;
			
			showBuyWindow(player, Integer.parseInt(st.nextToken()));
		}
		else if (actualCommand.equalsIgnoreCase("Sell"))
		{
			player.sendPacket(new SellList(player));
		}
		else if (actualCommand.equalsIgnoreCase("Wear") && Config.ALLOW_WEAR)
		{
			if (st.countTokens() < 1)
				return;
			
			showWearWindow(player, Integer.parseInt(st.nextToken()));
		}
		else if (actualCommand.equalsIgnoreCase("Multisell"))
		{
			if (st.countTokens() < 1)
				return;
			
			MultisellData.getInstance().separateAndSend(Integer.parseInt(st.nextToken()), player, false, getCastle().getTaxRate());
		}
		else if (actualCommand.equalsIgnoreCase("Multisell_Shadow"))
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			
			if (player.getLevel() < 40)
				html.setFile("data/html/common/shadow_item-lowlevel.htm");
			else if (player.getLevel() < 46)
				html.setFile("data/html/common/shadow_item_mi_c.htm");
			else if (player.getLevel() < 52)
				html.setFile("data/html/common/shadow_item_hi_c.htm");
			else
				html.setFile("data/html/common/shadow_item_b.htm");
			
			html.replace("%objectId%", getObjectId());
			player.sendPacket(html);
		}
		else if (actualCommand.equalsIgnoreCase("Exc_Multisell"))
		{
			if (st.countTokens() < 1)
				return;
			
			MultisellData.getInstance().separateAndSend(Integer.parseInt(st.nextToken()), player, true, getCastle().getTaxRate());
		}
		else if (actualCommand.equalsIgnoreCase("Newbie_Exc_Multisell"))
		{
			if (st.countTokens() < 1)
				return;
			
			if (player.isNewbie())
				MultisellData.getInstance().separateAndSend(Integer.parseInt(st.nextToken()), player, true, getCastle().getTaxRate());
			else
				showChatWindow(player, "data/html/exchangelvlimit.htm");
		}
		else
			super.onBypassFeedback(player, command);
	}
}