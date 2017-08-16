package net.sf.l2j.gameserver.network.client.game_to_client;

public class PlaySound extends L2GameServerPacket {
    private final String soundFile;
    private final int type;
    private final EType eType;
    private final int hasCenterObject;
    private final int objectId;
    private final int posX;
    private final int posY;
    private final int posZ;

    private ESound sound;

    public enum EType {SOUND, MUSIC, VOICE}

    public enum ESound {
        itemsound_ship_1min("itemsound.ship_1min", EType.SOUND),
        itemsound_ship_5min("itemsound.ship_5min", EType.SOUND),
        itemsound_ship_arrival_departure("itemsound.ship_arrival_departure", EType.SOUND),

        ItemSound_quest_accept("ItemSound.quest_accept", EType.SOUND),
        ItemSound_quest_finish("ItemSound.quest_finish", EType.SOUND),
        ItemSound_quest_itemget("ItemSound.quest_itemget", EType.SOUND),
        ItemSound_quest_middle("ItemSound.quest_middle", EType.SOUND),
        ItemSound_quest_jackpot("ItemSound.quest_jackpot", EType.SOUND),
        ItemSound_quest_giveup("ItemSound.quest_giveup", EType.SOUND),
        ItemSound_quest_fanfare_2("ItemSound.quest_fanfare_2", EType.SOUND),
        Itemsound_quest_before_battle("Itemsound.quest_before_battle", EType.SOUND),
        ItemSound2_race_start("ItemSound2.race_start", EType.SOUND),

        EtcSound_elcroki_song_full("EtcSound.elcroki_song_full", EType.SOUND),

        systemmsg_e_17("systemmsg_e.17", EType.SOUND),
        systemmsg_e_18("systemmsg_e.18", EType.SOUND),
        systemmsg_e_345("systemmsg_e.345", EType.SOUND),
        systemmsg_e_346("systemmsg_e.346", EType.SOUND),
        systemmsg_e_702("systemmsg_e.702", EType.SOUND),
        systemmsg_e_809("systemmsg_e.809", EType.SOUND),
        systemmsg_e_1209("systemmsg_e.1209", EType.SOUND),

        RM_03_A("Rm03_A", EType.MUSIC),
        BS01_D("BS01_D", EType.MUSIC),
        BS01_A("BS01_A", EType.MUSIC),
        BS02_D("BS02_D", EType.MUSIC),
        B03_A("B03_A", EType.MUSIC),
        B03_D("B03_D", EType.MUSIC),
        B04_S01("B04_S01", EType.MUSIC),
        SF_P_01("SF_P_01", EType.MUSIC),
        SF_S_01("SF_S_01", EType.MUSIC),

        S_RACE("S_Race", EType.MUSIC),
        SIEGE_VICTORY("Siege_Victory", EType.MUSIC);

        private final String sound;
        private final EType type;

        ESound(String sound, EType type) {
            this.sound = sound;
            this.type = type;
        }
    }

    public PlaySound(ESound sound) {
        this.sound = sound;
        eType = null;
        soundFile = null;
        type = 0;
        hasCenterObject = 0;
        objectId = 0;
        posX = 0;
        posY = 0;
        posZ = 0;
    }

    public PlaySound(ESound sound, int objectId, int posX, int posY, int posZ) {
        this.sound = sound;
        this.hasCenterObject = 1;
        this.objectId = objectId;
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        soundFile = null;
        type = 0;
        eType = null;
    }

    @Deprecated
    public PlaySound(String soundFile) {
        type = 0;
        this.soundFile = soundFile;
        hasCenterObject = 0;
        objectId = 0;
        posX = 0;
        posY = 0;
        posZ = 0;
        eType = null;
    }

    @Deprecated
    public PlaySound(int type, String soundFile, int hasCenterObject, int objectId, int posX, int posY, int posZ) {
        this.type = type;
        this.soundFile = soundFile;
        this.hasCenterObject = hasCenterObject;
        this.objectId = objectId;
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        eType = null;
    }

    @Override
    protected final void writeImpl() {
        writeC(0x98);
        writeD(type); // 0 for quest and ship;
        writeS(soundFile);
        writeD(hasCenterObject); // 0 for quest; 1 for ship;
        writeD(objectId); // 0 for quest; objectId of ship
        writeD(posX); // x
        writeD(posY); // y
        writeD(posZ); // z
        writeD(0);
    }
}