/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package download.manager;

import org.python.antlr.ast.Str;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import java.sql.*;

/**
 *
 * @author dell
 */

//This class manages the download's table data.
public class DownloadsTableModel extends AbstractTableModel implements Observer, Serializable
{

    //These are the names for the table's columns.
    private static final String[] columnNames = {"File Name", "Size", "Downloaded", "Progress", "Status", "Priority", "Transfer Rate", "Time Left", "Date Added", "URL"};
    
    //These are the classes for each column's values.
    private static final Class[] columnClasses = {String.class, String.class, String.class, JProgressBar.class, String.class, String.class, String.class, String.class, String.class, String.class};

    //The table's list of downloads.
    public ArrayList<Download> downloadList = new ArrayList<Download>(getdownloads());

    private Connection myConn;
    private Statement myStat;
    private String query,S,Sd;

    //Connection with database
    static Connection getConnection(){
        Connection con = null;
        String urlpath = "jdbc:mysql://localhost:3306/manager_database";
        String user = "root";
        String pass = "Abhi@97";
        try {
            con = DriverManager.getConnection(urlpath, user, pass);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return con;
    }
    
    //Add a new download to the table.
    public void addDownload(Download download)
    {
        //Register to be notified when the download changes.
        download.addObserver(this);
        downloadList.add(download);

        //Set up Database connection
        try {
            myConn = getConnection();

            //Statement creaion
            myStat = myConn.createStatement();

            //Execution of statement
            query = "insert into Downloads (d_Name,real_Size,real_DateStarted,ds_DateTime,directory,d_Link) " +
                    "values('" + download.getFileName(new URL(download.getURL())) + "','" + download.getSize() + "','" + download.getRealDateAdded() + "','" + download.getDateAdded() + "','" + download.getDirectoryPath() + "','" + download.getURL() + "')";
            myStat.executeUpdate(query);


        }catch(Exception exprn){
            exprn.printStackTrace();
        }
        //Fire table row insertion notification to table.
        fireTableRowsInserted(getRowCount()-1, getRowCount()-1);
    }
    
    //Get a download for a specified row.
    public Download getDownload(int row)
    {
         return downloadList.get(row);
    }
    
    //Remove a download from the list.
    public void clearDownload(int row)
    {
        Download selectedDownload =  downloadList.get(row);
        query = "DELETE FROM Downloads WHERE real_DateStarted = '" + selectedDownload.getRealDateAdded() + "'";
        try {
            myStat.executeUpdate(query);    //Deletes that file from db
        } catch (SQLException e) {
            e.printStackTrace();
        }

        downloadList.remove(row);
        //Fire table row deletion notification to table.
        fireTableRowsDeleted(row, row);
    }
    
    //Get table's column count.
    @Override
    public int getColumnCount()
    {
        return columnNames.length;
    }
    
    //Get a column's name.
    @Override
    public String getColumnName(int col)
    {
        return columnNames[col];
    }
    
    //Get a column's class.
    @Override
    public Class<?> getColumnClass(int col)
    {
        return columnClasses[col];
    }
    
    //Get table's row count.
    @Override
    public int getRowCount()
    {
        return downloadList.size();
    }

    //Startup retrieval
    public ArrayList<Download> getdownloads() {
        ArrayList<Download> downlist = new ArrayList<Download>();

        Connection thisconn = getConnection();
        Statement thisstat;
        ResultSet resultSet;
        String thisquery, thisurl, thissize, thisdownloaded, thisstatus, thisdir, thisprior, thisdateadded, thisdateended;

        Download thisdownload = null;

        try {
            thisquery = "SELECT * FROM Downloads ORDER BY d_Id";
            thisstat = thisconn.createStatement();
            resultSet = thisstat.executeQuery(thisquery);

            while (resultSet.next()){
                thisurl = resultSet.getString("d_Link");
                thissize = resultSet.getString("real_Size");
                thisdownloaded = resultSet.getString("real_DownloadedSize");
                thisstatus = resultSet.getString("status");
                thisprior = resultSet.getString("priority");
                System.out.println(thisstatus + " in getdownloads()Arraylist");
                thisdir = resultSet.getString("directory");
                thisdateadded = resultSet.getString("real_DateStarted");
                thisdateended = resultSet.getString("real_DateEnded");

                thisdownload = new Download(thisurl, thisdir,thisstatus);
                thisdownload.setSize(thissize);
                thisdownload.setDownloadedSize(thisdownloaded);
                thisdownload.setPriority(thisprior);
                thisdownload.setRealDateAdded(thisdateadded);
                thisdownload.setRealDateComplete(thisdateended);

//                thisdownload.download();

                downlist.add(thisdownload);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return downlist;
    }
    
    //Get value for a specific row and column combination and update it to db
    @Override
    public Object getValueAt(int row, int col)
    {
        Download download = downloadList.get(row);
        switch(col)
        {
            case 0: //File Name
                    try {
                        return download.getFileName(new URL(download.getURL()));
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }

            case 1: //Size
                    double size = download.getSize();
                    if (size < 1024){
                        S = Double.toString(size);
                        S += " B";
                    }
                    else if (size > 1024 && size < 1024000) {
                        size = size / 1000;
                        size = Math.round(size * 100.0)/100.0;
                        S = Double.toString(size);
                        S += " KB";
                    }
                    else if(size > 1024000 && size < 1024000000) {
                        size = size / 1000000;
                        size = Math.round(size * 100.0)/100.0;
                        S = Double.toString(size);
                        S += " MB";
                    }
                    else if(size > 1024000000) {
                        size = size / 1000000000;
                        size = Math.round(size * 100.0)/100.0;
                        S = Double.toString(size);
                        S += " GB";
                    }

                    return(size == -1) ? "" : S;

            case 2: //Downloaded
                    double downloaded = download.getDownloaded();
                    if (downloaded < 1024){
                        Sd = Double.toString(downloaded);
                        Sd += " B";
                    }
                    else if (downloaded > 1024 && downloaded < 1024000) {
                        downloaded = downloaded / 1000;
                        downloaded = Math.round(downloaded * 100.0)/100.0;
                        Sd = Double.toString(downloaded);
                        Sd += " KB";
                    }
                    else if(downloaded > 1024000 && downloaded < 1024000000) {
                        downloaded = downloaded / 1000000;
                        downloaded = Math.round(downloaded * 100.0)/100.0;
                        Sd = Double.toString(downloaded);
                        Sd += " MB";
                    }
                    else if(downloaded > 1024000000) {
                        downloaded = downloaded / 1000000000;
                        downloaded = Math.round(downloaded * 100.0)/100.0;
                        Sd = Double.toString(downloaded);
                        Sd += " GB";
                    }

                    return Sd;

            case 3: //Progress
                    return download.getProgress();

            case 4: //Status
                    return Download.STATUSES[download.getStatus()];

            case 5: //Priority
                    return download.getPriority();

            case 6: //Transfer Rate
                    return download.getSpeed();

            case 7://Time Left
                    return download.getTimeLeft();

            case 8: //Date Added
                    return download.getDateAdded();

            case 9://URL
                    return download.getURL();
        }

        return "";
    }

    //Updation of database
    public void databaseupdate(Download download){
        try{
            query = "UPDATE Downloads set actual_Size = '" + S + "' WHERE real_DateStarted = '" + download.getRealDateAdded() + "'";
            myStat.executeUpdate(query);    //Sets Size of the file

            query = "UPDATE Downloads set real_DownloadedSize = '" + download.getDownloaded() + "' WHERE real_DateStarted = '" + download.getRealDateAdded() + "'";
            myStat.executeUpdate(query);    //Sets Real Downloaded Size of the file

            query = "UPDATE Downloads set downloaded_Size = '" + Sd + "' WHERE real_DateStarted = '" + download.getRealDateAdded() + "'";
            myStat.executeUpdate(query);    //Sets Downloaded Size of the file

            query = "UPDATE Downloads set p_Bar = '" + download.getProgress() + "' WHERE real_DateStarted = '" + download.getRealDateAdded() + "'";
            myStat.executeUpdate(query);    //Sets percentage progress of the download

            if (Download.STATUSES[download.getStatus()] == "Complete"){
                query = "UPDATE Downloads set de_DateTime = '" + download.getDateComplete() + "' WHERE real_DateStarted = '" + download.getRealDateAdded() + "'";
                myStat.executeUpdate(query);    //Sets the date and time of the download

                query = "UPDATE Downloads set real_DateEnded = '" + download.getRealDateComplete() + "' WHERE real_DateStarted = '" + download.getRealDateAdded() + "'";
                myStat.executeUpdate(query);    //Sets the date and time of the download
            }

            query = "UPDATE Downloads set status = '" + Download.STATUSES[download.getStatus()] + "' WHERE real_DateStarted = '" + download.getRealDateAdded() + "'";
            myStat.executeUpdate(query);    //Sets status of the download

            query = "UPDATE Downloads set priority = '" + download.getPriority() + "' WHERE real_DateStarted = '" + download.getRealDateAdded() + "'";
            myStat.executeUpdate(query);    //Sets priority of the download

            query = "UPDATE Downloads SET time_Taken = '" + download.getTimeElapsed() + "' WHERE real_DateStarted = '" + download.getRealDateAdded() + "'";
            myStat.executeUpdate(query);    //Sets Elapsed time of the download

        } catch (SQLException e){ e.printStackTrace();}
    }

        //Update is called when a Download notifies its observers of any changes
    @Override
    public void update(Observable o, Object arg)
    {
        int index = downloadList.indexOf(o);
        
        //Fire table row update notification to table.
        fireTableRowsUpdated(index, index);
    }
}
