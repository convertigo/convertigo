#!/usr/bin/env node

const assert = require("node:assert/strict");
const { chromium } = require("playwright");

const appUrl =
  "http://127.0.0.1:19080/convertigo/projects/" +
  "sample_HelloWorldFlowRun5/DisplayObjects/mobile/index.html";

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
    if (response.url().includes("sample_HelloWorldFlowRun5")) {
      backendResponses.push({
        method: response.request().method(),
        status: response.status(),
        url: response.url(),
      });
    }
  });

  try {
    const navigation = await page.goto(appUrl, { waitUntil: "networkidle" });
    assert.equal(navigation.status(), 200);

    const button = page.getByRole("button", { name: "Load NASA stories" });
    assert.equal(await button.isVisible(), true);

    await button.click();

    const images = page.locator('img[alt="NASA Image of the Day"]');
    await images.nth(59).waitFor({ state: "attached", timeout: 120_000 });
    assert.equal(await images.count(), 60);
    assert.equal(backendResponses.some((response) => response.status === 200), true);
    assert.equal(browserErrors.length, 0, browserErrors.join("\n"));

    await page
      .waitForFunction(
        () =>
          Array.from(document.querySelectorAll('img[alt="NASA Image of the Day"]')).
            filter((image) => image.naturalWidth > 0).length === 60,
        { timeout: 30_000 }
      )
      .catch(() => {});
    const visual = await page.evaluate(() => {
      const images = Array.from(
        document.querySelectorAll('img[alt="NASA Image of the Day"]')
      );
      return {
        viewportWidth: window.innerWidth,
        documentWidth: document.documentElement.scrollWidth,
        loadedImageCount: images.filter((image) => image.naturalWidth > 0).length,
        failedImageCount: images.filter((image) => image.naturalWidth === 0).length,
      };
    });

    await page.screenshot({
      path: "diagnostics/helloworld-feed-perf/results/run5-desktop.png",
      fullPage: false,
    });
    process.stdout.write(
      JSON.stringify({
        ok: true,
        status: navigation.status(),
        backendResponses,
        imageCount: await images.count(),
        title: await page.title(),
        browserErrors,
        visual,
        visualOk:
          visual.documentWidth <= visual.viewportWidth &&
          visual.loadedImageCount === 60,
      }) + "\n"
    );
  } finally {
    await browser.close();
  }
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
