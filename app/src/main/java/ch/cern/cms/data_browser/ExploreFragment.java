package ch.cern.cms.data_browser;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import ch.cern.cms.data_browser.getData.dataList;
import ch.cern.cms.data_browser.getData.dataList.fillSpinner;

public class ExploreFragment extends Fragment implements fillSpinner {
	
	private ViewGroup container   = null;
	private boolean isVisible    = false;
	private boolean lastVisible  = false;
	private boolean readComplete = false;
	
	private Spinner  colSpinner   = null;  // Collection select spinner
	private Spinner  runSpinner   = null;  // Run select spinner
	private Spinner  evtSpinner   = null;  // Event select spinner
    private String   eventData    = null;
    private String   collection   = null;
    private String   eventName    = null;
    private String   runNumber    = null;
    private String   eventString  = null;
    private String   eventTag     = null;
    private String   eventTime    = null;
    private String   eventDate    = null;

    private TextView tv;
    
	private boolean initialized = false;
	private boolean broadcast   = true;

    private int selected1 = -1;
    private int selected2 = -1;
    private int selected3 = -1;
    private boolean restore = false;
    private boolean local = false;
    
    private boolean eventIncrement = false;
    private boolean runIncrement   = false;
    private boolean eventDecrement = false;
    private boolean runDecrement   = false;

    private Logger Log;
    private Settings settings;
        
    private dataList remoteList;
    
    public void fill(List<String> entries, int whichSpinner) {
    	
    	if ( whichSpinner == 0 )
    		fillFileSpinner(entries);
    	else if ( whichSpinner == 1 )
    		fillRunSpinner(entries);
    	else if ( whichSpinner == 2 )
    		fillEventSpinner(entries);
    	else
    		Log.e("fill()", "Don't know which spinner is #"+whichSpinner);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    	super.onCreate(savedInstanceState);
        
    	this.container = container;
    	settings = Settings.getInstance();

    	// Get the handle to the logging utility
    	Log = Logger.getInstance();
    	
    	setHasOptionsMenu(true);
    	
    	if ( savedInstanceState != null ) {
    		
    		if ( savedInstanceState.containsKey("selected1") )
    			selected1 = savedInstanceState.getInt("selected1");

    		if ( savedInstanceState.containsKey("selected2") )
        		selected2 = savedInstanceState.getInt("selected2");

    		if ( savedInstanceState.containsKey("selected3") )
        		selected3 = savedInstanceState.getInt("selected3");
    		
    		if ( savedInstanceState.containsKey("restore") )
        		restore = savedInstanceState.getBoolean("restore");
    		
    		if ( savedInstanceState.containsKey("isLocal") )
    			local = savedInstanceState.getBoolean("isLocal");

    	} // End if ( savedInstanceState != null )

    	// Inflate the layout for this fragment
        return inflater.inflate(R.layout.explore_fragment, container, false);
    }

    @Override
    public void onSaveInstanceState( Bundle savedInstanceState ) {
    	
    	savedInstanceState.putInt("selected1",  selected1);
    	savedInstanceState.putInt("selected2",  selected2);
    	savedInstanceState.putInt("selected3",  selected3);
    	savedInstanceState.putBoolean("isLocal", remoteList.isLocal());
    	savedInstanceState.putBoolean("restore", true);
    	
    	return;
    }
    
