import { chromium } from "/home/nicolas/.npm/_npx/9833c18b2d85bc59/node_modules/playwright/index.mjs";

const url = "http://127.0.0.1:18080/convertigo/projects/sample_RetailStoreFlowRun2/DisplayObjects/mobile/index.html";
const browser = await chromium.launch({
  headless: true,
  executablePath: "/home/nicolas/.cache/ms-playwright/chromium_headless_shell-1228/chrome-headless-shell-linux64/chrome-headless-shell"
});
const context = await browser.newContext({ viewport: { width: 1280, height: 900 } });
const page = await context.newPage();
const consoleErrors = [];
const pageErrors = [];
page.on("console", (message) => {
  if (message.type() === "error" && !message.text().includes("favicon.ico")) consoleErrors.push(message.text());
});
page.on("pageerror", (error) => pageErrors.push(error.message));

await page.goto(url, { waitUntil: "networkidle", timeout: 30_000 });
await page.getByRole("button", { name: "Initialize & synchronize" }).click();
await page.waitForTimeout(25_000);

const local = await page.evaluate(async () => {
  const databases = typeof indexedDB.databases === "function" ? await indexedDB.databases() : [];
  const database = new window.PouchDB("retailstore_device");
  const info = await database.info();
  let root;
  try {
    root = await database.query("catalog/by_parent", {
      startkey: ["Menu", "category"],
      endkey: ["Menu", "category", {}, {}],
      include_docs: true
    });
  } catch (error) {
    root = { error: error.name || "query_error", message: error.message };
  }
  return {
    databases,
    info,
    rootCount: Array.isArray(root.rows) ? root.rows.length : null,
    rootNames: Array.isArray(root.rows) ? root.rows.slice(0, 10).map((row) => row.doc?.name) : [],
    rootError: root.error || "",
    rootMessage: root.message || ""
  };
});

await context.setOffline(true);
const offlineBodyText = (await page.locator("body").innerText()).slice(0, 4_000);
await page.screenshot({
  path: "/home/nicolas/git/convertigo/diagnostics/retailstore-flow/results/run2-online.png",
  fullPage: true
});

console.log(JSON.stringify({
  title: await page.title(),
  local,
  consoleErrors,
  pageErrors,
  offlineBodyText,
  scrollWidth: await page.evaluate(() => document.documentElement.scrollWidth),
  clientWidth: await page.evaluate(() => document.documentElement.clientWidth)
}, null, 2));

await browser.close();
