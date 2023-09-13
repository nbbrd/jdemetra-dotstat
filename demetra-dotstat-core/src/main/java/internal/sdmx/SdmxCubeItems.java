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
import ec.tss.tsproviders.DataSet;
import ec.tss.tsproviders.HasFilePaths;
import ec.tss.tsproviders.cube.CubeAccessor;
import ec.tss.tsproviders.cube.CubeId;
import ec.tss.tsproviders.utils.IParam;
import sdmxdl.file.FileSource;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * @author Philippe Charles
 */
@lombok.Value
public class SdmxCubeItems {

    CubeAccessor accessor;
    IParam<DataSet, CubeId> idParam;

    public static FileSource resolveFileSet(HasFilePaths paths, SdmxFileBean bean) throws FileNotFoundException {
        FileSource.Builder result = FileSource.builder().data(paths.resolveFilePath(bean.getFile()));
        File structure = bean.getStructureFile();
        if (structure != null && !structure.toString().isEmpty()) {
            result.structure(paths.resolveFilePath(structure));
        }
        return result.build();
    }
}
