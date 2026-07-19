#!/usr/bin/env node

import http from 'node:http';

const listenPort = Number(process.env.MCP_PROXY_PORT || 19081);
const upstreamHost = process.env.MCP_UPSTREAM_HOST || '127.0.0.1';
const upstreamPort = Number(process.env.MCP_UPSTREAM_PORT || 19080);

let sessionCookie = '';
let queue = Promise.resolve();

function readBody(request) {
  return new Promise((resolve, reject) => {
    const chunks = [];
    request.on('data', (chunk) => chunks.push(chunk));
    request.on('end', () => resolve(Buffer.concat(chunks)));
    request.on('error', reject);
  });
}

function forward(request, response, body) {
  return new Promise((resolve) => {
    const headers = { ...request.headers, host: `${upstreamHost}:${upstreamPort}` };
    delete headers.cookie;
    if (sessionCookie) headers.cookie = sessionCookie;

    const upstream = http.request({
      host: upstreamHost,
      port: upstreamPort,
      method: request.method,
      path: request.url,
      headers
    }, (upstreamResponse) => {
      const setCookies = upstreamResponse.headers['set-cookie'] || [];
      const jsession = setCookies.find((value) => value.startsWith('JSESSIONID='));
      if (jsession) sessionCookie = jsession.split(';', 1)[0];

      response.writeHead(upstreamResponse.statusCode || 502, upstreamResponse.headers);
      upstreamResponse.pipe(response);
      upstreamResponse.on('end', resolve);
    });
    upstream.on('error', (error) => {
      if (!response.headersSent) response.writeHead(502, { 'content-type': 'text/plain' });
      response.end(`MCP upstream error: ${error.message}`);
      resolve();
    });
    upstream.end(body);
  });
}

const server = http.createServer(async (request, response) => {
  const body = await readBody(request);
  const turn = queue.then(() => forward(request, response, body));
  queue = turn.catch(() => {});
});

server.listen(listenPort, '127.0.0.1', () => {
  process.stderr.write(`MCP session proxy listening on 127.0.0.1:${listenPort}\n`);
});
