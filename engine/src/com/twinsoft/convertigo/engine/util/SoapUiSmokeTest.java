package com.twinsoft.convertigo.engine.util;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.model.settings.Settings;

public final class SoapUiSmokeTest {
	private SoapUiSmokeTest() {
	}

	public static void main(String[] args) {
		try {
			Settings settings = SoapUI.getSettings();
			System.out.println("SoapUI settings loaded: " + (settings != null));
			new WsdlProject();
			System.out.println("SoapUI WsdlProject initialized.");
			System.exit(0);
		} catch (Throwable t) {
			t.printStackTrace(System.err);
			System.exit(1);
		}
	}
}
