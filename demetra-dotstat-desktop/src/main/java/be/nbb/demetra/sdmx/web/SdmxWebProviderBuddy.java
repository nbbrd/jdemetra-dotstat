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
package be.nbb.demetra.sdmx.web;

import be.nbb.demetra.dotstat.DotStatOptionsPanelController;
import be.nbb.demetra.dotstat.DotStatProviderBuddy.BuddyConfig;
import ec.nbdemetra.ui.BeanHandler;
import ec.nbdemetra.ui.Config;
import ec.nbdemetra.ui.Configurator;
import ec.nbdemetra.ui.IConfigurable;
import ec.nbdemetra.ui.properties.PropertySheetDialogBuilder;
import ec.nbdemetra.ui.tsproviders.IDataSourceProviderBuddy;
import ec.tss.TsMoniker;
import ec.tss.tsproviders.DataSet;
import ec.tss.tsproviders.DataSource;
import internal.sdmx.SdmxIcons;
import internal.sdmx.SdmxWebFactory;
import lombok.NonNull;
import nbbrd.io.text.Parser;
import org.netbeans.api.options.OptionsDisplayer;
import org.openide.awt.StatusDisplayer;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;
import sdmxdl.Languages;
import sdmxdl.format.xml.XmlWebSource;
import sdmxdl.web.SdmxWebManager;
import sdmxdl.web.SdmxWebSource;

import java.awt.*;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author Philippe Charles
 * @since 2.2.0
 */
@ServiceProvider(service = IDataSourceProviderBuddy.class, supersedes = "be.nbb.demetra.dotstat.DotStatProviderBuddy")
public final class SdmxWebProviderBuddy implements IDataSourceProviderBuddy, IConfigurable {

    private final Configurator<SdmxWebProviderBuddy> configurator;

    private File customSources;

    private boolean curlBackend;

    @lombok.Getter
    private SdmxWebManager webManager;

    @lombok.Getter
    private Languages languages;

    public SdmxWebProviderBuddy() {
        this.configurator = createConfigurator();
        this.customSources = new File("");
        this.curlBackend = false;
        this.webManager = SdmxWebFactory.createManager(curlBackend);
        this.languages = Languages.ANY;
        lookupProvider().ifPresent(o -> {
            o.setSdmxManager(webManager);
            o.setLanguages(languages);
        });
    }

    @Override
    public @NonNull String getProviderName() {
        return SdmxWebProvider.NAME;
    }

    @Override
    public Image getIcon(int type, boolean opened) {
        return SdmxIcons.getDefaultIcon().getImage();
    }

    private Image getIcon(SdmxWebBean bean) {
        SdmxWebSource source = webManager.getSources().get(bean.getSource());
        return source != null ? ImageUtilities.icon2Image(SdmxIcons.getFavicon(source.getWebsite())) : null;
    }

    @Override
    public Image getIcon(@NonNull DataSource dataSource, int type, boolean opened) {
        Optional<SdmxWebProvider> lookupProvider = lookupProvider();
        if (lookupProvider.isPresent()) {
            SdmxWebBean bean = lookupProvider.get().decodeBean(dataSource);
            Image result = getIcon(bean);
            if (result != null) {
                return result;
            }
        }
        return IDataSourceProviderBuddy.super.getIcon(dataSource, type, opened);
    }

    @Override
    public Image getIcon(DataSet dataSet, int type, boolean opened) {
        switch (dataSet.getKind()) {
            case COLLECTION:
                return ImageUtilities.loadImage("ec/nbdemetra/ui/nodes/folder.png", true);
            case SERIES:
                return ImageUtilities.loadImage("ec/nbdemetra/ui/nodes/chart_line.png", true);
            case DUMMY:
                return null;
        }
        return IDataSourceProviderBuddy.super.getIcon(dataSet, type, opened);
    }

    @Override
    public Image getIcon(@NonNull IOException ex, int type, boolean opened) {
        return ImageUtilities.loadImage("ec/nbdemetra/ui/nodes/exclamation-red.png", true);
    }

    @Override
    public Image getIcon(TsMoniker moniker, int type, boolean opened) {
        // Fix Demetra bug -->
        if (!SdmxWebProvider.NAME.equals(moniker.getSource())) {
            return IDataSourceProviderBuddy.super.getIcon(moniker, type, opened);
        }
        // <--

        Optional<SdmxWebProvider> lookupProvider = lookupProvider();
        if (lookupProvider.isPresent()) {
            SdmxWebProvider provider = lookupProvider.get();
            DataSet dataSet = provider.toDataSet(moniker);
            if (dataSet != null) {
                SdmxWebBean bean = provider.decodeBean(dataSet.getDataSource());
                Image result = getIcon(bean);
                if (result != null) {
                    return result;
                }
            }
        }
        return IDataSourceProviderBuddy.super.getIcon(moniker, type, opened);
    }

    @Override
    public boolean editBean(@NonNull String title, @NonNull Object bean) throws IntrospectionException {
        if (bean instanceof SdmxWebBean) {
            Optional<SdmxWebProvider> provider = lookupProvider();
            if (provider.isPresent()) {
                SdmxWebProvider o = provider.get();
                return new PropertySheetDialogBuilder()
                        .title(title)
                        .icon(getIcon(BeanInfo.ICON_COLOR_16x16, false))
                        .editSheet(SdmxWebBeanSupport.newSheet((SdmxWebBean) bean, o));
            }
        }
        return IDataSourceProviderBuddy.super.editBean(title, bean);
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
        OptionsDisplayer.getDefault().open(DotStatOptionsPanelController.ID);
        return config;
    }

    //<editor-fold defaultstate="collapsed" desc="Implementation details">
    private static Optional<SdmxWebProvider> lookupProvider() {
        return Optional.ofNullable(Lookup.getDefault().lookup(SdmxWebProvider.class));
    }

    private static Configurator<SdmxWebProviderBuddy> createConfigurator() {
        return new BuddyConfigHandler().toConfigurator(BuddyConfig.converter());
    }

    private static List<SdmxWebSource> loadSources(File file) {
        if (file.exists()) {
            try {
                return XmlWebSource.getParser().parseFile(file);
            } catch (IOException ex) {
                StatusDisplayer.getDefault().setStatusText(ex.getMessage());
            }
        }
        return Collections.emptyList();
    }

    private static final class BuddyConfigHandler extends BeanHandler<BuddyConfig, SdmxWebProviderBuddy> {

        @Override
        public @NonNull BuddyConfig loadBean(SdmxWebProviderBuddy resource) {
            BuddyConfig result = new BuddyConfig();
            result.setCustomSources(resource.customSources);
            result.setCurlBackend(resource.curlBackend);
            result.setPreferredLanguage(resource.getLanguages().toString());
            lookupProvider().ifPresent(provider -> {
                result.setDisplayCodes(provider.isDisplayCodes());
            });
            return result;
        }

        @Override
        public void storeBean(SdmxWebProviderBuddy resource, BuddyConfig bean) {
            resource.customSources = bean.getCustomSources();
            resource.curlBackend = bean.isCurlBackend();
            resource.webManager = SdmxWebFactory.createManager(resource.curlBackend)
                    .toBuilder()
                    .customSources(loadSources(resource.customSources))
                    .build();
            resource.languages = Parser.of(Languages::parse).parseValue(bean.getPreferredLanguage()).orElse(Languages.ANY);
            lookupProvider().ifPresent(provider -> {
                provider.setDisplayCodes(bean.isDisplayCodes());
                provider.setSdmxManager(resource.webManager);
            });
        }
    }
    //</editor-fold>
}
