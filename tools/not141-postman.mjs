import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';
import assert from 'node:assert/strict';
import { fileURLToPath } from 'node:url';

// Lee exclusivamente los recursos NOT-141 generados en el dialecto YAML del proyecto.
// Los escalares son cadenas JSON, válidas en YAML 1.2; no es un parser YAML general.
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const dir = path.join(root, 'docs/api/NotiUrba API');
const evidenceFlag=process.argv.indexOf('--evidence-dir');
if(evidenceFlag>=0&&!process.argv[evidenceFlag+1])throw new Error('Falta directorio de evidencias');
const out = evidenceFlag>=0?path.resolve(root,process.argv[evidenceFlag+1]):path.join(root, 'docs/validacion/NOT-141/evidencias');
const files = fs.readdirSync(dir).filter(f => /^NOT-141 N141-\d+\.request\.yaml$/.test(f)).sort();
function scalar(text, key) {
  const match = text.match(new RegExp('^' + key + ': (.+)$', 'm'));
  return match ? JSON.parse(match[1]) : undefined;
}
const requests = files.map(file => {
  const text = fs.readFileSync(path.join(dir, file), 'utf8');
  const scripts = [...text.matchAll(/    type: "(beforeRequest|afterResponse)"\r?\n    code: (.+)/g)]
    .map(m => ({listen: m[1] === 'beforeRequest' ? 'prerequest' : 'test', script: {type:'text/javascript', exec:JSON.parse(m[2]).split('\n')}}));
  const token = text.match(/^    token: (.+)$/m);
  const content = text.match(/^  content: (".+")$/m);
  const photo = text.match(/^      src: (.+)$/m);
  return {name: scalar(text, 'description').split('\n')[0], file,
    request: {method:scalar(text,'method'), url:scalar(text,'url'),
      auth: token ? {type:'bearer', bearer:[{key:'token',value:JSON.parse(token[1]),type:'string'}]} : {type:'noauth'},
      header: content ? [{key:'Content-Type',value:'application/json'}] : [],
      ...(content ? {body:{mode:'raw',raw:JSON.parse(content[1]),options:{raw:{language:'json'}}}} :
        photo ? {body:{mode:'formdata',formdata:[{key:'foto',type:'file',src:[JSON.parse(photo[1])].flat()[0]}]}} : {}),
      description:scalar(text,'description')}, event:scripts};
});
const definition=fs.readFileSync(path.join(dir,'.resources/definition.yaml'),'utf8');
const variables=Object.fromEntries([...definition.matchAll(/^  (\w+): (.+)$/gm)].map(m=>[m[1],m[2].startsWith('"')?JSON.parse(m[2]):m[2]]));
const collection={info:{name:'NotiUrba API - casos NOT-141',schema:'https://schema.getpostman.com/json/collection/v2.1.0/collection.json',description:'Exportación temporal de los recursos oficiales. No versionar una segunda colección.'},variable:Object.entries(variables).map(([key,value])=>({key,value})),item:requests};
const expected=i=>Number(i.event.find(e=>e.listen==='test')?.script.exec.join('\n').match(/HTTP (\d{3})|esperado: (\d{3})|\.status\((\d{3})\)/)?.slice(1).find(Boolean));
for (const item of requests) {
  assert.ok(expected(item),item.name+' sin HTTP esperado');
  for(const e of item.event) new vm.Script(e.script.exec.join('\n'), {filename:item.file});
}
if(process.argv.includes('--export')) {
  const target=process.argv[process.argv.indexOf('--export')+1];
  if(!target) throw new Error('Falta ruta de salida temporal');
  fs.mkdirSync(path.dirname(path.resolve(target)),{recursive:true});
  fs.writeFileSync(target,JSON.stringify(collection,null,2));
  console.log(JSON.stringify({exported:requests.length,format:'Postman v2.1',target}));
}
if(process.argv.includes('--validate')) {
  let blocked=0;
  for(const i of requests) for(const e of i.event.filter(e=>e.listen==='prerequest')) {
    let skipped=false;
    vm.runInNewContext(e.script.exec.join('\n'),{pm:{variables:{get:k=>variables[k]},execution:{skipRequest:()=>{skipped=true;}}},console:{warn:()=>{}},atob:s=>Buffer.from(s,'base64').toString()}, {timeout:1000});
    if(skipped) blocked++;
  }
  console.log(JSON.stringify({resources:requests.length,syntax:'OK',defaultGuardsBlocked:blocked,productionTestsApproved:0}));
}
if(process.argv.includes('--reproduce-fallback')) {
  const source=fs.readFileSync(path.join(root,'frontend/src/admin/servicios/reportesService.js'),'utf8')
    .replace(/^import .*$/gm,'').replace('export async function','async function');
  const sentinel=[{id:'SENTINELA_QA_SINTETICA'}];
  const context=vm.createContext({reportesPrueba:sentinel,listarTodosLosReportes:async()=>{throw new Error('Fallo API inyectado solo en VM local');}});
  new vm.Script(source).runInContext(context);
  const returned=await context.obtenerReportesCiudadanos();
  const result={date:new Date().toISOString(),environment:'VM local aislada; no navegador; no API de producción',file:'frontend/src/admin/servicios/reportesService.js',injectedFailure:true,returnedSyntheticFallback:returned===sentinel,approvedProductionTest:false};
  fs.mkdirSync(out,{recursive:true});
  fs.writeFileSync(path.join(out,'reproduccion-fallback-local.json'),JSON.stringify(result,null,2));
  console.log(JSON.stringify(result));
}
if(process.argv.includes('--safe-get')) {
  fs.mkdirSync(out,{recursive:true});
  const rows=[];
  // Ningún token ni cuerpo se carga o escribe. Este runner NO sustituye Newman.
  for(const i of requests) {
    const row={id:i.file.match(/N141-\d+/)[0],name:i.name,method:i.request.method,url:i.request.url.replace('{{baseUrl}}','https://notiurba-api.onrender.com'),expected:expected(i),obtained:null,date:new Date().toISOString(),status:'BLOQUEADA',cause:''};
    if(i.request.method!=='GET') row.cause='No ejecutada: solo lectura; requiere preparación y ejecución Postman';
    else if(i.request.auth.type!=='noauth') row.cause='No ejecutada: necesita token QA verificado o administrador';
    else if(row.url.includes('{{')) row.cause='No ejecutada: recurso QA no preparado';
    else {
      try {
        const response=await fetch(row.url,{signal:AbortSignal.timeout(15000),redirect:'error',headers:{Origin:'https://notiurba.netlify.app'}});
        row.obtained=response.status;
        // No persistir cuerpos que puedan contener información de una cuenta.
        const body=await response.json().catch(()=>null);
        const shape=response.status===401 ? body?.codigo==='NO_AUTENTICADO' : Array.isArray(body);
        row.status=response.status===row.expected&&shape?'APROBADA':'FALLIDA';
        row.cause=row.status==='FALLIDA'?'HTTP o forma de respuesta diferente al contrato':'';
      } catch(e) { row.cause=e.cause?.code||e.code||e.name; }
    }
    rows.push(row);
  }
  const result={runner:'Node fetch seguro; no Newman; no ejecuta pm.test',date:new Date().toISOString(),total:rows.length,approved:rows.filter(r=>r.status==='APROBADA').length,failed:rows.filter(r=>r.status==='FALLIDA').length,blocked:rows.filter(r=>r.status==='BLOQUEADA').length,attempted:rows.filter(r=>!r.cause.startsWith('No ejecutada')).length,rows};
  fs.writeFileSync(path.join(out,'resultados-http-seguros.json'),JSON.stringify(result,null,2));
  console.log(JSON.stringify({...result,rows:undefined}));
}
if(process.argv.includes('--report')) {
  const result=JSON.parse(fs.readFileSync(path.join(out,'resultados-http-seguros.json'),'utf8'));
  const escape=s=>String(s).replaceAll('|','/').replaceAll('\n',' ');
  const table=result.rows.map(r=>`| ${r.id} | ${escape(r.name)} | ${r.method} | ${r.expected} | ${r.obtained??'Sin HTTP'} | ${r.status} | ${escape(r.cause)} |`).join('\n');
  const header=`# Resultados Postman y HTTP — NOT-141\n\nFecha de ejecución HTTP: ${result.date} (UTC; Colombia UTC-5). Entorno: Render. Cliente: Node 24.18.0 en Windows.\n\n**Newman y pm.test no ejecutados.** Newman no está instalado; la importación en Postman UI no fue posible. Se prepararon 114 recursos nativos en la colección oficial y un exportador temporal v2.1. Sintaxis y guardas se comprobaron localmente. El JSON no se mantiene como segunda colección.\n\n| Métrica | Cantidad |\n|---|---:|\n| Casos nuevos preparados | ${result.total} |\n| GET seguros intentados | ${result.attempted} |\n| Sin envío por precondición/alcance | ${result.total-result.attempted} |\n| Aprobados Render | ${result.approved} |\n| Fallidos con HTTP | ${result.failed} |\n| Bloqueados | ${result.blocked} |\n| Casos Newman | 0 |\n\nLos 12 intentos terminaron en ENOTFOUND. Sin HTTP no se evalúan aserciones de aplicación. Los códigos esperados proceden del contrato; no son respuestas capturadas. El estado BLOQUEADA incluye casos sin envío.\n\n## Casos individuales\n\nCada fila corresponde a docs/api/NotiUrba API/NOT-141 ID.request.yaml; fecha y URL completa están en [evidencia JSON](evidencias/resultados-http-seguros.json).\n\n| ID | Escenario | Método | HTTP esperado | Obtenido | Resultado | Motivo |\n|---|---|---|---:|---|---|---|\n${table}\n\n## Cómo reanudar\n\nElegir el entorno Render existente; reutilizar token, codigoReporte y tokenOtraPersona. No guardar secretos en archivos. Ejecutar únicamente el subconjunto seguro antes de preparar recursos QA. Las guardas requieren Postman con pm.execution.skipRequest; si la versión no lo soporta, no ejecutar escrituras. La versión de Postman instalada no fue acreditada; importación pendiente.\n\nLa carpeta histórica NOT-45 (24 casos) permanece intacta y no se ejecutó: incluye operaciones que escriben/envían. No sumar sus casos a los 114 intentados o preparados en esta ampliación.\n\nLos catálogos no reciben cuerpo ni filtros, por lo que datos faltantes/caso de rol prohibido no aplican a esos GET públicos. Las operaciones de perfil propio no exigen un rol exclusivo; el acceso a reportes de otra cuenta se oculta con 404, no 403. DELETE no recibe cuerpo: no se inventa un negativo por campo faltante. Token vencido requiere token firmado previamente válido, exp pasado y confirmación de su procedencia; un texto malformado no acredita expiración.\n\nPUT válido de vigencia: solo entorno aislado, pues cambia configuración global. Cuarta foto: reporte QA con exactamente tres fotos ya persistidas y JPG/PNG válido de hasta 1 MiB. Se incluyen cargas 1, 2, 3 y cuarta rechazada. Antes de actualizar un reporte, consultar detalle para guardar estado/historial y verificar que se agrega una nota conservando estado oficial.\n\nContrato y ejemplos esperados: [CONTRATOS-Y-COBERTURA.md](CONTRATOS-Y-COBERTURA.md).\n`;
  fs.writeFileSync(path.join(root,'docs/validacion/NOT-141/RESULTADOS-POSTMAN.md'),header);
  fs.writeFileSync(path.join(root,'docs/validacion/NOT-141/indice-casos.json'),JSON.stringify(requests.map((i,n)=>({id:`N141-${String(n+1).padStart(3,'0')}`,name:i.name,file:i.file,method:i.request.method,url:i.request.url,expected:expected(i)})),null,2));
  console.log(JSON.stringify({report:'RESULTADOS-POSTMAN.md',rows:result.rows.length}));
}
