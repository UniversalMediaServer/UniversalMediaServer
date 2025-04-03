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
import { Button, Divider, Group, Modal, Text, TextInput } from '@mantine/core'
import { useForm } from '@mantine/form'
import { IconLock, IconUser } from '@tabler/icons-react'
import axios from 'axios'
import { useState } from 'react'

import { I18nInterface } from '../../services/i18n-service'
import { SessionInterface } from '../../services/session-service'
import { allowHtml, authApiUrl } from '../../utils'
import { showError } from '../../utils/notifications'

export default function NoAdminLogin({ i18n, session }: { i18n: I18nInterface, session: SessionInterface }) {
  const [opened, setOpened] = useState(false)
  const form = useForm({
    initialValues: {
      username: '',
      password: '',
    },
  })

  const createUser = async (username: string, password: string) => {
    const response = await axios
      .post(authApiUrl + 'create', {
        username,
        password,
      })
    if (response.data.token) {
      session.setToken(response.data.token)
    }
    if (response.data.account) {
    // refresh session.account
    }
    return response.data
  }

  const disableAuth = async () => {
    return await axios.get(authApiUrl + 'disable')
  }

  const handleUserCreation = (values: typeof form.values) => {
    const { username, password } = values
    createUser(username, password).then(
      () => {
        session.resetLogout()
      },
      () => {
        showError({
          id: 'user-creation-error',
          title: i18n.get('Error'),
          message: i18n.get('NewUserNotCreated'),
        })
      },
    )
  }

  const handleAuthDisable = () => {
    disableAuth().then(
      () => {
        session.logout(false)
      },
      () => {
        showError({
          id: 'auth-disable-error',
          title: i18n.get('Error'),
          message: i18n.get('AuthenticationServiceNotDisabled'),
        })
      },
    )
  }

  return (
    <form onSubmit={form.onSubmit(handleUserCreation)}>
      <TextInput
        required
        label={i18n.get('Username')}
        leftSection={<IconUser size={14} />}
        {...form.getInputProps('username')}
      />
      <TextInput
        required
        label={i18n.get('Password')}
        type="password"
        leftSection={<IconLock size={14} />}
        {...form.getInputProps('password')}
      />
      <Group justify="flex-end" mt="md">
        <Button type="submit">{i18n.get('Create')}</Button>
      </Group>
      {session.authenticate && (
        <>
          <Divider my="lg" label={i18n.get('Or')} labelPosition="center" fz="md" c="var(--mantine-color-text)" />
          <Modal
            centered
            opened={opened}
            onClose={() => setOpened(false)}
            title={i18n.get('Warning')}
          >
            <Text>{allowHtml(i18n.get('DisablingAuthenticationReduces'))}</Text>
            <Group justify="flex-end" mt="md">
              <Button onClick={() => setOpened(false)}>{i18n.get('Cancel')}</Button>
              <Button color="red" onClick={() => handleAuthDisable()}>{i18n.get('Confirm')}</Button>
            </Group>
          </Modal>
          <Group justify="center" mt="md">
            <Button color="red" onClick={() => setOpened(true)}>{i18n.get('DisableAuthentication')}</Button>
          </Group>
        </>
      )}
    </form>
  )
}
