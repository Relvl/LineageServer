package net.sf.l2j.gameserver.handler;

import net.sf.l2j.gameserver.handler.itemhandlers.*;
import net.sf.l2j.gameserver.model.item.kind.EtcItem;

import java.util.HashMap;
import java.util.Map;

public class ItemHandler {
    private final Map<Integer, IItemHandler> handlers = new HashMap<>();

    protected ItemHandler() {
        registerItemHandler(new BeastSoulShot());
        registerItemHandler(new BeastSpice());
        registerItemHandler(new BeastSpiritShot());
        registerItemHandler(new BlessedSpiritShot());
        registerItemHandler(new Book());
        registerItemHandler(new Calculator());
        registerItemHandler(new Elixir());
        registerItemHandler(new EnchantScrolls());
        registerItemHandler(new FishShots());
        registerItemHandler(new Harvester());
        registerItemHandler(new ItemSkills());
        registerItemHandler(new Keys());
        registerItemHandler(new Maps());
        registerItemHandler(new MercTicket());
        registerItemHandler(new PaganKeys());
        registerItemHandler(new PetFood());
        registerItemHandler(new Recipes());
        registerItemHandler(new RollingDice());
        registerItemHandler(new ScrollOfResurrection());
        registerItemHandler(new Seed());
        registerItemHandler(new SevenSignsRecord());
        registerItemHandler(new SoulShots());
        registerItemHandler(new SpecialXMas());
        registerItemHandler(new SoulCrystals());
        registerItemHandler(new SpiritShot());
        registerItemHandler(new SummonItems());
    }

    public static ItemHandler getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public void registerItemHandler(IItemHandler handler) {
        handlers.put(handler.getClass().getSimpleName().intern().hashCode(), handler);
    }

    public IItemHandler getItemHandler(EtcItem item) {
        if (item == null || item.getHandlerName() == null) { return null; }
        return handlers.get(item.getHandlerName().hashCode());
    }

    private static final class SingletonHolder {
        private static final ItemHandler INSTANCE = new ItemHandler();
    }
}