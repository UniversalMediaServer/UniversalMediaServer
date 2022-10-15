/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.dlna;

import net.pms.Messages;
import net.pms.PMS;
import net.pms.dlna.virtual.VirtualFolder;
import net.pms.dlna.virtual.VirtualVideoAction;

public class CodeEnter extends VirtualFolder {
	private final DLNAResource resource;
	private String enteredCode;
	private String code;
	private long changed;
	private long commitTime;

	public static final int DIGITS = 0;
	public static final int LETTERS = 1;
	public static final int BOTH = 2;

	private abstract class CodeAction extends VirtualVideoAction {
		public CodeAction(String name, boolean enable) {
			super(name, enable);
		}

		@Override
		public boolean isLogPlayEvents() {
			return false;
		}
	}

	public CodeEnter(DLNAResource r) {
		super(r.getName(), r.getThumbnailURL(DLNAImageProfile.JPEG_TN));
		resource = r;
		code = "";
		enteredCode = "";
		changed = 0;
		commitTime = 0;
	}

	private boolean preventAutoPlay() {
		// Normally changed is 0 and 0+15000 is never larger
		// then now.
		int tmo;
		if (getDefaultRenderer() != null) {
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

	public String getCode() {
		return code;
	}

	public DLNAResource getResource() {
		return resource;
	}

	private void addCharVVA(final String ch) {
		super.addChild(new CodeAction(ch, true) {
			@Override
			public boolean enable() {
				if (preventAutoPlay()) {
					return false;
				}
				enteredCode += ch;
				changed = System.currentTimeMillis();
				return true;
			}
		});
	}

	@Override
	public void discoverChildren(String str) {
		discoverChildren();
	}

	@Override
	public void discoverChildren() {
		super.addChild(resource);
		int charset = configuration.getCodeCharSet();
		if (charset == LETTERS || charset == BOTH) {
			// Letters first
			for (char i = 'A'; i <= 'Z'; i++) {
				addCharVVA(String.valueOf(i));
			}
		}
		if (charset == DIGITS || charset == BOTH) {
			// then the digits
			for (int i = 0; i < 10; i++) {
				addCharVVA(String.valueOf(i));
			}
		}
		super.addChild(new CodeAction(Messages.getString("Clear"), true) {
			@Override
			public boolean enable() {
				if (preventAutoPlay()) {
					return false;
				}
				setEnteredCode("");
				changed = System.currentTimeMillis();
				return true;
			}
		});
	}

	@Override
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
		if (r instanceof CodeAction) {
			// always ok
			return true;
		}
		String realCode = PMS.get().codeDb().lookup(code);
		if (!enteredCode.equalsIgnoreCase(realCode)) {
			// bad code
			return false;
		}
		if (commitTime == 0) {
			// 1st commit
			commitTime = System.currentTimeMillis();
			return true;
		}
		boolean res = (System.currentTimeMillis() - commitTime) < configuration.getCodeValidTmo();
		if (!res) {
			// clear entered code
			setEnteredCode("");
			commitTime = 0;
			setDiscovered(false);
			getChildren().clear();
		}
		return res;
	}

	@Override
	public String toString() {
		return "CODE " + code;
	}
}
