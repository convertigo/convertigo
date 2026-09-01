import ServiceHelper from '$lib/common/ServiceHelper.svelte';
import { call } from '$lib/utils/service';

const defValues = {
	certificates: new Array(3).fill({}),
	candidates: new Array(3).fill({}),
	anonymous: new Array(3).fill({}),
	carioca: new Array(3).fill({})
};

let calling = $state(false);

async function doCall(service, event) {
	event.preventDefault?.();
	const formData = event.target ? new FormData(event.target) : event;
	calling = true;
	try {
		const res = await call(service, formData);
		if (!res.isError) {
			await values.refresh();
		}
		return res;
	} finally {
		calling = false;
	}
}

let values = {
	get calling() {
		return calling;
	},
	remove: async (event) => {
		return await doCall('certificates.Remove', event);
	},
	install: async (event) => {
		return await doCall('certificates.Install', event);
	},
	configure: async (event) => {
		const formData = new FormData();
		event.target
			.closest('tr')
			.querySelectorAll('input, select')
			.forEach((field) => {
				formData.append(field.name, field.value);
			});
		return await doCall('certificates.Configure', formData);
	},
	del: async (event) => {
		const formData = new FormData();
		formData.append(
			'certificateName_1',
			event.target.closest('tr').querySelector('input[name="name_0"]').value
		);
		return await doCall('certificates.Delete', formData);
	},
	mappingsConfigure: async (event) => {
		const formData = new FormData();
		const tr = event.target.closest('tr');
		const tas = tr.querySelector('input[name="user_0"]') ? true : false;
		tr.querySelectorAll('input, select').forEach((field) => {
			formData.append(tas ? field.name.replace('convP', 'p') : field.name, field.value);
		});
		formData.append('targettedObject_0', tas ? 'tas' : 'projects');
		return await doCall('certificates.mappings.Configure', formData);
	},
	mappingsDel: async (event) => {
		const formData = new FormData();
		formData.append('link_1', event.target.closest('tr').querySelector('input[name="link"]').value);
		return await doCall('certificates.mappings.Delete', formData);
	},
	importCertificates: async (event) => {
		const res = await doCall('certificates.Import', event);
		return !res.isError;
	},
	exportCertificates: async () => {
		const elements = [
			...[...values.certificates, ...values.candidates]
				.filter(({ export: selected }) => selected)
				.map(({ name }) => ({ category: 'certificates', name })),
			...[...values.anonymous, ...values.carioca]
				.filter(({ export: selected }) => selected)
				.map(({ link: name }) => ({ category: 'mappings', name }))
		];
		const res = await doCall('certificates.Export', {
			elements: JSON.stringify(elements)
		});
		return !res.isError;
	},
	selectForExport: (category, row, selected) => {
		row.export = selected;
		if (category == 'mappings' && selected) {
			const certificate = values.certificates.find(({ name }) => name == row.certificateName);
			if (certificate) {
				certificate.export = true;
			}
		} else if (category == 'certificates' && !selected) {
			for (const mapping of [...values.anonymous, ...values.carioca]) {
				if (mapping.certificateName == row.name) {
					mapping.export = false;
				}
			}
		}
	}
};

export default ServiceHelper({
	defValues,
	values,
	service: 'certificates.List',
	arrays: [
		'admin.certificates.certificate',
		'admin.candidates.candidate',
		'admin.bindings.anonymous.binding',
		'admin.bindings.carioca.binding'
	],
	mapping: {
		certificates: 'admin.certificates.certificate',
		candidates: 'admin.candidates.candidate',
		anonymous: 'admin.bindings.anonymous.binding',
		carioca: 'admin.bindings.carioca.binding'
	},
	beforeUpdate: (data) => {
		for (const certificate of data.certificates) {
			certificate.export = false;
			certificate.configured = true;
		}
		for (const candidate of data.candidates) {
			candidate.export = false;
			candidate.configured = false;
		}
		for (const mapping of [...data.anonymous, ...data.carioca]) {
			mapping.export = false;
		}
		return data;
	}
});
