package com.chat.chat.Service.AiService;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DeterministicAiMessageSearchNaturalQueryAnalyzer implements AiMessageSearchNaturalQueryAnalyzer {

    private static final ZoneId ZONE_ID = ZoneId.of("Europe/Madrid");
    private static final Pattern GROUP_PATTERN = Pattern.compile("(?:en\\s+el\\s+grupo|grupo(?:\\s+llamado)?|al\\s+grupo|del\\s+grupo)\\s+([\\p{L}0-9][\\p{L}0-9\\s'_-]{1,80})", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern PERSON_PATTERN = Pattern.compile("(?:a|con)\\s+([\\p{L}][\\p{L}\\s'_-]{1,80})", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern OUTGOING_TARGET_PATTERN = Pattern.compile("(?:le\\s+dije\\s+a|le\\s+mande\\s+a|le\\s+mande\\s+un\\s+mensaje\\s+a|envie\\s+a|envi[eé]\\s+a|le\\s+escrib[ií]\\s+a|hable\\s+con|habl[eé]\\s+con)\\s+([\\p{L}][\\p{L}\\s'_-]{1,80})", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern INCOMING_SOURCE_PATTERN = Pattern.compile(
            "([\\p{L}][\\p{L}\\s'_-]{1,80})\\s+" +
            "(?:me\\s+dijo|me\\s+mand[oó]|me\\s+escribi[oó]|me\\s+llam[oó]|me\\s+insult[oó]|me\\s+amenaz[oó]|me\\s+envi[oó]|me\\s+pas[oó]|me\\s+coment[oó]|me\\s+respondi[oó]|me\\s+falt[oó]|dijo)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    // Orden inverso: "me mandó [NOMBRE]" / "me dijo [NOMBRE]"
    private static final Pattern INCOMING_SOURCE_REVERSE_PATTERN = Pattern.compile(
            "\\bme\\s+(?:dijo|mand[oó]|escribi[oó]|llam[oó]|insult[oó]|amenaz[oó]|envi[oó]|pas[oó]|coment[oó]|respondi[oó]|falt[oó])\\s+([\\p{L}][\\p{L}\\s'_-]{1,60})",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern COMPLAINT_TARGET_PATTERN = Pattern.compile(
            "(?:denunci[eé]\\s+a|report[eé]\\s+a|puse\\s+una\\s+denuncia\\s+(?:contra|a)|hice\\s+una\\s+denuncia\\s+(?:contra|a)|mi\\s+denuncia\\s+(?:contra|a))\\s+([\\p{L}][\\p{L}\\s'_-]{1,60})",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final String[] CONVERSATIONAL_PREFIXES = {
            "buenas ", "hola ", "oye ", "ey ", "ei ",
            "el otro dia ", "creo que ", "pues ", "mira ",
            "entonces ", "la verdad ", "ayer ", "hoy "
    };
    private static final Pattern YEAR_PATTERN = Pattern.compile("\\b(20\\d{2})\\b");
    private static final Pattern IN_MONTH_PATTERN = Pattern.compile("\\ben\\s+(enero|febrero|marzo|abril|mayo|junio|julio|agosto|septiembre|setiembre|octubre|noviembre|diciembre)(?:\\s+de\\s+(20\\d{2}))?\\b", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern RELATIVE_RANGE_PATTERN = Pattern.compile(
            "\\bultim(?:o|os|a|as)\\s+(\\d+|un|una|uno|dos|tres|cuatro|cinco|seis|siete|ocho|nueve|diez)\\s+(hora|horas|dia|dias|semana|semanas)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Map<String, Month> MONTHS = Map.ofEntries(
            Map.entry("enero", Month.JANUARY), Map.entry("febrero", Month.FEBRUARY), Map.entry("marzo", Month.MARCH), Map.entry("abril", Month.APRIL),
            Map.entry("mayo", Month.MAY), Map.entry("junio", Month.JUNE), Map.entry("julio", Month.JULY), Map.entry("agosto", Month.AUGUST),
            Map.entry("septiembre", Month.SEPTEMBER), Map.entry("setiembre", Month.SEPTEMBER), Map.entry("octubre", Month.OCTOBER),
            Map.entry("noviembre", Month.NOVEMBER), Map.entry("diciembre", Month.DECEMBER)
    );
    private static final Map<String, Integer> NUMBER_WORDS = Map.ofEntries(
            Map.entry("un", 1), Map.entry("una", 1), Map.entry("uno", 1), Map.entry("dos", 2),
            Map.entry("tres", 3), Map.entry("cuatro", 4), Map.entry("cinco", 5), Map.entry("seis", 6),
            Map.entry("siete", 7), Map.entry("ocho", 8), Map.entry("nueve", 9), Map.entry("diez", 10)
    );

    @Override
    public AiMessageSearchNaturalQueryAnalysis analyze(String consulta) {
        AiMessageSearchNaturalQueryAnalysis out = new AiMessageSearchNaturalQueryAnalysis();
        String normalized = normalizeIntentText(consulta);
        boolean intencionLocalizacion = esConsultaDeLocalizacion(normalized);
        boolean explicitGroupReference = hasExplicitGroupReference(normalized);
        boolean explicitIndividualReference = hasExplicitIndividualReference(normalized);
        boolean anyParticipantIntent = hasAnyParticipantIntent(normalized);
        String outgoingTarget = cleanupExtractedName(extractWithPattern(consulta, normalized, OUTGOING_TARGET_PATTERN, false));
        String incomingSourceRaw = extractWithPattern(consulta, normalized, INCOMING_SOURCE_PATTERN, false);
        if (incomingSourceRaw == null || incomingSourceRaw.isBlank()) {
            incomingSourceRaw = extractWithPattern(consulta, normalized, INCOMING_SOURCE_REVERSE_PATTERN, false);
        }
        String incomingSource = stripConversationalPrefixes(cleanupExtractedName(incomingSourceRaw));
        out.setConsultaNormalizada(normalized);
        out.setIntencionLocalizacion(intencionLocalizacion);
        out.setIntencionGrupo(explicitGroupReference);
        out.setIntencionIndividual(explicitIndividualReference);
        out.setIntencionAudio(esConsultaDeAudio(normalized));
        out.setIntencionImagen(containsAny(normalized, "imagen", "foto"));
        out.setIntencionSticker(containsAny(normalized, "sticker"));
        out.setIntencionArchivo(containsAny(normalized, "archivo", "file", "documento"));
        out.setIntencionUltimoMensaje(containsAny(normalized, "ultimo mensaje", "lo ultimo", "mas reciente"));
        out.setIntencionPrimerMensaje(containsAny(normalized, "primer mensaje", "lo primero", "mas antiguo"));
        out.setIntencionContenidoOfensivo(esConsultaContenidoOfensivo(normalized));
        out.setNombreGrupoDetectado(cleanupExtractedName(extractGroupName(consulta, normalized, explicitGroupReference)));
        out.setNombrePersonaDetectado(cleanupExtractedName(extractPersonName(consulta, normalized, explicitIndividualReference)));
        out.setPersonaObjetivoDetectada(outgoingTarget);
        out.setEmisorObjetivoDetectado(incomingSource);
        out.setSenderScope(resolveSenderScope(anyParticipantIntent, outgoingTarget, incomingSource));
        out.setIntencionDenunciaRecibida(esConsultaDenunciaRecibida(normalized));
        out.setIntencionDenunciaCreada(esConsultaDenunciaCreada(normalized));
        out.setPersonaDenunciadaSolicitada(detectarPersonaDenunciada(consulta, normalized));
        out.setMotivoDenunciaDetectado(detectarMotivoDenuncia(normalized));
        applyTemporalDetection(out, normalized);
        applyExpandedTemporalRange(out, normalized);
        return out;
    }

    private boolean esConsultaDeDenuncia(String normalized) {
        return containsAny(normalized,
                "denuncia", "reporte", "reportado", "reportaron",
                "sancion", "moderacion");
    }

    private boolean esConsultaDenunciaCreada(String normalized) {
        return containsAny(normalized,
                "puse una denuncia", "hice una denuncia",
                "denuncie a", "reporte a", "mi denuncia contra",
                "denuncia que puse", "denuncia que hice",
                "denuncias que puse", "denuncias que hice",
                "he denunciado", "habia denunciado",
                "denuncia que envie", "denuncia enviada");
    }

    private boolean esConsultaDenunciaRecibida(String normalized) {
        return containsAny(normalized,
                "me han denunciado", "me denunciaron", "me denuncio",
                "tengo una denuncia", "tengo denuncias",
                "denuncia contra mi", "denuncias contra mi",
                "me reportaron", "me han reportado",
                "alguien me denuncio", "han puesto una denuncia",
                "pusieron una denuncia", "han puesto denuncia",
                "me han puesto una denuncia", "me han puesto denuncia",
                "me han puesto alguna denuncia", "me pusieron una denuncia");
    }

    private boolean esConsultaContenidoOfensivo(String normalized) {
        boolean ofensivoBase = containsAny(normalized,
                "he insultado", "insulte a alguien", "insulte", "he mandado insultos",
                "mande insultos", "he enviado insultos", "envie insultos",
                "he faltado el respeto", "le falte el respeto", "faltado el respeto",
                "mensaje ofensivo", "contenido ofensivo", "mensaje insultando",
                "he amenazado", "amenace a alguien", "amenace", "mande amenazas",
                "envie amenazas", "he mandado amenazas");
        boolean terminoOfensivo = containsAny(normalized,
                "insult", "ofensiv", "amenaz", "faltar el respeto", "falto el respeto", "falte el respeto");
        boolean verboEnvio = containsAny(normalized,
                "he mandado", "mande", "he enviado", "envie", "he dicho", "dije", "mensaje", "mensajes");
        return ofensivoBase || (terminoOfensivo && verboEnvio);
    }

    private String detectarPersonaDenunciada(String consulta, String normalized) {
        if (consulta == null || normalized == null) return null;
        Matcher m = COMPLAINT_TARGET_PATTERN.matcher(consulta);
        if (m.find()) {
            return cleanupExtractedName(m.group(1));
        }
        return null;
    }

    private String detectarMotivoDenuncia(String normalized) {
        if (normalized == null) return null;
        if (containsAny(normalized, "insulto", "insultos", "insultando", "insultaron")) return "insult";
        if (containsAny(normalized, "amenaza", "amenazas", "amenazando", "amenazaron")) return "amenaz";
        if (containsAny(normalized, "acoso", "acosando", "acosaron", "acosador")) return "acoso";
        if (containsAny(normalized, "spam", "publicidad no deseada")) return "spam";
        if (containsAny(normalized, "contenido inapropiado", "contenido ofensivo")) return "inapropiado";
        if (containsAny(normalized, "fraude", "estafa", "engano")) return "fraude";
        return null;
    }

    private void applyExpandedTemporalRange(AiMessageSearchNaturalQueryAnalysis out, String normalized) {
        if (!out.isRangoTemporalDetectado() || out.getFechaInicioDetectada() == null) return;
        LocalDateTime now = LocalDateTime.now(ZONE_ID);
        LocalDateTime inicio = out.getFechaInicioDetectada();
        LocalDateTime fin = out.getFechaFinDetectada() != null ? out.getFechaFinDetectada() : now;
        long dias = java.time.temporal.ChronoUnit.DAYS.between(inicio.toLocalDate(), fin.toLocalDate());
        LocalDateTime expandedInicio;
        LocalDateTime expandedFin;
        String descripcionExpandida;
        if (dias <= 1) {
            expandedInicio = inicio.minusDays(3);
            expandedFin = fin.plusDays(3);
            descripcionExpandida = "ultimos 7 dias (expansion de: " + out.getDescripcionRangoTemporal() + ")";
        } else if (dias <= 7) {
            expandedInicio = inicio.minusWeeks(3);
            expandedFin = fin.plusDays(7);
            descripcionExpandida = "ultimas 4 semanas (expansion de: " + out.getDescripcionRangoTemporal() + ")";
        } else if (dias <= 31) {
            expandedInicio = inicio.minusMonths(2);
            expandedFin = fin.plusMonths(1);
            descripcionExpandida = "ultimos 3 meses (expansion de: " + out.getDescripcionRangoTemporal() + ")";
        } else {
            return;
        }
        out.setFechaInicioExpandida(expandedInicio);
        out.setFechaFinExpandida(expandedFin);
        out.setDescripcionRangoExpandido(descripcionExpandida);
    }

    private void applyTemporalDetection(AiMessageSearchNaturalQueryAnalysis out, String normalized) {
        LocalDateTime now = LocalDateTime.now(ZONE_ID);
        Matcher relativeRangeMatcher = RELATIVE_RANGE_PATTERN.matcher(normalized);
        if (relativeRangeMatcher.find()) {
            Integer amount = parseTemporalAmount(relativeRangeMatcher.group(1));
            String unit = relativeRangeMatcher.group(2).toLowerCase(Locale.ROOT);
            if (amount != null && amount > 0) {
                String description = "ultimos " + amount + " " + unit;
                if (unit.startsWith("hora")) {
                    applyRange(out, now.minusHours(amount), now, description, 92);
                } else if (unit.startsWith("dia")) {
                    applyRange(out, now.minusDays(amount), now, description, 92);
                } else if (unit.startsWith("semana")) {
                    applyRange(out, now.minusWeeks(amount), now, description, 92);
                }
                if (out.isRangoTemporalDetectado()) {
                    return;
                }
            }
        }
        if (containsAny(normalized, "hoy")) { applyRange(out, atStart(now.toLocalDate()), atEnd(now.toLocalDate()), "hoy", 95); return; }
        if (containsAny(normalized, "anteayer")) { LocalDate d = now.toLocalDate().minusDays(2); applyRange(out, atStart(d), atEnd(d), "anteayer", 95); return; }
        if (containsAny(normalized, "ayer")) { LocalDate d = now.toLocalDate().minusDays(1); applyRange(out, atStart(d), atEnd(d), "ayer", 95); return; }
        if (containsAny(normalized, "esta semana")) { LocalDate start = now.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)); applyRange(out, start.atStartOfDay(), atEnd(now.toLocalDate()), "esta semana", 90); applyDayPeriod(out, normalized); return; }
        if (containsAny(normalized, "semana pasada", "la semana pasada")) { LocalDate currentMonday = now.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)); LocalDate start = currentMonday.minusWeeks(1); LocalDate end = start.plusDays(6); applyRange(out, atStart(start), atEnd(end), "semana pasada", 92); applyDayPeriod(out, normalized); return; }
        if (containsAny(normalized, "hace unos dias", "hace unos días", "hace pocos dias", "hace pocos días")) { applyRange(out, now.minusDays(7), now, "ultimos 7 dias", 75); applyDayPeriod(out, normalized); return; }
        if (containsAny(normalized, "hace una semana")) { applyRange(out, now.minusDays(14), now.minusDays(4), "aproximadamente hace una semana", 70); applyDayPeriod(out, normalized); return; }
        if (containsAny(normalized, "hace dos semanas")) { applyRange(out, now.minusDays(24), now.minusDays(10), "aproximadamente hace dos semanas", 70); applyDayPeriod(out, normalized); return; }
        if (containsAny(normalized, "este mes")) { LocalDate start = now.toLocalDate().withDayOfMonth(1); applyRange(out, atStart(start), now, "este mes", 90); applyPartOfMonth(out, normalized); applyDayPeriod(out, normalized); return; }
        if (containsAny(normalized, "mes pasado", "el mes pasado")) { YearMonth ym = YearMonth.from(now.minusMonths(1)); applyRange(out, ym.atDay(1).atStartOfDay(), atEnd(ym.atEndOfMonth()), "mes pasado", 92); applyPartOfMonth(out, normalized); applyDayPeriod(out, normalized); return; }

        Matcher monthMatcher = IN_MONTH_PATTERN.matcher(normalized);
        if (monthMatcher.find()) {
            Month month = MONTHS.get(monthMatcher.group(1).toLowerCase(Locale.ROOT));
            Integer year = monthMatcher.group(2) == null ? null : Integer.parseInt(monthMatcher.group(2));
            int resolvedYear = year != null ? year : now.getYear();
            YearMonth ym = YearMonth.of(resolvedYear, month);
            applyRange(out, ym.atDay(1).atStartOfDay(), atEnd(ym.atEndOfMonth()), "en " + monthMatcher.group(1) + (year != null ? " de " + year : ""), 88);
            applyPartOfMonth(out, normalized);
            applyDayPeriod(out, normalized);
            return;
        }

        Matcher yearMatcher = YEAR_PATTERN.matcher(normalized);
        if (yearMatcher.find()) {
            int year = Integer.parseInt(yearMatcher.group(1));
            applyRange(out, LocalDate.of(year, 1, 1).atStartOfDay(), LocalDate.of(year, 12, 31).atTime(23, 59, 59), "en " + year, 80);
            return;
        }
    }

    private Integer parseTemporalAmount(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.chars().allMatch(Character::isDigit)) {
            try {
                return Integer.parseInt(normalized);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return NUMBER_WORDS.get(normalized);
    }

    private void applyPartOfMonth(AiMessageSearchNaturalQueryAnalysis out, String normalized) {
        if (!out.isRangoTemporalDetectado() || out.getFechaInicioDetectada() == null || out.getFechaFinDetectada() == null) return;
        LocalDate start = out.getFechaInicioDetectada().toLocalDate();
        LocalDate end = out.getFechaFinDetectada().toLocalDate();
        YearMonth ym = YearMonth.from(start);
        if (containsAny(normalized, "a principios de mes")) {
            out.setFechaInicioDetectada(ym.atDay(1).atStartOfDay());
            out.setFechaFinDetectada(ym.atDay(Math.min(10, ym.lengthOfMonth())).atTime(23, 59, 59));
        } else if (containsAny(normalized, "a mediados de mes")) {
            out.setFechaInicioDetectada(ym.atDay(11).atStartOfDay());
            out.setFechaFinDetectada(ym.atDay(Math.min(20, ym.lengthOfMonth())).atTime(23, 59, 59));
        } else if (containsAny(normalized, "a finales de mes")) {
            out.setFechaInicioDetectada(ym.atDay(Math.min(21, ym.lengthOfMonth())).atStartOfDay());
            out.setFechaFinDetectada(ym.atEndOfMonth().atTime(23, 59, 59));
        } else {
            out.setFechaInicioDetectada(start.atStartOfDay());
            out.setFechaFinDetectada(end.atTime(23, 59, 59));
        }
    }

    private void applyDayPeriod(AiMessageSearchNaturalQueryAnalysis out, String normalized) {
        if (!out.isRangoTemporalDetectado() || out.getFechaInicioDetectada() == null || out.getFechaFinDetectada() == null) return;
        if (containsAny(normalized, "por la manana", "por la mañana")) {
            out.setFechaInicioDetectada(out.getFechaInicioDetectada().toLocalDate().atTime(6, 0));
            out.setFechaFinDetectada(out.getFechaFinDetectada().toLocalDate().atTime(12, 0));
        } else if (containsAny(normalized, "por la tarde")) {
            out.setFechaInicioDetectada(out.getFechaInicioDetectada().toLocalDate().atTime(12, 0));
            out.setFechaFinDetectada(out.getFechaFinDetectada().toLocalDate().atTime(20, 0));
        } else if (containsAny(normalized, "por la noche")) {
            out.setFechaInicioDetectada(out.getFechaInicioDetectada().toLocalDate().atTime(20, 0));
            out.setFechaFinDetectada(out.getFechaFinDetectada().toLocalDate().atTime(23, 59, 59));
        }
    }

    private void applyRange(AiMessageSearchNaturalQueryAnalysis out, LocalDateTime start, LocalDateTime end, String description, int confidence) {
        out.setRangoTemporalDetectado(true);
        out.setFechaInicioDetectada(start);
        out.setFechaFinDetectada(end);
        out.setDescripcionRangoTemporal(description);
        out.setConfidenceTemporal(confidence);
    }

    private String extractGroupName(String consulta, String normalized, boolean explicitGroupReference) {
        if (!explicitGroupReference || esConsultaDeLocalizacion(normalized)) {
            return null;
        }
        Matcher m = GROUP_PATTERN.matcher(consulta == null ? "" : consulta);
        return m.find() ? m.group(1) : null;
    }

    private String extractPersonName(String consulta, String normalized, boolean explicitIndividualReference) {
        if (!explicitIndividualReference || esConsultaDeLocalizacion(normalized)) {
            return null;
        }
        Matcher m = PERSON_PATTERN.matcher(consulta == null ? "" : consulta);
        return m.find() ? m.group(1) : null;
    }

    private String extractWithPattern(String consulta, String normalized, Pattern pattern, boolean disableForLocalization) {
        if (disableForLocalization && esConsultaDeLocalizacion(normalized)) {
            return null;
        }
        Matcher matcher = pattern.matcher(consulta == null ? "" : consulta);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String cleanupExtractedName(String value) {
        if (value == null || value.isBlank()) return null;
        String v = value.trim().replaceAll("[.,;:!?]+$", "").trim();
        String lower = v.toLowerCase(Locale.ROOT);
        for (String stop : new String[]{" sobre ", " que ", " donde ", " en "}) {
            int idx = lower.indexOf(stop);
            if (idx > 0) { v = v.substring(0, idx).trim(); break; }
        }
        return v;
    }

    private boolean esConsultaDeAudio(String normalized) {
        return containsAny(normalized, "audio", "nota de voz", "mensaje de voz", "voz", "escuchar", "donde dije en un audio", "audio donde dije", "audio en el que dije");
    }

    private boolean esConsultaDeLocalizacion(String normalized) {
        return containsAny(normalized,
                "en que grupo",
                "en que chat",
                "donde fue",
                "donde lo dije",
                "donde lo mande");
    }

    private boolean hasExplicitGroupReference(String normalized) {
        return containsAny(normalized,
                "en el grupo",
                "grupo llamado",
                "al grupo",
                "del grupo",
                "chat grupal",
                "en mi grupo",
                "en nuestro grupo");
    }

    private boolean hasExplicitIndividualReference(String normalized) {
        return containsAny(normalized,
                "le dije a",
                "le escribi a",
                "le mande a",
                "le mande un mensaje a",
                "envie a",
                "hable con",
                "en el chat con",
                "me dijo",
                "me mando",
                "me escribio",
                "me llamo",
                "me insulto",
                "me amenazo",
                "me envio",
                "me paso",
                "me comento",
                "me respondio",
                "me falto");
    }

    private boolean hasAnyParticipantIntent(String normalized) {
        return containsAny(normalized,
                "alguien dijo",
                "quien dijo",
                "se dijo");
    }

    private AiMessageSearchSenderScope resolveSenderScope(boolean anyParticipantIntent,
                                                          String outgoingTarget,
                                                          String incomingSource) {
        if (anyParticipantIntent) {
            return AiMessageSearchSenderScope.ANY_PARTICIPANT;
        }
        if (incomingSource != null && !incomingSource.isBlank()) {
            return AiMessageSearchSenderScope.SPECIFIC_OTHER_USER;
        }
        if (outgoingTarget != null && !outgoingTarget.isBlank()) {
            return AiMessageSearchSenderScope.AUTHENTICATED_USER;
        }
        return AiMessageSearchSenderScope.AUTHENTICATED_USER;
    }

    private String stripConversationalPrefixes(String rawName) {
        if (rawName == null || rawName.isBlank()) return rawName;
        String norm = normalizeIntentText(rawName);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (String prefix : CONVERSATIONAL_PREFIXES) {
                String normPrefix = normalizeIntentText(prefix);
                if (norm.startsWith(normPrefix)) {
                    norm = norm.substring(normPrefix.length()).stripLeading();
                    changed = true;
                    break;
                }
            }
        }
        if (norm.isEmpty()) return null;
        String normFull = normalizeIntentText(rawName);
        int strippedChars = normFull.length() - norm.length();
        if (strippedChars <= 0) return rawName.trim();
        String strippedPart = normFull.substring(0, strippedChars).trim();
        if (strippedPart.isEmpty()) return rawName.trim();
        int wordCount = strippedPart.split("\\s+").length;
        String[] rawWords = rawName.trim().split("\\s+");
        if (wordCount >= rawWords.length) return null;
        return String.join(" ", Arrays.copyOfRange(rawWords, wordCount, rawWords.length)).trim();
    }

    private String normalizeIntentText(String text) {
        if (text == null || text.isBlank()) return "";
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        normalized = normalized.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\s]", " ");
        return normalized.replaceAll("\\s+", " ").trim();
    }

    private boolean containsAny(String text, String... patterns) {
        if (text == null || text.isBlank()) return false;
        for (String pattern : patterns) if (pattern != null && !pattern.isBlank() && text.contains(normalizeIntentText(pattern))) return true;
        return false;
    }

    private LocalDateTime atStart(LocalDate d) { return d.atStartOfDay(); }
    private LocalDateTime atEnd(LocalDate d) { return d.atTime(23, 59, 59); }
}
