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

package com.twinsoft.convertigo.eclipse.moz;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.mozilla.interfaces.inIFlasher;
import org.mozilla.interfaces.nsIDOMAttr;
import org.mozilla.interfaces.nsIDOMDocument;
import org.mozilla.interfaces.nsIDOMElement;
import org.mozilla.interfaces.nsIDOMEvent;
import org.mozilla.interfaces.nsIDOMEventTarget;
import org.mozilla.interfaces.nsIDOMHTMLCollection;
import org.mozilla.interfaces.nsIDOMHTMLElement;
import org.mozilla.interfaces.nsIDOMHTMLFormElement;
import org.mozilla.interfaces.nsIDOMHTMLInputElement;
import org.mozilla.interfaces.nsIDOMHTMLOptionElement;
import org.mozilla.interfaces.nsIDOMHTMLOptionsCollection;
import org.mozilla.interfaces.nsIDOMHTMLSelectElement;
import org.mozilla.interfaces.nsIDOMHTMLTextAreaElement;
import org.mozilla.interfaces.nsIDOMMouseEvent;
import org.mozilla.interfaces.nsIDOMNode;
import org.mozilla.interfaces.nsIHttpChannel;
import org.mozilla.interfaces.nsIHttpHeaderVisitor;
import org.mozilla.interfaces.nsIInputStream;
import org.mozilla.interfaces.nsIMIMEInputStream;
import org.mozilla.interfaces.nsIPrefBranch;
import org.mozilla.interfaces.nsIScriptableInputStream;
import org.mozilla.interfaces.nsISeekableStream;
import org.mozilla.interfaces.nsIUploadChannel;
import org.mozilla.xpcom.Mozilla;

import com.twinsoft.convertigo.beans.core.StatementWithExpressions;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.connector.EventStatementGenerator;
import com.twinsoft.convertigo.eclipse.learnproxy.http.gui.HttpProxyEvent;
import com.twinsoft.convertigo.eclipse.learnproxy.http.gui.HttpProxyEventListener;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.KeyExpiredException;
import com.twinsoft.convertigo.engine.MaxCvsExceededException;
import com.twinsoft.convertigo.engine.parsers.AbstractXulWebViewer;
import com.twinsoft.convertigo.engine.parsers.DocumentCompletedListener;
import com.twinsoft.convertigo.engine.parsers.IWebViewer;
import com.twinsoft.convertigo.engine.parsers.SelectionChangedListener;
import com.twinsoft.convertigo.engine.parsers.WebViewerTabManager;
import com.twinsoft.util.Log;


/**
 * This class implements a iWebViewer interface responsible to displaying web pages and let users to select
 * elements with the mouse. This implementation is based on XulRunner 
 * 
 * @author opic
 *
 */
public class XulWebViewerImpl extends AbstractXulWebViewer implements nsIHttpHeaderVisitor, IWebViewerStudio {
	nsIHttpChannel lastChannel;
	byte[] lastData;
	
	// Nécessaire pour appeler 'protected void checkWidget()' de Composite
	private class CompositeCheckWidget extends Composite{
		CompositeCheckWidget(Composite parent,int style){
			super(parent,style);
		}
		
		@Override
		public void checkWidget(){
			super.checkWidget();
		}
	}
	
	// Our listener vectors
	private List<SelectionChangedListener>							selectionChangedListeners = new ArrayList<SelectionChangedListener>(); //SelectionChangedListener
	private List<DocumentCompletedListener>							documentCompletedListeners = new ArrayList<DocumentCompletedListener>(); //DocumentCompletedListener
	
	// Mozilla objects
	private nsIDOMElement  					selectedElement = null;
	private nsIScriptableInputStream		scriptableInputStream = null;
		
	// Current selected element Xpath
	private String		  					selectedXpath   = null;

	// for Create BoxedDiv, use Mozilla Flasher object
	private inIFlasher						flasher = null;
	private boolean							flasher_focus = false;
	
	// for learn mode proxy setting
	private String							oldProxyServer;
	private long							oldProxyPort;
	private long							oldProxySetting;
	private StringBuffer					requestString;
	private StringBuffer					responseString;
	private StringBuffer					lastBuffer;
	private Map<String, String>								uriToRequest = new HashMap<String, String>();
	private List<HttpProxyEventListener>							httpProxyEventListeners = new ArrayList<HttpProxyEventListener>(); //HttpProxyEventListener
	
