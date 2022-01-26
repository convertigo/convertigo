/*
 * Copyright (c) 2001-2022 Convertigo SA.
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

package com.twinsoft.convertigo.engine.admin.services.studio.database_objects;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.BlockFactory;
import com.twinsoft.convertigo.beans.core.Criteria;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.ExtractionRule;
import com.twinsoft.convertigo.beans.core.IEnableAble;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.core.Sheet;
import com.twinsoft.convertigo.beans.screenclasses.JavelinScreenClass;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.util.CachedIntrospector;

@ServiceDefinition(
		name = "Get",
		roles = { Role.WEB_ADMIN },
		parameters = {},
		returnValue = ""
	)
public class GetChildren extends XmlService {

	@Override
	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
        String qname = request.getParameter("qname");
        Element root = document.getDocumentElement();

        // Classic database objects
        if (qname != null) {
            getChildren(qname, root, 1);
        }
        // Project
        else {
            for (String qn: Engine.theApp.databaseObjectsManager.getAllProjectNamesList()) {
                getChildren(qn, root, 0);
            }
        }
	}

	public static void getChildren(String qname, Element parent, int depth) throws Exception   {
		DatabaseObject dbo = Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(qname);
		List<DatabaseObject> children = dbo.getDatabaseObjectChildren();

		// Get all children of the dbo
		Element elt = createDboElement(parent.getOwnerDocument(), dbo, !children.isEmpty());

		/*
		 *  In case of ScreenClass, we have to get Criteria, ExtractionRule and Sheets manually.
		 *  If fact, if the dbo is an inherited screen class, inherited Criteria, ExtractionRule and Sheets,
		 *  won't be retrieved by the method #getDatabaseObjectChildren.
		 */
		if (dbo instanceof ScreenClass) {
			ScreenClass sc = (ScreenClass) dbo;
			boolean hasChildren = false;

			// Get all Criteria
			List<Criteria> criteria = sc.getCriterias();
			for (Criteria criterion : criteria) {
				children.remove(criterion);
				Element eltCriterion = createScreenClassChildElement(parent.getOwnerDocument(), criterion, dbo);
				elt.appendChild(eltCriterion);
				hasChildren = true;
			}

			// Get all Extraction Rules
			List<ExtractionRule> extractionRules = sc.getExtractionRules();
			for (ExtractionRule extractionRule : extractionRules) {
				children.remove(extractionRule);
				Element eltExtractionRule = createScreenClassChildElement(parent.getOwnerDocument(), extractionRule, dbo);
				elt.appendChild(eltExtractionRule);
				hasChildren = true;
			}

			// Get all Sheets
			List<Sheet> sheets = sc.getSheets();
			for (Sheet sheet : sheets) {
				children.remove(sheet);
				Element eltSheet = createScreenClassChildElement(parent.getOwnerDocument(), sheet, dbo);
				elt.appendChild(eltSheet);
				hasChildren = true;
			}

			// In case of JavelinScreenClass, we also have to get the block factory manually
			if (dbo instanceof JavelinScreenClass) {
				JavelinScreenClass jsc = (JavelinScreenClass) sc;
				BlockFactory blockFactory = jsc.getBlockFactory();
				children.remove(blockFactory);
				Element eltBlockFactory = createScreenClassChildElement(parent.getOwnerDocument(), blockFactory, dbo);
				elt.appendChild(eltBlockFactory);
				hasChildren = true;
			}

			if (hasChildren) {
				elt.setAttribute("hasChildren", "true");
			}
		}

		parent.appendChild(elt);
		if (depth > 0) {
			for (DatabaseObject child: children) {
				getChildren(child.getQName(), elt, depth - 1);
			}
		}
	}

	private static Element createDboElement(Document document, DatabaseObject dbo, boolean hasChildren) throws DOMException, IntrospectionException {
		Element elt = document.createElement("dbo");

		elt.setAttribute("qname", dbo.getQName());
		elt.setAttribute("icon", dbo.getClass().getName() + "-16");
		elt.setAttribute("name", dbo.toString());
		elt.setAttribute("category", dbo.getDatabaseType());
		elt.setAttribute("comment", dbo.getComment());
		elt.setAttribute("hasChildren", Boolean.toString(hasChildren));
		elt.setAttribute("priority", Long.toString(dbo.priority));

		BeanInfo bi = CachedIntrospector.getBeanInfo(dbo);
		elt.setAttribute("beanClass", bi.getBeanDescriptor().getBeanClass().getName());

		if (dbo instanceof IEnableAble) {
			elt.setAttribute("isEnabled", Boolean.toString(((IEnableAble) dbo).isEnabled()));
		}
		return elt;
	}

	private static Element createScreenClassChildElement(Document document, DatabaseObject dbo, DatabaseObject dboParent) throws DOMException, Exception {
		Element elt = createDboElement(document, dbo, !dbo.getDatabaseObjectChildren().isEmpty());
		elt.setAttributeNode(createIsInheritedAttr(document, dbo, dboParent));
		return elt;
	}

	private static Attr createIsInheritedAttr(Document document, DatabaseObject dbo, DatabaseObject dboParent) {
		Attr attr = document.createAttribute("isInherited");
		attr.setNodeValue(Boolean.toString(!dboParent.toString().equals(dbo.getParent().toString())));
		return attr;
	}
}
