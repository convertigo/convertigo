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

package com.twinsoft.convertigo.eclipse.editors.ngx;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;

import com.teamdev.jxbrowser.browser.Browser;
import com.teamdev.jxbrowser.browser.callback.InjectJsCallback;
import com.teamdev.jxbrowser.browser.callback.InjectJsCallback.Response;
import com.teamdev.jxbrowser.browser.callback.ShowContextMenuCallback;
import com.teamdev.jxbrowser.dom.Document;
import com.teamdev.jxbrowser.dom.Element;
import com.teamdev.jxbrowser.dom.Node;
import com.teamdev.jxbrowser.frame.Frame;
import com.teamdev.jxbrowser.js.JsAccessible;
import com.teamdev.jxbrowser.js.JsObject;
import com.teamdev.jxbrowser.permission.callback.RequestPermissionCallback;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.ngx.components.MobileComponent;
import com.twinsoft.convertigo.beans.ngx.components.PageComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIActionStack;
import com.twinsoft.convertigo.beans.ngx.components.UIComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIControlEvent;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicAction;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicElement;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicInvoke;
import com.twinsoft.convertigo.beans.ngx.components.UISharedComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIUseShared;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.dnd.PaletteSourceTransfer;
import com.twinsoft.convertigo.eclipse.editors.CompositeEvent;
import com.twinsoft.convertigo.eclipse.popup.actions.ClipboardAction;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowser;
import com.twinsoft.convertigo.eclipse.swt.SwtUtils;
import com.twinsoft.convertigo.eclipse.views.mobile.MobileDebugView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.DatabaseObjectFoundException;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.enums.NgxBuilderBuildMode;
import com.twinsoft.convertigo.engine.helpers.BatchOperationHelper;
import com.twinsoft.convertigo.engine.helpers.WalkHelper;
import com.twinsoft.convertigo.engine.mobile.MobileBuilder;
import com.twinsoft.convertigo.engine.mobile.MobileEventListener;
import com.twinsoft.convertigo.engine.util.FileUtils;
import com.twinsoft.convertigo.engine.util.NetworkUtils;
import com.twinsoft.convertigo.engine.util.ProcessUtils;


public final class ApplicationComponentEditor extends EditorPart implements MobileEventListener {
	
	public class ApplicationComponentBrowserImpl {

		@JsAccessible
		public void onDragOver(JsObject o) {
			try {
				double x = (Double) o.property("screenX").get();
				double y = (Double) o.property("screenY").get();
				highlightPoint((int) x, (int) y);
			} catch (Exception e) {
				onDrop(o);
			}
		}

		@JsAccessible
		public void onDrop(JsObject o) {
			try {
				String xmlData = PaletteSourceTransfer.getInstance().getPaletteSource().getXmlData();
				DatabaseObject target = exHighlightMobileComponent;
				DatabaseObject source = (DatabaseObject) ConvertigoPlugin.clipboardManagerDND.read(xmlData).get(0);
				if (source instanceof UIDynamicAction && exHighlightMobileComponent instanceof UIDynamicElement) {
					for (UIComponent uic: ((UIDynamicElement) exHighlightMobileComponent).getUIComponentList()) {
						if (uic instanceof UIControlEvent) {
							target = uic;
							break;
						}
					}
				}
				DatabaseObject fTarget = target;
				c8oBrowser.getDisplay().asyncExec(() -> {
					MobileBuilder mb = null;

					Engine.logStudio.info("---------------------- Drop started ----------------------");
					try {
						IEditorPart editorPart = ApplicationComponentEditor.this;
						if (editorPart != null) {
							IEditorInput input = editorPart.getEditorInput();
							mb = ((ApplicationComponentEditorInput)input).getApplication().getProject().getMobileBuilder();
						}
						if (mb != null) {
							mb.prepareBatchBuild();
						}

						ProjectExplorerView view = ConvertigoPlugin.getDefault().getProjectExplorerView();
						TreeObject treeObject = view.findTreeObjectByUserObject(fTarget);
						BatchOperationHelper.start();
						ClipboardAction.dnd.paste(xmlData, ConvertigoPlugin.getMainShell(), view, treeObject, true);
						BatchOperationHelper.stop();
					} catch (Exception e) {
						Engine.logStudio.debug("Failed to drop: " + e.getMessage());
					} finally {
						PaletteSourceTransfer.getInstance().setPaletteSource(null);
						BatchOperationHelper.cancel();
						Engine.logStudio.info("---------------------- Drop ended   ----------------------");
					}
				});
			} catch (Exception e) {
			}
		}
	};
	
	private ApplicationComponentEditorInput applicationEditorInput;
	
	private ScrolledComposite browserScroll;
	private GridData browserGD;
	private Menu devicesMenu;
	private Composite deviceBar;

	private ToolBar toolbar;
	private ToolItem deviceOsToolItem;
	private Text deviceName;
	private Text deviceWidth;
	private Text deviceHeight;
	
	private C8oBrowser c8oBrowser;
	private Browser browser;
	private String debugUrl;
	private String baseUrl = null;
	private String pagePath = null;
	private Collection<Process> processes = new LinkedList<>();
	private File datasetDir;
	private File devicePref;
	private String dataset = "none";
	
	private JSONArray devicesDefinition;
	private JSONArray devicesDefinitionCustom;
	
	private DeviceOS deviceOS = DeviceOS.android;
	private ZoomFactor zoomFactor = ZoomFactor.z100;
	private boolean headlessBuild = false;
	private NgxBuilderBuildMode buildMode = null;
	private int buildCount = 0;
	private ToolItem buildItem;
	
	//private static Pattern pIsServerRunning = Pattern.compile(".*?server running: (http\\S*).*");
	private static Pattern pIsBrowserOpenable = Pattern.compile(".*?open your browser on (http\\S*).*");
	private static Pattern pRemoveEchap = Pattern.compile("\\x1b\\[\\d+m");
	private static Pattern pPriority = Pattern.compile("class(\\d+)");
	private static Pattern pDatasetFile = Pattern.compile("(.+).json");
	
	private static final Set<Integer> usedPort = new HashSet<>();
	private int portNode;
	private int portReload;
	private int portLogger;
	
	public ApplicationComponentBrowserImpl browserInterface;
	
	private Project project;
	private File ionicDir;
	private File nodeModules;
	private File nodeDir;
	
