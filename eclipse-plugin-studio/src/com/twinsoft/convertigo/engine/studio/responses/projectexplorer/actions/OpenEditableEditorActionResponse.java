package com.twinsoft.convertigo.engine.studio.responses.projectexplorer.actions;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.studio.responses.AbstractResponse;

public class OpenEditableEditorActionResponse extends AbstractResponse {

    private String filePath;
    private String typeEditor;

    public OpenEditableEditorActionResponse(String filePath, String typeEditor) {
        super();
        this.filePath = filePath;
        this.typeEditor = typeEditor;
    }

    @Override
    public Element toXml(Document document, String qname) throws Exception {
        Element response = super.toXml(document, qname);

        // Send file path
        Element eFilePath = document.createElement("filepath");
        eFilePath.setTextContent(filePath);

        response.setAttribute("type_editor", typeEditor);
        response.appendChild(eFilePath);

        return response;
    }
}
