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
package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.gameserver.ai.EIntention;
import net.sf.l2j.gameserver.ai.model.L2CharacterAI;
import net.sf.l2j.gameserver.ai.model.L2DoorAI;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.knownlist.DoorKnownList;
import net.sf.l2j.gameserver.model.actor.stat.DoorStat;
import net.sf.l2j.gameserver.model.actor.status.DoorStatus;
import net.sf.l2j.gameserver.model.actor.template.CharTemplate;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.entity.ClanHall;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Weapon;
import net.sf.l2j.gameserver.model.location.HeadedLocation;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.*;
import net.sf.l2j.gameserver.util.threading.ThreadPoolManager;

import java.util.concurrent.ScheduledFuture;

public class L2DoorInstance extends L2Character {
    protected final int _doorId;
    private final boolean _unlockable;
    protected int _autoActionDelay = -1;
    /** The castle index in the array of L2Castle this L2Npc belongs to */
    private int _castleIndex = -2;
    private int _mapRegion = -1;
    // when door is closed, the dimensions are
    private int _rangeXMin;
    private int _rangeYMin;
    private int _rangeZMin;
    private int _rangeXMax;
    private int _rangeYMax;
    private int _rangeZMax;
    // these variables assist in see-through calculation only
    private int _A;
    private int _B;
    private int _C;
    private int _D;
    private boolean _open;
    private boolean _isWall; // False by default
    private int _upgradeHpRatio = 1;
    private ClanHall _clanHall;
    private ScheduledFuture<?> _autoActionTask;

    public L2DoorInstance(int objectId, CharTemplate template, int doorId, String name, boolean unlockable) {
        super(objectId, template);

        _doorId = doorId;
        _unlockable = unlockable;

        setName(name);
    }

    @Override
    public L2CharacterAI getAI() {
        L2CharacterAI ai = this.ai;
        if (ai == null) {
            synchronized (this) {
                if (this.ai == null) { this.ai = new L2DoorAI(this); }

                return this.ai;
            }
        }
        return ai;
    }

    @Override
    public void initKnownList() {
        setKnownList(new DoorKnownList(this));
    }

    @Override
    public final DoorKnownList getKnownList() {
        return (DoorKnownList) super.getKnownList();
    }

    @Override
    public void initCharStat() {
        setStat(new DoorStat(this));
    }

    @Override
    public final DoorStat getStat() {
        return (DoorStat) super.getStat();
    }

    @Override
    public void initCharStatus() {
        setStatus(new DoorStatus(this));
    }

    @Override
    public final DoorStatus getStatus() {
        return (DoorStatus) super.getStatus();
    }

    public final boolean isUnlockable() {
        return _unlockable;
    }

    @Override
    public final int getLevel() {
        return 1;
    }

    /**
     * @return Returns the doorId.
     */
    public int getDoorId() {
        return _doorId;
    }

    /**
     * @return Returns the open.
     */
    public boolean isOpened() {
        return _open;
    }

    /**
     * @param open The open to set.
     */
    public void setOpen(boolean open) {
        _open = open;
    }

    /**
     * Sets the delay for automatic opening/closing of this door instance.<BR>
     * <B>Note:</B> A value of -1 cancels the auto open/close task.
     *
     * @param actionDelay Delay in milliseconds.
     */
    public void setAutoActionDelay(int actionDelay) {
        if (_autoActionDelay == actionDelay) { return; }

        if (actionDelay > -1) {
            AutoOpenClose ao = new AutoOpenClose();
            ThreadPoolManager.getInstance().scheduleAtFixedRate(ao, actionDelay, actionDelay);
        }
        else {
            if (_autoActionTask != null) { _autoActionTask.cancel(false); }
        }

        _autoActionDelay = actionDelay;
    }

    public int getDamage() {
        return Math.max(0, Math.min(6, 6 - (int) Math.ceil(getCurrentHp() / getMaxHp() * 6)));
    }

    public final Castle getCastle() {
        if (_castleIndex < 0) { _castleIndex = CastleManager.getInstance().getCastleIndex(this); }

        if (_castleIndex < 0) { return null; }

        return CastleManager.getInstance().getCastles().get(_castleIndex);
    }

    public ClanHall getClanHall() {
        return _clanHall;
    }

