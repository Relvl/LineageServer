package net.sf.l2j.gameserver.model.actor;

import net.sf.l2j.gameserver.ai.EIntention;
import net.sf.l2j.gameserver.ai.model.L2CharacterAI;
import net.sf.l2j.gameserver.ai.model.L2SummonAI;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.geoengine.PathFinding;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.handler.ItemHandler;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.ShotType;
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
import net.sf.l2j.gameserver.model.skill.ESkillTargetType;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.model.world.L2WorldRegion;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.AbstractNpcInfo.SummonInfo;
import net.sf.l2j.gameserver.network.client.game_to_client.*;

@SuppressWarnings("ObjectEquality")
public abstract class L2Summon extends L2Playable {
    private L2PcInstance owner;
    private boolean follow = true;
    private boolean previousFollowStatus = true;
    private int shotsMask;

    public L2Summon(int objectId, NpcTemplate template, L2PcInstance owner) {
        super(objectId, template);

        _showSummonAnimation = true;
        this.owner = owner;
        ai = new L2SummonAI(this);

        getPosition().setXYZInvisible(owner.getX() + 50, owner.getY() + 100, owner.getZ() + 100);
    }

    @Override
    public void initKnownList() { setKnownList(new SummonKnownList(this)); }

    @Override
    public final SummonKnownList getKnownList() { return (SummonKnownList) super.getKnownList(); }

    @Override
    public void initCharStat() { setStat(new SummonStat(this)); }

    @Override
    public SummonStat getStat() { return (SummonStat) super.getStat(); }

    @Override
    public void initCharStatus() { setStatus(new SummonStatus(this)); }

    @Override
    public SummonStatus getStatus() { return (SummonStatus) super.getStatus(); }

    @Override
    public L2CharacterAI getAI() {
        L2CharacterAI ai = this.ai;
        if (ai == null) {
            synchronized (this) {
                if (this.ai == null) {
                    this.ai = new L2SummonAI(this);
                }
                return this.ai;
            }
        }
        return ai;
    }

    @Override
    public NpcTemplate getTemplate() { return (NpcTemplate) super.getTemplate(); }

    public abstract int getSummonType();

    @Override
    public void updateAbnormalEffect() {
        for (L2PcInstance player : getKnownList().getKnownType(L2PcInstance.class)) {
            player.sendPacket(new SummonInfo(this, player, 1));
        }
    }

    public boolean isMountable() { return false; }

    @Override
    public void onAction(L2PcInstance player) {
        if (player.getTarget() != this) {
            player.setTarget(this);
        }
        else if (player == owner) {
            if (canInteract(player)) {
                player.sendPacket(new MoveToPawn(player, this, L2Npc.INTERACTION_DISTANCE));
                player.sendPacket(new PetStatusShow(this));
                player.sendPacket(ActionFailed.STATIC_PACKET);
            }
            else {
                player.getAI().setIntention(EIntention.INTERACT, this);
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
                player.sendPacket(new MoveToPawn(player, this, L2Npc.INTERACTION_DISTANCE));
                player.sendPacket(ActionFailed.STATIC_PACKET);
                if (PathFinding.getInstance().canSeeTarget(player, this)) {
                    player.getAI().setIntention(EIntention.FOLLOW, this);
                }
            }
        }
    }

