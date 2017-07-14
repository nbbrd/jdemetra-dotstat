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
package be.nbb.demetra.sdmx.file;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import ec.tss.tsproviders.DataSet;
import ec.tss.tsproviders.DataSource;
import ec.tss.tsproviders.cube.CubeId;
import ec.tss.tsproviders.cube.CubeSupport;
import ec.tss.tsproviders.utils.IConfig;
import ec.tss.tsproviders.utils.IParam;
import ec.tss.tsproviders.utils.Params;
import static ec.tss.tsproviders.utils.Params.onStringList;
import java.io.File;
import java.util.List;

/**
 *
 * @author Philippe Charles
 */
interface SdmxFileParam extends IParam<DataSource, SdmxFileBean> {

    String getVersion();

    IParam<DataSet, CubeId> getCubeIdParam(CubeId root);

    final class V1 implements SdmxFileParam {

        private final Splitter dimensionSplitter = Splitter.on(',').trimResults().omitEmptyStrings();
        private final Joiner dimensionJoiner = Joiner.on(',');

        private final IParam<DataSource, File> file = Params.onFile(new File(""), "f");
        private final IParam<DataSource, File> structureFile = Params.onFile(new File(""), "s");
        private final IParam<DataSource, List<String>> dimensionIds = onStringList(ImmutableList.of(), "d", dimensionSplitter, dimensionJoiner);
        private final IParam<DataSource, String> labelAttribute = Params.onString("", "l");

        @Override
        public String getVersion() {
            return "v1";
        }

        @Override
        public SdmxFileBean defaultValue() {
            SdmxFileBean result = new SdmxFileBean();
            result.setFile(file.defaultValue());
            result.setStructureFile(structureFile.defaultValue());
            result.setDimensions(dimensionIds.defaultValue());
            result.setLabelAttribute(labelAttribute.defaultValue());
            return result;
        }

        @Override
        public SdmxFileBean get(DataSource dataSource) {
            SdmxFileBean result = new SdmxFileBean();
            result.setFile(file.get(dataSource));
            result.setStructureFile(structureFile.get(dataSource));
            result.setDimensions(dimensionIds.get(dataSource));
            result.setLabelAttribute(labelAttribute.get(dataSource));
            return result;
        }

        @Override
        public void set(IConfig.Builder<?, DataSource> builder, SdmxFileBean value) {
            file.set(builder, value.getFile());
            structureFile.set(builder, value.getStructureFile());
            dimensionIds.set(builder, value.getDimensions());
            labelAttribute.set(builder, value.getLabelAttribute());
        }

        @Override
        public IParam<DataSet, CubeId> getCubeIdParam(CubeId root) {
            return CubeSupport.idBySeparator(root, ".", "k");
        }
    }
}
