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

import com.google.common.base.Joiner;
import ec.tss.tsproviders.db.DbAccessor;
import ec.tss.tsproviders.db.DbSeries;
import ec.tss.tsproviders.db.DbSetId;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.tstoolkit.timeseries.simplets.TsFrequency;
import ec.tstoolkit.timeseries.simplets.TsPeriod;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sdmxdl.*;
import sdmxdl.web.SdmxWebManager;
import test.samples.FacadeResource;
import tests.sdmxdl.web.MockedDriver;

import java.io.IOException;
import java.util.EnumSet;
import java.util.function.Consumer;

import static be.nbb.demetra.dotstat.DotStatAccessor.getKey;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static test.samples.FacadeResource.ECB_FLOW_REF;
import static test.samples.FacadeResource.NBB_FLOW_REF;

/**
 * @author Philippe Charles
 */
public class DotStatAccessorTest {

    private static SdmxWebManager manager;

    @BeforeAll
    public static void beforeClass() throws IOException {
        manager = SdmxWebManager
                .builder()
                .driver(MockedDriver
                        .builder()
                        .id("test")
                        .repo(FacadeResource.nbb(), EnumSet.noneOf(Feature.class))
                        .repo(FacadeResource.ecb(), EnumSet.allOf(Feature.class))
                        .build())
                .build();
    }

    private static DotStatBean nbbBean() {
        DotStatBean result = new DotStatBean();
        result.setDbName("NBB");
        result.setFlowRef(NBB_FLOW_REF);
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
        result.setFlowRef(ECB_FLOW_REF);
        result.setDimColumns(Joiner.on(',').join(dimensions));
        return result;
    }

    private static DbSetId ecbRoot() {
        return DbSetId.root("FREQ", "AME_REF_AREA", "AME_TRANSFORMATION",
                "AME_AGG_METHOD", "AME_UNIT", "AME_REFERENCE", "AME_ITEM");
    }

    @Test
    public void testGetKey() throws Exception {
        DataStructure dsd = manager.getConnection("NBB").getStructure(NBB_FLOW_REF);

        // default ordering of dimensions
        DbSetId r1 = DbSetId.root("SUBJECT", "LOCATION", "FREQUENCY");
        assertThat(getKey(dsd, r1.child("LOCSTL04", "AUS", "M"))).isEqualTo(Key.parse("LOCSTL04.AUS.M"));
        assertThat(getKey(dsd, r1.child("LOCSTL04", "AUS"))).isEqualTo(Key.parse("LOCSTL04.AUS."));
        assertThat(getKey(dsd, r1.child("LOCSTL04"))).isEqualTo(Key.parse("LOCSTL04.."));
        assertThat(getKey(dsd, r1)).isEqualTo(Key.ALL);

        // custom ordering of dimensions
        DbSetId r2 = DbSetId.root("FREQUENCY", "LOCATION", "SUBJECT");
        assertThat(getKey(dsd, r2.child("M", "AUS", "LOCSTL04"))).isEqualTo(Key.parse("LOCSTL04.AUS.M"));
        assertThat(getKey(dsd, r2.child("M", "AUS"))).isEqualTo(Key.parse(".AUS.M"));
        assertThat(getKey(dsd, r2.child("M"))).isEqualTo(Key.parse("..M"));
        assertThat(getKey(dsd, r2)).isEqualTo(Key.ALL);
    }

    @Test
    public void testGetKeyFromTs() throws Exception {
        assertThat(manager
                .getConnection("NBB")
                .getDataStream(NBB_FLOW_REF, DataQuery.builder().key(Key.ALL).detail(DataDetail.NO_DATA).build())
                .map(Series::getKey)
        ).contains(Key.parse("LOCSTL04.AUS.M"));
    }

    @Test
    public void testGetAllSeries20() throws Exception {
        DbAccessor<?> accessor = new DotStatAccessor(nbbBean(), manager);

        DbSetId single = nbbRoot().child("LOCSTL04", "AUS", "M");

        assertThat(accessor.getAllSeries()).containsExactly(single);
        assertThat(accessor.getAllSeries("LOCSTL04")).containsExactly(single);
        assertThat(accessor.getAllSeries("LOCSTL04", "AUS")).containsExactly(single);
    }

