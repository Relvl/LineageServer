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
public class SagaOfTheDreadnought extends SagasSuperClass
{
	public static String qn1 = "Q074_SagaOfTheDreadnought";
	public static int qnu = 74;
	public static String qna = "Saga of the Dreadnought";
	
	public SagaOfTheDreadnought()
	{
		super(qnu, qna);
		
		NPC = new int[]
		{
			30850,
			31624,
			31298,
			31276,
			31595,
			31646,
			31648,
			31650,
			31654,
			31655,
			31657,
			31522
		};
		
		Items = new int[]
		{
			7080,
			7538,
			7081,
			7489,
			7272,
			7303,
			7334,
			7365,
			7396,
			7427,
			7097,
			6480
		};
		
		Mob = new int[]
		{
			27290,
			27223,
			27282
		};
		
		qn = qn1;
		classid = 89;
		prevclass = 0x03;
		
		X = new int[]
		{
			191046,
			46087,
			46066
		};
		
		Y = new int[]
		{
			-40640,
			-36372,
			-36396
		};
		
		Z = new int[]
		{
			-3042,
			-1685,
			-1685
		};
		
		registerNPCs();
	}
}