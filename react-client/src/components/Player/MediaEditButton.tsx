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
import { Button, Menu } from '@mantine/core'
import { IconEdit, IconRecordMail, IconRecordMailOff } from '@tabler/icons-react'
import axios, { AxiosError } from 'axios'

import { I18nInterface } from '../../services/i18n-service'
import { PlayerInterface } from '../../services/player-service'
import { playerApiUrl } from '../../utils'
import VideoMetadataEditButton from './VideoMetadataEditButton'
import { showError } from '../../utils/notifications'

export default function MediaEditButton({ i18n, player, fullyplayed, videoMetadataEditable, isFolder, refreshPage, setLoading, setShowVideoMetadataEdit }: { i18n: I18nInterface, player: PlayerInterface, fullyplayed?: boolean, videoMetadataEditable?: boolean, isFolder?: boolean, refreshPage: () => void, setLoading: (loading: boolean) => void, setShowVideoMetadataEdit: (loading: boolean) => void }) {
  const setFullyPlayed = (id: string, fullyPlayed: boolean) => {
    setLoading(true)
    axios.post(playerApiUrl + 'setFullyPlayed', { uuid: player.uuid, id, fullyPlayed }, { headers: { Player: player.uuid } })
      .then(function () {
        refreshPage()
      })
      .catch(function (error: AxiosError) {
        if (!error.response && error.request) {
          i18n.showServerUnreachable()
        }
        else {
          showError({
            id: 'player-fully-played',
            title: i18n.get('Error'),
            message: 'Your request was not handled by the server.',
          })
        }
      })
      .then(function () {
        setLoading(false)
      })
  }

  return (
    <Menu shadow="md" width={200}>
      <Menu.Target>
        <Button variant="default" size="compact-md"><IconEdit size={14} /></Button>
      </Menu.Target>
      <Menu.Dropdown>
        {fullyplayed === false
          && (
            <Menu.Item
              color="blue"
              leftSection={<IconRecordMail />}
              onClick={() => setFullyPlayed(player.reqId, true)}
            >
              {i18n.get(isFolder ? 'MarkContentsFullyPlayed' : 'MarkFullyPlayed')}
            </Menu.Item>
          )}
        {fullyplayed === true
          && (
            <Menu.Item
              color="green"
              leftSection={<IconRecordMailOff />}
              onClick={() => setFullyPlayed(player.reqId, false)}
            >
              {i18n.get(isFolder ? 'MarkContentsUnplayed' : 'MarkUnplayed')}
            </Menu.Item>
          )}
        {videoMetadataEditable
          && (
            <VideoMetadataEditButton i18n={i18n} setShowVideoMetadataEdit={setShowVideoMetadataEdit} />
          )}
      </Menu.Dropdown>
    </Menu>
  )
}
