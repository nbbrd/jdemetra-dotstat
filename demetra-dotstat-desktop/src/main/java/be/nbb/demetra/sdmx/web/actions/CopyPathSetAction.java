package be.nbb.demetra.sdmx.web.actions;

import be.nbb.demetra.sdmx.web.SdmxWebBean;
import be.nbb.demetra.sdmx.web.SdmxWebProvider;
import ec.nbdemetra.ui.tsproviders.CollectionNode;
import ec.nbdemetra.ui.tsproviders.SeriesNode;
import ec.tss.tsproviders.DataSet;
import ec.tss.tsproviders.DataSource;
import ec.ui.ExtAction;
import internal.jd3.AbilityNodeAction3;
import internal.jd3.TsManager3;
import internal.sdmx.*;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.Presenter;
import sdmxdl.*;

import javax.swing.*;
import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ActionID(category = "Edit", id = CopyPathSetAction.ID)
@ActionRegistration(displayName = "#CTL_CopyPathSetAction", lazy = false)
@Messages("CTL_CopyPathSetAction=Copy Path/Reference...")
@ActionReferences({
        @ActionReference(path = CollectionNode.ACTION_PATH, position = 1422, id = @ActionID(category = "Edit", id = CopyPathSetAction.ID)),
        @ActionReference(path = SeriesNode.ACTION_PATH, position = 1422, id = @ActionID(category = "Edit", id = CopyPathSetAction.ID))
})
public final class CopyPathSetAction extends AbilityNodeAction3<DataSet> implements Presenter.Popup {

    static final String ID = "be.nbb.demetra.sdmx.web.actions.CopyPathSetAction";

    public CopyPathSetAction() {
        super(DataSet.class, true);
    }

    @Override
    public JMenuItem getPopupPresenter() {
        return ExtAction.hideWhenDisabled(new JMenuItem(this));
    }

    @Override
    protected void performAction(Stream<DataSet> items) {
        DataSet item = single(items).orElseThrow(NoSuchElementException::new);
        SdmxWebProvider provider = providerOf(item.getDataSource()).orElseThrow(NoSuchElementException::new);
        SdmxWebBean bean = provider.decodeBean(item.getDataSource());
        DatabaseRef databaseRef = SdmxBeans.getDatabase(bean);
        FlowRef flowRef = FlowRef.parse(bean.getFlow());
        Key key = getKey(provider, bean.getSource(), databaseRef, flowRef, item);
        KeyRequest keyRequest = KeyRequest
                .builder()
                .languages(provider.getLanguages())
                .database(databaseRef)
                .flow(flowRef)
                .key(key)
                .build();
        new OnDemandMenuBuilder()
                .copyToClipboard("SDMX-DL URI", SdmxURI.fromKeyRequest(bean.getSource(), keyRequest).toString())
                .copyToClipboard("Source", bean.getSource())
                .copyToClipboard("Flow", flowRef.toShortString())
                .copyToClipboard("Key", key.toString())
                .addSeparator()
                .copyToClipboard("Fetch data command", SdmxCommand.fetchData(bean.getSource(), keyRequest))
                .copyToClipboard("Fetch meta command", SdmxCommand.fetchMeta(bean.getSource(), keyRequest))
                .copyToClipboard("Fetch keys command", SdmxCommand.fetchKeys(bean.getSource(), keyRequest))
                .showMenuAsPopup(null);
    }

    @Override
    protected boolean enable(Stream<DataSet> items) {
        Optional<DataSet> item = single(items);
        return item.isPresent() && providerOf(item.get().getDataSource()).isPresent();
    }

    @Override
    public String getName() {
        return Bundle.CTL_CopyPathSetAction();
    }

    private static Optional<SdmxWebProvider> providerOf(DataSource dataSource) {
        return TsManager3.get().getProvider(SdmxWebProvider.class, dataSource);
    }

    private static <T> Optional<T> single(Stream<T> items) {
        List<T> list = items.collect(Collectors.toList());
        return list.size() == 1 ? Optional.of(list.get(0)) : Optional.empty();
    }

    private static Key getKey(SdmxWebProvider provider, String source, DatabaseRef databaseRef, FlowRef flowRef, DataSet dataSet) {
        try {
            Structure structure = provider
                    .getSdmxManager()
                    .usingName(source)
                    .getMeta(FlowRequest
                            .builder()
                            .languages(provider.getLanguages())
                            .database(databaseRef)
                            .flow(flowRef)
                            .build())
                    .getStructure();
            Key.Builder result = Key.builder(structure);
            structure.getDimensions().forEach(dimension -> result.put(dimension.getId(), dataSet.get(dimension.getId())));
            return result.build();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
