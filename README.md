# TejeChat Backend - Transcripcion Local de Audio (faster-whisper)

## Requisitos (Windows)
1. Instalar Python 3.10+ y validar:
```powershell
python --version
```
2. Instalar faster-whisper:
```powershell
pip install faster-whisper
```
3. Instalar ffmpeg y agregarlo al `PATH`.
4. Probar script local:
```powershell
python src/main/resources/transcribe_audio.py "C:/ruta/audio.webm" small es
```
5. Arrancar Spring Boot.

## Configuracion usada
```properties
ai.audio-transcription.enabled=true
ai.audio-transcription.provider=local-faster-whisper
ai.audio-transcription.model=small
ai.audio-transcription.language=es
ai.audio-transcription.python-command=python
ai.audio-transcription.script-path=src/main/resources/transcribe_audio.py
ai.audio-transcription.max-audio-size-bytes=10485760
ai.audio-transcription.timeout-ms=120000
```
