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

package l2server.gameserver.instancemanager;

import l2server.Config;
import l2server.gameserver.model.quest.Quest;
import l2server.gameserver.scripting.L2ScriptEngineManager;
import l2server.gameserver.scripting.ScriptManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class QuestManager extends ScriptManager<Quest> {
	
	private static Logger log = LoggerFactory.getLogger(QuestManager.class.getName());
	
	public static QuestManager getInstance() {
		return SingletonHolder.instance;
	}
	
	// =========================================================
	
	// =========================================================
	// Data Field
	private Map<String, Quest> quests = new HashMap<>();
	
	// =========================================================
	// Constructor
	private QuestManager() {
	}
	
	// =========================================================
	// Method - Public
	public final boolean reload(String questFolder) {
		Quest q = getQuest(questFolder);
		if (q == null) {
			return false;
		}
		return q.reload();
	}
	
	/**
	 * Reloads a the quest given by questId.<BR>
	 * <B>NOTICE: Will only work if the quest name is equal the quest folder name</B>
	 *
	 * @param questId The id of the quest to be reloaded
	 * @return true if reload was successful, false otherwise
	 */
	public final boolean reload(int questId) {
		Quest q = this.getQuest(questId);
		if (q == null) {
			return false;
		}
		return q.reload();
	}
	
	public final void reload() {
		log.info("Reloading Server Scripts");
		try {
			// unload all scripts
			for (Quest quest : quests.values()) {
				if (quest != null) {
					quest.unload(false);
				}
			}
			
			quests.clear();
			// now load all scripts
			File scripts = new File(Config.DATAPACK_ROOT + "/" + Config.DATA_FOLDER + "scripts.cfg");
			L2ScriptEngineManager.INSTANCE.executeScriptList(scripts);
			
			scripts = new File(Config.DATAPACK_ROOT + "/data_" + Config.SERVER_NAME + "/scripts.cfg");
			if (scripts.exists()) {
				L2ScriptEngineManager.INSTANCE.executeScriptList(scripts);
			}
			
			QuestManager.getInstance().report();
		} catch (IOException ioe) {
			log.error("Failed loading scripts.cfg, no script going to be loaded");
		}
	}
	
	public final void report() {
		log.info("Loaded: " + quests.size() + " quests");
	}
	
	public final void save() {
		for (Quest q : quests.values()) {
			q.saveGlobalData();
		}
	}
	
	// =========================================================
	// Property - Public
	public final Quest getQuest(String name) {
		return quests.get(name);
	}
	
	public final Quest getQuest(int questId) {
		for (Quest q : quests.values()) {
			if (q.getQuestIntId() == questId) {
				return q;
			}
		}
		return null;
	}
	
	public final void addQuest(Quest newQuest) {
		if (newQuest == null) {
			throw new IllegalArgumentException("Quest argument cannot be null");
		}
		Quest old = quests.get(newQuest.getName());
		
		// FIXME: unloading the old quest at this point is a tad too late.
		// the new quest has already initialized itself and read the data, starting
		// an unpredictable number of tasks with that data.  The old quest will now
		// save data which will never be read.
		// However, requesting the newQuest to re-read the data is not necessarily a
		// good option, since the newQuest may have already started timers, spawned NPCs
		// or taken any other action which it might re-take by re-reading the data.
		// the current solution properly closes the running tasks of the old quest but
		// ignores the data; perhaps the least of all evils...
		if (old != null) {
			old.unload();
			log.info("Replaced: (" + old.getName() + ") with a new version (" + newQuest.getName() + ")");
		}
		quests.put(newQuest.getName(), newQuest);
	}
	
	public final boolean removeQuest(Quest q) {
		return quests.remove(q.getName()) != null;
	}
	
	/**
	 * @see ScriptManager#getAllManagedScripts()
	 */
	@Override
	public Iterable<Quest> getAllManagedScripts() {
		return quests.values();
	}
	
	/**
	 * @see ScriptManager#unload(l2server.gameserver.scripting.ManagedScript)
	 */
	@Override
	public boolean unload(Quest ms) {
		ms.saveGlobalData();
		return removeQuest(ms);
	}
	
	/**
	 * @see ScriptManager#getScriptManagerName()
	 */
	@Override
	public String getScriptManagerName() {
		return "QuestManager";
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {
		protected static final QuestManager instance = new QuestManager();
	}
}
