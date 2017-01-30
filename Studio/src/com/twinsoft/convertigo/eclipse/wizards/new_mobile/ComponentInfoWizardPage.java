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

import java.beans.IntrospectionException;
import java.util.List;

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
import com.twinsoft.convertigo.beans.connectors.HtmlConnector;
import com.twinsoft.convertigo.beans.connectors.JavelinConnector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IScreenClassContainer;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.statements.FunctionStatement;
import com.twinsoft.convertigo.beans.statements.HandlerStatement;
import com.twinsoft.convertigo.beans.statements.ScEntryHandlerStatement;
import com.twinsoft.convertigo.beans.statements.ScExitHandlerStatement;
import com.twinsoft.convertigo.beans.statements.ScHandlerStatement;
import com.twinsoft.convertigo.beans.transactions.couchdb.AbstractCouchDbTransaction;
import com.twinsoft.convertigo.beans.transactions.couchdb.CouchVariable;
import com.twinsoft.convertigo.eclipse.dialogs.CouchVariablesComposite;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.CachedIntrospector;
import com.twinsoft.convertigo.engine.util.StringUtils;

public class ComponentInfoWizardPage extends WizardPage {
	private Object parentObject = null;
	
	private Text beanName;
	private Tree tree;
	private String treeItemName = null;
	
	private CouchVariablesComposite couchVariablesComposite = null;
	private Composite container = null;
	
	public ComponentInfoWizardPage(Object parentObject) {
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
				dialogChanged();
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
					String suffix = getBeanName().endsWith(ScHandlerStatement.EVENT_ENTRY_HANDLER) ? 
							ScHandlerStatement.EVENT_ENTRY_HANDLER:
								getBeanName().endsWith(ScHandlerStatement.EVENT_EXIT_HANDLER) ?
										ScHandlerStatement.EVENT_EXIT_HANDLER : "";
					setBeanName("on"+ treeItemName + suffix);
					dialogChanged();
				}
			});
			tree.setVisible(false);
		}
		
		initialize();
		dialogChanged();
		setControl(container);
	}
	
	@Override
	public void performHelp() {
		getPreviousPage().performHelp();
	}

	private void initialize() {
		beanName.setText("");
	}
	
	public void fillTree(Class<? extends DatabaseObject> beanClass) {
		treeItemName = null;
		tree.removeAll();
		if (parentObject instanceof Transaction) {
			Connector connector = (Connector) ((Transaction)parentObject).getParent();
			boolean isScreenClassAware = connector instanceof IScreenClassContainer<?>;
			if (beanClass.equals(ScEntryHandlerStatement.class) || beanClass.equals(ScExitHandlerStatement.class)) {
				if (isScreenClassAware) {
					if (connector instanceof HtmlConnector) {
						HtmlConnector htmlConnector = (HtmlConnector) connector;
						ScreenClass defaultScreenClass = htmlConnector.getDefaultScreenClass();
						TreeItem branch = new TreeItem(tree, SWT.NONE);
						branch.setText(defaultScreenClass.getName());
						
						List<ScreenClass> screenClasses = defaultScreenClass.getInheritedScreenClasses();
						
						for (ScreenClass screenClass : screenClasses) {
							getInHeritedScreenClass(screenClass, branch);				
						}
					} else if (connector instanceof JavelinConnector) {
						JavelinConnector javelinConnector = (JavelinConnector) connector;
						ScreenClass defaultScreenClass = javelinConnector.getDefaultScreenClass();
						TreeItem branch = new TreeItem(tree, SWT.NONE);
						branch.setText(defaultScreenClass.getName());
						
						List<ScreenClass> screenClasses = defaultScreenClass.getInheritedScreenClasses();
						
						for (ScreenClass screenClass : screenClasses) {
							getInHeritedScreenClass(screenClass, branch);				
						}	
					}
					tree.setVisible(true);
				}
			}
			else if (beanClass.equals(HandlerStatement.class)) {
				TreeItem branch;
				
				branch = new TreeItem(tree, SWT.NONE);
				branch.setText(HandlerStatement.EVENT_TRANSACTION_STARTED);				

				branch = new TreeItem(tree, SWT.NONE);
				branch.setText(HandlerStatement.EVENT_XML_GENERATED);
				
				tree.setVisible(true);
			}
			else
				tree.setVisible(false);
		}
	}
	
	public void getInHeritedScreenClass(ScreenClass screenClass, TreeItem branch) {
		TreeItem leaf = new TreeItem(branch, SWT.NONE);
		leaf.setText(screenClass.getName());
		List<ScreenClass> screenClasses = screenClass.getInheritedScreenClasses();
		for (ScreenClass sC : screenClasses) {
			getInHeritedScreenClass(sC, leaf);
		}
	}
	
	private void dialogChanged() {
		DatabaseObject dbo = ((ComponentExplorerWizardPage)getWizard().getPage("ComponentExplorerWizardPage")).getCreatedBean();
		if (dbo != null) {
			/*if (dbo instanceof FunctionStatement) {
				beanName.setEnabled(true);
				if (dbo instanceof HandlerStatement) {
					beanName.setEnabled(false);
				}
			}*/
			
			String name = getBeanName();
			if (name.length() == 0) {
				updateStatus("Name must be specified");
				return;
			}
			
			if (!StringUtils.isNormalized(name)) {
				updateStatus("Name must be normalized.\nDon't start with number and don't use non ASCII caracters.");
				return;
			}
			
			try {
				dbo.setName(name);
				/*if (treeItemName != null) {
					if (dbo instanceof ScHandlerStatement)
						((ScHandlerStatement)dbo).setNormalizedScreenClassName(treeItemName);
					else if (dbo instanceof HandlerStatement)
						((HandlerStatement)dbo).setHandlerType(treeItemName);
				}*/
			} catch (EngineException e) {
				updateStatus("Name could not be set on bean");
				return;
			} catch (NullPointerException e) {
				updateStatus("New Bean has not been instanciated");
				return;
			}
		}
		updateStatus(null);
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}
	
	public IWizardPage getNextPage() {
		try {
			DatabaseObject dbo = ((ComponentExplorerWizardPage)getWizard().getPage("ComponentExplorerWizardPage")).getCreatedBean();
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