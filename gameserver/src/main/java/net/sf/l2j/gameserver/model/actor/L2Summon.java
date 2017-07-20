package net.sf.l2j.gameserver.model.actor;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ai.EIntention;
import net.sf.l2j.gameserver.ai.model.L2CharacterAI;
import net.sf.l2j.gameserver.ai.model.L2SummonAI;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.geoengine.PathFinding;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.handler.ItemHandler;
import net.sf.l2j.gameserver.model.*;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.model.skill.ESkillTargetType;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SummonInstance;
import net.sf.l2j.gameserver.model.actor.knownlist.SummonKnownList;
import net.sf.l2j.gameserver.model.actor.stat.SummonStat;
import net.sf.l2j.gameserver.model.actor.status.SummonStatus;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.base.Experience;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Weapon;
import net.sf.l2j.gameserver.model.item.type.ActionType;
import net.sf.l2j.gameserver.model.itemcontainer.PetInventory;
import net.sf.l2j.gameserver.model.olympiad.OlympiadGameManager;
import net.sf.l2j.gameserver.model.world.L2WorldRegion;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.AbstractNpcInfo.SummonInfo;
import net.sf.l2j.gameserver.network.client.game_to_client.*;

public abstract class L2Summon extends L2Playable {
    private L2PcInstance _owner;
    private boolean _follow = true;
    private boolean _previousFollowStatus = true;

    private int _shotsMask = 0;

    public L2Summon(int objectId, NpcTemplate template, L2PcInstance owner) {
        super(objectId, template);

        _showSummonAnimation = true;
        _owner = owner;
        ai = new L2SummonAI(this);

        getPosition().setXYZInvisible(owner.getX() + 50, owner.getY() + 100, owner.getZ() + 100);
    }

    @Override
    public void initKnownList() {
        setKnownList(new SummonKnownList(this));
    }

    @Override
    public final SummonKnownList getKnownList() {
        return (SummonKnownList) super.getKnownList();
    }

    @Override
    public void initCharStat() {
        setStat(new SummonStat(this));
    }

    @Override
    public SummonStat getStat() {
        return (SummonStat) super.getStat();
    }

    @Override
    public void initCharStatus() {
        setStatus(new SummonStatus(this));
    }

    @Override
    public SummonStatus getStatus() {
        return (SummonStatus) super.getStatus();
    }

    @Override
    public L2CharacterAI getAI() {
        L2CharacterAI ai = this.ai;
        if (ai == null) {
            synchronized (this) {
                if (this.ai == null) { this.ai = new L2SummonAI(this); }

                return this.ai;
            }
        }
        return ai;
    }

    @Override
    public NpcTemplate getTemplate() {
        return (NpcTemplate) super.getTemplate();
    }

    // this defines the action buttons, 1 for Summon, 2 for Pets
    public abstract int getSummonType();

    @Override
    public void updateAbnormalEffect() {
        for (L2PcInstance player : getKnownList().getKnownType(L2PcInstance.class)) { player.sendPacket(new SummonInfo(this, player, 1)); }
    }

    /**
     * @return Returns the mountable.
     */
    public boolean isMountable() {
        return false;
    }

    @Override
    public void onAction(L2PcInstance player) {
        // Set the target of the L2PcInstance player
        if (player.getTarget() != this) { player.setTarget(this); }
        else if (player == _owner) {
            // Calculate the distance between the L2PcInstance and the L2Npc
            if (!canInteract(player)) {
                // Notify the L2PcInstance AI with INTERACT
                player.getAI().setIntention(EIntention.INTERACT, this);
            }
            else {
                // Rotate the player to face the instance
                player.sendPacket(new MoveToPawn(player, this, L2Npc.INTERACTION_DISTANCE));

                player.sendPacket(new PetStatusShow(this));

                // Send ActionFailed to the player in order to avoid he stucks
                player.sendPacket(ActionFailed.STATIC_PACKET);
            }
        }
        else {
            if (isAutoAttackable(player)) {
                if (PathFinding.getInstance().canSeeTarget(player, this)) {
                    player.getAI().setIntention(EIntention.ATTACK, this);
                    player.onActionRequest();
                }
            }
            else {
                // Rotate the player to face the instance
                player.sendPacket(new MoveToPawn(player, this, L2Npc.INTERACTION_DISTANCE));

                // Send ActionFailed to the player in order to avoid he stucks
                player.sendPacket(ActionFailed.STATIC_PACKET);

                if (PathFinding.getInstance().canSeeTarget(player, this)) { player.getAI().setIntention(EIntention.FOLLOW, this); }
            }
        }
    }

