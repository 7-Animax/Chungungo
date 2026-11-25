# Chungungo

Inventario del hogar para Android, desarrollado en Java con interfaces XML.
Integrantes: Aarón Luna, Elvis Huisa y Matías Velarde.

## Configurar y compilar

1. Abre esta carpeta en Android Studio con JDK 21 y el SDK de compilación Android 36.1. La aplicación requiere Android 12 (API 31) o superior.
2. En el proyecto Firebase del equipo, usa la app Android registrada como `com.devst.chungungo`. Descarga su `google-services.json` y colócalo en `app/`. Comparte la configuración Android con el equipo y el evaluador por el canal autorizado.
3. Habilita Cloud Firestore y el proveedor de correo electrónico y contraseña en Firebase Authentication.
4. Publica el contenido de `firestore.rules` en la pestaña **Reglas** de Cloud Firestore. Si el proyecto aloja otras aplicaciones, integra las reglas de Chungungo con las existentes y conserva las de las demás aplicaciones. Con Firebase CLI, también puedes usar `firebase deploy --only firestore:rules --project ID_DEL_PROYECTO`.
5. Sincroniza Gradle, ejecuta `.\gradlew.bat assembleDebug` e instala la app en un dispositivo o emulador compatible.

## Persistencia híbrida

El repositorio guarda cada operación en SQLite con el UID de la sesión y un identificador estable de Firestore. WorkManager ejecuta una cadena por UID con requisito de conexión. Al volver al inventario, la aplicación también solicita una lectura del servidor y actualiza el listado tras la sincronización.

Un cambio local pendiente tiene prioridad frente a una lectura remota. El DAO confirma una versión concreta para conservar las ediciones realizadas durante la sincronización. Tras subir los cambios locales, Firestore es la fuente de las filas sin cambios pendientes. Si dos dispositivos editan el mismo producto, prevalece la última escritura que acepta el servidor.

El borrado pendiente oculta la fila en SQLite. El trabajador elimina el documento mediante `.delete()` y confirma el borrado local tras la respuesta del servidor o al verificar que el documento ya no está. Al cerrar sesión se conserva la cola local filtrada por propietario. Se reanuda cuando vuelve a iniciar sesión esa cuenta.

Las pruebas instrumentadas del DAO se pueden ejecutar con `.\gradlew.bat connectedDebugAndroidTest` con la configuración de Firebase instalada y un dispositivo conectado.
