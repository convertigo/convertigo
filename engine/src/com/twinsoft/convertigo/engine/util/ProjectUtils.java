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

package com.twinsoft.convertigo.engine.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.connectors.CicsConnector;
import com.twinsoft.convertigo.beans.connectors.HttpConnector;
import com.twinsoft.convertigo.beans.connectors.JavelinConnector;
import com.twinsoft.convertigo.beans.connectors.SiteClipperConnector;
import com.twinsoft.convertigo.beans.connectors.SqlConnector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Criteria;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.DatabaseObject.ExportOption;
import com.twinsoft.convertigo.beans.core.ExtractionRule;
import com.twinsoft.convertigo.beans.core.Pool;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Reference;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Sheet;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.TestCase;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.core.TransactionWithVariables;
import com.twinsoft.convertigo.beans.core.Variable;
import com.twinsoft.convertigo.beans.references.ProjectSchemaReference;
import com.twinsoft.convertigo.beans.screenclasses.JavelinScreenClass;
import com.twinsoft.convertigo.beans.screenclasses.SiteClipperScreenClass;
import com.twinsoft.convertigo.beans.steps.SimpleStep;
import com.twinsoft.convertigo.beans.transactions.HttpTransaction;
import com.twinsoft.convertigo.beans.transactions.JavelinTransaction;
import com.twinsoft.convertigo.beans.transactions.JsonHttpTransaction;
import com.twinsoft.convertigo.beans.transactions.SiteClipperTransaction;
import com.twinsoft.convertigo.beans.transactions.SqlTransaction;
import com.twinsoft.convertigo.beans.transactions.XmlHttpTransaction;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.helpers.WalkHelper;
import com.twinsoft.convertigo.engine.proxy.translated.ProxyTransaction;

public class ProjectUtils {

	public static void copyIndexFile(String projectName) throws Exception {
		String projectRoot = Engine.projectDir(projectName);
		String templateBase = Engine.TEMPLATES_PATH + "/base";
		File indexPage = new File(projectRoot + "/index.html");
		if (!indexPage.exists()) {
			if (new File(projectRoot + "/sna.xsl").exists()) { /** webization javelin */
				if (new File(projectRoot + "/templates/status.xsl").exists()) { /** not DKU / DKU */
					FileUtils.copyFile(new File(templateBase + "/index_javelin.html"), indexPage);
				} else {
					FileUtils.copyFile(new File(templateBase + "/index_javelinDKU.html"), indexPage);
				}
			} else {
				FileFilter fileFilterNoSVN = new FileFilter() {
					public boolean accept(File pathname) {
						String name = pathname.getName();
						return !name.equals(".svn") || !name.equals("CVS") || !name.equals("node_modules");
					}
				};
				FileUtils.copyFile(new File(templateBase + "/index.html"), indexPage);
				FileUtils.copyDirectory(new File(templateBase + "/js"), new File(projectRoot + "/js"), fileFilterNoSVN);
				FileUtils.copyDirectory(new File(templateBase + "/css"), new File(projectRoot + "/css"), fileFilterNoSVN);
			}
		}
	}