    public void setClanHall(ClanHall clanhall) {
        _clanHall = clanhall;
    }

    @Override
    public boolean isAutoAttackable(L2Character attacker) {
        // Doors can`t be attacked by NPCs
        if (!(attacker instanceof L2Playable)) { return false; }

        if (_unlockable) { return true; }

        // Attackable during siege by attacker only
        boolean isCastle = getCastle() != null && getCastle().getSiege().isInProgress();
        if (isCastle) {
            L2Clan clan = attacker.getActingPlayer().getClan();
            if (clan != null && clan.getClanId() == getCastle().getOwnerId()) { return false; }
        }
        return isCastle;
    }

    public boolean isAttackable(L2Character attacker) {
        return isAutoAttackable(attacker);
    }

    @Override
    public void updateAbnormalEffect() {
    }

    @Override
    public L2ItemInstance getActiveWeaponInstance() {
        return null;
    }

    @Override
    public Weapon getActiveWeaponItem() {
        return null;
    }

    @Override
    public L2ItemInstance getSecondaryWeaponInstance() {
        return null;
    }

    @Override
    public Weapon getSecondaryWeaponItem() {
        return null;
    }

    @Override
    public void onAction(L2PcInstance player) {
        // Set the target of the L2PcInstance player
        if (player.getTarget() != this) {
            player.setTarget(this);
            player.sendPacket(new DoorStatusUpdate(this, player));
        }
        else {
            if (isAutoAttackable(player)) {
                if (Math.abs(player.getZ() - getZ()) < 400) // this max heigth difference might need some tweaking
                { player.getAI().setIntention(EIntention.ATTACK, this); }
            }
            else if (!isInsideRadius(player, L2Npc.INTERACTION_DISTANCE, false, false)) { player.getAI().setIntention(EIntention.INTERACT, this); }
            else if (player.getClan() != null && _clanHall != null && player.getClanId() == _clanHall.getOwnerId()) {
                player.gatesRequest(this);
                if (!_open) { player.sendPacket(new ConfirmDlg(1140)); }
                else { player.sendPacket(new ConfirmDlg(1141)); }

                player.sendPacket(ActionFailed.STATIC_PACKET);
            }
            else
            // Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
            { player.sendPacket(ActionFailed.STATIC_PACKET); }
        }
    }

    @Override
    public void onActionShift(L2PcInstance player) {
        if (player.isGM()) {
            NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
            html.setFile("data/html/admin/infos/doorinfo.htm");
            html.replace("%class%", getClass().getSimpleName());
            html.replace("%objid%", getObjectId());
            html.replace("%doorid%", _doorId);
            html.replace("%hp%", (int) getCurrentHp());
            html.replace("%hpmax%", getMaxHp());
            html.replace("%pdef%", getPDef(null));
            html.replace("%mdef%", getMDef(null, null));
            html.replace("%minx%", _rangeXMin);
            html.replace("%miny%", _rangeYMin);
            html.replace("%minz%", _rangeZMin);
            html.replace("%maxx%", _rangeXMax);
            html.replace("%maxy%", _rangeYMax);
            html.replace("%maxz%", _rangeZMax);
            html.replace("%unlock%", _unlockable ? "<font color=00FF00>YES<font>" : "<font color=FF0000>NO</font>");
            html.replace("%isWall%", _isWall ? "<font color=00FF00>YES<font>" : "<font color=FF0000>NO</font>");
            player.sendPacket(html);
            player.sendPacket(ActionFailed.STATIC_PACKET);
        }

        if (player.getTarget() != this) {
            player.setTarget(this);

            if (isAutoAttackable(player)) { player.sendPacket(new DoorStatusUpdate(this, player)); }
        }
        else { player.sendPacket(ActionFailed.STATIC_PACKET); }
    }

    @Override
    public void broadcastStatusUpdate() {
        for (L2PcInstance player : getKnownList().getKnownType(L2PcInstance.class)) { player.sendPacket(new DoorStatusUpdate(this, player)); }
    }

    public void onOpen() {
        ThreadPoolManager.getInstance().schedule(new CloseTask(), 60000);
    }

    public void onClose() {
        closeMe();
    }

    public final void closeMe() {
        setOpen(false);
        broadcastStatusUpdate();
    }

