package net.sf.l2j.gameserver.communitybbs.Manager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import net.sf.l2j.L2DatabaseFactoryOld;
import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.datatables.CharNameTable;
import net.sf.l2j.gameserver.model.BlockList;
import net.sf.l2j.gameserver.model.world.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.client.game_to_client.ExMailArrived;
import net.sf.l2j.gameserver.network.client.game_to_client.PlaySound;
import net.sf.l2j.gameserver.network.client.game_to_client.SystemMessage;

public class MailBBSManager extends BaseBBSManager
{
	private enum MailType
	{
		INBOX("Inbox", "<a action=\"bypass _bbsmail\">Inbox</a>"),
		SENTBOX("Sent Box", "<a action=\"bypass _bbsmail;sentbox\">Sent Box</a>"),
		ARCHIVE("Mail Archive", "<a action=\"bypass _bbsmail;archive\">Mail Archive</a>"),
		TEMPARCHIVE("Temporary Mail Archive", "<a action=\"bypass _bbsmail;temp_archive\">Temporary Mail Archive</a>");

		private final String _description;
		private final String _bypass;

		MailType(String description, String bypass)
		{
			_description = description;
			_bypass = bypass;
		}

		public String getDescription()
		{
			return _description;
		}

		public String getBypass()
		{
			return _bypass;
		}

		public static final MailType VALUES[] = values();
	}

	private final Map<Integer, List<Mail>> _mails = new HashMap<>();

	private int _lastid;

	private static final String SELECT_CHAR_MAILS = "SELECT * FROM character_mail WHERE charId = ? ORDER BY letterId ASC";
	private static final String INSERT_NEW_MAIL = "INSERT INTO character_mail (charId, letterId, senderId, location, recipientNames, subject, message, sentDate, unread) VALUES (?,?,?,?,?,?,?,?,?)";
	private static final String DELETE_MAIL = "DELETE FROM character_mail WHERE letterId = ?";
	private static final String MARK_MAIL_READ = "UPDATE character_mail SET unread = ? WHERE letterId = ?";
	private static final String SET_LETTER_LOC = "UPDATE character_mail SET location = ? WHERE letterId = ?";
	private static final String SELECT_LAST_ID = "SELECT letterId FROM character_mail ORDER BY letterId DESC LIMIT 1";

	public class Mail
	{
		int charId;
		int letterId;
		int senderId;
		MailType location;
		String recipientNames;
		String subject;
		String message;
		Timestamp sentDate;
		String sentDateString;
		boolean unread;
	}

	public static MailBBSManager getInstance()
	{
		return SingletonHolder._instance;
	}

	protected MailBBSManager()
	{
		initId();
	}

	@Override
	public void parseCmd(String command, L2PcInstance activeChar)
	{
		if (command.equals("_bbsmail") || command.equals("_maillist_0_1_0_"))
			showMailList(activeChar, 1, MailType.INBOX);
		else if (command.startsWith("_bbsmail"))
		{
			StringTokenizer st = new StringTokenizer(command, ";");
			st.nextToken();
			String action = st.nextToken();

			if (action.equals("inbox") || action.equals("sentbox") || action.equals("archive") || action.equals("temparchive"))
			{
				int page = (st.hasMoreTokens()) ? Integer.parseInt(st.nextToken()) : 1;
				String sType = (st.hasMoreTokens()) ? st.nextToken() : "";
				String search = (st.hasMoreTokens()) ? st.nextToken() : "";

				showMailList(activeChar, page, Enum.valueOf(MailType.class, action.toUpperCase()), sType, search);
			}
			else if (action.equals("crea"))
				showWriteView(activeChar);
			else if (action.equals("view"))
			{
				int letterId = (st.hasMoreTokens()) ? Integer.parseInt(st.nextToken()) : -1;

				Mail letter = getLetter(activeChar, letterId);
				if (letter == null)
					showLastForum(activeChar);
				else
				{
					showLetterView(activeChar, letter);
					if (letter.unread)
						setLetterToRead(activeChar, letter.letterId);
				}
			}
			else if (action.equals("reply"))
			{
				int letterId = (st.hasMoreTokens()) ? Integer.parseInt(st.nextToken()) : -1;

				Mail letter = getLetter(activeChar, letterId);
				if (letter == null)
					showLastForum(activeChar);
				else
					showWriteView(activeChar, getCharName(letter.senderId), letter);
			}
			else if (action.equals("del"))
			{
				int letterId = (st.hasMoreTokens()) ? Integer.parseInt(st.nextToken()) : -1;

				Mail letter = getLetter(activeChar, letterId);
				if (letter != null)
					deleteLetter(activeChar, letter.letterId);

				showLastForum(activeChar);
			}
			else if (action.equals("store"))
			{
				int letterId = (st.hasMoreTokens()) ? Integer.parseInt(st.nextToken()) : -1;

				Mail letter = getLetter(activeChar, letterId);
				if (letter != null)
					setLetterLocation(activeChar, letter.letterId, MailType.ARCHIVE);

				showMailList(activeChar, 1, MailType.ARCHIVE);
			}
		}
		else
			super.parseCmd(command, activeChar);
	}

