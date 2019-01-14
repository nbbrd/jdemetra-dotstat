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
package internal.connectors.drivers;

import be.nbb.sdmx.facade.parser.spi.SdmxDialect;
import be.nbb.sdmx.facade.util.HasCache;
import be.nbb.sdmx.facade.util.SdmxFix;
import static be.nbb.sdmx.facade.util.SdmxFix.Category.CONTENT;
import static be.nbb.sdmx.facade.util.SdmxFix.Category.ENDPOINT;
import it.bancaditalia.oss.sdmx.api.DataFlowStructure;
import it.bancaditalia.oss.sdmx.client.RestSdmxClient;
import it.bancaditalia.oss.sdmx.exceptions.SdmxException;
import org.openide.util.lookup.ServiceProvider;
import be.nbb.sdmx.facade.web.spi.SdmxWebDriver;
import internal.connectors.ConnectorRestClient;
import internal.connectors.HasSeriesKeysOnlySupported;
import internal.connectors.Connectors;
import internal.util.drivers.InseeDialect;
import internal.web.SdmxWebDriverSupport;
import it.bancaditalia.oss.sdmx.api.Codelist;
import it.bancaditalia.oss.sdmx.api.DSDIdentifier;
import it.bancaditalia.oss.sdmx.api.Dimension;
import java.net.URI;
import java.util.Map;
import java.util.logging.Level;

/**
 *
 * @author Philippe Charles
 */
@ServiceProvider(service = SdmxWebDriver.class)
public final class InseeDriver implements SdmxWebDriver, HasCache {

    @lombok.experimental.Delegate
    private final SdmxWebDriverSupport support = SdmxWebDriverSupport
            .builder()
            .name("connectors:insee")
            .rank(WRAPPED_RANK)
            .client(ConnectorRestClient.of(InseeClient::new, DIALECT))
            .supportedProperties(ConnectorRestClient.CONNECTION_PROPERTIES)
            .sourceOf("INSEE", "Institut national de la statistique et des études économiques", FALLBACK_ENDPOINT)
            .build();

    @SdmxFix(id = 1, category = ENDPOINT, cause = "Fallback to http due to some servers that use root certificate unknown to jdk'")
    private static final String FALLBACK_ENDPOINT = "http://bdm.insee.fr/series/sdmx";

    @SdmxFix(id = 2, category = CONTENT, cause = "Does not follow sdmx standard codes")
    private static final SdmxDialect DIALECT = new InseeDialect();

    private final static class InseeClient extends RestSdmxClient implements HasSeriesKeysOnlySupported {

        private InseeClient(URI endpoint, Map<?, ?> properties) {
            super("", endpoint, false, false, true);
        }

        @Override
        public DataFlowStructure getDataFlowStructure(DSDIdentifier dsd, boolean full) throws SdmxException {
            DataFlowStructure result = super.getDataFlowStructure(dsd, full);
            fixIds(result);
            fixMissingCodes(result);
            return result;
        }

        @Override
        public boolean isSeriesKeysOnlySupported() {
            return true;
        }

        @SdmxFix(id = 3, category = CONTENT, cause = "Some dimension/code ids are invalid")
        private void fixIds(DataFlowStructure dsd) {
            for (Dimension d : dsd.getDimensions()) {
                if (d.getId().endsWith("6")) {
                    d.setId(getValidId(d.getId()));
//                    d.getCodeList().setId(getValidId(d.getCodeList().getId()));
                }
            }
        }

        private String getValidId(String id) {
            return id.substring(0, id.length() - 1);
        }

        @SdmxFix(id = 4, category = CONTENT, cause = "Some codes are missing in dsd even when requested with 'references=children'")
        private void fixMissingCodes(DataFlowStructure dsd) throws SdmxException {
            for (Dimension d : dsd.getDimensions()) {
                Codelist codelist = d.getCodeList();
                if (codelist.isEmpty()) {
                    loadMissingCodes(codelist);
                }
            }
        }

        private void loadMissingCodes(Codelist codelist) throws SdmxException {
            try {
                codelist.setCodes(super.getCodes(codelist.getId(), codelist.getAgency(), codelist.getVersion()));
            } catch (SdmxException ex) {
                if (!Connectors.isNoResultMatchingQuery(ex)) {
                    throw ex;
                }
                logger.log(Level.WARNING, "Cannot retrieve codes for ''{0}''", codelist.getFullIdentifier());
            }
        }
    }
}
