// cronStore.js
import { writable } from 'svelte/store';

export const cronExpressionStore = writable('');

// Define the function to update cron settings
export function updateCronExpression(newValue) {
	cronExpressionStore.set(newValue);
}

// Define the cronData writable store
export const cronData = writable({
	minutes: [],
	hours: [],
	daysOfMonth: [],
	months: [],
	daysOfWeek: []
});

// Define the function to update cron settings
export function updateCronSettings(part, values) {
	cronData.update((current) => {
		current[part] = values;
		return current;
	});
}

// Define the function to compile cron expression
export function compileCronExpression(cronValues) {
	return `${cronValues.seconds} ${cronValues.minutes} ${cronValues.hours} ${cronValues.daysOfMonth} ${cronValues.months} ${cronValues.daysOfWeek}`;
}

/**
export let cronExpression = derived(cronData, $data => {
    return compileCronExpression({
        seconds: '0',
        minutes: $data.minutes.join('-'),
        hours: $data.hours.join('-'),
        daysOfMonth: $data.daysOfMonth.join('-'),
        months: $data.months.join('-'),
        daysOfWeek: $data.daysOfWeek.join('-')
    });
});

function createRange(selections) {
    if (!selections || selections.length === 0) return '';

    selections.sort((a, b) => a - b);
    const newRange = [];
    let i = 0;
    while (i < selections.length) {
        let start = selections[i];
        let end = start;
        let inc = 1;
        while (i + inc < selections.length && +selections[i + inc] === +selections[i] + inc) {
            end = +selections[i + inc];
            inc++;
        }
        newRange.push(start === end ? start.toString() : `${start}-${end}`);
        i += inc;
    }
    return newRange.join(',');
}


function createCronSettings() {
    const settings = writable({
        minutes: [],
        hours: [],
        daysOfMonth: [],
        months: [],
        daysOfWeek: []
    });

    return {
        subscribe: settings.subscribe,
        set: (newValues) => {
            settings.set(newValues);
        },
        updateSettings: (part, values) => {
            settings.update(current => {
                current[part] = values;
                return current;
            });
        },
        getRange: (part) => {
            let range = '';
            settings.subscribe(values => {
                range = createRange(values[part]);
            })();
            return range;
        }
    };
}
export const cronSettings = createCronSettings();
 */

/**
function createCronStore() {
    const store = writable({
        minute: '0',
        hour: '0',
        dayOfMonth: '0',
        month: '*',
        dayOfWeek: '*',
    });

    return {
        subscribe: store.subscribe,
        updateCronPart: (part, value) => {
            store.update((cron) => {
                if (part === 'dayOfMonth' && value !== '?') {
                    // If dayOfMonth is specified, set dayOfWeek to '?'
                    cron.dayOfWeek = '?';
                } else if (part === 'dayOfWeek' && value !== '?') {
                    // If dayOfWeek is specified, set dayOfMonth to '?'
                    cron.dayOfMonth = '?';
                }
                cron[part] = value;
                return cron;
            });
        },
        compileCronExpression: () => {
            const cron = get(store);
            return `${cron.minute} ${cron.hour} ${cron.dayOfMonth} ${cron.month} ${cron.dayOfWeek}`;
        }
    };

}

export const cronStore = createCronStore();
 */
