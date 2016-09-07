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
package be.nbb.demetra.dotstat;

import static be.nbb.demetra.dotstat.DotStatUtil.getAllSeries;
import static be.nbb.demetra.dotstat.DotStatUtil.getAllSeriesWithData;
import static be.nbb.demetra.dotstat.DotStatUtil.getChildren;
import static be.nbb.demetra.dotstat.DotStatUtil.getSeriesWithData;
import be.nbb.sdmx.facade.FlowRef;
import be.nbb.sdmx.facade.Key;
import be.nbb.sdmx.facade.SdmxConnection;
import be.nbb.sdmx.facade.connectors.TestResource;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.tstoolkit.timeseries.simplets.TsFrequency;
import ec.tstoolkit.timeseries.simplets.TsPeriod;
import java.io.IOException;
import java.util.List;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author Philippe Charles
 */
public class DotStatUtilTest {

    private final SdmxConnection nbb = TestResource.nbb();
    private final SdmxConnection ecb = TestResource.ecb();
    private final FlowRef nbbFlow = FlowRef.of("NBB", "TEST_DATASET", null);
    private final FlowRef ecbFlow = FlowRef.parse("ECB,AME,1.0");

    @Test
    public void testGetAllSeries20() throws Exception {
        SdmxConnection conn = nbb;

        Key single = Key.of("LOCSTL04", "AUS", "M");

        try (TsCursor<Key, IOException> cursor = getAllSeries(conn, nbbFlow, Key.ALL)) {
            assertTrue(cursor.nextSeries());
            assertEquals(single, cursor.getKey());
            assertFalse(cursor.nextSeries());
        }

        try (TsCursor<Key, IOException> cursor = getAllSeries(conn, nbbFlow, Key.of("LOCSTL04", "", ""))) {
            assertTrue(cursor.nextSeries());
            assertEquals(single, cursor.getKey());
            assertFalse(cursor.nextSeries());
        }

        try (TsCursor<Key, IOException> cursor = getAllSeries(conn, nbbFlow, Key.of("LOCSTL04", "AUS", ""))) {
            assertTrue(cursor.nextSeries());
            assertEquals(single, cursor.getKey());
            assertFalse(cursor.nextSeries());
        }
    }

    @Test
    public void testGetAllSeriesWithData20() throws Exception {
        SdmxConnection conn = nbb;

        Key single = Key.of("LOCSTL04", "AUS", "M");

        try (TsCursor<Key, IOException> cursor = getAllSeriesWithData(conn, nbbFlow, Key.ALL)) {
            assertTrue(cursor.nextSeries());
            assertEquals(single, cursor.getKey());
            assertEquals(55, cursor.getData().get().getLength());
            assertFalse(cursor.nextSeries());
        }

        try (TsCursor<Key, IOException> cursor = getAllSeriesWithData(conn, nbbFlow, Key.of("LOCSTL04", "", ""))) {
            assertTrue(cursor.nextSeries());
            assertEquals(single, cursor.getKey());
            assertEquals(55, cursor.getData().get().getLength());
            assertFalse(cursor.nextSeries());
        }

        try (TsCursor<Key, IOException> cursor = getAllSeriesWithData(conn, nbbFlow, Key.of("LOCSTL04", "AUS", ""))) {
            assertTrue(cursor.nextSeries());
            assertEquals(single, cursor.getKey());
            assertEquals(55, cursor.getData().get().getLength());
            assertFalse(cursor.nextSeries());
        }
    }

    @Test
    public void testGetSeriesWithData20() throws Exception {
        SdmxConnection conn = nbb;

        TsData data = getSeriesWithData(conn, nbbFlow, Key.of("LOCSTL04", "AUS", "M")).get();
        assertEquals(new TsPeriod(TsFrequency.Monthly, 1966, 1), data.getStart());
        assertEquals(new TsPeriod(TsFrequency.Monthly, 1970, 7), data.getLastPeriod());
        assertEquals(55, data.getLength());
        assertEquals(54, data.getObsCount());
        assertTrue(data.isMissing(50)); // 1970-04
        assertEquals(98.68823, data.getValues().get(0), 0d);
        assertEquals(101.1945, data.getValues().get(54), 0d);
    }

    @Test
    public void testGetChildren20() throws Exception {
        SdmxConnection conn = nbb;

        assertArrayEquals(new String[]{"LOCSTL04"}, getChildren(conn, nbbFlow, Key.ALL, 1).toArray());
        assertArrayEquals(new String[]{"AUS"}, getChildren(conn, nbbFlow, Key.of("LOCSTL04", "", ""), 2).toArray());
        assertArrayEquals(new String[]{"M"}, getChildren(conn, nbbFlow, Key.of("LOCSTL04", "AUS", ""), 3).toArray());
        assertArrayEquals(new String[]{"AUS"}, getChildren(conn, nbbFlow, Key.of("LOCSTL04", "", "M"), 2).toArray());
    }

