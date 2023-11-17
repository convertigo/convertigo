/*
 * Copyright (c) 2001-2023 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.menus;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.popup.actions.SequenceExecuteSelectedAction;
import com.twinsoft.convertigo.eclipse.popup.actions.TransactionExecuteSelectedFromStubAction;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
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
			if (element instanceof DatabaseObjectTreeObject) {
				DatabaseObjectTreeObject dboto = (DatabaseObjectTreeObject) element;
				DatabaseObject dbo = dboto.getObject();
				if (dbo instanceof Sequence || dbo instanceof Transaction) {
					String dir = Engine.projectDir(dbo.getProject().getName());
					try {
						Set<String> set = listStubFiles(dir + "/stubs", dbo);
						if (!set.isEmpty()) {

							// get menu item image
							Image image = null;
							try {
								image = ConvertigoPlugin.getDefault()
										.getStudioIcon("icons/studio/transaction_execute_selected.gif");
							} catch (Exception e) {
							}

							// create the menu item and sub dropdown menu
							MenuItem menuItem = new MenuItem(menu, SWT.CASCADE, index);
							menuItem.setText("Execute from stub");
							menuItem.setImage(image);

							Menu submenu = new Menu(menu.getParent(), SWT.DROP_DOWN);
							menuItem.setMenu(submenu);

							// add action for each stub file
							for (final String stubfileName : set) {
								File file = new File(dir + "/stubs/" + stubfileName);
								if (file.exists()) {
									if (isStubOf(file, dbo)) {
										MenuItem fileItem = new MenuItem(submenu, SWT.PUSH);
										fileItem.setText(stubfileName);
										fileItem.addSelectionListener(new SelectionAdapter() {
											public void widgetSelected(SelectionEvent e) {
												try {
													// for a sequence stub
													if (dbo instanceof Sequence) {
														SequenceExecuteSelectedAction action = new SequenceExecuteSelectedAction(stubfileName);
														action.setId(action.getId() + "FromStub"); // important !
														action.run();
													}
													// for a transaction stub
													else if (dbo instanceof Transaction) {
														TransactionExecuteSelectedFromStubAction action = new TransactionExecuteSelectedFromStubAction(stubfileName);
														action.run();
													}
												} catch (Throwable t) {}
											}
										});
									}
								}
							}
						}
					} catch (Exception e) {}
				}
			} else if (element instanceof IFile) {
				final File file = ((IFile) element).getRawLocation().makeAbsolute().toFile();
				if (isStub(file)) {
					Map<String, String> attributes = getDocAttributes(file);
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
												.getOriginalProjectByName(projectName).getSequenceByName(sequenceName);
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
								} catch (Throwable t) {}
							}
						});
					}
				}
			}
		}
	}

	static boolean isStubOf(File file, DatabaseObject dbo) {
		Map<String, String> attributes = getDocAttributes(file);
		return dbo instanceof Sequence ? dbo.getName().equals(attributes.get("sequence"))
				: dbo instanceof Transaction
						? dbo.getName().equals(attributes.get("transaction"))
								&& dbo.getParent().getName().equals(attributes.get("connector"))
						: false;
	}

	static public Set<String> listStubFiles(String dir, DatabaseObject dbo) throws IOException {
		try (Stream<Path> stream = Files.list(Paths.get(dir))) {
			return stream.filter((path) -> {
				File file = new File(path.toString());
				return !Files.isDirectory(path) && isStubOf(file, dbo);
			}).map(Path::getFileName).map(Path::toString).collect(Collectors.toCollection(TreeSet<String>::new));
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
		} catch (Throwable t) {}
		return map;
	}
}
