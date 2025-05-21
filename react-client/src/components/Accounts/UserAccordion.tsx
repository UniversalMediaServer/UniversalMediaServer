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
import { Accordion } from '@mantine/core'

import { getUserGroupsSelection, UmsAccounts } from '../../services/accounts-service'
import { I18nInterface } from '../../services/i18n-service'
import { SessionInterface, UmsPermission } from '../../services/session-service'
import UserAccordionItems from './UserAccordionItems'
import UserAddAccordionItem from './UserAddAccordionItem'

export default function UserAccordion({
  i18n,
  session,
  accounts,
  postAccountAction,
}: {
  i18n: I18nInterface
  session: SessionInterface
  accounts: UmsAccounts
  postAccountAction: (data: Record<string, unknown>, title: string, message: string, successmessage: string, errormessage: string) => void
}) {
  const canManageUsers = session.havePermission(UmsPermission.users_manage)
  const canManageGroups = session.havePermission(UmsPermission.groups_manage)
  const groupSelectionDatas = getUserGroupsSelection(accounts.groups, i18n.get('None'))

  return (
    <Accordion>
      <UserAddAccordionItem i18n={i18n} canManageUsers={canManageUsers} groupSelectionDatas={groupSelectionDatas} postAccountAction={postAccountAction} />
      <UserAccordionItems i18n={i18n} accounts={accounts} postAccountAction={postAccountAction} canManageGroups={canManageGroups} groupSelectionDatas={groupSelectionDatas} />
    </Accordion>
  )
}
