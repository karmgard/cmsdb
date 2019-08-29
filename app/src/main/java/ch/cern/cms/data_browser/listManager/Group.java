package ch.cern.cms.data_browser.listManager;

import java.util.ArrayList;

public class Group {
	public String xmlLayout;
	public String textviewID;
	public String iconID;
	public String title;
	public ArrayList<Item> items = new ArrayList<Item>();
	public boolean enabled = true;
	public boolean hidden = false;
	public boolean expanded = false;
	public boolean wasExpanded = false;
}
