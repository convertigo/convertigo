/** Discover declared builders on the project's immediate objects, never by guessed Flow QNames. */
export async function resolveStudioBuilder(projectName, { menu, children }) {
	if (!projectName) return { transport: 'none', builders: [] };
	const project = await menu(projectName);
	const definition = project?.menu ?? project;
	if (definition?.items?.some((item) => item.id === 'frontend.execute')) {
		return { transport: 'ngx', builders: [] };
	}
	let builders = definition?.builders ?? [];
	if (!builders.length) {
		const response = await children(projectName);
		// Folder ids are not DatabaseObjects and cannot have context actions.
		const objects = (response?.children ?? []).filter((node) => node.id && !node.id.includes(':'));
		const menus = await Promise.all(objects.map((node) => menu(node.id)));
		builders = menus.flatMap((response) => (response?.menu ?? response)?.builders ?? []);
	}
	return { transport: builders.length ? 'actions' : 'none', builders };
}

/** Refresh permissions/state through the same descriptor, without inventing fallback commands. */
export async function refreshStudioBuilder(builder, menu) {
	const response = await menu(builder.target);
	return (
		(response?.menu ?? response)?.builders?.find(
			(candidate) => candidate.id === builder.id && candidate.target === builder.target
		) ?? null
	);
}

/** Attaching never starts a server; serving is idempotent when it is already active. */
export function studioBuilderOperation(builder, command) {
	if (command === 'attach') return builder?.state?.serving ? 'open' : null;
	return command === 'serve' && builder?.state?.serving ? 'open' : command;
}

/** Provider actions are opaque. The host owns only semantic command selection and error handling. */
export async function runStudioBuilderCommand(builder, command, run) {
	const action = builder?.commands?.[command];
	if (!action || action.enabled === false)
		throw new Error(`Builder command unavailable: ${command}`);
	const result = await run(builder.target, action);
	if (!result || result.ok === false || result.isError) {
		throw new Error(result?.error?.message ?? result?.message ?? 'The builder action failed.');
	}
	return result;
}
