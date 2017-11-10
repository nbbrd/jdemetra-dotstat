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
import internal.connectors.TestResource;
import be.nbb.sdmx.facade.repo.SdmxRepository;
import ec.tss.tsproviders.cursor.TsCursor;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.tstoolkit.timeseries.simplets.TsFrequency;
import ec.tstoolkit.timeseries.simplets.TsPeriod;
import static internal.sdmx.SdmxQueryUtil.getAllSeries;
import static internal.sdmx.SdmxQueryUtil.getAllSeriesWithData;
import static internal.sdmx.SdmxQueryUtil.getChildren;
import static internal.sdmx.SdmxQueryUtil.getSeriesWithData;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;

/**
 *
 * @author Philippe Charles
 */
public class SdmxQueryUtilTest {

    private final SdmxRepository nbb = TestResource.nbb();
    private final SdmxRepository ecb = TestResource.ecb();
    private final DataflowRef nbbFlow = DataflowRef.of("NBB", "TEST_DATASET", null);
    private final DataflowRef ecbFlow = DataflowRef.parse("ECB,AME,1.0");
    private final String title = "FR. Germany - Net lending (+) or net borrowing (-): general government :- Excessive deficit procedure (Including one-off proceeds relative to the allocation of mobile phone licences (UMTS))";

    @Test
    public void testGetAllSeries20() throws Exception {
        SdmxConnection conn = nbb.asConnection();

        Key single = Key.of("LOCSTL04", "AUS", "M");

        try (TsCursor<Key> o = getAllSeries(conn, nbbFlow, Key.ALL, SdmxQueryUtil.NO_LABEL)) {
            assertThat(o.nextSeries()).isTrue();
            assertThat(o.getSeriesId()).isEqualTo(single);
            assertThat(o.getSeriesLabel()).isEqualTo(single.toString());
            assertThat(o.getSeriesMetaData()).isEmpty();
            assertThat(o.nextSeries()).isFalse();
        }

        try (TsCursor<Key> o = getAllSeries(conn, nbbFlow, Key.of("LOCSTL04", "", ""), SdmxQueryUtil.NO_LABEL)) {
            assertThat(o.nextSeries()).isTrue();
            assertThat(o.getSeriesId()).isEqualTo(single);
            assertThat(o.getSeriesLabel()).isEqualTo(single.toString());
            assertThat(o.getSeriesMetaData()).isEmpty();
            assertThat(o.nextSeries()).isFalse();
        }

        try (TsCursor<Key> o = getAllSeries(conn, nbbFlow, Key.of("LOCSTL04", "AUS", ""), SdmxQueryUtil.NO_LABEL)) {
            assertThat(o.nextSeries()).isTrue();
            assertThat(o.getSeriesId()).isEqualTo(single);
            assertThat(o.getSeriesLabel()).isEqualTo(single.toString());
            assertThat(o.getSeriesMetaData()).isEmpty();
            assertThat(o.nextSeries()).isFalse();
        }
    }

    @Test
    public void testGetAllSeriesWithData20() throws Exception {
        SdmxConnection conn = nbb.asConnection();

        Key single = Key.of("LOCSTL04", "AUS", "M");

        try (TsCursor<Key> o = getAllSeriesWithData(conn, nbbFlow, Key.ALL, SdmxQueryUtil.NO_LABEL)) {
            assertThat(o.nextSeries()).isTrue();
            assertThat(o.getSeriesId()).isEqualTo(single);
            assertThat(o.getSeriesLabel()).isEqualTo(single.toString());
            assertThat(o.getSeriesMetaData()).isEmpty();
            assertThat(o.getSeriesData().get().getLength()).isEqualTo(55);
            assertThat(o.nextSeries()).isFalse();
        }

        try (TsCursor<Key> o = getAllSeriesWithData(conn, nbbFlow, Key.of("LOCSTL04", "", ""), SdmxQueryUtil.NO_LABEL)) {
            assertThat(o.nextSeries()).isTrue();
            assertThat(o.getSeriesId()).isEqualTo(single);
            assertThat(o.getSeriesLabel()).isEqualTo(single.toString());
            assertThat(o.getSeriesMetaData()).isEmpty();
            assertThat(o.getSeriesData().get().getLength()).isEqualTo(55);
            assertThat(o.nextSeries()).isFalse();
        }

        try (TsCursor<Key> o = getAllSeriesWithData(conn, nbbFlow, Key.of("LOCSTL04", "AUS", ""), SdmxQueryUtil.NO_LABEL)) {
            assertThat(o.nextSeries()).isTrue();
            assertThat(o.getSeriesId()).isEqualTo(single);
            assertThat(o.getSeriesLabel()).isEqualTo(single.toString());
            assertThat(o.getSeriesMetaData()).isEmpty();
            assertThat(o.getSeriesData().get().getLength()).isEqualTo(55);
            assertThat(o.nextSeries()).isFalse();
        }
    }

