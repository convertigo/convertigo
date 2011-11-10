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
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.connectors.HtmlConnector;
import com.twinsoft.convertigo.beans.connectors.JavelinConnector;
import com.twinsoft.convertigo.beans.core.BlockFactory;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Criteria;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.ExtractionRule;
import com.twinsoft.convertigo.beans.core.Pool;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.RequestableStep;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Sheet;
import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.beans.core.StatementWithExpressions;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepEvent;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.core.TransactionWithVariables;
import com.twinsoft.convertigo.beans.core.Variable;
import com.twinsoft.convertigo.beans.screenclasses.JavelinScreenClass;
import com.twinsoft.convertigo.beans.statements.FunctionStatement;
import com.twinsoft.convertigo.beans.statements.HTTPStatement;
import com.twinsoft.convertigo.beans.transactions.HtmlTransaction;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.engine.ConvertigoException;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.ObjectWithSameNameException;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class ClipboardManager {

	public int databaseObjectsType = ProjectExplorerView.TREE_OBJECT_TYPE_UNKNOWN;
	public DatabaseObject[] databaseObjects;
	public DatabaseObjectTreeObject[] parentTreeNodeOfCutDatabaseObjects;

	protected Document clipboardDocument;
	protected Element clipboardRootElement;

	public boolean isCut = false;
	public boolean isCopy = false;
	
	public void reset() {
		databaseObjectsType = ProjectExplorerView.TREE_OBJECT_TYPE_UNKNOWN;
		databaseObjects = null;
		parentTreeNodeOfCutDatabaseObjects = null;
		isCut = false;
		isCopy = false;
	}

	public String copy(TreePath[] treePaths) throws EngineException, ParserConfigurationException {
		ProjectExplorerView projectExplorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();
		
		clipboardDocument = XMLUtils.documentBuilderDefault.newDocument();

		ProcessingInstruction pi = clipboardDocument.createProcessingInstruction("xml", "version=\"1.0\" encoding=\"ISO-8859-1\"");
		clipboardDocument.appendChild(pi);

		clipboardRootElement = clipboardDocument.createElement("convertigo-clipboard");
		clipboardDocument.appendChild(clipboardRootElement);

		TreePath[] selectedPaths = ((treePaths == null) ? projectExplorerView.getSelectionPaths() : treePaths);
		databaseObjects = new DatabaseObject[selectedPaths.length];
		parentTreeNodeOfCutDatabaseObjects = new DatabaseObjectTreeObject[selectedPaths.length];
		
		for (int i = 0 ; i < selectedPaths.length ; i++) {
			// Disable the isDefault boolean flag when the transaction is pasted
			DatabaseObject copiedDatabaseObject = (DatabaseObject) ((TreeObject)selectedPaths[i].getLastPathComponent()).getObject();
			databaseObjects[i] = copiedDatabaseObject;
        
			copy(copiedDatabaseObject);
			parentTreeNodeOfCutDatabaseObjects[i] = projectExplorerView.findTreeObjectByUserObject(copiedDatabaseObject.getParent());
		}

		String strObject = XMLUtils.prettyPrintDOM(clipboardDocument);
        
		return strObject;
	}
    
	DatabaseObject currentScreenClassOrTransaction;

	private void copy(DatabaseObject databaseObject) throws EngineException {
		currentScreenClassOrTransaction = null;
		copy(clipboardRootElement, databaseObject);
	}
    
	private void copy(Element parentElement, DatabaseObject databaseObject) throws EngineException {
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
		element = databaseObject.toXml(clipboardDocument);
		parentElement.appendChild(element);
		List<? extends DatabaseObject> vDatabaseObjects;

		if (databaseObject instanceof Project) {
			vDatabaseObjects = ((Project) databaseObject).getConnectorsList();
			for (DatabaseObject dbo : vDatabaseObjects) {
				copy(element, (Connector) dbo);
			}

			vDatabaseObjects = ((Project) databaseObject).getSequencesList();
			for (DatabaseObject dbo : vDatabaseObjects) {
				copy(element, (Sequence) dbo);
			}
		}
		else if (databaseObject instanceof Sequence) {
			vDatabaseObjects = ((Sequence) databaseObject).getSteps();
			for (DatabaseObject dbo : vDatabaseObjects) {
				copy(element, (Step) dbo);
			}

			vDatabaseObjects = ((Sequence) databaseObject).getSheetsList();
			for (DatabaseObject dbo : vDatabaseObjects) {
				copy(element, (Sheet) dbo);
			}

			vDatabaseObjects = ((Sequence) databaseObject).getVariablesList();
			for (DatabaseObject dbo : vDatabaseObjects) {
				copy(element, (Variable) dbo);
			}
		}
		else if (databaseObject instanceof Connector) {
			vDatabaseObjects = ((Connector) databaseObject).getPoolsList();
			for (DatabaseObject dbo : vDatabaseObjects) {
				copy(element, (Pool) dbo);
			}

			vDatabaseObjects = ((Connector) databaseObject).getTransactionsList();
			for (DatabaseObject dbo : vDatabaseObjects) {
				copy(element, (Transaction) dbo);
			}

			if (databaseObject instanceof JavelinConnector) {
				ScreenClass rootScreenClass = ((JavelinConnector) databaseObject).getDefaultScreenClass();
				if (rootScreenClass != null) {
					copy(element, rootScreenClass);
				}
			}
			if (databaseObject instanceof HtmlConnector) {
				ScreenClass rootScreenClass = ((HtmlConnector) databaseObject).getDefaultScreenClass();
				if (rootScreenClass != null) {
					copy(element, rootScreenClass);
				}
			}
		}
		else if (databaseObject instanceof Transaction) {
			vDatabaseObjects = ((Transaction) databaseObject).getSheetsList();
			for (DatabaseObject dbo : vDatabaseObjects) {
				copy(element, (Sheet) dbo);
			}
			
			if (databaseObject instanceof HtmlTransaction) {
				vDatabaseObjects = ((HtmlTransaction) databaseObject).getStatements();
				for (DatabaseObject dbo : vDatabaseObjects) {
					copy(element, (Statement) dbo);
				}
			}
			if (databaseObject instanceof TransactionWithVariables) {
				vDatabaseObjects = ((TransactionWithVariables) databaseObject).getVariablesList();
				for (DatabaseObject dbo : vDatabaseObjects) {
					copy(element, (Variable) dbo);
				}
			}
		}
		else if (databaseObject instanceof StatementWithExpressions) {
			vDatabaseObjects = ((StatementWithExpressions) databaseObject).getStatements();
			for (DatabaseObject dbo : vDatabaseObjects) {
				copy(element, (Statement) dbo);
			}
		}
		else if (databaseObject instanceof HTTPStatement) {
			vDatabaseObjects = ((HTTPStatement) databaseObject).getVariables();
			for (DatabaseObject dbo : vDatabaseObjects) {
				copy(element, (Variable) dbo);
			}
		}
		else if (databaseObject instanceof StepWithExpressions) {
			vDatabaseObjects = ((StepWithExpressions) databaseObject).getSteps();
			for (DatabaseObject dbo : vDatabaseObjects) {
				copy(element, (Step) dbo);
			}
		}
		else if (databaseObject instanceof RequestableStep) {
			vDatabaseObjects = ((RequestableStep) databaseObject).getVariables();
			for (DatabaseObject dbo : vDatabaseObjects) {
				copy(element, (Variable) dbo);
			}
		}
		else if (databaseObject instanceof ScreenClass) {
			if (databaseObject instanceof JavelinScreenClass) {
				BlockFactory blockFactory = ((JavelinScreenClass) databaseObject).getBlockFactory();
				copy(element, blockFactory);
			}
			vDatabaseObjects = ((ScreenClass) databaseObject).getCriterias();
			for (DatabaseObject dbo : vDatabaseObjects) {
				copy(element, (Criteria) dbo);
			}

			vDatabaseObjects = ((ScreenClass) databaseObject).getExtractionRules();
			for (DatabaseObject dbo : vDatabaseObjects) {
				copy(element, (ExtractionRule) dbo);
			}

			vDatabaseObjects = ((ScreenClass) databaseObject).getSheets();
			for (DatabaseObject dbo : vDatabaseObjects) {
				copy(element, (Sheet) dbo);
			}

			vDatabaseObjects = ((ScreenClass) databaseObject).getInheritedScreenClasses();
			for (DatabaseObject dbo : vDatabaseObjects) {
				copy(element, (ScreenClass) dbo);
			}
		}
	}

	public DatabaseObject[] pastedObjects = null;
	public Map<String, DatabaseObject> pastedSteps = null;
	
	public void paste(String xmlData, DatabaseObject parentDatabaseObject, boolean bChangeName) throws EngineException, SAXException, IOException {
		Document document = XMLUtils.documentBuilderDefault.parse(new InputSource(new StringReader(xmlData)));

		Element rootElement = document.getDocumentElement();
		NodeList nodeList = rootElement.getChildNodes();
		int len = nodeList.getLength();
		Node node;
		
		pastedSteps = new Hashtable<String, DatabaseObject>();
		
		pastedObjects = new DatabaseObject[]{};
		if (len > 0) {
			pastedObjects = new DatabaseObject[len];
		}
		
		for (int i = 0 ; i < len ; i++) {
			node = (Node) nodeList.item(i);
			if (node.getNodeType() != Node.TEXT_NODE) {
				DatabaseObject databaseObject = paste(node, parentDatabaseObject, bChangeName);
				pastedObjects[i] = databaseObject;
			}
		}
		
		for (String oldPriority : pastedSteps.keySet()) {
			DatabaseObject databaseObject = (DatabaseObject) pastedSteps.get(oldPriority);
			((Step) databaseObject).getSequence().fireStepCopied(new StepEvent(databaseObject, oldPriority));
		}
			
	}
    
	public DatabaseObject paste(Node node, DatabaseObject parentDatabaseObject, boolean bChangeName) throws EngineException {
		DatabaseObject databaseObject = DatabaseObject.read(node);
            
		String dboName = databaseObject.getName();
		String name = null;

		if (databaseObjectsType != ProjectExplorerView.TREE_OBJECT_TYPE_DBO_PROJECT) {
			// Disable the isDefault boolean flag when the connector is pasted
			if (databaseObject instanceof Connector) {
				Connector connector = (Connector) databaseObject;
				if (connector.isDefault) {
					connector.isDefault = false;
				}
			}
			if (databaseObjectsType != ProjectExplorerView.TREE_OBJECT_TYPE_DBO_CONNECTOR) {
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
						databaseObject.priority = databaseObject.getNewOrderValue();
						databaseObject.newPriority = databaseObject.priority;
						screenClass.add(databaseObject);
					}
					else if (databaseObject instanceof ExtractionRule) {
						databaseObject.priority = databaseObject.getNewOrderValue();
						databaseObject.newPriority = databaseObject.priority;
						screenClass.add(databaseObject);
					}
					else if (databaseObject instanceof Sheet) {
						screenClass.add(databaseObject);
					}
					else if (databaseObject instanceof ScreenClass) {
						databaseObject.priority = screenClass.priority + 1;
						
						XMLVector<XMLVector<Long>> orderedCriterias = new XMLVector<XMLVector<Long>>();
						orderedCriterias.addElement(new XMLVector<Long>());
						((ScreenClass)databaseObject).setOrderedCriterias(orderedCriterias);
						
						XMLVector<XMLVector<Long>> orderedExtractionRules = new XMLVector<XMLVector<Long>>();
						orderedExtractionRules.addElement(new XMLVector<Long>());
						((ScreenClass)databaseObject).setOrderedExtractionRules(orderedExtractionRules);
						screenClass.add(databaseObject);
					}
				}
				else if (parentDatabaseObject instanceof HtmlTransaction) {
					HtmlTransaction transaction = (HtmlTransaction) parentDatabaseObject;
					if (databaseObject instanceof Sheet) {
						transaction.add(databaseObject);
					}
					else if (databaseObject instanceof Variable) {
						databaseObject.priority = databaseObject.getNewOrderValue();
						databaseObject.newPriority = databaseObject.priority;
						transaction.add(databaseObject);
					}
					else if (databaseObject instanceof FunctionStatement) {
						if (databaseObject instanceof StatementWithExpressions) {
							databaseObject.priority = 0;
							databaseObject.newPriority = databaseObject.priority;
						}
						transaction.add(databaseObject);
					}
					else
						throw new EngineException("You cannot paste to an HtmlTransaction a database object of type " + databaseObject.getClass().getName());
				}
				else if (parentDatabaseObject instanceof TransactionWithVariables) {
					TransactionWithVariables transaction = (TransactionWithVariables) parentDatabaseObject;
					if (databaseObject instanceof Sheet) {
						transaction.add(databaseObject);
					}
					else if (databaseObject instanceof Variable) {
						databaseObject.priority = databaseObject.getNewOrderValue();
						databaseObject.newPriority = databaseObject.priority;
						transaction.add(databaseObject);
					}
				}
				else if (parentDatabaseObject instanceof Sequence) {
					Sequence sequence = (Sequence) parentDatabaseObject;
					if (databaseObject instanceof Sheet) {
						sequence.add(databaseObject);
					}
					else if (databaseObject instanceof Step) {
						databaseObject.priority = databaseObject.getNewOrderValue();
						databaseObject.newPriority = databaseObject.priority;
						sequence.add(databaseObject);
					}
					else if (databaseObject instanceof Variable) {
						databaseObject.priority = databaseObject.getNewOrderValue();
						databaseObject.newPriority = databaseObject.priority;
						sequence.add(databaseObject);
					}
					else
						throw new EngineException("You cannot paste to a Sequence a database object of type " + databaseObject.getClass().getName());
				}
				else if (parentDatabaseObject instanceof StatementWithExpressions) {
					StatementWithExpressions statement = (StatementWithExpressions) parentDatabaseObject;
					databaseObject.priority = databaseObject.getNewOrderValue();
					databaseObject.newPriority = databaseObject.priority;
					statement.add(databaseObject);
				}
				else if (parentDatabaseObject instanceof HTTPStatement) {
					HTTPStatement statement = (HTTPStatement) parentDatabaseObject;
					if (databaseObject instanceof Variable) {
						databaseObject.priority = databaseObject.getNewOrderValue();
						databaseObject.newPriority = databaseObject.priority;
						statement.add(databaseObject);
					}
				}
				else if (parentDatabaseObject instanceof StepWithExpressions) {
					StepWithExpressions step = (StepWithExpressions) parentDatabaseObject;
					databaseObject.priority = databaseObject.getNewOrderValue();
					databaseObject.newPriority = databaseObject.priority;
					step.add(databaseObject);
				}
				else if (parentDatabaseObject instanceof RequestableStep) {
					RequestableStep step = (RequestableStep) parentDatabaseObject;
					if (databaseObject instanceof Variable) {
						databaseObject.priority = databaseObject.getNewOrderValue();
						databaseObject.newPriority = databaseObject.priority;
						step.add(databaseObject);
					}
				}
				else if (parentDatabaseObject == null) {
					if (databaseObject instanceof Project) {
						if (Engine.theApp.databaseObjectsManager.existsProject(databaseObject.getName())) {
							throw new ObjectWithSameNameException("Project already exist!");
						}
					}
				}
				else {
					parentDatabaseObject.add(databaseObject);
				}
				bContinue = false;
			}
			catch(ObjectWithSameNameException owsne) {
				if ((parentDatabaseObject instanceof HtmlTransaction) && (databaseObject instanceof Statement))
					throw new EngineException("HtmlTransaction already contains a statement named \""+ name +"\".", owsne);
					
				if ((parentDatabaseObject instanceof Sequence) && (databaseObject instanceof Step))
					throw new EngineException("Sequence already contains a step named \""+ name +"\".", owsne);
				
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
                
			if (childNode.getNodeType() != Node.ELEMENT_NODE) continue;
                
			childNodeName = childNode.getNodeName();
                
			if (!(childNodeName.equalsIgnoreCase("property")) && 
				!(childNodeName.equalsIgnoreCase("handlers")) &&
				!(childNodeName.equalsIgnoreCase("wsdltype"))) {
				paste(childNode, databaseObject, bChangeName);
			}
		}

		// Update sources that reference this step
		if (databaseObject instanceof Step)
			pastedSteps.put(String.valueOf(oldPriority), databaseObject);
		
		return databaseObject;
	}
    
	public void cutAndPaste(DatabaseObject object, DatabaseObject parentDatabaseObject) throws ConvertigoException {
		// Verifying if a sheet with the same browser does not already exist
		if (object instanceof Sheet) {
			String browser = ((Sheet) object).getBrowser();
            
			Sheet sheet;
			if (parentDatabaseObject instanceof ScreenClass) {
				sheet = ((ScreenClass) parentDatabaseObject).getSheet(browser);
			}
			else {
				sheet = ((Transaction) parentDatabaseObject).getSheet(browser);
			}
            
			if (sheet != null) {
				String message = java.text.MessageFormat.format(
					java.util.ResourceBundle.getBundle("com/twinsoft/convertigo/studio/res/actions/ClipboardPasteAction").getString("unable_to_paste_database_object.screen_class_already_has_such_sheet"),
					new Object[] { browser, parentDatabaseObject.getName() }
				);
				throw new EngineException(message);
			}
		}
        
		// Verifying if a child object with same name exist
        List<? extends DatabaseObject> lDatabaseObjects = null;
        
        if (parentDatabaseObject instanceof Project) {
    		if (object instanceof Connector) {
    			lDatabaseObjects = ((Project)parentDatabaseObject).getConnectorsList();
    		}
            else {
                throw new EngineException("You cannot paste to a project a database object of type " + object.getClass().getName());
            }
        }
        else if (parentDatabaseObject instanceof Sequence){
            if (object instanceof Sheet) {
            	lDatabaseObjects = ((Sequence)parentDatabaseObject).getSheetsList();
            }
            else if (object instanceof Step) {
            	lDatabaseObjects = ((Sequence)parentDatabaseObject).getSteps();
            }
            else if (object instanceof Variable) {
            	lDatabaseObjects = ((Sequence)parentDatabaseObject).getVariablesList();
            }
            else {
                throw new EngineException("You cannot paste to a sequence a database object of type " + object.getClass().getName());
            }
        }
        else if (parentDatabaseObject instanceof Connector){
    		if (object instanceof Transaction) {
    			lDatabaseObjects = ((Connector)parentDatabaseObject).getTransactionsList();
    		}
    		else if (object instanceof Pool) {
    			lDatabaseObjects = ((Connector)parentDatabaseObject).getPoolsList();
    		}
    		else {
    			throw new EngineException("You cannot paste to a connector a database object of type " + object.getClass().getName());
    		}
        	
        }
        else if (parentDatabaseObject instanceof Transaction){
            if (object instanceof Sheet) {
            	lDatabaseObjects = ((Transaction)parentDatabaseObject).getSheetsList();
            }
            else if ((object instanceof FunctionStatement) && (parentDatabaseObject instanceof HtmlTransaction)) {
            	lDatabaseObjects = ((HtmlTransaction)parentDatabaseObject).getStatements();
            }
            else if ((object instanceof Variable) && (parentDatabaseObject instanceof TransactionWithVariables)) {
            	lDatabaseObjects = ((TransactionWithVariables)parentDatabaseObject).getVariablesList();
            }
            else {
                throw new EngineException("You cannot paste to a transaction a database object of type " + object.getClass().getName());
            }
        }
        else if (parentDatabaseObject instanceof ScreenClass){
            if (object instanceof Criteria) {
            	lDatabaseObjects = ((ScreenClass)parentDatabaseObject).getLocalCriterias();
            }
            else if (object instanceof ExtractionRule) {
            	lDatabaseObjects = ((ScreenClass)parentDatabaseObject).getLocalExtractionRules();
            }
            else if (object instanceof Sheet) {
            	lDatabaseObjects = ((ScreenClass)parentDatabaseObject).getLocalSheets();
            }
            else if (object instanceof ScreenClass) {
            	lDatabaseObjects = ((ScreenClass)parentDatabaseObject).getInheritedScreenClasses();
            }
            else {
                throw new EngineException("You cannot paste to a screen class a database object of type " + object.getClass().getName());
            }
        }
        else if (parentDatabaseObject instanceof StatementWithExpressions){
            if (object instanceof Statement) {
            	lDatabaseObjects = ((StatementWithExpressions)parentDatabaseObject).getStatements();
            }
            else {
                throw new EngineException("You cannot paste to a statement a database object of type " + object.getClass().getName());
            }
        }
        else if (parentDatabaseObject instanceof HTTPStatement){
            if (object instanceof Variable) {
            	lDatabaseObjects = ((HTTPStatement)parentDatabaseObject).getVariables();
            }
            else {
                throw new EngineException("You cannot paste to a statement a database object of type " + object.getClass().getName());
            }
        }
        else if (parentDatabaseObject instanceof StepWithExpressions){
            if (object instanceof Step) {
            	lDatabaseObjects = ((StepWithExpressions)parentDatabaseObject).getSteps();
            }
            else {
                throw new EngineException("You cannot paste to a step a database object of type " + object.getClass().getName());
            }
        }
        else if (parentDatabaseObject instanceof RequestableStep){
            if (object instanceof Variable) {
            	lDatabaseObjects = ((RequestableStep)parentDatabaseObject).getVariables();
            }
            else {
                throw new EngineException("You cannot paste to a step a database object of type " + object.getClass().getName());
            }
        }
        else {
        	throw new EngineException("You cannot paste anything to database object of type " + parentDatabaseObject.getClass().getName());
        }

        String newDatabaseObjectName = object.getName();
        for (DatabaseObject databaseObject : lDatabaseObjects) {
            if (newDatabaseObjectName.equals(databaseObject.getName())) {
                throw new ObjectWithSameNameException("Unable to cut the object because an object with the same name already exists in target.");
            }
        }
		
		move(object, parentDatabaseObject);
	}
    
	/**
	 * Moves the object to a target destination.
	 */
	public synchronized void move(DatabaseObject object, DatabaseObject target) throws ConvertigoException {

		// First, delete the object from its parent
		DatabaseObject parent = object.getParent();
		Engine.theApp.databaseObjectsManager.delete(object);
		parent.remove(object);
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
		if (((target instanceof ScreenClass) && (!((ScreenClass)target).handlePriorities)) ||
			((target instanceof HtmlTransaction) && (!((HtmlTransaction)target).handlePriorities)) ||
			((target instanceof StatementWithExpressions) && (!((StatementWithExpressions)target).handlePriorities))) {
			object.bNew = true;
		}
		
		// Second, add the source to the target and
		target.add(object);
		//ConvertigoPlugin.projectManager.save(target, false);
		
		// Update sources that reference this step
		if (object instanceof Step)
			((Step)object).getSequence().fireStepMoved(new StepEvent(object,String.valueOf(oldPriority)));
		
		// save the object and all its children
		//ConvertigoPlugin.projectManager.save(object, true);
	}
}
