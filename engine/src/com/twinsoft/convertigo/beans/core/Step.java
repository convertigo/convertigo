/*
 * Copyright (c) 2001-2021 Convertigo SA.
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

package com.twinsoft.convertigo.beans.core;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpState;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAnnotated;
import org.apache.ws.commons.schema.XmlSchemaAnnotation;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaDocumentation;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.constants.Constants;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.common.XmlQName;
import com.twinsoft.convertigo.beans.core.DatabaseObject.DboCategoryInfo;
import com.twinsoft.convertigo.beans.steps.AttributeStep;
import com.twinsoft.convertigo.beans.steps.ElementStep;
import com.twinsoft.convertigo.beans.steps.IThenElseContainer;
import com.twinsoft.convertigo.beans.steps.IfExistStep;
import com.twinsoft.convertigo.beans.steps.IfExistThenElseStep;
import com.twinsoft.convertigo.beans.steps.IfFileExistStep;
import com.twinsoft.convertigo.beans.steps.IfFileExistThenElseStep;
import com.twinsoft.convertigo.beans.steps.IfStep;
import com.twinsoft.convertigo.beans.steps.IfThenElseStep;
import com.twinsoft.convertigo.beans.steps.IsInStep;
import com.twinsoft.convertigo.beans.steps.IsInThenElseStep;
import com.twinsoft.convertigo.beans.steps.SequenceStep;
import com.twinsoft.convertigo.beans.steps.SimpleSourceStep;
import com.twinsoft.convertigo.beans.steps.SmartType;
import com.twinsoft.convertigo.beans.steps.SourceStep;
import com.twinsoft.convertigo.beans.steps.TransactionStep;
import com.twinsoft.convertigo.beans.steps.XMLAttributeStep;
import com.twinsoft.convertigo.beans.steps.XMLConcatStep;
import com.twinsoft.convertigo.beans.steps.XMLElementStep;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineEvent;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EngineStatistics;
import com.twinsoft.convertigo.engine.enums.FolderType;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.RhinoUtils;
import com.twinsoft.convertigo.engine.util.TwsCachedXPathAPI;
import com.twinsoft.convertigo.engine.util.VersionUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;


/**
 * The Step class is the base class for all steps.
 */
@DboCategoryInfo(
		getCategoryId = "Step",
		getCategoryName = "Step",
		getIconClassCSS = "convertigo-action-newStep"
	)
public abstract class Step extends DatabaseObject implements StepListener, ISheetContainer, ITagsProperty, ISchemaGenerator, IEnableAble {
	private static final long serialVersionUID = 1600450851360946365L;
	private String schemaDataType = "xsd:string"; // since beans version 5.0.2
	
    public static String loopSeparator = "--";
    
    public static final String NODE_USERDATA_OUTPUT = "step_output";
    
    private boolean isEnabled = true;
    private boolean output = false;
    private XmlQName xmlComplexTypeAffectation = new XmlQName();
    private XmlQName xmlSimpleTypeAffectation = new XmlQName(Constants.XSD_STRING);
    private XmlQName xmlElementRefAffectation = new XmlQName();
    
	transient protected boolean xml = false;
    transient protected List<Sheet> vSheets = new LinkedList<Sheet>();
	transient protected Map<Long, String> executedSteps = null;
	transient protected TwsCachedXPathAPI xpathApi = null;
	transient protected HttpState httpState = null;
	transient protected boolean stepDone = false;
	transient protected Sequence sequence = null;
	transient protected String executeTimeID = "";
	transient private boolean inError = false;
	transient private int cloneNumber = 0;
	
	transient public IContextMaintainer transactionContextMaintainer = null;
	
