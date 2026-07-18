#!/usr/bin/env node

const assert = require("node:assert/strict");
const { chromium } = require("playwright");

const project = process.env.FLOW_PROJECT || "sample_HelloWorldFlowRun11";
const outputPrefix = process.env.FLOW_OUTPUT_PREFIX || "run11";
const oddVariant = process.env.FLOW_ODD_VARIANT || "muted";
const loadButton = process.env.FLOW_LOAD_BUTTON || "Load NASA images";
const appUrl =
  `http://127.0.0.1:19080/convertigo/projects/${project}/DisplayObjects/mobile/`;

async function inspect(page, expected) {
  return page.evaluate((payload) => {
    const cards = Array.from(document.querySelectorAll("section.fb-card"))
      .filter((card) => card.querySelector(":scope > img.fb-image"));
    const rows = cards.map((card) => ({
      variant: card.dataset.variant,
      title: card.querySelector(':scope > p[data-variant="title"]')?.textContent?.trim() || "",
      description: card.querySelector(':scope > p[data-variant="description"]')?.textContent?.trim() || "",
      imageUrl: card.querySelector(":scope > img.fb-image")?.getAttribute("src") || "",
      imageLoaded: (() => {
        const image = card.querySelector(":scope > img.fb-image");
        return Boolean(image?.complete && image.naturalWidth > 0 && image.naturalHeight > 0);
      })()
    }));
    const mismatches = rows.filter((row, index) => {
      const item = payload.news[index];
      return !item || row.title !== item.title ||
        row.description !== item.description || row.imageUrl !== item.imageUrl;
    });
    return {
      viewportWidth: window.innerWidth,
      documentWidth: document.documentElement.scrollWidth,
      cardCount: rows.length,
      loadedImageCount: rows.filter((row) => row.imageLoaded).length,
      variantCounts: rows.reduce((counts, row) => {
        counts[row.variant] = (counts[row.variant] || 0) + 1;
        return counts;
      }, {}),
      alternates: rows.every((row, index) =>
        row.variant === (index % 2 === 0 ? "sky" : payload.oddVariant)),
      exactMatches: rows.length - mismatches.length,
      mismatchCount: mismatches.length
    };
  }, { ...expected, oddVariant });
}

async function main() {
  const browser = await chromium.launch({
    headless: true,
    executablePath: "/home/nicolas/.cache/ms-playwright/chromium_headless_shell-1228/chrome-headless-shell-linux64/chrome-headless-shell"
  });
  const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } });
  const browserErrors = [];
  const failedResponses = [];
  const failedRequests = [];
  page.on("console", (message) => {
    if (message.type() === "error" && !message.text().includes("favicon.ico")) {
      browserErrors.push(message.text());
    }
  });
  page.on("pageerror", (error) => browserErrors.push(error.message));
  page.on("response", (response) => {
    if (response.status() >= 400) failedResponses.push({ status: response.status(), url: response.url() });
  });
  page.on("requestfailed", (request) => {
    failedRequests.push({ error: request.failure()?.errorText || "", url: request.url() });
  });

  try {
    const responsePromise = page.waitForResponse(
      (response) => response.url().includes(`${project}/.json`) && response.status() === 200,
      { timeout: 30_000 }
    );
    const navigation = await page.goto(appUrl, {
      waitUntil: "domcontentloaded",
      timeout: 30_000
    });
    assert.equal(navigation.status(), 200);
    await page.waitForTimeout(500);
    if (await page.locator("section.fb-card > img.fb-image").count() === 0) {
      await page.getByRole("button", { name: loadButton }).click();
    }
    const response = await responsePromise;
    const payload = await response.json();
    await page.waitForFunction(
      (count) => document.querySelectorAll("section.fb-card > img.fb-image").length === count,
      payload.count,
      { timeout: 30_000 }
    );
    const imagesFullyLoaded = await page.waitForFunction(
      () => Array.from(document.querySelectorAll("section.fb-card > img.fb-image"))
        .every((image) => image.complete && image.naturalWidth > 0),
      null,
      { timeout: 60_000 }
    ).then(() => true, () => false);

    const desktop = await inspect(page, payload);
    await page.screenshot({
      path: `diagnostics/helloworld-feed-perf/results/${outputPrefix}-desktop.png`,
      fullPage: false
    });
    await page.setViewportSize({ width: 390, height: 844 });
    const mobile = await inspect(page, payload);
    await page.screenshot({
      path: `diagnostics/helloworld-feed-perf/results/${outputPrefix}-mobile.png`,
      fullPage: false
    });

    const report = {
      ok: true,
      applicationStatus: navigation.status(),
      backendStatus: response.status(),
      backendCount: payload.count,
      imagesFullyLoaded,
      browserErrors,
      failedResponses,
      failedRequests,
      desktop,
      mobile
    };
    process.stdout.write(JSON.stringify(report, null, 2) + "\n");

    assert.equal(payload.news.length, payload.count);
    assert.equal(browserErrors.length, 0, browserErrors.join("\n"));
    for (const result of [desktop, mobile]) {
      assert.equal(result.cardCount, payload.count);
      assert.equal(result.loadedImageCount, payload.count);
      assert.equal(result.exactMatches, payload.count);
      assert.equal(result.mismatchCount, 0);
      assert.deepEqual(result.variantCounts, { sky: 30, [oddVariant]: 30 });
      assert.equal(result.alternates, true);
      assert.equal(result.documentWidth <= result.viewportWidth, true);
    }
  } finally {
    await browser.close();
  }
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
