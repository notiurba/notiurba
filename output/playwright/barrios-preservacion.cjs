const fs=require('node:fs');
const path=require('node:path');
const crypto=require('node:crypto');
const cp=require('node:child_process');
const root=path.resolve(__dirname,'../..');
const dest=path.join(root,'docs/validacion/evidencias-barrios-frontend');
const baseline=path.join(dest,'preservacion-antes.json');
const sha=p=>crypto.createHash('sha256').update(fs.readFileSync(p)).digest('hex');
if(!fs.existsSync(baseline)){
  const tracked=cp.execFileSync('git',['ls-files','--','backend','frontend/src/compartido/geo'],{cwd:root,encoding:'utf8'}).trim().split(/\r?\n/).filter(Boolean);
  const evidence=fs.readdirSync(path.join(root,'docs/validacion/evidencias-NOT-65')).filter(n=>fs.statSync(path.join(root,'docs/validacion/evidencias-NOT-65',n)).isFile()).map(n=>'docs/validacion/evidencias-NOT-65/'+n);
  const protectedFiles=[...tracked,...evidence,'docs/validacion/NOT-65-informe.md','frontend/src/app/reportar/PasoUbicacion.jsx','frontend/src/app/reportar/ReportarLayout.jsx','frontend/src/compartido/api/catalogos.js'];
  fs.writeFileSync(baseline,JSON.stringify({date:new Date().toISOString(),files:Object.fromEntries(protectedFiles.map(p=>[p,sha(path.join(root,p))]))},null,2));
  fs.copyFileSync(path.join(root,'frontend/src/app/reportar/PasoUbicacion.jsx'),path.join(dest,'PasoUbicacion-antes.jsx'));
  console.log('Baseline recorded; pre-existing PasoUbicacion preserved.');
}else{
  const old=JSON.parse(fs.readFileSync(baseline,'utf8'));
  const results=Object.entries(old.files).map(([file,hash])=>({file,unchanged:sha(path.join(root,file))===hash}));
  const allowed=['frontend/src/app/reportar/PasoUbicacion.jsx','frontend/src/app/reportar/ReportarLayout.jsx','frontend/src/compartido/api/catalogos.js'];
  const prohibitedChanges=results.filter(r=>!r.unchanged&&!allowed.includes(r.file));
  fs.writeFileSync(path.join(dest,'preservacion-despues.json'),JSON.stringify({date:new Date().toISOString(),prohibitedChanges,allProtectedUnchanged:prohibitedChanges.length===0,results},null,2));
  console.log(JSON.stringify({prohibitedChanges,allProtectedUnchanged:prohibitedChanges.length===0}));
  if(prohibitedChanges.length)process.exitCode=1;
}
