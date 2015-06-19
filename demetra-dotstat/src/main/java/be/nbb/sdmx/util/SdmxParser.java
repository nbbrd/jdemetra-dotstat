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
package be.nbb.sdmx.util;

import be.nbb.sdmx.DataCursor;
import be.nbb.sdmx.DataStructure;
import be.nbb.sdmx.Dimension;
import be.nbb.sdmx.Key;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.io.InputStreamReader;
import javax.annotation.Nonnull;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

/**
 *
 * @author Philippe Charles
 */
public abstract class SdmxParser {

    @Nonnull
    abstract public DataCursor genericData20(@Nonnull InputStreamReader stream, @Nonnull DataStructure dsd) throws IOException;
    
    @Nonnull
    abstract public DataCursor genericData21(@Nonnull InputStreamReader stream, @Nonnull DataStructure dsd) throws IOException;

    @Nonnull
    abstract public DataCursor compactData21(@Nonnull InputStreamReader stream, @Nonnull DataStructure dsd) throws IOException;

    public static SdmxParser getDefault() {
        return DefaultParser.INSTANCE;
    }

    //<editor-fold defaultstate="collapsed" desc="Implementation details">
    private static final class DefaultParser extends SdmxParser {

        private static final DefaultParser INSTANCE = new DefaultParser();

        private final XMLInputFactory factory;

        private DefaultParser() {
            this.factory = XMLInputFactory.newInstance();
        }

        @Override
        public DataCursor genericData20(InputStreamReader stream, DataStructure dsd) throws IOException {
            try {
                return new XMLStreamGenericDataCursor20(factory.createXMLStreamReader(stream), Key.builder(dsd));
            } catch (XMLStreamException ex) {
                throw new IOException(ex);
            }
        }
        
        @Override
        public DataCursor genericData21(InputStreamReader stream, DataStructure dsd) throws IOException {
            try {
                return new XMLStreamGenericDataCursor21(factory.createXMLStreamReader(stream), Key.builder(dsd), getFrequencyCodeIdIndex(dsd));
            } catch (XMLStreamException ex) {
                throw new IOException(ex);
            }
        }

        @Override
        public DataCursor compactData21(InputStreamReader stream, DataStructure dsd) throws IOException {
            try {
                return new XMLStreamCompactDataCursor21(factory.createXMLStreamReader(stream), Key.builder(dsd), getFrequencyCodeIdIndex(dsd), dsd.getTimeDimensionId(), dsd.getPrimaryMeasureId());
            } catch (XMLStreamException ex) {
                throw new IOException(ex);
            }
        }

        private static int getFrequencyCodeIdIndex(DataStructure dfs) {
            Optional<Dimension> dimension = Iterables.tryFind(dfs.getDimensions(), FREQ_PREDICATE);
            return dimension.isPresent() ? (dimension.get().getPosition() - 1) : XMLStreamGenericDataCursor21.NO_FREQUENCY_CODE_ID_INDEX;
        }

        private static final Predicate<Dimension> FREQ_PREDICATE = new Predicate<Dimension>() {
            @Override
            public boolean apply(Dimension input) {
                switch (input.getId()) {
                    case "FREQ":
                    case "FREQUENCY":
                        return true;
                    default:
                        return false;
                }
            }
        };
    }
    //</editor-fold>
}
