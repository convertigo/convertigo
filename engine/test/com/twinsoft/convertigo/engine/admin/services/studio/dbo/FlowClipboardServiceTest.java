package com.twinsoft.convertigo.engine.admin.services.studio.dbo;

import static org.junit.Assert.*;
import java.lang.reflect.Proxy;
import java.util.Map;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import jakarta.servlet.http.HttpServletRequest;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.flow.FlowEngine;
import com.twinsoft.convertigo.beans.flow.FlowVirtualObject;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.engine.flow.FlowStudioSupport;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class FlowClipboardServiceTest {
	private String copied() throws Exception {
		var document = XMLUtils.getDefaultDocumentBuilder().newDocument();
		var root = document.createElement("convertigo");
		root.setAttribute("clipboard", "copy");
		document.appendChild(root);
		var object = new FlowVirtualObject();
		object.setVirtualPath("config.example");
		object.setDefinition("{\"text\":\"<test> & é\"}");
		DboUtils.xmlCopy(document, object);
		assertEquals("flow-virtual-clipboard", root.getFirstChild().getNodeName());
		assertTrue(FlowStudioSupport.isVirtualClipboard(root.getFirstChild().getTextContent()));
		return XMLUtils.prettyPrintDOM(document);
	}

	private JSONObject request(Paste service, String xml) throws Exception {
		var parameters = Map.of("target", "example", "xml", xml);
		var request = (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(),
				new Class<?>[] {HttpServletRequest.class}, (proxy, method, args) -> {
			if (method.getName().equals("getParameter")) return parameters.get(args[0]);
			throw new AssertionError("Unexpected request access: " + method.getName());
		});
		var result = new JSONObject();
		service.getServiceResult(request, result);
		return result;
	}

	@Test public void webTransportDelegatesToVirtualCommandAndRetainsSelection() throws Exception {
		var calls = new int[1];
		var target = new FlowEngine();
		var result = request(new Paste() {
			@Override protected DatabaseObject resolveTarget(String id) { return target; }
			@Override protected JSONObject pasteVirtual(DatabaseObject destination, String payload) throws Exception {
				calls[0]++;
				assertSame(target, destination);
				assertEquals("<test> & é", new JSONObject(payload).getJSONObject("value").getString("text"));
				return new JSONObject().put("done", true).put("id", "project.config.copy")
						.put("parentId", "project.config").put("selectionVirtualPath", "config.copy")
						.put("projectedRootPath", "config");
			}
		}, copied());
		assertEquals(1, calls[0]);
		assertTrue(result.getBoolean("done"));
		assertEquals("project.config.copy", result.getJSONArray("ids").getString(0));
		assertEquals("config.copy", result.getString("selectionVirtualPath"));
		assertEquals("project.config", result.getString("parentId"));
		assertEquals("config", result.getString("projectedRootPath"));
	}

	@Test public void refusedVirtualPasteDoesNotReportSuccess() throws Exception {
		var result = request(new Paste() {
			@Override protected DatabaseObject resolveTarget(String id) { return new FlowEngine(); }
			@Override protected JSONObject pasteVirtual(DatabaseObject target, String payload) throws Exception {
				return new JSONObject().put("done", false).put("error", "Incompatible slot");
			}
		}, copied());
		assertFalse(result.getBoolean("done"));
		assertFalse(result.getBoolean("partial"));
		assertEquals(0, result.getJSONArray("ids").length());
		assertTrue(result.getString("error").contains("Incompatible slot"));
	}

	@Test public void invalidEnvelopeNeverReachesMutation() throws Exception {
		var service = new Paste() {
			@Override protected JSONObject pasteVirtual(DatabaseObject target, String payload) {
				throw new AssertionError("Malformed input reached mutation");
			}
		};
		assertThrows(Exception.class, () -> service.pasteInto(new FlowEngine(),
				"<convertigo clipboard=\"copy\"><flow-virtual-clipboard>{}</flow-virtual-clipboard></convertigo>", new JSONObject()));
	}

	@Test public void nativeCopyKeepsTheDatabaseObjectXmlProtocol() throws Exception {
		var document = XMLUtils.getDefaultDocumentBuilder().newDocument();
		document.appendChild(document.createElement("convertigo"));
		DboUtils.xmlCopy(document, new RequestableVariable());
		var element = (org.w3c.dom.Element) document.getDocumentElement().getFirstChild();
		assertEquals(RequestableVariable.class.getName(), element.getAttribute("classname"));
		assertNotEquals("flow-virtual-clipboard", element.getNodeName());
	}
}
