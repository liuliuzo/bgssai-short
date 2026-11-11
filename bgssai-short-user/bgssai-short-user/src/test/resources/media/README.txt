Generated test fixtures; no user content or licensed recordings.
probe-tone.mp3: ffmpeg lavfi sine 440Hz, 24000Hz sample rate, 1.25 seconds, libmp3lame 32k.
probe-tone.wav: same sine, PCM signed 16-bit little-endian.
probe-video.mp4: ffmpeg lavfi blue color, 64x96, 24fps, 1.25 seconds, libx264 yuv420p.
Tests use real ffprobe and expect the public integer duration to round up to 2 seconds.
