import re
import os

components_dir = "app/src/main/java/com/example/ui/components"
screens_dir = "app/src/main/java/com/example/ui/screens"

# 1. TransactionItemCard.kt
with open(f"{components_dir}/TransactionItemCard.kt", "r") as f:
    content = f.read()

# Fix containerColor signature
content = re.sub(r'containerColor: androidx\.compose\.ui\.graphics\.Color\s*=\s*.*?\)', 'containerColor: androidx.compose.ui.graphics.Color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)', content)

# Remove hardcoded text colors
content = re.sub(r'color = androidx\.compose\.ui\.graphics\.Color\(0xFF1E1E1E\)', 'color = MaterialTheme.colorScheme.onSurfaceVariant', content)
content = re.sub(r'color = androidx\.compose\.ui\.graphics\.Color\.Black', 'color = MaterialTheme.colorScheme.onSurfaceVariant', content)

# Remove shadows
content = re.sub(r'elevation = CardDefaults\.cardElevation\(defaultElevation = 1\.dp\)', 'elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)', content)

with open(f"{components_dir}/TransactionItemCard.kt", "w") as f:
    f.write(content)

# 2. SubscriptionsScreen.kt
with open(f"{screens_dir}/SubscriptionsScreen.kt", "r") as f:
    content = f.read()

# Fix card colors
content = re.sub(r'containerColor\s*=\s*androidx\.compose\.ui\.graphics\.Color\(0xFFFAFAFA\)', 'containerColor = MaterialTheme.colorScheme.surfaceVariant', content)

# Fix text colors inside the card
content = content.replace('androidx.compose.ui.graphics.Color(0xFF1E1E1E)', 'MaterialTheme.colorScheme.onSurfaceVariant')

# Remove shadows
content = re.sub(r'elevation = CardDefaults\.cardElevation\(defaultElevation = 1\.dp\)', 'elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)', content)

with open(f"{screens_dir}/SubscriptionsScreen.kt", "w") as f:
    f.write(content)

# 3. HistoryScreen.kt
with open(f"{screens_dir}/HistoryScreen.kt", "r") as f:
    content = f.read()

content = re.sub(r'containerColor\s*=\s*androidx\.compose\.ui\.graphics\.Color\(0xFFFAFAFA\)', 'containerColor = MaterialTheme.colorScheme.surfaceVariant', content)

with open(f"{screens_dir}/HistoryScreen.kt", "w") as f:
    f.write(content)

# 4. AccountAllocationChart.kt
with open(f"{components_dir}/AccountAllocationChart.kt", "r") as f:
    content = f.read()

# Change the hardcoded 0xFFE4D086 to SavingsYellow
if "com.example.ui.theme.SavingsYellow" not in content:
    content = content.replace("import androidx.compose.ui.graphics.Color", "import androidx.compose.ui.graphics.Color\nimport com.example.ui.theme.SavingsYellow")

content = content.replace('Color(0xFFE4D086)', 'SavingsYellow')
content = content.replace('androidx.compose.ui.graphics.Color(0xFF1E1E1E)', 'MaterialTheme.colorScheme.onSurface')

with open(f"{components_dir}/AccountAllocationChart.kt", "w") as f:
    f.write(content)

# 5. DashboardScreen.kt
with open(f"{screens_dir}/DashboardScreen.kt", "r") as f:
    content = f.read()

# Fix containerColor if it has FAFAFA
content = re.sub(r'containerColor\s*=\s*androidx\.compose\.ui\.graphics\.Color\(0xFFFAFAFA\)', 'containerColor = MaterialTheme.colorScheme.surfaceVariant', content)
content = content.replace('androidx.compose.ui.graphics.Color(0xFF1E1E1E)', 'MaterialTheme.colorScheme.onSurfaceVariant')

with open(f"{screens_dir}/DashboardScreen.kt", "w") as f:
    f.write(content)
