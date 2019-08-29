package ch.cern.cms.data_browser.listManager;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;
import ch.cern.cms.data_browser.Logger;
import ch.cern.cms.data_browser.R;

public class ExpandableListAdapter extends BaseExpandableListAdapter {
	
    private Context context;
    private List<Group> listDataGroup;
    private HashMap<String, List<Item>> listDataItem;
    private ImageView _icon = null;
    private Logger Log;

    public ExpandableListAdapter(Context _context, List<Group> _listDataGroup, HashMap<String, List<Item>> _listDataItem) {
        this.context = _context;
        this.listDataGroup = _listDataGroup;
        this.listDataItem = _listDataItem;
        
        for ( int i=0; i<listDataGroup.size(); i++ ) {
        	listDataGroup.get(i).enabled = true;
        	listDataGroup.get(i).expanded = false;
        }
        
        Log = Logger.getInstance();
        
        return;
    }
     
    @Override
    public void onGroupExpanded( int groupPosition ) {
    	int resource = R.drawable.ic_action_collapse;
    	if ( this._icon != null )
    		this._icon.setImageResource(resource);
    	listDataGroup.get(groupPosition).expanded = true;
    	return;
    }

    @Override
    public void onGroupCollapsed( int groupPosition ) {
    	int resource = R.drawable.ic_action_expand;
    	if ( this._icon != null )
    		this._icon.setImageResource(resource);
    	listDataGroup.get(groupPosition).expanded = false;
    	return;
    }
    
    public boolean areAllItemsEnabled() { 
    	return false;
    } 
    
    public boolean isChildSelectable (int groupPosition, int childPosition) {
    	boolean selectable = 
    			listDataItem.get(listDataGroup.get(groupPosition).title).get(childPosition).selectable &&
    			listDataItem.get(listDataGroup.get(groupPosition).title).get(childPosition).enabled;
    	return selectable;
    }
    
    public boolean isEnabled(int groupPosition) {
    	return listDataGroup.get(groupPosition).enabled;
    }
    
    public void enableGroup( int groupPosition, boolean enabled ) {
    	listDataGroup.get(groupPosition).enabled  = enabled;
    	listDataGroup.get(groupPosition).expanded = enabled;
    	if ( this._icon != null ) {

    		if ( enabled ) {
            	int resource = (listDataGroup.get(groupPosition).expanded) ? R.drawable.ic_action_collapse : R.drawable.ic_action_expand;
            	this._icon.setImageResource(resource);
    		} else
    			this._icon.setImageResource(R.drawable.ic_action_none);

    	} else
    		Log.e("enableGroup()", "Icon for group "+groupPosition+" is not yet ready");

    	this.notifyDataSetChanged();

    	return;
    }
    
    public void enableGroup( String groupName, boolean enabled ) {
    	
    	Iterator<Group> it = listDataGroup.iterator();
    	Group group = null;
    	while ( it.hasNext() ) {
    		group = it.next();
    		if ( group.title.equalsIgnoreCase(groupName) ) {
    			group.enabled = group.expanded = enabled;
    			break;
    		}
    	}
    	
    	if ( this._icon != null && group != null ) {

    		if ( enabled ) {
            	int resource = (group.expanded) ? R.drawable.ic_action_collapse : R.drawable.ic_action_expand;
            	this._icon.setImageResource(resource);
    		} else
    			this._icon.setImageResource(R.drawable.ic_action_none);

    	} else
    		Log.e("enableGroup()", "Icon for group "+groupName+" is not yet ready");
    	
    	this.notifyDataSetChanged();
    	return;
    }

    public boolean getGroupExpanded(int groupPosition) {
    	return listDataGroup.get(groupPosition).expanded;
    }
    
    public boolean getGroupExpanded(String groupName) {
    	Iterator<Group> it = listDataGroup.iterator();
    	while ( it.hasNext() ) {
    		Group group = it.next();
    		if ( group.title.equalsIgnoreCase(groupName) )
    			return group.expanded;
    	}

    	return false;
    }    
    
