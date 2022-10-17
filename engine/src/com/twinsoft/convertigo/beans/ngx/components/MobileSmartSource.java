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

package com.twinsoft.convertigo.beans.ngx.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.connectors.FullSyncConnector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.couchdb.DesignDocument;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.InvalidSourceException;
import com.twinsoft.convertigo.engine.helpers.WalkHelper;
import com.twinsoft.convertigo.engine.util.GenericUtils;

//	{
//		"filter": "",
//		"project": "",
//		"input": "",	// old input string for cems < 7.9.0
//		"model": {
//			"data": [{}],
//			"path": "",
//			"prefix": "",
//			"suffix": ""
//		}
//	}

public class MobileSmartSource {

	public static Pattern listenPattern = Pattern.compile("listen\\(\\['(.*)'\\]\\)(.+)?");
	public static Pattern directivePattern = Pattern.compile("(item\\d+)(.+)?");
	public static Pattern formPattern = Pattern.compile("(form\\d+)(.+)?");
	public static Pattern cafPattern = Pattern.compile("'([^,]+)(,.+)?'");
	public static Pattern globalPattern = Pattern.compile("(router\\.sharedObject)(.+)?");
	public static Pattern actionPattern = Pattern.compile("(stack\\[['\"]\\d+['\"]\\])(.+)?");
	//public static Pattern sharedPattern = Pattern.compile("(params\\d+)(.+)?");
	public static Pattern sharedPattern = Pattern.compile("(comp\\d+)(.+)?");

	public static Pattern fsPattern = Pattern.compile("fs\\://(\\w+\\.)?(\\w+)\\.(\\w+)(#\\w+)?,?\\s*(\\{[^\\{\\}]*\\})?");
	public static Pattern kvPattern = Pattern.compile("(\\w+)(?:=|\\:)('[^']+'|\\w+)");
	
	public static String keyThis = "_this_";
	public static String keyThat = "_that_";
	
	static int findMatchingBracket(String input, int index) { 
		char c = input.charAt(index);
		char d = (c == '[' ? ']' : (c == '(' ? ')' : (c == '{' ? '}' : ' ')));
		if (d != ' ') {
			Stack<Integer> st = new Stack<>(); 
			for (int i = index; i < input.length(); i++) { 
				if (input.charAt(i) == c) { 
					st.push((int) input.charAt(i)); 
				} else if (input.charAt(i) == d) { 
					st.pop(); 
					if (st.empty()) { 
						return i; 
					} 
				} 
			} 

		} 
		return -1;
	} 
	
	public enum Filter {
		Action(ActionData.class),
		Shared(SharedData.class),
		Sequence(SequenceData.class),
		Database(DatabaseData.class),
		Iteration(IterationData.class),
		Form(FormData.class),
		Global(GlobalData.class),
		Local(LocalData.class),
		Icon(IconData.class),
		Asset(AssetData.class);
		
		Class<? extends SourceData> cls;
		Filter(Class<? extends SourceData> cls) {
			this.cls = cls;
		}
		
