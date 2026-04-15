/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;

import com.twinsoft.convertigo.beans.common.FormatedContent;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.MobileApplication;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.ngx.components.MobileSmartSourceType;
import com.twinsoft.convertigo.beans.ngx.components.MobileSmartSourceType.Mode;
import com.twinsoft.convertigo.beans.ngx.components.PageComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIAttribute;
import com.twinsoft.convertigo.beans.ngx.components.UICompEvent;
import com.twinsoft.convertigo.beans.ngx.components.UIControlDirective;
import com.twinsoft.convertigo.beans.ngx.components.UIControlEvent;
import com.twinsoft.convertigo.beans.ngx.components.UICustomAction;
import com.twinsoft.convertigo.beans.ngx.components.UIElement;
import com.twinsoft.convertigo.beans.ngx.components.UISharedRegularComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIStyle;
import com.twinsoft.convertigo.beans.ngx.components.UIText;
import com.twinsoft.convertigo.beans.ngx.components.UIUseShared;
import com.twinsoft.convertigo.beans.ngx.components.UIUseVariable;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.UndefinedSymbolsException;
import com.twinsoft.convertigo.engine.mobile.MobileBuilder;

public class NgxIonicRoundTripConverterTest {

	private static final Method APPLY_TEMPLATE_TRANSACTIONAL = privateMethod("applyTemplateTransactional",
		com.twinsoft.convertigo.beans.ngx.components.UISharedComponent.class,
		String.class,
		NgxIonicRoundTripConverter.ImportReport.class);
	private static final Method APPLY_TEMPLATE_TRANSACTIONAL_PAGE = privateMethod("applyTemplateTransactional",
		PageComponent.class,
		String.class,
		NgxIonicRoundTripConverter.ImportReport.class);
	private static final Method APPLY_TEMPLATE = privateMethod("applyTemplate",
		com.twinsoft.convertigo.beans.ngx.components.UISharedComponent.class,
		Path.class,
		NgxIonicRoundTripConverter.ImportReport.class);
	private static final Method APPLY_STYLE = privateMethod("applyStyle",
		com.twinsoft.convertigo.beans.ngx.components.UISharedComponent.class,
		Path.class,
		NgxIonicRoundTripConverter.ImportReport.class);
	private static final Method APPLY_APPLICATION_STYLE = privateMethod("applyStyle",
		ApplicationComponent.class,
		Path.class,
		NgxIonicRoundTripConverter.ImportReport.class);
	private static final Method APPLY_SCRIPT = privateMethod("applyScript",
		com.twinsoft.convertigo.beans.ngx.components.UISharedComponent.class,
		Path.class,
		NgxIonicRoundTripConverter.ImportReport.class);
	private static final Method APPLY_CUSTOM_ACTIONS = privateMethod("applyCustomActions",
		DatabaseObject.class,
		Path.class,
		NgxIonicRoundTripConverter.ImportReport.class);
	private static final Method SAME_DIRECTIVE_DEFINITION = privateMethod("sameDirectiveDefinition",
		UIControlDirective.class,
		UIControlDirective.class);
	private static final Method PATCH_ANCHORED_USE_SHARED_ATTRIBUTES = privateMethod("patchAnchoredUseSharedAttributes",
		Element.class,
		UIUseShared.class,
		privateNestedClass("ImportContext"));
	private static final Method COLLECT_IMPORT_MAPS = privateMethod("collectImportMaps",
		File.class,
		Path.class,
		Path.class);

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@BeforeClass
	public static void initializeEngineLogs() {
		if (Engine.logBeans == null) {
			Engine.logBeans = Logger.getLogger("test.beans");
		}
		if (Engine.logEngine == null) {
			Engine.logEngine = Logger.getLogger("test.engine");
		}
		if (Engine.theApp == null) {
			Engine.theApp = new Engine();
		}
		if (Engine.theApp.databaseObjectsManager == null) {
			Engine.theApp.databaseObjectsManager = new DatabaseObjectsManager() {
				@Override
				public Object getCompiledValue(Object propertyObjectValue) throws UndefinedSymbolsException {
					return propertyObjectValue;
				}
			};
		}
	}

	@Test
	public void applyTemplateTransactionalAddsAndRemovesMixedTextNodes() throws Exception {
		UISharedRegularComponent component = createTextComponent();
		UIElement title = (UIElement) findByPriority(component, 200L);

		NgxIonicRoundTripConverter.ImportReport addReport = new NgxIonicRoundTripConverter.ImportReport("Test");
		boolean changed = invokeBoolean(APPLY_TEMPLATE_TRANSACTIONAL,
			component,
			"<div class=\"class100\"><h1 class=\"class200\">{{this.loginTitle}} salut</h1></div>", addReport);

		assertTrue(changed);
		assertEquals(0, addReport.skipped);
		assertEquals(2, title.getUIComponentList().size());
		UIText expression = (UIText) title.getUIComponentList().get(0);
		UIText suffix = (UIText) title.getUIComponentList().get(1);
		assertEquals(Mode.SCRIPT, expression.getTextSmartType().getMode());
		assertEquals("this.loginTitle", expression.getTextSmartType().getSmartValue());
		assertEquals(Mode.PLAIN, suffix.getTextSmartType().getMode());
		assertEquals(" salut", suffix.getTextSmartType().getSmartValue());

		NgxIonicRoundTripConverter.ImportReport removeReport = new NgxIonicRoundTripConverter.ImportReport("Test");
		boolean removed = invokeBoolean(APPLY_TEMPLATE_TRANSACTIONAL,
			component,
			"<div class=\"class100\"><h1 class=\"class200\">{{this.loginTitle}}</h1></div>", removeReport);

		assertTrue(removed);
		assertEquals(0, removeReport.skipped);
		assertEquals(1, title.getUIComponentList().size());
		UIText remaining = (UIText) title.getUIComponentList().get(0);
		assertEquals(Mode.SCRIPT, remaining.getTextSmartType().getMode());
		assertEquals("this.loginTitle", remaining.getTextSmartType().getSmartValue());
	}

	@Test
	public void applyTemplateTransactionalPageAddsAndRemovesMixedTextNodes() throws Exception {
		PageComponent page = createTextPage();
		UIElement title = (UIElement) findByPriority(page, 1200L);

		NgxIonicRoundTripConverter.ImportReport addReport = new NgxIonicRoundTripConverter.ImportReport("Test");
		boolean changed = invokeBoolean(APPLY_TEMPLATE_TRANSACTIONAL_PAGE,
			page,
			"<div class=\"class1100\"><h1 class=\"class1200\">{{this.loginTitle}} salut</h1></div>", addReport);

		assertTrue(changed);
		assertEquals(0, addReport.skipped);
		assertEquals(2, title.getUIComponentList().size());
		UIText expression = (UIText) title.getUIComponentList().get(0);
		UIText suffix = (UIText) title.getUIComponentList().get(1);
		assertEquals(Mode.SCRIPT, expression.getTextSmartType().getMode());
		assertEquals("this.loginTitle", expression.getTextSmartType().getSmartValue());
		assertEquals(Mode.PLAIN, suffix.getTextSmartType().getMode());
		assertEquals(" salut", suffix.getTextSmartType().getSmartValue());

		NgxIonicRoundTripConverter.ImportReport removeReport = new NgxIonicRoundTripConverter.ImportReport("Test");
		boolean removed = invokeBoolean(APPLY_TEMPLATE_TRANSACTIONAL_PAGE,
			page,
			"<div class=\"class1100\"><h1 class=\"class1200\">{{this.loginTitle}}</h1></div>", removeReport);

		assertTrue(removed);
		assertEquals(0, removeReport.skipped);
		assertEquals(1, title.getUIComponentList().size());
		UIText remaining = (UIText) title.getUIComponentList().get(0);
		assertEquals(Mode.SCRIPT, remaining.getTextSmartType().getMode());
		assertEquals("this.loginTitle", remaining.getTextSmartType().getSmartValue());
	}