	public Step() {
        super();
		databaseType = "Step";
		
		// Set priority to creation time since version 4.0.1
		this.priority = getNewOrderValue();
	}
    
	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.beans.core.DatabaseObject#clone()
	 */
	@Override
	public Step clone() throws CloneNotSupportedException {
		Step clonedObject = (Step) super.clone();
		clonedObject.executeTimeID = "";
		clonedObject.cloneNumber = ++cloneNumber;
		clonedObject.transactionContextMaintainer = null;
		clonedObject.executedSteps = null;
		clonedObject.httpState = null;
		clonedObject.xpathApi = null;
		clonedObject.inError = false;
		clonedObject.stepDone = false;
		clonedObject.sequence = null;
		clonedObject.vSheets = new LinkedList<Sheet>();
		return clonedObject;
	}
    
	public Object copy() throws CloneNotSupportedException {
		Step copiedObject = (Step)clone();
		copiedObject.executedSteps = new HashMap<Long, String>(100);
		copiedObject.sequence = getSequence();
		copiedObject.vSheets = vSheets;
		copiedObject.isSubLoaded = isSubLoaded;
		return copiedObject;
	}

	@Override
	public void preconfigure(Element element) throws Exception {
		super.preconfigure(element);
		
		String version = element.getAttribute("version");
		if (VersionUtils.compare(version, "7.5.0") < 0) {
			NodeList properties = element.getElementsByTagName("property");
			
			Element propName = (Element) XMLUtils.findNodeByAttributeValue(properties, "name", "isEnable");
			if (propName != null) {
				propName.setAttribute("name", "isEnabled");
				hasChanged = true;
			}
			
			Engine.logBeans.warn("[Step] The object \"" + getName() + "\" has been updated to version 7.5.0 (property \"isEnable\" changed to \"isEnabled\")");
		}
	}
	
	protected void cleanCopy() {
		//System.out.println("Clean copy of step " + name + "("+executeTimeID+")");
		httpState = null;
		xpathApi = null;
		parent = null;
		sequence = null;
		transactionContextMaintainer = null;
		vSheets = null; // ! Do not clear()!
		if (executedSteps != null) {
			executedSteps.clear();
			executedSteps = null;
		}
	}
	
	public boolean isSynchronous() {
		return true;
	}
	
	public boolean isLoop() {
		return false;
	}
	
	public IContextMaintainer getTransactionContextMaintainer() {
		return transactionContextMaintainer;
	}

	protected String encodeValue(String value) {
		String s = value;
		if (s != null) {
			s = s.replaceAll("%", "%25");
			s = s.replaceAll("&", "%26");
			s = s.replaceAll("\\+", "%2B");
			s = s.replaceAll("\u0092", "%27");
		}
		return s;
	}
	
	protected TwsCachedXPathAPI getXPathAPI() {
		if (xpathApi == null) {
			if (Engine.logBeans.isTraceEnabled())
				Engine.logBeans.trace("[Step] Step needed to retrieve new TwsCachedXPathAPI");
			return xpathApi = new TwsCachedXPathAPI(getProject());
		}
		return xpathApi;
	}
	
	@SuppressWarnings("deprecation")
	@Override
    protected void finalize() throws Throwable {
		super.finalize();
	}

    /**
     * Get order for quick sort.
     */
    @Override
    public Object getOrderedValue() {
    	return priority;
    }    

	public Sequence getParentSequence() {
		Sequence sequence = null;
    	while (parent instanceof Step)
    		return ((Step)parent).getParentSequence();
    	if (parent instanceof Sequence)
    		sequence = (Sequence)parent;
    	return sequence;
    }
    
	public Sequence getSequence() {
		if (sequence == null) {
			sequence = getParentSequence();
		}
		return sequence;
	}
	
	public void setSequence(Sequence sequence) {
		this.sequence = sequence;
	}
	
	public boolean isXml() {
		return xml;
	}
	
	public boolean isPickable() {
		return isXml() && isEnabled();
	}
	
	public boolean isXmlOrOutput() {
		return isXml() || isOutput();
	}

	public boolean hasXmlParent() {
		try {
			Step parentStep = (Step)parent;
			while (parentStep != null) {
				if (parentStep.isXml())
					return true;
				parentStep = (Step)parentStep.parent;
			}
		}
		catch (Exception e) {}
		return false;
	}
	
