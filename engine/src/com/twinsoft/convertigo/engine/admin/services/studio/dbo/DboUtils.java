/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.connectors.CouchDbConnector;
import com.twinsoft.convertigo.beans.connectors.FullSyncConnector;
import com.twinsoft.convertigo.beans.connectors.SapJcoConnector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IContainerOrdered;
import com.twinsoft.convertigo.beans.core.ITokenPath;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.RequestableStep;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.TestCase;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.core.UrlAuthentication;
import com.twinsoft.convertigo.beans.references.ProjectSchemaReference;
import com.twinsoft.convertigo.beans.steps.SequenceStep;
import com.twinsoft.convertigo.beans.steps.TransactionStep;
import com.twinsoft.convertigo.beans.core.DatabaseObject.ExportOption;
import com.twinsoft.convertigo.beans.couchdb.DesignDocument;
import com.twinsoft.convertigo.beans.couchdb.JsonIndex;
import com.twinsoft.convertigo.beans.ngx.components.MobileSmartSourceType;
import com.twinsoft.convertigo.beans.ngx.components.UIActionStack;
import com.twinsoft.convertigo.beans.ngx.components.UICompVariable;
import com.twinsoft.convertigo.beans.ngx.components.UIComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIControlVariable;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicAction;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicInvoke;
import com.twinsoft.convertigo.beans.ngx.components.UISharedComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIStackVariable;
import com.twinsoft.convertigo.beans.ngx.components.dynamic.IonBean;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.beans.variables.StepVariable;
import com.twinsoft.convertigo.beans.variables.TestCaseVariable;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.ObjectWithSameNameException;
import com.twinsoft.convertigo.engine.admin.services.studio.Utils;
import com.twinsoft.convertigo.engine.helpers.WalkHelper;
import com.twinsoft.convertigo.engine.providers.couchdb.CouchDbManager;
import com.twinsoft.convertigo.engine.providers.couchdb.FullSyncClient;

public class DboUtils {

	static protected DatabaseObject findDbo(String id) throws Exception {
		return Utils.getDbo(id);
	}

	static protected boolean canCut(DatabaseObject dbo) {
		return DboFactory.isCuttable(dbo);
	}

	static protected boolean acceptDbo(DatabaseObject targetDatabaseObject, DatabaseObject databaseObject,
			boolean includeSpecials) {
		if (targetDatabaseObject.getQName().startsWith(databaseObject.getQName())) {
			return false;
		}
		if (!DboFactory.acceptDbo(targetDatabaseObject, databaseObject, includeSpecials)) {
			return false;
		}
		return true;
	}

	static protected DatabaseObject createDbo(JSONObject jsonData, DatabaseObject parentDbo) throws Exception {
		if (jsonData.has("type")) {
			var type = jsonData.getString("type");
			if (type.equals("paletteData")) {
				return createDboFromPalette(jsonData, parentDbo);
			} else if (type.equals("treeData")) {
				return createDboFromTree(jsonData, parentDbo);
			}
		}
		return null;
	}

	static private DatabaseObject createDboFromPalette(JSONObject jsonData, DatabaseObject parentDbo) throws Exception {
		DatabaseObject dbo = null;

		JSONObject jsonItem = jsonData.getJSONObject("data");

		var dboClassName = jsonItem.getString("classname");
		var dboType = jsonItem.getString("type");
		var dboId = jsonItem.getString("id");

		// case Bean
		if (dboType.equals("Dbo")) {
			dbo = (DatabaseObject) Class.forName(dboClassName).getConstructor().newInstance();
		}
		// case ionBean
		else if (dboType.equals("Ion")) {
			var kind = dboId.split(" ")[0];
			if (kind.equals("ngx")) {
				var ionBeanName = dboId.split(" ")[1];
				com.twinsoft.convertigo.beans.ngx.components.dynamic.Component component = null;
				component = com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.of(parentDbo)
						.getComponentByName(ionBeanName);
				if (component != null) {
					dbo = com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.of(parentDbo)
							.createBeanFromHint(component);
				}
			}
		}

		return dbo;
	}

