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

import be.nbb.demetra.sdmx.web.SdmxWebProviderBuddy;
import sdmxdl.web.SdmxWebManager;
import sdmxdl.web.SdmxWebSource;
import ec.nbdemetra.ui.completion.JAutoCompletionService;
import ec.nbdemetra.ui.nodes.AbstractNodeBuilder;
import ec.nbdemetra.ui.properties.NodePropertySetBuilder;
import ec.nbdemetra.ui.properties.PropertySheetDialogBuilder;
import ec.util.completion.ext.DesktopFileAutoCompletionSource;
import ec.util.completion.swing.FileListCellRenderer;
import ec.util.completion.swing.JAutoCompletion;
import ec.util.grid.swing.XTable;
import ec.util.various.swing.FontAwesome;
import ec.util.various.swing.StandardSwingColor;
import ec.util.various.swing.TextPrompt;
import ec.util.various.swing.ext.FontAwesomeUtils;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.beans.BeanInfo;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.FileFilter;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.netbeans.api.actions.Editable;
import org.netbeans.swing.outline.DefaultOutlineModel;
import org.openide.explorer.ExplorerManager;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.Sheet;
import org.openide.util.Lookup;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;
import sdmxdl.LanguagePriorityList;

final class DotStatPanel extends javax.swing.JPanel implements ExplorerManager.Provider {

    private final DotStatOptionsPanelController controller;
    private final ExplorerManager em;

    DotStatPanel(DotStatOptionsPanelController controller) {
        this.controller = controller;
        initComponents();

        FileFilter xmlFilter = file -> file.isDirectory() || file.toString().toLowerCase(Locale.ROOT).endsWith(".xml");
        JAutoCompletion customSourcesCompletion = new JAutoCompletion(customSources);
        customSourcesCompletion.setSource(new DesktopFileAutoCompletionSource(xmlFilter, new File[0]));
        customSourcesCompletion.getList().setCellRenderer(new FileListCellRenderer(Executors.newSingleThreadExecutor()));
        selectCustomSources.setText("");
        selectCustomSources.setIcon(FontAwesome.FA_FILE_O.getIcon(selectCustomSources.getForeground(), 16f));
        TextPrompt prompt = new TextPrompt("path to sources file", customSources);
        StandardSwingColor.TEXT_FIELD_INACTIVE_FOREGROUND.lookup().ifPresent(prompt::setForeground);

        addButton.setEnabled(false);
        removeButton.setEnabled(false);
        editButton.setEnabled(false);
        resetButton.setEnabled(false);

        this.em = new ExplorerManager();
        em.addVetoableChangeListener((PropertyChangeEvent evt) -> {
            switch (evt.getPropertyName()) {
                case ExplorerManager.PROP_SELECTED_NODES:
                    onNodeSelectionChange(evt);
                    break;
            }
        });

        outlineView1.getOutline().setRootVisible(false);
        ((DefaultOutlineModel) outlineView1.getOutline().getModel()).setNodesColumnLabel("Source");
        outlineView1.setPropertyColumns("name", "Name", "aliases", "Aliases");
        outlineView1.getOutline().setColumnHidingAllowed(false);
        XTable.setWidthAsPercentages(outlineView1.getOutline(), .2, .6, .2);

        JAutoCompletionService.forPathBind(JAutoCompletionService.LOCALE_PATH, preferedLangTextBox);
    }

