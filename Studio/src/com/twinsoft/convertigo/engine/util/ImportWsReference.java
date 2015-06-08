package com.twinsoft.convertigo.engine.util;

import com.twinsoft.convertigo.beans.connectors.HttpConnector;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.references.WebServiceReference;

public class ImportWsReference extends WsReference {

		public ImportWsReference(String wsdlURL, boolean needAuthentication, String login, String password) {
			super(wsdlURL, needAuthentication, login, password);
		}

		public ImportWsReference(WebServiceReference reference) {
			super(reference);
		}

		@Override
		public void setTaskLabel(String text) {
			super.setTaskLabel(text);
		}

		@Override
		public HttpConnector importInto(Project project) throws Exception {
			return super.importInto(project);
		}
}
