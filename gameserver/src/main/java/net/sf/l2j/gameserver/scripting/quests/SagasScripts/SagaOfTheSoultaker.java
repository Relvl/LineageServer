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
public class SagaOfTheSoultaker extends SagasSuperClass
{
	public static String qn1 = "Q094_SagaOfTheSoultaker";
	public static int qnu = 94;
	public static String qna = "Saga of the Soultaker";
	
	public SagaOfTheSoultaker()
	{
		super(qnu, qna);
		
		NPC = new int[]
		{
			30832,
			31623,
			31279,
			31279,
			31645,
			31646,
			31648,
			31650,
			31654,
			31655,
			31657,
			31279
		};
		
		Items = new int[]
		{
			7080,
			7533,
			7081,
			7509,
			7292,
			7323,
			7354,
			7385,
			7416,
			7447,
			7085,
			0
		};
		
		Mob = new int[]
		{
			27257,
			27243,
			27265
		};
		
		qn = qn1;
		classid = 95;
		prevclass = 0x0d;
		
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