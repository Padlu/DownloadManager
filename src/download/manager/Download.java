/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package download.manager;

import com.notification.NotificationFactory;
import com.notification.NotificationManager;
import com.notification.manager.SimpleManager;
import com.notification.types.TextNotification;
import com.theme.ThemePackagePresets;
import com.utils.Time;

import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;
import java.sql.*;
import java.util.Date;
import java.util.concurrent.*;

/**
 *
 * @author dell
 */

//This class downloads a file from a URL.
public class Download extends Observable implements Runnable, Serializable
{
    //Max size of download buffer
    private static final int MAX_BUFFER_SIZE = 1024;
    //These are the status names
    public static final String []STATUSES = {"Downloading", "Paused", "Complete", "Cancelled", "Error"};
    //These are the status codes
    public static final int DOWNLOADING = 0;
    public static final int PAUSED = 1;
    public static final int COMPLETE = 2;
    public static final int CANCELLED = 3;
    public static final int ERROR = 4;
    
    private URL url; // download url
    private int size; // size of download in bytes
    private int downloaded; // number of bytes downloaded
    private int status; // status of download
    private long downloadSTime;//When download was added
    private long startTime; //Measure when you start to download the file
    private long endTime;// time for updation of the speed
    private long downloadETime;
//    protected Timer t = new Timer();
    private String remainTime;
    private String directory;
    private String directoryPath;
    public  String priority = "Normal";

    DownloadsTableModel tableModel;
    
    // Constructor1 for Download
    public Download(URL url,String path)
    {
        this.url = url;
        size = -1;
        downloaded = 0;
        status = DOWNLOADING;
        downloadSTime = System.currentTimeMillis();
        startTime = System.nanoTime();
        directory = path;
        directoryPath = path + "/" + getFileName(url);

        // begin the download
        download();
    }

