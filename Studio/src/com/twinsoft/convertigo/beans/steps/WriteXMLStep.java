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
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class WriteXMLStep extends WriteFileStep {

	private static final long serialVersionUID = 2781335492473421310L;

	private String defaultRootTagname = "document";

	public WriteXMLStep() {
		super();
	}

	@Override
    public WriteXMLStep clone() throws CloneNotSupportedException {
    	WriteXMLStep clonedObject = (WriteXMLStep) super.clone();
        return clonedObject;
    }

	@Override
    public WriteXMLStep copy() throws CloneNotSupportedException {
    	WriteXMLStep copiedObject = (WriteXMLStep) super.copy();
        return copiedObject;
    }

	@Override
	public String toString() {
		String label = "";
		try {
			label += super.getLabel();
		} catch (EngineException e) {}
		return "WriteXML" + label;
	}

	protected void writeFile(String filePath, NodeList nodeList) throws EngineException {
		if (nodeList == null) {
			throw new EngineException("Unable to write to xml file: element is Null");
		}
		
		String fullPathName = getAbsoluteFilePath(filePath);
		synchronized (Engine.theApp.filePropertyManager.getMutex(fullPathName)) {
			try {
				String encoding = getEncoding();
				encoding = encoding.length() > 0 && Charset.isSupported(encoding) ? encoding : "UTF-8";
				if (!isReallyAppend(fullPathName)) {
					String tTag = defaultRootTagname.length() > 0 ? StringUtils.normalize(defaultRootTagname) : "document";
					FileUtils.write(new File(fullPathName), "<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>\n<" + tTag + "/>", encoding);
				}
				
				StringBuffer content = new StringBuffer();
				
				/* do the content, only append child element */
				for (int i = 0; i < nodeList.getLength(); i++) {
					if (nodeList.item(i).getNodeType() == Node.ELEMENT_NODE) {
						content.append(XMLUtils.prettyPrintElement((Element) nodeList.item(i), true, true));
					}
				}
				
				/* detect current xml encoding */
				RandomAccessFile randomAccessFile = null;
				try {
					randomAccessFile = new RandomAccessFile(fullPathName, "rw");
					FileChannel fc = randomAccessFile.getChannel();
					ByteBuffer buf = ByteBuffer.allocate(60);
					int nb = fc.read(buf);
					String sbuf = new String(buf.array(), 0, nb, "ASCII");
					String enc = sbuf.replaceFirst("^.*encoding=\"", "").replaceFirst("\"[\\d\\D]*$", "");
					
					if (!Charset.isSupported(enc)) {
						enc = encoding;
					}
					
					buf.clear();
					
					/* retrieve last header tag*/
					long pos = fc.size() - buf.capacity();
					if (pos < 0) {
						pos = 0;
					}
					
					nb = fc.read(buf, pos);
					
					boolean isUTF8 = Charset.forName(enc) == Charset.forName("UTF-8");
					
					if (isUTF8) {
						for (int i = 0; i < buf.capacity(); i++) {
							sbuf = new String(buf.array(), i, nb - i, enc);
							if (!sbuf.startsWith("�")) {
								pos += i;
								break;
							}	
						}
					} else {
						sbuf = new String(buf.array(), 0, nb, enc);
					}
					
					int lastTagIndex = sbuf.lastIndexOf("</");
					if (lastTagIndex == -1) {
						int iend = sbuf.lastIndexOf("/>");
						if (iend != -1) {
							lastTagIndex = sbuf.lastIndexOf("<", iend);
							String tagname = sbuf.substring(lastTagIndex + 1, iend);
							content = new StringBuffer("<" + tagname + ">\n" + content.toString() + "</" + tagname + ">");
						} else {
							throw new EngineException("Malformed XML file");
						}
					} else {
						content.append(sbuf.substring(lastTagIndex));
						
						if (isUTF8) {
							String before = sbuf.substring(0, lastTagIndex);
							lastTagIndex = before.getBytes(enc).length;
						}
					}
					fc.write(ByteBuffer.wrap(content.toString().getBytes(enc)), pos + lastTagIndex);
				} finally {
					if (randomAccessFile != null) {
						randomAccessFile.close();
					}
				}
			} catch (IOException e) {
				throw new EngineException("Unable to write to xml file",e);
			} finally {
				Engine.theApp.filePropertyManager.releaseMutex(fullPathName);
			}
		}
	}

	public String getDefaultRootTagname() {
		return defaultRootTagname;
	}

	public void setDefaultRootTagname(String defaultRootTagname) {
		this.defaultRootTagname = defaultRootTagname;
	}
}
