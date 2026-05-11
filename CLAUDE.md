# chatBack
Backend principal TejeChat. API REST + WebSocket. Gestiona usuarios, chats, mensajes E2E cifrados, grupos, llamadas, IA delegada a tejechat-ai-service.

## Stack
Java 17 · Spring Boot 3.5.4 · Maven · MySQL · Spring Security (JWT + OAuth2 Google) · Spring WebSocket · Spring Batch · Puerto 8080 · Context `/Nexo`

## Estructura
```
src/main/java/com/chat/chat/
  Batch/          schedulers: reportes, msgs programados/temporales, notificaciones, solicitudes
  Call/           señalización WebRTC (CallManager, CallSession, WS handlers)
  Configuracion/  beans config + SchemaFix (migraciones DDL en arranque)
  Controller/     16 REST + 2 WebSocket controllers
  DTO/            ~147 DTOs request/response por feature
  Entity/         25 entidades JPA
  Exceptions/     GlobalExceptionHandler + excepciones tipadas
  Mapper/         7 mappers record-based
  Repository/     25 repos JPA (JPQL custom)
  Security/       JWT, rate limiting, SQL injection filter, CORS, WS security
  Service/        ~85 services en 15 sub-paquetes
  Utils/          enums, constantes, helpers crypto E2E, AiTextMode
  WebSocketClass/ WebSocketPresenceListener
```

## Features
| Service sub-paquete | Responsabilidad |
|---|---|
| AiService/ | texto, búsqueda msgs cifrados, resumen, quick-reply, report-analysis, transcripción audio |
| MensajeriaService/ | envío/recepción msgs E2E |
| AuthService/ | login JWT + Google OAuth2 |
| ChatService/ | chats individuales y grupales |
| UploadService/ | archivos (100MB max, MIME whitelist) |
| NotificacionService/ | push notifications |
| EncuestaService/ | encuestas en mensajes |
| AdminAiReportService/ | reportes PDF con IA |
| CallService/ | señalización WebRTC |

## AiService — detalle (trabajo frecuente)
```
DeepSeekAiTextServiceImpl                    POST /api/ai/texto
DeepSeekAiEncryptedMessageSearchServiceImpl  POST /api/ai/buscar-mensajes/encrypted
DeepSeekAiEncryptedConversationSummaryServiceImpl
DeepSeekAiQuickReplyServiceImpl
DeepSeekAiReportAnalysisServiceImpl
AiEncryptedContextService       descifrado contexto para IA
AiRateLimitService              5/min·200/día user; 20/min·1000/día global
AudioTranscriptionService       faster-whisper local, 10MB, 120s timeout
AiMessageSearchMicroserviceClient  HTTP → tejechat-ai-service :8081
AdminAuditCrypto                RSA-OAEP descifrado auditoría admin
```

## Seguridad
- JWT `Authorization: Bearer <token>` (86400s)
- Rate limiting HTTP + WebSocket (InMemoryRateLimiterService)
- SQL injection detector en params + body
- E2E: chatBack cifra/descifra; tejechat-ai-service nunca ve claves

## Comandos
```bash
./mvnw clean compile       # compilar
./mvnw spring-boot:run     # dev :8080
./mvnw clean package       # JAR
./mvnw test
```
Env requeridas: `APP_JWT_SECRET`, `DEEPSEEK_API_KEY`, `GOOGLE_CLIENT_ID`. Dev: `.env`.

## Convenciones
- Controllers delegan todo a Services; sin lógica en controller
- Inyección por constructor (no @Autowired en campo)
- DTOs sin lógica, getters/setters, sin Lombok en mayoría
- Repos: JPQL nombrado; SQL nativo solo con motivo documentado
- SchemaFix: migraciones DDL en arranque — no tocar sin revisar dependencias
- Logs IA: `[AI][FEATURE] requestId=X campo=val` — formato parseado; no cambiar
- Nunca loguear texto descifrado ni keys

## Límites IA
texto=1000 · responder=20000 · summary=8000 · search=20000 · report-analysis=10000 · quick-reply=6000 · audio=10MB · quick-replies=50/user/día

## NO tocar
- `AdminAuditCrypto` — RSA-OAEP; cambio rompe descifrado histórico
- `SchemaFix` classes — DDL en arranque, orden calibrado
- `SecurityConfig` — whitelist rutas; cambios rompen auth
- `AiRateLimitService` — límites acordados; no subir sin revisar costes
- `MensajeRepository` — JPQL largas y testeadas
- Formato log `[AI][...]` — no cambiar estructura

