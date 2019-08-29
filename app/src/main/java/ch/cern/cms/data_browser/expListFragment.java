package ch.cern.cms.data_browser;

import java.util.ArrayList;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import ch.cern.cms.data_browser.listManager.expandedListManager;

public class expListFragment extends Fragment {
	private ExpandableListView listView = null;
	private expandedListManager viewManager = null;
	private boolean visible = false;
	private ViewGroup container = null;
	private ArrayList<Integer> listState = null;
	private ArrayList<Integer> itemState = null;
	private Settings settings = null;
	private Logger Log;

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
    	this.container = container;
    	this.settings = Settings.getInstance();
    	this.Log = Logger.getInstance();
    	return inflater.inflate(R.layout.list_fragment, container, false);
    }

    @Override
	public void onHiddenChanged( boolean hidden ) {		
		this.visible = !hidden;
		return;
	}
    
    public void setVisibleState( boolean canuseeme ) {
    	this.visible = canuseeme;
    }
    
    public boolean getVisibleState() {
    	return this.visible;
    }
    
    @Override
    public void onStart() {
    	super.onStart();

    	if ( this.container == null ) { 
    		this.container = (ViewGroup)getView(); 
    		if ( container == null ) {
    			Log.e("Control()","Who am I? Where's my mommy?");
    			return;
    		}
    	}

    	listView = container.findViewById(R.id.expListView);
    	if ( listView == null )
    		Log.e("onCreateView()", "listView is still null");
    	
    	viewManager = new expandedListManager(listView,container.getContext(), "Controls", "controls.xml");

    	// Override the default settings in the XML file and set the 
    	// visibility sliders & toggles to the saved values in the preferences bundle
    	viewManager.setItem("TransAll",  getIntSetting("transAll"),  getBooleanSetting("allState"));
    	viewManager.setItem("TransTrkr", getIntSetting("transTrkr"), getBooleanSetting("trkrState"));
    	viewManager.setItem("TransEcal", getIntSetting("transEcal"), getBooleanSetting("ecalState"));
    	viewManager.setItem("TransHcal", getIntSetting("transHcal"), getBooleanSetting("hcalState"));
    	viewManager.setItem("TransCryo", getIntSetting("transCryo"), getBooleanSetting("cryoState"));
    	viewManager.setItem("TransYoke", getIntSetting("transYoke"), getBooleanSetting("yokeState"));
    	viewManager.setItem("TransCaps", getIntSetting("transCaps"), getBooleanSetting("capsState"));
    	viewManager.setItem("TransAxes", getIntSetting("transAxes"), getBooleanSetting("axesState"));
    	
    	// Set the toggles in the "Display" group
    	viewManager.setItem("Detector",  getBooleanSetting("allState"));
    	viewManager.setItem("Response",  getBooleanSetting("response"));
    	viewManager.setItem("Paths",     getBooleanSetting("paths"));
    	viewManager.setItem("Electrons", getBooleanSetting("showElectrons"));
    	viewManager.setItem("Photons",   getBooleanSetting("showPhotons"));
    	viewManager.setItem("Tracks",    getBooleanSetting("showTracks"));
    	viewManager.setItem("JetMET",    getBooleanSetting("showJetMET"));
    	viewManager.setItem("Muons",     getBooleanSetting("showMuons"));
    	
    	// Set the toggles in the "Filters" group
    	viewManager.setItem("JetAxis",         getBooleanSetting("showAxis"));
    	viewManager.setItem("JetCone",         getBooleanSetting("showCone"));
    	viewManager.setItem("JetHadrons",      getBooleanSetting("showHadrons"));
    	viewManager.setItem("TrackerMuons",    getBooleanSetting("showTracker"));
    	viewManager.setItem("StandAloneMuons", getBooleanSetting("showStandAlone"));
    	viewManager.setItem("GlobalMuons",     getBooleanSetting("showGlobal"));
    	viewManager.setItem("UnderlyingEvent", getBooleanSetting("showUnderlying"));
    	viewManager.setItem("Undetectable",    getBooleanSetting("showUndetectable"));

    	// Set the debug slider
    	viewManager.setItem("DebugLevel",  getIntSetting("debugLevel"));
    	
    	// Set the sliders
    	viewManager.setItem("MaxEvents",   getIntSetting("maxEvents")/10);
    	viewManager.setItem("ZoomSpeed",   getIntSetting("zoomSpeed")-2);
    	viewManager.setItem("MaxTime",     getIntSetting("maxTime")-25);
    	
    	viewManager.setItem("RotateSpeed", 500f + 1f/getFloatSetting("rotateSpeed"));
    	viewManager.setItem("Resolution",  1f/getFloatSetting("trackRes")-1f);
    	viewManager.setItem("LineWidth",   10f*getFloatSetting("lineWidth")-10f);

    	// Don't show the "Save/Delete Event" button(s) until an event is loaded
    	viewManager.hideItem("eventSave");
    	viewManager.hideItem("eventDelete");

    	// Are we restoring a view?
    	if ( this.listState != null ) {
    		viewManager.restoreGroupState(this.listState);
    		listState = null;
    	}
    	if ( this.itemState != null ) {
    		viewManager.restoreItemState(itemState);
    		itemState = null;
    	}
    	return;
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);

    	this.listState = viewManager.getGroupState();
    	outState.putIntegerArrayList("listState", listState);

    	this.itemState = viewManager.getItemState();
    	outState.putIntegerArrayList("itemState", itemState);
    }    
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
   	 	super.onActivityCreated(savedInstanceState);

   	 	if ( savedInstanceState != null ) {
   	 		if ( savedInstanceState.containsKey("listState") )
   	 			this.listState = savedInstanceState.getIntegerArrayList("listState");
   	 	if ( savedInstanceState.containsKey("itemState") )
	 			this.itemState = savedInstanceState.getIntegerArrayList("itemState");
   	 	}
    }

    // Accessor functions into the main routine to grab settings values
    private int getIntSetting( String key ) {
    	return settings.getIntSetting(key);
    }
    
    private boolean getBooleanSetting( String key ) {
    	return settings.getBooleanSetting(key);
    }
    
    private float getFloatSetting( String key ) {
    	return settings.getFloatSetting(key);
    }
    /*********************************************************************/
    
    public void disableListGroup( int group ) {
    	if ( this.viewManager == null )
    		return;

    	this.viewManager.disableGroup(group);
    }
    
    public void enableListGroup( int group ) {
    	if ( this.viewManager == null )
    		return;

    	this.viewManager.enableGroup(group);
    }
    
    public void disableListGroup( String group ) {
    	if ( this.viewManager == null )
    		return;

    	this.viewManager.disableGroup(group);
    }
    
    public void enableListGroup( String group ) {
    	if ( this.viewManager == null )
    		return;

    	this.viewManager.enableGroup(group);
    }

    public void hideGroup( int group ) {
    	if ( this.viewManager == null )
    		return;

    	this.viewManager.hideGroup(group);
    	return;
    }
    
    public void unHideGroup( int group ) {
    	if ( this.viewManager == null )
    		return;

    	this.viewManager.unHideGroup(group);
    	return;
    }

    public void hideGroup( String group ) {
    	if ( this.viewManager == null )
    		return;

    	this.viewManager.hideGroup(group);
    	return;
    }
    
    public void unHideGroup( String group ) {
    	if ( this.viewManager == null )
    		return;

    	this.viewManager.unHideGroup(group);
    	return;
    }
    
    public void enableItem( String itemName ) {
    	if ( this.viewManager == null )
    		return;

    	this.viewManager.enableItem(itemName);
    	return;
    }
    
    public void disableItem( String itemName ) {
    	if ( this.viewManager == null )
    		return;

    	this.viewManager.disableItem(itemName);
    	return;
    }

    public void enableItem( int groupPosition, int childPosition ) {
    	if ( this.viewManager == null )
    		return;

    	this.viewManager.enableItem(groupPosition, childPosition);
    	return;
    }
    
    public void disableItem( int groupPosition, int childPosition ) {
    	if ( this.viewManager == null )
    		return;

    	this.viewManager.disableItem(groupPosition, childPosition);
    	return;
    }

    public void unHideItem( String itemName ) {
    	if ( this.viewManager == null )
    		return;

    	this.viewManager.unHideItem(itemName);
    	return;
    }
    
    public void hideItem( String itemName ) {
    	if ( this.viewManager == null )
    		return;

    	this.viewManager.hideItem(itemName);
    	return;
    }

    public void unHideItem( int groupPosition, int childPosition ) {
    	if ( this.viewManager == null )
    		return;

      	this.viewManager.unHideItem(groupPosition, childPosition);
    	return;
    }

    public void hideItem( int groupPosition, int childPosition ) {
    	if ( this.viewManager == null )
    		return;

    	this.viewManager.hideItem(groupPosition, childPosition);
    	return;
    }
 
    public int getWidgetValue( String widgetName, String type ) {
    	if ( this.viewManager == null )
    		return -1;

    	return this.viewManager.getWidgetValue(widgetName, type);
    }
    
    public boolean getWidgetState( String widgetName, String type ) {
    	if ( this.viewManager == null )
    		return false;

    	return this.viewManager.getWidgetState(widgetName, type);
    }
    
    public void setWidgetValue( String widgetName, String type, int value ) {
    	if ( this.viewManager == null )
    		return;

    	this.viewManager.setWidgetValue(widgetName, type, value);
    	return;
    }
    
    public void setWidgetState( String widgetName, String type, boolean state ) {
    	if ( this.viewManager == null )
    		return;

    	this.viewManager.setWidgetState(widgetName, type, state);
    	return;
    }
        
    public void setItem( String widgetName, int value, boolean state ) {
    	if ( this.viewManager == null )
    		return;

    	this.viewManager.setItem(widgetName, value, state);
    	return;
    }

    public void setItem( String widgetName, int value ) {
    	if ( this.viewManager == null )
    		return;

    	this.viewManager.setItem(widgetName, value);
    	return;
    }

    public void setItem( String widgetName, float value ) {
    	if ( this.viewManager == null )
    		return;

    	this.viewManager.setItem(widgetName, (int)value);
    	return;
    }

    public void setItem( String widgetName, boolean state ) {
    	if ( this.viewManager == null )
    		return;

    	this.viewManager.setItem(widgetName, state);
    	return;
    }
}
