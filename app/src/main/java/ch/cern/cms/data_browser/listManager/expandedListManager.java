package ch.cern.cms.data_browser.listManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupExpandListener;
import ch.cern.cms.data_browser.Logger;
import ch.cern.cms.data_browser.R;

public class expandedListManager {

	private ExpandableListAdapter listAdapter;
	private ExpandableListView expListView;
	private List<Group> listDataGroup;
	private HashMap<String, List<Item>> listDataChild;
	private ExpandableList list;
	private Context context;
	private Logger Log;
	private String listName;

	public expandedListManager(ExpandableListView view, Context context, String list, String xmlFile) {

		this.context = context;
		this.listName = list;
		this.Log = Logger.getInstance();
		
    	// get the listview
    	expListView = view;

    	listDataGroup = new ArrayList<Group>();
        listDataChild = new HashMap<String, List<Item>>();

        // Prepare the list items from the list.xml asset
        prepareLists(context);
            
        if ( listDataGroup == null ) {
    		Log.e("expandedListManager()", "listDataHeader is NULL");
    		return;
    	}
    	if ( listDataChild == null ) {
    		Log.e("expandedListManager()", "listDataChild is NULL");
    		return;
    	}
    	listAdapter = new ExpandableListAdapter(this.context, listDataGroup, listDataChild);
    	
    	if ( listAdapter == null ) {
    		Log.e("expandedListManager()", "listAdapter is NULL");
    		return;
    	}

    	// setting list adapter
    	expListView.setAdapter(listAdapter);

    	// Inhibit expansion of disabled groups
    	expListView.setOnGroupExpandListener(new OnGroupExpandListener() {
    		@Override
    		public void onGroupExpand( int groupPosition ) {
    			if ( !listAdapter.isEnabled(groupPosition) )
    				expListView.collapseGroup(groupPosition);
    			
    			return;
    		}
    		
    	});

    	// Set the listener for the text boxes
    	expListView.setOnChildClickListener(clickListener);
    	
    	return;

    }
    
	/***************** Handlers for hiding/disabling groups *************/
	public void enableGroup( int groupPosition ) {
    	listAdapter.enableGroup(groupPosition, true);
    	return;
    }
    
    public void disableGroup( int groupPosition ) {
    	listAdapter.enableGroup(groupPosition, false);
    	return;
    }
    
    public void enableGroup( String groupName ) {
    	listAdapter.enableGroup(groupName, true);
    	return;
    }
    
    public void disableGroup( String groupName ) {
		listAdapter.enableGroup(groupName, false);
		return;
    }
    
    public void unHideGroup( int groupPosition ) {
    	unHideGroup( list.groups.get(groupPosition).title );
    	return;
    }
    
    public void hideGroup( int groupPosition ) {
    	hideGroup( list.groups.get(groupPosition).title );
    	return;
    }
    
    public void unHideGroup( String groupName ) {

    	Iterator<Group> itg = list.groups.iterator();
    	while ( itg.hasNext() ) {
    			
    		Group group = itg.next();
    		if ( group.title.equalsIgnoreCase(groupName) ) {
    			group.hidden = false;
    			break;
    		}
    	}
    	remakeList();
    	
	return;
    }

    public void hideGroup( String groupName ) {
    	Iterator<Group> itg = list.groups.iterator();
    	while ( itg.hasNext() ) {
    			
    		Group group = itg.next();
    		if ( group.title.equalsIgnoreCase(groupName) ) {
    			group.hidden = true;
    			break;
    		}
    	}
    	
    	remakeList();
    	
    	return;
    }
	/********************************************************************/
    
    public int getNumberOfGroups() {
    	return listDataGroup.size();
    }
    