    @Override
    public void onStart() {
        super.onStart();
        
    	if ( container == null ) { 
    		this.container = (ViewGroup)getView(); 
    		if ( container == null ) {
    			Log.e("Explore()","Who am I? Where's my mommy?");
    			return;
    		}
    	}

    	// Create a new instance of the getList package if needed
    	if ( remoteList == null )
    		remoteList = new dataList(container.getContext(), this, getActivity());
    	
    	if ( restore )
    		remoteList.restore(local);

    	colSpinner = getView().findViewById(R.id.dataSpinner);
    	colSpinner.setOnItemSelectedListener(new DataOnItemSelectedListener());
    	
        runSpinner = getView().findViewById(R.id.runSpinner);
    	runSpinner.setOnItemSelectedListener(new RunOnItemSelectedListener());

        evtSpinner = getView().findViewById(R.id.eventSpinner);
    	evtSpinner.setOnItemSelectedListener(new EventOnItemSelectedListener());

    	/********************
    	// Set up the pt selector
		float ptcut = 1000.0f*settings.getFloatSetting("ptcut");
    	SeekBar ptSlider = (SeekBar)container.findViewById(R.id.ptCut);
    	if ( ptSlider == null )
    		Log.e("opengl.onCreate()", "ptSlider is null");
    	else {
    		ptSlider.setOnSeekBarChangeListener(new ImposePtCut());
    		ptSlider.setProgress((int)(1000*ptcut));
    	}
    	tv = ((TextView)container.findViewById(R.id.ptCutTxt));
    	if ( tv == null )
    		Log.e("opengl.onCreate()", "tv is null");
    	else {
			String text = "";
    		DecimalFormat df1 = new DecimalFormat("0.0");

			if ( ptcut < 1000 )
				text = getResources().getString(R.string.PTMeV, Integer.toString((int)ptcut));
			else
				text = getResources().getString(R.string.PTGeV, df1.format(ptcut/1000.0));
			
			if ( tv != null )
				tv.setText(Html.fromHtml(text));
    	}
    	*********************/
    	// Sync the pt selector
    	this.setPtSlider();

    	// Shut down the run/event spinners post haste
    	if ( selected2 == -1 )
    		runSpinner.setEnabled(false);
    	if ( selected3 == -1 )
    		evtSpinner.setEnabled(false);

		return;
    }
    
    @Override
	public void onHiddenChanged( boolean hidden ) {
    	super.onHiddenChanged(hidden);
    	this.lastVisible = this.isVisible;
		this.isVisible = !hidden;
		return;
	}
        
	@Override
	public void onCreateOptionsMenu( Menu menu, MenuInflater inflater) {
    	inflater.inflate(R.menu.explore_menu, menu);
		return;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (settings.getIntSetting("debugLevel") > 1) Log.w("ExpFrag()","onOptionsItemSelected()");
		
		switch (item.getItemId() ) {
		
		case R.id.action_exp_next:
			getNextEvent();
			return true;
			
		case R.id.action_exp_prev:
			getPrevEvent();
			return true;
			
		case R.id.action_exp_clear:
			eventData = null;
			return false;
			
		default:
			return super.onOptionsItemSelected(item);
		}
	}
    
	public void setPtSlider() {

		if ( container == null )
			return;
		
		// Set up the pt selector
		float ptcut = 1000.0f*settings.getFloatSetting("ptcut");
    	SeekBar ptSlider = container.findViewById(R.id.ptCut);
    	if ( ptSlider == null ) {
    		Log.e("opengl.onCreate()", "ptSlider is null");
    		return;
    	} else {
    		ptSlider.setOnSeekBarChangeListener(new ImposePtCut());
    		ptSlider.setProgress((int)(ptcut));
    	}
    	tv = container.findViewById(R.id.ptCutTxt);
    	if ( tv == null )
    		Log.e("opengl.onCreate()", "tv is null");
    	else {
			String text = "";
    		DecimalFormat df1 = new DecimalFormat("0.0");

			if ( ptcut < 1000 )
				text = getResources().getString(R.string.PTMeV, Integer.toString((int)ptcut));
			else
				text = getResources().getString(R.string.PTGeV, df1.format(ptcut/1000.0));
			
			if ( tv != null )
				tv.setText(Html.fromHtml(text));
    	}
    	broadcast = false;
    	return;
	}
	
