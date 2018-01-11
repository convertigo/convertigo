package com.twinsoft.convertigo.engine.studio.events;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.admin.util.DOMUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public abstract class AbstractEvent {

    protected String name;
    protected String data = null;

    protected String qname;
    protected Document document;

    public AbstractEvent(String name, String qname) {
        this.name = name;
        this.qname = qname;

        try {
            document = DOMUtils.createDocument();
        }
        catch (ParserConfigurationException e) {
        }
    }

    public String getName() {
        return name;
    }

    public String getData() {
        if (data == null) {
            try {
                data = XMLUtils.prettyPrintElement(toXml());
            }
            catch (Exception e) {
            }
        }

        return data;
    }

    protected abstract Element toXml() throws Exception;
}