    private void onNodeSelectionChange(PropertyChangeEvent evt) {
        Node[] nodes = (Node[]) evt.getNewValue();
//                    removeButton.setEnabled(nodes.length == 1);
        editButton.setEnabled(nodes.length == 1 && nodes[0].getLookup().lookup(Editable.class) != null);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButton1 = new javax.swing.JButton();
        outlineView1 = new org.openide.explorer.view.OutlineView();
        jToolBar1 = new javax.swing.JToolBar();
        addButton = new javax.swing.JButton();
        removeButton = new javax.swing.JButton();
        editButton = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        resetButton = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        preferedLangTextBox = new javax.swing.JTextField();
        displayCodesCheckBox = new javax.swing.JCheckBox();
        jLabel2 = new javax.swing.JLabel();
        customSources = new javax.swing.JTextField();
        selectCustomSources = new javax.swing.JButton();
        curlBackendCheckBox = new javax.swing.JCheckBox();

        org.openide.awt.Mnemonics.setLocalizedText(jButton1, org.openide.util.NbBundle.getMessage(DotStatPanel.class, "DotStatPanel.jButton1.text")); // NOI18N

        jToolBar1.setOrientation(javax.swing.SwingConstants.VERTICAL);
        jToolBar1.setRollover(true);

        addButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ec/nbdemetra/ui/list-add_16x16.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(addButton, org.openide.util.NbBundle.getMessage(DotStatPanel.class, "DotStatPanel.addButton.text")); // NOI18N
        addButton.setFocusable(false);
        addButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        addButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        addButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(addButton);

        removeButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ec/nbdemetra/ui/list-remove_16x16.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(removeButton, org.openide.util.NbBundle.getMessage(DotStatPanel.class, "DotStatPanel.removeButton.text")); // NOI18N
        removeButton.setFocusable(false);
        removeButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        removeButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        removeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(removeButton);

        editButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ec/nbdemetra/ui/preferences-system_16x16.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(editButton, org.openide.util.NbBundle.getMessage(DotStatPanel.class, "DotStatPanel.editButton.text")); // NOI18N
        editButton.setFocusable(false);
        editButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        editButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        editButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(editButton);
        jToolBar1.add(jSeparator1);

        resetButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ec/nbdemetra/ui/edit-clear_16x16.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(resetButton, org.openide.util.NbBundle.getMessage(DotStatPanel.class, "DotStatPanel.resetButton.text")); // NOI18N
        resetButton.setFocusable(false);
        resetButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        resetButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        resetButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(resetButton);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(DotStatPanel.class, "DotStatPanel.jLabel1.text")); // NOI18N

