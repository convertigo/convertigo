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

package com.twinsoft.convertigo.engine.studio;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.twinsoft.convertigo.beans.connectors.CouchDbConnector;
import com.twinsoft.convertigo.beans.connectors.FullSyncConnector;
import com.twinsoft.convertigo.beans.connectors.JavelinConnector;
import com.twinsoft.convertigo.beans.connectors.SiteClipperConnector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.ITestCaseContainer;
import com.twinsoft.convertigo.beans.core.IVariableContainer;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.core.TransactionWithVariables;
import com.twinsoft.convertigo.beans.core.UrlMappingOperation;
import com.twinsoft.convertigo.beans.core.UrlMappingParameter;
import com.twinsoft.convertigo.beans.references.ProjectSchemaReference;
import com.twinsoft.convertigo.beans.references.WebServiceReference;
import com.twinsoft.convertigo.beans.rest.BodyParameter;
import com.twinsoft.convertigo.beans.rest.FormParameter;
import com.twinsoft.convertigo.beans.rest.HeaderParameter;
import com.twinsoft.convertigo.beans.rest.PathParameter;
import com.twinsoft.convertigo.beans.rest.PostOperation;
import com.twinsoft.convertigo.beans.rest.PutOperation;
import com.twinsoft.convertigo.beans.rest.QueryParameter;
import com.twinsoft.convertigo.beans.steps.AttributeStep;
import com.twinsoft.convertigo.beans.steps.ElementStep;
import com.twinsoft.convertigo.beans.steps.JsonFieldStep;
import com.twinsoft.convertigo.beans.steps.XMLAttributeStep;
import com.twinsoft.convertigo.beans.steps.XMLConcatStep;
import com.twinsoft.convertigo.beans.steps.XMLElementStep;
import com.twinsoft.convertigo.beans.transactions.SiteClipperTransaction;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.util.GenericUtils;

public class DatabaseObjectsAction {
	
