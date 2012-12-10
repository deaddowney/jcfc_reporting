package coop.jcfoodcoop.reporting;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import coop.jcfoodcoop.reporting.zone7.Zone7Parser;

public class Zone7Dialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JPanel topLabelPanel;
    private JButton findFile;
    private JTextField fileNameBox;
    private JLabel statusLabel;
    private JButton buttonCancel;
    private String lastDirectory;

    DateFormat format = new SimpleDateFormat("MMM-dd-yy");

    public Zone7Dialog() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        statusLabel.setVisible(false);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

// call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

// call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        findFile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                JFileChooser chooser = new JFileChooser(lastDirectory);
                FileNameExtensionFilter filter = new FileNameExtensionFilter(
                        "Word 2007 Files", "docx");
                chooser.setFileFilter(filter);
                int returnVal = chooser.showOpenDialog(Zone7Dialog.this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {

                    final File selectedFile = chooser.getSelectedFile();
                    lastDirectory = selectedFile.getParent();
                    fileNameBox.setText(selectedFile.getAbsolutePath());
                    //Run button is set if both text boxes have content
                    buttonOK.setEnabled(fileNameBox.getText().trim() != null);

                }
            }
        });

    }

    private void onOK() {
        final String inFile = fileNameBox.getText();
        final String outputFile = "zone7-update-"+format.format(new Date())+".csv";
        statusLabel.setVisible(true);
        Executors.newSingleThreadExecutor().execute(
                new Runnable() {

                    public void run() {
                        Zone7Parser z7Parser = new Zone7Parser();
                        try {
                            z7Parser.parse(inFile, outputFile);
                            SwingUtilities.invokeLater(new Runnable() {

                                public void run() {
                                    statusLabel.setVisible(false);

                                    JOptionPane.showMessageDialog(Zone7Dialog.this, "Successfully created output file :" + new File(outputFile).getAbsolutePath());
                                    try {
                                        Desktop.getDesktop().edit(new File(outputFile));
                                    } catch (IOException e) {
                                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                                    }

                                }
                            });
                        } catch (final Exception e) {
                            SwingUtilities.invokeLater(new Runnable() {

                                public void run() {
                                    statusLabel.setVisible(false);
                                    JOptionPane.showMessageDialog(Zone7Dialog.this, "Error created output file :" + e.getMessage());
                                }
                            });

                        }

                    }
                }
        );

    }


    private void onCancel() {
// add your code here if necessary
        dispose();
    }

    public static void main(String[] args) {
        Zone7Dialog dialog = new Zone7Dialog();
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }
}
