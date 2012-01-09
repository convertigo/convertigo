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

package com.twinsoft.convertigo.eclipse.property_editors;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeItem;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.twinsoft.convertigo.eclipse.editors.connector.htmlconnector.TwsDomTree;
import com.twinsoft.convertigo.eclipse.editors.connector.htmlconnector.XpathEvaluatorComposite;
import com.twinsoft.convertigo.engine.util.TwsCachedXPathAPI;

public class StepXpathEvaluatorComposite extends XpathEvaluatorComposite {

	private IStepSourceEditor stepSourceEditorComposite = null;
	private TwsCachedXPathAPI twsCachedXPathAPI = null;
	
	public StepXpathEvaluatorComposite(Composite parent, int style, IStepSourceEditor stepSourceEditorComposite) {
		super(parent, style);
		getLabel().setToolTipText("ctrl+up : backward history\nctrl+down : forward history");
		this.stepSourceEditorComposite = stepSourceEditorComposite;
		this.noPredicate = true;
	}

	public Document getDom() {
		return stepSourceEditorComposite.getDom();
	}

	public TwsCachedXPathAPI getXpathApi() {
		if (twsCachedXPathAPI == null)
			return new TwsCachedXPathAPI();
		return twsCachedXPathAPI;
	}

	public TreeItem[] findTreeItems(String newXpath) {
		return stepSourceEditorComposite.findTreeItems(newXpath);
	}

//	public void selectElementsInTree(String newXpath) {
//		stepSourceEditorComposite.selectElementsInTree(newXpath);
//	}
	
	public void generateAbsoluteXpath(boolean overwrite, Node node){
		super.generateAbsoluteXpath(overwrite, node);
	}
	
	public void setSelectedXpath(String newXpath) {

	}

	protected void buttonSelected(String name) {
		
	}

	protected boolean isButtonEnabled(String name) {
		return true;
	}

	public String getAnchor() {
		return currentAnchor;
	}
		
	public void removeAnchor() {
		super.removeAnchor();
	}
	
	public void displaySelectionXpathWithAnchor(TwsDomTree tree, String anchor, String xpath) {
		TreeItem[] items;
		
		items = findTreeItems(anchor);
		if (items.length > 0) stepSourceEditorComposite.selectItemsInTree(items);
		setXpathText(anchor);
		setAnchor(true);
		
		if (!xpath.equals("")) {
			items = findTreeItems(currentAnchor + xpath.substring(1));
			if (items.length > 0) stepSourceEditorComposite.selectItemsInTree(items);
			setXpathText(currentAnchor + xpath.substring(1));
		}
	}
	
	protected Object getDragData() {
		return stepSourceEditorComposite.getDragData();
	}
	
}
