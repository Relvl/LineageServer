package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.ECtrlEvent;
import net.sf.l2j.gameserver.ai.EIntention;
import net.sf.l2j.gameserver.ai.NextAction;
import net.sf.l2j.gameserver.ai.model.L2CharacterAI;
import net.sf.l2j.gameserver.ai.model.L2PlayerAI;
import net.sf.l2j.gameserver.ai.model.L2SummonAI;
import net.sf.l2j.gameserver.communitybbs.BB.Forum;
import net.sf.l2j.gameserver.communitybbs.Manager.ForumsBBSManager;
import net.sf.l2j.gameserver.datatables.*;
import net.sf.l2j.gameserver.datatables.MapRegionTable.TeleportWhereType;
import net.sf.l2j.gameserver.datatables.SkillTable.FrequentSkill;
import net.sf.l2j.gameserver.geoengine.PathFinding;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.handler.ItemHandler;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminEditChar;
import net.sf.l2j.gameserver.instancemanager.*;
import net.sf.l2j.gameserver.model.*;
import net.sf.l2j.gameserver.model.L2Party.MessageType;
import net.sf.l2j.gameserver.model.L2PetData.L2PetLevelData;
import net.sf.l2j.gameserver.model.actor.*;
import net.sf.l2j.gameserver.model.actor.appearance.PcAppearance;
import net.sf.l2j.gameserver.model.actor.instance.playerpart.GatesRequest;
import net.sf.l2j.gameserver.model.actor.instance.playerpart.PrivateStoreType;
import net.sf.l2j.gameserver.model.actor.instance.playerpart.PunishLevel;
import net.sf.l2j.gameserver.model.actor.instance.playerpart.SummonRequest;
import net.sf.l2j.gameserver.model.actor.knownlist.PcKnownList;
import net.sf.l2j.gameserver.model.actor.position.PcPosition;
import net.sf.l2j.gameserver.model.actor.stat.PcStat;
import net.sf.l2j.gameserver.model.actor.status.PcStatus;
import net.sf.l2j.gameserver.model.actor.template.PcTemplate;
import net.sf.l2j.gameserver.model.base.*;
import net.sf.l2j.gameserver.model.entity.Castle;
import net.sf.l2j.gameserver.model.entity.Duel.DuelState;
import net.sf.l2j.gameserver.model.entity.Hero;
import net.sf.l2j.gameserver.model.entity.Siege;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.holder.SkillUseHolder;
import net.sf.l2j.gameserver.model.item.*;
import net.sf.l2j.gameserver.model.item.kind.Armor;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.item.kind.Weapon;
import net.sf.l2j.gameserver.model.item.type.ActionType;
import net.sf.l2j.gameserver.model.item.type.ArmorType;
import net.sf.l2j.gameserver.model.item.type.EWeaponType;
import net.sf.l2j.gameserver.model.item.type.EtcItemType;
import net.sf.l2j.gameserver.model.itemcontainer.*;
import net.sf.l2j.gameserver.model.itemcontainer.listeners.ItemPassiveSkillsListener;
import net.sf.l2j.gameserver.model.location.HeadedLocation;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.memo.PlayerMemo;
import net.sf.l2j.gameserver.model.olympiad.OlympiadGameManager;
import net.sf.l2j.gameserver.model.olympiad.OlympiadGameTask;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.model.partymatching.PartyMatchRoom;
import net.sf.l2j.gameserver.model.partymatching.PartyMatchRoomList;
import net.sf.l2j.gameserver.model.partymatching.PartyMatchWaitingList;
import net.sf.l2j.gameserver.model.skill.ESkillTargetType;
import net.sf.l2j.gameserver.model.skill.L2Skill;
import net.sf.l2j.gameserver.model.tradelist.TradeList;
import net.sf.l2j.gameserver.model.world.L2World;
import net.sf.l2j.gameserver.model.world.L2WorldRegion;
import net.sf.l2j.gameserver.model.zone.ZoneId;
import net.sf.l2j.gameserver.model.zone.type.L2BossZone;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.AbstractNpcInfo.PcMorphInfo;
import net.sf.l2j.gameserver.network.client.game_to_client.*;
import net.sf.l2j.gameserver.scripting.EventType;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;
import net.sf.l2j.gameserver.scripting.ScriptManager;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;
import net.sf.l2j.gameserver.skills.effects.EffectTemplate;
import net.sf.l2j.gameserver.skills.funcs.*;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillSiegeFlag;
import net.sf.l2j.gameserver.skills.l2skills.L2SkillSummon;
import net.sf.l2j.gameserver.taskmanager.*;
import net.sf.l2j.gameserver.templates.skills.L2EffectFlag;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;
import net.sf.l2j.gameserver.util.Broadcast;
import net.sf.l2j.gameserver.util.Util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

public final class L2PcInstance extends L2Playable {
    public static final int REQUEST_TIMEOUT = 15;
    private static final String RESTORE_SKILLS_FOR_CHAR = "SELECT skill_id,skill_level FROM character_skills WHERE char_obj_id=? AND class_index=?";
    private static final String ADD_NEW_SKILL = "INSERT INTO character_skills (char_obj_id,skill_id,skill_level,class_index) VALUES (?,?,?,?)";
    private static final String UPDATE_CHARACTER_SKILL_LEVEL = "UPDATE character_skills SET skill_level=? WHERE skill_id=? AND char_obj_id=? AND class_index=?";
    private static final String DELETE_SKILL_FROM_CHAR = "DELETE FROM character_skills WHERE skill_id=? AND char_obj_id=? AND class_index=?";
    private static final String DELETE_CHAR_SKILLS = "DELETE FROM character_skills WHERE char_obj_id=? AND class_index=?";
    private static final String ADD_SKILL_SAVE = "INSERT INTO character_skills_save (char_obj_id,skill_id,skill_level,effect_count,effect_cur_time,reuse_delay,systime,restore_type,class_index,buff_index) VALUES (?,?,?,?,?,?,?,?,?,?)";
    private static final String RESTORE_SKILL_SAVE = "SELECT skill_id,skill_level,effect_count,effect_cur_time, reuse_delay, systime, restore_type FROM character_skills_save WHERE char_obj_id=? AND class_index=? ORDER BY buff_index ASC";
    private static final String DELETE_SKILL_SAVE = "DELETE FROM character_skills_save WHERE char_obj_id=? AND class_index=?";
    private static final String INSERT_CHARACTER = "INSERT INTO characters (account_name,obj_Id,char_name,level,maxHp,curHp,maxCp,curCp,maxMp,curMp,face,hairStyle,hairColor,sex,exp,sp,karma,pvpkills,pkkills,clanid,race,classid,deletetime,cancraft,title,accesslevel,online,isin7sdungeon,clan_privs,wantspeace,base_class,nobless,power_grade,last_recom_date) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    private static final String UPDATE_CHARACTER = "UPDATE characters SET level=?,maxHp=?,curHp=?,maxCp=?,curCp=?,maxMp=?,curMp=?,face=?,hairStyle=?,hairColor=?,sex=?,heading=?,x=?,y=?,z=?,exp=?,expBeforeDeath=?,sp=?,karma=?,pvpkills=?,pkkills=?,rec_have=?,rec_left=?,clanid=?,race=?,classid=?,deletetime=?,title=?,accesslevel=?,online=?,isin7sdungeon=?,clan_privs=?,wantspeace=?,base_class=?,onlinetime=?,punish_level=?,punish_timer=?,nobless=?,power_grade=?,subpledge=?,last_recom_date=?,lvl_joined_academy=?,apprentice=?,sponsor=?,varka_ketra_ally=?,clan_join_expiry_time=?,clan_create_expiry_time=?,char_name=?,death_penalty_level=? WHERE obj_id=?";
    private static final String RESTORE_CHARACTER = "SELECT account_name, obj_Id, char_name, level, maxHp, curHp, maxCp, curCp, maxMp, curMp, face, hairStyle, hairColor, sex, heading, x, y, z, exp, expBeforeDeath, sp, karma, pvpkills, pkkills, clanid, race, classid, deletetime, cancraft, title, rec_have, rec_left, accesslevel, online, char_slot, lastAccess, clan_privs, wantspeace, base_class, onlinetime, isin7sdungeon, punish_level, punish_timer, nobless, power_grade, subpledge, last_recom_date, lvl_joined_academy, apprentice, sponsor, varka_ketra_ally,clan_join_expiry_time,clan_create_expiry_time,death_penalty_level FROM characters WHERE obj_id=?";
    private static final String RESTORE_CHAR_SUBCLASSES = "SELECT class_id,exp,sp,level,class_index FROM character_subclasses WHERE char_obj_id=? ORDER BY class_index ASC";
    private static final String ADD_CHAR_SUBCLASS = "INSERT INTO character_subclasses (char_obj_id,class_id,exp,sp,level,class_index) VALUES (?,?,?,?,?,?)";
    private static final String UPDATE_CHAR_SUBCLASS = "UPDATE character_subclasses SET exp=?,sp=?,level=?,class_id=? WHERE char_obj_id=? AND class_index =?";
    private static final String DELETE_CHAR_SUBCLASS = "DELETE FROM character_subclasses WHERE char_obj_id=? AND class_index=?";
    private static final String RESTORE_CHAR_HENNAS = "SELECT slot,symbol_id FROM character_hennas WHERE char_obj_id=? AND class_index=?";
    private static final String ADD_CHAR_HENNA = "INSERT INTO character_hennas (char_obj_id,symbol_id,slot,class_index) VALUES (?,?,?,?)";
    private static final String DELETE_CHAR_HENNA = "DELETE FROM character_hennas WHERE char_obj_id=? AND slot=? AND class_index=?";
    private static final String DELETE_CHAR_HENNAS = "DELETE FROM character_hennas WHERE char_obj_id=? AND class_index=?";
    private static final String DELETE_CHAR_SHORTCUTS = "DELETE FROM character_shortcuts WHERE char_obj_id=? AND class_index=?";
    private static final String RESTORE_CHAR_RECOMS = "SELECT char_id,target_id FROM character_recommends WHERE char_id=?";
    private static final String ADD_CHAR_RECOM = "INSERT INTO character_recommends (char_id,target_id) VALUES (?,?)";
    private static final String DELETE_CHAR_RECOMS = "DELETE FROM character_recommends WHERE char_id=?";
    private static final String UPDATE_NOBLESS = "UPDATE characters SET nobless=? WHERE obj_Id=?";
    private static final int[] EXPERTISE_LEVELS = {
            0, // NONE
            20, // D
            40, // C
            52, // B
            61, // A
            76, // S
    };
    private static final int[] COMMON_CRAFT_LEVELS = {5, 20, 28, 36, 43, 49, 55, 62};
    private static final int FALLING_VALIDATION_DELAY = 10000;

    private final L2Radar radar = new L2Radar(this);
    private final PcInventory inventory = new PcInventory(this);

    private final ReentrantLock _subclassLock = new ReentrantLock();
    private final Map<Integer, SubClass> _subClasses = new ConcurrentSkipListMap<>();
    private final Map<Integer, RecipeList> _dwarvenRecipeBook = new HashMap<>();
    private final Map<Integer, RecipeList> _commonRecipeBook = new HashMap<>();
    private final Location _savedLocation = new Location(0, 0, 0);
    private final List<Integer> _recomChars = new ArrayList<>();
    private final List<PcFreight> _depositedFreight = new ArrayList<>();
    private final List<QuestState> _quests = new ArrayList<>();
    private final List<QuestState> _notifyQuestOfDeathList = new ArrayList<>();
    private final PlayerMemo _vars = new PlayerMemo(getObjectId());
    private final ShortCuts _shortCuts = new ShortCuts(this);
    private final MacroList _macroses = new MacroList(this);
    private final Henna[] _henna = new Henna[3];
    private final AtomicInteger _charges = new AtomicInteger();
    private final L2Request _request = new L2Request(this);
    private final Map<Integer, String> _chars = new HashMap<>();
    private final int _loto[] = new int[5];
    private final int _race[] = new int[2];
    private final BlockList _blockList = new BlockList(this);
    private final List<String> _validBypass = new ArrayList<>();
    private final List<String> _validBypass2 = new ArrayList<>();
    private final SkillUseHolder _currentSkill = new SkillUseHolder();
    private final SkillUseHolder _currentPetSkill = new SkillUseHolder();
    private final SkillUseHolder _queuedSkill = new SkillUseHolder();
    private final SummonRequest _summonRequest = new SummonRequest();
    private final GatesRequest _gatesRequest = new GatesRequest();
    private final Map<Integer, TimeStamp> _reuseTimeStamps = new ConcurrentHashMap<>();
    private final List<Integer> _friendList = new ArrayList<>();
    private final List<Integer> _selectedFriendList = new ArrayList<>();
    private final List<Integer> _selectedBlocksList = new ArrayList<>();
    private final String accountName;
    private final PcAppearance appearance;
    public ScheduledFuture<?> _taskforfish;
    public int _telemode;
    protected int _baseClass;
    protected int _activeClass;
    protected int _classIndex;
    protected boolean _inventoryDisable;
    protected Map<Integer, L2CubicInstance> _cubics = new ConcurrentSkipListMap<>();
    protected Set<Integer> _activeSoulShots = ConcurrentHashMap.newKeySet(1);
    protected Future<?> _mountFeedTask;
    ScheduledFuture<?> _shortBuffTask;
    private L2GameClient _client;
    private long _deleteTimer;
    private boolean _isOnline;
    private long _onlineTime;
    private long _onlineBeginTime;
    private long _lastAccess;
    private long _uptime;
    private long _expBeforeDeath;
    private int _karma;
    private int _pvpKills;
    private int _pkKills;
    private byte _pvpFlag;
    private byte _siegeState;
    private int _curWeightPenalty;
    private int _lastCompassZone;
    private boolean _isInWater;
    private boolean _isIn7sDungeon;
    private PunishLevel _punishLevel = PunishLevel.NONE;
    private long _punishTimer;
    private ScheduledFuture<?> _punishTask;
    private boolean _inOlympiadMode;
    private boolean _OlympiadStart;
    private int _olympiadGameId = -1;
    private int _olympiadSide = -1;
    private boolean _isInDuel;
    private DuelState _duelState = DuelState.NO_DUEL;
    private int _duelId;
    private SystemMessageId _noDuelReason = SystemMessageId.THERE_IS_NO_OPPONENT_TO_RECEIVE_YOUR_CHALLENGE_FOR_A_DUEL;
    private L2Vehicle _vehicle;
    private Location _inVehiclePosition;
    private int _mountType;
    private int _mountNpcId;
    private int _mountLevel;
    private int _mountObjectID;
    private boolean _inCrystallize;
    private boolean _inCraftMode;
    private boolean _waitTypeSitting;
    private boolean _observerMode;
    private int _recomHave;
    private int _recomLeft;
    private long _lastRecomUpdate;
    private PcWarehouse _warehouse;
    private PcFreight _freight;
    private PrivateStoreType _privateStoreType = PrivateStoreType.NONE;
    private TradeList _activeTradeList;
    private ItemContainer _activeWarehouse;
    private L2ManufactureList _createList;
    private TradeList _sellList;
    private TradeList _buyList;
    private boolean _noble;
    private boolean _hero;
    private L2Npc _currentFolkNpc;
    private int _questNpcObject;
    private ClassId _skillLearningClassId;
    private int _hennaSTR;
    private int _hennaINT;
    private int _hennaDEX;
    private int _hennaMEN;
    private int _hennaWIT;
    private int _hennaCON;
    private L2Summon summon;
    private L2TamedBeastInstance _tamedBeast;
    private int _partyroom;
    private int _clanId;
    private L2Clan clan;
    private int _apprentice;
    private int _sponsor;
    private long _clanJoinExpiryTime;
    private long _clanCreateExpiryTime;
    private int _powerGrade;
    private int _clanPrivileges;
    private int _pledgeClass;
    private int _pledgeType;
    private int _lvlJoinedAcademy;
    private boolean _wantsPeace;
    private int _deathPenaltyBuffLevel;
    private ScheduledFuture<?> _chargeTask;
    private Location _currentSkillWorldPosition;
    private L2AccessLevel _accessLevel;
    private boolean _messageRefusal; // message refusal mode
    private boolean _tradeRefusal; // Trade refusal
    private boolean _exchangeRefusal; // Exchange refusal
    private L2Party _party;
    private L2PcInstance _activeRequester;
    private long _requestExpireTime;
    private L2ItemInstance _arrowItem;
    private ScheduledFuture<?> _protectTask;
    private long _recentFakeDeathEndTime;
    private boolean _isFakeDeath;
    private Weapon _fistsWeaponItem;
    private int _expertiseIndex;
    private int _expertiseArmorPenalty;
    private boolean _expertiseWeaponPenalty;
    private L2ItemInstance _activeEnchantItem;
    private int _team;
    private int _alliedVarkaKetra; // lvl of alliance with ketra orcs or varka silenos, used in quests and aggro checks [-5,-1] varka, 0 neutral, [1,5] ketra
    private Location _fishingLoc;
    private L2ItemInstance _lure;
    private L2Fishing _fishCombat;
    private FishData _fish;
    private Forum _forumMail;
    private Forum _forumMemo;
    private boolean _canFeed;
    private L2PetData _data;
    private L2PetLevelData _leveldata;
    private int _controlItemId;
    private int _curFeed;
    private ScheduledFuture<?> _dismountTask;
    private boolean _isInSiege;
    private int _cursedWeaponEquippedId;
    private int _reviveRequested;
    private double _revivePower;
    private boolean _revivePet;
    private double _cpUpdateIncCheck;
    private double _cpUpdateDecCheck;
    private double _cpUpdateInterval;
    private double _mpUpdateIncCheck;
    private double _mpUpdateDecCheck;
    private double _mpUpdateInterval;
    private volatile int _clientX;
    private volatile int _clientY;
    private volatile int _clientZ;
    private volatile int _clientHeading;
    private int _mailPosition;
    private volatile long _fallingTimestamp;
    private int _shortBuffTaskSkillId;
    private boolean _married;
    private int _coupleId;
    private boolean _marryrequest;
    private int _requesterId;

    private L2PcInstance(int objectId, PcTemplate template, String accountName, PcAppearance appearance) {
        super(objectId, template);
        initCharStatusUpdateValues();
        initPcStatusUpdateValues();

        this.accountName = accountName;
        this.appearance = appearance;

        this.ai = new L2PlayerAI(this);

        inventory.restore();
        getWarehouse();
        getFreight();
    }

    public static L2PcInstance create(int objectId, PcTemplate template, String accountName, String name, byte hairStyle, byte hairColor, byte face, boolean sex) {
        PcAppearance app = new PcAppearance(face, hairColor, hairStyle, sex);
        L2PcInstance player = new L2PcInstance(objectId, template, accountName, app);

        player.setName(name);
        player.setBaseClass(player.getClassId());

        // Add the player in the characters table of the database
        boolean ok = player.createDb();
        return ok ? player : null;
    }

    public static Weapon findFistsWeaponItem(int classId) {
        Weapon weaponItem = null;
        if ((classId >= 0x00) && (classId <= 0x09)) {
            // human fighter fists
            Item temp = ItemTable.getInstance().getTemplate(246);
            weaponItem = (Weapon) temp;
        }
        else if ((classId >= 0x0a) && (classId <= 0x11)) {
            // human mage fists
            Item temp = ItemTable.getInstance().getTemplate(251);
            weaponItem = (Weapon) temp;
        }
        else if ((classId >= 0x12) && (classId <= 0x18)) {
            // elven fighter fists
            Item temp = ItemTable.getInstance().getTemplate(244);
            weaponItem = (Weapon) temp;
        }
        else if ((classId >= 0x19) && (classId <= 0x1e)) {
            // elven mage fists
            Item temp = ItemTable.getInstance().getTemplate(249);
            weaponItem = (Weapon) temp;
        }
        else if ((classId >= 0x1f) && (classId <= 0x25)) {
            // dark elven fighter fists
            Item temp = ItemTable.getInstance().getTemplate(245);
            weaponItem = (Weapon) temp;
        }
        else if ((classId >= 0x26) && (classId <= 0x2b)) {
            // dark elven mage fists
            Item temp = ItemTable.getInstance().getTemplate(250);
            weaponItem = (Weapon) temp;
        }
        else if ((classId >= 0x2c) && (classId <= 0x30)) {
            // orc fighter fists
            Item temp = ItemTable.getInstance().getTemplate(248);
            weaponItem = (Weapon) temp;
        }
        else if ((classId >= 0x31) && (classId <= 0x34)) {
            // orc mage fists
            Item temp = ItemTable.getInstance().getTemplate(252);
            weaponItem = (Weapon) temp;
        }
        else if ((classId >= 0x35) && (classId <= 0x39)) {
            // dwarven fists
            Item temp = ItemTable.getInstance().getTemplate(247);
            weaponItem = (Weapon) temp;
        }

        return weaponItem;
    }

    public static L2PcInstance restore(int objectId) {
        L2PcInstance player = null;
        try (Connection con = L2DatabaseFactory.getInstance().getConnection();
             PreparedStatement statement = con.prepareStatement(RESTORE_CHARACTER);) {

            statement.setInt(1, objectId);
            ResultSet rset = statement.executeQuery();

            while (rset.next()) {
                int activeClassId = rset.getInt("classid");
                PcTemplate template = CharTemplateTable.getInstance().getTemplate(activeClassId);
                PcAppearance app = new PcAppearance(rset.getByte("face"), rset.getByte("hairColor"), rset.getByte("hairStyle"), rset.getInt("sex") != 0);

                player = new L2PcInstance(objectId, template, rset.getString("account_name"), app);
                player.setName(rset.getString("char_name"));
                player._lastAccess = rset.getLong("lastAccess");

                player.getStat().setExp(rset.getLong("exp"));
                player._expBeforeDeath = rset.getLong("expBeforeDeath");
                player.getStat().setLevel(rset.getByte("level"));
                player.getStat().setSp(rset.getInt("sp"));

                player._wantsPeace = rset.getInt("wantspeace") == 1;

                player.setHeading(rset.getInt("heading"));

                player.setKarma(rset.getInt("karma"));
                player._pvpKills = rset.getInt("pvpkills");
                player._pkKills = rset.getInt("pkkills");
                player.setOnlineTime(rset.getLong("onlinetime"));
                player.setNoble(rset.getInt("nobless") == 1, false);

                player._clanJoinExpiryTime = rset.getLong("clan_join_expiry_time");
                if (player._clanJoinExpiryTime < System.currentTimeMillis()) {
                    player._clanJoinExpiryTime = (long) 0;
                }

                player._clanCreateExpiryTime = rset.getLong("clan_create_expiry_time");
                if (player._clanCreateExpiryTime < System.currentTimeMillis()) {
                    player._clanCreateExpiryTime = (long) 0;
                }

                player._powerGrade = rset.getInt("power_grade");
                player._pledgeType = rset.getInt("subpledge");
                player._lastRecomUpdate = rset.getLong("last_recom_date");

                int clanId = rset.getInt("clanid");
                if (clanId > 0) { player.setClan(ClanTable.getInstance().getClan(clanId)); }

                if (player.clan != null) {
                    if (player.clan.getLeaderId() != player.getObjectId()) {
                        if (player._powerGrade == 0) {
                            player._powerGrade = 5;
                        }

                        player._clanPrivileges = player.clan.getRankPrivs(player._powerGrade);
                    }
                    else {
                        player._clanPrivileges = L2Clan.CP_ALL;
                        player._powerGrade = 1;
                    }
                }
                else {
                    player._clanPrivileges = L2Clan.CP_NOTHING;
                }

                player._deleteTimer = rset.getLong("deletetime");

                player.setTitle(rset.getString("title"));
                player.setAccessLevel(rset.getInt("accesslevel"));
                player._fistsWeaponItem = findFistsWeaponItem(activeClassId);
                player._uptime = System.currentTimeMillis();

                // Check recs
                player.checkRecom(rset.getInt("rec_have"), rset.getInt("rec_left"));

                player._classIndex = 0;
                try {
                    player._baseClass = rset.getInt("base_class");
                }
                catch (Exception e) {
                    player._baseClass = activeClassId;
                }

                // Restore Subclass Data (cannot be done earlier in function)
                if (restoreSubClassData(player)) {
                    if (activeClassId != player._baseClass) {
                        for (SubClass subClass : player._subClasses.values()) {
                            if (subClass.getClassId() == activeClassId) {
                                player._classIndex = subClass.getClassIndex();
                            }
                        }
                    }
                }
                if (player._classIndex == 0 && activeClassId != player._baseClass) {
                    // Subclass in use but doesn't exist in DB -
                    // a possible restart-while-modifysubclass cheat has been attempted.
                    // Switching to use base class
                    player.setClassId(player._baseClass);
                    _log.warning("Player " + player.getName() + " reverted to base class. Possibly has tried a relogin exploit while subclassing.");
                }
                else { player._activeClass = activeClassId; }

                player._apprentice = rset.getInt("apprentice");
                player._sponsor = rset.getInt("sponsor");
                player._lvlJoinedAcademy = rset.getInt("lvl_joined_academy");
                player._isIn7sDungeon = rset.getInt("isin7sdungeon") == 1;
                player.setPunishLevel(rset.getInt("punish_level"));
                if (player._punishLevel != PunishLevel.NONE) {
                    player._punishTimer = rset.getLong("punish_timer");
                }
                else {
                    player._punishTimer = (long) 0;
                }

                CursedWeaponsManager.getInstance().checkPlayer(player);

                player._alliedVarkaKetra = rset.getInt("varka_ketra_ally");

                player._deathPenaltyBuffLevel = rset.getInt("death_penalty_level");

                // Set the x,y,z position of the L2PcInstance and make it invisible
                player.setXYZInvisible(rset.getInt("x"), rset.getInt("y"), rset.getInt("z"));

                // Set Hero status if it applies
                if (Hero.getInstance().isActiveHero(objectId)) { player.setHero(true); }

                // Set pledge class rank.
                player._pledgeClass = L2ClanMember.calculatePledgeClass(player);

                // Retrieve from the database all secondary data of this L2PcInstance and reward expertise/lucky skills if necessary.
                // Note that Clan, Noblesse and Hero skills are given separately and not here.
                player.restoreCharData();
                player.rewardSkills();

                // buff and status icons
                if (Config.STORE_SKILL_COOLTIME) { player.restoreEffects(); }

                // Restore current CP, HP and MP values
                double currentHp = rset.getDouble("curHp");

                player.setCurrentCp(rset.getDouble("curCp"));
                player.setCurrentHp(currentHp);
                player.setCurrentMp(rset.getDouble("curMp"));

                if (currentHp < 0.5) {
                    player.setIsDead(true);
                    player.stopHpMpRegeneration();
                }

                // Restore pet if exists in the world
                player.summon = L2World.getInstance().getPet(player.getObjectId());
                if (player.summon != null) {
                    player.summon.setOwner(player);
                }

                player.refreshOverloaded();
                player.refreshExpertisePenalty();

                player.restoreFriendList();

                // Retrieve the name and ID of the other characters assigned to this account.
                PreparedStatement stmt = con.prepareStatement("SELECT obj_Id, char_name FROM characters WHERE account_name=? AND obj_Id<>?");
                stmt.setString(1, player.accountName);
                stmt.setInt(2, objectId);
                ResultSet chars = stmt.executeQuery();

                while (chars.next()) { player._chars.put(chars.getInt("obj_Id"), chars.getString("char_name")); }

                chars.close();
                stmt.close();
                break;
            }

            rset.close();
            statement.close();
        }
        catch (Exception e) {
            _log.severe("Could not restore char data: " + e);
        }

        return player;
    }

    private static boolean restoreSubClassData(L2PcInstance player) {
        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement(RESTORE_CHAR_SUBCLASSES);
            statement.setInt(1, player.getObjectId());

            ResultSet rset = statement.executeQuery();

            while (rset.next()) {
                SubClass subClass = new SubClass();
                subClass.setClassId(rset.getInt("class_id"));
                subClass.setLevel(rset.getByte("level"));
                subClass.setExp(rset.getLong("exp"));
                subClass.setSp(rset.getInt("sp"));
                subClass.setClassIndex(rset.getInt("class_index"));

                // Enforce the correct indexing of _subClasses against their class indexes.
                player._subClasses.put(subClass.getClassIndex(), subClass);
            }
            rset.close();
            statement.close();
        }
        catch (Exception e) {
            _log.warning("Could not restore classes for " + player.getName() + ": " + e);
            e.printStackTrace();
        }

