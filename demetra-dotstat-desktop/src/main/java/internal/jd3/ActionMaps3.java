package internal.jd3;

import ec.nbdemetra.ui.awt.ActionMaps;
import ec.util.chart.swing.Charts;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

@lombok.experimental.UtilityClass
public class ActionMaps3 {

    public static void onDoubleClick(@NonNull ActionMap actionMap, @NonNull String actionName, @NonNull JComponent component) {
        component.addMouseListener(new OnDoubleClick(actionMap, actionName));
    }

    @lombok.AllArgsConstructor
    private static final class OnDoubleClick extends MouseAdapter {

        @lombok.NonNull
        private final ActionMap actionMap;

        @lombok.NonNull
        private final String action;

        @Override
        public void mouseClicked(MouseEvent e) {
            if (!Charts.isPopup(e) && Charts.isDoubleClick(e)) {
                ActionMaps.performAction(actionMap, action, e);
            }
        }
    }
}
