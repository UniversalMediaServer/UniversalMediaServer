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
import { Box, Button, Group, LoadingOverlay, Modal, Paper, ScrollArea, Stack } from '@mantine/core'
import { IconFolder, IconFolders } from '@tabler/icons-react'
import axios, { AxiosError, AxiosResponse } from 'axios'
import { useEffect, useState } from 'react'

import DirectoryBreadcrumbs from './DirectoryBreadcrumbs'
import { I18nInterface, ValueLabelData } from '../../services/i18n-service'
import { settingsApiUrl } from '../../utils'
import { showError } from '../../utils/notifications'

export default function DirectoryModal({
  i18n,
  path,
  opened,
  onClose,
  setSelectedDirectory,
}: {
  i18n: I18nInterface
  path: string
  opened: boolean
  onClose: () => void
  setSelectedDirectory: (value: string) => void
}) {
  const [directories, setDirectories] = useState<ValueLabelData[]>([])
  const [parents, setParents] = useState<ValueLabelData[]>([])
  const [separator, setSeparator] = useState('/')
  const [isLoading, setLoading] = useState(true)

  useEffect(() => {
    if (opened) {
      getSubdirectories(path)
    }
  }, [opened, path])

  const getSubdirectories = (path: string) => {
    setLoading(true)
    axios.post(settingsApiUrl + 'directories', { path: (path) ? path : '' })
      .then(function (response: AxiosResponse) {
        const directoriesResponse = response.data
        setSeparator(directoriesResponse.separator)
        setDirectories(directoriesResponse.children)
        setParents(directoriesResponse.parents.reverse())
      })
      .catch(function (error: AxiosError) {
        if (!error.response && error.request) {
          i18n.showServerUnreachable()
        }
        else {
          showError({
            id: 'data-loading',
            title: i18n.get('Error'),
            message: i18n.get('SubdirectoriesNotReceived'),
            message2: i18n.getReportLink(),
          })
        }
      })
      .then(function () {
        setLoading(false)
      })
  }

  return (
    <Modal
      opened={opened}
      onClose={() => onClose()}
      title={(
        <Group>
          <IconFolders />
          {i18n.get('SelectedDirectory')}
        </Group>
      )}
      scrollAreaComponent={ScrollArea.Autosize}
      size="lg"
    >
      <Box mx="auto">
        <LoadingOverlay visible={isLoading} />
        <Paper shadow="md" p="xs" withBorder>
          <DirectoryBreadcrumbs i18n={i18n} separator={separator} parents={parents} setCurrentPath={getSubdirectories} setSelectedDirectory={setSelectedDirectory} />
        </Paper>
        <Stack gap="xs" align="flex-start" justify="flex-start" mt="sm">
          {directories.map(directory => (
            <Button
              key={directory.value}
              leftSection={<IconFolder size={18} />}
              variant="subtle"
              onClick={() => getSubdirectories(directory.value)}
              size="compact-md"
            >
              {directory.label}
            </Button>
          ))}
        </Stack>
      </Box>
    </Modal>
  )
}
