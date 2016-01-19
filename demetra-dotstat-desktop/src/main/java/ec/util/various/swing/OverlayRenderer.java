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
package ec.util.various.swing;

import java.awt.Component;
import javax.swing.JLabel;

/**
 *
 * @author Philippe Charles
 */
public abstract class OverlayRenderer<C extends Component> {

    abstract public Component getOverlayRenderer(C component);

    public static class Label<C extends Component> extends OverlayRenderer<C> {

        private final JLabel result;

        public Label() {
            this.result = new JLabel();
        }

        @Override
        public JLabel getOverlayRenderer(C component) {
            result.setHorizontalAlignment(JLabel.CENTER);
            result.setHorizontalTextPosition(JLabel.CENTER);
            result.setVerticalTextPosition(JLabel.BOTTOM);
            result.setOpaque(true);
            result.setText(component.getName());
            result.setToolTipText(null);
            result.setIcon(null);
            result.setBackground(component.getBackground());
            result.setForeground(component.getForeground());
            result.setFont(component.getFont());
            result.setSize(component.getSize());
            return result;
        }
    }
}