    public ArrayList<Integer> getGroupState() {
    	ArrayList<Integer> state = new ArrayList<Integer>();
    	
    	for (int i=0; i<listDataGroup.size(); i++) {
    		if ( listDataGroup.get(i).expanded )
    			state.add(1);
    		else if ( !listDataGroup.get(i).enabled )
    			state.add(2);
    		else if ( listDataGroup.get(i).hidden )
    			state.add(4);
    		else 
    			state.add(0);
    	}
    	
    	return state;
    }
    
    public void restoreGroupState( ArrayList<Integer> listState) {
    	if ( listDataGroup != null ) {
    		
    		if ( listState.size() != listDataGroup.size() ) {
    			Log.e("restoreGroupState()", "Unmatched sizes: listState = "+listState.size()+", listDataGroup = "+listDataGroup.size());
    			return;
    		}
    		
    		for ( int i=0; i<listDataGroup.size(); i++ ) {
    			int state = listState.get(i);
    			if ( state == 1 ) {
    				expListView.expandGroup(i);
    			} else if ( state == 2 ) {
    				//Log.w("restoreGroupState()", "Group "+listDataGroup.get(i).title+" was disabled");
    				this.disableGroup(i);
    			} else if ( state == 4 ) {
    				//Log.w("restoreGroupState()", "Group "+listDataGroup.get(i).title+" was hidden");
    				this.hideGroup(i);
    			}
    		}

    	} else
    		Log.e("restoreGroupState()", "listDataGroup is null! Unable to restore the list");

    	return;    	
    }
    
    public ArrayList<Integer> getItemState() {
    	ArrayList<Integer> state = new ArrayList<Integer>();
    	
    	Iterator<Group> itg = list.groups.iterator();
    	while ( itg.hasNext() ) {
    		Group group = itg.next();
    		
    		Iterator<Item> iti = group.items.iterator();
    		while ( iti.hasNext() ) {
    			Item item = iti.next();
    			
    			if ( item.enabled )
    				state.add(1);
    			else
    				state.add(0);
    			
    		}
    	}
    	
    	return state;
    }
    
    public void restoreItemState(ArrayList<Integer>itemState) {
    	
    	if ( itemState == null ) {	
    		Log.e("restoreItemState()", "itemState is null! Cannot restore");
    		return;
    	}
    	
    	Iterator<Group> itg = list.groups.iterator();
    	int counter = 0;
    	while ( itg.hasNext() ) {
    		Group group = itg.next();
    		
    		Iterator<Item> iti = group.items.iterator();
    		while ( iti.hasNext() ) {
    			Item item = iti.next();
    			item.enabled = itemState.get(counter++) == 1;
    			//Log.w("restoreItemState()", "item "+item.name+" state = "+item.enabled);
    		}
    	}
    	this.remakeList();
    	return;
    }
    
	/***************** Handlers for hiding/disabling items **************/
    public void enableItem( int groupPosition, int childPosition ) {
		Item item = ((Item)listAdapter.getChild(groupPosition, childPosition));
		item.enabled = true;
		listAdapter.notifyDataSetChanged();
		return;
    }
    
    public void disableItem( int groupPosition, int childPosition ) {
		Item item = ((Item)listAdapter.getChild(groupPosition, childPosition));
		item.enabled = false;
		listAdapter.notifyDataSetChanged();
		return;
    }

    public void enableItem( String itemName ) {
    	Iterator<Group> itg = list.groups.iterator();
    	while ( itg.hasNext() ) {
    			
    		Group group = itg.next();
    		Iterator<Item> iti = group.items.iterator();
    		while ( iti.hasNext() ) {
    			Item item = iti.next();
    			if ( item.name.equalsIgnoreCase(itemName) ) {
    				item.enabled = true;
    			}
    		}
    	}
    	listAdapter.notifyDataSetChanged();
    	return;
    }

