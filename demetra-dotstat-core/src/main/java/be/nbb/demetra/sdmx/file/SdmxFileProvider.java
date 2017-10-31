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
import be.nbb.sdmx.facade.DataflowRef;
import be.nbb.sdmx.facade.LanguagePriorityList;
import be.nbb.sdmx.facade.SdmxConnectionSupplier;
import be.nbb.sdmx.facade.file.SdmxFileManager;
import be.nbb.sdmx.facade.file.SdmxFile;
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
import internal.sdmx.SdmxCubeItems;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Philippe Charles
 * @since 2.2.0
 */
//@ServiceProvider(service = ITsProvider.class)
public final class SdmxFileProvider implements IFileLoader, HasSdmxProperties {

    private static final String NAME = "sdmx-file";

    private final AtomicReference<SdmxConnectionSupplier> connectionSupplier;
    private final AtomicReference<LanguagePriorityList> languages;

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
        this.connectionSupplier = new AtomicReference<>(SdmxFileManager.of());
        this.languages = new AtomicReference<>(LanguagePriorityList.ANY);

        Logger logger = LoggerFactory.getLogger(NAME);
        Cache<DataSource, SdmxCubeItems> cache = GuavaCaches.softValuesCache();
        SdmxFileParam sdmxParam = new SdmxFileParam.V1();

        this.mutableListSupport = HasDataSourceMutableList.of(NAME, logger, cache::invalidate);
        this.monikerSupport = HasDataMoniker.usingUri(NAME);
        this.beanSupport = HasDataSourceBean.of(NAME, sdmxParam, sdmxParam.getVersion());
        this.filePathSupport = HasFilePaths.of(cache::invalidateAll);
        this.cubeSupport = CubeSupport.of(new SdmxCubeResource(cache, connectionSupplier, languages, filePathSupport, sdmxParam));
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

    @Override
    public SdmxConnectionSupplier getConnectionSupplier() {
        return connectionSupplier.get();
    }

    @Override
    public void setConnectionSupplier(SdmxConnectionSupplier connectionSupplier) {
        SdmxConnectionSupplier old = this.connectionSupplier.get();
        if (this.connectionSupplier.compareAndSet(old, connectionSupplier != null ? connectionSupplier : SdmxFileManager.of())) {
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

    @lombok.AllArgsConstructor
    private static final class SdmxCubeResource implements CubeSupport.Resource {

        private final Cache<DataSource, SdmxCubeItems> cache;
        private final AtomicReference<SdmxConnectionSupplier> supplier;
        private final AtomicReference<LanguagePriorityList> languages;
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
            return GuavaCaches.getOrThrowIOException(cache, dataSource, () -> of(supplier.get(), languages.get(), paths, param, dataSource));
        }

        private static SdmxCubeItems of(SdmxConnectionSupplier supplier, LanguagePriorityList languages, HasFilePaths paths, SdmxFileParam param, DataSource dataSource) throws IOException {
            SdmxFileBean bean = param.get(dataSource);
            SdmxFile file = getFile(paths, bean.getFile(), bean.getStructureFile());

            String source = file.toString();
            DataflowRef flow = file.getDataflowRef();

            List<String> dimensions = bean.getDimensions();
            if (dimensions.isEmpty()) {
                dimensions = SdmxCubeItems.getDefaultDimIds(supplier, languages, source, flow);
            }

            CubeAccessor accessor = SdmxCubeAccessor.of(supplier, languages, source, flow, dimensions, bean.getLabelAttribute());

            IParam<DataSet, CubeId> idParam = param.getCubeIdParam(accessor.getRoot());

            return new SdmxCubeItems(accessor, idParam);
        }

        private static SdmxFile getFile(HasFilePaths paths, File data, File structure) throws FileNotFoundException {
            return SdmxFile.of(paths.resolveFilePath(data), structure.toString().isEmpty() ? null : paths.resolveFilePath(structure));
        }
    }
}
