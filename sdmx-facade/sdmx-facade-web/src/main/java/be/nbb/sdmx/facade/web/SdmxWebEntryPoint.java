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
package be.nbb.sdmx.facade.web;

import java.net.URI;
import java.util.Map;
import javax.annotation.Nonnull;

/**
 *
 * @author Philippe Charles
 */
@lombok.Value
@lombok.Builder(builderClassName = "Builder", toBuilder = true)
public final class SdmxWebEntryPoint {

    @lombok.NonNull
    String name;

    @lombok.NonNull
    @lombok.Builder.Default
    String description = "";

    @lombok.NonNull
    URI uri;

    @lombok.Singular
    Map<String, String> properties;

    public static class Builder {

        @Nonnull
        public Builder uri(@Nonnull URI uri) {
            this.uri = uri;
            return this;
        }

        @Nonnull
        public Builder uri(@Nonnull String uri) {
            return uri(URI.create(uri));
        }
    }
}