	@Test
	public void applyTemplateTransactionalLeavesExistingTreeUntouchedOnMismatch() throws Exception {
		UISharedRegularComponent component = createTextComponent();
		String before = toXml(component);

		NgxIonicRoundTripConverter.ImportReport report = new NgxIonicRoundTripConverter.ImportReport("Test");
		boolean changed = invokeBoolean(APPLY_TEMPLATE_TRANSACTIONAL,
			component,
			"<span class=\"class100\"><p class=\"class200\">broken</p></span>", report);

		assertFalse(changed);
		assertEquals(1, report.skipped);
		assertEquals(before, toXml(component));
	}

	@Test
	public void applyTemplateTransactionalRejectsDuplicatePriorityAnchors() throws Exception {
		UISharedRegularComponent component = createTextComponent();
		String before = toXml(component);

		NgxIonicRoundTripConverter.ImportReport report = new NgxIonicRoundTripConverter.ImportReport("Test");
		boolean changed = invokeBoolean(APPLY_TEMPLATE_TRANSACTIONAL,
			component,
			"<div class=\"class100\"><h1 class=\"class200\">{{this.loginTitle}}</h1><p class=\"class200\">bad reuse</p></div>",
			report);

		assertFalse(changed);
		assertEquals(1, report.skipped);
		assertTrue(report.warnings.get(0).contains("duplicate or ambiguous Convertigo priority anchor"));
		assertEquals(before, toXml(component));
	}

	@Test
	public void sameDirectiveDefinitionTreatsPlainAndScriptSourcesAsEquivalent() throws Exception {
		UIControlDirective plain = createIfDirective(100L, Mode.PLAIN, "this.visible");
		UIControlDirective script = createIfDirective(101L, Mode.SCRIPT, "this.visible");
		UIControlDirective different = createIfDirective(102L, Mode.SCRIPT, "this.hidden");

		assertTrue(invokeBoolean(SAME_DIRECTIVE_DEFINITION, plain, script));
		assertFalse(invokeBoolean(SAME_DIRECTIVE_DEFINITION, plain, different));
	}

	@Test
	public void applyTemplateTransactionalPreservesNestedNgForItemIndexAndInnerNgIf() throws Exception {
		UISharedRegularComponent component = createNestedDirectiveComponent();

		String html = ""
			+ "<ng-container class=\"class400\" *ngFor=\"let item of this.rows; index as i\">"
			+ "<ng-container class=\"class500\" *ngIf=\"item.visible && i > 0\">"
			+ "<div class=\"class600\">{{item.label}}</div>"
			+ "</ng-container>"
			+ "</ng-container>";

		NgxIonicRoundTripConverter.ImportReport report = new NgxIonicRoundTripConverter.ImportReport("Test");
		boolean changed = invokeBoolean(APPLY_TEMPLATE_TRANSACTIONAL, component, html, report);

		assertFalse(changed);
		assertEquals(0, report.skipped);

		UIControlDirective forEach = (UIControlDirective) findByPriority(component, 400L);
		UIControlDirective ifDirective = (UIControlDirective) findByPriority(component, 500L);
		assertNotNull(forEach);
		assertNotNull(ifDirective);
		assertEquals("item", forEach.getDirectiveItemName());
		assertEquals("i", forEach.getDirectiveIndexName());
		assertEquals("this.rows", forEach.getSourceSmartType().getSmartValue());
		assertEquals("item.visible && i > 0", ifDirective.getSourceSmartType().getSmartValue());
	}

	@Test
	public void applyTemplateTransactionalPreservesGeneratedNgForAliasesWhenAddingChildNgIf() throws Exception {
		UISharedRegularComponent component = createGeneratedNgForComponent();
		UIControlDirective forEach = (UIControlDirective) findByPriority(component, 400L);
		UIElement button = (UIElement) findByPriority(component, 500L);

		String html = ""
			+ "<ng-container class=\"class400\" *ngFor=\"let idTab = index;let tab of this.tabs;let item400 of this.tabs;trackBy:trackById\">"
			+ "<button class=\"class500\" *ngIf=\"!(item.type == 'map' && tab == 'tab_selector_choice_source')\">{{tab | translate}}</button>"
			+ "</ng-container>";

		NgxIonicRoundTripConverter.ImportReport report = new NgxIonicRoundTripConverter.ImportReport("Test");
		boolean changed = invokeBoolean(APPLY_TEMPLATE_TRANSACTIONAL, component, html, report);

		assertTrue(changed);
		assertEquals(0, report.skipped);
		assertEquals("tab", forEach.getDirectiveItemName());
		assertEquals("idTab", forEach.getDirectiveIndexName());
		assertEquals("this.tabs", forEach.getSourceSmartType().getSmartValue());
		assertEquals("trackBy:trackById", forEach.getDirectiveExpression());
		UIAttribute ngIf = findAttribute(button, "*ngIf");
		assertNotNull(ngIf);
		assertEquals("*ngIf=\"!(item.type == 'map' && tab == 'tab_selector_choice_source')\"", ngIf.computeTemplate().trim());
	}

	@Test
	public void applyTemplateTransactionalRecoversPollutedGeneratedNgForAliases() throws Exception {
		UISharedRegularComponent component = createGeneratedNgForComponent();
		UIControlDirective forEach = (UIControlDirective) findByPriority(component, 400L);

		String html = ""
			+ "<ng-container class=\"class400\" *ngFor=\"let item of let idTab = index;"
			+ "let item400 of let idTab = index;"
			+ "let tab of this.tabs;let item400 of this.tabs;trackBy:trackById;"
			+ "let tab of this.tabs;let item400 of this.tabs;trackBy:trackById\">"
			+ "<button class=\"class500\">{{tab | translate}}</button>"
			+ "</ng-container>";

		NgxIonicRoundTripConverter.ImportReport report = new NgxIonicRoundTripConverter.ImportReport("Test");
		boolean changed = invokeBoolean(APPLY_TEMPLATE_TRANSACTIONAL, component, html, report);

		assertFalse(changed);
		assertEquals(0, report.skipped);
		assertEquals("tab", forEach.getDirectiveItemName());
		assertEquals("idTab", forEach.getDirectiveIndexName());
		assertEquals("this.tabs", forEach.getSourceSmartType().getSmartValue());
		assertEquals("trackBy:trackById", forEach.getDirectiveExpression());
	}

