const fs = require("fs");
const path = require("path");
const playwrightPath = process.env.PLAYWRIGHT_MODULE || "/home/nicolas/.npm/_npx/9833c18b2d85bc59/node_modules/playwright";
const { chromium } = require(playwrightPath);
const baseURL = process.env.RETAILSTORE_URL || "http://localhost:19080/convertigo/projects/sample_RetailStoreFlowRun8/DisplayObjects/mobile/index.html";
const profile = process.env.RETAILSTORE_PROFILE || path.join(__dirname, ".run8-browser-profile");
const metrics = { synchronizationCompletionMs: null, firstLocalViewCompletionMs: null, firstUsefulRootRenderMs: null, firstLocalGetDetailMs: null, secondPersistentLaunchCompletionMs: null };
const failures = [];
const check = (condition, message) => { if (!condition) throw new Error(message); };
const imagesGood = async page => (await page.locator("img:visible").evaluateAll(xs => xs.every(x => x.naturalWidth > 0)));
const catalogPath = async page => {
  for (const label of ["EPICERIE SUCREE", "GOUTERS & BISCUITS", "BISCUITS AU CHOCOLAT"]) {
    await page.getByRole("button", { name: label, exact: true }).click();
    await page.waitForTimeout(300);
  }
  await page.getByRole("button", { name: "Biscuits Z'animo chocolat lait Cadbury", exact: true }).waitFor();
};
const rootLabels = async page => {
  const labels = await page.locator("button").allTextContents();
  return labels.filter(x => x !== "RAYONS");
};
async function launchOne(second) {
  const started = Date.now();
  const context = await chromium.launchPersistentContext(profile, { headless: true, executablePath: "/usr/bin/google-chrome", args: ["--no-sandbox"] });
  const pages = context.pages();
  const page = pages[0] || await context.newPage();
  try {
    await page.goto(baseURL);
    await page.getByRole("button", { name: "EPICERIE SUCREE", exact: true }).waitFor({ timeout: 60000 });
    const rootReady = Date.now() - started;
    if (!second) {
      metrics.synchronizationCompletionMs = rootReady;
      metrics.firstLocalViewCompletionMs = rootReady;
      metrics.firstUsefulRootRenderMs = rootReady;
    } else {
      metrics.secondPersistentLaunchCompletionMs = rootReady;
      check(!(await page.locator("body").innerText()).includes("100/100"), "second launch remained at active 100/100");
    }
    const roots = await rootLabels(page);
    check(roots.length === 14, "root must contain exactly 14 departments");
    check(new Set(roots).size === 14, "root department labels must be unique");
    check(await imagesGood(page), "a visible root image is broken");
    await catalogPath(page);
    const buttons = await page.locator("button").allTextContents();
    for (const crumb of ["RAYONS", "EPICERIE SUCREE", "GOUTERS & BISCUITS", "BISCUITS AU CHOCOLAT"]) check(buttons.includes(crumb), "missing breadcrumb " + crumb);
    const productImages = page.locator('img[alt="Product"]:visible');
    check(await productImages.count() === 22, "strict category must show exactly 22 product cards");
    check(await imagesGood(page), "a visible product image is broken");
    const expectedGrid = await page.locator("button").allTextContents();
    const historyBefore = await page.evaluate(() => history.length);
    await page.getByRole("button", { name: "Biscuits Z'animo chocolat lait Cadbury", exact: true }).click();
    await page.waitForURL(/\/segment\/?$/);
    metrics.firstLocalGetDetailMs ||= Date.now() - started;
    check((await page.evaluate(() => history.length)) === historyBefore + 1, "product selection must add exactly one history entry");
    const detailText = await page.locator("body").innerText();
    check(detailText.includes("Biscuits Z'animo chocolat lait Cadbury") && detailText.includes("€5.40"), "detail must show exact name and unit price");
    check(!detailText.includes("Biscuits Finger L'Original Cadbury"), "catalog grid must not appear under detail");
    check(await imagesGood(page), "detail image is broken");
    const minus = page.getByRole("button", { name: "−", exact: true });
    const plus = page.getByRole("button", { name: "+", exact: true });
    await minus.click();
    check((await page.locator("body").innerText()).includes("\n1\n"), "quantity must remain bounded at one");
    await plus.click();
    check((await page.locator("body").innerText()).includes("€10.80"), "quantity action must update currency total");
    await page.goBack();
    await page.waitForTimeout(500);
    check(JSON.stringify(await page.locator("button").allTextContents()) === JSON.stringify(expectedGrid), "Browser Back did not restore exact grid and breadcrumb path");
    await page.getByRole("button", { name: "Biscuits Z'animo chocolat lait Cadbury", exact: true }).click();
    await page.waitForURL(/\/segment\/?$/);
    await page.getByText("GoBack", { exact: true }).click();
    await page.waitForTimeout(500);
    check(JSON.stringify(await page.locator("button").allTextContents()) === JSON.stringify(expectedGrid), "visible Back did not restore exact grid and breadcrumb path");
    await context.setOffline(true);
    check(await imagesGood(page), "offline catalog image is broken");
    await page.getByRole("button", { name: "Biscuits Z'animo chocolat lait Cadbury", exact: true }).click();
    await page.waitForURL(/\/segment\/?$/);
    check((await page.locator("body").innerText()).includes("Biscuits Z'animo chocolat lait Cadbury"), "offline local get/detail failed");
  } catch (error) {
    failures.push((second ? "second: " : "first: ") + error.message);
  } finally {
    await context.close();
  }
}
(async () => {
  await launchOne(false);
  await launchOne(true);
  const payload = { ...metrics, failures };
  console.log("CAMPAIGN_BROWSER_METRICS=" + JSON.stringify(payload));
  if (failures.length) process.exitCode = 1;
})().catch(error => {
  failures.push("runner: " + error.message);
  console.log("CAMPAIGN_BROWSER_METRICS=" + JSON.stringify({ ...metrics, failures }));
  process.exitCode = 1;
});