	// the Brower toolbar
	private XulToolBar						toolBar = null;
	private boolean					allowAlertBox = false;


	// SWT
	private CompositeCheckWidget			composite;
	
	/**
	 * Contructor
	 * 
	 * @param parent	the parent composite
	 * @param style		style (Must be EMBED)
	 * @throws MaxCvsExceededException 
	 * @throws KeyExpiredException 
	 */
	public XulWebViewerImpl(Context context, Composite parent, int style) throws MaxCvsExceededException, KeyExpiredException {
		super(context);
		
		tabManager = new WebViewerTabManager(parent, style);
		tabManager.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		initialize();
		ConvertigoPlugin.logDebug("=== Mozilla XulWebViewerImpl initialized for context "+ context.contextID + " ===");
	}
	
	public XulWebViewerImpl(Context context, WebViewerTabManager tabManager){
		super(context, tabManager);
		initialize();
	}
	
	@Override
	protected void removeMozillaSession() {
	}
	
	/**
	 * This method initializes this
	 * 
	 */
	private void initialize() {		
		if(display==null)display = Display.getDefault();
		
		composite = new CompositeCheckWidget(tabManager, SWT.NONE);
		
		GridLayout gl = new GridLayout(1,false);
		gl.marginHeight = gl.marginWidth = gl.verticalSpacing = 0;
        composite.setLayout(gl);
        
        createToolBar();
        createXulContainer();
        //toolBar.setMozillaBrowser(mozillaBrowser);
        
        composite.setSize(new org.eclipse.swt.graphics.Point(347,141));
        
		tabManager.add(this, composite, true);
		
        toolBar.setXulWebViewer(this);
	}
	
	// doit être appelé à  la destruction
	@Override
	public void removeBrowser() {
//		Context ctx = context;
		super.removeBrowser();
//		Engine.logBeans.debug("=== Mozilla XulWebViewerImpl released for context "+ ctx.contextID + " ===", ctx.log);
//		Engine.logBeans.debug("=== Mozilla available cvs :"+ getAvailableCVS() + " ===", ctx.log);
	}

	/**
	 * Adds a DocumentCompletedListener to the WebViewer. This listener will be fired when the document is completly loaded
	 * 
	 * @param  listener the DocumentCompletedListener to be added
	 */
	public synchronized void addDocumentCompletedListener(DocumentCompletedListener listener) {
		composite.checkWidget();
		if (listener == null)
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		documentCompletedListeners.add(listener);
	}

	
	public synchronized void removeDocumentCompletedListener(DocumentCompletedListener listener) {
		composite.checkWidget();
		if (listener == null)
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		documentCompletedListeners.remove(listener);
	}
	

	/**
	 * Adds a HttpProxyEventListener to the WebViewer. This listener will be fired when a http request is done
	 * 
	 * @param  listener the HttpProxyEventListener to be added
	 */
	public synchronized void addHttpProxyEventListener(HttpProxyEventListener listener) {
		composite.checkWidget();
		if (listener == null)
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		httpProxyEventListeners.add(listener);
	}

	
	public synchronized void removeHttpProxyEventListener(HttpProxyEventListener listener) {
		composite.checkWidget();
		if (listener == null)
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		httpProxyEventListeners.remove(listener);
	}	
	
	
	/**
	 * Adds a SelectionChangedListener to the WebViewer. This listener will be fired when the users selects an element
	 * in the web page
	 * 
	 * @param  listener the SelectionChangedListener to be added
	 */
	public void addSelectionChangedListener(SelectionChangedListener listener) {
		composite.checkWidget();
		if (listener == null)
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		selectionChangedListeners.add(listener);
	}


	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.eclipse.browser.IWebViewer#selectParent()
	 */
	public void selectParent() {
		if(selectedElement==null)return;
		nsIDOMNode node = selectedElement.getParentNode();
		nsIDOMElement elt = null;
		try{ elt = (nsIDOMElement)node.queryInterface(nsIDOMElement.NS_IDOMELEMENT_IID);}
		catch(Exception e){}
		if(elt!=null)createBoxedDiv(elt, true);
	}

