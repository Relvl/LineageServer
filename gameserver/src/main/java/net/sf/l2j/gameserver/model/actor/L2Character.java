package net.sf.l2j.gameserver.model.actor;

import net.sf.l2j.Config;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.ECtrlEvent;
import net.sf.l2j.gameserver.ai.EIntention;
import net.sf.l2j.gameserver.ai.model.L2AttackableAI;
import net.sf.l2j.gameserver.ai.model.L2CharacterAI;
import net.sf.l2j.gameserver.datatables.DoorTable;
import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.datatables.MapRegionTable.TeleportWhereType;
import net.sf.l2j.gameserver.datatables.SkillTable.FrequentSkill;
import net.sf.l2j.gameserver.geoengine.GeoData;
import net.sf.l2j.gameserver.geoengine.PathFinding;
import net.sf.l2j.gameserver.handler.ISkillHandler;
import net.sf.l2j.gameserver.handler.SkillHandler;
import net.sf.l2j.gameserver.instancemanager.DimensionalRiftManager;
import net.sf.l2j.gameserver.model.*;
import net.sf.l2j.gameserver.model.actor.instance.*;
import net.sf.l2j.gameserver.model.actor.knownlist.CharKnownList;
import net.sf.l2j.gameserver.model.actor.position.CharPosition;
import net.sf.l2j.gameserver.model.actor.stat.CharStat;
import net.sf.l2j.gameserver.model.actor.status.CharStatus;
import net.sf.l2j.gameserver.model.actor.template.CharTemplate;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.holder.SkillUseHolder;
import net.sf.l2j.gameserver.model.item.EItemProcessPurpose;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Armor;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.item.kind.Weapon;
import net.sf.l2j.gameserver.model.item.type.EWeaponType;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
import net.sf.l2j.gameserver.model.location.HeadedLocation;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.skill.ESkillTargetType;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.model.world.L2World;
import net.sf.l2j.gameserver.model.world.L2WorldRegion;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.AbstractNpcInfo.NpcInfo;
import net.sf.l2j.gameserver.network.client.game_to_client.*;
import net.sf.l2j.gameserver.network.client.game_to_client.FlyToLocation.FlyType;
import net.sf.l2j.gameserver.scripting.EventType;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.skills.AbnormalEffect;
import net.sf.l2j.gameserver.skills.Calculator;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.basefuncs.Func;
import net.sf.l2j.gameserver.skills.effects.EffectChanceSkillTrigger;
import net.sf.l2j.gameserver.skills.funcs.*;
import net.sf.l2j.gameserver.taskmanager.AttackStanceTaskManager;
import net.sf.l2j.gameserver.taskmanager.MovementTaskManager;
import net.sf.l2j.gameserver.templates.skills.L2EffectFlag;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;
import net.sf.l2j.gameserver.util.Broadcast;
import net.sf.l2j.gameserver.util.Util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

public abstract class L2Character extends L2Object {
    protected final Map<Integer, L2Skill> _skills = new LinkedHashMap<>();
    private final Calculator[] _calculators;
    /** Zone system */
    private final byte[] _zones = new byte[ZoneId.getZoneCount()];
    /** Table containing all skillId that are disabled */
    private final Map<Integer, Long> _disabledSkills = new ConcurrentHashMap<>();
    protected boolean _isTeleporting;
    protected boolean _showSummonAnimation;
    protected boolean _isInvul;
    protected FusionSkill _fusionSkill;
    protected byte _zoneValidateCounter = 4;
    protected CharEffectList _effects = new CharEffectList(this);
    /** Movement data of this L2Character */
    protected MoveData _move;
    protected L2CharacterAI ai;
    /** Future Skill Cast */
    protected Future<?> _skillCast;
    protected Future<?> _skillCast2;
    private volatile boolean _isCastingNow;
    private volatile boolean _isCastingSimultaneouslyNow;
    private L2Skill _lastSkillCast;
    private L2Skill _lastSimultaneousSkillCast;
    private boolean _isImmobilized;
    private boolean _isOverloaded;
    private boolean _isParalyzed;
    private boolean _isDead;
    private boolean _isRunning;
    private boolean _isMortal = true;
    private boolean _isNoRndWalk;
    private boolean _AIdisabled;
    private CharStat _stat;
    private CharStatus _status;
    private CharTemplate _template; // The link on the L2CharTemplate object containing generic and static properties
    private CharKnownList _knownList;
    private String _title;
    private double _hpUpdateIncCheck;
    private double _hpUpdateDecCheck;
    private double _hpUpdateInterval;
    private boolean _champion;
    private ChanceSkillList _chanceSkills;
    private boolean _isRaid;
    /** Map 32 bits (0x0000) containing all abnormal effect in progress */
    private int _AbnormalEffects;
    private boolean _allSkillsDisabled;
    /** Orientation of the L2Character */
    private int _heading;
    /** L2Object targeted by the L2Character */
    private L2Object _target;
    // set by the start of attack, in game ticks
    private long _attackEndTime;
    private long _disableBowAttackEndTime;
    private long _castInterruptTime;

    /**
     * Constructor of L2Character.<BR>
     * <BR>
     * <B><U> Concept</U> :</B><BR>
     * <BR>
     * Each L2Character owns generic and static properties (ex : all Keltir have the same number of HP...). All of those properties are stored in a different template for each type of L2Character. Each template is loaded once in the server cache memory (reduce memory use). When a new instance of
     * L2Character is spawned, server just create a link between the instance and the template This link is stored in <B>_template</B><BR>
     * <BR>
     * <B><U> Actions</U> :</B>
     * <ul>
     * <li>Set the _template of the L2Character</li>
     * <li>Set _overloaded to false (the charcater can take more items)</li>
     * </ul>
     * <ul>
     * <li>If L2Character is a L2Npc, copy skills from template to object</li>
     * <li>If L2Character is a L2Npc, link _calculators to NPC_STD_CALCULATOR</li>
     * </ul>
     * <ul>
     * <li>If L2Character is NOT a L2Npc, create an empty _skills slot</li>
     * <li>If L2Character is a L2PcInstance or L2Summon, copy basic Calculator set to object</li>
     * </ul>
     *
     * @param objectId Identifier of the object to initialized
     * @param template The L2CharTemplate to apply to the object
     */
    public L2Character(int objectId, CharTemplate template) {
        super(objectId);
        initKnownList();
        initCharStat();
        initCharStatus();

        // Set its template to the new L2Character
        _template = template;

        _calculators = new Calculator[Stats.NUM_STATS];
        addFuncsToNewCharacter();

        if (this instanceof L2Npc || this instanceof L2Summon) {
            _skills.putAll(((NpcTemplate) template).getSkills());

            if (!_skills.isEmpty()) {
                for (L2Skill skill : getAllSkills()) { addStatFuncs(skill.getStatFuncs(this)); }
            }
        }
    }

    public static boolean isInsidePeaceZone(L2Character attacker, L2Object target) {
        if (target == null) { return false; }

        if (target instanceof L2Npc || attacker instanceof L2Npc) { return false; }

        // Summon or player check.
        if (attacker.getActingPlayer() != null && attacker.getActingPlayer().getAccessLevel().allowPeaceAttack()) { return false; }

        if (Config.KARMA_PLAYER_CAN_BE_KILLED_IN_PZ && target.getActingPlayer() != null && target.getActingPlayer().getKarma() > 0) { return false; }

        if (target instanceof L2Character) { return target.isInsideZone(ZoneId.PEACE) || attacker.isInsideZone(ZoneId.PEACE); }

        return MapRegionTable.getTown(target.getX(), target.getY(), target.getZ()) != null || attacker.isInsideZone(ZoneId.PEACE);
    }

    /**
     * This method is overidden in
     * <ul>
     * <li>L2PcInstance</li>
     * <li>L2DoorInstance</li>
     * </ul>
     */
    public void addFuncsToNewCharacter() {
        addStatFunc(FuncPAtkMod.getInstance());
        addStatFunc(FuncMAtkMod.getInstance());
        addStatFunc(FuncPDefMod.getInstance());
        addStatFunc(FuncMDefMod.getInstance());

        addStatFunc(FuncMaxHpMul.getInstance());
        addStatFunc(FuncMaxMpMul.getInstance());

        addStatFunc(FuncAtkAccuracy.getInstance());
        addStatFunc(FuncAtkEvasion.getInstance());

        addStatFunc(FuncPAtkSpeed.getInstance());
        addStatFunc(FuncMAtkSpeed.getInstance());

        addStatFunc(FuncMoveSpeed.getInstance());

        addStatFunc(FuncAtkCritical.getInstance());
        addStatFunc(FuncMAtkCritical.getInstance());
    }

    protected void initCharStatusUpdateValues() {
        _hpUpdateInterval = getMaxHp() / 352.0; // MAX_HP div MAX_HP_BAR_PX
        _hpUpdateIncCheck = getMaxHp();
        _hpUpdateDecCheck = getMaxHp() - _hpUpdateInterval;
    }

    /**
     * Remove the L2Character from the world when the decay task is launched.<BR>
     * <BR>
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T REMOVE the object from _allObjects of L2World </B></FONT><BR>
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND Server->Client packets to players</B></FONT><BR>
     * <BR>
     */
    public void onDecay() {
        L2WorldRegion reg = getWorldRegion();
        decayMe();
        if (reg != null) { reg.removeFromZones(this); }
    }

    @Override
    public void onSpawn() {
        super.onSpawn();
        revalidateZone(true);
    }

    public void onTeleported() {
        if (!_isTeleporting) { return; }

        spawnMe(getPosition().getX(), getPosition().getY(), getPosition().getZ());
        _isTeleporting = false;
    }

    /**
     * @return character inventory, default null, overridden in L2Playable types and in L2Npc.
     */
    public Inventory getInventory() {
        return null;
    }

    // =========================================================
    // Method - Private

    public boolean destroyItemByItemId(EItemProcessPurpose process, int itemId, int count, L2Object reference, boolean sendMessage) {
        return true;
    }

    public boolean destroyItem(EItemProcessPurpose process, int objectId, int count, L2Object reference, boolean sendMessage) {
        return true;
    }

    @Override
    public boolean isInsideZone(ZoneId zone) {
        return zone == ZoneId.PVP ? _zones[ZoneId.PVP.getId()] > 0 && _zones[ZoneId.PEACE.getId()] == 0 : _zones[zone.getId()] > 0;
    }

    public void setInsideZone(ZoneId zone, boolean state) {
        if (state) { _zones[zone.getId()]++; }
        else {
            _zones[zone.getId()]--;
            if (_zones[zone.getId()] < 0) { _zones[zone.getId()] = 0; }
        }
    }

    /**
     * @return true if the player is GM.
     */
    public boolean isGM() {
        return false;
    }

    /**
     * Send a packet to the L2Character AND to all L2PcInstance in the _KnownPlayers of the L2Character.
     *
     * @param mov The packet to send.
     */
    public void broadcastPacket(L2GameServerPacket mov) {
        Broadcast.toSelfAndKnownPlayers(this, mov);
    }

    /**
     * Send a packet to the L2Character AND to all L2PcInstance in the radius (max knownlist radius) from the L2Character.
     *
     * @param mov    The packet to send.
     * @param radius The radius to make check on.
     */
    public void broadcastPacket(L2GameServerPacket mov, int radius) {
        Broadcast.toSelfAndKnownPlayersInRadius(this, mov, radius);
    }

    /**
     * @param barPixels
     * @return boolean true if hp update should be done, false if not.
     */
    protected boolean needHpUpdate(int barPixels) {
        double currentHp = getCurrentHp();

        if (currentHp <= 1.0 || getMaxHp() < barPixels) { return true; }

        if (currentHp <= _hpUpdateDecCheck || currentHp >= _hpUpdateIncCheck) {
            if (currentHp == getMaxHp()) {
                _hpUpdateIncCheck = currentHp + 1;
                _hpUpdateDecCheck = currentHp - _hpUpdateInterval;
            }
            else {
                double doubleMulti = currentHp / _hpUpdateInterval;
                int intMulti = (int) doubleMulti;

                _hpUpdateDecCheck = _hpUpdateInterval * (doubleMulti < intMulti ? intMulti-- : intMulti);
                _hpUpdateIncCheck = _hpUpdateDecCheck + _hpUpdateInterval;
            }
            return true;
        }
        return false;
    }

    /**
     * Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform.<BR>
     * <BR>
     * <B><U> Actions</U> :</B>
     * <ul>
     * <li>Create the Server->Client packet StatusUpdate with current HP and MP</li>
     * <li>Send the Server->Client packet StatusUpdate with current HP and MP to all L2Character called _statusListener that must be informed of HP/MP updates of this L2Character</li>
     * </ul>
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND CP information</B></FONT><BR>
     * <BR>
     * <B><U>Overriden in L2PcInstance</U></B> : Send current HP,MP and CP to the L2PcInstance and only current HP, MP and Level to all other L2PcInstance of the Party
     */
    public void broadcastStatusUpdate() {
        if (getStatus().getStatusListener().isEmpty()) { return; }

        if (!needHpUpdate(352)) { return; }

        // Create the Server->Client packet StatusUpdate with current HP
        StatusUpdate su = new StatusUpdate(this);
        su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());

