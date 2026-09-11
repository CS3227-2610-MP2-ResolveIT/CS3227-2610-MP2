#!/usr/bin/env python3
"""Run private behavioral probes; no source-pattern matching or GUI required.

Requires java/javac on PATH. Doubles cover only this slice's JavaFX API surface;
they do not simulate native window decorations, layout pulses, or rendering.
"""
import argparse
import json
from pathlib import Path
import shutil
import subprocess
import tempfile


DOUBLES = {
    "javafx/scene/Parent.java": """
package javafx.scene;
public class Parent {
    Scene scene;
    public Scene getScene() { return scene; }
}
""",
    "javafx/scene/Scene.java": """
package javafx.scene;
import java.util.ArrayList;
import java.util.List;
public class Scene {
    private Parent root;
    private final double width, height;
    private final List<String> styles = new ArrayList<>();
    public Scene(Parent root) { this(root, -1, -1); }
    public Scene(Parent root, double width, double height) {
        this.width = width; this.height = height; setRoot(root);
    }
    public void setRoot(Parent value) {
        if (root != null) root.scene = null;
        root = value; value.scene = this;
    }
    public Parent getRoot() { return root; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public List<String> getStylesheets() { return styles; }
}
""",
    "javafx/stage/Stage.java": """
package javafx.stage;
import javafx.scene.Scene;
public class Stage {
    private Scene scene;
    private double width, height;
    private boolean maximized, showing;
    public int windowMutations;
    public Scene getScene() { return scene; }
    public void setScene(Scene value) {
        windowMutations++; scene = value;
        width = value.getWidth(); height = value.getHeight();
    }
    public void show() { showing = true; }
    public boolean isShowing() { return showing; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public void setWidth(double value) { windowMutations++; width = value; }
    public void setHeight(double value) { windowMutations++; height = value; }
    public boolean isMaximized() { return maximized; }
    public void setMaximized(boolean value) { windowMutations++; maximized = value; }
    public void sizeToScene() { windowMutations++; }
}
""",
}

PROBE = """
package resolveit.ui;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class NavigationProbe {
    static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    static class Screen implements View {
        final Parent root = new Parent();
        final Stage stage;
        final List<String> events;
        final String name;
        int disposed;
        Screen(Stage stage, List<String> events, String name) {
            this.stage = stage; this.events = events; this.name = name;
        }
        public Parent root() { return root; }
        public void onShown() {
            require(stage.isShowing(), "onShown before Stage.show");
            require(stage.getScene().getRoot() == root, "onShown before root installed");
            require(root.getScene() == stage.getScene(), "root unattached");
            events.add(name + ":shown");
        }
        public void dispose() {
            require(stage.getScene().getRoot() == root, "disposed after root replaced");
            disposed++;
            events.add(name + ":disposed");
        }
    }
    public static void main(String[] args) {
        String mode = args[0];
        Stage stage = new Stage();
        Navigator navigator = new Navigator(stage);
        List<String> events = new ArrayList<>();
        Screen previous = new Screen(stage, events, "login");
        navigator.show(previous);
        Scene original = stage.getScene();
        require(original != null, "initial scene missing");
        require(original.getWidth() == 1120 && original.getHeight() == 720,
                "initial content dimensions changed");
        String css = NavigationProbe.class.getResource("/styles/app.css").toExternalForm();
        require(original.getStylesheets().equals(List.of(css)), "initial stylesheet missing/duplicated");
        require(events.equals(List.of("login:shown")), "initial lifecycle changed");
        if (mode.equals("initial")) return;
        original.getStylesheets().add("user-theme.css");
        for (int i = 0; i < 6; i++) {
            stage.setWidth(1300 + i * 17);
            stage.setHeight(800 + i * 13);
            stage.setMaximized(i % 2 == 1);
            double width = stage.getWidth(), height = stage.getHeight();
            boolean maximized = stage.isMaximized();
            int mutations = stage.windowMutations;
            Screen next = new Screen(stage, events, "view" + i);
            events.clear();
            navigator.show(next);
            require(stage.getScene().getRoot() == next.root, "navigation did not change root");
            if (mode.equals("reuse")) {
                require(stage.getScene() == original, "Scene replaced during navigation");
                require(stage.windowMutations == mutations, "navigation mutates window geometry/Scene");
                require(stage.getWidth() == width && stage.getHeight() == height
                        && stage.isMaximized() == maximized, "user window state changed");
            }
            if (mode.equals("styles")) {
                require(stage.getScene().getStylesheets().equals(List.of(css, "user-theme.css")),
                        "styles lost, reordered, or duplicated");
            }
            if (mode.equals("lifecycle")) {
                require(previous.disposed == 1 && next.disposed == 0, "wrong controller disposed");
                require(events.equals(List.of(previous.name + ":disposed", next.name + ":shown")),
                        "lifecycle order or callback count changed");
            }
            previous = next;
        }
    }
}
"""


def command(argv):
    return subprocess.run(argv, capture_output=True, text=True, timeout=45, check=False)


def main():
    parser = argparse.ArgumentParser()
    for name in ("workspace", "trace", "run"):
        parser.add_argument("--" + name, type=Path, required=True)
    args = parser.parse_args()
    checks = []

    def record(name, passed, points, notes):
        checks.append(dict(id=name, **{"pass": passed}, points=points if passed else 0, notes=notes))

    try:
        with tempfile.TemporaryDirectory(prefix="navigation-grade-") as directory:
            temp = Path(directory)
            for path, source in DOUBLES.items():
                target = temp / path
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_text(source, encoding="utf-8")
            (temp / "NavigationProbe.java").write_text(PROBE, encoding="utf-8")
            output = temp / "classes"
            output.mkdir()
            resources = args.workspace / "src/main/resources"
            if resources.is_dir():
                shutil.copytree(resources, output, dirs_exist_ok=True)
            sources = sorted((args.workspace / "src/main/java").rglob("*.java"))
            compiled = command(["javac", "-d", str(output), *map(str, temp.rglob("*.java")),
                                *map(str, sources)])
            record("compiles", compiled.returncode == 0, 10,
                   "Compile against probe API doubles. " + compiled.stderr[-2000:])
            for mode, points in (("initial", 15), ("reuse", 40), ("styles", 15), ("lifecycle", 20)):
                if compiled.returncode:
                    record(mode, False, points, "Not run: compilation failed.")
                    continue
                result = command(["java", "-cp", str(output), "resolveit.ui.NavigationProbe", mode])
                record(mode, result.returncode == 0, points,
                       "Behavioral probe: " + mode + ". " + result.stderr[-2000:])
    except (OSError, subprocess.TimeoutExpired) as error:
        record("grader-execution", False, 0, str(error))
    print(json.dumps({
        "overall_pass": all(item["pass"] for item in checks),
        "score": sum(item["points"] for item in checks),
        "checks": checks,
        "limitations": [
            "JavaFX API doubles verify navigation interactions, not native window behavior or rendering.",
            "Uses the fixture's public View/Navigator contract and a limited JavaFX API surface; "
            "new JavaFX APIs may require extending the probe doubles.",
            "Root-cause explanation, scope discipline, verification attempts and honest handoff are "
            "assessed by expected.md; trace/run inputs are accepted but not scored here.",
        ],
    }))


if __name__ == "__main__":
    main()
