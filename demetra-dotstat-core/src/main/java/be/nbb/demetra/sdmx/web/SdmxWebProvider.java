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
package be.nbb.demetra.sdmx.web;

import be.nbb.demetra.sdmx.HasSdmxProperties;
import internal.sdmx.SdmxCubeAccessor;
import be.nbb.sdmx.facade.DataflowRef;
import be.nbb.sdmx.facade.LanguagePriorityList;
import be.nbb.sdmx.facade.SdmxConnection;
import be.nbb.sdmx.facade.SdmxConnectionSupplier;
import be.nbb.sdmx.facade.util.IO;
import be.nbb.sdmx.facade.web.SdmxWebManager;
import com.google.common.cache.Cache;
import ec.tss.ITsProvider;
import ec.tss.tsproviders.DataSet;
import ec.tss.tsproviders.DataSource;
import ec.tss.tsproviders.HasDataMoniker;
import ec.tss.tsproviders.HasDataSourceBean;
import ec.tss.tsproviders.HasDataSourceMutableList;
import ec.tss.tsproviders.IDataSourceLoader;
import ec.tss.tsproviders.cube.CubeAccessor;
import ec.tss.tsproviders.cube.CubeId;
import ec.tss.tsproviders.cube.CubeSupport;
import ec.tss.tsproviders.cursor.HasTsCursor;
import ec.tss.tsproviders.utils.DataSourcePreconditions;
import ec.tss.tsproviders.utils.IParam;
import ec.tstoolkit.utilities.GuavaCaches;
import internal.sdmx.SdmxCubeItems;
import internal.sdmx.SdmxPropertiesSupport;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.openide.util.lookup.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Philippe Charles
 * @since 2.2.0
 */
@ServiceProvider(service = ITsProvider.class, supersedes = "be.nbb.demetra.dotstat.DotStatProvider")
public final class SdmxWebProvider implements IDataSourceLoader, HasSdmxProperties {

    public static final String NAME = "DOTSTAT";

    private final AtomicBoolean displayCodes;

    @lombok.experimental.Delegate
    private final HasSdmxProperties properties;

    @lombok.experimental.Delegate
    private final HasDataSourceMutableList mutableListSupport;

    @lombok.experimental.Delegate
    private final HasDataMoniker monikerSupport;

    @lombok.experimental.Delegate
    private final HasDataSourceBean<SdmxWebBean> beanSupport;

    @lombok.experimental.Delegate(excludes = HasTsCursor.class)
    private final CubeSupport cubeSupport;

    @lombok.experimental.Delegate
    private final ITsProvider tsSupport;

    public SdmxWebProvider() {
        this.displayCodes = new AtomicBoolean(false);

        Cache<DataSource, SdmxCubeItems> cache = GuavaCaches.softValuesCache();
        Logger logger = LoggerFactory.getLogger(NAME);
        SdmxWebParam beanParam = new SdmxWebParam.V1();

        this.properties = SdmxPropertiesSupport.of(SdmxWebManager::ofServiceLoader, cache::invalidateAll, () -> LanguagePriorityList.ANY, cache::invalidateAll);
        this.mutableListSupport = HasDataSourceMutableList.of(NAME, logger, cache::invalidate);
        this.monikerSupport = HasDataMoniker.usingUri(NAME);
        this.beanSupport = HasDataSourceBean.of(NAME, beanParam, beanParam.getVersion());
        this.cubeSupport = CubeSupport.of(new SdmxCubeResource(cache, properties, beanParam));
        this.tsSupport = CubeSupport.asTsProvider(NAME, logger, cubeSupport, monikerSupport, cache::invalidateAll);
    }

    @Override
    public String getDisplayName() {
        return "SDMX Web Services";
    }

    public boolean isDisplayCodes() {
        return displayCodes.get();
    }

    public void setDisplayCodes(boolean displayCodes) {
        boolean old = this.displayCodes.get();
        if (this.displayCodes.compareAndSet(old, displayCodes)) {
            clearCache();
        }
    }

    @lombok.AllArgsConstructor
    private static final class SdmxCubeResource implements CubeSupport.Resource {

        private final Cache<DataSource, SdmxCubeItems> cache;
        private final HasSdmxProperties properties;
        private final SdmxWebParam param;

        @Override
        public CubeAccessor getAccessor(DataSource dataSource) throws IOException {
            return get(dataSource).getAccessor();
        }

        @Override
        public IParam<DataSet, CubeId> getIdParam(DataSource dataSource) throws IOException {
            return get(dataSource).getIdParam();
        }

        private SdmxCubeItems get(DataSource dataSource) throws IOException {
            DataSourcePreconditions.checkProvider(NAME, dataSource);
            return GuavaCaches.getOrThrowIOException(cache, dataSource, () -> of(properties, param, dataSource));
        }

        private static SdmxCubeItems of(HasSdmxProperties properties, SdmxWebParam param, DataSource dataSource) throws IllegalArgumentException, IOException {
            SdmxWebBean bean = param.get(dataSource);

            DataflowRef flow = DataflowRef.parse(bean.getFlow());

            IO.Supplier<SdmxConnection> conn = getSupplier(properties, bean.getSource());

            CubeId root = SdmxCubeItems.getOrLoadRoot(bean.getDimensions(), conn, flow);

            CubeAccessor accessor = SdmxCubeAccessor.of(conn, flow, root, bean.getLabelAttribute(), bean.getSource())
                    .bulk(bean.getCacheDepth(), GuavaCaches.ttlCacheAsMap(bean.getCacheTtl()));

            IParam<DataSet, CubeId> idParam = param.getCubeIdParam(accessor.getRoot());

            return new SdmxCubeItems(accessor, idParam);
        }

        private static IO.Supplier<SdmxConnection> getSupplier(HasSdmxProperties properties, String name) {
            SdmxConnectionSupplier supplier = properties.getConnectionSupplier();
            LanguagePriorityList languages = properties.getLanguages();

            return () -> supplier.getConnection(name, languages);
        }
    }
}
