package ch.cern.cms.data_browser.getData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import ch.cern.cms.data_browser.Logger;
import ch.cern.cms.data_browser.R;
import ch.cern.cms.data_browser.Settings;

public class publicDataDB extends SQLiteOpenHelper {
	
	private static publicDataDB instance;
	
	private static final String dbName = "cmsPublicData";
	private static final String tblName = "Events";
	
	private static final String [] colNames = { "run", "event", "type", "tag",  "date", "time", "isData", "data" };
	private static final String [] colTypes = { "INT", "INT",   "TEXT", "TEXT", "TEXT", "TEXT", "INT",    "TEXT" };

	private String columns;
	private int index = -1;
	private int maxStoredEvents = 5;
	
	private int runNum = -1;
	private int evtNum = -1;
	
	private Settings settings;
	private Logger Log;
	
	private Context mContext;
	
	public static synchronized publicDataDB getInstance( Context context ) {
		if ( instance == null )
			instance = new publicDataDB(context);
		return instance;
	}
	
	// Essential "helper" functions for making, updating, and whacking the DB
	private publicDataDB(Context context) {
		super(context, dbName, null, 1);
		
		settings = Settings.getInstance();
		Log = Logger.getInstance();
		
		maxStoredEvents = settings.getIntSetting("maxEvents");
		if ( settings.getIntSetting("debugLevel") > 0 ) Log.w("publicDataDB()", "maxStoredEvents = "+maxStoredEvents);
		
		// Try and open the DB, just to force the onCreate() or onUpgrade() to be called here instead of later
		SQLiteDatabase db=this.getWritableDatabase();
		db.close();

		if ( settings.getIntSetting("debugLevel") > 0 ) Log.w("publicDataDB()", "new called");
		
		columns = " (";
		for ( int i=0; i<colNames.length; i++ ) {
			columns += colNames[i];
			
			if ( i < colNames.length-1 )
				columns += ", ";
			else
				columns += ") ";
		}

		mContext = context;
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		if ( settings.getIntSetting("debugLevel") > 0 ) Log.w("publicDataDB().onCreate()", "called");
		
		String query = "CREATE TABLE IF NOT EXISTS " + publicDataDB.tblName + "(";
		
		for ( int i=0; i<colNames.length; i++ ) {
			query += colNames[i] + " " + colTypes[i];
			
			if ( i < colNames.length-1 )
				query += ", ";
			else
				query += ");";
		}
		
		if ( settings.getIntSetting("debugLevel") > 0 ) Log.w("onCreate()", query);
		db.execSQL(query);
		
		return;
	}
	
