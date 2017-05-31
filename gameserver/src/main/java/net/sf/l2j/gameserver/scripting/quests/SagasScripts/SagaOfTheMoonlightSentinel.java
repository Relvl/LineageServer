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
package net.sf.l2j.gameserver.scripting.quests.SagasScripts;

/**
 * @author Emperorc
 */
public class SagaOfTheMoonlightSentinel extends SagasSuperClass
{
	public static String qn1 = "Q083_SagaOfTheMoonlightSentinel";
	public static int qnu = 83;
	public static String qna = "Saga of the Moonlight Sentinel";
	
	public SagaOfTheMoonlightSentinel()
	{
		super(qnu, qna);
		
		NPC = new int[]
		{
			30702,
			31627,
			31604,
			31640,
			31634,
			31646,
			31648,
			31652,
			31654,
			31655,
			31658,
			31641
		};
		
		Items = new int[]
		{
			7080,
			7520,
			7081,
			7498,
			7281,
			7312,
			7343,
			7374,
			7405,
			7436,
			7106,
			0
		};
		
		Mob = new int[]
		{
			27297,
			27232,
			27306
		};
		
		qn = qn1;
		classid = 102;
		prevclass = 0x18;
		
		X = new int[]
		{
			161719,
			181227,
			181215
		};
		
		Y = new int[]
		{
			-92823,
			36703,
			36676
		};
		
		Z = new int[]
		{
			-1893,
			-4816,
			-4812
		};
		
		registerNPCs();
	}
}