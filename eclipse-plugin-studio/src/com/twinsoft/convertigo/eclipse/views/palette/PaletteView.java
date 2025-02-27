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

package com.twinsoft.convertigo.eclipse.views.palette;

import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.e4.ui.css.swt.dom.CompositeElement;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

import com.twinsoft.convertigo.beans.BeansUtils;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.ngx.components.IExposeAble;
import com.twinsoft.convertigo.beans.ngx.components.IShared;
import com.twinsoft.convertigo.beans.ngx.components.UISharedComponent;
import com.twinsoft.convertigo.beans.ngx.components.dynamic.Component;
import com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.dnd.PaletteSource;
import com.twinsoft.convertigo.eclipse.dnd.PaletteSourceTransfer;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowser;
import com.twinsoft.convertigo.eclipse.swt.SwtUtils;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectEvent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectListener;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.MobileApplicationComponentTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.NgxApplicationComponentTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ObjectsFolderTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.SequenceTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.dbo_explorer.DboBean;
import com.twinsoft.convertigo.engine.dbo_explorer.DboBeans;
import com.twinsoft.convertigo.engine.dbo_explorer.DboCategory;
import com.twinsoft.convertigo.engine.dbo_explorer.DboGroup;
import com.twinsoft.convertigo.engine.util.CachedIntrospector;
import com.twinsoft.convertigo.engine.util.RegexpUtils;

public class PaletteView extends ViewPart implements IPartListener2, ISelectionListener, TreeObjectListener {
	private static final int MAX_USED_HISTORY = 50;
	private static final int MAX_USED_VISIBLE = 8;

	private Composite parent, topBag, bag;
	private Control lastUsedlabel, favoriteslabel;
	private Cursor handCursor;
	private Text searchText;
	private Map<String, Image> imageCache = new HashMap<>();
	private HashMap<String, Item> all = new HashMap<>();
	private HashMap<String, Item> commons = new HashMap<>();
	private Project selectedProject = null;
	private boolean isVisible = true, isCtrl = false, isType = false;
	private ISelectionChangedListener selectionListener;
	private Set<String> hiddenCategories;
	private MouseListener mouseListener;
	private DragSourceAdapter dragListener;
	private MakeItem makeItem;
	private ComponentManager latestComponentManager;

	private abstract class Item implements Comparable<Item> {
		private String shortDescription;
		private String longDescription;
		private String searchText;
		private DatabaseObject dbo;

		Item() {
			String[] beanDescriptions = description().split("\\|");
			shortDescription = BeansUtils.cleanDescription(beanDescriptions.length >= 1 ? beanDescriptions[0] : "n/a", true);
			longDescription = BeansUtils.cleanDescription(beanDescriptions.length >= 2 ? beanDescriptions[1] : "n/a", true);
			searchText = name().toLowerCase() + " " + shortDescription.toLowerCase();
		}

		private String shortDescription() {
			return shortDescription;
		}

		private String longDescription() {
			return longDescription;
		}

		private String searchText() {
			return searchText;
		}

		@Override
		public int compareTo(Item i) {
			if (i == null) {
				return -1;
			}
			var r = Boolean.compare(!builtIn(), !i.builtIn());
			if (r == 0) {
				r = category().compareToIgnoreCase(i.category());
				if (r == 0) {
					r = name().compareToIgnoreCase(i.name());
					if (r == 0) {
						r = id().compareToIgnoreCase(i.id());
					}
					return r;
				}
			}
			if (r == 0) {
				r = 1;
			}
			return r;
		}

		private boolean allowedIn(int folderType) {
			return folderType == ProjectExplorerView.getDatabaseObjectType(databaseObject());
		}

		protected boolean builtIn() {
			return true;
		}

		private DatabaseObject databaseObject() {
			return dbo == null ? dbo = newDatabaseObject() : dbo; 
		}

		abstract Image image();
		abstract String id();
		abstract String name();
		abstract String category();
		abstract String description();
		abstract String propertiesDescription();
		abstract DatabaseObject newDatabaseObject();
		abstract boolean allowedIn(DatabaseObject parent);
	}

	private interface MakeItem {
		Control make(Composite parent, Item item);
	}

	private interface MakeLabel {
		Control make(Composite parent, String txt);
	}

	private interface UpdateLabel {
		void update(CLabel label);
	}

	public PaletteView() {
	}

	@Override
	public void dispose() {
		getSite().getPage().removeSelectionListener(this);
		getSite().getPage().removePartListener(this);
		var pev = ConvertigoPlugin.getDefault().getProjectExplorerView();
		if (pev != null) {
			pev.removeTreeObjectListener(this);
		}

		handCursor.dispose();
		for (Image image: imageCache.values()) {
			image.dispose();
		}
		imageCache.clear();
		super.dispose();
	}

	private Image getImage(String imagePath) {
		Image image = null;
		if (imagePath == null) {
		} else if (imagePath.startsWith("/com/twinsoft/convertigo/")) {
			try {
				image = ConvertigoPlugin.getDefault().getIconFromPath(imagePath, BeanInfo.ICON_COLOR_32x32);
			} catch (IOException e) {
			}
		} else if (imageCache.containsKey(imagePath)) {
			image = imageCache.get(imagePath);
		} else {
			try {
				image = new Image(handCursor.getDevice(), imagePath);
				imageCache.put(imagePath, image);
			} catch (Exception e) {
				System.out.println("Cannot load image " + imagePath);
			}
		}
		return image;
	}

