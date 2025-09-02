/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.views.references;

import java.util.Arrays;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.RequestableStep;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.core.UrlMappingOperation;
import com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIActionStack;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicAction;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicInvoke;
import com.twinsoft.convertigo.beans.ngx.components.UISharedComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIUseShared;
import com.twinsoft.convertigo.beans.steps.SequenceStep;
import com.twinsoft.convertigo.beans.steps.TransactionStep;
import com.twinsoft.convertigo.beans.transactions.JavelinTransaction;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.CompositeEvent;
import com.twinsoft.convertigo.eclipse.editors.CompositeListener;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.UnloadedProjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.references.model.AbstractParentNode;
import com.twinsoft.convertigo.eclipse.views.references.model.DboNode;
import com.twinsoft.convertigo.eclipse.views.references.model.InformationNode;
import com.twinsoft.convertigo.eclipse.views.references.model.IsUsedByNode;
import com.twinsoft.convertigo.eclipse.views.references.model.RequiresNode;
import com.twinsoft.convertigo.eclipse.views.references.model.RootNode;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.helpers.WalkHelper;

public class ReferencesView extends ViewPart implements CompositeListener,
ISelectionListener, IPartListener2 {

	private TreeViewer treeViewer;
	private boolean isVisible = true;

	public void objectSelected(CompositeEvent compositeEvent) {

	}

	public void objectChanged(CompositeEvent compositeEvent) {

	}

	@Override
	public void createPartControl(Composite parent) {
		treeViewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		treeViewer.getTree().setHeaderVisible(true);
		treeViewer.setContentProvider(new ViewRefContentProvider());
		var first = new TreeViewerColumn(treeViewer, SWT.NONE);
		first.setLabelProvider(new DelegatingStyledCellLabelProvider(new ViewRefLabelProvider()));
		first.getColumn().setText("Source");
		first.getColumn().setWidth(300);
		var second = new TreeViewerColumn(treeViewer, SWT.NONE);
		second.setLabelProvider(new DelegatingStyledCellLabelProvider(new ViewRefLabelProvider(true)));
		second.getColumn().setText("Target");
		second.getColumn().setWidth(300);
		treeViewer.getTree().addListener(SWT.MouseDown, (Listener) event -> {
			try {
				var p = new Point(event.x, event.y);
				var item = treeViewer.getTree().getItem(p);
				if (item == null) return;

				int colIndex = -1;
				var cols = treeViewer.getTree().getColumns();
				for (int i = 0; i < cols.length; i++) {
					if (item.getBounds(i).contains(p)) { colIndex = i; break; }
				}
				if (colIndex < 0) return;

				var element = item.getData();

				if (element instanceof DboNode node && node.getTarget() != null) {
					handleSelectedObjectInRefView(colIndex == 0 ? node.getSource() : node.getTarget());
					event.doit = false; // prevent selection change in tree
				}
			} catch (Exception e) {
				ConvertigoPlugin.logException(e, "Error handling click in ReferencesView", true);
			}
		});
		treeViewer.setInput(null);
		treeViewer.expandAll();

		getSite().setSelectionProvider(treeViewer);
		getSite().getPage().addSelectionListener(this);
		getSite().getPage().addPartListener(this);
		isVisible = getSite().getPage().isPartVisible(this);
	}

	@Override
	public void dispose() {
		getSite().getPage().removeSelectionListener(this);
		getSite().getPage().removePartListener(this);
		super.dispose();
	}

	@Override
	public void setFocus() {

	}

	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (!isVisible) {
			return;
		}
		if (selection instanceof IStructuredSelection) {
			if (part instanceof ProjectExplorerView) {
				var firstElement = ((IStructuredSelection) selection).getFirstElement();

				if (firstElement instanceof DatabaseObjectTreeObject dbot) {
					handleDatabaseObjectSelection(dbot.getObject());
				} else {
					var root = new InformationNode(null, "root");
					root.addChild(new InformationNode(root, "References are not handled for this object"));
					treeViewer.setInput(root);
				}
			}
			else if (part == ReferencesView.this) {
				var firstElement = ((IStructuredSelection) selection).getFirstElement();
				handleSelectedObjectInRefView(firstElement);
			}
		}
	}

	private void handleDatabaseObjectSelection(DatabaseObject dbo) {
		var thinking = new InformationNode(null, "Thinking …");
		treeViewer.setInput(thinking);
		treeViewer.refresh();

		new Job("Build references for " + dbo.getName()) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					var selProject   = dbo instanceof Project ? (Project) dbo : dbo.getProject();
					var selProjName  = selProject != null ? selProject.getName() : dbo.getName();

					var root         = new RootNode();
					var selectedNode = new DboNode(root, dbo.getName(), dbo);
					root.addChild(selectedNode);

					var requiresNode = new RequiresNode(selectedNode, "Requires");
					var usedByNode   = new IsUsedByNode(selectedNode, "Is used by");

					// one walker, two modes
					buildReferences(RefMode.REQUIRES, dbo, selProjName, requiresNode, null);
					buildReferences(RefMode.USED_BY,  dbo, selProjName, usedByNode, null);

					if (!requiresNode.hasChildren()) {
						requiresNode.addChild(new InformationNode(requiresNode, "This project does not require any other project"));
					}
					selectedNode.addChild(requiresNode);

					if (!usedByNode.hasChildren()) {
						usedByNode.addChild(new InformationNode(usedByNode, "Nothing depends on this object"));
					}
					selectedNode.addChild(usedByNode);

					treeViewer.getTree().getDisplay().asyncExec(() -> {
						try {
							treeViewer.setInput(root);
							treeViewer.expandAll();
							for (var n: Arrays.asList(requiresNode, usedByNode)) {
								for (var c: n.getChildren()) {
									if (c instanceof DboNode dn && selProject.equals(dn.getTarget())) {
										treeViewer.setExpandedState(c,false);
									}
								}
							}
							treeViewer.refresh();
						} catch (Exception e) {
							ConvertigoPlugin.logException(e, "Error while updating references view", true);
						}
					});

					return Status.OK_STATUS;
				} catch (Exception e) {
					ConvertigoPlugin.logException(e, "Error while building references", true);
					treeViewer.getTree().getDisplay().asyncExec(() -> {
						treeViewer.setInput(new InformationNode(null, "Error while building references"));
					});
					return new Status(IStatus.ERROR, ConvertigoPlugin.PLUGIN_UNIQUE_ID, e.getMessage(), e);
				}
			}
		}.schedule();
	}

	/* ============================= helpers (factorization) ============================= */

	private enum RefMode { REQUIRES, USED_BY }

	/**
	 * Single walker for both "Requires" and "Used by".
	 * - REQUIRES: walk inside the selected object; anchor grouping under the TARGET project.
	 * - USED_BY : walk all projects; anchor grouping under the SOURCE project;
	 *             only emit when the reference points to the selected object (or inside it).
	 */
	private void buildReferences(RefMode mode, DatabaseObject selection, String selProjName,
			AbstractParentNode sectionNode, Map<String, DboNode> _unused) {

		var selIsProject         = selection instanceof Project;
		var selIsSequence        = selection instanceof Sequence;
		var selIsConnector       = selection instanceof Connector;
		var selIsTransaction     = selection instanceof Transaction;
		var selIsScreenClass     = selection instanceof ScreenClass;
		var selIsSharedComponent = selection instanceof UISharedComponent;
		var selIsActionStack     = selection instanceof UIActionStack;

		var selProject       = selIsProject ? (Project) selection : selection.getProject();
		var selSequenceName  = selIsSequence ? ((Sequence) selection).getName() : null;
		var selConnectorName = selIsTransaction ? ((Transaction) selection).getParentName()
				: (selIsConnector ? ((Connector) selection).getName() : null);
		var selTxnName       = selIsTransaction ? ((Transaction) selection).getName() : null;

		var selFqnSeq   = selIsSequence    ? (selProjName + "." + selSequenceName) : null;
		var selFqnTxn   = selIsTransaction ? (selProjName + "." + selConnectorName + "." + selTxnName) : null;
		var selConnPref = selIsConnector   ? (selProjName + "." + selConnectorName + ".") : null;

		// index scoped PER parent to avoid attaching children under the wrong project
		var indexByParent = new java.util.IdentityHashMap<AbstractParentNode, java.util.LinkedHashMap<String, DboNode>>();
		java.util.function.Function<AbstractParentNode, java.util.LinkedHashMap<String, DboNode>> idx =
				parent -> indexByParent.computeIfAbsent(parent, k -> new java.util.LinkedHashMap<>());
				java.util.function.BiFunction<AbstractParentNode, DatabaseObject, DboNode> ensure =
						(parent, obj) -> ensureNode(idx.apply(parent), parent, obj);

						java.util.function.BiFunction<String,String,String> effProjectName = (raw, fallback) ->
						(raw == null || raw.isEmpty()) ? fallback : raw;

						java.util.function.Consumer<Project> scan = (Project srcProject) -> {
							try {
								new WalkHelper() {
									@Override
									protected void walk(DatabaseObject o) throws Exception {

										/* ======================== RequestableStep ======================== */
										if (o instanceof RequestableStep rs) {
											// ---- SequenceStep
											if (rs instanceof SequenceStep ss) {
												if (mode == RefMode.REQUIRES) {
													var pName = effProjectName.apply(ss.getProjectName(), selProjName);
													try {
														var p   = getProjectByName(pName);
														if (p == null) { super.walk(o); return; }
														var seq = p.getSequenceByName(ss.getSequenceName());
														var pNode = ensure.apply(sectionNode, p);                 // top = TARGET project
														ensure.apply(pNode, seq).setSource(ss);                  // child = TARGET sequence
													} catch (Exception ignoreBroken) {
														// broken → ignore (no fallback setSource on project)
													}
												} else { // USED_BY
													var tgtPrj = effProjectName.apply(ss.getProjectName(), srcProject.getName());
													if (selIsProject && selProjName.equals(tgtPrj)) {
														try {
															var seq = selProject.getSequenceByName(ss.getSequenceName());
															var srcNode = ensure.apply(sectionNode, srcProject);   // top = SOURCE project
															ensure.apply(srcNode, seq).setSource(rs);              // child = precise TARGET
														} catch (Exception ignoreBroken) {}
													} else if (selIsSequence && selProjName.equals(tgtPrj) && selSequenceName.equals(ss.getSequenceName())) {
														var srcNode = ensure.apply(sectionNode, srcProject);
														ensure.apply(srcNode, selection).setSource(rs);
													}
												}
											}
											// ---- TransactionStep
											else if (rs instanceof TransactionStep ts) {
												if (mode == RefMode.REQUIRES) {
													var pName = effProjectName.apply(ts.getProjectName(), selProjName);
													try {
														var p    = getProjectByName(pName);
														if (p == null) { super.walk(o); return; }
														var conn = p.getConnectorByName(ts.getConnectorName());
														var pNode = ensure.apply(sectionNode, p);                 // top = TARGET project
														try {
															var txn = conn.getTransactionByName(ts.getTransactionName());
															ensure.apply(pNode, txn).setSource(rs);               // child = TARGET txn
														} catch (Exception noTxn) {
															ensure.apply(pNode, conn).setSource(rs);              // fallback: TARGET connector (valid)
														}
													} catch (Exception ignoreBroken) {
														// broken project/connector → ignore
													}
												} else { // USED_BY
													var tgtPrj = effProjectName.apply(ts.getProjectName(), srcProject.getName());
													var matchProject     = selIsProject     && selProjName.equals(tgtPrj);
													var matchConnector   = selIsConnector   && selProjName.equals(tgtPrj) && selConnectorName.equals(ts.getConnectorName());
													var matchTransaction = selIsTransaction && selProjName.equals(tgtPrj)
															&& selConnectorName.equals(ts.getConnectorName())
															&& selTxnName.equals(ts.getTransactionName());

													if (matchProject) {
														try {
															var conn = selProject.getConnectorByName(ts.getConnectorName());
															var srcNode = ensure.apply(sectionNode, srcProject);  // top = SOURCE project
															try {
																var txn = conn.getTransactionByName(ts.getTransactionName());
																ensure.apply(srcNode, txn).setSource(rs);
															} catch (Exception noTxn) {
																ensure.apply(srcNode, conn).setSource(rs);
															}
														} catch (Exception ignoreBroken) {}
													} else if (matchConnector) {
														try {
															var txn = ((Connector) selection).getTransactionByName(ts.getTransactionName());
															var srcNode = ensure.apply(sectionNode, srcProject);
															ensure.apply(srcNode, txn).setSource(rs);
														} catch (Exception ignoreBroken) {
															// if txn not found, nothing to add
														}
													} else if (matchTransaction) {
														var srcNode = ensure.apply(sectionNode, srcProject);
														ensure.apply(srcNode, selection).setSource(rs);
													}
												}
											}
										}

										/* ======================== UrlMappingOperation ======================== */
										else if (o instanceof UrlMappingOperation op) {
											var target = op.getTargetRequestable();
											if (target != null && !target.isEmpty()) {
												var parts = target.split("\\.");
												if (parts.length >= 2) {
													var pName = parts[0];

													if (mode == RefMode.REQUIRES) {
														try {
															var p = getProjectByName(pName);
															if (p == null) { super.walk(o); return; }
															var pNode = ensure.apply(sectionNode, p);               // top = TARGET project
															if (parts.length == 2) {
																var seq = p.getSequenceByName(parts[1]);
																ensure.apply(pNode, seq).setSource(op);
															} else {
																var conn = p.getConnectorByName(parts[1]);
																try {
																	var txn = conn.getTransactionByName(parts[2]);
																	ensure.apply(pNode, txn).setSource(op);          // prefer txn
																} catch (Exception noTxn) {
																	ensure.apply(pNode, conn).setSource(op);         // else connector
																}
															}
														} catch (Exception ignoreBroken) {}
													} else { // USED_BY
														if (selIsProject && selProjName.equals(pName)) {
															var srcNode = ensure.apply(sectionNode, srcProject);    // top = SOURCE project
															try {
																if (parts.length == 2) {
																	var seq = selProject.getSequenceByName(parts[1]);
																	ensure.apply(srcNode, seq).setSource(op);
																} else {
																	var conn = selProject.getConnectorByName(parts[1]);
																	try {
																		var txn = conn.getTransactionByName(parts[2]);
																		ensure.apply(srcNode, txn).setSource(op);
																	} catch (Exception noTxn) {
																		ensure.apply(srcNode, conn).setSource(op);
																	}
																}
															} catch (Exception ignoreBroken) {}
														} else {
															var match = (selIsSequence    && target.equals(selFqnSeq))
																	|| (selIsTransaction && target.equals(selFqnTxn))
																	|| (selIsConnector   && target.startsWith(selConnPref));
															if (match) {
																var srcNode = ensure.apply(sectionNode, srcProject);
																if (selIsConnector && parts.length >= 3
																		&& selProjName.equals(pName) && selConnectorName.equals(parts[1])) {
																	try {
																		var txn = ((Connector) selection).getTransactionByName(parts[2]);
																		ensure.apply(srcNode, txn).setSource(op);
																	} catch (Exception ignoreBroken) {}
																} else if (selIsTransaction) {
																	ensure.apply(srcNode, selection).setSource(op);
																} else if (selIsSequence) {
																	ensure.apply(srcNode, selection).setSource(op);
																}
															}
														}
													}
												}
											}
										}

										/* ======================== NGX: UIUseShared / UIDynamicInvoke / UIDynamicAction ======================== */
										else if (o instanceof UIUseShared u) {
											var ref = normalizeSmartRef(u.getSharedComponentQName());
											if (ref != null) {
												var parts = ref.split("\\.");
												if (parts.length >= 2) {
													var pName = parts[0];
													var compName = parts[parts.length - 1];

													if (mode == RefMode.REQUIRES) {
														try {
															var p = getProjectByName(pName);
															if (p == null) { super.walk(o); return; }
															var mobApp = p.getMobileApplication();
															if (mobApp != null && mobApp.getApplicationComponent() instanceof ApplicationComponent ngxApp) {
																UISharedComponent sc = null;
																for (var s : ngxApp.getSharedComponentList()) {
																	if (compName.equals(s.getName())) { sc = s; break; }
																}
																if (sc != null) {
																	var pNode = ensure.apply(sectionNode, p);
																	ensure.apply(pNode, sc).setSource(u);
																}
															}
														} catch (Exception ignoreBroken) {}
													} else { // USED_BY
														if (selIsSharedComponent && selProjName.equals(pName)
																&& ((UISharedComponent) selection).getName().equals(compName)) {
															var srcNode = ensure.apply(sectionNode, srcProject);
															ensure.apply(srcNode, selection).setSource(u);
														} else if (selIsProject && selProjName.equals(pName)) {
															try {
																var mobApp = selProject.getMobileApplication();
																if (mobApp != null && mobApp.getApplicationComponent() instanceof ApplicationComponent ngxApp) {
																	UISharedComponent sc = null;
																	for (var s : ngxApp.getSharedComponentList()) {
																		if (compName.equals(s.getName())) { sc = s; break; }
																	}
																	if (sc != null) {
																		var srcNode = ensure.apply(sectionNode, srcProject);
																		ensure.apply(srcNode, sc).setSource(u);
																	}
																}
															} catch (Exception ignoreBroken) {}
														}
													}
												}
											}
										}
										else if (o instanceof UIDynamicInvoke inv) {
											var ref = normalizeSmartRef(inv.getSharedActionQName());
											if (ref != null) {
												var parts = ref.split("\\.");
												if (parts.length >= 2) {
													var pName = parts[0];
													var stackName = parts[parts.length - 1];

													if (mode == RefMode.REQUIRES) {
														try {
															var p = getProjectByName(pName);
															if (p == null) { super.walk(o); return; }
															var mobApp = p.getMobileApplication();
															UIActionStack as = null;
															if (mobApp != null && mobApp.getApplicationComponent() instanceof ApplicationComponent ngxApp) {
																for (var s : ngxApp.getSharedActionList()) {
																	if (stackName.equals(s.getName())) { as = s; break; }
																}
															}
															if (as != null) {
																var pNode = ensure.apply(sectionNode, p);
																ensure.apply(pNode, as).setSource(inv);
															}
														} catch (Exception ignoreBroken) {}
													} else { // USED_BY
														if (selIsActionStack && selProjName.equals(pName)
																&& ((UIActionStack) selection).getName().equals(stackName)) {
															var srcNode = ensure.apply(sectionNode, srcProject);
															ensure.apply(srcNode, selection).setSource(inv);
														} else if (selIsProject && selProjName.equals(pName)) {
															try {
																var mobApp = selProject.getMobileApplication();
																UIActionStack as = null;
																if (mobApp != null && mobApp.getApplicationComponent() instanceof ApplicationComponent ngxApp) {
																	for (var s : ngxApp.getSharedActionList()) {
																		if (stackName.equals(s.getName())) { as = s; break; }
																	}
																}
																if (as != null) {
																	var srcNode = ensure.apply(sectionNode, srcProject);
																	ensure.apply(srcNode, as).setSource(inv);
																}
															} catch (Exception ignoreBroken) {}
														}
													}
												}
											}
										}
										else if (o instanceof UIDynamicAction uda) {
											var ionBean = uda.getIonBean();
											if (ionBean != null) {
												var property = ionBean.getProperty("requestable");
												if (property != null) {
													var ref = normalizeSmartRef(property.getSmartValue());
													if (ref != null) {
														var parts = ref.split("\\.");
														if (parts.length >= 2) {
															var pName = parts[0];

															if (mode == RefMode.REQUIRES) {
																try {
																	var p = getProjectByName(pName);
																	if (p == null) { super.walk(o); return; }
																	var pNode = ensure.apply(sectionNode, p);
																	if (parts.length == 2) {
																		// sequence OR connector
																		try {
																			var seq = p.getSequenceByName(parts[1]);
																			ensure.apply(pNode, seq).setSource(uda);
																		} catch (Exception notSeq) {
																			try {
																				var conn = p.getConnectorByName(parts[1]);
																				ensure.apply(pNode, conn).setSource(uda);
																			} catch (Exception ignoreBroken) {}
																		}
																	} else {
																		try {
																			var conn = p.getConnectorByName(parts[1]);
																			try {
																				var txn = conn.getTransactionByName(parts[2]);
																				ensure.apply(pNode, txn).setSource(uda); // prefer txn
																			} catch (Exception noTxn) {
																				ensure.apply(pNode, conn).setSource(uda);
																			}
																		} catch (Exception ignoreBroken) {}
																	}
																} catch (Exception ignoreBroken) {}
															} else { // USED_BY
																if (selIsSequence && parts.length == 2
																		&& selProjName.equals(pName) && selSequenceName.equals(parts[1])) {
																	var srcNode = ensure.apply(sectionNode, srcProject);
																	ensure.apply(srcNode, selection).setSource(uda);
																}
																else if (selIsConnector && selProjName.equals(pName) && parts.length >= 2 && selConnectorName.equals(parts[1])) {
																	var srcNode = ensure.apply(sectionNode, srcProject);
																	if (parts.length >= 3) {
																		try {
																			var txn = ((Connector) selection).getTransactionByName(parts[2]);
																			ensure.apply(srcNode, txn).setSource(uda);
																		} catch (Exception ignoreBroken) {}
																	} else {
																		ensure.apply(srcNode, selection).setSource(uda); // target = connector
																	}
																}
																else if (selIsTransaction && parts.length >= 3
																		&& selProjName.equals(pName)
																		&& selConnectorName.equals(parts[1])
																		&& selTxnName.equals(parts[2])) {
																	var srcNode = ensure.apply(sectionNode, srcProject);
																	ensure.apply(srcNode, selection).setSource(uda);
																}
																else if (selIsProject && selProjName.equals(pName)) {
																	var srcNode = ensure.apply(sectionNode, srcProject);
																	if (parts.length == 2) {
																		try {
																			var seq = selProject.getSequenceByName(parts[1]);
																			ensure.apply(srcNode, seq).setSource(uda);
																		} catch (Exception notSeq) {
																			try {
																				var conn = selProject.getConnectorByName(parts[1]);
																				ensure.apply(srcNode, conn).setSource(uda);
																			} catch (Exception ignoreBroken) {}
																		}
																	} else {
																		try {
																			var conn = selProject.getConnectorByName(parts[1]);
																			try {
																				var txn = conn.getTransactionByName(parts[2]);
																				ensure.apply(srcNode, txn).setSource(uda);
																			} catch (Exception ignoreBroken) {
																				ensure.apply(srcNode, conn).setSource(uda);
																			}
																		} catch (Exception ignoreBroken) {}
																	}
																}
															}
														}
													}
												}
											}
										}

										/* ======================== ScreenClass in JavelinTransaction ======================== */
										else if (o instanceof Transaction t) {
											if (selIsScreenClass && t instanceof JavelinTransaction jt) {
												var scName = ((ScreenClass) selection).getName();
												var handlers = jt.handlers != null ? jt.handlers : "";
												if (handlers.contains("function on" + scName + "Entry()") || handlers.contains("function on" + scName + "Exit()")) {
													if (mode == RefMode.USED_BY) {
														var srcNode = ensure.apply(sectionNode, srcProject);
														ensure.apply(srcNode, selection).setSource(t);
													}
												}
											}
										}

										super.walk(o);
									}
								}.init(mode == RefMode.REQUIRES ? selection : srcProject);
							} catch (Exception e) {
								ConvertigoPlugin.logException(e, "Error while walking project " + srcProject.getName(), true);
							}
						};

						if (mode == RefMode.REQUIRES) {
							if (selProject != null) scan.accept(selProject);
						} else {
							for (var name : Engine.theApp.databaseObjectsManager.getAllProjectNamesList()) {
								try {
									var p = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(name);
									if (p != null) scan.accept(p);
								} catch (Exception e) {
									ConvertigoPlugin.logException(e, "Error while opening project " + name, true);
								}
							}
						}
	}

	/* -------------------- helpers -------------------- */

	private Project getProjectByName(String name) {
		try {
			return Engine.theApp.databaseObjectsManager.getOriginalProjectByName(name);
		} catch (Exception e) {
			return null;
		}
	}

	private DboNode ensureNode(Map<String, DboNode> index, AbstractParentNode parent, DatabaseObject target) {
		if (target == null) return null;
		var key = target.getFullQName();
		var n = index.get(key);
		if (n == null) {
			n = new DboNode(parent, target.getName(), target);
			index.put(key, n);
			if (parent != null) parent.addChild(n);
		}
		return n;
	}

	private String normalizeSmartRef(String s) {
		if (s == null) return null;
		var ref = s.trim();
		var i = ref.indexOf(':');
		if (i >= 0) ref = ref.substring(i + 1).trim(); // strip "plain:", "script:", etc.
		ref = ref.replaceAll("^['\"]|['\"]$", "");
		return ref.isEmpty() ? null : ref;
	}

	private void handleSelectedObjectInRefView(Object firstElement) {		
		if (firstElement != null) {
			DatabaseObject selectedDatabaseObject = null;
			if (firstElement instanceof DatabaseObject dbo) {
				selectedDatabaseObject = dbo;
			} else if (firstElement instanceof DboNode node && node.getSource() == null) {
				selectedDatabaseObject =  node.getTarget();
			}
			if (selectedDatabaseObject != null) {
				var projectExplorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();
				var selectedTreeObject = projectExplorerView.findTreeObjectByUserObject(selectedDatabaseObject);

				if (selectedTreeObject != null) {
					projectExplorerView.setSelectedTreeObject(selectedTreeObject);

					if (selectedTreeObject instanceof UnloadedProjectTreeObject) {
						ConvertigoPlugin.infoMessageBox("This project is closed. Please open the project first.");
					}
				}
			}
		}
	}

	@Override
	public void partHidden(IWorkbenchPartReference partRef) {
		if (partRef.getId().equals(getViewSite().getId())) {
			isVisible = false;
		}
	}

	@Override
	public void partVisible(IWorkbenchPartReference partRef) {
		if (partRef.getId().equals(getViewSite().getId()) && !isVisible) {
			isVisible = true;
			var pev = ConvertigoPlugin.getDefault().getProjectExplorerView();
			if (pev != null) {
				selectionChanged(pev, pev.viewer.getSelection());
			}
		}
	}
}
