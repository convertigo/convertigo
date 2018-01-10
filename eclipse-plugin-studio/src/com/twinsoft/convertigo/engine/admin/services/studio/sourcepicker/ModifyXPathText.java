package com.twinsoft.convertigo.engine.admin.services.studio.sourcepicker;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.w3c.dom.Document;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.studio.menu.CallAction;
import com.twinsoft.convertigo.engine.studio.CheStudio;
import com.twinsoft.convertigo.engine.studio.views.sourcepicker.SourcePickerViewWrap;

@ServiceDefinition(
        name = "ModifyXPathText",
        roles = { Role.WEB_ADMIN },
        parameters = {},
        returnValue = ""
    )
public class ModifyXPathText extends XmlService {

    @Override
    protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
        HttpSession session = request.getSession();

        CheStudio cheStudio = CallAction.getStudio(session);
        if (cheStudio != null) {
            SourcePickerViewWrap spv = cheStudio.getSourcePickerView();
            if (spv != null) {
                String xpath = request.getParameter("xpath").trim();
                spv.modifyXpathText(xpath);
            }
        }
    }
}