	public static ActionModel selectionChanged(String popupActionClass, DatabaseObject dbo) {
		ActionModel actionModel = new ActionModel();

		switch (popupActionClass) {
			case "com.twinsoft.convertigo.eclipse.popup.actions.ChangeToAttributeStepAction": {
				DatabaseObject dboParent = dbo.getParent();
				actionModel.isEnabled = false;
				if (DatabaseObjectsManager.acceptDatabaseObjects(dboParent, new AttributeStep())) {
					actionModel.isEnabled = dbo instanceof XMLAttributeStep ||dbo instanceof ElementStep;
				}
			}
			break;

			case "com.twinsoft.convertigo.eclipse.popup.actions.ChangeToBodyParameterAction": {
				UrlMappingParameter parameter = (UrlMappingParameter) dbo;
				UrlMappingOperation operation = (UrlMappingOperation) parameter.getParent();
				actionModel.isEnabled = !(parameter instanceof BodyParameter) && !(parameter instanceof PathParameter) 
						&& (operation instanceof PostOperation || operation instanceof PutOperation);
				
				
				if (actionModel.isEnabled) {
					List<UrlMappingParameter> params = operation.getParameterList();
					if (params.size() == 1) {
						actionModel.isEnabled = true;
					}
					else if (params.size() > 1) {
						for (UrlMappingParameter param : params) {
							if (!(param instanceof HeaderParameter)) {
								actionModel.isEnabled = false;
								break;
							}
						}
					}
					else {
						actionModel.isEnabled = false;
					}
				}
			}
			break;

			case "com.twinsoft.convertigo.eclipse.popup.actions.ChangeToElementStepAction": {
				actionModel.isEnabled = false;
				DatabaseObject dboParent = dbo.getParent();
				if (DatabaseObjectsManager.acceptDatabaseObjects(dboParent, new ElementStep())) {
					actionModel.isEnabled = dbo instanceof AttributeStep || dbo instanceof XMLElementStep || dbo instanceof JsonFieldStep;
				}
			}
			break;

			case "com.twinsoft.convertigo.eclipse.popup.actions.ChangeToFormParameterAction": {
				UrlMappingParameter parameter = (UrlMappingParameter) dbo;
				UrlMappingOperation operation = (UrlMappingOperation) parameter.getParent();
				actionModel.isEnabled = !(parameter instanceof FormParameter) && !(parameter instanceof PathParameter) 
									&& (operation instanceof PostOperation || operation instanceof PutOperation);
			}
			break;
			
			case "com.twinsoft.convertigo.eclipse.popup.actions.ChangeToHeaderParameterAction": {
				UrlMappingParameter parameter = (UrlMappingParameter) dbo;
				actionModel.isEnabled = !(parameter instanceof HeaderParameter) && !(parameter instanceof PathParameter);
			}
			break;

			case "com.twinsoft.convertigo.eclipse.popup.actions.ChangeToQueryParameterAction": {
				UrlMappingParameter parameter = (UrlMappingParameter) dbo;
				UrlMappingOperation operation = (UrlMappingOperation) parameter.getParent();
				actionModel.isEnabled = !(parameter instanceof QueryParameter) && !(parameter instanceof PathParameter) 
									&& !(operation instanceof PutOperation);
			}
			break;
			
			case "com.twinsoft.convertigo.eclipse.popup.actions.ChangeToXMLAttributeStepAction": {
				actionModel.isEnabled = false;
				
				DatabaseObject dboParent = dbo.getParent();
				if (DatabaseObjectsManager.acceptDatabaseObjects(dboParent, new XMLAttributeStep())) {
					actionModel.isEnabled = dbo instanceof AttributeStep || dbo instanceof XMLElementStep;
				}
			}
			break;
			
			case "com.twinsoft.convertigo.eclipse.popup.actions.ChangeToXMLElementStepAction": {
				actionModel.isEnabled = false;
				
				DatabaseObject dboParent = dbo.getParent();
				if (DatabaseObjectsManager.acceptDatabaseObjects(dboParent, new XMLElementStep())) {
					actionModel.isEnabled =
							dbo instanceof XMLConcatStep ||
							dbo instanceof XMLAttributeStep ||
							dbo instanceof ElementStep ||
							dbo instanceof JsonFieldStep;
				}
			}
			break;
			
//			case "com.twinsoft.convertigo.eclipse.popup.actions.CreateDesignDocumentFilterAction": {
//			}
//			break;
//			
//			case "com.twinsoft.convertigo.eclipse.popup.actions.CreateDesignDocumentUpdateAction": {
//			}
//			break;
//			
//			case "com.twinsoft.convertigo.eclipse.popup.actions.CreateDesignDocumentViewAction": {
//			}
//			break;
//			
//			case "com.twinsoft.convertigo.eclipse.popup.actions.CreateDesignDocumentViewReduceAction": {
//			}
//			break;
//			
			case "com.twinsoft.convertigo.eclipse.popup.actions.CreateDocumentAction": {
				actionModel.isEnabled = true;
				if (dbo instanceof Connector) {
					actionModel.isEnabled  = dbo instanceof CouchDbConnector;
				}
			}
			break;
			
			case "com.twinsoft.convertigo.eclipse.popup.actions.CreateEmptyStubAction": {
				actionModel.isEnabled = !(dbo instanceof SiteClipperTransaction);
			}
			break;
			
			case "com.twinsoft.convertigo.eclipse.popup.actions.CreateFormFromTransactionAction": {
				actionModel.isEnabled = (dbo instanceof TransactionWithVariables) && !(dbo instanceof SiteClipperTransaction);
			}
			break;
			
			case "com.twinsoft.convertigo.eclipse.popup.actions.CreateListenerAction": {
				actionModel.isEnabled = true;
				if (dbo instanceof Connector) {
					actionModel.isEnabled = dbo instanceof FullSyncConnector;
				}
			}
			break;
			
			case "com.twinsoft.convertigo.eclipse.popup.actions.CreateIndexAction": {
				actionModel.isEnabled = true;
				if (dbo instanceof Connector) {
					actionModel.isEnabled = dbo instanceof CouchDbConnector;
				}
			}
			break;
			
			case "com.twinsoft.convertigo.eclipse.popup.actions.CreateMobileUIComponentAction": {
				actionModel.isEnabled = !(dbo instanceof com.twinsoft.convertigo.beans.mobile.components.UIText ||
					    dbo instanceof com.twinsoft.convertigo.beans.mobile.components.UIAttribute || 
					    dbo instanceof com.twinsoft.convertigo.beans.mobile.components.UIStyle || 
					    dbo instanceof com.twinsoft.convertigo.beans.mobile.components.UIControlVariable);
				if (!actionModel.isEnabled && dbo instanceof com.twinsoft.convertigo.beans.mobile.components.UIControlAttr) {
				    actionModel.isEnabled = true;
				}
			}
			break;
			
			case "com.twinsoft.convertigo.eclipse.popup.actions.CreateNgxUIComponentAction": {
				actionModel.isEnabled = !(dbo instanceof com.twinsoft.convertigo.beans.ngx.components.UIText ||
					    dbo instanceof com.twinsoft.convertigo.beans.ngx.components.UIAttribute || 
					    dbo instanceof com.twinsoft.convertigo.beans.ngx.components.UIStyle || 
					    dbo instanceof com.twinsoft.convertigo.beans.ngx.components.UIControlVariable);
				if (!actionModel.isEnabled && dbo instanceof com.twinsoft.convertigo.beans.ngx.components.UIControlAttr) {
				    actionModel.isEnabled = true;
				}
			}
			break;
			
			case "com.twinsoft.convertigo.eclipse.popup.actions.CreateMobileRouteActionComponentAction": {
				actionModel.isEnabled = dbo instanceof com.twinsoft.convertigo.beans.mobile.components.RouteComponent;
			}
			break;
			
			case "com.twinsoft.convertigo.eclipse.popup.actions.CreateMobileRouteEventComponentAction": {
				actionModel.isEnabled = dbo instanceof com.twinsoft.convertigo.beans.mobile.components.RouteComponent;
			}
			break;
			
			case "com.twinsoft.convertigo.eclipse.popup.actions.CreatePoolAction": {
				actionModel.isEnabled = true;
				if (dbo instanceof Connector) {
					actionModel.isEnabled = dbo instanceof JavelinConnector;
				}
			}
			break;
//			
//			case "com.twinsoft.convertigo.eclipse.popup.actions.CreatePropertyTableColumnAction": {
//			}
//			break;
//			
//			case "com.twinsoft.convertigo.eclipse.popup.actions.CreatePropertyTableRowAction": {
//			}
//			break;
			
			case "com.twinsoft.convertigo.eclipse.popup.actions.CreateSheetAction": {
				actionModel.isEnabled = true;
				if (dbo instanceof ScreenClass || dbo instanceof Transaction) {
					actionModel.isEnabled = !(dbo.getConnector() instanceof SiteClipperConnector);
				}
			}
			break;
			
			case "com.twinsoft.convertigo.eclipse.popup.actions.CreateStubFromXMLAction": {
				actionModel.isEnabled = !(dbo instanceof SiteClipperTransaction);
			}
			break;
			
			case "com.twinsoft.convertigo.eclipse.popup.actions.CreateTestCaseAction": {
				actionModel.isEnabled = dbo instanceof ITestCaseContainer;
			}
			break;
			
			case "com.twinsoft.convertigo.eclipse.popup.actions.CreateVariableAction": {
				actionModel.isEnabled = dbo instanceof IVariableContainer;
			}
			break;
			
//			case "com.twinsoft.convertigo.eclipse.popup.actions.DeleteDesignDocumentFunctionAction": {
//			}
//			break;
//			
//			case "com.twinsoft.convertigo.eclipse.popup.actions.FunctionRenameAction": {
//			}
//			break;
//			
//			case "com.twinsoft.convertigo.eclipse.popup.actions.MobileBuilderAction": {
//			}
//			break;
//			
//			case "com.twinsoft.convertigo.eclipse.popup.actions.OutputStepAction": {
//			}
//			break;
			
			case "com.twinsoft.convertigo.eclipse.popup.actions.SetDefaultConnectorAction": {
				Connector connector = (Connector) dbo;
				actionModel.isChecked = connector.isDefault;
			}
			break;
			
			case "com.twinsoft.convertigo.eclipse.popup.actions.SetDefaultTransactionAction": {
				Transaction transaction = (Transaction) dbo;
				actionModel.isChecked  = transaction.isDefault;
			}
			break;
			
			case "com.twinsoft.convertigo.eclipse.popup.actions.SetMobileRootPageAction": {
				com.twinsoft.convertigo.beans.mobile.components.PageComponent page = GenericUtils.cast(dbo);
				actionModel.isChecked = page.isRoot;
			}
			break;
			
			case "com.twinsoft.convertigo.eclipse.popup.actions.SetNgxRootPageAction": {
				com.twinsoft.convertigo.beans.ngx.components.PageComponent page = GenericUtils.cast(dbo);
				actionModel.isChecked = page.isRoot;
			}
			break;
			
			case "com.twinsoft.convertigo.eclipse.popup.actions.TransactionCreateHandlerAction": {
			}
			break;
			
			case "com.twinsoft.convertigo.eclipse.popup.actions.TransactionEditHandlersAction": {
				actionModel.isEnabled = !(dbo instanceof SiteClipperTransaction);
			}
			break;
			
			case "com.twinsoft.convertigo.eclipse.popup.actions.UpdateReferenceAction": {
				actionModel.isEnabled = dbo instanceof WebServiceReference || (dbo instanceof ProjectSchemaReference ref && StringUtils.isNoneBlank(ref.getParser().getGitUrl()));
			}
			break;
			
			case "com.twinsoft.convertigo.eclipse.popup.actions.UpdateXSDTypesAction": {
				actionModel.isEnabled = !(dbo instanceof SiteClipperTransaction);
			}
			break;
			
//			case "com.twinsoft.convertigo.eclipse.popup.actions.ViewExecuteNoReduceAction": {
//			}
//			break;
			
			case "com.twinsoft.convertigo.eclipse.popup.actions.EnableMobilePageComponentAction": {
				com.twinsoft.convertigo.beans.mobile.components.PageComponent page = GenericUtils.cast(dbo);
				actionModel.isEnabled = !page.isRoot && !page.isEnabled();
			}
			break;
			case "com.twinsoft.convertigo.eclipse.popup.actions.EnableNgxPageComponentAction": {
				com.twinsoft.convertigo.beans.ngx.components.PageComponent page = GenericUtils.cast(dbo);
				actionModel.isEnabled = !page.isRoot && !page.isEnabled();
			}
			break;
			
			case "com.twinsoft.convertigo.eclipse.popup.actions.DisableMobilePageComponentAction": {
				com.twinsoft.convertigo.beans.mobile.components.PageComponent page = GenericUtils.cast(dbo);
				actionModel.isEnabled = !page.isRoot && page.isEnabled();
			}
			break;
			case "com.twinsoft.convertigo.eclipse.popup.actions.DisableNgxPageComponentAction": {
				com.twinsoft.convertigo.beans.ngx.components.PageComponent page = GenericUtils.cast(dbo);
				actionModel.isEnabled = !page.isRoot && page.isEnabled();
			}
			break;
		}
		
		return actionModel;
	}

}
