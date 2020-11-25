package com.twinsoft.convertigo.eclipse.editors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowser;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.ProductVersion;
import com.twinsoft.convertigo.engine.util.URLUtils;

public class StartupEditor extends EditorPart {

	public static final String ID = "com.twinsoft.convertigo.eclipse.editors.StartupEditor";

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new GridLayout(1, true));
		ToolBar tb = new ToolBar(parent, SWT.FLAT | SWT.WRAP | SWT.RIGHT);
		tb.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		tb.setVisible(false);
		
		C8oBrowser browser = new C8oBrowser(parent, SWT.NONE);
		browser.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		ToolItem ti = new ToolItem(tb, SWT.NONE);
		ti.setImage(new Image(Display.getCurrent(), getClass().getResourceAsStream("/com/twinsoft/convertigo/eclipse/editors/images/statement.png")));
		ti.setText("View with your external browser");
		ti.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				Program.launch(browser.getURL().replaceFirst("\\?user=.*", ""));
			}
			
		});
		ti = new ToolItem(tb, SWT.SEPARATOR);
		ToolItem tic = new ToolItem(tb, SWT.CHECK);
		tic.setImage(new Image(Display.getCurrent(), getClass().getResourceAsStream("/com/twinsoft/convertigo/eclipse/editors/images/stop.png")));
		tic.setText("Auto close");
		tic.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				if (tic.getSelection()) {
					ConvertigoPlugin.setProperty(ConvertigoPlugin.PREFERENCE_IGNORE_NEWS, "true");
					boolean s[] = {true};
					int remains[] = {10};
					tic.setText("Auto close in " + remains[0] + "s");
					Engine.execute(() -> {
						try {
							while (--remains[0] >= 0 && s[0]) {
								Thread.sleep(1000);
								tic.getDisplay().syncExec(() -> {
									if (!tic.isDisposed()) {
										if (s[0] = tic.getSelection()) {
											tic.setText("Auto close in " + remains[0] + "s");
										}
									}
								});
							}
							if (s[0]) {
								tic.getDisplay().asyncExec(() -> {
									PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().closeEditor(StartupEditor.this, false);
								});
							}
						} catch (Exception e2) {
						}
					});
				} else {
					tic.setText("Auto close");
					ConvertigoPlugin.setProperty(ConvertigoPlugin.PREFERENCE_IGNORE_NEWS, "false");
				}
			}
		});
		
		String url = "https://www.convertigo.com/quick-start-videos/";
		url += "?" + URLUtils.encodePart("user", getEditorInput().getName());
		url += "&" + URLUtils.encodePart("version", ProductVersion.fullProductVersion);

		browser.addProgressListener(new ProgressAdapter() {
			
			@Override
			public void completed(ProgressEvent event) {
				tb.getDisplay().asyncExec(() -> {
					tb.setVisible(true);
					if (ConvertigoPlugin.getProperty(ConvertigoPlugin.PREFERENCE_IGNORE_NEWS).equalsIgnoreCase("true")) {
						tic.setSelection(true);
						tic.notifyListeners(SWT.Selection, new Event());
					}
				});
			}
			
		});
		browser.setUrl(url);
	}

	@Override
	public void setFocus() {
	}

	public static IEditorInput makeInput(String username) {
		return new IEditorInput() {
			
			@Override
			public <T> T getAdapter(Class<T> adapter) {
				return null;
			}
			
			@Override
			public String getToolTipText() {
				return null;
			}
			
			@Override
			public IPersistableElement getPersistable() {
				return null;
			}
			
			@Override
			public String getName() {
				return username;
			}
			
			@Override
			public ImageDescriptor getImageDescriptor() {
				return null;
			}
			
			@Override
			public boolean exists() {
				return false;
			}
		};
	}

}
