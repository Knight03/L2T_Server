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

package l2server.gameserver.network.clientpackets;

import l2server.Config;
import l2server.gameserver.datatables.SkillTable;
import l2server.gameserver.model.Skill;
import l2server.gameserver.model.actor.instance.PetInstance;
import l2server.gameserver.model.actor.instance.Player;
import l2server.gameserver.model.actor.instance.SummonInstance;
import l2server.gameserver.templates.skills.SkillTargetType;
import l2server.gameserver.templates.skills.SkillType;

/**
 * @author KenM
 */
public class RequestDispel extends L2GameClientPacket {
	private int objectId;
	private int skillId;
	private int skillLevel;

	/**
	 * @see l2server.gameserver.network.clientpackets.L2GameClientPacket#readImpl()
	 */
	@Override
	protected void readImpl() {
		objectId = readD();
		skillId = readD();
		skillLevel = readD();
	}

	/**
	 * @see l2server.gameserver.network.clientpackets.L2GameClientPacket#runImpl()
	 */
	@Override
	protected void runImpl() {
		if (skillId <= 0 || skillLevel <= 0) {
			return;
		}

		final Player activeChar = getClient().getActiveChar();
		if (activeChar == null) {
			return;
		}

		Skill skill = SkillTable.getInstance().getInfo(skillId, skillLevel);
		if (skill == null) {
			return;
		}
		if (!skill.canBeDispeled() || skill.isStayAfterDeath() || skill.isDebuff()) {
			return;
		}
		if (skill.getTransformId() > 0 && skill.getTargetType() != SkillTargetType.TARGET_SELF &&
				skill.getTargetType() != SkillTargetType.TARGET_PARTY &&
				skill.getSkillType() != SkillType.BUFF) //LasTravel: Self/Party transformation buffs can be cancelled
		{
			return;
		}
		if (skill.isDance() && !Config.DANCE_CANCEL_BUFF) {
			return;
		}
		if (activeChar.getObjectId() == objectId) {
			activeChar.stopSkillEffects(skillId);
		} else {
			final PetInstance pet = activeChar.getPet();
			if (pet != null && pet.getObjectId() == objectId) {
				pet.stopSkillEffects(skillId);
			}
			for (SummonInstance summon : activeChar.getSummons()) {
				summon.stopSkillEffects(skillId);
			}
		}
	}
}
