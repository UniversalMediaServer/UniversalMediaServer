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
import { Button, Group } from '@mantine/core'
import { IconDevices2 } from '@tabler/icons-react'

import { I18nInterface, ValueLabelData } from '../../services/i18n-service'
import DirectoryBreadcrumbsButton from './DirectoryBreadcrumbsButton'
import DirectoryBreadcrumbsLastButton from './DirectoryBreadcrumbsLastButton'

export default function DirectoryBreadcrumbs({
  i18n,
  parents,
  separator,
  setCurrentPath,
  setSelectedDirectory,
}: {
  i18n: I18nInterface
  parents: ValueLabelData[]
  separator?: string
  setCurrentPath: (path: string) => void
  setSelectedDirectory: (path: string) => void
}) {
  return (
    <Group gap="0">
      <Button
        onClick={() => setCurrentPath('roots')}
        variant="default"
        size="compact-md"
        me="xs"
      >
        <IconDevices2 />
      </Button>
      {
        parents.map((path: ValueLabelData, i, { length }) => {
          return (length - 1 === i)
            ? <DirectoryBreadcrumbsLastButton key={i} i18n={i18n} path={path} setSelectedDirectory={setSelectedDirectory} />
            : <DirectoryBreadcrumbsButton key={i} path={path} separator={separator} setCurrentPath={setCurrentPath} />
        })
      }
    </Group>
  )
}
