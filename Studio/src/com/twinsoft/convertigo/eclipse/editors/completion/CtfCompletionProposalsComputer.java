/*
* Copyright (c) 2016 Convertigo. All Rights Reserved.
*
* The copyright to the computer  program(s) herein  is the property
* of Convertigo.
* The program(s) may  be used  and/or copied  only with the written
* permission  of  Convertigo  or in accordance  with  the terms and
* conditions  stipulated  in the agreement/contract under which the
* program(s) have been supplied.
*
* Convertigo makes  no  representations  or  warranties  about  the
* suitability of the software, either express or implied, including
* but  not  limited  to  the implied warranties of merchantability,
* fitness for a particular purpose, or non-infringement. Convertigo
* shall  not  be  liable for  any damage  suffered by licensee as a
* result of using,  modifying or  distributing this software or its
* derivatives.
*/

/*
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.eclipse.editors.completion;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext;
import org.eclipse.wst.sse.ui.contentassist.ICompletionProposalComputer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ViewContentProvider;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.UnloadedProjectTreeObject;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class CtfCompletionProposalsComputer implements ICompletionProposalComputer {
	
	private List<Entry> ctfAttributes = new ArrayList<Entry>();
	
	private static String ctfTemplates[] = { "__=tag__", "__{\"find\":\"selector\", \"attr\":\"name\"}__" };

	private List<String> projectNames = Engine.theApp.databaseObjectsManager.getAllProjectNamesList();

	
	public CtfCompletionProposalsComputer() {	
		loadCtfCompletionTable();
	}
	
	private String getTextValue(Element ele, String tagName) {
		String textVal = null;
		NodeList nl = ele.getElementsByTagName(tagName);

		if ((nl != null) && (nl.getLength() > 0)) {
			try {
				Element el = (Element) nl.item(0);
				textVal = el.getFirstChild().getNodeValue();
			}
			catch(Exception e) {
				textVal = "";
			}
		}

		return textVal;
	}

	private List<String> getParametersList(Element ele, String tagName) {
		List<String> parameters = new ArrayList<String>();
		
		NodeList listOfParameters = ele.getElementsByTagName(tagName);
		if ((listOfParameters != null) && (listOfParameters.getLength() > 0)) {
			try {
				for(int i = 0; i < listOfParameters.getLength(); i++) {
					Element item = (Element)listOfParameters.item(i);
					parameters.add(item.getTextContent());
				}
			}
			catch(Exception e) {				
			}
		}

		return parameters;
	}
	
	private Entry getEntry(Element empEl, int iconId) {

		// for each <Entry> element get text values of entry
		String keyword = getTextValue(empEl, "keyword");
		String definition = getTextValue(empEl, "definition");
		List<String> parameters = getParametersList(empEl, "item");

		// Create a new Entry with the value read from the xml nodes
		return new Entry(keyword, definition, iconId, parameters);
	}

	void loadSubDictionaries(NodeList ctfNode, int iconId) {
		try {
			Element ctfElement = (Element) ctfNode.item(0);
			NodeList listOfEntries = ctfElement.getElementsByTagName("entry"); 
	
			if (listOfEntries != null && listOfEntries.getLength() > 0) {
				for (int i = 0; i < listOfEntries.getLength(); i++) {
					// retrieve the ith Entry element
					Element el = (Element)listOfEntries.item(i);
					// get the Entry object text values and add it to list
					ctfAttributes.add(getEntry(el, iconId));
				}
			}
		} catch (Exception e) {
		}
	}
	
	void loadCtfCompletionTable() {
		try {
			Document doc = XMLUtils.getDefaultDocumentBuilder().parse(getClass().getResourceAsStream("c8oCompletionDict.xml"));
			// load all dictionary subsets one by one into the same table
			loadSubDictionaries(doc.getElementsByTagName("ctf"), Entry.ICONID_CTF);
			loadSubDictionaries(doc.getElementsByTagName("jquerymobile"), Entry.ICONID_JQM);
		} catch (Exception e) {;}
	}

	private int findCharReverse(char c, IDocument doc, int offset) {
		try {
			while (offset-- > 0) {
				char current = doc.getChar(offset);
				if (current == c)
					return offset;
			}
			return -1;
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			return -1;
		}
	}

	private Project getProjectByName(String projectName) throws EngineException {
		Project project;

		ProjectExplorerView projectExplorerView = ConvertigoPlugin.getDefault()
				.getProjectExplorerView();
		TreeObject projectTreeObject = ((ViewContentProvider) projectExplorerView.viewer
				.getContentProvider()).getProjectRootObject(projectName);
		if (projectTreeObject instanceof UnloadedProjectTreeObject) {
			project = Engine.theApp.databaseObjectsManager.getProjectByName(projectName);
		} else {
			project = projectExplorerView.getProject(projectName);
		}
		return project;
	}

	private List<String> getTransactionList(String projectName,
			String connectorName) throws EngineException {
		List<String> Transactions = new ArrayList<String>();
		
		Project project = getProjectByName(projectName);
		Connector connector = project.getConnectorByName(connectorName);

		List<Transaction> transactions = connector.getTransactionsList();
		if (!transactions.isEmpty()) {
			for (Transaction transaction : transactions) {
				String transactionName = transaction.getName();
				Transactions.add(transactionName);
			}
		}
		return Transactions;
	}

	private List<String> getConnectorList(String projectName)
			throws EngineException {
		List<String> Connectors = new ArrayList<String>();

		Project project = getProjectByName(projectName);
		List<Connector> connectors = project.getConnectorsList();
		if (!connectors.isEmpty()) {
			for (Connector connector : connectors) {
				String connectorName = connector.getName();
				Connectors.add(connectorName);
			}
		}
		return Connectors;
	}

	private List<String> getSequenceList(String projectName)
			throws EngineException {
		List<String> Sequences = new ArrayList<String>();

		Project project = getProjectByName(projectName);
		List<Sequence> sequences = project.getSequencesList();
		if (!sequences.isEmpty()) {
			for (Sequence sequence : sequences) {
				String sequenceName = sequence.getName();
				Sequences.add(sequenceName);
			}
		}
		return Sequences;
	}

	private String getClosestProjectName(String projectName) {
		for (int i = 0; i < projectNames.size(); i++) {
			if (projectNames.get(i).startsWith(projectName.substring(0, 3)))
				return projectNames.get(i); 
		}
		
		return "";
	}
/*	
	private int levenshteinDistance (CharSequence lhs, CharSequence rhs) {                          
	    int len0 = lhs.length() + 1;                                                     
	    int len1 = rhs.length() + 1;                                                     
	                                                                                    
	    // the array of distances                                                       
	    int[] cost = new int[len0];                                                     
	    int[] newcost = new int[len0];                                                  
	                                                                                    
	    // initial cost of skipping prefix in String s0                                 
	    for (int i = 0; i < len0; i++) cost[i] = i;                                     
	                                                                                    
	    // dynamically computing the array of distances                                  
	                                                                                    
	    // transformation cost for each letter in s1                                    
	    for (int j = 1; j < len1; j++) {                                                
	        // initial cost of skipping prefix in String s1                             
	        newcost[0] = j;                                                             
	                                                                                    
	        // transformation cost for each letter in s0                                
	        for(int i = 1; i < len0; i++) {                                             
	            // matching current letters in both strings                             
	            int match = (lhs.charAt(i - 1) == rhs.charAt(j - 1)) ? 0 : 1;             
	                                                                                    
	            // computing cost for each transformation                               
	            int cost_replace = cost[i - 1] + match;                                 
	            int cost_insert  = cost[i] + 1;                                         
	            int cost_delete  = newcost[i - 1] + 1;                                  
	                                                                                    
	            // keep minimum cost                                                    
	            newcost[i] = Math.min(Math.min(cost_insert, cost_delete), cost_replace);
	        }                                                                           
	                                                                                    
	        // swap cost/newcost arrays                                                 
	        int[] swap = cost; cost = newcost; newcost = swap;                          
	    }                                                                               
	                                                                                    
	    // the distance is the cost for transforming all letters in both strings        
	    return cost[len0 - 1];                                                          
	}
*/	
	private Image imageCtf = new Image(Display.getCurrent(), getClass().getResourceAsStream("/com/twinsoft/convertigo/eclipse/editors/images/completion_ctf.png"));

	private Image imageJqm = new Image(Display.getCurrent(), getClass().getResourceAsStream("/com/twinsoft/convertigo/eclipse/editors/images/completion_jqm.png"));

	private Image imageProject = new Image(Display.getCurrent(), getClass().getResourceAsStream("/com/twinsoft/convertigo/beans/core/images/c8o_color_16x16.png"));

	private Image imageSequence = new Image(Display.getCurrent(), getClass().getResourceAsStream("/com/twinsoft/convertigo/beans/core/images/sequence_color_16x16.png"));

	private Image imageConnector = new Image(Display.getCurrent(), getClass().getResourceAsStream("/com/twinsoft/convertigo/beans/core/images/connector_color_16x16.png"));

	private Image imageTransaction = new Image(Display.getCurrent(), getClass().getResourceAsStream("/com/twinsoft/convertigo/beans/core/images/transaction_color_16x16.png"));

	@Override
	public List<CompletionProposal> computeCompletionProposals(CompletionProposalInvocationContext context, IProgressMonitor monitor) {
		IDocument document = context.getDocument();
		int offset = context.getInvocationOffset();

		try {
			List<CompletionProposal> lProposals = new ArrayList<CompletionProposal>();
			boolean attributeMode = false;

			int endTagOffset = findCharReverse('>', document, offset);
			int startTagOffset = findCharReverse('<', document, offset);

			if (endTagOffset == -1)
				attributeMode = true;

			if (startTagOffset == -1)
				attributeMode = false;

			if (endTagOffset > startTagOffset)
				attributeMode = false;
			else
				attributeMode = true;

			if (!attributeMode) {
				// we are in a Text node, so display the templates proposals
				int oFound = findCharReverse('>', document, offset);
				if (oFound != -1) {
					int len = offset - oFound - 1;
					String alreadyTyped = document.get(oFound + 1, len);

					for (int i = 0; i < ctfTemplates.length; i++) {
						if (ctfTemplates[i].startsWith(alreadyTyped)) {
							String entry = (String) ctfTemplates[i];

							// Set additional information
							String descr = "/** TODO : retrieve description*/ " + entry;

							// Set the string to display
							String showing = ctfTemplates[i];

							// Set the replacement string
							String value = ctfTemplates[i].substring(alreadyTyped.length());

							IContextInformation info = new ContextInformation(showing, descr);
							lProposals.add(new CompletionProposal(value, offset, 0, value.length(), imageCtf, showing, info, descr));
						}
					}
					return lProposals;
				}
			}

			if (attributeMode) {
				// we are within a tag, so display attribute proposals...
				int oFound = findCharReverse('<', document, offset);
				if (oFound != -1) {
					//
					// we are in the <xxxx case. search the first space before our offset
					//
					oFound = findCharReverse(' ', document, offset);
					
					if (oFound != -1) {
						int len = offset - oFound - 1;
						String alreadyTyped = document.get(oFound + 1, len);

						/*
						 * deal with c8o-call (find related transactions and sequences)
						 */
						if (alreadyTyped.startsWith("data-c8o-call=\"") || alreadyTyped.startsWith("data-c8o-listen=\"")) {
							// We are typing in the content of a data-c8o-call
							// so handle the projects and sequences completion
							// extract the part typed in the attribute content
							alreadyTyped = alreadyTyped.substring(alreadyTyped.indexOf('"') + 1);
							if (alreadyTyped.contains(".")) {
								//
								//this in the form 'xxx.' or '.' so handle sequence completion
								//
								String projectName = alreadyTyped.substring(0, alreadyTyped.indexOf('.'));

								// if projectName is empty (ie current), get the fullname from context
								if (projectName.equalsIgnoreCase(""))								
									projectName = FileBuffers.getTextFileBufferManager().getTextFileBuffer(document).getLocation().segment(0);
								
								String currentProjectName = projectName; 
								
								// if the project has no sequences, try to find a related project
								// by comparing names using levenshtein disatnce algorithm
								// commented out for the moment
/*								
								if (getSequenceList(projectName).isEmpty()) {
									String closest = projectName;
									int	dist, smallestDist = 65535;	// some high number
									for (int i = 0; i < projectNames.size(); i++) {
										if (!projectName.equalsIgnoreCase(projectNames.get(i))) {
											if ((dist = levenshteinDistance(projectName, projectNames.get(i))) < smallestDist) {
												smallestDist = dist;
												closest = projectNames.get(i);
											}
										}
									}

									projectName = closest;
								}
*/
								alreadyTyped = alreadyTyped.substring(alreadyTyped.indexOf('.') + 1);
								if (alreadyTyped.contains(".")) {
									//
									// this in the form 'xxx.XXX' or '..' so handle sequence completion
									//
									String connector = alreadyTyped.substring(0, alreadyTyped.indexOf('.'));
									alreadyTyped = alreadyTyped.substring(alreadyTyped.indexOf('.') + 1);
									
									try {
										List<String> transactions = getTransactionList(projectName, connector);
										for (int i = 0; i < transactions.size(); i++) {
											if (alreadyTyped.equalsIgnoreCase("") || transactions.get(i).startsWith(alreadyTyped)) {
												String entry = (String) transactions.get(i);
												// Set additional information
												String descr = entry + " : " + "Transaction name";
												// Set the string to display
												String showing = transactions.get(i);
												// Set the replacement string
												String value = transactions.get(i).substring(alreadyTyped.length());
												IContextInformation info = new ContextInformation(showing, descr);
												lProposals.add(new CompletionProposal(value, offset, 0, value.length(), imageTransaction, showing, info, descr));
											}
										}
										return lProposals;
									}
									catch(Exception ee) {
										return lProposals;
									}
								}

								List<String> sequences = getSequenceList(projectName);
								for (int i = 0; i < sequences.size(); i++) {
									if (alreadyTyped.equalsIgnoreCase("") || sequences.get(i).startsWith(alreadyTyped)) {
										String entry = (String) sequences.get(i);
										// Set additional information
										String descr = (projectName.equalsIgnoreCase(currentProjectName) ? "":"WARNING ") + entry + " : Sequence name" + (projectName.equalsIgnoreCase(currentProjectName) ? "":" is from project " + projectName);
										// Set the string to display
										String showing = sequences.get(i);
										// Set the replacement string
										String value = sequences.get(i).substring(alreadyTyped.length());
										IContextInformation info = new ContextInformation(showing, descr);
										lProposals.add(new CompletionProposal(value, offset, 0, value.length(), imageSequence, showing, info,descr));
									}
								}

								projectName = currentProjectName;
								
								List<String> connectors = getConnectorList(projectName);
								for (int i = 0; i < connectors.size(); i++) {
									if (connectors.get(i).startsWith(alreadyTyped)) {
										String entry = (String) connectors.get(i);
										// Set additional information
										String descr = entry + " : " + "Connector name";
										// Set the string to display
										String showing = connectors.get(i);
										// Set the replacement string
										String value = connectors.get(i).substring(alreadyTyped.length());
										IContextInformation info = new ContextInformation(showing, descr);
										lProposals.add(new CompletionProposal(value, offset, 0, value.length(), imageConnector, showing, info, descr));
									}
								}

								return lProposals;
							}

							// always add the '.' self project proposal
							String entry = ".";
							String descr = "My Own Project";
							String showing = ".";
							String value = ".";
							IContextInformation info = new ContextInformation(showing, descr);
							lProposals.add(new CompletionProposal(value, offset, 0, value.length(), imageProject, showing, info, descr));

							// iterate through projects list
							for (int i = 0; i < projectNames.size(); i++) {
								if (projectNames.get(i).startsWith(alreadyTyped)) {
									entry = (String) projectNames.get(i);
									// Set additional information
									descr = entry + " : " + "Project Name";
									// Set the string to display
									showing = projectNames.get(i);
									// Set the replacement string
									value = projectNames.get(i).substring(alreadyTyped.length());
									info = new ContextInformation(showing, descr);
									lProposals.add(new CompletionProposal(value, offset, 0, value.length(), imageProject, showing, info, descr));
								}
							}
							return lProposals;
						} // end deal with c8o-call

						String	keyword;
						String	definition;
						int		iconId;
						
						for (int i = 0; i < ctfAttributes.size(); i++) {
							keyword = ctfAttributes.get(i).getKeyword();
							
							if (keyword.startsWith(alreadyTyped) && (alreadyTyped.endsWith("=\"") || alreadyTyped.endsWith("(\""))) {
								definition = ctfAttributes.get(i).getDefinition();
								iconId = ctfAttributes.get(i).getIconId();
								List<String> parameters = ctfAttributes.get(i).getParameters();

								// Set additional information
								String descr = keyword + " : " + definition;

								// Set the replacement string
								String value = "";
								
								// Set the string to display
								String showing = "";
								
								for (int j = 0; j < parameters.size(); j++) {
									showing = parameters.get(j);
									value = parameters.get(j) + "\"";
									
									IContextInformation info = new ContextInformation(showing, descr);
									
									/*
									 * adjust icon type
									 */
									if (iconId == Entry.ICONID_CTF)
										lProposals.add(new CompletionProposal(value, offset, 0, value.length(), imageCtf, showing, info, descr));
									else 
										lProposals.add(new CompletionProposal(value, offset, 0, value.length(), imageJqm, showing, info, descr));
								}
								
								return lProposals;
							}
						}

						for (int i = 0; i < ctfAttributes.size(); i++) {
							keyword = ctfAttributes.get(i).getKeyword();
							definition = ctfAttributes.get(i).getDefinition();
							iconId = ctfAttributes.get(i).getIconId();

							if (keyword.startsWith(alreadyTyped)) {
								// Set additional information
								String descr = keyword + " : " + definition;

								// Set the string to display
								String showing = keyword;

								// Set the replacement string
								String value = "";

								if ((alreadyTyped.endsWith("=\"") || alreadyTyped.endsWith("(\""))) {
									value = keyword.substring(alreadyTyped.length());
								}
								else { 
									value = keyword.substring(alreadyTyped.length());
								}
								
								IContextInformation info = new ContextInformation(showing, descr);
								
								/*
								 * adjust icon type
								 */
								if (iconId == Entry.ICONID_CTF)
									lProposals.add(new CompletionProposal(value, offset, 0, value.length(), imageCtf, showing, info, descr));
								else 
									lProposals.add(new CompletionProposal(value, offset, 0, value.length(), imageJqm, showing, info, descr));
							}
						}
						return lProposals;
					}
				}
			}
			return lProposals;

		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public List<?> computeContextInformation(CompletionProposalInvocationContext context, IProgressMonitor monitor) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getErrorMessage() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void sessionEnded() {
		// TODO Auto-generated method stub
	}

	@Override
	public void sessionStarted() {
		// TODO Auto-generated method stub
	}
}

