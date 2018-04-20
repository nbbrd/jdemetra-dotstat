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
package be.nbb.sdmx.facade.web;

import be.nbb.sdmx.facade.web.spi.SdmxWebContext;
import be.nbb.sdmx.facade.LanguagePriorityList;
import be.nbb.sdmx.facade.SdmxConnection;
import be.nbb.sdmx.facade.repo.SdmxRepository;
import be.nbb.sdmx.facade.tck.ConnectionSupplierAssert;
import be.nbb.sdmx.facade.web.spi.SdmxWebDriver;
import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import org.junit.Test;

/**
 *
 * @author Philippe Charles
 */
public class SdmxWebManagerTest {

    @Test
    public void testCompliance() {
        ConnectionSupplierAssert.assertCompliance(SdmxWebManager.of(REPO), HELLO.getName(), "ko");
    }

    @Test
    @SuppressWarnings("null")
    public void testFactories() {
        assertThatNullPointerException().isThrownBy(() -> SdmxWebManager.of((Iterable) null));
        assertThatNullPointerException().isThrownBy(() -> SdmxWebManager.of((SdmxWebDriver[]) null));
    }

    @Test
    @SuppressWarnings("null")
    public void testGetConnectionOfSource() {
        SdmxWebManager manager = SdmxWebManager.of(REPO);
        assertThatNullPointerException().isThrownBy(() -> manager.getConnection((SdmxWebSource) null));
        assertThatIOException().isThrownBy(() -> manager.getConnection(HELLO.toBuilder().endpointOf("http://ko").build()));
    }

    private static final SdmxWebSource HELLO = SdmxWebSource.builder().name("ok").driver(RepoDriver.NAME).endpointOf("http://r1").build();
    private static final SdmxWebDriver REPO = new RepoDriver();

    private static final class RepoDriver implements SdmxWebDriver {

        static final String NAME = "repo";

        final List<SdmxRepository> repos = Collections.singletonList(SdmxRepository.builder().name("http://r1").build());

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public SdmxWebConnection connect(SdmxWebSource source, LanguagePriorityList langs, SdmxWebContext context) throws IOException {
            return repos.stream()
                    .filter(o -> o.getName().equals(source.getEndpoint().toString()))
                    .findFirst()
                    .map(o -> new RepoWebConnection(o.asConnection()))
                    .orElseThrow(IOException::new);
        }

        @Override
        public Collection<SdmxWebSource> getDefaultSources() {
            return Collections.singletonList(HELLO);
        }

        @Override
        public Collection<String> getSupportedProperties() {
            return Collections.emptyList();
        }
    }

    @lombok.AllArgsConstructor
    private static final class RepoWebConnection implements SdmxWebConnection {

        @lombok.experimental.Delegate
        private final SdmxConnection delegate;

        @Override
        public Duration ping() throws IOException {
            return Duration.ZERO;
        }
    }
}
