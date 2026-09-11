package com.twinsoft.convertigo.engine.admin.services;

import static org.junit.Assert.*;

import java.util.Arrays;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.Test;
import org.xml.sax.SAXException;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.util.XMLUtils;

/** Keeps the hotfix access restrictions and develop's Servlet API aligned. */
public class SecurityMergeContractTest {
	@Test
	public void projectWriteServicesRequireConfigurationAndUseJakarta() throws Exception {
		for (var name : new String[] {"dbo.Accept", "dbo.Add", "dbo.Move", "dbo.Paste",
				"dbo.Remove", "dbo.Rename", "dbo.Save", "ngxpicker.Apply", "properties.Set", "sourcepicker.Apply"}) {
			var service = Class.forName("com.twinsoft.convertigo.engine.admin.services.studio." + name);
			assertArrayEquals(name, new Role[] {Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG},
					service.getAnnotation(ServiceDefinition.class).roles());
			assertTrue(name, Arrays.stream(service.getDeclaredMethods()).anyMatch(method ->
					method.getName().equals("getServiceResult") && method.getParameterCount() == 2
					&& method.getParameterTypes()[0] == HttpServletRequest.class));
		}
		assertArrayEquals(new Role[] {Role.WEB_ADMIN, Role.PROJECTS_CONFIG},
				com.twinsoft.convertigo.engine.admin.services.projects.ImportURL.class
				.getAnnotation(ServiceDefinition.class).roles());
	}

	@Test
	public void untrustedXmlRejectsDoctypeWithoutStartingEngine() throws Exception {
		assertThrows(SAXException.class, () -> XMLUtils.parseDOMFromString(
				"<!DOCTYPE root [<!ENTITY unsafe 'value'>]><root>&unsafe;</root>"));
	}

	@Test
	public void plainNamespacedXmlStillParses() throws Exception {
		var document = XMLUtils.parseDOMFromString("<root xmlns='urn:merge-test'>safe &amp; valid</root>");
		assertEquals("urn:merge-test", document.getDocumentElement().getNamespaceURI());
		assertEquals("safe & valid", document.getDocumentElement().getTextContent());
	}
}
