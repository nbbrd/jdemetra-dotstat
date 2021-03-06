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

import sdmxdl.DataStructure;
import sdmxdl.DataflowRef;
import sdmxdl.Dimension;
import sdmxdl.Key;
import sdmxdl.SdmxConnection;
import com.google.common.base.Converter;
import com.google.common.collect.Maps;
import ec.tss.tsproviders.cube.CubeAccessor;
import ec.tss.tsproviders.cube.CubeId;
import ec.tss.tsproviders.cursor.TsCursor;
import ec.tss.tsproviders.utils.IteratorWithIO;
import ec.tstoolkit.design.VisibleForTesting;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import nbbrd.io.function.IOSupplier;

/**
 *
 * @author Philippe Charles
 */
@lombok.AllArgsConstructor(staticName = "of")
public final class SdmxCubeAccessor implements CubeAccessor {

    private final IOSupplier<SdmxConnection> supplier;
    private final DataflowRef flowRef;
    private final CubeId root;
    private final String labelAttribute;
    private final String sourceLabel;
    private final boolean displayCodes;

    @Override
    public IOException testConnection() {
        return null;
    }

    @Override
    public CubeId getRoot() {
        return root;
    }

    @Override
    public TsCursor<CubeId> getAllSeries(CubeId ref) throws IOException {
        SdmxConnection conn = supplier.getWithIO();
        try {
            return getAllSeriesCursor(conn, flowRef, ref, labelAttribute).onClose(conn);
        } catch (IOException ex) {
            throw close(conn, ex);
        } catch (RuntimeException ex) {
            throw new UnexpectedIOException(close(conn, ex));
        }
    }

    @Override
    public TsCursor<CubeId> getAllSeriesWithData(CubeId ref) throws IOException {
        SdmxConnection conn = supplier.getWithIO();
        try {
            return getAllSeriesWithDataCursor(conn, flowRef, ref, labelAttribute).onClose(conn);
        } catch (IOException ex) {
            throw close(conn, ex);
        } catch (RuntimeException ex) {
            throw new UnexpectedIOException(close(conn, ex));
        }
    }

    @Override
    public TsCursor<CubeId> getSeriesWithData(CubeId ref) throws IOException {
        SdmxConnection conn = supplier.getWithIO();
        try {
            return getSeriesWithDataCursor(conn, flowRef, ref, labelAttribute).onClose(conn);
        } catch (IOException ex) {
            throw close(conn, ex);
        } catch (RuntimeException ex) {
            throw new UnexpectedIOException(close(conn, ex));
        }
    }

    @Override
    public IteratorWithIO<CubeId> getChildren(CubeId ref) throws IOException {
        SdmxConnection conn = supplier.getWithIO();
        try {
            return getChildren(conn, flowRef, ref).onClose(conn);
        } catch (IOException ex) {
            throw close(conn, ex);
        } catch (RuntimeException ex) {
            throw new UnexpectedIOException(close(conn, ex));
        }
    }

    @Override
    public String getDisplayName() throws IOException {
        try (SdmxConnection conn = supplier.getWithIO()) {
            return String.format("%s ~ %s", sourceLabel, conn.getFlow(flowRef).getLabel());
        } catch (RuntimeException ex) {
            throw new UnexpectedIOException(ex);
        }
    }

    @Override
    public String getDisplayName(CubeId id) throws IOException {
        if (id.isVoid()) {
            return "All";
        }
        try (SdmxConnection conn = supplier.getWithIO()) {
            Map<String, Dimension> dimensionById = dimensionById(conn.getStructure(flowRef));
            return getKey(dimensionById, id).toString();
        } catch (RuntimeException ex) {
            throw new UnexpectedIOException(ex);
        }
    }

    @Override
    public String getDisplayNodeName(CubeId id) throws IOException {
        if (id.isVoid()) {
            return "All";
        }
        try (SdmxConnection conn = supplier.getWithIO()) {
            return displayCodes
                    ? getDimensionCodeId(id)
                    : getDimensionCodeLabel(id, dimensionById(conn.getStructure(flowRef)));
        } catch (RuntimeException ex) {
            throw new UnexpectedIOException(ex);
        }
    }

    //<editor-fold defaultstate="collapsed" desc="Implementation details">
    private static TsCursor<CubeId> getAllSeriesCursor(SdmxConnection conn, DataflowRef flowRef, CubeId ref, String labelAttribute) throws IOException {
        Converter<CubeId, Key> converter = getConverter(conn.getStructure(flowRef), ref);

        Key colKey = converter.convert(ref);
        TsCursor<Key> cursor = SdmxQueryUtil.getAllSeries(conn, flowRef, colKey, labelAttribute);

        return cursor.transform(converter.reverse()::convert);
    }

