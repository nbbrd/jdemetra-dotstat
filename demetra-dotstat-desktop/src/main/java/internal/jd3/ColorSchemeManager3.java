package internal.jd3;

import ec.nbdemetra.ui.DemetraUI;
import ec.util.chart.ColorScheme;
import ec.util.chart.swing.SwingColorSchemeSupport;
import ec.util.various.swing.OnEDT;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.WeakHashMap;

public final class ColorSchemeManager3 {

    public static ColorSchemeManager3 get() {
        return INSTANCE;
    }

    private static final ColorSchemeManager3 INSTANCE = new ColorSchemeManager3();

    private final WeakHashMap<String, SwingColorSchemeSupport> cache = new WeakHashMap<>();

    private ColorSchemeManager3() {
    }

    @NonNull
    public ColorScheme getMainColorScheme() {
        return DemetraUI.getDefault().getColorScheme();
    }

    @OnEDT
    @NonNull
    public SwingColorSchemeSupport getSupport(@Nullable ColorScheme colorScheme) {
        ColorScheme result = colorScheme != null ? colorScheme : getMainColorScheme();
        return cache.computeIfAbsent(result.getName(), name -> SwingColorSchemeSupport.from(result));
    }
}
