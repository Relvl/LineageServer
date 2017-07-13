package net.sf.l2j.commons;

/**
 * This class is used to represent session keys used by the client to authenticate in the gameserver
 * A SessionKey is made up of two 8 bytes keys. One is send in the {LoginOk} packet and the other is sent in {PlayOk}
 */
public class SessionKey {
    private final int loginOkID1;
    private final int loginOkID2;
    private final int playOkID1;
    private final int playOkID2;

    public SessionKey(int loginOK1, int loginOK2, int playOK1, int playOK2) {
        playOkID1 = playOK1;
        playOkID2 = playOK2;
        loginOkID1 = loginOK1;
        loginOkID2 = loginOK2;
    }

    @Override
    public String toString() {
        return "SessionKey{" +
                "loginOkID1=" + loginOkID1 +
                ", loginOkID2=" + loginOkID2 +
                ", playOkID1=" + playOkID1 +
                ", playOkID2=" + playOkID2 +
                '}';
    }

    public boolean checkLoginPair(int loginOk1, int loginOk2) {
        return loginOkID1 == loginOk1 && loginOkID2 == loginOk2;
    }

    /**
     * Only checks the PlayOk part of the session key if server doesnt show the licence when player logs in.
     */
    public boolean equals(SessionKey key) {
        // when server doesnt show licence it deosnt send the {@link LoginOk LoginOk} packet, client doesnt have this part of the key then.
        return playOkID1 == key.playOkID1 && loginOkID1 == key.loginOkID1 && playOkID2 == key.playOkID2 && loginOkID2 == key.loginOkID2;
    }

    /** Первый ключ из пакета {LoginOk} */
    public int getLoginOkID1() {
        return loginOkID1;
    }

    /** Второй ключ из пакета {LoginOk} */
    public int getLoginOkID2() {
        return loginOkID2;
    }

    /** Первый ключ из пакета {PlayOk} */
    public int getPlayOkID1() {
        return playOkID1;
    }

    /** Второй ключ из пакета {PlayOk} */
    public int getPlayOkID2() {
        return playOkID2;
    }
}