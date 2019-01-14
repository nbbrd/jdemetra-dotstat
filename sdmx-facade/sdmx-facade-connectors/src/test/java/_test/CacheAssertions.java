/*
 * Copyright 2017 National Bank of Belgium
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
package _test;

import be.nbb.sdmx.facade.util.HasCache;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import static org.assertj.core.api.Assertions.*;

/**
 *
 * @author Philippe Charles
 */
@lombok.experimental.UtilityClass
public class CacheAssertions {

    public void assertCacheBehavior(HasCache o) {
        assertThat(o.getCache()).isNotNull();
        ConcurrentMap cache = new ConcurrentHashMap();
        o.setCache(cache);
        assertThat(o.getCache()).isSameAs(cache);
        o.setCache(null);
        assertThat(o.getCache()).isNotNull().isNotSameAs(cache);
    }
}
