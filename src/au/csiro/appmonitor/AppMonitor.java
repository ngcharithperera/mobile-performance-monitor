package au.csiro.appmonitor;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import au.csiro.powerestimator.PowerProfiler;
import edu.umich.PowerTutor.service.ICounterService;
import edu.umich.PowerTutor.service.PowerEstimator;
import edu.umich.PowerTutor.service.UMLoggerService;
import edu.umich.PowerTutor.service.UidInfo;
import edu.umich.PowerTutor.util.Counter;
import edu.umich.PowerTutor.util.SystemInfo;

public class AppMonitor extends Activity implements Runnable{
	
	private static final String MESSAGE = "MESSAGE";
	
	//public static String PROCESSNAMES[] = {"au.csiro.appmonitor", "com.svox.pico"};
	public static String PROCESSNAMES[] = {"au.csiro.gsnlite", "au.csiro.sensmalite.pluginlibrary"};

	//public static final String PROCESSNAME = "au.csiro.appmonitor";	
	private TextView mainScreenDisplay;
	public static Button btn_startstop;
	private LinearLayout chartlayoutCPU;
	private LinearLayout chartlayoutMem;
	private LinearLayout chartlayoutPower;
	
	//for graphing - processor and CPU usage
	
	private GraphicalView mChartCPU;
	private GraphicalView mChartMemory;
	private GraphicalView mChartPower;
	
	
	private XYMultipleSeriesDataset mDatasetCPU;
	private XYMultipleSeriesDataset mDatasetMem;
	private XYMultipleSeriesDataset mDatasetPower;
	private XYMultipleSeriesRenderer mRendererCPU;
	private XYMultipleSeriesRenderer mRendererMem;
	private XYMultipleSeriesRenderer mRendererPower;
	
	private XYSeries[] mMemorySeries;
	private XYSeries[] mCPUSeries;
	private XYSeries[] mPowerSeries;
	
	//making it a nice UI
	private RadioButton csvRadio;
	private RadioButton graphRadio;
	
	private int chartCounter = 0;
	private final int OUTPUTS = 4;
	
	private boolean running = false;
	private int outputmode = -1; //0 - csv mode and 1 for graph mode, -1 error
	
	Handler handler = null;
	ResultHandler csvResults = null;	
	
	private final String CSVFILENAME="AppMonitor.csv";
	
	private ResourceMonitor rm;
	private Context context;	
	
	//for power estimator
	private Intent serviceIntent;
	private CounterServiceConnection conn;
	private ICounterService counterService;
	private SharedPreferences prefs;
	private int noUidMask;
	private String[] componentNames;
	private Handler powerhandler;
	
