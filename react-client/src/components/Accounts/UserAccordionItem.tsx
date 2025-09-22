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

import { getUserGroup, UmsAccounts } from '../../services/accounts-service'
import { I18nInterface, ValueLabelData } from '../../services/i18n-service'
import { UmsUser } from '../../services/session-service'
import UserAccordionLabel from './UserAccordionLabel'
import UserDeleteForm from './UserDeleteForm'
import UserGroupForm from './UserGroupForm'
import UserIdentityForm from './UserIdentityForm'
import UserProfileForm from './UserProfileForm'

export default function UserAccordionItem({
  i18n,
  user,
  accounts,
  canManageGroups,
  groupSelectionDatas,
  postAccountAction,
}: {
  i18n: I18nInterface
  user: UmsUser
  accounts: UmsAccounts
  canManageGroups: boolean
  groupSelectionDatas: ValueLabelData[]
  postAccountAction: (data: Record<string, unknown>, title: string, message: string, successmessage: string, errormessage: string) => void
}) {
  const userGroup = getUserGroup(user, accounts)
  return (
    <Accordion.Item value={user.id.toString()} key={user.id}>
      <Accordion.Control><UserAccordionLabel user={user} group={userGroup} /></Accordion.Control>
      <Accordion.Panel>
        <UserIdentityForm i18n={i18n} user={user} postAccountAction={postAccountAction} />
        <UserProfileForm i18n={i18n} user={user} postAccountAction={postAccountAction} />
        <UserGroupForm i18n={i18n} user={user} postAccountAction={postAccountAction} canManageGroups={canManageGroups} groupSelectionDatas={groupSelectionDatas} />
        <UserDeleteForm i18n={i18n} user={user} postAccountAction={postAccountAction} />
      </Accordion.Panel>
    </Accordion.Item>
  )
}
