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
package be.nbb.demetra.sdmx.webservice;

import java.time.Duration;
import java.util.List;

/**
 *
 * @author Philippe Charles
 * @since 2.2.0
 */
@lombok.Data
public final class SdmxWebServiceBean {

    private String source;
    private String flow;
    private List<String> dimensions;
    private Duration cacheTtl;
    private int cacheDepth;

    public void setCacheTtl(Duration cacheTtl) {
        this.cacheTtl = cacheTtl.isNegative() ? Duration.ZERO : cacheTtl;
    }

    public void setCacheDepth(int cacheDepth) {
        this.cacheDepth = cacheDepth >= 0 ? cacheDepth : 0;
    }
}
