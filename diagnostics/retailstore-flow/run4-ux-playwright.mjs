import { chromium } from "/home/nicolas/.npm/_npx/9833c18b2d85bc59/node_modules/playwright/index.mjs";
import { mkdir, writeFile } from "node:fs/promises";

const url = process.env.RETAILSTORE_FLOW_URL
  || "https://beta.convertigo.net/convertigo/projects/sample_RetailStoreFlowRun4/DisplayObjects/mobile/";
const outputDir = new URL("./results/run4-ux/", import.meta.url).pathname;
const browser = await chromium.launch({
  headless: true,
  executablePath: "/home/nicolas/.cache/ms-playwright/chromium_headless_shell-1228/chrome-headless-shell-linux64/chrome-headless-shell"
});
const context = await browser.newContext({ viewport: { width: 390, height: 844 } });
const page = await context.newPage();
const states = [];

await mkdir(outputDir, { recursive: true });

async function capture(name) {
  const state = await page.evaluate(() => ({
    url: location.href,
    title: document.title,
    text: document.body.innerText.replace(/\n{3,}/g, "\n\n").trim(),
    buttons: [...document.querySelectorAll("button,[role=button]")].map((node) => node.textContent?.trim() || ""),
    historyLength: history.length,
    path: location.pathname
  }));
  states.push({ name, ...state });
  await page.screenshot({ path: `${outputDir}/${name}.png`, fullPage: true });
  return state;
}

async function openCard(name, buttonName, expectedName, stateName) {
  const label = page.getByText(name, { exact: true }).first();
  await label.waitFor({ timeout: 120_000 });
  const card = label.locator(`xpath=ancestor::*[.//button[normalize-space()="${buttonName}"]][1]`);
  await card.getByRole("button", { name: buttonName, exact: true }).click();
  await page.getByText(expectedName, { exact: true }).first().waitFor({ timeout: 30_000 });
  await capture(stateName);
}

await page.goto(url, { waitUntil: "domcontentloaded", timeout: 120_000 });
await capture("00-initial");
await page.waitForTimeout(250);
await capture("01-after-250ms");
await page.getByText("EPICERIE SUCREE", { exact: true }).first().waitFor({ timeout: 180_000 });
await capture("02-store-root");

await openCard("EPICERIE SUCREE", "OUVRIR", "GOUTERS & BISCUITS", "03-store-level-2");
await openCard("GOUTERS & BISCUITS", "OUVRIR", "BISCUITS AU CHOCOLAT", "04-store-level-3");
await openCard("BISCUITS AU CHOCOLAT", "OUVRIR", "Biscuits Z'animo chocolat lait Cadbury", "05-store-products");

const product = page.getByText("Biscuits Z'animo chocolat lait Cadbury", { exact: true }).first();
const productCard = product.locator('xpath=ancestor::*[.//button[normalize-space()="VOIR LE PRODUIT"]][1]');
await productCard.getByRole("button", { name: "VOIR LE PRODUIT", exact: true }).click();
await page.waitForURL(/\/detail\/?$/, { timeout: 30_000 });
await page.getByText("PRODUCT", { exact: true }).waitFor({ timeout: 30_000 });
await capture("06-product-detail");

await page.goBack({ waitUntil: "domcontentloaded" });
await page.getByText("Biscuits Z'animo chocolat lait Cadbury", { exact: true }).waitFor({ timeout: 30_000 });
await capture("07-browser-back-products");

await writeFile(`${outputDir}/states.json`, `${JSON.stringify(states, null, 2)}\n`, "utf8");
console.log(JSON.stringify(states, null, 2));
await browser.close();
