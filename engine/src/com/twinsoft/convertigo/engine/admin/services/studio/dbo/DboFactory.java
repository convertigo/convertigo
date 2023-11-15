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

package com.twinsoft.convertigo.engine.admin.services.studio.dbo;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IApplicationComponent;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.core.TransactionWithVariables;
import com.twinsoft.convertigo.beans.core.UrlMappingOperation;
import com.twinsoft.convertigo.beans.core.UrlMappingParameter;
import com.twinsoft.convertigo.beans.core.Variable;
import com.twinsoft.convertigo.beans.rest.FormParameter;
import com.twinsoft.convertigo.beans.rest.QueryParameter;
import com.twinsoft.convertigo.beans.steps.ElseStep;
import com.twinsoft.convertigo.beans.steps.IThenElseContainer;
import com.twinsoft.convertigo.beans.steps.SequenceStep;
import com.twinsoft.convertigo.beans.steps.ThenStep;
import com.twinsoft.convertigo.beans.steps.TransactionStep;
import com.twinsoft.convertigo.beans.steps.XMLElementStep;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.beans.variables.StepMultiValuedVariable;
import com.twinsoft.convertigo.beans.variables.StepVariable;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.enums.HttpMethodType;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class DboFactory {

	static protected boolean acceptDbo(DatabaseObject targetDatabaseObject, DatabaseObject databaseObject, boolean includeSpecials) {
		if (!acceptDboAsSuch(targetDatabaseObject, databaseObject)) {
			if (includeSpecials) {
				if (!acceptDboAsSpecial(targetDatabaseObject, databaseObject)) {
					return false;
				}
			} else {
				return false;
			}
		}
		return true;
	}
	
	static protected boolean acceptDboAsSuch(DatabaseObject targetDatabaseObject, DatabaseObject databaseObject) {
		if (!DatabaseObjectsManager.acceptDatabaseObjects(targetDatabaseObject, databaseObject)) {
			return false;
		}
		return true;
	}

	static protected boolean acceptDboAsSpecial(DatabaseObject targetDbo, DatabaseObject dbo) {
		// SEQUENCER
		if (targetDbo instanceof Sequence || targetDbo instanceof StepWithExpressions) {
			if (targetDbo instanceof XMLElementStep || targetDbo instanceof IThenElseContainer)
				return false;
			if (dbo instanceof Transaction || dbo instanceof Sequence) {
				return true;
			}
		}
		// URLMAPPER
		else if (targetDbo instanceof UrlMappingOperation) {
			if (dbo instanceof RequestableObject || dbo instanceof RequestableVariable) {
				return true;
			}
		}
		// MOBILE COMPONENTS
		else if (targetDbo instanceof com.twinsoft.convertigo.beans.mobile.components.MobileComponent) {
			return false;
		}
		// NGX COMPONENTS
		else if (targetDbo instanceof com.twinsoft.convertigo.beans.ngx.components.MobileComponent) {
			if (targetDbo.priority == dbo.priority) {
				return true;
			}
			if (dbo instanceof Sequence) {
				if (targetDbo instanceof com.twinsoft.convertigo.beans.ngx.components.UIForm
						|| targetDbo instanceof com.twinsoft.convertigo.beans.ngx.components.IEventGenerator
						|| targetDbo instanceof com.twinsoft.convertigo.beans.ngx.components.IAction
						|| targetDbo instanceof com.twinsoft.convertigo.beans.ngx.components.UIActionStack) {
					return true;
				}
			} else if (dbo instanceof com.twinsoft.convertigo.beans.ngx.components.UIActionStack) {
				if (targetDbo instanceof com.twinsoft.convertigo.beans.ngx.components.IEventGenerator
						|| targetDbo instanceof com.twinsoft.convertigo.beans.ngx.components.IAction
						|| targetDbo instanceof com.twinsoft.convertigo.beans.ngx.components.UIActionStack) {
					return true;
				}
			} else if (dbo instanceof com.twinsoft.convertigo.beans.ngx.components.UISharedComponent) {
				if (targetDbo instanceof com.twinsoft.convertigo.beans.ngx.components.PageComponent
						|| targetDbo instanceof com.twinsoft.convertigo.beans.ngx.components.UISharedComponent
						|| targetDbo instanceof com.twinsoft.convertigo.beans.ngx.components.UIElement
								&& !(targetDbo instanceof com.twinsoft.convertigo.beans.ngx.components.UIUseShared)) {
					return true;
				}
			}
		}
		return false;
	}
	
	static protected DatabaseObject createDbo(DatabaseObject parentDbo, DatabaseObject dbo) throws Exception {
		if (acceptDboAsSuch(parentDbo, dbo)) {
			return createSuchDbo(dbo);
		} else if (acceptDboAsSpecial(parentDbo, dbo)) {
			return createSpecialDbo(dbo, parentDbo);
		}
		return null;
	}
	
	static protected DatabaseObject createSuchDbo(DatabaseObject dbo) throws Exception {
		Document document = XMLUtils.getDefaultDocumentBuilder().newDocument();
		document.appendChild(document.createElement("convertigo"));
		DboUtils.xmlCopy(document, dbo);
		
		NodeList nodeList = document.getDocumentElement().getChildNodes();
		Node node = nodeList.item(0);
		Object object = DboUtils.read(node);
		if (object instanceof DatabaseObject) {
			DatabaseObject databaseObject = (DatabaseObject) object;

			NodeList childNodes = node.getChildNodes();
			int len = childNodes.getLength();

			Node childNode;
			String childNodeName;
			for (int i = 0; i < len; i++) {
				childNode = childNodes.item(i);
				if (childNode.getNodeType() != Node.ELEMENT_NODE) {
					continue;
				}
				childNodeName = childNode.getNodeName();
				if (!(childNodeName.equalsIgnoreCase("property")) && !(childNodeName.equalsIgnoreCase("handlers"))
						&& !(childNodeName.equalsIgnoreCase("wsdltype")) && !(childNodeName.equalsIgnoreCase("docdata"))
						&& !(childNodeName.equalsIgnoreCase("dnd"))) {
					DboUtils.xmlPaste(childNode, databaseObject);
				}
			}

			databaseObject.bNew = true;
			databaseObject.priority = databaseObject.getNewOrderValue();
			databaseObject.isImporting = false;
			databaseObject.isSubLoaded = true;
			return databaseObject;
		}
		return null;
	}
	
	static protected DatabaseObject createSpecialDbo(DatabaseObject dbo, DatabaseObject parentDbo) throws Exception {
		// SEQUENCER
		if (parentDbo instanceof Sequence || parentDbo instanceof StepWithExpressions) {
			if (parentDbo instanceof XMLElementStep)
				return null;
			if (parentDbo instanceof IThenElseContainer)
				return null;

			if (dbo instanceof Transaction) {
				return createTransactionStep((Transaction) dbo);
			} else if (dbo instanceof Sequence) {
				return createSequenceStep((Sequence) dbo);
			}
		}
		// URLMAPPER
		else if (parentDbo instanceof UrlMappingOperation) {
			if (dbo instanceof RequestableObject) {
				return updateUrlMappingOperation((UrlMappingOperation) parentDbo, (RequestableObject) dbo);
			} else if (dbo instanceof RequestableVariable) {
				return createUrlMappingParameter((UrlMappingOperation) parentDbo, (RequestableVariable) dbo);
			}
		}
		// MOBILE COMPONENTS
		else if (parentDbo instanceof com.twinsoft.convertigo.beans.mobile.components.MobileComponent) {
			return null;
		}
		// NGX COMPONENTS
		else if (parentDbo instanceof com.twinsoft.convertigo.beans.ngx.components.MobileComponent) {
			if (dbo instanceof Sequence) {
				if (parentDbo instanceof com.twinsoft.convertigo.beans.ngx.components.UIForm) {
					return updateNgxUIForm((com.twinsoft.convertigo.beans.ngx.components.UIForm)parentDbo, (Sequence)dbo);
				} else if (parentDbo instanceof com.twinsoft.convertigo.beans.ngx.components.IEventGenerator
						|| parentDbo instanceof com.twinsoft.convertigo.beans.ngx.components.IAction
						|| parentDbo instanceof com.twinsoft.convertigo.beans.ngx.components.UIActionStack) {
					return createNgxCallSequenceAction((Sequence) dbo);
				}
			} else if (dbo instanceof com.twinsoft.convertigo.beans.ngx.components.UIActionStack) {
				if (parentDbo instanceof com.twinsoft.convertigo.beans.ngx.components.IEventGenerator
						|| parentDbo instanceof com.twinsoft.convertigo.beans.ngx.components.IAction
						|| parentDbo instanceof com.twinsoft.convertigo.beans.ngx.components.UIActionStack) {
					return createNgxInvokesharedAction((com.twinsoft.convertigo.beans.ngx.components.UIActionStack) dbo);
				}
			} else if (dbo instanceof com.twinsoft.convertigo.beans.ngx.components.UISharedComponent) {
				if (parentDbo instanceof com.twinsoft.convertigo.beans.ngx.components.PageComponent
						|| parentDbo instanceof com.twinsoft.convertigo.beans.ngx.components.UISharedComponent
						|| parentDbo instanceof com.twinsoft.convertigo.beans.ngx.components.UIElement
								&& !(parentDbo instanceof com.twinsoft.convertigo.beans.ngx.components.UIUseShared)) {
					return createNgxUseShared((com.twinsoft.convertigo.beans.ngx.components.UISharedComponent)dbo);
				}
			}
		}
		return null;
	}

	static private DatabaseObject updateNgxUIForm(com.twinsoft.convertigo.beans.ngx.components.UIForm uiForm, Sequence sequence) throws Exception {
		// TODO
		return uiForm;
	}
	
	static private DatabaseObject createNgxUseShared(com.twinsoft.convertigo.beans.ngx.components.UISharedComponent sharedComp) throws Exception {
		com.twinsoft.convertigo.beans.ngx.components.UIUseShared use = new com.twinsoft.convertigo.beans.ngx.components.UIUseShared();
		if (use != null) {
			use.setSharedComponentQName(sharedComp.getQName());
			use.bNew = true;
			use.hasChanged = true;
		}
		return use;
	}

	static private DatabaseObject createNgxInvokesharedAction(com.twinsoft.convertigo.beans.ngx.components.UIActionStack stack) throws Exception {
		DatabaseObject invokeAction = com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.createBean(com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.getComponentByName("InvokeAction"));
		com.twinsoft.convertigo.beans.ngx.components.UIDynamicInvoke invoke = GenericUtils.cast(invokeAction);
		if (invoke != null) {
			invoke.setSharedActionQName(stack.getQName());
			invoke.bNew = true;
			invoke.hasChanged = true;
		}
		return invokeAction;
	}

	static private DatabaseObject createNgxCallSequenceAction(Sequence sequence) throws Exception {
		DatabaseObject call = com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.createBean(com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.getComponentByName("CallSequenceAction"));
		if (call != null && call instanceof com.twinsoft.convertigo.beans.ngx.components.UIDynamicAction) {
			com.twinsoft.convertigo.beans.ngx.components.UIDynamicAction dynAction = GenericUtils.cast(call);
			com.twinsoft.convertigo.beans.ngx.components.dynamic.IonBean ionBean = dynAction.getIonBean();
			if (ionBean != null && ionBean.hasProperty("requestable")) {
				ionBean.setPropertyValue("requestable", new com.twinsoft.convertigo.beans.ngx.components.MobileSmartSourceType(sequence.getQName()));
				call.bNew = true;
				call.hasChanged = true;
			}
		}
		return call;
	}

	static private DatabaseObject createUrlMappingParameter(UrlMappingOperation operation, RequestableVariable variable) throws Exception {
		UrlMappingParameter parameter = null;
		String variableName = variable.getName();
		Object variableValue = variable.getValueOrNull();
		try {
			parameter = operation.getParameterByName(variableName);
		} catch (Exception e) {
		}
		if (parameter == null) {
			boolean acceptForm = operation.getMethod().equalsIgnoreCase(HttpMethodType.POST.name())
					|| operation.getMethod().equalsIgnoreCase(HttpMethodType.PUT.name());
			parameter = acceptForm ? new FormParameter() : new QueryParameter();
			parameter.setName(variableName);
			parameter.setComment(variable.getComment());
			parameter.setArray(false);
			parameter.setExposed(((RequestableVariable) variable).isWsdl());
			parameter.setMultiValued(variable.isMultiValued());
			parameter.setRequired(variable.isRequired());
			parameter.setValueOrNull(!variable.isMultiValued() ? variableValue : null);
			parameter.setMappedVariableName(variableName);
			parameter.bNew = true;
		}
		return parameter;
	}

	static private DatabaseObject updateUrlMappingOperation(UrlMappingOperation operation, RequestableObject requestableObject) throws Exception {
		operation.setTargetRequestable(requestableObject.getQName());
		if (operation.getComment().isEmpty()) {
			operation.setComment(requestableObject.getComment());
		}
		try {
			List<RequestableVariable> variables = new ArrayList<RequestableVariable>();
			if (requestableObject instanceof TransactionWithVariables) {
				variables = ((TransactionWithVariables) requestableObject).getVariablesList();
			}
			if (requestableObject instanceof Sequence) {
				variables = ((Sequence) requestableObject).getVariablesList();
			}
			for (RequestableVariable variable : variables) {
				String variableName = variable.getName();
				Object variableValue = variable.getValueOrNull();
				UrlMappingParameter parameter = null;
				try {
					parameter = operation.getParameterByName(variableName);
				} catch (Exception e) {
				}
				if (parameter == null) {
					boolean acceptForm = operation.getMethod().equalsIgnoreCase(HttpMethodType.POST.name())
							|| operation.getMethod().equalsIgnoreCase(HttpMethodType.PUT.name());
					parameter = acceptForm ? new FormParameter() : new QueryParameter();
					parameter.setName(variableName);
					parameter.setComment(variable.getComment());
					parameter.setArray(false);
					parameter.setExposed(((RequestableVariable) variable).isWsdl());
					parameter.setMultiValued(variable.isMultiValued());
					parameter.setRequired(variable.isRequired());
					parameter.setValueOrNull(!variable.isMultiValued() ? variableValue : null);
					parameter.setMappedVariableName(variableName);
					parameter.bNew = true;
					operation.add(parameter);
				}
			}
		} catch (Exception e) {
		}
		operation.hasChanged = true;
		return operation;
	}

	static private DatabaseObject createSequenceStep(Sequence sequence) throws Exception {
		SequenceStep sequenceStep = new SequenceStep();
		sequenceStep.setSourceSequence(sequence.getQName());
		sequenceStep.bNew = true;
		for (Variable variable : sequence.getVariablesList()) {
			StepVariable stepVariable = variable.isMultiValued() ? new StepMultiValuedVariable() : new StepVariable();
			stepVariable.setName(variable.getName());
			stepVariable.setComment(variable.getComment());
			stepVariable.setDescription(variable.getDescription());
			stepVariable.setRequired(variable.isRequired());
			stepVariable.setValueOrNull(variable.getValueOrNull());
			stepVariable.setVisibility(variable.getVisibility());
			sequenceStep.addVariable(stepVariable);
		}
		return sequenceStep;
	}

	static private DatabaseObject createTransactionStep(Transaction transaction) throws Exception {
		TransactionStep transactionStep = new TransactionStep();
		transactionStep.setSourceTransaction(transaction.getQName());
		transactionStep.bNew = true;
		if (transaction instanceof TransactionWithVariables) {
			for (Variable variable : ((TransactionWithVariables) transaction).getVariablesList()) {
				StepVariable stepVariable = variable.isMultiValued() ? new StepMultiValuedVariable()
						: new StepVariable();
				stepVariable.setName(variable.getName());
				stepVariable.setComment(variable.getComment());
				stepVariable.setDescription(variable.getDescription());
				stepVariable.setRequired(variable.isRequired());
				stepVariable.setValueOrNull(variable.getValueOrNull());
				stepVariable.setVisibility(variable.getVisibility());
				transactionStep.addVariable(stepVariable);
			}
		}
		return transactionStep;
	}
	
	static protected boolean isCuttable(DatabaseObject dbo) {
		if (dbo.getParent() == null) {
			return false;
		}
		
		if (dbo instanceof Project) {
			return false;
		} else if (dbo instanceof Connector) {
			return !((Connector)dbo).isDefault;
		} else if (dbo instanceof ThenStep || dbo instanceof ElseStep) {
			return false;
		} else if (dbo instanceof IApplicationComponent) {
			return false;
		} else if (dbo instanceof com.twinsoft.convertigo.beans.mobile.components.MobileComponent) {
			return false;
		} else if (dbo instanceof com.twinsoft.convertigo.beans.ngx.components.PageComponent) {
			return !((com.twinsoft.convertigo.beans.ngx.components.PageComponent)dbo).isRoot;
		}
		return true;
	}
}
