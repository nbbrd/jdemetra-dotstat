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
package internal.web.drivers;

import be.nbb.sdmx.facade.util.SdmxFix;
import static be.nbb.sdmx.facade.util.SdmxFix.Category.CONTENT;
import static be.nbb.sdmx.facade.util.SdmxFix.Category.QUERY;
import be.nbb.sdmx.facade.web.SdmxWebSource;
import be.nbb.sdmx.facade.web.spi.SdmxWebDriver;
import internal.util.rest.RestQueryBuilder;
import internal.web.SdmxWebDriverSupport;
import java.io.IOException;
import java.net.URL;
import be.nbb.sdmx.facade.web.spi.SdmxWebContext;
import internal.web.DataRequest;
import internal.web.SdmxWebClient;
import java.net.HttpURLConnection;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Philippe Charles
 */
@ServiceProvider(service = SdmxWebDriver.class)
public final class NbbDriver2 implements SdmxWebDriver {

    @lombok.experimental.Delegate
    private final SdmxWebDriverSupport support = SdmxWebDriverSupport
            .builder()
            .name("web-ri:nbb")
            .rank(NATIVE_RANK)
            .client(NbbClient2::new)
            .supportedProperties(RestClients.CONNECTION_PROPERTIES)
            .sourceOf("NBB", "National Bank of Belgium", "https://stat.nbb.be/restsdmx/sdmx.ashx")
            .build();

    private static final class NbbClient2 extends DotStatRestClient {

        private NbbClient2(SdmxWebSource s, SdmxWebContext c) {
            super(SdmxWebClient.getClientName(s), s.getEndpoint(), c.getLanguages(),
                    RestClients.getRestClient(s, c, NbbClient2::checkResponseForError));
        }

        @SdmxFix(id = 1, category = QUERY, cause = "'/all' must be encoded to '%2Fall'")
        @Override
        protected URL getDataQuery(DataRequest request) throws IOException {
            return RestQueryBuilder
                    .of(endpoint)
                    .path(DATA_RESOURCE)
                    .path(request.getFlowRef().getId())
                    .path(request.getKey().toString() + "/all")
                    .param("format", "compact_v2")
                    .build();
        }

        @SdmxFix(id = 2, category = CONTENT, cause = "Some interal errors redirect to an html page")
        private static void checkResponseForError(HttpURLConnection http) throws IOException {
            if (http.getContentType().equals("text/html")) {
                throw new IOException("Service not available");
            }
        }
    }
}
