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

import static be.nbb.demetra.dotstat.DotStatAccessor.getKey;
import be.nbb.sdmx.facade.DataCursor;
import be.nbb.sdmx.facade.DataStructure;
import be.nbb.sdmx.facade.Dimension;
import be.nbb.sdmx.facade.DataflowRef;
import be.nbb.sdmx.facade.Key;
import be.nbb.sdmx.facade.SdmxConnectionSupplier;
import be.nbb.sdmx.facade.connectors.TestResource;
import be.nbb.sdmx.facade.util.MemSdmxConnectionSupplier;
import com.google.common.base.Joiner;
import ec.tss.tsproviders.db.DbAccessor;
import ec.tss.tsproviders.db.DbSeries;
import ec.tss.tsproviders.db.DbSetId;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.tstoolkit.timeseries.simplets.TsFrequency;
import ec.tstoolkit.timeseries.simplets.TsPeriod;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author Philippe Charles
 */
public class DotStatAccessorTest {

    private final SdmxConnectionSupplier supplier = MemSdmxConnectionSupplier.builder()
            .repository(TestResource.nbb())
            .repository(TestResource.ecb())
            .build();

    private static DotStatBean nbbBean() {
        DotStatBean result = new DotStatBean();
        result.setDbName("NBB");
        result.setFlowRef(DataflowRef.of("NBB", "TEST_DATASET", null));
        result.setDimColumns(Joiner.on(',').join(new String[]{"SUBJECT", "LOCATION", "FREQUENCY"}));
        return result;
    }

    private static DbSetId nbbRoot() {
        return DbSetId.root("SUBJECT", "LOCATION", "FREQUENCY");
    }

    private static DotStatBean ecbBean() {
        String[] dimensions = {"FREQ", "AME_REF_AREA", "AME_TRANSFORMATION",
            "AME_AGG_METHOD", "AME_UNIT", "AME_REFERENCE", "AME_ITEM"};

        DotStatBean result = new DotStatBean();
        result.setDbName("ECB");
        result.setFlowRef(DataflowRef.parse("ECB,AME,1.0"));
        result.setDimColumns(Joiner.on(',').join(dimensions));
        return result;
    }

    private static DbSetId ecbRoot() {
        return DbSetId.root("FREQ", "AME_REF_AREA", "AME_TRANSFORMATION",
                "AME_AGG_METHOD", "AME_UNIT", "AME_REFERENCE", "AME_ITEM");
    }

    @Test
    public void testGetKey() throws Exception {
        DataStructure dfs = supplier.getConnection("NBB").getDataStructure(DataflowRef.of("NBB", "TEST_DATASET", null));
        Map<String, Dimension> dimensionById = DotStatAccessor.dimensionById(dfs);

        // default ordering of dimensions
        DbSetId r1 = DbSetId.root("SUBJECT", "LOCATION", "FREQUENCY");
        assertEquals(Key.parse("LOCSTL04.AUS.M"), getKey(dimensionById, r1.child("LOCSTL04", "AUS", "M")));
        assertEquals(Key.parse("LOCSTL04.AUS."), getKey(dimensionById, r1.child("LOCSTL04", "AUS")));
        assertEquals(Key.parse("LOCSTL04.."), getKey(dimensionById, r1.child("LOCSTL04")));
        assertEquals(Key.ALL, getKey(dimensionById, r1));

        // custom ordering of dimensions
        DbSetId r2 = DbSetId.root("FREQUENCY", "LOCATION", "SUBJECT");
        assertEquals(Key.parse("LOCSTL04.AUS.M"), getKey(dimensionById, r2.child("M", "AUS", "LOCSTL04")));
        assertEquals(Key.parse(".AUS.M"), getKey(dimensionById, r2.child("M", "AUS")));
        assertEquals(Key.parse("..M"), getKey(dimensionById, r2.child("M")));
        assertEquals(Key.ALL, getKey(dimensionById, r2));
    }

    @Test
    public void testGetKeyFromTs() throws Exception {
        try (DataCursor cursor = supplier.getConnection("NBB").getData(DataflowRef.of("NBB", "TEST_DATASET", null), Key.ALL, true)) {
            cursor.nextSeries();
            assertEquals(Key.parse("LOCSTL04.AUS.M"), cursor.getKey());
        }
    }

    @Test
    public void testGetAllSeries20() throws Exception {
        DbAccessor<?> accessor = new DotStatAccessor(nbbBean(), supplier);

        List<DbSetId> allSeries;
        DbSetId single = nbbRoot().child("LOCSTL04", "AUS", "M");

        allSeries = accessor.getAllSeries();
        assertEquals(1, allSeries.size());
        assertEquals(single, allSeries.get(0));

        allSeries = accessor.getAllSeries("LOCSTL04");
        assertEquals(1, allSeries.size());
        assertEquals(single, allSeries.get(0));

        allSeries = accessor.getAllSeries("LOCSTL04", "AUS");
        assertEquals(1, allSeries.size());
        assertEquals(single, allSeries.get(0));
    }

