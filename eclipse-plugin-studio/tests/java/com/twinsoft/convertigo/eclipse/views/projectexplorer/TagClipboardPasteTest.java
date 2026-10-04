/* Copyright (c) 2001-2026 Convertigo SA. Licensed under the GNU AGPL v3. */
package com.twinsoft.convertigo.eclipse.views.projectexplorer;

/** Same live-model failures as the web tests, through the production Eclipse adapter without SWT UI. */
public class TagClipboardPasteTest extends com.twinsoft.convertigo.engine.tags.TagPasteAdapterTest {
	@Override protected void paste(String xml) throws Exception {
		var clipboard = new ClipboardManager();
		clipboard.objectsType = ProjectExplorerView.TREE_OBJECT_TYPE_DBO_SEQUENCE;
		try { clipboard.paste(xml, target, true); }
		catch (Exception e) { org.junit.Assert.assertEquals(0, clipboard.pastedObjects.length); throw e; }
	}
}
