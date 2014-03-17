package com.twinsoft.convertigo.engine.util;

import org.eclipse.core.runtime.IProgressMonitor;

import com.twinsoft.convertigo.beans.connectors.HttpConnector;
import com.twinsoft.convertigo.beans.core.Project;

public class ImportWsReference extends WsReference {
		IProgressMonitor monitor;

		public ImportWsReference(String wsdlURL, IProgressMonitor monitor) {
			super(wsdlURL);
		}

		@Override
		public void setTaskLabel(String text) {
			if (monitor != null) {
				monitor.setTaskName(text);
				monitor.worked(1);
			} else {
				super.setTaskLabel(text);
			}
		}

		@Override
		public HttpConnector importInto(Project project) throws Exception {
			return super.importInto(project);
		}
		
		@Override
		public HttpConnector importIntoAuthenticated(Project project, String login, String password) throws Exception {
			return super.importIntoAuthenticated(project, login, password);
		}
}
