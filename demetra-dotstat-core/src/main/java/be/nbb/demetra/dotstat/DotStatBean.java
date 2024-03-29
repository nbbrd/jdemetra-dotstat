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
package be.nbb.demetra.dotstat;

import ec.tss.tsproviders.DataSource;
import ec.tss.tsproviders.db.DbBean;
import sdmxdl.FlowRef;

/**
 * @author Philippe Charles
 */
@Deprecated
public final class DotStatBean extends DbBean.BulkBean {

    public DotStatBean() {
    }

    public DotStatBean(DataSource id) {
        super(id);
    }

    public FlowRef getFlowRef() throws IllegalArgumentException {
        return FlowRef.parse(getTableName());
    }

    public void setFlowRef(FlowRef flowRef) {
        setTableName(flowRef.toString());
    }
}
