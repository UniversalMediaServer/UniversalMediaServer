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
import { Button, Divider, Group, Text } from '@mantine/core'
import { useForm } from '@mantine/form'
import { IconExclamationMark, IconX } from '@tabler/icons-react'
import { useState } from 'react'

import { I18nInterface } from '../../services/i18n-service'
import { UmsGroup } from '../../services/session-service'

export default function GroupDeleteForm({
  i18n,
  group,
  postAccountAction,
}:
{
  i18n: I18nInterface
  group: UmsGroup
  postAccountAction: (data: Record<string, unknown>, title: string, message: string, successmessage: string, errormessage: string) => void
}) {
  const groupDeleteForm = useForm({ initialValues: { id: group.id } })
  const handleGroupDeleteSubmit = () => {
    const data = { operation: 'deletegroup', groupid: group.id }
    postAccountAction(data, i18n.get('GroupDeletion'), i18n.get('GroupDeleting'), i18n.get('GroupDeleted'), i18n.get('GroupNotDeleted'))
  }
  const [opened, setOpened] = useState(false)
  return group.id > 1
    ? (
        <form onSubmit={groupDeleteForm.onSubmit(handleGroupDeleteSubmit)}>
          <Divider my="sm" />
          {opened
            ? (
                <Group justify="flex-end" mt="md">
                  <Text c="red">{i18n.get('WarningGroupWillBeDeleted')}</Text>
                  <Button onClick={() => setOpened(false)}>
                    {i18n.get('Cancel')}
                  </Button>
                  <Button type="submit" color="red" leftSection={<IconExclamationMark />} rightSection={<IconExclamationMark />}>
                    {i18n.get('Confirm')}
                  </Button>
                </Group>
              )
            : (
                <Group justify="flex-end" mt="md">
                  <Text c="red">{i18n.get('DeleteGroup')}</Text>
                  <Button onClick={() => setOpened(true)} color="red" leftSection={<IconX />}>
                    {i18n.get('Delete')}
                  </Button>
                </Group>
              )}
        </form>
      )
    : undefined
}
