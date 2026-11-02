import { defineConfig } from '@playwright/test';

export default defineConfig({
	testDir: 'tests',
	testMatch: 'javascript-debugger.spec.js',
	workers: 1,
	use: { headless: true, viewport: { width: 1600, height: 1000 } },
	reporter: [['list']]
});