	static private DatabaseObject createDboFromTree(JSONObject jsonData, DatabaseObject parentDbo) throws Exception {
		JSONObject jsonItem = jsonData.getJSONObject("data");
		var dboId = jsonItem.getString("id");
		DatabaseObject dbo = findDbo(dboId);
		if (dbo != null) {
			return DboFactory.createDbo(parentDbo, dbo);
		}
		return null;
	}

	static protected Object read(Node node) throws Exception {
		Class<?> objectClass = null;
		Object object = null;
		Element element = (Element) node;
		String objectClassName = element.getAttribute("classname");
		try {
			objectClass = Class.forName(objectClassName);
			Method readMethod = objectClass.getMethod("read", new Class[] { Node.class });
			object = readMethod.invoke(null, new Object[] { node });
		} catch (Exception e) {
			throw new EngineException("Unable to read object", e);
		}
		return object;
	}

	static protected void xmlCut(Document document, String id) throws Exception {
		Element element = document.createElement("dbo");
		element.setAttribute("id", id);
		document.getDocumentElement().appendChild(element);
	}

	static protected void xmlCopy(Document document, DatabaseObject dbo) throws Exception {
		final Element rootElement = document.getDocumentElement();

		new WalkHelper() {
			protected Element parentElement = rootElement;

			@Override
			protected void walk(DatabaseObject databaseObject) throws Exception {
				Element parentElement = this.parentElement;

				Element element = databaseObject.toXml(document, ExportOption.bIncludeVersion);
				parentElement.appendChild(element);

				this.parentElement = element;
				super.walk(databaseObject);
				this.parentElement = parentElement;
			}
		}.init(dbo);
	}

