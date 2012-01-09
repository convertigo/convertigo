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

package com.twinsoft.convertigo.engine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.transactions.HttpTransaction;
import com.twinsoft.convertigo.engine.util.Base64v21;

public class AttachmentManager {
	public interface AttachmentDetails {
		byte[] getData();
		String getName();
		String getContentType();
	}
	
    public enum Policy {
    	localfile_increment,
    	localfile_override,
    	base64
    }
    
    public enum Status {
    	direct,
    	threshold,
    	timeout,
    	inprogress
    }
    
    private HttpTransaction transaction = null;
    
    public AttachmentManager(HttpTransaction transaction) {
    	this.transaction = transaction;
    }
    
    public void addAttachment(final byte[] data, final String name, final String contentType, String referer, String url, Policy policy, String filename, Status status) {
    	
    	Element eAttachment = transaction.context.outputDocument.createElement("attachment");
    	
    	eAttachment.setAttribute("type", "attachment");
    	eAttachment.setAttribute("name", name);
    	eAttachment.setAttribute("content-type", contentType);
    	if (referer != null) {
    		eAttachment.setAttribute("referer", referer);
    	}
    	if (url != null) {
    		eAttachment.setAttribute("url", url);
    	}
    	eAttachment.setAttribute("content-length", "" +  data.length);
    	eAttachment.setAttribute("status", status.name());

    	if (status == Status.timeout) {
			policy = Policy.base64;
		} else if (policy == null) {
    		policy = Policy.localfile_increment;
    	}
    	
    	switch (policy) {
    		case base64:
    			eAttachment.setAttribute("encoding", "base64");
				eAttachment.appendChild(transaction.context.outputDocument.createTextNode(Base64v21.encodeBytes(data)));
    			break;
    		case localfile_override:
    		case localfile_increment:
    		default:
    			if (filename == null || filename.length() == 0) {
    				filename = ".//downloads/" + name;
    			}
    			String projectName = transaction.getProject().getName();
				filename = Engine.theApp.filePropertyManager.getFilepathFromProperty(filename, projectName);
				File file = new File(filename);
				file.getParentFile().mkdirs();
				if (policy == Policy.localfile_increment && file.exists()) {
					int cpt = 2;
					String leaf = file.getName();
					while ((file = new File(file.getParentFile(), cpt + "_" + leaf)).exists()) {
						cpt++;
					}
				}

				synchronized (Engine.theApp.filePropertyManager.getMutex(file)) {
					try {
						FileOutputStream fileOut = new FileOutputStream(file);
						fileOut.write(data);
						fileOut.close();
					} catch(IOException e) {
						Engine.logEngine.error("AttachmentComponent: can't write attachment '" + name + "'", e);
					} finally {
						Engine.theApp.filePropertyManager.releaseMutex(file);
					}
				}

				String filepath = file.getAbsolutePath();
				eAttachment.setAttribute("local-url", filepath);
				String projectPath = Engine.PROJECTS_PATH + File.separator + projectName + File.separator;
				if (filepath.startsWith(projectPath)) {
					eAttachment.setAttribute("relative-url", filepath.substring(projectPath.length()).replaceAll("\\\\", "/"));
				}
				break;
    	}
    	
    	transaction.context.outputDocument.getDocumentElement().appendChild(eAttachment);
    }

	static public AttachmentDetails getAttachment(Element eAttachment) {
		try {
			if ("attachment".equals(eAttachment.getTagName()) && "attachment".equals(eAttachment.getAttribute("type"))) {
				String attr; 
				final String name = eAttachment.getAttribute("name");
				final String contentType = eAttachment.getAttribute("content-type");
				final byte[][] data = new byte[1][];
				if ((attr = eAttachment.getAttribute("local-url")) != null && attr.length() > 0) {
					FileInputStream fis = new FileInputStream(attr);
					fis.read(data[0] = new byte[fis.available()]);
				} else if ((attr = eAttachment.getAttribute("encoding")) != null && attr.length() > 0) {
					if ("base64".equals(attr)) {
						data[0] = Base64v21.decode(eAttachment.getTextContent());
					}
				}
				if (data[0] != null) {
					return new AttachmentDetails() {
						public byte[] getData() {
							return data[0];
						}
						public String getName() {
							return name;
						}
						public String getContentType() {
							return contentType;
						}
					};
				}
			}
		} catch (Exception e) {
			Engine.logEngine.error("failed to make AttachmentDetails", e);
		}
		return null;
	}
}