	protected void selectNextOrPreviousSibling(boolean next){
		if(selectedElement==null)return;
		
		nsIDOMElement elt = null;
		try{
			nsIDOMNode node = selectedElement;
			while(elt==null){
				node = next?node.getNextSibling():node.getPreviousSibling();
				try{ elt=(nsIDOMElement)node.queryInterface(nsIDOMElement.NS_IDOMELEMENT_IID); }
				catch(Exception e){}
			}
		}catch(Exception e){}
		
		if(elt!=null)createBoxedDiv(elt, true);
	}
	
	@Override
	public void selectNextSibling() {
		selectNextOrPreviousSibling(true);
	}

	@Override
	public void selectPreviousSibling() {
		selectNextOrPreviousSibling(false);
	}


	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.eclipse.browser.IWebViewer#setProxyAddress(java.lang.String)
	 */
	@Override
	public void setProxyAddress(String address) {
		nsIPrefBranch prefs = (nsIPrefBranch)Mozilla.getInstance().getServiceManager().getServiceByContractID("@mozilla.org/preferences-service;1",nsIPrefBranch.NS_IPREFBRANCH_IID);
        String	proxyServer;
        int		proxyPort;
        
    	// skip "http://" is present
        if (address.indexOf("http://") != -1)
        		address = address.substring(7, address.length());
        
        if (address.indexOf(":") != -1) {
        	proxyServer = address.substring(0, address.indexOf(":"));
        	proxyPort   = Integer.parseInt(address.substring(address.indexOf(":")+1));
        } else {
        	proxyServer = address;
        	proxyPort   = 8080;
        }
        
    	oldProxyServer  = prefs.getCharPref("network.proxy.http");
    	oldProxyPort    = prefs.getIntPref("network.proxy.http_port");
    	oldProxySetting = prefs.getIntPref("network.proxy.type"); 
    	
    	prefs.setCharPref("network.proxy.http",proxyServer);
        prefs.setIntPref("network.proxy.http_port",proxyPort);
        prefs.setIntPref("network.proxy.type", 1);
	}

	
	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.eclipse.browser.IWebViewer#restoreProxy()
	 */
	@Override
	public void restoreProxy() {
		nsIPrefBranch prefs = (nsIPrefBranch)Mozilla.getInstance().getServiceManager().getServiceByContractID("@mozilla.org/preferences-service;1",nsIPrefBranch.NS_IPREFBRANCH_IID);

		prefs.setCharPref("network.proxy.http",oldProxyServer);
		prefs.setIntPref("network.proxy.http_port",(int)oldProxyPort);
		prefs.setIntPref("network.proxy.type",(int)oldProxySetting);
	}

	/**
	 * @return Returns the selectedElement.
	 */
	public nsIDOMElement getSelectedElement() {
		return selectedElement;
	}

	/**
	 * @param selectedElement The selectedElement to set.
	 */
	public void setSelectedElement(nsIDOMElement selectedElement) {
		this.selectedElement = selectedElement;
	}

	/**
	 * @return Returns the selectedXpath.
	 */
	public String getSelectedXpath() {
		return selectedXpath;
	}

	/**
	 * @param selectedXpath The selectedXpath to set.
	 */
	public void setSelectedXpath(String selectedXpath, boolean notifyChange){
		nsIDOMNode[] nodes = evaluteXpath(selectedXpath);
		if(nodes.length==1)createBoxedDiv(nodes[0], notifyChange);
	}
	
