const fs=require('node:fs'),path=require('node:path');
const dir='docs/validacion/evidencias-NOT-59-20261108/';
const json=p=>JSON.parse(fs.readFileSync(p,'utf8').replace(/^\uFEFF/,''));
let log=fs.readFileSync('output/not59-duplicado-real.log','utf16le').replace(/^\uFEFF/,'');if(!log.includes('### Result'))log=fs.readFileSync('output/not59-duplicado-real.log','utf8');if(!log.includes('### Result'))throw Error('Falta resultado real');
const actual=JSON.parse(log.split('### Result')[1].trim().split(/\r?\n/)[0]);
const before=json(dir+'sql-antes.json'),after=json(dir+'sql-despues.json');
if(JSON.stringify(before)!==JSON.stringify(after)||after.users!==1||after.matchingEmail!==1||after.newDocument!==0||after.verificationTokens!==0||actual.apiStatus!==409||!actual.messageVisible)throw Error('Duplicado o integridad no demostrado');
const old='docs/validacion/evidencias-nueve-tareas-20261107-163018/';
const desktop=json(old+'registro-desktop-resultados.json'),mobile=json(old+'mobile-registro-validacion.json').observed,keyboard=json(old+'mobile-registro-teclado.json').observed,vacios=json(old+'mobile-registro-vacios.json').observed;
if(desktop.emptyErrors.length!==4||!desktop.consentError||!desktop.backPreserved||!desktop.forwardPreserved||desktop.keyboardFocus!=='password'||!mobile.consentMissing||!mobile.preservedNames||!mobile.preservedAccess||!keyboard.passed||vacios.errors.length!==4)throw Error('Evidencia previa insuficiente');
for(const width of [360,1366]){const v=desktop.viewports.find(v=>v.width===width);if(!v||v.client!==v.scroll)throw Error('Falta responsive '+width);}
const result={task:'NOT-59',verdict:'NOT-59 LISTA PARA DONE',scope:'Cierre de la tarea de pruebas y documentación; no aprobación del registro integral en producción',checkedAt:new Date().toISOString(),newCase:actual,sqlBefore:before,sqlAfter:after,rowsCreated:0,mailsOrTokensCreated:0,matrix:[
{case:'Correo duplicado',status:'APROBADO',kind:'React/Spring Boot/PostgreSQL reales aislados',evidence:'correo-duplicado-real.json'},
{case:'Campos vacíos',status:'APROBADO',kind:'UI previa móvil real y escritorio; API simulada',evidence:old+'mobile-registro-vacios.json'},
{case:'Aviso sin marcar',status:'APROBADO',kind:'Validación frontend previa',evidence:old+'mobile-registro-validacion.json'},
{case:'Teclado',status:'APROBADO',kind:'Tab escritorio y CDP en teléfono real; no teclado físico externo',evidence:old+'mobile-registro-teclado.json'},
{case:'Conservación de datos',status:'APROBADO',kind:'Atrás/adelante en resultados previos',evidence:old+'registro-desktop-resultados.json'},
{case:'Responsive 360 px',status:'APROBADO',kind:'Chrome escritorio 360x800; NO teléfono físico a 360 CSS',evidence:old+'registro-360.png'},
{case:'Responsive 1366 px',status:'APROBADO',kind:'Chrome escritorio 1366x768',evidence:old+'registro-1366.png'}],physicalPhone:{model:'Xiaomi del usuario',serial:'DEQCAQEUVS9PNRDQ',actualCssWidth:375,scrollWidth:375,native360TestClaimed:false,previousEvidenceReused:true},productionRegistration:{status:'BLOQUEADO POR CATÁLOGO SIN BARRIOS PÚBLICOS',v17Applied:false,productionTouched:false,checkedFrom:'Contexto confirmado y auditoría anterior; no nueva consulta productiva'},applicationFilesChanged:false,fullSuiteRepeated:false,linearReadOrWritten:false};
fs.writeFileSync(dir+'correo-duplicado-real.json',JSON.stringify(actual,null,2)+'\n',{flag:'wx'});
fs.writeFileSync(dir+'resultados.json',JSON.stringify(result,null,2)+'\n',{flag:'wx'});
fs.copyFileSync('output/playwright/not59-duplicado-real.js',dir+'verificacion-duplicado.js',fs.constants.COPYFILE_EXCL);
fs.copyFileSync('output/vite-not59-qa.mjs',dir+'vite-qa.config.referencia.mjs',fs.constants.COPYFILE_EXCL);
console.log(JSON.stringify({verdict:result.verdict,newApiStatus:409,usersBefore:before.users,usersAfter:after.users,newAccounts:0,existingCasesReused:6,phoneActualWidth:375}));