		public SourceData toSourceData(String project, String source) {
			if (project != null && !project.isEmpty()) {
				if (source != null && !source.isEmpty()) {
					try {
						return cls.getConstructor(String.class, String.class).newInstance(project, source);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			return null;
		}
		
		public SourceData toSourceData(JSONObject jsonObject) {
			if (jsonObject != null) {
				try {
					return cls.getConstructor(JSONObject.class).newInstance(jsonObject);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return null;
		}
		
		public SourceModel toSourceModel(String project, String input, String value) {
			SourceModel sm = new MobileSmartSource().new SourceModel(this);
			try {
				String prefix = "", suffix = "", path = "";
				
				String keyword = keyword();
				if (keyword != null) {
					if (input != null && !input.isEmpty() && !input.startsWith(keyword)) {
						int i = input.indexOf(keyword);
						prefix = input.substring(0, i);
						int j = findMatchingBracket(input, i-1);
						suffix = j == -1 ? "": input.substring(j);
						
						input = j == -1 ? input.substring(i) : input.substring(i, j);
					}
				}
				
				Matcher m = matcher(input);
				if (m.find()) {
					String group1 = m.group(1);
					String group2 = m.group(2);
					
					if (group1 != null) {
						for (String source : Arrays.asList(group1.split("'\\s*,\\s*'"))) {
							sm.addSourceData(project, source);
						}
					}
					if (group2 != null) {
						int k = group2.indexOf(' ');
						if (k == -1) {
							path = group2;
						} else {
							suffix = group2.substring(k) + suffix;
							path = group2.substring(0, k);
						}
					}

					sm.setPath(path);
					sm.setPrefix(prefix);
					sm.setSuffix(suffix);
				}
			} catch (Exception e) {
				//System.out.println("input: "+ input);
				//e.printStackTrace();
			}
			
			String computedValue = sm.computeValue();
			boolean useCustom = !computedValue.equals(value);
			if (useCustom) {
				//System.out.println("1:"+ computedValue);
				//System.out.println("2:"+ value);
				sm.setUseCustom(true);
				sm.setCustom(value);
			}
			return sm;
		}
		
		public String keyword() {
			if (this == Filter.Action) {
				return "stack";
			} else if (this == Filter.Shared) {
				return "comp";//"params";
			} else if (GenericUtils.contains(this, Filter.Sequence, Filter.Database)) {
				return "listen";
			} else if (this == Filter.Iteration) {
				return "item";
			} else if (this == Filter.Form) {
				return "form";
			} else if (this == Filter.Global) {
				return "router";
			}
			return null;
		}
		
		public Matcher matcher(String input) {
			if (input != null) {
				if (this == Filter.Action) {
					return actionPattern.matcher(input);
				} else if (this == Filter.Shared) {
					return sharedPattern.matcher(input);
				} else if (GenericUtils.contains(this, Filter.Sequence, Filter.Database)) {
					return listenPattern.matcher(input);
				} else if (this == Filter.Iteration) {
					return directivePattern.matcher(input);
				} else if (this == Filter.Form) {
					return formPattern.matcher(input);
				} else if (this == Filter.Global) {
					return globalPattern.matcher(input);
				}
			}
			return null;
		}
	}
	
	public enum Key {
		filter,
		project,
		input,
		model;
	}
	
	public class SourceModel {
		List<SourceData> data = new ArrayList<SourceData>();
		String prefix = "", suffix = "", path = "", custom = "";
		boolean useCustom = false;
		Filter filter;
		
		public SourceModel(Filter filter) {
			this.filter = filter;
		}
		
		public SourceModel(Filter filter, JSONObject jsonModel) {
			super();
			this.filter = filter;
			try {
				if (jsonModel.has("prefix")) {
					setPrefix(jsonModel.getString("prefix"));
				}
				if (jsonModel.has("suffix")) {
					setSuffix(jsonModel.getString("suffix"));
				}
				if (jsonModel.has("path")) {
					setPath(jsonModel.getString("path"));
				}
				if (jsonModel.has("custom")) {
					setCustom(jsonModel.getString("custom"));
				}
				if (jsonModel.has("useCustom")) {
					setUseCustom(jsonModel.getBoolean("useCustom"));
				}
				if (jsonModel.has("data")) {
					JSONArray jsonArray = jsonModel.getJSONArray("data");
					for (int i = 0; i < jsonArray.length(); i++) {
						try {
							JSONObject jsonSourceData = (JSONObject) jsonArray.get(i);
							addSourceData(filter.cls.getConstructor(JSONObject.class).newInstance(jsonSourceData));
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		public List<SourceData> getSourceData() {
			return data;
		}
		
		public void setSourceData(List<SourceData> data) {
			this.data = data;
		}

		public void addSourceData(SourceData sd) {
			if (sd != null) {
				data.add(sd);
			}
		}
		
		public void addSourceData(String project, String source) {
			if (filter != null) {
				addSourceData(filter.toSourceData(project, source));
			}
		}
		
		public String getPath() {
			return path;
		}
		
		public void setPath(String path) {
			if (path != null) {
				this.path = path;
			}
		}

		public String getPrefix() {
			return prefix;
		}

		public void setPrefix(String prefix) {
			if (prefix != null) {
				this.prefix = prefix;
			}
		}

		public String getSuffix() {
			return suffix;
		}

		public void setSuffix(String suffix) {
			if (suffix != null) {
				this.suffix = suffix;
			}
		}

		public String getCustom() {
			return custom;
		}

		public void setCustom(String custom) {
			if (custom != null) {
				this.custom = custom;
			}
		}
		
		public boolean getUseCustom() {
			return useCustom;
		}
		
		public void setUseCustom(boolean useCustom) {
			this.useCustom = useCustom;
		}
		
		public List<String> getSources() {
			List<String> sources = new ArrayList<String>();
			for (SourceData sd: data) {
				String source = sd.getSource();
				if (source != null && !source.isEmpty()) {
					sources.add(source);
				}
			}
			return sources;
		}

		public String getData() {
			boolean isCafListen = GenericUtils.contains(this, Filter.Sequence, Filter.Database);
			String buf = "";
			
			for (SourceData sd: data) {
				String sdv = sd.getSource();
				if (sdv != null && !sdv.isEmpty()) {
					buf += (buf.isEmpty() ? "":",") + sdv;
					if (!isCafListen) {
						break;
					}
				}
			}
			
			return buf.isEmpty() ? "" : (isCafListen ? "listen(["+ buf +"])" : buf) + path;
		}
		
		// compute value without 'this' keyword
		public String computeValue() {
			boolean isCafListen = GenericUtils.contains(filter, Filter.Sequence, Filter.Database);
			String buf = "";
			
			for (SourceData sd: data) {
				String sdv = sd.getValue();
				if (sdv != null && !sdv.isEmpty()) {
					buf += (buf.isEmpty() ? "":",") + sdv;
					if (!isCafListen) {
						break;
					}
				}
			}
			
			return buf.isEmpty() ? "" : prefix + (isCafListen ? "listen(["+ buf +"])" : buf) + path + suffix;
		}
		
		// compute value with 'this' keyword
		public String computeValueEx() {
			boolean isCafListen = GenericUtils.contains(filter, Filter.Sequence, Filter.Database);
			String buf = "";
			
			for (SourceData sd: data) {
				String sdv = sd.getValueEx();
				if (sdv != null && !sdv.isEmpty()) {
					buf += (buf.isEmpty() ? "":",") + sdv;
					if (!isCafListen) {
						break;
					}
				}
			}
			
			return buf.isEmpty() ? "" : prefix + (isCafListen ? "this.listen(["+ buf +"])" : buf) + path + suffix;
		}
		
		public String getValue(boolean extended) {
			if (useCustom) {
				return custom;
			} else {
				return extended ? computeValueEx() : computeValue();
			}
		}
		
		public JSONObject toJson() {
			JSONObject jsonModel = new JSONObject();
			try {
				JSONArray jsonArray = new JSONArray();
				for (SourceData sd: data) {
					jsonArray.put(sd.toJson());
				}
				jsonModel.put("data", jsonArray);
				jsonModel.put("path", path);
				jsonModel.put("prefix", prefix);
				jsonModel.put("suffix", suffix);
				jsonModel.put("custom", custom);
				jsonModel.put("useCustom", useCustom);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return jsonModel;
		}
	}
	
	static abstract public class SourceData {
		SourceData() {
			
		}

		SourceData(JSONObject jsonObject) {
			
		}

		SourceData(String project, String source) {
			
		}
		
		public abstract JSONObject toJson();
		public abstract String getValue();
		public abstract String getValueEx();
		public abstract String getSource();
	}
	
	static public class SequenceData extends SourceData {
		private String sequence = ""; // qname
		private String marker = "";
		
		public SequenceData(JSONObject jsonObject) {
			super(jsonObject);
			try {
				if (jsonObject.has("sequence")) {
					sequence = jsonObject.getString("sequence");
				}
				if (jsonObject.has("marker")) {
					marker = jsonObject.getString("marker");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public SequenceData(String project, String source) {
			super();
			try {
				//project.sequence#marker
				if (source != null && !source.isEmpty()) {
					if (source.indexOf(".") == -1) {
						source = project + "." + source;
					}
					int index = source.indexOf("#");
					sequence = index < 0 ? source : source.substring(0, index);
					marker = index < 0 ? "" : source.substring(index+1);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public JSONObject toJson() {
			JSONObject jsonObject = new JSONObject();
			try {
				jsonObject.put("sequence", sequence);
				jsonObject.put("marker", marker);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return jsonObject;
		}

		@Override
		public String getValue() {
			return getSource();
		}

		@Override
		public String getValueEx() {
			return getValue();
		}
		
		@Override
		public String getSource() {
			String source = null;
			if (!sequence.isEmpty()) {
				source = "'"+ sequence + (!marker.isEmpty() ? "#":"") + marker + "'";
			}
			return source;
		}

	}
	
	static public class DatabaseData extends SourceData {
		private boolean includeDocs = false;
		private String connector = "";	// qname
		private String document = "";  	// qname
		private String queryview = "";
		private String marker = "";
		private String verb = "get";
		
		public DatabaseData(JSONObject jsonObject) {
			super(jsonObject);
			try {
				if (jsonObject.has("includeDocs")) {
					includeDocs = jsonObject.getBoolean("includeDocs");
				}
				if (jsonObject.has("connector")) {
					connector = jsonObject.getString("connector");
				}
				if (jsonObject.has("document")) {
					document = jsonObject.getString("document");
				}
				if (jsonObject.has("queryview")) {
					queryview = jsonObject.getString("queryview");
				}
				if (jsonObject.has("verb")) {
					verb = jsonObject.getString("verb");
				}
				if (jsonObject.has("marker")) {
					marker = jsonObject.getString("marker");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public DatabaseData(String project, String source) {
			super();
			try {
				//'fs://project.connector.verb#marker, {ddoc='ddoc', view='view', include_docs='false'}'
				if (source != null && !source.isEmpty()) {
					Matcher mfs = fsPattern.matcher(source);
					if (mfs.find()) {
						String group1 = mfs.group(1); // project
						String group2 = mfs.group(2); // connector
						String group3 = mfs.group(3); // verb
						String group4 = mfs.group(4); // #marker
						String group5 = mfs.group(5); // {ddoc='ddoc', view='view', include_docs=false}
						
						String p = group1 != null ? group1.replaceFirst("\\.", "") : "";
						p = p.isEmpty() ? project : p;
						
						connector = p + "." + (group2 == null || group2.isEmpty() ? "null" : group2);
						verb =  (group3 == null || group3.isEmpty() ? "get" : group3);
						marker = (group4 == null || group4.isEmpty() ? "" : group4.substring(1));
						
						String ddoc = null, view = null, include_docs = null;
						if (group5 != null && !group5.isEmpty()) {
							Matcher mkv = kvPattern.matcher(group5);
							while (mkv.find()) {
								String key = mkv.group(1);
								String val = mkv.group(2);
								
								if (key != null && !key.isEmpty()) {
									val = val == null ? "": val;
									if (!val.isEmpty()) {
										char c = val.charAt(0);
										if ((c == '\'' || c == '"') && val.startsWith(""+c) && val.endsWith(""+c)) {
											val = val.substring(1, val.lastIndexOf(c));
										}
									}
									
									if (key.equals("ddoc")) {
										ddoc = val;
									} else if (key.equals("view")) {
										view = val;
									} else if (key.equals("include_docs")) {
										include_docs = val;
									}
								}
							}
						}
						document = connector + "." + (ddoc == null || ddoc.isEmpty() ? "null": ddoc);
						queryview = (view == null || view.isEmpty() ? "": view);
						includeDocs = Boolean.valueOf((include_docs == null || include_docs.isEmpty() ? "false": include_docs));
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public JSONObject toJson() {
			JSONObject jsonObject = new JSONObject();
			try {
				jsonObject.put("includeDocs", includeDocs);
				jsonObject.put("connector", connector);
				jsonObject.put("document", document);
				jsonObject.put("queryview", queryview);
				jsonObject.put("verb", verb);
				jsonObject.put("marker", marker);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return jsonObject;
		}

		@Override
		public String getValue() {
			String value = null;
			if (!connector.isEmpty()) {
				int i = 0;
				String database = connector.substring((i = connector.lastIndexOf(".")) < 0 ? 0 : i+1);
				value = "'fs://"+ database + "." + verb + (!marker.isEmpty() ? "#":"") + marker + "'";
			}
			return value;
		}

		@Override
		public String getValueEx() {
			return getValue();
		}
		
		@Override
		public String getSource() {
			String source = null;
			if (!connector.isEmpty()) {
				int i = -1;
				String ddoc = document.substring((i = document.lastIndexOf(".")) < 0 ? 0 : i+1);
				String view = queryview;
				source = "'fs://"+ connector + "." + verb + (!marker.isEmpty() ? "#":"") + marker 
						+ ", {ddoc='"+ ddoc +"', view='"+ view +"', include_docs='"+ includeDocs +"'}" + "'";
			}
			return source;
		}
	}
	
	static public class ActionData extends SourceData {
		private long priority = 0L;
		
		public ActionData(JSONObject jsonObject) {
			super(jsonObject);
			try {
				if (jsonObject.has("priority")) {
					priority = jsonObject.getLong("priority");
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		public ActionData(String project, String source) {
			super();
			try {
				if (source != null && !source.isEmpty()) {
					//stack['123456789'] or stack["123456789"]
					if (source.matches("stack\\[['\"]\\d+['\"]\\]")) {
						String p = source.replaceFirst("stack\\[", "").replaceFirst("\\]", "");
						p = p.substring(1, p.length() - 1); // ignore quotes
						priority = Long.parseLong(p);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public JSONObject toJson() {
			JSONObject jsonObject = new JSONObject();
			try {
				jsonObject.put("priority", priority);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return jsonObject;
		}
		
		@Override
		public String getValue() {
			return getSource();
		}

		@Override
		public String getValueEx() {
			return getValue();
		}
		
		@Override
		public String getSource() {
			String source = null;
			if (priority != 0L) {
				source = "stack['"+ priority + "']";
			}
			return source;
		}
	}
	
	static public class SharedData extends SourceData {
		private long priority = 0L;
		private boolean regular = true;
		
		public SharedData(JSONObject jsonObject) {
			super(jsonObject);
			try {
				if (jsonObject.has("priority")) {
					priority = jsonObject.getLong("priority");
				}
				if (jsonObject.has("regular")) {
					regular = jsonObject.getBoolean("regular");
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		public SharedData(String project, String source) {
			super();
			try {
				if (source != null && !source.isEmpty()) {
					//itemXXXXXXX
					if (source.matches("params\\d+")) {
						priority = Long.parseLong(source.replaceFirst("params", ""));
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public JSONObject toJson() {
			JSONObject jsonObject = new JSONObject();
			try {
				jsonObject.put("priority", priority);
				jsonObject.put("regular", regular);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return jsonObject;
		}
		
		@Override
		public String getValue() {
			String value = null;
			if (priority != 0L) {
				if (regular) {
					value = "this";
				} else {
					value = "params"+ priority;
				}
			}
			return value;
		}

		@Override
		public String getValueEx() {
			String valueEx = null;
			if (priority != 0L) {
				if (regular) {
					valueEx = "this";
				} else {
					valueEx = keyThis + ".params"+ priority;
				}
			}
			return valueEx;
		}
		
		@Override
		public String getSource() {
			String source = null;
			if (priority != 0L) {
				source = "comp"+ priority;
			}
			return source;
		}
	}
	
	static public class IterationData extends SourceData {
		private long priority = 0L;
		
		public IterationData(JSONObject jsonObject) {
			super(jsonObject);
			try {
				if (jsonObject.has("priority")) {
					priority = jsonObject.getLong("priority");
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		public IterationData(String project, String source) {
			super();
			try {
				if (source != null && !source.isEmpty()) {
					//itemXXXXXXX
					if (source.matches("item\\d+")) {
						priority = Long.parseLong(source.replaceFirst("item", ""));
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public JSONObject toJson() {
			JSONObject jsonObject = new JSONObject();
			try {
				jsonObject.put("priority", priority);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return jsonObject;
		}
		
		@Override
		public String getValue() {
			return getSource();
		}

		@Override
		public String getValueEx() {
			String valueEx = null;
			if (priority != 0L) {
				valueEx = keyThis + ".item"+ priority;
			}
			return valueEx;
		}
		
		@Override
		public String getSource() {
			String source = null;
			if (priority != 0L) {
				source = "item"+ priority;
			}
			return source;
		}
	}
	
	static public class FormData extends SourceData {
		private long priority = 0L;
		private String identifier = "";
		
		public FormData(JSONObject jsonObject) {
			super(jsonObject);
			try {
				if (jsonObject.has("priority")) {
					priority = jsonObject.getLong("priority");
				}
				if (jsonObject.has("identifier")) {
					identifier = jsonObject.getString("identifier");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		public FormData(String project, String source) {
			super();
			try {
				if (source != null && !source.isEmpty()) {
					//formXXXXXXX
					if (source.matches("form\\d+")) {
						priority = Long.parseLong(source.replaceFirst("form", ""));
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public JSONObject toJson() {
			JSONObject jsonObject = new JSONObject();
			try {
				jsonObject.put("priority", priority);
				jsonObject.put("identifier", identifier);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return jsonObject;
		}
		
		@Override
		public String getValue() {
			String value = null;
			if (!identifier.isBlank()) {
				value = ""+ identifier;
			}
			return value;
		}

		@Override
		public String getValueEx() {
			String valueEx = null;
			String value = getValue();
			if (value != null) {
				valueEx = keyThis + "." + value;
			}
			return valueEx;
		}
		
		@Override
		public String getSource() {
			String source = null;
			if (priority != 0L) {
				source = "form"+ priority;
			}
			return source;
		}
	}
	
	static public class GlobalData extends SourceData {
		private String sharedObject = "router.sharedObject";
		
		public GlobalData(JSONObject jsonObject) {
			super(jsonObject);
			try {
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public GlobalData(String project, String source) {
			super();
			try {
				//router.sharedObject
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public JSONObject toJson() {
			JSONObject jsonObject = new JSONObject();
			try {
				jsonObject.put("sharedObject", sharedObject);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return jsonObject;
		}

		@Override
		public String getValue() {
			return getSource();
		}

		@Override
		public String getValueEx() {
			return keyThis + "."+ sharedObject;
		}
		
		@Override
		public String getSource() {
			return sharedObject;
		}
		
	}
	
	static public class LocalData extends SourceData {
		private String localObject = "local";
		
		public LocalData(JSONObject jsonObject) {
			super(jsonObject);
			try {
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public LocalData(String project, String source) {
			super();
			try {
				;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public JSONObject toJson() {
			JSONObject jsonObject = new JSONObject();
			try {
				jsonObject.put("localObject", localObject);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return jsonObject;
		}

		@Override
		public String getValue() {
			return getSource();
		}

		@Override
		public String getValueEx() {
			return keyThis + "."+ localObject;
		}
		
		@Override
		public String getSource() {
			return localObject;
		}
		
	}
	
	static public class IconData extends SourceData {
		private String icon;
		
		public IconData(JSONObject jsonObject) {
			super(jsonObject);
			try {
				icon = jsonObject.getString("icon");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public IconData(String project, String source) {
			super();
			try {
				icon = source;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public JSONObject toJson() {
			JSONObject jsonObject = new JSONObject();
			try {
				jsonObject.put("icon", icon);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return jsonObject;
		}

		@Override
		public String getValue() {
			return getSource();
		}

		@Override
		public String getValueEx() {
			return icon;
		}
		
		@Override
		public String getSource() {
			return icon;
		}
		
	}

	static public class AssetData extends SourceData {
		private String asset;
		
		public AssetData(JSONObject jsonObject) {
			super(jsonObject);
			try {
				asset = jsonObject.getString("asset");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public AssetData(String project, String source) {
			super();
			try {
				asset = source;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public JSONObject toJson() {
			JSONObject jsonObject = new JSONObject();
			try {
				jsonObject.put("asset", asset);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return jsonObject;
		}

		@Override
		public String getValue() {
			return getSource();
		}

		@Override
		public String getValueEx() {
			return asset;
		}
		
		@Override
		public String getSource() {
			return asset;
		}
		
	}
	
	private JSONObject jsonObject = new JSONObject();
	
	private MobileSmartSource() {
		
	}
	
	public MobileSmartSource(Filter filter, String projectName, String input) {
		try {
			jsonObject.put(Key.filter.name(), filter.name());
			jsonObject.put(Key.project.name(), projectName);
			jsonObject.put(Key.input.name(), input);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public MobileSmartSource(Filter filter, String projectName, String input, JSONObject model) {
		try {
			jsonObject.put(Key.filter.name(), filter.name());
			jsonObject.put(Key.project.name(), projectName);
			jsonObject.put(Key.input.name(), input);
			jsonObject.put(Key.model.name(), model);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	protected MobileSmartSource(String jsonString) {
		try {
			jsonObject = new JSONObject(jsonString);
		} catch (Exception e) {
		}
	}
	
	public Filter getFilter() {
		try {
			return Filter.valueOf(jsonObject.getString(Key.filter.name()));
		} catch (JSONException e) {
			//e.printStackTrace();
		}
		return null;
	}

	public String getProjectName() {
		try {
			return jsonObject.getString(Key.project.name());
		} catch (JSONException e) {
			//e.printStackTrace();
		}
		return "";
	}

	public void setProjectName(String newName) {
		try {
			jsonObject.put(Key.project.name(), newName);
		} catch (JSONException e) {
			//e.printStackTrace();
		}
	}

	public String getInput() {
		try {
			String input = jsonObject.getString(Key.input.name());
			return input;
		} catch (JSONException e) {
			//e.printStackTrace();
		}
		return "";
	}

	public SourceModel getModel() {
		try {
			if (hasModel()) {
				Filter filter = Filter.valueOf(jsonObject.getString(Key.filter.name()));
				JSONObject jsonModel = jsonObject.getJSONObject(Key.model.name());
				return new SourceModel(filter, jsonModel);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public String getValue(boolean extended) {
		String value = null;
		try {
			if (hasModel()) {
				value = getModel().getValue(extended);
			} else {
				value = getInput();
				
				// remove parameters for caf listen
				int i = -1, j = -1;
				while ((i = value.indexOf(", {")) > 0) {
					if ((j = value.indexOf("}", i)) > 0) {
						value = value.substring(0, i) + value.substring(j+1);
					} else break;
				}
				
				// remove project names for fullsync
				if (Filter.Database.equals(getFilter())) {
					int z = 0;
					while ((i = value.indexOf("'fs://", z)) > 0) {
						z = i+("'fs://").length();
						if ((j = value.indexOf("'", z)) > 0) {
							String requestable = value.substring(z, j);
							String[] ar = requestable.split("\\.");
							if (ar.length > 2) {
								String projectName = ar[0];
								value = value.replaceFirst("fs://"+projectName+"\\.", "fs://");
							}
						}
					}
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return value == null ? "" : value;
	}
	
	public String toJsonString() {
		try {
			return jsonObject.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}
	
	public String toJsonString(int indentFactor) {
		try {
			return jsonObject.toString(indentFactor);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public List<String> getSources() {
		if (hasModel()) {
			return getModel().getSources();
		} else {
			return getSources(getInput());
		}
	}
	
	public List<String> getSources(String s) {
		List<String> sources = new ArrayList<String>();
		if (hasModel()) {
			try {
				SourceModel sm = new SourceModel(getFilter(), new JSONObject(s));
				sources.addAll(sm.getSources());
			} catch (JSONException e) {
				e.printStackTrace();
			}
		} else {
			String input = s;
			if (Filter.Action.equals(getFilter())) {
				Matcher m = actionPattern.matcher(input);
				if (m.find()) {
					String stack = m.group(1);
					if (stack != null) {
						sources.add(stack);
					}
				}
			} else if (Filter.Shared.equals(getFilter())) {
				Matcher m = sharedPattern.matcher(input);
				if (m.find()) {
					String shared = m.group(1);
					if (shared != null) {
						sources.add(shared);
					}
				}
			} else if (Filter.Iteration.equals(getFilter())) {
				Matcher m = directivePattern.matcher(input);
				if (m.find()) {
					String directive = m.group(1);
					if (directive != null) {
						sources.add(directive);
					}
				}
			} else if (Filter.Form.equals(getFilter())) {
				Matcher m = formPattern.matcher(input);
				if (m.find()) {
					String form = m.group(1);
					if (form != null) {
						sources.add(form);
					}
				}
			} else if (Filter.Global.equals(getFilter())) {
				Matcher m = globalPattern.matcher(input);
				if (m.find()) {
					String gbl = m.group(1);
					if (gbl != null) {
						sources.add(gbl);
					}
				}
			} else {
				Matcher m = listenPattern.matcher(input);
				if (m.find()) {
					String array = m.group(1);
					if (array != null) {
						List<String> items = Arrays.asList(array.split("'\\s*,\\s*'"));
						for (String item : items) {
							String source = "'"+item.replaceFirst("#[^,]*,,", "")+"'";
							if (Filter.Database.equals(getFilter())) {
								// fix for missing include_docs
								if (source.indexOf("include_docs") == -1) {
									source = source.replaceFirst("\\}", ", include_docs='false'}");
								}
							}
							sources.add(source);
						}
					}
				}
			}
		}
		return sources;
	}
	
	public boolean hasModel() {
		return this.jsonObject.has(Key.model.name());
	}

	public String getModelPath() {
		String modelPath = null;
		if (hasModel()) {
			try {
				modelPath = getModel().getPath();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			if (Filter.Action.equals(getFilter())) {
				Matcher m = actionPattern.matcher(getInput());
				if (m.find()) {
					modelPath = m.group(2);
				}
			} else if (Filter.Shared.equals(getFilter())) {
				Matcher m = sharedPattern.matcher(getInput());
				if (m.find()) {
					modelPath = m.group(2);
				}
			} else if (Filter.Iteration.equals(getFilter())) {
				Matcher m = directivePattern.matcher(getInput());
				if (m.find()) {
					modelPath = m.group(2);
				}
			} else if (Filter.Form.equals(getFilter())) {
				Matcher m = formPattern.matcher(getInput());
				if (m.find()) {
					modelPath = m.group(2);
				}
			} else if (Filter.Global.equals(getFilter())) {
				Matcher m = globalPattern.matcher(getInput());
				if (m.find()) {
					modelPath = m.group(2);
				}
			} else {
				Matcher m = listenPattern.matcher(getInput());
				if (m.find()) {
					modelPath = m.group(2);
				}
			}
		}
		return modelPath == null ? "" : modelPath;
	}
	
	public static SourceModel emptyModel(Filter filter) {
		return new MobileSmartSource().new SourceModel(filter);
	}
	
	public static MobileSmartSource valueOf(String jsonString) {
		if (jsonString != null && !jsonString.isEmpty()) {
			MobileSmartSource mss = new MobileSmartSource(jsonString);
			try {
				if (!mss.hasModel()) {
					return migrate(jsonString);
				}
			} catch (Exception e) {
				//e.printStackTrace();
			}
			return mss;
		}
		return null;
	}
	
	public static MobileSmartSource migrate(String jsonString) throws Exception {
		if (jsonString == null || jsonString.isEmpty() || jsonString.equals("{}")) {
			throw new InvalidSourceException("Invalid null or empty source. Please check your project.");
		}
		
		MobileSmartSource mss = new MobileSmartSource(jsonString);
		
		// Migrate source : add a source model (since 7.9.0)
		if (!mss.hasModel()) {
			String project = mss.getProjectName();
			Filter filter = mss.getFilter();
			String input = mss.getInput();
			
			if (filter == null) {
				throw new InvalidSourceException("Missing source's filter. Please check your project.");
			}
			if (project == null || project.isEmpty()) {
				throw new InvalidSourceException("Missing source's project. Please check your project.");
			}
			if (input == null || input.isEmpty()) {
				throw new InvalidSourceException("Missing source's input. Please check your project.");
			}
			
			String value = mss.getValue(false);
			SourceModel sm = filter.toSourceModel(project, input, value);
			if (sm != null) {
				MobileSmartSource mssn = new MobileSmartSource(filter, project, input, sm.toJson());
				//System.out.println("Old source with input '"+ input + "' migrated to :");
				//System.out.println(new JSONObject(mssn.toJsonString()).toString(1));
				return mssn;
			}
		}
		
		// Nothing to migrate
		return mss;
	}
	
	private DatabaseObject findDatabaseObject(final String dboName, final long priority) throws Exception {
		Project project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(getProjectName());
		
		DatabaseObject root = null;
		if (dboName != null) {
			ApplicationComponent app = (ApplicationComponent)project.getMobileApplication().getApplicationComponent();
			try {
				root = app.getPageComponentByName(dboName);
			} catch (Exception e1) {
				try {
					root = app.getMenuComponentByName(dboName);
				} catch (Exception e2) {
					try {
						root = app;
					} catch (Exception e3) {
						;
					}
				}
			}
		}
		
		if (root == null) {
			root = project;
		}

		final List<DatabaseObject> list = new ArrayList<DatabaseObject>();
		new WalkHelper() {
			@Override
			protected void walk(DatabaseObject databaseObject) throws Exception {
				if (databaseObject.priority == priority) {
					list.add(databaseObject);
				}
				if (list.isEmpty()) {
					super.walk(databaseObject);
				}
			}
		}.init(root);
		
		return list.isEmpty() ? null:list.get(0);
	}
	
	public boolean isDroppableInto(DatabaseObject targetDbo) {
		DatabaseObject dbo = getDatabaseObject();
		if (dbo == null) {
			return false;
		}
		if (targetDbo == null) {
			return false;
		}
		
		boolean isDroppable = true;
		String qname = dbo.getQName() + ".";
		if (GenericUtils.contains(getFilter(), Filter.Action, Filter.Shared, Filter.Iteration, Filter.Icon, Filter.Asset)) {
			isDroppable = targetDbo.getQName().startsWith(qname);
		}
		return isDroppable;
	}
	
	public DatabaseObject getDatabaseObject() {
		return getDatabaseObject(null);
	}
	
	public DatabaseObject getDatabaseObject(String rootDboName) {
		List<String> sourceData = getSources();
		String sourceInput = sourceData.size() > 0 ? sourceData.get(0):null;
		if (sourceInput != null) {
			if (Filter.Action.equals(getFilter())) {
				Matcher m = actionPattern.matcher(sourceInput);
				if (m.find()) {
					String stack = m.group(1);
					try {
						String p = stack.replaceFirst("stack\\[", "").replaceFirst("\\]", "");
						p = p.substring(1, p.length()-1); // ignore quotes
						long priority = Long.valueOf(p, 10);
						return findDatabaseObject(rootDboName, priority);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} else if (Filter.Shared.equals(getFilter())) {
				Matcher m = sharedPattern.matcher(sourceInput);
				if (m.find()) {
					String shared = m.group(1);
					try {
						long priority = Long.valueOf(shared.replaceFirst("comp", ""), 10);
						return findDatabaseObject(rootDboName, priority);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} else if (Filter.Iteration.equals(getFilter())) {
				Matcher m = directivePattern.matcher(sourceInput);
				if (m.find()) {
					String item = m.group(1);
					try {
						long priority = Long.valueOf(item.replaceFirst("item", ""), 10);
						return findDatabaseObject(rootDboName, priority);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} else if (Filter.Form.equals(getFilter())) {
				Matcher m = formPattern.matcher(sourceInput);
				if (m.find()) {
					String form = m.group(1);
					try {
						long priority = Long.valueOf(form.replaceFirst("form", ""), 10);
						return findDatabaseObject(rootDboName, priority);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} else if (Filter.Global.equals(getFilter())) {
				Matcher m = globalPattern.matcher(sourceInput);
				if (m.find()) {
					try {
						String projectName = getProjectName();
						Project project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName);
						
						DatabaseObject dbo = (ApplicationComponent)project.getMobileApplication().getApplicationComponent();
						return dbo;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

			} else if (Filter.Database.equals(getFilter())) {
				Matcher m = cafPattern.matcher(sourceInput);
				if (m.find()) {
					try {
						String name = m.group(1);
						name = name.replaceFirst("fs://", "");
						name = name.replaceFirst("\\.view", "");
						name = name.replaceFirst("\\.get", "");
						int i = name.indexOf('#');
						if (i != -1) {
							name = name.substring(0, i);
						}
						
						int index = name.indexOf('.');
						String projectName = index != -1 ? name.substring(0, index) : getProjectName();
						String dbName = index != -1 ? name.substring(index+1) : name;
						
						Project project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName);
						FullSyncConnector connector = (FullSyncConnector) project.getConnectorByName(dbName);
						String documentName = getParameters().get("ddoc");
						DesignDocument ddoc = (DesignDocument) connector.getDocumentByName(documentName);
						return ddoc;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} else if (Filter.Sequence.equals(getFilter())) {
				Matcher m = cafPattern.matcher(sourceInput);
				if (m.find()) {
					try {
						String name = m.group(1);
						int i = name.indexOf('#');
						if (i != -1) {
							name = name.substring(0, i);
						}
						
						int index = name.indexOf('.');
						String projectName = index != -1 ? name.substring(0, index) : getProjectName();
						projectName = projectName.isEmpty() ? getProjectName(): projectName;
						String sequenceName = index != -1 ? name.substring(index+1) : name;
						
						Project project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName);
						Sequence sequence = project.getSequenceByName(sequenceName);
						return sequence;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} else if (GenericUtils.contains(getFilter(), Filter.Icon, Filter.Asset)) {
				try {
					String projectName = getProjectName();
					Project project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName);
					
					DatabaseObject dbo = (ApplicationComponent)project.getMobileApplication().getApplicationComponent();
					return dbo;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	public Map<String, String> getParameters() {
		Map<String, String> map = new HashMap<String, String>();
		List<String> sourceData = getSources();
		String source = sourceData.size() > 0 ? sourceData.get(0):null;
		if (source != null) {
			if (Filter.Action.equals(getFilter())) {
				;
			} else if (Filter.Shared.equals(getFilter())) {
				;
			} else if (Filter.Iteration.equals(getFilter())) {
				;
			} else if (Filter.Form.equals(getFilter())) {
				;
			} else if (Filter.Global.equals(getFilter())) {
				;
			} else {
				Matcher m = cafPattern.matcher(source);
				if (m.find()) {
					String parameters = m.group(2);
					if (parameters != null) {
						parameters = parameters.replaceFirst("\\{", "");
						parameters = parameters.replaceFirst("\\}", "");
						String[] params = parameters.split(",");
						for (int i=0; i< params.length; i++) {
							String param = params[i];
							if (param.indexOf('=') != -1) {
								String[] values = param.split("=");
								String key = values[0].trim();
								if (!key.isEmpty()) {
									String value = values[1].trim();
									if (value.startsWith("'") && value.endsWith("'")) {
										value = value.substring(1, value.length()-1);
									}
									map.put(key, value);
								}
							}
						}
					}
				}
			}
		}
		return map;
	}
	
}
