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

package ai.individual;

import ai.group_template.L2AttackableAIScript;
import l2server.gameserver.GeoData;
import l2server.gameserver.datatables.SkillTable;
import l2server.gameserver.datatables.SpawnTable;
import l2server.gameserver.model.L2Spawn;
import l2server.gameserver.model.Skill;
import l2server.gameserver.model.actor.Npc;
import l2server.gameserver.model.actor.instance.Player;
import l2server.gameserver.util.Util;

/**
 * @author LasTravel
 * <p>
 * Wisp AI
 * <p>
 * Source:
 * - http://l2wiki.com/Fairy_Settlement
 */

public class Wisps extends L2AttackableAIScript {
	private static final int wisp = 32915;
	private static final int largeWisp = 32916;
	private static final Skill healSkill = SkillTable.getInstance().getInfo(14064, 1);

	public Wisps(int id, String name, String descr) {
		super(id, name, descr);

		addSpawnId(wisp);
		addSpawnId(largeWisp);

		addAggroRangeEnterId(wisp);
		addAggroRangeEnterId(largeWisp);

		addSpellFinishedId(wisp);
		addSpellFinishedId(largeWisp);

		for (L2Spawn spawn : SpawnTable.getInstance().getSpawnTable()) {
			if (spawn == null) {
				continue;
			}

			if (spawn.getNpcId() == wisp || spawn.getNpcId() == largeWisp) {
				notifySpawn(spawn.getNpc());
			}
		}
	}

	@Override
	public String onSpellFinished(Npc npc, Player player, Skill skill) {
		npc.doDie(null);

		return super.onSpellFinished(npc, player, skill);
	}

	@Override
	public String onAggroRangeEnter(Npc npc, Player player, boolean isPet) {
		if (!Util.checkIfInRange(500, player, npc, false) || !GeoData.getInstance().canSeeTarget(player, npc) || player.isDead() ||
				player.isInvul(npc) || player.getPvpFlag() > 0 || player.isFakeDeath()) {
			return super.onAggroRangeEnter(npc, player, isPet);
		}

		npc.setTarget(player);
		npc.doCast(healSkill);

		return super.onAggroRangeEnter(npc, player, isPet);
	}

	@Override
	public final String onSpawn(Npc npc) {
		npc.setIsImmobilized(true);
		npc.setIsInvul(true);

		return super.onSpawn(npc);
	}

	public static void main(String[] args) {
		new Wisps(-1, "Wisps", "ai");
	}
}
