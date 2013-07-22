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

package com.twinsoft.convertigo.eclipse.editors.xmlscanner;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.presentation.IPresentationDamager;
import org.eclipse.jface.text.presentation.IPresentationRepairer;
import org.eclipse.swt.custom.StyleRange;

public class NonRuleBasedDamagerRepairer
	implements IPresentationDamager, IPresentationRepairer {

	/** The document this object works on */
	protected IDocument fDocument;
	/** The default text attribute if non is returned as data by the current token */
	protected TextAttribute fDefaultTextAttribute;
	
	/**
	 * Constructor for NonRuleBasedDamagerRepairer.
	 */
	public NonRuleBasedDamagerRepairer(TextAttribute defaultTextAttribute) {
		Assert.isNotNull(defaultTextAttribute);

		fDefaultTextAttribute = defaultTextAttribute;
	}

	/**
	 * @see IPresentationRepairer#setDocument(IDocument)
	 */
	public void setDocument(IDocument document) {
		fDocument = document;
	}

	/**
	 * Returns the end offset of the line that contains the specified offset or
	 * if the offset is inside a line delimiter, the end offset of the next line.
	 *
	 * @param offset the offset whose line end offset must be computed
	 * @return the line end offset for the given offset
	 * @exception BadLocationException if offset is invalid in the current document
	 */
	protected int endOfLineOf(int offset) throws BadLocationException {

		IRegion info = fDocument.getLineInformationOfOffset(offset);
		if (offset <= info.getOffset() + info.getLength())
			return info.getOffset() + info.getLength();

		int line = fDocument.getLineOfOffset(offset);
		try {
			info = fDocument.getLineInformation(line + 1);
			return info.getOffset() + info.getLength();
		} catch (BadLocationException x) {
			return fDocument.getLength();
		}
	}

	/**
	 * @see IPresentationDamager#getDamageRegion(ITypedRegion, DocumentEvent, boolean)
	 */
	public IRegion getDamageRegion(
		ITypedRegion partition,
		DocumentEvent event,
		boolean documentPartitioningChanged) {
		if (!documentPartitioningChanged) {
			try {

				IRegion info =
					fDocument.getLineInformationOfOffset(event.getOffset());
				int start = Math.max(partition.getOffset(), info.getOffset());

				int end =
					event.getOffset()
						+ (event.getText() == null
							? event.getLength()
							: event.getText().length());

				if (info.getOffset() <= end
					&& end <= info.getOffset() + info.getLength()) {
					// optimize the case of the same line
					end = info.getOffset() + info.getLength();
				} else
					end = endOfLineOf(end);

				end =
					Math.min(
						partition.getOffset() + partition.getLength(),
						end);
				return new Region(start, end - start);

			} catch (BadLocationException x) {
			}
		}

		return partition;
	}

	/**
	 * @see IPresentationRepairer#createPresentation(TextPresentation, ITypedRegion)
	 */
	public void createPresentation(
		TextPresentation presentation,
		ITypedRegion region) {
		addRange(
			presentation,
			region.getOffset(),
			region.getLength(),
			fDefaultTextAttribute);
	}

	/**
	 * Adds style information to the given text presentation.
	 *
	 * @param presentation the text presentation to be extended
	 * @param offset the offset of the range to be styled
	 * @param length the length of the range to be styled
	 * @param attr the attribute describing the style of the range to be styled
	 */
	protected void addRange(
		TextPresentation presentation,
		int offset,
		int length,
		TextAttribute attr) {
		if (attr != null)
			presentation.addStyleRange(
				new StyleRange(
					offset,
					length,
					attr.getForeground(),
					attr.getBackground(),
					attr.getStyle()));
	}
}