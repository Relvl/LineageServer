package net.sf.l2j.gameserver.network.client.game_to_client.gm;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.item.EPaperdollSlot;
import net.sf.l2j.gameserver.network.client.game_to_client.L2GameServerPacket;

public class GMViewCharacterInfo extends L2GameServerPacket {
    private final L2PcInstance player;

    public GMViewCharacterInfo(L2PcInstance player) {
        this.player = player;
    }

    @Override
    protected final void writeImpl() {
        float moveMultiplier = player.getMovementSpeedMultiplier();
        int _runSpd = (int) (player.getRunSpeed() / moveMultiplier);
        int _walkSpd = (int) (player.getWalkSpeed() / moveMultiplier);

        writeC(0x8f);

        writeD(player.getX());
        writeD(player.getY());
        writeD(player.getZ());
        writeD(player.getHeading());
        writeD(player.getObjectId());
        writeS(player.getName());
        writeD(player.getRace().ordinal());
        writeD(player.getAppearance().isFemale() ? 1 : 0);
        writeD(player.getClassId().getId());
        writeD(player.getLevel());
        writeQ(player.getStat().getExp());
        writeD(player.getSTR());
        writeD(player.getDEX());
        writeD(player.getCON());
        writeD(player.getINT());
        writeD(player.getWIT());
        writeD(player.getMEN());
        writeD(player.getMaxHp());
        writeD((int) player.getCurrentHp());
        writeD(player.getMaxMp());
        writeD((int) player.getCurrentMp());
        writeD(player.getStat().getSp());
        writeD(player.getCurrentLoad());
        writeD(player.getMaxLoad());
        writeD(0x28); // unknown

        writeD(player.getInventory().getPaperdollObjectId(EPaperdollSlot.PAPERDOLL_HAIRALL));
        writeD(player.getInventory().getPaperdollObjectId(EPaperdollSlot.PAPERDOLL_REAR));
        writeD(player.getInventory().getPaperdollObjectId(EPaperdollSlot.PAPERDOLL_LEAR));
        writeD(player.getInventory().getPaperdollObjectId(EPaperdollSlot.PAPERDOLL_NECK));
        writeD(player.getInventory().getPaperdollObjectId(EPaperdollSlot.PAPERDOLL_RFINGER));
        writeD(player.getInventory().getPaperdollObjectId(EPaperdollSlot.PAPERDOLL_LFINGER));
        writeD(player.getInventory().getPaperdollObjectId(EPaperdollSlot.PAPERDOLL_HEAD));
        writeD(player.getInventory().getPaperdollObjectId(EPaperdollSlot.PAPERDOLL_RHAND));
        writeD(player.getInventory().getPaperdollObjectId(EPaperdollSlot.PAPERDOLL_LHAND));
        writeD(player.getInventory().getPaperdollObjectId(EPaperdollSlot.PAPERDOLL_GLOVES));
        writeD(player.getInventory().getPaperdollObjectId(EPaperdollSlot.PAPERDOLL_CHEST));
        writeD(player.getInventory().getPaperdollObjectId(EPaperdollSlot.PAPERDOLL_LEGS));
        writeD(player.getInventory().getPaperdollObjectId(EPaperdollSlot.PAPERDOLL_FEET));
        writeD(player.getInventory().getPaperdollObjectId(EPaperdollSlot.PAPERDOLL_BACK));
        writeD(player.getInventory().getPaperdollObjectId(EPaperdollSlot.PAPERDOLL_RHAND));
        writeD(player.getInventory().getPaperdollObjectId(EPaperdollSlot.PAPERDOLL_HAIR));
        writeD(player.getInventory().getPaperdollObjectId(EPaperdollSlot.PAPERDOLL_FACE));

        writeD(player.getInventory().getPaperdollItemId(EPaperdollSlot.PAPERDOLL_HAIRALL));
        writeD(player.getInventory().getPaperdollItemId(EPaperdollSlot.PAPERDOLL_REAR));
        writeD(player.getInventory().getPaperdollItemId(EPaperdollSlot.PAPERDOLL_LEAR));
        writeD(player.getInventory().getPaperdollItemId(EPaperdollSlot.PAPERDOLL_NECK));
        writeD(player.getInventory().getPaperdollItemId(EPaperdollSlot.PAPERDOLL_RFINGER));
        writeD(player.getInventory().getPaperdollItemId(EPaperdollSlot.PAPERDOLL_LFINGER));
        writeD(player.getInventory().getPaperdollItemId(EPaperdollSlot.PAPERDOLL_HEAD));
        writeD(player.getInventory().getPaperdollItemId(EPaperdollSlot.PAPERDOLL_RHAND));
        writeD(player.getInventory().getPaperdollItemId(EPaperdollSlot.PAPERDOLL_LHAND));
        writeD(player.getInventory().getPaperdollItemId(EPaperdollSlot.PAPERDOLL_GLOVES));
        writeD(player.getInventory().getPaperdollItemId(EPaperdollSlot.PAPERDOLL_CHEST));
        writeD(player.getInventory().getPaperdollItemId(EPaperdollSlot.PAPERDOLL_LEGS));
        writeD(player.getInventory().getPaperdollItemId(EPaperdollSlot.PAPERDOLL_FEET));
        writeD(player.getInventory().getPaperdollItemId(EPaperdollSlot.PAPERDOLL_BACK));
        writeD(player.getInventory().getPaperdollItemId(EPaperdollSlot.PAPERDOLL_RHAND));
        writeD(player.getInventory().getPaperdollItemId(EPaperdollSlot.PAPERDOLL_HAIR));
        writeD(player.getInventory().getPaperdollItemId(EPaperdollSlot.PAPERDOLL_FACE));

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
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        writeH(0x00);
        // end of c6 new h's

        writeD(player.getPAtk(null));
        writeD(player.getPAtkSpd());
        writeD(player.getPDef(null));
        writeD(player.getEvasionRate(null));
        writeD(player.getAccuracy());
        writeD(player.getCriticalHit(null, null));
        writeD(player.getMAtk(null, null));

        writeD(player.getMAtkSpd());
        writeD(player.getPAtkSpd());

        writeD(player.getMDef(null, null));

        writeD(player.getPvpFlag()); // 0-non-pvp 1-pvp = violett name
        writeD(player.getKarma());

        writeD(_runSpd);
        writeD(_walkSpd);
        writeD(_runSpd); // swimspeed
        writeD(_walkSpd); // swimspeed
        writeD(_runSpd);
        writeD(_walkSpd);
        writeD(_runSpd);
        writeD(_walkSpd);
        writeF(moveMultiplier);
        writeF(player.getAttackSpeedMultiplier()); // 2.9);//
        writeF(player.getTemplate().getCollisionRadius()); // scale
        writeF(player.getTemplate().getCollisionHeight()); // y offset ??!? fem dwarf 4033
        writeD(player.getAppearance().getHairStyle());
        writeD(player.getAppearance().getHairColor());
        writeD(player.getAppearance().getFace());
        writeD(player.isGM() ? 0x01 : 0x00); // builder level

        writeS(player.getTitle());
        writeD(player.getClanId()); // pledge id
        writeD(player.getClanCrestId()); // pledge crest id
        writeD(player.getAllyId()); // ally id
        writeC(player.getMountType()); // mount type
        writeC(player.getPrivateStoreType().getId());
        writeC(player.getRecipeController().hasDwarvenCraft() ? 1 : 0);
        writeD(player.getPkKills());
        writeD(player.getPvpKills());

        writeH(player.getAppearance().getRecomLeft());
        writeH(player.getAppearance().getRecomHave()); // Blue value for name (0 = white, 255 = pure blue)
        writeD(player.getClassId().getId());
        writeD(0x00); // special effects? circles around player...
        writeD(player.getMaxCp());
        writeD((int) player.getCurrentCp());

        writeC(player.isRunning() ? 0x01 : 0x00); // changes the Speed display on Status Window

        writeC(321);

        writeD(player.getPledgeClass()); // changes the text above CP on Status Window

        writeC(player.isNoble() ? 0x01 : 0x00);
        writeC(player.isHero() ? 0x01 : 0x00);

        writeD(player.getAppearance().getNameColor());
        writeD(player.getAppearance().getTitleColor());
    }
}