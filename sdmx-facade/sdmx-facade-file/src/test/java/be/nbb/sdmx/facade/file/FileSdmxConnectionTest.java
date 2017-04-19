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
package be.nbb.sdmx.facade.file;

import be.nbb.sdmx.facade.file.impl.XMLStreamSdmxDecoder;
import be.nbb.sdmx.facade.DataCursor;
import be.nbb.sdmx.facade.DataflowRef;
import be.nbb.sdmx.facade.Key;
import be.nbb.sdmx.facade.TimeFormat;
import be.nbb.sdmx.facade.samples.SdmxSource;
import java.io.File;
import java.io.IOException;
import javax.xml.stream.XMLInputFactory;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author Philippe Charles
 */
public class FileSdmxConnectionTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();
    private final XMLInputFactory factory = XMLInputFactory.newInstance();
    private final SdmxDecoder decoder = new XMLStreamSdmxDecoder(factory);

    @Test
    public void testCompactData21() throws IOException {
        File compact21 = temp.newFile();
        SdmxSource.OTHER_COMPACT21.copyTo(compact21.toPath());

        FileSdmxConnection conn = new FileSdmxConnection(compact21, factory, decoder);

        DataflowRef flowRef = DataflowRef.parse(compact21.getName());

        assertThat(conn.getDataflows()).hasSize(1);
        assertThat(conn.getDataStructure(flowRef).getDimensions()).hasSize(7);

        Key key = Key.of("A", "BEL", "1", "0", "0", "0", "OVGD");

        try (DataCursor o = conn.getData(flowRef, Key.ALL, false)) {
            assertThat(o.nextSeries()).isTrue();
            assertThat(o.getSeriesKey()).isEqualTo(key);
            assertThat(o.getSeriesTimeFormat()).isEqualTo(TimeFormat.YEARLY);
            int indexObs = -1;
            while (o.nextObs()) {
                switch (++indexObs) {
                    case 0:
                        assertThat(o.getObsPeriod()).isEqualTo("1960-01-01");
                        assertThat(o.getObsValue()).isEqualTo(92.0142);
                        break;
                    case 56:
                        assertThat(o.getObsPeriod()).isEqualTo("2016-01-01");
                        assertThat(o.getObsValue()).isEqualTo(386.5655);
                        break;
                }
            }
            assertThat(indexObs).isEqualTo(56);
            assertThat(o.nextSeries()).isFalse();
        }
    }
}
