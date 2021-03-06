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
import l2server.gameserver.TradeController;
import l2server.gameserver.model.L2TradeList;
import l2server.gameserver.model.WorldObject;
import l2server.gameserver.model.actor.Creature;
import l2server.gameserver.model.actor.instance.MerchantInstance;
import l2server.gameserver.model.actor.instance.MerchantSummonInstance;
import l2server.gameserver.model.actor.instance.Player;
import l2server.gameserver.network.serverpackets.ActionFailed;
import l2server.gameserver.network.serverpackets.ExSellList;
import l2server.gameserver.network.serverpackets.StatusUpdate;
import l2server.gameserver.util.Util;

import java.util.List;

import static l2server.gameserver.model.actor.Npc.DEFAULT_INTERACTION_DISTANCE;
import static l2server.gameserver.model.itemcontainer.PcInventory.MAX_ADENA;

/**
 * RequestSellItem client packet class.
 */
public final class RequestSellItem extends L2GameClientPacket {
	//

	private static final int BATCH_LENGTH = 16; // length of the one item

	private int listId;
	private Item[] items = null;

	/**
	 * packet type id 0x1e
	 * <p>
	 * sample
	 * <p>
	 * 1e
	 * 00 00 00 00		// list id
	 * 02 00 00 00		// number of items
	 * <p>
	 * 71 72 00 10		// object id
	 * ea 05 00 00		// item id
	 * 01 00 00 00		// item count
	 * <p>
	 * 76 4b 00 10		// object id
	 * 2e 0a 00 00		// item id
	 * 01 00 00 00		// item count
	 * <p>
	 * format:		cdd (ddd)
	 */

	@Override
	protected void readImpl() {
		listId = readD();
		int count = readD();
		if (count <= 0 || count > Config.MAX_ITEM_IN_PACKET || count * BATCH_LENGTH != buf.remaining()) {
			return;
		}

		items = new Item[count];
		for (int i = 0; i < count; i++) {
			int objectId = readD();
			int itemId = readD();
			long cnt = readQ();
			if (objectId < 1 || itemId < 1 || cnt < 1) {
				items = null;
				return;
			}
			items[i] = new Item(objectId, itemId, cnt);
		}
	}

	@Override
	protected void runImpl() {
		processSell();
	}

	protected void processSell() {
		Player player = getClient().getActiveChar();

		if (player == null) {
			return;
		}

		if (!getClient().getFloodProtectors().getTransaction().tryPerformAction("sell")) {
			player.sendMessage("You are selling too fast.");
			return;
		}

		if (items == null) {
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		// Alt game - Karma punishment
		if (!Config.ALT_GAME_KARMA_PLAYER_CAN_SHOP && player.getReputation() < 0) {
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		WorldObject target = player.getTarget();
		Creature merchant = null;
		if (!player.isGM()) {
			if (target == null || !player.isInsideRadius(target, DEFAULT_INTERACTION_DISTANCE, true, false)
					// Distance is too far)
					|| target.getInstanceId() != player.getObjectId() && player.getInstanceId() != target.getInstanceId()) {
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			if (target instanceof MerchantInstance || target instanceof MerchantSummonInstance) {
				merchant = (Creature) target;
			} else {
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}

		double taxRate = 0;

		L2TradeList list = null;
		if (merchant != null) {
			List<L2TradeList> lists;
			if (merchant instanceof MerchantInstance) {
				lists = TradeController.INSTANCE.getBuyListByNpcId(((MerchantInstance) merchant).getNpcId());
				taxRate = ((MerchantInstance) merchant).getMpc().getTotalTaxRate();
			} else {
				lists = TradeController.INSTANCE.getBuyListByNpcId(((MerchantSummonInstance) merchant).getNpcId());
				taxRate = 50;
			}

			if (!player.isGM()) {
				if (lists == null) {
					Util.handleIllegalPlayerAction(player,
							"Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " sent a false BuyList list_id " +
									listId,
							Config.DEFAULT_PUNISH);
					return;
				}
				for (L2TradeList tradeList : lists) {
					if (tradeList.getListId() == listId) {
						list = tradeList;
					}
				}
			} else {
				list = TradeController.INSTANCE.getBuyList(listId);
			}
		} else {
			list = TradeController.INSTANCE.getBuyList(listId);
		}

		if (list == null) {
			Util.handleIllegalPlayerAction(player,
					"Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " sent a false BuyList list_id " + listId,
					Config.DEFAULT_PUNISH);
			return;
		}

		long totalPrice = 0;
		// Proceed the sell
		for (Item i : items) {
			l2server.gameserver.model.Item item = player.checkItemManipulation(i.getObjectId(), i.getCount(), "sell");
			if (item == null || !item.isSellable()) {
				continue;
			}

			long price = item.getItem().getSalePrice();
			totalPrice += price * i.getCount();
			if (MAX_ADENA / i.getCount() < price || totalPrice > MAX_ADENA) {
				Util.handleIllegalPlayerAction(player,
						"Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " tried to purchase over " +
								MAX_ADENA + " adena worth of goods.",
						Config.DEFAULT_PUNISH);
				return;
			}

			if (Config.ALLOW_REFUND) {
				item = player.getInventory().transferItem("Sell", i.getObjectId(), i.getCount(), player.getRefund(), player, merchant);
			} else {
				item = player.getInventory().destroyItem("Sell", i.getObjectId(), i.getCount(), player, merchant);
			}
		}

		if (totalPrice != 0) {
			player.addAdena("Sell", totalPrice, merchant, true);
		}

		// Update current load as well
		StatusUpdate su = new StatusUpdate(player);
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(su);
		player.sendPacket(new ExSellList(player, list, taxRate, true));
	}

	private static class Item {
		private final int objectId;
		private final long count;

		public Item(int objId, int id, long num) {
			objectId = objId;
			count = num;
		}

		public int getObjectId() {
			return objectId;
		}

		public long getCount() {
			return count;
		}
	}
}
