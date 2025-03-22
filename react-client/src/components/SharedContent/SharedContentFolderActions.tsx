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
import { Menu } from '@mantine/core'
import { IconAnalyze, IconAnalyzeOff, IconEyeCheck, IconEyeOff } from '@tabler/icons-react'

import { I18nInterface } from '../../services/i18n-service'
import { Folder, SharedContentInterface } from '../../services/shared-service'
import _ from 'lodash'
import axios from 'axios'
import { sharedApiUrl } from '../../utils'
import { showError, showInfo } from '../../utils/notifications'
import { useState } from 'react'

export default function SharedContentFolderActions({
  i18n,
  value,
  sharedContents,
  setSharedContents,
  canModify,
}: {
  i18n: I18nInterface
  value: Folder
  sharedContents: SharedContentInterface[]
  setSharedContents: (sharedContents: SharedContentInterface[]) => void
  canModify: boolean
}) {
  const [isLoading, setLoading] = useState(false)

  const toggleFolderMonitored = (value: Folder) => {
    const sharedContentsTemp = _.cloneDeep(sharedContents)
    const index = sharedContents.indexOf(value);
    (sharedContentsTemp[index] as Folder).monitored = !(sharedContentsTemp[index] as Folder).monitored
    setSharedContents(sharedContentsTemp)
  }

  const markDirectoryFullyPlayed = async (item: string, isPlayed: boolean) => {
    setLoading(true)
    try {
      await axios.post(
        sharedApiUrl + 'mark-directory',
        { directory: item, isPlayed },
      )

      showInfo({
        message: i18n.get('Saved'),
      })
    }
    catch (err) {
      showError({
        title: i18n.get('Error'),
        message: i18n.get('ConfigurationNotSaved'),
        message2: i18n.getReportLink(),
      })
    }
    setLoading(false)
  }

  return (
    <>
      <Menu.Divider />
      <Menu.Item
        color={value.monitored ? 'green' : 'red'}
        leftSection={value.monitored ? <IconAnalyze /> : <IconAnalyzeOff />}
        disabled={!canModify}
        onClick={() => toggleFolderMonitored(value)}
      >
        {i18n.get('MonitorPlayedStatusFiles')}
      </Menu.Item>
      <Menu.Item
        color="blue"
        leftSection={<IconEyeCheck />}
        disabled={!canModify || !value.file || isLoading}
        onClick={() => markDirectoryFullyPlayed(value.file, true)}
      >
        {i18n.get('MarkContentsFullyPlayed')}
      </Menu.Item>
      <Menu.Item
        color="green"
        leftSection={<IconEyeOff />}
        disabled={!canModify || !value.file || isLoading}
        onClick={() => markDirectoryFullyPlayed(value.file, false)}
      >
        {i18n.get('MarkContentsUnplayed')}
      </Menu.Item>
    </>
  )
}
