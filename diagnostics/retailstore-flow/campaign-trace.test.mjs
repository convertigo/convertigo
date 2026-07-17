import assert from 'node:assert/strict';
import test from 'node:test';

import { analyzeRecords } from './campaign-trace.mjs';

function record(source, elapsedMs, event) {
  event._campaign = {
    run: 'run6',
    segment: source,
    observedAt: new Date(Date.UTC(2026, 6, 17, 8, 0, 0) + elapsedMs).toISOString(),
    elapsedMs
  };
  return { source, event };
}

test('summarizes durations, retries, recoveries and milestones', () => {
  const records = [
    record('authoring', 0, { type: 'thread.started' }),
    record('authoring', 10, {
      type: 'item.started',
      item: { id: 'one', type: 'mcp_tool_call', tool: 'flow-fullsync-scaffold' }
    }),
    record('authoring', 40, {
      type: 'item.completed',
      item: {
        id: 'one',
        type: 'mcp_tool_call',
        tool: 'flow-fullsync-scaffold',
        arguments: { project: 'sample_RetailStoreFlowRun6', dryRun: true },
        status: 'failed',
        error: { message: 'transport closed' }
      }
    }),
    record('authoring', 50, {
      type: 'item.started',
      item: { id: 'two', type: 'mcp_tool_call', tool: 'flow-fullsync-scaffold' }
    }),
    record('authoring', 90, {
      type: 'item.completed',
      item: {
        id: 'two',
        type: 'mcp_tool_call',
        tool: 'flow-fullsync-scaffold',
        arguments: { project: 'sample_RetailStoreFlowRun6', dryRun: true },
        status: 'completed'
      }
    }),
    record('authoring', 92, {
      type: 'item.started',
      item: { id: 'browser', type: 'command_execution' }
    }),
    record('authoring', 98, {
      type: 'item.completed',
      item: {
        id: 'browser',
        type: 'command_execution',
        command: 'npx playwright test campaign.spec.js',
        exit_code: 0,
        status: 'completed',
        aggregated_output: 'CAMPAIGN_BROWSER_METRICS={"firstUsefulRenderMs":1234}\n'
      }
    }),
    record('authoring', 100, { type: 'error', message: 'Selected model is at capacity.' }),
    record('authoring', 120, { type: 'campaign.segment_exit', exitCode: 7 })
  ];

  const result = analyzeRecords(records);
  assert.equal(result.timing.available, true);
  assert.equal(result.timing.observedActiveMs, 120);
  assert.equal(result.counts.mcpCalls, 2);
  assert.equal(result.counts.failedMcpCalls, 1);
  assert.equal(result.counts.retryCalls, 1);
  assert.equal(result.counts.recoveredFailures, 1);
  assert.equal(result.counts.modelInterruptions, 1);
  assert.equal(result.counts.failedSegments, 1);
  assert.equal(result.tools['flow-fullsync-scaffold'].latency.medianMs, 35);
  assert.equal(result.tools['flow-fullsync-scaffold'].latency.maxMs, 40);
  assert.equal(result.milestones.scaffoldDryRun.evidence, 'flow-fullsync-scaffold');
  assert.equal(result.milestones.firstPlaywrightPass.elapsedWallMs, 98);
  assert.equal(result.browserMetrics[0].metrics.firstUsefulRenderMs, 1234);
  assert.equal(result.hardPoints.some((point) => point.signature === 'model-capacity'), true);
  assert.equal(result.hardPoints.some((point) => point.signature === 'segment-exit:7'), true);
});

test('keeps counts but marks legacy unstamped traces as untimed', () => {
  const result = analyzeRecords([{
    source: 'legacy.jsonl',
    event: {
      type: 'item.completed',
      item: {
        id: 'one',
        type: 'mcp_tool_call',
        tool: 'flow-app-progress',
        arguments: {},
        status: 'completed'
      }
    }
  }]);

  assert.equal(result.timing.available, false);
  assert.equal(result.counts.mcpCalls, 1);
  assert.equal(result.tools['flow-app-progress'].latency.samples, 0);
});
