sed -i '7d' app/src/main/java/com/example/MainActivity.kt
sed -i '1s/^/package com.example\n\n/' app/src/main/java/com/example/MainActivity.kt
