/*
 * Copyright (c) 2001-2024 Convertigo SA.
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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.e4.ui.css.swt.dom.CompositeElement;
import org.eclipse.jface.viewers.ISelectionChangedListener;
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
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import com.twinsoft.convertigo.beans.BeansUtils;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.ngx.components.IExposeAble;
import com.twinsoft.convertigo.beans.ngx.components.UISharedComponent;
import com.twinsoft.convertigo.beans.ngx.components.dynamic.Component;
import com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.dnd.PaletteSource;
import com.twinsoft.convertigo.eclipse.dnd.PaletteSourceTransfer;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowser;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectEvent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectListener;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.MobileApplicationComponentTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.NgxApplicationComponentTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ObjectsFolderTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ProjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.SequenceTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.UnloadedProjectTreeObject;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.dbo_explorer.DboBean;
import com.twinsoft.convertigo.engine.dbo_explorer.DboBeans;
import com.twinsoft.convertigo.engine.dbo_explorer.DboCategory;
import com.twinsoft.convertigo.engine.dbo_explorer.DboGroup;
import com.twinsoft.convertigo.engine.util.CachedIntrospector;
import com.twinsoft.convertigo.engine.util.RegexpUtils;

public class PaletteView extends ViewPart {
	protected static final int MAX_USED_HISTORY = 50;
	protected static final int MAX_USED_VISIBLE = 8;

	private Composite parent;
	private Cursor handCursor;
	private Text searchText;
	private Map<String, Image> imageCache = new HashMap<>();
	private LinkedHashMap<String, Item> all = new LinkedHashMap<>();

	abstract class Item implements Comparable<Item> {
		String shortDescription;
		String longDescription;
		String searchText;
		DatabaseObject dbo;

		Item() {
			String[] beanDescriptions = description().split("\\|");
			shortDescription = BeansUtils.cleanDescription(beanDescriptions.length >= 1 ? beanDescriptions[0] : "n/a", true);
			longDescription = BeansUtils.cleanDescription(beanDescriptions.length >= 2 ? beanDescriptions[1] : "n/a", true);
			searchText = name().toLowerCase() + " " + shortDescription.toLowerCase();
		}

		String shortDescription() {
			return shortDescription;
		}

		String longDescription() {
			return longDescription;
		}

		String searchText() {
			return searchText;
		}

		@Override
		public int compareTo(Item o) {
			return name().compareTo(o.name());
		}
		
		boolean allowedIn(int folderType) {
			return folderType == ProjectExplorerView.getDatabaseObjectType(databaseObject());
		}
		
		boolean builtIn() {
			return true;
		}
		
		DatabaseObject databaseObject() {
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
		handCursor.dispose();
		for (Image image: imageCache.values()) {
			image.dispose();
		}
		imageCache.clear();
		super.dispose();
	}

	public Image getImage(String imagePath) {
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
			image = new Image(handCursor.getDevice(), imagePath);
			imageCache.put(imagePath, image);
		}
		return image;
	}

	@Override
	public void createPartControl(Composite parent) {
		handCursor = new Cursor(parent.getDisplay(), SWT.CURSOR_HAND);
		this.parent = parent;
		parent.setLayout(new GridLayout(1, true));
		ConvertigoPlugin.runAtStartup(() -> refresh(1));
	}

	public void init() {
		ConvertigoPlugin plugin = ConvertigoPlugin.getDefault();
		Deque<Item> lastUsed = new LinkedList<>();
		Set<Item> favorites = new TreeSet<>();
		Set<String> hiddenCategories = new HashSet<>();
		boolean[] isCtrl = {false};
		boolean[] isType = {"type".equals(ConvertigoPlugin.getProperty("palette.link"))};

		GridLayout gl;
		GridData gd;
		RowLayout rl;
		
		Composite top = new Composite(parent, SWT.NONE);
		top.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		top.setLayout(new GridLayout(4, false));
		
		ToolBar bar = new ToolBar(top, SWT.NONE);
		ToolItem tiLink = new ToolItem(bar, SWT.CHECK);
		tiLink.setToolTipText("Link with the 'Projects tree' selection");
		tiLink.setData("style", "background: unset");
		try {
			tiLink.setImage(ConvertigoPlugin.getDefault().getStudioIcon("icons/studio/resize_connector.gif"));
		} catch (Exception e3) {
			tiLink.setText("Link");
		}
		tiLink.setSelection(!"off".equals(ConvertigoPlugin.getProperty("palette.link")));
		if (isType[0]) {
			ConvertigoPlugin.asyncExec(() -> tiLink.setBackground(tiLink.getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION)));
		}
		
		bar = new ToolBar(top, SWT.NONE);
		ToolItem tiInternal = new ToolItem(bar, SWT.CHECK);
		tiInternal.setToolTipText("Built-in objects visibility");
		tiInternal.setData("style", "background: unset");
		try {
			tiInternal.setImage(ConvertigoPlugin.getDefault().getStudioIcon("icons/studio/convertigo_logo_16x16.png"));
		} catch (Exception e3) {
			tiInternal.setText("Internal");
		}
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
		
		Composite topBag = new Composite(left, SWT.NONE);
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

		Composite bag = new Composite(scroll, SWT.NONE);
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

		MouseListener mouseListener = new MouseAdapter() {

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
				}
				bag.setData("LatestSelected", c);
				c.setData("style", "color: blue; background-color: lightcyan");
				CompositeElement.getEngine(sash).applyStyles(sash, true);

				String propertiesDescription = item.propertiesDescription();
				browser.setText("<html>" +
						"<head>" +
						"<script type=\"text/javascript\">" +
						"document.oncontextmenu = new Function(\"return false\");" +
						"</script>" +
						"<style type=\"text/css\">" +
						"body {" +
						"font-family: Courrier new, sans-serif;" +
						"font-size: 14px;" +
						"padding-left: 0.3em;" +
						"color: $foreground$;" +
						"background-color: $background$ } \n" +
						"a { color: $link$; }" +
						"</style>" +
						"</head><body><p>"
						+ "<font size=\"4.5\"><u><b>" + item.name() + "</b></u></font>" + "<br><br>"
						+ "<i>" + item.shortDescription() + "</i>" + "<br><br>"
						+ item.longDescription() + "<br><br>"
						+ (propertiesDescription.isEmpty() ? "" : "<u>Properties</u>:<br>")
						+ propertiesDescription
						+ "</p></body></html>");
				updateFav.run();
			}

		};

		try {
			all.clear();
			for (DboGroup g: Engine.theApp.getDboExplorerManager().getGroups()) {
				String groupName = g.getName();
				for (DboCategory c: g.getCategories()) {
					String categoryName = c.getName().isEmpty() ? groupName : c.getName();

					for (DboBeans bs: c.getBeans()) {
						String beansName = bs.getName().isEmpty() ? categoryName : bs.getName();
						for (DboBean b: bs.getBeans()) {
							String cn = b.getClassName();
							if ((cn.startsWith("com.twinsoft.convertigo.beans.ngx.components.")
									|| cn.startsWith("com.twinsoft.convertigo.beans.mobile.components."))
									&& !cn.endsWith("PageComponent")) {
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
										if (isType[0]) {
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
										if(o1.isExpert() == o2.isExpert()) {
											return o1.getDisplayName().compareTo(o2.getDisplayName());
										} else if(o1.isExpert()) {
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

			String lastParent = null;
			Transfer[] types = new Transfer[] { PaletteSourceTransfer.getInstance(), TextTransfer.getInstance() };
			int operations = DND.DROP_COPY | DND.DROP_MOVE;
			DragSourceAdapter dragListener = new DragSourceAdapter() {
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

			for (Component comp: ComponentManager.getComponentsByGroup()) {
				String id = "ngx " + comp.getName();
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
						return isCtrl[0] ? ComponentManager.createBean(comp) : ComponentManager.createBeanFromHint(comp);
					}

					@Override
					boolean allowedIn(DatabaseObject parent) {
						if (isType[0]) {
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
					boolean builtIn() {
						return comp.isBuiltIn();
					}
				});
			}

			for (com.twinsoft.convertigo.beans.mobile.components.dynamic.Component comp: com.twinsoft.convertigo.beans.mobile.components.dynamic.ComponentManager.getComponentsByGroup()) {
				String id = "mb " + comp.getName();
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
						if (isType[0]) {
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

			UpdateLabel updateLabel = (lb) -> {
				try {
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

			MakeItem makeItem = (p, item) -> {
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
					isCtrl[0] = (event.stateMask & SWT.CTRL) != 0;
				});

				DragSource source = new DragSource(clabel, operations);
				source.setTransfer(types);
				source.addDragListener(dragListener);
				return clabel;
			};

			Control favoriteslabel = makeLabel.make(topBag, "Favorites");
			Control lastUsedlabel = makeLabel.make(topBag, "Last used");

			for (Item item: all.values()) {
				if (!item.category().equals(lastParent)
						&& StringUtils.isNotBlank(lastParent = item.category())) {
					makeLabel.make(bag, lastParent);
				}
				makeItem.make(bag, item);
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
					favoriteslabel.setEnabled(text.isEmpty());
					lastUsedlabel.setEnabled(text.isEmpty());

					DatabaseObject selected = (DatabaseObject) PaletteView.this.parent.getData("Selected");
					DatabaseObject parent = (DatabaseObject) PaletteView.this.parent.getData("Parent");
					Integer folderType = (Integer) PaletteView.this.parent.getData("FolderType");
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

			Runnable initPev = () -> {
				ProjectExplorerView pev = ConvertigoPlugin.getDefault().getProjectExplorerView();
				if (pev == null) {
					return;
				}
				ISelectionChangedListener selectionListener = new ISelectionChangedListener() {
					@Override
					public void selectionChanged(SelectionChangedEvent e) {
						if (bag.isDisposed()) {
							pev.removeSelectionChangedListener(this);
							return;
						}
						
						if (!tiLink.getSelection()) {
							return;
						}
						
						TreeSelection selection = (TreeSelection) e.getSelection();
						if (selection.getFirstElement() instanceof TreeObject) {
							TreeObject to = (TreeObject) selection.getFirstElement();
							while (to != null) {
								Integer folderType = null;
								DatabaseObject selected = null;
								Object parent = null;
								Boolean clear = null;
								
								if (isType[0]) {
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
				pev.addSelectionChangedListener(selectionListener);
				selectionListener.selectionChanged(new SelectionChangedEvent(pev.viewer, pev.viewer.getSelection()));
				
				tiLink.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						String val;
						if (!tiLink.getSelection() && "on".equals(ConvertigoPlugin.getProperty("palette.link"))) {
							tiLink.setSelection(true);
							tiLink.setBackground(tiLink.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
							val = "type";
							isType[0] = true;
						} else {
							tiLink.setBackground(null);
							val = tiLink.getSelection() ? "on" : "off";
							isType[0] = false;
						}
						ConvertigoPlugin.setProperty("palette.link", val);
						selectionListener.selectionChanged(new SelectionChangedEvent(pev.viewer, pev.viewer.getSelection()));
					}
				});
				
				TreeObjectListener tol = new TreeObjectListener() {
					@Override
					public void treeObjectRemoved(TreeObjectEvent treeObjectEvent) {
						if (bag.isDisposed()) {
							pev.removeTreeObjectListener(this);
						}
						TreeObject treeObject = (TreeObject) treeObjectEvent.getSource();
						if (treeObject instanceof ProjectTreeObject || treeObject instanceof UnloadedProjectTreeObject) {
							refresh();
						}
					}
					
					@Override
					public void treeObjectPropertyChanged(TreeObjectEvent treeObjectEvent) {
						if (bag.isDisposed()) {
							pev.removeTreeObjectListener(this);
							return;
						}
						TreeObject treeObject = (TreeObject) treeObjectEvent.getSource();
						if (treeObject.getObject() instanceof IExposeAble && "exposed".equals(treeObjectEvent.propertyName)) {
							refresh();
						}
					}
					
					@Override
					public void treeObjectAdded(TreeObjectEvent treeObjectEvent) {
						if (bag.isDisposed()) {
							pev.removeTreeObjectListener(this);
						}
						TreeObject treeObject = (TreeObject) treeObjectEvent.getSource();
						if (treeObject instanceof ProjectTreeObject || treeObject instanceof UnloadedProjectTreeObject) {
							refresh();
						}
					}
				};
				pev.addTreeObjectListener(tol);
			};

			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().addPartListener(new IPartListener2() {
				@Override
				public void partOpened(IWorkbenchPartReference partRef) {
					if (bag.isDisposed()) {
						partRef.getPage().removePartListener(this);
						return;
					}
					if (partRef.getPart(false) instanceof ProjectExplorerView) {
						ConvertigoPlugin.asyncExec(initPev);
					}
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
				}
			});
			parent.layout(true);

			ConvertigoPlugin.asyncExec(initPev);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setFocus() {
		if (searchText != null && !searchText.isDisposed()) {
			searchText.setFocus();
		}
	}
	
	public void refresh() {
		refresh(500);
	}
	
	public void refresh(long threshold) {
		Engine.execute(() -> {
			ComponentManager.reloadComponents();
			parent.getDisplay().asyncExec(() -> {
				String txt = searchText != null ? searchText.getText() : null;
				for (Control c: parent.getChildren()) {
					c.dispose();
				}
				init();
				if (txt != null) {
					searchText.setText(txt);
				}
			});
		}, threshold);
	}
}
