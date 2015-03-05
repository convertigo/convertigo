package com.twinsoft.convertigo.engine.enums;


public enum StoreFiles {
	css, 
	fonts,
	i18n,
	images,
	scripts,
	index("index.html");
	
	public final static String STORE_DIRECTORY_NAME = "store";
	String filename;
	
	StoreFiles() {
		filename = name();
	}
	
	StoreFiles(String filename) {
		this.filename = filename;
	}
	
	public String filename() {
		return filename;
	}
	
	static public int size() {
		return values().length;
	}
}
