/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.views.mobile;

import java.beans.BeanInfo;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TreeDragSourceEffect;
import org.eclipse.swt.dnd.TreeDropTargetEffect;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonViewer;
import org.eclipse.ui.navigator.resources.ProjectExplorer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.teamdev.jxbrowser.browser.callback.InjectCssCallback;
import com.teamdev.jxbrowser.browser.callback.StartDownloadCallback;
import com.teamdev.jxbrowser.dom.event.EventType;
import com.teamdev.jxbrowser.js.JsAccessible;
import com.teamdev.jxbrowser.js.JsObject;
import com.teamdev.jxbrowser.navigation.event.FrameLoadFinished;
import com.teamdev.jxbrowser.ui.Rect;
import com.twinsoft.convertigo.beans.connectors.CouchDbConnector;
import com.twinsoft.convertigo.beans.connectors.HttpConnector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.MobileObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.core.Variable;
import com.twinsoft.convertigo.beans.couchdb.DesignDocument;
import com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.ngx.components.IAction;
import com.twinsoft.convertigo.beans.ngx.components.IEventGenerator;
import com.twinsoft.convertigo.beans.ngx.components.MobileSmartSource;
import com.twinsoft.convertigo.beans.ngx.components.MobileSmartSource.Filter;
import com.twinsoft.convertigo.beans.ngx.components.MobileSmartSource.SourceData;
import com.twinsoft.convertigo.beans.ngx.components.MobileSmartSource.SourceModel;
import com.twinsoft.convertigo.beans.ngx.components.MobileSmartSourceType;
import com.twinsoft.convertigo.beans.ngx.components.PageComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIActionStack;
import com.twinsoft.convertigo.beans.ngx.components.UIComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIControlDirective;
import com.twinsoft.convertigo.beans.ngx.components.UICustomAction;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicAction;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicMenu;
import com.twinsoft.convertigo.beans.ngx.components.UIForm;
import com.twinsoft.convertigo.beans.ngx.components.UISharedComponent;
import com.twinsoft.convertigo.beans.ngx.components.dynamic.IonBean;
import com.twinsoft.convertigo.beans.transactions.couchdb.AbstractCouchDbTransaction;
import com.twinsoft.convertigo.beans.transactions.couchdb.AllDocsTransaction;
import com.twinsoft.convertigo.beans.transactions.couchdb.DeleteDatabaseTransaction;
import com.twinsoft.convertigo.beans.transactions.couchdb.DeleteDocumentAttachmentTransaction;
import com.twinsoft.convertigo.beans.transactions.couchdb.DeleteDocumentTransaction;
import com.twinsoft.convertigo.beans.transactions.couchdb.GetDocumentTransaction;
import com.twinsoft.convertigo.beans.transactions.couchdb.GetViewTransaction;
import com.twinsoft.convertigo.beans.transactions.couchdb.PostDocumentTransaction;
import com.twinsoft.convertigo.beans.transactions.couchdb.PostReplicateTransaction;
import com.twinsoft.convertigo.beans.transactions.couchdb.PutDatabaseTransaction;
import com.twinsoft.convertigo.beans.transactions.couchdb.PutDocumentAttachmentTransaction;
import com.twinsoft.convertigo.beans.transactions.couchdb.ResetDatabaseTransaction;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.dnd.MobileSource;
import com.twinsoft.convertigo.eclipse.dnd.MobileSourceTransfer;
import com.twinsoft.convertigo.eclipse.editors.connector.ConnectorEditor;
import com.twinsoft.convertigo.eclipse.editors.connector.ConnectorEditorInput;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowser;
import com.twinsoft.convertigo.eclipse.swt.SwtUtils;
import com.twinsoft.convertigo.eclipse.views.mobile.NgxPickerContentProvider.TVObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.NgxApplicationComponentTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.NgxPageComponentTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.NgxUIComponentTreeObject;
import com.twinsoft.convertigo.engine.ConvertigoError;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.CouchParam;
import com.twinsoft.convertigo.engine.enums.JsonOutput.JsonRoot;
import com.twinsoft.convertigo.engine.enums.SchemaMeta;
import com.twinsoft.convertigo.engine.util.CachedIntrospector;
import com.twinsoft.convertigo.engine.util.EngineListenerHelper;
import com.twinsoft.convertigo.engine.util.FileUtils;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.SchemaUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;

public class NgxPickerComposite extends Composite {	
	
	private ToolItem tiLink, btnAction, btnShared, btnSequence, btnDatabase, btnIteration, btnForm, btnGlobal, btnLocal, btnIcon, btnAsset;
	private CheckboxTreeViewer checkboxTreeViewer;
	private TreeViewer modelTreeViewer;
	private Button b_custom;
	private Control l_source;
	private Text t_custom, t_prefix, t_data, t_suffix;
	private Label message;
	private Filter currentFilter = Filter.Sequence;
	private String currentSource = null;
	private MobileObject currentMC = null;
	private Object firstSelected, lastSelected;
	private List<TVObject> checkedList = new ArrayList<TVObject>();
	private boolean isParentDialog = false;
	private boolean isUpdating = false;

	private static void set(Widget widget, Object o) {
		Class<?> cls = o.getClass();
		if (cls.isAnonymousClass() || cls.isSynthetic()) {
			Class<?>[] ifaces = cls.getInterfaces();
			if (ifaces.length == 1) {
				cls = ifaces[0];
			}
		}
		widget.setData(cls.toString(), o);
	}

	private static void set(Widget widget, Object o, Class<?> cls) {
		widget.setData(cls.toString(), o);
	}

	@SuppressWarnings("unchecked")
	private static <E> E get(Widget widget, Class<E> cls) {
		return (E) widget.getData(cls.toString());
	}
	
	private interface DownloadAction {
		void run(File dir, String url);
	}
	
	private interface CopyAction {
		void run(File dir, File toCopy);
	}
	
	private class IconDrag {
		@JsAccessible
		public void onDragStart(JsObject o) {
			_onDragStart(o);
		}
		
		void _onDragStart(JsObject o) {};
	}
	
