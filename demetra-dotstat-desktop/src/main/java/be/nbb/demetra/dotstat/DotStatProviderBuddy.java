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
import ec.util.completion.swing.CustomListCellRenderer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import org.netbeans.api.options.OptionsDisplayer;
import org.openide.nodes.Sheet;
import org.openide.util.lookup.ServiceProvider;
import be.nbb.sdmx.facade.util.HasCache;
import com.google.common.base.Strings;
import ec.tstoolkit.utilities.GuavaCaches;
import static ec.util.completion.AutoCompletionSource.Behavior.ASYNC;
import static ec.util.completion.AutoCompletionSource.Behavior.NONE;
import static ec.util.completion.AutoCompletionSource.Behavior.SYNC;
import ec.util.completion.ExtAutoCompletionSource;
import java.io.IOException;
import java.time.Duration;
import java.util.Comparator;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 *
 * @author Philippe Charles
 */
@ServiceProvider(service = IDataSourceProviderBuddy.class)
public final class DotStatProviderBuddy extends DbProviderBuddy<DotStatBean> implements IConfigurable {

    private final Configurator<DotStatProviderBuddy> configurator;
    private final SdmxConnectionSupplier supplier;
    private final ListCellRenderer tableRenderer;
    private final ListCellRenderer columnRenderer;

    public DotStatProviderBuddy() {
        this.configurator = createConfigurator();
        this.supplier = SdmxDriverManager.getDefault();
        this.tableRenderer = new DataflowRenderer();
        this.columnRenderer = new DimensionRenderer();
        initDriverCache();
    }

    private static void initDriverCache() {
        ConcurrentMap cache = GuavaCaches.softValuesCacheAsMap();
        SdmxDriverManager.getDefault().getDrivers().stream()
                .filter(o -> (o instanceof HasCache))
                .forEach(o -> ((HasCache) o).setCache(cache));
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
        b.withAutoCompletion()
                .select(bean, "dimColumns")
                .source(getColumnSource(bean))
                .separator(",")
                .defaultValueSupplier(() -> getColumnSource(bean).getValues("").stream().map(o -> ((Dimension) o).getId()).collect(Collectors.joining(",")))
                .cellRenderer(getColumnRenderer(bean))
                .display("Dimensions")
                .add();
        return b;
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
        return ExtAutoCompletionSource
                .builder(o -> getDataflows(supplier, bean))
                .behavior(o -> !Strings.isNullOrEmpty(bean.getDbName()) ? ASYNC : NONE)
                .postProcessor(DotStatProviderBuddy::getDataflows)
                .valueToString(o -> o.getFlowRef().toString())
                .cache(GuavaCaches.ttlCacheAsMap(Duration.ofMinutes(1)), o -> bean.getDbName(), SYNC)
                .build();
    }

    @Override
    protected ListCellRenderer getTableRenderer(DotStatBean bean) {
        return tableRenderer;
    }

    @Override
    protected AutoCompletionSource getColumnSource(DotStatBean bean) {
        return ExtAutoCompletionSource
                .builder(o -> getDimensions(supplier, bean))
                .behavior(o -> !Strings.isNullOrEmpty(bean.getDbName()) && !Strings.isNullOrEmpty(bean.getTableName()) ? ASYNC : NONE)
                .postProcessor(DotStatProviderBuddy::getDimensions)
                .valueToString(Dimension::getId)
                .cache(GuavaCaches.ttlCacheAsMap(Duration.ofMinutes(1)), o -> bean.getDbName() + "/" + bean.getTableName(), SYNC)
                .build();
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

    private static List<Dataflow> getDataflows(SdmxConnectionSupplier supplier, DotStatBean bean) throws IOException {
        try (SdmxConnection c = supplier.getConnection(bean.getDbName())) {
            return new ArrayList<>(c.getDataflows());
        }
    }

    private static List<Dimension> getDimensions(SdmxConnectionSupplier supplier, DotStatBean bean) throws IOException {
        try (SdmxConnection c = supplier.getConnection(bean.getDbName())) {
            return new ArrayList<>(c.getDataStructure(bean.getFlowRef()).getDimensions());
        }
    }

    private static List<Dataflow> getDataflows(List<Dataflow> values, String term) {
        Predicate<String> filter = ExtAutoCompletionSource.basicFilter(term);
        return values.stream()
                .filter(o -> filter.test(o.getLabel()) || filter.test(o.getFlowRef().getId()))
                .sorted(Comparator.comparing(Dataflow::getLabel))
                .collect(Collectors.toList());
    }

    private static List<Dimension> getDimensions(List<Dimension> values, String term) {
        Predicate<String> filter = ExtAutoCompletionSource.basicFilter(term);
        return values.stream()
                .filter(o -> filter.test(o.getId()) || filter.test(o.getLabel()) || filter.test(String.valueOf(o.getPosition())))
                .sorted(Comparator.comparing(Dimension::getId))
                .collect(Collectors.toList());
    }

    //<editor-fold defaultstate="collapsed" desc="Renderers">
    private static final class DataflowRenderer extends CustomListCellRenderer<Dataflow> {

        @Override
        protected String getValueAsString(Dataflow value) {
            return value.getLabel();
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

        @Override
        protected String toToolTipText(String term, JList list, Dimension value, int index, boolean isSelected, boolean cellHasFocus) {
            return value.getLabel();
        }
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Configuration">
    private static Configurator<DotStatProviderBuddy> createConfigurator() {
        return new BuddyConfigHandler().toConfigurator(new BuddyConfigConverter());
    }

    @lombok.Data
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
