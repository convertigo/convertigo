/* Copyright (c) 2001-2026 Convertigo SA. Licensed under the GNU AGPL v3. */
package com.twinsoft.convertigo.eclipse.views.palette;

import java.lang.reflect.Method;

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.ngx.components.UIText;
import com.twinsoft.convertigo.beans.ngx.components.dynamic.Component;
import com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;

/** Filtering the production NGX item must not evaluate insertion hints. */
public final class PaletteComponentFilterTest {
	public static void main(String[] args) throws Exception {
		var component = new FixtureComponent();
		var view = new PaletteView();
		Object item = view.ngxComponentItem(ComponentManager.of(null), component);
		Class<?> itemType = item.getClass().getSuperclass();
		Method allowed = itemType.getDeclaredMethod("allowedIn", int.class);
		allowed.setAccessible(true);
		if (!Boolean.TRUE.equals(allowed.invoke(item, ProjectExplorerView.TREE_OBJECT_TYPE_DBO_MOBILE_UICOMPONENT))) {
			throw new AssertionError("An NGX control must appear in the controls collection");
		}
		if (!Boolean.FALSE.equals(allowed.invoke(item, ProjectExplorerView.TREE_OBJECT_TYPE_DBO_SEQUENCE))) {
			throw new AssertionError("An NGX control must not appear in the sequences collection");
		}
		if (component.hints != 0 || component.beans != 1) {
			throw new AssertionError("Filtering must cache a bare prototype without evaluating its hint");
		}
		Method insert = item.getClass().getDeclaredMethod("newDatabaseObject");
		insert.setAccessible(true);
		if (!(insert.invoke(item) instanceof UIText) || component.hints != 1 || component.beans != 2) {
			throw new AssertionError("Normal insertion must still use the component hint");
		}
		var ctrl = PaletteView.class.getDeclaredField("isCtrl");
		ctrl.setAccessible(true);
		ctrl.setBoolean(view, true);
		if (!(insert.invoke(item) instanceof UIText) || component.hints != 1 || component.beans != 3) {
			throw new AssertionError("Ctrl insertion must continue to bypass the hint");
		}
		System.out.println("PaletteComponentFilterTest: filtering, prototype cache, normal and Ctrl insertion passed");
	}

	private static final class FixtureComponent extends Component {
		int hints, beans;
		@Override public String getTemplateProjectName() { return "Fixture"; }
		@Override public String getDescription() { return "Fixture component"; }
		@Override public String getName() { return "Fixture"; }
		@Override public String getGroup() { return "Controls"; }
		@Override public String getLabel() { return "Fixture"; }
		@Override public String getTag() { return "span"; }
		@Override public String getImagePath() { return ""; }
		@Override public String getPropertiesDescription() { return ""; }
		@Override public boolean isAllowedIn(DatabaseObject parent) { return true; }
		@Override public boolean isBuiltIn() { return true; }
		@Override public boolean isAdditional() { return false; }
		@Override protected DatabaseObject createBean() { beans++; return new UIText(); }
		@Override protected JSONObject getHint() { hints++; return new JSONObject(); }
	}
}