	public ApplicationComponentEditor() {
		try {
			devicesDefinition = new JSONArray(IOUtils.toString(getClass().getResourceAsStream("devices.json"), "UTF-8"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			devicesDefinition = new JSONArray();
		}
		
		try {
			devicesDefinitionCustom = new JSONArray(FileUtils.readFileToString(new File(Engine.USER_WORKSPACE_PATH, "studio/devices.json"), "UTF-8"));
		} catch (Exception e) {
			devicesDefinitionCustom = new JSONArray();
		}
	}

	private void saveDeviceBar() {
		try {
			if (deviceBar == null || !deviceBar.getParent().isVisible()) {
				return;
			}
			JSONObject device;
			try {
				device = new JSONObject(FileUtils.readFileToString(devicePref, "UTF-8"));
			} catch (Exception e) {
				device = new JSONObject();
			}
			
			device.put("visible", deviceBar.isVisible());
			device.put("name", deviceName.getText().trim());
			device.put("width", NumberUtils.toInt(deviceWidth.getText(), -1));
			device.put("height", NumberUtils.toInt(deviceHeight.getText(), -1));
			device.put("zoom", zoomFactor.percent());
			device.put("os", deviceOS.name());
			device.put("dataset", dataset);
			FileUtils.write(devicePref, device.toString(4), "UTF-8");
		} catch (Exception e) {
			Engine.logStudio.debug("Cannot save device bar", e);
		}
	}
	
	@Override
	public void dispose() {
		if (c8oBrowser != null) {
			c8oBrowser.dispose();
		}
		
		for (Process p: processes) {
			p.destroyForcibly();
			p.destroy();
		}
		
		terminateNode(false);
		synchronized (usedPort) {
			usedPort.remove(portNode);
			usedPort.remove(portReload);
			usedPort.remove(portLogger);
		}
		super.dispose();
	}
	
	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
		
		applicationEditorInput = (ApplicationComponentEditorInput) input;
		ApplicationComponent application = applicationEditorInput.application;
		Project project = application.getProject();

		datasetDir = new File(project.getDirPath(), "dataset");
		datasetDir.mkdirs();
		
		devicePref = new File(Engine.USER_WORKSPACE_PATH, "studio/device-" + project.getName() + ".json");
		
		setPartName(project.getName() + " [A: " + application.getName() + "]");
		terminateNode(false);
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		saveDeviceBar();
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		DeviceOS.init(parent.getDisplay());
		
		Composite editor = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout(2, false);
		gl.marginBottom = gl.marginTop = gl.marginLeft = gl.marginRight
				= gl.marginHeight = gl.marginWidth
				= gl.horizontalSpacing = gl.verticalSpacing = 0;
		
		editor.setLayout(gl);
				
		devicesMenu = new Menu(parent.getShell());
		
		JSONObject device = null;
		try {
			device = new JSONObject(FileUtils.readFileToString(devicePref, "UTF-8"));
			headlessBuild = device.getBoolean("headlessBuild");
		} catch (Exception e) { }
		
		updateDevicesMenu();
		createToolbar(editor);
		createDeviceBar(editor);
		createBrowser(editor);
		
		try {
			dataset = device.getString("dataset");
			deviceName.setText(device.getString("name").trim());
			deviceWidth.setText("" + device.getInt("width"));
			deviceHeight.setText("" + device.getInt("height"));
			zoomFactor = ZoomFactor.get(device.getInt("zoom"));
			setDeviceBarVisible(device.getBoolean("visible"));
			setDeviceOS(DeviceOS.valueOf(device.getString("os")));
			updateBrowserSize();
			
			for (MenuItem m: devicesMenu.getItems()) {
				if (deviceName.getText().equals(m.getText().substring(2))) {
					m.setSelection(true);
					break;
				}
			}
		} catch (Exception e) {
			devicesMenu.getItems()[0].notifyListeners(SWT.Selection, new Event());
		}
		
		if (applicationEditorInput.isAutoBuild()) {
			launchBuilder(false);
		}
		
		getSite().getWorkbenchWindow().getActivePage().activate(this);
	}

	private void createBrowser(Composite parent) {
		browserScroll = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		browserScroll.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
		browserScroll.setExpandHorizontal(true);
		browserScroll.setExpandVertical(true);
		
		Composite canvas = new Composite(browserScroll, SWT.NONE);
		browserScroll.setContent(canvas);
		
		GridLayout gl = new GridLayout(1, false);
		gl.marginBottom = gl.marginTop = gl.marginLeft = gl.marginRight
				= gl.marginHeight = gl.marginWidth
				= gl.horizontalSpacing = gl.verticalSpacing = 0;
		
		canvas.setLayout(gl);
		
		c8oBrowser = new C8oBrowser(canvas, SWT.NONE, applicationEditorInput.application.getProject());
		browserGD = new GridData(SWT.CENTER, SWT.CENTER, true, true);
		c8oBrowser.setLayoutData(browserGD);

		browser = c8oBrowser.getBrowser();
		debugUrl = c8oBrowser.getDebugUrl();
		
		browserInterface = new ApplicationComponentBrowserImpl();
		
		browser.set(InjectJsCallback.class, params -> {
			String url = params.frame().browser().url();
			if (baseUrl != null && url.startsWith(baseUrl)) {
				try {
					Frame frame = params.frame();
					JsObject sessionStorage = frame.executeJavaScript("sessionStorage");
					frame.executeJavaScript(
						"sessionStorage.setItem('_c8ocafsession_storage_mode', 'session');\n"
						+ "navigator.__defineGetter__('userAgent', function(){ return '" + deviceOS.agent() + "'});\n"
						+ IOUtils.toString(getClass().getResourceAsStream("inject.js"), "UTF-8")
					);
					sessionStorage.call("setItem", "_c8ocafsession_storage_mode", "session");
					if (!dataset.equals("none")) {
						String json = FileUtils.readFileToString(new File(datasetDir, dataset + ".json"), "UTF-8");
						sessionStorage.call("setItem", "_c8ocafsession_storage_data", json);
					} else {
						sessionStorage.call("setItem", "_c8ocafsession_storage_data", null);
					}
					JsObject window = frame.executeJavaScript("window");
					window.putProperty("java", browserInterface);
				} catch (Exception e) {
					Engine.logStudio.info("onScriptContextCreate failed for '" + url + "' with baseUrl '" + baseUrl + "': " + e.getMessage());
				}
			}
//			browser.setZoomLevel(zoomFactor.zoomLevel());
			return Response.proceed();
		});
		
		browser.set(ShowContextMenuCallback.class, (params, tell) -> {
			com.teamdev.jxbrowser.ui.Point location = params.location();
			highlightPoint(location.x(), location.y());
			tell.close();
		});
		
		browser.engine().permissions().set(RequestPermissionCallback.class, (params, tell) -> {
			tell.grant();
		});
	}
	
	private void createDeviceBar(Composite parent) {
		deviceBar = new Composite(parent, SWT.NONE);
		GridData gd = new GridData(GridData.FILL, GridData.CENTER, true, false);
		deviceBar.setLayoutData(gd);
		
		gd.exclude = true;
		deviceBar.setVisible(false);
		
		RowLayout layout = new RowLayout();
		layout.center = true;
		layout.spacing = 10;
		deviceBar.setLayout(layout);
		
		FocusListener focusListener = new FocusAdapter() {
			
			@Override
			public void focusLost(FocusEvent e) {
				updateBrowserSize();
			}
			
			@Override
			public void focusGained(FocusEvent e) {
				deviceBar.getDisplay().asyncExec(() -> ((Text) e.widget).selectAll());
			}
			
		};
		
		KeyListener keyListener = new KeyAdapter() {
			
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.keyCode == 13) {
					updateBrowserSize();
				}
			}
			
		};
		
		VerifyListener verifyListener = e -> {
			String oldS = ((Text) e.widget).getText();
			String newS = oldS.substring(0, e.start) + e.text + oldS.substring(e.end);
				if (!newS.isEmpty() && !newS.equals("-")) {
					try {
						Integer.parseInt(newS);
					} catch (Exception ex) {
						e.doit = false;
					}
				}
		};
		
