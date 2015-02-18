package com.twinsoft.convertigo.engine.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class Json {
	static public final Gson gson = new Gson();
	static public final Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
	
	public static JsonObject newJsonObject(String json) {
		return gson.fromJson(json, JsonObject.class);
	}
	
	public static JsonArray newJsonArray(String json) {
		return gson.fromJson(json, JsonArray.class);
	}
	
	public static boolean isEmpty(JsonObject object) {
		return object.entrySet().isEmpty();
	}
	
	public static void accumulate(JsonObject object, String key, String value) {
		accumulate(object, key, new JsonPrimitive(value));
	}
	
	public static void accumulate(JsonObject object, String key, JsonElement value) {
		if (object.has(key)) {
			JsonElement oldValue = object.get(key);
			
			if (oldValue.isJsonArray()) {
				oldValue.getAsJsonArray().add(value);;
			} else {
				JsonArray array = new JsonArray();
				array.add(oldValue);
				array.add(value);
				object.add(key, array);
			}
		} else {
			object.add(key, value);
		}
	}
	
	public static String toJson(Object object) {
		return gson.toJson(object);
	}
	
	public static String toPrettyJson(Object object) {
		return prettyGson.toJson(object);
	}
}
