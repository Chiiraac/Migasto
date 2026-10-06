# Activar el modo nube (Firebase)

Sin configuración, MiGasto funciona en **modo local**: los datos se guardan solo en el móvil y no hay cuentas. Para tener **cuentas con email**, **sincronización** y **grupos compartidos con código de invitación** (como en tu app original), conecta la app a un proyecto de Firebase. Es gratis (plan Spark) y se tarda unos 10 minutos.

## 1. Crear el proyecto

1. Entra en <https://console.firebase.google.com> con tu cuenta de Google.
2. **Crear un proyecto** → nombre `MiGasto`. Google Analytics no hace falta (puedes desactivarlo).

> Proyecto de MiGasto: **MiGasto** (`migasto-chiiraac`). El archivo `.firebaserc` ya apunta a él.

### Atajo: configurarlo todo desde Cloud Shell
En la consola de Firebase, abre **Cloud Shell** (icono `>_` arriba a la derecha) y pega:
```bash
P=migasto-chiiraac
gcloud services enable firestore.googleapis.com --project $P
gcloud firestore databases create --location=eur3 --project $P
git clone -b claude/android-app-apk-playstore-ol8yi9 https://github.com/Chiiraac/Migasto.git && cd Migasto
firebase login --no-localhost
firebase deploy --only firestore:rules --project $P
firebase apps:create ANDROID MiGasto --package-name com.chiiraac.migasto --project $P
firebase apps:sdkconfig ANDROID --project $P --out google-services.json
cloudshell download google-services.json
```
Solo falta activar **Authentication → Correo electrónico/contraseña** desde la consola (paso 3).

> **Si `firebase login` se complica**, no hace falta: pulsa `Ctrl+C`, registra la app con el formulario web (paso 2) y publica las reglas desde la consola (paso 5, opción A). En ese caso **no** ejecutes `firebase apps:create`, porque crearía una segunda app duplicada. Los comandos `gcloud` del principio sí funcionan sin login.

## 2. Registrar la app Android

1. En la página del proyecto, pulsa el icono de **Android** para añadir una app.
2. **Nombre del paquete**: `com.chiiraac.migasto` (exactamente así).
3. Apodo: `MiGasto`. El certificado SHA-1 no es necesario.
4. Descarga **`google-services.json`** y cópialo en la carpeta **`app/`** del proyecto (junto a `app/build.gradle.kts`).
   - No hace falta tocar ningún archivo Gradle: el proyecto ya activa el plugin de Google Services automáticamente cuando encuentra ese archivo.
   - El archivo está en `.gitignore` para no publicarlo en el repositorio.

## 3. Activar el inicio de sesión con email

1. Menú **Build → Authentication → Comenzar**.
2. Pestaña **Sign-in method** → **Correo electrónico/contraseña** → **Habilitar** → Guardar.
3. (Recomendado) Pestaña **Plantillas** → cambia el idioma a **Español** para que el email de «restablecer contraseña» llegue en español.

### Inicio de sesión con Google (opcional, recomendado)
1. **Authentication → Sign-in method → Agregar proveedor nuevo → Google → Habilitar**. Elige el *correo de asistencia* y pulsa Guardar.
2. ⚙️ **Configuración del proyecto → General → Tus apps → MiGasto → Agregar huella digital**. Añade la **SHA-1** (y si quieres la SHA-256) del certificado con el que se firma la app:
   - Clave de subida (el APK que instalas tú): `keytool -list -v -keystore release-output/migasto-upload.jks -alias migasto` (o mira `CLAVES-FIRMA.txt`).
   - Cuando publiques en Google Play, añade también la de **Play App Signing**: *Play Console → tu app → Prueba y lanzamiento → Configuración → Integridad de la app → Firma de apps* → «Huella digital del certificado SHA-1». No hace falta recompilar.
3. **Descarga de nuevo `google-services.json`** y cópialo en `app/`. El nuevo incluye el «ID de cliente web» que necesita el botón **Continuar con Google**; sin él, el botón no aparece (la compilación avisa con un *warning*). Si usas GitHub Actions, **actualiza también el secreto `GOOGLE_SERVICES_JSON`** con el contenido del archivo nuevo.

Si al pulsar el botón sale «El inicio de sesión con Google no está disponible en esta versión», falta la huella SHA-1 del certificado con el que está firmada esa copia de la app.

