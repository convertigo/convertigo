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

package com.twinsoft.convertigo.beans.steps;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.IContextMaintainer;
import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.beans.core.IVariableContainer;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.RequestableStep;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.sequences.GenericSequence;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.EngineStatistics;
import com.twinsoft.convertigo.engine.enums.MimeType;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.enums.Visibility;
import com.twinsoft.convertigo.engine.util.ProjectUtils;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.convertigo.engine.util.VersionUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class SequenceStep extends RequestableStep implements ITagsProperty{
	private static final long serialVersionUID = -8066934224685627694L;
	
	private transient String sequenceName;
	
	private String sourceSequence = "";
	
	// Specify if transaction's context should be inherited from mother sequence's one
	private boolean inheritTransactionCtx = false;
	
	public SequenceStep() {
		super();
	}

	public SequenceStep(boolean synchronous) {
		super();
	}

	@Override
	public SequenceStep clone() throws CloneNotSupportedException {
		SequenceStep clonedObject = (SequenceStep) super.clone();
		clonedObject.setSourceSequence(sourceSequence);
		return clonedObject;
	}

	@Override
	public SequenceStep copy() throws CloneNotSupportedException {
		SequenceStep copiedObject = (SequenceStep)super.copy();
		copiedObject.setSourceSequence(sourceSequence);
		return copiedObject;
	}

	@Override
	public String getStepNodeName() {
		return "sequence";
	}

	public String getSequenceName() {
		return sequenceName;
	}

	public boolean isInheritTransactionCtx() {
		return inheritTransactionCtx;
	}

	public void setInheritTransactionCtx(boolean inheritTransactionCtx) {
		this.inheritTransactionCtx = inheritTransactionCtx;
	}

	@Override
	protected void stepDone() {
		super.stepDone();
	}
	
	protected void prepareForRequestable(Context javascriptContext, Scriptable scope) throws MalformedURLException, EngineException {
		Sequence targetSequence = getTargetSequence();
		
		if (Engine.isStudioMode()) {
			if (targetSequence != null) {
				if (sequence.getOriginal().equals(targetSequence.getOriginal())) {
					throw new EngineException("Execution of recursive sequence is not allowed in Studio mode!");
				}
			}
		}
		
		String ctxName = getContextName(javascriptContext, scope);
		boolean useSequenceJSession = sequence.useSameJSessionForSteps();
		boolean maintainContext = useSequenceJSession && !ctxName.startsWith("Container-");
		boolean maintainSession = useSequenceJSession;
		
		boolean inheritContex = inheritTransactionCtx && sequence.equals(getTransactionContextMaintainer());
		String inheritedCxtName = sequence.getInheritedContextName();
		inheritedCxtName = (inheritContex && (inheritedCxtName == null)) ? sequence.getContextName():inheritedCxtName;
		inheritedCxtName = (inheritedCxtName == null) ? "":inheritedCxtName;
		
		if (isInternalInvoke()) {
	    	request.put(Parameter.Project.getName(), new String[] { projectName });
			request.put(Parameter.MotherSequenceContext.getName(),
					new String[] { sequence.context.contextID });
	    	request.put(Parameter.Sequence.getName(), new String[] { targetSequence.getName() });
	    	request.put(Parameter.Context.getName(), new String[] { sequence.addStepContextName(ctxName) });
	    	request.put(Parameter.SequenceInheritedTransactionContext.getName(), inheritContex ? new String[] { inheritedCxtName }:new String[] { "" });
	    	if (!maintainContext) request.put(Parameter.RemoveContext.getName(), new String[] { "" });
	    	if (!maintainSession) request.put(Parameter.RemoveSession.getName(), new String[] { "" });
			getPostQuery(scope);
		}
		else {
			targetUrl = EnginePropertiesManager.getProperty(PropertyName.APPLICATION_SERVER_CONVERTIGO_URL);
			targetUrl += "/projects/" + projectName + "/.xml?";
			
			URL url = new URL(targetUrl);
			String host = url.getHost();
			int port = url.getPort();
			
			Engine.logBeans.trace("(SequenceStep) Host: " + host + ":" + port);
			hostConfiguration.setHost(host, port);
			
			// Retrieves new HttpState!!
			// This will force a new http session on convertigo server : new context for sequence
	    	httpState = sequence.getNewHttpState();
			
			method = new PostMethod(targetUrl);
			method.setRequestHeader("Content-Type", MimeType.WwwForm.value());
	
			// Set transaction sessionId from context maintainer
			String sessionId = getTransactionSessionId();
			if (useSequenceJSession) {
				Engine.logBeans.trace("(SequenceStep) JSESSIONID required : " + sessionId);
				if (sessionId != null) {
					method.setRequestHeader("Cookie", "JSESSIONID="+sessionId+";");
					Engine.logBeans.trace("(SequenceStep) JSESSIONID used : " + sessionId);
				}
				else {
					Engine.logBeans.trace("(SequenceStep) JSESSIONID is null");
				}
			}
			else {
				if (sessionId != null) {
					method.addRequestHeader("Transaction-JSessionId",sessionId);
					Engine.logBeans.trace("(SequenceStep) Transaction JSESSIONID used : " + sessionId);
				}
				else {
					Engine.logBeans.trace("(SequenceStep) Transaction JSESSIONID is null");
				}
			}
			
			String postQuery = getPostQuery(scope);
			if (!maintainContext && (postQuery.indexOf(Parameter.RemoveContext.getName()) == -1))
				postQuery = addParamToPostQuery(Parameter.RemoveContext.getName(), "", postQuery);
			if (!maintainSession && (postQuery.indexOf(Parameter.RemoveSession.getName()) == -1))
				postQuery = addParamToPostQuery(Parameter.RemoveSession.getName(), "", postQuery);
			if (postQuery.indexOf(Parameter.SequenceInheritedTransactionContext.getName()) == -1)
				postQuery = addParamToPostQuery(Parameter.SequenceInheritedTransactionContext.getName(), inheritContex ? inheritedCxtName:"", postQuery);
			if (postQuery.indexOf(Parameter.Sequence.getName()) == -1)
				postQuery = addParamToPostQuery(Parameter.Sequence.getName(), targetSequence.getName(), postQuery);
			if (postQuery.indexOf(Parameter.Context.getName()) == -1)
				postQuery = addParamToPostQuery(Parameter.Context.getName(), sequence.addStepContextName(ctxName), postQuery);
			if (postQuery.indexOf(Parameter.MotherSequenceContext.getName()) == -1)
				postQuery = addParamToPostQuery(Parameter.MotherSequenceContext.getName(),
						sequence.context.contextID, postQuery);
			
			if (Engine.logBeans.isTraceEnabled())
				Engine.logBeans.trace("(SequenceStep) postQuery :" + Visibility.Logs.replaceVariables(getVariables(), postQuery));
			
			try {
				method.setRequestEntity(new StringRequestEntity(postQuery, null, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				throw new EngineException("Encoding error", e);
			}
		}
	}
	
	private String getContextName(Context javascriptContext, Scriptable scope) {
		boolean useSameJSessionForSteps = sequence.useSameJSessionForSteps();
		String ctxName = getStepContextName();
		
		// Auto context name
		if (ctxName.equals("")) {
			ctxName = useSameJSessionForSteps ? "error_"+priority:"default";
			IContextMaintainer transactionContextMaintainer = getTransactionContextMaintainer();
			if (transactionContextMaintainer != null) {
				ctxName = transactionContextMaintainer.getContextName();
				if (useSameJSessionForSteps) {
					// Sequence always requires new context
					ctxName += "-" + priority;
				}
			}
		}
		// Defined context name
		else {
			try {
				evaluate(javascriptContext, scope, ctxName, "ctxName", true);
			}
			catch (Exception e) {
				evaluated = null;
				Engine.logBeans.trace("(SequenceStep) " + e.getMessage());
			}
			ctxName = (evaluated != null) ? evaluated.toString():(useSameJSessionForSteps ? "error_"+priority:"default");
		}
		return ctxName;
	}
	
	private String getTransactionSessionId() {
		if (sequence.useSameJSessionForSteps()) {
			return sequence.getSessionId();
		}
		else {
			IContextMaintainer transactionContextMaintainer = getTransactionContextMaintainer();
			if (transactionContextMaintainer != null) {
				return transactionContextMaintainer.getTransactionSessionId();
			}
			return null;
		}
	}
	
	private void setTransactionSessionId(String sessionId) {
		if (sequence.useSameJSessionForSteps()) return;
		
		IContextMaintainer transactionContextMaintainer = getTransactionContextMaintainer();
		if (transactionContextMaintainer != null) {
			transactionContextMaintainer.setTransactionSessionId(sessionId);
		}
	}
	
	public void importVariableDefinition() throws EngineException {
		Sequence targetSequence = getTargetSequence();
		if (targetSequence != null && targetSequence instanceof GenericSequence) {
			importVariableDefinition(targetSequence);
		}
	}
	
	public Sequence getTargetSequence() throws EngineException {
		Project p = getTargetProject(projectName);
		if (p == null) {
			throw new EngineException("There is no project named \"" + projectName + "\" found.");
		}
		List<Sequence> v = p.getSequencesList();
		Sequence targetSequence = (sequenceName.equals("") ? (v.isEmpty() ? null: (Sequence)v.get(0)):p.getSequenceByName(sequenceName));
		return targetSequence;
	}
	
    protected byte[] executeMethod() throws IOException, URIException, MalformedURLException, EngineException {
		Header[] requestHeaders, responseHeaders = null;
		byte[] result = null;
		String contents = null;
		int statuscode = -1;
		
		if (sequence.isRunning()) {
			Engine.logBeans.debug("(SequenceStep) Executing method - "+ method.getName() +"("+ method.getPath()+")");
			
			String ts = sequence.context.statistics.start(EngineStatistics.EXECUTE_SEQUENCE_CALLS);
			
			try {
				requestHeaders = method.getRequestHeaders();
				if (Engine.logBeans.isTraceEnabled())
					Engine.logBeans.trace("(SequenceStep) Request headers :\n"+ Arrays.asList(requestHeaders).toString());
		
				statuscode = doExecuteMethod();
				
				Engine.logBeans.debug("(SequenceStep) Status: " + method.getStatusLine().toString());
				
	    		// stores transaction sessionId in context maintainer
				Header h = method.getResponseHeader("Transaction-JSessionId");
				if (h != null) setTransactionSessionId(h.getValue());
				
				responseHeaders = method.getResponseHeaders();
				if (Engine.logBeans.isTraceEnabled())
					Engine.logBeans.trace("(SequenceStep) Response headers:\n"+ Arrays.asList(responseHeaders).toString());
		
				if (statuscode != -1) {			
		            InputStream in = method.getResponseBodyAsStream();
		            if (in != null){
		                ByteArrayOutputStream bos = new ByteArrayOutputStream();
		                byte[] buf = new byte[1024];
		                int len;
		                while ((len = in.read(buf)) > 0) {
		                    bos.write(buf, 0, len);
		                }
		                result = bos.toByteArray();
		                in.close();
		                bos.close();
		            }
					
		            if (Engine.logBeans.isTraceEnabled()) {
		    			contents = new String((result != null) ? result:new byte[]{});
		    			Engine.logBeans.trace("(SequenceStep) Response content:\n"+ contents);
		    		}
		    		
				}
				if (statuscode >= 300) {
					throw new EngineException("(SequenceStep) HTTP response returned status :"+ ((method != null) ? method.getStatusLine().toString():String.valueOf(statuscode)));
				}
			}
			finally {
				if (method != null)
					method.releaseConnection();

				sequence.context.statistics.stop(ts, sequence.getCurrentChildStep() != 0);
			}
		}
		return result;
	}

	@Override
	public void configure(Element element) throws Exception {
        super.configure(element);
        
		String version = element.getAttribute("version");
        
		if (version == null) {
			String s = XMLUtils.prettyPrintDOM(element);
			EngineException ee = new EngineException("Unable to find version number for the database object \"" + getName() + "\".\nXML data: " + s);
			throw ee;
		}

        try {
            if (VersionUtils.compare(version, "6.0.3") < 0) {
				String projectName = (String) XMLUtils.findPropertyValue(element, "projectName");
				// Handle wrong project name
				if (projectName.equals("")) {
					projectName = "unknown_project";
				}
				
                String sequenceName = (String) XMLUtils.findPropertyValue(element, "sequenceName");
				// Handle wrong sequence name
				if (sequenceName.equals("")) {
					sequenceName = "unknown_sequence";
				}
				
                String sourceSequence = projectName + SequenceStep.SOURCE_SEPARATOR + sequenceName;
                
                setSourceSequence(sourceSequence);
                
                hasChanged = true;
                Engine.logBeans.warn("[SequenceStpe] The object \"" + getName() + "\" has been updated to version 6.0.3; source sequence: " + sourceSequence);
            }
        } catch(Exception e) {
            throw new EngineException("Unable to migrate the source definition for CallSequence step \"" + getName() + "\".", e);
        }
	}

	@Override
	public Element toXml(Document document) throws EngineException {
		Element element = super.toXml(document);
		
        // Storing the sequence WSDL type
        try {
            Element wsdlTypeElement = document.createElement("wsdltype");
            if (wsdlType != null) {
                CDATASection cDATASection = document.createCDATASection(wsdlType);
                wsdlTypeElement.appendChild(cDATASection);
                element.appendChild(wsdlTypeElement);
            }
        }
        catch(NullPointerException e) {
            // Silently ignore
        }
        
        return element;
	}

	@Override
	public String[] getTagsForProperty(String propertyName) {
		if (propertyName.equals("sourceSequence")) {
			List<String> sequencesList = new ArrayList<String>();
			
			try {
				List<String> projectNames = Engine.theApp.databaseObjectsManager.getAllProjectNamesList();
				for (String projectName : projectNames) {
					Project project = Engine.theApp.databaseObjectsManager.getProjectByName(projectName);
					List<Sequence> sequences = project.getSequencesList();
					
					for (Sequence sequence : sequences) {
						sequencesList.add(projectName + SOURCE_SEPARATOR + sequence.getName());
					}
				}
			} catch (EngineException e) {
				// Just ignore, should never happen
			}
			
			return sequencesList.toArray(new String[] {});
		}
		return super.getTagsForProperty(propertyName);
	}
	
	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.beans.core.RequestableStep#getSpecificLabel()
	 */
	@Override
	protected String getSpecificLabel() throws EngineException {
		String label = super.getSpecificLabel();
		if (label.equals("")) {// normal case
			// Check for project
			if (projectName.equals("")) {
				label = "! broken project !";
			}
			else {
				// Check for project
				try {
					if (ProjectUtils.existProjectSchemaReference(getProject(), projectName)) {
						Project p = getTargetProject(projectName);
						if (p == null) {
							label = "! broken project !";
						}
						else {
							//Check for sequence
							try {
								p.getSequenceByName(sequenceName);
							} catch (EngineException e) {
								label = "! broken sequence !";
							}
						}
					}
					else {
						label = "! broken reference !";
					}
				}
				catch (Exception e) {
					label = "! broken project !";
				}
			}
		}
		return label;
	}
	
	@Override
	public String toString() {
		String label = "";
		try {
			label += getLabel();
		} catch (EngineException e) { }
		return StringUtils.normalize("Call_" + getSourceSequence()) + (label.equals("") ? "" : " ") + label;
	}
	
	public String getSourceSequence() {
		return sourceSequence;
	}

	public void setSourceSequence(String sourceSequence) {
		this.sourceSequence = sourceSequence;
		StringTokenizer st = new StringTokenizer(sourceSequence, SequenceStep.SOURCE_SEPARATOR);
		try {
			projectName = st.nextToken();
			sequenceName = st.nextToken();
		}
		catch (Exception e) {}
	}
	
	protected String getRequestableName() {
		return sequenceName;
	}

	@Override
	protected IVariableContainer getTargetVariableContainer() throws EngineException {
		return getTargetSequence();
	}
}
