/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

package com.twinsoft.convertigo.beans.ngx.components;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.common.FormatedContent;
import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.beans.ngx.components.dynamic.IonBean;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

public class UIElement extends UIComponent implements ITagsProperty, IStyleGenerator {
	
	private static final long serialVersionUID = -8671694717057158581L;

	public UIElement() {
		super();
	}

	protected UIElement(String tagName) {
		this();
		this.tagName = tagName;
	}
	
	@Override
	public UIElement clone() throws CloneNotSupportedException {
		UIElement cloned = (UIElement) super.clone();
		return cloned;
	}

	/*
	 * The tagname
	 */
	protected String tagName = "tag";
	
	public String getTagName() {
		return tagName;
	}

	public void setTagName(String tagName) {
		this.tagName = tagName;
	}

	/*
	 * the self-closing state
	 */
	protected boolean selfClose = false;
	
	public boolean isSelfClose() {
		return selfClose;
	}

	public void setSelfClose(boolean selfClose) {
		this.selfClose = selfClose;
	}
	
	protected String identifier = "";
	
	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	protected boolean needNgTemplate() {
		return false;
	}
	
	@Override
	public void addUIComponent(UIComponent uiComponent) throws EngineException {
	    addUIComponent(uiComponent, null);
	}
	
	@Override
	protected void addUIComponent(UIComponent uiComponent, Long after) throws EngineException {
        if (isSelfClose() && !(uiComponent instanceof UIAttribute || uiComponent instanceof UIStyle)) {
            throw new EngineException("You cannot add component to this self-closing tag");
        } else {
        	if (uiComponent instanceof UIForm && uiComponent.bNew) {
            	// Auto set formXXXXXX as identifier
        		if (((UIForm)uiComponent).getIdentifier().isEmpty()) {
        			((UIForm)uiComponent).setIdentifier("form"+uiComponent.priority);
        		}
        	}
        	
        	if (uiComponent instanceof UIDynamicElement && uiComponent.bNew) {
				IonBean ionBean = ((UIDynamicElement)uiComponent).getIonBean();
				if (ionBean != null) {
		        	// Auto set nameXXXXXX as control name
					if (ionBean.hasProperty("ControlName")) {
						if (ionBean.getProperty("ControlName").getSmartType().getSmartValue().isEmpty()) {
							ionBean.setPropertyValue("ControlName", new MobileSmartSourceType("name"+uiComponent.priority));
						}
					}
				}
        	}
        	
        	if (uiComponent instanceof UIStyle && uiComponent.bNew) {
        		if (((UIStyle)uiComponent).getStyleContent().getString().isEmpty()) {
					String scss = getCustomScss();
					if (!scss.isEmpty()) {
						((UIStyle)uiComponent).setStyleContent(new FormatedContent(scss));
					}
        		}
        	}
        	
        	super.addUIComponent(uiComponent, after);
        }
	}

	@Override
	public String toString() {
		String id = getIdentifier();
		String label = getTagName();
		return label.isEmpty() ? super.toString() : label + (id.isEmpty() ? "":" #"+id);
	}

	public String getTagClass() {
		return "class"+priority;
	}
	
	protected String getFormControlName() {
		return "";
	}

	protected String getCustomScss() {
		return "";
	}
	
	protected StringBuilder initAttrClasses() {
		StringBuilder attrclasses = new StringBuilder();
		Iterator<UIComponent> it = getUIComponentList().iterator();
		while (it.hasNext()) {
			UIComponent component = (UIComponent)it.next();
			if (component instanceof UIAttribute) {
				UIAttribute uiAttribute = (UIAttribute)component;
				if (uiAttribute.getAttrName().equals("class")) {
					if (uiAttribute.isEnabled()) {
						attrclasses.append(attrclasses.length()>0 ? " ":"").append(uiAttribute.getAttrValue());
					}
				}
			}
			if (component instanceof UIDynamicAttr) {
				UIDynamicAttr uiDynAttr = (UIDynamicAttr)component;
				if (uiDynAttr.isEnabled()) {
					attrclasses.append(attrclasses.length()>0 ? " ":"").append(uiDynAttr.initAttrClasses());
				}
			}
		}
		return attrclasses;
	}
	
	protected StringBuilder initAttributes() {
		StringBuilder sb = new StringBuilder();
		if (!identifier.isEmpty()) {
			sb.append(" #"+ identifier);
		}
		
		Iterator<UIComponent> it = getUIComponentList().iterator();
		while (it.hasNext()) {
			UIComponent component = (UIComponent)it.next();
			if (component instanceof UIDynamicAttr) {
				UIDynamicAttr uiDynAttr = (UIDynamicAttr)component;
				if (uiDynAttr.isEnabled()) {
					sb.append(sb.length()>0 ? " ":"").append(uiDynAttr.initAttributes());
				}
			}
		}
		return sb;
	}
	
