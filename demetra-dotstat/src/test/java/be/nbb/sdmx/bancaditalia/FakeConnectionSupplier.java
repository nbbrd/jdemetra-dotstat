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

import be.nbb.sdmx.SdmxConnectionSupplier;
import be.nbb.sdmx.SdmxConnection;

/**
 *
 * @author Philippe Charles
 */
public final class FakeConnectionSupplier extends SdmxConnectionSupplier {

    public static final String NBB = "NBB";
    public static final String ECB = "ECB";

    private final SdmxConnection nbb;
    private final SdmxConnection ecb;

    public FakeConnectionSupplier() {
        this.nbb = TestResource.nbb().AsConnection();
        this.ecb = TestResource.ecb().AsConnection();
    }

    @Override
    public SdmxConnection getConnection(String endpoint) {
        switch (endpoint) {
            case NBB:
                return nbb;
            case ECB:
                return ecb;
            default:
                throw new RuntimeException();
        }
    }
}
