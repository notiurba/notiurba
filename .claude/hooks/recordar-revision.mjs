// Hook PostToolUse (Edit|Write): cuando Claude cambia código del proyecto, le recuerda pasar el
// subagente code-reviewer al terminar la tarea. Avisa como máximo una vez cada 15 minutos por sesión.
import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

const CODIGO = /(^|[\\/])(backend[\\/]src|frontend[\\/]src|tools)[\\/].+\.(java|sql|js|jsx|mjs|ts|tsx|css|py|sh|ps1)$/;
const ESPERA_MS = 15 * 60 * 1000;

let entrada = '';
process.stdin.on('data', (parte) => { entrada += parte; });
process.stdin.on('end', () => {
  let datos;
  try {
    datos = JSON.parse(entrada);
  } catch {
    return;
  }
  const archivo = datos?.tool_input?.file_path ?? datos?.tool_response?.filePath ?? '';
  if (!CODIGO.test(archivo)) return;

  const marca = join(tmpdir(), `notiurba-revision-${datos.session_id ?? 'sin-sesion'}`);
  const ahora = Date.now();
  if (existsSync(marca) && ahora - Number(readFileSync(marca, 'utf8')) < ESPERA_MS) return;
  writeFileSync(marca, String(ahora));

  process.stdout.write(JSON.stringify({
    hookSpecificOutput: {
      hookEventName: 'PostToolUse',
      additionalContext:
        `Se modificó código del proyecto (${archivo}). Regla de CLAUDE.md: al terminar la tarea, ` +
        'antes de cerrarla, usa el subagente code-reviewer sobre todos los cambios de la rama, ' +
        'corrige sus hallazgos con pruebas y reporta el antes/después.',
    },
  }));
});
