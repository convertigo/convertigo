/*
 * Copyright (c) 2001-2019 Convertigo SA.
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

package com.twinsoft.convertigo.engine.helpers;

import com.twinsoft.convertigo.beans.core.BlockFactory;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Criteria;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Document;
import com.twinsoft.convertigo.beans.core.ExtractionRule;
import com.twinsoft.convertigo.beans.core.IScreenClassContainer;
import com.twinsoft.convertigo.beans.core.Index;
import com.twinsoft.convertigo.beans.core.Listener;
import com.twinsoft.convertigo.beans.core.MobileApplication;
import com.twinsoft.convertigo.beans.core.UrlMapper;
import com.twinsoft.convertigo.beans.core.MobilePlatform;
import com.twinsoft.convertigo.beans.core.Pool;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Reference;
import com.twinsoft.convertigo.beans.core.RequestableStep;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Sheet;
import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.beans.core.StatementWithExpressions;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.beans.core.TestCase;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.core.TransactionWithVariables;
import com.twinsoft.convertigo.beans.core.UrlAuthentication;
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
import com.twinsoft.convertigo.beans.mobile.components.UIDynamicMenu;
import com.twinsoft.convertigo.beans.mobile.components.UISharedComponent;
import com.twinsoft.convertigo.beans.mobile.components.UIActionStack;
import com.twinsoft.convertigo.beans.screenclasses.JavelinScreenClass;
import com.twinsoft.convertigo.beans.statements.HTTPStatement;
import com.twinsoft.convertigo.beans.transactions.HtmlTransaction;
import com.twinsoft.convertigo.beans.variables.HttpStatementVariable;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;

public class WalkHelper {
	protected boolean walkInheritance = false;
	
	public void init(DatabaseObject databaseObject) throws Exception {
		walk(databaseObject);
	}
	
	protected boolean before(DatabaseObject databaseObject, Class<? extends DatabaseObject> dboClass) {
		return true;
	}

	protected void walk(DatabaseObject databaseObject) throws Exception {
		if (databaseObject instanceof Project) {
			Project project = (Project) databaseObject;

			if (before(databaseObject, Connector.class)) {
				for (Connector connector : project.getConnectorsList()) {
					walk(connector);
				}
			}

			if (before(databaseObject, Sequence.class)) {
				for (Sequence sequence : project.getSequencesList()) {
					walk(sequence);
				}
			}

			if (before(databaseObject, MobileApplication.class)) {
				MobileApplication mobileApplication = project.getMobileApplication();
				if (mobileApplication != null) {
					walk(mobileApplication);
				}
			}
			
			if (before(databaseObject, UrlMapper.class)) {
				UrlMapper urlMapper = project.getUrlMapper();
				if (urlMapper != null) {
					walk(urlMapper);
				}
			}
			
			if (before(databaseObject, Reference.class)) {
				for (Reference reference : project.getReferenceList()) {
					walk(reference);
				}
			}
		} else if (databaseObject instanceof MobileApplication) {
			MobileApplication mobileApplication = (MobileApplication) databaseObject;

			if (before(databaseObject, MobilePlatform.class)) {
				for (MobilePlatform device : mobileApplication.getMobilePlatformList()) {
					walk(device);
				}
			}
			
			if (before(databaseObject, ApplicationComponent.class)) {
				ApplicationComponent component = mobileApplication.getApplicationComponent();
				if (component != null) {
					walk(component);
				}
			}
		} else if (databaseObject instanceof ApplicationComponent) {
			ApplicationComponent applicationComponent = (ApplicationComponent) databaseObject;
			
			if (before(databaseObject, RouteComponent.class)) {
				for (RouteComponent route : applicationComponent.getRouteComponentList()) {
					walk(route);
				}
			}
			
			if (before(databaseObject, UIDynamicMenu.class)) {
				for (UIDynamicMenu menu : applicationComponent.getMenuComponentList()) {
					walk(menu);
				}
			}
			
			if (before(databaseObject, UIActionStack.class)) {
				for (UIActionStack uisa : applicationComponent.getSharedActionList()) {
					walk(uisa);
				}
			}
			
			if (before(databaseObject, UISharedComponent.class)) {
				for (UISharedComponent uisc : applicationComponent.getSharedComponentList()) {
					walk(uisc);
				}
			}
			
			if (before(databaseObject, PageComponent.class)) {
				for (PageComponent page : applicationComponent.getPageComponentList()) {
					walk(page);
				}
			}
			
			if (before(databaseObject, UIComponent.class)) {
				for (UIComponent ui : applicationComponent.getUIComponentList()) {
					walk(ui);
				}
			}
			
		} else if (databaseObject instanceof RouteComponent) {
			RouteComponent routingListenerComponent = (RouteComponent) databaseObject;
			
			if (before(databaseObject, RouteEventComponent.class)) {
				for (RouteEventComponent event : routingListenerComponent.getRouteEventComponentList()) {
					walk(event);
				}
			}
			
			if (before(databaseObject, RouteActionComponent.class)) {
				for (RouteActionComponent action : routingListenerComponent.getRouteActionComponentList()) {
					walk(action);
				}
			}
		} else if (databaseObject instanceof PageComponent) {
			PageComponent pageComponent = (PageComponent) databaseObject;
			
			if (before(databaseObject, UIComponent.class)) {
				for (UIComponent ui : pageComponent.getUIComponentList()) {
					walk(ui);
				}
			}
		} else if (databaseObject instanceof UIActionStack) {
			UIActionStack uisa = (UIActionStack)databaseObject;
			
			if (before(databaseObject, UIComponent.class)) {
				for (UIComponent ui : uisa.getUIComponentList()) {
					walk(ui);
				}
			}
		} else if (databaseObject instanceof UISharedComponent) {
			UISharedComponent uisc = (UISharedComponent)databaseObject;
			
			if (before(databaseObject, UIComponent.class)) {
				for (UIComponent ui : uisc.getUIComponentList()) {
					walk(ui);
				}
			}
		} else if (databaseObject instanceof UIDynamicMenu) {
			UIDynamicMenu uiMenu = (UIDynamicMenu) databaseObject;
			
			if (before(databaseObject, UIComponent.class)) {
				for (UIComponent ui : uiMenu.getUIComponentList()) {
					walk(ui);
				}
			}
		} else if (databaseObject instanceof UIComponent) {
			UIComponent uiComponent = (UIComponent) databaseObject;
			
			if (before(databaseObject, UIComponent.class)) {
				for (UIComponent ui : uiComponent.getUIComponentList()) {
					walk(ui);
				}
			}
		} else if (databaseObject instanceof UrlMapper) {
			UrlMapper urlMapper = (UrlMapper) databaseObject;

			if (before(databaseObject, UrlAuthentication.class)) {
				for (UrlAuthentication authentication : urlMapper.getAuthenticationList()) {
					walk(authentication);
				}
			}
			
			if (before(databaseObject, UrlMapping.class)) {
				for (UrlMapping mapping : urlMapper.getMappingList()) {
					walk(mapping);
				}
			}
		} else if (databaseObject instanceof UrlMapping) {
			UrlMapping urlMapping = (UrlMapping) databaseObject;

			if (before(databaseObject, UrlMappingOperation.class)) {
				for (UrlMappingOperation operation : urlMapping.getOperationList()) {
					walk(operation);
				}
			}
		} else if (databaseObject instanceof UrlMappingOperation) {
			UrlMappingOperation urlMappingOperation = (UrlMappingOperation) databaseObject;

			if (before(databaseObject, UrlMappingParameter.class)) {
				for (UrlMappingParameter parameter : urlMappingOperation.getParameterList()) {
					walk(parameter);
				}
			}
			
			if (before(databaseObject, UrlMappingResponse.class)) {
				for (UrlMappingResponse response : urlMappingOperation.getResponseList()) {
					walk(response);
				}
			}
		} else if (databaseObject instanceof Sequence) {
			Sequence sequence = (Sequence) databaseObject;

			if (before(databaseObject, Step.class)) {
				for (Step step : sequence.getSteps()) {
					walk(step);
				}
			}

			if (before(databaseObject, Sheet.class)) {
				for (Sheet sheet : sequence.getSheetsList()) {
					walk(sheet);
				}
			}

			if (before(databaseObject, RequestableVariable.class)) {
				for (RequestableVariable variable : sequence.getVariablesList()) {
					walk(variable);
				}
			}

			if (before(databaseObject, TestCase.class)) {
				for (TestCase testCase : sequence.getTestCasesList()) {
					walk(testCase);
				}
			}
		} else if (databaseObject instanceof Connector) {
			Connector connector = (Connector) databaseObject;

			if (databaseObject instanceof IScreenClassContainer<?>) {
				if (before(databaseObject, ScreenClass.class)) {
					ScreenClass defaultScreenClass = ((IScreenClassContainer<?>) databaseObject).getDefaultScreenClass();
					if (defaultScreenClass != null) {
						walk(defaultScreenClass);
					}
				}
			}

			if (before(databaseObject, Transaction.class)) {
				for (Transaction transaction : connector.getTransactionsList()) {
					walk(transaction);
				}
			}

			if (before(databaseObject, Pool.class)) {
				for (Pool pool : connector.getPoolsList()) {
					walk(pool);
				}
			}
			
			if (before(databaseObject, Document.class)) {
				for (Document document : connector.getDocumentsList()) {
					walk(document);
				}
			}
			
			if (before(databaseObject, Listener.class)) {
				for (Listener listener : connector.getListenersList()) {
					walk(listener);
				}
			}
			
			if (before(databaseObject, Index.class)) {
				for (Index index : connector.getIndexList()) {
					walk(index);
				}
			}
		} else if (databaseObject instanceof Transaction) {
			Transaction transaction = (Transaction) databaseObject;

			if (before(databaseObject, Sheet.class)) {
				for (Sheet sheet : transaction.getSheetsList()) {
					walk(sheet);
				}
			}

			if (transaction instanceof HtmlTransaction) {
				HtmlTransaction htmlTransaction = (HtmlTransaction) transaction;

				if (before(databaseObject, Statement.class)) {
					for (Statement statement : htmlTransaction.getStatements()) {
						walk(statement);
					}
				}
			}

			if (databaseObject instanceof TransactionWithVariables) {
				if (before(databaseObject, TestCase.class)) {
					for (TestCase testCase : ((TransactionWithVariables) databaseObject).getTestCasesList()) {
						walk(testCase);
					}
				}

				if (before(databaseObject, RequestableVariable.class)) {
					for (RequestableVariable variable : ((TransactionWithVariables) databaseObject).getVariablesList()) {
						walk(variable);
					}
				}
			}
		} else if (databaseObject instanceof StatementWithExpressions) {
			if (before(databaseObject, Statement.class)) {
				for (Statement statement : ((StatementWithExpressions) databaseObject).getStatements()) {
					walk(statement);
				}
			}
		} else if (databaseObject instanceof HTTPStatement) {
			if (before(databaseObject, HttpStatementVariable.class)) {
				for (HttpStatementVariable variable : ((HTTPStatement) databaseObject).getVariables()) {
					walk(variable);
				}
			}
		} else if (databaseObject instanceof StepWithExpressions) {
			if (before(databaseObject, Step.class)) {
				for (Step step : ((StepWithExpressions) databaseObject).getSteps()) {
					walk(step);
				}
			}
		} else if (databaseObject instanceof RequestableStep) {
			if (before(databaseObject, Variable.class)) {
				for (Variable variable : ((RequestableStep) databaseObject).getVariables()) {
					walk(variable);
				}
			}
		} else if (databaseObject instanceof TestCase) {
			if (before(databaseObject, Variable.class)) {
				for (Variable variable : ((TestCase) databaseObject).getVariables()) {
					walk(variable);
				}
			}
		} else if (databaseObject instanceof ScreenClass) {
			ScreenClass screenClass = (ScreenClass) databaseObject;
			if (screenClass instanceof JavelinScreenClass) {
				JavelinScreenClass javelinScreenClass = (JavelinScreenClass) screenClass;
				if (before(databaseObject, BlockFactory.class)) {
					BlockFactory blockFactory = walkInheritance ? javelinScreenClass.getBlockFactory() : javelinScreenClass.getLocalBlockFactory();
					if (blockFactory != null) {
						walk(blockFactory);
					}
				}
			}

			if (before(databaseObject, Criteria.class)) {
				for (Criteria criteria : walkInheritance ? screenClass.getCriterias() : screenClass.getLocalCriterias()) {
					walk(criteria);
				}
			}

			if (before(databaseObject, ExtractionRule.class)) {
				for (ExtractionRule extractionRule : walkInheritance ? screenClass.getExtractionRules() : screenClass.getLocalExtractionRules()) {
					walk(extractionRule);
				}
			}

			if (before(databaseObject, Sheet.class)) {
				for (Sheet sheet : walkInheritance ? screenClass.getSheets() : screenClass.getLocalSheets()) {
					walk(sheet);
				}
			}

			if (before(databaseObject, ScreenClass.class)) {
				for (ScreenClass inheritedScreenClass : ((ScreenClass) databaseObject).getInheritedScreenClasses()) {
					walk(inheritedScreenClass);
				}
			}
		}
	}
}
