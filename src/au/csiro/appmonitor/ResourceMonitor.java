package au.csiro.appmonitor;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.os.Debug.MemoryInfo;
import android.util.Log;

public class ResourceMonitor {
	
	public static ResourceMonitor instance = null;
	private Context context = null;
	
	public ResourceMonitor(Context context){
		//ToDo
		this.context = context;
	}
	
	public static ResourceMonitor getInstance(Context context){
		if (instance == null)
			instance = new ResourceMonitor(context);		
		return instance;
	}
	
	
	//we compute the CPU usage - using v1				-- currently not used as not accurate		
	//	_cpu = getCPUUsageV1(processID);
	//	_cpu = replaceMultipleSpaces(_cpu);				
	//	temp = _cpu.split(" ");
	//	data[0] = Integer.parseInt(temp[2].replace("%", ""));

	@Deprecated
	private String getCPUUsageV1(int pid) {
		
		Process p = null;
	    BufferedReader in = null;
	    String returnString = null;
	    try {
	        p = Runtime.getRuntime().exec("top -n 1");
	        in = new BufferedReader(new InputStreamReader(p.getInputStream()));
	        boolean found = false;
	        
	        while (!found) {
	            returnString = in.readLine();
	            if (!returnString.equals("") && returnString != null){
	            	returnString = trimLeft(returnString);
		            String tempString[] = returnString.split(" ");		            
		            String temp_pid = tempString[0].trim();
		            if (temp_pid.equals(pid+"")){
		            	found = true;
		            	break;
		            }
	            }
	        }
	    } catch (IOException e) {
	        Log.e("executeTop", "error in getting first line of top");
	        e.printStackTrace();
	    } finally {
	        try {
	            in.close();
	            //p.destroy();
	        } catch (IOException e) {
	            Log.e("executeTop",
	                    "error in closing and destroying top process");
	            e.printStackTrace();
	        }
	    }
	    return returnString;
	}
	
	
//	//memory usage - using v1 -- not working on real phone. Not used currently
//	_mem = getMemoryUsageV1(processID);
//	_mem = replaceMultipleSpaces(_mem);				
//	temp = _mem.split(" ");
//	data[2] = Integer.parseInt(temp[3].replace("K", "")) + Integer.parseInt(temp[4].replace("K", ""));
	
	@Deprecated
	private String getMemoryUsageV1(int pid) {
		
		Process p = null;
	    BufferedReader in = null;
	    String returnString = null;
	    try {
	        p = Runtime.getRuntime().exec("procrank");
	        in = new BufferedReader(new InputStreamReader(p.getInputStream()));
	        boolean found = false;
	        
	        while (!found) {
	            returnString = in.readLine();	            
	            if (!returnString.equals("") && returnString != null){
	            	returnString = trimLeft(returnString);
		            String tempString[] = returnString.split(" ");		            		            
		            if (tempString[0].trim().equals(pid+"")){
		            	found = true;
		            	break;
		            }
	            }
	        }
	    } catch (IOException e) {
	        Log.e("execute procrank", "error in getting first line of procrank command");
	        e.printStackTrace();
	    } finally {
	        try {
	            in.close();
	            p.destroy();
	        } catch (IOException e) {
	            Log.e("execute procrank",
	                    "error in closing and destroying procrank process");
	            e.printStackTrace();
	        }
	    }
	    return returnString;
	}