	@Test
	public void applyTemplateTransactionalIgnoresSyntheticOnlyNgForAlias() throws Exception {
		UISharedRegularComponent component = createSyntheticOnlyNgForComponent();
		UIControlDirective forEach = (UIControlDirective) findByPriority(component, 800L);
		UIElement button = (UIElement) findByPriority(component, 900L);

		String html = ""
			+ "<ng-container class=\"class800\" *ngFor=\"let item800 of this.things\">"
			+ "<button class=\"class900\" *ngIf=\"this.visible\">{{label}}</button>"
			+ "</ng-container>";

		NgxIonicRoundTripConverter.ImportReport report = new NgxIonicRoundTripConverter.ImportReport("Test");
		boolean changed = invokeBoolean(APPLY_TEMPLATE_TRANSACTIONAL, component, html, report);

		assertTrue(changed);
		assertEquals(0, report.skipped);
		assertEquals("", forEach.getDirectiveItemName());
		assertEquals("", forEach.getDirectiveIndexName());
		assertEquals("this.things", forEach.getSourceSmartType().getSmartValue());
		UIAttribute ngIf = findAttribute(button, "*ngIf");
		assertNotNull(ngIf);
		assertEquals("*ngIf=\"this.visible\"", ngIf.computeTemplate().trim());
	}

	@Test
	public void applyTemplateTransactionalRemovesDeletedAnchoredNgForSubtree() throws Exception {
		UISharedRegularComponent component = createTwoLoopComponent();

		String html = ""
			+ "<ng-container class=\"class700\" *ngFor=\"let item of this.secondRows\">"
			+ "<span class=\"class800\">{{item.label}}</span>"
			+ "</ng-container>";

		NgxIonicRoundTripConverter.ImportReport report = new NgxIonicRoundTripConverter.ImportReport("Test");
		boolean changed = invokeBoolean(APPLY_TEMPLATE_TRANSACTIONAL, component, html, report);

		assertTrue(changed);
		assertEquals(0, report.skipped);
		assertNull(findByPriority(component, 400L));
		assertNull(findByPriority(component, 500L));
		assertNotNull(findByPriority(component, 700L));
		assertNotNull(findByPriority(component, 800L));
	}

	@Test
	public void applyTemplateTransactionalUnwrapsDeletedAnchoredContainerWhenChildrenRemain() throws Exception {
		UISharedRegularComponent component = createWrappedAccordionComponent();

		NgxIonicRoundTripConverter.ImportReport report = new NgxIonicRoundTripConverter.ImportReport("Test");
		boolean changed = invokeBoolean(APPLY_TEMPLATE_TRANSACTIONAL,
			component,
			"<ion-item class=\"class600\"></ion-item>",
			report);

		assertTrue(changed);
		assertEquals(0, report.skipped);
		assertNull(findByPriority(component, 400L));
		assertNull(findByPriority(component, 500L));
		UIElement item = (UIElement) findByPriority(component, 600L);
		assertNotNull(item);
		assertTrue(component.getUIComponentList().contains(item));
	}

	@Test
	public void applyTemplateTransactionalAddsAnchoredNgIfAsAttribute() throws Exception {
		UISharedRegularComponent component = createTextComponent();
		UIElement title = (UIElement) findByPriority(component, 200L);

		NgxIonicRoundTripConverter.ImportReport report = new NgxIonicRoundTripConverter.ImportReport("Test");
		boolean changed = invokeBoolean(APPLY_TEMPLATE_TRANSACTIONAL,
			component,
			"<div class=\"class100\"><h1 class=\"class200\" *ngIf=\"this.visible\">{{this.loginTitle}}</h1></div>",
			report);

		assertTrue(changed);
		assertEquals(0, report.skipped);
		UIAttribute ngIf = findAttribute(title, "*ngIf");
		assertNotNull(ngIf);
		assertEquals("*ngIf=\"this.visible\"", ngIf.computeTemplate().trim());

		NgxIonicRoundTripConverter.ImportReport secondReport = new NgxIonicRoundTripConverter.ImportReport("Test");
		boolean secondChanged = invokeBoolean(APPLY_TEMPLATE_TRANSACTIONAL,
			component,
			"<div class=\"class100\"><h1 class=\"class200\" *ngIf=\"this.visible\">{{this.loginTitle}}</h1></div>",
			secondReport);

		assertFalse(secondChanged);
		assertEquals(0, secondReport.skipped);
	}

	@Test
	public void applyTemplateTransactionalAddsClassAttributeOnAnchoredElement() throws Exception {
		UISharedRegularComponent component = createTextComponent();
		UIElement title = (UIElement) findByPriority(component, 200L);

		NgxIonicRoundTripConverter.ImportReport report = new NgxIonicRoundTripConverter.ImportReport("Test");
		boolean changed = invokeBoolean(APPLY_TEMPLATE_TRANSACTIONAL,
			component,
			"<div class=\"class100\"><h1 class=\"main-title class200\">{{this.loginTitle}}</h1></div>",
			report);

		assertTrue(changed);
		assertEquals(0, report.skipped);
		UIAttribute classAttribute = findAttribute(title, "class");
		assertNotNull(classAttribute);
		assertEquals("class=\"main-title\"", classAttribute.computeTemplate().trim());
	}

	@Test
	public void applyTemplateTransactionalAddsClickEventOnAnchoredDiv() throws Exception {
		UISharedRegularComponent component = createTextComponent();
		UIElement root = (UIElement) findByPriority(component, 100L);

		NgxIonicRoundTripConverter.ImportReport report = new NgxIonicRoundTripConverter.ImportReport("Test");
		boolean changed = invokeBoolean(APPLY_TEMPLATE_TRANSACTIONAL,
			component,
			"<div class=\"class100\" (click)=\"doSomething()\"><h1 class=\"class200\">{{this.loginTitle}}</h1></div>",
			report);

		assertTrue(changed);
		assertEquals(0, report.skipped);
		UIControlEvent click = findControlEvent(root, "(click)");
		assertNotNull(click);
		assertEquals(UIControlEvent.AttrEvent.onClick.name(), click.getEventName());
	}

	@Test
	public void applyTemplateTransactionalAddsAngularAttributeOnAnchoredDiv() throws Exception {
		UISharedRegularComponent component = createTextComponent();
		UIElement root = (UIElement) findByPriority(component, 100L);

		NgxIonicRoundTripConverter.ImportReport report = new NgxIonicRoundTripConverter.ImportReport("Test");
		boolean changed = invokeBoolean(APPLY_TEMPLATE_TRANSACTIONAL,
			component,
			"<div class=\"class100\" [attr.draggable]=\"this.canDrag\"><h1 class=\"class200\">{{this.loginTitle}}</h1></div>",
			report);

		assertTrue(changed);
		assertEquals(0, report.skipped);
		UIAttribute draggable = findAttribute(root, "[attr.draggable]");
		assertNotNull(draggable);
		assertEquals(Mode.SCRIPT, draggable.getAttrSmartType().getMode());
		assertEquals("this.canDrag", draggable.getAttrSmartType().getSmartValue());

		NgxIonicRoundTripConverter.ImportReport secondReport = new NgxIonicRoundTripConverter.ImportReport("Test");
		boolean secondChanged = invokeBoolean(APPLY_TEMPLATE_TRANSACTIONAL,
			component,
			"<div class=\"class100\" [attr.draggable]=\"this.canDrag\"><h1 class=\"class200\">{{this.loginTitle}}</h1></div>",
			secondReport);

		assertFalse(secondChanged);
		assertEquals(0, secondReport.skipped);
	}

