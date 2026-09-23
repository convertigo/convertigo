package com.twinsoft.convertigo.eclipse.views.projectexplorer;

import java.util.HashSet;
import org.codehaus.jettison.json.JSONObject;
import com.twinsoft.convertigo.beans.flow.FlowVirtualObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.FlowVirtualObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;

/** Headless proof of wrapper retention; no SWT display or running Studio required. */
public class FlowTreeReconciliationTest {
	static class Wrapper extends FlowVirtualObjectTreeObject {
		Wrapper(FlowVirtualObject object) { super(null, object, false); }
		@Override public void setParent(TreeParent parent) { this.parent = parent; }
		@Override public TreeParent getParent() { return parent; }
		@Override public void hasBeenModified(boolean modified) { }
	}

	static Wrapper wrap(FlowVirtualObject bean) throws Exception {
		var wrapper = new Wrapper(bean);
		for (var child : bean.getDatabaseObjectChildren()) {
			wrapper.addChild(wrap((FlowVirtualObject) child));
		}
		return wrapper;
	}

	static FlowVirtualObject projection(String name) throws Exception {
		var root = new FlowVirtualObject();
		root.setName("Flow");
		root.setVirtualPath("nodes");
		root.replaceProjectedTree(new JSONObject("{name:'Flow',path:'nodes',children:["
				+ "{name:'" + name + "',path:'nodes[0]',kind:'provider.any',definition:'{\"id\":\"" + name + "\"}',children:["
				+ "{name:'nested',path:'nodes[0].children',children:[{name:'leaf',path:'nodes[0].children[0]'}]}]},"
				+ "{name:'eqOther',path:'nodes[1]'}]}"));
		return root;
	}

	public static void main(String[] args) throws Exception {
		var root = wrap(projection("eq"));
		var node = (Wrapper) root.getChildren().get(0);
		var nested = (Wrapper) node.getChildren().get(0);
		var leaf = nested.getChildren().get(0);
		var sibling = root.getChildren().get(1);
		var after = projection("equi");
		var id = ((FlowVirtualObject) after.getDatabaseObjectChildren().get(0)).getFullQName();
		var aliases = FlowTreeMutationReconciler.renamedSubtree(root, node.getObject().getFullQName(), id);
		if (aliases.size() != 3) throw new AssertionError("Only renamed subtree must be mapped");
		root.replaceFlowObject(after);
		var created = new HashSet<TreeObject>();
		FlowTreeMutationReconciler.reconcileProjectedChildren(root, after.getDatabaseObjectChildren(), created, aliases);
		if (!created.isEmpty() || root.getChildren().get(0) != node || node.getChildren().get(0) != nested
				|| nested.getChildren().get(0) != leaf || root.getChildren().get(1) != sibling) {
			throw new AssertionError("Rename replaced existing UI wrappers");
		}
		if (!"equi".equals(node.getObject().getName())) throw new AssertionError("Projection not replaced");
		node.getObject().setVirtualKind("node");
		node.getObject().setVirtualInfo("{\"propertyDefinitions\":{"
				+ "\"$$id\":{\"label\":\"Name\",\"category\":\"Information\",\"definitionPath\":\"id\",\"readOnly\":true},"
				+ "\"$$comment\":{\"label\":\"Comment\",\"category\":\"Base properties\",\"definitionPath\":\"comment\",\"type\":\"string\"}}}");
		node.replaceFlowObject(node.getObject());
		var nameCount = 0;
		var commentCount = 0;
		for (var descriptor : node.getPropertyDescriptors()) {
			if ("Comment".equals(descriptor.getDisplayName())) commentCount++;
			if ("Information".equals(descriptor.getCategory()) && "Name".equals(descriptor.getDisplayName())) {
				nameCount++;
				if (!"#flow_property:$$id".equals(descriptor.getId())) throw new AssertionError("Unexpected Name id: " + descriptor.getId());
			}
			if ("#flow_property:id".equals(descriptor.getId())) throw new AssertionError("Raw identity alias exposed");
		}
		if (nameCount != 1) throw new AssertionError("Expected one projected Name, got " + nameCount);
		if (commentCount != 1) throw new AssertionError("Expected one projected Comment, got " + commentCount);
		var updated = new FlowVirtualObject();
		updated.setVirtualKind("node");
		updated.setVirtualInfo(node.getObject().getVirtualInfo());
		updated.setDefinition("{\"id\":\"renamed\",\"comment\":\"Human note\"}");
		node.replaceFlowObject(updated);
		if (!"renamed".equals(node.getPropertyValue("#flow_property:$$id"))
				|| !"Human note".equals(node.getPropertyValue("#flow_property:$$comment"))) {
			throw new AssertionError("Retained property source did not reread the fresh projection");
		}
		System.out.println("FlowTreeReconciliationTest OK: renamed subtree and sibling wrappers preserved");
	}
}
