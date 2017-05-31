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
public class SagaOfTheGhostSentinel extends SagasSuperClass
{
	public static String qn1 = "Q084_SagaOfTheGhostSentinel";
	public static int qnu = 84;
	public static String qna = "Saga of the Ghost Sentinel";
	
	public SagaOfTheGhostSentinel()
	{
		super(qnu, qna);
		
		NPC = new int[]
		{
			30702,
			31587,
			31604,
			31640,
			31635,
			31646,
			31649,
			31652,
			31654,
			31655,
			31659,
			31641
		};
		
		Items = new int[]
		{
			7080,
			7521,
			7081,
			7499,
			7282,
			7313,
			7344,
			7375,
			7406,
			7437,
			7107,
			0
		};
		
		Mob = new int[]
		{
			27298,
			27233,
			27307
		};
		
		qn = qn1;
		classid = 109;
		prevclass = 0x25;
		
		X = new int[]
		{
			161719,
			124376,
			124376
		};
		
		Y = new int[]
		{
			-92823,
			82127,
			82127
		};
		
		Z = new int[]
		{
			-1893,
			-2796,
			-2796
		};
		
		registerNPCs();
	}
}