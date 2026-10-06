# Publicar MiGasto en Google Play — guía paso a paso

Todo lo que pide Google Play ya está preparado en este repositorio. Esta guía te dice **qué subir y qué contestar** en cada pantalla de Play Console.

| Necesitas | Dónde está |
|---|---|
| App en formato **AAB** firmada | `app-release.aab` (o `./gradlew :app:bundleRelease`, o la pestaña *Actions* de GitHub) |
| **Keystore de subida** y contraseña | `migasto-upload.jks` + `CLAVES-FIRMA.txt` (te los he enviado aparte; **no están en el repositorio**) |
| Icono 512×512 | [`docs/play-store/icon-512.png`](play-store/icon-512.png) |
| Gráfico destacado 1024×500 | [`docs/play-store/feature-graphic.png`](play-store/feature-graphic.png) |
| Capturas de móvil (6) | [`docs/play-store/screenshots/`](play-store/screenshots) |
| Política de privacidad | [`docs/privacy-policy.html`](privacy-policy.html) |
| Página para eliminar la cuenta | [`docs/delete-account.html`](delete-account.html) |

> **Antes de nada decide el modo.** Para tener grupos compartidos entre varias personas, sigue primero [CONFIGURAR_FIREBASE.md](CONFIGURAR_FIREBASE.md) y compila de nuevo el AAB. Si publicas sin Firebase, la app funciona en modo local (cada móvil con sus datos).

---

## 1. Guarda bien la clave de firma

`migasto-upload.jks` es tu **clave de subida**: Google Play la usa para comprobar que las actualizaciones vienen de ti. Haz al menos dos copias (por ejemplo, en tu Drive y en un USB) junto con su contraseña. Con *Play App Signing* (paso 6) Google guarda la clave definitiva; si algún día pierdes la de subida, puedes pedir que la restablezcan desde Play Console.

## 2. Publica las páginas web (GitHub Pages, gratis)

1. En GitHub, abre el repositorio → **Settings → Pages**.
2. *Source*: **Deploy from a branch** → rama `main`, carpeta **`/docs`** → *Save*.
3. En un par de minutos tendrás:
   - Política de privacidad: `https://chiiraac.github.io/Migasto/privacy-policy.html`
   - Eliminar cuenta: `https://chiiraac.github.io/Migasto/delete-account.html`

   (La app ya enlaza a la primera desde *Ajustes*.) Para eso los cambios deben estar en la rama `main`.

## 3. Crea la cuenta de desarrollador

1. <https://play.google.com/console/signup> → pago único de **25 USD** y verificación de identidad.
2. Si es una **cuenta personal nueva**, Google exige una **prueba cerrada con al menos 12 personas durante 14 días seguidos** antes de poder publicar en producción (paso 7). Las cuentas de organización no tienen este requisito.

## 4. Crea la app

**Play Console → Crear aplicación**
- Nombre: `MiGasto: gastos de casa`
- Idioma predeterminado: **Español (España) – es-ES**
- Aplicación o juego: **Aplicación** · Gratis o de pago: **Gratis**
- Acepta las declaraciones.

## 5. Contenido de la aplicación (menú *Política → Contenido de la aplicación*)

| Sección | Qué responder |
|---|---|
| **Política de privacidad** | `https://chiiraac.github.io/Migasto/privacy-policy.html` |
| **Acceso a la aplicación** | *Modo nube*: «Parte de la funcionalidad está restringida» → crea en la app una cuenta de prueba (p. ej. `revision.migasto@gmail.com`) con un grupo y algunos movimientos, y escribe aquí el email y la contraseña. *Modo local*: «Toda la funcionalidad está disponible sin acceso especial». |
| **Anuncios** | No, mi app no contiene anuncios. |
| **Clasificación de contenido** | Cuestionario → categoría *Utilidad, productividad, comunicación u otros* → responde **No** a todo. Resultado: PEGI 3 / Todos. |
| **Público objetivo** | **18 años o más** (es una app de finanzas; así evitas los requisitos de apps para niños). |
| **Aplicación de noticias** | No. |
| **Apps gubernamentales** | No. |
| **Funciones financieras** | Indica que la app **solo sirve para llevar un registro de gastos/presupuesto personal**: no ofrece préstamos, pagos, banca, inversiones ni criptomonedas. |
| **Salud** | No. |
| **Seguridad de los datos** | Ver tabla siguiente. |

### Seguridad de los datos

**Si publicas en modo nube (Firebase):**

| Pregunta | Respuesta |
|---|---|
| ¿Recoge o comparte datos de usuario? | **Sí** |
| ¿Datos cifrados en tránsito? | **Sí** |
| ¿Los usuarios pueden pedir que se eliminen? | **Sí** → URL: `https://chiiraac.github.io/Migasto/delete-account.html` |
| ¿Se pueden crear cuentas? | Sí, con usuario y contraseña. Eliminación desde la app (*Ajustes → Eliminar mi cuenta*) y desde la URL anterior. |

Tipos de datos (todos: **recogidos**, **no compartidos**, tratados de forma **no efímera**):

| Tipo | ¿Obligatorio? | Finalidad |
|---|---|---|
| Información personal → **Nombre** | Obligatorio | Funcionalidad de la app, Gestión de la cuenta |
| Información personal → **Dirección de correo electrónico** | Obligatorio | Funcionalidad de la app, Gestión de la cuenta |
| Información financiera → **Otra información financiera** (movimientos que introduce el usuario) | Obligatorio | Funcionalidad de la app |
| Fotos y vídeos → **Fotos** (tickets) | Opcional | Funcionalidad de la app |

Firebase actúa como *proveedor de servicios*, por eso no cuenta como «compartir». Revisa también la guía oficial de Firebase para este formulario por si Google añade algo: <https://firebase.google.com/docs/android/play-data-disclosure>.

