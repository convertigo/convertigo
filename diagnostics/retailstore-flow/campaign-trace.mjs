#!/usr/bin/env node

import { createHash } from 'node:crypto';
import { readFile } from 'node:fs/promises';
import { performance } from 'node:perf_hooks';
import { fileURLToPath } from 'node:url';
import { resolve } from 'node:path';
import readline from 'node:readline';

function stable(value) {
  if (Array.isArray(value)) return value.map(stable);
  if (value && typeof value === 'object') {
    return Object.fromEntries(Object.keys(value).sort().map((key) => [key, stable(value[key])]));
  }
  return value;
}

function fingerprint(value) {
  return createHash('sha256').update(JSON.stringify(stable(value))).digest('hex').slice(0, 16);
}

function percentile(values, ratio) {
  if (!values.length) return null;
  const sorted = [...values].sort((left, right) => left - right);
  const position = (sorted.length - 1) * ratio;
  const lower = Math.floor(position);
  const upper = Math.ceil(position);
  const interpolated = sorted[lower] + (sorted[upper] - sorted[lower]) * (position - lower);
  return Math.round(interpolated);
}

function summaryStats(values) {
  return {
    samples: values.length,
    totalMs: values.length ? Math.round(values.reduce((sum, value) => sum + value, 0)) : null,
    medianMs: percentile(values, 0.5),
    p95Ms: percentile(values, 0.95),
    maxMs: values.length ? Math.round(Math.max(...values)) : null
  };
}

function structuredError(item) {
  const result = item?.result;
  return result?.structured_content?.error
    || result?.structuredContent?.error
    || result?.structured_content?.result?.error
    || null;
}

function errorSignature(item) {
  const domain = structuredError(item);
  if (domain) return `domain:${domain.code || domain.message || 'unknown'}`;
  if (item?.error) {
    const message = typeof item.error === 'string' ? item.error : item.error.message;
    return `transport:${String(message || 'unknown').split('\n')[0].slice(0, 240)}`;
  }
  if (item?.status === 'failed') return 'transport:failed-without-message';
  return null;
}

function isFailed(item) {
  if (!item) return false;
  if (item.status === 'failed' || item.error || structuredError(item)) return true;
  return item.type === 'command_execution' && Number.isInteger(item.exit_code) && item.exit_code !== 0;
}

function eventTime(record) {
  const value = record.event?._campaign?.observedAt;
  const parsed = value ? Date.parse(value) : Number.NaN;
  return Number.isFinite(parsed) ? parsed : null;
}

function campaignMeta(record) {
  return record.event?._campaign || {};
}

function firstMilestone(milestones, name, record, evidence) {
  if (milestones[name]) return;
  milestones[name] = {
    observedAt: campaignMeta(record).observedAt || null,
    segment: campaignMeta(record).segment || null,
    evidence
  };
}

