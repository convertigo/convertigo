package com.twinsoft.convertigo.engine;

public class RequestableEngineEvent extends EngineEvent {
	private static final long serialVersionUID = -6581623177818044078L;

	private String projectName;
	private String sequenceName;
	private String connectorName;

	public RequestableEngineEvent(Object source, String projectName, String sequenceName, String connectorName) {
		super(source);
		
		this.projectName = projectName;
		this.sequenceName = sequenceName;
		this.connectorName = connectorName;
	}

	public String getProjectName() {
		return projectName;
	}
	
	public String getSequenceName() {
		return sequenceName;
	}
	
	public String getConnectorName() {
		return connectorName;
	}
}
