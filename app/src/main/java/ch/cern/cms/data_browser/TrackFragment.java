package ch.cern.cms.data_browser;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.Fragment;
import android.content.Context;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

public class TrackFragment extends Fragment {

	private Context  context     = null;
	private View     view        = null;
	private boolean isVisible   = false;
	private boolean lastVisible = false;

    private Menu menu = null;
    private int trackID = -1;
    private String color = "";
    
    private Logger Log;
    
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    	// Inflate the layout for this fragment
    	View view = inflater.inflate(R.layout.add_track_fragment, container, false);

    	// Attach to the logger
    	Log = Logger.getInstance();
    	
    	if ( this.view == null )
           	this.view = view;

    	this.context = getActivity().getApplicationContext();
    	if ( context == null )
    		Log.e("onCreate()", "Context is null!");
    	
    	setHasOptionsMenu(true);

       	TextView lbl = view.findViewById(R.id.pxLabel);
       	if ( lbl != null ) {
       		lbl.setText(Html.fromHtml(getString(R.string.px_label)));
       		lbl = null;
    	}
    			
       	lbl = view.findViewById(R.id.pyLabel);
       	if ( lbl != null ) {
       		lbl.setText(Html.fromHtml(getString(R.string.py_label)));
       		lbl = null;
       	}

       	lbl = view.findViewById(R.id.pzLabel);
       	if ( lbl != null ) {
       		lbl.setText(Html.fromHtml(getString(R.string.pz_label)));
       		lbl = null;
       	}
    		
       	Spinner trk = view.findViewById(R.id.trkSpin);
       	trk.setOnItemSelectedListener(new TrkItemSelectedListener());
    		
       	Spinner clr = view.findViewById(R.id.clrSpin);
       	clr.setOnItemSelectedListener(new ClrItemSelectedListener());
    		
