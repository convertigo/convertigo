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

package com.twinsoft.convertigo.eclipse.wizards.new_object;

import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.ScrolledComposite;
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

import com.twinsoft.convertigo.beans.BeansUtils;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowser;
import com.twinsoft.convertigo.engine.dbo_explorer.DboBean;
import com.twinsoft.convertigo.engine.dbo_explorer.DboBeans;
import com.twinsoft.convertigo.engine.dbo_explorer.DboCategory;
import com.twinsoft.convertigo.engine.dbo_explorer.DboExplorerManager;
import com.twinsoft.convertigo.engine.dbo_explorer.DboGroup;
import com.twinsoft.convertigo.engine.dbo_explorer.DboParent;
import com.twinsoft.convertigo.engine.dbo_explorer.DboUtils;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class ObjectsExplorerComposite extends Composite {
	static private Pattern removeTag = Pattern.compile("</?\\w+ ?/?>");
	
	protected Color FOREGROUND_COLOR = Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND);
	protected Color BACKGROUND_COLOR = Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
	protected Color FOREGROUND_SELECTED_COLOR = Display.getDefault().getSystemColor(SWT.COLOR_BLUE);
	protected Color BACKGROUND_SELECTED_COLOR = Display.getDefault().getSystemColor(
			SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW);

	private String technology = null;
	private Class<? extends DatabaseObject> databaseObjectClass = null;
	protected CLabel currentSelectedObject = null;

	private DatabaseObject parentObject = null;
	protected Cursor handCursor = null;
	protected Map<CLabel, Object> objectsMap = null;
	
	protected WizardPage wizardPage = null;
	private Composite compositeObjects;
	private C8oBrowser helpBrowser = null;
	public ScrolledComposite scrolledComposite;
	protected Composite composite = null;
	protected Composite [] composites = null;
	protected ExpandItem [] items = null;
	protected ExpandBar bar;
	
	protected List<String> defaultDboList = new ArrayList<String>();
	protected List<String> documentedDboList = new ArrayList<String>();
	
	public ObjectsExplorerComposite(WizardPage wizardPage, Composite parent, int style, Object parentObject,
			Class<? extends DatabaseObject> beanClass) {
		this(parent, style, parentObject, beanClass);
		this.wizardPage = wizardPage;
	}

	public ObjectsExplorerComposite(Composite parent, int style, Object parentObject,
			Class<? extends DatabaseObject> beanClass) {
		super(parent, style);
		this.parentObject = (DatabaseObject) parentObject;
		this.databaseObjectClass = beanClass;

		objectsMap = new HashMap<CLabel, Object>(32);
		initialize();
	}

	protected void findDatabaseObjects() {
		if (objectsMap.isEmpty()) {
			try {
				Class<? extends DatabaseObject> parentObjectClass = parentObject.getClass();

				Map<BeanInfo, DboBeans> beanMap = new HashMap<BeanInfo, DboBeans>();
				
				// Enumeration of the beans
				ConvertigoPlugin.logDebug2("Exploring Convertigo database objects list...");

				DboExplorerManager manager = new DboExplorerManager();
				List<DboGroup> groups = manager.getGroups();
				Set<DboBeans> beansCategorySet = new LinkedHashSet<DboBeans>();
				for (DboGroup group : groups) {
					List<DboCategory> categories = group.getCategories();
					for (DboCategory category : categories) {
						List<DboBeans> beansCategories	= category.getBeans();
						for (DboBeans beansCategory : beansCategories) {
							List<DboBean> beans = beansCategory.getBeans();
							for (DboBean bean : beans) {
								if (!bean.isEnable()) continue;
								
								String className = bean.getClassName();
								if (bean.isDefault()) {
									defaultDboList.add(className);
								}
								if (bean.isDocumented()) {
									documentedDboList.add(className);
								}
								
								try {
									Class<DatabaseObject> beanClass = GenericUtils.cast(Class.forName(className));
									ConvertigoPlugin.logDebug2("Bean class: "
											+ (beanClass != null ? beanClass.getName() : "null"));

									// The bean should derived from DatabaseObject...
									boolean isDatabaseObject = (DatabaseObject.class.isAssignableFrom(beanClass));

									if (isDatabaseObject) {
										// ... and should derived from the specified class
										boolean isFromSpecifiedClass = ((databaseObjectClass == null) ||
												((databaseObjectClass != null) && (databaseObjectClass.isAssignableFrom(beanClass))));
										if (isFromSpecifiedClass) {
											// Check parent
											Collection<DboParent> parents = bean.getParents();
											boolean bFound = false;
											for (DboParent possibleParent : parents) {
												// Check if parent allow inheritance
												if (Class.forName(possibleParent.getClassName()).equals(parentObjectClass)||
													possibleParent.allowInheritance() && Class.forName(possibleParent.getClassName()).isAssignableFrom(parentObjectClass)) {
														bFound = true;
														break;
												}
											}

											if (bFound) {
												// Check technology if needed
												if (technology != null) {
													Collection<String> acceptedTechnologies = bean.getEmulatorTechnologies();
	
													if (!acceptedTechnologies.isEmpty() && !acceptedTechnologies.contains(technology)) {
														continue;
													}
												}
	
												String beanInfoClassName = className + "BeanInfo";
												Class<BeanInfo> beanInfoClass = GenericUtils.cast(Class
														.forName(beanInfoClassName));
												if (beanInfoClass != null) {
													beanMap.put(beanInfoClass.newInstance(), beansCategory);
													beansCategorySet.add(beansCategory);
												} else {
													String message = java.text.MessageFormat.format(
															"The \"{0}\" does not exist.",
															new Object[] { beanInfoClassName });
													ConvertigoPlugin.logWarning(message);
												}
											}
										}
									} else {
										String message = java.text.MessageFormat.format(
												"The \"{0}\" class is not a Convertigo database object.",
												new Object[] { className });
										ConvertigoPlugin.logWarning(message);
									}
								} catch (ClassNotFoundException e) {
									String message = java.text.MessageFormat.format(
											"Unable to analyze the \"{0}\" class.\n\nClass not found: {1}", new Object[] { className, e.getMessage() });
									ConvertigoPlugin.logWarning(message);
								}
								/*
								 * catch(IntrospectionException e) { String
								 * message =java.text.MessageFormat.format(
								 * "Unable to find the bean info linked to the \"{0}\" class."
								 * , new Object[] {className});
								 * ConvertigoPlugin.logWarning(message); }
								 */
								catch (Throwable e) {
									String message = java.text.MessageFormat.format(
											"Unable to analyze the \"{0}\" Convertigo database object.",
											new Object[] { className });
									ConvertigoPlugin.logException(e, message);
								}
							}
						}
					}
				}
				
				List<BeanInfo> beanInfoList = new ArrayList<BeanInfo>(beanMap.keySet());
				Collections.sort(beanInfoList, new Comparator<BeanInfo>() {
        			public int compare(BeanInfo o1, BeanInfo o2) {
                        BeanDescriptor bd1 = o1.getBeanDescriptor();
                        BeanDescriptor bd2 = o2.getBeanDescriptor();
                        String name1 = bd1.getDisplayName().toLowerCase();
                        String name2 = bd2.getDisplayName().toLowerCase();
        				return name1.compareTo(name2);
        			}
				});
				
				//initialize composites.
				RowLayout rowLayout = new RowLayout();
				rowLayout.pack = false;
				bar = new ExpandBar(scrolledComposite, SWT.NONE);
				bar.setSpacing(8);
				String itemText = "";
				int i=0;
				Iterator<DboBeans> iterator = beansCategorySet.iterator();
				if (databaseObjectClass.equals(Connector.class)) {
					composites = new Composite[1];
					items = new ExpandItem[1];
					composites[i] = new Composite(bar, SWT.NONE);
					composites[i].setLayout(rowLayout);
					composites[i].setBackground(BACKGROUND_COLOR);
					items[i] = new ExpandItem(bar, SWT.NONE, i);
					items[i].setControl(composites[i]);
					items[i].setExpanded(true);
					items[i].setText("Connectors");
				} else {
					composites = new Composite[beansCategorySet.size()];
					items = new ExpandItem[beansCategorySet.size()];
					while(iterator.hasNext())
					{
						composites[i] = new Composite(bar, SWT.NONE);
						composites[i].setLayout(rowLayout);
						composites[i].setBackground(BACKGROUND_COLOR);
						items[i] = new ExpandItem(bar, SWT.NONE, i);
						items[i].setControl(composites[i]);
						items[i].setExpanded(true);
						itemText = iterator.next().getName();
						if (itemText.equals("")) {
							itemText = databaseObjectClass.getSimpleName();
							if (beanInfoList.size() > 1) {
								itemText.concat("s");
							}
						}
						items[i].setText(itemText);
						i++;
					}
				}
				
				handCursor = new Cursor(Display.getDefault(), SWT.CURSOR_HAND);

				boolean bSelected = false;
				boolean defaultDboFound = false;
				Iterator<BeanInfo> it = beanInfoList.iterator();
				
				
				while(it.hasNext()) {
					BeanInfo beanInfo = it.next();
					DboBeans beanCategory = beanMap.get(beanInfo);
					
					Class<DatabaseObject> beanClass = GenericUtils.cast(beanInfo.getBeanDescriptor().getBeanClass());
					boolean isDefault = defaultDboList.contains(beanClass.getName());
					boolean isDocumented = documentedDboList.contains(beanClass.getName());
					String beanName = beanInfo.getBeanDescriptor().getDisplayName();
					String beanDescription = isDocumented ? beanInfo.getBeanDescriptor().getShortDescription():"Not yet documented |";
					String[] beanDescriptions = beanDescription.split("\\|");
					String beanShortDescription = BeansUtils.cleanDescription(beanDescriptions[0], false);

					Image beanImage = ConvertigoPlugin.getDefault().getBeanIcon(beanInfo, BeanInfo.ICON_COLOR_32x32);

					if (isDefault) {
						bSelected = true;
						defaultDboFound = true;
					}
					
					addLabelEx(beanImage, beanClass, beanName, beanShortDescription, bSelected, beanInfo, beanCategory);
					
					bSelected = false;
				}
				
				// We select by default the first item if no default dbo found.
				if (!defaultDboFound && currentSelectedObject == null) {
					currentSelectedObject = (CLabel) composites[0].getChildren()[0];
					currentSelectedObject.setForeground(FOREGROUND_SELECTED_COLOR);
					currentSelectedObject.setBackground(BACKGROUND_SELECTED_COLOR);
		
					BeanInfo currentSelectedObjectBeanInfo = getCurrentSelectedBeanInfo();
					if (currentSelectedObjectBeanInfo != null) {
						updateHelpText(currentSelectedObjectBeanInfo);
					}
				}
				
			} catch (Exception e) {
				ConvertigoPlugin.logException(e, "Unable to load database objects properties.");
			}
		}
	}

	public BeanInfo getCurrentSelectedBeanInfo() {
		BeanInfo bi = null;
		if (currentSelectedObject != null) {
			bi = (BeanInfo) objectsMap.get(currentSelectedObject);
		}
		return bi;
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
		
		helpBrowser = new C8oBrowser(this, SWT.BORDER | SWT.MULTI | SWT.WRAP);
		gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.verticalAlignment = GridData.FILL;
		gridData.grabExcessVerticalSpace = true;
		gridData.grabExcessHorizontalSpace = true;
		helpBrowser.setLayoutData(gridData);

		GridLayout gridLayout = new GridLayout(3, true);
		gridLayout.verticalSpacing = 10;
		gridLayout.horizontalSpacing = 10;

		// retrieve 'project' technology
		technology = DboUtils.getTechnology(parentObject, databaseObjectClass);

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

	private void updateHelpText(BeanInfo bi) {
		BeanDescriptor beanDescriptor = bi.getBeanDescriptor();
		boolean isDocumented = documentedDboList.contains(beanDescriptor.getBeanClass().getName());
		String beanDescription = isDocumented ? beanDescriptor.getShortDescription():"Not yet documented. |";
		String[] beanDescriptions = beanDescription.split("\\|");
		String beanDisplayName = beanDescriptor.getDisplayName();
		String beanShortDescription = beanDescriptions.length >= 1 ? beanDescriptions[0] : "n/a";
		String beanLongDescription = beanDescriptions.length >= 2 ? beanDescriptions[1] : "n/a";

		beanShortDescription = BeansUtils.cleanDescription(beanShortDescription, true);
		beanLongDescription = BeansUtils.cleanDescription(beanLongDescription, true);

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

	protected void addLabelEx(Image beanImage, String beanName, String beanShortDescription, boolean selected,
			Object object) {
		CLabel label = new CLabel(compositeObjects, SWT.NONE);
		label.setImage(beanImage);
		label.setText(beanName);
		label.setAlignment(SWT.LEFT);
		label.setToolTipText(beanShortDescription);
		label.setForeground(FOREGROUND_COLOR);
		label.setBackground(this.getBackground());
		label.setCursor(handCursor);

		RowData rowData = new RowData();
		label.setLayoutData(rowData);
		objectsMap.put(label, object);

		// We select by default the first item
		if (currentSelectedObject == null) {
			currentSelectedObject = label;

			currentSelectedObject.setForeground(FOREGROUND_SELECTED_COLOR);
			currentSelectedObject.setBackground(BACKGROUND_SELECTED_COLOR);

			BeanInfo currentSelectedObjectBeanInfo = getCurrentSelectedBeanInfo();
			if (currentSelectedObjectBeanInfo != null) {
				updateHelpText(currentSelectedObjectBeanInfo);
			}
		}

		label.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
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

				BeanInfo currentSelectedObjectBeanInfo = getCurrentSelectedBeanInfo();
				if (currentSelectedObjectBeanInfo != null) {
					updateHelpText(currentSelectedObjectBeanInfo);
				}
			}

			@Override
			public void mouseDoubleClick(MouseEvent e) {
				currentSelectedObject = (CLabel) e.getSource();

				if (wizardPage != null) {
					wizardPage.setPageComplete(true);
					((ObjectExplorerWizardPage) wizardPage).showNextPage();
				}
			}
		});

		ConvertigoPlugin.logDebug("Loaded '" + beanName + "'.");
	}

	protected void addLabelEx(Image beanImage, Class<DatabaseObject> beanClass, String beanName,
			String beanShortDescription, boolean selected, Object object, DboBeans beansCategory) {
	
		if("".equals(beansCategory.getName())) {
			composite = composites[0];
		} else {
			for(int i=0; i < items.length; i++) {
				if(items[i].getText().equals(beansCategory.getName())) {
					composite = composites[i];
				}
			}
		}
		
		CLabel label = new CLabel(composite, SWT.NONE);
		label.setImage(beanImage);
		label.setText(beanName);
		label.setAlignment(SWT.LEFT);
		label.setToolTipText(removeTag.matcher(beanShortDescription).replaceAll(""));
		label.setCursor(handCursor);

		label.setLayoutData(new RowData());
		objectsMap.put(label, object);
		
		if (selected) {
			currentSelectedObject = label;

			currentSelectedObject.setForeground(FOREGROUND_SELECTED_COLOR);
			currentSelectedObject.setBackground(BACKGROUND_SELECTED_COLOR);

			BeanInfo currentSelectedObjectBeanInfo = getCurrentSelectedBeanInfo();
			if (currentSelectedObjectBeanInfo != null) {
				updateHelpText(currentSelectedObjectBeanInfo);
			}
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

				BeanInfo currentSelectedObjectBeanInfo = getCurrentSelectedBeanInfo();
				if (currentSelectedObjectBeanInfo != null) {
					updateHelpText(currentSelectedObjectBeanInfo);
				}
			}

			@Override
			public void mouseDoubleClick(MouseEvent e) {
				currentSelectedObject = (CLabel) e.getSource();

				if (wizardPage != null) {
					wizardPage.setPageComplete(true);
					((ObjectExplorerWizardPage) wizardPage).showNextPage();
				}
			}
		});

		ConvertigoPlugin.logDebug("Loaded '" + beanName + "'.");
	}
	

} // @jve:decl-index=0:visual-constraint="10,10"
