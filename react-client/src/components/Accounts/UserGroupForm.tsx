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
import { Button, Divider, Group, Select } from '@mantine/core'
import { useForm } from '@mantine/form'

import { I18nInterface, ValueLabelData } from '../../services/i18n-service'
import { UmsUser } from '../../services/session-service'

export default function UserGroupForm({
  i18n,
  user,
  canManageGroups,
  groupSelectionDatas,
  postAccountAction,
}:
{
  i18n: I18nInterface
  canManageGroups: boolean
  groupSelectionDatas: ValueLabelData[]
  user: UmsUser
  postAccountAction: (data: Record<string, unknown>, title: string, message: string, successmessage: string, errormessage: string) => void
}) {
  const userGroupForm = useForm({ initialValues: { id: user.id, groupId: user.groupId.toString() } })
  const handleUserGroupSubmit = (values: typeof userGroupForm.values) => {
    const data = { operation: 'modifyuser', userid: user.id, groupid: values.groupId }
    postAccountAction(data, i18n.get('UserGroupChange'), i18n.get('UserGroupChanging'), i18n.get('UserGroupChanged'), i18n.get('UserGroupNotChanged'))
  }
  return (
    <form onSubmit={userGroupForm.onSubmit(handleUserGroupSubmit)}>
      <Divider my="sm" label={i18n.get('Group')} fz="md" c="var(--mantine-color-text)" />
      <Select
        label={i18n.get('Group')}
        name="groupId"
        disabled={!canManageGroups}
        data={groupSelectionDatas}
        comboboxProps={{ withinPortal: true }}
        {...userGroupForm.getInputProps('groupId')}
      />
      {canManageGroups && userGroupForm.isDirty() && (
        <Group justify="flex-end" mt="md">
          <Button type="submit">
            {i18n.get('Apply')}
          </Button>
        </Group>
      )}
    </form>
  )
}
