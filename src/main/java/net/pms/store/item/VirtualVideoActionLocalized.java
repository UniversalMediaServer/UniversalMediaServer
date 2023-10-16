/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package net.pms.store.item;

import net.pms.Messages;
import net.pms.renderers.Renderer;

/**
 * @author Surf@ceS
 */
public abstract class VirtualVideoActionLocalized extends VirtualVideoAction {

	private final String i18nName;
	private final String[] formatStrings;

	protected VirtualVideoActionLocalized(Renderer renderer, String i18nName, boolean enabled, String enabledIconOverride) {
		this(renderer, i18nName, enabled, enabledIconOverride, (String[]) null);
	}

	protected VirtualVideoActionLocalized(Renderer renderer, String i18nName, boolean enabled, String enabledIconOverride, String formatString) {
		this(renderer, i18nName, enabled, enabledIconOverride, new String[] {formatString});
	}

	protected VirtualVideoActionLocalized(Renderer renderer, String i18nName, boolean enabled, String enabledIconOverride, String[] formatStrings) {
		super(renderer, null, enabled, enabledIconOverride);
		this.i18nName = i18nName;
		if (formatStrings != null && formatStrings.length != 0 && formatStrings[0] != null) {
			this.formatStrings =  formatStrings;
		} else {
			this.formatStrings = null;
		}
		if (i18nName != null) {
			this.name = String.format(Messages.getString(i18nName), (Object[]) this.formatStrings);
		}
	}

	@Override
	public String getSystemName() {
		if (i18nName != null) {
			if (formatStrings != null && formatStrings.length > 0 && formatStrings[0] != null) {
				return i18nName + "|" + String.join("|", formatStrings);
			} else {
				return i18nName;
			}
		}
		return getName();
	}

	/**
	 * Returns the unlocalized display name.
	 *
	 * @return The unlocalized display name.
	 */
	@Override
	public String getLocalizedDisplayName(String lang) {
		if (i18nName != null) {
			return "i18n@" + getSystemName();
		}
		return getDisplayName();
	}

}
