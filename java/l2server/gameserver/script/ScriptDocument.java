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

package l2server.gameserver.script;

import l2server.log.Log;
import lombok.Getter;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;

/**
 *
 *
 */
public class ScriptDocument
{
	@Getter private Document document;
	@Getter private String name;

	public ScriptDocument(String name, InputStream input)
	{
		this.name = name;

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try
		{
			DocumentBuilder builder = factory.newDocumentBuilder();
			document = builder.parse(input);
		}
		catch (SAXException sxe)
		{
			// Error generated during parsing)
			Exception x = sxe;
			if (sxe.getException() != null)
			{
				x = sxe.getException();
			}
			x.printStackTrace();
		}
		catch (ParserConfigurationException | IOException pce)
		{
			// Parser with specified options can't be built
			Log.log(Level.WARNING, "", pce);
		}
	}


	/**
	 * @return Returns the this.name.
	 */

	@Override
	public String toString()
	{
		return name;
	}
}
