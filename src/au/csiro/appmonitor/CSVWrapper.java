/**
 * 
 */
package au.csiro.appmonitor;

import au.com.bytecode.opencsv.CSVWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Wraps around au.com.bytecode.opencsv.CSVWriter with the aim of limiting dependency
 * on the package to this class.
 * 
 * @author kutila
 */
public class CSVWrapper {
    
    private CSVWriter writer;

    public CSVWrapper(FileWriter filewriter) {
        writer = new CSVWriter(filewriter, ',', CSVWriter.NO_QUOTE_CHARACTER);
    }
    
    public void writeNext(String[] row) {
        writer.writeNext(row);
        try {
            writer.flush();
        } catch (IOException ex) {
            System.err.println("Failed Flush: " + ex.toString());
        }
    }
    
    public void writeAll(ResultSet rs) throws SQLException, IOException {
        writer.writeAll(rs, false);
        writer.flush();
    }
    
    public void close() {
        try {
            writer.close();
        } catch (IOException ex) {
            System.err.println("Failed close: " + ex.toString());
        }
    }
}
