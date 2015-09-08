package net.pms.newgui.components;

public class CustomComboBoxItem<T> implements Comparable<CustomComboBoxItem<T>> {
	private String displayName;
	private T userObject;
	
	public CustomComboBoxItem(String displayName, T userObject){
		setDisplayName(displayName);
		setUserObject(userObject);
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public T getUserObject() {
		return userObject;
	}

	public void setUserObject(T userObject) {
		this.userObject = userObject;
	}
	
	public String toString() {
		return getDisplayName();
	}

	@Override
	public int compareTo(CustomComboBoxItem<T> o) {
		if(getDisplayName() == null && (o == null || o.getDisplayName() == null)) {
			return 0;
		}
		if(getDisplayName() == null) {
			return 1;
		}
		if(o == null || o.getDisplayName() == null) {
			return -1;
		}
		return getDisplayName().compareTo(o.getDisplayName());
	}
}
