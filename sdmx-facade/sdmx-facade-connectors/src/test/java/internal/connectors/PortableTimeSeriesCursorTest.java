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
package internal.connectors;

import _test.samples.ConnectorsResource;
import be.nbb.sdmx.facade.Key;
import be.nbb.sdmx.facade.Frequency;
import be.nbb.sdmx.facade.samples.SdmxSource;
import be.nbb.sdmx.facade.tck.DataCursorAssert;
import be.nbb.sdmx.facade.Obs;
import be.nbb.sdmx.facade.parser.DataFactory;
import be.nbb.sdmx.facade.util.SeriesSupport;
import it.bancaditalia.oss.sdmx.api.DataFlowStructure;
import it.bancaditalia.oss.sdmx.api.PortableTimeSeries;
import it.bancaditalia.oss.sdmx.util.LanguagePriorityList;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author Philippe Charles
 */
public class PortableTimeSeriesCursorTest {

    static DataFlowStructure DSD;
    static List<PortableTimeSeries<Double>> DATA;

    @BeforeClass
    public static void beforeClass() throws IOException {
        LanguagePriorityList l = LanguagePriorityList.parse("en");
        DSD = ConnectorsResource.struct21(SdmxSource.ECB_DATA_STRUCTURE, l).get(0);
        DATA = ConnectorsResource.data21(SdmxSource.ECB_DATA, DSD, l);
    }

    @Test
    public void test() throws IOException {
        assertThat(SeriesSupport.asStream(() -> PortableTimeSeriesCursor.of(DATA, DataFactory.sdmx21(), Connectors.toStructure(DSD))))
                .hasSize(120)
                .allMatch(o -> o.getFreq().equals(Frequency.ANNUAL))
                .element(0)
                .satisfies(o -> {
                    assertThat(o.getKey()).isEqualTo(Key.parse("A.DEU.1.0.319.0.UBLGE"));
                    assertThat(o.getMeta())
                            .hasSize(3)
                            .containsEntry("EXT_UNIT", "Percentage of GDP at market prices (excessive deficit procedure)")
                            .isNotEmpty();
                    assertThat(o.getObs())
                            .hasSize(25)
                            .startsWith(Obs.of(LocalDate.of(1991, 1, 1).atStartOfDay(), -2.8574221))
                            .endsWith(Obs.of(LocalDate.of(2015, 1, 1).atStartOfDay(), -0.1420473));
                });
    }

    @Test
    public void testCompliance() {
        DataCursorAssert.assertCompliance(() -> PortableTimeSeriesCursor.of(DATA, DataFactory.sdmx21(), Connectors.toStructure(DSD)));
    }
}