    public void getNextEvent() {

    	if ( selected3 > -1 ) {

    		// Make sure there's a next item to get
    		if ( selected3 < evtSpinner.getCount()-1 ) {
    			// We're good.... increment away
    			evtSpinner.setSelection(selected3+1);

    		} else {
    			// Oh-oh... not enough room.... try the run view
    			if ( selected2 < runSpinner.getCount()-1 ) {
    				// Raise a flag so the eventFill knows what to do
    				eventIncrement = true;
    				selected3 = -1;
    				
    				// and bump the run select
    				runSpinner.setSelection(selected2+1);
    				
    			} else {
    				// Damn it! So we're out of runs too.... welp, increment the data type also
					eventIncrement = runIncrement = true;
					selected3 = selected2 = -1;
					
    				if ( selected1 < colSpinner.getCount()-1 ) {
    					// OK, at least there's room here
    					colSpinner.setSelection(selected1+1);
    				} else {
    					// Sheesh... nowhere to go.... Recycle
    					colSpinner.setSelection(1);
    				}
    			}
    		}

    	}
    	return;
    }
    
    public void getPrevEvent() {

    	if ( selected3 > 1 ) {
    		// OK, there's a previous item here
    		evtSpinner.setSelection(selected3-1);
    	} else {
    		// Out of previous items.... check the run spinner
    		if ( selected2 > 1 ) {
				// Raise a flag so the eventFill knows what to do
				eventDecrement = true;
				selected3 = -1;

				// and drop the run select
				runSpinner.setSelection(selected2-1);
    		} else {
    			// Out of runs as well
				eventDecrement = runDecrement = true;
				selected3 = selected2 = -1;
				
    			if ( selected1 > 1 ) {
					// OK, at least there's still room here
					colSpinner.setSelection(selected1-1);
    			} else {
					// nowhere to go.... Recycle
					colSpinner.setSelection(colSpinner.getCount()-1);
    			}
    		}
    		
    	}
    	
    	return;
    }

    public void setEventString(List<String> data) {
    	
    	this.eventTag = data.get(0);
    	
    	String t = "";
    	for ( int i=1; i<data.size(); i++ )
    		t += data.get(i) + '\n';
    	
    	this.eventString = t;
    	
    	int start = eventTag.indexOf("recorded on") + 12;
    	int end = eventTag.indexOf(" at ");
    	
    	if ( end > start && end < eventTag.length() )
    		eventDate = eventTag.substring(start, end);
    	else
    		Log.w("setEventString()", "Oh-oh! start = "+start+", end = "+end);
    	
    	start = end + 4;
    	end = start + 8;
    	if ( end > start && end < eventTag.length() )
    		eventTime = eventTag.substring(start, end);
    	else
    		Log.w("setEventString()", "Oh-oh! start = "+start+", end = "+end);
    	
    	return;
    }
    
    public String getEventString() {
    	return this.eventString;
    }
    
    public String getEventTag() {
    	return this.eventTag;
    }
    
    public void fillFileSpinner(List<String> fileList) {
    	if (settings.getIntSetting("debugLevel") > 1) Log.w("Explore()", "fillFileSpinner()");
    	
    	ArrayAdapter<String> dataAdapter = 
    			new ArrayAdapter<String>(container.getContext(),
    					android.R.layout.simple_spinner_item, fileList);
    	dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	colSpinner.setAdapter(dataAdapter);

    	if ( selected1 != -1 && selected1 < colSpinner.getCount() ) {
    		collection = "";
    		colSpinner.setSelection(selected1);
    	}
    	
    	return;
    } // End fillFileSpinner()
     
    public boolean isLocal() {
    	return remoteList.isLocal();
    }
    
    public boolean hasLocalData() {
    	return remoteList.hasLocal(true);
    }
    
    public void fillEventSpinner(List<String> eventList) {
    	if (settings.getIntSetting("debugLevel") > 1) Log.w("Explore()", "fillEventSpinner()");
    	
    	ArrayAdapter<String> eventAdapter = 
    			new ArrayAdapter<String>(container.getContext(),
    					android.R.layout.simple_spinner_item, eventList);
    	eventAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	evtSpinner.setAdapter(eventAdapter);
    	evtSpinner.setEnabled(true); 
    	
    	if ( eventIncrement ) {
    		if ( evtSpinner.getCount() > 1 )
    			evtSpinner.setSelection(1);
    		else
    			evtSpinner.setSelection(0);
    		eventIncrement = false;
    		
    	} else if ( eventDecrement ) {
    		evtSpinner.setSelection(evtSpinner.getCount()-1);
    		eventDecrement = false;
    		
    	} else if ( selected3 != -1 && selected3 < evtSpinner.getCount() )
    		evtSpinner.setSelection(selected3);
    	
    	return;
    } // End fillEventSpinner
     
