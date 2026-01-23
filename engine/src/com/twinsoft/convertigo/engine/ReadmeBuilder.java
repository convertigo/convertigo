/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

package com.twinsoft.convertigo.engine;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IApplicationComponent;
import com.twinsoft.convertigo.beans.core.MobileApplication;
import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Reference;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.core.TransactionWithVariables;
import com.twinsoft.convertigo.beans.core.UrlAuthentication;
import com.twinsoft.convertigo.beans.core.UrlMapper;
import com.twinsoft.convertigo.beans.core.UrlMapping;
import com.twinsoft.convertigo.beans.core.UrlMappingOperation;
import com.twinsoft.convertigo.beans.core.UrlMappingParameter;
import com.twinsoft.convertigo.beans.core.UrlMappingResponse;
import com.twinsoft.convertigo.beans.references.ProjectSchemaReference;
import com.twinsoft.convertigo.beans.references.RemoteFileReference;
import com.twinsoft.convertigo.beans.rest.OperationResponse;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.engine.helpers.WalkHelper;
import com.twinsoft.convertigo.engine.util.FileUtils;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.ProjectUrlParser;

import freemarker.ext.beans.SimpleMapModel;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

public class ReadmeBuilder {
	
	private class FtlDboWrapper extends DefaultObjectWrapper {

	    public FtlDboWrapper(freemarker.template.Version incompatibleImprovements) {
	        super(incompatibleImprovements);
	    }

	    @Override
		public TemplateModel wrap(Object obj) throws TemplateModelException {
			return super.wrap(obj);
		}

		@Override
	    protected TemplateModel handleUnknownType(final Object obj) throws TemplateModelException {
	        if (obj instanceof DatabaseObject) {
	        	return new SimpleMapModel(getFtlDataModel((DatabaseObject)obj), this);
	        }
	        return super.handleUnknownType(obj);
	    }
	}
	
	private class FtlWalker extends WalkHelper {
		private Map<String, Object> dboMap = new HashMap<String, Object>();
		
		private Map<String, Object> getDboMap() {
			return dboMap;
		}
		
		private void put(String key, Object value) {
			if (value instanceof List) {
				dboMap.put(key, sort(filter(GenericUtils.cast(value), MarkdownType.Readme)));
			} else {
				dboMap.put(key, value);
			}
		}
		
