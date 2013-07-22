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

package com.twinsoft.convertigo.eclipse.views.projectexplorer;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.BlockFactory;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Criteria;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.DatabaseObject.ExportOption;
import com.twinsoft.convertigo.beans.core.ExtractionRule;
import com.twinsoft.convertigo.beans.core.IScreenClassContainer;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.beans.core.RequestableStep;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Sheet;
import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.beans.core.StatementWithExpressions;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepEvent;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.beans.core.TestCase;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.core.TransactionWithVariables;
import com.twinsoft.convertigo.beans.core.Variable;
import com.twinsoft.convertigo.beans.screenclasses.JavelinScreenClass;
import com.twinsoft.convertigo.beans.statements.ElseStatement;
import com.twinsoft.convertigo.beans.statements.FunctionStatement;
import com.twinsoft.convertigo.beans.statements.HTTPStatement;
import com.twinsoft.convertigo.beans.statements.ThenStatement;
import com.twinsoft.convertigo.beans.steps.ElseStep;
import com.twinsoft.convertigo.beans.steps.ThenStep;
import com.twinsoft.convertigo.beans.transactions.HtmlTransaction;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.engine.ConvertigoException;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.ObjectWithSameNameException;
import com.twinsoft.convertigo.engine.dbo_explorer.DboBean;
import com.twinsoft.convertigo.engine.dbo_explorer.DboBeans;
import com.twinsoft.convertigo.engine.dbo_explorer.DboCategory;
import com.twinsoft.convertigo.engine.dbo_explorer.DboExplorerManager;
import com.twinsoft.convertigo.engine.dbo_explorer.DboGroup;
import com.twinsoft.convertigo.engine.dbo_explorer.DboParent;
import com.twinsoft.convertigo.engine.dbo_explorer.DboUtils;
import com.twinsoft.convertigo.engine.helpers.WalkHelper;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class ClipboardManager2 {

	public int objectsType = ProjectExplorerView.TREE_OBJECT_TYPE_UNKNOWN;
	public Object[] objects;
	public TreeObject[] parentTreeNodeOfCutObjects;

	protected List<TreeObject> treeObjectsList;
	protected List<TreeObject> treeParentsList;
	protected Document clipboardDocument;
	protected Element clipboardRootElement;

	public boolean isCut = false;
	public boolean isCopy = false;

	public void reset() {
		objectsType = ProjectExplorerView.TREE_OBJECT_TYPE_UNKNOWN;
		objects = null;
		parentTreeNodeOfCutObjects = null;
		isCut = false;
		isCopy = false;
	}

	public String copy(TreePath[] treePaths) throws EngineException {
		ProjectExplorerView projectExplorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();
		
		clipboardDocument = XMLUtils.getDefaultDocumentBuilder().newDocument();
		
		ProcessingInstruction pi = clipboardDocument.createProcessingInstruction("xml", "version=\"1.0\" encoding=\"ISO-8859-1\"");
		clipboardDocument.appendChild(pi);
		
		clipboardRootElement = clipboardDocument.createElement("convertigo-clipboard");
		clipboardDocument.appendChild(clipboardRootElement);
		
		TreePath[] selectedPaths = ((treePaths == null) ? projectExplorerView.getSelectionPaths() : treePaths);
		treeObjectsList = new ArrayList<TreeObject>();
		treeParentsList = new ArrayList<TreeObject>();
		
		for (int i = 0 ; i < selectedPaths.length ; i++) {
			TreeObject treeObject = (TreeObject)selectedPaths[i].getLastPathComponent();
			if (treeObject instanceof DatabaseObjectTreeObject) {
				DatabaseObjectTreeObject databaseObjectTreeObject = (DatabaseObjectTreeObject) treeObject;
				DatabaseObjectTreeObject parentDatabaseObjectTreeObject = databaseObjectTreeObject.getParentDatabaseObjectTreeObject();
				treeObjectsList.add(databaseObjectTreeObject);
				treeParentsList.add(parentDatabaseObjectTreeObject);
				copyDatabaseObject((DatabaseObject) databaseObjectTreeObject.getObject());
			} else if (treeObject instanceof IPropertyTreeObject) {
				IPropertyTreeObject propertyTreeObject = (IPropertyTreeObject) treeObject;
				treeObjectsList.add(treeObject);
				treeParentsList.add(((IPropertyTreeObject) treeObject).getTreeObjectOwner());
				copyPropertyObject(propertyTreeObject);
			} else {
				throw new EngineException("Tree item not supported :"+ treeObject.getClass().getName());
			}
		}
		
		objects = treeObjectsList.toArray(new Object[selectedPaths.length]);
		parentTreeNodeOfCutObjects = treeParentsList.toArray(new TreeObject[selectedPaths.length]);
		
		String strObject = XMLUtils.prettyPrintDOM(clipboardDocument);
		return strObject;
	}

	DatabaseObject currentScreenClassOrTransaction;

	private void copyDatabaseObject(DatabaseObject databaseObject) throws EngineException {
		currentScreenClassOrTransaction = null;
		
		try {
			new WalkHelper() {
				// recursion parameters
				Element parentElement;
				
				public void init(DatabaseObject databaseObject, Element parentElement) throws Exception {
					this.parentElement = parentElement;
					super.init(databaseObject);
				}

				@Override
				protected void walk(DatabaseObject databaseObject) throws Exception {
					// retrieve recursion parameters
					final Element parentElement = this.parentElement;
					
					// Remember the current screen class or transaction for detecting inherited objects.
					if ((databaseObject instanceof ScreenClass) || (databaseObject instanceof Transaction)) {
						currentScreenClassOrTransaction = databaseObject;
					}
					
					// We should not include inherited objects (only for tree pastes).
					else if ((databaseObject instanceof Criteria) || (databaseObject instanceof ExtractionRule) ||
							(databaseObject instanceof Sheet) || (databaseObject instanceof BlockFactory)) {
						if ((currentScreenClassOrTransaction != null) && (currentScreenClassOrTransaction != databaseObject.getParent())) {
							return;
						}
					}
					
					Element element = parentElement;
					element = databaseObject.toXml(clipboardDocument, ExportOption.bIncludeVersion);
					appendDndData(element, databaseObject);
					parentElement.appendChild(element);
					
					// new value of recursion parameters
					this.parentElement = element;
					super.walk(databaseObject);
					
					// restore recursion parameters
					this.parentElement = parentElement;
				}
				
			}.init(databaseObject, clipboardRootElement);
		} catch (EngineException e) {
			throw e;
		} catch (Exception e) {
			throw new EngineException("Exception in copyDatabaseObject", e);
		}
	}

	private void copyPropertyObject(IPropertyTreeObject propertyObject) throws EngineException {
		if (propertyObject.isInherited()) {
			return;
		}
		Element element = propertyObject.toXml(clipboardDocument);
		clipboardRootElement.appendChild(element);
	}

	private void appendDndData(Element element, DatabaseObject databaseObject) {
		Element dnd = clipboardDocument.createElement("dnd");
		Element e;
		if (databaseObject instanceof Sequence) {
			Sequence sequence = (Sequence)databaseObject;
			e = clipboardDocument.createElement("project");
			e.setAttribute("name", sequence.getProject().getName());
			dnd.appendChild(e);
		} else if (databaseObject instanceof Transaction) {
			Transaction transaction = (Transaction)databaseObject;
			e = clipboardDocument.createElement("project");
			e.setAttribute("name", transaction.getProject().getName());
			dnd.appendChild(e);
			
			e = clipboardDocument.createElement("connector");
			e.setAttribute("name", transaction.getConnector().getName());
			dnd.appendChild(e);
		}
		element.appendChild(dnd);
	}

	public Object[] pastedObjects = null;
	public Map<String, Step> pastedSteps = new HashMap<String, Step>();

	public List<Object> read(String xmlData) throws SAXException, IOException {
		List<Object> objectList = new ArrayList<Object>();
		if (xmlData != null) {
			Document document = XMLUtils.getDefaultDocumentBuilder().parse(new InputSource(new StringReader(xmlData)));
			
			Element rootElement = document.getDocumentElement();
			NodeList nodeList = rootElement.getChildNodes();
			int len = nodeList.getLength();
			Object object;
			Node node;
			for (int i = 0 ; i < len ; i++) {
				node = (Node) nodeList.item(i);
				if (node.getNodeType() != Node.TEXT_NODE) {
					try {
						object = read(node);
						objectList.add(object);
					} catch (EngineException e) {
					}
				}
			}
		}
		return objectList;
	}

	public void paste(String xmlData, Object parentObject, boolean bChangeName) throws EngineException, SAXException, IOException {
		Document document = XMLUtils.getDefaultDocumentBuilder().parse(new InputSource(new StringReader(xmlData)));
		Element rootElement = document.getDocumentElement();
		NodeList nodeList = rootElement.getChildNodes();
		int len = nodeList.getLength();
		Node node;
		
		pastedSteps.clear();
		
		pastedObjects = new Object[]{};
		if (len > 0) {
			pastedObjects = new Object[len];
		}
		
		Object object = null;
		for (int i = 0 ; i < len ; i++) {
			node = (Node) nodeList.item(i);
			if (node.getNodeType() != Node.TEXT_NODE) {
				if (parentObject instanceof IPropertyTreeObject) {
					object = paste(node, (IPropertyTreeObject) parentObject, bChangeName);
				} else {
					object = paste(node, (DatabaseObject) parentObject, bChangeName);
				}
				pastedObjects[i] = object;
			}
		}
		
		for (Entry<String, Step> entry : pastedSteps.entrySet()) {
			Step step = entry.getValue();
			step.getSequence().fireStepCopied(new StepEvent(step, entry.getKey()));
		}
	}

	public Object read(Node node) throws EngineException {
		Class<?> objectClass = null;
		Object object = null;
		Element element = (Element) node;
		String objectClassName = element.getAttribute("classname");
		try {
			objectClass = Class.forName(objectClassName);
			Method readMethod = objectClass.getMethod("read", new Class[] { Node.class});
			object = readMethod.invoke(null, new Object[] { node } );
		} catch(Exception e) {
			throw new EngineException("Unable to read object", e);
		}
		return object;
	}

	public Object paste(Node node, IPropertyTreeObject parentPropertyTreeObject, boolean bChangeName) throws EngineException {
		Object object = read(node);
		if (object instanceof PropertyData) {
			PropertyData propertyData = (PropertyData)object;
			IPropertyTreeObject propertyTreeObject = parentPropertyTreeObject.add(propertyData, bChangeName);
			if (propertyTreeObject != null) {
				NodeList childNodes = node.getChildNodes();
				int len = childNodes.getLength();
				
				Node childNode;
				String childNodeName;
				
				for (int i = 0 ; i < len ; i++) {
					childNode = childNodes.item(i);
					
					if (childNode.getNodeType() != Node.ELEMENT_NODE) {
						continue;
					}
					
					childNodeName = childNode.getNodeName();
					if (!(childNodeName.equalsIgnoreCase("property"))) {
						paste(childNode, propertyTreeObject, bChangeName);
					}
				}
				return propertyTreeObject;
			}
		}
		return null;
	}

	public Object paste(Node node, DatabaseObject parentDatabaseObject, boolean bChangeName) throws EngineException {
		Object object = read(node);
		if (object instanceof DatabaseObject) {
			DatabaseObject databaseObject = (DatabaseObject) object;
			String dboName = databaseObject.getName();
			String name = null;
			
			if (objectsType != ProjectExplorerView.TREE_OBJECT_TYPE_DBO_PROJECT) {
				
				// Verify if object is accepted for paste
				if (!acceptDatabaseObjects(parentDatabaseObject, databaseObject)) {
					throw new EngineException("You cannot paste to a " + parentDatabaseObject.getClass().getSimpleName() + " a database object of type " + databaseObject.getClass().getSimpleName());
				}
				
				// Disable the isDefault boolean flag when the connector is pasted
				if (databaseObject instanceof Connector) {
					Connector connector = (Connector) databaseObject;
					if (connector.isDefault) {
						connector.isDefault = false;
					}
				}
				if (objectsType != ProjectExplorerView.TREE_OBJECT_TYPE_DBO_CONNECTOR) {
					// Disable the isDefault boolean flag when the transaction is pasted
					if (databaseObject instanceof Transaction) {
						Transaction transaction = (Transaction) databaseObject;
						if (transaction.isDefault) {
							transaction.isDefault = false;
						}
					}
				}
			}
			
			boolean bContinue = true;
			int index = 0;
			long oldPriority = databaseObject.priority;
			
			while (bContinue) {
				if (bChangeName) {
					if (index == 0) name = dboName;
					else name = dboName + index;
					databaseObject.setName(name);
				}
				
				databaseObject.hasChanged = true;
				databaseObject.bNew = true;
				
				try {
					if (parentDatabaseObject instanceof ScreenClass) {
						if (parentDatabaseObject instanceof JavelinScreenClass) {
							JavelinScreenClass screenClass = (JavelinScreenClass) parentDatabaseObject;
							if (databaseObject instanceof BlockFactory) {
								screenClass.add(databaseObject);
								screenClass.setBlockFactory((BlockFactory)databaseObject);
							}
						}
						
						ScreenClass screenClass = (ScreenClass) parentDatabaseObject;
						if (databaseObject instanceof Criteria) {
							if ((!screenClass.bNew) && (screenClass.equals(((IScreenClassContainer<?>) screenClass.getConnector()).getDefaultScreenClass()))) {
								throw new EngineException("You cannot paste a new criterion to the default screen class");								
							}
							databaseObject.priority = databaseObject.getNewOrderValue();
							databaseObject.newPriority = databaseObject.priority;
							screenClass.add(databaseObject);
						} else if (databaseObject instanceof ExtractionRule) {
							databaseObject.priority = databaseObject.getNewOrderValue();
							databaseObject.newPriority = databaseObject.priority;
							screenClass.add(databaseObject);
						} else if (databaseObject instanceof Sheet) {
							screenClass.add(databaseObject);
						} else if (databaseObject instanceof ScreenClass) {
							databaseObject.priority = screenClass.priority + 1;
							XMLVector<XMLVector<Long>> orderedCriterias = new XMLVector<XMLVector<Long>>();
							orderedCriterias.addElement(new XMLVector<Long>());
							((ScreenClass) databaseObject).setOrderedCriterias(orderedCriterias);
							XMLVector<XMLVector<Long>> orderedExtractionRules = new XMLVector<XMLVector<Long>>();
							orderedExtractionRules.addElement(new XMLVector<Long>());
							((ScreenClass) databaseObject).setOrderedExtractionRules(orderedExtractionRules);
							screenClass.add(databaseObject);
						}
					} else if (parentDatabaseObject instanceof HtmlTransaction) {
						HtmlTransaction transaction = (HtmlTransaction) parentDatabaseObject;
						if (databaseObject instanceof Sheet) {
							transaction.add(databaseObject);
						} else if (databaseObject instanceof TestCase) {
							transaction.add(databaseObject);
						} else if (databaseObject instanceof Variable) {
							databaseObject.priority = databaseObject.getNewOrderValue();
							databaseObject.newPriority = databaseObject.priority;
							transaction.add(databaseObject);
						} else if (databaseObject instanceof FunctionStatement) {
							if (databaseObject instanceof StatementWithExpressions) {
								databaseObject.priority = 0;
								databaseObject.newPriority = databaseObject.priority;
							}
							transaction.add(databaseObject);
						} else {
							throw new EngineException("You cannot paste to an HtmlTransaction a database object of type " + databaseObject.getClass().getName());
						}
					} else if (parentDatabaseObject instanceof TransactionWithVariables) {
						TransactionWithVariables transaction = (TransactionWithVariables) parentDatabaseObject;
						if (databaseObject instanceof Sheet) {
							transaction.add(databaseObject);
						} else if (databaseObject instanceof TestCase) {
							transaction.add(databaseObject);
						} else if (databaseObject instanceof Variable) {
							databaseObject.priority = databaseObject.getNewOrderValue();
							databaseObject.newPriority = databaseObject.priority;
							transaction.add(databaseObject);
						}
					} else if (parentDatabaseObject instanceof Sequence) {
						Sequence sequence = (Sequence) parentDatabaseObject;
						if (databaseObject instanceof Sheet) {
							sequence.add(databaseObject);
						} else if (databaseObject instanceof TestCase) {
							sequence.add(databaseObject);
						} else if (databaseObject instanceof Step) {
							databaseObject.priority = databaseObject.getNewOrderValue();
							databaseObject.newPriority = databaseObject.priority;
							sequence.add(databaseObject);
						} else if (databaseObject instanceof Variable) {
							databaseObject.priority = databaseObject.getNewOrderValue();
							databaseObject.newPriority = databaseObject.priority;
							sequence.add(databaseObject);
						} else {
							throw new EngineException("You cannot paste to a Sequence a database object of type " + databaseObject.getClass().getName());
						}
					} else if (parentDatabaseObject instanceof StatementWithExpressions) {
						StatementWithExpressions statement = (StatementWithExpressions) parentDatabaseObject;
						databaseObject.priority = databaseObject.getNewOrderValue();
						databaseObject.newPriority = databaseObject.priority;
						statement.add(databaseObject);
					} else if (parentDatabaseObject instanceof HTTPStatement) {
						HTTPStatement statement = (HTTPStatement) parentDatabaseObject;
						if (databaseObject instanceof Variable) {
							databaseObject.priority = databaseObject.getNewOrderValue();
							databaseObject.newPriority = databaseObject.priority;
							statement.add(databaseObject);
						}
					} else if (parentDatabaseObject instanceof StepWithExpressions) {
						StepWithExpressions step = (StepWithExpressions) parentDatabaseObject;
						databaseObject.priority = databaseObject.getNewOrderValue();
						databaseObject.newPriority = databaseObject.priority;
						step.add(databaseObject);
					} else if (parentDatabaseObject instanceof RequestableStep) {
						RequestableStep step = (RequestableStep) parentDatabaseObject;
						if (databaseObject instanceof Variable) {
							databaseObject.priority = databaseObject.getNewOrderValue();
							databaseObject.newPriority = databaseObject.priority;
							step.add(databaseObject);
						}
					} else if (parentDatabaseObject instanceof TestCase) {
						TestCase testCase = (TestCase) parentDatabaseObject;
						if (databaseObject instanceof Variable) {
							databaseObject.priority = databaseObject.getNewOrderValue();
							databaseObject.newPriority = databaseObject.priority;
							testCase.add(databaseObject);
						}
					} else if (parentDatabaseObject == null) {
						if (databaseObject instanceof Project) {
							if (Engine.theApp.databaseObjectsManager.existsProject(databaseObject.getName())) {
								throw new ObjectWithSameNameException("Project already exist!");
							}
						}
					} else {
						parentDatabaseObject.add(databaseObject);
					}
					bContinue = false;
				} catch(ObjectWithSameNameException owsne) {
					if ((parentDatabaseObject instanceof HtmlTransaction) && (databaseObject instanceof Statement)) {
						throw new EngineException("HtmlTransaction already contains a statement named \""+ name +"\".", owsne);
					}
					if ((parentDatabaseObject instanceof Sequence) && (databaseObject instanceof Step)) {
						throw new EngineException("Sequence already contains a step named \""+ name +"\".", owsne);
					}
					// Silently ignore
					index++;
				}
			}
			
			NodeList childNodes = node.getChildNodes();
			int len = childNodes.getLength();
			
			Node childNode;
			String childNodeName;
			
			for (int i = 0 ; i < len ; i++) {
				childNode = childNodes.item(i);
				if (childNode.getNodeType() != Node.ELEMENT_NODE) {
					continue;
				}
				childNodeName = childNode.getNodeName();
				if (!(childNodeName.equalsIgnoreCase("property")) && 
						!(childNodeName.equalsIgnoreCase("handlers")) &&
						!(childNodeName.equalsIgnoreCase("wsdltype")) &&
						!(childNodeName.equalsIgnoreCase("dnd"))) {
					paste(childNode, databaseObject, bChangeName);
				}
			}
			
			// Update sources that reference this step
			if (databaseObject instanceof Step) {
				pastedSteps.put(String.valueOf(oldPriority), (Step)databaseObject);
			}

			databaseObject.isSubLoaded = true;
			return databaseObject;
		}
		return null;
	}

	public void cutAndPaste(Object object, TreeObject targetTreeObject) throws ConvertigoException {
		if (object instanceof DatabaseObjectTreeObject) {
			if (targetTreeObject instanceof DatabaseObjectTreeObject) {
				DatabaseObject databaseObject = (DatabaseObject) ((DatabaseObjectTreeObject) object).getObject();
				cutAndPaste(databaseObject, (DatabaseObject) targetTreeObject.getObject());
			}
		} else if (object instanceof IPropertyTreeObject){
			if (object.equals(targetTreeObject)) {
				return;
			}
			if (targetTreeObject instanceof IPropertyTreeObject) {
				IPropertyTreeObject tpo = (IPropertyTreeObject) object;
				IPropertyTreeObject ttpo = (IPropertyTreeObject) targetTreeObject;
				if (tpo.getParent().equals(ttpo.getParent())) {
					return;
				}
				if (tpo.getParent().equals(ttpo)) {
					return;
				}
				tpo.remove(object);
				ttpo.add(object,false);
			}
		}
	}

	public void cutAndPaste(final DatabaseObject object, DatabaseObject parentDatabaseObject) throws ConvertigoException {
		// Verifying if a sheet with the same browser does not already exist
		if (object instanceof Sheet) {
			String browser = ((Sheet) object).getBrowser();
			Sheet sheet = null;
			if (parentDatabaseObject instanceof ScreenClass) {
				sheet = ((ScreenClass) parentDatabaseObject).getLocalSheet(browser);
			} else if (parentDatabaseObject instanceof RequestableObject) {
				sheet = ((RequestableObject) parentDatabaseObject).getSheet(browser);
			}
			if (sheet != null) {
				throw new EngineException("You cannot cut and paste the sheet because a sheet is already defined for the browser \"" + browser + "\" in the screen class \"" + parentDatabaseObject.getName() + "\".");
			}
		}
		
		if (object instanceof Step) {
			if (object instanceof ThenStep) {
				throw new EngineException("You cannot cut the \"Then\" step");
			}
			if (object instanceof ElseStep) {
				throw new EngineException("You cannot cut the \"Else\" step");
			}
		}
		
        if (object instanceof Statement) {
        	if (object instanceof ThenStatement)
        		throw new EngineException("You cannot cut the \"Then\" statement");
        	if (object instanceof ElseStatement)
        		throw new EngineException("You cannot cut the \"Else\" statement");
        }
		
        // Verify object is accepted for paste
		if (!acceptDatabaseObjects(parentDatabaseObject, object)) {
			throw new EngineException("You cannot cut and paste to a " + parentDatabaseObject.getClass().getSimpleName() + " a database object of type " + object.getClass().getSimpleName());
		}
        
        // Verify if a child object with same name exist
        try {
        	new WalkHelper() {
        		boolean root = true;
        		boolean find = false;
				
				@Override
				protected boolean before(DatabaseObject databaseObject, Class<? extends DatabaseObject> dboClass) {
					boolean isInstance = dboClass.isInstance(object);
					find |= isInstance;
					return isInstance;
				}
				
				@Override
				protected void walk(DatabaseObject databaseObject) throws Exception {
					if (root) {
						root = false;
						
						if (databaseObject instanceof Project) {
							if (object instanceof Connector && ((Connector) object).isDefault) {
								throw new EngineException("You cannot cut the default connector to another project");
							}
						} else if (databaseObject instanceof Connector) {
							if (object instanceof ScreenClass) {
								throw new EngineException("You cannot cut the default screen class to another connector");
							} else if (object instanceof Transaction && ((Transaction) object).isDefault) {
								throw new EngineException("You cannot cut the default transaction to another connector");
							}
						} else if (databaseObject instanceof ScreenClass) {
							if (object instanceof Criteria && databaseObject.getParent() instanceof Connector) {
								throw new EngineException("You cannot cut the criterion of default screen class");
							}
						}
						
						super.walk(databaseObject);
						if (!find) {
							throw new EngineException("You cannot cut and paste to a " + databaseObject.getClass().getSimpleName() + " a database object of type " + object.getClass().getSimpleName());
						}
					} else {
						if (object.getName().equals(databaseObject.getName())) {
							throw new ObjectWithSameNameException("Unable to cut the object because an object with the same name already exists in target.");
						}
					}
				}

        	}.init(parentDatabaseObject);
        } catch (EngineException e) {
			throw e;
		} catch (Exception e) {
			throw new EngineException("Exception in cutAndPaste", e);
		}
		move(object, parentDatabaseObject);
	}

	protected boolean acceptDatabaseObjects(DatabaseObject parentObject, DatabaseObject object ) {
		try {
			Class<? extends DatabaseObject> parentObjectClass = parentObject.getClass();
			Class<? extends DatabaseObject> objectClass = object.getClass();

			DboExplorerManager manager = new DboExplorerManager();
			List<DboGroup> groups = manager.getGroups();
			for (DboGroup group : groups) {
				List<DboCategory> categories = group.getCategories();
				for (DboCategory category : categories) {
					List<DboBeans> beansCategories	= category.getBeans();
					for (DboBeans beansCategory : beansCategories) {
						List<DboBean> beans = beansCategory.getBeans();
						for (DboBean bean : beans) {
							String className = bean.getClassName();
							Class<DatabaseObject> beanClass = GenericUtils.cast(Class.forName(className));
							
							// The bean should derived from DatabaseObject...
							boolean isDatabaseObject = (DatabaseObject.class.isAssignableFrom(beanClass));

							if (isDatabaseObject) {
								// ... and should derived from the specified class
								boolean isFromSpecifiedClass = ((objectClass == null) ||
										((objectClass != null) && (objectClass.isAssignableFrom(beanClass))));
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
										String technology = DboUtils.getTechnology(parentObject, objectClass);
										if (technology != null) {
											Collection<String> acceptedTechnologies = bean.getEmulatorTechnologies();
											if (!acceptedTechnologies.isEmpty() && !acceptedTechnologies.contains(technology)) {
												continue;
											}
										}
										return true;
									}
								}
							}
						}
					}
				}
			}
			return false;
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "Unable to load database objects properties.", false);
			return false;
		}
	}
	
	/**
	 * Moves the object to a target destination.
	 */
	public synchronized void move(DatabaseObject object, DatabaseObject target) throws ConvertigoException {
		// First, delete the object from its parent
		object.delete();
		//ConvertigoPlugin.projectManager.save(parent, false);

		long oldPriority = object.priority;

		// Sets new priority so object will be paste at end
		if ((object instanceof Criteria) || (object instanceof ExtractionRule) || (object instanceof Statement) || (object instanceof Step)) {
			object.priority = object.getNewOrderValue();
			object.newPriority = object.priority;
			object.hasChanged = true;
		}
		if ((object instanceof FunctionStatement) && (target instanceof HtmlTransaction)) {
			object.priority = 0;
			object.newPriority = object.priority;
			object.hasChanged = true;
		}
		if (object instanceof ScreenClass) {
			object.priority = target.priority + 1;
			object.hasChanged = true;
		}

		// Restriction due to migration to 4.0.1
		if (((target instanceof ScreenClass) && (!((ScreenClass) target).handlePriorities)) ||
				((target instanceof HtmlTransaction) && (!((HtmlTransaction) target).handlePriorities)) ||
				((target instanceof StatementWithExpressions) && (!((StatementWithExpressions) target).handlePriorities))) {
			object.bNew = true;
		}

		// Second, add the source to the target and
		target.add(object);
		//ConvertigoPlugin.projectManager.save(target, false);

		// Update sources that reference this step
		if (object instanceof Step) {
			((Step) object).getSequence().fireStepMoved(new StepEvent(object,String.valueOf(oldPriority)));
		}

		// save the object and all its children
		//ConvertigoPlugin.projectManager.save(object, true);
	}
}
