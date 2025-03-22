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

import { I18nInterface, ValueLabelData } from '../../services/i18n-service'
import { UmsGroup, UmsUser } from '../../services/session-service'
import UserAccordionLabel from './UserAccordionLabel'
import UserAddForm from './UserAddForm'

export default function UserAddAccordionItem({
  i18n,
  canManageUsers,
  groupSelectionDatas,
  postAccountAction,
}: {
  i18n: I18nInterface
  canManageUsers: boolean
  groupSelectionDatas: ValueLabelData[]
  postAccountAction: (data: any, title: string, message: string, successmessage: string, errormessage: string) => void
}) {
  const user = { id: 0, username: i18n.get('NewUser') } as UmsUser
  const group = { id: 0, displayName: '' } as UmsGroup
  return canManageUsers
    ? (
        <Accordion.Item value={user.id.toString()} key={user.id}>
          <Accordion.Control><UserAccordionLabel user={user} group={group} /></Accordion.Control>
          <Accordion.Panel><UserAddForm i18n={i18n} groupSelectionDatas={groupSelectionDatas} postAccountAction={postAccountAction} /></Accordion.Panel>
        </Accordion.Item>
      )
    : undefined
}
