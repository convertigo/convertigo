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
import java.util.Comparator;
import java.util.Iterator;

import javax.xml.namespace.QName;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.studio.Utils;
import com.twinsoft.convertigo.engine.enums.SchemaMeta;
import com.twinsoft.convertigo.engine.util.GenericUtils;

/**
 * The types or the elements of the schemas of a project a qualified name property can use, as the QName
 * editor of the Eclipse Studio shows them.
 * <ul>
 * <li>id: the object</li>
 * <li>property: its property</li>
 * </ul>
 */
@ServiceDefinition(name = "QNames", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG }, parameters = {}, returnValue = "")
public class QNames extends JSonService {
	private static final String XSD_NAMESPACE = "http://www.w3.org/2001/XMLSchema";

	/**
	 * @return what the property names: complexType, simpleType, type (both) or element
	 */
	static String kind(String property) {
		return switch (property) {
		case "xmlComplexTypeAffectation" -> "complexType";
		case "xmlSimpleTypeAffectation" -> "simpleType";
		case "xmlElementRefAffectation" -> "element";
		default -> "type";
		};
	}

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var dbo = Utils.getDbo(request.getParameter("id"));
		if (dbo == null || dbo.getProject() == null) {
			throw new ServiceException("The object " + request.getParameter("id") + " does not exist.");
		}
		var kind = kind(String.valueOf(request.getParameter("property")));
		var project = dbo.getProject();
		var collection = Engine.theApp.schemaManager.getSchemasForProject(project.getName());
		var objects = new ArrayList<XmlSchemaObject>();
		for (var schema : collection.getXmlSchemas()) {
			if (XSD_NAMESPACE.equals(schema.getTargetNamespace()) && !"simpleType".equals(kind)) {
				continue;
			}
			if ("element".equals(kind)) {
				for (Iterator<XmlSchemaElement> i = GenericUtils.cast(schema.getElements().getValues()); i.hasNext();) {
					objects.add(i.next());
				}
			} else {
				for (Iterator<XmlSchemaType> i = GenericUtils.cast(schema.getSchemaTypes().getValues()); i.hasNext();) {
					var type = i.next();
					if ("type".equals(kind) || ("complexType".equals(kind) ? type instanceof XmlSchemaComplexType
							: type instanceof XmlSchemaSimpleType)) {
						objects.add(type);
					}
				}
			}
		}
		var items = new JSONArray();
		objects.stream()
				.filter(object -> qnameOf(object) != null)
				.sorted(Comparator.comparing((XmlSchemaObject object) -> qnameOf(object).getNamespaceURI())
						.thenComparing(object -> qnameOf(object).getLocalPart()))
				.forEach(object -> {
					var qname = qnameOf(object);
					try {
						items.put(new JSONObject()
								.put("qname", qname.toString())
								.put("namespace", qname.getNamespaceURI())
								.put("name", qname.getLocalPart())
								.put("kind", object instanceof XmlSchemaElement ? "element"
										: object instanceof XmlSchemaComplexType ? "complexType" : "simpleType")
								.put("dynamic", SchemaMeta.isDynamic(object))
								.put("readOnly", SchemaMeta.isReadOnly(object)));
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				});
		response.put("kind", kind);
		response.put("namespace", project.getTargetNamespace());
		response.put("items", items);
	}

	private static QName qnameOf(XmlSchemaObject object) {
		return object instanceof XmlSchemaElement element ? element.getQName()
				: object instanceof XmlSchemaType type ? type.getQName() : null;
	}
}