    @Override
    public void onActionShift(L2PcInstance player) {
        if (player.isGM()) {
            NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
            html.setFile("data/html/admin/petinfo.htm");
            html.replace("%name%", getName() == null ? "N/A" : getName());
            html.replace("%level%", getLevel());
            html.replace("%exp%", getStat().getExp());
            html.replace("%owner%", " <a action=\"bypass -h admin_character_info " + owner.getName() + "\">" + owner.getName() + "</a>");
            html.replace("%class%", getClass().getSimpleName());
            html.replace("%ai%", hasAI() ? getAI().getIntention().name() : "NULL");
            html.replace("%hp%", (int) getStatus().getCurrentHp() + "/" + getStat().getMaxHp());
            html.replace("%mp%", (int) getStatus().getCurrentMp() + "/" + getStat().getMaxMp());
            html.replace("%karma%", getKarma());
            html.replace("%undead%", isUndead() ? "yes" : "no");

            if (this instanceof L2PetInstance) {
                html.replace("%inv%", " <a action=\"bypass admin_show_pet_inv " + owner.getObjectId() + "\">view</a>");
                html.replace("%food%", ((L2PetInstance) this).getCurrentFed() + "/" + ((L2PetInstance) this).getPetLevelData().getPetMaxFeed());
                html.replace("%load%", getInventory().getTotalWeight() + "/" + getMaxLoad());
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
    public final int getKarma() { return owner != null ? owner.getKarma() : 0; }

    @Override
    public final byte getPvpFlag() { return owner != null ? owner.getPvpFlag() : 0; }

    public final int getTeam() { return owner != null ? owner.getTeam() : 0; }

    public final L2PcInstance getOwner() { return owner; }

    public void setOwner(L2PcInstance newOwner) { owner = newOwner; }

    public final int getNpcId() { return getTemplate().getNpcId(); }

    public int getMaxLoad() { return 0; }

    public int getSoulShotsPerHit() { return getTemplate().getAIData().getSsCount(); }

    public int getSpiritShotsPerHit() { return getTemplate().getAIData().getSpsCount(); }

    public void followOwner() { setFollow(true); }

    @Override
    public boolean doDie(L2Character killer) {
        if (!super.doDie(killer)) { return false; }
        for (int itemId : owner.getAutoSoulShot()) {
            switch (ItemTable.getInstance().getTemplate(itemId).getDefaultAction()) {
                case summon_soulshot:
                case summon_spiritshot:
                    owner.disableAutoShot(itemId);
                    break;
            }
        }
        return true;
    }

    @Override
    public void onDecay() { deleteMe(owner); }

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
        deleteMe();
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

            if (hasAI()) {
                getAI().stopAITask();
            }

            stopAllEffects();
            L2WorldRegion oldRegion = getWorldRegion();

            decayMe();

            if (oldRegion != null) {
                oldRegion.removeFromZones(this);
            }

            getKnownList().removeAllKnownObjects();
            setTarget(null);

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

    public static int getAttackRange() { return 36; }

    public boolean isFollow() { return follow; }

    public void setFollow(boolean state) {
        follow = state;
        if (follow) {
            getAI().setIntention(EIntention.FOLLOW, owner);
        }
        else {
            getAI().setIntention(EIntention.IDLE, null);
        }
    }

    @Override
    public boolean isAutoAttackable(L2Character attacker) { return owner.isAutoAttackable(attacker); }

    public int getControlItemId() { return 0; }

    @Override
    public PetInventory getInventory() { return null; }

    public void store() { }

    @Override
    public L2ItemInstance getActiveWeaponInstance() { return null; }

    @Override
    public Weapon getActiveWeaponItem() { return null; }

    @Override
    public L2ItemInstance getSecondaryWeaponInstance() { return null; }

    @Override
    public Weapon getSecondaryWeaponItem() { return null; }

    @Override
    public boolean isInvul() { return super.isInvul() || owner.isSpawnProtected(); }

    @Override
    public L2Party getParty() { return owner == null ? null : owner.getParty(); }

    @Override
    public boolean isInParty() { return owner != null && owner.getParty() != null; }

    @Override
    public boolean useMagic(L2Skill skill, boolean forceUse, boolean dontMove) {
        if (skill == null || isDead()) { return false; }
        if (skill.isPassive()) { return false; }
        if (isCastingNow()) { return false; }
        owner.setCurrentPetSkill(skill, forceUse, dontMove);

        L2Object target;
        switch (skill.getTargetType()) {
            case TARGET_OWNER_PET:
                target = owner;
                break;
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
                target = skill.getFirstOfTargetList(this);
                break;
        }
        if (target == null) {
            sendPacket(SystemMessageId.TARGET_CANT_FOUND);
            return false;
        }

        if (isSkillDisabled(skill)) {
            sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_PREPARED_FOR_REUSE).addString(skill.getName()));
            return false;
        }

        if (getCurrentMp() < getStat().getMpConsume(skill) + getStat().getMpInitialConsume(skill)) {
            sendPacket(SystemMessageId.NOT_ENOUGH_MP);
            return false;
        }

        if (getCurrentHp() <= skill.getHpConsume()) {
            sendPacket(SystemMessageId.NOT_ENOUGH_HP);
            return false;
        }

        if (skill.isOffensive()) {
            if (isInsidePeaceZone(this, target)) {
                sendPacket(SystemMessage.getSystemMessage(SystemMessageId.TARGET_IN_PEACEZONE));
                return false;
            }

            if (owner != null && owner.isInOlympiadMode() && !owner.isOlympiadStart()) {
                sendPacket(ActionFailed.STATIC_PACKET);
                return false;
            }

            if (target instanceof L2DoorInstance) {
                if (!((L2DoorInstance) target).isAttackable(owner)) { return false; }
            }
            else {
                if (!target.isAttackable() && owner != null && !owner.getAccessLevel().allowPeaceAttack()) { return false; }
                if (!target.isAutoAttackable(this)
                        && !forceUse
                        && skill.getTargetType() != ESkillTargetType.TARGET_AURA
                        && skill.getTargetType() != ESkillTargetType.TARGET_FRONT_AURA
                        && skill.getTargetType() != ESkillTargetType.TARGET_BEHIND_AURA
                        && skill.getTargetType() != ESkillTargetType.TARGET_AURA_UNDEAD
                        && skill.getTargetType() != ESkillTargetType.TARGET_CLAN
                        && skill.getTargetType() != ESkillTargetType.TARGET_ALLY
                        && skill.getTargetType() != ESkillTargetType.TARGET_PARTY
                        && skill.getTargetType() != ESkillTargetType.TARGET_SELF
                        ) {
                    return false;
                }
            }
        }

        getAI().setIntention(EIntention.CAST, skill, target);
        return true;
    }

    @Override
    public void setIsImmobilized(boolean value) {
        super.setIsImmobilized(value);
        if (value) {
            previousFollowStatus = follow;
            if (previousFollowStatus) {
                setFollow(false);
            }
        }
        else {
            setFollow(previousFollowStatus);
        }
    }

    @Override
    public void sendDamageMessage(L2Character target, int damage, boolean mcrit, boolean pcrit, boolean miss) {
        if (miss || owner == null) { return; }
        if (target.getObjectId() != owner.getObjectId()) {
            if (pcrit || mcrit) {
                if (this instanceof L2SummonInstance) { sendPacket(SystemMessageId.CRITICAL_HIT_BY_SUMMONED_MOB); }
                else { sendPacket(SystemMessageId.CRITICAL_HIT_BY_PET); }
            }

            final SystemMessage sm;
            if (target.isInvul()) {
                sm = SystemMessage.getSystemMessage(target.isParalyzed() ? SystemMessageId.OPPONENT_PETRIFIED : SystemMessageId.ATTACK_WAS_BLOCKED);
            }
            else {
                sm = SystemMessage.getSystemMessage(SystemMessageId.PET_HIT_FOR_S1_DAMAGE).addNumber(damage);
            }
            sendPacket(sm);

            if (owner.isInOlympiadMode() && target instanceof L2PcInstance && ((L2PcInstance) target).isInOlympiadMode() && ((L2PcInstance) target).getOlympiadGameId() == owner.getOlympiadGameId()) {
                OlympiadGameManager.getInstance().notifyCompetitorDamage(owner, damage);
            }
        }
    }

    @Override
    public void reduceCurrentHp(double damage, L2Character attacker, L2Skill skill) { super.reduceCurrentHp(damage, attacker, skill); }

    @Override
    public void doCast(L2Skill skill) {
        if (!owner.checkPvpSkill(getTarget(), skill) && !owner.getAccessLevel().allowPeaceAttack()) {
            owner.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
            owner.sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }
        super.doCast(skill);
    }

    @Override
    public boolean isOutOfControl() { return super.isOutOfControl() || isBetrayed(); }

    @Override
    public boolean isInCombat() { return owner != null && owner.isInCombat(); }

    @Override
    public final boolean isAttackingNow() { return isInCombat(); }

    @Override
    public L2PcInstance getActingPlayer() { return owner; }

    @Override
    public boolean isUndead() { return getTemplate().isUndead(); }

    @Override
    public void sendPacket(L2GameServerPacket mov) {
        if (owner != null) {
            owner.sendPacket(mov);
        }
    }

    @Override
    public void sendPacket(SystemMessageId id) {
        if (owner != null) {
            owner.sendPacket(id);
        }
    }

    public int getWeapon() { return 0; }

    public int getArmor() { return 0; }

    @Override
    public void onTeleported() {
        super.onTeleported();
        sendPacket(new TeleportToLocation(this, getPosition().getX(), getPosition().getY(), getPosition().getZ()));
    }

    public void updateAndBroadcastStatusAndInfos(int val) {
        sendPacket(new PetInfo(this, val));
        updateEffectIcons(true);
        updateAndBroadcastStatus(val);
    }

    public void sendPetInfosToOwner() {
        sendPacket(new PetInfo(this, 2));
        updateEffectIcons(true);
    }

    public void updateAndBroadcastStatus(int val) {
        sendPacket(new PetStatusUpdate(this));
        if (isVisible()) {
            broadcastNpcInfo(val);
        }
    }

    public void broadcastNpcInfo(int val) {
        for (L2PcInstance player : getKnownList().getKnownType(L2PcInstance.class)) {
            if (player == owner) { continue; }
            player.sendPacket(new SummonInfo(this, player, val));
        }
    }

    public boolean isHungry() { return false; }

    @Override
    public void onSpawn() {
        super.onSpawn();
        sendPacket(new SummonInfo(this, owner, 0));
        sendPacket(new RelationChanged(this, owner.getRelationTo(owner), false));
        broadcastRelationsChanges();
    }

    @Override
    public void broadcastRelationsChanges() {
        for (L2PcInstance player : owner.getKnownList().getKnownType(L2PcInstance.class)) {
            player.sendPacket(new RelationChanged(this, owner.getRelationTo(player), isAutoAttackable(player)));
        }
    }

    @Override
    public void sendInfo(L2PcInstance activeChar) {
        if (activeChar.equals(owner)) {
            activeChar.sendPacket(new PetInfo(this, 0));
            updateEffectIcons(true);
            if (this instanceof L2PetInstance) {
                activeChar.sendPacket(new PetItemList((L2PetInstance) this));
            }
        }
        else {
            activeChar.sendPacket(new SummonInfo(this, activeChar, 0));
        }
    }

    @Override
    public boolean isChargedShot(ShotType type) { return (shotsMask & type.getMask()) == type.getMask(); }

    @Override
    public void setChargedShot(ShotType type, boolean charged) {
        if (charged) { shotsMask |= type.getMask(); }
        else { shotsMask &= ~type.getMask(); }
    }

    @Override
    public void rechargeShots(boolean physical, boolean magical) {
        if (owner.getAutoSoulShot() == null || owner.getAutoSoulShot().isEmpty()) { return; }
        for (int itemId : owner.getAutoSoulShot()) {
            L2ItemInstance item = owner.getInventory().getItemByItemId(itemId);
            if (item != null) {
                if (magical && item.getItem().getDefaultAction() == ActionType.summon_spiritshot) {
                    IItemHandler handler = ItemHandler.getInstance().getItemHandler(item.getEtcItem());
                    if (handler != null) {
                        handler.useItem(owner, item, false);
                    }
                }
                if (physical && item.getItem().getDefaultAction() == ActionType.summon_soulshot) {
                    IItemHandler handler = ItemHandler.getInstance().getItemHandler(item.getEtcItem());
                    if (handler != null) {
                        handler.useItem(owner, item, false);
                    }
                }
            }
            else {
                owner.removeAutoSoulShot(itemId);
            }
        }
    }

    @Override
    public boolean isSummon() { return true; }
}