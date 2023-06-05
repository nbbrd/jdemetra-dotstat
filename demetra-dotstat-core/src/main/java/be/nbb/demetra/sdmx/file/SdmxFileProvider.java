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

import be.nbb.demetra.sdmx.HasSdmxProperties;
import internal.sdmx.SdmxCubeAccessor;
import sdmxdl.DataflowRef;
import sdmxdl.file.SdmxFileManager;
import sdmxdl.file.SdmxFileSource;
import com.google.common.cache.Cache;
import ec.tss.ITsProvider;
import ec.tss.tsproviders.DataSet;
import ec.tss.tsproviders.DataSource;
import ec.tss.tsproviders.HasDataDisplayName;
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
import internal.sdmx.SdmxCubeItems;
import internal.sdmx.SdmxPropertiesSupport;
import java.io.File;
import java.io.IOException;
import org.openide.util.lookup.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ec.tss.TsAsyncMode;
import ec.tss.tsproviders.cursor.TsCursorAsFiller;
import ec.tss.tsproviders.utils.TsFillerAsProvider;
import java.io.EOFException;
import java.util.Locale;

import nbbrd.io.function.IOSupplier;
import sdmxdl.Connection;

/**
 *
 * @author Philippe Charles
 * @since 2.2.0
 */
@ServiceProvider(service = ITsProvider.class)
public final class SdmxFileProvider implements IFileLoader, HasSdmxProperties<SdmxFileManager> {

    public static final String NAME = "sdmx-file";

    @lombok.experimental.Delegate
    private final HasSdmxProperties<SdmxFileManager> properties;

    @lombok.experimental.Delegate
    private final HasDataSourceMutableList mutableListSupport;

    @lombok.experimental.Delegate
    private final HasDataMoniker monikerSupport;

    @lombok.experimental.Delegate
    private final HasDataSourceBean<SdmxFileBean> beanSupport;

    @lombok.experimental.Delegate
    private final HasFilePaths filePathSupport;

    @lombok.experimental.Delegate(excludes = {HasTsCursor.class, HasDataDisplayName.class})
    private final CubeSupport cubeSupport;

    @lombok.experimental.Delegate
    private final HasDataDisplayName dataDisplayName;

    @lombok.experimental.Delegate
    private final ITsProvider tsSupport;

    public SdmxFileProvider() {
        Logger logger = LoggerFactory.getLogger(NAME);
        Cache<DataSource, SdmxCubeItems> cache = GuavaCaches.softValuesCache();
        SdmxFileParam sdmxParam = new SdmxFileParam.V1();

        this.properties = SdmxPropertiesSupport.of(SdmxFileManager::ofServiceLoader, cache::invalidateAll);
        this.mutableListSupport = HasDataSourceMutableList.of(NAME, logger, cache::invalidate);
        this.monikerSupport = HasDataMoniker.usingUri(NAME);
        this.beanSupport = HasDataSourceBean.of(NAME, sdmxParam, sdmxParam.getVersion());
        this.filePathSupport = HasFilePaths.of(cache::invalidateAll);
        this.cubeSupport = CubeSupport.of(new SdmxCubeResource(cache, properties, filePathSupport, sdmxParam));
        this.dataDisplayName = new SdmxFileDataDisplayName(beanSupport, cubeSupport);
        this.tsSupport = TsFillerAsProvider.of(NAME, TsAsyncMode.Once, TsCursorAsFiller.of(logger, cubeSupport, monikerSupport, dataDisplayName), cache::invalidateAll);
    }

    @Override
    public String getDisplayName() {
        return "SDMX Files";
    }

    @Override
    public String getFileDescription() {
        return "SDMX file";
    }

    @Override
    public boolean accept(File pathname) {
        return pathname.getName().toLowerCase(Locale.ROOT).endsWith(".xml");
    }

    private static String getSourceLabel(SdmxFileBean bean) {
        return bean.getFile().getPath();
    }

    @lombok.AllArgsConstructor
    private static final class SdmxCubeResource implements CubeSupport.Resource {

        private final Cache<DataSource, SdmxCubeItems> cache;
        private final HasSdmxProperties properties;
        private final HasFilePaths paths;
        private final SdmxFileParam param;

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
            return GuavaCaches.getOrThrowIOException(cache, dataSource, () -> of(properties, paths, param, dataSource));
        }

        private static SdmxCubeItems of(HasSdmxProperties<SdmxFileManager> properties, HasFilePaths paths, SdmxFileParam param, DataSource dataSource) throws IOException {
            SdmxFileBean bean = param.get(dataSource);
            SdmxFileSource files = SdmxCubeItems.resolveFileSet(paths, bean);

            DataflowRef flowRef = files.asDataflowRef();

            IOSupplier<Connection> conn = toConnection(properties, files);

            CubeAccessor accessor = SdmxCubeAccessor.of(conn, flowRef, bean.getDimensions(), bean.getLabelAttribute(), getSourceLabel(bean), false);

            IParam<DataSet, CubeId> idParam = param.getCubeIdParam(accessor.getRoot());

            return new SdmxCubeItems(accessor, idParam);
        }

        private static IOSupplier<Connection> toConnection(HasSdmxProperties<SdmxFileManager> properties, SdmxFileSource files) throws IOException {
            SdmxFileManager manager = properties.getSdmxManager();
            return () -> manager.getConnection(files);
        }
    }

    @lombok.AllArgsConstructor
    private static final class SdmxFileDataDisplayName implements HasDataDisplayName {

        private final HasDataSourceBean<SdmxFileBean> beanSupport;
        private final CubeSupport cubeSupport;

        @Override
        public String getDisplayName(DataSource dataSource) throws IllegalArgumentException {
            return getSourceLabel(beanSupport.decodeBean(dataSource));
        }

        @Override
        public String getDisplayName(DataSet dataSet) throws IllegalArgumentException {
            return cubeSupport.getDisplayName(dataSet);
        }

        @Override
        public String getDisplayName(IOException exception) throws IllegalArgumentException {
            if (exception instanceof EOFException) {
                return "Unexpected end-of-file: " + exception.getMessage();
            }
            return cubeSupport.getDisplayName(exception);
        }

        @Override
        public String getDisplayNodeName(DataSet dataSet) throws IllegalArgumentException {
            return cubeSupport.getDisplayNodeName(dataSet);
        }
    }
}
