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

package com.twinsoft.convertigo.eclipse.editors.connector.htmlconnector;

import java.io.IOException;
import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TreeItem;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.twinsoft.convertigo.beans.core.StatementWithExpressions;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.connector.HtmlConnectorDesignComposite;
import com.twinsoft.convertigo.eclipse.editors.connector.htmlconnector.TwsDomTree.MenuMaker;
import com.twinsoft.convertigo.eclipse.moz.XulWebViewerImpl;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.TwsCachedXPathAPI;

public class HtmlXpathEvaluatorComposite extends XpathEvaluatorComposite {

	private HtmlConnectorDesignComposite htmlDesign;
	
	public HtmlXpathEvaluatorComposite(Composite parent, int style, HtmlConnectorDesignComposite htmlDesign) {
		super(parent, style);
		this.htmlDesign = htmlDesign;
	}

	public Document getDom() {
		return htmlDesign.getWebViewer().getDom();
	}

	public TwsCachedXPathAPI getXpathApi() {
		return htmlDesign.getXpathApi();
	}

	public void setSelectedXpath(String newXpath) {
		htmlDesign.getWebViewer().setSelectedXpath(newXpath, true);
	}

	public MenuMaker makeStatementGeneratorsMenuMaker(final boolean overwrite){
		return new MenuMaker() {
			public void makeMenu(final TwsDomTree tree, TreeItem treeItem, MouseEvent e, Menu menu) {
				if (e.button == 3 && treeItem!=null){
					Object object = treeItem.getData();
					final TreeItem ti = treeItem;
					if (object instanceof Node) {
						final Node node = (Node)object;
						
						if(overwrite || lastEval!=null) {
							Element element=null;
							if(node instanceof Attr){
								Attr attribut = (Attr)node;
								element = attribut.getOwnerElement();
							} else if (overwrite){
								MenuItem item1 = new MenuItem(menu, SWT.NONE);
								item1.setText("Select attributes in order to generate statement(s) in the selected statement container.");
								return;
							}
							if(node instanceof Element || element!=null){
								if(element==null)element=(Element)node;
							
								if(	element.getTagName().equalsIgnoreCase("A") ||
									element.getTagName().equalsIgnoreCase("INPUT") ||
									element.getTagName().equalsIgnoreCase("TEXTAREA") ||
									element.getTagName().equalsIgnoreCase("SELECT") ||
									element.getTagName().equalsIgnoreCase("FORM")
								){
									Object selected = htmlDesign.getProjectExplorerView().getFirstSelectedDatabaseObject();
									if(selected instanceof StatementWithExpressions){
										final StatementWithExpressions block = (StatementWithExpressions)selected;
										boolean clickable = false, valuable = false, checkable = false, selectable = false, radioable = false, formable = false;
	
										if(element.getTagName().equalsIgnoreCase("A")){
											clickable = true;
										}else if(element.getTagName().equalsIgnoreCase("INPUT")){
											String type = element.getAttribute("type");
											clickable = Arrays.binarySearch( new String[]{"button", "checkbox", "radio", "submit"}, type)>-1; //warning, must be sort
											valuable = Arrays.binarySearch( new String[]{"", "password", "text"}, type)>-1; //warning, must be sort
											checkable = Arrays.binarySearch( new String[]{"checkbox", "radio"}, type)>-1; //warning, must be sort
											radioable = type.equals("radio");
										}else if(element.getTagName().equalsIgnoreCase("TEXTAREA")){
											valuable = true;
										}else if(element.getTagName().equalsIgnoreCase("SELECT")){
											selectable = true;
										}else if(element.getTagName().equalsIgnoreCase("FORM")){
											formable = true;
										}
	
										final String [] etats = new String[]{
												clickable?"Generate mouse statement on this "+element.getTagName():null,
												valuable?"Generate set value statement on this "+element.getTagName():null,
												checkable?"Generate set checked statement on this "+element.getTagName():null,
												selectable?"Generate set selected statement on this "+element.getTagName():null,
												radioable?"Generate set checked statement for this radio group":null,
												formable?"Generate statements from FORM":null
										};
	
										for(int i=0;i<etats.length;i++){
											if(etats[i]!=null){
												final Integer index = new Integer(i);
												MenuItem item2 = new MenuItem(menu, SWT.NONE);
												item2.setText(etats[i]);
												item2.addSelectionListener(new org.eclipse.swt.events.SelectionListener() {
													public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
														TreeItem[] sel = ti.getParent().getSelection();
														String xpath = overwrite ? generateSelectionXpath(sel) : lastEval;
																int id = index.intValue();
																try {
																	switch(id){
																	/*clickable*/	case 0: ((XulWebViewerImpl)htmlDesign.getWebViewer()).generateMouseStatement(block, xpath); break;
																	/*valuable */ 	case 1: ((XulWebViewerImpl)htmlDesign.getWebViewer()).generateSetInputStatement(block, xpath); break;
																	/*checkable*/	case 2: ((XulWebViewerImpl)htmlDesign.getWebViewer()).generateSetCheckableStatement(block, xpath, false); break;
																	/*selectable*/	case 3: ((XulWebViewerImpl)htmlDesign.getWebViewer()).generateSetSelectStatement(block, xpath); break;
																	/*radioable*/	case 4: ((XulWebViewerImpl)htmlDesign.getWebViewer()).generateSetCheckableStatement(block, xpath, true); break;
																	/*formable*/	case 5: ((XulWebViewerImpl)htmlDesign.getWebViewer()).generateFormElements(block, xpath); break;
																	}
																}catch(EngineException e1){
																	ConvertigoPlugin.logException(e1, "Error when generate statement for this xPath : "+xpath);
																	ConvertigoPlugin.warningMessageBox("Error when generate statement for this xPath : "+xpath);
																}catch(Exception e1){
																	ConvertigoPlugin.logException(e1, "Error when generate statement for this xPath : "+xpath);
																}
																try{
																	htmlDesign.getProjectExplorerView().reloadDatabaseObject(block.getParentTransaction());
																} catch (EngineException e1) {
																	ConvertigoPlugin.logException(e1, "Error when reload explorer view tree");
																} catch (IOException e1) {
																	ConvertigoPlugin.logException(e1, "Error when reload explorer view tree");
																}
													}
													public void widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent e) {
													}
												});			        				
											}
										}
									}else{
										MenuItem item1 = new MenuItem(menu, SWT.NONE);
										item1.setText("Select a handler in project explorer to be able to generate statements automatically.");
									}
								}else{
									MenuItem item1 = new MenuItem(menu, SWT.NONE);
									item1.setText("Select a 'A', 'INPUT', 'TEXTAREA', 'SELECT' or 'FORM' element to be able to generate statements automatically.");        		
								}
							}
						}
					}
				}
			}
		};
	}

	protected void buttonSelected(String name) {
		try {
			if (name.equals("screenclass")) {
				htmlDesign.createScreenClassFromSelection();
			} else if (name.equals("criterion")) {
				htmlDesign.createCriteriasFromSelection(getDom());
			} else if (name.equals("extractionrule")) {
				htmlDesign.createExtractionRuleFromSelection(getDom());
			} else if (name.equals("statement")) {
				htmlDesign.createStatementFromSelection();
			}
		} catch (EngineException e) {
			ConvertigoPlugin.logInfo("Engine exception occurs: "+e.getMessage());
		}
	}

	protected boolean isButtonEnabled(String name) {
		boolean enable = true;
		if (name.equals("screenclass")||
			name.equals("criterion")||
			name.equals("extractionrule")) {
			enable = lastEval!=null;
		} else if (name.equals("statement")){
			enable = lastEval!=null && (ConvertigoPlugin.getDefault().getProjectExplorerView().getFirstSelectedDatabaseObject() instanceof StatementWithExpressions);
		}
		
		if (enable && (
		 name.equals("screenclass")||
		 name.equals("criterion")||
		 name.equals("extractionrule")||
		 name.equals("statement"))) {
			Document doc = nodesResult.getDocument();
			if(doc==null)enable = false;
			else{
				Node root = doc.getFirstChild();
				enable = (root.getChildNodes().getLength()!=0)?true:root.getAttributes().getLength()!=0;
			}
		}
		return enable;
	}

	protected String[][][] getButtonsDefinition() {
		String mustEvalMsg = "evaluate the Xpath";
		String [][][] buttonsDef = new String[][][] {
				{	//name					, tooltip													, disable_msg														, image_url																		, other
					{"screenclass"			, "Create new child screen class from current Xpath"		, mustEvalMsg														, "/com/twinsoft/convertigo/eclipse/editors/images/screenclass.png"	, null},
					{"criterion"			, "Create new criterion from current Xpath"					, mustEvalMsg														, "/com/twinsoft/convertigo/eclipse/editors/images/criteria.png"		, null},
					{"extractionrule"		, "Create new extraction rule from current Xpath"			, mustEvalMsg														, "/com/twinsoft/convertigo/eclipse/editors/images/extractionrule.png", null},
					{"statement"			, "Create new statement from current Xpath"					, mustEvalMsg+" and select a statement container in project tree" 	, "/com/twinsoft/convertigo/eclipse/editors/images/statement.png"		, null}
				}
		};
		return buttonsDef;
	}
}