	private EngineListenerHelper engineListener = new EngineListenerHelper() {

		private JSONObject computeJsonModel(AbstractCouchDbTransaction acdt, String dataPath, Document document) throws Exception {
			String responseEltName = acdt.getXsdTypePrefix() + acdt.getName() + "Response";
			String xsdTypes = acdt.generateXsdTypes(document, true);
			String xsdDom = acdt.generateXsd(xsdTypes);

			XmlSchemaCollection collection = new XmlSchemaCollection();
			XmlSchema xmlSchema = SchemaUtils.loadSchema(xsdDom, collection);
			SchemaMeta.setCollection(xmlSchema, collection);
			ConvertigoError.updateXmlSchemaObjects(xmlSchema);

			QName responseTypeQName = new QName(xmlSchema.getTargetNamespace(), acdt.getXsdResponseTypeName());
			XmlSchemaComplexType cType = (XmlSchemaComplexType) xmlSchema.getSchemaTypes().getItem(responseTypeQName);
			Transaction.addSchemaResponseObjects(xmlSchema, cType);

			QName responseQName = new QName(xmlSchema.getTargetNamespace(), acdt.getXsdResponseElementName());
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

			return jsonOutput;
		}

		@Override
		public void documentGenerated(Document document) {
			final Element documentElement = document.getDocumentElement();
			if (documentElement != null) {
				String project = documentElement.getAttribute("project");
				String connector = documentElement.getAttribute("connector");
				String transaction = documentElement.getAttribute("transaction");
				if (lastSelected != null && lastSelected instanceof TVObject) {
					TVObject tvObject = (TVObject)lastSelected;
					Object object = tvObject.getObject();
					if (object != null && object instanceof DatabaseObject) {

						Map<String, Object> data = lookupModelData(tvObject);
						DatabaseObject dbo = (DatabaseObject) data.get("databaseObject");
						//Map<String, String> params = GenericUtils.cast(data.get("params"));
						String dataPath = (String) data.get("searchPath");

						// internalView (GetViewTransaction)
						if (CouchDbConnector.internalView.equals(transaction)) {
							if (dbo instanceof DesignDocument) {
								DesignDocument dd = (DesignDocument) dbo;
								CouchDbConnector cc = dd.getConnector();
								if (cc.getName().equals(connector) && cc.getProject().getName().equals(project)) {
									GetViewTransaction gvt = (GetViewTransaction) cc.getTransactionByName(CouchDbConnector.internalView);
									if (gvt != null) {
										try {
											JSONObject jsonOutput = computeJsonModel(gvt, dataPath, document);
											jsonOutput.remove("_c8oMeta");
											jsonOutput.remove("error");
											jsonOutput.remove("reason");
											jsonOutput.remove("attr");

											ConvertigoPlugin.asyncExec(() -> {
												if (modelTreeViewer != null && !modelTreeViewer.getTree().isDisposed()) {
													modelTreeViewer.setInput(jsonOutput);
													initTreeSelection(modelTreeViewer, null);
													updateMessage();
												}
											});

										} catch (Exception e) {
											e.printStackTrace();
										}
									}
								}
							}
							if (dbo instanceof UIDynamicAction) {
								UIDynamicAction uida = (UIDynamicAction) dbo;
								IonBean ionBean = uida.getIonBean();
								if (ionBean != null) {
									try {
										String fsview = ionBean.getProperty("fsview").getValue().toString();
										String qname = fsview.substring(0, fsview.lastIndexOf('.'));
										DesignDocument dd = (DesignDocument) Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(qname);
										Connector cc = dd.getConnector();
										if (cc.getName().equals(connector) && cc.getProject().getName().equals(project)) {
											GetViewTransaction gvt = (GetViewTransaction) cc.getTransactionByName(CouchDbConnector.internalView);
											if (gvt != null) {
												JSONObject jsonObject = new JSONObject(uida.computeJsonModel());

												JSONObject jsonOutput = computeJsonModel(gvt, dataPath, document);
												jsonOutput.remove("_c8oMeta");
												jsonOutput.remove("error");
												jsonOutput.remove("reason");
												jsonOutput.remove("attr");

												if (jsonObject.has("out")) {
													jsonObject.put("out", jsonOutput);
												}

												ConvertigoPlugin.asyncExec(() -> {
													if (modelTreeViewer != null && !modelTreeViewer.getTree().isDisposed()) {
														modelTreeViewer.setInput(jsonObject);
														initTreeSelection(modelTreeViewer, null);
														updateMessage();
													}
												});
											}
										}
									} catch (Exception e) {
										e.printStackTrace();
									}
								}
							}
						}

						// internalDocument (GetDocumentTransaction)
						if (CouchDbConnector.internalDocument.equals(transaction)) {
							if (dbo instanceof UIDynamicAction) {
								UIDynamicAction uida = (UIDynamicAction) dbo;
								IonBean ionBean = uida.getIonBean();
								if (ionBean != null) {
									try {
										String qname = ionBean.getProperty("requestable").getValue().toString();
										CouchDbConnector cc = (CouchDbConnector) Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(qname);
										if (cc.getName().equals(connector) && cc.getProject().getName().equals(project)) {
											GetDocumentTransaction gdt = (GetDocumentTransaction) cc.getTransactionByName(CouchDbConnector.internalDocument);
											if (gdt != null) {
												JSONObject jsonObject = new JSONObject(uida.computeJsonModel());

												JSONObject jsonOutput = computeJsonModel(gdt, dataPath, document);
												jsonOutput.remove("_c8oMeta");
												jsonOutput.remove("error");
												jsonOutput.remove("reason");
												jsonOutput.remove("attr");

												if (jsonObject.has("out")) {
													jsonObject.put("out", jsonOutput);
												}

												ConvertigoPlugin.asyncExec(() -> {
													if (modelTreeViewer != null && !modelTreeViewer.getTree().isDisposed()) {
														modelTreeViewer.setInput(jsonObject);
														initTreeSelection(modelTreeViewer, null);
														updateMessage();
													}
												});
											}
										}
									} catch (Exception e) {
										e.printStackTrace();
									}
								}
							}
						}
					}
				}
			}
			ConvertigoPlugin.asyncExec(() -> {
				setWidgetsEnabled(true);
			});
		}
	};

