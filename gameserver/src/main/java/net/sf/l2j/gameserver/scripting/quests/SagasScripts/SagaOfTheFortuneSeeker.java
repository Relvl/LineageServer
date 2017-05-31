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
public class SagaOfTheFortuneSeeker extends SagasSuperClass
{
	public static String qn1 = "Q099_SagaOfTheFortuneSeeker";
	public static int qnu = 99;
	public static String qna = "Saga of the Fortune Seeker";
	
	public SagaOfTheFortuneSeeker()
	{
		super(qnu, qna);
		
		NPC = new int[]
		{
			31594,
			31623,
			31600,
			31600,
			31601,
			31646,
			31649,
			31650,
			31654,
			31655,
			31657,
			31600
		};
		
		Items = new int[]
		{
			7080,
			7608,
			7081,
			7514,
			7297,
			7328,
			7359,
			7390,
			7421,
			7452,
			7109,
			0
		};
		
		Mob = new int[]
		{
			27259,
			27248,
			27309
		};
		
		qn = qn1;
		classid = 117;
		prevclass = 0x37;
		
		X = new int[]
		{
			191046,
			46066,
			46087
		};
		
		Y = new int[]
		{
			-40640,
			-36396,
			-36372
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