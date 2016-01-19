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
package be.nbb.sdmx.facade.connectors;

import com.google.common.io.ByteSource;
import java.io.IOException;
import java.io.InputStream;
import lombok.experimental.UtilityClass;

/**
 *
 * @author Philippe Charles
 */
@UtilityClass
public class SdmxTestResources {

    public static final ByteSource NBB_DATAFLOWS = onResource("/be/nbb/sdmx/Dataflows.xml");
    public static final ByteSource NBB_DATA_STRUCTURE = onResource("/be/nbb/sdmx/DataflowStructure.xml");
    public static final ByteSource NBB_DATA = onResource("/be/nbb/sdmx/TimeSeries.xml");
    public static final ByteSource ECB_DATAFLOWS = onResource("/be/nbb/sdmx/EcbDataflows.xml");
    public static final ByteSource ECB_DATA_STRUCTURE = onResource("/be/nbb/sdmx/EcbDataStructure.xml");
    public static final ByteSource ECB_DATA = onResource("/be/nbb/sdmx/EcbDataKeys.xml");

    private static ByteSource onResource(final String id) {
        return new ByteSource() {
            @Override
            public InputStream openStream() throws IOException {
                return SdmxTestResources.class.getResource(id).openStream();
            }
        };
    }
}