## Contexto clave
- E2E: todo descifrado en chatBack (AdminAuditCrypto/AiEncryptedContextService); microservicio recibe texto plano
- RESPONDER: carga ≤50 msgs chat (contexto) + ≤100 msgs TEXT usuario (estilo), descifra, envía como `AiTextContextMessageDTO`
- Búsqueda semántica scopes: GLOBAL/INDIVIDUAL/GRUPO/GLOBAL_GRUPOS — fallback a GLOBAL si relevancia < 60
- tejechat-ai-service: localhost:8081, header `X-Internal-Api-Key`
- WebSocket: `/ws` — JWT en handshake vía WsClientIpHandshakeInterceptor
- Docker: `docker-compose.yml` (dev) · `docker-compose.full.yml` (MySQL + uploads)
- uploads/ en `chatBack/uploads` — no committear; volumen en Docker

## Búsqueda inteligente — POST /api/ai/buscar-mensajes/encrypted

### Pipeline
1. `validateAndResolve` → analizador determinista (regex + listas) calcula análisis base
2. Llama a `POST /internal/ai/search-intent` (tejechat) → LLM clasifica intención semántica
3. `enrichAnalysisWithIntent` overlay sobre análisis si `confidence >= 0.5`
4. Refresh `incluirGrupales/Individuales` desde flags actualizados (`withIncluir`)
5. Resolución scope + senderResolution + búsqueda directa o semántica vía IA
6. Resumen natural cifrado en `encryptedPayload` (nunca claro)

### Intent classifier — campos
- `target`: MESSAGES | COMPLAINTS_RECEIVED | COMPLAINTS_CREATED | MIXED | SCHEDULED_MESSAGES | UNREAD_MESSAGES | OFFENSIVE_CONTENT_SEARCH | APP_REPORT
- `senderScope`: AUTHENTICATED_USER | RECEIVED_MESSAGES | SPECIFIC_OTHER_USER | MULTIPLE_POSSIBLE_USERS | ANY_PARTICIPANT
- `tipoScopeSolicitado`: GLOBAL | GLOBAL_GRUPOS | GLOBAL_INDIVIDUALES | GRUPO_CONCRETO | INDIVIDUAL_CONCRETO | DESCONOCIDO
- `tipoMensajeSolicitado`: TEXT | AUDIO | IMAGE | STICKER | FILE | ANY
- `complaintDirection`: RECEIVED | CREATED | null
- `scheduledStatus`: PENDING | SENT | CANCELLED | FAILED | ANY | null
- `readStatus`: UNREAD | READ | ANY | null
- `tipoReporte`: DESBANEO | INCIDENCIA | QUEJA | MEJORA | ERROR_APP | SUGERENCIA | OTRO | null
- `motivoReporte`, `personaMencionada`, `grupoMencionado`, `motivoDenuncia`, `temporalExpression`, `orden` (LATEST/FIRST/NEXT/RELEVANCE), `confidence`

### Forks ramificados (orden tras enrich)
1. **APP_REPORT** → `crearReporteAplicacion()` → `SolicitudDesbaneoService.crearReporteDesdeAi()` → bypass búsqueda
2. **SCHEDULED_MESSAGES** → `buscarMensajesProgramados()` → `MensajeProgramadoRepository`
3. **COMPLAINTS_RECEIVED/CREATED** → `buscarDenunciasConIA()` → `UserComplaintRepository`
4. **Resolución directa** (intencionUltimoMensaje/PrimerMensaje) → DB sin IA, respeta senderScope
5. **Búsqueda semántica IA** → carga candidatos descifrados → `tejechat-ai-service`

### senderScope semántica
- `AUTHENTICATED_USER` → DB filter emisor=userId
- `RECEIVED_MESSAGES` → post-filter emisor!=userId (sin whitelist DB)
- `SPECIFIC_OTHER_USER` → whitelist `allowedEmitterIds` (resuelto contra UsuarioRepository por nombre)
- `MULTIPLE_POSSIBLE_USERS` → ambigüedad de nombres
- `ANY_PARTICIPANT` → sin filtro emisor

