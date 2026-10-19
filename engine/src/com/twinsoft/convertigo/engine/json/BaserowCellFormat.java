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


package com.twinsoft.convertigo.engine.json;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import com.twinsoft.convertigo.engine.EngineException;

/**
 * The "baserow" cell format of the native JSON steps: the cell of a Baserow value for a C8oForms data grid, as the
 * JavaScript of lib_BaseRow (formssource_GetTableData, js/utils.js) builds it. Same HTML, same values, same column
 * options for the lean cells, same aggregate cells: a native step must answer byte for byte what the JavaScript
 * answers.
 */
public class BaserowCellFormat {
	// the logger of the log object of the JavaScript (Engine.logUser)
	private static final Logger logUser = Logger.getLogger("cems.Context.User");

	public static final String BASE_HTML = "<div style='display:flex; white-space: normal; align-items:center; flex-wrap: wrap'>";
	public static final String CLOSE_HTML = "</div>";

	private static final Map<String, String> COLORS = new HashMap<String, String>();
	static {
		String[] colors = {
			"light-blue", "#f0f4fc", "light-green", "#ecfcf1", "light-cyan", "#cff5fa", "light-orange", "#fffbf0",
			"light-yellow", "#fffbf0", "light-red", "#fff2f0", "light-brown", "#f5e6dc", "light-purple", "#f9f1fd",
			"light-pink", "#f7e1f2", "light-gray", "#f5f5f5", "blue", "#dae4fd", "green", "#d0f6dc", "cyan", "#a0ebf5",
			"orange", "#fff4da", "yellow", "#ffe9b4", "red", "#ffdeda", "brown", "#f5ceb0", "purple", "#dfb9f7",
			"pink", "#f7b2e7", "gray", "#d7d8d9", "dark-blue", "#acc8f8", "dark-green", "#a0eeba", "dark-cyan", "#70e0ef",
			"dark-orange", "#ffe9b4", "dark-yellow", "#ffdd8f", "dark-red", "#ffbdb4", "dark-brown", "#f5c098",
			"dark-purple", "#cf96f2", "dark-pink", "#f285d9", "dark-gray", "#cdcecd", "darker-blue", "#689ef1",
			"darker-green", "#41dd75", "darker-cyan", "#41d6ea", "darker-orange", "#ffd269", "darker-yellow", "#ffd269",
			"darker-red", "#ff7b69", "darker-brown", "#f5a96e", "darker-purple", "#bf73ee", "darker-pink", "#ff6dde",
			"darker-gray", "#b5b5b7", "deep-dark-green", "#10bf4a", "deep-dark-orange", "#ffc744", "white", "#fff"
		};
		for (int i = 0; i < colors.length; i += 2) {
			COLORS.put(colors[i], colors[i + 1]);
		}
	}

