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

package com.twinsoft.convertigo.engine.util;

import io.swagger.models.Contact;
import io.swagger.models.ExternalDocs;
import io.swagger.models.Info;
import io.swagger.models.Model;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.RefModel;
import io.swagger.models.Response;
import io.swagger.models.Scheme;
import io.swagger.models.SecurityRequirement;
import io.swagger.models.Swagger;
import io.swagger.models.Tag;
import io.swagger.models.auth.BasicAuthDefinition;
import io.swagger.models.auth.SecuritySchemeDefinition;
import io.swagger.models.parameters.AbstractSerializableParameter;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.FormParameter;
import io.swagger.models.parameters.HeaderParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.models.parameters.SerializableParameter;
import io.swagger.models.properties.BooleanProperty;
import io.swagger.models.properties.DoubleProperty;
import io.swagger.models.properties.IntegerProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.properties.StringProperty;
import io.swagger.models.utils.PropertyModelConverter;
import io.swagger.parser.SwaggerParser;
import io.swagger.util.Json;
import io.swagger.util.Yaml;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.constants.Constants;
import org.apache.ws.commons.schema.utils.NamespaceMap;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.connectors.HttpConnector;
import com.twinsoft.convertigo.beans.core.IMappingRefModel;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.UrlAuthentication;
import com.twinsoft.convertigo.beans.core.UrlMapper;
import com.twinsoft.convertigo.beans.core.UrlMapping;
import com.twinsoft.convertigo.beans.core.UrlMappingOperation;
import com.twinsoft.convertigo.beans.core.UrlMappingParameter;
import com.twinsoft.convertigo.beans.core.UrlMappingParameter.DataContent;
import com.twinsoft.convertigo.beans.core.UrlMappingParameter.DataType;
import com.twinsoft.convertigo.beans.core.UrlMappingParameter.Type;
import com.twinsoft.convertigo.beans.core.UrlMappingResponse;
import com.twinsoft.convertigo.beans.core.UrlAuthentication.AuthenticationType;
import com.twinsoft.convertigo.beans.rest.AbstractRestOperation;
import com.twinsoft.convertigo.beans.transactions.AbstractHttpTransaction;
import com.twinsoft.convertigo.beans.transactions.HttpTransaction;
import com.twinsoft.convertigo.beans.transactions.JsonHttpTransaction;
import com.twinsoft.convertigo.beans.transactions.XmlHttpTransaction;
import com.twinsoft.convertigo.beans.variables.RequestableHttpMultiValuedVariable;
import com.twinsoft.convertigo.beans.variables.RequestableHttpVariable;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.SchemaManager.Option;
import com.twinsoft.convertigo.engine.enums.AuthenticationMode;
import com.twinsoft.convertigo.engine.enums.DoFileUploadMode;
import com.twinsoft.convertigo.engine.enums.HeaderName;
import com.twinsoft.convertigo.engine.enums.HttpMethodType;
import com.twinsoft.convertigo.engine.enums.MimeType;

public class SwaggerUtils {
	public static String servletMappingPath = "api";
	public static String jsonSchemaDirectory = "oas2";
	private static Pattern parseRequestUrl = Pattern.compile("http(s)?://(.*?)(/.*?"+servletMappingPath+")");

	public static Swagger read(String url) {
		return new SwaggerParser().read(url);
	}
	
	private static Swagger parseCommon(String requestUrl, Project project) {
		Swagger swagger = new Swagger();
		
		Contact contact = new Contact();
		/*contact.setName("Convertigo Support");
		contact.setEmail("support@convertigo.com");
		contact.setUrl("http://www.convertigo.com/#developers");*/
		
		Info info = new Info();
		info.setContact(contact);
		info.setTitle("Convertigo OAS2 REST API");
		info.setDescription("Find here all deployed projects");
		if (project != null) {
			info.setTitle(project.getName() + " OAS2 REST API");
			info.setDescription(project.getComment());
			info.setVersion(project.getVersion());			
		}

		List<Scheme> schemes = new ArrayList<Scheme>();
		String host;
		String basePath;
		
		Matcher matcher = parseRequestUrl.matcher(requestUrl);
		if (matcher.find()) {
			schemes.add(matcher.group(1) == null ? Scheme.HTTP : Scheme.HTTPS);
			host = matcher.group(2);
			basePath = matcher.group(3);
		} else {
			String webAppPath = EnginePropertiesManager.getProperty(PropertyName.APPLICATION_SERVER_CONVERTIGO_URL);
			int index = webAppPath.indexOf("://") + 3;
			host = webAppPath.substring(index, webAppPath.indexOf('/', index));
			basePath = webAppPath.substring(index + host.length()) + "/" + servletMappingPath;
			schemes.add(Scheme.HTTP);
			schemes.add(Scheme.HTTPS);
		}
		swagger.setInfo(info);
		swagger.setSchemes(schemes);
		swagger.setHost(host);
		swagger.setBasePath(basePath);
		
		swagger.setConsumes(Arrays.asList("multipart/form-data", MimeType.WwwForm.value(), MimeType.Json.value(), MimeType.Xml.value()));
		swagger.setProduces(Arrays.asList(MimeType.Json.value(), MimeType.Xml.value()));
		
		String oas3Url = requestUrl.substring(0,requestUrl.indexOf("/" + servletMappingPath)) + "/swagger/dist/index.html?" +
							URLUtils.encodePart("url",requestUrl.replace(servletMappingPath, OpenApiUtils.servletMappingPath) 
							+ "?YAML"+ (project != null ? "&__project=" + project.getName():""))
							+ (Engine.isStudioMode() ? "&showErrors" : "");
		
		ExternalDocs externalDocs = new ExternalDocs();
		externalDocs.setDescription("Switch to Open Api definition (oas3)");
		externalDocs.setUrl(oas3Url);
		swagger.setExternalDocs(externalDocs);
		
		return swagger;
	}
	
