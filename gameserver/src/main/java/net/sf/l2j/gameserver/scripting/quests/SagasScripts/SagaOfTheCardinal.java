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
public class SagaOfTheCardinal extends SagasSuperClass
{
	public static String qn1 = "Q085_SagaOfTheCardinal";
	public static int qnu = 85;
	public static String qna = "Saga of the Cardinal";
	
	public SagaOfTheCardinal()
	{
		super(qnu, qna);
		
		NPC = new int[]
		{
			30191,
			31626,
			31588,
			31280,
			31644,
			31646,
			31647,
			31651,
			31654,
			31655,
			31658,
			31280
		};
		
		Items = new int[]
		{
			7080,
			7522,
			7081,
			7500,
			7283,
			7314,
			7345,
			7376,
			7407,
			7438,
			7087,
			0
		};
		
		Mob = new int[]
		{
			27267,
			27234,
			27274
		};
		
		qn = qn1;
		classid = 97;
		prevclass = 0x10;
		
		X = new int[]
		{
			119518,
			181215,
			181227
		};
		
		Y = new int[]
		{
			-28658,
			36676,
			36703
		};
		
		Z = new int[]
		{
			-3811,
			-4812,
			-4816
		};
		
		registerNPCs();
	}
}