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

import sdmxdl.*;
import ec.tss.tsproviders.cursor.TsCursor;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.tstoolkit.timeseries.simplets.TsFrequency;
import ec.tstoolkit.timeseries.simplets.TsPeriod;
import static internal.sdmx.SdmxQueryUtil.getAllSeries;
import static internal.sdmx.SdmxQueryUtil.getAllSeriesWithData;
import static internal.sdmx.SdmxQueryUtil.getChildren;
import static internal.sdmx.SdmxQueryUtil.getSeriesWithData;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import org.junit.jupiter.api.Test;
import sdmxdl.web.SdmxWebSource;
import sdmxdl.web.spi.WebContext;
import test.samples.FacadeResource;
import tests.sdmxdl.web.spi.MockedDriver;

import static test.samples.FacadeResource.ECB_FLOW_REF;
import static test.samples.FacadeResource.NBB_FLOW_REF;

/**
 *
 * @author Philippe Charles
 */
public class SdmxQueryUtilTest {

    private final String title = "FR. Germany - Net lending (+) or net borrowing (-): general government :- Excessive deficit procedure (Including one-off proceeds relative to the allocation of mobile phone licences (UMTS))";

    private Connection asConnection(DataRepository repo, Set<Feature> features) throws IOException {
        MockedDriver driver = MockedDriver.builder().repo(repo, features).build();
        SdmxWebSource source = driver.getDefaultSources().iterator().next();
        return driver.connect(source, Languages.ANY, WebContext.builder().build());
    }

    @Test
    public void testGetAllSeries20() throws Exception {
        Connection conn = asConnection(FacadeResource.nbb(), EnumSet.noneOf(Feature.class));

        Key single = Key.of("LOCSTL04", "AUS", "M");

        try ( TsCursor<Key> o = getAllSeries(conn, NBB_FLOW_REF, Key.ALL, SdmxQueryUtil.NO_LABEL)) {
            assertThat(o.nextSeries()).isTrue();
            assertThat(o.getSeriesId()).isEqualTo(single);
            assertThat(o.getSeriesLabel()).isEqualTo(single.toString());
            assertThat(o.getSeriesMetaData()).isEmpty();
            assertThat(o.nextSeries()).isFalse();
        }

        try ( TsCursor<Key> o = getAllSeries(conn, NBB_FLOW_REF, Key.of("LOCSTL04", "", ""), SdmxQueryUtil.NO_LABEL)) {
            assertThat(o.nextSeries()).isTrue();
            assertThat(o.getSeriesId()).isEqualTo(single);
            assertThat(o.getSeriesLabel()).isEqualTo(single.toString());
            assertThat(o.getSeriesMetaData()).isEmpty();
            assertThat(o.nextSeries()).isFalse();
        }

        try ( TsCursor<Key> o = getAllSeries(conn, NBB_FLOW_REF, Key.of("LOCSTL04", "AUS", ""), SdmxQueryUtil.NO_LABEL)) {
            assertThat(o.nextSeries()).isTrue();
            assertThat(o.getSeriesId()).isEqualTo(single);
            assertThat(o.getSeriesLabel()).isEqualTo(single.toString());
            assertThat(o.getSeriesMetaData()).isEmpty();
            assertThat(o.nextSeries()).isFalse();
        }
    }

    @Test
    public void testGetAllSeriesWithData20() throws Exception {
        Connection conn = asConnection(FacadeResource.nbb(), EnumSet.noneOf(Feature.class));

        Key single = Key.of("LOCSTL04", "AUS", "M");

        try ( TsCursor<Key> o = getAllSeriesWithData(conn, NBB_FLOW_REF, Key.ALL, SdmxQueryUtil.NO_LABEL)) {
            assertThat(o.nextSeries()).isTrue();
            assertThat(o.getSeriesId()).isEqualTo(single);
            assertThat(o.getSeriesLabel()).isEqualTo(single.toString());
            assertThat(o.getSeriesMetaData()).containsKey("TIME_FORMAT");
            assertThat(o.getSeriesData().isPresent()).isFalse();
            assertThat(o.getSeriesData().getCause()).startsWith("Cannot guess").contains("duplicated");
            assertThat(o.nextSeries()).isFalse();
        }

        try ( TsCursor<Key> o = getAllSeriesWithData(conn, NBB_FLOW_REF, Key.of("LOCSTL04", "", ""), SdmxQueryUtil.NO_LABEL)) {
            assertThat(o.nextSeries()).isTrue();
            assertThat(o.getSeriesId()).isEqualTo(single);
            assertThat(o.getSeriesLabel()).isEqualTo(single.toString());
            assertThat(o.getSeriesMetaData()).containsKey("TIME_FORMAT");
            assertThat(o.getSeriesData().isPresent()).isFalse();
            assertThat(o.getSeriesData().getCause()).startsWith("Cannot guess").contains("duplicated");
            assertThat(o.nextSeries()).isFalse();
        }

        try ( TsCursor<Key> o = getAllSeriesWithData(conn, NBB_FLOW_REF, Key.of("LOCSTL04", "AUS", ""), SdmxQueryUtil.NO_LABEL)) {
            assertThat(o.nextSeries()).isTrue();
            assertThat(o.getSeriesId()).isEqualTo(single);
            assertThat(o.getSeriesLabel()).isEqualTo(single.toString());
            assertThat(o.getSeriesMetaData()).containsKey("TIME_FORMAT");
            assertThat(o.getSeriesData().isPresent()).isFalse();
            assertThat(o.getSeriesData().getCause()).startsWith("Cannot guess").contains("duplicated");
            assertThat(o.nextSeries()).isFalse();
        }
    }