	protected void reset() throws EngineException {
		stepDone = false;
	}
	
	public boolean workOnSource() {
		if (this instanceof IStepSourceContainer) {
			return !((IStepSourceContainer)this).getSourceDefinition().isEmpty();
		}
		else if (this instanceof IStepSourcesContainer) {
			return !((IStepSourcesContainer)this).getSourcesDefinition().isEmpty();
		}
		else if (this instanceof IStepSmartTypeContainer) {
			for (SmartType smartType: ((IStepSmartTypeContainer)this).getSmartTypes()) {
				if (smartType.isUseSource()) {
					return true;
				}
			}
		}
		return false;
		
	}
	
	public boolean canWorkOnSource() {
		return this instanceof IStepSourceContainer ||
				this instanceof IStepSourcesContainer ||
				this instanceof IStepSmartTypeContainer;
	}
	
	protected StepSource getSource() {
		if (this instanceof IStepSourceContainer) {
			return new StepSource(this, ((IStepSourceContainer)this).getSourceDefinition());
		}
		return null;
	}
	
	public Set<StepSource> getSources() {
		Set<StepSource> stepSources = new HashSet<StepSource>();
		if (this instanceof IStepSourceContainer) {
			stepSources.add(getSource());
		}
		else if (this instanceof IStepSourcesContainer) {
			for (XMLVector<Object> row: ((IStepSourcesContainer)this).getSourcesDefinition()) {
				XMLVector<String> sourceDefinition = GenericUtils.cast(row.get(1));
				stepSources.add(new StepSource(this, sourceDefinition));
			}
		}
		else if (this instanceof IStepSmartTypeContainer) {
			for (SmartType smartType: ((IStepSmartTypeContainer)this).getSmartTypes()) {
				if (smartType.isUseSource()) {
					stepSources.add(new StepSource(this, smartType.getSourceDefinition()));
				}
			}
		}
		return stepSources;
	}
	
	public Node getStepNode() throws EngineException {
		Node stepNode = createStepNode();
		return stepNode;
	}
	
	protected String getJSessionID() {
		String jSessionID = null;
		if (httpState != null) {
			Cookie[] httpCookies = httpState.getCookies();
			int len = httpCookies.length;
			Cookie cookie = null;
			
			for (int i=0; i<len; i++) {
				cookie = httpCookies[i];
				String cookieName = cookie.getName();
				String cookieValue = cookie.getValue();
				if (cookieName.equalsIgnoreCase("JSESSIONID")) {
					jSessionID = cookieValue;
					break;
				}
			}
		}
		return jSessionID;
	}
	
	protected Node createStepNode() throws EngineException {
		Document doc = getOutputDocument();
		Element stepNode = doc.createElement(getStepNodeName());
		stepNode.setUserData(NODE_USERDATA_OUTPUT, String.valueOf(isOutput()), null);
		
		if (!inError()) {
			createStepNodeValue(doc, stepNode);
		}
		return stepNode;
	}

	protected void createStepNodeValue(Document doc, Element stepNode) throws EngineException {
		//does nothing
	}

	public Document getOutputDocument() {
		return sequence.context.outputDocument;
	}
	
	protected String getLabel() throws EngineException {
		if (workOnSource()) {
			StepSource stepSource = getSource();
			if (stepSource != null) {
				return stepSource.getLabel();
			}
		}
		return getSpecificLabel();
	}
	
	protected String getSpecificLabel() throws EngineException {
		for (StepSource stepSource: getSources()) {
			if (stepSource.isBroken()) {
				return " (" + stepSource.getLabel() + ")";
			}
		}
		return "";
	}
	
	public Node getContextNode(int loop) throws EngineException {
		Element stepElement = sequence.findStepElement(this.executeTimeID);
		return stepElement;
	}
	
