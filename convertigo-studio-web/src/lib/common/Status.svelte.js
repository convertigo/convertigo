import { call } from '$lib/utils/service';

let status = $state({
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
});

let init = false;

async function statusCheck() {
	if (!init) {
		init = true;
		try {
			const response = await call('engine.JsonStatus');
			if ('javaVersion' in response) {
				for (let k in status) {
					if (k in response) {
						status[k] = response[k];
					}
				}
			}
		} catch (e) {
			init = false;
		}
	}
}

export default {
	get product() {
		statusCheck();
		return status.product;
	},
	get beans() {
		statusCheck();
		return status.beans;
	},
	get licenceType() {
		statusCheck();
		return status.licenceType;
	},
	get licenceNumber() {
		statusCheck();
		return status.licenceNumber;
	},
	get licenceEnd() {
		statusCheck();
		return status.licenceEnd;
	},
	get licenceExpired() {
		statusCheck();
		return status.licenceExpired;
	},
	get javaVersion() {
		statusCheck();
		return status.javaVersion;
	},
	get javaClassVersion() {
		statusCheck();
		return status.javaClassVersion;
	},
	get javaVendor() {
		statusCheck();
		return status.javaVendor;
	},
	get hostName() {
		statusCheck();
		return status.hostName;
	},
	get hostAddresses() {
		statusCheck();
		return status.hostAddresses;
	},
	get osName() {
		statusCheck();
		return status.osName;
	},
	get osVersion() {
		statusCheck();
		return status.osVersion;
	},
	get osArchitecture() {
		statusCheck();
		return status.osArchitecture;
	},
	get osAvailableProcessors() {
		statusCheck();
		return status.osAvailableProcessors;
	},
	get browser() {
		statusCheck();
		return status.browser;
	},
	get cloud() {
		statusCheck();
		return status.cloud;
	},
	get id() {
		statusCheck();
		return status.id;
	},
	get endpoint() {
		statusCheck();
		return status.endpoint;
	}
};
