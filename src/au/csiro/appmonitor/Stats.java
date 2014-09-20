package au.csiro.appmonitor;

import android.os.Parcel;
import android.os.Parcelable;

public class Stats implements Parcelable {

	private double cpuUsage;
	private double memoryUsage;
	private double currentPower;
	private double averagePower;
	
	
	public Stats(){
		this.cpuUsage = 0;
		this.memoryUsage = 0;
		this.currentPower = 0;
		this.averagePower = 0;
	}
	
	public Stats(double cpuUsage, double memoryUsage, double currentPower,
			double averagePower) {
		super();
		this.cpuUsage = cpuUsage;
		this.memoryUsage = memoryUsage;
		this.currentPower = currentPower;
		this.averagePower = averagePower;
	}
	
	public Stats(Parcel in){
		double[] data = new double[4];
		in.readDoubleArray(data);
		this.cpuUsage = data[0];
		this.memoryUsage = data[1];
		this.currentPower = data[2];
	}


	public double getCpuUsage() {
		return cpuUsage;
	}


	public void setCpuUsage(double cpuUsage) {
		this.cpuUsage = cpuUsage;
	}


	public double getMemoryUsage() {
		return memoryUsage;
	}


	public void setMemoryUsage(double memoryUsage) {
		this.memoryUsage = memoryUsage;
	}


	public double getCurrentPower() {
		return currentPower;
	}


	public void setCurrentPower(double currentPower) {
		this.currentPower = currentPower;
	}


	public double getAveragePower() {
		return averagePower;
	}


	public void setAveragePower(double averagePower) {
		this.averagePower = averagePower;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeDoubleArray(new double[] {this.cpuUsage,
                this.memoryUsage,
                this.currentPower,
                this.averagePower});
		
	}
	
	public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public Stats createFromParcel(Parcel in) {
            return new Stats(in); 
        }

        public Stats[] newArray(int size) {
            return new Stats[size];
        }
    };
	
	
	
}
