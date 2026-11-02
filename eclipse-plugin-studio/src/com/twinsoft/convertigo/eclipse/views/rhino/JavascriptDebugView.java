/* Copyright (c) 2001-2026 Convertigo SA. Licensed under the GNU AGPL, version 3 or later. */
package com.twinsoft.convertigo.eclipse.views.rhino;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowser;
import com.twinsoft.convertigo.eclipse.swt.C8oBrowserPostMessageHelper;
import com.twinsoft.convertigo.eclipse.swt.SwtUtils;
import com.twinsoft.convertigo.eclipse.views.admin.AdminView;
import com.twinsoft.convertigo.eclipse.views.admin.AuthenticatedBrowser;

/** Embedded administrator web debugger. The existing Swing debugger remains available separately. */
public class JavascriptDebugView extends ViewPart {
    private C8oBrowser browser;
    private AuthenticatedBrowser authentication;

    @Override public void createPartControl(Composite parent) {
        SwtUtils.refreshTheme();
        parent.setLayout(new FillLayout());
        browser = new C8oBrowser(parent, SWT.NONE);
        browser.setUseExternalBrowser(false);
        new C8oBrowserPostMessageHelper(browser);
        authentication = new AuthenticatedBrowser(browser,
                () -> AdminView.getStudioAuthenticatedUrl("/admin/eclipse/javascript-debugger"));
        authentication.load();
        parent.layout(true, true);
    }
    @Override public void setFocus() { if (browser != null) browser.setFocus(); }
    @Override public void dispose() {
        if (authentication != null) { authentication.dispose(); authentication = null; }
        if (browser != null) { browser.dispose(); browser = null; }
        super.dispose();
    }
}
