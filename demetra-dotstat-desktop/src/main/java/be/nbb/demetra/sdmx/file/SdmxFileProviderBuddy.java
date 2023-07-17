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

import ec.nbdemetra.ui.Config;
import ec.nbdemetra.ui.IConfigurable;
import ec.nbdemetra.ui.properties.PropertySheetDialogBuilder;
import ec.nbdemetra.ui.tsproviders.IDataSourceProviderBuddy;
import ec.tss.tsproviders.DataSet;
import internal.sdmx.SdmxIcons;
import lombok.AccessLevel;
import lombok.NonNull;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

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

    @lombok.Getter(AccessLevel.PACKAGE)
    @lombok.Setter(AccessLevel.PACKAGE)
    private SdmxFileConfiguration configuration;

    public SdmxFileProviderBuddy() {
        this.configuration = new SdmxFileConfiguration();
        updateProvider();
    }

    private void updateProvider() {
        lookupProvider().ifPresent(provider -> {
            provider.setSdmxManager(configuration.toSdmxFileManager());
            provider.setLanguages(configuration.toLanguages());
        });
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
                return new PropertySheetDialogBuilder()
                        .title(title)
                        .icon(getIcon(BeanInfo.ICON_COLOR_16x16, false))
                        .editSheet(SdmxFileBeanSupport.newSheet((SdmxFileBean) bean, provider.get()));
            }
        }
        return IDataSourceProviderBuddy.super.editBean(title, bean);
    }

    @Override
    public @NonNull Config getConfig() {
        return SdmxFileConfiguration.CONFIGURATOR.getConfig(this);
    }

    @Override
    public void setConfig(@NonNull Config config) throws IllegalArgumentException {
        SdmxFileConfiguration.CONFIGURATOR.setConfig(this, config);
    }

    @Override
    public @NonNull Config editConfig(@NonNull Config config) throws IllegalArgumentException {
        return SdmxFileConfiguration.CONFIGURATOR.editConfig(config);
    }

    private static Optional<SdmxFileProvider> lookupProvider() {
        return Optional.ofNullable(Lookup.getDefault().lookup(SdmxFileProvider.class));
    }
}
