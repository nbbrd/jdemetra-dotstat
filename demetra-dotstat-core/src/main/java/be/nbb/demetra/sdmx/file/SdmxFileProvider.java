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
package be.nbb.demetra.sdmx.file;

import be.nbb.sdmx.facade.DataStructure;
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
import ec.tss.tsproviders.utils.DataSourcePreconditions;
import ec.tss.tsproviders.utils.IParam;
import ec.tstoolkit.utilities.GuavaCaches;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
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
public final class SdmxFileProvider implements IFileLoader {

    private static final String NAME = "sdmx-file";

    private final AtomicReference<String> preferredLanguage;

    @lombok.experimental.Delegate
    private final HasDataSourceMutableList mutableListSupport;

    @lombok.experimental.Delegate
    private final HasDataMoniker monikerSupport;

    @lombok.experimental.Delegate
    private final HasDataSourceBean<SdmxFileBean> beanSupport;

    @lombok.experimental.Delegate
    private final HasFilePaths filePathSupport;

    @lombok.experimental.Delegate(excludes = HasTsCursor.class)
    private final CubeSupport cubeSupport;

    @lombok.experimental.Delegate
    private final ITsProvider tsSupport;

    public SdmxFileProvider() {
        this.preferredLanguage = new AtomicReference<>("en");

        Logger logger = LoggerFactory.getLogger(NAME);
        Cache<DataSource, CubeAccessor> cache = GuavaCaches.softValuesCache();
        SdmxFileParam sdmxParam = new SdmxFileParam.V1();

        this.mutableListSupport = HasDataSourceMutableList.of(NAME, logger, cache::invalidate);
        this.monikerSupport = HasDataMoniker.usingUri(NAME);
        this.beanSupport = HasDataSourceBean.of(NAME, sdmxParam, sdmxParam.getVersion());
        this.filePathSupport = HasFilePaths.of(cache::invalidateAll);
        this.cubeSupport = CubeSupport.of(new SdmxCubeResource(cache, sdmxParam, filePathSupport));
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
        return preferredLanguage.get();
    }

    public void setPreferredLanguage(@Nullable String lang) {
        preferredLanguage.set(lang != null ? lang : "en");
    }

    private static final class SdmxCubeResource implements CubeSupport.Resource {

        private final Cache<DataSource, CubeAccessor> cache;
        private final SdmxFileParam sdmxParam;
        private final HasFilePaths paths;

        SdmxCubeResource(Cache<DataSource, CubeAccessor> cache, SdmxFileParam param, HasFilePaths paths) {
            this.cache = cache;
            this.sdmxParam = param;
            this.paths = paths;
        }

        @Override
        public CubeAccessor getAccessor(DataSource dataSource) throws IOException {
            DataSourcePreconditions.checkProvider(NAME, dataSource);
            return GuavaCaches.getOrThrowIOException(cache, dataSource, () -> of(paths, sdmxParam.get(dataSource)));
        }

        @Override
        public IParam<DataSet, CubeId> getIdParam(DataSource dataSource) throws IOException {
            CubeId root = getAccessor(dataSource).getRoot();
            return CubeSupport.idBySeparator(root, ".", "k");
        }

        private static CubeAccessor of(HasFilePaths paths, SdmxFileBean bean) throws IOException {
            FileSupplier supplier = FileSupplier.of(paths, bean);
            DataflowRef flow;
            List<String> dimensions;
            try (SdmxConnection conn = supplier.getConnection("")) {
                flow = conn.getDataflows().iterator().next().getFlowRef();
                dimensions = getDimensionIds(conn.getDataStructure(flow));
            }
            return SdmxCubeAccessor.create(supplier, "???", flow, dimensions, bean.getLabelAttribute());
        }

        private static List<String> getDimensionIds(DataStructure dataStructure) {
            return dataStructure.getDimensions().stream()
                    .sorted(Comparator.comparing(Dimension::getPosition))
                    .map(Dimension::getId)
                    .collect(Collectors.toList());
        }
    }

    private static final FileSdmxDriver DRIVER = new FileSdmxDriver();

    private static final class FileSupplier implements SdmxConnectionSupplier {

        static FileSupplier of(HasFilePaths paths, SdmxFileBean bean) throws FileNotFoundException {
            return new FileSupplier(getUri(paths, bean), getProperties(paths, bean));
        }

        private final URI uri;
        private final Map<?, ?> properties;

        private FileSupplier(URI uri, Map<?, ?> properties) {
            this.uri = uri;
            this.properties = properties;
        }

        @Override
        public SdmxConnection getConnection(String name) throws IOException {
            return DRIVER.connect(uri, properties);
        }

        private static URI getUri(HasFilePaths paths, SdmxFileBean bean) throws FileNotFoundException {
            File target = paths.resolveFilePath(bean.getFile());
            return URI.create("sdmx:" + target.toURI().toString());
        }

        private static Properties getProperties(HasFilePaths paths, SdmxFileBean bean) throws FileNotFoundException {
            Properties p = new Properties();
            File structureFile = bean.getStructureFile();
            if (!structureFile.toString().isEmpty()) {
                p.put(p, paths.resolveFilePath(structureFile).toString());
            }
            return p;
        }
    }
}