    @Override
    public void onActionShift(L2PcInstance player) {
        if (player.isGM()) {
            final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
            html.setFile("data/html/admin/petinfo.htm");
            html.replace("%name%", getName() == null ? "N/A" : getName());
            html.replace("%level%", getLevel());
            html.replace("%exp%", getStat().getExp());
            html.replace("%owner%", " <a action=\"bypass -h admin_character_info " + getActingPlayer().getName() + "\">" + getActingPlayer().getName() + "</a>");
            html.replace("%class%", getClass().getSimpleName());
            html.replace("%ai%", hasAI() ? getAI().getIntention().name() : "NULL");
            html.replace("%hp%", (int) getStatus().getCurrentHp() + "/" + getStat().getMaxHp());
            html.replace("%mp%", (int) getStatus().getCurrentMp() + "/" + getStat().getMaxMp());
            html.replace("%karma%", getKarma());
            html.replace("%undead%", isUndead() ? "yes" : "no");

            if (this instanceof L2PetInstance) {
                html.replace("%inv%", " <a action=\"bypass admin_show_pet_inv " + getActingPlayer().getObjectId() + "\">view</a>");
                html.replace("%food%", ((L2PetInstance) this).getCurrentFed() + "/" + ((L2PetInstance) this).getPetLevelData().getPetMaxFeed());
                html.replace("%load%", ((L2PetInstance) this).getInventory().getTotalWeight() + "/" + ((L2PetInstance) this).getMaxLoad());
            }
            else {
                html.replace("%inv%", "none");
                html.replace("%food%", "N/A");
                html.replace("%load%", "N/A");
            }

            player.sendPacket(html);
            player.sendPacket(ActionFailed.STATIC_PACKET);
        }
        super.onActionShift(player);
    }

    public long getExpForThisLevel() {
        if (getLevel() >= Experience.LEVEL.length) { return 0; }

        return Experience.LEVEL[getLevel()];
    }

    public long getExpForNextLevel() {
        if (getLevel() >= Experience.LEVEL.length - 1) { return 0; }

        return Experience.LEVEL[getLevel() + 1];
    }

    @Override
    public final int getKarma() {
        return getOwner() != null ? getOwner().getKarma() : 0;
    }

    @Override
    public final byte getPvpFlag() {
        return getOwner() != null ? getOwner().getPvpFlag() : 0;
    }

    public final int getTeam() {
        return getOwner() != null ? getOwner().getTeam() : 0;
    }

    public final L2PcInstance getOwner() {
        return _owner;
    }

    public void setOwner(L2PcInstance newOwner) {
        _owner = newOwner;
    }

    public final int getNpcId() {
        return getTemplate().getNpcId();
    }

    public int getMaxLoad() {
        return 0;
    }

    public int getSoulShotsPerHit() {
        return getTemplate().getAIData().getSsCount();
    }

    public int getSpiritShotsPerHit() {
        return getTemplate().getAIData().getSpsCount();
    }

    public void followOwner() {
        setFollowStatus(true);
    }

    @Override
    public boolean doDie(L2Character killer) {
        if (!super.doDie(killer)) { return false; }

        // Disable beastshots
        for (int itemId : getOwner().getAutoSoulShot()) {
            switch (ItemTable.getInstance().getTemplate(itemId).getDefaultAction()) {
                case summon_soulshot:
                case summon_spiritshot:
                    getOwner().disableAutoShot(itemId);
                    break;
            }
        }
        return true;
    }

    @Override
    public void onDecay() {
        deleteMe(_owner);
    }

    @Override
    public void broadcastStatusUpdate() {
        super.broadcastStatusUpdate();
        updateAndBroadcastStatus(1);
    }

    public void deleteMe(L2PcInstance owner) {
        owner.sendPacket(new PetDelete(getSummonType(), getObjectId()));

        decayMe();
        getKnownList().removeAllKnownObjects();
        owner.setPet(null);
        super.deleteMe();
    }

    public void unSummon(L2PcInstance owner) {
        if (isVisible() && !isDead()) {
            abortCast();
            abortAttack();

            stopHpMpRegeneration();
            getAI().stopFollow();

            owner.sendPacket(new PetDelete(getSummonType(), getObjectId()));

            store();
            owner.setPet(null);

            // Stop AI tasks
            if (hasAI()) { getAI().stopAITask(); }

            stopAllEffects();
            L2WorldRegion oldRegion = getWorldRegion();

            decayMe();

            if (oldRegion != null) { oldRegion.removeFromZones(this); }

            getKnownList().removeAllKnownObjects();
            setTarget(null);

            // Disable beastshots
            for (int itemId : owner.getAutoSoulShot()) {
                switch (ItemTable.getInstance().getTemplate(itemId).getDefaultAction()) {
                    case summon_soulshot:
                    case summon_spiritshot:
                        owner.disableAutoShot(itemId);
                        break;
                }
            }
        }
    }

