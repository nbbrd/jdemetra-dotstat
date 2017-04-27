/*
 * Copyright 2017 National Bank of Belgium
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
package internal.desktop;

import ec.util.completion.swing.CustomListCellRenderer;
import java.util.function.Function;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

/**
 *
 * @author Philippe Charles
 */
@lombok.experimental.UtilityClass
public class AutoCompletionRenderers {

    public <T> DefaultListCellRenderer of(Function<T, String> toValueAsString, Function<T, String> toToolTipText) {
        return new CustomListCellRenderer<T>() {
            @Override
            protected String getValueAsString(T value) {
                return toValueAsString.apply(value);
            }

            @Override
            protected String toToolTipText(String term, JList list, T value, int index, boolean isSelected, boolean cellHasFocus) {
                return toToolTipText.apply(value);
            }
        };
    }
}
