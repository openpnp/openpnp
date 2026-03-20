package org.openpnp.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import javax.swing.Action;
import javax.swing.SwingUtilities;

import org.openpnp.util.UiUtils;
import org.pmw.tinylog.Logger;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;

/**
 * Polls a gamepad/stick and maps it to existing JogControlsPanel actions.
 *
 * Design goal: reuse OpenPnP's existing jog actions and safety checks instead of
 * introducing a new motion path.
 */
public class GamepadJogController {
    private static final long POLL_SLEEP_MS = 25L;
    private static final float DEADZONE = 0.25f;

    private static final long FAST_REPEAT_MS = 70L;
    private static final long MEDIUM_REPEAT_MS = 140L;
    private static final long SLOW_REPEAT_MS = 250L;

    private final MainFrame mainFrame;
    private final JogControlsPanel jog;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread worker;
    private boolean nativeAvailableChecked;
    private boolean nativeAvailable;

    private long nextXPos;
    private long nextXNeg;
    private long nextYPos;
    private long nextYNeg;
    private long nextZPos;
    private long nextZNeg;
    private long nextCPos;
    private long nextCNeg;

    private boolean lbDown;
    private boolean rbDown;
    private boolean dpadLeftDown;
    private boolean dpadRightDown;
    private boolean aDown;
    private long nextInputDebugLog;

    public GamepadJogController(MainFrame mainFrame, JogControlsPanel jog) {
        this.mainFrame = mainFrame;
        this.jog = jog;
    }

    public synchronized void start() {
        if (running.get()) {
            return;
        }
        running.set(true);
        worker = new Thread(this::runLoop, "GamepadJogController");
        worker.setDaemon(true);
        worker.start();
    }

