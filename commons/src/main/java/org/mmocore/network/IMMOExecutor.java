package org.mmocore.network;

public interface IMMOExecutor<T extends MMOClient<?>> {
    void execute(ReceivablePacket<T> packet);
}