	@Test
	public void applyTemplateTransactionalRemovesMissingAngularAttributesOnAnchoredDiv() throws Exception {
		UISharedRegularComponent component = createTextComponent();
		UIElement root = (UIElement) findByPriority(component, 100L);
		root.add(createAttribute("Draggable", "[attr.draggable]", 401L, Mode.SCRIPT, "this.canDrag"));
		root.add(createAttribute("DragStart", "(dragstart)", 402L, Mode.SCRIPT, "this.onDragStart($event)"));

		NgxIonicRoundTripConverter.ImportReport report = new NgxIonicRoundTripConverter.ImportReport("Test");
		boolean changed = invokeBoolean(APPLY_TEMPLATE_TRANSACTIONAL,
			component,
			"<div class=\"class100\"><h1 class=\"class200\">{{this.loginTitle}}</h1></div>",
			report);

		assertTrue(changed);
		assertEquals(0, report.skipped);
		assertNull(findAttribute(root, "[attr.draggable]"));
		assertNull(findAttribute(root, "(dragstart)"));
	}

	@Test
	public void applyTemplateTransactionalRemovesLegacyPriorityClassAnchors() throws Exception {
		UISharedRegularComponent component = createComponent("DropPlaceholderShared");
		UIElement template = createElement("ElseTemplate", "ng-template", 400L);
		template.setIdentifier("ElseBlock_1");
		template.add(createAttribute("LegacyTemplateClass", "class", 401L, Mode.PLAIN, "class1234567890000"));
		component.add(template);
		UIElement placeholder = createElement("DropPlaceholder", "div", 500L);
		placeholder.add(createAttribute("LegacyPlaceholderClass", "class", 501L, Mode.PLAIN, "drop-placeholder class1234567890001"));
		template.add(placeholder);

		NgxIonicRoundTripConverter.ImportReport report = new NgxIonicRoundTripConverter.ImportReport("Test");
		boolean changed = invokeBoolean(APPLY_TEMPLATE_TRANSACTIONAL,
			component,
			"<ng-template class=\"class400\" #ElseBlock_1><div class=\"drop-placeholder class500\"></div></ng-template>",
			report);

		assertTrue(changed);
		assertEquals(0, report.skipped);
		assertNull(findAttribute(template, "class"));
		UIAttribute placeholderClass = findAttribute(placeholder, "class");
		assertNotNull(placeholderClass);
		assertEquals("class=\"drop-placeholder\"", placeholderClass.computeTemplate().trim());
	}

	@Test
	public void applyTemplateDoesNotIgnorePriorityClassOnlyChanges() throws Exception {
		Project project = createProjectWithApplication("Test");
		ApplicationComponent application = (ApplicationComponent) project.getMobileApplication().getApplicationComponent();
		UISharedRegularComponent component = createComponent("DropPlaceholderShared");
		application.add(component);
		UIElement template = createElement("ElseTemplate", "ng-template", 400L);
		template.setIdentifier("ElseBlock_1");
		template.add(createAttribute("LegacyTemplateClass", "class", 401L, Mode.PLAIN, "class1234567890000"));
		component.add(template);
		UIElement placeholder = createElement("DropPlaceholder", "div", 500L);
		placeholder.add(createAttribute("LegacyPlaceholderClass", "class", 501L, Mode.PLAIN, "drop-placeholder class1234567890001"));
		template.add(placeholder);
		Path htmlPath = writeTempFile("drop-placeholder.html",
			"<ng-template class=\"class400\" #ElseBlock_1><div class=\"drop-placeholder class500\"></div></ng-template>");

		NgxIonicRoundTripConverter.ImportReport report = new NgxIonicRoundTripConverter.ImportReport("Test");
		boolean changed = invokeBoolean(APPLY_TEMPLATE, component, htmlPath, report);

		assertTrue(changed);
		assertEquals(0, report.skipped);
		assertNull(findAttribute(template, "class"));
		UIAttribute placeholderClass = findAttribute(placeholder, "class");
		assertNotNull(placeholderClass);
		assertEquals("class=\"drop-placeholder\"", placeholderClass.computeTemplate().trim());
	}

	@Test
	public void applyTemplateTransactionalIgnoresSharedComponentEventsWhenAddingDomChild() throws Exception {
		UISharedRegularComponent component = createTextComponent();
		UIElement root = (UIElement) findByPriority(component, 100L);
		UICompEvent onClose = new UICompEvent();
		onClose.setName("onClose");
		onClose.priority = 900L;
		component.add(onClose);

		NgxIonicRoundTripConverter.ImportReport report = new NgxIonicRoundTripConverter.ImportReport("Test");
		boolean changed = invokeBoolean(APPLY_TEMPLATE_TRANSACTIONAL,
			component,
			"<div class=\"class100\"><h1 class=\"class200\">{{this.loginTitle}}</h1><p>Technical id</p></div>",
			report);

		assertTrue(changed);
		assertEquals(0, report.skipped);
		assertNotNull(findByPriority(component, 900L));
		assertTrue(root.getUIComponentList().stream()
			.anyMatch(child -> child instanceof UIElement element && "p".equals(element.getTagName())));
	}

	@Test
	public void patchAnchoredUseSharedAttributesAddsMissingUseVariables() throws Exception {
		UIUseShared useShared = new UIUseShared();
		useShared.setName("DatasourceUse");
		useShared.priority = 700L;
		useShared.setSharedComponentQName("Test.MobileApplication.Application.datasource");

		Element element = Jsoup.parse("<c8oforms-datasource class=\"data-source class700\"></c8oforms-datasource>", "", Parser.xmlParser())
			.selectFirst("c8oforms-datasource");
		assertNotNull(element);
		element.attr("[editorHeight]", "'100%'");
		element.attr("[style.width]", "'100%'");
		element.attr("[style.display]", "'block'");

		boolean changed = invokeBoolean(PATCH_ANCHORED_USE_SHARED_ATTRIBUTES, element, useShared, null);

		assertTrue(changed);
		assertUseVariable(useShared, findUseVariableIgnoreCase(useShared, "editorHeight"), "'100%'");
		assertUseVariable(useShared, findUseVariableIgnoreCase(useShared, "style_width"), "'100%'");
		assertUseVariable(useShared, findUseVariableIgnoreCase(useShared, "style_display"), "'block'");
		UIAttribute classAttribute = findAttribute(useShared, "class");
		assertNotNull(classAttribute);
		assertEquals("class=\"data-source\"", classAttribute.computeTemplate().trim());
	}

