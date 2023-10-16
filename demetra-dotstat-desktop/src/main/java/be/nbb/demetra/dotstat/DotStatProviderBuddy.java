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
import internal.sdmx.SdmxWebSourceService;
import lombok.NonNull;
import org.netbeans.api.options.OptionsDisplayer;
import org.openide.nodes.Sheet;
import org.openide.util.lookup.ServiceProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author Philippe Charles
 */
@Deprecated
@ServiceProvider(service = IDataSourceProviderBuddy.class)
public final class DotStatProviderBuddy extends DbProviderBuddy<DotStatBean> implements IConfigurable {

    private final Configurator<DotStatProviderBuddy> configurator;

    public DotStatProviderBuddy() {
        this.configurator = createConfigurator();
    }

    @Override
    protected boolean isFile() {
        return false;
    }

    @Override
    public @NonNull String getProviderName() {
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
    protected @NonNull NodePropertySetBuilder withSource(@NonNull NodePropertySetBuilder b, @NonNull DotStatBean bean) {
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
                .cellRenderer(getColumnRenderer(bean))
                .display("Dimensions")
                .add();
        return b;
    }

    @Override
    protected @NonNull NodePropertySetBuilder withDbName(NodePropertySetBuilder b, @NonNull DotStatBean bean) {
        return b.withAutoCompletion()
                .select(bean, "dbName")
                .servicePath(SdmxWebSourceService.PATH)
                .display("REST endpoint name")
                .add();
    }

    @Override
    public @NonNull Config getConfig() {
        return configurator.getConfig(this);
    }

    @Override
    public void setConfig(@NonNull Config config) throws IllegalArgumentException {
        configurator.setConfig(this, config);
    }

    @Override
    public @NonNull Config editConfig(@NonNull Config config) throws IllegalArgumentException {
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
        boolean curlBackend;
        File customSources;

        public static Converter<BuddyConfig, Config> converter() {
            return new BuddyConfigConverter();
        }

        private static final class BuddyConfigConverter extends Converter<BuddyConfig, Config> {

            private final IParam<Config, String> preferredLanguageParam = Params.onString("en", "preferredLanguage");
            private final IParam<Config, Boolean> displayCodesParam = Params.onBoolean(false, "displayCodes");
            private final IParam<Config, Boolean> curlBackendParam = Params.onBoolean(false, "curlBackend");
            private final IParam<Config, File> customSourcesParam = Params.onFile(new File(""), "customSources");

            @Override
            protected Config doForward(BuddyConfig a) {
                Config.Builder result = Config.builder(BuddyConfig.class.getName(), "INSTANCE", "20150225");
                preferredLanguageParam.set(result, a.getPreferredLanguage());
                displayCodesParam.set(result, a.isDisplayCodes());
                curlBackendParam.set(result, a.isCurlBackend());
                customSourcesParam.set(result, a.getCustomSources());
                return result.build();
            }

            @Override
            protected BuddyConfig doBackward(Config b) {
                BuddyConfig result = new BuddyConfig();
                result.setPreferredLanguage(preferredLanguageParam.get(b));
                result.setDisplayCodes(displayCodesParam.get(b));
                result.setCurlBackend(curlBackendParam.get(b));
                result.setCustomSources(customSourcesParam.get(b));
                return result;
            }
        }
    }

    private static Optional<DotStatProvider> lookupProvider() {
        return TsProviders.lookup(DotStatProvider.class, DotStatProvider.NAME).toJavaUtil();
    }

    private static final class BuddyConfigHandler extends BeanHandler<BuddyConfig, DotStatProviderBuddy> {

        @Override
        public @NonNull BuddyConfig loadBean(@NonNull DotStatProviderBuddy resource) {
            BuddyConfig result = new BuddyConfig();
            lookupProvider().ifPresent(o -> {
                result.setPreferredLanguage(o.getPreferredLanguage());
                result.setDisplayCodes(o.isDisplayCodes());
            });
            return result;
        }

        @Override
        public void storeBean(@NonNull DotStatProviderBuddy resource, @NonNull BuddyConfig bean) {
            lookupProvider().ifPresent(o -> {
                o.setPreferredLanguage(bean.getPreferredLanguage());
                o.setDisplayCodes(bean.isDisplayCodes());
            });
        }
    }
    //</editor-fold>
}