    @Test
    public void testGetSeriesWithData20() throws Exception {
        Connection conn = asConnection(FacadeResource.nbb(), EnumSet.noneOf(Feature.class));

        try ( TsCursor<Key> c = getSeriesWithData(conn, NBB_FLOW_REF, Key.of("LOCSTL04", "AUS", "M"), SdmxQueryUtil.NO_LABEL)) {
            assertThat(c.nextSeries()).isTrue();
            assertThat(c.getSeriesData().isPresent()).isFalse();
            assertThat(c.getSeriesData().getCause()).startsWith("Cannot guess").contains("duplicated");
            assertThat(c.nextSeries()).isFalse();
        }
    }

    @Test
    public void testGetChildren20() throws Exception {
        Connection conn = asConnection(FacadeResource.nbb(), EnumSet.noneOf(Feature.class));

        assertThat(getChildren(conn, NBB_FLOW_REF, Key.ALL, 0)).containsExactly("LOCSTL04");
        assertThat(getChildren(conn, NBB_FLOW_REF, Key.of("LOCSTL04", "", ""), 1)).containsExactly("AUS");
        assertThat(getChildren(conn, NBB_FLOW_REF, Key.of("LOCSTL04", "AUS", ""), 2)).containsExactly("M");
        assertThat(getChildren(conn, NBB_FLOW_REF, Key.of("LOCSTL04", "", "M"), 1)).containsExactly("AUS");
    }

    @Test
    public void testGetAllSeries21() throws Exception {
        Connection conn = asConnection(FacadeResource.ecb(), EnumSet.allOf(Feature.class));
        Key key;

        key = Key.ALL;
        try ( TsCursor<Key> o = getAllSeries(conn, ECB_FLOW_REF, key, "EXT_TITLE")) {
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
        try ( TsCursor<Key> o = getAllSeries(conn, ECB_FLOW_REF, key, "EXT_TITLE")) {
            int index = 0;
            while (o.nextSeries()) {
                index++;
                assertThat(key.contains(o.getSeriesId())).isTrue();
            }
            assertThat(index).isEqualTo(120);
        }

        key = Key.of("A", "DEU", "", "", "", "", "");
        try ( TsCursor<Key> o = getAllSeries(conn, ECB_FLOW_REF, key, "EXT_TITLE")) {
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
        Connection conn = asConnection(FacadeResource.ecb(), EnumSet.allOf(Feature.class));
        Key key;

        key = Key.ALL;
        try ( TsCursor<Key> o = getAllSeriesWithData(conn, ECB_FLOW_REF, key, "EXT_TITLE")) {
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
        try ( TsCursor<Key> o = getAllSeriesWithData(conn, ECB_FLOW_REF, key, "EXT_TITLE")) {
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
        Connection conn = asConnection(FacadeResource.ecb(), EnumSet.allOf(Feature.class));

        Key key = Key.of("A", "DEU", "1", "0", "319", "0", "UBLGE");

        try ( TsCursor<Key> c = getSeriesWithData(conn, ECB_FLOW_REF, key, SdmxQueryUtil.NO_LABEL)) {
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
        Connection conn = asConnection(FacadeResource.ecb(), EnumSet.allOf(Feature.class));

        assertThat(getChildren(conn, ECB_FLOW_REF, Key.ALL, 0)).containsExactly("A");
        assertThat(getChildren(conn, ECB_FLOW_REF, Key.of("A", "", "", "", "", "", ""), 1)).hasSize(30).contains("BEL", "POL");
        assertThat(getChildren(conn, ECB_FLOW_REF, Key.of("A", "BEL", "", "", "", "", ""), 2)).containsExactly("1");
        assertThatIllegalArgumentException().isThrownBy(() -> getChildren(conn, ECB_FLOW_REF, Key.of("hello", "", "", "", "", "", ""), 1));
    }
}
