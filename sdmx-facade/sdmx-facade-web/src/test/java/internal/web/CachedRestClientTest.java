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
package internal.web;

import test.client.CallStackClient;
import be.nbb.sdmx.facade.DataQuery;
import be.nbb.sdmx.facade.Key;
import be.nbb.sdmx.facade.util.TypedId;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;
import org.junit.Test;
import test.samples.FacadeResource;
import static test.samples.FacadeResource.ECB_FLOW_REF;
import static test.samples.FacadeResource.ECB_STRUCT_REF;
import test.client.RepoRestClient;

/**
 *
 * @author Philippe Charles
 */
public class CachedRestClientTest {

    @Test
    public void testGetFlows() throws IOException {
        AtomicInteger count = new AtomicInteger();
        ConcurrentHashMap cache = new ConcurrentHashMap();
        FakeClock clock = new FakeClock();

        CachedRestClient target = new CachedRestClient(getClient(count), "", cache, clock, 100);

        target.getFlows();
        assertThat(count).hasValue(1);
        assertThat(cache).containsOnlyKeys(flowsId);

        target.getFlows();
        assertThat(count).hasValue(1);
        assertThat(cache).containsOnlyKeys(flowsId);

        clock.plus(100);
        target.getFlows();
        assertThat(count).hasValue(2);
        assertThat(cache).containsOnlyKeys(flowsId);

        cache.clear();
        target.getFlows();
        assertThat(count).hasValue(3);
        assertThat(cache).containsOnlyKeys(flowsId);
    }

    @Test
    public void testGetFlow() throws IOException {
        AtomicInteger count = new AtomicInteger();
        ConcurrentHashMap cache = new ConcurrentHashMap();
        FakeClock clock = new FakeClock();

        CachedRestClient target = new CachedRestClient(getClient(count), "", cache, clock, 100);

        assertThatNullPointerException().isThrownBy(() -> target.getFlow(null));

        target.getFlow(ECB_FLOW_REF);
        assertThat(count).hasValue(1);
        assertThat(cache).containsOnlyKeys(flowId);

        target.getFlow(ECB_FLOW_REF);
        assertThat(count).hasValue(1);
        assertThat(cache).containsOnlyKeys(flowId);

        clock.plus(100);
        target.getFlow(ECB_FLOW_REF);
        assertThat(count).hasValue(2);
        assertThat(cache).containsOnlyKeys(flowId);

        cache.clear();
        target.getFlow(ECB_FLOW_REF);
        assertThat(count).hasValue(3);
        assertThat(cache).containsOnlyKeys(flowId);

        cache.clear();
        target.getFlows();
        target.getFlow(ECB_FLOW_REF);
        assertThat(count).hasValue(4);
        assertThat(cache).containsOnlyKeys(flowsId);
    }

    @Test
    public void testGetStructure() throws IOException {
        AtomicInteger count = new AtomicInteger();
        ConcurrentHashMap cache = new ConcurrentHashMap();
        FakeClock clock = new FakeClock();

        CachedRestClient target = new CachedRestClient(getClient(count), "", cache, clock, 100);

        assertThatNullPointerException().isThrownBy(() -> target.getStructure(null));

        target.getStructure(ECB_STRUCT_REF);
        assertThat(count).hasValue(1);
        assertThat(cache).containsOnlyKeys(structId);

        target.getStructure(ECB_STRUCT_REF);
        assertThat(count).hasValue(1);
        assertThat(cache).containsOnlyKeys(structId);

        clock.plus(100);
        target.getStructure(ECB_STRUCT_REF);
        assertThat(count).hasValue(2);
        assertThat(cache).containsOnlyKeys(structId);

        cache.clear();
        target.getStructure(ECB_STRUCT_REF);
        assertThat(count).hasValue(3);
        assertThat(cache).containsOnlyKeys(structId);
    }

    @Test
    public void testLoadData() throws IOException {
        AtomicInteger count = new AtomicInteger();
        ConcurrentHashMap cache = new ConcurrentHashMap();
        FakeClock clock = new FakeClock();

        CachedRestClient target = new CachedRestClient(getClient(count), "", cache, clock, 100);

        assertThatNullPointerException().isThrownBy(() -> target.getData(null, query, null));

        target.getData(ECB_FLOW_REF, query, null);
        assertThat(count).hasValue(1);
        assertThat(cache).containsOnlyKeys(keysId);

        target.getData(ECB_FLOW_REF, query, null);
        assertThat(count).hasValue(1);
        assertThat(cache).containsOnlyKeys(keysId);

        clock.plus(100);
        target.getData(ECB_FLOW_REF, query, null);
        assertThat(count).hasValue(2);
        assertThat(cache).containsOnlyKeys(keysId);

        cache.clear();
        target.getData(ECB_FLOW_REF, query, null);
        assertThat(count).hasValue(3);
        assertThat(cache).containsOnlyKeys(keysId);
    }

    private final DataQuery query = DataQuery.of(Key.ALL, true);
    private final TypedId<?> flowsId = TypedId.of("flows://");
    private final TypedId<?> flowId = TypedId.of("flow://").with(ECB_FLOW_REF);
    private final TypedId<?> structId = TypedId.of("struct://").with(ECB_STRUCT_REF);
    private final TypedId<?> keysId = TypedId.of("keys://").with(ECB_FLOW_REF);

    private static final class FakeClock extends Clock {

        private Instant current = Instant.now();

        void plus(long durationInMillis) {
            current = current.plus(100, ChronoUnit.MILLIS);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.systemDefault();
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current;
        }
    }

    private static RestClient getClient(AtomicInteger count) throws IOException {
        RestClient original = RepoRestClient.of(FacadeResource.ecb());
        return CallStackClient.of(original, count);
    }
}
