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
import be.nbb.sdmx.driver.SdmxDriver;
import be.nbb.sdmx.driver.WsEntryPoint;
import static be.nbb.sdmx.driver.WsEntryPoint.of;
import ec.tss.tsproviders.utils.Parsers;
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
            return new ExtRestSdmxClient("", endpoint, isNeedsCredentials(info), isNeedsURLEncoding(info), isSupportsCompression(info));
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
                of("ECB", "European Central Bank", "sdmx:sdmx21:http://sdw-wsrest.ecb.europa.eu/service", withCompression()),
                of("EUROSTAT", "Eurostat", "sdmx:sdmx21:http://ec.europa.eu/eurostat/SDMX/diss-web/rest", withCompression()),
                of("ISTAT", "Istituto Nazionale di Statistica", "sdmx:sdmx21:http://sdmx.istat.it/SDMXWS/rest", withCompression()),
                of("INSEE", "Institut national de la statistique et des études économiques", "sdmx:sdmx21:http://www.bdm.insee.fr/series/sdmx")
        );
    }

    private Properties withCompression() {
        Properties result = new Properties();
        result.setProperty("supportsCompression", "true");
        return result;
    }

    private boolean isNeedsCredentials(Properties info) {
        String value = info.getProperty("needsCredentials");
        return value != null ? Parsers.boolParser().tryParse(value).or(Boolean.FALSE) : false;
    }

    private boolean isNeedsURLEncoding(Properties info) {
        String value = info.getProperty("needsURLEncoding");
        return value != null ? Parsers.boolParser().tryParse(value).or(Boolean.FALSE) : false;
    }

    private boolean isSupportsCompression(Properties info) {
        String value = info.getProperty("supportsCompression");
        return value != null ? Parsers.boolParser().tryParse(value).or(Boolean.FALSE) : false;
    }
}
