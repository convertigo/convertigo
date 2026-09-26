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

package com.twinsoft.convertigo.engine.admin.services.studio.properties;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import jakarta.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.connectors.FullSyncConnector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.MobileApplication;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.core.UrlAuthentication;
import com.twinsoft.convertigo.beans.core.UrlMappingOperation;
import com.twinsoft.convertigo.beans.couchdb.AbstractFullSyncFilterListener;
import com.twinsoft.convertigo.beans.couchdb.AbstractFullSyncListener;
import com.twinsoft.convertigo.beans.couchdb.AbstractFullSyncViewListener;
import com.twinsoft.convertigo.beans.couchdb.DesignDocument;
import com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.ngx.components.PageComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIActionStack;
import com.twinsoft.convertigo.beans.ngx.components.UICompEvent;
import com.twinsoft.convertigo.beans.ngx.components.UICompVariable;
import com.twinsoft.convertigo.beans.ngx.components.UIComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicAction;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicAnimate;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicElement;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicInfiniteScroll;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicInvoke;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicMenu;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicMenuItem;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicTab;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicTabButton;
import com.twinsoft.convertigo.beans.ngx.components.UIElement;
import com.twinsoft.convertigo.beans.ngx.components.UISharedComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIUseShared;
import com.twinsoft.convertigo.beans.steps.SequenceStep;
import com.twinsoft.convertigo.beans.steps.TransactionStep;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.studio.Utils;
import com.twinsoft.convertigo.engine.util.GenericUtils;

/**
 * The objects a property naming another object can name, as the named source selector of the Eclipse
 * Studio lists them: a requestable, a page, a shared component, a view of a design document…, named by
 * the names of its path from its project.
 * <ul>
 * <li>id: the object</li>
 * <li>property: its property</li>
 * </ul>
 */
