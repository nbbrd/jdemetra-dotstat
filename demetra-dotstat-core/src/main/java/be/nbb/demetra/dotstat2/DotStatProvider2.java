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
package be.nbb.demetra.dotstat2;

import internal.sdmx.SdmxCubeAccessor;
import be.nbb.sdmx.facade.DataflowRef;
import be.nbb.sdmx.facade.SdmxConnectionSupplier;
import be.nbb.sdmx.facade.driver.SdmxDriverManager;
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
import it.bancaditalia.oss.sdmx.util.Configuration;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Philippe Charles
 * @since 2.2.0
 */
//@ServiceProvider(service = ITsProvider.class, supersedes = "be.nbb.demetra.dotstat.DotStatProvider")
public final class DotStatProvider2 implements IDataSourceLoader {

    private static final String NAME = "DOTSTAT";

    private final AtomicReference<SdmxConnectionSupplier> connectionSupplier;
    private final AtomicBoolean displayCodes;

    @lombok.experimental.Delegate
    private final HasDataSourceMutableList mutableListSupport;

    @lombok.experimental.Delegate
    private final HasDataMoniker monikerSupport;

    @lombok.experimental.Delegate
    private final HasDataSourceBean<DotStatBean2> beanSupport;

    @lombok.experimental.Delegate(excludes = HasTsCursor.class)
    private final CubeSupport cubeSupport;

    @lombok.experimental.Delegate
    private final ITsProvider tsSupport;

    public DotStatProvider2() {
        this.connectionSupplier = new AtomicReference(SdmxDriverManager.getDefault());
        this.displayCodes = new AtomicBoolean(false);

        Cache<DataSource, CubeAccessor> cache = GuavaCaches.softValuesCache();
        Logger logger = LoggerFactory.getLogger(NAME);
        DotStatParam beanParam = new DotStatParam.V1();

        this.mutableListSupport = HasDataSourceMutableList.of(NAME, logger, cache::invalidate);
        this.monikerSupport = HasDataMoniker.usingUri(NAME);
        this.beanSupport = HasDataSourceBean.of(NAME, beanParam, beanParam.getVersion());
        this.cubeSupport = CubeSupport.of(new DotStatCubeResource(cache, connectionSupplier, beanParam));
        this.tsSupport = CubeSupport.asTsProvider(NAME, logger, cubeSupport, monikerSupport, cache::invalidateAll);
    }

    @Override
    public String getDisplayName() {
        return "DotStat";
    }

    @Nonnull
    public SdmxConnectionSupplier getConnectionSupplier() {
        return connectionSupplier.get();
    }

    public void setConnectionSupplier(@Nullable SdmxConnectionSupplier connectionSupplier) {
        SdmxConnectionSupplier old = this.connectionSupplier.get();
        if (this.connectionSupplier.compareAndSet(old, connectionSupplier != null ? connectionSupplier : SdmxDriverManager.getDefault())) {
            clearCache();
        }
    }

    @Nonnull
    public String getPreferredLanguage() {
        return Configuration.getLang();
    }

    public void setPreferredLanguage(@Nullable String lang) {
        Configuration.setLang(lang != null ? lang : "en");
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

    private static final class DotStatCubeResource implements CubeSupport.Resource {

        private final Cache<DataSource, CubeAccessor> cache;
        private final AtomicReference<SdmxConnectionSupplier> supplier;
        private final DotStatParam beanParam;

        public DotStatCubeResource(Cache<DataSource, CubeAccessor> cache, AtomicReference<SdmxConnectionSupplier> supplier, DotStatParam beanParam) {
            this.cache = cache;
            this.supplier = supplier;
            this.beanParam = beanParam;
        }

        @Override
        public CubeAccessor getAccessor(DataSource dataSource) throws IOException {
            DataSourcePreconditions.checkProvider(NAME, dataSource);
            return GuavaCaches.getOrThrowIOException(cache, dataSource, () -> load(supplier.get(), dataSource));
        }

        @Override
        public IParam<DataSet, CubeId> getIdParam(DataSource dataSource) throws IOException {
            return beanParam.getCubeIdParam(dataSource);
        }

        private CubeAccessor load(SdmxConnectionSupplier supplier, DataSource key) throws IllegalArgumentException {
            DotStatBean2 bean = beanParam.get(key);
            CubeAccessor accessor = SdmxCubeAccessor.create(supplier, bean.getDbName(), DataflowRef.parse(bean.getFlowRef()), bean.getDimensionIds());
            return accessor.bulk(bean.getCacheDepth(), GuavaCaches.ttlCacheAsMap(bean.getCacheTtl()));
        }
    }
}
