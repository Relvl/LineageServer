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
package net.sf.l2j.gameserver.ai;

/** Enumeration of generic intentions of an NPC/PC */
public enum CtrlIntention
{
	/** Do nothing, disconnect AI of NPC if no players around. */
	IDLE,
	/** Alerted state without goal : scan attackable targets, random walk, etc. */
	ACTIVE,
	/** Rest (sit until attacked). */
	REST,
	/** Attack target (cast combat magic, go to target, combat) - may be ignored (another target, invalid zoning, etc). */
	ATTACK,
	/** Cast a spell, depending on the spell - may start or stop attacking. */
	CAST,
	/** Just move to another location. */
	MOVE_TO,
	/** Like move, but check target's movement and follow it. */
	FOLLOW,
	/** Pick up item (go to item, pick up it, become idle). */
	PICK_UP,
	/** Move to target, then interact. */
	INTERACT
}