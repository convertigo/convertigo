import { chromium } from "/home/nicolas/.npm/_npx/9833c18b2d85bc59/node_modules/playwright/index.mjs";

const url = "http://127.0.0.1:18080/convertigo/projects/sample_RetailStoreFlowRun3/DisplayObjects/mobile/index.html";
const browser = await chromium.launch({
  headless: true,
  executablePath: "/home/nicolas/.cache/ms-playwright/chromium_headless_shell-1228/chrome-headless-shell-linux64/chrome-headless-shell"
});
const context = await browser.newContext({ viewport: { width: 1280, height: 900 } });
const page = await context.newPage();
const consoleErrors = [];
const pageErrors = [];

page.on("console", (message) => {
  if (message.type() === "error" && !message.text().includes("favicon.ico")) {
    consoleErrors.push(message.text());
  }
});
page.on("pageerror", (error) => pageErrors.push(error.message));

const assert = (condition, message) => {
  if (!condition) throw new Error(message);
};
const overflow = () => page.evaluate(() => ({
  scrollWidth: document.documentElement.scrollWidth,
  clientWidth: document.documentElement.clientWidth,
  overflowing: [...document.querySelectorAll("body *")]
    .filter((element) => element.getBoundingClientRect().right > document.documentElement.clientWidth + 1)
    .slice(0, 10)
    .map((element) => ({ tag: element.tagName, id: element.id, className: String(element.className) }))
}));

await page.goto(url, { waitUntil: "domcontentloaded", timeout: 30_000 });
await page.getByRole("button", { name: "Initialize & synchronize" }).click();
await page.waitForTimeout(15_000);
await page.getByText('{"ok":true}', { exact: true }).waitFor({ timeout: 120_000 });

await page.getByRole("button", { name: "Back to shop root / browse" }).click();
await page.getByRole("button", { name: "Open category" }).first().waitFor({ timeout: 10_000 });
const rootCount = await page.getByRole("button", { name: "Open category" }).count();
assert(rootCount === 14, `Expected 14 root categories, got ${rootCount}`);

await page.getByText("EPICERIE SUCREE", { exact: true }).locator("xpath=..").getByRole("button", { name: "Open category" }).click();
await page.getByText("GOUTERS & BISCUITS", { exact: true }).locator("xpath=..").getByRole("button", { name: "Open subcategory" }).click();
await page.getByText("BISCUITS AU CHOCOLAT", { exact: true }).locator("xpath=..").getByRole("button", { name: "Show products" }).click();

const onlineProducts = page.getByRole("button", { name: "View product details" });
await onlineProducts.first().waitFor();
const onlineProductCount = await onlineProducts.count();
assert(onlineProductCount === 22, `Expected 22 online products, got ${onlineProductCount}`);
const onlineCard = (await onlineProducts.first().locator("xpath=..").innerText()).split("\n").filter(Boolean);
await onlineProducts.first().click();
const detailBox = page.getByText("Selected product details", { exact: true }).locator("xpath=..");
await page.waitForTimeout(300);
const onlineDetail = (await detailBox.innerText()).split("\n").filter(Boolean);
assert(onlineDetail.includes(onlineCard[0]) && onlineDetail.includes(onlineCard[1]), "Online local get mismatch");

const desktop = await overflow();
assert(desktop.scrollWidth <= desktop.clientWidth, `Desktop overflow: ${JSON.stringify(desktop)}`);
await page.setViewportSize({ width: 390, height: 844 });
await page.waitForTimeout(200);
const mobile = await overflow();
assert(mobile.scrollWidth <= mobile.clientWidth, `Mobile overflow: ${JSON.stringify(mobile)}`);

const offlineRequests = [];
const recordRequest = (request) => {
  if (["xhr", "fetch"].includes(request.resourceType())) offlineRequests.push(request.url());
};
page.on("request", recordRequest);
await context.setOffline(true);

await page.getByRole("button", { name: "Back to shop root / browse" }).click();
await page.getByText("BIO & ECOLOGIE", { exact: true }).locator("xpath=..").getByRole("button", { name: "Open category" }).click();
const subcategories = page.getByRole("button", { name: "Open subcategory" });
await subcategories.first().waitFor();
const subcategory = (await subcategories.first().locator("xpath=..").innerText()).split("\n").filter(Boolean)[0];
await subcategories.first().click();

const leaves = page.getByRole("button", { name: "Show products" });
await leaves.first().waitFor();
const leaf = (await leaves.first().locator("xpath=..").innerText()).split("\n").filter(Boolean)[0];
await leaves.first().click();

const offlineProducts = page.getByRole("button", { name: "View product details" });
await offlineProducts.first().waitFor();
const offlineProductCount = await offlineProducts.count();
const offlineCard = (await offlineProducts.first().locator("xpath=..").innerText()).split("\n").filter(Boolean);
await offlineProducts.first().click();
await page.waitForTimeout(300);
const offlineDetail = (await detailBox.innerText()).split("\n").filter(Boolean);
assert(offlineDetail.includes(offlineCard[0]) && offlineDetail.includes(offlineCard[1]), "Offline local get mismatch");
assert(offlineRequests.length === 0, `Offline browsing attempted requests: ${offlineRequests.join(", ")}`);

await page.screenshot({
  path: "/home/nicolas/git/convertigo/diagnostics/retailstore-flow/results/run3-offline-mobile.png",
  fullPage: true
});
await context.setOffline(false);

console.log(JSON.stringify({
  rootCount,
  online: { productCount: onlineProductCount, card: onlineCard.slice(0, 4), detail: onlineDetail.slice(0, 5) },
  offline: { subcategory, leaf, productCount: offlineProductCount, card: offlineCard.slice(0, 4), detail: offlineDetail.slice(0, 5), requests: offlineRequests },
  layout: { desktop, mobile },
  consoleErrors,
  pageErrors
}, null, 2));

await browser.close();
