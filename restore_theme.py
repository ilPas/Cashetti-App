import os

screens_dir = "app/src/main/java/com/example/ui/screens"
components_dir = "app/src/main/java/com/example/ui/components"

for d in [screens_dir, components_dir]:
    for f_name in os.listdir(d):
        if f_name.endswith(".kt"):
            filepath = os.path.join(d, f_name)
            with open(filepath, "r") as f:
                content = f.read()

            # Restore main surfaces
            content = content.replace("color = androidx.compose.ui.graphics.Color.White", "color = MaterialTheme.colorScheme.surfaceVariant")
            # Restore card surfaces
            content = content.replace("containerColor = androidx.compose.ui.graphics.Color(0xFFFAFAFA)", "containerColor = MaterialTheme.colorScheme.surface")
            content = content.replace("containerColor = androidx.compose.ui.graphics.Color.White", "containerColor = MaterialTheme.colorScheme.surface")
            
            # Restore texts
            content = content.replace("androidx.compose.ui.graphics.Color.Black", "MaterialTheme.colorScheme.onSurfaceVariant")
            content = content.replace("androidx.compose.ui.graphics.Color(0xFF1E1E1E)", "MaterialTheme.colorScheme.onSurface")

            with open(filepath, "w") as f:
                f.write(content)
