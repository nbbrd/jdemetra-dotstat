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
import be.nbb.sdmx.facade.driver.SdmxDriverManager;
import com.google.common.base.Converter;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import javax.swing.ListCellRenderer;
import org.netbeans.api.options.OptionsDisplayer;
import org.openide.nodes.Sheet;
import org.openide.util.lookup.ServiceProvider;
import be.nbb.sdmx.facade.util.HasCache;
import ec.tstoolkit.utilities.GuavaCaches;
import ec.util.completion.swing.CustomListCellRenderer;
import internal.sdmx.SdmxAutoCompletion;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 *
 * @author Philippe Charles
 */
@Deprecated
@ServiceProvider(service = IDataSourceProviderBuddy.class)
public final class DotStatProviderBuddy extends DbProviderBuddy<DotStatBean> implements IConfigurable {

    private final Configurator<DotStatProviderBuddy> configurator;
    private final SdmxConnectionSupplier supplier;
    private final ListCellRenderer tableRenderer;
    private final ListCellRenderer columnRenderer;

    public DotStatProviderBuddy() {
        this.configurator = createConfigurator();
        this.supplier = SdmxDriverManager.getDefault();
        this.tableRenderer = CustomListCellRenderer.of(Dataflow::getLabel, o -> o.getFlowRef().toString());
        this.columnRenderer = CustomListCellRenderer.of(Dimension::getId, Dimension::getLabel);
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
        return SdmxAutoCompletion.onFlows(supplier, bean::getDbName);
    }

    @Override
    protected ListCellRenderer getTableRenderer(DotStatBean bean) {
        return tableRenderer;
    }

    @Override
    protected AutoCompletionSource getColumnSource(DotStatBean bean) {
        return SdmxAutoCompletion.onDimensions(supplier, bean::getDbName, bean::getTableName);
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

    //<editor-fold defaultstate="collapsed" desc="Configuration">
    private static Configurator<DotStatProviderBuddy> createConfigurator() {
        return new BuddyConfigHandler().toConfigurator(BuddyConfig.converter());
    }

    @lombok.Data
    public static final class BuddyConfig {

        String preferredLanguage;
        boolean displayCodes;

        public static Converter<BuddyConfig, Config> converter() {
            return new BuddyConfigConverter();
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
    }

    private static Optional<DotStatProvider> lookupProvider() {
        return TsProviders.lookup(DotStatProvider.class, DotStatProvider.NAME).toJavaUtil();
    }

    private static final class BuddyConfigHandler extends BeanHandler<BuddyConfig, DotStatProviderBuddy> {

        @Override
        public BuddyConfig loadBean(DotStatProviderBuddy resource) {
            BuddyConfig result = new BuddyConfig();
            lookupProvider().ifPresent(o -> {
                result.setPreferredLanguage(o.getPreferredLanguage());
                result.setDisplayCodes(o.isDisplayCodes());
            });
            return result;
        }

        @Override
        public void storeBean(DotStatProviderBuddy resource, BuddyConfig bean) {
            lookupProvider().ifPresent(o -> {
                o.setPreferredLanguage(bean.getPreferredLanguage());
                o.setDisplayCodes(bean.isDisplayCodes());
            });
        }
    }
    //</editor-fold>
}
