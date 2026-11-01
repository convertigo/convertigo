/** Monaco hover restricted to the current editor model and already inspected identifiers. */
export function debugHover(getModel, getValues) {
	return {
		provideHover(model, position) {
			if (model !== getModel()) return null;
			const word = model.getWordAtPosition(position);
			const values = getValues();
			if (!word || !values || !Object.hasOwn(values, word.word)) return null;
			const escape = (value) => String(value).replace(/[\\`*_{}[\]()<>#+.!|~-]/g, '\\$&');
			return {
				range: {
					startLineNumber: position.lineNumber,
					endLineNumber: position.lineNumber,
					startColumn: word.startColumn,
					endColumn: word.endColumn
				},
				contents: [
					{
						value: `${escape(word.word)} = ${escape(values[word.word])}`,
						isTrusted: false,
						supportHtml: false
					}
				]
			};
		}
	};
}
