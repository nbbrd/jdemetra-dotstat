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
import com.google.common.cache.Cache;
import ec.tss.ITsProvider;
import ec.tss.tsproviders.*;
import ec.tss.tsproviders.cube.CubeAccessor;
import ec.tss.tsproviders.cube.CubeId;
import ec.tss.tsproviders.cube.CubeSupport;
import ec.tss.tsproviders.cursor.HasTsCursor;
import ec.tss.tsproviders.utils.DataSourcePreconditions;
import ec.tss.tsproviders.utils.IParam;
import ec.tstoolkit.utilities.GuavaCaches;
import internal.sdmx.SdmxCubeAccessor;
import internal.sdmx.SdmxCubeItems;
import internal.sdmx.SdmxPropertiesSupport;
import lombok.NonNull;
import org.openide.util.lookup.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sdmxdl.Connection;
import sdmxdl.FlowRef;
import sdmxdl.web.SdmxWebManager;
import standalone_sdmxdl.nbbrd.io.function.IOSupplier;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

/**
 * @author Philippe Charles
 * @since 2.2.0
 */
@ServiceProvider(service = ITsProvider.class, supersedes = "be.nbb.demetra.dotstat.DotStatProvider")
public final class SdmxWebProvider implements IDataSourceLoader, HasSdmxProperties<SdmxWebManager> {

    public static final String NAME = "DOTSTAT";

    private final AtomicBoolean displayCodes;

    @lombok.experimental.Delegate
    private final HasSdmxProperties<SdmxWebManager> properties;

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

        this.properties = SdmxPropertiesSupport.of(SdmxWebManager::ofServiceLoader, cache::invalidateAll);
        this.mutableListSupport = HasDataSourceMutableList.of(NAME, logger, cache::invalidate);
        this.monikerSupport = HasDataMoniker.usingUri(NAME);
        this.beanSupport = HasDataSourceBean.of(NAME, beanParam, beanParam.getVersion());
        this.cubeSupport = CubeSupport.of(new SdmxCubeResource(cache, properties, beanParam, displayCodes::get));
        this.tsSupport = CubeSupport.asTsProvider(NAME, logger, cubeSupport, monikerSupport, cache::invalidateAll);
    }

    @Override
    public @NonNull String getDisplayName() {
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
        private final HasSdmxProperties<SdmxWebManager> properties;
        private final SdmxWebParam param;
        private final BooleanSupplier displayCodes;

        @Override
        public @NonNull CubeAccessor getAccessor(@NonNull DataSource dataSource) throws IOException {
            return get(dataSource).getAccessor();
        }

        @Override
        public @NonNull IParam<DataSet, CubeId> getIdParam(@NonNull DataSource dataSource) throws IOException {
            return get(dataSource).getIdParam();
        }

        private SdmxCubeItems get(DataSource dataSource) throws IOException {
            DataSourcePreconditions.checkProvider(NAME, dataSource);
            return GuavaCaches.getOrThrowIOException(cache, dataSource, () -> of(properties, param, dataSource, displayCodes.getAsBoolean()));
        }

        private static SdmxCubeItems of(HasSdmxProperties<SdmxWebManager> properties, SdmxWebParam param, DataSource dataSource, boolean displayCodes) throws IllegalArgumentException, IOException {
            SdmxWebBean bean = param.get(dataSource);

            FlowRef flowRef = FlowRef.parse(bean.getFlow());

            IOSupplier<Connection> conn = toConnection(properties, bean.getSource());

            CubeAccessor accessor = SdmxCubeAccessor
                    .of(conn, flowRef, bean.getDimensions(), bean.getLabelAttribute(), bean.getSource(), displayCodes)
                    .bulk(bean.getCacheDepth(), GuavaCaches.ttlCacheAsMap(bean.getCacheTtl()));

            IParam<DataSet, CubeId> idParam = param.getCubeIdParam(accessor.getRoot());

            return new SdmxCubeItems(accessor, idParam);
        }

        private static IOSupplier<Connection> toConnection(HasSdmxProperties<SdmxWebManager> properties, String name) {
            SdmxWebManager manager = properties.getSdmxManager();
            return () -> manager.getConnection(name, properties.getLanguages());
        }
    }
}
