/*
 * Copyright (c) 2019-2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.control;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.log.LogEntry;
import mekhq.campaign.personnel.Person;
import mekhq.gui.dialog.AddOrEditScenarioEntryDialog;
import mekhq.gui.model.LogTableModel;
import mekhq.gui.utilities.JScrollPaneWithSpeed;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.ResourceBundle;

public class EditScenarioLogControl extends JPanel {
    private JFrame parent;
    private Campaign campaign;
    private Person person;
    private LogTableModel logModel;

    private JButton btnAdd;
    private JButton btnEdit;
    private JButton btnDelete;
    private JTable logsTable;
    private JScrollPane scrollLogsTable;

    public EditScenarioLogControl(JFrame parent, Campaign campaign, Person person) {
        this.parent = parent;
        this.campaign = campaign;
        this.person = person;

        this.logModel = new LogTableModel(person.getScenarioLog());

        initComponents();
    }

    private void initComponents() {
        final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.EditScenarioLogControl",
                MekHQ.getMHQOptions().getLocale());

        setName(resourceMap.getString("control.name"));
        this.setLayout(new BorderLayout());

        JPanel panBtns = new JPanel(new GridLayout(1, 0));

        btnAdd = new JButton();
        btnAdd.setText(resourceMap.getString("btnAdd.text"));
        btnAdd.setName("btnAdd");
        btnAdd.addActionListener(evt -> addEntry());
        panBtns.add(btnAdd);

        btnEdit = new JButton();
        btnEdit.setText(resourceMap.getString("btnEdit.text"));
        btnEdit.setName("btnEdit");
        btnEdit.setEnabled(false);
        btnEdit.addActionListener(evt -> editEntry());
        panBtns.add(btnEdit);

        btnDelete = new JButton();
        btnDelete.setText(resourceMap.getString("btnDelete.text"));
        btnDelete.setName("btnDelete");
        btnDelete.setEnabled(false);
        btnDelete.addActionListener(evt -> deleteEntry());
        panBtns.add(btnDelete);
        this.add(panBtns, BorderLayout.PAGE_START);

        logsTable = new JTable(logModel);
        logsTable.setName(resourceMap.getString("logsTable.name"));
        TableColumn column;
        for (int i = 0; i < LogTableModel.N_COL; i++) {
            column = logsTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(logModel.getColumnWidth(i));
            column.setCellRenderer(logModel.getRenderer());
        }
        logsTable.setIntercellSpacing(new Dimension(0, 0));
        logsTable.setShowGrid(false);
        logsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        logsTable.getSelectionModel().addListSelectionListener(this::logTableValueChanged);

        scrollLogsTable = new JScrollPaneWithSpeed();
        scrollLogsTable.setName(resourceMap.getString("scrollLogsTable.name"));
        scrollLogsTable.setViewportView(logsTable);
        this.add(scrollLogsTable, BorderLayout.CENTER);
    }

    private void logTableValueChanged(ListSelectionEvent evt) {
        int row = logsTable.getSelectedRow();
        btnDelete.setEnabled(row != -1);
        btnEdit.setEnabled(row != -1);
    }

    private void addEntry() {
        AddOrEditScenarioEntryDialog dialog = new AddOrEditScenarioEntryDialog(parent, true, campaign.getLocalDate());
        dialog.setVisible(true);
        if (dialog.getEntry().isPresent()) {
            person.addScenarioLogEntry(dialog.getEntry().get());
        }
        refreshTable();
    }

    private void editEntry() {
        LogEntry entry = logModel.getEntry(logsTable.getSelectedRow());
        if (null != entry) {
            AddOrEditScenarioEntryDialog dialog = new AddOrEditScenarioEntryDialog(parent, true, entry);
            dialog.setVisible(true);
            refreshTable();
        }
    }

    private void deleteEntry() {
        person.getScenarioLog().remove(logsTable.getSelectedRow());
        refreshTable();
    }

    private void refreshTable() {
        int selectedRow = logsTable.getSelectedRow();
        logModel.setData(person.getScenarioLog());
        if (selectedRow != -1) {
            if (logsTable.getRowCount() > 0) {
                if (logsTable.getRowCount() == selectedRow) {
                    logsTable.setRowSelectionInterval(selectedRow - 1, selectedRow - 1);
                } else {
                    logsTable.setRowSelectionInterval(selectedRow, selectedRow);
                }
            }
        }
    }
}
