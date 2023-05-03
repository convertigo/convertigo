/*
 * Copyright (c) 2001-2023 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.views.projectexplorer;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
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
import com.twinsoft.convertigo.beans.core.IContainerOrdered;
import com.twinsoft.convertigo.beans.core.IScreenClassContainer;
import com.twinsoft.convertigo.beans.core.MobileObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.beans.core.RequestableStep;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Sheet;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepEvent;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.beans.core.TestCase;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.core.TransactionWithVariables;
import com.twinsoft.convertigo.beans.core.Variable;
import com.twinsoft.convertigo.beans.screenclasses.JavelinScreenClass;
import com.twinsoft.convertigo.beans.steps.ElseStep;
import com.twinsoft.convertigo.beans.steps.ThenStep;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.IDesignTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.IPropertyTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.ConvertigoException;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.InvalidOperationException;
import com.twinsoft.convertigo.engine.ObjectWithSameNameException;
import com.twinsoft.convertigo.engine.helpers.WalkHelper;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class ClipboardManager {
	
	public int objectsType = ProjectExplorerView.TREE_OBJECT_TYPE_UNKNOWN;
	public Object[] objects;
	public TreeObject[] parentTreeNodeOfCutObjects;

	private List<TreeObject> treeObjectsList;
	private List<TreeObject> treeParentsList;
	private Document clipboardDocument;
	private Element clipboardRootElement;

	public boolean isCut = false;
	public boolean isCopy = false;

	public void reset() {
		objectsType = ProjectExplorerView.TREE_OBJECT_TYPE_UNKNOWN;
		objects = null;
		parentTreeNodeOfCutObjects = null;
		isCut = false;
		isCopy = false;
	}

	public String copy(DatabaseObject dbo) throws EngineException {
		clipboardDocument = XMLUtils.getDefaultDocumentBuilder().newDocument();
		
		ProcessingInstruction pi = clipboardDocument.createProcessingInstruction("xml", "version=\"1.0\" encoding=\"ISO-8859-1\"");
		clipboardDocument.appendChild(pi);
		
		clipboardRootElement = clipboardDocument.createElement("convertigo-clipboard");
		clipboardDocument.appendChild(clipboardRootElement);
		
		if (dbo != null) {
			copyDatabaseObject(dbo);
		}
		
		String strObject = XMLUtils.prettyPrintDOM(clipboardDocument);
		return strObject;
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
			} else if (treeObject instanceof IDesignTreeObject) {
				IDesignTreeObject designTreeObject = (IDesignTreeObject) treeObject;
				treeObjectsList.add(treeObject);
				treeParentsList.add(((IDesignTreeObject) treeObject).getTreeObjectOwner());
				copyDesignObject(designTreeObject);
			} else {
				throw new EngineException("Tree item not supported :"+ treeObject.getClass().getName());
			}
		}
		
		objects = treeObjectsList.toArray(new Object[selectedPaths.length]);
		parentTreeNodeOfCutObjects = treeParentsList.toArray(new TreeObject[selectedPaths.length]);
		
		String strObject = XMLUtils.prettyPrintDOM(clipboardDocument);
		return strObject;
	}

	private DatabaseObject currentScreenClassOrTransaction;

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

	private void copyDesignObject(IDesignTreeObject designTreeObject) throws EngineException {
		Element element = designTreeObject.toXml(clipboardDocument);
		clipboardRootElement.appendChild(element);
	}
	
	private void appendDndData(Element element, DatabaseObject databaseObject) {
		Element dnd = clipboardDocument.createElement("dnd");
		Element e;
		try {
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
			} else if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UIComponent) {
				com.twinsoft.convertigo.beans.mobile.components.UIComponent uic = GenericUtils.cast(databaseObject);
				e = clipboardDocument.createElement("project");
				e.setAttribute("name", uic.getProject().getName());
				dnd.appendChild(e);
	
				e = clipboardDocument.createElement("mobileapplication");
				e.setAttribute("name", uic.getApplication().getParentName());
				dnd.appendChild(e);
	
				e = clipboardDocument.createElement("application");
				e.setAttribute("name", uic.getApplication().getName());
				dnd.appendChild(e);
			} else if (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UIComponent) {
				com.twinsoft.convertigo.beans.ngx.components.UIComponent uic = GenericUtils.cast(databaseObject);
				e = clipboardDocument.createElement("project");
				e.setAttribute("name", uic.getProject().getName());
				dnd.appendChild(e);
	
				e = clipboardDocument.createElement("mobileapplication");
				e.setAttribute("name", uic.getApplication().getParentName());
				dnd.appendChild(e);
	
				e = clipboardDocument.createElement("application");
				e.setAttribute("name", uic.getApplication().getName());
				dnd.appendChild(e);
			}
		} catch (Exception ex) {}
		element.appendChild(dnd);
	}

	public Object[] pastedObjects = null;
	private Map<String, Step> pastedSteps = new HashMap<String, Step>();
	private Map<String, MobileObject> pastedComponents = new HashMap<String, MobileObject>();

	public List<Object> read(String xmlData) throws SAXException, IOException {
		List<Object> objectList = new ArrayList<Object>();
		if (xmlData != null) {
			DocumentBuilder builder = XMLUtils.getDefaultDocumentBuilder();
			builder.setErrorHandler(null); // avoid 'content not allowed in prolog' to be printed out
			
			Document document = builder.parse(new InputSource(new StringReader(xmlData)));
			
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
						e.printStackTrace();
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
		pastedComponents.clear();
		
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
				} else if (parentObject instanceof IDesignTreeObject) {
					object = paste(node, (IDesignTreeObject) parentObject, bChangeName);
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
		
		for (Object ob : pastedObjects) {
			// MOBILE COMPONENTS
			if (ob instanceof com.twinsoft.convertigo.beans.mobile.components.MobileComponent) {
				if (ob instanceof com.twinsoft.convertigo.beans.mobile.components.PageComponent) {
					com.twinsoft.convertigo.beans.mobile.components.PageComponent page = GenericUtils.cast(ob);
					for (Entry<String, MobileObject> entry : pastedComponents.entrySet()) {
						if (page.updateSmartSources(entry.getKey(), String.valueOf(entry.getValue().priority))) {
							page.markPageAsDirty();
						}
					}
				}
				else if (ob instanceof com.twinsoft.convertigo.beans.mobile.components.UIComponent) {
					com.twinsoft.convertigo.beans.mobile.components.UIComponent uic = GenericUtils.cast(ob);
					for (Entry<String, MobileObject> entry : pastedComponents.entrySet()) {
						if (uic.updateSmartSources(entry.getKey(), String.valueOf(entry.getValue().priority))) {
							uic.markAsDirty();
						}
					}
				}
			}
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

	private Object paste(Node node, IPropertyTreeObject parentPropertyTreeObject, boolean bChangeName) throws EngineException {
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

	private Object paste(Node node, IDesignTreeObject parentDesignTreeObject, boolean bChangeName) throws EngineException {
		Object object = read(node);
		if (object instanceof JsonData) {
			JsonData jsonData = (JsonData)object;
			return parentDesignTreeObject.add(jsonData, bChangeName);
		}
		return null;
	}
	
	private XMLVector<XMLVector<Long>> getNewOrdered() {
		XMLVector<XMLVector<Long>> ordered = new XMLVector<XMLVector<Long>>();
		ordered.add(new XMLVector<Long>());
		return ordered;
	}
	
	private Object paste(Node node, DatabaseObject parentDatabaseObject, boolean bChangeName) throws EngineException {
		Object object = read(node);
		if (object instanceof DatabaseObject) {
			DatabaseObject databaseObject = (DatabaseObject) object;
			String dboName = databaseObject.getName();
			String name = null;
			
			if (objectsType != ProjectExplorerView.TREE_OBJECT_TYPE_DBO_PROJECT) {
				
				// Verify if object is accepted for paste
				if (!DatabaseObjectsManager.acceptDatabaseObjects(parentDatabaseObject, databaseObject)) {
					throw new EngineException("You cannot paste to a " + parentDatabaseObject.getClass().getSimpleName() + " a database object of type " + databaseObject.getClass().getSimpleName());
				}
				if (parentDatabaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.MobileComponent) {
					if (!com.twinsoft.convertigo.beans.mobile.components.dynamic.ComponentManager.acceptDatabaseObjects(parentDatabaseObject, databaseObject)) {
						throw new EngineException("You cannot paste to a " + parentDatabaseObject.getClass().getSimpleName() + " a database object of type " + databaseObject.getClass().getSimpleName());
					}
					if (!com.twinsoft.convertigo.beans.mobile.components.dynamic.ComponentManager.isTplCompatible(parentDatabaseObject, databaseObject)) {
						String tplVersion = com.twinsoft.convertigo.beans.mobile.components.dynamic.ComponentManager.getTplRequired(databaseObject);
						throw new EngineException("Template project "+ tplVersion +" compatibility required");
					}
				} else if (parentDatabaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.MobileComponent) {
					if (!com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.acceptDatabaseObjects(parentDatabaseObject, databaseObject)) {
						throw new EngineException("You cannot paste to a " + parentDatabaseObject.getClass().getSimpleName() + " a database object of type " + databaseObject.getClass().getSimpleName());
					}
					if (!com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.isTplCompatible(parentDatabaseObject, databaseObject)) {
						String tplVersion = com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.getTplRequired(databaseObject);
						throw new EngineException("Template project "+ tplVersion +" compatibility required");
					}
				}
				
				// Disable the isDefault boolean flag when the connector is pasted
				if (databaseObject instanceof Connector) {
					Connector connector = (Connector) databaseObject;
					if (connector.isDefault) {
						connector.isDefault = false;
					}
				}
				
				// Disable the isRoot boolean flag when the page is pasted
				if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.PageComponent) {
					com.twinsoft.convertigo.beans.mobile.components.PageComponent page = GenericUtils.cast(databaseObject);
					if (page.isRoot) {
						page.isRoot = false;
					}
				} else if (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.PageComponent) {
					com.twinsoft.convertigo.beans.ngx.components.PageComponent page = GenericUtils.cast(databaseObject);
					if (page.isRoot) {
						page.isRoot = false;
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
			
			// Special case of project
			if (databaseObject instanceof Project) {
				return databaseObject;
			}
			
			boolean bContinue = true;
			boolean bIncName = false;
			long oldPriority = databaseObject.priority;
			
			// Verify if a child object with same name exist and change name
			while (bContinue) {
				if (bIncName) {
					dboName = DatabaseObject.incrementName(dboName);
					databaseObject.setName(dboName);
				}
				
				databaseObject.hasChanged = true;
				databaseObject.bNew = true;
				
				try {
					new WalkHelper() {
						boolean root = true;
						boolean find = false;
						
						@Override
						protected boolean before(DatabaseObject dbo, Class<? extends DatabaseObject> dboClass) {
							boolean isInstance = dboClass.isInstance(databaseObject);
							find |= isInstance;
							return isInstance;
						}
						
						@Override
						protected void walk(DatabaseObject dbo) throws Exception {
							if (root) {
								root = false;
								super.walk(dbo);
								if (!find) {
									// ignore: we must accept special paste: e.g. transaction over sequence
								}
							} else {
								if (databaseObject.getName().equalsIgnoreCase(dbo.getName())) {
									throw new ObjectWithSameNameException("Unable to paste the object because an object with the same name already exists in target.");
								}
							}
						}

					}.init(parentDatabaseObject);
					bContinue = false;
				} catch (ObjectWithSameNameException owsne) {
					if (bChangeName) {
						bIncName = true;
					}
				} catch (EngineException ee) {
					throw ee;
				} catch (Exception e) {
					throw new EngineException("Exception in paste", e);
				}
			}
			
			// reset ordered properties
			if (databaseObject instanceof IContainerOrdered) {
				// Mobile beans
				if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.MobileComponent) {
					if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.ApplicationComponent) {
						com.twinsoft.convertigo.beans.mobile.components.ApplicationComponent ac = GenericUtils.cast(databaseObject);
						ac.setOrderedRoutes(getNewOrdered());
						ac.setOrderedMenus(getNewOrdered());
						ac.setOrderedPages(getNewOrdered());
						ac.setOrderedComponents(getNewOrdered());
						ac.setOrderedSharedActions(getNewOrdered());
						ac.setOrderedSharedComponents(getNewOrdered());
					}
					if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.RouteComponent) {
						com.twinsoft.convertigo.beans.mobile.components.RouteComponent rc = GenericUtils.cast(databaseObject);
						rc.setOrderedActions(getNewOrdered());
						rc.setOrderedEvents(getNewOrdered());
					}
					if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.PageComponent) {
						com.twinsoft.convertigo.beans.mobile.components.PageComponent pc = GenericUtils.cast(databaseObject);
						pc.setOrderedComponents(getNewOrdered());
					}
					if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UIComponent) {
						com.twinsoft.convertigo.beans.mobile.components.UIComponent uic = GenericUtils.cast(databaseObject);
						uic.setOrderedComponents(getNewOrdered());
					}
				} else if (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.MobileComponent) {
					if (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent) {
						com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent ac = GenericUtils.cast(databaseObject);
						ac.setOrderedMenus(getNewOrdered());
						ac.setOrderedPages(getNewOrdered());
						ac.setOrderedComponents(getNewOrdered());
						ac.setOrderedSharedActions(getNewOrdered());
						ac.setOrderedSharedComponents(getNewOrdered());
					}
					if (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.PageComponent) {
						com.twinsoft.convertigo.beans.ngx.components.PageComponent pc = GenericUtils.cast(databaseObject);
						pc.setOrderedComponents(getNewOrdered());
					}
					if (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UIComponent) {
						com.twinsoft.convertigo.beans.ngx.components.UIComponent uic = GenericUtils.cast(databaseObject);
						uic.setOrderedComponents(getNewOrdered());
					}
				}
				
				// Sequence beans
				if (databaseObject instanceof Sequence) {
					((Sequence)databaseObject).setOrderedSteps(getNewOrdered());
					((Sequence)databaseObject).setOrderedVariables(getNewOrdered());
				}
				if (databaseObject instanceof StepWithExpressions) {
					((StepWithExpressions)databaseObject).setOrderedSteps(getNewOrdered());
				}
				if (databaseObject instanceof RequestableStep) {
					((RequestableStep)databaseObject).setOrderedVariables(getNewOrdered());
				}
				
				// Transaction beans
				if (databaseObject instanceof TransactionWithVariables) {
					((TransactionWithVariables)databaseObject).setOrderedVariables(getNewOrdered());
				}
				if (databaseObject instanceof ScreenClass) {
					((ScreenClass)databaseObject).setOrderedCriterias(getNewOrdered());
					((ScreenClass)databaseObject).setOrderedExtractionRules(getNewOrdered());
				}
				
				// TestCase bean
				if (databaseObject instanceof TestCase) {
					((TestCase)databaseObject).setOrderedVariables(getNewOrdered());
				}
				
			}
			
			// Now add dbo to target
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
						screenClass.add(databaseObject);
					} else if (databaseObject instanceof ExtractionRule) {
						databaseObject.priority = databaseObject.getNewOrderValue();
						screenClass.add(databaseObject);
					} else if (databaseObject instanceof Sheet) {
						screenClass.add(databaseObject);
					} else if (databaseObject instanceof ScreenClass) {
						databaseObject.priority = screenClass.priority + 1;
						screenClass.add(databaseObject);
					}
				} else if (parentDatabaseObject instanceof TransactionWithVariables) {
					TransactionWithVariables transaction = (TransactionWithVariables) parentDatabaseObject;
					if (databaseObject instanceof Sheet) {
						transaction.add(databaseObject);
					} else if (databaseObject instanceof TestCase) {
						transaction.add(databaseObject);
					} else if (databaseObject instanceof Variable) {
						databaseObject.priority = databaseObject.getNewOrderValue();
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
						sequence.add(databaseObject);
					} else if (databaseObject instanceof Variable) {
						databaseObject.priority = databaseObject.getNewOrderValue();
						sequence.add(databaseObject);
					} else {
						throw new EngineException("You cannot paste to a Sequence a database object of type " + databaseObject.getClass().getName());
					}
				} else if (parentDatabaseObject instanceof StepWithExpressions) {
					StepWithExpressions step = (StepWithExpressions) parentDatabaseObject;
					databaseObject.priority = databaseObject.getNewOrderValue();
					step.add(databaseObject);
				} else if (parentDatabaseObject instanceof RequestableStep) {
					RequestableStep step = (RequestableStep) parentDatabaseObject;
					if (databaseObject instanceof Variable) {
						databaseObject.priority = databaseObject.getNewOrderValue();
						step.add(databaseObject);
					}
				} else if (parentDatabaseObject instanceof TestCase) {
					TestCase testCase = (TestCase) parentDatabaseObject;
					if (databaseObject instanceof Variable) {
						databaseObject.priority = databaseObject.getNewOrderValue();
						testCase.add(databaseObject);
					}
				}
				// MOBILE COMPONENTS
				else if (parentDatabaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.MobileComponent) {
					if (parentDatabaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.ApplicationComponent) {
						com.twinsoft.convertigo.beans.mobile.components.ApplicationComponent app = GenericUtils.cast(parentDatabaseObject);
						if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.PageComponent) {
							databaseObject.priority = databaseObject.getNewOrderValue();
							app.add(databaseObject);
						}
						else if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.RouteComponent) {
							databaseObject.priority = databaseObject.getNewOrderValue();
							app.add(databaseObject);
						}
						else if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UIDynamicMenu) {
							databaseObject.priority = databaseObject.getNewOrderValue();
							app.add(databaseObject);
						}
						else if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UIComponent) {
							databaseObject.priority = databaseObject.getNewOrderValue();
							app.add(databaseObject);
						}
					} else if (parentDatabaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.RouteComponent) {
						com.twinsoft.convertigo.beans.mobile.components.RouteComponent route = GenericUtils.cast(parentDatabaseObject);
						if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.RouteActionComponent) {
							databaseObject.priority = databaseObject.getNewOrderValue();
							com.twinsoft.convertigo.beans.mobile.components.RouteActionComponent rac = GenericUtils.cast(databaseObject);
							int i = rac.getPage().lastIndexOf(".");
							if (i != -1) {
								String pageName = rac.getPage().substring(i);
								String pageQName = route.getParent().getQName() + pageName;
								rac.setPage(pageQName);
							}
							route.add(rac);
						}
						else if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.RouteEventComponent) {
							databaseObject.priority = databaseObject.getNewOrderValue();
							route.add(databaseObject);
						}
					} else if (parentDatabaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.PageComponent) {
						com.twinsoft.convertigo.beans.mobile.components.PageComponent page = GenericUtils.cast(parentDatabaseObject);
						if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UIComponent) {
							databaseObject.priority = databaseObject.getNewOrderValue();
							page.add(databaseObject);
						}
					} else if (parentDatabaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UIDynamicMenu) {
						com.twinsoft.convertigo.beans.mobile.components.UIDynamicMenu menu = GenericUtils.cast(parentDatabaseObject);
						if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UIComponent) {
							databaseObject.priority = databaseObject.getNewOrderValue();
							menu.add(databaseObject);
						}
					} else if (parentDatabaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UIComponent) {
						com.twinsoft.convertigo.beans.mobile.components.UIComponent component = GenericUtils.cast(parentDatabaseObject);
						databaseObject.priority = databaseObject.getNewOrderValue();
						component.add(databaseObject);
					}
				}
				// NGX COMPONENTS
				else if (parentDatabaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.MobileComponent) {
					if (parentDatabaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent) {
						com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent app = GenericUtils.cast(parentDatabaseObject);
						if (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.PageComponent) {
							databaseObject.priority = databaseObject.getNewOrderValue();
							app.add(databaseObject);
						}
						else if (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UIDynamicMenu) {
							databaseObject.priority = databaseObject.getNewOrderValue();
							app.add(databaseObject);
						}
						else if (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UIComponent) {
							databaseObject.priority = databaseObject.getNewOrderValue();
							app.add(databaseObject);
						}
					} else if (parentDatabaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.PageComponent) {
						com.twinsoft.convertigo.beans.ngx.components.PageComponent page = GenericUtils.cast(parentDatabaseObject);
						if (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UIComponent) {
							databaseObject.priority = databaseObject.getNewOrderValue();
							page.add(databaseObject);
						}
					} else if (parentDatabaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UIDynamicMenu) {
						com.twinsoft.convertigo.beans.ngx.components.UIDynamicMenu menu = GenericUtils.cast(parentDatabaseObject);
						if (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UIComponent) {
							databaseObject.priority = databaseObject.getNewOrderValue();
							menu.add(databaseObject);
						}
					} else if (parentDatabaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UIComponent) {
						com.twinsoft.convertigo.beans.ngx.components.UIComponent component = GenericUtils.cast(parentDatabaseObject);
						databaseObject.priority = databaseObject.getNewOrderValue();
						component.add(databaseObject);
					}
				}
				else if (parentDatabaseObject == null) {
					if (databaseObject instanceof Project) {
						if (Engine.theApp.databaseObjectsManager.existsProject(databaseObject.getName())) {
							throw new ObjectWithSameNameException("Project already exist!");
						}
					}
				} else {
					parentDatabaseObject.add(databaseObject);
				}
			} catch(ObjectWithSameNameException owsne) {
				if ((parentDatabaseObject instanceof Sequence) && (databaseObject instanceof Step)) {
					throw new EngineException("Sequence already contains a step named \""+ name +"\".", owsne);
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
						!(childNodeName.equalsIgnoreCase("docdata")) &&
						!(childNodeName.equalsIgnoreCase("dnd"))) {
					paste(childNode, databaseObject, bChangeName);
				}
			}
			
			// For update of sources which reference this step
			if (databaseObject instanceof Step) {
				pastedSteps.put(String.valueOf(oldPriority), (Step)databaseObject);
			}
			// For update of sources which reference this mobile component
			if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.MobileComponent) {
				if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.IAction || 
					databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UIActionStack || 
					databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UIControlDirective || 
					databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UIForm ||
					databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UISharedComponent
				) {
					pastedComponents.put(String.valueOf(oldPriority), GenericUtils.cast(databaseObject));
				}
			} else if (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.MobileComponent) {
				if (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.IAction || 
					databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UIActionStack || 
					databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UIControlDirective || 
					databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UIForm ||
					databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UISharedComponent
				) {
					pastedComponents.put(String.valueOf(oldPriority), GenericUtils.cast(databaseObject));
				}
			}
			
			databaseObject.isImporting = false; // needed
			databaseObject.isSubLoaded = true;
			return databaseObject;
		} else if (object instanceof JsonData) {
			if (parentDatabaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.MobileComponent) {
				if (parentDatabaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UIPageEvent || 
						parentDatabaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UIAppEvent || 
						parentDatabaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UIDynamicAction || 
						parentDatabaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UIActionStack
					) {
					JsonData jsonData = (JsonData) object;
					JSONObject json = jsonData.getData();
					if (json.has("qname")) {
						try {
							com.twinsoft.convertigo.beans.mobile.components.UIComponent uiComponent = GenericUtils.cast(parentDatabaseObject);
								
							DatabaseObject call = com.twinsoft.convertigo.beans.mobile.components.dynamic.ComponentManager.createBean(com.twinsoft.convertigo.beans.mobile.components.dynamic.ComponentManager.getComponentByName("FullSyncViewAction"));
							if (call != null && call instanceof com.twinsoft.convertigo.beans.mobile.components.UIDynamicAction) {
								com.twinsoft.convertigo.beans.mobile.components.UIDynamicAction dynAction = GenericUtils.cast(call);
								com.twinsoft.convertigo.beans.mobile.components.dynamic.IonBean ionBean = dynAction.getIonBean();
								if (ionBean != null && ionBean.hasProperty("fsview")) {
									call.bNew = true;
									call.hasChanged = true;
									ionBean.setPropertyValue("fsview", new com.twinsoft.convertigo.beans.mobile.components.MobileSmartSourceType(json.getString("qname")));
									uiComponent.add(call);
									uiComponent.hasChanged = true;
								}
								return call;
							}
						} catch (JSONException e) {
							Engine.logStudio.warn("Failed to create a FullSyncViewAction", e);
						}
					}
				}
			}
			else if (parentDatabaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.MobileComponent) {
				if (parentDatabaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UIPageEvent || 
						parentDatabaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UISharedComponentEvent ||
						parentDatabaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UIAppEvent || 
						parentDatabaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UIDynamicAction || 
						parentDatabaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UIActionStack
					) {
					JsonData jsonData = (JsonData) object;
					JSONObject json = jsonData.getData();
					if (json.has("qname")) {
						try {
							com.twinsoft.convertigo.beans.ngx.components.UIComponent uiComponent = GenericUtils.cast(parentDatabaseObject);
								
							DatabaseObject call = com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.createBean(com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.getComponentByName("FullSyncViewAction"));
							if (call != null && call instanceof com.twinsoft.convertigo.beans.ngx.components.UIDynamicAction) {
								com.twinsoft.convertigo.beans.ngx.components.UIDynamicAction dynAction = GenericUtils.cast(call);
								com.twinsoft.convertigo.beans.ngx.components.dynamic.IonBean ionBean = dynAction.getIonBean();
								if (ionBean != null && ionBean.hasProperty("fsview")) {
									call.bNew = true;
									call.hasChanged = true;
									ionBean.setPropertyValue("fsview", new com.twinsoft.convertigo.beans.ngx.components.MobileSmartSourceType(json.getString("qname")));
									uiComponent.add(call);
									uiComponent.hasChanged = true;
								}
								return call;
							}
						} catch (JSONException e) {
							Engine.logStudio.warn("Failed to create a FullSyncViewAction", e);
						}
					}
				}
			}
		}
		return null;
	}

	public void cutAndPaste(Object object, TreeObject targetTreeObject) throws ConvertigoException {
		// Ignore cut paste on itself
		if (object.equals(targetTreeObject)) {
			return;
		}
		// Check for cut paste into itself
		if (object instanceof TreeObject) {
			if (targetTreeObject.isChildOf((TreeObject)object)) {
				throw new InvalidOperationException("You cannot cut and paste this object into itself");
			}
		}
		
		if (object instanceof DatabaseObjectTreeObject) {
			if (targetTreeObject instanceof DatabaseObjectTreeObject) {
				DatabaseObjectTreeObject sourceTreeObject = (DatabaseObjectTreeObject) object;
				DatabaseObject databaseObject = (DatabaseObject) sourceTreeObject.getObject();
				DatabaseObject targetObject = (DatabaseObject) targetTreeObject.getObject();
				String oldQName = databaseObject.getQName();
				cutAndPaste(databaseObject, targetObject);
				String newQName = databaseObject.getQName();
				
				ProjectExplorerView projectExplorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();
				if (projectExplorerView != null) {
					TreeObjectEvent treeObjectEvent = new TreeObjectEvent(sourceTreeObject, "qname", oldQName, newQName, TreeObjectEvent.UPDATE_ALL);
					projectExplorerView.fireTreeObjectPropertyChanged(treeObjectEvent);
				}
			}
		} else if (object instanceof IPropertyTreeObject) {
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
		} else if (object instanceof IDesignTreeObject) {
			if (targetTreeObject instanceof IDesignTreeObject) {
				IDesignTreeObject tpo = (IDesignTreeObject) object;
				IDesignTreeObject ttpo = (IDesignTreeObject) targetTreeObject;
				if (tpo.getParent().equals(ttpo.getParent())) {
					return;
				}
				if (tpo.getParent().equals(ttpo)) {
					return;
				}
				if (ttpo.canPaste(object)) {
					tpo.remove(object);
					ttpo.add(object,false);
				}
			}
		}
	}

	private void cutAndPaste(final DatabaseObject object, DatabaseObject parentDatabaseObject) throws ConvertigoException {
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
		
		// Verify object is accepted for paste
		if (!DatabaseObjectsManager.acceptDatabaseObjects(parentDatabaseObject, object)) {
			throw new EngineException("You cannot cut and paste to a " + parentDatabaseObject.getClass().getSimpleName() + " a database object of type " + object.getClass().getSimpleName());
		}
		if (parentDatabaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.MobileComponent) {
			if (!com.twinsoft.convertigo.beans.mobile.components.dynamic.ComponentManager.acceptDatabaseObjects(parentDatabaseObject, object)) {
				throw new EngineException("You cannot cut and paste to a " + parentDatabaseObject.getClass().getSimpleName() + " a database object of type " + object.getClass().getSimpleName());
			}
			if (!com.twinsoft.convertigo.beans.mobile.components.dynamic.ComponentManager.isTplCompatible(parentDatabaseObject, object)) {
				String tplVersion = com.twinsoft.convertigo.beans.mobile.components.dynamic.ComponentManager.getTplRequired(object);
				throw new EngineException("Template project "+ tplVersion +" compatibility required");
			}
		} else if (parentDatabaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.MobileComponent) {
			if (!com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.acceptDatabaseObjects(parentDatabaseObject, object)) {
				throw new EngineException("You cannot cut and paste to a " + parentDatabaseObject.getClass().getSimpleName() + " a database object of type " + object.getClass().getSimpleName());
			}
			if (!com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.isTplCompatible(parentDatabaseObject, object)) {
				String tplVersion = com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.getTplRequired(object);
				throw new EngineException("Template project "+ tplVersion +" compatibility required");
			}
		}
		
		// Verify if a child object with same name exist
		boolean bContinue = true;
		boolean bIncName = false;
		String dboName = object.getName();
		while (bContinue) {
			try {
				if (bIncName) {
					dboName = DatabaseObject.incrementName(dboName);
					object.setName(dboName);
				}
				
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
							} else if (databaseObject instanceof MobileObject) {
								if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.ApplicationComponent) {
									if (object instanceof com.twinsoft.convertigo.beans.mobile.components.PageComponent) {
										com.twinsoft.convertigo.beans.mobile.components.PageComponent pc = GenericUtils.cast(object);
										if (pc.isRoot) {
											throw new EngineException("You cannot cut the root page to another application");
										}
									}
								} else if (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent) {
									if (object instanceof com.twinsoft.convertigo.beans.ngx.components.PageComponent) {
										com.twinsoft.convertigo.beans.ngx.components.PageComponent pc = GenericUtils.cast(object);
										if (pc.isRoot) {
											throw new EngineException("You cannot cut the root page to another application");
										}
									}
								}
							}

							super.walk(databaseObject);
							if (!find) {
								throw new EngineException("You cannot cut and paste to a " + databaseObject.getClass().getSimpleName() + " a database object of type " + object.getClass().getSimpleName());
							}
						} else {

							if (object != databaseObject && object.getName().equalsIgnoreCase(databaseObject.getName())) {
								throw new ObjectWithSameNameException("Unable to cut the object because an object with the same name already exists in target.");
							}
						}
					}
				}.init(parentDatabaseObject);
				bContinue = false;
			} catch (ObjectWithSameNameException e) {
				bIncName = true;
			} catch (EngineException e) {
				throw e;
			} catch (Exception e) {
				throw new EngineException("Exception in cutAndPaste", e);
			}
		}
		
		move(object, parentDatabaseObject);
	}
	
	/**
	 * Moves the object to a target destination.
	 */
	private synchronized void move(DatabaseObject object, DatabaseObject target) throws ConvertigoException {
		if (object.getParent() != target) {
			// First, delete the object from its parent
			object.delete();
	
			// Second, add the source to the target
			target.add(object);
		}
	}
}
