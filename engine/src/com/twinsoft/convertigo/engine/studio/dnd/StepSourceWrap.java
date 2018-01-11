package com.twinsoft.convertigo.engine.studio.dnd;

public class StepSourceWrap {
    private String priority = null;
    private String xpath = null;

    public StepSourceWrap(String priority, String xpath) {
        this.priority = priority;
        this.xpath = xpath;
    }

    public String getPriority() {
        return priority;
    }

    public String getXpath() {
        return xpath;
    }
}
