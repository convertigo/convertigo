const fs = require("fs");
const os = require("os");
const path = require("path");
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || "/home/nicolas/.npm/_npx/9833c18b2d85bc59/node_modules/playwright");

const url = process.env.RETAILSTORE_URL || "http://127.0.0.1:19080/convertigo/projects/sample_RetailStoreFlowRun8/DisplayObjects/mobile/";
const profile = path.join(os.tmpdir(), "retailstore-flow-run8-independent-profile");
const evidence = path.resolve(__dirname);
const branch = ["EPICERIE SUCREE", "GOUTERS & BISCUITS", "BISCUITS AU CHOCOLAT"];
const product = "Biscuits Z'animo chocolat lait Cadbury";

const result = {
  passed: [],
  failed: [],
  metrics: {},
  consoleErrors: [],
  pageErrors: [],
  offlineRequests: []
};

function check(condition, name, detail = "") {
  if (condition) result.passed.push(name);
  else result.failed.push({ name, detail });
}

async function loadedImages(page) {
  const images = page.locator("img:visible");
  return {
    count: await images.count(),
    allLoaded: await images.evaluateAll(nodes => nodes.every(image => image.naturalWidth > 0))
  };
}

async function noOverflow(page) {
  return page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth);
}

async function clickBranch(page) {
  for (const label of branch) {
    await page.getByRole("button", { name: label, exact: true }).click();
    await page.waitForTimeout(300);
  }
}

async function waitForRoot(page) {
  await page.getByRole("button", { name: branch[0], exact: true }).waitFor({ timeout: 60_000 });
}

