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

import be.nbb.sdmx.facade.DataflowRef;
import be.nbb.sdmx.facade.Dimension;
import be.nbb.sdmx.facade.LanguagePriorityList;
import be.nbb.sdmx.facade.SdmxConnection;
import be.nbb.sdmx.facade.SdmxConnectionSupplier;
import be.nbb.sdmx.facade.util.UnexpectedIOException;
import ec.tss.tsproviders.DataSet;
import ec.tss.tsproviders.cube.CubeAccessor;
import ec.tss.tsproviders.cube.CubeId;
import ec.tss.tsproviders.utils.IParam;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author Philippe Charles
 */
@lombok.Value
public class SdmxCubeItems {

    CubeAccessor accessor;
    IParam<DataSet, CubeId> idParam;

    public static List<String> getDefaultDimIds(SdmxConnectionSupplier supplier, LanguagePriorityList languages, String source, DataflowRef flow) throws IOException {
        try (SdmxConnection conn = supplier.getConnection(source, languages)) {
            return conn.getDataStructure(flow).getDimensions().stream().map(Dimension::getId).collect(Collectors.toList());
        } catch (RuntimeException ex) {
            throw new UnexpectedIOException(ex);
        }
    }
}
