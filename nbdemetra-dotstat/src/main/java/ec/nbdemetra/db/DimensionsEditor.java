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
package ec.nbdemetra.db;

import ec.nbdemetra.ui.properties.AutoCompletedPropertyEditor3;
import static ec.nbdemetra.ui.properties.AutoCompletedPropertyEditor3.SOURCE_ATTRIBUTE;
import ec.util.completion.AutoCompletionSource;
import java.awt.Component;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import org.openide.explorer.propertysheet.PropertyEnv;

/**
 *
 * @author Philippe Charles
 */
public final class DimensionsEditor extends AutoCompletedPropertyEditor3 {

    private final JDimList editor;
    private PropertyEnv currentEnv;

    public DimensionsEditor() {
        this.editor = new JDimList();
        editor.setPreferredSize(new Dimension(300, 180));
        editor.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                switch (evt.getPropertyName()) {
                    case JDimList.DIMENSIONS_PROPERTY:
                        setValue(editor.getDimensions());
                        break;
                    case JDimList.RUNNING_PROPERTY:
                        currentEnv.setState(editor.isRunning() ? PropertyEnv.STATE_INVALID : PropertyEnv.STATE_VALID);
                        break;
                }
            }
        });
    }

    @Override
    public void attachEnv(PropertyEnv env) {
        super.attachEnv(env);
        currentEnv = env;
    }

    @Override
    public String getAsText() {
        String tmp = super.getAsText();
        return tmp.replace(",", " \u27A1 ");
    }

    @Override
    public boolean supportsCustomEditor() {
        return true;
    }

    @Override
    public Component getCustomEditor() {
        editor.setDimensions((String) getValue());
        editor.setAutoCompletionSource((AutoCompletionSource) currentEnv.getFeatureDescriptor().getValue(SOURCE_ATTRIBUTE));
        return editor;
    }
}
