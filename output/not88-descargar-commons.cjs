const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const titles = ['File:Damaged asphalt surface 2.jpg','File:Bache pequeño CDMX.jpg','File:Garbage dump 294A0043 São Vicente.jpg'];
const ua = 'NotiUrba-NOT88-QA/1.0 (manual Wikimedia Commons photo compression validation)';
(async () => {
  const query = new URLSearchParams({ action:'query', format:'json', prop:'imageinfo', titles:titles.join('|'), iiprop:'url|size|mime|sha1|extmetadata' });
  const response = await fetch('https://commons.wikimedia.org/w/api.php?' + query, { headers:{'User-Agent':ua}, signal:AbortSignal.timeout(25000) });
  if (!response.ok) throw new Error('Metadata HTTP ' + response.status);
  const raw = await response.json();
  const pages = Object.values(raw.query.pages);
  const sources = titles.map((title,i) => {
    const p = pages.find(p=>p.title===title);
    const info=p?.imageinfo?.[0];
    if (!info || info.size<5000000 || info.mime!=='image/jpeg' || !info.url.startsWith('https://upload.wikimedia.org/wikipedia/commons/') || info.url.includes('/thumb/')) throw new Error('Original no valido '+title);
    const m=info.extmetadata;
    if (!['CC0','CC BY-SA 4.0'].includes(m.LicenseShortName?.value)) throw new Error('Revisar licencia '+m.LicenseShortName?.value);
    return {id:i+1,title,originalUrl:info.url,pageUrl:info.descriptionurl,expectedBytes:info.size,width:info.width,height:info.height,commonsSha1:info.sha1,authorHtml:m.Artist?.value,license:m.LicenseShortName.value,licenseUrl:m.LicenseUrl?.value,descriptionHtml:m.ImageDescription?.value,date:m.DateTimeOriginal?.value,originalFile:`originales/foto-${i+1}.jpg`,reducedFile:`reducidas/foto-${i+1}.jpg`};
  });
  const folder=path.join('docs/validacion', 'NOT-88-commons-'+new Date().toISOString().replace(/[-:]/g,'').replace(/\.\d+Z$/,'Z'));
  fs.mkdirSync(folder);fs.mkdirSync(path.join(folder,'originales'));fs.mkdirSync(path.join(folder,'reducidas'));
  fs.writeFileSync('output/not88-carpeta.txt',folder);
  fs.writeFileSync(path.join(folder,'fuentes.json'),JSON.stringify({downloadedAt:new Date().toISOString(),purpose:'Fotografias publicas de pruebas; no reportes ni fotografias de ciudadanos de Tulua.',sources},null,2)+'\n');
  console.log(JSON.stringify({folder,sources:sources.map(s=>({title:s.title,bytes:s.expectedBytes,license:s.license,url:s.originalUrl}))}));
  for (const s of sources) {
    const r=await fetch(s.originalUrl,{headers:{'User-Agent':ua},signal:AbortSignal.timeout(90000)});
    if(!r.ok)throw new Error('Original HTTP '+r.status+' '+s.title);
    const bytes=Buffer.from(await r.arrayBuffer());
    if(bytes.length!==s.expectedBytes||bytes.length<5000000||bytes[0]!==255||bytes[1]!==216)throw new Error('Tamano o formato original incorrecto '+s.title);
    const sha1=crypto.createHash('sha1').update(bytes).digest('hex');
    if(sha1!==s.commonsSha1)throw new Error('SHA1 Commons no coincide '+s.title);
    fs.writeFileSync(path.join(folder,s.originalFile),bytes,{flag:'wx'});
    s.originalSha256=crypto.createHash('sha256').update(bytes).digest('hex');
    s.downloadedBytes=bytes.length;
    fs.writeFileSync(path.join(folder,'fuentes.json'),JSON.stringify({downloadedAt:new Date().toISOString(),purpose:'Fotografias publicas de pruebas; no reportes ni fotografias de ciudadanos de Tulua.',sources},null,2)+'\n');
    console.log(JSON.stringify({downloaded:s.id,bytes:bytes.length,sha1Verified:true}));
  }
})().catch(e=>{console.error(e.message);process.exitCode=1});