    public void fillRunSpinner(List<String> runList) {
    	if (settings.getIntSetting("debugLevel") > 1) Log.w("Explore()", "fillRunSpinner()");
    	
    	ArrayAdapter<String> runAdapter = 
    			new ArrayAdapter<String>(container.getContext(),
    					android.R.layout.simple_spinner_item, runList);
    	runAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	runSpinner.setAdapter(runAdapter);
    	runSpinner.setEnabled(true);
    	
    	if ( runIncrement ) {
    		if ( runSpinner.getCount() > 1 )
    			runSpinner.setSelection(1);
    		else
    			runSpinner.setSelection(0);
    		runIncrement = false;

    	} else if ( runDecrement ) {
    		runSpinner.setSelection(runSpinner.getCount()-1);
    		runDecrement = false;
    		
    	} else if ( selected2 != -1  && selected2 < runSpinner.getCount() ) {
    		eventName = "";
    		runSpinner.setSelection(selected2);
    	}

    	return;
    } // End fillRunSpinner

    public void setVisibleState(boolean visible) { 
    	this.lastVisible = this.isVisible;
    	this.isVisible = visible;
    	return;
    }
    
    public boolean getVisibleState() {
    	return this.isVisible;
    }

    public boolean getLastVisibleState() {
    	return this.lastVisible;
    }
    
    public void setNetworkState(boolean netState) {
    	
    	if ( !isAdded() ) {
    		try {
    			throw new IllegalStateException();
    		} catch (IllegalStateException e) {
    			Log.e("Exp.setNetworkState()", "Calling detached fragment");
    			return;
    		}
    	}
    	
    	remoteList.setNetworkState(netState);
    	
    	if ( netState && !initialized ) {
    		if ( remoteList != null )
    			remoteList.getData(getString(R.string.data_prompt), "getPublicData.pl", "select", "collections");
    	}
    	
    	return;
    }
    
    public boolean isReadComplete() {
    	return readComplete;
    }
    
    public String getRunNumber() {
    	return this.runNumber;
    }
    
    public String getEventNumber() {
    	return this.eventName;
    }
    
    public boolean storeEvent () {
    	if ( runNumber == null || eventName == null )
    		return false;
    	
		String [] args = new String [] {runNumber, eventName, collection, eventTag, eventDate, eventTime, eventString};
		remoteList.addEventToStore(args);
		
	    // If we've added a new event to the local store and the "Local" entry
	    // isn't already in the simulation list. Add it here. (It's always the last entry)
    	@SuppressWarnings("unchecked")
		ArrayAdapter<String> expAdapter = (ArrayAdapter<String>)colSpinner.getAdapter();
    	
    	if ( !expAdapter.getItem(expAdapter.getCount()-1).equals("Local") )
    		expAdapter.add("Local");
    	
    	return true;

    }
    
    public boolean removeEvent() {
    	if ( runNumber == null || eventName == null )
    		return false;
    	
		remoteList.deleteStoredEvent();
		removeEvent(runNumber, eventName);

		return true;

    }
    
    // If we deleted an event from the local store, remove it from the Local list
    private void removeEvent(String run, String event) {
    	@SuppressWarnings("unchecked")
		ArrayAdapter<String> evtAdapter = (ArrayAdapter<String>)evtSpinner.getAdapter();
    	
    	int length = evtAdapter.getCount();
    	for ( int i=length-1; i>=0; i-- ) {
    		if ( evtAdapter.getItem(i).equals(event) ) {
    			evtAdapter.remove(evtAdapter.getItem(i));
    			eventName = eventData = null;

    			// If no events are left from this run, delete the run.
    			if ( evtAdapter.getCount() <= 0 ) {
    				removeRun(run);
    				
    			// If the only thing left if the "Event" header, delete the run
    			} else if ( evtAdapter.getCount() == 1 && evtAdapter.getItem(0).equals(getString(R.string.event_prompt)) ) {
    				evtAdapter.remove(evtAdapter.getItem(0));
    				removeRun(run);
    				
    			// If there's only one event left, remove the "Event" header (superfluous)
    			} else if ( evtAdapter.getCount() == 2 && evtAdapter.getItem(0).equals(getString(R.string.event_prompt)) ) {
    				evtAdapter.remove(evtAdapter.getItem(0));
    			}

    			evtSpinner.setAdapter(evtAdapter);
    			break;
    		}
    	}

    	return;
    }
    
