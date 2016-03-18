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

import be.nbb.sdmx.facade.Dataflow;
import be.nbb.sdmx.facade.Dimension;
import ec.nbdemetra.db.DimensionsEditor;
import be.nbb.sdmx.facade.SdmxConnectionSupplier;
import be.nbb.sdmx.facade.SdmxConnection;
import be.nbb.sdmx.facade.driver.SdmxDriverManager;
import com.google.common.base.Converter;
import com.google.common.base.Optional;
import ec.nbdemetra.db.DbProviderBuddy;
import ec.nbdemetra.ui.BeanHandler;
import ec.nbdemetra.ui.Config;
import ec.nbdemetra.ui.Configurator;
import ec.nbdemetra.ui.IConfigurable;
import ec.nbdemetra.ui.properties.NodePropertySetBuilder;
import ec.nbdemetra.ui.tsproviders.IDataSourceProviderBuddy;
import ec.tss.tsproviders.TsProviders;
import ec.tss.tsproviders.utils.IParam;
import ec.tss.tsproviders.utils.Params;
import ec.util.completion.AutoCompletionSource;
import ec.util.completion.AutoCompletionSource.Behavior;
import ec.util.completion.ext.QuickAutoCompletionSource;
import ec.util.completion.swing.CustomListCellRenderer;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import lombok.Data;
import org.netbeans.api.options.OptionsDisplayer;
import org.openide.nodes.Sheet;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Philippe Charles
 */
@ServiceProvider(service = IDataSourceProviderBuddy.class)
public class DotStatProviderBuddy extends DbProviderBuddy<DotStatBean> implements IConfigurable {

    private final Configurator<DotStatProviderBuddy> configurator;
    private final SdmxConnectionSupplier supplier;
    private final ListCellRenderer tableRenderer;
    private final ListCellRenderer columnRenderer;

    public DotStatProviderBuddy() {
        this.configurator = createConfigurator();
        this.supplier = SdmxDriverManager.getDefault();
        this.tableRenderer = new DataflowRenderer();
        this.columnRenderer = new DimensionRenderer();
    }

    @Override
    protected boolean isFile() {
        return false;
    }

    @Override
    public String getProviderName() {
        return DotStatProvider.NAME;
    }

    @Override
    protected List<Sheet.Set> createSheetSets(Object bean) {
        List<Sheet.Set> result = new ArrayList<>();
        NodePropertySetBuilder b = new NodePropertySetBuilder();
        result.add(withSource(b.reset("Source"), (DotStatBean) bean).build());
        result.add(withCache(b.reset("Cache").description("Mechanism used to improve performance."), (DotStatBean) bean).build());
        return result;
    }

    @Override
    protected NodePropertySetBuilder withSource(NodePropertySetBuilder b, DotStatBean bean) {
        withDbName(b, bean);
        b.withAutoCompletion()
                .select(bean, "tableName")
                .source(getTableSource(bean))
                .cellRenderer(getTableRenderer(bean))
                .name("Dataset")
                .add();
        withDimColumns(b, bean);
        return b;
    }

    protected NodePropertySetBuilder withDimColumns(NodePropertySetBuilder b, DotStatBean bean) {
        return b.with(String.class)
                .select(bean, "dimColumns")
                .editor(DimensionsEditor.class)
                .attribute(DimensionsEditor.SOURCE_ATTRIBUTE, getColumnSource(bean))
                .attribute(DimensionsEditor.CELL_RENDERER_ATTRIBUTE, getColumnRenderer(bean))
                .attribute(DimensionsEditor.SEPARATOR_ATTRIBUTE, ",")
                .display("Dimensions")
                .add();
    }

    @Override
    protected NodePropertySetBuilder withDbName(NodePropertySetBuilder b, DotStatBean bean) {
        return b.withAutoCompletion()
                .select(bean, "dbName")
                .servicePath(SdmxWsAutoCompletionService.PATH)
                .display("REST endpoint name")
                .add();
    }

    @Override
    protected AutoCompletionSource getTableSource(DotStatBean bean) {
        return new DataflowSource(supplier, bean);
    }

    @Override
    protected ListCellRenderer getTableRenderer(DotStatBean bean) {
        return tableRenderer;
    }

    @Override
    protected AutoCompletionSource getColumnSource(DotStatBean bean) {
        return new DimensionSource(supplier, bean);
    }

    @Override
    protected ListCellRenderer getColumnRenderer(DotStatBean bean) {
        return columnRenderer;
    }

    @Override
    public Config getConfig() {
        return configurator.getConfig(this);
    }

    @Override
    public void setConfig(Config config) throws IllegalArgumentException {
        configurator.setConfig(this, config);
    }

    @Override
    public Config editConfig(Config config) throws IllegalArgumentException {
        OptionsDisplayer.getDefault().open(DotStatOptionsPanelController.ID);
        return config;
    }

    //<editor-fold defaultstate="collapsed" desc="Renderers">
    private static final class DataflowRenderer extends CustomListCellRenderer<Dataflow> {

