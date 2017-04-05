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
 * $URL: $
 * $Author: $
 * $Revision: $
 * $Date: $
 */

package com.twinsoft.convertigo.eclipse.wizards.new_mobile;

import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.mobile.components.dynamic.Component;
import com.twinsoft.convertigo.beans.mobile.components.dynamic.ComponentManager;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.dnd.PaletteSource;
import com.twinsoft.convertigo.eclipse.dnd.PaletteSourceTransfer;
import com.twinsoft.convertigo.eclipse.popup.actions.ClipboardAction;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowser;

public class ComponentExplorerComposite extends Composite {
	static private Pattern removeTag = Pattern.compile("</?\\w+ ?/?>");
	
	protected Color FOREGROUND_COLOR = Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND);
	protected Color BACKGROUND_COLOR = Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
	protected Color FOREGROUND_SELECTED_COLOR = Display.getDefault().getSystemColor(SWT.COLOR_BLUE);
	protected Color BACKGROUND_SELECTED_COLOR = Display.getDefault().getSystemColor(
			SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW);

	//private String technology = null;
	private Class<? extends DatabaseObject> databaseObjectClass = null;
	protected CLabel currentSelectedObject = null;

	private DatabaseObject parentObject = null;
	protected Cursor handCursor = null;
	protected Map<CLabel, Object> objectsMap = null;
	
	protected WizardPage wizardPage = null;
	private C8oBrowser helpBrowser = null;
	public ScrolledComposite scrolledComposite;
	protected Composite composite = null;
	protected Composite [] composites = null;
	protected ExpandItem [] items = null;
	protected ExpandBar bar;
	
	protected List<String> documentedDboList = new ArrayList<String>();
	
	public ComponentExplorerComposite(WizardPage wizardPage, Composite parent, int style, Object parentObject,
			Class<? extends DatabaseObject> beanClass) {
		this(parent, style, parentObject, beanClass);
		this.wizardPage = wizardPage;
	}

	public ComponentExplorerComposite(Composite parent, int style, Object parentObject,
			Class<? extends DatabaseObject> beanClass) {
		super(parent, style);
		this.parentObject = (DatabaseObject) parentObject;
		this.databaseObjectClass = beanClass;

		objectsMap = new HashMap<CLabel, Object>(32);
		initialize();
	}

	public ComponentExplorerComposite(Composite parent, int style) {
		super(parent, style);

		objectsMap = new HashMap<CLabel, Object>(32);
		initializePalette();
	}
	
	protected void findDatabaseObjects() {
		if (objectsMap.isEmpty()) {
			try {
				//Class<? extends DatabaseObject> parentObjectClass = parentObject.getClass();
				
				ComponentManager.refresh(); //TODO: to remove when models file is complete
				
				List<String> categories = ComponentManager.getGroups();
				List<Component> components = ComponentManager.getComponents();
				
				handCursor = new Cursor(Display.getDefault(), SWT.CURSOR_HAND);
				
				//initialize composites.
				RowLayout rowLayout = new RowLayout();
				rowLayout.pack = false;
				//bar = new ExpandBar(scrolledComposite, SWT.NONE);
				//bar.setSpacing(8);
				String itemText = "";
				int i=0;
				
				Iterator<String> iterator = categories.iterator();
				composites = new Composite[categories.size()];
				items = new ExpandItem[categories.size()];
				while (iterator.hasNext())
				{
					composites[i] = new Composite(bar, SWT.NONE);
					composites[i].setLayout(rowLayout);
					composites[i].setBackground(BACKGROUND_COLOR);
					items[i] = new ExpandItem(bar, SWT.NONE, i);
					items[i].setControl(composites[i]);
					items[i].setExpanded(true);
					itemText = iterator.next();
					/*if (itemText.equals("")) {
						itemText = databaseObjectClass.getSimpleName();
					}*/
					items[i].setText(itemText);
					i++;
				}
				
				boolean bSelected = true;
				for (Component c : components) {
					boolean isAllowed = parentObject != null ? c.isAllowedIn(parentObject):true;
					if (isAllowed) {
						addLabelEx(c, bSelected);
						bSelected = false;
					}
				}

				// We select by default the first item if no default dbo found.
				if (currentSelectedObject == null && composites[0].getChildren().length > 0) {
					currentSelectedObject = (CLabel) composites[0].getChildren()[0];
					currentSelectedObject.setForeground(FOREGROUND_SELECTED_COLOR);
					currentSelectedObject.setBackground(BACKGROUND_SELECTED_COLOR);
		
					Component currentSelectedComponent = getCurrentSelectedComponent();
					if (currentSelectedComponent != null) {
						//updateHelpText(currentSelectedObjectBeanInfo);
					}
				}
				
			} catch (Exception e) {
				ConvertigoPlugin.logException(e, "Unable to load database objects properties.");
			}
		}
	}
	
	private void addLabelEx(Component component, boolean bSelected) {
		String category = component.getGroup();
		if("".equals(category)) {
			composite = composites[0];
		} else {
			for(int i=0; i < items.length; i++) {
				if(items[i].getText().equals(category)) {
					composite = composites[i];
				}
			}
		}
		
		final CLabel label = new CLabel(composite, SWT.NONE);
		Image image = null;
		try {
			image = ConvertigoPlugin.getDefault().getIconFromPath(component.getImagePath(), BeanInfo.ICON_COLOR_32x32);
		} catch (Exception e) {}
		label.setImage(image);
		label.setText(component.getLabel());
		label.setAlignment(SWT.LEFT);
		label.setToolTipText(removeTag.matcher(component.getDescription()).replaceAll(""));
		label.setCursor(handCursor);
		label.setLayoutData(new RowData());
		
		objectsMap.put(label, component);
		
		if (bSelected) {
			currentSelectedObject = label;

			currentSelectedObject.setForeground(FOREGROUND_SELECTED_COLOR);
			currentSelectedObject.setBackground(BACKGROUND_SELECTED_COLOR);

			Component currentSelectedComponent = getCurrentSelectedComponent();
			if (currentSelectedComponent != null) {
				//updateHelpText(currentSelectedObjectBeanInfo);
			}
		}

		// DND support for Mobile palette
		if (wizardPage == null) {
		    Transfer[] types = new Transfer[] { PaletteSourceTransfer.getInstance() };
		    int operations = DND.DROP_COPY | DND.DROP_MOVE;
	
		    DragSource source = new DragSource(label, operations);
		    source.setTransfer(types);
		    source.addDragListener(new DragSourceAdapter() {
		    	@Override
		    	public void dragStart(DragSourceEvent event) {
					try {
						if (currentSelectedObject != null) {
							currentSelectedObject.setForeground(FOREGROUND_COLOR);
							currentSelectedObject.setBackground(BACKGROUND_COLOR);
						}
			    		currentSelectedObject = label;
						currentSelectedObject.setForeground(FOREGROUND_SELECTED_COLOR);
						currentSelectedObject.setBackground(BACKGROUND_SELECTED_COLOR);
						
			    		Component c = (Component) objectsMap.get(label);
						DatabaseObject dbo = ComponentManager.createBean(c);
						String sXml = ClipboardAction.dnd.copy(dbo);
						if (sXml != null) {
							event.doit = true;
							PaletteSourceTransfer.getInstance().setPaletteSource(new PaletteSource(sXml));
						}
					} catch (Exception e) {
						ConvertigoPlugin.logException(e, "Cannot drag");
					}
		    	}
		    });
		}
		
		label.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				if (currentSelectedObject == (CLabel) e.getSource())
					return;

				if (currentSelectedObject != null) {
					currentSelectedObject.setForeground(FOREGROUND_COLOR);
					currentSelectedObject.setBackground(BACKGROUND_COLOR);
				}

				currentSelectedObject = (CLabel) e.getSource();

				ConvertigoPlugin.logDebug("currentSelectedObject: '" + currentSelectedObject.getText() + "'.");

				currentSelectedObject.setForeground(FOREGROUND_SELECTED_COLOR);
				currentSelectedObject.setBackground(BACKGROUND_SELECTED_COLOR);

				Component currentSelectedComponent = getCurrentSelectedComponent();
				if (currentSelectedComponent != null) {
					//updateHelpText(currentSelectedObjectBeanInfo);
				}
			}

			@Override
			public void mouseDoubleClick(MouseEvent e) {
				currentSelectedObject = (CLabel) e.getSource();

				if (wizardPage != null) {
					wizardPage.setPageComplete(true);
					((ComponentExplorerWizardPage) wizardPage).showNextPage();
				}
			}
		});
		
		ConvertigoPlugin.logDebug("Loaded '" + component.getLabel() + "'.");
	}

	public Component getCurrentSelectedComponent() {
		Component c = null;
		if (currentSelectedObject != null) {
			c = (Component) objectsMap.get(currentSelectedObject);
		}
		return c;
	}
	
	/**
	 * This method initializes this
	 * 
	 */
	protected void initialize() {

		layout(true); 
		layout(true, true); 
		
		setLayout(new GridLayout(3, true));
		
		GridData gridData;
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalSpan = 2;
		gridData.verticalAlignment = GridData.BEGINNING;
		gridData.grabExcessVerticalSpace = true;

		scrolledComposite = new ScrolledComposite(this, SWT.V_SCROLL);
		scrolledComposite.setLayoutData(gridData);
		
		bar = new ExpandBar(scrolledComposite, SWT.NONE);
		bar.setSpacing(8);
		
		helpBrowser = new C8oBrowser(this, SWT.BORDER | SWT.MULTI | SWT.WRAP);
		gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.verticalAlignment = GridData.FILL;
		gridData.grabExcessVerticalSpace = true;
		gridData.grabExcessHorizontalSpace = true;
		helpBrowser.setLayoutData(gridData);

		// find associated database objects
		findDatabaseObjects();
		
		for (ExpandItem expandItem : bar.getItems()) {
			/* update the item's height if needed in response to changes*/ 
			final ExpandItem item = expandItem;
			final Composite composite = (Composite) expandItem.getControl();	
			composite.addControlListener(new ControlAdapter() {
				public void controlResized(ControlEvent e) {
					Point size = composite.getSize();
					Point size2 = composite.computeSize(size.x,
							SWT.DEFAULT);
					item.setHeight(size2.y);
		     }
		    });
		}
		
		scrolledComposite.setContent(bar);		
		scrolledComposite.setExpandVertical(true);
		scrolledComposite.setExpandHorizontal(true);
		
		scrolledComposite.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				Rectangle r = scrolledComposite.getClientArea();
				scrolledComposite.setMinSize(bar.computeSize(r.width, SWT.DEFAULT));
			}
		});
				
		this.setSize(new org.eclipse.swt.graphics.Point(800, 400));
	}

	protected void initializePalette() {

		layout(true); 
		layout(true, true); 
		
		setLayout(new GridLayout());
		
		GridData gridData;
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.verticalAlignment = SWT.FILL;
		gridData.grabExcessVerticalSpace = true;

		scrolledComposite = new ScrolledComposite(this, SWT.V_SCROLL);
		scrolledComposite.setLayoutData(gridData);
		
		bar = new ExpandBar(scrolledComposite, SWT.NONE);
		bar.setSpacing(8);
		
		// find associated database objects
		findDatabaseObjects();
		
		for (ExpandItem expandItem : bar.getItems()) {
			/* update the item's height if needed in response to changes*/ 
			final ExpandItem item = expandItem;
			final Composite composite = (Composite) expandItem.getControl();	
			composite.addControlListener(new ControlAdapter() {
				public void controlResized(ControlEvent e) {
					Point size = composite.getSize();
					Point size2 = composite.computeSize(size.x,
							SWT.DEFAULT);
					item.setHeight(size2.y);
		     }
		    });
		}
		
		scrolledComposite.setContent(bar);		
		scrolledComposite.setExpandVertical(true);
		scrolledComposite.setExpandHorizontal(true);
		
		scrolledComposite.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				Rectangle r = scrolledComposite.getClientArea();
				scrolledComposite.setMinSize(bar.computeSize(r.width, SWT.DEFAULT));
			}
		});
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.swt.widgets.Widget#dispose()
	 */
	public void dispose() {
		super.dispose();

		objectsMap.clear();

		if (handCursor != null)
			handCursor.dispose();

	}

	private String cleanDescription(String description, boolean bHtml) {
		String cleanDescription = description;
		// Replace first space
		if (cleanDescription.charAt(0) == ' ') {
			cleanDescription = cleanDescription.substring(1);
		}

		// replace orangetwinsoft class by text color style
		cleanDescription = cleanDescription.replace("class=\"orangetwinsoft\"", (bHtml ? "style=\"color=#FC870A;\"" : ""));
		
		// replace computer class by new font
		cleanDescription = cleanDescription.replace("class=\"computer\"", (bHtml ? "style=\"font-family: lucida Console;\"" : ""));
		
		// Double BR tags
		cleanDescription = cleanDescription.replaceAll("<br/>(?:<br/>)?", (bHtml ? "<br/><br/>" : ""));
		
		return cleanDescription;
	}

	private void updateHelpText(BeanInfo bi) {
		BeanDescriptor beanDescriptor = bi.getBeanDescriptor();
		boolean isDocumented = documentedDboList.contains(beanDescriptor.getBeanClass().getName());
		String beanDescription = isDocumented ? beanDescriptor.getShortDescription():"Not yet documented. |";
		String[] beanDescriptions = beanDescription.split("\\|");
		String beanDisplayName = beanDescriptor.getDisplayName();
		String beanShortDescription = beanDescriptions.length >= 1 ? beanDescriptions[0] : "n/a";
		String beanLongDescription = beanDescriptions.length >= 2 ? beanDescriptions[1] : "n/a";

		beanShortDescription = cleanDescription(beanShortDescription,true);
		beanLongDescription = cleanDescription(beanLongDescription,true);

		if (helpBrowser != null) {
			helpBrowser.setText("<html>" +
									"<head>" +
									"<script type=\"text/javascript\">"+
								        "document.oncontextmenu = new Function(\"return false\");"+
								    "</script>"+
											"<style type=\"text/css\">"+
												  "body {"+
												    "font-family: Courrier new, sans-serif;"+
												    "font-size: 14px;"+
												    "padding-left: 0.3em;"+
												    "background-color: #ECEBEB }"+
											"</style>"+
									"</head><p>" 
								+ "<font size=\"4.5\"><u><b>"+beanDisplayName+"</b></u></font>" + "<br><br>" 
								+ "<i>"+beanShortDescription+"</i>" + "<br><br>" 
								+ beanLongDescription + "</p></html>");
		}
	}

} // @jve:decl-index=0:visual-constraint="10,10"
