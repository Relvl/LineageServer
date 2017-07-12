package org.mmocore.network;

import java.nio.ByteBuffer;

public abstract class AbstractPacket<T extends MMOClient<?>> {
    protected ByteBuffer buffer;
    protected T client;

    public T getClient() {
        return client;
    }
}
