/*
 * Copyright (c) 2001-2024 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.beans.statements;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.beans.core.Statement;
import com.twinsoft.convertigo.engine.EngineException;

public class IfThenElseStatement extends BlockStatement implements IThenElseStatementContainer {

	private static final long serialVersionUID = -2253911307425225516L;
	private transient ThenStatement thenStatement = null;
	private transient ElseStatement elseStatement = null;

	public IfThenElseStatement() {
		super();
	}
	
	public IfThenElseStatement(String condition) {
		super(condition);
	}
	
    @Override
    public IfThenElseStatement clone() throws CloneNotSupportedException {
    	IfThenElseStatement clonedObject = (IfThenElseStatement) super.clone();
        clonedObject.thenStatement = null;
        clonedObject.elseStatement = null;
        return clonedObject;
    }
	
	public boolean hasThenElseStatements() {
		checkSubLoaded();
		return true;
	}
	
	public void setThenStatement(ThenStatement thenStatement) {
		checkSubLoaded();
		this.thenStatement = thenStatement;
	}

	public ThenStatement getThenStatement() {
		checkSubLoaded();
		return thenStatement;
	}

	public void setElseStatement(ElseStatement elseStatement) {
		checkSubLoaded();
		this.elseStatement = elseStatement;
	}

	public ElseStatement getElseStatement() {
		checkSubLoaded();
		return elseStatement;
	}
	
	@Override
	public boolean execute(Context javascriptContext, Scriptable scope) throws EngineException {		
		if (isEnabled()) {
			if (super.execute(javascriptContext, scope)) {
				evaluate(javascriptContext, scope, getCondition(), "condition", true);
				if (evaluated instanceof Boolean) {
					if (evaluated.equals(Boolean.TRUE)) {
        				ThenStatement thenStatement = getThenStatement();
        				if (thenStatement != null) {
        					thenStatement.execute(javascriptContext, scope);
        				}
    				} else {
        				ElseStatement elseStatement = getElseStatement();
        				if (elseStatement != null) {
        					elseStatement.execute(javascriptContext, scope);
        				}
    				}
					return true;
				}
				else {
					EngineException ee = new EngineException(
							"Invalid statement condition.\n" +
							"IfStatement: \"" + getName()+ "\"");
					throw ee;
				}
			}
		}
		return false;
	}
	
	@Override
	public void addStatement(Statement statement) throws EngineException {
		checkSubLoaded();
		
		if (hasThenElseStatements()) {
			if ((!(statement instanceof ThenStatement)) && (!(statement instanceof ElseStatement))) {
				throw new EngineException("You cannot add to this statement a database object of type " + statement.getClass().getName());
			}
			
			if ((thenStatement == null) || (elseStatement == null)) {
				if ((statement instanceof ThenStatement)) {
					if (thenStatement == null) {
						super.addStatement(statement);
						thenStatement = (ThenStatement)statement;
					}
					else
						throw new EngineException("You cannot add to this statement another database object of type " + statement.getClass().getName());
				}
				else if ((statement instanceof ElseStatement)) {
					if (elseStatement == null) {
						super.addStatement(statement);
						elseStatement = (ElseStatement)statement;
					}
					else
						throw new EngineException("You cannot add to this statement another database object of type " + statement.getClass().getName());
				}
			}
			else {
				throw new EngineException("You cannot add to this step another database object of type " + statement.getClass().getName());
			}
		}
		else {
			super.addStatement(statement);
		}
	}
	
	@Override
	public void removeStatement(Statement statement) {
		checkSubLoaded();
		
		super.removeStatement(statement);
		if (hasThenElseStatements()) {
			if (statement.equals(thenStatement)) {
				thenStatement = null;
			}
			else if (statement.equals(elseStatement)) {
				elseStatement = null;
			}
		}
	}
	
	@Override
	public String toString() {
		String condition = getCondition();
		return "if(" + (condition.equals("") ? "??" : condition) + ")";
	}
	
	@Override
	public String toJsString() {
		String condition = getCondition();
		String code = "";
		if (!condition.equals("")) {
			code += " if ("+ condition +") {\n";
			code += super.toString();
			code += " \n}\n";
		}
		return code;
	}
}