        // Go through the StatusListener
        for (L2Character temp : getStatus().getStatusListener()) {
            if (temp != null) { temp.sendPacket(su); }
        }
    }

    public void sendPacket(L2GameServerPacket mov) { }

    public void sendMessage(String text) { }

    public void teleToLocation(int x, int y, int z, int randomOffset) {
        // Stop movement
        stopMove(null);
        abortAttack();
        abortCast();

        _isTeleporting = true;
        setTarget(null);

        getAI().setIntention(EIntention.ACTIVE);

        if (randomOffset > 0) {
            x += Rnd.get(-randomOffset, randomOffset);
            y += Rnd.get(-randomOffset, randomOffset);
        }

        z += 5;

        // Send TeleportToLocationt to the L2Character AND to all L2PcInstance in the _KnownPlayers of the L2Character
        broadcastPacket(new TeleportToLocation(this, x, y, z));

        // remove the object from its old location
        decayMe();

        // Set the x,y,z position of the L2Object and if necessary modify its _worldRegion
        getPosition().setXYZ(x, y, z);

        if (!(this instanceof L2PcInstance) || (((L2PcInstance) this).getClient() != null && ((L2PcInstance) this).getClient().isDetached())) { onTeleported(); }

        revalidateZone(true);
    }

    public void teleToLocation(Location loc, int randomOffset) {
        int x = loc.getX();
        int y = loc.getY();
        int z = loc.getZ();

        if (this instanceof L2PcInstance && DimensionalRiftManager.getInstance().checkIfInRiftZone(getX(), getY(), getZ(), true)) // true -> ignore waiting room :)
        {
            L2PcInstance player = (L2PcInstance) this;
            player.sendMessage("You have been sent to the waiting room.");
            if (player.isInParty() && player.getParty().isInDimensionalRift()) {
                player.getParty().getDimensionalRift().usedTeleport(player);
            }
            int[] newCoords = DimensionalRiftManager.getInstance().getRoom((byte) 0, (byte) 0).getTeleportCoords();
            x = newCoords[0];
            y = newCoords[1];
            z = newCoords[2];
        }
        teleToLocation(x, y, z, randomOffset);
    }

    public void teleToLocation(TeleportWhereType teleportWhere) {
        teleToLocation(MapRegionTable.getInstance().getTeleToLocation(this, teleportWhere), 20);
    }

    /**
     * Launch a physical attack against a target (Simple, Bow, Pole or Dual).<BR>
     * <BR>
     * <B><U> Actions</U> :</B>
     * <ul>
     * <li>Get the active weapon (always equipped in the right hand)</li>
     * </ul>
     * <ul>
     * <li>If weapon is a bow, check for arrows, MP and bow re-use delay (if necessary, equip the L2PcInstance with arrows in left hand)</li>
     * <li>If weapon is a bow, consume MP and set the new period of bow non re-use</li>
     * </ul>
     * <ul>
     * <li>Get the Attack Speed of the L2Character (delay (in milliseconds) before next attack)</li>
     * <li>Select the type of attack to start (Simple, Bow, Pole or Dual) and verify if SoulShot are charged then start calculation</li>
     * <li>If the Server->Client packet Attack contains at least 1 hit, send the Server->Client packet Attack to the L2Character AND to all L2PcInstance in the _KnownPlayers of the L2Character</li>
     * <li>Notify AI with EVT_READY_TO_ACT</li>
     * </ul>
     *
     * @param target The L2Character targeted
     */
    public void doAttack(L2Character target) {
        if (target == null || isAttackingDisabled()) {
            sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }

        if (!isAlikeDead()) {
            if (this instanceof L2Npc && target.isAlikeDead() || !getKnownList().isObjectKnown(target)) {
                getAI().setIntention(EIntention.ACTIVE);
                sendPacket(ActionFailed.STATIC_PACKET);
                return;
            }

            if (this instanceof L2PcInstance && target._isDead) {
                getAI().setIntention(EIntention.ACTIVE);
                sendPacket(ActionFailed.STATIC_PACKET);
                return;
            }
        }

        L2PcInstance player = getActingPlayer();

        if (player != null && player.inObserverMode()) {
            sendPacket(SystemMessage.getSystemMessage(SystemMessageId.OBSERVERS_CANNOT_PARTICIPATE));
            sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }

        // Checking if target has moved to peace zone
        if (isInsidePeaceZone(this, target)) {
            getAI().setIntention(EIntention.ACTIVE);
            sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }

        stopEffectsOnAction();

        // Get the active weapon item corresponding to the active weapon instance (always equipped in the right hand)
        Weapon weaponItem = getActiveWeaponItem();
        EWeaponType weaponItemType = getAttackType();

        if (weaponItemType == EWeaponType.FISHINGROD) {
            // You can't make an attack with a fishing pole.
            getAI().setIntention(EIntention.IDLE);
            sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CANNOT_ATTACK_WITH_FISHING_POLE));
            sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }

        // GeoData Los Check here (or dz > 1000)
        if (!PathFinding.getInstance().canSeeTarget(this, target)) {
            getAI().setIntention(EIntention.ACTIVE);
            sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CANT_SEE_TARGET));
            sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }

        // Check for a bow
        if (weaponItemType == EWeaponType.BOW) {
            // Check for arrows and MP
            if (this instanceof L2PcInstance) {
                // Equip arrows needed in left hand and send ItemList to the L2PcINstance then return True
                if (!checkAndEquipArrows()) {
                    // Cancel the action because the L2PcInstance have no arrow
                    getAI().setIntention(EIntention.IDLE);
                    sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ARROWS));
                    sendPacket(ActionFailed.STATIC_PACKET);
                    return;
                }

                // Verify if the bow can be use
                long timeToNextBowAttack = _disableBowAttackEndTime - System.currentTimeMillis();
                if (timeToNextBowAttack > 0) {
                    // Cancel the action because the bow can't be re-use at this moment
                    ThreadPoolManager.getInstance().scheduleAi(new NotifyAITask(ECtrlEvent.EVT_READY_TO_ACT), timeToNextBowAttack);
                    sendPacket(ActionFailed.STATIC_PACKET);
                    return;
                }

                // Verify if L2PcInstance owns enough MP
                int mpConsume = weaponItem.getMpConsume();
                if (getCurrentMp() < mpConsume) {
                    // If L2PcInstance doesn't have enough MP, stop the attack
                    ThreadPoolManager.getInstance().scheduleAi(new NotifyAITask(ECtrlEvent.EVT_READY_TO_ACT), 100);
                    sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_MP));
                    sendPacket(ActionFailed.STATIC_PACKET);
                    return;
                }

                // If L2PcInstance have enough MP, the bow consummes it
                if (mpConsume > 0) { getStatus().reduceMp(mpConsume); }
            }
            else if (this instanceof L2Npc) {
                if (_disableBowAttackEndTime > System.currentTimeMillis()) { return; }
            }
        }

        // Add the L2PcInstance to _knownObjects and _knownPlayer of the target
        target.getKnownList().addKnownObject(this);

        // Recharge any active auto soulshot tasks for current L2Character instance.
        rechargeShots(true, false);

        // Get the Attack Speed of the L2Character (delay (in milliseconds) before next attack)
        int timeAtk = calculateTimeBetweenAttacks(target, weaponItemType);
        _attackEndTime = System.currentTimeMillis() + timeAtk;

        // Create Attack
        Attack attack = new Attack(this, isChargedShot(ShotType.SOULSHOT), (weaponItem != null) ? weaponItem.getCrystalType().getId() : 0);

        // Make sure that char is facing selected target
        _heading = Util.calculateHeadingFrom(this, target);

        boolean hitted;

        // Select the type of attack to start
        switch (weaponItemType) {
            case BOW:
                hitted = doAttackHitByBow(attack, target, timeAtk, weaponItem);
                break;

            case POLE:
                hitted = doAttackHitByPole(attack, target, timeAtk / 2);
                break;

            case DUAL:
            case DUALFIST:
                hitted = doAttackHitByDual(attack, target, timeAtk / 2);
                break;

            case FIST:
                if (getSecondaryWeaponItem() != null && getSecondaryWeaponItem() instanceof Armor) { hitted = doAttackHitSimple(attack, target, timeAtk / 2); }
                else { hitted = doAttackHitByDual(attack, target, timeAtk / 2); }
                break;

            default:
                hitted = doAttackHitSimple(attack, target, timeAtk / 2);
                break;
        }

        // Flag the attacker if it's a L2PcInstance outside a PvP area
        if (player != null) {
            AttackStanceTaskManager.getInstance().add(player);

            if (player.getPet() != target) { player.updatePvPStatus(target); }
        }

        // Check if hit isn't missed
        if (!hitted)
        // Abort the attack of the L2Character and send Server->Client ActionFailed packet
        { abortAttack(); }
        else {
            // IA implementation for ON_ATTACK_ACT (mob which attacks a player).
            if (this instanceof L2Attackable) {
                try {
                    // Bypass behavior if the victim isn't a player
                    L2PcInstance victim = target.getActingPlayer();
                    if (victim != null) {
                        L2Npc mob = (L2Npc) this;
                        List<Quest> quests = mob.getTemplate().getEventQuests(EventType.ON_ATTACK_ACT);
                        if (quests != null) { for (Quest quest : quests) { quest.notifyAttackAct(mob, victim); } }
                    }
                }
                catch (Exception e) {
                    LOGGER.error("", e);
                }
            }

            // If we didn't miss the hit, discharge the shoulshots, if any
            setChargedShot(ShotType.SOULSHOT, false);

            if (player != null) {
                if (player.isCursedWeaponEquipped()) {
                    // If hitted by a cursed weapon, Cp is reduced to 0
                    if (!target.isInvul()) { target.setCurrentCp(0); }
                }
                else if (player.isHero()) {
                    if (target instanceof L2PcInstance && ((L2PcInstance) target).isCursedWeaponEquipped())
                    // If a cursed weapon is hitted by a Hero, Cp is reduced to 0
                    { target.setCurrentCp(0); }
                }
            }
        }

        // If the Server->Client packet Attack contains at least 1 hit, send the Server->Client packet Attack
        // to the L2Character AND to all L2PcInstance in the _KnownPlayers of the L2Character
        if (attack.hasHits()) { broadcastPacket(attack); }

        // Notify AI with EVT_READY_TO_ACT
        ThreadPoolManager.getInstance().scheduleAi(new NotifyAITask(ECtrlEvent.EVT_READY_TO_ACT), timeAtk);
    }

    /**
     * Launch a Bow attack.<BR>
     * <BR>
     * <B><U> Actions</U> :</B>
     * <ul>
     * <li>Calculate if hit is missed or not</li>
     * <li>Consumme arrows</li>
     * <li>If hit isn't missed, calculate if shield defense is efficient</li>
     * <li>If hit isn't missed, calculate if hit is critical</li>
     * <li>If hit isn't missed, calculate physical damages</li>
     * <li>If the L2Character is a L2PcInstance, Send SetupGauge</li>
     * <li>Create a new hit task with Medium priority</li>
     * <li>Calculate and set the disable delay of the bow in function of the Attack Speed</li>
     * <li>Add this hit to the Server-Client packet Attack</li>
     * </ul>
     *
     * @param attack Server->Client packet Attack in which the hit will be added
     * @param target The L2Character targeted
     * @param sAtk   The Attack Speed of the attacker
     * @param weapon The weapon, which is attacker using
     * @return True if the hit isn't missed
     */
    private boolean doAttackHitByBow(Attack attack, L2Character target, int sAtk, Weapon weapon) {
        int damage1 = 0;
        byte shld1 = 0;
        boolean crit1 = false;

        // Calculate if hit is missed or not
        boolean miss1 = Formulas.calcHitMiss(this, target);

        // Consume arrows
        reduceArrowCount();

        _move = null;

        // Check if hit isn't missed
        if (!miss1) {
            // Calculate if shield defense is efficient
            shld1 = Formulas.calcShldUse(this, target, null);

            // Calculate if hit is critical
            crit1 = Formulas.calcCrit(getStat().getCriticalHit(target, null));

            // Calculate physical damages
            damage1 = (int) Formulas.calcPhysDam(this, target, null, shld1, crit1, attack.soulshot);
        }

        // Get the Attack Reuse Delay of the Weapon
        int reuse = weapon.getReuseDelay();
        if (reuse != 0) { reuse = (reuse * 345) / getStat().getPAtkSpd(); }

        // Check if the L2Character is a L2PcInstance
        if (this instanceof L2PcInstance) {
            // Send a system message
            sendPacket(SystemMessage.getSystemMessage(SystemMessageId.GETTING_READY_TO_SHOOT_AN_ARROW));

            // Send SetupGauge
            sendPacket(new SetupGauge(SetupGauge.RED, sAtk + reuse));
        }

        // Create a new hit task with Medium priority
        ThreadPoolManager.getInstance().scheduleAi(new HitTask(target, damage1, crit1, miss1, attack.soulshot, shld1), sAtk);

        // Calculate and set the disable delay of the bow in function of the Attack Speed
        _disableBowAttackEndTime = System.currentTimeMillis() + (sAtk + reuse);

        // Add this hit to the Server-Client packet Attack
        attack.hit(attack.createHit(target, damage1, miss1, crit1, shld1));

        // Return true if hit isn't missed
        return !miss1;
    }

    /**
     * Launch a Dual attack.<BR>
     * <BR>
     * <B><U> Actions</U> :</B>
     * <ul>
     * <li>Calculate if hits are missed or not</li>
     * <li>If hits aren't missed, calculate if shield defense is efficient</li>
     * <li>If hits aren't missed, calculate if hit is critical</li>
     * <li>If hits aren't missed, calculate physical damages</li>
     * <li>Create 2 new hit tasks with Medium priority</li>
     * <li>Add those hits to the Server-Client packet Attack</li>
     * </ul>
     *
     * @param attack Server->Client packet Attack in which the hit will be added
     * @param target The L2Character targeted
     * @param sAtk   The Attack Speed of the attacker
     * @return True if hit 1 or hit 2 isn't missed
     */
    private boolean doAttackHitByDual(Attack attack, L2Character target, int sAtk) {
        int damage1 = 0;
        int damage2 = 0;
        byte shld1 = 0;
        byte shld2 = 0;
        boolean crit1 = false;
        boolean crit2 = false;

        // Calculate if hits are missed or not
        boolean miss1 = Formulas.calcHitMiss(this, target);
        boolean miss2 = Formulas.calcHitMiss(this, target);

        // Check if hit 1 isn't missed
        if (!miss1) {
            // Calculate if shield defense is efficient against hit 1
            shld1 = Formulas.calcShldUse(this, target, null);

            // Calculate if hit 1 is critical
            crit1 = Formulas.calcCrit(getStat().getCriticalHit(target, null));

            // Calculate physical damages of hit 1
            damage1 = (int) Formulas.calcPhysDam(this, target, null, shld1, crit1, attack.soulshot);
            damage1 /= 2;
        }

        // Check if hit 2 isn't missed
        if (!miss2) {
            // Calculate if shield defense is efficient against hit 2
            shld2 = Formulas.calcShldUse(this, target, null);

            // Calculate if hit 2 is critical
            crit2 = Formulas.calcCrit(getStat().getCriticalHit(target, null));

            // Calculate physical damages of hit 2
            damage2 = (int) Formulas.calcPhysDam(this, target, null, shld2, crit2, attack.soulshot);
            damage2 /= 2;
        }

        // Create a new hit task with Medium priority for hit 1
        ThreadPoolManager.getInstance().scheduleAi(new HitTask(target, damage1, crit1, miss1, attack.soulshot, shld1), sAtk / 2);

        // Create a new hit task with Medium priority for hit 2 with a higher delay
        ThreadPoolManager.getInstance().scheduleAi(new HitTask(target, damage2, crit2, miss2, attack.soulshot, shld2), sAtk);

        // Add those hits to the Server-Client packet Attack
        attack.hit(attack.createHit(target, damage1, miss1, crit1, shld1), attack.createHit(target, damage2, miss2, crit2, shld2));

        // Return true if hit 1 or hit 2 isn't missed
        return !miss1 || !miss2;
    }

    /**
     * Launch a Pole attack.<BR>
     * <B><U> Actions</U> :</B>
     * <ul>
     * <li>Get all visible objects in a spherical area near the L2Character to obtain possible targets</li>
     * <li>If possible target is the L2Character targeted, launch a simple attack against it</li>
     * <li>If possible target isn't the L2Character targeted but is attackable, launch a simple attack against it</li>
     * </ul>
     *
     * @param attack Server->Client packet Attack in which the hit will be added
     * @param target The L2Character targeted
     * @param sAtk   The Attack Speed of the attacker
     * @return True if one hit isn't missed
     */
    private boolean doAttackHitByPole(Attack attack, L2Character target, int sAtk) {
        int maxRadius = getPhysicalAttackRange();
        int maxAngleDiff = (int) getStat().calcStat(Stats.POWER_ATTACK_ANGLE, 120, null, null);

        // Get the number of targets (-1 because the main target is already used)
        int attackRandomCountMax = (int) getStat().calcStat(Stats.ATTACK_COUNT_MAX, 0, null, null) - 1;
        int attackcount = 0;

        boolean hitted = doAttackHitSimple(attack, target, 100, sAtk);
        double attackpercent = 85;

        for (L2Character obj : getKnownList().getKnownType(L2Character.class)) {
            if (obj == target || obj.isAlikeDead()) { continue; }

            if (this instanceof L2PcInstance) {
                if (obj instanceof L2PetInstance && ((L2PetInstance) obj).getOwner() == this) { continue; }
            }
            else if (this instanceof L2Attackable) {
                if (obj instanceof L2PcInstance && _target instanceof L2Attackable) { continue; }

                if (obj instanceof L2Attackable && !isConfused()) { continue; }
            }

            if (!Util.checkIfInRange(maxRadius, this, obj, false)) { continue; }

            // otherwise hit too high/low. 650 because mob z coord sometimes wrong on hills
            if (Math.abs(obj.getZ() - getZ()) > 650) { continue; }

            if (!isFacing(obj, maxAngleDiff)) { continue; }

            // Launch an attack on each character, until attackRandomCountMax is reached.
            if (obj == getAI().getTarget() || obj.isAutoAttackable(this)) {
                attackcount++;
                if (attackcount > attackRandomCountMax) { break; }

                hitted |= doAttackHitSimple(attack, obj, attackpercent, sAtk);
                attackpercent /= 1.15;
            }
        }
        // Return true if one hit isn't missed
        return hitted;
    }

    /**
     * Launch a simple attack.<BR>
     * <BR>
     * <B><U> Actions</U> :</B>
     * <ul>
     * <li>Calculate if hit is missed or not</li>
     * <li>If hit isn't missed, calculate if shield defense is efficient</li>
     * <li>If hit isn't missed, calculate if hit is critical</li>
     * <li>If hit isn't missed, calculate physical damages</li>
     * <li>Create a new hit task with Medium priority</li>
     * <li>Add this hit to the Server-Client packet Attack</li>
     * </ul>
     *
     * @param attack Server->Client packet Attack in which the hit will be added
     * @param target The L2Character targeted
     * @param sAtk   The Attack Speed of the attacker
     * @return True if the hit isn't missed
     */
    private boolean doAttackHitSimple(Attack attack, L2Character target, int sAtk) {
        return doAttackHitSimple(attack, target, 100, sAtk);
    }

    private boolean doAttackHitSimple(Attack attack, L2Character target, double attackpercent, int sAtk) {
        int damage1 = 0;
        byte shld1 = 0;
        boolean crit1 = false;

        // Calculate if hit is missed or not
        boolean miss1 = Formulas.calcHitMiss(this, target);

        // Check if hit isn't missed
        if (!miss1) {
            // Calculate if shield defense is efficient
            shld1 = Formulas.calcShldUse(this, target, null);

            // Calculate if hit is critical
            crit1 = Formulas.calcCrit(getStat().getCriticalHit(target, null));

            // Calculate physical damages
            damage1 = (int) Formulas.calcPhysDam(this, target, null, shld1, crit1, attack.soulshot);

            if (attackpercent != 100) { damage1 = (int) (damage1 * attackpercent / 100); }
        }

        // Create a new hit task with Medium priority
        ThreadPoolManager.getInstance().scheduleAi(new HitTask(target, damage1, crit1, miss1, attack.soulshot, shld1), sAtk);

        // Add this hit to the Server-Client packet Attack
        attack.hit(attack.createHit(target, damage1, miss1, crit1, shld1));

        // Return true if hit isn't missed
        return !miss1;
    }

    /**
     * Manage the casting task (casting and interrupt time, re-use delay...) and display the casting bar and animation on client.<BR>
     * <BR>
     * <B><U> Actions</U> :</B>
     * <ul>
     * <li>Verify the possibilty of the the cast : skill is a spell, caster isn't muted...</li>
     * <li>Get the list of all targets (ex : area effects) and define the L2Charcater targeted (its stats will be used in calculation)</li>
     * <li>Calculate the casting time (base + modifier of MAtkSpd), interrupt time and re-use delay</li>
     * <li>Send MagicSkillUse (to diplay casting animation), a packet SetupGauge (to display casting bar) and a system message</li>
     * <li>Disable all skills during the casting time (create a task EnableAllSkills)</li>
     * <li>Disable the skill during the re-use delay (create a task EnableSkill)</li>
     * <li>Create a task MagicUseTask (that will call method onMagicUseTimer) to launch the Magic Skill at the end of the casting time</li>
     * </ul>
     *
     * @param skill The L2Skill to use
     */
    public void doCast(L2Skill skill) {
        beginCast(skill, false);
    }

    public void doSimultaneousCast(L2Skill skill) {
        beginCast(skill, true);
    }

    private void beginCast(L2Skill skill, boolean simultaneously) {
        if (!checkDoCastConditions(skill)) {
            if (simultaneously) {
                _isCastingSimultaneouslyNow = false;
            }
            else {
                _isCastingNow = false;
            }

            if (this instanceof L2PcInstance) { getAI().setIntention(EIntention.ACTIVE); }

            return;
        }
        // Override casting type
        if (skill.isSimultaneousCast() && !simultaneously) { simultaneously = true; }

        stopEffectsOnAction();

        // Recharge AutoSoulShot
        rechargeShots(skill.isUsingSoulshot(), skill.isUsingSpiritshot());

        // Set the target of the skill in function of Skill Type and Target Type
        L2Character target = null;
        // Get all possible targets of the skill in a table in function of the skill target type
        L2Object[] targets = skill.getTargetType().getTargetList(this, skill);

        boolean doit = false;

        // AURA skills should always be using caster as target
        switch (skill.getTargetType()) {
            case TARGET_AREA_SUMMON: // We need it to correct facing
                target = getPet();
                break;
            case TARGET_AURA:
            case TARGET_FRONT_AURA:
            case TARGET_BEHIND_AURA:
            case TARGET_AURA_UNDEAD:
            case TARGET_GROUND:
                target = this;
                break;
            case TARGET_SELF:
            case TARGET_CORPSE_ALLY:
            case TARGET_PET:
            case TARGET_SUMMON:
            case TARGET_OWNER_PET:
            case TARGET_PARTY:
            case TARGET_CLAN:
            case TARGET_ALLY:
                doit = true;
            default:
                if (targets.length == 0) {
                    if (simultaneously) {
                        _isCastingSimultaneouslyNow = false;
                    }
                    else {
                        _isCastingNow = false;
                    }
                    // Send ActionFailed to the L2PcInstance
                    if (this instanceof L2PcInstance) {
                        sendPacket(ActionFailed.STATIC_PACKET);
                        getAI().setIntention(EIntention.ACTIVE);
                    }
                    return;
                }

                switch (skill.getSkillType()) {
                    case BUFF:
                    case HEAL:
                    case COMBATPOINTHEAL:
                    case MANAHEAL:
                    case SEED:
                    case REFLECT:
                        doit = true;
                        break;
                }

                target = (doit) ? (L2Character) targets[0] : (L2Character) _target;
        }
        beginCast(skill, simultaneously, target, targets);
    }

    private void beginCast(L2Skill skill, boolean simultaneously, L2Character target, L2Object[] targets) {
        if (target == null) {
            if (simultaneously) {
                _isCastingSimultaneouslyNow = false;
            }
            else {
                _isCastingNow = false;
            }

            if (this instanceof L2PcInstance) {
                sendPacket(ActionFailed.STATIC_PACKET);
                getAI().setIntention(EIntention.ACTIVE);
            }
            return;
        }

        // Get the casting time of the skill (base)
        int hitTime = skill.getHitTime();
        int coolTime = skill.getCoolTime();

        boolean effectWhileCasting = skill.getSkillType() == L2SkillType.FUSION || skill.getSkillType() == L2SkillType.SIGNET_CASTTIME;

        // Calculate the casting time of the skill (base + modifier of MAtkSpd)
        // Don't modify the skill time for FUSION skills. The skill time for those skills represent the buff time.
        if (!effectWhileCasting) {
            hitTime = Formulas.calcAtkSpd(this, skill, hitTime);
            if (coolTime > 0) { coolTime = Formulas.calcAtkSpd(this, skill, coolTime); }
        }

        // Calculate altered Cast Speed due to BSpS/SpS
        if (skill.isMagic() && !effectWhileCasting) {
            // Only takes 70% of the time to cast a BSpS/SpS cast
            if (isChargedShot(ShotType.SPIRITSHOT) || isChargedShot(ShotType.BLESSED_SPIRITSHOT)) {
                hitTime = (int) (0.70 * hitTime);
                coolTime = (int) (0.70 * coolTime);
            }
        }

        // Don't modify skills HitTime if staticHitTime is specified for skill in datapack.
        if (skill.isStaticHitTime()) {
            hitTime = skill.getHitTime();
            coolTime = skill.getCoolTime();
        }
        // if basic hitTime is higher than 500 than the min hitTime is 500
        else if (skill.getHitTime() >= 500 && hitTime < 500) { hitTime = 500; }

        // Set the _castInterruptTime and casting status (L2PcInstance already has this true)
        if (simultaneously) {
            // queue herbs and potions
            if (_isCastingSimultaneouslyNow) {
                ThreadPoolManager.getInstance().scheduleAi(new UsePotionTask(this, skill), 100);
                return;
            }
            _isCastingSimultaneouslyNow = true;
            _lastSimultaneousSkillCast = skill;
        }
        else {
            _isCastingNow = true;
            _castInterruptTime = System.currentTimeMillis() + hitTime / 2;
            _lastSkillCast = skill;
        }

        // Init the reuse time of the skill
        int reuseDelay = skill.getReuseDelay();

        if (!skill.isStaticReuse()) {
            reuseDelay *= calcStat(skill.isMagic() ? Stats.MAGIC_REUSE_RATE : Stats.P_REUSE, 1, null, null);
            reuseDelay *= 333.0 / (skill.isMagic() ? getMAtkSpd() : getPAtkSpd());
        }

        boolean skillMastery = Formulas.calcSkillMastery(this, skill);

        // Skill reuse check
        if (reuseDelay > 30000 && !skillMastery) { addTimeStamp(skill, reuseDelay); }

        // Check if this skill consume mp on start casting
        int initmpcons = getStat().getMpInitialConsume(skill);
        if (initmpcons > 0) {
            getStatus().reduceMp(initmpcons);
            StatusUpdate su = new StatusUpdate(this);
            su.addAttribute(StatusUpdate.CUR_MP, (int) getCurrentMp());
            sendPacket(su);
        }

        // Disable the skill during the re-use delay and create a task EnableSkill with Medium priority to enable it at the end of the re-use delay
        if (reuseDelay > 10) {
            if (skillMastery) {
                reuseDelay = 100;

                if (getActingPlayer() != null) { getActingPlayer().sendPacket(SystemMessageId.SKILL_READY_TO_USE_AGAIN); }
            }

            disableSkill(skill, reuseDelay);
        }

        // Make sure that char is facing selected target
        if (target != this) {
            _heading = Util.calculateHeadingFrom(this, target);
        }

        // For force buff skills, start the effect as long as the player is casting.
        if (effectWhileCasting) {
            // Consume Items if necessary and Send the Server->Client packet InventoryUpdate with Item modification to all the L2Character
            if (skill.getItemConsumeId() > 0) {
                if (!destroyItemByItemId(EItemProcessPurpose.CONSUME, skill.getItemConsumeId(), skill.getItemConsume(), null, true)) {
                    sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
                    if (simultaneously) {
                        _isCastingSimultaneouslyNow = false;
                    }
                    else {
                        _isCastingNow = false;
                    }

                    if (this instanceof L2PcInstance) { getAI().setIntention(EIntention.ACTIVE); }
                    return;
                }
            }

            if (skill.getSkillType() == L2SkillType.FUSION) { startFusionSkill(target, skill); }
            else { callSkill(skill, targets); }
        }

        // Get the Display Identifier for a skill that client can't display
        int displayId = skill.getId();

        // Get the level of the skill
        int level = skill.getLevel();
        if (level < 1) { level = 1; }

        // Send MagicSkillUse with target, displayId, level, skillTime, reuseDelay
        // to the L2Character AND to all L2PcInstance in the _KnownPlayers of the L2Character
        if (!skill.isPotion()) {
            broadcastPacket(new MagicSkillUse(this, target, displayId, level, hitTime, reuseDelay, false));
            broadcastPacket(new MagicSkillLaunched(this, displayId, level, (targets == null || targets.length == 0) ? new L2Object[]
                    {
                            target
                    } : targets));
        }
        else { broadcastPacket(new MagicSkillUse(this, target, displayId, level, 0, 0)); }

        if (this instanceof L2Playable) {
            // Send a system message USE_S1 to the L2Character
            if (this instanceof L2PcInstance && skill.getId() != 1312) {
                SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.USE_S1);
                sm.addSkillName(skill);
                sendPacket(sm);
            }

            if (!effectWhileCasting && skill.getItemConsumeId() > 0) {
                if (!destroyItemByItemId(EItemProcessPurpose.CONSUME, skill.getItemConsumeId(), skill.getItemConsume(), null, true)) {
                    getActingPlayer().sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
                    abortCast();
                    return;
                }
            }

            // Before start AI Cast Broadcast Fly Effect is Need
            if (this instanceof L2PcInstance && skill.getFlyType() != null) { ThreadPoolManager.getInstance().scheduleEffect(new FlyToLocationTask(this, target, skill), 50); }
        }

        MagicUseTask mut = new MagicUseTask(targets, skill, hitTime, coolTime, simultaneously);

        // launch the magic in hitTime milliseconds
        if (hitTime > 410) {
            // Send SetupGauge with the color of the gauge and the casting time
            if (this instanceof L2PcInstance && !effectWhileCasting) { sendPacket(new SetupGauge(SetupGauge.BLUE, hitTime)); }

            if (effectWhileCasting) { mut.phase = 2; }

            if (simultaneously) {
                Future<?> future = _skillCast2;
                if (future != null) {
                    future.cancel(true);
                    _skillCast2 = null;
                }

                // Create a task MagicUseTask to launch the MagicSkill at the end of the casting time (hitTime)
                // For client animation reasons (party buffs especially) 400 ms before!
                _skillCast2 = ThreadPoolManager.getInstance().scheduleEffect(mut, hitTime - 400);
            }
            else {
                Future<?> future = _skillCast;
                if (future != null) {
                    future.cancel(true);
                    _skillCast = null;
                }

                // Create a task MagicUseTask to launch the MagicSkill at the end of the casting time (hitTime)
                // For client animation reasons (party buffs especially) 400 ms before!
                _skillCast = ThreadPoolManager.getInstance().scheduleEffect(mut, hitTime - 400);
            }
        }
        else {
            mut.hitTime = 0;
            onMagicLaunchedTimer(mut);
        }
    }

    /**
     * Check if casting of skill is possible
     *
     * @param skill
     * @return True if casting is possible
     */
    protected boolean checkDoCastConditions(L2Skill skill) {
        if (skill == null || isSkillDisabled(skill)) {
            // Send ActionFailed to the L2PcInstance
            sendPacket(ActionFailed.STATIC_PACKET);
            return false;
        }

        // Check if the caster has enough MP
        if (getCurrentMp() < getStat().getMpConsume(skill) + getStat().getMpInitialConsume(skill)) {
            // Send a System Message to the caster
            sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_MP));

            // Send ActionFailed to the L2PcInstance
            sendPacket(ActionFailed.STATIC_PACKET);
            return false;
        }

        // Check if the caster has enough HP
        if (getCurrentHp() <= skill.getHpConsume()) {
            // Send a System Message to the caster
            sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_HP));

            // Send ActionFailed to the L2PcInstance
            sendPacket(ActionFailed.STATIC_PACKET);
            return false;
        }

        // Verify the different types of silence (magic and physic)
        if (!skill.isPotion() && ((skill.isMagic() && isMuted()) || (!skill.isMagic() && isPhysicalMuted()))) {
            // Send ActionFailed to the L2PcInstance
            sendPacket(ActionFailed.STATIC_PACKET);
            return false;
        }

        // prevent casting signets to peace zone
        if (skill.getSkillType() == L2SkillType.SIGNET || skill.getSkillType() == L2SkillType.SIGNET_CASTTIME) {
            L2WorldRegion region = getWorldRegion();
            if (region == null) { return false; }

            if (skill.getTargetType() == ESkillTargetType.TARGET_GROUND && this instanceof L2PcInstance) {
                Location wp = ((L2PcInstance) this).getCurrentSkillWorldPosition();
                if (!region.isEffectRangeInsidePeaceZone(skill, wp.getX(), wp.getY(), wp.getZ())) {
                    sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addSkillName(skill));
                    return false;
                }
            }
            else if (!region.isEffectRangeInsidePeaceZone(skill, getX(), getY(), getZ())) {
                sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addSkillName(skill));
                return false;
            }
        }

        // Check if the caster owns the weapon needed
        if (!skill.getWeaponDependancy(this)) {
            // Send ActionFailed to the L2PcInstance
            sendPacket(ActionFailed.STATIC_PACKET);
            return false;
        }

        // Check if the spell consumes an Item
        if (skill.getItemConsumeId() > 0 && getInventory() != null) {
            // Get the L2ItemInstance consumed by the spell
            L2ItemInstance requiredItems = getInventory().getItemByItemId(skill.getItemConsumeId());

            // Check if the caster owns enough consumed Item to cast
            if (requiredItems == null || requiredItems.getCount() < skill.getItemConsume()) {
                // Checked: when a summon skill failed, server show required consume item count
                if (skill.getSkillType() == L2SkillType.SUMMON) {
                    SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.SUMMONING_SERVITOR_COSTS_S2_S1);
                    sm.addItemName(skill.getItemConsumeId());
                    sm.addNumber(skill.getItemConsume());
                    sendPacket(sm);
                    return false;
                }

                sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NUMBER_INCORRECT));
                return false;
            }
        }

        return true;
    }

    /**
     * Index according to skill id the current timestamp of use, overridden in L2PcInstance.
     *
     * @param skill id
     * @param reuse delay
     */
    public void addTimeStamp(L2Skill skill, long reuse) {
    }

    public void startFusionSkill(L2Character target, L2Skill skill) {
        if (skill.getSkillType() != L2SkillType.FUSION) { return; }

        if (_fusionSkill == null) { _fusionSkill = new FusionSkill(this, target, skill); }
    }

    /**
     * Kill the L2Character.<BR>
     * <BR>
     * <B><U> Actions</U> :</B>
     * <ul>
     * <li>Set target to null and cancel Attack or Cast</li>
     * <li>Stop movement</li>
     * <li>Stop HP/MP/CP Regeneration task</li>
     * <li>Stop all active skills effects in progress on the L2Character</li>
     * <li>Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform</li>
     * <li>Notify L2Character AI</li>
     * </ul>
     * <B><U> Overridden in </U> :</B>
     * <ul>
     * <li>L2Npc : Create a DecayTask to remove the corpse of the L2Npc after 7 seconds</li>
     * <li>L2Attackable : Distribute rewards (EXP, SP, Drops...) and notify Quest Engine</li>
     * <li>L2PcInstance : Apply Death Penalty, Manage gain/loss Karma and Item Drop</li>
     * </ul>
     *
     * @param killer The L2Character who killed it
     * @return true if successful.
     */
    public boolean doDie(L2Character killer) {
        // killing is only possible one time
        synchronized (this) {
            if (_isDead) { return false; }

            // now reset currentHp to zero
            setCurrentHp(0);

            _isDead = true;
        }

        // Set target to null and cancel Attack or Cast
        setTarget(null);

        // Stop movement
        stopMove(null);

        // Stop Regeneration task, and removes all current effects
        getStatus().stopHpMpRegeneration();
        stopAllEffectsExceptThoseThatLastThroughDeath();

        calculateRewards(killer);

        // Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform
        broadcastStatusUpdate();

        // Notify L2Character AI
        if (hasAI()) { getAI().notifyEvent(ECtrlEvent.EVT_DEAD, null); }

        if (getWorldRegion() != null) { getWorldRegion().onDeath(this); }

        return true;
    }

    public void deleteMe() {
        if (hasAI()) { getAI().stopAITask(); }
    }

    public void detachAI() {
        ai = null;
    }

    protected void calculateRewards(L2Character killer) {
    }

    /** Sets HP, MP and CP and revives the L2Character. */
    public void doRevive() {
        if (!_isDead || _isTeleporting) { return; }

        _isDead = false;

        _status.setCurrentHp(getMaxHp() * Config.RESPAWN_RESTORE_HP);

        // Start broadcast status
        broadcastPacket(new Revive(this));

        if (getWorldRegion() != null) { getWorldRegion().onRevive(this); }
    }

    /**
     * Revives the L2Character using skill.
     *
     * @param revivePower
     */
    public void doRevive(double revivePower) {
        doRevive();
    }

    /**
     * @return the L2CharacterAI of the L2Character and if its null create a new one.
     */
    public L2CharacterAI getAI() {
        L2CharacterAI ai = this.ai;
        if (ai == null) {
            synchronized (this) {
                if (this.ai == null) { this.ai = new L2CharacterAI(this); }

                return this.ai;
            }
        }
        return ai;
    }

    public void setAI(L2CharacterAI newAI) {
        L2CharacterAI oldAI = getAI();
        if (oldAI != null && oldAI != newAI && oldAI instanceof L2AttackableAI) { oldAI.stopAITask(); }

        ai = newAI;
    }

    /**
     * @return True if the L2Character has a L2CharacterAI.
     */
    public boolean hasAI() {
        return ai != null;
    }

    /**
     * @return True if the L2Character is RaidBoss or his minion.
     */
    public boolean isRaid() {
        return _isRaid;
    }

    /**
     * Set this Npc as a Raid instance.
     *
     * @param isRaid
     */
    public void setIsRaid(boolean isRaid) {
        _isRaid = isRaid;
    }

    /**
     * @return True if the L2Character is minion.
     */
    public boolean isMinion() {
        return false;
    }

    /**
     * @return True if the L2Character is Raid minion.
     */
    public boolean isRaidMinion() {
        return false;
    }

    public final L2Skill getLastSimultaneousSkillCast() {
        return _lastSimultaneousSkillCast;
    }

    public void setLastSimultaneousSkillCast(L2Skill skill) {
        _lastSimultaneousSkillCast = skill;
    }

    public final L2Skill getLastSkillCast() {
        return _lastSkillCast;
    }

    public void setLastSkillCast(L2Skill skill) {
        _lastSkillCast = skill;
    }

    public final boolean isNoRndWalk() {
        return _isNoRndWalk;
    }

    public final void setIsNoRndWalk(boolean value) {
        _isNoRndWalk = value;
    }

    public final boolean isAfraid() {
        return isAffected(L2EffectFlag.FEAR);
    }

    public final boolean isConfused() {
        return isAffected(L2EffectFlag.CONFUSED);
    }

    public final boolean isMuted() {
        return isAffected(L2EffectFlag.MUTED);
    }

    public final boolean isPhysicalMuted() {
        return isAffected(L2EffectFlag.PHYSICAL_MUTED);
    }

    public final boolean isRooted() {
        return isAffected(L2EffectFlag.ROOTED);
    }

    public final boolean isSleeping() {
        return isAffected(L2EffectFlag.SLEEP);
    }

    public final boolean isStunned() {
        return isAffected(L2EffectFlag.STUNNED);
    }

    public final boolean isBetrayed() {
        return isAffected(L2EffectFlag.BETRAYED);
    }

    public final boolean isImmobileUntilAttacked() {
        return isAffected(L2EffectFlag.MEDITATING);
    }

    /**
     * @return True if the L2Character can't use its skills (ex : stun, sleep...).
     */
    public final boolean isAllSkillsDisabled() {
        return _allSkillsDisabled || isStunned() || isImmobileUntilAttacked() || isSleeping() || isParalyzed();
    }

    /**
     * @return True if the L2Character can't attack (stun, sleep, attackEndTime, fakeDeath, paralyse).
     */
    public boolean isAttackingDisabled() {
        return isFlying() || isStunned() || isImmobileUntilAttacked() || isSleeping() || isAttackingNow() || isParalyzed() || isAlikeDead() || isCoreAIDisabled();
    }

    public final Calculator[] getCalculators() {
        return _calculators;
    }

    public boolean isImmobilized() {
        return _isImmobilized;
    }

    public void setIsImmobilized(boolean value) {
        _isImmobilized = value;
    }

    /**
     * @return True if the L2Character is dead or use fake death.
     */
    public boolean isAlikeDead() {
        return _isDead;
    }

    /**
     * @return True if the L2Character is dead.
     */
    public final boolean isDead() {
        return _isDead;
    }

    public final void setIsDead(boolean value) {
        _isDead = value;
    }

    /**
     * @return True if the L2Character is in a state where he can't move.
     */
    public boolean isMovementDisabled() {
        return isStunned() || isImmobileUntilAttacked() || isRooted() || isSleeping() || _isOverloaded || isParalyzed() || _isImmobilized || isAlikeDead() || _isTeleporting;
    }

    /**
     * @return True if the L2Character is in a state where he can't be controlled.
     */
    public boolean isOutOfControl() {
        return isConfused() || isAfraid() || isParalyzed() || isStunned() || isSleeping();
    }

    public final boolean isOverloaded() {
        return _isOverloaded;
    }

    public final void setIsOverloaded(boolean value) {
        _isOverloaded = value;
    }

    public final boolean isParalyzed() {
        return _isParalyzed || isAffected(L2EffectFlag.PARALYZED);
    }

    public final void setIsParalyzed(boolean value) {
        _isParalyzed = value;
    }

    /**
     * Overriden in L2PcInstance.
     *
     * @return the L2Summon of the L2Character.
     */
    public L2Summon getPet() {
        return null;
    }

    public boolean isSeated() {
        return false;
    }

    public boolean isRiding() {
        return false;
    }

    public boolean isFlying() {
        return false;
    }

    public final boolean isRunning() {
        return _isRunning;
    }

    public final void setIsRunning(boolean value) {
        _isRunning = value;
        if (getRunSpeed() != 0) { broadcastPacket(new ChangeMoveType(this)); }

        if (this instanceof L2PcInstance) { ((L2PcInstance) this).broadcastUserInfo(); }
        else if (this instanceof L2Summon) { broadcastStatusUpdate(); }
        else if (this instanceof L2Npc) {
            for (L2PcInstance player : getKnownList().getKnownType(L2PcInstance.class)) {
                if (getRunSpeed() == 0) { player.sendPacket(new ServerObjectInfo((L2Npc) this, player)); }
                else { player.sendPacket(new NpcInfo((L2Npc) this, player)); }
            }
        }
    }

    /** Set the L2Character movement type to run and send Server->Client packet ChangeMoveType to all others L2PcInstance. */
    public final void setRunning() {
        if (!_isRunning) { setIsRunning(true); }
    }

    public final boolean isTeleporting() {
        return _isTeleporting;
    }

    public final void setIsTeleporting(boolean value) {
        _isTeleporting = value;
    }

    public void setIsInvul(boolean b) {
        _isInvul = b;
    }

    public boolean isInvul() {
        return _isInvul || _isTeleporting;
    }

    public void setIsMortal(boolean b) {
        _isMortal = b;
    }

    public boolean isMortal() {
        return _isMortal;
    }

    public boolean isUndead() {
        return false;
    }

    public void initKnownList() {
        _knownList = new CharKnownList(this);
    }

    @Override
    public CharKnownList getKnownList() {
        return _knownList;
    }

    public void setKnownList(CharKnownList value) {
        _knownList = value;
    }

    public void initCharStat() {
        _stat = new CharStat(this);
    }

    public CharStat getStat() {
        return _stat;
    }

    public final void setStat(CharStat value) {
        _stat = value;
    }

    public void initCharStatus() {
        _status = new CharStatus(this);
    }

    // =========================================================

    public CharStatus getStatus() {
        return _status;
    }

    public final void setStatus(CharStatus value) {
        _status = value;
    }

    @Override
    public void initPosition() {
        setObjectPosition(new CharPosition(this));
    }

    @Override
    public CharPosition getPosition() {
        return (CharPosition) super.getPosition();
    }

    public CharTemplate getTemplate() {
        return _template;
    }

    /**
     * Set the template of the L2Character.<BR>
     * <BR>
     * Each L2Character owns generic and static properties (ex : all Keltir have the same number of HP...). All of those properties are stored in a different template for each type of L2Character. Each template is loaded once in the server cache memory (reduce memory use). When a new instance of
     * L2Character is spawned, server just create a link between the instance and the template This link is stored in <B>_template</B>
     *
     * @param template The template to set up.
     */
    protected final void setTemplate(CharTemplate template) {
        _template = template;
    }

    /**
     * @return the Title of the L2Character.
     */
    public final String getTitle() {
        return _title;
    }

    /**
     * Set the Title of the L2Character. Concatens it if length > 16.
     *
     * @param value The String to test.
     */
    public final void setTitle(String value) {
        if (value == null) { _title = ""; }
        else { _title = value.length() > 16 ? value.substring(0, 15) : value; }
    }

    /** Set the L2Character movement type to walk and send Server->Client packet ChangeMoveType to all others L2PcInstance. */
    public final void setWalking() {
        if (_isRunning) { setIsRunning(false); }
    }

    /**
     * Launch and add L2Effect (including Stack Group management) to L2Character and update client magic icone.<BR>
     * <BR>
     * <B><U> Concept</U> :</B><BR>
     * <BR>
     * All active skills effects in progress on the L2Character are identified in ConcurrentHashMap(Integer,L2Effect) <B>_effects</B>. The Integer key of _effects is the L2Skill Identifier that has created the L2Effect.<BR>
     * <BR>
     * Several same effect can't be used on a L2Character at the same time. Indeed, effects are not stackable and the last cast will replace the previous in progress. More, some effects belong to the same Stack Group (ex WindWald and Haste Potion). If 2 effects of a same group are used at the same
     * time on a L2Character, only the more efficient (identified by its priority order) will be preserve.<BR>
     * <BR>
     * <B><U> Actions</U> :</B>
     * <ul>
     * <li>Add the L2Effect to the L2Character _effects</li>
     * <li>If this effect doesn't belong to a Stack Group, add its Funcs to the Calculator set of the L2Character (remove the old one if necessary)</li>
     * <li>If this effect has higher priority in its Stack Group, add its Funcs to the Calculator set of the L2Character (remove previous stacked effect Funcs if necessary)</li>
     * <li>If this effect has NOT higher priority in its Stack Group, set the effect to Not In Use</li>
     * <li>Update active skills in progress icones on player client</li>
     * </ul>
     *
     * @param newEffect
     */
    public void addEffect(L2Effect newEffect) {
        _effects.queueEffect(newEffect, false);
    }

    /**
     * Stop and remove L2Effect (including Stack Group management) from L2Character and update client magic icone.<BR>
     * <BR>
     * <B><U> Concept</U> :</B><BR>
     * <BR>
     * All active skills effects in progress on the L2Character are identified in ConcurrentHashMap(Integer,L2Effect) <B>_effects</B>. The Integer key of _effects is the L2Skill Identifier that has created the L2Effect.<BR>
     * <BR>
     * Several same effect can't be used on a L2Character at the same time. Indeed, effects are not stackable and the last cast will replace the previous in progress. More, some effects belong to the same Stack Group (ex WindWald and Haste Potion). If 2 effects of a same group are used at the same
     * time on a L2Character, only the more efficient (identified by its priority order) will be preserve.<BR>
     * <BR>
     * <B><U> Actions</U> :</B>
     * <ul>
     * <li>Remove Func added by this effect from the L2Character Calculator (Stop L2Effect)</li>
     * <li>If the L2Effect belongs to a not empty Stack Group, replace theses Funcs by next stacked effect Funcs</li>
     * <li>Remove the L2Effect from _effects of the L2Character</li>
     * <li>Update active skills in progress icones on player client</li>
     * </ul>
     *
     * @param effect
     */
    public final void removeEffect(L2Effect effect) {
        _effects.queueEffect(effect, true);
    }

    public final void startAbnormalEffect(AbnormalEffect mask) {
        _AbnormalEffects |= mask.getMask();
        updateAbnormalEffect();
    }

    public final void startAbnormalEffect(int mask) {
        _AbnormalEffects |= mask;
        updateAbnormalEffect();
    }

    public final void stopAbnormalEffect(AbnormalEffect mask) {
        _AbnormalEffects &= ~mask.getMask();
        updateAbnormalEffect();
    }

    public final void stopAbnormalEffect(int mask) {
        _AbnormalEffects &= ~mask;
        updateAbnormalEffect();
    }

    /**
     * Stop all active skills effects in progress on the L2Character.<BR>
     * <BR>
     */
    public void stopAllEffects() {
        _effects.stopAllEffects();
    }

    public void stopAllEffectsExceptThoseThatLastThroughDeath() {
        _effects.stopAllEffectsExceptThoseThatLastThroughDeath();
    }

    /**
     * Confused
     */
    public final void startConfused() {
        getAI().notifyEvent(ECtrlEvent.EVT_CONFUSED);
        updateAbnormalEffect();
    }

    public final void stopConfused(L2Effect effect) {
        if (effect == null) { stopEffects(L2EffectType.CONFUSION); }
        else { removeEffect(effect); }

        if (!(this instanceof L2PcInstance)) { getAI().notifyEvent(ECtrlEvent.EVT_THINK); }
        updateAbnormalEffect();
    }

    /**
     * Fake Death
     */
    public final void startFakeDeath() {
        if (!(this instanceof L2PcInstance)) { return; }

        ((L2PcInstance) this).setIsFakeDeath(true);
        abortAttack();
        abortCast();
        stopMove(null);
        getAI().notifyEvent(ECtrlEvent.EVT_FAKE_DEATH);
        broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_START_FAKEDEATH));
    }

    public final void stopFakeDeath(boolean removeEffects) {
        if (!(this instanceof L2PcInstance)) { return; }

        L2PcInstance player = (L2PcInstance) this;

        if (removeEffects) { stopEffects(L2EffectType.FAKE_DEATH); }

        // if this is a player instance, start the grace period for this character (grace from mobs only)!
        player.setIsFakeDeath(false);
        player.setRecentFakeDeath();

        broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_STOP_FAKEDEATH));
        broadcastPacket(new Revive(this));

        // Schedule a paralyzed task to wait for the animation to finish
        ThreadPoolManager.getInstance().scheduleGeneral(new Runnable() {
            @Override
            public void run() {
                player.setIsParalyzed(false);
            }
        }, player.getAnimationTimer());
        _isParalyzed = true;
    }

    /**
     * Fear
     */
    public final void startFear() {
        abortAttack();
        abortCast();
        stopMove(null);
        getAI().notifyEvent(ECtrlEvent.EVT_AFRAID);
        updateAbnormalEffect();
    }

    public final void stopFear(boolean removeEffects) {
        if (removeEffects) { stopEffects(L2EffectType.FEAR); }
        updateAbnormalEffect();
    }

    /**
     * ImmobileUntilAttacked
     */
    public final void startImmobileUntilAttacked() {
        abortAttack();
        abortCast();
        stopMove(null);
        getAI().notifyEvent(ECtrlEvent.EVT_SLEEPING);
        updateAbnormalEffect();
    }

    public final void stopImmobileUntilAttacked(L2Effect effect) {
        if (effect == null) { stopEffects(L2EffectType.IMMOBILEUNTILATTACKED); }
        else {
            removeEffect(effect);
            stopSkillEffects(effect.getSkill().getId());
        }

        getAI().notifyEvent(ECtrlEvent.EVT_THINK, null);
        updateAbnormalEffect();
    }

    /**
     * Muted
     */
    public final void startMuted() {
        abortCast();
        getAI().notifyEvent(ECtrlEvent.EVT_MUTED);
        updateAbnormalEffect();
    }

    public final void stopMuted(boolean removeEffects) {
        if (removeEffects) { stopEffects(L2EffectType.MUTE); }

        updateAbnormalEffect();
    }

    /**
     * Paralize
     */
    public final void startParalyze() {
        abortAttack();
        abortCast();
        stopMove(null);
        getAI().notifyEvent(ECtrlEvent.EVT_PARALYZED);
    }

    public final void stopParalyze(boolean removeEffects) {
        if (removeEffects) { stopEffects(L2EffectType.PARALYZE); }

        if (!(this instanceof L2PcInstance)) { getAI().notifyEvent(ECtrlEvent.EVT_THINK); }
    }

    /**
     * PsychicalMuted
     */
    public final void startPhysicalMuted() {
        getAI().notifyEvent(ECtrlEvent.EVT_MUTED);
        updateAbnormalEffect();
    }

    public final void stopPhysicalMuted(boolean removeEffects) {
        if (removeEffects) { stopEffects(L2EffectType.PHYSICAL_MUTE); }

        updateAbnormalEffect();
    }

    /**
     * Root
     */
    public final void startRooted() {
        stopMove(null);
        getAI().notifyEvent(ECtrlEvent.EVT_ROOTED);
        updateAbnormalEffect();
    }

    public final void stopRooting(boolean removeEffects) {
        if (removeEffects) { stopEffects(L2EffectType.ROOT); }

        if (!(this instanceof L2PcInstance)) { getAI().notifyEvent(ECtrlEvent.EVT_THINK); }
        updateAbnormalEffect();
    }

    /**
     * Sleep
     */
    public final void startSleeping() {
        /* Aborts any attacks/casts if slept */
        abortAttack();
        abortCast();
        stopMove(null);
        getAI().notifyEvent(ECtrlEvent.EVT_SLEEPING);
        updateAbnormalEffect();
    }

    public final void stopSleeping(boolean removeEffects) {
        if (removeEffects) { stopEffects(L2EffectType.SLEEP); }

        if (!(this instanceof L2PcInstance)) { getAI().notifyEvent(ECtrlEvent.EVT_THINK); }
        updateAbnormalEffect();
    }

    /**
     * Stun
     */
    public final void startStunning() {
        /* Aborts any attacks/casts if stunned */
        abortAttack();
        abortCast();
        stopMove(null);
        getAI().notifyEvent(ECtrlEvent.EVT_STUNNED);

        if (!(this instanceof L2Summon)) { getAI().setIntention(EIntention.IDLE); }

        updateAbnormalEffect();
    }

    public final void stopStunning(boolean removeEffects) {
        if (removeEffects) { stopEffects(L2EffectType.STUN); }

        if (!(this instanceof L2PcInstance)) { getAI().notifyEvent(ECtrlEvent.EVT_THINK); }
        updateAbnormalEffect();
    }

    /**
     * Stop and remove the L2Effects corresponding to the L2Skill Identifier and update client magic icon.<BR>
     * <BR>
     * <B><U> Concept</U> :</B><BR>
     * <BR>
     * All active skills effects in progress on the L2Character are identified in ConcurrentHashMap(Integer,L2Effect) <B>_effects</B>. The Integer key of _effects is the L2Skill Identifier that has created the L2Effect.<BR>
     * <BR>
     *
     * @param skillId The L2Skill Identifier of the L2Effect to remove from _effects
     */
    public final void stopSkillEffects(int skillId) {
        _effects.stopSkillEffects(skillId);
    }

    /**
     * Stop and remove the L2Effects corresponding to the L2SkillType and update client magic icon.<BR>
     * <BR>
     * <B><U> Concept</U> :</B><BR>
     * <BR>
     * All active skills effects in progress on the L2Character are identified in ConcurrentHashMap(Integer,L2Effect) <B>_effects</B>. The Integer key of _effects is the L2Skill Identifier that has created the L2Effect.<BR>
     * <BR>
     *
     * @param skillType The L2SkillType of the L2Effect to remove from _effects
     * @param negateLvl
     */
    public final void stopSkillEffects(L2SkillType skillType, int negateLvl) {
        _effects.stopSkillEffects(skillType, negateLvl);
    }

    public final void stopSkillEffects(L2SkillType skillType) {
        _effects.stopSkillEffects(skillType, -1);
    }

    /**
     * Stop and remove all L2Effect of the selected type (ex : BUFF, DMG_OVER_TIME...) from the L2Character and update client magic icone.<BR>
     * <BR>
     * <B><U> Concept</U> :</B><BR>
     * <BR>
     * All active skills effects in progress on the L2Character are identified in ConcurrentHashMap(Integer,L2Effect) <B>_effects</B>. The Integer key of _effects is the L2Skill Identifier that has created the L2Effect.<BR>
     * <BR>
     * <B><U> Actions</U> :</B>
     * <ul>
     * <li>Remove Func added by this effect from the L2Character Calculator (Stop L2Effect)</li>
     * <li>Remove the L2Effect from _effects of the L2Character</li>
     * <li>Update active skills in progress icones on player client</li>
     * </ul>
     *
     * @param type The type of effect to stop ((ex : BUFF, DMG_OVER_TIME...)
     */
    public final void stopEffects(L2EffectType type) {
        _effects.stopEffects(type);
    }

    /**
     * Exits all buffs effects of the skills with "removedOnAnyAction" set. Called on any action except movement (attack, cast).
     */
    public final void stopEffectsOnAction() {
        _effects.stopEffectsOnAction();
    }

    /**
     * Exits all buffs effects of the skills with "removedOnDamage" set. Called on decreasing HP and mana burn.
     *
     * @param awake
     */
    public final void stopEffectsOnDamage(boolean awake) {
        _effects.stopEffectsOnDamage(awake);
    }

    /**
     * <B><U> Overridden in</U> :</B>
     * <ul>
     * <li>L2Npc</li>
     * <li>L2PcInstance</li>
     * <li>L2Summon</li>
     * <li>L2DoorInstance</li>
     * </ul>
     * <BR>
     */
    public abstract void updateAbnormalEffect();

    /**
     * Update active skills in progress (In Use and Not In Use because stacked) icones on client.<BR>
     * <BR>
     * <B><U> Concept</U> :</B><BR>
     * <BR>
     * All active skills effects in progress (In Use and Not In Use because stacked) are represented by an icone on the client.<BR>
     * <BR>
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method ONLY UPDATE the client of the player and not clients of all players in the party.</B></FONT><BR>
     * <BR>
     */
    public final void updateEffectIcons() {
        updateEffectIcons(false);
    }

    /**
     * Updates Effect Icons for this character(palyer/summon) and his party if any<BR>
     * Overridden in:
     * <ul>
     * <li>L2PcInstance</li>
     * <li>L2Summon</li>
     * </ul>
     *
     * @param partyOnly
     */
    public void updateEffectIcons(boolean partyOnly) {
        // overridden
    }

    /**
     * In Server->Client packet, each effect is represented by 1 bit of the map (ex : BLEEDING = 0x0001 (bit 1), SLEEP = 0x0080 (bit 8)...). The map is calculated by applying a BINARY OR operation on each effect.
     *
     * @return a map of 16 bits (0x0000) containing all abnormal effect in progress for this L2Character.
     */
    public int getAbnormalEffect() {
        int ae = _AbnormalEffects;
        if (isStunned()) { ae |= AbnormalEffect.STUN.getMask(); }
        if (isRooted()) { ae |= AbnormalEffect.ROOT.getMask(); }
        if (isSleeping()) { ae |= AbnormalEffect.SLEEP.getMask(); }
        if (isConfused()) { ae |= AbnormalEffect.FEAR.getMask(); }
        if (isAfraid()) { ae |= AbnormalEffect.FEAR.getMask(); }
        if (isMuted()) { ae |= AbnormalEffect.MUTED.getMask(); }
        if (isPhysicalMuted()) { ae |= AbnormalEffect.MUTED.getMask(); }
        if (isImmobileUntilAttacked()) { ae |= AbnormalEffect.FLOATING_ROOT.getMask(); }

        return ae;
    }

    /**
     * Return all active skills effects in progress on the L2Character.<BR>
     * <BR>
     * <B><U> Concept</U> :</B><BR>
     * <BR>
     * All active skills effects in progress on the L2Character are identified in <B>_effects</B>. The Integer key of _effects is the L2Skill Identifier that has created the effect.<BR>
     * <BR>
     *
     * @return A table containing all active skills effect in progress on the L2Character
     */
    public final L2Effect[] getAllEffects() {
        return _effects.getAllEffects();
    }

    /**
     * Return L2Effect in progress on the L2Character corresponding to the L2Skill Identifier.<BR>
     * <BR>
     * <B><U> Concept</U> :</B><BR>
     * <BR>
     * All active skills effects in progress on the L2Character are identified in <B>_effects</B>.
     *
     * @param skillId The L2Skill Identifier of the L2Effect to return from the _effects
     * @return The L2Effect corresponding to the L2Skill Identifier
     */
    public final L2Effect getFirstEffect(int skillId) {
        return _effects.getFirstEffect(skillId);
    }

    /**
     * Return the first L2Effect in progress on the L2Character created by the L2Skill.<BR>
     * <BR>
     * <B><U> Concept</U> :</B><BR>
     * <BR>
     * All active skills effects in progress on the L2Character are identified in <B>_effects</B>.
     *
     * @param skill The L2Skill whose effect must be returned
     * @return The first L2Effect created by the L2Skill
     */
    public final L2Effect getFirstEffect(L2Skill skill) {
        return _effects.getFirstEffect(skill);
    }

    /**
     * Return the first L2Effect in progress on the L2Character corresponding to the Effect Type (ex : BUFF, STUN, ROOT...).<BR>
     * <BR>
     * <B><U> Concept</U> :</B><BR>
     * <BR>
     * All active skills effects in progress on the L2Character are identified in ConcurrentHashMap(Integer,L2Effect) <B>_effects</B>. The Integer key of _effects is the L2Skill Identifier that has created the L2Effect.<BR>
     * <BR>
     *
     * @param tp The Effect Type of skills whose effect must be returned
     * @return The first L2Effect corresponding to the Effect Type
     */
    public final L2Effect getFirstEffect(L2EffectType tp) {
        return _effects.getFirstEffect(tp);
    }

    /**
     * Add a Func to the Calculator set of the L2Character.
     *
     * @param f The Func object to add to the Calculator corresponding to the state affected
     */
    public final void addStatFunc(Func f) {
        if (f == null) { return; }

        // Select the Calculator of the affected state in the Calculator set
        int stat = f.stat.ordinal();

        synchronized (_calculators) {
            if (_calculators[stat] == null) { _calculators[stat] = new Calculator(); }

            // Add the Func to the calculator corresponding to the state
            _calculators[stat].addFunc(f);
        }
    }

    /**
     * Add a list of Funcs to the Calculator set of the L2Character.
     *
     * @param funcs The list of Func objects to add to the Calculator corresponding to the state affected
     */
    public final void addStatFuncs(List<Func> funcs) {
        List<Stats> modifiedStats = new ArrayList<>();
        for (Func f : funcs) {
            modifiedStats.add(f.stat);
            addStatFunc(f);
        }
        broadcastModifiedStats(modifiedStats);
    }

    /**
     * Remove all Func objects with the selected owner from the Calculator set of the L2Character.
     *
     * @param owner The Object(Skill, Item...) that has created the effect
     */
    public final void removeStatsByOwner(Object owner) {
        List<Stats> modifiedStats = null;

        int i = 0;
        // Go through the Calculator set
        synchronized (_calculators) {
            for (Calculator calc : _calculators) {
                if (calc != null) {
                    // Delete all Func objects of the selected owner
                    if (modifiedStats != null) { modifiedStats.addAll(calc.removeOwner(owner)); }
                    else { modifiedStats = calc.removeOwner(owner); }

                    if (calc.size() == 0) { _calculators[i] = null; }
                }
                i++;
            }

            if (owner instanceof L2Effect) {
                if (!((L2Effect) owner).preventExitUpdate) { broadcastModifiedStats(modifiedStats); }
            }
            else { broadcastModifiedStats(modifiedStats); }
        }
    }

    private void broadcastModifiedStats(List<Stats> stats) {
        if (stats == null || stats.isEmpty()) { return; }

        boolean broadcastFull = false;
        StatusUpdate su = null;

        if (this instanceof L2Summon && ((L2Summon) this).getOwner() != null) { ((L2Summon) this).updateAndBroadcastStatusAndInfos(1); }
        else {
            for (Stats stat : stats) {
                if (stat == Stats.POWER_ATTACK_SPEED) {
                    if (su == null) { su = new StatusUpdate(this); }

                    su.addAttribute(StatusUpdate.ATK_SPD, getPAtkSpd());
                }
                else if (stat == Stats.MAGIC_ATTACK_SPEED) {
                    if (su == null) { su = new StatusUpdate(this); }

                    su.addAttribute(StatusUpdate.CAST_SPD, getMAtkSpd());
                }
                else if (stat == Stats.MAX_HP && this instanceof L2Attackable) {
                    if (su == null) { su = new StatusUpdate(this); }

                    su.addAttribute(StatusUpdate.MAX_HP, getMaxHp());
                }
                else if (stat == Stats.RUN_SPEED) { broadcastFull = true; }
            }
        }

        if (this instanceof L2PcInstance) {
            if (broadcastFull) { ((L2PcInstance) this).updateAndBroadcastStatus(2); }
            else {
                ((L2PcInstance) this).updateAndBroadcastStatus(1);
                if (su != null) { broadcastPacket(su); }
            }
        }
        else if (this instanceof L2Npc) {
            if (broadcastFull) {
                for (L2PcInstance player : getKnownList().getKnownType(L2PcInstance.class)) {
                    if (getRunSpeed() == 0) { player.sendPacket(new ServerObjectInfo((L2Npc) this, player)); }
                    else { player.sendPacket(new NpcInfo((L2Npc) this, player)); }
                }
            }
            else if (su != null) { broadcastPacket(su); }
        }
        else if (su != null) { broadcastPacket(su); }
    }

    /**
     * @return the orientation of the L2Character.
     */
    public final int getHeading() {
        return _heading;
    }

    /**
     * Set the orientation of the L2Character.
     *
     * @param heading
     */
    public final void setHeading(int heading) {
        _heading = heading;
    }

    public final int getXdestination() {
        MoveData m = _move;
        if (m != null) { return m._xDestination; }

        return getX();
    }

    public final int getYdestination() {
        MoveData m = _move;
        if (m != null) { return m._yDestination; }

        return getY();
    }

    public final int getZdestination() {
        MoveData m = _move;
        if (m != null) { return m._zDestination; }

        return getZ();
    }

    /**
     * @return True if the L2Character is in combat.
     */
    public boolean isInCombat() {
        return getAI().getTarget() != null || getAI().isAutoAttacking();
    }

    /**
     * @return True if the L2Character is moving.
     */
    public final boolean isMoving() {
        return _move != null;
    }

    /**
     * @return True if the L2Character is travelling a calculated path.
     */
    public final boolean isOnGeodataPath() {
        MoveData m = _move;
        if (m == null) { return false; }

        if (m.onGeodataPathIndex == -1) { return false; }

        return m.onGeodataPathIndex != m.geoPath.size() - 1;

    }

    /**
     * @return True if the L2Character is casting.
     */
    public final boolean isCastingNow() {
        return _isCastingNow;
    }

    public void setIsCastingNow(boolean value) {
        _isCastingNow = value;
    }

    public final boolean isCastingSimultaneouslyNow() {
        return _isCastingSimultaneouslyNow;
    }

    public void setIsCastingSimultaneouslyNow(boolean value) {
        _isCastingSimultaneouslyNow = value;
    }

    /**
     * @return True if the cast of the L2Character can be aborted.
     */
    public final boolean canAbortCast() {
        return _castInterruptTime > System.currentTimeMillis();
    }

    /**
     * @return True if the L2Character is attacking.
     */
    public boolean isAttackingNow() {
        return _attackEndTime > System.currentTimeMillis();
    }

    /**
     * Abort the attack of the L2Character and send Server->Client ActionFailed packet.
     */
    public final void abortAttack() {
        if (isAttackingNow()) { sendPacket(ActionFailed.STATIC_PACKET); }
    }

    /**
     * Abort the cast of the L2Character and send Server->Client MagicSkillCanceld/ActionFailed packet.<BR>
     * <BR>
     */
    public final void abortCast() {
        if (_isCastingNow || _isCastingSimultaneouslyNow) {
            Future<?> future = _skillCast;
            // cancels the skill hit scheduled task
            if (future != null) {
                future.cancel(true);
                _skillCast = null;
            }
            future = _skillCast2;
            if (future != null) {
                future.cancel(true);
                _skillCast2 = null;
            }

            if (_fusionSkill != null) {
                _fusionSkill.onCastAbort();
            }

            L2Effect mog = getFirstEffect(L2EffectType.SIGNET_GROUND);
            if (mog != null) { mog.exit(); }

            if (_allSkillsDisabled) {
                enableAllSkills(); // this remains for forced skill use, e.g. scroll of escape
            }

            _isCastingNow = false;
            _isCastingSimultaneouslyNow = false;

            // safeguard for cannot be interrupt any more
            _castInterruptTime = 0;

            if (this instanceof L2Playable) {
                getAI().notifyEvent(ECtrlEvent.EVT_FINISH_CASTING); // setting back previous intention
            }

            broadcastPacket(new MagicSkillCanceld(getObjectId())); // broadcast packet to stop animations client-side
            sendPacket(ActionFailed.STATIC_PACKET); // send an "action failed" packet to the caster
        }
    }

    /**
     * Update the position of the L2Character during a movement and return True if the movement is finished.<BR>
     * <BR>
     * <B><U> Concept</U> :</B><BR>
     * <BR>
     * At the beginning of the move action, all properties of the movement are stored in the MoveData object called <B>_move</B> of the L2Character. The position of the start point and of the destination permit to estimated in function of the movement speed the time to achieve the destination.<BR>
     * <BR>
     * When the movement is started (ex : by MovetoLocation), this method will be called each 0.1 sec to estimate and update the L2Character position on the server. Note, that the current server position can differe from the current client position even if each movement is straight foward. That's
     * why, client send regularly a Client->Server ValidatePosition packet to eventually correct the gap on the server. But, it's always the server position that is used in range calculation.<BR>
     * <BR>
     * At the end of the estimated movement time, the L2Character position is automatically set to the destination position even if the movement is not finished.<BR>
     * <BR>
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : The current Z position is obtained FROM THE CLIENT by the Client->Server ValidatePosition Packet. But x and y positions must be calculated to avoid that players try to modify their movement speed.</B></FONT><BR>
     * <BR>
     *
     * @return True if the movement is finished
     */
    public boolean updatePosition() {
        // Get movement data
        MoveData m = _move;

        if (m == null) { return true; }

        if (!isVisible()) {
            _move = null;
            return true;
        }

        // Check if this is the first update
        if (m._moveTimestamp == 0) {
            m._moveTimestamp = m._moveStartTime;
            m._xAccurate = getX();
            m._yAccurate = getY();
        }

        // get current time
        long time = System.currentTimeMillis();

        // Check if the position has already been calculated
        if (m._moveTimestamp > time) { return false; }

        int xPrev = getX();
        int yPrev = getY();
        int zPrev = getZ(); // the z coordinate may be modified by coordinate synchronizations

        double dx, dy, dz;
        if (Config.COORD_SYNCHRONIZE == 1) {
            // the only method that can modify x,y while moving (otherwise _move would/should be set null)
            dx = m._xDestination - xPrev;
            dy = m._yDestination - yPrev;
        }
        else {
            // otherwise we need saved temporary values to avoid rounding errors
            dx = m._xDestination - m._xAccurate;
            dy = m._yDestination - m._yAccurate;
        }

        boolean isFloating = isFlying() || isInsideZone(ZoneId.WATER);

        // Z coordinate will follow geodata or client values once a second to reduce possible cpu load
        if (Config.GEODATA > 0 && Config.COORD_SYNCHRONIZE == 2 && !isFloating && !m.disregardingGeodata && Rnd.get(10) == 0 && GeoData.getInstance().hasGeo(xPrev, yPrev)) {
            short geoHeight = GeoData.getInstance().getHeight(xPrev, yPrev, zPrev);
            dz = m._zDestination - geoHeight;
            // quite a big difference, compare to validatePosition packet
            if (this instanceof L2PcInstance && Math.abs(((L2PcInstance) this).getClientZ() - geoHeight) > 200 && Math.abs(((L2PcInstance) this).getClientZ() - geoHeight) < 1500) {
                // allow diff
                dz = m._zDestination - zPrev;
            }
            // allow mob to climb up to pcinstance
            else if (isInCombat() && Math.abs(dz) > 200 && (dx * dx + dy * dy) < 40000) {
                // climbing
                dz = m._zDestination - zPrev;
            }
            else { zPrev = geoHeight; }
        }
        else { dz = m._zDestination - zPrev; }

        double delta = dx * dx + dy * dy;
        // close enough, allows error between client and server geodata if it cannot be avoided
        // should not be applied on vertical movements in water or during flight
        if (delta < 10000 && (dz * dz > 2500) && !isFloating) { delta = Math.sqrt(delta); }
        else { delta = Math.sqrt(delta + dz * dz); }

        double distFraction = Double.MAX_VALUE;
        if (delta > 1) {
            double distPassed = (getStat().getMoveSpeed() * (time - m._moveTimestamp)) / 1000;
            distFraction = distPassed / delta;
        }

        // already there, Set the position of the L2Character to the destination
        if (distFraction > 1) { super.getPosition().setXYZ(m._xDestination, m._yDestination, m._zDestination); }
        else {
            m._xAccurate += dx * distFraction;
            m._yAccurate += dy * distFraction;

            // Set the position of the L2Character to estimated after parcial move
            super.getPosition().setXYZ((int) m._xAccurate, (int) m._yAccurate, zPrev + (int) (dz * distFraction + 0.5));
        }
        revalidateZone(false);

        // Set the timer of last position update to now
        m._moveTimestamp = time;

        return distFraction > 1;
    }

    public void revalidateZone(boolean force) {
        if (getWorldRegion() == null) { return; }

        // This function is called too often from movement code
        if (force) { _zoneValidateCounter = 4; }
        else {
            _zoneValidateCounter--;
            if (_zoneValidateCounter < 0) { _zoneValidateCounter = 4; }
            else { return; }
        }
        getWorldRegion().revalidateZones(this);
    }

    /**
     * Stop movement of the L2Character (Called by AI Accessor only).<BR>
     * <BR>
     * <B><U> Actions</U> :</B>
     * <ul>
     * <li>Delete movement data of the L2Character</li>
     * <li>Set the current position (x,y,z), its current L2WorldRegion if necessary and its heading</li>
     * <li>Remove the L2Object object from _gmList** of GmListTable</li>
     * <li>Remove object from _knownObjects and _knownPlayer* of all surrounding L2WorldRegion L2Characters</li>
     * </ul>
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T send Server->Client packet StopMove/StopRotation </B></FONT><BR>
     * <BR>
     *
     * @param pos
     */
    public void stopMove(HeadedLocation pos) {
        // Delete movement data of the L2Character
        _move = null;

        // Set the current position (x,y,z), its current L2WorldRegion if necessary and its heading
        // All data are contained in a L2Position object
        if (pos != null) {
            getPosition().setXYZ(pos.getX(), pos.getY(), pos.getZ());
            _heading = pos.getHeading();
            revalidateZone(true);
        }
        broadcastPacket(new StopMove(this));
    }

    /**
     * @return Returns the showSummonAnimation.
     */
    public boolean isShowSummonAnimation() {
        return _showSummonAnimation;
    }

    /**
     * @param showSummonAnimation The showSummonAnimation to set.
     */
    public void setShowSummonAnimation(boolean showSummonAnimation) {
        _showSummonAnimation = showSummonAnimation;
    }

    /**
     * @return the identifier of the L2Object targeted or -1.
     */
    public final int getTargetId() {
        if (_target != null) { return _target.getObjectId(); }

        return -1;
    }

    /**
     * @return the L2Object targeted or null.
     */
    public final L2Object getTarget() {
        return _target;
    }

    /**
     * Target a L2Object (add the target to the L2Character _target, _knownObject and L2Character to _KnownObject of the L2Object).<BR>
     * <BR>
     * <B><U> Concept</U> :</B><BR>
     * <BR>
     * The L2Object (including L2Character) targeted is identified in <B>_target</B> of the L2Character<BR>
     * <BR>
     * <B><U> Actions</U> :</B>
     * <ul>
     * <li>Set the _target of L2Character to L2Object</li>
     * <li>If necessary, add L2Object to _knownObject of the L2Character</li>
     * <li>If necessary, add L2Character to _KnownObject of the L2Object</li>
     * <li>If object==null, cancel Attak or Cast</li>
     * </ul>
     * <B><U>Overridden in L2PcInstance</U></B> : Remove the L2PcInstance from the old target _statusListener and add it to the new target if it was a L2Character
     *
     * @param object L2object to target
     */
    public void setTarget(L2Object object) {
        if (object != null) {
            if (!object.isVisible()) { object = null; }
            else if (object != _target) {
                getKnownList().addKnownObject(object);

                if (object.getKnownList() != null) { object.getKnownList().addKnownObject(this); }
            }
        }

        _target = object;
    }

    /**
     * Calculate movement data for a move to location action and add the L2Character to movingObjects of GameTimeController (only called by AI Accessor).<BR>
     * <BR>
     * <B><U> Concept</U> :</B><BR>
     * <BR>
     * At the beginning of the move action, all properties of the movement are stored in the MoveData object called <B>_move</B> of the L2Character. The position of the start point and of the destination permit to estimated in function of the movement speed the time to achieve the destination.<BR>
     * <BR>
     * All L2Character in movement are identified in <B>movingObjects</B> of GameTimeController that will call the updatePosition method of those L2Character each 0.1s.<BR>
     * <BR>
     * <B><U> Actions</U> :</B>
     * <ul>
     * <li>Get current position of the L2Character</li>
     * <li>Calculate distance (dx,dy) between current position and destination including offset</li>
     * <li>Create and Init a MoveData object</li>
     * <li>Set the L2Character _move object to MoveData object</li>
     * <li>Add the L2Character to movingObjects of the GameTimeController</li>
     * <li>Create a task to notify the AI that L2Character arrives at a check point of the movement</li>
     * </ul>
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T send Server->Client packet MoveToPawn/MoveToLocation </B></FONT><BR>
     * <BR>
     * <B><U> Example of use </U> :</B>
     * <ul>
     * <li>AI : onIntentionMoveTo(L2Position), onIntentionPickUp(L2Object), onIntentionInteract(L2Object)</li>
     * <li>FollowTask</li>
     * </ul>
     *
     * @param x      The X position of the destination
     * @param y      The Y position of the destination
     * @param z      The Y position of the destination
     * @param offset The size of the interaction area of the L2Character targeted
     */
    public void moveToLocation(int x, int y, int z, int offset) {
        // get movement speed of character
        float speed = getStat().getMoveSpeed();
        if (speed <= 0 || isMovementDisabled()) { return; }

        // get current position of character
        int curX = getX();
        int curY = getY();
        int curZ = getZ();

        // calculate distance (dx, dy, dz) between current position and new destination
        // TODO: improve Z axis move/follow support when dx,dy are small compared to dz
        double dx = x - curX;
        double dy = y - curY;
        double dz = z - curZ;
        double distance = Math.sqrt(dx * dx + dy * dy);

        // check vertical movement
        boolean verticalMovementOnly = isFlying() && distance == 0 && dz != 0;
        if (verticalMovementOnly) { distance = Math.abs(dz); }

        // TODO: really necessary?
        // adjust target XYZ when swiming in water (can be easily over 3000)
        if (Config.GEODATA > 0 && isInsideZone(ZoneId.WATER) && distance > 700) {
            double divider = 700 / distance;
            x = curX + (int) (divider * dx);
            y = curY + (int) (divider * dy);
            z = curZ + (int) (divider * dz);
            dx = x - curX;
            dy = y - curY;
            dz = z - curZ;
            distance = Math.sqrt(dx * dx + dy * dy);
        }

        double cos;
        double sin;

        // Check if a movement offset is defined or no distance to go through
        if (offset > 0 || distance < 1) {
            // approximation for moving closer when z coordinates are different
            // TODO: handle Z axis movement better
            offset -= Math.abs(dz);
            if (offset < 5) { offset = 5; }

            // If no distance to go through, the movement is canceled
            if (distance < 1 || distance - offset <= 0) {
                // Notify the AI that the L2Character is arrived at destination
                getAI().notifyEvent(ECtrlEvent.EVT_ARRIVED);
                return;
            }

            // Calculate movement angles needed
            sin = dy / distance;
            cos = dx / distance;

            distance -= offset - 5; // due to rounding error, we have to move a bit closer to be in range

            // Calculate the new destination with offset included
            x = curX + (int) (distance * cos);
            y = curY + (int) (distance * sin);
        }
        else {
            // Calculate movement angles needed
            sin = dy / distance;
            cos = dx / distance;
        }

        // get new MoveData
        MoveData newMd = new MoveData();

        // initialize new MoveData
        newMd.onGeodataPathIndex = -1;
        newMd.disregardingGeodata = false;

        // flying chars not checked - even canSeeTarget doesn't work yet
        // swimming also not checked unless in siege zone - but distance is limited
        // npc walkers not checked
        if (Config.GEODATA > 0 && !isFlying() && (!isInsideZone(ZoneId.WATER) || isInsideZone(ZoneId.SIEGE)) && !(this instanceof L2NpcWalkerInstance)) {
            boolean isInVehicle = this instanceof L2PcInstance && ((L2PcInstance) this).getVehicle() != null;
            if (isInVehicle) { newMd.disregardingGeodata = true; }

            double originalDistance = distance;
            int originalX = x;
            int originalY = y;
            int originalZ = z;
            int gtx = (originalX - L2World.WORLD_X_MIN) >> 4;
            int gty = (originalY - L2World.WORLD_Y_MIN) >> 4;

            // Movement checks:
            // when geodata == 2, for all characters except mobs returning home (could be changed later to teleport if pathfinding fails)
            // when geodata == 1, for l2playableinstance and l2riftinstance only
            // assuming intention_follow only when following owner
            if ((Config.GEODATA == 2 && !(this instanceof L2Attackable && ((L2Attackable) this).isReturningToSpawnPoint())) || (this instanceof L2PcInstance && !(isInVehicle && distance > 1500)) || (this instanceof L2Summon && !(getAI().getIntention() == EIntention.FOLLOW)) || isAfraid() || this instanceof L2RiftInvaderInstance) {
                if (isOnGeodataPath()) {
                    try {
                        if (gtx == _move.geoPathGtx && gty == _move.geoPathGty) { return; }

                        _move.onGeodataPathIndex = -1; // Set not on geodata path
                    }
                    catch (NullPointerException e) {
                        // nothing
                    }
                }

                if (curX < L2World.WORLD_X_MIN || curX > L2World.WORLD_X_MAX || curY < L2World.WORLD_Y_MIN || curY > L2World.WORLD_Y_MAX) {
                    // Temporary fix for character outside world region errors
                    LOGGER.warn("Character {} outside world area, in coordinates x:{} y:{}", getName(), curX, curY);
                    getAI().setIntention(EIntention.IDLE);

                    if (this instanceof L2PcInstance) { ((L2PcInstance) this).logout(); }
                    else if (this instanceof L2Summon) {
                        return; // prevention when summon get out of world coords, player will not loose him, unsummon handled from pcinstance
                    }
                    else { onDecay(); }

                    return;
                }

                // location different if destination wasn't reached (or just z coord is different)
                Location destiny = PathFinding.getInstance().canMoveToTargetLoc(curX, curY, curZ, x, y, z);
                x = destiny.getX();
                y = destiny.getY();
                z = destiny.getZ();
                dx = x - curX;
                dy = y - curY;
                dz = z - curZ;
                distance = verticalMovementOnly ? Math.abs(dz * dz) : Math.sqrt(dx * dx + dy * dy);
            }

            // Pathfinding checks. Only when geodata setting is 2, the LoS check gives shorter result than the original movement was and the LoS gives a shorter distance than 2000
            // This way of detecting need for pathfinding could be changed.
            if (Config.GEODATA == 2 && originalDistance - distance > 30 && distance < 2000 && !isAfraid()) {
                // Path calculation -- overrides previous movement check
                if ((this instanceof L2Playable && !isInVehicle) || isMinion() || isInCombat()) {
                    newMd.geoPath = PathFinding.getInstance().findPath(curX, curY, curZ, originalX, originalY, originalZ, this instanceof L2Playable);
                    if (newMd.geoPath == null || newMd.geoPath.size() < 2) {
                        // No path found
                        // Even though there's no path found (remember geonodes aren't perfect), the mob is attacking and right now we set it so that the mob will go after target anyway, is dz is small enough.
                        // With cellpathfinding this approach could be changed but would require taking off the geonodes and some more checks.
                        // Summons will follow their masters no matter what.
                        // Currently minions also must move freely since L2AttackableAI commands them to move along with their leader
                        if (this instanceof L2PcInstance || (!(this instanceof L2Playable) && !isMinion() && Math.abs(z - curZ) > 140) || (this instanceof L2Summon && !((L2Summon) this).getFollowStatus())) { return; }

                        newMd.disregardingGeodata = true;
                        x = originalX;
                        y = originalY;
                        z = originalZ;
                        distance = originalDistance;
                    }
                    else {
                        newMd.onGeodataPathIndex = 0; // on first segment
                        newMd.geoPathGtx = gtx;
                        newMd.geoPathGty = gty;
                        newMd.geoPathAccurateTx = originalX;
                        newMd.geoPathAccurateTy = originalY;

                        x = newMd.geoPath.get(newMd.onGeodataPathIndex).getX();
                        y = newMd.geoPath.get(newMd.onGeodataPathIndex).getY();
                        z = newMd.geoPath.get(newMd.onGeodataPathIndex).getZ();

                        // check for doors in the route
                        if (DoorTable.getInstance().checkIfDoorsBetween(curX, curY, curZ, x, y, z)) {
                            newMd.geoPath = null;
                            getAI().setIntention(EIntention.IDLE);
                            return;
                        }

                        for (int i = 0; i < newMd.geoPath.size() - 1; i++) {
                            if (DoorTable.getInstance().checkIfDoorsBetween(newMd.geoPath.get(i), newMd.geoPath.get(i + 1))) {
                                newMd.geoPath = null;
                                getAI().setIntention(EIntention.IDLE);
                                return;
                            }
                        }

                        dx = x - curX;
                        dy = y - curY;
                        dz = z - curZ;
                        distance = verticalMovementOnly ? Math.abs(dz * dz) : Math.sqrt(dx * dx + dy * dy);
                        sin = dy / distance;
                        cos = dx / distance;
                    }
                }
            }

            // If no distance to go through, the movement is canceled
            if (distance < 1 && (Config.GEODATA == 2 || this instanceof L2Playable || this instanceof L2RiftInvaderInstance || isAfraid())) {
                if (this instanceof L2Summon) { ((L2Summon) this).setFollowStatus(false); }

                getAI().setIntention(EIntention.IDLE);
                return;
            }
        }

        // Apply Z distance for flying or swimming for correct timing calculations
        if ((isFlying() || isInsideZone(ZoneId.WATER)) && !verticalMovementOnly) { distance = Math.sqrt(distance * distance + dz * dz); }

        // Caclulate the Nb of ticks between the current position and the destination
        newMd._xDestination = x;
        newMd._yDestination = y;
        newMd._zDestination = z;

        // Calculate and set the heading of the L2Character
        newMd._heading = 0;

        newMd._moveStartTime = System.currentTimeMillis();

        // set new MoveData as character MoveData
        _move = newMd;

        // Does not broke heading on vertical movements
        if (!verticalMovementOnly) {
            _heading = Util.calculateHeadingFrom(cos, sin);
        }

        // add the character to moving objects of the GameTimeController
        MovementTaskManager.getInstance().add(this);
    }

    public boolean moveToNextRoutePoint() {
        // character is not on geodata path, return
        if (!isOnGeodataPath()) {
            _move = null;
            return false;
        }

        // character movement is not allowed, return
        if (getStat().getMoveSpeed() <= 0 || isMovementDisabled()) {
            _move = null;
            return false;
        }

        // get current MoveData
        MoveData oldMd = _move;

        // get new MoveData
        MoveData newMd = new MoveData();

        // initialize new MoveData
        newMd.onGeodataPathIndex = oldMd.onGeodataPathIndex + 1;
        newMd.geoPath = oldMd.geoPath;
        newMd.geoPathGtx = oldMd.geoPathGtx;
        newMd.geoPathGty = oldMd.geoPathGty;
        newMd.geoPathAccurateTx = oldMd.geoPathAccurateTx;
        newMd.geoPathAccurateTy = oldMd.geoPathAccurateTy;

        if (oldMd.onGeodataPathIndex == oldMd.geoPath.size() - 2) {
            newMd._xDestination = oldMd.geoPathAccurateTx;
            newMd._yDestination = oldMd.geoPathAccurateTy;
            newMd._zDestination = oldMd.geoPath.get(newMd.onGeodataPathIndex).getZ();
        }
        else {
            newMd._xDestination = oldMd.geoPath.get(newMd.onGeodataPathIndex).getX();
            newMd._yDestination = oldMd.geoPath.get(newMd.onGeodataPathIndex).getY();
            newMd._zDestination = oldMd.geoPath.get(newMd.onGeodataPathIndex).getZ();
        }

        newMd._heading = 0;
        newMd._moveStartTime = System.currentTimeMillis();

        // set new MoveData as character MoveData
        _move = newMd;

        // get travel distance
        double dx = _move._xDestination - getX();
        double dy = _move._yDestination - getY();
        double distance = Math.sqrt(dx * dx + dy * dy);

        // set character heading
        if (distance != 0) {
            _heading = Util.calculateHeadingFrom(dx, dy);
        }

        // add the character to moving objects of the GameTimeController
        MovementTaskManager.getInstance().add(this);

        // send MoveToLocation packet to known objects
        broadcastPacket(new MoveToLocation(this));

        return true;
    }

    public boolean validateMovementHeading(int heading) {
        MoveData m = _move;

        if (m == null) { return true; }

        boolean result = true;
        if (m._heading != heading) {
            result = m._heading == 0; // initial value or false
            m._heading = heading;
        }

        return result;
    }

    /**
     * Return the squared distance between the current position of the L2Character and the given object.
     *
     * @param object L2Object
     * @return the squared distance
     */
    public final double getDistanceSq(L2Object object) {
        return getDistanceSq(object.getX(), object.getY(), object.getZ());
    }

    /**
     * Return the squared distance between the current position of the L2Character and the given x, y, z.
     *
     * @param x X position of the target
     * @param y Y position of the target
     * @param z Z position of the target
     * @return the squared distance
     */
    public final double getDistanceSq(int x, int y, int z) {
        double dx = x - getX();
        double dy = y - getY();
        double dz = z - getZ();

        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Return the squared plan distance between the current position of the L2Character and the given x, y, z.<BR>
     * (check only x and y, not z)
     *
     * @param x X position of the target
     * @param y Y position of the target
     * @return the squared plan distance
     */
    public final double getPlanDistanceSq(int x, int y) {
        double dx = x - getX();
        double dy = y - getY();

        return dx * dx + dy * dy;
    }

    /**
     * Check if this object is inside the given radius around the given object. Warning: doesn't cover collision radius!
     *
     * @param object      the target
     * @param radius      the radius around the target
     * @param checkZ      should we check Z axis also
     * @param strictCheck true if (distance < radius), false if (distance <= radius)
     * @return true is the L2Character is inside the radius.
     */
    public final boolean isInsideRadius(L2Object object, int radius, boolean checkZ, boolean strictCheck) {
        return isInsideRadius(object.getX(), object.getY(), object.getZ(), radius, checkZ, strictCheck);
    }

    /**
     * Check if this object is inside the given plan radius around the given point. Warning: doesn't cover collision radius!
     *
     * @param x           X position of the target
     * @param y           Y position of the target
     * @param radius      the radius around the target
     * @param strictCheck true if (distance < radius), false if (distance <= radius)
     * @return true is the L2Character is inside the radius.
     */
    public final boolean isInsideRadius(int x, int y, int radius, boolean strictCheck) {
        return isInsideRadius(x, y, 0, radius, false, strictCheck);
    }

    /**
     * Check if this object is inside the given radius around the given point.
     *
     * @param x           X position of the target
     * @param y           Y position of the target
     * @param z           Z position of the target
     * @param radius      the radius around the target
     * @param checkZ      should we check Z axis also
     * @param strictCheck true if (distance < radius), false if (distance <= radius)
     * @return true is the L2Character is inside the radius.
     */
    public final boolean isInsideRadius(int x, int y, int z, int radius, boolean checkZ, boolean strictCheck) {
        double dx = x - getX();
        double dy = y - getY();
        double dz = z - getZ();

        if (strictCheck) {
            if (checkZ) { return (dx * dx + dy * dy + dz * dz) < radius * radius; }

            return (dx * dx + dy * dy) < radius * radius;
        }

        if (checkZ) { return (dx * dx + dy * dy + dz * dz) <= radius * radius; }

        return (dx * dx + dy * dy) <= radius * radius;
    }

    /**
     * @return True if arrows are available.
     */
    protected boolean checkAndEquipArrows() {
        return true;
    }

    /**
     * Add Exp and Sp to the L2Character.
     *
     * @param addToExp An int value.
     * @param addToSp  An int value.
     */
    public void addExpAndSp(long addToExp, int addToSp) {
        // Dummy method (overridden by players and pets)
    }

    /**
     * @return the active weapon instance (always equipped in the right hand).
     */
    public abstract L2ItemInstance getActiveWeaponInstance();

    /**
     * @return the active weapon item (always equipped in the right hand).
     */
    public abstract Weapon getActiveWeaponItem();

    /**
     * @return the secondary weapon instance (always equipped in the left hand).
     */
    public abstract L2ItemInstance getSecondaryWeaponInstance();

    /**
     * @return the secondary {@link Item} item (always equiped in the left hand).
     */
    public abstract Item getSecondaryWeaponItem();

    /**
     * Manage hit process (called by Hit Task).<BR>
     * <BR>
     * <B><U> Actions</U> :</B>
     * <ul>
     * <li>If the attacker/target is dead or use fake death, notify the AI with EVT_CANCEL and send ActionFailed (if attacker is a L2PcInstance)</li>
     * <li>If attack isn't aborted, send a message system (critical hit, missed...) to attacker/target if they are L2PcInstance</li>
     * <li>If attack isn't aborted and hit isn't missed, reduce HP of the target and calculate reflection damage to reduce HP of attacker if necessary</li>
     * <li>if attack isn't aborted and hit isn't missed, manage attack or cast break of the target (calculating rate, sending message...)</li>
     * </ul>
     *
     * @param target   The L2Character targeted
     * @param damage   Nb of HP to reduce
     * @param crit     True if hit is critical
     * @param miss     True if hit is missed
     * @param soulshot True if SoulShot are charged
     * @param shld     True if shield is efficient
     */
    protected void onHitTimer(L2Character target, int damage, boolean crit, boolean miss, boolean soulshot, byte shld) {
        // If the attacker/target is dead or use fake death, notify the AI with EVT_CANCEL
        if (target == null || isAlikeDead()) {
            getAI().notifyEvent(ECtrlEvent.EVT_CANCEL);
            return;
        }

        if ((this instanceof L2Npc && target.isAlikeDead()) || target._isDead || (!getKnownList().isObjectKnown(target) && !(this instanceof L2DoorInstance))) {
            getAI().notifyEvent(ECtrlEvent.EVT_CANCEL);

            sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }

        if (miss) {
            // Notify target AI
            if (target.hasAI()) { target.getAI().notifyEvent(ECtrlEvent.EVT_EVADED, this); }

            // ON_EVADED_HIT
            if (target._chanceSkills != null) {
                target._chanceSkills.onEvadedHit(this);
            }

            if (target instanceof L2PcInstance) { target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.AVOIDED_S1_ATTACK).addCharName(this)); }
        }

        // Send message about damage/crit or miss
        sendDamageMessage(target, damage, false, crit, miss);

        // Character will be petrified if attacking a raid that's more than 8 levels lower
        if (!Config.RAID_DISABLE_CURSE && target.isRaid() && getLevel() > target.getLevel() + 8) {
            L2Skill skill = FrequentSkill.RAID_CURSE2.getSkill();
            if (skill != null) {
                // Send visual and skill effects. Caster is the victim.
                broadcastPacket(new MagicSkillUse(this, this, skill.getId(), skill.getLevel(), 300, 0));
                skill.getEffects(this, this);
            }

            damage = 0; // prevents messing up drop calculation
        }

        // If the target is a player, start AutoAttack
        if (target instanceof L2PcInstance) { target.getAI().clientStartAutoAttack(); }

        if (!miss && damage > 0) {
            boolean isBow = getAttackType() == EWeaponType.BOW;
            int reflectedDamage = 0;

            // Reflect damage system - do not reflect if weapon is a bow or target is invulnerable
            if (!isBow && !target.isInvul()) {
                // quick fix for no drop from raid if boss attack high-level char with damage reflection
                if (!target.isRaid() || getActingPlayer() == null || getActingPlayer().getLevel() <= target.getLevel() + 8) {
                    // Calculate reflection damage to reduce HP of attacker if necessary
                    double reflectPercent = target.getStat().calcStat(Stats.REFLECT_DAMAGE_PERCENT, 0, null, null);
                    if (reflectPercent > 0) {
                        reflectedDamage = (int) (reflectPercent / 100. * damage);

                        // You can't kill someone from a reflect. If value > current HPs, make damages equal to current HP - 1.
                        int currentHp = (int) getCurrentHp();
                        if (reflectedDamage >= currentHp) { reflectedDamage = currentHp - 1; }
                    }
                }
            }

            // Reduce target HPs
            target.reduceCurrentHp(damage, this, null);

            // Reduce attacker HPs in case of a reflect.
            if (reflectedDamage > 0) { reduceCurrentHp(reflectedDamage, target, true, false, null); }

            if (!isBow) // Do not absorb if weapon is of type bow
            {
                // Absorb HP from the damage inflicted
                double absorbPercent = getStat().calcStat(Stats.ABSORB_DAMAGE_PERCENT, 0, null, null);

                if (absorbPercent > 0) {
                    int maxCanAbsorb = (int) (getMaxHp() - getCurrentHp());
                    int absorbDamage = (int) (absorbPercent / 100. * damage);

                    if (absorbDamage > maxCanAbsorb) {
                        absorbDamage = maxCanAbsorb; // Can't absord more than max hp
                    }

                    if (absorbDamage > 0) { setCurrentHp(getCurrentHp() + absorbDamage); }
                }
            }

            // Notify AI with EVT_ATTACKED
            if (target.hasAI()) { target.getAI().notifyEvent(ECtrlEvent.EVT_ATTACKED, this); }

            getAI().clientStartAutoAttack();

            // Manage cast break of the target (calculating rate, sending message...)
            Formulas.calcCastBreak(target, damage);

            // Maybe launch chance skills on us
            if (_chanceSkills != null) {
                _chanceSkills.onHit(target, false, crit);

                // Reflect triggers onHit
                if (reflectedDamage > 0) { _chanceSkills.onHit(target, true, false); }
            }

            // Maybe launch chance skills on target
            if (target._chanceSkills != null) {
                target._chanceSkills.onHit(this, true, crit);
            }
        }

        // Launch weapon Special ability effect if available
        Weapon activeWeapon = getActiveWeaponItem();
        if (activeWeapon != null) { activeWeapon.getSkillEffects(this, target, crit); }
    }

    /**
     * Break an attack and send Server->Client ActionFailed packet and a System Message to the L2Character.
     */
    public void breakAttack() {
        if (isAttackingNow()) {
            // Abort the attack of the L2Character and send Server->Client ActionFailed packet
            abortAttack();

            if (this instanceof L2PcInstance) { sendPacket(SystemMessage.getSystemMessage(SystemMessageId.ATTACK_FAILED)); }
        }
    }

    /**
     * Break a cast and send Server->Client ActionFailed packet and a System Message to the L2Character.
     */
    public void breakCast() {
        // damage can only cancel magical skills
        if (_isCastingNow && canAbortCast() && _lastSkillCast != null && _lastSkillCast.isMagic()) {
            // Abort the cast of the L2Character and send Server->Client MagicSkillCanceld/ActionFailed packet.
            abortCast();

            if (this instanceof L2PcInstance) { sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CASTING_INTERRUPTED)); }
        }
    }

    /**
     * Reduce the arrow number of the L2Character.<BR>
     * <BR>
     * <B><U> Overriden in </U> :</B><BR>
     * <BR>
     * <li>L2PcInstance</li><BR>
     * <BR>
     */
    protected void reduceArrowCount() {
        // default is to do nothing
    }

    /**
     * Manage Forced attack (shift + select target).<BR>
     * <BR>
     * <B><U> Actions</U> :</B>
     * <ul>
     * <li>If L2Character or target is in a town area, send a system message TARGET_IN_PEACEZONE ActionFailed</li>
     * <li>If target is confused, send ActionFailed</li>
     * <li>If L2Character is a L2ArtefactInstance, send ActionFailed</li>
     * <li>Send MyTargetSelected to start attack and Notify AI with ATTACK</li>
     * </ul>
     *
     * @param player The L2PcInstance to attack
     */
    @Override
    public void onForcedAttack(L2PcInstance player) {
        if (isInsidePeaceZone(player, this)) {
            // If L2Character or target is in a peace zone, send a system message TARGET_IN_PEACEZONE ActionFailed
            player.sendPacket(SystemMessageId.TARGET_IN_PEACEZONE);
            player.sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }

        if (player.isInOlympiadMode() && player.getTarget() != null && player.getTarget() instanceof L2Playable) {
            L2PcInstance target = player.getTarget().getActingPlayer();
            if (target == null || (target.isInOlympiadMode() && (!player.isOlympiadStart() || player.getOlympiadGameId() != target.getOlympiadGameId()))) {
                // if L2PcInstance is in Olympia and the match isn't already start, send ActionFailed
                player.sendPacket(ActionFailed.STATIC_PACKET);
                return;
            }
        }

        if (player.getTarget() != null && !player.getTarget().isAttackable() && !player.getAccessLevel().allowPeaceAttack()) {
            // If target is not attackable, send ActionFailed
            player.sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }

        if (player.isConfused()) {
            // If target is confused, send ActionFailed
            player.sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }

        // GeoData Los Check or dz > 1000
        if (!PathFinding.getInstance().canSeeTarget(player, this)) {
            player.sendPacket(SystemMessageId.CANT_SEE_TARGET);
            player.sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }

        // Notify AI with ATTACK
        player.getAI().setIntention(EIntention.ATTACK, this);
    }

    /**
     * This method checks if the player given as argument can interact with the L2Npc.
     *
     * @param player The player to test
     * @return true if the player can interact with the L2Npc
     */
    public boolean canInteract(L2PcInstance player) {
        // Can't interact while casting a spell.
        if (player.isCastingNow() || player.isCastingSimultaneouslyNow()) { return false; }

        // Can't interact while died.
        if (player.isDead() || player.isFakeDeath()) { return false; }

        // Can't interact sitted.
        if (player.isSitting()) { return false; }

        // Can't interact in shop mode, or during a transaction or a request.
        if (player.isInStoreMode() || player.isProcessingTransaction()) { return false; }

        // Can't interact if regular distance doesn't match.
        return isInsideRadius(player, L2Npc.INTERACTION_DISTANCE, true, false);

    }

    /**
     * @return true if this character is inside an active grid.
     */
    public boolean isInActiveRegion() {
        try {
            L2WorldRegion region = L2World.getInstance().getRegion(getX(), getY());
            return (region != null) && region.isActive();
        }
        catch (Exception e) {
            if (this instanceof L2PcInstance) {
                LOGGER.warn("Player {} at bad coords: (x: {}, y: {}, z: {}).", getName(), getX(), getY(), getZ());
                sendMessage("Error with your coordinates! Please reboot your game fully!");
                teleToLocation(80753, 145481, -3532, 0); // Near Giran luxury shop
            }
            else {
                LOGGER.warn("Object {} at bad coords: (x: {}, y: {}, z: {}).", getName(), getX(), getY(), getZ());
                decayMe();
            }
            return false;
        }
    }

    /**
     * @return True if the L2Character has a Party in progress.
     */
    public boolean isInParty() {
        return false;
    }

    /**
     * @return the L2Party object of the L2Character.
     */
    public L2Party getParty() {
        return null;
    }

    /**
     * @param target      The target to test.
     * @param EWeaponType The weapon type to test.
     * @return The Attack Speed of the L2Character (delay (in milliseconds) before next attack).
     */
    public int calculateTimeBetweenAttacks(L2Character target, EWeaponType EWeaponType) {
        switch (EWeaponType) {
            case BOW:
                return 1500 * 345 / getStat().getPAtkSpd();

            default:
                return Formulas.calcPAtkSpd(this, target, getStat().getPAtkSpd());
        }
    }

    /**
     * @return the type of attack, depending of the worn weapon.
     */
    public EWeaponType getAttackType() {
        Weapon weapon = getActiveWeaponItem();
        if (weapon != null) { return weapon.getItemType(); }

        return EWeaponType.NONE;
    }

    /**
     * Add a skill to the L2Character _skills and its Func objects to the calculator set of the L2Character.<BR>
     * <BR>
     * <B><U> Concept</U> :</B><BR>
     * <BR>
     * All skills own by a L2Character are identified in <B>_skills</B><BR>
     * <BR>
     * <B><U> Actions</U> :</B>
     * <ul>
     * <li>Replace oldSkill by newSkill or Add the newSkill</li>
     * <li>If an old skill has been replaced, remove all its Func objects of L2Character calculator set</li>
     * <li>Add Func objects of newSkill to the calculator set of the L2Character</li>
     * </ul>
     * <B><U>Overriden in:</U></B>
     * <ul>
     * <li>L2PcInstance : Save update in the character_skills table of the database</li>
     * </ul>
     *
     * @param newSkill The L2Skill to add to the L2Character
     * @return The L2Skill replaced or null if just added a new L2Skill
     */
    public L2Skill addSkill(L2Skill newSkill) {
        L2Skill oldSkill = null;

        if (newSkill != null) {
            // Replace oldSkill by newSkill or Add the newSkill
            oldSkill = _skills.put(newSkill.getId(), newSkill);

            // If an old skill has been replaced, remove all its Func objects
            if (oldSkill != null) {
                // if skill came with another one, we should delete the other one too.
                if (oldSkill.triggerAnotherSkill()) { removeSkill(oldSkill.getTriggeredId(), true); }

                removeStatsByOwner(oldSkill);
            }
            // Add Func objects of newSkill to the calculator set of the L2Character
            addStatFuncs(newSkill.getStatFuncs(this));

            if (oldSkill != null && _chanceSkills != null) { removeChanceSkill(oldSkill.getId()); }

            if (newSkill.isChance()) { addChanceTrigger(newSkill); }
        }

        return oldSkill;
    }

    /**
     * Remove a skill from the L2Character and its Func objects from calculator set of the L2Character.<BR>
     * <BR>
     * <B><U> Concept</U> :</B><BR>
     * <BR>
     * All skills own by a L2Character are identified in <B>_skills</B><BR>
     * <BR>
     * <B><U> Actions</U> :</B>
     * <ul>
     * <li>Remove the skill from the L2Character _skills</li>
     * <li>Remove all its Func objects from the L2Character calculator set</li>
     * </ul>
     * <B><U> Overriden in </U> :</B>
     * <ul>
     * <li>L2PcInstance : Save update in the character_skills table of the database</li>
     * </ul>
     *
     * @param skill The L2Skill to remove from the L2Character
     * @return The L2Skill removed
     */
    public L2Skill removeSkill(L2Skill skill) {
        if (skill == null) { return null; }

        return removeSkill(skill.getId(), true);
    }

    public L2Skill removeSkill(L2Skill skill, boolean cancelEffect) {
        if (skill == null) { return null; }

        // Remove the skill from the L2Character _skills
        return removeSkill(skill.getId(), cancelEffect);
    }

    public L2Skill removeSkill(int skillId) {
        return removeSkill(skillId, true);
    }

    public L2Skill removeSkill(int skillId, boolean cancelEffect) {
        // Remove the skill from the L2Character _skills
        L2Skill oldSkill = _skills.remove(skillId);

        // Remove all its Func objects from the L2Character calculator set
        if (oldSkill != null) {
            // this is just a fail-safe againts buggers and gm dummies...
            if (oldSkill.triggerAnotherSkill() && oldSkill.getTriggeredId() > 0) { removeSkill(oldSkill.getTriggeredId(), true); }

            // Stop casting if this skill is used right now
            if (_lastSkillCast != null && _isCastingNow) {
                if (oldSkill.getId() == _lastSkillCast.getId()) { abortCast(); }
            }
            if (_lastSimultaneousSkillCast != null && _isCastingSimultaneouslyNow) {
                if (oldSkill.getId() == _lastSimultaneousSkillCast.getId()) { abortCast(); }
            }

            if (cancelEffect || oldSkill.isToggle()) {
                removeStatsByOwner(oldSkill);
                stopSkillEffects(oldSkill.getId());
            }

            if (oldSkill.isChance() && _chanceSkills != null) { removeChanceSkill(oldSkill.getId()); }
        }

        return oldSkill;
    }

    public void removeChanceSkill(int id) {
        if (_chanceSkills == null) { return; }

        for (IChanceSkillTrigger trigger : _chanceSkills.keySet()) {
            if (!(trigger instanceof L2Skill)) { continue; }

            if (((L2Skill) trigger).getId() == id) { _chanceSkills.remove(trigger); }
        }
    }

    public void addChanceTrigger(IChanceSkillTrigger trigger) {
        if (_chanceSkills == null) { _chanceSkills = new ChanceSkillList(this); }

        _chanceSkills.put(trigger, trigger.getTriggeredChanceCondition());
    }

    public void removeChanceEffect(EffectChanceSkillTrigger effect) {
        if (_chanceSkills == null) { return; }

        _chanceSkills.remove(effect);
    }

    public void onStartChanceEffect() {
        if (_chanceSkills == null) { return; }

        _chanceSkills.onStart();
    }

    public void onActionTimeChanceEffect() {
        if (_chanceSkills == null) { return; }

        _chanceSkills.onActionTime();
    }

    public void onExitChanceEffect() {
        if (_chanceSkills == null) { return; }

        _chanceSkills.onExit();
    }

    /**
     * @return A skill array fed with all skills that L2Character owns.
     */
    public final L2Skill[] getAllSkills() {
        return _skills.values().toArray(new L2Skill[_skills.values().size()]);
    }

    public ChanceSkillList getChanceSkills() {
        return _chanceSkills;
    }

    /**
     * Return the level of a skill owned by the L2Character.
     *
     * @param skillId The identifier of the L2Skill whose level must be returned
     * @return The level of the L2Skill identified by skillId
     */
    public int getSkillLevel(int skillId) {
        L2Skill skill = _skills.get(skillId);

        return (skill == null) ? -1 : skill.getLevel();
    }

    /**
     * @param skillId The identifier of the L2Skill to check the knowledge
     * @return True if the skill is known by the L2Character.
     */
    public final L2Skill getKnownSkill(int skillId) {
        return _skills.get(skillId);
    }

    /**
     * Return the number of skills of type(Buff, Debuff, HEAL_PERCENT, MANAHEAL_PERCENT) affecting this L2Character.
     *
     * @return The number of Buffs affecting this L2Character
     */
    public int getBuffCount() {
        return _effects.getBuffCount();
    }

    public int getDanceCount() {
        return _effects.getDanceCount();
    }

    /**
     * Manage the magic skill launching task (MP, HP, Item consummation...) and display the magic skill animation on client.<BR>
     * <BR>
     * <B><U> Actions</U> :</B>
     * <ul>
     * <li>Send MagicSkillLaunched (to display magic skill animation) to all L2PcInstance of L2Charcater _knownPlayers</li>
     * <li>Consumme MP, HP and Item if necessary</li>
     * <li>Send StatusUpdate with MP modification to the L2PcInstance</li>
     * <li>Launch the magic skill in order to calculate its effects</li>
     * <li>If the skill type is PDAM, notify the AI of the target with ATTACK</li>
     * <li>Notify the AI of the L2Character with EVT_FINISH_CASTING</li>
     * </ul>
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : A magic skill casting MUST BE in progress</B></FONT>
     *
     * @param mut
     */
    public void onMagicLaunchedTimer(MagicUseTask mut) {
        L2Skill skill = mut.skill;
        L2Object[] targets = mut.targets;

        if (skill == null || targets == null) {
            abortCast();
            return;
        }

        if (targets.length == 0) {
            switch (skill.getTargetType()) {
                // only AURA-type skills can be cast without target
                case TARGET_AURA:
                case TARGET_FRONT_AURA:
                case TARGET_BEHIND_AURA:
                case TARGET_AURA_UNDEAD:
                    break;
                default:
                    abortCast();
                    return;
            }
        }

        // Escaping from under skill's radius and peace zone check. First version, not perfect in AoE skills.
        int escapeRange = 0;
        if (skill.getEffectRange() > escapeRange) { escapeRange = skill.getEffectRange(); }
        else if (skill.getCastRange() < 0 && skill.getSkillRadius() > 80) { escapeRange = skill.getSkillRadius(); }

        if (targets.length > 0 && escapeRange > 0) {
            int _skiprange = 0;
            int _skipgeo = 0;
            int _skippeace = 0;
            List<L2Character> targetList = new ArrayList<>(targets.length);
            for (L2Object target : targets) {
                if (target instanceof L2Character) {
                    if (!Util.checkIfInRange(escapeRange, this, target, true)) {
                        _skiprange++;
                        continue;
                    }

                    if (skill.getSkillRadius() > 0 && skill.isOffensive() && !PathFinding.getInstance().canSeeTarget(this, target)) {
                        _skipgeo++;
                        continue;
                    }

                    if (skill.isOffensive() && isInsidePeaceZone(this, target)) {
                        _skippeace++;
                        continue;
                    }
                    targetList.add((L2Character) target);
                }
            }

            if (targetList.isEmpty()) {
                if (this instanceof L2PcInstance) {
                    if (_skiprange > 0) { sendPacket(SystemMessage.getSystemMessage(SystemMessageId.DIST_TOO_FAR_CASTING_STOPPED)); }
                    else if (_skipgeo > 0) { sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CANT_SEE_TARGET)); }
                    else if (_skippeace > 0) { sendPacket(SystemMessage.getSystemMessage(SystemMessageId.TARGET_IN_PEACEZONE)); }
                }
                abortCast();
                return;
            }
            mut.targets = targetList.toArray(new L2Character[targetList.size()]);
        }

        // Ensure that a cast is in progress
        // Check if player is using fake death.
        // Potions can be used while faking death.
        if ((mut.simultaneously && !_isCastingSimultaneouslyNow) || (!mut.simultaneously && !_isCastingNow) || (isAlikeDead() && !skill.isPotion())) {
            // now cancels both, simultaneous and normal
            getAI().notifyEvent(ECtrlEvent.EVT_CANCEL);
            return;
        }

        mut.phase = 2;
        if (mut.hitTime == 0) { onMagicHitTimer(mut); }
        else { _skillCast = ThreadPoolManager.getInstance().scheduleEffect(mut, 400); }
    }

    /*
     * Runs in the end of skill casting
	 */
    public void onMagicHitTimer(MagicUseTask mut) {
        L2Skill skill = mut.skill;
        L2Object[] targets = mut.targets;

        if (skill == null || targets == null) {
            abortCast();
            return;
        }

        if (_fusionSkill != null) {
            if (mut.simultaneously) {
                _skillCast2 = null;
                _isCastingSimultaneouslyNow = false;
            }
            else {
                _skillCast = null;
                _isCastingNow = false;
            }
            _fusionSkill.onCastAbort();
            notifyQuestEventSkillFinished(skill, targets[0]);
            return;
        }

        L2Effect mog = getFirstEffect(L2EffectType.SIGNET_GROUND);
        if (mog != null) {
            if (mut.simultaneously) {
                _skillCast2 = null;
                _isCastingSimultaneouslyNow = false;
            }
            else {
                _skillCast = null;
                _isCastingNow = false;
            }
            mog.exit();
            notifyQuestEventSkillFinished(skill, targets[0]);
            return;
        }

        // Go through targets table
        for (L2Object tgt : targets) {
            if (tgt instanceof L2Playable) {
                if (skill.getSkillType() == L2SkillType.BUFF || skill.getSkillType() == L2SkillType.FUSION || skill.getSkillType() == L2SkillType.SEED) {
                    ((L2Character) tgt).sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT).addSkillName(skill));
                }

                if (this instanceof L2PcInstance && tgt instanceof L2Summon) { ((L2Summon) tgt).updateAndBroadcastStatus(1); }
            }
        }

        StatusUpdate su = new StatusUpdate(this);
        boolean isSendStatus = false;

        // Consume MP of the L2Character and Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform
        double mpConsume = getStat().getMpConsume(skill);
        if (mpConsume > 0) {
            if (mpConsume > getCurrentMp()) {
                sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_MP));
                abortCast();
                return;
            }

            getStatus().reduceMp(mpConsume);
            su.addAttribute(StatusUpdate.CUR_MP, (int) getCurrentMp());
            isSendStatus = true;
        }

        // Consume HP if necessary and Send the Server->Client packet StatusUpdate with current HP and MP to all other L2PcInstance to inform
        double hpConsume = skill.getHpConsume();
        if (hpConsume > 0) {
            if (hpConsume > getCurrentHp()) {
                sendPacket(SystemMessage.getSystemMessage(SystemMessageId.NOT_ENOUGH_HP));
                abortCast();
                return;
            }

            getStatus().reduceHp(hpConsume, this, true);
            su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
            isSendStatus = true;
        }

        // Send StatusUpdate with MP modification to the L2PcInstance
        if (isSendStatus) { sendPacket(su); }

        if (this instanceof L2PcInstance) {
            // check for charges
            int charges = ((L2PcInstance) this).getCharges();
            if (skill.getMaxCharges() == 0 && charges < skill.getNumCharges()) {
                SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
                sm.addSkillName(skill);
                sendPacket(sm);
                abortCast();
                return;
            }

            // generate charges if any
            if (skill.getNumCharges() > 0) {
                if (skill.getMaxCharges() > 0) { ((L2PcInstance) this).increaseCharges(skill.getNumCharges(), skill.getMaxCharges()); }
                else { ((L2PcInstance) this).decreaseCharges(skill.getNumCharges()); }
            }
        }

        // Launch the magic skill in order to calculate its effects
        callSkill(mut.skill, mut.targets);

        mut.phase = 3;
        if (mut.hitTime == 0 || mut.coolTime == 0) { onMagicFinalizer(mut); }
        else {
            if (mut.simultaneously) { _skillCast2 = ThreadPoolManager.getInstance().scheduleEffect(mut, mut.coolTime); }
            else { _skillCast = ThreadPoolManager.getInstance().scheduleEffect(mut, mut.coolTime); }
        }
    }

    /*
     * Runs after skill hitTime+coolTime
	 */
    public void onMagicFinalizer(MagicUseTask mut) {
        if (mut.simultaneously) {
            _skillCast2 = null;
            _isCastingSimultaneouslyNow = false;
            return;
        }

        _skillCast = null;
        _isCastingNow = false;
        _castInterruptTime = 0;

        L2Skill skill = mut.skill;
        L2Object target = mut.targets.length > 0 ? mut.targets[0] : null;

        // Attack target after skill use
        if (skill.nextActionIsAttack() && _target instanceof L2Character && _target != this && _target == target && _target.isAttackable()) {
            if (getAI() == null || getAI().getNextIntention() == null || getAI().getNextIntention().getCtrlIntention() != EIntention.MOVE_TO) { getAI().setIntention(EIntention.ATTACK, target); }
        }

        if (skill.isOffensive() && !(skill.getSkillType() == L2SkillType.UNLOCK) && !(skill.getSkillType() == L2SkillType.DELUXE_KEY_UNLOCK)) { getAI().clientStartAutoAttack(); }

        // Notify the AI of the L2Character with EVT_FINISH_CASTING
        getAI().notifyEvent(ECtrlEvent.EVT_FINISH_CASTING);

        notifyQuestEventSkillFinished(skill, target);

        // If the current character is a summon, refresh _currentPetSkill, otherwise if it's a player, refresh _currentSkill and _queuedSkill.
        if (this instanceof L2Playable) {
            boolean isPlayer = this instanceof L2PcInstance;
            L2PcInstance player = getActingPlayer();

            if (isPlayer) {
                // Wipe current cast state.
                player.setCurrentSkill(null, false, false);

                // Check if a skill is queued.
                SkillUseHolder queuedSkill = player.getQueuedSkill();
                if (queuedSkill.getSkill() != null) {
                    ThreadPoolManager.getInstance().executeTask(new QueuedMagicUseTask(player, queuedSkill.getSkill(), queuedSkill.isCtrlPressed(), queuedSkill.isShiftPressed()));
                    player.setQueuedSkill(null, false, false);
                }
            }
            else { player.setCurrentPetSkill(null, false, false); }
        }
    }

    // Quest event ON_SPELL_FINISHED
    protected void notifyQuestEventSkillFinished(L2Skill skill, L2Object target) {
    }

    public Map<Integer, Long> getDisabledSkills() {
        return _disabledSkills;
    }

    /**
     * Enable a skill (remove it from _disabledSkills of the L2Character).<BR>
     * <BR>
     * <B><U> Concept</U> :</B><BR>
     * <BR>
     * All skills disabled are identified by their skillId in <B>_disabledSkills</B> of the L2Character
     *
     * @param skill The L2Skill to enable
     */
    public void enableSkill(L2Skill skill) {
        if (skill == null) { return; }

        _disabledSkills.remove(skill.getReuseHashCode());
    }

    /**
     * Disable this skill id for the duration of the delay in milliseconds.
     *
     * @param skill
     * @param delay (seconds * 1000)
     */
    public void disableSkill(L2Skill skill, long delay) {
        if (skill == null) { return; }

        _disabledSkills.put(skill.getReuseHashCode(), (delay > 10) ? System.currentTimeMillis() + delay : Long.MAX_VALUE);
    }

    /**
     * Check if a skill is disabled. All skills disabled are identified by their reuse hashcodes in <B>_disabledSkills</B>.
     *
     * @param skill The L2Skill to check
     * @return true if the skill is currently disabled.
     */
    public boolean isSkillDisabled(L2Skill skill) {
        if (_disabledSkills.isEmpty()) { return false; }

        if (skill == null || isAllSkillsDisabled()) { return true; }

        int hashCode = skill.getReuseHashCode();

        Long timeStamp = _disabledSkills.get(hashCode);
        if (timeStamp == null) { return false; }

        if (timeStamp < System.currentTimeMillis()) {
            _disabledSkills.remove(hashCode);
            return false;
        }

        return true;
    }

    /**
     * Disable all skills (set _allSkillsDisabled to True).
     */
    public void disableAllSkills() {
        _allSkillsDisabled = true;
    }

    /**
     * Enable all skills (set _allSkillsDisabled to False).
     */
    public void enableAllSkills() {
        _allSkillsDisabled = false;
    }

    /**
     * Launch the magic skill and calculate its effects on each target contained in the targets table.
     *
     * @param skill   The L2Skill to use
     * @param targets The table of L2Object targets
     */
    public void callSkill(L2Skill skill, L2Object[] targets) {
        try {
            // Check if the toggle skill effects are already in progress on the L2Character
            if (skill.isToggle() && getFirstEffect(skill.getId()) != null) { return; }

            // Initial checks
            for (L2Object trg : targets) {
                if (!(trg instanceof L2Character)) { continue; }

                // Set some values inside target's instance for later use
                L2Character target = (L2Character) trg;

                if (this instanceof L2Playable) {
                    // Raidboss curse.
                    if (!Config.RAID_DISABLE_CURSE) {
                        boolean isVictimTargetBoss = false;

                        // If the skill isn't offensive, we check extra things such as target's target.
                        if (!skill.isOffensive()) {
                            L2Object victimTarget = (target.hasAI()) ? target.getAI().getTarget() : null;
                            if (victimTarget != null) { isVictimTargetBoss = victimTarget instanceof L2Character && ((L2Character) victimTarget).isRaid() && getLevel() > ((L2Character) victimTarget).getLevel() + 8; }
                        }

                        // Target must be either a raid type, or if the skill is beneficial it checks the target's target.
                        if ((target.isRaid() && getLevel() > target.getLevel() + 8) || isVictimTargetBoss) {
                            L2Skill curse = FrequentSkill.RAID_CURSE.getSkill();
                            if (curse != null) {
                                // Send visual and skill effects. Caster is the victim.
                                broadcastPacket(new MagicSkillUse(this, this, curse.getId(), curse.getLevel(), 300, 0));
                                curse.getEffects(this, this);
                            }
                            return;
                        }
                    }

                    // Check if over-hit is possible
                    if (skill.isOverhit() && target instanceof L2Attackable) { ((L2Attackable) target).overhitEnabled(true); }
                }

                switch (skill.getSkillType()) {
                    case COMMON_CRAFT: // Crafting does not trigger any chance skills.
                    case DWARVEN_CRAFT:
                        break;

                    default: // Launch weapon Special ability skill effect if available
                        if (getActiveWeaponItem() != null && !target._isDead) {
                            if (this instanceof L2PcInstance && !getActiveWeaponItem().getSkillEffects(this, target, skill).isEmpty()) {
                                sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_BEEN_ACTIVATED).addSkillName(skill));
                            }
                        }

                        // Maybe launch chance skills on us
                        if (_chanceSkills != null) { _chanceSkills.onSkillHit(target, false, skill.isMagic(), skill.isOffensive()); }

                        // Maybe launch chance skills on target
                        if (target._chanceSkills != null) {
                            target._chanceSkills.onSkillHit(this, true, skill.isMagic(), skill.isOffensive());
                        }
                }
            }

            // Launch the magic skill and calculate its effects
            ISkillHandler handler = SkillHandler.getInstance().getSkillHandler(skill.getSkillType());
            if (handler != null) { handler.useSkill(this, skill, targets); }
            else { skill.useSkill(this, targets); }

            L2PcInstance player = getActingPlayer();
            if (player != null) {
                for (L2Object target : targets) {
                    // EVT_ATTACKED and PvPStatus
                    if (target instanceof L2Character) {
                        if (skill.isOffensive()) {
                            if (target instanceof L2Playable) {
                                // Signets are a special case, casted on target_self but don't harm self
                                if (skill.getSkillType() != L2SkillType.SIGNET && skill.getSkillType() != L2SkillType.SIGNET_CASTTIME) {
                                    ((L2Character) target).getAI().clientStartAutoAttack();

                                    // attack of the own pet does not flag player
                                    if (player.getPet() != target) { player.updatePvPStatus((L2Character) target); }
                                }
                            }
                            // Add attacker into list
                            else if (target instanceof L2Attackable && skill.getId() != 51) { ((L2Attackable) target).addAttackerToAttackByList(this); }

                            // notify target AI about the attack
                            if (((L2Character) target).hasAI()) {
                                switch (skill.getSkillType()) {
                                    case AGGREDUCE:
                                    case AGGREDUCE_CHAR:
                                    case AGGREMOVE:
                                        break;

                                    default:
                                        ((L2Character) target).getAI().notifyEvent(ECtrlEvent.EVT_ATTACKED, this);
                                }
                            }
                        }
                        else {
                            if (target instanceof L2PcInstance) {
                                // Casting non offensive skill on player with pvp flag set or with karma
                                if (!(target.equals(this) || target.equals(player)) && (((L2PcInstance) target).getPvpFlag() > 0 || ((L2PcInstance) target).getKarma() > 0)) { player.updatePvPStatus(); }
                            }
                            else if (target instanceof L2Attackable && !((L2Attackable) target).isGuard()) {
                                switch (skill.getSkillType()) {
                                    case SUMMON:
                                    case BEAST_FEED:
                                    case UNLOCK:
                                    case UNLOCK_SPECIAL:
                                    case DELUXE_KEY_UNLOCK:
                                        break;

                                    default:
                                        player.updatePvPStatus();
                                }
                            }
                        }

                        switch (skill.getTargetType()) {
                            case TARGET_CORPSE_MOB:
                            case TARGET_AREA_CORPSE_MOB:
                                if (((L2Character) target)._isDead) { ((L2Npc) target).endDecayTask(); }
                                break;
                        }
                    }
                }

                // Mobs in range 1000 see spell
                for (L2Npc npcMob : player.getKnownList().getKnownTypeInRadius(L2Npc.class, 1000)) {
                    List<Quest> quests = npcMob.getTemplate().getEventQuests(EventType.ON_SKILL_SEE);
                    if (quests != null) { for (Quest quest : quests) { quest.notifySkillSee(npcMob, player, skill, targets, this instanceof L2Summon); } }
                }
            }

            // Notify AI
            if (skill.isOffensive()) {
                switch (skill.getSkillType()) {
                    case AGGREDUCE:
                    case AGGREDUCE_CHAR:
                    case AGGREMOVE:
                        break;

                    default:
                        for (L2Object target : targets) {
                            // notify target AI about the attack
                            if (target instanceof L2Character && ((L2Character) target).hasAI()) { ((L2Character) target).getAI().notifyEvent(ECtrlEvent.EVT_ATTACKED, this); }
                        }
                        break;
                }
            }
        }
        catch (Exception e) {
            LOGGER.error("{}: callSkill() failed on skill id: {}", getClass().getSimpleName(), skill.getId(), e);
        }
    }

    /**
     * @param target Target to check.
     * @return True if the L2Character is behind the target and can't be seen.
     */
    public boolean isBehind(L2Character target) {
        if (target == null) { return false; }

        double maxAngleDiff = 60;

        double angleChar = Util.calculateAngleFrom(this, target);
        double angleTarget = Util.convertHeadingToDegree(target._heading);
        double angleDiff = angleChar - angleTarget;

        if (angleDiff <= -360 + maxAngleDiff) { angleDiff += 360; }

        if (angleDiff >= 360 - maxAngleDiff) { angleDiff -= 360; }

        return Math.abs(angleDiff) <= maxAngleDiff;
    }

    public boolean isBehindTarget() {
        L2Object target = _target;
        if (target instanceof L2Character) { return isBehind((L2Character) target); }

        return false;
    }

    /**
     * @param target Target to check.
     * @return True if the target is facing the L2Character.
     */
    public boolean isInFrontOf(L2Character target) {
        if (target == null) { return false; }

        double maxAngleDiff = 60;

        double angleTarget = Util.calculateAngleFrom(target, this);
        double angleChar = Util.convertHeadingToDegree(target._heading);
        double angleDiff = angleChar - angleTarget;

        if (angleDiff <= -360 + maxAngleDiff) { angleDiff += 360; }

        if (angleDiff >= 360 - maxAngleDiff) { angleDiff -= 360; }

        return Math.abs(angleDiff) <= maxAngleDiff;
    }

    /**
     * @param target   Target to check.
     * @param maxAngle The angle to check.
     * @return true if target is in front of L2Character (shield def etc)
     */
    public boolean isFacing(L2Object target, int maxAngle) {
        if (target == null) { return false; }

        double maxAngleDiff = maxAngle / 2;
        double angleTarget = Util.calculateAngleFrom(this, target);
        double angleChar = Util.convertHeadingToDegree(_heading);
        double angleDiff = angleChar - angleTarget;

        if (angleDiff <= -360 + maxAngleDiff) { angleDiff += 360; }

        if (angleDiff >= 360 - maxAngleDiff) { angleDiff -= 360; }

        return Math.abs(angleDiff) <= maxAngleDiff;
    }

    public boolean isInFrontOfTarget() {
        L2Object target = _target;
        if (target instanceof L2Character) { return isInFrontOf((L2Character) target); }

        return false;
    }

    /**
     * @return the level modifier.
     */
    public double getLevelMod() {
        return (100.0 - 11 + getLevel()) / 100.0;
    }

    public final void setSkillCast(Future<?> newSkillCast) {
        _skillCast = newSkillCast;
    }

    /**
     * @param target Target to check.
     * @return a Random Damage in function of the weapon.
     */
    public final int getRandomDamage(L2Character target) {
        Weapon weaponItem = getActiveWeaponItem();
        if (weaponItem == null) { return 5 + (int) Math.sqrt(getLevel()); }

        return weaponItem.getRandomDamage();
    }

    @Override
    public String toString() {
        return "mob " + getObjectId();
    }

    public long getAttackEndTime() {
        return _attackEndTime;
    }

    /**
     * @return the level of the L2Character.
     */
    public abstract int getLevel();

    // =========================================================
    // Stat - NEED TO REMOVE ONCE L2CHARSTAT IS COMPLETE
    // Property - Public
    public final double calcStat(Stats stat, double init, L2Character target, L2Skill skill) {
        return getStat().calcStat(stat, init, target, skill);
    }

    // Property - Public
    public int getCON() {
        return getStat().getCON();
    }

    public int getDEX() {
        return getStat().getDEX();
    }

    public int getINT() {
        return getStat().getINT();
    }

    public int getMEN() {
        return getStat().getMEN();
    }

    public int getSTR() {
        return getStat().getSTR();
    }

    public int getWIT() {
        return getStat().getWIT();
    }

    public int getAccuracy() {
        return getStat().getAccuracy();
    }

    public final float getAttackSpeedMultiplier() {
        return getStat().getAttackSpeedMultiplier();
    }

    public int getCriticalHit(L2Character target, L2Skill skill) {
        return getStat().getCriticalHit(target, skill);
    }

    public int getEvasionRate(L2Character target) {
        return getStat().getEvasionRate(target);
    }

    public int getMDef(L2Character target, L2Skill skill) {
        return getStat().getMDef(target, skill);
    }

    public int getPDef(L2Character target) {
        return getStat().getPDef(target);
    }

    public final int getShldDef() {
        return getStat().getShldDef();
    }

    public final int getPhysicalAttackRange() {
        return getStat().getPhysicalAttackRange();
    }

    public int getPAtk(L2Character target) {
        return getStat().getPAtk(target);
    }

    public int getPAtkSpd() {
        return getStat().getPAtkSpd();
    }

    public int getMAtk(L2Character target, L2Skill skill) {
        return getStat().getMAtk(target, skill);
    }

    public int getMAtkSpd() {
        return getStat().getMAtkSpd();
    }

    public final int getMCriticalHit(L2Character target, L2Skill skill) {
        return getStat().getMCriticalHit(target, skill);
    }

    public int getMaxMp() {
        return getStat().getMaxMp();
    }

    public int getMaxHp() {
        return getStat().getMaxHp();
    }

    public final int getMaxCp() {
        return getStat().getMaxCp();
    }

    public float getMovementSpeedMultiplier() {
        return getStat().getMovementSpeedMultiplier();
    }

    public double getPAtkAnimals(L2Character target) {
        return getStat().getPAtkAnimals(target);
    }

    public double getPAtkDragons(L2Character target) {
        return getStat().getPAtkDragons(target);
    }

    public double getPAtkInsects(L2Character target) {
        return getStat().getPAtkInsects(target);
    }

    public double getPAtkMonsters(L2Character target) {
        return getStat().getPAtkMonsters(target);
    }

    public double getPAtkPlants(L2Character target) {
        return getStat().getPAtkPlants(target);
    }

    public double getPAtkGiants(L2Character target) {
        return getStat().getPAtkGiants(target);
    }

    public double getPAtkMagicCreatures(L2Character target) {
        return getStat().getPAtkMagicCreatures(target);
    }

    public double getPDefAnimals(L2Character target) {
        return getStat().getPDefAnimals(target);
    }

    public double getPDefDragons(L2Character target) {
        return getStat().getPDefDragons(target);
    }

    public double getPDefInsects(L2Character target) {
        return getStat().getPDefInsects(target);
    }

    public double getPDefMonsters(L2Character target) {
        return getStat().getPDefMonsters(target);
    }

    public double getPDefPlants(L2Character target) {
        return getStat().getPDefPlants(target);
    }

    public double getPDefGiants(L2Character target) {
        return getStat().getPDefGiants(target);
    }

    public double getPDefMagicCreatures(L2Character target) {
        return getStat().getPDefMagicCreatures(target);
    }

    public int getRunSpeed() {
        return getStat().getRunSpeed();
    }

    public final int getWalkSpeed() {
        return getStat().getWalkSpeed();
    }

    // =========================================================
    // Status - NEED TO REMOVE ONCE L2CHARTATUS IS COMPLETE
    // Method - Public
    public void addStatusListener(L2Character object) {
        getStatus().addStatusListener(object);
    }

    public void reduceCurrentHp(double i, L2Character attacker, L2Skill skill) {
        reduceCurrentHp(i, attacker, true, false, skill);
    }

    public void reduceCurrentHpByDOT(double i, L2Character attacker, L2Skill skill) {
        reduceCurrentHp(i, attacker, !skill.isToggle(), true, skill);
    }

    public void reduceCurrentHp(double i, L2Character attacker, boolean awake, boolean isDOT, L2Skill skill) {
        if (_champion && Config.CHAMPION_HP != 0) { getStatus().reduceHp(i / Config.CHAMPION_HP, attacker, awake, isDOT, false); }
        else { getStatus().reduceHp(i, attacker, awake, isDOT, false); }
    }

    public void reduceCurrentMp(double i) {
        getStatus().reduceMp(i);
    }

    public void removeStatusListener(L2Character object) {
        getStatus().removeStatusListener(object);
    }

    protected void stopHpMpRegeneration() {
        getStatus().stopHpMpRegeneration();
    }

    // Property - Public
    public final double getCurrentCp() {
        return getStatus().getCurrentCp();
    }

    public final void setCurrentCp(double newCp) {
        getStatus().setCurrentCp(newCp);
    }

    public final double getCurrentHp() {
        return getStatus().getCurrentHp();
    }

    public final void setCurrentHp(double newHp) {
        getStatus().setCurrentHp(newHp);
    }

    public final void setCurrentHpMp(double newHp, double newMp) {
        getStatus().setCurrentHpMp(newHp, newMp);
    }

    public final double getCurrentMp() {
        return getStatus().getCurrentMp();
    }

    public final void setCurrentMp(double newMp) {
        getStatus().setCurrentMp(newMp);
    }

    public boolean isChampion() {
        return _champion;
    }

    public void setChampion(boolean champ) {
        _champion = champ;
    }

    /**
     * Send system message about damage.<BR>
     * <BR>
     * <B><U> Overriden in </U> :</B>
     * <ul>
     * <li>L2PcInstance</li>
     * <li>L2SummonInstance</li>
     * <li>L2PetInstance</li>
     * </ul>
     *
     * @param target
     * @param damage
     * @param mcrit
     * @param pcrit
     * @param miss
     */
    public void sendDamageMessage(L2Character target, int damage, boolean mcrit, boolean pcrit, boolean miss) {
    }

    public FusionSkill getFusionSkill() {
        return _fusionSkill;
    }

    public void setFusionSkill(FusionSkill fb) {
        _fusionSkill = fb;
    }

    public int getAttackElementValue(byte attackAttribute) {
        return getStat().getAttackElementValue(attackAttribute);
    }

    // =========================================================

    public int getDefenseElementValue(byte defenseAttribute) {
        return getStat().getDefenseElementValue(defenseAttribute);
    }

    /**
     * Check if target is affected with special buff
     *
     * @param flag int
     * @return boolean
     * @see CharEffectList#isAffected(L2EffectFlag)
     */
    public boolean isAffected(L2EffectFlag flag) {
        return _effects.isAffected(flag);
    }

    /**
     * Check player max buff count
     *
     * @return max buff count
     */
    public int getMaxBuffCount() {
        return Config.BUFFS_MAX_AMOUNT + Math.max(0, getSkillLevel(L2Skill.SKILL_DIVINE_INSPIRATION));
    }

    /**
     * @return a multiplier based on weapon random damage.
     */
    public final double getRandomDamageMultiplier() {
        Weapon activeWeapon = getActiveWeaponItem();
        int random;

        if (activeWeapon != null) { random = activeWeapon.getRandomDamage(); }
        else { random = 5 + (int) Math.sqrt(getLevel()); }

        return 1 + ((double) Rnd.get(0 - random, random) / 100);
    }

    public void disableCoreAI(boolean val) {
        _AIdisabled = val;
    }

    public boolean isCoreAIDisabled() {
        return _AIdisabled;
    }

    /**
     * @return true if the character is located in an arena (aka a PvP zone which isn't a siege).
     */
    public boolean isInArena() {
        return false;
    }

    @Override
    public boolean isCharacter() { return true; }

    /** Task launching the function useMagic() */
    private static class QueuedMagicUseTask implements Runnable {
        L2PcInstance _currPlayer;
        L2Skill _queuedSkill;
        boolean _isCtrlPressed;
        boolean _isShiftPressed;

        public QueuedMagicUseTask(L2PcInstance currPlayer, L2Skill queuedSkill, boolean isCtrlPressed, boolean isShiftPressed) {
            _currPlayer = currPlayer;
            _queuedSkill = queuedSkill;
            _isCtrlPressed = isCtrlPressed;
            _isShiftPressed = isShiftPressed;
        }

        @Override
        public void run() {
            try {
                _currPlayer.useMagic(_queuedSkill, _isCtrlPressed, _isShiftPressed);
            }
            catch (Exception e) {
                LOGGER.error("Failed executing QueuedMagicUseTask.", e);
            }
        }
    }

    /**
     * This class group all mouvement data.<BR>
     * <BR>
     * <B><U> Data</U> :</B>
     * <ul>
     * <li>_moveTimestamp : Last time position update</li>
     * <li>_xDestination, _yDestination, _zDestination : Position of the destination</li>
     * <li>_xMoveFrom, _yMoveFrom, _zMoveFrom : Position of the origin</li>
     * <li>_moveStartTime : Start time of the movement</li>
     * <li>_ticksToMove : Nb of ticks between the start and the destination</li>
     * <li>_xSpeedTicks, _ySpeedTicks : Speed in unit/ticks</li>
     * </ul>
     */
    public static class MoveData {
        // when we retrieve x/y/z we use GameTimeControl.getGameTicks()
        // if we are moving, but move timestamp==gameticks, we don't need
        // to recalculate position
        public long _moveStartTime;
        public long _moveTimestamp; // last update
        public int _xDestination;
        public int _yDestination;
        public int _zDestination;
        public double _xAccurate; // otherwise there would be rounding errors
        public double _yAccurate;
        public double _zAccurate;
        public int _heading;

        public boolean disregardingGeodata;
        public int onGeodataPathIndex;
        public List<Location> geoPath;
        public int geoPathAccurateTx;
        public int geoPathAccurateTy;
        public int geoPathGtx;
        public int geoPathGty;
    }

    /** Task for potion and herb queue */
    private static class UsePotionTask implements Runnable {
        private final L2Character _activeChar;
        private final L2Skill _skill;

        UsePotionTask(L2Character activeChar, L2Skill skill) {
            _activeChar = activeChar;
            _skill = skill;
        }

        @Override
        public void run() {
            _activeChar.doSimultaneousCast(_skill);
        }
    }

    /**
     * Task lauching the function onHitTimer().<BR>
     * <BR>
     * <B><U> Actions</U> :</B>
     * <ul>
     * <li>If the attacker/target is dead or use fake death, notify the AI with EVT_CANCEL and send ActionFailed (if attacker is a L2PcInstance)</li>
     * <li>If attack isn't aborted, send a message system (critical hit, missed...) to attacker/target if they are L2PcInstance</li>
     * <li>If attack isn't aborted and hit isn't missed, reduce HP of the target and calculate reflection damage to reduce HP of attacker if necessary</li>
     * <li>if attack isn't aborted and hit isn't missed, manage attack or cast break of the target (calculating rate, sending message...)</li>
     * </ul>
     */
    class HitTask implements Runnable {
        L2Character _hitTarget;
        int _damage;
        boolean _crit;
        boolean _miss;
        byte _shld;
        boolean _soulshot;

        public HitTask(L2Character target, int damage, boolean crit, boolean miss, boolean soulshot, byte shld) {
            _hitTarget = target;
            _damage = damage;
            _crit = crit;
            _shld = shld;
            _miss = miss;
            _soulshot = soulshot;
        }

        @Override
        public void run() {
            onHitTimer(_hitTarget, _damage, _crit, _miss, _soulshot, _shld);
        }
    }

    /** Task lauching the magic skill phases */
    class MagicUseTask implements Runnable {
        L2Object[] targets;
        L2Skill skill;
        int hitTime;
        int coolTime;
        int phase;
        boolean simultaneously;

        public MagicUseTask(L2Object[] tgts, L2Skill s, int hit, int coolT, boolean simultaneous) {
            targets = tgts;
            skill = s;
            phase = 1;
            hitTime = hit;
            coolTime = coolT;
            simultaneously = simultaneous;
        }

        @Override
        public void run() {
            try {
                switch (phase) {
                    case 1:
                        onMagicLaunchedTimer(this);
                        break;
                    case 2:
                        onMagicHitTimer(this);
                        break;
                    case 3:
                        onMagicFinalizer(this);
                        break;
                    default:
                        break;
                }
            }
            catch (Exception e) {
                LOGGER.error("Failed executing MagicUseTask.", e);
                if (simultaneously) { setIsCastingSimultaneouslyNow(false); }
                else { setIsCastingNow(false); }
            }
        }
    }

    /** Task of AI notification */
    public class NotifyAITask implements Runnable {
        private final ECtrlEvent _evt;

        NotifyAITask(ECtrlEvent evt) {
            _evt = evt;
        }

        @Override
        public void run() {
            try {
                getAI().notifyEvent(_evt, null);
            }
            catch (Throwable t) {
                LOGGER.error("", t);
            }
        }
    }

    /** Task lauching the magic skill phases */
    class FlyToLocationTask implements Runnable {
        private final L2Object _tgt;
        private final L2Character _actor;
        private final L2Skill _skill;

        public FlyToLocationTask(L2Character actor, L2Object target, L2Skill skill) {
            _actor = actor;
            _tgt = target;
            _skill = skill;
        }

        @Override
        public void run() {
            try {
                FlyType _flyType;

                _flyType = FlyType.valueOf(_skill.getFlyType());

                broadcastPacket(new FlyToLocation(_actor, _tgt, _flyType));
                getPosition().setXYZ(_tgt.getX(), _tgt.getY(), _tgt.getZ());
            }
            catch (Exception e) {
                LOGGER.error("Failed executing FlyToLocationTask.", e);
            }
        }
    }

    public boolean isHolyArtefact() { return false; }
}