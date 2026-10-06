# Reglas de R8 de MiGasto.
# No hacen falta reglas propias: la app no usa reflexión (Firestore se mapea a mano)
# y Room, Firebase, Compose y Coil incluyen sus propias reglas de consumidor.

# Conserva números de línea en los informes de errores.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