	public static Swagger parse(String requestUrl, Collection<UrlMapper> collection) {
		Swagger swagger = parseCommon(requestUrl, null);
		
		List<Tag> tags = new ArrayList<Tag>();
		Map<String, Path> paths = new HashMap<String, Path>();
		Map<String, Model> models = new HashMap<String, Model>();
		for (UrlMapper urlMapper : collection) {
			if (urlMapper != null) {
				Swagger p_swagger = parse(requestUrl, urlMapper);
				if (p_swagger != null) {
					if (p_swagger != null) {
						tags.addAll(p_swagger.getTags());
						paths.putAll(p_swagger.getPaths());
						models.putAll(p_swagger.getDefinitions());
					}
				}
			}
		}
		swagger.setTags(tags);
		swagger.setPaths(paths);
		swagger.setDefinitions(models);
		
		return swagger;
	}
	
	public static Swagger parse(String requestUrl, String projectName) {
		Swagger swagger;
		
		Project project = null;
		try {
			project = Engine.theApp.databaseObjectsManager.getProjectByName(projectName);
			swagger = parseCommon(requestUrl, project);
			
			List<Tag> tags = new ArrayList<Tag>();
			Tag tag = new Tag();
			tag.setName(projectName);
			tag.setDescription(project.getComment());
			tags.add(tag);
			swagger.setTags(tags);
			
		} catch (Exception e) {
			e.printStackTrace();
			swagger = new Swagger();
		}
		
		return swagger;
	}
	
	private static String getModels(String oasDirUrl, UrlMapper urlMapper) {
		Project project = urlMapper.getProject();
		String projectName = project.getName();
		
		// User defined models
		String models = "{}";
		String mapperModels = urlMapper.getModels();
		if (!mapperModels.isEmpty()) {
			models = mapperModels;
		}
		
		// Generated models from XSD
		File targetDir = new File(Engine.PROJECTS_PATH + "/" + projectName + "/" + jsonSchemaDirectory);
		boolean doIt = Engine.isStudioMode() || !targetDir.exists();
		if (doIt) {
			try {
				File xsdFile = new File(targetDir, "xsd.jsonschema");
				if (!xsdFile.exists()) {
					FileUtils.copyFile(new File(Engine.WEBAPP_PATH + "/oas/xsd2.jsonschema"), xsdFile, true);
				}
				
				XmlSchemaCollection xmlSchemaCollection = Engine.theApp.schemaManager.getSchemasForProject(projectName, Option.noCache);
				NamespaceMap nsMap = (NamespaceMap) xmlSchemaCollection.getNamespaceContext();
				for (XmlSchema xmlSchema : xmlSchemaCollection.getXmlSchemas()) {
					String tns = xmlSchema.getTargetNamespace();
					if (tns.equals(Constants.URI_2001_SCHEMA_XSD)) continue;
					if (tns.equals(SchemaUtils.URI_SOAP_ENC)) continue;
	
					String prefix = nsMap.getPrefix(tns);
					File jsonschemaFile = new File(targetDir, prefix+".jsonschema" );
					JSONObject jsonObject = JsonSchemaUtils.getJsonSchema(xmlSchemaCollection, xmlSchema, oasDirUrl, true);
					String content = jsonObject.toString(4);
					FileUtils.write(jsonschemaFile, content, "UTF-8");
					//System.out.println(content);
				}
			} catch (Exception e) {
				e.printStackTrace();
				try {
					Engine.logEngine.warn("Unexpected exception while generating Oas2 models from XSD", e);
					FileUtils.deleteDirectory(targetDir);
				} catch (Exception ex) {}
			}
		}
		
		return models;
	}
	
