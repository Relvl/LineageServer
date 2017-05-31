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
package net.sf.l2j.gameserver.templates.skills;

/**
 * @author nBd
 */
public enum L2EffectType
{
	BLOCK_BUFF,
	BLOCK_DEBUFF,
	
	BUFF,
	DEBUFF,
	
	CANCEL,
	
	CANCEL_DEBUFF,
	NEGATE,
	
	CLAN_GATE,
	CHANCE_SKILL_TRIGGER,
	INCREASE_CHARGES,
	
	DMG_OVER_TIME,
	HEAL_OVER_TIME,
	COMBAT_POINT_HEAL_OVER_TIME,
	MANA_DMG_OVER_TIME,
	MANA_HEAL_OVER_TIME,
	
	ABORT_CAST,
	BLUFF,
	BETRAY,
	STUN,
	ROOT,
	SLEEP,
	MUTE,
	PHYSICAL_MUTE,
	SILENCE_MAGIC_PHYSICAL,
	FEAR,
	PARALYZE,
	PETRIFICATION,
	IMMOBILEUNTILATTACKED,
	STUN_SELF,
	CONFUSION,
	CONFUSE_MOB_ONLY,
	HATE,
	
	FAKE_DEATH,
	SILENT_MOVE,
	
	SEED,
	SPOIL,
	
	REMOVE_TARGET,
	TARGET_ME,
	
	RELAXING,
	NOBLESSE_BLESSING,
	PROTECTION_BLESSING,
	FUSION,
	CHARMOFCOURAGE,
	CHARM_OF_LUCK,
	INVINCIBLE,
	PHOENIX_BLESSING,
	
	THROW_UP,
	WARP,
	
	SIGNET_GROUND,
	SIGNET_EFFECT
}