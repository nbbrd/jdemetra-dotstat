/*
 * Copyright 2016 National Bank of Belgium
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
package be.nbb.demetra.sdmx2;

import ec.tss.tsproviders.DataSource;
import ec.tss.tsproviders.IDataSourceLoaderAssert;
import static ec.tss.tsproviders.IDataSourceLoaderAssert.assertThat;
import ec.tss.tsproviders.sdmx.SdmxBean;
import ec.tss.tsproviders.sdmx.SdmxProvider;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author Philippe Charles
 */
public class SdmxProvider2Test {

    private static File getSampleFile() {
        try {
            return new File(SdmxProvider2Test.class.getResource("/be/nbb/sdmx/TimeSeries.xml").toURI());
        } catch (URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    @Ignore
    public void testEquivalence() throws IOException {
        assertThat(getProvider())
                .isEquivalentTo(getPreviousProvider(), SdmxProvider2Test::getSampleDataSource);
    }

    @Test
    public void testTspCompliance() {
        IDataSourceLoaderAssert.assertCompliance(SdmxProvider2Test::getProvider, o -> {
            SdmxBean2 result = o.newBean();
            result.setFile(getSampleFile());
            return result;
        });
    }

    private static SdmxProvider2 getProvider() {
        SdmxProvider2 result = new SdmxProvider2();
        return result;
    }

    private static SdmxProvider getPreviousProvider() {
        SdmxProvider result = new SdmxProvider();
        return result;
    }

    private static DataSource getSampleDataSource(SdmxProvider o) {
        SdmxBean result = o.newBean();
        result.setFile(getSampleFile());
        return o.encodeBean(result);
    }
}
