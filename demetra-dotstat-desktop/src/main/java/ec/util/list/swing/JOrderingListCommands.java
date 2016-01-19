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
package ec.util.list.swing;

import ec.util.various.swing.JCommand;
import javax.annotation.Nonnull;
import javax.swing.ListSelectionModel;

/**
 *
 * @author Philippe Charles
 */
public final class JOrderingListCommands {

    private JOrderingListCommands() {
        // static class   
    }

    @Nonnull
    public static JCommand<JOrderingList<?>> moveUp() {
        return MoveUp.INSTANCE;
    }

    @Nonnull
    public static JCommand<JOrderingList<?>> moveDown() {
        return MoveDown.INSTANCE;
    }

    //<editor-fold defaultstate="collapsed" desc="Implementation details">
    private static final class MoveUp extends JCommand<JOrderingList<?>> {

        private static final MoveUp INSTANCE = new MoveUp();

        @Override
        public void execute(JOrderingList<?> c) {
            ListSelectionModel selectionModel = c.getSelectionModel();
            int index = selectionModel.getMinSelectionIndex();
            c.getModel().move(new int[]{index}, index - 1);
            selectionModel.setSelectionInterval(index - 1, index - 1);
            c.ensureIndexIsVisible(index - 1);
        }

        @Override
        public boolean isEnabled(JOrderingList<?> c) {
            if (!c.isEnabled()) {
                return false;
            }
            ListSelectionModel selectionModel = c.getSelectionModel();
            if (selectionModel.isSelectionEmpty()) {
                return false;
            }
            int min = selectionModel.getMinSelectionIndex();
            int max = selectionModel.getMaxSelectionIndex();
            return min == max && min > 0;
        }

        @Override
        public JCommand.ActionAdapter toAction(JOrderingList<?> c) {
            return super.toAction(c)
                    .withWeakListSelectionListener(c.getSelectionModel())
                    .withWeakPropertyChangeListener(c);
        }
    }

    private static final class MoveDown extends JCommand<JOrderingList<?>> {

        private static final MoveDown INSTANCE = new MoveDown();

        @Override
        public void execute(JOrderingList<?> c) {
            ListSelectionModel selectionModel = c.getSelectionModel();
            int index = selectionModel.getMinSelectionIndex();
            c.getModel().move(new int[]{index}, index + 1);
            selectionModel.setSelectionInterval(index + 1, index + 1);
            c.ensureIndexIsVisible(index + 1);
        }

        @Override
        public boolean isEnabled(JOrderingList<?> c) {
            if (!c.isEnabled()) {
                return false;
            }
            ListSelectionModel selectionModel = c.getSelectionModel();
            if (selectionModel.isSelectionEmpty()) {
                return false;
            }
            int min = selectionModel.getMinSelectionIndex();
            int max = selectionModel.getMaxSelectionIndex();
            return min == max && max < c.getModel().getSize() - 1;
        }

        @Override
        public JCommand.ActionAdapter toAction(JOrderingList<?> c) {
            return super.toAction(c)
                    .withWeakListSelectionListener(c.getSelectionModel())
                    .withWeakPropertyChangeListener(c);
        }
    }
    //</editor-fold>
}