	@Override
	public void createBoxedDiv(Object obj, boolean notifyChange) {
		ConvertigoPlugin.logDebug("Create Boxed DIV enter");
		
		nsIDOMNode node = (nsIDOMNode)obj;
			switch(node.getNodeType()){
				case nsIDOMNode.TEXT_NODE: node = node.getParentNode(); break;
				case nsIDOMNode.ATTRIBUTE_NODE:
					nsIDOMAttr attr = (nsIDOMAttr)node.queryInterface(nsIDOMAttr.NS_IDOMATTR_IID);
					node = attr.getOwnerElement(); break;
		}
		
		nsIDOMElement elt = (nsIDOMElement)node.queryInterface(nsIDOMElement.NS_IDOMELEMENT_IID);
		
		if(selectedElement != null && !selectedElement.equals(elt)){
			flasher.repaintElement(selectedElement);
			selectedElement = null;
		}
		if(selectedElement == null) flasher_focus = true;
		selectedElement = elt;
		
		ConvertigoPlugin.logDebug("End Set style on selected Node");
		
		ConvertigoPlugin.logDebug("start calc Xpath");
		selectedXpath   = calcXpath(node, null);
		ConvertigoPlugin.logDebug("end calc Xpath");
		
		if (notifyChange) {
			
			// now notify all our selectionChanged Listeners
			for(SelectionChangedListener scl : selectionChangedListeners) scl.changed();
		}			
	}

	@Override
	public void fireDocumentCompletedListeners() {
		for(DocumentCompletedListener dcl : documentCompletedListeners) dcl.completed();
	}
	
	public void fireHttpProxyEventListeners(HttpProxyEvent event) {
		for(HttpProxyEventListener hpel : httpProxyEventListeners) hpel.modelChanged(event);
	}

	/**
	 * This method initializes toolBar	
	 *
	 */
	private void createToolBar() {
		toolBar = new XulToolBar(composite, SWT.NONE);
		toolBar.setLayoutData(new GridData(SWT.FILL,SWT.DEFAULT,true,false));
	}


	/**
	 * This method initializes XulContainer	
	 *
	 */
	private void createXulContainer() {
		initMozillaSWT(composite);
		
		mozillaBrowser.setFocus();

		// init static objects
		initMozillaStatic();
		
		initMozillaCurrent();
		
		scriptableInputStream = (nsIScriptableInputStream) componentManager.createInstanceByContractID("@mozilla.org/scriptableinputstream;1", null, nsIScriptableInputStream.NS_ISCRIPTABLEINPUTSTREAM_IID);
		
		flasher = (inIFlasher) componentManager.createInstanceByContractID("@mozilla.org/inspector/flasher;1", null, inIFlasher.INIFLASHER_IID);
		flasher.setColor("lime");
		flasher.setThickness(4);
		Thread flasherd = new Thread(new Runnable() {
			public void run() {
				long waitTime = 500;
				while (flasher != null) {
					display.syncExec(new Runnable() {
						public void run() {
							if (selectedElement != null) {
								if (flasher_focus) {
									if (flasher != null) {
										flasher.scrollElementIntoView(selectedElement);
									}
									flasher_focus = false;
								}
								if (flasher != null) {
									flasher.drawElementOutline(selectedElement);
								}
							}
						}
					});
					try {	
						Thread.sleep(waitTime);
					} catch (InterruptedException e) {}
				}
			}
		});
		flasherd.setName("flasherd");
		flasherd.setDaemon(true);
		flasherd.start();
	}

	@Override
	public void completed(ProgressEvent e) {
		super.completed(e);
		ConvertigoPlugin.logDebug("=== Start   fireDocumentCompletedListeners ====");
		
		selectedElement = null;
		// Signal the listeners that the document is now built...
		fireDocumentCompletedListeners();
		
		ConvertigoPlugin.logDebug("=== Stop   fireDocumentCompletedListeners ====");
	}

	/**
	 * will be called for evry click on one element of the web page
	 * @param	event	the event
	 */
	@Override
	public void handleEvent(nsIDOMEvent event) {
		super.handleEvent(event);
		if(event.getType().equalsIgnoreCase("mouseup")){
			setFocus();
			
			nsIDOMMouseEvent mouseEvent = (nsIDOMMouseEvent) event.queryInterface(nsIDOMMouseEvent.NS_IDOMMOUSEEVENT_IID);
			if(mouseEvent.getButton() == 2){

				nsIDOMEventTarget 		target = event.getTarget();
				final nsIDOMElement     elt 	 = (nsIDOMElement)target.queryInterface(nsIDOMElement.NS_IDOMELEMENT_IID);
				
				createBoxedDiv(elt, true);
			}
		}
	}
	
	public void setLayoutData(Object layoutData){
		composite.setLayoutData(layoutData);
	}
	
	public boolean setFocus(){
		return mozillaBrowser.setFocus();
	}

