const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const { createRequire } = require("node:module");
const { pathToFileURL } = require("node:url");

async function verify(repo) {
  const requireWeb = createRequire(
    path.join(repo, "convertigo-studio-web/package.json"),
  );
  const { chromium } = requireWeb("playwright");
  const output = path.join(repo, "build/graphify/graphify-out");
  const browser = await chromium.launch({ headless: true });
  const errors = [];
  try {
    const page = await browser.newPage({
      viewport: { width: 1440, height: 1000 },
    });
    page.on("pageerror", (error) => errors.push(error.message));
    page.on("console", (message) => {
      if (message.type() === "error") errors.push(message.text());
    });
    await page.goto(pathToFileURL(path.join(output, "services.html")).href);
    await page.waitForFunction(
      () =>
        typeof network !== "undefined" &&
        network?.body?.data?.nodes?.length > 0,
    );
    await page.waitForTimeout(2000);
    const canvas = await page.evaluate(() => {
      const canvas = document.querySelector("#graph canvas");
      const pixels = canvas
        .getContext("2d")
        .getImageData(0, 0, canvas.width, canvas.height).data;
      let painted = 0;
      for (let i = 3; i < pixels.length; i += 4) if (pixels[i]) painted++;
      return { width: canvas.width, height: canvas.height, painted };
    });
    assert(canvas.width > 400 && canvas.height > 400 && canvas.painted > 2000);
    await page.selectOption("#root", "http:studio.dbo.ImportCopybook");
    assert(
      (await page.locator("#details").innerText()).includes("getServiceResult"),
    );
    await page
      .locator("#members a")
      .filter({ hasText: "calls_http" })
      .first()
      .click();
    const detail = await page.locator("#details").innerText();
    assert(detail.includes("StudioCopybookDialog.svelte:L37"));
    assert(detail.includes("fetch"));
    await page.selectOption(
      "#root",
      "web:convertigo-studio-web/src/lib/studio/StudioCopybookDialog.svelte",
    );
    await page.waitForTimeout(1200);
    assert((await page.locator("#details").innerText()).includes("component"));
    await page.screenshot({
      path: path.join(output, "services-desktop.png"),
      fullPage: true,
    });
    await page.setViewportSize({ width: 390, height: 844 });
    await page.waitForTimeout(500);
    assert(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= window.innerWidth,
      ),
    );
    await page.screenshot({
      path: path.join(output, "services-mobile.png"),
      fullPage: true,
    });
    await page.fill("#search", "no-such-symbol-xyz");
    assert.equal(
      await page.locator("#title").innerText(),
      "No matching symbol",
    );
    assert.deepEqual(errors, []);
    const result = {
      canvas,
      errors,
      checks: [
        "nonblank canvas",
        "service/source/entry inspection",
        "edge evidence/source inspection",
        "component selection",
        "desktop/mobile screenshots",
        "mobile horizontal overflow",
        "empty search",
      ],
    };
    fs.writeFileSync(
      path.join(output, "view-validation.json"),
      JSON.stringify(result, null, 2) + "\n",
    );
    console.log(JSON.stringify(result, null, 2));
  } finally {
    await browser.close();
  }
}

verify(path.resolve(process.argv[2] ?? process.cwd())).catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
