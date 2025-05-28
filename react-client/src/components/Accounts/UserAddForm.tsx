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
import { Button, Group, PasswordInput, Select, TextInput } from '@mantine/core'
import { useForm } from '@mantine/form'

import { I18nInterface, ValueLabelData } from '../../services/i18n-service'

export default function UserAddForm({
  i18n,
  groupSelectionDatas,
  postAccountAction,
}:
{
  i18n: I18nInterface
  groupSelectionDatas: ValueLabelData[]
  postAccountAction: (data: Record<string, unknown>, title: string, message: string, successmessage: string, errormessage: string) => void
}) {
  const newUserForm = useForm({ initialValues: { username: '', password: '', groupid: '0', displayname: '' } })
  const handleNewUserSubmit = (values: typeof newUserForm.values) => {
    const data = { operation: 'createuser', username: values.username, password: values.password, groupid: values.groupid, displayname: values.displayname }
    postAccountAction(data, i18n.get('UserCreation'), i18n.get('NewUserCreating'), i18n.get('NewUserCreated'), i18n.get('NewUserNotCreated'))
  }
  return (
    <form onSubmit={newUserForm.onSubmit(handleNewUserSubmit)}>
      <TextInput
        required
        label={i18n.get('Username')}
        name="username"
        {...newUserForm.getInputProps('username')}
      />
      <PasswordInput
        required
        label={i18n.get('Password')}
        name="username"
        type="password"
        {...newUserForm.getInputProps('password')}
      />
      <TextInput
        required
        label={i18n.get('DisplayName')}
        name="displayname"
        {...newUserForm.getInputProps('displayname')}
      />
      <Select
        required
        label={i18n.get('Group')}
        name="groupId"
        data={groupSelectionDatas}
        {...newUserForm.getInputProps('groupid')}
      />
      {newUserForm.isValid() && (
        <Group justify="flex-end" mt="md">
          <Button type="submit">
            {i18n.get('Create')}
          </Button>
        </Group>
      )}
    </form>
  )
}
