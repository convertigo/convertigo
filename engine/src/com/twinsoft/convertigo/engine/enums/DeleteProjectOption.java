package com.twinsoft.convertigo.engine.enums;

import org.apache.commons.lang3.ArrayUtils;

public enum DeleteProjectOption {
	createBackup,
	dataOnly,
	preserveEclipse,
	preserveVCS;
	
	public boolean as(DeleteProjectOption... options) {
		return ArrayUtils.contains(options, this);
	}
}
