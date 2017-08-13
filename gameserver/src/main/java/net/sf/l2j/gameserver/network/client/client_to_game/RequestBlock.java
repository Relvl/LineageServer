package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.gameserver.datatables.CharNameTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;

public final class RequestBlock extends L2GameClientPacket {
    private static final int BLOCK = 0;
    private static final int UNBLOCK = 1;
    private static final int BLOCKLIST = 2;
    private static final int ALLBLOCK = 3;
    private static final int ALLUNBLOCK = 4;

    private String name;
    private int type;

    @Override
    protected void readImpl() {
        type = readD(); // 0x00 - block, 0x01 - unblock, 0x03 - allblock, 0x04 - allunblock
        if (type == BLOCK || type == UNBLOCK) {
            name = readS();
        }
    }

    @Override
    protected void runImpl() {
        L2PcInstance player = getClient().getActiveChar();
        if (player == null) { return; }

        switch (type) {
            case BLOCK:
            case UNBLOCK:
                int targetId = CharNameTable.getInstance().getIdByName(name);
                int targetAL = CharNameTable.getInstance().getAccessLevelById(targetId);

                // Can't block/unblock to locate invisible characters.
                if (targetId <= 0) {
                    player.sendPacket(SystemMessageId.FAILED_TO_REGISTER_TO_IGNORE_LIST);
                    return;
                }

                // Can't block a GM character.
                if (targetAL > 0) {
                    player.sendPacket(SystemMessageId.YOU_MAY_NOT_IMPOSE_A_BLOCK_ON_GM);
                    return;
                }

                if (player.getObjectId() == targetId) { return; }

                if (type == BLOCK) {
                    player.getContactController().blockPlayer(targetId);
                }
                else {
                    player.getContactController().unblockPlayer(targetId);
                }
                break;

            case BLOCKLIST:
                player.getContactController().printBlockList();
                break;

            case ALLBLOCK:
                player.getContactController().setBlockAll(true);
                break;

            case ALLUNBLOCK:
                player.getContactController().setBlockAll(false);
                break;

            default:
                _log.info("Unknown 0x0a block type: {}", type);
        }
    }
}