    private static TsCursor<CubeId> getAllSeriesWithDataCursor(SdmxConnection conn, DataflowRef flowRef, CubeId ref, String labelAttribute) throws IOException {
        Converter<CubeId, Key> converter = getConverter(conn.getStructure(flowRef), ref);

        Key colKey = converter.convert(ref);
        TsCursor<Key> cursor = SdmxQueryUtil.getAllSeriesWithData(conn, flowRef, colKey, labelAttribute);

        return cursor.transform(converter.reverse()::convert);
    }

    private static TsCursor<CubeId> getSeriesWithDataCursor(SdmxConnection conn, DataflowRef flowRef, CubeId ref, String labelAttribute) throws IOException {
        Converter<CubeId, Key> converter = getConverter(conn.getStructure(flowRef), ref);

        Key seriesKey = converter.convert(ref);
        TsCursor<Key> cursor = SdmxQueryUtil.getSeriesWithData(conn, flowRef, seriesKey, labelAttribute);

        return cursor.transform(converter.reverse()::convert);
    }

    private static IteratorWithIO<CubeId> getChildren(SdmxConnection conn, DataflowRef flowRef, CubeId ref) throws IOException {
        DataStructure dsd = conn.getStructure(flowRef);
        Converter<CubeId, Key> converter = getConverter(dsd, ref);
        int dimensionPosition = dimensionById(dsd).get(ref.getDimensionId(ref.getLevel())).getPosition();
        List<String> children = SdmxQueryUtil.getChildren(conn, flowRef, converter.convert(ref), dimensionPosition);
        return IteratorWithIO.from(children.iterator()).transform(ref::child);
    }

    private static <EX extends Throwable> EX close(SdmxConnection conn, EX ex) {
        try {
            conn.close();
        } catch (IOException other) {
            ex.addSuppressed(other);
        }
        return ex;
    }

    private static String getDimensionCodeId(CubeId ref) {
        int index = ref.getLevel() - 1;
        String codeId = ref.getDimensionValue(index);
        return codeId;
    }

    private static String getDimensionCodeLabel(CubeId ref, Map<String, Dimension> dimensionById) {
        if (ref.isRoot()) {
            return "Invalid reference '" + dump(ref) + "'";
        }
        int index = ref.getLevel() - 1;
        String codeId = ref.getDimensionValue(index);
        Map<String, String> codes = dimensionById.get(ref.getDimensionId(index)).getCodes();
        return codes.getOrDefault(codeId, codeId);
    }

    private static String dump(CubeId ref) {
        return IntStream.range(0, ref.getMaxLevel())
                .mapToObj(o -> ref.getDimensionId(o) + "=" + (o < ref.getLevel() ? ref.getDimensionValue(0) : "null"))
                .collect(Collectors.joining(", "));
    }

    @VisibleForTesting
    static Key getKey(Map<String, Dimension> dimensionById, CubeId ref) {
        if (ref.isRoot()) {
            return Key.ALL;
        }
        String[] result = new String[ref.getMaxLevel()];
        for (int i = 0; i < result.length; i++) {
            result[dimensionById.get(ref.getDimensionId(i)).getPosition() - 1] = i < ref.getLevel() ? ref.getDimensionValue(i) : "";
        }
        return Key.of(result);
    }

    static Converter<CubeId, Key> getConverter(DataStructure ds, CubeId ref) {
        final Map<String, Dimension> dimensionById = dimensionById(ds);
        final CubeIdBuilder builder = new CubeIdBuilder(dimensionById, ref);
        return new Converter<CubeId, Key>() {
            @Override
            protected Key doForward(CubeId a) {
                return getKey(dimensionById, a);
            }

            @Override
            protected CubeId doBackward(Key b) {
                return builder.getId(b);
            }
        };
    }

    private static final class CubeIdBuilder {

        private final CubeId ref;
        private final int[] indices;
        private final String[] dimValues;

        CubeIdBuilder(Map<String, Dimension> dimensionById, CubeId ref) {
            this.ref = ref;
            this.indices = new int[ref.getDepth()];
            for (int i = 0; i < indices.length; i++) {
                indices[i] = dimensionById.get(ref.getDimensionId(ref.getLevel() + i)).getPosition() - 1;
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

    @VisibleForTesting
    static Map<String, Dimension> dimensionById(DataStructure ds) {
        return Maps.uniqueIndex(ds.getDimensions(), Dimension::getId);
    }
    //</editor-fold>
}
