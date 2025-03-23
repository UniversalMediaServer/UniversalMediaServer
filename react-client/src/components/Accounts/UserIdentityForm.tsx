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
import { Button, Divider, Group, PasswordInput, TextInput } from '@mantine/core'
import { useForm } from '@mantine/form'

import { I18nInterface } from '../../services/i18n-service'
import { UmsUser } from '../../services/session-service'

export default function UserIdentityForm({
  i18n,
  user,
  postAccountAction,
}:
{
  i18n: I18nInterface
  user: UmsUser
  postAccountAction: (data: any, title: string, message: string, successmessage: string, errormessage: string) => void
}) {
  const userIdentityForm = useForm({ initialValues: { id: user.id, username: user.username, password: '' } })
  const handleUserIdentitySubmit = (values: typeof userIdentityForm.values) => {
    const data = { operation: 'changelogin', userid: user.id, username: values.username, password: values.password }
    postAccountAction(data, i18n.get('CredentialsUpdate'), i18n.get('CredentialsUpdating'), i18n.get('CredentialsUpdated'), i18n.get('CredentialsNotUpdated'))
  }
  return (
    <form onSubmit={userIdentityForm.onSubmit(handleUserIdentitySubmit)}>
      <Divider my="sm" label={i18n.get('Credentials')} fz="md" c="var(--mantine-color-text)" />
      <TextInput
        required
        label={i18n.get('Username')}
        name="username"
        {...userIdentityForm.getInputProps('username')}
      />
      <PasswordInput
        required
        label={i18n.get('Password')}
        name="username"
        type="password"
        {...userIdentityForm.getInputProps('password')}
      />
      <Group justify="flex-end" mt="md">
        <Button type="submit">
          {i18n.get('Apply')}
        </Button>
      </Group>
    </form>
  )
}
