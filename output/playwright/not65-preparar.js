async page => {
  const target='http://127.0.0.1:5173/reportar/ubicacion';
  const main=page.context().pages().find(p=>p.url()===target);
  if(!main)throw Error('Falta pestana de prueba');
  const permissionBefore=await main.evaluate(async()=>(await navigator.permissions.query({name:'geolocation'})).state);
  if(permissionBefore!=='denied')return {case:'MAP-03',blocked:true,permissionBefore};
  const point=()=>main.evaluate(()=>{const b=JSON.parse(sessionStorage.getItem('notiurba.borrador'));return [b.latitud,b.longitud]});
  const before=await point();
  await main.getByRole('button',{name:'Volver a obtener mi ubicación',exact:true}).click();
  const dialog=main.getByRole('dialog');
  await dialog.waitFor({state:'visible'});
  const denialText=await dialog.innerText();
  const afterDenied=await point();
  await dialog.screenshot({path:'output/playwright/not65-map03-denegado-real.png'});
  await dialog.getByRole('button',{name:'Marcar en el mapa',exact:true}).click();
  await dialog.waitFor({state:'hidden'});
  const map=main.locator('.mapa-ubicacion');
  await map.click({position:{x:140,y:140}});
  await main.locator('.leaflet-marker-icon').waitFor({state:'visible'});
  const afterManual=await point();
  const observed=await main.evaluate(async()=>({date:new Date().toISOString(),url:location.href,permission:(await navigator.permissions.query({name:'geolocation'})).state,busy:!!document.querySelector('.ubicacion__buscando'),visibleDialogs:[...document.querySelectorAll('[role=dialog]')].filter(e=>e.getBoundingClientRect().height>0).length,markers:document.querySelectorAll('.leaflet-marker-icon').length,selected:document.body.innerText.includes('Ubicación seleccionada')}));
  await main.screenshot({path:'output/playwright/not65-map03-manual-sin-gps.png',mask:[map]});
  const pointPreservedAfterDenied=JSON.stringify(before)===JSON.stringify(afterDenied);
  const manualPointChanged=JSON.stringify(afterDenied)!==JSON.stringify(afterManual);
  const manualPointFinite=afterManual.every(Number.isFinite);
  return {case:'MAP-03',permissionBefore,denialText,pointPreservedAfterDenied,manualPointChanged,manualPointFinite,observed,passed:denialText.includes('Permiso de ubicación denegado')&&pointPreservedAfterDenied&&manualPointChanged&&manualPointFinite&&observed.permission==='denied'&&!observed.busy&&observed.visibleDialogs===0&&observed.markers===1&&observed.selected,personalCoordinatesIncluded:false,simulatedPermissions:false,simulatedCoordinates:false};
}