	public Node getContextNode(String xpath, int loop) {
		try {
			Node contextNode = getContextNode(loop);
			String contextXPath = getContextXpath(xpath);
			Node node = getXPathAPI().selectSingleNode(contextNode, contextXPath);
			return node;
		} catch (Exception e) {
			if (Engine.logBeans.isInfoEnabled())
				Engine.logBeans.warn("Unable to retrieve context node for step "+ this +" and for xpath \""+ xpath +"\"");
			return null;
		}
	}
	
	public NodeList getContextValues() throws EngineException {
		if (workOnSource())
			return getSource().getContextValues();
		return null;
	}

	protected String getNodeValue(Node node) {
		if (node != null) {
			int len;
			int nodeType = node.getNodeType();
			switch (nodeType) {
				case Node.ELEMENT_NODE:
					if (sequence.getProject().isStrictMode()) {
						return XMLUtils.prettyPrintElement((Element)node, true, false);
					}
					else {
						len = node.getChildNodes().getLength();
						Node firstChild = node.getFirstChild();
						if (firstChild != null) {
							int firstChildType = firstChild.getNodeType();
							switch (firstChildType) {
								case Node.CDATA_SECTION_NODE:
								case Node.TEXT_NODE: 
									return ((len<2) ? firstChild.getNodeValue():XMLUtils.getNormalizedText(node));
								case Node.ELEMENT_NODE: 
									return XMLUtils.prettyPrintElement((Element)node, true, false);
								default: 
									return null;
							}
						} else {
							if (Engine.logBeans.isInfoEnabled())
								Engine.logBeans.warn("Applied XPath on step '"+ this +"' returned node with null value ('"+node.getNodeName()+"')");
							return null;
						}
					}
				case Node.CDATA_SECTION_NODE:
				case Node.TEXT_NODE:
					len = node.getChildNodes().getLength();
					return ((len<2) ? node.getNodeValue():XMLUtils.getNormalizedText(node));
				case Node.ATTRIBUTE_NODE:
					return node.getNodeValue();
				default:
					if (Engine.logBeans.isInfoEnabled())
						Engine.logBeans.warn("Applied XPath on step '"+ this +"' is not supported");
					return null;
			}
		}
		return null;
	}
	
	protected synchronized NodeList getContextValues(String xpath, int loop) throws EngineException {
		NodeList list = null;
		String contextXpath = null;
		try {
			contextXpath = getContextXpath(xpath);
			Node contextNode = getContextNode(loop);
			
			if (Engine.logBeans.isTraceEnabled())
				Engine.logBeans.trace("Source for step is : " + this.getName() + " , contextXPath is : " + contextXpath);
			if (contextNode != null) {
				list = getXPathAPI().selectNodeList(contextNode, contextXpath);
			} else {
				if (Engine.logBeans.isDebugEnabled())
					Engine.logBeans.debug("No context node ! Source for step is : " + this.getName() + " , contextXPath is : " + contextXpath);
			}
		} catch (Exception e) {
			if (Engine.logBeans.isInfoEnabled())
				Engine.logBeans.warn("Error in XPath '" + contextXpath + "' applied to data from Step '" + this.getName()+"' : " +e.getMessage());
		}
		return list;
	}
	
	public String getContextXpath(String xpath) throws EngineException {
		String	contextXpath;
		String 	anchor = getAnchor();
	
		if (anchor == null)
			return xpath;
		
		// remove XPath predicate from anchor if any
		int index = anchor.indexOf('[');
		if (index != -1)
			anchor = anchor.substring(0, index);

		contextXpath = xpath;
		if (xpath.indexOf(anchor) != -1) {
			if (anchor == "//document") {
				contextXpath = xpath.replaceFirst(anchor, "./");
			} else {
				contextXpath = xpath.replaceFirst(anchor, ".");
			}
		}
		return contextXpath;
	}
	
	public String getAnchor() throws EngineException {
		if (!isXml()) {
			if (workOnSource())
				return getSource().getAnchor();
			}
		if (isXml())
			return "//document/"+ getStepNodeName();
		return "//document";
	}
	
