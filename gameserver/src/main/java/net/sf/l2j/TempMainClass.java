package net.sf.l2j;

import net.sf.l2j.gameserver.model.item.EItemSlot;

/**
 * @author Johnson / 17.07.2017
 */
public class TempMainClass {

    public static void main(String... args) {
        for (EItemSlot slot : EItemSlot.values()) {
            System.out.println(">>> " + slot.name() + ": " + slot.getBodyPart());
        }
    }
}
