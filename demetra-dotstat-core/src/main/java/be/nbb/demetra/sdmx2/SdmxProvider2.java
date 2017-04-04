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
package be.nbb.demetra.sdmx2;

import internal.sdmx.SdmxCubeAccessor;
import be.nbb.sdmx.facade.DataflowRef;
import be.nbb.sdmx.facade.Dimension;
import be.nbb.sdmx.facade.SdmxConnection;
import be.nbb.sdmx.facade.SdmxConnectionSupplier;
import be.nbb.sdmx.facade.file.FileSdmxDriver;
import com.google.common.cache.Cache;
import ec.tss.ITsProvider;
import ec.tss.tsproviders.DataSet;
import ec.tss.tsproviders.DataSource;
import ec.tss.tsproviders.HasDataMoniker;
import ec.tss.tsproviders.HasDataSourceBean;
import ec.tss.tsproviders.HasDataSourceMutableList;
import ec.tss.tsproviders.HasFilePaths;
import ec.tss.tsproviders.IFileLoader;
import ec.tss.tsproviders.cube.CubeAccessor;
import ec.tss.tsproviders.cube.CubeId;
import ec.tss.tsproviders.cube.CubeSupport;
import ec.tss.tsproviders.cursor.HasTsCursor;
import ec.tss.tsproviders.utils.IParam;
import ec.tstoolkit.utilities.GuavaCaches;
import it.bancaditalia.oss.sdmx.util.Configuration;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Philippe Charles
 * @since 2.2.0
 */
//@ServiceProvider(service = ITsProvider.class)
public final class SdmxProvider2 implements IFileLoader {

    private static final String NAME = "TSProviders.Sdmx.SdmxProvider";

    @lombok.experimental.Delegate
    private final HasDataSourceMutableList mutableListSupport;

    @lombok.experimental.Delegate
    private final HasDataMoniker monikerSupport;

    @lombok.experimental.Delegate
    private final HasDataSourceBean<SdmxBean2> beanSupport;

    @lombok.experimental.Delegate
    private final HasFilePaths filePathSupport;

    @lombok.experimental.Delegate(excludes = HasTsCursor.class)
    private final CubeSupport cubeSupport;

    @lombok.experimental.Delegate
    private final ITsProvider tsSupport;

    public SdmxProvider2() {
        Logger logger = LoggerFactory.getLogger(NAME);
        Cache<DataSource, CubeAccessor> cache = GuavaCaches.softValuesCache();
        SdmxParam sdmxParam = new SdmxParam.V1();

        this.mutableListSupport = HasDataSourceMutableList.of(NAME, logger, cache::invalidate);
        this.monikerSupport = HasDataMoniker.usingUri(NAME);
        this.beanSupport = HasDataSourceBean.of(NAME, sdmxParam, sdmxParam.getVersion());
        this.filePathSupport = HasFilePaths.of(cache::invalidateAll);
        this.cubeSupport = CubeSupport.of(new SdmxCubeResource(cache, beanSupport::decodeBean));
        this.tsSupport = CubeSupport.asTsProvider(NAME, logger, cubeSupport, monikerSupport, cache::invalidateAll);
    }

    @Override
    public String getFileDescription() {
        return "SDMX file";
    }

    @Override
    public boolean accept(File pathname) {
        return pathname.getName().toLowerCase().endsWith(".xml");
    }

    @Nonnull
    public String getPreferredLanguage() {
        return Configuration.getLang();
    }

    public void setPreferredLanguage(@Nullable String lang) {
        Configuration.setLang(lang != null ? lang : "en");
    }

    private static final class SdmxCubeResource implements CubeSupport.Resource {

        private final Cache<DataSource, CubeAccessor> cache;
        private final Function<DataSource, SdmxBean2> decoder;

        public SdmxCubeResource(Cache<DataSource, CubeAccessor> cache, Function<DataSource, SdmxBean2> decoder) {
            this.cache = cache;
            this.decoder = decoder;
        }

        @Override
        public CubeAccessor getAccessor(DataSource dataSource) throws IOException {
            return GuavaCaches.getOrThrowIOException(cache, dataSource, () -> load(dataSource));
        }

        @Override
        public IParam<DataSet, CubeId> getIdParam(DataSource dataSource) throws IOException {
            CubeId root = getAccessor(dataSource).getRoot();
            // FIXME: compatibility with previous code
            return CubeSupport.idByName(root);
        }

        private static final FileSdmxDriver DRIVER = new FileSdmxDriver();

        private CubeAccessor load(DataSource key) throws IOException {
            SdmxBean2 bean = decoder.apply(key);
            SdmxConnection conn = DRIVER.connect(URI.create("sdmx:" + bean.getFile().toURI().toString()), new Properties());
            DataflowRef flowRef = conn.getDataflows().iterator().next().getFlowRef();
            Set<Dimension> dimensions = conn.getDataStructure(flowRef).getDimensions();
            List<String> dimensionIds = dimensions.stream()
                    .sorted(Comparator.comparing(Dimension::getPosition))
                    .map(Dimension::getId)
                    .collect(Collectors.toList());
            return SdmxCubeAccessor.create(new Stuff(conn), "???", flowRef, dimensionIds);
        }
    }

    private static final class Stuff implements SdmxConnectionSupplier {

        private final SdmxConnection single;

        public Stuff(SdmxConnection single) {
            this.single = single;
        }

        @Override
        public SdmxConnection getConnection(String name) {
            return single;
        }
    }
}
