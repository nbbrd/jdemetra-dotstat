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
package be.nbb.sdmx.facade.xml.stream;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 *
 * @author Philippe Charles
 */
final class AttributesBuilder {

    private final Map<String, String> data = new HashMap<>();

    @Nonnull
    AttributesBuilder clear() {
        data.clear();
        return this;
    }

    @Nonnull
    AttributesBuilder put(@Nullable String key, @Nullable String value) {
        if (key != null && value != null) {
            data.put(key, value);
        }
        return this;
    }

    @Nullable
    String getAttribute(@Nonnull String key) {
        Objects.requireNonNull(key);
        return data.get(key);
    }

    @Nonnull
    Map<String, String> build() {
        switch (data.size()) {
            case 0:
                return Collections.emptyMap();
            case 1:
                Map.Entry<String, String> single = data.entrySet().iterator().next();
                return Collections.singletonMap(single.getKey(), single.getValue());
            default:
                return Collections.unmodifiableMap(new HashMap<>(data));
        }
    }
}
