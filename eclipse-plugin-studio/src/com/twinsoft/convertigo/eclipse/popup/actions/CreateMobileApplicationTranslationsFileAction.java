/*
 * Copyright (c) 2001-2019 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.popup.actions;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.mobile.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.mobile.components.MobileSmartSourceType;
import com.twinsoft.convertigo.beans.mobile.components.PageComponent;
import com.twinsoft.convertigo.beans.mobile.components.UISharedComponent;
import com.twinsoft.convertigo.beans.mobile.components.UIText;
import com.twinsoft.convertigo.beans.mobile.components.UIUseShared;
import com.twinsoft.convertigo.beans.mobile.components.MobileSmartSourceType.Mode;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.dialogs.MobileApplicationTranslationsDialog;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.helpers.WalkHelper;
import com.twinsoft.convertigo.engine.mobile.TranslateUtils;
import com.twinsoft.convertigo.engine.mobile.TranslateUtils.Translator;

public class CreateMobileApplicationTranslationsFileAction extends MyAbstractAction {

	public CreateMobileApplicationTranslationsFileAction() {
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
    			Object databaseObject = treeObject.getObject();

    			List<String> textList = new ArrayList<String>();
    			if ((databaseObject != null) && (databaseObject instanceof ApplicationComponent)) {
    				ApplicationComponent application = (ApplicationComponent) databaseObject;
    				
    				new WalkHelper() {
    					@Override
    					protected void walk(DatabaseObject databaseObject) throws Exception {
    						String text = null;
    						if (databaseObject instanceof PageComponent) {
								PageComponent page = (PageComponent)databaseObject;
								text = page.getTitle();
    						} else if (databaseObject instanceof UIUseShared) {
    							UIUseShared uius = (UIUseShared)databaseObject;
    							UISharedComponent uisc = uius.getTargetSharedComponent();
    							if (uisc != null && ! uius.isRecursive()) {
    								super.walk(uisc);
    							}
    						} else if (databaseObject instanceof UIText) {
    							UIText uiText = (UIText)databaseObject;
    							MobileSmartSourceType msst = uiText.getTextSmartType();
    							if (Mode.PLAIN.equals(msst.getMode())) {
    								text = msst.getValue();
    							}
    						}
    						
    						if (text != null && !textList.contains(text)) {
    							textList.add(text);
    						}
    						
    						super.walk(databaseObject);
    					}
    				}.init(application);
    				
    				MobileApplicationTranslationsDialog dlg = new MobileApplicationTranslationsDialog(shell);
    				int ret = dlg.open();
                	if (ret != Window.OK) {
                		return;
                	}
    				
                	Locale from = dlg.getLocaleFrom();
                	Locale to = dlg.getLocaleTo();
                	boolean auto = dlg.isAuto();
                	
    				File i18nDir = new File(application.getProject().getDirPath(), "DisplayObjects/mobile/assets/i18n");
    				
                	// store source file
    				File source = new File(i18nDir, from.getLanguage() + ".json");
    				TranslateUtils.storeTranslations(textList, source);
    				ConvertigoPlugin.logDebug(source.getName() + " file successfully created or updated.");
    				
                	// store target file
    				if (!to.equals(from)) {
	    				File target = new File(i18nDir, to.getLanguage() + ".json");
	    				// translate with google api
	    				if (auto) {
	    					ProgressMonitorDialog dialog = new ProgressMonitorDialog(shell);
	    					dialog.run(true, false, new IRunnableWithProgress() {
	    						@Override
	    						public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
	    							monitor.beginTask("translating", IProgressMonitor.UNKNOWN);
	    	    					Translator translator = TranslateUtils.newTranslator();
	    	    					try {
	    	    						translator.translate(from, source, to, target);
	    	    						ConvertigoPlugin.logDebug(target.getName() + " file successfully translated.");
	    	    					} catch (Exception e) {
	    	    						ConvertigoPlugin.logError(e.getMessage(), false);
	    	    						try {
											TranslateUtils.storeTranslations(textList, target);
										} catch (Exception ex) {}
	    	    					}
	    							monitor.done();
	    						}
	    					});
	    				}
	    				// do not translate
	    				else {
	        				TranslateUtils.storeTranslations(textList, target);
	    				}
	    				ConvertigoPlugin.logDebug(target.getName() + " file successfully created or updated.");
    				}
    				
    				// regenerate app templates
    				try {
    					application.markApplicationAsDirty();
    					for (PageComponent page : application.getPageComponentList()) {
    						if (page.isEnabled()) {
    							page.markPageAsDirty();
    						}
    					}
    				} catch (Throwable t) {}

    				ConvertigoPlugin.logInfo("Translations file(s) successfully created or updated.", true);
    			}
    		}
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to create the Mobile application translations file(s)!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
}
