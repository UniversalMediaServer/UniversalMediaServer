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
import { IconListSearch, IconLoader } from '@tabler/icons-react'
import { useState } from 'react'

import { sendAction } from '../../services/actions-service'
import { I18nInterface } from '../../services/i18n-service'
import { SharedContentInterface } from '../../services/shared-service'

export default function ScanSharedFoldersButton({
  i18n,
  scan,
  sharedContents,
  canModify,
}: {
  i18n: I18nInterface
  scan: boolean
  sharedContents: SharedContentInterface[]
  canModify: boolean
}) {
  const [isLoading, setLoading] = useState(false)
  const haveFolder = sharedContents.find(sharedContent => sharedContent.type.startsWith('Folder'))
  const scanAllSharedFolders = async () => {
    setLoading(true)
    try {
      await sendAction('Server.ScanAllSharedFolders')
    }
    catch (err) {
      console.error(err)
    }
    setLoading(false)
  }

  const scanAllSharedFoldersCancel = async () => {
    setLoading(true)
    try {
      await sendAction('Server.ScanAllSharedFoldersCancel')
    }
    catch (err) {
      console.error(err)
    }
    setLoading(false)
  }

  return haveFolder
    ? (
        <Button
          disabled={!canModify || isLoading}
          leftSection={<IconListSearch />}
          variant="outline"
          color={scan ? 'red' : 'blue'}
          onClick={() => scan ? scanAllSharedFoldersCancel() : scanAllSharedFolders()}
        >
          {i18n.get(scan ? 'CancelScanningSharedFolders' : 'ScanAllSharedFolders')}
          {scan && (<IconLoader />)}
        </Button>
      )
    : undefined
}
