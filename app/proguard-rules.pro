# Reglas de R8 de MiGasto.
# No hacen falta reglas propias: la app no usa reflexión (Firestore se mapea a mano)
# y Room, Firebase, Compose y Coil incluyen sus propias reglas de consumidor.

# Credential Manager (inicio de sesión con Google) carga su proveedor de Play Services por reflexión.
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** {
  *;
}

# Conserva números de línea en los informes de errores.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
