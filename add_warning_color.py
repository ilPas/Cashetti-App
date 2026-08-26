with open("app/src/main/java/com/example/ui/theme/Color.kt", "r") as f:
    content = f.read()

content = content.replace("val StatusError = AlertRedDark", "val StatusError = AlertRedDark\n    val StatusWarning = WarningOrange")

with open("app/src/main/java/com/example/ui/theme/Color.kt", "w") as f:
    f.write(content)
