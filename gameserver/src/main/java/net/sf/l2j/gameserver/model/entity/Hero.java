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

/**
 * @author godson
 */
package net.sf.l2j.gameserver.model.entity;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.gameserver.datatables.CharNameTable;
import net.sf.l2j.gameserver.datatables.CharTemplateTable;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.item.EPaperdollSlot;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.model.world.L2World;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.*;
import net.sf.l2j.gameserver.templates.StatsSet;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Hero {
    private static Logger _log = Logger.getLogger(Hero.class.getName());

    private static final String GET_HEROES = "SELECT heroes.char_id, characters.char_name, heroes.class_id, heroes.count, heroes.played, heroes.active FROM heroes, characters WHERE characters.obj_Id = heroes.char_id AND heroes.played = 1";
    private static final String GET_ALL_HEROES = "SELECT heroes.char_id, characters.char_name, heroes.class_id, heroes.count, heroes.played, heroes.active FROM heroes, characters WHERE characters.obj_Id = heroes.char_id";
    private static final String UPDATE_ALL = "UPDATE heroes SET played = 0";
    private static final String INSERT_HERO = "INSERT INTO heroes (char_id, class_id, count, played, active) VALUES (?,?,?,?,?)";
    private static final String UPDATE_HERO = "UPDATE heroes SET count = ?, played = ?, active = ? WHERE char_id = ?";
    private static final String GET_CLAN_ALLY = "SELECT characters.clanid AS clanid, coalesce(clan_data.ally_Id, 0) AS allyId FROM characters LEFT JOIN clan_data ON clan_data.clan_id = characters.clanid WHERE characters.obj_Id = ?";

    private static final String DELETE_ITEMS = "DELETE FROM items WHERE item_id IN (6842, 6611, 6612, 6613, 6614, 6615, 6616, 6617, 6618, 6619, 6620, 6621) AND owner_id NOT IN (SELECT obj_Id FROM characters WHERE accesslevel > 0)";

    private static final String GET_DIARIES = "SELECT * FROM  heroes_diary WHERE char_id=? ORDER BY time ASC";
    private static final String UPDATE_DIARIES = "INSERT INTO heroes_diary (char_id, time, action, param) VALUES(?,?,?,?)";

    private static Map<Integer, StatsSet> _heroes;
    private static Map<Integer, StatsSet> _completeHeroes;

    private static Map<Integer, StatsSet> _herocounts;
    private static Map<Integer, List<StatsSet>> _herofights;
    private static List<StatsSet> _fights;

    private static Map<Integer, List<StatsSet>> _herodiary;
    private static Map<Integer, String> _heroMessage;
    private static List<StatsSet> _diary;

    public static final String COUNT = "count";
    public static final String PLAYED = "played";
    public static final String CLAN_NAME = "clan_name";
    public static final String CLAN_CREST = "clan_crest";
    public static final String ALLY_NAME = "ally_name";
    public static final String ALLY_CREST = "ally_crest";
    public static final String ACTIVE = "active";

    public static final int ACTION_RAID_KILLED = 1;
    public static final int ACTION_HERO_GAINED = 2;
    public static final int ACTION_CASTLE_TAKEN = 3;

    public static Hero getInstance() {
        return SingletonHolder._instance;
    }

    protected Hero() {
        init();
    }

    private void init() {
        _heroes = new HashMap<>();
        _completeHeroes = new HashMap<>();

        _herofights = new HashMap<>();
        _herocounts = new HashMap<>();
        _herodiary = new HashMap<>();
        _heroMessage = new HashMap<>();

        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement(GET_HEROES);
            ResultSet rset = statement.executeQuery();
            PreparedStatement statement2 = con.prepareStatement(GET_CLAN_ALLY);
            ResultSet rset2 = null;

            while (rset.next()) {
                StatsSet hero = new StatsSet();
                int charId = rset.getInt(Olympiad.CHAR_ID);
                hero.set(Olympiad.CHAR_NAME, rset.getString(Olympiad.CHAR_NAME));
                hero.set(Olympiad.CLASS_ID, rset.getInt(Olympiad.CLASS_ID));
                hero.set(COUNT, rset.getInt(COUNT));
                hero.set(PLAYED, rset.getInt(PLAYED));
                hero.set(ACTIVE, rset.getInt(ACTIVE));

                loadFights(charId);
                loadDiary(charId);
                loadMessage(charId);

                statement2.setInt(1, charId);
                rset2 = statement2.executeQuery();

                if (rset2.next()) {
                    int clanId = rset2.getInt("clanid");
                    int allyId = rset2.getInt("allyId");

                    String clanName = "";
                    String allyName = "";
                    int clanCrest = 0;
                    int allyCrest = 0;

                    if (clanId > 0) {
                        clanName = ClanTable.getInstance().getClan(clanId).getName();
                        clanCrest = ClanTable.getInstance().getClan(clanId).getCrestId();

                        if (allyId > 0) {
                            allyName = ClanTable.getInstance().getClan(clanId).getAllyName();
                            allyCrest = ClanTable.getInstance().getClan(clanId).getAllyCrestId();
                        }
                    }

                    hero.set(CLAN_CREST, clanCrest);
                    hero.set(CLAN_NAME, clanName);
                    hero.set(ALLY_CREST, allyCrest);
                    hero.set(ALLY_NAME, allyName);
                }

                rset2.close();
                statement2.clearParameters();

                _heroes.put(charId, hero);
            }

            rset.close();
            statement.close();

            statement = con.prepareStatement(GET_ALL_HEROES);
            rset = statement.executeQuery();

            while (rset.next()) {
                StatsSet hero = new StatsSet();
                int charId = rset.getInt(Olympiad.CHAR_ID);
                hero.set(Olympiad.CHAR_NAME, rset.getString(Olympiad.CHAR_NAME));
                hero.set(Olympiad.CLASS_ID, rset.getInt(Olympiad.CLASS_ID));
                hero.set(COUNT, rset.getInt(COUNT));
                hero.set(PLAYED, rset.getInt(PLAYED));
                hero.set(ACTIVE, rset.getInt(ACTIVE));

                statement2.setInt(1, charId);
                rset2 = statement2.executeQuery();

                if (rset2.next()) {
                    int clanId = rset2.getInt("clanid");
                    int allyId = rset2.getInt("allyId");

                    String clanName = "";
                    String allyName = "";
                    int clanCrest = 0;
                    int allyCrest = 0;

                    if (clanId > 0) {
                        clanName = ClanTable.getInstance().getClan(clanId).getName();
                        clanCrest = ClanTable.getInstance().getClan(clanId).getCrestId();

                        if (allyId > 0) {
                            allyName = ClanTable.getInstance().getClan(clanId).getAllyName();
                            allyCrest = ClanTable.getInstance().getClan(clanId).getAllyCrestId();
                        }
                    }

                    hero.set(CLAN_CREST, clanCrest);
                    hero.set(CLAN_NAME, clanName);
                    hero.set(ALLY_CREST, allyCrest);
                    hero.set(ALLY_NAME, allyName);
                }

                rset2.close();
                statement2.clearParameters();

                _completeHeroes.put(charId, hero);
            }

            statement2.close();
            rset.close();
            statement.close();
        }
        catch (SQLException e) {
            _log.log(Level.WARNING, "Hero: Couldnt load heroes: " + e.getMessage(), e);
        }

        _log.info("Hero: Loaded " + _heroes.size() + " heroes.");
        _log.info("Hero: Loaded " + _completeHeroes.size() + " all time heroes.");
    }

    private static String calcFightTime(long FightTime) {
        String format = String.format("%%0%dd", 2);
        FightTime = FightTime / 1000;
        String seconds = String.format(format, FightTime % 60);
        String minutes = String.format(format, (FightTime % 3600) / 60);
        String time = minutes + ":" + seconds;
        return time;
    }

    /**
     * Restore hero message from Db.
     *
     * @param charId
     */
    public void loadMessage(int charId) {
        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            String message = null;
            PreparedStatement statement = con.prepareStatement("SELECT message FROM heroes WHERE char_id=?");
            statement.setInt(1, charId);
            ResultSet rset = statement.executeQuery();
            rset.next();
            message = rset.getString("message");
            _heroMessage.put(charId, message);
            rset.close();
            statement.close();
        }
        catch (SQLException e) {
            _log.log(Level.WARNING, "Hero: Couldnt load hero message for char_id: " + charId, e);
        }
    }

    public void loadDiary(int charId) {
        _diary = new ArrayList<>();

        int diaryentries = 0;
        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement(GET_DIARIES);
            statement.setInt(1, charId);
            ResultSet rset = statement.executeQuery();

            while (rset.next()) {
                StatsSet _diaryentry = new StatsSet();

                long time = rset.getLong("time");
                int action = rset.getInt("action");
                int param = rset.getInt("param");

                _diaryentry.set("date", new SimpleDateFormat("yyyy-MM-dd HH").format(time));

                if (action == ACTION_RAID_KILLED) {
                    NpcTemplate template = NpcTable.getInstance().getTemplate(param);
                    if (template != null) { _diaryentry.set("action", template.getName() + " was defeated"); }
                }
                else if (action == ACTION_HERO_GAINED) { _diaryentry.set("action", "Gained Hero status"); }
                else if (action == ACTION_CASTLE_TAKEN) {
                    Castle castle = CastleManager.getInstance().getCastleById(param);
                    if (castle != null) { _diaryentry.set("action", castle.getName() + " Castle was successfuly taken"); }
                }
                _diary.add(_diaryentry);
                diaryentries++;
            }
            rset.close();
            statement.close();

            _herodiary.put(charId, _diary);

            _log.info("Hero: Loaded " + diaryentries + " diary entries for hero: " + CharNameTable.getInstance().getNameById(charId));
        }
        catch (SQLException e) {
            _log.log(Level.WARNING, "Hero: Couldnt load hero diary for char_id: " + charId + ", " + e.getMessage(), e);
        }
    }

    public void loadFights(int charId) {
        _fights = new ArrayList<>();

        StatsSet _herocountdata = new StatsSet();

        Calendar _data = Calendar.getInstance();
        _data.set(Calendar.DAY_OF_MONTH, 1);
        _data.set(Calendar.HOUR_OF_DAY, 0);
        _data.set(Calendar.MINUTE, 0);
        _data.set(Calendar.MILLISECOND, 0);

        long from = _data.getTimeInMillis();
        int numberoffights = 0;
        int _victorys = 0;
        int _losses = 0;
        int _draws = 0;

        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("SELECT * FROM olympiad_fights WHERE (charOneId=? OR charTwoId=?) AND start<? ORDER BY start ASC");
            statement.setInt(1, charId);
            statement.setInt(2, charId);
            statement.setLong(3, from);
            ResultSet rset = statement.executeQuery();

            while (rset.next()) {
                int charOneId = rset.getInt("charOneId");
                int charOneClass = rset.getInt("charOneClass");
                int charTwoId = rset.getInt("charTwoId");
                int charTwoClass = rset.getInt("charTwoClass");
                int winner = rset.getInt("winner");
                long start = rset.getLong("start");
                int time = rset.getInt("time");
                int classed = rset.getInt("classed");

                if (charId == charOneId) {
                    String name = CharNameTable.getInstance().getNameById(charTwoId);
                    String cls = CharTemplateTable.getInstance().getClassNameById(charTwoClass);
                    if (name != null && cls != null) {
                        StatsSet fight = new StatsSet();
                        fight.set("oponent", name);
                        fight.set("oponentclass", cls);

                        fight.set("time", calcFightTime(time));
                        fight.set("start", new SimpleDateFormat("yyyy-MM-dd HH:mm").format(start));

                        fight.set("classed", classed);
                        if (winner == 1) {
                            fight.set("result", "<font color=\"00ff00\">victory</font>");
                            _victorys++;
                        }
                        else if (winner == 2) {
                            fight.set("result", "<font color=\"ff0000\">loss</font>");
                            _losses++;
                        }
                        else if (winner == 0) {
                            fight.set("result", "<font color=\"ffff00\">draw</font>");
                            _draws++;
                        }

                        _fights.add(fight);

                        numberoffights++;
                    }
                }
                else if (charId == charTwoId) {
                    String name = CharNameTable.getInstance().getNameById(charOneId);
                    String cls = CharTemplateTable.getInstance().getClassNameById(charOneClass);
                    if (name != null && cls != null) {
                        StatsSet fight = new StatsSet();
                        fight.set("oponent", name);
                        fight.set("oponentclass", cls);

                        fight.set("time", calcFightTime(time));
                        fight.set("start", new SimpleDateFormat("yyyy-MM-dd HH:mm").format(start));

                        fight.set("classed", classed);
                        if (winner == 1) {
                            fight.set("result", "<font color=\"ff0000\">loss</font>");
                            _losses++;
                        }
                        else if (winner == 2) {
                            fight.set("result", "<font color=\"00ff00\">victory</font>");
                            _victorys++;
                        }
                        else if (winner == 0) {
                            fight.set("result", "<font color=\"ffff00\">draw</font>");
                            _draws++;
                        }

                        _fights.add(fight);

                        numberoffights++;
                    }
                }
            }
            rset.close();
            statement.close();

            _herocountdata.set("victory", _victorys);
            _herocountdata.set("draw", _draws);
            _herocountdata.set("loss", _losses);

            _herocounts.put(charId, _herocountdata);
            _herofights.put(charId, _fights);

            _log.info("Hero: Loaded " + numberoffights + " fights for: " + CharNameTable.getInstance().getNameById(charId));
        }
        catch (SQLException e) {
            _log.log(Level.WARNING, "Hero: Couldnt load hero fights history for char_id: " + charId + ", " + e.getMessage(), e);
        }
    }

    public Map<Integer, StatsSet> getHeroes() {
        return _heroes;
    }

    public int getHeroByClass(int classid) {
        if (!_heroes.isEmpty()) {
            for (Map.Entry<Integer, StatsSet> heroEntry : _heroes.entrySet()) {
                if (heroEntry.getValue().getInteger(Olympiad.CLASS_ID) == classid) { return heroEntry.getKey(); }
            }
        }
        return 0;
    }

    public void resetData() {
        _herodiary.clear();
        _herofights.clear();
        _herocounts.clear();
        _heroMessage.clear();
    }

    public void showHeroDiary(L2PcInstance activeChar, int heroclass, int charid, int page) {
        if (!_herodiary.containsKey(charid)) { return; }

        final int perpage = 10;

        List<StatsSet> _mainlist = _herodiary.get(charid);

        final NpcHtmlMessage html = new NpcHtmlMessage(0);
        html.setFile("data/html/olympiad/herodiary.htm");
        html.replace("%heroname%", CharNameTable.getInstance().getNameById(charid));
        html.replace("%message%", _heroMessage.get(charid));
        html.disableValidation();

        if (!_mainlist.isEmpty()) {
            List<StatsSet> _list = new ArrayList<>();
            _list.addAll(_mainlist);
            Collections.reverse(_list);

            boolean color = true;
            int counter = 0;
            int breakat = 0;

            final StringBuilder sb = new StringBuilder(500);
            for (int i = ((page - 1) * perpage); i < _list.size(); i++) {
                breakat = i;
                StatsSet _diaryentry = _list.get(i);
                StringUtil.append(sb, "<tr><td>", ((color) ? "<table width=270 bgcolor=\"131210\">" : "<table width=270>"), "<tr><td width=270><font color=\"LEVEL\">", _diaryentry.getString("date"), ":xx</font></td></tr><tr><td width=270>", _diaryentry.getString("action"), "</td></tr><tr><td>&nbsp;</td></tr></table></td></tr>");
                color = !color;

                counter++;
                if (counter >= perpage) { break; }
            }

            if (breakat < (_list.size() - 1)) {
                html.replace("%buttprev%", "<button value=\"Prev\" action=\"bypass _diary?class=" + heroclass + "&page=" + (page + 1) + "\" width=60 height=25 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
            }
            else { html.replace("%buttprev%", ""); }

            if (page > 1) { html.replace("%buttnext%", "<button value=\"Next\" action=\"bypass _diary?class=" + heroclass + "&page=" + (page - 1) + "\" width=60 height=25 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">"); }
            else { html.replace("%buttnext%", ""); }

            html.replace("%list%", sb.toString());
        }
        else {
            html.replace("%list%", "");
            html.replace("%buttprev%", "");
            html.replace("%buttnext%", "");
        }
        activeChar.sendPacket(html);
    }

    public void showHeroFights(L2PcInstance activeChar, int heroclass, int charid, int page) {
        if (!_herofights.containsKey(charid)) { return; }

        final int perpage = 20;
        int _win = 0;
        int _loss = 0;
        int _draw = 0;

        List<StatsSet> _list = _herofights.get(charid);

        final NpcHtmlMessage html = new NpcHtmlMessage(0);
        html.setFile("data/html/olympiad/herohistory.htm");
        html.replace("%heroname%", CharNameTable.getInstance().getNameById(charid));
        html.disableValidation();

        if (!_list.isEmpty()) {
            if (_herocounts.containsKey(charid)) {
                StatsSet _herocount = _herocounts.get(charid);
                _win = _herocount.getInteger("victory");
                _loss = _herocount.getInteger("loss");
                _draw = _herocount.getInteger("draw");
            }

            boolean color = true;
            int counter = 0;
            int breakat = 0;

            final StringBuilder sb = new StringBuilder(500);
            for (int i = ((page - 1) * perpage); i < _list.size(); i++) {
                breakat = i;
                StatsSet fight = _list.get(i);
                StringUtil.append(sb, "<tr><td>", ((color) ? "<table width=270 bgcolor=\"131210\">" : "<table width=270><tr><td width=220><font color=\"LEVEL\">"), fight.getString("start"), "</font>&nbsp;&nbsp;", fight.getString("result"), "</td><td width=50 align=right>", ((fight.getInteger("classed") > 0) ? "<font color=\"FFFF99\">cls</font>" : "<font color=\"999999\">non-cls<font>"), "</td></tr><tr><td width=220>vs ", fight.getString("oponent"), " (", fight.getString("oponentclass"), ")</td><td width=50 align=right>(", fight.getString("time"), ")</td></tr><tr><td colspan=2>&nbsp;</td></tr></table></td></tr>");
                color = !color;

                counter++;
                if (counter >= perpage) { break; }
            }

            if (breakat < (_list.size() - 1)) {
                html.replace("%buttprev%", "<button value=\"Prev\" action=\"bypass _match?class=" + heroclass + "&page=" + (page + 1) + "\" width=60 height=25 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
            }
            else { html.replace("%buttprev%", ""); }

            if (page > 1) { html.replace("%buttnext%", "<button value=\"Next\" action=\"bypass _match?class=" + heroclass + "&page=" + (page - 1) + "\" width=60 height=25 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">"); }
            else { html.replace("%buttnext%", ""); }

            html.replace("%list%", sb.toString());
        }
        else {
            html.replace("%list%", "");
            html.replace("%buttprev%", "");
            html.replace("%buttnext%", "");
        }

        html.replace("%win%", _win);
        html.replace("%draw%", _draw);
        html.replace("%loos%", _loss);

        activeChar.sendPacket(html);
    }

    public synchronized void computeNewHeroes(Collection<StatsSet> newHeroes) {
        updateHeroes(true);

        if (!_heroes.isEmpty()) {
            for (StatsSet hero : _heroes.values()) {
                String name = hero.getString(Olympiad.CHAR_NAME);

                L2PcInstance player = L2World.getInstance().getPlayer(name);
                if (player == null) { continue; }

                try {
                    player.setHero(false);

                    for (EPaperdollSlot paperdollSlot : EPaperdollSlot.values()) {
                        L2ItemInstance equippedItem = player.getInventory().getPaperdollItem(paperdollSlot);
                        if ((equippedItem != null) && equippedItem.isHeroItem()) {
                            player.getInventory().unEquipItemInSlot(paperdollSlot);
                        }
                    }

                    for (L2ItemInstance item : player.getInventory().getAvailableItems(false, false)) {
                        if ((item != null) && item.isHeroItem()) {
                            player.destroyItem("Hero", item, null, true);
                            InventoryUpdate iu = new InventoryUpdate();
                            iu.addRemovedItem(item);
                            player.sendPacket(iu);
                        }
                    }

                    player.broadcastUserInfo();
                }
                catch (NullPointerException e) {
                }
            }
        }

        if (newHeroes.isEmpty()) {
            _heroes.clear();
            return;
        }

        Map<Integer, StatsSet> heroes = new HashMap<>();

        for (StatsSet hero : newHeroes) {
            int charId = hero.getInteger(Olympiad.CHAR_ID);

            if ((_completeHeroes != null) && _completeHeroes.containsKey(charId)) {
                StatsSet oldHero = _completeHeroes.get(charId);
                int count = oldHero.getInteger(COUNT);
                oldHero.set(COUNT, count + 1);
                oldHero.set(PLAYED, 1);
                oldHero.set(ACTIVE, 0);

                heroes.put(charId, oldHero);
            }
            else {
                StatsSet newHero = new StatsSet();
                newHero.set(Olympiad.CHAR_NAME, hero.getString(Olympiad.CHAR_NAME));
                newHero.set(Olympiad.CLASS_ID, hero.getInteger(Olympiad.CLASS_ID));
                newHero.set(COUNT, 1);
                newHero.set(PLAYED, 1);
                newHero.set(ACTIVE, 0);

                heroes.put(charId, newHero);
            }
        }

        deleteItemsInDb();

        _heroes.clear();
        _heroes.putAll(heroes);

        heroes.clear();

        updateHeroes(false);
    }

    public void updateHeroes(boolean setDefault) {
        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            if (setDefault) {
                PreparedStatement statement = con.prepareStatement(UPDATE_ALL);
                statement.execute();
                statement.close();
            }
            else {
                PreparedStatement statement;

                for (Map.Entry<Integer, StatsSet> heroEntry : _heroes.entrySet()) {
                    final int heroId = heroEntry.getKey();
                    final StatsSet hero = heroEntry.getValue();

                    if (_completeHeroes == null || !_completeHeroes.containsKey(heroId)) {
                        statement = con.prepareStatement(INSERT_HERO);
                        statement.setInt(1, heroId);
                        statement.setInt(2, hero.getInteger(Olympiad.CLASS_ID));
                        statement.setInt(3, hero.getInteger(COUNT));
                        statement.setInt(4, hero.getInteger(PLAYED));
                        statement.setInt(5, hero.getInteger(ACTIVE));
                        statement.execute();
                        statement.close();

                        statement = con.prepareStatement(GET_CLAN_ALLY);
                        statement.setInt(1, heroId);
                        ResultSet rset = statement.executeQuery();

                        if (rset.next()) {
                            int clanId = rset.getInt("clanid");
                            int allyId = rset.getInt("allyId");

                            String clanName = "";
                            String allyName = "";
                            int clanCrest = 0;
                            int allyCrest = 0;

                            if (clanId > 0) {
                                clanName = ClanTable.getInstance().getClan(clanId).getName();
                                clanCrest = ClanTable.getInstance().getClan(clanId).getCrestId();

                                if (allyId > 0) {
                                    allyName = ClanTable.getInstance().getClan(clanId).getAllyName();
                                    allyCrest = ClanTable.getInstance().getClan(clanId).getAllyCrestId();
                                }
                            }

                            hero.set(CLAN_CREST, clanCrest);
                            hero.set(CLAN_NAME, clanName);
                            hero.set(ALLY_CREST, allyCrest);
                            hero.set(ALLY_NAME, allyName);
                        }

                        rset.close();
                        statement.close();

                        _heroes.put(heroId, hero);
                        _completeHeroes.put(heroId, hero);
                    }
                    else {
                        statement = con.prepareStatement(UPDATE_HERO);
                        statement.setInt(1, hero.getInteger(COUNT));
                        statement.setInt(2, hero.getInteger(PLAYED));
                        statement.setInt(3, hero.getInteger(ACTIVE));
                        statement.setInt(4, heroId);
                        statement.execute();
                        statement.close();
                    }
                }
            }
        }
        catch (SQLException e) {
            _log.log(Level.WARNING, "Hero: Couldnt update heroes: " + e.getMessage(), e);
        }
    }

    public void setHeroGained(int charId) {
        setDiaryData(charId, ACTION_HERO_GAINED, 0);
    }

    public void setRBkilled(int charId, int npcId) {
        setDiaryData(charId, ACTION_RAID_KILLED, npcId);

        NpcTemplate template = NpcTable.getInstance().getTemplate(npcId);

        if (_herodiary.containsKey(charId) && (template != null)) {
            // Get Data
            List<StatsSet> _list = _herodiary.get(charId);

            // Clear old data
            _herodiary.remove(charId);

            // Prepare new data
            StatsSet _diaryentry = new StatsSet();
            _diaryentry.set("date", new SimpleDateFormat("yyyy-MM-dd HH").format(System.currentTimeMillis()));
            _diaryentry.set("action", template.getName() + " was defeated");

            // Add to old list
            _list.add(_diaryentry);

            // Put new list into diary
            _herodiary.put(charId, _list);
        }
    }

    public void setCastleTaken(int charId, int castleId) {
        setDiaryData(charId, ACTION_CASTLE_TAKEN, castleId);

        Castle castle = CastleManager.getInstance().getCastleById(castleId);

        if (_herodiary.containsKey(charId) && (castle != null)) {
            // Get Data
            List<StatsSet> _list = _herodiary.get(charId);

            // Clear old data
            _herodiary.remove(charId);

            // Prepare new data
            StatsSet _diaryentry = new StatsSet();
            _diaryentry.set("date", new SimpleDateFormat("yyyy-MM-dd HH").format(System.currentTimeMillis()));
            _diaryentry.set("action", castle.getName() + " Castle was successfuly taken");

            // Add to old list
            _list.add(_diaryentry);

            // Put new list into diary
            _herodiary.put(charId, _list);
        }
    }

    public void setDiaryData(int charId, int action, int param) {
        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement(UPDATE_DIARIES);
            statement.setInt(1, charId);
            statement.setLong(2, System.currentTimeMillis());
            statement.setInt(3, action);
            statement.setInt(4, param);
            statement.execute();
            statement.close();
        }
        catch (SQLException e) {
            _log.log(Level.SEVERE, "Hero: SQL exception while saving DiaryData.", e);
        }
    }

    /**
     * Set new hero message for hero
     *
     * @param player  the player instance
     * @param message String to set
     */
    public void setHeroMessage(L2PcInstance player, String message) {
        _heroMessage.put(player.getObjectId(), message);
    }

    /**
     * Update hero message in database
     *
     * @param charId character objid
     */
    public void saveHeroMessage(int charId) {
        if (_heroMessage.get(charId) == null) { return; }

        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("UPDATE heroes SET message=? WHERE char_id=?;");
            statement.setString(1, _heroMessage.get(charId));
            statement.setInt(2, charId);
            statement.execute();
            statement.close();
        }
        catch (SQLException e) {
            _log.log(Level.SEVERE, "Hero: SQL exception while saving HeroMessage.", e);
        }
    }

    private static void deleteItemsInDb() {
        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement(DELETE_ITEMS);
            statement.execute();
            statement.close();
        }
        catch (SQLException e) {
            _log.log(Level.WARNING, "Hero: Couldn't delete items on db: " + e.getMessage(), e);
        }
    }

    /**
     * Saving task for {@link Hero}<BR>
     * Save all hero messages to DB.
     */
    public void shutdown() {
        for (int charId : _heroMessage.keySet()) { saveHeroMessage(charId); }
    }

    public boolean isActiveHero(int id) {
        if (_heroes == null || _heroes.isEmpty()) { return false; }

        if (_heroes.containsKey(id) && _heroes.get(id).getInteger(ACTIVE) == 1) { return true; }

        return false;
    }

    public boolean isInactiveHero(int id) {
        if (_heroes == null || _heroes.isEmpty()) { return false; }

        if (_heroes.containsKey(id) && _heroes.get(id).getInteger(ACTIVE) == 0) { return true; }

        return false;
    }

    public void activateHero(L2PcInstance player) {
        StatsSet hero = _heroes.get(player.getObjectId());
        hero.set(ACTIVE, 1);

        _heroes.put(player.getObjectId(), hero);

        player.setHero(true);
        player.broadcastPacket(new SocialAction(player, 16));
        player.broadcastUserInfo();

        L2Clan clan = player.getClan();
        if (clan != null && clan.getLevel() >= 5) {
            String name = hero.getString("char_name");

            clan.addReputationScore(1000);
            clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan), SystemMessage.getSystemMessage(SystemMessageId.CLAN_MEMBER_S1_BECAME_HERO_AND_GAINED_S2_REPUTATION_POINTS).addString(name).addNumber(1000));
        }

        // Set Gained hero and reload data
        setHeroGained(player.getObjectId());
        loadFights(player.getObjectId());
        loadDiary(player.getObjectId());
        _heroMessage.put(player.getObjectId(), "");

        updateHeroes(false);
    }

    private static class SingletonHolder {
        protected static final Hero _instance = new Hero();
    }
}