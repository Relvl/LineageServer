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

import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;

/**
 * This class is here to avoid hardcoded IDs.<br>
 * It refers to mobs that can be attacked but can also be fed.<br>
 * This class is only used by handlers in order to check the correctness of the target.<br>
 * However, no additional tasks are needed, since they are all handled by scripted AI.
 */
public class L2FeedableBeastInstance extends L2MonsterInstance
{
	public L2FeedableBeastInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
}