	@Test
	public void patchAnchoredUseSharedAttributesRemovesMissingVariablesAndAttributes() throws Exception {
		Project project = createProjectWithApplication("Test");
		ApplicationComponent application = (ApplicationComponent) project.getMobileApplication().getApplicationComponent();
		UISharedRegularComponent target = createComponent("Datasource");
		target.priority = 600L;
		application.add(target);
		UISharedRegularComponent host = createComponent("Host");
		application.add(host);

		UIUseShared useShared = new UIUseShared();
		useShared.setName("DatasourceUse");
		useShared.priority = 700L;
		useShared.setSharedComponentQName(target.getQName());
		setTargetSharedComponent(useShared, target);
		host.add(useShared);
		UIUseVariable editorHeight = new UIUseVariable();
		editorHeight.setName("editorHeight");
		editorHeight.setVarSmartType(scriptSmartType("'100%'"));
		useShared.add(editorHeight);
		useShared.add(createAttribute("DragAttr", "[attr.draggable]", 701L, Mode.SCRIPT, "this.canDrag"));

		String selector = target.getSelector();
		Element element = Jsoup.parse("<" + selector + " class=\"class700\"></" + selector + ">", "", Parser.xmlParser())
			.selectFirst(selector);
		assertNotNull(element);

		boolean changed = invokeBoolean(PATCH_ANCHORED_USE_SHARED_ATTRIBUTES, element, useShared, null);

		assertTrue(changed);
		assertNull(findUseVariableIgnoreCase(useShared, "editorHeight"));
		assertNull(findAttribute(useShared, "[attr.draggable]"));
	}

	@Test
	public void applyStyleSkipsEquivalentContentAndUpdatesChangedContent() throws Exception {
		UISharedRegularComponent component = createComponent("StyleShared");
		UIStyle style = new UIStyle();
		style.setName(NgxIonicRoundTripConverter.STYLE_NAME);
		style.priority = 300L;
		style.setStyleContent(new FormatedContent(".shell { color: red; }"));
		component.add(style);

		Path scssPath = writeTempFile("shared.scss", ".shell { color: red; }\n");
		NgxIonicRoundTripConverter.ImportReport noOpReport = new NgxIonicRoundTripConverter.ImportReport("Test");
		invokeVoid(APPLY_STYLE, component, scssPath, noOpReport);
		assertEquals(0, noOpReport.stylesUpdated);
		assertEquals(".shell { color: red; }", style.getStyleContent().getString());

		Files.writeString(scssPath, ".shell { color: blue; }\n", StandardCharsets.UTF_8);
		NgxIonicRoundTripConverter.ImportReport updateReport = new NgxIonicRoundTripConverter.ImportReport("Test");
		invokeVoid(APPLY_STYLE, component, scssPath, updateReport);
		assertEquals(1, updateReport.stylesUpdated);
		assertEquals(".shell { color: blue; }", style.getStyleContent().getString());
	}

	@Test
	public void applyApplicationStyleImportsOnlyManagedDeltaWhenBaselineMatches() throws Exception {
		ApplicationComponent application = createApplication("Application");
		UIStyle baseStyle = createStyle("BaseStyle", 100L, ".base { color: red; }");
		application.add(baseStyle);

		Path scssPath = writeTempFile("app.component.scss", ".base { color: red; }\n.app-shell { color: blue; }\n");
		NgxIonicRoundTripConverter.ImportReport report = new NgxIonicRoundTripConverter.ImportReport("Test");
		invokeVoid(APPLY_APPLICATION_STYLE, application, scssPath, report);

		UIStyle managedStyle = findApplicationStyle(application, NgxIonicRoundTripConverter.STYLE_NAME);
		assertNotNull(managedStyle);
		assertEquals(1, report.stylesUpdated);
		assertEquals(".app-shell { color: blue; }", managedStyle.getStyleContent().getString());

		NgxIonicRoundTripConverter.ImportReport noOpReport = new NgxIonicRoundTripConverter.ImportReport("Test");
		invokeVoid(APPLY_APPLICATION_STYLE, application, scssPath, noOpReport);
		assertEquals(0, noOpReport.stylesUpdated);
	}

	@Test
	public void applyApplicationStyleUpdatesMarkedExistingStyleByPriority() throws Exception {
		ApplicationComponent application = createApplication("Application");
		UIStyle baseStyle = createStyle("BaseStyle", 100L, "@use '@angular/material' as mat;\n.base { color: red; }");
		application.add(baseStyle);

		String generated = NgxIonicRoundTripConverter.computeApplicationStyle(application);
		Path scssPath = writeTempFile("marked-app.component.scss", generated.replace("color: red", "color: blue"));
		NgxIonicRoundTripConverter.ImportReport report = new NgxIonicRoundTripConverter.ImportReport("Test");
		invokeVoid(APPLY_APPLICATION_STYLE, application, scssPath, report);

		assertEquals(1, report.stylesUpdated);
		assertEquals("@use '@angular/material' as mat;\n.base { color: blue; }", baseStyle.getStyleContent().getString());
		assertNull(findApplicationStyle(application, NgxIonicRoundTripConverter.STYLE_NAME));
	}

	@Test
	public void importIntoProjectCanTargetAppComponentScssWithoutSidecar() throws Exception {
		Path projectDir = temporaryFolder.newFolder("AppStyleImport").toPath();
		Path appDir = projectDir.resolve("_private/ionic/src/app");
		Files.createDirectories(appDir);
		Path scssPath = appDir.resolve("app.component.scss");
		Files.writeString(scssPath, ".app-global { display: block; }\n", StandardCharsets.UTF_8);

		Project project = new Project();
		project.setName("AppStyleImport");
		MobileApplication mobileApplication = new MobileApplication();
		mobileApplication.setName("MobileApplication");
		project.add(mobileApplication);
		ApplicationComponent application = createApplication("Application");
		mobileApplication.addApplicationComponent(application);

		NgxIonicRoundTripConverter.ImportReport report = NgxIonicRoundTripConverter.importIntoProject(
			project,
			projectDir.toFile(),
			Path.of("src/app/app.component.scss"));

		UIStyle managedStyle = findApplicationStyle(application, NgxIonicRoundTripConverter.STYLE_NAME);
		assertNotNull(managedStyle);
		assertEquals(1, report.processed);
		assertEquals(1, report.stylesUpdated);
		assertEquals(0, report.skipped);
		assertEquals(".app-global { display: block; }", managedStyle.getStyleContent().getString());
	}

	@Test
	public void importIntoProjectSkipsUnchangedMappedAppComponentScssOnGlobalImport() throws Exception {
		Path projectDir = temporaryFolder.newFolder("AppStyleNoOp").toPath();
		Path appDir = projectDir.resolve("_private/ionic/src/app");
		Files.createDirectories(appDir);
		Path scssPath = appDir.resolve("app.component.scss");
		Files.writeString(scssPath, ".generated { color: red; }\n", StandardCharsets.UTF_8);

		Project project = createProjectWithApplication("AppStyleNoOp");
		ApplicationComponent application = (ApplicationComponent) project.getMobileApplication().getApplicationComponent();
		NgxIonicRoundTripConverter.writeApplicationStyleMap(project, projectDir.toFile(), application, scssPath.toFile());

		NgxIonicRoundTripConverter.ImportReport report = NgxIonicRoundTripConverter.importIntoProject(project, projectDir.toFile());

		assertEquals(1, report.processed);
		assertEquals(0, report.stylesUpdated);
		assertEquals(0, report.skipped);
		assertNull(findApplicationStyle(application, NgxIonicRoundTripConverter.STYLE_NAME));
	}

