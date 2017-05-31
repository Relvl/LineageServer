package net.sf.l2j.loginserver;

import net.sf.l2j.loginserver.network.serverpackets.Init;
import net.sf.l2j.util.IPv4Filter;
import org.mmocore.network.*;

import java.nio.channels.SocketChannel;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SelectorHelper implements IMMOExecutor<L2LoginClient>, IClientFactory<L2LoginClient>, IAcceptFilter {
    private final ThreadPoolExecutor _generalPacketsThreadPool;

    private final IPv4Filter _ipv4filter;

    public SelectorHelper() {
        _generalPacketsThreadPool = new ThreadPoolExecutor(4, 6, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        _ipv4filter = new IPv4Filter();
    }

    @Override
    public void execute(ReceivablePacket<L2LoginClient> packet) {
        _generalPacketsThreadPool.execute(packet);
    }

    @Override
    public L2LoginClient create(MMOConnection<L2LoginClient> con) {
        L2LoginClient client = new L2LoginClient(con);
        client.sendPacket(new Init(client));
        return client;
    }

    @Override
    public boolean accept(SocketChannel sc) {
        return _ipv4filter.accept(sc) && !LoginController.getInstance().isBannedAddress(sc.socket().getInetAddress());
    }
}