	/**
	 * This will be called by the Mozilla Observer Service to notify any http request 
	 */
	@Override
	protected void onModifyRequest(nsIHttpChannel channel){
		super.onModifyRequest(channel);
		if(httpProxyEventListeners.size()!=0 || ConvertigoPlugin.getLogLevel()>=Log.LOGLEVEL_DEBUG3){
			String key = channel.getURI().getAsciiSpec();
			
			requestString = lastBuffer = new StringBuffer();
			
			// Dump channel info
			ConvertigoPlugin.logDebug3("Observer: ======== Request ===================================");
			ConvertigoPlugin.logDebug3("Observer:           " +channel.getRequestMethod() + "  " + channel.getURI().getAsciiSpec());
			
			requestString.append(channel.getRequestMethod() + ' ' + channel.getURI().getPath() + " HTTP/1.1\r");
			
			// Dump headers
			channel.visitRequestHeaders(this);
						
			// Dump Post data if POST request
			if (channel.getRequestMethod().equalsIgnoreCase("POST")) {
				String postData = visitPostHeaders(channel);
				ConvertigoPlugin.logDebug3("Observer:      data :     " + postData);
				StringTokenizer strtok = new StringTokenizer(postData,"\r\n");
				int count = strtok.countTokens();
				for(int i=0;i<count-1;i++){
					requestString.append(' '+strtok.nextToken()+'\r');
				}
				requestString.append("\n\r"+strtok.nextToken());
			}
			
			ConvertigoPlugin.logDebug3("Observer: ======== Request String ===================================\r"+requestString.toString());
	
			uriToRequest.put(key, requestString.toString());
		}
	}
	
	@Override
	protected void onExamineResponse(nsIHttpChannel channel){
		super.onExamineResponse(channel);
		
		if(httpProxyEventListeners.size()!=0 || ConvertigoPlugin.getLogLevel()>=Log.LOGLEVEL_DEBUG3){
			
			String key = channel.getURI().getAsciiSpec();
			
			responseString = lastBuffer = new StringBuffer();
	
			responseString.append("HTTP/1.x "+channel.getResponseStatus()+' '+channel.getResponseStatusText()+'\r');
			
			channel.visitResponseHeaders(this);
			
			ConvertigoPlugin.logDebug3("Observer: ======== Response String ===================================\r"+responseString.toString());
			
			if(uriToRequest.containsKey(key)){
				fireHttpProxyEventListeners(
						new HttpProxyEvent(
								getConvertigoContext().contextID,								//running context id
								(String)uriToRequest.get(key),			//request
								responseString.toString(),				//response
								"",										//path
								""+channel.getResponseStatus(),			//status
								0,										//elapsedTime
								0,										//requestStarted
								"",										//method
								0,										//size
								channel.getURI().schemeIs("https")		//https ?
						)
				);
				uriToRequest.remove(key);
			}
		}
	}
	
	/**
	 * called by visitRequestHeaders of nsIHttpChannel
	 */
	public void visitHeader(String header, String value) {
		ConvertigoPlugin.logDebug3("Observer:                " + header+"="+value);
		lastBuffer.append(' '+header+": "+value+'\r');
	}

	/**
	 * Returns the data posted in a http stream
	 * @param 	channel the HttpChannel
	 * @return  Data
	 */
	private String visitPostHeaders(nsIHttpChannel channel)
	{
	    // Get the headers from postData stream if present
	    try {
	      // Must change HttpChannel to UploadChannel to be able to access post data
	      nsIUploadChannel uploadChannel = (nsIUploadChannel)channel.queryInterface(nsIUploadChannel.NS_IUPLOADCHANNEL_IID);
	      nsIInputStream uploadStream = uploadChannel.getUploadStream();
	      // Get the post data stream
	      if (uploadStream != null) {
	        try {
	          // Must check if there is headers in the stream
	          nsIMIMEInputStream MIMEInputStream = (nsIMIMEInputStream)uploadStream.queryInterface(nsIMIMEInputStream.NS_IMIMEINPUTSTREAM_IID);
	          
	          // Must change to SeekableStream to be able to rewind the stream
	          nsISeekableStream seekableStream = (nsISeekableStream)MIMEInputStream.queryInterface(nsISeekableStream.NS_ISEEKABLESTREAM_IID);
	          seekableStream.seek(0,0);
	          
	          // Read POST data
	          scriptableInputStream.init(uploadStream);
	          String res = scriptableInputStream.read(scriptableInputStream.available());
	          
	          // Rewind the stream
	          seekableStream.seek(0,0);
	          
	          // Close streams
	          scriptableInputStream.close();
	          uploadStream.close();
	          
	          return res;
	        } catch (Exception e) {
	        	ConvertigoPlugin.logException(e, "Unexpected exception");
	        }
	      }
	    } catch (Exception ee) {
	    	
	    }
	    return null;
	}

