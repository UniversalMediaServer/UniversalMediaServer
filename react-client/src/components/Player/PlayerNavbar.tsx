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
import { Button } from '@mantine/core'
import { IconFolder } from '@tabler/icons-react'

import { I18nInterface } from '../../services/i18n-service'
import { BaseBrowse, getMediaIcon, PlayerInterface } from '../../services/player-service'

export default function PlayerNavbar({ i18n, player, data }: { i18n: I18nInterface, player: PlayerInterface, data: BaseBrowse }) {
  const FoldersButtons = () => {
    return data.folders.map((folder) => {
      return (
        <Button
          key={folder.id}
          onClick={() => player.askBrowseId(folder.id)}
          color="gray"
          variant="subtle"
          size="compact-md"
          leftSection={getMediaIcon(folder, i18n, 20, IconFolder)}
        >
          {i18n.getLocalizedName(folder.name)}
        </Button>
      )
    })
  }

  const MediaLibraryFolders = () => {
    return data.mediaLibraryFolders?.map((folder) => {
      return (
        <Button
          key={folder.id}
          onClick={() => player.askBrowseId(folder.id)}
          color="gray"
          variant="subtle"
          size="compact-md"
          leftSection={getMediaIcon(folder, i18n, 20, IconFolder)}
        >
          {i18n.getLocalizedName(folder.name)}
        </Button>
      )
    })
  }

  return data.mediaLibraryFolders && data.mediaLibraryFolders.length > 0
    ? (
        <>
          <div>{i18n.get('MediaLibrary')}</div>
          <MediaLibraryFolders />
          <div>{i18n.get('YourFolders')}</div>
          <FoldersButtons />
        </>
      )
    : (
        <FoldersButtons />
      )
}
