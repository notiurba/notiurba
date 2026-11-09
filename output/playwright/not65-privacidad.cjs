const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const root = path.resolve(__dirname, '../../docs/validacion/evidencias-NOT-65');
const dest = path.join(root, 'privado');
fs.mkdirSync(dest, {recursive:true});
const files = fs.readdirSync(root).filter(n => /^android-\d{2}-.+\.(png|json)$/.test(n));
const manifest = [];
for (const name of files) {
  const from = path.resolve(root, name), to = path.resolve(dest, name);
  if (path.dirname(from) !== root || path.dirname(to) !== dest) throw Error('Outside evidence directory');
  if (fs.existsSync(to)) throw Error('Refusing overwrite: '+name);
  const bytes=fs.readFileSync(from);
  fs.renameSync(from, to);
  manifest.push({file:name,bytes:bytes.length,sha256:crypto.createHash('sha256').update(bytes).digest('hex'),private:true});
}
const priorPath = path.join(root,'privacidad-android.json');
const prior = fs.existsSync(priorPath) ? JSON.parse(fs.readFileSync(priorPath,'utf8')).files : [];
fs.writeFileSync(priorPath,JSON.stringify({date:new Date().toISOString(),policy:'Originales preservados solo en privado/, excluidos de Git. No adjuntar originales a Linear/Figma ni servicios compartidos. Publicar solo resultados agregados sin coordenadas.',physicalTestsRepeated:false,screenshots:15,files:[...prior,...manifest]},null,2));
console.log(JSON.stringify({originalsMoved:manifest.length,privateDirectory:'docs/validacion/evidencias-NOT-65/privado/',screenshots:15}));
