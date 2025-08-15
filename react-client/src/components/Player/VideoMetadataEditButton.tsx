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
import { IconPencilSearch } from '@tabler/icons-react'

import { I18nInterface } from '../../services/i18n-service'

export default function VideoMetadataEditButton({ i18n, setShowVideoMetadataEdit }: { i18n: I18nInterface, setShowVideoMetadataEdit: (value: boolean) => void }) {
  return (
    <Menu.Item
      leftSection={<IconPencilSearch />}
      onClick={() => setShowVideoMetadataEdit(true)}
    >
      {i18n.get('Edit')}
    </Menu.Item>
  )
}
