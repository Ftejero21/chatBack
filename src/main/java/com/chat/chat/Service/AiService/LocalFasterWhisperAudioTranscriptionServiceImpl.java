package com.chat.chat.Service.AiService;

import com.chat.chat.Configuracion.AiProperties;
import com.chat.chat.DTO.AudioTranscriptionResultDTO;
import com.chat.chat.Entity.MensajeEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class LocalFasterWhisperAudioTranscriptionServiceImpl implements AudioTranscriptionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalFasterWhisperAudioTranscriptionServiceImpl.class);
    private static final Set<String> ALLOWED_MIMES = Set.of(
            "audio/webm", "audio/mpeg", "audio/mp3", "audio/wav", "audio/ogg", "audio/mp4"
    );

    private final AiProperties aiProperties;

    public LocalFasterWhisperAudioTranscriptionServiceImpl(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
    }

    @Override
    public AudioTranscriptionResultDTO transcribirAudio(MensajeEntity mensaje, byte[] audioBytes, String mimeType) {
        AiProperties.AudioTranscription cfg = aiProperties.getAudioTranscription();
        if (!cfg.isEnabled()) {
            return fail("AI_AUDIO_TRANSCRIPTION_DISABLED", "transcripcion deshabilitada", mensaje);
        }
        if (!"local-faster-whisper".equalsIgnoreCase(cfg.getProvider())) {
            return fail("AI_AUDIO_TRANSCRIPTION_DISABLED", "provider no soportado", mensaje);
        }
        String normalizedMime = normalizeMime(mimeType);
        if (!ALLOWED_MIMES.contains(normalizedMime)) {
            return fail("AI_AUDIO_INVALID_MIME", "formato no permitido", mensaje);
        }
        if (audioBytes == null || audioBytes.length == 0) {
            return fail("AI_AUDIO_TRANSCRIPTION_ERROR", "audio vacio", mensaje);
        }
        if (audioBytes.length > cfg.getMaxAudioSizeBytes()) {
            return fail("AI_AUDIO_TOO_LARGE", "supera tamano maximo", mensaje);
        }

        Path tempFile = null;
        Path tempScript = null;
        Process process = null;
        try {
            String ext = extensionByMime(normalizedMime);
            tempFile = Files.createTempFile("tejechat-audio-", ext);
            Files.write(tempFile, audioBytes);

            ScriptResolution scriptResolution = resolveScriptPath(cfg.getScriptPath());
            if (scriptResolution == null || scriptResolution.path() == null) {
                return fail("AI_AUDIO_TRANSCRIPTION_SCRIPT_NOT_FOUND", "script no encontrado", mensaje);
            }
            Path script = scriptResolution.path();
            tempScript = scriptResolution.temporal() ? script : null;

            List<String> cmd = new ArrayList<>();
            cmd.add(cfg.getPythonCommand());
            cmd.add(script.toString());
            cmd.add(tempFile.toString());
            cmd.add(cfg.getModel());
            cmd.add(cfg.getLanguage());
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(false);
            injectFfmpegPath(pb, cfg.getFfmpegBinPath());
            process = pb.start();

            boolean finished = process.waitFor(cfg.getTimeoutMs(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                return fail("AI_AUDIO_TRANSCRIPTION_TIMEOUT", "timeout de transcripcion", mensaje);
            }

            String stdout = readAll(process.getInputStream());
            String stderr = readAll(process.getErrorStream());
            int exit = process.exitValue();
            if (exit != 0) {
                LOGGER.warn("[AI][AUDIO_TRANSCRIPTION] script_error exit={} errClass={} errToken={}",
                        exit, classifyErr(stderr), extractErrToken(stderr));
                return fail("AI_AUDIO_TRANSCRIPTION_ERROR", "error al procesar audio", mensaje);
            }

            String transcription = normalizeOutput(stdout);
            if (transcription == null || transcription.isBlank()) {
                return fail("AI_AUDIO_TRANSCRIPTION_ERROR", "error al procesar audio", mensaje);
            }

            AudioTranscriptionResultDTO ok = new AudioTranscriptionResultDTO();
            ok.setSuccess(true);
            ok.setCodigo("OK");
            ok.setMensaje("ok");
            ok.setTranscripcion(transcription);
            ok.setModelo(cfg.getModel());
            ok.setDuracionMs(mensaje == null ? null : mensaje.getMediaDuracionMs());
            return ok;
        } catch (IOException ex) {
            return fail("AI_AUDIO_TEMP_FILE_ERROR", "error temporal de archivo", mensaje);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return fail("AI_AUDIO_TRANSCRIPTION_TIMEOUT", "timeout de transcripcion", mensaje);
        } catch (RuntimeException ex) {
            return fail("AI_AUDIO_TRANSCRIPTION_ERROR", "error al procesar audio", mensaje);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
            if (tempScript != null) {
                try {
                    Files.deleteIfExists(tempScript);
                } catch (IOException ex) {
                    LOGGER.warn("[AI][AUDIO_TRANSCRIPTION] temp_script_delete_failed errClass={}", ex.getClass().getSimpleName());
                }
            }
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ex) {
                    LOGGER.warn("[AI][AUDIO_TRANSCRIPTION] temp_delete_failed errClass={}", ex.getClass().getSimpleName());
                }
            }
        }
    }

    private String normalizeMime(String mimeType) {
        if (mimeType == null) return "";
        String normalized = mimeType.trim().toLowerCase(Locale.ROOT);
        if ("audio/mp3".equals(normalized)) return "audio/mpeg";
        return normalized;
    }

    private String extensionByMime(String mime) {
        return switch (mime) {
            case "audio/webm" -> ".webm";
            case "audio/mpeg", "audio/mp3" -> ".mp3";
            case "audio/wav" -> ".wav";
            case "audio/ogg" -> ".ogg";
            case "audio/mp4" -> ".m4a";
            default -> ".bin";
        };
    }

    private String readAll(java.io.InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (sb.length() > 0) sb.append('\n');
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private String normalizeOutput(String out) {
        if (out == null) return null;
        String trimmed = out.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            int idx = trimmed.indexOf("\"transcription\"");
            if (idx >= 0) {
                int colon = trimmed.indexOf(':', idx);
                if (colon > 0) {
                    String tail = trimmed.substring(colon + 1).trim();
                    if (tail.startsWith("\"")) {
                        int end = tail.lastIndexOf('"');
                        if (end > 0) {
                            return tail.substring(1, end).replace("\\n", " ").replace("\\\"", "\"").trim();
                        }
                    }
                }
            }
        }
        return trimmed;
    }

    private String classifyErr(String stderr) {
        if (stderr == null || stderr.isBlank()) return "NONE";
        String s = stderr.toLowerCase(Locale.ROOT);
        if (s.contains("ffmpeg")) return "FFMPEG";
        if (s.contains("faster_whisper") || s.contains("whisper")) return "WHISPER";
        if (s.contains("localentrynotfounderror")) return "HF_MODEL_NOT_FOUND";
        if (s.contains("filenotfounderror")) return "FILE_NOT_FOUND";
        if (s.contains("permissionerror")) return "PERMISSION";
        if (s.contains("valueerror")) return "VALUE";
        if (s.contains("modulenotfounderror")) return "PY_MODULE";
        return "GENERIC";
    }

    private String extractErrToken(String stderr) {
        if (stderr == null || stderr.isBlank()) return "none";
        String trimmed = stderr.trim();
        int nl = trimmed.indexOf('\n');
        if (nl > 0) trimmed = trimmed.substring(0, nl).trim();
        if (trimmed.length() > 120) trimmed = trimmed.substring(0, 120);
        return trimmed;
    }

    private AudioTranscriptionResultDTO fail(String code, String msg, MensajeEntity mensaje) {
        AudioTranscriptionResultDTO dto = new AudioTranscriptionResultDTO();
        dto.setSuccess(false);
        dto.setCodigo(code);
        dto.setMensaje(msg);
        dto.setTranscripcion(null);
        dto.setModelo(aiProperties.getAudioTranscription().getModel());
        dto.setDuracionMs(mensaje == null ? null : mensaje.getMediaDuracionMs());
        return dto;
    }

    private void injectFfmpegPath(ProcessBuilder pb, String ffmpegBinPath) {
        if (ffmpegBinPath == null || ffmpegBinPath.isBlank()) return;
        try {
            Path p = Paths.get(ffmpegBinPath).toAbsolutePath().normalize();
            if (!Files.exists(p) || !Files.isDirectory(p)) return;
            String currentPath = pb.environment().getOrDefault("PATH", "");
            String sep = System.getProperty("path.separator", ";");
            if (!currentPath.contains(p.toString())) {
                pb.environment().put("PATH", p + sep + currentPath);
            }
        } catch (Exception ignored) {
            // no-op
        }
    }

    private ScriptResolution resolveScriptPath(String configuredPath) throws IOException {
        if (configuredPath == null || configuredPath.isBlank()) {
            return null;
        }
        Path direct = Path.of(configuredPath).toAbsolutePath().normalize();
        if (Files.exists(direct) && Files.isRegularFile(direct)) {
            return new ScriptResolution(direct, false);
        }
        Path cwdResolved = Path.of(System.getProperty("user.dir", ".")).resolve(configuredPath).normalize();
        if (Files.exists(cwdResolved) && Files.isRegularFile(cwdResolved)) {
            return new ScriptResolution(cwdResolved, false);
        }

        String cpPath = configuredPath.replace("\\", "/");
        if (cpPath.startsWith("/")) cpPath = cpPath.substring(1);
        if (cpPath.startsWith("src/main/resources/")) cpPath = cpPath.substring("src/main/resources/".length());
        ClassPathResource resource = new ClassPathResource(cpPath);
        if (resource.exists()) {
            Path tmp = Files.createTempFile("tejechat-transcribe-script-", ".py");
            Files.write(tmp, resource.getInputStream().readAllBytes());
            return new ScriptResolution(tmp, true);
        }
        return null;
    }

    private record ScriptResolution(Path path, boolean temporal) {
    }
}
