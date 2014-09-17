package net.pms.dlna;

import ch.qos.logback.classic.spi.LoggingEvent;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.dlna.virtual.VirtualFolder;
import net.pms.dlna.virtual.VirtualVideoAction;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;


public class CodeEnter extends VirtualFolder {
	private static final Logger LOGGER = LoggerFactory.getLogger(CodeEnter.class);

	private DLNAResource resource;
	private String enteredCode;
	private String code;
	private long changed;
	private long commitTime;

	private abstract class CodeAction extends VirtualVideoAction {
		public CodeAction(String name, boolean enable) {
			super(name,enable);
		}
	}

	public CodeEnter(DLNAResource r) {
		super(r.getName(),r.getThumbnailURL());
		resource = r;
		code = "";
		enteredCode = "";
		changed=0;
		commitTime = 0;
	}

	private boolean preventAutoPlay() {
		// Normally changed is 0 and 0+15000 is never larger
		// then now.
		int tmo;
		if(getDefaultRenderer() != null) {
			tmo = getDefaultRenderer().getAutoPlayTmo();
		} else {
			tmo = 5000;
		}
		return (changed + tmo) > System.currentTimeMillis();
	}

	public void setCode(String str) {
		code = str;
	}

	public void setEnteredCode(String str) {
		enteredCode = str;
	}

	public String getCode() { return code; }

	public void discoverChildren(String str) {
		discoverChildren();
	}

	public void discoverChildren() {
		super.addChild(resource);
		for(int i=0;i<10;i++) {
			final int j=i;
			super.addChild(new CodeAction(String.valueOf(i),true) {
				public boolean enable() {
					if(preventAutoPlay()) {
						return false;
					}
					enteredCode += String.valueOf(j);
					changed = System.currentTimeMillis();
					return true;
				}
			});
		}
		super.addChild(new CodeAction(Messages.getString("TracesTab.3"), true) {
			public boolean enable() {
				if(preventAutoPlay()) {
					return false;
				}
				setEnteredCode("");
				changed = System.currentTimeMillis();
				return true;
			}
		});
	}

	public void doRefreshChildren() {
		doRefreshChildren(null);
	}

	@Override
	public void doRefreshChildren(String str) {
		setEnteredCode("");
		getChildren().clear();
		discoverChildren(str);
		analyzeChildren(-1);
	}

	public boolean validCode(DLNAResource r) {
		if (r != null  && r instanceof CodeAction) {
			// always ok
			return true;
		}
		String realCode = PMS.get().codeDb().lookup(code);
		LOGGER.debug("valid code "+commitTime);
		if(!enteredCode.equals(realCode)) {
			// bad code
			return false;
		}
		if(commitTime == 0) {
			// 1st commit
			commitTime = System.currentTimeMillis();
			return true;
		}
		boolean res = (System.currentTimeMillis() - commitTime) < PMS.getConfiguration().getCodeValidTmo();
		if(!res) {
			// clear entered code
			setEnteredCode("");
			commitTime = 0;
			setDiscovered(false);
			getChildren().clear();
		}
		return res;
	}

	public String toString() {
		return "CODE "+code;
	}
}
