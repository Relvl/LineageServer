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
public class SagaOfEvasSaint extends SagasSuperClass
{
	public static String qn1 = "Q087_SagaOfEvasSaint";
	public static int qnu = 87;
	public static String qna = "Saga of Eva's Saint";
	
	public SagaOfEvasSaint()
	{
		super(qnu, qna);
		
		NPC = new int[]
		{
			30191,
			31626,
			31588,
			31280,
			31620,
			31646,
			31649,
			31653,
			31654,
			31655,
			31657,
			31280
		};
		
		Items = new int[]
		{
			7080,
			7524,
			7081,
			7502,
			7285,
			7316,
			7347,
			7378,
			7409,
			7440,
			7088,
			0
		};
		
		Mob = new int[]
		{
			27266,
			27236,
			27276
		};
		
		qn = qn1;
		classid = 105;
		prevclass = 0x1e;
		
		X = new int[]
		{
			164650,
			46087,
			46066
		};
		
		Y = new int[]
		{
			-74121,
			-36372,
			-36396
		};
		
		Z = new int[]
		{
			-2871,
			-1685,
			-1685
		};
		
		registerNPCs();
	}
}