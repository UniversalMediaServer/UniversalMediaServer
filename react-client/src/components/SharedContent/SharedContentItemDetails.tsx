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
import { I18nInterface } from '../../services/i18n-service'
import { UmsGroup } from '../../services/session-service'

import { Feed, Folder, SharedContentInterface, Stream, VirtualFolder } from '../../services/shared-service'
import SharedContentFeedDetails from './SharedContentFeedDetails'
import SharedContentFolderDetails from './SharedContentFolderDetails'
import SharedContentStreamDetails from './SharedContentStreamDetails'
import SharedContentVirtualFolderDetails from './SharedContentVirtualFolderDetails'

export default function SharedContentItemDetails({
  i18n,
  value,
  groups,
}: {
  i18n: I18nInterface
  value: SharedContentInterface
  groups: UmsGroup[]
}) {
  switch (value.type) {
    case 'FeedAudio':
    case 'FeedImage':
    case 'FeedVideo':
      return <SharedContentFeedDetails i18n={i18n} value={value as Feed} groups={groups} />
    case 'StreamAudio':
    case 'StreamVideo':
      return <SharedContentStreamDetails i18n={i18n} value={value as Stream} groups={groups} />
    case 'Folder':
      return <SharedContentFolderDetails i18n={i18n} value={value as Folder} groups={groups} />
    case 'VirtualFolder':
      return <SharedContentVirtualFolderDetails i18n={i18n} value={value as VirtualFolder} groups={groups} />
    case 'iTunes':
      return i18n.get('ItunesLibrary')
    case 'iPhoto':
      return i18n.get('IphotoLibrary')
    case 'Aperture':
      return i18n.get('ApertureLibrary')
  }
  return (i18n.get('Unknown'))
}
