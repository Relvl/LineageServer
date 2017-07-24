package net.sf.l2j.gameserver.model.base;

public enum PlayerRace {
    Human(1),
    Elf(1.5),
    DarkElf(1.5),
    Orc(0.9),
    Dwarf(0.8);

    private final double breathMultiplier;

    PlayerRace(double breathMultiplier) {
        this.breathMultiplier = breathMultiplier;
    }

    public double getBreathMultiplier() {
        return breathMultiplier;
    }
}