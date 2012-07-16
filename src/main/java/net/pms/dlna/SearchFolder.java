package net.pms.dlna;

import net.pms.dlna.virtual.VirtualFolder;


public class SearchFolder extends VirtualFolder {
	private String name;
	private SearchObj sobj;
	
	public SearchFolder (String name,SearchObj sobj){
		super(name,null);
		this.name=name;
		this.sobj=sobj;
	}
	
	
	private void createSearcher(SearchObj obj,String initStr) {
		char i;
		Search s=new Search(obj,initStr);
		addChild(s);
		addChild(new SearchAction(s,'\0',"Clear"));
		addChild(new SearchAction(s,' ',"Space"));
		addChild(new SearchAction(s,'\b',"Delete"));
		for(i='A';i<='Z';i++) 
			addChild(new SearchAction(s,i));
		for(i='0';i<='9';i++)
			addChild(new SearchAction(s,i));
	}
	
	public void resolve() {
	}
	
	public void discoverChildren(String str) {
		if(str==null)
			discoverChildren();
		else
			sobj.search(str, this);
	}
	
	public boolean isSearched() {
		return true;
	}
	
	//@Override
    public void discoverChildren()  {
		createSearcher(sobj,"");
    }
    
    public boolean isRefreshNeeded() {
		return true;
	}
    
    public boolean refreshChildren() {
    	refreshChildren(null);
    	return true;
    }
	
	public boolean refreshChildren(String str) {
		if(str==null)
			return false;
		getChildren().clear();
		discoverChildren(str);
		return true;
	}
}

