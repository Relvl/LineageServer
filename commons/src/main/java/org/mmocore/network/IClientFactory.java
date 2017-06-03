package org.mmocore.network;

/**  */
public interface IClientFactory<T extends MMOClient<?>> {
    /**  */
    T create(final MMOConnection<T> connection);
}
