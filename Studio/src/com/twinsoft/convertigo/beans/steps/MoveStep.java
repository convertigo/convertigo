package com.twinsoft.convertigo.beans.steps;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public class MoveStep extends CopyStep {

	private static final long serialVersionUID = 2273726638385880897L;

	@Override
	protected String getAction() {
		return "move";
	}

	@Override
	protected void doActionForSourceFile(File sourceFile, File destinationFile) throws IOException {
		FileUtils.moveFile(sourceFile, destinationFile);
	}

	@Override
	protected void doActionForSourceDirectory(File sourceFile, File destinationFile) throws IOException {
		FileUtils.moveDirectory(sourceFile, destinationFile);
	}
}
