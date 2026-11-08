async page => {
  await page.goto('http://127.0.0.1:5175/app/reportes/NU-000001');
  await page.locator('img[src*="19026/fotos/"]').waitFor({timeout:25000});
  const photo=await page.locator('img[src*="19026/fotos/"]').evaluate(async img=>{await img.decode();return{url:img.src,complete:img.complete,width:img.naturalWidth,height:img.naturalHeight}});
  if(!photo.complete||photo.width!==1200||photo.height!==1600)throw Error('El detalle no muestra el JPEG persistido');
  await page.screenshot({path:'C:/notiurba/noti-urba/docs/validacion/NOT-25-final-20261107/04-detalle-persistido.png',fullPage:true});
  return{url:page.url(),reportCode:'NU-000001',photo,source:'Nueva navegación y GET del reporte ya guardado; no se creó ni se subió otro archivo'};
}
