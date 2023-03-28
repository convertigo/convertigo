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

package com.twinsoft.convertigo.engine.enums;

public enum NgxBuilderBuildMode {
	prod("p", "ionic:build:prod", "production", "long build time > 5 mins but automatically removes debug data, unusued code, shrinks and use code scrambler. The application will be smaller and start faster."),
	fast("f", "ionic:build:fast", "fast", "fast build time."),
	watch("w", "ionic:build:watch", "watch", "fast build time and rebuild at each changes.");
	
	String label;
	String description;
	String icon;
	String command;

	NgxBuilderBuildMode(String code, String command, String label, String description) {
		this.label = label;
		this.description = description;
		this.command = command;
		icon = "/studio/build_prod_" + code + ".png";
	}
	
	public String label() {
		return label;
	}
	
	public String command() {
		return command;
	}
	
	public String description() {
		return description;
	}
	
	public String icon() {
		return icon;
	}
	
	public static NgxBuilderBuildMode get(String value) {
		try {
			return NgxBuilderBuildMode.valueOf(value);
		} catch (Exception e) {
			return NgxBuilderBuildMode.fast;
		}
	}
}
