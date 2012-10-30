package com.twinsoft.convertigo.beans.references;

import java.io.File;

import com.twinsoft.convertigo.engine.Engine;

public class ImportLocalXsdReference extends AbstractImportLocalXsdReference {
	private static final long serialVersionUID = -3972926767184718646L;
	
	@Override
	protected File getXsdFile() {
		return new File(Engine.theApp.filePropertyManager.getFilepathFromProperty(getFilepath(), getProject().getName()));
	}
}
