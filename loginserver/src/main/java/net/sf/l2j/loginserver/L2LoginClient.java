/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.loginserver;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.loginserver.crypt.LoginCrypt;
import net.sf.l2j.loginserver.crypt.ScrambledKeyPair;
import net.sf.l2j.loginserver.network.serverpackets.L2LoginServerPacket;
import net.sf.l2j.loginserver.network.serverpackets.LoginFail;
import net.sf.l2j.loginserver.network.serverpackets.LoginFail.LoginFailReason;
import net.sf.l2j.loginserver.network.serverpackets.PlayFail;
import net.sf.l2j.loginserver.network.serverpackets.PlayFail.PlayFailReason;
import org.mmocore.network.MMOClient;
import org.mmocore.network.MMOConnection;
import org.mmocore.network.SendablePacket;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.security.interfaces.RSAPrivateKey;
import java.util.logging.Logger;

/**
 * Represents a client connected into the LoginServer
 *
 * @author KenM
 */
public final class L2LoginClient extends MMOClient<MMOConnection<L2LoginClient>> {
    private static Logger _log = Logger.getLogger(L2LoginClient.class.getName());
    // Crypt
    private final LoginCrypt _loginCrypt;
    private final ScrambledKeyPair _scrambledPair;
    private final byte[] _blowfishKey;
    private final int _sessionId;
    private final long _connectionStartTime;
    private LoginClientState _state;
    private String _account;
    private int _accessLevel;
    private int _lastServer;
    private boolean _usesInternalIP;
    private SessionKey _sessionKey;
    private boolean _joinedGS;

    /**
     * @param con
     */
    public L2LoginClient(MMOConnection<L2LoginClient> con) {
        super(con);
        _state = LoginClientState.CONNECTED;
        String ip = getConnection().getInetAddress().getHostAddress();

        // TODO unhardcode this
        if (ip.startsWith("192.168") || ip.startsWith("10.0") || ip.equals("127.0.0.1")) {
            _usesInternalIP = true;
        }

        _scrambledPair = LoginController.getInstance().getScrambledRSAKeyPair();
        _blowfishKey = LoginController.getInstance().getBlowfishKey();
        _sessionId = Rnd.nextInt();
        _connectionStartTime = System.currentTimeMillis();
        _loginCrypt = new LoginCrypt();
        _loginCrypt.setKey(_blowfishKey);
    }

    public boolean usesInternalIP() {
        return _usesInternalIP;
    }

    @Override
    public boolean decrypt(ByteBuffer buf, int size) {
        boolean ret = false;
        try {
            ret = _loginCrypt.decrypt(buf.array(), buf.position(), size);
        } catch (IOException e) {
            e.printStackTrace();
            super.getConnection().close((SendablePacket<L2LoginClient>) null);
            return false;
        }

        if (!ret) {
            byte[] dump = new byte[size];
            System.arraycopy(buf.array(), buf.position(), dump, 0, size);
            _log.warning("Wrong checksum from client: " + toString());
            super.getConnection().close((SendablePacket<L2LoginClient>) null);
        }

        return ret;
    }

    @Override
    public boolean encrypt(ByteBuffer buf, int size) {
        final int offset = buf.position();
        try {
            size = _loginCrypt.encrypt(buf.array(), offset, size);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        buf.position(offset + size);
        return true;
    }

    public LoginClientState getState() {
        return _state;
    }

    public void setState(LoginClientState state) {
        _state = state;
    }

    public byte[] getBlowfishKey() {
        return _blowfishKey;
    }

    public byte[] getScrambledModulus() {
        return _scrambledPair.getScrambledModulus();
    }

    public RSAPrivateKey getRSAPrivateKey() {
        return (RSAPrivateKey) _scrambledPair.getKeyPair().getPrivate();
    }

    public String getAccount() {
        return _account;
    }

    public void setAccount(String account) {
        _account = account;
    }

    public int getAccessLevel() {
        return _accessLevel;
    }

    public void setAccessLevel(int accessLevel) {
        _accessLevel = accessLevel;
    }

    public int getLastServer() {
        return _lastServer;
    }

    public void setLastServer(int lastServer) {
        _lastServer = lastServer;
    }

    public int getSessionId() {
        return _sessionId;
    }

    public boolean hasJoinedGS() {
        return _joinedGS;
    }

    public void setJoinedGS(boolean val) {
        _joinedGS = val;
    }

    public SessionKey getSessionKey() {
        return _sessionKey;
    }

    public void setSessionKey(SessionKey sessionKey) {
        _sessionKey = sessionKey;
    }

    public long getConnectionStartTime() {
        return _connectionStartTime;
    }

    public void sendPacket(L2LoginServerPacket lsp) {
        getConnection().sendPacket(lsp);
    }

    public void close(LoginFailReason reason) {
        getConnection().close(new LoginFail(reason));
    }

    public void close(PlayFailReason reason) {
        getConnection().close(new PlayFail(reason));
    }

    public void close(L2LoginServerPacket lsp) {
        getConnection().close(lsp);
    }

    @Override
    public void onDisconnection() {
        if (Config.DEBUG)
            _log.info("DISCONNECTED: " + toString());

        if (!hasJoinedGS() || (getConnectionStartTime() + LoginController.LOGIN_TIMEOUT) < System.currentTimeMillis())
            LoginController.getInstance().removeAuthedLoginClient(getAccount());
    }

    @Override
    public String toString() {
        InetAddress address = getConnection().getInetAddress();
        if (getState() == LoginClientState.AUTHED_LOGIN) {
            return "[" + getAccount() + " (" + (address == null ? "disconnected" : address.getHostAddress()) + ")]";
        }
        return "[" + (address == null ? "disconnected" : address.getHostAddress()) + "]";
    }

    @Override
    protected void onForcedDisconnection() {
        // Empty
    }

    public static enum LoginClientState {
        CONNECTED,
        AUTHED_GG,
        AUTHED_LOGIN
    }
}
