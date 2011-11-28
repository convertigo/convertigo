/*
 * Copyright (c) 2001-2011 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL: $
 * $Author: $
 * $Revision: $
 * $Date: $
 */

package com.twinsoft.convertigo.beans.steps;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepSource;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

public class ProcessExecStep extends Step {

	private static final long serialVersionUID = -7463773363374539938L;

	/** Holds the value for command line */
	private String commandLine = "";
	
	/** Holds the value for the execution directory */
	private String executionDirectory = "";

	/** Holds value for the environment parameters */
    private XMLVector<XMLVector<String>> envParameters = new XMLVector<XMLVector<String>>();
	
    /** Holds value for wait of process end */
    private boolean waitForProcessEnd = true;
    
	public ProcessExecStep() {
		super();
		xml = true;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		ProcessExecStep clonedObject = (ProcessExecStep)super.clone();
		return clonedObject;
	}
	
	@Override
	public Object copy() throws CloneNotSupportedException {
		ProcessExecStep clopiedObject = (ProcessExecStep)super.copy();
		return clopiedObject;
	}
	
	public void setCommandLine(String commandLine) {
		this.commandLine = commandLine;
	}

	public String getCommandLine() {
		return commandLine;
	}
	
	public void setExecutionDirectory(String executionDirectory) {
		this.executionDirectory = executionDirectory;
	}

	public String getExecutionDirectory() {
		return executionDirectory;
	}

	public void setEnvParameters(XMLVector<XMLVector<String>> envParameters) {
		this.envParameters = envParameters;
	}

	public XMLVector<XMLVector<String>> getEnvParameters() {
		return envParameters;
	}

	public void setWaitForProcessEnd(boolean waitForProcessEnd) {
		this.waitForProcessEnd = waitForProcessEnd;
	}

	public boolean isWaitForProcessEnd() {
		return waitForProcessEnd;
	}

	@Override
	protected StepSource getSource() {
		return null;
	}

	@Override
	public String toString() {
		return "Exec "+ commandLine;
	}

	@Override
	public String toJsString() {
		return "";
	}

	@Override
	protected boolean workOnSource() {
		return false;
	}

	@Override
	public String getStepNodeName() {
		return "process";
	}

