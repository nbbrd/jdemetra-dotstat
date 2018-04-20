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
package internal.file.xml;

import be.nbb.sdmx.facade.xml.Sdmxml;
import static be.nbb.sdmx.facade.xml.Sdmxml.*;
import be.nbb.util.StaxUtil;
import static internal.file.SdmxDecoder.DataType.*;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import internal.file.SdmxDecoder;
import ioutil.Stax;
import ioutil.Xml;
import java.net.URI;

/**
 *
 * @author Philippe Charles
 */
final class DataTypeProbe {

    public static Xml.Parser<SdmxDecoder.DataType> of() {
        return Stax.StreamParser.valueOf(DataTypeProbe::probeDataType);
    }

    private static SdmxDecoder.DataType probeDataType(XMLStreamReader reader) throws XMLStreamException {
        if (StaxUtil.isNotNamespaceAware(reader)) {
            throw new XMLStreamException("Cannot probe data type");
        }

        int level = 0;
        while (reader.hasNext()) {
            switch (reader.next()) {
                case START_ELEMENT:
                    level++;
                    if (level == 2 && reader.getLocalName().equals("Header")) {
                        URI uri = URI.create(reader.getNamespaceURI());
                        if (Sdmxml.equals(NS_V10_URI, uri)) {
                            return UNKNOWN;
                        } else if (Sdmxml.equals(NS_V20_URI, uri)) {
                            while (reader.hasNext()) {
                                switch (reader.next()) {
                                    case START_ELEMENT:
                                        level++;
                                        if (level == 3 && reader.getLocalName().equals("KeyFamilyRef")) {
                                            return GENERIC20;
                                        }
                                        break;
                                    case END_ELEMENT:
                                        level--;
                                        break;
                                }
                            }
                            return COMPACT20;
                        } else if (Sdmxml.equals(NS_V21_URI, uri)) {
                            while (reader.hasNext()) {
                                switch (reader.next()) {
                                    case START_ELEMENT:
                                        level++;
                                        if (level == 4 && reader.getLocalName().equals("SeriesKey")) {
                                            return GENERIC21;
                                        }
                                        break;
                                    case END_ELEMENT:
                                        level--;
                                        break;
                                }
                            }
                            return COMPACT21;
                        }
                    }
                    break;
                case END_ELEMENT:
                    level--;
                    break;
            }
        }
        return UNKNOWN;
    }
}
