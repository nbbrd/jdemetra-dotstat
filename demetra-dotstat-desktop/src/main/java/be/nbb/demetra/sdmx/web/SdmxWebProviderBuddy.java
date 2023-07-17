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

import ec.nbdemetra.ui.Config;
import ec.nbdemetra.ui.IConfigurable;
import ec.nbdemetra.ui.properties.PropertySheetDialogBuilder;
import ec.nbdemetra.ui.tsproviders.IDataSourceProviderBuddy;
import ec.tss.TsMoniker;
import ec.tss.tsproviders.DataSet;
import ec.tss.tsproviders.DataSource;
import ec.util.various.swing.FontAwesome;
import internal.sdmx.SdmxIcons;
import lombok.AccessLevel;
import lombok.NonNull;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;
import sdmxdl.Connection;
import sdmxdl.Feature;
import sdmxdl.web.SdmxWebSource;

import java.awt.*;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.io.IOException;
import java.util.Optional;

import static ec.util.chart.impl.TangoColorScheme.DARK_ORANGE;
import static ec.util.chart.swing.SwingColorSchemeSupport.rgbToColor;

/**
 * @author Philippe Charles
 * @since 2.2.0
 */
@ServiceProvider(service = IDataSourceProviderBuddy.class, supersedes = "be.nbb.demetra.dotstat.DotStatProviderBuddy")
public final class SdmxWebProviderBuddy implements IDataSourceProviderBuddy, IConfigurable {

    @lombok.Getter(AccessLevel.PACKAGE)
    @lombok.Setter(AccessLevel.PACKAGE)
    private SdmxWebConfiguration configuration;

    public SdmxWebProviderBuddy() {
        this.configuration = new SdmxWebConfiguration();
        updateProvider();
    }

    private void updateProvider() {
        lookupProvider().ifPresent(provider -> {
            provider.setSdmxManager(configuration.toSdmxWebManager());
            provider.setLanguages(configuration.toLanguages());
            provider.setDisplayCodes(configuration.isDisplayCodes());
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

    @Override
    public Image getIcon(@NonNull DataSource dataSource, int type, boolean opened) {
        Optional<SdmxWebProvider> lookupProvider = lookupProvider();
        if (lookupProvider.isPresent()) {
            SdmxWebProvider provider = lookupProvider.get();
            SdmxWebBean bean = provider.decodeBean(dataSource);
            SdmxWebSource source = provider.getSdmxManager().getSources().get(bean.getSource());
            if (source != null) {
                Image result = getSourceIcon(provider, source);
                return supportsDataQueryDetail(provider, source)
                        ? result
                        : ImageUtilities.mergeImages(result, getWarningBadge(), 13, 8);
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
                SdmxWebSource source = provider.getSdmxManager().getSources().get(bean.getSource());
                if (source != null) {
                    return getSourceIcon(provider, source);
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
        return SdmxWebConfiguration.CONFIGURATOR.getConfig(this);
    }

    @Override
    public void setConfig(@NonNull Config config) throws IllegalArgumentException {
        SdmxWebConfiguration.CONFIGURATOR.setConfig(this, config);
        updateProvider();
    }

    @Override
    public @NonNull Config editConfig(@NonNull Config config) throws IllegalArgumentException {
        return SdmxWebConfiguration.CONFIGURATOR.editConfig(config);
    }

    private static Image getSourceIcon(SdmxWebProvider provider, SdmxWebSource source) {
        return ImageUtilities.icon2Image(SdmxIcons.getFavicon(provider.getSdmxManager().getNetworking(), source.getWebsite()));
    }

    private static boolean supportsDataQueryDetail(SdmxWebProvider provider, SdmxWebSource source) {
        try (Connection conn = provider.getSdmxManager().getConnection(source, provider.getLanguages())) {
            return conn.getSupportedFeatures().contains(Feature.DATA_QUERY_DETAIL);
        } catch (IOException ex) {
            return false;
        }
    }

    private static Image getWarningBadge() {
        return FontAwesome.FA_EXCLAMATION_TRIANGLE.getImage(rgbToColor(DARK_ORANGE), 8f);
    }

    private static Optional<SdmxWebProvider> lookupProvider() {
        return Optional.ofNullable(Lookup.getDefault().lookup(SdmxWebProvider.class));
    }
}