    @Test
    public void testGetSeriesWithData20() throws Exception {
        SdmxConnection conn = nbb.asConnection();

        try (TsCursor<Key> c = getSeriesWithData(conn, nbbFlow, Key.of("LOCSTL04", "AUS", "M"), SdmxQueryUtil.NO_LABEL)) {
            assertThat(c.nextSeries()).isTrue();
            TsData o = c.getSeriesData().get();
            assertThat(o.getStart()).isEqualTo(new TsPeriod(TsFrequency.Monthly, 1966, 1));
            assertThat(o.getLastPeriod()).isEqualTo(new TsPeriod(TsFrequency.Monthly, 1970, 7));
            assertThat(o.getLength()).isEqualTo(55);
            assertThat(o.getObsCount()).isEqualTo(54);
            assertThat(o.isMissing(50)).isTrue(); // 1970-04
            assertThat(o.get(0)).isEqualTo(98.68823);
            assertThat(o.get(54)).isEqualTo(101.1945);
            assertThat(c.nextSeries()).isFalse();
        }
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
        try (TsCursor<Key> o = getAllSeries(conn, ecbFlow, key, "EXT_TITLE")) {
            int index = 0;
            while (o.nextSeries()) {
                switch (index++) {
                    case 0:
                        assertThat(o.getSeriesId()).isEqualTo(Key.of("A", "DEU", "1", "0", "319", "0", "UBLGE"));
                        assertThat(o.getSeriesLabel()).isEqualTo(title);
                        assertThat(o.getSeriesMetaData()).containsOnlyKeys("EXT_TITLE", "EXT_UNIT", "TITLE_COMPL");
                        break;
                    case 119:
                        assertThat(o.getSeriesId()).isEqualTo(Key.of("A", "HRV", "1", "0", "0", "0", "ZUTN"));
                        assertThat(o.getSeriesLabel()).isEqualTo(o.getSeriesId().toString());
                        assertThat(o.getSeriesMetaData()).isEmpty();
                        break;
                }
                assertThat(key.contains(o.getSeriesId())).isTrue();
            }
            assertThat(index).isEqualTo(120);
        }

        key = Key.of("A", "", "", "", "", "", "");
        try (TsCursor<Key> o = getAllSeries(conn, ecbFlow, key, "EXT_TITLE")) {
            int index = 0;
            while (o.nextSeries()) {
                index++;
                assertThat(key.contains(o.getSeriesId())).isTrue();
            }
            assertThat(index).isEqualTo(120);
        }

        key = Key.of("A", "DEU", "", "", "", "", "");
        try (TsCursor<Key> o = getAllSeries(conn, ecbFlow, key, "EXT_TITLE")) {
            int index = 0;
            while (o.nextSeries()) {
                index++;
                assertThat(key.contains(o.getSeriesId())).isTrue();
            }
            assertThat(index).isEqualTo(4);
        }
    }

    @Test
    public void testGetAllSeriesWithData21() throws Exception {
        SdmxConnection conn = ecb.asConnection();
        Key key;

        key = Key.ALL;
        try (TsCursor<Key> o = getAllSeriesWithData(conn, ecbFlow, key, "EXT_TITLE")) {
            int index = 0;
            while (o.nextSeries()) {
                switch (index++) {
                    case 0:
                        assertThat(o.getSeriesId()).isEqualTo(Key.of("A", "DEU", "1", "0", "319", "0", "UBLGE"));
                        assertThat(o.getSeriesLabel()).isEqualTo(title);
                        assertThat(o.getSeriesMetaData()).containsOnlyKeys("EXT_TITLE", "EXT_UNIT", "TITLE_COMPL");
                        assertThat(o.getSeriesData().get().getLength()).isEqualTo(25);
                        break;
                }
                assertThat(key.contains(o.getSeriesId())).isTrue();
            }
            assertThat(index).isEqualTo(120);
        }

        key = Key.of("A", "DEU", "", "", "", "", "");
        try (TsCursor<Key> o = getAllSeriesWithData(conn, ecbFlow, key, "EXT_TITLE")) {
            int index = 0;
            while (o.nextSeries()) {
                switch (index++) {
                    case 0:
                        assertThat(o.getSeriesId()).isEqualTo(Key.of("A", "DEU", "1", "0", "319", "0", "UBLGE"));
                        assertThat(o.getSeriesLabel()).isEqualTo(title);
                        assertThat(o.getSeriesMetaData()).containsOnlyKeys("EXT_TITLE", "EXT_UNIT", "TITLE_COMPL");
                        assertThat(o.getSeriesData().get().getLength()).isEqualTo(25);
                        break;
                }
                assertThat(key.contains(o.getSeriesId())).isTrue();
            }
            assertThat(index).isEqualTo(4);
        }
    }

    @Test
    public void testGetSeriesWithData21() throws Exception {
        SdmxConnection conn = ecb.asConnection();

        Key key = Key.of("A", "DEU", "1", "0", "319", "0", "UBLGE");

        try (TsCursor<Key> c = getSeriesWithData(conn, ecbFlow, key, SdmxQueryUtil.NO_LABEL)) {
            assertThat(c.nextSeries()).isTrue();
            TsData o = c.getSeriesData().get();
            assertThat(o.getStart()).isEqualTo(new TsPeriod(TsFrequency.Yearly, 1991, 0));
            assertThat(o.getLastPeriod()).isEqualTo(new TsPeriod(TsFrequency.Yearly, 2015, 0));
            assertThat(o.getLength()).isEqualTo(25);
            assertThat(o.getObsCount()).isEqualTo(25);
            assertThat(o.get(0)).isEqualTo(-2.8574221);
            assertThat(o.get(24)).isEqualTo(-0.1420473);
            assertThat(c.nextSeries()).isFalse();
        }
    }

    @Test
    public void testGetChildren21() throws Exception {
        SdmxConnection conn = ecb.asConnection();

        assertThat(getChildren(conn, ecbFlow, Key.ALL, 1)).containsExactly("A");
        assertThat(getChildren(conn, ecbFlow, Key.of("A", "", "", "", "", "", ""), 2)).hasSize(30).contains("BEL", "POL");
        assertThat(getChildren(conn, ecbFlow, Key.of("A", "BEL", "", "", "", "", ""), 3)).containsExactly("1");
        assertThat(getChildren(conn, ecbFlow, Key.of("hello", "", "", "", "", "", ""), 2)).isEmpty();
    }
}