### `senderAllowsAuthor` — punto crítico
Filtra autor en `toRichCandidate`. Cubre TODOS los enum values; añadir nuevos requiere caso aquí o caen en `allowedEmitterIds.contains(...)` con lista vacía → rechazo silencioso.

### Reportes administrativos via IA
- Reutiliza `SolicitudDesbaneoEntity` + `ReporteTipo` enum (DESBANEO, CHAT_CERRADO, INCIDENCIA, QUEJA, MEJORA, ERROR_APP, SUGERENCIA, OTRO)
- `crearReporteDesdeAi(usuarioId, tipo, motivo)` — usuarioId siempre del JWT, email del UsuarioEntity, nunca payload front
- Bloquea `CHAT_CERRADO` (mantiene flujo dedicado)
- Aparecen en `GET /api/usuarios/admin/solicitudes-desbaneo` con tipoReporte distinto
- Endpoint `POST /api/usuarios/solicitudes-desbaneo` (público) intacto para flujo DESBANEO original

### WebSocket progress
- Canal: `/user/queue/ai-search-progress` (mismo para todos los flujos IA)
- DTO `AiSearchProgressWS`: requestId, step, status, message, timestamp, hasApproximateResult, target, tipoReporte
- Steps: ANALYZING_CONTEXT, ANALYZING_MESSAGES, MESSAGE_FOUND, MESSAGE_NOT_FOUND, APP_REPORT, ERROR
- APP_REPORT statuses: STARTED ("Generando reporte...") / COMPLETED ("Reporte generado") / FAILED ("No se pudo generar el reporte")
- `AiSearchProgressNotifier.notifyAppReport{Started,Completed,Failed}(userEmail, requestId, tipoReporte)`

### encryptedPayload
- `resumenBusqueda` SIEMPRE viaja cifrado AES-GCM dentro de `encryptedPayload` (key envuelta RSA-OAEP receptor)
- Campo claro `resumenBusqueda` en respuesta = null por diseño (línea 3258 success())
- Helper `buildEmptyResumenBusqueda(analysis, senderResolution, scope, intent)` genera resumen contextual cuando 0 resultados (no dejar null)

### tejechat-ai-service intent classifier
- `DeepSeekInternalAiSearchIntentServiceImpl` — prompt en español con reglas + 24 ejemplos
- `ai.debug-log-prompts=false` (default) — si true loggea prompt completo + raw response
- Sanitización VALID_* sets + coherencia post-parse (auto-promote scope, force readStatus para UNREAD_MESSAGES, etc.)

### Logs trazabilidad
```
[AI][SEARCH_INTENT_CLIENT] outbound consulta=... usuarioActualNombre=...
[AI][SEARCH_INTENT] classified target=... senderScope=... tipoScopeSolicitado=... tipoReporte=... readStatus=... confidence=...
[AI][MESSAGE_SEARCH_ENCRYPTED] intent-applied source=LLM
[AI][MESSAGE_SEARCH_ENCRYPTED] intent-final consulta=... target=... senderScopeAplicado=... tipoScopeAplicado=... source=LLM|DETERMINISTIC
[AI][MESSAGE_SEARCH_ENCRYPTED] directSearch=true totalCandidatosAntesFiltro=... totalCandidatosDespuesFiltro=... mensajeIdSeleccionado=...
[AI][APP_REPORT] creating=true ... created=true|false solicitudId=...
[AI][APP_REPORT_WS] status=STARTED|COMPLETED|FAILED tipoReporte=...
```

### Tests
- `DeepSeekAiEncryptedMessageSearchServiceImplResumenBusquedaTest` — resumen empty + intent enrichment
- `DeterministicAiMessageSearchNaturalQueryAnalyzerTest` — analizador regex/listas

### Puntos críticos NO romper
- `senderAllowsAuthor` cubrir cada enum value de `AiMessageSearchSenderScope`
- `ValidationValues.withIncluir` / `withTemporalRange` — record immutable, factory necesario
- LLM autoritativo si `confidence >= MIN_INTENT_CONFIDENCE` (0.5) — sobrescribe AUTHENTICATED_USER default determinista
- `resolveDirectSearch` honra `senderScope` en select + cuenta candidatos individuales/grupales
- `mapTipoReporte` bloquea CHAT_CERRADO via fork IA (mapea OTRO)
- WS canal único `/user/queue/ai-search-progress` — no abrir paralelo
