const fs=require('node:fs');
const path=require('node:path');
const http=require('node:http');
const root=process.env.NOT25_STORAGE_DIR;
if(!root)throw Error('Falta almacenamiento QA');
http.createServer((req,res)=>{
  const match=/^\/fotos\/([a-f0-9]{64}\.jpg)$/.exec(req.url);
  if(req.method!=='GET'||!match){res.writeHead(404);res.end();return;}
  const file=path.join(root,match[1]);
  if(!fs.existsSync(file)){res.writeHead(404);res.end();return;}
  res.writeHead(200,{'Content-Type':'image/jpeg','Content-Length':fs.statSync(file).size,'Cache-Control':'no-store'});
  fs.createReadStream(file).pipe(res);
}).listen(19026,'127.0.0.1',()=>console.log('NOT25 almacén HTTP local: 127.0.0.1:19026'));
