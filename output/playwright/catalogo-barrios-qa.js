async page => {
  const assert = (condition, message) => { if (!condition) throw new Error(message); };
  const posts = [];
  page.on('request', r => { if (r.method() !== 'GET') posts.push({ method: r.method(), url: r.url() }); });
  const result = await page.evaluate(async () => {
    const env = (await import('/@vite/env')).default;
    const source = await (await fetch('/src/compartido/api/cliente.js')).text();
    const response = await fetch('/api/catalogos/barrios');
    const barrios = await response.json();
    return { url: location.href, fecha: new Date().toISOString(), estado: response.status, barrios, mocksDesactivados: source.includes('"VITE_USAR_MOCKS": "false"') || /VITE_USAR_MOCKS["']?\s*:\s*["']false/.test(source) };
  });
  assert(result.estado === 200 && result.barrios.length === 135, 'La API real local no devuelve 135 barrios');
  await page.locator('#nombres').fill('Prueba Catalogo');
  await page.locator('#apellidos').fill('Validacion Local');
  await page.locator('#idTipoDocumento').selectOption('1');
  await page.locator('#numeroDocumento').fill('9876543210');
  await page.getByRole('button', { name: 'Continuar', exact: true }).click();
  await page.locator('#idBarrio').waitFor();
  const centro = result.barrios.find(b => b.nombre === 'Centro');
  assert(centro?.idComuna === 4, 'Centro no tiene la comuna de la fuente');
  const options = await page.locator('#idBarrio option').evaluateAll(es => es.filter(e => e.value).map(e => ({ id: Number(e.value), nombre: e.textContent })));
  assert(options.length === 135, 'El formulario descarta registros');
  assert(options.every((o,i) => o.id === result.barrios[i].idBarrio && o.nombre === result.barrios[i].nombre), 'Opciones distintas al catálogo');
  await page.locator('#idBarrio').selectOption(String(centro.idBarrio));
  await page.screenshot({ path: 'docs/validacion/evidencias-catalogo-barrios-20261107/registro-barrios-qa.png', fullPage: true });
  await page.getByRole('button', { name: 'Volver a datos personales', exact: true }).click();
  await page.getByRole('button', { name: 'Continuar', exact: true }).click();
  const preserved = await page.locator('#idBarrio').inputValue();
  assert(preserved === String(centro.idBarrio), 'Se pierde el barrio al retroceder');
  await page.screenshot({ path: 'docs/validacion/evidencias-catalogo-barrios-20261107/registro-barrio-conservado-qa.png', fullPage: true });
  const errors = await page.locator('[role="alert"]').allTextContents();
  assert(!errors.some(e => /barrio|catálogo/i.test(e)), 'Hay errores de catálogo en la interfaz');
  assert(posts.length === 0, 'La prueba realizó escrituras HTTP');
  return { ...result, opciones: options.length, seleccionado: centro, conservado: preserved, erroresCatalogo: [], escriturasHttp: posts, userAgent: await page.evaluate(() => navigator.userAgent), alcance: 'Chrome real, frontend QA sin mocks, Spring Boot del checkout y PostgreSQL desechable; no se creó una cuenta ni se envió un correo.' };
}
