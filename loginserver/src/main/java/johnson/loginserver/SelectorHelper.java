package johnson.loginserver;

import johnson.loginserver.network.serverpackets.Init;
import johnson.loginserver.security.SecurityController;
import net.sf.l2j.util.IPv4Filter;
import org.mmocore.network.*;

import java.nio.channels.SocketChannel;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SelectorHelper implements IMMOExecutor<L2LoginClient>, IClientFactory<L2LoginClient>, IAcceptFilter {
    private final ThreadPoolExecutor generalPacketsThreadPool;

    private final IPv4Filter ipv4filter;

    public SelectorHelper() {
        generalPacketsThreadPool = new ThreadPoolExecutor(4, 6, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        ipv4filter = new IPv4Filter();
    }

    @Override
    public void execute(ReceivablePacket<L2LoginClient> packet) {
        generalPacketsThreadPool.execute(packet);
    }

    @Override
    public L2LoginClient create(MMOConnection<L2LoginClient> connection) {
        L2LoginClient client = new L2LoginClient(connection);
        // Сразу после подключения ЛС должен инициировать процедуру логина, отправив Init.
        return client.sendPacket(new Init(client));
    }

    @Override
    public boolean accept(SocketChannel sc) {
        return ipv4filter.accept(sc) && !SecurityController.getInstance().isBannedAddress(sc.socket().getInetAddress());
    }
}