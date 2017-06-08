/*
 * Copyright (c) 2001-2016 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.views.mobile;

import java.beans.BeanInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.connectors.CouchDbConnector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.core.Variable;
import com.twinsoft.convertigo.beans.couchdb.DesignDocument;
import com.twinsoft.convertigo.beans.mobile.components.MobileSmartSource;
import com.twinsoft.convertigo.beans.mobile.components.PageComponent;
import com.twinsoft.convertigo.beans.mobile.components.UIComponent;
import com.twinsoft.convertigo.beans.mobile.components.UIControlDirective;
import com.twinsoft.convertigo.beans.mobile.components.MobileSmartSource.Filter;
import com.twinsoft.convertigo.beans.mobile.components.MobileSmartSourceType;
import com.twinsoft.convertigo.beans.transactions.couchdb.GetViewTransaction;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.dnd.MobileSource;
import com.twinsoft.convertigo.eclipse.dnd.MobileSourceTransfer;
import com.twinsoft.convertigo.eclipse.editors.connector.ConnectorEditor;
import com.twinsoft.convertigo.eclipse.editors.connector.ConnectorEditorInput;
import com.twinsoft.convertigo.eclipse.swt.SwtUtils;
import com.twinsoft.convertigo.eclipse.views.mobile.MobilePickerContentProvider.TVObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.MobileComponentTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.MobilePageComponentTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.MobileUIComponentTreeObject;
import com.twinsoft.convertigo.engine.ConvertigoError;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.CouchParam;
import com.twinsoft.convertigo.engine.enums.JsonOutput.JsonRoot;
import com.twinsoft.convertigo.engine.enums.SchemaMeta;
import com.twinsoft.convertigo.engine.util.EngineListenerHelper;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.SchemaUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;

public class MobilePickerComposite extends Composite {

	Composite content, headerComposite;
	private ToolItem btnSequence, btnDatabase, btnIteration;
	private CheckboxTreeViewer checkboxTreeViewer;
	private TreeViewer modelTreeViewer;
	private Label label;
	private Text text;
	private Label message;
	private String currentSource = null;
	private PageComponent currentPage = null;
	private Object lastSelected;
	private List<TVObject> checkedList = new ArrayList<TVObject>();
	private boolean isParentDialog = false;
	private boolean isUpdating = false;
	
	private EngineListenerHelper engineListener = new EngineListenerHelper() {

		@Override
		public void documentGenerated(Document document) {
			final Element documentElement = document.getDocumentElement();
			if (documentElement != null) {
				String project = documentElement.getAttribute("project");
				String connector = documentElement.getAttribute("connector");
				String transaction = documentElement.getAttribute("transaction");
				if (CouchDbConnector.internalView.equals(transaction)) {
					if (lastSelected !=null && lastSelected instanceof TVObject) {
						TVObject tvObject = (TVObject)lastSelected;
						Object object = tvObject.getObject();
						if (object != null && object instanceof DatabaseObject) {
							
							Map<String, Object> data = lookupModelData(tvObject);
							DatabaseObject dbo = (DatabaseObject) data.get("databaseObject");
							//Map<String, String> params = GenericUtils.cast(data.get("params"));
							String dataPath = (String) data.get("searchPath");
							
							if (dbo instanceof DesignDocument) {
								DesignDocument dd = (DesignDocument)dbo;
								CouchDbConnector cc = dd.getConnector();
								if (cc.getName().equals(connector) && cc.getProject().getName().equals(project)) {
									GetViewTransaction gvt = (GetViewTransaction) cc.getTransactionByName(CouchDbConnector.internalView);
									if (gvt != null) {
										try {
											String responseEltName = gvt.getXsdTypePrefix() + gvt.getName() + "Response";
											String xsdTypes = gvt.generateXsdTypes(document, true);
											String xsdDom = gvt.generateXsd(xsdTypes);
											
											XmlSchemaCollection collection = new XmlSchemaCollection();
											XmlSchema xmlSchema = SchemaUtils.loadSchema(xsdDom, collection);
											SchemaMeta.setCollection(xmlSchema, collection);
											ConvertigoError.updateXmlSchemaObjects(xmlSchema);
											
											QName responseTypeQName = new QName(xmlSchema.getTargetNamespace(), gvt.getXsdResponseTypeName());
											XmlSchemaComplexType cType = (XmlSchemaComplexType) xmlSchema.getSchemaTypes().getItem(responseTypeQName);
											Transaction.addSchemaResponseObjects(xmlSchema, cType);
											
											QName responseQName = new QName(xmlSchema.getTargetNamespace(), gvt.getXsdResponseElementName());
											XmlSchemaElement xse = xmlSchema.getElementByName(responseQName);
											SchemaMeta.setSchema(xse, xmlSchema);
											
											Document doc = XmlSchemaUtils.getDomInstance(xse);
											//System.out.println(XMLUtils.prettyPrintDOM(doc));
											
											String jsonString = XMLUtils.XmlToJson(doc.getDocumentElement(), true, true);
											JSONObject jsonObject = new JSONObject(jsonString);
											//System.out.println(jsonString);
											
											String searchPath = "document."+ responseEltName +".response.couchdb_output";
											searchPath += dataPath;
											JSONObject jsonOutput = findJSONObject(jsonObject, searchPath);
	
											Display.getDefault().asyncExec(new Runnable() {
												public void run() {
													if (modelTreeViewer != null && !modelTreeViewer.getTree().isDisposed()) {
														modelTreeViewer.setInput(jsonOutput);
														initTreeSelection(modelTreeViewer, null);
														updateMessage();
													}
												}
											});
											
										} catch (Exception e) {
											e.printStackTrace();
										}
									}
								}
							}
						}
					}
				}
			}
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					setWidgetsEnabled(true);
				}
			});
		}
	};
	
	public MobilePickerComposite(Composite parent, boolean isParentDialog) {
		super(parent, SWT.NONE);
		this.isParentDialog = isParentDialog;
		makeUI(this);
		updateMessage();
		ConvertigoPlugin.runAtStartup(new Runnable() {
			@Override
			public void run() {
				Engine.theApp.addEngineListener(engineListener);
			}
		});
	}
	
	@Override
	public void dispose() {
		try {
			Engine.theApp.removeEngineListener(engineListener);
		}
		catch (Exception e) {};
		super.dispose();
	}

	private void makeUI(Composite parent) {
		parent.setLayout(new GridLayout(1, false));
		parent.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		
		SelectionListener listener = new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				final MobilePickerContentProvider contentProvider = (MobilePickerContentProvider) checkboxTreeViewer.getContentProvider();
				if (contentProvider != null) {
					btnSequence.setSelection(false);
					btnDatabase.setSelection(false);
					btnIteration.setSelection(false);
					
					ToolItem button = (ToolItem) e.widget;
					button.setSelection(true);
					
					if (btnSequence.getSelection()) {
						contentProvider.setFilterBy(Filter.Sequence);
					} else if (btnDatabase.getSelection()) {
						contentProvider.setFilterBy(Filter.Database);
					} else if (btnIteration.getSelection()) {
						contentProvider.setFilterBy(Filter.Iteration);
					}
					modelTreeViewer.setInput(null);
					checkboxTreeViewer.getTree().removeAll();
					checkboxTreeViewer.refresh();
					initTreeSelection(checkboxTreeViewer, null);
				}
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				
			}
		};
		
		headerComposite = new Composite(parent, SWT.NONE);
		headerComposite.setLayout(SwtUtils.newGridLayout(2, false, 0, 0, 0, 0));
		headerComposite.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 1, 1));
		
		ToolBar toolbar = new ToolBar(headerComposite, SWT.NONE);
		
		int btnStyle = SWT.CHECK;
		Image image = null;
				
		btnSequence = new ToolItem(toolbar, btnStyle);
		try {
			image = ConvertigoPlugin.getDefault().getIconFromPath("/com/twinsoft/convertigo/beans/core/images/sequence_color_16x16.png", BeanInfo.ICON_COLOR_16x16);
		} catch (Exception e) {
			btnSequence.setText("SQ");
		}
		btnSequence.setImage(image);
		btnSequence.setToolTipText("Sequences");
		btnSequence.addSelectionListener(listener);
		btnSequence.setSelection(true);
		
		btnDatabase = new ToolItem(toolbar, btnStyle);
		try {
			image = ConvertigoPlugin.getDefault().getIconFromPath("/com/twinsoft/convertigo/beans/connectors/images/fullsyncconnector_color_16x16.png", BeanInfo.ICON_COLOR_16x16);
		} catch (Exception e) {
			btnDatabase.setText("FS");
		}
		btnDatabase.setImage(image);
		btnDatabase.setToolTipText("FullSync Databases");
		btnDatabase.addSelectionListener(listener);
		
		btnIteration = new ToolItem(toolbar, btnStyle);
		try {
			image = ConvertigoPlugin.getDefault().getIconFromPath("/com/twinsoft/convertigo/beans/steps/images/iterator_16x16.png", BeanInfo.ICON_COLOR_16x16);
		} catch (Exception e) {
			btnIteration.setText("IT");
		}
		btnIteration.setImage(image);
		btnIteration.setToolTipText("Iterations");
		btnIteration.addSelectionListener(listener);
		
		message = new Label(headerComposite, SWT.NONE);
		message.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		SashForm treesSashForm = new SashForm(parent, SWT.NONE);
		treesSashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		checkboxTreeViewer = new CheckboxTreeViewer(treesSashForm, SWT.BORDER | SWT.SINGLE);
		checkboxTreeViewer.setContentProvider(new MobilePickerContentProvider());
		checkboxTreeViewer.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				Object element = event.getElement();
				if (element instanceof TVObject) {
					if (btnIteration.getSelection()) {
						checkboxTreeViewer.setChecked(element, !event.getChecked());
						return;
					}
					
					TVObject tvoChecked = (TVObject)element;
					if (event.getChecked())
						checkedList.add(tvoChecked);
					else
						checkedList.remove(tvoChecked);
					updateGrayChecked();
					updateText();
				}
			}
		});
		checkboxTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Object selected = ((IStructuredSelection) event.getSelection()).getFirstElement();
				if (selected instanceof TVObject && !selected.equals(lastSelected)) {
					TVObject tvoSelected = (TVObject)selected;
					lastSelected = selected;
					
					checkedList.clear();
					checkedList.add(tvoSelected);
					modelTreeViewer.setInput(null);
					updateModel(tvoSelected);
					updateGrayChecked();
					updateText();
				}
			}
		});
	
		
		modelTreeViewer = new TreeViewer(treesSashForm, SWT.BORDER);
		modelTreeViewer.setContentProvider(new MobilePickerContentProvider());
		modelTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Object selected = ((IStructuredSelection) event.getSelection()).getFirstElement();
				if (selected instanceof TVObject) {
					updateText();
				}
			}
			
		});
		
		treesSashForm.setWeights(new int[] {1, 1});

		Composite xpathComposite = new Composite(parent, SWT.NONE);
		xpathComposite.setLayout(new GridLayout(2, false));
		xpathComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		label = new Label(xpathComposite, SWT.NONE);
		//label.setText("Path");
		label.setText("Source");
		
		text = new Text(xpathComposite, SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		// Add DND support
		boolean dragEnabled = !isParentDialog;
		if (dragEnabled) {
		    Transfer[] dragTransfers = new Transfer[] { MobileSourceTransfer.getInstance() };
		    int operations = DND.DROP_COPY | DND.DROP_MOVE;
			
			DragSourceAdapter dragAdapter = new DragSourceAdapter() {
		    	@Override
		    	public void dragStart(DragSourceEvent event) {
					try {
						String jsonString = getSmartSourceString();
						if (jsonString != null && !jsonString.isEmpty()) {
							event.doit = true;
							MobileSourceTransfer.getInstance().setMobileSource(new MobileSource(jsonString));
						}
					} catch (Exception e) {
						ConvertigoPlugin.logException(e, "Cannot drag");
					}
		    	}
			};
			
			DragSource source = null;
	
			source = new DragSource(modelTreeViewer.getTree(), operations);
			source.setTransfer(dragTransfers);
			source.addDragListener(dragAdapter);
			
			source = new DragSource(label, operations);
			source.setTransfer(dragTransfers);
			source.addDragListener(dragAdapter);
		}
	}
	
	public String getSmartSourceString() {
		try {
			Filter filter = null;
			if (btnSequence.getSelection())
				filter = Filter.Sequence;
			else if (btnDatabase.getSelection())
				filter = Filter.Database;
			else if (btnIteration.getSelection())
				filter = Filter.Iteration;
			String projectName = currentPage.getProject().getName();
			String input = text.getText();
			MobileSmartSource cs = new MobileSmartSource(filter, projectName, input);
			String jsonString = cs.toJsonString();
			//System.out.println(jsonString);
			return jsonString;
		}
		catch (Exception e) {
			return "";
		}
	}
	
	private void resetViewers() {
		checkboxTreeViewer.setInput(null);
		modelTreeViewer.setInput(null);
		currentSource = null;
		lastSelected = null;
		checkedList.clear();
		text.setText("");
	}
	
	private void setWidgetsEnabled(boolean enabled) {
		try {
			btnSequence.setEnabled(enabled);
			btnDatabase.setEnabled(enabled);
			btnIteration.setEnabled(enabled);
			checkboxTreeViewer.getTree().setEnabled(enabled);
		} catch (Exception e) {
			
		}
	}

	private void updateGrayChecked() {
		checkboxTreeViewer.setCheckedElements(new Object[]{});
		checkboxTreeViewer.setGrayedElements(new Object[]{});
		for (TVObject tvo: checkedList) {
			checkboxTreeViewer.setParentsGrayed(tvo, true);
		}
		for (Object ob: checkboxTreeViewer.getGrayedElements()) {
			checkboxTreeViewer.setChecked(ob, true);
			if (ob instanceof TVObject && !((TVObject)ob).getSourceData().isEmpty()) {
				if (btnIteration.getSelection()) {
					checkboxTreeViewer.setGrayed(ob, !ob.equals(lastSelected));
				} else {
					checkboxTreeViewer.setGrayed(ob, false);
				}
			}
		}
	}
	
	private void initTreeSelection(TreeViewer treeViewer, Object object) {
		if (treeViewer != null) {
			if (object == null) {
				TreeItem[] treeItems = treeViewer.getTree().getItems();
				if (treeItems.length > 0) {
					object = treeItems[0].getData();
				}
			}
			
			StructuredSelection structuredSelection = null;
			if (object != null) {
				structuredSelection = new StructuredSelection(object);
			}
			
			MobileSmartSource cs = MobileSmartSource.valueOf(currentSource);
			if (treeViewer.equals(checkboxTreeViewer)) {
				checkboxTreeViewer.expandAll();
				if (cs != null) {
					checkedList.clear();
					fillCheckedList(null, cs.getSources());
					if (checkedList.size() > 0) {
						structuredSelection = new StructuredSelection(checkedList.get(0));
					}
				}
				if (checkedList.isEmpty() && structuredSelection != null) {
					checkedList.add((TVObject) structuredSelection.getFirstElement());
				}
				checkboxTreeViewer.setSelection(structuredSelection);
				updateGrayChecked();
			} else if (treeViewer.equals(modelTreeViewer)) {
				modelTreeViewer.refresh();
				if (cs != null) {
					String modelPath = cs.getModelPath();
					if (!modelPath.isEmpty()) {
						modelTreeViewer.expandAll();
						TVObject tvo = findModelItem(null, modelPath);
						if (tvo != null) {
							modelTreeViewer.collapseAll();
							modelTreeViewer.expandToLevel(tvo, 0);
							modelTreeViewer.setSelection(new StructuredSelection(tvo));
						} else {
							modelTreeViewer.collapseAll();
							modelTreeViewer.expandToLevel(1);
						}
					}
				}
			}
			
			if (cs != null) {
				if (treeViewer.equals(checkboxTreeViewer)) {
					checkedList.clear();
					fillCheckedList(null, cs.getSources());
					updateGrayChecked();
				}
				updateText(cs.getInput());
			} else {
				updateText();
			}
		}
	}
	
	private void updateMessage() {
		updateMessage(null);
	}
	
	private void updateMessage(String msg) {
		String msgTxt = "      ";
		if (currentPage == null) {
			msgTxt = msgTxt + "Please select a mobile page";
		} else {
			msgTxt = msgTxt + "Page : "+ currentPage.getName() + (msg != null ? " -> "+msg:"");
		}
		message.setText(msgTxt);
	}
	
	private List<String> getSourceData() {
		TVObject tvoSelected = null;
		Object selected = checkboxTreeViewer.getStructuredSelection().getFirstElement();
		if (selected != null && selected instanceof TVObject) {
			tvoSelected = (TVObject)selected;
		}
		
		List<String> sourceData =  new ArrayList<String>();
		List<TVObject> tvoList = GenericUtils.cast(Arrays.asList(checkboxTreeViewer.getCheckedElements()));
		for (TVObject tvo : tvoList) {
			if (tvo.equals(tvoSelected)) {
				sourceData.add(0, tvo.getSourceData());
			}
			else {
				sourceData.add(tvo.getSourceData());
			}
		}
		return sourceData;
	}
	
	private String getModelPath() {
		String path = "";
		ITreeSelection selection = modelTreeViewer.getStructuredSelection();
		if (selection != null && !selection.isEmpty()) {
			TVObject tvo = (TVObject)selection.getFirstElement();
			path = tvo.getSourcePath();
		}
		return path;
	}
	
	private void updateText() {
		boolean isDirective = btnIteration.getSelection();
		List<String> sourceData = getSourceData();
		int size = sourceData.size();
		
		StringBuffer buf = new StringBuffer();
		if (isDirective && size > 0) {
			buf.append(sourceData.get(0));
		}
		else {
			for (String data : sourceData) {
				if (!data.isEmpty()) {
					buf.append(buf.length() > 0 ? ", ":"").append(data);
				}
			}
		}
		
		String path = getModelPath();
		String searchPath = "root";
		int index = path.indexOf(searchPath);
		if (index != -1) {
			path = path.substring(index + searchPath.length());
		}
		
		String computedText = buf.length() > 0 ? (isDirective ? buf + path : "listen(["+ buf +"])" + path):"";
		text.setText(computedText);
	}
	
	private void updateText(String s) {
		text.setText(s);
	}
	
	private Map<String, Object> lookupModelData(TVObject tvObject) {
		Map<String, Object> data = new HashMap<String, Object>();
		Map<String, String> params = new HashMap<String, String>();
		DatabaseObject dbo = null;
		String searchPath = "";
		
		Object object = tvObject.getObject();
		if (object != null) {
			try {
				if (object instanceof RequestableObject) {
					dbo = (RequestableObject)object;
					searchPath = "";
				} else if (object instanceof DesignDocument) {
					dbo = (DesignDocument)object;
					DesignDocument dd = (DesignDocument)dbo;
					params.put("ddoc", dd.getName());
					params.put("view", tvObject.getParent().getName());
					searchPath = tvObject.getName().equals("get") ? ".rows.value":"";
				} else if (object instanceof UIControlDirective) {
					dbo = (UIControlDirective)object;
					do {
						UIControlDirective directive = (UIControlDirective)dbo;					
						String pageName = directive.getPage().getName();
						MobileSmartSourceType msst = directive.getSourceSmartType();
						MobileSmartSource mss = msst.getSmartSource();
						dbo = mss.getDatabaseObject(pageName);
						params.putAll(mss.getParameters());
						searchPath = mss.getModelPath() + searchPath;
					} while (dbo != null && dbo instanceof UIControlDirective);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		data.put("databaseObject", dbo);
		data.put("params", params);
		data.put("searchPath", searchPath);
		return data;
	}
	
	private void updateModel(TVObject tvObject) {
		Object object = tvObject.getObject();
		if (object != null) {
			Thread t = new Thread(new Runnable() {
				public void run() {
					isUpdating = true;
					
					Display.getDefault().asyncExec(new Runnable() {
						public void run() {
							setWidgetsEnabled(false);
							updateMessage("generating model...");
						}
					});
					
					try {
						Map<String, Object> data = lookupModelData(tvObject);
						DatabaseObject dbo = (DatabaseObject) data.get("databaseObject");
						Map<String, String> params = GenericUtils.cast(data.get("params"));
						String dataPath = (String) data.get("searchPath");
						
						// case of requestable
						if (dbo instanceof RequestableObject) {
							RequestableObject ro = (RequestableObject)dbo;
							
							Project project = ro.getProject();
							String responseEltName = ro.getXsdTypePrefix() + ro.getName() + "Response";
							boolean isDocumentNode = JsonRoot.docNode.equals(project.getJsonRoot()) && dataPath.isEmpty();
							
							XmlSchema schema = Engine.theApp.schemaManager.getSchemaForProject(project.getName());
							XmlSchemaObject xso = SchemaMeta.getXmlSchemaObject(schema, ro);
							if (xso != null) {
								Document document = XmlSchemaUtils.getDomInstance(xso);
								//System.out.println(XMLUtils.prettyPrintDOM(document));
								
								String jsonString = XMLUtils.XmlToJson(document.getDocumentElement(), true, true);
								JSONObject jsonObject = new JSONObject(jsonString);
								
								String searchPath = "document."+ responseEltName +".response";
								searchPath += isDocumentNode ? dataPath : dataPath.replaceFirst("\\.document", "");
								
								JSONObject jsonOutput = findJSONObject(jsonObject,searchPath);
								
								JSONObject jsonResponse = isDocumentNode ? new JSONObject().put("document", jsonOutput) : jsonOutput;
								
								Display.getDefault().asyncExec(new Runnable() {
									public void run() {
										modelTreeViewer.setInput(jsonResponse);
										initTreeSelection(modelTreeViewer, null);
										setWidgetsEnabled(true);
										updateMessage();
									}
								});
							}
						}
						// case of design document
						else if (dbo instanceof DesignDocument) {
							DesignDocument dd = (DesignDocument)dbo;
							Connector connector = dd.getConnector();
							String ddoc = params.get("ddoc");
							String view = params.get("view");
							String viewName = ddoc + "/" + view;
							
							Display.getDefault().asyncExec(new Runnable() {
								public void run() {
									IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

									ConnectorEditor connectorEditor = ConvertigoPlugin.getDefault().getConnectorEditor(connector);
									if (connectorEditor == null) {
										try {
											connectorEditor = (ConnectorEditor) activePage.openEditor(new ConnectorEditorInput(connector),
															"com.twinsoft.convertigo.eclipse.editors.connector.ConnectorEditor");
										} catch (PartInitException e) {
											ConvertigoPlugin.logException(e,
													"Error while loading the connector editor '"
															+ connector.getName() + "'");
										}
									}
									
				    				if (connectorEditor != null) {
				    					// activate connector's editor
				    					activePage.activate(connectorEditor);
				    					
				    					// set transaction's parameters
				    					Transaction transaction = connector.getTransactionByName(CouchDbConnector.internalView);
				    					((GetViewTransaction)transaction).setViewname(viewName);
				    					
				    					Variable view_reduce = ((GetViewTransaction)transaction).getVariable(CouchParam.prefix + "reduce");
				   						view_reduce.setValueOrNull(false);
				    					
				    					// execute view transaction
				    					connectorEditor.getDocument(CouchDbConnector.internalView, false);
				    				}
								}
							});
						}
						// should not happened
						else {
							throw new Exception("DatabaseObject "+ dbo.getClass().getName() +" not supported!");
						}
					} catch (Exception e) {
						e.printStackTrace();
						
						Display.getDefault().asyncExec(new Runnable() {
							public void run() {
								setWidgetsEnabled(true);
								updateMessage();
							}
						});
					} finally {
						isUpdating = false;
					}
				}
			});
			t.start();
		} else {
			modelTreeViewer.setInput(null);
		}
	}
	
	private JSONObject findJSONObject(JSONObject jsonParent, String searchPath) {
		try {
			JSONObject jsonObject = jsonParent;
			String path = searchPath;
			path = path.replaceAll("\\['", ".");
			path = path.replaceAll("'\\]", ".");
			
			String[] keys = path.split("\\.");
			for (String key: keys) {
				if (key.startsWith("[") && key.endsWith("]"))
					continue;
				if (jsonObject.has(key)) {
					Object ob = jsonObject.get(key);
					if (ob instanceof JSONObject) {
						jsonObject = (JSONObject)ob;
					} else if (ob instanceof JSONArray) {
						JSONArray jsonArray = (JSONArray)ob;
						jsonObject = jsonArray.getJSONObject(0);
					} else {
						break;
					}
				} else {
					break;
				}
			}
			return jsonObject;
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private void fillCheckedList(TreeItem parent, List<String> csSourceData) {
		if (csSourceData != null && !csSourceData.isEmpty()) {
			TreeItem[] items = null;
			
			if (parent == null) {
				items = checkboxTreeViewer.getTree().getItems();
			}
			else {
				items = parent.getItems();
				
				TVObject tvo = (TVObject) parent.getData();
				String tvoSourceData = tvo.getSourceData();
				if (csSourceData.contains(tvoSourceData)) {
					int index = csSourceData.indexOf(tvoSourceData);
					if (index == 0)
						checkedList.add(0,tvo);
					else
						checkedList.add(tvo);
				}
			}
			
			for (int i=0; i<items.length; i++) {
				fillCheckedList(items[i], csSourceData);
			}
		}
	}
	
	private TVObject findModelItem(TreeItem parent, String modelPath) {
		if (modelPath != null && !modelPath.isEmpty()) {
			TreeItem[] items = null;
			
			items = parent == null ? modelTreeViewer.getTree().getItems() : parent.getItems();
			for (int i=0; i<items.length; i++) {
				TreeItem treeItem = items[i];
				TVObject tvo = (TVObject) treeItem.getData();
				if (tvo != null) {
					String tvoSourcePath = tvo.getSourcePath();
					if (modelPath.startsWith(tvoSourcePath.replaceFirst("root", ""))) {
						if (modelPath.equals(tvoSourcePath.replaceFirst("root", ""))) {
							return tvo;
						}
						return findModelItem(items[i], modelPath);
					}
				}
			}
		}
		return null;
	}
	
	@Override
	public boolean setFocus() {
		return checkboxTreeViewer.getTree().setFocus();
	}
	
	public void setCurrentInput(Object selected, String source) {
		if (isUpdating) return;
		
		currentPage = null;
		setWidgetsEnabled(true);
		
		if (selected instanceof MobileComponentTreeObject) {
			UIComponent uic = null;
			if (selected instanceof MobilePageComponentTreeObject) {
				currentPage = ((MobilePageComponentTreeObject) selected).getObject();
			} else if (selected instanceof MobileUIComponentTreeObject) {
				uic = ((MobileUIComponentTreeObject) selected).getObject();
				currentPage = uic.getPage();
			}
			
			if (currentPage == null) {
				resetViewers();
			} else {
				if (!currentPage.equals(checkboxTreeViewer.getInput())) {
					resetViewers();
					checkboxTreeViewer.setInput(currentPage);
					initTreeSelection(checkboxTreeViewer, null);
				}
				
				MobileSmartSource cs = MobileSmartSource.valueOf(source);
				if (cs != null) {
					MobilePickerContentProvider contentProvider = (MobilePickerContentProvider) checkboxTreeViewer.getContentProvider();
					if (isParentDialog) { // when dbo's property edition
						contentProvider.setSelectedDbo(uic);
					}
					
					ToolItem buttonToSelect = btnSequence;
					currentSource = source;
					Filter filter = cs.getFilter();
					if (Filter.Sequence.equals(filter)) {
						buttonToSelect = btnSequence;
					}
					if (Filter.Database.equals(filter)) {
						buttonToSelect = btnDatabase;
					}
					if (Filter.Iteration.equals(filter)) {
						buttonToSelect = btnIteration;
					}
					buttonToSelect.notifyListeners(SWT.Selection, null);
				}
				
			}
			updateMessage();
		} else {
			resetViewers();
			updateMessage();
		}
	}
}
