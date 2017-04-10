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
package internal.sdmx;

import be.nbb.sdmx.facade.DataflowRef;
import be.nbb.sdmx.facade.Key;
import be.nbb.sdmx.facade.SdmxConnection;
import be.nbb.sdmx.facade.connectors.TestResource;
import be.nbb.sdmx.facade.util.MemSdmxRepository;
import ec.tss.tsproviders.cursor.TsCursor;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.tstoolkit.timeseries.simplets.TsFrequency;
import ec.tstoolkit.timeseries.simplets.TsPeriod;
import static internal.sdmx.SdmxQueryUtil.getAllSeries;
import static internal.sdmx.SdmxQueryUtil.getAllSeriesWithData;
import static internal.sdmx.SdmxQueryUtil.getChildren;
import static internal.sdmx.SdmxQueryUtil.getSeriesWithData;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;

/**
 *
 * @author Philippe Charles
 */
public class SdmxQueryUtilTest {

    private final MemSdmxRepository nbb = TestResource.nbb();
    private final MemSdmxRepository ecb = TestResource.ecb();
    private final DataflowRef nbbFlow = DataflowRef.of("NBB", "TEST_DATASET", null);
    private final DataflowRef ecbFlow = DataflowRef.parse("ECB,AME,1.0");

    @Test
    public void testGetAllSeries20() throws Exception {
        SdmxConnection conn = nbb.asConnection();

        Key single = Key.of("LOCSTL04", "AUS", "M");

        try (TsCursor<Key> cursor = getAllSeries(conn, nbbFlow, Key.ALL)) {
            assertThat(cursor.nextSeries()).isTrue();
            assertThat(cursor.getSeriesId()).isEqualTo(single);
            assertThat(cursor.nextSeries()).isFalse();
        }

        try (TsCursor<Key> cursor = getAllSeries(conn, nbbFlow, Key.of("LOCSTL04", "", ""))) {
            assertThat(cursor.nextSeries()).isTrue();
            assertThat(cursor.getSeriesId()).isEqualTo(single);
            assertThat(cursor.nextSeries()).isFalse();
        }

        try (TsCursor<Key> cursor = getAllSeries(conn, nbbFlow, Key.of("LOCSTL04", "AUS", ""))) {
            assertThat(cursor.nextSeries()).isTrue();
            assertThat(cursor.getSeriesId()).isEqualTo(single);
            assertThat(cursor.nextSeries()).isFalse();
        }
    }

    @Test
    public void testGetAllSeriesWithData20() throws Exception {
        SdmxConnection conn = nbb.asConnection();

        Key single = Key.of("LOCSTL04", "AUS", "M");

        try (TsCursor<Key> cursor = getAllSeriesWithData(conn, nbbFlow, Key.ALL)) {
            assertThat(cursor.nextSeries()).isTrue();
            assertThat(cursor.getSeriesId()).isEqualTo(single);
            assertThat(cursor.getSeriesData().get().getLength()).isEqualTo(55);
            assertThat(cursor.nextSeries()).isFalse();
        }

        try (TsCursor<Key> cursor = getAllSeriesWithData(conn, nbbFlow, Key.of("LOCSTL04", "", ""))) {
            assertThat(cursor.nextSeries()).isTrue();
            assertThat(cursor.getSeriesId()).isEqualTo(single);
            assertThat(cursor.getSeriesData().get().getLength()).isEqualTo(55);
            assertThat(cursor.nextSeries()).isFalse();
        }

        try (TsCursor<Key> cursor = getAllSeriesWithData(conn, nbbFlow, Key.of("LOCSTL04", "AUS", ""))) {
            assertThat(cursor.nextSeries()).isTrue();
            assertThat(cursor.getSeriesId()).isEqualTo(single);
            assertThat(cursor.getSeriesData().get().getLength()).isEqualTo(55);
            assertThat(cursor.nextSeries()).isFalse();
        }
    }

    @Test
    public void testGetSeriesWithData20() throws Exception {
        SdmxConnection conn = nbb.asConnection();

        TsData data = getSeriesWithData(conn, nbbFlow, Key.of("LOCSTL04", "AUS", "M")).get();
        assertThat(data.getStart()).isEqualTo(new TsPeriod(TsFrequency.Monthly, 1966, 1));
        assertThat(data.getLastPeriod()).isEqualTo(new TsPeriod(TsFrequency.Monthly, 1970, 7));
        assertThat(data.getLength()).isEqualTo(55);
        assertThat(data.getObsCount()).isEqualTo(54);
        assertThat(data.isMissing(50)).isTrue(); // 1970-04
        assertThat(data.get(0)).isEqualTo(98.68823);
        assertThat(data.get(54)).isEqualTo(101.1945);
    }

    @Test
    public void testGetChildren20() throws Exception {
        SdmxConnection conn = nbb.asConnection();

        assertThat(getChildren(conn, nbbFlow, Key.ALL, 1)).containsExactly("LOCSTL04");
        assertThat(getChildren(conn, nbbFlow, Key.of("LOCSTL04", "", ""), 2)).containsExactly("AUS");
        assertThat(getChildren(conn, nbbFlow, Key.of("LOCSTL04", "AUS", ""), 3)).containsExactly("M");
        assertThat(getChildren(conn, nbbFlow, Key.of("LOCSTL04", "", "M"), 2)).containsExactly("AUS");
    }