	// \s of JavaScript
	private static final String JS_SPACE = "[\\t\\n\\u000B\\f\\r \\u00A0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000\\uFEFF]";
	private static final Pattern NUMBER = Pattern.compile("-?\\d+(\\.\\d+)?");
	private static final Pattern DATE_ONLY = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})");
	private static final Pattern ZONED = Pattern.compile("[zZ]\\z|[+-]\\d{2}:?\\d{2}\\z");
	private static final Pattern ZONE_WITHOUT_COLON = Pattern.compile("([+-]\\d{2})(\\d{2})\\z");
	private static final Pattern WITH_TIME = Pattern.compile("[T" + JS_SPACE.substring(1, JS_SPACE.length() - 1) + "]\\d{2}:\\d{2}");
	private static final Pattern URL_SCHEME = Pattern.compile("(https?://|mailto:|tel:)", Pattern.CASE_INSENSITIVE);
	private static final String[] FORMULA_FIELD_TYPES = { "formula", "count", "rollup", "lookup" };
	private static final String[] LEAN_OPTIONS = { "date_format", "date_time_format", "duration_format", "number_decimal_places", "date_include_time" };

	private final String defaultTimezone;
	private final Context cx;
	private final Scriptable scope;

	public BaserowCellFormat(String defaultTimezone, Context cx, Scriptable scope) {
		this.defaultTimezone = defaultTimezone;
		this.cx = cx;
		this.scope = scope;
	}

	private static Object get(Object obj, String key) {
		return JsonData.get(obj, key);
	}

	private static boolean nullish(Object value) {
		return JsonData.isNullish(value);
	}

	private static String str(Object value) {
		return JsonData.toStr(value);
	}

	private static boolean is(Object value, String text) {
		return value instanceof CharSequence && text.equals(value.toString());
	}

	/** JavaScript "a".replace("x", "y"): the first occurrence only */
	private static String replaceFirst(String text, String search, String replacement) {
		int index = text.indexOf(search);
		return index < 0 ? text : text.substring(0, index) + replacement + text.substring(index + search.length());
	}

	private static String pad(long value, int size) {
		String text = Long.toString(value);
		while (text.length() < size) {
			text = "0" + text;
		}
		return text;
	}

	private static String pad(String text, int size) {
		while (text.length() < size) {
			text = "0" + text;
		}
		return text;
	}

	public static String escape(Object value) {
		String text = nullish(value) ? "" : str(value);
		StringBuilder sb = null;
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			String replacement = c == '&' ? "&amp;" : c == '<' ? "&lt;" : c == '>' ? "&gt;" : c == '"' ? "&quot;" : null;
			if (replacement != null) {
				if (sb == null) {
					sb = new StringBuilder(text.length() + 16).append(text, 0, i);
				}
				sb.append(replacement);
			} else if (sb != null) {
				sb.append(c);
			}
		}
		return sb == null ? text : sb.toString();
	}

	static boolean isTrueValue(Object value) {
		return Boolean.TRUE.equals(value) || (value instanceof Number && ((Number) value).doubleValue() == 1)
				|| JsonData.looseEq(value, "1") || "true".equals(str(value).toLowerCase(Locale.ROOT));
	}

	private static String fieldOption(Object field, String name) {
		if (field == null) {
			return "";
		}
		Object value = get(field, name);
		return nullish(value) ? "" : str(value);
	}

	/** getResolvedFieldType: the type of a formula, count, rollup or lookup field is the type it gives */
	public static Object resolvedType(Object field) {
		if (field == null) {
			return null;
		}
		Object type = get(field, "type");
		for (String formulaType : FORMULA_FIELD_TYPES) {
			if (is(type, formulaType)) {
				Object resultType = get(field, "formula_type");
				return JsonData.looseEq(resultType, "array") ? get(field, "array_formula_type") : resultType;
			}
		}
		return type;
	}

	public String timezone(Object field) {
		String timezone = fieldOption(field, "date_force_timezone");
		if (timezone.isEmpty()) {
			timezone = fieldOption(field, "date_timezone");
		}
		if (timezone.isEmpty()) {
			timezone = fieldOption(field, "timezone");
		}
		if (timezone.isEmpty()) {
			timezone = fieldOption(field, "date_time_zone");
		}
		if (timezone.isEmpty()) {
			timezone = defaultTimezone;
		}
		timezone = JsonData.trim(timezone);
		if (timezone.isEmpty() || timezone.toLowerCase(Locale.ROOT).equals("null")) {
			timezone = "UTC";
		}
		return timezone;
	}

	private static Object primitiveValue(Object element) {
		if (element != null && JsonData.isObject(element) && !JsonData.isArray(element)) {
			Object value = get(element, "value");
			if (!nullish(value) && !JsonData.isObject(value)) {
				return value;
			}
			if (!nullish(get(element, "name")) && nullish(get(element, "is_image")) && nullish(get(element, "url"))) {
				return get(element, "name");
			}
		}
		return element;
	}

	private static boolean isNumberField(Object resolvedType, Object field) {
		Object type = field != null ? get(field, "type") : null;
		return is(resolvedType, "number") || is(resolvedType, "autonumber") || is(type, "number") || is(type, "autonumber") || is(type, "count");
	}

	private static boolean isDateField(Object resolvedType, Object field) {
		Object type = field != null ? get(field, "type") : null;
		return is(resolvedType, "date") || is(type, "date") || is(type, "created_on") || is(type, "last_modified");
	}

	/** formatBaserowCell: appends the HTML of the value to html and returns the value of the cell */
	public Object format(Object element, Object field, boolean nested, StringBuilder html) throws EngineException {
		if (JsonData.isArray(element)) {
			List<Object> items = JsonData.list(element);
			List<Object> values = new ArrayList<Object>(items.size());
			for (Object item : items) {
				values.add(format(item, field, true, html));
			}
			if (!nested) {
				html.append(CLOSE_HTML);
			}
			return values;
		}
		Object resolvedType = resolvedType(field);
		Object fieldType = field != null ? get(field, "type") : null;
		Object primitive = primitiveValue(element);
		if (is(resolvedType, "rating")) {
			html.append(ratingHtml(primitive));
		} else if (is(resolvedType, "boolean")) {
			html.append(booleanHtml(primitive));
		} else if (isNumberField(resolvedType, field)) {
			String text = escape(formatNumber(primitive, field));
			if (nested) {
				arrayHtml(html, text, "light-gray");
			} else {
				html.append(text);
			}
		} else if (is(resolvedType, "email") || is(fieldType, "email")) {
			html.append(emailHtml(primitive));
		} else if (is(resolvedType, "phone_number") || is(fieldType, "phone_number")) {
			html.append(phoneHtml(primitive));
		} else if (isDateField(resolvedType, field)) {
			String text = escape(formatDate(primitive, field));
			if (nested) {
				arrayHtml(html, text, "light-gray");
			} else {
				html.append(text);
			}
		} else if (is(resolvedType, "duration") || is(fieldType, "duration")) {
			String text = escape(formatDuration(primitive, field));
			if (nested) {
				arrayHtml(html, text, "light-gray");
			} else {
				html.append(text);
			}
		} else {
			return anyType(element, nested, resolvedType, html);
		}
		if (!nested) {
			html.append(CLOSE_HTML);
		}
		return primitive;
	}

	/** computeAnyType of js/utils.js */
	private Object anyType(Object element, boolean nested, Object type, StringBuilder html) throws EngineException {
		Object value;
		if (JsonData.isArray(element)) {
			List<Object> items = JsonData.list(element);
			List<Object> values = new ArrayList<Object>(items.size());
			for (Object item : items) {
				values.add(anyType(item, true, type, html));
			}
			value = values;
		} else if (JsonData.isObject(element)) {
			if (element == null) {
				throw new EngineException("TypeError: Cannot read property \"value\" from null");
			}
			Object elementValue = get(element, "value");
			if (!nullish(elementValue) && JsonData.isObject(elementValue)) {
				value = anyType(elementValue, true, type, html);
			} else if (!nullish(elementValue)) {
				if (is(type, "url")) {
					html.append(urlHtml(elementValue));
				} else {
					arrayHtml(html, str(elementValue), get(element, "color"));
				}
				value = element;
			} else if (!nullish(get(element, "name")) && nullish(get(element, "is_image")) && nullish(get(element, "url"))) {
				arrayHtml(html, str(get(element, "name")), get(element, "color"));
				value = element;
			} else if (!nullish(get(element, "url")) && !nullish(get(element, "label")) && nullish(get(element, "is_image"))) {
				html.append("<div style=\"background-color: ").append(COLORS.get("light-blue"))
					.append(";border-radius: 10px; padding-left: 10px; padding-top: 4px; padding-bottom: 4px; width: fit-content; padding-right: 10px; margin-top: 3px; cursor:pointer\">")
					.append("<a  href=\"").append(str(get(element, "url"))).append("\" target=\"_blank\">").append(str(get(element, "label"))).append("</a>")
					.append("</div>");
				value = element;
			} else if (!nullish(get(element, "is_image"))) {
				Object url = get(element, "url");
				if (JsonData.looseEq(get(element, "is_image"), Boolean.TRUE)) {
					html.append("<img style='width: 100%; margin-top: 5px; margin-bottom: 5px; border-radius: 3px; box-shadow: 0px 8px 9px -4px rgba(0,0,0,0.73);' src=\"")
						.append(str(url)).append("\" ></img>");
				} else {
					String visibleName = str(get(element, "visible_name"));
					html.append("<p style='background-color : lightgray;' ><a  href=\"").append(str(url))
						.append("\" target=\"_blank\" download=\"").append(visibleName).append("\">").append(visibleName).append("</a></p>");
				}
				value = element;
			} else {
				logUser.warn("type not handled");
				value = JsonData.UNDEFINED;
			}
		} else {
			html.append(is(type, "url") ? urlHtml(element) : str(element));
			value = element;
		}
		if (!nested) {
			html.append(CLOSE_HTML);
		}
		return value;
	}

	private static void arrayHtml(StringBuilder html, String value, Object color) {
		String background = ScriptRuntime.toBoolean(color) ? COLORS.get(str(color)) : null;
		html.append("<div style=\"background-color: ").append(background != null ? background : "lightgrey")
			.append(";border-radius: 10px; padding-left: 10px; padding-top: 4px; padding-bottom: 4px; width: fit-content; padding-right: 10px; margin-top: 3px;\">")
			.append(value).append("</div>");
	}

	private static String urlHtml(Object url) {
		String text = JsonData.trim(nullish(url) ? "" : str(url));
		if (text.isEmpty()) {
			return "";
		}
		String href = URL_SCHEME.matcher(text).lookingAt() ? text : "https://" + text;
		return "<a href=\"" + escape(href) + "\" target=\"_blank\" rel=\"noopener noreferrer\">" + escape(text) + "</a>";
	}

	private static String ratingHtml(Object value) {
		double rating = JsonData.parseInt(str(value));
		if (Double.isNaN(rating) || rating <= 0) {
			return "";
		}
		StringBuilder stars = new StringBuilder("<span style=\"color:#f59e0b;letter-spacing:1px\">");
		for (int i = 0; i < rating; i++) {
			stars.append("&#9733;");
		}
		return stars.append("</span>").toString();
	}

	private static String booleanHtml(Object value) {
		if (isTrueValue(value)) {
			return "<span style=\"display:inline-flex;width:16px;height:16px;border:1px solid #22c55e;background:#22c55e;border-radius:3px;color:#fff;align-items:center;justify-content:center;font-size:12px;line-height:16px\">&#10003;</span>";
		}
		return "<span style=\"display:inline-flex;width:16px;height:16px;border:1px solid #cbd5e1;background:#fff;border-radius:3px\"></span>";
	}

	private static String emailHtml(Object value) {
		String email = JsonData.trim(nullish(value) ? "" : str(value));
		if (email.isEmpty()) {
			return "";
		}
		return "<a href=\"mailto:" + escape(email) + "\">" + escape(email) + "</a>";
	}

	private static String phoneHtml(Object value) {
		String phone = JsonData.trim(nullish(value) ? "" : str(value));
		if (phone.isEmpty()) {
			return "";
		}
		StringBuilder href = new StringBuilder();
		for (int i = 0; i < phone.length(); i++) {
			char c = phone.charAt(i);
			if ((c >= '0' && c <= '9') || c == '+') {
				href.append(c);
			}
		}
		if (href.length() == 0) {
			return escape(phone);
		}
		return "<a href=\"tel:" + escape(href) + "\">" + escape(phone) + "</a>";
	}

	private static boolean isEmpty(Object value) {
		return nullish(value) || JsonData.looseEq(value, "");
	}

	private static String numberSource(Object value) {
		return value instanceof Number ? JsonData.numberToString(((Number) value).doubleValue()) : replaceFirst(str(value), ",", ".");
	}

	static String formatNumber(Object value, Object field) {
		if (isEmpty(value)) {
			return "";
		}
		String source = numberSource(value);
		if (!NUMBER.matcher(source).matches()) {
			return str(value);
		}
		double number = Double.parseDouble(source);
		double decimalPlaces = JsonData.parseInt(fieldOption(field, "number_decimal_places"));
		if (Double.isNaN(decimalPlaces)) {
			Object type = field != null ? get(field, "type") : null;
			if (is(type, "autonumber") || is(type, "count")) {
				decimalPlaces = 0;
			} else {
				return str(value);
			}
		}
		decimalPlaces = Math.min(Math.max(decimalPlaces, 0), 20);
		return JsonData.toFixed(number, (int) decimalPlaces);
	}

	private static String formatDuration(Object value, Object field) {
		if (isEmpty(value)) {
			return "";
		}
		String source = numberSource(value);
		if (!NUMBER.matcher(source).matches()) {
			return str(value);
		}
		double secondsValue = Double.parseDouble(source);
		String sign = secondsValue < 0 ? "-" : "";
		long totalMilliseconds = (long) JsonData.round(Math.abs(secondsValue) * 1000);
		long totalSeconds = totalMilliseconds / 1000;
		long milliseconds = totalMilliseconds % 1000;
		long days = totalSeconds / 86400;
		long hoursTotal = totalSeconds / 3600;
		long hours = (totalSeconds % 86400) / 3600;
		long minutes = (totalSeconds % 3600) / 60;
		long seconds = totalSeconds % 60;
		String format = fieldOption(field, "duration_format");
		if (format.isEmpty()) {
			format = "h:mm:ss";
		}
		if (format.equals("d h")) {
			return sign + days + "d " + hours + "h";
		}
		if (format.equals("d h m")) {
			return sign + days + "d " + hours + "h " + minutes + "m";
		}
		if (format.equals("d h m s")) {
			return sign + days + "d " + hours + "h " + minutes + "m " + seconds + "s";
		}
		int decimals = format.contains(".sss") ? 3 : (format.contains(".ss") ? 2 : (format.contains(".s") ? 1 : 0));
		String secondsText = pad(seconds, 2);
		if (decimals > 0) {
			secondsText += "." + pad(milliseconds, 3).substring(0, decimals);
		}
		if (format.startsWith("d h:mm")) {
			String dayClock = days + "d " + hours + ":" + pad(minutes, 2);
			if (format.contains(":ss")) {
				dayClock += ":" + secondsText;
			}
			return sign + dayClock;
		}
		String clock = hoursTotal + ":" + pad(minutes, 2);
		if (format.contains(":ss")) {
			clock += ":" + secondsText;
		}
		return sign + clock;
	}

	private static final class DateParts {
		String year, month, day;
		long hours, minutes;
	}

	private DateParts dateTimeParts(String rawDate, Object field) {
		try {
			String normalized = replaceFirst(rawDate, " ", "T");
			ZoneId zoneId = ZoneId.of(timezone(field));
			DateParts parts = new DateParts();
			if (ZONED.matcher(normalized).find()) {
				Matcher m = ZONE_WITHOUT_COLON.matcher(normalized);
				if (m.find()) {
					normalized = normalized.substring(0, m.start()) + m.group(1) + ":" + m.group(2);
				}
				ZonedDateTime zoned = OffsetDateTime.parse(normalized).toInstant().atZone(zoneId);
				parts.year = Integer.toString(zoned.getYear());
				parts.month = pad(zoned.getMonthValue(), 2);
				parts.day = pad(zoned.getDayOfMonth(), 2);
				parts.hours = zoned.getHour();
				parts.minutes = zoned.getMinute();
				return parts;
			}
			LocalDateTime local = LocalDateTime.parse(normalized);
			parts.year = Integer.toString(local.getYear());
			parts.month = pad(local.getMonthValue(), 2);
			parts.day = pad(local.getDayOfMonth(), 2);
			parts.hours = local.getHour();
			parts.minutes = local.getMinute();
			return parts;
		} catch (Exception e) {
			return null;
		}
	}

	private String formatDate(Object value, Object field) {
		if (isEmpty(value)) {
			return "";
		}
		String rawDate = str(value);
		DateParts parts;
		Matcher dateOnly = DATE_ONLY.matcher(rawDate);
		if (dateOnly.matches()) {
			parts = new DateParts();
			parts.year = dateOnly.group(1);
			parts.month = dateOnly.group(2);
			parts.day = dateOnly.group(3);
		} else {
			parts = dateTimeParts(rawDate, field);
		}
		if (parts == null) {
			// new Date(rawDate) of the JavaScript, in the time zone of the JVM
			Scriptable date = cx.newObject(scope, "Date", new Object[] { rawDate });
			double time = ScriptRuntime.toNumber(ScriptableObject.callMethod(cx, date, "getTime", new Object[0]));
			if (Double.isNaN(time)) {
				return rawDate;
			}
			parts = new DateParts();
			parts.year = str(ScriptableObject.callMethod(cx, date, "getFullYear", new Object[0]));
			parts.month = pad(str(ScriptRuntime.toNumber(ScriptableObject.callMethod(cx, date, "getMonth", new Object[0])) + 1), 2);
			parts.day = pad(str(ScriptableObject.callMethod(cx, date, "getDate", new Object[0])), 2);
			parts.hours = (long) ScriptRuntime.toNumber(ScriptableObject.callMethod(cx, date, "getHours", new Object[0]));
			parts.minutes = (long) ScriptRuntime.toNumber(ScriptableObject.callMethod(cx, date, "getMinutes", new Object[0]));
		}
		String dateFormat = fieldOption(field, "date_format").toUpperCase(Locale.ROOT);
		String text;
		if (dateFormat.equals("EU") || dateFormat.equals("EUROPEAN")) {
			text = parts.day + "/" + parts.month + "/" + parts.year;
		} else if (dateFormat.equals("US")) {
			text = parts.month + "/" + parts.day + "/" + parts.year;
		} else {
			text = parts.year + "-" + parts.month + "-" + parts.day;
		}
		Object includeTimeOption = field != null ? get(field, "date_include_time") : null;
		boolean includeTime = isTrueValue(includeTimeOption) || (nullish(includeTimeOption) && WITH_TIME.matcher(rawDate).find());
		if (includeTime) {
			String timeFormat = fieldOption(field, "date_time_format").toLowerCase(Locale.ROOT);
			if (timeFormat.equals("12") || timeFormat.contains("12")) {
				String suffix = parts.hours >= 12 ? "PM" : "AM";
				long hour12 = parts.hours % 12;
				if (hour12 == 0) {
					hour12 = 12;
				}
				text += " " + hour12 + ":" + pad(parts.minutes, 2) + " " + suffix;
			} else {
				text += " " + pad(parts.hours, 2) + ":" + pad(parts.minutes, 2);
			}
		}
		return text;
	}

	/** isLeanTypedField: the columns whose lean value is the primitive value of the cell */
	public static boolean isLeanTyped(Object field) {
		Object resolvedType = resolvedType(field);
		Object type = field != null ? get(field, "type") : null;
		for (String t : new String[] { "rating", "boolean", "number", "autonumber", "email", "phone_number", "date", "duration" }) {
			if (is(resolvedType, t)) {
				return true;
			}
		}
		for (String t : new String[] { "number", "autonumber", "count", "email", "phone_number", "date", "created_on", "last_modified", "duration" }) {
			if (is(type, t)) {
				return true;
			}
		}
		return false;
	}

	/** leanCellValue: the value formatBaserowCell returns, without its HTML */
	public static Object leanValue(Object element, boolean typed) {
		if (JsonData.isArray(element)) {
			List<Object> items = JsonData.list(element);
			List<Object> values = new ArrayList<Object>(items.size());
			for (Object item : items) {
				values.add(leanValue(item, typed));
			}
			return values;
		}
		return typed ? primitiveValue(element) : leanAnyValue(element);
	}

	private static Object leanAnyValue(Object element) {
		if (JsonData.isArray(element)) {
			List<Object> items = JsonData.list(element);
			List<Object> values = new ArrayList<Object>(items.size());
			for (Object item : items) {
				values.add(leanAnyValue(item));
			}
			return values;
		}
		if (element != null && JsonData.isObject(element)) {
			Object value = get(element, "value");
			if (!nullish(value) && JsonData.isObject(value)) {
				return leanAnyValue(value);
			}
			if (!nullish(value) || (!nullish(get(element, "name")) && nullish(get(element, "is_image")) && nullish(get(element, "url")))
					|| (!nullish(get(element, "url")) && !nullish(get(element, "label")) && nullish(get(element, "is_image")))
					|| !nullish(get(element, "is_image"))) {
				return element;
			}
			logUser.warn("type not handled");
			return JsonData.UNDEFINED;
		}
		return element;
	}

	/** leanFieldOptions: the options of a column the viewer needs to build the HTML of its cells */
	public Map<String, Object> leanFieldOptions(Object field) {
		Map<String, Object> options = new LinkedHashMap<String, Object>();
		options.put("type", str(get(field, "type")));
		Object resolvedType = resolvedType(field);
		if (!nullish(resolvedType)) {
			options.put("resolvedType", str(resolvedType));
		}
		for (String name : LEAN_OPTIONS) {
			Object option = get(field, name);
			if (!nullish(option)) {
				options.put(name, str(option));
			}
		}
		if (isDateField(resolvedType, field)) {
			options.put("timezone", timezone(field));
		}
		return options;
	}

	/** buildAggregateCell */
	public static Map<String, Object> aggregateCell(String type, Object key, Object column, Object label, Object field, Object value) {
		Object displayValue = nullish(value) ? "" : value;
		if ((type.equals("sum") || type.equals("avg")) && value instanceof Number && Double.isFinite(((Number) value).doubleValue())) {
			int decimalPlaces = aggregationDecimalPlaces(type, field);
			double rounded = roundAggregate(((Number) value).doubleValue(), decimalPlaces);
			value = rounded;
			displayValue = JsonData.toFixed(rounded, decimalPlaces);
		}
		Map<String, Object> cell = new LinkedHashMap<String, Object>(16);
		cell.put("displayName", key);
		cell.put("type", "html");
		cell.put("value", value);
		cell.put("displayValue", BASE_HTML + str(displayValue) + CLOSE_HTML);
		cell.put("cType", type.equals("min") || type.equals("max") ? "text" : "number");
		cell.put("filterCompletion", new ArrayList<Object>(0));
		cell.put("filterHide", Boolean.TRUE);
		cell.put("sortHide", Boolean.FALSE);
		cell.put("aggregationType", type);
		cell.put("aggregationColumn", column);
		if (!nullish(label) && !JsonData.looseEq(label, "")) {
			cell.put("label", label);
		}
		return cell;
	}

	private static int aggregationDecimalPlaces(String type, Object field) {
		double decimalPlaces = JsonData.parseInt(fieldOption(field, "number_decimal_places"));
		if (Double.isNaN(decimalPlaces)) {
			Object fieldType = field != null ? get(field, "type") : null;
			decimalPlaces = is(fieldType, "autonumber") || is(fieldType, "count") || is(fieldType, "rating") ? 0 : 2;
		}
		if (type.equals("avg") && decimalPlaces < 2) {
			decimalPlaces = 2;
		}
		return (int) Math.min(Math.max(decimalPlaces, 0), 10);
	}

	/** roundAggregateValue: half away from zero through the decimal exponent */
	private static double roundAggregate(double value, int decimalPlaces) {
		double abs = Math.abs(value);
		double rounded = JsonData.toNumber(JsonData.numberToString(JsonData.round(JsonData.toNumber(JsonData.numberToString(abs) + "e" + decimalPlaces))) + "e-" + decimalPlaces);
		if (Double.isNaN(rounded) || Double.isInfinite(rounded)) {
			rounded = JsonData.toNumber(JsonData.toFixed(abs, decimalPlaces));
		}
		return value < 0 ? -rounded : rounded;
	}
}