(async () => {
  fs.rmSync(profile, { recursive: true, force: true });
  const context = await chromium.launchPersistentContext(profile, {
    headless: true,
    executablePath: "/usr/bin/google-chrome",
    viewport: { width: 390, height: 844 },
    args: ["--no-sandbox"]
  });
  const page = context.pages()[0] || await context.newPage();
  page.on("pageerror", error => result.pageErrors.push(String(error)));
  page.on("console", message => {
    if (message.type() === "error") result.consoleErrors.push(message.text());
  });

  const started = Date.now();
  await page.goto(url, { waitUntil: "domcontentloaded" });
  await waitForRoot(page);
  result.metrics.firstRootMs = Date.now() - started;

  const rootButtons = (await page.locator("button:visible").allTextContents()).filter(text => text !== "RAYONS");
  check(rootButtons.length === 14, "root-count-14", JSON.stringify(rootButtons));
  check(new Set(rootButtons).size === 14, "root-unique-14", JSON.stringify(rootButtons));
  check(await noOverflow(page), "mobile-root-no-overflow");
  const rootImages = await loadedImages(page);
  check(rootImages.count === 14 && rootImages.allLoaded, "root-images-loaded", JSON.stringify(rootImages));
  await page.screenshot({ path: path.join(evidence, "run8-root-mobile.png"), fullPage: true });

  await clickBranch(page);
  const branchButtons = await page.locator("button:visible").allTextContents();
  for (const crumb of ["RAYONS", ...branch]) {
    check(branchButtons.includes(crumb), `breadcrumb-visible-${crumb}`, JSON.stringify(branchButtons));
  }
  const productImages = page.locator('img[alt="Product"]:visible');
  check(await productImages.count() === 22, "strict-branch-22-products", String(await productImages.count()));
  check((await loadedImages(page)).allLoaded, "product-images-loaded");
  check(await noOverflow(page), "mobile-products-no-overflow");
  await page.screenshot({ path: path.join(evidence, "run8-products-mobile.png"), fullPage: true });

  const expectedGrid = await page.locator("button:visible").allTextContents();
  const historyBefore = await page.evaluate(() => history.length);
  await page.getByRole("button", { name: product, exact: true }).click();
  await page.waitForURL(/\/segment\/?$/);
  await page.waitForTimeout(300);
  const detailText = await page.locator("body").innerText();
  check((await page.evaluate(() => history.length)) === historyBefore + 1, "detail-adds-one-history-entry");
  check(detailText.includes(product) && detailText.includes("€5.40"), "detail-name-price");
  check(!detailText.includes("Biscuits Finger L'Original Cadbury"), "detail-hides-grid");
  check((await loadedImages(page)).allLoaded, "detail-image-loaded");
  await page.getByRole("button", { name: "−", exact: true }).click();
  check((await page.locator("body").innerText()).includes("\n1\n"), "quantity-min-one");
  await page.getByRole("button", { name: "+", exact: true }).click();
  check((await page.locator("body").innerText()).includes("€10.80"), "quantity-total");
  await page.screenshot({ path: path.join(evidence, "run8-detail-mobile.png"), fullPage: true });

  await page.goBack();
  await page.waitForTimeout(700);
  const browserBackGrid = await page.locator("button:visible").allTextContents();
  check(JSON.stringify(browserBackGrid) === JSON.stringify(expectedGrid), "browser-back-restores-grid", JSON.stringify(browserBackGrid));

  await page.getByRole("button", { name: "RAYONS", exact: true }).click();
  await waitForRoot(page);
  await clickBranch(page);
  await page.getByRole("button", { name: product, exact: true }).click();
  await page.waitForURL(/\/segment\/?$/);
  const visibleBack = page.getByText("GoBack", { exact: true });
  check(await visibleBack.count() === 1, "visible-back-present");
  await visibleBack.click();
  await page.waitForTimeout(700);
  const visibleBackGrid = await page.locator("button:visible").allTextContents();
  check(JSON.stringify(visibleBackGrid) === JSON.stringify(expectedGrid), "visible-back-restores-grid", JSON.stringify(visibleBackGrid));

  if (JSON.stringify(visibleBackGrid) !== JSON.stringify(expectedGrid)) {
    await page.goto(url, { waitUntil: "domcontentloaded" });
    await waitForRoot(page);
  } else {
    await page.getByRole("button", { name: "RAYONS", exact: true }).click();
  }
  await waitForRoot(page);
  await clickBranch(page);
  const beforeCrumbHistory = await page.evaluate(() => history.length);
  const ancestor = page.getByRole("button", { name: branch[0], exact: true });
  if (await ancestor.count()) {
    await ancestor.click();
    await page.waitForTimeout(500);
    check(await page.getByRole("button", { name: branch[1], exact: true }).count() === 1, "breadcrumb-restores-earlier-level");
    check((await page.evaluate(() => history.length)) === beforeCrumbHistory, "breadcrumb-does-not-add-history");
  } else {
    check(false, "breadcrumb-restores-earlier-level", "ancestor action is missing");
    check(false, "breadcrumb-does-not-add-history", "ancestor action is missing");
  }

  await page.goto(url, { waitUntil: "domcontentloaded" });
  await waitForRoot(page);
  page.on("request", request => {
    if (["fetch", "xhr"].includes(request.resourceType())) result.offlineRequests.push(request.url());
  });
  await context.setOffline(true);
  await clickBranch(page);
  check(await page.locator('img[alt="Product"]:visible').count() === 22, "offline-local-views");
  await page.getByRole("button", { name: product, exact: true }).click();
  await page.waitForURL(/\/segment\/?$/);
  await page.waitForTimeout(1000);
  check((await page.locator("body").innerText()).includes(product), "offline-detail-local-get");
  await context.setOffline(false);
  await page.waitForTimeout(100);
  check(result.offlineRequests.length === 0, "offline-no-fetch-xhr", JSON.stringify(result.offlineRequests));
  await page.setViewportSize({ width: 1280, height: 900 });
  check(await noOverflow(page), "desktop-detail-no-overflow");
  await page.screenshot({ path: path.join(evidence, "run8-detail-desktop.png"), fullPage: true });
  await context.close();

  const secondStarted = Date.now();
  const secondContext = await chromium.launchPersistentContext(profile, {
    headless: true,
    executablePath: "/usr/bin/google-chrome",
    viewport: { width: 390, height: 844 },
    args: ["--no-sandbox"]
  });
  const secondPage = secondContext.pages()[0] || await secondContext.newPage();
  await secondPage.goto(url, { waitUntil: "domcontentloaded" });
  await waitForRoot(secondPage);
  result.metrics.secondRootMs = Date.now() - secondStarted;
  check(!(await secondPage.locator("body").innerText()).includes("100/100"), "second-launch-not-stuck");
  await secondContext.close();

  check(result.pageErrors.length === 0, "no-page-errors", JSON.stringify(result.pageErrors));
  check(result.consoleErrors.length === 0, "no-console-errors", JSON.stringify(result.consoleErrors));
  console.log(`RUN8_INDEPENDENT_STATE=${JSON.stringify(result)}`);
  process.exitCode = result.failed.length ? 1 : 0;
})().catch(error => {
  result.failed.push({ name: "runner", detail: error.stack || String(error) });
  console.log(`RUN8_INDEPENDENT_STATE=${JSON.stringify(result)}`);
  process.exitCode = 1;
});
