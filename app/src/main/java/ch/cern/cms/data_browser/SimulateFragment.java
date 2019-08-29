package ch.cern.cms.data_browser;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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

public class SimulateFragment  extends Fragment implements fillSpinner {
	private ViewGroup container   = null;
	private boolean isVisible    = false;
	private boolean lastVisible  = false;
	
	private String simName        = "none";
	private String chanName       = "none";
	private Spinner spin1         = null;
	private Spinner spin2         = null;
	private String simTag         = null;
	private String simDate        = null;
	private String simTime        = null;
	private String simType        = null;
    
	private TextView tv;
	
	private boolean initialized  = false;
	private boolean broadcast    = false;
	private Settings settings     = null;
	private Logger Log;

	private int selected1         = -1;
	private int selected2         = -1;
	private boolean restore      = false;
	private boolean local        = false;
	
	private String simString;
	private dataList remoteList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        
    	this.container = container;

    	// Set up the local simulate menu options
    	setHasOptionsMenu(true);
    	
    	if ( savedInstanceState != null ) {
    		
    		if ( savedInstanceState.containsKey("selected1") )
    			selected1 = savedInstanceState.getInt("selected1");

    		if ( savedInstanceState.containsKey("selected2") )
        		selected2 = savedInstanceState.getInt("selected2");

    		if ( savedInstanceState.containsKey("restore") )
        		restore = savedInstanceState.getBoolean("restore");
    		
    		if ( savedInstanceState.containsKey("isLocal") )
    			local = savedInstanceState.getBoolean("isLocal");

    	} // End if ( savedInstanceState != null )