	public static File renameProjectFile(File oldXml, String newName, boolean keepOldReferences) throws Exception {
		File oldYaml = oldXml.getName().equals("c8oProject.yaml") ? oldXml : new File(oldXml.getParentFile(), "c8oProject.yaml");

		if (!oldXml.exists() && !oldYaml.exists()) {
			throw new Exception("File \"" + oldXml.getAbsolutePath() + "\" does not exist");
		}

		String oldName = null;
		File newFile = null;
		List<Replacement> replacements = new ArrayList<>(10);

		if (!oldYaml.exists()) {
			newFile = new File(oldXml.getParentFile(), newName + ".xml");

			if (newFile.exists()) {
				throw new Exception("File \"" + newFile.getAbsolutePath() + "\" already exists");
			}

			if (!oldXml.renameTo(newFile)) {
				throw new Exception("Unable to rename \"" + oldXml.getAbsolutePath() + "\" to \"" + newFile.getAbsolutePath() + "\"");
			}

			oldName = oldXml.getName();
			oldName = oldName.substring(0, oldName.length() - 4);

			if (isPreviousXmlFileFormat(newFile)) {
				// replace project's bean name
				replacements.add(new Replacement("value=\"" + oldName + "\"", "value=\"" + newName + "\""));
			} else {
				// replace project's bean name
				replacements.add(new Replacement("<!--<Project : " + oldName + ">", "<!--<Project : " + newName + ">"));
				replacements.add(new Replacement("value=\"" + oldName + "\"", "value=\"" + newName + "\"", "<!--<Project"));
				replacements.add(new Replacement("<!--</Project : " + oldName + ">", "<!--</Project : " + newName + ">"));
			}

			// replace project's name references
			if (!keepOldReferences) {
				replacements.add(new Replacement("value=\"" + oldName + "\\.", "value=\"" + newName + "\\."));
			}

			makeReplacementsInFile(replacements, newFile);
		} else {
			newFile = oldYaml;
			oldName = DatabaseObjectsManager.getProjectName(oldYaml);
			if (oldName.equals(newName)) {
				return newFile;
			}

			replacements.add(new Replacement("↓" + oldName + " \\[core\\.Project\\]:", "↓" + newName + " [core.Project]:"));

			makeReplacementsInFile(replacements, newFile, "UTF-8");

			// replace project's name references
			if (!keepOldReferences) {
				replacements.clear();
				replacements.add(new Replacement(": " + oldName + "\\.", ": " + newName + "."));
				replacements.add(new Replacement(":" + oldName + "\\.", ":" + newName + "."));
				replacements.add(new Replacement("\"" + oldName + "\"", "\"" + newName + "\""));
				replacements.add(new Replacement("\\\\\"" + oldName + "\\\\\"", "\\\\\"" + newName + "\\\\\""));
				replacements.add(new Replacement("\'" + oldName + "\\.", "\'" + newName + "."));
				makeReplacementsInFile(replacements, newFile, "UTF-8");
				File sub = new File(newFile.getParentFile(), "_c8oProject");
				if (sub.exists()) {
					List<File> files = new ArrayList<File>(Arrays.asList(sub.listFiles()));
					while (!files.isEmpty()) {
						File f = files.remove(0);
						if (f.isDirectory()) {
							files.addAll(Arrays.asList(f.listFiles()));
						} else if (f.getName().endsWith(".yaml")) {
							makeReplacementsInFile(replacements, f, "UTF-8");
						}
					}
				}
			}
		}

		File dotProject = new File(oldXml.getParentFile(), ".project");
		if (dotProject.exists()) {
			replacements.clear();
			replacements.add(new Replacement(oldName, newName));
			makeReplacementsInFile(replacements, dotProject);
		}

		ArrayList<File> deep = CarUtils.deepListFiles(oldXml.getParent() + "/xsd/internal", ".xsd");
		File xsd = new File(oldXml.getParentFile(), "xsd/" + oldName + ".xsd");
		if (!xsd.exists()) {
			xsd = new File(oldXml.getParentFile(), oldName + ".xsd");
		}
		if (xsd.exists()) {
			File oldXsd = xsd;
			oldXsd.renameTo(xsd = new File(oldXml.getParentFile(), newName + ".xsd"));
			deep.add(xsd);
		}

		if (deep != null && !deep.isEmpty()) {
			// update transaction schema files with new project's name
			replacements.clear();
			replacements.add(new Replacement("/" + oldName, "/" + newName));
			replacements.add(new Replacement(oldName + "_ns", newName + "_ns"));
			for (File schema : deep) {
				try {
					ProjectUtils.makeReplacementsInFile(replacements, schema.getAbsolutePath());
					Engine.logDatabaseObjectManager.debug("Successfully updated schema file \"" + schema.getAbsolutePath() + "\"");
				} catch (Exception e) {
					Engine.logDatabaseObjectManager.warn("Unable to update schema file \"" + schema.getAbsolutePath() + "\"");
				}
			}
		}

		File wsld = new File(oldXml.getParentFile(), oldName + ".wsdl");
		if (wsld.exists()) {
			File oldWsld = wsld;
			oldWsld.renameTo(wsld = new File(oldXml.getParentFile(), newName + ".wsld"));
			replacements.clear();
			replacements.add(new Replacement("/" + oldName, "/" + newName));
			replacements.add(new Replacement(oldName + "_ns", newName + "_ns"));
			replacements.add(new Replacement(oldName + ".xsd", newName+".xsd"));
			replacements.add(new Replacement(oldName + "Port", newName+"Port"));
			replacements.add(new Replacement(oldName + "SOAP", newName+"SOAP"));
			replacements.add(new Replacement("soapAction=\"" + oldName + "\\?", "soapAction=\"" + newName + "\\?"));
			replacements.add(new Replacement("definitions name=\"" + oldName + "\"", "definitions name=\"" + newName + "\""));
			replacements.add(new Replacement("service name=\"" + oldName + "\"", "service name=\"" + newName + "\""));
			makeReplacementsInFile(replacements, wsld);
		}

		return newFile;
	}

