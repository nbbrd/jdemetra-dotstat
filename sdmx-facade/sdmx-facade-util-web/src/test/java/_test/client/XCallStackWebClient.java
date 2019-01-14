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
package _test.client;

import be.nbb.sdmx.facade.DataCursor;
import be.nbb.sdmx.facade.DataStructure;
import be.nbb.sdmx.facade.DataStructureRef;
import be.nbb.sdmx.facade.Dataflow;
import be.nbb.sdmx.facade.DataflowRef;
import internal.web.DataRequest;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.Duration;
import internal.web.SdmxWebClient;

/**
 *
 * @author Philippe Charles
 */
@lombok.RequiredArgsConstructor(staticName = "of")
public final class XCallStackWebClient implements SdmxWebClient {

    @lombok.NonNull
    private final SdmxWebClient delegate;

    @lombok.NonNull
    private final AtomicInteger count;

    @Override
    public String getName() throws IOException {
        return delegate.getName();
    }

    @Override
    public List<Dataflow> getFlows() throws IOException {
        count.incrementAndGet();
        return delegate.getFlows();
    }

    @Override
    public Dataflow getFlow(DataflowRef ref) throws IOException {
        count.incrementAndGet();
        return delegate.getFlow(ref);
    }

    @Override
    public DataStructure getStructure(DataStructureRef ref) throws IOException {
        count.incrementAndGet();
        return delegate.getStructure(ref);
    }

    @Override
    public DataCursor getData(DataRequest request, DataStructure dsd) throws IOException {
        count.incrementAndGet();
        return delegate.getData(request, dsd);
    }

    @Override
    public boolean isSeriesKeysOnlySupported() throws IOException {
        return delegate.isSeriesKeysOnlySupported();
    }

    @Override
    public DataStructureRef peekStructureRef(DataflowRef flowRef) throws IOException {
        return delegate.peekStructureRef(flowRef);
    }

    @Override
    public Duration ping() throws IOException {
        return delegate.ping();
    }
}
