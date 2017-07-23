package net.sf.l2j.gameserver.playerpart;

/**
 * @author Johnson / 19.07.2017
 */
public enum PrivateStoreType {
    NONE(0),
    SELL(1),
    SELL_MANAGE(2),
    BUY(3),
    BUY_MANAGE(4),
    MANUFACTURE(5),
    PACKAGE_SELL(8);

    private final int _id;

    PrivateStoreType(int id) {
        _id = id;
    }

    public int getId() {
        return _id;
    }
}