		ToolBar tb = new ToolBar(deviceBar, SWT.NONE);
		deviceOsToolItem = new ToolItem(tb, SWT.DROP_DOWN);
		final Menu mOS = new Menu(tb);
		SelectionListener selectionListener = new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				setDeviceOS((DeviceOS) e.widget.getData());
			}
			
		};
		
		for (DeviceOS device: DeviceOS.values()) {
			MenuItem menuItem = new MenuItem(mOS, SWT.NONE);
			menuItem.setData(device);
			menuItem.setImage(device.image());
			menuItem.setText(device.displayName());
			menuItem.addSelectionListener(selectionListener);
		}
		
		deviceOsToolItem.setToolTipText("Select device OS.");
		deviceOsToolItem.setImage(deviceOS.image());
		deviceOsToolItem.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				ToolItem item = (ToolItem) e.widget;
				Rectangle rect = item.getBounds(); 
				Point pt = item.getParent().toDisplay(new Point(rect.x, rect.y));
				mOS.setLocation(pt);
				mOS.setVisible(true);
			}
			
		});
		
		new Label(deviceBar, SWT.NONE).setText(" ");
		
		new Label(deviceBar, SWT.NONE).setText("Device name:");
		deviceName = new Text(deviceBar, SWT.NONE);
		deviceName.setFont(JFaceResources.getTextFont());
		deviceName.setLayoutData(new RowData(200, SWT.DEFAULT));
		
		new Label(deviceBar, SWT.NONE).setText(" ");
		
		new Label(deviceBar, SWT.NONE).setText("Width:");
		deviceWidth = new Text(deviceBar, SWT.NONE);
		
		new Label(deviceBar, SWT.NONE).setText(" ");
		
		new Label(deviceBar, SWT.NONE).setText("Height:");
		deviceHeight = new Text(deviceBar, SWT.NONE);
		
		for (Text t: new Text[]{deviceWidth, deviceHeight}) {
			t.setTextLimit(4);
			t.setFont(JFaceResources.getTextFont());
			t.setLayoutData(new RowData(35, SWT.DEFAULT));
			t.addFocusListener(focusListener);
			t.addVerifyListener(verifyListener);
			t.addKeyListener(keyListener);
		}
		
		new Label(deviceBar, SWT.NONE).setText(" ");
		
		tb = new ToolBar(deviceBar, SWT.NONE);
		ToolItem button = new ToolItem(tb, SWT.PUSH);
		button.setImage(new Image(parent.getDisplay(), getClass().getResourceAsStream("/studio/zoom_out.png")));
		button.setToolTipText("Zoom out");
		button.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				zoomFactor = zoomFactor.out();
				updateBrowserSize();
			}
		});
		
		button = new ToolItem(tb, SWT.PUSH);
		button.setImage(new Image(parent.getDisplay(), getClass().getResourceAsStream("/studio/zoom_reset.png")));
		button.setToolTipText("Zoom reset");
		button.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				zoomFactor = ZoomFactor.z100;
				updateBrowserSize();
			}
		});
		
		button = new ToolItem(tb, SWT.PUSH);
		button.setImage(new Image(parent.getDisplay(), getClass().getResourceAsStream("/studio/zoom_in.png")));
		button.setToolTipText("Zoom in");
		button.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				zoomFactor = zoomFactor.in();
				updateBrowserSize();
			}
		});
		
		new ToolItem(tb, SWT.SEPARATOR);
		
		button = new ToolItem(tb, SWT.PUSH);
		button.setImage(new Image(parent.getDisplay(), getClass().getResourceAsStream("/studio/dbo_save.gif")));
		button.setToolTipText("Save");
		button.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				String name = deviceName.getText().trim();
				
				if (name.isEmpty()) {
					toast("Device name must no be empty.");
					return;
				}
				
				if (findDevice(devicesDefinition, name) != null) {
					toast("Cannot override the default device '" + name + "'.");
					return;
				}
				
				int width = NumberUtils.toInt(deviceWidth.getText(), -1);
				int height = NumberUtils.toInt(deviceHeight.getText(), -1);
				
				C8oBrowser.run(() -> {
					JSONObject device = findDevice(devicesDefinitionCustom, name);
					
					try {
						if (device == null) {
							device = new JSONObject();
							device.put("name", name);
							devicesDefinitionCustom.put(device);
						}
						device.put("width", width);
						device.put("height", height);
						device.put("zoom", zoomFactor.percent());
						device.put("os", deviceOS.name());
						FileUtils.write(new File(Engine.USER_WORKSPACE_PATH, "studio/devices.json"), devicesDefinitionCustom.toString(4), "UTF-8");
						toast("Device '" + name + "' saved !");
						parent.getDisplay().asyncExec(() -> updateDevicesMenu());
					} catch (Exception ex) {
						toast("Device '" + name + "' NOT saved ! " + ex.getMessage());
					}
				});
			}
		});
		
		button = new ToolItem(tb, SWT.PUSH);
		button.setImage(new Image(parent.getDisplay(), getClass().getResourceAsStream("/studio/project_delete.gif")));
		button.setToolTipText("Delete");
		button.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				String name = deviceName.getText().trim();
				
				if (findDevice(devicesDefinition, name) != null) {
					toast("Cannot remove the default device '" + name + "' !");
					return;
				}
				
				JSONObject device = findDevice(devicesDefinitionCustom, name);
				
				try {
					if (device != null) {
						devicesDefinitionCustom.remove(device);
						FileUtils.write(new File(Engine.USER_WORKSPACE_PATH, "studio/devices.json"), devicesDefinitionCustom.toString(4), "UTF-8");
						toast("Device '" + deviceName.getText() + "' removed !");
						updateDevicesMenu();
					} else {
						toast("Device '" + deviceName.getText() + "' not found !");
					}
				} catch (Exception ex) {
					toast("Device '" + deviceName.getText() + "' NOT removed ! " + ex);
				}
			}			
		});
	}

	private void createToolbar(Composite parent) {		
		toolbar = new ToolBar(parent, SWT.VERTICAL);
		GridData gd = new GridData(GridData.FILL, GridData.FILL, false, true);
		gd.verticalSpan = 2;
		gd.verticalIndent = 4;
		toolbar.setLayoutData(gd);

		ToolItem item = new ToolItem(toolbar, SWT.DROP_DOWN);
		item.setToolTipText("Select device viewport. Click to toggle the custom device bar.");
		item.setImage(new Image(parent.getDisplay(), getClass().getResourceAsStream("/com/twinsoft/convertigo/beans/core/images/mobiledevice_color_16x16.png")));
		item.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (e.detail == SWT.ARROW) {
					ToolItem item = (ToolItem) e.widget;
					Rectangle rect = item.getBounds(); 
					Point pt = item.getParent().toDisplay(new Point(rect.x + 8, rect.y + 8));
					devicesMenu.setLocation(pt);
					devicesMenu.setVisible(true);
				} else {
					setDeviceBarVisible(!deviceBar.getVisible());
				}
			}
			
		});
		
		new ToolItem(toolbar, SWT.SEPARATOR);
		
		item = new ToolItem(toolbar, SWT.PUSH);
		item.setToolTipText("Change orientation");
		item.setImage(new Image(parent.getDisplay(), getClass().getResourceAsStream("/com/twinsoft/convertigo/beans/connectors/images/fullsyncconnector_color_16x16.png")));
		item.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				String width = deviceWidth.getText();
				deviceWidth.setText(deviceHeight.getText());
				deviceHeight.setText(width);
				updateBrowserSize();
			}
			
		});
		
		new ToolItem(toolbar, SWT.SEPARATOR);
		
		item = new ToolItem(toolbar, SWT.PUSH);
		item.setToolTipText("Refresh");
		item.setImage(new Image(parent.getDisplay(), getClass().getResourceAsStream("/studio/refresh.gif")));
		item.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				doReload();
			}
			
		});
				
		item = new ToolItem(toolbar, SWT.PUSH);
		item.setToolTipText("Back");
		item.setImage(new Image(parent.getDisplay(), getClass().getResourceAsStream("/studio/undo.gif")));
		item.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				C8oBrowser.run(() -> {
					int index = c8oBrowser.getCurrentNavigationEntryIndex();
					if (index > 2) {
						c8oBrowser.goBack();
					}
				});
			}
			
		});
		
		new ToolItem(toolbar, SWT.SEPARATOR);
				
		item = new ToolItem(toolbar, SWT.PUSH);
		item.setToolTipText("Remove highlight");
		item.setImage(new Image(parent.getDisplay(), getClass().getResourceAsStream("/studio/write_wait_zone.d.gif")));
		item.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				exHighlightElement = null;
				exHighlightMobileComponent = null;
				C8oBrowser.run(() -> c8oBrowser.executeJavaScriptAndReturnValue("_c8o_remove_all_overlay()"));
			}
			
		});
		
		new ToolItem(toolbar, SWT.SEPARATOR);
				
		item = new ToolItem(toolbar, SWT.PUSH);
		item.setToolTipText("Show debug");
		item.setImage(new Image(parent.getDisplay(), getClass().getResourceAsStream("/studio/debug.gif")));
		item.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				MobileDebugView view = ConvertigoPlugin.getDefault().getMobileDebugView(true);
				getSite().getPage().activate(view);
				view.onActivated(ApplicationComponentEditor.this);
			}
			
		});
		
		item = new ToolItem(toolbar, SWT.PUSH);
		item.setToolTipText("Open in default browser");
		item.setImage(new Image(parent.getDisplay(), getClass().getResourceAsStream("/com/twinsoft/convertigo/beans/statements/images/ContinueWithSiteClipperStatement_color_16x16.png")));
		item.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				C8oBrowser.run(() -> {
					String url = headlessBuild ? getPageUrl() : c8oBrowser.getURL();
					if (url.startsWith("http")) {
						org.eclipse.swt.program.Program.launch(url);
					}
				});
			}
			
		});
		
		item = new ToolItem(toolbar, SWT.CHECK);
		item.setToolTipText("Headless build");
		item.setImage(new Image(parent.getDisplay(), getClass().getResourceAsStream("/com/twinsoft/convertigo/beans/statements/images/invokebrowserjs_16x16.png")));
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				headlessBuild = ((ToolItem) e.widget).getSelection();
				try {
					JSONObject device = new JSONObject(FileUtils.readFileToString(devicePref, "UTF-8"));
					device.put("headlessBuild", headlessBuild);
					FileUtils.write(devicePref, device.toString(4), "UTF-8");
				} catch (Exception ex) {
					// TODO: handle exception
				}
				launchBuilder(false);
			}
		});
		item.setSelection(headlessBuild);
		
		final Menu mBuild = new Menu(toolbar);

		for (NgxBuilderBuildMode mode: NgxBuilderBuildMode.values()) {
			MenuItem menuItem = new MenuItem(mBuild, SWT.NONE);
			menuItem.setText(mode.label());
			menuItem.setToolTipText(mode.description());
			menuItem.setData(mode);
			menuItem.setImage(new Image(parent.getDisplay(), getClass().getResourceAsStream(mode.icon())));
			menuItem.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					buildItem.setSelection(true);
					buildMode = (NgxBuilderBuildMode) e.widget.getData();
					handleProdBuild();
				}
				
			});
		}
		
		buildItem = item = new ToolItem(toolbar, SWT.CHECK);
		item.setToolTipText("Build locally");
		item.setImage(new Image(parent.getDisplay(), getClass().getResourceAsStream("/studio/build_prod_b.png")));
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean selected = ((ToolItem) e.widget).getSelection();
				if (!selected) {
					buildMode = null;
					handleProdBuild();
				} else {
					ToolItem item = (ToolItem) e.widget;
					Rectangle rect = item.getBounds(); 
					Point pt = item.getParent().toDisplay(new Point(rect.x, rect.y));
					mBuild.setLocation(pt);
					mBuild.setVisible(true);
					buildItem.setSelection(false);
				}
			}
		});
		
		new ToolItem(toolbar, SWT.SEPARATOR);
		
		item = new ToolItem(toolbar, SWT.CHECK);
		item.setToolTipText("Toggle auto build");
		item.setSelection(true);
		item.setImage(new Image(parent.getDisplay(), getClass().getResourceAsStream("/studio/accumulate.gif")));
		item.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				MobileBuilder mb = applicationEditorInput.application.getProject().getMobileBuilder();
				mb.setAutoBuild(((ToolItem) e.widget).getSelection());
			}
			
		});
		
		item = new ToolItem(toolbar, SWT.PUSH);
		item.setToolTipText("Manage modules");
		item.setImage(new Image(parent.getDisplay(), getClass().getResourceAsStream("/studio/show_blocks.gif")));
		item.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				MessageDialog dialog = new MessageDialog(
					null, "Node_module Update / Reinstall",
					null, "This will update or reinstall all your project's node_module dependencies. Update when you just " +
						     "added a new dependency to your ionicTpl/package.json file, or re-install if you want clean all your node_modules and do a fresh install (takes more time).",
					MessageDialog.QUESTION,
					new String[] {"Update", "Re-install", "Cancel"}, 0
				);
				int result = dialog.open();
				if (result < 2) {
					launchBuilder(true, result == 1);
				}
			}
			
		});
		
