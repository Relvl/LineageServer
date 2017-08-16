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
package net.sf.l2j.gameserver.scripting.scripts.custom;

import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;
import net.sf.l2j.gameserver.scripting.scripts.village_master.FirstClassChange;

/**
 * @authors: DrLecter (python), Nyaran (java)
 */
public class ShadowWeapon extends Quest {
    private static final String qn = "ShadowWeapon";

    // itemId for shadow weapon coupons, it's not used more than once but increases readability
    private static final int D_COUPON = 8869;
    private static final int C_COUPON = 8870;

    // TODO: That list will be moved into SecondClassChange once it's made.
    public static final int[] SECONDCLASSNPCS =
            {
                    30109,
                    30115,
                    30120,
                    30174,
                    30176,
                    30187,
                    30191,
                    30195,
                    30474,
                    30511,
                    30512,
                    30513,
                    30676,
                    30677,
                    30681,
                    30685,
                    30687,
                    30689,
                    30694,
                    30699,
                    30704,
                    30845,
                    30847,
                    30849,
                    30854,
                    30857,
                    30862,
                    30865,
                    30894,
                    30897,
                    30900,
                    30905,
                    30910,
                    30913,
                    31269,
                    31272,
                    31276,
                    31279,
                    31285,
                    31288,
                    31314,
                    31317,
                    31321,
                    31324,
                    31326,
                    31328,
                    31331,
                    31334,
                    31336,
                    31755,
                    31958,
                    31961,
                    31965,
                    31968,
                    31974,
                    31977,
                    31996,
                    32094,
                    32095,
                    32096
            };

    public ShadowWeapon() {
        super(-1, "custom");

        addStartNpc(FirstClassChange.FIRSTCLASSNPCS);
        addTalkId(FirstClassChange.FIRSTCLASSNPCS);

        addStartNpc(SECONDCLASSNPCS);
        addTalkId(SECONDCLASSNPCS);
    }

    @Override
    public String onTalk(L2Npc npc, L2PcInstance player) {
        QuestState st = player.getQuestState(qn);
        String htmltext = getNoQuestMsg();
        if (st == null) { return htmltext; }

        boolean hasD = st.hasQuestItems(D_COUPON);
        boolean hasC = st.hasQuestItems(C_COUPON);

        if (hasD || hasC) {
            // let's assume character had both c & d-grade coupons, we'll confirm later
            String multisell = "306893003";
            if (!hasD) // if s/he had c-grade only...
            { multisell = "306893002"; }
            else if (!hasC) // or d-grade only.
            { multisell = "306893001"; }

            // finally, return htm with proper multisell value in it.
            htmltext = getHtmlText("exchange.htm").replace("%msid%", multisell);
        }
        else { htmltext = "exchange-no.htm"; }

        st.exitQuest(true);
        return htmltext;
    }
}