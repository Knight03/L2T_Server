package l2server.gameserver.events;

import l2server.Config;
import l2server.gameserver.Announcements;
import l2server.gameserver.GeoData;
import l2server.gameserver.ThreadPoolManager;
import l2server.gameserver.datatables.NpcTable;
import l2server.gameserver.datatables.SpawnTable;
import l2server.gameserver.model.L2Spawn;
import l2server.gameserver.model.actor.Creature;
import l2server.gameserver.model.actor.Npc;
import l2server.gameserver.model.actor.instance.Player;
import l2server.gameserver.network.serverpackets.MagicSkillLaunched;
import l2server.gameserver.network.serverpackets.NpcHtmlMessage;
import l2server.gameserver.templates.chars.NpcTemplate;
import l2server.gameserver.util.Util;
import l2server.util.Rnd;
import l2server.util.loader.annotations.Load;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Pere
 */
public class HiddenChests {
	private static Logger log = LoggerFactory.getLogger(HiddenChests.class.getName());
	
	public static HiddenChests instance = null;
	
	private static final int SPECIAL_CHEST_COUNT = 5;
	private L2Spawn[] specialChestSpawns = new L2Spawn[SPECIAL_CHEST_COUNT];
	private HiddenChestsTask[] specialChestTasks = new HiddenChestsTask[SPECIAL_CHEST_COUNT];
	
	public static HiddenChests getInstance() {
		if (instance == null) {
			instance = new HiddenChests();
		}
		return instance;
	}
	
	@Load(dependencies = SpawnTable.class)
	public void spawnChests() {
		if (!Config.isServer(Config.TENKAI)) {
			return;
		}
		NpcTemplate tmpl = NpcTable.getInstance().getTemplate(50101);
		try {
			for (int i = 0; i < SPECIAL_CHEST_COUNT; i++) {
				L2Spawn chestSpawn = new L2Spawn(tmpl);
				int x = 0;
				int y = 0;
				int z = 0;
				boolean found = false;
				while (!found) {
					L2Spawn randomSpawn = SpawnTable.getInstance().getRandomDistributedSpawn();
					Npc randomNpc = randomSpawn.getNpc();
					while (randomSpawn.getNpc().getX() < 150000 || randomSpawn.getNpc().getY() > 227000 ||
							randomSpawn.getNpc().isInsideZone(Creature.ZONE_CASTLE) || randomSpawn.getNpc().isInsideZone(Creature.ZONE_CLANHALL) ||
							randomSpawn.getNpc().isInsideZone(Creature.ZONE_FORT)) {
						randomSpawn = SpawnTable.getInstance().getRandomDistributedSpawn();
					}
					
					x = randomNpc.getX() + Rnd.get(800) - 400;
					y = randomNpc.getY() + Rnd.get(800) - 400;
					z = GeoData.getInstance().getHeight(x, y, randomNpc.getZ());
					
					int sec = 0;
					while (!GeoData.getInstance().canSeeTarget(randomSpawn.getX(), randomSpawn.getY(), randomSpawn.getZ(), x, y, z) && sec < 20) {
						x = randomNpc.getX() + Rnd.get(800) - 400;
						y = randomNpc.getY() + Rnd.get(800) - 400;
						z = GeoData.getInstance().getHeight(x, y, randomNpc.getZ());
						chestSpawn.getNpc().setXYZ(x, y, z);
						sec++;
					}
					
					if (sec < 20) {
						found = true;
					}
				}
				
				chestSpawn.setX(x);
				chestSpawn.setY(y);
				chestSpawn.setZ(z);
				chestSpawn.setHeading(Rnd.get(65536));
				chestSpawn.setRespawnDelay(10);
				chestSpawn.setInstanceId(0);
				
				SpawnTable.getInstance().addNewSpawn(chestSpawn, false);
				
				chestSpawn.stopRespawn();
				chestSpawn.doSpawn();
				
				String name = "";
				for (int j = 0; j < 10; j++) {
					int rnd = (int) (Math.random() * 36);
					char c = (char) ('0' + rnd);
					if (rnd >= 10) {
						c = (char) ('a' + rnd - 10);
					}
					name += c;
				}
				
				chestSpawn.getNpc().setName(name);
				
				specialChestTasks[i] = new HiddenChestsTask(i, System.currentTimeMillis() + 3600000L * 5 + Rnd.get(3600000 * 3));
				ThreadPoolManager.getInstance().executeTask(specialChestTasks[i]);
				
				specialChestSpawns[i] = chestSpawn;
			}
		} catch (Exception e) {
			log.warn("Chest event exception:");
			e.printStackTrace();
		}
	}
	
