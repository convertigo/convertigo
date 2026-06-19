/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program  is free software; you  can redistribute it and/or
 * Modify  it under the terms of the GNU Affero General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 */

package com.twinsoft.convertigo.engine.studio;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Document;

import com.twinsoft.convertigo.beans.connectors.FullSyncConnector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.couchdb.DesignDocument;
import com.twinsoft.convertigo.beans.ngx.components.IAction;
import com.twinsoft.convertigo.beans.ngx.components.IEventGenerator;
import com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.ngx.components.MobileSmartSource;
import com.twinsoft.convertigo.beans.ngx.components.MobileSmartSource.Filter;
import com.twinsoft.convertigo.beans.ngx.components.MobileSmartSource.SourceData;
import com.twinsoft.convertigo.beans.ngx.components.MobileSmartSource.SourceModel;
import com.twinsoft.convertigo.beans.ngx.components.MobileSmartSourceType;
import com.twinsoft.convertigo.beans.ngx.components.MobileComponent;
import com.twinsoft.convertigo.beans.ngx.components.PageComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIActionEvent;
import com.twinsoft.convertigo.beans.ngx.components.UIActionStack;
import com.twinsoft.convertigo.beans.ngx.components.UIAppEvent;
import com.twinsoft.convertigo.beans.ngx.components.UIComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIControlDirective;
import com.twinsoft.convertigo.beans.ngx.components.UIControlDirective.AttrDirective;
import com.twinsoft.convertigo.beans.ngx.components.UIControlEvent;
import com.twinsoft.convertigo.beans.ngx.components.UICustomAction;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicAction;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicElement;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicInvoke;
import com.twinsoft.convertigo.beans.ngx.components.UIEventSubscriber;
import com.twinsoft.convertigo.beans.ngx.components.UIForm;
import com.twinsoft.convertigo.beans.ngx.components.UIPageEvent;
import com.twinsoft.convertigo.beans.ngx.components.UISharedComponent;
import com.twinsoft.convertigo.beans.ngx.components.UISharedComponentEvent;
import com.twinsoft.convertigo.beans.ngx.components.UIUseShared;
import com.twinsoft.convertigo.beans.ngx.components.dynamic.IonBean;
import com.twinsoft.convertigo.beans.ngx.components.dynamic.IonProperty;
import com.twinsoft.convertigo.beans.transactions.couchdb.AbstractCouchDbTransaction;
import com.twinsoft.convertigo.beans.transactions.couchdb.AllDocsTransaction;
import com.twinsoft.convertigo.beans.transactions.couchdb.DeleteDatabaseTransaction;
import com.twinsoft.convertigo.beans.transactions.couchdb.DeleteDocumentAttachmentTransaction;
import com.twinsoft.convertigo.beans.transactions.couchdb.DeleteDocumentTransaction;
import com.twinsoft.convertigo.beans.transactions.couchdb.GetDocumentTransaction;
import com.twinsoft.convertigo.beans.transactions.couchdb.GetViewTransaction;
import com.twinsoft.convertigo.beans.transactions.couchdb.PostDocumentTransaction;
import com.twinsoft.convertigo.beans.transactions.couchdb.PostReplicateTransaction;
import com.twinsoft.convertigo.beans.transactions.couchdb.PutDatabaseTransaction;
import com.twinsoft.convertigo.beans.transactions.couchdb.PutDocumentAttachmentTransaction;
import com.twinsoft.convertigo.beans.transactions.couchdb.ResetDatabaseTransaction;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.CouchKey;
import com.twinsoft.convertigo.engine.enums.SchemaMeta;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;

public class NgxSourcePickerModel {
	private static final List<Filter> PICKER_FILTERS = List.of(
			Filter.Sequence,
			Filter.Database,
			Filter.Action,
			Filter.Shared,
			Filter.Iteration,
			Filter.Form,
			Filter.Global,
			Filter.Local,
			Filter.Icon,
			Filter.Asset);
	private static final Set<Filter> SUPPORTED_FILTERS = EnumSet.of(
			Filter.Sequence,
			Filter.Database,
			Filter.Action,
			Filter.Shared,
			Filter.Iteration,
			Filter.Form,
			Filter.Global,
			Filter.Local,
			Filter.Icon,
			Filter.Asset);

	public static class Model {
		public String ownerId = "";
		public String propertyName = "";
		public String projectName = "";
		public String filter = Filter.Sequence.name();
		public String path = "";
		public String prefix = "";
		public String suffix = "";
		public String custom = "";
		public String input = "";
		public String computedValue = "";
		public boolean useCustom = false;
		public boolean available = false;
		public String message = "";
		public List<FilterModel> filters = new ArrayList<>();
		public NodeModel sources = null;
		public NodeModel modelTree = null;
		public JSONObject sourceData = null;
		public JSONObject sourceValue = null;
	}

	public static class FilterModel {
		public String value = "";
		public String label = "";
		public boolean supported = false;

		FilterModel(Filter filter) {
			this.value = filter.name();
			this.label = filterLabel(filter);
			this.supported = isSupported(filter);
		}
	}

	public static class NodeModel {
		public String type = "";
		public String label = "";
		public String name = "";
		public String value = "";
		public String path = "";
		public String qname = "";
		public String source = "";
		public JSONObject sourceData = null;
		public boolean selected = false;
		public List<NodeModel> children = new ArrayList<>();
	}

