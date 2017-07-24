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
package net.sf.l2j.gameserver.network.client.game_to_client;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.instancemanager.CursedWeaponsManager;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.EPaperdollSlot;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.skills.AbnormalEffect;

public class CharInfo extends L2GameServerPacket {
    private final L2PcInstance player;
    private final Inventory inventory;
    private final int _x, _y, _z, _heading;
    private final int _mAtkSpd, _pAtkSpd;
    private final int _runSpd, _walkSpd;
    private final float _moveMultiplier;

    public CharInfo(L2PcInstance cha) {
        player = cha;
        inventory = player.getInventory();

        _x = player.getX();
        _y = player.getY();
        _z = player.getZ();
        _heading = player.getHeading();

        _mAtkSpd = player.getMAtkSpd();
        _pAtkSpd = player.getPAtkSpd();

        _moveMultiplier = player.getMovementSpeedMultiplier();
        _runSpd = (int) (player.getRunSpeed() / _moveMultiplier);
        _walkSpd = (int) (player.getWalkSpeed() / _moveMultiplier);
    }

    @Override
    protected final void writeImpl() {
        boolean gmSeeInvis = false;

        if (player.getAppearance().isInvisible()) {
            L2PcInstance tmp = getClient().getActiveChar();
            if (tmp != null && tmp.isGM()) { gmSeeInvis = true; }
        }

        writeC(0x03);
        writeD(_x);
        writeD(_y);
        writeD(_z);
        writeD(_heading);
        writeD(player.getObjectId());
        writeS(player.getName());
        writeD(player.getRace().ordinal());
        writeD(player.getAppearance().isFemale() ? 1 : 0);

        if (player.getClassIndex() == 0) { writeD(player.getClassId().getId()); }
        else { writeD(player.getBaseClass()); }

        writeD(inventory.getPaperdollItemId(EPaperdollSlot.PAPERDOLL_HAIRALL));
        writeD(inventory.getPaperdollItemId(EPaperdollSlot.PAPERDOLL_HEAD));
        writeD(inventory.getPaperdollItemId(EPaperdollSlot.PAPERDOLL_RHAND));
        writeD(inventory.getPaperdollItemId(EPaperdollSlot.PAPERDOLL_LHAND));
        writeD(inventory.getPaperdollItemId(EPaperdollSlot.PAPERDOLL_GLOVES));
        writeD(inventory.getPaperdollItemId(EPaperdollSlot.PAPERDOLL_CHEST));
        writeD(inventory.getPaperdollItemId(EPaperdollSlot.PAPERDOLL_LEGS));
        writeD(inventory.getPaperdollItemId(EPaperdollSlot.PAPERDOLL_FEET));
        writeD(inventory.getPaperdollItemId(EPaperdollSlot.PAPERDOLL_BACK));
        writeD(inventory.getPaperdollItemId(EPaperdollSlot.PAPERDOLL_RHAND));
        writeD(inventory.getPaperdollItemId(EPaperdollSlot.PAPERDOLL_HAIR));
        writeD(inventory.getPaperdollItemId(EPaperdollSlot.PAPERDOLL_FACE));

        // c6 new h's
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeD(inventory.getPaperdollAugmentationId(EPaperdollSlot.PAPERDOLL_RHAND));
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeD(inventory.getPaperdollAugmentationId(EPaperdollSlot.PAPERDOLL_LHAND));
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);

        writeD(player.getPvpFlag());
        writeD(player.getKarma());

        writeD(_mAtkSpd);
        writeD(_pAtkSpd);

        writeD(player.getPvpFlag());
        writeD(player.getKarma());

        writeD(_runSpd);
        writeD(_walkSpd);
        writeD(_runSpd); // swim run speed
        writeD(_walkSpd); // swim walk speed
        writeD(_runSpd); // fl run speed
        writeD(_walkSpd); // fl walk speed
        writeD(_runSpd); // fly run speed
        writeD(_walkSpd); // fly walk speed
        writeF(player.getMovementSpeedMultiplier());
        writeF(player.getAttackSpeedMultiplier());

