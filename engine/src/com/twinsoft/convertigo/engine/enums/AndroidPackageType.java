package com.twinsoft.convertigo.engine.enums;

public enum AndroidPackageType {
	apk("apk"),
	bundle("aab");
	
	private String extension;
	
	AndroidPackageType(String extension) {
		this.extension = extension;
	}
	
	public String extension() {
        return extension;
    }
	
	@Override
	public String toString() {
		return name() + " (." + extension + ")";
	}
}