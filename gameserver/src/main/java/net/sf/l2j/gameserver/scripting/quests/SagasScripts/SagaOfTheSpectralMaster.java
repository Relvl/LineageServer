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
public class SagaOfTheSpectralMaster extends SagasSuperClass
{
	public static String qn1 = "Q093_SagaOfTheSpectralMaster";
	public static int qnu = 93;
	public static String qna = "Saga of the Spectral Master";
	
	public SagaOfTheSpectralMaster()
	{
		super(qnu, qna);
		
		NPC = new int[]
		{
			30175,
			31287,
			31613,
			30175,
			31632,
			31646,
			31649,
			31653,
			31654,
			31655,
			31656,
			31613
		};
		
		Items = new int[]
		{
			7080,
			7606,
			7081,
			7508,
			7291,
			7322,
			7353,
			7384,
			7415,
			7446,
			7112,
			0
		};
		
		Mob = new int[]
		{
			27315,
			27242,
			27312
		};
		
		qn = qn1;
		classid = 111;
		prevclass = 0x29;
		
		X = new int[]
		{
			164650,
			47429,
			47391
		};
		
		Y = new int[]
		{
			-74121,
			-56923,
			-56929
		};
		
		Z = new int[]
		{
			-2871,
			-2383,
			-2370
		};
		
		registerNPCs();
	}
}