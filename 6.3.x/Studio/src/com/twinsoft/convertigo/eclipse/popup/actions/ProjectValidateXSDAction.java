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
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.eclipse.popup.actions;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.xml.sax.SAXParseException;

import com.sun.tools.xjc.ConsoleErrorReporter;
import com.sun.tools.xjc.XJCListener;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObject;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.util.JAXBUtils;

public class ProjectValidateXSDAction extends MyAbstractAction {

	public ProjectValidateXSDAction() {
		super();
	}

	public void run() {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);

		Shell shell = getParentShell();
		shell.setCursor(waitCursor);

		try {
			ProjectExplorerView explorerView = getProjectExplorerView();
			if (explorerView != null) {
				TreeObject treeObject = explorerView.getFirstSelectedTreeObject();
				if (treeObject instanceof ProjectTreeObject) {
					ProjectTreeObject projectTreeObject = (ProjectTreeObject)treeObject;
					
					// Validate Schema using SUN's XJC tool
			        class PSListener extends XJCListener {
			        	boolean valid = true;
			            ConsoleErrorReporter cer = new ConsoleErrorReporter(ConvertigoPlugin.getDefault().stdoutConsoleStream);
			            
			            public void generatedFile(String fileName, int count, int total) {
			                message2("generating: "+ fileName);
			            }
			            public void message(String msg) {
			                cer.debug(msg);
			                ConvertigoPlugin.logDebug(msg);
			            }
			            public void message2(String msg) {
			                cer.debug(msg);
			                ConvertigoPlugin.logDebug2(msg);
			            }
			            public void error(SAXParseException exception) {
			            	valid = false;
			                cer.error(exception);
			                ConvertigoPlugin.logException(exception, "Unable to validate XSD for project");
			            }
			            public void fatalError(SAXParseException exception) {
			            	valid = false;
			                cer.fatalError(exception);
			                ConvertigoPlugin.logException(exception, "Unable to validate XSD for project");
			            }
			            public void warning(SAXParseException exception) {
			                cer.warning(exception);
			            }
			            public void info(SAXParseException exception) {
			                cer.info(exception);
			            }
			            public boolean wasValid() {
			            	return valid;
			            }
			        }
			        
			        String projectName = projectTreeObject.getName();
					String applicationServerUrl = EnginePropertiesManager.getProperty(PropertyName.APPLICATION_SERVER_CONVERTIGO_URL);
					String xsdUrl = applicationServerUrl + "/projects/"+projectName+"/"+projectName+".temp.xsd";
			        
			        PSListener listener = new PSListener();
			        //String outpuDir = Engine.PROJECTS_DIRECTORY + "/" + projectName + "/jaxb-xjc/src";
			        //JAXBUtils.compileSchema(projectName, outpuDir, xsdUrl, listener);
					JAXBUtils.validateSchema(xsdUrl, listener);
					if (listener.wasValid()) {
						ConvertigoPlugin.logDebug("Schema successfully validated for "+ projectName);
						ConvertigoPlugin.logInfo("Schema successfully validated for "+ projectName);
					}
				}
			}
		} catch (Throwable e) {
			ConvertigoPlugin.logException(e, "Unable to validate XSD for project!");
		} finally {
			shell.setCursor(null);
			waitCursor.dispose();
		}
	}

}
