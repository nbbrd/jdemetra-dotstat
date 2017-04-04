/*
 * Copyright 2016 National Bank of Belgium
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
package be.nbb.demetra.sdmx2;

import ec.tss.tsproviders.DataSource;
import ec.tss.tsproviders.utils.IConfig;
import ec.tss.tsproviders.utils.IParam;
import ec.tss.tsproviders.utils.Params;
import java.io.File;

/**
 *
 * @author Philippe Charles
 */
interface SdmxParam extends IParam<DataSource, SdmxBean2> {

    String getVersion();

    static final class V1 implements SdmxParam {

        private final IParam<DataSource, File> file = Params.onFile(new File(""), "url");
        private final IParam<DataSource, String> factory = Params.onString("Cunning plan", "factory");
        private final IParam<DataSource, String> titleAttribute = Params.onString("", "titleAttribute");

        @Override
        public String getVersion() {
            return "20150909";
        }

        @Override
        public SdmxBean2 defaultValue() {
            SdmxBean2 result = new SdmxBean2();
            result.setFile(file.defaultValue());
            result.setFactory(factory.defaultValue());
            result.setTitleAttribute(titleAttribute.defaultValue());
            return result;
        }

        @Override
        public SdmxBean2 get(DataSource dataSource) {
            SdmxBean2 result = new SdmxBean2();
            result.setFile(file.get(dataSource));
            result.setFactory(factory.get(dataSource));
            result.setTitleAttribute(titleAttribute.get(dataSource));
            return result;
        }

        @Override
        public void set(IConfig.Builder<?, DataSource> builder, SdmxBean2 value) {
            file.set(builder, value.getFile());
            factory.set(builder, value.getFactory());
            titleAttribute.set(builder, value.getTitleAttribute());
        }
    }
}
