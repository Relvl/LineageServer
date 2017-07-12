package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.gameserver.datatables.CharNameTable;
import net.sf.l2j.gameserver.model.CharSelectInfoPackage;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.GameClientState;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.client.game_to_client.CharSelected;
import net.sf.l2j.gameserver.network.client.game_to_client.SignsSky;
import net.sf.l2j.gameserver.util.FloodProtectors;
import net.sf.l2j.gameserver.util.FloodProtectors.Action;

public class CharacterSelected extends L2GameClientPacket {
    private int charSlot;

    @Override
    protected void readImpl() {
        charSlot = readD();
        readH();
        readD();
        readD();
        readD();
    }

    @Override
    protected void runImpl() {
        L2GameClient client = getClient();
        if (!FloodProtectors.performAction(client, Action.CHARACTER_SELECT)) { return; }

        // we should always be able to acquire the lock
        // but if we cant lock then nothing should be done (ie repeated packet)
        if (client.getActiveCharLock().tryLock()) {
            try {
                // should always be null
                // but if not then this is repeated packet and nothing should be done here
                if (client.getActiveChar() == null) {
                    CharSelectInfoPackage info = client.getCharSelection(charSlot);
                    if (info == null) { return; }

                    // Selected character is banned. Acts like if nothing occured...
                    if (info.getAccessLevel() < 0) { return; }

                    // Load up character from disk
                    L2PcInstance cha = client.loadCharFromDisk(charSlot);
                    if (cha == null) { return; }

                    CharNameTable.getInstance().addName(cha);

                    cha.setClient(client);
                    client.setActiveChar(cha);
                    cha.setOnlineStatus(true, true);

                    sendPacket(new SignsSky());

                    client.setState(GameClientState.IN_GAME);
                    CharSelected cs = new CharSelected(cha, client.getSessionId().playOkID1);
                    sendPacket(cs);
                }
            } finally {
                client.getActiveCharLock().unlock();
            }
        }
    }
}