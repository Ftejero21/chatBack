package com.chat.chat.Service.AiService;

import com.chat.chat.Configuracion.AiProperties;
import com.chat.chat.DTO.AiEncryptedMessageSearchRequestDTO;
import com.chat.chat.DTO.AiEncryptedMessageSearchResponseDTO;
import com.chat.chat.DTO.AiEncryptedMessageSearchResultDTO;
import com.chat.chat.DTO.AiEncryptedResponseDTO;
import com.chat.chat.DTO.AudioTranscriptionResultDTO;
import com.chat.chat.DTO.AiMessageSearchCandidateDTO;
import com.chat.chat.DTO.AiMessageSearchInternalRequestDTO;
import com.chat.chat.DTO.AiMessageSearchInternalResponseDTO;
import com.chat.chat.DTO.AiMessageSearchInternalResultDTO;
import com.chat.chat.DTO.AiMessageSearchPersonCandidateDTO;
import com.chat.chat.Entity.ChatEntity;
import com.chat.chat.Entity.ChatGrupalEntity;
import com.chat.chat.Entity.ChatIndividualEntity;
import com.chat.chat.Entity.MensajeEntity;
import com.chat.chat.Entity.UsuarioEntity;
import com.chat.chat.DTO.AiComplaintCandidateDTO;
import com.chat.chat.DTO.AiScheduledMessageSummaryCandidateDTO;
import com.chat.chat.DTO.AiScheduledMessageSummaryInternalRequestDTO;
import com.chat.chat.DTO.AiScheduledMessageSummaryInternalResponseDTO;
import com.chat.chat.DTO.AiSearchIntentInternalRequestDTO;
import com.chat.chat.DTO.AiSearchIntentInternalResponseDTO;
import com.chat.chat.DTO.SolicitudDesbaneoDTO;
import com.chat.chat.Entity.UserComplaintEntity;
import com.chat.chat.Service.SolicitudDesbaneoService.SolicitudDesbaneoService;
import com.chat.chat.Utils.ReporteTipo;
import com.chat.chat.Entity.MensajeProgramadoEntity;
import com.chat.chat.Repository.ChatGrupalRepository;
import com.chat.chat.Repository.ChatIndividualRepository;
import com.chat.chat.Repository.MensajeRepository;
import com.chat.chat.Repository.MensajeProgramadoRepository;
import com.chat.chat.Repository.UserComplaintRepository;
import com.chat.chat.Repository.UsuarioRepository;
import com.chat.chat.Utils.AdminAuditCrypto;
import com.chat.chat.Utils.AiGlobalSearchTarget;
import com.chat.chat.Utils.Constantes;
import com.chat.chat.Utils.EstadoMensajeProgramado;
import com.chat.chat.Utils.MessageType;
import com.chat.chat.Utils.AiSearchProgressStep;
import com.chat.chat.Utils.SecurityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Validated
public class DeepSeekAiEncryptedMessageSearchServiceImpl implements AiEncryptedMessageSearchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeepSeekAiEncryptedMessageSearchServiceImpl.class);
    private static final int DEFAULT_MAX_RESULTADOS = 10;
    private static final int MAX_MAX_RESULTADOS = 20;
    private static final int DEFAULT_MAX_MENSAJES = 300;
    private static final int MAX_MAX_MENSAJES = 1000;
    private static final int MAX_OFFENSIVE_PREFILTER_CANDIDATES = 120;
    private static final int MIN_RELEVANCIA_PUBLICA = 70;
    private static final int MAX_MESSAGE_CONTENT_LENGTH = 500;
    private static final int MAX_REASON_LENGTH = 300;
    private static final String TRANSFORM_AES_GCM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final String AUDIO_TRANSCRIBED_PREFIX = "[Audio transcrito automaticamente]: ";
    private static final String SCHEDULED_CONTENT_PLACEHOLDER = "[Mensaje programado]";
    private static final double MIN_SCHEDULED_INTENT_CONFIDENCE = 0.7d;
    private static final Set<String> ALLOWED_AUDIO_MIMES = Set.of("audio/webm", "audio/mpeg", "audio/mp3", "audio/wav", "audio/ogg", "audio/mp4");
    private static final Pattern OFFENSIVE_TERM_PATTERN = Pattern.compile(
            "\\b(?:gilipoll\\w*|idiot\\w*|imbecil\\w*|estupid\\w*|subnormal\\w*|inutil\\w*|payas\\w*|capull\\w*|cabron\\w*|mierd\\w*|asqueros\\w*|retrasad\\w*|mongol\\w*|zorra\\w*|zorro\\w*|pringa\\w*|put\\w*)\\b");
    private static final Pattern OFFENSIVE_PHRASE_PATTERN = Pattern.compile(
            "\\b(?:hijo\\s+de\\s+puta|vete\\s+a\\s+la\\s+mierda|das\\s+asco|me\\s+das\\s+asco|eres\\s+basura|eres\\s+una\\s+mierda|que\\s+te\\s+jodan)\\b");
    private static final Pattern THREAT_PATTERN = Pattern.compile(
            "\\b(?:te\\s+voy\\s+a\\s+(?:matar|reventar|romper|partir)|te\\s+mato|te\\s+reviento|te\\s+rompo|te\\s+parto\\s+la\\s+cara|te\\s+pego|muerete|ojala\\s+te\\s+mueras)\\b");
    private static final Pattern ALT_PERSON_DOUBT_PATTERN = Pattern.compile("\\b([\\p{L}][\\p{L}'_-]{1,40})\\s+o\\s+([\\p{L}][\\p{L}'_-]{1,40})\\b", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern ALT_PERSON_WAS_PATTERN = Pattern.compile("\\b([\\p{L}][\\p{L}'_-]{1,40})\\b\\s+.*?\\bo\\s+era\\s+a\\s+([\\p{L}][\\p{L}'_-]{1,40})\\b", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Set<String> SUMMARY_NAME_STOPWORDS = Set.of(
            "audio", "archivo", "chat", "con", "el", "ella", "era", "es", "foto", "grupo", "imagen",
            "insultandole", "insultándole", "la", "le", "lo", "mensaje", "no", "o", "pls", "por", "si", "sticker", "texto", "un", "una", "y"
    );

    private final AiProperties aiProperties;
    private final AiRateLimitService aiRateLimitService;
    private final SecurityUtils securityUtils;
    private final ChatIndividualRepository chatIndividualRepository;
    private final ChatGrupalRepository chatGrupalRepository;
    private final MensajeRepository mensajeRepository;
    private final MensajeProgramadoRepository mensajeProgramadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final UserComplaintRepository userComplaintRepository;
    private final AiEncryptedContextService aiEncryptedContextService;
    private final AdminAuditCrypto adminAuditCrypto;
    private final AudioTranscriptionService audioTranscriptionService;
    private final AiMessageSearchNaturalQueryAnalyzer aiMessageSearchNaturalQueryAnalyzer;
    private final AiMessageSearchScopeResolverService aiMessageSearchScopeResolverService;
    private final AiMessageSearchMicroserviceClient aiMessageSearchMicroserviceClient;
    private final AiSearchIntentMicroserviceClient aiSearchIntentMicroserviceClient;
    private final AiScheduledMessageSummaryMicroserviceClient aiScheduledMessageSummaryMicroserviceClient;
    private final SolicitudDesbaneoService solicitudDesbaneoService;
    private final AiSearchProgressNotifier aiSearchProgressNotifier;
    private final ObjectMapper objectMapper;
    private final String applicationContextPath;
    private final String backendPublicRootUrl;
    private final String uploadsRoot;

    public DeepSeekAiEncryptedMessageSearchServiceImpl(AiProperties aiProperties,
                                                       AiRateLimitService aiRateLimitService,
                                                       SecurityUtils securityUtils,
                                                       ChatIndividualRepository chatIndividualRepository,
                                                       ChatGrupalRepository chatGrupalRepository,
                                                       MensajeRepository mensajeRepository,
                                                       MensajeProgramadoRepository mensajeProgramadoRepository,
                                                       UsuarioRepository usuarioRepository,
                                                       UserComplaintRepository userComplaintRepository,
                                                       AiEncryptedContextService aiEncryptedContextService,
                                                       AdminAuditCrypto adminAuditCrypto,
                                                       AudioTranscriptionService audioTranscriptionService,
                                                       AiMessageSearchNaturalQueryAnalyzer aiMessageSearchNaturalQueryAnalyzer,
                                                       AiMessageSearchScopeResolverService aiMessageSearchScopeResolverService,
                                                       AiMessageSearchMicroserviceClient aiMessageSearchMicroserviceClient,
                                                       AiSearchIntentMicroserviceClient aiSearchIntentMicroserviceClient,
                                                       AiScheduledMessageSummaryMicroserviceClient aiScheduledMessageSummaryMicroserviceClient,
                                                       SolicitudDesbaneoService solicitudDesbaneoService,
                                                       AiSearchProgressNotifier aiSearchProgressNotifier,
                                                       ObjectMapper objectMapper,
                                                       @Value(Constantes.PROP_UPLOADS_ROOT) String uploadsRoot,
                                                       @Value("${server.servlet.context-path:}") String applicationContextPath,
                                                       @Value("${app.public-base-url:}") String appPublicBaseUrl,
                                                       @Value("${server.address:localhost}") String serverAddress,
                                                       @Value("${server.port:8080}") String serverPort) {
        this.aiProperties = aiProperties;
        this.aiRateLimitService = aiRateLimitService;
        this.securityUtils = securityUtils;
        this.chatIndividualRepository = chatIndividualRepository;
        this.chatGrupalRepository = chatGrupalRepository;
        this.mensajeRepository = mensajeRepository;
        this.mensajeProgramadoRepository = mensajeProgramadoRepository;
        this.usuarioRepository = usuarioRepository;
        this.userComplaintRepository = userComplaintRepository;
        this.aiEncryptedContextService = aiEncryptedContextService;
        this.adminAuditCrypto = adminAuditCrypto;
        this.audioTranscriptionService = audioTranscriptionService;
        this.aiMessageSearchNaturalQueryAnalyzer = aiMessageSearchNaturalQueryAnalyzer;
        this.aiMessageSearchScopeResolverService = aiMessageSearchScopeResolverService;
        this.aiMessageSearchMicroserviceClient = aiMessageSearchMicroserviceClient;
        this.aiSearchIntentMicroserviceClient = aiSearchIntentMicroserviceClient;
        this.aiScheduledMessageSummaryMicroserviceClient = aiScheduledMessageSummaryMicroserviceClient;
        this.solicitudDesbaneoService = solicitudDesbaneoService;
        this.aiSearchProgressNotifier = aiSearchProgressNotifier;
        this.objectMapper = objectMapper;
        this.uploadsRoot = uploadsRoot;
        this.applicationContextPath = normalizeContextPath(applicationContextPath);
        this.backendPublicRootUrl = resolveBackendPublicRootUrl(appPublicBaseUrl, serverAddress, serverPort, this.applicationContextPath);
    }

    @Override
    public AiEncryptedMessageSearchResponseDTO buscarMensajes(AiEncryptedMessageSearchRequestDTO request) {
        Long userId = securityUtils.getAuthenticatedUserId();
        String requestId = UUID.randomUUID().toString();

        ValidationValues values = validateAndResolve(request);
        if (!values.valid()) {
            return failure("AI_MESSAGE_SEARCH_INVALID_REQUEST", values.errorMessage(), null);
        }

        String userEmail = securityUtils.getAuthenticatedUserEmail();
        aiSearchProgressNotifier.notifyStarted(userEmail, requestId, AiSearchProgressStep.ANALYZING_CONTEXT);

        AiMessageSearchNaturalQueryAnalysis analysis = values.analysis();

        // LLM-based intent classification (overlay over deterministic analysis)
        AiSearchIntentInternalResponseDTO intentResp = null;
        try {
            AiSearchIntentInternalRequestDTO intentReq = new AiSearchIntentInternalRequestDTO();
            intentReq.setRequestId(requestId);
            intentReq.setConsulta(values.consulta());
            intentReq.setUsuarioActualNombre(resolveUserDisplayName(userId));
            intentResp = aiSearchIntentMicroserviceClient.classifyIntent(requestId, intentReq);
            logIntentClassifierOutcome(requestId, values.consulta(), analysis, intentResp);
            enrichAnalysisWithIntent(requestId, analysis, intentResp);
        } catch (RuntimeException ex) {
            LOGGER.warn("[AI][MESSAGE_SEARCH_ENCRYPTED] requestId={} intent-classification-error errorClass={} fallback=deterministic",
                    requestId, ex.getClass().getSimpleName());
        }

        // Refresh incluirGrupales/incluirIndividuales after enrichment (LLM may have flipped intencionGrupo/Individual)
        boolean newIncluirGrupales = values.incluirGrupales();
        boolean newIncluirIndividuales = values.incluirIndividuales();
        boolean personTargetDetected = analysis != null
                && (hasText(analysis.getPersonaObjetivoDetectada()) || hasText(analysis.getEmisorObjetivoDetectado()));
        if (analysis != null) {
            if (analysis.isIntencionGrupo() && !analysis.isIntencionIndividual()) {
                newIncluirIndividuales = false;
                newIncluirGrupales = true;
            } else if (analysis.isIntencionIndividual() && !analysis.isIntencionGrupo() && !personTargetDetected) {
                newIncluirGrupales = false;
                newIncluirIndividuales = true;
            }
        }
        if (newIncluirGrupales != values.incluirGrupales() || newIncluirIndividuales != values.incluirIndividuales()) {
            LOGGER.info("[AI][MESSAGE_SEARCH_ENCRYPTED] requestId={} intent-scope-refresh tipoScopeSolicitado={} incluirGrupalesAntes={} incluirGrupalesDespues={} incluirIndividualesAntes={} incluirIndividualesDespues={}",
                    requestId,
                    intentResp == null ? null : intentResp.getTipoScopeSolicitado(),
                    values.incluirGrupales(), newIncluirGrupales,
                    values.incluirIndividuales(), newIncluirIndividuales);
            values = values.withIncluir(newIncluirGrupales, newIncluirIndividuales);
        }

        // Fork: APP_REPORT — create administrative report and return immediately, no semantic search
        if (intentResp != null
                && intentResp.isSuccess()
                && "APP_REPORT".equals(intentResp.getTarget())
                && intentResp.getConfidence() != null
                && intentResp.getConfidence() >= MIN_INTENT_CONFIDENCE) {
            aiSearchProgressNotifier.notifyCompleted(userEmail, requestId, AiSearchProgressStep.ANALYZING_CONTEXT);
            return crearReporteAplicacion(requestId, userId, userEmail, values.consulta(), intentResp);
        }

        if (shouldSearchScheduledMessages(intentResp, values.consulta(), analysis)) {
            aiSearchProgressNotifier.notifyCompleted(userEmail, requestId, AiSearchProgressStep.ANALYZING_CONTEXT);
            aiSearchProgressNotifier.notifyStarted(userEmail, requestId, AiSearchProgressStep.ANALYZING_MESSAGES);
            AiEncryptedMessageSearchResponseDTO scheduledResponse = buscarMensajesProgramados(
                    requestId,
                    userId,
                    userEmail,
                    values,
                    intentResp
            );
            aiSearchProgressNotifier.notifyCompleted(userEmail, requestId, AiSearchProgressStep.ANALYZING_MESSAGES);
            boolean foundScheduled = scheduledResponse != null
                    && scheduledResponse.getResultados() != null
                    && !scheduledResponse.getResultados().isEmpty();
            if (foundScheduled) {
                aiSearchProgressNotifier.notifyStarted(userEmail, requestId, AiSearchProgressStep.MESSAGE_FOUND);
                aiSearchProgressNotifier.notifyCompleted(userEmail, requestId, AiSearchProgressStep.MESSAGE_FOUND);
            } else {
                aiSearchProgressNotifier.notifyStarted(userEmail, requestId, AiSearchProgressStep.MESSAGE_NOT_FOUND);
                aiSearchProgressNotifier.notifyCompleted(userEmail, requestId, AiSearchProgressStep.MESSAGE_NOT_FOUND, false);
            }
            return scheduledResponse;
        }

        ComplaintBranch complaintBranch = resolveComplaintBranch(intentResp, analysis);
        if (complaintBranch != ComplaintBranch.NONE) {
            aiSearchProgressNotifier.notifyCompleted(userEmail, requestId, AiSearchProgressStep.ANALYZING_CONTEXT);
            AiRateLimitCheck complaintRateLimitCheck = aiRateLimitService.checkUsage(userId);
            if (!complaintRateLimitCheck.isAllowed()) {
                aiSearchProgressNotifier.notifyError(userEmail, requestId);
                return failure(complaintRateLimitCheck.getCode(), complaintRateLimitCheck.getMessage(), null);
            }
            aiRateLimitService.registrarUso(userId);
            return buscarDenunciasConIA(requestId, userId, userEmail, values, complaintBranch, intentResp);
        }

        AiMessageSearchScopeDTO scope = aiMessageSearchScopeResolverService.resolverScope(
                userId,
                values.consulta(),
                values.incluirGrupales(),
                values.incluirIndividuales(),
                analysis
        );

        SearchIntent intent = resolveSearchIntent(values.consulta(), analysis);
        boolean intencionAudioDetectada = analysis != null && analysis.isIntencionAudio();
        String requestedType = intent == null ? null : intent.requestedType();
        boolean useDirectResolution = shouldUseDirectResolution(scope, intent);
        AiMessageSearchScopeType scopeInicialType = effectiveScopeType(scope);
        String nombreScopeInicial = scope != null ? scope.getNombreScopeAplicado() : null;
        String nombreDetectadoInicial = scope != null ? scope.getNombreDetectado() : null;
        String personaSolicitada = resolvePersonaSolicitada(scope, analysis);
        String grupoSolicitado = resolveGrupoSolicitado(scope, analysis);
        String nombreScopeSolicitado = resolveNombreScopeSolicitado(scope, analysis, personaSolicitada, grupoSolicitado);
        SenderResolution senderResolution = resolveSenderResolution(userId, analysis);

        // Final intent applied (LLM if confidence ≥ MIN_INTENT_CONFIDENCE, else deterministic fallback)
        boolean intentFromLlm = intentResp != null
                && intentResp.isSuccess()
                && intentResp.getConfidence() != null
                && intentResp.getConfidence() >= MIN_INTENT_CONFIDENCE;
        LOGGER.info("[AI][MESSAGE_SEARCH_ENCRYPTED] requestId={} intent-final consulta=\"{}\" target={} senderScopeAplicado={} tipoScopeSolicitado={} tipoScopeAplicado={} nombreScopeAplicado={} incluirGrupales={} incluirIndividuales={} orden={} tipoMensajeSolicitado={} confidence={} intencionMensajesNoLeidos={} intencionContenidoOfensivo={} source={}",
                requestId,
                values.consulta(),
                intentResp == null ? null : intentResp.getTarget(),
                senderResolution.senderScope().name(),
                intentResp == null ? null : intentResp.getTipoScopeSolicitado(),
                effectiveScopeType(scope).name(),
                scope == null ? null : scope.getNombreScopeAplicado(),
                values.incluirGrupales(),
                values.incluirIndividuales(),
                intentResp == null ? null : intentResp.getOrden(),
                intentResp == null ? null : intentResp.getTipoMensajeSolicitado(),
                intentResp == null ? null : intentResp.getConfidence(),
                analysis != null && analysis.isIntencionMensajesNoLeidos(),
                analysis != null && analysis.isIntencionContenidoOfensivo(),
                intentFromLlm ? "LLM" : "DETERMINISTIC");

        LOGGER.info("[AI][MESSAGE_SEARCH_ENCRYPTED] requestId={} intentTargetRecibido={} complaintDirection={} confidence={} ramaEjecutada=MESSAGES consultaNormalizada=\"{}\" scopeAplicado={} senderScope={} tipoMensajeDetectado={} rangoTemporalDetectado={} descripcionRangoTemporal={} fechaInicioDetectada={} fechaFinDetectada={} intencionGrupoDetectada={} intencionIndividualDetectada={} intencionContenidoOfensivoDetectada={} intencionUltimoMensajeDetectada={} usaResolucionDirectaSinIA={} llamadaMicroservicioIA=false",
                requestId,
                intentResp == null ? null : intentResp.getTarget(),
                intentResp == null ? null : intentResp.getComplaintDirection(),
                intentResp == null ? null : intentResp.getConfidence(),
                values.consulta(),
                effectiveScopeType(scope).name(),
                senderResolution.senderScope().name(),
                requestedType,
                values.rangoTemporalDetectado(),
                values.descripcionRangoTemporal(),
                values.fechaInicio(),
                values.fechaFin(),
                scope != null && scope.isIntencionGrupoDetectada(),
                scope != null && scope.isIntencionIndividualDetectada(),
                analysis != null && analysis.isIntencionContenidoOfensivo(),
                intent != null && intent.directResolution(),
                useDirectResolution);

        aiSearchProgressNotifier.notifyCompleted(userEmail, requestId, AiSearchProgressStep.ANALYZING_CONTEXT);

        if (useDirectResolution) {
            aiSearchProgressNotifier.notifyStarted(userEmail, requestId, AiSearchProgressStep.ANALYZING_MESSAGES);
            AiEncryptedMessageSearchResponseDTO directResult = resolveDirectSearch(requestId, userId, values, scope, intent, senderResolution);
            aiSearchProgressNotifier.notifyCompleted(userEmail, requestId, AiSearchProgressStep.ANALYZING_MESSAGES);
            boolean directFound = directResult != null && directResult.getResultados() != null && !directResult.getResultados().isEmpty();
            if (directFound) {
                aiSearchProgressNotifier.notifyStarted(userEmail, requestId, AiSearchProgressStep.MESSAGE_FOUND);
                aiSearchProgressNotifier.notifyCompleted(userEmail, requestId, AiSearchProgressStep.MESSAGE_FOUND);
            } else {
                aiSearchProgressNotifier.notifyStarted(userEmail, requestId, AiSearchProgressStep.MESSAGE_NOT_FOUND);
                aiSearchProgressNotifier.notifyCompleted(userEmail, requestId, AiSearchProgressStep.MESSAGE_NOT_FOUND, false);
            }
            return directResult;
        }

        if (!aiProperties.isEnabled()) {
            aiSearchProgressNotifier.notifyError(userEmail, requestId);
            return failure("AI_DISABLED", "La ayuda de IA no esta habilitada.", scope);
        }
        if (!"deepseek".equalsIgnoreCase(aiProperties.getProvider())) {
            aiSearchProgressNotifier.notifyError(userEmail, requestId);
            return failure("AI_PROVIDER_NOT_SUPPORTED", "El proveedor de IA configurado no es compatible.", scope);
        }
        // Key check only required for message decryption — skip for complaint search
        AiEncryptedMessageSearchResponseDTO keyFailure = validateAdminAuditKeyForTextSearch(scope);
        if (keyFailure != null) {
            aiSearchProgressNotifier.notifyError(userEmail, requestId);
            return keyFailure;
        }

        AiRateLimitCheck rateLimitCheck = aiRateLimitService.checkUsage(userId);
        if (!rateLimitCheck.isAllowed()) {
            aiSearchProgressNotifier.notifyError(userEmail, requestId);
            return failure(rateLimitCheck.getCode(), rateLimitCheck.getMessage(), scope);
        }

        // Fork: complaint search intent → bypass message candidate loading
        aiSearchProgressNotifier.notifyStarted(userEmail, requestId, AiSearchProgressStep.ANALYZING_MESSAGES);

        List<MensajeEntity> candidatos = loadCandidatesByScope(userId, values, scope, requestedType, senderResolution, false);
        Map<Long, CandidateMessage> decryptedCandidates = buildCandidatesMap(userId, candidatos, values, scope, requestedType, intencionAudioDetectada, senderResolution);
        int totalAudiosCandidatos = countAudioCandidates(decryptedCandidates.values());
        int totalAudiosTranscritos = countAudioTranscriptions(decryptedCandidates.values());

        int totalDescifrados = decryptedCandidates.size();
        boolean fallbackSinRangoTemporal = false;
        if (decryptedCandidates.isEmpty() && values.rangoTemporalDetectado()) {
            fallbackSinRangoTemporal = true;
            values = values.withoutTemporalRange();
            candidatos = loadCandidatesByScope(userId, values, scope, requestedType, senderResolution, false);
            decryptedCandidates = buildCandidatesMap(userId, candidatos, values, scope, requestedType, intencionAudioDetectada, senderResolution);
            totalAudiosCandidatos = countAudioCandidates(decryptedCandidates.values());
            totalAudiosTranscritos = countAudioTranscriptions(decryptedCandidates.values());
            totalDescifrados = decryptedCandidates.size();
        }
        if (decryptedCandidates.isEmpty()) {
            LOGGER.info("[AI][MESSAGE_SEARCH_ENCRYPTED] requestId={} intencionAudioDetectada={} totalAudiosCandidatos={} totalAudiosTranscritos={} totalCandidatosEnviadosIA={} totalResultadosAudio=0 tipoMensajePrimerResultado=null empty-context userId={} scope={} totalMensajesRepositorio={} totalCandidatos={} totalResultadosFinales=0",
                    requestId,
                    intencionAudioDetectada,
                    totalAudiosCandidatos,
                    totalAudiosTranscritos,
                    totalDescifrados,
                    userId,
                    effectiveScopeType(scope).name(),
                    candidatos.size(),
                    totalDescifrados);
            aiSearchProgressNotifier.notifyError(userEmail, requestId);
            return failure("AI_MESSAGE_SEARCH_EMPTY_CONTEXT", "No hay mensajes descifrables para analizar.", scope);
        }

        try {
            CandidateBatch aiCandidateBatch = selectCandidatesForAi(requestId, values, decryptedCandidates, senderResolution, "primary");
            LOGGER.info("[AI][MESSAGE_SEARCH_ENCRYPTED] requestId={} intencionAudioDetectada={} totalAudiosCandidatos={} totalAudiosTranscritos={} totalCandidatosEnviadosIA={} offensivePrefilterApplied={} offensivePrefilterMatches={} request userId={} scope={} scopeResuelto={} confidenceScope={} totalMensajesRepositorio={} totalCandidatos={} llamadaMicroservicioIA=true maxResultados={} maxMensajesAnalizar={}",
                    requestId,
                    intencionAudioDetectada,
                    totalAudiosCandidatos,
                    totalAudiosTranscritos,
                    aiCandidateBatch.candidates().size(),
                    aiCandidateBatch.prefilterApplied(),
                    aiCandidateBatch.prefilterMatchCount(),
                    userId,
                    effectiveScopeType(scope).name(),
                    scope != null && scope.isScopeResuelto(),
                    scope == null ? 0 : scope.getConfidence(),
                    candidatos.size(),
                    totalDescifrados,
                    values.maxResultados(),
                    values.maxMensajesAnalizar());

            AiMessageSearchInternalResponseDTO internalResponse = aiMessageSearchMicroserviceClient.buscarMensajesConIA(
                    requestId,
                    buildInternalRequest(values, scope, aiCandidateBatch.candidates().values(), intencionAudioDetectada, fallbackSinRangoTemporal,
                            scopeInicialType, nombreScopeSolicitado, personaSolicitada, grupoSolicitado, false, userId, requestedType, senderResolution)
            );
            AiMessageSearchInternalResponseDTO winningInternalResponse = internalResponse;

            if (!isValidInternalResponse(internalResponse)) {
                LOGGER.warn("[AI][MESSAGE_SEARCH_ENCRYPTED] requestId={} invalid-service-response userId={} scope={}",
                        requestId,
                        userId,
                        effectiveScopeType(scope).name());
                aiSearchProgressNotifier.notifyError(userEmail, requestId);
                return failure("AI_MESSAGE_SEARCH_INVALID_SERVICE_RESPONSE", "La respuesta interna del servicio de IA no es valida.", scope);
            }

            if (!internalResponse.isSuccess()) {
                LOGGER.warn("[AI][MESSAGE_SEARCH_ENCRYPTED] requestId={} service-error userId={} scope={} statusRespuestaMicroservicio={} codigo={}",
                        requestId,
                        userId,
                        effectiveScopeType(scope).name(),
                        internalResponse.isSuccess(),
                        internalResponse.getCodigo());
                aiSearchProgressNotifier.notifyError(userEmail, requestId);
                return failure("AI_MESSAGE_SEARCH_SERVICE_ERROR", resolveServiceErrorMessage(internalResponse), scope);
            }

            List<AiMessageSearchInternalResultDTO> filteredInternalResults = filterInternalResultsByMinRelevancia(requestId, internalResponse.getResultados());
            List<AiEncryptedMessageSearchResultDTO> resultados = buildPublicResults(filteredInternalResults, decryptedCandidates, values.maxResultados(), scope, intencionAudioDetectada, values.consulta());
            if (shouldFallbackWithoutTemporalRange(values, resultados)) {
                fallbackSinRangoTemporal = true;
                ValidationValues sinRango = values.withoutTemporalRange();
                List<MensajeEntity> candidatosSinRango = loadCandidatesByScope(userId, sinRango, scope, requestedType, senderResolution, false);
                Map<Long, CandidateMessage> decryptedSinRango = buildCandidatesMap(userId, candidatosSinRango, sinRango, scope, requestedType, intencionAudioDetectada, senderResolution);
                if (!decryptedSinRango.isEmpty()) {
                    CandidateBatch fallbackCandidateBatch = selectCandidatesForAi(requestId, sinRango, decryptedSinRango, senderResolution, "temporal-fallback");
                    AiMessageSearchInternalResponseDTO fallbackResponse = aiMessageSearchMicroserviceClient.buscarMensajesConIA(
                            requestId,
                            buildInternalRequest(sinRango, scope, fallbackCandidateBatch.candidates().values(), intencionAudioDetectada, true,
                                    scopeInicialType, nombreScopeSolicitado, personaSolicitada, grupoSolicitado, false, userId, requestedType, senderResolution)
                    );
                    if (isValidInternalResponse(fallbackResponse) && fallbackResponse.isSuccess()) {
                        List<AiMessageSearchInternalResultDTO> filteredFallbackResults = filterInternalResultsByMinRelevancia(requestId, fallbackResponse.getResultados());
                        resultados = buildPublicResults(filteredFallbackResults, decryptedSinRango, sinRango.maxResultados(), scope, intencionAudioDetectada, sinRango.consulta());
                        decryptedCandidates = decryptedSinRango;
                        candidatos = candidatosSinRango;
                        values = sinRango;
                        totalDescifrados = decryptedCandidates.size();
                        winningInternalResponse = fallbackResponse;
                    }
                }
            }
            // Scope fallback: si el scope inicial era INDIVIDUAL/GRUPO y los resultados son insuficientes, buscar GLOBAL
            boolean fallbackScopeGlobal = false;
            int relevanciaMaximaScopeInicial = getMaxRelevancia(resultados);
            int relevanciaMaximaGlobal = 0;
            String faseGanadora = "scope-inicial";
            boolean incluyeAudios = intencionAudioDetectada || !hasText(requestedType);

            if (needsScopeFallback(scope, resultados) || needsSenderFallback(senderResolution, resultados)) {
                fallbackScopeGlobal = true;
                AiMessageSearchScopeDTO globalScope = buildGlobalFallbackScope(scope);
                List<MensajeEntity> globalCandidatos = loadCandidatesByScope(userId, values, globalScope, requestedType, senderResolution, true);
                Map<Long, CandidateMessage> globalDecrypted = buildCandidatesMap(userId, globalCandidatos, values, globalScope, requestedType, intencionAudioDetectada, senderResolution);
                ValidationValues globalValues = values;
                boolean fallbackGlobalSinRango = false;
                if (globalDecrypted.isEmpty() && globalValues.rangoTemporalDetectado()) {
                    fallbackGlobalSinRango = true;
                    globalValues = globalValues.withoutTemporalRange();
                    globalCandidatos = loadCandidatesByScope(userId, globalValues, globalScope, requestedType, senderResolution, true);
                    globalDecrypted = buildCandidatesMap(userId, globalCandidatos, globalValues, globalScope, requestedType, intencionAudioDetectada, senderResolution);
                }
                if (!globalDecrypted.isEmpty()) {
                    try {
                        CandidateBatch globalCandidateBatch = selectCandidatesForAi(requestId, globalValues, globalDecrypted, senderResolution, "scope-fallback");
                        AiMessageSearchInternalResponseDTO globalResponse = aiMessageSearchMicroserviceClient.buscarMensajesConIA(
                                requestId,
                                buildInternalRequest(globalValues, globalScope, globalCandidateBatch.candidates().values(), intencionAudioDetectada, fallbackGlobalSinRango,
                                        scopeInicialType, nombreScopeSolicitado, personaSolicitada, grupoSolicitado, true, userId, requestedType, senderResolution)
                        );
                        if (isValidInternalResponse(globalResponse) && globalResponse.isSuccess()) {
                            List<AiMessageSearchInternalResultDTO> filteredGlobalResults = filterInternalResultsByMinRelevancia(requestId, globalResponse.getResultados());
                            List<AiEncryptedMessageSearchResultDTO> globalResultados = buildPublicResults(
                                    filteredGlobalResults, globalDecrypted, globalValues.maxResultados(), globalScope, intencionAudioDetectada, globalValues.consulta()
                            );
                            relevanciaMaximaGlobal = getMaxRelevancia(globalResultados);
                            if (relevanciaMaximaGlobal > relevanciaMaximaScopeInicial && !globalResultados.isEmpty()) {
                                resultados = enrichScopeFallbackMotivo(globalResultados,
                                        hasText(nombreScopeInicial) ? nombreScopeInicial : "la persona o grupo indicado");
                                scope = globalScope;
                                decryptedCandidates = globalDecrypted;
                                candidatos = globalCandidatos;
                                totalDescifrados = globalDecrypted.size();
                                totalAudiosCandidatos = countAudioCandidates(globalDecrypted.values());
                                totalAudiosTranscritos = countAudioTranscriptions(globalDecrypted.values());
                                faseGanadora = "scope-global-fallback";
                                winningInternalResponse = globalResponse;
                            }
                        }
                    } catch (AiMessageSearchMicroserviceUnavailableException | AiMessageSearchMicroserviceException ex) {
                        LOGGER.warn("[AI][MESSAGE_SEARCH_ENCRYPTED] requestId={} scope-fallback-global-service-error userId={}", requestId, userId);
                    }
                }
            }

            aiRateLimitService.registrarUso(userId);
            long totalResultadosAudio = resultados.stream().filter(r -> "AUDIO".equals(r.getTipoMensaje())).count();

            LOGGER.info("[AI][MESSAGE_SEARCH_ENCRYPTED] requestId={} consultaNormalizada=\"{}\" scopeAplicado={} tipoMensajeDetectado={} rangoTemporalDetectado={} descripcionRangoTemporal={} fechaInicioDetectada={} fechaFinDetectada={} totalCandidatosConRango={} resultadosConRango={} fallbackSinRangoTemporal={} totalCandidatosSinRango={} resultadosFinales={} intencionAudioDetectada={} totalAudiosCandidatos={} totalAudiosTranscritos={} totalCandidatosEnviadosIA={} totalResultadosAudio={} tipoMensajePrimerResultado={} success userId={} scope={} statusRespuestaMicroservicio={} tipoChatPrimerResultado={}",
                    requestId,
                    values.consulta(),
                    effectiveScopeType(scope).name(),
                    requestedType,
                    values.rangoTemporalDetectado(),
                    values.descripcionRangoTemporal(),
                    values.fechaInicio(),
                    values.fechaFin(),
                    fallbackSinRangoTemporal ? 0 : candidatos.size(),
                    fallbackSinRangoTemporal ? 0 : resultados.size(),
                    fallbackSinRangoTemporal,
                    fallbackSinRangoTemporal ? candidatos.size() : 0,
                    resultados.size(),
                    intencionAudioDetectada,
                    totalAudiosCandidatos,
                    totalAudiosTranscritos,
                    totalDescifrados,
                    totalResultadosAudio,
                    resultados.isEmpty() ? null : resultados.get(0).getTipoMensaje(),
                    userId,
                    effectiveScopeType(scope).name(),
                    internalResponse.isSuccess(),
                    resultados.isEmpty() ? null : resultados.get(0).getTipoChat());
            LOGGER.info("[AI][MESSAGE_SEARCH_ENCRYPTED] requestId={} scopeInicial={} scopeFinal={} fallbackScopeGlobal={} tipoMensajeSolicitado={} incluyeAudios={} relevanciaMaximaScopeInicial={} relevanciaMaximaGlobal={} faseGanadora={}",
                    requestId,
                    scopeInicialType.name(),
                    effectiveScopeType(scope).name(),
                    fallbackScopeGlobal,
                    requestedType,
                    incluyeAudios,
                    relevanciaMaximaScopeInicial,
                    relevanciaMaximaGlobal,
                    faseGanadora);
            aiSearchProgressNotifier.notifyCompleted(userEmail, requestId, AiSearchProgressStep.ANALYZING_MESSAGES);
            boolean sinResultadosFuertes = Boolean.TRUE.equals(winningInternalResponse.getSinResultadosFuertes());
            if (!resultados.isEmpty() && !sinResultadosFuertes) {
                aiSearchProgressNotifier.notifyStarted(userEmail, requestId, AiSearchProgressStep.MESSAGE_FOUND);
                aiSearchProgressNotifier.notifyCompleted(userEmail, requestId, AiSearchProgressStep.MESSAGE_FOUND);
            } else {
                boolean hasApproximateResult = !resultados.isEmpty() && sinResultadosFuertes;
                aiSearchProgressNotifier.notifyStarted(userEmail, requestId, AiSearchProgressStep.MESSAGE_NOT_FOUND);
                aiSearchProgressNotifier.notifyCompleted(userEmail, requestId, AiSearchProgressStep.MESSAGE_NOT_FOUND, hasApproximateResult);
            }
            String resumenBusqueda = resolveNaturalSummary(winningInternalResponse, resultados, scope, requestedType);
            if (!hasText(resumenBusqueda) && (resultados == null || resultados.isEmpty())) {
                resumenBusqueda = buildEmptyResumenBusqueda(values.analysis(), senderResolution, scope, intent);
                LOGGER.info("[AI][MESSAGE_SEARCH_ENCRYPTED] requestId={} ia-result=empty fallback-resumen=true resumenBusqueda=\"{}\"",
                        requestId, resumenBusqueda);
            }
            return success(resultados, scope, resumenBusqueda, userId);
        } catch (AiMessageSearchMicroserviceUnavailableException ex) {
            LOGGER.warn("[AI][MESSAGE_SEARCH_ENCRYPTED] requestId={} service-unavailable userId={} scope={} llamadaMicroservicioIA=true errorClass={}",
                    requestId,
                    userId,
                    effectiveScopeType(scope).name(),
                    ex.getClass().getSimpleName());
            aiSearchProgressNotifier.notifyError(userEmail, requestId);
            return failure("AI_MESSAGE_SEARCH_SERVICE_UNAVAILABLE", "El microservicio de busqueda IA no esta disponible temporalmente.", scope);
        } catch (AiMessageSearchMicroserviceException ex) {
            LOGGER.warn("[AI][MESSAGE_SEARCH_ENCRYPTED] requestId={} service-runtime-error userId={} scope={} llamadaMicroservicioIA=true errorClass={}",
                    requestId,
                    userId,
                    effectiveScopeType(scope).name(),
                    ex.getClass().getSimpleName());
            aiSearchProgressNotifier.notifyError(userEmail, requestId);
            return failure("AI_MESSAGE_SEARCH_SERVICE_ERROR", "El microservicio de busqueda IA devolvio un error.", scope);
        } catch (RuntimeException ex) {
            LOGGER.warn("[AI][MESSAGE_SEARCH_ENCRYPTED] requestId={} runtime-error userId={} scope={} errorClass={}",
                    requestId,
                    userId,
                    effectiveScopeType(scope).name(),
                    ex.getClass().getSimpleName());
            aiSearchProgressNotifier.notifyError(userEmail, requestId);
            return failure("AI_MESSAGE_SEARCH_RUNTIME_ERROR", "No se pudo completar la busqueda de mensajes cifrados.", scope);
        }
    }

    private AiEncryptedMessageSearchResponseDTO resolveDirectSearch(String requestId,
                                                                    Long userId,
                                                                    ValidationValues values,
                                                                    AiMessageSearchScopeDTO scope,
                                                                    SearchIntent intent,
                                                                    SenderResolution senderResolution) {
        AiMessageSearchSenderScope senderScope = senderResolution == null
                ? AiMessageSearchSenderScope.AUTHENTICATED_USER
                : senderResolution.senderScope();
        boolean filtroExcluirUsuarioActual = senderScope == AiMessageSearchSenderScope.RECEIVED_MESSAGES;
        List<Long> emisorIds = senderResolution == null ? List.of() : senderResolution.allowedEmitterIds();

        LOGGER.info("[AI][MESSAGE_SEARCH_ENCRYPTED] requestId={} directSearch=true orden={} senderScopeAplicado={} userId={} emisorIds={} filtroExcluirUsuarioActual={} tipoScopeAplicado={} requestedType={}",
                requestId,
                intent == null ? null : intent.direction(),
                senderScope.name(),
                userId,
                emisorIds,
                filtroExcluirUsuarioActual,
                effectiveScopeType(scope).name(),
                intent == null ? null : intent.requestedType());

        List<MensajeEntity> mensajes = loadOrderedCandidatesByScope(userId, values, scope, intent.direction(), senderResolution, false);
        int totalAntes = mensajes == null ? 0 : mensajes.size();
        CandidateMessage selected = selectDirectCandidate(requestId, userId, mensajes, values.incluirIndividuales(), values.incluirGrupales(), intent, scope, senderResolution);
        boolean fallbackSinRango = false;
        if (selected == null && values.rangoTemporalDetectado()) {
            fallbackSinRango = true;
            ValidationValues withoutRange = values.withoutTemporalRange();
            mensajes = loadOrderedCandidatesByScope(userId, withoutRange, scope, intent.direction(), senderResolution, false);
            totalAntes = mensajes == null ? 0 : mensajes.size();
            selected = selectDirectCandidate(requestId, userId, mensajes, withoutRange.incluirIndividuales(), withoutRange.incluirGrupales(), intent, scope, senderResolution);
        }

        // Count post-filter candidates for log clarity
        int totalDespues = 0;
        int totalCandidatosIndividuales = 0;
        int totalCandidatosGrupales = 0;
        if (mensajes != null && !mensajes.isEmpty()) {
            for (MensajeEntity m : mensajes) {
                Long eid = m != null && m.getEmisor() != null ? m.getEmisor().getId() : null;
                if (filtroExcluirUsuarioActual && userId != null && userId.equals(eid)) continue;
                if (senderScope == AiMessageSearchSenderScope.AUTHENTICATED_USER
                        && userId != null && eid != null && !userId.equals(eid)) continue;
                if (!emisorIds.isEmpty() && (eid == null || !emisorIds.contains(eid))) continue;
                totalDespues++;
                if (m.getChat() instanceof ChatGrupalEntity) totalCandidatosGrupales++;
                else if (m.getChat() instanceof ChatIndividualEntity) totalCandidatosIndividuales++;
            }
            LOGGER.info("[AI][MESSAGE_SEARCH_ENCRYPTED] requestId={} directSearch=true totalCandidatosIndividuales={} totalCandidatosGrupales={} totalCandidatosDespuesFiltro={}",
                    requestId, totalCandidatosIndividuales, totalCandidatosGrupales, totalDespues);
        }

        if (selected == null) {
            String emptyResumen = buildEmptyResumenBusqueda(values.analysis(), senderResolution, scope, intent);
            LOGGER.info("[AI][MESSAGE_SEARCH_ENCRYPTED] requestId={} directSearch=true result=empty senderScopeAplicado={} totalCandidatosAntesFiltro={} totalCandidatosDespuesFiltro={} fallbackSinRango={} resumenBusqueda=\"{}\"",
                    requestId, senderScope.name(), totalAntes, totalDespues, fallbackSinRango, emptyResumen);
            return success(List.of(), scope, emptyResumen, userId);
        }

        if ("TEXT".equals(selected.tipoMensaje())) {
            AiEncryptedMessageSearchResponseDTO keyFailure = validateAdminAuditKeyForTextSearch(scope);
            if (keyFailure != null) {
                return keyFailure;
            }
            CandidateMessage textRich = toRichCandidate(userId, selected.mensajeEntity(), values.incluirIndividuales(), values.incluirGrupales(), true, false, senderResolution);
            if (textRich == null) {
                String emptyResumen = buildEmptyResumenBusqueda(values.analysis(), senderResolution, scope, intent);
                LOGGER.warn("[AI][MESSAGE_SEARCH_ENCRYPTED] requestId={} directSearch=true errorBuildResult=text-rich-null mensajeId={} emisorId={} resumenBusqueda=\"{}\"",
                        requestId, selected.mensajeId(), selected.emisorId(), emptyResumen);
                return success(List.of(), scope, emptyResumen, userId);
            }
            selected = textRich;
        }

        AiEncryptedMessageSearchResultDTO result;
        try {
            result = toPublicResult(selected, buildDirectReason(selected, scope, intent), 100);
        } catch (RuntimeException ex) {
            String emptyResumen = buildEmptyResumenBusqueda(values.analysis(), senderResolution, scope, intent);
            LOGGER.warn("[AI][MESSAGE_SEARCH_ENCRYPTED] requestId={} directSearch=true errorBuildResult={} mensajeId={} emisorId={} errorClass={} resumenBusqueda=\"{}\"",
                    requestId, ex.getMessage(), selected.mensajeId(), selected.emisorId(), ex.getClass().getSimpleName(), emptyResumen);
            return success(List.of(), scope, emptyResumen, userId);
        }
        LOGGER.info("[AI][MESSAGE_SEARCH_ENCRYPTED] requestId={} directSearch=true result=ok userId={} senderScopeAplicado={} tipoScopeAplicado={} totalCandidatosAntesFiltro={} totalCandidatosDespuesFiltro={} mensajeIdSeleccionado={} emisorIdSeleccionado={} tipoChatPrimerResultado={} usaResolucionDirectaSinIA=true llamadaMicroservicioIA=false",
                requestId,
                userId,
                senderScope.name(),
                effectiveScopeType(scope).name(),
                totalAntes,
                totalDespues,
                selected.mensajeId(),
                selected.emisorId(),
                result.getTipoChat());
        String resumenBusqueda = buildMinimalDirectSummary(result, intent == null ? null : intent.requestedType(), scope);
        return success(List.of(result), scope, resumenBusqueda, userId);
    }

    private AiEncryptedMessageSearchResponseDTO validateAdminAuditKeyForTextSearch(AiMessageSearchScopeDTO scope) {
        if (!adminAuditCrypto.hasPrivateKeyConfigured()) {
            return failure("AI_ADMIN_PRIVATE_KEY_MISSING", "No esta configurada la clave privada de auditoria para buscar mensajes cifrados.", scope);
        }
        if (!adminAuditCrypto.hasMatchingPrivateKeyForAuditPublicKey()) {
            return failure("AI_ADMIN_PRIVATE_KEY_MISMATCH", "La clave privada de auditoria configurada no corresponde a la audit public key actual.", scope);
        }
        return null;
    }

    private boolean shouldSearchScheduledMessages(AiSearchIntentInternalResponseDTO intent,
                                                  String consulta,
                                                  AiMessageSearchNaturalQueryAnalysis analysis) {
        if (isHighConfidenceScheduledIntent(intent)) {
            return true;
        }
        if (analysis != null && analysis.isIntencionDenuncia()) {
            return false;
        }
        if (intent != null
                && intent.isSuccess()
                && hasText(intent.getTarget())
                && !AiGlobalSearchTarget.MESSAGES.name().equalsIgnoreCase(intent.getTarget())
                && !AiGlobalSearchTarget.SCHEDULED_MESSAGES.name().equalsIgnoreCase(intent.getTarget())) {
            return false;
        }
        return matchesScheduledFallback(consulta);
    }

    private void logIntentClassifierOutcome(String requestId,
                                            String consulta,
                                            AiMessageSearchNaturalQueryAnalysis analysis,
                                            AiSearchIntentInternalResponseDTO intent) {
        LOGGER.info("[AI][MESSAGE_SEARCH_ENCRYPTED] requestId={} intent-outcome consulta=\"{}\" deterministicRecibida={} deterministicCreada={} responseNull={} success={} codigo={} target={} complaintDirection={} senderScope={} personaMencionada=\"{}\" grupoMencionado=\"{}\" temporalExpression=\"{}\" orden={} confidence={}",
                requestId,
                safeForLog(consulta),
                analysis != null && analysis.isIntencionDenunciaRecibida(),
                analysis != null && analysis.isIntencionDenunciaCreada(),
                intent == null,
                intent != null && intent.isSuccess(),
                intent == null ? null : intent.getCodigo(),
                intent == null ? null : intent.getTarget(),
                intent == null ? null : intent.getComplaintDirection(),
                intent == null ? null : intent.getSenderScope(),
                safeForLog(intent == null ? null : intent.getPersonaMencionada()),
                safeForLog(intent == null ? null : intent.getGrupoMencionado()),
                safeForLog(intent == null ? null : intent.getTemporalExpression()),
                intent == null ? null : intent.getOrden(),
                intent == null ? null : intent.getConfidence());
    }

    private ComplaintBranch resolveComplaintBranch(AiSearchIntentInternalResponseDTO intent,
                                                   AiMessageSearchNaturalQueryAnalysis analysis) {
        if (intent != null
                && intent.isSuccess()
                && intent.getConfidence() != null
                && intent.getConfidence() >= MIN_INTENT_CONFIDENCE) {
            if (AiGlobalSearchTarget.COMPLAINTS_RECEIVED.name().equalsIgnoreCase(intent.getTarget())
                    || "RECEIVED".equalsIgnoreCase(intent.getComplaintDirection())) {
                return ComplaintBranch.RECEIVED;
            }
            if (AiGlobalSearchTarget.COMPLAINTS_CREATED.name().equalsIgnoreCase(intent.getTarget())
                    || "CREATED".equalsIgnoreCase(intent.getComplaintDirection())) {
                return ComplaintBranch.CREATED;
            }
        }
        if (analysis != null) {
            if (analysis.isIntencionDenunciaRecibida()) {
                return ComplaintBranch.RECEIVED;
            }
            if (analysis.isIntencionDenunciaCreada()) {
                return ComplaintBranch.CREATED;
            }
        }
        return ComplaintBranch.NONE;
    }

    private boolean isHighConfidenceScheduledIntent(AiSearchIntentInternalResponseDTO intent) {
        return intent != null
                && intent.isSuccess()
                && AiGlobalSearchTarget.SCHEDULED_MESSAGES.name().equalsIgnoreCase(intent.getTarget())
                && intent.getConfidence() != null
                && intent.getConfidence() >= MIN_SCHEDULED_INTENT_CONFIDENCE;
    }

    private boolean matchesScheduledFallback(String consulta) {
        String normalized = normalizeIntentText(consulta);
        if (!hasText(normalized)) {
            return false;
        }
        boolean hasSendContext = normalized.contains("enviar")
                || normalized.contains("mandar")
                || normalized.contains("mensaje")
                || normalized.contains("envio");
        if (normalized.contains("program") && hasSendContext) {
            return true;
        }
        if ((normalized.contains("pendient") || normalized.contains("cola")) && hasSendContext) {
            return true;
        }
        return normalized.contains("mas tarde")
                && (normalized.contains("deje") || normalized.contains("prepare") || hasSendContext);
    }

    private AiEncryptedMessageSearchResponseDTO crearReporteAplicacion(String requestId,
                                                                       Long userId,
                                                                       String userEmail,
                                                                       String consulta,
                                                                       AiSearchIntentInternalResponseDTO intent) {
        AiRateLimitCheck rateLimitCheck = aiRateLimitService.checkUsage(userId);
        if (!rateLimitCheck.isAllowed()) {
            aiSearchProgressNotifier.notifyError(userEmail, requestId);
            return failure(rateLimitCheck.getCode(), rateLimitCheck.getMessage(), null);
        }

        ReporteTipo tipo = mapTipoReporte(intent.getTipoReporte());
        String motivoIa = hasText(intent.getMotivoReporte()) ? intent.getMotivoReporte() : consulta;
        String motivo = motivoIa == null ? null : motivoIa.trim();
        if (motivo != null && motivo.length() > 1000) {
            motivo = motivo.substring(0, 1000);
        }

        LOGGER.info("[AI][APP_REPORT] requestId={} creating=true userId={} tipoReporte={} motivoLength={}",
                requestId, userId, tipo, motivo == null ? 0 : motivo.length());
        LOGGER.info("[AI][APP_REPORT_WS] requestId={} userId={} status=STARTED tipoReporte={}",
                requestId, userId, tipo);
        aiSearchProgressNotifier.notifyAppReportStarted(userEmail, requestId, tipo.name());

        try {
            aiRateLimitService.registrarUso(userId);
            SolicitudDesbaneoDTO created = solicitudDesbaneoService.crearReporteDesdeAi(userId, tipo, motivo);
            LOGGER.info("[AI][APP_REPORT] requestId={} created=true solicitudId={} tipoReporte={} estado=PENDIENTE",
                    requestId, created == null ? null : created.getId(), tipo);
            LOGGER.info("[AI][APP_REPORT_WS] requestId={} userId={} status=COMPLETED tipoReporte={} solicitudId={}",
                    requestId, userId, tipo, created == null ? null : created.getId());
            aiSearchProgressNotifier.notifyAppReportCompleted(userEmail, requestId, tipo.name());
            String resumen = buildAppReportResumen(tipo);
            AiEncryptedMessageSearchResponseDTO response = success(List.of(), null, resumen, userId);
            response.setCodigo("APP_REPORT_CREATED");
            response.setMensaje("Reporte enviado correctamente al administrador.");
            return response;
        } catch (RuntimeException ex) {
            LOGGER.warn("[AI][APP_REPORT] requestId={} created=false userId={} tipoReporte={} error={} errorClass={}",
                    requestId, userId, tipo, ex.getMessage(), ex.getClass().getSimpleName());
            LOGGER.warn("[AI][APP_REPORT_WS] requestId={} userId={} status=FAILED tipoReporte={} error={}",
                    requestId, userId, tipo, ex.getClass().getSimpleName());
            aiSearchProgressNotifier.notifyAppReportFailed(userEmail, requestId, tipo.name());
            return failure("AI_APP_REPORT_ERROR", "No se pudo registrar el reporte.", null);
        }
    }

    private ReporteTipo mapTipoReporte(String raw) {
        if (raw == null || raw.isBlank()) return ReporteTipo.OTRO;
        try {
            ReporteTipo tipo = ReporteTipo.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            // CHAT_CERRADO must come from chat-closed flow, not AI fork
            if (tipo == ReporteTipo.CHAT_CERRADO) return ReporteTipo.OTRO;
            return tipo;
        } catch (IllegalArgumentException ex) {
            return ReporteTipo.OTRO;
        }
    }

    private String buildAppReportResumen(ReporteTipo tipo) {
        return switch (tipo) {
            case QUEJA -> "He enviado tu queja al administrador para que pueda revisarla.";
            case INCIDENCIA -> "He registrado tu incidencia y la he enviado al administrador para revisión.";
            case ERROR_APP -> "He registrado el error que reportas y lo he enviado al administrador.";
            case MEJORA -> "He enviado tu sugerencia de mejora al administrador.";
            case SUGERENCIA -> "He enviado tu sugerencia al administrador.";
            case DESBANEO -> "He enviado tu solicitud de desbaneo al administrador.";
            default -> "He enviado tu reporte al administrador para que lo revise.";
        };
    }

    private AiEncryptedMessageSearchResponseDTO buscarMensajesProgramados(String requestId,
                                                                          Long userId,
                                                                          String userEmail,
                                                                          ValidationValues values,
                                                                          AiSearchIntentInternalResponseDTO intent) {
        AiRateLimitCheck rateLimitCheck = aiRateLimitService.checkUsage(userId);
        if (!rateLimitCheck.isAllowed()) {
            aiSearchProgressNotifier.notifyError(userEmail, requestId);
            return failure(rateLimitCheck.getCode(), rateLimitCheck.getMessage(), null);
        }

        ScheduledQueryContext queryContext = resolveScheduledQueryContext(values, intent);
        List<MensajeProgramadoEntity> rows = mensajeProgramadoRepository.findByCreatedByIdOrderByScheduledAtAscIdAsc(userId);
        Map<Long, ScheduledChatContext> chatContexts = loadScheduledChatContexts(userId);
        List<MensajeProgramadoEntity> filtered = filterScheduledRows(rows, chatContexts, queryContext, values);

        boolean huboFallbackTemporal = false;
        ValidationValues effectiveValues = values;
        if (filtered.isEmpty() && values.rangoTemporalDetectado()) {
            huboFallbackTemporal = true;
            effectiveValues = values.withoutTemporalRange();
            filtered = filterScheduledRows(rows, chatContexts, queryContext, effectiveValues);
        }

        List<MensajeProgramadoEntity> ordered = orderScheduledRows(filtered, queryContext);
        int limit = Math.min(values.maxResultados(), ordered.size());
        List<MensajeProgramadoEntity> selected = ordered.subList(0, limit);

        List<AiEncryptedMessageSearchResultDTO> resultados = new ArrayList<>();
        List<AiScheduledMessageSummaryCandidateDTO> summaryCandidates = new ArrayList<>();
        for (MensajeProgramadoEntity row : selected) {
            ScheduledChatContext chatContext = chatContexts.get(resolveScheduledChatId(row));
            AiEncryptedMessageSearchResultDTO result = toScheduledPublicResult(row, chatContext, queryContext, huboFallbackTemporal);
            if (result != null) {
                resultados.add(result);
                summaryCandidates.add(toScheduledSummaryCandidate(row, chatContext, result));
            }
        }

        String resumenBusqueda = generarResumenMensajesProgramados(
                requestId,
                userId,
                values.consulta(),
                queryContext,
                huboFallbackTemporal,
                summaryCandidates
        );

        LOGGER.info("[AI][MESSAGE_SEARCH_ENCRYPTED] requestId={} target={} userId={} scheduledStatus={} orden={} persona={} grupo={} rangoTemporalDetectado={} huboFallbackTemporal={} totalProgramados={} totalResultados={}",
                requestId,
                AiGlobalSearchTarget.SCHEDULED_MESSAGES.name(),
                userId,
                queryContext.scheduledStatus(),
                queryContext.orden(),
                queryContext.personaMencionada(),
                queryContext.grupoMencionado(),
                values.rangoTemporalDetectado(),
                huboFallbackTemporal,
                rows.size(),
                resultados.size());

        aiRateLimitService.registrarUso(userId);
        return success(resultados, null, resumenBusqueda, userId);
    }

    private ScheduledQueryContext resolveScheduledQueryContext(ValidationValues values,
                                                               AiSearchIntentInternalResponseDTO intent) {
        AiMessageSearchNaturalQueryAnalysis analysis = values == null ? null : values.analysis();
        String persona = firstNonBlank(
                intent == null ? null : intent.getPersonaMencionada(),
                analysis == null ? null : analysis.getPersonaObjetivoDetectada(),
                analysis == null ? null : analysis.getEmisorObjetivoDetectado(),
                analysis == null ? null : analysis.getNombrePersonaDetectado()
        );
        String grupo = firstNonBlank(
                intent == null ? null : intent.getGrupoMencionado(),
                analysis == null ? null : analysis.getNombreGrupoDetectado()
        );
        String scheduledStatus = resolveRequestedScheduledStatus(intent);
        String orden = resolveScheduledOrder(intent, scheduledStatus);
        String temporalExpression = firstNonBlank(
                intent == null ? null : intent.getTemporalExpression(),
                values == null ? null : values.descripcionRangoTemporal()
        );
        return new ScheduledQueryContext(persona, grupo, temporalExpression, scheduledStatus, orden);
    }

    private String resolveRequestedScheduledStatus(AiSearchIntentInternalResponseDTO intent) {
        if (intent == null || !hasText(intent.getScheduledStatus())) {
            return null;
        }
        String status = intent.getScheduledStatus().trim().toUpperCase(Locale.ROOT);
        if ("CANCELED".equals(status)) {
            return "CANCELLED";
        }
        return status;
    }

    private String resolveScheduledOrder(AiSearchIntentInternalResponseDTO intent, String scheduledStatus) {
        String orden = intent == null ? null : intent.getOrden();
        if (hasText(orden)) {
            return orden.trim().toUpperCase(Locale.ROOT);
        }
        if ("PENDING".equals(scheduledStatus)) {
            return "NEXT";
        }
        return "LATEST";
    }

    private Map<Long, ScheduledChatContext> loadScheduledChatContexts(Long userId) {
        Map<Long, ScheduledChatContext> contexts = new LinkedHashMap<>();
        for (ChatGrupalEntity group : chatGrupalRepository.findAllByUsuariosId(userId)) {
            if (group == null || group.getId() == null || !group.isActivo()) {
                continue;
            }
            contexts.put(group.getId(), new ScheduledChatContext(
                    group.getId(),
                    "GRUPAL",
                    group.getNombreGrupo(),
                    normalizeIntentText(group.getNombreGrupo())
            ));
        }
        for (ChatIndividualEntity chat : chatIndividualRepository.findAllByUsuario1IdOrUsuario2Id(userId, userId)) {
            if (chat == null || chat.getId() == null) {
                continue;
            }
            UsuarioEntity other = resolveOtherParticipant(chat, userId);
            String otherName = resolveDisplayName(other);
            contexts.put(chat.getId(), new ScheduledChatContext(
                    chat.getId(),
                    "INDIVIDUAL",
                    otherName,
                    normalizeIntentText(otherName)
            ));
        }
        return contexts;
    }

    private UsuarioEntity resolveOtherParticipant(ChatIndividualEntity chat, Long userId) {
        if (chat == null || userId == null) {
            return null;
        }
        if (chat.getUsuario1() != null && userId.equals(chat.getUsuario1().getId())) {
            return chat.getUsuario2();
        }
        if (chat.getUsuario2() != null && userId.equals(chat.getUsuario2().getId())) {
            return chat.getUsuario1();
        }
        return chat.getUsuario2() != null ? chat.getUsuario2() : chat.getUsuario1();
    }

    private String resolveDisplayName(UsuarioEntity user) {
        if (user == null) {
            return null;
        }
        String nombre = normalizeInput(user.getNombre());
        String apellido = normalizeInput(user.getApellido());
        String fullName = firstNonBlank(
                hasText(nombre) && hasText(apellido) ? nombre + " " + apellido : null,
                nombre,
                user.getEmail()
        );
        return hasText(fullName) ? fullName : null;
    }

    private List<MensajeProgramadoEntity> filterScheduledRows(List<MensajeProgramadoEntity> rows,
                                                              Map<Long, ScheduledChatContext> chatContexts,
                                                              ScheduledQueryContext queryContext,
                                                              ValidationValues values) {
        List<MensajeProgramadoEntity> filtered = new ArrayList<>();
        if (rows == null || rows.isEmpty()) {
            return filtered;
        }
        for (MensajeProgramadoEntity row : rows) {
            if (row == null) {
                continue;
            }
            Long chatId = resolveScheduledChatId(row);
            ScheduledChatContext chatContext = chatId == null ? null : chatContexts.get(chatId);
            if (chatContext == null) {
                continue;
            }
            if (!matchesScheduledStatus(row, queryContext.scheduledStatus())) {
                continue;
            }
            if (!matchesScheduledChatContext(chatContext, queryContext)) {
                continue;
            }
            if (!matchesScheduledTemporalRange(row, values)) {
                continue;
            }
            filtered.add(row);
        }
        return filtered;
    }

    private boolean matchesScheduledStatus(MensajeProgramadoEntity row, String scheduledStatus) {
        if (!hasText(scheduledStatus) || "ANY".equalsIgnoreCase(scheduledStatus)) {
            return true;
        }
        EstadoMensajeProgramado status = row == null ? null : row.getStatus();
        if ("PENDING".equalsIgnoreCase(scheduledStatus)) {
            return status == EstadoMensajeProgramado.PENDING || status == EstadoMensajeProgramado.PROCESSING;
        }
        if ("SENT".equalsIgnoreCase(scheduledStatus)) {
            return status == EstadoMensajeProgramado.SENT;
        }
        if ("FAILED".equalsIgnoreCase(scheduledStatus)) {
            return status == EstadoMensajeProgramado.FAILED;
        }
        if ("CANCELLED".equalsIgnoreCase(scheduledStatus)) {
            return status == EstadoMensajeProgramado.CANCELED;
        }
        return true;
    }

    private boolean matchesScheduledChatContext(ScheduledChatContext chatContext,
                                                ScheduledQueryContext queryContext) {
        if (chatContext == null) {
            return false;
        }
        String normalizedChatName = chatContext.normalizedName();
        if (hasText(queryContext.personaMencionada())) {
            if (!"INDIVIDUAL".equalsIgnoreCase(chatContext.tipoChat())
                    || !normalizedChatName.contains(normalizeIntentText(queryContext.personaMencionada()))) {
                return false;
            }
        }
        if (hasText(queryContext.grupoMencionado())) {
            if (!"GRUPAL".equalsIgnoreCase(chatContext.tipoChat())
                    || !normalizedChatName.contains(normalizeIntentText(queryContext.grupoMencionado()))) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesScheduledTemporalRange(MensajeProgramadoEntity row, ValidationValues values) {
        if (row == null || row.getScheduledAt() == null || values == null || !values.rangoTemporalDetectado()) {
            return true;
        }
        LocalDateTime scheduledAtLocal = toLocalDateTime(row.getScheduledAt());
        if (values.fechaInicio() != null && scheduledAtLocal.isBefore(values.fechaInicio())) {
            return false;
        }
        return values.fechaFin() == null || !scheduledAtLocal.isAfter(values.fechaFin());
    }

    private List<MensajeProgramadoEntity> orderScheduledRows(List<MensajeProgramadoEntity> rows,
                                                             ScheduledQueryContext queryContext) {
        List<MensajeProgramadoEntity> ordered = new ArrayList<>(rows == null ? List.of() : rows);
        Comparator<MensajeProgramadoEntity> comparator = Comparator
                .comparing(MensajeProgramadoEntity::getScheduledAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(MensajeProgramadoEntity::getId, Comparator.nullsLast(Comparator.naturalOrder()));
        String orden = queryContext == null ? null : queryContext.orden();
        if ("LATEST".equalsIgnoreCase(orden)) {
            comparator = comparator.reversed();
        } else if (!"NEXT".equalsIgnoreCase(orden) && !"FIRST".equalsIgnoreCase(orden)) {
            comparator = "PENDING".equalsIgnoreCase(queryContext == null ? null : queryContext.scheduledStatus())
                    ? comparator
                    : comparator.reversed();
        }
        ordered.sort(comparator);
        return ordered;
    }

    private AiEncryptedMessageSearchResultDTO toScheduledPublicResult(MensajeProgramadoEntity row,
                                                                      ScheduledChatContext chatContext,
                                                                      ScheduledQueryContext queryContext,
                                                                      boolean huboFallbackTemporal) {
        if (row == null || chatContext == null) {
            return null;
        }
        AiEncryptedMessageSearchResultDTO result = new AiEncryptedMessageSearchResultDTO();
        result.setMensajeId(row.getId());
        result.setChatId(chatContext.chatId());
        result.setTipoChat(chatContext.tipoChat());
        result.setFechaEnvio(formatInstant(row.getScheduledAt()));
        result.setTipoResultado("SCHEDULED_MESSAGE");
        result.setTipoMensaje("TEXT");
        result.setDescripcionTipoMensaje("Mensaje programado");
        result.setContenidoVisible(SCHEDULED_CONTENT_PLACEHOLDER);
        result.setContenido(null);
        result.setMotivoCoincidencia(buildScheduledReason(queryContext, chatContext, row, huboFallbackTemporal));
        result.setRelevancia(calculateScheduledRelevance(queryContext, huboFallbackTemporal));
        if ("GRUPAL".equalsIgnoreCase(chatContext.tipoChat())) {
            result.setNombreChatGrupal(chatContext.nombreChat());
        } else {
            result.setNombreReceptor(chatContext.nombreChat());
        }
        return result;
    }

    private AiScheduledMessageSummaryCandidateDTO toScheduledSummaryCandidate(MensajeProgramadoEntity row,
                                                                              ScheduledChatContext chatContext,
                                                                              AiEncryptedMessageSearchResultDTO result) {
        AiScheduledMessageSummaryCandidateDTO candidate = new AiScheduledMessageSummaryCandidateDTO();
        candidate.setScheduledMessageId(row.getId());
        candidate.setTipoChat(chatContext.tipoChat());
        candidate.setNombreChat(resolveScheduledChatLabel(chatContext));
        candidate.setScheduledAt(formatInstant(row.getScheduledAt()));
        candidate.setScheduledStatus(normalizeScheduledStatus(row.getStatus()));
        candidate.setContenidoVisible(result.getContenidoVisible());
        candidate.setMotivoCoincidencia(result.getMotivoCoincidencia());
        return candidate;
    }

    private String generarResumenMensajesProgramados(String requestId,
                                                     Long userId,
                                                     String consulta,
                                                     ScheduledQueryContext queryContext,
                                                     boolean huboFallbackTemporal,
                                                     List<AiScheduledMessageSummaryCandidateDTO> resultados) {
        String resumenFallback = buildScheduledFallbackSummary(queryContext, huboFallbackTemporal, resultados);
        try {
            AiScheduledMessageSummaryInternalRequestDTO request = new AiScheduledMessageSummaryInternalRequestDTO();
            request.setConsultaOriginal(consulta);
            request.setUsuarioActualNombre(resolveUserDisplayName(userId));
            request.setTemporalExpression(queryContext == null ? null : queryContext.temporalExpression());
            request.setPersonaMencionada(queryContext == null ? null : queryContext.personaMencionada());
            request.setGrupoMencionado(queryContext == null ? null : queryContext.grupoMencionado());
            request.setScheduledStatus(queryContext == null ? null : queryContext.scheduledStatus());
            request.setOrden(queryContext == null ? null : queryContext.orden());
            request.setHuboFallbackTemporal(huboFallbackTemporal);
            request.setResultados(resultados);
            AiScheduledMessageSummaryInternalResponseDTO response =
                    aiScheduledMessageSummaryMicroserviceClient.resumirMensajesProgramados(requestId, request);
            if (response != null && response.isSuccess() && hasText(response.getResumenBusquedaNatural())) {
                return response.getResumenBusquedaNatural();
            }
        } catch (RuntimeException ex) {
            LOGGER.warn("[AI][MESSAGE_SEARCH_ENCRYPTED] requestId={} scheduled-summary-error errorClass={}",
                    requestId, ex.getClass().getSimpleName());
        }
        return resumenFallback;
    }

    private String buildScheduledFallbackSummary(ScheduledQueryContext queryContext,
                                                 boolean huboFallbackTemporal,
                                                 List<AiScheduledMessageSummaryCandidateDTO> resultados) {
        if (resultados == null || resultados.isEmpty()) {
            if (hasText(queryContext == null ? null : queryContext.temporalExpression())) {
                return huboFallbackTemporal
                        ? "No vi nada en ese rango exacto, y tampoco encuentro mensajes programados relacionados al ampliar la busqueda."
                        : "No veo nada preparado para " + queryContext.temporalExpression() + ".";
            }
            return "No veo mensajes programados con esos criterios.";
        }
        AiScheduledMessageSummaryCandidateDTO primero = resultados.get(0);
        String status = normalizeInput(primero.getScheduledStatus());
        String nombreChat = normalizeInput(primero.getNombreChat());
        String base = switch (status) {
            case "SENT" -> "Tienes un mensaje programado que ya se envio";
            case "FAILED" -> "Encontr\u00e9 un envio programado que fallo";
            case "CANCELLED" -> "Encontr\u00e9 un mensaje programado cancelado";
            default -> "Tienes un mensaje pendiente de envio";
        };
        if (hasText(nombreChat)) {
            return base + " en " + nombreChat + ".";
        }
        return base + ".";
    }

    private String buildScheduledReason(ScheduledQueryContext queryContext,
                                        ScheduledChatContext chatContext,
                                        MensajeProgramadoEntity row,
                                        boolean huboFallbackTemporal) {
        List<String> motivos = new ArrayList<>();
        if (hasText(queryContext == null ? null : queryContext.personaMencionada())) {
            motivos.add("coincide con la persona " + queryContext.personaMencionada());
        }
        if (hasText(queryContext == null ? null : queryContext.grupoMencionado())) {
            motivos.add("coincide con el grupo " + queryContext.grupoMencionado());
        }
        if (hasText(queryContext == null ? null : queryContext.temporalExpression()) && !huboFallbackTemporal) {
            motivos.add("entra en " + queryContext.temporalExpression());
        }
        if (hasText(queryContext == null ? null : queryContext.scheduledStatus())
                && !"ANY".equalsIgnoreCase(queryContext.scheduledStatus())) {
            motivos.add("estado " + queryContext.scheduledStatus());
        }
        if (motivos.isEmpty()) {
            motivos.add("programado en " + resolveScheduledChatLabel(chatContext));
        }
        if (huboFallbackTemporal) {
            motivos.add("se amplio la busqueda fuera del rango temporal inicial");
        }
        return String.join("; ", motivos);
    }

    private int calculateScheduledRelevance(ScheduledQueryContext queryContext, boolean huboFallbackTemporal) {
        int relevancia = 88;
        if (hasText(queryContext == null ? null : queryContext.personaMencionada())
                || hasText(queryContext == null ? null : queryContext.grupoMencionado())) {
            relevancia += 7;
        }
        if (hasText(queryContext == null ? null : queryContext.temporalExpression()) && !huboFallbackTemporal) {
            relevancia += 3;
        }
        return Math.min(100, relevancia);
    }

    private String normalizeScheduledStatus(EstadoMensajeProgramado status) {
        if (status == null) {
            return "PENDING";
        }
        return switch (status) {
            case SENT -> "SENT";
            case FAILED -> "FAILED";
            case CANCELED -> "CANCELLED";
            default -> "PENDING";
        };
    }

    private String resolveScheduledChatLabel(ScheduledChatContext chatContext) {
        if (chatContext == null) {
            return "otro chat";
        }
        if ("GRUPAL".equalsIgnoreCase(chatContext.tipoChat())) {
            return "el grupo " + chatContext.nombreChat();
        }
        return "el chat con " + chatContext.nombreChat();
    }

    private Long resolveScheduledChatId(MensajeProgramadoEntity row) {
        return row == null || row.getChat() == null ? null : row.getChat().getId();
    }

    private String formatInstant(Instant instant) {
        return instant == null ? null : toLocalDateTime(instant).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private List<MensajeEntity> loadCandidatesByScope(Long userId,
                                                      ValidationValues values,
                                                      AiMessageSearchScopeDTO scope,
                                                      String requestedType,
                                                      SenderResolution senderResolution,
                                                      boolean fallbackGlobal) {
        if (!"TEXT".equals(requestedType)) {
            return loadOrderedCandidatesByScope(userId, values, scope, SearchDirection.LAST, senderResolution, fallbackGlobal);
        }
        return loadTextCandidatesByScope(userId, values, scope, senderResolution, fallbackGlobal);
    }

    private List<MensajeEntity> loadTextCandidatesByScope(Long userId,
                                                          ValidationValues values,
                                                          AiMessageSearchScopeDTO scope,
                                                          SenderResolution senderResolution,
                                                          boolean fallbackGlobal) {
        SearchWindow searchWindow = buildSearchWindow(userId, values, scope, senderResolution, fallbackGlobal);
        if (searchWindow.chatIds().isEmpty()) {
            return List.of();
        }
        PageRequest page = PageRequest.of(0, values.maxMensajesAnalizar());
        return mensajeRepository.buscarMensajesParaAiEnChatsYEmisores(
                searchWindow.chatIds(),
                searchWindow.filtrarEmisores(),
                searchWindow.emisorIds(),
                MessageType.TEXT,
                values.fechaInicio(),
                values.fechaFin(),
                page
        ).getContent();
    }

    private List<MensajeEntity> loadOrderedCandidatesByScope(Long userId,
                                                             ValidationValues values,
                                                             AiMessageSearchScopeDTO scope,
                                                             SearchDirection direction,
                                                             SenderResolution senderResolution,
                                                             boolean fallbackGlobal) {
        SearchWindow searchWindow = buildSearchWindow(userId, values, scope, senderResolution, fallbackGlobal);
        if (searchWindow.chatIds().isEmpty()) {
            return List.of();
        }
        PageRequest page = PageRequest.of(0, values.maxMensajesAnalizar());

        if (direction == SearchDirection.FIRST) {
            return mensajeRepository.buscarPrimerosMensajesParaAiEnChatsYEmisoresSinFiltroTipo(
                    searchWindow.chatIds(),
                    searchWindow.filtrarEmisores(),
                    searchWindow.emisorIds(),
                    values.fechaInicio(),
                    values.fechaFin(),
                    page
            ).getContent();
        }
        return mensajeRepository.buscarMensajesParaAiEnChatsYEmisoresSinFiltroTipo(
                searchWindow.chatIds(),
                searchWindow.filtrarEmisores(),
                searchWindow.emisorIds(),
                values.fechaInicio(),
                values.fechaFin(),
                page
        ).getContent();
    }

    private ValidationValues validateAndResolve(AiEncryptedMessageSearchRequestDTO request) {
        if (request == null || !hasText(request.getConsulta())) {
            return ValidationValues.invalid("consulta es obligatoria");
        }

        int maxResultados = request.getMaxResultados() == null ? DEFAULT_MAX_RESULTADOS : request.getMaxResultados();
        if (maxResultados < 1 || maxResultados > MAX_MAX_RESULTADOS) {
            return ValidationValues.invalid("maxResultados debe estar entre 1 y 20");
        }

        int maxMensajesAnalizar = request.getMaxMensajesAnalizar() == null ? DEFAULT_MAX_MENSAJES : request.getMaxMensajesAnalizar();
        if (maxMensajesAnalizar < 1 || maxMensajesAnalizar > MAX_MAX_MENSAJES) {
            return ValidationValues.invalid("maxMensajesAnalizar debe estar entre 1 y 1000");
        }

        AiMessageSearchNaturalQueryAnalysis analysis = aiMessageSearchNaturalQueryAnalyzer.analyze(request.getConsulta());
        String consultaNormalizada = hasText(analysis.getConsultaNormalizada())
                ? analysis.getConsultaNormalizada()
                : normalizeInput(request.getConsulta());

        LocalDateTime fechaInicio = request.getFechaInicio() != null ? request.getFechaInicio() : analysis.getFechaInicioDetectada();
        LocalDateTime fechaFin = request.getFechaFin() != null ? request.getFechaFin() : analysis.getFechaFinDetectada();
        if (fechaInicio != null && fechaFin != null && fechaInicio.isAfter(fechaFin)) {
            return ValidationValues.invalid("fechaInicio debe ser menor o igual a fechaFin");
        }

        boolean incluirGrupales = request.getIncluirGrupales() == null || request.getIncluirGrupales();
        boolean incluirIndividuales = request.getIncluirIndividuales() == null || request.getIncluirIndividuales();
        boolean personTargetDetected = hasText(analysis.getPersonaObjetivoDetectada()) || hasText(analysis.getEmisorObjetivoDetectado());
        if (isGlobalOffensiveContentSearch(analysis, personTargetDetected)) {
            maxMensajesAnalizar = MAX_MAX_MENSAJES;
        }
        if (analysis.isIntencionGrupo() && !analysis.isIntencionIndividual()) {
            incluirIndividuales = false;
        } else if (analysis.isIntencionIndividual() && !analysis.isIntencionGrupo() && !personTargetDetected) {
            incluirGrupales = false;
        }
        if (!incluirGrupales && !incluirIndividuales) {
            return ValidationValues.invalid("incluirGrupales e incluirIndividuales no pueden ser ambos false");
        }

        return ValidationValues.valid(
                consultaNormalizada,
                maxResultados,
                maxMensajesAnalizar,
                fechaInicio,
                fechaFin,
                incluirGrupales,
                incluirIndividuales,
                analysis,
                fechaInicio != null || fechaFin != null,
                request.getFechaInicio() != null || request.getFechaFin() != null
                        ? "rango temporal indicado por request"
                        : analysis.getDescripcionRangoTemporal(),
                request.getFechaInicio() != null || request.getFechaFin() != null
                        ? 100
                        : (analysis.getConfidenceTemporal() != null ? analysis.getConfidenceTemporal() : 0)
        );
    }

    private CandidateMessage selectDirectCandidate(Long userId,
                                                   List<MensajeEntity> mensajes,
                                                   boolean incluirIndividuales,
                                                   boolean incluirGrupales,
                                                   SearchIntent intent,
                                                   AiMessageSearchScopeDTO scope,
                                                   SenderResolution senderResolution) {
        return selectDirectCandidate(null, userId, mensajes, incluirIndividuales, incluirGrupales, intent, scope, senderResolution);
    }

    private CandidateMessage selectDirectCandidate(String requestId,
                                                   Long userId,
                                                   List<MensajeEntity> mensajes,
                                                   boolean incluirIndividuales,
                                                   boolean incluirGrupales,
                                                   SearchIntent intent,
                                                   AiMessageSearchScopeDTO scope,
                                                   SenderResolution senderResolution) {
        if (mensajes == null || mensajes.isEmpty()) {
            return null;
        }
        AiMessageSearchSenderScope senderScope = senderResolution == null
                ? AiMessageSearchSenderScope.AUTHENTICATED_USER
                : senderResolution.senderScope();
        boolean excludeAuthUser = senderScope == AiMessageSearchSenderScope.RECEIVED_MESSAGES;
        boolean assertAuthUser = senderScope == AiMessageSearchSenderScope.AUTHENTICATED_USER;
        Set<Long> allowedEmitters = senderScope == AiMessageSearchSenderScope.SPECIFIC_OTHER_USER
                || senderScope == AiMessageSearchSenderScope.MULTIPLE_POSSIBLE_USERS
                ? new java.util.HashSet<>(senderResolution.allowedEmitterIds())
                : java.util.Collections.emptySet();

        boolean firstLogged = false;
        int idxConsidered = 0;
        for (MensajeEntity mensaje : mensajes) {
            Long emisorId = mensaje != null && mensaje.getEmisor() != null ? mensaje.getEmisor().getId() : null;
            if (excludeAuthUser && userId != null && userId.equals(emisorId)) {
                continue;
            }
            if (assertAuthUser && userId != null && emisorId != null && !userId.equals(emisorId)) {
                continue;
            }
            if (!allowedEmitters.isEmpty() && (emisorId == null || !allowedEmitters.contains(emisorId))) {
                continue;
            }
            idxConsidered++;
            if (!firstLogged && requestId != null) {
                LOGGER.info("[AI][MESSAGE_SEARCH_ENCRYPTED] requestId={} directSearch=true primerCandidatoId={} primerCandidatoFecha={} primerCandidatoEmisorId={} primerCandidatoTipo={}",
                        requestId, mensaje.getId(), mensaje.getFechaEnvio(), emisorId,
                        mensaje.getTipo() == null ? null : mensaje.getTipo().name());
                firstLogged = true;
            }
            CandidateMessage candidate = toRichCandidate(userId, mensaje, incluirIndividuales, incluirGrupales, false, false, senderResolution);
            if (candidate == null) {
                if (requestId != null && idxConsidered <= 5) {
                    LOGGER.info("[AI][MESSAGE_SEARCH_ENCRYPTED] requestId={} directSearch=true motivoDescartado=toRichCandidate-null mensajeId={} emisorId={}",
                            requestId, mensaje.getId(), emisorId);
                }
                continue;
            }
            if (!matchesScopeRestriction(scope, candidate)) {
                if (requestId != null && idxConsidered <= 5) {
                    LOGGER.info("[AI][MESSAGE_SEARCH_ENCRYPTED] requestId={} directSearch=true motivoDescartado=scopeRestriction mensajeId={} emisorId={} tipoMensaje={}",
                            requestId, mensaje.getId(), emisorId, candidate.tipoMensaje());
                }
                continue;
            }
            if (intent.requestedType() != null && !intent.requestedType().equals(candidate.tipoMensaje())) {
                if (requestId != null && idxConsidered <= 5) {
                    LOGGER.info("[AI][MESSAGE_SEARCH_ENCRYPTED] requestId={} directSearch=true motivoDescartado=tipoMismatch mensajeId={} requestedType={} tipoMensaje={}",
                            requestId, mensaje.getId(), intent.requestedType(), candidate.tipoMensaje());
                }
                continue;
            }
            if (requestId != null) {
                LOGGER.info("[AI][MESSAGE_SEARCH_ENCRYPTED] requestId={} directSearch=true candidatoSeleccionadoId={} emisorId={} tipoMensaje={} fechaEnvio={}",
                        requestId, candidate.mensajeId(), candidate.emisorId(), candidate.tipoMensaje(), candidate.fechaEnvio());
            }
            return candidate;
        }
        if (requestId != null) {
            LOGGER.info("[AI][MESSAGE_SEARCH_ENCRYPTED] requestId={} directSearch=true result=no-match-after-filter totalConsiderados={} senderScope={}",
                    requestId, idxConsidered, senderScope.name());
        }
        return null;
    }

    private CandidateMessage toSemanticCandidate(Long userId,
                                                 MensajeEntity mensaje,
                                                 boolean incluirIndividuales,
                                                 boolean incluirGrupales,
                                                 AiMessageSearchScopeDTO scope,
                                                 String requestedType,
                                                 boolean intencionAudio,
                                                 SenderResolution senderResolution) {
        boolean textOnly = "TEXT".equals(requestedType);
        boolean transcribeAudio = intencionAudio || !hasText(requestedType);
        CandidateMessage candidate = toRichCandidate(userId, mensaje, incluirIndividuales, incluirGrupales, textOnly, transcribeAudio, senderResolution);
        if (candidate == null || !matchesScopeRestriction(scope, candidate)) {
            return null;
        }
        if (hasText(requestedType) && !requestedType.equals(candidate.tipoMensaje())) {
            return null;
        }
        return candidate;
    }

    private Map<Long, CandidateMessage> buildCandidatesMap(Long userId,
                                                           List<MensajeEntity> candidatos,
                                                           ValidationValues values,
                                                           AiMessageSearchScopeDTO scope,
                                                           String requestedType,
                                                           boolean intencionAudio,
                                                           SenderResolution senderResolution) {
        Map<Long, CandidateMessage> decryptedCandidates = new LinkedHashMap<>();
        if (candidatos == null || candidatos.isEmpty()) {
            return decryptedCandidates;
        }
        boolean excludeAuthUserAsEmisor = senderResolution != null
                && senderResolution.senderScope() == AiMessageSearchSenderScope.RECEIVED_MESSAGES;
        for (MensajeEntity mensaje : candidatos) {
            // RECEIVED_MESSAGES: drop messages emitted by authenticated user
            if (excludeAuthUserAsEmisor && mensaje != null && mensaje.getEmisor() != null
                    && userId != null && userId.equals(mensaje.getEmisor().getId())) {
                continue;
            }
            CandidateMessage candidate = toSemanticCandidate(
                    userId,
                    mensaje,
                    values.incluirIndividuales(),
                    values.incluirGrupales(),
                    scope,
                    requestedType,
                    intencionAudio,
                    senderResolution
            );
            if (candidate != null) {
                decryptedCandidates.put(candidate.mensajeId(), candidate);
            }
        }
        return decryptedCandidates;
    }

    private CandidateBatch selectCandidatesForAi(String requestId,
                                                 ValidationValues values,
                                                 Map<Long, CandidateMessage> decryptedCandidates,
                                                 SenderResolution senderResolution,
                                                 String phase) {
        if (decryptedCandidates == null || decryptedCandidates.isEmpty()) {
            return new CandidateBatch(Map.of(), false, 0);
        }
        if (!isGlobalOffensiveContentSearch(values, senderResolution)) {
            return new CandidateBatch(decryptedCandidates, false, 0);
        }

        Map<Long, CandidateMessage> filtered = prefilterOffensiveCandidates(decryptedCandidates);
        if (filtered.isEmpty()) {
            LOGGER.info("[AI][MESSAGE_SEARCH_ENCRYPTED] requestId={} ramaEjecutada=MESSAGES offensivePrefilterApplied=true offensivePrefilterMatches=0 phase={} fallbackSemanticNormal=true",
                    requestId, phase);
            return new CandidateBatch(decryptedCandidates, true, 0);
        }

        LOGGER.info("[AI][MESSAGE_SEARCH_ENCRYPTED] requestId={} ramaEjecutada=MESSAGES offensivePrefilterApplied=true offensivePrefilterMatches={} offensiveCandidatesSent={} phase={} fallbackSemanticNormal=false",
                requestId, filtered.size(), filtered.size(), phase);
        return new CandidateBatch(filtered, true, filtered.size());
    }

    private Map<Long, CandidateMessage> prefilterOffensiveCandidates(Map<Long, CandidateMessage> decryptedCandidates) {
        List<CandidateMessage> sorted = new ArrayList<>(decryptedCandidates.values());
        sorted.sort(Comparator.comparing(CandidateMessage::fechaEnvio,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(CandidateMessage::mensajeId, Comparator.reverseOrder()));

        Map<Long, CandidateMessage> filtered = new LinkedHashMap<>();
        for (CandidateMessage candidate : sorted) {
            if (!matchesOffensiveContentPrefilter(candidate)) {
                continue;
            }
            filtered.put(candidate.mensajeId(), candidate);
            if (filtered.size() >= MAX_OFFENSIVE_PREFILTER_CANDIDATES) {
                break;
            }
        }
        return filtered;
    }

    private boolean matchesOffensiveContentPrefilter(CandidateMessage candidate) {
        if (candidate == null) {
            return false;
        }
        String normalizedContent = normalizeIntentText(firstNonBlank(candidate.contenido(), candidate.contenidoVisible()));
        if (!hasText(normalizedContent)) {
            return false;
        }
        return THREAT_PATTERN.matcher(normalizedContent).find()
                || OFFENSIVE_PHRASE_PATTERN.matcher(normalizedContent).find()
                || OFFENSIVE_TERM_PATTERN.matcher(normalizedContent).find();
    }

    private int countAudioCandidates(Collection<CandidateMessage> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (CandidateMessage candidate : candidates) {
            if (candidate != null && "AUDIO".equals(candidate.tipoMensaje())) {
                total++;
            }
        }
        return total;
    }

    private int countAudioTranscriptions(Collection<CandidateMessage> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (CandidateMessage candidate : candidates) {
            if (candidate != null && "AUDIO".equals(candidate.tipoMensaje()) && candidate.audioTranscrito()) {
                total++;
            }
        }
        return total;
    }

    private CandidateMessage toRichCandidate(Long userId,
                                             MensajeEntity mensaje,
                                             boolean incluirIndividuales,
                                             boolean incluirGrupales,
                                             boolean textOnly,
                                             boolean transcribeAudioForSearch,
                                             SenderResolution senderResolution) {
        if (mensaje == null || mensaje.getId() == null || mensaje.getChat() == null) {
            return null;
        }

        Long emisorId = mensaje.getEmisor() == null ? null : mensaje.getEmisor().getId();
        if (emisorId == null || !senderAllowsAuthor(userId, senderResolution, emisorId)) {
            return null;
        }

        ChatScopeMetadata chatScopeMetadata = resolveChatScopeMetadata(mensaje, userId, incluirIndividuales, incluirGrupales);
        if (chatScopeMetadata == null) {
            return null;
        }

        String tipoMensaje = resolverTipoMensaje(mensaje);
        if (textOnly && !"TEXT".equals(tipoMensaje)) {
            return null;
        }

        String decrypted = null;
        String responseContent = null;
        if ("TEXT".equals(tipoMensaje)) {
            decrypted = normalizeInput(aiEncryptedContextService.decryptMessagePayload(mensaje.getContenido()));
            if (!hasText(decrypted) || isNonTextContent(decrypted)) {
                return null;
            }
            decrypted = truncate(decrypted, MAX_MESSAGE_CONTENT_LENGTH);
            responseContent = decrypted;
        } else if ("AUDIO".equals(tipoMensaje) && transcribeAudioForSearch) {
            AudioSearchContent audioSearchContent = resolveAudioSearchContent(mensaje);
            if (audioSearchContent == null || !audioSearchContent.transcrito() || !hasText(audioSearchContent.contenido())) {
                return null;
            }
            responseContent = truncate(audioSearchContent.contenido(), MAX_MESSAGE_CONTENT_LENGTH);
        } else if (textOnly) {
            return null;
        } else if ("IMAGE".equals(tipoMensaje) || "STICKER".equals(tipoMensaje) || "FILE".equals(tipoMensaje)) {
            responseContent = resolverContenidoVisible(tipoMensaje, null);
        }

        MediaMeta mediaMeta = extractMediaMeta(mensaje);
        ImageCompatibilityMeta imageCompatibilityMeta = extractImageCompatibilityMeta(mensaje, tipoMensaje, mediaMeta);
        return new CandidateMessage(
                mensaje.getId(),
                chatScopeMetadata.chatId(),
                chatScopeMetadata.tipoChat(),
                emisorId,
                displayName(mensaje.getEmisor()),
                chatScopeMetadata.receptorId(),
                chatScopeMetadata.nombreReceptor(),
                chatScopeMetadata.chatGrupalId(),
                chatScopeMetadata.nombreChatGrupal(),
                mensaje.getFechaEnvio(),
                responseContent,
                resolverContenidoVisible(tipoMensaje, decrypted),
                chatScopeMetadata.chatNombre(),
                tipoMensaje,
                resolverDescripcionTipoMensaje(tipoMensaje),
                !"TEXT".equals(tipoMensaje),
                mediaMeta.mediaUrl(),
                mediaMeta.mimeType(),
                mediaMeta.nombreArchivo(),
                imageCompatibilityMeta.imageUrl(),
                imageCompatibilityMeta.imageMime(),
                imageCompatibilityMeta.imageNombre(),
                imageCompatibilityMeta.stickerId(),
                imageCompatibilityMeta.contentKind(),
                "AUDIO".equals(tipoMensaje) && transcribeAudioForSearch,
                mensaje
        );
    }

    private ChatScopeMetadata resolveChatScopeMetadata(MensajeEntity mensaje,
                                                       Long userId,
                                                       boolean incluirIndividuales,
                                                       boolean incluirGrupales) {
        ChatEntity chat = mensaje.getChat();
        if (chat instanceof ChatIndividualEntity ci) {
            if (!incluirIndividuales || ci.isAdminDirect()) {
                return null;
            }
            UsuarioEntity other = resolveOtherUser(ci, userId);
            Long receptorId = other == null ? null : other.getId();
            String nombreReceptor = other == null ? null : displayName(other);
            String chatNombre = hasText(nombreReceptor) ? nombreReceptor : "Chat individual";
            return new ChatScopeMetadata(chat.getId(), Constantes.CHAT_TIPO_INDIVIDUAL, receptorId, nombreReceptor, null, null, chatNombre);
        }
        if (chat instanceof ChatGrupalEntity cg) {
            if (!incluirGrupales || !cg.isActivo()) {
                return null;
            }
            String nombreChatGrupal = truncate(normalizeInput(cg.getNombreGrupo()), 120);
            String chatNombre = hasText(nombreChatGrupal) ? nombreChatGrupal : "Grupo";
            return new ChatScopeMetadata(chat.getId(), Constantes.CHAT_TIPO_GRUPAL, null, null, cg.getId(), nombreChatGrupal, chatNombre);
        }
        return null;
    }

    private UsuarioEntity resolveOtherUser(ChatIndividualEntity chat, Long userId) {
        if (chat == null || userId == null) {
            return null;
        }
        UsuarioEntity u1 = chat.getUsuario1();
        UsuarioEntity u2 = chat.getUsuario2();
        if (u1 != null && userId.equals(u1.getId())) {
            return u2;
        }
        if (u2 != null && userId.equals(u2.getId())) {
            return u1;
        }
        return null;
    }

    private AiMessageSearchInternalRequestDTO buildInternalRequest(ValidationValues values,
                                                                  AiMessageSearchScopeDTO scope,
                                                                  Collection<CandidateMessage> candidates,
                                                                  boolean intencionAudio,
                                                                  boolean fallbackSinRangoTemporal,
                                                                  AiMessageSearchScopeType scopeInicialType,
                                                                  String nombreScopeSolicitado,
                                                                  String personaSolicitada,
                                                                  String grupoSolicitado,
                                                                  boolean huboFallbackScope,
                                                                  Long userId,
                                                                  String requestedType,
                                                                  SenderResolution senderResolution) {
        AiMessageSearchInternalRequestDTO request = new AiMessageSearchInternalRequestDTO();
        request.setConsulta(values.consulta());
        request.setConsultaOriginal(values.consulta());
        request.setUsuarioActualNombre(resolveUserDisplayName(userId));
        request.setMaxResultados(values.maxResultados());
        request.setSenderScope(senderResolution.senderScope().name());
        request.setSearchTarget(isGlobalOffensiveContentSearch(values, senderResolution)
                ? "OFFENSIVE_CONTENT_SEARCH"
                : "MESSAGES");

        AiMessageSearchScopeType type = scope == null || scope.getTipoScope() == null ? AiMessageSearchScopeType.GLOBAL : scope.getTipoScope();
        request.setTipoScopeInicial(scopeInicialType == null ? AiMessageSearchScopeType.GLOBAL.name() : scopeInicialType.name());
        request.setTipoScopeAplicado(type.name());
        request.setNombreScopeSolicitado(nombreScopeSolicitado);
        request.setNombreScopeAplicado(scope == null ? null : scope.getNombreScopeAplicado());
        request.setPersonaSolicitada(personaSolicitada);
        request.setGrupoSolicitado(grupoSolicitado);
        request.setPersonaObjetivoNombre(senderResolution.personaObjetivoNombre());
        request.setEmisorObjetivoNombre(senderResolution.emisorObjetivoNombre());
        request.setScopeResuelto(scope != null && scope.isScopeResuelto());
        request.setHuboFallbackScope(huboFallbackScope);
        request.setBusquedaEnMensajesDeOtroUsuario(senderResolution.busquedaEnMensajesDeOtroUsuario());
        request.setIntencionAudio(intencionAudio);
        request.setIntencionLocalizacion(values.analysis() != null && values.analysis().isIntencionLocalizacion());
        request.setTipoMensajeSolicitado(requestedType);
        request.setRangoTemporalAplicado(values.rangoTemporalDetectado());
        request.setHuboFallbackTemporal(fallbackSinRangoTemporal);
        request.setDescripcionRangoTemporal(values.descripcionRangoTemporal());
        request.setFallbackSinRangoTemporal(fallbackSinRangoTemporal);
        request.setPersonasCandidatas(toPersonCandidateDtos(senderResolution.relatedUsers()));

        List<AiMessageSearchCandidateDTO> candidatos = new ArrayList<>();
        for (CandidateMessage candidate : candidates) {
            AiMessageSearchCandidateDTO dto = new AiMessageSearchCandidateDTO();
            dto.setMensajeId(candidate.mensajeId());
            dto.setChatId(candidate.chatId());
            dto.setAutorId(candidate.emisorId());
            dto.setTipoChat(candidate.tipoChat());
            dto.setNombreChat(candidate.chatNombre());
            dto.setAutor(candidate.nombreEmisor());
            dto.setFechaEnvio(formatDate(candidate.fechaEnvio()));
            dto.setTipoMensaje(candidate.tipoMensaje());
            dto.setEsMultimedia(candidate.esMultimedia());
            dto.setContenidoVisible(candidate.contenidoVisible());
            dto.setContenido(candidate.contenido());
            candidatos.add(dto);
        }
        request.setCandidatos(candidatos);
        return request;
    }

    private List<AiMessageSearchPersonCandidateDTO> toPersonCandidateDtos(List<RelatedUserCandidate> relatedUsers) {
        if (relatedUsers == null || relatedUsers.isEmpty()) {
            return List.of();
        }
        List<AiMessageSearchPersonCandidateDTO> out = new ArrayList<>();
        for (RelatedUserCandidate candidate : relatedUsers) {
            if (candidate == null || candidate.userId() == null || !hasText(candidate.fullName())) {
                continue;
            }
            AiMessageSearchPersonCandidateDTO dto = new AiMessageSearchPersonCandidateDTO();
            dto.setId(candidate.userId());
            dto.setNombreCompleto(candidate.fullName());
            out.add(dto);
        }
        return out;
    }

    private SenderResolution resolveSenderResolution(Long userId, AiMessageSearchNaturalQueryAnalysis analysis) {
        if (userId == null || analysis == null) {
            return SenderResolution.authenticatedUser(null);
        }

        AiMessageSearchSenderScope detectedScope = analysis.getSenderScope() == null
                ? AiMessageSearchSenderScope.AUTHENTICATED_USER
                : analysis.getSenderScope();
        if (detectedScope == AiMessageSearchSenderScope.ANY_PARTICIPANT) {
            return SenderResolution.anyParticipant();
        }
        if (detectedScope == AiMessageSearchSenderScope.RECEIVED_MESSAGES) {
            return SenderResolution.receivedMessages();
        }

        String rawTarget = detectedScope == AiMessageSearchSenderScope.AUTHENTICATED_USER
                ? analysis.getPersonaObjetivoDetectada()
                : analysis.getEmisorObjetivoDetectado();
        if (!hasText(rawTarget)) {
            return SenderResolution.authenticatedUser(null);
        }

        List<RelatedUserCandidate> matches = resolveRelatedUserCandidates(userId, rawTarget);
        if (matches.isEmpty()) {
            return detectedScope == AiMessageSearchSenderScope.AUTHENTICATED_USER
                    ? SenderResolution.authenticatedUser(rawTarget)
                    : SenderResolution.specificOther(rawTarget, List.of(), false);
        }
        if (matches.size() == 1) {
            RelatedUserCandidate match = matches.get(0);
            return detectedScope == AiMessageSearchSenderScope.AUTHENTICATED_USER
                    ? SenderResolution.authenticatedUser(match.fullName(), List.of(match))
                    : SenderResolution.specificOther(match.fullName(), List.of(match), true);
        }
        if (detectedScope == AiMessageSearchSenderScope.AUTHENTICATED_USER) {
            return SenderResolution.authenticatedUser(rawTarget, matches);
        }
        return SenderResolution.multiplePossibleUsers(rawTarget, matches);
    }

    private boolean isGlobalOffensiveContentSearch(ValidationValues values,
                                                   SenderResolution senderResolution) {
        if (values == null || senderResolution == null) {
            return false;
        }
        boolean personTargetDetected = hasText(senderResolution.personaObjetivoNombre())
                || hasText(senderResolution.emisorObjetivoNombre());
        return isGlobalOffensiveContentSearch(values.analysis(), personTargetDetected)
                && senderResolution.senderScope() == AiMessageSearchSenderScope.AUTHENTICATED_USER
                && !senderResolution.busquedaEnMensajesDeOtroUsuario();
    }

    private boolean isGlobalOffensiveContentSearch(AiMessageSearchNaturalQueryAnalysis analysis,
                                                   boolean personTargetDetected) {
        if (analysis == null || !analysis.isIntencionContenidoOfensivo()) {
            return false;
        }
        if (personTargetDetected || hasText(analysis.getNombreGrupoDetectado())) {
            return false;
        }
        AiMessageSearchSenderScope senderScope = analysis.getSenderScope();
        return senderScope == null || senderScope == AiMessageSearchSenderScope.AUTHENTICATED_USER;
    }

    private List<RelatedUserCandidate> resolveRelatedUserCandidates(Long userId, String rawName) {
        if (userId == null || !hasText(rawName)) {
            return List.of();
        }
        String normalizedTarget = normalizeSummaryName(rawName);
        if (!hasText(normalizedTarget)) {
            return List.of();
        }

        Map<Long, RelatedUserCandidate> byUserId = new LinkedHashMap<>();
        List<ChatIndividualEntity> individualChats = chatIndividualRepository.findAllByUsuario1IdOrUsuario2Id(userId, userId);
        if (individualChats != null) {
            for (ChatIndividualEntity chat : individualChats) {
                if (chat == null || chat.isAdminDirect()) {
                    continue;
                }
                UsuarioEntity other = resolveOtherUser(chat, userId);
                if (other == null || other.getId() == null || !other.isActivo()) {
                    continue;
                }
                RelatedUserCandidate existing = byUserId.get(other.getId());
                if (existing == null) {
                    existing = new RelatedUserCandidate(other.getId(), displayName(other), scoreRelatedUserCandidate(normalizedTarget, other), chat.getId(), new LinkedHashSet<>());
                    byUserId.put(other.getId(), existing);
                } else if (existing.individualChatId() == null) {
                    existing = new RelatedUserCandidate(existing.userId(), existing.fullName(), existing.score(), chat.getId(), existing.sharedGroupChatIds());
                    byUserId.put(other.getId(), existing);
                }
            }
        }

        List<ChatGrupalEntity> groupChats = chatGrupalRepository.findAllByUsuariosId(userId);
        if (groupChats != null) {
            for (ChatGrupalEntity group : groupChats) {
                if (group == null || !group.isActivo() || group.getUsuarios() == null) {
                    continue;
                }
                for (UsuarioEntity member : group.getUsuarios()) {
                    if (member == null || member.getId() == null || userId.equals(member.getId()) || !member.isActivo()) {
                        continue;
                    }
                    RelatedUserCandidate existing = byUserId.get(member.getId());
                    if (existing == null) {
                        LinkedHashSet<Long> groupIds = new LinkedHashSet<>();
                        groupIds.add(group.getId());
                        existing = new RelatedUserCandidate(member.getId(), displayName(member), scoreRelatedUserCandidate(normalizedTarget, member), null, groupIds);
                    } else {
                        LinkedHashSet<Long> groupIds = new LinkedHashSet<>(existing.sharedGroupChatIds());
                        groupIds.add(group.getId());
                        existing = new RelatedUserCandidate(existing.userId(), existing.fullName(), existing.score(), existing.individualChatId(), groupIds);
                    }
                    byUserId.put(member.getId(), existing);
                }
            }
        }

        List<RelatedUserCandidate> matches = new ArrayList<>();
        for (RelatedUserCandidate candidate : byUserId.values()) {
            if (candidate.score() >= 78) {
                matches.add(candidate);
            }
        }
        matches.sort(Comparator.comparingInt(RelatedUserCandidate::score).reversed()
                .thenComparing(RelatedUserCandidate::fullName, Comparator.nullsLast(String::compareToIgnoreCase)));
        if (matches.isEmpty()) {
            return List.of();
        }

        RelatedUserCandidate top = matches.get(0);
        List<RelatedUserCandidate> narrowed = new ArrayList<>();
        int threshold = tokenCount(normalizedTarget) >= 2 ? Math.max(85, top.score() - 8) : Math.max(78, top.score() - 4);
        for (RelatedUserCandidate candidate : matches) {
            if (candidate.score() >= threshold) {
                narrowed.add(candidate);
            }
        }
        return narrowed;
    }

    private int scoreRelatedUserCandidate(String normalizedTarget, UsuarioEntity user) {
        if (!hasText(normalizedTarget) || user == null) {
            return 0;
        }
        String normalizedFull = normalizeSummaryName(displayName(user));
        String normalizedNombre = normalizeSummaryName(user.getNombre());
        String normalizedApellido = normalizeSummaryName(user.getApellido());
        int best = 0;
        best = Math.max(best, scoreNameCandidate(normalizedTarget, normalizedFull));
        best = Math.max(best, scoreNameCandidate(normalizedTarget, normalizedNombre));
        best = Math.max(best, scoreNameCandidate(normalizedTarget, normalizedApellido));
        return best;
    }

    private int scoreNameCandidate(String target, String candidate) {
        if (!hasText(target) || !hasText(candidate)) {
            return 0;
        }
        if (target.equals(candidate)) {
            return 100;
        }
        if (candidate.contains(target) || target.contains(candidate)) {
            return tokenCount(target) >= 2 ? 94 : 88;
        }
        return similarityScore(target, candidate);
    }

    private int similarityScore(String a, String b) {
        if (!hasText(a) || !hasText(b)) {
            return 0;
        }
        int maxLen = Math.max(a.length(), b.length());
        if (maxLen == 0) {
            return 100;
        }
        int distance = levenshteinDistance(a, b);
        return (int) Math.round((1.0 - ((double) distance / maxLen)) * 100.0);
    }

    private int levenshteinDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            dp[0][j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[a.length()][b.length()];
    }

    private int tokenCount(String value) {
        if (!hasText(value)) {
            return 0;
        }
        return value.trim().split("\\s+").length;
    }

    private boolean senderAllowsAuthor(Long userId, SenderResolution senderResolution, Long emisorId) {
        if (emisorId == null) {
            return false;
        }
        if (senderResolution == null || senderResolution.senderScope() == AiMessageSearchSenderScope.ANY_PARTICIPANT) {
            return true;
        }
        if (senderResolution.senderScope() == AiMessageSearchSenderScope.RECEIVED_MESSAGES) {
            return userId == null || !userId.equals(emisorId);
        }
        if (senderResolution.senderScope() == AiMessageSearchSenderScope.AUTHENTICATED_USER) {
            return userId != null && userId.equals(emisorId);
        }
        return senderResolution.allowedEmitterIds().contains(emisorId);
    }

    private SearchWindow buildSearchWindow(Long userId,
                                           ValidationValues values,
                                           AiMessageSearchScopeDTO scope,
                                           SenderResolution senderResolution,
                                           boolean fallbackGlobal) {
        LinkedHashSet<Long> chatIds = fallbackGlobal
                ? new LinkedHashSet<>(resolveFallbackChatIds(userId, values, scope, senderResolution))
                : new LinkedHashSet<>(resolveInitialChatIds(userId, values, scope, senderResolution));
        boolean unresolvedSpecificPerson = !fallbackGlobal
                && senderResolution != null
                && senderResolution.relatedUsers().isEmpty()
                && (hasText(senderResolution.personaObjetivoNombre()) || hasText(senderResolution.emisorObjetivoNombre()))
                && senderResolution.senderScope() != AiMessageSearchSenderScope.ANY_PARTICIPANT;
        if (chatIds.isEmpty() && !unresolvedSpecificPerson) {
            chatIds.addAll(resolveScopeChatIds(userId, values, scope));
        }
        List<Long> emitterIds = resolveEmitterIds(userId, senderResolution, fallbackGlobal);
        boolean filterByEmitters = emitterIds != null && !emitterIds.isEmpty();
        return new SearchWindow(new ArrayList<>(chatIds), filterByEmitters, filterByEmitters ? emitterIds : List.of(-1L));
    }

    private List<Long> resolveInitialChatIds(Long userId,
                                             ValidationValues values,
                                             AiMessageSearchScopeDTO scope,
                                             SenderResolution senderResolution) {
        if (senderResolution == null || senderResolution.relatedUsers().isEmpty()) {
            return resolveScopeChatIds(userId, values, scope);
        }
        if (scope != null && scope.getTipoScope() == AiMessageSearchScopeType.GRUPO && scope.getChatGrupalId() != null) {
            return List.of(scope.getChatGrupalId());
        }
        LinkedHashSet<Long> chatIds = new LinkedHashSet<>();
        for (RelatedUserCandidate user : senderResolution.relatedUsers()) {
            if (user.individualChatId() != null && values.incluirIndividuales()) {
                chatIds.add(user.individualChatId());
            }
            if (values.incluirGrupales()) {
                chatIds.addAll(user.sharedGroupChatIds());
            }
        }
        return new ArrayList<>(chatIds);
    }

    private List<Long> resolveFallbackChatIds(Long userId,
                                              ValidationValues values,
                                              AiMessageSearchScopeDTO scope,
                                              SenderResolution senderResolution) {
        if (scope != null && scope.getTipoScope() == AiMessageSearchScopeType.GRUPO && scope.getChatGrupalId() != null) {
            return List.of(scope.getChatGrupalId());
        }
        return resolveScopeChatIds(userId, values, buildGlobalFallbackScope(scope));
    }

    private List<Long> resolveScopeChatIds(Long userId,
                                           ValidationValues values,
                                           AiMessageSearchScopeDTO scope) {
        LinkedHashSet<Long> chatIds = new LinkedHashSet<>();
        AiMessageSearchScopeType type = scope == null || scope.getTipoScope() == null ? AiMessageSearchScopeType.GLOBAL : scope.getTipoScope();
        if (type == AiMessageSearchScopeType.INDIVIDUAL && scope.getChatId() != null) {
            chatIds.add(scope.getChatId());
            return new ArrayList<>(chatIds);
        }
        if (type == AiMessageSearchScopeType.GRUPO && scope.getChatGrupalId() != null) {
            chatIds.add(scope.getChatGrupalId());
            return new ArrayList<>(chatIds);
        }
        boolean includeIndividuals = values.incluirIndividuales() && type != AiMessageSearchScopeType.GLOBAL_GRUPOS;
        boolean includeGroups = values.incluirGrupales();
        if (includeIndividuals) {
            List<ChatIndividualEntity> chats = chatIndividualRepository.findAllByUsuario1IdOrUsuario2Id(userId, userId);
            if (chats != null) {
                for (ChatIndividualEntity chat : chats) {
                    if (chat != null && chat.getId() != null && !chat.isAdminDirect()) {
                        chatIds.add(chat.getId());
                    }
                }
            }
        }
        if (includeGroups) {
            List<ChatGrupalEntity> groups = chatGrupalRepository.findAllByUsuariosId(userId);
            if (groups != null) {
                for (ChatGrupalEntity group : groups) {
                    if (group != null && group.getId() != null && group.isActivo()) {
                        chatIds.add(group.getId());
                    }
                }
            }
        }
        return new ArrayList<>(chatIds);
    }

    private List<Long> resolveEmitterIds(Long userId, SenderResolution senderResolution, boolean fallbackGlobal) {
        if (senderResolution == null) {
            return List.of(userId);
        }
        if (senderResolution.senderScope() == AiMessageSearchSenderScope.RECEIVED_MESSAGES) {
            // No emisor whitelist; post-filter excludes userId after load.
            return List.of();
        }
        if (fallbackGlobal) {
            if (senderResolution.senderScope() == AiMessageSearchSenderScope.AUTHENTICATED_USER) {
                return userId == null ? List.of() : List.of(userId);
            }
            return List.of();
        }
        if (senderResolution.senderScope() == AiMessageSearchSenderScope.AUTHENTICATED_USER) {
            return userId == null ? List.of() : List.of(userId);
        }
        if (senderResolution.senderScope() == AiMessageSearchSenderScope.ANY_PARTICIPANT) {
            return List.of();
        }
        return senderResolution.allowedEmitterIds();
    }

    private String resolveUserDisplayName(Long userId) {
        if (userId == null) {
            return null;
        }
        try {
            return usuarioRepository.findById(userId)
                    .map(this::displayName)
                    .orElse(null);
        } catch (Exception ex) {
            return null;
        }
    }

    private String resolvePersonaSolicitada(AiMessageSearchScopeDTO scope, AiMessageSearchNaturalQueryAnalysis analysis) {
        String explicit = analysis == null ? null : firstNonBlank(analysis.getPersonaObjetivoDetectada(), analysis.getNombrePersonaDetectado());
        if (hasText(explicit)) {
            return explicit;
        }
        if (scope != null && scope.isIntencionIndividualDetectada()) {
            return firstNonBlank(scope.getNombreDetectado(), scope.getNombreScopeAplicado());
        }
        return null;
    }

    private String resolveGrupoSolicitado(AiMessageSearchScopeDTO scope, AiMessageSearchNaturalQueryAnalysis analysis) {
        String explicit = analysis == null ? null : analysis.getNombreGrupoDetectado();
        if (hasText(explicit)) {
            return explicit;
        }
        if (scope != null && scope.isIntencionGrupoDetectada()) {
            return firstNonBlank(scope.getNombreDetectado(), scope.getNombreScopeAplicado());
        }
        return null;
    }

    private String resolveNombreScopeSolicitado(AiMessageSearchScopeDTO scope,
                                                AiMessageSearchNaturalQueryAnalysis analysis,
                                                String personaSolicitada,
                                                String grupoSolicitado) {
        if (hasText(grupoSolicitado)) {
            return grupoSolicitado;
        }
        if (hasText(personaSolicitada)) {
            return personaSolicitada;
        }
        if (scope != null && (scope.isIntencionGrupoDetectada() || scope.isIntencionIndividualDetectada())) {
            return firstNonBlank(scope.getNombreScopeAplicado(), scope.getNombreDetectado());
        }
        if (analysis != null) {
            return firstNonBlank(analysis.getNombreGrupoDetectado(), analysis.getNombrePersonaDetectado());
        }
        return null;
    }

    private String resolveNaturalSummary(AiMessageSearchInternalResponseDTO internalResponse,
                                         List<AiEncryptedMessageSearchResultDTO> resultados,
                                         AiMessageSearchScopeDTO scope,
                                         String requestedType) {
        if (resultados == null || resultados.isEmpty()) {
            return "No encontré una coincidencia lo bastante clara con esa búsqueda.";
        }
        String fromAi = internalResponse == null ? null : normalizeSummaryPunctuation(internalResponse.getResumenBusquedaNatural());
        if (hasText(fromAi)) {
            return truncate(fromAi, 220);
        }
        return buildMinimalDirectSummary(resultados.get(0), requestedType, scope);
    }

    private List<AiMessageSearchInternalResultDTO> filterInternalResultsByMinRelevancia(String requestId,
                                                                                         List<AiMessageSearchInternalResultDTO> internalResults) {
        int totalAntes = internalResults == null ? 0 : internalResults.size();
        List<AiMessageSearchInternalResultDTO> filtrados = new ArrayList<>();
        if (internalResults != null) {
            for (AiMessageSearchInternalResultDTO result : internalResults) {
                if (result == null) continue;
                int relevancia = result.getRelevancia() == null ? 0 : result.getRelevancia();
                // Dejar pasar resultados >=umbral o resultados marcados como mejor aproximado (sinResultadosFuertes)
                if (relevancia >= MIN_RELEVANCIA_PUBLICA || Boolean.TRUE.equals(result.getMejorResultadoAproximado())) {
                    filtrados.add(result);
                }
            }
        }
        filtrados.sort(Comparator.comparing(
                        (AiMessageSearchInternalResultDTO r) -> r == null || r.getRelevancia() == null ? 0 : r.getRelevancia())
                .reversed()
                .thenComparing(r -> r == null ? null : r.getMensajeId(), Comparator.nullsLast(Comparator.reverseOrder())));
        LOGGER.info("[AI][MESSAGE_SEARCH_ENCRYPTED] requestId={} totalResultadosAntesFiltro={} totalResultadosDespuesFiltro={} relevanciaMinimaAplicada={}",
                requestId,
                totalAntes,
                filtrados.size(),
                MIN_RELEVANCIA_PUBLICA);
        return filtrados;
    }

    private String buildMinimalDirectSummary(AiEncryptedMessageSearchResultDTO result,
                                             String requestedType,
                                             AiMessageSearchScopeDTO scope) {
        if (result == null) {
            return null;
        }
        String base;
        if (hasText(result.getNombreChatGrupal())) {
            base = "Lo encontré en el grupo " + result.getNombreChatGrupal();
        } else if (hasText(result.getNombreReceptor())) {
            base = "Lo encontré en el chat con " + result.getNombreReceptor();
        } else if (scope != null && hasText(scope.getNombreScopeAplicado())) {
            base = "Lo encontré en " + scope.getNombreScopeAplicado();
        } else {
            base = "Encontré una coincidencia";
        }
        if (hasText(requestedType) && hasText(result.getTipoMensaje()) && !requestedType.equals(result.getTipoMensaje())) {
            return base + ". Era " + resolveTipoHumano(result.getTipoMensaje()) + ".";
        }
        return base + ".";
    }

    private boolean isValidInternalResponse(AiMessageSearchInternalResponseDTO response) {
        return response != null && response.getResultados() != null;
    }

    private String resolveServiceErrorMessage(AiMessageSearchInternalResponseDTO response) {
        if (response == null || !hasText(response.getMensaje())) {
            return "El microservicio de busqueda IA devolvio un error.";
        }
        return response.getMensaje();
    }

    private List<AiEncryptedMessageSearchResultDTO> buildPublicResults(List<AiMessageSearchInternalResultDTO> internalResults,
                                                                       Map<Long, CandidateMessage> candidates,
                                                                       int maxResultados,
                                                                       AiMessageSearchScopeDTO scope,
                                                                       boolean intencionAudio,
                                                                       String consulta) {
        List<AiEncryptedMessageSearchResultDTO> out = new ArrayList<>();
        if (internalResults == null || internalResults.isEmpty() || candidates == null || candidates.isEmpty()) {
            return intencionAudio ? fallbackAudioResults(candidates, maxResultados, consulta) : out;
        }

        for (AiMessageSearchInternalResultDTO internalResult : internalResults) {
            if (internalResult == null || internalResult.getMensajeId() == null) {
                continue;
            }
            CandidateMessage base = candidates.get(internalResult.getMensajeId());
            if (base == null) {
                continue;
            }
            if (!matchesScopeRestriction(scope, base)) {
                continue;
            }

            Integer relevancia = internalResult.getRelevancia();
            if (relevancia == null || relevancia < 1 || relevancia > 100) {
                continue;
            }

            String motivo = truncate(normalizeInput(internalResult.getMotivoCoincidencia()), MAX_REASON_LENGTH);
            if (!hasText(motivo)) {
                continue;
            }
            if ("AUDIO".equals(base.tipoMensaje()) && base.audioTranscrito() && !intencionAudio) {
                String audioNote = "El usuario menciono mensaje, pero la coincidencia esta en un audio transcrito automaticamente. ";
                motivo = truncate(audioNote + motivo, MAX_REASON_LENGTH);
            }
            AiEncryptedMessageSearchResultDTO publicResult = toPublicResult(base, motivo, relevancia);
            if (Boolean.TRUE.equals(internalResult.getMejorResultadoAproximado())) {
                publicResult.setMejorResultadoAproximado(true);
            }
            out.add(publicResult);
        }

        out.sort(Comparator.comparing(AiEncryptedMessageSearchResultDTO::getRelevancia).reversed()
                .thenComparing(AiEncryptedMessageSearchResultDTO::getMensajeId, Comparator.reverseOrder()));

        if (intencionAudio) {
            List<AiEncryptedMessageSearchResultDTO> audioOnly = out.stream()
                    .filter(result -> "AUDIO".equals(result.getTipoMensaje()))
                    .toList();
            if (!audioOnly.isEmpty()) {
                out = new ArrayList<>(audioOnly);
            } else {
                out = fallbackAudioResults(candidates, maxResultados, consulta);
            }
        }

        if (out.size() > maxResultados) {
            return new ArrayList<>(out.subList(0, maxResultados));
        }
        LOGGER.info("[AI][MESSAGE_SEARCH_ENCRYPTED] results-filtered tipoScopeAplicado={} totalCandidatos={} tipoChatPrimerResultado={}",
                scope == null || scope.getTipoScope() == null ? "GLOBAL" : scope.getTipoScope().name(),
                candidates.size(),
                out.isEmpty() ? null : out.get(0).getTipoChat());
        return out;
    }

    private AiEncryptedMessageSearchResultDTO toPublicResult(CandidateMessage candidate, String motivoCoincidencia, Integer relevancia) {
        AiEncryptedMessageSearchResultDTO dto = new AiEncryptedMessageSearchResultDTO();
        dto.setMensajeId(candidate.mensajeId());
        dto.setChatId(candidate.chatId());
        dto.setTipoChat(candidate.tipoChat());
        dto.setEmisorId(candidate.emisorId());
        dto.setNombreEmisor(candidate.nombreEmisor());
        dto.setReceptorId(candidate.receptorId());
        dto.setNombreReceptor(candidate.nombreReceptor());
        dto.setChatGrupalId(candidate.chatGrupalId());
        dto.setNombreChatGrupal(candidate.nombreChatGrupal());
        dto.setFechaEnvio(formatDate(candidate.fechaEnvio()));
        dto.setTipoMensaje(candidate.tipoMensaje());
        dto.setDescripcionTipoMensaje(candidate.descripcionTipoMensaje());
        dto.setEsMultimedia(candidate.esMultimedia());
        dto.setContenido(candidate.contenido());
        dto.setContenidoVisible(candidate.contenidoVisible());
        dto.setMediaUrl(candidate.mediaUrl());
        dto.setMimeType(candidate.mimeType());
        dto.setNombreArchivo(candidate.nombreArchivo());
        dto.setImageUrl(candidate.imageUrl());
        dto.setImageMime(candidate.imageMime());
        dto.setImageNombre(candidate.imageNombre());
        dto.setStickerId(candidate.stickerId());
        dto.setContentKind(candidate.contentKind());
        dto.setMotivoCoincidencia(motivoCoincidencia);
        dto.setRelevancia(relevancia);
        return dto;
    }

    private AudioSearchContent resolveAudioSearchContent(MensajeEntity mensaje) {
        try {
            Optional<AudioPayloadDTO> maybeAudioPayload = resolveAudioFromDbMessage(mensaje);
            if (maybeAudioPayload.isEmpty()) {
                return null;
            }
            AudioPayloadDTO audioPayload = maybeAudioPayload.get();
            if (!isAllowedAudioMime(audioPayload.mimeType())) {
                return null;
            }
            if (audioPayload.bytes().length > aiProperties.getAudioTranscription().getMaxAudioSizeBytes()) {
                return null;
            }
            AudioTranscriptionResultDTO transcription = audioTranscriptionService.transcribirAudio(mensaje, audioPayload.bytes(), audioPayload.mimeType());
            if (transcription == null || !transcription.isSuccess() || !hasText(transcription.getTranscripcion())) {
                return null;
            }
            return new AudioSearchContent(truncate(AUDIO_TRANSCRIBED_PREFIX + normalizeInput(transcription.getTranscripcion()), MAX_MESSAGE_CONTENT_LENGTH), true);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private Optional<AudioPayloadDTO> resolveAudioFromDbMessage(MensajeEntity mensaje) {
        if (mensaje == null) return Optional.empty();
        if (hasText(mensaje.getMediaUrl())) {
            byte[] bytes = readAudioFromMediaUrl(mensaje.getMediaUrl());
            if (bytes != null && bytes.length > 0) {
                String mime = normalizeAudioMime(mensaje.getMediaMime(), readContentAudioMime(mensaje.getContenido()));
                if (looksLikeAudioBinary(bytes, mime)) return Optional.of(new AudioPayloadDTO(bytes, mime));
                byte[] decrypted = decryptAudioFilePayload(bytes, mensaje.getContenido());
                if (decrypted != null && decrypted.length > 0) return Optional.of(new AudioPayloadDTO(decrypted, mime));
            }
        }
        String contentAudioUrl = readContentAudioUrl(mensaje.getContenido());
        if (hasText(contentAudioUrl)) {
            byte[] bytes = readAudioFromMediaUrl(contentAudioUrl);
            if (bytes != null && bytes.length > 0) {
                String mime = normalizeAudioMime(readContentAudioMime(mensaje.getContenido()), mensaje.getMediaMime());
                if (looksLikeAudioBinary(bytes, mime)) return Optional.of(new AudioPayloadDTO(bytes, mime));
                byte[] decrypted = decryptAudioFilePayload(bytes, mensaje.getContenido());
                if (decrypted != null && decrypted.length > 0) return Optional.of(new AudioPayloadDTO(decrypted, mime));
            }
        }
        String contenido = normalizeInput(mensaje.getContenido());
        if (hasText(contenido)) {
            byte[] decrypted = aiEncryptedContextService.decryptMessagePayloadToBytes(contenido);
            if (decrypted != null && decrypted.length > 0) {
                return Optional.of(new AudioPayloadDTO(decrypted, normalizeAudioMime(mensaje.getMediaMime(), null)));
            }
        }
        return Optional.empty();
    }

    private byte[] readAudioFromMediaUrl(String mediaUrl) {
        if (!hasText(mediaUrl) || !mediaUrl.startsWith(Constantes.UPLOADS_PREFIX)) {
            return null;
        }
        try {
            String relative = mediaUrl.substring(Constantes.UPLOADS_PREFIX.length());
            Path root = Paths.get(uploadsRoot).toAbsolutePath().normalize();
            Path file = root.resolve(relative).normalize();
            if (!file.startsWith(root) || !Files.exists(file) || !Files.isRegularFile(file)) {
                return null;
            }
            return Files.readAllBytes(file);
        } catch (Exception ex) {
            return null;
        }
    }

    private String readContentAudioUrl(String contenido) {
        return readContentTextField(contenido, "audioUrl");
    }

    private String readContentAudioMime(String contenido) {
        return readContentTextField(contenido, "audioMime");
    }

    private String readContentIvFile(String contenido) {
        return readContentTextField(contenido, "ivFile");
    }

    private String readContentAdminEnvelope(String contenido) {
        return readContentTextField(contenido, "forAdmin");
    }

    private String readContentTextField(String contenido, String fieldName) {
        if (!hasText(contenido) || !hasText(fieldName)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(contenido);
            String value = normalizeInput(root.path(fieldName).asText(null));
            return hasText(value) ? value : null;
        } catch (Exception ex) {
            return null;
        }
    }

    private byte[] decryptAudioFilePayload(byte[] encryptedBytes, String contenido) {
        if (encryptedBytes == null || encryptedBytes.length == 0 || !hasText(contenido)) {
            return null;
        }
        String ivFile = readContentIvFile(contenido);
        String adminEnvelope = readContentAdminEnvelope(contenido);
        if (!hasText(ivFile) || !hasText(adminEnvelope)) {
            return null;
        }
        try {
            byte[] adminEnvelopeBytes = adminAuditCrypto.decryptBase64EnvelopeBytes(adminEnvelope);
            byte[] aesKey = resolveAesKey(adminEnvelopeBytes);
            byte[] iv = Base64.getDecoder().decode(ivFile);
            Cipher cipher = Cipher.getInstance(TRANSFORM_AES_GCM);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            return cipher.doFinal(encryptedBytes);
        } catch (Exception ex) {
            return null;
        }
    }

    private byte[] resolveAesKey(byte[] adminEnvelopeBytes) {
        if (adminEnvelopeBytes == null || adminEnvelopeBytes.length == 0) {
            return null;
        }
        if (isValidAesKeyLength(adminEnvelopeBytes.length)) {
            return adminEnvelopeBytes;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(new String(adminEnvelopeBytes, StandardCharsets.UTF_8));
            return isValidAesKeyLength(decoded.length) ? decoded : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean isValidAesKeyLength(int len) {
        return len == 16 || len == 24 || len == 32;
    }

    private boolean isAllowedAudioMime(String mime) {
        return hasText(mime) && ALLOWED_AUDIO_MIMES.contains(normalizeAudioMime(mime, null));
    }

    private String normalizeAudioMime(String first, String second) {
        String mime = firstNonBlank(first, second);
        if (!hasText(mime)) {
            return "audio/webm";
        }
        String normalized = mime.trim().toLowerCase(Locale.ROOT);
        return "audio/mp3".equals(normalized) ? "audio/mpeg" : normalized;
    }

    private boolean looksLikeAudioBinary(byte[] bytes, String mime) {
        if (bytes == null || bytes.length < 4) return false;
        String m = normalizeAudioMime(mime, null);
        if ("audio/webm".equals(m)) return bytes.length > 4 && (bytes[0] & 0xFF) == 0x1A && (bytes[1] & 0xFF) == 0x45 && (bytes[2] & 0xFF) == 0xDF && (bytes[3] & 0xFF) == 0xA3;
        if ("audio/ogg".equals(m)) return bytes.length > 4 && bytes[0] == 'O' && bytes[1] == 'g' && bytes[2] == 'g' && bytes[3] == 'S';
        if ("audio/wav".equals(m)) return bytes.length > 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F' && bytes[8] == 'W' && bytes[9] == 'A' && bytes[10] == 'V' && bytes[11] == 'E';
        if ("audio/mpeg".equals(m)) return (bytes[0] == 'I' && bytes[1] == 'D' && bytes[2] == '3') || ((bytes[0] & 0xFF) == 0xFF && ((bytes[1] & 0xE0) == 0xE0));
        if ("audio/mp4".equals(m)) return bytes.length > 12 && bytes[4] == 'f' && bytes[5] == 't' && bytes[6] == 'y' && bytes[7] == 'p';
        return true;
    }

    private boolean matchesScopeRestriction(AiMessageSearchScopeDTO scope, CandidateMessage candidate) {
        if (candidate == null) {
            return false;
        }
        if (scope == null || scope.getTipoScope() == null) {
            return true;
        }
        if (scope.getTipoScope() == AiMessageSearchScopeType.GRUPO
                || scope.getTipoScope() == AiMessageSearchScopeType.GLOBAL_GRUPOS) {
            return Constantes.CHAT_TIPO_GRUPAL.equals(candidate.tipoChat());
        }
        if (scope.getTipoScope() == AiMessageSearchScopeType.INDIVIDUAL) {
            return Constantes.CHAT_TIPO_INDIVIDUAL.equals(candidate.tipoChat());
        }
        if (scope.isIntencionGrupoDetectada() && !scope.isIntencionIndividualDetectada()) {
            return Constantes.CHAT_TIPO_GRUPAL.equals(candidate.tipoChat());
        }
        if (scope.isIntencionIndividualDetectada() && !scope.isIntencionGrupoDetectada()) {
            return Constantes.CHAT_TIPO_INDIVIDUAL.equals(candidate.tipoChat());
        }
        return true;
    }

    private boolean isNonTextContent(String content) {
        String normalized = content.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("data:image")
                || normalized.startsWith("data:audio")
                || normalized.startsWith("data:video")
                || normalized.startsWith("data:application")
                || normalized.equals("[audio]")
                || normalized.equals("[imagen]")
                || normalized.equals("[image]")
                || normalized.equals("[video]")
                || normalized.equals("[file]")
                || normalized.equals("[archivo]")
                || normalized.equals("[sticker]");
    }

    private SearchIntent resolveSearchIntent(String consulta, AiMessageSearchNaturalQueryAnalysis analysis) {
        String normalized = normalizeIntentText(consulta);
        String requestedType = detectRequestedMessageType(normalized, analysis);

        if ((analysis != null && analysis.isIntencionUltimoMensaje()) || esConsultaDeUltimoMensaje(consulta)) {
            return new SearchIntent(true, SearchDirection.LAST, requestedType);
        }
        if ((analysis != null && analysis.isIntencionPrimerMensaje()) || esConsultaDePrimerMensaje(consulta)) {
            return new SearchIntent(true, SearchDirection.FIRST, requestedType);
        }
        return SearchIntent.semantic(requestedType);
    }

    private boolean esConsultaDeAudio(String consulta) {
        String normalized = normalizeIntentText(consulta);
        return containsAny(normalized,
                "audio",
                "nota de voz",
                "mensaje de voz",
                "voz",
                "escuchar",
                "donde dije en un audio",
                "audio donde dije",
                "audio en el que dije");
    }

    private boolean esConsultaDeUltimoMensaje(String consulta) {
        String normalized = normalizeIntentText(consulta);
        return containsAny(normalized,
                "ultimo mensaje",
                "lo ultimo",
                "ultimo que mande",
                "ultimo que envie",
                "mas reciente",
                "ultimo audio",
                "ultima imagen",
                "ultimo sticker",
                "ultimo archivo");
    }

    private boolean esConsultaDePrimerMensaje(String consulta) {
        String normalized = normalizeIntentText(consulta);
        return containsAny(normalized,
                "primer mensaje",
                "lo primero",
                "primero que mande",
                "primero que envie",
                "mas antiguo",
                "primer audio",
                "primera imagen",
                "primer sticker",
                "primer archivo");
    }

    private String detectRequestedMessageType(String normalizedConsulta, AiMessageSearchNaturalQueryAnalysis analysis) {
        if (analysis != null) {
            if (analysis.isIntencionAudio()) {
                return "AUDIO";
            }
            if (analysis.isIntencionImagen()) {
                return "IMAGE";
            }
            if (analysis.isIntencionSticker()) {
                return "STICKER";
            }
            if (analysis.isIntencionArchivo()) {
                return "FILE";
            }
        }
        if (!hasText(normalizedConsulta)) {
            return null;
        }
        if (normalizedConsulta.contains("texto") || normalizedConsulta.contains("mensaje de texto")) {
            return "TEXT";
        }
        if (esConsultaDeAudio(normalizedConsulta)) {
            return "AUDIO";
        }
        if (normalizedConsulta.contains("imagen") || normalizedConsulta.contains("foto")) {
            return "IMAGE";
        }
        if (normalizedConsulta.contains("sticker")) {
            return "STICKER";
        }
        if (normalizedConsulta.contains("archivo") || normalizedConsulta.contains("file")) {
            return "FILE";
        }
        return null;
    }

    private boolean containsAny(String text, String... patterns) {
        if (!hasText(text) || patterns == null) {
            return false;
        }
        for (String pattern : patterns) {
            if (hasText(pattern) && text.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldFallbackWithoutTemporalRange(ValidationValues values,
                                                       List<AiEncryptedMessageSearchResultDTO> resultados) {
        if (values == null || !values.rangoTemporalDetectado()) {
            return false;
        }
        if (resultados == null || resultados.isEmpty()) {
            return true;
        }
        return resultados.stream()
                .map(AiEncryptedMessageSearchResultDTO::getRelevancia)
                .filter(java.util.Objects::nonNull)
                .allMatch(relevancia -> relevancia < 60);
    }

    private List<AiEncryptedMessageSearchResultDTO> fallbackAudioResults(Map<Long, CandidateMessage> candidates,
                                                                         int maxResultados,
                                                                         String consulta) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        List<ScoredAudioCandidate> scored = new ArrayList<>();
        for (CandidateMessage candidate : candidates.values()) {
            if (candidate == null || !"AUDIO".equals(candidate.tipoMensaje()) || !candidate.audioTranscrito()) {
                continue;
            }
            int score = estimateAudioMatchScore(candidate.contenido(), consulta);
            if (score > 0) {
                scored.add(new ScoredAudioCandidate(candidate, score));
            }
        }
        scored.sort(Comparator.comparing(ScoredAudioCandidate::score).reversed()
                .thenComparing(item -> item.candidate().mensajeId(), Comparator.reverseOrder()));

        List<AiEncryptedMessageSearchResultDTO> out = new ArrayList<>();
        for (ScoredAudioCandidate item : scored) {
            out.add(toPublicResult(item.candidate(), "Coincide por la transcripcion del audio.", Math.min(100, item.score())));
            if (out.size() == maxResultados) {
                break;
            }
        }
        return out;
    }

    private int estimateAudioMatchScore(String contenido, String consulta) {
        String normalizedContent = normalizeIntentText(contenido);
        String normalizedQuery = normalizeIntentText(consulta);
        if (!hasText(normalizedContent) || !hasText(normalizedQuery)) {
            return 0;
        }
        int matches = 0;
        for (String token : normalizedQuery.split(" ")) {
            if (token.length() < 3 || "audio".equals(token) || "voz".equals(token) || "mensaje".equals(token)) {
                continue;
            }
            if (normalizedContent.contains(token)) {
                matches++;
            }
        }
        return matches == 0 ? 0 : Math.min(95, 45 + (matches * 15));
    }

    private String normalizeIntentText(String text) {
        if (!hasText(text)) {
            return "";
        }
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        normalized = normalized.toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("[^a-z0-9\\s]", " ");
        return normalized.replaceAll("\\s+", " ").trim();
    }

    private String buildDirectReason(CandidateMessage candidate, AiMessageSearchScopeDTO scope, SearchIntent intent) {
        String scopeFragment = scope != null && scope.getTipoScope() == AiMessageSearchScopeType.GLOBAL
                ? "en el scope aplicado"
                : "en este chat";
        String action = intent.direction() == SearchDirection.FIRST ? "primer" : "ultimo";
        String description = candidate.descripcionTipoMensaje() == null ? "mensaje" : candidate.descripcionTipoMensaje().toLowerCase(Locale.ROOT);

        if (intent.requestedType() != null) {
            return "Es el " + action + " " + description + " enviado " + scopeFragment + ".";
        }
        if ("TEXT".equals(candidate.tipoMensaje())) {
            return "Es el " + action + " mensaje enviado " + scopeFragment + ".";
        }
        String article = startsWithFeminine(description) ? "una " : "un ";
        return "Es el " + action + " mensaje enviado " + scopeFragment + " y se trata de " + article + description + ".";
    }

    private boolean startsWithFeminine(String description) {
        return hasText(description) && description.startsWith("imagen");
    }

    private String resolverTipoMensaje(MensajeEntity mensaje) {
        if (mensaje == null) {
            return "UNKNOWN";
        }
        if (mensaje.getStickerId() != null) {
            return "STICKER";
        }
        if (hasText(mensaje.getMediaMime())) {
            String mime = mensaje.getMediaMime().trim().toLowerCase(Locale.ROOT);
            if (mime.startsWith("audio/")) {
                return "AUDIO";
            }
            if (mime.startsWith("image/")) {
                return "IMAGE";
            }
            if (mime.startsWith("application/")) {
                return "FILE";
            }
        }
        if (hasText(mensaje.getMediaUrl())) {
            return inferMediaTypeFromPayload(mensaje.getContenido(), "FILE");
        }
        if (mensaje.getTipo() != null) {
            return switch (mensaje.getTipo()) {
                case TEXT -> inferMediaTypeFromPayload(mensaje.getContenido(), "TEXT");
                case AUDIO -> "AUDIO";
                case IMAGE -> "IMAGE";
                case STICKER -> "STICKER";
                case FILE -> "FILE";
                default -> inferMediaTypeFromPayload(mensaje.getContenido(), "UNKNOWN");
            };
        }
        return inferMediaTypeFromPayload(mensaje.getContenido(), "UNKNOWN");
    }

    private String inferMediaTypeFromPayload(String contenido, String defaultType) {
        if (!hasText(contenido)) {
            return defaultType;
        }
        String normalized = normalizeIntentText(contenido);
        if (normalized.contains("sticker")) {
            return "STICKER";
        }
        if (normalized.contains("audiourl") || normalized.contains("audiomime") || normalized.contains("audio/")) {
            return "AUDIO";
        }
        if (normalized.contains("imageurl") || normalized.contains("imagemime") || normalized.contains("image/")
                || normalized.contains("imagenombre")) {
            return "IMAGE";
        }
        if (normalized.contains("fileurl") || normalized.contains("filemime") || normalized.contains("application/")
                || normalized.contains("filename") || normalized.contains("filenombre")) {
            return "FILE";
        }
        return defaultType;
    }

    private String resolverContenidoVisible(String tipoMensaje, String textoDescifrado) {
        if ("TEXT".equals(tipoMensaje)) {
            return textoDescifrado;
        }
        if ("AUDIO".equals(tipoMensaje)) {
            return "[Audio]";
        }
        if ("IMAGE".equals(tipoMensaje)) {
            return "[Imagen]";
        }
        if ("STICKER".equals(tipoMensaje)) {
            return "[Sticker]";
        }
        if ("FILE".equals(tipoMensaje)) {
            return "[Archivo]";
        }
        return "[Mensaje no textual]";
    }

    private String resolverDescripcionTipoMensaje(String tipoMensaje) {
        if ("TEXT".equals(tipoMensaje)) {
            return "Mensaje de texto";
        }
        if ("AUDIO".equals(tipoMensaje)) {
            return "Audio";
        }
        if ("IMAGE".equals(tipoMensaje)) {
            return "Imagen";
        }
        if ("STICKER".equals(tipoMensaje)) {
            return "Sticker";
        }
        if ("FILE".equals(tipoMensaje)) {
            return "Archivo";
        }
        return "Mensaje no textual";
    }

    private MediaMeta extractMediaMeta(MensajeEntity mensaje) {
        String mimeType = normalizeInput(mensaje == null ? null : mensaje.getMediaMime());
        String mediaUrl = normalizeOutboundMediaUrl(normalizeInput(mensaje == null ? null : mensaje.getMediaUrl()));
        String nombreArchivo = extractFileNameFromUrl(mediaUrl);
        String contenido = mensaje == null ? null : mensaje.getContenido();

        if (hasText(contenido) && (mediaUrl == null || mimeType == null || nombreArchivo == null)) {
            try {
                JsonNode root = objectMapper.readTree(contenido);
                if (mediaUrl == null) {
                    mediaUrl = normalizeOutboundMediaUrl(firstNonBlank(root.path("audioUrl").asText(null),
                            root.path("imageUrl").asText(null),
                            root.path("fileUrl").asText(null),
                            root.path("mediaUrl").asText(null),
                            root.path("url").asText(null)));
                }
                if (mimeType == null) {
                    mimeType = firstNonBlank(root.path("audioMime").asText(null),
                            root.path("imageMime").asText(null),
                            root.path("fileMime").asText(null),
                            root.path("mime").asText(null));
                }
                if (nombreArchivo == null) {
                    nombreArchivo = firstNonBlank(root.path("imageNombre").asText(null),
                            root.path("fileNombre").asText(null),
                            root.path("fileName").asText(null),
                            extractFileNameFromUrl(mediaUrl));
                }
            } catch (Exception ignored) {
                // Search enrichment uses metadata only as best-effort.
            }
        }

        return new MediaMeta(normalizeInput(mediaUrl), normalizeInput(mimeType), normalizeInput(nombreArchivo));
    }

    private ImageCompatibilityMeta extractImageCompatibilityMeta(MensajeEntity mensaje,
                                                                 String tipoMensaje,
                                                                 MediaMeta mediaMeta) {
        if (!"IMAGE".equals(tipoMensaje) && !"STICKER".equals(tipoMensaje)) {
            return ImageCompatibilityMeta.empty();
        }
        String contenido = mensaje == null ? null : mensaje.getContenido();
        String imageUrl = mediaMeta == null ? null : mediaMeta.mediaUrl();
        String imageMime = normalizeInput(mensaje == null ? null : mensaje.getMediaMime());
        String imageNombre = mediaMeta == null ? null : mediaMeta.nombreArchivo();

        if (hasText(contenido)) {
            try {
                JsonNode root = objectMapper.readTree(contenido);
                imageUrl = firstNonBlank(imageUrl, normalizeOutboundMediaUrl(root.path("imageUrl").asText(null)));
                imageMime = firstNonBlank(imageMime, normalizeInput(root.path("imageMime").asText(null)));
                imageNombre = firstNonBlank(imageNombre, normalizeInput(root.path("imageNombre").asText(null)), extractFileNameFromUrl(imageUrl));
            } catch (Exception ignored) {
                // Best-effort compatibility metadata only.
            }
        }

        Long stickerId = mensaje == null ? null : mensaje.getStickerId();
        String contentKind = "STICKER".equals(tipoMensaje) || isLegacyStickerPayload(contenido)
                ? Constantes.TIPO_STICKER
                : Constantes.TIPO_IMAGE;
        return new ImageCompatibilityMeta(imageUrl, imageMime, imageNombre, stickerId, contentKind);
    }

    private String normalizeOutboundMediaUrl(String mediaUrl) {
        String normalized = normalizeInput(mediaUrl);
        if (!hasText(normalized)) {
            return null;
        }
        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            return normalized;
        }
        if (hasText(backendPublicRootUrl) && normalized.startsWith(applicationContextPath + "/")) {
            return backendPublicRootUrl + normalized.substring(applicationContextPath.length());
        }
        if (hasText(applicationContextPath) && normalized.startsWith(applicationContextPath + "/")) {
            return normalized;
        }
        if (normalized.startsWith(Constantes.UPLOADS_PREFIX)
                || normalized.startsWith(Constantes.API_UPLOADS_ALL + "/")
                || normalized.startsWith(Constantes.API_STICKERS + "/")) {
            if (hasText(backendPublicRootUrl)) {
                return backendPublicRootUrl + normalized;
            }
            return applicationContextPath + normalized;
        }
        return normalized;
    }

    private String normalizeContextPath(String contextPath) {
        String normalized = normalizeInput(contextPath);
        if (!hasText(normalized) || "/".equals(normalized)) {
            return "";
        }
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private boolean isLegacyStickerPayload(String contenido) {
        if (!hasText(contenido)) {
            return false;
        }
        try {
            JsonNode root = objectMapper.readTree(contenido);
            String kind = firstNonBlank(
                    normalizeInput(root.path("contentKind").asText(null)),
                    normalizeInput(root.path("content_kind").asText(null)),
                    normalizeInput(root.path("tipo").asText(null)));
            return hasText(kind) && Constantes.TIPO_STICKER.equalsIgnoreCase(kind);
        } catch (Exception ignored) {
            return false;
        }
    }

    private String resolveBackendPublicRootUrl(String configuredPublicBaseUrl,
                                               String serverAddress,
                                               String serverPort,
                                               String contextPath) {
        String configured = normalizeInput(configuredPublicBaseUrl);
        if (hasText(configured)) {
            String trimmed = configured.endsWith("/") ? configured.substring(0, configured.length() - 1) : configured;
            if (hasText(contextPath) && !trimmed.endsWith(contextPath)) {
                return trimmed + contextPath;
            }
            return trimmed;
        }

        String host = normalizeInput(serverAddress);
        if (!hasText(host) || "0.0.0.0".equals(host) || "::".equals(host)) {
            host = "localhost";
        }
        String port = normalizeInput(serverPort);
        StringBuilder out = new StringBuilder("http://").append(host);
        if (hasText(port)) {
            out.append(":").append(port);
        }
        if (hasText(contextPath)) {
            out.append(contextPath);
        }
        return out.toString();
    }

    private String extractFileNameFromUrl(String mediaUrl) {
        if (!hasText(mediaUrl)) {
            return null;
        }
        int slash = mediaUrl.lastIndexOf('/');
        return slash >= 0 && slash + 1 < mediaUrl.length() ? mediaUrl.substring(slash + 1) : mediaUrl;
    }

    private String displayName(UsuarioEntity user) {
        if (user == null) {
            return null;
        }
        String nombre = normalizeInput(user.getNombre());
        String apellido = normalizeInput(user.getApellido());
        String full = ((nombre == null ? "" : nombre) + " " + (apellido == null ? "" : apellido)).trim();
        if (hasText(full)) {
            return truncate(full, 120);
        }
        return truncate(normalizeInput(user.getEmail()), 120);
    }

    private String formatDate(LocalDateTime date) {
        if (date == null) {
            return null;
        }
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private String normalizeInput(String text) {
        if (text == null) {
            return null;
        }
        String compact = text.replace("\r\n", "\n").replace('\r', '\n');
        compact = compact.replaceAll("[\\t\\f\\x0B]+", " ");
        compact = compact.replaceAll(" {2,}", " ");
        compact = compact.replaceAll("\\n{2,}", " ");
        return compact.trim();
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength).trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String safeForLog(String value) {
        return normalizeInput(value) == null ? "" : normalizeInput(value);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (hasText(value)) {
                return value;
            }
        }
        return null;
    }

    // Plantillas con nombre: %s = nombre usuario, %s = nombre incorrecto, %s = chat real
    private static final String[] RESUMEN_PERSONA_INCORRECTA_CON_NOMBRE = {
            "Hola %s, creo que te has confundido un poco: no aparece nada con %s, lo encontré en %s.",
            "%s, parece que el mensaje no estaba con %s sino en %s. ¿Puede ser?",
            "Oye %s, no vi nada con %s pero en %s hay algo que cuadra bastante.",
            "%s, el mensaje que buscas no parece estar con %s, lo encontré en %s."
    };
    // %s = nombre incorrecto, %s = chat real
    private static final String[] RESUMEN_PERSONA_INCORRECTA = {
            "Creo que te has confundido de persona: no apareció en el chat con %s, pero sí encontré algo muy parecido en %s.",
            "Me parece que el mensaje no estaba con %s, sino en %s. ¿Puede ser?",
            "No vi nada con %s, pero sí en %s hay una coincidencia que cuadra bastante.",
            "El mensaje que buscas no parece estar con %s, lo encontré en %s."
    };
    // Persona incorrecta + era audio: %s = nombre usuario, %s = nombre incorrecto, %s = chat real
    private static final String[] RESUMEN_PERSONA_INCORRECTA_Y_AUDIO_CON_NOMBRE = {
            "Hola %s, creo que te confundiste un poco: no fue a %s, lo encontré en %s, y además era un audio.",
            "%s, el mensaje no estaba con %s sino en %s, y encima no era texto sino un audio.",
            "Oye %s, no fue a %s: lo encontré en %s y era un audio, no texto."
    };
    // Persona incorrecta + era audio sin nombre: %s = nombre incorrecto, %s = chat real
    private static final String[] RESUMEN_PERSONA_INCORRECTA_Y_AUDIO = {
            "Creo que te confundiste: no fue a %s, lo encontré en %s, y además era un audio.",
            "El mensaje no estaba con %s sino en %s, y tampoco era texto sino un audio.",
            "No fue a %s: lo localicé en %s, y era un audio, no un texto."
    };
    // Persona incorrecta + otro tipo: %s = nombre usuario, %s = nombre incorrecto, %s = chat real, %s = tipo
    private static final String[] RESUMEN_PERSONA_INCORRECTA_Y_TIPO_CON_NOMBRE = {
            "Hola %s, no fue a %s: lo encontré en %s, y además era %s.",
            "%s, el mensaje no estaba con %s sino en %s, y era %s, no texto."
    };
    // Persona incorrecta + otro tipo sin nombre: %s = nombre incorrecto, %s = chat real, %s = tipo
    private static final String[] RESUMEN_PERSONA_INCORRECTA_Y_TIPO = {
            "No fue a %s: lo encontré en %s, y además era %s.",
            "El mensaje no estaba con %s sino en %s, y encima era %s."
    };
    // %s = chat real
    private static final String[] RESUMEN_SCOPE_SIN_NOMBRE = {
            "No encontré nada en el chat indicado, pero sí encontré una coincidencia en %s.",
            "El mensaje parece estar en %s, no en el chat que mencionaste.",
            "Tu mensaje no estaba donde indicabas; lo localicé en %s."
    };
    private static final String[] RESUMEN_AUDIO_VS_TEXTO = {
            "Creo que el mensaje que buscabas no era texto, sino un audio donde decías algo parecido.",
            "El que mejor coincide es un audio, no un mensaje de texto.",
            "Lo encontré, pero no era texto: era un audio que mandaste con esa idea.",
            "No era un mensaje de texto, era un audio. ¿Es ese?"
    };
    // %s = tipo de mensaje
    private static final String[] RESUMEN_OTRO_TIPO = {
            "El mejor resultado no es texto: es %s relacionado con tu búsqueda.",
            "Lo que más se parece es %s, no un mensaje de texto.",
            "No encontré texto, pero sí %s que cuadra con lo que buscas."
    };
    private static final String[] RESUMEN_TEMPORAL_AMPLIADO = {
            "No apareció nada en el rango de fechas indicado, así que amplié la búsqueda y encontré esto.",
            "Tuve que ampliar la fecha porque no encontré nada en ese período.",
            "El mensaje no está dentro del rango temporal que pediste, pero algo parecido aparece más allá.",
            "No había nada en esas fechas, así que busqué más atrás y encontré esto."
    };
    private static final String[] RESUMEN_RELEVANCIA_BAJA = {
            "Encontré algo, pero no estoy del todo seguro de que sea exactamente lo que buscas.",
            "Lo más parecido que encontré no coincide del todo con lo que pediste.",
            "La coincidencia no es muy fuerte, podría no ser el mensaje exacto.",
            "Esto es lo más parecido que encontré, aunque puede que no sea exactamente lo que buscas."
    };

    private static final String[] RESUMEN_SALUDO = {
            "Hola %s, ",
            "%s, "
    };
    private static final String[] RESUMEN_MISMATCH = {
            "creo que no fue a %s: la mejor coincidencia aparece en %s",
            "diria que no fue a %s; la coincidencia mas clara aparece en %s",
            "parece que no fue a %s: lo encontre en %s"
    };
    private static final String[] RESUMEN_MISMATCH_DUDA = {
            "creo que te confundiste un poco: no parece que fuera a %s, sino en %s",
            "diria que era mas bien en %s y no con %s",
            "tiene pinta de que fue en %s, no con %s"
    };
    private static final String[] RESUMEN_FALLBACK_GLOBAL = {
            "no lo encontre claro en el chat inicial, asi que amplie la busqueda y aparecio una coincidencia en %s",
            "en el chat que marcabas no salio nada claro; al ampliar la busqueda aparecio esta coincidencia en %s",
            "tuve que abrir la busqueda a mas chats y la mejor coincidencia salio en %s"
    };
    private static final String[] RESUMEN_FALLBACK_TEMPORAL = {
            "no aparecio nada claro en el rango inicial, asi que amplie la busqueda y encontre esta coincidencia en %s",
            "fuera de las fechas que marcabas aparecio la coincidencia mas clara, en %s",
            "amplie el rango de fechas porque ahi no salia nada claro, y termino apareciendo en %s"
    };
    private static final String[] RESUMEN_MATCH_OK = {
            "si, encontre una coincidencia clara en el chat que indicabas",
            "si, la coincidencia mas clara sale justo en el chat que mencionaste",
            "encaja bastante bien con el chat que tenias en mente"
    };
    private static final String[] RESUMEN_MATCH_GENERIC = {
            "encontre una coincidencia en %s",
            "la mejor coincidencia que vi aparece en %s",
            "lo mas parecido que encontre sale en %s"
    };
    private static final String[] RESUMEN_TIPO = {
            "ademas, no era %s, sino %s",
            "y por cierto, no era %s: era %s",
            "tambien veo que no era %s, sino %s"
    };
    private static final String[] RESUMEN_RELEVANCIA_BAJA_HUMANA = {
            "Eso si, la coincidencia no es del todo fuerte.",
            "Aun asi, no estoy del todo seguro de que sea exacto.",
            "No te lo daria por seguro al cien por cien."
    };
    private static final String[] RESUMEN_BASE_MISMATCH_CHAT = {
            "👀 Creo que te confundiste de chat: no sale con %s, pero la coincidencia mas clara aparece en %s",
            "👀 Diria que no fue con %s; lo que mejor encaja esta en %s",
            "👀 Tiene mas pinta de estar en %s que con %s"
    };
    private static final String[] RESUMEN_BASE_MISMATCH_CHAT_CON_NOMBRE = {
            "Hola %s, 👀 creo que te fuiste a otro chat: no sale con %s, pero si aparece algo claro en %s",
            "%s, 👀 diria que no fue con %s; la pista buena esta en %s",
            "👀 %s, esto me cuadra bastante mas en %s que con %s"
    };
    private static final String[] RESUMEN_BASE_FALLBACK_SCOPE = {
            "🔎 No lo vi claro en el chat indicado, asi que amplie la busqueda y aparecio esta coincidencia en %s",
            "🔎 En el chat inicial no salia nada claro; al abrir la busqueda encontre esto en %s",
            "🔎 Tuve que mirar mas alla del chat que marcabas y la mejor coincidencia salio en %s"
    };
    private static final String[] RESUMEN_BASE_FALLBACK_TEMPORAL = {
            "🔎 En ese rango no aparecia nada claro, asi que amplie la busqueda y encontre esta coincidencia en %s",
            "🔎 Fuera de las fechas iniciales aparecio la pista mas clara, en %s",
            "🔎 Amplie un poco el rango y lo que mejor cuadra termino saliendo en %s"
    };
    private static final String[] RESUMEN_BASE_MATCH_CLARO = {
            "💬 Si, encontre una coincidencia bastante clara en ese chat",
            "💬 Si, lo que mejor encaja aparece justo donde lo esperabas",
            "💬 Cuadra bastante bien con el chat que tenias en mente"
    };
    private static final String[] RESUMEN_BASE_MATCH_CHAT = {
            "Encontre una coincidencia en %s",
            "La pista mas clara aparece en %s",
            "Lo que mejor encaja sale en %s"
    };
    private static final String[] RESUMEN_BASE_LOCALIZACION_GRUPO = {
            "🔎 Lo encontre en %s",
            "Parece que eso lo dijiste en %s",
            "🔎 Al final aparece en %s"
    };
    private static final String[] RESUMEN_BASE_LOCALIZACION_CHAT = {
            "🔎 Lo encontre en %s",
            "Parece que eso lo mandaste en %s",
            "🔎 La coincidencia aparece en %s"
    };
    private static final String[] RESUMEN_TIPO_AUDIO_HUMANO = {
            "🎧 Ademas, no era un texto escrito: parece que fue un audio",
            "🎧 Y ojo, la coincidencia esta en un audio, no en un mensaje de texto",
            "🎧 Tambien veo que eso iba en audio, no en texto"
    };
    private static final String[] RESUMEN_TIPO_IMAGEN_HUMANO = {
            "🖼 No era un texto: lo que aparecio fue una imagen",
            "🖼 La coincidencia esta en una imagen, no en un mensaje escrito",
            "🖼 Eso encaja mas con una imagen que con un texto"
    };
    private static final String[] RESUMEN_TIPO_STICKER_HUMANO = {
            "🖼 No parece texto: lo que salio fue un sticker",
            "🖼 La mejor coincidencia esta en un sticker, no en un mensaje escrito",
            "🖼 Eso apunta mas a un sticker que a un texto"
    };
    private static final String[] RESUMEN_TIPO_FILE_HUMANO = {
            "👀 No era un texto como tal: aparece como archivo",
            "👀 La coincidencia esta en un archivo, no en un mensaje escrito",
            "👀 Eso salio asociado a un archivo, no a un texto"
    };
    private static final String[] RESUMEN_TIPO_TEXT_HUMANO = {
            "💬 Al final si era un mensaje de texto",
            "💬 La coincidencia cae en un texto escrito",
            "💬 Aqui lo que aparece es un mensaje de texto"
    };
    private static final String[] RESUMEN_RELEVANCIA_SUAVE = {
            "No te lo daria por seguro al cien por cien.",
            "La pista existe, aunque no es una coincidencia super fuerte.",
            "Lo tome como lo mas cercano, pero no es exactisimo."
    };

    private AiEncryptedMessageSearchResponseDTO success(List<AiEncryptedMessageSearchResultDTO> resultados,
                                                        AiMessageSearchScopeDTO scope) {
        return success(resultados, scope, null, null);
    }

    private String buildEmptyResumenBusqueda(AiMessageSearchNaturalQueryAnalysis analysis,
                                             SenderResolution senderResolution,
                                             AiMessageSearchScopeDTO scope,
                                             SearchIntent intent) {
        // Target-based base sentence
        if (analysis != null) {
            if (analysis.isIntencionDenunciaRecibida() && !analysis.isIntencionDenunciaCreada()) {
                return "No he encontrado denuncias recibidas con esos criterios. Prueba a ampliar el rango de fechas o quitar filtros.";
            }
            if (analysis.isIntencionDenunciaCreada() && !analysis.isIntencionDenunciaRecibida()) {
                return "No he encontrado denuncias que hayas puesto con esos criterios. Prueba a indicar la persona concreta o ampliar el rango temporal.";
            }
            if (analysis.isIntencionDenunciaCreada() && analysis.isIntencionDenunciaRecibida()) {
                return "No he encontrado denuncias asociadas a tu cuenta con esos criterios.";
            }
            if (analysis.isIntencionMensajesNoLeidos()) {
                return "No tienes mensajes nuevos pendientes de leer con esos filtros.";
            }
            if (analysis.isIntencionContenidoOfensivo()) {
                return "No he encontrado mensajes ofensivos enviados por ti con esos criterios.";
            }
        }

        // Build descriptive empty result for MESSAGES
        StringBuilder sb = new StringBuilder("No he encontrado ");
        boolean tipoSet = false;
        if (analysis != null) {
            if (analysis.isIntencionAudio()) { sb.append("audios"); tipoSet = true; }
            else if (analysis.isIntencionImagen()) { sb.append("imágenes"); tipoSet = true; }
            else if (analysis.isIntencionSticker()) { sb.append("stickers"); tipoSet = true; }
            else if (analysis.isIntencionArchivo()) { sb.append("archivos"); tipoSet = true; }
        }
        if (!tipoSet) {
            sb.append("mensajes");
        }

        AiMessageSearchSenderScope ss = senderResolution == null ? null : senderResolution.senderScope();
        if (ss == AiMessageSearchSenderScope.RECEIVED_MESSAGES) {
            sb.append(" que te hayan enviado");
        } else if (ss == AiMessageSearchSenderScope.AUTHENTICATED_USER && hasText(senderResolution.personaObjetivoNombre())) {
            sb.append(" que hayas enviado a ").append(senderResolution.personaObjetivoNombre());
        } else if (ss == AiMessageSearchSenderScope.AUTHENTICATED_USER) {
            sb.append(" que hayas enviado");
        } else if ((ss == AiMessageSearchSenderScope.SPECIFIC_OTHER_USER || ss == AiMessageSearchSenderScope.MULTIPLE_POSSIBLE_USERS)
                && hasText(senderResolution.emisorObjetivoNombre())) {
            sb.append(" de ").append(senderResolution.emisorObjetivoNombre());
        }

        AiMessageSearchScopeType tipoScope = effectiveScopeType(scope);
        String nombreScope = scope == null ? null : scope.getNombreScopeAplicado();
        if (tipoScope == AiMessageSearchScopeType.GLOBAL_GRUPOS) {
            sb.append(" en tus grupos");
        } else if (tipoScope == AiMessageSearchScopeType.GRUPO && hasText(nombreScope)) {
            sb.append(" en el grupo ").append(nombreScope);
        } else if (tipoScope == AiMessageSearchScopeType.INDIVIDUAL && hasText(nombreScope)) {
            sb.append(" en el chat con ").append(nombreScope);
        }

        sb.append(" con esos criterios.");

        // Suggestion
        if (tipoScope == AiMessageSearchScopeType.GLOBAL && (ss == null || ss == AiMessageSearchSenderScope.AUTHENTICATED_USER)) {
            sb.append(" Prueba a indicar la persona o el grupo concreto, o amplía el rango de fechas.");
        } else if (tipoScope == AiMessageSearchScopeType.GLOBAL_GRUPOS) {
            sb.append(" Prueba a indicar el nombre del grupo o ampliar el rango de fechas.");
        } else if (tipoScope == AiMessageSearchScopeType.INDIVIDUAL || tipoScope == AiMessageSearchScopeType.GRUPO) {
            sb.append(" Prueba a buscar en todos tus chats o ampliar el rango de fechas.");
        }
        return sb.toString();
    }

    private AiEncryptedMessageSearchResponseDTO success(List<AiEncryptedMessageSearchResultDTO> resultados,
                                                        AiMessageSearchScopeDTO scope,
                                                        String resumenBusqueda,
                                                        Long userId) {
        AiEncryptedMessageSearchResponseDTO response = new AiEncryptedMessageSearchResponseDTO();
        response.setSuccess(true);
        response.setCodigo("OK");
        response.setMensaje("Busqueda realizada correctamente");
        applyScopeMetadata(response, scope);

        boolean hasResultados = resultados != null && !resultados.isEmpty();
        boolean tieneDatosSensibles = hasResultados || hasText(resumenBusqueda);
        String encryptedPayload = null;
        if (userId != null && tieneDatosSensibles) {
            encryptedPayload = buildAndApplyEncryptedPayload(userId, resumenBusqueda, resultados);
        }

        response.setResultados(resultados == null ? List.of() : resultados);
        response.setResumenBusqueda(null); // Nunca devolver en claro: viaja dentro de encryptedPayload
        response.setEncryptedPayload(encryptedPayload);
        return response;
    }

    private String buildAndApplyEncryptedPayload(Long userId,
                                                 String resumenBusqueda,
                                                 List<AiEncryptedMessageSearchResultDTO> resultados) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("resumenBusqueda", resumenBusqueda);
        List<Map<String, Object>> sensitiveResultados = new ArrayList<>();
        if (resultados != null) {
            for (AiEncryptedMessageSearchResultDTO r : resultados) {
                Map<String, Object> sens = new LinkedHashMap<>();
                if (isComplaintResult(r)) {
                    sens.put("tipoResultado", r.getTipoResultado());
                    sens.put("denunciaId", r.getDenunciaId());
                    sens.put("tipoDenuncia", r.getTipoDenuncia());
                    sens.put("motivoDenuncia", r.getMotivoDenuncia());
                    sens.put("gravedadDenuncia", r.getGravedadDenuncia());
                    sens.put("estadoDenuncia", r.getEstadoDenuncia());
                    sens.put("fechaDenuncia", r.getFechaDenuncia());
                    sens.put("contenidoVisible", r.getContenidoVisible());
                    sens.put("motivoCoincidencia", r.getMotivoCoincidencia());
                } else if ("SCHEDULED_MESSAGE".equals(r.getTipoResultado())) {
                    sens.put("tipoResultado", r.getTipoResultado());
                    sens.put("scheduledMessageId", r.getMensajeId());
                    sens.put("chatId", r.getChatId());
                    sens.put("tipoChat", r.getTipoChat());
                    sens.put("nombreReceptor", r.getNombreReceptor());
                    sens.put("nombreChatGrupal", r.getNombreChatGrupal());
                    sens.put("fechaProgramada", r.getFechaEnvio());
                    sens.put("contenidoVisible", r.getContenidoVisible());
                    sens.put("motivoCoincidencia", r.getMotivoCoincidencia());
                } else {
                    sens.put("mensajeId", r.getMensajeId());
                    sens.put("contenido", r.getContenido());
                    sens.put("contenidoVisible", r.getContenidoVisible());
                    sens.put("motivoCoincidencia", r.getMotivoCoincidencia());
                }
                sensitiveResultados.add(sens);
                sanitizeResultPublic(r);
            }
        }
        payload.put("resultados", sensitiveResultados);

        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new RuntimeException("No se pudo serializar el payload sensible de busqueda", ex);
        }
        AiEncryptedResponseDTO encrypted = aiEncryptedContextService.encryptAiResponseForUser(json, userId);
        return encrypted == null ? null : encrypted.getEncryptedPayload();
    }

    private void sanitizeResultPublic(AiEncryptedMessageSearchResultDTO r) {
        // Elimina texto descifrado del DTO publico
        r.setContenido(null);
        r.setMotivoCoincidencia(null);
        if (isComplaintResult(r)) {
            r.setContenidoVisible("[Denuncia]");
        } else if ("SCHEDULED_MESSAGE".equals(r.getTipoResultado())) {
            r.setContenidoVisible(SCHEDULED_CONTENT_PLACEHOLDER);
        } else {
            r.setContenidoVisible(placeholderForType(r.getTipoMensaje()));
        }
    }

    private boolean isComplaintResult(AiEncryptedMessageSearchResultDTO result) {
        return result != null
                && hasText(result.getTipoResultado())
                && result.getTipoResultado().toUpperCase(Locale.ROOT).startsWith("COMPLAINT");
    }

    // ── Intent enrichment (LLM-based) ──────────────────────────────────────────

    private static final double MIN_INTENT_CONFIDENCE = 0.5d;

    private void enrichAnalysisWithIntent(String requestId,
                                          AiMessageSearchNaturalQueryAnalysis analysis,
                                          AiSearchIntentInternalResponseDTO intent) {
        if (analysis == null || intent == null || !intent.isSuccess()) {
            LOGGER.info("[AI][MESSAGE_SEARCH_ENCRYPTED] requestId={} intent-skipped reason={}",
                    requestId,
                    intent == null ? "no-response" : (!intent.isSuccess() ? "service-failure" : "no-analysis"));
            return;
        }
        Double confidence = intent.getConfidence();
        if (confidence == null || confidence < MIN_INTENT_CONFIDENCE) {
            LOGGER.info("[AI][MESSAGE_SEARCH_ENCRYPTED] requestId={} intent-low-confidence confidence={} target={}",
                    requestId, confidence, intent.getTarget());
            return;
        }

        String target = intent.getTarget();
        String complaintDirection = intent.getComplaintDirection();
        String senderScope = intent.getSenderScope();

        // Target → complaint flags (overlay, do not clear deterministic flags)
        if ("COMPLAINTS_RECEIVED".equals(target) || "RECEIVED".equals(complaintDirection)) {
            analysis.setIntencionDenunciaRecibida(true);
        }
        if ("COMPLAINTS_CREATED".equals(target) || "CREATED".equals(complaintDirection)) {
            analysis.setIntencionDenunciaCreada(true);
        }
        if ("MIXED".equals(target)) {
            analysis.setIntencionDenunciaRecibida(true);
            analysis.setIntencionDenunciaCreada(true);
        }

        // Target → unread flag
        if ("UNREAD_MESSAGES".equals(target)) {
            analysis.setIntencionMensajesNoLeidos(true);
            // Unread = received from others; force RECEIVED_MESSAGES if scope not specified
            if (senderScope == null || "AUTHENTICATED_USER".equals(senderScope)) {
                senderScope = "RECEIVED_MESSAGES";
            }
        }
        // readStatus=UNREAD also flags unread (target may be MESSAGES with explicit unread filter)
        if ("UNREAD".equals(intent.getReadStatus())) {
            analysis.setIntencionMensajesNoLeidos(true);
            if (senderScope == null || "AUTHENTICATED_USER".equals(senderScope)) {
                senderScope = "RECEIVED_MESSAGES";
            }
        }

        // Target → offensive content
        if ("OFFENSIVE_CONTENT_SEARCH".equals(target)) {
            analysis.setIntencionContenidoOfensivo(true);
            // Offensive content the user sent → AUTHENTICATED_USER
            if (senderScope == null) {
                senderScope = "AUTHENTICATED_USER";
            }
        }

        // Person mentioned for COMPLAINTS_CREATED → set persona denunciada
        if (analysis.isIntencionDenunciaCreada()
                && hasText(intent.getPersonaMencionada())
                && !hasText(analysis.getPersonaDenunciadaSolicitada())) {
            analysis.setPersonaDenunciadaSolicitada(intent.getPersonaMencionada());
        }

        // Person mentioned for MESSAGES + AUTHENTICATED_USER → personaObjetivoDetectada
        if ("MESSAGES".equals(target)
                && "AUTHENTICATED_USER".equals(senderScope)
                && hasText(intent.getPersonaMencionada())
                && !hasText(analysis.getPersonaObjetivoDetectada())) {
            analysis.setPersonaObjetivoDetectada(intent.getPersonaMencionada());
        }

        // Person mentioned for SPECIFIC_OTHER_USER → emisorObjetivoDetectado
        if ("SPECIFIC_OTHER_USER".equals(senderScope)
                && hasText(intent.getPersonaMencionada())
                && !hasText(analysis.getEmisorObjetivoDetectado())) {
            analysis.setEmisorObjetivoDetectado(intent.getPersonaMencionada());
        }

        // Motivo
        if (hasText(intent.getMotivoDenuncia()) && !hasText(analysis.getMotivoDenunciaDetectado())) {
            String motivo = mapMotivoDenuncia(intent.getMotivoDenuncia());
            if (motivo != null) analysis.setMotivoDenunciaDetectado(motivo);
        }

        // Sender scope (LLM is now authoritative when present)
        if (hasText(senderScope)) {
            try {
                AiMessageSearchSenderScope parsed = AiMessageSearchSenderScope.valueOf(senderScope);
                // LLM authoritative: override deterministic if it was AUTHENTICATED_USER (default fallback) and LLM says otherwise
                if (analysis.getSenderScope() == null
                        || analysis.getSenderScope() == AiMessageSearchSenderScope.AUTHENTICATED_USER
                        || parsed != AiMessageSearchSenderScope.AUTHENTICATED_USER) {
                    analysis.setSenderScope(parsed);
                }
            } catch (IllegalArgumentException ignore) { }
        }

        // Tipo scope solicitado (LLM authoritative when set)
        String tipoScope = intent.getTipoScopeSolicitado();
        if (hasText(tipoScope)) {
            switch (tipoScope) {
                case "GLOBAL_GRUPOS" -> {
                    analysis.setIntencionGrupo(true);
                    analysis.setIntencionIndividual(false);
                }
                case "GLOBAL_INDIVIDUALES" -> {
                    analysis.setIntencionIndividual(true);
                    analysis.setIntencionGrupo(false);
                }
                case "GRUPO_CONCRETO" -> {
                    analysis.setIntencionGrupo(true);
                    analysis.setIntencionIndividual(false);
                    if (hasText(intent.getGrupoMencionado()) && !hasText(analysis.getNombreGrupoDetectado())) {
                        analysis.setNombreGrupoDetectado(intent.getGrupoMencionado());
                    }
                }
                case "INDIVIDUAL_CONCRETO" -> {
                    analysis.setIntencionIndividual(true);
                    analysis.setIntencionGrupo(false);
                    if (hasText(intent.getPersonaMencionada()) && !hasText(analysis.getNombrePersonaDetectado())) {
                        analysis.setNombrePersonaDetectado(intent.getPersonaMencionada());
                    }
                }
                default -> { /* GLOBAL/DESCONOCIDO: no flag mutation */ }
            }
        }

        // Tipo mensaje
        String tipo = intent.getTipoMensajeSolicitado();
        if (hasText(tipo)) {
            switch (tipo) {
                case "AUDIO" -> { if (!analysis.isIntencionAudio()) analysis.setIntencionAudio(true); }
                case "IMAGE" -> { if (!analysis.isIntencionImagen()) analysis.setIntencionImagen(true); }
                case "STICKER" -> { if (!analysis.isIntencionSticker()) analysis.setIntencionSticker(true); }
                case "FILE" -> { if (!analysis.isIntencionArchivo()) analysis.setIntencionArchivo(true); }
                default -> { /* TEXT/ANY: no-op */ }
            }
        }

        // Orden
        String orden = intent.getOrden();
        if ("LATEST".equals(orden) && !analysis.isIntencionUltimoMensaje()) {
            analysis.setIntencionUltimoMensaje(true);
        }
        if ("FIRST".equals(orden) && !analysis.isIntencionPrimerMensaje()) {
            analysis.setIntencionPrimerMensaje(true);
        }

        LOGGER.info("[AI][MESSAGE_SEARCH_ENCRYPTED] requestId={} intent-applied target={} complaintDirection={} senderScope={} tipoMensaje={} orden={} confidence={} personaMencionada={} grupoMencionado={} motivoDenuncia={} intencionContenidoOfensivo={} intencionMensajesNoLeidos={} source=LLM",
                requestId, target, complaintDirection, senderScope, tipo, orden, confidence,
                intent.getPersonaMencionada(), intent.getGrupoMencionado(), intent.getMotivoDenuncia(),
                analysis.isIntencionContenidoOfensivo(), analysis.isIntencionMensajesNoLeidos());
    }

    private String mapMotivoDenuncia(String raw) {
        if (raw == null) return null;
        String lower = raw.toLowerCase(Locale.ROOT);
        if (lower.contains("insult")) return "insult";
        if (lower.contains("amenaz")) return "amenaz";
        if (lower.contains("acoso") || lower.contains("acos")) return "acoso";
        if (lower.contains("spam")) return "spam";
        if (lower.contains("fraude") || lower.contains("estafa")) return "fraude";
        if (lower.contains("inapropi") || lower.contains("ofensiv")) return "inapropiado";
        return raw.trim();
    }

    // ── Complaint search ───────────────────────────────────────────────────────

    private List<UserComplaintEntity> loadDenuncias(Long userId, boolean esCreada, ValidationValues values, Pageable pageable) {
        if (esCreada) {
            if (values.rangoTemporalDetectado() && values.fechaInicio() != null) {
                LocalDateTime fin = values.fechaFin() != null ? values.fechaFin() : LocalDateTime.now();
                return userComplaintRepository.findByDenuncianteIdAndCreatedAtBetweenOrdered(userId, values.fechaInicio(), fin, pageable);
            }
            return userComplaintRepository.findByDenuncianteIdOrderByCreatedAtDescIdDesc(userId, pageable);
        } else {
            if (values.rangoTemporalDetectado() && values.fechaInicio() != null) {
                LocalDateTime fin = values.fechaFin() != null ? values.fechaFin() : LocalDateTime.now();
                return userComplaintRepository.findByDenunciadoIdAndCreatedAtBetweenOrdered(userId, values.fechaInicio(), fin, pageable);
            }
            return userComplaintRepository.findByDenunciadoIdOrderByCreatedAtDescIdDesc(userId, pageable);
        }
    }

    private AiEncryptedMessageSearchResponseDTO buscarDenunciasConIA(
            String requestId,
            Long userId,
            String userEmail,
            ValidationValues values,
            ComplaintBranch complaintBranch,
            AiSearchIntentInternalResponseDTO intent) {

        aiSearchProgressNotifier.notifyStarted(userEmail, requestId, AiSearchProgressStep.ANALYZING_MESSAGES);
        try {
            AiMessageSearchNaturalQueryAnalysis analysis = values.analysis();
            boolean esCreada = complaintBranch == ComplaintBranch.CREATED;
            String complaintDirection = esCreada ? "COMPLAINT_CREATED" : "COMPLAINT_RECEIVED";
            String personaDenunciada = analysis != null ? analysis.getPersonaDenunciadaSolicitada() : null;
            String motivoFiltro = analysis != null ? analysis.getMotivoDenunciaDetectado() : null;

            int maxResults = values.maxResultados() > 0 ? Math.min(values.maxResultados(), 20) : 10;
            Pageable pageable = PageRequest.of(0, maxResults * 3);

            List<UserComplaintEntity> denuncias = loadDenuncias(userId, esCreada, values, pageable);

            // Temporal expansion fallback
            boolean huboFallbackTemporalExpandido = false;
            if (denuncias.isEmpty() && values.rangoTemporalDetectado() && analysis != null && analysis.getFechaInicioExpandida() != null) {
                ValidationValues expandedValues = values.withTemporalRange(
                        analysis.getFechaInicioExpandida(), analysis.getFechaFinExpandida(), analysis.getDescripcionRangoExpandido());
                List<UserComplaintEntity> expandedDenuncias = loadDenuncias(userId, esCreada, expandedValues, pageable);
                if (!expandedDenuncias.isEmpty()) {
                    huboFallbackTemporalExpandido = true;
                    values = expandedValues;
                    denuncias = expandedDenuncias;
                }
            }

            // In-memory filter by motivo
            if (hasText(motivoFiltro) && !denuncias.isEmpty()) {
                String motivoLower = motivoFiltro.toLowerCase(Locale.ROOT);
                List<UserComplaintEntity> filtradas = denuncias.stream()
                        .filter(d -> d.getMotivo() != null && d.getMotivo().toLowerCase(Locale.ROOT).contains(motivoLower))
                        .toList();
                if (!filtradas.isEmpty()) denuncias = filtradas;
            }

            // In-memory filter by person name (for CREATED: filter by denunciado name)
            if (hasText(personaDenunciada) && !denuncias.isEmpty() && esCreada) {
                String personaNorm = Normalizer.normalize(personaDenunciada, Normalizer.Form.NFD)
                        .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
                List<UserComplaintEntity> filtradas = denuncias.stream()
                        .filter(d -> {
                            if (d.getDenunciadoNombre() == null) return false;
                            String nombreNorm = Normalizer.normalize(d.getDenunciadoNombre(), Normalizer.Form.NFD)
                                    .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
                            return nombreNorm.contains(personaNorm);
                        })
                        .toList();
                if (!filtradas.isEmpty()) denuncias = filtradas;
            }

            // Limit to maxResults
            if (denuncias.size() > maxResults) {
                denuncias = denuncias.subList(0, maxResults);
            }

            LOGGER.info("[AI][MESSAGE_SEARCH_ENCRYPTED] requestId={} intentTargetRecibido={} complaintDirection={} confidence={} ramaEjecutada={} userId={} totalDenunciasEncontradas={} totalMensajesBuscados=0 rangoTemporalDetectado={} huboFallbackTemporalExpandido={} personaDenunciadaFiltro={} motivoFiltro={} llamadaMicroservicioIA={}",
                    requestId,
                    intent == null ? null : intent.getTarget(),
                    intent == null ? null : intent.getComplaintDirection(),
                    intent == null ? null : intent.getConfidence(),
                    complaintDirection,
                    userId,
                    denuncias.size(),
                    values.rangoTemporalDetectado(),
                    huboFallbackTemporalExpandido,
                    personaDenunciada,
                    motivoFiltro,
                    !denuncias.isEmpty());

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            final boolean esCreatedFinal = esCreada;
            final String directionFinal = complaintDirection;
            List<AiComplaintCandidateDTO> candidatas = denuncias.stream()
                    .map(d -> {
                        AiComplaintCandidateDTO dto = new AiComplaintCandidateDTO();
                        dto.setDenunciaId(d.getId());
                        dto.setMotivo(d.getMotivo());
                        dto.setEstado(d.getEstado() != null ? d.getEstado().name() : "PENDIENTE");
                        dto.setFechaCreacion(d.getCreatedAt() != null ? d.getCreatedAt().format(dateFormatter) : null);
                        dto.setNombreDenunciado(esCreatedFinal ? d.getDenunciadoNombre() : d.getDenuncianteNombre());
                        dto.setComplaintDirection(directionFinal);
                        return dto;
                    })
                    .toList();

            String usuarioNombre;
            if (esCreada) {
                usuarioNombre = denuncias.isEmpty() ? null : denuncias.get(0).getDenuncianteNombre();
            } else {
                usuarioNombre = denuncias.isEmpty() ? null : denuncias.get(0).getDenunciadoNombre();
            }
            String resumenBusquedaNatural = null;

            if (!candidatas.isEmpty()) {
                try {
                    AiMessageSearchInternalRequestDTO aiRequest =
                            buildComplaintInternalRequest(values, candidatas, usuarioNombre, complaintDirection, personaDenunciada, motivoFiltro, huboFallbackTemporalExpandido);
                    AiMessageSearchInternalResponseDTO internalResponse =
                            aiMessageSearchMicroserviceClient.buscarMensajesConIA(requestId, aiRequest);
                    if (isValidInternalResponse(internalResponse) && internalResponse.isSuccess()) {
                        resumenBusquedaNatural = internalResponse.getResumenBusquedaNatural();
                    }
                } catch (AiMessageSearchMicroserviceUnavailableException | AiMessageSearchMicroserviceException ex) {
                    LOGGER.warn("[AI][MESSAGE_SEARCH_ENCRYPTED] requestId={} complaint-ai-summary-error userId={} errorClass={}",
                            requestId, userId, ex.getClass().getSimpleName());
                }
            }

            if (!hasText(resumenBusquedaNatural)) {
                int n = denuncias.size();
                if (n == 0) {
                    resumenBusquedaNatural = esCreada
                            ? "No he encontrado denuncias creadas por tu cuenta."
                            : "No he encontrado denuncias contra tu cuenta.";
                } else if (esCreada) {
                    resumenBusquedaNatural = "He encontrado " + n + " denuncia" + (n > 1 ? "s" : "") + " que has puesto.";
                } else {
                    resumenBusquedaNatural = "He encontrado " + n + " denuncia" + (n > 1 ? "s" : "") + " contra tu cuenta.";
                }
            }

            List<AiEncryptedMessageSearchResultDTO> resultados = enforceComplaintOnlyResults(
                    requestId,
                    buildComplaintResults(denuncias, dateFormatter, complaintDirection),
                    complaintDirection
            );
            String tipoPrimerResultado = resultados.isEmpty() ? null : resultados.get(0).getTipoResultado();
            LOGGER.info("[AI][MESSAGE_SEARCH_ENCRYPTED] requestId={} ramaEjecutada={} totalDenunciasEncontradas={} totalMensajesBuscados=0 totalResultadosFinales={} tipoResultadoPrimerResultado={}",
                    requestId,
                    complaintDirection,
                    denuncias.size(),
                    resultados.size(),
                    tipoPrimerResultado);

            aiSearchProgressNotifier.notifyCompleted(userEmail, requestId, AiSearchProgressStep.ANALYZING_MESSAGES);
            if (!resultados.isEmpty()) {
                aiSearchProgressNotifier.notifyStarted(userEmail, requestId, AiSearchProgressStep.MESSAGE_FOUND);
                aiSearchProgressNotifier.notifyCompleted(userEmail, requestId, AiSearchProgressStep.MESSAGE_FOUND);
            } else {
                aiSearchProgressNotifier.notifyStarted(userEmail, requestId, AiSearchProgressStep.MESSAGE_NOT_FOUND);
                aiSearchProgressNotifier.notifyCompleted(userEmail, requestId, AiSearchProgressStep.MESSAGE_NOT_FOUND, false);
            }

            return success(resultados, null, resumenBusquedaNatural, userId);

        } catch (RuntimeException ex) {
            LOGGER.warn("[AI][MESSAGE_SEARCH_ENCRYPTED] requestId={} complaint-runtime-error userId={} errorClass={}",
                    requestId, userId, ex.getClass().getSimpleName());
            aiSearchProgressNotifier.notifyError(userEmail, requestId);
            return failure("AI_COMPLAINT_SEARCH_ERROR", "No se pudo completar la busqueda de denuncias.", null);
        }
    }

    private AiMessageSearchInternalRequestDTO buildComplaintInternalRequest(
            ValidationValues values,
            List<AiComplaintCandidateDTO> candidatas,
            String usuarioNombre,
            String complaintDirection,
            String personaDenunciadaSolicitada,
            String motivoDetectado,
            boolean huboFallbackTemporalExpandido) {
        AiMessageSearchInternalRequestDTO req = new AiMessageSearchInternalRequestDTO();
        req.setConsulta(values.consulta());
        req.setConsultaOriginal(values.consulta());
        req.setSearchTarget("COMPLAINTS");
        req.setComplaintDirection(complaintDirection);
        req.setPersonaDenunciadaSolicitada(personaDenunciadaSolicitada);
        req.setMotivoDetectado(motivoDetectado);
        req.setHuboFallbackTemporalExpandido(huboFallbackTemporalExpandido);
        req.setDenunciasCandidatas(candidatas);
        req.setTotalDenunciasEncontradas(candidatas.size());
        req.setMaxResultados(Math.max(1, candidatas.size()));
        req.setRangoTemporalAplicado(values.rangoTemporalDetectado());
        req.setDescripcionRangoTemporal(values.descripcionRangoTemporal());
        req.setUsuarioActualNombre(usuarioNombre);
        return req;
    }

    private List<AiEncryptedMessageSearchResultDTO> buildComplaintResults(
            List<UserComplaintEntity> denuncias,
            DateTimeFormatter dateFormatter,
            String complaintDirection) {
        List<AiEncryptedMessageSearchResultDTO> results = new ArrayList<>();
        String tipoResultado = "COMPLAINT_CREATED".equals(complaintDirection) ? "COMPLAINT_CREATED" : "COMPLAINT_RECEIVED";
        for (UserComplaintEntity d : denuncias) {
            AiEncryptedMessageSearchResultDTO r = new AiEncryptedMessageSearchResultDTO();
            r.setTipoResultado(tipoResultado);
            r.setDenunciaId(d.getId());
            r.setTipoDenuncia(d.getMotivo());
            r.setMotivoDenuncia(d.getMotivo());
            r.setGravedadDenuncia(null);
            r.setEstadoDenuncia(d.getEstado() != null ? d.getEstado().name() : null);
            r.setFechaDenuncia(d.getCreatedAt() != null ? d.getCreatedAt().format(dateFormatter) : null);
            r.setContenidoVisible("[Denuncia]");
            r.setMotivoCoincidencia("Denuncia por: " + d.getMotivo());
            results.add(r);
        }
        return results;
    }

    private List<AiEncryptedMessageSearchResultDTO> enforceComplaintOnlyResults(String requestId,
                                                                                List<AiEncryptedMessageSearchResultDTO> resultados,
                                                                                String ramaEjecutada) {
        List<AiEncryptedMessageSearchResultDTO> filtrados = new ArrayList<>();
        if (resultados == null) {
            return filtrados;
        }
        for (AiEncryptedMessageSearchResultDTO resultado : resultados) {
            if (resultado == null) {
                continue;
            }
            if (!isComplaintResult(resultado)) {
                LOGGER.error("[AI][MESSAGE_SEARCH_ENCRYPTED] requestId={} ramaEjecutada={} invalid-result-type tipoResultado={} denunciaId={} mensajeId={} action=discard",
                        requestId,
                        ramaEjecutada,
                        resultado.getTipoResultado(),
                        resultado.getDenunciaId(),
                        resultado.getMensajeId());
                continue;
            }
            filtrados.add(resultado);
        }
        return filtrados;
    }

    // ── End complaint search ───────────────────────────────────────────────────

    private String placeholderForType(String tipo) {
        if (tipo == null) return "[Mensaje]";
        return switch (tipo) {
            case "TEXT" -> "[Mensaje de texto]";
            case "AUDIO" -> "[Audio]";
            case "IMAGE" -> "[Imagen]";
            case "STICKER" -> "[Sticker]";
            case "FILE" -> "[Archivo]";
            default -> "[Mensaje]";
        };
    }

    private String construirResumenBusqueda(AiMessageSearchScopeType scopeInicialType,
                                            String nombreScopeInicial,
                                            String nombreDetectadoInicial,
                                            boolean intencionPersonaOGrupoInicial,
                                            AiMessageSearchScopeDTO scopeFinal,
                                            List<AiEncryptedMessageSearchResultDTO> resultados,
                                            boolean fallbackScopeGlobal,
                                            boolean fallbackSinRangoTemporal,
                                            String requestedType,
                                            boolean intencionAudioDetectada,
                                            Long userId) {
        if (resultados == null || resultados.isEmpty()) {
            return null;
        }
        AiEncryptedMessageSearchResultDTO primero = resultados.get(0);
        String tipoMensajePrimero = primero.getTipoMensaje();
        int relevancia = primero.getRelevancia() != null ? primero.getRelevancia() : 0;
        String firstName = resolveFirstName(userId);

        // Detectar si la persona buscada no coincide con donde se encontró el mensaje
        boolean personaIncorrecta = false;
        String nombrePersonaBuscada = null;
        String chatReal = resolveNombreChatResultado(primero);

        // Caso 1: fallback explícito INDIVIDUAL/GRUPO -> GLOBAL ganó
        boolean globalGano = fallbackScopeGlobal
                && effectiveScopeType(scopeFinal) == AiMessageSearchScopeType.GLOBAL
                && (scopeInicialType == AiMessageSearchScopeType.INDIVIDUAL
                    || scopeInicialType == AiMessageSearchScopeType.GRUPO);
        if (globalGano) {
            personaIncorrecta = true;
            nombrePersonaBuscada = hasText(nombreScopeInicial) ? nombreScopeInicial : null;
        }

        // Caso 2: scope inicial GLOBAL pero usuario mencionó persona/grupo no resuelto
        if (!personaIncorrecta) {
            boolean nombreNoResueltoPeroDetectado = scopeInicialType == AiMessageSearchScopeType.GLOBAL
                    && hasText(nombreDetectadoInicial)
                    && intencionPersonaOGrupoInicial;
            if (nombreNoResueltoPeroDetectado) {
                String nombreChatReal = primero.getNombreReceptor() != null
                        ? primero.getNombreReceptor()
                        : primero.getNombreChatGrupal();
                if (!nombreCoincide(nombreDetectadoInicial, nombreChatReal)) {
                    personaIncorrecta = true;
                    nombrePersonaBuscada = nombreDetectadoInicial;
                }
            }
        }

        // Detectar mismatch de tipo de mensaje
        boolean eraAudioNoDetectado = !intencionAudioDetectada && !hasText(requestedType) && "AUDIO".equals(tipoMensajePrimero);
        boolean eraOtroTipo = !hasText(requestedType) && hasText(tipoMensajePrimero)
                && !"TEXT".equals(tipoMensajePrimero) && !"AUDIO".equals(tipoMensajePrimero);

        // Combinar persona incorrecta + tipo incorrecto en una sola frase natural
        if (personaIncorrecta) {
            if (eraAudioNoDetectado) {
                if (hasText(firstName) && hasText(nombrePersonaBuscada)) {
                    return truncate(String.format(pickVariant(RESUMEN_PERSONA_INCORRECTA_Y_AUDIO_CON_NOMBRE),
                            firstName, nombrePersonaBuscada, chatReal), 220);
                } else if (hasText(nombrePersonaBuscada)) {
                    return truncate(String.format(pickVariant(RESUMEN_PERSONA_INCORRECTA_Y_AUDIO),
                            nombrePersonaBuscada, chatReal), 200);
                }
                // sin nombre de persona conocido, caemos al genérico de scope sin nombre + audio
                return truncate(String.format(pickVariant(RESUMEN_SCOPE_SIN_NOMBRE), chatReal)
                        + " Además, era un audio, no texto.", 200);
            }
            if (eraOtroTipo) {
                String descripcionTipo = resolveDescripcionTipoMensaje(tipoMensajePrimero);
                if (hasText(firstName) && hasText(nombrePersonaBuscada)) {
                    return truncate(String.format(pickVariant(RESUMEN_PERSONA_INCORRECTA_Y_TIPO_CON_NOMBRE),
                            firstName, nombrePersonaBuscada, chatReal, descripcionTipo), 220);
                } else if (hasText(nombrePersonaBuscada)) {
                    return truncate(String.format(pickVariant(RESUMEN_PERSONA_INCORRECTA_Y_TIPO),
                            nombrePersonaBuscada, chatReal, descripcionTipo), 200);
                }
                return truncate(String.format(pickVariant(RESUMEN_SCOPE_SIN_NOMBRE), chatReal)
                        + " Y era " + descripcionTipo + ", no texto.", 200);
            }
            // Solo persona incorrecta, sin mismatch de tipo
            if (hasText(firstName) && hasText(nombrePersonaBuscada)) {
                return truncate(String.format(pickVariant(RESUMEN_PERSONA_INCORRECTA_CON_NOMBRE),
                        firstName, nombrePersonaBuscada, chatReal), 220);
            } else if (hasText(nombrePersonaBuscada)) {
                return truncate(String.format(pickVariant(RESUMEN_PERSONA_INCORRECTA),
                        nombrePersonaBuscada, chatReal), 200);
            }
            return truncate(String.format(pickVariant(RESUMEN_SCOPE_SIN_NOMBRE), chatReal), 180);
        }

        if (eraAudioNoDetectado) {
            return pickVariant(RESUMEN_AUDIO_VS_TEXTO);
        }

        if (eraOtroTipo) {
            return truncate(String.format(pickVariant(RESUMEN_OTRO_TIPO), resolveDescripcionTipoMensaje(tipoMensajePrimero)), 180);
        }

        if (fallbackSinRangoTemporal) {
            return pickVariant(RESUMEN_TEMPORAL_AMPLIADO);
        }

        if (relevancia < 60) {
            return pickVariant(RESUMEN_RELEVANCIA_BAJA);
        }

        return null;
    }

    private String construirResumenBusqueda(String consulta,
                                            AiMessageSearchNaturalQueryAnalysis analysis,
                                            AiMessageSearchScopeType scopeInicialType,
                                            String nombreScopeInicial,
                                            String nombreDetectadoInicial,
                                            boolean intencionPersonaOGrupoInicial,
                                            AiMessageSearchScopeDTO scopeFinal,
                                            List<AiEncryptedMessageSearchResultDTO> resultados,
                                            boolean fallbackScopeGlobal,
                                            boolean fallbackSinRangoTemporal,
                                            String requestedType,
                                            boolean intencionAudioDetectada,
                                            Long userId) {
        if (resultados == null || resultados.isEmpty()) {
            return null;
        }
        AiEncryptedMessageSearchResultDTO primero = resultados.get(0);
        int relevancia = primero.getRelevancia() != null ? primero.getRelevancia() : 0;
        String tipoMensajePrimero = hasText(primero.getTipoMensaje()) ? primero.getTipoMensaje() : "TEXT";
        String firstName = resolveFirstName(userId);
        String chatReal = resolveNombreChatResultado(primero);
        String nombreReal = resolveNombreRealResultado(primero);
        List<String> nombresMencionados = extractMentionedNames(consulta, analysis, nombreScopeInicial, nombreDetectadoInicial);
        String nombreEsperado = firstNonBlank(nombreScopeInicial, nombreDetectadoInicial, nombresMencionados.isEmpty() ? null : nombresMencionados.get(0));
        String nombreAlternativo = findAlternativeName(nombresMencionados, nombreEsperado, nombreReal);
        boolean localizationQuery = analysis != null && analysis.isIntencionLocalizacion();
        boolean explicitScopeSolicitado = hasText(nombreScopeInicial) || hasText(nombreDetectadoInicial);
        boolean personaIncorrecta = explicitScopeSolicitado && hasText(nombreEsperado) && !nombreCoincide(nombreEsperado, nombreReal);
        boolean coincideExactamenteConScope = !personaIncorrecta
                && intencionPersonaOGrupoInicial
                && hasText(nombreEsperado)
                && nombreCoincide(nombreEsperado, nombreReal);
        boolean mismatchTipo = isTypeMismatch(requestedType, tipoMensajePrimero, intencionAudioDetectada, analysis);
        boolean pareceDudaEntreDos = hasText(nombreAlternativo) && nombreCoincide(nombreAlternativo, nombreReal);

        StringBuilder resumen = new StringBuilder();
        String base;
        if (localizationQuery && !explicitScopeSolicitado) {
            base = buildLocationSummaryBase(primero, chatReal);
        } else if (fallbackScopeGlobal) {
            base = String.format(pickVariant(RESUMEN_BASE_FALLBACK_SCOPE), chatReal);
        } else if (fallbackSinRangoTemporal) {
            base = String.format(pickVariant(RESUMEN_BASE_FALLBACK_TEMPORAL), chatReal);
        } else if (personaIncorrecta) {
            if (hasText(firstName) && ThreadLocalRandom.current().nextBoolean()) {
                base = String.format(pickVariant(RESUMEN_BASE_MISMATCH_CHAT_CON_NOMBRE), firstName, nombreEsperado, chatReal);
            } else {
                String template = pickVariant(pareceDudaEntreDos ? RESUMEN_MISMATCH_DUDA : RESUMEN_BASE_MISMATCH_CHAT);
                base = formatMismatchTemplate(template, nombreEsperado, chatReal);
            }
        } else if (coincideExactamenteConScope && relevancia >= 75) {
            base = pickVariant(RESUMEN_BASE_MATCH_CLARO);
        } else {
            base = String.format(pickVariant(RESUMEN_BASE_MATCH_CHAT), chatReal);
        }
        resumen.append(base);

        if (mismatchTipo) {
            appendSummarySentence(resumen, buildTypeSummary(tipoMensajePrimero));
        } else if ("TEXT".equals(tipoMensajePrimero)
                && coincideExactamenteConScope
                && !fallbackScopeGlobal
                && !fallbackSinRangoTemporal
                && ThreadLocalRandom.current().nextInt(4) == 0) {
            appendSummarySentence(resumen, pickVariant(RESUMEN_TIPO_TEXT_HUMANO));
        }

        if (relevancia < 60) {
            appendSummarySentence(resumen, pickVariant(RESUMEN_RELEVANCIA_SUAVE));
        }

        return truncate(normalizeSummaryPunctuation(resumen.toString()), 220);
    }

    private String resolveFirstName(Long userId) {
        if (userId == null) return null;
        try {
            return usuarioRepository.findById(userId)
                    .map(u -> {
                        String nombre = u.getNombre();
                        if (!hasText(nombre)) return null;
                        String trimmed = nombre.trim();
                        int space = trimmed.indexOf(' ');
                        return space > 0 ? trimmed.substring(0, space) : trimmed;
                    })
                    .orElse(null);
        } catch (Exception ex) {
            return null;
        }
    }

    private String pickVariant(String[] variants) {
        return variants[ThreadLocalRandom.current().nextInt(variants.length)];
    }

    private boolean nombreCoincide(String detectado, String chatNombre) {
        if (!hasText(detectado) || !hasText(chatNombre)) return false;
        String d = normalizeSummaryName(detectado);
        String c = normalizeSummaryName(chatNombre);
        return c.contains(d) || d.contains(c);
    }

    private String resolveNombreChatResultado(AiEncryptedMessageSearchResultDTO resultado) {
        if (hasText(resultado.getNombreChatGrupal())) {
            return "el grupo " + resultado.getNombreChatGrupal();
        }
        if (hasText(resultado.getNombreReceptor())) {
            return "el chat con " + resultado.getNombreReceptor();
        }
        return "otro chat";
    }

    private String resolveDescripcionTipoMensaje(String tipo) {
        if (tipo == null) return "un archivo multimedia";
        return switch (tipo) {
            case "IMAGE" -> "una imagen";
            case "FILE" -> "un archivo";
            case "STICKER" -> "un sticker";
            default -> "un archivo multimedia";
        };
    }

    private void appendSummarySentence(StringBuilder resumen, String sentence) {
        if (resumen == null || !hasText(sentence)) {
            return;
        }
        if (resumen.length() > 0 && resumen.charAt(resumen.length() - 1) != '.') {
            resumen.append(". ");
        } else if (resumen.length() > 0) {
            resumen.append(' ');
        }
        resumen.append(sentence.trim());
    }

    private String formatMismatchTemplate(String template, String nombreEsperado, String chatReal) {
        if (!hasText(template)) {
            return chatReal;
        }
        String lower = template.toLowerCase(Locale.ROOT);
        if (lower.contains("en %s que con %s") || lower.contains("mas %s que %s")) {
            return String.format(template, chatReal, nombreEsperado);
        }
        return String.format(template, nombreEsperado, chatReal);
    }

    private String buildTypeSummary(String tipoMensajePrimero) {
        if (!hasText(tipoMensajePrimero)) {
            return null;
        }
        return switch (tipoMensajePrimero) {
            case "AUDIO" -> pickVariant(RESUMEN_TIPO_AUDIO_HUMANO);
            case "IMAGE" -> pickVariant(RESUMEN_TIPO_IMAGEN_HUMANO);
            case "STICKER" -> pickVariant(RESUMEN_TIPO_STICKER_HUMANO);
            case "FILE" -> pickVariant(RESUMEN_TIPO_FILE_HUMANO);
            case "TEXT" -> pickVariant(RESUMEN_TIPO_TEXT_HUMANO);
            default -> null;
        };
    }

    private String buildLocationSummaryBase(AiEncryptedMessageSearchResultDTO primero, String chatReal) {
        if (primero != null && hasText(primero.getNombreChatGrupal())) {
            return String.format(pickVariant(RESUMEN_BASE_LOCALIZACION_GRUPO), chatReal);
        }
        return String.format(pickVariant(RESUMEN_BASE_LOCALIZACION_CHAT), chatReal);
    }

    private String resolveNombreRealResultado(AiEncryptedMessageSearchResultDTO resultado) {
        if (resultado == null) {
            return null;
        }
        if (hasText(resultado.getNombreChatGrupal())) {
            return resultado.getNombreChatGrupal().trim();
        }
        if (hasText(resultado.getNombreReceptor())) {
            return resultado.getNombreReceptor().trim();
        }
        return null;
    }

    private String resolveTipoHumano(String tipo) {
        if (tipo == null) {
            return "otro tipo de mensaje";
        }
        return switch (tipo) {
            case "TEXT" -> "un mensaje de texto";
            case "AUDIO" -> "un audio";
            case "IMAGE" -> "una imagen";
            case "STICKER" -> "un sticker";
            case "FILE" -> "un archivo";
            default -> "otro tipo de mensaje";
        };
    }

    private String resolveRequestedTypeDescription(String requestedType, AiMessageSearchNaturalQueryAnalysis analysis) {
        if (hasText(requestedType)) {
            return resolveTipoHumano(requestedType);
        }
        if (analysis != null && !analysis.isIntencionAudio() && !analysis.isIntencionArchivo()
                && !analysis.isIntencionImagen() && !analysis.isIntencionSticker()) {
            return "un mensaje de texto";
        }
        return "ese tipo de mensaje";
    }

    private boolean isTypeMismatch(String requestedType,
                                   String actualType,
                                   boolean intencionAudioDetectada,
                                   AiMessageSearchNaturalQueryAnalysis analysis) {
        if (!hasText(actualType)) {
            return false;
        }
        if (hasText(requestedType)) {
            return !requestedType.equals(actualType);
        }
        boolean pidioContenidoNoTexto = intencionAudioDetectada
                || (analysis != null && (analysis.isIntencionImagen() || analysis.isIntencionSticker() || analysis.isIntencionArchivo()));
        return !pidioContenidoNoTexto && !"TEXT".equals(actualType);
    }

    private List<String> extractMentionedNames(String consulta,
                                               AiMessageSearchNaturalQueryAnalysis analysis,
                                               String nombreScopeInicial,
                                               String nombreDetectadoInicial) {
        List<String> names = new ArrayList<>();
        if (analysis != null && analysis.isIntencionLocalizacion()
                && !hasText(nombreScopeInicial)
                && !hasText(nombreDetectadoInicial)) {
            return names;
        }
        addIfUniqueMentionedName(names, nombreScopeInicial);
        addIfUniqueMentionedName(names, nombreDetectadoInicial);
        if (analysis != null) {
            addIfUniqueMentionedName(names, analysis.getNombrePersonaDetectado());
            addIfUniqueMentionedName(names, analysis.getNombreGrupoDetectado());
        }
        collectAlternativeMentionNames(names, consulta, ALT_PERSON_DOUBT_PATTERN);
        collectAlternativeMentionNames(names, consulta, ALT_PERSON_WAS_PATTERN);
        return names;
    }

    private void collectAlternativeMentionNames(List<String> names, String consulta, Pattern pattern) {
        if (names == null || !hasText(consulta) || pattern == null) {
            return;
        }
        Matcher matcher = pattern.matcher(consulta);
        while (matcher.find()) {
            addIfUniqueMentionedName(names, matcher.group(1));
            addIfUniqueMentionedName(names, matcher.group(2));
        }
    }

    private void addIfUniqueMentionedName(List<String> names, String rawName) {
        String cleaned = cleanMentionedName(rawName);
        if (!hasText(cleaned)) {
            return;
        }
        for (String existing : names) {
            if (nombreCoincide(existing, cleaned)) {
                return;
            }
        }
        names.add(cleaned);
    }

    private String cleanMentionedName(String rawName) {
        if (!hasText(rawName)) {
            return null;
        }
        String cleaned = rawName.trim().replaceAll("^[^\\p{L}]+|[^\\p{L}]+$", "");
        if (!hasText(cleaned)) {
            return null;
        }
        String normalized = normalizeSummaryName(cleaned);
        if (!hasText(normalized) || SUMMARY_NAME_STOPWORDS.contains(normalized)) {
            return null;
        }
        return cleaned;
    }

    private String findAlternativeName(List<String> names, String nombrePrimario, String nombreReal) {
        if (names == null || names.isEmpty()) {
            return null;
        }
        for (String name : names) {
            if (!hasText(name)) {
                continue;
            }
            if (hasText(nombrePrimario) && nombreCoincide(nombrePrimario, name)) {
                continue;
            }
            if (hasText(nombreReal) && nombreCoincide(nombreReal, name)) {
                return name;
            }
        }
        for (String name : names) {
            if (!hasText(name)) {
                continue;
            }
            if (hasText(nombrePrimario) && nombreCoincide(nombrePrimario, name)) {
                continue;
            }
            return name;
        }
        return null;
    }

    private String normalizeSummaryName(String value) {
        if (!hasText(value)) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return normalized.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}0-9\\s]", " ").replaceAll("\\s+", " ").trim();
    }

    private String normalizeSummaryPunctuation(String text) {
        if (!hasText(text)) {
            return null;
        }
        String normalized = text.trim().replaceAll("\\s+", " ");
        if (!normalized.endsWith(".") && !normalized.endsWith("!") && !normalized.endsWith("?")) {
            normalized = normalized + ".";
        }
        return normalized;
    }

    private AiEncryptedMessageSearchResponseDTO failure(String code, String message, AiMessageSearchScopeDTO scope) {
        AiEncryptedMessageSearchResponseDTO response = new AiEncryptedMessageSearchResponseDTO();
        response.setSuccess(false);
        response.setCodigo(code);
        response.setMensaje(message);
        applyScopeMetadata(response, scope);
        response.setResultados(List.of());
        return response;
    }

    private void applyScopeMetadata(AiEncryptedMessageSearchResponseDTO response, AiMessageSearchScopeDTO scope) {
        AiMessageSearchScopeType type = publicScopeType(scope);
        response.setTipoScopeAplicado(type.name());
        response.setNombreScopeAplicado(scope == null ? null : scope.getNombreScopeAplicado());
        response.setScopeResuelto(scope != null && scope.isScopeResuelto());
        response.setMotivoScope(scope == null ? "No se encontro un chat concreto con suficiente confianza, se aplico busqueda global." : scope.getMotivo());
        response.setConfidenceScope(scope == null || scope.getConfidence() == null ? 0 : scope.getConfidence());
    }

    private boolean shouldUseDirectResolution(AiMessageSearchScopeDTO scope, SearchIntent intent) {
        if (intent == null || !intent.directResolution()) {
            return false;
        }
        if (scope == null) {
            return true;
        }
        if (scope.isIntencionGrupoDetectada() && !scope.isScopeResuelto()) {
            return false;
        }
        if (scope.isIntencionIndividualDetectada() && !scope.isScopeResuelto()) {
            return false;
        }
        return true;
    }

    private AiMessageSearchScopeType effectiveScopeType(AiMessageSearchScopeDTO scope) {
        return scope == null || scope.getTipoScope() == null ? AiMessageSearchScopeType.GLOBAL : scope.getTipoScope();
    }

    private AiMessageSearchScopeType publicScopeType(AiMessageSearchScopeDTO scope) {
        AiMessageSearchScopeType type = effectiveScopeType(scope);
        return type == AiMessageSearchScopeType.GLOBAL_GRUPOS ? AiMessageSearchScopeType.GLOBAL : type;
    }

    private boolean needsScopeFallback(AiMessageSearchScopeDTO scope, List<AiEncryptedMessageSearchResultDTO> resultados) {
        if (scope == null) {
            return false;
        }
        AiMessageSearchScopeType type = scope.getTipoScope();
        if (type != AiMessageSearchScopeType.INDIVIDUAL && type != AiMessageSearchScopeType.GRUPO) {
            return false;
        }
        if (resultados == null || resultados.isEmpty()) {
            return true;
        }
        return resultados.stream()
                .map(AiEncryptedMessageSearchResultDTO::getRelevancia)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0) < 60;
    }

    private boolean needsSenderFallback(SenderResolution senderResolution,
                                        List<AiEncryptedMessageSearchResultDTO> resultados) {
        if (senderResolution == null) {
            return false;
        }
        if (senderResolution.senderScope() == AiMessageSearchSenderScope.AUTHENTICATED_USER
                || senderResolution.senderScope() == AiMessageSearchSenderScope.ANY_PARTICIPANT) {
            return false;
        }
        return resultados == null || resultados.isEmpty();
    }

    private AiMessageSearchScopeDTO buildGlobalFallbackScope(AiMessageSearchScopeDTO originalScope) {
        AiMessageSearchScopeDTO globalScope = new AiMessageSearchScopeDTO();
        globalScope.setTipoScope(AiMessageSearchScopeType.GLOBAL);
        globalScope.setScopeResuelto(false);
        globalScope.setNombreScopeAplicado(null);
        globalScope.setMotivo("Fallback global: no se encontro coincidencia suficiente en el scope inicial.");
        globalScope.setConfidence(0);
        globalScope.setIntencionGrupoDetectada(originalScope != null && originalScope.isIntencionGrupoDetectada());
        globalScope.setIntencionIndividualDetectada(originalScope != null && originalScope.isIntencionIndividualDetectada());
        return globalScope;
    }

    private int getMaxRelevancia(List<AiEncryptedMessageSearchResultDTO> resultados) {
        if (resultados == null || resultados.isEmpty()) {
            return 0;
        }
        return resultados.stream()
                .map(AiEncryptedMessageSearchResultDTO::getRelevancia)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
    }

    private List<AiEncryptedMessageSearchResultDTO> enrichScopeFallbackMotivo(List<AiEncryptedMessageSearchResultDTO> resultados,
                                                                               String nombreScopeInicial) {
        if (resultados == null) {
            return List.of();
        }
        List<AiEncryptedMessageSearchResultDTO> enriched = new ArrayList<>(resultados.size());
        for (AiEncryptedMessageSearchResultDTO r : resultados) {
            String chatNombre = hasText(r.getNombreReceptor()) ? r.getNombreReceptor()
                    : hasText(r.getNombreChatGrupal()) ? r.getNombreChatGrupal() : "otro chat";
            String prefix = "Se busco primero en el chat con " + nombreScopeInicial
                    + ", pero la mejor coincidencia aparecio en el chat con " + chatNombre + ". ";
            r.setMotivoCoincidencia(truncate(prefix + (r.getMotivoCoincidencia() == null ? "" : r.getMotivoCoincidencia()), MAX_REASON_LENGTH));
            enriched.add(r);
        }
        return enriched;
    }

    private record ValidationValues(boolean valid,
                                    String errorMessage,
                                    String consulta,
                                    int maxResultados,
                                    int maxMensajesAnalizar,
                                    LocalDateTime fechaInicio,
                                    LocalDateTime fechaFin,
                                    boolean incluirGrupales,
                                    boolean incluirIndividuales,
                                    AiMessageSearchNaturalQueryAnalysis analysis,
                                    boolean rangoTemporalDetectado,
                                    String descripcionRangoTemporal,
                                    Integer confidenceTemporal) {

        private static ValidationValues valid(String consulta,
                                              int maxResultados,
                                              int maxMensajesAnalizar,
                                              LocalDateTime fechaInicio,
                                              LocalDateTime fechaFin,
                                              boolean incluirGrupales,
                                              boolean incluirIndividuales,
                                              AiMessageSearchNaturalQueryAnalysis analysis,
                                              boolean rangoTemporalDetectado,
                                              String descripcionRangoTemporal,
                                              Integer confidenceTemporal) {
            return new ValidationValues(true, null, consulta, maxResultados, maxMensajesAnalizar, fechaInicio, fechaFin, incluirGrupales, incluirIndividuales, analysis, rangoTemporalDetectado, descripcionRangoTemporal, confidenceTemporal);
        }

        private static ValidationValues invalid(String message) {
            return new ValidationValues(false, message, null, 0, 0, null, null, false, false, null, false, null, null);
        }

        private ValidationValues withoutTemporalRange() {
            return new ValidationValues(valid, errorMessage, consulta, maxResultados, maxMensajesAnalizar, null, null, incluirGrupales, incluirIndividuales, analysis, false, null, confidenceTemporal);
        }

        private ValidationValues withTemporalRange(LocalDateTime inicio, LocalDateTime fin, String descripcion) {
            return new ValidationValues(valid, errorMessage, consulta, maxResultados, maxMensajesAnalizar, inicio, fin, incluirGrupales, incluirIndividuales, analysis, true, descripcion, confidenceTemporal);
        }

        private ValidationValues withIncluir(boolean newIncluirGrupales, boolean newIncluirIndividuales) {
            return new ValidationValues(valid, errorMessage, consulta, maxResultados, maxMensajesAnalizar, fechaInicio, fechaFin, newIncluirGrupales, newIncluirIndividuales, analysis, rangoTemporalDetectado, descripcionRangoTemporal, confidenceTemporal);
        }
    }

    private record ScheduledQueryContext(String personaMencionada,
                                         String grupoMencionado,
                                         String temporalExpression,
                                         String scheduledStatus,
                                         String orden) {
    }

    private record ScheduledChatContext(Long chatId,
                                        String tipoChat,
                                        String nombreChat,
                                        String normalizedName) {
    }

    private enum ComplaintBranch {
        NONE,
        RECEIVED,
        CREATED
    }

    private record CandidateBatch(Map<Long, CandidateMessage> candidates,
                                  boolean prefilterApplied,
                                  int prefilterMatchCount) {
    }

    private record CandidateMessage(Long mensajeId,
                                    Long chatId,
                                    String tipoChat,
                                    Long emisorId,
                                    String nombreEmisor,
                                    Long receptorId,
                                    String nombreReceptor,
                                    Long chatGrupalId,
                                    String nombreChatGrupal,
                                    LocalDateTime fechaEnvio,
                                    String contenido,
                                    String contenidoVisible,
                                    String chatNombre,
                                    String tipoMensaje,
                                    String descripcionTipoMensaje,
                                    boolean esMultimedia,
                                    String mediaUrl,
                                    String mimeType,
                                    String nombreArchivo,
                                    String imageUrl,
                                    String imageMime,
                                    String imageNombre,
                                    Long stickerId,
                                    String contentKind,
                                    boolean audioTranscrito,
                                    MensajeEntity mensajeEntity) {
    }

    private record ChatScopeMetadata(Long chatId,
                                     String tipoChat,
                                     Long receptorId,
                                     String nombreReceptor,
                                     Long chatGrupalId,
                                     String nombreChatGrupal,
                                     String chatNombre) {
    }

    private record MediaMeta(String mediaUrl, String mimeType, String nombreArchivo) {
    }

    private record AudioPayloadDTO(byte[] bytes, String mimeType) {
    }

    private record AudioSearchContent(String contenido, boolean transcrito) {
    }

    private record ScoredAudioCandidate(CandidateMessage candidate, int score) {
    }

    private record SearchWindow(List<Long> chatIds,
                                boolean filtrarEmisores,
                                List<Long> emisorIds) {
    }

    private record RelatedUserCandidate(Long userId,
                                        String fullName,
                                        int score,
                                        Long individualChatId,
                                        Set<Long> sharedGroupChatIds) {
    }

    private record SenderResolution(AiMessageSearchSenderScope senderScope,
                                    String personaObjetivoNombre,
                                    String emisorObjetivoNombre,
                                    boolean busquedaEnMensajesDeOtroUsuario,
                                    List<RelatedUserCandidate> relatedUsers,
                                    List<Long> allowedEmitterIds) {
        private static SenderResolution authenticatedUser(String personaObjetivoNombre) {
            return authenticatedUser(personaObjetivoNombre, List.of());
        }

        private static SenderResolution authenticatedUser(String personaObjetivoNombre,
                                                          List<RelatedUserCandidate> relatedUsers) {
            return new SenderResolution(
                    AiMessageSearchSenderScope.AUTHENTICATED_USER,
                    personaObjetivoNombre,
                    null,
                    false,
                    relatedUsers == null ? List.of() : relatedUsers,
                    List.of()
            );
        }

        private static SenderResolution specificOther(String emisorObjetivoNombre,
                                                      List<RelatedUserCandidate> relatedUsers,
                                                      boolean resolvedToSpecific) {
            List<RelatedUserCandidate> safeUsers = relatedUsers == null ? List.of() : relatedUsers;
            AiMessageSearchSenderScope scope = resolvedToSpecific
                    ? AiMessageSearchSenderScope.SPECIFIC_OTHER_USER
                    : AiMessageSearchSenderScope.MULTIPLE_POSSIBLE_USERS;
            List<Long> emitterIds = new ArrayList<>();
            for (RelatedUserCandidate candidate : safeUsers) {
                if (candidate != null && candidate.userId() != null) {
                    emitterIds.add(candidate.userId());
                }
            }
            return new SenderResolution(scope, null, emisorObjetivoNombre, true, safeUsers, emitterIds);
        }

        private static SenderResolution multiplePossibleUsers(String emisorObjetivoNombre,
                                                              List<RelatedUserCandidate> relatedUsers) {
            return specificOther(emisorObjetivoNombre, relatedUsers, false);
        }

        private static SenderResolution anyParticipant() {
            return new SenderResolution(
                    AiMessageSearchSenderScope.ANY_PARTICIPANT,
                    null,
                    null,
                    true,
                    List.of(),
                    List.of()
            );
        }

        private static SenderResolution receivedMessages() {
            return new SenderResolution(
                    AiMessageSearchSenderScope.RECEIVED_MESSAGES,
                    null,
                    null,
                    true,
                    List.of(),
                    List.of()
            );
        }
    }

    private record ImageCompatibilityMeta(String imageUrl,
                                          String imageMime,
                                          String imageNombre,
                                          Long stickerId,
                                          String contentKind) {
        private static ImageCompatibilityMeta empty() {
            return new ImageCompatibilityMeta(null, null, null, null, null);
        }
    }

    private record SearchIntent(boolean directResolution,
                                SearchDirection direction,
                                String requestedType) {
        private static SearchIntent semantic(String requestedType) {
            return new SearchIntent(false, SearchDirection.LAST, requestedType);
        }
    }

    private enum SearchDirection {
        FIRST,
        LAST
    }
}