	@Override
	public void createPartControl(Composite parent) {
		handCursor = new Cursor(parent.getDisplay(), SWT.CURSOR_HAND);
		this.parent = parent;
		parent.setLayout(new GridLayout(1, true));
		init();
		//		ConvertigoPlugin.runAtStartup(() -> refresh(1));
		getSite().getPage().addSelectionListener(this);
		getSite().getPage().addPartListener(this);
		isVisible = getSite().getPage().isPartVisible(this);
	}

	private void init() {
		Deque<Item> lastUsed = new LinkedList<>();
		Set<Item> favorites = new TreeSet<>();
		hiddenCategories = new HashSet<>();
		isCtrl = false;
		isType = "type".equals(ConvertigoPlugin.getProperty("palette.link"));

		GridLayout gl;
		GridData gd;
		RowLayout rl;

		Composite top = new Composite(parent, SWT.NONE);
		top.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		top.setLayout(new GridLayout(4, false));

		ToolBar bar = new ToolBar(top, SWT.NONE);
		ToolItem tiLink = new ToolItem(bar, SWT.CHECK);
		SwtUtils.setToolItemIcon(tiLink, "icons/studio/resize_connector.gif", "Link", "Link with the 'Projects tree' selection");
		tiLink.setData("style", "background: unset");
		tiLink.setSelection(!"off".equals(ConvertigoPlugin.getProperty("palette.link")));
		if (isType) {
			ConvertigoPlugin.asyncExec(() -> tiLink.setBackground(tiLink.getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION)));
		}

		bar = new ToolBar(top, SWT.NONE);
		ToolItem tiInternal = new ToolItem(bar, SWT.CHECK);
		SwtUtils.setToolItemIcon(tiInternal, "icons/studio/convertigo_logo_16x16.png", "Internal", "Built-in objects visibility");
		tiInternal.setData("style", "background: unset");
		tiInternal.setSelection(!"off".equals(ConvertigoPlugin.getProperty("palette.internal")));

		ToolItem tiShared = new ToolItem(bar, SWT.CHECK);
		tiShared.setToolTipText("Shared objects visibility");
		tiShared.setData("style", "background: unset");
		try {
			tiShared.setImage(ConvertigoPlugin.getDefault().getBeanIcon(CachedIntrospector.getBeanInfo(UISharedComponent.class), BeanInfo.ICON_COLOR_16x16));
		} catch (Exception e3) {
			tiShared.setText("Shared");
		}
		tiShared.setSelection(!"off".equals(ConvertigoPlugin.getProperty("palette.shared")));

		bar = new ToolBar(top, SWT.NONE);

		ToolItem fav = new ToolItem(bar, SWT.PUSH);

