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
import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.beans.transactions.HtmlTransaction;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.parsers.IWebViewer;
import com.twinsoft.convertigo.engine.parsers.HtmlParser;
import com.twinsoft.convertigo.engine.util.VersionUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class BrowserPropertyChangeStatement extends Statement implements ITagsProperty{
	private static final long serialVersionUID = -2138787100756864583L;
	
	public static String convertigoModeStudio = "studio mode";
	public static String convertigoModeEngine = "engine mode";
	public static String convertigoModeBoth = "both modes";
	
	String convertigoMode = convertigoModeBoth;
	
	
	
	public static String javascriptModeNoChange = "no change";
	public static String javascriptModeForceOn = "force on";
	public static String javascriptModeForceOff = "force off";
	
	boolean bJavascriptChange = false;
	boolean bJavascriptStat = true;
	
	String javascriptMode = javascriptModeNoChange;
	
	
	
	public static String imageModeNoChange = "no change";
	public static String imageModeForceOn = "force on";
	public static String imageModeForceOff = "force off";
	
	boolean bImageChange = false;
	boolean bImageStat = true;
	
	String imageMode = imageModeNoChange;
	
	
	
	public static String pluginModeNoChange = "no change";
	public static String pluginModeForceOn = "force on";
	public static String pluginModeForceOff = "force off";

	boolean bPluginChange = false;
	boolean bPluginStat = true;

	String pluginMode = pluginModeNoChange;
	
	
	
	public static String attachmentModeNoChange = "no change";
	public static String attachmentModeForceOn = "force on";
	public static String attachmentModeForceOff = "force off";

	boolean bAttachmentChange = false;
	boolean bAttachmentStat = true;

	String attachmentMode = attachmentModeNoChange;
	
	
	
	public static String windowOpenModeNoChange = "no change";
	public static String windowOpenModeForceOnSameWindow = "force on same window";
	public static String windowOpenModeForceOnNewWindow = "force on new window";
	public static String windowOpenModeForceOff = "force off";
	
	boolean bWindowOpenChange = false;
	boolean bWindowOpenStat = true;
	
	String windowOpenMode = windowOpenModeNoChange;
	

	
	boolean bClearCookies = false;
	
	
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
		if (isEnable) {
			if (super.execute(javascriptContext, scope)) {
				if(convertigoMode.equals(convertigoModeBoth) ||
				   convertigoMode.equals(convertigoModeStudio) && Engine.isStudioMode() ||
				   convertigoMode.equals(convertigoModeEngine) && Engine.isEngineMode() ){

					HtmlTransaction htmlTransaction = (HtmlTransaction)getParentTransaction();
					HtmlConnector htmlConnector = (HtmlConnector)htmlTransaction.getParent();

					HtmlParser htmlParser = htmlConnector.getHtmlParser();
					
					if(javascriptMode.equals(javascriptModeForceOn))
						htmlParser.setAllowJavascript(htmlTransaction.context, true);
					else if(javascriptMode.equals(javascriptModeForceOff))
						htmlParser.setAllowJavascript(htmlTransaction.context, false);
					
					if(imageMode.equals(imageModeForceOn))
						htmlParser.setAllowImage(htmlTransaction.context, true);
					else if(imageMode.equals(imageModeForceOff))
						htmlParser.setAllowImage(htmlTransaction.context, false);
					
					if(pluginMode.equals(pluginModeForceOn))
						htmlParser.setAllowPlugin(htmlTransaction.context, true);
					else if(pluginMode.equals(pluginModeForceOff))
						htmlParser.setAllowPlugin(htmlTransaction.context, false);
					
					if(attachmentMode.equals(attachmentModeForceOn))
						htmlParser.setAllowAttachment(htmlTransaction.context, true);
					else if(attachmentMode.equals(attachmentModeForceOff))
						htmlParser.setAllowAttachment(htmlTransaction.context, false);
					
					if(windowOpenMode.equals(windowOpenModeForceOnNewWindow))
						htmlParser.setWindowOpenState(htmlTransaction.context, IWebViewer.windowOpenNewWindow);
					else if(windowOpenMode.equals(windowOpenModeForceOnSameWindow))
						htmlParser.setWindowOpenState(htmlTransaction.context, IWebViewer.windowOpenSameWindow);
					else if(windowOpenMode.equals(windowOpenModeForceOff))
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
		String text = this.getComment();
		return ("props :" +
				((!javascriptMode.equals(javascriptModeNoChange))?" js"+(javascriptMode.equals(javascriptModeForceOn)?"On":"Off"):"") +
				((!imageMode.equals(imageModeNoChange))?" img"+(imageMode.equals(imageModeForceOn)?"On":"Off"):"") +
				((!pluginMode.equals(pluginModeNoChange))?" pg"+(pluginMode.equals(pluginModeForceOn)?"On":"Off"):"") +
				((!attachmentMode.equals(attachmentModeNoChange))?" attachment"+(attachmentMode.equals(pluginModeForceOn)?"On":"Off"):"") +
				((!windowOpenMode.equals(windowOpenModeNoChange))?" wo"+(windowOpenMode.equals(windowOpenModeForceOnNewWindow)?"New":(windowOpenMode.equals(windowOpenModeForceOnSameWindow)?"Same":"Off")):"") +
				((bClearCookies)?" clearCookies":"")) +
				(!text.equals("") ? " // "+text:"");
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

	public String getConvertigoMode() {
		return convertigoMode;
	}

	public void setConvertigoMode(String convertigoMode) {
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
	
	public String getJavascriptMode() {
		return javascriptMode;
	}

	public void setJavascriptMode(String javascriptMode) {
		this.javascriptMode = javascriptMode;
	}

	public String getImageMode() {
		return imageMode;
	}

	public void setImageMode(String imageMode) {
		this.imageMode = imageMode;
	}

	public String getPluginMode() {
		return pluginMode;
	}

	public void setPluginMode(String pluginMode) {
		this.pluginMode = pluginMode;
	}

	public String getAttachmentMode() {
		return attachmentMode;
	}

	public void setAttachmentMode(String attachmentMode) {
		this.attachmentMode = attachmentMode;
	}

	public String getWindowOpenMode() {
		return windowOpenMode;
	}

	public void setWindowOpenMode(String windowOpenMode) {
		this.windowOpenMode = windowOpenMode;
	}

	public String[] getTagsForProperty(String propertyName) {
		if(propertyName.equals("convertigoMode")){
			return new String[]{
					convertigoModeStudio,
					convertigoModeEngine,
					convertigoModeBoth
			};
		}else if(propertyName.equals("javascriptMode")){
			return new String[]{
					javascriptModeNoChange,
					javascriptModeForceOn,
					javascriptModeForceOff
			};
		}else if(propertyName.equals("imageMode")){
			return new String[]{
					imageModeNoChange,
					imageModeForceOn,
					imageModeForceOff
			};
		}else  if(propertyName.equals("pluginMode")){
			return new String[]{
					pluginModeNoChange,
					pluginModeForceOn,
					pluginModeForceOff
			};
		}else if(propertyName.equals("attachmentMode")){
			return new String[]{
					attachmentModeNoChange,
					attachmentModeForceOn,
					attachmentModeForceOff
			};
		}else if(propertyName.equals("windowOpenMode")){
			return new String[]{
					windowOpenModeNoChange,
					windowOpenModeForceOnNewWindow,
					windowOpenModeForceOnSameWindow,
					windowOpenModeForceOff
			};
		}
		return new String[0];
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
        	javascriptMode = bJavascriptChange?(bJavascriptStat?javascriptModeForceOn:javascriptModeForceOff):javascriptModeNoChange;
        	imageMode = bImageChange?(bImageStat?imageModeForceOn:imageModeForceOff):imageModeNoChange;
        	pluginMode = bPluginChange?(bPluginStat?pluginModeForceOn:pluginModeForceOff):pluginModeNoChange;
        	attachmentMode = bAttachmentChange?(bAttachmentStat?attachmentModeForceOn:attachmentModeForceOff):attachmentModeNoChange;
        	windowOpenMode = bWindowOpenChange?(bWindowOpenStat?windowOpenModeForceOnSameWindow:windowOpenModeForceOff):windowOpenModeNoChange;
        	
			hasChanged = true;
			Engine.logBeans.warn("[HttpStatement] The object \"" + getName()+ "\" has been updated to version 4.4.1");
        }
	}
}