    public int getAttackRange() {
        return 36;
    }

    public boolean getFollowStatus() {
        return _follow;
    }

    public void setFollowStatus(boolean state) {
        _follow = state;
        if (_follow) { getAI().setIntention(EIntention.FOLLOW, getOwner()); }
        else { getAI().setIntention(EIntention.IDLE, null); }
    }

    @Override
    public boolean isAutoAttackable(L2Character attacker) {
        return _owner.isAutoAttackable(attacker);
    }

    public int getControlItemId() {
        return 0;
    }

    public Weapon getActiveWeapon() {
        return null;
    }

    @Override
    public PetInventory getInventory() {
        return null;
    }

    public void store() {
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

    /**
     * Return True if the L2Summon is invulnerable or if the summoner is in spawn protection.<BR>
     * <BR>
     */
    @Override
    public boolean isInvul() {
        return super.isInvul() || getOwner().isSpawnProtected();
    }

    /**
     * Return the L2Party object of its L2PcInstance owner or null.<BR>
     * <BR>
     */
    @Override
    public L2Party getParty() {
        if (_owner == null) { return null; }

        return _owner.getParty();
    }

    /**
     * Return True if the L2Character has a Party in progress.<BR>
     * <BR>
     */
    @Override
    public boolean isInParty() {
        if (_owner == null) { return false; }

        return _owner.getParty() != null;
    }

    /**
     * Check if the active L2Skill can be casted.<BR>
     * <BR>
     * <B><U> Actions</U> :</B><BR>
     * <BR>
     * <li>Check if the target is correct</li> <li>Check if the target is in the skill cast range</li> <li>Check if the summon owns enough HP and MP to cast the skill</li> <li>Check if all skills are enabled and this skill is enabled</li><BR>
     * <BR>
     * <li>Check if the skill is active</li><BR>
     * <BR>
     * <li>Notify the AI with CAST and target</li><BR>
     * <BR>
     *
     * @param skill    The L2Skill to use
     * @param forceUse used to force ATTACK on players
     * @param dontMove used to prevent movement, if not in range
     */
    @Override
    public boolean useMagic(L2Skill skill, boolean forceUse, boolean dontMove) {
        if (skill == null || isDead()) { return false; }

        // Check if the skill is active and ignore the passive skill request
        if (skill.isPassive()) { return false; }

        // ************************************* Check Casting in Progress *******************************************

        // If a skill is currently being used
        if (isCastingNow()) { return false; }

        // Set current pet skill
        getOwner().setCurrentPetSkill(skill, forceUse, dontMove);

        // ************************************* Check Target *******************************************

        // Get the target for the skill
        L2Object target = null;

        switch (skill.getTargetType()) {
            // OWNER_PET should be cast even if no target has been found
            case TARGET_OWNER_PET:
                target = getOwner();
                break;
            // PARTY, AURA, SELF should be cast even if no target has been found
            case TARGET_PARTY:
            case TARGET_AURA:
            case TARGET_FRONT_AURA:
            case TARGET_BEHIND_AURA:
            case TARGET_AURA_UNDEAD:
            case TARGET_SELF:
            case TARGET_CORPSE_ALLY:
                target = this;
                break;
            default:
                // Get the first target of the list
                target = skill.getFirstOfTargetList(this);
                break;
        }

        // Check the validity of the target
        if (target == null) {
            sendPacket(SystemMessageId.TARGET_CANT_FOUND);
            return false;
        }

        // ************************************* Check skill availability *******************************************

        // Check if this skill is enabled (e.g. reuse time)
        if (isSkillDisabled(skill)) {
            sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_PREPARED_FOR_REUSE).addString(skill.getName()));
            return false;
        }

        // ************************************* Check Consumables *******************************************

        // Check if the summon has enough MP
        if (getCurrentMp() < getStat().getMpConsume(skill) + getStat().getMpInitialConsume(skill)) {
            // Send a System Message to the caster
            sendPacket(SystemMessageId.NOT_ENOUGH_MP);
            return false;
        }

        // Check if the summon has enough HP
        if (getCurrentHp() <= skill.getHpConsume()) {
            // Send a System Message to the caster
            sendPacket(SystemMessageId.NOT_ENOUGH_HP);
            return false;
        }

        // ************************************* Check Summon State *******************************************

