package org.mmocore.network;

import java.nio.ByteBuffer;

public interface IPacketHandler<T extends MMOClient<?>> {
    ReceivablePacket<T> handlePacket(ByteBuffer buf, T client);
}