	/**
	 * Add a listener for any click on a element in the browser, This routine is responsible for calling
	 * the  webviewer's routine to highlight the selection @see createBoxedDiv
	 * 
	 * @param doc  document where the event handler has to added.
	 */
	@Override
	protected void addDomEvents(nsIDOMDocument doc){
		try {
			super.addDomEvents(doc);
			nsIDOMEventTarget target= (nsIDOMEventTarget)doc.queryInterface(nsIDOMEventTarget.NS_IDOMEVENTTARGET_IID);
			recordDomEvents(target,"mouseup");
		} catch (Exception ex) {
			ConvertigoPlugin.logException(ex, "error");
		}
	}
	
	protected Object[] initGenerate(StatementWithExpressions block, String xpath)throws EngineException{
		return initGenerate(block, xpath, false);
	}
	
	protected Object[] initGenerate(StatementWithExpressions block, String xpath, boolean allowMultiple)throws EngineException{
		nsIDOMNode[] nodes = evaluteXpath(xpath);
		if(nodes.length == 0){
			throw new EngineException("No node evaluated from this XPath : "+xpath);
		}else if(!allowMultiple && nodes.length > 1){
			throw new EngineException("More than one node evaluated from this XPath : "+xpath);
		}
		nsIDOMHTMLElement element = null;
		try{
			element = (nsIDOMHTMLElement)nodes[0].queryInterface(nsIDOMHTMLElement.NS_IDOMHTMLELEMENT_IID);
		}catch (Exception e) {
			throw new EngineException("Node is not a Html Element from this XPath : "+xpath);
		}
		
		Object[] ids = giveIds(element);
		String selectBy = (String)ids[0];
		String selectType = (String)ids[1];
		String tagName = (String)ids[2];
		
		EventStatementGenerator evtGen = tagName.equalsIgnoreCase("FORM")?
				new EventStatementGenerator(block, xpath, selectType):
				new EventStatementGenerator(block, xpath);

		return new Object[]{selectBy, selectType, tagName, element, evtGen};
		/** Sample code :
Object[] init = initGenerate(block, xpath);
String selectBy = (String)init[0];
String selectType = (String)init[1];
String tagName = (String)init[2];
nsIDOMHTMLElement element = (nsIDOMHTMLElement)init[3];
EventStatementGenerator evtGen = (EventStatementGenerator)init[4];
		**/
	}
	
	protected Object[] giveIds(nsIDOMHTMLElement element){
		String selectBy = "name";
		String selectType = element.getAttribute(selectBy);

		if(selectType == null || selectType.equals("")){
			selectBy = "id";
			selectType = element.getAttribute(selectBy);
		}
		
		if(selectType == null || selectType.equals("")){
			selectBy = "tagname";
			selectType = element.getTagName();
		}
		
		String tagName = element.getTagName();
		
		return new Object[]{selectBy, selectType, tagName};
	}
	