	@Override
	public void parseWrite(String ar1, String ar2, String ar3, String ar4, String ar5, L2PcInstance activeChar)
	{
		if (ar1.equals("Send"))
		{
			sendLetter(ar3, ar4, ar5, activeChar);
			showMailList(activeChar, 1, MailType.SENTBOX);
		}
		else if (ar1.startsWith("Search"))
		{
			StringTokenizer st = new StringTokenizer(ar1, ";");
			st.nextToken();

			showMailList(activeChar, 1, Enum.valueOf(MailType.class, st.nextToken().toUpperCase()), ar4, ar5);
		}
		else
			super.parseWrite(ar1, ar2, ar3, ar4, ar5, activeChar);
	}

	private void initId()
	{
		try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement(SELECT_LAST_ID);
			ResultSet result = statement.executeQuery();
			while (result.next())
			{
				if (result.getInt(1) > _lastid)
					_lastid = result.getInt(1);
			}
			result.close();
			statement.close();
		}
		catch (Exception e)
		{
			LOGGER.error(getClass().getSimpleName() + ": data error on MailBBS (initId): " + e);
			e.printStackTrace();
		}
	}

	private synchronized int getNewMailId()
	{
		return ++_lastid;
	}

	private List<Mail> getPlayerMails(int objId)
	{
		List<Mail> _letters = _mails.get(objId);
		if (_letters == null)
		{
			_letters = new ArrayList<>();
			try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection())
			{
				PreparedStatement statement = con.prepareStatement(SELECT_CHAR_MAILS);
				statement.setInt(1, objId);
				ResultSet result = statement.executeQuery();
				while (result.next())
				{
					Mail letter = new Mail();
					letter.charId = result.getInt("charId");
					letter.letterId = result.getInt("letterId");
					letter.senderId = result.getInt("senderId");
					letter.location = Enum.valueOf(MailType.class, result.getString("location").toUpperCase());
					letter.recipientNames = result.getString("recipientNames");
					letter.subject = result.getString("subject");
					letter.message = result.getString("message");
					letter.sentDate = result.getTimestamp("sentDate");
					letter.sentDateString = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(letter.sentDate);
					letter.unread = result.getInt("unread") != 0;
					_letters.add(0, letter);
				}
				result.close();
				statement.close();
			}
			catch (Exception e)
			{
				LOGGER.error("couldnt load mail for ID:{} {}", objId, e.getMessage());
			}
			_mails.put(objId, _letters);
		}
		return _letters;
	}

	private Mail getLetter(L2PcInstance activeChar, int letterId)
	{
		for (Mail letter : getPlayerMails(activeChar.getObjectId()))
		{
			if (letter.letterId == letterId)
				return letter;
		}
		return null;
	}

	private static String abbreviate(String s, int maxWidth)
	{
		return s.length() > maxWidth ? s.substring(0, maxWidth) : s;
	}

	public int checkUnreadMail(L2PcInstance activeChar)
	{
		int count = 0;
		for (Mail letter : getPlayerMails(activeChar.getObjectId()))
		{
			if (letter.unread)
				count++;
		}
		return count;
	}

	private void showMailList(L2PcInstance activeChar, int page, MailType type)
	{
		showMailList(activeChar, page, type, "", "");
	}

	private void showMailList(L2PcInstance activeChar, int page, MailType type, String sType, String search)
	{
		List<Mail> letters;
		if (!sType.equals("") && !search.equals(""))
		{
			letters = new ArrayList<>();

			boolean byTitle = sType.equalsIgnoreCase("title");

			for (Mail letter : getPlayerMails(activeChar.getObjectId()))
			{
				if (byTitle && letter.subject.toLowerCase().contains(search.toLowerCase()))
					letters.add(letter);
				else if (!byTitle)
				{
					String writer = getCharName(letter.senderId);
					if (writer.toLowerCase().contains(search.toLowerCase()))
						letters.add(letter);
				}
			}
		}
		else
			letters = getPlayerMails(activeChar.getObjectId());

		int countMails = getCountLetters(activeChar.getObjectId(), type, sType, search);
		int maxpage = getMaxPageId(countMails);

		if (page > maxpage)
			page = maxpage;
		if (page < 1)
			page = 1;

		activeChar.setMailPosition(page);
		int index = 0, minIndex = 0, maxIndex = 0;
		maxIndex = page == 1 ? page * 9 : (page * 10) - 1;
		minIndex = maxIndex - 9;

		String content = HtmCache.getInstance().getHtm(CB_PATH + "mail/mail.htm");
		content = content.replace("%inbox%", Integer.toString(getCountLetters(activeChar.getObjectId(), MailType.INBOX, "", "")));
		content = content.replace("%sentbox%", Integer.toString(getCountLetters(activeChar.getObjectId(), MailType.SENTBOX, "", "")));
		content = content.replace("%archive%", Integer.toString(getCountLetters(activeChar.getObjectId(), MailType.ARCHIVE, "", "")));
		content = content.replace("%temparchive%", Integer.toString(getCountLetters(activeChar.getObjectId(), MailType.TEMPARCHIVE, "", "")));
		content = content.replace("%type%", type.getDescription());
		content = content.replace("%htype%", type.toString().toLowerCase());

		StringBuilder sb = new StringBuilder();
		for (Mail letter : letters)
		{
			if (letter.location.equals(type))
			{
				if (index < minIndex)
				{
					index++;
					continue;
				}

				if (index > maxIndex)
					break;

				StringUtil.append(sb, "<table width=610><tr><td width=5></td><td width=150>", getCharName(letter.senderId), "</td><td width=300><a action=\"bypass _bbsmail;view;", letter.letterId, "\">");

				if (letter.unread)
					sb.append("<font color=\"LEVEL\">");

				sb.append(abbreviate(letter.subject, 51));

				if (letter.unread)
					sb.append("</font>");

				StringUtil.append(sb, "</a></td><td width=150>", letter.sentDateString, "</td><td width=5></td></tr></table><img src=\"L2UI.Squaregray\" width=610 height=1>");
				index++;
			}
		}
		content = content.replace("%maillist%", sb.toString());

		// CLeanup sb.
		sb.setLength(0);

		String fullSearch = (!sType.equals("") && !search.equals("")) ? ";" + sType + ";" + search : "";

		StringUtil.append(sb, "<td><table><tr><td></td></tr><tr><td><button action=\"bypass _bbsmail;", type, ";", page == 1 ? page : page - 1, fullSearch, "\" back=\"l2ui_ch3.prev1_down\" fore=\"l2ui_ch3.prev1\" width=16 height=16></td></tr></table></td>");

		int i = 0;
		if (maxpage > 21)
		{
			if (page <= 11)
			{
				for (i = 1; i <= (10 + page); i++)
				{
					if (i == page)
						StringUtil.append(sb, "<td> ", i, " </td>");
					else
						StringUtil.append(sb, "<td><a action=\"bypass _bbsmail;", type, ";", i, fullSearch, "\"> ", i, " </a></td>");
				}
			}
			else if (page > 11 && (maxpage - page) > 10)
			{
				for (i = page - 10; i <= (page - 1); i++)
				{
					if (i == page)
						continue;

					StringUtil.append(sb, "<td><a action=\"bypass _bbsmail;", type, ";", i, fullSearch, "\"> ", i, " </a></td>");
				}
				for (i = page; i <= (page + 10); i++)
				{
					if (i == page)
						StringUtil.append(sb, "<td> ", i, " </td>");
					else
						StringUtil.append(sb, "<td><a action=\"bypass _bbsmail;", type, ";", i, fullSearch, "\"> ", i, " </a></td>");
				}
			}
			else if ((maxpage - page) <= 10)
			{
				for (i = page - 10; i <= maxpage; i++)
				{
					if (i == page)
						StringUtil.append(sb, "<td> ", i, " </td>");
					else
						StringUtil.append(sb, "<td><a action=\"bypass _bbsmail;", type, ";", i, fullSearch, "\"> ", i, " </a></td>");
				}
			}
		}
		else
		{
			for (i = 1; i <= maxpage; i++)
			{
				if (i == page)
					StringUtil.append(sb, "<td> ", i, " </td>");
				else
					StringUtil.append(sb, "<td><a action=\"bypass _bbsmail;", type, ";", i, fullSearch, "\"> ", i, " </a></td>");
			}
		}
		StringUtil.append(sb, "<td><table><tr><td></td></tr><tr><td><button action=\"bypass _bbsmail;", type, ";", page == maxpage ? page : page + 1, fullSearch, "\" back=\"l2ui_ch3.next1_down\" fore=\"l2ui_ch3.next1\" width=16 height=16 ></td></tr></table></td>");

		content = content.replace("%maillistlength%", sb.toString());

		separateAndSend(content, activeChar);
	}

	private void showLetterView(L2PcInstance activeChar, Mail letter)
	{
		if (letter == null)
		{
			showMailList(activeChar, 1, MailType.INBOX);
			return;
		}

		String content = HtmCache.getInstance().getHtm(CB_PATH + "mail/mail-show.htm");

		String link = letter.location.getBypass() + "&nbsp;&gt;&nbsp;" + letter.subject;
		content = content.replace("%maillink%", link);

		content = content.replace("%writer%", getCharName(letter.senderId));
		content = content.replace("%sentDate%", letter.sentDateString);
		content = content.replace("%receiver%", letter.recipientNames);
		content = content.replace("%delDate%", "Unknown");
		content = content.replace("%title%", letter.subject.replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\"", "&quot;"));
		content = content.replace("%mes%", letter.message.replaceAll("\r\n", "<br>").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\"", "&quot;"));
		content = content.replace("%letterId%", letter.letterId + "");
		separateAndSend(content, activeChar);
	}

	private static void showWriteView(L2PcInstance activeChar)
	{
		String content = HtmCache.getInstance().getHtm(CB_PATH + "mail/mail-write.htm");
		separateAndSend(content, activeChar);
	}

	private static void showWriteView(L2PcInstance activeChar, String parcipientName, Mail letter)
	{
		String content = HtmCache.getInstance().getHtm(CB_PATH + "mail/mail-reply.htm");

		String link = letter.location.getBypass() + "&nbsp;&gt;&nbsp;<a action=\"bypass _bbsmail;view;" + letter.letterId + "\">" + letter.subject + "</a>&nbsp;&gt;&nbsp;";
		content = content.replace("%maillink%", link);

		content = content.replace("%recipients%", letter.senderId == activeChar.getObjectId() ? letter.recipientNames : getCharName(letter.senderId));
		content = content.replace("%letterId%", letter.letterId + "");
		send1001(content, activeChar);
		send1002(activeChar, " ", "Re: " + letter.subject, "0");
	}

	public void sendLetter(String recipients, String subject, String message, L2PcInstance activeChar)
	{
		int countTodaysLetters = 0;
		Timestamp ts = new Timestamp(Calendar.getInstance().getTimeInMillis() - 86400000L);
		long date = Calendar.getInstance().getTimeInMillis();

		for (Mail letter : getPlayerMails(activeChar.getObjectId()))
			if (letter.sentDate.after(ts) && letter.location == MailType.SENTBOX)
				countTodaysLetters++;

		if (countTodaysLetters >= 10 && !activeChar.isGM())
		{
			activeChar.sendPacket(SystemMessageId.NO_MORE_MESSAGES_TODAY);
			return;
		}

		if (subject == null || subject.isEmpty())
			subject = "(no subject)";

		try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection())
		{
			Set<String> recipts = new HashSet<>(5);
			String[] recipAr = recipients.split(";");
			for (String r : recipAr)
				recipts.add(r.trim());

			message = message.replaceAll("\n", "<br1>");

			boolean sent = false;
			int countRecips = 0;

			Timestamp time = new Timestamp(date);
			PreparedStatement statement = null;

			for (String recipient : recipts)
			{
				int recipId = CharNameTable.getInstance().getIdByName(recipient);
				if (recipId <= 0 || recipId == activeChar.getObjectId())
					activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
				else if (!activeChar.isGM())
				{
					if (isGM(recipId))
						activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CANNOT_MAIL_GM_S1).addString(recipient));
					else if (isBlocked(activeChar, recipId))
						activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_BLOCKED_YOU_CANNOT_MAIL).addString(recipient));
					else if (isRecipInboxFull(recipId))
					{
						activeChar.sendPacket(SystemMessageId.MESSAGE_NOT_SENT);

						L2PcInstance PCrecipient = L2World.getInstance().getPlayer(recipient);
						if (PCrecipient != null)
							PCrecipient.sendPacket(SystemMessageId.MAILBOX_FULL);
					}
				}
				else if (countRecips < 5 && !activeChar.isGM() || activeChar.isGM())
				{
					int id = getNewMailId();
					if (statement == null)
					{
						statement = con.prepareStatement(INSERT_NEW_MAIL);
						statement.setInt(3, activeChar.getObjectId());
						statement.setString(4, "inbox");
						statement.setString(5, recipients);
						statement.setString(6, abbreviate(subject, 128));
						statement.setString(7, message);
						statement.setTimestamp(8, time);
						statement.setInt(9, 1);
					}
					statement.setInt(1, recipId);
					statement.setInt(2, id);
					statement.execute();
					sent = true;

					Mail letter = new Mail();
					letter.charId = recipId;
					letter.letterId = id;
					letter.senderId = activeChar.getObjectId();
					letter.location = MailType.INBOX;
					letter.recipientNames = recipients;
					letter.subject = abbreviate(subject, 128);
					letter.message = message;
					letter.sentDate = time;
					letter.sentDateString = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(letter.sentDate);
					letter.unread = true;
					getPlayerMails(recipId).add(0, letter);

					countRecips++;

					L2PcInstance PCrecipient = L2World.getInstance().getPlayer(recipient);
					if (PCrecipient != null)
					{
						PCrecipient.sendPacket(SystemMessageId.NEW_MAIL);
						PCrecipient.sendPacket(new PlaySound("systemmsg_e.1233"));
						PCrecipient.sendPacket(ExMailArrived.STATIC_PACKET);
					}
				}
			}

			// Create a copy into activeChar's sent box
			if (statement != null)
			{
				int id = getNewMailId();

				statement.setInt(1, activeChar.getObjectId());
				statement.setInt(2, id);
				statement.setString(4, "sentbox");
				statement.setInt(9, 0);
				statement.execute();
				statement.close();

				Mail letter = new Mail();
				letter.charId = activeChar.getObjectId();
				letter.letterId = id;
				letter.senderId = activeChar.getObjectId();
				letter.location = MailType.SENTBOX;
				letter.recipientNames = recipients;
				letter.subject = abbreviate(subject, 128);
				letter.message = message;
				letter.sentDate = time;
				letter.sentDateString = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(letter.sentDate);
				letter.unread = false;
				getPlayerMails(activeChar.getObjectId()).add(0, letter);
			}

			if (countRecips > 5 && !activeChar.isGM())
				activeChar.sendPacket(SystemMessageId.ONLY_FIVE_RECIPIENTS);

			if (sent)
				activeChar.sendPacket(SystemMessageId.SENT_MAIL);
		}
		catch (Exception e)
		{
			LOGGER.error("couldnt send letter for {} {}", activeChar.getName(), e.getMessage());
		}
	}

	private int getCountLetters(int objId, MailType location, String sType, String search)
	{
		int count = 0;
		if (!sType.equals("") && !search.equals(""))
		{
			boolean byTitle = sType.equalsIgnoreCase("title");
			for (Mail letter : getPlayerMails(objId))
			{
				if (!letter.location.equals(location))
					continue;

				if (byTitle && letter.subject.toLowerCase().contains(search.toLowerCase()))
					count++;
				else if (!byTitle)
				{
					String writer = getCharName(letter.senderId);
					if (writer.toLowerCase().contains(search.toLowerCase()))
						count++;
				}
			}
		}
		else
		{
			for (Mail letter : getPlayerMails(objId))
			{
				if (letter.location.equals(location))
					count++;
			}
		}
		return count;
	}

	private static boolean isBlocked(L2PcInstance activeChar, int recipId)
	{
		for (L2PcInstance player : L2World.getInstance().getPlayers())
		{
			if (player.getObjectId() == recipId)
			{
                return BlockList.isInBlockList(player, activeChar);

            }
		}
		return false;
	}

	private void deleteLetter(L2PcInstance activeChar, int letterId)
	{
		for (Mail letter : getPlayerMails(activeChar.getObjectId()))
		{
			if (letter.letterId == letterId)
			{
				getPlayerMails(activeChar.getObjectId()).remove(letter);
				break;
			}
		}

		try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement(DELETE_MAIL);
			statement.setInt(1, letterId);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			LOGGER.error("couldnt delete letter {}", letterId, e);
		}
	}

	private void setLetterToRead(L2PcInstance activeChar, int letterId)
	{
		getLetter(activeChar, letterId).unread = false;

		try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement(MARK_MAIL_READ);
			statement.setInt(1, 0);
			statement.setInt(2, letterId);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			LOGGER.error("couldnt set unread to false for {}", letterId, e);
		}
	}

	private void setLetterLocation(L2PcInstance activeChar, int letterId, MailType location)
	{
		getLetter(activeChar, letterId).location = location;

		try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement(SET_LETTER_LOC);
			statement.setString(1, location.toString().toLowerCase());
			statement.setInt(2, letterId);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			LOGGER.error("couldnt set location to false for {}", letterId, e);
		}
	}

	private static String getCharName(int charId)
	{
		String name = CharNameTable.getInstance().getNameById(charId);
		return name == null ? "Unknown" : name;
	}

	private static boolean isGM(int charId)
	{
		boolean isGM = false;
		try (Connection con = L2DatabaseFactoryOld.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT accesslevel FROM characters WHERE obj_Id = ?");
			statement.setInt(1, charId);
			ResultSet result = statement.executeQuery();
			result.next();
			isGM = result.getInt(1) > 0;
			result.close();
			statement.close();
		}
		catch (Exception e)
		{
			LOGGER.error("", e);
		}
		return isGM;
	}

	private boolean isRecipInboxFull(int charId)
	{
		return getCountLetters(charId, MailType.INBOX, "", "") >= 100;
	}

	private void showLastForum(L2PcInstance activeChar)
	{
		int page = activeChar.getMailPosition() % 1000;
		int type = activeChar.getMailPosition() / 1000;

		showMailList(activeChar, page, MailType.VALUES[type]);
	}

	private static int getMaxPageId(int letterCount)
	{
		if (letterCount < 1)
			return 1;

		if (letterCount % 10 == 0)
			return letterCount / 10;

		return (letterCount / 10) + 1;
	}

	private static class SingletonHolder
	{
		protected static final MailBBSManager _instance = new MailBBSManager();
	}
}