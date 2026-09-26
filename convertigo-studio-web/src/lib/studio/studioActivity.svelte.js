/**
 * What the user did last in the Studio, as the Eclipse Studio tells its Tutorial view: the project
 * deployed, the link opened after a deployment and the page of the application previewed.
 */
export const studioActivity = $state({
	lastDeployment: '',
	lastLink: '',
	previewProject: '',
	previewUrl: ''
});
