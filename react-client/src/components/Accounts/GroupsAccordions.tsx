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
import { Accordion, Avatar, Button, Checkbox, Divider, Group, Stack, Text, TextInput } from '@mantine/core'
import { useForm } from '@mantine/form'
import { IconExclamationMark, IconFolder, IconFolderPlus, IconX } from '@tabler/icons-react'
import { useState } from 'react'

import { Permissions, UmsAccounts } from '../../services/accounts-service'
import { I18nInterface } from '../../services/i18n-service'
import { UmsGroup } from '../../services/session-service'

export default function GroupsAccordions({
  i18n,
  accounts,
  postAccountAction,
}: {
  i18n: I18nInterface
  accounts: UmsAccounts
  postAccountAction: (data: any, title: string, message: string, successmessage: string, errormessage: string) => void
}) {
  const [groupOpened, setGroupOpened] = useState<string | null>(null)

  const GroupDisplayNameForm = ({ group }: { group: UmsGroup }) => {
    const groupDisplayNameForm = useForm({ initialValues: { id: group.id, displayName: group.displayName } })
    const handleGroupDisplayNameSubmit = (values: typeof groupDisplayNameForm.values) => {
      const data = { operation: 'modifygroup', groupid: group.id, name: values.displayName }
      postAccountAction(data, i18n.get('DisplayNameUpdate'), i18n.get('DisplayNameUpdating'), i18n.get('DisplayNameUpdated'), i18n.get('DisplayNameNotUpdated'))
    }
    return (
      <form onSubmit={groupDisplayNameForm.onSubmit(handleGroupDisplayNameSubmit)}>
        <Divider my="sm" label={i18n.get('DisplayName')} fz="md" c="var(--mantine-color-text)" />
        <TextInput
          label={i18n.get('DisplayName')}
          name="displayName"
          {...groupDisplayNameForm.getInputProps('displayName')}
        />
        <Group justify="flex-end" mt="md">
          <Button type="submit">
            {i18n.get('Update_verb')}
          </Button>
        </Group>
      </form>
    )
  }

  const GroupPermissionsForm = ({ group }: { group: UmsGroup }) => {
    const [permissions, setPermissions] = useState<number>(group.permissions ? group.permissions.value : 0)
    const addPermission = (permission: number) => {
      setPermissions(permissions | permission)
    }
    const removePermission = (permission: number) => {
      setPermissions(permissions & ~permission)
    }
    const groupPermissionsForm = useForm({ initialValues: { id: group.id } })
    const handleGroupPermissionsSubmit = () => {
      const data = { operation: 'updatepermission', groupid: group.id, permissions: permissions }
      postAccountAction(data, i18n.get('PermissionsUpdate'), i18n.get('PermissionsUpdating'), i18n.get('PermissionsUpdated'), i18n.get('PermissionsNotUpdated'))
    }
    return (
      <form onSubmit={groupPermissionsForm.onSubmit(handleGroupPermissionsSubmit)}>
        <Divider my="sm" label={i18n.get('Permissions')} fz="md" c="var(--mantine-color-text)" />
        <Stack>
          <Checkbox disabled={group.id < 2} label={i18n.get('AllPermissions')} checked={(permissions & Permissions.all) === Permissions.all} onChange={(event: React.ChangeEvent<HTMLInputElement>) => event.currentTarget.checked ? addPermission(Permissions.all) : removePermission(Permissions.all)} />
          <Checkbox disabled={group.id < 2} label={i18n.get('ManageUsers')} checked={(permissions & Permissions.users_manage) === Permissions.users_manage} onChange={(event: React.ChangeEvent<HTMLInputElement>) => event.currentTarget.checked ? addPermission(Permissions.users_manage) : removePermission(Permissions.users_manage)} />
          <Checkbox disabled={group.id < 2} label={i18n.get('ManageGroups')} checked={(permissions & Permissions.groups_manage) === Permissions.groups_manage} onChange={(event: React.ChangeEvent<HTMLInputElement>) => event.currentTarget.checked ? addPermission(Permissions.groups_manage) : removePermission(Permissions.groups_manage)} />
          <Checkbox disabled={group.id < 2} label={i18n.get('ViewSettings')} checked={(permissions & Permissions.settings_view) === Permissions.settings_view} onChange={(event: React.ChangeEvent<HTMLInputElement>) => event.currentTarget.checked ? addPermission(Permissions.settings_view) : removePermission(Permissions.settings_view)} />
          <Checkbox disabled={group.id < 2} label={i18n.get('ModifySettings')} checked={(permissions & Permissions.settings_modify) === Permissions.settings_modify} onChange={(event: React.ChangeEvent<HTMLInputElement>) => event.currentTarget.checked ? addPermission(Permissions.settings_modify) : removePermission(Permissions.settings_modify)} />
          <Checkbox disabled={group.id < 2} label={i18n.get('ControlDevices')} checked={(permissions & Permissions.devices_control) === Permissions.devices_control} onChange={(event: React.ChangeEvent<HTMLInputElement>) => event.currentTarget.checked ? addPermission(Permissions.devices_control) : removePermission(Permissions.devices_control)} />
          <Checkbox disabled={group.id < 2} label={i18n.get('RestartServer')} checked={(permissions & Permissions.server_restart) === Permissions.server_restart} onChange={(event: React.ChangeEvent<HTMLInputElement>) => event.currentTarget.checked ? addPermission(Permissions.server_restart) : removePermission(Permissions.server_restart)} />
          <Checkbox disabled={group.id < 2} label={i18n.get('ShutDownComputer')} checked={(permissions & Permissions.computer_shutdown) === Permissions.computer_shutdown} onChange={(event: React.ChangeEvent<HTMLInputElement>) => event.currentTarget.checked ? addPermission(Permissions.computer_shutdown) : removePermission(Permissions.computer_shutdown)} />
          <Checkbox disabled={group.id < 2} label={i18n.get('RestartApplication')} checked={(permissions & Permissions.application_restart) === Permissions.application_restart} onChange={(event: React.ChangeEvent<HTMLInputElement>) => event.currentTarget.checked ? addPermission(Permissions.application_restart) : removePermission(Permissions.application_restart)} />
          <Checkbox disabled={group.id < 2} label={i18n.get('ShutdownApplication')} checked={(permissions & Permissions.application_shutdown) === Permissions.application_shutdown} onChange={(event: React.ChangeEvent<HTMLInputElement>) => event.currentTarget.checked ? addPermission(Permissions.application_shutdown) : removePermission(Permissions.application_shutdown)} />
          <Checkbox disabled={group.id < 2} label={i18n.get('BrowseWebPlayer')} checked={(permissions & Permissions.web_player_browse) === Permissions.web_player_browse} onChange={(event: React.ChangeEvent<HTMLInputElement>) => event.currentTarget.checked ? addPermission(Permissions.web_player_browse) : removePermission(Permissions.web_player_browse)} />
          <Checkbox disabled={group.id < 2} label={i18n.get('DownloadWebPlayer')} checked={(permissions & Permissions.web_player_download) === Permissions.web_player_download} onChange={(event: React.ChangeEvent<HTMLInputElement>) => event.currentTarget.checked ? addPermission(Permissions.web_player_download) : removePermission(Permissions.web_player_download)} />
          <Checkbox disabled={group.id < 2} label={i18n.get('EditMetadataWebPlayer')} checked={(permissions & Permissions.web_player_edit) === Permissions.web_player_edit} onChange={(event: React.ChangeEvent<HTMLInputElement>) => event.currentTarget.checked ? addPermission(Permissions.web_player_edit) : removePermission(Permissions.web_player_edit)} />
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

  const GroupDeleteForm = ({ group }: { group: UmsGroup }) => {
    const groupDeleteForm = useForm({ initialValues: { id: group.id } })
    const handleGroupDeleteSubmit = () => {
      const data = { operation: 'deletegroup', groupid: group.id }
      postAccountAction(data, i18n.get('GroupDeletion'), i18n.get('GroupDeleting'), i18n.get('GroupDeleted'), i18n.get('GroupNotDeleted'))
    }
    const [opened, setOpened] = useState(false)
    return group.id > 1
      ? (
          <form onSubmit={groupDeleteForm.onSubmit(handleGroupDeleteSubmit)}>
            <Divider my="sm" />
            {opened
              ? (
                  <Group justify="flex-end" mt="md">
                    <Text c="red">{i18n.get('WarningGroupWillBeDeleted')}</Text>
                    <Button onClick={() => setOpened(false)}>
                      {i18n.get('Cancel')}
                    </Button>
                    <Button type="submit" color="red" leftSection={<IconExclamationMark />} rightSection={<IconExclamationMark />}>
                      {i18n.get('Confirm')}
                    </Button>
                  </Group>
                )
              : (
                  <Group justify="flex-end" mt="md">
                    <Text c="red">{i18n.get('DeleteGroup')}</Text>
                    <Button onClick={() => setOpened(true)} color="red" leftSection={<IconX />}>
                      {i18n.get('Delete')}
                    </Button>
                  </Group>
                )}
          </form>
        )
      : null
  }

  const NewGroupForm = () => {
    const newGroupForm = useForm({ initialValues: { displayName: '' } })
    const handleNewGroupSubmit = (values: typeof newGroupForm.values) => {
      const data = { operation: 'creategroup', name: values.displayName }
      postAccountAction(data, i18n.get('GroupCreation'), i18n.get('NewGroupCreating'), i18n.get('NewGroupCreated'), i18n.get('NewGroupNotCreated'))
    }
    return (
      <form onSubmit={newGroupForm.onSubmit(handleNewGroupSubmit)}>
        <TextInput
          required
          label={i18n.get('DisplayName')}
          name="displayName"
          {...newGroupForm.getInputProps('displayName')}
        />
        <Group justify="flex-end" mt="md">
          <Button type="submit">
            {i18n.get('Create')}
          </Button>
        </Group>
      </form>
    )
  }

  const GroupAccordionLabel = ({ group }: { group: UmsGroup }) => {
    return (
      <Group wrap="nowrap">
        <Avatar radius="xl" size="lg">
          {group.id === 0 ? (<IconFolderPlus size={24} />) : (<IconFolder size={24} />)}
        </Avatar>
        <Text>{group.displayName}</Text>
      </Group>
    )
  }

  const GroupAccordion = ({ group }: { group: UmsGroup }) => {
    return group.id > 0
      ? (
          <Accordion.Item value={group.id.toString()} key={group.id}>
            <Accordion.Control><GroupAccordionLabel group={group} /></Accordion.Control>
            <Accordion.Panel>
              <GroupDisplayNameForm group={group} />
              <GroupPermissionsForm group={group} />
              <GroupDeleteForm group={group} />
            </Accordion.Panel>
          </Accordion.Item>
        )
      : null
  }

  const GroupsAccordions = accounts.groups.map((group) => {
    return <GroupAccordion group={group} />
  })

  const NewGroupAccordion = () => {
    const group = { id: 0, displayName: i18n.get('NewGroup') } as UmsGroup
    return (
      <Accordion.Item value={group.id.toString()} key={group.id}>
        <Accordion.Control><GroupAccordionLabel group={group} /></Accordion.Control>
        <Accordion.Panel><NewGroupForm /></Accordion.Panel>
      </Accordion.Item>
    )
  }

  return (
    <Accordion value={groupOpened} onChange={setGroupOpened}>
      <NewGroupAccordion />
      {GroupsAccordions}
    </Accordion>
  )
}
