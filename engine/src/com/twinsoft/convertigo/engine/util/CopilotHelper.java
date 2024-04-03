package com.twinsoft.convertigo.engine.util;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IVariableContainer;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.core.Variable;
import com.twinsoft.convertigo.beans.couchdb.DesignDocument;
import com.twinsoft.convertigo.engine.Engine;

public class CopilotHelper {

	public static String addInstruction(DatabaseObject dbo, String code) {
		if (!Engine.isStudioMode()) {
			return code;
		}
		var found = false;
		try {
            var frameworkUtilClass = Class.forName("org.osgi.framework.FrameworkUtil");
            var convertigoPluginClass = Class.forName("com.twinsoft.convertigo.eclipse.ConvertigoPlugin");
            var bundle = frameworkUtilClass.getMethod("getBundle", Class.class).invoke(null, convertigoPluginClass);
            var bundleContext = bundle.getClass().getMethod("getBundleContext").invoke(bundle);
            var bundles = (Object[]) bundleContext.getClass().getMethod("getBundles").invoke(bundleContext);
            for (var b : bundles) {
                var symbolicName = b.getClass().getMethod("getSymbolicName").invoke(b);
                if ("com.genuitec.copilot4eclipse".equals(symbolicName)) {
                	found = true;
                	break;
                }
            }
        } catch (Exception e) {
        }
        if (!found) {
        	return code;
        }
		
        if (dbo instanceof DesignDocument) {
			code = "/* Copilot helper: this is a javascript script executed by a CouchDB view of a design document */\n" + code;
			return code;
        }
        
		var variables = ". Available variables: log, dom, context";
		if (dbo instanceof Step) {
			dbo = ((Step) dbo).getSequence();
		}
		if (dbo instanceof IVariableContainer vc) {
			for (Variable v: vc.getVariables()) {
				variables += ", " + v.getName();
			}
		}
		code = "/* Copilot helper: this is a javascript script executed with Rhino JS by a Convertigo " + (dbo instanceof Transaction ? "Transaction" : "Sequence ") + " over the JVM" + variables + "*/\n" + code;
		return code;
	}
	
	public static String removeInstruction(String code) {
		return code.replaceAll("/\\* Copilot helper:.*\n", "");
	}
}