	public static Model get(String ownerId, String propertyName, String filterName, String sourceData,
			String path, String prefix, String suffix, String custom, Boolean useCustom) throws Exception {
		Model model = new Model();
		model.ownerId = ownerId == null ? "" : ownerId;
		model.propertyName = propertyName == null ? "" : propertyName;
		for (Filter filter : PICKER_FILTERS) {
			model.filters.add(new FilterModel(filter));
		}

		DatabaseObject dbo = Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(model.ownerId);
		if (dbo == null) {
			model.message = "No object selected";
			return model;
		}

		ApplicationComponent application = getApplication(dbo.getProject());
		if (application == null) {
			model.message = "No NGX application available";
			return model;
		}

		model.projectName = dbo.getProject().getName();
		MobileSmartSource smartSource = readSmartSource(dbo, model.propertyName);
		Filter filter = parseFilter(filterName, smartSource);
		model.filter = filter.name();

		SourceModel sourceModel = smartSource == null ? null : smartSource.getModel();
		if (sourceModel != null) {
			model.path = sourceModel.getPath();
			model.prefix = sourceModel.getPrefix();
			model.suffix = sourceModel.getSuffix();
			model.custom = sourceModel.getCustom();
			model.useCustom = sourceModel.getUseCustom();
			model.input = smartSource.getInput();
			List<SourceData> data = sourceModel.getSourceData();
			if (!data.isEmpty()) {
				model.sourceData = data.get(0).toJson();
			}
		} else if (smartSource != null) {
			model.input = smartSource.getInput();
		}

		if (path != null) {
			model.path = path;
		}
		if (prefix != null) {
			model.prefix = prefix;
		}
		if (suffix != null) {
			model.suffix = suffix;
		}
		if (custom != null) {
			model.custom = custom;
		}
		if (useCustom != null) {
			model.useCustom = useCustom;
		}
		if (sourceData != null && !sourceData.isBlank()) {
			model.sourceData = new JSONObject(sourceData);
		}

		if (!SUPPORTED_FILTERS.contains(filter)) {
			model.sources = emptyRoot(filter.name());
			model.message = filterLabel(filter) + " sources are not available yet";
			model.available = false;
			return model;
		}

		model.sources = sourcesTree(application, dbo, filter, model.sourceData);
		model.modelTree = modelTree(application, dbo, model.projectName, filter, model.sourceData);
		model.sourceValue = toSourceValue(model.projectName, filter, model.sourceData, model.path, model.prefix,
				model.suffix, model.custom, model.useCustom, model.input);
		MobileSmartSourceType smartType = new MobileSmartSourceType();
		smartType.setMode(MobileSmartSourceType.Mode.SOURCE);
		smartType.setSmartValue(model.sourceValue == null ? "{}" : model.sourceValue.toString());
		model.computedValue = smartType.getValue();
		model.available = true;
		return model;
	}

	private static String filterLabel(Filter filter) {
		return switch (filter) {
		case Sequence -> "Sequence";
		case Database -> "FullSync";
		case Action -> "Action";
		case Shared -> "Shared component";
		case Iteration -> "Iteration";
		case Form -> "Form";
		case Global -> "Global";
		case Local -> "Local";
		case Icon -> "Icon";
		case Asset -> "Asset";
		};
	}

	public static boolean isSupported(Filter filter) {
		return SUPPORTED_FILTERS.contains(filter);
	}

	private static NodeModel emptyRoot(String label) {
		NodeModel root = new NodeModel();
		root.type = "root";
		root.label = label;
		root.name = label;
		return root;
	}

	private static ApplicationComponent getApplication(Project project) {
		try {
			if (project.getMobileApplication() != null
					&& project.getMobileApplication().getApplicationComponent() instanceof ApplicationComponent app) {
				return app;
			}
		} catch (Exception e) {
			// No NGX application on this project.
		}
		return null;
	}

	private static MobileSmartSource readSmartSource(DatabaseObject dbo, String propertyName) throws Exception {
		MobileSmartSourceType smartType = readSmartSourceType(dbo, propertyName);
		return smartType == null ? null : smartType.getSmartSource();
	}

	public static MobileSmartSourceType readSmartSourceType(DatabaseObject dbo, String propertyName) throws Exception {
		if (dbo instanceof UIDynamicElement dynamicElement) {
			IonBean ionBean = dynamicElement.getIonBean();
			IonProperty property = ionBean == null ? null : ionBean.getProperty(propertyName);
			if (property != null) {
				return property.getSmartType();
			}
		}
		for (PropertyDescriptor descriptor : Introspector.getBeanInfo(dbo.getClass()).getPropertyDescriptors()) {
			if (!descriptor.getName().equals(propertyName)) {
				continue;
			}
			if (descriptor.getReadMethod() != null
					&& descriptor.getPropertyEditorClass() != null
					&& "NgxSmartSourcePropertyDescriptor".equals(descriptor.getPropertyEditorClass().getSimpleName())) {
				Object value = descriptor.getReadMethod().invoke(dbo);
				return value instanceof MobileSmartSourceType smartType ? smartType : null;
			}
		}
		return null;
	}

	private static Filter parseFilter(String filterName, MobileSmartSource smartSource) {
		if (filterName != null && !filterName.isBlank()) {
			try {
				return Filter.valueOf(filterName);
			} catch (Exception e) {
				// Fall back below.
			}
		}
		Filter filter = smartSource == null ? null : smartSource.getFilter();
		return filter == null ? Filter.Sequence : filter;
	}

	private static NodeModel sourcesTree(ApplicationComponent application, DatabaseObject selectedDbo, Filter filter,
			JSONObject selectedSourceData) {
		NodeModel root = new NodeModel();
		root.type = "root";
		root.label = filter.name();
		root.name = filter.name();
		switch (filter) {
		case Sequence:
			addSequenceSources(application, root, selectedSourceData);
			break;
		case Database:
			addDatabaseSources(application, root, selectedSourceData);
			break;
		case Action:
			addActionSources(sourceContext(application, selectedDbo), selectedDbo, root);
			markSelectedSources(root, filter, selectedSourceData);
			break;
		case Shared:
			addSharedSources(application, selectedDbo, root);
			markSelectedSources(root, filter, selectedSourceData);
			break;
		case Iteration:
			addIterationSources(sourceContext(application, selectedDbo), selectedDbo, root);
			markSelectedSources(root, filter, selectedSourceData);
			break;
		case Form:
			addFormSources(sourceContext(application, selectedDbo), selectedDbo, root);
			markSelectedSources(root, filter, selectedSourceData);
			break;
		case Global:
			addGlobalSource(application, root, selectedSourceData);
			break;
		case Local:
			addLocalSource(application, root, selectedSourceData);
			break;
		case Icon:
			addIconSources(application, root, selectedSourceData);
			break;
		case Asset:
			addAssetSources(application, root, selectedSourceData);
			break;
		default:
			break;
		}
		return root;
	}

