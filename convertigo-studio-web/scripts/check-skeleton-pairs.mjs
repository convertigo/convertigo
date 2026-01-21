#!/usr/bin/env node
import { readdir, readFile } from 'node:fs/promises';
import path from 'node:path';

const allowedSteps = new Set([50, 100, 200, 300, 400, 600, 700, 800, 900, 950]);
const ignoredDirs = new Set([
	'.git',
	'.svelte-kit',
	'.vite',
	'.output',
	'node_modules',
	'dist',
	'build',
	'coverage',
	'tmp'
]);

const roots = process.argv.slice(2);
const scanRoots = roots.length ? roots : ['src'];
const pairRegex = /[A-Za-z][A-Za-z0-9_-]*-(\d{2,3})-(\d{2,3})/g;

/** @param {string} filePath */
async function checkFile(filePath) {
	const content = await readFile(filePath, 'utf8');
	const issues = [];
	let match;

	while ((match = pairRegex.exec(content))) {
		const first = Number.parseInt(match[1], 10);
		const second = Number.parseInt(match[2], 10);
		const token = match[0];

		if (!Number.isFinite(first) || !Number.isFinite(second)) {
			continue;
		}

		const hasValidSteps = allowedSteps.has(first) && allowedSteps.has(second);
		const sumsToComplement = first + second === 1000;
		const hasForbiddenPair = first === 500 || second === 500;

		if (!hasValidSteps || !sumsToComplement || hasForbiddenPair) {
			const index = match.index;
			const line = content.slice(0, index).split('\n').length;
			const lastNewline = content.lastIndexOf('\n', index - 1);
			const column = index - (lastNewline === -1 ? -1 : lastNewline);
			let reason = 'pair must sum to 1000 using allowed steps';
			if (hasForbiddenPair) {
				reason = '500-500 is not allowed (use single -500 when needed)';
			} else if (!hasValidSteps) {
				reason = 'pair uses a non-standard scale step';
			} else if (!sumsToComplement) {
				reason = 'pair does not sum to 1000';
			}

			issues.push({
				filePath,
				line,
				column,
				token,
				reason
			});
		}
	}

	return issues;
}

/** @param {string} dir */
async function walk(dir) {
	const entries = await readdir(dir, { withFileTypes: true });
	const issues = [];

	for (const entry of entries) {
		if (entry.isDirectory()) {
			if (ignoredDirs.has(entry.name)) {
				continue;
			}
			issues.push(...(await walk(path.join(dir, entry.name))));
			continue;
		}

		if (entry.isFile()) {
			const filePath = path.join(dir, entry.name);
			issues.push(...(await checkFile(filePath)));
		}
	}

	return issues;
}

const allIssues = [];
for (const root of scanRoots) {
	const resolved = path.resolve(process.cwd(), root);
	allIssues.push(...(await walk(resolved)));
}

if (allIssues.length > 0) {
	console.error('Invalid Skeleton scale pairs found:');
	for (const issue of allIssues) {
		console.error(
			`- ${issue.filePath}:${issue.line}:${issue.column} ${issue.token} (${issue.reason})`
		);
	}
	process.exitCode = 1;
} else {
	console.log('Skeleton scale pairs OK.');
}