    @Test
    public void testGetAllSeriesWithData20() throws Exception {
        DbAccessor<?> accessor = new DotStatAccessor(nbbBean(), manager);

        DbSetId single = nbbRoot().child("LOCSTL04", "AUS", "M");
        Consumer<DbSeries> singleCheck = o -> {
            assertThat(o.getId()).isEqualTo(single);
            assertThat(o.getData().isPresent()).isFalse();
            assertThat(o.getData().getCause()).startsWith("Cannot guess").contains("duplicated");
        };

        assertThat(accessor.getAllSeriesWithData()).hasSize(1).first().satisfies(singleCheck);
        assertThat(accessor.getAllSeriesWithData("LOCSTL04")).hasSize(1).first().satisfies(singleCheck);
        assertThat(accessor.getAllSeriesWithData("LOCSTL04", "AUS")).hasSize(1).first().satisfies(singleCheck);
    }

    @Test
    public void testGetSeriesWithData20() throws Exception {
        DbAccessor<?> accessor = new DotStatAccessor(nbbBean(), manager);

        DbSeries series = accessor.getSeriesWithData("LOCSTL04", "AUS", "M");
        assertThat(series.getId()).isEqualTo(nbbRoot().child("LOCSTL04", "AUS", "M"));

        assertThat(series.getData().isPresent()).isFalse();
        assertThat(series.getData().getCause()).startsWith("Cannot guess").contains("duplicated");
    }

    @Test
    public void testGetChildren20() throws Exception {
        DbAccessor<?> accessor = new DotStatAccessor(nbbBean(), manager);

        assertThat(accessor.getChildren()).containsExactly("LOCSTL04");
        assertThat(accessor.getChildren("LOCSTL04")).containsExactly("AUS");
        assertThat(accessor.getChildren("LOCSTL04", "AUS")).containsExactly("M");
    }

    @Test
    public void testGetAllSeries21() throws Exception {
        DbAccessor<?> accessor = new DotStatAccessor(ecbBean(), manager);

        DbSetId root = ecbRoot();

        assertThat(accessor.getAllSeries())
                .hasSize(120)
                .contains(root.child("A", "DEU", "1", "0", "319", "0", "UBLGE"))
                .contains(root.child("A", "HRV", "1", "0", "0", "0", "ZUTN"))
                .allMatch(DbSetId::isSeries);

        assertThat(accessor.getAllSeries("A"))
                .hasSize(120)
                .allMatch(DbSetId::isSeries);

        assertThat(accessor.getAllSeries("A", "DEU"))
                .hasSize(4)
                .contains(root.child("A", "DEU", "1", "0", "319", "0", "UBLGE"))
                .doesNotContain(root.child("A", "HRV", "1", "0", "0", "0", "ZUTN"))
                .allMatch(DbSetId::isSeries);
    }

    @Test
    public void testGetAllSeriesWithData21() throws Exception {
        DbAccessor<?> accessor = new DotStatAccessor(ecbBean(), manager);

        DbSetId item = ecbRoot().child("A", "DEU", "1", "0", "319", "0", "UBLGE");

        assertThat(accessor.getAllSeriesWithData())
                .hasSize(120)
                .filteredOn(o -> o.getId().equals(item))
                .first()
                .satisfies(o -> assertThat(o.getData().get().getLength()).isEqualTo(25));

        assertThat(accessor.getAllSeriesWithData("A", "DEU"))
                .hasSize(4)
                .filteredOn(o -> o.getId().equals(item))
                .first()
                .satisfies(o -> assertThat(o.getData().get().getLength()).isEqualTo(25));
    }

    @Test
    public void testGetSeriesWithData21() throws Exception {
        DbAccessor<?> accessor = new DotStatAccessor(ecbBean(), manager);

        DbSeries series = accessor.getSeriesWithData("A", "DEU", "1", "0", "319", "0", "UBLGE");
        assertThat(series.getId()).isEqualTo(ecbRoot().child("A", "DEU", "1", "0", "319", "0", "UBLGE"));

        TsData o = series.getData().get();
        assertThat(o.getStart()).isEqualTo(new TsPeriod(TsFrequency.Yearly, 1991, 0));
        assertThat(o.getLastPeriod()).isEqualTo(new TsPeriod(TsFrequency.Yearly, 2015, 0));
        assertThat(o.getLength()).isEqualTo(25);
        assertThat(o.getObsCount()).isEqualTo(25);
        assertThat(o.get(0)).isEqualTo(-2.8574221);
        assertThat(o.get(24)).isEqualTo(-0.1420473);
    }

    @Test
    public void testGetChildren21() throws Exception {
        DbAccessor<?> accessor = new DotStatAccessor(ecbBean(), manager);

        assertThat(accessor.getChildren())
                .hasSize(1)
                .contains("A");

        assertThat(accessor.getChildren("A"))
                .hasSize(30)
                .contains("BEL", "POL");

        assertThat(accessor.getChildren("A", "BEL"))
                .hasSize(1)
                .contains("1");

        assertThatIllegalArgumentException().isThrownBy(() -> accessor.getChildren("hello"));
    }
}
