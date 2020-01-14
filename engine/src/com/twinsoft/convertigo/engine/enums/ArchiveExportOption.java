/*
 * Copyright (c) 2001-2020 Convertigo SA.
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

package com.twinsoft.convertigo.engine.enums;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import com.twinsoft.convertigo.engine.util.FileUtils;

public enum ArchiveExportOption {
	includeTestCase("include Requestables TestCases", dir -> new File[] { new File(dir, "c8oProject.yaml"), new File(dir, "_c8oProject")}),
	includeStubs("include Stubs", dir -> new File[] { new File(dir, "stubs")}),
	includeMobileApp("include Mobile Builder build result", dir -> {
		File[] files = new File(dir, "DisplayObjects/mobile").listFiles((file) -> !file.getName().equals("assets"));
		return files;
	}),
	includeMobileAppAssets("include Mobile Builder assets", dir -> new File[] { new File(dir, "DisplayObjects/mobile/assets") }),
	includeMobileDataset("include Mobile Builder dataset", dir -> new File[] { new File(dir, "dataset") }),
	includeMobilePlatformsAssets("include Mobile Platforms assets", dir -> {
		File[] platforms = new File(dir, "DisplayObjects/platforms").listFiles();
		if (platforms == null) {
			return new File[0];
		}
		File[] res = new File[platforms.length];
		int i = 0;
		for (File platform: platforms) {
			res[i++] = new File(platform, "res");
		}
		return res;
	});
	
	String display;
	Filter filter;
	
	ArchiveExportOption(String display, Filter filter) {
		this.display = display;
		this.filter = filter;
	}
	
	public String display() {
		return display;
	}
	
	public long size(File dir) {
		long sum = 0;
		for (File d: dirs(dir)) {
			if (d.exists()) {
				sum += FileUtils.sizeOf(d);
			}
		}
		return sum;
	}
	
	public File[] dirs(File dir) {
		File[] res = filter.filter(dir);
		return res == null ? new File[]{} : res;
	}
	
	interface Filter {
		File[] filter(File dir);
	}
	
	public static Set<ArchiveExportOption> load(File dir) {
		Properties props = new Properties();
		Set<ArchiveExportOption> opts = new HashSet<ArchiveExportOption>(Arrays.asList(ArchiveExportOption.values()));
		try (FileInputStream fis = new FileInputStream(new File(dir, "_private/archiveExportOption.properties"))) {
			props.load(fis);
			opts.removeIf(o -> "false".equals(props.getProperty(o.name())));
		} catch (Exception e) {
		}
		return opts;
	}
	
	public static void save(File dir, Set<ArchiveExportOption> opts) {
		Properties props = new Properties();
		for (ArchiveExportOption o: ArchiveExportOption.values()) {
			props.setProperty(o.name(), Boolean.toString(opts.contains(o)));
		}
		try (FileOutputStream fos = new FileOutputStream(new File(dir, "_private/archiveExportOption.properties"))) {
			props.store(fos, "ArchiveExportOption preference");
		} catch (Exception e) {
		}
	}

	public static final Set<ArchiveExportOption> all = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(ArchiveExportOption.values())));
}
