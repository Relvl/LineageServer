package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.ShotType;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.item.EItemProcessPurpose;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Weapon;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.MagicSkillUse;
import net.sf.l2j.gameserver.util.Broadcast;

public class SoulShots implements IItemHandler {
    @Override
    public void useItem(L2Playable playable, L2ItemInstance item, boolean forceUse) {
        if (!(playable instanceof L2PcInstance)) { return; }

        L2PcInstance activeChar = (L2PcInstance) playable;
        L2ItemInstance weaponInst = activeChar.getActiveWeaponInstance();
        Weapon weaponItem = activeChar.getActiveWeaponItem();
        int itemId = item.getItemId();

        // Check if soulshot can be used
        if (weaponInst == null || weaponItem.getSoulShotCount() == 0) {
            if (!activeChar.getAutoSoulShot().contains(itemId)) { activeChar.sendPacket(SystemMessageId.CANNOT_USE_SOULSHOTS); }
            return;
        }

        if (weaponItem.getCrystalType() != item.getItem().getCrystalType()) {
            if (!activeChar.getAutoSoulShot().contains(itemId)) { activeChar.sendPacket(SystemMessageId.SOULSHOTS_GRADE_MISMATCH); }

            return;
        }

        // Check if Soulshot are already active.
        if (activeChar.isChargedShot(ShotType.SOULSHOT)) { return; }

        // Consume Soulshots if player has enough of them.
        int ssCount = weaponItem.getSoulShotCount();
        if (weaponItem.getReducedSoulShot() > 0 && Rnd.get(100) < weaponItem.getReducedSoulShotChance()) { ssCount = weaponItem.getReducedSoulShot(); }

        if (!activeChar.destroyItemWithoutTrace(EItemProcessPurpose.CONSUME, item.getObjectId(), ssCount, null, false)) {
            if (!activeChar.disableAutoShot(itemId)) { activeChar.sendPacket(SystemMessageId.NOT_ENOUGH_SOULSHOTS); }

            return;
        }

        IntIntHolder[] skills = item.getItem().getSkills();

        weaponInst.setChargedShot(ShotType.SOULSHOT, true);
        activeChar.sendPacket(SystemMessageId.ENABLED_SOULSHOT);
        Broadcast.toSelfAndKnownPlayersInRadiusSq(activeChar, new MagicSkillUse(activeChar, activeChar, skills[0].getId(), 1, 0, 0), 360000);
    }
}