    // If we've cleared out all of the events from this run, remove this run from the Local list
    private void removeRun(String run) {

    	@SuppressWarnings("unchecked")
		ArrayAdapter<String> runAdapter = (ArrayAdapter<String>)runSpinner.getAdapter();

    	int length = runAdapter.getCount();
    	for ( int i=length-1; i>=0; i-- ) {
    		
    		if (  runAdapter.getItem(i).equals(run) ) {
    			runAdapter.remove(runAdapter.getItem(i));
    			runNumber = eventName = eventData = null;
    			
    			// If this was the last run on the list, check and see if we still have the run prompt hanging around
		    	if ( runAdapter.getCount() == 1 && runAdapter.getItem(0).equals(getString(R.string.run_prompt)) )
		    			runAdapter.remove(runAdapter.getItem(0));
		    	// If there are two runs on the list, and one of them is the run prompt, remove it
		    	else if ( runAdapter.getCount() == 2 && runAdapter.getItem(0).equals(getString(R.string.run_prompt)) )
	    			runAdapter.remove(runAdapter.getItem(0));

    			// If this was the last run in the Local list, remove Local from the list
    			if ( runAdapter.getCount() <= 0 ) {

    		    	@SuppressWarnings("unchecked")
    				ArrayAdapter<String> colAdapter = (ArrayAdapter<String>)colSpinner.getAdapter();
    		    	
    		    	if ( colAdapter.getItem(colAdapter.getCount()-1).equals("Local") ) {
    		    		colAdapter.remove(colAdapter.getItem(colAdapter.getCount()-1));
    		    		colSpinner.setAdapter(colAdapter);
    		    	}
    			}
    			runSpinner.setAdapter(runAdapter);
    			break;
    		}
    	}
    	
    	return;
    }

    public void clearDB() {
    	remoteList.clearDB(true);
    	return;
    }
    
    public void checkMD5File() {
    	remoteList.checkFile("hcal.ser");
    	return;
    }
    
    /******************************************************************************************/
    public class ImposePtCut implements OnSeekBarChangeListener {
    	
    	@Override
    	public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
    		broadCastStatus("seekBarProgressChanged", progressValue);
    		
			String text = "";
    		DecimalFormat df1 = new DecimalFormat("0.0");

			if ( progressValue < 1000 )
				text = getResources().getString(R.string.PTMeV, Integer.toString(progressValue));
			else
				text = getResources().getString(R.string.PTGeV, df1.format(progressValue/1000.0));
			
			if ( tv != null )
				tv.setText(Html.fromHtml(text));
			
    		return;
    	}

    	@Override
    	public void onStartTrackingTouch(SeekBar seekBar) {
    		onProgressChanged(seekBar, seekBar.getProgress(), false);
    		//tv.setTextSize(12f);
    	}

    	@Override
    	public void onStopTrackingTouch(SeekBar seekBar) {
    		broadCastStatus("seekBarStopTracking", seekBar.getProgress());
    	}
    	
