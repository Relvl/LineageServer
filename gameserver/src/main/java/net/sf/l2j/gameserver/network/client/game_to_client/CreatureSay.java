package net.sf.l2j.gameserver.network.client.game_to_client;

import net.sf.l2j.gameserver.EChatType;
import net.sf.l2j.gameserver.network.SystemMessageId;

import java.util.ArrayList;
import java.util.List;

public class CreatureSay extends L2GameServerPacket {
    private final int objectId;
    private final EChatType chatType;
    private String charName;
    private int charId;
    private String text;
    private int npcString = -1;
    private List<String> parameters;

    public CreatureSay(int objectId, EChatType chatType, String charName, String text) {
        this.objectId = objectId;
        this.chatType = chatType;
        this.charName = charName;
        this.text = text;
    }

    public CreatureSay(int objectId, EChatType chatType, int charId, SystemMessageId sysString) {
        this.objectId = objectId;
        this.chatType = chatType;
        this.charId = charId;
        this.npcString = sysString.getId();
    }

    public void addStringParameter(String param) {
        if (parameters == null) {
            parameters = new ArrayList<>();
        }
        parameters.add(param);
    }

    @Override
    protected void writeImpl() {
        writeC(0x4A);
        writeD(objectId);
        writeD(chatType.getCode());
        if (charName != null) { writeS(charName); }
        else { writeD(charId); }
        writeD(npcString); // High Five NPCString ID
        if (text != null) { writeS(text); }
        else {
            if (parameters != null) {
                for (String s : parameters) { writeS(s); }
            }
        }
    }

    @Override
    public String toString() {
        return "CreatureSay{" +
                "objectId=" + objectId +
                ", chatType=" + chatType +
                ", charName='" + charName + '\'' +
                ", charId=" + charId +
                ", text='" + text + '\'' +
                ", npcString=" + npcString +
                ", parameters=" + parameters +
                '}';
    }
}