       	// Return the view
        return view;
    }

	@Override
	public void onHiddenChanged( boolean hidden ) {
		this.lastVisible = this.isVisible;
		
		this.isVisible = !hidden;
		if ( this.isVisible ) {
			List<String> numberOfTracks  = ((MainActivity)view.getContext()).getNumberOfTracks();
			ArrayAdapter<String> trackAdapter = 
					new ArrayAdapter<String>(view.getContext(),	android.R.layout.simple_spinner_item, numberOfTracks);
			trackAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			((Spinner)view.findViewById(R.id.trkSpin)).setAdapter(trackAdapter);
		}
		return;
	}
	
	@Override
	public void onCreateOptionsMenu( Menu menu, MenuInflater inflater) {
	   inflater.inflate(R.menu.track_menu, menu);
	   this.menu = menu;
	   
	   MenuItem item = menu.findItem(R.id.action_track_edit);
	   item.setEnabled(false);
	   item.setVisible(false);
	   
	   item = menu.findItem(R.id.action_track_add);
	   item.setEnabled(false);
	   item.setVisible(false);

	   item = menu.findItem(R.id.action_track_discard);
	   item.setEnabled(false);
	   item.setVisible(false);

	   item = menu.findItem(R.id.action_track_cancel);
	   item.setEnabled(false);
	   item.setVisible(false);

	   return;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		switch (item.getItemId()) {

			case R.id.action_track_add:
				addTrack();
				return true;

			case R.id.action_track_discard:
				delTrack();
				return true;
				
			case R.id.action_track_edit:
				editTrack();
				return true;
				
			case R.id.action_track_cancel:
				reset();
				return true;
				
			/*case R.id.action_track_back:
				return false;  // Handled in MainActivity.onOptionsItemSelected(MenuItem item)
				*/
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
        return;
	}
	
    @Override
    public void onActivityCreated(Bundle inState) {
    	super.onActivityCreated(inState);
    	return;
    }
	
	public void getTrackData() {
		Bundle trackData = ((MainActivity)getActivity()).getTrackData(trackID);
		if ( trackData == null )
			return;
		
		int id = trackData.getInt("id");
		if ( id != trackID ) {
			Log.e("TrackFrag()", "Got id = "+ id +" instead of "+ trackID);
			return;
		}

		double [] p = trackData.getDoubleArray("p");
		int vtx = trackData.getInt("vtx");
		int type = trackData.getInt("type");
		int charge = trackData.getInt("charge");
		String color = trackData.getString("color");
    	 
		// Load in the proper color from the color strings
		String name = context.getPackageName();

		String[] colorNames = context.getResources().getStringArray(R.array.colors);

		for ( int i=0; i<colorNames.length; i++ ) {
			if ( !colorNames[i].equals("color") ) {
				String clr = 
						context.getString(context.getResources().getIdentifier(colorNames[i], "color", name ));
				if ( color.equals(clr) ) {
					((Spinner)view.findViewById(R.id.clrSpin)).setSelection(i);
					break;
				}
			}
		}
    	 
		RadioGroup rg = view.findViewById(R.id.typeRadioGroup);
		if ( rg != null ) {
    	 
			if ( type == constants.EM ) 
				rg.check(R.id.typeEMRadio);
    		 
			else if ( type == constants.HD || type == constants.TK ) 
				rg.check(R.id.typeHDRadio);
    		 
			else if ( type == constants.MU ) 
				rg.check(R.id.typeMURadio);    		 
		}
    	 
		rg = view.findViewById(R.id.chargeRadioGroup);
		if ( rg != null ) {
    	 
			if ( charge == -1 )
				rg.check(R.id.chargeMinusRadio);
			
			else if ( charge == 0 )
				rg.check(R.id.chargeZeroRadio);
    		 
			else if ( charge == 1 )
				rg.check(R.id.chargePlusRadio);
		}
		
		DecimalFormat df = new DecimalFormat("#.###");

		((EditText)view.findViewById(R.id.pxEntry)).setText(df.format(p[0]));
		((EditText)view.findViewById(R.id.pyEntry)).setText(df.format(p[1]));
		((EditText)view.findViewById(R.id.pzEntry)).setText(df.format(p[2]));
		((EditText)view.findViewById(R.id.vtxEntry)).setText(df.format(vtx));
    	 
		return;
	}
     
	private void broadcast( String messageType, Bundle trackData ) {
    	 
		Intent intent = new Intent("fragmentMsg");
		intent.putExtra("widgetName", messageType);
		intent.putExtra("trackData",  trackData);
		LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

		return;
     }
     
     public void editTrack() {
    	 Bundle trackData = new Bundle();

      	double px = 0.0, py = 0.0, pz = 0.0;
      	int vtx = 0, type = 0, charge = 0;
      	
      	try {
      		px = Double.parseDouble(
      				((EditText)view.findViewById(R.id.pxEntry)).getText().toString());
      	} catch (Exception e) {}
      	try {
      		py = Double.parseDouble(
      				((EditText)view.findViewById(R.id.pyEntry)).getText().toString());
      	} catch (Exception e) {}
      	
      	try {
      			pz = Double.parseDouble(
      					((EditText)view.findViewById(R.id.pzEntry)).getText().toString());
      	} catch (Exception e) {}
      	
      	try {
      		vtx = Integer.parseInt( 
      				((EditText)view.findViewById(R.id.vtxEntry)).getText().toString());
      	} catch(Exception e) {}

      	int t = ((RadioGroup)view.findViewById(R.id.typeRadioGroup)).getCheckedRadioButtonId();
      	
      	if ( t == R.id.typeEMRadio )
      		type = constants.EM;
      	else if ( t == R.id.typeHDRadio )
      		type = constants.HD;
      	else if ( t == R.id.typeMURadio )
      		type = constants.MU;
      	else
      		type = -1;

      	t = ((RadioGroup)view.findViewById(R.id.chargeRadioGroup)).getCheckedRadioButtonId();

      	if ( t == R.id.chargeMinusRadio)
      		charge = -1;
      	else if ( t == R.id.chargeZeroRadio )
      		charge = 0;
      	else if ( t == R.id.chargePlusRadio )
      		charge = +1;
      	
     	if ( color == null || color.equals("") )
     		color = getString(getResources().getIdentifier("red" , "color", context.getPackageName()));

      	// Stuff 'em into the bundle
      	trackData.putDoubleArray("p", new double [] {px,py,pz});
      	trackData.putInt("vtx", vtx);
      	trackData.putInt("id", trackID);
      	trackData.putString("color", color);	        	
      	trackData.putInt("type", type);
      	trackData.putInt("charge",  charge);
      	
     	InputMethodManager imm = (InputMethodManager)context.getSystemService(
     			Context.INPUT_METHOD_SERVICE);
     			imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
      	
      	broadcast("editTrack", trackData);

    	 return;
     }
     
     public void delTrack() {
    	 Bundle trackData = new Bundle();
    	 trackData.putInt("trackID", trackID);
    	 broadcast("deleteTrack", trackData);
    	 
       	// Update the track list to remove the deleted track. getNumberOfTracks fails
       	// as it gets called from MainActivity before the broadcast message is received
       	int size = ((Spinner)view.findViewById(R.id.trkSpin)).getAdapter().getCount();
       	List<String> numberOfTracks = new ArrayList<String>();
     	numberOfTracks.add("Track");
     	numberOfTracks.add("New");

     	for ( int i=0; i<size-2; i++ ) {
     		if ( i != trackID )
     			numberOfTracks.add(String.valueOf(i+1));
     	}
     	ArrayAdapter<String> trackAdapter = 
 				new ArrayAdapter<String>(view.getContext(),	android.R.layout.simple_spinner_item, numberOfTracks);
 		trackAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
 		((Spinner)view.findViewById(R.id.trkSpin)).setAdapter(trackAdapter);
       	
    	 return;
     }
     
     public void addTrack() {
    	 
     	Bundle trackData = new Bundle();
     	double px = 0.0, py = 0.0, pz = 0.0;
     	int vtx = 0, type = 0, charge = 0;
     	
     	try {
     		px = Double.parseDouble(
     				((EditText)view.findViewById(R.id.pxEntry)).getText().toString());
     	} catch (Exception e) {}
     	try {
     		py = Double.parseDouble(
     				((EditText)view.findViewById(R.id.pyEntry)).getText().toString());
     	} catch (Exception e) {}
     	
     	try {
     			pz = Double.parseDouble(
     					((EditText)view.findViewById(R.id.pzEntry)).getText().toString());
     	} catch (Exception e) {}
     	
     	try {
     		vtx = Integer.parseInt( 
     				((EditText)view.findViewById(R.id.vtxEntry)).getText().toString());
     	} catch(Exception e) {}

     	int t = ((RadioGroup)view.findViewById(R.id.typeRadioGroup)).getCheckedRadioButtonId();
     	
     	if ( t == R.id.typeEMRadio )
     		type = constants.EM;
     	else if ( t == R.id.typeHDRadio )
     		type = constants.HD;
     	else if ( t == R.id.typeMURadio )
     		type = constants.MU;
     	else
     		type = -1;

     	t = ((RadioGroup)view.findViewById(R.id.chargeRadioGroup)).getCheckedRadioButtonId();

     	if ( t == R.id.chargeMinusRadio)
     		charge = -1;
     	else if ( t == R.id.chargeZeroRadio )
     		charge = 0;
     	else if ( t == R.id.chargePlusRadio )
     		charge = +1;
     	
     	if ( color == null || color.equals("") )
     		color = getString(getResources().getIdentifier("red" , "color", context.getPackageName()));

     	// Stuff 'em into the bundle
     	trackData.putDoubleArray("p", new double [] {px,py,pz});
     	trackData.putInt("vtx", vtx);
     	trackData.putString("color", color);	        	
     	trackData.putInt("type", type);
     	trackData.putInt("charge",  charge);
     	
     	InputMethodManager imm = (InputMethodManager)context.getSystemService(
     			Context.INPUT_METHOD_SERVICE);
     			imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

      	broadcast("addTrack", trackData);

      	// Clear the box
      	reset();
      	
      	// Update the track list to add the new track. getNumberOfTracks fails because
      	// it gets called from MainActivity before the broadcast message is received
      	int size = ((Spinner)view.findViewById(R.id.trkSpin)).getAdapter().getCount();
      	List<String> numberOfTracks = new ArrayList<String>();
    	numberOfTracks.add("Track");
    	numberOfTracks.add("New");

    	for ( int i=2; i<size+1; i++ ) {
    		numberOfTracks.add(String.valueOf(i-1));
    	}
    	ArrayAdapter<String> trackAdapter = 
				new ArrayAdapter<String>(view.getContext(),	android.R.layout.simple_spinner_item, numberOfTracks);
		trackAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		((Spinner)view.findViewById(R.id.trkSpin)).setAdapter(trackAdapter);
      	
    	 return;
     }
     
     public void reset() {
    	 ((EditText)view.findViewById(R.id.pxEntry)).setText("");
    	 ((EditText)view.findViewById(R.id.pyEntry)).setText("");
    	 ((EditText)view.findViewById(R.id.pzEntry)).setText("");
    	 ((EditText)view.findViewById(R.id.vtxEntry)).setText("");
    	 ((RadioGroup)view.findViewById(R.id.typeRadioGroup)).check(R.id.typeHDRadio);
    	 ((RadioGroup)view.findViewById(R.id.chargeRadioGroup)).check(R.id.chargeZeroRadio);
    	 ((Spinner)view.findViewById(R.id.trkSpin)).setSelection(0);
    	 ((Spinner)view.findViewById(R.id.clrSpin)).setSelection(0);

    	 InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
    	 imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    	 
    	 return;
     }
     
     public class TrkItemSelectedListener implements OnItemSelectedListener {

    	 public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
    		trackID = pos-2;
    		String name = parent.getItemAtPosition(pos).toString().toLowerCase(Locale.US);

    		if ( name.equals("track") ) {
    			
    			MenuItem item = menu.findItem(R.id.action_track_edit);
    			item.setEnabled(false);
    			item.setVisible(false);

    			item = menu.findItem(R.id.action_track_cancel);
    			item.setEnabled(false);
    			item.setVisible(false);

    			item = menu.findItem(R.id.action_track_add);
    			item.setEnabled(false);
    			item.setVisible(false);

    			item = menu.findItem(R.id.action_track_discard);
    			item.setEnabled(false);
    			item.setVisible(false);

    			reset();
    			
    		} else if ( name.equals("new") ) {

    			MenuItem item = menu.findItem(R.id.action_track_edit);
    			item.setEnabled(false);
    			item.setVisible(false);

    			item = menu.findItem(R.id.action_track_discard);
    			item.setEnabled(false);
    			item.setVisible(false);

    			item = menu.findItem(R.id.action_track_add);
    			item.setEnabled(true);
    			item.setVisible(true);

    			item = menu.findItem(R.id.action_track_cancel);
    			item.setEnabled(true);
    			item.setVisible(true);

    		} else {

    			MenuItem item = menu.findItem(R.id.action_track_edit);
    			item.setEnabled(true);
    			item.setVisible(true);

    			item = menu.findItem(R.id.action_track_cancel);
    			item.setEnabled(true);
    			item.setVisible(true);
    			
    			item = menu.findItem(R.id.action_track_discard);
    			item.setEnabled(true);
    			item.setVisible(true);

    			item = menu.findItem(R.id.action_track_add);
    			item.setEnabled(false);
    			item.setVisible(false);

    			getTrackData();
    		}
    		return;
    	 }

    	 public void onNothingSelected(AdapterView<?> parent) {
    		 // Do nothing.
    	 }
     }
     
     public class ClrItemSelectedListener implements OnItemSelectedListener {

    	 public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
    		 if ( pos == 0 ) {
    			 color = "";
    			 return;
    		 }
    		String name = parent.getItemAtPosition(pos).toString().toLowerCase(Locale.US);
     		if ( name.equals("color") )
     			return;
     		
     		color = getString(getResources().getIdentifier(name , "color", context.getPackageName()));
    	 }

    	 public void onNothingSelected(AdapterView<?> parent) {
    		 // Do nothing.
    	 }
     }

     public void setVisibleState(boolean visible) {
    	 this.lastVisible = this.isVisible;
    	 this.isVisible = visible;
    	 return; 
     }

     public boolean getVisibleState() { return this.isVisible;   }

     public boolean getLastVisible()  { return this.lastVisible; }
     
}
