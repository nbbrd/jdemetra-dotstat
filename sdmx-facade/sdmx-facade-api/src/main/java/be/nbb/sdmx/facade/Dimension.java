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
package be.nbb.sdmx.facade;

import java.util.Map;

/**
 * Defines dimension for the statistical cubes in SDMX.
 *
 * @author Philippe Charles
 */
@lombok.Value
@lombok.Builder(builderClassName = "Builder")
public class Dimension implements HasLabel {

    @lombok.NonNull
    String id;

    int position;

    /**
     * Non-null map of code description by code id that represents a codelist
     * (predefined sets of terms from which some statistical coded concepts take
     * their values).
     */
    @lombok.NonNull
    @lombok.Singular
    Map<String, String> codes;

    @lombok.NonNull
    String label;
}
