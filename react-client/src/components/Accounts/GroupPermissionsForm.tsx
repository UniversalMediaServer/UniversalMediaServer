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
import { Button, Checkbox, Divider, Group, Stack } from '@mantine/core'
import { useForm } from '@mantine/form'

import { I18nInterface } from '../../services/i18n-service'
import { UmsGroup, UmsPermission } from '../../services/session-service'
import { useState } from 'react'

export default function GroupPermissionsForm({
  i18n,
  group,
  postAccountAction,
}:
{
  i18n: I18nInterface
  group: UmsGroup
  postAccountAction: (data: any, title: string, message: string, successmessage: string, errormessage: string) => void
}) {
  const [permissions, setPermissions] = useState<number>(group.permissions ? group.permissions.value : 0)
  const addPermission = (permission: number) => {
    setPermissions(permissions | permission)
  }
  const removePermission = (permission: number) => {
    setPermissions(permissions & ~permission)
  }
  const hasPermission = (permission: number) => {
    return (permissions & permission) === permission
  }
  const groupPermissionsForm = useForm({ initialValues: { id: group.id } })
  const handleGroupPermissionsSubmit = () => {
    const data = { operation: 'updatepermission', groupid: group.id, permissions: permissions }
    postAccountAction(data, i18n.get('PermissionsUpdate'), i18n.get('PermissionsUpdating'), i18n.get('PermissionsUpdated'), i18n.get('PermissionsNotUpdated'))
  }
  const PermissionCheckbox = ({ label, permission }: { label: string, permission: number }) => {
    return (
      <Checkbox
        disabled={group.id < 2}
        label={label}
        checked={hasPermission(permission)}
        onChange={(event: React.ChangeEvent<HTMLInputElement>) =>
          event.currentTarget.checked
            ? addPermission(permission)
            : removePermission(permission)}
      />
    )
  }

  return (
    <form onSubmit={groupPermissionsForm.onSubmit(handleGroupPermissionsSubmit)}>
      <Divider my="sm" label={i18n.get('Permissions')} fz="md" c="var(--mantine-color-text)" />
      <Stack>
        <PermissionCheckbox label={i18n.get('AllPermissions')} permission={UmsPermission.all} />
        <PermissionCheckbox label={i18n.get('ManageUsers')} permission={UmsPermission.users_manage} />
        <PermissionCheckbox label={i18n.get('ManageGroups')} permission={UmsPermission.groups_manage} />
        <PermissionCheckbox label={i18n.get('ViewSettings')} permission={UmsPermission.settings_view} />
        <PermissionCheckbox label={i18n.get('ModifySettings')} permission={UmsPermission.settings_modify} />
        <PermissionCheckbox label={i18n.get('ControlDevices')} permission={UmsPermission.devices_control} />
        <PermissionCheckbox label={i18n.get('RestartServer')} permission={UmsPermission.server_restart} />
        <PermissionCheckbox label={i18n.get('ShutDownComputer')} permission={UmsPermission.computer_shutdown} />
        <PermissionCheckbox label={i18n.get('RestartApplication')} permission={UmsPermission.application_restart} />
        <PermissionCheckbox label={i18n.get('ShutdownApplication')} permission={UmsPermission.application_shutdown} />
        <PermissionCheckbox label={i18n.get('BrowseWebPlayer')} permission={UmsPermission.web_player_browse} />
        <PermissionCheckbox label={i18n.get('DownloadWebPlayer')} permission={UmsPermission.web_player_download} />
        <PermissionCheckbox label={i18n.get('EditMetadataWebPlayer')} permission={UmsPermission.web_player_edit} />
      </Stack>
      {group.id > 1 && (
        <Group justify="flex-end" mt="md">
          <Button type="submit">
            {i18n.get('Update_verb')}
          </Button>
        </Group>
      )}
    </form>
  )
}
