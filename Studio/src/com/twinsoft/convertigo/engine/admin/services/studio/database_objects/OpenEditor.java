package com.twinsoft.convertigo.engine.admin.services.studio.database_objects;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Sheet;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.steps.SimpleStep;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;

@ServiceDefinition(
        name = "OpenEditor",
        roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG, Role.PROJECT_DBO_VIEW },
        parameters = {},
        returnValue = ""
    )
public class OpenEditor extends XmlService {

    private final static String JSCRIPT_STEP_EDITOR = "c8o_JscriptStepEditor";
    private final static String XSL_EDITOR = "c8o_XslEditor";
    private final static String JSCRIPT_TRANSACTION_EDITOR = "c8o_JscriptTransactionEditor";
    private final static String PRIVATE = "_private";

    @Override
    protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
        String qname = request.getParameter("qname");
        DatabaseObject dbo = Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(qname);
        Element response = document.createElement("response");

        if (dbo != null) {
            // File path + Editor
            Pair<String, String> filePathEditor = null;

            // Sequence_JS
            if (dbo instanceof SimpleStep) {
                filePathEditor = openJscriptStepEditor(dbo);
            }
            // Sheet
            else if (dbo instanceof Sheet) {
                filePathEditor = openXslEditor(dbo);
            }
            // Transaction
            else if (dbo instanceof Transaction) {
                filePathEditor = openJscriptTransactionEditor(dbo);
            }

            if (filePathEditor != null) {
                // Send file path
                Element eFilePath = document.createElement("filepath");
                eFilePath.setTextContent(filePathEditor.getLeft());

                response.setAttribute("type_editor", filePathEditor.getRight());
                response.appendChild(eFilePath);
            }
        }
        else {
            response.setAttribute("status", "error");
            response.setAttribute("message", "Database object not found.");
        }
        document.getDocumentElement().appendChild(response);
    }

    private Pair<String, String> openJscriptStepEditor(DatabaseObject dbo) throws IOException {
        Project project = dbo.getProject();
        String fileName = createTmpFileWithUTF8Data(
                project.getDirPath(),
                PRIVATE + "/" + Base64.encodeBase64URLSafeString(DigestUtils.sha1(dbo.getQName())) + " " + dbo.getName() + "." + JSCRIPT_STEP_EDITOR,
                ((SimpleStep) dbo).getExpression()
        );

        return Pair.of(
            project.getQName() + "/" + PRIVATE + "/" +  fileName,
            JSCRIPT_STEP_EDITOR
        );
    }

    private Pair<String, String> openXslEditor(DatabaseObject dbo) throws IOException {
        return Pair.of(
            dbo.getProject().getQName() + "/" + ((Sheet) dbo).getUrl(),
            XSL_EDITOR
        );
    }

    private Pair<String, String> openJscriptTransactionEditor(DatabaseObject dbo) throws IOException {
        Transaction transaction = (Transaction) dbo;
        Project project = dbo.getProject();
        String fileName = createTmpFileWithUTF8Data(
                project.getDirPath(),
                PRIVATE + "/" + dbo.getProject().getName() + "__" + transaction.getConnector().getName() + "__" + transaction.getName() + "." + JSCRIPT_TRANSACTION_EDITOR,
                transaction.handlers
        );

        return Pair.of(
            project.getQName() + "/" + PRIVATE + "/" + fileName,
            JSCRIPT_TRANSACTION_EDITOR
        );
    }

    private String createTmpFileWithUTF8Data(String parentPath, String childPath, String content) throws IOException {
        File tempEditorFile = new File(parentPath, childPath);
        try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(tempEditorFile), StandardCharsets.UTF_8)) {
            osw.append(content);
        }

        return tempEditorFile.getName();
    }
}
