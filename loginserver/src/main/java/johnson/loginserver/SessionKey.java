package johnson.loginserver;

import johnson.loginserver.network.client.login_to_client.LoginOk;
import johnson.loginserver.network.client.login_to_client.PlayOk;

/**
 * This class is used to represent session keys used by the client to authenticate in the gameserver
 * A SessionKey is made up of two 8 bytes keys. One is send in the {@link LoginOk LoginOk} packet and the other is sent in {@link PlayOk PlayOk}
 */
public class SessionKey {
    /** Первый ключ из пакета {@link LoginOk LoginOk} */
    public int loginOkID1;
    /** Второй ключ из пакета {@link LoginOk LoginOk} */
    public int loginOkID2;

    /** Первый ключ из пакета {@link PlayOk PlayOk} */
    public int playOkID1;
    /** Второй ключ из пакета {@link PlayOk PlayOk} */
    public int playOkID2;

    public SessionKey(int loginOK1, int loginOK2, int playOK1, int playOK2) {
        playOkID1 = playOK1;
        playOkID2 = playOK2;
        loginOkID1 = loginOK1;
        loginOkID2 = loginOK2;
    }

    @Override
    public String toString() {
        return "PlayOk: " + playOkID1 + " " + playOkID2 + " LoginOk:" + loginOkID1 + " " + loginOkID2;
    }

    public boolean checkLoginPair(int loginOk1, int loginOk2) {
        return loginOkID1 == loginOk1 && loginOkID2 == loginOk2;
    }

    /**
     * Only checks the PlayOk part of the session key if server doesnt show the licence when player logs in.
     */
    public boolean equals(SessionKey key) {
        // when server doesnt show licence it deosnt send the {@link LoginOk LoginOk} packet, client doesnt have this part of the key then.
        return (playOkID1 == key.playOkID1 && loginOkID1 == key.loginOkID1 && playOkID2 == key.playOkID2 && loginOkID2 == key.loginOkID2);
    }
}