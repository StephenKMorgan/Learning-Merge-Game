// Renders SVG files to transparent PNGs with Chromium.
// usage: node render.js jobs.json   where jobs = [{svg, png, width, height, scale}]
const { chromium } = require('playwright');
const fs = require('fs');
(async () => {
  const jobs = JSON.parse(fs.readFileSync(process.argv[2], 'utf8'));
  const browser = await chromium.launch();
  const page = await browser.newPage();
  for (const j of jobs) {
    await page.setViewportSize({ width: j.width, height: j.height });
    const svg = fs.readFileSync(j.svg, 'utf8');
    await page.setContent(`<html><body style="margin:0;background:transparent">
      <div style="width:${j.width}px;height:${j.height}px">${svg.replace('<svg ', `<svg style="width:${j.width}px;height:${j.height}px;display:block" `)}</div></body></html>`);
    await page.screenshot({ path: j.png, omitBackground: true, clip: { x: 0, y: 0, width: j.width, height: j.height } });
  }
  await browser.close();
})();
