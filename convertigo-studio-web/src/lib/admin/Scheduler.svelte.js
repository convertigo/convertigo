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

let values = {
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
	}
};

export default ServiceHelper({
	defValues,
	values,
	arrays: ['admin.element'],
	service: 'scheduler.List',
	mapping: { element: 'admin.element' },
	beforeUpdate: ({ element }) => {
		for (const job of element.filter(({ type }) => type == 'JobGroupJob')) {
			job.jobsname = checkArray(job.job_group_member);
			delete job.job_group_member;
		}
		return {
			jobs: element.filter(({ category }) => category == 'jobs'),
			scheduled: element.filter(({ category }) => category == 'scheduledJobs'),
			schedules: element.filter(({ category }) => category == 'schedules')
		};
	}
});
