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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.constants.Constants;
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
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;

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

	/** Holds value of command line output charset */
	private String commandCharset = "UTF-8";

	public ProcessExecStep() {
		super();
		xml = true;
	}

	@Override
	public ProcessExecStep clone() throws CloneNotSupportedException {
		ProcessExecStep clonedObject = (ProcessExecStep) super.clone();
		return clonedObject;
	}

	@Override
	public ProcessExecStep copy() throws CloneNotSupportedException {
		ProcessExecStep clopiedObject = (ProcessExecStep) super.copy();
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

	public String getCommandCharset() {
		return commandCharset;
	}

	public void setCommandCharset(String commandCharset) {
		this.commandCharset = commandCharset;
	}

	@Override
	protected StepSource getSource() {
		return null;
	}

	@Override
	public String toString() {
		return "Exec " + commandLine;
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
		ProcessStreamReaderThread stderrThread = null, stdoutThread = null;

		// Create step child nodes
		final Node errorNode = stepNode.appendChild(doc.createElement("error"));
		final Node outputNode = stepNode.appendChild(doc.createElement("output"));
		final Node exitNode = stepNode.appendChild(doc.createElement("exit"));
		
		// Status exit code
		int status = -1;

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
					envp[i++] = entry.getKey() + "=" + entry.getValue();
			}

			// Execution directory
			File dir = null;
			if (!executionDirectory.equals(""))
				dir = Engine.theApp.filePropertyManager.getFileFromProperty(executionDirectory, getProject()
						.getName());

			// Command line string
			if (evaluated instanceof org.mozilla.javascript.Undefined)
				throw new EngineException("Process command line argument is empty.");
			
			String cmd = null;
			String[] command = null;
			if (evaluated instanceof org.mozilla.javascript.NativeArray) {
				long ln = ((org.mozilla.javascript.NativeArray) evaluated).getLength();
				command = new String[(int) ln];
				for (int i=0; i < ln; i++) {
				command[i] = (String) ((org.mozilla.javascript.NativeArray) evaluated).get(i, null);
				}
			} else {
				cmd = evaluated.toString();
			}

			// Check if encoding is supported
			try {
				if (!Charset.isSupported(commandCharset)) {
					throw new EngineException(
							"Wrong encoding for \"Process execute\" step, please enter a valid one.");
				}
			} catch (IllegalCharsetNameException e) {
				throw new EngineException(
						"Wrong encoding for \"Process execute\" step, please enter a valid one.");
			}

			// Launch the process :
			// if envp is null, current environment parameters are used
			// if dir is null, current execution directory is used
//			final Process process = Runtime.getRuntime().exec(command, envp, dir);		
			final Process process;
			if (command != null) {
				process = Runtime.getRuntime().exec(command, envp, dir);
			} else {
				process = Runtime.getRuntime().exec(cmd, envp, dir);
			}
			

			// Create and launch process stream reader threads
			stderrThread = new ProcessStreamReaderThread(process.getErrorStream(), errorNode);
			stdoutThread = new ProcessStreamReaderThread(process.getInputStream(), outputNode);
			stderrThread.start();
			stdoutThread.start();

			if (isWaitForProcessEnd()) {
				// Launch a thread to handle sequence abortion
				final Sequence s = sequence;
				Thread th = new Thread(new Runnable() { 
					public void run() {
						try {
							while (s.isRunning()) {
								Thread.sleep(500);
							}
							process.destroy();
						} catch (Exception e) {
							Engine.logBeans.warn("An error occured while executing process.", e);
						}
					}
				});
				th.setDaemon(true);
				th.start();

				// Wait for process end
				process.waitFor();
				stdoutThread.join();
				stderrThread.join();
			}

			// Append process exit status to the 'exit' child node
			try {
				status = process.exitValue();
			} catch (IllegalThreadStateException e) {
			}
			
		} catch (Throwable t) {
			setErrorStatus(true);
			if (t instanceof EngineException) {
				Engine.logBeans.error("An error occured while executing process.", t);
				errorNode.appendChild(errorNode.getOwnerDocument().createCDATASection("An error occured. See engine logs for more details..."));
			}
			else
				errorNode.appendChild(errorNode.getOwnerDocument().createCDATASection(t.getMessage()));
		} finally {
			exitNode.appendChild(doc.createTextNode("" + status));
			try {
				stderrThread.bContinue = false;
				stdoutThread.bContinue = false;
			} catch (Exception e) {
			}
		}
	}

	@Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnable()) {
			evaluate(javascriptContext, scope, getCommandLine(), "commandLine", true);
			if (super.stepExecute(javascriptContext, scope)) {
				return true;
			}
		}
		return false;
	}

	class ProcessStreamReaderThread implements Runnable {
		boolean bContinue = true;
		Node outputNode;
		InputStream processStream;
		Thread thread;

		public ProcessStreamReaderThread(final InputStream processStream, final Node outputNode) {
			this.processStream = processStream;
			this.outputNode = outputNode;
			thread = new Thread(this);
			thread.setDaemon(true);
		}

		public void join() throws InterruptedException {
			thread.join();
		}

		public void start() {
			thread.start();
		}

		public void run() {
			BufferedReader reader = null;
			String line, serror = "";
			StringBuffer sb = new StringBuffer();
			try {
				reader = new BufferedReader(commandCharset.equals("") ? new InputStreamReader(processStream)
						: new InputStreamReader(processStream, commandCharset));
				while (bContinue && (line = reader.readLine()) != null) {
					sb.append(line + "\n");
				}
			} catch (Exception e) {
				serror += e.getMessage();
			} finally {
				try {
					// Close reader
					reader.close();

					// Append exception (if any) to the step's child node
					outputNode.appendChild(outputNode.getOwnerDocument().createCDATASection(serror));

					// Append read data to the step's child node
					outputNode.appendChild(outputNode.getOwnerDocument().createCDATASection(sb.toString()));
				} catch (Exception e) {
				}
			}
		}
	}
	
	@Override
	public XmlSchemaElement getXmlSchemaObject(XmlSchemaCollection collection, XmlSchema schema) {
		XmlSchemaElement element = (XmlSchemaElement) super.getXmlSchemaObject(collection, schema);

		XmlSchemaComplexType cType = XmlSchemaUtils.makeDynamic(this, new XmlSchemaComplexType(schema));
		element.setType(cType);

		XmlSchemaSequence sequence = XmlSchemaUtils.makeDynamic(this, new XmlSchemaSequence());
		cType.setParticle(sequence);
		
		XmlSchemaElement elt = XmlSchemaUtils.makeDynamic(this, new XmlSchemaElement());
		sequence.getItems().add(elt);
		elt.setName("error");
		elt.setSchemaTypeName(Constants.XSD_STRING);		

		elt = XmlSchemaUtils.makeDynamic(this, new XmlSchemaElement());
		sequence.getItems().add(elt);
		elt.setName("output");
		elt.setSchemaTypeName(Constants.XSD_STRING);

		elt = XmlSchemaUtils.makeDynamic(this, new XmlSchemaElement());
		sequence.getItems().add(elt);
		elt.setName("exit");
		elt.setSchemaTypeName(Constants.XSD_INTEGER);
		
		return element;
	}
}
