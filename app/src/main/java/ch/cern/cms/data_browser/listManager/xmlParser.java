package ch.cern.cms.data_browser.listManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;
import android.content.res.XmlResourceParser;
import ch.cern.cms.data_browser.Logger;

public class xmlParser {

	private ArrayList<ExpandableList> lists;
	private Logger Log;
	
	// Reads & processes a menu definition XML file from res/xml
    public xmlParser(Context context, int xmlID) throws XmlPullParserException, IOException {
		XmlResourceParser parser = context.getResources().getXml(xmlID);
        lists = new ArrayList<ExpandableList>();
        Log = Logger.getInstance();
        parseXML(parser);
    }
    
	// Reads & processes a menu definition XML file from assests/
    public xmlParser(Context context, String asset) throws XmlPullParserException, IOException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        
        XmlPullParser parser = factory.newPullParser();
	    InputStream in_s = context.getAssets().open(asset);
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        parser.setInput(in_s, null);

        lists = new ArrayList<ExpandableList>();
        parseXML(parser);
        Log = Logger.getInstance();

    }

    public ArrayList<ExpandableList> getListArray() {
    	return this.lists;
    }
    
    public ExpandableList getList(String listName) {
    	
    	ExpandableList list;
    	Iterator<ExpandableList> it = lists.iterator();
    	while ( it.hasNext() ) {
    		list = it.next();
    		if ( list.name.equalsIgnoreCase(listName) )
    			return list;
    	}
    	return null;
    }
    
    private void parseXML(XmlPullParser parser) throws XmlPullParserException,IOException {

    	int eventType = parser.getEventType();
    	
    	ExpandableList list = new ExpandableList();
    	Group group = new Group();
    	Item item = new Item();
    	Widget widget = new Widget();
    	String name;
    	
    	while ( eventType != XmlPullParser.END_DOCUMENT ) {
        	switch ( eventType ) {

        	case XmlPullParser.START_DOCUMENT:
        		break;
        		
            case XmlPullParser.START_TAG:
            	
            	name = parser.getName();
            	
            	if ( name.equalsIgnoreCase("list") ) {                    // Initializers... XML tags indicate
            		list = new ExpandableList();                          // when a new arraylist is about to be necessary
            	} else if ( name.equalsIgnoreCase("groups") ) {          // or when a new instance is about to be necessary
            		list.groups = new ArrayList<Group>();
            	} else if ( name.equalsIgnoreCase("group") ) {
            		group = new Group();
            	} else if ( name.equalsIgnoreCase("items") ) {
            		group.items = new ArrayList<Item>();
            	} else if ( name.equalsIgnoreCase("item") ) {
            		item = new Item();
            	} else if ( name.equalsIgnoreCase("widgets") ) {
            		item.widgets = new ArrayList<Widget>();
            	} else if ( name.equalsIgnoreCase("widget") ) {
            		widget = new Widget();                                // The meat. Fill in the parameters of the various 
            		                                                      // pieces we'll need to build the visuals
            	
            	} else if ( name.equalsIgnoreCase("name") ) {             // list name
            		list.name = parser.nextText();
            		
            	} else if ( name.equalsIgnoreCase("listLayout") ) {       // list layout resource
            		list.layout = parser.nextText();
            		
            	} else if ( name.equalsIgnoreCase("title") ) {            // group title
            		group.title = parser.nextText();
            		
            	} else if ( name.equalsIgnoreCase("groupEnabled") ) {     // Boolean enabled switch
            		group.enabled = Boolean.parseBoolean(parser.nextText());
            		
            	} else if ( name.equalsIgnoreCase("groupLayout") ) {      // group layout resource
            		group.xmlLayout = parser.nextText();
            		
            	} else if ( name.equalsIgnoreCase("textViewID") ) {       // Header label resource (in the layout xml)
            		group.textviewID = parser.nextText();
            		
            	} else if ( name.equalsIgnoreCase("iconID") ) {           // Icon resource (in the layout xml)
            		group.iconID = parser.nextText();
            		
            	} else if ( name.equalsIgnoreCase("label") ) {            // item label
            		item.label = parser.nextText();
            		
            	} else if ( name.equalsIgnoreCase("itemLayout") ) {       // Item layout resource
                	item.xmlLayout = parser.nextText();
                	
            	} else if ( name.equalsIgnoreCase("itemEnabled") ) {      // Boolean enabled switch
            		item.enabled = Boolean.parseBoolean(parser.nextText());
            		
            	} else if ( name.equalsIgnoreCase("selectable") ) {       // Boolean is it selectable?
            		item.selectable = Boolean.parseBoolean(parser.nextText());
            		
            	} else if (name.equalsIgnoreCase("itemName") ) {          // Item name
            		item.name = parser.nextText();
            		
            	} else if ( name.equalsIgnoreCase("type") )  {            // widget type
            		widget.type = parser.nextText();

            	} else if ( name.equalsIgnoreCase("text") ) {             // widget text
            		widget.text = parser.nextText();
            		
            	} else if ( name.equalsIgnoreCase("value") )  {           // widget integer value
            		widget.value = Integer.parseInt(parser.nextText());

            	} else if ( name.equalsIgnoreCase("minimum") ) {          // minimum widget integer value
            		widget.minimum = Integer.parseInt(parser.nextText());

            	} else if ( name.equalsIgnoreCase("maximum") ) {          // maximum widget integer value
            		widget.maximum = Integer.parseInt(parser.nextText());

            	} else if ( name.equalsIgnoreCase("state") ) {            // widget boolean state
            		widget.state = Boolean.parseBoolean(parser.nextText());

            	} else if ( name.equalsIgnoreCase("widgetID") ) {        // widgets resource ID within Item layout resource
            		widget.ID = parser.nextText();
            		
            	} 
            	break;
            	
            case XmlPullParser.END_TAG:
            	name = parser.getName();

            	if ( name.equalsIgnoreCase("widget") ) {
            		item.widgets.add(widget);
            		
            	} else if ( name.equalsIgnoreCase("item") ) {
            		group.items.add( item );
            		
            	} else if ( name.equalsIgnoreCase("group") ) {
            		list.groups.add( group );
            		
            	} else if ( name.equalsIgnoreCase("list") ) {
            		lists.add(list);
            	}
            	
            	break;
            	
            }
            eventType = parser.next();
        }
    	
    	return;
    }

    public void dumpLists(ArrayList<ExpandableList>lists) {
    	
    	Iterator<ExpandableList> itl = lists.iterator();
    	while ( itl.hasNext() ) {
    		
    		ExpandableList list = itl.next();
    		Log.w("dumpLists()", "Found list "+list.name);
    		
    		Iterator<Group> itg = list.groups.iterator();
    		while ( itg.hasNext() ) {
    			
    			Group group = itg.next();
    			Log.w("dumpLists()", "Found group "+group.title+" in list "+list.name);
    			
    			Iterator<Item> iti = group.items.iterator();
    			while ( iti.hasNext() ) {
    				
    				Item item = iti.next();
    				Log.w("dumpLists()", "Found item "+item.label+" in group "+group.title+" on list "+list.name);
    				
    				Iterator<Widget> itw = item.widgets.iterator();
    				while ( itw.hasNext() ) {
    					
    					Widget widget = itw.next();
        				Log.w("dumpLists()", "Found widget "+widget.text+" of type "+widget.type+" in item "+item.label+" in group "+group.title+" on list "+list.name);
 
    				} // End while ( itw.hasNext() )
    			} // End while ( iti.hasNext() )
    		} // End while ( itg.hasNext() )
    	} // End while ( itl.hasNext() )
    	
    	return;
    }
}
