package com.twinsoft.convertigo.engine.admin.services.studio.sourcepicker;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.studio.menu.CallAction;
import com.twinsoft.convertigo.engine.studio.CheStudio;
import com.twinsoft.convertigo.engine.studio.editors.connectors.htmlconnector.TwsDomTreeWrap;
import com.twinsoft.convertigo.engine.studio.editors.connectors.htmlconnector.XpathEvaluatorCompositeWrap;
import com.twinsoft.convertigo.engine.studio.views.sourcepicker.SourcePickerViewWrap;

@ServiceDefinition(
        name = "EvaluateXpath",
        roles = { Role.WEB_ADMIN },
        parameters = {},
        returnValue = ""
    )
public class EvaluateXpath extends XmlService {

    @Override
    protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
        HttpSession session = request.getSession();

        CheStudio cheStudio = CallAction.getStudio(session);
        if (cheStudio != null) {
            SourcePickerViewWrap spv = cheStudio.getSourcePickerView();
            if (spv != null) {
                String xpath = request.getParameter("xpath").trim();

                try {
                    Document doc = XpathEvaluatorCompositeWrap.getXpathData(spv.getDom(), xpath);
                    Element xPathTree = TwsDomTreeWrap.getTree2(doc, doc.getDocumentElement(), "xpath_tree");

                    xPathTree = (Element) document.importNode(xPathTree, true);
                    document.getDocumentElement().appendChild(xPathTree);
                }
                catch (Exception e) {
                }
            }
        }
    }
}
