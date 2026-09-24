/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.views.projectexplorer;

import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerLabel;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;

class DecoratingColumnLabelProvider extends ColumnLabelProvider {
	
	private DecoratingLabelProvider provider;
	
	/** for each element, its undecorated text, and the text and image it was last shown with */
	private Map<Object, Object[]> shown = new WeakHashMap<Object, Object[]>();
	
	DecoratingColumnLabelProvider(ILabelProvider provider, ILabelDecorator decorator) {
		this.provider = new DecoratingLabelProvider(provider, decorator);		
	}

	@Override
	public void update(ViewerCell cell) {
		Object element = cell.getElement();
		String text = provider.getLabelProvider().getText(element);
		// while the decoration of a label is pending, as when the Git decorations are computed again after a project
		// is loaded, an element whose text has not changed keeps the decorated text and image it was shown with
		// instead of losing its decoration until it is computed again
		Object[] last = shown.get(element);
		boolean unchanged = last != null && text != null && text.equals(last[0]) && !(last[2] instanceof Image image && image.isDisposed());
		ViewerLabel label = unchanged ? new ViewerLabel((String) last[1], (Image) last[2]) : new ViewerLabel("", null);
		provider.updateLabel(label, element);
		shown.put(element, new Object[] { text, label.getText(), label.getImage() });
		cell.setText(label.getText());
		cell.setImage(label.getImage());
		cell.setBackground(getBackground(element));
		cell.setForeground(getForeground(element));
		cell.setFont(getFont(element));
	}

	@Override
	public Font getFont(Object element) {
		return provider.getFont(element);
	}

	@Override
	public Color getBackground(Object element) {
		return provider.getBackground(element);
	}

	@Override
	public Color getForeground(Object element) {
		return provider.getForeground(element);
	}

	@Override
	public Image getImage(Object element) {
		return provider.getImage(element);
	}

	@Override
	public String getText(Object element) {
		return provider.getText(element);
	}

	@Override
	public void addListener(ILabelProviderListener listener) {
		super.addListener(listener);
		provider.addListener(listener);
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return provider.isLabelProperty(element, property);
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
		super.removeListener(listener);
		provider.removeListener(listener);
	}

}
