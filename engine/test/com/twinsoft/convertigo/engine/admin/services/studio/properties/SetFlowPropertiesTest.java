package com.twinsoft.convertigo.engine.admin.services.studio.properties;

import static org.junit.Assert.*;
import java.lang.reflect.Proxy;
import java.util.Map;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import jakarta.servlet.http.HttpServletRequest;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.flow.FlowVirtualObject;

public class SetFlowPropertiesTest {
	private static class Target extends FlowVirtualObject {
		JSONObject pending;
		int mutations;
		@Override public boolean setDynamicProperty(String name, String value) {
			assertEquals("arbitraryProperty", name);
			mutations++;
			try {
				pending = new JSONObject().put("done", true).put("id", "Project.authoritative")
						.put("parentId", "Project.parent").put("projectedRootPath", "provider.root")
						.put("selectionVirtualPath", "provider.root.child")
						.put("revision", mutations);
			} catch (Exception e) { throw new AssertionError(e); }
			return true;
		}
		@Override public JSONObject consumeLastSourceMutationResult() {
			var result = pending;
			pending = null;
			return result;
		}
	}

	private static class Service extends Set {
		final Target target = new Target();
		int exports;
		@Override protected DatabaseObject resolveTarget(String id) { return target; }
		@Override protected void saveProject(Project project) { exports++; }
		JSONObject apply(String save, String props) throws Exception {
			var parameters = Map.of("id", "Project.before", "props", props, "save", save);
			var request = (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(),
					new Class<?>[] {HttpServletRequest.class}, (proxy, method, args) -> {
				if (method.getName().equals("getParameter")) return parameters.get(args[0]);
				throw new AssertionError(method.getName());
			});
			var response = new JSONObject();
			getServiceResult(request, response);
			return response;
		}
	}

	private static final String PROPS = "[{\"name\":\"arbitraryProperty\",\"value\":\"after\"}]";

	@Test public void draftForwardsAuthoritativeMutationWithoutExporting() throws Exception {
		var service = new Service();
		service.target.setVirtualKind("provider.futureKind");
		var response = service.apply("false", PROPS);
		assertTrue(response.getBoolean("done"));
		assertEquals("Project.before", response.getString("previousId"));
		assertEquals("Project.authoritative", response.getString("id"));
		assertEquals("Project.parent", response.getString("parentId"));
		assertEquals("provider.root", response.getString("projectedRootPath"));
		assertEquals("provider.root.child", response.getString("selectionVirtualPath"));
		assertEquals(0, service.exports);
		assertNull(service.target.pending);
	}

	@Test public void explicitPersistenceStillExportsTheProject() throws Exception {
		var service = new Service();
		service.apply("true", PROPS);
		assertEquals(1, service.exports);
	}

	@Test public void batchForwardsTheLatestProjectionAndTheOriginalIdentity() throws Exception {
		var service = new Service();
		var response = service.apply("false", PROPS.substring(0, PROPS.length()-1) + "," + PROPS.substring(1));
		assertEquals(2, response.getInt("revision"));
		assertEquals("Project.before", response.getString("previousId"));
		assertEquals(0, service.exports);
	}
}