export function analyzeRecords(records) {
  const starts = new Map();
  const toolRows = [];
  const commandRows = [];
  const modelInterruptions = [];
  const segmentExits = [];
  const segments = new Map();
  const milestones = {};
  const browserMetrics = [];

  for (const record of records) {
    const event = record.event;
    const meta = campaignMeta(record);
    const segmentKey = `${meta.run || 'legacy'}:${meta.segment || record.source || 'unknown'}`;
    const timestamp = eventTime(record);
    if (!segments.has(segmentKey)) {
      segments.set(segmentKey, {
        run: meta.run || null,
        segment: meta.segment || record.source || null,
        firstAt: timestamp,
        lastAt: timestamp,
        maxElapsedMs: Number.isFinite(meta.elapsedMs) ? meta.elapsedMs : null,
        events: 0
      });
    }
    const segment = segments.get(segmentKey);
    segment.events += 1;
    if (timestamp !== null) {
      segment.firstAt = segment.firstAt === null ? timestamp : Math.min(segment.firstAt, timestamp);
      segment.lastAt = segment.lastAt === null ? timestamp : Math.max(segment.lastAt, timestamp);
    }
    if (Number.isFinite(meta.elapsedMs)) {
      segment.maxElapsedMs = segment.maxElapsedMs === null
        ? meta.elapsedMs
        : Math.max(segment.maxElapsedMs, meta.elapsedMs);
    }
    if (event?.type === 'campaign.segment_exit' && timestamp !== null && Number.isFinite(meta.elapsedMs)) {
      segment.firstAt = Math.min(segment.firstAt ?? timestamp, timestamp - meta.elapsedMs);
      segmentExits.push({
        segment: meta.segment || null,
        exitCode: Number(event.exitCode),
        observedAt: meta.observedAt || null
      });
    }

    if (event?.type === 'item.started' && event.item?.id) {
      starts.set(`${record.source}:${event.item.id}`, record);
      continue;
    }

    if (event?.type === 'error' || event?.type === 'turn.failed') {
      modelInterruptions.push({
        observedAt: meta.observedAt || null,
        segment: meta.segment || null,
        message: String(event.message || event.error?.message || event.error || 'unknown').slice(0, 500)
      });
      continue;
    }

    if (event?.type !== 'item.completed') continue;
    const item = event.item || {};
    const started = starts.get(`${record.source}:${item.id}`);
    const startedAt = started ? eventTime(started) : null;
    const durationMs = startedAt !== null && timestamp !== null ? Math.max(0, timestamp - startedAt) : null;

    if (item.type === 'mcp_tool_call') {
      const failed = isFailed(item);
      const row = {
        tool: item.tool || 'unknown',
        argumentsHash: fingerprint(item.arguments || {}),
        failed,
        errorSignature: failed ? errorSignature(item) : null,
        durationMs,
        observedAt: meta.observedAt || null,
        segment: meta.segment || null,
        arguments: item.arguments || {}
      };
      toolRows.push(row);

      if (!failed) {
        if (row.tool === 'flow-project-bootstrap') firstMilestone(milestones, 'bootstrap', record, row.tool);
        if (row.tool === 'flow-fullsync-scaffold' && row.arguments.dryRun === true) {
          firstMilestone(milestones, 'scaffoldDryRun', record, row.tool);
        }
        if (row.tool === 'flow-fullsync-scaffold' && row.arguments.dryRun === false) {
          firstMilestone(milestones, 'scaffoldApply', record, row.tool);
        }
        if (row.tool === 'code-run') {
          firstMilestone(milestones, 'firstFlowRun', record, row.arguments.qname || row.arguments.flowName || row.tool);
          if (/init|seed|catalog/i.test(String(row.arguments.qname || row.arguments.flowName || ''))) {
            firstMilestone(milestones, 'firstInitializationRun', record, row.arguments.qname || row.arguments.flowName);
          }
        }
        if (row.tool === 'frontend-svelte-fullsync-schema') {
          firstMilestone(milestones, 'firstFullSyncSchema', record, row.tool);
        }
        if (row.tool === 'frontend-svelte-action' && /build/i.test(String(row.arguments.actionId || ''))) {
          firstMilestone(milestones, 'productionBuild', record, row.arguments.actionId);
        }
      }
      continue;
    }

    if (item.type === 'command_execution') {
      const failed = isFailed(item);
      const command = String(item.command || '');
      commandRows.push({
        command: command.slice(0, 500),
        failed,
        exitCode: item.exit_code ?? null,
        durationMs,
        observedAt: meta.observedAt || null,
        segment: meta.segment || null
      });
      const output = String(item.aggregated_output || '');
      for (const match of output.matchAll(/CAMPAIGN_BROWSER_METRICS=(\{[^\n]+\})/g)) {
        try {
          browserMetrics.push({
            observedAt: meta.observedAt || null,
            segment: meta.segment || null,
            metrics: JSON.parse(match[1])
          });
        } catch {}
      }
      if (!failed && /playwright(?:\s+|[^\n]*\s)test(?:\s+|$)/i.test(command)) {
        firstMilestone(milestones, 'firstPlaywrightPass', record, command.slice(0, 160));
      }
    }
  }

  const groupedTools = {};
  for (const row of toolRows) {
    groupedTools[row.tool] ||= { calls: 0, failures: 0, durations: [] };
    groupedTools[row.tool].calls += 1;
    if (row.failed) groupedTools[row.tool].failures += 1;
    if (row.durationMs !== null) groupedTools[row.tool].durations.push(row.durationMs);
  }
  const tools = Object.fromEntries(Object.entries(groupedTools)
    .sort(([, left], [, right]) => right.calls - left.calls)
    .map(([tool, value]) => [tool, {
      calls: value.calls,
      failures: value.failures,
      latency: summaryStats(value.durations)
    }]));

  const attempts = new Map();
  for (const row of toolRows) {
    const key = `${row.tool}:${row.argumentsHash}`;
    if (!attempts.has(key)) attempts.set(key, []);
    attempts.get(key).push(row);
  }
  let retryCalls = 0;
  let recoveredFailures = 0;
  for (const rows of attempts.values()) {
    retryCalls += Math.max(0, rows.length - 1);
    if (rows.some((row) => row.failed) && rows.some((row) => !row.failed)) recoveredFailures += 1;
  }

  const hardPointMap = new Map();
  const addHardPoint = (category, signature, durationMs) => {
    const key = `${category}:${signature}`;
    if (!hardPointMap.has(key)) hardPointMap.set(key, { category, signature, count: 0, durations: [] });
    const value = hardPointMap.get(key);
    value.count += 1;
    if (durationMs !== null) value.durations.push(durationMs);
  };
  for (const row of toolRows.filter((row) => row.failed)) {
    addHardPoint('mcp', row.errorSignature || 'unknown', row.durationMs);
  }
  for (const row of commandRows.filter((row) => row.failed)) {
    addHardPoint('command', `exit:${row.exitCode}:${row.command.split('\n')[0].slice(0, 160)}`, row.durationMs);
  }
  for (const interruption of modelInterruptions) {
    const signature = /capacity/i.test(interruption.message)
      ? 'model-capacity'
      : interruption.message.split('\n')[0].slice(0, 200);
    addHardPoint('model', signature, null);
  }
  for (const segmentExit of segmentExits.filter((entry) => entry.exitCode !== 0)) {
    addHardPoint('process', `segment-exit:${segmentExit.exitCode}`, null);
  }
  const hardPoints = [...hardPointMap.values()]
    .sort((left, right) => right.count - left.count)
    .map((value) => ({
      category: value.category,
      signature: value.signature,
      count: value.count,
      latency: summaryStats(value.durations)
    }));

  const segmentRows = [...segments.values()].map((segment) => ({
    run: segment.run,
    segment: segment.segment,
    firstObservedAt: segment.firstAt === null ? null : new Date(segment.firstAt).toISOString(),
    lastObservedAt: segment.lastAt === null ? null : new Date(segment.lastAt).toISOString(),
    observedDurationMs: segment.maxElapsedMs === null
      ? (segment.firstAt !== null && segment.lastAt !== null ? segment.lastAt - segment.firstAt : null)
      : Math.round(segment.maxElapsedMs),
    events: segment.events
  }));
  const timedSegments = segmentRows.filter((segment) => segment.firstObservedAt && segment.lastObservedAt);
  const firstAt = timedSegments.length ? Math.min(...timedSegments.map((segment) => Date.parse(segment.firstObservedAt))) : null;
  const lastAt = timedSegments.length ? Math.max(...timedSegments.map((segment) => Date.parse(segment.lastObservedAt))) : null;
  const activeMs = timedSegments.length
    ? timedSegments.reduce((sum, segment) => sum + (segment.observedDurationMs || 0), 0)
    : null;
  const wallMs = firstAt === null ? null : lastAt - firstAt;
  for (const milestone of Object.values(milestones)) {
    const timestamp = milestone.observedAt ? Date.parse(milestone.observedAt) : Number.NaN;
    milestone.elapsedWallMs = firstAt !== null && Number.isFinite(timestamp) ? timestamp - firstAt : null;
  }

  return {
    schemaVersion: 1,
    timing: {
      available: timedSegments.length > 0,
      firstObservedAt: firstAt === null ? null : new Date(firstAt).toISOString(),
      lastObservedAt: lastAt === null ? null : new Date(lastAt).toISOString(),
      wallMs,
      observedActiveMs: activeMs,
      interSegmentGapMs: wallMs === null || activeMs === null ? null : Math.max(0, wallMs - activeMs),
      segments: segmentRows
    },
    counts: {
      events: records.length,
      mcpCalls: toolRows.length,
      failedMcpCalls: toolRows.filter((row) => row.failed).length,
      retryCalls,
      recoveredFailures,
      frontendMutations: toolRows.filter((row) => row.tool === 'frontend-svelte-mutate').length,
      failedFrontendMutations: toolRows.filter((row) => row.tool === 'frontend-svelte-mutate' && row.failed).length,
      commandRuns: commandRows.length,
      failedCommandRuns: commandRows.filter((row) => row.failed).length,
      modelInterruptions: modelInterruptions.length,
      failedSegments: segmentExits.filter((entry) => entry.exitCode !== 0).length
    },
    milestones,
    browserMetrics,
    tools,
    hardPoints,
    modelInterruptions
  };
}