### Avisos de movimientos nuevos (opcional, plan Blaze)
Cuando un miembro añade un movimiento, el resto del grupo recibe una notificación (cada uno puede desactivarla por grupo en *Ajustes del grupo*). La envía la Cloud Function de [`functions/`](../functions), que necesita el plan **Blaze** (pago por uso; con el uso de una familia o un piso compartido, el coste es prácticamente 0 €).

1. Consola de Firebase → abajo a la izquierda, plan **Spark** → **Actualizar** → **Blaze** y vincula una cuenta de facturación.
2. Pon una alerta: <https://console.cloud.google.com/billing/budgets> → **Crear presupuesto** → importe **1 €**, avisos al 50 %, 90 % y 100 %.
3. En **Cloud Shell** (icono `>_` de la consola) pega esta única línea:
   ```bash
   cd ~ && rm -rf Migasto && git clone -q -b claude/android-app-apk-playstore-ol8yi9 https://github.com/Chiiraac/Migasto.git && bash Migasto/tools/desplegar-firebase.sh
   ```
   El script ([`tools/desplegar-firebase.sh`](../tools/desplegar-firebase.sh)) instala lo necesario, solo pide `firebase login` si hace falta (abre el enlace, entra con tu cuenta y pega el código del final; ese código no se comparte con nadie) y publica las reglas y la función, reintentando solo si el primer despliegue falla por los permisos internos que Google prepara la primera vez.

## 4. Crear la base de datos

1. Menú **Build → Firestore Database → Crear base de datos**.
2. Ubicación: una europea, por ejemplo `eur3 (europe-west)` o `europe-southwest1 (Madrid)`. **No se puede cambiar después.**
3. Empieza en **modo de producción**.

## 5. Publicar las reglas de seguridad

Las reglas garantizan que **solo los miembros de un grupo** puedan ver y modificar sus datos.

- **Opción A (consola):** Firestore Database → pestaña **Reglas** → borra lo que haya, pega el contenido de [`firestore.rules`](../firestore.rules) y pulsa **Publicar**.
- **Opción B (terminal):**
  ```bash
  npx firebase-tools login
  npx firebase-tools deploy --only firestore:rules --project TU-ID-DE-PROYECTO
  ```

## 6. Compilar la versión con nube

```bash
./gradlew :app:assembleRelease   # APK
./gradlew :app:bundleRelease     # AAB para Google Play
```

En **Ajustes → Información de la App** verás «Sincronización en la nube activada».

### Con GitHub Actions (sin instalar nada)
En GitHub: **Settings → Secrets and variables → Actions → New repository secret**:

| Secreto | Valor |
|---|---|
| `GOOGLE_SERVICES_JSON` | Contenido completo de `google-services.json` |
| `MIGASTO_KEYSTORE_BASE64` | El keystore en base64 (`base64 -w0 migasto-upload.jks`) |
| `MIGASTO_KEYSTORE_PASSWORD` | Contraseña del keystore |
| `MIGASTO_KEY_ALIAS` | `migasto` |
| `MIGASTO_KEY_PASSWORD` | Contraseña de la clave (la misma) |

Cada push compila el APK y el AAB firmados y los deja en la pestaña **Actions** como artefactos descargables.

## Preguntas frecuentes

**¿Cuánto cuesta?** Nada para un uso familiar. El plan gratuito incluye 1 GiB de datos, 50.000 lecturas y 20.000 escrituras al día. Las fotos se guardan comprimidas (menos de 700 KB) dentro de Firestore precisamente para no necesitar Cloud Storage, que exige el plan de pago.

**¿Funciona sin conexión?** Sí. Firestore guarda una copia en el móvil: puedes apuntar gastos sin cobertura y se sincronizan al volver la conexión. Lo único que necesita conexión es iniciar sesión y unirse a un grupo.

**¿Cómo invito a alguien?** Pulsa la rueda dentada de arriba a la derecha (Ajustes del grupo): verás el código de 6 letras y un botón para compartirlo. La otra persona instala la app, se registra y en «Tus Grupos → Unirse a otro grupo» escribe el código.

**Tenía datos en modo local, ¿se pasan a la nube?** No automáticamente. Antes de instalar la versión con nube, usa **Ajustes → Exportar movimientos (CSV)** para guardar una copia.

**Probar las reglas en tu ordenador:** `cd firebase-tests && npm ci && npm test` (necesita Java 11+).