	@Override
	protected void createStepNodeValue(Document doc, Element stepNode) throws EngineException {
		// Create step child nodes
		final Node errorNode = stepNode.appendChild(doc.createElement("error"));
		final Node outputNode = stepNode.appendChild(doc.createElement("output"));
		final Node exitNode = stepNode.appendChild(doc.createElement("exit"));
		
		try {
			// Environment parameters (name/value pairs)
			String[] envp = null;
			if (envParameters.size() > 0) {
				// Retrieve current environment parameters and overrides
				Map<String, String> envmap = new HashMap<String, String>();
				envmap.putAll(System.getenv());
				for (List<String> parameter : envParameters)
					envmap.put(parameter.get(0), parameter.get(1));
				// Fill parameters array
				int i = 0;
				envp = new String[envmap.size()];
				for (Map.Entry<String, String> entry : envmap.entrySet()) 
					envp[i++] = entry.getKey()+"="+entry.getValue();
			}
			
			// Execution directory
			File dir = null;
			if (!executionDirectory.equals(""))
				dir = Engine.theApp.filePropertyManager.getFileFromProperty(executionDirectory, getProject().getName());
			
			// Command line string
			if (evaluated instanceof org.mozilla.javascript.Undefined)
				throw new EngineException("Process command line argument is empty.");
			String command = evaluated.toString();
			
			// Status exit code
			String status = "-1";
			
			// Launch the process :
			// if envp is null, current environment parameters are used
			// if dir is null, current execution directory is used
			final Process process = Runtime.getRuntime().exec(command, envp, dir);
			
			// Launch the stdout consumer thread
			new Thread() {
				public void run() {
					BufferedReader reader = null;
					String line, cdata = "";
					try {
						reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
						while((line = reader.readLine()) != null) {
							cdata += line + "\n";
						}
					}
					catch(Exception e) {
						//Engine.logBeans.warn("An error occured while executing process.", e);
						errorNode.appendChild(errorNode.getOwnerDocument().createCDATASection(e.getMessage()));
					}
					finally {
						try {
							reader.close();
						} catch (IOException e) {}
						// Append data to the 'ouput' child node
						outputNode.appendChild(outputNode.getOwnerDocument().createCDATASection(cdata));
					}
				}
			}.start();

			// Launch the stderr consumer thread
			new Thread() {
				public void run() {
					BufferedReader reader = null;
					String line, cdata = "";
					try {
						reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
						while((line = reader.readLine()) != null) {
							cdata += line + "\n";
						}
					}
					catch(Exception e) {
						//Engine.logBeans.warn("An error occured while executing process.", e);
						errorNode.appendChild(errorNode.getOwnerDocument().createCDATASection(e.getMessage()));
					}
					finally {
						try {
							reader.close();
						} catch (Exception e) {}
						// Append data to the 'error' child node
						errorNode.appendChild(errorNode.getOwnerDocument().createCDATASection(cdata));
					}
				}
			}.start();
			
			if (isWaitForProcessEnd()) {
				// Launch a thread to handle sequence abortion
				final Sequence s = sequence;
				new Thread() {
					public void run() {
						try {
							while (s.isRunning()) {
								Thread.sleep(500);
							}
							process.destroy();
						} catch(Exception e) {
							Engine.logBeans.warn("An error occured while executing process.", e);
						}
					}
				}.start();

				// Wait for process end
				process.waitFor();
			}
			
			// Append process exit status to the 'exit' child node
			try {status = ""+process.exitValue();}
			catch (IllegalThreadStateException e) {}
			exitNode.appendChild(doc.createTextNode(status));
		}
		catch (Throwable t) {
			setErrorStatus(true);
			//Engine.logBeans.error("An error occured while executing process.", t);
			errorNode.appendChild(errorNode.getOwnerDocument().createCDATASection(t.getMessage()));
		}
	}

	@Override
	protected boolean stepExcecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable) {
			evaluate(javascriptContext, scope, getCommandLine(), "commandLine", true);
			if (super.stepExcecute(javascriptContext, scope)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String getSchemaType(String tns) {
		return tns +":"+ getStepNodeName() + priority +"StepType";
	}
	
	@Override
	public void addSchemaType(HashMap<Long, String> stepTypes, String tns, String occurs) throws EngineException {
		String stepTypeSchema = "";
		stepTypeSchema += "\t<xsd:complexType name=\""+ getSchemaTypeName(tns) +"\">\n";
		stepTypeSchema += "\t\t<xsd:sequence>\n";
		stepTypeSchema += "\t\t\t<xsd:element minOccurs=\"0\" maxOccurs=\"1\" name=\"error\" type=\"xsd:string\" />\n";
		stepTypeSchema += "\t\t\t<xsd:element minOccurs=\"0\" maxOccurs=\"1\" name=\"output\" type=\"xsd:string\" />\n";
		stepTypeSchema += "\t\t\t<xsd:element minOccurs=\"0\" maxOccurs=\"1\" name=\"exit\" type=\"xsd:int\" />\n";
		stepTypeSchema += "\t\t</xsd:sequence>\n";
		stepTypeSchema += "\t</xsd:complexType>\n";
		
		stepTypes.put(new Long(priority), stepTypeSchema);
	}
	
	@Override
	protected Node createWsdlDom() throws EngineException {
		Element element = (Element) super.createWsdlDom();
		element.appendChild(wsdlDom.createElement("error"));
		element.appendChild(wsdlDom.createElement("output"));
		element.appendChild(wsdlDom.createElement("exit"));
		return element;
	}
}
