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
package be.nbb.sdmx;

import com.google.common.base.Strings;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

/**
 * Identifier of a data flow used in a data (or meta data) query.
 * <p>
 * The syntax is agency id, artefact id, version, separated by a “,”. For
 * example: AGENCY_ID,FLOW_ID,VERSION
 * <p>
 * In case the string only contains one out of these 3 elements, it is
 * considered to be the flow id, i.e. ALL,FLOW_ID,LATEST
 * <p>
 * In case the string only contains two out of these 3 elements, they are
 * considered to be the agency id and the flow id, i.e. AGENCY_ID,FLOW_ID,LATEST
 *
 * @author Philippe Charles
 */
@Immutable
public final class FlowRef {

    public static final String ALL_AGENCIES = "all";
    public static final String LATEST_VERSION = "latest";

    private final String agencyId;
    private final String flowId;
    private final String version;

    public FlowRef(String agencyId, String flowId, String version) {
        this.agencyId = Strings.isNullOrEmpty(agencyId) ? ALL_AGENCIES : agencyId;
        this.flowId = flowId;
        this.version = Strings.isNullOrEmpty(version) ? LATEST_VERSION : version;
    }

    @Nonnull
    public String getAgencyId() {
        return agencyId;
    }

    @Nonnull
    public String getFlowId() {
        return flowId;
    }

    @Nonnull
    public String getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return agencyId + "," + flowId + "," + version;
    }

    @Override
    public int hashCode() {
        return Objects.hash(agencyId, flowId, version);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof FlowRef && equals((FlowRef) obj));
    }

    private boolean equals(FlowRef that) {
        return Objects.equals(this.agencyId, that.agencyId)
                && Objects.equals(this.flowId, that.flowId)
                && Objects.equals(this.version, that.version);
    }

    @Nonnull
    public static FlowRef parse(@Nonnull String input) throws IllegalArgumentException {
        String[] items = input.split(",");
        switch (items.length) {
            case 3:
                return new FlowRef(items[0], items[1], items[2]);
            case 2:
                return new FlowRef(items[0], items[1], LATEST_VERSION);
            case 1:
                return new FlowRef(ALL_AGENCIES, items[0], LATEST_VERSION);
            default:
                throw new IllegalArgumentException(input);
        }
    }
}
