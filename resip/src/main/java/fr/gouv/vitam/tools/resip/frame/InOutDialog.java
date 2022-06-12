/**
 * Copyright French Prime minister Office/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@programmevitam.fr
 * <p>
 * This software is developed as a validation helper tool, for constructing Submission Information Packages (archives
 * sets) in the Vitam program whose purpose is to implement a digital archiving back-office system managing high
 * volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA archiveTransfer the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.tools.resip.frame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;

/**
 * The class InOutDialog.
 * <p>
 * Class for following logs during import and export threads.
 */
public class InOutDialog extends JDialog {

    /**
     * The actions components.
     */
    public JTextArea extProgressTextArea;
    /**
     * The Ok button.
     */
    public JButton okButton;
    /**
     * The Cancel button.
     */
    public JButton cancelButton;

    /**
     * The data.
     */
    private transient SwingWorker<?, ?> thread;

    // Dialog test context

    /**
     * The entry point of dialog test.
     *
     * @param args the input arguments
     * @throws ClassNotFoundException          the class not found exception
     * @throws UnsupportedLookAndFeelException the unsupported look and feel exception
     * @throws InstantiationException          the instantiation exception
     * @throws IllegalAccessException          the illegal access exception
     * @throws NoSuchMethodException           the no such method exception
     * @throws InvocationTargetException       the invocation target exception
     */
    public static void main(String[] args) throws ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        TestDialogWindow window = new TestDialogWindow(InOutDialog.class);//NOSONAR used for debug run
    }

    /**
     * Instantiates a new InOutDialog for test.
     *
     * @param owner the owner
     */
    public InOutDialog(JFrame owner) {
        this(owner, "Test InOut");
    }

    /**
     * Create the dialog.
     *
     * @param owner the owner
     * @param title the title
     */
    public InOutDialog(JFrame owner, String title) {
        super(owner, title, false);
        GridBagConstraints gbc;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        setMinimumSize(new Dimension(300, 200));
        setPreferredSize(new Dimension(600, 300));

        Container contentPane = getContentPane();
        contentPane.setLayout(new GridBagLayout());

        GridBagLayout gridBagLayout = new GridBagLayout();
        gridBagLayout.columnWeights = new double[]{1.0,1.0};
        gridBagLayout.rowWeights = new double[]{0.0, 1.0, 0.0};
        contentPane.setLayout(gridBagLayout);

        JLabel lblNewLabel = new JLabel("Informations de progression");
        lblNewLabel.setFont(MainWindow.BOLD_LABEL_FONT);
        lblNewLabel.setHorizontalAlignment(SwingConstants.LEFT);
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth=2;
        contentPane.add(lblNewLabel, gbc);

        JScrollPane scrollPane = new JScrollPane();
        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 5, 5, 5);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth=2;
        contentPane.add(scrollPane, gbc);
        extProgressTextArea = new JTextArea();
        extProgressTextArea.setFont(MainWindow.LABEL_FONT);
        extProgressTextArea.setWrapStyleWord(true);
        extProgressTextArea.setEditable(false);
        extProgressTextArea.setLineWrap(true);
        scrollPane.setViewportView(extProgressTextArea);

        cancelButton = new JButton("Annuler");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 5, 5, 5);
        contentPane.add(cancelButton, gbc);
        cancelButton.addActionListener(arg -> buttonCancel());

        okButton = new JButton("Fermer");
        okButton.setEnabled(false);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 5, 5, 5);
        contentPane.add(okButton, gbc);
        okButton.addActionListener(arg -> buttonOk());
        getRootPane().setDefaultButton(okButton);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cancelButton.doClick();
                okButton.doClick();
            }
        });

        pack();
        setLocationRelativeTo(owner);
    }

    // actions

    private void buttonCancel() {
        if (thread != null)
            thread.cancel(true);
        else
            setVisible(false);
    }

    private void buttonOk() {
        setVisible(false);
    }

    /**
     * Sets the thread.
     *
     * @param thread the thread
     */
    public void setThread(SwingWorker<?, ?> thread) {
        this.thread = thread;
    }
}
