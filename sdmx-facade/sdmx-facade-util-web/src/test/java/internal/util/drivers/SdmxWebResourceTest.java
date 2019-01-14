/*
 * Copyright 2018 National Bank of Belgium
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
package internal.util.drivers;

import be.nbb.sdmx.facade.web.SdmxWebSource;
import java.io.IOException;
import static org.assertj.core.api.Assertions.*;
import org.junit.Test;

/**
 *
 * @author Philippe Charles
 */
public class SdmxWebResourceTest {

    @Test
    public void testParser() throws IOException {
        String xml = "<sources>\n"
                + "    <source>\n"
                + "        <name>ECB</name>\n"
                + "        <description>European Central Bank</description>\n"
                + "        <driver>web-ri:sdmx21</driver>\n"
                + "        <endpoint>https://sdw-wsrest.ecb.europa.eu/service</endpoint>\n"
                + "        <property key=\"seriesKeysOnlySupported\" value=\"true\"/>\n"
                + "    </source>\n"
                + "</sources>";

        assertThat(SdmxWebResource.getParser().parseChars(xml))
                .hasSize(1)
                .element(0)
                .isEqualToComparingFieldByField(SdmxWebSource
                        .builder()
                        .name("ECB")
                        .description("European Central Bank")
                        .driver("web-ri:sdmx21")
                        .endpointOf("https://sdw-wsrest.ecb.europa.eu/service")
                        .property("seriesKeysOnlySupported", "true")
                        .build()
                );
    }
}
