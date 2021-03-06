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
import l2server.DatabasePool;
import l2server.gameserver.datatables.SkillTable;
import l2server.gameserver.model.L2Clan;
import l2server.gameserver.model.Location;
import l2server.gameserver.model.Skill;
import l2server.gameserver.model.WorldObject;
import l2server.gameserver.model.actor.Creature;
import l2server.gameserver.model.actor.instance.Player;
import l2server.gameserver.model.entity.Castle;
import l2server.gameserver.model.entity.Siege;
import l2server.util.loader.annotations.Load;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class CastleSiegeManager {
	private static Logger log = LoggerFactory.getLogger(CastleSiegeManager.class.getName());



	public static CastleSiegeManager getInstance() {
		return SingletonHolder.instance;
	}

	// =========================================================
	// Data Field
	private int attackerMaxClans = 500; // Max number of clans
	private int attackerRespawnDelay = 0; // Time in ms. Changeable in siege.config
	private int defenderMaxClans = 500; // Max number of clans

	private int flagMaxCount = 1; // Changeable in siege.config
	private int siegeClanMinLevel = 5; // Changeable in siege.config
	private int siegeLength = 120; // Time in minute. Changeable in siege.config
	private int bloodAllianceReward = 0; // Number of Blood Alliance items reward for successful castle defending

	// =========================================================
	// Constructor
	private CastleSiegeManager() {
	}

	// =========================================================
	// Method - Public
	public final void addSiegeSkills(Player character) {
		for (Skill sk : SkillTable.getInstance().getSiegeSkills(character.isNoble(), character.getClan().getHasCastle() > 0)) {
			character.addSkill(sk, false);
		}
	}

	/**
	 * Return true if character summon<BR><BR>
	 *
	 * @param activeChar The Creature of the character can summon
	 */
	public final boolean checkIfOkToSummon(Creature activeChar, boolean isCheckOnly) {
		if (!(activeChar instanceof Player)) {
			return false;
		}

		String text = "";
		Player player = (Player) activeChar;
		Castle castle = CastleManager.getInstance().getCastle(player);

		if (castle == null || castle.getCastleId() <= 0) {
			text = "You must be on castle ground to summon this";
		} else if (!castle.getSiege().getIsInProgress()) {
			text = "You can only summon this during a siege.";
		} else if (player.getClanId() != 0 && castle.getSiege().getAttackerClan(player.getClanId()) == null) {
			text = "You can only summon this as a registered attacker.";
		} else {
			return true;
		}

		if (!isCheckOnly) {
			player.sendMessage(text);
		}
		return false;
	}

	/**
	 * Return true if the clan is registered or owner of a castle<BR><BR>
	 *
	 * @param clan The L2Clan of the player
	 */
	public final boolean checkIsRegistered(L2Clan clan, int castleid) {
		if (clan == null) {
			return false;
		}

		if (clan.getHasCastle() > 0) {
			return true;
		}

		Connection con = null;
		boolean register = false;
		try {
			con = DatabasePool.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT clan_id FROM siege_clans WHERE clan_id=? AND castle_id=?");
			statement.setInt(1, clan.getClanId());
			statement.setInt(2, castleid);
			ResultSet rs = statement.executeQuery();

			if (rs.next()) {
				register = true;
			}

			rs.close();
			statement.close();
		} catch (Exception e) {
			log.warn("Exception: checkIsRegistered(): " + e.getMessage(), e);
		} finally {
			DatabasePool.close(con);
		}
		return register;
	}

	public final void removeSiegeSkills(Player character) {
		for (Skill sk : SkillTable.getInstance().getSiegeSkills(character.isNoble(), character.getClan().getHasCastle() > 0)) {
			character.removeSkill(sk);
		}
	}
	
	
	@Load(dependencies = CastleManager.class)
	public void load() {
		log.info("Initializing CastleSiegeManager");
		InputStream is = null;
		try {
			is = new FileInputStream(new File(Config.CONFIG_DIRECTORY + Config.CONFIG_FILE));
			Properties siegeSettings = new Properties();
			siegeSettings.load(is);

			// Siege setting
			attackerMaxClans = Integer.decode(siegeSettings.getProperty("AttackerMaxClans", "500"));
			attackerRespawnDelay = Integer.decode(siegeSettings.getProperty("AttackerRespawn", "0"));
			defenderMaxClans = Integer.decode(siegeSettings.getProperty("DefenderMaxClans", "500"));
			flagMaxCount = Integer.decode(siegeSettings.getProperty("MaxFlags", "1"));
			siegeClanMinLevel = Integer.decode(siegeSettings.getProperty("SiegeClanMinLevel", "5"));
			siegeLength = Integer.decode(siegeSettings.getProperty("SiegeLength", "120"));
			bloodAllianceReward = Integer.decode(siegeSettings.getProperty("BloodAllianceReward", "0"));

			for (Castle castle : CastleManager.getInstance().getCastles()) {
				MercTicketManager.MERCS_MAX_PER_CASTLE[castle.getCastleId() - 1] =
						Integer.parseInt(siegeSettings.getProperty(castle.getName() + "MaxMercenaries",
								Integer.toString(MercTicketManager.MERCS_MAX_PER_CASTLE[castle.getCastleId() - 1])).trim());
			}
		} catch (Exception e) {
			//initialized = false;
			log.warn("Error while loading siege data: " + e.getMessage(), e);
		} finally {
			try {
				is.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	// =========================================================
	// Property - Public
	public final int getAttackerMaxClans() {
		return attackerMaxClans;
	}

	public final int getAttackerRespawnDelay() {
		return attackerRespawnDelay;
	}

	public final int getDefenderMaxClans() {
		return defenderMaxClans;
	}

	public final int getFlagMaxCount() {
		return flagMaxCount;
	}

	public final Siege getSiege(WorldObject activeObject) {
		return getSiege(activeObject.getX(), activeObject.getY(), activeObject.getZ());
	}

	public final Siege getSiege(int x, int y, int z) {
		for (Castle castle : CastleManager.getInstance().getCastles()) {
			if (castle.getSiege().checkIfInZone(x, y, z)) {
				return castle.getSiege();
			}
		}
		return null;
	}

	public final int getSiegeClanMinLevel() {
		return siegeClanMinLevel;
	}

	public final int getSiegeLength() {
		return siegeLength;
	}

	public final int getBloodAllianceReward() {
		return bloodAllianceReward;
	}

	public final List<Siege> getSieges() {
		ArrayList<Siege> sieges = new ArrayList<>();
		for (Castle castle : CastleManager.getInstance().getCastles()) {
			sieges.add(castle.getSiege());
		}
		return sieges;
	}

	public static class SiegeSpawn {
		Location location;
		private int npcId;
		private int heading;
		private int castleId;
		private int hp;

		public SiegeSpawn(int castle_id, int x, int y, int z, int heading, int npc_id) {
			castleId = castle_id;
			location = new Location(x, y, z, heading);
			this.heading = heading;
			npcId = npc_id;
		}

		public SiegeSpawn(int castle_id, int x, int y, int z, int heading, int npc_id, int hp) {
			castleId = castle_id;
			location = new Location(x, y, z, heading);
			this.heading = heading;
			npcId = npc_id;
			this.hp = hp;
		}

		public int getCastleId() {
			return castleId;
		}

		public int getNpcId() {
			return npcId;
		}

		public int getHeading() {
			return heading;
		}

		public int getHp() {
			return hp;
		}

		public Location getLocation() {
			return location;
		}
	}

	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {
		protected static final CastleSiegeManager instance = new CastleSiegeManager();
	}
}
