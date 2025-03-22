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
import { Button, Group, Modal, Text } from '@mantine/core'
import axios from 'axios'
import { useState } from 'react'

import { UmsAccounts } from '../../services/accounts-service'
import { I18nInterface } from '../../services/i18n-service'
import { SessionInterface } from '../../services/session-service'
import { accountApiUrl, allowHtml } from '../../utils'
import { showError } from '../../utils/notifications'

export default function AuthenticationSettings({
  i18n,
  session,
  accounts,
}: {
  i18n: I18nInterface
  session: SessionInterface
  accounts: UmsAccounts
}) {
  const [authOpened, setAuthOpened] = useState(false)
  const [localhostOpened, setLocalhostOpened] = useState(false)

  const postAccountAuthAction = async (data: any, errormessage: string) => {
    try {
      await axios.post(accountApiUrl + 'action', data)
      await session.logout(false)
    }
    catch {
      showError({
        title: 'Error',
        message: errormessage,
      })
    }
  }

  const handleAuthenticateLocalhostToggle = () => {
    const data = { operation: 'localhost', enabled: !accounts.localhost }
    postAccountAuthAction(data, i18n.get('AuthenticationServiceNotToggled'))
  }
  const handleAuthenticationToggle = () => {
    const data = { operation: 'authentication', enabled: !accounts.enabled }
    postAccountAuthAction(data, accounts.enabled ? i18n.get('AuthenticationServiceNotDisabled') : i18n.get('AuthenticationServiceNotEnabled'))
  }

  const AuthenticateLocalhostAdmin = () => {
    return accounts.localhost
      ? (
          <Group justify="flex-start" mt="md">
            <Button onClick={() => handleAuthenticateLocalhostToggle()}>{i18n.get('Disable')}</Button>
            <Text>{i18n.get('AuthenticateLocalhostAdminEnabled')}</Text>
          </Group>
        )
      : (
          <Group justify="flex-start" mt="md">
            <Modal
              centered
              opened={localhostOpened}
              onClose={() => setLocalhostOpened(false)}
              title={i18n.get('Warning')}
            >
              <Text>{i18n.get('EnablingAuthenticateLocalhost')}</Text>
              <Group justify="flex-end" mt="md">
                <Button onClick={() => setLocalhostOpened(false)}>{i18n.get('Cancel')}</Button>
                <Button
                  color="red"
                  onClick={() => {
                    setLocalhostOpened(false)
                    handleAuthenticateLocalhostToggle()
                  }}
                >
                  {i18n.get('Confirm')}
                </Button>
              </Group>
            </Modal>
            <Button onClick={() => setLocalhostOpened(true)}>{i18n.get('Enable')}</Button>
            <Text>{i18n.get('AuthenticateLocalhostAdminDisabled')}</Text>
          </Group>
        )
  }

  const AuthenticationServiceButton = () => {
    return accounts.enabled
      ? (
          <Group justify="flex-start" mt="md">
            <Modal
              centered
              opened={authOpened}
              onClose={() => setAuthOpened(false)}
              title={i18n.get('Warning')}
            >
              <Text>{allowHtml(i18n.get('DisablingAuthenticationReduces'))}</Text>
              <Group justify="flex-end" mt="md">
                <Button onClick={() => setAuthOpened(false)}>{i18n.get('Cancel')}</Button>
                <Button
                  color="red"
                  onClick={() => {
                    setAuthOpened(false)
                    handleAuthenticationToggle()
                  }}
                >
                  {i18n.get('Confirm')}
                </Button>
              </Group>
            </Modal>
            <Button onClick={() => setAuthOpened(true)}>{i18n.get('Disable')}</Button>
            <Text>{i18n.get('AuthenticationServiceEnabled')}</Text>
          </Group>
        )
      : (
          <Group justify="flex-start" mt="md">
            <Button onClick={() => handleAuthenticationToggle()}>{i18n.get('Enable')}</Button>
            <Text>{i18n.get('AuthenticationServiceDisabled')}</Text>
          </Group>
        )
  }

  return accounts.enabled
    ? (
        <>
          <AuthenticationServiceButton />
          <AuthenticateLocalhostAdmin />
        </>
      )
    : (
        <AuthenticationServiceButton />
      )
}
