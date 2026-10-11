/**
 * The transaction Ctrl+F5 runs, chosen as a request without parameters chooses it on a server (the implicit
 * connector and transaction of the engine).
 */

/**
 * @param {string} nodeId the selected object
 * @returns {string} the id of the connector holding the selection, the connector itself included, or none
 */
function connectorIdOf(nodeId = '') {
	return /^[^.:/]+\.cn:[^.:/]+/.exec(String(nodeId))?.[0] ?? '';
}

/**
 * Mirrors Project.getImplicitConnector and Connector.getImplicitTransaction of the engine.
 *
 * @param {any[]} children the connectors of a project, or the transactions of a connector
 * @param {string} mark how the tree marks the default one
 * @returns {string} the id of the default child, else of the only child, else none
 */
function implicitChildId(children, mark) {
	const list = Array.isArray(children) ? children : [];
	const marked = list.find((child) => child?.default === mark);
	const child = marked ?? (list.length === 1 ? list[0] : undefined);
	return String(child?.id ?? '');
}

/**
 * @param {string} projectName the project of the selection
 * @param {string} nodeId the selected object
 * @param {(id: string) => Promise<any[]>} childrenOf the children of a tree folder
 * @returns {Promise<string>} the id of the transaction: of the connector of the selection, else of the
 *          default connector of the project, else of its only connector; its default transaction, else its
 *          only transaction; none when nothing can be chosen
 */
async function implicitTransactionId(projectName, nodeId, childrenOf) {
	let connectorId = connectorIdOf(nodeId);
	if (!connectorId) {
		connectorId = implicitChildId(await childrenOf(`${projectName}:cn`), 'Default connector');
	}
	if (!connectorId) {
		return '';
	}
	return implicitChildId(await childrenOf(`${connectorId}:tr`), 'Default transaction');
}

export { connectorIdOf, implicitChildId, implicitTransactionId };
