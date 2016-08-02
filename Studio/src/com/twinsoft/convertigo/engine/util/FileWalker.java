package com.twinsoft.convertigo.engine.util;

import java.io.File;

public class FileWalker {

	public FileWalker() {
	}
	
	public void walk(File file) {
		if (file.isDirectory()) {
			for (File subfile : file.listFiles()) {
				walk(subfile);
			}
		}
	}
}
