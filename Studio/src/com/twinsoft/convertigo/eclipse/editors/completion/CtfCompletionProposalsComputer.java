package com.twinsoft.convertigo.eclipse.editors.completion;

import java.util.List;
import java.util.Vector;

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

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ViewContentProvider;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.UnloadedProjectTreeObject;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

public class CtfCompletionProposalsComputer implements  ICompletionProposalComputer {

	private static String ctfAttributes[][] = {
		{"data-c8o-call=\"\"","Call a Convertigo sequence or transaction"},
		{"data-c8o-listen=\"\"","Listen to a Convertigo sequence or transaction"},
		{"data-c8o-call-mode=\"auto\"","Call mode to be automatic"},
		{"data-c8o-each=\"\"","Iterate on selector for all childs"}
	};
	
	private static String ctfTemplates[] = {
		"__=tag__",
		"__{\"find\":\"selector\", \"attr\":\"name\"}__"
	};

	private List<String> projectNames = Engine.theApp.databaseObjectsManager.getAllProjectNamesList();
	
	
	private int findCharReverse(char c, IDocument doc, int offset)
	{
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
	
	private List<String> getSequenceList(String projectName) throws EngineException {
		Vector<String> Sequences = new Vector<String>();
		
		ProjectExplorerView projectExplorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();
		Project project = null;
		TreeObject projectTreeObject = ((ViewContentProvider) projectExplorerView.viewer
				.getContentProvider()).getProjectRootObject(projectName);
		if (projectTreeObject instanceof UnloadedProjectTreeObject) {
			project = Engine.theApp.databaseObjectsManager.getProjectByName(projectName);
		} else {
			project = projectExplorerView.getProject(projectName);
		}		
		List<Sequence> sequences = project.getSequencesList();
		if (!sequences.isEmpty()) {
			for (Sequence sequence : sequences) {
				String sequenceName = sequence.getName();
				Sequences.add(sequenceName);
			}
		}		
		return Sequences;
	}

	private Image imageCtf = new Image(Display.getCurrent(), getClass().getResourceAsStream(
													"/com/twinsoft/convertigo/eclipse/dialogs/images/key.png"));			

	@Override
	public List<CompletionProposal> computeCompletionProposals(
			CompletionProposalInvocationContext context, IProgressMonitor monitor) {
		// TODO Auto-generated method stub
		
		IDocument				document	= context.getDocument();
		int						offset		= context.getInvocationOffset();
		
		try {
			Vector<CompletionProposal> lProposals = new Vector<CompletionProposal>();
			boolean	attributeMode = false;
			
			int endTagOffset   = findCharReverse('>', document, offset);
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
				if (oFound != -1){
					int len = offset-oFound-1;
					String	alreadyTyped = document.get(oFound+1, len);
					
					for (int i=0; i < ctfTemplates.length ; i++) {
						if (ctfTemplates[i].startsWith(alreadyTyped)) {
							String entry = (String) ctfTemplates[i];
							
							//Set additional information
							String descr = "/** TODO : retrieve description*/ " + entry;
							
							//Set the string to display
							String showing = ctfTemplates[i];
							
							//Set the replacement string
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
				if (oFound != -1){
					// we are in the <xxxx case. search the first space before our offset
					oFound = findCharReverse(' ', document, offset);
					if (oFound != -1) {
						int len = offset-oFound-1;
						String	alreadyTyped = document.get(oFound+1, len);
						
						if (alreadyTyped.startsWith("data-c8o-call=\"") ||
							alreadyTyped.startsWith("data-c8o-listen=\"")) {
							// We are typing in the content of a data-c8o-call so handle the projects and sequences completion
							// extract the part typed in the attribute content
							alreadyTyped = alreadyTyped.substring(alreadyTyped.indexOf('"')+1);
							if (alreadyTyped.contains(".")) {
								// this in the form 'xxx.' or '.' so handle sequence completion
								String projectName = alreadyTyped.substring(0, alreadyTyped.indexOf('.'));
								alreadyTyped = alreadyTyped.substring(alreadyTyped.indexOf('.') +1);
								List<String> sequences = getSequenceList(projectName);
								for (int i=0; i< sequences.size(); i++) {
									if (sequences.get(i).startsWith(alreadyTyped)) {
										String entry = (String) sequences.get(i);
										//Set additional information
										String descr = entry + " : " + "Sequence Name";
										//Set the string to display
										String showing = sequences.get(i);
										//Set the replacement string
										String value = sequences.get(i).substring(alreadyTyped.length());
										IContextInformation info = new ContextInformation(showing, descr);
										lProposals.add(new CompletionProposal(value, offset, 0, value.length(), imageCtf, showing, info, descr));
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
							lProposals.add(new CompletionProposal(value, offset, 0, value.length(), imageCtf, showing, info, descr));

							// iterate through projects list
							for (int i=0; i< projectNames.size(); i++) {
								if (projectNames.get(i).startsWith(alreadyTyped)) {
									entry = (String) projectNames.get(i);
									//Set additional information
									descr = entry + " : " + "Project Name";
									//Set the string to display
									showing = projectNames.get(i);
									//Set the replacement string
									value = projectNames.get(i).substring(alreadyTyped.length());
									info = new ContextInformation(showing, descr);
									lProposals.add(new CompletionProposal(value, offset, 0, value.length(), imageCtf, showing, info, descr));
								}
							}
							return lProposals;
						}
						
						
						for (int i=0; i < ctfAttributes.length ; i++) {
							if (ctfAttributes[i][0].startsWith(alreadyTyped)) {
								String entry = (String) ctfAttributes[i][0];
								
								//Set additional information
								String descr = entry + " : " + ctfAttributes[i][1];
								
								//Set the string to display
								String showing = ctfAttributes[i][0];
								
								//Set the replacement string
								String value = ctfAttributes[i][0].substring(alreadyTyped.length());
								
								IContextInformation info = new ContextInformation(showing, descr);
								lProposals.add(new CompletionProposal(value, offset, 0, value.length(), imageCtf, showing, info, descr));
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
	public List<?> computeContextInformation(
			CompletionProposalInvocationContext context, IProgressMonitor monitor) {
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
