/*
 * Copyright 2015 National Bank of Belgium
 * 
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved 
 * by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * 
 * http://ec.europa.eu/idabc/eupl
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and 
 * limitations under the Licence.
 */
package ec.util.list.swing;

import ec.util.various.swing.BasicSwingLauncher;
import ec.util.various.swing.FontAwesome;
import ec.util.various.swing.ext.FontAwesomeUtils;
import java.awt.BorderLayout;
import java.awt.Component;
import java.beans.BeanInfo;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JToolBar;

/**
 *
 * @author Philippe Charles
 */
final class JOrderingListDemo implements Callable<Component> {

    public static void main(String[] args) {
        new BasicSwingLauncher()
                .content(new JOrderingListDemo())
                .size(400, 300)
                .logLevel(Level.FINE)
                .launch();
    }

    @Override
    public Component call() throws Exception {
        JOrderingList<FontAwesome> list = new JOrderingList<>();
        DefaultOrderingListModel<FontAwesome> model = new DefaultOrderingListModel<>();
        Collections.addAll(model, FontAwesome.values());
        list.setModel(model);
        list.setCellRenderer(new LabelListCellRenderer<FontAwesome>() {
            private final float size = FontAwesomeUtils.toSize(BeanInfo.ICON_MONO_16x16);

            @Override
            public JLabel getListCellRendererComponent(JList<? extends FontAwesome> list, FontAwesome value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel result = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                result.setIcon(value.getIcon(result.getForeground(), size));
                return result;
            }
        });

        JButton button;

        JToolBar toolBar = new JToolBar();
        toolBar.setOrientation(JToolBar.VERTICAL);
        toolBar.setFloatable(false);

        button = toolBar.add(JOrderingListCommands.moveUp().toAction(list));
        button.setIcon(FontAwesomeUtils.getIcon(FontAwesome.FA_CARET_UP, BeanInfo.ICON_MONO_16x16));

        button = toolBar.add(JOrderingListCommands.moveDown().toAction(list));
        button.setIcon(FontAwesomeUtils.getIcon(FontAwesome.FA_CARET_DOWN, BeanInfo.ICON_MONO_16x16));

        JPanel result = new JPanel();
        result.setLayout(new BorderLayout());
        result.add(list, BorderLayout.CENTER);
        result.add(toolBar, BorderLayout.EAST);
        return result;
    }
}
