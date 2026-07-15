import { chromium } from "/home/nicolas/.npm/_npx/9833c18b2d85bc59/node_modules/playwright/index.mjs";

const url = "http://127.0.0.1:18080/convertigo/projects/sample_RetailStoreFlowRun1/DisplayObjects/mobile/index.html";
const browser = await chromium.launch({
  headless: true,
  executablePath: "/home/nicolas/.cache/ms-playwright/chromium_headless_shell-1228/chrome-headless-shell-linux64/chrome-headless-shell"
});
const page = await browser.newPage({ viewport: { width: 1280, height: 900 } });
const consoleErrors = [];
const pageErrors = [];
const failedResponses = [];
page.on("console", (message) => {
  if (message.type() === "error") consoleErrors.push(message.text());
});
page.on("pageerror", (error) => pageErrors.push(error.message));
page.on("response", (response) => {
  if (response.status() >= 400) failedResponses.push({ status: response.status(), url: response.url() });
});

await page.goto(url, { waitUntil: "networkidle", timeout: 30_000 });
await page.getByRole("button", { name: "Initialize & synchronize" }).click();
await page.waitForTimeout(25_000);
const databases = await page.evaluate(async () =>
  typeof indexedDB.databases === "function" ? await indexedDB.databases() : []
);
const indexedDbStores = await page.evaluate(async () => {
  const names = typeof indexedDB.databases === "function" ? await indexedDB.databases() : [];
  const result = {};
  for (const entry of names) {
    if (!entry.name) continue;
    result[entry.name] = await new Promise((resolve, reject) => {
      const request = indexedDB.open(entry.name);
      request.onerror = () => reject(request.error);
      request.onsuccess = () => {
        const database = request.result;
        const stores = Array.from(database.objectStoreNames);
        const counts = {};
        if (!stores.length) {
          database.close();
          resolve(counts);
          return;
        }
        const transaction = database.transaction(stores, "readonly");
        for (const store of stores) {
          const count = transaction.objectStore(store).count();
          count.onsuccess = () => { counts[store] = count.result; };
        }
        transaction.oncomplete = () => { database.close(); resolve(counts); };
        transaction.onerror = () => reject(transaction.error);
      };
    });
  }
  return result;
});
await page.screenshot({
  path: "/home/nicolas/git/convertigo/diagnostics/retailstore-flow/results/run1-online.png",
  fullPage: true
});

console.log(JSON.stringify({
  title: await page.title(),
  url: page.url(),
  buttons: await page.getByRole("button").allTextContents(),
  databases,
  indexedDbStores,
  consoleErrors,
  pageErrors,
  failedResponses,
  bodyText: (await page.locator("body").innerText()).slice(0, 2_000),
  scrollWidth: await page.evaluate(() => document.documentElement.scrollWidth),
  clientWidth: await page.evaluate(() => document.documentElement.clientWidth)
}, null, 2));

await browser.close();
