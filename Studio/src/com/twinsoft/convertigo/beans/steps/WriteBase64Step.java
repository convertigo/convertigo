/*
 * Copyright (c) 2001-2011 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.beans.steps;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.Base64;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class WriteBase64Step extends WriteFileStep {

	private static final long serialVersionUID = 2781335492473421310L;

	public WriteBase64Step() {
		super();
	}

	@Override
    public WriteBase64Step clone() throws CloneNotSupportedException {
    	WriteBase64Step clonedObject = (WriteBase64Step) super.clone();
        return clonedObject;
    }

	@Override
    public WriteBase64Step copy() throws CloneNotSupportedException {
    	WriteBase64Step copiedObject = (WriteBase64Step) super.copy();
        return copiedObject;
    }

	@Override
	public String toString() {
		String text = this.getComment();
		String label = "";
		try {
			label += super.getLabel();
		} catch (EngineException e) {}
		return "WriteBase64" + label + (!text.equals("") ? " // "+text:"");
	}

	protected void writeFile(String filePath, NodeList nodeList) throws EngineException {
		if (nodeList == null) {
			throw new EngineException("Unable to write to xml file: element is Null");
		}
		
		String fullPathName = getAbsoluteFilePath(filePath);
		synchronized (Engine.theApp.filePropertyManager.getMutex(fullPathName)) {
			try {
				for (Node node : XMLUtils.toNodeArray(nodeList)) {
					try {
						String content = node instanceof Element ? ((Element) node).getTextContent() : node.getNodeValue();
						if (content != null && content.length() > 0) {
							byte[] bytes = Base64.decode(content);
							if (bytes != null && bytes.length > 0) {
								FileUtils.writeByteArrayToFile(new File(fullPathName), bytes);
								return;
							}
						}
					} catch (Exception e) {
						Engine.logBeans.info("(WriteBase64Step) Failed to decode and write base64 content : " + e.getClass().getCanonicalName());
					}
				}
			} finally {
				Engine.theApp.filePropertyManager.releaseMutex(fullPathName);
			}
		}
	}
}