	@Override
	public String computeJsonModel() {
		if (isEnabled()) {
			String formControlVarName = getFormControlName();
			if (!formControlVarName.isEmpty()) {
				return "\"['"+formControlVarName+"']\":{\"dirty\":\"\",\"errors\":\"\",\"valid\":\"\",\"value\":\"\"}";
			} else {
				StringBuilder models = new StringBuilder();
				Iterator<UIComponent> it = getUIComponentList().iterator();
				while (it.hasNext()) {
					UIComponent component = (UIComponent)it.next();
					if (component instanceof UIElement ) {
						String model = ((UIElement)component).computeJsonModel();
						models.append(models.length() > 0 && !model.isEmpty() ? ",":"").append(model);
					}
				}
				return models.toString();
			}
		}
		return "";
	}
	
	protected String computeConstructor() {
		return "";
	}
	
	protected String computeFunction() {
		return "";
	}
	
	@Override
	public void computeScripts(JSONObject jsonScripts) {
		IScriptComponent main = getMainScriptComponent();
		if (main == null) {
			return;
		}
		
		if (!identifier.isEmpty()) {
			try {
				String imports = jsonScripts.getString("imports");
				if (main.addImport("ViewChild", "@angular/core")) {
					imports += "import { ViewChild } from '@angular/core';" + System.lineSeparator();
				}
				if (main.addImport("ViewChildren", "@angular/core")) {
					imports += "import { ViewChildren } from '@angular/core';" + System.lineSeparator();
				}
				if (main.addImport("QueryList", "@angular/core")) {
					imports += "import { QueryList } from '@angular/core';" + System.lineSeparator();
				}
				
				jsonScripts.put("imports", imports);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			try {
				String declarations = jsonScripts.getString("declarations");
				String all_dname = "all_" + identifier;
				String all_dcode = "@ViewChildren(\""+ identifier +"\") public all_"+ identifier+" : QueryList<any>;";
				if (main.addDeclaration(all_dname, all_dcode)) {
					declarations += System.lineSeparator() + "\t" + all_dcode;
				}
				String dname = identifier;
				String dcode = "@ViewChild(\""+ identifier +"\", { static: false }) public "+ identifier+";";
				if (main.addDeclaration(dname, dcode)) {
					declarations += System.lineSeparator() + "\t" + dcode;
				}
				jsonScripts.put("declarations", declarations);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
		super.computeScripts(jsonScripts);
	}

	@Override
	public String computeTemplate() {
		if (isDeprecated()) {
			String deprecationText = getName() + " (tagname: " + getTagName() + ")";
			Engine.logBeans.warn(deprecationText + " is deprecated or does not exists");
			return "<!-- Warn:" + deprecationText + " is deprecated or does not exists -->" + System.getProperty("line.separator");
		}
		
		if (isEnabled()) {
			StringBuilder attributes = initAttributes();
			StringBuilder attrclasses = initAttrClasses();
			StringBuilder children = new StringBuilder();
			
			Iterator<UIComponent> it = getUIComponentList().iterator();
			while (it.hasNext()) {
				UIComponent component = (UIComponent)it.next();
				if (component instanceof UIStyle) {
					;// ignore
				} else if (component instanceof UIAttribute) {
					UIAttribute uiAttribute = (UIAttribute)component;
					if (uiAttribute.getAttrName().equals("class")) {
						;// ignore
					} else {
						attributes.append(component.computeTemplate());
					}
				} else {
					children.append(component.computeTemplate());
				}
			}
			
			String tagClass = getTagClass();
			if (attrclasses.indexOf(tagClass) == -1) {
				attrclasses.append(attrclasses.length()>0 ? " ":"").append(tagClass);
			}
			String attrclass = attrclasses.length()>0 ? " class=\""+ attrclasses +"\"":"";
			
			StringBuilder sb = new StringBuilder();
			sb.append("<").append(getTagName())
				.append(attrclass)
				.append(attributes.length()>0 ? attributes:"");
			
			if (isSelfClose()) {
				sb.append("/>").append(System.getProperty("line.separator"));
			}
			else {
				sb.append(">").append(System.getProperty("line.separator"))
					.append(needNgTemplate() ? "<ng-template>"+ System.getProperty("line.separator"):"")
						.append(children.length()>0 ? children:"")
						.append(children.toString().endsWith(System.getProperty("line.separator")) ? "":System.getProperty("line.separator"))
					.append(needNgTemplate() ? "</ng-template>"+ System.getProperty("line.separator"):"")
				.append("</").append(getTagName())
				.append(">").append(System.getProperty("line.separator"));
			}
			
			return sb.toString();
		}
		return "";
	}

	@Override
	public String computeStyle() {
		StringBuilder fonts = new StringBuilder();
		StringBuilder styles = new StringBuilder();
		StringBuilder others = new StringBuilder();
		
		for (UIComponent component: getUIComponentList()) {
			if (component instanceof UIFont) {
				UIFont font = (UIFont)component;
				String fontImport = font.computeStyle();
				if (!fontImport.isEmpty()) {
					styles.append(fontImport).append(System.getProperty("line.separator"));
				}
				String tpl = font.computeTemplate();
				if (!tpl.isEmpty()) {
					String fontFamily = font.getFontSource().getFontFamily();
					if (fonts.indexOf(fontFamily) == -1) {
						boolean first = fonts.length() == 0;
						fonts.append(first ? "": ", ").append("\""+ fontFamily +"\"");
						if (first) {
							styles.append(tpl).append(";").append(System.getProperty("line.separator"));
						}
					}
				}
			}
			else if (component instanceof UIStyle) {
				String tpl = component.computeTemplate();
				if (!tpl.isEmpty()) {
					styles.append(tpl).append(";").append(System.getProperty("line.separator"));
				}
			} else if (component instanceof UIUseShared) {
				String tpl = ((UIUseShared)component).computeStyle();
				if (!tpl.isEmpty()) {
					others.append(tpl);
				}
			} else if (component instanceof UIElement) {
				String tpl = ((UIElement)component).computeStyle();
				if (!tpl.isEmpty()) {
					others.append(tpl);
				}
			}
		}
		
		StringBuilder sb = new StringBuilder();
		if (others.length() > 0) {
			sb.append(others).append(System.getProperty("line.separator"));
		}
		if (styles.length() > 0 || fonts.length() > 0) {
			sb.append("."+ getTagClass()).append(" {").append(System.getProperty("line.separator"));
			if (fonts.length() > 0) {
				sb.append("\tfont-family: ").append(fonts).append(";").append(System.getProperty("line.separator"));
			}
			sb.append(styles);
			sb.append("}").append(System.getProperty("line.separator"));
		}
		return cleanStyle(sb.toString());
	}

	@Override
	public String[] getTagsForProperty(String propertyName) {
		return new String[0];
	}

	@Override
	protected Contributor getContributor() {
		final boolean tplIsStandalone = UIElement.this.isTplStandalone();
		
		return new Contributor() {

			private boolean usesRouterLink() {
				if ("a".equalsIgnoreCase(UIElement.this.getTagName())) {
					for (UIComponent uic: getUIComponentList()) {
						if (uic instanceof UIAttribute) {
							String attrname = ((UIAttribute)uic).getAttrName();
							if ("[routerLink]".equalsIgnoreCase(attrname) ||
								"routerLink".equalsIgnoreCase(attrname)) {
								return true;
							}
						}
					}
				}
				return false;
			}
			
			@Override
			public Map<String, File> getCompBeanDir() {
				return new HashMap<String, File>();
			}

			@Override
			public Map<String, String> getActionTsFunctions() {
				return new HashMap<String, String>();
			}

			@Override
			public Map<String, String> getActionTsImports() {
				return new HashMap<String, String>();
			}

			@Override
			public Map<String, String> getModuleTsImports() {
				Map<String, String> imports = new HashMap<String, String>();
				if (tplIsStandalone) {
					if (usesRouterLink()) {
						imports.put("{ RouterLink }", "@angular/router");
						imports.put("{ IonRouterLinkWithHref }", "@ionic/angular/standalone");
					}
				}
				return imports;
			}

			@Override
			public Map<String, String> getPackageDependencies() {
				return new HashMap<String, String>();
			}

			@Override
			public Map<String, String> getConfigPlugins() {
				return new HashMap<String, String>();
			}

			@Override
			public Set<String> getBuildAssets() {
				return new HashSet<String>();
			}

			@Override
			public Set<String> getBuildScripts() {
				return new HashSet<String>();
			}

			@Override
			public Set<String> getBuildStyles() {
				return new HashSet<String>();
			}

			@Override
			public Set<String> getModuleNgImports() {
				Set<String> set = new HashSet<String>();
				if (tplIsStandalone) {
					if (usesRouterLink()) {
						set.add("RouterLink");
						set.add("IonRouterLinkWithHref ");
					}
				}
				return set;
			}

			@Override
			public Set<String> getModuleNgProviders() {
				return new HashSet<String>();
			}

			@Override
			public Set<String> getModuleNgDeclarations() {
				return new HashSet<String>();
			}

			@Override
			public Set<String> getModuleNgComponents() {
				return new HashSet<String>();
			}

			@Override
			public Set<String> getModuleNgRoutes(String pageSegment) {
				return new HashSet<String>();
			}
		};
	}
	
	
}
