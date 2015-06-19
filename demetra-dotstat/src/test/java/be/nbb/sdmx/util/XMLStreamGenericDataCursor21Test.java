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
package be.nbb.sdmx.util;

import be.nbb.sdmx.SdmxTestResources;
import be.nbb.sdmx.DataCursor;
import be.nbb.sdmx.Key;
import be.nbb.sdmx.TimeFormat;
import com.google.common.io.ByteSource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author charphi
 */
public class XMLStreamGenericDataCursor21Test {

    private final QuickStax stax = new QuickStax();
    private final QuickCalendar cal = new QuickCalendar();

    @Test
    public void testGenericData21() throws Exception {
        Key.Builder keyBuilder = Key.builder("FREQ", "AME_REF_AREA", "AME_TRANSFORMATION", "AME_AGG_METHOD", "AME_UNIT", "AME_REFERENCE", "AME_ITEM");
        Key key = Key.valueOf("A", "BEL", "1", "0", "0", "0", "OVGD");
        ByteSource xml = SdmxTestResources.onResource("/be/nbb/sdmx/util/GenericData21.xml");

        try (DataCursor cursor = new XMLStreamGenericDataCursor21(stax.open(xml), keyBuilder, 0)) {
            assertTrue(cursor.nextSeries());
            assertEquals(key, cursor.getKey());
            assertEquals(TimeFormat.YEARLY, cursor.getTimeFormat());
            int indexObs = -1;
            while (cursor.nextObs()) {
                switch (++indexObs) {
                    case 0:
                        assertEquals(cal.getDate(1960, 0, 1), cursor.getPeriod());
                        assertEquals(92.0142, cursor.getValue(), 0d);
                        break;
                    case 56:
                        assertEquals(cal.getDate(2016, 0, 1), cursor.getPeriod());
                        assertEquals(386.5655, cursor.getValue(), 0d);
                        break;
                }
            }
            assertEquals(56, indexObs);
            assertFalse(cursor.nextSeries());
        }
    }

    @Test
    public void testCursor() throws Exception {
        Key.Builder keyBuilder = Key.builder("FREQ", "AME_REF_AREA", "AME_TRANSFORMATION", "AME_AGG_METHOD", "AME_UNIT", "AME_REFERENCE", "AME_ITEM");
        Key firstKey = Key.valueOf("A", "DEU", "1", "0", "319", "0", "UBLGE");
        Key lastKey = Key.valueOf("A", "HRV", "1", "0", "0", "0", "ZUTN");

        try (DataCursor cursor = new XMLStreamGenericDataCursor21(stax.open(SdmxTestResources.ECB_DATA), keyBuilder, 0)) {
            int indexSeries = -1;
            while (cursor.nextSeries()) {
                switch (++indexSeries) {
                    case 0:
                        assertEquals(firstKey, cursor.getKey());
                        assertEquals(TimeFormat.YEARLY, cursor.getTimeFormat());
                        int indexObs = -1;
                        while (cursor.nextObs()) {
                            switch (++indexObs) {
                                case 0:
                                    assertEquals(cal.getDate(1991, 0, 1), cursor.getPeriod());
                                    assertEquals(-2.8574221, cursor.getValue(), 0d);
                                    break;
                                case 24:
                                    assertEquals(cal.getDate(2015, 0, 1), cursor.getPeriod());
                                    assertEquals(-0.1420473, cursor.getValue(), 0d);
                                    break;
                            }
                        }
                        assertEquals(24, indexObs);
                        break;
                    case 119:
                        assertEquals(lastKey, cursor.getKey());
                        assertEquals(TimeFormat.YEARLY, cursor.getTimeFormat());
                        assertFalse(cursor.nextObs());
                        break;
                }
            }
            assertEquals(119, indexSeries);
        }
    }
}
