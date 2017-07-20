package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.ShotType;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.item.EItemProcessPurpose;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Weapon;
import net.sf.l2j.gameserver.model.item.type.EWeaponType;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.MagicSkillUse;
import net.sf.l2j.gameserver.util.Broadcast;

/**
 * @author -Nemesiss-
 */
public class FishShots implements IItemHandler {
    @Override
    public void useItem(L2Playable playable, L2ItemInstance item, boolean forceUse) {
        if (!(playable instanceof L2PcInstance)) { return; }

        L2PcInstance activeChar = (L2PcInstance) playable;
        L2ItemInstance weaponInst = activeChar.getActiveWeaponInstance();
        Weapon weaponItem = activeChar.getActiveWeaponItem();

        if (weaponInst == null || weaponItem.getItemType() != EWeaponType.FISHINGROD) { return; }

        // Fishshot is already active
        if (activeChar.isChargedShot(ShotType.FISH_SOULSHOT)) { return; }

        // Wrong grade of soulshot for that fishing pole.
        if (weaponItem.getCrystalType() != item.getItem().getCrystalType()) {
            activeChar.sendPacket(SystemMessageId.WRONG_FISHINGSHOT_GRADE);
            return;
        }

        if (!activeChar.destroyItemWithoutTrace(EItemProcessPurpose.CONSUME, item.getObjectId(), 1, null, false)) {
            activeChar.sendPacket(SystemMessageId.NOT_ENOUGH_SOULSHOTS);
            return;
        }

        IntIntHolder[] skills = item.getItem().getSkills();

        activeChar.setChargedShot(ShotType.FISH_SOULSHOT, true);
        Broadcast.toSelfAndKnownPlayers(activeChar, new MagicSkillUse(activeChar, skills[0].getId(), 1, 0, 0));
    }
}