const fs = require('node:fs');
(async () => {
  const cv = require('C:/Users/Usuario/AppData/Local/Temp/not68-pdf-render/node_modules/@napi-rs/canvas');
  Object.assign(globalThis, { DOMMatrix: cv.DOMMatrix, ImageData: cv.ImageData, Path2D: cv.Path2D });
  const pdf = await import('file:///C:/Users/Usuario/AppData/Local/Temp/not68-pdf-render/node_modules/pdfjs-dist/legacy/build/pdf.mjs');
  const d = await pdf.getDocument({ data: new Uint8Array(fs.readFileSync('docs/validacion/evidencias-catalogo-barrios-20261107/fuente-pmgr-2020.pdf')), useSystemFonts: true }).promise;
  const result = [];
  for (let i = 16; i <= 21; i++) {
    const p = await d.getPage(i);
    const t = (await p.getTextContent()).items;
    const rows = new Map();
    for (const x of t) {
      const y = Math.round(x.transform[5]);
      if (!rows.has(y)) rows.set(y, []);
      rows.get(y).push({ x: Math.round(x.transform[4]), s: x.str });
    }
    result.push({ page: i, rows: [...rows].sort((a, b) => b[0] - a[0]).map(([y, r]) => ({ y, c: r.sort((a, b) => a.x - b.x) })) });
    const v = p.getViewport({ scale: 1.3 });
    const c = cv.createCanvas(Math.ceil(v.width), Math.ceil(v.height));
    await p.render({ canvasContext: c.getContext('2d'), viewport: v }).promise;
    fs.writeFileSync('output/pmgr-p' + i + '.png', c.toBuffer('image/png'));
  }
  fs.writeFileSync('output/pmgr-tabla.json', JSON.stringify(result, null, 2));
  for (const p of result) {
    console.log('PAGE ' + p.page);
    for (const r of p.rows) console.log(r.y + ' ' + r.c.map(x => x.x + ':' + x.s).join('|'));
  }
})().catch(e => { console.error(e); process.exitCode = 1; });
