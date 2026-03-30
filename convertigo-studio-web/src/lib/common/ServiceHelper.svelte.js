import { browser } from '$app/environment';
import Instances from '$lib/admin/Instances.svelte';
import Authentication from '$lib/common/Authentication.svelte';
import { call, checkArray, getNestedProperty, setNestedProperty } from '$lib/utils/service';
import { untrack } from 'svelte';

/**
 * @param {{values?: any, defValues?: any, service?: any, params?: any, delay?: number, arrays?: any[], mapping?: any, beforeUpdate?: (data: any) => any, needAuth?: boolean | (() => boolean), onInstanceChange?: () => void}} param0
 */
export default function ({
	values = {},
	defValues = {},
	service = false,
	params = {},
	delay = -1,
	arrays = [],
	mapping = {},
	beforeUpdate = (data) => data,
	needAuth = true,
	onInstanceChange = () => {}
}) {
	let interval;
	let stopAuthEffect;
	let _delay = $state(-1);
	let _values = $state({ ...defValues });
	let _needRefresh = $state(Boolean(service));
	let _calling = $state(false);
	let _instanceRevision = $state(Instances.revision);
	let _callSerial = 0;
	const resetValues = (next = defValues) => {
		_values = { ...next };
	};
	const hasRequiredAuth = () => {
		if (typeof needAuth == 'function') {
			return needAuth();
		}
		return !needAuth || Authentication.authenticated;
	};
	const stopInterval = () => {
		if (browser) {
			window.clearInterval(interval);
		}
		_delay = -1;
	};
	const resetForInstanceChange = () => {
		stopInterval();
		stopAuthEffect?.();
		stopAuthEffect = undefined;
		resetValues();
		_calling = false;
		_needRefresh = Boolean(service);
		_callSerial += 1;
		onInstanceChange();
	};

	const ensureAuthEffect = () => {
		if (!needAuth || !browser || stopAuthEffect) {
			return;
		}
		stopAuthEffect = $effect.root(() => {
			$effect(() => {
				if (hasRequiredAuth() && _needRefresh && !_calling) {
					untrack(values.refresh);
				}
			});
		});
	};
	if (browser) {
		$effect.root(() => {
			$effect(() => {
				const revision = Instances.revision;
				if (revision === _instanceRevision) {
					return;
				}
				_instanceRevision = revision;
				resetForInstanceChange();
			});
		});
	}

	values.reset = () => {
		resetValues();
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
		ensureAuthEffect();
		if (!hasRequiredAuth()) {
			return;
		}
		_calling = true;
		const callSerial = ++_callSerial;
		const callInstanceRevision = _instanceRevision;
		let res = {};
		try {
			res = await (typeof service == 'string' ? call(service, params) : service(params));
			if (callSerial != _callSerial || callInstanceRevision != _instanceRevision) {
				return;
			}
			if (res?.offline) {
				if (_needRefresh) {
					values.delay = delay;
					_needRefresh = false;
				}
				return;
			}
			if (res) {
				if (res.isError) {
					throw res;
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
			if (callSerial != _callSerial || callInstanceRevision != _instanceRevision) {
				return;
			}
			if (error != res) {
				console.error('ServiceHelper error', error);
			}
			values.stop();
			resetValues({ ...defValues, res });
			_needRefresh = false;
			window.setTimeout(() => (_needRefresh = true), 10000);
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
			stopInterval();
			if (browser && value > 0) {
				_delay = value;
				interval = window.setInterval(values.refresh, value);
			}
		}
	});
	values.stop = () => {
		stopInterval();
		if (service) {
			_needRefresh = true;
		}
		stopAuthEffect?.();
		stopAuthEffect = undefined;
	};
	return values;
}
