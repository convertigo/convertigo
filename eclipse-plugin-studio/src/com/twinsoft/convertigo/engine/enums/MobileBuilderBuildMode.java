package com.twinsoft.convertigo.engine.enums;

public enum MobileBuilderBuildMode {
	fast("f", "ionic:serve:eval", "fast mode", "the fastest dev build mode but no source code debugging."),
	debug("d", "ionic:serve:nosourcemap", "debug mode", "slower dev build mode with javascript (not typescript) debugging."),
	debugplus("dp", "ionic:serve", "debug plus mode", "slowest dev build mode with the full typescript debugging (use external debugger Chrome to use sourcemap)."),
	production("p", "ionic:build:prod", "production mode", "long build time > 5 mins but automatically removes debug data, unusued code, shrinks and use code scrambler. The application will be smaller and start faster.");
	
	String label;
	String description;
	String icon;
	String command;

	MobileBuilderBuildMode(String code, String command, String label, String description) {
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
	
	public static MobileBuilderBuildMode get(String value) {
		try {
			return MobileBuilderBuildMode.valueOf(value);
		} catch (Exception e) {
			return MobileBuilderBuildMode.fast;
		}
	}
}
