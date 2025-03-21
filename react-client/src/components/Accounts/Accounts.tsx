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
import { Box, Tabs, Text } from '@mantine/core'
import { hideNotification } from '@mantine/notifications'
import axios, { AxiosError } from 'axios'
import { useEffect, useState } from 'react'

import { UmsAccounts } from '../../services/accounts-service'
import { I18nInterface } from '../../services/i18n-service'
import { ServerEventInterface } from '../../services/server-event-service'
import { SessionInterface, UmsPermission } from '../../services/session-service'
import { accountApiUrl } from '../../utils'
import { showError, showLoading, updateError, updateSuccess } from '../../utils/notifications'
import AuthenticationSettings from './AuthenticationSettings'
import GroupAccordion from './GroupAccordion'
import UserAccordion from './UserAccordion'

const Accounts = ({ i18n, session, sse }: { i18n: I18nInterface, session: SessionInterface, sse: ServerEventInterface }) => {
  const [accounts, setAccounts] = useState({ users: [], groups: [], enabled: true, localhost: false } as UmsAccounts)
  const canModifySettings = session.havePermission(UmsPermission.settings_modify)
  const canManageGroups = session.havePermission(UmsPermission.groups_manage)
  const [filled, setFilled] = useState(false)

  useEffect(() => {
    session.useSseAs(Accounts.name)
    session.stopPlayerSse()
    session.setDocumentI18nTitle('ManageAccounts')
    session.setNavbarManage(Accounts.name)
  }, [])

  useEffect(() => {
    if (filled && !sse.updateAccounts) {
      return
    }
    setFilled(true)
    sse.setUpdateAccounts(false)
    axios.get(accountApiUrl + 'accounts')
      .then(function (response: any) {
        setAccounts(response.data)
      })
      .catch(function (error: AxiosError) {
        if (!error.response && error.request) {
          i18n.showServerUnreachable()
        }
        else {
          showError({
            title: i18n.get('Error'),
            message: i18n.get('AccountsNotReceived'),
          })
        }
      })
  }, [sse.updateAccounts, sse.setUpdateAccounts])

  const postAccountAction = (data: any, title: string, message: string, successmessage: string, errormessage: string) => {
    showLoading({
      id: 'account-action',
      title: title,
      message: message,
    })
    return axios.post(accountApiUrl + 'action', data)
      .then(function () {
        updateSuccess({
          id: 'account-action',
          title: title,
          message: successmessage,
        })
      })
      .catch(function (error: AxiosError) {
        if (!error.response && error.request) {
          hideNotification('account-action')
          i18n.showServerUnreachable()
        }
        else {
          updateError({
            id: 'account-action',
            title: i18n.get('Error'),
            message: errormessage,
          })
        }
      })
  }

  return (
    <Box style={{ maxWidth: 1024 }} mx="auto">
      {canManageGroups
        ? (
            <Tabs defaultValue={accounts.enabled && session.authenticate ? 'users' : 'settings'}>
              <Tabs.List>
                {session.authenticate && (
                  <Tabs.Tab value="users">
                    {i18n.get('Users')}
                  </Tabs.Tab>
                )}
                {session.authenticate && (
                  <Tabs.Tab value="groups">
                    {i18n.get('Groups')}
                  </Tabs.Tab>
                )}
                {canModifySettings && (
                  <Tabs.Tab value="settings">
                    {i18n.get('Settings')}
                  </Tabs.Tab>
                )}
              </Tabs.List>
              {session.authenticate && (
                <Tabs.Panel value="users">
                  <UserAccordion i18n={i18n} session={session} accounts={accounts} postAccountAction={postAccountAction} />
                </Tabs.Panel>
              )}
              {session.authenticate && (
                <Tabs.Panel value="groups">
                  <GroupAccordion i18n={i18n} accounts={accounts} postAccountAction={postAccountAction} />
                </Tabs.Panel>
              )}
              {canModifySettings && (
                <Tabs.Panel value="settings">
                  <AuthenticationSettings i18n={i18n} session={session} accounts={accounts} />
                </Tabs.Panel>
              )}
            </Tabs>
          )
        : session.authenticate
          ? (
              <UserAccordion i18n={i18n} session={session} accounts={accounts} postAccountAction={postAccountAction} />
            )
          : (
              <Box style={{ maxWidth: 1024 }} mx="auto">
                <Text c="red">{i18n.get('YouDontHaveAccessArea')}</Text>
              </Box>
            )}
    </Box>
  )
}

export default Accounts
