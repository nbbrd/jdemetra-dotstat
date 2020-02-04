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

import be.nbb.sdmx.facade.web.SdmxWebManager;
import be.nbb.sdmx.facade.web.SdmxWebSource;
import internal.util.rest.RestClient;
import internal.util.rest.RestClientImpl;
import internal.web.SdmxWebProperty;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import be.nbb.sdmx.facade.web.spi.SdmxWebContext;
import java.net.HttpURLConnection;
import java.net.Proxy;
import nbbrd.io.function.IOConsumer;

/**
 *
 * @author Philippe Charles
 */
@lombok.experimental.UtilityClass
class RestClients {

    public RestClient getRestClient(SdmxWebSource o, SdmxWebContext context) {
        return getRestClient(o, context, IOConsumer.noOp());
    }

    public RestClient getRestClient(SdmxWebSource o, SdmxWebContext context, IOConsumer<HttpURLConnection> validator) {
        return RestClientImpl.of(
                SdmxWebProperty.getReadTimeout(o.getProperties()),
                SdmxWebProperty.getConnectTimeout(o.getProperties()),
                SdmxWebProperty.getMaxRedirects(o.getProperties()),
                context.getProxySelector(),
                context.getSslSocketFactory(),
                EventLogger.INSTANCE,
                validator
        );
    }

    public final List<String> CONNECTION_PROPERTIES = Collections.unmodifiableList(
            Arrays.asList(
                    SdmxWebProperty.CONNECT_TIMEOUT_PROPERTY,
                    SdmxWebProperty.READ_TIMEOUT_PROPERTY,
                    SdmxWebProperty.MAX_REDIRECTS_PROPERTY
            ));

    @lombok.AllArgsConstructor
    private enum EventLogger implements RestClientImpl.EventListener {

        INSTANCE;
        
        @lombok.NonNull
        private final Logger logger = Logger.getLogger(SdmxWebManager.class.getName());

        @Override
        public void onOpenStream(URL query, String mediaType, String langs) {
            logger.log(Level.INFO, "Querying ''{0}''", query);
        }

        @Override
        public void onRedirection(URL oldUrl, URL newUrl) {
            logger.log(Level.INFO, "Redirecting to ''{0}''", newUrl);
        }

        @Override
        public void onProxy(URL query, Proxy proxy) {
            logger.log(Level.INFO, "Using proxy ''{0}''", proxy);
        }
    }
}