    public final void openMe() {
        setOpen(true);
        broadcastStatusUpdate();
    }

    @Override
    public String toString() {
        return "door " + _doorId;
    }

    public int getXMin() {
        return _rangeXMin;
    }

    public int getYMin() {
        return _rangeYMin;
    }

    public int getZMin() {
        return _rangeZMin;
    }

    public int getXMax() {
        return _rangeXMax;
    }

    public int getYMax() {
        return _rangeYMax;
    }

    public int getZMax() {
        return _rangeZMax;
    }

    public void setRange(int xMin, int yMin, int zMin, int xMax, int yMax, int zMax) {
        _rangeXMin = xMin;
        _rangeYMin = yMin;
        _rangeZMin = zMin;

        _rangeXMax = xMax;
        _rangeYMax = yMax;
        _rangeZMax = zMax;

        _A = _rangeYMax * (_rangeZMax - _rangeZMin) + _rangeYMin * (_rangeZMin - _rangeZMax);
        _B = _rangeZMin * (_rangeXMax - _rangeXMin) + _rangeZMax * (_rangeXMin - _rangeXMax);
        _C = _rangeXMin * (_rangeYMax - _rangeYMin) + _rangeXMin * (_rangeYMin - _rangeYMax);
        _D = -1 * (_rangeXMin * (_rangeYMax * _rangeZMax - _rangeYMin * _rangeZMax) + _rangeXMax * (_rangeYMin * _rangeZMin - _rangeYMin * _rangeZMax) + _rangeXMin * (_rangeYMin * _rangeZMax - _rangeYMax * _rangeZMin));
    }

    public int getMapRegion() {
        return _mapRegion;
    }

    public void setMapRegion(int region) {
        _mapRegion = region;
    }

    public int getA() {
        return _A;
    }

    public int getB() {
        return _B;
    }

    public int getC() {
        return _C;
    }

    public int getD() {
        return _D;
    }

    public void setIsWall(boolean isWall) {
        _isWall = isWall;
    }

    public boolean isWall() {
        return _isWall;
    }

    @Override
    public void reduceCurrentHp(double damage, L2Character attacker, boolean awake, boolean isDOT, L2Skill skill) {
        if (_isWall && !(attacker instanceof L2SiegeSummonInstance)) { return; }

        if (!(getCastle() != null && getCastle().getSiege().isInProgress())) { return; }

        super.reduceCurrentHp(damage, attacker, awake, isDOT, skill);
    }

    @Override
    public void reduceCurrentHpByDOT(double i, L2Character attacker, L2Skill skill) {
        // Doors can't be damaged by DOTs.
    }

    @Override
    public boolean doDie(L2Character killer) {
        if (!super.doDie(killer)) { return false; }

        if (getCastle() != null && getCastle().getSiege().isInProgress()) {
            getCastle().getSiege().announceToPlayer(SystemMessage.getSystemMessage((_isWall) ? SystemMessageId.CASTLE_WALL_DAMAGED : SystemMessageId.CASTLE_GATE_BROKEN_DOWN), false);
        }

        return true;
    }

    @Override
    public int getMaxHp() {
        return super.getMaxHp() * _upgradeHpRatio;
    }

    public int getUpgradeHpRatio() {
        return _upgradeHpRatio;
    }

    public void setUpgradeHpRatio(int hpRatio) {
        _upgradeHpRatio = hpRatio;
    }

    @Override
    public void addFuncsToNewCharacter() {}

    @Override
    public void moveToLocation(int x, int y, int z, int offset) { }

    @Override
    public void stopMove(HeadedLocation pos) { }

    @Override
    public synchronized void doAttack(L2Character target) { }

    @Override
    public void doCast(L2Skill skill) { }

    @Override
    public void sendInfo(L2PcInstance activeChar) {
        activeChar.sendPacket(new DoorInfo(this, activeChar));
        activeChar.sendPacket(new DoorStatusUpdate(this, activeChar));
    }

    class CloseTask implements Runnable {
        @Override
        public void run() {
            onClose();
        }
    }

    /**
     * Manages the auto open and closing of a door.
     */
    class AutoOpenClose implements Runnable {
        @Override
        public void run() {
            if (!isOpened()) { openMe(); }
            else { closeMe(); }
        }
    }
}