	static protected Object xmlPaste(Node node, DatabaseObject parentDbo) throws Exception {
		Object object = read(node);
		if (object instanceof DatabaseObject) {
			DatabaseObject databaseObject = (DatabaseObject) object;
			String dboName = databaseObject.getName();

			// Special case of project
			if (databaseObject instanceof Project) {
				return databaseObject;
			}

			if (!DboFactory.acceptDboAsSuch(parentDbo, databaseObject)) {
				throw new EngineException("You cannot paste to a " + parentDbo.getClass().getSimpleName()
						+ " a database object of type " + databaseObject.getClass().getSimpleName());
			}

			boolean bContinue = true;
			boolean bIncName = false;
			// long oldPriority = databaseObject.priority;

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
									throw new ObjectWithSameNameException(
											"Unable to paste the object because an object with the same name already exists in target.");
								}
							}
						}

					}.init(parentDbo);
					bContinue = false;
				} catch (ObjectWithSameNameException owsne) {
					bIncName = true;
				} catch (EngineException ee) {
					throw ee;
				} catch (Exception e) {
					throw new EngineException("Exception in paste", e);
				}
			}

			if (parentDbo instanceof IContainerOrdered) {
				databaseObject.priority = databaseObject.getNewOrderValue();
			}
			parentDbo.add(databaseObject);

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
					xmlPaste(childNode, databaseObject);
				}
			}

			databaseObject.isImporting = false; // needed
			databaseObject.isSubLoaded = true;
			return databaseObject;
		}
		return null;
	}

	static protected boolean changeBeanName(JSONArray ids, DatabaseObject dbo, Object oldValue, Object newValue,
			String update) {
		if (dbo == null || newValue == null || newValue.toString().isBlank()) {
			return false;
		}

		RefactorMap map = new DboUtils().new RefactorMap();
		try {
			// first rename dbo
			try {
				String oldQName = dbo.getFullQName();
				dbo.setName((String) newValue);
				dbo.hasChanged = true;
				map.addEvent(oldQName, "name", dbo, oldValue, newValue);
			} catch (Exception e) {
				Engine.logEngine.error("Failed to rename " + dbo.getClass().getName() + " " + dbo.getQName(), e);
				return false;
			}

			// if nothing else to do return
			if (update.equals("UPDATE_NONE") || update.isBlank()) {
				return true;
			}

			// then propagate to other beans
			List<String> projectNames = null;
			if (update.equals("UPDATE_LOCAL")) {
				projectNames = new ArrayList<String>();
				projectNames.add(dbo.getProject().getName());
			} else if (update.equals("UPDATE_ALL")) {
				projectNames = Engine.theApp.databaseObjectsManager.getAllProjectNamesList(true);
			}

			if (projectNames != null) {
				Map.Entry<String, DboUtils.DboChangeEvent> entry = null;
				while ((entry = map.getNextEntry()) != null) {
					DboChangeEvent event = entry.getValue();
					if (event.propertyName.isBlank()) continue;
					WalkHelper walker = getEventHelper(map, event, update);
					for (String projectName : projectNames) {
						Project project;
						try {
							project = (Project) Engine.theApp.databaseObjectsManager
									.getDatabaseObjectByQName(projectName);
							walker.init(project);
						} catch (Exception e) {
							Engine.logEngine.error(
									"Failed to propagate rename of " + dbo.getClass().getName() + " " + dbo.getQName(),
									e);
						}
					}
				}

				map.fillWithKeys(ids);
			}

			return true;
		} finally {
			if (map != null) {
				map.clear();
				map = null;
			}
		}
	}

	private class RefactorMap {
		Map<String, DboChangeEvent> map = new LinkedHashMap<String, DboUtils.DboChangeEvent>(10);
		Set<String> set = new HashSet<String>();

		private synchronized void addEvent(String key, String propertyName, DatabaseObject dbo, Object oldValue,
				Object newValue) {
			DboChangeEvent event = new DboChangeEvent(propertyName, dbo, oldValue, newValue);
			set.add(key);
			map.put(key, event);
			System.out.println("map size=" + map.size() + " - added event " + event.toString());
		}

		private synchronized Entry<String, DboChangeEvent> getNextEntry() {
			try {
				Entry<String, DboChangeEvent> entry = map.entrySet().iterator().next();
				DboChangeEvent event = map.remove(entry.getKey());
				System.out.println("map size=" + map.size() + " - peek event " + event.toString());
				if (event != null) {
					return entry;
				}
			} catch (Exception e) {
			}
			return null;
		}

		private synchronized void fillWithKeys(JSONArray ids) {
			for (String key : set) {
				ids.put(key);
			}
		}

		private synchronized void clear() {
			map.clear();
			set.clear();
		}
	}

	static private WalkHelper getEventHelper(RefactorMap map, DboChangeEvent event, String update) {
		DatabaseObject dbo = event.dbo;
		String propertyName = event.propertyName;
		Object oldValue = event.oldValue;
		Object newValue = event.newValue;

		return new WalkHelper() {
			private void setDboName(RefactorMap map, DatabaseObject databaseObject) {
				try {
					String oldQName = databaseObject.getFullQName();
					databaseObject.setName((String) newValue);
					databaseObject.hasChanged = true;
					map.addEvent(oldQName, "name", databaseObject, oldValue, newValue);
				} catch (EngineException e) {
					Engine.logEngine.warn(
							"Failed to rename " + databaseObject.getClass().getName() + " " + databaseObject.getQName(),
							e);
				}
			}

			private void refactor(DatabaseObject databaseObject) {
				String oldToken = null, newToken = null;
				if (("name".equals(propertyName) && dbo instanceof ITokenPath) || "qname".equals(propertyName)) {
					oldToken = dbo.getTokenPath((String)oldValue);
					newToken = dbo.getTokenPath((String)newValue);
				}
				
				if ("name".equals(propertyName)) {
					// A project changed its name
					if (dbo instanceof Project) {
						// refactor reference's project
						if (databaseObject instanceof ProjectSchemaReference) {
							ProjectSchemaReference reference = (ProjectSchemaReference) databaseObject;
							if (reference.getParser().getProjectName().equals(oldValue)) {
								String oldQName = reference.getFullQName();
								reference.setProjectName((String) newValue);
								reference.hasChanged = true;
								map.addEvent(oldQName, "projectName", reference, oldValue, newValue);
							}
						}
					}
					// A transaction changed its name
					else if (dbo instanceof Transaction) {
						// refactor connector's end transaction
						if (databaseObject instanceof Connector) {
							Connector connector = (Connector)databaseObject;
							if (((Transaction)dbo).getConnector().equals(connector)) {
								if (connector.getEndTransactionName().equals(oldValue)) {
									String oldQName = connector.getFullQName();
									connector.setEndTransactionName((String)newValue);
									connector.hasChanged = true;
									map.addEvent(oldQName, "endTransactionName", connector, oldValue, newValue);
								}
							}
						}
					}
					// A sequence changed its name
					else if (dbo instanceof Sequence) {
						;
					}
	
					// Bean names equality
					else if (databaseObject.getName().equals(oldValue)) {
						
						// A RequestableVariable changed its name
						if (dbo instanceof RequestableVariable) {
							RequestableVariable requestableVariable = (RequestableVariable) dbo;
							String rqname = requestableVariable.getParent().getQName();
	
							// refactor TestCaseVariable name
							if (databaseObject instanceof TestCaseVariable) {
								TestCaseVariable testCaseVariable = (TestCaseVariable) databaseObject;
								TestCase testCase = (TestCase) testCaseVariable.getParent();
								String tqname = testCase.getParent().getQName();
								if (rqname.equals(tqname)) {
									setDboName(map, databaseObject);
								}
							}
							// refactor StepVariable name
							else if (databaseObject instanceof StepVariable) {
								StepVariable stepVariable = (StepVariable) databaseObject;
								RequestableStep requestableStep = (RequestableStep) stepVariable.getParent();
								boolean isTransactionStep = requestableStep instanceof TransactionStep;
								String sqname = isTransactionStep
										? ((TransactionStep) requestableStep).getSourceTransaction()
										: ((SequenceStep) requestableStep).getSourceSequence();
								if (rqname.equals(sqname)) {
									setDboName(map, databaseObject);
								}
							}
							// refactor UIControlVariable name
							else if (databaseObject instanceof UIControlVariable) {
								UIControlVariable uiControlVariable = (UIControlVariable)databaseObject;
								DatabaseObject parent = uiControlVariable.getParent();
								if (parent instanceof UIDynamicAction) {
									IonBean ionBean = ((UIDynamicAction)parent).getIonBean();
									if (ionBean != null && ionBean.getName().equals("CallSequenceAction")) {
										String qname = (String) ionBean.getProperty("requestable").getValue();
										if (qname != null && rqname.equals(qname)) {
											setDboName(map, databaseObject);
										}
									}
								}
							}
						}
						// A UIStackVariable changed its name
						else if (dbo instanceof UIStackVariable) {
							UIStackVariable uiStackVariable = (UIStackVariable) dbo;
							String pqname = uiStackVariable.getParent().getQName();
							
							// refactor UIControlVariable name
							if (databaseObject instanceof UIControlVariable) {
								UIControlVariable uiControlVariable = (UIControlVariable)databaseObject;
								DatabaseObject parent = uiControlVariable.getParent();
								if (parent instanceof UIDynamicInvoke) {
									String qname = ((UIDynamicInvoke) parent).getSharedActionQName();
									if (qname != null && pqname.equals(qname)) {
										setDboName(map, databaseObject);
									}
								}
							}
							
						}
					}
					
					// Bean property equals token (qname)
					if (oldToken != null && newToken != null) {
						if (databaseObject instanceof TransactionStep) {
							TransactionStep transactionStep = (TransactionStep)databaseObject;
							if (transactionStep.getSourceTransaction().equals(oldToken)) {
								String oldQName = transactionStep.getFullQName();
								transactionStep.setSourceTransaction(newToken);
								transactionStep.hasChanged = true;
								map.addEvent(oldQName, "sourceTransaction", transactionStep, oldToken, newToken);
							}
						}
						else if (databaseObject instanceof SequenceStep) {
							SequenceStep sequenceStep = (SequenceStep)databaseObject;
							if (sequenceStep.getSourceSequence().equals(oldToken)) {
								String oldQName = sequenceStep.getFullQName();
								sequenceStep.setSourceSequence(newToken);
								sequenceStep.hasChanged = true;
								map.addEvent(oldQName, "sourceSequence", sequenceStep, oldToken, newToken);
							}
						}
						else if (databaseObject instanceof UrlAuthentication) {
							UrlAuthentication urlAuthentication = (UrlAuthentication)databaseObject;
							if (urlAuthentication.getAuthRequestable().equals(oldToken)) {
								String oldQName = urlAuthentication.getFullQName();
								urlAuthentication.setAuthRequestable(newToken);
								urlAuthentication.hasChanged = true;
								map.addEvent(oldQName, "authRequestable", urlAuthentication, oldToken, newToken);
							}
						}
						else if (databaseObject instanceof UIDynamicInvoke) {
							UIDynamicInvoke uiDynamicInvoke = (UIDynamicInvoke)databaseObject;
							if (uiDynamicInvoke.getSharedActionQName().equals(oldToken)) {
								String oldQName = uiDynamicInvoke.getFullQName();
								uiDynamicInvoke.setSharedActionQName(newToken);
								uiDynamicInvoke.hasChanged = true;
								map.addEvent(oldQName, "stack", uiDynamicInvoke, oldToken, newToken);
							}
						} else if (databaseObject instanceof UIDynamicAction) {
							UIDynamicAction uiDynamicAction = (UIDynamicAction)databaseObject;
							IonBean ionBean = uiDynamicAction.getIonBean();
							if (ionBean != null && ionBean.getName().equals("CallSequenceAction")) {
								if (ionBean.getProperty("requestable").getValue().equals(oldToken)) {
									String oldQName = uiDynamicAction.getFullQName();
									ionBean.setPropertyValue("requestable", new MobileSmartSourceType(newToken));
									uiDynamicAction.hasChanged = true;
									map.addEvent(oldQName, "requestable", uiDynamicAction, oldToken, newToken);
								}
							}
						}
					}
				}
			}

			private static boolean hasSameScriptComponent(UIComponent uic1, UIComponent uic2) {
				if (uic1 != null && uic2 != null) {
					try {
						return uic1.getMainScriptComponent().equals(uic2.getMainScriptComponent());
					} catch (Exception e) {}
				}
				return false;
			}
			
			private void refactorSmartSources(UIComponent databaseObject, Object oldValue, Object newValue) {
				try {
					boolean sourcesUpdated = false;

					// A bean name has changed
					if (propertyName.equals("name")) {
						try {
							if (dbo instanceof Project) {
								String oldName = (String)oldValue;
								String newName = (String)newValue;
								if (!newValue.equals(oldValue)) {
									if (databaseObject.updateSmartSource("'"+oldName+"\\.", "'"+newName+".")) {
										sourcesUpdated = true;
									}
									if (databaseObject.updateSmartSource("\\/"+oldName+"\\.", "/"+newName+".")) {
										sourcesUpdated = true;
									}
								}
							}
							else if (dbo instanceof Sequence) {
								String oldName = (String)oldValue;
								String newName = (String)newValue;
								String projectName = dbo.getProject().getName();
								if (!newValue.equals(oldValue)) {
									if (databaseObject.updateSmartSource("'"+projectName+"\\."+oldName, "'"+projectName+"."+newName)) {
										sourcesUpdated = true;
									}
								}
							}
							else if (dbo instanceof FullSyncConnector) {
								String oldName = (String)oldValue;
								String newName = (String)newValue;
								String projectName = dbo.getProject().getName();
								if (!newValue.equals(oldValue)) {
									if (databaseObject.updateSmartSource("\\/"+projectName+"\\."+oldName+"\\.", "/"+projectName+"."+newName+".")) {
										sourcesUpdated = true;
									}
									if (databaseObject.updateSmartSource("\\/"+oldName+"\\.", "/"+newName+".")) {
										sourcesUpdated = true;
									}
								}
							}
							else if (dbo instanceof DesignDocument) {
								String oldName = (String)oldValue;
								String newName = (String)newValue;
								if (!newValue.equals(oldValue)) {
									if (databaseObject.updateSmartSource("ddoc='"+oldName+"'", "ddoc='"+newName+"'")) {
										sourcesUpdated = true;
									}
								}
							} else if (dbo instanceof UIStackVariable || dbo instanceof UICompVariable) {
								if (!newValue.equals(oldValue)) {
									UIComponent obj = databaseObject;
									DatabaseObject d = obj;
									while (d != null) {
										if (dbo instanceof UIStackVariable) {
											if (d instanceof UIActionStack) {
												break;
											} else if (d instanceof UIDynamicInvoke) {
												String pqname = dbo.getParent().getQName();
												String qname = ((UIDynamicInvoke) d).getSharedActionQName();
												if (pqname.equals(qname)) {
													break;
												}
											}
										} else if (dbo instanceof UICompVariable) {
											if (d instanceof UISharedComponent) {
												break;
											}
										}
										d = d.getParent();
									}
									if (d != null) {
										String oldName = (String)oldValue;
										String newName = (String)newValue;
										try {
											if (obj.updateSmartSource("((?:\"|vars)\\??\\.)"+oldName+"\\b", "$1"+newName)) {
												sourcesUpdated = true;
											}
										} catch (Exception e) {}
									}
								}
							} else if (dbo instanceof UIControlVariable) {
								if (!newValue.equals(oldValue)) {
									UIComponent obj = databaseObject;
									DatabaseObject p = dbo.getParent();
									if (obj.getQName().startsWith(p.getQName())) {
										boolean doIt = false; //true; //TODO: obj.checkSmartSource
										if (doIt) {
											String oldName = (String)oldValue;
											String newName = (String)newValue;
											try {
												if (obj.updateSmartSource("((?:\"|vars)\\??\\.)"+oldName+"\\b", "$1"+newName)) {
													sourcesUpdated = true;
												}
											} catch (Exception e) {}
										}
									}
								}
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					if (dbo instanceof UIComponent) {
						UIComponent uic = (UIComponent)dbo;
						if (hasSameScriptComponent(databaseObject, uic)) {
							// A ControlName property has changed
							if (propertyName.equals("ControlName") || uic.isFormControlAttribute()) {
								if (!newValue.equals(oldValue)) {
									try {
										String oldSmart = ((MobileSmartSourceType)oldValue).getSmartValue();
										String newSmart = ((MobileSmartSourceType)newValue).getSmartValue();
										if (uic.getUIForm() != null) {
											if (databaseObject.updateSmartSource("\\?\\.controls\\['"+oldSmart+"'\\]", "?.controls['"+newSmart+"']")) {
												sourcesUpdated = true;
											}
										}
									} catch (Exception e) {}
								}
							}
							else if (propertyName.equals("identifier")) {
								if (!newValue.equals(oldValue)) {
									try {
										String oldId = (String)oldValue;
										String newId = (String)newValue;
										if (uic.getUIForm() != null) {
											if (databaseObject.updateSmartSource("\"identifier\":\""+oldId+"\"", "\"identifier\":\""+newId+"\"")) {
												sourcesUpdated = true;
											}
										}
									} catch (Exception e) {}
								}
							}
						}
					}
					
					if (sourcesUpdated) {
						String oldQName = databaseObject.getQName();
						map.addEvent(oldQName, "", databaseObject, oldValue, newValue);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			@Override
			protected void walk(DatabaseObject databaseObject) throws Exception {
				boolean isSameProject = dbo.getProject().equals(databaseObject.getProject());
				boolean doUpdate = update.equals("UPDATE_ALL") || (update.equals("UPDATE_LOCAL") && isSameProject);
				if (doUpdate) {

					// This bean changed : do some utility works
					if (databaseObject.equals(dbo)) {
						// This project changed
						if (databaseObject instanceof Project) {
							Project project = (Project) databaseObject;
							if (propertyName.equals("schemaElementForm") || propertyName.equals("namespaceUri")) {
								for (Connector connector : project.getConnectorsList()) {
									for (Transaction transaction : connector.getTransactionsList()) {
										synchronized (transaction) {
											transaction.updateSchemaToFile();
										}
									}
								}
							}
							Engine.theApp.schemaManager.clearCache(project.getName());
						}
						// This SapJcoConnector changed
						else if (databaseObject instanceof SapJcoConnector) {
							try {
								((SapJcoConnector) databaseObject).getSapJCoProvider().updateDestination();
							} catch (Exception e) {
								Engine.logEngine.error("Could not update SAP destination !", e);
							}
						}
						// This CouchDbConnector changed
						else if (databaseObject instanceof CouchDbConnector) {
							CouchDbConnector connector = (CouchDbConnector) databaseObject;
							if (propertyName.equals("name")) {
								if (databaseObject instanceof FullSyncConnector) {
									try {
										FullSyncClient fsclient = Engine.theApp.couchDbManager.getFullSyncClient();
										JSONObject res = fsclient.getDatabase((String) oldValue);
										if (res.getInt("doc_count") <= 1) {
											fsclient.deleteDatabase((String) oldValue);
										}
									} catch (Exception e) {}
									CouchDbManager.syncDocument(connector);
								}
							}
							else if (propertyName.equals("https") || propertyName.equals("port")
									|| propertyName.equals("server") || propertyName.equals("couchUsername")
									|| propertyName.equals("couchPassword")) {
								connector.release();
								CouchDbManager.syncDocument(connector);
							} else if (propertyName.equals("databaseName")) {
								CouchDbManager.syncDocument(connector);
							}
						}
						// This JsonIndex changed
						else if (databaseObject instanceof JsonIndex) {
							if (propertyName.equals("name") || propertyName.equals("fields") || propertyName.equals("ascending")) {
								Connector connector = (Connector) databaseObject.getParent();
								CouchDbManager.syncDocument(connector);
							}
						}
						

					}

					// Propagate to other beans
					else {
						
						// makes refactoring (will create new events)
						refactor(databaseObject);
						if (databaseObject instanceof UIComponent) {
							refactorSmartSources((UIComponent)databaseObject, oldValue, newValue);
						}
						
						if (databaseObject instanceof Project) {
							if (databaseObject.equals(dbo.getProject())) {
								Engine.theApp.schemaManager.clearCache(databaseObject.getName());
							}
						}
						else if (databaseObject instanceof ProjectSchemaReference) {
							ProjectSchemaReference reference = (ProjectSchemaReference) databaseObject;
							if (dbo.getProject().getName().equals(reference.getParser().getProjectName())) {
								Engine.theApp.schemaManager.clearCache(reference.getProject().getName());
							}
						}
					}
					
					super.walk(databaseObject);
				}
			}
		};
	}

	private class DboChangeEvent {
		private DatabaseObject dbo;
		private String propertyName;
		private Object oldValue;
		private Object newValue;
		private JSONObject jsonOb = new JSONObject();

		private DboChangeEvent(String propertyName, DatabaseObject dbo, Object oldValue, Object newValue) {
			this.propertyName = propertyName;
			this.dbo = dbo;
			this.oldValue = oldValue;
			this.newValue = newValue;

			try {
				jsonOb.put("priority", dbo.priority);
				jsonOb.put("databaseObject", dbo.getName());
				jsonOb.put("propertyName", propertyName);
				jsonOb.put("oldValue", oldValue.toString());
				jsonOb.put("newValue", newValue.toString());
			} catch (Exception e) {
			}
		}

		@Override
		public String toString() {
			return jsonOb.toString();
		}

	}

}
