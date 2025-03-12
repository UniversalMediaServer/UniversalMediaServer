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
import axios from 'axios'
import { useEffect, useState } from 'react'

import { havePermission, Permissions } from '../../services/accounts-service'
import { I18nInterface } from '../../services/i18n-service'
import { MainInterface } from '../../services/main-service'
import { ServerEventInterface } from '../../services/server-event-service'
import { UmsAccounts } from '../../services/accounts-service'
import { SessionInterface } from '../../services/session-service'
import { accountApiUrl } from '../../utils'
import { showError, showLoading, updateError, updateSuccess } from '../../utils/notifications'
import AuthenticationSettings from './AuthenticationSettings'
import GroupsAccordions from './GroupsAccordions'
import UsersAccordions from './UsersAccordions'

const Accounts = ({ i18n, main, session, sse }: { i18n: I18nInterface, main: MainInterface, session: SessionInterface, sse: ServerEventInterface }) => {
  const [accounts, setAccounts] = useState({ users: [], groups: [], enabled: true, localhost: false } as UmsAccounts)
  const canModifySettings = havePermission(session, Permissions.settings_modify)
  const canManageGroups = havePermission(session, Permissions.groups_manage)
  const [filled, setFilled] = useState(false)

  useEffect(() => {
    session.useSseAs(Accounts.name)
    session.stopPlayerSse()
  }, [])

  useEffect(() => {
    session.setDocumentTitle(i18n.get('ManageAccounts'))
    main.setNavbarItem(i18n, session, Accounts.name)
  }, [i18n, session.account])

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
      .catch(function () {
        showError({
          title: i18n.get('Error'),
          message: i18n.get('AccountsNotReceived'),
        })
      })
  }, [sse])

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
      .catch(function () {
        updateError({
          id: 'account-action',
          title: i18n.get('Error'),
          message: errormessage,
        })
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
                  <UsersAccordions i18n={i18n} session={session} accounts={accounts} postAccountAction={postAccountAction} />
                </Tabs.Panel>
              )}
              {session.authenticate && (
                <Tabs.Panel value="groups">
                  <GroupsAccordions i18n={i18n} accounts={accounts} postAccountAction={postAccountAction} />
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
              <UsersAccordions i18n={i18n} session={session} accounts={accounts} postAccountAction={postAccountAction} />
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