    @Test
    public void testGetAllSeries21() throws Exception {
        SdmxConnection conn = ecb;
        Key key;

        key = Key.ALL;
        try (TsCursor<Key, IOException> cursor = getAllSeries(conn, ecbFlow, key)) {
            int index = 0;
            while (cursor.nextSeries()) {
                switch (index++) {
                    case 0:
                        assertEquals(Key.of("A", "DEU", "1", "0", "319", "0", "UBLGE"), cursor.getKey());
                        break;
                    case 119:
                        assertEquals(Key.of("A", "HRV", "1", "0", "0", "0", "ZUTN"), cursor.getKey());
                        break;
                }
                assertTrue(key.contains(cursor.getKey()));
            }
            assertEquals(120, index);
        }

        key = Key.of("A", "", "", "", "", "", "");
        try (TsCursor<Key, IOException> cursor = getAllSeries(conn, ecbFlow, key)) {
            int index = 0;
            while (cursor.nextSeries()) {
                index++;
                assertTrue(key.contains(cursor.getKey()));
            }
            assertEquals(120, index);
        }

        key = Key.of("A", "DEU", "", "", "", "", "");
        try (TsCursor<Key, IOException> cursor = getAllSeries(conn, ecbFlow, key)) {
            int index = 0;
            while (cursor.nextSeries()) {
                index++;
                assertTrue(key.contains(cursor.getKey()));
            }
            assertEquals(4, index);
        }
    }

    @Test
    public void testGetAllSeriesWithData21() throws Exception {
        SdmxConnection conn = ecb;
        Key key;

        key = Key.ALL;
        try (TsCursor<Key, IOException> cursor = getAllSeriesWithData(conn, ecbFlow, key)) {
            int index = 0;
            while (cursor.nextSeries()) {
                switch (index++) {
                    case 0:
                        assertEquals(Key.of("A", "DEU", "1", "0", "319", "0", "UBLGE"), cursor.getKey());
                        assertEquals(25, cursor.getData().get().getLength());
                        break;
                }
                assertTrue(key.contains(cursor.getKey()));
            }
            assertEquals(120, index);
        }

        key = Key.of("A", "DEU", "", "", "", "", "");
        try (TsCursor<Key, IOException> cursor = getAllSeriesWithData(conn, ecbFlow, key)) {
            int index = 0;
            while (cursor.nextSeries()) {
                switch (index++) {
                    case 0:
                        assertEquals(Key.of("A", "DEU", "1", "0", "319", "0", "UBLGE"), cursor.getKey());
                        assertEquals(25, cursor.getData().get().getLength());
                        break;
                }
                assertTrue(key.contains(cursor.getKey()));
            }
            assertEquals(4, index);
        }
    }

    @Test
    public void testGetSeriesWithData21() throws Exception {
        SdmxConnection conn = ecb;

        Key key = Key.of("A", "DEU", "1", "0", "319", "0", "UBLGE");

        TsData data = getSeriesWithData(conn, ecbFlow, key).get();
        assertEquals(new TsPeriod(TsFrequency.Yearly, 1991, 0), data.getStart());
        assertEquals(new TsPeriod(TsFrequency.Yearly, 2015, 0), data.getLastPeriod());
        assertEquals(25, data.getLength());
        assertEquals(25, data.getObsCount());
        assertEquals(-2.8574221, data.getValues().get(0), 0d);
        assertEquals(-0.1420473, data.getValues().get(24), 0d);
    }

    @Test
    public void testGetChildren21() throws Exception {
        SdmxConnection conn = ecb;

        List<String> children;

        children = getChildren(conn, ecbFlow, Key.ALL, 1);
        assertEquals(1, children.size());
        assertTrue(children.contains("A"));

        children = getChildren(conn, ecbFlow, Key.of("A", "", "", "", "", "", ""), 2);
        assertEquals(30, children.size());
        assertTrue(children.contains("BEL"));
        assertTrue(children.contains("POL"));

        children = getChildren(conn, ecbFlow, Key.of("A", "BEL", "", "", "", "", ""), 3);
        assertEquals(1, children.size());
        assertTrue(children.contains("1"));

        children = getChildren(conn, ecbFlow, Key.of("hello", "", "", "", "", "", ""), 2);
        assertTrue(children.isEmpty());
    }
}
