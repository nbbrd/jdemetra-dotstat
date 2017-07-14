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

import be.nbb.sdmx.facade.LanguagePriorityList;
import static be.nbb.sdmx.facade.LanguagePriorityList.ANY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.Test;

/**
 *
 * @author Philippe Charles
 */
public class TextBuilderTest {

    @Test
    @SuppressWarnings("null")
    public void test() {
        assertThat(new TextBuilder(ANY).build()).isNull();
        assertThat(new TextBuilder(ANY).put("en", null).build()).isNull();
        assertThat(new TextBuilder(ANY).put("en", "hello").clear().build()).isNull();
        assertThat(new TextBuilder(ANY).put("en", "hello").build()).isEqualTo("hello");
        assertThat(new TextBuilder(ANY).put("en", "hello").put("fr", "bonjour").build()).isEqualTo("hello");
        assertThat(new TextBuilder(ANY).put("fr", "bonjour").put("en", "hello").build()).isEqualTo("bonjour");

        assertThatThrownBy(() -> new TextBuilder(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new TextBuilder(ANY).put(null, "hello")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new TextBuilder(ANY).put("en", "hello").build(null)).isInstanceOf(NullPointerException.class);

        assertThat(new TextBuilder(LanguagePriorityList.parse("fr")).put("en", "hello").put("fr", "bonjour").build()).isEqualTo("bonjour");
        assertThat(new TextBuilder(LanguagePriorityList.parse("en")).put("fr", "bonjour").put("en", "hello").build()).isEqualTo("hello");
    }
}
