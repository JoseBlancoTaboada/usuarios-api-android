# UsuariosApi

App Android (Java) que consume la API REST de gestión de usuarios desplegada en Render:
https://users-management-microservice.onrender.com

Autor: José Eduardo Blanco Taboada · Unicolombo · código 2410180110

## Qué hace

- Iniciar sesión (`POST /api/auth/login`, token JWT de 15 minutos).
- Listar usuarios (`GET /api/users`).
- Consultar un usuario por id (`GET /api/users/{id}`), al abrir su edición.
- Crear (`POST /api/users`), actualizar (`PUT /api/users/{id}`) y eliminar (`DELETE /api/users/{id}`) con confirmación.
- No deja eliminar al usuario con la sesión abierta. Cerrar sesión vuelve al login.

## Organización

| Paquete | Contenido |
|---|---|
| `vistas` | `PantallaLogin`, `PantallaListado`, `PantallaUsuario`, `Avisos` |
| `datos` | `Usuario`, `ApiException` |
| `controladores` | `ClienteApi`, `RespuestaApi`, `ServicioUsuarios`, `SegundoPlano`, `Sesion` |

- `HttpURLConnection` + Gson; la red corre en segundo plano (`ExecutorService` + `Handler`).
- El token vive solo en memoria. Único permiso: `INTERNET`. La URL de la API está en `strings.xml`.
- Tiempo de espera de lectura de 120 s: el plan gratuito de Render tarda más de un minuto en despertar.

## Pruebas

- 30 pruebas unitarias JUnit contra un servidor falso con `ServerSocket`: `./gradlew testDebugUnitTest`.
- 27 comprobaciones de punta a punta en un emulador Android 15 contra la API real.

## Compilar

Android Studio, minSdk 26. `./gradlew assembleDebug`. El APK firmado está en la sección Releases.