	public void generateFormElements(StatementWithExpressions block, String formXPath)throws EngineException{
		Object[] init = initGenerate(block, formXPath);
		nsIDOMHTMLFormElement form;
		try {
			form = (nsIDOMHTMLFormElement)((nsIDOMHTMLElement)init[3]).queryInterface(nsIDOMHTMLFormElement.NS_IDOMHTMLFORMELEMENT_IID);
		} catch (Exception e) {
			throw new EngineException("Node is not a Html Form Element from this XPath : "+formXPath);
		}
		EventStatementGenerator evtGen = ((EventStatementGenerator)init[4]);
		
		List<String> radios_done = new LinkedList<String>();
		
		nsIDOMHTMLCollection champs = form.getElements();
		try{
			for(int i=0;i<champs.getLength();i++){
				nsIDOMHTMLElement element = (nsIDOMHTMLElement)champs.item(i).queryInterface(nsIDOMHTMLElement.NS_IDOMHTMLELEMENT_IID);
				
				Object[] ids = giveIds(element);
				String selectBy = (String)ids[0];
				String selectType = (String)ids[1];
				String tagName = (String)ids[2];
				
				if(tagName.equalsIgnoreCase("INPUT")){
					nsIDOMHTMLInputElement input = (nsIDOMHTMLInputElement) element.queryInterface(nsIDOMHTMLInputElement.NS_IDOMHTMLINPUTELEMENT_IID);

					String type = input.getType();

					if(type.equalsIgnoreCase("text")||type.equalsIgnoreCase("password")){
						evtGen.addInputText(selectBy, selectType, tagName, input.getValue());
					}else if(type.equalsIgnoreCase("radio")){
						String key = selectBy + "=" + selectType;
						if(!radios_done.contains(key)){
							radios_done.add(key);
							generateSetCheckableStatementForRadio(selectBy, selectType, form, evtGen);
						}
					}else if(type.equalsIgnoreCase("checkbox")){
						evtGen.addInputCheckbox(selectBy, selectType, input.getChecked());
					}
				}else if(tagName.equalsIgnoreCase("SELECT")){
					nsIDOMHTMLSelectElement select = (nsIDOMHTMLSelectElement) element.queryInterface(nsIDOMHTMLSelectElement.NS_IDOMHTMLSELECTELEMENT_IID);
					generateSetSelectStatement(selectBy, selectType, select, evtGen);
				}else if(tagName.equalsIgnoreCase("TEXTAREA")){
					nsIDOMHTMLTextAreaElement textArea = (nsIDOMHTMLTextAreaElement) element.queryInterface(nsIDOMHTMLTextAreaElement.NS_IDOMHTMLTEXTAREAELEMENT_IID);
					evtGen.addInputText(selectBy, selectType, tagName, textArea.getValue());
				}else {
					ConvertigoPlugin.logDebug("XulWebViewer generateFormElements unknow tagName : "+tagName);
				}
			}
		}catch (Exception e) {
			throw new EngineException("Some error when generate statement from this FORM XPath : "+formXPath, e);
		}
	}
	
	public void generateMouseStatement(StatementWithExpressions block, String xpath)throws EngineException{
		Object[] init = initGenerate(block, xpath);
		String selectBy = (String)init[0];
		String selectType = (String)init[1];
		String tagName = (String)init[2];
		EventStatementGenerator evtGen = (EventStatementGenerator)init[4];
		
		evtGen.addInputMouse(selectBy, selectType, tagName);
	}
	