        // Check if this is offensive magic skill
        if (skill.isOffensive()) {
            if (isInsidePeaceZone(this, target)) {
                // If summon or target is in a peace zone, send a system message TARGET_IN_PEACEZONE
                sendPacket(SystemMessage.getSystemMessage(SystemMessageId.TARGET_IN_PEACEZONE));
                return false;
            }

            if (getOwner() != null && getOwner().isInOlympiadMode() && !getOwner().isOlympiadStart()) {
                // if L2PcInstance is in Olympia and the match isn't already start, send ActionFailed
                sendPacket(ActionFailed.STATIC_PACKET);
                return false;
            }

            // Check if the target is attackable
            if (target instanceof L2DoorInstance) {
                if (!((L2DoorInstance) target).isAttackable(getOwner())) { return false; }
            }
            else {
                if (!target.isAttackable() && getOwner() != null && !getOwner().getAccessLevel().allowPeaceAttack()) { return false; }

                // Check if a Forced ATTACK is in progress on non-attackable target
                if (!target.isAutoAttackable(this) && !forceUse && skill.getTargetType() != ESkillTargetType.TARGET_AURA && skill.getTargetType() != ESkillTargetType.TARGET_FRONT_AURA && skill.getTargetType() != ESkillTargetType.TARGET_BEHIND_AURA && skill.getTargetType() != ESkillTargetType.TARGET_AURA_UNDEAD && skill.getTargetType() != ESkillTargetType.TARGET_CLAN && skill.getTargetType() != ESkillTargetType.TARGET_ALLY && skill.getTargetType() != ESkillTargetType.TARGET_PARTY && skill.getTargetType() != ESkillTargetType.TARGET_SELF) {
                    return false;
                }
            }
        }

