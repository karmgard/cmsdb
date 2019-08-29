package ch.cern.cms.data_browser.listManager;
import java.util.ArrayList;
public class Item {
	public String xmlLayout;
	public String label;
	public String name;
	public ArrayList<Widget> widgets = new ArrayList<Widget>();
	public boolean enabled;
	public boolean selectable = true;
}
