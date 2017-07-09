package org.mmocore.network;

@FunctionalInterface
public interface IMMOExecutor<T extends MMOClient<?>> {
    void execute(ReceivablePacket<T> packet);
}
