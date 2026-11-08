async page => {
  const folder='C:/notiurba/noti-urba/docs/validacion/NOT-88-commons-20261108T011359Z';
  const names=['Asfalto deteriorado — Mateusz Konieczny · CC0','Bache pequeño CDMX — PilinsaurioRex · CC0','Acumulación de residuos, Mindelo — Christian Pirkl · CC BY-SA 4.0'];
  const unexpected=[];
  await page.route('**/api/**', async route => { unexpected.push({url:route.request().url(),method:route.request().method()}); await route.abort(); });
  await page.setViewportSize({width:1500,height:1100});
  await page.evaluate(() => {
    document.title='NOT-88 · Comparación de fotografías públicas de prueba';
    document.body.replaceChildren();
    const style=document.createElement('style');
    style.textContent='body{margin:0;padding:24px;background:#fff;color:#15253c;font:16px Arial}h1{font-size:22px}h2{font-size:19px}.pair{padding:12px;border:1px solid #bbc;margin:16px 0}.columns{display:grid;grid-template-columns:1fr 1fr;gap:16px}figure{margin:0}img{width:100%;height:760px;object-fit:contain;background:#eee}figcaption{padding:8px;font-size:14px}';
    document.head.append(style);
    const title=document.createElement('h1');title.textContent='NOT-88 · Fotografías públicas de prueba, ajenas a reportes de Tuluá';document.body.append(title);
    const input=document.createElement('input');input.type='file';input.accept='image/jpeg,image/png';input.id='not88-file';input.hidden=true;document.body.append(input);
  });
  const results=[];
  for(let id=1;id<=3;id++) {
    await page.locator('#not88-file').setInputFiles(`${folder}/originales/foto-${id}.jpg`);
    const downloaded=page.waitForEvent('download');
    const result=await page.evaluate(async ({id,title}) => {
      const {reducirFoto}=await import('/src/compartido/utilidades/fotos.js');
      const file=document.querySelector('#not88-file').files[0];
      if(file.size<5000000) throw new Error('Original menor de 5 MB');
      const hex=buffer=>[...new Uint8Array(buffer)].map(b=>b.toString(16).padStart(2,'0')).join('');
      const hash=async f=>hex(await crypto.subtle.digest('SHA-256',await f.arrayBuffer()));
      const originalHash=await hash(file);
      const start=performance.now();
      const reduced=await reducirFoto(file);
      const elapsed=performance.now()-start;
      if(reduced.size>1000000)throw new Error('Resultado supera incluso el limite decimal de 1 MB');
      const a=await createImageBitmap(file),b=await createImageBitmap(reduced);
      const dimensions={original:{width:a.width,height:a.height},reduced:{width:b.width,height:b.height}};a.close();b.close();
      const section=document.createElement('section');section.id='pair-'+id;section.className='pair';
      const heading=document.createElement('h2');heading.textContent=id+'. '+title;section.append(heading);
      const cols=document.createElement('div');cols.className='columns';section.append(cols);
      const urls=[];
      for(const [label,photo] of [['Original público sin modificar',file],['Reducida con reducirFoto de NotiUrba',reduced]]) {
        const figure=document.createElement('figure'),caption=document.createElement('figcaption'),img=document.createElement('img');
        const url=URL.createObjectURL(photo);urls.push(url);img.src=url;img.alt=label;await img.decode();
        caption.textContent=label+' · '+photo.size.toLocaleString('es-CO')+' bytes';figure.append(caption,img);cols.append(figure);
      }
      document.body.append(section);
      const anchor=document.createElement('a');anchor.href=urls[1];anchor.download='foto-'+id+'.jpg';document.body.append(anchor);anchor.click();anchor.remove();
      return {id,title,originalBytes:file.size,reducedBytes:reduced.size,reductionPercent:100*(1-reduced.size/file.size),dimensions,originalSha256:originalHash,reducedSha256:await hash(reduced),compressionMs:elapsed,function:'/src/compartido/utilidades/fotos.js#reducirFoto',defaultLimitUsed:true};
    }, {id,title:names[id-1]});
    const download=await downloaded;
    await download.saveAs(`${folder}/reducidas/foto-${id}.jpg`);
    await page.locator('#pair-'+id).screenshot({path:`${folder}/comparacion-${id}.png`});
    results.push(result);
  }
  if(unexpected.length)throw new Error('Se intento utilizar API: '+JSON.stringify(unexpected));
  return {date:new Date().toISOString(),browser:page.context().browser().version(),folder,results,apiRequests:unexpected,originalsPublicNotTuluaCitizenPhotos:true,visualReview:'PENDIENTE de inspeccion de las capturas y archivos'};
}
