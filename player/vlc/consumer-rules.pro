# libVLC's native code finds its Java classes, fields and callbacks by name (JNI_OnLoad fails
# and playback crashes the app if R8 renames or removes them), and the AAR ships no rules of its own.
-keep class org.videolan.libvlc.** { *; }
