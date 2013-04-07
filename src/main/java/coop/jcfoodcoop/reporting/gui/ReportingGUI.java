package coop.jcfoodcoop.reporting.gui;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;

import coop.jcfoodcoop.reporting.invoicing.SupplierForm;
import coop.jcfoodcoop.reporting.productupdate.gui.ParseLancasterScreen;

/**
 * @author akrieg
 */
public class ReportingGUI {
    private JTabbedPane tabbedPane;

    public ReportingGUI() {
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Customer Invoices", new SupplierForm().getMainPane());
        tabbedPane.addTab("Lancaster Update", new ParseLancasterScreen().getMainPane());
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("JCFC Data Tools");
        frame.setContentPane(new ReportingGUI().tabbedPane);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setVisible(true);
    }
}