        if (player.getMountType() != 0) {
            writeF(NpcTable.getInstance().getTemplate(player.getMountNpcId()).getCollisionRadius());
            writeF(NpcTable.getInstance().getTemplate(player.getMountNpcId()).getCollisionHeight());
        }
        else {
            writeF(player.getBaseTemplate().getCollisionRadius());
            writeF(player.getBaseTemplate().getCollisionHeight());
        }

        writeD(player.getAppearance().getHairStyle());
        writeD(player.getAppearance().getHairColor());
        writeD(player.getAppearance().getFace());

        if (gmSeeInvis) { writeS("Invisible"); }
        else { writeS(player.getTitle()); }

        writeD(player.getClanId());
        writeD(player.getClanCrestId());
        writeD(player.getAllyId());
        writeD(player.getAllyCrestId());

        writeD(0);

        writeC(player.isSitting() ? 0 : 1); // standing = 1 sitting = 0
        writeC(player.isRunning() ? 1 : 0); // running = 1 walking = 0
        writeC(player.isInCombat() ? 1 : 0);
        writeC(player.isAlikeDead() ? 1 : 0);

        if (gmSeeInvis) { writeC(0); }
        else {
            writeC(player.getAppearance().isInvisible() ? 1 : 0); // invisible = 1 visible =0
        }

        writeC(player.getMountType()); // 1 on strider 2 on wyvern 0 no mount
        writeC(player.getPrivateStoreType().getId()); // 1 - sellshop

        writeH(player.getCubics().size());
        for (int id : player.getCubics().keySet()) { writeH(id); }

        writeC(player.isInPartyMatchRoom() ? 1 : 0);

        if (gmSeeInvis) { writeD((player.getAbnormalEffect() | AbnormalEffect.STEALTH.getMask())); }
        else { writeD(player.getAbnormalEffect()); }

        writeC(player.getRecomLeft());
        writeH(player.getRecomHave()); // Blue value for name (0 = white, 255 = pure blue)
        writeD(player.getClassId().getId());

        writeD(player.getMaxCp());
        writeD((int) player.getCurrentCp());
        writeC(player.isMounted() ? 0 : player.getEnchantEffect());

        if (player.getTeam() == 1 || (Config.PLAYER_SPAWN_PROTECTION > 0 && player.isSpawnProtected())) {
            writeC(0x01); // team circle around feet 1= Blue, 2 = red
        }
        else if (player.getTeam() == 2) {
            writeC(0x02); // team circle around feet 1= Blue, 2 = red
        }
        else {
            writeC(0x00); // team circle around feet 1= Blue, 2 = red
        }

        writeD(player.getClanCrestLargeId());
        writeC(player.isNoble() ? 1 : 0); // Symbol on char menu ctrl+I
        writeC((player.isHero() || (player.isGM() && Config.GM_HERO_AURA)) ? 1 : 0); // Hero Aura

        writeC(player.isFishing() ? 1 : 0); // 0x01: Fishing Mode (Cant be undone by setting back to 0)

        Location loc = player.getFishingLoc();
        if (loc != null) {
            writeD(loc.getX());
            writeD(loc.getY());
            writeD(loc.getZ());
        }
        else {
            writeD(0);
            writeD(0);
            writeD(0);
        }

        writeD(player.getAppearance().getNameColor());

        writeD(0x00); // isRunning() as in UserInfo?

        writeD(player.getPledgeClass());
        writeD(player.getPledgeType());

        writeD(player.getAppearance().getTitleColor());

        if (player.isCursedWeaponEquipped()) { writeD(CursedWeaponsManager.getInstance().getCurrentStage(player.getCursedWeaponEquippedId()) - 1); }
        else { writeD(0x00); }
    }
}