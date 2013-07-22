package com.twinsoft.convertigo.beans.statements;
import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.engine.EngineException;

public interface IThenElseStatementContainer {
	public boolean hasThenElseStatements();
	public void addStatement(Statement statement) throws EngineException;
	public void removeStatement(Statement statement);
}