	@Test
	public void importIntoProjectDoesNotImportUnchangedAppStyleWhenTargetingAppComponentTs() throws Exception {
		Path projectDir = temporaryFolder.newFolder("AppTsTarget").toPath();
		Path appDir = projectDir.resolve("_private/ionic/src/app");
		Files.createDirectories(appDir);
		Path scssPath = appDir.resolve("app.component.scss");
		Files.writeString(scssPath, ".generated { color: red; }\n", StandardCharsets.UTF_8);
		Files.writeString(appDir.resolve("app.component.ts"), "export class AppComponent {}\n", StandardCharsets.UTF_8);

		Project project = createProjectWithApplication("AppTsTarget");
		ApplicationComponent application = (ApplicationComponent) project.getMobileApplication().getApplicationComponent();
		NgxIonicRoundTripConverter.writeApplicationStyleMap(project, projectDir.toFile(), application, scssPath.toFile());

		NgxIonicRoundTripConverter.ImportReport report = NgxIonicRoundTripConverter.importIntoProject(
			project,
			projectDir.toFile(),
			Path.of("src/app/app.component.ts"));

		assertEquals(1, report.processed);
		assertEquals(0, report.stylesUpdated);
		assertEquals(0, report.skipped);
		assertNull(findApplicationStyle(application, NgxIonicRoundTripConverter.STYLE_NAME));
	}

	@Test
	public void importIntoProjectImportsChangedMappedAppComponentScssOnGlobalImport() throws Exception {
		Path projectDir = temporaryFolder.newFolder("AppStyleChanged").toPath();
		Path appDir = projectDir.resolve("_private/ionic/src/app");
		Files.createDirectories(appDir);
		Path scssPath = appDir.resolve("app.component.scss");
		Files.writeString(scssPath, ".generated { color: red; }\n", StandardCharsets.UTF_8);

		Project project = createProjectWithApplication("AppStyleChanged");
		ApplicationComponent application = (ApplicationComponent) project.getMobileApplication().getApplicationComponent();
		NgxIonicRoundTripConverter.writeApplicationStyleMap(project, projectDir.toFile(), application, scssPath.toFile());
		Files.writeString(scssPath, ".generated { color: blue; }\n", StandardCharsets.UTF_8);

		NgxIonicRoundTripConverter.ImportReport report = NgxIonicRoundTripConverter.importIntoProject(project, projectDir.toFile());

		UIStyle managedStyle = findApplicationStyle(application, NgxIonicRoundTripConverter.STYLE_NAME);
		assertNotNull(managedStyle);
		assertEquals(1, report.processed);
		assertEquals(1, report.stylesUpdated);
		assertEquals(0, report.skipped);
		assertEquals(".generated { color: blue; }", managedStyle.getStyleContent().getString());
	}

	@Test
	public void applyScriptSkipsEquivalentMarkersAndUpdatesChangedMarkers() throws Exception {
		UISharedRegularComponent component = createComponent("ScriptShared");
		component.setScriptContent(new FormatedContent(
			marker("CompImport", "import { A } from 'a';\n") + "\n" +
			marker("CompFunction", "foo() {\n\treturn true;\n}\n")));

		Path tsPath = writeTempFile("shared.ts",
			"import { Component } from '@angular/core';\n" +
			marker("CompImport", "import { A } from 'a';\n") + "\n" +
			marker("CompFunction", "foo() {\n\treturn true;\n}\n") + "\n" +
			"const ignored = true;\n");

		NgxIonicRoundTripConverter.ImportReport noOpReport = new NgxIonicRoundTripConverter.ImportReport("Test");
		invokeVoid(APPLY_SCRIPT, component, tsPath, noOpReport);
		assertEquals(0, noOpReport.scriptsUpdated);

		Files.writeString(tsPath,
			marker("CompImport", "import { A } from 'a';\n") + "\n" +
			marker("CompFunction", "foo() {\n\treturn false;\n}\n"),
			StandardCharsets.UTF_8);
		NgxIonicRoundTripConverter.ImportReport updateReport = new NgxIonicRoundTripConverter.ImportReport("Test");
		invokeVoid(APPLY_SCRIPT, component, tsPath, updateReport);
		assertEquals(1, updateReport.scriptsUpdated);
		assertEquals(normalizeNewlines(
			marker("CompImport", "import { A } from 'a';\n") + "\n" +
			marker("CompFunction", "foo() {\n\treturn false;\n}\n")),
			normalizeNewlines(component.getScriptContent().getString()));
	}

	@Test
	public void applyScriptKeepsDisabledComponentScriptWhenImportedMarkersAreEmpty() throws Exception {
		UISharedRegularComponent component = createComponent("DisabledShared");
		component.setEnabled(false);
		component.setScriptContent(new FormatedContent(marker("CompFunction", "foo() {\n\treturn true;\n}\n")));

		Path tsPath = writeTempFile("disabled.ts", marker("CompFunction", ""));
		NgxIonicRoundTripConverter.ImportReport report = new NgxIonicRoundTripConverter.ImportReport("Test");
		invokeVoid(APPLY_SCRIPT, component, tsPath, report);

		assertEquals(0, report.scriptsUpdated);
		assertEquals(normalizeNewlines(marker("CompFunction", "foo() {\n\treturn true;\n}\n")),
			normalizeNewlines(component.getScriptContent().getString()));
	}

	@Test
	public void applyCustomActionsSkipsEquivalentBodiesAndUpdatesExistingCTSMarkers() throws Exception {
		UISharedRegularComponent component = createComponent("ActionShared");
		UIElement button = createElement("Button", "ion-button", 100L);
		component.add(button);

		UIControlEvent click = new UIControlEvent();
		click.setName("Click");
		click.priority = 200L;
		button.add(click);

		UICustomAction action = new UICustomAction();
		action.setName("Run");
		action.priority = 300L;
		action.setActionValue(new FormatedContent("resolve(true);\n"));
		click.add(action);

		Path tsPath = writeTempFile("component.ts",
			marker("function:" + action.getActionName(), "\nresolve(true);\t\n"));
		NgxIonicRoundTripConverter.ImportReport noOpReport = new NgxIonicRoundTripConverter.ImportReport("Test");
		invokeVoid(APPLY_CUSTOM_ACTIONS, component, tsPath, noOpReport);
		assertEquals(0, noOpReport.scriptsUpdated);
		assertEquals("resolve(true);\n", action.getActionValue().getString());

		Files.writeString(tsPath,
			marker("function:" + action.getActionName(), "\nresolve(false);\n"),
			StandardCharsets.UTF_8);
		NgxIonicRoundTripConverter.ImportReport updateReport = new NgxIonicRoundTripConverter.ImportReport("Test");
		invokeVoid(APPLY_CUSTOM_ACTIONS, component, tsPath, updateReport);
		assertEquals(1, updateReport.scriptsUpdated);
		assertEquals("resolve(false);\n", action.getActionValue().getString());
	}