		searchText = new Text(top, SWT.BORDER | SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL);
		searchText.setMessage("Search…");
		searchText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		searchText.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.character == SWT.ESC) {
					e.doit = false;
					if (!searchText.getText().isEmpty()) {
						searchText.setText("");
					}
				}
			}
		});

		selectionListener = new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent e) {
				if (tiLink.isDisposed()) {
					selectionListener = null;
					refresh();
					return;
				}

				if (!tiLink.getSelection() || !isVisible) {
					return;
				}

				TreeSelection selection = (TreeSelection) e.getSelection();
				if (selection.getFirstElement() instanceof TreeObject to) {
					while (to != null) {
						Integer folderType = null;
						DatabaseObject selected = null;
						Object parent = null;
						Boolean clear = null;

						if (isType) {
							TreeObject ttype = to;
							while (ttype != null) {
								if (ttype instanceof SequenceTreeObject ||
										ttype instanceof NgxApplicationComponentTreeObject ||
										ttype instanceof MobileApplicationComponentTreeObject) {
									to = ttype;
									break;
								}
								ttype = ttype.getParent();
							}
						}

						if (to instanceof ObjectsFolderTreeObject) {
							ObjectsFolderTreeObject folder = (ObjectsFolderTreeObject) to;
							Integer last = (Integer) PaletteView.this.parent.getData("FolderType");
							try  {
								folderType = ProjectExplorerView.getDatabaseObjectType((DatabaseObject) folder.getFirstChild().getObject());
							} catch (Exception e2) {
								folderType = 0;
							}
							parent = folder.getParent() == null ? null : folder.getParent().getObject();
							clear = last == null || last != folderType;
						} else if (to instanceof DatabaseObjectTreeObject) {
							DatabaseObjectTreeObject dbot = (DatabaseObjectTreeObject) to;
							DatabaseObject last = (DatabaseObject) PaletteView.this.parent.getData("Selected");
							selected = dbot.getObject();
							parent = selected.getParent();
							clear = last == null || !last.getClass().equals(dbot.getObject().getClass());
						}
						if (clear != null) {
							PaletteView.this.parent.setData("FolderType", folderType);
							PaletteView.this.parent.setData("Selected", selected);
							PaletteView.this.parent.setData("Parent", parent);
							if (selected != null || parent instanceof DatabaseObject) {
								var project = (selected != null ? selected : (DatabaseObject) parent).getProject();
								if (project != selectedProject) {
									selectedProject = project;
									refresh();
									break;
								}
							} else {
								selectedProject = null;
							}
							if (clear == true) {
								searchText.setText("");
							} else {
								searchText.notifyListeners(SWT.Modify, new Event());
							}
							break;
						}
						to = to.getParent();
					}
				}
			}
		};

		tiLink.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String val;
				if (!tiLink.getSelection() && "on".equals(ConvertigoPlugin.getProperty("palette.link"))) {
					tiLink.setSelection(true);
					tiLink.setBackground(tiLink.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
					val = "type";
					isType = true;
				} else {
					tiLink.setBackground(null);
					val = tiLink.getSelection() ? "on" : "off";
					isType = false;
				}
				ConvertigoPlugin.setProperty("palette.link", val);
				update();
			}
		});

		var pev = ConvertigoPlugin.getDefault().getProjectExplorerView();
		if (pev != null) {
			pev.addTreeObjectListener(this);
		}

		//		if (selectedProject == null) {
		//			CompositeElement.getEngine(top).applyStyles(top, true);
		//			parent.layout(true);
		//			update();
		//			return;
		//		}

		SelectionListener tiListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ToolItem ti = (ToolItem) e.widget;
				if (!ti.getSelection()) {
					if (ti == tiShared) {
						tiInternal.setSelection(true);
					} else {
						tiShared.setSelection(true);
					}
				}
				searchText.notifyListeners(SWT.Modify, new Event());
			}
		};
		tiInternal.addSelectionListener(tiListener);
		tiShared.addSelectionListener(tiListener);

		SashForm sash = new SashForm(parent, SWT.HORIZONTAL);
		sash.setLayoutData(new GridData(GridData.FILL_BOTH));
		RGB rgb = parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND).getRGB();
		sash.setData("style", "background-color: rgb(" + rgb.red + ", " + rgb.green + ", " + rgb.blue + ")");

		Composite left = new Composite(sash, SWT.NONE);
		left.setLayout(gl = new GridLayout(1, true));
		gl.marginHeight = gl.marginWidth = gl.verticalSpacing = 0;

		topBag = new Composite(left, SWT.NONE);
		topBag.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		topBag.setLayout(rl = new RowLayout());
		rl.marginTop = rl.marginRight = rl.marginLeft = 0;

		Composite border = new Composite(left, SWT.NONE);
		border.setLayoutData(gd = new GridData(GridData.FILL_HORIZONTAL));
		gd.heightHint = 1;
		border.setData("style", "background-color: rgb(0, 200, 247)");

		ScrolledComposite scroll = new ScrolledComposite(left, SWT.V_SCROLL);
		scroll.setData("style", "color: inherit; background-color: inherit");
		scroll.setExpandVertical(true);
		scroll.setExpandHorizontal(true);
		scroll.setLayoutData(new GridData(GridData.FILL_BOTH));

		bag = new Composite(scroll, SWT.NONE);
		bag.setData("style", "color: inherit; background-color: inherit");
		scroll.setContent(bag);
		bag.setLayout(rl = new RowLayout());
		rl.marginTop = rl.marginRight = rl.marginBottom = rl.marginLeft = 0;

		Composite right = new Composite(sash, SWT.NONE);
		right.setLayout(gl = new GridLayout(1, false));

		Runnable updateFav = () -> {
			Control latestSelected = (Control) bag.getData("LatestSelected");
			fav.setEnabled(false);
			if (latestSelected != null && !latestSelected.isDisposed()) {
				Item item = (Item) latestSelected.getData("Item");
				if (item != null) {
					fav.setEnabled(true);
					if (favorites.contains(item)) {
						fav.setToolTipText("Remove from favorite");
						try {
							fav.setImage(ConvertigoPlugin.getDefault().getStudioIcon("icons/studio/star_16x16.png"));
						} catch (IOException e1) {
						}
						return;
					}
				}
			}

			fav.setToolTipText("Add to favorite");
			try {
				fav.setImage(ConvertigoPlugin.getDefault().getStudioIcon("icons/studio/unstar_16x16.png"));
			} catch (IOException e1) {
			}
		};

		updateFav.run();

		fav.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				Control latestSelected = (Control) bag.getData("LatestSelected");
				if (latestSelected != null && !latestSelected.isDisposed()) {
					Item item = (Item) latestSelected.getData("Item");
					if (item != null) {
						if (favorites.contains(item)) {
							favorites.remove(item);
						} else {
							favorites.add(item);
						}
						String str = favorites.stream().map(Item::id).collect(Collectors.joining(","));
						ConvertigoPlugin.setProperty("palette.favorites", str);
						searchText.notifyListeners(SWT.Modify, new Event());
					}
				}
				fav.setToolTipText("Remove from favorite");
				try {
					fav.setImage(ConvertigoPlugin.getDefault().getStudioIcon("icons/studio/star_16x16.png"));
				} catch (IOException e1) {
				}
				updateFav.run();
			}
		});

		C8oBrowser browser = new C8oBrowser(right, SWT.NONE);
		browser.setLayoutData(new GridData(GridData.FILL_BOTH));
		browser.setUseExternalBrowser(true);

		Runnable updateDoc = () -> {
			var item = (Item) null;
			var latestSelected = (Control) bag.getData("LatestSelected");

			if (latestSelected != null && !latestSelected.isDisposed()) {
				item = (Item) latestSelected.getData("Item");
			};

			if (item == null || sash.getWeights()[1] < 100) {
				browser.setText("<html>" +
						"<head>" +
						"<script type=\"text/javascript\">" +
						"document.oncontextmenu = new Function(\"return false\");" +
						"</script>" +
						"<style type=\"text/css\">" +
						"html {" +
						"padding: 0px; margin: 0px;" + 
						"border-left: lightgrey solid 2px;" +
						"}\n" + 
						"body {" +
						"font-family: Courrier new, sans-serif;" +
						"font-size: 14px;" +
						"padding-left: 5px;" +
						"color: $foreground$;" +
						"background-color: $background$ } \n" +
						"a { color: $link$; }" +
						"</style>" +
						"</head><body></body></html>");
				bag.setData("LatestDoc", null);
			} else {
				var lastItem = (Item) bag.getData("LatestDoc");
				if (item.compareTo(lastItem) != 0) {
					var propertiesDescription = item.propertiesDescription();
					browser.setText("<html>" +
							"<head>" +
							"<script type=\"text/javascript\">" +
							"document.oncontextmenu = new Function(\"return false\");" +
							"</script>" +
							"<style type=\"text/css\">" +
							"@import url('https://fonts.googleapis.com/css2?family=Inter:ital,opsz,wght@0,14..32,100..900;1,14..32,100..900&display=swap');\n" +
							"html {" +
							"padding: 0px; margin: 0px;" + 
							"border-left: lightgrey solid 2px;" +
							"}\n" + 
							"body {" +
							"font-family: 'Inter', sans-serif;" +
							"padding-left: 0.3em;" +
							"color: $foreground$;" +
							"background-color: $background$ } \n" +
							"a { color: $link$; }" +
							"li { margin-bottom: 10px }" +
							"</style>" +
							"</head><body><p>"
							+ "<b style=\"font-size: 24pt\">" + item.name() + "</b>" + "<br><br>"
							+ "<i>" + item.shortDescription() + "</i>" + "<br><br>"
							+ item.longDescription() + "<br>"
							+ (propertiesDescription.isEmpty() ? "" : "<h4><b>Properties:</b></h4>")
							+ propertiesDescription
							+ "</p></body></html>");
					bag.setData("LatestDoc", item);
				}
			}
		};

		mouseListener = new MouseAdapter() {

			@Override
			public void mouseDown(MouseEvent e) {
				Control c = (Control) e.widget;

				Item item = (Item) c.getData("Item");
				if (item == null) {
					return;
				}
				Control latestSelected = (Control) bag.getData("LatestSelected");
				if (latestSelected != null && !latestSelected.isDisposed()) {
					latestSelected.setData("style", "color: inherit; background-color: inherit");
					CompositeElement.getEngine(latestSelected).applyStyles(latestSelected, false);
				}
				bag.setData("LatestSelected", c);
				c.setData("style", "color: blue; background-color: lightcyan");
				CompositeElement.getEngine(c).applyStyles(c, false);

				updateDoc.run();
				updateFav.run();
			}

		};

		try {
			all.clear();
			if (commons.isEmpty()) {
				for (DboGroup g: Engine.theApp.getDboExplorerManager().getGroups()) {
					String groupName = g.getName();
					for (DboCategory c: g.getCategories()) {
						String categoryName = c.getName().isEmpty() ? groupName : c.getName();

						for (DboBeans bs: c.getBeans()) {
							String beansName = bs.getName().isEmpty() ? categoryName : bs.getName();
							for (DboBean b: bs.getBeans()) {
								String cn = b.getClassName();
								if (cn.startsWith("com.twinsoft.convertigo.beans.mobile.components.")
										&& !cn.endsWith("PageComponent")) {
									continue;
								}
								if (cn.startsWith("com.twinsoft.convertigo.beans.ngx.components.")) {
									continue;
								}
								String category = beansName;
								String beanInfoClassName = b.getClassName() + "BeanInfo";
								Class<?> beanInfoClass = Class.forName(beanInfoClassName);
								BeanInfo bi = (BeanInfo) beanInfoClass.getConstructor().newInstance();
								BeanDescriptor bd = bi.getBeanDescriptor();
								String description = b.isDocumented() ? bd.getShortDescription() : "Not yet documented |";
								String i = cn;
								for (int j = 1; all.containsKey(i);j++) {
									i = cn + j;
								}
								String id = i;
								Class<?> cls = Class.forName(cn);
								Constructor<?> constructor = cls.getConstructor();
								all.put(id, new Item() {

									@Override
									public String category() {
										return category;
									}

									@Override
									public String name() {
										return bd.getDisplayName();
									}

									@Override
									String description() {
										return description;
									}

									@Override
									Image image() {
										try {
											return ConvertigoPlugin.getDefault().getBeanIcon(bi, BeanInfo.ICON_COLOR_32x32);
										} catch (IntrospectionException e) {
											return null;
										}
									}

									@Override
									DatabaseObject newDatabaseObject() {
										try {
											return (DatabaseObject) constructor.newInstance();
										} catch (RuntimeException e) {
											throw e;
										} catch (Exception e) {
											throw new RuntimeException(e);
										}
									}

									@Override
									boolean allowedIn(DatabaseObject parent) {
										try {
											boolean force = false;
											if (isType) {
												String cls = b.getClassName();
												if (parent instanceof Sequence) {
													force = cls.startsWith("com.twinsoft.convertigo.beans.steps.")
															|| cls.startsWith("com.twinsoft.convertigo.beans.variables.Step");
												} else if (parent instanceof ApplicationComponent) {
													force = cls.startsWith("com.twinsoft.convertigo.beans.ngx.");
												} else if (parent instanceof com.twinsoft.convertigo.beans.mobile.components.ApplicationComponent) {
													force = cls.startsWith("com.twinsoft.convertigo.beans.mobile.");
												}
											}
											return force || DatabaseObjectsManager.checkParent(parent.getClass(), b);
										} catch (Exception e) {
											return false;
										}
									}

									@Override
									String propertiesDescription() {
										PropertyDescriptor[] propertyDescriptors = bi.getPropertyDescriptors();

										propertyDescriptors = propertyDescriptors.clone();
										Arrays.sort(propertyDescriptors, (o1, o2) -> {
											if (o1.isExpert() == o2.isExpert()) {
												return o1.getDisplayName().compareToIgnoreCase(o2.getDisplayName());
											} else if (o1.isExpert()) {
												return 1;
											} else {
												return -1;
											}
										});

										String propertiesDescription = "";
										for (PropertyDescriptor dbopd : propertyDescriptors) {
											if (!dbopd.isHidden()) {
												propertiesDescription += "<li><i>"+ dbopd.getDisplayName() +"</i>" ;
												propertiesDescription += "</br>"+ dbopd.getShortDescription().replace("|", "") +"</li>";
											}
										}
										return propertiesDescription.isEmpty() ? "": "<ul>"+propertiesDescription+"</ul>";
									}

									@Override
									String id() {
										return id;
									}
								});
							}
						}
					}
				}

				for (com.twinsoft.convertigo.beans.mobile.components.dynamic.Component comp: com.twinsoft.convertigo.beans.mobile.components.dynamic.ComponentManager.getComponentsByGroup()) {
					String id = "mb " + comp.getGroup() + "] " + comp.getName();
					all.put(id, new Item() {

						@Override
						public String category() {
							return comp.getGroup();
						}

						@Override
						public String name() {
							return comp.getLabel();
						}

						@Override
						String description() {
							return comp.getDescription();
						}

						@Override
						Image image() {
							return getImage(comp.getImagePath());
						}

						@Override
						DatabaseObject newDatabaseObject() {
							return com.twinsoft.convertigo.beans.mobile.components.dynamic.ComponentManager.createBean(comp);
						}

						@Override
						boolean allowedIn(DatabaseObject parent) {
							if (isType) {
								return parent instanceof com.twinsoft.convertigo.beans.mobile.components.ApplicationComponent;
							}
							return comp.isAllowedIn(parent);
						}

						@Override
						String propertiesDescription() {
							return comp.getPropertiesDescription();
						}

						@Override
						String id() {
							return id;
						}
					});
				}

				commons.putAll(all);
			} else {
				all.putAll(commons);
			}

			dragListener = new DragSourceAdapter() {
				@Override
				public void dragStart(DragSourceEvent event) {
					try {
						Item item = (Item) ((DragSource) event.widget).getControl().getData("Item");
						DatabaseObject dbo = item.newDatabaseObject();
						dbo.priority = dbo.getNewOrderValue();
						event.doit = true;
						PaletteSourceTransfer.getInstance().setPaletteSource(new PaletteSource(dbo));
						dragSetData(event);
					} catch (Exception e) {
						ConvertigoPlugin.logException(e, "Cannot drag");
					}
				}

				@Override
				public void dragFinished(DragSourceEvent event) {
					Item item = (Item) ((DragSource) event.widget).getControl().getData("Item");
					if (lastUsed.isEmpty() || !item.equals(lastUsed.getFirst())) {
						lastUsed.removeFirstOccurrence(item);
						lastUsed.addFirst(item);
						while (lastUsed.size() > MAX_USED_HISTORY) {
							lastUsed.pollLast();
						}
						String str = lastUsed.stream().map(Item::id).collect(Collectors.joining(","));
						ConvertigoPlugin.setProperty("palette.history", str);
					}
				}

				@Override
				public void dragSetData(DragSourceEvent event) {
					event.data = PaletteSourceTransfer.getInstance().getPaletteSource().getXmlData();
				}
			};

			String pref = ConvertigoPlugin.getProperty("palette.favorites");
			if (StringUtils.isNotBlank(pref)) {
				for (String h: pref.split(",")) {
					if (all.containsKey(h)) {
						favorites.add(all.get(h));
					}
				}
			}

			pref = ConvertigoPlugin.getProperty("palette.history");
			if (StringUtils.isNotBlank(pref)) {
				for (String h: pref.split(",")) {
					if (all.containsKey(h)) {
						lastUsed.add(all.get(h));
					}
				}
			}

			pref = ConvertigoPlugin.getProperty("palette.hiddenCategories");
			if (StringUtils.isNotBlank(pref)) {
				hiddenCategories.clear();
				for (String h: pref.split(",")) {
					hiddenCategories.add(h);
				}
			}

			bag.addControlListener(new ControlListener() {
				@Override
				public void controlResized(ControlEvent e) {
					ConvertigoPlugin.asyncExec(() -> {
						int min = -1;
						Control[] children = bag.getChildren();
						for (int i = children.length - 1; i >= 0; i--) {
							if (children[i].isVisible()) {
								Rectangle r = children[i].getBounds();
								min = r.y + r.height;
								break;
							}
						}
						scroll.setMinHeight(min);
					});
				}

				@Override
				public void controlMoved(ControlEvent e) {
				}
			});

			searchText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					String text = searchText.getText().toLowerCase();

					DatabaseObject selected = (DatabaseObject) PaletteView.this.parent.getData("Selected");
					DatabaseObject parent = (DatabaseObject) PaletteView.this.parent.getData("Parent");
					Integer folderType = (Integer) PaletteView.this.parent.getData("FolderType");

					var skipKey = text + ":"
							+ (selected != null ? selected.getClass().getCanonicalName() : folderType != null ? folderType.toString() : "null") + ":"
							+ (selectedProject != null ? selectedProject.getName() : "null") + ":"
							+ hiddenCategories + ":"
							+ favorites.stream().map(i -> i.name()).collect( Collectors.joining( "," )) + ":"
							+ lastUsed.stream().map(i -> i.name()).collect( Collectors.joining( "," ));
					var lastSkipKey = (String) bag.getData("LastSkipKey");
					if (skipKey.equals(lastSkipKey)) {
						return;
					} else {
						bag.setData("LastSkipKey", skipKey);
					}

					favoriteslabel.setEnabled(text.isEmpty());
					lastUsedlabel.setEnabled(text.isEmpty());

					Control headerLabel = null;
					boolean empty = true;
					for (Control c: bag.getChildren()) {
						Item item = (Item) c.getData("Item");
						boolean ok = false;
						if (item != null) {
							ok = (tiInternal.getSelection() && item.builtIn()) || (tiShared.getSelection() && !item.builtIn());
							if (selected != null) {
								ok = ok && item.allowedIn(selected);
							} else {
								ok = ok && folderType != null && item.allowedIn(folderType) && item.allowedIn(parent);
							}
							if (empty && ok) {
								empty = false;
							}
							ok = ok && (text.isEmpty() || item.searchText().contains(text));
							c.setData("Ok", ok);
							if (ok) {
								if (headerLabel != null && headerLabel.getData("Label").equals(item.category())) {
									headerLabel.setVisible(true);
									((RowData) headerLabel.getLayoutData()).exclude = false;
									ok = !text.isEmpty() || headerLabel.getData("Show") == Boolean.TRUE;
								}
							}
						} else if (c.getData("Label") != null) {
							headerLabel = c;
							c.setEnabled(text.isEmpty());
						}
						c.setVisible(ok);
						((RowData) c.getLayoutData()).exclude = !ok;
					}

					for (Control c: topBag.getChildren()) {
						c.setVisible(false);
						((RowData) c.getLayoutData()).exclude = true;
					}

					if (!empty) {
						boolean found = false;
						Control moveBelow = favoriteslabel;
						if (!text.isEmpty() || favoriteslabel.getData("Show") == Boolean.TRUE) {
							for (Item lu: favorites) {
								Control existing = null;
								for (Control c: bag.getChildren()) {
									if (lu.equals(c.getData("Item"))) {
										if (c.getData("Ok") == Boolean.TRUE) {
											for (Control tc: topBag.getChildren()) {
												if (lu.equals(tc.getData("Item"))) {
													existing = tc;
													break;
												}
											}
											found = true;
											if (existing == null) {
												existing = makeItem.make(topBag, lu);
											}
											existing.moveBelow(moveBelow);
											moveBelow = existing;
											existing.setVisible(true);
											((RowData) existing.getLayoutData()).exclude = false;
										}
										break;
									}
								}
							}
						} else {
							found = true;
						}
						favoriteslabel.setVisible(found);
						((RowData) favoriteslabel.getLayoutData()).exclude = !found;

						found = false;
						moveBelow = lastUsedlabel;
						if (!text.isEmpty() || moveBelow.getData("Show") == Boolean.TRUE) {
							int maxVisible = MAX_USED_VISIBLE;
							for (Item lu: lastUsed) {
								Control existing = null;
								for (Control c: bag.getChildren()) {
									if (lu.equals(c.getData("Item"))) {
										if (c.getData("Ok") == Boolean.TRUE && !favorites.contains(lu)) {
											for (Control tc: topBag.getChildren()) {
												if (lu.equals(tc.getData("Item"))) {
													existing = tc;
													break;
												}
											}
											found = true;
											if (existing == null) {
												existing = makeItem.make(topBag, lu);
											}

											existing.moveBelow(moveBelow);
											moveBelow = existing;
											existing.setVisible(true);
											((RowData) existing.getLayoutData()).exclude = false;
											maxVisible--;
											break;
										}
										break;
									}
								}
								if (maxVisible == 0) {
									break;
								}
							}
						} else {
							found = true;
						}
						lastUsedlabel.setVisible(found);
						((RowData) lastUsedlabel.getLayoutData()).exclude = !found;
					}

					if (empty && selected != null && parent != null) {
						PaletteView.this.parent.setData("Selected", parent);
						PaletteView.this.parent.setData("Parent", parent.getParent());
						modifyText(e);
						return;
					}

					int clear = MAX_USED_VISIBLE;
					for (Control c: topBag.getChildren()) {
						Object o = c.getData("Item");
						if (o != null && !favorites.contains(o)) {
							if (clear-- < 0) {
								c.dispose();
							}
						}
					}

					Control lastSelected = (Control) bag.getData("LatestSelected");
					if (lastSelected == null || lastSelected.isDisposed() || !lastSelected.isVisible()) {
						Optional<Control> opt = Stream.concat(Arrays.stream(topBag.getChildren()), Arrays.stream(bag.getChildren()))
								.filter(c -> c.getData("Item") != null && c.isVisible()).findFirst();
						if (opt.isPresent()) {
							Event event = new Event();
							event.widget = opt.get();
							event.widget.notifyListeners(SWT.MouseDown, event);
						} else {
							browser.setText("<html><head>" +
									"<script type=\"text/javascript\">" +
									"document.oncontextmenu = new Function(\"return false\");" +
									"</script>" +
									"<style type=\"text/css\">" +
									"body {background-color: $background$;} \n" +
									"</style></head><body></body></html>");
						}
					}
					left.layout(true, true);
					bag.notifyListeners(SWT.Resize, new Event());
				}
			});

			pref = ConvertigoPlugin.getProperty("palette.sash");
			try {
				String[] prefs = pref.split(";");
				sash.setWeights(Integer.parseInt(prefs[0]), Integer.parseInt(prefs[1]));
			} catch (Exception e) {
				sash.setWeights(70, 30);
			}
			left.addControlListener(new ControlAdapter() {
				@Override
				public void controlResized(ControlEvent e) {
					int w[] = sash.getWeights();
					ConvertigoPlugin.setProperty("palette.sash", w[0] + ";" + w[1]);
					updateDoc.run();
				}
			});
			CompositeElement.getEngine(top).applyStyles(top, true);
			parent.layout(true);
			update();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void updateBags() {
		UpdateLabel updateLabel = (lb) -> {
			try {
				var plugin = ConvertigoPlugin.getDefault();
				if (lb.getData("Show") == Boolean.TRUE) {
					lb.setImage(plugin.getStudioIcon("icons/studio/show.gif"));
				} else {
					lb.setImage(plugin.getStudioIcon("icons/studio/hide.gif"));
				}
			} catch (IOException e1) {
			}
		};

		MakeLabel makeLabel = (p, txt) -> {
			CLabel lb = new CLabel(p, SWT.NONE);
			RowData rowData = new RowData();
			lb.setLayoutData(rowData);
			rowData.width = 4000;
			rowData.exclude = true;
			lb.setVisible(false);
			lb.setText("    " + txt);
			lb.setAlignment(SWT.LEFT);
			lb.setData("style", "color: black; background-color: lightgrey");
			lb.setData("Label", txt);
			lb.setData("Show", !hiddenCategories.contains(txt));
			updateLabel.update(lb);
			lb.setCursor(handCursor);
			lb.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseDown(MouseEvent e) {
					boolean show = lb.getData("Show") != Boolean.TRUE;
					lb.setData("Show", show);
					updateLabel.update(lb);
					if (show) {
						hiddenCategories.remove(txt);
					} else {
						hiddenCategories.add(txt);
					}
					String str = hiddenCategories.stream().collect(Collectors.joining(","));
					ConvertigoPlugin.setProperty("palette.hiddenCategories", str);
					searchText.notifyListeners(SWT.Modify, new Event());
				}
			});
			return lb;
		};

		int operations = DND.DROP_COPY | DND.DROP_MOVE;
		Transfer[] types = new Transfer[] { PaletteSourceTransfer.getInstance(), TextTransfer.getInstance() };

		makeItem = (p, item) -> {
			String text = item.name();
			if (text.length() > 18) {
				int id = text.substring(0, 18).lastIndexOf(' ');
				if (id > 5) {
					text = text.substring(0, id) + '\n' + text.substring(id + 1);
				} else {
					id = Math.min(18, text.length() / 2);
					text = text.substring(0, id) + '\n' + text.substring(id);
				}
			}
			CLabel clabel = new CLabel(p, SWT.NONE);
			RowData rowData = new RowData();
			clabel.setLayoutData(rowData);
			rowData.width = 160;
			rowData.exclude = true;
			clabel.setVisible(false);
			clabel.setImage(item.image());
			clabel.setText(text);
			clabel.setAlignment(SWT.LEFT);
			clabel.setToolTipText(RegexpUtils.removeTag.matcher(item.shortDescription()).replaceAll(""));
			clabel.setCursor(handCursor);
			clabel.setData("Item", item);
			clabel.setData("style", "color: inherit; background-color: inherit");
			clabel.addMouseListener(mouseListener);

			clabel.addListener(SWT.DragDetect, event -> {
				isCtrl = (event.stateMask & SWT.CTRL) != 0;
			});

			DragSource source = new DragSource(clabel, operations);
			source.setTransfer(types);
			source.addDragListener(dragListener);
			return clabel;
		};

		SwtUtils.disposeAllChildren(topBag);
		SwtUtils.disposeAllChildren(bag);
		bag.setData("LastSkipKey", "");
		
		favoriteslabel = makeLabel.make(topBag, "Favorites");
		lastUsedlabel = makeLabel.make(topBag, "Last used");
		var ordered = new TreeSet<Item>((i1, i2) -> i1.compareTo(i2));
		ordered.addAll(all.values());

		String lastParent = null;
		for (Item item: ordered) {
			if (!item.category().equals(lastParent)
					&& StringUtils.isNotBlank(lastParent = item.category())) {
				makeLabel.make(bag, lastParent);
			}
			makeItem.make(bag, item);
		}
	}

	@Override
	public void setFocus() {
		if (searchText != null && !searchText.isDisposed()) {
			searchText.setFocus();
		}
	}

	public void refresh() {
		if (isVisible) {
			refresh(300);
		}
	}

	private void refresh(long threshold) {
		Engine.execute(() -> {
			boolean[] needUpdate = {false};
			if (selectedProject != null) {
				ComponentManager cm = ComponentManager.of(selectedProject);
				if (cm != latestComponentManager) {
					needUpdate[0] = true;
					latestComponentManager = cm;
					cm.reloadComponents();
					all.clear();
					all.putAll(commons);
					for (Component comp: cm.getComponentsByGroup()) {
						String id = "ngx [" + comp.getGroup() + "] " + comp.getName();
						all.put(id, new Item() {

							@Override
							public String category() {
								return comp.getGroup();
							}

							@Override
							public String name() {
								return comp.getLabel();
							}

							@Override
							String description() {
								return comp.getDescription();
							}

							@Override
							Image image() {
								return getImage(comp.getImagePath());
							}

							@Override
							DatabaseObject newDatabaseObject() {
								DatabaseObject dbo = isCtrl ? cm.createBean(comp) : cm.createBeanFromHint(comp);
								return dbo;
							}

							@Override
							boolean allowedIn(DatabaseObject parent) {
								/*if (!cm.equals(ComponentManager.of(parent))) {
								return false;
							}*/

								if (isType) {
									return parent instanceof ApplicationComponent;
								}
								return comp.isAllowedIn(parent);
							}

							@Override
							String propertiesDescription() {
								return comp.getPropertiesDescription();
							}

							@Override
							String id() {
								return id;
							}

							@Override
							protected boolean builtIn() {
								return comp.isBuiltIn();
							}
						});
					}
				}
			}
			parent.getDisplay().asyncExec(() -> {
				try {
					String txt = searchText != null ? searchText.getText() : "";
					if (needUpdate[0]) {
						updateBags();
					}
					if (txt != null && searchText != null) {
						searchText.setText(txt);
					}
				} catch (Exception e) {
					Engine.logStudio.debug("(PaletteView) Palette init failed, retrying [" + e.getClass() + ": " + e.getMessage() + "]");
					refresh(1000);
				}
			});
		}, threshold);
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			if (part instanceof ProjectExplorerView && !selection.isEmpty()) {
				update();
			}
		}
	}

	@Override
	public void partOpened(IWorkbenchPartReference partRef) {
		if (partRef.getPart(false) instanceof ProjectExplorerView pev) {
			pev.addTreeObjectListener(this);
			update();
		}
	}

	@Override
	public void partClosed(IWorkbenchPartReference partRef) {
		if (partRef.getPart(false) instanceof ProjectExplorerView pev) {
			pev.removeTreeObjectListener(this);
		}
	}

	@Override
	public void partVisible(IWorkbenchPartReference partRef) {
		if (partRef.getId().equals(getViewSite().getId())) {
			if (!isVisible) {
				isVisible = true;
				update();
			}
		}
	}

	@Override
	public void partHidden(IWorkbenchPartReference partRef) {
		if (partRef.getId().equals(getViewSite().getId())) {
			isVisible = false;
		}
	}

	@Override
	public void treeObjectRemoved(TreeObjectEvent treeObjectEvent) {
		var treeObject = (TreeObject) treeObjectEvent.getSource();
		var obj = treeObject.getObject();
		if (obj instanceof Project || obj instanceof IShared) {
			selectedProject = null;
			latestComponentManager = null;
			refresh();
		}
	}

	@Override
	public void treeObjectPropertyChanged(TreeObjectEvent treeObjectEvent) {
		var treeObject = (TreeObject) treeObjectEvent.getSource();
		var obj = treeObject.getObject();
		if (obj instanceof IExposeAble && "exposed".equals(treeObjectEvent.propertyName) ||
				obj instanceof IShared && "name".equals(treeObjectEvent.propertyName)) {
			selectedProject = null;
			latestComponentManager = null;
			refresh();
		}
	}

	@Override
	public void treeObjectAdded(TreeObjectEvent treeObjectEvent) {
		var treeObject = (TreeObject) treeObjectEvent.getSource();
		var obj = treeObject.getObject();
		if (obj instanceof Project || obj instanceof IShared) {
			selectedProject = null;
			latestComponentManager = null;
			refresh();
		}
	}

	private void update() {
		var pev = ConvertigoPlugin.getDefault().getProjectExplorerView();
		if (selectionListener != null && pev != null) {
			selectionListener.selectionChanged(new SelectionChangedEvent(pev.viewer, pev.viewer.getSelection()));
		}
	}
}
