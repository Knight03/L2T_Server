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

package l2server.gameserver.stats.conditions;

import l2server.gameserver.model.actor.Creature;
import l2server.gameserver.model.actor.instance.MonsterInstance;
import l2server.gameserver.model.actor.instance.Player;
import l2server.gameserver.stats.Env;

/**
 * The Class ConditionTargetAggro.
 *
 * @author mkizub
 */
public class ConditionTargetAggro extends Condition {

	private final boolean isAggro;

	/**
	 * Instantiates a new condition target aggro.
	 *
	 * @param isAggro the is aggro
	 */
	public ConditionTargetAggro(boolean isAggro) {
		this.isAggro = isAggro;
	}

	/* (non-Javadoc)
	 * @see l2server.gameserver.stats.conditions.Condition#testImpl(l2server.gameserver.stats.Env)
	 */
	@Override
	public boolean testImpl(Env env) {
		Creature target = env.target;
		if (target instanceof MonsterInstance) {
			return ((MonsterInstance) target).isAggressive() == isAggro;
		}
		if (target instanceof Player) {
			return ((Player) target).getReputation() < 0;
		}
		return false;
	}
}
