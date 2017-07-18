package net.sf.l2j;

import net.sf.l2j.gameserver.model.item.EItemSlot;
import net.sf.l2j.gameserver.model.item.EPaperdollSlot;

import java.util.EnumMap;

/**
 * @author Johnson / 17.07.2017
 */
public class TempMainClass {
    private static EnumMap<EPaperdollSlot, Integer> enumMap = new EnumMap<>(EPaperdollSlot.class);

    public static void main(String... args) {

        System.out.println(">>> " + enumMap.get(null));

    }
}
