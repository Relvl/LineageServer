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
package net.sf.l2j.gameserver.network.client.client_to_game;

import net.sf.l2j.Config;
import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.gameserver.datatables.CharNameTable;
import net.sf.l2j.gameserver.datatables.CharTemplateTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.datatables.SkillTreeTable;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.L2ShortCut;
import net.sf.l2j.gameserver.model.L2SkillLearn;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.template.PcTemplate;
import net.sf.l2j.gameserver.model.item.EItemProcessPurpose;
import net.sf.l2j.gameserver.model.item.EItemType2;
import net.sf.l2j.gameserver.model.item.L2ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.world.L2World;
import net.sf.l2j.gameserver.network.client.game_to_client.CharCreateFail;
import net.sf.l2j.gameserver.network.client.game_to_client.CharCreateOk;
import net.sf.l2j.gameserver.network.client.game_to_client.CharSelectInfo;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.ScriptManager;

@SuppressWarnings("unused")
public final class CharacterCreate extends L2GameClientPacket {
    // cSdddddddddddd
    private String _name;
    private int _race;
    private byte _sex;
    private int _classId;
    private int _int;
    private int _str;
    private int _con;
    private int _men;
    private int _dex;
    private int _wit;
    private byte _hairStyle;
    private byte _hairColor;
    private byte _face;

    @Override
    protected void readImpl() {
        _name = readS();
        _race = readD();
        _sex = (byte) readD();
        _classId = readD();
        _int = readD();
        _str = readD();
        _con = readD();
        _men = readD();
        _dex = readD();
        _wit = readD();
        _hairStyle = (byte) readD();
        _hairColor = (byte) readD();
        _face = (byte) readD();
    }

    @Override
    protected void runImpl() {
        if (_name.length() > 16) {
            sendPacket(new CharCreateFail(CharCreateFail.REASON_16_ENG_CHARS));
            return;
        }

        if (!StringUtil.isValidPlayerName(_name)) {
            sendPacket(new CharCreateFail(CharCreateFail.REASON_INCORRECT_NAME));
            return;
        }

        if (_face > 2 || _face < 0) {
            sendPacket(new CharCreateFail(CharCreateFail.REASON_CREATION_FAILED));
            return;
        }

        if (_hairStyle < 0 || (_sex == 0 && _hairStyle > 4) || (_sex != 0 && _hairStyle > 6)) {
            sendPacket(new CharCreateFail(CharCreateFail.REASON_CREATION_FAILED));
            return;
        }

        if (_hairColor > 3 || _hairColor < 0) {
            sendPacket(new CharCreateFail(CharCreateFail.REASON_CREATION_FAILED));
            return;
        }

        L2PcInstance newChar = null;
        PcTemplate template = null;

		/*
         * DrHouse: Since checks for duplicate names are done using SQL, lock must be held until data is written to DB as well.
		 */
        synchronized (CharNameTable.getInstance()) {
            if (CharNameTable.accountCharNumber(getClient().getAccountName()) >= 7) {
                sendPacket(new CharCreateFail(CharCreateFail.REASON_TOO_MANY_CHARACTERS));
                return;
            }

            if (CharNameTable.doesCharNameExist(_name)) {
                sendPacket(new CharCreateFail(CharCreateFail.REASON_NAME_ALREADY_EXISTS));
                return;
            }

            template = CharTemplateTable.getInstance().getTemplate(_classId);
            if (template == null || template.getClassBaseLevel() > 1) {
                sendPacket(new CharCreateFail(CharCreateFail.REASON_CREATION_FAILED));
                return;
            }

            int objectId = IdFactory.getInstance().getNextId();
            newChar = L2PcInstance.create(objectId, template, getClient().getAccountName(), _name, _hairStyle, _hairColor, _face, _sex != 0);
        }

        newChar.setCurrentCp(0);
        newChar.setCurrentHp(newChar.getMaxHp());
        newChar.setCurrentMp(newChar.getMaxMp());

        // send acknowledgement
        sendPacket(CharCreateOk.STATIC_PACKET);

        L2World.getInstance().addObject(newChar);

        newChar.addAdena(EItemProcessPurpose.INIT, Config.STARTING_ADENA, null, false);
        newChar.getPosition().setXYZInvisible(template.getSpawnX(), template.getSpawnY(), template.getSpawnZ());
        newChar.setTitle("");

        newChar.registerShortCut(new L2ShortCut(0, 0, 3, 2, -1, 1)); // attack shortcut
        newChar.registerShortCut(new L2ShortCut(3, 0, 3, 5, -1, 1)); // take shortcut
        newChar.registerShortCut(new L2ShortCut(10, 0, 3, 0, -1, 1)); // sit shortcut

        for (Item ia : template.getItems()) {
            L2ItemInstance item = newChar.getInventory().addItem(EItemProcessPurpose.INIT, ia.getItemId(), 1, newChar, null);
            if (item.getItemId() == 5588) // tutorial book shortcut
            { newChar.registerShortCut(new L2ShortCut(11, 0, 1, item.getObjectId(), -1, 1)); }

            if (item.isEquipable()) {
                if (newChar.getActiveWeaponItem() == null || item.getItem().getType2() == EItemType2.TYPE2_WEAPON) {
                    newChar.getInventory().equipItemAndRecord(item);
                }
            }
        }

        for (L2SkillLearn skill : SkillTreeTable.getInstance().getAvailableSkills(newChar, newChar.getClassId())) {
            newChar.addSkill(SkillTable.getInfo(skill.getId(), skill.getLevel()), true);
            if (skill.getId() == 1001 || skill.getId() == 1177) { newChar.registerShortCut(new L2ShortCut(1, 0, 2, skill.getId(), 1, 1)); }

            if (skill.getId() == 1216) { newChar.registerShortCut(new L2ShortCut(9, 0, 2, skill.getId(), 1, 1)); }
        }

        if (!Config.DISABLE_TUTORIAL) {
            if (newChar.getQuestState("Tutorial") == null) {
                Quest q = ScriptManager.getInstance().getQuest("Tutorial");
                if (q != null) { q.newQuestState(newChar).setState(Quest.STATE_STARTED); }
            }
        }

        newChar.setOnlineStatus(true, false);
        newChar.deleteMe();

        CharSelectInfo cl = new CharSelectInfo(getClient().getAccountName(), getClient().getSessionId().getPlayOkID1());
        getClient().getConnection().sendPacket(cl);
        getClient().setCharSelection(cl.getCharInfo());
    }
}