async function stamp(args) {
  const runIndex = args.indexOf('--run');
  const segmentIndex = args.indexOf('--segment');
  const run = runIndex >= 0 ? args[runIndex + 1] : null;
  const segment = segmentIndex >= 0 ? args[segmentIndex + 1] : null;
  if (!run || !segment) throw new Error('stamp requires --run <id> and --segment <id>');

  const start = performance.now();
  const input = readline.createInterface({ input: process.stdin, crlfDelay: Infinity });
  for await (const line of input) {
    if (!line.trim()) continue;
    let event;
    try {
      event = JSON.parse(line);
    } catch {
      event = { type: 'campaign.unparsed', raw: line };
    }
    event._campaign = {
      run,
      segment,
      observedAt: new Date().toISOString(),
      elapsedMs: Math.round(performance.now() - start)
    };
    process.stdout.write(`${JSON.stringify(event)}\n`);
  }
}

async function analyze(files) {
  if (!files.length) throw new Error('analyze requires at least one JSONL file');
  const records = [];
  for (const source of files) {
    const content = await readFile(source, 'utf8');
    for (const line of content.split(/\r?\n/)) {
      if (!line.trim()) continue;
      try {
        records.push({ source, event: JSON.parse(line) });
      } catch {
        records.push({ source, event: { type: 'campaign.unparsed', raw: line } });
      }
    }
  }
  process.stdout.write(`${JSON.stringify(analyzeRecords(records), null, 2)}\n`);
}