//		final ToolItem buildModeItem = item = new ToolItem(toolbar, SWT.DROP_DOWN);
		
//		final Menu buildModeMenu = new Menu(parent.getShell());
//		SelectionListener buildModeListener = new SelectionAdapter() {
//
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				dialogBuild(buildModeItem, (MenuItem) e.widget);
//			}
//			
//		};
//		
//		for (MobileBuilderBuildMode mode: MobileBuilderBuildMode.values()) {
//			MenuItem menuItem = new MenuItem(buildModeMenu, SWT.NONE);
//			menuItem.setText(mode.label());
//			menuItem.setToolTipText(mode.description());
//			menuItem.setData(mode);
//			menuItem.setImage(new Image(parent.getDisplay(), getClass().getResourceAsStream(mode.icon())));
//			menuItem.addSelectionListener(buildModeListener);
//			if (mode.equals(buildMode)) {
//				item.setImage(menuItem.getImage());
//				item.setToolTipText(mode.description());
//			}
//		}
//		
//		item.addSelectionListener(new SelectionAdapter() {
//			
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				if (e.detail == SWT.ARROW) {
//					ToolItem item = (ToolItem) e.widget;
//					Rectangle rect = item.getBounds(); 
//					Point pt = item.getParent().toDisplay(new Point(rect.x + 8, rect.y + 8));
//					buildModeMenu.setLocation(pt);
//					buildModeMenu.setVisible(true);
//				} else {
//					dialogBuild(buildMode);
//				}
//			}
//			
//		});
		
		new ToolItem(toolbar, SWT.SEPARATOR);
		
		item = new ToolItem(toolbar, SWT.DROP_DOWN);
		item.setToolTipText("Select dataset");
		item.setImage(new Image(parent.getDisplay(), getClass().getResourceAsStream("/studio/cvs_show_history.gif")));
		
		SelectionListener selectionListener = new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				setDataset(((MenuItem) e.widget).getText());
			}
			
		};
		
		Image iDataset = new Image(parent.getDisplay(), getClass().getResourceAsStream("/studio/cvs_checkin.gif"));
		Image iDatasetSelected = new Image(parent.getDisplay(), getClass().getResourceAsStream("/studio/cvs_checkout.gif"));
		final Menu mDataset = new Menu(toolbar);
				
		item.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (MenuItem item: mDataset.getItems()) {
					item.dispose();
				}
				
				MenuItem menuItem = new MenuItem(mDataset, SWT.NONE);
				menuItem.setText("none");
				
				for (String fDataset: datasetDir.list()) {
					Matcher m = pDatasetFile.matcher(fDataset);
					if (m.matches()) {
						menuItem = new MenuItem(mDataset, SWT.NONE);
						menuItem.setText(m.group(1));
					}
				}
				
				for (MenuItem item: mDataset.getItems()) {
					if (item.getText().equals(dataset)) {
						item.setImage(iDatasetSelected);
					} else {
						item.setImage(iDataset);
					}
					item.addSelectionListener(selectionListener);
				}

				ToolItem item = (ToolItem) e.widget;
				Rectangle rect = item.getBounds(); 
				Point pt = item.getParent().toDisplay(new Point(rect.x, rect.y));
				mDataset.setLocation(pt);
				mDataset.setVisible(true);
			}
			
		});
		
		item = new ToolItem(toolbar, SWT.PUSH);
		item.setToolTipText("Save dataset");
		item.setImage(new Image(parent.getDisplay(), getClass().getResourceAsStream("/studio/cvs_add.gif")));
		item.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				String[] name = {dataset.equals("none") ? "" : dataset};
				String extra = "";
				int response;
				
				do {
					MessageDialog dialog = new MessageDialog(null, "Dataset name", null, "What is the name of the dataset ?" + extra, MessageDialog.QUESTION, 0, new String[]{"Save", "Cancel"}) {
						
						@Override
						protected Control createCustomArea(Composite parent) {
							Text t = new Text(parent, SWT.NONE);
							t.setLayoutData(new GridData(GridData.FILL_BOTH));
							t.setText(name[0]);
							t.addModifyListener(new ModifyListener() {
								
								@Override
								public void modifyText(ModifyEvent e) {
									name[0] = t.getText();
								}
							});
							return t;
						}
						
					};
					response = dialog.open();
					extra = "";
					
					if (response == 0) {
						if (StringUtils.isBlank(name[0])) {
							extra = " (cannot be empty)";
						} else if (name[0].equals("none")) {
							extra = " (cannot override 'none')";
						}
					}
				} while (!extra.isEmpty());
				
				if (response == 0) {
					C8oBrowser.run(() -> {
						String value = c8oBrowser.executeJavaScriptAndReturnValue("sessionStorage._c8ocafsession_storage_data");
						try {
							FileUtils.write(new File(datasetDir, name[0] + ".json"), new JSONArray(value).toString(2), "UTF-8");
							toast("Dataset '" + name[0] + "' saved !");
							dataset = name[0];
						} catch (Exception e1) {
							toast("Dataset '" + name[0] + "' NOT saved ! " + e1.getMessage());
						}
					});
				}
			}
			
		});
		
		item = new ToolItem(toolbar, SWT.PUSH);
		item.setToolTipText("Remove dataset");
		item.setImage(new Image(parent.getDisplay(), getClass().getResourceAsStream("/studio/cvs_delete.gif")));
		item.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (!dataset.equals("none")) {
					boolean ok = MessageDialog.openQuestion(null, "Delete '" + dataset + "' ?", "You really want delete'" + dataset + "' ?");
					if (ok) {
						new File(datasetDir, dataset + ".json").delete();
						toast("Dataset '" + dataset + "' removed !");
						setDataset("none");
					}
				} else {
					toast("No dataset selected !");
				}
			}
			
		});
		
		
		item = new ToolItem(toolbar, SWT.CHECK);
		item.setToolTipText("Show Grids");
		item.setImage(new Image(parent.getDisplay(), getClass().getResourceAsStream("/studio/grid_color_16x16.png")));
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean showGrid = ((ToolItem) e.widget).getSelection();
				C8oBrowser.run(() -> c8oBrowser.executeJavaScriptAndReturnValue("_c8o_showGrids(" + (showGrid ? "true":"false") +")")); 
			}
		});
	}
	
	private void updateDevicesMenu() {
		for (MenuItem m: devicesMenu.getItems()) {
			m.dispose();
		}
		
		SelectionAdapter selectionAdapter = new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				deviceName.setText(((MenuItem) e.widget).getText().substring(2));
				deviceWidth.setText("" + e.widget.getData("width"));
				deviceHeight.setText("" + e.widget.getData("height"));
				zoomFactor = (ZoomFactor) e.widget.getData("zoom");
				setDeviceOS((DeviceOS) e.widget.getData("os"));
				
				updateBrowserSize();
			}
			
		};
		
		for (JSONArray devices: new JSONArray[]{devicesDefinition, devicesDefinitionCustom}) {
			int len = devices.length();
			for (int i = 0; i < len; i++) {
				MenuItem device = new MenuItem(devicesMenu, SWT.RADIO);
				try {
					JSONObject json = devices.getJSONObject(i);
					
					DeviceOS os = DeviceOS.android;
					try {
						os = DeviceOS.valueOf(json.getString("os"));
					} catch (Exception e) {	}
					
					device.addSelectionListener(selectionAdapter);
					device.setText((devices == devicesDefinition ? "🔒 " : "👤 ") + json.getString("name"));
					device.setImage(os.image());
					device.setData("width", json.getInt("width"));
					device.setData("height", json.getInt("height"));
					device.setData("zoom", ZoomFactor.get(json.has("zoom") ? json.getInt("zoom") : 100));
					device.setData("os", os);
					
					if (json.has("desc")) {
						device.setToolTipText(json.getString("desc"));
					}
				} catch (JSONException e1) {
					device.dispose();
				}
			}			
		}
	}
		
	private void updateBrowserSize() {
		int width = NumberUtils.toInt(deviceWidth.getText(), -1);
		int height = NumberUtils.toInt(deviceHeight.getText(), -1);
		
		width = zoomFactor.swt(width);
		height = zoomFactor.swt(height);
		browserGD.horizontalAlignment = width < 0 ? GridData.FILL : GridData.CENTER;
		browserGD.verticalAlignment = height < 0 ? GridData.FILL : GridData.CENTER;
		browserScroll.setMinWidth(browserGD.widthHint = browserGD.minimumWidth = width);
		browserScroll.setMinHeight(browserGD.heightHint = browserGD.minimumHeight = height);
		c8oBrowser.getParent().layout();
		
		C8oBrowser.run(() -> {
			c8oBrowser.executeJavaScriptAndReturnValue("try {_c8o_remove_all_overlay()} catch(e){}");
			c8oBrowser.setZoomLevel(zoomFactor.zoomLevel());
		});
	}
	
	@Override
	public void setFocus() {
		c8oBrowser.setFocus();
	}
	
	private void appendOutput(String... msg) {
		C8oBrowser.run(() -> {
			if (c8oBrowser.getURL().equals("about:blank")) {
				try {
					for (String m: msg) {
						c8oBrowser.executeFunctionAndReturnValue("loader_log", m);
					}
				} catch (Exception e) {
					// silently ignore
				}
			}
		});
	}
	
	private void progress(int progress) {
		C8oBrowser.run(() -> {
			if (c8oBrowser.getURL().equals("about:blank")) {
				try {
					c8oBrowser.executeFunctionAndReturnValue("doProgress", progress);
				} catch (Exception e) {
					// silently ignore
				}
			}
		});
	}
	
	private void toast(String msg) {
		Engine.logStudio.info("[Toast] " + msg);
		C8oBrowser.run(() -> {
			c8oBrowser.executeFunctionAndReturnValue("_c8o_toast");
		});
	}
	
	public void launchBuilder(boolean forceInstall) {
		launchBuilder(forceInstall, false);
	}
	
	public void launchBuilder(boolean forceInstall, boolean forceClean) {
		final int buildCount = ++this.buildCount;
		final boolean isDark = SwtUtils.isDark();

		// Close editors (*.temp.ts) to avoid npm error at build launch
		ConvertigoPlugin.getDisplay().syncExec(
			new Runnable() {
				public void run() {
					try {
						ApplicationComponent app = applicationEditorInput.application;
						IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
						if (activePage != null) {
							IEditorReference[] editorRefs = activePage.getEditorReferences();
							for (int i = 0; i < editorRefs.length; i++) {
								IEditorReference editorRef = (IEditorReference) editorRefs[i];
								try {
									IEditorInput editorInput = editorRef.getEditorInput();
									if (editorInput != null && editorInput instanceof ComponentFileEditorInput) {
										if (((ComponentFileEditorInput)editorInput).is(app) ||
											((ComponentFileEditorInput)editorInput).isChildOf(app)) {
												activePage.closeEditor(editorRef.getEditor(false), false);
										}
									}
								} catch(Exception e) {}
							}
						}
					} catch (Throwable t) {}
				}
			}
		);
		
		// Launch build
		Engine.execute(() -> {
			try {
				String loader = IOUtils.toString(getClass().getResourceAsStream("loader.html"), "UTF-8");
				if (isDark) {
					loader = loader.replace("lightblue", "rgb(47,47,47); color: white");
				}
				c8oBrowser.setText(loader);
			} catch (Exception e1) {
				throw new RuntimeException(e1);
			}
			
			project = applicationEditorInput.application.getProject();
			ionicDir = new File(project.getDirPath(), "_private/ionic");
			nodeModules = new File(ionicDir, "node_modules");
			
			String nodeVersion = ProcessUtils.getNodeVersion(project);
			nodeDir = ProcessUtils.getDefaultNodeDir();
			try {
				nodeDir = ProcessUtils.getNodeDir(nodeVersion, (r , t, x) -> {
					appendOutput("Downloading nodejs " + nodeVersion + ": " + Math.round((r * 100f) / t) + "%");
				});
			} catch (Exception e1) {
			}
			
			{
				String versions = "Will use nodejs " + ProcessUtils.getNodeVersion(nodeDir) + " and npm " + ProcessUtils.getNpmVersion(nodeDir);
				appendOutput(versions);
				Engine.logStudio.info(versions);
			}
			
			String path = nodeDir.getAbsolutePath();
			
			terminateNode(false);
			
			MobileBuilder mb = project.getMobileBuilder();
			
			if (forceInstall || !nodeModules.exists() || mb.getNeedPkgUpdate()) {
				boolean[] running = {true};
				try {
					new File(ionicDir, "package-lock.json").delete();
					new File(ionicDir, "pnpm-lock.yaml").delete();
					
					if (forceClean) {
						appendOutput("...", "...", "Removing existing node_modules... This can take several seconds...");
						Engine.logStudio.info("Removing existing node_modules... This can take several seconds...");
						com.twinsoft.convertigo.engine.util.FileUtils.deleteQuietly(nodeModules);
					}
					appendOutput("Installing node_modules... This can take several minutes depending on your network connection speed...");
					Engine.logStudio.info("Installing node_modules... This can take several minutes depending on your network connection speed...");
					
					ProcessBuilder pb;
					Process p;
					
					boolean useYarn = false;  // set to true to enable yarn again.
					if (useYarn) {
						File yarnFile = new File(ionicDir.toString() + "/node_modules/.bin/yarn");
						if (!yarnFile.exists()) {
							Engine.logStudio.info("Installing Yarn...");
							pb = ProcessUtils.getNpmProcessBuilder(path, "npm", "install", "yarn");
							pb.redirectErrorStream(true);
							pb.directory(ionicDir);
							p = pb.start();
							p.waitFor();
						}
						
						pb = ProcessUtils.getNpmProcessBuilder(path + File.pathSeparator + ionicDir.toString() + "/node_modules/.bin/", "yarn");
						pb.redirectErrorStream(true);
						pb.directory(ionicDir);
						p = pb.start();
					} else {
						File pnpmFile = new File(ionicDir.toString() + "/node_modules/.bin/pnpm");
						if (!pnpmFile.exists()) {
							Engine.logStudio.info("Installing Pnpm...");
							pb = ProcessUtils.getNpmProcessBuilder(path, "npm", "install", "pnpm", "--force");
							pb.redirectErrorStream(true);
							pb.directory(ionicDir);
							p = pb.start();
							p.waitFor();
						}
						
						pb = ProcessUtils.getNpmProcessBuilder(path + File.pathSeparator + ionicDir.toString() + "/node_modules/.bin/", "pnpm", "install", "--shamefully-hoist");
						pb.redirectErrorStream(true);
						pb.directory(ionicDir);
						p = pb.start();
					}
					
					/*
					Engine.execute(() -> {
						try {
							while (running[0]) {
								appendOutput("Collecting node_modules: " + FileUtils.byteCountToDisplaySize(FileUtils.sizeOfAsBigInteger(nodeModules)) + " (" + Math.round(System.currentTimeMillis() - start) / 1000 + " sec)");
								Engine.logStudio.info("Installing, node_module size is now : " + FileUtils.byteCountToDisplaySize(FileUtils.sizeOfAsBigInteger(nodeModules)));
								Thread.sleep(1000);
							} 
						} catch (Exception e) {
							appendOutput("Something wrong during the install: " + e);
						}
					});
					*/
					
					processes.add(p);
					BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
					String line;
					while ((line = br.readLine()) != null) {
						line = pRemoveEchap.matcher(line).replaceAll("");
						if (StringUtils.isNotBlank(line)) {
							Engine.logStudio.info(line);
							appendOutput(line);
						}
					}
					Engine.logStudio.info(line);
					appendOutput("\\o/");
				} catch (Exception e) {
					appendOutput(":( " + e);
				}
				running[0] = false;
			}
			
			mb.setNeedPkgUpdate(false);
			build(path, buildCount, mb);
		});
	}

	private void build(final String path, final int buildCount, final MobileBuilder mb) {	
		Object mutex = new Object();
		mb.setBuildMutex(mutex);
		try {
			ConvertigoPlugin.getDefault().getProjectPluginResource(project.getName()).refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (CoreException ce) {}
		
		try {
			mb.startBuild();
			File displayObjectsMobile = new File(project.getDirPath(), "DisplayObjects/mobile");
			displayObjectsMobile.mkdirs();
			
			this.applicationEditorInput.application.checkFolder();
			
			File assets = new File(displayObjectsMobile, "assets");
			if (assets.exists() && assets.isDirectory()) {
				appendOutput("Handle application assets");
				Engine.logStudio.info("Handle application assets");
				File privAssets = new File(ionicDir, "src/assets");
				FileUtils.deleteDirectory(privAssets);
				FileUtils.copyDirectory(assets, privAssets);
			}
			
			try {
				// TODO: to be removed -> has to be fixed in CAF
				File routerJS = new File(project.getDirPath(), "_private/ionic/node_modules/c8ocaf/src/caf/convertigo.router.js");
				if (routerJS.exists()) {
					String txt = FileUtils.readFileToString(routerJS, "UTF-8");
					String ntxt = txt.replaceAll("window\\[\\\"cordova\\\"\\]\\.InAppBrowser\\.open", "window[\"cordova\"][\"InAppBrowser\"].open");
					if (!txt.equals(ntxt)); {
						FileUtils.writeStringToFile(routerJS, ntxt, "UTF-8");
					}
				}
				File routerTS = new File(project.getDirPath(), "_private/ionic/node_modules/c8ocaf/src/caf/convertigo.router.ts");
				if (routerTS.exists()) {
					String txt = FileUtils.readFileToString(routerTS, "UTF-8");
					String ntxt = txt.replaceAll("window\\[\\\"cordova\\\"\\]\\.InAppBrowser\\.open", "window[\"cordova\"][\"InAppBrowser\"].open");
					if (!txt.equals(ntxt)); {
						FileUtils.writeStringToFile(routerTS, ntxt, "UTF-8");
					}
				}
			} catch (Exception e) {
				Engine.logStudio.warn("Failed to update CAF router", e);
			}
			
			ProcessBuilder pb = ProcessUtils.getNpmProcessBuilder(path, "npm", "run", "ionic:serve");
			
			List<String> cmd = pb.command();
			synchronized (usedPort) {
				cmd.add("--");
				cmd.add("--port="+ NetworkUtils.nextAvailable(8100, usedPort));
			}
			
			// #183 add useless option to help terminateNode method to find the current path 
			cmd.add("--ssl-key=" + ionicDir);
			
			pb.redirectErrorStream(true);
			pb.directory(ionicDir);
			Process p = pb.start();
			processes.add(p);
			

			Matcher matcher = Pattern.compile("(\\d+)% (.*)").matcher("");
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;
			
			while ((line = br.readLine()) != null) {
				line = pRemoveEchap.matcher(line).replaceAll("");
				if (StringUtils.isNotBlank(line)) {
					Engine.logStudio.info(line);
					matcher.reset(line);
					if (matcher.find()) {
						progress(Integer.parseInt(matcher.group(1)));
						appendOutput(matcher.group(2));
					} else {
						appendOutput(line);
					}
					if (line.matches(".*Compiled .*successfully.*")) {
						synchronized (mutex) {
							mutex.notify();
						}
						mb.buildFinished();
					}
					
					Matcher m = pIsBrowserOpenable.matcher(line);
					if (m.matches()) {
						JSONObject envJSON = new JSONObject();
						envJSON.put("remoteBase", EnginePropertiesManager.getProperty(PropertyName.APPLICATION_SERVER_CONVERTIGO_URL) + "/projects/" + project.getName() + "/_private");
						FileUtils.write(new File(displayObjectsMobile, "env.json"), envJSON.toString(4), "UTF-8");
						String sGroup = m.group(1);
						baseUrl = sGroup.substring(0, sGroup.lastIndexOf("/"));
						doLoad();
					}
				}
			}
			
			if (buildCount == this.buildCount) {
				appendOutput("\\o/");
			} else {
				appendOutput("previous build canceled !");
			}
		} catch (Exception e) {
			appendOutput(":( " + e);
		} finally {
			synchronized (mutex) {
				mutex.notify();
			}
			mb.setBuildMutex(null);
			mb.buildFinished();
			try {
				ConvertigoPlugin.getDefault().getProjectPluginResource(project.getName()).refreshLocal(IResource.DEPTH_INFINITE, null);
			} catch (CoreException ce) {}
		}
	}

	private JSONObject findDevice(JSONArray devices, String name) {
		int len = devices.length();
		for (int i = 0; i < len; i++) {
			try {
				JSONObject device = devices.getJSONObject(i);
				if (name.equals(device.getString("name"))) {
					return device;
				}
			} catch (Exception ex) {
			}
		}
		return null;
	}
	
	private void setDeviceBarVisible(boolean visible) {
		deviceBar.setVisible(visible);
		GridData gd = (GridData) deviceBar.getLayoutData();
		gd.exclude = !visible;
		deviceBar.getParent().layout();
	}
	
	private String getPageUrl() {
		String url = baseUrl;
		if (url != null && pagePath != null) {
			url += "/" + pagePath;
		}
		return url;
	}
	
	private void doLoad() {
		String url = getPageUrl();
		if (url != null) {
			C8oBrowser.run(() -> {
				String currentUrl = c8oBrowser.getURL();
				if (headlessBuild) {
					c8oBrowser.setUseExternalBrowser(true);
					c8oBrowser.executeFunctionAndReturnValue("_c8o_doLoad", url);
				} else if (!currentUrl.equals(url)) {
					c8oBrowser.setUseExternalBrowser(false);
					c8oBrowser.loadURL(url);
				}
			});
			c8oBrowser.getDisplay().asyncExec(() -> {
				MobileDebugView view = ConvertigoPlugin.getDefault().getMobileDebugView(false);
				if (view != null) {
					view.onActivated(ApplicationComponentEditor.this);
				}
			});
		}
	}
	
	public String getDebugUrl() {
		return debugUrl;
	}
	
	public void selectPage(String pagePath) {
		if (this.pagePath != null && this.pagePath.equals(pagePath)) {
			return;
		}
		
		this.pagePath = pagePath;
		doLoad();
	}
	
	private Element exHighlightElement = null;
	private MobileComponent exHighlightMobileComponent = null;
	
	private void highlightPoint(int x, int y) {
		Node node = browser.mainFrame().get().inspect(x, y).node().get();
		while (!(node == null || node instanceof Element)) {
			node = node.parent().orElse(null);
		}
		
		Object shadowHost = c8oBrowser.executeFunctionAndReturnValue("_c8o_getShadowHost", node);
		if (shadowHost != null && shadowHost instanceof Element) {
			node = (Element) shadowHost;
		}
		
		while (node != null) {
			Element element = (Element) node;
			if (element.equals(exHighlightElement)) {
				return;
			}
			exHighlightElement = element;
			String classes = element.attributeValue("class");
			Matcher mPriority = pPriority.matcher(classes);
			if (mPriority.find()) {
				try {
					node = null;
					long priority = Long.parseLong(mPriority.group(1));
					new WalkHelper() {

						@Override
						protected void walk(DatabaseObject databaseObject) throws Exception {
							if (databaseObject instanceof UISharedComponent) {
								UISharedComponent uisc = (UISharedComponent)databaseObject;
								if (uisc != null) {
									databaseObject = uisc;
								}
							} else if (databaseObject instanceof UIUseShared) {
								UISharedComponent uisc = ((UIUseShared)databaseObject).getTargetSharedComponent();
								if (uisc != null) {
									databaseObject = uisc;
								}
							} else if (databaseObject instanceof UIDynamicInvoke) {
								UIDynamicInvoke uidi = (UIDynamicInvoke) databaseObject;
								UIActionStack uisa = uidi.getTargetSharedAction();
								if (uisa != null) {
									if (!uidi.isRecursive()) {
										databaseObject = uisa;
									}
								}
							}
							
							if (databaseObject.priority == priority) {
								throw new DatabaseObjectFoundException(databaseObject);
							}
							super.walk(databaseObject);
						}
						
					}.init(applicationEditorInput.application);
				} catch (DatabaseObjectFoundException e) {
					DatabaseObject databaseObject = e.getDatabaseObject();

					c8oBrowser.getDisplay().asyncExec(() -> ConvertigoPlugin.getDefault().getProjectExplorerView().objectSelected(new CompositeEvent(databaseObject)));
					
					if (databaseObject instanceof MobileComponent && !databaseObject.equals(exHighlightMobileComponent)) {
						highlightComponent(exHighlightMobileComponent = (MobileComponent) databaseObject, false);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				node = node.parent().orElse(null);
				while (!(node == null || node instanceof Element)) {
					node = node.parent().orElse(null);
				}
			}
		}
	}

	public void highlightComponent(MobileComponent mobileComponent, boolean selectPage) {
		C8oBrowser.run(() -> {
			if (selectPage && mobileComponent instanceof UIComponent) {
				PageComponent pageComponent = ((UIComponent) mobileComponent).getPage();
				if (pageComponent != null) {
					selectPage(pageComponent.getSegment());
				}
			}
			
			Document doc = browser.mainFrame().get().document().get();
			MobileComponent mc = mobileComponent;
			if (mc instanceof UISharedComponent) {
				UISharedComponent uisc = (UISharedComponent)mc;
				if (uisc != null) {
					try {
						mc = uisc.getDisplayableComponentList().get(0);
					} catch (IndexOutOfBoundsException ioobe) {}
				}
			} else if (mc instanceof UIUseShared) {
				UISharedComponent uisc = ((UIUseShared)mc).getTargetSharedComponent();
				if (uisc != null) {
					try {
						mc = uisc.getDisplayableComponentList().get(0);
					} catch (IndexOutOfBoundsException ioobe) {}
				}
			}
			
			while (doc.findElementsByClassName("class" + mc.priority).isEmpty()) {
				DatabaseObject parent = mc.getParent();
				if (parent instanceof MobileComponent) {
					mc = (MobileComponent) parent;
				} else {
					return;
				}
			}
			c8oBrowser.executeJavaScriptAndReturnValue("_c8o_highlight_class('class" + mc.priority + "');");
		});
	}
	
	private void doReload() {
		C8oBrowser.run(() -> {
			if (!c8oBrowser.getURL().equals("about:blank")) {
				c8oBrowser.reload();
			}
		});
	}
	
	private void setDeviceOS(DeviceOS deviceOS) {
		if (!this.deviceOS.equals(deviceOS) && deviceOS != null) {
			this.deviceOS = deviceOS;
			deviceOsToolItem.setImage(deviceOS.image());
			doReload();
		}
	}

	private void setDataset(String dataset) {
		if (!this.dataset.equals(dataset)) {
			this.dataset = dataset;
			doReload();
		}
	}
	
	private void terminateNode(boolean prodOnly) {
		String projectName = new File(applicationEditorInput.application.getProject().getDirPath()).getName();
		int retry = 10;
		try {
			while (retry-- > 0) {
				if (Engine.isWindows()) {
					String prod = prodOnly ? " AND CommandLine Like '%--watch%'" : "";
					Process process = new ProcessBuilder("wmic", "PROCESS", "WHERE",
						"Name='node.exe' AND CommandLine Like '%\\\\" + projectName + "\\\\_private\\\\%'" + prod,
						"CALL", "TERMINATE").start();
					String output = IOUtils.toString(process.getInputStream(), Charset.defaultCharset());
					process.waitFor();
					int id = output.indexOf('\n');
					if (id == -1 || output.indexOf('\n', id) == -1) {
						retry = 0;
					}
				} else {
					//ps -e | sed -n -E "s/ ([0-9]+).*Fli.*/\1/p" | xargs kill
					String prod = prodOnly ? " | grep \"--watch\"" : "";
					Process process = new ProcessBuilder("/bin/bash", "-c",
						"ps -e | grep -v \"sed -n\"" + prod + " | sed -n -E \"s, ([0-9]+).*node.*/"+ projectName + "/_private/.*,\\1,p\" | xargs kill"
					).start();
					int code = process.waitFor();
					if (code == 0) {
						retry = 0;
					}
				}
			}
		} catch (Exception e) {
			Engine.logStudio.warn("Failed to terminate the node server", e);
		}
	}

	@Override
	public void onPackageUpdated() {
		launchBuilder(true);
		ConvertigoPlugin.getDisplay().syncExec(
			new Runnable() {
				public void run() {
					try {
						ConvertigoPlugin.infoMessageBox("Some needed packages will be installed");
					} catch (Throwable t) {
						t.printStackTrace();
					}
				}
			}
		);
	}
	
	Job prodJob = null;
	private void handleProdBuild() {
		terminateNode(true);
		if (prodJob != null) {
			prodJob.cancel();
			prodJob = null;
		}
		if (buildMode == null) {
			return;
		}
		NgxBuilderBuildMode buildMode = this.buildMode;
		
		String appName = applicationEditorInput.application.getParent().getComputedApplicationName();
		prodJob = Job.create("Build in " + buildMode.label() + " mode for " + appName, monitor -> {
			try {
				Engine.logStudio.debug("Build in " + buildMode.label() + " mode requested for " + appName);
				monitor.beginTask("Removing previous build directory", 5);
				monitor.worked(1);
				
				File displayObjectsMobile = new File(project.getDirPath(), "DisplayObjects/mobile");
				displayObjectsMobile.mkdirs();

				monitor.worked(1);
				for (File f: displayObjectsMobile.listFiles()) {
					if (!f.getName().equals("assets")) {
						com.twinsoft.convertigo.engine.util.FileUtils.deleteQuietly(f);
					}
				}
				monitor.worked(3);
				monitor.beginTask("Launching the " + buildMode.label() + " build", 200);
				
				String path = nodeDir.getAbsolutePath();
				
				ProcessBuilder pb = ProcessUtils.getNpmProcessBuilder(path, "npm", "run", buildMode.command());
				
				List<String> cmd = pb.command();
				cmd.add("--");
				cmd.add("--progress=true");
				cmd.add("--watch=" + (buildMode == NgxBuilderBuildMode.watch));
				cmd.add("--outputPath=./../../DisplayObjects/mobile/");
				cmd.add("--baseHref=./");
				cmd.add("--deployUrl=./");
				
				// #183 add useless option to help terminateNode method to find the current path
				cmd.add("--ngsw-config-path=" + ionicDir);
				
				pb.redirectErrorStream(true);
				pb.directory(ionicDir);
				pb.environment().put("NODE_OPTIONS", "max-old-space-size=8192");
				Process p = pb.start();
				processes.add(p);
				BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String line;
				
				Matcher matcher = Pattern.compile("(\\d+)% (.*)").matcher("");
				int lastProgress = 0;
				
				while ((line = br.readLine()) != null) {
					line = pRemoveEchap.matcher(line).replaceAll("");
					if (StringUtils.isNotBlank(line)) {
						matcher.reset(line);
						if (matcher.find()) {
							if (lastProgress == 0) {
								monitor.beginTask("Webpack in progress", 200);
							}
							int progress = Integer.parseInt(matcher.group(1));
							int diff = progress - lastProgress;
							lastProgress = progress;
							monitor.subTask(matcher.group(2));
							monitor.worked(diff);
							if (progress == 100) {
								lastProgress = 0;
								monitor.beginTask("Build almost finish", 200);
							}
						} else {
							monitor.worked(1);
						}
						Engine.logStudio.trace(line);
						if (line.contains("- Hash:")) {
							Engine.logStudio.debug("Build " + buildMode.label() + " finished for " + appName);
							if (buildMode == NgxBuilderBuildMode.watch) {
								monitor.beginTask("Build finished, waiting for changes or build cancel", 200);
							}
						}
					}
				}
				buildItem.getDisplay().asyncExec(() -> {
					if (!buildItem.isDisposed()) {
						buildItem.setSelection(false);
					}					
				});
				monitor.done();
				terminateNode(true);
			} catch (Exception e) {
				Engine.logStudio.error("Failed to process the build: " + e.getMessage(), e);
			}
		});
		prodJob.schedule();
	}
}