	public static Swagger parse(String requestUrl, UrlMapper urlMapper) {
		Project project = urlMapper.getProject();
		String projectName = project.getName();
		
		String oasDirUrl = requestUrl.substring(0,requestUrl.indexOf("/" + servletMappingPath)) + 
								"/projects/"+ projectName + "/"+ jsonSchemaDirectory+"/";
		
		Swagger swagger = parseCommon(requestUrl, project);
		
		List<Tag> tags = new ArrayList<Tag>();
		Tag tag = new Tag();
		tag.setName(urlMapper.getProject().getName());
		tag.setDescription(urlMapper.getProject().getComment());
		tags.add(tag);
		swagger.setTags(tags);
		
		// Security
		Map<String, SecuritySchemeDefinition> securityDefinitions = swagger.getSecurityDefinitions();
		for (UrlAuthentication authentication: urlMapper.getAuthenticationList()) {
			if (AuthenticationType.Basic.equals(authentication.getType())) {
				if (securityDefinitions == null || !securityDefinitions.containsKey("basicAuth")) {
					BasicAuthDefinition basicAuthDefinition = new BasicAuthDefinition();
					swagger.addSecurityDefinition("basicAuth", basicAuthDefinition);
					
					SecurityRequirement securityRequirement = new SecurityRequirement();
					securityRequirement.requirement("basicAuth", new ArrayList<String>());
					swagger.addSecurity(securityRequirement);
				}
			}
		}
		
		// Models and Schemas
		Map<String, Model> swagger_models = new HashMap<String, Model>();		
		try {
			String models = getModels(oasDirUrl, urlMapper);
			if (!models.isEmpty()) {
				ObjectMapper mapper = Json.mapper();
				JsonNode definitionNode = mapper.readTree(models);
				for (Iterator<Entry<String, JsonNode>> it = GenericUtils.cast(definitionNode.fields()); it.hasNext();) {
					Entry<String, JsonNode> entry = it.next();
					swagger_models.put(entry.getKey().toString(), mapper.convertValue(entry.getValue(), Model.class));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			Engine.logEngine.warn("Unexpected exception while reading UrlMapper defined models", e);
		}
		swagger.setDefinitions(swagger_models);
		
		// Mappings
		Map<String, Path> swagger_paths = new HashMap<String, Path>();
		try {
			for (UrlMapping urlMapping: urlMapper.getMappingList()) {
				Path swagger_path = new Path();
				for (UrlMappingOperation umo : urlMapping.getOperationList()) {
					Operation s_operation = new Operation();
					s_operation.setOperationId(umo.getQName());
					s_operation.setDescription(umo.getComment());
					s_operation.setSummary(umo.getComment());
					
					// Operation produces
					if (umo instanceof AbstractRestOperation) {
						DataContent dataOutput = ((AbstractRestOperation)umo).getOutputContent();
						if (dataOutput.equals(DataContent.toJson)) {
							s_operation.setProduces(Arrays.asList(MimeType.Json.value()));
						}
						else if (dataOutput.equals(DataContent.toXml)) {
							s_operation.setProduces(Arrays.asList(MimeType.Xml.value()));
						}
						else {
							s_operation.setProduces(Arrays.asList(MimeType.Json.value(), MimeType.Xml.value()));
						}
					}
					
					// Operation tags
					List<String> list = Arrays.asList(""+ project.getName());
					s_operation.setTags(list);
					
					// Operation consumes
					List<String> consumes = new ArrayList<String>();
					
					// Operation parameters
					List<Parameter> s_parameters = new ArrayList<Parameter>();
					// 1 - add path parameters
					for (String pathVarName: urlMapping.getPathVariableNames()) {
						PathParameter s_parameter = new PathParameter();
						s_parameter.setName(pathVarName);
						s_parameter.setRequired(true);
						s_parameter.setType("string");
						
						// retrieve parameter description from bean
						UrlMappingParameter ump = null;
						try {
							ump = umo.getParameterByName(pathVarName);
						} catch (Exception e) {}
						if (ump != null && ump.getType() == Type.Path) {
							s_parameter.setDescription(ump.getComment());
							s_parameter.setType(ump.getInputType().toLowerCase());
							
							Object value = ump.getValueOrNull();
							if (value != null) {
								s_parameter.setDefaultValue(String.valueOf(value));
							}
						}
						
						s_parameters.add(s_parameter);
					}
					// 2 - add other parameters
					for (UrlMappingParameter ump: umo.getParameterList()) {
						Parameter s_parameter = null;
						if (ump.getType() == Type.Query) {
							s_parameter = new QueryParameter();
						}
						else if (ump.getType() == Type.Form) {
							s_parameter = new FormParameter();
						}
						else if (ump.getType() == Type.Body) {
							s_parameter = new BodyParameter();
							if (ump instanceof IMappingRefModel) {
								String modelReference = ((IMappingRefModel)ump).getModelReference();
								if (!modelReference.isEmpty()) {
									if (modelReference.indexOf(".jsonschema") != -1) {
										modelReference = oasDirUrl + modelReference;
									}
									RefModel refModel = new RefModel(modelReference);
									((BodyParameter)s_parameter).setSchema(refModel);
								}
							}
						}
						else if (ump.getType() == Type.Header) {
							s_parameter = new HeaderParameter();
						}
						else if (ump.getType() == Type.Path) {
							// ignore : should have been treated before
						}
						
						if (s_parameter != null) {
							s_parameter.setName(ump.getName());
							s_parameter.setDescription(ump.getComment());
							s_parameter.setRequired(ump.isRequired());
							
							if (s_parameter instanceof SerializableParameter) {
								boolean isArray = ump.isMultiValued() || ump.isArray();
								String _type = isArray ? "array":ump.getDataType().name().toLowerCase();
								String _collectionFormat = ump.isMultiValued() ? "multi":(isArray ? "csv":null);
								Property _items = isArray ? getItems(ump.getDataType()):null;
								
								((SerializableParameter)s_parameter).setType(_type);
								((SerializableParameter)s_parameter).setCollectionFormat(_collectionFormat);
								((SerializableParameter) s_parameter).setItems(_items);
								
								Object value = ump.getValueOrNull();
								if (value != null) {
									String collection = ((SerializableParameter)s_parameter).getCollectionFormat();
									if (collection != null && collection.equals("multi")) {
										Property items = new StringProperty();
										//items.setDefault(String.valueOf(value));
										((SerializableParameter) s_parameter).setItems(items);
										//((SerializableParameter) s_parameter).setEnumValue(Arrays.asList("val1","val2","val3"));
									} else {
										((AbstractSerializableParameter<?>)s_parameter).setDefaultValue(String.valueOf(value));
									}
								}
							}
							
							DataContent dataInput = ump.getInputContent();
							if (dataInput.equals(DataContent.toJson)) {
								if (!consumes.contains(MimeType.Json.value())) {
									consumes.add(MimeType.Json.value());
								}
							}
							else if (dataInput.equals(DataContent.toXml)) {
								if (!consumes.contains(MimeType.Xml.value())) {
									consumes.add(MimeType.Xml.value());
								}
							}
							
							// swagger-ui workaround for invalid request content-type for POST
							if (ump.getType() == Type.Form) {
								if (!DataType.File.equals(ump.getDataType())) {
									if (!consumes.contains(MimeType.WwwForm.value())) {
										consumes.add(MimeType.WwwForm.value());
									}
								} else {
									if (!consumes.contains("multipart/form-data")) {
										consumes.add("multipart/form-data");
									}
								}
							}
							
							// add parameter
							if (ump.isExposed()) {
								s_parameters.add(s_parameter);
							}
						}
					}
					s_operation.setParameters(s_parameters);
					
					if (!consumes.isEmpty()) {
						s_operation.setConsumes(consumes);
					}
					
					// Set operation responses
					Map<String, Response> responses = new HashMap<String, Response>();
					for (UrlMappingResponse umr: umo.getResponseList()) {
						String statusCode = umr.getStatusCode();
						if (!statusCode.isEmpty()) {
							if (!responses.containsKey(statusCode)) {
								Response response = new Response();
								//response.setDescription(umr.getComment());
								response.setDescription(umr.getStatusText());
								if (umr instanceof IMappingRefModel) {
									String modelReference = ((IMappingRefModel)umr).getModelReference();
									if (!modelReference.isEmpty()) {
										if (modelReference.indexOf(".jsonschema") != -1) {
											modelReference = oasDirUrl + modelReference;
										}
										RefProperty refProperty = new RefProperty(modelReference);
										response.setResponseSchema(new PropertyModelConverter().propertyToModel(refProperty));
									}
								}
								responses.put(statusCode, response);
							}
						}
					}
					if (responses.isEmpty()) {
						Response resp200 = new Response();
						resp200.description("successful operation");
						responses.put("200", resp200);
					}
					s_operation.setResponses(responses);
					
					// Add operation to path
					String s_method = umo.getMethod().toLowerCase();
					swagger_path.set(s_method, s_operation);
				}
				swagger_paths.put(urlMapping.getPathWithPrefix(), swagger_path);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			Engine.logEngine.error("Unexpected exception while parsing UrlMapper to generate definition", e);
		}
		
		swagger.setPaths(swagger_paths);
		
		return swagger;
	}
	
	public static HttpConnector createRestConnector(Swagger swagger) throws Exception {
		try {
			HttpConnector httpConnector = new HttpConnector();
			httpConnector.bNew = true;
			
			
			Info info = swagger.getInfo();
			String title = info != null ? info.getTitle():"";
			title = title == null || title.isEmpty() ? "RestConnector":title;
			httpConnector.setName(StringUtils.normalize(title));
			
			String host = swagger.getHost();
			int index = host.indexOf(":");
			String server = index == -1 ? host : host.substring(0, index);
			int port = index == -1 ? 0 : Integer.parseInt(host.substring(index+1),10);
			httpConnector.setServer(server);
			httpConnector.setPort(port <= 0 ? 80:port);
			
			String basePath = swagger.getBasePath();
			httpConnector.setBaseDir(basePath);
			
			Map<String, SecuritySchemeDefinition> securityMap = swagger.getSecurityDefinitions();
			if (securityMap != null && securityMap.size() > 0) {
				for (String securityName : securityMap.keySet()) {
					SecuritySchemeDefinition securityScheme = securityMap.get(securityName);
					if (securityScheme != null) {
						boolean isBasicScheme = securityScheme.getType().toLowerCase().equals("basic");
						if (isBasicScheme) {
							httpConnector.setAuthenticationType(AuthenticationMode.Basic);
							break;
						}
					}
				}
			}
			
			List<String> _consumeList = swagger.getConsumes();
			List<String> _produceList = swagger.getProduces();

			//Map<String, Model> models = swagger.getDefinitions();
			
			Map<String, Path> paths = swagger.getPaths();
			for (String subDir : paths.keySet()) {
				Path path = paths.get(subDir);
				
				// Add transactions
				List<Operation> operations = path.getOperations();
				for (Operation operation : operations) {
					
					HttpMethodType httpMethodType = null;
					if (operation.equals(path.getGet())) {
						httpMethodType = HttpMethodType.GET;
					} else if (operation.equals(path.getPost())) {
						httpMethodType = HttpMethodType.POST;
					} else if (operation.equals(path.getPut())) {
						httpMethodType = HttpMethodType.PUT;
					} else if (operation.equals(path.getDelete())) {
						httpMethodType = HttpMethodType.DELETE;
					} else if (operation.equals(path.getHead())) {
						httpMethodType = HttpMethodType.HEAD;
					} else if (operation.equals(path.getOptions())) {
						httpMethodType = HttpMethodType.OPTIONS;
					} else {
						httpMethodType = null;
					}
					
					if (httpMethodType != null) {
						List<String> consumeList = operation.getConsumes();
						consumeList = consumeList== null || consumeList.isEmpty() ? _consumeList : consumeList;
						
						List<String> produceList = operation.getProduces();
						produceList = produceList== null || produceList.isEmpty() ? _produceList : produceList;
						
						String operationId = operation.getOperationId();
						String description = operation.getDescription();
						String summary = operation.getSummary();
						
						String name = StringUtils.normalize(subDir + ":" + httpMethodType.toString());
						if (name.isEmpty()) {
							name = StringUtils.normalize(operationId);
							if (name.isEmpty()) {
								name = StringUtils.normalize(summary);
								if (name.isEmpty()) {
									name = "operation";
								}
							}
						}
						
						String comment = summary;
						if (comment == null)
							comment ="";
						
						if (comment.isEmpty()) {
							comment = description;
						}
						
						XMLVector<XMLVector<String>> httpParameters = new XMLVector<XMLVector<String>>();
						AbstractHttpTransaction transaction = new HttpTransaction();
						
						String h_ContentType = MimeType.WwwForm.value();
						if (consumeList != null) {
							if (consumeList.contains(MimeType.Json.value())) {
								h_ContentType = MimeType.Json.value();
							}
							else if (consumeList.contains(MimeType.Xml.value())) {
								h_ContentType = MimeType.Xml.value();
							}
							else {
								h_ContentType = consumeList.size() > 0 ? 
										consumeList.get(0) : MimeType.WwwForm.value();
							}
						}
						
						String h_Accept = MimeType.Json.value();
						if (produceList != null) {
							if (produceList.contains(h_ContentType)) {
								h_Accept = h_ContentType;
							}
							else {
								if (produceList.contains(MimeType.Json.value())) {
									h_Accept = MimeType.Json.value();
								}
								else if (produceList.contains(MimeType.Xml.value())) {
									h_Accept = MimeType.Xml.value();
								}
							}
							
							if (consumeList == null && h_Accept != null) {
								h_ContentType = h_Accept;
							}
						}
						
						if (h_Accept != null) {
							XMLVector<String> xmlv = new XMLVector<String>();
							xmlv.add("Accept");
							xmlv.add(h_Accept);
				   			httpParameters.add(xmlv);
				   			
							if (h_Accept.equals(MimeType.Xml.value())) {
								transaction = new XmlHttpTransaction();
								((XmlHttpTransaction)transaction).setXmlEncoding("UTF-8");
							}
							else if (h_Accept.equals(MimeType.Json.value())) {
								transaction = new JsonHttpTransaction();
								((JsonHttpTransaction)transaction).setIncludeDataType(false);
							}
						}
						
						// Add variables
						boolean hasBodyVariable = false;
						List<io.swagger.models.parameters.Parameter> parameters = operation.getParameters();
						for (io.swagger.models.parameters.Parameter parameter : parameters) {
							//String p_access = parameter.getAccess();
							String p_description = parameter.getDescription();
							//String p_in = parameter.getIn();
							String p_name = parameter.getName();
							//String p_pattern = parameter.getPattern();
							boolean p_required = parameter.getRequired();
							//Map<String,Object> p_extensions = parameter.getVendorExtensions();
							
							boolean isMultiValued = false;
							if (parameter instanceof SerializableParameter) {
								SerializableParameter serializable = (SerializableParameter)parameter;
								if (serializable.getType().equalsIgnoreCase("array")) {
									if (serializable.getCollectionFormat().equalsIgnoreCase("multi")) {
										isMultiValued = true;
									}
								}
							}
							
							RequestableHttpVariable httpVariable = isMultiValued ? 
																	new RequestableHttpMultiValuedVariable():
																	new RequestableHttpVariable();
							httpVariable.bNew = true;
							
							httpVariable.setName(p_name);
							httpVariable.setHttpName(p_name);
							httpVariable.setRequired(p_required);
							
							if (parameter instanceof QueryParameter || parameter instanceof PathParameter || parameter instanceof HeaderParameter) {
								httpVariable.setHttpMethod(HttpMethodType.GET.name());
								if (parameter instanceof HeaderParameter) {
									// overrides variable's name : will be treated as dynamic header
									httpVariable.setName(com.twinsoft.convertigo.engine.enums.Parameter.HttpHeader.getName() + p_name);
									httpVariable.setHttpName(""); // do not post on target server
								}
								if (parameter instanceof PathParameter) {
									httpVariable.setHttpName(""); // do not post on target server
								}
							}
							else if (parameter instanceof FormParameter || parameter instanceof BodyParameter) {
								httpVariable.setHttpMethod(HttpMethodType.POST.name());
								if (parameter instanceof FormParameter) {
									FormParameter formParameter = (FormParameter) parameter;
									if (formParameter.getType().equalsIgnoreCase("file")) {
										httpVariable.setDoFileUploadMode(DoFileUploadMode.multipartFormData);
									}
								}
								else if (parameter instanceof BodyParameter) {
									hasBodyVariable = true;
									// overrides variable's name for internal use
									httpVariable.setName(com.twinsoft.convertigo.engine.enums.Parameter.HttpBody.getName());
									
									// add internal __contentType variable
									/*RequestableHttpVariable ct = new RequestableHttpVariable();
									ct.setName(Parameter.HttpContentType.getName());
									ct.setHttpMethod(HttpMethodType.POST.name());
									ct.setValueOrNull(null);
									ct.bNew = true;
									transaction.addVariable(ct);*/
									
									BodyParameter bodyParameter = (BodyParameter)parameter;
									Model model = bodyParameter.getSchema();
									if (model != null) {
										
									}
								}
							}
							else {
								httpVariable.setHttpMethod("");
							}
							
							Object defaultValue = null;
							if (parameter instanceof AbstractSerializableParameter<?>) {
								defaultValue = ((AbstractSerializableParameter<?>)parameter).getDefaultValue();
							}
							if (defaultValue == null && parameter instanceof SerializableParameter) {
								SerializableParameter serializable = (SerializableParameter)parameter;
								if (serializable.getType().equalsIgnoreCase("array")) {
									Property items = serializable.getItems();
									try {
										Class<?> c = items.getClass();
										defaultValue = c.getMethod("getDefault").invoke(items);
									} catch (Exception e) {}
								}
							}
							if (defaultValue == null && p_required) {
								defaultValue = "";
							}
							httpVariable.setValueOrNull(defaultValue);
							
							if (p_description != null) {
								httpVariable.setDescription(p_description);
								httpVariable.setComment(p_description);
							}
							
							transaction.addVariable(httpVariable);
						}
						
						// Set Content-Type
						if (h_ContentType != null) {
							XMLVector<String> xmlv = new XMLVector<String>();
							xmlv.add(HeaderName.ContentType.value());
							xmlv.add(hasBodyVariable ? h_ContentType:MimeType.WwwForm.value());
				   			httpParameters.add(xmlv);
						}
						
						
						transaction.bNew =  true;
						transaction.setName(name);
						transaction.setComment(comment);
						transaction.setSubDir(subDir);
						transaction.setHttpVerb(httpMethodType);
						transaction.setHttpParameters(httpParameters);
						transaction.setHttpInfo(true);
						
						httpConnector.add(transaction);
						
					}
				}
			}
			
			return httpConnector;
		}
		catch (Throwable t) {
			Engine.logEngine.error("Unable to create connector", t);
			throw new Exception("Unable to create connector", t);
		}
	}
	
	@SuppressWarnings("unused")
	private static HttpConnector createRestConnector(JSONObject json) throws Exception {
		try {
			HttpConnector httpConnector = new HttpConnector();
			httpConnector.bNew = true;
			
			JSONObject info = json.getJSONObject("info");
			httpConnector.setName(StringUtils.normalize(info.getString("title")));
			
			String host = json.getString("host");
			int index = host.indexOf(":");
			String server = index == -1 ? host : host.substring(0, index);
			int port = index == -1 ? 0 : Integer.parseInt(host.substring(index+1, 10));
			httpConnector.setServer(server);
			httpConnector.setPort(port <= 0 ? 80:port);
			
			String basePath = json.getString("basePath");
			httpConnector.setBaseDir(basePath);
			
			JSONArray _consumes = new JSONArray();
			if (json.has("consumes")) {
				_consumes = json.getJSONArray("consumes");
			}
			
			JSONArray _produces = new JSONArray();
			if (json.has("produces")) {
				_produces = json.getJSONArray("produces");
			}

			Map<String, JSONObject> models = new HashMap<String, JSONObject>();
			JSONObject definitions = new JSONObject();
			if (json.has("definitions")) {
				definitions = json.getJSONObject("definitions");
				for (Iterator<String> i = GenericUtils.cast(definitions.keys()); i.hasNext(); ) {
					String key = i.next();
					JSONObject model = definitions.getJSONObject(key);
					models.put(key, model);
				}
			}
			
			JSONObject paths = json.getJSONObject("paths");
			for (Iterator<String> i1 = GenericUtils.cast(paths.keys()); i1.hasNext(); ) {
				String subDir = i1.next();
				JSONObject path = paths.getJSONObject(subDir);
				
				for (Iterator<String> i2 = GenericUtils.cast(path.keys()); i2.hasNext(); ) {
					String httpVerb = i2.next();
					JSONObject verb = path.getJSONObject(httpVerb);
					
					XMLVector<XMLVector<String>> httpParameters = new XMLVector<XMLVector<String>>();
					AbstractHttpTransaction transaction = new HttpTransaction();
					
					JSONArray consumes = verb.has("consumes") ? verb.getJSONArray("consumes"):_consumes;
					List<String> consumeList = new ArrayList<String>();
					for (int i=0; i<consumes.length(); i++) {
						consumeList.add(consumes.getString(i));
					}
					
					String h_ContentType = null;
					if (consumeList.contains(MimeType.Xml.value())) {
						h_ContentType = MimeType.Xml.value();
					}
					else if (consumeList.contains(MimeType.Json.value())) {
						h_ContentType = MimeType.Json.value();
					}
					else {
						h_ContentType = consumeList.size() > 0 ? 
								consumeList.get(0) : MimeType.WwwForm.value();
					}
					
					JSONArray produces = verb.has("produces") ? verb.getJSONArray("produces"):_produces;
					List<String> produceList = new ArrayList<String>();
					for (int i=0; i<produces.length(); i++) {
						produceList.add(produces.getString(i));
					}
					
					String h_Accept = null;
					if (produceList.contains(h_ContentType)) {
						h_Accept = h_ContentType;
					}
					else {
						if (produceList.contains(MimeType.Xml.value())) {
							h_Accept = MimeType.Xml.value();
						}
						else if (produceList.contains(MimeType.Json.value())) {
							h_Accept = MimeType.Json.value();
						}
					}
					
					if (h_Accept != null) {
						XMLVector<String> xmlv = new XMLVector<String>();
						xmlv.add("Accept");
						xmlv.add(h_Accept);
			   			httpParameters.add(xmlv);
			   			
						if (h_Accept.equals(MimeType.Xml.value())) {
							transaction = new XmlHttpTransaction();
							((XmlHttpTransaction)transaction).setXmlEncoding("UTF-8");
						}
						else if (h_Accept.equals(MimeType.Json.value())) {
							transaction = new JsonHttpTransaction();
							((JsonHttpTransaction)transaction).setIncludeDataType(false);
						}
						
					}
					
					if (h_ContentType != null) {
						XMLVector<String> xmlv = new XMLVector<String>();
						xmlv.add(HeaderName.ContentType.value());
						xmlv.add(h_ContentType);
			   			httpParameters.add(xmlv);
					}
					

					String operationId = "";
					if (verb.has("operationId")) {
						operationId = verb.getString("operationId");
					}
					
					String summary = "";
					if (verb.has("summary")) {
						summary = verb.getString("summary");
					}
					
					String description ="";
					if (verb.has("description")) {
						description = verb.getString("description");
					}
					
					String name = StringUtils.normalize(operationId);
					if (name.isEmpty()) {
						name = StringUtils.normalize(summary);
						if (name.isEmpty()) {
							name = "operation";
						}
					}
					
					String comment = summary;
					if (comment.isEmpty()) {
						comment = description;
					}
					
					JSONArray parameters = new JSONArray();
					if (verb.has("parameters")) {
						parameters = verb.getJSONArray("parameters");
						for (int i=0; i<parameters.length(); i++) {
							JSONObject parameter = (JSONObject) parameters.get(i);
							
							String type = "string";
							if (parameter.has("collectionFormat")) {
								type = parameter.getString("type");
							}
							
							String collectionFormat = "csv";
							if (parameter.has("collectionFormat")) {
								collectionFormat = parameter.getString("collectionFormat");
							}
							
							boolean isMultiValued = type.equalsIgnoreCase("array") && 
														collectionFormat.equals("multi");
							
							RequestableHttpVariable httpVariable = isMultiValued ? 
																	new RequestableHttpMultiValuedVariable():
																	new RequestableHttpVariable();
							httpVariable.bNew = true;
							
							httpVariable.setName(parameter.getString("name"));
							httpVariable.setHttpName(parameter.getString("name"));
							
							String in = parameter.getString("in");
							if (in.equals("query") || in.equals("path") || in.equals("header")) {
								httpVariable.setHttpMethod(HttpMethodType.GET.name());
								if (in.equals("header")) {
									// overrides variable's name : will be treated as dynamic header
									httpVariable.setName(com.twinsoft.convertigo.engine.enums.Parameter.HttpHeader.getName() + parameter.getString("name"));
									httpVariable.setHttpName(""); // do not post on target server
								}
							}
							else if (in.equals("formData") || in.equals("body")) {
								httpVariable.setHttpMethod(HttpMethodType.POST.name());
								if (in.equals("body")) {
									// overrides variable's name for internal use
									httpVariable.setName(com.twinsoft.convertigo.engine.enums.Parameter.HttpBody.getName());
									
									// add internal __contentType variable
									RequestableHttpVariable ct = new RequestableHttpVariable();
									ct.setName(com.twinsoft.convertigo.engine.enums.Parameter.HttpContentType.getName());
									ct.setHttpName(""); // do not post on target server
									ct.setHttpMethod(HttpMethodType.POST.name());
									ct.setValueOrNull(null);
									ct.bNew = true;
									transaction.addVariable(ct);
									
									//
									if (parameter.has("schema")) {
										//String schema = parameter.getString("schema");
									}
								}
							}
							else {
								httpVariable.setHttpMethod("");
							}
							
							Object defaultValue = null;
							if (parameter.has("default")) {
								defaultValue = parameter.get("default");
							}
							if (defaultValue == null && type.equalsIgnoreCase("array")) {
								JSONObject items = parameter.getJSONObject("items");
								if (items.has("default")) {
									defaultValue = items.get("default");
								}
							}
							httpVariable.setValueOrNull(defaultValue);
							
							if (parameter.has("description")) {
								httpVariable.setDescription(parameter.getString("description"));
							}
							
							transaction.addVariable(httpVariable);
						}
					}
					
					transaction.bNew =  true;
					transaction.setName(name);
					transaction.setComment(comment);
					transaction.setSubDir(subDir);
					transaction.setHttpVerb(HttpMethodType.valueOf(httpVerb.toUpperCase()));
					transaction.setHttpParameters(httpParameters);
					transaction.setHttpInfo(true);
					
					httpConnector.add(transaction);
				}
			}
			
			return httpConnector;
		}
		catch (Throwable t) {
			System.out.println(t);
			throw new Exception("Invalid Swagger format", t);
		}
	}
	
	public static Property getItems(DataType dataType) {
		if (DataType.String.equals(dataType))
			return new StringProperty();
		else if (DataType.Integer.equals(dataType))
			return new IntegerProperty();
		else if (DataType.Boolean.equals(dataType))
			return new BooleanProperty();
		else if (DataType.Number.equals(dataType))
			return new DoubleProperty();
		return null;
	}
	
	public static String getYamlDefinition(String requestUrl, Object object) throws JsonProcessingException {
		if (object instanceof String) {	// project name
			return prettyPrintYaml(parse(requestUrl, (String) object));
		}
		if (object instanceof UrlMapper) {	// urlmapper of project
			return prettyPrintYaml(parse(requestUrl, (UrlMapper) object));
		}
		if (object instanceof Collection<?>) { // all projects urlmapper
			Collection<UrlMapper> collection = GenericUtils.cast(object);
			return prettyPrintYaml(parse(requestUrl, collection));
		}
		return null;
	}

	public static String getJsonDefinition(String requestUrl, Object object) {
		if (object instanceof String) {	// project name
			return prettyPrintJson(parse(requestUrl, (String)object));
		}
		if (object instanceof UrlMapper) {
			return prettyPrintJson(parse(requestUrl, (UrlMapper)object));
		}
		if (object instanceof Collection<?>) {
			Collection<UrlMapper> collection = GenericUtils.cast(object);
			return prettyPrintJson(parse(requestUrl, collection));
		}
		return null;
	}
	
	public static String prettyPrintJson(Swagger swagger) {
		return Json.pretty(swagger);
	}
	
	public static String prettyPrintYaml(Swagger swagger) throws JsonProcessingException {
		return Yaml.pretty().writeValueAsString(swagger);
	}

	public static void testReadJson() {
		Swagger swagger = read("https://petstore.swagger.io/v2/swagger.json");
		if (swagger != null) {
			Json.prettyPrint(swagger);
			Yaml.prettyPrint(swagger);
		}		
	}
	
	public static void testReadYaml() {
		Swagger swagger = read("https://petstore.swagger.io/v2/swagger.yaml");
		if (swagger != null) {
			Json.prettyPrint(swagger);
			Yaml.prettyPrint(swagger);
		}		
	}
	
	public static void testReadPath() throws JsonProcessingException, IOException {
		String data = "{"
				+ "\"post\": { \"tags\": [\"pet\"], \"summary\": \"add a new pet to the store\", \"description\": \"\", \"operationid\": \"addpet\", \"consumes\": [\"application/json\", \"application/xml\"], \"produces\": [\"application/xml\", \"application/json\"], \"parameters\": [{ \"in\": \"body\", \"name\": \"body\", \"description\": \"pet object that needs to be added to the store\", \"required\": true, \"schema\": { \"$ref\": \"#/definitions/pet\" } }], \"responses\": { \"405\": { \"description\": \"invalid input\" } }, \"security\": [{ \"petstore_auth\": [\"write:pets\", \"read:pets\"] }] },"
				+ "\"put\": { \"tags\": [\"pet\"], \"summary\": \"update an existing pet\", \"description\": \"\", \"operationid\": \"updatepet\", \"consumes\": [\"application/json\", \"application/xml\"], \"produces\": [\"application/xml\", \"application/json\"], \"parameters\": [{ \"in\": \"body\", \"name\": \"body\", \"description\": \"pet object that needs to be added to the store\", \"required\": true, \"schema\": { \"$ref\": \"#/definitions/pet\" } }], \"responses\": { \"400\": { \"description\": \"invalid id supplied\" }, \"404\": { \"description\": \"pet not found\" }, \"405\": { \"description\": \"validation exception\" } }, \"security\": [{ \"petstore_auth\": [\"write:pets\", \"read:pets\"] }] }"
				+ "}";
		ObjectMapper mapper = Json.mapper();
		JsonNode pathNode = mapper.readTree(data);
		Path path = mapper.convertValue(pathNode, Path.class);
		Json.prettyPrint(path);
	}

/*
	public static void main(String[] args) {
		try {
			testReadJson();
			testReadYaml();
			testReadPath();
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}
*/
}
