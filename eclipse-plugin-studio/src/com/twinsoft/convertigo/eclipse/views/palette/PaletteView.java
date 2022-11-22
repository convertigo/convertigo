/*
 * Copyright (c) 2001-2022 Convertigo SA.
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
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import com.twinsoft.convertigo.beans.BeansUtils;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.ngx.components.dynamic.Component;
import com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.dnd.PaletteSource;
import com.twinsoft.convertigo.eclipse.dnd.PaletteSourceTransfer;
import com.twinsoft.convertigo.eclipse.popup.actions.ClipboardAction;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowser;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.dbo_explorer.DboBean;
import com.twinsoft.convertigo.engine.dbo_explorer.DboBeans;
import com.twinsoft.convertigo.engine.dbo_explorer.DboCategory;
import com.twinsoft.convertigo.engine.dbo_explorer.DboGroup;
import com.twinsoft.convertigo.engine.util.RegexpUtils;

public class PaletteView extends ViewPart {
	protected static final int MAX_USED_HISTORY = 50;
	protected static final int MAX_USED_VISIBLE = 9;

	private Cursor handCursor;
	private Text searchText;
	private Map<String, Image> imageCache = new HashMap<>();
	private Deque<Item> lastUsed = new LinkedList<>();
	private Set<Item> favorites = new TreeSet<>();
	private LinkedHashMap<String, Item> all = new LinkedHashMap<>();

	abstract class Item implements Comparable<Item> {
		String shortDescription;
		String longDescription;
		String searchText;

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
		ConvertigoPlugin.runAtStartup(() -> init(parent));
	}

	public void init(Composite parent) {
		GridLayout gl;
		GridData gd;
		RowLayout rl;
		SashForm sash = new SashForm(parent, SWT.HORIZONTAL);
		RGB rgb = parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND).getRGB();
		sash.setData("style", "background-color: rgb(" + rgb.red + ", " + rgb.green + ", " + rgb.blue + ")");

		Composite left = new Composite(sash, SWT.NONE);
		left.setLayout(gl = new GridLayout(1, true));
		gl.marginHeight = gl.marginWidth = gl.verticalSpacing = 0;

		Composite search = new Composite(left, SWT.NONE);
		search.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		search.setLayout(new GridLayout(3, false));

		Label label = new Label(search, SWT.NONE);
		label.setLayoutData(new GridData());
		label.setText("Search: ");

		searchText = new Text(search, SWT.BORDER);
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

		CLabel clear = new CLabel(search, SWT.NONE);
		try {
			clear.setImage(ConvertigoPlugin.getDefault().getStudioIcon("icons/studio/delete.gif"));
		} catch (IOException e2) {
			clear.setText("X");
		}
		clear.setToolTipText("clear");
		clear.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				searchText.setText("");
			}
		});
		Composite topBag = new Composite(left, SWT.NONE);
		topBag.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		topBag.setLayout(rl = new RowLayout());
		rl.marginTop = rl.marginRight = rl.marginBottom = rl.marginLeft = 0;

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

		CLabel fav = new CLabel(right, SWT.NONE);
		fav.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fav.setCursor(handCursor);

		Runnable updateFav = () -> {
			Control latestSelected = (Control) bag.getData("LatestSelected");
			fav.setEnabled(false);
			if (latestSelected != null) {
				Item item = (Item) latestSelected.getData("Item");
				if (item != null) {
					fav.setEnabled(true);
					if (favorites.contains(item)) {
						fav.setText("Remove from favorite");
						try {
							fav.setImage(ConvertigoPlugin.getDefault().getStudioIcon("icons/studio/star_32x32.png"));
						} catch (IOException e1) {
						}
						return;
					}
				}
			}

			fav.setText("Add to favorite");
			try {
				fav.setImage(ConvertigoPlugin.getDefault().getStudioIcon("icons/studio/unstar_32x32.png"));
			} catch (IOException e1) {
			}
		};

		updateFav.run();

		fav.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				Control latestSelected = (Control) bag.getData("LatestSelected");
				if (latestSelected != null) {
					Item item = (Item) latestSelected.getData("Item");
					if (item != null) {
						if (favorites.contains(item)) {
							favorites.remove(item);
						} else {
							favorites.add(item);
						}
						String str = favorites.stream().map(Item::id).collect(Collectors.joining(","));
						ConvertigoPlugin.getDefault().getPreferenceStore().setValue("palette.favorites", str);
						searchText.notifyListeners(SWT.Modify, new Event());
					}
				}
				fav.setText("Remove from favorite");
				try {
					fav.setImage(ConvertigoPlugin.getDefault().getStudioIcon("icons/studio/star_32x32.png"));
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
				if (latestSelected != null) {
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
			for (DboGroup g: Engine.theApp.getDboExplorerManager().getGroups()) {
				String groupName = g.getName();
				for (DboCategory c: g.getCategories()) {
					String categoryName = c.getName().isEmpty() ? groupName : c.getName();

					for (DboBeans bs: c.getBeans()) {
						String beansName = bs.getName().isEmpty() ? categoryName : bs.getName();
						for (DboBean b: bs.getBeans()) {
							String cn = b.getClassName();
							if (cn.startsWith("com.twinsoft.convertigo.beans.ngx.components.")
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
									} catch (Exception e) {
										e.printStackTrace();
										return null;
									}
								}

								@Override
								boolean allowedIn(DatabaseObject parent) {
									try {
										return DatabaseObjectsManager.checkParent(parent.getClass(), b);
									} catch (ClassNotFoundException e) {
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
						String sXml = ClipboardAction.dnd.copy(item.newDatabaseObject());
						if (sXml != null) {
							event.doit = true;
							event.data = sXml;
							PaletteSourceTransfer.getInstance().setPaletteSource(new PaletteSource(sXml));
							if (lastUsed.isEmpty() || !item.equals(lastUsed.getFirst())) {
								lastUsed.removeFirstOccurrence(item);
								lastUsed.addFirst(item);
								while (lastUsed.size() > MAX_USED_HISTORY) {
									lastUsed.pollLast();
								}
								String str = lastUsed.stream().map(Item::id).collect(Collectors.joining(","));
								ConvertigoPlugin.getDefault().getPreferenceStore().setValue("palette.history", str);
							}
						}
					} catch (Exception e) {
						ConvertigoPlugin.logException(e, "Cannot drag");
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
						return ComponentManager.createBeanFromHint(comp);
					}

					@Override
					boolean allowedIn(DatabaseObject parent) {
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

			String pref = ConvertigoPlugin.getDefault().getPreferenceStore().getString("palette.favorites");
			if (StringUtils.isNotBlank(pref)) {
				for (String h: pref.split(",")) {
					if (all.containsKey(h)) {
						favorites.add(all.get(h));
					}
				}
			}

			pref = ConvertigoPlugin.getDefault().getPreferenceStore().getString("palette.history");
			if (StringUtils.isNotBlank(pref)) {
				for (String h: pref.split(",")) {
					if (all.containsKey(h)) {
						lastUsed.add(all.get(h));
					}
				}
			}

			MakeLabel makeLabel = (p, txt) -> {
				Label lb = new Label(p, SWT.NONE);
				RowData rowData = new RowData();
				lb.setLayoutData(rowData);
				rowData.width = 4000;
				rowData.exclude = true;
				lb.setVisible(false);
				lb.setText("    " + txt);
				lb.setData("style", "color: black; background-color: lightgrey");
				lb.setData("Label", txt);
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
						Control last = (Control) bag.getData("last");
						if (last != null) {
							Rectangle r = last.getBounds();
							min = r.y + r.height;
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
					Control last = null;
					DatabaseObject selected = (DatabaseObject) bag.getData("selected");
					Control headerLabel = null;
					boolean empty = true;
					for (Control c: bag.getChildren()) {
						Item item = (Item) c.getData("Item");
						boolean ok = false;
						if (item != null) {
							ok = selected != null;
							ok = ok && item.allowedIn(selected);
							if (empty && ok) {
								empty = false;
							}
							ok = ok && (text.isEmpty() || item.searchText().contains(text));
							if (ok) {
								last = c;
								if (headerLabel != null && headerLabel.getData("Label").equals(item.category())) {
									headerLabel.setVisible(true);
									((RowData) headerLabel.getLayoutData()).exclude = false;
								}
							}
						} else if (c.getData("Label") != null) {
							headerLabel = c;
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
						for (Item lu: favorites) {
							Control existing = null;
							for (Control c: bag.getChildren()) {
								if (lu.equals(c.getData("Item"))) {
									if (c.isVisible()) {
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
						favoriteslabel.setVisible(found);
						((RowData) favoriteslabel.getLayoutData()).exclude = !found;

						found = false;
						moveBelow = lastUsedlabel;
						int maxVisible = MAX_USED_VISIBLE;
						for (Item lu: lastUsed) {
							Control existing = null;
							for (Control c: bag.getChildren()) {
								if (lu.equals(c.getData("Item"))) {
									if (c.isVisible() && !favorites.contains(lu)) {
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
						lastUsedlabel.setVisible(found);
						((RowData) lastUsedlabel.getLayoutData()).exclude = !found;
					}

					if (empty && selected != null) {
						bag.setData("selected", selected.getParent());
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
					if (lastSelected == null || !lastSelected.isVisible()) {
						Optional<Control> opt = Stream.concat(Arrays.stream(topBag.getChildren()), Arrays.stream(bag.getChildren()))
								.filter(c -> c.getData("Item") != null && c.isVisible()).findFirst();
						if (opt.isPresent()) {
							Event event = new Event();
							event.widget = opt.get();
							event.widget.notifyListeners(SWT.MouseDown, event);
						}
					}
					bag.setData("last", last);
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
						TreeSelection selection = (TreeSelection) e.getSelection();
						if (selection.getFirstElement() instanceof TreeObject) {
							TreeObject to = (TreeObject) selection.getFirstElement();
							while (to != null) {
								if (to instanceof DatabaseObjectTreeObject) {
									DatabaseObjectTreeObject dbot = (DatabaseObjectTreeObject) to;
									DatabaseObject last = (DatabaseObject) bag.getData("selected");
									bag.setData("selected", dbot.getObject());
									if (last == null || !last.getClass().equals(dbot.getObject().getClass())) {
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

			sash.setWeights(70, 30);
			parent.layout(true);

			ConvertigoPlugin.asyncExec(initPev);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setFocus() {
		searchText.setFocus();
	}

}
