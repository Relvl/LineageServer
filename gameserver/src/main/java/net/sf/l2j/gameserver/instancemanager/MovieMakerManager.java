package net.sf.l2j.gameserver.instancemanager;

import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SpecialCamera;

/**
 * @author KKnD
 */
public class MovieMakerManager
{
	protected Map<Integer, Sequence> _sequence = new HashMap<>();
	
	public static final MovieMakerManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected class Sequence
	{
		protected int _sequenceId;
		protected int _objid;
		protected int _dist;
		protected int _yaw;
		protected int _pitch;
		protected int _time;
		protected int _duration;
		protected int _turn;
		protected int _rise;
		protected int _widescreen;
	}
	
	public void mainHtm(L2PcInstance player)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		
		if (!_sequence.isEmpty())
		{
			final StringBuilder sb = new StringBuilder();
			for (Sequence s : _sequence.values())
				StringUtil.append(sb, "<tr><td>", s._sequenceId, ": (", s._dist, ", ", s._yaw, ", ", s._pitch, ", ", s._time, ", ", s._duration, ", ", s._turn, ", ", s._rise, ", ", s._widescreen, ")</td></tr>");
			
			html.setFile("data/html/admin/movie/main_notempty.htm");
			html.replace("%sequences%", sb.toString());
		}
		else
			html.setFile("data/html/admin/movie/main_empty.htm");
		
		player.sendPacket(html);
	}
	
	public void playSequence(int id, L2PcInstance player)
	{
		if (_sequence.containsKey(id))
		{
			final Sequence s = _sequence.get(id);
			player.sendPacket(new SpecialCamera(s._objid, s._dist, s._yaw, s._pitch, s._time, s._duration, s._turn, s._rise, s._widescreen, 0));
		}
		else
		{
			player.sendMessage("Wrong sequence id.");
			mainHtm(player);
		}
	}
	
	public void broadcastSequence(int id, L2PcInstance player)
	{
		if (_sequence.containsKey(id))
		{
			final Sequence s = _sequence.get(id);
			player.broadcastPacket(new SpecialCamera(s._objid, s._dist, s._yaw, s._pitch, s._time, s._duration, s._turn, s._rise, s._widescreen, 0));
		}
		else
		{
			player.sendMessage("Wrong sequence id.");
			mainHtm(player);
		}
	}
	
	public void playSequence(L2PcInstance player, int objid, int dist, int yaw, int pitch, int time, int duration, int turn, int rise, int screen)
	{
		player.sendPacket(new SpecialCamera(objid, dist, yaw, pitch, time, duration, turn, rise, screen, 0));
	}
	
	public void addSequence(L2PcInstance player, int seqId, int objid, int dist, int yaw, int pitch, int time, int duration, int turn, int rise, int screen)
	{
		if (!_sequence.containsKey(seqId))
		{
			final Sequence s = new Sequence();
			s._sequenceId = seqId;
			s._objid = objid;
			s._dist = dist;
			s._yaw = yaw;
			s._pitch = pitch;
			s._time = time;
			s._duration = duration;
			s._turn = turn;
			s._rise = rise;
			s._widescreen = screen;
			_sequence.put(seqId, s);
			mainHtm(player);
		}
		else
		{
			player.sendMessage("This sequence already exists.");
			mainHtm(player);
		}
	}
	
	public void addSequence(L2PcInstance player)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile("data/html/admin/movie/add_sequence.htm");
		player.sendPacket(html);
	}
	
	public void editSequence(int id, L2PcInstance player)
	{
		if (_sequence.containsKey(id))
		{
			final Sequence s = _sequence.get(id);
			
			final NpcHtmlMessage html = new NpcHtmlMessage(0);
			html.setFile("data/html/admin/movie/edit_sequence.htm");
			html.replace("%sId%", s._sequenceId);
			html.replace("%sDist%", s._dist);
			html.replace("%sYaw%", s._yaw);
			html.replace("%sPitch%", s._pitch);
			html.replace("%sTime%", s._time);
			html.replace("%sDuration%", s._duration);
			html.replace("%sTurn%", s._turn);
			html.replace("%sRise%", s._rise);
			html.replace("%sWidescreen%", s._widescreen);
			player.sendPacket(html);
		}
		else
		{
			player.sendMessage("The sequence couldn't be updated.");
			mainHtm(player);
		}
	}
	
	public void updateSequence(L2PcInstance player, int seqId, int objid, int dist, int yaw, int pitch, int time, int duration, int turn, int rise, int screen)
	{
		if (_sequence.containsKey(seqId))
		{
			final Sequence s = new Sequence();
			s._sequenceId = seqId;
			s._objid = objid;
			s._dist = dist;
			s._yaw = yaw;
			s._pitch = pitch;
			s._time = time;
			s._duration = duration;
			s._turn = turn;
			s._rise = rise;
			s._widescreen = screen;
			
			_sequence.put(seqId, s);
		}
		else
			player.sendMessage("This sequence doesn't exist.");
		
		mainHtm(player);
	}
	
	public void deleteSequence(int id, L2PcInstance player)
	{
		if (_sequence.containsKey(id))
			_sequence.remove(id);
		else
			player.sendMessage("This sequence id doesn't exist.");
		
		mainHtm(player);
	}
	
	public void playMovie(int broadcast, L2PcInstance player)
	{
		if (!_sequence.isEmpty())
			ThreadPoolManager.getInstance().scheduleGeneral(new Play(1, broadcast, player), 500);
		else
		{
			player.sendMessage("There is nothing to play.");
			mainHtm(player);
		}
	}
	
	private class Play implements Runnable
	{
		private final int _id;
		private final int _broad;
		private final L2PcInstance _player;
		
		public Play(int id, int broadcast, L2PcInstance player)
		{
			_id = id;
			_broad = broadcast;
			_player = player;
		}
		
		@Override
		public void run()
		{
			if (_sequence.containsKey(_id))
			{
				final Sequence sec = _sequence.get(_id);
				
				if (_broad == 1)
					_player.broadcastPacket(new SpecialCamera(sec._objid, sec._dist, sec._yaw, sec._pitch, sec._time, sec._duration, sec._turn, sec._rise, sec._widescreen, 0));
				else
					_player.sendPacket(new SpecialCamera(sec._objid, sec._dist, sec._yaw, sec._pitch, sec._time, sec._duration, sec._turn, sec._rise, sec._widescreen, 0));
				
				ThreadPoolManager.getInstance().scheduleGeneral(new Play(_id + 1, _broad, _player), (sec._duration - 100));
			}
			else
			{
				_player.sendMessage("Movie ended on sequence: " + (_id - 1) + ".");
				mainHtm(_player);
			}
		}
	}
	
	private static class SingletonHolder
	{
		protected static final MovieMakerManager _instance = new MovieMakerManager();
	}
}