	@Test
	public void collectImportMapsCanTargetGeneratedFileOrDirectory() throws Exception {
		Path projectDir = temporaryFolder.newFolder("TargetedImport").toPath();
		Path ionicRoot = projectDir.resolve("_private/ionic");
		Path pageDir = ionicRoot.resolve("src/app/pages/page");
		Files.createDirectories(pageDir);
		Path html = pageDir.resolve("page.html");
		Path scss = pageDir.resolve("page.scss");
		Path ts = pageDir.resolve("page.ts");
		Path map = pageDir.resolve("page.c8o-map.json");
		Files.writeString(html, "<ion-content></ion-content>", StandardCharsets.UTF_8);
		Files.writeString(scss, "", StandardCharsets.UTF_8);
		Files.writeString(ts, "", StandardCharsets.UTF_8);
		Files.writeString(map, "{"
			+ "\"kind\":\"page\","
			+ "\"qname\":\"TargetedImport.MobileApplication.Application.Page\","
			+ "\"files\":{"
			+ "\"html\":\"_private/ionic/src/app/pages/page/page.html\","
			+ "\"scss\":\"_private/ionic/src/app/pages/page/page.scss\","
			+ "\"ts\":\"_private/ionic/src/app/pages/page/page.ts\""
			+ "}}", StandardCharsets.UTF_8);

		assertEquals(List.of(map), collectImportMaps(projectDir, ionicRoot, html));
		assertEquals(List.of(map), collectImportMaps(projectDir, ionicRoot, projectDir.relativize(scss)));
		assertEquals(List.of(map), collectImportMaps(projectDir, ionicRoot, Path.of("src/app/pages/page/page.ts")));
		assertEquals(List.of(map), collectImportMaps(projectDir, ionicRoot, pageDir));
	}

	private static UISharedRegularComponent createTextComponent() throws Exception {
		UISharedRegularComponent component = createComponent("TextShared");

		UIElement root = createElement("Root", "div", 100L);
		component.add(root);

		UIElement title = createElement("Title", "h1", 200L);
		root.add(title);

		title.add(createText("TitleExpr", 300L, Mode.SCRIPT, "this.loginTitle"));
		return component;
	}

	private static UISharedRegularComponent createNestedDirectiveComponent() throws Exception {
		UISharedRegularComponent component = createComponent("DirectiveShared");

		UIControlDirective forEach = new UIControlDirective();
		forEach.setName("RowsLoop");
		forEach.priority = 400L;
		forEach.setDirectiveName(UIControlDirective.AttrDirective.ForEach.name());
		forEach.setDirectiveItemName("item");
		forEach.setDirectiveIndexName("i");
		forEach.setSourceSmartType(scriptSmartType("this.rows"));
		component.add(forEach);

		UIControlDirective ifDirective = new UIControlDirective();
		ifDirective.setName("VisibleOnly");
		ifDirective.priority = 500L;
		ifDirective.setDirectiveName(UIControlDirective.AttrDirective.If.name());
		ifDirective.setSourceSmartType(scriptSmartType("item.visible && i > 0"));
		forEach.add(ifDirective);

		UIElement content = createElement("RowContent", "div", 600L);
		ifDirective.add(content);
		content.add(createText("RowLabel", 700L, Mode.SCRIPT, "item.label"));
		return component;
	}

	private static UISharedRegularComponent createGeneratedNgForComponent() throws Exception {
		UISharedRegularComponent component = createComponent("GeneratedNgForShared");

		UIControlDirective forEach = new UIControlDirective();
		forEach.setName("GeneratedLoop");
		forEach.priority = 400L;
		forEach.setDirectiveName(UIControlDirective.AttrDirective.ForEach.name());
		forEach.setDirectiveItemName("tab");
		forEach.setDirectiveIndexName("idTab");
		forEach.setSourceSmartType(scriptSmartType("this.tabs"));
		forEach.setDirectiveExpression("trackBy:trackById");
		component.add(forEach);

		UIElement button = createElement("ActionButton", "button", 500L);
		forEach.add(button);
		button.add(createText("ButtonText", 600L, Mode.SCRIPT, "tab | translate"));
		return component;
	}

	private static UISharedRegularComponent createSyntheticOnlyNgForComponent() throws Exception {
		UISharedRegularComponent component = createComponent("SyntheticOnlyNgForShared");

		UIControlDirective forEach = new UIControlDirective();
		forEach.setName("SyntheticLoop");
		forEach.priority = 800L;
		forEach.setDirectiveName(UIControlDirective.AttrDirective.ForEach.name());
		forEach.setDirectiveItemName("");
		forEach.setDirectiveIndexName("");
		forEach.setSourceSmartType(scriptSmartType("this.things"));
		component.add(forEach);

		UIElement button = createElement("SyntheticButton", "button", 900L);
		forEach.add(button);
		button.add(createText("SyntheticText", 1000L, Mode.SCRIPT, "label"));
		return component;
	}

	private static UISharedRegularComponent createTwoLoopComponent() throws Exception {
		UISharedRegularComponent component = createComponent("TwoLoopShared");

		UIControlDirective firstLoop = new UIControlDirective();
		firstLoop.setName("FirstLoop");
		firstLoop.priority = 400L;
		firstLoop.setDirectiveName(UIControlDirective.AttrDirective.ForEach.name());
		firstLoop.setDirectiveItemName("item");
		firstLoop.setSourceSmartType(scriptSmartType("this.firstRows"));
		component.add(firstLoop);

		UIElement firstContent = createElement("FirstContent", "div", 500L);
		firstLoop.add(firstContent);
		firstContent.add(createText("FirstText", 600L, Mode.SCRIPT, "item.label"));

		UIControlDirective secondLoop = new UIControlDirective();
		secondLoop.setName("SecondLoop");
		secondLoop.priority = 700L;
		secondLoop.setDirectiveName(UIControlDirective.AttrDirective.ForEach.name());
		secondLoop.setDirectiveItemName("item");
		secondLoop.setSourceSmartType(scriptSmartType("this.secondRows"));
		component.add(secondLoop);

		UIElement secondContent = createElement("SecondContent", "span", 800L);
		secondLoop.add(secondContent);
		secondContent.add(createText("SecondText", 900L, Mode.SCRIPT, "item.label"));
		return component;
	}

	private static UISharedRegularComponent createWrappedAccordionComponent() throws Exception {
		UISharedRegularComponent component = createComponent("WrappedAccordionShared");

		UIElement group = createElement("AccordionGroup", "ion-accordion-group", 400L);
		component.add(group);

		UIElement accordion = createElement("Accordion", "ion-accordion", 500L);
		group.add(accordion);

		UIElement item = createElement("AccordionContent", "ion-item", 600L);
		accordion.add(item);
		return component;
	}

	private static PageComponent createTextPage() throws Exception {
		PageComponent page = new PageComponent();
		page.setName("TextPage");
		page.priority = 1010L;

		UIElement root = createElement("PageRoot", "div", 1100L);
		page.add(root);

		UIElement title = createElement("PageTitle", "h1", 1200L);
		root.add(title);

		title.add(createText("PageTitleExpr", 1300L, Mode.SCRIPT, "this.loginTitle"));
		return page;
	}

	private static UISharedRegularComponent createComponent(String name) throws Exception {
		UISharedRegularComponent component = new UISharedRegularComponent();
		component.setName(name);
		component.priority = 10L;
		return component;
	}

	private static ApplicationComponent createApplication(String name) throws Exception {
		ApplicationComponent application = new ApplicationComponent();
		application.setName(name);
		application.priority = 20L;
		return application;
	}

