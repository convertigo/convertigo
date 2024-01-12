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

package com.twinsoft.convertigo.eclipse.wizards.new_mobile;

import java.beans.IntrospectionException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import com.twinsoft.convertigo.beans.connectors.CouchDbConnector;
import com.twinsoft.convertigo.beans.connectors.FullSyncConnector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.transactions.couchdb.AbstractCouchDbTransaction;
import com.twinsoft.convertigo.beans.transactions.couchdb.CouchVariable;
import com.twinsoft.convertigo.eclipse.dialogs.CouchVariablesComposite;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.ObjectWithSameNameException;
import com.twinsoft.convertigo.engine.util.CachedIntrospector;
import com.twinsoft.convertigo.engine.util.StringUtils;

class ComponentInfoWizardPage extends WizardPage {
	private Object parentObject = null;

	private Text beanName;
	private Tree tree;
	private String treeItemName = null;

	private CouchVariablesComposite couchVariablesComposite = null;
	private Composite container = null;

	ComponentInfoWizardPage(Object parentObject) {
		super("ComponentInfoWizardPage");
		this.parentObject = parentObject;
		setTitle("Informations");
		setDescription("Please enter a name for object.");
	}

	public void createControl(Composite parent) {
		container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 2;
		layout.verticalSpacing = 9;

		Label label = new Label(container, SWT.NULL);
		label.setText("&Name:");

		beanName = new Text(container, SWT.BORDER | SWT.SINGLE);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		beanName.setLayoutData(gd);
		beanName.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged(false);
			}
		});

		if (parentObject instanceof CouchDbConnector || parentObject instanceof FullSyncConnector) {
			couchVariablesComposite = new CouchVariablesComposite(container, SWT.V_SCROLL);

			GridData couchVarData = new GridData(GridData.FILL_BOTH);
			couchVarData.horizontalSpan = 2;

			couchVariablesComposite.setLayoutData(couchVarData);

		} else {
			tree = new Tree(container, SWT.SINGLE | SWT.BORDER);
			tree.setHeaderVisible(false);
			GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
			gridData.verticalSpan = 20;
			gridData.horizontalSpan = 2;
			tree.setLayoutData(gridData);
			tree.addListener(SWT.Selection, new Listener() {
				public void handleEvent(final Event event) {
					TreeItem item = (TreeItem) event.item;
					treeItemName = item.getText();
					setBeanName("on"+ treeItemName);
					dialogChanged(true);
				}
			});
			tree.setVisible(false);
		}

		initialize();
		dialogChanged(true);
		setControl(container);
	}

	private void initialize() {
		beanName.setText("");
	}

	void fillTree(Class<? extends DatabaseObject> beanClass) {
		treeItemName = null;
		tree.removeAll();
		if (parentObject instanceof Transaction) {
			tree.setVisible(false);
		}
	}
	
	private void dialogChanged(boolean increment) {
		DatabaseObject dbo = ((ComponentExplorerWizardPage)getWizard().getPage("ComponentExplorerWizardPage")).getCreatedBean();
		if (dbo != null) {
			String name = getBeanName();
			if (name.length() == 0) {
				updateStatus("Name must be specified");
				return;
			}

			if (!StringUtils.isNormalized(name)) {
				updateStatus("Name must be normalized.\nDon't start with number and don't use non ASCII caracters.");
				return;
			}

			Matcher m = Pattern.compile("\\d+$").matcher("");
			boolean sameName;
			do {
				sameName = false;
				try {
					dbo.setName(name);
				} catch (ObjectWithSameNameException e) {
					if (!increment) {
						updateStatus("Name already used by siblings");
						return;
					}
					sameName = true;
					m.reset(name);
					if (m.find()) {
						name = name.substring(0, m.start()) + (Integer.parseInt(m.group()) + 1);
					} else {
						name = name + "_1";
					}
					setBeanName(name);
				} catch (EngineException e) {
					updateStatus("Name could not be set on bean");
					return;
				} catch (NullPointerException e) {
					updateStatus("New Bean has not been instanciated");
					return;
				}
			} while (sameName);
		}
		updateStatus(null);
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}

	public IWizardPage getNextPage() {
		try {
			//			DatabaseObject dbo =
			((ComponentExplorerWizardPage) getWizard().getPage("ComponentExplorerWizardPage")).getCreatedBean();
			/*if (dbo instanceof XMLTable) {
				return getWizard().getPage("XMLTableWizardPage");
			}
			else if (dbo instanceof JavelinConnector) {
				return getWizard().getPage("EmulatorTechnologyWizardPage");
			}
			else if (dbo instanceof ProjectSchemaReference) {
				return getWizard().getPage("ProjectSchemaWizardPage");
			}
			else if (dbo instanceof RestServiceReference) {
				return getWizard().getPage("RestServiceWizardPage");
			}
			else if (dbo instanceof WebServiceReference) {
				return getWizard().getPage("WebServiceWizardPage");
			}
			else if (dbo instanceof WsdlSchemaReference) {
				return getWizard().getPage("WsdlSchemaFileWizardPage");
			}
			else if (dbo instanceof XsdSchemaReference) {
				return getWizard().getPage("XsdSchemaFileWizardPage");
			}
			else if (dbo instanceof SqlTransaction){
				return getWizard().getPage("SQLQueriesWizardPage");
			}
			else if (dbo instanceof UrlMapping){
				return getWizard().getPage("UrlMappingWizardPage");
			}*/
		}
		catch (NullPointerException e) {
			return null;
		}
		return null;
	}

	public String getBeanName() {
		return beanName.getText();
	}

	public void setBeanName(String name) {
		beanName.setText(name);
		dialogChanged(true);
	}

	@Override
	public void setVisible(boolean visible) {
		// TODO Auto-generated method stub
		super.setVisible(visible);

		if (visible) {
			ComponentExplorerWizardPage objectExplorerWizardPage = (ComponentExplorerWizardPage) this.getPreviousPage();

			try {
				Object o = objectExplorerWizardPage.getCreatedBean();
				if (o instanceof AbstractCouchDbTransaction) {
					AbstractCouchDbTransaction dbo = (AbstractCouchDbTransaction) objectExplorerWizardPage.getCreatedBean();
					if (dbo != null && couchVariablesComposite != null){
						couchVariablesComposite.setPropertyDescriptor(dbo, CachedIntrospector.getBeanInfo(dbo).getPropertyDescriptors(),
								(DatabaseObject) parentObject);
					}
				}
			} catch (IntrospectionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public List<CouchVariable> getSelectedParameters() {
		return couchVariablesComposite.getSelectedParameters();
	}


}