package com.chat.chat.Service.AiService;

import com.chat.chat.Entity.ChatGrupalEntity;
import com.chat.chat.Entity.ChatIndividualEntity;
import com.chat.chat.Entity.UsuarioEntity;
import com.chat.chat.Repository.ChatGrupalRepository;
import com.chat.chat.Repository.ChatIndividualRepository;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiMessageSearchScopeResolverServiceImpl implements AiMessageSearchScopeResolverService {

    private static final Set<String> STOPWORDS = Set.of(
            "grupo", "chat", "el", "la", "los", "las", "de", "del", "con", "mi", "nuestro", "nuestra", "en", "al"
    );
    private static final Set<String> COMMON_CONTEXT_WORDS = Set.of(
            "playa", "manana", "hora", "sitio", "lugar", "quedar", "quedamos", "hoy", "ayer"
    );

    private static final Pattern GROUP_PATTERN = Pattern.compile(
            "(?:en\\s+el\\s+grupo|por\\s+el\\s+grupo|del\\s+grupo|grupo(?:\\s+llamado)?|chat\\s+grupal|en\\s+el\\s+chat\\s+grupal|en\\s+el\\s+chat\\s+de\\s+grupo|en\\s+mi\\s+grupo|en\\s+nuestro\\s+grupo)\\s+([\\p{L}0-9][\\p{L}0-9\\s'_-]{1,80})",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern PERSON_PATTERN = Pattern.compile(
            "(?:le\\s+dije\\s+a|le\\s+escrib[ii]\\s+a|hable\\s+con|en\\s+el\\s+chat\\s+con)\\s+([\\p{L}][\\p{L}\\s'_-]{1,80})",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private final ChatGrupalRepository chatGrupalRepository;
    private final ChatIndividualRepository chatIndividualRepository;

    public AiMessageSearchScopeResolverServiceImpl(ChatGrupalRepository chatGrupalRepository,
                                                    ChatIndividualRepository chatIndividualRepository) {
        this.chatGrupalRepository = chatGrupalRepository;
        this.chatIndividualRepository = chatIndividualRepository;
    }

    @Override
    public AiMessageSearchScopeDTO resolverScope(Long userId, String consulta, Boolean incluirGrupales, Boolean incluirIndividuales) {
        return resolverScope(userId, consulta, incluirGrupales, incluirIndividuales, null);
    }

    @Override
    public AiMessageSearchScopeDTO resolverScope(Long userId, String consulta, Boolean incluirGrupales, Boolean incluirIndividuales, AiMessageSearchNaturalQueryAnalysis analysis) {
        AiMessageSearchScopeDTO fallback = globalFallback();
        if (userId == null || !hasText(consulta)) {
            return fallback;
        }

        boolean allowGroups = incluirGrupales == null || incluirGrupales;
        boolean allowIndividuals = incluirIndividuales == null || incluirIndividuales;
        boolean localizationQuery = analysis != null && analysis.isIntencionLocalizacion();
        boolean explicitGroupIntent = analysis != null ? analysis.isIntencionGrupo() : hasExplicitGroupIntent(consulta);
        boolean explicitIndividualIntent = analysis != null ? analysis.isIntencionIndividual() && !explicitGroupIntent : hasExplicitIndividualIntent(consulta) && !explicitGroupIntent;

        if (allowGroups) {
            String detectedGroup = analysis != null ? analysis.getNombreGrupoDetectado() : detectGroupName(consulta);
            if (localizationQuery && !hasText(detectedGroup)) {
                explicitGroupIntent = false;
            }
            if (explicitGroupIntent || hasText(detectedGroup)) {
                AiMessageSearchScopeDTO resolvedGroup = resolveGroupScope(userId, detectedGroup);
                resolvedGroup.setIntencionGrupoDetectada(explicitGroupIntent);
                resolvedGroup.setIntencionIndividualDetectada(false);
                if (resolvedGroup.isScopeResuelto()) {
                    return resolvedGroup;
                }
                if (explicitGroupIntent) {
                    resolvedGroup.setTipoScope(AiMessageSearchScopeType.GLOBAL_GRUPOS);
                    resolvedGroup.setMotivo("La consulta indica explicitamente un grupo, pero no se encontro uno con suficiente confianza; se aplicara busqueda global priorizando chats grupales.");
                    return resolvedGroup;
                }
            }
        }

        if (allowIndividuals && !explicitGroupIntent) {
            String detectedPerson = analysis != null ? analysis.getNombrePersonaDetectado() : detectPersonName(consulta);
            if (!hasText(detectedPerson) && !localizationQuery) {
                detectedPerson = detectSimpleAName(consulta);
            }
            if (localizationQuery && !hasText(detectedPerson)) {
                explicitIndividualIntent = false;
            }
            if (hasText(detectedPerson)) {
                AiMessageSearchScopeDTO resolvedIndividual = resolveIndividualScope(userId, detectedPerson);
                resolvedIndividual.setIntencionGrupoDetectada(false);
                resolvedIndividual.setIntencionIndividualDetectada(explicitIndividualIntent);
                if (resolvedIndividual.isScopeResuelto()) {
                    return resolvedIndividual;
                }
            }
        }

        fallback.setIntencionGrupoDetectada(explicitGroupIntent);
        fallback.setIntencionIndividualDetectada(explicitIndividualIntent);

        return fallback;
    }

    private AiMessageSearchScopeDTO resolveGroupScope(Long userId, String detectedRaw) {
        List<ChatGrupalEntity> groups = chatGrupalRepository.findAllByUsuariosId(userId);
        if (groups == null || groups.isEmpty()) {
            return globalFallback();
        }

        String detectedNormalized = normalizeName(detectedRaw);
        if (!isDetectedNameValid(detectedNormalized)) {
            return globalFallback();
        }

        List<ScopeCandidate> candidates = new ArrayList<>();
        for (ChatGrupalEntity g : groups) {
            if (g == null || g.getId() == null || !g.isActivo()) {
                continue;
            }
            String name = g.getNombreGrupo();
            String normalized = normalizeName(name);
            if (!hasText(normalized)) {
                continue;
            }
            int score = scoreMatch(detectedNormalized, normalized);
            candidates.add(new ScopeCandidate(AiMessageSearchScopeType.GRUPO, g.getId(), null, name, score));
        }

        return resolveByThresholds(candidates, detectedRaw, detectedNormalized, true);
    }

    private AiMessageSearchScopeDTO resolveIndividualScope(Long userId, String detectedRaw) {
        List<ChatIndividualEntity> chats = chatIndividualRepository.findAllByUsuario1IdOrUsuario2Id(userId, userId);
        if (chats == null || chats.isEmpty()) {
            return globalFallback();
        }

        String detectedNormalized = normalizeName(detectedRaw);
        if (!isDetectedNameValid(detectedNormalized)) {
            return globalFallback();
        }

        List<ScopeCandidate> candidates = new ArrayList<>();
        for (ChatIndividualEntity ci : chats) {
            if (ci == null || ci.getId() == null || ci.isAdminDirect()) {
                continue;
            }
            UsuarioEntity other = resolveOther(ci, userId);
            if (other == null || other.getId() == null || !other.isActivo()) {
                continue;
            }
            String fullName = buildDisplayName(other);
            String normalizedFull = normalizeName(fullName);
            String normalizedNombre = normalizeName(other.getNombre());
            String normalizedApellido = normalizeName(other.getApellido());

            int best = 0;
            best = Math.max(best, scoreMatch(detectedNormalized, normalizedFull));
            best = Math.max(best, scoreMatch(detectedNormalized, normalizedNombre));
            best = Math.max(best, scoreMatch(detectedNormalized, normalizedApellido));

            candidates.add(new ScopeCandidate(
                    AiMessageSearchScopeType.INDIVIDUAL,
                    ci.getId(),
                    other.getId(),
                    fullName,
                    best
            ));
        }

        AiMessageSearchScopeDTO resolved = resolveByThresholds(candidates, detectedRaw, detectedNormalized, false);
        if (!resolved.isScopeResuelto()) {
            return resolved;
        }

        int topScore = resolved.getConfidence() == null ? 0 : resolved.getConfidence();
        long similarCount = candidates.stream()
                .filter(c -> c.score() >= Math.max(75, topScore - 8))
                .count();
        if (similarCount > 1 && tokenCount(detectedNormalized) <= 1) {
            return globalFallback();
        }
        return resolved;
    }

    private AiMessageSearchScopeDTO resolveByThresholds(List<ScopeCandidate> candidates,
                                                        String detectedRaw,
                                                        String detectedNormalized,
                                                        boolean groupMode) {
        if (candidates == null || candidates.isEmpty()) {
            return globalFallback();
        }
        candidates.sort(Comparator.comparingInt(ScopeCandidate::score).reversed());
        ScopeCandidate best = candidates.get(0);
        ScopeCandidate second = candidates.size() > 1 ? candidates.get(1) : null;
        int delta = second == null ? 100 : best.score() - second.score();

        boolean accepted;
        if (groupMode) {
            accepted = best.score() >= 85 || (best.score() >= 75 && best.score() <= 84 && delta >= 10);
        } else {
            accepted = best.score() >= 85;
        }

        if (!accepted) {
            return globalFallback();
        }

        AiMessageSearchScopeDTO out = new AiMessageSearchScopeDTO();
        out.setTipoScope(best.type());
        out.setChatId(best.chatId());
        out.setChatGrupalId(best.type() == AiMessageSearchScopeType.GRUPO ? best.chatId() : null);
        out.setUsuarioRelacionadoId(best.relatedUserId());
        out.setNombreDetectado(detectedRaw);
        out.setNombreNormalizadoDetectado(detectedNormalized);
        out.setNombreScopeAplicado(best.displayName());
        out.setScopeResuelto(true);
        out.setConfidence(best.score());
        if (best.type() == AiMessageSearchScopeType.GRUPO) {
            out.setMotivo("Se detecto una referencia aproximada al grupo " + best.displayName() + " en la consulta.");
        } else {
            out.setMotivo("Se detecto una referencia al chat individual con " + best.displayName() + " en la consulta.");
        }
        return out;
    }

    private String detectGroupName(String consulta) {
        Matcher m = GROUP_PATTERN.matcher(consulta);
        if (m.find()) {
            return cleanupExtractedName(m.group(1));
        }
        return null;
    }

    private String detectPersonName(String consulta) {
        Matcher m = PERSON_PATTERN.matcher(consulta);
        if (m.find()) {
            return cleanupExtractedName(m.group(1));
        }
        return null;
    }

    private String detectSimpleAName(String consulta) {
        String normalized = consulta == null ? "" : consulta.trim();
        Pattern p = Pattern.compile("\\ba\\s+([\\p{L}][\\p{L}]{2,30}(?:\\s+[\\p{L}][\\p{L}]{2,30})?)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        Matcher m = p.matcher(normalized);
        if (m.find()) {
            return cleanupExtractedName(m.group(1));
        }
        return null;
    }

    private String cleanupExtractedName(String value) {
        if (!hasText(value)) {
            return null;
        }
        String v = value.trim();
        v = v.replaceAll("[.,;:!?]+$", "").trim();
        String[] stopAt = {" dije", " escribi", " hable", " propuse", " comente"};
        String lower = v.toLowerCase(Locale.ROOT);
        for (String s : stopAt) {
            int idx = lower.indexOf(s);
            if (idx > 0) {
                v = v.substring(0, idx).trim();
                break;
            }
        }
        return v;
    }

    private int scoreMatch(String detected, String candidate) {
        if (!hasText(detected) || !hasText(candidate)) {
            return 0;
        }
        if (detected.equals(candidate)) {
            return 100;
        }
        if (candidate.contains(detected) || detected.contains(candidate)) {
            int base = 88;
            if (tokenCount(detected) >= 2) {
                base = 92;
            }
            return base;
        }
        return calcularSimilitud(detected, candidate);
    }

    private int calcularSimilitud(String a, String b) {
        if (!hasText(a) || !hasText(b)) {
            return 0;
        }
        if (a.equals(b)) {
            return 100;
        }

        int lev = levenshtein(a, b);
        int maxLen = Math.max(a.length(), b.length());
        int levScore = maxLen == 0 ? 100 : (int) Math.round((1.0 - ((double) lev / maxLen)) * 100.0);

        Set<String> tokensA = tokenSet(a);
        Set<String> tokensB = tokenSet(b);
        int tokenScore = tokenSimilarity(tokensA, tokensB);

        int blend = (int) Math.round((levScore * 0.55) + (tokenScore * 0.45));
        return Math.max(0, Math.min(100, blend));
    }

    private int tokenSimilarity(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        int matched = 0;
        for (String ta : a) {
            int best = 0;
            for (String tb : b) {
                if (ta.equals(tb)) {
                    best = 100;
                    break;
                }
                int local = tokenLevenshteinScore(ta, tb);
                if (local > best) {
                    best = local;
                }
            }
            if (best >= 70) {
                matched++;
            }
        }
        return (int) Math.round(((double) matched / Math.max(a.size(), b.size())) * 100.0);
    }

    private int tokenLevenshteinScore(String a, String b) {
        int d = levenshtein(a, b);
        int maxLen = Math.max(a.length(), b.length());
        if (maxLen == 0) {
            return 100;
        }
        return (int) Math.round((1.0 - ((double) d / maxLen)) * 100.0);
    }

    private int levenshtein(String s1, String s2) {
        int n = s1.length();
        int m = s2.length();
        int[][] dp = new int[n + 1][m + 1];
        for (int i = 0; i <= n; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= m; j++) {
            dp[0][j] = j;
        }
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }
        return dp[n][m];
    }

    private String normalizeName(String text) {
        if (!hasText(text)) {
            return null;
        }
        String lower = text.toLowerCase(Locale.ROOT).trim();
        String noAccents = Normalizer.normalize(lower, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        String clean = noAccents.replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
        if (!hasText(clean)) {
            return null;
        }
        List<String> out = new ArrayList<>();
        for (String t : clean.split(" ")) {
            if (!hasText(t) || STOPWORDS.contains(t)) {
                continue;
            }
            out.add(t);
        }
        if (out.isEmpty()) {
            return clean;
        }
        return String.join(" ", out).trim();
    }

    private boolean isDetectedNameValid(String normalizedDetected) {
        if (!hasText(normalizedDetected) || normalizedDetected.length() < 3) {
            return false;
        }
        if (tokenCount(normalizedDetected) == 1 && COMMON_CONTEXT_WORDS.contains(normalizedDetected)) {
            return false;
        }
        return true;
    }

    private Set<String> tokenSet(String value) {
        Set<String> out = new HashSet<>();
        if (!hasText(value)) {
            return out;
        }
        for (String t : value.split(" ")) {
            if (hasText(t)) {
                out.add(t);
            }
        }
        return out;
    }

    private int tokenCount(String value) {
        return tokenSet(value).size();
    }

    private UsuarioEntity resolveOther(ChatIndividualEntity chat, Long userId) {
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

    private String buildDisplayName(UsuarioEntity user) {
        if (user == null) {
            return null;
        }
        String nombre = user.getNombre() == null ? "" : user.getNombre().trim();
        String apellido = user.getApellido() == null ? "" : user.getApellido().trim();
        String full = (nombre + " " + apellido).trim();
        if (hasText(full)) {
            return full;
        }
        return user.getEmail();
    }

    private AiMessageSearchScopeDTO globalFallback() {
        AiMessageSearchScopeDTO dto = new AiMessageSearchScopeDTO();
        dto.setTipoScope(AiMessageSearchScopeType.GLOBAL);
        dto.setScopeResuelto(false);
        dto.setConfidence(0);
        dto.setMotivo("No se encontro un chat concreto con suficiente confianza, se aplico busqueda global.");
        return dto;
    }

    private boolean hasExplicitGroupIntent(String consulta) {
        String normalized = normalizeIntentText(consulta);
        return hasText(normalized) && (
                normalized.contains("grupo")
                        || normalized.contains("chat grupal")
                        || normalized.contains("en el grupo")
                        || normalized.contains("por el grupo")
                        || normalized.contains("del grupo")
                        || normalized.contains("en el chat grupal")
                        || normalized.contains("en mi grupo")
                        || normalized.contains("en nuestro grupo")
        );
    }

    private boolean hasExplicitIndividualIntent(String consulta) {
        String normalized = normalizeIntentText(consulta);
        return hasText(normalized) && (
                normalized.contains("chat con")
                        || normalized.contains("le dije a")
                        || normalized.contains("le escribi a")
                        || normalized.contains("hable con")
                        || normalized.matches(".*\\ba\\s+[a-z0-9].*")
        );
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

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private record ScopeCandidate(AiMessageSearchScopeType type,
                                  Long chatId,
                                  Long relatedUserId,
                                  String displayName,
                                  int score) {
    }
}
