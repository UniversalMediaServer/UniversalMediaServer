package net.pms.newgui.components;

public class CustomComboBoxItem<T> {
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
}
