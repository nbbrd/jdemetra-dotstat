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
package be.nbb.demetra.sdmx.webservice;

import be.nbb.demetra.dotstat.DotStatOptionsPanelController;
import be.nbb.demetra.dotstat.DotStatProviderBuddy.BuddyConfig;
import be.nbb.sdmx.facade.SdmxConnectionSupplier;
import be.nbb.sdmx.facade.driver.SdmxDriverManager;
import be.nbb.sdmx.facade.util.HasCache;
import ec.nbdemetra.db.DbIcon;
import ec.nbdemetra.ui.BeanHandler;
import ec.nbdemetra.ui.Config;
import ec.nbdemetra.ui.Configurator;
import ec.nbdemetra.ui.IConfigurable;
import ec.nbdemetra.ui.properties.PropertySheetDialogBuilder;
import ec.nbdemetra.ui.tsproviders.IDataSourceProviderBuddy;
import ec.tss.tsproviders.DataSet;
import ec.tss.tsproviders.TsProviders;
import ec.tstoolkit.utilities.GuavaCaches;
import java.awt.Image;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import org.netbeans.api.options.OptionsDisplayer;
import org.openide.nodes.Sheet;
import org.openide.util.ImageUtilities;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Philippe Charles
 * @since 2.2.0
 */
@ServiceProvider(service = IDataSourceProviderBuddy.class, supersedes = "be.nbb.demetra.dotstat.DotStatProviderBuddy")
public final class SdmxWebServiceProviderBuddy implements IDataSourceProviderBuddy, IConfigurable {

    private static final String COLLECTION_ICON = "ec/nbdemetra/ui/nodes/folder.png";
    private static final String SERIES_ICON = "ec/nbdemetra/ui/nodes/chart_line.png";
    private static final String EXCEPTION_ICON = "ec/nbdemetra/ui/nodes/exclamation-red.png";

    private final Configurator<SdmxWebServiceProviderBuddy> configurator;
    private final SdmxConnectionSupplier supplier;

    public SdmxWebServiceProviderBuddy() {
        this.configurator = createConfigurator();
        this.supplier = SdmxDriverManager.getDefault();
        initDriverCache();
    }

    @Override
    public String getProviderName() {
        return SdmxWebServiceProvider.NAME;
    }

    @Override
    public Image getIcon(int type, boolean opened) {
        return DbIcon.DATABASE.getImageIcon().getImage();
    }

    @Override
    public Image getIcon(DataSet dataSet, int type, boolean opened) {
        switch (dataSet.getKind()) {
            case COLLECTION:
                return ImageUtilities.loadImage(COLLECTION_ICON, true);
            case SERIES:
                return ImageUtilities.loadImage(SERIES_ICON, true);
            case DUMMY:
                return null;
        }
        return IDataSourceProviderBuddy.super.getIcon(dataSet, type, opened);
    }

    @Override
    public Image getIcon(IOException ex, int type, boolean opened) {
        return ImageUtilities.loadImage(EXCEPTION_ICON, true);
    }

    @Override
    public boolean editBean(String title, Object bean) throws IntrospectionException {
        if (bean instanceof SdmxWebServiceBean) {
            Sheet sheet = Util.createSheet((SdmxWebServiceBean) bean, supplier);
            Image image = getIcon(BeanInfo.ICON_COLOR_16x16, false);
            return new PropertySheetDialogBuilder().title(title).icon(image).editSheet(sheet);
        }
        return IDataSourceProviderBuddy.super.editBean(title, bean);
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

    private static Configurator<SdmxWebServiceProviderBuddy> createConfigurator() {
        return new BuddyConfigHandler().toConfigurator(BuddyConfig.converter());
    }

    private static Optional<SdmxWebServiceProvider> lookupProvider() {
        return TsProviders.lookup(SdmxWebServiceProvider.class, SdmxWebServiceProvider.NAME).toJavaUtil();
    }

    private static final class BuddyConfigHandler extends BeanHandler<BuddyConfig, SdmxWebServiceProviderBuddy> {

        @Override
        public BuddyConfig loadBean(SdmxWebServiceProviderBuddy resource) {
            BuddyConfig result = new BuddyConfig();
            lookupProvider().ifPresent(o -> {
                result.setPreferredLanguage(o.getPreferredLanguage());
                result.setDisplayCodes(o.isDisplayCodes());
            });
            return result;
        }

        @Override
        public void storeBean(SdmxWebServiceProviderBuddy resource, BuddyConfig bean) {
            lookupProvider().ifPresent(o -> {
                o.setPreferredLanguage(bean.getPreferredLanguage());
                o.setDisplayCodes(bean.isDisplayCodes());
            });
        }
    }

    private static void initDriverCache() {
        ConcurrentMap cache = GuavaCaches.softValuesCacheAsMap();
        SdmxDriverManager.getDefault().getDrivers().stream()
                .filter(o -> (o instanceof HasCache))
                .forEach(o -> ((HasCache) o).setCache(cache));
    }
}
