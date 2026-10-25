# Benchmark builds only. Class names are kept, and classes aren't merged, so a Baseline Profile
# recorded from this build names the real classes and methods; release builds then map them through
# R8 as usual. Shrinking stays on, so the code measured is otherwise close to the release code.
-dontobfuscate
-dontoptimize
