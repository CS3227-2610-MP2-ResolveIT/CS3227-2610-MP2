"""Compile the navigation slice using an already installed JavaFX SDK."""
import os
from pathlib import Path
import subprocess
import tempfile

root = Path(__file__).resolve().parent
sdk = os.environ.get("JAVAFX_LIB")
if not sdk or not (Path(sdk) / "javafx.graphics.jar").is_file():
    raise SystemExit("Unavailable: set JAVAFX_LIB to the JavaFX 25 SDK lib directory.")
with tempfile.TemporaryDirectory() as output:
    result = subprocess.run([
        "javac", "--release", "25", "--module-path", sdk,
        "--add-modules", "javafx.graphics", "-d", output,
        *map(str, (root / "src/main/java").rglob("*.java")),
    ], check=False)
    raise SystemExit(result.returncode)
