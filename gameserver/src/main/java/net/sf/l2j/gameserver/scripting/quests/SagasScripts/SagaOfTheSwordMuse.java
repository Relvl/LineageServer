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
public class SagaOfTheSwordMuse extends SagasSuperClass
{
	public static String qn1 = "Q072_SagaOfTheSwordMuse";
	public static int qnu = 72;
	public static String qna = "Saga of the Sword Muse";
	
	public SagaOfTheSwordMuse()
	{
		super(qnu, qna);
		
		NPC = new int[]
		{
			30853,
			31624,
			31583,
			31537,
			31618,
			31646,
			31649,
			31652,
			31654,
			31655,
			31659,
			31281
		};
		
		Items = new int[]
		{
			7080,
			7536,
			7081,
			7487,
			7270,
			7301,
			7332,
			7363,
			7394,
			7425,
			7095,
			6482
		};
		
		Mob = new int[]
		{
			27288,
			27221,
			27280
		};
		
		qn = qn1;
		classid = 100;
		prevclass = 0x15;
		
		X = new int[]
		{
			161719,
			124355,
			124376
		};
		
		Y = new int[]
		{
			-92823,
			82155,
			82127
		};
		
		Z = new int[]
		{
			-1893,
			-2803,
			-2796
		};
		
		registerNPCs();
	}
}