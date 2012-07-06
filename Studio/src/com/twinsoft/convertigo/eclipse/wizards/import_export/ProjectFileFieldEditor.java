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

package com.twinsoft.convertigo.eclipse.wizards.import_export;

import java.io.File;

import org.eclipse.jface.preference.StringButtonFieldEditor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;

import com.twinsoft.convertigo.engine.Engine;

public class ProjectFileFieldEditor extends StringButtonFieldEditor {

	private String[] filterExtensions = new String[]{"*.car;*.xml"};
	private String[] filterNames = new String[]{"Convertigo projects"};
	
	/**
     * Indicates whether the path must be absolute;
     * <code>false</code> by default.
     */
    private boolean enforceAbsolute = false;

    /**
     * Creates a new file field editor 
     */
    protected ProjectFileFieldEditor() {
    }

    /**
     * Creates a file field editor.
     * 
     * @param name the name of the preference this field editor works on
     * @param labelText the label text of the field editor
     * @param parent the parent of the field editor's control
     */
    public ProjectFileFieldEditor(String name, String labelText, Composite parent) {
        this(name, labelText, false, parent);
    }

    /**
     * Creates a file field editor.
     * 
     * @param name the name of the preference this field editor works on
     * @param labelText the label text of the field editor
     * @param enforceAbsolute <code>true</code> if the file path
     *  must be absolute, and <code>false</code> otherwise
     * @param parent the parent of the field editor's control
     */
    public ProjectFileFieldEditor(String name, String labelText,
            boolean enforceAbsolute, Composite parent) {
        init(name, labelText);
        this.enforceAbsolute = enforceAbsolute;
        setErrorMessage(JFaceResources.getString("FileFieldEditor.errorMessage"));//$NON-NLS-1$
        setChangeButtonText(JFaceResources.getString("openBrowse"));//$NON-NLS-1$
        setValidateStrategy(VALIDATE_ON_FOCUS_LOST);
        createControl(parent);
    }

    /* (non-Javadoc)
     * Method declared on StringButtonFieldEditor.
     * Opens the file chooser dialog and returns the selected file.
     */
    protected String changePressed() {
        File f = new File(getTextControl().getText());
        if (!f.exists()) {
			f = null;
		}
        File d = getFile(f);
        if (d == null) {
			return null;
		}

        return d.getAbsolutePath();
    }

    /* (non-Javadoc)
     * Method declared on StringFieldEditor.
     * Checks whether the text input field specifies an existing file.
     */
    protected boolean checkState() {

        String msg = null;

        String path = getTextControl().getText();
        if (path != null) {
			path = path.trim();
		} else {
			path = "";//$NON-NLS-1$
		}
        if (path.length() == 0) {
            if (!isEmptyStringAllowed()) {
				msg = getErrorMessage();
			}
        } else {
            File file = new File(path);
            if (file.isFile()) {
                if (enforceAbsolute && !file.isAbsolute()) {
					msg = JFaceResources
                            .getString("FileFieldEditor.errorMessage2");//$NON-NLS-1$
				}
            } else {
                msg = getErrorMessage();
            }
        }

        if (msg != null) { // error
            showErrorMessage(msg);
            return false;
        }

        // OK!
        clearErrorMessage();
        return true;
    }

    /**
     * Helper to open the file chooser dialog.
     * @param startingDirectory the directory to open the dialog on.
     * @return File The File the user selected or <code>null</code> if they
     * do not.
     */
    private File getFile(File startingDirectory) {

        FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
        dialog.setText("Select a project file");
        dialog.setFilterExtensions(filterExtensions);
        dialog.setFilterNames(filterNames);
        
        if (!getTextControl().getText().equals("")) {
        	dialog.setFilterPath(getTextControl().getText());
        } else {
        	dialog.setFilterPath(Engine.PROJECTS_PATH);
        }

        if (startingDirectory != null) {
			dialog.setFileName(startingDirectory.getPath());
		}

        String file = dialog.open();
        if (file != null) {
            file = file.trim();
            if (file.length() > 0) {
				return new File(file);
			}
        }

        return null;
    }
}
