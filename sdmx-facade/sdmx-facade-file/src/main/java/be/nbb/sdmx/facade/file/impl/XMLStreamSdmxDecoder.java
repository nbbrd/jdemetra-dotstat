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
package be.nbb.sdmx.facade.file.impl;

import be.nbb.sdmx.facade.DataStructure;
import be.nbb.sdmx.facade.LanguagePriorityList;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.xml.stream.XMLInputFactory;
import be.nbb.sdmx.facade.file.SdmxDecoder;
import static be.nbb.sdmx.facade.file.SdmxDecoder.DataType.COMPACT20;
import static be.nbb.sdmx.facade.file.SdmxDecoder.DataType.COMPACT21;
import static be.nbb.sdmx.facade.file.SdmxDecoder.DataType.GENERIC20;
import static be.nbb.sdmx.facade.file.SdmxDecoder.DataType.GENERIC21;
import be.nbb.sdmx.facade.file.SdmxFile;
import be.nbb.sdmx.facade.xml.stream.SdmxXmlStreams;
import be.nbb.sdmx.facade.xml.stream.XMLStream;
import java.io.Reader;
import java.nio.file.Files;
import java.util.List;

/**
 *
 * @author Philippe Charles
 */
public final class XMLStreamSdmxDecoder implements SdmxDecoder {

    private final XMLInputFactory factory;

    public XMLStreamSdmxDecoder(XMLInputFactory factory) {
        this.factory = factory;
    }

    @Override
    public Info decode(SdmxFile file, LanguagePriorityList langs) throws IOException {
        DataType dataType = probeDataType(file.getData());
        return Info.of(dataType, file.getStructure() != null
                ? parseStruct(dataType, file.getStructure(), langs)
                : decodeStruct(dataType, file.getData()));
    }

    private DataType probeDataType(File data) throws IOException {
        try (Reader stream = Files.newBufferedReader(data.toPath())) {
            return DataTypeProbe.probeDataType(factory, stream);
        }
    }

    private DataStructure parseStruct(DataType dataType, File structure, LanguagePriorityList langs) throws IOException {
        return getStructSupplier(dataType, langs).get(factory, structure.toPath(), StandardCharsets.UTF_8).get(0);
    }

    private XMLStream<List<DataStructure>> getStructSupplier(DataType o, LanguagePriorityList langs) throws IOException {
        switch (o) {
            case GENERIC20:
            case COMPACT20:
                return SdmxXmlStreams.struct20(langs);
            case GENERIC21:
            case COMPACT21:
                return SdmxXmlStreams.struct21(langs);
            default:
                throw new IOException("Don't know how to handle '" + o + "'");
        }
    }

    private DataStructure decodeStruct(DataType dataType, File data) throws IOException {
        try (Reader stream = Files.newBufferedReader(data.toPath(), StandardCharsets.UTF_8)) {
            return DataStructureDecoder.decodeDataStructure(dataType, factory, stream);
        }
    }
}
