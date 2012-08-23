/*
 * Copyright (c) 2001-2011 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.beans.common;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.twinsoft.convertigo.beans.core.Block;
import com.twinsoft.convertigo.beans.core.BlockFactory;
import com.twinsoft.convertigo.beans.core.ComplexExtractionRule;
import com.twinsoft.convertigo.beans.core.ExtractionRule;
import com.twinsoft.convertigo.beans.extractionrules.JavelinExtractionRuleResult;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.twinj.iJavelin;

public class SplitFields extends ComplexExtractionRule {
	
	private static final long serialVersionUID = 3408126699479451113L;

	/** Tag du tableau */
	private String tagTable = "table";

	/** Séparateur de début et fin de tableau */
	private String separatorTableStart 	= "";
	private String separatorTableEnd	= "";
	
	/** Séparateur de début et fin de ligne */
	private String separatorRowStart	= "";
	private String separatorRowEnd		= "";
	
	/** Séparateur de début et fin de cellule */
	private String separatorCelStart	= "";
	private String separatorCelEnd		= "";
	
	/** Noms des colonnes */
	private XMLVector<XMLVector<String>> columns = new XMLVector<XMLVector<String>>();
	
	/** Holds value of property doNotAccumulate. */
	private boolean doNotAccumulate = false;
	
	private transient Block accumulatedData;
	private transient String[] cols  = null;
	private transient SplitWith  tab = null;
	private transient SplitWith  row = null;
	private transient SplitWith  cel = null;

	public String getTagTable() {
		return tagTable;
	}

	public void setTagTable(String tagTable) {
		this.tagTable = tagTable;
	}
	
	public String getSeparatorCelStart() {
		return separatorCelStart;
	}

	public void setSeparatorCelStart(String separatorCelStart) {
		this.separatorCelStart = separatorCelStart;
		cel = getBestSplitWith(separatorCelStart,separatorCelEnd);
	}

	public String getSeparatorCelEnd() {
		return separatorCelEnd;
	}

	public void setSeparatorCelEnd(String separatorCelEnd) {
		this.separatorCelEnd = separatorCelEnd;
		cel = getBestSplitWith(separatorCelStart,separatorCelEnd);
	}

	public String getSeparatorRowEnd() {
		return separatorRowEnd;
	}

	public void setSeparatorRowEnd(String separatorRowEnd) {
		this.separatorRowEnd = separatorRowEnd;
		row = getBestSplitWith(separatorRowStart,separatorRowEnd);
	}

	public String getSeparatorRowStart() {
		return separatorRowStart;
	}

	public void setSeparatorRowStart(String separatorRowStart) {
		this.separatorRowStart = separatorRowStart;
		row = getBestSplitWith(separatorRowStart,separatorRowEnd);
	}

	public String getSeparatorTableEnd() {
		return separatorTableEnd;
	}

	public void setSeparatorTableEnd(String separatorTableEnd) {
		this.separatorTableEnd = separatorTableEnd;
		tab = getBestSplitWith(separatorTableStart,separatorTableEnd);
	}

	public String getSeparatorTableStart() {
		return separatorTableStart;
	}

	public void setSeparatorTableStart(String separatorTableStart) {
		this.separatorTableStart = separatorTableStart;
		tab = getBestSplitWith(separatorTableStart,separatorTableEnd);
	}
	
	public XMLVector<XMLVector<String>> getColumns() {
		return columns;
	}

	public void setColumns(XMLVector<XMLVector<String>> columns) {
		this.columns = columns;
		
		int i=0;
		int size = columns.size();
		
		
		/** copie dans un tableau pour un accès direct */
		cols = new String[size];
		
		for(List<String> column: columns)
			cols[i++] = column.get(0);
	}	

	/** Getter for property doNotAccumulate.
	 * @return Value of property doNotAccumulate.
	 */
	public boolean isDoNotAccumulate() {
		return this.doNotAccumulate;
	}
    
	/** Setter for property doNotAccumulate.
	 * @param doNotAccumulate New value of property doNotAccumulate.
	 */
	public void setDoNotAccumulate(boolean doNotAccumulate) {
		this.doNotAccumulate = doNotAccumulate;
	}
	
	@Override
    public void init(int reason) {
        // We set our accumulated data Element to null. Other calls to the rule within
        // the same transaction will cause data to be accumulated in the same Table
		Engine.logBeans.trace("### Initializing splitFields (reason = " + reason + ")...");
			
        if (reason == ExtractionRule.INITIALIZING) {
            accumulatedData = null;
//           lines = 0;
//			indexRow = 0;
        }
    }
	
	/** Creates new SplitFields */
	public SplitFields() {
		super();
	}

	/** il y a plusieurs type de split */
	interface SplitWith{
		abstract List<String> splitWith(String s);
	}

	/** sans séparateur */
	class DontSplit implements SplitWith{
		
		/** retourne la chaîne directement dans un vecteur */
		public List<String> splitWith(String s){
			return Arrays.asList(new String[]{s});
		}	
	}
	
	/** séparateur au début */
	class SplitStartWith implements SplitWith{
		private String _sep;
		private int _lsep;
				
		public SplitStartWith(String sep){
			_sep = sep;
			_lsep = _sep.length();
			Engine.logBeans.trace("SplitStartWith "+sep+" is build");
		}
		
		public List<String> splitWith(String str) {
			List<String> strings = new LinkedList<String>();
			int curindex, nextindex = str.indexOf(_sep, 0);
			while((curindex = nextindex) != -1 ){
				nextindex = str.indexOf(_sep, (curindex += _lsep));
				strings.add(str.substring(curindex,(nextindex != -1) ? nextindex : str.length()));
			}
			return strings;
		}
	}
	
	/** séparateur à la fin */
	class SplitEndWith implements SplitWith{
		private String _sep;
		private int _lsep;
				
		public SplitEndWith(String sep){
			_sep = sep;
			_lsep = _sep.length();
			Engine.logBeans.trace("SplitEndWith "+sep+" is build");
		}		
		
		public List<String> splitWith(String str) {
			List<String> strings = new LinkedList<String>();
			int curindex = 0;
			int nextindex = str.indexOf(_sep, curindex);
			while(nextindex != -1){
				strings.add(str.substring(curindex,nextindex));
				curindex = nextindex + _lsep;
				nextindex = str.indexOf(_sep,curindex);
			}
			return strings;
		}
		
	}
	
	/** sous chaînes entourées par deux séparateurs */
	class SplitStartEndWith implements SplitWith{
		private String _sepstart;
		private String _sepend;
		private int _lsepstart;
		private int _lsepend;
				
		public SplitStartEndWith(String sepstart,String sepend){
			_sepstart = sepstart;
			_sepend = sepend;
			_lsepstart = _sepstart.length();
			_lsepend = _sepend.length();
			Engine.logBeans.trace("SplitStartEndWith "+sepstart+" "+sepend+" is build");
		}
		
		public List<String> splitWith(String str) {
			List<String> strings = new LinkedList<String>();
			boolean start = true;
			int nextindex = 0;
			int curindex = str.indexOf(_sepstart, 0);
			while(nextindex != -1 && curindex != -1)
				if(start=!start){
					strings.add(str.substring(curindex + _lsepstart,nextindex));
					curindex = str.indexOf(_sepstart,nextindex + _lsepend);
				} else nextindex = str.indexOf(_sepend,curindex + _lsepstart);
			return strings;
		}
		
	}	
	
	/** retourne le spliteur adapté aux séparateurs */
	SplitWith getBestSplitWith(String sepstart,String sepend){
		if(sepstart.equals("") && sepend.equals("")) return new DontSplit();
		if(sepend.equals("")) return new SplitStartWith(sepstart);
		if(sepstart.equals("")) return new SplitEndWith(sepend);
		return new SplitStartEndWith(sepstart,sepend);
	}
	
