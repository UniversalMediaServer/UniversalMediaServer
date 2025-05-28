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
import { Button, Group, TextInput } from '@mantine/core'
import { useForm } from '@mantine/form'

import { I18nInterface } from '../../services/i18n-service'

export default function GroupAddForm({
  i18n,
  postAccountAction,
}:
{
  i18n: I18nInterface
  postAccountAction: (data: Record<string, unknown>, title: string, message: string, successmessage: string, errormessage: string) => void
}) {
  const newGroupForm = useForm({ initialValues: { displayName: '' } })
  const handleNewGroupSubmit = (values: typeof newGroupForm.values) => {
    const data = { operation: 'creategroup', name: values.displayName }
    postAccountAction(data, i18n.get('GroupCreation'), i18n.get('NewGroupCreating'), i18n.get('NewGroupCreated'), i18n.get('NewGroupNotCreated'))
  }
  return (
    <form onSubmit={newGroupForm.onSubmit(handleNewGroupSubmit)}>
      <TextInput
        required
        label={i18n.get('DisplayName')}
        name="displayName"
        {...newGroupForm.getInputProps('displayName')}
      />
      <Group justify="flex-end" mt="md">
        <Button type="submit">
          {i18n.get('Create')}
        </Button>
      </Group>
    </form>
  )
}