    public void disableItem( String itemName ) {
    	Iterator<Group> itg = list.groups.iterator();
    	while ( itg.hasNext() ) {
    			
    		Group group = itg.next();
    		Iterator<Item> iti = group.items.iterator();
    		while ( iti.hasNext() ) {
    			Item item = iti.next();
    			if ( item.name.equalsIgnoreCase(itemName) ) {
    				item.enabled = false;
    			}
    		}
    	}
    	listAdapter.notifyDataSetChanged();
    	return;
    }

    public void unHideItem( String itemName ) {

    	Iterator<Group> itg = list.groups.iterator();
    	while ( itg.hasNext() ) {
    			
    		Group group = itg.next();
    		Iterator<Item> iti = group.items.iterator();
    		while ( iti.hasNext() ) {
    			Item item = iti.next();
    			if ( item.name.equalsIgnoreCase(itemName) ) {
    				item.enabled = true;
    				break;
    			}
    		}
    	}
    	
    	remakeList();

    	return;
    }
    
    public void hideItem( String itemName ) {
    	
    	Iterator<Group> itg = list.groups.iterator();
    	while ( itg.hasNext() ) {
    			
    		Group group = itg.next();
    		Iterator<Item> iti = group.items.iterator();
    		while ( iti.hasNext() ) {
    			Item item = iti.next();
    			if ( item.name.equalsIgnoreCase(itemName) ) {
    				item.enabled = false;
    				break;
    			}
    		}
    	}

    	remakeList();

    	return;    	
    }

    public void unHideItem( int groupPosition, int childPosition ) {
    	Item item = list.groups.get(groupPosition).items.get(childPosition);
    	item.enabled = true;
    	remakeList();
    	return;
    }
    
    public void hideItem( int groupPosition, int childPosition ) {
    	Item item = list.groups.get(groupPosition).items.get(childPosition);
    	item.enabled = false;
    	remakeList();
    	return;
    }
    /********************************************************************/

    /***************** Handlers for setting item values *****************/
    public void setItem( String name, int value, boolean state ) {
    	Iterator<Group> itg = list.groups.iterator();
    	while ( itg.hasNext() ) {
    		Group group = itg.next();
    		Iterator<Item> iti = group.items.iterator();
    		while ( iti.hasNext() ) {
    			Item item = iti.next();
    			
    			if ( item.name.equalsIgnoreCase(name) ) {
    			
    				Iterator<Widget> itw = item.widgets.iterator();
    				while ( itw.hasNext() ) {
    					Widget widget = itw.next();

    					if ( widget.type.equalsIgnoreCase("checkbox") || widget.type.equalsIgnoreCase("toggle") || widget.type.equalsIgnoreCase("switch") ) {
    						widget.state = state;
    					} else if ( widget.type.equalsIgnoreCase("slider") ) {
    						widget.value = value;
    					}
    				}
    			}
    		}
    	}
    	listAdapter.notifyDataSetChanged();
    	return;
    }
    
    public void setItem( String name, int value ) {
    	Iterator<Group> itg = list.groups.iterator();
    	while ( itg.hasNext() ) {
    		Group group = itg.next();
    		Iterator<Item> iti = group.items.iterator();
    		while ( iti.hasNext() ) {
    			Item item = iti.next();
    			
    			if ( item.name.equalsIgnoreCase(name) ) {
    				
    				Iterator<Widget> itw = item.widgets.iterator();
    				while ( itw.hasNext() ) {
    					Widget widget = itw.next();

    					if ( widget.type.equalsIgnoreCase("slider") )
    						widget.value = value;
    					
    				}
    			}
    		}
    	}
    	listAdapter.notifyDataSetChanged();
    	return;
    }

    public void setItem( String name, float value ) {
    	setItem(name, (int)value);
    	return;
    }
    