		@Override
		protected void walk(DatabaseObject databaseObject) throws Exception {
			if (databaseObject instanceof Project) {
				Project project = (Project) databaseObject;
				
				if (before(databaseObject, Reference.class)) {
					List<Reference> filteredList = project.getReferenceList()
							  						.stream()
							  						.filter(ref -> !isMobileBuilderTemplate(ref))
							  						.collect(Collectors.toList());
					put("references", filteredList);
				}

				if (before(databaseObject, Connector.class)) {
					put("connectors", project.getConnectorsList());
				}

				if (before(databaseObject, Sequence.class)) {
					put("sequences", project.getSequencesList());
				}
				
				if (before(databaseObject, MobileApplication.class)) {
					MobileApplication mobileApplication = project.getMobileApplication();
					if (mobileApplication != null) {
						put("mobileapp", mobileApplication);
					}
				}
				
				if (before(databaseObject, UrlMapper.class)) {
					UrlMapper urlMapper = project.getUrlMapper();
					if (urlMapper != null) {
						put("urlmapper", urlMapper);
					}
				}
				
			} else if (databaseObject instanceof Connector) {
				Connector connector = (Connector) databaseObject;
				
				if (before(databaseObject, Transaction.class)) {
					put("transactions", connector.getTransactionsList());
				}
				
			} else if (databaseObject instanceof TransactionWithVariables) {
				TransactionWithVariables transaction = (TransactionWithVariables) databaseObject;
				
				if (before(databaseObject, RequestableVariable.class)) {
					put("variables", transaction.getVariablesList());
				}
				
			} else if (databaseObject instanceof Sequence) {
				Sequence sequence = (Sequence) databaseObject;
				
				if (before(databaseObject, RequestableVariable.class)) {
					put("variables", sequence.getVariablesList());
				}
				
			} else if (databaseObject instanceof UrlMapper) {
				UrlMapper urlMapper = (UrlMapper) databaseObject;

				if (before(databaseObject, UrlAuthentication.class)) {
					put("authentications", urlMapper.getAuthenticationList());
				}
				
				if (before(databaseObject, UrlMapping.class)) {
					put("mappings", urlMapper.getMappingList());
				}
				
			} else if (databaseObject instanceof UrlMapping) {
				UrlMapping urlMapping = (UrlMapping) databaseObject;

				if (before(databaseObject, UrlMappingOperation.class)) {
					put("operations", urlMapping.getOperationList());
				}
				
			} else if (databaseObject instanceof UrlMappingOperation) {
				UrlMappingOperation urlMappingOperation = (UrlMappingOperation) databaseObject;

				if (before(databaseObject, UrlMappingParameter.class)) {
					put("parameters", urlMappingOperation.getParameterList());
				}
				
				if (before(databaseObject, UrlMappingResponse.class)) {
					put("responses", urlMappingOperation.getResponseList());
				}
				
			} else if (databaseObject instanceof MobileApplication) {
				MobileApplication mobileApplication = (MobileApplication) databaseObject;

				IApplicationComponent applicationComponent = mobileApplication.getApplicationComponent();
				
				if (before(mobileApplication, com.twinsoft.convertigo.beans.mobile.components.ApplicationComponent.class)) {
					if (applicationComponent instanceof com.twinsoft.convertigo.beans.mobile.components.ApplicationComponent) {
						com.twinsoft.convertigo.beans.mobile.components.ApplicationComponent app = GenericUtils.cast(applicationComponent);
						walk(app);
					}
				}
				if (before(mobileApplication, com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent.class)) {
					if (applicationComponent instanceof com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent) {
						com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent app = GenericUtils.cast(applicationComponent);
						walk(app);
					}
				}
				
			}
			/*****************************************************************************************************************************/
			/**           com.twinsoft.convertigo.beans.mobile.components                                                                   */
			/*****************************************************************************************************************************/
			else if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.ApplicationComponent) {
				com.twinsoft.convertigo.beans.mobile.components.ApplicationComponent applicationComponent = GenericUtils.cast(databaseObject);
				
				if (before(databaseObject, com.twinsoft.convertigo.beans.mobile.components.PageComponent.class)) {
					put("pages", applicationComponent.getPageComponentList());
				}
				
				if (before(databaseObject, com.twinsoft.convertigo.beans.mobile.components.UIActionStack.class)) {
					put("actions", applicationComponent.getSharedActionList());
				}
				
				if (before(databaseObject, com.twinsoft.convertigo.beans.mobile.components.UISharedComponent.class)) {
					put("components", applicationComponent.getSharedComponentList());
				}
				
			} else if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.PageComponent) {
				// do not walk
				
			} else if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UIActionStack) {
				com.twinsoft.convertigo.beans.mobile.components.UIActionStack uisa = GenericUtils.cast(databaseObject);
				
				if (before(databaseObject, com.twinsoft.convertigo.beans.mobile.components.UIComponent.class)) {
					put("variables", uisa.getVariables());
				}
				
			} else if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UISharedComponent) {
				com.twinsoft.convertigo.beans.mobile.components.UISharedComponent uisc = GenericUtils.cast(databaseObject);
				
				if (before(databaseObject, com.twinsoft.convertigo.beans.mobile.components.UIComponent.class)) {
					put("variables", uisc.getVariables());
				}
			}
			/*****************************************************************************************************************************/
			/**           com.twinsoft.convertigo.beans.ngx.components                                                                   */
			/*****************************************************************************************************************************/
			else if (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent) {
				com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent applicationComponent = GenericUtils.cast(databaseObject);
				
				if (before(databaseObject, com.twinsoft.convertigo.beans.ngx.components.PageComponent.class)) {
					put("pages", applicationComponent.getPageComponentList());
				}
				
				if (before(databaseObject, com.twinsoft.convertigo.beans.ngx.components.UIActionStack.class)) {
					put("actions", applicationComponent.getSharedActionList());
				}
				
				if (before(databaseObject, com.twinsoft.convertigo.beans.ngx.components.UISharedComponent.class)) {
					put("components", applicationComponent.getSharedComponentList());
				}
				
			} else if (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.PageComponent) {
				// do not walk
				
			} else if (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UIActionStack) {
				com.twinsoft.convertigo.beans.ngx.components.UIActionStack uisa = GenericUtils.cast(databaseObject);
				
				if (before(databaseObject, com.twinsoft.convertigo.beans.ngx.components.UIComponent.class)) {
					put("variables", uisa.getVariables());
				}
				
			} else if (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UISharedRegularComponent) {
				com.twinsoft.convertigo.beans.ngx.components.UISharedRegularComponent uisc = GenericUtils.cast(databaseObject);
				
				if (before(databaseObject, com.twinsoft.convertigo.beans.ngx.components.UIComponent.class)) {
					put("variables", uisc.getVariables());
					put("events", uisc.getUICompEventList());
				}
			}
			else {
				super.walk(databaseObject);
			}
		}
	}
	
	private class MdWalker extends WalkHelper {
		private String content = "";
		
		private String getContent() {
			return content;
		}
		
		private String writeDetailContentBlock(DatabaseObject dbo) {
			int level = getLevel(dbo);
			String image = getC8oIconMd(dbo);
			String name = getLabel(dbo);
			String comment = getComment(dbo);
			
			if (dbo instanceof ProjectSchemaReference) {
				String readmeUrl = getProjectReadmeUrl(((ProjectSchemaReference)dbo).getParser());
				if (!readmeUrl.isEmpty()) {
					comment += String.format("%nsee [readme](%s)", readmeUrl);
				}
			} else if (dbo instanceof RemoteFileReference) {
				String link = ((RemoteFileReference)dbo).getUrlpath();
				if (link.isEmpty()) link = ((RemoteFileReference)dbo).getFilepath();
				comment += String.format("%nsee [documentation](%s)", link);
			}
			
			/*if (dbo instanceof UrlMapper) {
				String baseUrl = EnginePropertiesManager.getProperty(PropertyName.APPLICATION_SERVER_CONVERTIGO_URL);
				String yamlUrl = baseUrl + "/openapi?YAML&__project=InseeBE";
				String swaggerUrl = baseUrl;
				try {
					swaggerUrl = baseUrl + "/swagger/dist/index.html?url="+URLEncoder.encode(yamlUrl, "UTF-8");
				} catch (UnsupportedEncodingException e) {}
				comment += String.format("%n%nget [YAML description](%s)", yamlUrl);
				comment += String.format("%n%nopen [API console](%s)", swaggerUrl);
			}*/
			
			String block = "";
			block += writeHeadingBlock(level, image + " " + name);
			block += writeTextBlock(comment);
			return block;
		}
		
		private String writeSimpleBlock(DatabaseObject dbo) {
			String image = getC8oIconImg(dbo);
			String name = dbo.getName();
			String comment = getComment(dbo);
			
			if (dbo instanceof OperationResponse) {
				name = getLabel(dbo);
				comment = comment.isEmpty() ? ((OperationResponse)dbo).getStatusText() : comment;
			}
			
			return  writeTableRowBlock(image + "&nbsp;" + name, comment);
		}
		
		private String writeHeadingBlock(int level, String text) {
			return String.format("%n%1$s %2$s%n%n", "#".repeat(level), text);
		}
		
		private String writeTitleBlock(String text) {
			return String.format("%n%s%n%n", text);
		}
		
		private String writeTableStartBlock() {
			return String.format("%s%n", "<table>");
		}
		
		private String writeTableEndBlock() {
			return String.format("%s%n", "</table>");
		}
		
		private String writeTableHeaderBlock(String... args) {
//			String line1 = "|";
//			String line2 = "|---".repeat(args.length) + "|";
//			for (int i=0; i< args.length; i++) {line1 += " "+ args[i] + " |";};
//			return String.format("%1$s%n%2$s%n", line1, line2);
			String headers = "";
			for (int i=0; i< args.length; i++) {headers += String.format("%1$s%n%2$s%n%3$s%n", "<th>", args[i], "</th>");};
			return String.format("%1$s%n%2$s%3$s%n", "<tr>", headers, "</tr>");
		}
		
		private String writeTableRowBlock(String... args) {
//			String line = "";
//			for (int i=0; i< args.length; i++) {line += " "+ args[i] + " |";};
//			return String.format("|%s%n", line);
			String row = "";
			for (int i=0; i< args.length; i++) {row += String.format("%1$s%n%2$s%n%3$s%n", "<td>", args[i], "</td>");};
			return String.format("%1$s%n%2$s%3$s%n", "<tr>", row, "</tr>");
		}
		
		private String writeTextBlock(String text) {
			return String.format("%s%n", text);
		}
		
		private String writeReadmeBlock() {
			return "";//String.format("[readme](./readme.md)%n%n");
		}
		
		private String writeEmtyLineBlock() {
			return String.format("%n");
		}
		
		private String writeDetailsStartBlock(String title) {
			return String.format("%n%1$s%2$s%n%n", "<details><summary>"+title+"</summary>","<blockquote><p>");
		}
		
		private String writeDetailsEndBlock() {
			return String.format("%1$s%2$s%n", "</p></blockquote>","</details>");
		}

		private void walkDetailList(String title, List<? extends DatabaseObject> list) throws Exception {
			List<? extends DatabaseObject> filteredList = sort(filter(list, MarkdownType.Project));
			if (filteredList.size() > 0) {
				content += writeDetailsStartBlock("<span style=\"color:"+ FONT_COLOR +"\"><i>"+title+"</i></span>");
				for (DatabaseObject dbo : filteredList) {
					if (filteredList.size() > 1) content += writeDetailsStartBlock(getSummary(dbo));
					content += writeDetailContentBlock(dbo);
					walk(dbo);
					if (filteredList.size() > 1) content += writeDetailsEndBlock();
				}
				content += writeDetailsEndBlock();
			}
		}
		
		private void walkSimpleList(String title, List<? extends DatabaseObject> list) throws Exception {
			List<? extends DatabaseObject> filteredList = sort(filter(list, MarkdownType.Project));
			if (filteredList.size() > 0) {
				content += writeTitleBlock("<span style=\"color:"+ FONT_COLOR +"\">"+title+"</span>");
				content += writeTableStartBlock();
				content += writeTableHeaderBlock("name", "comment");
				for (DatabaseObject dbo : filteredList) {
					content += writeSimpleBlock(dbo);
					walk(dbo);
				}
				content += writeTableEndBlock();
				content += writeEmtyLineBlock();
			}
		}
		
		@Override
		protected void walk(DatabaseObject databaseObject) throws Exception {
			if (databaseObject instanceof Project) {
				content += writeReadmeBlock();
				
				Project project = (Project) databaseObject;
				content += writeDetailContentBlock(project);
				
				if (before(databaseObject, Reference.class)) {
					List<Reference> filteredList = project.getReferenceList()
							  						.stream()
							  						.filter(ref -> !isMobileBuilderTemplate(ref))
							  						.collect(Collectors.toList());
					walkDetailList("References", filteredList);
				}

				if (before(databaseObject, Connector.class)) {
					walkDetailList("Connectors", project.getConnectorsList());
				}

				if (before(databaseObject, Sequence.class)) {
					walkDetailList("Sequences", project.getSequencesList());
				}
				
				if (before(databaseObject, MobileApplication.class)) {
					MobileApplication mobileApplication = project.getMobileApplication();
					if (mobileApplication != null) {
						List<MobileApplication> list = new ArrayList<MobileApplication>();
						list.add(mobileApplication);
						walkDetailList("Mobile Application", list);
					}
				}
				
				if (before(databaseObject, UrlMapper.class)) {
					UrlMapper urlMapper = project.getUrlMapper();
					if (urlMapper != null) {
						List<UrlMapper> list = new ArrayList<UrlMapper>();
						list.add(urlMapper);
						walkDetailList("Rest Web Service", list);
					}
				}
				
			} else if (databaseObject instanceof Connector) {
				Connector connector = (Connector) databaseObject;
				
				if (before(databaseObject, Transaction.class)) {
					walkDetailList("Transactions", connector.getTransactionsList());
				}
				
			} else if (databaseObject instanceof TransactionWithVariables) {
				TransactionWithVariables transaction = (TransactionWithVariables) databaseObject;
				
				if (before(databaseObject, RequestableVariable.class)) {
					walkSimpleList("Variables", transaction.getVariablesList());
				}
				
			} else if (databaseObject instanceof Sequence) {
				Sequence sequence = (Sequence) databaseObject;
				
				if (before(databaseObject, RequestableVariable.class)) {
					walkSimpleList("Variables", sequence.getVariablesList());
				}
				
			} else if (databaseObject instanceof UrlMapper) {
				UrlMapper urlMapper = (UrlMapper) databaseObject;

				if (before(databaseObject, UrlAuthentication.class)) {
					walkDetailList("Authentications", urlMapper.getAuthenticationList());
				}
				
				if (before(databaseObject, UrlMapping.class)) {
					walkDetailList("Mappings", urlMapper.getMappingList());
				}
				
			} else if (databaseObject instanceof UrlMapping) {
				UrlMapping urlMapping = (UrlMapping) databaseObject;

				if (before(databaseObject, UrlMappingOperation.class)) {
					walkDetailList("Operations", urlMapping.getOperationList());
				}
				
			} else if (databaseObject instanceof UrlMappingOperation) {
				UrlMappingOperation urlMappingOperation = (UrlMappingOperation) databaseObject;

				if (before(databaseObject, UrlMappingParameter.class)) {
					walkSimpleList("Parameters", urlMappingOperation.getParameterList());
				}
				
				if (before(databaseObject, UrlMappingResponse.class)) {
					walkSimpleList("Responses", urlMappingOperation.getResponseList());
				}
				
			} else if (databaseObject instanceof MobileApplication) {
				MobileApplication mobileApplication = (MobileApplication) databaseObject;

				IApplicationComponent applicationComponent = mobileApplication.getApplicationComponent();
				
				if (before(mobileApplication, com.twinsoft.convertigo.beans.mobile.components.ApplicationComponent.class)) {
					if (applicationComponent instanceof com.twinsoft.convertigo.beans.mobile.components.ApplicationComponent) {
						com.twinsoft.convertigo.beans.mobile.components.ApplicationComponent app = GenericUtils.cast(applicationComponent);
						walk(app);
					}
				}
				if (before(mobileApplication, com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent.class)) {
					if (applicationComponent instanceof com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent) {
						com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent app = GenericUtils.cast(applicationComponent);
						walk(app);
					}
				}
				
			}
			/*****************************************************************************************************************************/
			/**           com.twinsoft.convertigo.beans.mobile.components                                                                   */
			/*****************************************************************************************************************************/
			else if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.ApplicationComponent) {
				com.twinsoft.convertigo.beans.mobile.components.ApplicationComponent applicationComponent = GenericUtils.cast(databaseObject);
				
				if (before(databaseObject, com.twinsoft.convertigo.beans.mobile.components.PageComponent.class)) {
					walkDetailList("Pages", applicationComponent.getPageComponentList());
				}
				
				if (before(databaseObject, com.twinsoft.convertigo.beans.mobile.components.UIActionStack.class)) {
					walkDetailList("Shared Actions", applicationComponent.getSharedActionList());
				}
				
				if (before(databaseObject, com.twinsoft.convertigo.beans.mobile.components.UISharedComponent.class)) {
					walkDetailList("Shared Components", applicationComponent.getSharedComponentList());
				}
				
			} else if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.PageComponent) {
				// do not walk
				
			} else if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UIActionStack) {
				com.twinsoft.convertigo.beans.mobile.components.UIActionStack uisa = GenericUtils.cast(databaseObject);
				
				if (before(databaseObject, com.twinsoft.convertigo.beans.mobile.components.UIComponent.class)) {
					walkSimpleList("Variables", uisa.getVariables());
				}
				
			} else if (databaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.UISharedComponent) {
				com.twinsoft.convertigo.beans.mobile.components.UISharedComponent uisc = GenericUtils.cast(databaseObject);
				
				if (before(databaseObject, com.twinsoft.convertigo.beans.mobile.components.UIComponent.class)) {
					walkSimpleList("Variables", uisc.getVariables());
				}
			}
			/*****************************************************************************************************************************/
			/**           com.twinsoft.convertigo.beans.ngx.components                                                                   */
			/*****************************************************************************************************************************/
			else if (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent) {
				com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent applicationComponent = GenericUtils.cast(databaseObject);
				
				if (before(databaseObject, com.twinsoft.convertigo.beans.ngx.components.PageComponent.class)) {
					walkDetailList("Pages", applicationComponent.getPageComponentList());
				}
				
				if (before(databaseObject, com.twinsoft.convertigo.beans.ngx.components.UIActionStack.class)) {
					walkDetailList("Shared Actions", applicationComponent.getSharedActionList());
				}
				
				if (before(databaseObject, com.twinsoft.convertigo.beans.ngx.components.UISharedComponent.class)) {
					walkDetailList("Shared Components", applicationComponent.getSharedComponentList());
				}
				
			} else if (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.PageComponent) {
				// do not walk
				
			} else if (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UIActionStack) {
				com.twinsoft.convertigo.beans.ngx.components.UIActionStack uisa = GenericUtils.cast(databaseObject);
				
				if (before(databaseObject, com.twinsoft.convertigo.beans.ngx.components.UIComponent.class)) {
					walkSimpleList("Variables", uisa.getVariables());
				}
				
			} else if (databaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.UISharedRegularComponent) {
				com.twinsoft.convertigo.beans.ngx.components.UISharedRegularComponent uisc = GenericUtils.cast(databaseObject);
				
				if (before(databaseObject, com.twinsoft.convertigo.beans.ngx.components.UIComponent.class)) {
					walkSimpleList("Variables", uisc.getVariables());
					walkSimpleList("Events", uisc.getUICompEventList());
				}
			}
			else {
				super.walk(databaseObject);
			}
		}
	}
	
	private static Map<String, Object> getFtlDataModel(DatabaseObject dbo) {
		try {
			FtlWalker fw = ReadmeBuilder.instance.new FtlWalker();
			fw.init(dbo);
			
			Map<String, Object> dboMap = fw.getDboMap();
			
			PropertyDescriptor[] pds = Introspector.getBeanInfo(dbo.getClass()).getPropertyDescriptors();
			for (PropertyDescriptor pd : pds) {
				Method getter = pd.getReadMethod();
				Method setter = pd.getWriteMethod();
				if (getter != null && setter != null) {
					String propertyName = pd.getName();
					Object value = getter.invoke(dbo);
					
					if (propertyName.equals("comment")) {
						String comment = getComment(dbo);
						
						if (dbo instanceof ProjectSchemaReference) {
							String readmeUrl = getProjectReadmeUrl(((ProjectSchemaReference)dbo).getParser());
							if (!readmeUrl.isEmpty()) {
								comment += String.format("%nsee [readme](%s)", readmeUrl);
							}
						} else if (dbo instanceof RemoteFileReference) {
							String link = ((RemoteFileReference)dbo).getUrlpath();
							if (link.isEmpty()) link = ((RemoteFileReference)dbo).getFilepath();
							comment += String.format("%nsee [documentation](%s)", link);
						}
						
						dboMap.put("summary", getFirstLine(comment));

						value = comment;
					}
					
					dboMap.put(propertyName, value);
				}
			}
			
			dboMap.put("label", getLabel(dbo));
			dboMap.put("c8oIconMd", getC8oIconMd(dbo));
			dboMap.put("c8oIconImg", getC8oIconImg(dbo));
			dboMap.put("classname", dbo.getClass().getName());
			
			if (dbo instanceof Project) {
				dboMap.put("url", ProjectUrlParser.getUrl((Project)dbo));
				dboMap.put("contributeUrl", ProjectUrlParser.getContributeUrl((Project)dbo));
				dboMap.put("usageUrl", ProjectUrlParser.getUsageUrl((Project)dbo));
			}
			
			return dboMap;
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new HashMap<String, Object>();
	}
	
	private static String getLabel(DatabaseObject dbo) {
		String label = dbo.toString();
		int i = label.toLowerCase().indexOf("! broken");
		if (i != -1) label = label.substring(0, i);
		label = label.trim();
		return label;
	}
	
	private static int getLevel(DatabaseObject dbo) {
		/*try {
			String qname = dbo.getFullQName().replace(':', '.');
			qname = qname.replace(dbo.getProject().getQName(), ".");
			return StringUtils.countMatches(qname, ".");
		} catch (Exception e) {}
		return 0;*/
		if (dbo instanceof Project) {
			return 1;
		} else if (dbo.getParent() instanceof Project) {
			return 2;
		} else {
			return 3;
		}
	}
	
	private static String getIconUrl(DatabaseObject dbo) {
		String iconName = MySimpleBeanInfo.getIconName(dbo, BeanInfo.ICON_COLOR_16x16);
		if (iconName.startsWith("/com/twinsoft/convertigo")) {
			return "https://github.com/convertigo/convertigo/blob/develop/engine/src" + iconName;
		}
		try {
			File f = new File(iconName);
			if (f.exists() && f.isFile()) {
				Path fPath = Paths.get(f.getCanonicalPath());
				Path pPath = Paths.get(dbo.getProject().getDirFile().getCanonicalPath());
				String relPath = pPath.relativize(fPath).toString().replace('\\', '/');
				return "./" + relPath;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "./unknow.png";
	}
	
	private static String getC8oIconMd(DatabaseObject dbo) {
		String dboClass = dbo.getClass().getSimpleName();
		String iconUrl = getIconUrl(dbo);
		return "![]("+iconUrl+"?raw=true \""+ dboClass +"\")";
	}
	
	private static String getC8oIconImg(DatabaseObject dbo) {
		String dboClass = dbo.getClass().getSimpleName();
		String iconUrl = getIconUrl(dbo);
		return "<img src=\""+ iconUrl+"?raw=true \"  alt=\""+dboClass+"\" >";
	}
	
	private static String getFirstLine(String text) {
		String line = text;
		try {
			if (!line.isEmpty()) {
				if (line.indexOf(System.lineSeparator()) != -1) {
					line = line.substring(0, line.indexOf(System.lineSeparator()));
				}
				if (line.indexOf('.') == -1) {
					line += ".";
				}
				line = line.substring(0, line.indexOf('.'));
			}
		} catch (Exception e) {}
		return line;
	}
	
	private static String getComment(DatabaseObject dbo) {
		String comment = dbo.getComment();
		
		// handle objects for a c8oForms based project
		try {
			String json = comment;
			JSONObject jsonObject = new JSONObject(json);
			if (jsonObject.has("en")) {
				comment = jsonObject.getJSONObject("en").getString("comment");
			} else {
				String firstKey = (String) jsonObject.keys().next();
				comment = jsonObject.getJSONObject(firstKey).getString("comment");
			}
		} catch (Exception e) {}
		
		comment = comment.replace("<code>", "<breakLine><breakLine>```<breakLine>");
		comment = comment.replace("</code>", "<breakLine>```<breakLine><breakLine>");
		comment = comment.replace("<breakLine>", System.lineSeparator());
		
		return comment;
	}
	
	private static String getSummary(DatabaseObject dbo) {
		String summary = "<b>"+getLabel(dbo)+"</b>";
		String line = getFirstLine(getComment(dbo));
		if (!line.isEmpty()) {
			summary += " : " + line;
		}
		return summary;
	}
	
	private static boolean isEnabled(DatabaseObject dbo) {
		try {
			Method method = dbo.getClass().getMethod("isEnabled", new Class[] {});
			return (boolean) method.invoke(dbo, new Object[] {});
		} catch (Exception e) {}
		return true;
	}
	
	private static boolean isExposed(DatabaseObject dbo) {
		try {
			Method method = dbo.getClass().getMethod("isExposed", new Class[] {});
			return (boolean) method.invoke(dbo, new Object[] {});
		} catch (Exception e) {
			try {
				Method method = dbo.getClass().getMethod("isWsdl", new Class[] {});
				return (boolean) method.invoke(dbo, new Object[] {});
			} catch (Exception ex) {}
		}
		return true;
	}
	
	private static boolean isPublic(DatabaseObject dbo) {
		/*try {
			Method method = dbo.getClass().getMethod("isPublicAccessibility", new Class[] {});
			return (boolean) method.invoke(dbo, new Object[] {});
		} catch (Exception e) {}*/
		return true;
	}
	
	private static boolean isMobileBuilderTemplate(DatabaseObject dbo) {
		if (dbo instanceof ProjectSchemaReference) {
			String p_name = ((ProjectSchemaReference)dbo).getParser().getProjectName();
			return p_name.startsWith("mobilebuilder_tpl");
		}
		return dbo.getProject().getName().startsWith("mobilebuilder_tpl");
	}
	
	private static List<? extends DatabaseObject> sort(List<? extends DatabaseObject> list) {
		List<DatabaseObject> sortedList = new ArrayList<DatabaseObject>(list);
		Collections.sort(sortedList, new Comparator<DatabaseObject>() {
			@Override
			public int compare(DatabaseObject c1, DatabaseObject c2) {
				return c1.toString().toLowerCase().compareTo(c2.toString().toLowerCase());
			}				
		} );
		return sortedList = Collections.unmodifiableList(sortedList);
	}
	
	private static String getProjectReadmeUrl(ProjectUrlParser parser) {
		return ProjectUrlParser.getReadmeUrl(parser.getProjectName());
	}
	
	private static List<? extends DatabaseObject> filter(List<? extends DatabaseObject> list, MarkdownType type) {
		List<? extends DatabaseObject> filteredList;
		if (MarkdownType.Project.equals(type)) {
			filteredList = list
						.stream()
						.filter(dbo -> isEnabled(dbo))
						.collect(Collectors.toList());
		} else {
			filteredList = list
						.stream()
						.filter(dbo -> isEnabled(dbo) && isPublic(dbo) && isExposed(dbo))
						.collect(Collectors.toList());
		}
		return filteredList = Collections.unmodifiableList(filteredList);
	}
	
	private static void generateProjectMd(Project project) throws Exception {
		MdWalker rw = ReadmeBuilder.instance.new MdWalker();
		rw.init(project);
		String content = rw.getContent();
		
		File mdFile = new File(project.getDirPath(),"project.md");
		String excontent = mdFile.exists() ? FileUtils.readFileToString(mdFile, "UTF-8") : "";
		if (!content.equals(excontent)) {
			FileUtils.write(mdFile, content, "UTF-8");
		}
	}
	
	private static void generateReadmeFtl(Project project) throws Exception {
		getFtlFile(project);
	}
	
	private static File getFtlFile(Project project) throws Exception {
        File ftlFile = new File(project.getDirFile(), "readme.ftl");
        if (!ftlFile.exists()) {
        	String content = ReadmeBuilder.instance.getDefaultFtlContent();
        	FileUtils.write(ftlFile, content, "UTF-8");
        }
        return ftlFile;
	}
	
	private static void generateReadmeMd(Project project) throws Exception {
        
		// Configure FreeMarker
        Configuration cfg = new Configuration(new freemarker.template.Version(2, 3, 31));
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setObjectWrapper(ReadmeBuilder.instance.new FtlDboWrapper(cfg.getIncompatibleImprovements()));
        cfg.setDirectoryForTemplateLoading(project.getDirFile());
        cfg.setAPIBuiltinEnabled(true);
        cfg.setDefaultEncoding("UTF-8");
        //cfg.setLocale(Locale.US);

        // Generate data model
		Map<String, Object> settings = new HashMap<String, Object>();
		settings.put("lineBreak", System.lineSeparator());
        SimpleHash map = new SimpleHash(cfg.getObjectWrapper());
		map.put("settings", settings);
        map.put("project", project);
        
        // Process ftl template and generate output
        Template template = cfg.getTemplate(getFtlFile(project).getName());
        
        // Write output to the console
        //Writer consoleWriter = new OutputStreamWriter(System.out);
        //template.process(map, consoleWriter);

        // Write output to md file
        File mdFile = new File(project.getDirFile(), "readme.md");
        Writer fileWriter = new FileWriter(mdFile, Charset.forName("UTF-8"));
        try {
            template.process(map, fileWriter);
        } finally {
            fileWriter.close();
        }
	}
	
	private static String FONT_COLOR = "DarkGoldenRod";
	
	private static ReadmeBuilder instance = new ReadmeBuilder();
	
	private String defaultFtlContent;
	
	private ReadmeBuilder() {
		
	}
	
	private synchronized String getDefaultFtlContent() throws Exception {
		if (defaultFtlContent != null) {
			return defaultFtlContent;
		}
		
		File defaultFtlTemplate = new File(Engine.TEMPLATES_PATH, "ftl/readme.ftl");
		if (defaultFtlTemplate.exists()) {
			return defaultFtlContent = FileUtils.readFileToString(defaultFtlTemplate, "UTF-8");
		} else {
			Engine.logBeans.warn("Unable to retrieve default c8o template \"ftl/readme.ftl\"");
			return defaultFtlContent = "# project.name" + System.lineSeparator();
		}
	}
	
	public static void process(Project project, MarkdownType type) {
		if (Engine.isStudioMode() && !isMobileBuilderTemplate(project)) {
			try {
				if (MarkdownType.Project.equals(type) || MarkdownType.Readme.equals(type)) {
					// Generate project.md : if does not exist or is different
					generateProjectMd(project);
				}
				
				if (MarkdownType.Readme.equals(type)) {
					// Generate readme.ftl : if does not exist, otherwise keep existing ftl which may have been customized by user
					generateReadmeFtl(project);
					
					// Generate readme.md 	: if does not exist, otherwise overwrites it
					generateReadmeMd(project);
				}
			} catch (Exception e) {
				Engine.logBeans.error("Unable to generate markdown files for project \""+ project.getName() +"\"", e);
			}
		}
	}
	
	public enum MarkdownType {
		Project,
		Readme;
	}
}
