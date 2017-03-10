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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Text;
import org.w3c.dom.Document;

import com.twinsoft.convertigo.beans.common.DefaultBlockFactory;
import com.twinsoft.convertigo.beans.common.EmulatorTechnology;
import com.twinsoft.convertigo.beans.connectors.CicsConnector;
import com.twinsoft.convertigo.beans.connectors.CouchDbConnector;
import com.twinsoft.convertigo.beans.connectors.HtmlConnector;
import com.twinsoft.convertigo.beans.connectors.HttpConnector;
import com.twinsoft.convertigo.beans.connectors.JavelinConnector;
import com.twinsoft.convertigo.beans.connectors.SapJcoConnector;
import com.twinsoft.convertigo.beans.connectors.SiteClipperConnector;
import com.twinsoft.convertigo.beans.connectors.SqlConnector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Criteria;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.ExtractionRule;
import com.twinsoft.convertigo.beans.core.IScreenClassContainer;
import com.twinsoft.convertigo.beans.core.MobileApplication;
import com.twinsoft.convertigo.beans.core.MobilePlatform;
import com.twinsoft.convertigo.beans.core.Pool;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Reference;
import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Sheet;
import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.TestCase;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.core.UrlMapper;
import com.twinsoft.convertigo.beans.core.UrlMapping;
import com.twinsoft.convertigo.beans.core.UrlMappingOperation;
import com.twinsoft.convertigo.beans.core.UrlMappingParameter;
import com.twinsoft.convertigo.beans.core.UrlMappingResponse;
import com.twinsoft.convertigo.beans.core.Variable;
import com.twinsoft.convertigo.beans.mobile.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.mobile.components.PageComponent;
import com.twinsoft.convertigo.beans.mobile.components.RouteActionComponent;
import com.twinsoft.convertigo.beans.mobile.components.RouteEventComponent;
import com.twinsoft.convertigo.beans.mobile.components.RouteComponent;
import com.twinsoft.convertigo.beans.mobile.components.UIComponent;
import com.twinsoft.convertigo.beans.references.ImportXsdSchemaReference;
import com.twinsoft.convertigo.beans.references.RestServiceReference;
import com.twinsoft.convertigo.beans.references.WebServiceReference;
import com.twinsoft.convertigo.beans.references.XsdSchemaReference;
import com.twinsoft.convertigo.beans.screenclasses.HtmlScreenClass;
import com.twinsoft.convertigo.beans.screenclasses.JavelinScreenClass;
import com.twinsoft.convertigo.beans.screenclasses.SiteClipperScreenClass;
import com.twinsoft.convertigo.beans.statements.ContinueWithSiteClipperStatement;
import com.twinsoft.convertigo.beans.statements.ElseStatement;
import com.twinsoft.convertigo.beans.statements.HTTPStatement;
import com.twinsoft.convertigo.beans.statements.HandlerStatement;
import com.twinsoft.convertigo.beans.statements.IThenElseStatementContainer;
import com.twinsoft.convertigo.beans.statements.ThenStatement;
import com.twinsoft.convertigo.beans.steps.ElseStep;
import com.twinsoft.convertigo.beans.steps.IThenElseContainer;
import com.twinsoft.convertigo.beans.steps.SequenceStep;
import com.twinsoft.convertigo.beans.steps.ThenStep;
import com.twinsoft.convertigo.beans.steps.TransactionStep;
import com.twinsoft.convertigo.beans.transactions.AbstractHttpTransaction;
import com.twinsoft.convertigo.beans.transactions.CicsTransaction;
import com.twinsoft.convertigo.beans.transactions.HtmlTransaction;
import com.twinsoft.convertigo.beans.transactions.HttpTransaction;
import com.twinsoft.convertigo.beans.transactions.JavelinTransaction;
import com.twinsoft.convertigo.beans.transactions.SapJcoLogonTransaction;
import com.twinsoft.convertigo.beans.transactions.SapJcoTransaction;
import com.twinsoft.convertigo.beans.transactions.SiteClipperTransaction;
import com.twinsoft.convertigo.beans.transactions.SqlTransaction;
import com.twinsoft.convertigo.beans.transactions.couchdb.AbstractCouchDbTransaction;
import com.twinsoft.convertigo.beans.transactions.couchdb.CouchVariable;
import com.twinsoft.convertigo.beans.transactions.couchdb.GetServerInfoTransaction;
import com.twinsoft.convertigo.beans.variables.RequestableHttpVariable;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.wizards.new_project.EmulatorTechnologyWizardPage;
import com.twinsoft.convertigo.eclipse.wizards.new_project.SQLQueriesWizardPage;
import com.twinsoft.convertigo.eclipse.wizards.new_project.ServiceCodeWizardPage;
import com.twinsoft.convertigo.eclipse.wizards.new_project.UrlMappingWizardPage;
import com.twinsoft.convertigo.eclipse.wizards.references.ProjectSchemaWizardPage;
import com.twinsoft.convertigo.eclipse.wizards.references.RestServiceWizardPage;
import com.twinsoft.convertigo.eclipse.wizards.references.WebServiceWizardPage;
import com.twinsoft.convertigo.eclipse.wizards.references.WsdlSchemaFileWizardPage;
import com.twinsoft.convertigo.eclipse.wizards.references.XsdSchemaFileWizardPage;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.HttpMethodType;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.ImportWsReference;
import com.twinsoft.convertigo.engine.util.StringUtils;

