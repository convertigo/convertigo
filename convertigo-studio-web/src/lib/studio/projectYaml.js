import { call } from '#lib/utils/service.js';

/** Save the project in standard YAML only after the compatibility warning is accepted. */
export async function convertProjectYaml(projectName, confirm, request = call) {
	if (!projectName || !confirm(`Convert "${projectName}" to standard YAML?\n\nThis saves the current project changes and raises its minimum engine version to 8.5. Older engines will no longer be able to open it.`)) {
		return false;
	}
	const result = await request('projects.ConvertYaml', { projectName });
	return !result?.isError && Boolean(result?.admin?.success);
}
