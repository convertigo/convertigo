const { test, expect } = require("@playwright/test");

const root = "https://beta.convertigo.net/convertigo/projects/sample_RetailStoreFlowRun4/DisplayObjects/mobile/";

test("FullSync migration synchronizes and survives reload", async ({ page }) => {
  test.setTimeout(300_000);
  const errors = [];
  page.on("pageerror", (error) => errors.push(error.message));
  page.on("console", (message) => {
    const text = message.text();
    if (message.type() === "error" && !text.includes("404") && !text.includes("ERR_NETWORK_CHANGED")) {
      errors.push(text);
    }
  });

  await page.goto(root, { waitUntil: "domcontentloaded", timeout: 120_000 });
  await expect(page).toHaveURL(/\/store\/$/, { timeout: 30_000 });
  await expect(page.getByText("EPICERIE SUCREE", { exact: true }).first()).toBeVisible({ timeout: 180_000 });
  await expect(page.getByRole("button", { name: "OUVRIR", exact: true })).toHaveCount(14);

  const marker = await page.evaluate(() => Object.entries(localStorage)
    .find(([key]) => key.startsWith("c8o.fullsync.reset."))?.[1]);
  expect(marker).toBe("retailstore-items-by-parent-v2");

  await page.reload({ waitUntil: "domcontentloaded", timeout: 120_000 });
  await expect(page.getByText("EPICERIE SUCREE", { exact: true }).first()).toBeVisible({ timeout: 60_000 });
  await expect(page.getByRole("button", { name: "OUVRIR", exact: true })).toHaveCount(14);
  expect(errors).toEqual([]);
});
