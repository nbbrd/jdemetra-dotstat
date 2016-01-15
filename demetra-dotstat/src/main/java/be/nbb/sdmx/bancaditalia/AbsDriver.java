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
import it.bancaditalia.oss.sdmx.api.GenericSDMXClient;
import it.bancaditalia.oss.sdmx.client.custom.ABS;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import static java.util.Collections.singletonList;
import java.util.List;
import java.util.Properties;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Philippe Charles
 */
@ServiceProvider(service = SdmxDriver.class)
public final class AbsDriver extends SdmxDriver {

    private static final String PREFIX = "sdmx:abs:";

    private final Util.ClientSupplier supplier = new Util.ClientSupplier() {
        @Override
        public GenericSDMXClient getClient(URL endpoint, Properties info) throws MalformedURLException {
            GenericSDMXClient result = new ABS();
            result.setEndpoint(endpoint);
            return result;
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
        return singletonList(of("ABS", "Australian Bureau of Statistics", "sdmx:abs:http://stat.abs.gov.au/restsdmx/sdmx.ashx"));
    }
}
