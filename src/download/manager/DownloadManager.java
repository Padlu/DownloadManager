/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package download.manager;

import java.awt.*;
import java.awt.Desktop;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import org.python.*;

//import java.sql.*;
import java.text.*;


//The Download Manager.
public class DownloadManager extends JFrame implements Observer
{
    //Add download Text Field.
    private JTextField addTextField;
    
    //Download table's data model
    private DownloadsTableModel tableModel;
    
    //Table listing downloads.
    private JTable table;
    
    //These are the buttons for managing the selected download.
    private JButton pauseButton, resumeButton, cancelButton, clearButton;
    
    //Currently selected download.
    private Download selectedDownload;
    
    //Flag for whether or not table selection is being cleared.
    private boolean clearing;

    //Chooser for selecting file path
    private JFileChooser chooser;
    
    //Constructor for DownloadManager.
    public DownloadManager()
    {
        //Set aplication title.
        setTitle("Download Manager");
        
        //Set Window Size
        setSize(1420, 880);

        //Handle Window Starting Events.
        addWindowListener(new WindowAdapter(){
            public void windowOpened(WindowEvent e)
            {
                actionSet();
            }
        });

        //Handle Window Closing Events.
        addWindowListener(new WindowAdapter(){
            public void windowClosing(WindowEvent e)
            {
                actionExit();
            }
        });

        //Make Context
        DownloadManager context = this;
        
        //Set up file menu.
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        JMenuItem fileExitMenuItem = new JMenuItem("Exit", KeyEvent.VK_X);
        fileExitMenuItem.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e)
            {
                actionExit();
            }
        });
        fileMenu.add(fileExitMenuItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
        
        //Set up add panel.
        JPanel addPanel = new JPanel();
        addTextField = new JTextField(30);
        addPanel.add(addTextField);
        JButton addButton = new JButton("Add Download");
        addButton.addActionListener(new ActionListener(){
           @Override
           public void actionPerformed(ActionEvent e)
           {
               if(!addTextField.getText().equals("")) {
                   String path;
                   chooser = new JFileChooser();
                   chooser.setCurrentDirectory(new java.io.File("/Users/abhishekpadalkar/Downloads/"));
                   chooser.setDialogTitle("Choose the directory to Save the file");
                   chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                   // disable the "All files" option.
                   chooser.setAcceptAllFileFilterUsed(false);
                   //
                   if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                       path = chooser.getSelectedFile().getAbsolutePath();
                   } else {
                       path = chooser.getCurrentDirectory().getAbsolutePath();
                   }
                   actionAdd(path);
               }
               else
                   JOptionPane.showMessageDialog(context, "URL cannot be empty!", "ERROR", JOptionPane.ERROR_MESSAGE);

           }
        });
        addPanel.add(addButton);

        
        //Set up Downloads Table.
        tableModel = new DownloadsTableModel();
        table = new JTable(tableModel) {

            public String getToolTipText(MouseEvent e) {
                String tip = null;
                java.awt.Point p = e.getPoint();
                int rowIndex = rowAtPoint(p);
                int colIndex = columnAtPoint(p);

                try {
                    tip = tableModel.getValueAt(rowIndex, colIndex).toString();
                } catch (RuntimeException e1) {
                    //catch null pointer exception if mouse is over an empty line
                    tip = "";
                }

                return tip;
            }
        };

        table.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
            @Override
            public void valueChanged(ListSelectionEvent e)
            {
                tableSelectionChanged();
            }
        });

        JPopupMenu pm = new JPopupMenu();
        pm.add(new CopyAction(table));
        pm.add(new MidAction(table));
        pm.add(new HighAction(table));
        pm.add(new LowAction(table));

        table.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedColumn() == 0) {
                    int selectedRow = table.getSelectedRow();
                    try {
                        Desktop.getDesktop().open(new File((String) tableModel.getDownload(selectedRow).getDirectoryPath()));
                    } catch (IOException e1) {
                        JOptionPane.showMessageDialog(context, "Error Opening File", "ERROR", JOptionPane.ERROR_MESSAGE);
                    }
                }

                if(SwingUtilities.isRightMouseButton(e) && table.getSelectedColumn() == 9)
                {
                    highlightRow(e);
                    doPopup(e);
                }

                if(SwingUtilities.isRightMouseButton(e) && table.getSelectedColumn() == 5)
                {
                    highlightRow(e);
                    doPopup(e);
                }
            }

            protected void doPopup (MouseEvent e) {
                pm.show(e.getComponent(), e.getX(), e.getY());
            }

            protected void highlightRow(MouseEvent e) {
                JTable table = (JTable) e.getSource();
                Point point = e.getPoint();
                int row = table.rowAtPoint(point);
                int col = table.columnAtPoint(point);

                table.setRowSelectionInterval(row, row);
                table.setColumnSelectionInterval(col, col);
            }
        });


        //Allow only one row at a time to be selected.
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        //Set up ProgressBar as renderer for progress column.
        ProgressRenderer renderer = new ProgressRenderer(0, 100);
        renderer.setStringPainted(true); // show progress text
        table.setDefaultRenderer(JProgressBar.class, renderer);
        
        // Set table's row height large enough to fill JProgressBar.
        table.setRowHeight((int) renderer.getPreferredSize().getHeight());
        
        //Set up downloads panel.
        JPanel downloadsPanel = new JPanel();
        downloadsPanel.setBorder(BorderFactory.createTitledBorder("Downloads"));
        downloadsPanel.setLayout(new BorderLayout());
        downloadsPanel.add(new JScrollPane(table),BorderLayout.CENTER);
        
        //Set up buttons panel.
        JPanel buttonsPanel = new JPanel();
        pauseButton = new JButton("Pause");
        pauseButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e)
            {
                actionPause();
            }
        });
        pauseButton.setEnabled(false);
        buttonsPanel.add(pauseButton);
        
        resumeButton = new JButton("Resume");
        resumeButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e)
            {
                actionResume();
            }
        });
        resumeButton.setEnabled(false);
        buttonsPanel.add(resumeButton);
        
        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e)
            {
                actionCancel();
            }
        });cancelButton.setEnabled(false);
        buttonsPanel.add(cancelButton);
        
        clearButton = new JButton("Delete");
        clearButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e)
            {
                actionClear();
            }
        });clearButton.setEnabled(false);
        buttonsPanel.add(clearButton);
        
        //Add panels to display.
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(addPanel, BorderLayout.NORTH);
        getContentPane().add(downloadsPanel, BorderLayout.CENTER);
        getContentPane().add(buttonsPanel, BorderLayout.SOUTH);
    }

    //Sets this program.
    private void actionSet()
    {
        tableModel.downloadList = new ArrayList<>(tableModel.getdownloads());
        System.out.println(tableModel.downloadList);
    }

    //Exit this program.
    private void actionExit()
    {
        System.exit(0);
    }
    
    //Add a new download.
    private void actionAdd(String path)
    {
        URL verifiedUrl = verifyUrl(addTextField.getText());
        if(verifiedUrl != null)
        {
            tableModel.addDownload(new Download(verifiedUrl,path));
            addTextField.setText(""); //reset add text field
        }
        else
        {
            JOptionPane.showMessageDialog(this, "Invalid Download URL", "ERROR", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    //Verify Download URL
    private URL verifyUrl(String url)
    {
        //Only allow HTTP URLs
        if(!url.toLowerCase().startsWith("http://"))
        {
            return null;
        }
        
        //Verify URL Format
        URL verifiedUrl = null;
        try
        {
            verifiedUrl = new URL(url);
        }
        catch(Exception e)
        {
            return null;
        }
        
        //Make sure URL specifies a file
        if(verifiedUrl.getFile().length()<2)
        {
            return null;
        }
        
        return verifiedUrl;
    }

    //Called when table row selection changes.
    private void tableSelectionChanged()
    {
        //Unregister from receiving notifications from the last selected download.
        if(selectedDownload != null)
        {
            selectedDownload.deleteObserver(DownloadManager.this);
        }
        
        //If not in the middle of clearing a download, set the selected download and register to receive notifications from it.
        if(!clearing && table.getSelectedRow() > -1)
        {
            selectedDownload = tableModel.getDownload(table.getSelectedRow());
            selectedDownload.addObserver(DownloadManager.this);
            updateButtons();
        }
    }
    
    //Pause the selected download.
    private void actionPause()
    {
        updateButtons();
        selectedDownload.pause();
    }
    
    //Resume the selected download.
    private void actionResume()
    {
        updateButtons();
        selectedDownload.resume();
    }
        
    //Cancel the selected download.
    private void actionCancel()
    {
        updateButtons();
        selectedDownload.cancel();
    }
    
    //Clear the selected download.
    private void actionClear()
    {
        updateButtons();
        clearing = true;
        tableModel.clearDownload(table.getSelectedRow());
        clearing = false;
        selectedDownload = null;
    }
    
    //Update each button's state based off of the currently selected download's status
    private void updateButtons()
    {
        if(selectedDownload != null)
        {
            int status = selectedDownload.getStatus();
            switch(status)
            {
                case Download.DOWNLOADING:
                    pauseButton.setEnabled(true);
                    resumeButton.setEnabled(false);
                    cancelButton.setEnabled(false);
                    clearButton.setEnabled(false);
                    break;
                    
                case Download.PAUSED:
                    pauseButton.setEnabled(false);
                    resumeButton.setEnabled(true);
                    cancelButton.setEnabled(true);
                    clearButton.setEnabled(false);
                    break;
                    
                case Download.ERROR:
                    pauseButton.setEnabled(false);
                    resumeButton.setEnabled(true);
                    cancelButton.setEnabled(false);
                    clearButton.setEnabled(true);
                    break;
                    
                default: //COMPLETE or CANCELLED
                    pauseButton.setEnabled(false);
                    resumeButton.setEnabled(false);
                    cancelButton.setEnabled(false);
                    clearButton.setEnabled(true);
                    break;
            }
        }
        else
        {
            //No Download is selected in table.
            pauseButton.setEnabled(false);
            resumeButton.setEnabled(false);
            cancelButton.setEnabled(false);
            clearButton.setEnabled(false);
        }
    }

    class CopyAction extends AbstractAction {

        private JTable table;

        public CopyAction(JTable table) {
            this.table = table;
            putValue(NAME, "Open in Folder");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Download download = tableModel.getDownload(table.getSelectedRow());
            try {
                Desktop.getDesktop().open(new File(download.getDirectory()));
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    class HighAction extends AbstractAction {

        private JTable table;

        public HighAction(JTable table) {
            this.table = table;
            putValue(NAME, "High");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Download download = tableModel.getDownload(table.getSelectedRow());
            download.priority = "High";
        }
    }

    class MidAction extends AbstractAction {

        private JTable table;

        public MidAction(JTable table) {
            this.table = table;
            putValue(NAME, "Normal");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Download download = tableModel.getDownload(table.getSelectedRow());
            download.priority = "Normal";
        }
    }

    class LowAction extends AbstractAction {

        private JTable table;

        public LowAction(JTable table) {
            this.table = table;
            putValue(NAME, "Low");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Download download = tableModel.getDownload(table.getSelectedRow());
            download.priority = "Low";
        }
    }

    public static class CellTransferable implements Transferable {

        public static final DataFlavor CELL_DATA_FLAVOR = new DataFlavor(Object.class, "application/x-cell-value");

        private Object cellValue;

        public CellTransferable(Object cellValue) {
            this.cellValue = cellValue;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{CELL_DATA_FLAVOR};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return CELL_DATA_FLAVOR.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            if (!isDataFlavorSupported(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return cellValue;
        }

    }

    //Update is called when a Download notifies its observers of any changes.
    @Override
    public void update(Observable o, Object arg)
    {
        //Update buttons if the selected download has changed.
        if(selectedDownload != null && selectedDownload.equals(o))
            SwingUtilities.invokeLater(new Runnable(){
                @Override
                public void run()
                {
                    updateButtons();
                }
            });
    }
    
    /**
     * Run the Download Manager.
     * @param args the command line arguments.
     */
    public static void main(String[] args) 
    {
        SwingUtilities.invokeLater(new Runnable(){
            @Override
            public void run()
            {
                DownloadManager manager = new DownloadManager();
                manager.setVisible(true);
            }
        });
    }
}
