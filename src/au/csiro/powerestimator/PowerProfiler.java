/*
Copyright (C) 2011 The University of Michigan

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

Please send inquiries to powertutor@umich.edu
*/

package au.csiro.powerestimator;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import edu.umich.PowerTutor.service.ICounterService;
import edu.umich.PowerTutor.service.UidInfo;
import edu.umich.PowerTutor.util.BatteryStats;
import edu.umich.PowerTutor.util.Counter;
import edu.umich.PowerTutor.util.SystemInfo;

public class PowerProfiler {
  	

	private ICounterService counterService; 
	private BatteryStats batteryStats;  
	private Context context;
	private static PowerProfiler power = null;

	public PowerProfiler(Context context, ICounterService service) {   
		this.context = context;
		this.counterService = service;
		batteryStats = BatteryStats.getInstance();
	}
	
	public static PowerProfiler getInstance(Context context, ICounterService service){
		if (power == null)
			power = new PowerProfiler(context, service);
		return power;
	}

  
	/*"Current Power"
  	"Weighted average of power consumption over the last " +
              		"five minutes"
	 */
	public String instantPowerItem(int uid) {	
	  double POLY_WEIGHT = 0.10;	  
	  if(counterService != null) try {
		// 	Compute what we're going to call the temporal power usage.
		int count = 0;
		int[] history = counterService.getComponentHistory(5 * 60, -1, uid);
		double weightedAvgPower = 0;
		for(int i = history.length - 1; i >= 0; i--) {
			if(history[i] != 0) {
				count++;
				weightedAvgPower *= 1.0 - POLY_WEIGHT;
				weightedAvgPower += POLY_WEIGHT * history[i] / 1000.0;
			}
        }
        if(count > 0) {
          double charge = batteryStats.getCharge();
          double volt = batteryStats.getVoltage();
          if(charge > 0 && volt > 0) {
            weightedAvgPower /= 1.0 - Math.pow(1.0 - POLY_WEIGHT, count);
            long time = (long)(charge * volt / weightedAvgPower);
            return (String.format("%1$.0f mW\n" +
                        "time: %2$d:%3$02d:%4$02d", weightedAvgPower * 1000.0,
                        time / 60 / 60, time / 60 % 60, time % 60));
          } else {
        	  return(String.format("%1$.0f mW", weightedAvgPower * 1000.0));
          }
        } else {
        	return("No data");
        }
      } catch(RemoteException e) {
    	  return("Error");
      } else {
    	  return("No data");
      }
    }
 
	/*
	 * 
	 * 
		Average Power
		Average power consumption since profiler started
	 */

	public String averagePowerItem(int uid){
    
	if(counterService != null) try {
		// 	Compute what we're going to call the temporal power usage.
		double power = 0;
		long[] means = counterService.getMeans(uid, Counter.WINDOW_TOTAL);
		if(means != null) for(long p : means) {
			power += p / 1000.0;
		}
        
		if(power > 0) {
			double charge = batteryStats.getCharge();
			double volt = batteryStats.getVoltage();
			if(charge > 0 && volt > 0) {
				long time = (long)(charge * volt / power);
				return (String.format("%1$.0f\n" +
						"time: %2$d:%3$02d:%4$02d", power * 1000.0,
						time / 60 / 60, time / 60 % 60, time % 60));				
//				return (String.format("%1$.0f mW\n" +
//						"time: %2$d:%3$02d:%4$02d", power * 1000.0,
//						time / 60 / 60, time / 60 % 60, time % 60));
			} else {
//				return(String.format("%1$.0f mW", power * 1000.0));
				return(String.format("%1$.0f", power * 1000.0));
			}
		} else {
			return("No data");
        	}
		} catch(RemoteException e) {
			return("Error");
		} else {
			return("No data");
		}
	
	}
  
	public String currentPowerItem(UidInfo uidInfo){
	  PackageManager pm = this.context.getPackageManager();
	  SystemInfo sysInfo = SystemInfo.getInstance();
      String name = sysInfo.getUidName(uidInfo.uid, pm);
      
      String prefix;
      if(uidInfo.key > 1e12) {
        prefix = "G";
        uidInfo.key /= 1e12;
      } else if(uidInfo.key > 1e9) {
        prefix = "M";
        uidInfo.key /= 1e9;
      } else if(uidInfo.key > 1e6) {
        prefix = "k";
        uidInfo.key /= 1e6;
      } else if(uidInfo.key > 1e3) {
        prefix = "";
        uidInfo.key /= 1e3;
      } else {
        prefix = "m";
      }
      long secs = (long)Math.round(uidInfo.runtime);
      String formattedPower = String.format("%1$.1f%% [%3$d:%4$02d:%5$02d] %2$s\n" +
          "%6$.1f %7$s%8$s",
          uidInfo.percentage, name, secs / 60 / 60, (secs / 60) % 60,
          secs % 60, uidInfo.key, prefix, uidInfo.unit);
      
      return (int)uidInfo.key + "";
	}
}



