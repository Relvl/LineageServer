package net.sf.l2j.gameserver.model.buylist;

import net.sf.l2j.L2DatabaseFactoryOld;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.item.kind.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author KenM
 */
public class Product {
    private static final Logger LOGGER = LoggerFactory.getLogger(Product.class);

    private final int _buyListId;
    private final Item _item;
    private final int _price;
    private final long _restockDelay;
    private final int _maxCount;
    private AtomicInteger _count = null;
    private ScheduledFuture<?> _restockTask = null;

    public Product(int buyListId, Item item, int price, long restockDelay, int maxCount) {
        _buyListId = buyListId;
        _item = item;
        _price = price;
        _restockDelay = restockDelay * 60000;
        _maxCount = maxCount;

        if (hasLimitedStock()) { _count = new AtomicInteger(maxCount); }
    }

    public int getBuyListId() {
        return _buyListId;
    }

    public Item getItem() {
        return _item;
    }

    public int getItemId() {
        return _item.getItemId();
    }

    public int getPrice() {
        return _price;
    }

    public long getRestockDelay() {
        return _restockDelay;
    }

    public int getMaxCount() {
        return _maxCount;
    }

    public int getCount() {
        if (_count == null) { return 0; }

        final int count = _count.get();
        return (count > 0) ? count : 0;
    }

    public void setCount(int currentCount) {
        if (_count == null) { _count = new AtomicInteger(); }

        _count.set(currentCount);
    }

    public boolean decreaseCount(int val) {
        if (_count == null) { return false; }

        if (_restockTask == null || _restockTask.isDone()) { _restockTask = ThreadPoolManager.getInstance().scheduleGeneral(new RestockTask(), getRestockDelay()); }

        boolean result = _count.addAndGet(-val) >= 0;
        save();
        return result;
    }

    public boolean hasLimitedStock() {
        return getMaxCount() > -1;
    }

    public void restartRestockTask(long nextRestockTime) {
        final long remainingTime = nextRestockTime - System.currentTimeMillis();
        if (remainingTime > 0) { _restockTask = ThreadPoolManager.getInstance().scheduleGeneral(new RestockTask(), remainingTime); }
        else { restock(); }
    }

    public void restock() {
        setCount(getMaxCount());
        save();
    }

    protected final class RestockTask implements Runnable {
        @Override
        public void run() {
            restock();
        }
    }

    private void save() {
        try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("INSERT INTO `buylists`(`buylist_id`, `item_id`, `count`, `next_restock_time`) VALUES(?, ?, ?, ?) ON DUPLICATE KEY UPDATE `count` = ?, `next_restock_time` = ?");
            statement.setInt(1, getBuyListId());
            statement.setInt(2, getItemId());
            statement.setInt(3, getCount());
            statement.setInt(5, getCount());

            if (_restockTask != null && _restockTask.getDelay(TimeUnit.MILLISECONDS) > 0) {
                long nextRestockTime = System.currentTimeMillis() + _restockTask.getDelay(TimeUnit.MILLISECONDS);
                statement.setLong(4, nextRestockTime);
                statement.setLong(6, nextRestockTime);
            }
            else {
                statement.setLong(4, 0);
                statement.setLong(6, 0);
            }
            statement.executeUpdate();
            statement.close();
        }
        catch (Exception e) {
            LOGGER.error("Failed to save Product buylist_id:{} item_id:{}", getBuyListId(), getItemId(), e);
        }
    }
}