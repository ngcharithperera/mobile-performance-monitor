package au.csiro.appmonitor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.os.Environment;

public class ResultHandler {

	private CSVWrapper csvWrapper = null;    

    private String csvFilename;    
    private String columns;
    
    public boolean init(String filename, String header) {
        
    	boolean success = true;
    	this.csvFilename = filename;
        this.columns = header;
        File sdcard = Environment.getExternalStorageDirectory();
        
//		if (!new File(sdcard, filename).isFile()) {			
//			System.out.println("Unable to open output CSV file. Please check if the file is currently open. Stopping operations");
//			success = false;
//		}
        try {
            csvWrapper = new CSVWrapper(new FileWriter(sdcard.getAbsolutePath() + "//CSVData//" + getTimestamp() + "_" + csvFilename));
            csvWrapper.writeNext(columns.split(","));
        } catch (IOException ex) {
        	System.out.println("Unable to open output CSV file. Please check if the file is currently open. Stopping operations");            
        	success = false;        
        }
        return success;
    }

   
    
    /**
     * Writes data as a row in a CSV file.
     * 
     */
	public void handleResults(String[] data) {
		                               
        csvWrapper.writeNext(data);
        
        	
	}
    
	public void tearDown() {
		if (csvWrapper != null)
			csvWrapper.close();
	}
	
	public String getTimestamp(){				
		Long tsLong = System.currentTimeMillis()/1000;
		String timestamp = tsLong.toString();		
		return timestamp;
	}
	
}