	public void moveChest(Npc chest, final boolean delayed) {
		if (chest == null) {
			return;
		}
		
		final NpcTemplate tmpl = NpcTable.getInstance().getTemplate(50101);
		if (tmpl == null) {
			log.warn("ERROR: NPC " + chest.getObjectId() + " has a null template.");
			return;
		}
		
		int index = 0;
		for (L2Spawn scs : specialChestSpawns) {
			if (scs != null && scs.getNpc() != null && scs.getNpc() == chest) {
				break;
			}
			index++;
		}
		
		if (index >= specialChestSpawns.length) {
			log.warn("ERROR: NPC " + chest.getObjectId() + " is not in the chest spawns list.");
			chest.deleteMe();
			chest.getSpawn().stopRespawn();
			return;
		}
		
		final int respawnTime = specialChestSpawns[index].getRespawnDelay() / 1000;
		chest.deleteMe();
		specialChestSpawns[index].stopRespawn();
		SpawnTable.getInstance().deleteSpawn(specialChestSpawns[index], false);
		specialChestSpawns[index] = null;
		
		final int fIndex = index;
		
		ThreadPoolManager.getInstance().scheduleGeneral(() -> {
			try {
				
				L2Spawn chestSpawn = new L2Spawn(tmpl);
				int x = 0;
				int y = 0;
				int z = 0;
				boolean found = false;
				while (!found) {
					L2Spawn randomSpawn = SpawnTable.getInstance().getRandomDistributedSpawn();
					Npc randomNpc = randomSpawn.getNpc();
					while (randomSpawn.getNpc().getX() < 150000 || randomSpawn.getNpc().getY() > 227000 ||
							randomSpawn.getNpc().isInsideZone(Creature.ZONE_CASTLE) || randomSpawn.getNpc().isInsideZone(Creature.ZONE_CLANHALL) ||
							randomSpawn.getNpc().isInsideZone(Creature.ZONE_FORT)) {
						randomSpawn = SpawnTable.getInstance().getRandomDistributedSpawn();
					}
					
					x = randomNpc.getX() + Rnd.get(800) - 400;
					y = randomNpc.getY() + Rnd.get(800) - 400;
					z = GeoData.getInstance().getHeight(x, y, randomNpc.getZ());
					
					int sec = 0;
					while (!GeoData.getInstance().canSeeTarget(randomSpawn.getX(), randomSpawn.getY(), randomSpawn.getZ(), x, y, z) && sec < 20) {
						x = randomNpc.getX() + Rnd.get(800) - 400;
						y = randomNpc.getY() + Rnd.get(800) - 400;
						z = GeoData.getInstance().getHeight(x, y, randomNpc.getZ());
						chestSpawn.getNpc().setXYZ(x, y, z);
						sec++;
					}
					
					if (sec < 20) {
						found = true;
					}
				}
				
				chestSpawn.setX(x);
				chestSpawn.setY(y);
				chestSpawn.setZ(z);
				chestSpawn.setHeading(Rnd.get(65536));
				chestSpawn.setRespawnDelay(respawnTime);
				chestSpawn.setInstanceId(0);
				
				SpawnTable.getInstance().addNewSpawn(chestSpawn, false);
				
				chestSpawn.stopRespawn();
				chestSpawn.doSpawn();
				
				String name = "";
				for (int j = 0; j < 10; j++) {
					int rnd = (int) (Math.random() * 36);
					char c = (char) ('0' + rnd);
					if (rnd >= 10) {
						c = (char) ('a' + rnd - 10);
					}
					name += c;
				}
				
				chestSpawn.getNpc().setName(name);
				
				specialChestTasks[fIndex].setStartTime(System.currentTimeMillis() + 3600000L * 5);
				
				if (delayed) {
					Announcements.getInstance()
							.announceToAll("The treasure chest #" + (fIndex + 1) + " has respawned! Use .treasure for hints to find it.");
				}
				
				specialChestSpawns[fIndex] = chestSpawn;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}, delayed ? 6 * 3600 * 1000 : 1000);
	}
	
	public void showInfo(Player activeChar) {
		String html = "<html>" + "<body>" +
				"<center><table><tr><td><img src=icon.etc_alphabet_h_i00 width=32 height=32></td><td><img src=icon.etc_alphabet_i_i00 width=32 height=32></td><td><img src=icon.etc_alphabet_d_i00 width=32 height=32></td><td><img src=icon.etc_alphabet_d_i00 width=32 height=32></td><td><img src=icon.etc_alphabet_e_i00 width=32 height=32></td><td><img src=icon.etc_alphabet_n_i00 width=32 height=32></td></tr></table></center>" +
				"<table><tr><td><img src=icon.etc_alphabet_t_i00 width=32 height=32></td><td><img src=icon.etc_alphabet_r_i00 width=32 height=32></td><td><img src=icon.etc_alphabet_e_i00 width=32 height=32></td><td><img src=icon.etc_alphabet_a_i00 width=32 height=32></td><td><img src=icon.etc_alphabet_s_i00 width=32 height=32></td><td><img src=icon.etc_alphabet_u_i00 width=32 height=32></td><td><img src=icon.etc_alphabet_r_i00 width=32 height=32></td><td><img src=icon.etc_alphabet_e_i00 width=32 height=32></td></tr></table>" +
				"<br><br>" +
				"<table width=300><tr><td>These are all the treasures in the world with hints for you to find them:</td></tr></table><br>" +
				"<center>";
		int x = 0;
		boolean someChest = false;
		for (int i = 0; i < SPECIAL_CHEST_COUNT; i++) {
			L2Spawn chest = specialChestSpawns[i];
			if (chest == null) {
				continue;
			}
			
			long dx = activeChar.getX() - chest.getX();
			long dy = activeChar.getY() - chest.getY();
			long dz = activeChar.getZ() - chest.getZ();
			int distance = (int) Math.sqrt(dx * dx + dy * dy + dz * dz);
			String hint = "<font color=00FF00>Far far away</font>";
			if (distance < 500) {
				hint = "<font color=9B0000>Extremely hot!</font>";
			} else if (distance < 1000) {
				hint = "<font color=FF0000>Very hot!</font>";
			} else if (distance < 2000) {
				hint = "<font color=EA0000>Hot!</font>";
			} else if (distance < 5000) {
				hint = "<font color=FF2020>Very warm</font>";
			} else if (distance < 10000) {
				hint = "<font color=FF4646>Warm</font>";
			} else if (distance < 20000) {
				hint = "<font color=379BFF>Cold</font>";
			} else if (distance < 50000) {
				hint = "<font color=0074E8>Very cold</font>";
			}
			html += "<table height=50 width=300 " + (x % 2 == 0 ? "bgcolor=131210" : "") + "><tr>";
			html += "<td><img src=icon.etc_treasure_box_i08 width=32 height=32></td><td fixwidth=\"70\">Chest #" + (i + 1) + "</td>";
			html += "<td fixwidth=\"100\">" + (activeChar.isGM() ?
					"<a action=\"bypass -h admin_move_to " + chest.getX() + " " + chest.getY() + " " + chest.getZ() + "\"> " + hint + "</a>" : hint) +
					"</td>";
			html += "</tr></table>";
			someChest = true;
			x++;
		}
		if (!someChest) {
			html += "<table width=280><tr><td align=center><font color=LEVEL>There are no chests at this moment!</font></a></td></tr></table>";
		}
		html += "</center><br>";
		
		html +=
				"<center><button value=\"Reload!\" width=103 height=24 action=\"bypass -h treasure\" fore=L2UI_CT1.Button_DF_Calculator back=L2UI_CT1.Button_DF_Calculator_Over></center>";
		html += "</body></html>";
		activeChar.sendPacket(new NpcHtmlMessage(0, html));
	}
	
	class HiddenChestsTask implements Runnable {
		private int index;
		private long startTime;
		
		public HiddenChestsTask(int index, long startTime) {
			this.index = index;
			this.startTime = startTime;
		}
		
		public void setStartTime(long startTime) {
			this.startTime = startTime;
		}
		
		/**
		 * @see Runnable#run()
		 */
		@Override
		public void run() {
			long delay = startTime - System.currentTimeMillis();
			
			if (delay < 1000 && specialChestSpawns[index] != null) {
				moveChest(specialChestSpawns[index].getNpc(), false);
				ThreadPoolManager.getInstance().scheduleGeneral(this, System.currentTimeMillis() + 3600000L * 6);
			} else {
				ThreadPoolManager.getInstance().scheduleGeneral(this, delay);
			}
		}
	}
	
	class OpenChestCastFinalizer implements Runnable {
		private Player player;
		private Npc chest;
		
		OpenChestCastFinalizer(Player player, Npc chest) {
			this.player = player;
			this.chest = chest;
		}
		
		@Override
		public void run() {
			if (player.isCastingNow()) {
				player.sendPacket(new MagicSkillLaunched(player, 11030, 1));
				player.setIsCastingNow(false);
				
				if (player.getTarget() == chest && !chest.isDead() && Util.checkIfInRange(1000, player, chest, true)) {
					String name = player.getName();
					if (player.getActingPlayer() != null) {
						name = player.getActingPlayer().getName();
					}
					Announcements.getInstance().announceToAll(name + " has opened a treasure chest!");
					chest.reduceCurrentHp(chest.getMaxHp() + 1, player, null);
					
					ThreadPoolManager.getInstance().scheduleGeneral(() -> HiddenChests.getInstance().moveChest(chest, !player.isGM()), 5000L);
				}
			}
		}
	}

/*	public void tryOpenChest(Player activeChar, Npc npc)
	{
		if (activeChar == null)
		{
			return;
		}

		activeChar.stopMove(null, false);
		int castingMillis = 30000;
		activeChar.broadcastPacket(new MagicSkillUse(activeChar, 11030, 1, castingMillis, 0));
		activeChar.sendPacket(new SetupGauge(0, castingMillis));
		activeChar.sendMessage("Opening bitch...");
		activeChar.setLastSkillCast(SkillTable.getInstance().getInfo(11030, 1));
		OpenChestCastFinalizer fcf = new OpenChestCastFinalizer(activeChar, npc);
		activeChar.setSkillCast(ThreadPoolManager.getInstance().scheduleEffect(fcf, castingMillis));
		activeChar.forceIsCasting(TimeController.getGameTicks() + castingMillis / TimeController.MILLIS_IN_TICK);
	}*/
}
