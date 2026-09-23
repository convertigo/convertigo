package com.twinsoft.convertigo.engine.admin.services.studio.dbo;

import static org.junit.Assert.*;
import java.lang.reflect.Proxy;
import java.util.Map;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import jakarta.servlet.http.HttpServletRequest;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.flow.FlowVirtualObject;

public class FlowRenameServiceTest {
	@Test public void webRenameUsesProviderCapabilityWithoutKnowingTheKind() throws Exception {
		var target = new FlowVirtualObject() {
			@Override public boolean isDefinitionWritable() { return true; }
		};
		target.setVirtualKind("provider.future-kind");
		target.setVirtualInfo("{\"renameMutation\":{\"op\":\"provider.rename\"}}");
		var parameters = Map.of("id", "project.old", "name", "after", "update", "UPDATE_NONE");
		var request = (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(),
				new Class<?>[] {HttpServletRequest.class}, (proxy, method, args) -> {
			if (method.getName().equals("getParameter")) return parameters.get(args[0]);
			throw new AssertionError(method.getName());
		});
		var response = new JSONObject();
		var service = new Rename() {
			@Override protected DatabaseObject resolveTarget(String id) { return target; }
			@Override protected JSONObject renameVirtual(FlowVirtualObject object, String name) throws Exception {
				assertSame(target, object);
				assertEquals("after", name);
				return new JSONObject().put("done", true).put("id", "project.authoring_after")
						.put("parentId", "project").put("projectedRootPath", "nodes")
						.put("selectionVirtualPath", "nodes[0]");
			}
		};
		service.getServiceResult(request, response);
		assertTrue(response.getBoolean("done"));
		assertEquals("project.authoring_after", response.getString("id"));
		assertEquals(response.getString("id"), response.getJSONArray("ids").getString(0));
		assertEquals("nodes[0]", response.getString("selectionVirtualPath"));
		assertEquals("nodes", response.getString("projectedRootPath"));
		// A virtual wrapper without the capability must not fall through to native
		// DBO rename, which would only rename the projection, not its source draft.
		target.setVirtualInfo("{}");
		assertThrows(com.twinsoft.convertigo.engine.EngineException.class,
				() -> service.getServiceResult(request, new JSONObject()));
	}
}
