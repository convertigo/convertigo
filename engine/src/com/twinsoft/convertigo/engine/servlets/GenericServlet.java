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

package com.twinsoft.convertigo.engine.servlets;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.soap.SOAPMessage;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.w3c.dom.Document;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.AttachmentManager.AttachmentDetails;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.enums.HeaderName;
import com.twinsoft.convertigo.engine.enums.MimeType;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.enums.SessionAttribute;
import com.twinsoft.convertigo.engine.requesters.Requester;
import com.twinsoft.convertigo.engine.requesters.ServletRequester;
import com.twinsoft.convertigo.engine.requesters.WebServiceServletRequester;
import com.twinsoft.convertigo.engine.util.FileUtils;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.HttpServletRequestTwsWrapper;
import com.twinsoft.convertigo.engine.util.HttpUtils;
import com.twinsoft.convertigo.engine.util.SOAPUtils;
import com.twinsoft.convertigo.engine.util.ServletUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public abstract class GenericServlet extends HttpServlet {

	private static final long serialVersionUID = -6386155197912471410L;

	public GenericServlet() {
	}

	private void handleStaticData(HttpServletRequest request, HttpServletResponse response) {
		String resourceUri = request.getServletPath();
		Engine.logContext.debug("Serving static ressource: " + resourceUri);
		HttpUtils.applyCorsHeaders(request, response);

		// TODO: enhance to support content types according to file extension
		if (resourceUri.endsWith(".xml") || resourceUri.endsWith(".cxml") || resourceUri.endsWith(".pxml"))
			response.setContentType(MimeType.TextXml.value());
		else
			response.setContentType(MimeType.Html.value());

		try {
			InputStream is = getServletContext().getResourceAsStream(resourceUri);
			if (is == null) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "Static resource " + resourceUri
						+ " not found");
				return;
			}

			byte array[] = new byte[4096];

			OutputStream os = response.getOutputStream();
			while (is.available() != 0) {
				int nb = is.read(array);
				os.write(array, 0, nb);
			}
			os.flush();

		} catch (IOException e) {
			Engine.logContext.trace("Error serving static resource: " + resourceUri);
		}
	}

	private static String getServletBaseUrl(HttpServletRequest request) {
		String base = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
		String requestURI = request.getRequestURI();
		int i = requestURI.lastIndexOf('/');
		return base + requestURI.substring(0, i);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doRequest(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doRequest(request, response);
	}

	@Override
	protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doRequest(request, response);
	}

	@Override
	protected void doHead(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doRequest(request, response);
	}

	@Override
	protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String corsOrigin = HttpUtils.applyCorsHeaders(request, response);
		if (corsOrigin != null) {
			Engine.logEngine.trace("Add CORS header for OPTIONS: " + corsOrigin);
		}
		response.setStatus(HttpServletResponse.SC_NO_CONTENT);
	}

	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doRequest(request, response);
	}

	@Override
	protected void doTrace(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doRequest(request, response);
	}

	protected void doRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.setAttribute("response", response);
		com.twinsoft.convertigo.engine.util.HttpUtils.applyCorsHeaders(request, response);
		HttpServletRequestTwsWrapper wrapped_request = new HttpServletRequestTwsWrapper(request);
		request = wrapped_request;

		String baseUrl = getServletBaseUrl(request);
		boolean isProject;
		
		if ((isProject = baseUrl.contains("/projects/") || baseUrl.contains("/system/")) || baseUrl.contains("/webclipper/")) {
			long t0 = System.currentTimeMillis();
			try {
				if (EnginePropertiesManager.getPropertyAsBoolean(PropertyName.XSRF_API)) {
					HttpUtils.checkXSRF(request, response);
				}
				String encoded = request.getParameter(Parameter.RsaEncoded.getName());
				if (encoded != null) {
					String query = Engine.theApp.rsaManager.decrypt(encoded, request.getSession());
					wrapped_request.clearParameters();
					wrapped_request.addQuery(query);
				}
				
				if (isProject && request.getMethod().equalsIgnoreCase("OPTIONS") && Engine.isStarted) {
					Project project = null;
					String projectName = request.getParameter(Parameter.Project.getName());
					if (projectName == null) {
						projectName = request.getRequestURI().replaceFirst(".*/projects/(.*?)/.*", "$1");
					}
					
					if (!projectName.contains("/")) {
						try {
							project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName);
						} catch (Exception e) { }
					}
					
					if (project == null) {
						response.setStatus(HttpServletResponse.SC_NOT_FOUND);
						return;
					}
					
					HttpUtils.applyFilterCorsHeaders(request, response, project.getCorsOrigin());
					
					response.setStatus(HttpServletResponse.SC_NO_CONTENT);
					return;
				}
				
				Object result = processRequest(request);

				response.addHeader("Expires", "-1");

				if (getCacheControl(request).equals("false")) {
					HeaderName.CacheControl.addHeader(response,
							"no-store, no-cache, must-revalidate, post-check=0, pre-check=0");
				}
				
				HttpUtils.applyCorsHeaders(request, response);
				
				/**
				 * Disabled since #253 : Too much HTML Connector cookies in
				 * response header make a tomcat exception
				 * http://sourceus.twinsoft.fr/ticket/253 cookies must be in xml
				 * if wanted, not in headers
				 * 
				 * Vector cookies = (Vector)
				 * request.getAttribute("convertigo.cookies"); for (int i=0;
				 * i<cookies.size(); i++) { String sCookie =
				 * (String)cookies.elementAt(i);
				 * response.addHeader("Set-Cookie", sCookie);
				 * Engine.logContext.trace("[GenericServlet] Set-Cookie: " +
				 * sCookie); }
				 */

				String trSessionId = (String) request.getAttribute("sequence.transaction.sessionid");
				if ((trSessionId != null) && (!trSessionId.equals(""))) {
					response.setHeader("Transaction-JSessionId", trSessionId);
				}
				
				String requested_content_type = request.getParameter(Parameter.ContentType.getName());
				String content_type = getContentType(request);
				if (requested_content_type != null && !requested_content_type.equals(content_type)) {
					Engine.logEngine.debug("(GenericServlet) Override Content-Type requested to change : " + content_type + " to " + requested_content_type);
					content_type = requested_content_type;
				} else {
					requested_content_type = null;
				}
				
				response.setContentType(content_type);
				if (content_type.startsWith("text")) {
					String charset = (String) request.getAttribute("convertigo.charset");
					if (charset != null && charset.length() > 0) {
						response.setCharacterEncoding(charset);
					}
				}
				
				try {

					if (result != null) {

						Boolean b = (Boolean) request.getAttribute("convertigo.isErrorDocument");
						if (b.booleanValue()) {
							Requester requester = getRequester();
							boolean bThrowHTTP500 = false;
							
							if (requester instanceof WebServiceServletRequester) {
								bThrowHTTP500 = Boolean.parseBoolean(EnginePropertiesManager
										.getProperty(EnginePropertiesManager.PropertyName.THROW_HTTP_500_SOAP_FAULT));
							}
							else if (requester instanceof ServletRequester) {
								bThrowHTTP500 = Boolean.parseBoolean(EnginePropertiesManager
										.getProperty(EnginePropertiesManager.PropertyName.THROW_HTTP_500));
							}

							if (bThrowHTTP500) {
								response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
								Engine.logEngine.debug("(GenericServlet) Requested HTTP 500 status code");
							}
						}
						else {
							applyCustomStatus(request, response);
						}

						if (result instanceof AttachmentDetails) {
							AttachmentDetails attachment = (AttachmentDetails) result;
							byte[] data = attachment.getData();
							String contentType = attachment.getContentType();
							
							if (requested_content_type != null) {
								contentType = requested_content_type;
							}
							
							String name = attachment.getName();

							HeaderName.ContentType.setHeader(response, contentType);
							HeaderName.ContentLength.setHeader(response, "" + data.length);
							HeaderName.ContentDisposition.setHeader(response, "attachment; filename=\"" + name + "\"");
							
							applyCustomHeaders(request, response);
							
							OutputStream out = response.getOutputStream();
							out.write(data);
							out.flush();
						} else if (result instanceof byte[]) {
							if (requested_content_type != null) {
								response.setContentType(requested_content_type);
							} else {
								response.setContentType(getContentType(request));
								response.setCharacterEncoding((String) request.getAttribute("convertigo.charset"));
							}
							HeaderName.ContentLength.addHeader(response, "" + ((byte[]) result).length);
							
							applyCustomHeaders(request, response);

							OutputStream out = response.getOutputStream();
							out.write((byte[]) result);
							out.flush();
						} else {
							String sResult = "";
							if (result instanceof String) {
								sResult = (String) result;
							} else if (result instanceof Document) {
								sResult = XMLUtils.prettyPrintDOM((Document) result);
							} else if (result instanceof SOAPMessage){
								sResult = SOAPUtils.toString((SOAPMessage) result, (String) request.getAttribute("convertigo.charset"));
							}
							
							applyCustomHeaders(request, response);

							Writer writer = response.getWriter();
							writer.write(sResult);
							writer.flush();
						}
					} else {
						applyCustomHeaders(request, response);
						
						response.setStatus(HttpServletResponse.SC_NO_CONTENT);
					}
				} catch (IOException e) {
					// The connection has probably been reset by peer
					Engine.logContext
							.warn("[GenericServlet] The connection has probably been reset by peer (IOException): "
									+ e.getMessage());
				} finally {
					onFinally(request);
				}
			} catch (Exception e) {
				Engine.logContext.error("Unable to process the request!", e);
				processException(request, response, e);
			}
			finally {
				long t1 = System.currentTimeMillis();
				Engine.theApp.pluginsManager.fireHttpServletRequestEnd(request, t0, t1);
			}
		} else {
			// Not a valid Convertigo invocation URL, use retrieve as static
			// resource
			handleStaticData(request, response);
			return;
		}
	}

	void onFinally(HttpServletRequest request) {
		// Supervision mode
		String supervision = request.getParameter(Parameter.Supervision.getName());
		if (supervision != null) {
			Engine.logContext
					.debug("[GenericServlet] Supervision mode => invalidating HTTP session in 30s.");
			removeSession(request, 30);
		}

		// Removes context and session when finished
		// Note: case of context.requireEndOfContext has been set in
		// scope
		if (request.getAttribute("convertigo.requireEndOfContext") != null) {
			removeContext(request);
			removeSession(request, 1);
		}

		// Removes context when finished
		String removeContextParam = request.getParameter(Parameter.RemoveContext.getName());
		if (removeContextParam == null) {
			// case of a mother sequence (context is removed by
			// default)
			Boolean removeContextAttr = (Boolean) request
					.getAttribute("convertigo.context.removalRequired");
			if ((removeContextAttr != null) && removeContextAttr.booleanValue()) {
				removeContext(request);
			}
		} else {
			// other cases (remove context if exist __removeContext
			// or __removeContext=true/false)
			if (!("false".equals(removeContextParam))) {
				removeContext(request);
			}
		}

		// Removes session when finished
		String removeSessionParam = request.getParameter(Parameter.RemoveSession.getName());
		if (removeSessionParam != null) {
			// __removeSession or __removeSession=true/false
			// or __removeSession=xx (where xx is a number of seconds)
			if (!("false".equals(removeSessionParam))) {
				int interval = 1;
				try {
					interval = Integer.parseInt(removeSessionParam, 10);
				}
				catch (Exception e) {}
				removeSession(request, interval);
			}
		}
	}
	
	private void removeContext(HttpServletRequest request) {
		if (Engine.isEngineMode()) {
			Context context = (Context) request.getAttribute("convertigo.context");
			if (context != null) {
				Engine.logContext.debug("[GenericServlet] End of context " + context.contextID
						+ " (" + context + ") required => removing context");
				Engine.theApp.contextManager.remove(context);
			}
		}
		request.removeAttribute("convertigo.context");
	}

	protected void removeSession(HttpServletRequest request, int interval) {
		if (Engine.isEngineMode()) {
			Engine.logContext.debug("[GenericServlet] End of session required => try to invalidate session");
			try {
				HttpSession httpSession = request.getSession();
				boolean isAdminSession = Engine.authenticatedSessionManager.isAuthenticated(httpSession);
				if (!isAdminSession && Engine.theApp.contextManager.isSessionEmtpy(httpSession)) {
					Engine.logContext
							.debug("[GenericServlet] The owner HTTP session is empty => invalidating HTTP session in "
									+ interval + "s.");
					httpSession.setMaxInactiveInterval(interval);
				}
			} catch (Exception e) {
				Engine.logContext.debug("[GenericServlet] End of session required => failed to get the session: " + e);
			}
		}
	}

	public void processException(HttpServletRequest request, HttpServletResponse response, Exception e)
			throws ServletException {
		boolean hide_error = EnginePropertiesManager.getProperty( PropertyName.HIDING_ERROR_INFORMATION ).equals( "true" );
		boolean bThrowHTTP500 = Boolean.parseBoolean(EnginePropertiesManager
				.getProperty(EnginePropertiesManager.PropertyName.THROW_HTTP_500));

		HttpUtils.applyCorsHeaders(request, response);
		Engine.logEngine.error("Unexpected exception", e);

		if (bThrowHTTP500) {
			if(hide_error) 
				throw new ServletException();
			else
				throw new ServletException(e);
		} else {
			try {
				HeaderName.XConvertigoException.addHeader(response, hide_error ? "" : e.getClass().getName());
				response.setContentType(MimeType.Plain.value());
				PrintWriter out = response.getWriter();
				out.println("Convertigo error:" + (hide_error ? "" : e.getMessage()));
			} catch (IOException e1) {
				Engine.logEngine.error("Unexpected exception", e1);
				if (hide_error) 
					throw new ServletException();
				else
					throw new ServletException(e);
			}
		}
	}

	public Object processRequest(HttpServletRequest request) throws Exception {
		HttpServletRequestTwsWrapper twsRequest = request instanceof HttpServletRequestTwsWrapper ? (HttpServletRequestTwsWrapper) request : null;
		File temporaryFile = null;

		try {
			// Check multipart request
			if (ServletFileUpload.isMultipartContent(request)) {
				Engine.logContext.debug("(ServletRequester.initContext) Multipart resquest");
	
				// Create a factory for disk-based file items
				DiskFileItemFactory factory = new DiskFileItemFactory();
	
				// Set factory constraints
				factory.setSizeThreshold(1000);
	
				temporaryFile = File.createTempFile("c8o-multipart-files", ".tmp");
				int cptFile = 0;
				temporaryFile.delete();
				temporaryFile.mkdirs();
				factory.setRepository(temporaryFile);
				Engine.logContext.debug("(ServletRequester.initContext) Temporary folder for upload is : " + temporaryFile.getAbsolutePath());
	
				// Create a new file upload handler
				ServletFileUpload upload = new ServletFileUpload(factory);
				
				// Set overall request size constraint
				upload.setSizeMax(EnginePropertiesManager.getPropertyAsLong(PropertyName.FILE_UPLOAD_MAX_REQUEST_SIZE));
				upload.setFileSizeMax(EnginePropertiesManager.getPropertyAsLong(PropertyName.FILE_UPLOAD_MAX_FILE_SIZE));
	
				// Parse the request
				List<FileItem> items = GenericUtils.cast(upload.parseRequest(request));
	
				for (FileItem fileItem : items) {
					String parameterName = fileItem.getFieldName();
					String parameterValue;
					if (fileItem.isFormField()) {
						String ct = fileItem.getContentType();
						parameterValue = ct != null && ct.contains("charset=") ? fileItem.getString() : fileItem.getString(StandardCharsets.UTF_8.name());
						Engine.logContext.trace("(ServletRequester.initContext) Value for field '" + parameterName + "' : " + parameterValue);
					} else {
						String name = fileItem.getName().replaceFirst("^.*(?:\\\\|/)(.*?)$", "$1");
						if (name.length() > 0) {
							File wDir = new File(temporaryFile, "" + (++cptFile));
							wDir.mkdirs();
							File wFile = new File(wDir, name);
							fileItem.write(wFile);
							fileItem.delete();
							parameterValue = wFile.getAbsolutePath();
							Engine.logContext.debug("(ServletRequester.initContext) Temporary uploaded file for field '" + parameterName + "' : " + parameterValue);
						} else {
							Engine.logContext.debug("(ServletRequester.initContext) No temporary uploaded file for field '" + parameterName + "', empty name");
							parameterValue = "";
						}
					}
	
					if (twsRequest != null) {
						twsRequest.addParameter(parameterName, parameterValue);
					}
				}
			}
			
			Requester requester = getRequester();
			request.setAttribute("convertigo.requester", requester);
	
			Object result = requester.processRequest(request);
	
			processRequestEnd(request, requester);
			
			return result;
		} finally {
			if (temporaryFile != null) {
				try {
					Engine.logEngine.debug("(GenericServlet) Removing the temporary file : " + temporaryFile.getAbsolutePath());
					FileUtils.deleteDirectory(temporaryFile);
				} catch (IOException e) {
					Engine.logEngine.warn("(GenericServlet) Failed to delete: " + temporaryFile + " [" + e.getMessage() + "]");
				}
			}
		}
	}
	
	void processRequestEnd(HttpServletRequest request, Requester requester) {
		Context context = requester.context;
		
		request.setAttribute("convertigo.cookies", context.getCookieStrings());
		
		String trSessionId = context.getSequenceTransactionSessionId();
		if (trSessionId != null) {
			request.setAttribute("sequence.transaction.sessionid", trSessionId);
		}
		
		boolean isNew = true;
		HttpSession session = request.getSession(false);
		if (session != null) {
			if (SessionAttribute.isNew.has(session)) {
				isNew = false;
			} else {
				SessionAttribute.isNew.set(session, true);
			}
		}
		
		if (requester.context.requireEndOfContext || (isNew && context.isErrorDocument) || context.project == null) {
			// request.setAttribute("convertigo.requireEndOfContext",
			// requester);
			request.setAttribute("convertigo.requireEndOfContext", Boolean.TRUE);
		}

		if (request.getAttribute("convertigo.contentType") == null) { // if
			// contentType
			// set by
			// webclipper
			// servlet
			// (#320)
			request.setAttribute("convertigo.contentType", context.contentType);
		}
		
		request.setAttribute("convertigo.cacheControl", context.cacheControl);
		request.setAttribute("convertigo.context", context);
		request.setAttribute("convertigo.isErrorDocument", Boolean.valueOf(context.isErrorDocument));
		request.setAttribute("convertigo.context.removalRequired", Boolean.valueOf(context.removalRequired()));
		if (request.getAttribute("convertigo.charset") == null) {
			request.setAttribute("convertigo.charset", "UTF-8");
		}
	}
	
	public abstract Requester getRequester();

	public String getContentType(HttpServletRequest request) {
		String contentType = (String) request.getAttribute("convertigo.contentType");
		if (contentType == null)
			contentType = getDefaultContentType();
		return contentType;
	}

	public String getCacheControl(HttpServletRequest request) {
		String cacheControl = (String) request.getAttribute("convertigo.cacheControl");
		if (cacheControl == null)
			cacheControl = "false";
		return cacheControl;
	}

	public String getDefaultContentType() {
		return MimeType.Html.value();
	}

	public abstract String getDocumentExtension();
	
	private void applyCustomHeaders(HttpServletRequest request, HttpServletResponse response) {
		ServletUtils.applyCustomHeaders(request, response);
	}
	private void applyCustomStatus(HttpServletRequest request, HttpServletResponse response) {
		ServletUtils.applyCustomStatus(request, response);
	}
}