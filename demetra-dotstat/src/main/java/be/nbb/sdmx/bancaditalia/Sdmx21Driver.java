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
package be.nbb.sdmx.bancaditalia;

import be.nbb.sdmx.SdmxConnection;
import be.nbb.sdmx.bancaditalia.RestSdmxClientWithCursor.Config;
import static be.nbb.sdmx.bancaditalia.Util.NEEDS_CREDENTIALS_PROPERTY;
import static be.nbb.sdmx.bancaditalia.Util.NEEDS_URL_ENCODING_PROPERTY;
import static be.nbb.sdmx.bancaditalia.Util.SERIES_KEYS_ONLY_SUPPORTED_PROPERTY;
import static be.nbb.sdmx.bancaditalia.Util.SUPPORTS_COMPRESSION_PROPERTY;
import static be.nbb.sdmx.bancaditalia.Util.get;
import be.nbb.sdmx.driver.SdmxDriver;
import be.nbb.sdmx.driver.WsEntryPoint;
import it.bancaditalia.oss.sdmx.api.GenericSDMXClient;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import static java.util.Arrays.asList;
import java.util.List;
import java.util.Properties;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Philippe Charles
 */
@ServiceProvider(service = SdmxDriver.class)
public final class Sdmx21Driver extends SdmxDriver {

    private static final String PREFIX = "sdmx:sdmx21:";

    private final Util.ClientSupplier supplier = new Util.ClientSupplier() {
        @Override
        public GenericSDMXClient getClient(URL endpoint, Properties info) throws MalformedURLException {
            return new RestSdmxClientWithCursor("", endpoint, load(info));
        }
    };

    @Override
    public SdmxConnection connect(String url, Properties info) throws IOException {
        return Util.getConnection(url.substring(PREFIX.length()), info, supplier);
    }

    @Override
    public boolean acceptsURL(String url) throws IOException {
        return url.startsWith(PREFIX);
    }

    @Override
    public List<WsEntryPoint> getDefaultEntryPoints() {
        return asList(
                of("ECB", "European Central Bank", "sdmx:sdmx21:http://sdw-wsrest.ecb.europa.eu/service", Config.builder().supportsCompression(true).seriesKeysOnlySupported(true).build()),
                of("EUROSTAT", "Eurostat", "sdmx:sdmx21:http://ec.europa.eu/eurostat/SDMX/diss-web/rest", Config.builder().supportsCompression(true).build()),
                of("ISTAT", "Istituto Nazionale di Statistica", "sdmx:sdmx21:http://sdmx.istat.it/SDMXWS/rest", Config.builder().supportsCompression(true).seriesKeysOnlySupported(true).build()),
                of("INSEE", "Institut national de la statistique et des études économiques", "sdmx:sdmx21:http://www.bdm.insee.fr/series/sdmx", Config.builder().seriesKeysOnlySupported(true).build())
        );
    }

    private static WsEntryPoint of(String name, String description, String url, RestSdmxClientWithCursor.Config c) {
        Properties p = new Properties();
        store(p, c);
        return WsEntryPoint.of(name, description, url, p);
    }

    private static Config load(Properties p) {
        return new Config(
                get(p, NEEDS_CREDENTIALS_PROPERTY, false),
                get(p, NEEDS_URL_ENCODING_PROPERTY, false),
                get(p, SUPPORTS_COMPRESSION_PROPERTY, false),
                get(p, SERIES_KEYS_ONLY_SUPPORTED_PROPERTY, false));
    }

    private static void store(Properties p, Config c) {
        p.setProperty(NEEDS_CREDENTIALS_PROPERTY, String.valueOf(c.isNeedsCredentials()));
        p.setProperty(NEEDS_URL_ENCODING_PROPERTY, String.valueOf(c.isNeedsURLEncoding()));
        p.setProperty(SUPPORTS_COMPRESSION_PROPERTY, String.valueOf(c.isSupportsCompression()));
        p.setProperty(SERIES_KEYS_ONLY_SUPPORTED_PROPERTY, String.valueOf(c.isSeriesKeysOnlySupported()));
    }
}