	public void generateSetInputStatement(StatementWithExpressions block, String xpath)throws EngineException{	
		Object[] init = initGenerate(block, xpath);
		String selectBy = (String)init[0];
		String selectType = (String)init[1];
		String tagName = (String)init[2];
		String value = "";
		if(tagName.equalsIgnoreCase("textarea")){
			nsIDOMHTMLTextAreaElement element = (nsIDOMHTMLTextAreaElement)((nsIDOMHTMLElement)init[3]).queryInterface(nsIDOMHTMLTextAreaElement.NS_IDOMHTMLTEXTAREAELEMENT_IID);
			value = element.getValue();
		}else{
			nsIDOMHTMLInputElement element = (nsIDOMHTMLInputElement)((nsIDOMHTMLElement)init[3]).queryInterface(nsIDOMHTMLInputElement.NS_IDOMHTMLINPUTELEMENT_IID);
			value = element.getValue();
		}
		
		EventStatementGenerator evtGen = (EventStatementGenerator)init[4];
		
		evtGen.addInputText(selectBy, selectType, tagName, value);
	}
	
	
	protected void generateSetCheckableStatementForRadio(String selectBy, String selectType, nsIDOMHTMLElement element, EventStatementGenerator evtGen){
		String prexpath = element.getTagName().equalsIgnoreCase("FORM")?"":"ancestor::FORM[1]";
		String sufxpath = "//INPUT[@"+selectBy+"=\""+selectType+"\" and @type=\"radio\"]";
		
		nsIDOMNode[] radios = evaluteXpath(prexpath+sufxpath, element);
		if(radios.length==0){
			evtGen.setXpath(null);
			radios = evaluteXpath(sufxpath, element);
		}
		if(radios.length>0){
			String [] values = new String[radios.length];
			int check_index = -1;
			for(int j=0;j<values.length;j++){
				nsIDOMHTMLInputElement radio = (nsIDOMHTMLInputElement) radios[j].queryInterface(nsIDOMHTMLInputElement.NS_IDOMHTMLINPUTELEMENT_IID);
				values[j] = radio.getValue();
				if(radio.getChecked()) check_index = j;
			}
			evtGen.addInputRadio(selectBy, selectType, check_index, values);
		}
	}
	 
	
	public void generateSetCheckableStatement(StatementWithExpressions block, String xpath, boolean radioGroup)throws EngineException{
		Object[] init = initGenerate(block, xpath, radioGroup);
		String selectBy = (String)init[0];
		String selectType = (String)init[1];
		nsIDOMHTMLInputElement element = (nsIDOMHTMLInputElement)((nsIDOMHTMLElement)init[3]).queryInterface(nsIDOMHTMLInputElement.NS_IDOMHTMLINPUTELEMENT_IID);
		EventStatementGenerator evtGen = (EventStatementGenerator)init[4];
				
		if(radioGroup) generateSetCheckableStatementForRadio(selectBy, selectType, element, evtGen);
		else evtGen.addInputCheckbox(selectBy, selectType, element.getChecked());	
	}
	
	protected void generateSetSelectStatement(String selectBy, String selectType, nsIDOMHTMLSelectElement select, EventStatementGenerator evtGen){
		nsIDOMHTMLOptionsCollection options = select.getOptions();
		int nb_options = (int)options.getLength();
		boolean [] checks = new boolean[nb_options];
		String [] values = new String[nb_options];
		String [] contents = new String[nb_options];
		
		for(int j=0;j<nb_options;j++){
			nsIDOMHTMLOptionElement option = (nsIDOMHTMLOptionElement)options.item(j).queryInterface(nsIDOMHTMLOptionElement.NS_IDOMHTMLOPTIONELEMENT_IID);
			nsIDOMNode child = option.getFirstChild();
			contents[j] = (child!=null && child.getNodeType() == nsIDOMNode.TEXT_NODE)? child.getNodeValue():"";
			checks[j] = option.getSelected();
			values[j] = option.getValue();
			if(values[j]==null)values[j] = "";
		}
		evtGen.addSelect(selectBy, selectType, checks, values, contents);
	}
	
	public void generateSetSelectStatement(StatementWithExpressions block, String xpath)throws EngineException{
		Object[] init = initGenerate(block, xpath);
		String selectBy = (String)init[0];
		String selectType = (String)init[1];
		nsIDOMHTMLSelectElement select = (nsIDOMHTMLSelectElement)((nsIDOMHTMLElement)init[3]).queryInterface(nsIDOMHTMLSelectElement.NS_IDOMHTMLSELECTELEMENT_IID);
		EventStatementGenerator evtGen = (EventStatementGenerator)init[4];
		
		generateSetSelectStatement(selectBy, selectType, select, evtGen);
	}
	
	public Browser getBrowser(){
		return mozillaBrowser;
	}
	
	@Override
	public IWebViewer createTab(boolean copyProperties){
		XulWebViewerImpl res = (XulWebViewerImpl) super.createTab(copyProperties);
		if(res!=null){
			res.selectionChangedListeners = selectionChangedListeners;
			res.documentCompletedListeners = documentCompletedListeners;
		}
		return res;
	}
	
	@Override
	public void beforeBrowserDispose(){
		super.beforeBrowserDispose();
		flasher = null;
		selectionChangedListeners = null;
		documentCompletedListeners = null;
		composite.dispose();
		composite = null;
		toolBar = null;
	}
	
	public boolean isAllowAlertBox() {
		return allowAlertBox;
	}

	public void setAllowAlertBox(boolean allowAlertBox) {
		this.allowAlertBox = allowAlertBox;
	}
}