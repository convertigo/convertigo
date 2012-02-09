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

package com.twinsoft.convertigo.engine.util;

import java.io.File;
import java.io.FilenameFilter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.output.FileWriterWithEncoding;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.BlockFactory;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Criteria;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.ExtractionRule;
import com.twinsoft.convertigo.beans.core.IScreenClassContainer;
import com.twinsoft.convertigo.beans.core.MobileDevice;
import com.twinsoft.convertigo.beans.core.Pool;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.RequestableStep;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Sheet;
import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.beans.core.StatementWithExpressions;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.beans.core.TestCase;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.core.TransactionWithVariables;
import com.twinsoft.convertigo.beans.core.Variable;
import com.twinsoft.convertigo.beans.screenclasses.JavelinScreenClass;
import com.twinsoft.convertigo.beans.statements.HTTPStatement;
import com.twinsoft.convertigo.beans.transactions.HtmlTransaction;
import com.twinsoft.convertigo.beans.variables.HttpStatementVariable;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

public class CarUtils {

	public static void makeArchive(String projectName) throws EngineException {
		Project project = Engine.theApp.databaseObjectsManager.getProjectByName(projectName);
		makeArchive(project);
	}

	public static void makeArchive(Project project) throws EngineException {
		makeArchive(Engine.PROJECTS_PATH, project);
	}

	public static void makeArchive(String dir, Project project) throws EngineException {
		List<File> undeployedFiles=getUndeployedFiles(project.getName());	
		String projectName = project.getName();
		try {
			// Export the project
			String exportedProjectFileName = Engine.PROJECTS_PATH + "/" + projectName + "/" + projectName + ".xml";
			exportProject(project, exportedProjectFileName);
			
			// Create Convertigo archive
			String projectArchiveFilename = dir + "/" + projectName + ".car";
			ZipUtils.makeZip(projectArchiveFilename, Engine.PROJECTS_PATH + "/" + projectName, projectName, undeployedFiles);
		} catch(Exception e) {
			throw new EngineException("Unable to make the archive file for the project \"" + projectName + "\".", e);
		}
	}

	private static List<File> getUndeployedFiles(String projectName){
		List<File> undeployedFiles = new LinkedList<File>();
		
		File privateDir = new File(Engine.PROJECTS_PATH + "/" + projectName + "/_private");
		undeployedFiles.add(privateDir);
		File dataDir = new File(Engine.PROJECTS_PATH + "/" + projectName + "/_data");
		undeployedFiles.add(dataDir);
		File carFile = new File(Engine.PROJECTS_PATH + "/" + projectName + "/" + projectName + ".car");
		undeployedFiles.add(carFile);
		
		File tempXsdFile = new File(Engine.PROJECTS_PATH + "/" + projectName + "/" + projectName + ".temp.xsd");
		undeployedFiles.add(tempXsdFile);
		File tempWsdlFile = new File(Engine.PROJECTS_PATH + "/" + projectName + "/" + projectName + ".temp.wsdl");
		undeployedFiles.add(tempWsdlFile);
		
		List<File> svnFiles = CarUtils.deepListFiles(Engine.PROJECTS_PATH + "/" + projectName, ".svn");
		undeployedFiles.addAll(svnFiles);
		
		return undeployedFiles;
	}

	public static void exportProject(Project project, String fileName) throws EngineException {
		Document document = exportProject(project);
		try {
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			
			boolean isCR = FileUtils.isCRLF();
			
			Writer writer = isCR ? new StringWriter() : new FileWriterWithEncoding(fileName, "UTF-8");
			transformer.transform(new DOMSource(document), new StreamResult(writer));
			
			if (isCR) {
				String content = FileUtils.CrlfToLf(writer.toString());
				writer = new FileWriterWithEncoding(fileName, "UTF-8");
				writer.write(content);
			}
			
			writer.close();
		} catch (Exception e) {
			throw new EngineException("(CarUtils) exportProject failed", e);
		}
	}