        preferedLangTextBox.setText(org.openide.util.NbBundle.getMessage(DotStatPanel.class, "DotStatPanel.preferedLangTextBox.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(displayCodesCheckBox, org.openide.util.NbBundle.getMessage(DotStatPanel.class, "DotStatPanel.displayCodesCheckBox.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(DotStatPanel.class, "DotStatPanel.jLabel2.text")); // NOI18N

        customSources.setText(org.openide.util.NbBundle.getMessage(DotStatPanel.class, "DotStatPanel.customSources.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(selectCustomSources, org.openide.util.NbBundle.getMessage(DotStatPanel.class, "DotStatPanel.selectCustomSources.text")); // NOI18N
        selectCustomSources.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectCustomSourcesActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(curlBackendCheckBox, org.openide.util.NbBundle.getMessage(DotStatPanel.class, "DotStatPanel.curlBackendCheckBox.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(outlineView1, javax.swing.GroupLayout.DEFAULT_SIZE, 487, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(customSources, javax.swing.GroupLayout.DEFAULT_SIZE, 240, Short.MAX_VALUE)
                    .addComponent(preferedLangTextBox))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(selectCustomSources, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(24, 24, 24)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(curlBackendCheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(displayCodesCheckBox)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(preferedLangTextBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(displayCodesCheckBox))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(curlBackendCheckBox)
                    .addComponent(selectCustomSources, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(customSources, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 157, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(105, Short.MAX_VALUE))
                    .addComponent(outlineView1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void addButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addButtonActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_addButtonActionPerformed

    private void removeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeButtonActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_removeButtonActionPerformed

    private void editButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editButtonActionPerformed
        em.getSelectedNodes()[0].getLookup().lookup(Editable.class).edit();
    }//GEN-LAST:event_editButtonActionPerformed

    private void resetButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetButtonActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_resetButtonActionPerformed

    private void selectCustomSourcesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectCustomSourcesActionPerformed
        JFileChooser fileChooser = new JFileChooser(new File(customSources.getText()));
        fileChooser.setFileFilter(new FileNameExtensionFilter("Xml file", "xml"));
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            customSources.setText(fileChooser.getSelectedFile().toString());
        }
    }//GEN-LAST:event_selectCustomSourcesActionPerformed

    @Override
    public ExplorerManager getExplorerManager() {
        return em;
    }

    private static Optional<SdmxWebProviderBuddy> lookupBuddy() {
        return Optional.ofNullable(Lookup.getDefault().lookup(SdmxWebProviderBuddy.class));
    }

    private void loadSources(SdmxWebManager webManager) {
        AbstractNodeBuilder b = new AbstractNodeBuilder();
        // webManager.getCustomSources().forEach(x -> b.add(new ConfigNode(x, true)));
        webManager.getSources().values().stream().filter(source -> !source.isAlias()).forEach(x -> b.add(new ConfigNode(x, false, webManager.getLanguages())));
        em.setRootContext(b.name("root").build());
    }

    void load() {
        lookupBuddy().ifPresent(buddy -> {
            DotStatProviderBuddy.BuddyConfig bean = DotStatProviderBuddy.BuddyConfig.converter().reverse().convert(buddy.getConfig());
            preferedLangTextBox.setText(bean.getPreferredLanguage());
            displayCodesCheckBox.setSelected(bean.isDisplayCodes());
            curlBackendCheckBox.setSelected(bean.isCurlBackend());
            customSources.setText(bean.getCustomSources().toString());
            loadSources(buddy.getWebManager());
        });
    }

    void store() {
        lookupBuddy().ifPresent(buddy -> {
            DotStatProviderBuddy.BuddyConfig bean = new DotStatProviderBuddy.BuddyConfig();
            bean.setPreferredLanguage(preferedLangTextBox.getText());
            bean.setDisplayCodes(displayCodesCheckBox.isSelected());
            bean.setCurlBackend(curlBackendCheckBox.isSelected());
            bean.setCustomSources(new File(customSources.getText()));
            buddy.setConfig(DotStatProviderBuddy.BuddyConfig.converter().convert(bean));
        });
    }

    boolean valid() {
        return true;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addButton;
    private javax.swing.JCheckBox curlBackendCheckBox;
    private javax.swing.JTextField customSources;
    private javax.swing.JCheckBox displayCodesCheckBox;
    private javax.swing.JButton editButton;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JToolBar jToolBar1;
    private org.openide.explorer.view.OutlineView outlineView1;
    private javax.swing.JTextField preferedLangTextBox;
    private javax.swing.JButton removeButton;
    private javax.swing.JButton resetButton;
    private javax.swing.JButton selectCustomSources;
    // End of variables declaration//GEN-END:variables

    private static final class ConfigNode extends AbstractNode {

        private final LanguagePriorityList langs;
        
        public ConfigNode(SdmxWebSource source, boolean customSource, LanguagePriorityList langs) {
            this(source, customSource, new InstanceContent(), langs);
        }

        private ConfigNode(SdmxWebSource source, boolean customSource, InstanceContent abilities, LanguagePriorityList langs) {
            super(Children.LEAF, new ProxyLookup(Lookups.singleton(source), new AbstractLookup(abilities)));
            this.langs = langs;
            setDisplayName(source.getId());
            setShortDescription(source.toString());

            if (customSource) {
                abilities.add(new EditableImpl());
            }
        }

        @Override
        public Image getIcon(int type) {
            return FontAwesomeUtils.getImage(FontAwesome.FA_LINK, type);
        }

        @Override
        public Image getOpenedIcon(int type) {
            return getIcon(type);
        }

        @Override
        protected Sheet createSheet() {
            SdmxWebSource bean = getLookup().lookup(SdmxWebSource.class);
            Sheet result = new Sheet();
            NodePropertySetBuilder b = new NodePropertySetBuilder();
            b.with(String.class)
                    .select(bean, "getId", null)
                    .display("Id")
                    .add();
            b.with(String.class)
                    .selectConst("name", langs.select(bean.getNames()))
                    .display("Name")
                    .add();
            b.with(String.class)
                    .selectConst("aliases", String.join(", ", bean.getAliases()))
                    .display("Aliases")
                    .add();
            b.with(String.class)
                    .select(bean, "getDriver", null)
                    .display("Driver")
                    .add();
            b.with(String.class)
                    .selectConst("endpoint", bean.getEndpoint().toString())
                    .display("Endpoint")
                    .add();
            result.put(b.build());
            return result;
        }

        @Override
        public Action getPreferredAction() {
            return new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Optional.ofNullable(getLookup().lookup(Editable.class)).ifPresent(Editable::edit);
                }
            };
        }

        private final class EditableImpl implements Editable {

            @Override
            public void edit() {
                if (new PropertySheetDialogBuilder().title("Edit web source").icon(getIcon(BeanInfo.ICON_MONO_16x16)).editNode(ConfigNode.this)) {
                    setDisplayName(getLookup().lookup(SdmxWebSource.class).getId());
                }
            }
        }
    }
}