public class NewObjectWizard extends Wizard {
	
	private String className = "java.lang.Object";
	private DatabaseObject parentObject = null; 
	private String xpath = null;
	private Document dom = null;
	
    private ObjectExplorerWizardPage objectExplorerPage = null;
    private ObjectInfoWizardPage objectInfoPage = null;
    private SQLQueriesWizardPage sqlQueriesWizardPage = null;
    
	public Button useAuthentication = null;
	public Text loginText = null, passwordText = null;
    public DatabaseObject newBean = null;

    public NewObjectWizard(DatabaseObject selectedDatabaseObject, String newClassName, String xpath, Document dom) {
    	this(selectedDatabaseObject, newClassName);
		this.xpath = xpath;
		this.dom = dom;
	}

    public NewObjectWizard(DatabaseObject selectedDatabaseObject, String newClassName) {
		super();
		this.parentObject = selectedDatabaseObject;
		this.className = newClassName;
		setWindowTitle("Create a new object");
		setNeedsProgressMonitor(true);
		setHelpAvailable(true);
	}

    
	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
    @Override
	public void addPages() {
		try {
			String objectExplorerPageTitle = "", objectExplorerPageMessage = "";
			Class<DatabaseObject> beanClass = GenericUtils.cast(Class.forName(className));
			
			if (beanClass.equals(Project.class)) {
				objectExplorerPageTitle = "New Project";
				objectExplorerPageMessage = "Please select a project template.";
			}
			else if (beanClass.equals(Sequence.class)) {
				objectExplorerPageTitle = "New Sequence";
				objectExplorerPageMessage = "Please select a sequence template.";
			}
			else if (beanClass.equals(Connector.class)) {
				objectExplorerPageTitle = "New Connector";
				objectExplorerPageMessage = "Please select a connector template.";
			}
			else if (beanClass.equals(Transaction.class)) {
				objectExplorerPageTitle = "New Transaction";
				objectExplorerPageMessage = "Please select a transaction template.";
			}
			else if (beanClass.equals(Pool.class)) {
				objectExplorerPageTitle = "New Pool";
				objectExplorerPageMessage = "Please select a pool template.";
			}
			else if (beanClass.equals(ScreenClass.class)) {
				objectExplorerPageTitle = "New Screen Class";
				objectExplorerPageMessage = "Please select a screen class template.";
			}
			else if (beanClass.equals(Criteria.class)) {
				objectExplorerPageTitle = "New Criteria";
				objectExplorerPageMessage = "Please select a criteria template.";
			}
			else if (beanClass.equals(ExtractionRule.class)) {
				objectExplorerPageTitle = "New Extraction Rule";
				objectExplorerPageMessage = "Please select an extraction rule template.";
			}
			else if (beanClass.equals(Sheet.class)) {
				objectExplorerPageTitle = "New Sheet";
				objectExplorerPageMessage = "Please select a sheet template.";
			}
			else if (beanClass.equals(Statement.class)) {
				objectExplorerPageTitle = "New Statement";
				objectExplorerPageMessage = "Please select a statement template.";
			}
			else if (beanClass.equals(Step.class)) {
				objectExplorerPageTitle = "New Step";
				objectExplorerPageMessage = "Please select a step template.";
			}
			else if (beanClass.equals(Variable.class)) {
				objectExplorerPageTitle = "New Variable";
				objectExplorerPageMessage = "Please select a variable template.";
			}
			else if (beanClass.equals(TestCase.class)) {
				objectExplorerPageTitle = "New Test case";
				objectExplorerPageMessage = "Please select a test case template.";
			}
			else if (beanClass.equals(Reference.class)) {
				objectExplorerPageTitle = "New Reference";
				objectExplorerPageMessage = "Please select a reference template.";
			}
			else if (beanClass.equals(MobileApplication.class)) {
				objectExplorerPageTitle = "New Mobile Application";
				objectExplorerPageMessage = "Please select a mobile application template.";
			}
			else if (beanClass.equals(MobilePlatform.class)) {
				objectExplorerPageTitle = "New Mobile Platform";
				objectExplorerPageMessage = "Please select a mobile platform template.";
			}
			else if (beanClass.equals(ApplicationComponent.class)) {
				objectExplorerPageTitle = "New Application Component";
				objectExplorerPageMessage = "Please select an application component template.";
			}
			else if (beanClass.equals(RouteComponent.class)) {
				objectExplorerPageTitle = "New Route Component";
				objectExplorerPageMessage = "Please select a route component template.";
			}
			else if (beanClass.equals(RouteEventComponent.class)) {
				objectExplorerPageTitle = "New Event Component";
				objectExplorerPageMessage = "Please select an event component template.";
			}
			else if (beanClass.equals(RouteActionComponent.class)) {
				objectExplorerPageTitle = "New Action Component";
				objectExplorerPageMessage = "Please select an action component template.";
			}
			else if (beanClass.equals(PageComponent.class)) {
				objectExplorerPageTitle = "New Page Component";
				objectExplorerPageMessage = "Please select a page component template.";
			}
			else if (beanClass.equals(UIComponent.class)) {
				objectExplorerPageTitle = "New UI Component";
				objectExplorerPageMessage = "Please select a UI component template.";
			}
			else if (beanClass.equals(com.twinsoft.convertigo.beans.core.Document.class)) {
				objectExplorerPageTitle = "New Document";
				objectExplorerPageMessage = "Please select a document template.";
			}
			else if (beanClass.equals(com.twinsoft.convertigo.beans.core.Listener.class)) {
				objectExplorerPageTitle = "New Listener";
				objectExplorerPageMessage = "Please select a listener template.";
			}
			else if (beanClass.equals(UrlMapper.class)) {
				objectExplorerPageTitle = "New URL Mapper";
				objectExplorerPageMessage = "Please select an URL mapper template.";
			}
			else if (beanClass.equals(UrlMapping.class)) {
				objectExplorerPageTitle = "New URL Mapping";
				objectExplorerPageMessage = "Please select an URL mapping template.";
			}
			else if (beanClass.equals(UrlMappingOperation.class)) {
				objectExplorerPageTitle = "New Mapping Operation";
				objectExplorerPageMessage = "Please select a mapping operation template.";
			}
			else if (beanClass.equals(UrlMappingParameter.class)) {
				objectExplorerPageTitle = "New Mapping Parameter";
				objectExplorerPageMessage = "Please select a mapping parameter template.";
			}
			else if (beanClass.equals(UrlMappingResponse.class)) {
				objectExplorerPageTitle = "New Mapping Response";
				objectExplorerPageMessage = "Please select a mapping response template.";
			}
			
			addBeanPages(objectExplorerPageTitle, objectExplorerPageMessage, beanClass);
		}
        catch (ClassNotFoundException e) {
            String message = java.text.MessageFormat.format("Unable to find the \"{0}\" class.", new Object[] {className});
            ConvertigoPlugin.logWarning(message);
        }
        finally {
        	;
        }
	}

