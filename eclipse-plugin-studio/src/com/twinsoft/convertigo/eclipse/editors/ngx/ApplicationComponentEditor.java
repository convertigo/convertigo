/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

import java.awt.image.BufferedImage;
import java.beans.BeanInfo;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collection;
import java.util.Collections;
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
import org.eclipse.jface.dialogs.Dialog;
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
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
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

import com.d_project.qrcode.QRCode;
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
import com.twinsoft.convertigo.beans.common.FormatedContent;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.ngx.components.MobileComponent;
import com.twinsoft.convertigo.beans.ngx.components.MobileSmartSourceType;
import com.twinsoft.convertigo.beans.ngx.components.PageComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIControlEvent;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicAction;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicElement;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicInvoke;
import com.twinsoft.convertigo.beans.ngx.components.UIElement;
import com.twinsoft.convertigo.beans.ngx.components.UISharedComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIStyle;
import com.twinsoft.convertigo.beans.ngx.components.UIText;
import com.twinsoft.convertigo.beans.ngx.components.UIUseShared;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.dnd.PaletteSource;
import com.twinsoft.convertigo.eclipse.dnd.PaletteSourceTransfer;
import com.twinsoft.convertigo.eclipse.dnd.TreeDropAdapter;
import com.twinsoft.convertigo.eclipse.editors.CompositeEvent;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowser;
import com.twinsoft.convertigo.eclipse.swt.SwtUtils;
import com.twinsoft.convertigo.eclipse.swt.SwtUtils.SelectionListener;
import com.twinsoft.convertigo.eclipse.views.mobile.MobileDebugView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.DatabaseObjectFoundException;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.admin.services.studio.ngxbuilder.BuilderUtils;
import com.twinsoft.convertigo.engine.enums.MobileBuilderBuildMode;
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
		public void onDrag(JsObject o) {
			if (o != null && "cancel".equals(o.property("msg").orElse(""))) {
				dragStartMobileComponent = null;
			} else {
				dragStartMobileComponent = exHighlightMobileComponent;
			}
		}

		@JsAccessible
		public void onDragOver(JsObject o) {
			try {
				PaletteSource paletteSource = PaletteSourceTransfer.getInstance().getPaletteSource();
				if (paletteSource != null) {
					highlightPoint((Node) o.property("target").get());
				}
			} catch (Exception e) {
			}
		}

		@JsAccessible
		public void onDrop(JsObject o) {
			try {
				String dropOption = o.property("dropOption").get().toString();
				PaletteSource paletteSource = PaletteSourceTransfer.getInstance().getPaletteSource();
				DatabaseObject target = "inside".equals(dropOption) ? exHighlightMobileComponent : exHighlightMobileComponent.getParent();
				if (paletteSource == null && exHighlightMobileComponent != null && dragStartMobileComponent != null) {
					DatabaseObject fTarget = target;
					c8oBrowser.getDisplay().asyncExec(() -> {
						try {
							ProjectExplorerView view = ConvertigoPlugin.getDefault().getProjectExplorerView();
							TreeObject src = view.findTreeObjectByUserObject(dragStartMobileComponent);

							TreeParent trg = (TreeParent) view.findTreeObjectByUserObject(fTarget);
							BatchOperationHelper.start();
							if (src.getParent() != trg) {
								ConvertigoPlugin.clipboardManagerDND.cutAndPaste(src, trg);
								view.reloadTreeObject(trg);
								view.reloadTreeObject(src.getParent());
							}
							if (fTarget != exHighlightMobileComponent) {
								view.moveChildTo(trg, src, view.findTreeObjectByUserObject(exHighlightMobileComponent), "before".equals(dropOption));
							}
							BatchOperationHelper.stop();
						} catch (Exception e) {
							e.printStackTrace();
						} finally {
							BatchOperationHelper.cancel();
							dragStartMobileComponent = null;
						}
					});
				} else if (paletteSource != null) {
					DatabaseObject source = PaletteSourceTransfer.getInstance().getPaletteSource().getDatabaseObject();
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
								mb = project.getMobileBuilder();
							}
							if (mb != null) {
								mb.prepareBatchBuild();
							}

							ProjectExplorerView view = ConvertigoPlugin.getDefault().getProjectExplorerView();
							TreeParent treeObject = (TreeParent) view.findTreeObjectByUserObject(fTarget);
							BatchOperationHelper.start();
							TreeDropAdapter.paste(source, fTarget, true);
							view.reloadTreeObject(treeObject);
							if (fTarget != exHighlightMobileComponent) {
								view.moveLastTo(treeObject, view.findTreeObjectByUserObject(exHighlightMobileComponent), "before".equals(dropOption));
							}
							BatchOperationHelper.stop();
						} catch (Exception e) {
							Engine.logStudio.debug("Failed to drop: " + e.getMessage());
						} finally {
							PaletteSourceTransfer.getInstance().setPaletteSource(null);
							BatchOperationHelper.cancel();
							dragStartMobileComponent = null;
							Engine.logStudio.info("---------------------- Drop ended   ----------------------");
						}
					});
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		@JsAccessible
		public void onEditorEvent(String s) {
			Engine.logStudio.info("onEditorEvent: " + s);
			try {
				JSONObject json = new JSONObject(s);
				String event = json.getString("event");
				if ("style:target".equals(event)) {
					JSONArray selectors = json.getJSONObject("target").getJSONArray("selectors");
					for (int i = 0; i < selectors.length(); i++) {
						var cls = selectors.getString(i);
						var matcher =  pPriority.matcher(cls);
						if (matcher.matches()) {
							highlightPriority(Long.parseLong(matcher.group(1)));
							return;
						}
					}
				}
				json.toString();
			} catch (Exception e) {
				Engine.logStudio.error("onEditorEvent", e);
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
	private ToolItem showGrids;
	private ToolItem editStyle;
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

	private static Pattern pIsBrowserOpenable = Pattern.compile(".*?open your browser on (http\\S*).*");
	private static Pattern pRemoveEchap = Pattern.compile("\\x1b\\[\\d+m");
	private static Pattern pPriority = Pattern.compile("class(\\d+)");
	private static Pattern pDatasetFile = Pattern.compile("(.+).json");

	private static final Set<Integer> usedPort = new HashSet<>();
	private int portNode;

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
		project = application.getProject();

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
			try {
				headlessBuild = device.getBoolean("headlessBuild");
			} catch (Exception e) { }
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

		String[] inject = {null};
		try (InputStream is = getClass().getResourceAsStream("inject.js")) {
			inject[0] = IOUtils.toString(is, "UTF-8");
			inject[0] = inject[0].replace("${projectUrl}", EnginePropertiesManager.getProperty(PropertyName.APPLICATION_SERVER_CONVERTIGO_URL) + "/projects/" + project.getName());
		} catch (Exception e2) {
			Engine.logStudio.info("failure", e2);
			inject[0] = "alert('the editor is broken, please restart the studio')";
		}

		browser.set(InjectJsCallback.class, params -> {
			String url = params.frame().browser().url();
			if (baseUrl != null && url.startsWith(baseUrl)) {
				try {
					Frame frame = params.frame();
					JsObject sessionStorage = frame.executeJavaScript("sessionStorage");
					frame.executeJavaScript(
							"sessionStorage.setItem('_c8ocafsession_storage_mode', 'session');\n"
									+ "navigator.__defineGetter__('userAgent', function(){ return '" + deviceOS.agent() + "'});\n"
									+ inject[0]
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
		if (SwtUtils.isDark()) {
			deviceBar.setData("style", "background-color: #4c525d");
		}
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
		SelectionListener selectionListener = e -> {
			setDeviceOS((DeviceOS) e.widget.getData());
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
		deviceOsToolItem.addSelectionListener((SelectionListener) e -> {
			ToolItem item = (ToolItem) e.widget;
			Rectangle rect = item.getBounds();
			Point pt = item.getParent().toDisplay(new Point(rect.x, rect.y));
			mOS.setLocation(pt);
			mOS.setVisible(true);
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

		for (ToolItem ti: tb.getItems()) {
			ti.setData("style", "background-color: unset");
		}

		tb = new ToolBar(deviceBar, SWT.NONE);
		ToolItem button = new ToolItem(tb, SWT.PUSH);
		SwtUtils.setToolItemIcon(button, "icons/studio/zoom_out.png", "Zoom out", "Zoom out");
		button.addSelectionListener((SelectionListener) e -> {
			zoomFactor = zoomFactor.out();
			updateBrowserSize();
		});

		button = new ToolItem(tb, SWT.PUSH);
		SwtUtils.setToolItemIcon(button, "icons/studio/zoom_reset.png", "Zoom reset", "Zoom reset");
		button.addSelectionListener((SelectionListener) e -> {
			zoomFactor = ZoomFactor.z100;
			updateBrowserSize();
		});

		button = new ToolItem(tb, SWT.PUSH);
		SwtUtils.setToolItemIcon(button, "icons/studio/zoom_in.png", "Zoom in", "Zoom in");
		button.addSelectionListener((SelectionListener) e -> {
			zoomFactor = zoomFactor.in();
			updateBrowserSize();
		});

		new ToolItem(tb, SWT.SEPARATOR);

		button = new ToolItem(tb, SWT.PUSH);
		SwtUtils.setToolItemIcon(button, "icons/studio/dbo_save.gif", "Save", "Save");
		button.addSelectionListener((SelectionListener) e -> {
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
		});

		button = new ToolItem(tb, SWT.PUSH);
		SwtUtils.setToolItemIcon(button, "icons/studio/project_delete.gif", "Delete", "Delete");
		button.addSelectionListener((SelectionListener) e -> {
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
		});

		for (ToolItem ti: tb.getItems()) {
			ti.setData("style", "background-color: unset");
		}

		for (Control c: deviceBar.getChildren()) {
			c.setData("style", "background-color: unset");
		}
	}

	private void createToolbar(Composite parent) {
		ConvertigoPlugin plugin = ConvertigoPlugin.getDefault();
		toolbar = new ToolBar(parent, SWT.VERTICAL);
		if (SwtUtils.isDark()) {
			toolbar.setData("style", "background-color: #4c525d");
		}
		GridData gd = new GridData(GridData.FILL, GridData.FILL, false, true);
		gd.verticalSpan = 2;
		toolbar.setLayoutData(gd);

		new ToolItem(toolbar, SWT.SEPARATOR);

		ToolItem item = new ToolItem(toolbar, SWT.DROP_DOWN);
		item.setToolTipText("Select device viewport. Click to toggle the custom device bar.");
		try {
			item.setImage(plugin.getIconFromPath("/com/twinsoft/convertigo/beans/core/images/mobiledevice_color_16x16.png", BeanInfo.ICON_COLOR_16x16));
		} catch (Exception e) {
		}
		item.addSelectionListener((SelectionListener) e -> {
			if (!((ToolItem) e.widget).isEnabled()) {
				return;
			}
			if (e.detail == SWT.ARROW) {
				ToolItem it = (ToolItem) e.widget;
				Rectangle rect = it.getBounds();
				Point pt = it.getParent().toDisplay(new Point(rect.x + 8, rect.y + 8));
				devicesMenu.setLocation(pt);
				devicesMenu.setVisible(true);
			} else {
				setDeviceBarVisible(!deviceBar.getVisible());
			}
		});

		new ToolItem(toolbar, SWT.SEPARATOR);

		item = new ToolItem(toolbar, SWT.PUSH);
		item.setToolTipText("Change orientation");
		try {
			item.setImage(plugin.getIconFromPath("/com/twinsoft/convertigo/beans/connectors/images/fullsyncconnector_color_16x16.png", BeanInfo.ICON_COLOR_16x16));
		} catch (Exception e) {
		}
		item.addSelectionListener((SelectionListener) e -> {
			String width = deviceWidth.getText();
			deviceWidth.setText(deviceHeight.getText());
			deviceHeight.setText(width);
			updateBrowserSize();
		});

		new ToolItem(toolbar, SWT.SEPARATOR);
		
		item = new ToolItem(toolbar, SWT.PUSH);
		var refreshBtn = item;
		SwtUtils.setToolItemIcon(item, "icons/studio/refresh.gif", "Refresh", "Refresh");
		item.addSelectionListener((SelectionListener) e -> {
			disableEdition();
			doReload();
		});

		item = new ToolItem(toolbar, SWT.PUSH);
		SwtUtils.setToolItemIcon(item, "icons/studio/undo.gif", "Back", "Back");
		item.addSelectionListener((SelectionListener) e -> {
			C8oBrowser.run(() -> {
				int index = c8oBrowser.getCurrentNavigationEntryIndex();
				if (index > 2) {
					c8oBrowser.goBack();
				}
			});
		});

		new ToolItem(toolbar, SWT.SEPARATOR);
		
		item = new ToolItem(toolbar, SWT.CHECK);
		editStyle = item;
		SwtUtils.setToolItemIcon(item, "icons/studio/edit_16x16.png", "Style editor", "Style editor");
		item.addSelectionListener((SelectionListener) e -> {
			var selected = ((ToolItem) e.widget).getSelection();
			for (var it: toolbar.getItems()) {
				if (it != e.widget && it != refreshBtn) {
					it.setEnabled(!selected);
				}
			}
			int width = NumberUtils.toInt(deviceWidth.getText(), -1);
			int height = NumberUtils.toInt(deviceHeight.getText(), -1);
			var device = width < 1 ? "desktop" : width > height ? "mobileLandscape" : "mobilePortrait";
			if (selected) {
				editStyle.setData("deviceWidth", deviceWidth.getText());
				editStyle.setData("deviceHeight", deviceHeight.getText());
				editStyle.setData("zoomFactor", zoomFactor);
				editStyle.setData("deviceBar.isVisible", deviceBar.isVisible());
				deviceWidth.setText("-1");
				deviceHeight.setText("-1");
				zoomFactor = ZoomFactor.z100;
				setDeviceBarVisible(false);
				updateBrowserSize();
			} else {
				applySelectedDevice();
			}
			C8oBrowser.run(() -> {
				if (selected) {
					c8oBrowser.executeFunctionAndReturnValue("initGrapesJS", device);
				} else {
					try {
						var changes = new JSONObject((String) c8oBrowser.executeFunctionAndReturnValue("getEditorChanges"));
						Engine.execute(() -> {
							try {
								var dbosToReload = new HashSet<DatabaseObject>();
								var scss = changes.getJSONObject("scss");
								for (var k = scss.keys(); k.hasNext();) {
									var p = (String) k.next();
									try {
										var dbo = findDatabaseObject(Long.parseLong(p));
										if (dbo instanceof UIElement uie) {
											var dboChanged = false;
											var style = (UIStyle) null;
											for (var child :uie.getDatabaseObjectChildren()) {
												if (child instanceof UIStyle s) {
													if (style == null) {
														style = s;
													} else {
														uie.remove(s);
														dboChanged = true;
													}
												}
											};

											var content = scss.getString(p);											
											if (style == null) {
												style = new UIStyle();
												style.setName("styleEditor");
												uie.addUIComponent(style);
												BuilderUtils.dboAdded(uie);
											} else if (!dboChanged && style.getStyleContent().getString().equals(content)) {
												continue;
											}
											style.setStyleContent(new FormatedContent(content));
											dbosToReload.add(dbo);
										}
									} catch (Exception ex) {
										Engine.logStudio.error("failed", ex);
									}
								}
								var text = changes.getJSONObject("text");
								for (var i = text.keys(); i.hasNext();) {
									var priority = (String) i.next();
									var txt = text.getString(priority);
									var dbo = findDatabaseObject(Long.parseLong(priority));
									var uitext = (UIText) null;
									for (var c: dbo.getAllChildren()) {
										if (c instanceof UIText u) {
											if (uitext == null) {
												uitext = u;
											} else {
												uitext = null;
												break;
											}
										}
									}
									if (uitext != null) {
										var smart = uitext.getTextSmartType();
										if (smart.getMode() == MobileSmartSourceType.Mode.PLAIN) {
											var exValue = smart.getSmartValue();
											if (!exValue.equals(txt)) {
												smart.setSmartValue(txt);
												dbosToReload.add(uitext);
											}
										}
									}
								}
								var moved = changes.getJSONArray("move");
								for (var i = 0; i < moved.length(); i++) {
									try {
										var move = moved.getJSONObject(i);
										var target = (UIComponent) findDatabaseObject(Long.parseLong(move.getString("target")));
										var parentc = (UIComponent) findDatabaseObject(Long.parseLong(move.getString("parent")));
										var index = move.getLong("index");
										var exParent = target.getParent();
										if (parentc != exParent) {
											exParent.remove(target);
											dbosToReload.add(exParent);
											parentc.add(target, null);
										}
										var order = (long) parentc.getOrder(target);
										while (order < index) {
											parentc.decreasePriority(target);
											order = (long) parentc.getOrder(target);
										}
										while (order > index) {
											parentc.increasePriority(target);
											order = (long) parentc.getOrder(target);
										}
										dbosToReload.add(parentc);
									} catch (Exception ex) {
										// TODO: handle exception
									}
								}
								for (var dbo: dbosToReload) {
									BuilderUtils.dboUpdated(dbo);
								}
								c8oBrowser.getDisplay().asyncExec(() -> {
									if (dbosToReload.isEmpty()) {
										doReload();
									} else {
										var pew = ConvertigoPlugin.getDefault().getProjectExplorerView();
										for (var dbo: dbosToReload) {
											try {
												pew.reloadDatabaseObject(dbo);
											} catch (Exception e1) {
												e1.printStackTrace();
											}
										}
									}
								});
							} catch (Exception e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						});
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			});
		});
		item.setSelection(false);

		new ToolItem(toolbar, SWT.SEPARATOR);

		item = new ToolItem(toolbar, SWT.PUSH);
		SwtUtils.setToolItemIcon(item, "icons/studio/write_wait_zone.gif", "Remove highlight", "Remove highlight");
		item.addSelectionListener((SelectionListener) e -> {
			exHighlightElement = null;
			exHighlightMobileComponent = null;
			C8oBrowser.run(() -> c8oBrowser.executeJavaScriptAndReturnValue("_c8o_remove_all_overlay()"));
		});

		new ToolItem(toolbar, SWT.SEPARATOR);

		item = new ToolItem(toolbar, SWT.PUSH);
		SwtUtils.setToolItemIcon(item, "icons/studio/debug.gif", "Show debug", "Show debug");
		item.addSelectionListener((SelectionListener) e -> {
			MobileDebugView view = ConvertigoPlugin.getDefault().getMobileDebugView(true);
			getSite().getPage().activate(view);
			view.onActivated(ApplicationComponentEditor.this);
		});

		item = new ToolItem(toolbar, SWT.PUSH);
		SwtUtils.setToolItemIcon(item, "icons/studio/ContinueWithSiteClipperStatement_color_16x16.png", "Open in default browser", "Open in default browser");
		item.addSelectionListener((SelectionListener) e -> {
			C8oBrowser.run(() -> {
				String url = headlessBuild ? getPageUrl() : c8oBrowser.getURL();
				if (url.startsWith("http")) {
					org.eclipse.swt.program.Program.launch(url);
				}
			});
		});

		item = new ToolItem(toolbar, SWT.PUSH);
		SwtUtils.setToolItemIcon(item, "icons/studio/qrcode_16x16.png", "Show QR Code", "Show QR Code");
		item.addSelectionListener((SelectionListener) e -> {
			C8oBrowser.run(() -> {
				String url = headlessBuild ? getPageUrl() : c8oBrowser.getURL();
				if (url.startsWith("http")) {
					ConvertigoPlugin.asyncExec(() -> {
						Image[] img = new Image[] { null };
						Label[] image = new Label[] { null };
						Dialog dialog = new Dialog(parent.getShell()) {

							@Override
							protected Control createContents(Composite parent) {
								Composite composite = new Composite(parent, SWT.NONE);
								composite.setLayoutData(new GridData(GridData.FILL_BOTH));
								composite.setLayout(new GridLayout(1, true));
								Label tips = new Label(composite, SWT.NONE);
								tips.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
								tips.setText("Show the live-view on your Mobile Device  !");
								tips = new Label(composite, SWT.NONE);
								tips.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
								tips.setText("Please choose the endpoint on the same network");
								tips = new Label(composite, SWT.NONE);
								tips.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
								tips.setText("your mobile device is connnected to. This will display a QRCode");
								tips = new Label(composite, SWT.NONE);
								tips.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
								tips.setText("you will be able to flash from your mobile device :\n ");
								try {
									for (NetworkInterface netint: Collections.list(NetworkInterface.getNetworkInterfaces())) {
										for (InetAddress addr: Collections.list(netint.getInetAddresses())) {
											String ip = addr.getHostAddress();
											if (!ip.contains(":")) {
												Button label = new Button(composite, SWT.FLAT | SWT.PUSH | SWT.WRAP);
												String href = url.replace("localhost", ip);
												label.setText("[" + netint.getDisplayName() + "]\n" + href);
												GridData gd;
												label.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_VERTICAL));
												gd.minimumHeight = 50;
												label.addSelectionListener((SelectionListener) e -> {
													try {
														if (img[0] != null) {
															img[0].dispose();
															img[0] = null;
														}
														QRCode qrcode = QRCode.getMinimumQRCode(href, 0);
														BufferedImage bufferedImage = qrcode.createImage(8, 8);
														ImageData imageData = SwtUtils.convertToSWT(bufferedImage).scaledTo(250, 250);
														img[0] = new Image(parent.getDisplay(), imageData);
														image[0].setImage(img[0]);
														image[0].addMouseListener(new MouseAdapter() {

															@Override
															public void mouseDown(MouseEvent e) {
																close();
															}
														});
														parent.layout();
													} catch (Exception e2) {}
												});
											}
										}
									}
								} catch (SocketException e) {}
								image[0] = new Label(composite, SWT.NONE);
								GridData gd;
								image[0].setLayoutData(gd = new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.HORIZONTAL_ALIGN_CENTER));
								gd.minimumHeight = 250;
								gd.minimumWidth = 250;
								return composite;
							}
						};
						dialog.open();
						if (img[0] != null) {
							img[0].dispose();
						}
					});
				}
			});
		});

		item = new ToolItem(toolbar, SWT.CHECK);
		SwtUtils.setToolItemIcon(item, "icons/studio/invokebrowserjs_16x16.png", "Headless build", "Headless build");
		item.addSelectionListener((SelectionListener) e -> {
			headlessBuild = ((ToolItem) e.widget).getSelection();
			try {
				JSONObject device = new JSONObject(FileUtils.readFileToString(devicePref, "UTF-8"));
				device.put("headlessBuild", headlessBuild);
				FileUtils.write(devicePref, device.toString(4), "UTF-8");
			} catch (Exception ex) {
				// TODO: handle exception
			}
			if (headlessBuild) {
				initLoader();
			}
			doLoad();
		});
		item.setSelection(headlessBuild);

		final Menu mBuild = new Menu(toolbar);

		for (NgxBuilderBuildMode mode: NgxBuilderBuildMode.values()) {
			MenuItem menuItem = new MenuItem(mBuild, SWT.NONE);
			menuItem.setText(mode.label());
			menuItem.setToolTipText(mode.description());
			menuItem.setData(mode);
			menuItem.setImage(new Image(parent.getDisplay(), getClass().getResourceAsStream(mode.icon())));
			menuItem.addSelectionListener((SelectionListener) e -> {
				buildItem.setSelection(true);
				buildMode = (NgxBuilderBuildMode) e.widget.getData();
				handleProdBuild();
			});
		}

		buildItem = item = new ToolItem(toolbar, SWT.CHECK);
		SwtUtils.setToolItemIcon(item, "icons/studio/build_prod_b.png", "Build locally", "Build locally");
		item.addSelectionListener((SelectionListener) e -> {
			boolean selected = ((ToolItem) e.widget).getSelection();
			if (!selected) {
				buildMode = null;
				handleProdBuild();
			} else {
				ToolItem it = (ToolItem) e.widget;
				Rectangle rect = it.getBounds();
				Point pt = it.getParent().toDisplay(new Point(rect.x, rect.y));
				mBuild.setLocation(pt);
				mBuild.setVisible(true);
				buildItem.setSelection(false);
			}
		});

		new ToolItem(toolbar, SWT.SEPARATOR);

		item = new ToolItem(toolbar, SWT.CHECK);
		SwtUtils.setToolItemIcon(item, "icons/studio/accumulate.gif", "Toggle auto build", "Toggle auto build");
		item.setSelection(true);
		item.addSelectionListener((SelectionListener) e -> {
			MobileBuilder mb = project.getMobileBuilder();
			mb.setAutoBuild(((ToolItem) e.widget).getSelection());
		});

		item = new ToolItem(toolbar, SWT.PUSH);
		SwtUtils.setToolItemIcon(item, "icons/studio/show_blocks.gif", "Manage modules", "Manage modules");
		item.addSelectionListener((SelectionListener) e -> {
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
		});

		new ToolItem(toolbar, SWT.SEPARATOR);

		item = new ToolItem(toolbar, SWT.DROP_DOWN);
		SwtUtils.setToolItemIcon(item, "icons/studio/cvs_show_history.gif", "Select dataset", "Select dataset");

		SelectionListener selectionListener = e -> {
			setDataset(((MenuItem) e.widget).getText());
		};

		try {
			Image iDataset = plugin.getStudioIcon("icons/studio/cvs_checkin.gif");
			Image iDatasetSelected = plugin.getStudioIcon("icons/studio/cvs_checkout.gif");
			final Menu mDataset = new Menu(toolbar);

			item.addSelectionListener((SelectionListener) e -> {
				for (MenuItem it: mDataset.getItems()) {
					it.dispose();
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

				for (MenuItem it: mDataset.getItems()) {
					if (it.getText().equals(dataset)) {
						it.setImage(iDatasetSelected);
					} else {
						it.setImage(iDataset);
					}
					it.addSelectionListener(selectionListener);
				}

				ToolItem it = (ToolItem) e.widget;
				Rectangle rect = it.getBounds();
				Point pt = it.getParent().toDisplay(new Point(rect.x, rect.y));
				mDataset.setLocation(pt);
				mDataset.setVisible(true);
			});

		} catch (Exception e) {
		}
		item = new ToolItem(toolbar, SWT.PUSH);
		SwtUtils.setToolItemIcon(item, "icons/studio/cvs_add.gif", "Save dataset", "Save dataset");
		item.addSelectionListener((SelectionListener) e -> {
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
		});

		item = new ToolItem(toolbar, SWT.PUSH);
		SwtUtils.setToolItemIcon(item, "icons/studio/cvs_delete.gif", "Remove dataset", "Remove dataset");
		item.addSelectionListener((SelectionListener) e -> {
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
		});
		
		showGrids = item = new ToolItem(toolbar, SWT.CHECK);
		SwtUtils.setToolItemIcon(item, "icons/studio/grid_color_16x16.png", "Show", "Show all grids or current selected");
		item.addSelectionListener((SelectionListener) e -> {
			boolean showGrid = ((ToolItem) e.widget).getSelection();
			C8oBrowser.run(() -> c8oBrowser.executeJavaScriptAndReturnValue("_c8o_showGrids(" + (showGrid ? "true":"false") +")")); 
		});

		for (ToolItem ti: toolbar.getItems()) {
			ti.setData("style", "background-color: unset");
		}
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
	
	private void applySelectedDevice() {
		if (editStyle.getData("deviceWidth") == null) {
			return;
		}
		deviceWidth.setText((String) editStyle.getData("deviceWidth"));
		deviceHeight.setText((String) editStyle.getData("deviceHeight"));
		zoomFactor = (ZoomFactor) editStyle.getData("zoomFactor");
		setDeviceBarVisible((boolean) editStyle.getData("deviceBar.isVisible"));
		editStyle.setData("deviceWidth", null);
		updateBrowserSize();
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
			try {
				c8oBrowser.executeFunctionAndReturnValue("doProgress", progress);
			} catch (Exception e) {
				// silently ignore
			}
		});
		disableEdition();
	}

	private void error(String msg) {
		C8oBrowser.run(() -> {
			try {
				c8oBrowser.executeFunctionAndReturnValue("_c8o_error", msg);
			} catch (Exception e) {
				if (msg != null) {
					try {
						Thread.sleep(1000);
						c8oBrowser.executeFunctionAndReturnValue("_c8o_error", msg);
					} catch (Exception e2) {
						Engine.logStudio.warn("[Error] " + msg);
					}
				}
			}
		});
	}

	private void toast(String msg) {
		Engine.logStudio.info("[Toast] " + msg);
		C8oBrowser.run(() -> {
			c8oBrowser.executeFunctionAndReturnValue("_c8o_toast", msg);
		});
	}

	private void initLoader() {
		try {
			boolean isDark = SwtUtils.isDark();
			c8oBrowser.reset();
			String loader = IOUtils.toString(getClass().getResourceAsStream("loader.html"), "UTF-8");
			if (isDark) {
				loader = loader.replace("lightblue", "rgb(47,47,47); color: white");
			}
			c8oBrowser.setText(loader);
		} catch (Exception e1) {
			throw new RuntimeException(e1);
		}
	}

	public void launchBuilder(boolean forceInstall) {
		launchBuilder(forceInstall, false);
	}

	public void launchBuilder(boolean forceInstall, boolean forceClean) {
		final int buildCount = ++this.buildCount;

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
			initLoader();
			
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
					if (forceClean) {
						appendOutput("...", "...", "Removing existing node_modules... This can take several seconds...");
						Engine.logStudio.info("Removing existing node_modules... This can take several seconds...");
						com.twinsoft.convertigo.engine.util.FileUtils.deleteQuietly(nodeModules);
					}
					appendOutput("Installing node_modules... This can take several minutes depending on your network connection speed...");
					Engine.logStudio.info("Installing node_modules... This can take several minutes depending on your network connection speed...");

					if (!nodeModules.exists()) {
						File packageLockTpl = new File(ionicDir, "package-lock-tpl.json");
						if (packageLockTpl.exists()) {
							com.twinsoft.convertigo.engine.util.FileUtils.copyFile(packageLockTpl, new File(ionicDir, "package-lock.json"));
						}
					}

					ProcessBuilder pb = ProcessUtils.getNpmProcessBuilder(path + File.pathSeparator + ionicDir.toString() , "npm", "install",
							"--legacy-peer-deps", "--loglevel", "info", "--ssl-key=" + new File(project.getDirFile(), "DisplayObjects/mobile").getAbsolutePath());
					pb.redirectErrorStream(true);
					pb.directory(ionicDir);
					Process p = pb.start();

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
				int port = (Math.abs(ionicDir.getAbsolutePath().hashCode()) % 10000) + 40000;
				cmd.add("--");
				usedPort.clear();
				portNode = NetworkUtils.nextAvailable(port, usedPort);
				cmd.add("--port="+ portNode);
				cmd.add("--host=0.0.0.0");
			}

			// #183 add useless option to help terminateNode method to find the current path
			cmd.add("--ssl-key=" + new File(project.getDirFile(), "DisplayObjects/mobile").getAbsolutePath());

			pb.redirectErrorStream(true);
			pb.directory(ionicDir);
			
			String angular_json = FileUtils.readFileToString(new File(ionicDir, "angular.json"), "UTF-8");
			angular_json = angular_json.replaceFirst("(\"serve\":\s*\\{).*", "$1");
			FileUtils.write(new File(ionicDir, "angular.json"), angular_json, "UTF-8");
			
			Process p = pb.start();
			processes.add(p);


			Matcher matcher = Pattern.compile("(\\d+)% (.*)").matcher("");
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;

			StringBuilder sb = null;

			while ((line = br.readLine()) != null) {
				line = pRemoveEchap.matcher(line).replaceAll("");
				if (StringUtils.isNotBlank(line)) {
					Engine.logStudio.info(line);
					if (line.startsWith("Error: ")) {
						sb = new StringBuilder();
					}
					if (sb != null) {
						if (line.contains("Failed to compile.")) {
							sb.append(line);
							error(sb.toString());
							sb = null;
						} else {
							sb.append(line + "\n");
						}
					}

					matcher.reset(line);
					if (matcher.find()) {
						progress(Integer.parseInt(matcher.group(1)));
						appendOutput(matcher.group(2));
					} else {
						appendOutput(line);
					}
					if (line.matches(".*Compiled .*successfully.*")) {
						progress(100);
						error(null);
						synchronized (mutex) {
							mutex.notify();
						}
						mb.buildFinished();
					}

					Matcher m = pIsBrowserOpenable.matcher(line);
					if (m.matches()) {
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
					boolean done = false;
					if (pagePath != null) {
						try {
							c8oBrowser.executeFunctionAndReturnValue("_c8o_remove_all_overlay");
							done = "done".equals(c8oBrowser.executeFunctionAndReturnValue("_c8o_changePage", pagePath));
						} catch (Exception e) {
						}
					}
					if (!done) {
						c8oBrowser.loadURL(url);
					}
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
	private MobileComponent dragStartMobileComponent = null;

	private void highlightPoint(int x, int y) {
		Node node = browser.mainFrame().get().inspect(x, y).node().get();
		highlightPoint(node);
	}

	private void highlightPoint(Node node) {
		while (!(node == null || node instanceof Element)) {
			node = node.parent().orElse(null);
		}

		if (node == null) {
			return;
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
					highlightPriority(priority);
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
	
	private void highlightPriority(long priority) throws Exception {
		var databaseObject = findDatabaseObject(priority);
		if (databaseObject == null) {
			return;
		}
		if (databaseObject instanceof MobileComponent && !databaseObject.equals(exHighlightMobileComponent)) {
			if (dragStartMobileComponent != null) {
				var ancestor = databaseObject;
				while (dragStartMobileComponent != ancestor && ancestor != null) {
					ancestor = ancestor.getParent();
				}
				if (dragStartMobileComponent == ancestor) {
					return;
				}
			}
			c8oBrowser.getDisplay().asyncExec(() -> ConvertigoPlugin.getDefault().getProjectExplorerView().objectSelected(new CompositeEvent(databaseObject)));
			highlightComponent(exHighlightMobileComponent = (MobileComponent) databaseObject, false);
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
		});
		C8oBrowser.run(() -> {
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
	
	private DatabaseObject findDatabaseObject(long priority) throws Exception {
		try {
			var alreadyWalked =  new HashSet<DatabaseObject>();

			new WalkHelper() {

				@Override
				protected void walk(DatabaseObject databaseObject) throws Exception {
					if (databaseObject instanceof UISharedComponent) {
						var uisc = (UISharedComponent) databaseObject;
						if (uisc != null) {
							databaseObject = uisc;
						}
					} else if (databaseObject instanceof UIUseShared) {
						var uisc = ((UIUseShared) databaseObject).getTargetSharedComponent();
						if (uisc != null) {
							databaseObject = uisc;
						}
					} else if (databaseObject instanceof UIDynamicInvoke) {
						var uidi = (UIDynamicInvoke) databaseObject;
						var uisa = uidi.getTargetSharedAction();
						if (uisa != null) {
							if (!uidi.isRecursive()) {
								databaseObject = uisa;
							}
						}
					}

					if (databaseObject.priority == priority) {
						throw new DatabaseObjectFoundException(databaseObject);
					}
					if(!alreadyWalked.contains(databaseObject)) {
						alreadyWalked.add(databaseObject);
						super.walk(databaseObject);
					}

				}

			}.init(applicationEditorInput.application);
		} catch (DatabaseObjectFoundException e) {
			return e.getDatabaseObject();
		}
		return null;
	}

	private void doReload() {
		showGrids.setSelection(false);
		editStyle.setSelection(false);
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
		String projectName = new File(project.getDirPath()).getName();
		int retry = 10;
		try {
			while (retry-- > 0) {
				int code;
				if (Engine.isWindows()) {
					String prod = prodOnly ? " -and $_.CommandLine -like '*--watch*'" : "";
					var process = new ProcessBuilder("powershell", "-Command",
						"Get-WmiObject Win32_Process | Where-Object { $_.Name -eq 'node.exe' -and $_.CommandLine -like '*\\" + projectName + "\\DisplayObjects\\*' " + prod + " } | ForEach-Object { $_.Terminate() }"
					).redirectError(Redirect.DISCARD).redirectOutput(Redirect.DISCARD).start();
					code = process.waitFor();
				} else {
					String prod = prodOnly ? " | grep -e \"--watch\" -e \":watch\"" : "";
					Process process = new ProcessBuilder("/bin/bash", "-c",
							"ps -e" + (Engine.isLinux() ? "f" : "") + " | grep -v \"sed -n\"" + prod + " | sed -n -E \"s,[^0-9]*([0-9]+).*(node|npm|ng).*/"+ projectName + "/DisplayObjects/.*,\\1,p\" | xargs kill"
							).redirectError(Redirect.DISCARD).redirectOutput(Redirect.DISCARD).start();
					code = process.waitFor();
				}
				if (code == 0) {
					retry = 0;
				}
			}
			synchronized (usedPort) {
				usedPort.remove(portNode);
			}
		} catch (Exception e) {
			Engine.logStudio.warn("Failed to terminate the node server", e);
		}
	}

	@Override
	public void onPackageUpdated() {
		launchBuilder(true, false);
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
		boolean[] terminated = { false };
		MobileBuilder mb = project.getMobileBuilder();
		if (buildMode.equals(NgxBuilderBuildMode.prod)) {
			mb.setAppBuildMode(MobileBuilderBuildMode.production);
		} else {
			mb.setAppBuildMode(MobileBuilderBuildMode.fast);
		}

		String endPointUrl = applicationEditorInput.application.getParent().getComputedEndpoint();
		String baseHref = "/convertigo/projects/"+ project.getName() +"/DisplayObjects/mobile/";
		try {
			baseHref = (endPointUrl.isEmpty() ? "/convertigo": endPointUrl.replaceFirst("https?://.*?/", "/").replaceFirst("(/.*)/.*?$", "$1")) + 
					"/projects/"+ project.getName() +"/DisplayObjects/mobile/";
		} catch (Exception e) {}

		final String appBaseHref = baseHref;

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
				// #183 add useless option to help terminateNode method to find the current path
				cmd.add("--output-path=" + new File(project.getDirFile(), "DisplayObjects/mobile").getAbsolutePath());
				// #393 add base href for project's web app
				cmd.add("--base-href="+ appBaseHref);

				pb.redirectErrorStream(true);
				pb.directory(ionicDir);
				Process p = pb.start();
				processes.add(p);
				BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String line;

				Matcher matcher = Pattern.compile("(\\d+)% (.*)").matcher("");
				int lastProgress = 0;
				Engine.execute(() -> {
					while (!monitor.isCanceled() && !terminated[0]) {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					if (!terminated[0]) {
						terminateNode(true);
					}
				});
				while (!monitor.isCanceled() && (line = br.readLine()) != null) {
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
						Engine.logStudio.debug(line);
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
			terminated[0] = true;
		});
		prodJob.schedule();
	}

	public String getCurrentUrl() {
		return c8oBrowser != null ? c8oBrowser.getURL() : "";
	}

	public boolean check(String check) throws Exception {
		if (c8oBrowser == null) {
			return false;
		}
		boolean [] ok = {false};
		Exception [] ex = {null};
		C8oBrowser.run(() -> {
			try {
				ok[0] = c8oBrowser.executeJavaScriptAndReturnValue(check);
			} catch (Exception e) {
				ex[0] = e;
			}
		});
		if (ex[0] != null) {
			throw ex[0];
		}
		return ok[0];
	}
	
	private void disableEdition() {
		editStyle.getDisplay().syncExec(() -> {
			if (editStyle.getSelection()) {
				editStyle.setSelection(false);
				Event event = new Event();
				event.widget = editStyle;
				event.type = SWT.Selection;
				editStyle.notifyListeners(SWT.Selection, event);
			}
		});
	}
}
