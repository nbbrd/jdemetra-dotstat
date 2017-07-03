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
package be.nbb.demetra.sdmx.webservice;

import be.nbb.demetra.sdmx.HasSdmxProperties;
import internal.sdmx.SdmxCubeAccessor;
import be.nbb.sdmx.facade.DataflowRef;
import be.nbb.sdmx.facade.LanguagePriorityList;
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
import internal.sdmx.SdmxCubeItems;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.openide.util.lookup.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Philippe Charles
 * @since 2.2.0
 */
@ServiceProvider(service = ITsProvider.class, supersedes = "be.nbb.demetra.dotstat.DotStatProvider")
public final class SdmxWebServiceProvider implements IDataSourceLoader, HasSdmxProperties {

    public static final String NAME = "DOTSTAT";

    private final AtomicReference<SdmxConnectionSupplier> connectionSupplier;
    private final AtomicReference<LanguagePriorityList> languages;
    private final AtomicBoolean displayCodes;

    @lombok.experimental.Delegate
    private final HasDataSourceMutableList mutableListSupport;

    @lombok.experimental.Delegate
    private final HasDataMoniker monikerSupport;

    @lombok.experimental.Delegate
    private final HasDataSourceBean<SdmxWebServiceBean> beanSupport;

    @lombok.experimental.Delegate(excludes = HasTsCursor.class)
    private final CubeSupport cubeSupport;

    @lombok.experimental.Delegate
    private final ITsProvider tsSupport;

    public SdmxWebServiceProvider() {
        this.connectionSupplier = new AtomicReference<>(SdmxDriverManager.getDefault());
        this.languages = new AtomicReference<>(LanguagePriorityList.ANY);
        this.displayCodes = new AtomicBoolean(false);

        Cache<DataSource, SdmxCubeItems> cache = GuavaCaches.softValuesCache();
        Logger logger = LoggerFactory.getLogger(NAME);
        SdmxWebServiceParam beanParam = new SdmxWebServiceParam.V1();

        this.mutableListSupport = HasDataSourceMutableList.of(NAME, logger, cache::invalidate);
        this.monikerSupport = HasDataMoniker.usingUri(NAME);
        this.beanSupport = HasDataSourceBean.of(NAME, beanParam, beanParam.getVersion());
        this.cubeSupport = CubeSupport.of(new SdmxCubeResource(cache, connectionSupplier, languages, beanParam));
        this.tsSupport = CubeSupport.asTsProvider(NAME, logger, cubeSupport, monikerSupport, cache::invalidateAll);
    }

    @Override
    public String getDisplayName() {
        return "DotStat";
    }

    @Override
    public SdmxConnectionSupplier getConnectionSupplier() {
        return connectionSupplier.get();
    }

    @Override
    public void setConnectionSupplier(SdmxConnectionSupplier connectionSupplier) {
        SdmxConnectionSupplier old = this.connectionSupplier.get();
        if (this.connectionSupplier.compareAndSet(old, connectionSupplier != null ? connectionSupplier : SdmxDriverManager.getDefault())) {
            clearCache();
        }
    }

    @Override
    public LanguagePriorityList getLanguages() {
        return languages.get();
    }

    @Override
    public void setLanguages(LanguagePriorityList languages) {
        LanguagePriorityList old = this.languages.get();
        if (this.languages.compareAndSet(old, languages != null ? languages : LanguagePriorityList.ANY)) {
            clearCache();
        }
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
        private final AtomicReference<SdmxConnectionSupplier> supplier;
        private final AtomicReference<LanguagePriorityList> languages;
        private final SdmxWebServiceParam param;

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
            return GuavaCaches.getOrThrowIOException(cache, dataSource, () -> of(supplier.get(), languages.get(), param, dataSource));
        }

        private static SdmxCubeItems of(SdmxConnectionSupplier supplier, LanguagePriorityList languages, SdmxWebServiceParam param, DataSource dataSource) throws IllegalArgumentException, IOException {
            SdmxWebServiceBean bean = param.get(dataSource);

            DataflowRef flow = DataflowRef.parse(bean.getFlow());

            List<String> dimensions = bean.getDimensions();
            if (dimensions.isEmpty()) {
                dimensions = SdmxCubeItems.getDefaultDimIds(supplier, languages, bean.getSource(), flow);
            }

            CubeAccessor accessor = SdmxCubeAccessor.of(supplier, languages, bean.getSource(), flow, dimensions, bean.getLabelAttribute())
                    .bulk(bean.getCacheDepth(), GuavaCaches.ttlCacheAsMap(bean.getCacheTtl()));

            IParam<DataSet, CubeId> idParam = param.getCubeIdParam(accessor.getRoot());

            return new SdmxCubeItems(accessor, idParam);
        }
    }
}