	protected boolean inError() throws EngineException {
		if (canWorkOnSource()) {
			for (StepSource stepSource: getSources()) {
				if (stepSource.inError()) {
					return inError = true;
				}
			}
		}
		return inError;
	}
	
	protected void setErrorStatus(boolean inError) {
		this.inError = inError;
	}
	
	@Override
    public void add(DatabaseObject databaseObject) throws EngineException {
        if (databaseObject instanceof Sheet) {
        	addSheet((Sheet) databaseObject);
        }
        else {
            throw new EngineException("You cannot add to a step a database object of type " + databaseObject.getClass().getName());
        }
    }
	
	@Override
	public void remove(DatabaseObject databaseObject) throws EngineException {
        if (databaseObject instanceof Sheet) {
        	removeSheet((Sheet) databaseObject);
        }
        else {
            throw new EngineException("You cannot remove from a step class a database object of type " + databaseObject.getClass().getName());
        }
		super.remove(databaseObject);
	}
	
    public void addSheet(Sheet sheet) throws EngineException {
    	checkSubLoaded();
    	
		String newDatabaseObjectName = getChildBeanName(vSheets, sheet.getName(), sheet.bNew);
		sheet.setName(newDatabaseObjectName);

        // Check for sheet with the same browser
        String requestedBrowser = sheet.getBrowser();
        for(Sheet sh : vSheets) {
            if (sh.getBrowser().equals(requestedBrowser) && (!sh.getName().equals(newDatabaseObjectName)))
                throw new EngineException("Cannot add the sheet because a sheet is already defined for the browser \"" + requestedBrowser + "\" in the step \"" + getName() + "\".");
        }

        vSheets.add(sheet);
        
        super.add(sheet);
    }
    
    public List<Sheet> getSheets() {
    	checkSubLoaded();
        return new ArrayList<Sheet>(vSheets);
    }
    
    public Sheet getSheet(String browser) {
    	checkSubLoaded();
        for(Sheet sheet : vSheets)
            if (sheet.getBrowser().equals(browser))
            	return sheet;
        return null;
    }
    
    public void removeSheet(Sheet sheet) {
    	checkSubLoaded();
        vSheets.remove(sheet);
    }
		
	public String getStepNodeName() {
		return "step";
	}
	
	final public boolean execute(Context javascriptContext, Scriptable scope) throws EngineException {
		EngineStatistics stat = sequence.context.statistics;
		String ts = stat.start(EngineStatistics.EXECUTE_SEQUENCE_STEPS);

		try {
			stepInit();
			if (stepExecute(javascriptContext, scope))
				return true;
			return false;
		}
		catch (Exception e) {
			throw new EngineException("An exception occured while executing step",e);
		}
		finally {
			stepDone();
			
			stat.stop(ts, sequence.getCurrentChildStep() != 0);
		}
	}

