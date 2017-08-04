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
package net.sf.l2j.gameserver.handler.admincommandhandlers;

import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2ChestInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.world.L2World;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.AbstractNpcInfo.NpcInfo;
import net.sf.l2j.gameserver.network.client.game_to_client.*;
import net.sf.l2j.gameserver.util.Broadcast;

import java.util.StringTokenizer;

/**
 * This class handles following admin commands:
 * <ul>
 * <li>hide = makes yourself invisible or visible.</li>
 * <li>earthquake = causes an earthquake of a given intensity and duration around you.</li>
 * <li>gmspeed = temporary Super Haste effect.</li>
 * <li>para/unpara = paralyze/remove paralysis from target.</li>
 * <li>para_all/unpara_all = same as para/unpara, affects the whole world.</li>
 * <li>polyself/unpolyself = makes you look as a specified mob.</li>
 * <li>changename = temporary change name.</li>
 * <li>social = forces an L2Character instance to broadcast social action packets.</li>
 * <li>effect = forces an L2Character instance to broadcast MSU packets.</li>
 * <li>abnormal = force changes over an L2Character instance's abnormal state.</li>
 * <li>play_sound/jukebox = Music broadcasting related commands.</li>
 * <li>atmosphere = sky change related commands.</li>
 * </ul>
 */
public class AdminEffects implements IAdminCommandHandler {
    private static final String[] ADMIN_COMMANDS =
            {
                    "admin_hide",
                    "admin_earthquake",
                    "admin_earthquake_menu",
                    "admin_gmspeed",
                    "admin_gmspeed_menu",
                    "admin_unpara_all",
                    "admin_para_all",
                    "admin_unpara",
                    "admin_para",
                    "admin_unpara_all_menu",
                    "admin_para_all_menu",
                    "admin_unpara_menu",
                    "admin_para_menu",
                    "admin_changename",
                    "admin_changename_menu",
                    "admin_social",
                    "admin_social_menu",
                    "admin_effect",
                    "admin_effect_menu",
                    "admin_abnormal",
                    "admin_abnormal_menu",
                    "admin_jukebox",
                    "admin_play_sound",
                    "admin_atmosphere",
                    "admin_atmosphere_menu"
            };