    public void setItem( String name, boolean state ) {
    	
    	Iterator<Group> itg = list.groups.iterator();
    	while ( itg.hasNext() ) {
    		Group group = itg.next();
    		Iterator<Item> iti = group.items.iterator();
    		while ( iti.hasNext() ) {
    			Item item = iti.next();
    			
    			if ( item.name.equalsIgnoreCase(name) ) {
    			
    				Iterator<Widget> itw = item.widgets.iterator();
    				while ( itw.hasNext() ) {
    					Widget widget = itw.next();

    					if ( widget.type.equalsIgnoreCase("checkbox") || widget.type.equalsIgnoreCase("toggle") || widget.type.equalsIgnoreCase("switch") )
    						widget.state = state;
    					
    				}
    			}
    		}
    	}
    	listAdapter.notifyDataSetChanged();
    	return;
    }
    /********************************************************************/

    /***************** Handlers for getting widget values ***************/
    public int getWidgetValue(String name, String type) {
    	
    	Iterator<Group> itg = list.groups.iterator();
    	while ( itg.hasNext() ) {
    		Group group = itg.next();
    		Iterator<Item> iti = group.items.iterator();
    		while ( iti.hasNext() ) {
    			Item item = iti.next();
    			Iterator<Widget> itw = item.widgets.iterator();
    			while ( itw.hasNext() ) {
    				Widget widget = itw.next();
    				
    				if ( widget.text.equalsIgnoreCase(name) && widget.type.equalsIgnoreCase(type)) {
    					return widget.value;
    				}
    			}
    		}
    	}
    	return -1;
    	
    }
    
    public boolean getWidgetState( String name, String type ) {
    	
    	Iterator<Group> itg = list.groups.iterator();
    	while ( itg.hasNext() ) {
    		Group group = itg.next();
    		Iterator<Item> iti = group.items.iterator();
    		while ( iti.hasNext() ) {
    			Item item = iti.next();
    			Iterator<Widget> itw = item.widgets.iterator();
    			while ( itw.hasNext() ) {
    				Widget widget = itw.next();
    				if ( widget.text.equalsIgnoreCase(name) && widget.type.equalsIgnoreCase(type))
    						return widget.state;
    			}
    		}
    		
    	}
    	return false;

    }
	/********************************************************************/

    /***************** Handlers for setting widget values ***************/
    public void setWidgetValue(String name, String type, int value) {
    	Iterator<Group> itg = list.groups.iterator();
    	while ( itg.hasNext() ) {
    		Group group = itg.next();
    		Iterator<Item> iti = group.items.iterator();
    		while ( iti.hasNext() ) {
    			Item item = iti.next();
    			Iterator<Widget> itw = item.widgets.iterator();
    			while ( itw.hasNext() ) {
    				Widget widget = itw.next();
    				
    				if ( widget.text.equalsIgnoreCase(name) && widget.type.equalsIgnoreCase(type) ) {
    					widget.value = value;
    					break;
    				}
    			}
    		}
    	}
    	listAdapter.notifyDataSetChanged();
    	return;
    }
    
    public void setWidgetState( String name, String type, boolean state ) {
    	Iterator<Group> itg = list.groups.iterator();
    	while ( itg.hasNext() ) {
    		Group group = itg.next();
    		Iterator<Item> iti = group.items.iterator();
    		while ( iti.hasNext() ) {
    			Item item = iti.next();
    			Iterator<Widget> itw = item.widgets.iterator();
    			while ( itw.hasNext() ) {
    				Widget widget = itw.next();
    				
    				if ( widget.text.equalsIgnoreCase(name) && widget.type.equalsIgnoreCase(type) ) {
    					widget.state = state;
    					break;
    				}
    			}
    		}
    	}
    	listAdapter.notifyDataSetChanged();
    	return;
    }
	/********************************************************************/

    public ExpandableListAdapter getListAdapter() {
    	return this.listAdapter;
    }
    
    public ExpandableListView getListView() {
    	return this.expListView;
    }
    
    public OnChildClickListener getChildClickListener() {
    	if ( this.clickListener == null )
    		Log.e("getChildClickListener()", "OnChildClickListener is NULL!");
    	
    	return this.clickListener;
    }

