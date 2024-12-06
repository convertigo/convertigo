/* eslint-disable quote-props */
export const emulatedDevices = [
	{
		order: 10,
		showByDefault: false,
		title: 'iPhone SE',
		screen: {
			horizontal: { width: 667, height: 375 },
			devicePixelRatio: 2,
			vertical: { width: 375, height: 667 }
		},
		capabilities: ['touch', 'mobile'],
		userAgent:
			'Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1',
		type: 'phone',
        bezel: null
	},
	{
		order: 12,
		showByDefault: false,
		title: 'iPhone XR',
		screen: {
			horizontal: { width: 896, height: 414 },
			devicePixelRatio: 2,
			vertical: { width: 414, height: 896 }
		},
		capabilities: ['touch', 'mobile'],
		userAgent:
			'Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1',
		type: 'phone',
        bezel: null
	},
	{
		order: 14,
		showByDefault: true,
		title: 'iPhone 12 Pro',
		screen: {
			horizontal: { width: 844, height: 390 },
			devicePixelRatio: 3,
			vertical: { width: 390, height: 844 }
		},
		capabilities: ['touch', 'mobile'],
		userAgent:
			'Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1',
		type: 'phone',
        bezel: 'Bezel.png'
	},
	{
		order: 15,
		showByDefault: false,
		title: 'iPhone 14 Pro Max',
		screen: {
			horizontal: { width: 932, height: 430 },
			devicePixelRatio: 3,
			vertical: { width: 430, height: 932 }
		},
		capabilities: ['touch', 'mobile'],
		userAgent:
			'Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1',
		type: 'phone'
	},
	{
		order: 16,
		showByDefault: false,
		title: 'Pixel 3 XL',
		screen: {
			horizontal: { width: 786, height: 393 },
			devicePixelRatio: 2.75,
			vertical: { width: 393, height: 786 }
		},
		capabilities: ['touch', 'mobile'],
		userAgent:
			'Mozilla/5.0 (Linux; Android 11; Pixel 3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.181 Mobile Safari/537.36',
		userAgentMetadata: {
			platform: 'Android',
			platformVersion: '11',
			architecture: '',
			model: 'Pixel 3',
			mobile: true
		},
		type: 'phone'
	},
	{
		order: 18,
		showByDefault: true,
		title: 'Pixel 7',
		screen: {
			horizontal: { width: 915, height: 412 },
			devicePixelRatio: 2.625,
			vertical: { width: 412, height: 915 }
		},
		capabilities: ['touch', 'mobile'],
		userAgent:
			'Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36',
		userAgentMetadata: {
			platform: 'Android',
			platformVersion: '13',
			architecture: '',
			model: 'Pixel 5',
			mobile: true
		},
		type: 'phone'
	},
	{
		order: 20,
		showByDefault: true,
		title: 'Samsung Galaxy S8+',
		screen: {
			horizontal: { width: 740, height: 360 },
			devicePixelRatio: 4,
			vertical: { width: 360, height: 740 }
		},
		capabilities: ['touch', 'mobile'],
		userAgent:
			'Mozilla/5.0 (Linux; Android 8.0.0; SM-G955U Build/R16NW) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36',
		userAgentMetadata: {
			platform: 'Android',
			platformVersion: '8.0.0',
			architecture: '',
			model: 'SM-G955U',
			mobile: true
		},
		type: 'phone'
	},
	{
		order: 24,
		showByDefault: true,
		title: 'Samsung Galaxy S20 Ultra',
		screen: {
			horizontal: { width: 915, height: 412 },
			devicePixelRatio: 3.5,
			vertical: { width: 412, height: 915 }
		},
		capabilities: ['touch', 'mobile'],
		userAgent:
			'Mozilla/5.0 (Linux; Android 13; SM-G981B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36',
		userAgentMetadata: {
			platform: 'Android',
			platformVersion: '13',
			architecture: '',
			model: 'SM-G981B',
			mobile: true
		},
		type: 'phone'
	},
	{
		order: 26,
		showByDefault: true,
		title: 'iPad Mini',
		screen: {
			horizontal: { width: 1024, height: 768 },
			devicePixelRatio: 2,
			vertical: { width: 768, height: 1024 }
		},
		capabilities: ['touch', 'mobile'],
		userAgent:
			'Mozilla/5.0 (iPad; CPU OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1',
		type: 'tablet'
	},
	{
		order: 30,
		showByDefault: true,
		title: 'Surface Pro 7',
		screen: {
			horizontal: {
				width: 1368,
				height: 912
			},
			devicePixelRatio: 2,
			vertical: {
				width: 912,
				height: 1368
			}
		},
		capabilities: ['touch', 'mobile'],
		userAgent:
			'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Safari/537.36',
		type: 'tablet'
	}
];