    @Override
    public boolean useAdminCommand(String command, L2PcInstance activeChar) {
        StringTokenizer st = new StringTokenizer(command);
        st.nextToken();

        if (command.startsWith("admin_hide")) {
            if (!activeChar.isInvisible()) {
                activeChar.setInvisible();
                activeChar.decayMe();
                activeChar.broadcastUserInfo();
                activeChar.spawnMe();
            }
            else {
                activeChar.setVisible();
                activeChar.broadcastUserInfo();
            }
        }
        else if (command.startsWith("admin_earthquake")) {
            try {
                activeChar.broadcastPacket(new Earthquake(activeChar.getX(), activeChar.getY(), activeChar.getZ(), Integer.parseInt(st.nextToken()), Integer.parseInt(st.nextToken())));
            }
            catch (Exception e) {
                activeChar.sendMessage("Use: //earthquake <intensity> <duration>");
            }
        }
        else if (command.startsWith("admin_atmosphere")) {
            try {
                String type = st.nextToken();
                String state = st.nextToken();

                L2GameServerPacket packet = null;

                if (type.equals("signsky")) {
                    if (state.equals("dawn")) { packet = new SignsSky(2); }
                    else if (state.equals("dusk")) { packet = new SignsSky(1); }
                }
                else if (type.equals("sky")) {
                    if (state.equals("night")) { packet = SunSet.STATIC_PACKET; }
                    else if (state.equals("day")) { packet = SunRise.STATIC_PACKET; }
                    else if (state.equals("red")) { packet = new ExRedSky(10); }
                }
                else {
                    activeChar.sendMessage("Usage: //atmosphere <signsky dawn|dusk> <sky day|night|red>");
                }

                if (packet != null) { Broadcast.toAllOnlinePlayers(packet); }
            }
            catch (Exception ex) {
                activeChar.sendMessage("Usage: //atmosphere <signsky dawn|dusk> <sky day|night|red>");
            }
        }
        else if (command.startsWith("admin_jukebox")) {
            AdminHelpPage.showHelpPage(activeChar, "songs/songs.htm");
        }
        else if (command.startsWith("admin_play_sound")) {
            try {
                final String sound = command.substring(17);
                final PlaySound snd = (sound.contains(".")) ? new PlaySound(sound) : new PlaySound(1, sound, 0, 0, 0, 0, 0);

                activeChar.broadcastPacket(snd);
                activeChar.sendMessage("Playing " + sound + ".");
            }
            catch (StringIndexOutOfBoundsException e) {
            }
        }
        else if (command.startsWith("admin_para_all")) {
            for (L2PcInstance player : activeChar.getKnownList().getKnownType(L2PcInstance.class)) {
                if (!player.isGM()) {
                    player.startAbnormalEffect(0x0800);
                    player.setIsParalyzed(true);
                    player.broadcastPacket(new StopMove(player));
                }
            }
        }
        else if (command.startsWith("admin_unpara_all")) {
            for (L2PcInstance player : activeChar.getKnownList().getKnownType(L2PcInstance.class)) {
                player.stopAbnormalEffect(0x0800);
                player.setIsParalyzed(false);
            }
        }
        else if (command.startsWith("admin_para")) {
            final L2Object target = activeChar.getTarget();
            if (target instanceof L2Character) {
                final L2Character player = (L2Character) target;

                player.startAbnormalEffect(0x0800);
                player.setIsParalyzed(true);
                player.broadcastPacket(new StopMove(player));
            }
            else { activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET); }
        }
        else if (command.startsWith("admin_unpara")) {
            final L2Object target = activeChar.getTarget();
            if (target instanceof L2Character) {
                final L2Character player = (L2Character) target;

                player.stopAbnormalEffect(0x0800);
                player.setIsParalyzed(false);
            }
            else { activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET); }
        }
        else if (command.startsWith("admin_gmspeed")) {
            try {
                activeChar.stopSkillEffects(7029);

                final int val = Integer.parseInt(st.nextToken());
                if (val > 0 && val < 5) { activeChar.doCast(SkillTable.getInfo(7029, val)); }
            }
            catch (Exception e) {
                activeChar.sendMessage("Use: //gmspeed value (0-4).");
            }
            finally {
                activeChar.updateEffectIcons();
            }
        }
        else if (command.startsWith("admin_changename")) {
            try {
                String name = st.nextToken();
                String oldName = "null";

                L2Object target = activeChar.getTarget();
                L2Character player = null;

                if (target instanceof L2Character) {
                    player = (L2Character) target;
                    oldName = player.getName();
                }
                else {
                    player = activeChar;
                    oldName = activeChar.getName();
                }

                if (player instanceof L2PcInstance) { L2World.getInstance().removePlayer((L2PcInstance) player); }

                player.setName(name);

                if (player instanceof L2PcInstance) {
                    L2World.getInstance().addVisibleObject(player, null);
                    ((L2PcInstance) player).broadcastUserInfo();
                }
                else if (player instanceof L2Npc) { player.broadcastPacket(new NpcInfo((L2Npc) player, null)); }

                activeChar.sendMessage("Changed name from " + oldName + " to " + name + ".");
            }
            catch (Exception e) {
            }
        }
        else if (command.startsWith("admin_social")) {
            try {
                final int social = Integer.parseInt(st.nextToken());

                if (st.countTokens() == 2) {
                    final String targetOrRadius = st.nextToken();
                    if (targetOrRadius != null) {
                        L2PcInstance player = L2World.getInstance().getPlayer(targetOrRadius);
                        if (player != null) {
                            if (performSocial(social, player)) { activeChar.sendMessage(player.getName() + " was affected by your social request."); }
                            else { activeChar.sendPacket(SystemMessageId.NOTHING_HAPPENED); }
                        }
                        else {
                            final int radius = Integer.parseInt(targetOrRadius);

                            for (L2Object object : activeChar.getKnownList().getKnownTypeInRadius(L2Character.class, radius)) { performSocial(social, object); }

                            activeChar.sendMessage(radius + " units radius was affected by your social request.");
                        }
                    }
                }
                else if (st.countTokens() == 1) {
                    L2Object obj = activeChar.getTarget();
                    if (obj == null) { obj = activeChar; }

                    if (performSocial(social, obj)) { activeChar.sendMessage(obj.getName() + " was affected by your social request."); }
                    else { activeChar.sendPacket(SystemMessageId.NOTHING_HAPPENED); }
                }
                else if (!command.contains("menu")) {
                    activeChar.sendMessage("Usage: //social <social_id> [player_name|radius]");
                }
            }
            catch (Exception e) {
                activeChar.sendMessage("Usage: //social <social_id> [player_name|radius]");
            }
        }
        else if (command.startsWith("admin_abnormal")) {
            try {
                final int abnormal = Integer.decode("0x" + st.nextToken());

                if (st.countTokens() == 2) {
                    final String targetOrRadius = st.nextToken();
                    if (targetOrRadius != null) {
                        L2PcInstance player = L2World.getInstance().getPlayer(targetOrRadius);
                        if (player != null) {
                            if (performAbnormal(abnormal, player)) { activeChar.sendMessage(player.getName() + " was affected by your abnormal request."); }
                            else { activeChar.sendPacket(SystemMessageId.NOTHING_HAPPENED); }
                        }
                        else {
                            final int radius = Integer.parseInt(targetOrRadius);

                            for (L2Object object : activeChar.getKnownList().getKnownTypeInRadius(L2Character.class, radius)) { performAbnormal(abnormal, object); }

                            activeChar.sendMessage(radius + " units radius was affected by your abnormal request.");
                        }
                    }
                }
                else if (st.countTokens() == 1) {
                    L2Object obj = activeChar.getTarget();
                    if (obj == null) { obj = activeChar; }

                    if (performAbnormal(abnormal, obj)) { activeChar.sendMessage(obj.getName() + " was affected by your abnormal request."); }
                    else { activeChar.sendPacket(SystemMessageId.NOTHING_HAPPENED); }
                }
                else if (!command.contains("menu")) {
                    activeChar.sendMessage("Usage: //abnormal <abnormal_mask> [player_name|radius]");
                }
            }
            catch (Exception e) {
                activeChar.sendMessage("Usage: //abnormal <hex_abnormal_mask> [player|radius]");
            }
        }
        else if (command.startsWith("admin_effect")) {
            try {
                L2Object obj = activeChar.getTarget();
                int level = 1, hittime = 1;
                int skill = Integer.parseInt(st.nextToken());

                if (st.hasMoreTokens()) { level = Integer.parseInt(st.nextToken()); }
                if (st.hasMoreTokens()) { hittime = Integer.parseInt(st.nextToken()); }

                if (obj == null) { obj = activeChar; }

                if (!(obj instanceof L2Character)) { activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET); }
                else {
                    L2Character target = (L2Character) obj;
                    target.broadcastPacket(new MagicSkillUse(target, activeChar, skill, level, hittime, 0));
                    activeChar.sendMessage(obj.getName() + " performs MSU " + skill + "/" + level + " by your request.");
                }
            }
            catch (Exception e) {
                activeChar.sendMessage("Usage: //effect skill [level | level hittime]");
            }
        }

        if (command.contains("menu")) {
            String filename = "effects_menu.htm";
            if (command.contains("abnormal")) { filename = "abnormal.htm"; }
            else if (command.contains("social")) { filename = "social.htm"; }

            AdminHelpPage.showHelpPage(activeChar, filename);
        }

        return true;
    }

    private static boolean performAbnormal(int action, L2Object target) {
        if (target instanceof L2Character) {
            final L2Character character = (L2Character) target;
            if ((character.getAbnormalEffect() & action) == action) { character.stopAbnormalEffect(action); }
            else { character.startAbnormalEffect(action); }

            return true;
        }
        return false;
    }

    private static boolean performSocial(int action, L2Object target) {
        if (target instanceof L2Character) {
            if (target instanceof L2Summon || target instanceof L2ChestInstance || (target instanceof L2Npc && (action < 1 || action > 3)) || (target instanceof L2PcInstance && (action < 2 || action > 16))) { return false; }

            final L2Character character = (L2Character) target;
            character.broadcastPacket(new SocialAction(character, action));
            return true;
        }
        return false;
    }

    @Override
    public String[] getAdminCommandList() {
        return ADMIN_COMMANDS;
    }
}