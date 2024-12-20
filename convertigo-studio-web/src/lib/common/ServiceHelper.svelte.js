import { browser } from '$app/environment';
import { call, checkArray, getNestedProperty, setNestedProperty } from '$lib/utils/service';
import { untrack } from 'svelte';

/**
 * @param {{values?: any, defValues?: any, service?: any, params?: any, delay?: number, arrays?: any[], mapping?: any, beforeUpdate?: (data: any) => any}} param0
 */
export default function ({
	values = {},
	defValues = {},
	service = false,
	params = {},
	delay = -1,
	arrays = [],
	mapping = {},
	beforeUpdate = (data) => data
}) {
	let interval;
	let _delay = $state(-1);
	let _values = $state({ ...defValues });
	let _needRefresh = $state(true && service);
	let _calling = $state(false);

	values.reset = () => {
		Object.assign(_values, { ...defValues });
	};
	Object.defineProperty(values, 'loading', {
		get() {
			return _needRefresh || _calling;
		}
	});
	Object.defineProperty(values, 'init', {
		get() {
			return !_needRefresh;
		}
	});
	values.refresh = async () => {
		if (!browser || _calling || !service) {
			return;
		}
		_calling = true;
		try {
			const res = await (typeof service == 'string' ? call(service, params) : service(params));
			if (res) {
				if (res.isError) {
					Object.assign(_values, { ...defValues, res });
					_needRefresh = true;
				} else {
					for (const array of arrays) {
						let prop = getNestedProperty(res, array);
						prop = checkArray(prop);
						setNestedProperty(res, array, prop);
					}
					if (Object.keys(mapping).length) {
						let _res = {};
						for (const key in mapping) {
							if (key == '') {
								_res = getNestedProperty(res, mapping[key]);
							} else {
								_res[key] = getNestedProperty(res, mapping[key]);
							}
						}
						_values = beforeUpdate(_res) ?? _res;
					} else {
						_values = beforeUpdate(res) ?? res;
					}
					if (_needRefresh) {
						values.delay = delay;
						_needRefresh = false;
					}
				}
			}
		} catch (error) {
			_needRefresh = true;
		}
		_calling = false;
	};
	Object.keys(defValues).forEach((key) => {
		Object.defineProperty(values, key, {
			get() {
				if (_needRefresh) {
					untrack(values.refresh);
				}
				return _values?.[key];
			},
			set(v) {
				_values[key] = v;
			}
		});
	});
	Object.defineProperty(values, 'delay', {
		get() {
			return _delay;
		},
		set(value) {
			if (browser) {
				window.clearInterval(interval);
				if (value > 0) {
					_delay = value;
					interval = window.setInterval(values.refresh, value);
				}
			}
		}
	});
	values.stop = () => {
		if (browser) {
			window.clearInterval(interval);
		}
		if (service) {
			_needRefresh = true;
		}
	};
	return values;
}
