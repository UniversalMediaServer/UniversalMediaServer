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
import { IconZoomCheck } from '@tabler/icons-react'
import _ from 'lodash'
import { useState } from 'react'

import { I18nInterface } from '../../services/i18n-service'
import { Feed, getFeedName, SharedContentInterface } from '../../services/shared-service'
import { showError, showWarning } from '../../utils/notifications'

export default function SharedContentFeedActions({
  i18n,
  value,
  sharedContents,
  setSharedContents,
  canModify,
}: {
  i18n: I18nInterface
  value: Feed
  sharedContents: SharedContentInterface[]
  setSharedContents: (sharedContents: SharedContentInterface[]) => void
  canModify: boolean
}) {
  const [isLoading, setLoading] = useState(false)
  const updateSharedContentFeedName = async (value: Feed) => {
    setLoading(true)
    try {
      const sharedContentsTemp = _.cloneDeep(sharedContents)
      const index = sharedContents.indexOf(value)
      const name = await getFeedName(value.uri)
      if (name) {
        (sharedContentsTemp[index] as Feed).name = name
        setSharedContents(sharedContentsTemp)
      }
      else {
        showWarning({
          title: i18n.get('Information'),
          message: i18n.get('FeedNameNotFound'),
        })
      }
    }
    catch (err) {
      console.error(err)
      showError({
        title: i18n.get('Error'),
        message: i18n.get('DataNotReceived'),
      })
    }
    setLoading(false)
  }

  return (
    <>
      <Menu.Divider />
      <Menu.Item
        color="blue"
        leftSection={<IconZoomCheck />}
        disabled={!canModify || !value.uri || isLoading}
        onClick={() => updateSharedContentFeedName(value)}
      >
        {i18n.get('UpdateFeedName')}
      </Menu.Item>
    </>
  )
}
