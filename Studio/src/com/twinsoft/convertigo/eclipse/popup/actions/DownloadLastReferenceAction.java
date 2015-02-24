/**
 * @author julienda
 * @date 23/02/2015
 */
package com.twinsoft.convertigo.eclipse.popup.actions;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.references.WebServiceReference;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;

public class DownloadLastReferenceAction extends MyAbstractAction {

	public DownloadLastReferenceAction() { }

	/*
	 * (non-Javadoc)
	 * @see com.twinsoft.convertigo.eclipse.popup.actions.MyAbstractAction#run()
	 */
	@Override
	public void run() {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);		
		
		Shell shell = getParentShell();
		shell.setCursor(waitCursor);
		MessageBox dialog;
		
        try {
    		ProjectExplorerView explorerView = getProjectExplorerView();
    		if (explorerView != null) {
    			TreeObject treeObject = explorerView.getFirstSelectedTreeObject();
    			Object databaseObject = treeObject.getObject();
    			
    			if ((databaseObject != null) && (databaseObject instanceof WebServiceReference)) {
    				WebServiceReference webServiceReference = (WebServiceReference) databaseObject;
					File webServiceFile = webServiceReference.getFile();
					
					FileUtils.copyURLToFile(webServiceReference.getUrl(), webServiceFile);
					
					dialog = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
					dialog.setText("SUCCESS");
					dialog.setMessage("The reference file has been downloaded with success!");
					dialog.open();
				}
			}
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to download the selected reference!");
        	
        	dialog = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
			dialog.setText("ERROR");
			dialog.setMessage("Unable to download the selected reference!");
			dialog.open();
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
}
