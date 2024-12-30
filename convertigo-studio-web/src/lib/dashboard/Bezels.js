const devices = {
	'iPhone-15': {
		title: 'iPhone 15',
		iframe: { width: 393, height: 852, marginTop: 20, marginLeft: 24, borderRadius: 20 },
		bezel: { width: 445, height: 897 },
		type: 'phone'
	},
	'iPhone-15-Plus': {
		title: 'iPhone 15 Plus',
		iframe: { width: 430, height: 932, marginTop: 23, marginLeft: 24, borderRadius: 20 },
		bezel: { width: 487, height: 983 },
		type: 'phone'
	},
	'Google-Pixel-8-Pro': {
		title: 'Google Pixel 8 Pro',
		iframe: { width: 412, height: 915, marginTop: 17, marginLeft: 16, borderRadius: 20 },
		bezel: { width: 452, height: 950 },
		type: 'phone'
	},
	// 'Desktop-HIDPI': {
	// 	title: 'Desktop HIPDI',
	// 	iframe: { width: 1920, height: 1080, marginTop: 63, marginLeft: 63 },
	// 	bezel: { width: 2046, height: 1361 },
	// 	type: 'desktop'
	// },
	'Galaxy-S24': {
		title: 'Galaxy S24',
		iframe: { width: 360, height: 780, marginTop: 13, marginLeft: 13, borderRadius: 20 },
		bezel: { width: 390, height: 804 },
		type: 'phone'
	},
	'Galaxy-S24-Plus': {
		title: 'Galaxy S24 Plus',
		iframe: { width: 384, height: 832, marginTop: 13, marginLeft: 13, borderRadius: 20 },
		bezel: { width: 415, height: 858 },
		type: 'phone'
	},
	'Galaxy-S24-Ultra': {
		title: 'Galaxy S24 Ultra',
		iframe: { width: 384, height: 824, marginTop: 13, marginLeft: 13, borderRadius: 5 },
		bezel: { width: 408, height: 846 },
		type: 'phone'
	},
	'iPad-Pro-11-2021': {
		title: 'iPad Pro 11 (2021)',
		iframe: { width: 834, height: 1194, marginTop: 46, marginLeft: 46, borderRadius: 20 },
		bezel: { width: 925, height: 1283 },
		type: 'phone'
	},
	'iPad-Pro-12-2021': {
		title: 'iPad Pro-12 (2021))',
		iframe: { width: 1024, height: 1366, marginTop: 46, marginLeft: 46, borderRadius: 20 },
		bezel: { width: 1120, height: 1460 },
		type: 'phone'
	},
	'iPad-4-Mini': {
		title: 'iPad 4 Mini',
		iframe: { width: 768, height: 1024, marginTop: 136, marginLeft: 46, borderRadius: 20 },
		bezel: { width: 871, height: 1288 },
		type: 'phone'
	},
	'iPad-10': {
		title: 'iPad 10',
		iframe: { width: 810, height: 1080, marginTop: 119, marginLeft: 63, borderRadius: 5 },
		bezel: { width: 923, height: 1308 },
		type: 'phone'
	},
	'iPhone-se-2022': {
		title: 'iPhone SE 2022',
		iframe: { width: 375, height: 667, marginTop: 120, marginLeft: 21, borderRadius: 5 },
		bezel: { width: 443, height: 912 },
		type: 'phone'
	},
	none: {
		title: 'None',
		iframe: { width: '100%', height: '100%', position: 'relative' },
		bezel: { width: '100%', height: '100%', position: 'relative' },
		type: 'desktop'
	}
};

let index = 0;
for (const device in devices) {
	devices[device].id = device;
	devices[device].index = index++;
}

export default devices;