	@Override 
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if ( settings.getIntSetting("debugLevel") > 0 ) Log.w("publicDataDB().onUpgrade()", "called");
		db.execSQL("DROP TABLE IF EXISTS "+publicDataDB.tblName);
		onCreate(db);
		return;
	}
	
	public boolean deleteDB() {
		if ( settings.getIntSetting("debugLevel") > 0 ) Log.w("publicDataDB().deleteDB()", "called");
		
		SQLiteDatabase db=this.getWritableDatabase();
		db.execSQL("DROP TABLE IF EXISTS "+publicDataDB.tblName);
		//return SQLiteDatabase.deleteDatabase(new File(db.getPath())); // Only works in API 16+
		return mContext.deleteDatabase( dbName );                      // Older version for API 14+
	}

	/************************** DB Query/Maintainance functions **************************/
	// Simple general query with no returned data;
	private void queryDB( String query ) {
		SQLiteDatabase db=this.getWritableDatabase();
		db.execSQL(query);
		return;
	}
	
	/*****
	private boolean insert(ContentValues cv) {
		// Get a handle to the active DB
		if ( cv == null )
			return false;
		
		SQLiteDatabase db=this.getWritableDatabase();
		long retVal = db.insert( publicDataDB.tblName, null, cv);
		db.close();
		
		return (retVal > -1);
	}
	*****/

	private boolean delete(String run, String event) {
		
		if ( settings.getIntSetting("debugLevel") > 0 ) Log.w("delete()", "Deleting "+run+" "+event+" from "+publicDataDB.tblName);

		SQLiteDatabase db=this.getWritableDatabase();
		if ( db == null )
			return true;
		String[] args = new String[] { run, event };
		
		int numDelete = db.delete(publicDataDB.tblName,"run=? and event=?", args);
		if ( settings.getIntSetting("debugLevel") > 0 ) Log.w("delete()", numDelete+" rows deleted");
		
		db.close();

		return false;
	}

	private void delete ( int run, int event ) {
		if ( settings.getIntSetting("debugLevel") > 0 ) Log.w("delete()", "Deleting "+run+" "+event+" from "+publicDataDB.tblName);

		SQLiteDatabase db=this.getWritableDatabase();
		if ( db == null )
			return;
		String[] args = new String[] {};
		
		int numDelete = db.delete(publicDataDB.tblName,"run="+run+" and event="+event, args);
		if ( settings.getIntSetting("debugLevel") > 0 ) Log.w("delete()", numDelete+" rows deleted");
		
		db.close();
		
		return;
	}
	
	// If we're saving a new event, do a round-robin purge to maintain no more than maxEvents stored
	private void purgeDB() {
		
		int stored = getEventCount();
		if ( settings.getIntSetting("debugLevel") > 0 ) Log.w("purgeDB()", stored+" events stored on device");
		
		if (stored >= this.maxStoredEvents) {
			SQLiteDatabase db=this.getWritableDatabase();
			if ( db != null ) {
				
				String[] args = new String[] {};
				String query = "SELECT run,event FROM "+publicDataDB.tblName;
				Cursor cursor = db.rawQuery(query, args);

				int i = 0;
				
				if ( cursor.moveToFirst() ) {
					while ( i < stored - this.maxStoredEvents + 1 ) {
						delete(cursor.getString(0), cursor.getString(1));
						if ( !cursor.moveToNext() )
							break;
						i++;
					}
				}
				
				cursor.close();
				db.close();
					
			} // End if ( db != null )
		} // End if (getEventCount() > publicDataDB.maxStoredEvents)
		return;
	}
	
	/***************************** Event DB Functions ************************************/	
	// Get a list of the runs stored in the local DB
	public List<String> getRunList() {
		if ( settings.getIntSetting("debugLevel") > 0 ) Log.w("getRunList()", "called");
		
		SQLiteDatabase db=this.getWritableDatabase();
		if ( db == null )
			return null;
		
		String[] args = new String[] {};

		Cursor cursor = db.rawQuery("SELECT DISTINCT run FROM "+publicDataDB.tblName+" where isData>0", args);
		if ( cursor.getCount() <= 0 )
			return null;

		List<String> runList = new ArrayList<String>();
	
		if ( cursor.moveToFirst() ) {
			runList.add(cursor.getString(0));
			while ( cursor.moveToNext() ) {
				runList.add(cursor.getString(0));
			}
		}
				
		if ( runList.size() > 1 )
			runList.add(0, mContext.getString(R.string.run_prompt));
		
		return runList;
	}

	// Get a list of the events for a particular run number stored in the local DB
	public List<String> getEventList( String run ) {
		if ( settings.getIntSetting("debugLevel") > 0 ) Log.w("getEventList()", "called");
		
		SQLiteDatabase db=this.getWritableDatabase();
		if ( db == null )
			return null;
		
		String[] args = new String[] {run};

		Cursor cursor = db.rawQuery("SELECT event FROM "+publicDataDB.tblName+" WHERE run=?", args);
		if ( cursor.getCount() <= 0 )
			return null;

		List<String> evtList = new ArrayList<String>();
	
		if ( cursor.moveToFirst() ) {
			evtList.add(cursor.getString(0));
			while ( cursor.moveToNext() ) {
				evtList.add(cursor.getString(0));
			}
		}

		if ( evtList.size() > 1 )
			evtList.add(0,mContext.getString(R.string.event_prompt));
		
		return evtList;
	}
	
	public void deleteStoredEvent() {
		String run = ""+this.runNum;
		String evt = ""+this.evtNum;
		
		if ( checkEventStored(run, evt) )
			delete(this.runNum, this.evtNum);
		else
			Log.e("deleteStoredEvent()()", "Run "+run+" Event "+evt+" not stored on device");
		
		return;
	}
	
	public void addEventToStore(String...evtValues) {
		
		if ( checkEventStored(evtValues[0], evtValues[1]) ) {
			Log.e("addEventToStore()", "Run "+evtValues[0]+" Event "+evtValues[1]+" already stored on device");
			return;
		}
		
		if ( settings.getIntSetting("debugLevel") > 0 ) Log.w("addEventToStore()", "called with "+evtValues);

		String values = "(" + evtValues[0] + ", " + evtValues[1] + ", '" + evtValues[2] + "', '" + evtValues[3] + 
				"', '" + evtValues[4] + "', '" + evtValues[5] + "', 1, '" + evtValues[6] + "')";

		addToStore(values);
		return;
	}
	
	// Check and see if this event is already saved
	private boolean checkEventStored( String run, String event ) {
		if ( settings.getIntSetting("debugLevel") > 0 ) Log.w("checkEventStore()", "Checking for 'run="+run+" and event="+event+"' in the DB");

		// Always return false (not there) for simulations
		if ( run.equals("-1") || event.equals("-1") )
			return false;
		
		boolean isThere = false;
		SQLiteDatabase db=this.getWritableDatabase();
		
		if ( db == null )
			return false;
		
		String[] args = new String[] { run, event };

		Cursor cursor = db.rawQuery("SELECT type FROM "+publicDataDB.tblName+" WHERE run=? and event=?", args);
		if ( cursor.getCount() > 0 )
			isThere = true;
		else if ( settings.getIntSetting("debugLevel") > 0 ) Log.w("checkEventStored()", "No results from query ");

		cursor.close();
		db.close();
	
		return isThere;
	}
	
	/******************************* Sim DB Functions ************************************/	
	// Get a list of the locally stored simulations
	public List<String> getSimList() {
		if ( settings.getIntSetting("debugLevel") > 0 ) Log.w("getSimList()", "called");
		
		SQLiteDatabase db=this.getWritableDatabase();
		if ( db == null )
			return null;

		String[] args = new String[] {};

		Cursor cursor = db.rawQuery("SELECT type FROM "+publicDataDB.tblName+" WHERE isData='0'", args);
		if ( cursor.getCount() <= 0 )
			return null;

		List<String> simList = new ArrayList<String>();

		if ( cursor.moveToFirst() ) {
			simList.add(cursor.getString(0));
			while ( cursor.moveToNext() ) {
				simList.add(cursor.getString(0));
			}
		}

		if ( simList.size() > 1 )
			simList.add(0, mContext.getString(R.string.channel_prompt));

		return simList;
	}

	// In case of multiple instances of the same event tag, 
	// set the index to the one we'll want (1st, 2nd, ...)
	public void setSimIndex(int index) {
		this.index = index;
		return;
	}
	
	// Add a new simulated event to the local database
	public void addSimToStore(String ...simValues) {
		
		// Make the string for storage in the database
		String values = "";

		// Get or create run & event numbers for this event
		runNum = getRunNumber(simValues[0]);
		evtNum = getEventNumber(runNum);
		
		// Build the SQLite insert command
		values = "("+runNum+", "+evtNum+", '"+simValues[0]+"', '"+simValues[1]+"', '"+simValues[2]+"', '"+simValues[3]+"', 0, '"+simValues[4]+"')";
		
		// Inject it into the DB
		addToStore(values);
		
		return;
	}
	
	// Remove a currently stored simulation from the local database
	public void deleteStoredSim() {
		delete(this.runNum, this.evtNum);
		return;
	}

	/*
	* Index simulations in the DB by run/event even though those don't
	* really exist for these sims. In the next two functions, if the 
	* sim type already exists, add a new event to the existing run number
	* otherwise create a new run number (+1 to the largest existing 
	* number) and event number
	*/
	// Given a run number, find any sim events associated with it
	private int getEventNumber( int run ) {
		SQLiteDatabase db=this.getWritableDatabase();
		if ( db == null )
			return 0;

		String[] args = new String[] {};

		// Check and see how many events are associated with this run number
		Cursor cursor = db.rawQuery("SELECT event FROM "+publicDataDB.tblName+" WHERE isData=0 and run='"+run+"' ORDER BY event DESC", args);

		// If there are events stored under this run, increment and return
		if ( cursor.getCount() > 0 ) {
			if ( cursor.moveToFirst() )
				return cursor.getInt(0) + 1;
		}
		return 1;
	}
	
	// Given a sim type, find a run number associated with it
	private int getRunNumber(String type) {
		SQLiteDatabase db=this.getWritableDatabase();
		if ( db == null )
			return 0;
		String[] args = new String[] { };

		// Check and see if there are any other simulations in the DB with this same tag.
		Cursor cursor = db.rawQuery("SELECT run FROM "+publicDataDB.tblName+" WHERE isData=0 and type='"+type+"'", args);

		// If this tag is already there, return the existing run number
		if ( cursor.moveToFirst() )
			return cursor.getInt(0);
		
		// If not, get all the existing run numbers and increment it
		cursor = db.rawQuery("SELECT run FROM "+publicDataDB.tblName+" WHERE isData=0 ORDER BY run DESC", args);
		
		if ( cursor.getCount() > 0 ) {
			if ( cursor.moveToFirst() ) {
				int run = cursor.getInt(0) + 1;
				
				if (run < 1000)
					run = 1000;
				
				return run;
			}
		}
		
		return 1000;
		
	}

	/******************************* Common event Functions *****************************/
	// Get a count of the number of events & sims stored on this device
	private int getEventCount() {
		if ( settings.getIntSetting("debugLevel") > 0 ) Log.w("getEventCount()", "called");

		SQLiteDatabase db=this.getWritableDatabase();
		if ( db == null )
			return -1;

		String[] args = new String[] {};
		String query = "SELECT event FROM "+publicDataDB.tblName;
		Cursor cursor = db.rawQuery(query, args);

		int events = cursor.getCount();
		db.close();
		
		return events;

	}
	
	// Get the particle data as a List<String>
	public List<String> getDataList( String filter ) {
		
		if ( settings.getIntSetting("debugLevel") > 0 ) Log.w("getDataList()", "called with filter="+filter+"!!!");
		
		if ( filter == null ) {
			Log.e("getDataList()", "Null filter! Bailing");
			return null;
		}
		
		SQLiteDatabase db=this.getWritableDatabase();
		if ( db == null )
			return null;
		
		String[] args = new String[] {};
		String query = "SELECT tag,data,run,event FROM "+publicDataDB.tblName+" WHERE "+filter;
		Cursor cursor = db.rawQuery(query, args);

		if ( cursor.getCount() <= 0 ) {
			if ( settings.getIntSetting("debugLevel") > 0 ) Log.w("getDataList()", "No results from database query");
			return null;
		}
		
		if ( settings.getIntSetting("debugLevel") > 0 ) Log.w("getDataList()", "Found "+cursor.getCount()+" items, getting item "+index);
		
		boolean moveSucceeded = false;
		if ( index != -1 ) {
			moveSucceeded = cursor.moveToPosition(index);
			index = -1;
		} else
			moveSucceeded = cursor.moveToFirst();
		
		if ( !moveSucceeded )
			return null;
		
		// Save the run/event numbers of the current event
		this.runNum = cursor.getInt(2);
		this.evtNum = cursor.getInt(3);
		
		if ( settings.getIntSetting("debugLevel") > 0 ) Log.w("getDataList()", "Retreiving event "+evtNum+" from run "+runNum);

		List<String> list = new ArrayList<String>();
		if ( filter.substring(0, 8).equals("isData=0") )
			list.add("simulation");
		else
			list.add("data");
		
		list.add("success");
		list.add(cursor.getString(0));
		
		List<String> particles = Arrays.asList(cursor.getString(1).split("\n"));
		list.addAll(particles);
		
		return list;
	}
	
	// Add an event to the local store
	private void addToStore(String values) {

		purgeDB();
		try {
			queryDB("INSERT INTO "+publicDataDB.tblName+columns+"VALUES "+values);
			if ( settings.getIntSetting("debugLevel") > 0 ) Log.w("addEvent()", "Event stored: "+values);
		} catch ( Exception e ) {
			Log.e("addEvent()", "Failed to store event in DB. "+e.toString());
		}

		return;
	}
	
	// Simple search to see if we've stored events or simulations in the local DB
	public boolean hasLocal(boolean isData) {
		
		SQLiteDatabase db=this.getWritableDatabase();
		if ( db == null )
			return false;
		
		String[] args = new String[] {};

		Cursor cursor;
		if ( isData )
			cursor = db.rawQuery("SELECT type FROM "+publicDataDB.tblName+" WHERE isData>0", args);
		else
			cursor = db.rawQuery("SELECT type FROM "+publicDataDB.tblName+" WHERE isData=0", args);

		if ( settings.getIntSetting("debugLevel") > 0 ) Log.w("hasLocal()", "found "+cursor.getCount()+" events with isData="+isData);

        return cursor.getCount() > 0;

    }
		
	// Clear the database of events or sims
	public void clearDB(boolean isData) {
		SQLiteDatabase db=this.getWritableDatabase();
		if ( db == null )
			return;
		String[] args = new String[] {};
		
		int numDelete = 0;
		
		if ( isData )
			numDelete = db.delete(publicDataDB.tblName, "isData>0", args);
		else
			numDelete = db.delete(publicDataDB.tblName, "isData=0", args);

		if ( settings.getIntSetting("debugLevel") > 0 ) Log.w("delete()", numDelete+" rows deleted");
		
		db.close();
		
		return;
	}
	
} // End of publicDataDB class