        // Notify the AI with CAST and target
        getAI().setIntention(EIntention.CAST, skill, target);
        return true;
    }

    @Override
    public void setIsImmobilized(boolean value) {
        super.setIsImmobilized(value);

        if (value) {
            _previousFollowStatus = getFollowStatus();
            // if immobilized, disable follow mode
            if (_previousFollowStatus) { setFollowStatus(false); }
        }
        else {
            // if not more immobilized, restore follow mode
            setFollowStatus(_previousFollowStatus);
        }
    }

    @Override
    public void sendDamageMessage(L2Character target, int damage, boolean mcrit, boolean pcrit, boolean miss) {
        if (miss || getOwner() == null) { return; }

        // Prevents the double spam of system messages, if the target is the owning player.
        if (target.getObjectId() != getOwner().getObjectId()) {
            if (pcrit || mcrit) {
                if (this instanceof L2SummonInstance) { sendPacket(SystemMessageId.CRITICAL_HIT_BY_SUMMONED_MOB); }
                else { sendPacket(SystemMessageId.CRITICAL_HIT_BY_PET); }
            }

            final SystemMessage sm;

            if (target.isInvul()) {
                if (target.isParalyzed()) { sm = SystemMessage.getSystemMessage(SystemMessageId.OPPONENT_PETRIFIED); }
                else { sm = SystemMessage.getSystemMessage(SystemMessageId.ATTACK_WAS_BLOCKED); }
            }
            else { sm = SystemMessage.getSystemMessage(SystemMessageId.PET_HIT_FOR_S1_DAMAGE).addNumber(damage); }

            sendPacket(sm);

            if (getOwner().isInOlympiadMode() && target instanceof L2PcInstance && ((L2PcInstance) target).isInOlympiadMode() && ((L2PcInstance) target).getOlympiadGameId() == getOwner().getOlympiadGameId()) {
                OlympiadGameManager.getInstance().notifyCompetitorDamage(getOwner(), damage);
            }
        }
    }

    @Override
    public void reduceCurrentHp(double damage, L2Character attacker, L2Skill skill) {
        super.reduceCurrentHp(damage, attacker, skill);
    }

    @Override
    public void doCast(L2Skill skill) {
        final L2PcInstance actingPlayer = getActingPlayer();
        if (!actingPlayer.checkPvpSkill(getTarget(), skill) && !actingPlayer.getAccessLevel().allowPeaceAttack()) {
            // Send a System Message to the L2PcInstance
            actingPlayer.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);

            // Send ActionFailed to the L2PcInstance
            actingPlayer.sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }

        super.doCast(skill);
    }

    @Override
    public boolean isOutOfControl() {
        return super.isOutOfControl() || isBetrayed();
    }

    @Override
    public boolean isInCombat() {
        return getOwner() != null ? getOwner().isInCombat() : false;
    }

    @Override
    public final boolean isAttackingNow() {
        return isInCombat();
    }

    @Override
    public L2PcInstance getActingPlayer() {
        return getOwner();
    }

    @Override
    public String toString() {
        return super.toString() + "(" + getNpcId() + ") Owner: " + getOwner();
    }

    @Override
    public boolean isUndead() {
        return getTemplate().isUndead();
    }

    @Override
    public void sendPacket(L2GameServerPacket mov) {
        if (getOwner() != null) { getOwner().sendPacket(mov); }
    }

    @Override
    public void sendPacket(SystemMessageId id) {
        if (getOwner() != null) { getOwner().sendPacket(id); }
    }

    public int getWeapon() {
        return 0;
    }

    public int getArmor() {
        return 0;
    }

    @Override
    public void onTeleported() {
        super.onTeleported();
        sendPacket(new TeleportToLocation(this, getPosition().getX(), getPosition().getY(), getPosition().getZ()));
    }

    public void updateAndBroadcastStatusAndInfos(int val) {
        sendPacket(new PetInfo(this, val));

        // The PetInfo packet wipes the PartySpelled (list of active spells' icons). Re-add them
        updateEffectIcons(true);

        updateAndBroadcastStatus(val);
    }

    public void sendPetInfosToOwner() {
        sendPacket(new PetInfo(this, 2));

        // The PetInfo packet wipes the PartySpelled (list of active spells' icons). Re-add them
        updateEffectIcons(true);
    }

    public void updateAndBroadcastStatus(int val) {
        sendPacket(new PetStatusUpdate(this));

        if (isVisible()) { broadcastNpcInfo(val); }
    }

    public void broadcastNpcInfo(int val) {
        for (L2PcInstance player : getKnownList().getKnownType(L2PcInstance.class)) {
            if (player == getOwner()) { continue; }

            player.sendPacket(new SummonInfo(this, player, val));
        }
    }

    public boolean isHungry() {
        return false;
    }

    @Override
    public void onSpawn() {
        super.onSpawn();

        // Need it only for "crests on summons" custom.
        if (Config.SHOW_SUMMON_CREST) { sendPacket(new SummonInfo(this, getOwner(), 0)); }

        sendPacket(new RelationChanged(this, getOwner().getRelationTo(getOwner()), false));
        broadcastRelationsChanges();
    }

    @Override
    public void broadcastRelationsChanges() {
        for (L2PcInstance player : getOwner().getKnownList().getKnownType(L2PcInstance.class)) { player.sendPacket(new RelationChanged(this, getOwner().getRelationTo(player), isAutoAttackable(player))); }
    }

    @Override
    public void sendInfo(L2PcInstance activeChar) {
        // Check if the L2PcInstance is the owner of the Pet
        if (activeChar.equals(getOwner())) {
            activeChar.sendPacket(new PetInfo(this, 0));

            // The PetInfo packet wipes the PartySpelled (list of active spells' icons). Re-add them
            updateEffectIcons(true);

            if (this instanceof L2PetInstance) { activeChar.sendPacket(new PetItemList((L2PetInstance) this)); }
        }
        else { activeChar.sendPacket(new SummonInfo(this, activeChar, 0)); }
    }

    @Override
    public boolean isChargedShot(ShotType type) {
        return (_shotsMask & type.getMask()) == type.getMask();
    }

    @Override
    public void setChargedShot(ShotType type, boolean charged) {
        if (charged) { _shotsMask |= type.getMask(); }
        else { _shotsMask &= ~type.getMask(); }
    }

    @Override
    public void rechargeShots(boolean physical, boolean magic) {
        if (getOwner().getAutoSoulShot() == null || getOwner().getAutoSoulShot().isEmpty()) { return; }

        for (int itemId : getOwner().getAutoSoulShot()) {
            L2ItemInstance item = getOwner().getInventory().getItemByItemId(itemId);
            if (item != null) {
                if (magic && item.getItem().getDefaultAction() == ActionType.summon_spiritshot) {
                    IItemHandler handler = ItemHandler.getInstance().getItemHandler(item.getEtcItem());
                    if (handler != null) { handler.useItem(getOwner(), item, false); }
                }

                if (physical && item.getItem().getDefaultAction() == ActionType.summon_soulshot) {
                    IItemHandler handler = ItemHandler.getInstance().getItemHandler(item.getEtcItem());
                    if (handler != null) { handler.useItem(getOwner(), item, false); }
                }
            }
            else { getOwner().removeAutoSoulShot(itemId); }
        }
    }

    @Override
    public boolean isSummon() {
        return true;
    }
}