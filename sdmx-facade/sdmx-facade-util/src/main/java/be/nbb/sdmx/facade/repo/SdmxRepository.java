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
package be.nbb.sdmx.facade.repo;

import be.nbb.sdmx.facade.Series;
import be.nbb.sdmx.facade.DataCursor;
import be.nbb.sdmx.facade.DataStructure;
import be.nbb.sdmx.facade.Dataflow;
import be.nbb.sdmx.facade.DataflowRef;
import be.nbb.sdmx.facade.DataQuery;
import be.nbb.sdmx.facade.DataStructureRef;
import be.nbb.sdmx.facade.SdmxConnection;
import be.nbb.sdmx.facade.util.SdmxExceptions;
import be.nbb.sdmx.facade.util.SeriesSupport;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

/**
 *
 * @author Philippe Charles
 */
@lombok.Value
@lombok.Builder(builderClassName = "Builder")
public class SdmxRepository {

    @lombok.NonNull
    String name;

    @lombok.NonNull
    @lombok.Singular
    List<DataStructure> structures;

    @lombok.NonNull
    @lombok.Singular
    Set<Dataflow> flows;

    @lombok.NonNull
    Map<DataflowRef, List<Series>> data;

    @lombok.Builder.Default
    boolean seriesKeysOnlySupported = true;

    @Nonnull
    public SdmxConnection asConnection() {
        return new RepoConnection(this);
    }

    @Nonnull
    public Optional<DataStructure> getStructure(@Nonnull DataStructureRef ref) {
        Objects.requireNonNull(ref);
        return structures
                .stream()
                .filter(ref::equalsRef)
                .findFirst();
    }

    @Nonnull
    public Optional<Dataflow> getFlow(@Nonnull DataflowRef ref) {
        Objects.requireNonNull(ref);
        return flows
                .stream()
                .filter(ref::containsRef)
                .findFirst();
    }

    @Nonnull
    public Optional<DataCursor> getCursor(@Nonnull DataflowRef flowRef, @Nonnull DataQuery query) {
        return getData(flowRef).map(toCursor(query));
    }

    @Nonnull
    public Optional<Stream<Series>> getStream(@Nonnull DataflowRef flowRef, @Nonnull DataQuery query) {
        return getData(flowRef).map(toStream(query));
    }

    @Nonnull
    private Optional<List<Series>> getData(@Nonnull DataflowRef flowRef) {
        Objects.requireNonNull(flowRef);
        return Optional.ofNullable(data.get(flowRef));
    }

    @Nonnull
    private static Function<List<Series>, DataCursor> toCursor(@Nonnull DataQuery query) {
        Objects.requireNonNull(query);
        return o -> SeriesSupport.asCursor(o, query.getKey());
    }

    @Nonnull
    private static Function<List<Series>, Stream<Series>> toStream(@Nonnull DataQuery query) {
        Objects.requireNonNull(query);
        return o -> o.stream().filter(s -> query.getKey().contains(s.getKey()));
    }

    public static final class Builder {

        private final Map<DataflowRef, List<Series>> data = new HashMap<>();

        @Nonnull
        public Builder clearData() {
            this.data.clear();
            return this;
        }

        @Nonnull
        public Builder data(@Nonnull DataflowRef flowRef, @Nonnull Series series) {
            data.computeIfAbsent(flowRef, o -> new ArrayList<>()).add(series);
            return this;
        }

        @Nonnull
        public Builder data(@Nonnull DataflowRef flowRef, @Nonnull List<Series> list) {
            data.computeIfAbsent(flowRef, o -> new ArrayList<>()).addAll(list);
            return this;
        }

        @Nonnull
        public Builder copyOf(@Nonnull DataflowRef flowRef, @Nonnull DataCursor cursor) throws IOException {
            return data(flowRef, SeriesSupport.copyOf(cursor));
        }
    }

    //<editor-fold defaultstate="collapsed" desc="Implementation details">
    private static final class RepoConnection implements SdmxConnection {

        private final SdmxRepository repo;
        private boolean closed;

        private RepoConnection(SdmxRepository repo) {
            this.repo = repo;
            this.closed = false;
        }

        @Override
        public Set<Dataflow> getFlows() throws IOException {
            checkState();
            return repo.getFlows();
        }

        @Override
        public Dataflow getFlow(DataflowRef flowRef) throws IOException {
            checkState();
            return repo
                    .getFlow(flowRef)
                    .orElseThrow(() -> SdmxExceptions.missingFlow(flowRef));
        }

        @Override
        public DataStructure getStructure(DataflowRef flowRef) throws IOException {
            checkState();
            DataStructureRef structRef = getFlow(flowRef).getStructureRef();
            return repo
                    .getStructure(structRef)
                    .orElseThrow(() -> SdmxExceptions.missingStructure(structRef));
        }

        @Override
        public DataCursor getCursor(DataflowRef flowRef, DataQuery query) throws IOException {
            checkState();
            return repo
                    .getCursor(flowRef, query)
                    .orElseThrow(() -> SdmxExceptions.missingData(flowRef));
        }

        @Override
        public Stream<Series> getStream(DataflowRef flowRef, DataQuery query) throws IOException {
            checkState();
            return repo
                    .getStream(flowRef, query)
                    .orElseThrow(() -> SdmxExceptions.missingData(flowRef));
        }

        @Override
        public boolean isSeriesKeysOnlySupported() {
            return repo.isSeriesKeysOnlySupported();
        }

        @Override
        public void close() throws IOException {
            closed = true;
        }

        private void checkState() throws IOException {
            if (closed) {
                throw SdmxExceptions.connectionClosed();
            }
        }
    }
    //</editor-fold>}
}
