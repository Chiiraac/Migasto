// Vídeo promocional vertical (1080×1920, ~25 s, sin audio) a partir de las capturas de Google Play.
//   1. Renderiza la portada y el cierre con Playwright (mismo estilo que play-store-assets.mjs).
//   2. Une portada + docs/play-store/screenshots/*.png + cierre con ffmpeg (zoom suave y fundidos).
// Requisitos: Node 18+, Playwright y ffmpeg. Uso: node tools/promo-video.mjs [salida.mp4]
import { createRequire } from 'node:module';
import { mkdirSync, readdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { execFileSync } from 'node:child_process';
import { tmpdir } from 'node:os';

const { chromium } = await import('playwright').catch(() => createRequire(import.meta.url)('playwright'));

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const shotsDir = join(root, 'docs/play-store/screenshots');
const output = process.argv[2] ?? join(root, 'release-output/MiGasto-promo.mp4');
const work = join(tmpdir(), 'migasto-promo');
mkdirSync(work, { recursive: true });
mkdirSync(dirname(output), { recursive: true });

const walletSvg = `
  <g transform="rotate(-9 54 44)">
    <path fill="#34D399" d="M40,31h28a5,5 0,0 1,5 5v12h-38v-12a5,5 0,0 1,5 -5z"/>
    <path fill="#10B981" d="M35,38h38v3h-38z"/>
  </g>
  <path fill="#FFFFFF" d="M38,42h32a8,8 0,0 1,8 8v16a8,8 0,0 1,-8 8h-32a8,8 0,0 1,-8 -8v-16a8,8 0,0 1,8 -8z"/>
  <path fill="#E0E7FF" d="M63,51h15v14h-15a7,7 0,0 1,-7 -7a7,7 0,0 1,7 -7z"/>
  <circle fill="#6366F1" cx="63" cy="58" r="3"/>`;

const icon = (size) => `
  <svg width="${size}" height="${size}" viewBox="0 0 108 108" style="filter: drop-shadow(0 30px 60px rgba(0,0,0,.35))">
    <defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0" stop-color="#7C7FF6"/><stop offset="1" stop-color="#4338CA"/></linearGradient></defs>
    <rect width="108" height="108" rx="26" fill="url(#g)"/>
    <g transform="translate(54 54) scale(1.25) translate(-54 -54)">${walletSvg}</g>
  </svg>`;

const page = (body) => `<!doctype html><html><head><style>
  @import url('https://fonts.googleapis.com/css2?family=Inter:wght@500;700;800&display=block');
  * { margin: 0; padding: 0; box-sizing: border-box; }
  body { width: 1080px; height: 1920px; overflow: hidden; color: #fff;
    font-family: 'Inter', 'Liberation Sans', 'DejaVu Sans', sans-serif; -webkit-font-smoothing: antialiased;
    background: radial-gradient(1200px 900px at 50% 30%, #4F46E5 0%, #312E81 55%, #0B1120 100%);
    display: flex; flex-direction: column; align-items: center; justify-content: center; text-align: center; }
  h1 { font-size: 150px; font-weight: 800; letter-spacing: -4px; margin-top: 70px; }
  .tag { font-size: 58px; font-weight: 500; line-height: 1.25; margin-top: 36px; color: #E0E7FF; }
  .pills { display: flex; flex-wrap: wrap; gap: 22px; justify-content: center; margin-top: 80px; width: 900px; }
  .pill { font-size: 40px; font-weight: 700; padding: 22px 38px; border-radius: 999px;
    background: rgba(255,255,255,.12); border: 2px solid rgba(255,255,255,.22); }
  .soon { margin-top: 110px; font-size: 52px; font-weight: 700; color: #34D399; }
</style></head><body>${body}</body></html>`;

const frames = {
  intro: page(`${icon(380)}<h1>MiGasto</h1>
    <p class="tag">Las cuentas de casa,<br>claras y compartidas.</p>`),
  outro: page(`${icon(300)}<h1 style="font-size:130px">MiGasto</h1>
    <div class="pills"><span class="pill">Gratis</span><span class="pill">Sin anuncios</span>
      <span class="pill">Banco y efectivo</span><span class="pill">Grupos compartidos</span></div>
    <p class="soon">Muy pronto en Google Play</p>`),
};

const browser = await chromium.launch();
const tab = await browser.newPage({ viewport: { width: 1080, height: 1920 } });
for (const [name, html] of Object.entries(frames)) {
  await tab.setContent(html, { waitUntil: 'networkidle' });
  await tab.screenshot({ path: join(work, `${name}.png`) });
}
await browser.close();

// Portada, las capturas en orden y el cierre.
const shots = readdirSync(shotsDir).filter((f) => f.endsWith('.png')).sort().map((f) => join(shotsDir, f));
const clips = [
  { file: join(work, 'intro.png'), seconds: 3 },
  ...shots.map((file) => ({ file, seconds: 3.2 })),
  { file: join(work, 'outro.png'), seconds: 3.8 },
];

const FPS = 30;
const FADE = 0.5;
const inputs = clips.flatMap((c) => ['-loop', '1', '-framerate', String(FPS), '-t', String(c.seconds), '-i', c.file]);
// Zoom suave (Ken Burns) sobre una versión ampliada para evitar saltos de píxel.
const zooms = clips.map((c, i) => {
  const frames = Math.round(c.seconds * FPS);
  return `[${i}:v]scale=2160:3840,zoompan=z='1+0.035*on/${frames}':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)'` +
    `:d=1:s=1080x1920:fps=${FPS},setsar=1,format=yuv420p[v${i}]`;
});
let chain = '[v0]';
let offset = 0;
const fades = [];
for (let i = 1; i < clips.length; i++) {
  offset += clips[i - 1].seconds - FADE;
  const out = i === clips.length - 1 ? '[out]' : `[x${i}]`;
  fades.push(`${chain}[v${i}]xfade=transition=fade:duration=${FADE}:offset=${offset.toFixed(2)}${out}`);
  chain = out;
}
const filter = [...zooms, ...fades].join(';');
writeFileSync(join(work, 'filter.txt'), filter);

execFileSync('ffmpeg', [
  '-y', '-loglevel', 'error', ...inputs,
  '-filter_complex_script', join(work, 'filter.txt'), '-map', '[out]',
  '-c:v', 'libx264', '-preset', 'medium', '-crf', '20', '-pix_fmt', 'yuv420p', '-movflags', '+faststart',
  output,
], { stdio: 'inherit' });
console.log(`Vídeo: ${output}`);