	private static boolean isPreviousXmlFileFormat(File file) throws Exception {
		boolean isPreviousFormat = false;
		String line= null;
		BufferedReader br = new BufferedReader(new FileReader(file));
		while ((line = br.readLine()) != null) {
			if (line.indexOf("<project classname=\"com.twinsoft.convertigo.beans.core.Project\"")!=-1) {
				isPreviousFormat = !line.trim().startsWith("<!--<Project");
				break;
			}
		}
		br.close();
		return isPreviousFormat;
	}

	public static void makeReplacementsInFile(List<Replacement> replacements, String filePath) throws Exception {
		makeReplacementsInFile(replacements, new File(filePath));
	}

	private static void makeReplacementsInFile(List<Replacement> replacements, File file) throws Exception {
		makeReplacementsInFile(replacements, file, Charset.defaultCharset().name());
	}

	private static void makeReplacementsInFile(List<Replacement> replacements, File file, String encoding) throws Exception {
		if (file.exists()) {
			String line;
			StringBuffer sb = new StringBuffer();

			try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), encoding))) {
				while ((line = br.readLine()) != null) {
					for (Replacement replacement: replacements) {
						String lineBegin = replacement.getStartsWith();
						if ((lineBegin == null) || (line.trim().startsWith(lineBegin))) {
							line = line.replaceAll(replacement.getSource(), replacement.getTarget());
						}
					}
					sb.append(line+"\n");
				}
			}

			FileUtils.write(file, sb.toString(), encoding);
		}
		else {
			throw new Exception("File \"" + file.getAbsolutePath() + "\" does not exist");
		}
	}

	public static void xsdRenameProject(String filePath, String sourceProjectName, String targetProjectName) throws Exception {
		if (filePath.endsWith(".xsd")) {
			List<Replacement> replacements = new ArrayList<Replacement>();
			replacements.add(new Replacement("/"+sourceProjectName, "/"+targetProjectName));
			replacements.add(new Replacement(sourceProjectName+"_ns", targetProjectName+"_ns"));
			makeReplacementsInFile(replacements, filePath);
		}
	}

	public static void xsdRenameConnector(String filePath, String oldName, String newName) throws Exception {
		if (filePath.endsWith(".xsd")) {
			List<Replacement> replacements = new ArrayList<Replacement>();
			replacements.add(new Replacement(oldName+"__", newName+"__"));
			makeReplacementsInFile(replacements, filePath);
		}
	}

	private static void constructDom(Document document, Element root, DatabaseObject father, ExportOption... exportOptions) throws EngineException {
		List<DatabaseObject> dbos = father.getAllChildren();
		addElement(dbos, document, root, exportOptions);
	}

	private static <E extends DatabaseObject> void addElement(Collection<E> collection, Document document, Element root, ExportOption... exportOptions) throws EngineException {
		for (E dbo : collection) {
			Element tag = dbo.toXml(document, exportOptions);
			tag.setAttribute("qname", dbo.getQName());
			root.appendChild(tag);
			constructDom(document, tag, dbo, exportOptions);
		}
	}

	public static boolean existProjectSchemaReference(Project project, String projectName) {
		if (projectName.equals(project.getName()))
			return true;
		for (Reference reference : project.getReferenceList()) {
			if (reference instanceof ProjectSchemaReference) {
				if (((ProjectSchemaReference)reference).getParser().getProjectName().equals(projectName))
					return true;
			}
		}
		return false;
	}

	public static Map<String,String> getStatByProject(Project project) throws Exception {
		final Map<String,String> result = new HashMap<String,String>();
		try {
			if (project != null) {
				try {

					new WalkHelper() {
						String displayString = "";

						@SuppressWarnings("unused")
						int depth = 0;
						int sequenceJavascriptLines;
						int sequenceJavascriptFunction;

						int connectorCount = 0;
						int httpConnectorCount = 0;
						int httpsConnectorCount = 0;
						int cicsConnectorCount = 0;
						int siteClipperConnectorCount = 0;
						int sqlConnectorCount = 0;
						int javelinConnectorCount = 0;

						int htmlScreenclassCount = 0;
						int htmlCriteriaCount = 0;
						int siteClipperScreenclassCount = 0;
						int siteClipperCriteriaCount = 0;
						int htmlExtractionRuleCount = 0;
						int htmlTransactionVariableCount = 0;

						int sqlTransactionVariableCount = 0;
						int javelinTransactionVariableCount = 0;
						int javelinScreenclassCount = 0;
						int javelinCriteriaCount = 0;
						int javelinExtractionRuleCount = 0;
						int javelinEntryHandlerCount = 0;
						int javelinExitHandlerCount = 0;
						int javelinHandlerCount = 0;
						int javelinJavascriptLines = 0;
						int poolCount = 0;
						@SuppressWarnings("unused")
						int reqVariableCount = 0;
						int sequenceVariableCount = 0;
						@SuppressWarnings("unused")
						int transactionVariableCount = 0;
						int testcaseVariableCount = 0;
						int testcaseCount = 0;
						int sequenceCount = 0;
						int stepCount = 0;
						int sheetCount = 0;
						int referenceCount = 0;
						int selectInQueryCount = 0;

						/*
						 * transaction counters
						 */
						@SuppressWarnings("unused")
						int transactionCount = 0;
						@SuppressWarnings("unused")
						int transactionWithVariablesCount = 0;

						int htmltransactionCount = 0;
						int httpTransactionCount = 0;
						int httpsTransactionCount = 0;
						int xmlHttpTransactionCount = 0;
						int xmlHttpsTransactionCount = 0;
						int jsonHttpTransactionCount = 0;
						int jsonHttpsTransactionCount = 0;
						int proxyTransactionCount = 0;
						int siteClipperTransactionCount = 0;
						int javelinTransactionCount = 0;
						int sqlTransactionCount = 0;
						int totalC8oObjects = 0;

						public void go(DatabaseObject project) {
							try {
								String projectName = project.getName();

								init(project);

								connectorCount = httpConnectorCount + httpsConnectorCount + cicsConnectorCount + siteClipperConnectorCount + sqlConnectorCount + javelinConnectorCount;

								totalC8oObjects = 1
										+ connectorCount	// connectors
										+ htmlScreenclassCount
										+ htmlCriteriaCount
										+ htmlExtractionRuleCount
										+ htmlTransactionVariableCount
										+ javelinScreenclassCount
										+ javelinCriteriaCount
										+ javelinExtractionRuleCount
										+ javelinTransactionCount
										+ javelinEntryHandlerCount
										+ javelinExitHandlerCount
										+ javelinHandlerCount
										+ javelinTransactionVariableCount
										+ sqlTransactionCount
										+ sqlTransactionVariableCount
										+ sheetCount
										+ jsonHttpTransactionCount
										+ jsonHttpsTransactionCount
										+ xmlHttpTransactionCount
										+ xmlHttpsTransactionCount
										+ httpTransactionCount
										+ httpsTransactionCount
										+ proxyTransactionCount
										+ siteClipperTransactionCount
										+ siteClipperScreenclassCount
										+ siteClipperCriteriaCount
										+ sequenceCount
										+ stepCount
										+ sequenceVariableCount
										+ sequenceJavascriptFunction
										+ poolCount
										+ referenceCount
										+ testcaseCount
										+ testcaseVariableCount;

								displayString = totalC8oObjects + " object(s)<br/>"														// ok
										+ connectorCount +" connector(s)";															// ok

								result.put(projectName, displayString);

								/*
								 * html connector
								 */

								if (htmltransactionCount > 0) {

									displayString =
											(htmlScreenclassCount>0 ? "&nbsp;screenclassCount = " + htmlScreenclassCount + "<br/>" : "")											// ok
											+ (htmlCriteriaCount>0 ? "&nbsp;criteriaCount = " + htmlCriteriaCount + "<br/>" : "")
											+ (htmlExtractionRuleCount>0 ? "&nbsp;extractionRuleCount = " + htmlExtractionRuleCount + "<br/>" : "")
											+ "&nbsp;transactionCount = " + htmltransactionCount + "<br/>"											// ok
											+ (htmlTransactionVariableCount>0 ? "&nbsp;transactionVariableCount = " + htmlTransactionVariableCount + "<br/>" : "") + ")";

									result.put("HTML connector", displayString);
								}

								/*
								 * javelin connector
								 */
								if (javelinScreenclassCount > 0) {

									displayString =
											"&nbsp;screenclassCount = " + javelinScreenclassCount + "<br/>"											// ok
											+ (javelinCriteriaCount>0 ? "&nbsp;criteriaCount = " + javelinCriteriaCount + "<br/>" : "")
											+ (javelinExtractionRuleCount>0 ? "&nbsp;extractionRuleCount = " + javelinExtractionRuleCount + "<br/>" : "")
											+ (javelinTransactionCount>0 ? "&nbsp;transactionCount = " + javelinTransactionCount + "<br/>" : "")											// ok
											+ "&nbsp;handlerCount (Entry = " + javelinEntryHandlerCount + ", Exit = " + javelinExitHandlerCount + ", Screenclass = " + javelinHandlerCount + "), total = "
											+ (int)(javelinEntryHandlerCount + javelinExitHandlerCount + javelinHandlerCount) + " in " + javelinJavascriptLines + " lines<br/>"
											+ (javelinTransactionVariableCount>0 ? "&nbsp;variableCount = " + javelinTransactionVariableCount : "");

									result.put("Javelin connector", displayString);
								}

								/*
								 * SQL connector
								 */
								if (sqlTransactionCount > 0) {

									displayString =
											"&nbsp;sqltransactionCount = " + sqlTransactionCount + "<br/>"											// ok
											+ (selectInQueryCount>0 ? "&nbsp;selectInQueryCount = " + selectInQueryCount + "<br/>" : "")											// ok
											+ (sqlTransactionVariableCount>0 ? "&nbsp;transactionVariableCount = " + sqlTransactionVariableCount : "");

									if (sheetCount > 0) {
										displayString +=
												"<br/>Sheets<br/>"
														+ "&nbsp;sheetCount = " + sheetCount;
									}

									result.put("SQL connector", displayString);
								}

								/*
								 * Http connector
								 */
								if(httpConnectorCount>0) {
									displayString =
											"&nbsp;connectorCount = " + httpConnectorCount + "<br/>";
								}
								if (jsonHttpTransactionCount > 0) {

									displayString +=
											"&nbsp;JSONTransactionCount = " + jsonHttpTransactionCount + "<br/>"									// ok
											+ (xmlHttpTransactionCount>0 ? "&nbsp;XmlTransactionCount = " + xmlHttpTransactionCount + "<br/>" : "")	// ok
											+ (httpTransactionCount>0 ? "&nbsp;HTTPtransactionCount = " + httpTransactionCount : "");

									result.put("HTTP connector", displayString);
								}

								/*
								 * Https connector
								 */
								if (httpsConnectorCount > 0) {
									displayString =
											"&nbsp;connectorCount = " + httpsConnectorCount + "<br/>"
													+ (jsonHttpsTransactionCount>0 ? "&nbsp;JSONTransactionCount = " + jsonHttpsTransactionCount + "<br/>" :"")		// ok
													+ (xmlHttpsTransactionCount>0 ? "&nbsp;XmlTransactionCount = " + xmlHttpsTransactionCount + "<br/>"	: "")		// ok
													+ (httpsTransactionCount>0 ? "&nbsp;HTTPStransactionCount = " + httpsTransactionCount : "");														// ok

									result.put("HTTPS connector", displayString);
								}

								/*
								 * Proxy connector
								 */
								if (proxyTransactionCount > 0) {

									displayString =
											"&nbsp;TransactionCount = " + proxyTransactionCount;

									result.put("Proxy connector", displayString);
								}

								/*
								 * Siteclipper connector
								 */
								if (siteClipperTransactionCount > 0) {

									displayString =
											"&nbsp;TransactionCount = " + siteClipperTransactionCount + "<br/>"											// ok
											+ (siteClipperScreenclassCount>0 ? "&nbsp;screenclassCount = " + siteClipperScreenclassCount + "<br/>" :"")	// ok
											+ (siteClipperCriteriaCount>0 ? "&nbsp;criteriaCount = " + siteClipperCriteriaCount : "");

									result.put("SiteClipper connector", displayString);
								}

								/*
								 * Sequencer
								 */
								if (sequenceCount > 0) {

									displayString =
											"&nbsp;sequenceCount = " + sequenceCount + "<br/>"														// ok
											+ (stepCount>0 ? "&nbsp;stepCount = " + stepCount + "<br/>"	: "")														// ok
											+ (sequenceVariableCount>0 ? "&nbsp;variableCount = " + sequenceVariableCount + "<br/>" : "")
											+ "&nbsp;javascriptCode = " + sequenceJavascriptFunction + " functions in " + sequenceJavascriptLines + " lines"
											+  ((boolean)(sequenceJavascriptFunction == 0) ? " (declarations or so)":"");

									result.put("Sequencer", displayString);
								}

								// 								displayString += " reqVariableCount = " + reqVariableCount + "\r\n";

								if (poolCount > 0) {

									displayString =
											"&nbsp;poolCount = " + poolCount;

									result.put("Pools", displayString);
								}

								if (referenceCount > 0) {

									displayString =
											"&nbsp;referenceCount = " + referenceCount;

									result.put("References", displayString);
								}

								if (testcaseCount > 0) {

									displayString =
											"&nbsp;testcaseCount = " + testcaseCount + "<br/>"
													+ (testcaseVariableCount>0 ? "&nbsp;testcaseVariableCount = " + testcaseVariableCount : "");

									result.put("Test cases", displayString);
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
						}

						@Override
						protected void walk(DatabaseObject databaseObject) throws Exception {
							depth++;

							// String name = databaseObject.getName();

							// deal with connectors
							if (databaseObject instanceof Connector) {
								if (databaseObject instanceof HttpConnector) {
									if (((HttpConnector)databaseObject).isHttps())
										httpsConnectorCount++;
									else
										httpConnectorCount++;
								}
								else
									if (databaseObject instanceof CicsConnector) {
										cicsConnectorCount++;
									}
									else
										if (databaseObject instanceof SiteClipperConnector) {
											siteClipperConnectorCount++;
										}
										else
											if (databaseObject instanceof SqlConnector) {
												sqlConnectorCount++;
											}
											else
												if (databaseObject instanceof JavelinConnector) {
													javelinConnectorCount++;
												}
							}
							/*
							else
							if (databaseObject instanceof Reference) {
								referenceCount++;
							}
							 */
							else // deal with screenclasses
								if (databaseObject instanceof ScreenClass) {
									if (databaseObject instanceof JavelinScreenClass) {	// deal with javelinScreenClasses
										javelinScreenclassCount++;
									}
									else
										if (databaseObject instanceof SiteClipperScreenClass) {	// deal with siteClipperScreenClasses
											siteClipperScreenclassCount++;
										}
										else {												// deal with html ScreenClasses
											htmlScreenclassCount++;
										}
								}
								else
									if (databaseObject instanceof Criteria) {
										if (databaseObject.getParent() instanceof JavelinScreenClass) {
											javelinCriteriaCount++;
										}
										else
											if (databaseObject.getParent() instanceof SiteClipperScreenClass) {
												siteClipperCriteriaCount++;
											}
											else {
												htmlCriteriaCount++;
											}
									}
									else
										if (databaseObject instanceof ExtractionRule) {
											if (databaseObject.getParent() instanceof JavelinScreenClass) {
												javelinExtractionRuleCount++;
											}
											else {
												htmlExtractionRuleCount++;
											}
										}
										else
											if (databaseObject instanceof Transaction) {
												if (databaseObject instanceof TransactionWithVariables) {
													if (databaseObject instanceof JsonHttpTransaction) {
														if (((HttpConnector)databaseObject.getParent()).isHttps())
															jsonHttpsTransactionCount++;
														else
															jsonHttpTransactionCount++;
													}
													else
														if (databaseObject instanceof HttpTransaction) {
															if (((HttpConnector)databaseObject.getParent()).isHttps())
																httpsTransactionCount++;
															else
																httpTransactionCount++;
														}
														else
															if (databaseObject instanceof XmlHttpTransaction) {
																if (((HttpConnector)databaseObject.getParent()).isHttps())
																	xmlHttpsTransactionCount++;
																else
																	xmlHttpTransactionCount++;
															}
															else
																if (databaseObject instanceof ProxyTransaction) {
																	proxyTransactionCount++;
																}
																else
																	if (databaseObject instanceof SiteClipperTransaction) {
																		siteClipperTransactionCount++;
																	}
																	else
																		if (databaseObject instanceof JavelinTransaction) {
																			JavelinTransaction javelinTransaction = (JavelinTransaction)databaseObject;

																			// Functions
																			String line;
																			int lineNumber = 0;
																			BufferedReader br = new BufferedReader(new StringReader(javelinTransaction.handlers));

																			while ((line = br.readLine()) != null) {
																				line = line.trim();
																				lineNumber++;
																				if (line.startsWith("function ")) {
																					try {
																						String functionName = line.substring(9, line.indexOf(')') + 1);

																						if (functionName.endsWith(JavelinTransaction.EVENT_ENTRY_HANDLER + "()")) {
																							// TYPE_FUNCTION_SCREEN_CLASS_ENTRY
																							javelinEntryHandlerCount++;
																						} else if (functionName.endsWith(JavelinTransaction.EVENT_EXIT_HANDLER + "()")) {
																							// TYPE_FUNCTION_SCREEN_CLASS_EXIT
																							javelinExitHandlerCount++;
																						} else {
																							// TYPE_OTHER
																							javelinHandlerCount++;
																						}
																					} catch(StringIndexOutOfBoundsException e) {
																						// Ignore
																					}
																				}
																			}

																			// compute total number of lines of javascript
																			javelinJavascriptLines += lineNumber;

																			javelinTransactionCount++;
																		}
																		else
																			if (databaseObject instanceof SqlTransaction) {
																				SqlTransaction sqlTransaction = (SqlTransaction)databaseObject;
																				/*
																				 * count the number of SELECT
																				 */
																				String query = sqlTransaction.getSqlQuery();
																				if (query != null) {
																					query = query.toLowerCase();
																					String pattern = "select";
																					int lastIndex = 0;

																					while(lastIndex != -1) {
																						lastIndex = query.indexOf(pattern, lastIndex);
																						if (lastIndex != -1) {
																							selectInQueryCount++;
																							lastIndex += pattern.length();
																						}
																					}
																				}

																				sqlTransactionCount++;
																			}

													transactionWithVariablesCount++;
												}
												else { // transaction with no variables
													transactionCount++;
												}
											}
											else // deal with variables
												if (databaseObject instanceof Variable) {
													if (databaseObject.getParent() instanceof Transaction) {
														if (databaseObject.getParent() instanceof JavelinTransaction) {
															javelinTransactionVariableCount++;
														}
														else
															if (databaseObject.getParent() instanceof SqlTransaction) {
																sqlTransactionVariableCount++;
															}
															else { // should be zero
																transactionVariableCount++;
															}
													}
													else
														if (databaseObject.getParent() instanceof Sequence) {
															sequenceVariableCount++;
														}
														else
															if (databaseObject.getParent() instanceof TestCase) {
																testcaseVariableCount++;
															}
												}
												else
													if (databaseObject instanceof TestCase) {
														testcaseCount++;
													}
													else
														if (databaseObject instanceof Sequence) {
															sequenceCount++;
														}
														else
															if (databaseObject instanceof Step) {
																if (databaseObject instanceof SimpleStep) {
																	SimpleStep simpleStep = (SimpleStep)databaseObject;

																	// Functions
																	String line;
																	int lineNumber = 0;
																	BufferedReader br = new BufferedReader(new StringReader(simpleStep.getExpression()));

																	while ((line = br.readLine()) != null) {
																		line = line.trim();
																		lineNumber++;
																		if (line.startsWith("function ")) {
																			try {
																				sequenceJavascriptFunction++;
																			} catch(StringIndexOutOfBoundsException e) {
																				// Ignore
																			}
																		}
																	}

																	sequenceJavascriptLines += lineNumber;
																	stepCount++;
																}
																else
																	stepCount++;
															}
															else
																if (databaseObject instanceof Sheet) {
																	sheetCount++;
																}
																else
																	if (databaseObject instanceof Pool) {
																		poolCount++;
																	}

							super.walk(databaseObject);
						}

					}.go(project);
				} catch (Exception e) {
					// Just ignore, should never happen
				}
			}
		}
		catch (Throwable e) {
			throw new Exception("Unable to compute statistics of the project!: \n"+e.getMessage());
		}
		finally {
		}
		return result;
	}
	
    private static final Pattern ID_YAML_PATTERN = Pattern.compile("-(\\d+)]\\:");
    private static final Pattern ID_XML_PATTERN = Pattern.compile("priority=\"(\\d+)\"");

    private static final AtomicLong lastTime = new AtomicLong(System.currentTimeMillis());
	
    private static long getNewPriority() {
        while (true) {
            long current = System.currentTimeMillis();
            long last = lastTime.get();
            long next = Math.max(current, last + 1);
            if (lastTime.compareAndSet(last, next)) {
                return next;
            }
        }
    }
    
	public static File changePiorities(File oldXml) throws Exception {
		File oldYaml = oldXml.getName().equals("c8oProject.yaml") ? oldXml : new File(oldXml.getParentFile(), "c8oProject.yaml");

		if (!oldXml.exists() && !oldYaml.exists()) {
			throw new Exception("File \"" + oldXml.getAbsolutePath() + "\" does not exist");
		}

		File newFile = null;

		if (!oldYaml.exists()) {
			newFile = oldXml;
			
			Map<String, String> priorities = new HashMap<String, String>(10);
			Path xmlPath = Path.of(newFile.getAbsolutePath());
			extractAndMapPriorities(xmlPath, priorities);
			replacePrioritiesInFile(xmlPath, priorities);
			priorities.clear();
		} else {
			newFile = oldYaml;
			
	        Map<String, String> priorities = new ConcurrentHashMap<String, String>(10);        
	        Path yamlPath = Path.of(newFile.getAbsolutePath());
	        Path c8oPath = Path.of(new File(newFile.getParentFile(), "_c8oProject").getAbsolutePath());	  
	        
	        // extract priorities and map to new ones
	        try (Stream<Path> paths = getC8oFiles(c8oPath, yamlPath)) {
	            paths.filter(Files::isRegularFile)
	                 .filter(path -> path.toString().endsWith(".yaml"))
	                 .parallel()
	                 .forEach(path -> {
	                	try {
	                		extractAndMapPriorities(path, priorities);
						} catch (Exception e) {
							Engine.logDatabaseObjectManager.warn("Unable to extract priorities from file \"" + path + "\"");
						}
	                 });
	        }
	        
	        // replace priorities in files
	        try (Stream<Path> paths = getC8oFiles(c8oPath, yamlPath)) {
	            paths.filter(Files::isRegularFile)
	                 .filter(path -> path.toString().endsWith(".yaml"))
	                 .parallel()
	                 .forEach(path -> {
	                	try {
	                		replacePrioritiesInFile(path, priorities);
						} catch (Exception e) {
							Engine.logDatabaseObjectManager.warn("Unable to update priorities in file \"" + path + "\"");
						}
	                 });
	        }
	        
	        priorities.clear();
		}
		return newFile;
	}
	
    private static Stream<Path> getC8oFiles(Path dirPath, Path filePath) throws IOException {
        Stream<Path> streamDir = Files.exists(dirPath)
                ? Files.walk(dirPath)
                : Stream.empty();

        Stream<Path> streamFile = (filePath != null && Files.exists(filePath))
                ? Stream.of(filePath)
                : Stream.empty();

        return Stream.concat(streamDir, streamFile).distinct();
    }
    
    private static void extractAndMapPriorities(Path filePath, Map<String, String> priorities) throws IOException {
    	boolean isYaml = filePath.toString().endsWith(".yaml");
        String content = Files.readString(filePath);
        Matcher matcher = isYaml ? ID_YAML_PATTERN.matcher(content) : ID_XML_PATTERN.matcher(content);
        while (matcher.find()) {
            Long originalId = Long.parseLong(matcher.group(1));
            priorities.put(String.valueOf(originalId), String.valueOf(getNewPriority()));					
        }
    }

    private static void replacePrioritiesInFile(Path inputPath, Map<String, String> priorities) throws IOException {
        Path tempPath = inputPath.resolveSibling(inputPath.getFileName() + ".tmp");

        try (Stream<String> lines = Files.lines(inputPath, StandardCharsets.UTF_8);
             BufferedWriter writer = Files.newBufferedWriter(
                     tempPath,
                     StandardCharsets.UTF_8,
                     StandardOpenOption.CREATE,
                     StandardOpenOption.TRUNCATE_EXISTING)) {

            lines.forEach(line -> {
                String nouvelleLigne = applyReplacements(line, priorities);
                try {
                    writer.write(nouvelleLigne);
                    writer.newLine();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }

        Files.move(tempPath, inputPath, StandardCopyOption.REPLACE_EXISTING);
    }

    private static String applyReplacements(String text, Map<String, String> priorities) {
        for (Map.Entry<String, String> e : priorities.entrySet()) {
            text = text.replace(e.getKey(), e.getValue());
        }
        return text;
    }
	
}
