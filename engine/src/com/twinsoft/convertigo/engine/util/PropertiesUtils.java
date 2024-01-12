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

package com.twinsoft.convertigo.engine.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Properties;

import com.twinsoft.convertigo.engine.Engine;

public class PropertiesUtils {
	
	public static Properties load(String filepath) throws IOException {
		try (FileInputStream fis = new FileInputStream(filepath)) {
			return load(fis);
		}
	}
	
	public static void load(Properties properties, String filepath) throws IOException {
		try (FileInputStream fis = new FileInputStream(filepath)) {
			load(properties, fis);
		}
	}
	
	public static Properties load(File file) throws IOException {
		try (FileInputStream fis = new FileInputStream(file)) {
			return load(fis);
		}
	}
	
	public static void load(Properties properties, File file) throws IOException {
		try (FileInputStream fis = new FileInputStream(file)) {
			load(properties, fis);
		}
	}
	
	public static Properties load(InputStream inputStream) throws IOException {
		Properties properties = new Properties();
		load(properties, inputStream);
		return properties;
	}
	
	public static void load(Properties properties, InputStream inputStream) throws IOException {
		if (inputStream != null) {
			try (Reader reader = new InputStreamReader(inputStream, "UTF-8")) {
				load(properties, reader);
			}
		}
	}
	
	public static void load(Properties properties, Reader reader) throws IOException {
		properties.load(reader);
	}
	
	public static void store(Properties properties, File file) throws IOException {
		store(properties, file, null);
	}
	
	public static void store(Properties properties, File file, String comments) throws IOException {
		Properties existing = new Properties();
		try {
			load(existing, file);
			if (existing.equals(properties)) {
				Engine.logEngine.debug("(PropertiesUtils) Same content, doesn't write properties to " + file);
				return;
			}
		} catch (Exception e) {
		}
		try (FileOutputStream fos = new FileOutputStream(file)) {
			store(properties, fos, comments);
		}
	}
	
	public static void store(Properties properties, String filepath) throws IOException {
		store(properties, filepath, null);
	}
	
	public static void store(Properties properties, String filepath, String comments) throws IOException {
		store(properties, new File(filepath), comments);
	}
	
	public static void store(Properties properties, OutputStream outputStream, String comments) throws IOException {
		try (Writer writer = new OutputStreamWriter(outputStream, "UTF-8")) {
			store(properties, writer, comments);
		}
	}
	
	public static void store(Properties properties, Writer writer, String comments) throws IOException {
		properties.store(writer, comments);
	}
}
