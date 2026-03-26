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

package com.twinsoft.convertigo.eclipse.views.picker;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DatabaseObjectPickerNode {

	public static final String FOLDER_ICON = "icons/studio/folder.png";

	private final String name;
	private final String label;
	private final Object object;
	private final boolean selectable;
	private final String technicalText;
	private final String iconPath;
	protected DatabaseObjectPickerNode parent;
	protected final List<DatabaseObjectPickerNode> children = new ArrayList<>();

	public DatabaseObjectPickerNode(String name) {
		this(name, name, null, false, null, null);
	}

	public DatabaseObjectPickerNode(String name, Object object) {
		this(name, name, object, false, null, null);
	}

	public DatabaseObjectPickerNode(String name, String label, Object object, boolean selectable, String technicalText, String iconPath) {
		this.name = name;
		this.label = label == null ? name : label;
		this.object = object;
		this.selectable = selectable;
		this.technicalText = technicalText;
		this.iconPath = iconPath;
	}

	public String getName() {
		return name;
	}

	public String getLabel() {
		return label;
	}

	public Object getObject() {
		return object;
	}

	public boolean isSelectable() {
		return selectable;
	}

	public String getIconPath() {
		return iconPath;
	}

	public DatabaseObjectPickerNode getParent() {
		return parent;
	}

	public List<DatabaseObjectPickerNode> getChildren() {
		return children;
	}

	public boolean isEmpty() {
		return children.isEmpty();
	}

	public String getTechnicalText() {
		return technicalText == null ? "" : technicalText;
	}

	public String getSearchText() {
		StringBuilder sb = new StringBuilder(getLabel());
		String technical = getTechnicalText();
		if (!technical.isEmpty() && !technical.equals(getLabel())) {
			sb.append(' ').append(technical);
		}
		return sb.toString();
	}

	public boolean matches(String filterText) {
		if (filterText == null || filterText.isBlank()) {
			return true;
		}
		return getSearchText().toLowerCase(Locale.ROOT).contains(filterText.toLowerCase(Locale.ROOT));
	}

	public <T extends DatabaseObjectPickerNode> T add(T child) {
		if (child != null) {
			child.parent = this;
			if (!children.contains(child)) {
				children.add(child);
			}
		}
		return child;
	}

	public boolean remove(DatabaseObjectPickerNode child) {
		if (child != null && children.remove(child)) {
			child.parent = null;
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return getLabel();
	}
}