        return true;
    }

    public static void teleToTarget(L2PcInstance targetChar, L2PcInstance summonerChar, L2Skill summonSkill) {
        if (targetChar == null || summonerChar == null || summonSkill == null) { return; }

        if (!checkSummonerStatus(summonerChar)) { return; }

        if (!checkSummonTargetStatus(targetChar, summonerChar)) { return; }

        int itemConsumeId = summonSkill.getTargetConsumeId();
        int itemConsumeCount = summonSkill.getTargetConsume();

        if (itemConsumeId != 0 && itemConsumeCount != 0) {
            if (targetChar.inventory.getInventoryItemCount(itemConsumeId, 0) < itemConsumeCount) {
                targetChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_REQUIRED_FOR_SUMMONING).addItemName(summonSkill.getTargetConsumeId()));
                return;
            }

            targetChar.destroyItemByItemId(EItemProcessPurpose.CONSUME, itemConsumeId, itemConsumeCount, targetChar, true);
        }
        targetChar.teleToLocation(summonerChar.getX(), summonerChar.getY(), summonerChar.getZ(), 20);
    }

    public static boolean checkSummonerStatus(L2PcInstance summonerChar) {
        if (summonerChar == null) { return false; }

        return !(summonerChar._inOlympiadMode || summonerChar.inObserverMode() || summonerChar.isInsideZone(ZoneId.NO_SUMMON_FRIEND) || summonerChar.isMounted());

    }

    public static boolean checkSummonTargetStatus(L2Object target, L2PcInstance summonerChar) {
        if (target == null || !target.isPlayer()) { return false; }
        L2PcInstance targetChar = (L2PcInstance) target;

        if (targetChar.isAlikeDead()) {
            summonerChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_IS_DEAD_AT_THE_MOMENT_AND_CANNOT_BE_SUMMONED).addPcName(targetChar));
            return false;
        }

        if (targetChar.isInStoreMode()) {
            summonerChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CURRENTLY_TRADING_OR_OPERATING_PRIVATE_STORE_AND_CANNOT_BE_SUMMONED).addPcName(targetChar));
            return false;
        }

        if (targetChar.isRooted() || targetChar.isInCombat()) {
            summonerChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_IS_ENGAGED_IN_COMBAT_AND_CANNOT_BE_SUMMONED).addPcName(targetChar));
            return false;
        }

        if (targetChar._inOlympiadMode) {
            summonerChar.sendPacket(SystemMessageId.YOU_CANNOT_SUMMON_PLAYERS_WHO_ARE_IN_OLYMPIAD);
            return false;
        }

        if (targetChar.isFestivalParticipant() || targetChar.isMounted()) {
            summonerChar.sendPacket(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING);
            return false;
        }

        if (targetChar.inObserverMode() || targetChar.isInsideZone(ZoneId.NO_SUMMON_FRIEND)) {
            summonerChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_IN_SUMMON_BLOCKING_AREA).addCharName(targetChar));
            return false;
        }

        return true;
    }

    public void gatesRequest(L2DoorInstance door) {
        _gatesRequest.setTarget(door);
    }

    public void gatesAnswer(int answer, int type) {
        if (_gatesRequest.getDoor() == null) { return; }

        if (answer == 1 && getTarget() == _gatesRequest.getDoor() && type == 1) { _gatesRequest.getDoor().openMe(); }
        else if (answer == 1 && getTarget() == _gatesRequest.getDoor() && type == 0) {
            _gatesRequest.getDoor().closeMe();
        }

        _gatesRequest.setTarget(null);
    }

    public String getAccountName() {
        return _client.getAccountName();
    }

    public Map<Integer, String> getAccountChars() {
        return _chars;
    }

    public int getRelationTo(L2PcInstance target) {
        int result = 0;

        // karma and pvp may not be required
        if (_pvpFlag != 0) { result |= RelationChanged.RELATION_PVP_FLAG; }
        if (_karma > 0) { result |= RelationChanged.RELATION_HAS_KARMA; }

        if (isClanLeader()) { result |= RelationChanged.RELATION_LEADER; }

        if (_siegeState != 0) {
            result |= RelationChanged.RELATION_INSIEGE;
            result |= (_siegeState == target._siegeState) ? RelationChanged.RELATION_ALLY : RelationChanged.RELATION_ENEMY;

            if (_siegeState == 1) { result |= RelationChanged.RELATION_ATTACKER; }
        }

        if (clan != null && target.clan != null) {
            if (target._pledgeType != L2Clan.SUBUNIT_ACADEMY && _pledgeType != L2Clan.SUBUNIT_ACADEMY && target.clan.isAtWarWith(clan.getClanId())) {
                result |= RelationChanged.RELATION_1SIDED_WAR;
                if (clan.isAtWarWith(target.clan.getClanId())) {
                    result |= RelationChanged.RELATION_MUTUAL_WAR;
                }
            }
        }
        return result;
    }

    private void initPcStatusUpdateValues() {
        _cpUpdateInterval = getMaxCp() / 352.0;
        _cpUpdateIncCheck = getMaxCp();
        _cpUpdateDecCheck = getMaxCp() - _cpUpdateInterval;
        _mpUpdateInterval = getMaxMp() / 352.0;
        _mpUpdateIncCheck = getMaxMp();
        _mpUpdateDecCheck = getMaxMp() - _mpUpdateInterval;
    }

    @Override
    public void addFuncsToNewCharacter() {
        // Add L2Character functionalities.
        super.addFuncsToNewCharacter();

        addStatFunc(FuncMaxCpMul.getInstance());

        addStatFunc(FuncHennaSTR.getInstance());
        addStatFunc(FuncHennaDEX.getInstance());
        addStatFunc(FuncHennaINT.getInstance());
        addStatFunc(FuncHennaMEN.getInstance());
        addStatFunc(FuncHennaCON.getInstance());
        addStatFunc(FuncHennaWIT.getInstance());
    }

    @Override
    public void initKnownList() {
        setKnownList(new PcKnownList(this));
    }

    @Override
    public final PcKnownList getKnownList() {
        return (PcKnownList) super.getKnownList();
    }

    @Override
    public void initCharStat() {
        setStat(new PcStat(this));
    }

    @Override
    public final PcStat getStat() {
        return (PcStat) super.getStat();
    }

    @Override
    public void initCharStatus() {
        setStatus(new PcStatus(this));
    }

    @Override
    public final PcStatus getStatus() {
        return (PcStatus) super.getStatus();
    }

    @Override
    public void initPosition() {
        setObjectPosition(new PcPosition(this));
    }

    @Override
    public PcPosition getPosition() {
        return (PcPosition) super.getPosition();
    }

    public final PcAppearance getAppearance() {
        return appearance;
    }

    /**
     * @return the base L2PcTemplate link to the L2PcInstance.
     */
    public final PcTemplate getBaseTemplate() {
        return CharTemplateTable.getInstance().getTemplate(_baseClass);
    }

    /** Return the L2PcTemplate link to the L2PcInstance. */
    @Override
    public final PcTemplate getTemplate() {
        return (PcTemplate) super.getTemplate();
    }

    public void setTemplate(ClassId newclass) {
        setTemplate(CharTemplateTable.getInstance().getTemplate(newclass));
    }

    /**
     * Return the AI of the L2PcInstance (create it if necessary).
     */
    @Override
    public L2CharacterAI getAI() {
        L2CharacterAI ai = this.ai;
        if (ai == null) {
            synchronized (this) {
                if (this.ai == null) { this.ai = new L2PlayerAI(this); }

                return this.ai;
            }
        }
        return ai;
    }

    /** Return the Level of the L2PcInstance. */
    @Override
    public final int getLevel() {
        return getStat().getLevel();
    }

    /**
     * A newbie is a player reaching level 6. He isn't considered newbie at lvl 25.<br>
     * Since IL newbie isn't anymore the first character of an account reaching that state, but any.
     *
     * @return True if newbie.
     */
    public boolean isNewbie() {
        return getClassId().level() <= 1 && getLevel() >= 6 && getLevel() <= 25;
    }

    public void setBaseClass(int baseClass) {
        _baseClass = baseClass;
    }

    public boolean isInStoreMode() {
        return _privateStoreType != PrivateStoreType.NONE;
    }

    public boolean isInCraftMode() {
        return _inCraftMode;
    }

    public void isInCraftMode(boolean b) {
        _inCraftMode = b;
    }

    public void logout() {
        logout(true);
    }

    public void logout(L2GameServerPacket packet) {
        sendPacket(packet);
        logout();
    }

    public void logout(boolean closeClient) {
        try {
            closeNetConnection(closeClient);
        }
        catch (Exception e) {
            _log.log(Level.WARNING, "Exception on logout(): " + e.getMessage(), e);
        }
    }

    /**
     * @return a table containing all Common RecipeList of the L2PcInstance.
     */
    public Collection<RecipeList> getCommonRecipeBook() {
        return _commonRecipeBook.values();
    }

    /**
     * @return a table containing all Dwarf RecipeList of the L2PcInstance.
     */
    public Collection<RecipeList> getDwarvenRecipeBook() {
        return _dwarvenRecipeBook.values();
    }

    /**
     * Add a new L2RecipList to the table _commonrecipebook containing all RecipeList of the L2PcInstance.
     *
     * @param recipe The RecipeList to add to the _recipebook
     */
    public void registerCommonRecipeList(RecipeList recipe) {
        _commonRecipeBook.put(recipe.getId(), recipe);
    }

    /**
     * Add a new L2RecipList to the table _recipebook containing all RecipeList of the L2PcInstance.
     *
     * @param recipe The RecipeList to add to the _recipebook
     */
    public void registerDwarvenRecipeList(RecipeList recipe) {
        _dwarvenRecipeBook.put(recipe.getId(), recipe);
    }

    /**
     * @param recipeId The Identifier of the RecipeList to check in the player's recipe books
     * @return <b>TRUE</b> if player has the recipe on Common or Dwarven Recipe book else returns <b>FALSE</b>
     */
    public boolean hasRecipeList(int recipeId) {
        return _dwarvenRecipeBook.containsKey(recipeId) || _commonRecipeBook.containsKey(recipeId);
    }

    /**
     * Tries to remove a L2RecipList from the table _DwarvenRecipeBook or from table _CommonRecipeBook, those table contain all RecipeList of the L2PcInstance.
     *
     * @param recipeId The Identifier of the RecipeList to remove from the _recipebook.
     */
    public void unregisterRecipeList(int recipeId) {
        if (_dwarvenRecipeBook.containsKey(recipeId)) { _dwarvenRecipeBook.remove(recipeId); }
        else if (_commonRecipeBook.containsKey(recipeId)) { _commonRecipeBook.remove(recipeId); }
        else { _log.warning("Attempted to remove unknown RecipeList: " + recipeId); }

        for (L2ShortCut sc : getAllShortCuts()) {
            if (sc != null && sc.getId() == recipeId && sc.getType() == L2ShortCut.TYPE_RECIPE) {
                deleteShortCut(sc.getSlot(), sc.getPage());
            }
        }
    }

    /**
     * @return the Id for the last talked quest NPC.
     */
    public int getLastQuestNpcObject() {
        return _questNpcObject;
    }

    public void setLastQuestNpcObject(int npcId) {
        _questNpcObject = npcId;
    }

    /**
     * @param name The name of the quest.
     * @return The QuestState object corresponding to the quest name.
     */
    public QuestState getQuestState(String name) {
        for (QuestState qs : _quests) {
            if (name.equals(qs.getQuest().getName())) { return qs; }
        }
        return null;
    }

    /**
     * Add a QuestState to the table _quest containing all quests began by the L2PcInstance.
     *
     * @param qs The QuestState to add to _quest.
     */
    public void setQuestState(QuestState qs) {
        _quests.add(qs);
    }

    /**
     * Remove a QuestState from the table _quest containing all quests began by the L2PcInstance.
     *
     * @param qs : The QuestState to be removed from _quest.
     */
    public void delQuestState(QuestState qs) {
        _quests.remove(qs);
    }

    /**
     * @param completed : If true, include completed quests to the list.
     * @return list of started and eventually completed quests of the player.
     */
    public List<Quest> getAllQuests(boolean completed) {
        List<Quest> quests = new ArrayList<>();

        for (QuestState qs : _quests) {
            if (qs == null || completed && qs.isCreated() || !completed && !qs.isStarted()) { continue; }

            Quest quest = qs.getQuest();
            if (quest == null || !quest.isRealQuest()) { continue; }

            quests.add(quest);
        }

        return quests;
    }

    public void processQuestEvent(String questName, String event) {
        Quest quest = ScriptManager.getInstance().getQuest(questName);
        if (quest == null) { return; }

        QuestState qs = getQuestState(questName);
        if (qs == null) { return; }

        L2Object object = L2World.getInstance().getObject(_questNpcObject);
        if (!(object instanceof L2Npc) || !isInsideRadius(object, L2Npc.INTERACTION_DISTANCE, false, false)) { return; }

        L2Npc npc = (L2Npc) object;
        List<Quest> quests = npc.getTemplate().getEventQuests(EventType.ON_TALK);
        if (quests != null) {
            for (Quest onTalk : quests) {
                if (onTalk == null || !onTalk.equals(quest)) { continue; }

                quest.notifyEvent(event, npc, this);
                break;
            }
        }
    }

    /**
     * Add QuestState instance that is to be notified of L2PcInstance's death.
     *
     * @param qs The QuestState that subscribe to this event
     */
    public void addNotifyQuestOfDeath(QuestState qs) {
        if (qs == null) { return; }

        if (!_notifyQuestOfDeathList.contains(qs)) { _notifyQuestOfDeathList.add(qs); }
    }

    /**
     * Remove QuestState instance that is to be notified of L2PcInstance's death.
     *
     * @param qs The QuestState that subscribe to this event
     */
    public void removeNotifyQuestOfDeath(QuestState qs) {
        if (qs == null) { return; }

        _notifyQuestOfDeathList.remove(qs);
    }

    /**
     * @return A list of QuestStates which registered for notify of death of this L2PcInstance.
     */
    public final List<QuestState> getNotifyQuestOfDeath() {
        return _notifyQuestOfDeathList;
    }

    /**
     * @return player memos.
     */
    public PlayerMemo getMemos() {
        return _vars;
    }

    /**
     * @return A table containing all L2ShortCut of the L2PcInstance.
     */
    public L2ShortCut[] getAllShortCuts() {
        return _shortCuts.getAllShortCuts();
    }

    /**
     * @param slot The slot in wich the shortCuts is equipped
     * @param page The page of shortCuts containing the slot
     * @return The L2ShortCut of the L2PcInstance corresponding to the position (page-slot).
     */
    public L2ShortCut getShortCut(int slot, int page) {
        return _shortCuts.getShortCut(slot, page);
    }

    /**
     * Add a L2shortCut to the L2PcInstance _shortCuts
     *
     * @param shortcut The shortcut to add.
     */
    public void registerShortCut(L2ShortCut shortcut) {
        _shortCuts.registerShortCut(shortcut);
    }

    /**
     * Delete the L2ShortCut corresponding to the position (page-slot) from the L2PcInstance _shortCuts.
     *
     * @param slot
     * @param page
     */
    public void deleteShortCut(int slot, int page) {
        _shortCuts.deleteShortCut(slot, page);
    }

    /**
     * Add a L2Macro to the L2PcInstance _macroses.
     *
     * @param macro The Macro object to add.
     */
    public void registerMacro(L2Macro macro) {
        _macroses.registerMacro(macro);
    }

    /**
     * Delete the L2Macro corresponding to the Identifier from the L2PcInstance _macroses.
     *
     * @param id
     */
    public void deleteMacro(int id) {
        _macroses.deleteMacro(id);
    }

    /**
     * @return all L2Macro of the L2PcInstance.
     */
    public MacroList getMacroses() {
        return _macroses;
    }

    /**
     * @return the siege state of the L2PcInstance.
     */
    public byte getSiegeState() {
        return _siegeState;
    }

    /**
     * Set the siege state of the L2PcInstance.
     *
     * @param siegeState 1 = attacker, 2 = defender, 0 = not involved
     */
    public void setSiegeState(byte siegeState) {
        _siegeState = siegeState;
    }

    @Override
    public byte getPvpFlag() {
        return _pvpFlag;
    }

    /**
     * Set the PvP Flag of the L2PcInstance.
     *
     * @param pvpFlag 0 or 1.
     */
    public void setPvpFlag(int pvpFlag) {
        _pvpFlag = (byte) pvpFlag;
    }

    public void updatePvPFlag(int value) {
        if (_pvpFlag == value) { return; }

        setPvpFlag(value);
        sendPacket(new UserInfo(this));

        if (summon != null) {
            sendPacket(new RelationChanged(summon, getRelationTo(this), false));
        }

        broadcastRelationsChanges();
    }

    @Override
    public void revalidateZone(boolean force) {
        // Cannot validate if not in a world region (happens during teleport)
        if (getWorldRegion() == null) { return; }

        // This function is called very often from movement code
        if (force) { _zoneValidateCounter = 4; }
        else {
            _zoneValidateCounter--;
            if (_zoneValidateCounter >= 0) { return; }

            _zoneValidateCounter = 4;
        }

        getWorldRegion().revalidateZones(this);

        checkWaterState();

        if (isInsideZone(ZoneId.SIEGE)) {
            if (_lastCompassZone == ExSetCompassZoneCode.SIEGEWARZONE2) { return; }

            _lastCompassZone = ExSetCompassZoneCode.SIEGEWARZONE2;
            sendPacket(new ExSetCompassZoneCode(ExSetCompassZoneCode.SIEGEWARZONE2));
        }
        else if (isInsideZone(ZoneId.PVP)) {
            if (_lastCompassZone == ExSetCompassZoneCode.PVPZONE) { return; }

            _lastCompassZone = ExSetCompassZoneCode.PVPZONE;
            sendPacket(new ExSetCompassZoneCode(ExSetCompassZoneCode.PVPZONE));
        }
        else if (_isIn7sDungeon) {
            if (_lastCompassZone == ExSetCompassZoneCode.SEVENSIGNSZONE) { return; }

            _lastCompassZone = ExSetCompassZoneCode.SEVENSIGNSZONE;
            sendPacket(new ExSetCompassZoneCode(ExSetCompassZoneCode.SEVENSIGNSZONE));
        }
        else if (isInsideZone(ZoneId.PEACE)) {
            if (_lastCompassZone == ExSetCompassZoneCode.PEACEZONE) { return; }

            _lastCompassZone = ExSetCompassZoneCode.PEACEZONE;
            sendPacket(new ExSetCompassZoneCode(ExSetCompassZoneCode.PEACEZONE));
        }
        else {
            if (_lastCompassZone == ExSetCompassZoneCode.GENERALZONE) { return; }

            if (_lastCompassZone == ExSetCompassZoneCode.SIEGEWARZONE2) { updatePvPStatus(); }

            _lastCompassZone = ExSetCompassZoneCode.GENERALZONE;
            sendPacket(new ExSetCompassZoneCode(ExSetCompassZoneCode.GENERALZONE));
        }
    }

    /**
     * @return True if the L2PcInstance can Craft Dwarven Recipes.
     */
    public boolean hasDwarvenCraft() {
        return getSkillLevel(L2Skill.SKILL_CREATE_DWARVEN) >= 1;
    }

    public int getDwarvenCraft() {
        return getSkillLevel(L2Skill.SKILL_CREATE_DWARVEN);
    }

    /**
     * @return True if the L2PcInstance can Craft Dwarven Recipes.
     */
    public boolean hasCommonCraft() {
        return getSkillLevel(L2Skill.SKILL_CREATE_COMMON) >= 1;
    }

    public int getCommonCraft() {
        return getSkillLevel(L2Skill.SKILL_CREATE_COMMON);
    }

    /**
     * @return the PK counter of the L2PcInstance.
     */
    public int getPkKills() {
        return _pkKills;
    }

    /**
     * Set the PK counter of the L2PcInstance.
     *
     * @param pkKills A number.
     */
    public void setPkKills(int pkKills) {
        _pkKills = pkKills;
    }

    /**
     * @return The _deleteTimer of the L2PcInstance.
     */
    public long getDeleteTimer() {
        return _deleteTimer;
    }

    /**
     * Set the _deleteTimer of the L2PcInstance.
     *
     * @param deleteTimer Time in ms.
     */
    public void setDeleteTimer(long deleteTimer) {
        _deleteTimer = deleteTimer;
    }

    /**
     * @return The current weight of the L2PcInstance.
     */
    public int getCurrentLoad() {
        return inventory.getTotalWeight();
    }

    /**
     * @return The date of last update of recomPoints.
     */
    public long getLastRecomUpdate() {
        return _lastRecomUpdate;
    }

    public void setLastRecomUpdate(long date) {
        _lastRecomUpdate = date;
    }

    /**
     * @return The number of recommandation obtained by the L2PcInstance.
     */
    public int getRecomHave() {
        return _recomHave;
    }

    /**
     * Set the number of recommandations obtained by the L2PcInstance (Max : 255).
     *
     * @param value Number of recommandations obtained.
     */
    public void setRecomHave(int value) {
        if (value > 255) { _recomHave = 255; }
        else if (value < 0) { _recomHave = 0; }
        else { _recomHave = value; }
    }

    /**
     * Increment the number of recommandation obtained by the L2PcInstance (Max : 255).
     */
    protected void incRecomHave() {
        if (_recomHave < 255) { _recomHave++; }
    }

    /**
     * @return The number of recommandation that the L2PcInstance can give.
     */
    public int getRecomLeft() {
        return _recomLeft;
    }

    /**
     * Increment the number of recommandation that the L2PcInstance can give.
     */
    protected void decRecomLeft() {
        if (_recomLeft > 0) { _recomLeft--; }
    }

    public void giveRecom(L2PcInstance target) {
        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement(ADD_CHAR_RECOM);
            statement.setInt(1, getObjectId());
            statement.setInt(2, target.getObjectId());
            statement.execute();
            statement.close();
        }
        catch (Exception e) {
            _log.warning("Could not update char recommendations: " + e);
        }

        target.incRecomHave();
        decRecomLeft();
        _recomChars.add(target.getObjectId());
    }

    public boolean canRecom(L2PcInstance target) {
        return !_recomChars.contains(target.getObjectId());
    }

    public long getExpBeforeDeath() {
        return _expBeforeDeath;
    }

    /**
     * Set the exp of the L2PcInstance before a death
     *
     * @param exp
     */
    public void setExpBeforeDeath(long exp) {
        _expBeforeDeath = exp;
    }

    /**
     * Return the Karma of the L2PcInstance.
     */
    @Override
    public int getKarma() {
        return _karma;
    }

    /**
     * Set the Karma of the L2PcInstance and send StatusUpdate (broadcast).
     *
     * @param karma A value.
     */
    public void setKarma(int karma) {
        if (karma < 0) { karma = 0; }

        if (_karma > 0 && karma == 0) {
            sendPacket(new UserInfo(this));
            broadcastRelationsChanges();
        }

        // send message with new karma value
        sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOUR_KARMA_HAS_BEEN_CHANGED_TO_S1).addNumber(karma));

        _karma = karma;
        broadcastKarma();
    }

    /**
     * Weight Limit = (CON Modifier*69000)*Skills
     *
     * @return The max weight that the L2PcInstance can load.
     */
    public int getMaxLoad() {
        int con = getCON();
        if (con < 1) { return 31000; }

        if (con > 59) { return 176000; }

        double baseLoad = Math.pow(1.029993928, con) * 30495.627366;
        return (int) calcStat(Stats.MAX_LOAD, baseLoad * Config.ALT_WEIGHT_LIMIT, this, null);
    }

    public int getExpertiseArmorPenalty() {
        return _expertiseArmorPenalty;
    }

    public boolean getExpertiseWeaponPenalty() {
        return _expertiseWeaponPenalty;
    }

    public int getWeightPenalty() {
        return _curWeightPenalty;
    }

    /**
     * Update the overloaded status of the L2PcInstance.
     */
    public void refreshOverloaded() {
        int maxLoad = getMaxLoad();
        if (maxLoad > 0) {
            int weightproc = getCurrentLoad() * 1000 / maxLoad;
            int newWeightPenalty;

            if (weightproc < 500) { newWeightPenalty = 0; }
            else if (weightproc < 666) { newWeightPenalty = 1; }
            else if (weightproc < 800) { newWeightPenalty = 2; }
            else if (weightproc < 1000) { newWeightPenalty = 3; }
            else { newWeightPenalty = 4; }

            if (_curWeightPenalty != newWeightPenalty) {
                _curWeightPenalty = newWeightPenalty;

                if (newWeightPenalty > 0) {
                    addSkill(SkillTable.getInfo(4270, newWeightPenalty));
                    setIsOverloaded(getCurrentLoad() > maxLoad);
                }
                else {
                    super.removeSkill(getKnownSkill(4270));
                    setIsOverloaded(false);
                }

                sendPacket(new UserInfo(this));
                sendPacket(new EtcStatusUpdate(this));
                broadcastCharInfo();
            }
        }
    }

    /**
     * Refresh expertise level ; weapon got one rank, when armor got 4 ranks.<br>
     */
    public void refreshExpertisePenalty() {
        int armorPenalty = 0;
        boolean weaponPenalty = false;

        for (L2ItemInstance item : inventory.getItems()) {
            if (item != null && item.isEquipped() && item.getItemType() != EtcItemType.ARROW && item.getItem().getCrystalType().getId() > _expertiseIndex) {
                if (item.isWeapon()) { weaponPenalty = true; }
                else { armorPenalty += (item.getItem().getBodyPart() == EItemBodyPart.SLOT_FULL_ARMOR) ? 2 : 1; }
            }
        }

        armorPenalty = Math.min(armorPenalty, 4);

        // Found a different state than previous ; update it.
        if (_expertiseWeaponPenalty != weaponPenalty || _expertiseArmorPenalty != armorPenalty) {
            _expertiseWeaponPenalty = weaponPenalty;
            _expertiseArmorPenalty = armorPenalty;

            // Passive skill "Grade Penalty" is either granted or dropped.
            if (_expertiseWeaponPenalty || _expertiseArmorPenalty > 0) {
                addSkill(SkillTable.getInfo(4267, 1));
            }
            else { super.removeSkill(getKnownSkill(4267)); }

            sendSkillList();
            sendPacket(new EtcStatusUpdate(this));

            L2ItemInstance weapon = inventory.getPaperdollItem(EPaperdollSlot.PAPERDOLL_RHAND);
            if (weapon != null) {
                if (_expertiseWeaponPenalty) {
                    // TODO! Какого хрена? Почему в рубашку?!
                    ItemPassiveSkillsListener.getInstance().onUnequip(EPaperdollSlot.PAPERDOLL_UNDER, weapon, this);
                }
                else {
                    // TODO! Какого хрена? Почему в рубашку?!
                    ItemPassiveSkillsListener.getInstance().onEquip(EPaperdollSlot.PAPERDOLL_UNDER, weapon, this);
                }
            }
        }
    }

    /**
     * Equip or unequip the item.
     * <UL>
     * <LI>If item is equipped, shots are applied if automation is on.</LI>
     * <LI>If item is unequipped, shots are discharged.</LI>
     * </UL>
     *
     * @param item        The item to charge/discharge.
     * @param abortAttack If true, the current attack will be aborted in order to equip the item.
     */
    public void useEquippableItem(L2ItemInstance item, boolean abortAttack) {
        L2ItemInstance[] items = null;
        boolean isEquipped = item.isEquipped();
        int oldInvLimit = getInventoryLimit();
        SystemMessage sm = null;

        if (item.getItem() instanceof Weapon) { item.unChargeAllShots(); }

        if (isEquipped) {
            if (item.getEnchantLevel() > 0) {
                sm = SystemMessage.getSystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED).addNumber(item.getEnchantLevel()).addItemName(item);
            }
            else { sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISARMED).addItemName(item); }

            sendPacket(sm);

            EItemBodyPart slot = inventory.getSlotFromItem(item);
            items = inventory.unEquipItemInBodySlotAndRecord(slot);
        }
        else {
            items = inventory.equipItemAndRecord(item);

            if (item.isEquipped()) {
                if (item.getEnchantLevel() > 0) {
                    sm = SystemMessage.getSystemMessage(SystemMessageId.S1_S2_EQUIPPED).addNumber(item.getEnchantLevel()).addItemName(item);
                }
                else { sm = SystemMessage.getSystemMessage(SystemMessageId.S1_EQUIPPED).addItemName(item); }

                sendPacket(sm);

                if (EItemBodyPart.SLOT_ALLWEAPON.isInBodyPart(item.getItem().getBodyPart())) {
                    rechargeShots(true, true);
                }
            }
            else { sendPacket(SystemMessageId.CANNOT_EQUIP_ITEM_DUE_TO_BAD_CONDITION); }
        }
        refreshExpertisePenalty();
        broadcastUserInfo();

        InventoryUpdate iu = new InventoryUpdate();
        iu.addItems(Arrays.asList(items));
        sendPacket(iu);

        if (abortAttack) { abortAttack(); }

        if (getInventoryLimit() != oldInvLimit) { sendPacket(new ExStorageMaxCount(this)); }
    }

    /**
     * @return PvP Kills of the L2PcInstance (number of player killed during a PvP).
     */
    public int getPvpKills() {
        return _pvpKills;
    }

    /**
     * Set PvP Kills of the L2PcInstance (number of player killed during a PvP).
     *
     * @param pvpKills A value.
     */
    public void setPvpKills(int pvpKills) {
        _pvpKills = pvpKills;
    }

    /**
     * @return The ClassId object of the L2PcInstance contained in L2PcTemplate.
     */
    public ClassId getClassId() {
        return getTemplate().getClassId();
    }

    /**
     * Set the template of the L2PcInstance.
     *
     * @param Id The Identifier of the L2PcTemplate to set to the L2PcInstance
     */
    public void setClassId(int Id) {
        if (!_subclassLock.tryLock()) { return; }

        try {
            if (_lvlJoinedAcademy != 0 && clan != null && PlayerClass.values()[Id].getLevel() == ClassLevel.Third) {
                if (_lvlJoinedAcademy <= 16) { clan.addReputationScore(400); }
                else if (_lvlJoinedAcademy >= 39) { clan.addReputationScore(170); }
                else {
                    clan.addReputationScore(400 - (_lvlJoinedAcademy - 16) * 10);
                }

                _lvlJoinedAcademy = 0;

                // Oust pledge member from the academy, because he has finished his 2nd class transfer.
                clan.broadcastToOnlineMembers(new PledgeShowMemberListDelete(getName()), SystemMessage.getSystemMessage(SystemMessageId.CLAN_MEMBER_S1_EXPELLED).addString(getName()));
                clan.removeClanMember(getObjectId(), 0);
                sendPacket(SystemMessageId.ACADEMY_MEMBERSHIP_TERMINATED);

                // receive graduation gift : academy circlet
                addItem(EItemProcessPurpose.GIFT, 8181, 1, this, true);
            }

            if (isSubClassActive()) {
                _subClasses.get(_classIndex).setClassId(Id);
            }

            broadcastPacket(new MagicSkillUse(this, this, 5103, 1, 1000, 0));
            setClassTemplate(Id);

            if (getClassId().level() == 3) { sendPacket(SystemMessageId.THIRD_CLASS_TRANSFER); }
            else { sendPacket(SystemMessageId.CLASS_TRANSFER); }

            // Update class icon in party and clan
            if (isInParty()) {
                _party.broadcastToPartyMembers(new PartySmallWindowUpdate(this));
            }

            if (clan != null) {
                clan.broadcastToOnlineMembers(new PledgeShowMemberListUpdate(this));
            }

            if (Config.AUTO_LEARN_SKILLS) { rewardSkills(); }
        }
        finally {
            _subclassLock.unlock();
        }
    }

    /**
     * @return the Experience of the L2PcInstance.
     */
    public long getExp() {
        return getStat().getExp();
    }

    public L2ItemInstance getActiveEnchantItem() {
        return _activeEnchantItem;
    }

    public void setActiveEnchantItem(L2ItemInstance scroll) {
        _activeEnchantItem = scroll;
    }

    /**
     * @return The fists weapon of the L2PcInstance (used when no weapon is equipped).
     */
    public Weapon getFistsWeaponItem() {
        return _fistsWeaponItem;
    }

    /**
     * Set the fists weapon of the L2PcInstance (used when no weapon is equipped).
     *
     * @param weaponItem The fists Weapon to set to the L2PcInstance
     */
    public void setFistsWeaponItem(Weapon weaponItem) {
        _fistsWeaponItem = weaponItem;
    }

    /**
     * This method is kinda polymorph :
     * <ul>
     * <li>it gives proper Expertise, Dwarven && Common Craft skill level ;</li>
     * <li>it controls the Lucky skill (remove at lvl 10) ;</li>
     * <li>it finally sends the skill list.</li>
     * </ul>
     */
    public void rewardSkills() {
        // Get the Level of the L2PcInstance
        int lvl = getLevel();

        // Remove the Lucky skill once reached lvl 10.
        if (getSkillLevel(L2Skill.SKILL_LUCKY) > 0 && lvl >= 10) { removeSkill(FrequentSkill.LUCKY.getSkill()); }

        // Calculate the current higher Expertise of the L2PcInstance
        for (int i = 0; i < EXPERTISE_LEVELS.length; i++) {
            if (lvl >= EXPERTISE_LEVELS[i]) {
                _expertiseIndex = i;
            }
        }

        // Add the Expertise skill corresponding to its Expertise level
        if (_expertiseIndex > 0) {
            L2Skill skill = SkillTable.getInfo(239, _expertiseIndex);
            addSkill(skill, true);
        }

        // Active skill dwarven craft
        if (getSkillLevel(1321) < 1 && getClassId().equalsOrChildOf(ClassId.dwarvenFighter)) {
            L2Skill skill = FrequentSkill.DWARVEN_CRAFT.getSkill();
            addSkill(skill, true);
        }

        // Active skill common craft
        if (getSkillLevel(1322) < 1) {
            L2Skill skill = FrequentSkill.COMMON_CRAFT.getSkill();
            addSkill(skill, true);
        }

        for (int i = 0; i < COMMON_CRAFT_LEVELS.length; i++) {
            if (lvl >= COMMON_CRAFT_LEVELS[i] && getSkillLevel(1320) < (i + 1)) {
                L2Skill skill = SkillTable.getInfo(1320, i + 1);
                addSkill(skill, true);
            }
        }

        // Auto-Learn skills if activated
        if (Config.AUTO_LEARN_SKILLS) { giveAvailableSkills(); }

        sendSkillList();
    }

    /**
     * Regive all skills which aren't saved to database, like Noble, Hero, Clan Skills.<br>
     * <b>Do not call this on enterworld or char load.</b>.
     */
    private void regiveTemporarySkills() {
        // Add noble skills if noble.
        if (_noble) { setNoble(true, false); }

        // Add Hero skills if hero.
        if (_hero) { setHero(true); }

        // Add clan skills.
        if (clan != null) {
            clan.addSkillEffects(this);

            if (clan.getLevel() >= SiegeManager.MINIMUM_CLAN_LEVEL && isClanLeader()) {
                SiegeManager.addSiegeSkills(this);
            }
        }

        // Reload passive skills from armors / jewels / weapons
        inventory.reloadEquippedItems();

        // Add Death Penalty Buff Level
        restoreDeathPenaltyBuffLevel();
    }

    /**
     * Give all available skills to the player.
     *
     * @return The number of given skills.
     */
    public int giveAvailableSkills() {
        int result = 0;
        for (L2SkillLearn sl : SkillTreeTable.getInstance().getAllAvailableSkills(this, getClassId())) {
            addSkill(SkillTable.getInfo(sl.getId(), sl.getLevel()), true);
            result++;
        }
        return result;
    }

    /**
     * @return The Race object of the L2PcInstance.
     */
    public Race getRace() {
        if (!isSubClassActive()) { return getTemplate().getRace(); }

        return CharTemplateTable.getInstance().getTemplate(_baseClass).getRace();
    }

    public L2Radar getRadar() {
        return radar;
    }

    /**
     * @return the SP amount of the L2PcInstance.
     */
    public int getSp() {
        return getStat().getSp();
    }

    /**
     * @param castleId The castle to check.
     * @return True if this L2PcInstance is a clan leader in ownership of the passed castle.
     */
    public boolean isCastleLord(int castleId) {
        L2Clan clan = this.clan;

        // player has clan and is the clan leader, check the castle info
        if ((clan != null) && (clan.getLeader().getPlayerInstance() == this)) {
            // if the clan has a castle and it is actually the queried castle, return true
            Castle castle = CastleManager.getInstance().getCastleByOwner(clan);
            if ((castle != null) && (castle == CastleManager.getInstance().getCastleById(castleId))) { return true; }
        }

        return false;
    }

    /**
     * @return The Clan Identifier of the L2PcInstance.
     */
    public int getClanId() {
        return _clanId;
    }

    /**
     * @return The Clan Crest Identifier of the L2PcInstance or 0.
     */
    public int getClanCrestId() {
        if (clan != null) { return clan.getCrestId(); }

        return 0;
    }

    /**
     * @return The Clan CrestLarge Identifier or 0
     */
    public int getClanCrestLargeId() {
        if (clan != null) { return clan.getCrestLargeId(); }

        return 0;
    }

    public long getClanJoinExpiryTime() {
        return _clanJoinExpiryTime;
    }

    public void setClanJoinExpiryTime(long time) {
        _clanJoinExpiryTime = time;
    }

    public long getClanCreateExpiryTime() {
        return _clanCreateExpiryTime;
    }

    public void setClanCreateExpiryTime(long time) {
        _clanCreateExpiryTime = time;
    }

    public void setOnlineTime(long time) {
        _onlineTime = time;
        _onlineBeginTime = System.currentTimeMillis();
    }

    /**
     * Return the PcInventory Inventory of the L2PcInstance contained in inventory.
     */
    @Override
    public PcInventory getInventory() {
        return inventory;
    }

    /**
     * Delete a ShortCut of the L2PcInstance _shortCuts.
     *
     * @param objectId The shortcut id.
     */
    public void removeItemFromShortCut(int objectId) {
        _shortCuts.deleteShortCutByObjectId(objectId);
    }

    /**
     * @return True if the L2PcInstance is sitting.
     */
    public boolean isSitting() {
        return _waitTypeSitting;
    }

    /**
     * Set _waitTypeSitting to given value.
     *
     * @param state A boolean.
     */
    public void setIsSitting(boolean state) {
        _waitTypeSitting = state;
    }

    /**
     * Sit down the L2PcInstance, set the AI Intention to REST and send ChangeWaitType packet (broadcast)
     */
    public void sitDown() {
        sitDown(true);
    }

    public void sitDown(boolean checkCast) {
        if (checkCast && isCastingNow()) { return; }

        if (!_waitTypeSitting && !isAttackingDisabled() && !isOutOfControl() && !isImmobilized()) {
            breakAttack();
            _waitTypeSitting = true;
            broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_SITTING));

            // Schedule a sit down task to wait for the animation to finish
            getAI().setIntention(EIntention.REST);
            ThreadPoolManager.getInstance().scheduleGeneral(new SitDownTask(), 2500);
            setIsParalyzed(true);
        }
    }

    /**
     * Stand up the L2PcInstance, set the AI Intention to IDLE and send ChangeWaitType packet (broadcast)
     */
    public void standUp() {
        if (_waitTypeSitting && !isInStoreMode() && !isAlikeDead() && !isParalyzed()) {
            if (_effects.isAffected(L2EffectFlag.RELAXING)) { stopEffects(L2EffectType.RELAXING); }

            broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_STANDING));
            // Schedule a stand up task to wait for the animation to finish
            ThreadPoolManager.getInstance().scheduleGeneral(new StandUpTask(), 2500);
            setIsParalyzed(true);
        }
    }

    /**
     * Stands up and close any opened shop window, if any.
     */
    public void forceStandUp() {
        // Cancels any shop types.
        if (isInStoreMode()) {
            _privateStoreType = PrivateStoreType.NONE;
            broadcastUserInfo();
        }

        // Stand up.
        standUp();
    }

    /**
     * Used to sit or stand. If not possible, queue the action.
     *
     * @param target       The target, used for thrones types.
     * @param sittingState The sitting state, inheritated from packet or player status.
     */
    public void tryToSitOrStand(L2Object target, boolean sittingState) {
        if (_isFakeDeath) {
            stopFakeDeath(true);
            return;
        }

        boolean isThrone = target instanceof L2StaticObjectInstance && ((L2StaticObjectInstance) target).getType() == 1;

        // Player wants to sit on a throne but is out of radius, move to the throne delaying the sit action.
        if (isThrone && !sittingState && !isInsideRadius(target, L2Npc.INTERACTION_DISTANCE, false, false)) {
            getAI().setIntention(EIntention.MOVE_TO, new HeadedLocation(target.getX(), target.getY(), target.getZ(), 0));

            NextAction nextAction = new NextAction(ECtrlEvent.EVT_ARRIVED, EIntention.MOVE_TO, new Runnable() {
                @Override
                public void run() {
                    if (getMountType() != 0) { return; }

                    sitDown();

                    if (!((L2StaticObjectInstance) target).isBusy()) {
                        ((L2StaticObjectInstance) target).setBusy(true);
                        setMountObjectID(target.getObjectId());
                        broadcastPacket(new ChairSit(getObjectId(), ((L2StaticObjectInstance) target).getStaticObjectId()));
                    }
                }
            });

            // Binding next action to AI.
            getAI().setNextAction(nextAction);
            return;
        }

        // Player isn't moving, sit directly.
        if (!isMoving()) {
            if (_mountType != 0) { return; }

            if (sittingState) {
                if (_mountObjectID != 0) {
                    L2Object obj = L2World.getInstance().getObject(_mountObjectID);
                    ((L2StaticObjectInstance) obj).setBusy(false);

                    _mountObjectID = 0;
                }

                standUp();
            }
            else {
                sitDown();

                if (isThrone && !((L2StaticObjectInstance) target).isBusy() && isInsideRadius(target, L2Npc.INTERACTION_DISTANCE, false, false)) {
                    ((L2StaticObjectInstance) target).setBusy(true);
                    _mountObjectID = target.getObjectId();
                    broadcastPacket(new ChairSit(getObjectId(), ((L2StaticObjectInstance) target).getStaticObjectId()));
                }
            }
        }
        // Player is moving, wait the current action is done, then sit.
        else {
            NextAction nextAction = new NextAction(ECtrlEvent.EVT_ARRIVED, EIntention.MOVE_TO, new Runnable() {
                @Override
                public void run() {
                    if (getMountType() != 0) { return; }

                    if (sittingState) {
                        if (getMountObjectID() != 0) {
                            L2Object obj = L2World.getInstance().getObject(getMountObjectID());
                            ((L2StaticObjectInstance) obj).setBusy(false);

                            setMountObjectID(0);
                        }

                        standUp();
                    }
                    else {
                        sitDown();

                        if (isThrone && !((L2StaticObjectInstance) target).isBusy() && isInsideRadius(target, L2Npc.INTERACTION_DISTANCE, false, false)) {
                            ((L2StaticObjectInstance) target).setBusy(true);
                            setMountObjectID(target.getObjectId());
                            broadcastPacket(new ChairSit(getObjectId(), ((L2StaticObjectInstance) target).getStaticObjectId()));
                        }
                    }
                }
            });

            // Binding next action to AI.
            getAI().setNextAction(nextAction);
        }
    }

    /**
     * @return The PcWarehouse object of the L2PcInstance.
     */
    public PcWarehouse getWarehouse() {
        if (_warehouse == null) {
            _warehouse = new PcWarehouse(this);
            _warehouse.restore();
        }
        return _warehouse;
    }

    /**
     * Free memory used by Warehouse
     */
    public void clearWarehouse() {
        if (_warehouse != null) { _warehouse.deleteMe(); }

        _warehouse = null;
    }

    /**
     * @return The PcFreight object of the L2PcInstance.
     */
    public PcFreight getFreight() {
        if (_freight == null) {
            _freight = new PcFreight(this);
            _freight.restore();
        }
        return _freight;
    }

    /**
     * Free memory used by Freight
     */
    public void clearFreight() {
        if (_freight != null) { _freight.deleteMe(); }

        _freight = null;
    }

    /**
     * @param objectId The id of the owner.
     * @return deposited PcFreight object for the objectId or create new if not existing.
     */
    public PcFreight getDepositedFreight(int objectId) {
        for (PcFreight freight : _depositedFreight) {
            if (freight != null && freight.getOwnerId() == objectId) { return freight; }
        }

        PcFreight freight = new PcFreight(null);
        freight.doQuickRestore(objectId);
        _depositedFreight.add(freight);
        return freight;
    }

    /**
     * Clear memory used by deposited freight
     */
    public void clearDepositedFreight() {
        for (PcFreight freight : _depositedFreight) {
            if (freight != null) { freight.deleteMe(); }
        }
        _depositedFreight.clear();
    }

    /**
     * @return The Adena amount of the L2PcInstance.
     */
    public int getAdena() {
        return inventory.getAdena();
    }

    /**
     * @return The Ancient Adena amount of the L2PcInstance.
     */
    public int getAncientAdena() {
        return inventory.getAncientAdena();
    }

    /**
     * Add adena to Inventory of the L2PcInstance and send InventoryUpdate packet to the L2PcInstance.
     *
     * @param process     String Identifier of process triggering this action
     * @param count       int Quantity of adena to be added
     * @param reference   L2Object Object referencing current action like NPC selling item or previous item in transformation
     * @param sendMessage boolean Specifies whether to send message to Client about this action
     */
    public void addAdena(EItemProcessPurpose process, int count, L2Object reference, boolean sendMessage) {
        if (sendMessage) {
            sendPacket(SystemMessage.getSystemMessage(SystemMessageId.EARNED_S1_ADENA).addNumber(count));
        }

        if (count > 0) {
            inventory.addAdena(process, count, this, reference);

            InventoryUpdate iu = new InventoryUpdate();
            iu.addItem(inventory.getAdenaInstance());
            sendPacket(iu);
        }
    }

    /**
     * Reduce adena in Inventory of the L2PcInstance and send InventoryUpdate packet to the L2PcInstance.
     *
     * @param process     String Identifier of process triggering this action
     * @param count       int Quantity of adena to be reduced
     * @param reference   L2Object Object referencing current action like NPC selling item or previous item in transformation
     * @param sendMessage boolean Specifies whether to send message to Client about this action
     * @return boolean informing if the action was successfull
     */
    public boolean reduceAdena(EItemProcessPurpose process, int count, L2Object reference, boolean sendMessage) {
        if (count > getAdena()) {
            if (sendMessage) { sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA); }

            return false;
        }

        if (count > 0) {
            L2ItemInstance adenaItem = inventory.getAdenaInstance();
            if (!inventory.reduceAdena(process, count, this, reference)) { return false; }

            // Send update packet
            InventoryUpdate iu = new InventoryUpdate();
            iu.addItem(adenaItem);
            sendPacket(iu);

            if (sendMessage) {
                sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED_ADENA).addNumber(count));
            }
        }
        return true;
    }

    /**
     * Add ancient adena to Inventory of the L2PcInstance and send InventoryUpdate packet to the L2PcInstance.
     *
     * @param process     String Identifier of process triggering this action
     * @param count       int Quantity of ancient adena to be added
     * @param reference   L2Object Object referencing current action like NPC selling item or previous item in transformation
     * @param sendMessage boolean Specifies whether to send message to Client about this action
     */
    public void addAncientAdena(EItemProcessPurpose process, int count, L2Object reference, boolean sendMessage) {
        if (sendMessage) {
            sendPacket(SystemMessage.getSystemMessage(SystemMessageId.EARNED_S2_S1_S).addItemName(PcInventory.ANCIENT_ADENA_ID).addNumber(count));
        }

        if (count > 0) {
            inventory.addAncientAdena(process, count, this, reference);

            InventoryUpdate iu = new InventoryUpdate();
            iu.addItem(inventory.getAncientAdenaInstance());
            sendPacket(iu);
        }
    }

    /**
     * Reduce ancient adena in Inventory of the L2PcInstance and send InventoryUpdate packet to the L2PcInstance.
     *
     * @param process     String Identifier of process triggering this action
     * @param count       int Quantity of ancient adena to be reduced
     * @param reference   L2Object Object referencing current action like NPC selling item or previous item in transformation
     * @param sendMessage boolean Specifies whether to send message to Client about this action
     * @return boolean informing if the action was successfull
     */
    public boolean reduceAncientAdena(EItemProcessPurpose process, int count, L2Object reference, boolean sendMessage) {
        if (count > getAncientAdena()) {
            if (sendMessage) { sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA); }

            return false;
        }

        if (count > 0) {
            L2ItemInstance ancientAdenaItem = inventory.getAncientAdenaInstance();
            if (!inventory.reduceAncientAdena(process, count, this, reference)) { return false; }

            InventoryUpdate iu = new InventoryUpdate();
            iu.addItem(ancientAdenaItem);
            sendPacket(iu);

            if (sendMessage) {
                if (count > 1) {
                    sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S2_S1_DISAPPEARED).addItemName(PcInventory.ANCIENT_ADENA_ID).addItemNumber(count));
                }
                else {
                    sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED).addItemName(PcInventory.ANCIENT_ADENA_ID));
                }
            }
        }
        return true;
    }

    /**
     * Adds item to inventory and send InventoryUpdate packet to the L2PcInstance.
     *
     * @param process     String Identifier of process triggering this action
     * @param item        L2ItemInstance to be added
     * @param reference   L2Object Object referencing current action like NPC selling item or previous item in transformation
     * @param sendMessage boolean Specifies whether to send message to Client about this action
     */
    public void addItem(EItemProcessPurpose process, L2ItemInstance item, L2Object reference, boolean sendMessage) {
        if (item.getCount() > 0) {
            // Sends message to client if requested
            if (sendMessage) {
                if (item.getCount() > 1) {
                    sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_PICKED_UP_S2_S1).addItemName(item).addNumber(item.getCount()));
                }
                else if (item.getEnchantLevel() > 0) {
                    sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_PICKED_UP_A_S1_S2).addNumber(item.getEnchantLevel()).addItemName(item));
                }
                else { sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_PICKED_UP_S1).addItemName(item)); }
            }

            // Add the item to inventory
            L2ItemInstance newitem = inventory.addItem(process, item, this, reference);

            // Send inventory update packet
            InventoryUpdate playerIU = new InventoryUpdate();
            playerIU.addItem(newitem);
            sendPacket(playerIU);

            // Update current load as well
            StatusUpdate su = new StatusUpdate(this);
            su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
            sendPacket(su);

            // Cursed Weapon
            if (CursedWeaponsManager.getInstance().isCursed(newitem.getItemId())) {
                CursedWeaponsManager.getInstance().activate(this, newitem);
            }
            // If you pickup arrows and a bow is equipped, try to equip them if no arrows is currently equipped.
            else if (item.getItem().getItemType() == EtcItemType.ARROW && getAttackType() == EWeaponType.BOW && inventory.getPaperdollItem(EPaperdollSlot.PAPERDOLL_LHAND) == null) {
                checkAndEquipArrows();
            }
        }
    }

    /**
     * Adds item to Inventory and send InventoryUpdate packet to the L2PcInstance.
     *
     * @param process     String Identifier of process triggering this action
     * @param itemId      int Item Identifier of the item to be added
     * @param count       int Quantity of items to be added
     * @param reference   L2Object Object referencing current action like NPC selling item or previous item in transformation
     * @param sendMessage boolean Specifies whether to send message to Client about this action
     * @return The created L2ItemInstance.
     */
    public L2ItemInstance addItem(EItemProcessPurpose process, int itemId, int count, L2Object reference, boolean sendMessage) {
        if (count > 0) {
            // Retrieve the template of the item.
            Item item = ItemTable.getInstance().getTemplate(itemId);
            if (item == null) {
                _log.log(Level.SEVERE, "Item id " + itemId + "doesn't exist, so it can't be added.");
                return null;
            }

            // Sends message to client if requested.
            if (sendMessage && ((!isCastingNow() && item.getItemType() == EtcItemType.HERB) || item.getItemType() != EtcItemType.HERB)) {
                if (count > 1) {
                    if (process==EItemProcessPurpose.SWEEP || process == EItemProcessPurpose.QUEST) {
                        sendPacket(SystemMessage.getSystemMessage(SystemMessageId.EARNED_S2_S1_S).addItemName(itemId).addItemNumber(count));
                    }
                    else {
                        sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_PICKED_UP_S2_S1).addItemName(itemId).addItemNumber(count));
                    }
                }
                else {
                    if (process == EItemProcessPurpose.SWEEP || process == EItemProcessPurpose.QUEST) {
                        sendPacket(SystemMessage.getSystemMessage(SystemMessageId.EARNED_ITEM_S1).addItemName(itemId));
                    }
                    else {
                        sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_PICKED_UP_S1).addItemName(itemId));
                    }
                }
            }

            // If the item is herb type, dont add it to inventory.
            if (item.getItemType() == EtcItemType.HERB) {
                L2ItemInstance herb = new L2ItemInstance(0, itemId);

                IItemHandler handler = ItemHandler.getInstance().getItemHandler(herb.getEtcItem());
                if (handler != null) { handler.useItem(this, herb, false); }
            }
            else {
                // Add the item to inventory
                L2ItemInstance createdItem = inventory.addItem(process, itemId, count, this, reference);

                // Cursed Weapon
                if (CursedWeaponsManager.getInstance().isCursed(createdItem.getItemId())) {
                    CursedWeaponsManager.getInstance().activate(this, createdItem);
                }
                // If you pickup arrows and a bow is equipped, try to equip them if no arrows is currently equipped.
                else if (item.getItemType() == EtcItemType.ARROW && getAttackType() == EWeaponType.BOW && inventory.getPaperdollItem(EPaperdollSlot.PAPERDOLL_LHAND) == null) {
                    checkAndEquipArrows();
                }

                return createdItem;
            }
        }
        return null;
    }

    /**
     * Destroy item from inventory and send InventoryUpdate packet to the L2PcInstance.
     *
     * @param process     String Identifier of process triggering this action
     * @param item        L2ItemInstance to be destroyed
     * @param reference   L2Object Object referencing current action like NPC selling item or previous item in transformation
     * @param sendMessage boolean Specifies whether to send message to Client about this action
     * @return boolean informing if the action was successfull
     */
    public boolean destroyItem(EItemProcessPurpose process, L2ItemInstance item, L2Object reference, boolean sendMessage) {
        return destroyItem(process, item, item.getCount(), reference, sendMessage);
    }

    /**
     * Destroy item from inventory and send InventoryUpdate packet to the L2PcInstance.
     *
     * @param process     String Identifier of process triggering this action
     * @param item        L2ItemInstance to be destroyed
     * @param count       int Quantity of ancient adena to be reduced
     * @param reference   L2Object Object referencing current action like NPC selling item or previous item in transformation
     * @param sendMessage boolean Specifies whether to send message to Client about this action
     * @return boolean informing if the action was successfull
     */
    public boolean destroyItem(EItemProcessPurpose process, L2ItemInstance item, int count, L2Object reference, boolean sendMessage) {
        item = inventory.destroyItem(process, item, count, this, reference);

        if (item == null) {
            if (sendMessage) { sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS); }

            return false;
        }

        // Send inventory update packet
        InventoryUpdate playerIU = new InventoryUpdate();
        playerIU.addItem(item);
        sendPacket(playerIU);

        // Update current load as well
        StatusUpdate su = new StatusUpdate(this);
        su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
        sendPacket(su);

        // Sends message to client if requested
        if (sendMessage) {
            if (count > 1) {
                sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S2_S1_DISAPPEARED).addItemName(item).addItemNumber(count));
            }
            else { sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED).addItemName(item)); }
        }
        return true;
    }

    /**
     * Destroys item from inventory and send InventoryUpdate packet to the L2PcInstance.
     *
     * @param process     String Identifier of process triggering this action
     * @param objectId    int Item Instance identifier of the item to be destroyed
     * @param count       int Quantity of items to be destroyed
     * @param reference   L2Object Object referencing current action like NPC selling item or previous item in transformation
     * @param sendMessage boolean Specifies whether to send message to Client about this action
     * @return boolean informing if the action was successfull
     */
    @Override
    public boolean destroyItem(EItemProcessPurpose process, int objectId, int count, L2Object reference, boolean sendMessage) {
        L2ItemInstance item = inventory.getItemByObjectId(objectId);

        if (item == null) {
            if (sendMessage) { sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS); }

            return false;
        }
        return destroyItem(process, item, count, reference, sendMessage);
    }

    /**
     * Destroys shots from inventory without logging and only occasional saving to database. Sends InventoryUpdate packet to the L2PcInstance.
     *
     * @param process     String Identifier of process triggering this action
     * @param objectId    int Item Instance identifier of the item to be destroyed
     * @param count       int Quantity of items to be destroyed
     * @param reference   L2Object Object referencing current action like NPC selling item or previous item in transformation
     * @param sendMessage boolean Specifies whether to send message to Client about this action
     * @return boolean informing if the action was successfull
     */
    public boolean destroyItemWithoutTrace(EItemProcessPurpose process, int objectId, int count, L2Object reference, boolean sendMessage) {
        L2ItemInstance item = inventory.getItemByObjectId(objectId);

        if (item == null || item.getCount() < count) {
            if (sendMessage) { sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS); }

            return false;
        }

        return destroyItem(null, item, count, reference, sendMessage);
    }

    /**
     * Destroy item from inventory by using its <B>itemId</B> and send InventoryUpdate packet to the L2PcInstance.
     *
     * @param process     String Identifier of process triggering this action
     * @param itemId      int Item identifier of the item to be destroyed
     * @param count       int Quantity of items to be destroyed
     * @param reference   L2Object Object referencing current action like NPC selling item or previous item in transformation
     * @param sendMessage boolean Specifies whether to send message to Client about this action
     * @return boolean informing if the action was successfull
     */
    @Override
    public boolean destroyItemByItemId(EItemProcessPurpose process, int itemId, int count, L2Object reference, boolean sendMessage) {
        if (itemId == 57) { return reduceAdena(process, count, reference, sendMessage); }

        L2ItemInstance item = inventory.getItemByItemId(itemId);

        if (item == null || item.getCount() < count || inventory.destroyItemByItemId(process, itemId, count, this, reference) == null) {
            if (sendMessage) { sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS); }

            return false;
        }

        // Send inventory update packet
        InventoryUpdate playerIU = new InventoryUpdate();
        playerIU.addItem(item);
        sendPacket(playerIU);

        // Update current load as well
        StatusUpdate su = new StatusUpdate(this);
        su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
        sendPacket(su);

        // Sends message to client if requested
        if (sendMessage) {
            if (count > 1) {
                sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S2_S1_DISAPPEARED).addItemName(itemId).addItemNumber(count));
            }
            else { sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED).addItemName(itemId)); }
        }
        return true;
    }

    /**
     * Transfers item to another ItemContainer and send InventoryUpdate packet to the L2PcInstance.
     *
     * @param process   String Identifier of process triggering this action
     * @param objectId  int Item Identifier of the item to be transfered
     * @param count     int Quantity of items to be transfered
     * @param target    Inventory the Inventory target.
     * @param reference L2Object Object referencing current action like NPC selling item or previous item in transformation
     * @return L2ItemInstance corresponding to the new item or the updated item in inventory
     */
    public L2ItemInstance transferItem(EItemProcessPurpose process, int objectId, int count, Inventory target, L2Object reference) {
        L2ItemInstance oldItem = checkItemManipulation(objectId, count);
        if (oldItem == null) { return null; }

        L2ItemInstance newItem = inventory.transferItem(process, objectId, count, target, this, reference);
        if (newItem == null) { return null; }

        // Send inventory update packet
        InventoryUpdate playerIU = new InventoryUpdate();

        if (oldItem.getCount() > 0 && oldItem != newItem) { playerIU.addModifiedItem(oldItem); }
        else { playerIU.addRemovedItem(oldItem); }

        sendPacket(playerIU);

        // Update current load as well
        StatusUpdate playerSU = new StatusUpdate(this);
        playerSU.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
        sendPacket(playerSU);

        // Send target update packet
        if (target instanceof PcInventory) {
            L2PcInstance targetPlayer = ((PcInventory) target).getOwner();

            InventoryUpdate playerIU2 = new InventoryUpdate();
            if (newItem.getCount() > count) { playerIU2.addModifiedItem(newItem); }
            else { playerIU2.addNewItem(newItem); }
            targetPlayer.sendPacket(playerIU2);

            // Update current load as well
            playerSU = new StatusUpdate(targetPlayer);
            playerSU.addAttribute(StatusUpdate.CUR_LOAD, targetPlayer.getCurrentLoad());
            targetPlayer.sendPacket(playerSU);
        }
        else if (target instanceof PetInventory) {
            PetInventoryUpdate petIU = new PetInventoryUpdate();
            if (newItem.getCount() > count) { petIU.addModifiedItem(newItem); }
            else { petIU.addNewItem(newItem); }
            ((PetInventory) target).getOwner().getOwner().sendPacket(petIU);
        }
        return newItem;
    }

    /**
     * Drop item from inventory and send InventoryUpdate packet to the L2PcInstance.
     *
     * @param process     String Identifier of process triggering this action
     * @param item        L2ItemInstance to be dropped
     * @param reference   L2Object Object referencing current action like NPC selling item or previous item in transformation
     * @param sendMessage boolean Specifies whether to send message to Client about this action
     * @param protectItem whether or not dropped item must be protected temporary against other players
     * @return boolean informing if the action was successfull
     */
    public boolean dropItem(EItemProcessPurpose process, L2ItemInstance item, L2Object reference, boolean sendMessage, boolean protectItem) {
        item = inventory.dropItem(process, item, this, reference);

        if (item == null) {
            if (sendMessage) { sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS); }

            return false;
        }

        item.dropMe(this, getX() + Rnd.get(50) - 25, getY() + Rnd.get(50) - 25, getZ() + 20);

        // retail drop protection
        if (protectItem) { item.getDropProtection().protect(this); }

        // Send inventory update packet
        InventoryUpdate playerIU = new InventoryUpdate();
        playerIU.addItem(item);
        sendPacket(playerIU);

        // Update current load as well
        StatusUpdate su = new StatusUpdate(this);
        su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
        sendPacket(su);

        // Sends message to client if requested
        if (sendMessage) {
            sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_DROPPED_S1).addItemName(item));
        }

        return true;
    }

    public boolean dropItem(EItemProcessPurpose process, L2ItemInstance item, L2Object reference, boolean sendMessage) {
        return dropItem(process, item, reference, sendMessage, false);
    }

    /**
     * Drop item from inventory by using its <B>objectID</B> and send InventoryUpdate packet to the L2PcInstance.
     *
     * @param process     String Identifier of process triggering this action
     * @param objectId    int Item Instance identifier of the item to be dropped
     * @param count       int Quantity of items to be dropped
     * @param x           int coordinate for drop X
     * @param y           int coordinate for drop Y
     * @param z           int coordinate for drop Z
     * @param reference   L2Object Object referencing current action like NPC selling item or previous item in transformation
     * @param sendMessage boolean Specifies whether to send message to Client about this action
     * @param protectItem boolean Activates drop protection on that item if true
     * @return L2ItemInstance corresponding to the new item or the updated item in inventory
     */
    public L2ItemInstance dropItem(EItemProcessPurpose process, int objectId, int count, int x, int y, int z, L2Object reference, boolean sendMessage, boolean protectItem) {
        L2ItemInstance invitem = inventory.getItemByObjectId(objectId);
        L2ItemInstance item = inventory.dropItem(process, objectId, count, this, reference);

        if (item == null) {
            if (sendMessage) { sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS); }

            return null;
        }

        item.dropMe(this, x, y, z);

        // retail drop protection
        if (protectItem) { item.getDropProtection().protect(this); }

        // Send inventory update packet
        InventoryUpdate playerIU = new InventoryUpdate();
        playerIU.addItem(invitem);
        sendPacket(playerIU);

        // Update current load as well
        StatusUpdate su = new StatusUpdate(this);
        su.addAttribute(StatusUpdate.CUR_LOAD, getCurrentLoad());
        sendPacket(su);

        // Sends message to client if requested
        if (sendMessage) {
            sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_DROPPED_S1).addItemName(item));
        }

        return item;
    }

    public L2ItemInstance checkItemManipulation(int objectId, int count) {
        if (L2World.getInstance().getObject(objectId) == null) { return null; }

        L2ItemInstance item = inventory.getItemByObjectId(objectId);

        if (item == null || item.getOwnerId() != getObjectId()) { return null; }

        if (count < 1 || (count > 1 && !item.isStackable())) { return null; }

        if (count > item.getCount()) { return null; }

        // Pet is summoned and not the item that summoned the pet AND not the buggle from strider you're mounting
        if (summon != null && summon.getControlItemId() == objectId || _mountObjectID == objectId) {
            return null;
        }

        if (_activeEnchantItem != null && _activeEnchantItem.getObjectId() == objectId) { return null; }

        // We cannot put a Weapon with Augmention in WH while casting (Possible Exploit)
        if (item.isAugmented() && (isCastingNow() || isCastingSimultaneouslyNow())) { return null; }

        return item;
    }

    /**
     * Launch a task corresponding to Config time.
     *
     * @param protect boolean Drop timer or activate it.
     */
    public void setProtection(boolean protect) {
        if (protect) {
            if (_protectTask == null) {
                _protectTask = ThreadPoolManager.getInstance().scheduleGeneral(new ProtectTask(), Config.PLAYER_SPAWN_PROTECTION * 1000);
            }
        }
        else {
            _protectTask.cancel(true);
            _protectTask = null;
        }
        broadcastUserInfo();
    }

    public boolean isSpawnProtected() {
        return _protectTask != null;
    }

    /**
     * Set protection from agro mobs when getting up from fake death, according settings.
     */
    public void setRecentFakeDeath() {
        _recentFakeDeathEndTime = System.currentTimeMillis() + Config.PLAYER_FAKEDEATH_UP_PROTECTION * 1000;
    }

    public void clearRecentFakeDeath() {
        _recentFakeDeathEndTime = 0;
    }

    public boolean isRecentFakeDeath() {
        return _recentFakeDeathEndTime > System.currentTimeMillis();
    }

    public final boolean isFakeDeath() {
        return _isFakeDeath;
    }

    public final void setIsFakeDeath(boolean value) {
        _isFakeDeath = value;
    }

    @Override
    public final boolean isAlikeDead() {
        if (super.isAlikeDead()) { return true; }

        return _isFakeDeath;
    }

    /**
     * @return The client owner of this char.
     */
    public L2GameClient getClient() {
        return _client;
    }

    public void setClient(L2GameClient client) {
        _client = client;
    }

    /**
     * Close the active connection with the client.
     *
     * @param closeClient
     */
    private void closeNetConnection(boolean closeClient) {
        L2GameClient client = _client;
        if (client != null) {
            if (client.isDetached()) { client.cleanMe(true); }
            else {
                if (!client.getConnection().isClosed()) {
                    if (closeClient) { client.close(LeaveWorld.STATIC_PACKET); }
                    else { client.close(ServerClose.STATIC_PACKET); }
                }
            }
        }
    }

    public Location getCurrentSkillWorldPosition() {
        return _currentSkillWorldPosition;
    }

    public void setCurrentSkillWorldPosition(Location worldPosition) {
        _currentSkillWorldPosition = worldPosition;
    }

    /**
     * @see L2Character#enableSkill(L2Skill)
     */
    @Override
    public void enableSkill(L2Skill skill) {
        super.enableSkill(skill);
        _reuseTimeStamps.remove(skill.getReuseHashCode());
    }

    /**
     * @see L2Character#checkDoCastConditions(L2Skill)
     */
    @Override
    protected boolean checkDoCastConditions(L2Skill skill) {
        if (!super.checkDoCastConditions(skill)) { return false; }

        if (skill.getSkillType() == L2SkillType.SUMMON) {
            if (!((L2SkillSummon) skill).isCubic() && (summon != null || isMounted())) {
                sendPacket(SystemMessageId.SUMMON_ONLY_ONE);
                return false;
            }
        }

        // Can't use Hero and resurrect skills during Olympiad
        if (_inOlympiadMode && (skill.isHeroSkill() || skill.getSkillType() == L2SkillType.RESURRECT)) {
            sendPacket(SystemMessage.getSystemMessage(SystemMessageId.THIS_SKILL_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT));
            return false;
        }

        // Check if the spell uses charges
        int charges = getCharges();
        if (skill.getMaxCharges() == 0 && charges < skill.getNumCharges()) {
            sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addSkillName(skill));
            return false;
        }

        return true;
    }

    /**
     * Manage actions when a player click on this L2PcInstance.<BR>
     * <BR>
     * <B><U> Actions on first click on the L2PcInstance (Select it)</U> :</B>
     * <ul>
     * <li>Set the target of the player</li>
     * <li>Send MyTargetSelected to the player (display the select window)</li>
     * </ul>
     * <B><U> Actions on second click on the L2PcInstance (Follow it/Attack it/Intercat with it)</U> :</B>
     * <ul>
     * <li>Send MyTargetSelected to the player (display the select window)</li>
     * <li>If this L2PcInstance has a Private Store, notify the player AI with INTERACT</li>
     * <li>If this L2PcInstance is autoAttackable, notify the player AI with ATTACK</li>
     * <li>If this L2PcInstance is NOT autoAttackable, notify the player AI with FOLLOW</li>
     * </ul>
     *
     * @param player The player that start an action on this L2PcInstance
     */
    @Override
    public void onAction(L2PcInstance player) {
        // Set the target of the player
        if (player.getTarget() != this) { player.setTarget(this); }
        else {
            // Check if this L2PcInstance has a Private Store
            if (isInStoreMode()) {
                player.getAI().setIntention(EIntention.INTERACT, this);
                return;
            }

            // Check if this L2PcInstance is autoAttackable
            if (isAutoAttackable(player)) {
                // Player with lvl < 21 can't attack a cursed weapon holder and a cursed weapon holder can't attack players with lvl < 21
                if ((isCursedWeaponEquipped() && player.getLevel() < 21) || (player.isCursedWeaponEquipped() && getLevel() < 21)) {
                    player.sendPacket(ActionFailed.STATIC_PACKET);
                    return;
                }

                if (PathFinding.getInstance().canSeeTarget(player, this)) {
                    player.getAI().setIntention(EIntention.ATTACK, this);
                    player.onActionRequest();
                }
            }
            else {
                // avoids to stuck when clicking two or more times
                player.sendPacket(ActionFailed.STATIC_PACKET);

                if (player != this && PathFinding.getInstance().canSeeTarget(player, this)) {
                    player.getAI().setIntention(EIntention.FOLLOW, this);
                }
            }
        }
    }

    @Override
    public void onActionShift(L2PcInstance player) {
        if (player.isGM()) { AdminEditChar.showCharacterInfo(player, this); }

        super.onActionShift(player);
    }

    /**
     * @param barPixels
     * @return true if cp update should be done, false if not
     */
    private boolean needCpUpdate(int barPixels) {
        double currentCp = getCurrentCp();

        if (currentCp <= 1.0 || getMaxCp() < barPixels) { return true; }

        if (currentCp <= _cpUpdateDecCheck || currentCp >= _cpUpdateIncCheck) {
            if (currentCp == getMaxCp()) {
                _cpUpdateIncCheck = currentCp + 1;
                _cpUpdateDecCheck = currentCp - _cpUpdateInterval;
            }
            else {
                double doubleMulti = currentCp / _cpUpdateInterval;
                int intMulti = (int) doubleMulti;

                _cpUpdateDecCheck = _cpUpdateInterval * (doubleMulti < intMulti ? intMulti-- : intMulti);
                _cpUpdateIncCheck = _cpUpdateDecCheck + _cpUpdateInterval;
            }

            return true;
        }

        return false;
    }

    /**
     * @param barPixels
     * @return true if mp update should be done, false if not
     */
    private boolean needMpUpdate(int barPixels) {
        double currentMp = getCurrentMp();

        if (currentMp <= 1.0 || getMaxMp() < barPixels) { return true; }

        if (currentMp <= _mpUpdateDecCheck || currentMp >= _mpUpdateIncCheck) {
            if (currentMp == getMaxMp()) {
                _mpUpdateIncCheck = currentMp + 1;
                _mpUpdateDecCheck = currentMp - _mpUpdateInterval;
            }
            else {
                double doubleMulti = currentMp / _mpUpdateInterval;
                int intMulti = (int) doubleMulti;

                _mpUpdateDecCheck = _mpUpdateInterval * (doubleMulti < intMulti ? intMulti-- : intMulti);
                _mpUpdateIncCheck = _mpUpdateDecCheck + _mpUpdateInterval;
            }

            return true;
        }

        return false;
    }

    /**
     * Send packet StatusUpdate with current HP,MP and CP to the L2PcInstance and only current HP, MP and Level to all other L2PcInstance of the Party.
     * <ul>
     * <li>Send StatusUpdate with current HP, MP and CP to this L2PcInstance</li>
     * <li>Send PartySmallWindowUpdate with current HP, MP and Level to all other L2PcInstance of the Party</li>
     * </ul>
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T SEND current HP and MP to all L2PcInstance of the _statusListener</B></FONT>
     */
    @Override
    public void broadcastStatusUpdate() {
        // Send StatusUpdate with current HP, MP and CP to this L2PcInstance
        StatusUpdate su = new StatusUpdate(this);
        su.addAttribute(StatusUpdate.CUR_HP, (int) getCurrentHp());
        su.addAttribute(StatusUpdate.CUR_MP, (int) getCurrentMp());
        su.addAttribute(StatusUpdate.CUR_CP, (int) getCurrentCp());
        su.addAttribute(StatusUpdate.MAX_CP, getMaxCp());
        sendPacket(su);

        boolean needCpUpdate = needCpUpdate(352);
        boolean needHpUpdate = needHpUpdate(352);

        // Check if a party is in progress and party window update is needed.
        if (_party != null && (needCpUpdate || needHpUpdate || needMpUpdate(352))) {
            _party.broadcastToPartyMembers(this, new PartySmallWindowUpdate(this));
        }

        if (_inOlympiadMode && _OlympiadStart && (needCpUpdate || needHpUpdate)) {
            OlympiadGameTask game = OlympiadGameManager.getInstance().getOlympiadTask(_olympiadGameId);
            if (game != null && game.isBattleStarted()) { game.getZone().broadcastStatusUpdate(this); }
        }

        // In duel, MP updated only with CP or HP
        if (_isInDuel && (needCpUpdate || needHpUpdate)) {
            ExDuelUpdateUserInfo update = new ExDuelUpdateUserInfo(this);
            DuelManager.getInstance().broadcastToOppositeTeam(this, update);
        }
    }

    /**
     * Broadcast informations from a user to himself and his knownlist.<BR>
     * If player is morphed, it sends informations from the template the player is using.
     * <ul>
     * <li>Send a UserInfo packet (public and private data) to this L2PcInstance.</li>
     * <li>Send a CharInfo packet (public data only) to L2PcInstance's knownlist.</li>
     * </ul>
     */
    public final void broadcastUserInfo() {
        sendPacket(new UserInfo(this));

        if (getPoly().isMorphed()) {
            Broadcast.toKnownPlayers(this, new PcMorphInfo(this, getPoly().getNpcTemplate()));
        }
        else { broadcastCharInfo(); }
    }

    public final void broadcastCharInfo() {
        for (L2PcInstance player : getKnownList().getKnownType(L2PcInstance.class)) {
            player.sendPacket(new CharInfo(this));

            int relation = getRelationTo(player);
            player.sendPacket(new RelationChanged(this, relation, isAutoAttackable(player)));
            if (summon != null) {
                player.sendPacket(new RelationChanged(summon, relation, isAutoAttackable(player)));
            }
        }
    }

    /**
     * Broadcast player title information.
     */
    public final void broadcastTitleInfo() {
        sendPacket(new UserInfo(this));
        broadcastPacket(new TitleUpdate(this));
    }

    /**
     * @return the Alliance Identifier of the L2PcInstance.
     */
    public int getAllyId() {
        if (clan == null) { return 0; }

        return clan.getAllyId();
    }

    public int getAllyCrestId() {
        if (_clanId == 0) { return 0; }

        if (clan.getAllyId() == 0) { return 0; }

        return clan.getAllyCrestId();
    }

    /**
     * Send a packet to the L2PcInstance.
     */
    @Override
    public void sendPacket(L2GameServerPacket packet) {
        if (_client != null) { _client.sendPacket(packet); }
    }

    /**
     * Send SystemMessage packet.
     *
     * @param id SystemMessageId
     */
    @Override
    public void sendPacket(SystemMessageId id) {
        sendPacket(SystemMessage.getSystemMessage(id));
    }

    /**
     * Manage Interact Task with another L2PcInstance.<BR>
     * Turn the character in front of the target.<BR>
     * In case of private stores, send the related packet.
     *
     * @param target The L2Character targeted
     */
    public void doInteract(L2Character target) {
        if (target instanceof L2PcInstance) {
            L2PcInstance temp = (L2PcInstance) target;
            sendPacket(new MoveToPawn(this, temp, L2Npc.INTERACTION_DISTANCE));

            switch (temp._privateStoreType) {
                case SELL:
                case PACKAGE_SELL:
                    sendPacket(new PrivateStoreListSell(this, temp));
                    break;

                case BUY:
                    sendPacket(new PrivateStoreListBuy(this, temp));
                    break;

                case MANUFACTURE:
                    sendPacket(new RecipeShopSellList(this, temp));
                    break;
            }
        }
        else {
            // _interactTarget=null should never happen but one never knows ^^;
            if (target != null) { target.onAction(this); }
        }
    }

    public void doAutoLoot(L2Attackable target, IntIntHolder item) {
        if (isInParty()) {
            _party.distributeItem(this, item, false, target);
        }
        else if (item.getId() == 57) { addAdena(EItemProcessPurpose.LOOT, item.getValue(), target, true); }
        else { addItem(EItemProcessPurpose.LOOT, item.getId(), item.getValue(), target, true); }
    }

    @Override
    public void doPickupItem(L2Object object) {
        if (isAlikeDead() || _isFakeDeath) { return; }

        // Set the AI Intention to IDLE
        getAI().setIntention(EIntention.IDLE);

        // Check if the L2Object to pick up is a L2ItemInstance
        if (!(object instanceof L2ItemInstance)) {
            // dont try to pickup anything that is not an item :)
            _log.warning(getName() + " tried to pickup a wrong target: " + object);
            return;
        }

        L2ItemInstance target = (L2ItemInstance) object;

        // Send ActionFailed to this L2PcInstance
        sendPacket(ActionFailed.STATIC_PACKET);
        sendPacket(new StopMove(this));

        synchronized (target) {
            if (!target.isVisible()) { return; }

            if (isInStoreMode()) { return; }

            if (!target.getDropProtection().tryPickUp(this)) {
                sendPacket(SystemMessage.getSystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1).addItemName(target.getItemId()));
                return;
            }

            if (!inventory.validateWeight(target.getCount() * target.getItem().getWeight())) {
                sendPacket(SystemMessage.getSystemMessage(SystemMessageId.WEIGHT_LIMIT_EXCEEDED));
                return;
            }

            if (((isInParty() && _party.getLootDistribution() == L2Party.ITEM_LOOTER) || !isInParty()) && !inventory.validateCapacity(target)) {
                sendPacket(SystemMessageId.SLOTS_FULL);
                return;
            }

            if (_activeTradeList != null) {
                sendPacket(SystemMessageId.CANNOT_PICKUP_OR_USE_ITEM_WHILE_TRADING);
                return;
            }

            if (target.getOwnerId() != 0 && target.getOwnerId() != getObjectId() && !isInLooterParty(target.getOwnerId())) {
                if (target.getItemId() == 57) {
                    sendPacket(SystemMessage.getSystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1_ADENA).addNumber(target.getCount()));
                }
                else if (target.getCount() > 1) {
                    sendPacket(SystemMessage.getSystemMessage(SystemMessageId.FAILED_TO_PICKUP_S2_S1_S).addItemName(target).addNumber(target.getCount()));
                }
                else {
                    sendPacket(SystemMessage.getSystemMessage(SystemMessageId.FAILED_TO_PICKUP_S1).addItemName(target));
                }

                return;
            }

            if (target.getItemLootShedule() != null && (target.getOwnerId() == getObjectId() || isInLooterParty(target.getOwnerId()))) {
                target.resetOwnerTimer();
            }

            // Remove the L2ItemInstance from the world and send GetItem packets
            target.pickupMe(this);

            // item must be removed from ItemsOnGroundManager if is active
            ItemsOnGroundTaskManager.getInstance().remove(target);
        }

        // Auto use herbs - pick up
        if (target.getItemType() == EtcItemType.HERB) {
            IItemHandler handler = ItemHandler.getInstance().getItemHandler(target.getEtcItem());
            if (handler != null) { handler.useItem(this, target, false); }

            ItemTable.getInstance().destroyItem(EItemProcessPurpose.CONSUME, target, this, null);
        }
        // Cursed Weapons are not distributed
        else if (CursedWeaponsManager.getInstance().isCursed(target.getItemId())) {
            addItem(EItemProcessPurpose.PICKUP, target, null, true);
        }
        else {
            // if item is instance of L2ArmorType or WeaponType broadcast an "Attention" system message
            if (target.getItemType() instanceof ArmorType || target.getItemType() instanceof EWeaponType) {
                if (target.getEnchantLevel() > 0) {
                    SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.ATTENTION_S1_PICKED_UP_S2_S3);
                    msg.addString(getName());
                    msg.addNumber(target.getEnchantLevel());
                    msg.addItemName(target.getItemId());
                    broadcastPacket(msg, 1400);
                }
                else {
                    SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.ATTENTION_S1_PICKED_UP_S2);
                    msg.addString(getName());
                    msg.addItemName(target.getItemId());
                    broadcastPacket(msg, 1400);
                }
            }

            // Check if a Party is in progress
            if (isInParty()) {
                _party.distributeItem(this, target);
            }
            // Target is adena
            else if (target.getItemId() == 57 && inventory.getAdenaInstance() != null) {
                addAdena(EItemProcessPurpose.PICKUP, target.getCount(), null, true);
                ItemTable.getInstance().destroyItem(EItemProcessPurpose.PICKUP, target, this, null);
            }
            // Target is regular item
            else { addItem(EItemProcessPurpose.PICKUP, target, null, true); }
        }

        // Schedule a paralyzed task to wait for the animation to finish
        ThreadPoolManager.getInstance().scheduleGeneral(new Runnable() {
            @Override
            public void run() {
                setIsParalyzed(false);
            }
        }, 250);
        setIsParalyzed(true);
    }

    @Override
    public void doAttack(L2Character target) {
        super.doAttack(target);
        getActingPlayer().clearRecentFakeDeath();
    }

    @Override
    public void doCast(L2Skill skill) {
        super.doCast(skill);
        getActingPlayer().clearRecentFakeDeath();
    }

    public boolean canOpenPrivateStore() {
        if (_activeTradeList != null) { cancelActiveTrade(); }

        return !isAlikeDead() && !_inOlympiadMode && !isMounted() && !isInsideZone(ZoneId.NO_STORE) && !isCastingNow();
    }

    public void tryOpenPrivateBuyStore() {
        if (canOpenPrivateStore()) {
            if (_privateStoreType == PrivateStoreType.BUY || _privateStoreType == PrivateStoreType.BUY_MANAGE) {
                _privateStoreType = PrivateStoreType.NONE;
            }

            if (_privateStoreType == PrivateStoreType.NONE) {
                standUp();

                _privateStoreType = PrivateStoreType.BUY_MANAGE;
                sendPacket(new PrivateStoreManageListBuy(this));
            }
        }
        else {
            if (isInsideZone(ZoneId.NO_STORE)) { sendPacket(SystemMessageId.NO_PRIVATE_STORE_HERE); }

            sendPacket(ActionFailed.STATIC_PACKET);
        }
    }

    public void tryOpenPrivateSellStore(boolean isPackageSale) {
        if (canOpenPrivateStore()) {
            if (_privateStoreType == PrivateStoreType.SELL || _privateStoreType == PrivateStoreType.SELL_MANAGE || _privateStoreType == PrivateStoreType.PACKAGE_SELL) {
                _privateStoreType = PrivateStoreType.NONE;
            }

            if (_privateStoreType == PrivateStoreType.NONE) {
                standUp();

                _privateStoreType = PrivateStoreType.SELL_MANAGE;
                sendPacket(new PrivateStoreManageListSell(this, isPackageSale));
            }
        }
        else {
            if (isInsideZone(ZoneId.NO_STORE)) { sendPacket(SystemMessageId.NO_PRIVATE_STORE_HERE); }

            sendPacket(ActionFailed.STATIC_PACKET);
        }
    }

    public void tryOpenWorkshop(boolean isDwarven) {
        if (canOpenPrivateStore()) {
            if (isInStoreMode()) {
                _privateStoreType = PrivateStoreType.NONE;
            }

            if (_privateStoreType == PrivateStoreType.NONE) {
                standUp();

                if (_createList == null) {
                    _createList = new L2ManufactureList();
                }

                sendPacket(new RecipeShopManageList(this, isDwarven));
            }
        }
        else {
            if (isInsideZone(ZoneId.NO_STORE)) { sendPacket(SystemMessageId.NO_PRIVATE_WORKSHOP_HERE); }

            sendPacket(ActionFailed.STATIC_PACKET);
        }
    }

    /**
     * Set a target.
     * <ul>
     * <li>Remove the L2PcInstance from the _statusListener of the old target if it was a L2Character</li>
     * <li>Add the L2PcInstance to the _statusListener of the new target if it's a L2Character</li>
     * <li>Target the new L2Object (add the target to the L2PcInstance _target, _knownObject and L2PcInstance to _KnownObject of the L2Object)</li>
     * </ul>
     *
     * @param newTarget The L2Object to target
     */
    @Override
    public void setTarget(L2Object newTarget) {
        if (newTarget != null) {
            boolean isParty = (newTarget instanceof L2PcInstance) && isInParty() && _party.getPartyMembers().contains(newTarget);

            // Check if the new target is visible
            if (!isParty && (!newTarget.isVisible() || Math.abs(newTarget.getZ() - getZ()) > 1000)) {
                newTarget = null;
            }
        }

        // Can't target and attack festival monsters if not participant
        if ((newTarget instanceof L2FestivalMonsterInstance) && !isFestivalParticipant()) { newTarget = null; }
        // Can't target and attack rift invaders if not in the same room
        else if (isInParty() && _party.isInDimensionalRift()) {
            byte riftType = _party.getDimensionalRift().getType();
            byte riftRoom = _party.getDimensionalRift().getCurrentRoom();

            if (newTarget != null && !DimensionalRiftManager.getInstance().getRoom(riftType, riftRoom).checkIfInZone(newTarget.getX(), newTarget.getY(), newTarget.getZ())) {
                newTarget = null;
            }
        }

        // Get the current target
        L2Object oldTarget = getTarget();

        if (oldTarget != null) {
            if (oldTarget.equals(newTarget)) {
                return; // no target change
            }

            // Remove the L2PcInstance from the _statusListener of the old target if it was a L2Character
            if (oldTarget instanceof L2Character) { ((L2Character) oldTarget).removeStatusListener(this); }
        }

        // Verify if it's a static object.
        if (newTarget instanceof L2StaticObjectInstance) {
            sendPacket(new MyTargetSelected(newTarget.getObjectId(), getLevel()));
            sendPacket(new StaticObject((L2StaticObjectInstance) newTarget));
        }
        // Add the L2PcInstance to the _statusListener of the new target if it's a L2Character
        else if (newTarget instanceof L2Character) {
            L2Character target = (L2Character) newTarget;

            target.addStatusListener(this);

            // Show the client his new target.
            if (target.isAutoAttackable(this)) {
                // Show the client his new target.
                sendPacket(new MyTargetSelected(target.getObjectId(), getLevel() - target.getLevel()));

                // Send max/current hp.
                StatusUpdate su = new StatusUpdate(target);
                su.addAttribute(StatusUpdate.MAX_HP, target.getMaxHp());
                su.addAttribute(StatusUpdate.CUR_HP, (int) target.getCurrentHp());
                sendPacket(su);
            }
            else { sendPacket(new MyTargetSelected(target.getObjectId(), 0)); }

            Broadcast.toKnownPlayers(this, new TargetSelected(getObjectId(), newTarget.getObjectId(), getX(), getY(), getZ()));
        }

        if (newTarget == null && getTarget() != null) {
            broadcastPacket(new TargetUnselected(this));
            _currentFolkNpc = null;
        }
        else {
            // Rehabilitates that useful check.
            if (newTarget instanceof L2NpcInstance) {
                _currentFolkNpc = (L2Npc) newTarget;
            }
        }

        // Target the new L2Object (add the target to the L2PcInstance _target, _knownObject and L2PcInstance to _KnownObject of the L2Object)
        super.setTarget(newTarget);
    }

    /**
     * Return the active weapon instance (always equipped in the right hand).
     */
    @Override
    public L2ItemInstance getActiveWeaponInstance() {
        return inventory.getPaperdollItem(EPaperdollSlot.PAPERDOLL_RHAND);
    }

    /**
     * Return the active weapon item (always equipped in the right hand).
     */
    @Override
    public Weapon getActiveWeaponItem() {
        L2ItemInstance weapon = getActiveWeaponInstance();

        if (weapon == null) {
            return _fistsWeaponItem;
        }

        return (Weapon) weapon.getItem();
    }

    public L2ItemInstance getChestArmorInstance() {
        return inventory.getPaperdollItem(EPaperdollSlot.PAPERDOLL_CHEST);
    }

    public Armor getActiveChestArmorItem() {
        L2ItemInstance armor = getChestArmorInstance();

        if (armor == null) { return null; }

        return (Armor) armor.getItem();
    }

    public boolean isWearingHeavyArmor() {
        L2ItemInstance armor = getChestArmorInstance();

        return armor.getItemType() == ArmorType.HEAVY;

    }

    public boolean isWearingLightArmor() {
        L2ItemInstance armor = getChestArmorInstance();

        return armor.getItemType() == ArmorType.LIGHT;

    }

    public boolean isWearingMagicArmor() {
        L2ItemInstance armor = getChestArmorInstance();

        return armor.getItemType() == ArmorType.MAGIC;

    }

    public boolean isMarried() {
        return _married;
    }

    public void setMarried(boolean state) {
        _married = state;
    }

    public boolean isUnderMarryRequest() {
        return _marryrequest;
    }

    public void setUnderMarryRequest(boolean state) {
        _marryrequest = state;
    }

    public int getCoupleId() {
        return _coupleId;
    }

    public void setCoupleId(int coupleId) {
        _coupleId = coupleId;
    }

    public void setRequesterId(int requesterId) {
        _requesterId = requesterId;
    }

    public void EngageAnswer(int answer) {
        if (!_marryrequest || _requesterId == 0) { return; }

        L2PcInstance ptarget = L2World.getInstance().getPlayer(_requesterId);
        if (ptarget != null) {
            if (answer == 1) {
                // Create the couple
                CoupleManager.getInstance().createCouple(ptarget, this);

                // Then "finish the job"
                L2WeddingManagerInstance.justMarried(ptarget, this);
            }
            else {
                _marryrequest = false;
                sendMessage("You declined your partner's marriage request.");

                ptarget._marryrequest = false;
                ptarget.sendMessage("Your partner declined your marriage request.");
            }
        }
    }

    /**
     * Return the secondary weapon instance (always equipped in the left hand).
     */
    @Override
    public L2ItemInstance getSecondaryWeaponInstance() {
        return inventory.getPaperdollItem(EPaperdollSlot.PAPERDOLL_LHAND);
    }

    /**
     * Return the secondary L2Item item (always equiped in the left hand).
     */
    @Override
    public Item getSecondaryWeaponItem() {
        L2ItemInstance item = inventory.getPaperdollItem(EPaperdollSlot.PAPERDOLL_LHAND);
        if (item != null) { return item.getItem(); }

        return null;
    }

    /**
     * Kill the L2Character, Apply Death Penalty, Manage gain/loss Karma and Item Drop.
     * <ul>
     * <li>Reduce the Experience of the L2PcInstance in function of the calculated Death Penalty</li>
     * <li>If necessary, unsummon the Pet of the killed L2PcInstance</li>
     * <li>Manage Karma gain for attacker and Karam loss for the killed L2PcInstance</li>
     * <li>If the killed L2PcInstance has Karma, manage Drop Item</li>
     * <li>Kill the L2PcInstance</li>
     * </ul>
     *
     * @param killer The L2Character who attacks
     */
    @Override
    public boolean doDie(L2Character killer) {
        // Kill the L2PcInstance
        if (!super.doDie(killer)) { return false; }

        if (isMounted()) { stopFeed(); }

        synchronized (this) {
            if (_isFakeDeath) { stopFakeDeath(true); }
        }

        if (killer != null) {
            L2PcInstance pk = killer.getActingPlayer();

            // Clear resurrect xp calculation
            _expBeforeDeath = (long) 0;

            if (isCursedWeaponEquipped()) { CursedWeaponsManager.getInstance().drop(_cursedWeaponEquippedId, killer); }
            else {
                if (pk == null || !pk.isCursedWeaponEquipped()) {
                    onDieDropItem(killer); // Check if any item should be dropped

                    // if the area isn't an arena
                    if (!isInArena()) {
                        // if both victim and attacker got clans & aren't academicians
                        if (pk != null && pk.clan != null && clan != null && !isAcademyMember() && !pk.isAcademyMember()) {
                            // if clans got mutual war, then use the reputation calcul
                            if (clan.isAtWarWith(pk._clanId) && pk.clan.isAtWarWith(clan.getClanId())) {
                                // when your reputation score is 0 or below, the other clan cannot acquire any reputation points
                                if (clan.getReputationScore() > 0) {
                                    pk.clan.addReputationScore(1);
                                }
                                // when the opposing sides reputation score is 0 or below, your clans reputation score doesn't decrease
                                if (pk.clan.getReputationScore() > 0) { clan.takeReputationScore(1); }
                            }
                        }
                    }

                    // Reduce player's xp and karma.
                    if (Config.ALT_GAME_DELEVEL && (getSkillLevel(L2Skill.SKILL_LUCKY) < 0 || getStat().getLevel() > 9)) {
                        deathPenalty(pk != null && clan != null && pk.clan != null && (clan.isAtWarWith(pk._clanId) || pk.clan.isAtWarWith(_clanId)), pk != null, killer instanceof L2SiegeGuardInstance);
                    }
                }
            }
        }

        // Unsummon Cubics
        if (!_cubics.isEmpty()) {
            for (L2CubicInstance cubic : _cubics.values()) {
                cubic.stopAction();
                cubic.cancelDisappear();
            }

            _cubics.clear();
        }

        if (_fusionSkill != null) { abortCast(); }

        for (L2Character character : getKnownList().getKnownType(L2Character.class)) {
            if (character.getFusionSkill() != null && character.getFusionSkill().getTarget() == this) {
                character.abortCast();
            }
        }

        if (isInParty() && _party.isInDimensionalRift()) {
            _party.getDimensionalRift().getDeadMemberList().add(this);
        }

        // calculate death penalty buff
        calculateDeathPenaltyBuffLevel(killer);

        stopWaterTask();

        if (isPhoenixBlessed() || (isAffected(L2EffectFlag.CHARM_OF_COURAGE) && _isInSiege)) {
            reviveRequest(this, null, false);
        }

        // Icons update in order to get retained buffs list
        updateEffectIcons();

        return true;
    }

    private void onDieDropItem(L2Character killer) {
        if (killer == null) { return; }

        L2PcInstance pk = killer.getActingPlayer();
        if (_karma <= 0 && pk != null && pk.clan != null && clan != null && pk.clan.isAtWarWith(_clanId)) {
            return;
        }

        if ((!isInsideZone(ZoneId.PVP) || pk == null) && (!isGM() || Config.KARMA_DROP_GM)) {
            boolean isKillerNpc = killer instanceof L2Npc;
            int pkLimit = Config.KARMA_PK_LIMIT;

            int dropEquip = 0;
            int dropEquipWeapon = 0;
            int dropItem = 0;
            int dropLimit = 0;
            int dropPercent = 0;

            if (_karma > 0 && _pkKills >= pkLimit) {
                dropPercent = Config.KARMA_RATE_DROP;
                dropEquip = Config.KARMA_RATE_DROP_EQUIP;
                dropEquipWeapon = Config.KARMA_RATE_DROP_EQUIP_WEAPON;
                dropItem = Config.KARMA_RATE_DROP_ITEM;
                dropLimit = Config.KARMA_DROP_LIMIT;
            }
            else if (isKillerNpc && getLevel() > 4 && !isFestivalParticipant()) {
                dropPercent = Config.PLAYER_RATE_DROP;
                dropEquip = Config.PLAYER_RATE_DROP_EQUIP;
                dropEquipWeapon = Config.PLAYER_RATE_DROP_EQUIP_WEAPON;
                dropItem = Config.PLAYER_RATE_DROP_ITEM;
                dropLimit = Config.PLAYER_DROP_LIMIT;
            }

            if (dropPercent > 0 && Rnd.get(100) < dropPercent) {
                int dropCount = 0;
                int itemDropPercent = 0;

                for (L2ItemInstance itemDrop : inventory.getItems()) {
                    // Don't drop those following things
                    if (!itemDrop.isDropable() || itemDrop.isShadowItem() || itemDrop.getItemId() == 57 || itemDrop.getItem().getType2() == EItemType2.TYPE2_QUEST || summon != null && summon.getControlItemId() == itemDrop.getItemId() || Arrays.binarySearch(Config.KARMA_LIST_NONDROPPABLE_ITEMS, itemDrop.getItemId()) >= 0 || Arrays.binarySearch(Config.KARMA_LIST_NONDROPPABLE_PET_ITEMS, itemDrop.getItemId()) >= 0) {
                        continue;
                    }

                    if (itemDrop.isEquipped()) {
                        // Set proper chance according to Item type of equipped Item
                        itemDropPercent = itemDrop.getItem().getType2() == EItemType2.TYPE2_WEAPON ? dropEquipWeapon : dropEquip;
                        inventory.unEquipItemInSlot(EPaperdollSlot.getByIndex(itemDrop.getLocationSlot()));
                    }
                    else {
                        itemDropPercent = dropItem; // Item in inventory
                    }

                    // NOTE: Each time an item is dropped, the chance of another item being dropped gets lesser (dropCount * 2)
                    if (Rnd.get(100) < itemDropPercent) {
                        dropItem(EItemProcessPurpose.DIE_DROP, itemDrop, killer, true);

                        if (++dropCount >= dropLimit) { break; }
                    }
                }
            }
        }
    }

    public void updateKarmaLoss(long exp) {
        if (!isCursedWeaponEquipped() && _karma > 0) {
            int karmaLost = Formulas.calculateKarmaLost(getLevel(), exp);
            if (karmaLost > 0) {
                setKarma(_karma - karmaLost);
            }
        }
    }

    /**
     * This method is used to update PvP counter, or PK counter / add Karma if necessary.<br>
     * It also updates clan kills/deaths counters on siege.
     *
     * @param target The L2Playable victim.
     */
    public void onKillUpdatePvPKarma(L2Playable target) {
        if (target == null) { return; }

        L2PcInstance targetPlayer = target.getActingPlayer();
        if (targetPlayer == null || targetPlayer == this) { return; }

        // Don't rank up the CW if it was a summon.
        if (isCursedWeaponEquipped() && target instanceof L2PcInstance) {
            CursedWeaponsManager.getInstance().increaseKills(_cursedWeaponEquippedId);
            return;
        }

        // If in duel and you kill (only can kill l2summon), do nothing
        if (_isInDuel && targetPlayer._isInDuel) { return; }

        // If in pvp zone, do nothing.
        if (isInsideZone(ZoneId.PVP) && targetPlayer.isInsideZone(ZoneId.PVP)) {
            // Until the zone was a siege zone. Check also if victim was a player. Randomers aren't counted.
            if (target instanceof L2PcInstance && _siegeState > 0 && targetPlayer._siegeState > 0 && _siegeState != targetPlayer._siegeState) {
                // Now check clan relations.
                L2Clan killerClan = clan;
                if (killerClan != null) { killerClan.setSiegeKills(killerClan.getSiegeKills() + 1); }

                L2Clan targetClan = targetPlayer.clan;
                if (targetClan != null) { targetClan.setSiegeDeaths(targetClan.getSiegeDeaths() + 1); }
            }
            return;
        }

        // Check if it's pvp (cases : regular, wars, victim is PKer)
        if (checkIfPvP(target) || (targetPlayer.clan != null && clan != null && clan.isAtWarWith(targetPlayer._clanId) && targetPlayer.clan.isAtWarWith(_clanId) && targetPlayer._pledgeType != L2Clan.SUBUNIT_ACADEMY && _pledgeType != L2Clan.SUBUNIT_ACADEMY) || (targetPlayer._karma > 0 && Config.KARMA_AWARD_PK_KILL)) {
            if (target instanceof L2PcInstance) {
                // Add PvP point to attacker.
                _pvpKills = _pvpKills + 1;

                // Send UserInfo packet to attacker with its Karma and PK Counter
                sendPacket(new UserInfo(this));
            }
        }
        // Otherwise, killer is considered as a PKer.
        else if (targetPlayer._karma == 0 && targetPlayer._pvpFlag == 0) {
            // PK Points are increased only if you kill a player.
            if (target instanceof L2PcInstance) {
                _pkKills = _pkKills + 1;
            }

            // Calculate new karma.
            setKarma(_karma + Formulas.calculateKarmaGain(_pkKills, target instanceof L2Summon));

            // Send UserInfo packet to attacker with its Karma and PK Counter
            sendPacket(new UserInfo(this));
        }
    }

    public void updatePvPStatus() {
        if (isInsideZone(ZoneId.PVP)) { return; }

        PvpFlagTaskManager.getInstance().add(this, Config.PVP_NORMAL_TIME);

        if (_pvpFlag == 0) { updatePvPFlag(1); }
    }

    public void updatePvPStatus(L2Character target) {
        L2PcInstance player = target.getActingPlayer();
        if (player == null) { return; }

        if (_isInDuel && player._duelId == _duelId) { return; }

        if ((!isInsideZone(ZoneId.PVP) || !target.isInsideZone(ZoneId.PVP)) && player._karma == 0) {
            PvpFlagTaskManager.getInstance().add(this, checkIfPvP(player) ? Config.PVP_PVP_TIME : Config.PVP_NORMAL_TIME);

            if (_pvpFlag == 0) { updatePvPFlag(1); }
        }
    }

    /**
     * Restore the experience this L2PcInstance has lost and sends StatusUpdate packet.
     *
     * @param restorePercent The specified % of restored experience.
     */
    public void restoreExp(double restorePercent) {
        if (_expBeforeDeath > 0) {
            getStat().addExp((int) Math.round((_expBeforeDeath - getExp()) * restorePercent / 100));
            _expBeforeDeath = (long) 0;
        }
    }

    /**
     * Reduce the Experience (and level if necessary) of the L2PcInstance in function of the calculated Death Penalty.
     * <ul>
     * <li>Calculate the Experience loss</li>
     * <li>Set the value of _expBeforeDeath</li>
     * <li>Set the new Experience value of the L2PcInstance and Decrease its level if necessary</li>
     * <li>Send StatusUpdate packet with its new Experience</li>
     * </ul>
     *
     * @param atWar            If true, use clan war penalty system instead of regular system.
     * @param killedByPlayable Used to see if victim loses XP or not.
     * @param killedBySiegeNpc Used to see if victim loses XP or not.
     */
    public void deathPenalty(boolean atWar, boolean killedByPlayable, boolean killedBySiegeNpc) {
        // No xp loss inside pvp zone unless
        // - it's a siege zone and you're NOT participating
        // - you're killed by a non-pc whose not belong to the siege
        if (isInsideZone(ZoneId.PVP)) {
            // No xp loss for siege participants inside siege zone.
            if (isInsideZone(ZoneId.SIEGE)) {
                if (_isInSiege && (killedByPlayable || killedBySiegeNpc)) { return; }
            }
            // No xp loss for arenas participants killed by playable.
            else if (killedByPlayable) { return; }
        }

        // Get the level of the L2PcInstance
        int lvl = getLevel();

        // The death steal you some Exp
        double percentLost = 7.0;
        if (getLevel() >= 76) { percentLost = 2.0; }
        else if (getLevel() >= 40) { percentLost = 4.0; }

        if (_karma > 0) { percentLost *= Config.RATE_KARMA_EXP_LOST; }

        if (isFestivalParticipant() || atWar || isInsideZone(ZoneId.SIEGE)) { percentLost /= 4.0; }

        // Calculate the Experience loss
        long lostExp = 0;

        if (lvl < Experience.MAX_LEVEL) {
            lostExp = Math.round((getStat().getExpForLevel(lvl + 1) - getStat().getExpForLevel(lvl)) * percentLost / 100);
        }
        else {
            lostExp = Math.round((getStat().getExpForLevel(Experience.MAX_LEVEL) - getStat().getExpForLevel(Experience.MAX_LEVEL - 1)) * percentLost / 100);
        }

        // Get the Experience before applying penalty
        _expBeforeDeath = getExp();

        // Set new karma
        updateKarmaLoss(lostExp);

        // Set the new Experience value of the L2PcInstance
        getStat().addExp(-lostExp);
    }

    public boolean isPartyWaiting() {
        return PartyMatchWaitingList.getInstance().getPlayers().contains(this);
    }

    public int getPartyRoom() {
        return _partyroom;
    }

    public void setPartyRoom(int id) {
        _partyroom = id;
    }

    public boolean isInPartyMatchRoom() {
        return _partyroom > 0;
    }

    /**
     * Stop all timers related to that L2PcInstance.
     */
    public void stopAllTimers() {
        stopHpMpRegeneration();
        stopWaterTask();
        stopFeed();
        clearPetData();
        storePetFood(_mountNpcId);
        stopPunishTask(true);
        stopChargeTask();

        AttackStanceTaskManager.getInstance().remove(this);
        PvpFlagTaskManager.getInstance().remove(this);
        GameTimeTaskManager.getInstance().remove(this);
        ShadowItemTaskManager.getInstance().remove(this);
    }

    /**
     * Return the L2Summon of the L2PcInstance or null.
     */
    @Override
    public L2Summon getPet() {
        return summon;
    }

    /**
     * Set the L2Summon of the L2PcInstance.
     *
     * @param summon The Object.
     */
    public void setPet(L2Summon summon) {
        this.summon = summon;
    }

    /**
     * @return {@code true} if the player has a pet, {@code false} otherwise
     */
    public boolean hasPet() {
        return summon instanceof L2PetInstance;
    }

    /**
     * @return {@code true} if the player has a summon, {@code false} otherwise
     */
    public boolean hasServitor() {
        return summon instanceof L2SummonInstance;
    }

    /**
     * @return the L2TamedBeast of the L2PcInstance or null.
     */
    public L2TamedBeastInstance getTrainedBeast() {
        return _tamedBeast;
    }

    /**
     * Set the L2TamedBeast of the L2PcInstance.
     *
     * @param tamedBeast The Object.
     */
    public void setTrainedBeast(L2TamedBeastInstance tamedBeast) {
        _tamedBeast = tamedBeast;
    }

    /**
     * @return the L2PcInstance requester of a transaction (ex : FriendInvite, JoinAlly, JoinParty...).
     */
    public L2Request getRequest() {
        return _request;
    }

    /**
     * @return the L2PcInstance requester of a transaction (ex : FriendInvite, JoinAlly, JoinParty...).
     */
    public L2PcInstance getActiveRequester() {
        if (_activeRequester != null && _activeRequester.isRequestExpired() && _activeTradeList == null) {
            _activeRequester = null;
        }

        return _activeRequester;
    }

    /**
     * Set the L2PcInstance requester of a transaction (ex : FriendInvite, JoinAlly, JoinParty...).
     *
     * @param requester
     */
    public void setActiveRequester(L2PcInstance requester) {
        _activeRequester = requester;
    }

    /**
     * @return True if a request is in progress.
     */
    public boolean isProcessingRequest() {
        return getActiveRequester() != null || _requestExpireTime > System.currentTimeMillis();
    }

    /**
     * @return True if a transaction <B>(trade OR request)</B> is in progress.
     */
    public boolean isProcessingTransaction() {
        return getActiveRequester() != null || _activeTradeList != null || _requestExpireTime > System.currentTimeMillis();
    }

    /**
     * Set the _requestExpireTime of that L2PcInstance, and set his partner as the active requester.
     *
     * @param partner The partner to make checks on.
     */
    public void onTransactionRequest(L2PcInstance partner) {
        _requestExpireTime = System.currentTimeMillis() + REQUEST_TIMEOUT * 1000;
        partner._activeRequester = this;
    }

    /**
     * @return true if last request is expired.
     */
    public boolean isRequestExpired() {
        return _requestExpireTime <= System.currentTimeMillis();
    }

    /**
     * Select the Warehouse to be used in next activity.
     */
    public void onTransactionResponse() {
        _requestExpireTime = 0;
    }

    /**
     * @return The active Warehouse.
     */
    public ItemContainer getActiveWarehouse() {
        return _activeWarehouse;
    }

    /**
     * Select the Warehouse to be used in next activity.
     *
     * @param warehouse An active warehouse.
     */
    public void setActiveWarehouse(ItemContainer warehouse) {
        _activeWarehouse = warehouse;
    }

    /**
     * @return The active TradeList.
     */
    public TradeList getActiveTradeList() {
        return _activeTradeList;
    }

    /**
     * Set the TradeList to be used in next activity.
     *
     * @param tradeList The TradeList to be used.
     */
    public void setActiveTradeList(TradeList tradeList) {
        _activeTradeList = tradeList;
    }

    public void onTradeStart(L2PcInstance partner) {
        _activeTradeList = new TradeList(this);
        _activeTradeList.setPartner(partner);

        sendPacket(SystemMessage.getSystemMessage(SystemMessageId.BEGIN_TRADE_WITH_S1).addString(partner.getName()));
        sendPacket(new TradeStart(this));
    }

    public void onTradeConfirm(L2PcInstance partner) {
        sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CONFIRMED_TRADE).addString(partner.getName()));

        partner.sendPacket(TradePressOwnOk.STATIC_PACKET);
        sendPacket(TradePressOtherOk.STATIC_PACKET);
    }

    public void onTradeCancel(L2PcInstance partner) {
        if (_activeTradeList == null) { return; }

        _activeTradeList.lock();
        _activeTradeList = null;

        sendPacket(new SendTradeDone(0));
        sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CANCELED_TRADE).addString(partner.getName()));
    }

    public void onTradeFinish(boolean successfull) {
        _activeTradeList = null;
        sendPacket(new SendTradeDone(1));
        if (successfull) { sendPacket(SystemMessageId.TRADE_SUCCESSFUL); }
    }

    public void startTrade(L2PcInstance partner) {
        onTradeStart(partner);
        partner.onTradeStart(this);
    }

    public void cancelActiveTrade() {
        if (_activeTradeList == null) { return; }

        L2PcInstance partner = _activeTradeList.getPartner();
        if (partner != null) { partner.onTradeCancel(this); }

        onTradeCancel(this);
    }

    /**
     * @return The _createList object of the L2PcInstance.
     */
    public L2ManufactureList getCreateList() {
        return _createList;
    }

    /**
     * Set the _createList object of the L2PcInstance.
     *
     * @param list
     */
    public void setCreateList(L2ManufactureList list) {
        _createList = list;
    }

    /**
     * @return The _sellList object of the L2PcInstance.
     */
    public TradeList getSellList() {
        if (_sellList == null) { _sellList = new TradeList(this); }

        return _sellList;
    }

    /**
     * @return the _buyList object of the L2PcInstance.
     */
    public TradeList getBuyList() {
        if (_buyList == null) { _buyList = new TradeList(this); }

        return _buyList;
    }

    /**
     * @return The Private Store type of the L2PcInstance.
     */
    public PrivateStoreType getPrivateStoreType() {
        return _privateStoreType;
    }

    /**
     * Set the Private Store type of the L2PcInstance.
     *
     * @param type The value : 0 = none, 1 = sell, 2 = sellmanage, 3 = buy, 4 = buymanage, 5 = manufacture.
     */
    public void setPrivateStoreType(PrivateStoreType type) {
        _privateStoreType = type;
    }

    /**
     * @return The _skillLearningClassId object of the L2PcInstance.
     */
    public ClassId getSkillLearningClassId() {
        return _skillLearningClassId;
    }

    /**
     * Set the _skillLearningClassId object of the L2PcInstance.
     *
     * @param classId The parameter.
     */
    public void setSkillLearningClassId(ClassId classId) {
        _skillLearningClassId = classId;
    }

    /**
     * @return The clan object of the L2PcInstance.
     */
    public L2Clan getClan() {
        return clan;
    }

    /**
     * Set the clan object, _clanId, _clanLeader Flag and title of the L2PcInstance.
     *
     * @param clan The Clan object which is used to feed L2PcInstance values.
     */
    public void setClan(L2Clan clan) {
        this.clan = clan;
        setTitle("");

        if (clan == null) {
            _clanId = 0;
            _clanPrivileges = 0;
            _pledgeType = 0;
            _powerGrade = 0;
            _lvlJoinedAcademy = 0;
            _apprentice = 0;
            _sponsor = 0;
            return;
        }

        if (!clan.isMember(getObjectId())) {
            // char has been kicked from clan
            setClan(null);
            return;
        }

        _clanId = clan.getClanId();
    }

    /**
     * @return True if the L2PcInstance is the leader of its clan.
     */
    public boolean isClanLeader() {
        if (clan == null) { return false; }
        return getObjectId() == clan.getLeaderId();
    }

    /**
     * Reduce the number of arrows owned by the L2PcInstance and send InventoryUpdate or ItemList (to unequip if the last arrow was consummed).
     */
    @Override
    protected void reduceArrowCount() {
        L2ItemInstance arrows = inventory.getPaperdollItem(EPaperdollSlot.PAPERDOLL_LHAND);

        if (arrows == null) {
            inventory.unEquipItemInSlot(EPaperdollSlot.PAPERDOLL_LHAND);
            _arrowItem = null;
            sendPacket(new ItemList(this, false));
            return;
        }

        // Adjust item quantity
        if (arrows.getCount() > 1) {
            synchronized (arrows) {
                arrows.changeCountWithoutTrace(-1, this, null);
                arrows.setModifyState(EItemModifyState.MODIFIED);

                // could do also without saving, but let's save approx 1 of 10
                if (Rnd.get(10) < 1) { arrows.updateDatabase(); }
                inventory.refreshWeight();
            }
        }
        else {
            // Destroy entire item and save to database
            inventory.destroyItem(EItemProcessPurpose.CONSUME, arrows, this, null);
            inventory.unEquipItemInSlot(EPaperdollSlot.PAPERDOLL_LHAND);
            _arrowItem = null;

            sendPacket(new ItemList(this, false));
            return;
        }

        InventoryUpdate iu = new InventoryUpdate();
        iu.addModifiedItem(arrows);
        sendPacket(iu);
    }

    /**
     * Equip arrows needed in left hand and send ItemList to the L2PcInstance then return True.
     */
    @Override
    protected boolean checkAndEquipArrows() {
        // Check if nothing is equipped in left hand
        if (inventory.getPaperdollItem(EPaperdollSlot.PAPERDOLL_LHAND) == null) {
            // Get the L2ItemInstance of the arrows needed for this bow
            _arrowItem = inventory.findArrowForBow(getActiveWeaponItem());

            if (_arrowItem != null) {
                // Equip arrows needed in left hand
                inventory.setPaperdollItem(EPaperdollSlot.PAPERDOLL_LHAND, _arrowItem);

                // Send ItemList to this L2PcINstance to update left hand equipement
                sendPacket(new ItemList(this, false));
            }
        }
        // Get the L2ItemInstance of arrows equipped in left hand
        else {
            _arrowItem = inventory.getPaperdollItem(EPaperdollSlot.PAPERDOLL_LHAND);
        }

        return _arrowItem != null;
    }

    /**
     * Disarm the player's weapon and shield.
     *
     * @return true if successful, false otherwise.
     */
    public boolean disarmWeapons() {
        // Don't allow disarming a cursed weapon
        if (isCursedWeaponEquipped()) { return false; }

        // Unequip the weapon
        L2ItemInstance wpn = inventory.getPaperdollItem(EPaperdollSlot.PAPERDOLL_RHAND);
        if (wpn != null) {
            L2ItemInstance[] unequipped = inventory.unEquipItemInBodySlotAndRecord(wpn.getItem().getBodyPart());
            InventoryUpdate iu = new InventoryUpdate();
            for (L2ItemInstance itm : unequipped) { iu.addModifiedItem(itm); }
            sendPacket(iu);

            abortAttack();
            broadcastUserInfo();

            // this can be 0 if the user pressed the right mousebutton twice very fast
            if (unequipped.length > 0) {
                SystemMessage sm;
                if (unequipped[0].getEnchantLevel() > 0) {
                    sm = SystemMessage.getSystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED).addNumber(unequipped[0].getEnchantLevel()).addItemName(unequipped[0]);
                }
                else { sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISARMED).addItemName(unequipped[0]); }

                sendPacket(sm);
            }
        }

        // Unequip the shield
        L2ItemInstance sld = inventory.getPaperdollItem(EPaperdollSlot.PAPERDOLL_LHAND);
        if (sld != null) {
            L2ItemInstance[] unequipped = inventory.unEquipItemInBodySlotAndRecord(sld.getItem().getBodyPart());
            InventoryUpdate iu = new InventoryUpdate();
            for (L2ItemInstance itm : unequipped) { iu.addModifiedItem(itm); }
            sendPacket(iu);

            abortAttack();
            broadcastUserInfo();

            // this can be 0 if the user pressed the right mousebutton twice very fast
            if (unequipped.length > 0) {
                SystemMessage sm;
                if (unequipped[0].getEnchantLevel() > 0) {
                    sm = SystemMessage.getSystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED).addNumber(unequipped[0].getEnchantLevel()).addItemName(unequipped[0]);
                }
                else { sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISARMED).addItemName(unequipped[0]); }

                sendPacket(sm);
            }
        }
        return true;
    }

    public boolean mount(L2Summon pet) {
        if (!disarmWeapons()) { return false; }

        stopAllToggles();
        Ride mount = new Ride(getObjectId(), Ride.ACTION_MOUNT, pet.getTemplate().getNpcId());
        setMount(pet.getNpcId(), pet.getLevel(), mount.getMountType());
        _mountObjectID = pet.getControlItemId();
        clearPetData();
        startFeed(pet.getNpcId());
        broadcastPacket(mount);

        // Notify self and others about speed change
        broadcastUserInfo();

        pet.unSummon(this);
        return true;
    }

    public boolean mount(int npcId, int controlItemId, boolean useFood) {
        if (!disarmWeapons()) { return false; }

        stopAllToggles();
        Ride mount = new Ride(getObjectId(), Ride.ACTION_MOUNT, npcId);
        if (setMount(npcId, getLevel(), mount.getMountType())) {
            clearPetData();
            _mountObjectID = controlItemId;
            broadcastPacket(mount);

            // Notify self and others about speed change
            broadcastUserInfo();

            if (useFood) { startFeed(npcId); }

            return true;
        }
        return false;
    }

    public boolean mountPlayer(L2Summon summon) {
        if (summon != null && summon.isMountable() && !isMounted() && !isBetrayed()) {
            if (isDead()) // A strider cannot be ridden when dead.
            {
                sendPacket(SystemMessageId.STRIDER_CANT_BE_RIDDEN_WHILE_DEAD);
                return false;
            }

            if (summon.isDead()) // A dead strider cannot be ridden.
            {
                sendPacket(SystemMessageId.DEAD_STRIDER_CANT_BE_RIDDEN);
                return false;
            }

            if (summon.isInCombat() || summon.isRooted()) // A strider in battle cannot be ridden.
            {
                sendPacket(SystemMessageId.STRIDER_IN_BATLLE_CANT_BE_RIDDEN);
                return false;
            }

            if (isInCombat()) // A strider cannot be ridden while in battle
            {
                sendPacket(SystemMessageId.STRIDER_CANT_BE_RIDDEN_WHILE_IN_BATTLE);
                return false;
            }

            if (_waitTypeSitting) // A strider can be ridden only when standing
            {
                sendPacket(SystemMessageId.STRIDER_CAN_BE_RIDDEN_ONLY_WHILE_STANDING);
                return false;
            }

            if (isFishing()) // You can't mount, dismount, break and drop items while fishing
            {
                sendPacket(SystemMessageId.CANNOT_DO_WHILE_FISHING_2);
                return false;
            }

            if (isCursedWeaponEquipped()) // You can't mount, dismount, break and drop items while weilding a cursed weapon
            {
                sendPacket(SystemMessageId.STRIDER_CANT_BE_RIDDEN_WHILE_IN_BATTLE);
                return false;
            }

            if (!Util.checkIfInRange(200, this, summon, true)) {
                sendPacket(SystemMessageId.TOO_FAR_AWAY_FROM_STRIDER_TO_MOUNT);
                return false;
            }

            if (summon.isHungry()) {
                sendPacket(SystemMessageId.HUNGRY_STRIDER_NOT_MOUNT);
                return false;
            }

            if (!summon.isDead() && !isMounted()) { mount(summon); }
        }
        else if (isMounted()) {
            if (_mountType == 2 && isInsideZone(ZoneId.NO_LANDING)) {
                sendPacket(SystemMessageId.NO_DISMOUNT_HERE);
                return false;
            }

            if (isHungry()) {
                sendPacket(SystemMessageId.HUNGRY_STRIDER_NOT_MOUNT);
                return false;
            }

            dismount();
        }
        return true;
    }

    public boolean dismount() {
        sendPacket(new SetupGauge(3, 0, 0));
        int petId = _mountNpcId;
        if (setMount(0, 0, 0)) {
            stopFeed();
            clearPetData();

            broadcastPacket(new Ride(getObjectId(), Ride.ACTION_DISMOUNT, 0));

            _mountObjectID = 0;
            storePetFood(petId);

            // Notify self and others about speed change
            broadcastUserInfo();
            return true;
        }
        return false;
    }

    public void storePetFood(int petId) {
        if (_controlItemId != 0 && petId != 0) {
            try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
                PreparedStatement statement = con.prepareStatement("UPDATE pets SET fed=? WHERE item_obj_id = ?");
                statement.setInt(1, _curFeed);
                statement.setInt(2, _controlItemId);
                statement.executeUpdate();
                statement.close();
                _controlItemId = 0;
            }
            catch (Exception e) {
                _log.log(Level.SEVERE, "Failed to store Pet [NpcId: " + petId + "] data", e);
            }
        }
    }

    protected synchronized void startFeed(int npcId) {
        _canFeed = npcId > 0;
        if (!isMounted()) { return; }

        if (summon != null) {
            setCurrentFeed(((L2PetInstance) summon).getCurrentFed());
            _controlItemId = summon.getControlItemId();
            sendPacket(new SetupGauge(3, _curFeed * 10000 / getFeedConsume(), getMaxFeed() * 10000 / getFeedConsume()));
            if (!isDead()) {
                _mountFeedTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new FeedTask(), 10000, 10000);
            }
        }
        else if (_canFeed) {
            setCurrentFeed(getMaxFeed());
            sendPacket(new SetupGauge(3, _curFeed * 10000 / getFeedConsume(), getMaxFeed() * 10000 / getFeedConsume()));
            if (!isDead()) {
                _mountFeedTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new FeedTask(), 10000, 10000);
            }
        }
    }

    protected synchronized void stopFeed() {
        if (_mountFeedTask != null) {
            _mountFeedTask.cancel(false);
            _mountFeedTask = null;
        }
    }

    private final void clearPetData() {
        _data = null;
    }

    protected final L2PetData getPetData(int npcId) {
        if (_data == null) { _data = PetDataTable.getInstance().getPetData(npcId); }

        return _data;
    }

    private final L2PetLevelData getPetLevelData(int npcId) {
        if (_leveldata == null) {
            _leveldata = PetDataTable.getInstance().getPetData(npcId).getPetLevelData(_mountLevel);
        }

        return _leveldata;
    }

    public int getCurrentFeed() {
        return _curFeed;
    }

    public void setCurrentFeed(int num) {
        _curFeed = (num > getMaxFeed()) ? getMaxFeed() : num;
        sendPacket(new SetupGauge(3, _curFeed * 10000 / getFeedConsume(), getMaxFeed() * 10000 / getFeedConsume()));
    }

    protected int getFeedConsume() {
        return (isAttackingNow()) ? getPetLevelData(_mountNpcId).getPetFeedBattle() : getPetLevelData(_mountNpcId).getPetFeedNormal();
    }

    private int getMaxFeed() {
        return getPetLevelData(_mountNpcId).getPetMaxFeed();
    }

    protected boolean isHungry() {
        return _canFeed ? (_curFeed < (getPetLevelData(_mountNpcId).getPetMaxFeed() * 0.55)) : false;
    }

    /**
     * @return the type of attack, depending of the worn weapon.
     */
    @Override
    public EWeaponType getAttackType() {
        Weapon weapon = getActiveWeaponItem();
        if (weapon != null) { return weapon.getItemType(); }

        return EWeaponType.FIST;
    }

    public long getUptime() {
        return System.currentTimeMillis() - _uptime;
    }

    public void setUptime(long time) {
        _uptime = time;
    }

    /**
     * Return True if the L2PcInstance is invulnerable.
     */
    @Override
    public boolean isInvul() {
        return super.isInvul() || isSpawnProtected();
    }

    /**
     * Return True if the L2PcInstance has a Party in progress.
     */
    @Override
    public boolean isInParty() {
        return _party != null;
    }

    /**
     * Set the _party object of the L2PcInstance AND join it.
     *
     * @param party
     */
    public void joinParty(L2Party party) {
        if (party != null) {
            _party = party;
            party.addPartyMember(this);
        }
    }

    /**
     * Manage the Leave Party task of the L2PcInstance.
     */
    public void leaveParty() {
        if (isInParty()) {
            _party.removePartyMember(this, MessageType.Disconnected);
            _party = null;
        }
    }

    /**
     * Return the _party object of the L2PcInstance.
     */
    @Override
    public L2Party getParty() {
        return _party;
    }

    /**
     * Set the _party object of the L2PcInstance (without joining it).
     *
     * @param party The object.
     */
    public void setParty(L2Party party) {
        _party = party;
    }

    /**
     * Return True if the L2PcInstance is a GM.
     */
    @Override
    public boolean isGM() {
        return getAccessLevel().isGm();
    }

    /**
     * @return the _accessLevel of the L2PcInstance.
     */
    public L2AccessLevel getAccessLevel() {
        if (Config.EVERYBODY_HAS_ADMIN_RIGHTS) { return AccessLevels.MASTER_ACCESS_LEVEL; }

        if (_accessLevel == null) /* This is here because inventory etc. is loaded before access level on login, so it is not null */ {
            setAccessLevel(AccessLevels.USER_ACCESS_LEVEL_NUMBER);
        }

        return _accessLevel;
    }

    /**
     * Set the _accessLevel of the L2PcInstance.
     *
     * @param level
     */
    public void setAccessLevel(int level) {
        if (level == AccessLevels.MASTER_ACCESS_LEVEL_NUMBER) {
            _log.warning(getName() + " has logged in with Master access level.");
            _accessLevel = AccessLevels.MASTER_ACCESS_LEVEL;
        }
        else if (level == AccessLevels.USER_ACCESS_LEVEL_NUMBER) { _accessLevel = AccessLevels.USER_ACCESS_LEVEL; }
        else {
            L2AccessLevel accessLevel = AccessLevels.getInstance().getAccessLevel(level);

            if (accessLevel == null) {
                if (level < 0) {
                    AccessLevels.getInstance().addBanAccessLevel(level);
                    _accessLevel = AccessLevels.getInstance().getAccessLevel(level);
                }
                else {
                    _log.warning("Server tried to set unregistered access level " + level + " to " + getName() + ". His access level have been reseted to user level.");
                    _accessLevel = AccessLevels.USER_ACCESS_LEVEL;
                }
            }
            else {
                _accessLevel = accessLevel;
                setTitle(_accessLevel.getName());
            }
        }

        appearance.setNameColor(_accessLevel.getNameColor());
        appearance.setTitleColor(_accessLevel.getTitleColor());
        broadcastUserInfo();

        CharNameTable.getInstance().addName(this);
    }

    /**
     * Update Stats of the L2PcInstance client side by sending UserInfo/StatusUpdate to this L2PcInstance and CharInfo/StatusUpdate to all L2PcInstance in its _KnownPlayers (broadcast).
     *
     * @param broadcastType
     */
    public void updateAndBroadcastStatus(int broadcastType) {
        refreshOverloaded();
        refreshExpertisePenalty();

        if (broadcastType == 1) { sendPacket(new UserInfo(this)); }
        else if (broadcastType == 2) { broadcastUserInfo(); }
    }

    /**
     * Send StatusUpdate packet with Karma to the L2PcInstance and all L2PcInstance to inform (broadcast).
     */
    public void broadcastKarma() {
        StatusUpdate su = new StatusUpdate(this);
        su.addAttribute(StatusUpdate.KARMA, _karma);
        sendPacket(su);

        if (summon != null) {
            sendPacket(new RelationChanged(summon, getRelationTo(this), false));
        }

        broadcastRelationsChanges();
    }

    /**
     * Set the online Flag to True or False and update the characters table of the database with online status and lastAccess (called when login and logout).
     *
     * @param isOnline
     * @param updateInDb
     */
    public void setOnlineStatus(boolean isOnline, boolean updateInDb) {
        if (_isOnline != isOnline) { _isOnline = isOnline; }

        // Update the characters table of the database with online status and lastAccess (called when login and logout)
        if (updateInDb) { updateOnlineStatus(); }
    }

    public void setIsIn7sDungeon(boolean isIn7sDungeon) {
        _isIn7sDungeon = isIn7sDungeon;
    }

    /**
     * Update the characters table of the database with online status and lastAccess of this L2PcInstance (called when login and logout).
     */
    public void updateOnlineStatus() {
        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("UPDATE characters SET online=?, lastAccess=? WHERE obj_id=?");
            statement.setInt(1, isOnlineInt());
            statement.setLong(2, System.currentTimeMillis());
            statement.setInt(3, getObjectId());
            statement.execute();
            statement.close();
        }
        catch (Exception e) {
            _log.warning("could not set char online status:" + e);
        }
    }

    /**
     * Create a new player in the characters table of the database.
     *
     * @return true if successful.
     */
    private boolean createDb() {
        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement(INSERT_CHARACTER);
            statement.setString(1, accountName);
            statement.setInt(2, getObjectId());
            statement.setString(3, getName());
            statement.setInt(4, getLevel());
            statement.setInt(5, getMaxHp());
            statement.setDouble(6, getCurrentHp());
            statement.setInt(7, getMaxCp());
            statement.setDouble(8, getCurrentCp());
            statement.setInt(9, getMaxMp());
            statement.setDouble(10, getCurrentMp());
            statement.setInt(11, appearance.getFace());
            statement.setInt(12, appearance.getHairStyle());
            statement.setInt(13, appearance.getHairColor());
            statement.setInt(14, appearance.getSex() ? 1 : 0);
            statement.setLong(15, getExp());
            statement.setInt(16, getSp());
            statement.setInt(17, _karma);
            statement.setInt(18, _pvpKills);
            statement.setInt(19, _pkKills);
            statement.setInt(20, _clanId);
            statement.setInt(21, getRace().ordinal());
            statement.setInt(22, getClassId().getId());
            statement.setLong(23, _deleteTimer);
            statement.setInt(24, hasDwarvenCraft() ? 1 : 0);
            statement.setString(25, getTitle());
            statement.setInt(26, getAccessLevel().getLevel());
            statement.setInt(27, isOnlineInt());
            statement.setInt(28, _isIn7sDungeon ? 1 : 0);
            statement.setInt(29, _clanPrivileges);
            statement.setInt(30, wantsPeace() ? 1 : 0);
            statement.setInt(31, _baseClass);
            statement.setInt(32, _noble ? 1 : 0);
            statement.setLong(33, 0);
            statement.setLong(34, System.currentTimeMillis());
            statement.executeUpdate();
            statement.close();
        }
        catch (Exception e) {
            _log.severe("Could not insert char data: " + e);
            return false;
        }
        return true;
    }

    public Forum getMail() {
        if (_forumMail == null) {
            _forumMail = ForumsBBSManager.getInstance().getForumByName("MailRoot").getChildByName(getName());

            if (_forumMail == null) {
                ForumsBBSManager.getInstance().createNewForum(getName(), ForumsBBSManager.getInstance().getForumByName("MailRoot"), Forum.MAIL, Forum.OWNERONLY, getObjectId());
                _forumMail = ForumsBBSManager.getInstance().getForumByName("MailRoot").getChildByName(getName());
            }
        }

        return _forumMail;
    }

    public void setMail(Forum forum) {
        _forumMail = forum;
    }

    public Forum getMemo() {
        if (_forumMemo == null) {
            _forumMemo = ForumsBBSManager.getInstance().getForumByName("MemoRoot").getChildByName(accountName);

            if (_forumMemo == null) {
                ForumsBBSManager.getInstance().createNewForum(accountName, ForumsBBSManager.getInstance().getForumByName("MemoRoot"), Forum.MEMO, Forum.OWNERONLY, getObjectId());
                _forumMemo = ForumsBBSManager.getInstance().getForumByName("MemoRoot").getChildByName(accountName);
            }
        }

        return _forumMemo;
    }

    public void setMemo(Forum forum) {
        _forumMemo = forum;
    }

    /**
     * Restores secondary data for the L2PcInstance, based on the current class index.
     */
    private void restoreCharData() {
        // Retrieve from the database all skills of this L2PcInstance and add them to _skills.
        restoreSkills();

        // Retrieve from the database all macroses of this L2PcInstance and add them to _macroses.
        _macroses.restore();

        // Retrieve from the database all shortCuts of this L2PcInstance and add them to _shortCuts.
        _shortCuts.restore();

        // Retrieve from the database all henna of this L2PcInstance and add them to _henna.
        restoreHenna();

        // Retrieve from the database all recom data of this L2PcInstance and add to _recomChars.
        restoreRecom();

        // Retrieve from the database the recipe book of this L2PcInstance.
        if (!isSubClassActive()) { restoreRecipeBook(); }
    }

    /**
     * Store recipe book data for this L2PcInstance, if not on an active sub-class.
     */
    private void storeRecipeBook() {
        // If the player is on a sub-class don't even attempt to store a recipe book.
        if (isSubClassActive()) { return; }

        if (getCommonRecipeBook().isEmpty() && getDwarvenRecipeBook().isEmpty()) { return; }

        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("DELETE FROM character_recipebook WHERE char_id=?");
            statement.setInt(1, getObjectId());
            statement.execute();
            statement.close();

            for (RecipeList recipe : getCommonRecipeBook()) {
                statement = con.prepareStatement("INSERT INTO character_recipebook (char_id, id, type) VALUES(?,?,0)");
                statement.setInt(1, getObjectId());
                statement.setInt(2, recipe.getId());
                statement.execute();
                statement.close();
            }

            for (RecipeList recipe : getDwarvenRecipeBook()) {
                statement = con.prepareStatement("INSERT INTO character_recipebook (char_id, id, type) VALUES(?,?,1)");
                statement.setInt(1, getObjectId());
                statement.setInt(2, recipe.getId());
                statement.execute();
                statement.close();
            }
        }
        catch (Exception e) {
            _log.warning("Could not store recipe book data: " + e);
        }
    }

    /**
     * Restore recipe book data for this L2PcInstance.
     */
    private void restoreRecipeBook() {
        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("SELECT id, type FROM character_recipebook WHERE char_id=?");
            statement.setInt(1, getObjectId());
            ResultSet rset = statement.executeQuery();

            while (rset.next()) {
                RecipeList recipe = RecipeTable.getInstance().getRecipeList(rset.getInt("id"));
                if (rset.getInt("type") == 1) { registerDwarvenRecipeList(recipe); }
                else { registerCommonRecipeList(recipe); }
            }

            rset.close();
            statement.close();
        }
        catch (Exception e) {
            _log.warning("Could not restore recipe book data:" + e);
        }
    }

    /**
     * Update L2PcInstance stats in the characters table of the database.
     *
     * @param storeActiveEffects
     */
    public synchronized void store(boolean storeActiveEffects) {
        // update client coords, if these look like true
        if (isInsideRadius(_clientX, _clientY, 1000, true)) {
            setXYZ(_clientX, _clientY, _clientZ);
        }

        storeCharBase();
        storeCharSub();
        storeEffect(storeActiveEffects);
        storeRecipeBook();

        SevenSigns.getInstance().saveSevenSignsData(getObjectId());

        _vars.storeMe();
    }

    public void store() {
        store(true);
    }

    private void storeCharBase() {
        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            // Get the exp, level, and sp of base class to store in base table
            int currentClassIndex = _classIndex;
            _classIndex = 0;
            long exp = getStat().getExp();
            int level = getStat().getLevel();
            int sp = getStat().getSp();
            _classIndex = currentClassIndex;

            PreparedStatement statement = con.prepareStatement(UPDATE_CHARACTER);

            statement.setInt(1, level);
            statement.setInt(2, getMaxHp());
            statement.setDouble(3, getCurrentHp());
            statement.setInt(4, getMaxCp());
            statement.setDouble(5, getCurrentCp());
            statement.setInt(6, getMaxMp());
            statement.setDouble(7, getCurrentMp());
            statement.setInt(8, appearance.getFace());
            statement.setInt(9, appearance.getHairStyle());
            statement.setInt(10, appearance.getHairColor());
            statement.setInt(11, appearance.getSex() ? 1 : 0);
            statement.setInt(12, getHeading());
            statement.setInt(13, _observerMode ? _savedLocation.getX() : getX());
            statement.setInt(14, _observerMode ? _savedLocation.getY() : getY());
            statement.setInt(15, _observerMode ? _savedLocation.getZ() : getZ());
            statement.setLong(16, exp);
            statement.setLong(17, _expBeforeDeath);
            statement.setInt(18, sp);
            statement.setInt(19, _karma);
            statement.setInt(20, _pvpKills);
            statement.setInt(21, _pkKills);
            statement.setInt(22, _recomHave);
            statement.setInt(23, _recomLeft);
            statement.setInt(24, _clanId);
            statement.setInt(25, getRace().ordinal());
            statement.setInt(26, getClassId().getId());
            statement.setLong(27, _deleteTimer);
            statement.setString(28, getTitle());
            statement.setInt(29, getAccessLevel().getLevel());
            statement.setInt(30, isOnlineInt());
            statement.setInt(31, _isIn7sDungeon ? 1 : 0);
            statement.setInt(32, _clanPrivileges);
            statement.setInt(33, wantsPeace() ? 1 : 0);
            statement.setInt(34, _baseClass);

            long totalOnlineTime = _onlineTime;
            if (_onlineBeginTime > 0) { totalOnlineTime += (System.currentTimeMillis() - _onlineBeginTime) / 1000; }

            statement.setLong(35, totalOnlineTime);
            statement.setInt(36, _punishLevel.value());
            statement.setLong(37, _punishTimer);
            statement.setInt(38, _noble ? 1 : 0);
            statement.setLong(39, _powerGrade);
            statement.setInt(40, _pledgeType);
            statement.setLong(41, _lastRecomUpdate);
            statement.setInt(42, _lvlJoinedAcademy);
            statement.setLong(43, _apprentice);
            statement.setLong(44, _sponsor);
            statement.setInt(45, _alliedVarkaKetra);
            statement.setLong(46, _clanJoinExpiryTime);
            statement.setLong(47, _clanCreateExpiryTime);
            statement.setString(48, getName());
            statement.setLong(49, _deathPenaltyBuffLevel);
            statement.setInt(50, getObjectId());

            statement.execute();
            statement.close();
        }
        catch (Exception e) {
            _log.warning("Could not store char base data: " + e);
        }
    }

    private void storeCharSub() {
        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement(UPDATE_CHAR_SUBCLASS);

            if (getTotalSubClasses() > 0) {
                for (SubClass subClass : _subClasses.values()) {
                    statement.setLong(1, subClass.getExp());
                    statement.setInt(2, subClass.getSp());
                    statement.setInt(3, subClass.getLevel());
                    statement.setInt(4, subClass.getClassId());
                    statement.setInt(5, getObjectId());
                    statement.setInt(6, subClass.getClassIndex());

                    statement.execute();
                }
            }
            statement.close();
        }
        catch (Exception e) {
            _log.warning("Could not store sub class data for " + getName() + ": " + e);
        }
    }

    private void storeEffect(boolean storeEffects) {
        if (!Config.STORE_SKILL_COOLTIME) { return; }

        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            // Delete all current stored effects for char to avoid dupe
            PreparedStatement statement = con.prepareStatement(DELETE_SKILL_SAVE);

            statement.setInt(1, getObjectId());
            statement.setInt(2, _classIndex);
            statement.execute();
            statement.close();

            int buff_index = 0;

            List<Integer> storedSkills = new ArrayList<>();

            // Store all effect data along with calulated remaining reuse delays for matching skills. 'restore_type'= 0.
            statement = con.prepareStatement(ADD_SKILL_SAVE);

            if (storeEffects) {
                for (L2Effect effect : getAllEffects()) {
                    if (effect == null) { continue; }

                    switch (effect.getEffectType()) {
                        case HEAL_OVER_TIME:
                        case COMBAT_POINT_HEAL_OVER_TIME:
                            continue;
                    }

                    L2Skill skill = effect.getSkill();
                    if (storedSkills.contains(skill.getReuseHashCode())) { continue; }

                    storedSkills.add(skill.getReuseHashCode());

                    if (!effect.isHerbEffect() && effect.getInUse() && !skill.isToggle()) {
                        statement.setInt(1, getObjectId());
                        statement.setInt(2, skill.getId());
                        statement.setInt(3, skill.getLevel());
                        statement.setInt(4, effect.getCount());
                        statement.setInt(5, effect.getTime());

                        if (_reuseTimeStamps.containsKey(skill.getReuseHashCode())) {
                            TimeStamp t = _reuseTimeStamps.get(skill.getReuseHashCode());
                            statement.setLong(6, t.hasNotPassed() ? t.getReuse() : 0);
                            statement.setDouble(7, t.hasNotPassed() ? t.getStamp() : 0);
                        }
                        else {
                            statement.setLong(6, 0);
                            statement.setDouble(7, 0);
                        }

                        statement.setInt(8, 0);
                        statement.setInt(9, _classIndex);
                        statement.setInt(10, ++buff_index);
                        statement.execute();
                    }
                }
            }

            // Store the reuse delays of remaining skills which lost effect but still under reuse delay. 'restore_type' 1.
            for (Entry<Integer, TimeStamp> timestampEntry : _reuseTimeStamps.entrySet()) {
                int hash = timestampEntry.getKey();
                if (storedSkills.contains(hash)) { continue; }

                TimeStamp t = timestampEntry.getValue();
                if (t != null && t.hasNotPassed()) {
                    storedSkills.add(hash);

                    statement.setInt(1, getObjectId());
                    statement.setInt(2, t.getSkillId());
                    statement.setInt(3, t.getSkillLvl());
                    statement.setInt(4, -1);
                    statement.setInt(5, -1);
                    statement.setLong(6, t.getReuse());
                    statement.setDouble(7, t.getStamp());
                    statement.setInt(8, 1);
                    statement.setInt(9, _classIndex);
                    statement.setInt(10, ++buff_index);
                    statement.execute();
                }
            }
            statement.close();
        }
        catch (Exception e) {
            _log.log(Level.WARNING, "Could not store char effect data: ", e);
        }
    }

    /**
     * @return True if the L2PcInstance is online.
     */
    public boolean isOnline() {
        return _isOnline;
    }

    /**
     * @return an int interpretation of online status.
     */
    public int isOnlineInt() {
        if (_isOnline && _client != null) {
            return _client.isDetached() ? 2 : 1;
        }

        return 0;
    }

    public boolean isIn7sDungeon() {
        return _isIn7sDungeon;
    }

    /**
     * Add a skill to the L2PcInstance _skills and its Func objects to the calculator set of the L2PcInstance and save update in the character_skills table of the database.
     * <ul>
     * <li>Replace oldSkill by newSkill or Add the newSkill</li>
     * <li>If an old skill has been replaced, remove all its Func objects of L2Character calculator set</li>
     * <li>Add Func objects of newSkill to the calculator set of the L2Character</li>
     * </ul>
     *
     * @param newSkill The L2Skill to add to the L2Character
     * @param store
     * @return The L2Skill replaced or null if just added a new L2Skill
     */
    public L2Skill addSkill(L2Skill newSkill, boolean store) {
        // Add a skill to the L2PcInstance _skills and its Func objects to the calculator set of the L2PcInstance
        L2Skill oldSkill = addSkill(newSkill);

        // Add or update a L2PcInstance skill in the character_skills table of the database
        if (store) { storeSkill(newSkill, oldSkill, -1); }

        return oldSkill;
    }

    @Override
    public L2Skill removeSkill(L2Skill skill, boolean store) {
        if (store) { return removeSkill(skill); }

        return super.removeSkill(skill, true);
    }

    public L2Skill removeSkill(L2Skill skill, boolean store, boolean cancelEffect) {
        if (store) { return removeSkill(skill); }

        return super.removeSkill(skill, cancelEffect);
    }

    /**
     * Remove a skill from the L2Character and its Func objects from calculator set of the L2Character and save update in the character_skills table of the database.
     * <ul>
     * <li>Remove the skill from the L2Character _skills</li>
     * <li>Remove all its Func objects from the L2Character calculator set</li>
     * </ul>
     *
     * @param skill The L2Skill to remove from the L2Character
     * @return The L2Skill removed
     */
    @Override
    public L2Skill removeSkill(L2Skill skill) {
        // Remove a skill from the L2Character and its Func objects from calculator set of the L2Character
        L2Skill oldSkill = super.removeSkill(skill);

        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement(DELETE_SKILL_FROM_CHAR);

            if (oldSkill != null) {
                statement.setInt(1, oldSkill.getId());
                statement.setInt(2, getObjectId());
                statement.setInt(3, _classIndex);
                statement.execute();
            }
            statement.close();
        }
        catch (Exception e) {
            _log.warning("Error could not delete skill: " + e);
        }

        // Don't busy with shortcuts if skill was a passive skill.
        if (skill != null && !skill.isPassive()) {
            for (L2ShortCut sc : getAllShortCuts()) {
                if (sc != null && sc.getId() == skill.getId() && sc.getType() == L2ShortCut.TYPE_SKILL) {
                    deleteShortCut(sc.getSlot(), sc.getPage());
                }
            }
        }

        return oldSkill;
    }

    /**
     * Add or update a L2PcInstance skill in the character_skills table of the database. <BR>
     * <BR>
     * If newClassIndex > -1, the skill will be stored with that class index, not the current one.
     *
     * @param newSkill
     * @param oldSkill
     * @param newClassIndex
     */
    private void storeSkill(L2Skill newSkill, L2Skill oldSkill, int newClassIndex) {
        int classIndex = _classIndex;

        if (newClassIndex > -1) { classIndex = newClassIndex; }

        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            if (oldSkill != null && newSkill != null) {
                PreparedStatement statement = con.prepareStatement(UPDATE_CHARACTER_SKILL_LEVEL);
                statement.setInt(1, newSkill.getLevel());
                statement.setInt(2, oldSkill.getId());
                statement.setInt(3, getObjectId());
                statement.setInt(4, classIndex);
                statement.execute();
                statement.close();
            }
            else if (newSkill != null) {
                PreparedStatement statement = con.prepareStatement(ADD_NEW_SKILL);
                statement.setInt(1, getObjectId());
                statement.setInt(2, newSkill.getId());
                statement.setInt(3, newSkill.getLevel());
                statement.setInt(4, classIndex);
                statement.execute();
                statement.close();
            }
            else {
                _log.warning("storeSkill() couldn't store new skill. It's null type.");
            }
        }
        catch (Exception e) {
            _log.warning("Error could not store char skills: " + e);
        }
    }

    /**
     * Retrieve from the database all skills of this L2PcInstance and add them to _skills.
     */
    private void restoreSkills() {
        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement(RESTORE_SKILLS_FOR_CHAR);
            statement.setInt(1, getObjectId());
            statement.setInt(2, _classIndex);
            ResultSet rset = statement.executeQuery();

            // Go though the recordset of this SQL query
            while (rset.next()) {
                int id = rset.getInt("skill_id");
                int level = rset.getInt("skill_level");

                if (id > 9000) {
                    continue; // fake skills for base stats
                }

                // Create a L2Skill object for each record
                L2Skill skill = SkillTable.getInfo(id, level);

                // Add the L2Skill object to the L2Character _skills and its Func objects to the calculator set of the L2Character
                addSkill(skill);
            }

            rset.close();
            statement.close();
        }
        catch (Exception e) {
            _log.warning("Could not restore character skills: " + e);
        }
    }

    /**
     * Retrieve from the database all skill effects of this L2PcInstance and add them to the player.
     */
    public void restoreEffects() {
        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement(RESTORE_SKILL_SAVE);
            statement.setInt(1, getObjectId());
            statement.setInt(2, _classIndex);
            ResultSet rset = statement.executeQuery();

            while (rset.next()) {
                int effectCount = rset.getInt("effect_count");
                int effectCurTime = rset.getInt("effect_cur_time");
                long reuseDelay = rset.getLong("reuse_delay");
                long systime = rset.getLong("systime");
                int restoreType = rset.getInt("restore_type");

                L2Skill skill = SkillTable.getInfo(rset.getInt("skill_id"), rset.getInt("skill_level"));
                if (skill == null) { continue; }

                long remainingTime = systime - System.currentTimeMillis();
                if (remainingTime > 10) {
                    disableSkill(skill, remainingTime);
                    addTimeStamp(skill, reuseDelay, systime);
                }

                /**
                 * Restore Type 1 The remaning skills lost effect upon logout but were still under a high reuse delay.
                 */
                if (restoreType > 0) { continue; }

                /**
                 * Restore Type 0 These skills were still in effect on the character upon logout. Some of which were self casted and might still have a long reuse delay which also is restored.
                 */
                if (skill.hasEffects()) {
                    Env env = new Env();
                    env.setCharacter(this);
                    env.setTarget(this);
                    env.setSkill(skill);

                    for (EffectTemplate et : skill.getEffectTemplates()) {
                        L2Effect ef = et.getEffect(env);
                        if (ef != null) {
                            ef.setCount(effectCount);
                            ef.setFirstTime(effectCurTime);
                            ef.scheduleEffect();
                        }
                    }
                }
            }

            rset.close();
            statement.close();

            statement = con.prepareStatement(DELETE_SKILL_SAVE);
            statement.setInt(1, getObjectId());
            statement.setInt(2, _classIndex);
            statement.executeUpdate();
            statement.close();
        }
        catch (Exception e) {
            _log.log(Level.WARNING, "Could not restore " + this + " active effect data: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieve from the database all Henna of this L2PcInstance, add them to _henna and calculate stats of the L2PcInstance.
     */
    private void restoreHenna() {
        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement(RESTORE_CHAR_HENNAS);
            statement.setInt(1, getObjectId());
            statement.setInt(2, _classIndex);
            ResultSet rset = statement.executeQuery();

            for (int i = 0; i < 3; i++) { _henna[i] = null; }

            while (rset.next()) {
                int slot = rset.getInt("slot");

                if (slot < 1 || slot > 3) { continue; }

                int symbolId = rset.getInt("symbol_id");
                if (symbolId != 0) {
                    Henna tpl = HennaTable.getInstance().getTemplate(symbolId);
                    if (tpl != null) { _henna[slot - 1] = tpl; }
                }
            }

            rset.close();
            statement.close();
        }
        catch (Exception e) {
            _log.warning("could not restore henna: " + e);
        }

        // Calculate Henna modifiers of this L2PcInstance
        recalcHennaStats();
    }

    /**
     * Retrieve from the database all Recommendation data of this L2PcInstance, add to _recomChars and calculate stats of the L2PcInstance.
     */
    private void restoreRecom() {
        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement(RESTORE_CHAR_RECOMS);
            statement.setInt(1, getObjectId());
            ResultSet rset = statement.executeQuery();
            while (rset.next()) { _recomChars.add(rset.getInt("target_id")); }

            rset.close();
            statement.close();
        }
        catch (Exception e) {
            _log.warning("could not restore recommendations: " + e);
        }
    }

    /**
     * @return the number of Henna empty slot of the L2PcInstance.
     */
    public int getHennaEmptySlots() {
        int totalSlots = 0;
        if (getClassId().level() == 1) { totalSlots = 2; }
        else { totalSlots = 3; }

        for (int i = 0; i < 3; i++) {
            if (_henna[i] != null) { totalSlots--; }
        }

        if (totalSlots <= 0) { return 0; }

        return totalSlots;
    }

    /**
     * Remove a Henna of the L2PcInstance, save update in the character_hennas table of the database and send HennaInfo/UserInfo packet to this L2PcInstance.
     *
     * @param slot The slot number to make checks on.
     * @return true if successful.
     */
    public boolean removeHenna(int slot) {
        if (slot < 1 || slot > 3) { return false; }

        slot--;

        if (_henna[slot] == null) { return false; }

        Henna henna = _henna[slot];
        _henna[slot] = null;

        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement(DELETE_CHAR_HENNA);

            statement.setInt(1, getObjectId());
            statement.setInt(2, slot + 1);
            statement.setInt(3, _classIndex);

            statement.execute();
            statement.close();
        }
        catch (Exception e) {
            _log.warning("could not remove char henna: " + e);
        }

        // Calculate Henna modifiers of this L2PcInstance
        recalcHennaStats();

        // Send HennaInfo packet to this L2PcInstance
        sendPacket(new HennaInfo(this));

        // Send UserInfo packet to this L2PcInstance
        sendPacket(new UserInfo(this));

        reduceAdena(EItemProcessPurpose.HENNA, henna.getPrice() / 5, this, false);

        // Add the recovered dyes to the player's inventory and notify them.
        addItem(EItemProcessPurpose.HENNA, henna.getDyeId(), Henna.getAmountDyeRequire() / 2, this, true);
        sendPacket(SystemMessageId.SYMBOL_DELETED);
        return true;
    }

    /**
     * Add a Henna to the L2PcInstance, save update in the character_hennas table of the database and send Server->Client HennaInfo/UserInfo packet to this L2PcInstance.
     *
     * @param henna The Henna template to add.
     */
    public void addHenna(Henna henna) {
        for (int i = 0; i < 3; i++) {
            if (_henna[i] == null) {
                _henna[i] = henna;

                // Calculate Henna modifiers of this L2PcInstance
                recalcHennaStats();

                try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
                    PreparedStatement statement = con.prepareStatement(ADD_CHAR_HENNA);

                    statement.setInt(1, getObjectId());
                    statement.setInt(2, henna.getSymbolId());
                    statement.setInt(3, i + 1);
                    statement.setInt(4, _classIndex);

                    statement.execute();
                    statement.close();
                }
                catch (Exception e) {
                    _log.warning("could not save char henna: " + e);
                }

                sendPacket(new HennaInfo(this));
                sendPacket(new UserInfo(this));
                sendPacket(SystemMessageId.SYMBOL_ADDED);
                return;
            }
        }
    }

    /**
     * Calculate Henna modifiers of this L2PcInstance.
     */
    private void recalcHennaStats() {
        _hennaINT = 0;
        _hennaSTR = 0;
        _hennaCON = 0;
        _hennaMEN = 0;
        _hennaWIT = 0;
        _hennaDEX = 0;

        for (int i = 0; i < 3; i++) {
            if (_henna[i] == null) { continue; }

            _hennaINT += _henna[i].getStatINT();
            _hennaSTR += _henna[i].getStatSTR();
            _hennaMEN += _henna[i].getStatMEN();
            _hennaCON += _henna[i].getStatCON();
            _hennaWIT += _henna[i].getStatWIT();
            _hennaDEX += _henna[i].getStatDEX();
        }

        if (_hennaINT > 5) { _hennaINT = 5; }

        if (_hennaSTR > 5) { _hennaSTR = 5; }

        if (_hennaMEN > 5) { _hennaMEN = 5; }

        if (_hennaCON > 5) { _hennaCON = 5; }

        if (_hennaWIT > 5) { _hennaWIT = 5; }

        if (_hennaDEX > 5) { _hennaDEX = 5; }
    }

    /**
     * @param slot A slot to check.
     * @return the Henna of this L2PcInstance corresponding to the selected slot.
     */
    public Henna getHenna(int slot) {
        if (slot < 1 || slot > 3) { return null; }

        return _henna[slot - 1];
    }

    public int getHennaStatINT() {
        return _hennaINT;
    }

    public int getHennaStatSTR() {
        return _hennaSTR;
    }

    public int getHennaStatCON() {
        return _hennaCON;
    }

    public int getHennaStatMEN() {
        return _hennaMEN;
    }

    public int getHennaStatWIT() {
        return _hennaWIT;
    }

    public int getHennaStatDEX() {
        return _hennaDEX;
    }

    /**
     * Return True if the L2PcInstance is autoAttackable.
     * <ul>
     * <li>Check if the attacker isn't the L2PcInstance Pet</li>
     * <li>Check if the attacker is L2MonsterInstance</li>
     * <li>If the attacker is a L2PcInstance, check if it is not in the same party</li>
     * <li>Check if the L2PcInstance has Karma</li>
     * <li>If the attacker is a L2PcInstance, check if it is not in the same siege clan (Attacker, Defender)</li>
     * </ul>
     */
    @Override
    public boolean isAutoAttackable(L2Character attacker) {
        // Check if the attacker isn't the L2PcInstance Pet
        if (attacker == this || attacker == summon) { return false; }

        // Check if the attacker is a L2MonsterInstance
        if (attacker instanceof L2MonsterInstance) { return true; }

        // Check if the attacker is not in the same party
        if (_party != null && _party.getPartyMembers().contains(attacker)) { return false; }

        // Check if the attacker is a L2Playable
        if (attacker instanceof L2Playable) {
            if (isInsideZone(ZoneId.PEACE)) { return false; }

            // Get L2PcInstance
            L2PcInstance cha = attacker.getActingPlayer();

            // Check if the attacker is in olympiad and olympiad start
            if (attacker instanceof L2PcInstance && cha._inOlympiadMode) {
                return _inOlympiadMode && _OlympiadStart && cha._olympiadGameId == _olympiadGameId;

            }

            // is AutoAttackable if both players are in the same duel and the duel is still going on
            if (_duelState == DuelState.DUELLING && _duelId == cha._duelId) { return true; }

            if (clan != null) {
                Siege siege = SiegeManager.getSiege(getX(), getY(), getZ());
                if (siege != null) {
                    // Check if a siege is in progress and if attacker and the L2PcInstance aren't in the Defender clan
                    if (siege.checkIsDefender(cha.clan) && siege.checkIsDefender(clan)) { return false; }

                    // Check if a siege is in progress and if attacker and the L2PcInstance aren't in the Attacker clan
                    if (siege.checkIsAttacker(cha.clan) && siege.checkIsAttacker(clan)) { return false; }
                }

                // Check if clan is at war
                if (clan.isAtWarWith(cha._clanId) && !wantsPeace() && !cha.wantsPeace() && !isAcademyMember()) {
                    return true;
                }
            }

            // Check if the L2PcInstance is in an arena.
            if (isInArena() && attacker.isInArena()) { return true; }

            // Check if the attacker is not in the same ally.
            if (getAllyId() != 0 && getAllyId() == cha.getAllyId()) { return false; }

            // Check if the attacker is not in the same clan.
            if (clan != null && clan.isMember(cha.getObjectId())) { return false; }

            // Now check again if the L2PcInstance is in pvp zone (as arenas check was made before, it ends with sieges).
            if (isInsideZone(ZoneId.PVP) && attacker.isInsideZone(ZoneId.PVP)) { return true; }
        }
        else if (attacker instanceof L2SiegeGuardInstance) {
            if (clan != null) {
                Siege siege = SiegeManager.getSiege(this);
                return siege != null && siege.checkIsAttacker(clan);
            }
        }

        // Check if the L2PcInstance has Karma
        return _karma > 0 || _pvpFlag > 0;

    }

    /**
     * Check if the active L2Skill can be casted.
     * <ul>
     * <li>Check if the skill isn't toggle and is offensive</li>
     * <li>Check if the target is in the skill cast range</li>
     * <li>Check if the skill is Spoil type and if the target isn't already spoiled</li>
     * <li>Check if the caster owns enought consummed Item, enough HP and MP to cast the skill</li>
     * <li>Check if the caster isn't sitting</li>
     * <li>Check if all skills are enabled and this skill is enabled</li>
     * <li>Check if the caster own the weapon needed</li>
     * <li>Check if the skill is active</li>
     * <li>Check if all casting conditions are completed</li>
     * <li>Notify the AI with CAST and target</li>
     * </ul>
     *
     * @param skill    The L2Skill to use
     * @param forceUse used to force ATTACK on players
     * @param dontMove used to prevent movement, if not in range
     */
    @Override
    public boolean useMagic(L2Skill skill, boolean forceUse, boolean dontMove) {
        // Check if the skill is active
        if (skill.isPassive()) {
            sendPacket(ActionFailed.STATIC_PACKET);
            return false;
        }

        // Cancels the use of skills when player uses a cursed weapon or is flying.
        if ((isCursedWeaponEquipped() && !skill.isDemonicSkill()) // If CW, allow ONLY demonic skills.
                || (_mountType == 1 && !skill.isStriderSkill()) // If mounted, allow ONLY Strider skills.
                || (_mountType == 2 && !skill.isFlyingSkill())) // If flying, allow ONLY Wyvern skills.
        {
            sendPacket(ActionFailed.STATIC_PACKET);
            return false;
        }

        // Players wearing Formal Wear cannot use skills.
        L2ItemInstance formal = inventory.getPaperdollItem(EPaperdollSlot.PAPERDOLL_CHEST);
        if (formal != null && formal.getItem().getBodyPart() == EItemBodyPart.SLOT_ALLDRESS) {
            sendPacket(SystemMessageId.CANNOT_USE_ITEMS_SKILLS_WITH_FORMALWEAR);
            sendPacket(ActionFailed.STATIC_PACKET);
            return false;
        }

        // ************************************* Check Casting in Progress *******************************************

        // If a skill is currently being used, queue this one if this is not the same
        if (isCastingNow()) {
            // Check if new skill different from current skill in progress ; queue it in the player _queuedSkill
            if (_currentSkill.getSkill() != null && skill.getId() != _currentSkill.getSkillId()) {
                setQueuedSkill(skill, forceUse, dontMove);
            }

            sendPacket(ActionFailed.STATIC_PACKET);
            return false;
        }

        setIsCastingNow(true);

        // Set the player _currentSkill.
        setCurrentSkill(skill, forceUse, dontMove);

        // Wipe queued skill.
        if (_queuedSkill.getSkill() != null) { setQueuedSkill(null, false, false); }

        if (!checkUseMagicConditions(skill, forceUse, dontMove)) {
            setIsCastingNow(false);
            return false;
        }

        // Check if the target is correct and Notify the AI with CAST and target
        L2Object target = null;

        switch (skill.getTargetType()) {
            case TARGET_AURA:
            case TARGET_FRONT_AURA:
            case TARGET_BEHIND_AURA:
            case TARGET_GROUND:
            case TARGET_SELF:
            case TARGET_CORPSE_ALLY:
            case TARGET_AURA_UNDEAD:
                target = this;
                break;

            default: // Get the first target of the list
                target = skill.getFirstOfTargetList(this);
                break;
        }

        // Notify the AI with CAST and target
        getAI().setIntention(EIntention.CAST, skill, target);
        return true;
    }

    private boolean checkUseMagicConditions(L2Skill skill, boolean forceUse, boolean dontMove) {
        // ************************************* Check Player State *******************************************

        // Check if the player is dead or out of control.
        if (isDead() || isOutOfControl()) {
            sendPacket(ActionFailed.STATIC_PACKET);
            return false;
        }

        L2SkillType sklType = skill.getSkillType();

        if (isFishing() && sklType != L2SkillType.PUMPING && sklType != L2SkillType.REELING && sklType != L2SkillType.FISHING) {
            // Only fishing skills are available
            sendPacket(SystemMessageId.ONLY_FISHING_SKILLS_NOW);
            return false;
        }

        if (inObserverMode()) {
            sendPacket(SystemMessageId.OBSERVERS_CANNOT_PARTICIPATE);
            abortCast();
            sendPacket(ActionFailed.STATIC_PACKET);
            return false;
        }

        // Check if the caster is sitted. Toggle skills can be only removed, not activated.
        if (_waitTypeSitting) {
            if (skill.isToggle()) {
                // Get effects of the skill
                L2Effect effect = getFirstEffect(skill.getId());
                if (effect != null) {
                    effect.exit();

                    // Send ActionFailed to the L2PcInstance
                    sendPacket(ActionFailed.STATIC_PACKET);
                    return false;
                }
            }

            // Send a System Message to the caster
            sendPacket(SystemMessageId.CANT_MOVE_SITTING);

            // Send ActionFailed to the L2PcInstance
            sendPacket(ActionFailed.STATIC_PACKET);
            return false;
        }

        // Check if the skill type is TOGGLE
        if (skill.isToggle()) {
            // Get effects of the skill
            L2Effect effect = getFirstEffect(skill.getId());

            if (effect != null) {
                // If the toggle is different of FakeDeath, you can de-activate it clicking on it.
                if (skill.getId() != 60) { effect.exit(); }

                // Send ActionFailed to the L2PcInstance
                sendPacket(ActionFailed.STATIC_PACKET);
                return false;
            }
        }

        // Check if the player uses "Fake Death" skill
        if (_isFakeDeath) {
            // Send ActionFailed to the L2PcInstance
            sendPacket(ActionFailed.STATIC_PACKET);
            return false;
        }

        // ************************************* Check Target *******************************************
        // Create and set a L2Object containing the target of the skill
        L2Object target = null;
        ESkillTargetType sklTargetType = skill.getTargetType();
        Location worldPosition = _currentSkillWorldPosition;

        if (sklTargetType == ESkillTargetType.TARGET_GROUND && worldPosition == null) {
            _log.info("WorldPosition is null for skill: " + skill.getName() + ", player: " + getName() + ".");
            sendPacket(ActionFailed.STATIC_PACKET);
            return false;
        }

        switch (sklTargetType) {
            // Target the player if skill type is AURA, PARTY, CLAN or SELF
            case TARGET_AURA:
            case TARGET_FRONT_AURA:
            case TARGET_BEHIND_AURA:
            case TARGET_AURA_UNDEAD:
            case TARGET_PARTY:
            case TARGET_ALLY:
            case TARGET_CLAN:
            case TARGET_GROUND:
            case TARGET_SELF:
            case TARGET_CORPSE_ALLY:
            case TARGET_AREA_SUMMON:
                target = this;
                break;
            case TARGET_PET:
            case TARGET_SUMMON:
                target = summon;
                break;
            default:
                target = getTarget();
                break;
        }

        // Check the validity of the target
        if (target == null) {
            sendPacket(ActionFailed.STATIC_PACKET);
            return false;
        }

        if (target instanceof L2DoorInstance) {
            if (!((L2DoorInstance) target).isAttackable(this) // Siege doors only hittable during siege
                    || (((L2DoorInstance) target).isUnlockable() && skill.getSkillType() != L2SkillType.UNLOCK)) // unlockable doors
            {
                sendPacket(SystemMessageId.INCORRECT_TARGET);
                sendPacket(ActionFailed.STATIC_PACKET);
                return false;
            }
        }

        // Are the target and the player in the same duel?
        if (_isInDuel) {
            if (target instanceof L2Playable) {
                // Get L2PcInstance
                L2PcInstance cha = target.getActingPlayer();
                if (cha._duelId != _duelId) {
                    sendPacket(SystemMessageId.INCORRECT_TARGET);
                    sendPacket(ActionFailed.STATIC_PACKET);
                    return false;
                }
            }
        }

        // ************************************* Check skill availability *******************************************

        // Siege summon checks. Both checks send a message to the player if it return false.
        if (skill.isSiegeSummonSkill() && (!SiegeManager.checkIfOkToSummon(this) || !SevenSigns.getInstance().checkSummonConditions(this))) {
            return false;
        }

        // Check if this skill is enabled (ex : reuse time)
        if (isSkillDisabled(skill)) {
            sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_PREPARED_FOR_REUSE).addSkillName(skill));
            return false;
        }

        // ************************************* Check casting conditions *******************************************

        // Check if all casting conditions are completed
        if (!skill.checkCondition(this, target, false)) {
            // Send ActionFailed to the L2PcInstance
            sendPacket(ActionFailed.STATIC_PACKET);
            return false;
        }

        // ************************************* Check Skill Type *******************************************

        // Check if this is offensive magic skill
        if (skill.isOffensive()) {
            if (isInsidePeaceZone(this, target)) {
                // If L2Character or target is in a peace zone, send a system message TARGET_IN_PEACEZONE ActionFailed
                sendPacket(SystemMessageId.TARGET_IN_PEACEZONE);
                sendPacket(ActionFailed.STATIC_PACKET);
                return false;
            }

            if (_inOlympiadMode && !_OlympiadStart) {
                // if L2PcInstance is in Olympia and the match isn't already start, send ActionFailed
                sendPacket(ActionFailed.STATIC_PACKET);
                return false;
            }

            // Check if the target is attackable
            if (!target.isAttackable() && !getAccessLevel().allowPeaceAttack()) {
                // If target is not attackable, send ActionFailed
                sendPacket(ActionFailed.STATIC_PACKET);
                return false;
            }

            // Check if a Forced ATTACK is in progress on non-attackable target
            if (!target.isAutoAttackable(this) && !forceUse) {
                switch (sklTargetType) {
                    case TARGET_AURA:
                    case TARGET_FRONT_AURA:
                    case TARGET_BEHIND_AURA:
                    case TARGET_AURA_UNDEAD:
                    case TARGET_CLAN:
                    case TARGET_ALLY:
                    case TARGET_PARTY:
                    case TARGET_SELF:
                    case TARGET_GROUND:
                    case TARGET_CORPSE_ALLY:
                    case TARGET_AREA_SUMMON:
                        break;
                    default: // Send ActionFailed to the L2PcInstance
                        sendPacket(ActionFailed.STATIC_PACKET);
                        return false;
                }
            }

            // Check if the target is in the skill cast range
            if (dontMove) {
                // Calculate the distance between the L2PcInstance and the target
                if (sklTargetType == ESkillTargetType.TARGET_GROUND) {
                    if (!isInsideRadius(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), skill.getCastRange() + getTemplate().getCollisionRadius(), false, false)) {
                        // Send a System Message to the caster
                        sendPacket(SystemMessageId.TARGET_TOO_FAR);

                        // Send ActionFailed to the L2PcInstance
                        sendPacket(ActionFailed.STATIC_PACKET);
                        return false;
                    }
                }
                else if (skill.getCastRange() > 0 && !isInsideRadius(target, skill.getCastRange() + getTemplate().getCollisionRadius(), false, false)) {
                    // Send a System Message to the caster
                    sendPacket(SystemMessageId.TARGET_TOO_FAR);

                    // Send ActionFailed to the L2PcInstance
                    sendPacket(ActionFailed.STATIC_PACKET);
                    return false;
                }
            }
        }

        // Check if the skill is defensive
        if (!skill.isOffensive() && target instanceof L2MonsterInstance && !forceUse) {
            // check if the target is a monster and if force attack is set.. if not then we don't want to cast.
            switch (sklTargetType) {
                case TARGET_PET:
                case TARGET_SUMMON:
                case TARGET_AURA:
                case TARGET_FRONT_AURA:
                case TARGET_BEHIND_AURA:
                case TARGET_AURA_UNDEAD:
                case TARGET_CLAN:
                case TARGET_SELF:
                case TARGET_CORPSE_ALLY:
                case TARGET_PARTY:
                case TARGET_ALLY:
                case TARGET_CORPSE_MOB:
                case TARGET_AREA_CORPSE_MOB:
                case TARGET_GROUND:
                    break;
                default:
                    switch (sklType) {
                        case BEAST_FEED:
                        case DELUXE_KEY_UNLOCK:
                        case UNLOCK:
                            break;
                        default:
                            sendPacket(ActionFailed.STATIC_PACKET);
                            return false;
                    }
                    break;
            }
        }

        // Check if the skill is Spoil type and if the target isn't already spoiled
        if (sklType == L2SkillType.SPOIL) {
            if (!(target instanceof L2MonsterInstance)) {
                // Send a System Message to the L2PcInstance
                sendPacket(SystemMessageId.INCORRECT_TARGET);

                // Send ActionFailed to the L2PcInstance
                sendPacket(ActionFailed.STATIC_PACKET);
                return false;
            }
        }

        // Check if the skill is Sweep type and if conditions not apply
        if (sklType == L2SkillType.SWEEP && target instanceof L2Attackable) {
            if (((L2Attackable) target).isDead()) {
                int spoilerId = ((L2Attackable) target).getSpoilerId();
                if (spoilerId == 0) {
                    // Send a System Message to the L2PcInstance
                    sendPacket(SystemMessageId.SWEEPER_FAILED_TARGET_NOT_SPOILED);

                    // Send ActionFailed to the L2PcInstance
                    sendPacket(ActionFailed.STATIC_PACKET);
                    return false;
                }

                if (getObjectId() != spoilerId && !isInLooterParty(spoilerId)) {
                    // Send a System Message to the L2PcInstance
                    sendPacket(SystemMessageId.SWEEP_NOT_ALLOWED);

                    // Send ActionFailed to the L2PcInstance
                    sendPacket(ActionFailed.STATIC_PACKET);
                    return false;
                }
            }
        }

        // Check if the skill is Drain Soul (Soul Crystals) and if the target is a MOB
        if (sklType == L2SkillType.DRAIN_SOUL) {
            if (!(target instanceof L2MonsterInstance)) {
                // Send a System Message to the L2PcInstance
                sendPacket(SystemMessageId.INCORRECT_TARGET);

                // Send ActionFailed to the L2PcInstance
                sendPacket(ActionFailed.STATIC_PACKET);
                return false;
            }
        }

        // Check if this is a Pvp skill and target isn't a non-flagged/non-karma player
        switch (sklTargetType) {
            case TARGET_PARTY:
            case TARGET_ALLY: // For such skills, checkPvpSkill() is called from L2Skill.getTargetList()
            case TARGET_CLAN: // For such skills, checkPvpSkill() is called from L2Skill.getTargetList()
            case TARGET_AURA:
            case TARGET_FRONT_AURA:
            case TARGET_BEHIND_AURA:
            case TARGET_AURA_UNDEAD:
            case TARGET_GROUND:
            case TARGET_SELF:
            case TARGET_CORPSE_ALLY:
                break;
            default:
                if (!checkPvpSkill(target, skill) && !getAccessLevel().allowPeaceAttack()) {
                    // Send a System Message to the L2PcInstance
                    sendPacket(SystemMessageId.TARGET_IS_INCORRECT);

                    // Send ActionFailed to the L2PcInstance
                    sendPacket(ActionFailed.STATIC_PACKET);
                    return false;
                }
        }

        if ((sklTargetType == ESkillTargetType.TARGET_HOLY && !checkIfOkToCastSealOfRule(CastleManager.getInstance().getCastle(this), false, skill, target)) || (sklType == L2SkillType.SIEGEFLAG && !L2SkillSiegeFlag.checkIfOkToPlaceFlag(this, false)) || (sklType == L2SkillType.STRSIEGEASSAULT && !checkIfOkToUseStriderSiegeAssault(skill)) || (sklType == L2SkillType.SUMMON_FRIEND && !(checkSummonerStatus(this) && checkSummonTargetStatus(target, this)))) {
            sendPacket(ActionFailed.STATIC_PACKET);
            abortCast();
            return false;
        }

        // GeoData Los Check here
        if (skill.getCastRange() > 0) {
            if (sklTargetType == ESkillTargetType.TARGET_GROUND) {
                if (!PathFinding.getInstance().canSeeTarget(this, worldPosition)) {
                    sendPacket(SystemMessageId.CANT_SEE_TARGET);
                    sendPacket(ActionFailed.STATIC_PACKET);
                    return false;
                }
            }
            else if (!PathFinding.getInstance().canSeeTarget(this, target)) {
                sendPacket(SystemMessageId.CANT_SEE_TARGET);
                sendPacket(ActionFailed.STATIC_PACKET);
                return false;
            }
        }
        // finally, after passing all conditions
        return true;
    }

    public boolean checkIfOkToUseStriderSiegeAssault(L2Skill skill) {
        SystemMessage sm;
        Castle castle = CastleManager.getInstance().getCastle(this);

        if (!isRiding()) {
            sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addSkillName(skill);
        }
        else if (!(getTarget() instanceof L2DoorInstance)) {
            sm = SystemMessage.getSystemMessage(SystemMessageId.INCORRECT_TARGET);
        }
        else if (castle == null || castle.getCastleId() <= 0) {
            sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addSkillName(skill);
        }
        else if (!castle.getSiege().isInProgress() || castle.getSiege().getAttackerClan(clan) == null) {
            sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addSkillName(skill);
        }
        else {
            return true;
        }

        sendPacket(sm);
        return false;
    }

    public boolean checkIfOkToCastSealOfRule(Castle castle, boolean isCheckOnly, L2Skill skill, L2Object target) {
        SystemMessage sm;

        if (castle == null || castle.getCastleId() <= 0) {
            sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addSkillName(skill);
        }
        else if (!castle.getArtefacts().contains(target)) {
            sm = SystemMessage.getSystemMessage(SystemMessageId.INCORRECT_TARGET);
        }
        else if (!castle.getSiege().isInProgress()) {
            sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addSkillName(skill);
        }
        else if (!Util.checkIfInRange(200, this, target, true)) {
            sm = SystemMessage.getSystemMessage(SystemMessageId.DIST_TOO_FAR_CASTING_STOPPED);
        }
        else if (!isInsideZone(ZoneId.CAST_ON_ARTIFACT)) {
            sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addSkillName(skill);
        }
        else if (castle.getSiege().getAttackerClan(clan) == null) {
            sm = SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addSkillName(skill);
        }
        else {
            if (!isCheckOnly) {
                sm = SystemMessage.getSystemMessage(SystemMessageId.OPPONENT_STARTED_ENGRAVING);
                castle.getSiege().announceToPlayer(sm, false);
            }
            return true;
        }
        sendPacket(sm);
        return false;
    }

    public boolean isInLooterParty(int LooterId) {
        L2PcInstance looter = L2World.getInstance().getPlayer(LooterId);

        // if L2PcInstance is in a CommandChannel
        if (isInParty() && _party.isInCommandChannel() && looter != null) {
            return _party.getCommandChannel().getMembers().contains(looter);
        }

        if (isInParty() && looter != null) {
            return _party.getPartyMembers().contains(looter);
        }

        return false;
    }

    public boolean checkPvpSkill(L2Object target, L2Skill skill) {
        if (skill == null || target == null) { return false; }

        if (!(target instanceof L2Playable)) { return true; }

        if (skill.isDebuff() || skill.isOffensive()) {
            L2PcInstance targetPlayer = target.getActingPlayer();
            if (targetPlayer == null || this == target) { return false; }

            // Peace Zone
            if (target.isInsideZone(ZoneId.PEACE)) { return false; }

            // Duel
            if (_isInDuel && targetPlayer._isInDuel && _duelId == targetPlayer._duelId) {
                return true;
            }

            boolean isCtrlPressed = _currentSkill != null && _currentSkill.isCtrlPressed();

            // Party
            if (isInParty() && targetPlayer.isInParty()) {
                if (_party.getLeader() == targetPlayer._party.getLeader()) {
                    return skill.getEffectRange() > 0 && isCtrlPressed && getTarget() == target && skill.getSkillType().isDamage();
                }
                else if (_party.getCommandChannel() != null && _party.getCommandChannel().containsPlayer(targetPlayer)) {
                    return skill.getEffectRange() > 0 && isCtrlPressed && getTarget() == target && skill.getSkillType().isDamage();
                }
            }

            // You can debuff anyone except party members while in an arena...
            if (isInsideZone(ZoneId.PVP) && targetPlayer.isInsideZone(ZoneId.PVP)) { return true; }

            // Olympiad
            if (_inOlympiadMode && targetPlayer._inOlympiadMode && _olympiadGameId == targetPlayer._olympiadGameId) {
                return true;
            }

            if (clan != null && targetPlayer.clan != null) {
                if (clan.isAtWarWith(targetPlayer.clan.getClanId()) && targetPlayer.clan.isAtWarWith(clan.getClanId())) {
                    if (skill.getEffectRange() > 0 && isCtrlPressed && getTarget() == target && skill.getTargetType().isAoeSkill()) {
                        return true;
                    }
                    return isCtrlPressed;
                }
                else if (_clanId == targetPlayer._clanId || (getAllyId() > 0 && getAllyId() == targetPlayer.getAllyId())) {
                    return skill.getEffectRange() > 0 && isCtrlPressed && getTarget() == target && skill.getSkillType().isDamage();
                }
            }

            if (targetPlayer._pvpFlag == 0 && targetPlayer._karma == 0) {
                return skill.getEffectRange() > 0 && isCtrlPressed && getTarget() == target && skill.getSkillType().isDamage();
            }

            return targetPlayer._pvpFlag > 0 || targetPlayer._karma > 0;

        }
        return true;
    }

    public boolean isMageClass() {
        return getClassId().isMage();
    }

    public boolean isMounted() {
        return _mountType > 0;
    }

    /**
     * This method allows to :
     * <ul>
     * <li>change isRiding/isFlying flags</li>
     * <li>gift player with Wyvern Breath skill if mount is a wyvern</li>
     * <li>send the skillList (faded icons update)</li>
     * </ul>
     *
     * @param npcId     the npcId of the mount
     * @param npcLevel  The level of the mount
     * @param mountType 0, 1 or 2 (dismount, strider or wyvern).
     * @return always true.
     */
    public boolean setMount(int npcId, int npcLevel, int mountType) {
        switch (mountType) {
            case 0: // Dismounted
                if (isFlying()) { removeSkill(FrequentSkill.WYVERN_BREATH.getSkill()); }
                break;

            case 2: // Flying Wyvern
                addSkill(FrequentSkill.WYVERN_BREATH.getSkill(), false); // not saved to DB
                break;
        }

        _mountNpcId = npcId;
        _mountType = mountType;
        _mountLevel = npcLevel;

        sendSkillList(); // Update faded icons && eventual added skills.
        return true;
    }

    @Override
    public boolean isSeated() {
        return _mountObjectID > 0;
    }

    @Override
    public boolean isRiding() {
        return _mountType == 1;
    }

    @Override
    public boolean isFlying() {
        return _mountType == 2;
    }

    /**
     * @return the type of Pet mounted (0 : none, 1 : Strider, 2 : Wyvern).
     */
    public int getMountType() {
        return _mountType;
    }

    @Override
    public void stopAllEffects() {
        super.stopAllEffects();
        updateAndBroadcastStatus(2);
    }

    @Override
    public void stopAllEffectsExceptThoseThatLastThroughDeath() {
        super.stopAllEffectsExceptThoseThatLastThroughDeath();
        updateAndBroadcastStatus(2);
    }

    public void stopAllToggles() {
        _effects.stopAllToggles();
    }

    public void stopCubics() {
        if (_cubics != null) {
            boolean removed = false;
            for (L2CubicInstance cubic : _cubics.values()) {
                cubic.stopAction();
                delCubic(cubic.getId());
                removed = true;
            }
            if (removed) { broadcastUserInfo(); }
        }
    }

    public void stopCubicsByOthers() {
        if (_cubics != null) {
            boolean removed = false;
            for (L2CubicInstance cubic : _cubics.values()) {
                if (cubic.givenByOther()) {
                    cubic.stopAction();
                    delCubic(cubic.getId());
                    removed = true;
                }
            }
            if (removed) { broadcastUserInfo(); }
        }
    }

    /**
     * Send UserInfo to this L2PcInstance and CharInfo to all L2PcInstance in its _KnownPlayers.<BR>
     * <ul>
     * <li>Send UserInfo to this L2PcInstance (Public and Private Data)</li>
     * <li>Send CharInfo to all L2PcInstance in _KnownPlayers of the L2PcInstance (Public data only)</li>
     * </ul>
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : DON'T SEND UserInfo packet to other players instead of CharInfo packet. Indeed, UserInfo packet contains PRIVATE DATA as MaxHP, STR, DEX...</B></FONT><BR>
     * <BR>
     */
    @Override
    public void updateAbnormalEffect() {
        broadcastUserInfo();
    }

    /**
     * Disable the Inventory and create a new task to enable it after 1.5s.
     */
    public void tempInventoryDisable() {
        _inventoryDisable = true;

        ThreadPoolManager.getInstance().scheduleGeneral(new InventoryEnable(), 1500);
    }

    /**
     * @return True if the Inventory is disabled.
     */
    public boolean isInventoryDisabled() {
        return _inventoryDisable;
    }

    public Map<Integer, L2CubicInstance> getCubics() {
        return _cubics;
    }

    /**
     * Add a L2CubicInstance to the L2PcInstance _cubics.
     *
     * @param id
     * @param level
     * @param matk
     * @param activationtime
     * @param activationchance
     * @param totalLifetime
     * @param givenByOther
     */
    public void addCubic(int id, int level, double matk, int activationtime, int activationchance, int totalLifetime, boolean givenByOther) {
        _cubics.put(id, new L2CubicInstance(this, id, level, (int) matk, activationtime, activationchance, totalLifetime, givenByOther));
    }

    /**
     * Remove a L2CubicInstance from the L2PcInstance _cubics.
     *
     * @param id
     */
    public void delCubic(int id) {
        _cubics.remove(id);
    }

    /**
     * @param id
     * @return the L2CubicInstance corresponding to the Identifier of the L2PcInstance _cubics.
     */
    public L2CubicInstance getCubic(int id) {
        return _cubics.get(id);
    }

    @Override
    public String toString() {
        return "player " + getName();
    }

    /**
     * @return the modifier corresponding to the Enchant Effect of the Active Weapon (Min : 127).
     */
    public int getEnchantEffect() {
        L2ItemInstance wpn = getActiveWeaponInstance();

        if (wpn == null) { return 0; }

        return Math.min(127, wpn.getEnchantLevel());
    }

    /**
     * @return the _currentFolkNpc of the player.
     */
    public L2Npc getCurrentFolkNPC() {
        return _currentFolkNpc;
    }

    /**
     * Set the _currentFolkNpc of the player.
     *
     * @param npc
     */
    public void setCurrentFolkNPC(L2Npc npc) {
        _currentFolkNpc = npc;
    }

    /**
     * @return True if L2PcInstance is a participant in the Festival of Darkness.
     */
    public boolean isFestivalParticipant() {
        return SevenSignsFestival.getInstance().isParticipant(this);
    }

    public void addAutoSoulShot(int itemId) {
        _activeSoulShots.add(itemId);
    }

    public boolean removeAutoSoulShot(int itemId) {
        return _activeSoulShots.remove(itemId);
    }

    public Set<Integer> getAutoSoulShot() {
        return _activeSoulShots;
    }

    @Override
    public boolean isChargedShot(ShotType type) {
        L2ItemInstance weapon = getActiveWeaponInstance();
        return weapon != null && weapon.isChargedShot(type);
    }

    @Override
    public void setChargedShot(ShotType type, boolean charged) {
        L2ItemInstance weapon = getActiveWeaponInstance();
        if (weapon != null) { weapon.setChargedShot(type, charged); }
    }

    @Override
    public void rechargeShots(boolean physical, boolean magic) {
        if (_activeSoulShots.isEmpty()) { return; }

        for (int itemId : _activeSoulShots) {
            L2ItemInstance item = inventory.getItemByItemId(itemId);
            if (item != null) {
                if (magic && item.getItem().getDefaultAction() == ActionType.spiritshot) {
                    IItemHandler handler = ItemHandler.getInstance().getItemHandler(item.getEtcItem());
                    if (handler != null) { handler.useItem(this, item, false); }
                }

                if (physical && item.getItem().getDefaultAction() == ActionType.soulshot) {
                    IItemHandler handler = ItemHandler.getInstance().getItemHandler(item.getEtcItem());
                    if (handler != null) { handler.useItem(this, item, false); }
                }
            }
            else { removeAutoSoulShot(itemId); }
        }
    }

    /**
     * Cancel autoshot use for shot itemId
     *
     * @param itemId int id to disable
     * @return true if canceled.
     */
    public boolean disableAutoShot(int itemId) {
        if (_activeSoulShots.contains(itemId)) {
            removeAutoSoulShot(itemId);
            sendPacket(new ExAutoSoulShot(itemId, 0));
            sendPacket(SystemMessage.getSystemMessage(SystemMessageId.AUTO_USE_OF_S1_CANCELLED).addItemName(itemId));
            return true;
        }

        return false;
    }

    /**
     * Cancel all autoshots for player
     */
    public void disableAutoShotsAll() {
        for (int itemId : _activeSoulShots) {
            sendPacket(new ExAutoSoulShot(itemId, 0));
            sendPacket(SystemMessage.getSystemMessage(SystemMessageId.AUTO_USE_OF_S1_CANCELLED).addItemName(itemId));
        }
        _activeSoulShots.clear();
    }

    public int getClanPrivileges() {
        return _clanPrivileges;
    }

    public void setClanPrivileges(int n) {
        _clanPrivileges = n;
    }

    public int getPledgeClass() {
        return _pledgeClass;
    }

    // baron etc
    public void setPledgeClass(int classId) {
        _pledgeClass = classId;
    }

    public int getPledgeType() {
        return _pledgeType;
    }

    public void setPledgeType(int typeId) {
        _pledgeType = typeId;
    }

    public int getApprentice() {
        return _apprentice;
    }

    public void setApprentice(int apprentice_id) {
        _apprentice = apprentice_id;
    }

    public int getSponsor() {
        return _sponsor;
    }

    public void setSponsor(int sponsor_id) {
        _sponsor = sponsor_id;
    }

    @Override
    public void sendMessage(String message) {
        sendPacket(SystemMessage.sendString(message));
    }

    /**
     * Unsummon all types of summons : pets, cubics, normal summons and trained beasts.
     */
    public void dropAllSummons() {
        // Delete summons and pets
        if (summon != null) {
            summon.unSummon(this);
        }

        // Delete trained beasts
        if (_tamedBeast != null) {
            _tamedBeast.deleteMe();
        }

        // Delete any form of cubics
        stopCubics();
    }

    public void enterObserverMode(int x, int y, int z) {
        _savedLocation.setXYZ(getX(), getY(), getZ());
        _observerMode = true;

        standUp();

        dropAllSummons();
        setTarget(null);
        setIsParalyzed(true);
        startParalyze();
        setIsInvul(true);
        appearance.setInvisible();

        sendPacket(new ObservationMode(x, y, z));
        getKnownList().removeAllKnownObjects(); // reinit knownlist
        setXYZ(x, y, z);

        broadcastUserInfo();
    }

    public void enterOlympiadObserverMode(int id) {
        OlympiadGameTask task = OlympiadGameManager.getInstance().getOlympiadTask(id);
        if (task == null) { return; }

        dropAllSummons();

        if (_party != null) {
            _party.removePartyMember(this, MessageType.Expelled);
        }

        _olympiadGameId = id;

        standUp();

        if (!_observerMode) { _savedLocation.setXYZ(getX(), getY(), getZ()); }

        _observerMode = true;
        setTarget(null);
        setIsInvul(true);
        appearance.setInvisible();
        teleToLocation(task.getZone().getSpawns().get(2), 0);
        sendPacket(new ExOlympiadMode(3));
        broadcastUserInfo();
    }

    public void leaveObserverMode() {
        setTarget(null);
        getKnownList().removeAllKnownObjects(); // reinit knownlist
        setXYZ(_savedLocation.getX(), _savedLocation.getY(), _savedLocation.getZ());
        setIsParalyzed(false);
        stopParalyze(false);
        appearance.setVisible();
        setIsInvul(false);

        if (hasAI()) { getAI().setIntention(EIntention.IDLE); }

        // prevent receive falling damage
        setFalling();

        _observerMode = false;
        _savedLocation.setXYZ(getX(), getY(), getZ());
        sendPacket(new ObservationReturn(_savedLocation));
        broadcastUserInfo();
    }

    public void leaveOlympiadObserverMode() {
        if (_olympiadGameId == -1) { return; }

        _olympiadGameId = -1;
        _observerMode = false;

        setTarget(null);
        sendPacket(new ExOlympiadMode(0));
        teleToLocation(_savedLocation, 20);
        appearance.setVisible();
        setIsInvul(false);

        if (hasAI()) { getAI().setIntention(EIntention.IDLE); }

        _savedLocation.setXYZ(getX(), getY(), getZ());
        broadcastUserInfo();
    }

    public int getOlympiadSide() {
        return _olympiadSide;
    }

    public void setOlympiadSide(int i) {
        _olympiadSide = i;
    }

    public int getOlympiadGameId() {
        return _olympiadGameId;
    }

    public void setOlympiadGameId(int id) {
        _olympiadGameId = id;
    }

    public Location getSavedLocation() {
        return _savedLocation;
    }

    public boolean inObserverMode() {
        return _observerMode;
    }

    public int getTeleMode() {
        return _telemode;
    }

    public void setTeleMode(int mode) {
        _telemode = mode;
    }

    public void setLoto(int i, int val) {
        _loto[i] = val;
    }

    public int getLoto(int i) {
        return _loto[i];
    }

    public void setRace(int i, int val) {
        _race[i] = val;
    }

    public int getRace(int i) {
        return _race[i];
    }

    public boolean isInRefusalMode() {
        return _messageRefusal;
    }

    public void setInRefusalMode(boolean mode) {
        _messageRefusal = mode;
        sendPacket(new EtcStatusUpdate(this));
    }

    public boolean getTradeRefusal() {
        return _tradeRefusal;
    }

    public void setTradeRefusal(boolean mode) {
        _tradeRefusal = mode;
    }

    public boolean getExchangeRefusal() {
        return _exchangeRefusal;
    }

    public void setExchangeRefusal(boolean mode) {
        _exchangeRefusal = mode;
    }

    public BlockList getBlockList() {
        return _blockList;
    }

    public void setIsInOlympiadMode(boolean b) {
        _inOlympiadMode = b;
    }

    public void setIsOlympiadStart(boolean b) {
        _OlympiadStart = b;
    }

    public boolean isOlympiadStart() {
        return _OlympiadStart;
    }

    public boolean isHero() {
        return _hero;
    }

    public void setHero(boolean hero) {
        if (hero && _baseClass == _activeClass) {
            for (L2Skill s : SkillTable.getHeroSkills()) {
                addSkill(s, false); // Dont Save Hero skills to database
            }
        }
        else {
            for (L2Skill s : SkillTable.getHeroSkills()) {
                super.removeSkill(s); // Just Remove skills from nonHero characters
            }
        }
        _hero = hero;

        sendSkillList();
    }

    public boolean isInOlympiadMode() {
        return _inOlympiadMode;
    }

    public boolean isInDuel() {
        return _isInDuel;
    }

    /**
     * Sets up the duel state using a non 0 duelId.
     *
     * @param duelId 0=not in a duel
     */
    public void setInDuel(int duelId) {
        if (duelId > 0) {
            _isInDuel = true;
            _duelState = DuelState.DUELLING;
            _duelId = duelId;
        }
        else {
            if (_duelState == DuelState.DEAD) {
                enableAllSkills();
                getStatus().startHpMpRegeneration();
            }
            _isInDuel = false;
            _duelState = DuelState.NO_DUEL;
            _duelId = 0;
        }
    }

    public int getDuelId() {
        return _duelId;
    }

    public DuelState getDuelState() {
        return _duelState;
    }

    public void setDuelState(DuelState state) {
        _duelState = state;
    }

    /**
     * This returns a SystemMessage stating why the player is not available for duelling.
     *
     * @return S1_CANNOT_DUEL... message
     */
    public SystemMessage getNoDuelReason() {
        SystemMessage sm = SystemMessage.getSystemMessage(_noDuelReason);
        sm.addPcName(this);
        _noDuelReason = SystemMessageId.THERE_IS_NO_OPPONENT_TO_RECEIVE_YOUR_CHALLENGE_FOR_A_DUEL;
        return sm;
    }

    /**
     * Checks if this player might join / start a duel. To get the reason use getNoDuelReason() after calling this function.
     *
     * @return true if the player might join/start a duel.
     */
    public boolean canDuel() {
        if (isInCombat() || _punishLevel == PunishLevel.JAIL) {
            _noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_CURRENTLY_ENGAGED_IN_BATTLE;
        }
        else if (isDead() || isAlikeDead() || getCurrentHp() < getMaxHp() / 2 || getCurrentMp() < getMaxMp() / 2) {
            _noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_HP_OR_MP_IS_BELOW_50_PERCENT;
        }
        else if (_isInDuel) { _noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_ALREADY_ENGAGED_IN_A_DUEL; }
        else if (_inOlympiadMode) {
            _noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_PARTICIPATING_IN_THE_OLYMPIAD;
        }
        else if (isCursedWeaponEquipped() || _karma != 0) {
            _noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_IN_A_CHAOTIC_STATE;
        }
        else if (isInStoreMode()) {
            _noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_CURRENTLY_ENGAGED_IN_A_PRIVATE_STORE_OR_MANUFACTURE;
        }
        else if (isMounted() || isInBoat()) {
            _noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_CURRENTLY_RIDING_A_BOAT_WYVERN_OR_STRIDER;
        }
        else if (isFishing()) { _noDuelReason = SystemMessageId.S1_CANNOT_DUEL_BECAUSE_S1_IS_CURRENTLY_FISHING; }
        else if (isInsideZone(ZoneId.PVP) || isInsideZone(ZoneId.PEACE) || isInsideZone(ZoneId.SIEGE)) {
            _noDuelReason = SystemMessageId.S1_CANNOT_MAKE_A_CHALLANGE_TO_A_DUEL_BECAUSE_S1_IS_CURRENTLY_IN_A_DUEL_PROHIBITED_AREA;
        }
        else { return true; }

        return false;
    }

    public boolean isNoble() {
        return _noble;
    }

    /**
     * Set Noblesse Status, and reward with nobles' skills.
     *
     * @param val   Add skills if setted to true, else remove skills.
     * @param store Store the status directly in the db if setted to true.
     */
    public void setNoble(boolean val, boolean store) {
        if (val) {
            for (L2Skill s : SkillTable.getNobleSkills()) {
                addSkill(s, false); // Dont Save Noble skills to Sql
            }
        }
        else {
            for (L2Skill s : SkillTable.getNobleSkills()) {
                super.removeSkill(s); // Just Remove skills without deleting from Sql
            }
        }

        _noble = val;

        sendSkillList();

        if (store) {
            try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
                PreparedStatement statement = con.prepareStatement(UPDATE_NOBLESS);
                statement.setBoolean(1, val);
                statement.setInt(2, getObjectId());
                statement.executeUpdate();
                statement.close();
            }
            catch (Exception e) {
                _log.log(Level.WARNING, "Could not update " + getName() + " nobless status: " + e.getMessage(), e);
            }
        }
    }

    public int getLvlJoinedAcademy() {
        return _lvlJoinedAcademy;
    }

    public void setLvlJoinedAcademy(int lvl) {
        _lvlJoinedAcademy = lvl;
    }

    public boolean isAcademyMember() {
        return _lvlJoinedAcademy > 0;
    }

    public int getTeam() {
        return _team;
    }

    public void setTeam(int team) {
        _team = team;
    }

    public void setWantsPeace(boolean wantsPeace) {
        _wantsPeace = wantsPeace;
    }

    public boolean wantsPeace() {
        return _wantsPeace;
    }

    public boolean isFishing() {
        return _fishingLoc != null;
    }

    /**
     * [-5,-1] varka, 0 neutral, [1,5] ketra
     *
     * @return the side faction.
     */
    public int getAllianceWithVarkaKetra() {
        return _alliedVarkaKetra;
    }

    public void setAllianceWithVarkaKetra(int sideAndLvlOfAlliance) {
        _alliedVarkaKetra = sideAndLvlOfAlliance;
    }

    public boolean isAlliedWithVarka() {
        return _alliedVarkaKetra < 0;
    }

    public boolean isAlliedWithKetra() {
        return _alliedVarkaKetra > 0;
    }

    public void sendSkillList() {
        L2ItemInstance formal = inventory.getPaperdollItem(EPaperdollSlot.PAPERDOLL_CHEST);
        boolean isWearingFormalWear = formal != null && formal.getItem().getBodyPart() == EItemBodyPart.SLOT_ALLDRESS;

        boolean isDisabled = false;
        SkillList sl = new SkillList();
        for (L2Skill s : getAllSkills()) {
            if (s == null) { continue; }

            if (s.getId() > 9000 && s.getId() < 9007) {
                continue; // Fake skills to change base stats
            }

            if (clan != null) {
                isDisabled = s.isClanSkill() && clan.getReputationScore() < 0;
            }

            if (isCursedWeaponEquipped()) // Only Demonic skills are available
            { isDisabled = !s.isDemonicSkill(); }
            else if (isMounted()) // else if, because only ONE state is possible
            {
                if (_mountType == 1) // Only Strider skills are available
                { isDisabled = !s.isStriderSkill(); }
                else if (_mountType == 2) // Only Wyvern skills are available
                { isDisabled = !s.isFlyingSkill(); }
            }

            if (isWearingFormalWear) { isDisabled = true; }

            sl.addSkill(s.getId(), s.getLevel(), s.isPassive(), isDisabled);
        }
        sendPacket(sl);
    }

    /**
     * 1. Add the specified class ID as a subclass (up to the maximum number of <b>three</b>) for this character.<BR>
     * 2. This method no longer changes the active _classIndex of the player. This is only done by the calling of setActiveClass() method as that should be the only way to do so.
     *
     * @param classId
     * @param classIndex
     * @return boolean subclassAdded
     */
    public boolean addSubClass(int classId, int classIndex) {
        if (!_subclassLock.tryLock()) { return false; }

        try {
            if (getTotalSubClasses() == 3 || classIndex == 0) { return false; }

            if (_subClasses.containsKey(classIndex)) { return false; }

            // Note: Never change _classIndex in any method other than setActiveClass().

            SubClass newClass = new SubClass();
            newClass.setClassId(classId);
            newClass.setClassIndex(classIndex);

            try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
                PreparedStatement statement = con.prepareStatement(ADD_CHAR_SUBCLASS);
                statement.setInt(1, getObjectId());
                statement.setInt(2, newClass.getClassId());
                statement.setLong(3, newClass.getExp());
                statement.setInt(4, newClass.getSp());
                statement.setInt(5, newClass.getLevel());
                statement.setInt(6, newClass.getClassIndex()); // <-- Added

                statement.execute();
                statement.close();
            }
            catch (Exception e) {
                _log.warning("WARNING: Could not add character sub class for " + getName() + ": " + e);
                return false;
            }

            // Commit after database INSERT incase exception is thrown.
            _subClasses.put(newClass.getClassIndex(), newClass);

            ClassId subTemplate = ClassId.values()[classId];
            Collection<L2SkillLearn> skillTree = SkillTreeTable.getInstance().getAllowedSkills(subTemplate);

            if (skillTree == null) { return true; }

            Map<Integer, L2Skill> prevSkillList = new LinkedHashMap<>();

            for (L2SkillLearn skillInfo : skillTree) {
                if (skillInfo.getMinLevel() <= 40) {
                    L2Skill prevSkill = prevSkillList.get(skillInfo.getId());
                    L2Skill newSkill = SkillTable.getInfo(skillInfo.getId(), skillInfo.getLevel());

                    if (prevSkill != null && (prevSkill.getLevel() > newSkill.getLevel())) { continue; }

                    prevSkillList.put(newSkill.getId(), newSkill);
                    storeSkill(newSkill, prevSkill, classIndex);
                }
            }

            return true;
        }
        finally {
            _subclassLock.unlock();
        }
    }

    /**
     * 1. Completely erase all existance of the subClass linked to the classIndex.<BR>
     * 2. Send over the newClassId to addSubClass()to create a new instance on this classIndex.<BR>
     * 3. Upon Exception, revert the player to their BaseClass to avoid further problems.<BR>
     *
     * @param classIndex
     * @param newClassId
     * @return boolean subclassAdded
     */
    public boolean modifySubClass(int classIndex, int newClassId) {
        if (!_subclassLock.tryLock()) { return false; }

        try {
            try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
                // Remove all henna info stored for this sub-class.
                PreparedStatement statement = con.prepareStatement(DELETE_CHAR_HENNAS);
                statement.setInt(1, getObjectId());
                statement.setInt(2, classIndex);
                statement.execute();
                statement.close();

                // Remove all shortcuts info stored for this sub-class.
                statement = con.prepareStatement(DELETE_CHAR_SHORTCUTS);
                statement.setInt(1, getObjectId());
                statement.setInt(2, classIndex);
                statement.execute();
                statement.close();

                // Remove all effects info stored for this sub-class.
                statement = con.prepareStatement(DELETE_SKILL_SAVE);
                statement.setInt(1, getObjectId());
                statement.setInt(2, classIndex);
                statement.execute();
                statement.close();

                // Remove all skill info stored for this sub-class.
                statement = con.prepareStatement(DELETE_CHAR_SKILLS);
                statement.setInt(1, getObjectId());
                statement.setInt(2, classIndex);
                statement.execute();
                statement.close();

                // Remove all basic info stored about this sub-class.
                statement = con.prepareStatement(DELETE_CHAR_SUBCLASS);
                statement.setInt(1, getObjectId());
                statement.setInt(2, classIndex);
                statement.execute();
                statement.close();
            }
            catch (Exception e) {
                _log.warning("Could not modify subclass for " + getName() + " to class index " + classIndex + ": " + e);

                // This must be done in order to maintain data consistency.
                _subClasses.remove(classIndex);
                return false;
            }

            _subClasses.remove(classIndex);
        }
        finally {
            _subclassLock.unlock();
        }

        return addSubClass(newClassId, classIndex);
    }

    public boolean isSubClassActive() {
        return _classIndex > 0;
    }

    public Map<Integer, SubClass> getSubClasses() {
        return _subClasses;
    }

    public int getTotalSubClasses() {
        return _subClasses.size();
    }

    public int getBaseClass() {
        return _baseClass;
    }

    public void setBaseClass(ClassId classId) {
        _baseClass = classId.ordinal();
    }

    public int getActiveClass() {
        return _activeClass;
    }

    public int getClassIndex() {
        return _classIndex;
    }

    private void setClassTemplate(int classId) {
        _activeClass = classId;

        PcTemplate t = CharTemplateTable.getInstance().getTemplate(classId);

        if (t == null) {
            _log.severe("Missing template for classId: " + classId);
            throw new Error();
        }

        // Set the template of the L2PcInstance
        setTemplate(t);
    }

    /**
     * Changes the character's class based on the given class index. <BR>
     * <BR>
     * An index of zero specifies the character's original (base) class, while indexes 1-3 specifies the character's sub-classes respectively.
     */
    public boolean setActiveClass(int classIndex) {
        if (!_subclassLock.tryLock()) { return false; }

        try {
            // Remove active item skills before saving char to database because next time when choosing this class, worn items can be different
            for (L2ItemInstance item : inventory.getAugmentedItems()) {
                if (item != null && item.isEquipped()) { item.getAugmentation().removeBonus(this); }
            }

            // abort any kind of cast.
            abortCast();

            // Stop casting for any player that may be casting a force buff on this l2pcinstance.
            for (L2Character character : getKnownList().getKnownType(L2Character.class)) {
                if (character.getFusionSkill() != null && character.getFusionSkill().getTarget() == this) {
                    character.abortCast();
                }
            }

            store();
            _reuseTimeStamps.clear();

            // clear charges
            _charges.set(0);
            stopChargeTask();

            if (classIndex == 0) {
                setClassTemplate(_baseClass);
            }
            else {
                try {
                    setClassTemplate(_subClasses.get(classIndex).getClassId());
                }
                catch (Exception e) {
                    _log.info("Could not switch " + getName() + "'s sub class to class index " + classIndex + ": " + e);
                    return false;
                }
            }
            _classIndex = classIndex;

            if (isInParty()) {
                _party.recalculatePartyLevel();
            }

            if (summon instanceof L2SummonInstance) {
                summon.unSummon(this);
            }

            for (L2Skill oldSkill : getAllSkills()) { super.removeSkill(oldSkill); }

            stopAllEffectsExceptThoseThatLastThroughDeath();
            stopCubics();

            if (isSubClassActive()) {
                _dwarvenRecipeBook.clear();
                _commonRecipeBook.clear();
            }
            else { restoreRecipeBook(); }

            restoreSkills();
            rewardSkills();
            regiveTemporarySkills();

            // Prevents some issues when changing between subclases that shares skills
            getDisabledSkills().clear();

            restoreEffects();
            updateEffectIcons();
            sendPacket(new EtcStatusUpdate(this));

            // If player has quest "Repent Your Sins", remove it
            QuestState st = getQuestState("Q422_RepentYourSins");
            if (st != null) { st.exitQuest(true); }

            for (int i = 0; i < 3; i++) { _henna[i] = null; }

            restoreHenna();
            sendPacket(new HennaInfo(this));

            if (getCurrentHp() > getMaxHp()) { setCurrentHp(getMaxHp()); }
            if (getCurrentMp() > getMaxMp()) { setCurrentMp(getMaxMp()); }
            if (getCurrentCp() > getMaxCp()) { setCurrentCp(getMaxCp()); }

            refreshOverloaded();
            refreshExpertisePenalty();
            broadcastUserInfo();

            // Clear resurrect xp calculation
            _expBeforeDeath = (long) 0;

            _shortCuts.restore();
            sendPacket(new ShortCutInit(this));

            broadcastPacket(new SocialAction(this, 15));
            sendPacket(new SkillCoolTime(this));
            return true;
        }
        finally {
            _subclassLock.unlock();
        }
    }

    public boolean isSubclassChangeLocked() {
        return _subclassLock.isLocked();
    }

    public void stopWaterTask() {
        if (_isInWater) {
            _isInWater = false;
            sendPacket(new SetupGauge(2, 0));
            WaterTaskManager.getInstance().remove(this);
        }
    }

    public void startWaterTask() {
        if (!isDead() && !_isInWater) {
            _isInWater = true;
            int time = (int) calcStat(Stats.BREATH, 60000 * getRace().getBreathMultiplier(), this, null);

            sendPacket(new SetupGauge(2, time));
            WaterTaskManager.getInstance().add(this, time);
        }
    }

    public void checkWaterState() {
        if (isInsideZone(ZoneId.WATER)) { startWaterTask(); }
        else { stopWaterTask(); }
    }

    public void onPlayerEnter() {
        if (isCursedWeaponEquipped()) {
            CursedWeaponsManager.getInstance().getCursedWeapon(_cursedWeaponEquippedId).cursedOnLogin();
        }

        // Add to the GameTimeTask to keep inform about activity time.
        GameTimeTaskManager.getInstance().add(this);

        // Teleport player if the Seven Signs period isn't the good one, or if the player isn't in a cabal.
        if (_isIn7sDungeon && !isGM()) {
            if (SevenSigns.getInstance().isSealValidationPeriod() || SevenSigns.getInstance().isCompResultsPeriod()) {
                if (SevenSigns.getInstance().getPlayerCabal(getObjectId()) != SevenSigns.getInstance().getCabalHighestScore()) {
                    teleToLocation(TeleportWhereType.Town);
                    _isIn7sDungeon = false;
                }
            }
            else if (SevenSigns.getInstance().getPlayerCabal(getObjectId()) == SevenSigns.CABAL_NULL) {
                teleToLocation(TeleportWhereType.Town);
                _isIn7sDungeon = false;
            }
        }

        // Jail task
        updatePunishState();

        if (isGM()) {
            if (isInvul()) { sendMessage("Entering world in Invulnerable mode."); }
            if (appearance.getInvisible()) { sendMessage("Entering world in Invisible mode."); }
            if (_messageRefusal) { sendMessage("Entering world in Message Refusal mode."); }
        }

        revalidateZone(true);
        notifyFriends(true);
    }

    public long getLastAccess() {
        return _lastAccess;
    }

    private void checkRecom(int recsHave, int recsLeft) {
        Calendar check = Calendar.getInstance();
        check.setTimeInMillis(_lastRecomUpdate);
        check.add(Calendar.DAY_OF_MONTH, 1);

        Calendar min = Calendar.getInstance();

        _recomHave = recsHave;
        _recomLeft = recsLeft;

        if (getStat().getLevel() < 10 || check.after(min)) { return; }

        restartRecom();
    }

    public void restartRecom() {
        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement(DELETE_CHAR_RECOMS);
            statement.setInt(1, getObjectId());
            statement.execute();
            statement.close();

            _recomChars.clear();
        }
        catch (Exception e) {
            _log.warning("could not clear char recommendations: " + e);
        }

        if (getStat().getLevel() < 20) {
            _recomLeft = 3;
            _recomHave--;
        }
        else if (getStat().getLevel() < 40) {
            _recomLeft = 6;
            _recomHave -= 2;
        }
        else {
            _recomLeft = 9;
            _recomHave -= 3;
        }

        if (_recomHave < 0) { _recomHave = 0; }

        // If we have to update last update time, but it's now before 13, we should set it to yesterday
        Calendar update = Calendar.getInstance();
        if (update.get(Calendar.HOUR_OF_DAY) < 13) { update.add(Calendar.DAY_OF_MONTH, -1); }

        update.set(Calendar.HOUR_OF_DAY, 13);
        _lastRecomUpdate = update.getTimeInMillis();
    }

    @Override
    public void doRevive() {
        super.doRevive();

        stopEffects(L2EffectType.CHARMOFCOURAGE);
        sendPacket(new EtcStatusUpdate(this));

        _reviveRequested = 0;
        _revivePower = 0;

        if (isMounted()) { startFeed(_mountNpcId); }

        if (isInParty() && _party.isInDimensionalRift()) {
            if (!DimensionalRiftManager.getInstance().checkIfInPeaceZone(getX(), getY(), getZ())) {
                _party.getDimensionalRift().memberRessurected(this);
            }
        }

        // Schedule a paralyzed task to wait for the animation to finish
        ThreadPoolManager.getInstance().scheduleGeneral(new Runnable() {
            @Override
            public void run() {
                setIsParalyzed(false);
            }
        }, getAnimationTimer());
        setIsParalyzed(true);
    }

    @Override
    public void doRevive(double revivePower) {
        // Restore the player's lost experience, depending on the % return of the skill used (based on its power).
        restoreExp(revivePower);
        doRevive();
    }

    public void reviveRequest(L2PcInstance Reviver, L2Skill skill, boolean Pet) {
        if (_reviveRequested == 1) {
            // Resurrection has already been proposed.
            if (_revivePet == Pet) { Reviver.sendPacket(SystemMessageId.RES_HAS_ALREADY_BEEN_PROPOSED); }
            else {
                if (Pet)
                // A pet cannot be resurrected while it's owner is in the process of resurrecting.
                { Reviver.sendPacket(SystemMessageId.CANNOT_RES_PET2); }
                else
                // While a pet is attempting to resurrect, it cannot help in resurrecting its master.
                { Reviver.sendPacket(SystemMessageId.MASTER_CANNOT_RES); }
            }
            return;
        }

        if ((Pet && summon != null && summon.isDead()) || (!Pet && isDead())) {
            _reviveRequested = 1;

            if (isPhoenixBlessed()) { _revivePower = 100; }
            else if (isAffected(L2EffectFlag.CHARM_OF_COURAGE)) { _revivePower = 0; }
            else { _revivePower = Formulas.calculateSkillResurrectRestorePercent(skill.getPower(), Reviver); }

            _revivePet = Pet;

            if (isAffected(L2EffectFlag.CHARM_OF_COURAGE)) {
                sendPacket(new ConfirmDlg(SystemMessageId.DO_YOU_WANT_TO_BE_RESTORED).addTime(60000));
                return;
            }

            sendPacket(new ConfirmDlg(SystemMessageId.RESSURECTION_REQUEST_BY_S1).addPcName(Reviver));
        }
    }

    public void reviveAnswer(int answer) {
        if (_reviveRequested != 1 || (!isDead() && !_revivePet) || (_revivePet && summon != null && !summon.isDead())) {
            return;
        }

        if (answer == 0 && isPhoenixBlessed()) { stopPhoenixBlessing(null); }
        else if (answer == 1) {
            if (!_revivePet) {
                if (_revivePower != 0) { doRevive(_revivePower); }
                else { doRevive(); }
            }
            else if (summon != null) {
                if (_revivePower != 0) {
                    summon.doRevive(_revivePower);
                }
                else {
                    summon.doRevive();
                }
            }
        }
        _reviveRequested = 0;
        _revivePower = 0;
    }

    public boolean isReviveRequested() {
        return _reviveRequested == 1;
    }

    public boolean isRevivingPet() {
        return _revivePet;
    }

    public void removeReviving() {
        _reviveRequested = 0;
        _revivePower = 0;
    }

    public void onActionRequest() {
        if (isSpawnProtected()) {
            sendMessage("As you acted, you are no longer under spawn protection.");
            setProtection(false);
        }
    }

    /**
     * @return Returns the expertiseIndex.
     */
    public int getExpertiseIndex() {
        return _expertiseIndex;
    }

    /**
     * @param expertiseIndex The expertiseIndex to set.
     */
    public void setExpertiseIndex(int expertiseIndex) {
        _expertiseIndex = expertiseIndex;
    }

    @Override
    public final void onTeleported() {
        super.onTeleported();

        // Force a revalidation
        revalidateZone(true);

        if (Config.PLAYER_SPAWN_PROTECTION > 0) { setProtection(true); }

        // Stop toggles upon teleport.
        if (!isGM()) { stopAllToggles(); }

        // Modify the position of the tamed beast if necessary
        if (_tamedBeast != null) {
            _tamedBeast.getAI().stopFollow();
            _tamedBeast.teleToLocation(getPosition().getX(), getPosition().getY(), getPosition().getZ(), 0);
            _tamedBeast.getAI().startFollow(this);
        }

        // Modify the position of the pet if necessary
        L2Summon pet = summon;
        if (pet != null) {
            pet.setFollowStatus(false);
            pet.teleToLocation(getPosition().getX(), getPosition().getY(), getPosition().getZ(), 0);
            ((L2SummonAI) pet.getAI()).setStartFollowController(true);
            pet.setFollowStatus(true);
        }
    }

    @Override
    public void addExpAndSp(long addToExp, int addToSp) {
        getStat().addExpAndSp(addToExp, addToSp);
    }

    public void removeExpAndSp(long removeExp, int removeSp) {
        getStat().removeExpAndSp(removeExp, removeSp);
    }

    @Override
    public void reduceCurrentHp(double value, L2Character attacker, boolean awake, boolean isDOT, L2Skill skill) {
        if (skill != null) {
            getStatus().reduceHp(value, attacker, awake, isDOT, skill.isToggle(), skill.getDmgDirectlyToHP());
        }
        else { getStatus().reduceHp(value, attacker, awake, isDOT, false, false); }

        // notify the tamed beast of attacks
        if (_tamedBeast != null) {
            _tamedBeast.onOwnerGotAttacked(attacker);
        }
    }

    public synchronized void addBypass(String bypass) {
        if (bypass == null) { return; }

        _validBypass.add(bypass);
    }

    public synchronized void addBypass2(String bypass) {
        if (bypass == null) { return; }

        _validBypass2.add(bypass);
    }

    public synchronized boolean validateBypass(String cmd) {
        for (String bp : _validBypass) {
            if (bp == null) { continue; }

            if (bp.equals(cmd)) { return true; }
        }

        for (String bp : _validBypass2) {
            if (bp == null) { continue; }

            if (cmd.startsWith(bp)) { return true; }
        }

        return false;
    }

    /**
     * Test multiple cases where the item shouldn't be able to manipulate.
     *
     * @param objectId : The item objectId.
     * @return true if it the item can be manipulated, false ovtherwise.
     */
    public boolean validateItemManipulation(int objectId) {
        L2ItemInstance item = inventory.getItemByObjectId(objectId);

        // You don't own the item, or item is null.
        if (item == null || item.getOwnerId() != getObjectId()) { return false; }

        // Pet whom item you try to manipulate is summoned/mounted.
        if (summon != null && summon.getControlItemId() == objectId || _mountObjectID == objectId) {
            return false;
        }

        if (_activeEnchantItem != null && _activeEnchantItem.getObjectId() == objectId) { return false; }

        // Can't trade a cursed weapon.
        return !CursedWeaponsManager.getInstance().isCursed(item.getItemId());

    }

    public synchronized void clearBypass() {
        _validBypass.clear();
        _validBypass2.clear();
    }

    /**
     * @return Returns the inBoat.
     */
    public boolean isInBoat() {
        return _vehicle != null && _vehicle.isBoat();
    }

    public L2BoatInstance getBoat() {
        return (L2BoatInstance) _vehicle;
    }

    public L2Vehicle getVehicle() {
        return _vehicle;
    }

    public void setVehicle(L2Vehicle v) {
        if (v == null && _vehicle != null) { _vehicle.removePassenger(this); }

        _vehicle = v;
    }

    public boolean isInCrystallize() {
        return _inCrystallize;
    }

    public void setInCrystallize(boolean inCrystallize) {
        _inCrystallize = inCrystallize;
    }

    public Location getInVehiclePosition() {
        return _inVehiclePosition;
    }

    public void setInVehiclePosition(Location pt) {
        _inVehiclePosition = pt;
    }

    /**
     * Manage the delete task of a L2PcInstance (Leave Party, Unsummon pet, Save its inventory in the database, Remove it from the world...).
     * <ul>
     * <li>If the L2PcInstance is in observer mode, set its position to its position before entering in observer mode</li>
     * <li>Set the online Flag to True or False and update the characters table of the database with online status and lastAccess</li>
     * <li>Stop the HP/MP/CP Regeneration task</li>
     * <li>Cancel Crafting, Attak or Cast</li>
     * <li>Remove the L2PcInstance from the world</li>
     * <li>Stop Party and Unsummon Pet</li>
     * <li>Update database with items in its inventory and remove them from the world</li>
     * <li>Remove all L2Object from _knownObjects and _knownPlayer of the L2Character then cancel Attak or Cast and notify AI</li>
     * <li>Close the connection with the client</li>
     * </ul>
     */
    @Override
    public void deleteMe() {
        cleanup();
        store();
        super.deleteMe();
    }

    private synchronized void cleanup() {
        try {
            // Put the online status to false
            setOnlineStatus(false, true);

            // abort cast & attack and remove the target. Cancels movement aswell.
            abortAttack();
            abortCast();
            stopMove(null);
            setTarget(null);

            PartyMatchWaitingList.getInstance().removePlayer(this);
            if (_partyroom != 0) {
                PartyMatchRoom room = PartyMatchRoomList.getInstance().getRoom(_partyroom);
                if (room != null) { room.deleteMember(this); }
            }

            if (isFlying()) { removeSkill(SkillTable.getInfo(4289, 1)); }

            // Stop all scheduled tasks
            stopAllTimers();

            // Cancel the cast of eventual fusion skill users on this target.
            for (L2Character character : getKnownList().getKnownType(L2Character.class)) {
                if (character.getFusionSkill() != null && character.getFusionSkill().getTarget() == this) {
                    character.abortCast();
                }
            }

            // Stop signets & toggles effects.
            for (L2Effect effect : getAllEffects()) {
                if (effect.getSkill().isToggle()) {
                    effect.exit();
                    continue;
                }

                switch (effect.getEffectType()) {
                    case SIGNET_GROUND:
                    case SIGNET_EFFECT:
                        effect.exit();
                        break;
                }
            }

            // Remove the L2PcInstance from the world
            decayMe();

            // Remove from world regions zones
            L2WorldRegion oldRegion = getWorldRegion();
            if (oldRegion != null) { oldRegion.removeFromZones(this); }

            // If a party is in progress, leave it
            if (isInParty()) { leaveParty(); }

            // If the L2PcInstance has Pet, unsummon it
            if (summon != null) {
                summon.unSummon(this);
            }

            // Handle removal from olympiad game
            if (OlympiadManager.getInstance().isRegistered(this) || _olympiadGameId != -1) {
                OlympiadManager.getInstance().removeDisconnectedCompetitor(this);
            }

            // set the status for pledge member list to OFFLINE
            if (clan != null) {
                L2ClanMember clanMember = clan.getClanMember(getObjectId());
                if (clanMember != null) { clanMember.setPlayerInstance(null); }
            }

            // deals with sudden exit in the middle of transaction
            if (getActiveRequester() != null) {
                _activeRequester = null;
                cancelActiveTrade();
            }

            // If the L2PcInstance is a GM, remove it from the GM List
            if (isGM()) { GmListTable.getInstance().deleteGm(this); }

            // Check if the L2PcInstance is in observer mode to set its position to its position
            // before entering in observer mode
            if (inObserverMode()) {
                setXYZInvisible(_savedLocation.getX(), _savedLocation.getY(), _savedLocation.getZ());
            }

            // Oust player from boat
            if (_vehicle != null) {
                _vehicle.oustPlayer(this, true);
            }

            // Update inventory and remove them from the world
            inventory.deleteMe();

            // Update warehouse and remove them from the world
            clearWarehouse();

            // Update freight and remove them from the world
            clearFreight();
            clearDepositedFreight();

            if (isCursedWeaponEquipped()) {
                CursedWeaponsManager.getInstance().getCursedWeapon(_cursedWeaponEquippedId).setPlayer(null);
            }

            // Remove all L2Object from _knownObjects and _knownPlayer of the L2Character then cancel Attak or Cast and notify AI
            getKnownList().removeAllKnownObjects();

            if (_clanId > 0) {
                clan.broadcastToOtherOnlineMembers(new PledgeShowMemberListUpdate(this), this);
            }

            if (isSeated()) {
                L2Object obj = L2World.getInstance().getObject(_mountObjectID);
                ((L2StaticObjectInstance) obj).setBusy(false);
            }

            // Remove L2Object object from _allObjects of L2World
            L2World.getInstance().removeObject(this);
            L2World.getInstance().removePlayer(this); // force remove in case of crash during teleport

            // friends & blocklist update
            notifyFriends(false);
            _blockList.playerLogout();
        }
        catch (Exception e) {
            _log.log(Level.WARNING, "Exception on deleteMe()" + e.getMessage(), e);
        }
    }

    public void startFishing(Location loc) {
        stopMove(null);
        setIsImmobilized(true);

        _fishingLoc = loc;

        // Starts fishing
        int group = getRandomGroup();

        _fish = FishTable.getFish(getRandomFishLvl(), getRandomFishType(group), group);
        if (_fish == null) {
            endFishing(false);
            return;
        }

        sendPacket(SystemMessageId.CAST_LINE_AND_START_FISHING);

        broadcastPacket(new ExFishingStart(this, _fish.getType(_lure.isNightLure()), loc, _lure.isNightLure()));
        sendPacket(new PlaySound(1, "SF_P_01", 0, 0, 0, 0, 0));
        startLookingForFishTask();
    }

    public void stopLookingForFishTask() {
        if (_taskforfish != null) {
            _taskforfish.cancel(false);
            _taskforfish = null;
        }
    }

    public void startLookingForFishTask() {
        if (!isDead() && _taskforfish == null) {
            int checkDelay = 0;
            boolean isNoob = false;
            boolean isUpperGrade = false;

            if (_lure != null) {
                int lureid = _lure.getItemId();
                isNoob = _fish.getGroup() == 0;
                isUpperGrade = _fish.getGroup() == 2;
                if (lureid == 6519 || lureid == 6522 || lureid == 6525 || lureid == 8505 || lureid == 8508 || lureid == 8511) // low grade
                { checkDelay = Math.round((float) (_fish.getGutsCheckTime() * 1.33)); }
                else if (lureid == 6520 || lureid == 6523 || lureid == 6526 || (lureid >= 8505 && lureid <= 8513) || (lureid >= 7610 && lureid <= 7613) || (lureid >= 7807 && lureid <= 7809) || (lureid >= 8484 && lureid <= 8486)) // medium grade, beginner, prize-winning & quest special bait
                { checkDelay = Math.round((float) (_fish.getGutsCheckTime() * 1.00)); }
                else if (lureid == 6521 || lureid == 6524 || lureid == 6527 || lureid == 8507 || lureid == 8510 || lureid == 8513) // high grade
                { checkDelay = Math.round((float) (_fish.getGutsCheckTime() * 0.66)); }
            }
            _taskforfish = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new LookingForFishTask(_fish.getWaitTime(), _fish.getFishGuts(), _fish.getType(_lure.isNightLure()), isNoob, isUpperGrade), 10000, checkDelay);
        }
    }

    private int getRandomGroup() {
        switch (_lure.getItemId()) {
            case 7807: // green for beginners
            case 7808: // purple for beginners
            case 7809: // yellow for beginners
            case 8486: // prize-winning for beginners
                return 0;

            case 8485: // prize-winning luminous
            case 8506: // green luminous
            case 8509: // purple luminous
            case 8512: // yellow luminous
                return 2;

            default:
                return 1;
        }
    }

    private int getRandomFishType(int group) {
        int check = Rnd.get(100);
        int type = 1;
        switch (group) {
            case 0: // fish for novices
                switch (_lure.getItemId()) {
                    case 7807: // green lure, preferred by fast-moving (nimble) fish (type 5)
                        if (check <= 54) { type = 5; }
                        else if (check <= 77) { type = 4; }
                        else { type = 6; }
                        break;

                    case 7808: // purple lure, preferred by fat fish (type 4)
                        if (check <= 54) { type = 4; }
                        else if (check <= 77) { type = 6; }
                        else { type = 5; }
                        break;

                    case 7809: // yellow lure, preferred by ugly fish (type 6)
                        if (check <= 54) { type = 6; }
                        else if (check <= 77) { type = 5; }
                        else { type = 4; }
                        break;

                    case 8486: // prize-winning fishing lure for beginners
                        if (check <= 33) { type = 4; }
                        else if (check <= 66) { type = 5; }
                        else { type = 6; }
                        break;
                }
                break;

            case 1: // normal fish
                switch (_lure.getItemId()) {
                    case 7610:
                    case 7611:
                    case 7612:
                    case 7613:
                        type = 3;
                        break;

                    case 6519: // all theese lures (green) are prefered by fast-moving (nimble) fish (type 1)
                    case 8505:
                    case 6520:
                    case 6521:
                    case 8507:
                        if (check <= 54) { type = 1; }
                        else if (check <= 74) { type = 0; }
                        else if (check <= 94) { type = 2; }
                        else { type = 3; }
                        break;

                    case 6522: // all theese lures (purple) are prefered by fat fish (type 0)
                    case 8508:
                    case 6523:
                    case 6524:
                    case 8510:
                        if (check <= 54) { type = 0; }
                        else if (check <= 74) { type = 1; }
                        else if (check <= 94) { type = 2; }
                        else { type = 3; }
                        break;

                    case 6525: // all theese lures (yellow) are prefered by ugly fish (type 2)
                    case 8511:
                    case 6526:
                    case 6527:
                    case 8513:
                        if (check <= 55) { type = 2; }
                        else if (check <= 74) { type = 1; }
                        else if (check <= 94) { type = 0; }
                        else { type = 3; }
                        break;
                    case 8484: // prize-winning fishing lure
                        if (check <= 33) { type = 0; }
                        else if (check <= 66) { type = 1; }
                        else { type = 2; }
                        break;
                }
                break;

            case 2: // upper grade fish, luminous lure
                switch (_lure.getItemId()) {
                    case 8506: // green lure, preferred by fast-moving (nimble) fish (type 8)
                        if (check <= 54) { type = 8; }
                        else if (check <= 77) { type = 7; }
                        else { type = 9; }
                        break;

                    case 8509: // purple lure, preferred by fat fish (type 7)
                        if (check <= 54) { type = 7; }
                        else if (check <= 77) { type = 9; }
                        else { type = 8; }
                        break;

                    case 8512: // yellow lure, preferred by ugly fish (type 9)
                        if (check <= 54) { type = 9; }
                        else if (check <= 77) { type = 8; }
                        else { type = 7; }
                        break;

                    case 8485: // prize-winning fishing lure
                        if (check <= 33) { type = 7; }
                        else if (check <= 66) { type = 8; }
                        else { type = 9; }
                        break;
                }
        }
        return type;
    }

    private int getRandomFishLvl() {
        int skilllvl = getSkillLevel(1315);

        L2Effect e = getFirstEffect(2274);
        if (e != null) { skilllvl = (int) e.getSkill().getPower(); }

        if (skilllvl <= 0) { return 1; }

        int randomlvl;

        int check = Rnd.get(100);
        if (check <= 50) { randomlvl = skilllvl; }
        else if (check <= 85) {
            randomlvl = skilllvl - 1;
            if (randomlvl <= 0) { randomlvl = 1; }
        }
        else {
            randomlvl = skilllvl + 1;
            if (randomlvl > 27) { randomlvl = 27; }
        }
        return randomlvl;
    }

    public void startFishCombat(boolean isNoob, boolean isUpperGrade) {
        _fishCombat = new L2Fishing(this, _fish, isNoob, isUpperGrade, _lure.getItemId());
    }

    public void endFishing(boolean win) {
        if (_fishCombat == null) { sendPacket(SystemMessageId.BAIT_LOST_FISH_GOT_AWAY); }
        else { _fishCombat = null; }

        _lure = null;
        _fishingLoc = null;

        // Ends fishing
        broadcastPacket(new ExFishingEnd(win, getObjectId()));
        sendPacket(SystemMessageId.REEL_LINE_AND_STOP_FISHING);
        setIsImmobilized(false);
        stopLookingForFishTask();
    }

    public L2Fishing getFishCombat() {
        return _fishCombat;
    }

    public Location getFishingLoc() {
        return _fishingLoc;
    }

    public L2ItemInstance getLure() {
        return _lure;
    }

    public void setLure(L2ItemInstance lure) {
        _lure = lure;
    }

    public int getInventoryLimit() {
        return ((getRace() == Race.Dwarf) ? Config.INVENTORY_MAXIMUM_DWARF : Config.INVENTORY_MAXIMUM_NO_DWARF) + (int) getStat().calcStat(Stats.INV_LIM, 0, null, null);
    }

    public int getWareHouseLimit() {
        return ((getRace() == Race.Dwarf) ? Config.WAREHOUSE_SLOTS_DWARF : Config.WAREHOUSE_SLOTS_NO_DWARF) + (int) getStat().calcStat(Stats.WH_LIM, 0, null, null);
    }

    public int getPrivateSellStoreLimit() {
        return ((getRace() == Race.Dwarf) ? Config.MAX_PVTSTORE_SLOTS_DWARF : Config.MAX_PVTSTORE_SLOTS_OTHER) + (int) getStat().calcStat(Stats.P_SELL_LIM, 0, null, null);
    }

    public int getPrivateBuyStoreLimit() {
        return ((getRace() == Race.Dwarf) ? Config.MAX_PVTSTORE_SLOTS_DWARF : Config.MAX_PVTSTORE_SLOTS_OTHER) + (int) getStat().calcStat(Stats.P_BUY_LIM, 0, null, null);
    }

    public int getFreightLimit() {
        return Config.FREIGHT_SLOTS + (int) getStat().calcStat(Stats.FREIGHT_LIM, 0, null, null);
    }

    public int getDwarfRecipeLimit() {
        return Config.DWARF_RECIPE_LIMIT + (int) getStat().calcStat(Stats.REC_D_LIM, 0, null, null);
    }

    public int getCommonRecipeLimit() {
        return Config.COMMON_RECIPE_LIMIT + (int) getStat().calcStat(Stats.REC_C_LIM, 0, null, null);
    }

    public int getMountNpcId() {
        return _mountNpcId;
    }

    public int getMountLevel() {
        return _mountLevel;
    }

    public int getMountObjectID() {
        return _mountObjectID;
    }

    public void setMountObjectID(int newID) {
        _mountObjectID = newID;
    }

    /**
     * @return the current player skill in use.
     */
    public SkillUseHolder getCurrentSkill() {
        return _currentSkill;
    }

    /**
     * Update the _currentSkill holder.
     *
     * @param skill        : The skill to update for (or null)
     * @param ctrlPressed  : The boolean information regarding ctrl key.
     * @param shiftPressed : The boolean information regarding shift key.
     */
    public void setCurrentSkill(L2Skill skill, boolean ctrlPressed, boolean shiftPressed) {
        _currentSkill.setSkill(skill);
        _currentSkill.setCtrlPressed(ctrlPressed);
        _currentSkill.setShiftPressed(shiftPressed);
    }

    /**
     * @return the current pet skill in use.
     */
    public SkillUseHolder getCurrentPetSkill() {
        return _currentPetSkill;
    }

    /**
     * Update the _currentPetSkill holder.
     *
     * @param skill        : The skill to update for (or null)
     * @param ctrlPressed  : The boolean information regarding ctrl key.
     * @param shiftPressed : The boolean information regarding shift key.
     */
    public void setCurrentPetSkill(L2Skill skill, boolean ctrlPressed, boolean shiftPressed) {
        _currentPetSkill.setSkill(skill);
        _currentPetSkill.setCtrlPressed(ctrlPressed);
        _currentPetSkill.setShiftPressed(shiftPressed);
    }

    /**
     * @return the current queued skill in use.
     */
    public SkillUseHolder getQueuedSkill() {
        return _queuedSkill;
    }

    /**
     * Update the _queuedSkill holder.
     *
     * @param skill        : The skill to update for (or null)
     * @param ctrlPressed  : The boolean information regarding ctrl key.
     * @param shiftPressed : The boolean information regarding shift key.
     */
    public void setQueuedSkill(L2Skill skill, boolean ctrlPressed, boolean shiftPressed) {
        _queuedSkill.setSkill(skill);
        _queuedSkill.setCtrlPressed(ctrlPressed);
        _queuedSkill.setShiftPressed(shiftPressed);
    }

    /**
     * @return the timer to delay animation tasks, based on run speed.
     */
    public int getAnimationTimer() {
        return Math.max(1000, 5000 - getRunSpeed() * 20);
    }

    /**
     * @return punishment level of player
     */
    public PunishLevel getPunishLevel() {
        return _punishLevel;
    }

    public void setPunishLevel(int state) {
        switch (state) {
            case 0:
                _punishLevel = PunishLevel.NONE;
                break;
            case 1:
                _punishLevel = PunishLevel.CHAT;
                break;
            case 2:
                _punishLevel = PunishLevel.JAIL;
                break;
            case 3:
                _punishLevel = PunishLevel.CHAR;
                break;
            case 4:
                _punishLevel = PunishLevel.ACC;
                break;
        }
    }

    /**
     * @return True if player is jailed
     */
    public boolean isInJail() {
        return _punishLevel == PunishLevel.JAIL;
    }

    /**
     * @return True if player is chat banned
     */
    public boolean isChatBanned() {
        return _punishLevel == PunishLevel.CHAT;
    }

    /**
     * Sets punish level for player based on delay
     *
     * @param state
     * @param delayInMinutes -- 0 for infinite
     */
    public void setPunishLevel(PunishLevel state, int delayInMinutes) {
        long delayInMilliseconds = delayInMinutes * 60000L;
        switch (state) {
            case NONE: // Remove Punishments
                switch (_punishLevel) {
                    case CHAT:
                        _punishLevel = state;
                        stopPunishTask(true);
                        sendPacket(new EtcStatusUpdate(this));
                        sendMessage("Chatting is now available.");
                        sendPacket(new PlaySound("systemmsg_e.345"));
                        break;
                    case JAIL:
                        _punishLevel = state;

                        // Open a Html message to inform the player
                        NpcHtmlMessage html = new NpcHtmlMessage(0);
                        html.setFile("data/html/jail_out.htm");
                        sendPacket(html);

                        stopPunishTask(true);
                        teleToLocation(17836, 170178, -3507, 20); // Floran village
                        break;
                }
                break;
            case CHAT: // Chat ban
                // not allow player to escape jail using chat ban
                if (_punishLevel == PunishLevel.JAIL) { break; }

                _punishLevel = state;
                _punishTimer = 0;
                sendPacket(new EtcStatusUpdate(this));

                // Remove the task if any
                stopPunishTask(false);

                if (delayInMinutes > 0) {
                    _punishTimer = delayInMilliseconds;

                    // start the countdown
                    _punishTask = ThreadPoolManager.getInstance().scheduleGeneral(new PunishTask(), _punishTimer);
                    sendMessage("Chatting has been suspended for " + delayInMinutes + " minute(s).");
                }
                else { sendMessage("Chatting has been suspended."); }

                // Send same sound packet in both "delay" cases.
                sendPacket(new PlaySound("systemmsg_e.346"));
                break;

            case JAIL: // Jail Player
                _punishLevel = state;
                _punishTimer = 0;

                // Remove the task if any
                stopPunishTask(false);

                if (delayInMinutes > 0) {
                    _punishTimer = delayInMilliseconds;

                    // start the countdown
                    _punishTask = ThreadPoolManager.getInstance().scheduleGeneral(new PunishTask(), _punishTimer);
                    sendMessage("You are jailed for " + delayInMinutes + " minutes.");
                }

                if (OlympiadManager.getInstance().isRegisteredInComp(this)) {
                    OlympiadManager.getInstance().removeDisconnectedCompetitor(this);
                }

                // Open a Html message to inform the player
                NpcHtmlMessage html = new NpcHtmlMessage(0);
                html.setFile("data/html/jail_in.htm");
                sendPacket(html);

                _isIn7sDungeon = false;
                teleToLocation(-114356, -249645, -2984, 0); // Jail
                break;
            case CHAR: // Ban Character
                setAccessLevel(-100);
                logout();
                break;
            case ACC: // Ban Account
                // TODO send ban to login server
                logout();
                break;
            default:
                _punishLevel = state;
                break;
        }

        // store in database
        storeCharBase();
    }

    public long getPunishTimer() {
        return _punishTimer;
    }

    public void setPunishTimer(long time) {
        _punishTimer = time;
    }

    private void updatePunishState() {
        if (_punishLevel != PunishLevel.NONE) {
            // If punish timer exists, restart punishtask.
            if (_punishTimer > 0) {
                _punishTask = ThreadPoolManager.getInstance().scheduleGeneral(new PunishTask(), _punishTimer);
                sendMessage("You are still " + _punishLevel.string() + " for " + Math.round(_punishTimer / 60000f) + " minutes.");
            }
            if (_punishLevel == PunishLevel.JAIL) {
                // If player escaped, put him back in jail
                if (!isInsideZone(ZoneId.JAIL)) { teleToLocation(-114356, -249645, -2984, 20); }
            }
        }
    }

    public void stopPunishTask(boolean save) {
        if (_punishTask != null) {
            if (save) {
                long delay = _punishTask.getDelay(TimeUnit.MILLISECONDS);
                if (delay < 0) { delay = 0; }
                _punishTimer = delay;
            }
            _punishTask.cancel(false);
            _punishTask = null;
        }
    }

    public int getPowerGrade() {
        return _powerGrade;
    }

    public void setPowerGrade(int power) {
        _powerGrade = power;
    }

    public boolean isCursedWeaponEquipped() {
        return _cursedWeaponEquippedId != 0;
    }

    public int getCursedWeaponEquippedId() {
        return _cursedWeaponEquippedId;
    }

    public void setCursedWeaponEquippedId(int value) {
        _cursedWeaponEquippedId = value;
    }

    public void shortBuffStatusUpdate(int magicId, int level, int time) {
        if (_shortBuffTask != null) {
            _shortBuffTask.cancel(false);
            _shortBuffTask = null;
        }
        _shortBuffTask = ThreadPoolManager.getInstance().scheduleGeneral(new ShortBuffTask(), time * 1000);
        _shortBuffTaskSkillId = magicId;

        sendPacket(new ShortBuffStatusUpdate(magicId, level, time));
    }

    public int getShortBuffTaskSkillId() {
        return _shortBuffTaskSkillId;
    }

    public void setShortBuffTaskSkillId(int id) {
        _shortBuffTaskSkillId = id;
    }

    public int getDeathPenaltyBuffLevel() {
        return _deathPenaltyBuffLevel;
    }

    public void setDeathPenaltyBuffLevel(int level) {
        _deathPenaltyBuffLevel = level;
    }

    public void calculateDeathPenaltyBuffLevel(L2Character killer) {
        if (_deathPenaltyBuffLevel >= 15) // maximum level reached
        { return; }

        if ((_karma > 0 || Rnd.get(1, 100) <= Config.DEATH_PENALTY_CHANCE) && !(killer instanceof L2PcInstance) && !isGM() && !(getCharmOfLuck() && (killer == null || killer.isRaid())) && !isPhoenixBlessed() && !(isInsideZone(ZoneId.PVP) || isInsideZone(ZoneId.SIEGE))) {
            if (_deathPenaltyBuffLevel != 0) {
                L2Skill skill = SkillTable.getInfo(5076, _deathPenaltyBuffLevel);
                if (skill != null) { removeSkill(skill, true); }
            }

            _deathPenaltyBuffLevel++;

            addSkill(SkillTable.getInfo(5076, _deathPenaltyBuffLevel), false);
            sendPacket(new EtcStatusUpdate(this));
            sendPacket(SystemMessage.getSystemMessage(SystemMessageId.DEATH_PENALTY_LEVEL_S1_ADDED).addNumber(_deathPenaltyBuffLevel));
        }
    }

    public void reduceDeathPenaltyBuffLevel() {
        if (_deathPenaltyBuffLevel <= 0) { return; }

        L2Skill skill = SkillTable.getInfo(5076, _deathPenaltyBuffLevel);
        if (skill != null) { removeSkill(skill, true); }

        _deathPenaltyBuffLevel--;

        if (_deathPenaltyBuffLevel > 0) {
            addSkill(SkillTable.getInfo(5076, _deathPenaltyBuffLevel), false);
            sendPacket(SystemMessage.getSystemMessage(SystemMessageId.DEATH_PENALTY_LEVEL_S1_ADDED).addNumber(_deathPenaltyBuffLevel));
        }
        else { sendPacket(SystemMessageId.DEATH_PENALTY_LIFTED); }

        sendPacket(new EtcStatusUpdate(this));
    }

    public void restoreDeathPenaltyBuffLevel() {
        if (_deathPenaltyBuffLevel > 0) {
            addSkill(SkillTable.getInfo(5076, _deathPenaltyBuffLevel), false);
        }
    }

    public Collection<TimeStamp> getReuseTimeStamps() {
        return _reuseTimeStamps.values();
    }

    public Map<Integer, TimeStamp> getReuseTimeStamp() {
        return _reuseTimeStamps;
    }

    /**
     * Index according to skill id the current timestamp of use.
     *
     * @param skill
     * @param reuse delay
     */
    @Override
    public void addTimeStamp(L2Skill skill, long reuse) {
        _reuseTimeStamps.put(skill.getReuseHashCode(), new TimeStamp(skill, reuse));
    }

    /**
     * Index according to skill this TimeStamp instance for restoration purposes only.
     *
     * @param skill
     * @param reuse
     * @param systime
     */
    public void addTimeStamp(L2Skill skill, long reuse, long systime) {
        _reuseTimeStamps.put(skill.getReuseHashCode(), new TimeStamp(skill, reuse, systime));
    }

    @Override
    public L2PcInstance getActingPlayer() {
        return this;
    }

    @Override
    public final void sendDamageMessage(L2Character target, int damage, boolean mcrit, boolean pcrit, boolean miss) {
        // Check if hit is missed
        if (miss) {
            sendPacket(SystemMessageId.MISSED_TARGET);
            return;
        }

        // Check if hit is critical
        if (pcrit) { sendPacket(SystemMessageId.CRITICAL_HIT); }
        if (mcrit) { sendPacket(SystemMessageId.CRITICAL_HIT_MAGIC); }

        if (target.isInvul()) {
            if (target.isParalyzed()) { sendPacket(SystemMessageId.OPPONENT_PETRIFIED); }
            else { sendPacket(SystemMessageId.ATTACK_WAS_BLOCKED); }
        }
        else { sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_DID_S1_DMG).addNumber(damage)); }

        if (_inOlympiadMode && target instanceof L2PcInstance && ((L2PcInstance) target)._inOlympiadMode && ((L2PcInstance) target)._olympiadGameId == _olympiadGameId) {
            OlympiadGameManager.getInstance().notifyCompetitorDamage(this, damage);
        }
    }

    public void checkItemRestriction() {
        for (EPaperdollSlot paperdollSlot : EPaperdollSlot.values()) {
            L2ItemInstance equippedItem = inventory.getPaperdollItem(paperdollSlot);
            if (equippedItem != null && !equippedItem.getItem().checkCondition(this, this, false)) {
                inventory.unEquipItemInSlot(paperdollSlot);

                InventoryUpdate iu = new InventoryUpdate();
                iu.addModifiedItem(equippedItem);
                sendPacket(iu);

                SystemMessage sm = null;
                if (equippedItem.getEnchantLevel() > 0) {
                    sm = SystemMessage.getSystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED);
                    sm.addNumber(equippedItem.getEnchantLevel());
                    sm.addItemName(equippedItem);
                }
                else {
                    sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISARMED);
                    sm.addItemName(equippedItem);
                }
                sendPacket(sm);
            }
        }
    }

    public void enteredNoLanding(int delay) {
        _dismountTask = ThreadPoolManager.getInstance().scheduleGeneral(new Dismount(), delay * 1000);
    }

    public void exitedNoLanding() {
        if (_dismountTask != null) {
            _dismountTask.cancel(true);
            _dismountTask = null;
        }
    }

    public void setIsInSiege(boolean b) {
        _isInSiege = b;
    }

    public boolean isInSiege() {
        return _isInSiege;
    }

    /**
     * Remove player from BossZones (used on char logout/exit)
     */
    public void removeFromBossZone() {
        try {
            for (L2BossZone _zone : GrandBossManager.getInstance().getZones()) { _zone.removePlayer(this); }
        }
        catch (Exception e) {
            _log.log(Level.WARNING, "Exception on removeFromBossZone(): " + e.getMessage(), e);
        }
    }

    /**
     * @return the number of charges this L2PcInstance got.
     */
    public int getCharges() {
        return _charges.get();
    }

    public void increaseCharges(int count, int max) {
        if (_charges.get() >= max) {
            sendPacket(SystemMessageId.FORCE_MAXLEVEL_REACHED);
            return;
        }

        restartChargeTask();

        if (_charges.addAndGet(count) >= max) {
            _charges.set(max);
            sendPacket(SystemMessageId.FORCE_MAXLEVEL_REACHED);
        }
        else {
            sendPacket(SystemMessage.getSystemMessage(SystemMessageId.FORCE_INCREASED_TO_S1).addNumber(_charges.get()));
        }

        sendPacket(new EtcStatusUpdate(this));
    }

    public boolean decreaseCharges(int count) {
        if (_charges.get() < count) { return false; }

        if (_charges.addAndGet(-count) == 0) { stopChargeTask(); }
        else { restartChargeTask(); }

        sendPacket(new EtcStatusUpdate(this));
        return true;
    }

    public void clearCharges() {
        _charges.set(0);
        sendPacket(new EtcStatusUpdate(this));
    }

    /**
     * Starts/Restarts the ChargeTask to Clear Charges after 10 Mins.
     */
    private void restartChargeTask() {
        if (_chargeTask != null) {
            _chargeTask.cancel(false);
            _chargeTask = null;
        }
        _chargeTask = ThreadPoolManager.getInstance().scheduleGeneral(new ChargeTask(), 600000);
    }

    /**
     * Stops the Charges Clearing Task.
     */
    public void stopChargeTask() {
        if (_chargeTask != null) {
            _chargeTask.cancel(false);
            _chargeTask = null;
        }
    }

    /**
     * Signets check used to valid who is affected when he entered in the aoe effect.
     *
     * @param cha The target to make checks on.
     * @return true if player can attack the target.
     */
    public boolean canAttackCharacter(L2Character cha) {
        if (cha instanceof L2Attackable) { return true; }

        if (cha instanceof L2Playable) {
            if (cha.isInArena()) { return true; }

            L2PcInstance target = cha.getActingPlayer();

            if (_isInDuel && target._isInDuel && target._duelId == _duelId) { return true; }

            if (isInParty() && target.isInParty()) {
                if (_party == target._party) { return false; }

                if ((_party.getCommandChannel() != null || target._party.getCommandChannel() != null) && (_party.getCommandChannel() == target._party.getCommandChannel())) {
                    return false;
                }
            }

            if (clan != null && target.clan != null) {
                if (_clanId == target._clanId) { return false; }

                if ((getAllyId() > 0 || target.getAllyId() > 0) && getAllyId() == target.getAllyId()) { return false; }

                if (clan.isAtWarWith(target._clanId)) { return true; }
            }
            else {
                if (target._pvpFlag == 0 && target._karma == 0) { return false; }
            }
        }
        return true;
    }

    /**
     * Request Teleport
     *
     * @param requester The player who requested the teleport.
     * @param skill     The used skill.
     * @return true if successful.
     **/
    public boolean teleportRequest(L2PcInstance requester, L2Skill skill) {
        if (_summonRequest.getTarget() != null && requester != null) { return false; }

        _summonRequest.setTarget(requester, skill);
        return true;
    }

    /**
     * Action teleport
     *
     * @param answer
     * @param requesterId
     **/
    public void teleportAnswer(int answer, int requesterId) {
        if (_summonRequest.getTarget() == null) { return; }

        if (answer == 1 && _summonRequest.getTarget().getObjectId() == requesterId) {
            teleToTarget(this, _summonRequest.getTarget(), _summonRequest.getSkill());
        }

        _summonRequest.setTarget(null, null);
    }

    public final int getClientX() {
        return _clientX;
    }

    public final void setClientX(int val) {
        _clientX = val;
    }

    public final int getClientY() {
        return _clientY;
    }

    public final void setClientY(int val) {
        _clientY = val;
    }

    public final int getClientZ() {
        return _clientZ;
    }

    public final void setClientZ(int val) {
        _clientZ = val;
    }

    public final int getClientHeading() {
        return _clientHeading;
    }

    public final void setClientHeading(int val) {
        _clientHeading = val;
    }

    /**
     * @return the mailPosition.
     */
    public int getMailPosition() {
        return _mailPosition;
    }

    /**
     * @param mailPosition The mailPosition to set.
     */
    public void setMailPosition(int mailPosition) {
        _mailPosition = mailPosition;
    }

    /**
     * @param z
     * @return true if character falling now On the start of fall return false for correct coord sync !
     */
    public final boolean isFalling(int z) {
        if (isDead() || isFlying() || isInsideZone(ZoneId.WATER)) { return false; }

        if (System.currentTimeMillis() < _fallingTimestamp) { return true; }

        int deltaZ = getZ() - z;
        if (deltaZ <= getBaseTemplate().getFallHeight()) { return false; }

        int damage = (int) Formulas.calcFallDam(this, deltaZ);
        if (damage > 0) {
            reduceCurrentHp(Math.min(damage, getCurrentHp() - 1), null, false, true, null);
            sendPacket(SystemMessage.getSystemMessage(SystemMessageId.FALL_DAMAGE_S1).addNumber(damage));
        }

        setFalling();

        return false;
    }

    /**
     * Set falling timestamp
     */
    public final void setFalling() {
        _fallingTimestamp = System.currentTimeMillis() + FALLING_VALIDATION_DELAY;
    }

    public boolean isAllowedToEnchantSkills() {
        if (isSubclassChangeLocked()) { return false; }

        if (AttackStanceTaskManager.getInstance().isInAttackStance(this)) { return false; }

        if (isCastingNow() || isCastingSimultaneouslyNow()) { return false; }

        return !isInBoat();

    }

    public List<Integer> getFriendList() {
        return _friendList;
    }

    public void selectFriend(Integer friendId) {
        if (!_selectedFriendList.contains(friendId)) { _selectedFriendList.add(friendId); }
    }

    public void deselectFriend(Integer friendId) {
        if (_selectedFriendList.contains(friendId)) { _selectedFriendList.remove(friendId); }
    }

    public List<Integer> getSelectedFriendList() {
        return _selectedFriendList;
    }

    private void restoreFriendList() {
        _friendList.clear();

        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("SELECT friend_id FROM character_friends WHERE char_id = ? AND relation = 0");
            statement.setInt(1, getObjectId());
            ResultSet rset = statement.executeQuery();

            int friendId;
            while (rset.next()) {
                friendId = rset.getInt("friend_id");
                if (friendId == getObjectId()) { continue; }

                _friendList.add(friendId);
            }

            rset.close();
            statement.close();
        }
        catch (Exception e) {
            _log.log(Level.WARNING, "Error found in " + getName() + "'s friendlist: " + e.getMessage(), e);
        }
    }

    private void notifyFriends(boolean login) {
        for (int id : _friendList) {
            L2PcInstance friend = L2World.getInstance().getPlayer(id);
            if (friend != null) {
                friend.sendPacket(new FriendList(friend));

                if (login) {
                    friend.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.FRIEND_S1_HAS_LOGGED_IN).addPcName(this));
                }
            }
        }
    }

    public void selectBlock(Integer friendId) {
        if (!_selectedBlocksList.contains(friendId)) { _selectedBlocksList.add(friendId); }
    }

    public void deselectBlock(Integer friendId) {
        if (_selectedBlocksList.contains(friendId)) { _selectedBlocksList.remove(friendId); }
    }

    public List<Integer> getSelectedBlocksList() {
        return _selectedBlocksList;
    }

    @Override
    public void broadcastRelationsChanges() {
        for (L2PcInstance player : getKnownList().getKnownType(L2PcInstance.class)) {
            player.sendPacket(new RelationChanged(this, getRelationTo(player), isAutoAttackable(player)));
            if (summon != null) {
                player.sendPacket(new RelationChanged(summon, getRelationTo(player), isAutoAttackable(player)));
            }
        }
    }

    @Override
    public void sendInfo(L2PcInstance activeChar) {
        if (isInBoat()) { getPosition().setWorldPosition(getBoat().getPosition().getWorldPosition()); }

        if (getPoly().isMorphed()) {
            activeChar.sendPacket(new PcMorphInfo(this, getPoly().getNpcTemplate()));
        }
        else {
            activeChar.sendPacket(new CharInfo(this));
            if (isSeated()) {
                L2Object object = L2World.getInstance().getObject(_mountObjectID);
                if (object instanceof L2StaticObjectInstance) {
                    activeChar.sendPacket(new ChairSit(getObjectId(), ((L2StaticObjectInstance) object).getStaticObjectId()));
                }
            }
        }

        int relationOther = getRelationTo(activeChar);
        activeChar.sendPacket(new RelationChanged(this, relationOther, isAutoAttackable(activeChar)));
        if (summon != null) {
            activeChar.sendPacket(new RelationChanged(summon, relationOther, isAutoAttackable(activeChar)));
        }

        int relation = activeChar.getRelationTo(this);
        sendPacket(new RelationChanged(activeChar, relation, activeChar.isAutoAttackable(this)));
        if (activeChar.summon != null) {
            sendPacket(new RelationChanged(activeChar.summon, relation, activeChar.isAutoAttackable(this)));
        }

        if (isInBoat()) {
            activeChar.sendPacket(new GetOnVehicle(getObjectId(), getBoat().getObjectId(), _inVehiclePosition));
        }

        switch (_privateStoreType) {
            case SELL:
            case PACKAGE_SELL:
                activeChar.sendPacket(new PrivateStoreMsgSell(this));
                break;
            case BUY:
                activeChar.sendPacket(new PrivateStoreMsgBuy(this));
                break;
            case MANUFACTURE:
                activeChar.sendPacket(new RecipeShopMsg(this));
                break;
        }
    }

    @Override
    public boolean isPlayer() {
        return true;
    }

    private class ShortBuffTask implements Runnable {
        @Override
        public void run() {
            sendPacket(new ShortBuffStatusUpdate(0, 0, 0));
            setShortBuffTaskSkillId(0);
        }
    }

    private class SitDownTask implements Runnable {
        @Override
        public void run() {
            setIsParalyzed(false);
        }
    }

    private class StandUpTask implements Runnable {
        @Override
        public void run() {
            setIsSitting(false);
            setIsParalyzed(false);
            getAI().setIntention(EIntention.IDLE);
        }
    }

    private class ProtectTask implements Runnable {
        @Override
        public void run() {
            setProtection(false);
            sendMessage("The spawn protection has ended.");
        }
    }

    private class FeedTask implements Runnable {
        @Override
        public void run() {
            try {
                if (!isMounted()) {
                    stopFeed();
                    return;
                }

                if (getCurrentFeed() > getFeedConsume()) {
                    // eat
                    setCurrentFeed(getCurrentFeed() - getFeedConsume());
                }
                else {
                    // go back to pet control item, or simply said, unsummon it
                    setCurrentFeed(0);
                    stopFeed();
                    dismount();
                    sendPacket(SystemMessageId.OUT_OF_FEED_MOUNT_CANCELED);
                }

                int[] foodIds = getPetData(getMountNpcId()).getFood();
                if (foodIds.length == 0) { return; }

                L2ItemInstance food = null;
                for (int id : foodIds) {
                    food = getInventory().getItemByItemId(id);
                    if (food != null) { break; }
                }

                if (food != null && isHungry()) {
                    IItemHandler handler = ItemHandler.getInstance().getItemHandler(food.getEtcItem());
                    if (handler != null) {
                        handler.useItem(L2PcInstance.this, food, false);
                        sendPacket(SystemMessage.getSystemMessage(SystemMessageId.PET_TOOK_S1_BECAUSE_HE_WAS_HUNGRY).addItemName(food));
                    }
                }
            }
            catch (Exception e) {
                _log.log(Level.SEVERE, "Mounted Pet [NpcId: " + getMountNpcId() + "] a feed task error has occurred", e);
            }
        }
    }

    private class InventoryEnable implements Runnable {
        @Override
        public void run() {
            _inventoryDisable = false;
        }
    }

    private class LookingForFishTask implements Runnable {
        boolean _isNoob, _isUpperGrade;
        int _fishType, _fishGutsCheck;
        long _endTaskTime;

        protected LookingForFishTask(int fishWaitTime, int fishGutsCheck, int fishType, boolean isNoob, boolean isUpperGrade) {
            _fishGutsCheck = fishGutsCheck;
            _endTaskTime = System.currentTimeMillis() + fishWaitTime + 10000;
            _fishType = fishType;
            _isNoob = isNoob;
            _isUpperGrade = isUpperGrade;
        }

        @Override
        public void run() {
            if (System.currentTimeMillis() >= _endTaskTime) {
                endFishing(false);
                return;
            }

            if (_fishType == -1) { return; }

            int check = Rnd.get(1000);
            if (_fishGutsCheck > check) {
                stopLookingForFishTask();
                startFishCombat(_isNoob, _isUpperGrade);
            }
        }
    }

    private class PunishTask implements Runnable {
        @Override
        public void run() {
            setPunishLevel(PunishLevel.NONE, 0);
        }
    }

    private class Dismount implements Runnable {
        @Override
        public void run() {
            try {
                dismount();
            }
            catch (Exception e) {
                _log.log(Level.WARNING, "Exception on dismount(): " + e.getMessage(), e);
            }
        }
    }

    private class ChargeTask implements Runnable {
        @Override
        public void run() {
            clearCharges();
        }
    }
}