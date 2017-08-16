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
package net.sf.l2j.gameserver.model.base;

/**
 * This class defines all classes (ex : human fighter, darkFighter...) that a player can chose.
 * <ul>
 * <li>id : The Identifier of the class</li>
 * <li>isMage : True if the class is a mage class</li>
 * <li>race : The race of this class</li>
 * <li>parent : The parent ClassId or null if this class is the root</li>
 * </ul>
 */
public enum ClassId {
    fighter(0x00, false, PlayerRace.Human, null),

    warrior(0x01, false, PlayerRace.Human, fighter),
    gladiator(0x02, false, PlayerRace.Human, warrior),
    warlord(0x03, false, PlayerRace.Human, warrior),
    knight(0x04, false, PlayerRace.Human, fighter),
    paladin(0x05, false, PlayerRace.Human, knight),
    darkAvenger(0x06, false, PlayerRace.Human, knight),
    rogue(0x07, false, PlayerRace.Human, fighter),
    treasureHunter(0x08, false, PlayerRace.Human, rogue),
    hawkeye(0x09, false, PlayerRace.Human, rogue),

    mage(0x0a, true, PlayerRace.Human, null),
    wizard(0x0b, true, PlayerRace.Human, mage),
    sorceror(0x0c, true, PlayerRace.Human, wizard),
    necromancer(0x0d, true, PlayerRace.Human, wizard),
    warlock(0x0e, true, PlayerRace.Human, wizard),
    cleric(0x0f, true, PlayerRace.Human, mage),
    bishop(0x10, true, PlayerRace.Human, cleric),
    prophet(0x11, true, PlayerRace.Human, cleric),

    elvenFighter(0x12, false, PlayerRace.Elf, null),
    elvenKnight(0x13, false, PlayerRace.Elf, elvenFighter),
    templeKnight(0x14, false, PlayerRace.Elf, elvenKnight),
    swordSinger(0x15, false, PlayerRace.Elf, elvenKnight),
    elvenScout(0x16, false, PlayerRace.Elf, elvenFighter),
    plainsWalker(0x17, false, PlayerRace.Elf, elvenScout),
    silverRanger(0x18, false, PlayerRace.Elf, elvenScout),

    elvenMage(0x19, true, PlayerRace.Elf, null),
    elvenWizard(0x1a, true, PlayerRace.Elf, elvenMage),
    spellsinger(0x1b, true, PlayerRace.Elf, elvenWizard),
    elementalSummoner(0x1c, true, PlayerRace.Elf, elvenWizard),
    oracle(0x1d, true, PlayerRace.Elf, elvenMage),
    elder(0x1e, true, PlayerRace.Elf, oracle),

    darkFighter(0x1f, false, PlayerRace.DarkElf, null),
    palusKnight(0x20, false, PlayerRace.DarkElf, darkFighter),
    shillienKnight(0x21, false, PlayerRace.DarkElf, palusKnight),
    bladedancer(0x22, false, PlayerRace.DarkElf, palusKnight),
    assassin(0x23, false, PlayerRace.DarkElf, darkFighter),
    abyssWalker(0x24, false, PlayerRace.DarkElf, assassin),
    phantomRanger(0x25, false, PlayerRace.DarkElf, assassin),

    darkMage(0x26, true, PlayerRace.DarkElf, null),
    darkWizard(0x27, true, PlayerRace.DarkElf, darkMage),
    spellhowler(0x28, true, PlayerRace.DarkElf, darkWizard),
    phantomSummoner(0x29, true, PlayerRace.DarkElf, darkWizard),
    shillienOracle(0x2a, true, PlayerRace.DarkElf, darkMage),
    shillenElder(0x2b, true, PlayerRace.DarkElf, shillienOracle),

    orcFighter(0x2c, false, PlayerRace.Orc, null),
    orcRaider(0x2d, false, PlayerRace.Orc, orcFighter),
    destroyer(0x2e, false, PlayerRace.Orc, orcRaider),
    orcMonk(0x2f, false, PlayerRace.Orc, orcFighter),
    tyrant(0x30, false, PlayerRace.Orc, orcMonk),

    orcMage(0x31, false, PlayerRace.Orc, null),
    orcShaman(0x32, true, PlayerRace.Orc, orcMage),
    overlord(0x33, true, PlayerRace.Orc, orcShaman),
    warcryer(0x34, true, PlayerRace.Orc, orcShaman),

    dwarvenFighter(0x35, false, PlayerRace.Dwarf, null),
    scavenger(0x36, false, PlayerRace.Dwarf, dwarvenFighter),
    bountyHunter(0x37, false, PlayerRace.Dwarf, scavenger),
    artisan(0x38, false, PlayerRace.Dwarf, dwarvenFighter),
    warsmith(0x39, false, PlayerRace.Dwarf, artisan),

