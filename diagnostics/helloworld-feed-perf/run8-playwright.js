#!/usr/bin/env node

const assert = require("node:assert/strict");
const { chromium } = require("playwright");

const appUrl =
  "http://127.0.0.1:19080/convertigo/projects/" +
  "sample_HelloWorldFlowRun8/DisplayObjects/mobile/index.html";

async function inspect(page) {
  return page.evaluate(() => {
    const images = Array.from(document.querySelectorAll("img"));
    const links = Array.from(document.querySelectorAll("a"));
    const cards = Array.from(document.querySelectorAll("article, [class*='card']"));
    const dates = Array.from(document.querySelectorAll("body *")).filter(
      (node) => node.children.length === 0 && /(?:Mon|Tue|Wed|Thu|Fri|Sat|Sun),\s+\d{1,2}\s+[A-Z][a-z]{2}\s+\d{4}/.test(node.textContent || "")
    );
    return {
      viewportWidth: window.innerWidth,
      documentWidth: document.documentElement.scrollWidth,
      cardCount: cards.length,
      imageCount: images.length,
      imagesWithSource: images.filter((image) => Boolean(image.currentSrc || image.src)).length,
      imagesLoaded: images.filter((image) => image.complete && image.naturalWidth > 0).length,
      storyLinkCount: links.filter((link) => link.textContent.trim() === "Read full story" && /^https?:/.test(link.href)).length,
      dateCount: dates.length,
      cardBackgrounds: [...new Set(cards.map((card) => getComputedStyle(card).backgroundColor))],
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
    if (response.url().includes("sample_HelloWorldFlowRun8/.json")) {
      backendResponses.push({ status: response.status(), url: response.url() });
    }
  });

  try {
    const navigation = await page.goto(appUrl, { waitUntil: "domcontentloaded", timeout: 30_000 });
    assert.equal(navigation.status(), 200);
    const responsePromise = page.waitForResponse(
      (response) => response.url().includes("sample_HelloWorldFlowRun8/.json") && response.status() === 200,
      { timeout: 30_000 }
    );
    await page.getByRole("button", { name: "Load NASA images" }).click();
    await responsePromise;
    await page.locator("img").first().waitFor({ timeout: 30_000 });
    await page.waitForTimeout(2_000);

    const desktop = await inspect(page);
    await page.screenshot({ path: "diagnostics/helloworld-feed-perf/results/run8-desktop.png", fullPage: false });
    await page.setViewportSize({ width: 390, height: 844 });
    await page.waitForTimeout(250);
    const mobile = await inspect(page);
    await page.screenshot({ path: "diagnostics/helloworld-feed-perf/results/run8-mobile.png", fullPage: false });

    const report = {
      ok: true,
      status: navigation.status(),
      title: await page.title(),
      backendResponses,
      browserErrors,
      desktop: { ...desktop, bodyText: undefined },
      mobile: { ...mobile, bodyText: undefined },
      responsive: {
        desktop: desktop.documentWidth <= desktop.viewportWidth,
        mobile: mobile.documentWidth <= mobile.viewportWidth
      }
    };
    process.stdout.write(JSON.stringify(report, null, 2) + "\n");
    assert.equal(backendResponses.some((response) => response.status === 200), true);
    assert.equal(browserErrors.length, 0, browserErrors.join("\n"));
    assert.equal(desktop.imagesWithSource, 60);
    assert.equal(desktop.imagesLoaded > 0, true);
    assert.equal(desktop.storyLinkCount, 60);
    assert.equal(desktop.dateCount >= 60, true);
    assert.equal(desktop.cardBackgrounds.length >= 2, true);
    assert.equal(desktop.documentWidth <= desktop.viewportWidth, true);
    assert.equal(mobile.documentWidth <= mobile.viewportWidth, true);
  } finally {
    await browser.close();
  }
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
