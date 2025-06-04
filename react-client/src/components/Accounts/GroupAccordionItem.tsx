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

import { I18nInterface } from '../../services/i18n-service'
import { UmsGroup } from '../../services/session-service'
import GroupAccordionLabel from './GroupAccordionLabel'
import GroupDeleteForm from './GroupDeleteForm'
import GroupDisplayNameForm from './GroupDisplayNameForm'
import GroupPermissionsForm from './GroupPermissionsForm'

export default function GroupAccordionItem({
  i18n,
  group,
  postAccountAction,
}:
{
  i18n: I18nInterface
  group: UmsGroup
  postAccountAction: (data: Record<string, unknown>, title: string, message: string, successmessage: string, errormessage: string) => void
}) {
  return group.id > 0
    ? (
        <Accordion.Item value={group.id.toString()} key={group.id}>
          <Accordion.Control><GroupAccordionLabel group={group} /></Accordion.Control>
          <Accordion.Panel>
            <GroupDisplayNameForm i18n={i18n} group={group} postAccountAction={postAccountAction} />
            <GroupPermissionsForm i18n={i18n} group={group} postAccountAction={postAccountAction} />
            <GroupDeleteForm i18n={i18n} group={group} postAccountAction={postAccountAction} />
          </Accordion.Panel>
        </Accordion.Item>
      )
    : undefined
}
