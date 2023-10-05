package com.twinsoft.convertigo.eclipse.menus;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.PlatformUI;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.popup.actions.SequenceExecuteSelectedAction;
import com.twinsoft.convertigo.eclipse.popup.actions.TransactionExecuteSelectedFromStubAction;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class StubDynamicMenu extends ContributionItem {
	public StubDynamicMenu() {

	}

	public StubDynamicMenu(String id) {
		super(id);
	}

	@Override
	public void fill(Menu menu, int index) {
		ISelection selection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			Object element = structuredSelection.getFirstElement();
			if (element instanceof IFile) {
				final File file = ((IFile) element).getRawLocation().makeAbsolute().toFile();
				if (isStub(file)) {
					final Map<String, String> attributes = getDocAttributes(file);
					final String projectName = attributes.get("project");
					final String sequenceName = attributes.get("sequence");
					final String connectorName = attributes.get("connector");
					final String transactionName = attributes.get("transaction");

					if (!projectName.isBlank()) {
						// get menu item image
						Image image = null;
						try {
							image = ConvertigoPlugin.getDefault()
									.getStudioIcon("icons/studio/transaction_execute_selected.gif");
						} catch (Exception e) {
						}

						// create the menu item
						MenuItem menuItem = new MenuItem(menu, SWT.CHECK, index);
						menuItem.setImage(image);
						menuItem.setText("Execute As Convertigo Stub");
						menuItem.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent e) {
								try {
									ProjectExplorerView pev = ConvertigoPlugin.getDefault().getProjectExplorerView();

									// for a sequence stub
									if (!sequenceName.isBlank()) {
										// select sequence in tree
										Sequence sequence = Engine.theApp.databaseObjectsManager
												.getOriginalProjectByName(projectName)
												.getSequenceByName(sequenceName);
										pev.setSelectedTreeObject(pev.findTreeObjectByUserObject(sequence));

										// run action
										String stubfileName = file.getName();
										SequenceExecuteSelectedAction action = new SequenceExecuteSelectedAction(stubfileName);
										action.setId(action.getId() + "FromStub"); // important !
										action.run();
									}
									// for a transaction stub
									else if (!connectorName.isBlank() && !transactionName.isBlank()) {
										// select transaction in tree
										Transaction transaction = Engine.theApp.databaseObjectsManager
												.getOriginalProjectByName(projectName).getConnectorByName(connectorName)
												.getTransactionByName(transactionName);
										pev.setSelectedTreeObject(pev.findTreeObjectByUserObject(transaction));

										// run action
										String stubfileName = file.getName();
										TransactionExecuteSelectedFromStubAction action = new TransactionExecuteSelectedFromStubAction(stubfileName);
										action.run();
									}
								} catch (Throwable t) {
									t.printStackTrace();
								}
							}
						});
					}
				}
			}
		}
	}

	static private boolean isStub(File file) {
		if ("xml".equals(FilenameUtils.getExtension(file.getName()))) {
			if (file.getParent().endsWith("stubs")) {
				return true;
			}
		}
		return false;
	}

	static private Map<String, String> getDocAttributes(File xmlFile) {
		Map<String, String> map = new HashMap<String, String>();
		try {
			Document xmlDoc = XMLUtils.parseDOM(xmlFile);
			Element xmlRoot = xmlDoc.getDocumentElement();
			NamedNodeMap nnm = xmlRoot.getAttributes();
			for (int i = 0; i < nnm.getLength(); i++) {
				Node node = nnm.item(i);
				map.put(node.getNodeName(), node.getNodeValue());
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return map;
	}
}