        @Override
        protected String getValueAsString(Dataflow value) {
            return value.getName();
        }

        @Override
        protected String toToolTipText(String term, JList list, Dataflow value, int index, boolean isSelected, boolean cellHasFocus) {
            return value.getFlowRef().toString();
        }
    }

    private static final class DimensionRenderer extends CustomListCellRenderer<Dimension> {

        @Override
        protected String getValueAsString(Dimension value) {
            return value.getId();
        }
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Auto completion sources">    
    private static abstract class DotStatAutoCompletionSource<T> extends QuickAutoCompletionSource<T> {

        protected final SdmxConnectionSupplier supplier;
        protected final DotStatBean bean;

        public DotStatAutoCompletionSource(SdmxConnectionSupplier supplier, DotStatBean bean) {
            this.supplier = supplier;
            this.bean = bean;
        }

        abstract protected Iterable<T> getAllValues(SdmxConnection c) throws Exception;

        @Override
        protected Iterable<T> getAllValues() throws Exception {
            try (SdmxConnection conn = supplier.getConnection(bean.getDbName())) {
                return getAllValues(conn);
            }
        }

        @Override
        public Behavior getBehavior(String term) {
            return Behavior.ASYNC;
        }
    }

    private static final class DataflowSource extends DotStatAutoCompletionSource<Dataflow> {

        public DataflowSource(SdmxConnectionSupplier supplier, DotStatBean bean) {
            super(supplier, bean);
        }

        @Override
        protected Iterable<Dataflow> getAllValues(SdmxConnection c) throws Exception {
            return c.getDataflows();
        }

        @Override
        protected boolean matches(TermMatcher termMatcher, Dataflow input) {
            return termMatcher.matches(input.getName())
                    || termMatcher.matches(input.getFlowRef().getFlowId());
        }

        @Override
        protected String valueToString(Dataflow value) {
            return value.getFlowRef().toString();
        }
    }

    private static final class DimensionSource extends DotStatAutoCompletionSource<Dimension> {

        public DimensionSource(SdmxConnectionSupplier supplier, DotStatBean bean) {
            super(supplier, bean);
        }

        @Override
        protected Iterable<Dimension> getAllValues(SdmxConnection c) throws Exception {
            return c.getDataStructure(bean.getFlowRef()).getDimensions();
        }

        @Override
        protected boolean matches(TermMatcher termMatcher, Dimension input) {
            return termMatcher.matches(input.getId())
                    || termMatcher.matches(String.valueOf(input.getPosition()));
        }

        @Override
        protected String valueToString(Dimension value) {
            return value.getId();
        }

        @Override
        public int compare(Dimension left, Dimension right) {
            return Integer.compare(left.getPosition(), right.getPosition());
        }
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Configuration">
    private static Configurator<DotStatProviderBuddy> createConfigurator() {
        return new BuddyConfigHandler().toConfigurator(new BuddyConfigConverter());
    }

    @Data
    public static final class BuddyConfig {

        private String preferredLanguage;
        private boolean displayCodes;
    }

    private static final class BuddyConfigHandler extends BeanHandler<BuddyConfig, DotStatProviderBuddy> {

        @Override
        public BuddyConfig loadBean(DotStatProviderBuddy resource) {
            BuddyConfig result = new BuddyConfig();
            Optional<DotStatProvider> provider = TsProviders.lookup(DotStatProvider.class, DotStatProvider.NAME);
            if (provider.isPresent()) {
                result.setPreferredLanguage(provider.get().getPreferredLanguage());
                result.setDisplayCodes(provider.get().isDisplayCodes());
            }
            return result;
        }

        @Override
        public void storeBean(DotStatProviderBuddy resource, BuddyConfig bean) {
            Optional<DotStatProvider> provider = TsProviders.lookup(DotStatProvider.class, DotStatProvider.NAME);
            if (provider.isPresent()) {
                provider.get().setPreferredLanguage(bean.getPreferredLanguage());
                provider.get().setDisplayCodes(bean.isDisplayCodes());
            }
        }
    }

    private static final class BuddyConfigConverter extends Converter<BuddyConfig, Config> {

        private final IParam<Config, String> prefferedLanguageParam = Params.onString("en", "preferredLanguage");
        private final IParam<Config, Boolean> displayCodesParam = Params.onBoolean(false, "displayCodes");

        @Override
        protected Config doForward(BuddyConfig a) {
            Config.Builder result = Config.builder(BuddyConfig.class.getName(), "INSTANCE", "20150225");
            prefferedLanguageParam.set(result, a.getPreferredLanguage());
            displayCodesParam.set(result, a.isDisplayCodes());
            return result.build();
        }

        @Override
        protected BuddyConfig doBackward(Config b) {
            BuddyConfig result = new BuddyConfig();
            result.setPreferredLanguage(prefferedLanguageParam.get(b));
            result.setDisplayCodes(displayCodesParam.get(b));
            return result;
        }
    }
    //</editor-fold>
}