	protected void stepInit() throws EngineException {
		getSequence();
		reset();
	}
	
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnabled && sequence.isRunning()) {
			if (Engine.logBeans.isDebugEnabled())
				Engine.logBeans.debug("Executing step named '"+ this +"' ("+ this.getName() +")");
			
			Long key = priority;
			
			// We fire engine events only in studio mode.
            if (Engine.isStudioMode()) {
            	Step loadedStep = (Step) sequence.loadedSteps.get(key).getOriginal();
            	Engine.theApp.fireObjectDetected(new EngineEvent(loadedStep));
            	if (Engine.logBeans.isTraceEnabled())
            		Engine.logBeans.trace("(Step) Step reached before its execution \"" + getName() + "\" ( "+ this+" ["+ hashCode() +"] ).");
				Engine.theApp.fireStepReached(new EngineEvent(loadedStep));
            }
            
            // Generates execution ID
            executeTimeID = getExecuteTimeID();

            // Adds step's reference to executed steps
            executedSteps.put(key, executeTimeID);
            if (Engine.logBeans.isTraceEnabled())
            	Engine.logBeans.trace("Step copy ["+executeTimeID+"] contains "+ executedSteps.size() + " executed steps.");
			
			// Adds step's reference to sequence copies
			sequence.addCopy(executeTimeID, this);
			sequence.setCurrentStep(this);
			sequence.appendStepNode(this);

			return true;
		}
		return false;
	}
	
	protected void stepDone() {
		if (Engine.logBeans.isTraceEnabled())
			Engine.logBeans.trace("Step "+ getName() + " ("+executeTimeID+") done");
		stepDone = true;
	}
	
	protected String getExecuteTimeID() {
		return priority + "." + cloneNumber + "-" + System.currentTimeMillis();
	}
	
	@Override
	public boolean isEnabled() {
		return isEnabled;
	}

	@Override
	public void setEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
	}

	public boolean isOutput() {
		return output;
	}

	public void setOutput(boolean output) {
		this.output = output;
	}
	
	public void stepCopied(StepEvent stepEvent) {
		if (bNew) {
			stepMoved(stepEvent);
		}
	}

	public void stepMoved(StepEvent stepEvent) {
		if (workOnSource()) {
			for (StepSource stepSource: getSources()) {
				stepSource.updateTargetStep((Step) stepEvent.getSource(), (String) stepEvent.data);	
			}
		}
	}
	
	protected transient Object evaluated = null;
	
	public static Long stringToLong(String source) {
		try {
			if (source.isEmpty()) {
				return -1L;
			}
			else {
				return Long.valueOf(String.valueOf(source), 10);
			}
		}
		catch (NumberFormatException nfe) {}
		return null;
	}
	
	protected long evaluateToLong(Context javascriptContext, Scriptable scope, String source, String sourceName, boolean bDialog) throws EngineException {
		Long value = stringToLong(source);
		if (value == null) {
			evaluate(javascriptContext, scope, source, sourceName, true);
			if (evaluated instanceof Undefined || evaluated.equals(""))
				value = -1L;
			else if (evaluated instanceof Number) {
				value = ((Number)evaluated).longValue();
			}
			else {
				try {value = Long.valueOf(String.valueOf(evaluated), 10);}
				catch (NumberFormatException nfe) {}
			}
			if (value == null) {
				EngineException ee = new EngineException(
						"Invalid \""+sourceName+"\" value.\n" +
						"Step: \"" + getName()+ "\"");
				throw ee;
			}
		}
		return value;
	}
	
	protected String evaluateToString(Context javascriptContext, Scriptable scope, String source, String sourceName, boolean bDialog) throws EngineException {
		String value = null;
		evaluate(javascriptContext, scope, source, sourceName, true);
		value = String.valueOf(evaluated);
		if (value == null) {
			EngineException ee = new EngineException(
					"Invalid \""+sourceName+"\" value.\n" +
					"Step: \"" + getName()+ "\"");
			throw ee;
		}
		return value;
	}
	
	protected void evaluate(Context javascriptContext, Scriptable scope, SmartType smartType) throws EngineException {
		smartType.setEvaluated(null);
		
		switch (smartType.getMode()) {
		case PLAIN:
			smartType.setEvaluated(smartType.getExpression());
			break;
		case JS:
			evaluate(javascriptContext, scope, smartType.getExpression(), "smartType", false);
			smartType.setEvaluated(evaluated);
			evaluated = null;
			break;
		default:
			break;
		}
	}
	
	protected void evaluate(Context javascriptContext, Scriptable scope, String source, String sourceName, boolean bDialog) throws EngineException {
		org.mozilla.javascript.Context jsContext = null;
		if (javascriptContext == null) {// evalution of step's property at design time
			jsContext = org.mozilla.javascript.Context.enter();
			scope = jsContext.initStandardObjects(null);
			javascriptContext = jsContext;
		}
		
		String message = null;
		evaluated = null;
		try {
			evaluated = RhinoUtils.evalCachedJavascript(javascriptContext, scope, source, sourceName, 1, null);
			if (evaluated != null && evaluated instanceof NativeJavaObject) {
				evaluated = ((NativeJavaObject) evaluated).unwrap();
			}
		}
		catch(EcmaError e) {
			message = "Unable to evaluate step expression code for '"+ sourceName +"' property or variable.\n" +
			"Step: \"" + getName() + "\"\n" +
			"A Javascript runtime error has occured at line " + 
			e.lineNumber() + ", column " + e.columnNumber() + ": " +
			e.getMessage() + " \n" + e.lineSource();
			logException(e,message, bDialog);
		}
		catch(EvaluatorException e) {
			message = "Unable to evaluate step expression code for '"+ sourceName +"' property or variable.\n" +
			"Step: \"" + getName() + "\"\n" +
			"A Javascript evaluation error has occured: " + e.getMessage();
			logException(e,message, bDialog);
		}
		catch(JavaScriptException e) {
			message = "Unable to evaluate step expression code for '"+ sourceName +"' property or variable.\n" +
			"Step: \"" + getName() + "\"\n" +
			"A Javascript error has occured: " + e.getMessage();
			logException(e,message, bDialog);
		}
		finally {
			if (jsContext != null) {
				org.mozilla.javascript.Context.exit();
			}
			
			if (message != null) {
				EngineException ee = new EngineException(message);
				throw ee;
			}
		}
	}
		
	protected void logException(Throwable e, String message, boolean bDialog) {
   		try {
			Class<?> c = Class.forName("com.twinsoft.convertigo.eclipse.ConvertigoPlugin");
			if (c != null) {
				Object args[] = {null,"error",Boolean.TRUE};
				args[0] = e;
				args[1] = message;
				args[2] = Boolean.valueOf(bDialog);
				try {
					Method method = c.getMethod("logException", new Class[] {Throwable.class, String.class, Boolean.class});
					method.invoke(c,args);
				} catch (Exception ee) {
					;
				}
			}
		} catch (ClassNotFoundException ee) {
			;
		}
	}
	
	protected void logWarning(Throwable e, String message, boolean bDialog) {
   		try {
			Class<?> c = Class.forName("com.twinsoft.convertigo.eclipse.ConvertigoPlugin");
			if (c != null) {
				Object args[] = {e,"warning",Boolean.TRUE};
				args[0] = e;
				args[1] = message;
				args[2] = Boolean.valueOf(bDialog);
				try {
					Method method = c.getMethod("logWarning", new Class[] {Throwable.class, String.class, Boolean.class});
					method.invoke(c,args);
				} catch (Exception ee) {
					;
				}
			}
		} catch (ClassNotFoundException ee) {
			;
		}
	}

	protected void logInfo(String message) {
   		try {
			Class<?> c = Class.forName("com.twinsoft.convertigo.eclipse.ConvertigoPlugin");
			if (c != null) {
				Object args[] = {"info"};
				args[0] = message;
				try {
					Method method = c.getMethod("logInfo", new Class[] {String.class});
					method.invoke(c,args);
				} catch (Exception ee) {
					;
				}
			}
		} catch (ClassNotFoundException ee) {
			;
		}
	}
	
	public abstract String toJsString();
		
	protected void addXmlSchemaAnnotation(XmlSchemaAnnotated annoted) {
		String comment = getComment();
		if (comment != null && comment.length() > 0) {
			XmlSchemaAnnotation annotation = XmlSchemaUtils.makeDynamic(this, new XmlSchemaAnnotation());
			annoted.setAnnotation(annotation);
			XmlSchemaDocumentation documentation = XmlSchemaUtils.makeDynamic(this, new XmlSchemaDocumentation());
			annotation.getItems().add(documentation);
			
			documentation.setMarkup(XMLUtils.asNodeList(comment));
		}
	}
	
	public XmlSchemaObject getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		XmlSchemaElement element = XmlSchemaUtils.makeDynamic(this, new XmlSchemaElement());
		if (getXmlElementRefAffectation().isEmpty()) {
			element.setName(getStepNodeName());
		}
		else {
			element.setRefName(getXmlElementRefAffectation().getQName());
		}
		addXmlSchemaAnnotation(element);
		return element;
	}
	
	public boolean isGenerateSchema() {
		return isOutput();
	}

	public String getSchemaDataType() {
		return schemaDataType;
	}

	public void setSchemaDataType(String schemaDataType) {
		this.schemaDataType = schemaDataType;
	}
	
	public String[] getTagsForProperty(String propertyName) {
		return new String[0];
	}

	public XmlQName getXmlComplexTypeAffectation() {
		return xmlComplexTypeAffectation;
	}

	public void setXmlComplexTypeAffectation(XmlQName xmlComplexTypeAffectation) {
		this.xmlComplexTypeAffectation = xmlComplexTypeAffectation;
	}
	
	public QName getComplexTypeAffectation() {
		return getXmlComplexTypeAffectation().getQName();
	}
    
	public XmlQName getXmlSimpleTypeAffectation() {
		return xmlSimpleTypeAffectation;
	}

	public void setXmlSimpleTypeAffectation(XmlQName xmlSimpleTypeAffectation) {
		this.xmlSimpleTypeAffectation = xmlSimpleTypeAffectation;
	}
	
	public QName getSimpleTypeAffectation() {
		QName qName = getXmlSimpleTypeAffectation().getQName();
		if (qName.getLocalPart().length() == 0) {
			qName = Constants.XSD_STRING;
		}
		return qName;
	}
	
	public XmlQName getXmlElementRefAffectation() {
		return xmlElementRefAffectation;
	}

	public void setXmlElementRefAffectation(XmlQName xmlElementRefAffectation) {
		this.xmlElementRefAffectation = xmlElementRefAffectation;
	}

	public QName getElementRefAffectation() {
		return getXmlElementRefAffectation().getQName();
	}

	@Override
	public boolean testAttribute(String name, String value) {
		if (name.equals("isEnable")) {
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(isEnabled));
		}
		if (name.equals("isTransactionStep")) {
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(this instanceof TransactionStep));
		}
		if (name.equals("isSequenceStep")) {
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(this instanceof SequenceStep));
		}
		if (name.equals("isThenElseStep")) {
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(this instanceof IThenElseContainer));
		}
		if (name.equals("workOnSource")) {
			Boolean bool = Boolean.valueOf(value);
			return bool == workOnSource();
		}
		if (name.equals("canChangeTo")) {
			Boolean bool = Boolean.valueOf(value);
			return 	bool.equals(Boolean.valueOf(this instanceof IfStep)) ||
					bool.equals(Boolean.valueOf(this instanceof IfThenElseStep)) ||
					bool.equals(Boolean.valueOf(this instanceof IsInStep)) ||
					bool.equals(Boolean.valueOf(this instanceof IsInThenElseStep)) ||
					bool.equals(Boolean.valueOf(this instanceof IfExistStep)) ||
					bool.equals(Boolean.valueOf(this instanceof IfExistThenElseStep)) || 
					bool.equals(Boolean.valueOf(this instanceof IfFileExistStep)) ||
					bool.equals(Boolean.valueOf(this instanceof IfFileExistThenElseStep)) ||
					bool.equals(Boolean.valueOf(this instanceof XMLConcatStep)) ||
					bool.equals(Boolean.valueOf(this instanceof XMLElementStep)) ||
					bool.equals(Boolean.valueOf(this instanceof XMLAttributeStep)) ||
					bool.equals(Boolean.valueOf(this instanceof SourceStep)) ||
					bool.equals(Boolean.valueOf(this instanceof SimpleSourceStep)) ||
					bool.equals(Boolean.valueOf(this instanceof ElementStep)) ||
					bool.equals(Boolean.valueOf(this instanceof AttributeStep));
		}
		return super.testAttribute(name, value);
	}

	@Override
	public FolderType getFolderType() {
		return (parent != null && parent instanceof Step) ? FolderType.NONE : FolderType.STEP;
	}
}