	private static Project createProjectWithApplication(String projectName) throws Exception {
		Project project = new Project();
		project.setName(projectName);
		MobileApplication mobileApplication = new MobileApplication();
		mobileApplication.setName("MobileApplication");
		project.add(mobileApplication);
		mobileApplication.addApplicationComponent(createApplication("Application"));
		return project;
	}

	private static UIStyle createStyle(String name, long priority, String content) throws Exception {
		UIStyle style = new UIStyle();
		style.setName(name);
		style.priority = priority;
		style.setStyleContent(new FormatedContent(content));
		return style;
	}

	private static UIElement createElement(String name, String tagName, long priority) throws Exception {
		UIElement element = new UIElement();
		element.setName(name);
		element.setTagName(tagName);
		element.priority = priority;
		return element;
	}

	private static UIText createText(String name, long priority, Mode mode, String value) throws Exception {
		UIText text = new UIText();
		text.setName(name);
		text.priority = priority;
		MobileSmartSourceType smartType = new MobileSmartSourceType();
		smartType.setMode(mode);
		smartType.setSmartValue(value);
		text.setTextSmartType(smartType);
		return text;
	}

	private static UIAttribute createAttribute(String name, String attrName, long priority, Mode mode, String value) throws Exception {
		UIAttribute attribute = new UIAttribute();
		attribute.setName(name);
		attribute.priority = priority;
		attribute.setAttrName(attrName);
		attribute.setAttrSmartType(smartType(mode, value));
		return attribute;
	}

	private static void setTargetSharedComponent(UIUseShared useShared, UISharedRegularComponent target) throws Exception {
		Field field = UIUseShared.class.getDeclaredField("target");
		field.setAccessible(true);
		field.set(useShared, target);
	}

	private static UIControlDirective createIfDirective(long priority, Mode mode, String value) throws Exception {
		UIControlDirective directive = new UIControlDirective();
		directive.setName("Directive" + priority);
		directive.priority = priority;
		directive.setDirectiveName(UIControlDirective.AttrDirective.If.name());
		directive.setSourceSmartType(smartType(mode, value));
		return directive;
	}

	private static MobileSmartSourceType scriptSmartType(String value) {
		return smartType(Mode.SCRIPT, value);
	}

	private static MobileSmartSourceType smartType(Mode mode, String value) {
		MobileSmartSourceType smartType = new MobileSmartSourceType();
		smartType.setMode(mode);
		smartType.setSmartValue(value);
		return smartType;
	}

	private Path writeTempFile(String name, String content) throws Exception {
		Path path = temporaryFolder.newFile(name).toPath();
		Files.writeString(path, content, StandardCharsets.UTF_8);
		return path;
	}

	private static String marker(String markerId, String body) {
		return "/*Begin_c8o_" + markerId + "*/\n" + body + "/*End_c8o_" + markerId + "*/";
	}

	private static DatabaseObject findByPriority(DatabaseObject root, long priority) {
		if (root == null) {
			return null;
		}
		if (root.priority == priority) {
			return root;
		}
		if (root instanceof UIComponent component) {
			for (UIComponent child : component.getUIComponentList()) {
				DatabaseObject found = findByPriority(child, priority);
				if (found != null) {
					return found;
				}
			}
		}
		if (root instanceof PageComponent page) {
			for (UIComponent child : page.getUIComponentList()) {
				DatabaseObject found = findByPriority(child, priority);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}

	private static UIStyle findApplicationStyle(ApplicationComponent application, String name) {
		for (UIComponent component : application.getUIComponentList()) {
			if (component instanceof UIStyle style && name.equals(style.getName())) {
				return style;
			}
		}
		return null;
	}

	private static String toXml(DatabaseObject databaseObject) throws Exception {
		Document document = XMLUtils.getDefaultDocumentBuilder().newDocument();
		document.appendChild(databaseObject.toXml(document, DatabaseObject.ExportOption.bIncludeVersion));
		return XMLUtils.prettyPrintDOM(document);
	}

	private static Method privateMethod(String name, Class<?>... parameterTypes) {
		try {
			Method method = NgxIonicRoundTripConverter.class.getDeclaredMethod(name, parameterTypes);
			method.setAccessible(true);
			return method;
		} catch (Exception e) {
			throw new IllegalStateException("Unable to access " + name, e);
		}
	}

	private static Class<?> privateNestedClass(String simpleName) {
		try {
			return Class.forName(NgxIonicRoundTripConverter.class.getName() + "$" + simpleName);
		} catch (Exception e) {
			throw new IllegalStateException("Unable to access nested class " + simpleName, e);
		}
	}

	private static boolean invokeBoolean(Method method, Object... args) throws Exception {
		return (Boolean) invoke(method, args);
	}

	private static void invokeVoid(Method method, Object... args) throws Exception {
		invoke(method, args);
	}

	private static Object invoke(Method method, Object... args) throws Exception {
		try {
			return method.invoke(null, args);
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			if (cause instanceof Exception exception) {
				throw exception;
			}
			if (cause instanceof Error error) {
				throw error;
			}
			throw e;
		}
	}

	private static String normalizeNewlines(String value) {
		return value == null ? "" : value.replace("\r\n", "\n").replace('\r', '\n');
	}

	@SuppressWarnings("unchecked")
	private static List<Path> collectImportMaps(Path projectDir, Path ionicRoot, Path targetPath) throws Exception {
		return (List<Path>) invoke(COLLECT_IMPORT_MAPS, projectDir.toFile(), ionicRoot, targetPath);
	}

	private static UIAttribute findAttribute(UIComponent component, String attrName) {
		if (component == null || attrName == null) {
			return null;
		}
		for (UIComponent child : component.getUIComponentList()) {
			if (child instanceof UIAttribute attribute && attrName.equals(attribute.getAttrName())) {
				return attribute;
			}
		}
		return null;
	}

	private static UIControlEvent findControlEvent(UIComponent component, String attrName) {
		if (component == null || attrName == null) {
			return null;
		}
		for (UIComponent child : component.getUIComponentList()) {
			if (child instanceof UIControlEvent event && attrName.equals(event.getAttrName())) {
				return event;
			}
		}
		return null;
	}

	private static void assertUseVariable(UIUseShared useShared, UIUseVariable variable, String expectedSmartValue) {
		assertNotNull(describeUseSharedChildren(useShared), variable);
		assertEquals(expectedSmartValue, variable.getVarSmartType().getSmartValue());
	}

	private static UIUseVariable findUseVariableIgnoreCase(UIUseShared useShared, String expectedName) {
		if (useShared == null || expectedName == null) {
			return null;
		}
		for (UIComponent child : useShared.getUIComponentList()) {
			if (child instanceof UIUseVariable variable && expectedName.equalsIgnoreCase(variable.getName())) {
				return variable;
			}
		}
		return null;
	}

	private static String describeUseSharedChildren(UIUseShared useShared) {
		if (useShared == null) {
			return "useShared is null";
		}
		StringBuilder sb = new StringBuilder();
		for (UIComponent child : useShared.getUIComponentList()) {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append(child.getClass().getSimpleName()).append(':').append(child.getName());
		}
		return sb.toString();
	}
}
