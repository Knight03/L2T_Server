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

package l2server.gameserver.taskmanager;

import l2server.Config;
import l2server.gameserver.taskmanager.TaskManager.ExecutedTask;
import l2server.log.Log;

import java.util.concurrent.ScheduledFuture;

/**
 * @author Layane
 */
public abstract class Task
{

	public void initialize()
	{
		if (Config.DEBUG)
		{
			Log.info("Task" + getName() + " inializate");
		}
	}

	public ScheduledFuture<?> launchSpecial(ExecutedTask instance)
	{
		return null;
	}

	public abstract String getName();

	public abstract void onTimeElapsed(ExecutedTask task);

	public void onDestroy()
	{
	}
}
