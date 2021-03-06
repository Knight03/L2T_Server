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

package l2server.loginserver.network.clientpackets;

import l2server.Config;
import l2server.DatabasePool;
import l2server.loginserver.LoginController;
import l2server.loginserver.SessionKey;
import l2server.loginserver.network.serverpackets.LoginFail.LoginFailReason;
import l2server.loginserver.network.serverpackets.PlayFail.PlayFailReason;
import l2server.loginserver.network.serverpackets.PlayOk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * Fromat is ddc
 * d: first part of session id
 * d: second part of session id
 * c: server ID
 */
public class RequestServerLogin extends L2LoginClientPacket {
	private static Logger log = LoggerFactory.getLogger(RequestServerLogin.class.getName());


	private int skey1;
	private int skey2;
	private int serverId;
	
	public int getSessionKey1() {
		return skey1;
	}
	
	public int getSessionKey2() {
		return skey2;
	}
	
	public int getServerID() {
		return serverId;
	}
	
	@Override
	public boolean readImpl() {
		if (super.buf.remaining() >= 9) {
			skey1 = readD();
			skey2 = readD();
			serverId = readC();
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public void run() {
		SessionKey sk = getClient().getSessionKey();
		
		// if we didnt showed the license we cant check these values
		if (!Config.SHOW_LICENCE || sk.checkLoginPair(skey1, skey2)) {
			//System.out.println("Logging Into Server " + serverId);
			int logIntoDimensionId = 0;
			if (serverId == 32) {
				serverId = 31;
				
				logIntoDimensionId = 1;

				/*
				if (getClient().getAccessLevel() <= 0)
				{
					getClient().close(LoginFailReason.REASON_ACCESS_FAILED);
					return;
				}*/
			}
			
			if (LoginController.getInstance().isLoginPossible(getClient(), serverId)) {
				getClient().setJoinedGS(true);
				getClient().sendPacket(new PlayOk(sk));
				
				if (!Config.DATABASE_LOGIN.contains("tenkai")) {
					//if (logIntoDimensionId == 1)
					{
						Connection con = null;
						PreparedStatement statement = null;
						try {
							con = DatabasePool.getInstance().getConnection();
							
							String stmt = "UPDATE accounts SET lastDimensionId = ? WHERE login = ?";
							statement = con.prepareStatement(stmt);
							statement.setInt(1, logIntoDimensionId);
							statement.setString(2, getClient().getAccount());
							statement.executeUpdate();
							statement.close();
						} catch (Exception e) {
							log.warn("Could not set LastDimensionId: " + e.getMessage(), e);
						} finally {
							DatabasePool.close(con);
						}
						
						log.info("Update LastDimensionId = " + logIntoDimensionId + " for " + getClient().getAccount());
					}
				}
			} else {
				getClient().close(PlayFailReason.REASON_SERVER_OVERLOADED);
			}
		} else {
			getClient().close(LoginFailReason.REASON_ACCESS_FAILED);
		}
	}
}