@ServiceDefinition(name = "NamedSources", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG }, parameters = {}, returnValue = "")
public class NamedSources extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var dbo = Utils.getDbo(request.getParameter("id"));
		var property = String.valueOf(request.getParameter("property"));
		if (dbo == null) {
			throw new ServiceException("The object " + request.getParameter("id") + " does not exist.");
		}
		if (!handles(dbo, property)) {
			throw new ServiceException("The property " + property + " does not name another object.");
		}
		var items = new JSONArray();
		var current = dbo.getProject();
		var names = new ArrayList<>(Engine.theApp.databaseObjectsManager.getAllProjectNamesList());
		names.sort((a, b) -> a.equals(current.getName()) ? -1 : b.equals(current.getName()) ? 1 : a.compareToIgnoreCase(b));
		var deep = deep(dbo, property);
		for (var name : names) {
			var project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(name);
			if (project != null) {
				visit(dbo, property, project, deep, items);
			}
		}
		response.put("items", items);
	}

	/**
	 * @return whether the property of the object names another object
	 */
	static boolean handles(DatabaseObject dbo, String property) {
		if (dbo instanceof UrlMappingOperation) {
			return "targetRequestable".equals(property);
		}
		if (dbo instanceof UrlAuthentication) {
			return "authRequestable".equals(property);
		}
		if (dbo instanceof TransactionStep) {
			return "sourceTransaction".equals(property);
		}
		if (dbo instanceof SequenceStep) {
			return "sourceSequence".equals(property);
		}
		if (dbo instanceof AbstractFullSyncListener) {
			return "targetSequence".equals(property) || "targetView".equals(property) || "targetFilter".equals(property);
		}
		if (dbo instanceof MobileApplication) {
			return "fsConnector".equals(property) || "fsDesignDocument".equals(property);
		}
		if (dbo instanceof PageComponent) {
			return "startMenu".equals(property) || "endMenu".equals(property);
		}
		if (dbo instanceof UIDynamicTabButton || dbo instanceof UIDynamicTab) {
			return "tabpage".equals(property);
		}
		if (dbo instanceof UIDynamicMenuItem) {
			return "itempage".equals(property);
		}
		if (dbo instanceof UIDynamicAnimate) {
			return "identifiable".equals(property);
		}
		if (dbo instanceof UIDynamicInvoke) {
			return "stack".equals(property);
		}
		if (dbo instanceof UIUseShared) {
			return "sharedcomponent".equals(property);
		}
		if (dbo instanceof UIDynamicInfiniteScroll) {
			return "scrollaction".equals(property);
		}
		if (dbo instanceof UIDynamicElement) {
			return "requestable".equals(property) || "fsview".equals(property) || "page".equals(property)
					|| "event".equals(property) || "compvar".equals(property);
		}
		return false;
	}

	/**
	 * @return whether the candidates are inside the pages and the shared components
	 */
	private static boolean deep(DatabaseObject dbo, String property) {
		return "identifiable".equals(property) || "event".equals(property) || "compvar".equals(property)
				|| "scrollaction".equals(property);
	}

	private static void visit(DatabaseObject dbo, String property, DatabaseObject candidate, boolean deep,
			JSONArray items) throws Exception {
		add(dbo, property, candidate, candidate.getTokenPath(null), items);
		var children = new ArrayList<DatabaseObject>();
		if (candidate instanceof Project project) {
			if (project.getMobileApplication() != null) {
				children.add(project.getMobileApplication());
			}
			children.addAll(project.getConnectorsList());
			children.addAll(project.getSequencesList());
		} else if (candidate instanceof MobileApplication application) {
			if (application.getApplicationComponent() instanceof ApplicationComponent component) {
				children.add(component);
			}
		} else if (candidate instanceof ApplicationComponent application) {
			children.addAll(application.getMenuComponentList());
			children.addAll(application.getPageComponentList());
			children.addAll(application.getSharedActionList());
			children.addAll(application.getSharedComponentList());
		} else if (deep && (candidate instanceof UIDynamicMenu || candidate instanceof PageComponent
				|| candidate instanceof UIComponent)) {
			children.addAll(uiComponents(candidate));
		} else if (candidate instanceof Connector connector) {
			children.addAll(connector.getTransactionsList());
			children.addAll(connector.getDocumentsList());
			children.addAll(connector.getListenersList());
		} else if (candidate instanceof DesignDocument document) {
			var json = document.getJSONObject();
			var keys = new ArrayList<String>();
			if (dbo instanceof AbstractFullSyncViewListener || dbo instanceof UIDynamicElement) {
				keys.add("views");
			}
			if (dbo instanceof AbstractFullSyncFilterListener) {
				keys.add("filters");
			}
			for (var key : keys) {
				var functions = json == null ? null : json.optJSONObject(key);
				if (functions != null) {
					for (Iterator<String> i = GenericUtils.cast(functions.keys()); i.hasNext();) {
						var name = candidate.getTokenPath(null) + "." + i.next();
						add(dbo, property, name, name, items);
					}
				}
			}
		}
		for (var child : children) {
			visit(dbo, property, child, deep, items);
		}
	}

	private static List<UIComponent> uiComponents(DatabaseObject dbo) {
		if (dbo instanceof PageComponent page) {
			return page.getUIComponentList();
		}
		if (dbo instanceof UIComponent component) {
			return component.getUIComponentList();
		}
		return List.of();
	}

	private static void add(DatabaseObject dbo, String property, Object candidate, String name, JSONArray items)
			throws Exception {
		if (!selectable(dbo, property, candidate)) {
			return;
		}
		var item = new JSONObject().put("name", name);
		if (candidate instanceof DatabaseObject object) {
			item.put("label", object.toString());
			item.put("type", object.getClass().getSimpleName());
			item.put("project", object.getProject().getName());
		} else {
			item.put("label", name.substring(name.lastIndexOf('.') + 1));
			item.put("type", "view".equals(property) || "targetView".equals(property) || "fsview".equals(property)
					? "View" : "Filter");
			item.put("project", name.substring(0, name.indexOf('.')));
		}
		items.put(item);
	}

	/**
	 * @return whether the property of the object can name the candidate, as the Eclipse Studio allows it
	 */
	private static boolean selectable(DatabaseObject dbo, String property, Object candidate) {
		var same = candidate instanceof DatabaseObject object && object.getProject().equals(dbo.getProject());
		if (dbo instanceof UrlMappingOperation || dbo instanceof UrlAuthentication) {
			return candidate instanceof RequestableObject;
		}
		if (dbo instanceof TransactionStep) {
			return candidate instanceof Transaction;
		}
		if (dbo instanceof SequenceStep) {
			return candidate instanceof Sequence;
		}
		if (dbo instanceof AbstractFullSyncListener) {
			if ("targetSequence".equals(property)) {
				return candidate instanceof Sequence;
			}
			return candidate instanceof String name && name.startsWith(dbo.getParent().getTokenPath(null));
		}
		if (dbo instanceof MobileApplication application) {
			if ("fsConnector".equals(property)) {
				return candidate instanceof FullSyncConnector;
			}
			return candidate instanceof DesignDocument document && document.getParent() instanceof FullSyncConnector
					&& document.getParent().getTokenPath(null).equals(application.getFsConnector());
		}
		if (dbo instanceof PageComponent) {
			return candidate instanceof UIDynamicMenu && same;
		}
		if (dbo instanceof UIDynamicTabButton || dbo instanceof UIDynamicTab || dbo instanceof UIDynamicMenuItem) {
			return candidate instanceof PageComponent && same;
		}
		if (dbo instanceof UIDynamicAnimate animate) {
			return candidate instanceof UIElement element && !element.getIdentifier().isEmpty()
					&& Objects.equals(scriptComponent(animate), scriptComponent(element));
		}
		if (dbo instanceof UIDynamicInvoke) {
			return candidate instanceof UIActionStack stack && (same || stack.isExposed());
		}
		if (dbo instanceof UIUseShared) {
			return candidate instanceof UISharedComponent shared && (same || shared.isExposed());
		}
		if (dbo instanceof UIDynamicInfiniteScroll) {
			return candidate instanceof UIDynamicAction action && same
					&& ("CallSequenceAction".equals(beanName(action)) || "FullSyncViewAction".equals(beanName(action)));
		}
		if (dbo instanceof UIDynamicElement element) {
			var bean = beanName(element);
			return switch (property) {
			case "requestable" -> switch (bean) {
			case "CallSequenceAction", "AutoScrollComponent" -> candidate instanceof Sequence;
			case "FullSyncViewAction" -> candidate instanceof DesignDocument;
			case "CallFullSyncAction", "FullSyncSyncAction", "FullSyncPostAction", "FullSyncGetAction",
					"FullSyncDeleteAction", "FullSyncPutAttachmentAction", "FullSyncDeleteAttachmentAction", "FSImage" ->
				candidate instanceof FullSyncConnector;
			default -> false;
			};
			case "fsview" -> "FullSyncViewAction".equals(bean) ? candidate instanceof String
					: "AutoScrollComponent".equals(bean) && (candidate instanceof DesignDocument || candidate instanceof String);
			case "page" -> candidate instanceof PageComponent && same;
			case "event" -> "EmitEventAction".equals(bean) && candidate instanceof UICompEvent event
					&& Objects.equals(event.getSharedComponent(), element.getSharedComponent());
			case "compvar" -> "EmitValueAction".equals(bean) && candidate instanceof UICompVariable variable
					&& Objects.equals(variable.getSharedComponent(), element.getSharedComponent());
			default -> false;
			};
		}
		return false;
	}

	private static String beanName(UIDynamicElement element) {
		return element.getIonBean() == null ? "" : element.getIonBean().getName();
	}

	private static Object scriptComponent(UIComponent component) {
		try {
			return component.getMainScriptComponent();
		} catch (Exception e) {
			return null;
		}
	}
}