		private void broadCastStatus( String msg, int progress ) {
			
			if ( broadcast ) {
				Intent intent = new Intent("fragmentMsg");
				intent.putExtra("seekBarStatusChanged", true);
				intent.putExtra("changeType", msg);
				intent.putExtra("widgetName", "ptcut");
				intent.putExtra("progress",   progress);
				LocalBroadcastManager.getInstance(container.getContext()).sendBroadcast(intent);
			} else
				broadcast = true;
			
		}
    	
    }
    
    private class DataOnItemSelectedListener implements OnItemSelectedListener {
   	 
    	public void onItemSelected(AdapterView<?> parent, View view, int pos,long id) {
    		
    		// If we just "chose" the same thing we already got, bail
    		if ( collection != null && collection.equals(parent.getItemAtPosition(pos).toString()) )
    			return;

    		// We're getting a new collection, reset everything in preparation
			fillRunSpinner(new ArrayList<String>(Arrays.asList((getString(R.string.run_prompt)))));
			fillEventSpinner(new ArrayList<String>(Arrays.asList((getString(R.string.event_prompt)))));
			runSpinner.setEnabled(false);
			evtSpinner.setEnabled(false);
			runSpinner.setSelection(0);
			evtSpinner.setSelection(0);
			if ( !restore )
				selected2 = selected3 = -1;
			
			runNumber = eventName = eventData = null;

    		// If we selected the collection header, reset and bail
    		if ( parent.getItemAtPosition(pos).toString().equals(getString(R.string.data_prompt)) ) {
    			collection = null;
    			return;
    		}

    		// Store the new collection name
    		collection = parent.getItemAtPosition(pos).toString();
    		selected1 = pos;
    		
			// If this is a terminal restore, reset restore = false
			restore = !(selected2 == -1);

			if ( collection.equalsIgnoreCase("Local") ) {
				fillRunSpinner( remoteList.getLocalRunList() );
				
			} else if ( remoteList != null )
				remoteList.getData(getString(R.string.run_prompt), "getPublicData.pl", "select", "runs", "filter","type='"+collection+"'");

			return;
    	
    	}
    	 
    	@Override
    	public void onNothingSelected(AdapterView<?> arg0) {
    		return;
    	}
    	 
    } // End private class DataOnItemSelectedListener

    private class RunOnItemSelectedListener implements OnItemSelectedListener {
    	 
    	public void onItemSelected(AdapterView<?> parent, View view, int pos,long id) {

    		if ( runNumber != null && runNumber.equals(parent.getItemAtPosition(pos).toString()))
				return;

    		// We've chosen a new run number... get ready for it
    		fillEventSpinner(new ArrayList<String>(Arrays.asList((getString(R.string.event_prompt)))));
			evtSpinner.setEnabled(false);
			evtSpinner.setSelection(0);
			if ( !restore )
				selected3 = -1;
			eventName = null;
			eventData = null;

			restore = !(selected3 == -1 );
			
    		if ( parent.getItemAtPosition(pos).toString().equals(getString(R.string.run_prompt)) ) {
    			runNumber = null;
    			return;
    		}

    		runNumber = parent.getItemAtPosition(pos).toString();
    		selected2 = pos;

        	if ( collection.equalsIgnoreCase("Local") ) {
				fillEventSpinner(remoteList.getLocalEventList(runNumber));
				
			} else if ( remoteList != null )
				remoteList.getData(getString(R.string.event_prompt), "getPublicData.pl", "select", "event", "filter","run="+runNumber+" and type='"+collection+"'");
    	}
    	 
    	@Override
    	public void onNothingSelected(AdapterView<?> arg0) {
    		return;
    	}
    	 
     } // End private class RunOnItemSelectedListener
    
    private class EventOnItemSelectedListener implements OnItemSelectedListener {
    	 
    	public void onItemSelected(AdapterView<?> parent, View view, int pos,long id) {

    		if ( parent.getItemAtPosition(pos).toString().equals(getString(R.string.event_prompt)) ) {
    			eventName = null;
     			eventData = null;
     			return;
     		}

     		eventName = parent.getItemAtPosition(pos).toString();
     		eventData="type='"+collection+"' and run="+runNumber+" and event="+eventName;
     		
     		// If we were restoring... we're done now.
     		restore = false;
     		
     		if ( pos != selected3 )
     			remoteList.getData("data", "getPublicData.pl", "select", "tag,data", "filter", eventData);
     		
     		selected3 = pos;

        	return;
     	} // End void onItemSelected
  
     	@Override
 	    public void onNothingSelected(AdapterView<?> arg0) {
     		return;
     	}
     } // End class EventOnItemSelectedListener
     
} // public class ExploreFragment 
