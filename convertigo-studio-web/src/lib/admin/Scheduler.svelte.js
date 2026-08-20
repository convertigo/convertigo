import ServiceHelper from '$lib/common/ServiceHelper.svelte';
import { call, checkArray } from '$lib/utils/service';

const defValues = {
	jobs: Array(5).fill({
		name: null,
		decription: null,
		context: null,
		enabled: false,
		info: null,
		project: null,
		sequence: null,
		type: null,
		writeOutput: false,
		connector: null,
		transaction: null,
		parameter: Array(3).fill({ name: null, value: null })
	}),
	schedules: Array(4).fill({
		name: null,
		decription: null,
		cron: null,
		info: null,
		type: null,
		enabled: false
	}),
	scheduled: Array(3).fill({
		name: null,
		decription: null,
		scheduleName: null,
		jobName: null,
		info: null,
		enabled: false
	})
};

function normalizeParameters(parameter) {
	return checkArray(parameter).reduce((parameters, parameter) => {
		const { name, value } = parameter ?? {};
		if (name) {
			parameters[name] = checkArray(value);
		}
		return parameters;
	}, {});
}

let waiting = $state(false);

async function doCall(action, param) {
	waiting = true;
	try {
		param?.preventDefault?.();
		const res = await call(
			`scheduler.${action}`,
			param?.target ? new FormData(param.target) : param
		);
		if (!res.isError) {
			values.refresh();
		}
		return res;
	} finally {
		waiting = false;
	}
}

function includeJobDependencies(job, visited = []) {
	if (!job || visited.includes(job.name)) {
		return;
	}
	visited.push(job.name);
	job.export = true;
	for (const memberName of checkArray(job.jobsname)) {
		includeJobDependencies(
			values.jobs.find(({ name }) => name == memberName),
			visited
		);
	}
}

let values = {
	get waiting() {
		return waiting;
	},

	async configure(e) {
		e.preventDefault?.();
		const params = e.preventDefault ? new FormData(e.target) : e;
		await call('scheduler.CreateScheduledElements', params);
		values.refresh();
	},
	async remove(exname, type) {
		await call('scheduler.CreateScheduledElements', {
			del: 'true',
			exname,
			type: `schedulerNew${type}`
		});
		values.refresh();
	},
	async importScheduler(event) {
		const res = await doCall('Import', event);
		return !res.isError;
	},
	async exportScheduler() {
		const elements = [
			...values.jobs
				.filter(({ export: selected }) => selected)
				.map(({ name }) => ({ category: 'jobs', name })),
			...values.schedules
				.filter(({ export: selected }) => selected)
				.map(({ name }) => ({ category: 'schedules', name })),
			...values.scheduled
				.filter(({ export: selected }) => selected)
				.map(({ name }) => ({ category: 'scheduledJobs', name }))
		];
		const res = await doCall('Export', { elements: JSON.stringify(elements) });
		return !res.isError;
	},
	selectForExport(category, row, selected) {
		row.export = selected;
		if (!selected) {
			return;
		}
		if (category == 'jobs') {
			includeJobDependencies(row);
		} else if (category == 'scheduledJobs') {
			includeJobDependencies(values.jobs.find(({ name }) => name == row.jobName));
			const schedule = values.schedules.find(({ name }) => name == row.scheduleName);
			if (schedule) {
				schedule.export = true;
			}
		}
	}
};

export default ServiceHelper({
	defValues,
	values,
	arrays: ['admin.element'],
	service: 'scheduler.List',
	mapping: { element: 'admin.element' },
	beforeUpdate: ({ element }) => {
		for (const schedulerElement of element) {
			schedulerElement.export = false;
		}
		for (const job of element.filter(({ type }) => String(type ?? '').endsWith('ConvertigoJob'))) {
			job.parameterMap = normalizeParameters(job.parameter);
		}
		for (const job of element.filter(({ type }) => type == 'JobGroupJob')) {
			job.jobsname = checkArray(job.job_group_member);
			delete job.job_group_member;
		}
		const schedules = element.filter(({ category }) => category == 'schedules');
		schedules.forEach((schedule, i) => {
			if (schedule.cron) {
				schedule.next = null;
				call('scheduler.CronCalculator', {
					name: schedule.name,
					input: schedule.info,
					iteration: 20
				}).then((res) => {
					values.schedules[i].next = checkArray(res?.admin?.crons?.nextTime);
				});
			} else {
				schedule.next = ['n/a'];
			}
		});
		return {
			jobs: element.filter(({ category }) => category == 'jobs'),
			scheduled: element.filter(({ category }) => category == 'scheduledJobs'),
			schedules
		};
	}
});