	private void addBeanPages(String objectExplorerPageTitle, String objectExplorerPageMessage, Class<DatabaseObject> beanClass) {
		objectExplorerPage = new ObjectExplorerWizardPage(parentObject, beanClass, xpath);
		objectExplorerPage.setTitle(objectExplorerPageTitle);
		objectExplorerPage.setMessage(objectExplorerPageMessage);
		this.addPage(objectExplorerPage);
		
		objectInfoPage = new ObjectInfoWizardPage(parentObject);
		this.addPage(objectInfoPage);
		
		if ((xpath != null) && (dom != null)) {
			if (beanClass.equals(ExtractionRule.class)) {
				this.addPage(new XMLTableWizardPage(xpath, dom));
			}
		}
		if (parentObject instanceof SqlConnector) {
			sqlQueriesWizardPage = new SQLQueriesWizardPage();
			this.addPage(sqlQueriesWizardPage);
		}
		if (beanClass.equals(Connector.class)) {
			// add emulator technology wizard page
			EmulatorTechnologyWizardPage emulatorTechnologyPage = new EmulatorTechnologyWizardPage();
			this.addPage(emulatorTechnologyPage);
			// add service code wizard page
			ServiceCodeWizardPage serviceCodePage = new ServiceCodeWizardPage();
			this.addPage(serviceCodePage);
		}
		
		if (beanClass.equals(Reference.class)) {
			ProjectSchemaWizardPage projectSchemaWizardPage = new ProjectSchemaWizardPage(parentObject);
			this.addPage(projectSchemaWizardPage);
			
			XsdSchemaFileWizardPage xsdSchemaFileWizardPage = new XsdSchemaFileWizardPage(parentObject);
			this.addPage(xsdSchemaFileWizardPage);
			
			WsdlSchemaFileWizardPage wsdlSchemaWizardPage = new WsdlSchemaFileWizardPage(parentObject);
			this.addPage(wsdlSchemaWizardPage);
			
			WebServiceWizardPage soapServiceWizardPage = new WebServiceWizardPage(parentObject);
			this.addPage(soapServiceWizardPage);

			RestServiceWizardPage restServiceWizardPage = new RestServiceWizardPage(parentObject);
			this.addPage(restServiceWizardPage);
		}
		
		if (beanClass.equals(UrlMapping.class)) {
			UrlMappingWizardPage urlMappingWizardPage = new UrlMappingWizardPage(parentObject);
			this.addPage(urlMappingWizardPage);
		}
	}
	
