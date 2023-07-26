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
package internal.sdmx;

import lombok.NonNull;
import standalone_sdmxdl.nbbrd.io.WrappedIOException;
import sdmxdl.DataStructure;
import sdmxdl.Key;
import ec.tss.tsproviders.cube.CubeAccessor;
import ec.tss.tsproviders.cube.CubeId;
import ec.tss.tsproviders.cursor.TsCursor;
import ec.tss.tsproviders.utils.IteratorWithIO;
import ec.tstoolkit.design.VisibleForTesting;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import standalone_sdmxdl.nbbrd.io.function.IOSupplier;
import sdmxdl.Connection;
import sdmxdl.Dataflow;
import sdmxdl.DataflowRef;
import sdmxdl.Dimension;
import sdmxdl.ext.SdmxCubeUtil;

/**
 *
 * @author Philippe Charles
 */
@lombok.RequiredArgsConstructor
public final class SdmxCubeAccessor implements CubeAccessor {

    public static SdmxCubeAccessor of(IOSupplier<Connection> supplier, DataflowRef ref, List<String> dimensions, String labelAttribute, String sourceLabel, boolean displayCodes) throws IOException {
        try (Connection conn = supplier.getWithIO()) {
            Dataflow flow = conn.getFlow(ref);
            DataStructure dsd = conn.getStructure(ref);
            CubeId root = getOrLoadRoot(dimensions, dsd);
            return new SdmxCubeAccessor(supplier, flow, dsd, root, labelAttribute, sourceLabel, displayCodes);
        }
    }

    private final IOSupplier<Connection> supplier;
    private final Dataflow flow;
    private final DataStructure dsd;
    private final CubeId root;
    private final String labelAttribute;
    private final String sourceLabel;
    private final boolean displayCodes;

    @Override
    public IOException testConnection() {
        try (Connection conn = supplier.getWithIO()) {
            conn.testConnection();
            return null;
        } catch (IOException ex) {
            return ex;
        }
    }

    @Override
    public @NonNull CubeId getRoot() {
        return root;
    }

    @Override
    public @NonNull TsCursor<CubeId> getAllSeries(@NonNull CubeId ref) throws IOException {
        Connection conn = supplier.getWithIO();
        try {
            return getAllSeries(conn, flow, dsd, ref, labelAttribute).onClose(conn);
        } catch (IOException ex) {
            throw close(conn, ex);
        } catch (RuntimeException ex) {
            throw WrappedIOException.wrap(close(conn, ex));
        }
    }

    @Override
    public @NonNull TsCursor<CubeId> getAllSeriesWithData(@NonNull CubeId ref) throws IOException {
        Connection conn = supplier.getWithIO();
        try {
            return getAllSeriesWithData(conn, flow, dsd, ref, labelAttribute).onClose(conn);
        } catch (IOException ex) {
            throw close(conn, ex);
        } catch (RuntimeException ex) {
            throw WrappedIOException.wrap(close(conn, ex));
        }
    }

    @Override
    public @NonNull TsCursor<CubeId> getSeriesWithData(@NonNull CubeId ref) throws IOException {
        Connection conn = supplier.getWithIO();
        try {
            return getSeriesWithData(conn, flow, dsd, ref, labelAttribute).onClose(conn);
        } catch (IOException ex) {
            throw close(conn, ex);
        } catch (RuntimeException ex) {
            throw WrappedIOException.wrap(close(conn, ex));
        }
    }

    @Override
    public @NonNull IteratorWithIO<CubeId> getChildren(@NonNull CubeId ref) throws IOException {
        Connection conn = supplier.getWithIO();
        try {
            return getChildren(conn, flow, dsd, ref).onClose(conn);
        } catch (IOException ex) {
            throw close(conn, ex);
        } catch (RuntimeException ex) {
            throw WrappedIOException.wrap(close(conn, ex));
        }
    }

    @Override
    public @NonNull String getDisplayName() {
        return String.format(Locale.ROOT, "%s ~ %s", sourceLabel, flow.getName());
    }

    @Override
    public @NonNull String getDisplayName(CubeId id) {
        if (id.isVoid()) {
            return "All";
        }
        return getKey(dsd, id).toString();
    }

    @Override
    public @NonNull String getDisplayNodeName(CubeId id) {
        if (id.isVoid()) {
            return "All";
        }
        return displayCodes
                ? getDimensionCodeId(id)
                : getDimensionCodeLabel(id, dsd);
    }

    //<editor-fold defaultstate="collapsed" desc="Implementation details">
    private static TsCursor<CubeId> getAllSeries(Connection conn, Dataflow flow, DataStructure dsd, CubeId node, String labelAttribute) throws IOException {
        KeyConverter converter = KeyConverter.of(dsd, node);
        return SdmxQueryUtil
                .getAllSeries(conn, flow.getRef(), converter.toKey(node), labelAttribute)
                .transform(converter::fromKey);
    }