//	private void insertBefore(BlockFactory bf, Block block, Block before){
//		bf.insertBlock(block,before);
//		bf.removeBlock(before);
//		bf.insertBlock(before,block);
//	}
	
	/**
	 * Applies the extraction rule to the current iJavelin object.
	 * 
	 * @param javelin
	 *            the Javelin object.
	 * @param block
	 *            the current block to analyze.
	 * @param blockFactory
	 *            the block context of the current block.
	 * @param dom
	 *            the XML DOM.
	 * 
	 * @return an ExtractionRuleResult object containing the result of the
	 *         query.
	 */
	public JavelinExtractionRuleResult execute(iJavelin javelin, Block block, BlockFactory blockFactory, org.w3c.dom.Document dom) {
		/* Devrait déjà être initialisé, à voir pourquoi les setter ne sont pas appelé lors d'un load */
		if(tab==null)tab=getBestSplitWith(separatorTableStart,separatorTableEnd);
		if(row==null)row=getBestSplitWith(separatorRowStart,separatorRowEnd);
		if(cel==null)cel=getBestSplitWith(separatorCelStart,separatorCelEnd);
		if(cols==null)setColumns(columns);
		
		JavelinExtractionRuleResult xrs = new JavelinExtractionRuleResult();
		Block curBlock = block;

		StringBuffer strbuf = new StringBuffer();
		
		while(!canBlockBeSelected(curBlock) && curBlock != null)
			curBlock = blockFactory.getNextBlock(curBlock);
		
		if(curBlock == null){
			Engine.logBeans.trace("SplitFields extraction have no block");
			xrs.hasMatched = false;
			xrs.newCurrentBlock = curBlock;
			return xrs;
		}
		
		Block tmpBlock = new Block();
		tmpBlock.setText("");
		blockFactory.insertBlock(tmpBlock, blockFactory.getPreviousBlock(block));
		//insertBefore(blockFactory,tmpBlock,curBlock);
		
		while(curBlock != null)
			if(canBlockBeSelected(curBlock)){
				strbuf.append(curBlock.getText());
				curBlock = blockFactory.getNextBlock(curBlock);
				//blockFactory.removeBlock(exBlock);				
			} else curBlock=blockFactory.getNextBlock(curBlock);
		
		Block lastBlock = tmpBlock;
		int tab_count = 1;
		
		for(String ltab : tab.splitWith(strbuf.toString())){
			Block tableBlock;
			if(accumulatedData == null || doNotAccumulate){
				tableBlock = new Block();
				accumulatedData = tableBlock;
				blockFactory.insertBlock(tableBlock,lastBlock);
				
				// OPIC : Changed name to name of the rule instead of tagName. This is to avoid
				// A CPU loop in getNextBlock. As block equality is done by comparing blocks by 
				// tagName and attributes. If all attibutes are the same, the routine goes in CPU loop.
				
				tableBlock.name = this.getName()+"_"+(tab_count++);
				tableBlock.type = "table";
				tableBlock.tagName = tagTable;
				tableBlock.setText("");
			}else tableBlock = accumulatedData;
			
			lastBlock = tableBlock;
			for(String lrow : row.splitWith(ltab)){
				Block rowBlock = new Block();
				rowBlock.name = "";
				rowBlock.type = "row";
				rowBlock.tagName = "row";
				rowBlock.setText("");
				tableBlock.addOptionalChildren(rowBlock);
				
				int i = 0;
				for(String lcel : cel.splitWith(lrow)){
					Block celBlock = new Block();
					celBlock.name = "";
					celBlock.type = "static";
					celBlock.tagName = (cols.length > i++)? cols[i-1] : "block";
					celBlock.setText(lcel);					
					rowBlock.addOptionalChildren(celBlock);
				}		
			}
		}
		
		xrs.hasMatched = (lastBlock != tmpBlock) ? true : false;
		//xrs.hasMatched = true;
		xrs.newCurrentBlock = blockFactory.getLastBlock();//blockFactory.getNextBlock(lastBlock);
		blockFactory.removeBlock(tmpBlock);
		return xrs;
	}
}