	@Override
	public boolean isHelpAvailable() {
		return false;// removes "Help" rectangular button next to other wizard buttons
	}

	private DatabaseObject getCreatedBean() {
		DatabaseObject dbo = null;
		if (objectExplorerPage != null) {
			dbo = objectExplorerPage.getCreatedBean();
		}
		return dbo;
	}
	
	private Class<?> getCreatedBeanClass() {
		if (objectExplorerPage != null) {
			return objectExplorerPage.getCreatedBeanClass();
		}
		return null;
	}
	
	@Override
	public boolean canFinish() {
		IWizardPage nextPage = getPage("SQLQueriesWizardPage");
		if (nextPage!=null) {
			return (getContainer().getCurrentPage().getNextPage() == getPage("SQLQueriesWizardPage") || 
				getContainer().getCurrentPage().getNextPage() == null && getContainer().getCurrentPage().isPageComplete());
		} else {
			return (getContainer().getCurrentPage().getNextPage() == null && getContainer().getCurrentPage().isPageComplete());
		}
	}

	private void doFinish(IProgressMonitor monitor) throws CoreException {
		String dboName, name;
		boolean bContinue = true;
		int index = 0;

		try {
			int total = 0;
			Class<?> c = getCreatedBeanClass();
			if (c != null) {
				total = 4;
				if (c.equals(WebServiceReference.class)) {
					total += ImportWsReference.getTotalTaskNumber();
				}
			}
			monitor.beginTask("Creating new object", total);
			
			newBean = getCreatedBean();
            if (newBean != null) {
    			monitor.setTaskName("Object created");
    			monitor.worked(1);
    			
            	dboName = newBean.getName();
				if (!StringUtils.isNormalized(dboName))
					throw new EngineException("Bean name is not normalized : \""+dboName+"\".");
            	
				while (bContinue) {
					if (index == 0) name = dboName;
					else name = dboName + index;
					newBean.setName(name);
					newBean.hasChanged = true;
					newBean.bNew = true;
					
					try {
						if ((newBean instanceof Statement) && (parentObject instanceof Transaction)) {
							newBean.priority = 0;
							newBean.newPriority = 0;
						}
						
						if (newBean instanceof ScreenClass)
							newBean.priority = parentObject.priority + 1;
						
						if (newBean instanceof Criteria) {
							Connector connector = parentObject.getConnector();
							if (parentObject.equals(((IScreenClassContainer<?>)connector).getDefaultScreenClass()))
								throw new EngineException("You cannot add a new criterion on default screenclass.");
						}
							
						parentObject.add(newBean);
		    			monitor.setTaskName("Object added");
		    			monitor.worked(1);
						
						if (newBean instanceof HTTPStatement) {
							HTTPStatement httpStatement = (HTTPStatement)newBean;
							HtmlConnector connector = (HtmlConnector)httpStatement.getParentTransaction().getParent();
							httpStatement.setMethod("GET");
							httpStatement.setHost(connector.getServer());
							httpStatement.setPort(connector.getPort());
							httpStatement.setHttps(connector.isHttps());
						}

						if (newBean instanceof ContinueWithSiteClipperStatement) {
							Project project = newBean.getProject();	
							if (project != null) {
								
								String[] connectorWithSiteClipperConnector = ContinueWithSiteClipperStatement
										.getSiteClippersConnectorNames(project);
								if (connectorWithSiteClipperConnector.length > 0) {
									((ContinueWithSiteClipperStatement) newBean)
											.setSiteClipperConnectorName(connectorWithSiteClipperConnector[0]);
								}
							}
						}
						
						if (newBean instanceof Connector) {
							Project project = (Project)parentObject;
							if (project.getDefaultConnector() == null)
								project.setDefaultConnector((Connector)newBean);
							
							this.setupConnector(newBean);
						}
						
						if (newBean instanceof PageComponent) {
							ApplicationComponent application = (ApplicationComponent)parentObject;
							if (application.getRootPage() == null)
								application.setRootPage((PageComponent)newBean);
						}
						
						if (newBean instanceof SequenceStep) {
							Project project = newBean.getProject();
							
							((SequenceStep) newBean).setSourceSequence(project.getName() + TransactionStep.SOURCE_SEPARATOR +
									project.getSequencesList().get(0));
						}
						
						if (newBean instanceof TransactionStep) {
							Project project = newBean.getProject();
							Connector connector = project.getDefaultConnector();
							Transaction transaction = connector.getDefaultTransaction();
							
							((TransactionStep) newBean).setSourceTransaction(
									project.getName() + TransactionStep.SOURCE_SEPARATOR +
									connector.getName() + TransactionStep.SOURCE_SEPARATOR +
									transaction.getName());
						}
						
						if (newBean instanceof IThenElseContainer) {
							ThenStep thenStep = new ThenStep();
							((IThenElseContainer)newBean).addStep(thenStep);
							
							ElseStep elseStep = new ElseStep();
							((IThenElseContainer)newBean).addStep(elseStep);
						}						
						
						if (newBean instanceof IThenElseStatementContainer) {
							ThenStatement thenStatement = new ThenStatement();
							((IThenElseStatementContainer)newBean).addStatement(thenStatement);
							
							ElseStatement elseStatement = new ElseStatement();
							((IThenElseStatementContainer)newBean).addStatement(elseStatement);
						}
						
						if (newBean instanceof Sheet) {
							InputStream is = null;
							try {
								String sheetName = newBean.getName()+".xsl";
								is = new FileInputStream(new File(Engine.XSL_PATH + "/customsheet.xsl"));
								String projectName = ((DatabaseObject)parentObject).getProject().getName();
								IProject project = ConvertigoPlugin.getDefault().getProjectPluginResource(projectName);
								final IFile file = project.getFile(sheetName);
								if (!file.exists()) file.create(is, true, null);
								((Sheet)newBean).setUrl(sheetName);
							} catch (Exception e) {}
							finally {
								if (is != null) {
									try {is.close();}
									catch (IOException e) {}
								}
							}
						}
						
						if (newBean instanceof TestCase) {
							TestCase testCase = (TestCase)newBean;
		    				testCase.importRequestableVariables((RequestableObject)testCase.getParent());
						}
						
						if (newBean instanceof RequestableHttpVariable) {
							RequestableHttpVariable variable = (RequestableHttpVariable)newBean;
							AbstractHttpTransaction httpTransaction = (AbstractHttpTransaction) variable.getParent();
							HttpMethodType httpMethodType = httpTransaction.getHttpVerb();
							boolean isVarPost = httpMethodType.equals(HttpMethodType.PUT) || httpMethodType.equals(HttpMethodType.POST);
							variable.setHttpMethod(isVarPost ? HttpMethodType.POST.name() : HttpMethodType.GET.name());
							if (!(httpTransaction instanceof HtmlTransaction)) {
								variable.setHttpName(variable.getName());
							}
						}
						
						if (newBean instanceof WebServiceReference) {
							try {
								Project project = (Project)parentObject;
								WebServiceReference webServiceReference = (WebServiceReference)newBean;
								ImportWsReference wsr = new ImportWsReference(webServiceReference);
								wsr.importInto(project);
							} catch (Exception e){
								if (newBean != null) {
									parentObject.remove(newBean);
								}
								throw new Exception(e.getMessage());
							}
						}
						
						if (newBean instanceof RestServiceReference) {
							try {
								Project project = (Project)parentObject;
								RestServiceReference restServiceReference = (RestServiceReference)newBean;
								ImportWsReference wsr = new ImportWsReference(restServiceReference);
								wsr.importInto(project);
							} catch (Exception e){
								if (newBean != null) {
									parentObject.remove(newBean);
								}
								throw new Exception(e.getMessage());
							}
						}
						
						if (newBean instanceof SqlTransaction) {
							SqlTransaction sqlTransaction = (SqlTransaction)newBean;
							sqlTransaction.setSqlQuery(sqlQueriesWizardPage.getSQLQueries());
							sqlTransaction.initializeQueries(true);
						}
						
						if (newBean instanceof SapJcoLogonTransaction) {
							SapJcoLogonTransaction sapLogonTransaction = (SapJcoLogonTransaction)newBean;
							sapLogonTransaction.addCredentialsVariables();
						}

						if (newBean instanceof AbstractCouchDbTransaction) {
							AbstractCouchDbTransaction abstractCouchDbTransaction = (AbstractCouchDbTransaction) newBean;
							List<CouchVariable> selectedVariables = objectInfoPage.getSelectedParameters();
							abstractCouchDbTransaction.createVariables(selectedVariables);
						}

						ConvertigoPlugin.logInfo("New object class '"+ this.className +"' named '" + newBean.getName() + "' has been added");
		    			monitor.setTaskName("Object setted up");
		    			monitor.worked(1);

						bContinue = false;
					}
					catch(com.twinsoft.convertigo.engine.ObjectWithSameNameException owsne) {
						if (newBean instanceof HandlerStatement) {
							throw owsne;
						}
						index++;
					}
				}
            }
            else {
            	throw new Exception("Could not instantiate bean!");
            }
		}
		catch (Exception e) {
            String message = "Unable to create a new object from class '"+ this.className +"'.";
            ConvertigoPlugin.logException(e, message);
            newBean = null;
		}
	}
	
