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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.eclipse.editors.flow;

import java.util.Objects;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

public class FlowEngineEditorInput implements IEditorInput {

	private final String id;
	private final String title;
	private final String url;
	private final String projectName;
	private final String tooltip;
	private final String authoringProtocol;

	public FlowEngineEditorInput(String id, String title, String url, String projectName, String tooltip) {
		this(id, title, url, projectName, tooltip, "");
	}

	public FlowEngineEditorInput(String id, String title, String url, String projectName, String tooltip, String authoringProtocol) {
		this.url = Objects.toString(url, "");
		this.projectName = Objects.toString(projectName, "");
		this.title = Objects.toString(title, "Flow");
		this.id = Objects.toString(id, "").isBlank()
				? "flow.browser:" + this.projectName + ":" + this.url
				: id;
		this.tooltip = Objects.toString(tooltip, "").isBlank()
				? this.url
				: tooltip;
		this.authoringProtocol = Objects.toString(authoringProtocol, "");
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}

	@Override
	public boolean exists() {
		return false;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	@Override
	public String getName() {
		return title;
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return tooltip;
	}

	public String getId() {
		return id;
	}

	public String getUrl() {
		return url;
	}

	public String getProjectName() {
		return projectName;
	}

	public String getAuthoringProtocol() {
		return authoringProtocol;
	}

	public boolean supportsAuthoring() {
		return !authoringProtocol.isBlank();
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof FlowEngineEditorInput other && id.equals(other.id);
	}
}