	private static Document exportProject(Project project) throws EngineException {
		try {
			Document document = XMLUtils.getDefaultDocumentBuilder().newDocument();
			//            ProcessingInstruction pi = document.createProcessingInstruction("xml", "version=\"1.0\" encoding=\"UTF-8\"");
			//            document.appendChild(pi);
			Element rootElement = document.createElement("convertigo");
			DateFormat df = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT, Locale.getDefault());
			String exportDate = df.format(Calendar.getInstance().getTime());
			rootElement.setAttribute("exported", exportDate);
			rootElement.setAttribute("version", com.twinsoft.convertigo.engine.Version.fullProductVersion);
			rootElement.setAttribute("engine", com.twinsoft.convertigo.engine.Version.version);
			rootElement.setAttribute("beans", com.twinsoft.convertigo.beans.Version.version);
			String studioVersion = "";
			try {
				Class<?> c = Class.forName("com.twinsoft.convertigo.eclipse.Version");
				studioVersion = (String)c.getDeclaredField("version").get(null);
			} catch (Exception e) {
			} catch (Throwable th) {
			}
			
			rootElement.setAttribute("studio", studioVersion);
			document.appendChild(rootElement);
			exportDatabaseObject(document, rootElement, project);
			
			return document;
		} catch(Exception e) {
			throw new EngineException("Unable to export the project \"" + project.getName() + "\".", e);
		}
	}

	private static void exportDatabaseObject(Document document, Element parentElement, DatabaseObject databaseObject) throws EngineException {
		Element element = parentElement;
		element = databaseObject.toXml(document);
		String name =  "(" + databaseObject.getClass().getSimpleName() + ") " + databaseObject.getName();
		Integer depth = (Integer) document.getUserData("depth");
		if (depth == null) {
			depth = 0;
		}
		String openpad = StringUtils.repeat("  ", depth);
		String closepad = StringUtils.repeat("  ", depth);
		parentElement.appendChild(document.createTextNode("\n"));
		parentElement.appendChild(document.createComment(StringUtils.rightPad(openpad + "\\ "+ name , 150)));
		parentElement.appendChild(element);
		
		document.setUserData("depth", depth + 1, null);
		
		if (databaseObject instanceof Project) {
			Project project = (Project) databaseObject;
			
			for (Connector connector : project.getConnectorsList()) {
				exportDatabaseObject(document, element, connector);
			}
			
			for (Sequence sequence : project.getSequencesList()) {
				exportDatabaseObject(document, element, sequence);
			}
			
			for (MobileDevice device : project.getMobileDeviceList()) {
				exportDatabaseObject(document, element, device);
			}
		} else if (databaseObject instanceof Sequence) {
			Sequence sequence = (Sequence) databaseObject;
			
			for (Step step : sequence.getSteps()) {
				exportDatabaseObject(document, element, step);
			}
			
			for (Sheet sheet : sequence.getSheetsList()) {
				exportDatabaseObject(document, element, sheet);
			}
			
			for (RequestableVariable variable : sequence.getVariablesList()) {
				exportDatabaseObject(document, element, variable);
			}
			
			for (TestCase testCase : sequence.getTestCasesList()) {
				exportDatabaseObject(document, element, testCase);
			}
		} else if (databaseObject instanceof Connector) {
			Connector connector = (Connector) databaseObject;
			
			if (databaseObject instanceof IScreenClassContainer<?>) {
				ScreenClass defaultScreenClass = ((IScreenClassContainer<?>) databaseObject).getDefaultScreenClass();
				if (defaultScreenClass != null) {
					exportDatabaseObject(document, element, defaultScreenClass);
				}
			}
			
			for (Transaction transaction : connector.getTransactionsList()) {
				exportDatabaseObject(document, element, transaction);
			}
			
			for (Pool pool : connector.getPoolsList()) {
				exportDatabaseObject(document, element, pool);
			}
		} else if (databaseObject instanceof Transaction) {
			Transaction transaction = (Transaction) databaseObject;
			for (Sheet sheet : transaction.getSheetsList()) {
				exportDatabaseObject(document, element, sheet);
			}
			
			if (transaction instanceof HtmlTransaction) {
				HtmlTransaction htmlTransaction = (HtmlTransaction) transaction;
				
				for (Statement statement : htmlTransaction.getStatements()) {
					exportDatabaseObject(document, element, statement);
				}
			}
			
			if (databaseObject instanceof TransactionWithVariables) {
				for (TestCase testCase : ((TransactionWithVariables) databaseObject).getTestCasesList()) {
					exportDatabaseObject(document, element, testCase);
				}
				
				for (RequestableVariable variable : ((TransactionWithVariables) databaseObject).getVariablesList()) {
					exportDatabaseObject(document, element, variable);
				}
			}
		} else if (databaseObject instanceof StatementWithExpressions) {
			for (Statement statement : ((StatementWithExpressions) databaseObject).getStatements()) {
				exportDatabaseObject(document, element, statement);
			}
		} else if (databaseObject instanceof HTTPStatement) {
			for (HttpStatementVariable variable : ((HTTPStatement) databaseObject).getVariables()) {
				exportDatabaseObject(document, element, variable);
			}
		} else if (databaseObject instanceof StepWithExpressions) {
			for (Step step : ((StepWithExpressions) databaseObject).getSteps()) {
				exportDatabaseObject(document, element, step);
			}
		} else if (databaseObject instanceof RequestableStep) {
			for (Variable variable : ((RequestableStep) databaseObject).getVariables()) {
				exportDatabaseObject(document, element, variable);
			}
		} else if (databaseObject instanceof TestCase) {
			for (Variable variable : ((TestCase) databaseObject).getVariables()) {
				exportDatabaseObject(document, element, variable);
			}
		} else if (databaseObject instanceof ScreenClass) {
			if (databaseObject instanceof JavelinScreenClass) {
				BlockFactory blockFactory = ((JavelinScreenClass) databaseObject).getLocalBlockFactory();
				if (blockFactory != null) {
					exportDatabaseObject(document, element, blockFactory);
				}
			}
			
			for (Criteria criteria : ((ScreenClass) databaseObject).getLocalCriterias()) {
				exportDatabaseObject(document, element, criteria);
			}
			
			for (ExtractionRule extractionRule : ((ScreenClass) databaseObject).getLocalExtractionRules()) {
				exportDatabaseObject(document, element, extractionRule);
			}
			
			for (Sheet sheet : ((ScreenClass) databaseObject).getLocalSheets()) {
				exportDatabaseObject(document, element, sheet);
			}
			
			for (ScreenClass screenClass : ((ScreenClass) databaseObject).getInheritedScreenClasses()) {
				exportDatabaseObject(document, element, screenClass);
			}
		}
		
		element.appendChild(document.createTextNode("\n"));
		element.appendChild(document.createComment(StringUtils.rightPad(closepad + "/ " + name, 150)));
		document.setUserData("depth", depth, null);
	}

	/*
	 * Returns an ArrayList of abstract pathnames denoting the files and directories
	 * in the directory denoted by this abstract pathname
	 * that satisfy the specified suffix
	 */
	public static ArrayList<File> deepListFiles(String sDir, String suffix) {
		final String _suffix = suffix;
		File[] all, files;
		File f, dir;
		
		dir = new File(sDir);
		
		all = dir.listFiles();
		
		files = dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				File file = new File(dir, name);
				return (file.getName().endsWith(_suffix));
			}
		});
		
		ArrayList<File> list = null, deep = null;
		if (files != null) {
			list = new ArrayList<File>(Arrays.asList(files));
		}
		
		if ((list != null) && (all != null)) {
			for (int i=0; i<all.length; i++) {
				f = all[i];
				if (f.isDirectory() && !list.contains(f)) {
					deep = deepListFiles(f.getAbsolutePath(), suffix);
					if (deep != null) {
						list.addAll(deep);
					}
				}
			}
		}
		
		return list;
	}
}
