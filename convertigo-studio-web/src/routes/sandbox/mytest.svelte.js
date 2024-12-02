import ServiceHelperSvelte from '$lib/common/ServiceHelper.svelte';

const defValues = {
	name: 'John',
	age: 25,
	rules: ['a', 'b']
};

//let value = $state({...defValues});
// let values = {
// 	reset: () => {
// 		Object.assign(val, { ...defValues });
// 	}
// };
let val = ServiceHelperSvelte({ defValues });
// Object.keys(defValues).forEach(key => {
//     Object.defineProperty(val, key, {
//         get() {
//             return value[key];
//         },
//         set(v) {
//             value[key] = v;
//         }
//     });
// });
// let val = {
//     get ['name']() {
//         return value.name;
//     },
//     get ['age']() {
//         return value.age;
//     },
//     set ['age'](v) {
//         value.age = v;
//     }

// };

export default val;
