package net.sf.l2j.gameserver.network.client.game_to_client;

public class PetInventoryUpdate extends InventoryUpdate {
    @Override
    protected int getPacketId() { return 0xb3; }

    @Override
    protected boolean isPlayerPacket() { return false; }
}