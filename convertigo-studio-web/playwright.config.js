/** @type {import('@playwright/test').PlaywrightTestConfig} */
const config = {
	webServer: [
		{ command: 'npm run build && npm run preview', port: 4173 },
		{
			command: 'node tests/chunk-recovery-server.mjs',
			url: 'http://127.0.0.1:4174/convertigo/dashboard/Demo/backend/'
		}
	],
	use: { baseURL: 'http://localhost:4173' },
	testDir: 'tests',
	testMatch: /(.+\.)?(test|spec)\.[jt]s/
};

export default config;