	public boolean performFinish() {
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					doFinish(monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		};

		try {
			getContainer().run(true, false, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			Throwable realException = e.getTargetException();
			MessageDialog.openError(getShell(), "Error", realException.getMessage());
			return false;
		}
		return true;
	}

    private void setupConnector(DatabaseObject connector) throws EngineException {
		
    	if (connector instanceof JavelinConnector) {
			JavelinConnector javelinConnector = (JavelinConnector) connector;
			
			JavelinScreenClass defaultScreenClass = new JavelinScreenClass();
			defaultScreenClass.setName("Default_screen_class");
			defaultScreenClass.hasChanged = true;
			defaultScreenClass.bNew = true;
			javelinConnector.setDefaultScreenClass(defaultScreenClass);

			DefaultBlockFactory blockFactory = new DefaultBlockFactory();
			blockFactory.setName("Block_factory");
			blockFactory.hasChanged = true;
			blockFactory.bNew = true;
			defaultScreenClass.setBlockFactory(blockFactory);

			EmulatorTechnology emulatorTechnology = new EmulatorTechnology();
			emulatorTechnology.hasChanged = true;
			emulatorTechnology.bNew = true;
			emulatorTechnology.setName("Emulator_technology");
			defaultScreenClass.add(emulatorTechnology);
			
			JavelinTransaction transaction = new JavelinTransaction();
			transaction.hasChanged = true;
			transaction.bNew = true;
			transaction.setName("XMLize");
			javelinConnector.add(transaction);
			javelinConnector.setDefaultTransaction(transaction);
		}
		else if (connector instanceof HtmlConnector) {
			HtmlConnector htmlConnector = (HtmlConnector) connector;
			htmlConnector.setServer("www.convertigo.com");
			
			HtmlScreenClass defaultScreenClass = new HtmlScreenClass();
			defaultScreenClass.setName("Default_screen_class");
			defaultScreenClass.hasChanged = true;
			defaultScreenClass.bNew = true;
			htmlConnector.setDefaultScreenClass(defaultScreenClass);
			
			HtmlTransaction transaction = new HtmlTransaction();
			transaction.hasChanged = true;
			transaction.bNew = true;
			transaction.setName("XMLize");
			htmlConnector.add(transaction);
			htmlConnector.setDefaultTransaction(transaction);
		}
		else if (connector instanceof HttpConnector) {
			HttpConnector httpConnector = (HttpConnector) connector;
			
			HttpTransaction transaction = new HttpTransaction();
			transaction.hasChanged = true;
			transaction.bNew = true;
			transaction.setName("Default_transaction");
			httpConnector.add(transaction);
			httpConnector.setDefaultTransaction(transaction);
		}
    	
		else if (connector instanceof SapJcoConnector) {
			SapJcoConnector sapConnector = (SapJcoConnector)connector;
			
			SapJcoLogonTransaction sapLogon = new SapJcoLogonTransaction();
			sapLogon.hasChanged = true;
			sapLogon.bNew = true;
			sapLogon.setName("Logon");
			sapLogon.addCredentialsVariables();
			sapConnector.add(sapLogon);
			sapConnector.setDefaultTransaction(sapLogon);
			
			SapJcoTransaction transaction = new SapJcoTransaction();
			transaction.hasChanged = true;
			transaction.bNew = true;
			transaction.setName("RFC_FUNCTION_SEARCH");
			transaction.setBapiName("RFC_FUNCTION_SEARCH");
			RequestableVariable variable = new RequestableVariable();
			variable.hasChanged = true;
			variable.bNew = true;
			variable.setName("FUNCNAME");
			variable.setValueOrNull("BAPI_*");
			transaction.add(variable);
			sapConnector.add(transaction);
		}
    	
		else if (connector instanceof SqlConnector) {
			SqlConnector sqlConnector = (SqlConnector)connector;
			sqlConnector.setJdbcDriverClassName("org.hsqldb.jdbcDriver");
			
			SqlTransaction transaction = new SqlTransaction();
			transaction.hasChanged = true;
			transaction.bNew = true;
			transaction.setName("Default_transaction");
			sqlConnector.add(transaction);
			sqlConnector.setDefaultTransaction(transaction);
		}
		else if (connector instanceof CicsConnector) {
			CicsConnector cicsConnector = (CicsConnector) connector;
			
			CicsTransaction transaction = new CicsTransaction();
			transaction.hasChanged = true;
			transaction.bNew = true;
			transaction.setName("Default_transaction");
			cicsConnector.add(transaction);
			cicsConnector.setDefaultTransaction(transaction);
		}
		else if (connector instanceof SiteClipperConnector) {
			SiteClipperConnector siteClipperConnector = (SiteClipperConnector) connector;
			
			SiteClipperScreenClass defaultScreenClass = new SiteClipperScreenClass();
			defaultScreenClass.setName("Default_screen_class");
			defaultScreenClass.hasChanged = true;
			defaultScreenClass.bNew = true;
			siteClipperConnector.setDefaultScreenClass(defaultScreenClass);
			
			SiteClipperTransaction transaction = new SiteClipperTransaction();
			transaction.hasChanged = true;
			transaction.bNew = true;
			transaction.setName("Default_transaction");
			siteClipperConnector.add(transaction);
			siteClipperConnector.setDefaultTransaction(transaction);
		}
		else if (connector instanceof CouchDbConnector) {
			CouchDbConnector couchDbConnector = (CouchDbConnector)connector;
			
			String couchDbXsdPath = AbstractCouchDbTransaction.COUCHDB_XSD_LOCATION;
			boolean existReference = false;
			for (Reference reference : couchDbConnector.getProject().getReferenceList()) {
				if (reference instanceof XsdSchemaReference) {
					String urlPath = ((XsdSchemaReference)reference).getUrlpath();
					if (urlPath.equals(couchDbXsdPath)) {
						existReference = true;
						break;
					}
				}
			}
			if (!existReference) {
				ImportXsdSchemaReference reference = new ImportXsdSchemaReference();
				reference.setName("CouchDb_schema");
				reference.setUrlpath(couchDbXsdPath);
				couchDbConnector.getProject().add(reference);
			}
			
			GetServerInfoTransaction transaction = new GetServerInfoTransaction();
			couchDbConnector.add(transaction);
			couchDbConnector.setDefaultTransaction(transaction);
		}
    }
    
}
