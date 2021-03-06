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

package handlers.chathandlers;

import l2server.gameserver.handler.IChatHandler;
import l2server.gameserver.model.PartyMatchRoom;
import l2server.gameserver.model.PartyMatchRoomList;
import l2server.gameserver.model.actor.instance.Player;
import l2server.gameserver.network.serverpackets.CreatureSay;

/**
 * A chat handler
 *
 * @author Gnacik
 */
public class ChatPartyMatchRoom implements IChatHandler {
	private static final int[] COMMAND_IDS = {14};

	/**
	 * Handle chat type 'partymatchroom'
	 */
	@Override
	public void handleChat(int type, Player activeChar, String target, String text) {
		if (activeChar.isInPartyMatchRoom()) {
			PartyMatchRoom room = PartyMatchRoomList.getInstance().getPlayerRoom(activeChar);
			if (room != null) {
				CreatureSay cs = new CreatureSay(activeChar, type, activeChar.getName(), text);
				for (Player member : room.getPartyMembers()) {
					member.sendPacket(cs);
				}
			}
		}
	}

	/**
	 * Returns the chat types registered to this handler
	 */
	@Override
	public int[] getChatTypeList() {
		return COMMAND_IDS;
	}

	public static void main(String[] args) {
		new ChatPartyMatchRoom();
	}
}