function argument(args, name) {
  const index = args.indexOf(name);
  return index >= 0 ? args[index + 1] : null;
}

async function segmentExit(args) {
  const run = argument(args, '--run');
  const segment = argument(args, '--segment');
  const status = Number(argument(args, '--status'));
  const startedMs = Number(argument(args, '--started-ms'));
  if (!run || !segment || !Number.isFinite(status) || !Number.isFinite(startedMs)) {
    throw new Error('segment-exit requires --run, --segment, --status and --started-ms');
  }
  const event = {
    type: 'campaign.segment_exit',
    exitCode: status,
    _campaign: {
      run,
      segment,
      observedAt: new Date().toISOString(),
      elapsedMs: Math.max(0, Date.now() - startedMs)
    }
  };
  process.stdout.write(`${JSON.stringify(event)}\n`);
}

async function main() {
  const [command, ...args] = process.argv.slice(2);
  if (command === 'stamp') return stamp(args);
  if (command === 'analyze') return analyze(args);
  if (command === 'segment-exit') return segmentExit(args);
  throw new Error('usage: campaign-trace.mjs stamp --run <id> --segment <id> | analyze <trace...> | segment-exit ...');
}

if (process.argv[1] && fileURLToPath(import.meta.url) === resolve(process.argv[1])) {
  main().catch((error) => {
    process.stderr.write(`${error.message}\n`);
    process.exitCode = 1;
  });
}
