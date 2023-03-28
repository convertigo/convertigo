/*
 * Copyright (c) 2001-2023 Convertigo SA.
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

package com.twinsoft.convertigo.engine.util;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.twinsoft.convertigo.beans.BeansDefaultValues;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;

class CustomDirectoryFilter implements FileFilter {
	@Override
	public boolean accept(File file) {
	    if (file.isDirectory()) {
	    	if (file.getName().equals("_private") || file.getName().equals("build")) {
	    		return false;
	    	}
	    }
	    return true; 
	}
}

public class NgxConverter {
	private static final Pattern patternBeanName = Pattern.compile("(.*) \\[(.*?)(?:-(.*))?\\]");
	static TwsCachedXPathAPI xpath = TwsCachedXPathAPI.getInstance();
	
	File outputDir;
	String tplScss;
	String indent = "";

	public NgxConverter(File outputDir) {
		this.outputDir = outputDir;
		this.tplScss = getThemeTplScss();
	}

	private String getThemeTplScss() {
		File appThemeTpl = new File(outputDir, "../mobilebuilder_tpl_8_0_0_ngx/ionicTpl/src/theme/variables.scss");
		try {
			return FileUtils.readFileToString(appThemeTpl, "UTF-8");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}
	
	private static String getBeanQName(Element beanEl) {
		String beanName = "";
		try {
			String yaml_key = beanEl.getAttribute("yaml_key");
			if (!yaml_key.isBlank()) {
				beanName = yaml_key.substring(0, yaml_key.indexOf('[')).trim();
				String qname = getBeanQName((Element) beanEl.getParentNode());
				return qname + (qname.isBlank() ? "":".") + beanName;
			}
		} catch (Exception e) {}
		return beanName;
	}
	
	private static Element getBeanDataElement(Element beanEl) {
		try {
			return (Element) xpath.selectList(beanEl, "beanData").get(0);
		} catch (Exception e) {
			return null;
		}
	}
	
	private static JSONObject getJsonBean(Element beanEl) {
		try {
			return new JSONObject(getBeanDataElement(beanEl).getTextContent());
		} catch (Exception e) {}
		return null;
	}
	
	private static String getIonBeanName(Element beanEl) {
		try {
			JSONObject jsonBean = new JSONObject(getBeanDataElement(beanEl).getTextContent());
			return jsonBean.getString("ionBean");
		} catch (Exception e) {}
		return "";
	}
	
	private static String getTagName(Element beanEl) {
		try {
			return xpath.selectList(beanEl, "tagName").get(0).getTextContent();
		} catch (Exception e) {}
		return "";
	}
	
	private static void setTagName(Element beanEl, String textContent) {
		try {
			xpath.selectList(beanEl, "tagName").get(0).setTextContent(textContent);
		} catch (Exception e) {}
	}
	
	private static void setBeanData(Element beanEl, String textContent) {
		try {
			xpath.selectList(beanEl, "beanData").get(0).setTextContent(textContent);
		} catch (Exception e) {}
	}
	
	private static void cleanBeanData(Element beanEl) {
		JSONObject jsonBean = getJsonBean(beanEl);
		try {
			JSONObject json = new JSONObject().put("ionBean", jsonBean.getString("ionBean"));
			setBeanData(beanEl, json.toString());
		} catch (Exception e) {}
	}

	private static void capitalizeBeanData(Element beanEl) {
		JSONObject jsonBean = getJsonBean(beanEl);
		try {
			JSONObject json = new JSONObject().put("ionBean", jsonBean.getString("ionBean"));
			@SuppressWarnings("unchecked")
			Iterator<String> it = jsonBean.keys();
			while (it.hasNext()) {
				String key = it.next();
				if (!key.equals("ionBean")) {
					String nkey = key.substring(0, 1).toUpperCase() + key.substring(1);
					json.put(nkey, jsonBean.get(key));
				}
			}
			setBeanData(beanEl, json.toString());
		} catch (Exception e) {}
	}

	private static boolean checkBean(Element beanEl) {
		String yaml_key = beanEl.getAttribute("yaml_key");
		
		try {
			// check for class
			Matcher matcherBeanName = patternBeanName.matcher(yaml_key);
			matcherBeanName.matches();
			String classname = "com.twinsoft.convertigo.beans." + matcherBeanName.group(2);
			Class.forName(classname);
			return true;
		} catch (Exception e) {
			System.out.println(yaml_key.replaceAll("ngx\\.components", "mobile.components") + " : ERROR");
			System.err.println("Unhandled bean : "+ yaml_key.replaceAll("ngx\\.components", "mobile.components"));
			System.err.println(XMLUtils.prettyPrintElement(beanEl).replaceAll("ngx\\.components", "mobile.components"));
			beanEl.getParentNode().removeChild(beanEl);
		}
		return false;
	}
	
	private static boolean isPseudoBean(Element beanEl) {
		return !getIonBeanName(beanEl).isEmpty();
	}
	
	private static boolean checkPseudoBean(Element beanEl) {
		String yaml_key = beanEl.getAttribute("yaml_key");
		try {
			if (isPseudoBean(beanEl)) {
				String ionBeanName = getIonBeanName(beanEl);
				if (com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.getComponentByName(ionBeanName) == null) {
					System.out.println(yaml_key.replaceAll("ngx\\.components", "mobile.components") + " : ERROR");
					if (ionBeanName.endsWith("Action")) {
						System.err.println("Unhandled pseudo-action (replaced by a ToastAction) : "+ yaml_key.replaceAll("ngx\\.components", "mobile.components"));
						System.err.println(XMLUtils.prettyPrintElement(beanEl).replaceAll("ngx\\.components", "mobile.components"));
						
						// replace action by a toast action
						Matcher matcherBeanName = patternBeanName.matcher(yaml_key);
						matcherBeanName.matches();
						String classpath = matcherBeanName.group(2);
						beanEl.getAttributeNode("yaml_key").setTextContent(yaml_key.replace(classpath, "ngx.components.UIDynamicAction"));
						setBeanData(beanEl, "{\"ionBean\":\"ToastAction\",\"message\":\"plain:Unhandled "+ ionBeanName +"\"}");
					} else {
						// unhandled pseudo-bean
						throw new Exception("Unhandled pseudo-bean");
					}
				}
			}
			return true;
		} catch (Exception e) {
			System.err.println("Unhandled pseudo-bean : "+ yaml_key.replaceAll("ngx\\.components", "mobile.components"));
			System.err.println(XMLUtils.prettyPrintElement(beanEl).replaceAll("ngx\\.components", "mobile.components"));
			beanEl.getParentNode().removeChild(beanEl);
		}
		return false;
	}
	
	private void handleBean(Element beanEl) {
		String yaml_key = beanEl.getAttribute("yaml_key");
		
		/* Standard Beans */
		// for application
		if (yaml_key.indexOf("ngx.components.ApplicationComponent") != -1) {
			try {
				xpath.selectList(beanEl, "tplProjectName").get(0).setTextContent("mobilebuilder_tpl_8_0_0_ngx");
			} catch (Exception e) {}
		}
		
		// for shared component
		if (yaml_key.indexOf("ngx.components.UISharedComponent") != -1) {
			beanEl.getAttributeNode("yaml_key").setTextContent(yaml_key.replaceFirst("UISharedComponent", "UISharedRegularComponent"));
		}

		// for useshared variable
		if (yaml_key.indexOf("ngx.components.UIControlVariable") != -1) {
			Element parentEl = (Element) beanEl.getParentNode();
			if (parentEl.getAttribute("yaml_key").indexOf("ngx.components.UIUseShared") != -1) {
				beanEl.getAttributeNode("yaml_key").setTextContent(yaml_key.replaceFirst("UIControlVariable", "UIUseVariable"));
			}
		}
		
		// for application theme
		if (yaml_key.indexOf("ngx.components.UITheme") != -1) {
			try {
				Node formatedContent = xpath.selectList(beanEl, "//com.twinsoft.convertigo.beans.common.FormatedContent").get(0);
				CDATASection cdata = (CDATASection)formatedContent.getFirstChild();
				cdata.setTextContent(this.tplScss);
				//System.out.println("CDATASection:\n"+ cdata.getTextContent());
			} catch (Exception e) {}
		}
		
		// for style
		if (yaml_key.indexOf("ngx.components.UIStyle") != -1) {
			try {
				Node formatedContent = xpath.selectList(beanEl, "//com.twinsoft.convertigo.beans.common.FormatedContent").get(0);
				CDATASection cdata = (CDATASection)formatedContent.getFirstChild();
				String text = cdata.getTextContent();
				Pattern pattern = Pattern.compile("color\\(\\$colors\\,(.*)\\)");
				Matcher matcher = pattern.matcher(text);
				while (matcher.find()) {
					String match = matcher.group(0);
					String color = matcher.group(1);
					if (color != null) {
						text = text.replace(match, "var(--ion-color-"+ color.trim() +")");
					}
				}
				cdata.setTextContent(text);
				//System.out.println("UISTyle:\n"+ text);
			} catch (Exception e) {}
		}
		
		// for form
		if (yaml_key.indexOf("ngx.components.UIForm") != -1) {
			try {
				int index = yaml_key.indexOf("ngx.components.UIForm-");
				String priority = yaml_key.substring(index+"ngx.components.UIForm-".length(), yaml_key.indexOf("]"));
				Element identifier = beanEl.getOwnerDocument().createElement("identifier");
				identifier.appendChild(beanEl.getOwnerDocument().createTextNode("form"+ priority));
				beanEl.insertBefore(identifier, beanEl.getFirstChild());
			} catch (Exception e) {}
		}
				
		/* Pseudo-Beans */
		if (isPseudoBean(beanEl)) {
			try {
				JSONObject jsonBean = getJsonBean(beanEl);
				
				// for forms controls
				if (jsonBean.has("FormControlName")) {
					String fcn = jsonBean.getString("FormControlName");
					jsonBean.put("ControlName", fcn);
					jsonBean.remove("FormControlName");
					
					jsonBean.remove("TextAlign"); // for input
				}
				
				// for avatar, badge, icon, formitem, listitem, genericitem
				if (jsonBean.has("ItemPosition")) {
					String value = jsonBean.getString("ItemPosition");
					if (value.equals("plain:item-start") || value.equals("plain:item-left")) {
						jsonBean.put("IonSlot", "plain:start");
					}
					if (value.equals("plain:item-end") || value.equals("plain:item-right")) {
						jsonBean.put("IonSlot", "plain:end");
					}
					jsonBean.remove("ItemPosition");
				}
				
				// for content
				if (jsonBean.has("IonPadding")) {
					jsonBean.remove("IonPadding");
				}
				
				// for menu
				if (jsonBean.has("MenuSide")) {
					String value = jsonBean.getString("MenuSide");
					if (value.equals("plain:left")) {
						jsonBean.put("MenuSide", "plain:start");
					}
					if (value.equals("plain:right")) {
						jsonBean.put("MenuSide", "plain:end");
					}
					jsonBean.remove("MenuPersistent");
				}
				
				setBeanData(beanEl, jsonBean.toString());
			} catch (Exception e) {}
		}
	}
	
	private static void handleNavBar(Element beanEl) {
		String yaml_key = beanEl.getAttribute("yaml_key");
		beanEl.getAttributeNode("yaml_key").setTextContent(yaml_key.replaceFirst("NavBar", "ToolBar"));
		
		JSONObject jsonBean = getJsonBean(beanEl);
		try {
			jsonBean.put("ionBean", "ToolBar");
			setBeanData(beanEl, jsonBean.toString());
		} catch (Exception e) {}
		
		setTagName(beanEl, "ion-toolbar");
	}
	
	private static void handleButton(Element beanEl) {
		JSONObject jsonBean = getJsonBean(beanEl);
		
		// standard buttons
		boolean isMenuButton = false;
		try {
			isMenuButton = jsonBean.has("MenuMode") ? jsonBean.get("MenuMode").equals("plain:menuToggle") : false;
			if (isMenuButton) {
				jsonBean.put("ionBean", "MenuButton");
				jsonBean.remove("MenuMode");
			}
			setBeanData(beanEl, jsonBean.toString());
		} catch (Exception e) {}
		setTagName(beanEl, isMenuButton ? "ion-menu-button" : "ion-button");
		
		// sliding button
		Element parentEl = (Element) beanEl.getParentNode();
		if (parentEl != null && "ion-item-options".equals(getTagName(parentEl))) {
			try {
				jsonBean.put("ionBean", "SlidingButton");
				setBeanData(beanEl, jsonBean.toString());
			} catch (Exception e) {}
			setTagName(beanEl, "ion-item-option");
		}
		
	}
	
	private static void handleMenuItems(Element beanEl) {
		setTagName(beanEl, "ion-menu-toggle");
	}
	
	private static void handleFabButton(Element beanEl) {
		JSONObject jsonBean = getJsonBean(beanEl);
		try {
			jsonBean.remove("ButtonType");
			if (jsonBean.has("Size")) {
				String size = jsonBean.getString("Size");
				if (size.equals("plain:mini")) {
					jsonBean.put("Size", "plain:small");
				}
			}
			setBeanData(beanEl, jsonBean.toString());
		} catch (Exception e) {}
		
		setTagName(beanEl, "ion-fab-button");
	}

	private static void handleButtonSet(Element beanEl) {
		JSONObject jsonBean = getJsonBean(beanEl);
		try {
			if (jsonBean.has("SetPosition")) {
				String position = jsonBean.getString("SetPosition");
				if (position.equals("plain:left")) {
					jsonBean.put("SetPosition", "plain:start");
				}
				if (position.equals("plain:right")) {
					jsonBean.put("SetPosition", "plain:end");
				}
			}
			setBeanData(beanEl, jsonBean.toString());
		} catch (Exception e) {}
	}
	
	private static void handleIcon(Element beanEl) {
		JSONObject jsonBean = getJsonBean(beanEl);
		try {
			if (jsonBean.has("IconName")) {
				String name = jsonBean.getString("IconName");
				if (name.equals("plain:more")) {
					jsonBean.put("IconName", "plain:ellipsis-vertical-outline");
				}
			}
			jsonBean.remove("IonColor");
			jsonBean.remove("RangeLeft");
			jsonBean.remove("RangeRight");
			setBeanData(beanEl, jsonBean.toString());
			
			Element parentEl = (Element) beanEl.getParentNode();
			if (parentEl != null && "ion-menu-button".equals(getTagName(parentEl))) {
				parentEl.removeChild(beanEl);
			}
		} catch (Exception e) {}
	}
	
	private static void handleList(Element beanEl) {
		JSONObject jsonBean = getJsonBean(beanEl);
		try {
			if (jsonBean.has("Nolines")) {
				String nolines = jsonBean.getString("Nolines");
				if (nolines.equals("plain:no-lines")) {
					jsonBean.put("Lines", "plain:none");
				}
				jsonBean.remove("Nolines");
			}
			if (jsonBean.has("Inset")) {
				String inset = jsonBean.getString("Inset");
				if (inset.equals("plain:inset")) {
					jsonBean.put("Inset", "plain:true");
				} else {
					jsonBean.put("Inset", "plain:false");
				}
			}
			setBeanData(beanEl, jsonBean.toString());
		} catch (Exception e) {}
	}
	
	private static void handleDateTime(Element beanEl) {
		capitalizeBeanData(beanEl);
	}

	private static void handleFabContainer(Element beanEl) {
		JSONObject jsonBean = getJsonBean(beanEl);
		try {
			if (jsonBean.has("HorzPosition")) {
				String horizontal = jsonBean.getString("HorzPosition");
				if (horizontal.equals("plain:left")) {
					jsonBean.put("Horizontal", "plain:start");
				} else if (horizontal.equals("plain:center")) {
					jsonBean.put("Horizontal", "plain:center");
				} else if (horizontal.equals("plain:right")) {
					jsonBean.put("Horizontal", "plain:end");
				}
				jsonBean.remove("HorzPosition");
			}
			if (jsonBean.has("VertPosition")) {
				String vertical = jsonBean.getString("VertPosition");
				if (vertical.equals("plain:middle")) {
					jsonBean.put("Vertical", "plain:center");
				} else if (vertical.equals("plain:bottom")) {
					jsonBean.put("Vertical", "plain:bottom");
				} else if (vertical.equals("plain:top")) {
					jsonBean.put("Vertical", "plain:top");
				}
				jsonBean.remove("VertPosition");
			}
			if (jsonBean.has("Edge")) {
				String edge = jsonBean.getString("Inset");
				if (edge.equals("plain:edge")) {
					jsonBean.put("Inset", "plain:true");
				} else {
					jsonBean.put("Inset", "plain:false");
				}
			}
			setBeanData(beanEl, jsonBean.toString());
		} catch (Exception e) {}
	}

	private static void handleFabList(Element beanEl) {
		JSONObject jsonBean = getJsonBean(beanEl);
		try {
			if (jsonBean.has("side")) {
				String side = jsonBean.getString("side");
				if (side.equals("plain:left")) {
					jsonBean.put("Side", "plain:start");
				} else if (side.equals("plain:top")) {
					jsonBean.put("Side", "plain:top");
				} else if (side.equals("plain:right")) {
					jsonBean.put("Side", "plain:end");
				}
				jsonBean.remove("side");
			}
			setBeanData(beanEl, jsonBean.toString());
		} catch (Exception e) {}
	}

	private static void handleGrid(Element beanEl) {
		JSONObject jsonBean = getJsonBean(beanEl);
		try {
			if (jsonBean.has("Height")) {
				String height = jsonBean.getString("Height");
				int index = height.indexOf("plain:height:");
				if (index != -1) {
					jsonBean.put("Height", "plain:"+ height.substring(index + "plain:height:".length()));					
				}
			}
			jsonBean.remove("NoPadding");
			setBeanData(beanEl, jsonBean.toString());
		} catch (Exception e) {}
	}

	private static void handleGridCol(Element beanEl) {
		JSONObject jsonBean = getJsonBean(beanEl);
		try {
			JSONObject json = new JSONObject().put("ionBean", jsonBean.getString("ionBean"));
			@SuppressWarnings("unchecked")
			Iterator<String> it = jsonBean.keys();
			while (it.hasNext()) {
				String key = it.next();
				String value = jsonBean.getString(key);
				// @Vertical alignments
				if (key.startsWith("Align")) {
					if (!value.equals("plain:false")) {
						json.put("FlexAlignement", value.replaceFirst("plain\\:", "plain:ion-"));
					}
				}
				// @Widths
				else if (key.startsWith("Columns")) {
					if (!value.equals("plain:false")) {
						if (key.equals("Columns")) {
							json.put("Size", value);
						} else if (key.equals("ColumnsXSmall")) {
							json.put("SizeXs", value);							
						} else if (key.equals("ColumnsSmall")) {
							json.put("SizeSm", value);							
						} else if (key.equals("ColumnsMedium")) {
							json.put("SizeMd", value);							
						} else if (key.equals("ColumnsLarge")) {
							json.put("SizeLg", value);							
						} else if (key.equals("ColumnsXLarge")) {
							json.put("SizeXl", value);							
						}
					}
				}
				// @Offsets
				else if (key.startsWith("Offset")) {
					if (!value.equals("plain:false")) {
						if (key.equals("Offset")) {
							json.put("Offset", value);
						} else if (key.equals("OffsetXSmall")) {
							json.put("OffsetXs", value);							
						} else if (key.equals("OffsetSmall")) {
							json.put("OffsetSm", value);							
						} else if (key.equals("OffsetMedium")) {
							json.put("OffsetMd", value);							
						} else if (key.equals("OffsetLarge")) {
							json.put("OffsetLg", value);							
						} else if (key.equals("OffsetXLarge")) {
							json.put("OffsetXl", value);							
						}
					}
				}
			}
			setBeanData(beanEl, json.toString());
		} catch (Exception e) {}
	}
	
	private static void handleGridRow(Element beanEl) {
		JSONObject jsonBean = getJsonBean(beanEl);
		try {
			JSONObject json = new JSONObject().put("ionBean", jsonBean.getString("ionBean"));
			@SuppressWarnings("unchecked")
			Iterator<String> it = jsonBean.keys();
			while (it.hasNext()) {
				String key = it.next();
				String value = jsonBean.getString(key);
				// @Vertical alignments
				if (key.startsWith("Align")) {
					if (!value.equals("plain:false")) {
						json.put("FlexAlignement", value.replaceFirst("plain\\:", "plain:ion-"));
					}
				}
				// @Justifications
				else if (key.startsWith("JustifyContent")) {
					if (!value.equals("plain:false")) {
						json.put("FlexJustifyContent", value.replaceFirst("plain\\:", "plain:ion-"));
					}
				}
				// @Wrapping
				else if (key.equals("Nowrap")) {
					if (value.equals("plain:nowrap")) {
						json.put("FlexWrap", "ion-nowrap");
					}
				}
				else if (key.equals("WrapReverse")) {
					if (value.equals("plain:wrap-reverse")) {
						json.put("FlexWrap", "ion-wrap-reverse");
					}
				}
				else if (key.equals("Height")) {
					int index = value.indexOf("plain:height:");
					if (index != -1) {
						json.put("Height", "plain:"+ value.substring(index + "plain:height:".length()));					
					}
				}
			}
			setBeanData(beanEl, json.toString());
		} catch (Exception e) {}
	}

	private static void handleHeader(Element beanEl) {
		JSONObject jsonBean = getJsonBean(beanEl);
		try {
			jsonBean.remove("NoBorder");
			setBeanData(beanEl, jsonBean.toString());
		} catch (Exception e) {}
	}
	
	private static void handleHeading(Element beanEl) {
		JSONObject jsonBean = getJsonBean(beanEl);
		try {
			jsonBean.remove("IonColor");
			jsonBean.remove("IonText");
			setBeanData(beanEl, jsonBean.toString());
		} catch (Exception e) {}
	}
	
	private static void handleLabel(Element beanEl) {
		JSONObject jsonBean = getJsonBean(beanEl);
		try {
			if (jsonBean.has("Fixed")) {
				String value = jsonBean.getString("Fixed");
				if (value.equals("plain:fixed")) {
					jsonBean.put("Position", "plain:fixed");
				}
				jsonBean.remove("Fixed");
			}
			if (jsonBean.has("Floating")) {
				String value = jsonBean.getString("Floating");
				if (value.equals("plain:floating")) {
					jsonBean.put("Position", "plain:floating");
				}
				jsonBean.remove("Floating");
			}
			if (jsonBean.has("Stacked")) {
				String value = jsonBean.getString("Stacked");
				if (value.equals("plain:stacked")) {
					jsonBean.put("Position", "plain:stacked");
				}
				jsonBean.remove("Stacked");
			}
			jsonBean.remove("RangeLeft");
			jsonBean.remove("RangeRight");
			setBeanData(beanEl, jsonBean.toString());
		} catch (Exception e) {}
	}
	
	private static void handleOption(Element beanEl) {
		capitalizeBeanData(beanEl);
		
		JSONObject jsonBean = getJsonBean(beanEl);
		try {
			jsonBean.remove("selected");
			jsonBean.put("ionBean", "SelectOption");
			setBeanData(beanEl, jsonBean.toString());
		} catch (Exception e) {}
		
		setTagName(beanEl, "ion-select-option");
	}

	private static void handleProgressBar(Element beanEl) {
		JSONObject jsonBean = getJsonBean(beanEl);
		try {
			if (jsonBean.has("color")) {
				String value = jsonBean.getString("color");
				if (!value.equals("plain:false")) {
					jsonBean.put("IonColor", value);
				}
				jsonBean.remove("color");
			}
			jsonBean.remove("progress");
			setBeanData(beanEl, jsonBean.toString());
		} catch (Exception e) {}
		
		setTagName(beanEl, "ion-progress-bar");
	}

	private static void handleRadioButton(Element beanEl) {
		JSONObject jsonBean = getJsonBean(beanEl);
		try {
			if (jsonBean.has("Value")) {
				String value = jsonBean.getString("Value");
				if (value.equals("plain:false")) {
					jsonBean.put("Value", "plain:");
				}
			}
			jsonBean.remove("Checked");
			setBeanData(beanEl, jsonBean.toString());
		} catch (Exception e) {}
	}
	
	private static void handleRadioList(Element beanEl) {
		JSONObject jsonBean = getJsonBean(beanEl);
		try {
			if (jsonBean.has("DefaultValue")) {
				String value = jsonBean.getString("DefaultValue");
				if (value.equals("plain:false")) {
					jsonBean.put("Value", "plain:");
				} else {
					jsonBean.put("Value", value);
				}
				jsonBean.remove("DefaultValue");
			}
			jsonBean.remove("ListType");
			jsonBean.put("ionBean", "RadioGroup");
			setBeanData(beanEl, jsonBean.toString());
			
		} catch (Exception e) {}
		
		setTagName(beanEl, "ion-radio-group");
	}
	
	private static void handleRange(Element beanEl) {
		JSONObject jsonBean = getJsonBean(beanEl);
		try {
			if (jsonBean.has("DefaultValue")) {
				String value = jsonBean.getString("DefaultValue");
				if (value.equals("plain:false")) {
					jsonBean.put("Value", "plain:");
				} else {
					jsonBean.put("Value", value);
				}
				jsonBean.remove("DefaultValue");
			}
			if (jsonBean.has("Debounce")) {
				String value = jsonBean.getString("Debounce");
				if (value.equals("plain:false")) {
					jsonBean.put("Debounce", "plain:0");
				}
			}
			if (jsonBean.has("Max")) {
				String value = jsonBean.getString("Max");
				if (value.equals("plain:false")) {
					jsonBean.put("Max", "plain:0");
				}
			}
			if (jsonBean.has("Min")) {
				String value = jsonBean.getString("Min");
				if (value.equals("plain:false")) {
					jsonBean.put("Min", "plain:0");
				}
			}
			setBeanData(beanEl, jsonBean.toString());
		} catch (Exception e) {}
	}
	
	private static void handleFormButton(Element beanEl) {
		JSONObject jsonBean = getJsonBean(beanEl);
		try {
			if (jsonBean.has("IconOnly")) {
				String value = jsonBean.getString("IconOnly");
				if (value.equals("plain:icon-only")) {
					jsonBean.put("IconSlot", value);
				}
				jsonBean.remove("IconOnly");
			}
			if (jsonBean.has("IconPosition")) {
				String value = jsonBean.getString("IconPosition");
				if (value.equals("plain:icon-only")) {
					jsonBean.put("IconSlot", value);
				}
				if (value.equals("plain:icon-start") || value.equals("plain:icon-left")) {
					jsonBean.put("IconSlot", "plain:start");
				}
				if (value.equals("plain:icon-end") || value.equals("plain:icon-right")) {
					jsonBean.put("IconSlot", "plain:end");
				}
				jsonBean.remove("IconPosition");
			}
			if (jsonBean.has("IonClear")) {
				String value = jsonBean.getString("IonClear");
				if (value.equals("plain:clear")) {
					jsonBean.put("IonFill", value);
				}
				jsonBean.remove("IonClear");
			}
			if (jsonBean.has("IonOutline")) {
				String value = jsonBean.getString("IonOutline");
				if (value.equals("plain:outline")) {
					jsonBean.put("IonFill", value);
				}
				jsonBean.remove("IonOutline");
			}
			if (jsonBean.has("IonShape")) {
				String value = jsonBean.getString("IonShape");
				if (value.equals("plain:round")) {
					jsonBean.put("IonShape", value);
				}
				if (value.equals("plain:block") || value.equals("plain:full")) {
					jsonBean.put("IonExpand", value);
				}
			}
			if (jsonBean.has("IonSize")) {
				String value = jsonBean.getString("IonSize");
				if (value.equals("plain:mini")) {
					jsonBean.put("IonSize", "plain:default");
				}
			}
			jsonBean.remove("ButtonType");
			setBeanData(beanEl, jsonBean.toString());
		} catch (Exception e) {}
		
		setTagName(beanEl, "ion-button");
	}
	
	private static void handleSegment(Element beanEl) {
		JSONObject jsonBean = getJsonBean(beanEl);
		try {
			if (jsonBean.has("Name")) {
				String value = jsonBean.getString("Name");
				jsonBean.put("Value", value);
			}
			jsonBean.remove("Name");
			jsonBean.remove("IonColor");
			setBeanData(beanEl, jsonBean.toString());
		} catch (Exception e) {}
	}
	
	private static void handleSelect(Element beanEl) {
		capitalizeBeanData(beanEl);
		
		JSONObject jsonBean = getJsonBean(beanEl);
		try {
			if (jsonBean.has("CancelText")) {
				String value = jsonBean.getString("CancelText");
				if (value.equals("plain:false")) {
					jsonBean.put("CancelText", "plain:Cancel");
				}
			}
			if (jsonBean.has("OkText")) {
				String value = jsonBean.getString("OkText");
				if (value.equals("plain:false")) {
					jsonBean.put("OkText", "plain:Ok");
				}
			}
			if (jsonBean.has("DefaultValue")) {
				String value = jsonBean.getString("DefaultValue");
				if (value.equals("plain:false")) {
					jsonBean.put("Value", "plain:");
				} else {
					jsonBean.put("Value", value);
				}
				jsonBean.remove("DefaultValue");
			}
			setBeanData(beanEl, jsonBean.toString());
		} catch (Exception e) {}
	}
	
	private static void handleSpinner(Element beanEl) {
		capitalizeBeanData(beanEl);
		
		JSONObject jsonBean = getJsonBean(beanEl);
		try {
			if (jsonBean.has("Name")) {
				String value = jsonBean.getString("Name");
				if (value.equals("plain:ios")) {
					jsonBean.put("Name", "plain:false");
				}
			}
			setBeanData(beanEl, jsonBean.toString());
		} catch (Exception e) {}
	}

	private static void handleTabs(Element beanEl) {
		cleanBeanData(beanEl);
		
		try {
			DatabaseObject dbo = com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.createBean(
					com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.getComponentByName("TabBar"));
			if (dbo != null) {
				JSONObject jsonTabBar = new JSONObject().put("ionBean", "TabBar");
				
				Element tabBarEl = beanEl.getOwnerDocument().createElement("bean");
				tabBarEl.setAttribute("yaml_key", "TabBar [ngx.components.UIDynamicElement-"+ dbo.priority +"]");
				Element child1 = beanEl.getOwnerDocument().createElement("beanData");
				child1.appendChild(beanEl.getOwnerDocument().createTextNode(jsonTabBar.toString()));
				tabBarEl.appendChild(child1);
				Element child2 = beanEl.getOwnerDocument().createElement("comment");
				child2.appendChild(beanEl.getOwnerDocument().createTextNode("Tab bar"));
				tabBarEl.appendChild(child2);
				Element child3 = beanEl.getOwnerDocument().createElement("tagName");
				child3.appendChild(beanEl.getOwnerDocument().createTextNode("ion-tab-bar"));
				tabBarEl.appendChild(child3);
				
				beanEl.insertBefore(tabBarEl, xpath.selectList(beanEl, "bean").get(0));
				Node node = null;
				while ((node = tabBarEl.getNextSibling()) != null) {
					tabBarEl.appendChild(node);
				}
			}
		} catch (Exception e) {}
	}
	
	private static void handleTab(Element beanEl) {
		JSONObject jsonBean = getJsonBean(beanEl);
		String tabTitle = "tab";
		String tabIcon = "bookmark";
		
		try {
			if (jsonBean.has("TabTitle")) {
				String value = jsonBean.getString("TabTitle");
				if (!value.equals("plain:false")) {
					tabTitle = value.substring(value.indexOf(":")+1);
				}
			}
			if (jsonBean.has("TabIcon")) {
				String value = jsonBean.getString("TabIcon");
				if (!value.equals("plain:false")) {
					tabIcon = value.substring(value.indexOf(":")+1);
				}
			}
			
			JSONObject json = new JSONObject().put("ionBean", "TabButton");
			setBeanData(beanEl, json.toString());
			setTagName(beanEl, "ion-tab-button");
			
			String yaml_key = beanEl.getAttribute("yaml_key");
			beanEl.getAttributeNode("yaml_key").setTextContent(yaml_key.replace("UIDynamicTab", "UIDynamicTabButton"));
		} catch (Exception e) {}	

		try {
			DatabaseObject dbo = null;
			
			dbo = com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.createBean(
					com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.getComponentByName("Label"));
			if (dbo != null) {
				JSONObject jsonLabel =  new JSONObject().put("ionBean", "Label");
				
				Element labelEl = beanEl.getOwnerDocument().createElement("bean");
				labelEl.setAttribute("yaml_key", "Label [ngx.components.UIDynamicElement-"+ dbo.priority +"]");
				beanEl.appendChild(labelEl);
				Element child1 = beanEl.getOwnerDocument().createElement("beanData");
				child1.appendChild(beanEl.getOwnerDocument().createTextNode(jsonLabel.toString()));
				labelEl.appendChild(child1);
				Element child2 = beanEl.getOwnerDocument().createElement("comment");
				child2.appendChild(beanEl.getOwnerDocument().createTextNode("Tab label"));
				labelEl.appendChild(child2);
				Element child3 = beanEl.getOwnerDocument().createElement("tagName");
				child3.appendChild(beanEl.getOwnerDocument().createTextNode("ion-label"));
				labelEl.appendChild(child3);
				
				dbo = com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.createBean(
						com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.getComponentByName("UIText"));
				if (dbo != null) {
					Element textEl = beanEl.getOwnerDocument().createElement("bean");
					textEl.setAttribute("yaml_key", "Text [ngx.components.UIText-"+ dbo.priority +"]");
					labelEl.appendChild(textEl);
					Element textValueEl = beanEl.getOwnerDocument().createElement("textValue");
					textEl.appendChild(textValueEl);
					Element xmlizableEl = beanEl.getOwnerDocument().createElement("xmlizable");
					xmlizableEl.setAttribute("classname", "com.twinsoft.convertigo.beans.ngx.components.MobileSmartSourceType");
					textValueEl.appendChild(xmlizableEl);
					Element sourceEl = beanEl.getOwnerDocument().createElement("MobileSmartSourceType");
					sourceEl.appendChild(beanEl.getOwnerDocument().createTextNode("plain:"+ tabTitle));
					xmlizableEl.appendChild(sourceEl);
				}
			}
			
			dbo = com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.createBean(
					com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.getComponentByName("Icon"));
			if (dbo != null) {
				JSONObject jsonIcon = new JSONObject().put("ionBean", "Icon").put("IconName", "plain:"+ tabIcon);
				
				Element iconEl = beanEl.getOwnerDocument().createElement("bean");
				iconEl.setAttribute("yaml_key", "Icon [ngx.components.UIDynamicElement-"+ dbo.priority +"]");
				beanEl.appendChild(iconEl);
				Element child1 = beanEl.getOwnerDocument().createElement("beanData");
				child1.appendChild(beanEl.getOwnerDocument().createTextNode(jsonIcon.toString()));
				iconEl.appendChild(child1);
				Element child2 = beanEl.getOwnerDocument().createElement("comment");
				child2.appendChild(beanEl.getOwnerDocument().createTextNode("Icon"));
				iconEl.appendChild(child2);
				Element child3 = beanEl.getOwnerDocument().createElement("tagName");
				child3.appendChild(beanEl.getOwnerDocument().createTextNode("ion-icon"));
				iconEl.appendChild(child3);
			}
			
		} catch (Exception e) {}
	}
	
	private static void handleTextFormat(Element beanEl) {
		JSONObject jsonBean = getJsonBean(beanEl);
		try {
			Element parentEl = (Element) beanEl.getParentNode();
			DatabaseObject dbo = com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.createBean(
					com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.getComponentByName("FormatLayout"));
			if (dbo != null) {
				JSONObject jsonData = new JSONObject().put("ionBean", "FormatLayout");
				
				try {
					if (jsonBean.has("TextLeft")) {
						String value = jsonBean.getString("TextLeft");
						if (value.equals("plain:text-left")) {
							jsonData.put("TextAlignment", "plain:ion-text-left");
						}
					}
					if (jsonBean.has("TextRight")) {
						String value = jsonBean.getString("TextRight");
						if (value.equals("plain:text-right")) {
							jsonData.put("TextAlignment", "plain:ion-text-right");
						}
					}
					if (jsonBean.has("TextCenter")) {
						String value = jsonBean.getString("TextCenter");
						if (value.equals("plain:text-center")) {
							jsonData.put("TextAlignment", "plain:ion-text-center");
						}
					}
					if (jsonBean.has("TextJustify")) {
						String value = jsonBean.getString("TextJustify");
						if (value.equals("plain:text-justify")) {
							jsonData.put("TextAlignment", "plain:ion-text-justify");
						}
					}
					if (jsonBean.has("TextWrap")) {
						String value = jsonBean.getString("TextWrap");
						if (value.equals("plain:text-wrap")) {
							jsonData.put("TextAlignment", "plain:ion-text-wrap");
						}
					}
					if (jsonBean.has("TextNoWrap")) {
						String value = jsonBean.getString("TextNoWrap");
						if (value.equals("plain:text-nowrap")) {
							jsonData.put("TextAlignment", "plain:ion-text-nowrap");
						}
					}

					if (jsonBean.has("TextUpperCase")) {
						String value = jsonBean.getString("TextUpperCase");
						if (value.equals("plain:text-uppercase")) {
							jsonData.put("TextTransformation", "plain:ion-text-uppercase");
						}
					}
					if (jsonBean.has("TextLowerCase")) {
						String value = jsonBean.getString("TextLowerCase");
						if (value.equals("plain:text-lowercase")) {
							jsonData.put("TextTransformation", "plain:ion-text-lowercase");
						}
					}
					if (jsonBean.has("TextCapitalize")) {
						String value = jsonBean.getString("TextCapitalize");
						if (value.equals("plain:text-capitalize")) {
							jsonData.put("TextTransformation", "plain:ion-text-capitalize");
						}
					}
				} catch (Exception e) {}
				
				
				Element formatLayoutEl = beanEl.getOwnerDocument().createElement("bean");
				formatLayoutEl.setAttribute("yaml_key", "FormatLayout [ngx.components.UIDynamicAttr-"+ dbo.priority +"]");
				Element fchild1 = beanEl.getOwnerDocument().createElement("beanData");
				fchild1.appendChild(beanEl.getOwnerDocument().createTextNode(jsonData.toString()));
				formatLayoutEl.appendChild(fchild1);
				Element fchild2 = beanEl.getOwnerDocument().createElement("comment");
				fchild2.appendChild(beanEl.getOwnerDocument().createTextNode("FormatLayout"));
				formatLayoutEl.appendChild(fchild2);
				Element fchild3 = beanEl.getOwnerDocument().createElement("tagName");
				fchild3.appendChild(beanEl.getOwnerDocument().createTextNode("div"));
				formatLayoutEl.appendChild(fchild3);
				
				parentEl.insertBefore(formatLayoutEl, beanEl);
				for (Node child: xpath.selectList(beanEl, "bean")) {
					parentEl.insertBefore(child, beanEl);
				}
				parentEl.removeChild(beanEl);
			}
		} catch (Exception e) {}
	}
	
	private static void handlePageEvent(Element beanEl) {
		try {
			String yaml_key = beanEl.getAttribute("yaml_key");
			Matcher matcherBeanName = patternBeanName.matcher(yaml_key);
			matcherBeanName.matches();
			String beanName = matcherBeanName.group(1);
			if (beanName != null && !beanName.isEmpty()) {
				Node viewEventNode = xpath.selectList(beanEl, "viewEvent").get(0);
				String viewEvent = viewEventNode.getTextContent();
				if (viewEvent != null && !viewEvent.isEmpty()) {
					if ("onCanEnter".equals(viewEvent) || "onCanLeave".equals(viewEvent)) {
						try {
							Element parentEl = (Element) beanEl.getParentNode();
							DatabaseObject dbo = com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.createBean(
									com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.getComponentByName("UIEventSubscriber"));
							if (dbo != null) {
								Element eventSubscriberEl = beanEl.getOwnerDocument().createElement("bean");
								eventSubscriberEl.setAttribute("yaml_key", beanName + " [ngx.components.UIEventSubscriber-"+ dbo.priority +"]");
								Element fchild1 = beanEl.getOwnerDocument().createElement("comment");
								fchild1.appendChild(beanEl.getOwnerDocument().createTextNode("Old implementation of "+ viewEvent + ". Prefer using AppGuard now."));
								eventSubscriberEl.appendChild(fchild1);
								Element fchild2 = beanEl.getOwnerDocument().createElement("topic");
								fchild2.appendChild(beanEl.getOwnerDocument().createTextNode(viewEvent.substring(2)));
								eventSubscriberEl.appendChild(fchild2);
								
								parentEl.insertBefore(eventSubscriberEl, beanEl);
								for (Node child: xpath.selectList(beanEl, "bean")) {
									eventSubscriberEl.appendChild(child.cloneNode(true));
								}
								parentEl.removeChild(beanEl);
							}
						} catch (Exception e) {}
					} else if ("onWillUnload".equals(viewEvent)) {
						try {
							Element parentEl = (Element) beanEl.getParentNode();
							if (parentEl != null) {
								String parent_yaml_key = parentEl.getAttribute("yaml_key");;
								if (parent_yaml_key.indexOf("ngx.components.UISharedRegularComponent") != -1) {
									viewEventNode.setTextContent("onDidLeave");
								}
							}
						} catch (Exception e) {}
					}
				}
			}
		} catch (Exception e) {}
	}
	
	private static void handleSharedComponent(Element beanEl, Map<Element, List<Element>> sharedMap) {
		String yaml_key = beanEl.getAttribute("yaml_key");
		for (Element el: sharedMap.get(beanEl)) {
			try {
				JSONObject jsonBean = getJsonBean(el);
				
				// for PublishEventAction inside a shared component
				if ("PublishEventAction".equalsIgnoreCase(jsonBean.getString("ionBean"))) {
					String eventName = null;
					
					String topic = jsonBean.getString("topic");
					if (topic.indexOf("plain:") != -1) {
						eventName = topic.substring(topic.indexOf(":")+1);
					} else if (topic.indexOf("script:'") != -1) {
						eventName = topic.substring(topic.indexOf("'")+1, topic.lastIndexOf("'"));
					}
					if (eventName != null) {
						DatabaseObject dbo = null;
						
						// add a CompEvent for each event a shared component expose
						dbo = com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.createBean(
								com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.getComponentByName("UICompEvent"));
						if (dbo != null) {
							Element compEventEl = beanEl.getOwnerDocument().createElement("bean");
							compEventEl.setAttribute("yaml_key", eventName + " [ngx.components.UICompEvent-"+ dbo.priority +"]");
							
							Element childAttr = beanEl.getOwnerDocument().createElement("attrName");
							childAttr.appendChild(beanEl.getOwnerDocument().createTextNode(eventName));
							compEventEl.appendChild(childAttr);
							
							if (xpath.selectList(beanEl, "bean[starts-with(@yaml_key,'"+eventName+" ')]").size() == 0) {
								beanEl.appendChild(compEventEl);
							}
						}
						
						// add an EmitEventAction action beside each PublishEventAction
						dbo = com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.createBean(
								com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.getComponentByName("EmitEventAction"));
						if (dbo != null) {
							Element emitEventEl = beanEl.getOwnerDocument().createElement("bean");
							emitEventEl.setAttribute("yaml_key", "EmitEvent" + " [ngx.components.UIDynamicEmit-"+ dbo.priority +"]");
							
							String qname = getBeanQName(beanEl) + "." + eventName;
							
							JSONObject jsonEmit =  new JSONObject().put("ionBean", "EmitEventAction");
							jsonEmit.put("event", "plain:"+ qname);
							jsonEmit.put("data", jsonBean.getString("data"));
							
							Element emitDataChild = beanEl.getOwnerDocument().createElement("beanData");
							emitDataChild.appendChild(beanEl.getOwnerDocument().createTextNode(jsonEmit.toString()));
							emitEventEl.appendChild(emitDataChild);
							
							if (xpath.selectList(el.getParentNode(), "bean[starts-with(@yaml_key,'EmitEvent ')]").size() == 0) {
								el.getParentNode().insertBefore(emitEventEl, el);
							}
						}
					}
				}
			} catch (Exception e) {
				System.err.println("Error handling shared component : "+ yaml_key.replaceAll("ngx\\.components", "mobile.components"));
				System.err.println(XMLUtils.prettyPrintElement(el).replaceAll("ngx\\.components", "mobile.components"));
			}
		}
		
		// replace 'paramsXXXXX' with 'this' (where XXXXX is the shared component priority)
		String priority = yaml_key.substring(yaml_key.lastIndexOf('-')+1, yaml_key.indexOf(']'));
		String params = "params"+ priority;
		for (Node node: xpath.selectList(beanEl, "//*[text()[contains(.,'"+ params +"')]]")) {
			String content = node.getTextContent();
			try {
				content = content.replaceAll("props.stack.root.scope."+ params, "this");
				content = content.replaceAll("stack.root.scope."+ params, "this");
				content = content.replaceAll(params, "this");
				node.setTextContent(content);
			} catch (Exception e) {
				System.err.println("Error handling shared component : "+ yaml_key.replaceAll("ngx\\.components", "mobile.components"));
				System.err.println("could not replace "+ params +" in "+ content);
			}
		}
	}
	
	private static void handleToggle(Element beanEl) {
		JSONObject jsonBean = getJsonBean(beanEl);
		try {
			jsonBean.remove("Checked");
			setBeanData(beanEl, jsonBean.toString());
		} catch (Exception e) {}
	}
	
	private static void handleTooltips(Element beanEl) {
		JSONObject jsonBean = getJsonBean(beanEl);
		try {
			JSONObject json = new JSONObject().put("ionBean", "Tooltips");
			if (jsonBean.has("duration")) {
				json.put("AnimationDuration", jsonBean.get("duration"));
			}
			if (jsonBean.has("tooltip")) {
				json.put("Tooltip", jsonBean.get("tooltip"));
			}
			if (jsonBean.has("event")) {
				json.put("Trigger", jsonBean.get("event"));
			}
			setBeanData(beanEl, json.toString());
		} catch (Exception e) {}
	}
	
	private static void handleScroll(Element beanEl) {
		JSONObject jsonBean = getJsonBean(beanEl);
		try {
			jsonBean.put("ionBean", "DivTag");
			setBeanData(beanEl, jsonBean.toString());
		} catch (Exception e) {}
		setTagName(beanEl, "div");
	}

	private static void handleSliding(Element beanEl) {
		JSONObject jsonBean = getJsonBean(beanEl);
		try {
			jsonBean.put("ionBean", "SlidingContainer");
			setBeanData(beanEl, jsonBean.toString());
		} catch (Exception e) {}
	}
	
	private static void handleSlidingOption(Element beanEl) {
		JSONObject jsonBean = getJsonBean(beanEl);
		try {
			jsonBean.put("ionBean", "SlidingButtonSet");			
			if (jsonBean.has("side")) {
				String value = jsonBean.getString("side");
				jsonBean.remove("side");
				if (value.equals("plain:left")) {
					jsonBean.put("Side", "plain:start");
				}
				if (value.equals("plain:right")) {
					jsonBean.put("Side", "plain:end");
				}
			}
			setBeanData(beanEl, jsonBean.toString());
		} catch (Exception e) {}
	}
	
	private static void handleAlertAction(Element beanEl) {
		JSONObject jsonBean = getJsonBean(beanEl);
		try {
			if (jsonBean.has("enableBackdropDismiss")) {
				jsonBean.put("backdropDismiss", jsonBean.getString("enableBackdropDismiss"));
				jsonBean.remove("enableBackdropDismiss");
			}
			if (jsonBean.has("title")) {
				String value = jsonBean.getString("title");
				if (!value.equals("plain:false")) {
					jsonBean.put("header", value);
				}
				jsonBean.remove("title");
			}
			if (jsonBean.has("subTitle")) {
				String value = jsonBean.getString("subTitle");
				if (!value.equals("plain:false")) {
					jsonBean.put("subHeader", value);
				}
				jsonBean.remove("subTitle");
			}
			setBeanData(beanEl, jsonBean.toString());
		} catch (Exception e) {}
	}
	
	private static void handleModalAction(Element beanEl) {
		JSONObject jsonBean = getJsonBean(beanEl);
		try {
			if (jsonBean.has("cssClass")) {
				String value = jsonBean.getString("cssClass");
				if (value.equals("plain:false")) {
					jsonBean.put("cssClass", "plain:");
				}
			}
			setBeanData(beanEl, jsonBean.toString());
		} catch (Exception e) {}
	}
	
	private static void handlePopoverAction(Element beanEl) {
		JSONObject jsonBean = getJsonBean(beanEl);
		try {
			if (jsonBean.has("cssClass")) {
				String value = jsonBean.getString("cssClass");
				if (value.equals("plain:false")) {
					jsonBean.put("cssClass", "plain:");
				}
			}
			setBeanData(beanEl, jsonBean.toString());
		} catch (Exception e) {}
	}
	
	private static void handleShowLoadingAction(Element beanEl) {
		JSONObject jsonBean = getJsonBean(beanEl);
		try {
			jsonBean.remove("content");
			setBeanData(beanEl, jsonBean.toString());
		} catch (Exception e) {}
	}

	private static void handleToastAction(Element beanEl) {
		JSONObject jsonBean = getJsonBean(beanEl);
		try {
			if (jsonBean.has("cssClass")) {
				String value = jsonBean.getString("cssClass");
				if (value.equals("plain:false")) {
					jsonBean.put("cssClass", "plain:");
				}
			}
			jsonBean.remove("dismissOnPageChange");
			setBeanData(beanEl, jsonBean.toString());
		} catch (Exception e) {}
	}

	private Element sharedCompEl = null;
	
	private void convertBean(Element element) throws Exception {
		for (Node node: xpath.selectList(element, "//*[@classname]")) {
			Element el = (Element) node;
			String classname = el.getAttribute("classname");
			el.getAttributeNode("classname").setTextContent(classname.replaceFirst("\\.mobile\\.", ".ngx."));
		}
		
		for (Node node: xpath.selectList(element, "bean[@yaml_key]")) {
			Element beanEl = (Element) node;
			boolean isTextFormat = false;
			boolean isPageEvent = false;
			
			String _indent = indent;
			
			String yaml_key = beanEl.getAttribute("yaml_key");
			
			// Ionic3 project migration
			if (yaml_key.indexOf("mobile.components") != -1) {
				
				if (yaml_key.indexOf("mobile.components.UISharedComponent") != -1) {
					sharedCompEl = beanEl;
					sharedMap.put(beanEl, new ArrayList<Element>());
				}
				
				if (yaml_key.indexOf("mobile.components.UIPageEvent") != -1) {
					isPageEvent = true;
				}
				
				beanEl.getAttributeNode("yaml_key").setTextContent(yaml_key.replaceFirst("mobile\\.components", "ngx.components"));
				
				if (checkBean(beanEl)) {
					
					// Generals
					handleBean(beanEl);
					
					// Pseudo-beans Specifics
					String ionBeanName = getIonBeanName(beanEl);
					if ("NavBar".equalsIgnoreCase(ionBeanName)) {
						handleNavBar(beanEl);
					}
					else if ("Button".equalsIgnoreCase(ionBeanName)) {
						handleButton(beanEl);
					}
					else if ("FabButton".equalsIgnoreCase(ionBeanName)) {
						handleFabButton(beanEl);
					}
					else if ("ButtonSet".equalsIgnoreCase(ionBeanName)) {
						handleButtonSet(beanEl);
					}
					else if ("Icon".equalsIgnoreCase(ionBeanName)) {
						handleIcon(beanEl);
					}
					else if ("List".equalsIgnoreCase(ionBeanName)) {
						handleList(beanEl);
					}
					else if ("DateTime".equalsIgnoreCase(ionBeanName)) {
						handleDateTime(beanEl);
					}
					else if ("FabContainer".equalsIgnoreCase(ionBeanName)) {
						handleFabContainer(beanEl);
					}
					else if ("FabList".equalsIgnoreCase(ionBeanName)) {
						handleFabList(beanEl);
					}
					else if ("Grid".equalsIgnoreCase(ionBeanName)) {
						handleGrid(beanEl);
					}
					else if ("GridCol".equalsIgnoreCase(ionBeanName)) {
						handleGridCol(beanEl);
					}
					else if ("GridRow".equalsIgnoreCase(ionBeanName)) {
						handleGridRow(beanEl);
					}
					else if ("Header".equalsIgnoreCase(ionBeanName)) {
						handleHeader(beanEl);
					}
					else if (ionBeanName.startsWith("Heading")) {
						handleHeading(beanEl);
					}
					else if ("Label".equalsIgnoreCase(ionBeanName)) {
						handleLabel(beanEl);
					}
					else if ("MenuAutoItems".equalsIgnoreCase(ionBeanName) ||"MenuItem".equalsIgnoreCase(ionBeanName)) {
						handleMenuItems(beanEl);
					}
					else if ("Option".equalsIgnoreCase(ionBeanName)) {
						handleOption(beanEl);
					}
					else if ("ProgressBar".equalsIgnoreCase(ionBeanName)) {
						handleProgressBar(beanEl);
					}
					else if ("RadioList".equalsIgnoreCase(ionBeanName)) {
						handleRadioList(beanEl);
					}
					else if ("RadioButton".equalsIgnoreCase(ionBeanName)) {
						handleRadioButton(beanEl);
					}
					else if ("Range".equalsIgnoreCase(ionBeanName)) {
						handleRange(beanEl);
					}
					else if ("RefresherContent".equalsIgnoreCase(ionBeanName)) {
						cleanBeanData(beanEl);
					}
					else if ("ResetButton".equalsIgnoreCase(ionBeanName) || "SubmitButton".equalsIgnoreCase(ionBeanName)) {
						handleFormButton(beanEl);
					}
					else if (ionBeanName.startsWith("Segment") || ionBeanName.startsWith("SegmentButton")) {
						handleSegment(beanEl);
					}
					else if ("Select".equalsIgnoreCase(ionBeanName)) {
						handleSelect(beanEl);
					}
					else if ("Slides".equalsIgnoreCase(ionBeanName)) {
						cleanBeanData(beanEl);
					}
					else if ("Spinner".equalsIgnoreCase(ionBeanName)) {
						handleSpinner(beanEl);
					}
					else if ("Tabs".equalsIgnoreCase(ionBeanName)) {
						handleTabs(beanEl);
					}
					else if ("Tab".equalsIgnoreCase(ionBeanName)) {
						handleTab(beanEl);
					}
					else if ("TextFormat".equalsIgnoreCase(ionBeanName)) {
						isTextFormat = true;
					}
					else if ("Toggle".equalsIgnoreCase(ionBeanName)) {
						handleToggle(beanEl);
					}
					else if ("Tooltips".equalsIgnoreCase(ionBeanName)) {
						handleTooltips(beanEl);
					}
					else if ("Scroll".equalsIgnoreCase(ionBeanName)) {
						handleScroll(beanEl);
					}
					else if ("Sliding".equalsIgnoreCase(ionBeanName)) {
						handleSliding(beanEl);
					}
					else if ("SlidingOption".equalsIgnoreCase(ionBeanName)) {
						handleSlidingOption(beanEl);
					}
					else if ("AlertAction".equalsIgnoreCase(ionBeanName)) {
						handleAlertAction(beanEl);
					}
					else if ("ModalAction".equalsIgnoreCase(ionBeanName)) {
						handleModalAction(beanEl);
					}
					else if ("PopoverAction".equalsIgnoreCase(ionBeanName)) {
						handlePopoverAction(beanEl);
					}
					else if ("ShowLoadingAction".equalsIgnoreCase(ionBeanName)) {
						handleShowLoadingAction(beanEl);
					}
					else if ("ToastAction".equalsIgnoreCase(ionBeanName)) {
						handleToastAction(beanEl);
					}
					else if ("PublishEventAction".equalsIgnoreCase(ionBeanName)) {
						if (sharedCompEl != null) {
							sharedMap.get(sharedCompEl).add(beanEl);
						}
					}
					else if (!checkPseudoBean(beanEl)) {
						continue;
					}
					
					System.out.println(indent+ yaml_key + " : SUCCESS");
				} else {
					continue;
				}
				
				indent += "\t";
			}
			// Already migrated project
			else if (yaml_key.indexOf("ngx.components") != -1) {
				
				// for shared component
				if (yaml_key.indexOf("ngx.components.UISharedComponent") != -1) {
					beanEl.getAttributeNode("yaml_key").setTextContent(yaml_key.replaceFirst("UISharedComponent", "UISharedRegularComponent"));
					sharedCompEl = beanEl;
					sharedMap.put(beanEl, new ArrayList<Element>());
				}
				
				// for useshared variable
				if (yaml_key.indexOf("ngx.components.UIControlVariable") != -1) {
					Element parentEl = (Element) beanEl.getParentNode();
					if (parentEl.getAttribute("yaml_key").indexOf("ngx.components.UIUseShared") != -1) {
						beanEl.getAttributeNode("yaml_key").setTextContent(yaml_key.replaceFirst("UIControlVariable", "UIUseVariable"));
					}
				}
				
				String ionBeanName = getIonBeanName(beanEl);
				if ("PublishEventAction".equalsIgnoreCase(ionBeanName)) {
					if (sharedCompEl != null) {
						sharedMap.get(sharedCompEl).add(beanEl);
					}
				}
			}
			
			convertBean(beanEl);
			indent =_indent;
			
			if (isTextFormat) {
				handleTextFormat(beanEl);
			}
			
			if (isPageEvent) {
				handlePageEvent(beanEl); // for removed canEnter, canLeave
			}
			
			if (beanEl.equals(sharedCompEl)) {
				handleSharedComponent(beanEl, sharedMap); // add CompEvent, EmitEventAction
				sharedCompEl = null;
			}
		}
	}
	
	private Map<Element, List<Element>> sharedMap = new HashMap<Element, List<Element>>();
	
	public void convertFile() throws Exception {
		File yaml = new File(outputDir, "c8oProject.yaml");
		
		Document document = YamlConverter.readYaml(yaml);
		//XMLUtils.saveXml(document, new File(outputDir, "a.xml"));
		Element root = document.getDocumentElement();
		root.getAttributeNode("convertigo").setTextContent("8.0.0.m006");
		convertBean(root);
		//XMLUtils.saveXml(document, new File(outputDir, "b.xml"));
		document = BeansDefaultValues.unshrinkProject(document);
		Document shrink = BeansDefaultValues.shrinkProject(document);
		YamlConverter.writeYaml(shrink, new File(outputDir, "c8oProject.yaml"), new File(outputDir, "_c8oProject"));
		
		sharedMap.clear();
		
		com.twinsoft.convertigo.engine.util.FileUtils.deleteQuietly(new File(outputDir, "_private/ionic"));
	}

	private static String time() {
		return new SimpleDateFormat("HH:mm:ss").format(new Date(System.currentTimeMillis()));
	}
	
	private synchronized static void initEngine() throws EngineException {
		if (Engine.isCliMode()) {
			return;
		}
		
		Engine.startStopDate = System.currentTimeMillis();
		
		EnginePropertiesManager.initProperties();
		Engine.logConvertigo = Logger.getLogger("cems");
		Engine.logEngine = Logger.getLogger("cems.Engine");
		Engine.logAdmin = Logger.getLogger("cems.Admin");
		Engine.logBeans = Logger.getLogger("cems.Beans");
		Engine.logBillers = Logger.getLogger("cems.Billers");
		Engine.logEmulators = Logger.getLogger("cems.Emulators");
		Engine.logContext = Logger.getLogger("cems.Context");
		Engine.logUser = Logger.getLogger("cems.Context.User");
		Engine.logUsageMonitor = Logger.getLogger("cems.UsageMonitor");
		Engine.logStatistics = Logger.getLogger("cems.Statistics");
		Engine.logScheduler = Logger.getLogger("cems.Scheduler");
		Engine.logSiteClipper = Logger.getLogger("cems.SiteClipper");
		Engine.logSecurityFilter = Logger.getLogger("cems.SecurityFilter");
		Engine.logStudio = Logger.getLogger("cems.Studio");
		Engine.logAudit = Logger.getLogger("cems.Context.Audit");
		
		// Managers
		Engine.logContextManager = Logger.getLogger("cems.ContextManager");
		Engine.logCacheManager = Logger.getLogger("cems.CacheManager");
		Engine.logTracePlayerManager = Logger.getLogger("cems.TracePlayerManager");
		Engine.logJobManager = Logger.getLogger("cems.JobManager");
		Engine.logCertificateManager = Logger.getLogger("cems.CertificateManager");
		Engine.logDatabaseObjectManager = Logger.getLogger("cems.DatabaseObjectManager");
		Engine.logProxyManager = Logger.getLogger("cems.ProxyManager");
		Engine.logDevices = Logger.getLogger("cems.Devices");
		Engine.logCouchDbManager = Logger.getLogger("cems.CouchDbManager");
		Engine.logSecurityTokenManager = Logger.getLogger("cems.SecurityTokenManager");

		/*Engine.theApp = new Engine();
		Engine.theApp.eventManager = new EventManager();
		Engine.theApp.eventManager.init();
		Engine.theApp.referencedProjectManager = new ReferencedProjectManager();
		Engine.theApp.databaseObjectsManager = new DatabaseObjectsManager();
		Engine.theApp.databaseObjectsManager.init();
		Engine.theApp.proxyManager = new ProxyManager();
		Engine.theApp.proxyManager.init();
		
		Engine.theApp.httpClient4 = HttpUtils.makeHttpClient(true);
		Engine.theApp.httpClient = HttpUtils.makeHttpClient3(true);
		
		Engine.logEngine.info("Using Properties: " + System.getProperties());*/
		
		Engine.isStarted = true;
	}
	
	public static void main(String[] args) {
		PrintStream stdout = System.out;
		PrintStream stderr = System.err;
		try {
			if (args.length > 0) {
				File projectsDir = new File(args[0]);
				String sourceProjectName = args[1];
				String targetProjectName = args[2];
				
				boolean needCopy = !sourceProjectName.equalsIgnoreCase(targetProjectName);
				
				if (!projectsDir.exists() || !projectsDir.isDirectory()) {
					System.err.println("Directory " + projectsDir.getCanonicalPath() + " doesn't exists nor a directory.");
					return;
				}
				
				File inputDir = new File(projectsDir, sourceProjectName);
				if (!inputDir.exists() || !inputDir.isDirectory()) {
					System.err.println("Directory " + inputDir.getCanonicalPath() + " doesn't exists nor is a directory.");
					return;
				}
				
				File outputDir = new File(projectsDir, targetProjectName);
				
				// Copy project
				if (needCopy) {
					System.out.println(time() + "\tCopying from \""+ inputDir.getAbsolutePath() + "\" to \""+ outputDir.getAbsolutePath() +"\"");
					FileUtils.copyDirectory(inputDir, outputDir, new CustomDirectoryFilter());
					if (!outputDir.exists() || !outputDir.isDirectory()) {
						System.err.println("Directory " + outputDir.getCanonicalPath() + " doesn't exists nor is a directory.");
						return;
					}
				}
				
				initEngine();
				
				// Rename project
				if (needCopy) {
					System.out.println(time() + "\tRenaming project in target files");
					ProjectUtils.renameProjectFile(new File(outputDir, "c8oProject.yaml"), targetProjectName, false);
				}
				
				// Convert project
				System.out.println(time() + "\tConverting project");
				File out = new File(outputDir, "ngx-conversion-out.txt");
				File err = new File(outputDir, "ngx-conversion-errors.txt");
				System.setOut(new PrintStream(out));
				System.setErr(new PrintStream(err));
				NgxConverter ngxConverter = new NgxConverter(outputDir);
				ngxConverter.convertFile();
				
				System.setOut(stdout);
				System.setErr(stderr);
				System.out.println(time() + "\tConversion ended");
				System.out.println("See conversion results in following files:");
				System.out.println(" - " + out.getAbsolutePath());
				System.out.println(" - " + err.getAbsolutePath());
			}
		} catch (Throwable t) {
			System.setErr(stderr);
			t.printStackTrace();
		}
	}

}