	public NgxPickerComposite(Composite parent, boolean isParentDialog) {
		super(parent, SWT.NONE);
		this.isParentDialog = isParentDialog;
		makeUI(this);
		updateMessage();
		ConvertigoPlugin.runAtStartup(() -> {
			Engine.theApp.addEngineListener(engineListener);
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
		GridLayout gl = new GridLayout(1, false);
		parent.setLayout(gl);

		StackLayout stackLayout = new StackLayout();

		SelectionListener listener = new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (ToolItem i: btnSequence.getParent().getItems()) {
					if (i != tiLink && i != e.widget) {
						i.setSelection(false);
					}
				}
				ToolItem button = (ToolItem) e.widget;
				button.setSelection(true);
				currentFilter = get(button, Filter.class);
				Composite topControl = get(button, Composite.class);
				if (topControl == null) {
					topControl = get(btnSequence, Composite.class);
				}

				Runnable init = get(topControl, Runnable.class);
				if (init != null && topControl.getChildren().length == 0) {
					topControl.setLayout(new FillLayout());
					init.run();
					topControl.layout(true);
				}

				if (stackLayout.topControl != topControl) {
					stackLayout.topControl = topControl;
					topControl.getParent().layout(true);
				}

				Runnable onSelect = get(button, Runnable.class);
				if (onSelect != null) {
					onSelect.run();
				}
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {

			}
		};

		SelectionListener c_listener = new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean isCustom = b_custom.getSelection();

				boolean doSetCustomText = false;
				if (isParentDialog) {
					MobileSmartSource cs = MobileSmartSource.valueOf(currentSource);
					if (cs != null && cs.getModel() != null && cs.getModel().getCustom().isEmpty()) {
						doSetCustomText = true;
					}
				} else {
					doSetCustomText = true;
				}

				if (doSetCustomText) {
					MobileSmartSource nmss = MobileSmartSource.valueOf(getSmartSourceString());
					if (nmss != null) {
						if (isCustom && !t_custom.getEnabled()) {
							t_custom.setText(nmss.getModel().computeValue());
						}
						if (!isCustom && t_custom.getEnabled()) {
							t_custom.setText("");
						}
					}
				}
				t_custom.setEnabled(isCustom);
				t_prefix.setEnabled(!isCustom);
				t_data.setEnabled(!isCustom);
				t_suffix.setEnabled(!isCustom);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {

			}
		};

		Composite headerComposite = new Composite(parent, SWT.NONE);
		headerComposite.setLayout(SwtUtils.newGridLayout(2, false, 0, 0, 0, 0));
		headerComposite.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 1, 1));

		ToolBar toolbar = new ToolBar(headerComposite, SWT.NONE);

		int btnStyle = SWT.CHECK;
		
		if (!isParentDialog) {
			tiLink = new ToolItem(toolbar, SWT.CHECK);
			new ToolItem(toolbar, SWT.SEPARATOR);
		}
		
		btnSequence = new ToolItem(toolbar, btnStyle);
		try {
			btnSequence.setImage(ConvertigoPlugin.getDefault().getIconFromPath("/com/twinsoft/convertigo/beans/sequences/images/genericsequence_color_16x16.png", BeanInfo.ICON_COLOR_16x16));
		} catch (Exception e) {
			btnSequence.setText("SQ");
		}
		btnSequence.setToolTipText("Show Sequence Sources");
		btnSequence.setSelection(true);
		set(btnSequence, Filter.Sequence);

		btnDatabase = new ToolItem(toolbar, btnStyle);
		try {
			btnDatabase.setImage(ConvertigoPlugin.getDefault().getIconFromPath("/com/twinsoft/convertigo/beans/connectors/images/fullsyncconnector_color_16x16.png", BeanInfo.ICON_COLOR_16x16));
		} catch (Exception e) {
			btnDatabase.setText("FS");
		}
		btnDatabase.setToolTipText("Show FullSync Databases Sources");
		set(btnDatabase, Filter.Database);

		btnAction = new ToolItem(toolbar, btnStyle);
		try {
			btnAction.setImage(ConvertigoPlugin.getDefault().getIconFromPath("/com/twinsoft/convertigo/beans/ngx/components/images/uicustomaction_16x16.png", BeanInfo.ICON_COLOR_16x16));
		} catch (Exception e) {
			btnAction.setText("AC");
		}
		btnAction.setToolTipText("Show Action Sources");
		set(btnAction, Filter.Action);

		btnShared = new ToolItem(toolbar, btnStyle);
		try {
			btnShared.setImage(ConvertigoPlugin.getDefault().getIconFromPath("/com/twinsoft/convertigo/beans/ngx/components/images/uisharedcomponent_16x16.png", BeanInfo.ICON_COLOR_16x16));
		} catch (Exception e) {
			btnShared.setText("SH");
		}
		btnShared.setToolTipText("Show Shared component Sources");
		set(btnShared, Filter.Shared);

		btnIteration = new ToolItem(toolbar, btnStyle);
		try {
			btnIteration.setImage(ConvertigoPlugin.getDefault().getIconFromPath("/com/twinsoft/convertigo/beans/steps/images/iterator_16x16.png", BeanInfo.ICON_COLOR_16x16));
		} catch (Exception e) {
			btnIteration.setText("IT");
		}
		btnIteration.setToolTipText("Show Iterators on current page Sources");
		set(btnIteration, Filter.Iteration);

		btnForm = new ToolItem(toolbar, btnStyle);
		try {
			btnForm.setImage(ConvertigoPlugin.getDefault().getIconFromPath("/com/twinsoft/convertigo/beans/ngx/components/images/uiform_16x16.png", BeanInfo.ICON_COLOR_16x16));
		} catch (Exception e) {
			btnForm.setText("FM");
		}
		btnForm.setToolTipText("Show Forms on current page Sources");
		set(btnForm, Filter.Form);

		btnGlobal = new ToolItem(toolbar, btnStyle);
		try {
			btnGlobal.setImage(ConvertigoPlugin.getDefault().getIconFromPath("/com/twinsoft/convertigo/beans/ngx/components/dynamic/images/setglobalaction_16x16.png", BeanInfo.ICON_COLOR_16x16));
		} catch (Exception e) {
			btnGlobal.setText("GS");
		}
		btnGlobal.setToolTipText("Show Global objects");
		set(btnGlobal, Filter.Global);

		btnLocal = new ToolItem(toolbar, btnStyle);
		try {
			btnLocal.setImage(ConvertigoPlugin.getDefault().getIconFromPath("/com/twinsoft/convertigo/beans/ngx/components/dynamic/images/setlocalaction_16x16.png", BeanInfo.ICON_COLOR_16x16));
		} catch (Exception e) {
			btnLocal.setText("LS");
		}
		btnLocal.setToolTipText("Show Local objects");
		set(btnLocal, Filter.Local);

		btnIcon = new ToolItem(toolbar, btnStyle);
		try {
			btnIcon.setImage(ConvertigoPlugin.getDefault().getIconFromPath("/com/twinsoft/convertigo/beans/ngx/components/dynamic/images/icon_16x16.png", BeanInfo.ICON_COLOR_16x16));
		} catch (Exception e) {
			btnIcon.setText("IC");
		}
		btnIcon.setToolTipText("Show available Icons");
		set(btnIcon, Filter.Icon);
		btnIcon.setData("updateTexts", true);

		btnAsset = new ToolItem(toolbar, btnStyle);
		try {
			btnAsset.setImage(ConvertigoPlugin.getDefault().getIconFromPath("/com/twinsoft/convertigo/beans/ngx/components/dynamic/images/filechooseraction_16x16.png", BeanInfo.ICON_COLOR_16x16));
		} catch (Exception e) {
			btnAsset.setText("AS");
		}
		btnAsset.setToolTipText("Show available Assets");
		set(btnAsset, Filter.Asset);
		btnAsset.setData("updateTexts", true);
		
		for (ToolItem ti: toolbar.getItems()) {
			if (ti != tiLink) {
				ti.addSelectionListener(listener);
			}
			ti.setData("style", "background: unset");
		}
		btnSequence.setSelection(true);

		message = new Label(headerComposite, SWT.NONE);
		message.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Composite stack = new Composite(parent, SWT.NONE);
		stack.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		stack.setLayout(stackLayout);

		SashForm treesSashForm = new SashForm(stack, SWT.NONE);
		set(btnSequence, treesSashForm, Composite.class);
		set(btnAction, treesSashForm, Composite.class);
		set(btnShared, treesSashForm, Composite.class);
		set(btnDatabase, treesSashForm, Composite.class);
		set(btnIteration, treesSashForm, Composite.class);
		set(btnForm, treesSashForm, Composite.class);
		set(btnGlobal, treesSashForm, Composite.class);
		set(btnLocal, treesSashForm, Composite.class);

		stackLayout.topControl = treesSashForm;

		checkboxTreeViewer = new CheckboxTreeViewer(treesSashForm, SWT.BORDER | SWT.SINGLE);
		checkboxTreeViewer.setContentProvider(new NgxPickerContentProvider());
		checkboxTreeViewer.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				Object element = event.getElement();
				if (element instanceof TVObject) {
					if (btnIteration.getSelection() || btnForm.getSelection() || btnGlobal.getSelection() || btnLocal.getSelection()) {
						checkboxTreeViewer.setChecked(element, !event.getChecked());
						return;
					}

					TVObject tvoChecked = (TVObject)element;
					if (event.getChecked())
						checkedList.add(tvoChecked);
					else
						checkedList.remove(tvoChecked);
					updateGrayChecked();
					updateTexts();
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
					if (firstSelected == null) {
						firstSelected = selected;
					}
					checkedList.clear();
					checkedList.add(tvoSelected);
					modelTreeViewer.setInput(null);
					updateModel(tvoSelected);
					updateGrayChecked();
					updateTexts();
				}
			}
		});


		modelTreeViewer = new TreeViewer(treesSashForm, SWT.BORDER);
		modelTreeViewer.setContentProvider(new NgxPickerContentProvider());
		modelTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Object selected = ((IStructuredSelection) event.getSelection()).getFirstElement();
				if (selected instanceof TVObject) {
					updateTexts();
				}
			}

		});

		treesSashForm.setWeights(1, 1);

		Composite sourceComposite = new Composite(parent, SWT.NONE);
		sourceComposite.setLayout(new GridLayout(2, false));
		sourceComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		if (isParentDialog) {
			l_source = new Label(sourceComposite, SWT.NONE);
			((Label) l_source).setText(" SOURCE ");
		} else {
			l_source = new Button(sourceComposite, SWT.NONE);
			((Button) l_source).setText(" SOURCE ");
			l_source.setToolTipText("Drag me on a Ngx UI component in the project tree to bind this source to an UI component property");
		}

		Composite dataComposite = new Composite(sourceComposite, SWT.NONE);
		dataComposite.setLayout(new GridLayout(2, false));
		dataComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label l_prefix = new Label(dataComposite, SWT.NONE);
		l_prefix.setText("Prefix");

		t_prefix = new Text(dataComposite, SWT.BORDER);
		t_prefix.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label l_data = new Label(dataComposite, SWT.NONE);
		l_data.setText("Data");

		t_data = new Text(dataComposite, SWT.BORDER | SWT.READ_ONLY);
		t_data.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label l_suffix = new Label(dataComposite, SWT.NONE);
		l_suffix.setText("Suffix");

		t_suffix = new Text(dataComposite, SWT.BORDER);
		t_suffix.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		b_custom = new Button(dataComposite, SWT.CHECK);
		b_custom.setText("Custom");
		b_custom.addSelectionListener(c_listener);

		t_custom = new Text(dataComposite, SWT.BORDER);
		t_custom.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		t_custom.setEnabled(false);

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

			source = new DragSource(l_source, operations);
			source.setTransfer(dragTransfers);
			source.addDragListener(dragAdapter);
		}
		set(btnSequence, (Runnable) () -> {
			final NgxPickerContentProvider contentProvider = (NgxPickerContentProvider) checkboxTreeViewer.getContentProvider();
			if (contentProvider != null) {
				contentProvider.setFilterBy(currentFilter);
				modelTreeViewer.setInput(null);
				checkboxTreeViewer.getTree().removeAll();
				checkboxTreeViewer.refresh();
				initTreeSelection(checkboxTreeViewer, null);
			}
		});
		set(btnAction, get(btnSequence, Runnable.class));
		set(btnShared, get(btnSequence, Runnable.class));
		set(btnDatabase, get(btnSequence, Runnable.class));
		set(btnIteration, get(btnSequence, Runnable.class));
		set(btnForm, get(btnSequence, Runnable.class));
		set(btnGlobal, get(btnSequence, Runnable.class));
		set(btnLocal, get(btnSequence, Runnable.class));
		
		Composite browserComposite = new Composite(stack, SWT.NONE);
		set(btnIcon, browserComposite);
		set(browserComposite, (Runnable) () -> {
			browserComposite.setLayout(GridLayoutFactory.fillDefaults().margins(0, 0).create());
			if (!isParentDialog) {
				Label dndLabel = new Label(browserComposite, SWT.CENTER | SWT.WRAP);
				dndLabel.setText("↓ Drag icons on project treeview objects to set properties ↓");
				dndLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			}
			C8oBrowser browser = new C8oBrowser(browserComposite, SWT.NONE);
			browser.setLayoutData(new GridData(GridData.FILL_BOTH));
			if (currentMC == null) {
				browser.setText("<h1>Please select a NGX project</h1>");
			}
			browser.getBrowser().set(InjectCssCallback.class, event -> {
				return InjectCssCallback.Response.inject("h1, h3 { display: none }\n"
						+ ".selected { border-style: outset; border-color: red; border-radius: 10px }\n");
			});
			browser.getBrowser().navigation().on(FrameLoadFinished.class, event -> {
				com.teamdev.jxbrowser.dom.Document doc = event.frame().document().get();

				doc.addEventListener(EventType.MOUSE_DOWN, c -> {
					com.teamdev.jxbrowser.dom.Element elt = (com.teamdev.jxbrowser.dom.Element) c.target().get();
					if (!elt.nodeName().equals("use")) {
						List<com.teamdev.jxbrowser.dom.Element> lst = elt.findElementsByCssSelector("use");
						elt = lst.size() == 1 ? lst.get(0) : null;
					}
					if (elt != null) {
						String icon = "'" + elt.attributes().get("href").substring(1) + "'";
						doc.findElementsByCssSelector("svg.selected").forEach(s -> s.attributes().remove("class"));
						((com.teamdev.jxbrowser.dom.Element) elt.parent().get()).attributes().put("class", "selected");
						ConvertigoPlugin.asyncExec(() -> t_data.setText(icon));
					}
				}, false);

				doc.addEventListener(EventType.CLICK, c -> {
					c.preventDefault();
				}, false);

				try {
					MobileSmartSource cs = MobileSmartSource.valueOf(currentSource);
					if (currentFilter == Filter.Icon) {
						String iconName;
						if (cs != null && cs.getFilter() == Filter.Icon) {
							iconName = cs.getModel().getData();
						} else {
							iconName = "'add'";
							ConvertigoPlugin.asyncExec(() -> t_data.setText(iconName));
						}
						String selected = "#" + iconName.replace("'", "");
						for (com.teamdev.jxbrowser.dom.Element elt: doc.findElementsByCssSelector("use")) {
							if (selected.equals(elt.attributes().get("href"))) {
								((com.teamdev.jxbrowser.dom.Element) elt.parent().get()).attributes().put("class", "selected");
								Rect rect = ((com.teamdev.jxbrowser.dom.Element) elt.parent().get()).boundingClientRect();
								event.frame().executeJavaScript("window.scrollTo(0, " + (rect.y() - 40) + ");");
								break;
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (!isParentDialog) {
					JsObject window = event.frame().executeJavaScript("window");
					window.putProperty("java", new IconDrag() {
						@Override
						void _onDragStart(JsObject o) {
							JsObject dt = (JsObject) o.property("dataTransfer").get();
							dt.call("clearData");
							String[] key = {null};
							ConvertigoPlugin.syncExec(() -> {
								String jsonString = getSmartSourceString();
								if (StringUtils.isNotBlank(jsonString)) {
									key[0] = MobileSourceTransfer.getInstance().setMobileSource(new MobileSource(jsonString));
								}
							});
							if (key[0] != null) {
								dt.call("setData", "text/plain", key[0]);
							}
						}
					});
					event.frame().executeJavaScript("Array.from(document.getElementsByTagName('a')).forEach(a => a.addEventListener('dragstart', e => window.java.onDragStart(e)))");
				} else {
					browser.getBrowserView().dragAndDrop().disable();
				}
			});
			set(btnIcon, (Runnable) () -> {
				if (currentMC != null) {
					Project project = currentMC.getProject();
					File page = new File(project.getDirFile(), "_private/ionic/node_modules/ionicons/dist/cheatsheet.html");
					if (page.exists()) {
						String url = page.getAbsoluteFile().toURI().toString();
						if (!url.equals(browser.getData("LastUrl"))) {
							browser.setData("LastUrl", url);
							browser.setUrl(url);
						}
					} else {
						browser.setText("<html><body><h1 style=\"text-align: center\">Application must be built once<br/>"
								+ "to initialize icons preview</h1></body></html>");
					}
				}
			});
		});

		Composite assetComposite = new Composite(stack, SWT.NONE);
		set(btnAsset, assetComposite);
		set(assetComposite, (Runnable) () -> {
			SashForm assetSash = new SashForm(assetComposite, SWT.NONE);
			
			Composite assetLeft = new Composite(assetSash, SWT.NONE);
			assetLeft.setLayout(GridLayoutFactory.fillDefaults().spacing(0, 0).create());
			
			Label dndInfo = new Label(assetLeft, SWT.CENTER | SWT.WRAP);
			dndInfo.setText(isParentDialog ? "↓ Drop files below ↓":
				"↓ Drop files below and drag on project treeview objects to set properties ↓");
			dndInfo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			
			TreeViewer assetTree = new TreeViewer(assetLeft, SWT.VIRTUAL);
			assetTree.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
			set(assetComposite, assetTree);
			
			Composite tb = new Composite(assetLeft, SWT.NONE);
			ConvertigoPlugin.asyncExec(() -> {
				tb.setBackground(assetTree.getTree().getBackground());
			});
			tb.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
			tb.setLayout(GridLayoutFactory.fillDefaults().numColumns(4).equalWidth(true).extendedMargins(5, 5, 0, 5).create());
			
			Callable<File> getFile = () -> {
				String path = t_data.getText();
				if (path.length() > 2) {
					File root = (File) assetTree.getInput();
					path = path.substring(1, path.length() - 1);
					File f = new File(root.getParentFile(), path);
					if (f.exists()) {
						return f;
					}
				}
				return null;
			};
			
			DownloadAction downloadAction = (dir, url) -> {
				File dest = dir == null ? (File) assetTree.getInput() : dir.isFile() ? dir.getParentFile() : dir;
				Engine.execute(() -> {
					try {
						HttpGet get = new HttpGet(url);
						try (CloseableHttpResponse response = Engine.theApp.httpClient4.execute(get)) {
							Header header = response.getLastHeader("Content-Disposition");
							String name = null;
							if (header != null) {
								Matcher m = Pattern.compile("filename=\"(.*?)\"").matcher(header.getValue());
								if (m.find()) {
									name = m.group(1);
								}
							}
							if (name == null) {
								name = get.getURI().getPath().replaceAll(".*/", "");
							}
							if (StringUtils.isNotBlank(name)) {
								try (FileOutputStream fos = new FileOutputStream(new File(dest, name))) {
									response.getEntity().writeTo(fos);
								}
							}
						}
						ConvertigoPlugin.asyncExec(() -> assetTree.refresh());
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				});
			};
			
			CopyAction copyAction = (dir, toCopy) -> {
				if (toCopy.exists()) {
					File dest = dir == null ? (File) assetTree.getInput() : dir.isFile() ? dir.getParentFile() : dir;
					Engine.execute(() -> {
						try {
							FileUtils.copyFileToDirectory(toCopy, dest);
						} catch (IOException e1) {
						}
						ConvertigoPlugin.asyncExec(() -> assetTree.refresh());
					});
				}
			};
			
			Button ti = null;
			try {
				ti = new Button(tb, SWT.PUSH);
				ti.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
				ti.setToolTipText("Refresh file tree");
				ti.addSelectionListener(new SelectionAdapter() {

					@Override
					public void widgetSelected(SelectionEvent e) {
						assetTree.refresh();
					}
					
				});
				ti.setImage(ConvertigoPlugin.getDefault().getStudioIcon("icons/studio/refresh.gif"));
			} catch (IOException e1) {
				ti.setText("Refresh");
			}
			
			try {
				ti = new Button(tb, SWT.PUSH);
				ti.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
				ti.setToolTipText("Add an asset file");
				ti.addSelectionListener(new SelectionAdapter() {

					@Override
					public void widgetSelected(SelectionEvent e) {
						try {
							FileDialog fd = new FileDialog(NgxPickerComposite.this.getShell(), SWT.NONE);
							File dir = getFile.call();
							String path = fd.open();
							if (StringUtils.isBlank(path)) {
								return;
							}
							copyAction.run(dir, new File(path));
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
					
				});
				ti.setImage(ConvertigoPlugin.getDefault().getStudioIcon("icons/studio/project_open.gif"));
			} catch (IOException e1) {
				ti.setText("Add");
			}
			
			try {
				ti = new Button(tb, SWT.PUSH);
				ti.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
				ti.setToolTipText("Download an asset file");
				ti.addSelectionListener(new SelectionAdapter() {

					@Override
					public void widgetSelected(SelectionEvent e) {
						try {
							InputDialog id = new InputDialog(getShell(), "Download asset", "Download an HTTP url as a file asset.", "https://", v -> Pattern.matches("^https?://.*", v) ? null : "You must enter an http URL");
							if (id.open() == InputDialog.OK) {
								String url = id.getValue();
								File dir = getFile.call();
								downloadAction.run(dir, url);
							}
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
					
				});
				ti.setImage(ConvertigoPlugin.getDefault().getBeanIcon(CachedIntrospector.getBeanInfo(HttpConnector.class), BeanInfo.ICON_COLOR_16x16));
			} catch (Exception e1) {
				ti.setText("Add");
			}
			
			try {
				ti = new Button(tb, SWT.PUSH);
				ti.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
				ti.setToolTipText("Explore files");
				ti.addSelectionListener(new SelectionAdapter() {

					@Override
					public void widgetSelected(SelectionEvent e) {
						try {
							IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
							ProjectExplorer pe;
							if ((pe = (ProjectExplorer) activePage.showView("org.eclipse.ui.navigator.ProjectExplorer")) != null) {
								CommonViewer cv = pe.getCommonViewer();
								File file = getFile.call();
								IProject iproject = ConvertigoPlugin.getDefault().getProjectPluginResource(currentMC.getProject().getName());
								
								String path = currentMC.getProject().getDirFile().toPath().relativize(file.toPath()).toString();
								iproject.findMember("DisplayObjects/mobile/assets").refreshLocal(IResource.DEPTH_INFINITE, null);
								IResource ifile = iproject.findMember(path);
								List<Object> res = new LinkedList<>();
								res.add(ifile);
								while (res.get(0) != iproject) {
									res.add(0, ((IResource) res.get(0)).getParent());
								}
								
								Object[] resObj = res.toArray();
								ITreeSelection ts = new TreeSelection(new TreePath(resObj));
								cv.setSelection(ts, false);
								cv.setExpandedElements(resObj);
								Tree tree = cv.getTree();
								tree.setTopItem(tree.getSelection()[0]);
							}
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
					
				});
				ti.setImage(ConvertigoPlugin.getDefault().getStudioIcon("icons/studio/project_explorer.gif"));
			} catch (IOException e1) {
				ti.setText("Explore");
			}
			
			C8oBrowser assetBrowser = new C8oBrowser(assetSash, SWT.NONE);
			assetBrowser.getBrowserView().dragAndDrop().disable();
			assetBrowser.getBrowser().set(StartDownloadCallback.class, (a, b) -> {
				ConvertigoPlugin.asyncExec(() -> {
					assetBrowser.setVisible(false);
				assetSash.layout(true);
				});
				b.cancel();
			});
			
			assetBrowser.getBrowser().navigation().on(FrameLoadFinished.class, event -> {
				ConvertigoPlugin.asyncExec(() -> {
					assetBrowser.setVisible(true);
					assetSash.layout(true);
				});
			});
			
			assetTree.setContentProvider(new ITreeContentProvider() {

				@Override
				public Object[] getElements(Object inputElement) {
					return getChildren(inputElement);
				}

				@Override
				public Object[] getChildren(Object parentElement) {
					File file = (File) parentElement;
					File[] files = file.listFiles();
					if (files != null) {
						Arrays.sort(files, (o1, o2) -> {
							if (o1.isDirectory() && o2.isFile()) {
								return -1;
							}
							if (o1.isFile() && o2.isDirectory()) {
								return 1;
							}
							return o1.getName().compareTo(o2.getName());
						});
					}
					return files;
				}

				@Override
				public Object getParent(Object element) {
					File file = (File) element;
					return file.getParentFile();
				}

				@Override
				public boolean hasChildren(Object element) {
					return getChildren(element) != null;
				}

			});
			
			assetTree.setLabelProvider(new LabelProvider() {

				@Override
				public Image getImage(Object element) {
					File file = (File) element;
					try {
						return ConvertigoPlugin.getDefault().getStudioIcon("icons/studio/" + (file.isDirectory() ? "folder.png" : "new.gif"));
					} catch (IOException e) {
						return super.getImage(element);
					}
				}

				@Override
				public String getText(Object element) {
					File file = (File) element;
					return super.getText(file.getName());
				}

			});
			
			assetTree.addSelectionChangedListener(event -> {
				TreeSelection ts = (TreeSelection) event.getSelection();
				File file = (File) ts.getFirstElement();
				if (file == null) {
					return;
				}
				if (file.isFile()) {
					assetBrowser.setUrl(file.getAbsolutePath());
				} else {
					assetBrowser.setVisible(false);
					assetSash.layout(true);
				}
				File root = ((File) assetTree.getInput()).getParentFile();
				String path = root.toPath().relativize(file.toPath()).toString();
				path = path.replace('\\', '/');
				path = "'" + path + "'";
				t_data.setText(path);
			});
			
			assetTree.addDropSupport(DND.DROP_MOVE | DND.DROP_COPY, new Transfer[] {
					TextTransfer.getInstance(), FileTransfer.getInstance()
			}, new TreeDropTargetEffect(assetTree.getTree()) {

				@Override
				public void drop(DropTargetEvent event) {
					String[] strs = (event.data instanceof String) ? new String[]{(String) event.data} :
						(event.data instanceof String[]) ? GenericUtils.cast(event.data) : new String[0];
					
					for (String str: strs) {
						try {
							File dir = event.item == null ? null : (File) event.item.getData();
							if (dir == null) {
								dir = (File) assetTree.getInput();
							}
							if (Pattern.matches("^https?://.*", str)) {
								downloadAction.run(dir, str);
							} else {
								copyAction.run(dir, new File(str));
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					
					super.drop(event);
				}
				
			});
			
			assetTree.addDragSupport(DND.DROP_MOVE | DND.DROP_COPY, new Transfer[] { MobileSourceTransfer.getInstance() }, new TreeDragSourceEffect(assetTree.getTree()) {

				@Override
				public void dragStart(DragSourceEvent event) {
					try {
						String jsonString = getSmartSourceString();
						if (StringUtils.isNotBlank(jsonString)) {
							MobileSourceTransfer.getInstance().setMobileSource(new MobileSource(jsonString));
						}
					} catch (Exception e) {
					}
				}

			});
			
			assetBrowser.setVisible(false);
			
			if (currentMC == null) {
				tb.setEnabled(false);
			}
			set(btnAsset, (Runnable) () -> {
				if (currentMC == null) {
					assetTree.setInput(null);
					return;
				}
				tb.setEnabled(true);
				Project project = currentMC.getProject();
				File dir = new File(project.getDirFile(), "DisplayObjects/mobile/assets/");
				if (dir.exists() && dir.isDirectory() && !dir.equals(assetTree.getInput())) {
					assetTree.setInput(dir);
					MobileSmartSource cs = MobileSmartSource.valueOf(currentSource);
					if (currentFilter == Filter.Asset && cs != null) {
						String path = cs.getModel().getData();
						if (path.length() > 2) {
							path = path.substring(1, path.length() - 1);
							File f = new File(dir.getParentFile(), path);
							if (f.exists()) {
								TreePath tpath = new TreePath(new File[] {f});
								assetTree.setSelection(new TreeSelection(tpath), true);
							}
						}
					}
				}
			});
		});

		for (ToolItem ti : btnSequence.getParent().getItems()) {
			ti.setData("style", "background: unset");
		}
	}

	private Filter getFilter() {
		return currentFilter;
	}

	public String getSmartSourceString() {
		try {
			Filter filter = getFilter();
			String projectName = currentMC.getProject().getName();

			MobileSmartSource cmss = MobileSmartSource.valueOf(currentSource);
			String input = cmss == null ? "": cmss.getInput();

			String path = getModelPath();
			String searchPath = "root";
			int index = path.indexOf(searchPath);
			if (index != -1) {
				path = path.substring(index + searchPath.length());
			}

			SourceModel model = MobileSmartSource.emptyModel(filter);
			model.setCustom(t_custom.getText());
			model.setPrefix(t_prefix.getText());
			model.setSuffix(t_suffix.getText());
			model.setUseCustom(b_custom.getSelection());
			model.setSourceData(getModelData());
			if (!GenericUtils.contains(filter, Filter.Icon, Filter.Asset)) {
				model.setPath(path);
			}

			JSONObject jsonModel = model.toJson();
			//System.out.println(jsonModel.toString(1));

			MobileSmartSource nmss = new MobileSmartSource(filter, projectName, input, jsonModel);
			//System.out.println(nmss.toJsonString(1));
			return nmss.toJsonString();
		}
		catch (Exception e) {
			return "";
		}
	}

	private void resetViewers() {
		checkboxTreeViewer.setInput(null);
		modelTreeViewer.setInput(null);
		for (ToolItem item: btnSequence.getParent().getItems()) {
			if (item.getSelection() && item != tiLink) {
				item.notifyListeners(SWT.Selection, null);
			}
		}
		currentSource = null;
		firstSelected = null;
		lastSelected = null;
		checkedList.clear();
		t_prefix.setText("");
		t_suffix.setText("");
		t_data.setText("");
		t_custom.setText("");
	}

	private void setWidgetsEnabled(boolean enabled) {
		try {
			for (ToolItem i: btnSequence.getParent().getItems()) {
				if (i != tiLink) {
					i.setEnabled(enabled);
				}
			}
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
			if (ob instanceof TVObject && !((TVObject)ob).getSource().isEmpty()) {
				if (btnIteration.getSelection() || btnForm.getSelection() || btnGlobal.getSelection() || btnLocal.getSelection()) {
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
						TVObject tvo = findModelItem(null, modelPath.replaceAll("\\?\\.", "."));
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
					updateTexts(cs);
				} else {
					if (lastSelected.equals(firstSelected)) {
						updateTexts(cs);
					} else {
						updateTexts();
					}
				}
			} else {
				updateTexts();
			}
		}
	}

	private void updateMessage() {
		updateMessage(null);
	}

	private void updateMessage(String msg) {
		String msgTxt = "      ";
		if (currentMC == null) {
			msgTxt = msgTxt + "Please select any Application sub component";
		} else {
			if (currentMC instanceof PageComponent)
				msgTxt = msgTxt + "Page : "+ currentMC.getName() + (msg != null ? " -> "+msg:"");
			else if (currentMC instanceof UIDynamicMenu)
				msgTxt = msgTxt + "Menu : "+ currentMC.getName() + (msg != null ? " -> "+msg:"");
			else if (currentMC instanceof ApplicationComponent)
				msgTxt = msgTxt + "App : "+ currentMC.getName() + (msg != null ? " -> "+msg:"");
		}
		message.setText(msgTxt);
	}

	//	private List<String> getSourceList() {
	//		TVObject tvoSelected = null;
	//		Object selected = checkboxTreeViewer.getStructuredSelection().getFirstElement();
	//		if (selected != null && selected instanceof TVObject) {
	//			tvoSelected = (TVObject)selected;
	//		}
	//		
	//		List<String> sourceList =  new ArrayList<String>();
	//		List<TVObject> tvoList = GenericUtils.cast(Arrays.asList(checkboxTreeViewer.getCheckedElements()));
	//		for (TVObject tvo : tvoList) {
	//			if (tvo.equals(tvoSelected)) {
	//				sourceList.add(0, tvo.getSource());
	//			}
	//			else {
	//				sourceList.add(tvo.getSource());
	//			}
	//		}
	//		return sourceList;
	//	}

	private List<SourceData> getModelData() {
		List<SourceData> sourceList =  new ArrayList<SourceData>();
		if (Filter.Icon == getFilter()) {
			SourceData sd = Filter.Icon.toSourceData(currentMC.getProject().getName(), t_data.getText());
			if (sd != null) {
				sourceList.add(sd);
			}
		} else if (Filter.Asset == getFilter()) {
			SourceData sd = Filter.Asset.toSourceData(currentMC.getProject().getName(), t_data.getText());
			if (sd != null) {
				sourceList.add(sd);
			}
		} else {
			TVObject tvoSelected = null;
			Object selected = checkboxTreeViewer.getStructuredSelection().getFirstElement();
			if (selected != null && selected instanceof TVObject) {
				tvoSelected = (TVObject) selected;
			}

			List<TVObject> tvoList = GenericUtils.cast(Arrays.asList(checkboxTreeViewer.getCheckedElements()));
			for (TVObject tvo : tvoList) {
				SourceData sd = tvo.getSourceData();
				if (sd != null) {
					if (tvo.equals(tvoSelected)) {
						sourceList.add(0, sd);
					}
					else {
						sourceList.add(sd);
					}
				}
			}
		}
		return sourceList;
	}

	private String getModelPath() {
		String path = "";
		ITreeSelection selection = modelTreeViewer.getStructuredSelection();
		if (selection != null && !selection.isEmpty()) {
			TVObject tvo = (TVObject)selection.getFirstElement();
			path = tvo.getPath();
		}
		return path;
	}

	private void updateTexts() {
		Filter filter = getFilter();

		String path = getModelPath();
		String searchPath = "root";
		int index = path.indexOf(searchPath);
		if (index != -1) {
			path = path.substring(index + searchPath.length());
		}

		SourceModel model = MobileSmartSource.emptyModel(filter);
		model.setSourceData(getModelData());
		model.setPath(path);
		t_data.setText(model.computeValue());
	}

	private void updateTexts(MobileSmartSource cs) {
		if (cs != null) {
			SourceModel sm = cs.getModel();
			if (sm != null) {
				t_prefix.setText(sm.getPrefix());
				t_data.setText(sm.getData());
				t_suffix.setText(sm.getSuffix());
				t_custom.setText(sm.getCustom());
				if (sm.getUseCustom()) {
					b_custom.setSelection(true);
					b_custom.notifyListeners(SWT.Selection, null);
				}
			} else {
				//t_custom.setText(cs.getInput());
				t_custom.setText(cs.getValue(false));
				b_custom.setSelection(false);
				b_custom.notifyListeners(SWT.Selection, null);
			}
		}
	}

	private Map<String, Object> lookupModelData(TVObject tvObject) {
		Map<String, Object> data = new HashMap<String, Object>();
		Map<String, String> params = new HashMap<String, String>();
		DatabaseObject dbo = null;
		String searchPath = "";

		Object object = tvObject.getObject();
		JSONObject infos = tvObject.getInfos();
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
					params.put("include_docs", infos.has("include_docs") ? infos.getString("include_docs"):"false");
					searchPath = tvObject.getName().startsWith("get") ? ".rows.value":"";
				} else if (object instanceof UIControlDirective) {
					dbo = (UIControlDirective)object;
					do {
						UIControlDirective directive = (UIControlDirective)dbo;

						String rootDboName = "";
						if (directive.getPage() != null) {
							rootDboName = directive.getPage().getName();
						} else if (directive.getMenu() != null) {
							rootDboName = directive.getMenu().getName();
						}

						MobileSmartSourceType msst = directive.getSourceSmartType();
						MobileSmartSource mss = msst.getSmartSource();
						if (mss != null) {
							dbo = mss.getDatabaseObject(rootDboName);
							params.putAll(mss.getParameters());
							searchPath = mss.getModelPath().replaceAll("\\?\\.", ".") + searchPath;
						} else {
							dbo = null;
						}
					} while (dbo != null && dbo instanceof UIControlDirective);
				} else if (object instanceof UIForm) {
					dbo = (UIForm)object;
					searchPath = "";
				} else if (object instanceof ApplicationComponent) {
					dbo = (ApplicationComponent)object;
					params.put("json", infos.toString());
					searchPath = "";
				} else if (object instanceof PageComponent) {
					dbo = (PageComponent)object;
					params.put("json", infos.toString());
					searchPath = "";
				} else if (object instanceof UIActionStack) {
					dbo = (UIActionStack)object;
					searchPath = "";
				} else if (object instanceof IAction) {
					if (object instanceof UIDynamicAction) {
						dbo = (UIDynamicAction)object;
						searchPath = "";
					} else if (object instanceof UICustomAction) {
						dbo = (UICustomAction)object;
						searchPath = "";
					}
				} else if (object instanceof IEventGenerator && object instanceof UIComponent) {
					dbo = (UIComponent)object;
					searchPath = "";
				} else if (object instanceof UISharedComponent) {
					dbo = (UISharedComponent)object;
					searchPath = "";
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

	private void cleanJsonModel(Object object) {
		if (object != null) {
			if (object instanceof JSONObject) {
				JSONObject jsonObject = (JSONObject) object;
				jsonObject.remove("text");
				jsonObject.remove("attr");

				JSONArray names = jsonObject.names();
				if (names != null) {
					for (int i = 0; i < names.length(); i++) {
						try {
							cleanJsonModel(jsonObject.get((String) names.get(i)));
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
				}
			}
			else if (object instanceof JSONArray) {
				JSONArray jsonArray = (JSONArray) object;
				for (int i = 0; i < jsonArray.length(); i++) {
					try {
						cleanJsonModel(jsonArray.get(i));
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	private JSONObject getJsonModel(Map<String, Object> data, DatabaseObject databaseObject) throws Exception {
		JSONObject jsonModel = new JSONObject();

		Map<String, String> params;
		DatabaseObject dbo;
		String dataPath;

		if (databaseObject == null) {
			dbo = (DatabaseObject) data.get("databaseObject");
			params = GenericUtils.cast(data.get("params"));
			dataPath = (String) data.get("searchPath");
		} else {
			dbo = databaseObject;
			params = new HashMap<String, String>();
			dataPath = "";
		}

		if (dbo != null) {
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
					searchPath += isDocumentNode || !dataPath.startsWith(".document.")? dataPath : dataPath.replaceFirst("\\.document\\.", ".");

					JSONObject jsonOutput = findJSONObject(jsonObject,searchPath);

					jsonModel = isDocumentNode ? new JSONObject().put("document", jsonOutput) : jsonOutput;
				}
			}
			else if (dbo instanceof DesignDocument) {
				DesignDocument dd = (DesignDocument)dbo;
				Connector connector = dd.getConnector();
				String ddoc = params.get("ddoc");
				String view = params.get("view");
				String viewName = ddoc + "/" + view;
				String includeDocs = params.get("include_docs");

				ConvertigoPlugin.asyncExec(() -> {
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
						((GetViewTransaction)transaction).setQ_include_docs(includeDocs);

						Variable view_reduce = ((GetViewTransaction)transaction).getVariable(CouchParam.prefix + "reduce");
						view_reduce.setValueOrNull(false);

						// execute view transaction
						connectorEditor.getDocument(CouchDbConnector.internalView, false);
					}
				});
			}
			// case of UIForm
			else if (dbo instanceof UIForm) {
				//JSONObject jsonObject = new JSONObject("{\"controls\":{\"['area']\":{\"value\":\"\"}}}");
				JSONObject jsonObject = new JSONObject(((UIForm)dbo).computeJsonModel());

				String searchPath = dataPath;

				JSONObject jsonOutput = findJSONObject(jsonObject,searchPath);

				jsonModel = jsonOutput;
			}
			// case of UIACtionStack
			else if (dbo instanceof UIActionStack) {
				JSONObject jsonObject = new JSONObject(((UIActionStack)dbo).computeJsonModel());

				String searchPath = dataPath;

				JSONObject jsonOutput = findJSONObject(jsonObject,searchPath);

				jsonModel = jsonOutput;
			}
			// case of UIDynamicAction or UICustomAction
			else if (dbo instanceof IAction) {
				JSONObject jsonObject = new JSONObject();

				if (dbo instanceof UIDynamicAction) {
					UIDynamicAction uida = (UIDynamicAction)dbo;
					jsonObject = new JSONObject(uida.computeJsonModel());

					IonBean ionBean = uida.getIonBean();
					if (ionBean != null) {
						String name = ionBean.getName();

						if ("CallSequenceAction".equals(name)) {
							String qname = ionBean.getProperty("requestable").getValue().toString();
							DatabaseObject sequence = Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(qname);
							if (sequence != null) {
								JSONObject targetJsonModel = getJsonModel(data, sequence);
								if (jsonObject.has("out")) {
									jsonObject.put("out", targetJsonModel);
								}
							}
						}
						else if ("CallFullSyncAction".equals(name)) {
							String qname = ionBean.getProperty("requestable").getValue().toString();
							String verb = ionBean.getProperty("verb").getValue().toString();
							Connector connector = (Connector) Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(qname);
							if (connector != null) {
								XmlSchema schema = Engine.theApp.schemaManager.getSchemaForProject(connector.getProject().getName());
								AbstractCouchDbTransaction act = null;

								if ("all".equals(verb))
									act = new AllDocsTransaction();
								else if ("create".equals(verb))
									act = new PutDatabaseTransaction();
								else if ("destroy".equals(verb))
									act = new DeleteDatabaseTransaction();
								else if ("get".equals(verb))
									act = new GetDocumentTransaction();
								else if ("delete".equals(verb))
									act = new DeleteDocumentTransaction();
								else if ("delete_attachment".equals(verb))
									act = new DeleteDocumentAttachmentTransaction();
								else if ("post".equals(verb))
									act = new PostDocumentTransaction();
								else if ("put_attachment".equals(verb))
									act = new PutDocumentAttachmentTransaction();
								else if ("replicate_push".equals(verb))
									act = new PostReplicateTransaction();
								else if ("reset".equals(verb))
									act = new ResetDatabaseTransaction();
								else if ("view".equals(verb))
									act = new GetViewTransaction();

								if (act != null) {
									QName typeQName = act.getComplexTypeAffectation();
									XmlSchemaType xmlSchemaType = schema.getTypeByName(typeQName);
									Document document = XmlSchemaUtils.getDomInstance(xmlSchemaType);

									String jsonString = XMLUtils.XmlToJson(document.getDocumentElement(), true, true);
									JSONObject jsonOutput = new JSONObject(jsonString).getJSONObject("document");
									cleanJsonModel(jsonOutput);
									jsonOutput.remove("_c8oMeta");
									jsonOutput.remove("error");
									jsonOutput.remove("reason");

									if (jsonObject.has("out")) {
										jsonObject.put("out", jsonOutput);
									}
								}
							}
						}
						else if ("FullSyncGetAction".equals(name)) {
							String qname = ionBean.getProperty("requestable").getValue().toString();
							String docid = ionBean.getProperty("_id").getValue().toString();
							Connector connector = (Connector) Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(qname);
							if (connector != null) {
								ConvertigoPlugin.asyncExec(() -> {
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
										Transaction transaction = connector.getTransactionByName(CouchDbConnector.internalDocument);
										Variable var_docid = ((GetDocumentTransaction)transaction).getVariable(CouchParam.docid.param());
										var_docid.setValueOrNull(docid);

										// execute view transaction
										connectorEditor.getDocument(CouchDbConnector.internalDocument, false);
									}
								});
							}
						}
						else if ("FullSyncViewAction".equals(name)) {
							String fsview = ionBean.getProperty("fsview").getValue().toString();
							String includeDocs =  ionBean.getProperty("include_docs").getValue().toString();
							String reduce =  ionBean.getProperty("reduce").getValue().toString();

							String qname = fsview.substring(0, fsview.lastIndexOf('.'));
							DesignDocument dd = (DesignDocument) Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(qname);
							Connector connector = dd.getConnector();

							String viewName = dd.getName() + "/" + fsview.substring(fsview.lastIndexOf('.')+1);

							ConvertigoPlugin.asyncExec(() -> {
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
									((GetViewTransaction)transaction).setQ_include_docs(includeDocs);

									Variable view_reduce = ((GetViewTransaction)transaction).getVariable(CouchParam.prefix + "reduce");
									view_reduce.setValueOrNull(reduce);

									// execute view transaction
									connectorEditor.getDocument(CouchDbConnector.internalView, false);
								}
							});
						} else if (name.startsWith("FullSync")) {
							if (ionBean.getProperty("requestable") != null) {
								String qname = ionBean.getProperty("requestable").getValue().toString();
								DatabaseObject connector = Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(qname);
								if (connector != null) {
									XmlSchema schema = Engine.theApp.schemaManager.getSchemaForProject(connector.getProject().getName());
									AbstractCouchDbTransaction act = null;

									if ("FullSyncDeleteAction".equals(name))
										act = new DeleteDocumentTransaction();
									else if ("FullSyncDeleteAttachmentAction".equals(name))
										act = new DeleteDocumentAttachmentTransaction();
									else if ("FullSyncPostAction".equals(name))
										act = new PostDocumentTransaction();
									else if ("FullSyncPutAttachmentAction".equals(name))
										act = new PutDocumentAttachmentTransaction();

									if (act != null) {
										QName typeQName = act.getComplexTypeAffectation();
										XmlSchemaType xmlSchemaType = schema.getTypeByName(typeQName);
										Document document = XmlSchemaUtils.getDomInstance(xmlSchemaType);

										String jsonString = XMLUtils.XmlToJson(document.getDocumentElement(), true, true);
										JSONObject jsonOutput = new JSONObject(jsonString).getJSONObject("document");
										cleanJsonModel(jsonOutput);
										jsonOutput.remove("_c8oMeta");
										jsonOutput.remove("error");
										jsonOutput.remove("reason");

										if (jsonObject.has("out")) {
											jsonObject.put("out", jsonOutput);
										}
									}
								}
							}
						}
					}
				}
				else if (dbo instanceof UICustomAction) {
					jsonObject = new JSONObject(((UICustomAction)dbo).computeJsonModel());
				}

				String searchPath = dataPath;

				JSONObject jsonOutput = findJSONObject(jsonObject,searchPath);

				jsonModel = jsonOutput;
			}
			else if (dbo instanceof IEventGenerator && dbo instanceof UIComponent) {
				JSONObject jsonObject = new JSONObject(((UIComponent)dbo).computeJsonModel());

				String searchPath = dataPath;

				JSONObject jsonOutput = findJSONObject(jsonObject,searchPath);

				jsonModel = jsonOutput;
			}
			// case of UISharedComponent
			else if (dbo instanceof UISharedComponent) {
				JSONObject jsonObject = new JSONObject(((UISharedComponent)dbo).computeJsonModel());

				String searchPath = dataPath;

				JSONObject jsonOutput = findJSONObject(jsonObject,searchPath);

				jsonModel = jsonOutput;
			}
			// case of ApplicationComponent
			else if (dbo instanceof ApplicationComponent) {
				String json = params.get("json");
				jsonModel = new JSONObject(json);
			}
			// case of PageComponent
			else if (dbo instanceof PageComponent) {
				String json = params.get("json");
				jsonModel = new JSONObject(json);
			}
			// should not happened
			else {
				throw new Exception("DatabaseObject "+ dbo.getClass().getName() +" not supported!");
			}
		}
		return jsonModel;
	}

	private void updateModel(TVObject tvObject) {
		Object object = tvObject.getObject();
		if (object != null) {
			Thread t = new Thread(() -> {
				isUpdating = true;

				ConvertigoPlugin.asyncExec(() -> {
					setWidgetsEnabled(false);
					updateMessage("generating model...");
				});

				try {
					Map<String, Object> data = lookupModelData(tvObject);
					JSONObject jsonModel = getJsonModel(data, null);

					ConvertigoPlugin.asyncExec(() -> {
						modelTreeViewer.setInput(jsonModel);
						initTreeSelection(modelTreeViewer, null);
						setWidgetsEnabled(true);
						updateMessage();
					});
				} catch (Exception e) {
					e.printStackTrace();

					ConvertigoPlugin.asyncExec(() -> {
						setWidgetsEnabled(true);
						updateMessage();
					});
				} finally {
					isUpdating = false;
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
				if (key.isBlank())
					continue;
				if (key.startsWith("[") && key.endsWith("]"))
					continue;
				if (jsonObject.has(key)) {
					Object ob = jsonObject.get(key);
					if (ob instanceof JSONObject) {
						jsonObject = (JSONObject)ob;
					} else if (ob instanceof JSONArray) {
						JSONArray jsonArray = (JSONArray)ob;
						Object o0 = jsonArray.get(0);
						if (o0 instanceof JSONObject) {
							jsonObject = (JSONObject)o0;
						} else {
							jsonObject = new JSONObject();
							break;
						}
					} else {
						jsonObject = new JSONObject();
						break;
					}
				} else {
					break;
				}
			}
			return jsonObject;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private void fillCheckedList(TreeItem parent, List<String> csSourceList) {
		if (csSourceList != null && !csSourceList.isEmpty()) {
			TreeItem[] items = null;

			if (parent == null) {
				items = checkboxTreeViewer.getTree().getItems();
			}
			else {
				items = parent.getItems();

				TVObject tvo = (TVObject) parent.getData();
				String tvoSource = tvo.getSource();
				if (csSourceList.contains(tvoSource)) {
					int index = csSourceList.indexOf(tvoSource);
					if (index == 0)
						checkedList.add(0,tvo);
					else
						checkedList.add(tvo);
				}
			}

			for (int i=0; i<items.length; i++) {
				fillCheckedList(items[i], csSourceList);
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
					String tvoPath = tvo.getPath().replaceAll("\\?\\.", ".");
					if (modelPath.startsWith(tvoPath.replaceFirst("root", ""))) {
						if (modelPath.equals(tvoPath.replaceFirst("root", ""))) {
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
		
		currentMC = null;
		setWidgetsEnabled(true);

		if (selected instanceof NgxApplicationComponentTreeObject || selected instanceof NgxPageComponentTreeObject || selected instanceof NgxUIComponentTreeObject) {
			UIComponent uic = null;
			if (selected instanceof NgxApplicationComponentTreeObject) {
				currentMC = ((NgxApplicationComponentTreeObject) selected).getObject();
			} else if (selected instanceof NgxPageComponentTreeObject) {
				currentMC = ((NgxPageComponentTreeObject) selected).getObject();
			} else if (selected instanceof NgxUIComponentTreeObject) {
				uic = ((NgxUIComponentTreeObject) selected).getObject();
				currentMC = currentMC == null ? uic.getPage() : currentMC;
				currentMC = currentMC == null ? uic.getSharedAction() : currentMC;
				currentMC = currentMC == null ? uic.getSharedComponent() : currentMC;
				
				currentMC = currentMC == null ? uic.getMenu() : currentMC;
				currentMC = currentMC == null ? uic.getPEvent() : currentMC;
				currentMC = currentMC == null ? uic.getApplication() : currentMC;
			}

			if (currentMC == null) {
				resetViewers();
			} else {
				if (!currentMC.equals(checkboxTreeViewer.getInput())) {
					resetViewers();
					checkboxTreeViewer.setInput(currentMC);
					initTreeSelection(checkboxTreeViewer, null);
				}
				if (btnIcon.getSelection()) {
					btnIcon.notifyListeners(SWT.Selection, null);
				}
				if (btnAsset.getSelection()) {
					btnAsset.notifyListeners(SWT.Selection, null);
				}

				MobileSmartSource cs = MobileSmartSource.valueOf(source);
				if (cs != null) {
					NgxPickerContentProvider contentProvider = (NgxPickerContentProvider) checkboxTreeViewer.getContentProvider();
					if (isParentDialog) { // when dbo's property edition
						contentProvider.setSelectedDbo(uic);
					}

					ToolItem buttonToSelect = btnSequence;
					currentSource = source;
					Filter filter = cs.getFilter();
					for (ToolItem i: btnSequence.getParent().getItems()) {
						if (filter == get(i, Filter.class)) {
							buttonToSelect = i;
							if (Boolean.TRUE == i.getData("updateTexts")) {
								updateTexts(cs);
							}
						}
					}
					buttonToSelect.notifyListeners(SWT.Selection, null);
				}

			}
			updateMessage();
			return;
		}
//		if (selected instanceof DatabaseObjectTreeObject) {
//			try {
//				ProjectTreeObject prjt = ((DatabaseObjectTreeObject) selected).getProjectTreeObject();
//				MobileApplication app = prjt.getObject().getMobileApplication();
//				IApplicationComponent iapp = app.getApplicationComponent();
//				if (iapp instanceof ApplicationComponent) {
//					TreeObject dbot = prjt.findTreeObjectByUserObject(((ApplicationComponent) iapp).getRootPage());
//					if (dbot != null) {
//						setCurrentInput(dbot, source);
//						return;
//					}
//				}
//			} catch (Exception e) {
//			}
//		}
		
		resetViewers();
		updateMessage();
	}
	
	public ToolItem getTiLink() {
		return tiLink;
	}
}
