sed -i '1d;2d' app/src/main/java/com/example/MainActivity.kt
sed -i '1s/^/package com.example\n\nimport androidx.compose.animation.AnimatedContentTransitionScope\n/' app/src/main/java/com/example/MainActivity.kt
