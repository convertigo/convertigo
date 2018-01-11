package com.twinsoft.convertigo.engine.admin.services.studio.sourcepicker;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.studio.menu.CallAction;
import com.twinsoft.convertigo.engine.admin.util.DOMUtils;
import com.twinsoft.convertigo.engine.studio.CheStudio;
import com.twinsoft.convertigo.engine.studio.views.sourcepicker.SourcePickerViewWrap;

@ServiceDefinition(
        name = "GetXpathTree",
        roles = { Role.WEB_ADMIN },
        parameters = {},
        returnValue = ""
    )
public class GetXpathTree extends XmlService {

    @Override
    protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
        HttpSession session = request.getSession();

        CheStudio cheStudio = CallAction.getStudio(session);
        if (cheStudio != null) {
            SourcePickerViewWrap spv = cheStudio.getSourcePickerView();
            if (spv != null) {
                String nodeId = request.getParameter("nodeId");

                Element root = document.getDocumentElement();

                // Tree
                Element xpahtTree = spv.getXpathTree(nodeId);
                xpahtTree = (Element) document.importNode(xpahtTree, true);
                root.appendChild(xpahtTree);

                // xPath
                String xpath = spv.getXpath(nodeId);
                root.appendChild(DOMUtils.createElementWithText(document, "xpath", xpath));

                // Anchor
                String anchor = spv.getAnchor();
                root.appendChild(DOMUtils.createElementWithText(document, "anchor", anchor));
            }
        }
    }
}
