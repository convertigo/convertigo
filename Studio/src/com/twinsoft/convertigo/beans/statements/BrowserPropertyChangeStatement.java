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

package com.twinsoft.convertigo.beans.statements;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.connectors.HtmlConnector;
import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.beans.transactions.HtmlTransaction;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.parsers.HtmlParser;
import com.twinsoft.convertigo.engine.parsers.IWebViewer;
import com.twinsoft.convertigo.engine.util.VersionUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class BrowserPropertyChangeStatement extends Statement {
	private static final long serialVersionUID = -2138787100756864583L;
	
	public enum ConvertigoMode {
		studio("studio mode"),
		engine("engine mode"),
		both("both modes");
		
		private final String label;
		
		private ConvertigoMode(String label) {
			this.label = label;
		}
		
		@Override
		public String toString() {
			return label;
		}
	}
	
	public enum JavascriptMode {
		noChange("no change"),
		forceOn("force on"),
		forceOff("force off");
		
		private final String label;
		
		private JavascriptMode(String label) {
			this.label = label;
		}
		
		@Override
		public String toString() {
			return label;
		}
	}
	
	public enum ImageMode {
		noChange("no change"),
		forceOn("force on"),
		forceOff("force off");
		
		private final String label;
		
		private ImageMode(String label) {
			this.label = label;
		}
		
		@Override
		public String toString() {
			return label;
		}
	}
	
	public enum PluginMode {
		noChange("no change"),
		forceOn("force on"),
		forceOff("force off");
		
		private final String label;
		
		private PluginMode(String label) {
			this.label = label;
		}
		
		@Override
		public String toString() {
			return label;
		}
	}
	
	public enum AttachmentMode {
		noChange("no change"),
		forceOn("force on"),
		forceOff("force off");
		
		private final String label;
		
		private AttachmentMode(String label) {
			this.label = label;
		}
		
		@Override
		public String toString() {
			return label;
		}
	}
	
	public enum WindowOpenMode {
		noChange("no change"),
		forceOnSameWindow("force on same window"),
		forceOnNewWindow("force on new window"),
		forceOff("force off");
		
		private final String label;
		
		private WindowOpenMode(String label) {
			this.label = label;
		}
		
		@Override
		public String toString() {
			return label;
		}
	}
	
	private ConvertigoMode convertigoMode = ConvertigoMode.both;
	
	private boolean bJavascriptChange = false;
	private boolean bJavascriptStat = true;
	
	private JavascriptMode javascriptMode = JavascriptMode.noChange;
	
	private boolean bImageChange = false;
	private boolean bImageStat = true;
	
	private ImageMode imageMode = ImageMode.noChange;

	private boolean bPluginChange = false;
	private boolean bPluginStat = true;

	private PluginMode pluginMode = PluginMode.noChange;

	private boolean bAttachmentChange = false;
	private boolean bAttachmentStat = true;

	private AttachmentMode attachmentMode = AttachmentMode.noChange;
	
	private boolean bWindowOpenChange = false;
	private boolean bWindowOpenStat = true;
	
	private WindowOpenMode windowOpenMode = WindowOpenMode.noChange;
	
	private boolean bClearCookies = false;
	
	public BrowserPropertyChangeStatement() {
		super();
	}
	
	public BrowserPropertyChangeStatement(boolean bJavascriptChange, boolean bJavascriptStat, boolean bImageChange, boolean bImageStat, boolean bWindowOpenChange, boolean bWindowOpenStat, boolean bPluginChange, boolean bPluginStat, boolean bClearCookies) {
		this.bJavascriptChange = bJavascriptChange;
		this.bJavascriptStat = bJavascriptStat;
		
		this.bImageChange = bImageChange;
		this.bImageStat = bImageStat;
		
		this.bWindowOpenChange = bWindowOpenChange;
		this.bWindowOpenStat = bWindowOpenStat;
		
		this.bPluginChange = bPluginChange;
		this.bPluginStat = bPluginStat;
		
		this.bClearCookies = bClearCookies;
	}
	
	public BrowserPropertyChangeStatement(boolean bJavascriptChange, boolean bJavascriptStat, boolean bImageChange, boolean bImageStat, boolean bWindowOpenChange, boolean bWindowOpenStat, boolean bPluginChange, boolean bPluginStat, boolean bAttachmentChange, boolean bAttachmentStat, boolean bClearCookies) {
		this.bJavascriptChange = bJavascriptChange;
		this.bJavascriptStat = bJavascriptStat;
		
		this.bImageChange = bImageChange;
		this.bImageStat = bImageStat;
		
		this.bWindowOpenChange = bWindowOpenChange;
		this.bWindowOpenStat = bWindowOpenStat;
		
		this.bPluginChange = bPluginChange;
		this.bPluginStat = bPluginStat;
		
		this.bAttachmentChange = bAttachmentChange;
		this.bAttachmentStat = bAttachmentStat;
		
		this.bClearCookies = bClearCookies;
	}

    @Override
	public boolean execute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable()) {
			if (super.execute(javascriptContext, scope)) {
				if(convertigoMode == ConvertigoMode.both ||
				   convertigoMode == ConvertigoMode.studio && Engine.isStudioMode() ||
				   convertigoMode == ConvertigoMode.engine && Engine.isEngineMode() ){

					HtmlTransaction htmlTransaction = (HtmlTransaction)getParentTransaction();
					HtmlConnector htmlConnector = (HtmlConnector)htmlTransaction.getParent();

					HtmlParser htmlParser = htmlConnector.getHtmlParser();
					
					if(javascriptMode == JavascriptMode.forceOn)
						htmlParser.setAllowJavascript(htmlTransaction.context, true);
					else if(javascriptMode == JavascriptMode.forceOff)
						htmlParser.setAllowJavascript(htmlTransaction.context, false);
					
					if(imageMode == ImageMode.forceOn)
						htmlParser.setAllowImage(htmlTransaction.context, true);
					else if(imageMode == ImageMode.forceOff)
						htmlParser.setAllowImage(htmlTransaction.context, false);
					
					if(pluginMode == PluginMode.forceOn)
						htmlParser.setAllowPlugin(htmlTransaction.context, true);
					else if(pluginMode == PluginMode.forceOff)
						htmlParser.setAllowPlugin(htmlTransaction.context, false);
					
					if(attachmentMode == AttachmentMode.forceOn)
						htmlParser.setAllowAttachment(htmlTransaction.context, true);
					else if(attachmentMode == AttachmentMode.forceOff)
						htmlParser.setAllowAttachment(htmlTransaction.context, false);
					
					if(windowOpenMode == WindowOpenMode.forceOnNewWindow)
						htmlParser.setWindowOpenState(htmlTransaction.context, IWebViewer.windowOpenNewWindow);
					else if(windowOpenMode == WindowOpenMode.forceOnSameWindow)
						htmlParser.setWindowOpenState(htmlTransaction.context, IWebViewer.windowOpenSameWindow);
					else if(windowOpenMode == WindowOpenMode.forceOff)
						htmlParser.setWindowOpenState(htmlTransaction.context, IWebViewer.windowOpenCancel);
					
					if(bClearCookies){
						htmlConnector.resetHttpState(htmlTransaction.context);
					}
				}	

				return true;
			}
		}
		return false;
	}

    @Override
	public String toString(){
		return ("props :" +
				(javascriptMode != JavascriptMode.noChange ? " js" + (javascriptMode == JavascriptMode.forceOn ? "On" : "Off") : "") +
				(imageMode != ImageMode.noChange ? " img" + (imageMode == ImageMode.forceOn ? "On" : "Off") : "") +
				(pluginMode != PluginMode.noChange ? " pg" + (pluginMode == PluginMode.forceOn ? "On" : "Off") : "") +
				(attachmentMode != AttachmentMode.noChange ? " attachment" + (attachmentMode == AttachmentMode.forceOn ? "On" : "Off") : "") +
				(windowOpenMode != WindowOpenMode.noChange ? " wo" + (windowOpenMode == WindowOpenMode.forceOnNewWindow ? "New" : (windowOpenMode == WindowOpenMode.forceOnSameWindow ? "Same" : "Off")) : "") +
				(bClearCookies ? " clearCookies" : ""));
	}

    @Override
	public String toJsString() {
		return "";
	}

	public boolean getBClearCookies() {
		return bClearCookies;
	}

	public void setBClearCookies(boolean clearCookies) {
		bClearCookies = clearCookies;
	}

	public boolean getBImageChange() {
		return bImageChange;
	}

	public void setBImageChange(boolean imageChange) {
		bImageChange = imageChange;
	}

	public boolean getBImageStat() {
		return bImageStat;
	}

	public void setBImageStat(boolean imageStat) {
		bImageStat = imageStat;
	}

	public boolean getBJavascriptChange() {
		return bJavascriptChange;
	}

	public void setBJavascriptChange(boolean javascriptChange) {
		bJavascriptChange = javascriptChange;
	}

	public boolean getBJavascriptStat() {
		return bJavascriptStat;
	}

	public void setBJavascriptStat(boolean javascriptStat) {
		bJavascriptStat = javascriptStat;
	}

	public boolean getBPluginChange() {
		return bPluginChange;
	}

	public void setBPluginChange(boolean pluginChange) {
		bPluginChange = pluginChange;
	}

	public boolean getBPluginStat() {
		return bPluginStat;
	}

	public void setBPluginStat(boolean pluginStat) {
		bPluginStat = pluginStat;
	}

	public boolean getBWindowOpenChange() {
		return bWindowOpenChange;
	}

	public void setBWindowOpenChange(boolean windowOpenChange) {
		bWindowOpenChange = windowOpenChange;
	}

	public boolean getBWindowOpenStat() {
		return bWindowOpenStat;
	}

	public void setBWindowOpenStat(boolean windowOpenStat) {
		bWindowOpenStat = windowOpenStat;
	}

	public ConvertigoMode getConvertigoMode() {
		return convertigoMode;
	}

	public void setConvertigoMode(ConvertigoMode convertigoMode) {
		this.convertigoMode = convertigoMode;
	}

	public boolean getBAttachmentChange() {
		return bAttachmentChange;
	}

	public void setBAttachmentChange(boolean attachmentChange) {
		bAttachmentChange = attachmentChange;
	}

	public boolean getBAttachmentStat() {
		return bAttachmentStat;
	}

	public void setBAttachmentStat(boolean attachmentStat) {
		bAttachmentStat = attachmentStat;
	}
	
	public JavascriptMode getJavascriptMode() {
		return javascriptMode;
	}

	public void setJavascriptMode(JavascriptMode javascriptMode) {
		this.javascriptMode = javascriptMode;
	}

	public ImageMode getImageMode() {
		return imageMode;
	}

	public void setImageMode(ImageMode imageMode) {
		this.imageMode = imageMode;
	}

	public PluginMode getPluginMode() {
		return pluginMode;
	}

	public void setPluginMode(PluginMode pluginMode) {
		this.pluginMode = pluginMode;
	}

	public AttachmentMode getAttachmentMode() {
		return attachmentMode;
	}

	public void setAttachmentMode(AttachmentMode attachmentMode) {
		this.attachmentMode = attachmentMode;
	}

	public WindowOpenMode getWindowOpenMode() {
		return windowOpenMode;
	}

	public void setWindowOpenMode(WindowOpenMode windowOpenMode) {
		this.windowOpenMode = windowOpenMode;
	}

	@Override
	public void preconfigure(Element element) throws Exception {
		super.preconfigure(element);
		
		String version = element.getAttribute("version");
        
        if (version == null) {
            String s = XMLUtils.prettyPrintDOM(element);
            EngineException ee = new EngineException(
                "Unable to find version number for the database object \"" + getName() + "\".\n" +
                "XML data: " + s
            );
            throw ee;
        }
        
        if (VersionUtils.compare(version, "5.2.0") < 0) {
        	NodeList properties = element.getElementsByTagName("property");
			
			Element propConvertigoMode = (Element) XMLUtils.findNodeByAttributeValue(properties, "name", "convertigoMode");
			String convertigoModeValue = (String) XMLUtils.readObjectFromXml((Element) XMLUtils.findChildNode(propConvertigoMode, Node.ELEMENT_NODE));
			
			if (convertigoModeValue.equals("both mode")){
				Element convertigoModeValueNode = (Element) XMLUtils.findChildNode(propConvertigoMode, Node.ELEMENT_NODE);
				convertigoModeValueNode.setAttribute("value", "both modes");
				hasChanged = true;
				Engine.logBeans.warn("[HttpStatement] The object \"" + getName()+ "\" has been updated to version 5.2.0");
			}
        }
	}
	
    @Override
	public void configure(Element element) throws Exception {
		super.configure(element);
		
        String version = element.getAttribute("version");
        
        if (version == null) {
            String s = XMLUtils.prettyPrintDOM(element);
            EngineException ee = new EngineException(
                "Unable to find version number for the database object \"" + getName() + "\".\n" +
                "XML data: " + s
            );
            throw ee;
        }
        
        if (VersionUtils.compare(version, "4.4.1") < 0) {
        	javascriptMode = bJavascriptChange ? (bJavascriptStat ? JavascriptMode.forceOn : JavascriptMode.forceOff) : JavascriptMode.noChange;
        	imageMode = bImageChange? (bImageStat ? ImageMode.forceOn : ImageMode.forceOff) : ImageMode.noChange;
        	pluginMode = bPluginChange ? (bPluginStat ? PluginMode.forceOn : PluginMode.forceOff) : PluginMode.noChange;
        	attachmentMode = bAttachmentChange ? (bAttachmentStat ? AttachmentMode.forceOn : AttachmentMode.forceOff) : AttachmentMode.noChange;
        	windowOpenMode = bWindowOpenChange ? (bWindowOpenStat ? WindowOpenMode.forceOnSameWindow : WindowOpenMode.forceOff) : WindowOpenMode.noChange;
        	
			hasChanged = true;
			Engine.logBeans.warn("[HttpStatement] The object \"" + getName()+ "\" has been updated to version 4.4.1");
        }
	}
}