package com.twinsoft.convertigo.engine.util;

import com.twinsoft.convertigo.beans.connectors.HttpConnector;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.references.RestServiceReference;
import com.twinsoft.convertigo.beans.references.WebServiceReference;

public class ImportWsReference extends WsReference {

		public ImportWsReference(WebServiceReference reference) {
			super(reference);
		}

		public ImportWsReference(RestServiceReference reference) {
			super(reference);
		}

		@Override
		public HttpConnector importInto(Project project) throws Exception {
			return super.importInto(project);
		}
}
