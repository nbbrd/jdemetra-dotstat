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

import be.nbb.demetra.sdmx.web.SdmxWebProviderBuddy;
import ec.nbdemetra.ui.completion.JAutoCompletionService;
import ec.util.completion.AutoCompletionSource;
import ec.util.completion.swing.JAutoCompletion;
import internal.sdmx.SdmxAutoCompletion;
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

    private final AutoCompletionSource source = SdmxAutoCompletion.onEntryPoints(SdmxWebProviderBuddy.getDefaultManager());
    private final ListCellRenderer renderer = SdmxAutoCompletion.getEntryPointsRenderer();

    @Override
    public JAutoCompletion bind(JTextComponent textComponent) {
        JAutoCompletion result = new JAutoCompletion(textComponent);
        result.setMinLength(0);
        result.setSource(source);
        result.getList().setCellRenderer(renderer);
        return result;
    }
}
