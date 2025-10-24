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
import { Button, Group, Modal, Text, Switch } from '@mantine/core'
import axios from 'axios'
import { useState, ChangeEvent } from 'react'

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

  const postAccountAuthAction = async (data: Record<string, unknown>, errormessage: string): Promise<boolean> => {
    try {
      await axios.post(accountApiUrl + 'action', data)
      await session.logout(false)
      return true
    }
    catch {
      showError({
        title: 'Error',
        message: errormessage,
      })
      return false
    }
  }

  const handleAuthenticateLocalhostToggle = async (): Promise<boolean> => {
    const data = { operation: 'localhost', enabled: !accounts.localhost }
    return await postAccountAuthAction(data, i18n.get('AuthenticationServiceNotToggled'))
  }
  const handleAuthenticationToggle = async (): Promise<boolean> => {
    const data = { operation: 'authentication', enabled: !accounts.enabled }
    return await postAccountAuthAction(data, accounts.enabled ? i18n.get('AuthenticationServiceNotDisabled') : i18n.get('AuthenticationServiceNotEnabled'))
  }

  const [authLoading, setAuthLoading] = useState(false)
  const [localhostLoading, setLocalhostLoading] = useState(false)

  const AuthenticateLocalhostAdmin = () => {
    const onLocalhostSwitchChange = async (e: ChangeEvent<HTMLInputElement>) => {
      const checked = e.currentTarget.checked
      // original behaviour: enabling shows a confirmation modal, disabling acts immediately
      if (checked) {
        setLocalhostOpened(true)
      }
      else {
        setLocalhostLoading(true)
        await handleAuthenticateLocalhostToggle()
        setLocalhostLoading(false)
      }
    }

    return (
      <>
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
              onClick={async () => {
                setLocalhostOpened(false)
                setLocalhostLoading(true)
                await handleAuthenticateLocalhostToggle()
                setLocalhostLoading(false)
              }}
            >
              {i18n.get('Confirm')}
            </Button>
          </Group>
        </Modal>

        <Group justify="flex-start" mt="md">
          <Switch
            checked={accounts.localhost}
            onChange={onLocalhostSwitchChange}
            disabled={localhostLoading}
            aria-label={i18n.get('AuthenticateLocalhostAdmin')}
          />
          <Text>{accounts.localhost ? i18n.get('AuthenticateLocalhostAdminEnabled') : i18n.get('AuthenticateLocalhostAdminDisabled')}</Text>
        </Group>
      </>
    )
  }

  const AuthenticationServiceButton = () => {
    const onAuthSwitchChange = async (e: ChangeEvent<HTMLInputElement>) => {
      const checked = e.currentTarget.checked
      // original behaviour: disabling shows confirmation, enabling acts immediately
      if (checked) {
        setAuthLoading(true)
        await handleAuthenticationToggle()
        setAuthLoading(false)
      }
      else {
        setAuthOpened(true)
      }
    }

    return (
      <>
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
              onClick={async () => {
                setAuthOpened(false)
                setAuthLoading(true)
                await handleAuthenticationToggle()
                setAuthLoading(false)
              }}
            >
              {i18n.get('Confirm')}
            </Button>
          </Group>
        </Modal>

        <Group justify="flex-start" mt="md">
          <Switch
            checked={accounts.enabled}
            onChange={onAuthSwitchChange}
            disabled={authLoading}
            aria-label={i18n.get('AuthenticationService')}
          />
          <Text>{accounts.enabled ? i18n.get('AuthenticationServiceEnabled') : i18n.get('AuthenticationServiceDisabled')}</Text>
        </Group>
      </>
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
