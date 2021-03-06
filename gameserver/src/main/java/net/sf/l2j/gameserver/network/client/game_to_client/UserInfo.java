package net.sf.l2j.gameserver.network.client.game_to_client;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.instancemanager.CursedWeaponsManager;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.item.EPaperdollSlot;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.skills.AbnormalEffect;

public class UserInfo extends L2GameServerPacket {
    private final L2PcInstance _activeChar;
    private final int _runSpd, _walkSpd;
    private int _relation;
    private final float _moveMultiplier;

    public UserInfo(L2PcInstance character) {
        _activeChar = character;

        _moveMultiplier = _activeChar.getMovementSpeedMultiplier();
        _runSpd = (int) (_activeChar.getRunSpeed() / _moveMultiplier);
        _walkSpd = (int) (_activeChar.getWalkSpeed() / _moveMultiplier);
        _relation = _activeChar.isClanLeader() ? 0x40 : 0;

        if (_activeChar.getSiegeState() == 1) { _relation |= 0x180; }
        if (_activeChar.getSiegeState() == 2) { _relation |= 0x80; }
    }

    @Override
    protected final void writeImpl() {
        writeC(0x04);

        writeD(_activeChar.getX());
        writeD(_activeChar.getY());
        writeD(_activeChar.getZ());
        writeD(_activeChar.getHeading());
        writeD(_activeChar.getObjectId());

        String name = _activeChar.getName();
        if (_activeChar.getPoly().isMorphed()) {
            NpcTemplate polyObj = NpcTable.getInstance().getTemplate(_activeChar.getPoly().getPolyId());
            if (polyObj != null) { name = polyObj.getName(); }
        }
        writeS(name);

        writeD(_activeChar.getRace().ordinal());
        writeD(_activeChar.getAppearance().isFemale() ? 1 : 0);

        if (_activeChar.getClassIndex() == 0) { writeD(_activeChar.getClassId().getId()); }
        else { writeD(_activeChar.getBaseClass()); }

        writeD(_activeChar.getLevel());
        writeQ(_activeChar.getStat().getExp());
        writeD(_activeChar.getSTR());
        writeD(_activeChar.getDEX());
        writeD(_activeChar.getCON());
        writeD(_activeChar.getINT());
        writeD(_activeChar.getWIT());
        writeD(_activeChar.getMEN());
        writeD(_activeChar.getMaxHp());
        writeD((int) _activeChar.getCurrentHp());
        writeD(_activeChar.getMaxMp());
        writeD((int) _activeChar.getCurrentMp());
        writeD(_activeChar.getStat().getSp());
        writeD(_activeChar.getCurrentLoad());
        writeD(_activeChar.getMaxLoad());

        writeD(_activeChar.getActiveWeaponItem() != null ? 40 : 20); // 20 no weapon, 40 weapon equipped

        writeD(_activeChar.getInventory().getPaperdollObjectId(EPaperdollSlot.PAPERDOLL_HAIRALL));
        writeD(_activeChar.getInventory().getPaperdollObjectId(EPaperdollSlot.PAPERDOLL_REAR));
        writeD(_activeChar.getInventory().getPaperdollObjectId(EPaperdollSlot.PAPERDOLL_LEAR));
        writeD(_activeChar.getInventory().getPaperdollObjectId(EPaperdollSlot.PAPERDOLL_NECK));
        writeD(_activeChar.getInventory().getPaperdollObjectId(EPaperdollSlot.PAPERDOLL_RFINGER));
        writeD(_activeChar.getInventory().getPaperdollObjectId(EPaperdollSlot.PAPERDOLL_LFINGER));
        writeD(_activeChar.getInventory().getPaperdollObjectId(EPaperdollSlot.PAPERDOLL_HEAD));
        writeD(_activeChar.getInventory().getPaperdollObjectId(EPaperdollSlot.PAPERDOLL_RHAND));
        writeD(_activeChar.getInventory().getPaperdollObjectId(EPaperdollSlot.PAPERDOLL_LHAND));
        writeD(_activeChar.getInventory().getPaperdollObjectId(EPaperdollSlot.PAPERDOLL_GLOVES));
        writeD(_activeChar.getInventory().getPaperdollObjectId(EPaperdollSlot.PAPERDOLL_CHEST));
        writeD(_activeChar.getInventory().getPaperdollObjectId(EPaperdollSlot.PAPERDOLL_LEGS));
        writeD(_activeChar.getInventory().getPaperdollObjectId(EPaperdollSlot.PAPERDOLL_FEET));
        writeD(_activeChar.getInventory().getPaperdollObjectId(EPaperdollSlot.PAPERDOLL_BACK));
        writeD(_activeChar.getInventory().getPaperdollObjectId(EPaperdollSlot.PAPERDOLL_RHAND));
        writeD(_activeChar.getInventory().getPaperdollObjectId(EPaperdollSlot.PAPERDOLL_HAIR));
        writeD(_activeChar.getInventory().getPaperdollObjectId(EPaperdollSlot.PAPERDOLL_FACE));

        writeD(_activeChar.getInventory().getPaperdollItemId(EPaperdollSlot.PAPERDOLL_HAIRALL));
        writeD(_activeChar.getInventory().getPaperdollItemId(EPaperdollSlot.PAPERDOLL_REAR));
        writeD(_activeChar.getInventory().getPaperdollItemId(EPaperdollSlot.PAPERDOLL_LEAR));
        writeD(_activeChar.getInventory().getPaperdollItemId(EPaperdollSlot.PAPERDOLL_NECK));
        writeD(_activeChar.getInventory().getPaperdollItemId(EPaperdollSlot.PAPERDOLL_RFINGER));
        writeD(_activeChar.getInventory().getPaperdollItemId(EPaperdollSlot.PAPERDOLL_LFINGER));
        writeD(_activeChar.getInventory().getPaperdollItemId(EPaperdollSlot.PAPERDOLL_HEAD));
        writeD(_activeChar.getInventory().getPaperdollItemId(EPaperdollSlot.PAPERDOLL_RHAND));
        writeD(_activeChar.getInventory().getPaperdollItemId(EPaperdollSlot.PAPERDOLL_LHAND));
        writeD(_activeChar.getInventory().getPaperdollItemId(EPaperdollSlot.PAPERDOLL_GLOVES));
        writeD(_activeChar.getInventory().getPaperdollItemId(EPaperdollSlot.PAPERDOLL_CHEST));
        writeD(_activeChar.getInventory().getPaperdollItemId(EPaperdollSlot.PAPERDOLL_LEGS));
        writeD(_activeChar.getInventory().getPaperdollItemId(EPaperdollSlot.PAPERDOLL_FEET));
        writeD(_activeChar.getInventory().getPaperdollItemId(EPaperdollSlot.PAPERDOLL_BACK));
        writeD(_activeChar.getInventory().getPaperdollItemId(EPaperdollSlot.PAPERDOLL_RHAND));
        writeD(_activeChar.getInventory().getPaperdollItemId(EPaperdollSlot.PAPERDOLL_HAIR));
        writeD(_activeChar.getInventory().getPaperdollItemId(EPaperdollSlot.PAPERDOLL_FACE));

        // c6 new h's
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
        writeH(0x00);
        writeH(0x00);
        writeD(_activeChar.getInventory().getPaperdollAugmentationId(EPaperdollSlot.PAPERDOLL_RHAND));
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
        writeD(_activeChar.getInventory().getPaperdollAugmentationId(EPaperdollSlot.PAPERDOLL_LHAND));
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        // end of c6 new h's

        writeD(_activeChar.getPAtk(null));
        writeD(_activeChar.getPAtkSpd());
        writeD(_activeChar.getPDef(null));
        writeD(_activeChar.getEvasionRate(null));
        writeD(_activeChar.getAccuracy());
        writeD(_activeChar.getCriticalHit(null, null));
        writeD(_activeChar.getMAtk(null, null));

        writeD(_activeChar.getMAtkSpd());
        writeD(_activeChar.getPAtkSpd());

        writeD(_activeChar.getMDef(null, null));

        writeD(_activeChar.getPvpFlag()); // 0-non-pvp 1-pvp = violett name
        writeD(_activeChar.getKarma());

        writeD(_runSpd);
        writeD(_walkSpd);
        writeD(_runSpd); // swim run speed
        writeD(_walkSpd); // swim walk speed
        writeD(0);
        writeD(0);
        writeD(_activeChar.isFlying() ? _runSpd : 0); // fly speed
        writeD(_activeChar.isFlying() ? _walkSpd : 0); // fly speed
        writeF(_moveMultiplier);
        writeF(_activeChar.getAttackSpeedMultiplier());

        L2Summon pet = _activeChar.getPet();
        if (_activeChar.getMountType() != 0 && pet != null) {
            writeF(pet.getTemplate().getCollisionRadius());
            writeF(pet.getTemplate().getCollisionHeight());
        }
        else {
            writeF(_activeChar.getBaseTemplate().getCollisionRadius());
            writeF(_activeChar.getBaseTemplate().getCollisionHeight());
        }

        writeD(_activeChar.getAppearance().getHairStyle());
        writeD(_activeChar.getAppearance().getHairColor());
        writeD(_activeChar.getAppearance().getFace());
        writeD(_activeChar.isGM() ? 1 : 0); // builder level

        writeS(_activeChar.getPoly().isMorphed() ? "Morphed" : _activeChar.getTitle());

        writeD(_activeChar.getClanId());
        writeD(_activeChar.getClanCrestId());
        writeD(_activeChar.getAllyId());
        writeD(_activeChar.getAllyCrestId()); // ally crest id
        // 0x40 leader rights
        // siege flags: attacker - 0x180 sword over name, defender - 0x80 shield, 0xC0 crown (|leader), 0x1C0 flag (|leader)
        writeD(_relation);
        writeC(_activeChar.getMountType()); // mount type
        writeC(_activeChar.getPrivateStoreType().getId());
        writeC(_activeChar.getRecipeController().hasDwarvenCraft() ? 1 : 0);
        writeD(_activeChar.getPkKills());
        writeD(_activeChar.getPvpKills());

        writeH(_activeChar.getCubics().size());
        for (int id : _activeChar.getCubics().keySet()) { writeH(id); }

        writeC(_activeChar.isInPartyMatchRoom() ? 1 : 0);

        if (_activeChar.isInvisible() && _activeChar.isGM()) { writeD(_activeChar.getAbnormalEffect() | AbnormalEffect.STEALTH.getMask()); }
        else { writeD(_activeChar.getAbnormalEffect()); }
        writeC(0x00);

        writeD(_activeChar.getClanPrivileges());

        writeH(_activeChar.getAppearance().getRecomLeft()); // c2 recommendations remaining
        writeH(_activeChar.getAppearance().getRecomHave()); // c2 recommendations received
        writeD(_activeChar.getMountNpcId() > 0 ? _activeChar.getMountNpcId() + 1000000 : 0);
        writeH(_activeChar.getInventoryLimit());

        writeD(_activeChar.getClassId().getId());
        writeD(0x00); // special effects? circles around player...
        writeD(_activeChar.getMaxCp());
        writeD((int) _activeChar.getCurrentCp());
        writeC(_activeChar.isMounted() ? 0 : _activeChar.getEnchantEffect());

        if (_activeChar.getTeam() == 1 || (Config.PLAYER_SPAWN_PROTECTION > 0 && _activeChar.isSpawnProtected())) {
            writeC(0x01); // team circle around feet 1= Blue, 2 = red
        }
        else if (_activeChar.getTeam() == 2) {
            writeC(0x02); // team circle around feet 1= Blue, 2 = red
        }
        else {
            writeC(0x00); // team circle around feet 1= Blue, 2 = red
        }

        writeD(_activeChar.getClanCrestLargeId());
        writeC(_activeChar.isNoble() ? 1 : 0); // 0x01: symbol on char menu ctrl+I
        writeC(_activeChar.isHero() || (_activeChar.isGM() && Config.GM_HERO_AURA) ? 1 : 0); // 0x01: Hero Aura

        writeC(_activeChar.isFishing() ? 1 : 0); // Fishing Mode

        Location loc = _activeChar.getFishingLoc();
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

        writeD(_activeChar.getAppearance().getNameColor());

        // new c5
        writeC(_activeChar.isRunning() ? 0x01 : 0x00); // changes the Speed display on Status Window

        writeD(_activeChar.getPledgeClass()); // changes the text above CP on Status Window
        writeD(_activeChar.getPledgeType());

        writeD(_activeChar.getAppearance().getTitleColor());

        if (_activeChar.isCursedWeaponEquipped()) { writeD(CursedWeaponsManager.getInstance().getCurrentStage(_activeChar.getCursedWeaponEquippedId()) - 1); }
        else { writeD(0x00); }
    }
}