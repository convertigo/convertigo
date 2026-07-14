#!/usr/bin/env node

const assert = require("node:assert/strict");
const { chromium } = require("playwright");

const appUrl =
  "http://127.0.0.1:19080/convertigo/projects/" +
  "sample_HelloWorldFlowRun6/DisplayObjects/mobile/index.html";

async function inspect(page) {
  return page.evaluate(() => {
    const images = Array.from(document.querySelectorAll("img"));
    return {
      viewportWidth: window.innerWidth,
      documentWidth: document.documentElement.scrollWidth,
      cardCount: document.querySelectorAll("article, [class*='card']").length,
      imageCount: images.length,
      imagesWithSource: images.filter((image) => Boolean(image.currentSrc || image.src)).length,
      placeholderTitleCount: Array.from(document.querySelectorAll("body *")).filter(
        (node) => node.children.length === 0 && node.textContent === "NASA image title"
      ).length,
      bodyText: document.body.innerText
    };
  });
}

async function main() {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } });
  const browserErrors = [];
  const backendResponses = [];
  page.on("console", (message) => {
    if (message.type() === "error") browserErrors.push(message.text());
  });
  page.on("pageerror", (error) => browserErrors.push(error.message));
  page.on("response", (response) => {
    if (response.url().includes("sample_HelloWorldFlowRun6/.json")) {
      backendResponses.push({ status: response.status(), url: response.url() });
    }
  });

  try {
    const navigation = await page.goto(appUrl, {
      waitUntil: "domcontentloaded",
      timeout: 30_000
    });
    assert.equal(navigation.status(), 200);

    const button = page.getByRole("button", { name: "Load NASA images" });
    await button.click();
    await page.waitForResponse(
      (response) => response.url().includes("sample_HelloWorldFlowRun6/.json") && response.status() === 200,
      { timeout: 30_000 }
    );
    await page.getByText("NASA image title", { exact: true }).first().waitFor({ timeout: 30_000 });

    const desktop = await inspect(page);
    await page.screenshot({
      path: "diagnostics/helloworld-feed-perf/results/run6-desktop.png",
      fullPage: false
    });

    await page.setViewportSize({ width: 390, height: 844 });
    await page.waitForTimeout(250);
    const mobile = await inspect(page);
    await page.screenshot({
      path: "diagnostics/helloworld-feed-perf/results/run6-mobile.png",
      fullPage: false
    });

    assert.equal(backendResponses.some((response) => response.status === 200), true);
    assert.equal(browserErrors.length, 0, browserErrors.join("\n"));
    process.stdout.write(JSON.stringify({
      ok: true,
      status: navigation.status(),
      title: await page.title(),
      backendResponses,
      browserErrors,
      desktop: { ...desktop, bodyText: undefined },
      mobile: { ...mobile, bodyText: undefined },
      functionalContent: {
        expectedItems: 60,
        placeholderTitles: desktop.placeholderTitleCount,
        imagesWithSource: desktop.imagesWithSource,
        realDataRendered: desktop.imagesWithSource === 60 && desktop.placeholderTitleCount === 0
      },
      responsive: {
        desktop: desktop.documentWidth <= desktop.viewportWidth,
        mobile: mobile.documentWidth <= mobile.viewportWidth
      }
    }, null, 2) + "\n");
  } finally {
    await browser.close();
  }
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
