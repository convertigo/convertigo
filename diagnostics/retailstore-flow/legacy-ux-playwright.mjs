import { chromium } from "/home/nicolas/.npm/_npx/9833c18b2d85bc59/node_modules/playwright/index.mjs";
import { mkdir, writeFile } from "node:fs/promises";

const url = process.env.RETAILSTORE_LEGACY_URL
  || "https://beta.convertigo.net/convertigo/projects/sampleMobileRetailStore/DisplayObjects/mobile/";
const outputDir = new URL("./results/legacy-ux/", import.meta.url).pathname;
const browser = await chromium.launch({
  headless: true,
  executablePath: "/home/nicolas/.cache/ms-playwright/chromium_headless_shell-1228/chrome-headless-shell-linux64/chrome-headless-shell"
});
const context = await browser.newContext({ viewport: { width: 390, height: 844 } });
const page = await context.newPage();
const states = [];

await mkdir(outputDir, { recursive: true });

async function capture(name) {
  await page.waitForTimeout(500);
  await page.evaluate(() => window.scrollTo(0, 0));
  const state = await page.evaluate(() => ({
    url: location.href,
    title: document.title,
    text: document.body.innerText.replace(/\n{3,}/g, "\n\n").trim(),
    headings: [...document.querySelectorAll("h1,h2,h3,h4,[role=heading]")].map((node) => node.textContent?.trim()).filter(Boolean),
    buttons: [...document.querySelectorAll("button,[role=button],ion-button")].map((node) => ({
      text: node.textContent?.trim() || "",
      ariaLabel: node.getAttribute("aria-label") || "",
      title: node.getAttribute("title") || ""
    })),
    links: [...document.querySelectorAll("a")].map((node) => ({
      text: node.textContent?.trim() || "",
      href: node.getAttribute("href") || ""
    })),
    selectedSegments: [...document.querySelectorAll("ion-segment-button.segment-button-checked,[role=tab][aria-selected=true]")]
      .map((node) => node.textContent?.trim()).filter(Boolean),
    segments: [...document.querySelectorAll("ion-segment,ion-segment-button,[class*=segment]")]
      .map((node) => ({ tag: node.tagName, className: String(node.className), text: node.textContent?.trim() || "" }))
      .filter((node) => node.text),
    historyLength: history.length,
    path: location.pathname,
    hash: location.hash
  }));
  states.push({ name, ...state });
  await page.screenshot({ path: `${outputDir}/${name}.png`, fullPage: true });
  return state;
}

await page.goto(url, { waitUntil: "domcontentloaded", timeout: 60_000 });
await capture("00-initial");

let previousPath = new URL(page.url()).pathname;
const deadline = Date.now() + 300_000;
while (Date.now() < deadline && !new URL(page.url()).pathname.endsWith("/Store")) {
  await page.waitForTimeout(1_000);
  const path = new URL(page.url()).pathname;
  if (path !== previousPath) {
    await capture(`01-transition-${path.split("/").filter(Boolean).at(-1)?.toLowerCase() || "root"}`);
    previousPath = path;
  }
}
if (!new URL(page.url()).pathname.endsWith("/Store")) {
  throw new Error(`Legacy bootstrap did not reach Store: ${page.url()}`);
}
await capture("02-store-root");

async function clickText(text, stateName) {
  const target = page.getByText(text, { exact: true }).first();
  await target.waitFor({ timeout: 30_000 });
  await target.click();
  await capture(stateName);
}

await clickText("EPICERIE SUCREE", "03-store-level-2");
await clickText("GOUTERS & BISCUITS", "04-store-level-3");
await clickText("BISCUITS AU CHOCOLAT", "05-store-products");
await clickText("Biscuits Z'animo chocolat lait Cadbury", "06-product-detail");

await page.goBack({ waitUntil: "domcontentloaded" });
await capture("07-browser-back-products");
await clickText("GOUTERS & BISCUITS", "08-segment-level-3");
await clickText("EPICERIE SUCREE", "09-segment-level-2");
await page.getByRole("button", { name: "RAYONS" }).click();
await capture("10-segment-root");

await writeFile(`${outputDir}/states.json`, `${JSON.stringify(states, null, 2)}\n`, "utf8");
console.log(JSON.stringify(states, null, 2));
await browser.close();