    // Dummy Entries (id's already in decimal format) <START>
    dummyEntry1(58, false, null, null),
    dummyEntry2(59, false, null, null),
    dummyEntry3(60, false, null, null),
    dummyEntry4(61, false, null, null),
    dummyEntry5(62, false, null, null),
    dummyEntry6(63, false, null, null),
    dummyEntry7(64, false, null, null),
    dummyEntry8(65, false, null, null),
    dummyEntry9(66, false, null, null),
    dummyEntry10(67, false, null, null),
    dummyEntry11(68, false, null, null),
    dummyEntry12(69, false, null, null),
    dummyEntry13(70, false, null, null),
    dummyEntry14(71, false, null, null),
    dummyEntry15(72, false, null, null),
    dummyEntry16(73, false, null, null),
    dummyEntry17(74, false, null, null),
    dummyEntry18(75, false, null, null),
    dummyEntry19(76, false, null, null),
    dummyEntry20(77, false, null, null),
    dummyEntry21(78, false, null, null),
    dummyEntry22(79, false, null, null),
    dummyEntry23(80, false, null, null),
    dummyEntry24(81, false, null, null),
    dummyEntry25(82, false, null, null),
    dummyEntry26(83, false, null, null),
    dummyEntry27(84, false, null, null),
    dummyEntry28(85, false, null, null),
    dummyEntry29(86, false, null, null),
    dummyEntry30(87, false, null, null),

    // 3rd classes
    duelist(0x58, false, PlayerRace.Human, gladiator),
    dreadnought(0x59, false, PlayerRace.Human, warlord),
    phoenixKnight(0x5a, false, PlayerRace.Human, paladin),
    hellKnight(0x5b, false, PlayerRace.Human, darkAvenger),
    sagittarius(0x5c, false, PlayerRace.Human, hawkeye),
    adventurer(0x5d, false, PlayerRace.Human, treasureHunter),
    archmage(0x5e, true, PlayerRace.Human, sorceror),
    soultaker(0x5f, true, PlayerRace.Human, necromancer),
    arcanaLord(0x60, true, PlayerRace.Human, warlock),
    cardinal(0x61, true, PlayerRace.Human, bishop),
    hierophant(0x62, true, PlayerRace.Human, prophet),

    evaTemplar(0x63, false, PlayerRace.Elf, templeKnight),
    swordMuse(0x64, false, PlayerRace.Elf, swordSinger),
    windRider(0x65, false, PlayerRace.Elf, plainsWalker),
    moonlightSentinel(0x66, false, PlayerRace.Elf, silverRanger),
    mysticMuse(0x67, true, PlayerRace.Elf, spellsinger),
    elementalMaster(0x68, true, PlayerRace.Elf, elementalSummoner),
    evaSaint(0x69, true, PlayerRace.Elf, elder),

    shillienTemplar(0x6a, false, PlayerRace.DarkElf, shillienKnight),
    spectralDancer(0x6b, false, PlayerRace.DarkElf, bladedancer),
    ghostHunter(0x6c, false, PlayerRace.DarkElf, abyssWalker),
    ghostSentinel(0x6d, false, PlayerRace.DarkElf, phantomRanger),
    stormScreamer(0x6e, true, PlayerRace.DarkElf, spellhowler),
    spectralMaster(0x6f, true, PlayerRace.DarkElf, phantomSummoner),
    shillienSaint(0x70, true, PlayerRace.DarkElf, shillenElder),

    titan(0x71, false, PlayerRace.Orc, destroyer),
    grandKhauatari(0x72, false, PlayerRace.Orc, tyrant),
    dominator(0x73, true, PlayerRace.Orc, overlord),
    doomcryer(0x74, true, PlayerRace.Orc, warcryer),

    fortuneSeeker(0x75, false, PlayerRace.Dwarf, bountyHunter),
    maestro(0x76, false, PlayerRace.Dwarf, warsmith);

    /** The Identifier of the Class */
    private final int _id;

    /** True if the class is a mage class */
    private final boolean _isMage;

    /** The Race object of the class */
    private final PlayerRace _race;

    /** The parent ClassId or null if this class is a root */
    private final ClassId _parent;

    private ClassId(int pId, boolean pIsMage, PlayerRace pRace, ClassId pParent) {
        _id = pId;
        _isMage = pIsMage;
        _race = pRace;
        _parent = pParent;
    }

    /**
     * @return the Identifier of the Class.
     */
    public final int getId() {
        return _id;
    }

    /**
     * @return True if the class is a mage class.
     */
    public final boolean isMage() {
        return _isMage;
    }

    /**
     * @return the Race object of the class.
     */
    public final PlayerRace getRace() {
        return _race;
    }

    /**
     * @param cid The parent ClassId to check
     * @return True if this Class is a child of the selected ClassId.
     */
    public final boolean childOf(ClassId cid) {
        if (_parent == null) { return false; }

        if (_parent == cid) { return true; }

        return _parent.childOf(cid);
    }

    /**
     * @param cid the parent ClassId to check.
     * @return true if this Class is equal to the selected ClassId or a child of the selected ClassId.
     */
    public final boolean equalsOrChildOf(ClassId cid) {
        return this == cid || childOf(cid);
    }

    /**
     * @return the child level of this Class (0=root, 1=child leve 1...)
     */
    public final int level() {
        if (_parent == null) { return 0; }

        return 1 + _parent.level();
    }

    /**
     * @return its parent ClassId
     */
    public final ClassId getParent() {
        return _parent;
    }
}