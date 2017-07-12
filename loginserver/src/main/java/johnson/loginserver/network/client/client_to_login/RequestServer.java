package johnson.loginserver.network.client.client_to_login;

import johnson.loginserver.GameServerInfo;
import johnson.loginserver.GameServerTable;
import johnson.loginserver.L2LoginClient;
import johnson.loginserver.database.SetAccountLastServerCall;
import johnson.loginserver.network.client.login_to_client.LoginFail.LoginFailReason;
import johnson.loginserver.network.client.login_to_client.PlayFail.PlayFailReason;
import johnson.loginserver.network.client.login_to_client.PlayOk;
import net.sf.l2j.commons.EServerStatus;
import net.sf.l2j.commons.database.CallException;
import org.mmocore.network.ReceivablePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestServer extends ReceivablePacket<L2LoginClient> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestServer.class);

    private int sKey1;
    private int sKey2;
    private int serverId;

    @Override
    public boolean read() {
        if (_buf.remaining() >= 9) {
            sKey1 = readD();
            sKey2 = readD();
            serverId = readC();
            return true;
        }
        return false;
    }

    @Override
    public void run() {
        // Сессия не авторизована.
        if (!getClient().getSessionKey().checkLoginPair(sKey1, sKey2)) {
            System.out.println(">>> 1");
            getClient().close(LoginFailReason.REASON_ACCESS_FAILED);
            return;
        }

        GameServerInfo gsi = GameServerTable.getInstance().getGameServer(serverId);

        // Указанного сервера нет, либо он не прошел авторизацию.
        if (gsi == null || !gsi.isAuthed()) {
            System.out.println(">>> 2");
            getClient().close(LoginFailReason.REASON_ACCESS_FAILED);
            return;
        }

        // Слишком много игроков - войти могут только ГМы и премаки.
        if (gsi.getCurrentPlayerCount() >= gsi.getMaxPlayers()) {
            getClient().close(PlayFailReason.REASON_TOO_MANY_PLAYERS);
            return;
        }

        // Сервер запущен только для ГМов. Войти могут только ГМы и тестеры.
        if (gsi.getStatus() == EServerStatus.STATUS_GM_ONLY) { // TODO! and client is not GM
            getClient().close(PlayFailReason.REASON3); // TODO! Но это не точно!
            return;
        }

        // Сохраняем последний посещенный сервер.
        if (getClient().getLastServer() != serverId) {
            try (SetAccountLastServerCall call = new SetAccountLastServerCall(getClient().getAccount(), serverId)) {
                call.execute();
            } catch (CallException e) {
                LOGGER.warn("Could not set lastServer: {}", e.getMessage(), e);
            }
        }

        getClient().setJoinedGS(true);
        getClient().sendPacket(new PlayOk(getClient().getSessionKey()));
    }

    @Override
    public String toString() {
        return "RequestServer{" +
                "sKey1=" + sKey1 +
                ", sKey2=" + sKey2 +
                ", serverId=" + serverId +
                '}';
    }
}
