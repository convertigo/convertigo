/*
 * Copyright (c) 2001-2020 Convertigo SA.
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

package com.twinsoft.convertigo.engine.admin.services.studio.palette;

import java.beans.BeanInfo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.connectors.CicsConnector;
import com.twinsoft.convertigo.beans.connectors.HtmlConnector;
import com.twinsoft.convertigo.beans.connectors.HttpConnector;
import com.twinsoft.convertigo.beans.connectors.JavelinConnector;
import com.twinsoft.convertigo.beans.connectors.SiteClipperConnector;
import com.twinsoft.convertigo.beans.connectors.SqlConnector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Criteria;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.DatabaseObject.DboCategoryInfo;
import com.twinsoft.convertigo.beans.core.ExtractionRule;
import com.twinsoft.convertigo.beans.core.Listener;
import com.twinsoft.convertigo.beans.core.MobilePlatform;
import com.twinsoft.convertigo.beans.core.Pool;
import com.twinsoft.convertigo.beans.core.Reference;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.core.Sheet;
import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.TestCase;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.core.UrlMapping;
import com.twinsoft.convertigo.beans.core.UrlMappingOperation;
import com.twinsoft.convertigo.beans.core.UrlMappingParameter;
import com.twinsoft.convertigo.beans.core.UrlMappingResponse;
import com.twinsoft.convertigo.beans.core.Variable;
import com.twinsoft.convertigo.beans.dbo_explorer.DboBeanData;
import com.twinsoft.convertigo.beans.dbo_explorer.DboBeansData;
import com.twinsoft.convertigo.beans.dbo_explorer.DboCategoryData;
import com.twinsoft.convertigo.beans.mobile.components.PageComponent;
import com.twinsoft.convertigo.beans.mobile.components.RouteActionComponent;
import com.twinsoft.convertigo.beans.mobile.components.RouteComponent;
import com.twinsoft.convertigo.beans.mobile.components.RouteEventComponent;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.dbo_explorer.DboBean;
import com.twinsoft.convertigo.engine.dbo_explorer.DboBeans;
import com.twinsoft.convertigo.engine.dbo_explorer.DboCategory;
import com.twinsoft.convertigo.engine.dbo_explorer.DboExplorerManager;
import com.twinsoft.convertigo.engine.dbo_explorer.DboGroup;
import com.twinsoft.convertigo.engine.dbo_explorer.DboUtils;
import com.twinsoft.convertigo.engine.util.GenericUtils;

@ServiceDefinition(
		name = "Get",
		roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG },
		parameters = {},
		returnValue = ""
	)
public class Get extends XmlService {

	// Get the bean class of a folder from its name
	public static Map<String, Class<? extends DatabaseObject>> folderNameToBeanClass = new HashMap<>(24);
	static {
		folderNameToBeanClass.put("Listener", Listener.class);
		folderNameToBeanClass.put("Transaction", Transaction.class);
		//folderNameToBeanClass.put("Handler", Handler);
	    folderNameToBeanClass.put("ScreenClass", ScreenClass.class);
		folderNameToBeanClass.put("InheritedScreenClass", ScreenClass.class);
		folderNameToBeanClass.put("Sheet", Sheet.class);
		folderNameToBeanClass.put("Pool", Pool.class);
		folderNameToBeanClass.put("ExtractionRule", ExtractionRule.class);
		folderNameToBeanClass.put("Criteria", Criteria.class);
		folderNameToBeanClass.put("Connector", Connector.class);
		folderNameToBeanClass.put("Sequence", com.twinsoft.convertigo.beans.core.Sequence.class);
		folderNameToBeanClass.put("Step", Step.class);
		folderNameToBeanClass.put("TestCase", TestCase.class);
		folderNameToBeanClass.put("Variable", Variable.class);
		folderNameToBeanClass.put("Reference", Reference.class);
		folderNameToBeanClass.put("Document", com.twinsoft.convertigo.beans.core.Document.class);
		folderNameToBeanClass.put("UrlMapping", UrlMapping.class);
		folderNameToBeanClass.put("UrlMappingOperation", UrlMappingOperation.class);
		folderNameToBeanClass.put("UrlMappingParameter", UrlMappingParameter.class);
		folderNameToBeanClass.put("UrlMappingResponse", UrlMappingResponse.class);
		folderNameToBeanClass.put("MobilePlatform", MobilePlatform.class);
		folderNameToBeanClass.put("Action", RouteActionComponent.class);
		folderNameToBeanClass.put("Event", RouteEventComponent.class);
		folderNameToBeanClass.put("Route", RouteComponent.class);
		folderNameToBeanClass.put("Page", PageComponent.class);
	}

	@Override
	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		String qname = request.getParameter("qname");
		String folderType = request.getParameter("folderType");

		DatabaseObject dbo = Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(qname);
		Class<? extends DatabaseObject> databaseObjectClass = folderNameToBeanClass.get(folderType);
		createCategories(document, dbo, databaseObjectClass, document.getDocumentElement());
	}

	private void createCategories(Document document, DatabaseObject dbo, Class<? extends DatabaseObject> databaseObjectClass, Element root) throws Exception {
		Element response = document.createElement("response");

		try {
			List<String> defaultDboList = new ArrayList<>();
			Class<? extends DatabaseObject> parentObjectClass = dbo.getClass();

			Map<String, DboCategoryData> categoryNameToDboCategory = new HashMap<>();
			DboExplorerManager manager = Engine.theApp.getDboExplorerManager();
			for (DboGroup group : manager.getGroups()) {
				for (DboCategory category : group.getCategories()) {
					for (DboBeans beansCategory : category.getBeans()) {
						for (DboBean bean : beansCategory.getBeans()) {
							// Skip if bean is disabled
							if (!bean.isEnable()) {
								continue;
							}

							String className = bean.getClassName();

							try {
								Class<DatabaseObject> beanClass = GenericUtils.cast(Class.forName(className));
								DboCategoryInfo dboCategoryInfo = DatabaseObject.getDboGroupInfo(beanClass);

                                if (dboCategoryInfo == null) {
                                    continue;
                                }

						        // If one of these cases, do not add the category
						        if (dbo instanceof ScreenClass) {
						            ScreenClass sc = (ScreenClass) dbo;
						            // Do not show Criteria category if it is the default Screen Class
						            if (sc.getDepth() == 0 && dboCategoryInfo.equals(DatabaseObject.getDboGroupInfo(Criteria.class))) {
                                        continue;
						            }
						        }
						        else if (dbo instanceof CicsConnector) {
						            // Do not show Pool category
						            if (dboCategoryInfo.equals(DatabaseObject.getDboGroupInfo(Pool.class))) {
                                        continue;
						            }
						        }
						        else if (dbo instanceof JavelinConnector) {
						            // Do not show ScreenClass category
						            if (dboCategoryInfo.equals(DatabaseObject.getDboGroupInfo(ScreenClass.class))) {
                                        continue;
						            }
						        }
						        else if (dbo instanceof SqlConnector) {
						            // Do not show Pool category
						            if (dboCategoryInfo.equals(DatabaseObject.getDboGroupInfo(Pool.class))) {
                                        continue;
						            }
						        }
						        else if (dbo instanceof HtmlConnector) {
						            // Do not show Pool and ScreenClass categories
						            if (dboCategoryInfo.equals(DatabaseObject.getDboGroupInfo(Pool.class)) ||
						                dboCategoryInfo.equals(DatabaseObject.getDboGroupInfo(ScreenClass.class))) {
                                        continue;
						            }
						        }
						        else if (dbo instanceof HttpConnector) {
						            // Do not show Pool category
						            if (dboCategoryInfo.equals(DatabaseObject.getDboGroupInfo(Pool.class))) {
                                        continue;
						            }
						        }
						        else if (dbo instanceof SiteClipperConnector) {
						            // Do not show Pool and ScreenClass categories
						            if (dboCategoryInfo.equals(DatabaseObject.getDboGroupInfo(Pool.class)) ||
						                dboCategoryInfo.equals(DatabaseObject.getDboGroupInfo(ScreenClass.class))) {
						                continue;
						            }
						        }
						        else if (dbo instanceof Transaction) {
						            // Do not show Statement category
						            if (dboCategoryInfo.equals(DatabaseObject.getDboGroupInfo(Statement.class))) {
						                continue;
						            }
						        }
								
								if (bean.isDefault()) {
									defaultDboList.add(className);
								}

								// The bean should derived from
								// DatabaseObject...
								boolean isDatabaseObject = (DatabaseObject.class.isAssignableFrom(beanClass));
								if (isDatabaseObject) {
									// ... and should derived from the specified class
									boolean isFromSpecifiedClass = (databaseObjectClass == null ||
										    (databaseObjectClass != null && databaseObjectClass.isAssignableFrom(beanClass)));
									
									if (isFromSpecifiedClass) {
									    // Check parent
										boolean bFound = DatabaseObjectsManager.checkParent(parentObjectClass, bean);
										if (bFound) {
											String technology = DboUtils.getTechnology(dbo, beanClass);
	
											// Check technology if needed
											if (technology != null) {
												Collection<String> acceptedTechnologies = bean.getEmulatorTechnologies();
	
												if (!acceptedTechnologies.isEmpty()
														&& !acceptedTechnologies.contains(technology)) {
													continue;
												}
											}

											String beanInfoClassName = className + "BeanInfo";
											Class<BeanInfo> beanInfoClass = GenericUtils.cast(Class.forName(beanInfoClassName));
											if (beanInfoClass != null) {
												String categoryName = dboCategoryInfo.getCategoryName();

												// Create category
												DboCategoryData dboCategoryData = categoryNameToDboCategory.get(categoryName);
												if (dboCategoryData == null) {
													dboCategoryData = new DboCategoryData(
															dboCategoryInfo.getCategoryId(),
															categoryName,
															dboCategoryInfo.getIconClassCSS()
													);
													categoryNameToDboCategory.put(categoryName, dboCategoryData);
												}

												// Beans name
												String beansName = beansCategory.getName();
												if (beansName.length() == 0) {
													beansName = categoryName;
												}

												// Create beans
												DboBeansData dboBeansData = dboCategoryData.getDboBeans(beansName);
												if (dboBeansData == null) {
													dboBeansData = new DboBeansData(beansName);
													dboCategoryData.addDboBeans(beansName, dboBeansData);
												}

												// Create bean
												DboBeanData dboBeanData = new DboBeanData(beanInfoClass.newInstance());
												dboBeansData.addDboBean(dboBeanData);
											}
											else {
												String message = java.text.MessageFormat.format(
														"The \"{0}\" does not exist.", new Object[] { beanInfoClassName });
			                                    throw new Exception(message);
											}
										}
									}
								}
								else {
									String message = java.text.MessageFormat.format(
											"The \"{0}\" class is not a Convertigo database object.",
											new Object[] { className });
									throw new Exception(message);
								}
							}
							catch (ClassNotFoundException e) {
								String message = java.text.MessageFormat.format(
										"Unable to analyze the \"{0}\" class.\n\nClass not found: {1}",
										new Object[] { className, e.getMessage() });
                                throw new Exception(message);
							}
							catch (Throwable e) {
								String message = java.text.MessageFormat.format(
										"Unable to analyze the \"{0}\" Convertigo database object.",
										new Object[] { className });
                                throw new Exception(message);
							}
						}
					}
				}
			}

			// Find the default selected bean for each categories
			for (DboCategoryData dboCategory : categoryNameToDboCategory.values()) {
				boolean defaultDboFound = false;
				List<DboBeanData> dboBeansList = dboCategory.getAllDboBean(true);

				// By default, we chose the first bean as default selected bean
				DboBeanData defaultSelectedBean = dboBeansList.get(0);

				// Find the default selected bean
				for (int i = 0; i < dboBeansList.size() && !defaultDboFound; ++i) {
					Class<DatabaseObject> beanClass = dboBeansList.get(i).getBeanClass();

					// Another bean is set as default selected bean
					if (defaultDboFound = defaultDboList.contains(beanClass.getName())) {
						defaultSelectedBean = dboBeansList.get(i);
					}
				}

				defaultSelectedBean.setSelectedByDefault(true);
			}

			// XmlLize
			for (DboCategoryData dboCategory: categoryNameToDboCategory.values()) {
				response.appendChild(dboCategory.toXml(document));
			}
		}
		catch (Exception e) {
            throw new Exception("Unable to load database objects properties.");
		}

		root.appendChild(response);
	}
}
