/*
 * Copyright (c) 2001-2019 Convertigo SA.
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
package com.twinsoft.convertigo.engine.mobile;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.commons.io.FileUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

public class TranslateUtils {
	public static class Translator {
		
		private Translator() {
			
		}
		
		public void translate(Locale from, File fromFile, Locale to, File toFile) throws EngineException {
			boolean failed = false;
			try {
				// load source file (from)
				JSONObject jsonObject = loadTranslations(fromFile);
				
				// translate using free google api
				JSONObject translations = new JSONObject();
				String value = null, key = null;
				try {
					@SuppressWarnings("unchecked")
					Iterator<String> it = jsonObject.keys();
					while (it.hasNext()) {
						key = it.next();
						value = jsonObject.getString(key);
						
						String url = "https://translate.googleapis.com/translate_a/single?"+
								"client=gtx&"+
								"sl=" + from.getLanguage() + 
								"&tl=" + to.getLanguage() + 
								"&dt=t&q=" + URLEncoder.encode(value, "UTF-8");    
						
						URL obj = new URL(url);
						HttpURLConnection con = (HttpURLConnection) obj.openConnection(); 
						con.setRequestProperty("User-Agent", "Mozilla/5.0");
						con.setRequestProperty("Accept-Charset", "UTF-8");	
						
						String encoding = con.getContentEncoding();
						if (encoding == null) {
							encoding = "UTF-8";
						}
						
						BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), encoding));
						StringBuffer response = new StringBuffer();
						String inputLine;
						while ((inputLine = in.readLine()) != null) {
							response.append(inputLine);
						}
						in.close();
					
						String translation = parseResult(response.toString());
						translations.put(key, translation);
					}
				} catch (Exception e) {
					failed = true;
				}
				
				// save file translated
				if (!failed) {
					if (!toFile.exists()) {
						saveTranslations(translations, toFile);
					} else {
						storeTranslations(translations, toFile);
					}
				}
				
			} catch (Exception e) {
				if (failed) {
					throw new EngineException("Unable to translate file through google api", e);
				} else {
					throw new EngineException("Unexpected error while translating file", e);
				}
			}
		}
		
		private String parseResult(String jsonResponse) throws Exception {
			//System.out.println(jsonResponse);
			String translation = "";
			JSONArray jsonArray1 = new JSONArray(jsonResponse);
			JSONArray jsonArray2 = jsonArray1.getJSONArray(0);
			int len = jsonArray2.length();
			for (int i= 0; i < len; i++) {
				JSONArray jsonArray = jsonArray2.getJSONArray(i);
				translation += jsonArray.getString(0);
			}
			return translation;
		}		
	}
	
	public static Translator newTranslator() {
		return new Translator();
	}
	
	private static boolean existTranslationFiles(Project project) {
		if (project != null) {
			File i18nDir = new File(project.getDirPath(), "DisplayObjects/mobile/assets/i18n");
			for (Locale locale: Locale.getAvailableLocales()) {
				File translations = new File(i18nDir, locale.getLanguage() + ".json");
				if (translations.exists()) {
					return true;
				}
			}
		}
		return false;
	}
	
	public static String htmlIonicTranslate(Project project, String text) {
		if (existTranslationFiles(project)) {
			String key = computeKey(text);
			if (key != null  && !key.isEmpty()) {
				return "{{ '" + key + "' | translate }}";
			}
		}
		return text;
	}
	
	public static String getComputedKey(Project project, String text) {
		if (existTranslationFiles(project)) {
			String key = computeKey(text);
			if (key != null  && !key.isEmpty()) {
				return key;
			}
		}
		return text;
	}
	
	private static String escape(String text) {
		String escaped = text;
		escaped = escaped.replaceAll("\\'", "\'");
		escaped = escaped.replaceAll("\\s", "_");
		return escaped;
	}
	
	private static String computeKey(String text) {
		if (text != null  && !text.isEmpty()) {
			String escaped = escape(text);
			return escaped.length() < 40 ? escaped : escaped.substring(0, 40);
		}
		return null;
	}

	public static void storeTranslations(List<String> textList, File file) throws EngineException {
		// create translation keys from texts
		JSONObject jsonObject = new JSONObject();
		String key = null;
		for (String text: textList) {
			key = computeKey(text);
			if (key != null) {
				try {
					if (jsonObject.has(key)) {
						Engine.logEngine.debug("(TranslateUtils) For text \""+text+"\" : key \""+key+"\" already exist with value \""+jsonObject.getString(key)+"\"");
					} else {
						jsonObject.put(key, text);
					}
				} catch (JSONException e) {
					Engine.logEngine.warn("(TranslateUtils) An exception occured", e);
				}
			}
		}
		
		// store translations to file
		storeTranslations(jsonObject, file);
	}
	
	private static void storeTranslations(JSONObject jsonObject, File file) throws EngineException {
		if (file == null || jsonObject == null) {
			throw new EngineException("Unable to store translations : invalid null argument");
		}
		
		// update translations : do not overwrite existing ones
		if (file.exists()) {
			JSONObject translations = loadTranslations(file);
			
			// return if no change
			if (jsonObject.toString().equals(translations.toString())) {
				return;
			}
			
			String translation = null, key = null;
			@SuppressWarnings("unchecked")
			Iterator<String> it = translations.keys();
			while (it.hasNext()) {
				key = it.next();
				try {
					translation = translations.getString(key);
					jsonObject.put(key, translation);
				} catch (JSONException e) {}
			}
		}
		
		// save translations to file
		saveTranslations(jsonObject, file);
	}
	
	public static void saveTranslations(JSONObject jsonObject, File file) throws EngineException {
		try {
			String content = jsonObject.toString(1);
			FileUtils.writeStringToFile(file, content, "UTF-8");
		} catch (Exception e) {
			throw new EngineException("Unable to save translations to file", e);
		}
	}
	
	public static JSONObject loadTranslations(File file) throws EngineException {
		try {
			String content = FileUtils.readFileToString(file, "UTF-8");
			return new JSONObject(content);
		} catch (Exception e) {
			throw new EngineException("Unable to load translations from file", e);
		}
	}
	
}
