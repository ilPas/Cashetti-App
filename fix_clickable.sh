sed -i '1d' app/src/main/java/com/example/ui/components/DetectedTransactionDialog.kt
sed -i '2s/^/import androidx.compose.foundation.clickable\n/' app/src/main/java/com/example/ui/components/DetectedTransactionDialog.kt
