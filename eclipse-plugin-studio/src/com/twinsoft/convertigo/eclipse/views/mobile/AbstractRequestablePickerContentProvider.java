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

package com.twinsoft.convertigo.eclipse.views.mobile;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.twinsoft.convertigo.beans.connectors.FullSyncConnector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Document;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.couchdb.DesignDocument;
import com.twinsoft.convertigo.eclipse.views.picker.DatabaseObjectPickerNode;
import com.twinsoft.convertigo.engine.enums.CouchKey;
import com.twinsoft.convertigo.engine.util.GenericUtils;

abstract class AbstractRequestablePickerContentProvider implements ITreeContentProvider {

	protected static final Pattern INVALID_CHARACTERS = Pattern.compile("[~:\\-\\s]+");

	public class TVObject extends DatabaseObjectPickerNode {
		private final Object sourceData;
		private final JSONObject infos;

		protected TVObject(String name) {
			this(name, null, null);
		}

		protected TVObject(String name, Object object, Object sourceData) {
			this(name, object, sourceData, null);
		}

		protected TVObject(String name, Object object, Object sourceData, JSONObject infos) {
			super(name, name, object, false, null, null);
			this.sourceData = sourceData;
			this.infos = infos == null ? new JSONObject() : infos;
		}

		public String getPath() {
			String name = getName();
			String path = INVALID_CHARACTERS.matcher(name).find() ? "['" + name + "']" : name;
			TVObject parent = getParent();
			if (parent != null) {
				path = parent.getPath() + (omitOptionalAccessorBeforeBracket() && path.startsWith("[") ? "" : "?.") + path;
			}
			return path;
		}

		@Override
		public String getTechnicalText() {
			String source = getSource();
			return source.isEmpty() ? getPath() : source + " " + getPath();
		}

		@SuppressWarnings("unchecked")
		public <T> T getSourceData() {
			return (T) sourceData;
		}

		public String getSource() {
			String source = getExplicitSource(sourceData);
			if (source != null && !source.isEmpty()) {
				return source;
			}
			return computeLegacySource(this);
		}

		public TVObject getParent() {
			return (TVObject) super.getParent();
		}

		public JSONObject getInfos() {
			return infos;
		}

		public boolean isEmpty() {
			return getChildren().isEmpty();
		}

		protected TVObject add(TVObject child) {
			return super.add(child);
		}

		protected boolean remove(TVObject child) {
			return super.remove(child);
		}
	}