    	// Inflate the layout for this fragment
        return inflater.inflate(R.layout.simulate_fragment, container, false);
    
    }

    @Override
    public void onStart() {
        super.onStart();

        // Get the instance of the logging utility
        Log = Logger.getInstance();

    	if ( container == null ) { 
    		this.container = (ViewGroup)getView(); 
    		if ( container == null ) {
    			Log.e("Simulate()","Who am I? Where's my mommy?");
    			return;
    		}
    	}
    	
    	settings = Settings.getInstance();
    	
    	if ( remoteList == null )
    		remoteList = new dataList(container.getContext(), this, getActivity());

    	if ( restore )
    		remoteList.restore(local);

        spin1 = getView().findViewById(R.id.simSpinner);
    	spin1.setOnItemSelectedListener(new SimOnItemSelectedListener());
    	
        spin2 = getView().findViewById(R.id.chanSpinner);
    	spin2.setOnItemSelectedListener(new ChanOnItemSelectedListener());

    	/*************************
    	// Set up the pt selector
		float ptcut = 1000.0f*settings.getFloatSetting("ptcut");
    	SeekBar ptSlider = (SeekBar)container.findViewById(R.id.ptCut);
    	if ( ptSlider == null )
    		Log.e("opengl.onCreate()", "ptSlider is null");
    	else {
    		ptSlider.setOnSeekBarChangeListener(new ImposePtCut());
    		ptSlider.setProgress((int)(ptcut));
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
    	**********************/

    	// Sync the pt selector
    	this.setPtSlider();
    	   	
    	if ( selected2 == -1 )
    		spin2.setEnabled(false);
    	
        return;
    }
    
    @Override
    public void onSaveInstanceState( Bundle savedInstanceState ) {
    	
    	savedInstanceState.putInt("selected1",  selected1);
    	savedInstanceState.putInt("selected2",  selected2);
    	savedInstanceState.putBoolean("restore", true);
    	savedInstanceState.putBoolean("isLocal", remoteList.isLocal());
    	return;
    }
    
    @Override
	public void onHiddenChanged( boolean hidden ) {
    	this.lastVisible = this.isVisible;
		this.isVisible = !hidden;
		return;
	}

    @Override
	public void onCreateOptionsMenu( Menu menu, MenuInflater inflater) {
    	inflater.inflate(R.menu.simulate_menu, menu);
	   return;
	}
	
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId() ) {
		
		case R.id.action_sim_new:
			getNewSimEvent();
			return true;
			
		case R.id.action_sim_clear:
			return false;

		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
    /****************************************************/
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
    		//tv.setText(Html.fromHtml(getString(R.string.PTMin)));
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

    public void setSimulateString(List<String> data) {

    	simTag = data.get(0);
    	int length = data.size();

    	simString = "";
    	for ( int i=1; i<length; i++ )
    		simString += data.get(i)+'\n';
		
		DateFormat format = new SimpleDateFormat("yyyy/MM/dd", Locale.US);
		Date today = new Date();
		
		simDate = format.format(today);
		format = new SimpleDateFormat( "HH:mm:ss", Locale.US );
		simTime = format.format(today);
		
		String [] simArray = simTag.split(" -> ");

		if ( simArray.length > 2 ) {
			simType = simArray[1] + "->" + simArray[simArray.length-1];
			
		} else if (simArray.length == 2 ) {
			simType = simArray[0].substring(0, simArray[0].indexOf(" (E_{cm}")) + "->" + simArray[1];
		} else
			simType = simName+"->"+chanName;
		
		if ( settings .getIntSetting("debugLevel") > 1 ) Log.w("setSimulateString()", " type = "+simType);

    	return;
    }
 
    public String getSimulateString() {
    	return this.simString;
    }
    
    public void removeSim() {

    	if ( simName.equals("none") || chanName.equals("none") ) {
    		Log.e("removeSim()", "No event to remove");
    		return;
    	}
    	
		// Delete this simulation
		remoteList.deleteStoredSim();

		// Delete this entry from the list
		removeChannelEntry(simType);
		
		// Clear the current simulation channel
		chanName = "none";
		
    	return;
    }
    
    public void storeSim() {

    	if ( simName.equals("none") || chanName.equals("none") ) {
    		Log.e("removeSim()", "No event to remove");
    		return;
    	}
    	
    	// Add the currently displayed simulation to the local device database
    	remoteList.addSimToStore(simType,simTag,simDate,simTime,simString);
		
	    // If we've added a new event to the local store and the "Local" entry
	    // isn't already in the simulation list. Add it here. (It's always the last entry)
    	@SuppressWarnings("unchecked")
		ArrayAdapter<String> simAdapter = (ArrayAdapter<String>)spin1.getAdapter();
    	
    	if ( !simAdapter.getItem(simAdapter.getCount()-1).equals("Local") )
    		simAdapter.add("Local");

    	return;
    }
    
	@SuppressWarnings("unchecked")
    private void removeChannelEntry(String tag) {

    	ArrayAdapter<String> chanAdapter = (ArrayAdapter<String>)spin2.getAdapter();
    	
    	int length = chanAdapter.getCount();
    	
    	for ( int i=length-1; i>=0; i-- ) {

    		if ( chanAdapter.getItem(i).equals(tag) ) {
    			if ( settings .getIntSetting("debugLevel") > 1 ) Log.w("removeChannelEntry()", "Removing "+chanAdapter.getItem(i)+" from list");
    			chanAdapter.remove(chanAdapter.getItem(i));
    			spin2.setSelection(0);

    			// If this was the last item in the Local list, remove Local from
    			// the simulations list and reset to "Simulation" (item 0)
    			if ( chanAdapter.getCount() <= 0 ) {
    		    	
    				ArrayAdapter<String> simAdapter = (ArrayAdapter<String>)spin1.getAdapter();
    		    	if ( simAdapter.getItem(simAdapter.getCount()-1).equals("Local") ) {
    		    		simAdapter.remove(simAdapter.getItem(simAdapter.getCount()-1));
    		    		spin1.setSelection(0);
    		    		simName = "none";
    		    	}
    		    // If there's only two items left in the channel list, and
    		    // one of them is the channel_prompt header, then delete it
    			} else if ( chanAdapter.getCount() == 2 && chanAdapter.getItem(0).equals(getString(R.string.channel_prompt)) ) {
    				chanAdapter.remove(chanAdapter.getItem(0));
    				spin2.setSelection(0);
    			}
    		}
    	}
    	
    	return;
    	
    }
    
    public void clearDB() {
    	@SuppressWarnings("unchecked")
    	ArrayAdapter<String> simAdapter = (ArrayAdapter<String>)spin1.getAdapter();
    	if ( simAdapter.getItem(simAdapter.getCount()-1).equals("Local") ) {
    		simAdapter.remove(simAdapter.getItem(simAdapter.getCount()-1));
    		spin1.setSelection(0);
    		simName = "none";
    	}
    	remoteList.clearDB(false);
    	return;
    }
    
    public String getSelectedSimName() {
    	
    	if ( simType == null )
    		return "Event";
    	
    	return simType;
    }
    
    public void fill(List<String> entries, int whichSpinner) {
    	
    	if ( whichSpinner == 0 )
    		fillSimSpinner(entries);
    	else if ( whichSpinner == 1 )
    		fillChannelSpinner(entries);
    	else
    		Log.e("fill()", "Don't know which spinner is #"+whichSpinner);
    }
   
    public boolean isLocal() {
    	return remoteList.isLocal();
    }
    
    public boolean hasLocalData() {
    	return remoteList.hasLocal(false);
    }
    
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
    			remoteList.getSimulation(getString(R.string.simulate_prompt), "runSim.android.pl", "proc", "GET");
    	}
    	
    	return;
    }

    public void getNewSimEvent() {
    	
    	boolean condition = simName != null && !simName.equals("none") &&
    				chanName != null && !chanName.equals("none");
    	
    	if ( condition )
 			remoteList.getSimulation("simulation", "runSim.android.pl", "proc", simName, "chan", chanName);
    		
    	return;
    }
    
    public void fillSimSpinner(List<String> simList) {

    	ArrayAdapter<String> simAdapter = 
    			new ArrayAdapter<String>(container.getContext(),
    					android.R.layout.simple_spinner_item, simList);
    	simAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	spin1.setAdapter(simAdapter);

    	if ( selected1 > -1 && selected1 < spin1.getCount() ) {
    		simName = "";                   // Flush the simName on restores so it triggers a channel update
    		spin1.setSelection(selected1);
    	}
    	
    	return;
    } // End fillSimSpinner()

    private void fillChannelSpinner(List<String> chanList) {

    	if ( chanList != null && chanList.size() > 0 ) {
    		ArrayAdapter<String> chanAdapter = 
    				new ArrayAdapter<String>(container.getContext(),
    						android.R.layout.simple_spinner_item, chanList);
    		chanAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    		spin2.setAdapter(chanAdapter);
    		spin2.setEnabled(true);

    		if ( selected2 > -1 && selected2 < spin2.getCount() )
    			spin2.setSelection(selected2);
    		
    	} else {
    		spin2.setEnabled(false);
    	}
    	return;
    }
    
    private class SimOnItemSelectedListener implements OnItemSelectedListener {
   	 
    	public void onItemSelected(AdapterView<?> parent, View view, int pos,long id) {
    		 
    		if ( parent.getItemAtPosition(pos).toString().equals(getString(R.string.simulate_prompt)) ) {
    			spin2.setSelection(0);
    			spin2.setEnabled(false);
    			return;
    		}
    		
    		if ( simName.equals(parent.getItemAtPosition(pos).toString()) )
    			return;
    		
    		selected1 = pos;
    		simName = parent.getItemAtPosition(selected1).toString();
			spin2.setSelection(0);
			spin2.setEnabled(false);
			if ( !restore )
				selected2 = -1;
			
			// If this is a terminal restore, reset restore = false
			restore = !(selected2 == -1);
			
			if ( simName.equalsIgnoreCase("Local") )
				fillChannelSpinner( remoteList.getLocalSimList() );
				
			else
    			remoteList.getSimulation(getString(R.string.channel_prompt), "runSim.android.pl", "proc", simName, "chan", "GET");
    		
    		return;
    	}
    	 
    	@Override
    	public void onNothingSelected(AdapterView<?> arg0) {
    		return;
    	}
    	 
    } // End private class DataOnItemSelectedListener

    private class ChanOnItemSelectedListener implements OnItemSelectedListener {
   	 
    	public void onItemSelected(AdapterView<?> parent, View view, int pos,long id) {

    		if ( pos < 0 )
    			return;
    		
    		if ( parent.getItemAtPosition(pos).toString().equals(getString(R.string.channel_prompt)) ) {
     			return;
     		}

    		if ( chanName.equals(getString(R.string.channel_prompt)) )
    			return;
    		
     		chanName = parent.getItemAtPosition(pos).toString();

     		// If we're pulling up stored simulations, scan the list for duplicate names
     		if ( simName.equals("Local") ) {
     			
     			int index = 0;
     			for ( int i=0; i<pos; i++ ) {
     				if ( chanName.equals(parent.getItemAtPosition(i).toString()) ) {
     					index++;
     				}
     			}
     			// If there are multiple instances of this name, set the one we actually want
     			remoteList.setSimIndex(index);
     			
     		} else // Otherwise tell the DB class not to worry about it.
     			remoteList.setSimIndex(-1);
     		
     		if ( pos != selected2 )
     			remoteList.getSimulation("simulation", "runSim.android.pl", "proc", simName, "chan", chanName);

			selected2 = pos;
			restore = false;
			
			return;

     	} // End void onItemSelected
  
     	@Override
 	    public void onNothingSelected(AdapterView<?> arg0) {
     		return;
     	}
     } // End class EventOnItemSelectedListener

} // End public class SimulateFragment
