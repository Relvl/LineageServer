package johnson.loginserver;

import johnson.loginserver.crypt.LoginCrypt;
import johnson.loginserver.crypt.ScrambledKeyPair;
import johnson.loginserver.network.ABaseLoginServerPacket;
import johnson.loginserver.network.serverpackets.LoginFail;
import johnson.loginserver.network.serverpackets.LoginFail.LoginFailReason;
import johnson.loginserver.network.serverpackets.PlayFail;
import johnson.loginserver.network.serverpackets.PlayFail.PlayFailReason;
import net.sf.l2j.commons.random.Rnd;
import org.mmocore.network.MMOClient;
import org.mmocore.network.MMOConnection;
import org.mmocore.network.SendablePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.security.interfaces.RSAPrivateKey;

public final class L2LoginClient extends MMOClient<MMOConnection<L2LoginClient>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(L2LoginClient.class);

    private final LoginCrypt loginCrypt;
    private final ScrambledKeyPair scrambledPair;
    private final byte[] blowfishKey;
    private final int sessionId;
    private final long loginTimestamp;
    private ELoginClientState state;
    private String account;
    private int accessLevel;
    private int lastServer;

    @Deprecated
    private boolean isInternalIp;
    private SessionKey sessionKey;
    private boolean joinedGS;

    public L2LoginClient(MMOConnection<L2LoginClient> con) {
        super(con);
        this.state = ELoginClientState.CONNECTED;

        String ip = getConnection().getInetAddress().getHostAddress();
        // TODO unhardcode this
        if (ip.startsWith("192.168") || ip.startsWith("10.0") || ip.equals("127.0.0.1")) {
            this.isInternalIp = true;
        }

        this.scrambledPair = LoginController.getInstance().getScrambledRSAKeyPair();
        this.blowfishKey = LoginController.getInstance().getBlowfishKey();
        this.sessionId = Rnd.nextInt();
        this.loginTimestamp = System.currentTimeMillis();
        this.loginCrypt = new LoginCrypt();
        this.loginCrypt.setKey(blowfishKey);
    }

    /** @deprecated Не очень хорошая идея ходить через локал - можно чего-то не заметить. */
    @Deprecated
    public boolean usesInternalIP() {
        return isInternalIp;
    }

    @Override
    public boolean decrypt(ByteBuffer buf, int size) {
        boolean ret;
        try {
            ret = loginCrypt.decrypt(buf.array(), buf.position(), size);
        } catch (IOException e) {
            e.printStackTrace();
            super.getConnection().close((SendablePacket<L2LoginClient>) null);
            return false;
        }

        if (!ret) {
            System.arraycopy(buf.array(), buf.position(), new byte[size], 0, size);
            LOGGER.warn("Wrong checksum from client: {}", toString());
            super.getConnection().close((SendablePacket<L2LoginClient>) null);
        }

        return ret;
    }

    @Override
    public boolean encrypt(ByteBuffer buf, int size) {
        final int offset = buf.position();
        try {
            size = loginCrypt.encrypt(buf.array(), offset, size);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        buf.position(offset + size);
        return true;
    }

    public ELoginClientState getState() {
        return state;
    }

    public void setState(ELoginClientState state) {
        this.state = state;
    }

    public byte[] getBlowfishKey() {
        return blowfishKey;
    }

    public byte[] getScrambledModulus() {
        return scrambledPair.getScrambledModulus();
    }

    public RSAPrivateKey getRSAPrivateKey() {
        return (RSAPrivateKey) scrambledPair.getKeyPair().getPrivate();
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public int getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(int accessLevel) {
        this.accessLevel = accessLevel;
    }

    public int getLastServer() {
        return lastServer;
    }

    public void setLastServer(int lastServer) {
        this.lastServer = lastServer;
    }

    public int getSessionId() {
        return sessionId;
    }

    public boolean hasJoinedGS() {
        return joinedGS;
    }

    public void setJoinedGS(boolean val) {
        joinedGS = val;
    }

    public SessionKey getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(SessionKey sessionKey) {
        this.sessionKey = sessionKey;
    }

    public long getLoginTimestamp() {
        return loginTimestamp;
    }

    public L2LoginClient sendPacket(ABaseLoginServerPacket lsp) {
        getConnection().sendPacket(lsp);
        return this;
    }

    public void close(LoginFailReason reason) {
        this.close(new LoginFail(reason));
    }

    public void close(PlayFailReason reason) {
        this.close(new PlayFail(reason));
    }

    public void close(ABaseLoginServerPacket lsp) {
        getConnection().close(lsp);
    }

    @Override
    public void onDisconnection() {
        LOGGER.debug("DISCONNECTED: {}", toString());

        if (!hasJoinedGS() || (getLoginTimestamp() + LoginServer.config.clientListener.loginTimeout) < System.currentTimeMillis()) {
            LoginController.getInstance().removeClient(getAccount());
        }
    }

    @Override
    public String toString() {
        InetAddress address = getConnection().getInetAddress();
        if (getState() == ELoginClientState.AUTHED_LOGIN) {
            return "[" + getAccount() + " (" + (address == null ? "disconnected" : address.getHostAddress()) + ")]";
        }
        return "[" + (address == null ? "disconnected" : address.getHostAddress()) + "]";
    }

    @Override
    protected void onForcedDisconnection() {}

}
