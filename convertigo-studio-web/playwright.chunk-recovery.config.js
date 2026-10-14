import { defineConfig } from '@playwright/test';

export default defineConfig({
	testDir: 'tests',
	testMatch: 'chunk-recovery.spec.js',
	workers: 1,
	use: { baseURL: 'http://127.0.0.1:4174', headless: true },
	webServer: {
		command: 'node tests/chunk-recovery-server.mjs',
		url: 'http://127.0.0.1:4174/convertigo/dashboard/Demo/backend/',
		reuseExistingServer: false
	}
});