	@Deprecated
	private String getMemoryUsageV2(int pid) {
		
		Process p = null;
	    BufferedReader in = null;
	    String returnString = null;
	    try {
	        p = Runtime.getRuntime().exec("dumpsys meminfo " + pid);
	        in = new BufferedReader(new InputStreamReader(p.getInputStream()));
	        boolean found = false;
	        
	        while (!found) {
	            returnString = in.readLine();	            
	            if (!returnString.equals("") && returnString != null){
	            	returnString = trimLeft(returnString);
		            String tempString[] = returnString.split(" ");		            		            
		            if (tempString[0].trim().equals("TOTAL")){
		            	found = true;
		            	break;
		            }
	            }
	        }
	    } catch (IOException e) {
	        Log.e("execute procrank", "error in getting first line of procrank command");
	        e.printStackTrace();
	    } finally {
	        try {
	            in.close();
	            p.destroy();
	        } catch (IOException e) {
	            Log.e("execute procrank",
	                    "error in closing and destroying procrank process");
	            e.printStackTrace();
	        }
	    }
	    return returnString;
	}
	
	//being used in the code
	public int getMemoryUsageV3(int pid) {
		
		ActivityManager manager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
		int processIDS[] = {pid};
		MemoryInfo[] memoryInfo = manager.getProcessMemoryInfo(processIDS);			
		
		int totalMem =0;
		
		
		for (MemoryInfo memory: memoryInfo){		
						
			totalMem = memory.getTotalPss() + memory.getTotalPrivateDirty(); 
			//tempString.append("Total Memory Used->: SharedDirty: -> " +memory.getTotalSharedDirty() + "\n");
		}	
		return totalMem;
	}
	
	public static String trimLeft(String s) {
	    return s.replaceAll("^\\s+", "");
	}
	 
	public static String trimRight(String s) {
	    return s.replaceAll("\\s+$", "");
	}
	
	public static String replaceMultipleSpaces(String s){
		return s.replaceAll(" +", " ");
	}
	
	//being used in the code
	
	//Version 2
	public int getCPUUsageV2(int pid) throws FileNotFoundException, IOException{
		
		int cpuUsage = -1;
		String line[];
		long time_total_before, utime_before, stime_before;		
		long time_total_after, utime_after, stime_after;	
		long user_util, sys_util;
		
		
		//http://stackoverflow.com/questions/1420426/calculating-cpu-usage-of-a-process-in-linux
		// check above link for more explanations
		
		BufferedReader readStream = new BufferedReader(new FileReader("/proc/stat"));
		line = readStream.readLine().split("[ ]+", 9);         
		time_total_before = Long.parseLong(line[1]) + Long.parseLong(line[2]) + Long.parseLong(line[3]) 
        		+ Long.parseLong(line[4]) + Long.parseLong(line[5]) + Long.parseLong(line[6]) + Long.parseLong(line[7]);
        
        
        readStream = new BufferedReader(new FileReader("/proc/"+pid+"/stat"));
        line = readStream.readLine().split("[ ]+", 18);
        utime_before = Long.parseLong(line[13]);
        stime_before = Long.parseLong(line[14]);
        
        try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        
        readStream = new BufferedReader(new FileReader("/proc/stat"));
		line = readStream.readLine().split("[ ]+", 9);         
		time_total_after = Long.parseLong(line[1]) + Long.parseLong(line[2]) + Long.parseLong(line[3]) 
        		+ Long.parseLong(line[4]) + Long.parseLong(line[5]) + Long.parseLong(line[6]) + Long.parseLong(line[7]);
        
        
        readStream = new BufferedReader(new FileReader("/proc/"+pid+"/stat"));
        line = readStream.readLine().split("[ ]+", 18);
        utime_after = Long.parseLong(line[13]);
        stime_after = Long.parseLong(line[14]);
        
        user_util = 100 * (utime_after - utime_before) / (time_total_after - time_total_before);
        sys_util = 100 * (stime_after - stime_before) / (time_total_after - time_total_before);
        
        cpuUsage = (int) (user_util + sys_util);
        		
		return cpuUsage;
		
	}
	
	
	public int getProcessId(String processName){
		int found = -1;
		ActivityManager manager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningAppProcessInfo> service= manager.getRunningAppProcesses();
		for (RunningAppProcessInfo process: service){
			if (process.processName.equals(processName)){				
				found = process.pid;
				break;
			}			
		}
		return found;
	}
}
