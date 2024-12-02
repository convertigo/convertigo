import ServiceHelper from './ServiceHelper.svelte';

const defValues = {
	product: null,
	beans: null,
	licenceType: null,
	licenceNumber: null,
	licenceEnd: null,
	licenceExpired: null,
	javaVersion: null,
	javaClassVersion: null,
	javaVendor: null,
	hostName: null,
	hostAddresses: null,
	osName: null,
	osVersion: null,
	osArchitecture: null,
	osAvailableProcessors: null,
	browser: null,
	cloud: null,
	id: null,
	endpoint: null
};

export default ServiceHelper({ service: 'engine.JsonStatus', defValues });
