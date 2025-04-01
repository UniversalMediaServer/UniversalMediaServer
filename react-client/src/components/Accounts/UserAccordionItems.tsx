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
import { UmsAccounts } from '../../services/accounts-service'
import { I18nInterface, ValueLabelData } from '../../services/i18n-service'
import UserAccordionItem from './UserAccordionItem'

export default function UserAccordionItems({
  i18n,
  accounts,
  canManageGroups,
  groupSelectionDatas,
  postAccountAction,
}: {
  i18n: I18nInterface
  accounts: UmsAccounts
  canManageGroups: boolean
  groupSelectionDatas: ValueLabelData[]
  postAccountAction: (data: Record<string, unknown>, title: string, message: string, successmessage: string, errormessage: string) => void
}) {
  return accounts.users.map((user) => {
    return <UserAccordionItem key={user.id} i18n={i18n} user={user} accounts={accounts} postAccountAction={postAccountAction} canManageGroups={canManageGroups} groupSelectionDatas={groupSelectionDatas} />
  })
}