    public synchronized void stop() {
        running.set(false);
        if (worker != null) {
            worker.interrupt();
            try {
                worker.join(500);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            worker = null;
        }
    }

    private void runLoop() {
        Logger.info("Gamepad jog controller starting.");
        Controller controller = null;
        long lastDiscoveryLog = 0;

        while (running.get()) {
            try {
                if (controller == null || !controller.poll()) {
                    controller = findController();
                    if (controller == null) {
                        long now = System.currentTimeMillis();
                        if (now - lastDiscoveryLog > 5000) {
                            Logger.debug("No gamepad found for jog control.");
                            lastDiscoveryLog = now;
                        }
                        sleepQuiet(POLL_SLEEP_MS);
                        continue;
                    }
                    Logger.info("Gamepad connected for jog control: " + controller.getName());
                    resetRepeatTimers();
                    lbDown = rbDown = dpadLeftDown = dpadRightDown = aDown = false;
                    nextInputDebugLog = 0;
                }

                handleController(controller);
                sleepQuiet(POLL_SLEEP_MS);
            }
            catch (Throwable t) {
                Logger.warn(t, "Gamepad jog controller loop error.");
                controller = null;
                sleepQuiet(500);
            }
        }

        Logger.info("Gamepad jog controller stopped.");
    }

    private Controller findController() {
        if (!isNativeAvailable()) {
            return null;
        }

        Controller[] roots;
        try {
            roots = ControllerEnvironment.getDefaultEnvironment().getControllers();
        }
        catch (UnsatisfiedLinkError e) {
            Logger.warn("JInput native library missing. Gamepad control disabled. "
                    + "Install jinput native libs (e.g. libjinput-jni) or add them to java.library.path.");
            nativeAvailableChecked = true;
            nativeAvailable = false;
            return null;
        }

        List<Controller> controllers = new ArrayList<>();
        for (Controller root : roots) {
            collectControllers(root, controllers);
        }

        Controller fallback = null;
        for (Controller c : controllers) {
            if (!isGamepadLike(c)) {
                continue;
            }

            String name = c.getName() == null ? "" : c.getName().toLowerCase();
            // Prefer the requested family first, but allow any gamepad as fallback.
            if (name.contains("thrustmaster") || name.contains("gp xid") || name.contains("xid")) {
                return c;
            }
            if (fallback == null) {
                fallback = c;
            }
        }
        return fallback;
    }

    private void collectControllers(Controller controller, List<Controller> out) {
        if (controller == null) {
            return;
        }
        out.add(controller);
        for (Controller child : controller.getControllers()) {
            collectControllers(child, out);
        }
    }

    private boolean isGamepadLike(Controller controller) {
        Controller.Type type = controller.getType();
        if (type == Controller.Type.GAMEPAD || type == Controller.Type.STICK) {
            return true;
        }

        // Linux/JInput can expose a usable gamepad as UNKNOWN under a parent controller.
        boolean hasX = false;
        boolean hasY = false;
        boolean hasButton = false;
        for (Component component : controller.getComponents()) {
            Component.Identifier id = component.getIdentifier();
            if (id == Component.Identifier.Axis.X) {
                hasX = true;
            }
            else if (id == Component.Identifier.Axis.Y) {
                hasY = true;
            }
            else if (id == Component.Identifier.Button._0) {
                hasButton = true;
            }
        }
        return hasX && hasY && hasButton;
    }

    private boolean isNativeAvailable() {
        if (nativeAvailableChecked) {
            return nativeAvailable;
        }
        nativeAvailableChecked = true;

        String os = System.getProperty("os.name", "").toLowerCase();
        if (!os.contains("linux")) {
            nativeAvailable = true;
            return true;
        }

        List<String> candidateLibNames = new ArrayList<>();
        candidateLibNames.add(System.mapLibraryName("jinput-linux64"));
        candidateLibNames.add(System.mapLibraryName("jinput-linux"));
        candidateLibNames.add(System.mapLibraryName("jinput"));

        // 1) Happy path: already in java.library.path.
        for (String libName : candidateLibNames) {
            if (existsInJavaLibraryPath(libName)) {
                nativeAvailable = true;
                return true;
            }
        }

        // 2) Try to point JInput to an existing matching native file.
        for (String libName : candidateLibNames) {
            File libFile = findNativeInCommonLocations(libName);
            if (libFile == null) {
                continue;
            }
            String parent = libFile.getParentFile().getAbsolutePath();
            System.setProperty("net.java.games.input.librarypath", parent);
            nativeAvailable = true;
            Logger.info("Using JInput native path: " + parent);
            return true;
        }

        // 3) Ubuntu/Debian commonly ship only libjinput.so.
        //    Create runtime aliases (without root) so JInput's linux-specific lookup succeeds.
        File genericLib = findNativeInCommonLocations(System.mapLibraryName("jinput"));
        if (genericLib != null) {
            File aliasDir = ensureLinuxAliasLibraries(genericLib);
            if (aliasDir != null) {
                System.setProperty("net.java.games.input.librarypath", aliasDir.getAbsolutePath());
                nativeAvailable = true;
                Logger.info("Using JInput alias native path: " + aliasDir.getAbsolutePath());
                return true;
            }
        }

        nativeAvailable = false;
        Logger.warn("Gamepad control disabled: missing JInput native library in java.library.path and common system locations.");
        return false;
    }

    private boolean existsInJavaLibraryPath(String libName) {
        String libraryPath = System.getProperty("java.library.path", "");
        for (String dir : libraryPath.split(File.pathSeparator)) {
            if (dir == null || dir.isEmpty()) {
                continue;
            }
            File f = new File(dir, libName);
            if (f.exists()) {
                return true;
            }
        }
        return false;
    }

    private File findNativeInCommonLocations(String libName) {
        Set<String> dirs = new LinkedHashSet<>();

        // Optional explicit override.
        String jinputHome = System.getProperty("jinput.home");
        if (jinputHome != null && !jinputHome.trim().isEmpty()) {
            dirs.add(jinputHome.trim());
        }

        String jinputLibPath = System.getProperty("net.java.games.input.librarypath");
        if (jinputLibPath != null && !jinputLibPath.trim().isEmpty()) {
            dirs.add(jinputLibPath.trim());
        }

        // java.library.path entries.
        String javaLibPath = System.getProperty("java.library.path", "");
        for (String dir : javaLibPath.split(File.pathSeparator)) {
            if (dir != null && !dir.isEmpty()) {
                dirs.add(dir);
            }
        }

        // Common Linux locations for JNI libs.
        List<String> common = new ArrayList<>();
        common.add("/usr/lib");
        common.add("/usr/lib64");
        common.add("/lib");
        common.add("/lib64");
        common.add("/usr/lib/jni");
        common.add("/usr/lib/x86_64-linux-gnu");
        common.add("/usr/lib/x86_64-linux-gnu/jni");
        common.add("/lib/x86_64-linux-gnu");
        common.add("/lib/x86_64-linux-gnu/jni");
        for (String dir : common) {
            dirs.add(dir);
        }

        for (String dir : dirs) {
            File f = new File(dir, libName);
            if (f.exists()) {
                return f;
            }
        }
        return null;
    }

    private File ensureLinuxAliasLibraries(File genericLib) {
        try {
            Path aliasDir = Path.of(System.getProperty("java.io.tmpdir"), "openpnp-jinput-natives");
            Files.createDirectories(aliasDir);

            Path linux64Alias = aliasDir.resolve(System.mapLibraryName("jinput-linux64"));
            Path linuxAlias = aliasDir.resolve(System.mapLibraryName("jinput-linux"));

            createAliasOrCopy(genericLib.toPath(), linux64Alias);
            createAliasOrCopy(genericLib.toPath(), linuxAlias);

            return aliasDir.toFile();
        }
        catch (Throwable t) {
            Logger.warn(t, "Failed to create runtime JInput alias libraries.");
            return null;
        }
    }

    private void createAliasOrCopy(Path source, Path target) throws Exception {
        if (Files.exists(target)) {
            return;
        }
        try {
            Files.createSymbolicLink(target, source);
        }
        catch (UnsupportedOperationException | SecurityException | java.io.IOException e) {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void handleController(Controller controller) {
        if (!mainFrame.isActive() || UiUtils.isTextInputFocused()) {
            return;
        }

        float lx = axisValue(controller, Component.Identifier.Axis.X);
        float ly = axisValue(controller, Component.Identifier.Axis.Y);
        float rx = axisValue(controller, Component.Identifier.Axis.RX);
        float ry = axisValue(controller, Component.Identifier.Axis.RY);

        long now = System.currentTimeMillis();

        // Left stick => X/Y jogging.
        if (lx > DEADZONE && now >= nextXPos) {
            fire(jog.xPlusAction);
            nextXPos = now + repeatDelay(lx);
        }
        else if (lx < -DEADZONE && now >= nextXNeg) {
            fire(jog.xMinusAction);
            nextXNeg = now + repeatDelay(-lx);
        }

        // Typical stick Y is negative for up: invert so push up => Y+
        if (ly < -DEADZONE && now >= nextYPos) {
            fire(jog.yPlusAction);
            nextYPos = now + repeatDelay(-ly);
        }
        else if (ly > DEADZONE && now >= nextYNeg) {
            fire(jog.yMinusAction);
            nextYNeg = now + repeatDelay(ly);
        }

        // Right stick Y => Z
        if (ry < -DEADZONE && now >= nextZPos) {
            fire(jog.zPlusAction);
            nextZPos = now + repeatDelay(-ry);
        }
        else if (ry > DEADZONE && now >= nextZNeg) {
            fire(jog.zMinusAction);
            nextZNeg = now + repeatDelay(ry);
        }

        // Right stick X => C (inverted direction, requested behavior)
        if (rx > DEADZONE && now >= nextCPos) {
            fire(jog.cMinusAction);
            nextCPos = now + repeatDelay(rx);
        }
        else if (rx < -DEADZONE && now >= nextCNeg) {
            fire(jog.cPlusAction);
            nextCNeg = now + repeatDelay(-rx);
        }

        // Shoulder buttons and D-Pad left/right => jog increment +/-
        // (some controllers expose shoulders differently in JInput)
        boolean lb = anyButtonDown(controller, 4, 6, 8, 9)
                || buttonNameDown(controller, "Left Thumb");
        boolean rb = anyButtonDown(controller, 5, 7, 10, 11)
                || buttonNameDown(controller, "Right Thumb");

        float pov = axisValue(controller, Component.Identifier.Axis.POV);
        boolean dpadLeft = pov == Component.POV.LEFT
                || pov == Component.POV.UP_LEFT
                || pov == Component.POV.DOWN_LEFT;
        boolean dpadRight = pov == Component.POV.RIGHT
                || pov == Component.POV.UP_RIGHT
                || pov == Component.POV.DOWN_RIGHT;

        debugInputMapping(controller, lb, rb, pov, dpadLeft, dpadRight);

        if ((lb && !lbDown) || (dpadLeft && !dpadLeftDown)) {
            fire(jog.lowerIncrementAction);
        }
        if ((rb && !rbDown) || (dpadRight && !dpadRightDown)) {
            fire(jog.raiseIncrementAction);
        }
        lbDown = lb;
        rbDown = rb;
        dpadLeftDown = dpadLeft;
        dpadRightDown = dpadRight;

        // A button => Safe Z
        boolean a = buttonValue(controller, Component.Identifier.Button._0);
        if (a && !aDown) {
            fire(jog.safezAction);
        }
        aDown = a;
    }

    private float axisValue(Controller controller, Component.Identifier.Axis axis) {
        for (Component component : controller.getComponents()) {
            if (component.getIdentifier().equals(axis)) {
                return component.getPollData();
            }
        }
        return 0f;
    }

    private boolean buttonValue(Controller controller, Component.Identifier.Button button) {
        for (Component component : controller.getComponents()) {
            if (component.getIdentifier().equals(button)) {
                return component.getPollData() > 0.5f;
            }
        }
        return false;
    }

    private boolean anyButtonDown(Controller controller, int... indices) {
        for (Component component : controller.getComponents()) {
            Component.Identifier id = component.getIdentifier();
            if (!(id instanceof Component.Identifier.Button)) {
                continue;
            }
            String name = id.getName();
            for (int index : indices) {
                if (Integer.toString(index).equals(name) && component.getPollData() > 0.5f) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean buttonNameDown(Controller controller, String containsName) {
        String needle = containsName == null ? "" : containsName.toLowerCase();
        for (Component component : controller.getComponents()) {
            Component.Identifier id = component.getIdentifier();
            if (!(id instanceof Component.Identifier.Button)) {
                continue;
            }
            String name = id.getName();
            if (name != null && name.toLowerCase().contains(needle) && component.getPollData() > 0.5f) {
                return true;
            }
        }
        return false;
    }

    private long repeatDelay(float magnitude) {
        if (magnitude > 0.85f) {
            return FAST_REPEAT_MS;
        }
        if (magnitude > 0.6f) {
            return MEDIUM_REPEAT_MS;
        }
        return SLOW_REPEAT_MS;
    }

    private void debugInputMapping(Controller controller, boolean lb, boolean rb, float pov,
            boolean dpadLeft, boolean dpadRight) {
        long now = System.currentTimeMillis();
        boolean edge = (lb && !lbDown) || (rb && !rbDown) || (dpadLeft && !dpadLeftDown)
                || (dpadRight && !dpadRightDown);
        if (!edge && now < nextInputDebugLog) {
            return;
        }
        nextInputDebugLog = now + 2000;

        StringBuilder buttons = new StringBuilder();
        StringBuilder axes = new StringBuilder();
        for (Component component : controller.getComponents()) {
            float value = component.getPollData();
            Component.Identifier id = component.getIdentifier();
            if (id instanceof Component.Identifier.Button) {
                if (value > 0.5f) {
                    if (buttons.length() > 0) {
                        buttons.append(' ');
                    }
                    buttons.append("B").append(id.getName());
                }
            }
            else if (id instanceof Component.Identifier.Axis) {
                if (id == Component.Identifier.Axis.POV || Math.abs(value) > 0.4f) {
                    if (axes.length() > 0) {
                        axes.append(' ');
                    }
                    axes.append(id.getName()).append('=').append(String.format("%.2f", value));
                }
            }
        }

        Logger.debug("Gamepad map debug: LB=" + lb + " RB=" + rb + " POV="
                + String.format("%.2f", pov) + " DPadL=" + dpadLeft + " DPadR=" + dpadRight
                + " pressedButtons=[" + buttons + "] activeAxes=[" + axes + "]");
    }

    private void resetRepeatTimers() {
        long now = System.currentTimeMillis();
        nextXPos = nextXNeg = nextYPos = nextYNeg = now;
        nextZPos = nextZNeg = nextCPos = nextCNeg = now;
    }

    private void fire(Action action) {
        if (action == null || !action.isEnabled()) {
            return;
        }
        SwingUtilities.invokeLater(() -> action.actionPerformed(null));
    }

    private void sleepQuiet(long millis) {
        try {
            Thread.sleep(millis);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