	private static final double HIDE_UID_THRESHOLD = 0.1;
	public static final int KEY_CURRENT_POWER = 0;
	public static final int KEY_AVERAGE_POWER = 1;
	public static final int KEY_TOTAL_ENERGY = 2;
	private PowerProfiler power;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_app_monitor);
		this.context = this;
		initPowerProfiler(savedInstanceState);
		
		mainScreenDisplay = (TextView) findViewById(R.id.mainScreenDisplay);
		csvRadio = (RadioButton) findViewById(R.id.radio0);
		graphRadio = (RadioButton) findViewById(R.id.radio1);
		chartlayoutCPU = (LinearLayout) findViewById(R.id.chartCPU);	
		chartlayoutMem = (LinearLayout) findViewById(R.id.chartMemory);
		chartlayoutPower = (LinearLayout) findViewById(R.id.chartPower);
        btn_startstop = (Button) findViewById(R.id.button1);
		
		//handler method to manager async UI updates
		handler = new Handler(){
			public void handleMessage(Message msg) {

				//the data bundle is array list of Stats object
				//we unparse it here
				
				ArrayList<Stats> data = msg.getData().getParcelableArrayList(MESSAGE);
				
				//currently we display the message in the textView.
				//int[] data = msg.getData().getIntArray(MESSAGE);
				
				//data[0] is current power
				//data[2] is average power
				if (outputmode == 0){
					//change csv file format when changing the outputs.
					for (int i=0; i < data.size(); i++){
						Stats stats = data.get(i);
						String pName = PROCESSNAMES[i];
						
						mainScreenDisplay.append("CPU v2 ->" + stats.getCpuUsage() + "\n");
						mainScreenDisplay.append("Mem v3 ->" + stats.getMemoryUsage() + "\n");
						mainScreenDisplay.append("Current Power Usage ->" + stats.getCurrentPower() + "\n");
						mainScreenDisplay.append("Average Power Usage ->" + stats.getAveragePower() + "\n");											
						
						String tempData[] = {pName, stats.getCpuUsage()+"", stats.getMemoryUsage()+"", stats.getCurrentPower()+"", stats.getAveragePower()+""};
						csvResults.handleResults(tempData);
					}
				}

				else if(outputmode == 1){			
				//code to update the chart with new values
					for (int i=0; i < data.size(); i++){
						Stats stats = data.get(i);						
						addDataToSeries(mCPUSeries[i], chartCounter, stats.getCpuUsage());
						addDataToSeries(mMemorySeries[i], chartCounter, stats.getMemoryUsage());
						addDataToSeries(mPowerSeries[i], chartCounter, stats.getCurrentPower());		
					}
										
					chartCounter++;
					refreshChart();			
				}
			}
		};
		
	
        btn_startstop.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if (!running){
					if (csvRadio.isChecked())
						outputmode = 0;
					else if(graphRadio.isChecked())
						outputmode = 1;

					if(outputmode != -1){				
						
				        rm = ResourceMonitor.getInstance(v.getContext());				        
				        
						running = true;
						csvRadio.setEnabled(false);
						graphRadio.setEnabled(false);
						btn_startstop.setText("Stop");
						
						// if output mode is graphical. we setup the graph
						if (outputmode == 1){
							if (mChartCPU == null && mChartMemory == null && mChartPower == null) {
								initChart();
							    //to change. the data will be updated dynamically
								//addSampleData();								

								mChartCPU = ChartFactory.getCubeLineChartView(v.getContext(), mDatasetCPU, mRendererCPU, 0.3f);
								mChartMemory = ChartFactory.getCubeLineChartView(v.getContext(), mDatasetMem, mRendererMem, 0.3f);
								mChartPower = ChartFactory.getCubeLineChartView(v.getContext(), mDatasetPower, mRendererPower, 0.3f);
								
								chartlayoutCPU.addView(mChartCPU);
								chartlayoutMem.addView(mChartMemory);
								chartlayoutPower.addView(mChartPower);
								
								//chartlayout.setMinimumHeight(250);
								refreshChart();								
						    }
						}
						else if(outputmode == 0){
							csvResults = new ResultHandler();
							//change this when changing the outputs. - currently we only use v2 to compute CPU and Mem usage
							// we also use power tutor to compute power consumed - current and average
							boolean success = csvResults.init(CSVFILENAME, "Proecss Name, CPU, MEM, CurrentPower, AveragePower");
							if (!success){
								System.out.println("Unable to Open the CSV File");
								System.exit(0);
							}
						}
						 //This is where execution begins
						running = true;
						Integer[] processIDs = new Integer[PROCESSNAMES.length];
						
						for (int i=0; i <PROCESSNAMES.length; i++){
							processIDs[i] = rm.getProcessId(PROCESSNAMES[i]);
						}
						
						//int processID = rm.getProcessId(PROCESSNAME);
						mainScreenDisplay.append("Displaying outputs for " + PROCESSNAMES + " with process ID \t" + processIDs + "\n");
						ComputeResources res = new ComputeResources(handler);
						res.execute(processIDs);							
					}
					else
						msgbox("Please select an output option to start");					
				}
				else{
					//clean up if graph was chosen as output form 
					if (outputmode == 1){
						chartlayoutCPU.removeAllViews();						
						mChartCPU = null;
						chartlayoutMem.removeAllViews();						
						mChartMemory = null;
						chartlayoutPower.removeAllViews();						
						mChartPower = null;
					}
					else if (outputmode == 0){
						if (csvResults!=null)
							csvResults.tearDown();
						csvResults = null;
					}
					running = false;
					outputmode = -1;
					csvRadio.setEnabled(true);
					graphRadio.setEnabled(true);
					btn_startstop.setText("Start");
					mainScreenDisplay.setText("Logs\n");
					
					//clean up profiler
					 if(counterService != null) {
				          stopService(serviceIntent);
					 }					

				}
				
			}
        	
        });

	}
	
	
	private void initPowerProfiler(Bundle instanceState){
	    
	    if(instanceState!= null) {
	      componentNames = instanceState.getStringArray("componentNames");
	      noUidMask = instanceState.getInt("noUidMask");
	    }
	    prefs = PreferenceManager.getDefaultSharedPreferences(this);
		serviceIntent = new Intent(this, UMLoggerService.class);
		conn = new CounterServiceConnection();
	    if(counterService != null) {
	        stopService(serviceIntent);
	    } else {
	    	if(conn == null) {
	    		Toast.makeText(AppMonitor.this, "Power Profiler failed to start",
	                         Toast.LENGTH_SHORT).show();
	        } else {
	        	startService(serviceIntent);	        	
	        }
	    }	    
	    
	}

	@Override
	protected void onResume() {		
		super.onResume();		
		powerhandler = new Handler();
		powerhandler.postDelayed(this, 100);
		getApplicationContext().bindService(serviceIntent, conn, 0);
		
	}

	@Override
	protected void onPause() {
		super.onPause();
		
		getApplicationContext().unbindService(conn);
		powerhandler.removeCallbacks(this);
		powerhandler = null;
		
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putStringArray("componentNames", componentNames);
		outState.putInt("noUidMask", noUidMask);
		
	}	
	
	private void msgbox(String m){
		Toast.makeText(this, m, Toast.LENGTH_LONG);
	}
	
	public int generateColor(){
		java.util.Random rand = new java.util.Random();
		int r = rand.nextInt(255);
		int g = rand.nextInt(255);
		int b = rand.nextInt(255);

		int color = Color.argb(255, r, g, b);   
				
		return color;
	}
	
	private void initChart() {
        		
	    PointStyle style = PointStyle.POINT;
	    
	    
	    	    	    

	    
		//upto a maximum of 3 process can be plotted. More than 3 only csv outputs
        //create a new dataset and add the series to the dataset
	    
	    mRendererCPU = new XYMultipleSeriesRenderer(3);
	    mRendererMem = new XYMultipleSeriesRenderer(3);
	    mRendererPower = new XYMultipleSeriesRenderer(3);
	    
	    mDatasetCPU = new XYMultipleSeriesDataset();
        mDatasetMem = new XYMultipleSeriesDataset();
        mDatasetPower = new XYMultipleSeriesDataset();
	    
	    mCPUSeries = new XYSeries[PROCESSNAMES.length];
	    for (int i=0;i< PROCESSNAMES.length; i++){
	    	//create the render
	    	mRendererCPU.addSeriesRenderer(renderXY(generateColor(), style));
	    	//create the series
	    	mCPUSeries[i] = new XYSeries("CPU Usage", i);
	    	//add the dataset for the series	    	
	    	mDatasetCPU.addSeries(mCPUSeries[i]);
	    }
	    
	    mMemorySeries = new XYSeries[PROCESSNAMES.length];
	    for (int i=0;i< PROCESSNAMES.length; i++){
	    	mRendererMem.addSeriesRenderer(renderXY(generateColor(), style));
	    	mMemorySeries[i] = new XYSeries("Memory Usage (MB)", i);
	    	mDatasetMem.addSeries(mMemorySeries[i]);
	    }

	    mPowerSeries = new XYSeries[PROCESSNAMES.length];
	    for (int i=0;i< PROCESSNAMES.length; i++){
	    	mRendererPower.addSeriesRenderer(renderXY(generateColor(), style));
	    	mPowerSeries[i] = new XYSeries("Power Usage (mW)", i);
	    	mDatasetPower.addSeries(mPowerSeries[i]);
	    }	

	    //design the chart
	    renderChart(mRendererCPU, mRendererCPU.getSeriesRendererAt(0).getColor(), "CPU Usage", "CPU Usage");
	    renderChart(mRendererMem, mRendererMem.getSeriesRendererAt(0).getColor(), "Mem Usage", "Mem Usage");
	    renderChart(mRendererPower, mRendererPower.getSeriesRendererAt(0).getColor(), "Power Usage", "Power Usage");
        
    }
	
	private void renderChart(XYMultipleSeriesRenderer renderer, int colors, String axisTitle, String chartTitle){

	    //common attributed
	    renderer.setAxesColor(Color.LTGRAY);
	    renderer.setLabelsColor(Color.LTGRAY);
	    renderer.setXLabels(12);
	    renderer.setYLabels(10);
	    renderer.setShowGrid(true);	    
	    renderer.setXLabelsAlign(Align.LEFT);	    
	    renderer.setLabelsColor(Color.WHITE);
	    renderer.setXLabelsColor(Color.GREEN);
	    
	    
		renderer.setChartTitle(chartTitle);	    	    
	    renderer.setXAxisMin(0);
	    renderer.setXAxisMax(100);
	    
	    //rendering y axis
	    renderer.setYTitle(axisTitle,0);
	    renderer.setYLabelsColor(0, colors);	    
	    //renderer.setYAxisMin(5000, 0);
	    //renderer.setYAxisMax(25000 , 0);
	    renderer.setYLabelsAlign(Align.LEFT,0);	    
	    

	    	    	   	    
	}
	
	private void refreshChart(){
		if(mChartCPU!=null)
			 mChartCPU.repaint();
		if(mChartMemory!=null)
			mChartMemory.repaint();
		if(mChartPower!=null)
			 mChartPower.repaint();
	}
	
	private XYSeriesRenderer renderXY(int color, PointStyle style){
		XYSeriesRenderer newRenderer = new XYSeriesRenderer();        
		newRenderer.setColor(color);
		newRenderer.setPointStyle(style);
	    return newRenderer;
	}

