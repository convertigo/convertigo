/*
 * Copyright (c) 2001-2021 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.editors.sequence;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.part.FileInPlaceEditorInput;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.swt.SwtUtils;

public class SequenceEditorInput extends FileInPlaceEditorInput {

	Sequence sequence;
	private String qname;
	
	static IFile getTmpFile(Sequence sequence) {
		try {
			return ConvertigoPlugin.getDefault().getProjectPluginResource(sequence.getProject().getName()).getFile("_private/studio/wait.txt");
		} catch (Exception e) {
		}
		return null;
	}
	
	static IFile getTmpFile(Sequence sequence, String extension) {
		try {
			return ConvertigoPlugin.getDefault().getProjectPluginResource(sequence.getProject().getName()).getFile("_private/studio/" + sequence.getQName() + extension);
		} catch (Exception e) {
		}
		return null;
	}
	
	public SequenceEditorInput(Sequence sequence) {
		super(getTmpFile(sequence));
		this.sequence = sequence;
		qname = sequence.getQName();
	}
	
	public SequenceEditorInput(Sequence sequence, String extension) {
		super(getTmpFile(sequence, extension));
		this.sequence = sequence;
		qname = sequence.getQName();
	}
	
	public boolean exists() {
		return false;
	}

	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	public String getName() {
		return sequence.getName() + ".json";
	}

	public IPersistableElement getPersistable() {
		return null;
	}

	public String getToolTipText() {
		return sequence.getParent().getName() + "/" + sequence.getName();
	}

	@SuppressWarnings("unchecked")
	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
		return null;
	}
	
	public boolean is(Sequence sequence) {
		return sequence.equals(this.sequence) && sequence.getQName().equals(qname);
	}
	
	public boolean is(Project project) {
		return qname.startsWith(project.getQName());
	}
	
	public Sequence getSequence() {
		return sequence;
	}
	
	public void fileDelete() {
		try {
			getFile().delete(true, null);
		} catch (CoreException e) {
		}
	}
	
	public boolean fileExists() {
		return getFile().exists();
	}
	
	public void fileWrite(String str) {
		SwtUtils.fillFile(getFile(), str);
	}
}
