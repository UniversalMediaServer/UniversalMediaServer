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
import { Button, Group, TextInput, VisuallyHidden } from '@mantine/core'
import { useForm } from '@mantine/form'
import { IconLock, IconUser } from '@tabler/icons-react'
import { useState } from 'react'

import { I18nInterface } from '../../services/i18n-service'
import { SessionInterface, UmsUserLogin } from '../../services/session-service'

export default function PassLogin({ i18n, session, user }: { i18n: I18nInterface, session: SessionInterface, user?: UmsUserLogin }) {
  const username = (user && user.username) ? user.username : ''
  const [isLoading, setLoading] = useState(false)
  const form = useForm({
    initialValues: {
      username: username,
      password: '',
    },
  })

  const handleLogin = (values: typeof form.values) => {
    const { username, password } = values
    setLoading(true)
    session.login(username, password).then(() =>
      setLoading(false),
    )
  }

  return (
    <form onSubmit={form.onSubmit(handleLogin)}>
      { username
        ? (
            <VisuallyHidden>
              <TextInput
                name="username"
                {...form.getInputProps('username')}
              />
            </VisuallyHidden>
          )
        : (
            <TextInput
              required
              label={i18n.get('Username')}
              leftSection={<IconUser size={14} />}
              {...form.getInputProps('username')}
            />
          )}
      <TextInput
        required
        label={i18n.get('Password')}
        type="password"
        leftSection={<IconLock size={14} />}
        {...form.getInputProps('password')}
      />
      <Group justify="flex-end" mt="md">
        <Button disabled={isLoading} loading={isLoading} type="submit">{i18n.get('LogIn')}</Button>
      </Group>
    </form>
  )
}
