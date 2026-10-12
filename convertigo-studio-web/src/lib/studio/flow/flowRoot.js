/**
 * The Flow implementation of a block holding the selection, the last one of its trail: its graph is shown like the
 * one of a sequence, named as a Flow calls the block, "namespace.block".
 * @param {{ id: string, label: string, flowType?: string }[]} trail the nodes from the project to the selection
 * @returns {{ id: string, name: string } | null}
 */
export function flowImplementationRoot(trail) {
	const nodes = trail ?? [];
	const index = nodes.findLastIndex(
		(node) => node?.flowType === 'flow' && /\.implementation$/.test(String(node.id ?? ''))
	);
	if (index < 0) {
		return null;
	}
	const block = nodes[index - 1];
	const namespace = nodes[index - 2];
	const name =
		block && namespace && /\.namespace_[^.]*$/.test(String(namespace.id ?? ''))
			? `${namespace.label}.${block.label}`
			: (block?.label ?? '');
	return { id: nodes[index].id, name: name || nodes[index].label };
}
