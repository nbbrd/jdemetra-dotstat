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
package be.nbb.demetra.sdmx.file;

import be.nbb.demetra.dotstat.DotStatOptionsPanelController;
import com.google.common.base.Converter;
import ec.nbdemetra.ui.BeanHandler;
import ec.nbdemetra.ui.Config;
import ec.nbdemetra.ui.Configurator;
import ec.nbdemetra.ui.IConfigurable;
import ec.nbdemetra.ui.properties.PropertySheetDialogBuilder;
import ec.nbdemetra.ui.tsproviders.IDataSourceProviderBuddy;
import ec.tss.tsproviders.DataSet;
import ec.tstoolkit.utilities.GuavaCaches;
import internal.sdmx.SdmxIcons;
import lombok.NonNull;
import org.netbeans.api.options.OptionsDisplayer;
import org.openide.awt.StatusDisplayer;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;
import sdmxdl.file.SdmxFileManager;
import sdmxdl.file.spi.FileCaching;
import sdmxdl.format.MemCachingSupport;

import java.awt.*;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.io.IOException;
import java.util.Optional;

/**
 * @author Philippe Charles
 * @since 2.2.1
 */
@ServiceProvider(service = IDataSourceProviderBuddy.class)
public final class SdmxFileProviderBuddy implements IDataSourceProviderBuddy, IConfigurable {

    private final Configurator<SdmxFileProviderBuddy> configurator;

    public SdmxFileProviderBuddy() {
        this.configurator = createConfigurator();
        lookupProvider().ifPresent(o -> o.setSdmxManager(createManager()));
    }

    @Override
    public @NonNull String getProviderName() {
        return SdmxFileProvider.NAME;
    }

    @Override
    public Image getIcon(int type, boolean opened) {
        return SdmxIcons.getDefaultIcon().getImage();
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
    public boolean editBean(@NonNull String title, @NonNull Object bean) throws IntrospectionException {
        if (bean instanceof SdmxFileBean) {
            Optional<SdmxFileProvider> provider = lookupProvider();
            if (provider.isPresent()) {
                return editBean(title, (SdmxFileBean) bean, provider.get());
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
    private static SdmxFileManager createManager() {
        return SdmxFileManager.ofServiceLoader()
                .toBuilder()
                .onEvent((src, marker, msg) -> StatusDisplayer.getDefault().setStatusText(msg.toString()))
                .caching(getCaching())
                .build();
    }

    private static FileCaching getCaching() {
        return MemCachingSupport
                .builder()
                .id("SHARED_SOFT_MEM")
                .repositoriesOf(GuavaCaches.softValuesCacheAsMap())
                .build();
    }

    private static Optional<SdmxFileProvider> lookupProvider() {
        return Optional.ofNullable(Lookup.getDefault().lookup(SdmxFileProvider.class));
    }

    private static Configurator<SdmxFileProviderBuddy> createConfigurator() {
        return new BuddyConfigHandler().toConfigurator(new BuddyConfigConverter());
    }

    private static final class BuddyConfig {

    }

    private static final class BuddyConfigHandler extends BeanHandler<BuddyConfig, SdmxFileProviderBuddy> {

        @Override
        public @NonNull BuddyConfig loadBean(@NonNull SdmxFileProviderBuddy resource) {
            return new BuddyConfig();
        }

        @Override
        public void storeBean(@NonNull SdmxFileProviderBuddy resource, @NonNull BuddyConfig bean) {
        }
    }

    private static final class BuddyConfigConverter extends Converter<BuddyConfig, Config> {

        @Override
        protected Config doForward(BuddyConfig a) {
            return Config.builder(Void.class.getName(), "INSTANCE", "2017").build();
        }

        @Override
        protected BuddyConfig doBackward(Config b) {
            return new BuddyConfig();
        }
    }

    private boolean editBean(String title, SdmxFileBean bean, SdmxFileProvider o) {
        return new PropertySheetDialogBuilder()
                .title(title)
                .icon(getIcon(BeanInfo.ICON_COLOR_16x16, false))
                .editSheet(SdmxFileBeanSupport.newSheet(bean, o));
    }
    //</editor-fold>
}