    @Override
    public Object getChild(int groupPosition, int childPosititon) {
        return this.listDataItem.get(this.listDataGroup.get(groupPosition).title).get(childPosititon);
    }
 
    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }
    
	@SuppressLint("NewApi")
	@Override
    public View getChildView(int groupPosition, final int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
    	final Item item = (Item)getChild(groupPosition, childPosition);
    	
    	int layoutID = -1;
    	if ( item.xmlLayout != null )
    		layoutID = this.context.getResources().getIdentifier(item.xmlLayout, "layout", this.context.getPackageName() );
    	
    		LayoutInflater infalInflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    		convertView = infalInflater.inflate(layoutID, null);

    	Iterator<Widget> it = item.widgets.iterator();
    	while ( it.hasNext() ) {
    		final Widget widget = it.next();

        	int widgetID = -1;
        	if ( widget.ID != null )
        		widgetID = this.context.getResources().getIdentifier(widget.ID, "id", this.context.getPackageName() );

        	if ( widget.type.equalsIgnoreCase("text") ) {

        		TextView textView = convertView.findViewById(widgetID);
        		textView.setText( widget.text );
        		textView.setEnabled(item.enabled);
        		if ( !item.enabled )
        			textView.setTextColor(0x55cccccc);
        		else
        			textView.setTextColor(0xffcccccc);
        		
        	} else if ( widget.type.equalsIgnoreCase("slider") ) {

        		int value = widget.value;
            	SeekBar slider = convertView.findViewById(widgetID);
            	slider.setMax(widget.maximum);
            	slider.setProgress((value));
            	slider.setEnabled(item.enabled);
            	
            	// Set a callback for changes in the seek bar
            	slider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            		@Override
            		public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
            			widget.value = progressValue + widget.minimum;
                		broadCastStatus("seekBarProgressChanged");
                		return;
            		}
            	
            		@Override
            		public void onStartTrackingTouch(SeekBar seekBar) {}

            		@Override
            		public void onStopTrackingTouch(SeekBar seekBar) {
            			widget.value = seekBar.getProgress() + widget.minimum;
                		broadCastStatus("seekBarStopTracking");
            		}

            		private void broadCastStatus( String msg ) {
                		Intent intent = new Intent("listManMsg");
                		intent.putExtra("seekBarStatusChanged", true);
                        intent.putExtra("changeType", msg);
                		intent.putExtra("widgetName", widget.text);
                		intent.putExtra("progress",   widget.value);
                		LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            		}

            	});

        	} else if ( widget.type.equalsIgnoreCase("switch") ) {
        		
            	if (  Build.VERSION.SDK_INT > 13 ) {
            		Switch state = convertView.findViewById(widgetID);
            		state.setText(widget.text);
            		if ( !item.enabled )
            			state.setTextColor(0x55cccccc);
            		else
            			state.setTextColor(0xffcccccc);

            		state.setChecked( widget.state );
            		state.setEnabled(item.enabled);
            		
            		// Set the callback for the click events
            		state.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener() {
            			@Override 
            			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            				widget.state = isChecked;
                    		Intent intent = new Intent("listManMsg");
                    		intent.putExtra("switchCheckedChanged", true);
                    		intent.putExtra("widgetName", widget.text);
                    		intent.putExtra("checked",    widget.state);
                    		LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            			}
            		});
            	} else {
            		CheckBox state = convertView.findViewById(widgetID);
            		state.setText(widget.text);
            		if ( !item.enabled )
            			state.setTextColor(0x55cccccc);
            		else
            			state.setTextColor(0xffcccccc);

            		state.setChecked( widget.state );
            		state.setEnabled(item.enabled);

            		// Set the callback for the click events
            		state.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener() {
            			@Override 
            			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            				widget.state = isChecked;
                    		Intent intent = new Intent("listManMsg");
                    		intent.putExtra("switchCheckedChanged", true);
                    		intent.putExtra("widgetName", widget.text);
                    		intent.putExtra("checked",    widget.state);
                    		LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            			}
            		});
            	}
            	
        	} else if ( widget.type.equalsIgnoreCase("checkbox")) {

            	CheckBox state = convertView.findViewById(widgetID);
            	state.setText(widget.text);
        		if ( !item.enabled )
        			state.setTextColor(0x55cccccc);
        		else
        			state.setTextColor(0xffcccccc);

        		state.setChecked(widget.state);
        		state.setEnabled(item.enabled);

        		// Set the callback for the click events
            	state.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener() {
        	
            		@Override 
            		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            			widget.state = isChecked;
                		Intent intent = new Intent("listManMsg");
                		intent.putExtra("switchCheckedChanged", true);
                		intent.putExtra("widgetName", widget.text);
                		intent.putExtra("checked",    widget.state);
                		LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            		}
            		
            	});

        	} else if ( widget.type.equalsIgnoreCase("togglebutton")) {
        		
            	ToggleButton state = convertView.findViewById(widgetID);
            	state.setText(widget.text);
        		if ( !item.enabled )
        			state.setTextColor(0x55cccccc);
        		else
        			state.setTextColor(0xffcccccc);

        		state.setChecked(widget.state);
        		state.setEnabled(item.enabled);

        		// Set the callback for the click events
            	state.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener() {
        	
            		@Override 
            		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            			widget.state = isChecked;
                		Intent intent = new Intent("listManMsg");
                		intent.putExtra("switchCheckedChanged", true);
                		intent.putExtra("widgetName", widget.text);
                		intent.putExtra("checked",    widget.state);
                		LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            		}
            		
            	});

        	}
 
    	}

    	return convertView;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
    	if ( !this.listDataItem.isEmpty() && !this.listDataGroup.isEmpty() )
    		return this.listDataItem.get(this.listDataGroup.get(groupPosition).title).size();
    	else
    		return 0;
    }
 
    @Override
    public Object getGroup(int groupPosition) {
        return this.listDataGroup.get(groupPosition).title;
    }
 
    @Override
    public int getGroupCount() {
        return this.listDataGroup.size();
    }
 
    @Override
    public long getGroupId(int groupPosition) {
    	return groupPosition;
    }
 
    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
    	
        String headerTitle = (String) getGroup(groupPosition);
        
        if (convertView == null) {
        	int layoutID = -1;
        	if ( listDataGroup.get(groupPosition).xmlLayout != null )
        		layoutID = this.context.getResources().getIdentifier(listDataGroup.get(groupPosition).xmlLayout, "layout", this.context.getPackageName() );
    	
            LayoutInflater infalInflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(layoutID, null);
        }
        
        int resource = -1;
        if ( listDataGroup.get(groupPosition).textviewID != null )
        	resource = this.context.getResources().getIdentifier(listDataGroup.get(groupPosition).textviewID, "id", this.context.getPackageName() );

        TextView lblListHeader = convertView.findViewById(resource);
        
        if ( listDataGroup.get(groupPosition).enabled )
        	lblListHeader.setTypeface(null, Typeface.BOLD);
        else
        	lblListHeader.setTypeface(null, Typeface.NORMAL);
        
        lblListHeader.setText(headerTitle);
 
        listDataGroup.get(groupPosition).expanded = listDataGroup.get(groupPosition).enabled && isExpanded;
        
        resource = -1;
        if ( listDataGroup.get(groupPosition).iconID != null )
        	resource = this.context.getResources().getIdentifier(listDataGroup.get(groupPosition).iconID, "id", this.context.getPackageName() );

        if ( resource != -1 ) {
        	this._icon = convertView.findViewById(resource);
        	if ( listDataGroup.get(groupPosition).enabled ) {
        		resource = (listDataGroup.get(groupPosition).expanded) ? R.drawable.ic_action_collapse : R.drawable.ic_action_expand;
        		this._icon.setImageResource(resource);
        	} else
        		this._icon.setImageResource(R.drawable.ic_action_none);
        }
        
        return convertView;
    }
    
    @Override
    public boolean hasStableIds() {
        return false;
    }

}
