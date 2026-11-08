async page => {
  const writes = [];
  page.on('request', r => { if (!['GET','HEAD'].includes(r.method())) writes.push({ method:r.method(),url:r.url() }); });
  const responsePromise = page.waitForResponse(r => r.url().endsWith('/api/catalogos/barrios'), { timeout:25000 });
  const barrios = await page.evaluate(async () => (await import('/src/compartido/api/catalogos.js')).obtenerBarrios());
  const response = await responsePromise;
  if (response.status() !== 200 || !Array.isArray(barrios) || barrios.length) throw new Error('El catálogo real cambió; revisar evidencia');
  await page.locator('#nombres').fill('Prueba Local');
  await page.locator('#apellidos').fill('Catalogo Temporal');
  await page.locator('#idTipoDocumento').selectOption('1');
  await page.locator('#numeroDocumento').fill('9876543210');
  await page.getByRole('button', { name:'Continuar',exact:true }).click();
  const selector = page.locator('#idBarrio');
  await selector.waitFor();
  const options = await selector.locator('option').allTextContents();
  const values = await selector.locator('option').evaluateAll(es => es.map(e=>({valor:e.value,texto:e.textContent})));
  if (options.some(n => /Barrio (de )?ejemplo|Administración NotiUrba/i.test(n))) throw new Error('Temporales visibles');
  if (!await selector.isDisabled()) throw new Error('El selector vacío debe estar deshabilitado');
  if (!await page.getByRole('button',{name:'Crear cuenta',exact:true}).isDisabled()) throw new Error('Se permite registro sin catálogo');
  await page.screenshot({path:'docs/validacion/evidencias-barrios-temporales-20261107/selector-sin-temporales.png',fullPage:true});
  if(writes.length) throw new Error('Se enviaron datos');
  return {fecha:new Date().toISOString(),url:page.url(),endpoint:response.url(),http:response.status(),barrios,opciones:values,selectorDeshabilitado:true,registroDeshabilitado:true,escriturasHttp:writes,origen:'GET real por proxy Vite a Render; no hay interceptores Playwright ni respuestas fabricadas',limitacion:'No puede seleccionarse ni verificarse persistencia de un barrio real porque la respuesta real es []'};
}
