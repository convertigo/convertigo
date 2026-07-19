#!/usr/bin/env node

import fs from "node:fs";

const baseUrl = process.env.C8O_BASE_URL || "http://127.0.0.1:19080/convertigo";
const cookieFile = process.env.C8O_COOKIE_FILE || "/tmp/c8o-local-anon.cookies";
const includeCatalog = process.env.C8O_INCLUDE_CATALOG === "true";
const projects = process.argv.slice(2);

if (projects.length === 0) {
  throw new Error("Usage: studio-tree-audit.mjs <project> [project ...]");
}

function readCookieHeader(file) {
  return fs.readFileSync(file, "utf8")
    .split(/\r?\n/)
    .filter((line) => line && (!line.startsWith("#") || line.startsWith("#HttpOnly_")))
    .map((line) => line.replace(/^#HttpOnly_/, ""))
    .map((line) => line.split("\t"))
    .filter((fields) => fields.length >= 7)
    .map((fields) => `${fields[5]}=${fields[6]}`)
    .join("; ");
}

const cookie = readCookieHeader(cookieFile);

async function service(name, parameters) {
  const url = new URL(`${baseUrl}/admin/services/${name}`);
  for (const [key, value] of Object.entries(parameters)) {
    url.searchParams.set(key, value);
  }
  const response = await fetch(url, { headers: { cookie } });
  if (!response.ok) {
    throw new Error(`${name} ${response.status}: ${await response.text()}`);
  }
  return response.json();
}

async function auditProject(project) {
  const occurrences = new Map();
  const nodes = new Map();
  const expanded = new Set();
  const skippedBranches = [];

  function skipChildren(id) {
    return !includeCatalog && (id.includes(".FlowEngine.catalog") || id.includes(".frontends.builder_svelte.catalog"));
  }

  async function visit(node, parentId) {
    const occurrence = {
      id: node.id,
      parentId,
      label: node.label,
      classname: node.classname || "folder"
    };
    occurrences.set(node.id, [...(occurrences.get(node.id) || []), occurrence]);
    if (!nodes.has(node.id)) nodes.set(node.id, { ...node, parentIds: [] });
    nodes.get(node.id).parentIds.push(parentId);

    let children = Array.isArray(node.children) ? node.children : [];
    if (node.children === true && skipChildren(node.id)) {
      skippedBranches.push(node.id);
    } else if (node.children === true && !expanded.has(node.id)) {
      expanded.add(node.id);
      const result = await service("studio.treeview.Get", { id: node.id, flow: "true" });
      children = result.children || [];
    }
    for (const child of children) await visit(child, node.id);
  }

  const root = await service("studio.treeview.Get", { id: project, flow: "true" });
  for (const child of root.children || []) await visit(child, project);

  const virtualNodes = [...nodes.values()].filter((node) => node.classname === "FlowVirtualObject");
  for (const node of virtualNodes) {
    try {
      const result = await service("studio.properties.Get", { id: node.id });
      node.properties = result.properties || {};
    } catch (error) {
      node.properties = {};
      node.propertyError = error.message.split("\n", 1)[0];
    }
  }

  const duplicateIds = [...occurrences.entries()]
    .filter(([, values]) => values.length > 1)
    .map(([id, values]) => ({ id, occurrences: values }));
  const opaqueLabels = [...nodes.values()]
    .filter((node) => /\[object Object\]|\bundefined\b|\bnull\b|Invalid model/i.test(node.label || ""))
    .map((node) => ({ id: node.id, label: node.label }));
  const virtualWithoutProperties = virtualNodes
    .filter((node) => Object.keys(node.properties).length === 0)
    .map((node) => node.id);

  return {
    project,
    counts: {
      uniqueNodes: nodes.size,
      occurrences: [...occurrences.values()].reduce((sum, values) => sum + values.length, 0),
      virtualNodes: virtualNodes.length
    },
    duplicateIds,
    opaqueLabels,
    virtualWithoutProperties,
    propertyErrors: virtualNodes
      .filter((node) => node.propertyError)
      .map((node) => ({ id: node.id, error: node.propertyError })),
    skippedBranches,
    nodes: [...nodes.values()].map((node) => ({
      id: node.id,
      label: node.label,
      classname: node.classname || "folder",
      parentIds: node.parentIds,
      properties: node.properties,
      propertyError: node.propertyError
    }))
  };
}

const audits = [];
for (const project of projects) audits.push(await auditProject(project));
console.log(JSON.stringify({ schemaVersion: 1, baseUrl, includeCatalog, audits }, null, 2));
