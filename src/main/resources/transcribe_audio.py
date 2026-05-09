#!/usr/bin/env python3
import json
import sys


def eprint(msg: str) -> None:
    sys.stderr.write(msg + "\n")


def main() -> int:
    if len(sys.argv) < 2:
        eprint("missing_audio_path")
        return 2

    audio_path = sys.argv[1]
    model_name = sys.argv[2] if len(sys.argv) > 2 and sys.argv[2] else "small"
    language = sys.argv[3] if len(sys.argv) > 3 and sys.argv[3] else "es"

    try:
        from faster_whisper import WhisperModel
    except Exception as ex:
        eprint(f"import_error:{type(ex).__name__}")
        return 3

    try:
        model = WhisperModel(model_name, device="cpu", compute_type="int8")
        segments, _info = model.transcribe(audio_path, language=language, vad_filter=True)
        parts = []
        for seg in segments:
            txt = (seg.text or "").strip()
            if txt:
                parts.append(txt)
        transcription = " ".join(parts).strip()
        print(json.dumps({"transcription": transcription}, ensure_ascii=False))
        return 0
    except Exception as ex:
        eprint(f"transcription_error:{type(ex).__name__}")
        return 4


if __name__ == "__main__":
    raise SystemExit(main())
