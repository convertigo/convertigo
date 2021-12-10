/*
 * Copyright (c) 2001-2021 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.beans.steps;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.codehaus.jettison.json.JSONException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class WriteJSONStep extends WriteFileStep {

	private static final long serialVersionUID = 2781335492473141310L;

	public WriteJSONStep() {
		super();
	}

	@Override
	public WriteJSONStep clone() throws CloneNotSupportedException {
		WriteJSONStep clonedObject = (WriteJSONStep) super.clone();
		return clonedObject;
	}

	@Override
	public WriteJSONStep copy() throws CloneNotSupportedException {
		WriteJSONStep copiedObject = (WriteJSONStep) super.copy();
		return copiedObject;
	}

	@Override
	public String toString() {
		String label = "";
		try {
			label += super.getLabel();
		} catch (EngineException e) {}
		return "WriteJSON" + label;
	}

	protected void writeFile(String filePath, NodeList nodeList) throws EngineException {
		if (nodeList == null) {
			throw new EngineException("Unable to write to json file: element is Null");
		}
		File file = new File(getAbsoluteFilePath(filePath));
		synchronized (Engine.theApp.filePropertyManager.getMutex(file)) {
			try {
				String enc = getEncoding();
				
				StringBuilder sb = new StringBuilder();
				boolean isArray = isAppendResult() || nodeList.getLength() > 1;
				for (int i = 0; i < nodeList.getLength();) {
					Node node = nodeList.item(i);
					if (node instanceof Element) {
						try {
							sb.append(XMLUtils.XmlToJson((Element) node, true, true, null));
						} catch (JSONException e) {
							sb.append(node.getNodeValue());
						}
					} else {
						sb.append(node.getNodeValue());
					}
					if (++i < nodeList.getLength()) {
						sb.append(",\n");
					}
				}
				
				boolean writeAll = true;
				if (isAppendResult()) {
					try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw")) {
						FileChannel fc = randomAccessFile.getChannel();
						byte[] bChar = new byte[1];
						String str = null;
						boolean empty = true;
						if (file.exists() && file.length() >= 2) {
							fc.read(ByteBuffer.wrap(bChar), 0);
							str = new String(bChar, StandardCharsets.US_ASCII);
							empty = !"[".equals(str);
							if (!empty) {
								fc.read(ByteBuffer.wrap(bChar), 1);
								str = new String(bChar, StandardCharsets.US_ASCII);
								empty = "]".equals(str);
							}
						}
						if (!empty) {
							long min = Math.max(0, fc.size() - 10);
							long i;
							for (i = fc.size() - 1; i > min; i--) {
								fc.read(ByteBuffer.wrap(bChar), i);
								str = new String(bChar, StandardCharsets.US_ASCII);
								if (!str.isBlank()) {
									break;
								}
							}
							if ("]".equals(str)) {
								sb.insert(0, ",\n");
								sb.append("]");
								fc.write(ByteBuffer.wrap(sb.toString().getBytes(enc)), i);
								writeAll = false;
							}
						}
					}
				}
				
				if (writeAll) {
					if (isArray) {
						sb.insert(0, "[").append("]");
					}
					FileUtils.write(file, sb.toString(), enc);
				}
			} catch (IOException e) {
				throw new EngineException("Unable to write to json file",e);
			} finally {
				Engine.theApp.filePropertyManager.releaseMutex(file);
			}
		}
	}
}