    private void remakeList() {

    	// Flush the old lists
    	listDataGroup = new ArrayList<Group>();
        listDataChild = new HashMap<String, List<Item>>();

        // And remake it from scratch    		
        Iterator<Group> itg = list.groups.iterator();
        while ( itg.hasNext() ) {
    			
        	Group group = itg.next();

        	if ( !group.enabled )
        		continue;
    		
        	group.wasExpanded = group.expanded;
        	listDataGroup.add(group);
    			
        	Iterator<Item> iti = group.items.iterator();
        	List<Item> listItem = new ArrayList<Item>();
        	
        	while ( iti.hasNext() ) {
    				
        		Item item = iti.next();
    				
        		if ( !item.enabled )
        			continue;
    				
        		listItem.add( item );
    				
        	} // End while ( iti.hasNext() ) 
    			
        	listDataChild.put( group.title, listItem );
    			
        } // End while ( itg.hasNext() ) 
    	
    	listAdapter = new ExpandableListAdapter(this.context, listDataGroup, listDataChild);
    	expListView.setAdapter(listAdapter);

    	
    	itg = list.groups.iterator();
    	int groupPos = 0;
        while ( itg.hasNext() ) {
			
        	Group group = itg.next();

        	if ( !group.enabled )
        		continue;

        	if ( group.wasExpanded )
        		expListView.expandGroup(groupPos);
        	groupPos++;
        }
    	
    	return;

    }
    
    private void prepareLists(Context context) {
    	xmlParser xp;
    	try {
    		//xp = new xmlParser(context, this.xmlFileName);
    		xp = new xmlParser(context, R.xml.controls);
    		list = xp.getList(this.listName);
    		
    	} catch(Exception e) {
    		Log.e("onCreate()", "xml parse failed: "+e.toString());
    		return;
    	}

    	Iterator<Group> itg = list.groups.iterator();
    	while ( itg.hasNext() ) {
    			
    		Group group = itg.next();

    		if ( group.hidden )
    			continue;
    			
    		if ( group.xmlLayout == null )
    			group.xmlLayout = list.layout;
    		
    		listDataGroup.add(group);
    		
    		Iterator<Item> iti = group.items.iterator();
    		List<Item> listItem = new ArrayList<Item>();
    			
    		while ( iti.hasNext() ) {

    			Item item = iti.next();
    				
    			if ( !item.enabled )
    				continue;
    			
    			if ( item.xmlLayout == null )
    				item.xmlLayout = group.xmlLayout;

    			// Add the new item to the list
    			listItem.add( item );

    		} // End while ( iti.hasNext() ) 
    			
    		listDataChild.put( group.title, listItem );
    			
    	} // End while ( itg.hasNext() ) 
    	
    	return;
    }
    
    // Add a handler to listen to click events in the text items in groups (Data) and (Views).
    // Not sure why, but they don't always recieve the click event from their own onClickListener
    // since they're straight text boxes and not normally clickable
    private OnChildClickListener clickListener = new OnChildClickListener() {
    	@Override
    	public boolean onChildClick(ExpandableListView parent, View view, int groupPosition, int childPosition, long id) {
    		
    		Item item = ((Item)listAdapter.getChild(groupPosition, childPosition));
    		boolean isTextWidget = true;
    		
    		Iterator<Widget> it = item.widgets.iterator();
    		while ( it.hasNext() ) {
    			Widget widget = it.next();
    			if ( !widget.type.equalsIgnoreCase("text") ) {
    				isTextWidget = false;
    				break;
    			}
    		}
    		if ( !isTextWidget )
    			return false;
    		
    		if ( item.enabled ) {
    			Intent intent = new Intent("listManMsg");
    			intent.putExtra("textOnClick", true);
    			intent.putExtra("itemName", item.name);
    			LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    		}
    		return true;
    	}
    };
}
