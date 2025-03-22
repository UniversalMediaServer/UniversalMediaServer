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
import { Button, Divider, Group, TextInput } from '@mantine/core'
import { useForm } from '@mantine/form'

import { I18nInterface } from '../../services/i18n-service'
import { UmsGroup } from '../../services/session-service'

export default function GroupDisplayNameForm({
  i18n,
  group,
  postAccountAction,
}:
{
  i18n: I18nInterface
  group: UmsGroup
  postAccountAction: (data: any, title: string, message: string, successmessage: string, errormessage: string) => void
}) {
  const groupDisplayNameForm = useForm({ initialValues: { id: group.id, displayName: group.displayName } })
  const handleGroupDisplayNameSubmit = (values: typeof groupDisplayNameForm.values) => {
    const data = { operation: 'modifygroup', groupid: group.id, name: values.displayName }
    postAccountAction(data, i18n.get('DisplayNameUpdate'), i18n.get('DisplayNameUpdating'), i18n.get('DisplayNameUpdated'), i18n.get('DisplayNameNotUpdated'))
  }
  return (
    <form onSubmit={groupDisplayNameForm.onSubmit(handleGroupDisplayNameSubmit)}>
      <Divider my="sm" label={i18n.get('DisplayName')} fz="md" c="var(--mantine-color-text)" />
      <TextInput
        label={i18n.get('DisplayName')}
        name="displayName"
        {...groupDisplayNameForm.getInputProps('displayName')}
      />
      <Group justify="flex-end" mt="md">
        <Button type="submit">
          {i18n.get('Update_verb')}
        </Button>
      </Group>
    </form>
  )
}