	private static void addSequenceSources(ApplicationComponent application, NodeModel root, JSONObject selectedSourceData) {
		List<String> projectNames = Engine.theApp.databaseObjectsManager.getAllProjectNamesList(true);
		projectNames.remove(application.getProject().getName());
		projectNames.add(0, application.getProject().getName());
		for (String projectName : projectNames) {
			try {
				Project project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName, false);
				if (project.getSequencesList().isEmpty()) {
					continue;
				}
				NodeModel projectNode = new NodeModel();
				projectNode.type = "project";
				projectNode.name = project.getName();
				projectNode.label = project.getName();
				projectNode.qname = project.getQName();
				for (Sequence sequence : project.getSequencesList()) {
					NodeModel sequenceNode = sequenceNode(sequence, selectedSourceData);
					projectNode.children.add(sequenceNode);
					for (JSONObject marker : sequenceMarkers(application, sequence)) {
						NodeModel markerNode = sequenceMarkerNode(sequence, marker, selectedSourceData);
						projectNode.children.add(markerNode);
					}
				}
				root.children.add(projectNode);
			} catch (Exception e) {
				// Ignore closed or broken projects in the picker.
			}
		}
	}

	private static List<JSONObject> sequenceMarkers(ApplicationComponent application, Sequence sequence) {
		List<JSONObject> markers = new ArrayList<>();
		try {
			var infos = application.getInfoMap().get(sequence.getQName());
			if (infos == null) {
				return markers;
			}
			for (String info : infos) {
				JSONObject json = new JSONObject(info);
				if (json.has("marker") && !json.optString("marker").isBlank()) {
					markers.add(json);
				}
			}
		} catch (Exception e) {
			// Markers are optional.
		}
		return markers;
	}

	private static NodeModel sequenceNode(Sequence sequence, JSONObject selectedSourceData) throws Exception {
		JSONObject sourceData = new JSONObject().put("sequence", sequence.getQName());
		return sourceNode(Filter.Sequence, sequence.getName(), sequence.getQName(), sourceData, selectedSourceData);
	}

	private static NodeModel sequenceMarkerNode(Sequence sequence, JSONObject marker, JSONObject selectedSourceData)
			throws Exception {
		String markerName = marker.optString("marker");
		JSONObject sourceData = new JSONObject().put("sequence", sequence.getQName()).put("marker", markerName);
		return sourceNode(Filter.Sequence, sequence.getName() + "#" + markerName, sequence.getQName(), sourceData,
				selectedSourceData);
	}

	private static NodeModel sourceNode(Filter filter, String label, String qname, JSONObject sourceData,
			JSONObject selectedSourceData) throws Exception {
		NodeModel node = new NodeModel();
		node.type = "source";
		node.name = label;
		node.label = label;
		node.qname = qname;
		SourceData data = filter.toSourceData(sourceData);
		node.sourceData = data == null ? sourceData : data.toJson();
		node.source = data == null ? "" : data.getSource();
		node.selected = sameSourceData(node.sourceData, selectedSourceData);
		return node;
	}

	private static boolean sameSourceData(JSONObject left, JSONObject right) {
		return left != null && right != null && left.toString().equals(right.toString());
	}

	private static NodeModel groupNode(String label) {
		NodeModel node = new NodeModel();
		node.type = "group";
		node.name = label;
		node.label = label;
		return node;
	}

	private static MobileComponent sourceContext(ApplicationComponent application, DatabaseObject selectedDbo) {
		PageComponent page = pageContext(selectedDbo);
		if (page != null) {
			return page;
		}
		if (selectedDbo instanceof MobileComponent mobileComponent) {
			return mobileComponent;
		}
		return application;
	}

	private static PageComponent pageContext(DatabaseObject dbo) {
		for (DatabaseObject current = dbo; current != null; current = current.getParent()) {
			if (current instanceof PageComponent page) {
				return page;
			}
		}
		return null;
	}

	private static boolean isVisibleInSelectedScope(DatabaseObject selectedDbo, UIComponent component) {
		if (component == null || component.equals(selectedDbo)) {
			return false;
		}
		if (selectedDbo instanceof UIComponent selectedComponent) {
			return selectedComponent.getQName().startsWith(component.getQName() + ".");
		}
		return true;
	}

	private static List<UIComponent> uiChildren(Object object) {
		List<UIComponent> list = new ArrayList<>();
		if (object instanceof PageComponent page) {
			list.addAll(page.getUIComponentList());
		} else if (object instanceof UIComponent component) {
			list.addAll(component.getUIComponentList());
		}
		return list;
	}

	private static void addDatabaseSources(ApplicationComponent application, NodeModel root,
			JSONObject selectedSourceData) {
		NodeModel databasesNode = groupNode("databases");
		List<String> projectNames = Engine.theApp.databaseObjectsManager.getAllProjectNamesList(true);
		projectNames.remove(application.getProject().getName());
		projectNames.add(0, application.getProject().getName());
		for (String projectName : projectNames) {
			try {
				Project project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName, false);
				addProjectDatabaseSources(application, databasesNode, project, selectedSourceData);
			} catch (Exception e) {
				// Ignore closed or broken projects in the picker.
			}
		}
		if (!databasesNode.children.isEmpty()) {
			root.children.add(databasesNode);
		}
	}

	private static void addProjectDatabaseSources(ApplicationComponent application, NodeModel parent, Project project,
			JSONObject selectedSourceData) throws Exception {
		for (Connector connector : project.getConnectorsList()) {
			if (!(connector instanceof FullSyncConnector)) {
				continue;
			}
			NodeModel connectorNode = groupNode(connector.getProject().equals(application.getProject())
					? connector.getName()
					: connector.getQName());
			connectorNode.type = "project";
			connectorNode.qname = connector.getQName();
			for (com.twinsoft.convertigo.beans.core.Document document : connector.getDocumentsList()) {
				if (!(document instanceof DesignDocument designDocument)) {
					continue;
				}
				NodeModel documentNode = groupNode(document.getName());
				documentNode.qname = document.getQName();
				JSONObject views = CouchKey.views.JSONObject(designDocument.getJSONObject());
				if (views == null) {
					continue;
				}
				for (Iterator<String> it = GenericUtils.cast(views.keys()); it.hasNext();) {
					String view = it.next();
					NodeModel viewNode = groupNode(view);
					addDatabaseVerbSources(application, connector, designDocument, view, "get", viewNode,
							selectedSourceData);
					addDatabaseVerbSources(application, connector, designDocument, view, "view", viewNode,
							selectedSourceData);
					if (!viewNode.children.isEmpty()) {
						documentNode.children.add(viewNode);
					}
				}
				if (!documentNode.children.isEmpty()) {
					connectorNode.children.add(documentNode);
				}
			}
			if (!connectorNode.children.isEmpty()) {
				parent.children.add(connectorNode);
			}
		}
	}

	private static void addDatabaseVerbSources(ApplicationComponent application, Connector connector,
			DesignDocument document, String view, String verb, NodeModel viewNode, JSONObject selectedSourceData)
			throws Exception {
		JSONObject baseSourceData = new JSONObject()
				.put("connector", connector.getQName())
				.put("document", document.getQName())
				.put("queryview", view)
				.put("verb", verb);
		viewNode.children.add(sourceNode(Filter.Database, verb, document.getQName(), baseSourceData,
				selectedSourceData));

		String key = connector.getQName() + "." + document.getName() + "." + view + "." + verb;
		Set<String> infos = application.getInfoMap().get(key);
		if (infos == null && "get".equals(verb)) {
			infos = application.getInfoMap().get(connector.getQName() + ".get");
		}
		if (infos == null) {
			return;
		}
		for (String info : infos) {
			JSONObject jsonInfo = new JSONObject(info);
			String marker = jsonInfo.optString("marker");
			if (marker.isBlank()) {
				continue;
			}
			boolean includeDocs = Boolean.parseBoolean(jsonInfo.optString("include_docs", "false"));
			JSONObject markerSourceData = new JSONObject(baseSourceData.toString())
					.put("marker", marker)
					.put("includeDocs", includeDocs);
			viewNode.children.add(sourceNode(Filter.Database, verb + "#" + marker, document.getQName(),
					markerSourceData, selectedSourceData));
		}
	}

	private static void addActionSources(Object sourceContext, DatabaseObject selectedDbo, NodeModel root) {
		NodeModel actionsNode = groupNode("actions");
		PageComponent page = pageContext(selectedDbo);
		if (page != null) {
			NodeModel localsNode = groupNode("locals");
			try {
				localsNode.children.add(sourceNode(Filter.Action, page.getName(), page.getQName(),
						new JSONObject().put("pageLocals", true), null));
			} catch (Exception e) {
				// Ignore malformed page locals.
			}
			actionsNode.children.add(localsNode);
		}

		NodeModel eventsNode = groupNode("events");
		NodeModel controlsNode = groupNode("controls");
		actionsNode.children.add(eventsNode);
		actionsNode.children.add(controlsNode);
		addActionSources(sourceContext, selectedDbo, actionsNode, eventsNode, controlsNode);
		actionsNode.children.removeIf(child -> child.children.isEmpty());
		if (!actionsNode.children.isEmpty()) {
			root.children.add(actionsNode);
		}
	}

	private static void addActionSources(Object object, DatabaseObject selectedDbo, NodeModel actionsNode,
			NodeModel eventsNode, NodeModel controlsNode) {
		for (UIComponent component : actionChildren(object)) {
			if (!isVisibleInSelectedScope(selectedDbo, component)) {
				continue;
			}
			try {
				if (component instanceof UIAppEvent
						|| component instanceof UIPageEvent
						|| component instanceof UISharedComponentEvent
						|| component instanceof UIEventSubscriber) {
					NodeModel eventNode = sourceNode(Filter.Action, component.toString(), component.getQName(),
							new JSONObject().put("priority", component.priority).put("rootEvent", true), null);
					eventsNode.children.add(eventNode);
					addActionSources(component, selectedDbo, eventNode, eventNode, eventNode);
				} else if (component instanceof UIActionEvent || component instanceof UIControlEvent) {
					NodeModel controlNode = component instanceof UIControlEvent
							? sourceNode(Filter.Action, component.toString(), component.getQName(),
									new JSONObject().put("priority", component.priority).put("rootEvent", true), null)
							: groupNode(component.toString());
					controlNode.qname = component.getQName();
					controlsNode.children.add(controlNode);
					addActionSources(component, selectedDbo, controlNode, controlNode, controlNode);
				} else if (component instanceof IAction || component instanceof UIActionStack) {
					NodeModel actionNode = sourceNode(Filter.Action, component.toString(), component.getQName(),
							new JSONObject().put("priority", component.priority), null);
					actionsNode.children.add(actionNode);
					addActionSources(component, selectedDbo, actionNode, actionNode, actionNode);
				} else {
					addActionSources(component, selectedDbo, actionsNode, eventsNode, controlsNode);
				}
			} catch (Exception e) {
				// Skip broken mobile action branches.
			}
		}
	}

	private static List<UIComponent> actionChildren(Object object) {
		List<UIComponent> list = new ArrayList<>();
		if (object instanceof ApplicationComponent application) {
			list.addAll(GenericUtils.cast(application.getUIAppEventList()));
			list.addAll(GenericUtils.cast(application.getUIEventSubscriberList()));
			list.addAll(GenericUtils.cast(application.getSharedActionList()));
		} else if (object instanceof UIAppEvent appEvent) {
			list.addAll(appEvent.getUIComponentList());
		} else if (object instanceof UIActionStack actionStack) {
			list.addAll(actionStack.getUIComponentList());
		} else if (object instanceof UISharedComponent sharedComponent) {
			list.addAll(sharedComponent.getUIComponentList());
		} else {
			list.addAll(uiChildren(object));
		}
		return list;
	}

	private static void addSharedSources(ApplicationComponent application, DatabaseObject selectedDbo, NodeModel root) {
		NodeModel sharedNode = groupNode("shared");
		for (UISharedComponent component : application.getSharedComponentList()) {
			if (!isVisibleInSelectedScope(selectedDbo, component)) {
				continue;
			}
			try {
				sharedNode.children.add(sourceNode(Filter.Shared, component.toString(), component.getQName(),
						new JSONObject()
								.put("priority", component.priority)
								.put("regular", component.isRegular()),
						null));
			} catch (Exception e) {
				// Skip malformed shared components.
			}
		}
		if (!sharedNode.children.isEmpty()) {
			root.children.add(sharedNode);
		}
	}

	private static void addIterationSources(Object object, DatabaseObject selectedDbo, NodeModel parent) {
		for (UIComponent component : uiChildren(object)) {
			if (component.equals(selectedDbo)) {
				continue;
			}
			if (component instanceof UIControlDirective directive
					&& AttrDirective.isForDirective(directive.getDirectiveName())) {
				if (!isVisibleInSelectedScope(selectedDbo, component)) {
					continue;
				}
				try {
					NodeModel directiveNode = sourceNode(Filter.Iteration, component.toString(), component.getQName(),
							new JSONObject().put("priority", component.priority), null);
					parent.children.add(directiveNode);
					addIterationSources(component, selectedDbo, directiveNode);
				} catch (Exception e) {
					// Skip malformed iterations.
				}
			} else {
				addIterationSources(component, selectedDbo, parent);
			}
		}
	}

	private static void addFormSources(Object object, DatabaseObject selectedDbo, NodeModel parent) {
		for (UIComponent component : uiChildren(object)) {
			if (component.equals(selectedDbo)) {
				continue;
			}
			if (component instanceof UIForm form) {
				try {
					NodeModel formNode = sourceNode(Filter.Form, component.toString(), component.getQName(),
							new JSONObject()
									.put("priority", component.priority)
									.put("identifier", form.getIdentifier()),
							null);
					parent.children.add(formNode);
					addFormSources(component, selectedDbo, formNode);
				} catch (Exception e) {
					// Skip malformed forms.
				}
			} else {
				addFormSources(component, selectedDbo, parent);
			}
		}
	}

	private static void addGlobalSource(ApplicationComponent application, NodeModel root, JSONObject selectedSourceData) {
		try {
			JSONObject sourceData = new JSONObject().put("sharedObject", "router.sharedObject");
			NodeModel node = sourceNode(Filter.Global, "sharedObject", application.getQName(), sourceData,
					selectedSourceData);
			node.value = globalsJsonModel(application).optString("sharedObject");
			root.children.add(node);
		} catch (Exception e) {
			// Ignore malformed global state.
		}
	}

	private static void addLocalSource(ApplicationComponent application, NodeModel root, JSONObject selectedSourceData) {
		try {
			JSONObject sourceData = new JSONObject().put("localObject", "local");
			NodeModel node = sourceNode(Filter.Local, "localObject", application.getQName(), sourceData,
					selectedSourceData);
			node.value = localsJsonModel(application).optString("localObject");
			root.children.add(node);
		} catch (Exception e) {
			// Ignore malformed local state.
		}
	}

	private static void addIconSources(ApplicationComponent application, NodeModel root,
			JSONObject selectedSourceData) {
		NodeModel iconsNode = groupNode("icons");
		for (String icon : ionIconNames(application.getProject())) {
			try {
				JSONObject sourceData = new JSONObject().put("icon", quoteSource(icon));
				iconsNode.children.add(sourceNode(Filter.Icon, icon, application.getQName(), sourceData,
						selectedSourceData));
			} catch (Exception e) {
				// Ignore malformed icon names.
			}
		}
		if (!iconsNode.children.isEmpty()) {
			root.children.add(iconsNode);
		}
	}

	private static Set<String> ionIconNames(Project project) {
		Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		File ionicDir = new File(project.getDirFile(), "_private/ionic/node_modules/ionicons/dist");
		addIonIconNamesFromCheatsheet(names, new File(ionicDir, "cheatsheet.html"));
		addIonIconNamesFromSvgDir(names, new File(ionicDir, "ionicons/svg"));
		addIonIconNamesFromSvgDir(names, new File(ionicDir, "svg"));
		return names;
	}

	private static void addIonIconNamesFromCheatsheet(Set<String> names, File cheatsheet) {
		if (!cheatsheet.isFile()) {
			return;
		}
		try {
			String html = Files.readString(cheatsheet.toPath(), StandardCharsets.UTF_8);
			Matcher matcher = Pattern.compile("(?:href|xlink:href)=[\"']#([^\"']+)[\"']").matcher(html);
			while (matcher.find()) {
				names.add(matcher.group(1));
			}
			matcher = Pattern.compile("<symbol\\s+[^>]*id=[\"']([^\"']+)[\"']").matcher(html);
			while (matcher.find()) {
				names.add(matcher.group(1));
			}
		} catch (Exception e) {
			// The icon list is optional and depends on the mobile build output.
		}
	}

	private static void addIonIconNamesFromSvgDir(Set<String> names, File svgDir) {
		File[] files = svgDir.isDirectory() ? svgDir.listFiles((dir, name) -> name.endsWith(".svg")) : null;
		if (files == null) {
			return;
		}
		for (File file : files) {
			String name = file.getName();
			names.add(name.substring(0, name.length() - 4));
		}
	}

	private static void addAssetSources(ApplicationComponent application, NodeModel root,
			JSONObject selectedSourceData) {
		File assetsDir = new File(application.getProject().getDirFile(), "DisplayObjects/mobile/assets");
		if (!assetsDir.isDirectory()) {
			return;
		}
		NodeModel assetsNode = groupNode("assets");
		addAssetChildren(application, assetsNode, assetsDir, assetsDir.getParentFile(), selectedSourceData);
		if (!assetsNode.children.isEmpty()) {
			root.children.add(assetsNode);
		}
	}

	private static void addAssetChildren(ApplicationComponent application, NodeModel parent, File directory,
			File relativeRoot, JSONObject selectedSourceData) {
		File[] files = directory.listFiles();
		if (files == null) {
			return;
		}
		Arrays.sort(files, (left, right) -> {
			if (left.isDirectory() && right.isFile()) {
				return -1;
			}
			if (left.isFile() && right.isDirectory()) {
				return 1;
			}
			return left.getName().compareToIgnoreCase(right.getName());
		});
		for (File file : files) {
			if (file.isDirectory()) {
				NodeModel folder = groupNode(file.getName());
				addAssetChildren(application, folder, file, relativeRoot, selectedSourceData);
				if (!folder.children.isEmpty()) {
					parent.children.add(folder);
				}
				continue;
			}
			try {
				String path = relativeRoot.toPath().relativize(file.toPath()).toString().replace('\\', '/');
				JSONObject sourceData = new JSONObject().put("asset", quoteSource(path));
				NodeModel node = sourceNode(Filter.Asset, file.getName(), application.getQName(), sourceData,
						selectedSourceData);
				node.value = path;
				parent.children.add(node);
			} catch (Exception e) {
				// Ignore malformed asset paths.
			}
		}
	}

	private static String quoteSource(String value) {
		return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'";
	}

	private static void markSelectedSources(NodeModel node, Filter filter, JSONObject selectedSourceData) {
		if (node == null) {
			return;
		}
		if (node.sourceData != null) {
			node.selected = sameSourceData(node.sourceData, selectedSourceData);
			SourceData data = filter.toSourceData(node.sourceData);
			node.source = data == null ? "" : data.getSource();
		}
		for (NodeModel child : node.children) {
			markSelectedSources(child, filter, selectedSourceData);
		}
	}

	private static NodeModel modelTree(ApplicationComponent application, DatabaseObject selectedDbo, String projectName,
			Filter filter, JSONObject sourceData) throws Exception {
		if (sourceData == null) {
			return null;
		}
		JSONObject json = switch (filter) {
		case Sequence -> sequenceJsonModel(sourceData);
		case Database -> databaseJsonModel(sourceData);
		case Action -> actionJsonModel(application, sourceData);
		case Shared -> uiJsonModel(sourceByPriority(application, sourceData.optLong("priority", 0)));
		case Iteration -> iterationJsonModel(application, selectedDbo, sourceData);
		case Form -> uiJsonModel(sourceByPriority(application, sourceData.optLong("priority", 0)));
		case Global -> globalsJsonModel(application);
		case Local -> localsJsonModel(application);
		case Icon, Asset -> null;
		default -> null;
		};
		if (json == null) {
			return null;
		}
		cleanJsonModel(json);
		return toJsonNodeModel("root", json, "");
	}

	private static JSONObject sequenceJsonModel(JSONObject sourceData) throws Exception {
		if (!sourceData.has("sequence")) {
			return null;
		}
		DatabaseObject dbo = Engine.theApp.databaseObjectsManager
				.getDatabaseObjectByQName(sourceData.getString("sequence"));
		return dbo instanceof RequestableObject requestable ? requestableJsonModel(requestable) : null;
	}

	private static JSONObject databaseJsonModel(JSONObject sourceData) throws Exception {
		DatabaseObject dbo = Engine.theApp.databaseObjectsManager
				.getDatabaseObjectByQName(sourceData.optString("connector"));
		if (!(dbo instanceof Connector connector)) {
			return null;
		}

		JSONObject json = couchTransactionJsonModel(connector, new GetViewTransaction());
		if (!"get".equals(sourceData.optString("verb"))) {
			return json;
		}

		JSONObject valueRows = findJSONObject(json, "rows.value");
		return valueRows.length() == 0 ? json : valueRows;
	}

	private static JSONObject actionJsonModel(ApplicationComponent application, JSONObject sourceData) throws Exception {
		if (sourceData.optBoolean("pageLocals")) {
			return new JSONObject().put("navParams", new JSONObject().put("data", ""));
		}
		return jsonModelFor(sourceByPriority(application, sourceData.optLong("priority", 0)));
	}

	private static JSONObject iterationJsonModel(ApplicationComponent application, DatabaseObject selectedDbo,
			JSONObject sourceData) throws Exception {
		DatabaseObject dbo = sourceByPriority(application, sourceData.optLong("priority", 0));
		if (!(dbo instanceof UIControlDirective directive)) {
			return null;
		}
		MobileSmartSourceType smartType = directive.getSourceSmartType();
		MobileSmartSource smartSource = smartType == null ? null : smartType.getSmartSource();
		if (smartSource == null) {
			return null;
		}
		DatabaseObject sourceDbo = smartSource.getDatabaseObject(rootName(selectedDbo));
		JSONObject json = jsonModelFor(sourceDbo);
		String path = smartSource.getModelPath().replace("?.", ".");
		return path.isBlank() ? json : findJSONObject(json, path);
	}

	private static String rootName(DatabaseObject selectedDbo) {
		PageComponent page = pageContext(selectedDbo);
		return page == null ? "" : page.getName();
	}

	private static DatabaseObject sourceByPriority(DatabaseObject root, long priority) {
		if (root == null || priority == 0) {
			return null;
		}
		if (root.priority == priority) {
			return root;
		}
		for (DatabaseObject child : root.getAllChildren()) {
			DatabaseObject found = sourceByPriority(child, priority);
			if (found != null) {
				return found;
			}
		}
		return null;
	}

	private static JSONObject uiJsonModel(DatabaseObject dbo) throws Exception {
		return dbo == null ? null : jsonModelFor(dbo);
	}

	private static JSONObject jsonModelFor(DatabaseObject dbo) throws Exception {
		if (dbo instanceof RequestableObject requestable) {
			return requestableJsonModel(requestable);
		}
		if (dbo instanceof UIForm form) {
			return new JSONObject(form.computeJsonModel());
		}
		if (dbo instanceof UIActionStack actionStack) {
			return new JSONObject(actionStack.computeJsonModel());
		}
		if (dbo instanceof UIDynamicAction action) {
			return dynamicActionJsonModel(action);
		}
		if (dbo instanceof UICustomAction action) {
			return new JSONObject(action.computeJsonModel());
		}
		if (dbo instanceof IEventGenerator && dbo instanceof UIComponent component) {
			return new JSONObject(component.computeJsonModel());
		}
		if (dbo instanceof UISharedComponent component) {
			return new JSONObject(component.computeJsonModel());
		}
		if (dbo instanceof UIComponent component) {
			return new JSONObject(component.computeJsonModel());
		}
		return null;
	}

	private static JSONObject dynamicActionJsonModel(UIDynamicAction action) throws Exception {
		JSONObject jsonObject = new JSONObject(action.computeJsonModel());
		IonBean ionBean = action.getIonBean();
		if (ionBean == null) {
			return jsonObject;
		}

		String name = ionBean.getName();
		JSONObject output = null;
		if ("CallSequenceAction".equals(name)) {
			output = requestableActionOutput(ionBean);
		} else if ("CallFullSyncAction".equals(name)) {
			output = fullSyncActionOutput(ionBean, ionPropertyValue(ionBean, "verb"));
		} else if (name.startsWith("FullSync")) {
			output = fullSyncActionOutput(ionBean, null);
		}
		if (output != null && jsonObject.has("out")) {
			jsonObject.put("out", output);
		}
		return jsonObject;
	}

	private static JSONObject requestableActionOutput(IonBean ionBean) throws Exception {
		DatabaseObject dbo = Engine.theApp.databaseObjectsManager
				.getDatabaseObjectByQName(ionPropertyValue(ionBean, "requestable"));
		return dbo instanceof RequestableObject requestable ? requestableJsonModel(requestable) : null;
	}

	private static JSONObject fullSyncActionOutput(IonBean ionBean, String verb) throws Exception {
		Connector connector = fullSyncConnector(ionBean);
		if (connector == null) {
			return null;
		}

		AbstractCouchDbTransaction transaction = couchTransaction(ionBean.getName(), verb);
		if (transaction == null) {
			return null;
		}

		return couchTransactionJsonModel(connector, transaction);
	}

	private static JSONObject couchTransactionJsonModel(Connector connector, AbstractCouchDbTransaction transaction)
			throws Exception {
		XmlSchema schema = Engine.theApp.schemaManager.getSchemaForProject(connector.getProject().getName());
		QName typeQName = transaction.getComplexTypeAffectation();
		XmlSchemaType xmlSchemaType = schema == null || typeQName == null ? null : schema.getTypeByName(typeQName);
		if (xmlSchemaType == null) {
			return null;
		}

		Document document = XmlSchemaUtils.getDomInstance(xmlSchemaType);
		JSONObject jsonOutput = new JSONObject(XMLUtils.XmlToJson(document.getDocumentElement(), true, true))
				.getJSONObject("document");
		cleanJsonModel(jsonOutput);
		jsonOutput.remove("_c8oMeta");
		jsonOutput.remove("error");
		jsonOutput.remove("reason");
		return jsonOutput;
	}

	private static Connector fullSyncConnector(IonBean ionBean) throws Exception {
		String actionName = ionBean == null ? "" : ionBean.getName();
		String qname = "FullSyncViewAction".equals(actionName)
				? designDocumentQName(ionPropertyValue(ionBean, "fsview"))
				: ionPropertyValue(ionBean, "requestable");
		DatabaseObject dbo = Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(qname);
		if (dbo instanceof Connector connector) {
			return connector;
		}
		if (dbo instanceof DesignDocument designDocument) {
			return designDocument.getConnector();
		}
		return null;
	}

	private static String designDocumentQName(String fsview) {
		int index = fsview == null ? -1 : fsview.lastIndexOf('.');
		return index < 0 ? "" : fsview.substring(0, index);
	}

	private static AbstractCouchDbTransaction couchTransaction(String actionName, String verb) {
		if ("CallFullSyncAction".equals(actionName)) {
			return switch (verb == null ? "" : verb) {
			case "all" -> new AllDocsTransaction();
			case "create" -> new PutDatabaseTransaction();
			case "destroy" -> new DeleteDatabaseTransaction();
			case "get" -> new GetDocumentTransaction();
			case "delete" -> new DeleteDocumentTransaction();
			case "delete_attachment" -> new DeleteDocumentAttachmentTransaction();
			case "post" -> new PostDocumentTransaction();
			case "put_attachment" -> new PutDocumentAttachmentTransaction();
			case "replicate_push" -> new PostReplicateTransaction();
			case "reset" -> new ResetDatabaseTransaction();
			case "view" -> new GetViewTransaction();
			default -> null;
			};
		}
		return switch (actionName) {
		case "FullSyncDeleteAction" -> new DeleteDocumentTransaction();
		case "FullSyncDeleteAttachmentAction" -> new DeleteDocumentAttachmentTransaction();
		case "FullSyncGetAction" -> new GetDocumentTransaction();
		case "FullSyncPostAction" -> new PostDocumentTransaction();
		case "FullSyncPutAttachmentAction" -> new PutDocumentAttachmentTransaction();
		case "FullSyncViewAction" -> new GetViewTransaction();
		default -> null;
		};
	}

	private static String ionPropertyValue(IonBean ionBean, String propertyName) {
		IonProperty property = ionBean == null ? null : ionBean.getProperty(propertyName);
		Object value = property == null ? null : property.getValue();
		return value == null ? "" : value.toString();
	}

	private static JSONObject globalsJsonModel(ApplicationComponent application) throws Exception {
		Map<String, UIDynamicAction> globals = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		collectSetActions(application, globals, true);
		JSONObject model = new JSONObject();
		for (String key : globals.keySet()) {
			if ("FullSyncSyncAction".equals(key)) {
				model.put(key, new JSONObject().put("progress", new JSONObject()
						.put("changed", "")
						.put("continuous", "")
						.put("finished", "")
						.put("pull", "")
						.put("current", "")
						.put("total", "")
						.put("status", "")
						.put("taskInfo", "")
						.put("raw", "")));
			} else {
				model.put(key, "");
			}
		}
		return model;
	}

	private static JSONObject localsJsonModel(ApplicationComponent application) throws Exception {
		Map<String, UIDynamicAction> locals = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		collectSetActions(application, locals, false);
		JSONObject model = new JSONObject();
		for (String key : locals.keySet()) {
			model.put(key, "");
		}
		return model;
	}

	private static void collectSetActions(DatabaseObject object, Map<String, UIDynamicAction> actions, boolean global) {
		if (object == null) {
			return;
		}
		if (object instanceof UIDynamicAction action) {
			if (global && action.isSetGlobalAction()) {
				String key = action.getSetActionKeyName();
				if (key != null && !key.isEmpty()) {
					actions.putIfAbsent(key, action);
				}
			} else if (!global && action.isSetLocalAction()) {
				String key = action.getSetActionKeyName();
				if (key != null && !key.isEmpty()) {
					actions.putIfAbsent(key, action);
				}
			}
			if (global && action.isFullSyncSyncAction()) {
				actions.putIfAbsent("FullSyncSyncAction", action);
			}
		}
		for (DatabaseObject child : object.getAllChildren()) {
			collectSetActions(child, actions, global);
			if (child instanceof UIUseShared useShared && !useShared.isRecursive()) {
				UISharedComponent sharedComponent = useShared.getTargetSharedComponent();
				if (sharedComponent != null && sharedComponent.isEnabled()) {
					collectSetActions(sharedComponent, actions, global);
				}
			}
			if (child instanceof UIDynamicInvoke invoke && !invoke.isRecursive()) {
				UIActionStack actionStack = invoke.getTargetSharedAction();
				if (actionStack != null && actionStack.isEnabled()) {
					collectSetActions(actionStack, actions, global);
				}
			}
		}
	}

	private static JSONObject requestableJsonModel(RequestableObject requestable) throws Exception {
		Project project = requestable.getProject();
		XmlSchema schema = Engine.theApp.schemaManager.getSchemaForProject(project.getName());
		XmlSchemaObject xso = SchemaMeta.getXmlSchemaObject(schema, requestable);
		if (xso == null) {
			return new JSONObject();
		}
		Document document = XmlSchemaUtils.getDomInstance(xso);
		String jsonString = XMLUtils.XmlToJson(document.getDocumentElement(), true, true);
		JSONObject jsonObject = new JSONObject(jsonString);

		String responseEltName = requestable.getXsdTypePrefix() + requestable.getName() + "Response";
		JSONObject jsonOutput = findJSONObject(jsonObject, "document." + responseEltName + ".response");
		return com.twinsoft.convertigo.engine.enums.JsonOutput.JsonRoot.docNode.equals(project.getJsonRoot())
				? new JSONObject().put("document", jsonOutput)
				: jsonOutput;
	}

	private static JSONObject findJSONObject(JSONObject object, String searchPath) throws Exception {
		JSONObject current = object;
		for (String part : searchPath.split("\\.")) {
			if (part.isBlank()) {
				continue;
			}
			Object child = current.opt(part);
			if (child instanceof JSONObject jsonChild) {
				current = jsonChild;
			} else {
				return new JSONObject();
			}
		}
		return current;
	}

	private static void cleanJsonModel(Object object) throws Exception {
		if (object instanceof JSONObject jsonObject) {
			jsonObject.remove("text");
			jsonObject.remove("attr");
			JSONArray names = jsonObject.names();
			if (names != null) {
				for (int i = 0; i < names.length(); i++) {
					cleanJsonModel(jsonObject.get(names.getString(i)));
				}
			}
		} else if (object instanceof JSONArray jsonArray) {
			for (int i = 0; i < jsonArray.length(); i++) {
				cleanJsonModel(jsonArray.get(i));
			}
		}
	}

	private static NodeModel toJsonNodeModel(String label, Object value, String parentPath) throws Exception {
		NodeModel model = new NodeModel();
		model.type = value instanceof JSONArray ? "array" : value instanceof JSONObject ? "object" : "value";
		model.label = label;
		model.name = label;
		model.path = parentPath;
		if (value instanceof JSONObject object) {
			for (Iterator<?> i = object.keys(); i.hasNext();) {
				String key = String.valueOf(i.next());
				model.children.add(toJsonNodeModel(key, object.get(key), childPath(parentPath, key)));
			}
		} else if (value instanceof JSONArray array) {
			for (int i = 0; i < array.length(); i++) {
				String key = "[" + i + "]";
				model.children.add(toJsonNodeModel(key, array.get(i), childPath(parentPath, key)));
			}
		} else {
			model.value = value == null || JSONObject.NULL.equals(value) ? "null" : String.valueOf(value);
		}
		return model;
	}

	private static String childPath(String parentPath, String key) {
		String segment = key.matches("[A-Za-z_$][A-Za-z0-9_$]*") ? key : "['" + key.replace("'", "\\'") + "']";
		if (key.matches("\\[\\d+\\]")) {
			segment = key;
		}
		return parentPath + "?." + segment;
	}

	public static JSONObject toSourceValue(String projectName, Filter filter, JSONObject sourceData, String path,
			String prefix, String suffix, String custom, boolean useCustom, String input) throws Exception {
		SourceModel sourceModel = MobileSmartSource.emptyModel(filter);
		sourceModel.setPath(path == null ? "" : path);
		sourceModel.setPrefix(prefix == null ? "" : prefix);
		sourceModel.setSuffix(suffix == null ? "" : suffix);
		sourceModel.setCustom(custom == null ? "" : custom);
		sourceModel.setUseCustom(useCustom);
		if (sourceData != null) {
			SourceData data = filter.toSourceData(sourceData);
			sourceModel.addSourceData(data);
		}
		MobileSmartSource source = new MobileSmartSource(filter, projectName, input == null ? "" : input,
				sourceModel.toJson());
		return new JSONObject(source.toJsonString());
	}
}
