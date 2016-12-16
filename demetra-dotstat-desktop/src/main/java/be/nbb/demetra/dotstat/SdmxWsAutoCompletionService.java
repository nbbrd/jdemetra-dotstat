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
package be.nbb.demetra.dotstat;

import be.nbb.sdmx.facade.driver.SdmxDriverManager;
import be.nbb.sdmx.facade.driver.WsEntryPoint;
import ec.nbdemetra.ui.completion.JAutoCompletionService;
import ec.util.completion.AbstractAutoCompletionSource;
import ec.util.completion.AutoCompletionSource;
import ec.util.completion.ext.QuickAutoCompletionSource;
import ec.util.completion.swing.CustomListCellRenderer;
import ec.util.completion.swing.JAutoCompletion;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.text.JTextComponent;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Philippe Charles
 */
@ServiceProvider(service = JAutoCompletionService.class, path = SdmxWsAutoCompletionService.PATH)
public final class SdmxWsAutoCompletionService extends JAutoCompletionService {

    public static final String PATH = "JAutoCompletionService/SdmxWs";

    private final AutoCompletionSource source = new ConnectionSource();
    private final ListCellRenderer renderer = new ConnectionRenderer();

    @Override
    public JAutoCompletion bind(JTextComponent textComponent) {
        JAutoCompletion result = new JAutoCompletion(textComponent);
        result.setMinLength(0);
        result.setSource(source);
        result.getList().setCellRenderer(renderer);
        return result;
    }

    private static final class ConnectionSource extends QuickAutoCompletionSource<WsEntryPoint> {

        @Override
        protected Iterable<WsEntryPoint> getAllValues() throws Exception {
            return SdmxDriverManager.getDefault().getEntryPoints();
        }

        @Override
        public AutoCompletionSource.Behavior getBehavior(String term) {
            return AutoCompletionSource.Behavior.SYNC;
        }

        @Override
        protected boolean matches(AbstractAutoCompletionSource.TermMatcher termMatcher, WsEntryPoint input) {
            return termMatcher.matches(input.getName())
                    || termMatcher.matches(input.getDescription())
                    || termMatcher.matches(input.getUri().toString());
        }

        @Override
        protected String valueToString(WsEntryPoint value) {
            return value.getName();
        }

        @Override
        public int compare(WsEntryPoint left, WsEntryPoint right) {
            return left.getDescription().compareTo(right.getDescription());
        }
    }

    private static final class ConnectionRenderer extends CustomListCellRenderer<WsEntryPoint> {

        @Override
        protected String getValueAsString(WsEntryPoint value) {
            return value.getDescription();
        }

        @Override
        protected String toToolTipText(String term, JList list, WsEntryPoint value, int index, boolean isSelected, boolean cellHasFocus) {
            return value.getName();
        }
    }
}