    @Test
    public void testGetAllSeriesWithData20() throws Exception {
        DbAccessor<?> accessor = new DotStatAccessor(nbbBean(), supplier);

        List<DbSeries> allSeries;
        DbSetId single = nbbRoot().child("LOCSTL04", "AUS", "M");

        allSeries = accessor.getAllSeriesWithData();
        assertEquals(1, allSeries.size());
        assertEquals(single, allSeries.get(0).getId());
        assertEquals(55, allSeries.get(0).getData().get().getLength());

        allSeries = accessor.getAllSeriesWithData("LOCSTL04");
        assertEquals(1, allSeries.size());
        assertEquals(single, allSeries.get(0).getId());
        assertEquals(55, allSeries.get(0).getData().get().getLength());

        allSeries = accessor.getAllSeriesWithData("LOCSTL04", "AUS");
        assertEquals(1, allSeries.size());
        assertEquals(single, allSeries.get(0).getId());
        assertEquals(55, allSeries.get(0).getData().get().getLength());
    }

    @Test
    public void testGetSeriesWithData20() throws Exception {
        DbAccessor<?> accessor = new DotStatAccessor(nbbBean(), supplier);

        DbSetId single = nbbRoot().child("LOCSTL04", "AUS", "M");

        DbSeries series = accessor.getSeriesWithData("LOCSTL04", "AUS", "M");
        assertEquals(single, series.getId());

        TsData data = series.getData().get();
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
        DbAccessor<?> accessor = new DotStatAccessor(nbbBean(), supplier);

        assertArrayEquals(new String[]{"LOCSTL04"}, accessor.getChildren().toArray());
        assertArrayEquals(new String[]{"AUS"}, accessor.getChildren("LOCSTL04").toArray());
        assertArrayEquals(new String[]{"M"}, accessor.getChildren("LOCSTL04", "AUS").toArray());
    }

    @Test
    public void testGetAllSeries21() throws Exception {
        DbAccessor<?> accessor = new DotStatAccessor(ecbBean(), supplier);

        List<DbSetId> allSeries;
        DbSetId root = ecbRoot();

        allSeries = accessor.getAllSeries();
        assertEquals(120, allSeries.size());
        assertTrue(allSeries.contains(root.child("A", "DEU", "1", "0", "319", "0", "UBLGE")));
        assertTrue(allSeries.contains(root.child("A", "HRV", "1", "0", "0", "0", "ZUTN")));
        for (DbSetId o : allSeries) {
            assertTrue(o.isSeries());
        }

        allSeries = accessor.getAllSeries("A");
        assertEquals(120, allSeries.size());

        allSeries = accessor.getAllSeries("A", "DEU");
        assertEquals(4, allSeries.size());
        assertTrue(allSeries.contains(root.child("A", "DEU", "1", "0", "319", "0", "UBLGE")));
        assertFalse(allSeries.contains(root.child("A", "HRV", "1", "0", "0", "0", "ZUTN")));
    }

    @Test
    public void testGetAllSeriesWithData21() throws Exception {
        DbAccessor<?> accessor = new DotStatAccessor(ecbBean(), supplier);

        List<DbSeries> allSeries;
        DbSetId item = ecbRoot().child("A", "DEU", "1", "0", "319", "0", "UBLGE");

        allSeries = accessor.getAllSeriesWithData();
        assertEquals(120, allSeries.size());
        assertNotNull(DbSeries.findById(allSeries, item));
        assertEquals(25, DbSeries.findById(allSeries, item).getData().get().getLength());

        allSeries = accessor.getAllSeriesWithData("A", "DEU");
        assertEquals(4, allSeries.size());
        assertNotNull(DbSeries.findById(allSeries, item));
        assertEquals(25, DbSeries.findById(allSeries, item).getData().get().getLength());
    }

    @Test
    public void testGetSeriesWithData21() throws Exception {
        DbAccessor<?> accessor = new DotStatAccessor(ecbBean(), supplier);

        DbSetId single = ecbRoot().child("A", "DEU", "1", "0", "319", "0", "UBLGE");

        DbSeries series = accessor.getSeriesWithData("A", "DEU", "1", "0", "319", "0", "UBLGE");
        assertEquals(single, series.getId());

        TsData data = series.getData().get();
        assertEquals(new TsPeriod(TsFrequency.Yearly, 1991, 0), data.getStart());
        assertEquals(new TsPeriod(TsFrequency.Yearly, 2015, 0), data.getLastPeriod());
        assertEquals(25, data.getLength());
        assertEquals(25, data.getObsCount());
        assertEquals(-2.8574221, data.getValues().get(0), 0d);
        assertEquals(-0.1420473, data.getValues().get(24), 0d);
    }

    @Test
    public void testGetChildren21() throws Exception {
        DbAccessor<?> accessor = new DotStatAccessor(ecbBean(), supplier);

        List<String> children;

        children = accessor.getChildren();
        assertEquals(1, children.size());
        assertTrue(children.contains("A"));

        children = accessor.getChildren("A");
        assertEquals(30, children.size());
        assertTrue(children.contains("BEL"));
        assertTrue(children.contains("POL"));

        children = accessor.getChildren("A", "BEL");
        assertEquals(1, children.size());
        assertTrue(children.contains("1"));

        children = accessor.getChildren("hello");
        assertTrue(children.isEmpty());
    }
}