    // Constructor2 for Download
    public Download(String url,String path, String status)
    {
        try {
            this.url = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        this.status = statusChanged(status);
        directoryPath = path;
    }
    
    //Get this download's url
    public String getURL()
    {
        return url.toString();
    }

    //Set this download's url
    public void setURL(String thisurl){
        try {
            this.url = new URL(thisurl);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
    
    //Get this download's size
    public int getSize()
    {
        return size;
    }

    //Set this download's size
    public void setSize(String thissize){
        this.size = Integer.parseInt(thissize);
    }

    //Get this download's downloaded size
    public int getDownloaded() { return downloaded;}

    //Set this download's downloaded size
    public void setDownloadedSize(String thisdownloaded){
        this.downloaded = Integer.parseInt(thisdownloaded);
    }
    
    //Get this download's progress
    public float getProgress()
    {
        return ( (float) downloaded / size) * 100;
    }
    
    //Get this download's status
    public int getStatus()
    {
        return status;
    }

    //Set this download's status
    public void setStatus(String thisstatus){
        this.status = statusChanged(thisstatus);
    }

    //Get this download's status
    public String getPriority()
    {
        return priority;
    }

    //Set this download's status
    public void setPriority(String thisprior){
        this.priority = thisprior;
    }

    //Get this download's transfer speed
    public String getSpeed() {
        float dspeed = (float)downloaded/((endTime - startTime)/1000000000);
        if(dspeed < 1024){
            return Math.round(dspeed * 100.0)/100.0+" Bps";
        }
        else if(dspeed < 1048576){
            return Math.round(dspeed/1000 * 100.0)/100.0 +" KBps";
        }
        else {
            return Math.round(dspeed/1000000 * 100.0)/100.0 +" MBps";
        }
    }

    //Get this download's time left
    public String getTimeLeft(){
        int rt = (size - downloaded) * (int)((float)(endTime - startTime)/(downloaded * 1000000000));
        Date date = new Date(rt);
        DateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        remainTime = formatter.format(date);
        return remainTime;
    }

/*    //Get this download's time left
    public String getTimeLeft(){
        //Timertask
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                long rt = (size - downloaded) * (int)((float)(endTime - startTime)/(downloaded * 1000000000));
                Date date = new Date(rt);
                DateFormat formatter = new SimpleDateFormat("HH:mm:ss");
                remainTime = formatter.format(date);
            }
        };
        //Timer delay for timeleft
        timer.schedule(timerTask , 0 , 2000);
        return remainTime;
    }

*/

    //Get Elapsed time of the download
    public String getTimeElapsed(){
        long millis = downloadETime - downloadSTime;
        String elapsedTime = String.format("%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
        return elapsedTime;
    }

    //Get Date when download was added
    public String getDateAdded(){
        Date date = new Date(downloadSTime);
        DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        String dateFormatted = formatter.format(date);
        return dateFormatted;
    }

    //Get Date when download is complete
    public String getDateComplete(){
        Date date = new Date(downloadETime);
        DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        String dateFormatted = formatter.format(date);
        return dateFormatted;
    }

    //Get Real Date when download was added
    public String getRealDateAdded(){ return String.valueOf(downloadSTime); }

    //Set Real Date when download was added
    public void setRealDateAdded(String thisdateadded){ this.downloadSTime = Long.parseLong(thisdateadded); }

    //Get Real Date when download is complete
    public String getRealDateComplete(){ return String.valueOf(downloadETime); }

    //Set Real Date when download was ended
    public void setRealDateComplete(String thisdateended){ this.downloadETime = Long.parseLong(thisdateended); }

    //Get Folder Directory of the download
    public String getDirectory(){ return directory;}

    //Get Final Directory of the download
    public String getDirectoryPath(){ return directoryPath;}

    //Set Final Directory of the download
    public void setDirectoryPath(String thisdir){ this.directoryPath = thisdir;}

    //Pause this download.
    public void pause()
    {
        status = PAUSED;
        tableModel.databaseupdate(this);
        stateChanged();
    }
    
    //Resume this download.    
    public void resume()
    {
        status = DOWNLOADING;
        tableModel.databaseupdate(this);
        stateChanged();
    }
    
    //Cancel this download.    
    public void cancel()
    {
        status = CANCELLED;
//        tableModel.databaseupdate(this);
        stateChanged();
    }
    
    //Mark this download as having an error.    
    private void error()
    {
        status = ERROR;
//        tableModel.databaseupdate(this);
        stateChanged();
    }
    
    //Start or resume downloading    //changing from private to public
    private void download()
    {
        Thread thread = new Thread(this);
        thread.start();
    }
    
    //Get file name portion of URL.
    public String getFileName(URL url)
    {
        String fileName = url.getFile();
        return fileName.substring(fileName.lastIndexOf('/')+1);
    }
    
    //Download File
    @Override
    public void run()
    {
        File file1 = new File(getFileName(url));    //Main file created first in directory of the manager
        RandomAccessFile file = null;   //file created which actually writes data randomly into Main file
        InputStream stream = null;

        try
        {
            //Open Connection to URL
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            //Specify what portion of file is to download.
            connection.setRequestProperty("Range", "bytes=" + downloaded + "-");
            
            //Connect to server
            connection.connect();
            
            //Make sure the response code is in the 200 range(since range of 200 means request was successful)
            if(connection.getResponseCode()/100 != 2)
            {
                error();
            }
            
            //Check for valid content length
            int contentLength = connection.getContentLength();
            if(contentLength < 1)
            {
                error();
            }

            
            //Set the size for this download if it hasn't been set.
            if(size == -1)
            {
                size = contentLength;
                stateChanged();
            }


            
            //Open file and seek to the end of it.
            file = new RandomAccessFile(file1, "rw");

            //Setting File Directory
            file1.renameTo(new File(directoryPath));

            file.seek(downloaded);
            
            stream = connection.getInputStream();
            while(downloaded < size)
            {

                while (status == PAUSED){
                    Thread.sleep(500);
                }
                if (status == DOWNLOADING) {
                    //Size buffer according to how much of the file is left to download.
                    byte[] buffer;
                    if (size - downloaded > MAX_BUFFER_SIZE) {
                        buffer = new byte[MAX_BUFFER_SIZE];
                    } else {
                        buffer = new byte[size - downloaded];
                    }

                    //Read from server into buffer.
                    int read = stream.read(buffer);
                    if (read == -1) {
                        break;
                    }

                    //Write buffer to file.
                    file.write(buffer, 0, read);
                    downloaded += read;
                    endTime = System.nanoTime(); //Measure when we're done downloading the file
                    downloadETime = System.currentTimeMillis(); // Sets the current intermediate time for calc elapsed time
                }
                if (status == CANCELLED)
                    break;
                stateChanged();

                if(priority == "Normal"){
                    Thread.sleep(50);
                }
                if(priority == "Low"){
                    Thread.sleep(100);
                }
            }
            
            //Change status to complete if this point was reached because downloading has finished.
            if(status == DOWNLOADING)
            {
                status = COMPLETE;
                this.endTime = System.nanoTime(); //Measure when we're done downloading the file
                this.downloadETime = System.currentTimeMillis(); // Sets the current intermediate time for calc elapsed time
//                tableModel.databaseupdate(this);
                stateChanged();

                NotificationFactory factory = new NotificationFactory(ThemePackagePresets.cleanDark());
                NotificationManager plain = new SimpleManager(NotificationFactory.Location.NORTHEAST);
                TextNotification notification = factory.buildTextNotification("Download Complete", getFileName(url));
                notification.setCloseOnClick(true);
                plain.addNotification(notification, Time.seconds(4));
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
            error();
        }
        finally
        {
            //Close File.
            if(file != null)
            {
                try
                {
                    file.close();
                }
                catch(Exception e) {}
            }
            
            //Close connection to server.
            if(stream != null)
            {
                try
                {
                    stream.close();
                }
                catch(Exception e) {}
            }
        }
    }
    
    //Notify observers that this download's status has changed.
    private void stateChanged()
    {
        setChanged();
        notifyObservers();
    }

    //Setting the status from db
    private int statusChanged(String stat){
        System.out.println(stat + " inside status changed");
        switch (stat){
            case "Downloading" : return DOWNLOADING;
            case "Paused" : return PAUSED;
            case "Complete" : return COMPLETE;
            case "Cancelled" : return CANCELLED;
            case "Error" : return ERROR;
        }
        return 0;
    }
}
