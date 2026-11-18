/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow;

import static org.junit.Assert.*;

import java.util.regex.Pattern;

import org.junit.Test;

public class FlowStudioSupportHtmlTest {
	private static final Pattern NAMED_ENTITY = Pattern.compile("&[a-zA-Z]+[a-zA-Z0-9]*;");

	@Test
	public void keepsAccentedCharactersReadable() {
		// Real description text of the sql.query block.
		var text = "Exécuter une requête SQL native nommée et paramétrée.";
		var html = FlowStudioSupport.html(text);
		assertEquals(text, html);
		assertFalse(NAMED_ENTITY.matcher(html).find());
	}

	@Test
	public void keepsOtherUnicodeCharacters() {
		var text = "à ç € — 中";
		assertEquals(text, FlowStudioSupport.html(text));
	}

	@Test
	public void stillEscapesMarkup() {
		assertEquals("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt;", FlowStudioSupport.html("<script>alert('x')</script>"));
		assertEquals("a &amp; b &quot;c&quot;", FlowStudioSupport.html("a & b \"c\""));
		assertEquals("", FlowStudioSupport.html(null));
	}
}
