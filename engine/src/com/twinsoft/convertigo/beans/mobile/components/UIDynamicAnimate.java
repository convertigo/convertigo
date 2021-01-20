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

package com.twinsoft.convertigo.beans.mobile.components;

import java.util.HashMap;
import java.util.Map;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.helpers.WalkHelper;

public class UIDynamicAnimate extends UIDynamicAction {

	private static final long serialVersionUID = -9133704806789703685L;

	public enum ApplyMode {
		all,
		single
	}
	
	public UIDynamicAnimate() {
		super();
	}

	public UIDynamicAnimate(String tagName) {
		super(tagName);
	}

	@Override
	public UIDynamicAnimate clone() throws CloneNotSupportedException {
		UIDynamicAnimate cloned = (UIDynamicAnimate) super.clone();
		return cloned;
	}
	
	private String identifiable = "";

	public String getIdentifiable() {
		return identifiable;
	}

	public void setIdentifiable(String identifiable) {
		this.identifiable = identifiable;
	}
	
	private String mode = ApplyMode.single.name();
	
	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	private String getAnimatableId() {
		if (!identifiable.isEmpty()) {
			String p_name = identifiable.substring(0, identifiable.indexOf('.'));
			Project project = this.getProject();
			if (project != null) {
				Project p = null;
				try {
					//p = p_name.equals(project.getName()) ? project: Engine.theApp.databaseObjectsManager.getOriginalProjectByName(p_name);
					p = Engine.theApp.referencedProjectManager.importProjectFrom(project, p_name);
				} catch (Exception e) {
					Engine.logBeans.warn("(UIDynamicAnimate) For \""+  this.toString() +"\", targeted project \""+ p_name +"\" is missing !");
				}
				
				if (p != null) {
					Map<String, DatabaseObject> map = new HashMap<String, DatabaseObject>();
					try {
						new WalkHelper() {
							@Override
							protected void walk(DatabaseObject databaseObject) throws Exception {
								map.put(databaseObject.getQName(), databaseObject);
								super.walk(databaseObject);
							}
							
						}.init(p);
						
						DatabaseObject animatable = map.get(identifiable);
						if (animatable != null && animatable instanceof UIElement) {
							return ((UIElement)animatable).getIdentifier();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		return "";
	}
	
	@Override
	protected StringBuilder initProps(boolean forTemplate) {
		StringBuilder sbProps = super.initProps(forTemplate); //new StringBuilder();
		
		String animatableId = getAnimatableId();
		boolean isEmpty = animatableId.isEmpty();
		
		// animatable property (viewChild identifier)
		sbProps.append(sbProps.length() > 0 ? ", ":"");
		sbProps.append("mode").append(": ");
		sbProps.append("'"+ getMode() +"'");
		
		// animatable property (viewChild identifier)
		sbProps.append(sbProps.length() > 0 ? ", ":"");
		sbProps.append("animatable").append(": ");
		if (forTemplate) {
			sbProps.append(isEmpty ? "null": animatableId);
		} else {
			sbProps.append(isEmpty ? "null": "scope."+ animatableId + " ? scope."+ animatableId + " : get('animatable',`c8oPage."+ animatableId+"`)");
		}
		
		// animatables property (viewChildren identifier)
		sbProps.append(sbProps.length() > 0 ? ", ":"");
		sbProps.append("animatables").append(": ");
		if (forTemplate) {
			sbProps.append(isEmpty ? "null": "all_"+animatableId);
		} else {
			sbProps.append(isEmpty ? "null": "get('animatables', `c8oPage.all_"+animatableId+"`)");
		}
		
		return sbProps;
	}
}
