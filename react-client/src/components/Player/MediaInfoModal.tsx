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
import { Button, Group, Modal, ScrollArea } from '@mantine/core'
import { IconDeviceSpeaker, IconFileInfo, IconMovie } from '@tabler/icons-react'
import axios, { AxiosError, AxiosResponse } from 'axios'
import { useEffect, useState } from 'react'

import { I18nInterface } from '../../services/i18n-service'
import { playerApiUrl } from '../../utils'
import { showError } from '../../utils/notifications'
import { BaseMediaInfo } from '../../services/player-service'
import MediaInfoNumberValue from './MediaInfoNumberValue'
import MediaInfoPanel from './MediaInfoPanel'
import MediaInfoStringValue from './MediaInfoStringValue'

export default function MediaInfoModal({
  i18n,
  uuid,
  id,
}: {
  i18n: I18nInterface
  uuid: string
  id: string
}) {
  const [isLoading, setLoading] = useState(true)
  const [opened, setOpened] = useState(false)
  const [mediaInfo, setMediaInfo] = useState<BaseMediaInfo | null>(null)

  const getMediaInfo = () => {
    setLoading(true)
    axios.post(playerApiUrl + 'getMediaInfo', { uuid: uuid, id: id })
      .then(function (response: AxiosResponse) {
        setMediaInfo(response.data)
        setOpened(true)
      })
      .catch(function (error: AxiosError) {
        if (!error.response && error.request) {
          i18n.showServerUnreachable()
        }
        else {
          showError({
            id: 'data-loading',
            title: i18n.get('Error'),
            message: 'Your edit data was not received from the server.',
          })
        }
      })
      .then(function () {
        setLoading(false)
      })
  }

  const getTitle = () => {
    if (mediaInfo && mediaInfo.mediaInfo && mediaInfo.mediaInfo.mediaType) {
      switch (mediaInfo.mediaInfo.mediaType) {
        case 'video':
          return (
            <Group>
              <IconMovie />
              {i18n.get('MediaInfo')}
            </Group>
          )
        case 'audio':
          return (
            <Group>
              <IconDeviceSpeaker />
              {i18n.get('MediaInfo')}
            </Group>
          )
      }
    }
    return i18n.get('MediaInfo')
  }

  useEffect(() => {
    if (opened) {
      getMediaInfo()
    }
  }, [opened])

  return (
    <>
      <Modal
        opened={opened}
        onClose={() => setOpened(false)}
        title={getTitle()}
        scrollAreaComponent={ScrollArea.Autosize}
        size="lg"
      >
        <MediaInfoStringValue value={mediaInfo?.filename} title={i18n.get('File')} />
        <MediaInfoNumberValue value={mediaInfo?.size} title={i18n.get('Size')} />
        <MediaInfoPanel i18n={i18n} mediaInfo={mediaInfo?.mediaInfo} />
        <Button
          fullWidth
          mt="md"
          loading={isLoading}
          size="compact-md"
          onClick={() => {
            setOpened(false)
          }}
        >
          {i18n.get('Close')}
        </Button>
      </Modal>
      <Button
        variant="default"
        size="compact-md"
        onClick={() => {
          setOpened(true)
        }}
      >
        <IconFileInfo size={14} />
      </Button>
    </>
  )
}
