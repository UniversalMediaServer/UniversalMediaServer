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
import { Button, Group, Tooltip } from '@mantine/core'
import { IconDevices2, IconFolderCheck } from '@tabler/icons-react'
import { I18nInterface, ValueLabelData } from '../../services/i18n-service'
import { ReactNode } from 'react'

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
  const getParentButton = (parent: ValueLabelData): ReactNode => {
    return (
      <Button
        onClick={() => setCurrentPath(parent.value)}
        variant="default"
        size="compact-md"
      >
        {parent.label + separator}
      </Button>
    )
  }

  const getLastParentButton = (parent: ValueLabelData): ReactNode => {
    return (
      <Tooltip label={i18n.get('Select')} color="blue" multiline withArrow={true}>
        <Button
          onClick={() => setSelectedDirectory(parent.value)}
          variant="outline"
          size="compact-md"
          rightSection={<IconFolderCheck />}
        >
          {parent.label}
        </Button>
      </Tooltip>
    )
  }

  const getParentButtons = (): ReactNode => {
    return parents.map((parent, i, { length }) => {
      return (length - 1 === i) ? getLastParentButton(parent) : getParentButton(parent)
    })
  }

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
      {getParentButtons()}
    </Group>
  )
}