	protected Object selected = null;

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		ITreeContentProvider.super.inputChanged(viewer, oldInput, newInput);
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof TVObject tvObject) {
			return tvObject.getParent();
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return getChildren(element).length > 0;
	}

	public void setSelectedDbo(Object object) {
		this.selected = object;
	}

	protected void addSequences(Map<String, Set<String>> map, TVObject parent, Object object, boolean isReferenced) {
		if (!(object instanceof Project project)) {
			return;
		}
		for (Sequence sequence : project.getSequencesList()) {
			String label = isReferenced ? sequence.getQName() : sequence.getName();
			parent.add(new TVObject(sequence.getName(), sequence, createSequenceSourceData(sequence.getQName(), null)));

			Set<String> infos = map.get(sequence.getQName());
			if (infos == null) {
				continue;
			}
			for (String info : infos) {
				try {
					JSONObject jsonInfo = new JSONObject(info);
					if (jsonInfo.has("marker")) {
						String marker = jsonInfo.getString("marker");
						if (!marker.isEmpty()) {
							parent.add(new TVObject(label + "#" + marker, sequence, createSequenceSourceData(sequence.getQName(), marker), jsonInfo));
						}
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
	}

	protected void addFsObjects(Map<String, Set<String>> map, TVObject parent, Object object, boolean isReferenced) {
		if (!(object instanceof Project project)) {
			return;
		}
		for (Connector connector : project.getConnectorsList()) {
			if (!(connector instanceof FullSyncConnector)) {
				continue;
			}

			String label = isReferenced ? connector.getQName() : connector.getName();
			TVObject connectorNode = parent.add(new TVObject(label, connector, null));

			for (Document document : connector.getDocumentsList()) {
				if (!(document instanceof DesignDocument designDocument)) {
					continue;
				}

				TVObject documentNode = connectorNode.add(new TVObject(document.getName(), document, null));
				JSONObject views = CouchKey.views.JSONObject(designDocument.getJSONObject());
				if (views == null) {
					continue;
				}

				for (Iterator<String> it = GenericUtils.cast(views.keys()); it.hasNext();) {
					try {
						Set<String> infos = null;
						String view = it.next();
						String key = connector.getQName() + "." + document.getName() + "." + view;
						TVObject viewNode = documentNode.add(new TVObject(view));

						viewNode.add(new TVObject("get", document, createDatabaseSourceData(connector.getQName(), document.getQName(), view, "get", null, false)));
						infos = map.get(key + ".get");
						if (infos == null) {
							infos = map.get(connector.getQName() + ".get");
						}
						if (infos != null) {
							for (String info : infos) {
								try {
									JSONObject jsonInfo = new JSONObject(info);
									boolean includeDocs = false;
									if (jsonInfo.has("include_docs")) {
										includeDocs = Boolean.parseBoolean(jsonInfo.getString("include_docs"));
									}
									if (jsonInfo.has("marker")) {
										String marker = jsonInfo.getString("marker");
										if (!marker.isEmpty()) {
											viewNode.add(new TVObject("get#" + marker, document,
												createDatabaseSourceData(connector.getQName(), document.getQName(), view, "get", marker, includeDocs),
												jsonInfo));
										}
									}
								} catch (JSONException e) {
									e.printStackTrace();
								}
							}
						}

						viewNode.add(new TVObject("view", document, createDatabaseSourceData(connector.getQName(), document.getQName(), view, "view", null, false)));
						infos = map.get(key + ".view");
						if (infos != null) {
							for (String info : infos) {
								try {
									JSONObject jsonInfo = new JSONObject(info);
									boolean includeDocs = false;
									if (jsonInfo.has("include_docs")) {
										includeDocs = Boolean.parseBoolean(jsonInfo.getString("include_docs"));
									}
									if (jsonInfo.has("marker")) {
										String marker = jsonInfo.getString("marker");
										if (!marker.isEmpty()) {
											viewNode.add(new TVObject("view#" + marker, document,
												createDatabaseSourceData(connector.getQName(), document.getQName(), view, "view", marker, includeDocs),
												jsonInfo));
										}
									}
								} catch (JSONException e) {
									e.printStackTrace();
								}
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	protected void addJsonObjects(TVObject parent) {
		try {
			if (parent == null) {
				return;
			}
			Object object = parent.getObject();
			if (object instanceof JSONObject jsonObject) {
				for (Iterator<String> it = GenericUtils.cast(jsonObject.keys()); it.hasNext();) {
					String key = it.next();
					TVObject child = new TVObject(key, jsonObject.get(key), null);
					addJsonObjects(child);
					parent.add(child);
				}
			} else if (object instanceof JSONArray jsonArray) {
				for (int i = 0; i < jsonArray.length(); i++) {
					TVObject child = new TVObject("[" + i + "]", jsonArray.get(i), null);
					addJsonObjects(child);
					parent.add(child);
				}
			} else if (object != null) {
				String key = object.toString();
				if (!key.isEmpty()) {
					parent.add(new TVObject(key, object, null));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected boolean omitOptionalAccessorBeforeBracket() {
		return false;
	}

	protected abstract String getExplicitSource(Object sourceData);

	protected abstract String computeLegacySource(TVObject node);

	protected abstract Object createSequenceSourceData(String sequenceQName, String marker);

	protected abstract Object createDatabaseSourceData(String connectorQName, String documentQName, String queryView, String verb, String marker, boolean includeDocs);
}