//    private void addSampleData() {
//        mMemorySeries.add(100, 2000);
//        mMemorySeries.add(200, 3000);
//        mMemorySeries.add(300, 2000);
//        mMemorySeries.add(400, 5000);
//        mMemorySeries.add(500, 4000);
//        
//        mCPUSeries.add(200, 2000);
//        mCPUSeries.add(300, 3000);
//        mCPUSeries.add(400, 2000);
//        mCPUSeries.add(500, 5000);
//        mCPUSeries.add(600, 4000);
//    }
        
    private void addDataToSeries(XYSeries series, double x, double y){
    	series.add(x, y);
    }
    

	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.app_monitor, menu);
		return true;
	}

	

	
	
	//the background task that will compute the CPU and Memory usage
	//this task will asynchronously update the UI graph
	private class ComputeResources extends AsyncTask<Integer, Void, Void>{

		private Handler localhandler;
		public ComputeResources(Handler handler){
			this.localhandler = handler;
		}
		
		@Override
		protected Void doInBackground(Integer... params) {
			
			//we are using the CPU computing version 2
			// we are using the Memory computing version 3
			
			Integer[] processID = params;			
			
			Message msg;
			Bundle bundle;

			
			
			while(running){
				
				ArrayList<Stats> data = new ArrayList<Stats>();						
				String _cpu, _mem;
				//update the outputs to point to a stats object
				//the stats object will have all the results corresponding to a particular process.
							
				for (Integer pID : processID){
					
					Stats stats = new Stats();
					
					//compute CPU usage using V2
					int tempCPU = 0;
					try {
						tempCPU = rm.getCPUUsageV2(pID.intValue());
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
					
					//update stats with cpu data
					//data[1] = tempCPU;
					stats.setCpuUsage(tempCPU);

					
					
					//memory usage - using v3
					int tempMem = rm.getMemoryUsageV3(pID.intValue());					
					//data[3] = tempMem;
					//update stats with memory usage
					stats.setMemoryUsage(tempMem);	
					data.add(stats);
					
				}
				//computing the power usage of the application
				if(counterService == null) {	  		
					System.out.println("Waiting for profiler service...");	  						  	
				}
				
				
				else{			  						  	
				  	int keyId = KEY_CURRENT_POWER;
				  	power = PowerProfiler.getInstance(context, counterService);
				  	
				    try {
				    	int noUidMask = counterService.getNoUidMask();				    	
				    	byte[] rawUidInfo = null;
				    	try{

					    	int val = prefs.getInt("topWindowType", Counter.WINDOW_TOTAL);
					    	int val1 = prefs.getInt("topIgnoreMask", 0);
					    	
					        rawUidInfo = counterService.getUidInfo(
				            prefs.getInt("topWindowType", Counter.WINDOW_TOTAL),
				            noUidMask | prefs.getInt("topIgnoreMask", 0));
				    	}
				    	catch(Exception e){
				    		e.printStackTrace();
				    	}
				    	
				        if(rawUidInfo != null) {
				        	UidInfo[] uidInfos = (UidInfo[])new ObjectInputStream(
				        			new ByteArrayInputStream(rawUidInfo)).readObject();
				        	double total = 0;
				        	for(UidInfo uidInfo : uidInfos) {
				        		if(uidInfo.uid == SystemInfo.AID_ALL) continue;
				        		switch(keyId) {
				        		case KEY_CURRENT_POWER:
				        				uidInfo.key = uidInfo.currentPower;
				        				uidInfo.unit = "W";
				        				break;
				        			case KEY_AVERAGE_POWER:
					        			uidInfo.key = uidInfo.totalEnergy /
					        			(uidInfo.runtime == 0 ? 1 : uidInfo.runtime);
					        			uidInfo.unit = "W";
					        			break;
					        		case KEY_TOTAL_ENERGY:
					        			uidInfo.key = uidInfo.totalEnergy;
					        			uidInfo.unit = "J";
					        			break;
					        		default:
					        			uidInfo.key = uidInfo.currentPower;
					        			uidInfo.unit = "W";
					        	}
					        	total += uidInfo.key;
					        }
					        if(total == 0) total = 1;
					        for(UidInfo uidInfo : uidInfos) {
					        	uidInfo.percentage = 100.0 * uidInfo.key / total;
					        }
					        Arrays.sort(uidInfos);
					        for(int i = 0; i < uidInfos.length; i++) {
					        	if(uidInfos[i].uid == SystemInfo.AID_ALL ||
					        			uidInfos[i].percentage < HIDE_UID_THRESHOLD) {
					        		continue;
					        	}          
					        	
					        	String currentPower = power.currentPowerItem(uidInfos[i]);
					        	String instantPower = power.instantPowerItem(uidInfos[i].uid);
					        	String averagePower = power.averagePowerItem(uidInfos[i].uid);
					        	
					        	System.out.println("Current Power ->" + i + "," + currentPower);
					        	System.out.println("Instant Power ->" + i + "," + instantPower);
					        	System.out.println("Average Power ->" + i + "," + averagePower);
					        	
					        	data.get(i).setCurrentPower(Double.parseDouble(currentPower));
					        	data.get(i).setAveragePower(Double.parseDouble(averagePower));
					        	
					        	
					        	//saving the data to reflect in the UI - we send the 
					        	//current power in pos 0 and average power in pos 2
//					        	data[0] = Integer.parseInt(currentPower);
//					        	data[2] = (int)Double.parseDouble(averagePower);
					        }
				        }
				    } catch(IOException e) {
				    } catch(RemoteException e) {
				    } catch(ClassNotFoundException e) {
				    } catch(ClassCastException e) {
				    }
				}				
							
				

				// we combine both mem, cpu and power usage and send it as one message in a int array
				msg = localhandler.obtainMessage();
				bundle = new Bundle();				
				bundle.putParcelableArrayList(MESSAGE, data);
				msg.setData(bundle);
				localhandler.sendMessage(msg);				
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}					
			return null;
		}
		
	}
	
	
	
	private class CounterServiceConnection implements ServiceConnection {
		    public void onServiceConnected(ComponentName className,
		                                   IBinder boundService ) {
		      counterService = ICounterService.Stub.asInterface((IBinder)boundService);
		      try {
		        componentNames = counterService.getComponents();
		        noUidMask = counterService.getNoUidMask();		        
		        for(int i = 0; i < componentNames.length; i++) {
		          int ignMask = prefs.getInt("topIgnoreMask", 0);
		          if((noUidMask & 1 << i) != 0) continue;
		        }
		      } catch(RemoteException e) {
		        counterService = null;
		      }
		    }

			@Override
			public void onServiceDisconnected(ComponentName arg0) {				
				counterService = null;
			    getApplicationContext().unbindService(conn);
			    getApplicationContext().bindService(serviceIntent, conn, 0);
			    Log.w("AppMonitor", "Unexpectedly lost connection to service");
			}
	  }

	@Override
	public void run() {
	    if(handler != null) {
	        handler.postDelayed(this, 2 * PowerEstimator.ITERATION_INTERVAL);
	      }
//	    writeCSV();
	}
	
	
}
