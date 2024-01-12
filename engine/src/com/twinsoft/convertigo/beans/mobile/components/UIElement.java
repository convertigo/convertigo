/*
 * Copyright (c) 2001-2024 Convertigo SA.
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

import java.util.Iterator;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.beans.mobile.components.dynamic.IonBean;
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
	private boolean selfClose = false;
	
	public boolean isSelfClose() {
		return selfClose;
	}

	public void setSelfClose(boolean selfClose) {
		this.selfClose = selfClose;
	}
	
	private String identifier = "";
	
	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	@Override
	public void addUIComponent(UIComponent uiComponent) throws EngineException {
	    addUIComponent(uiComponent, null);
	}
	
	@Override
	protected void addUIComponent(UIComponent uiComponent, Long after) throws EngineException {
        if (isSelfClose() && !(uiComponent instanceof UIAttribute 
        						|| uiComponent instanceof UIStyle
        							|| uiComponent instanceof UIFormValidator)) {
            throw new EngineException("You cannot add component to this self-closing tag");
        } else if (uiComponent instanceof UIFormValidator) {
        	if (this instanceof UIForm) {
        		if (!(uiComponent instanceof UIFormCustomValidator)) {
        			throw new EngineException("You can only add a custom validator to this component");
        		} else {
	    			super.addUIComponent(uiComponent, after);
        		}
        	} else {
	    		String formControlName = getFormControlName();
	    		if (formControlName.isEmpty()) {
	    			Engine.logBeans.warn("Validator is missing \"formControlName\" property or attribute for component "+ this.getQName());
	    		}
	    		super.addUIComponent(uiComponent, after);
        	}
        } else {
        	if (uiComponent instanceof UIDynamicElement && uiComponent.bNew) {
        		if (getUIForm() != null) {
					IonBean ionBean = ((UIDynamicElement)uiComponent).getIonBean();
					if (ionBean != null && ionBean.hasProperty("FormControlName")) {
						ionBean.setPropertyValue("FormControlName", new MobileSmartSourceType("var"+uiComponent.priority));
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
		Iterator<UIComponent> it = getUIComponentList().iterator();
		while (it.hasNext()) {
			UIComponent component = (UIComponent)it.next();
			if (component instanceof UIAttribute) {
				UIAttribute attribute = (UIAttribute)component;
				if (attribute.getAttrName().equals("formControlName")) {
					if (attribute.isEnabled()) {
						return attribute.getAttrValue();
					}
				}
			}
		}
		return "";
	}

	protected StringBuilder initAttributes() {
		StringBuilder sb = new StringBuilder();
		if (!identifier.isEmpty()) {
			sb.append(" #"+ identifier);
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
		if (isEnabled()) {
			StringBuilder sb = new StringBuilder();
			
			String formControlVarName = getFormControlName();
			if (!formControlVarName.isEmpty()) {
				StringBuilder constructors = new StringBuilder();
				StringBuilder asyncConstructors = new StringBuilder();
				
				Iterator<UIComponent> it = getUIComponentList().iterator();
				while (it.hasNext()) {
					UIComponent component = (UIComponent)it.next();
					if (component instanceof UIFormValidator) {
						UIFormValidator validator = (UIFormValidator)component;
						String constructor = validator.computeConstructor();
						if (validator.isAsync()) {
							asyncConstructors.append(asyncConstructors.length() > 0 && !constructor.isEmpty() ? ",":"").append(constructor);
						} else {
							constructors.append(constructors.length() > 0 && !constructor.isEmpty() ? ",":"").append(constructor);
						}
					}
				}
				sb.append(System.lineSeparator());
				sb.append("\t\t\t"+formControlVarName + " : new FormControl('',")
					.append(" Validators.compose([").append(constructors).append("]),")
					.append(" Validators.composeAsync([").append(asyncConstructors).append("])")
					.append(")").append(",");
			}
			else {
				StringBuilder constructors = new StringBuilder();
				Iterator<UIComponent> it = getUIComponentList().iterator();
				while (it.hasNext()) {
					UIComponent component = (UIComponent)it.next();
					if (component instanceof UIElement ) {
						String constructor = ((UIElement)component).computeConstructor();
						constructors.append(constructor);
					}
				}
				sb.append(constructors);
			}
			return sb.toString().replaceAll(",+", ",");
		}
		return "";
	}
	
	protected String computeFunction() {
		if (isEnabled()) {
			StringBuilder sb = new StringBuilder();
			String formControlVarName = getFormControlName();
			if (!formControlVarName.isEmpty()) {
				Iterator<UIComponent> it = getUIComponentList().iterator();
				while (it.hasNext()) {
					UIComponent component = (UIComponent)it.next();
					if (component instanceof UIFormValidator) {
						sb.append(((UIFormValidator)component).computeFunction());
					}
				}
			}
			else {
				Iterator<UIComponent> it = getUIComponentList().iterator();
				while (it.hasNext()) {
					UIComponent component = (UIComponent)it.next();
					if (component instanceof UIElement ) {
						sb.append(((UIElement)component).computeFunction());
					}
				}
			}
			return sb.toString();
		}
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
				if (main.addImport("ViewChild", "@angular/forms")) {
					imports += "import { ViewChild } from '@angular/core';" + System.lineSeparator();
				}
				if (main.addImport("ViewChildren", "@angular/forms")) {
					imports += "import { ViewChildren } from '@angular/core';" + System.lineSeparator();
				}
				if (main.addImport("QueryList", "@angular/forms")) {
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
				String dcode = "@ViewChild(\""+ identifier +"\") public "+ identifier+";";
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
		if (isEnabled()) {
			StringBuilder attributes = initAttributes();
			StringBuilder attrclasses = new StringBuilder();
			StringBuilder children = new StringBuilder();
			
			Iterator<UIComponent> it = getUIComponentList().iterator();
			while (it.hasNext()) {
				UIComponent component = (UIComponent)it.next();
				if (component instanceof UIFormControlValidator) {
					;// ignore
				} else if (component instanceof UIStyle) {
					;// ignore
				} else if (component instanceof UIAttribute) {
					UIAttribute uiAttribute = (UIAttribute)component;
					if (uiAttribute.getAttrName().equals("class")) {
						if (uiAttribute.isEnabled()) {
							attrclasses.append(attrclasses.length()>0 ? " ":"").append(uiAttribute.getAttrValue());
						}
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
					.append(children.length()>0 ? children:"")
				  .append("</").append(getTagName())
				  	.append(">").append(System.getProperty("line.separator"));
			}
			
			return sb.toString();
		}
		return "";
	}

	@Override
	public String computeStyle() {
		StringBuilder styles = new StringBuilder();
		StringBuilder others = new StringBuilder();
		
		Iterator<UIComponent> it = getUIComponentList().iterator();
		while (it.hasNext()) {
			UIComponent component = (UIComponent)it.next();
			if (component instanceof UIStyle) {
				String tpl = component.computeTemplate();
				if (!tpl.isEmpty()) {
					styles.append(tpl).append(";").append(System.getProperty("line.separator"));
				}
			}
			else if (component instanceof UIElement) {
				String tpl = ((UIElement)component).computeStyle();
				if (!tpl.isEmpty()) {
					others.append(tpl);
				}
			}
		}
		
		StringBuilder sb = new StringBuilder();
		if (styles.length()>0) {
			sb.append("."+ getTagClass()).append(" {").append(System.getProperty("line.separator"));
			sb.append(styles);
			sb.append("}").append(System.getProperty("line.separator"));
		}
		if (others.length()>0) {
			sb.append(others);
		}
		return sb.toString();
	}

	@Override
	public String[] getTagsForProperty(String propertyName) {
		return new String[0];
	}

}
