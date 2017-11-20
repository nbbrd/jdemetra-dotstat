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
package internal.sdmx;

import be.nbb.demetra.sdmx.file.SdmxFileBean;
import be.nbb.sdmx.facade.DataStructure;
import be.nbb.sdmx.facade.DataflowRef;
import be.nbb.sdmx.facade.Dimension;
import be.nbb.sdmx.facade.SdmxConnection;
import be.nbb.sdmx.facade.file.SdmxFileSet;
import be.nbb.sdmx.facade.util.IO;
import be.nbb.sdmx.facade.util.UnexpectedIOException;
import ec.tss.tsproviders.DataSet;
import ec.tss.tsproviders.HasFilePaths;
import ec.tss.tsproviders.cube.CubeAccessor;
import ec.tss.tsproviders.cube.CubeId;
import ec.tss.tsproviders.utils.IParam;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 *
 * @author Philippe Charles
 */
@lombok.Value
public class SdmxCubeItems {

    CubeAccessor accessor;
    IParam<DataSet, CubeId> idParam;

    public static DataStructure loadStructure(IO.Supplier<SdmxConnection> supplier, DataflowRef flow) throws IOException {
        try (SdmxConnection conn = supplier.getWithIO()) {
            return conn.getStructure(flow);
        } catch (RuntimeException ex) {
            throw new UnexpectedIOException(ex);
        }
    }

    public static CubeId getOrLoadRoot(List<String> dimensions, IO.Supplier<DataStructure> structure) throws IOException {
        return dimensions.isEmpty()
                ? CubeId.root(loadDefaultDimIds(structure))
                : CubeId.root(dimensions);
    }

    public static List<String> loadDefaultDimIds(IO.Supplier<DataStructure> structure) throws IOException {
        return structure.getWithIO()
                .getDimensions()
                .stream()
                .map(Dimension::getId)
                .collect(Collectors.toList());
    }

    public static Optional<SdmxFileSet> tryResolveFileSet(HasFilePaths paths, SdmxFileBean bean) {
        try {
            return Optional.of(resolveFileSet(paths, bean));
        } catch (FileNotFoundException ex) {
            return Optional.empty();
        }
    }

    public static SdmxFileSet resolveFileSet(HasFilePaths paths, SdmxFileBean bean) throws FileNotFoundException {
        SdmxFileSet.Builder result = SdmxFileSet.builder().data(bean.getFile());
        File structure = bean.getStructureFile();
        if (structure != null && !structure.toString().isEmpty()) {
            result.structure(paths.resolveFilePath(structure));
        }
        String dialect = bean.getDialect();
        if (dialect != null && !dialect.isEmpty()) {
            result.dialect(dialect);
        }
        return result.build();
    }
}
