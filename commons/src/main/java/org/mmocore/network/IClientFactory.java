package org.mmocore.network;

@FunctionalInterface
public interface IClientFactory<T extends MMOClient<?>> {
    T create(MMOConnection<T> connection);
}
