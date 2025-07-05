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
import { Box, Button, Collapse, Group } from '@mantine/core'
import { useDisclosure } from '@mantine/hooks'

import { I18nInterface } from '../../services/i18n-service'
import { ImageInfo } from '../../services/player-service'
import MediaInfoNumberValue from './MediaInfoNumberValue'
import MediaInfoStringValue from './MediaInfoStringValue'

export default function MediaInfoImage({ i18n, image, title }: { i18n: I18nInterface, image?: ImageInfo, title: string }) {
  const [opened, { toggle }] = useDisclosure(false)

  return image && (
    <Box>
      <Group>
        <Button variant="default" size="compact-xs" onClick={toggle}>{title}</Button>
      </Group>

      <Collapse in={opened}>
        <MediaInfoStringValue value={image.format} title={i18n.get('Format')} />
        <MediaInfoStringValue value={image.resolution} title={i18n.get('Resolution')} />
        <MediaInfoNumberValue value={image.size} title={i18n.get('Size')} />
        <MediaInfoNumberValue value={image.bitDepth} title="BitDepth" />
      </Collapse>
    </Box>
  )
}
