// Genera los gráficos de la ficha de Google Play en docs/play-store/:
//   - icon-512.png          Icono de alta resolución (512×512)
//   - feature-graphic.png   Gráfico destacado (1024×500)
//   - screenshots/*.png     Capturas 1080×1920 con texto
//
// Requisitos: Node 18+ y Playwright (npm i -g playwright && npx playwright install chromium).
// Antes, renderiza las capturas base con:
//   ./gradlew :app:testDebugUnitTest --tests '*PlayStoreScreenshotTest*'
// Uso: node tools/play-store-assets.mjs
import { createRequire } from 'node:module';
import { readFileSync, mkdirSync, existsSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

// Playwright local o global (require respeta NODE_PATH; import no).
const { chromium } = await import('playwright').catch(() => createRequire(import.meta.url)('playwright'));

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const rawDir = join(root, 'app/build/play-store/raw');
const outDir = join(root, 'docs/play-store');
mkdirSync(join(outDir, 'screenshots'), { recursive: true });

const fontCss = `
  @import url('https://fonts.googleapis.com/css2?family=Inter:wght@500;700;800&display=block');
  * { margin: 0; padding: 0; box-sizing: border-box; }
  body { font-family: 'Inter', 'Liberation Sans', 'DejaVu Sans', sans-serif; -webkit-font-smoothing: antialiased; }
`;

// Mismo dibujo que app/src/main/res/drawable/ic_launcher_foreground.xml
const walletSvg = (scale = 1) => `
  <g transform="translate(54 54) scale(${scale}) translate(-54 -54)">
    <g transform="rotate(-9 54 44)">
      <path fill="#34D399" d="M40,31h28a5,5 0,0 1,5 5v12h-38v-12a5,5 0,0 1,5 -5z"/>
      <path fill="#10B981" d="M35,38h38v3h-38z"/>
    </g>
    <path fill="#FFFFFF" d="M38,42h32a8,8 0,0 1,8 8v16a8,8 0,0 1,-8 8h-32a8,8 0,0 1,-8 -8v-16a8,8 0,0 1,8 -8z"/>
    <path fill="#E0E7FF" d="M63,51h15v14h-15a7,7 0,0 1,-7 -7a7,7 0,0 1,7 -7z"/>
    <circle fill="#6366F1" cx="63" cy="58" r="3"/>
  </g>`;

const iconSvg = (size, rounded = false, scale = 1.25) => `
  <svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 108 108">
    <defs>
      <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0" stop-color="#7C7FF6"/><stop offset="1" stop-color="#4338CA"/>
      </linearGradient>
    </defs>
    <rect width="108" height="108" ${rounded ? 'rx="26"' : ''} fill="url(#bg)"/>
    ${walletSvg(scale)}
  </svg>`;

const captions = {
  '1_home': ['Tu dinero de casa,', 'claro de un vistazo'],
  '2_add': ['Apunta un gasto en segundos,', 'con foto del ticket'],
  '3_calendar': ['Revisa cada día', 'en el calendario'],
  '4_stats': ['Descubre en qué', 'se va tu dinero'],
  '5_group': ['Comparte las cuentas', 'con tu pareja o tu piso'],
  '6_light': ['Banco y efectivo,', 'en modo claro u oscuro'],
};

const dataUrl = (file) => `data:image/png;base64,${readFileSync(file).toString('base64')}`;

const browser = await chromium.launch(
  process.env.HTTPS_PROXY ? { proxy: { server: process.env.HTTPS_PROXY } } : {},
);
const page = await browser.newPage({ deviceScaleFactor: 1 });

async function render(html, width, height, file) {
  await page.setViewportSize({ width, height });
  await page.setContent(`<html><head><style>${fontCss}</style></head><body>${html}</body></html>`, { waitUntil: 'networkidle' });
  await page.evaluate(() => document.fonts.ready);
  await page.screenshot({ path: join(outDir, file), omitBackground: false });
  console.log('✓', file);
}

// Icono 512×512 (Google Play aplica la máscara redondeada)
await render(`<div style="width:512px;height:512px">${iconSvg(512)}</div>`, 512, 512, 'icon-512.png');

// Gráfico destacado 1024×500
const home = join(rawDir, '1_home.png');
await render(`
  <div style="width:1024px;height:500px;position:relative;overflow:hidden;
              background:radial-gradient(circle at 15% 20%, #7C7FF6 0%, #4F46E5 45%, #1E1B4B 100%);">
    <div style="position:absolute;left:72px;top:110px;display:flex;flex-direction:column;gap:22px;width:520px">
      <div style="display:flex;align-items:center;gap:26px">
        <div style="width:120px;height:120px;border-radius:30px;overflow:hidden;box-shadow:0 18px 40px rgba(0,0,0,.35)">${iconSvg(120)}</div>
        <div style="color:white;font-size:76px;font-weight:800;letter-spacing:-1.5px">MiGasto</div>
      </div>
      <div style="color:#E0E7FF;font-size:34px;font-weight:500;line-height:1.25">Las cuentas de casa,<br>claras y compartidas.</div>
      <div style="display:flex;gap:12px;margin-top:6px">
        ${['Gastos y facturas', 'Banco y efectivo', 'Grupos'].map((t) =>
          `<span style="color:white;background:rgba(255,255,255,.14);border:1px solid rgba(255,255,255,.25);
                  padding:9px 18px;border-radius:999px;font-size:20px;font-weight:600;white-space:nowrap;
                  line-height:1">${t}</span>`).join('')}
      </div>
    </div>
    ${existsSync(home) ? `
    <img src="${dataUrl(home)}" style="position:absolute;right:70px;top:46px;width:300px;border-radius:34px;
         border:8px solid #0B1120;box-shadow:0 30px 60px rgba(0,0,0,.5);transform:rotate(4deg)">` : ''}
  </div>`, 1024, 500, 'feature-graphic.png');

// Capturas 1080×1920
for (const [name, lines] of Object.entries(captions)) {
  const file = join(rawDir, `${name}.png`);
  if (!existsSync(file)) {
    console.warn('Falta', file, '— ejecuta antes PlayStoreScreenshotTest');
    continue;
  }
  const light = name === '6_light';
  await render(`
    <div style="width:1080px;height:1920px;position:relative;overflow:hidden;
                background:${light
                  ? 'linear-gradient(180deg,#EEF2FF 0%,#E0E7FF 100%)'
                  : 'linear-gradient(180deg,#4F46E5 0%,#312E81 38%,#111827 100%)'};">
      <div style="position:absolute;top:120px;width:100%;text-align:center;color:${light ? '#1E1B4B' : 'white'};
                  font-size:68px;font-weight:800;line-height:1.18;letter-spacing:-1px">
        ${lines.join('<br>')}
      </div>
      <img src="${dataUrl(file)}" style="position:absolute;left:50%;top:430px;width:790px;transform:translateX(-50%);
           border-radius:56px;border:10px solid ${light ? '#FFFFFF' : '#0B1120'};
           box-shadow:0 40px 90px rgba(0,0,0,${light ? '.18' : '.55'})">
    </div>`, 1080, 1920, `screenshots/${name}.png`);
}

await browser.close();