    @Test
    public void testGetAllSeries21() throws Exception {
        SdmxConnection conn = ecb.asConnection();
        Key key;

        key = Key.ALL;
        try (TsCursor<Key> cursor = getAllSeries(conn, ecbFlow, key)) {
            int index = 0;
            while (cursor.nextSeries()) {
                switch (index++) {
                    case 0:
                        assertThat(cursor.getSeriesId()).isEqualTo(Key.of("A", "DEU", "1", "0", "319", "0", "UBLGE"));
                        break;
                    case 119:
                        assertThat(cursor.getSeriesId()).isEqualTo(Key.of("A", "HRV", "1", "0", "0", "0", "ZUTN"));
                        break;
                }
                assertThat(key.contains(cursor.getSeriesId())).isTrue();
            }
            assertThat(index).isEqualTo(120);
        }

        key = Key.of("A", "", "", "", "", "", "");
        try (TsCursor<Key> cursor = getAllSeries(conn, ecbFlow, key)) {
            int index = 0;
            while (cursor.nextSeries()) {
                index++;
                assertThat(key.contains(cursor.getSeriesId())).isTrue();
            }
            assertThat(index).isEqualTo(120);
        }

        key = Key.of("A", "DEU", "", "", "", "", "");
        try (TsCursor<Key> cursor = getAllSeries(conn, ecbFlow, key)) {
            int index = 0;
            while (cursor.nextSeries()) {
                index++;
                assertThat(key.contains(cursor.getSeriesId())).isTrue();
            }
            assertThat(index).isEqualTo(4);
        }
    }

    @Test
    public void testGetAllSeriesWithData21() throws Exception {
        SdmxConnection conn = ecb.asConnection();
        Key key;

        key = Key.ALL;
        try (TsCursor<Key> cursor = getAllSeriesWithData(conn, ecbFlow, key)) {
            int index = 0;
            while (cursor.nextSeries()) {
                switch (index++) {
                    case 0:
                        assertThat(cursor.getSeriesId()).isEqualTo(Key.of("A", "DEU", "1", "0", "319", "0", "UBLGE"));
                        assertThat(cursor.getSeriesData().get().getLength()).isEqualTo(25);
                        break;
                }
                assertThat(key.contains(cursor.getSeriesId())).isTrue();
            }
            assertThat(index).isEqualTo(120);
        }

        key = Key.of("A", "DEU", "", "", "", "", "");
        try (TsCursor<Key> cursor = getAllSeriesWithData(conn, ecbFlow, key)) {
            int index = 0;
            while (cursor.nextSeries()) {
                switch (index++) {
                    case 0:
                        assertThat(cursor.getSeriesId()).isEqualTo(Key.of("A", "DEU", "1", "0", "319", "0", "UBLGE"));
                        assertThat(cursor.getSeriesData().get().getLength()).isEqualTo(25);
                        break;
                }
                assertThat(key.contains(cursor.getSeriesId())).isTrue();
            }
            assertThat(index).isEqualTo(4);
        }
    }

    @Test
    public void testGetSeriesWithData21() throws Exception {
        SdmxConnection conn = ecb.asConnection();

        Key key = Key.of("A", "DEU", "1", "0", "319", "0", "UBLGE");

        TsData data = getSeriesWithData(conn, ecbFlow, key).get();
        assertThat(data.getStart()).isEqualTo(new TsPeriod(TsFrequency.Yearly, 1991, 0));
        assertThat(data.getLastPeriod()).isEqualTo(new TsPeriod(TsFrequency.Yearly, 2015, 0));
        assertThat(data.getLength()).isEqualTo(25);
        assertThat(data.getObsCount()).isEqualTo(25);
        assertThat(data.get(0)).isEqualTo(-2.8574221);
        assertThat(data.get(24)).isEqualTo(-0.1420473);
    }

    @Test
    public void testGetChildren21() throws Exception {
        SdmxConnection conn = ecb.asConnection();

        List<String> children;

        children = getChildren(conn, ecbFlow, Key.ALL, 1);
        assertThat(children.size()).isEqualTo(1);
        assertThat(children.contains("A")).isTrue();

        children = getChildren(conn, ecbFlow, Key.of("A", "", "", "", "", "", ""), 2);
        assertThat(children.size()).isEqualTo(30);
        assertThat(children.contains("BEL")).isTrue();
        assertThat(children.contains("POL")).isTrue();

        children = getChildren(conn, ecbFlow, Key.of("A", "BEL", "", "", "", "", ""), 3);
        assertThat(children.size()).isEqualTo(1);
        assertThat(children.contains("1")).isTrue();

        children = getChildren(conn, ecbFlow, Key.of("hello", "", "", "", "", "", ""), 2);
        assertThat(children.isEmpty()).isTrue();
    }
}