**Si publicas en modo local:** «Mi app no recoge ni comparte datos de usuario» (todo se queda en el dispositivo). En la pregunta de eliminación de cuenta indica que la app no permite crear cuentas.

## 6. Ficha de Play Store (*Crecimiento → Presencia en Play Store → Ficha principal*)

**Nombre** (máx. 30): `MiGasto: gastos de casa`

**Descripción breve** (máx. 80):
```
Gastos, ingresos y facturas de casa: banco y efectivo, calendario y grupos.
```

**Descripción completa**:
```
MiGasto es la forma más sencilla de llevar las cuentas de casa, solo o en compañía.

💰 TODO TU DINERO DE UN VISTAZO
• Balance total con el desglose de banco y efectivo
• Ingresos y gastos del mes
• Lista de movimientos con categoría, fecha y quién los añadió

✍️ APUNTA EN SEGUNDOS
• Gastos, ingresos y facturas
• Traspasos entre banco y efectivo (por ejemplo, al sacar del cajero)
• Más de 25 categorías con icono: supermercado, casa, luz, agua, ocio, transporte…
• Foto del ticket con la cámara o desde la galería
• Edita o elimina cualquier movimiento

📅 CALENDARIO
• Ve de un vistazo qué días hubo ingresos y gastos
• Toca un día para ver su detalle

📊 GRÁFICAS
• Evolución día a día, mes a mes o del año completo
• Gastos por categoría, por cuenta y por persona
• Descubre cuánto ahorras cada mes

👨‍👩‍👧 GRUPOS COMPARTIDOS
• Grupos para casa, pareja, piso compartido o viajes
• Invita con un código de 6 letras
• Todos ven los mismos movimientos al momento, incluso sin conexión se sincroniza después

🔒 PRIVADA Y SIN ANUNCIOS
• Sin publicidad ni rastreadores
• Solo los miembros de tu grupo ven tus datos
• Exporta tus movimientos a CSV (Excel, Google Sheets)
• Elimina tu cuenta desde la app cuando quieras

🌙 Modo oscuro y claro · Español e inglés
```
*(Si publicas en modo local, elimina el bloque «GRUPOS COMPARTIDOS» y cambia «Solo los miembros de tu grupo ven tus datos» por «Tus datos se quedan en tu móvil».)*

**Gráficos**
- Icono: `docs/play-store/icon-512.png`
- Gráfico destacado: `docs/play-store/feature-graphic.png`
- Capturas de teléfono: las 6 de `docs/play-store/screenshots/` en orden.

**Categoría y contacto** (*Configuración de la tienda*): categoría **Finanzas**; email de contacto (obligatorio, es público); sitio web opcional `https://chiiraac.github.io/Migasto/`.

**Inglés (opcional, *Añadir traducciones* → English (United States))**
- Nombre: `MiGasto: household expenses`
- Descripción breve: `Track household expenses and income, bank and cash, calendar and shared groups.`

## 7. Prueba cerrada y producción

1. **Pruebas → Prueba cerrada → Crear canal** (o usa *Alpha*).
2. **Testers**: añade una lista de emails (o un Grupo de Google) con al menos **12 personas** y compárteles el enlace de participación.
3. **Crear versión**:
   - Acepta **Play App Signing** (recomendado).
   - Sube `app-release.aab`.
   - Notas de la versión: `Primera versión de MiGasto.`
   - Guardar → Revisar versión → **Iniciar lanzamiento**.
4. Tras **14 días** con los 12 testers activos: **Panel → Solicitar acceso a producción**, responde el breve cuestionario y, cuando lo aprueben, **Producción → Crear versión** con el mismo AAB (o uno nuevo) → **Enviar a revisión**.
5. La primera revisión suele tardar de unas horas a 7 días.

## 7 bis. «Invítame a un café» (Bizum) y Google Play

La app incluye en *Ajustes* un apartado para invitarte a un café por Bizum. **Atención:** la política de pagos de Google Play exige usar su sistema de cobro para pagos dentro de la app y Google ha retirado apps por enlazar a donaciones externas (fuera de EE. UU. la excepción solo cubre donativos a entidades benéficas). Para no arriesgar la publicación:

- **APK que instalas tú o compartes directamente:** puede llevar el apartado de Bizum sin problema.
- **AAB para Google Play:** se compila **sin** el apartado (así lo has decidido):
  ```bash
  ./gradlew :app:bundleRelease -Pmigasto.bizum=false
  ```
  GitHub Actions ya genera el AAB sin Bizum. Si algún día quieres incluirlo, crea la variable del repositorio `MIGASTO_BIZUM_EN_PLAY` con valor `true` (*Settings → Secrets and variables → Actions → Variables*).
- Si quieres donaciones dentro de la versión de Play, la forma permitida es un producto de compra integrada («Café», p. ej. 1,99 €) con Google Play Billing.

## 8. Publicar actualizaciones

1. En `app/build.gradle.kts` sube `versionCode` (2, 3, 4…) y `versionName` (`1.0.1`, `1.1.0`…).
2. `./gradlew :app:bundleRelease` (o espera al AAB de GitHub Actions).
3. Play Console → Producción → Crear versión → sube el nuevo AAB.

## 9. Instalar el APK directamente (sin Play Store)

`app-release.apk` se puede instalar en cualquier Android 8 o superior: cópialo al móvil, ábrelo y permite «instalar apps de origen desconocido». Las actualizaciones posteriores deben firmarse con **la misma clave** para instalarse encima sin perder datos. Ten en cuenta que la versión que descargues de Google Play (firmada por Google con Play App Signing) **no** se puede instalar encima de un APK firmado por ti: habría que desinstalar primero.
