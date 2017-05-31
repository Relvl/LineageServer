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
public class SagaOfTheHierophant extends SagasSuperClass
{
	public static String qn1 = "Q086_SagaOfTheHierophant";
	public static int qnu = 86;
	public static String qna = "Saga of the Hierophant";
	
	public SagaOfTheHierophant()
	{
		super(qnu, qna);
		
		NPC = new int[]
		{
			30191,
			31626,
			31588,
			31280,
			31591,
			31646,
			31648,
			31652,
			31654,
			31655,
			31659,
			31280
		};
		
		Items = new int[]
		{
			7080,
			7523,
			7081,
			7501,
			7284,
			7315,
			7346,
			7377,
			7408,
			7439,
			7089,
			0
		};
		
		Mob = new int[]
		{
			27269,
			27235,
			27275
		};
		
		qn = qn1;
		classid = 98;
		prevclass = 0x11;
		
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