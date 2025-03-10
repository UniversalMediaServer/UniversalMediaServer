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
import { Box, Group, MantineSize, Text, TextInput, Tooltip } from '@mantine/core'
import { useState, ReactNode } from 'react'
import { IconFolders, IconFolderX } from '@tabler/icons-react'

import { I18nInterface } from '../../services/i18n-service'
import DirectoryModal from './DirectoryModal'

export default function DirectoryChooser(props: {
  i18n: I18nInterface
  tooltipText: string
  path: string
  callback: any
  label?: string
  disabled?: boolean
  formKey?: string
  size?: MantineSize
  placeholder?: string
  withAsterisk?: boolean
}) {
  const [opened, setOpened] = useState(false)
  const i18n = props.i18n

  const setDirectory = (value: string) => {
    if (props.formKey) {
      props.callback(props.formKey, value)
    }
    else {
      props.callback(value)
    }
  }

  const setSelectedDirectory = (value: string) => {
    if (value) {
      setDirectory(value)
    }
    setOpened(false)
  }

  const hasRightSection = !props.disabled && props.path

  const DirectoryRightSection = (): ReactNode => {
    return hasRightSection && (
      <Box
        c="red"
        pt="4"
        style={{ cursor: 'pointer' }}
        onClick={() => setDirectory('')}
      >
        <IconFolderX size={18} />
      </Box>
    )
  }

  const hasLeftSection = () => {
    return !props.disabled && !props.path
  }

  const DirectoryLeftSection = (): ReactNode | undefined => {
    return hasLeftSection() && (
      <Group
        gap="0"
        style={{ cursor: 'pointer' }}
        onClick={() => { openModal() }}
      >
        <IconFolders size={18} />
        <Text>...</Text>
      </Group>
    )
  }

  const openModal = () => {
    if (!props.disabled) {
      // getSubdirectories(props.path)
      setOpened(true)
    }
  }

  const DirectoryTextInput = (): ReactNode => {
    return (
      <TextInput
        size={props.size}
        label={props.label}
        disabled={props.disabled}
        style={{ flex: 1 }}
        value={props.path}
        placeholder={props.placeholder}
        withAsterisk={props.withAsterisk}
        leftSection={<DirectoryLeftSection />}
        leftSectionWidth={hasLeftSection() ? 40 : 10}
        rightSection={<DirectoryRightSection />}
        rightSectionWidth={hasRightSection ? 30 : 10}
        onClick={() => { openModal() }}
        readOnly
      />
    )
  }

  const DirectoryTooltipText = (): ReactNode => {
    return props.tooltipText
      ? (
          <Tooltip label={props.tooltipText} style={{ width: 350 }} color="blue" multiline withArrow={true}>
            <DirectoryTextInput />
          </Tooltip>
        )
      : <DirectoryTextInput />
  }

  return (
    <Group>
      <DirectoryModal
        i18n={i18n}
        path={props.path}
        opened={opened}
        onClose={() => setOpened(false)}
        setSelectedDirectory={setSelectedDirectory}
      />
      <DirectoryTooltipText />
    </Group>
  )
}

DirectoryChooser.defaultProps = {
  tooltipText: null,
}