    private static TsCursor<CubeId> getAllSeriesWithData(Connection conn, Dataflow flow, DataStructure dsd, CubeId node, String labelAttribute) throws IOException {
        KeyConverter converter = KeyConverter.of(dsd, node);
        return SdmxQueryUtil
                .getAllSeriesWithData(conn, flow.getRef(), converter.toKey(node), labelAttribute)
                .transform(converter::fromKey);
    }

    private static TsCursor<CubeId> getSeriesWithData(Connection conn, Dataflow flow, DataStructure dsd, CubeId leaf, String labelAttribute) throws IOException {
        KeyConverter converter = KeyConverter.of(dsd, leaf);
        return SdmxQueryUtil
                .getSeriesWithData(conn, flow.getRef(), converter.toKey(leaf), labelAttribute)
                .transform(converter::fromKey);
    }

    private static IteratorWithIO<CubeId> getChildren(Connection conn, Dataflow flow, DataStructure dsd, CubeId node) throws IOException {
        KeyConverter converter = KeyConverter.of(dsd, node);
        String dimensionId = node.getDimensionId(node.getLevel());
        int dimensionIndex = SdmxCubeUtil.getDimensionIndexById(dsd, dimensionId).orElseThrow(RuntimeException::new);
        List<String> children = SdmxQueryUtil.getChildren(conn, flow.getRef(), converter.toKey(node), dimensionIndex);
        return IteratorWithIO.from(children.iterator()).transform(node::child);
    }

    private static <EX extends Throwable> EX close(Connection conn, EX ex) {
        try {
            conn.close();
        } catch (IOException other) {
            ex.addSuppressed(other);
        }
        return ex;
    }

    private static String getDimensionCodeId(CubeId ref) {
        int index = ref.getLevel() - 1;
        return ref.getDimensionValue(index);
    }

    private static String getDimensionCodeLabel(CubeId ref, DataStructure dsd) {
        if (ref.isRoot()) {
            return "Invalid reference '" + dump(ref) + "'";
        }
        int index = ref.getLevel() - 1;
        Map<String, String> codes = SdmxCubeUtil.getDimensionById(dsd, ref.getDimensionId(index)).orElseThrow(RuntimeException::new).getCodes();
        String codeId = ref.getDimensionValue(index);
        return codes.getOrDefault(codeId, codeId);
    }

    private static String dump(CubeId ref) {
        return IntStream.range(0, ref.getMaxLevel())
                .mapToObj(o -> ref.getDimensionId(o) + "=" + (o < ref.getLevel() ? ref.getDimensionValue(0) : "null"))
                .collect(Collectors.joining(", "));
    }

    @VisibleForTesting
    static Key getKey(DataStructure dsd, CubeId ref) {
        if (ref.isRoot()) {
            return Key.ALL;
        }
        String[] result = new String[ref.getMaxLevel()];
        for (int i = 0; i < result.length; i++) {
            String id = ref.getDimensionId(i);
            String value = i < ref.getLevel() ? ref.getDimensionValue(i) : "";
            int index = SdmxCubeUtil.getDimensionIndexById(dsd, id).orElseThrow(RuntimeException::new);
            result[index] = value;
        }
        return Key.of(result);
    }

    @lombok.RequiredArgsConstructor
    static final class KeyConverter {

        static KeyConverter of(DataStructure dsd, CubeId ref) {
            return new KeyConverter(dsd, new CubeIdBuilder(dsd, ref));
        }

        final DataStructure dsd;
        final CubeIdBuilder builder;

        public Key toKey(CubeId a) {
            return getKey(dsd, a);
        }

        public CubeId fromKey(Key b) {
            return builder.getId(b);
        }
    }

    private static final class CubeIdBuilder {

        private final CubeId ref;
        private final int[] indices;
        private final String[] dimValues;

        CubeIdBuilder(DataStructure dsd, CubeId ref) {
            this.ref = ref;
            this.indices = new int[ref.getDepth()];
            for (int i = 0; i < indices.length; i++) {
                indices[i] = SdmxCubeUtil.getDimensionIndexById(dsd, ref.getDimensionId(ref.getLevel() + i)).orElseThrow(RuntimeException::new);
            }
            this.dimValues = new String[indices.length];
        }

        public CubeId getId(Key o) {
            for (int i = 0; i < indices.length; i++) {
                dimValues[i] = o.get(indices[i]);
            }
            return ref.child(dimValues);
        }
    }

    private static CubeId getOrLoadRoot(List<String> dimensions, DataStructure dsd) {
        return dimensions.isEmpty()
                ? CubeId.root(loadDefaultDimIds(dsd))
                : CubeId.root(dimensions);
    }

    private static List<String> loadDefaultDimIds(DataStructure dsd) {
        return dsd
                .getDimensions()
                .stream()
                .map(Dimension::getId)
                .collect(Collectors.toList());
    }
    //</editor-fold>
}
