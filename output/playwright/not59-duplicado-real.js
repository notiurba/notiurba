async page => {
  page.setDefaultTimeout(30000);
  await page.setViewportSize({width:1366,height:768});
  await page.goto('http://127.0.0.1:5176/registro');
  await page.locator('#nombres').fill('Prueba Registro');
  await page.locator('#apellidos').fill('Validacion Aislada');
  await page.locator('#idTipoDocumento').selectOption('1');
  await page.locator('#numeroDocumento').fill('9000000059');
  await page.getByRole('button',{name:'Continuar',exact:true}).click();
  await page.locator('#idBarrio').selectOption({label:'Franciscanos'});
  await page.locator('#direccionCorrespondencia').fill('Entorno aislado de QA');
  await page.locator('#correo').fill('admin.dev@notiurba.local');
  await page.locator('#password').fill('SoloPruebasNOT59-123');
  await page.locator('#confirmar').fill('SoloPruebasNOT59-123');
  await page.locator('#aceptaPrivacidad').check();
  const responsePromise=page.waitForResponse(r=>r.request().method()==='POST'&&r.url().endsWith('/api/auth/registro'));
  await page.getByRole('button',{name:'Crear cuenta',exact:true}).click();
  const response=await responsePromise,body=await response.json();
  if(response.status()!==409||body.codigo!=='CORREO_YA_REGISTRADO'||body.campo!=='correo')throw Error('Respuesta inesperada: '+response.status()+' '+JSON.stringify(body));
  const message=page.getByText(body.mensaje,{exact:true});await message.waitFor();await message.scrollIntoViewIfNeeded();
  await page.screenshot({path:'C:/notiurba/noti-urba/docs/validacion/evidencias-NOT-59-20261108/correo-duplicado-real-1366.png',fullPage:true});
  const retained=await page.evaluate(()=>({nombres:document.querySelector('#nombres')?.value??null,correo:document.querySelector('#correo')?.value,passwordLength:document.querySelector('#password')?.value.length,barrio:document.querySelector('#idBarrio')?.selectedOptions[0]?.textContent}));
  return{case:'NOT-59 correo duplicado',date:new Date().toISOString(),status:'APROBADO',apiStatus:response.status(),response:body,messageVisible:await message.isVisible(),existingAccount:'Cuenta administrativa de desarrollo sembrada por V4 en PostgreSQL desechable',email:'admin.dev@notiurba.local',newDocument:'9000000059',retained,viewport:{width:1366,height:768},environment:{frontend:'http://127.0.0.1:5176',backend:'http://127.0.0.1:19059',database:'not59_qa',apiSimulated:false,emailsSent:false,productionTouched:false,v17Applied:false},scope:'Solo caso pendiente de duplicado con React y Spring Boot reales; no se repitieron las validaciones/responsive anteriores'};
}
