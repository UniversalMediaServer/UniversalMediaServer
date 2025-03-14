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
import { Accordion, Avatar, Button, Divider, Group, HoverCard, Input, PasswordInput, Select, Text, TextInput } from '@mantine/core'
import { Dropzone, FileWithPath, IMAGE_MIME_TYPE } from '@mantine/dropzone'
import { useForm } from '@mantine/form'
import { IconExclamationMark, IconPhotoUp, IconPhotoX, IconUser, IconUserPlus, IconX } from '@tabler/icons-react'
import { useState } from 'react'

import { getUserGroup, getUserGroupsSelection, havePermission, Permissions, UmsAccounts } from '../../services/accounts-service'
import { I18nInterface } from '../../services/i18n-service'
import { SessionInterface, UmsGroup, UmsUser } from '../../services/session-service'

export default function UsersAccordions({
  i18n,
  session,
  accounts,
  postAccountAction,
}: {
  i18n: I18nInterface
  session: SessionInterface
  accounts: UmsAccounts
  postAccountAction: (data: any, title: string, message: string, successmessage: string, errormessage: string) => void
}) {
  const [userOpened, setUserOpened] = useState<string | null>(null)
  const canManageUsers = havePermission(session, Permissions.users_manage)
  const canManageGroups = havePermission(session, Permissions.groups_manage)
  const groupSelectionDatas = getUserGroupsSelection(accounts.groups, i18n.get('None'))

  const UserAccordionLabel = ({ user, group }: { user: UmsUser, group: UmsGroup }) => {
    const showAsUsername = (user.displayName == null || user.displayName.length === 0 || user.displayName === user.username)
    const groupDisplayName = group.displayName.length !== 0 ? ' ('.concat(group.displayName).concat(')') : ''
    const displayName = showAsUsername ? user.username : user.displayName
    return (
      <Group wrap="nowrap">
        <Avatar radius="xl" size="lg" src={user.avatar}>
          {!user.avatar && (user.id === 0
            ? (<IconUserPlus size={24} />)
            : (<IconUser size={24} />)
          )}
        </Avatar>
        <div>
          <Text>
            {displayName}
            {groupDisplayName}
          </Text>
          {!showAsUsername && (
            <Text size="sm" c="dimmed" fw={400}>
              {user.username}
            </Text>
          )}
        </div>
      </Group>
    )
  }

  const NewUserForm = ({ i18n }: { i18n: I18nInterface }) => {
    const newUserForm = useForm({ initialValues: { username: '', password: '', groupid: '0', displayname: '' } })
    const handleNewUserSubmit = (values: typeof newUserForm.values) => {
      const data = { operation: 'createuser', username: values.username, password: values.password, groupid: values.groupid, displayname: values.displayname }
      postAccountAction(data, i18n.get('UserCreation'), i18n.get('NewUserCreating'), i18n.get('NewUserCreated'), i18n.get('NewUserNotCreated'))
    }
    return (
      <form onSubmit={newUserForm.onSubmit(handleNewUserSubmit)}>
        <TextInput
          required
          label={i18n.get('Username')}
          name="username"
          {...newUserForm.getInputProps('username')}
        />
        <PasswordInput
          required
          label={i18n.get('Password')}
          name="username"
          type="password"
          {...newUserForm.getInputProps('password')}
        />
        <TextInput
          required
          label={i18n.get('DisplayName')}
          name="displayname"
          {...newUserForm.getInputProps('displayname')}
        />
        <Select
          required
          label={i18n.get('Group')}
          name="groupId"
          data={groupSelectionDatas}
          {...newUserForm.getInputProps('groupid')}
        />
        {newUserForm.isValid() && (
          <Group justify="flex-end" mt="md">
            <Button type="submit">
              {i18n.get('Create')}
            </Button>
          </Group>
        )}
      </form>
    )
  }

  const UserIdentityForm = ({ user }: { user: UmsUser }) => {
    const userIdentityForm = useForm({ initialValues: { id: user.id, username: user.username, password: '' } })
    const handleUserIdentitySubmit = (values: typeof userIdentityForm.values) => {
      const data = { operation: 'changelogin', userid: user.id, username: values.username, password: values.password }
      postAccountAction(data, i18n.get('CredentialsUpdate'), i18n.get('CredentialsUpdating'), i18n.get('CredentialsUpdated'), i18n.get('CredentialsNotUpdated'))
    }
    return (
      <form onSubmit={userIdentityForm.onSubmit(handleUserIdentitySubmit)}>
        <Divider my="sm" label={i18n.get('Credentials')} fz="md" c="var(--mantine-color-text)" />
        <TextInput
          required
          label={i18n.get('Username')}
          name="username"
          {...userIdentityForm.getInputProps('username')}
        />
        <PasswordInput
          required
          label={i18n.get('Password')}
          name="username"
          type="password"
          {...userIdentityForm.getInputProps('password')}
        />
        <Group justify="flex-end" mt="md">
          <Button type="submit">
            {i18n.get('Apply')}
          </Button>
        </Group>
      </form>
    )
  }

  const UserProfileForm = ({ user }: { user: UmsUser }) => {
    const [avatar, setAvatar] = useState<string>(user.avatar ? user.avatar : '')
    const userProfileForm = useForm({ initialValues: { id: user.id, displayName: user.displayName, avatar: user.avatar ? user.avatar : '', pinCode: user.pinCode ? user.pinCode : '', libraryHidden: user.libraryHidden } })
    const handleUserProfileSubmit = (values: typeof userProfileForm.values) => {
      const data = { operation: 'modifyuser', userid: user.id, name: values.displayName } as any
      if (userProfileForm.isDirty('displayName')) data.name = values.displayName
      if (userProfileForm.isDirty('avatar')) data.avatar = values.avatar
      if (userProfileForm.isDirty('pinCode')) data.pincode = values.pinCode
      if (userProfileForm.isDirty('libraryHidden')) data.library_hidden = values.libraryHidden
      postAccountAction(data, i18n.get('UserProfileUpdate'), i18n.get('UserProfileUpdating'), i18n.get('UserProfileUpdated'), i18n.get('UserProfileNotUpdated'))
    }
    return (
      <form onSubmit={userProfileForm.onSubmit(handleUserProfileSubmit)}>
        <Divider my="sm" label={i18n.get('Profile')} fz="md" c="var(--mantine-color-text)" />
        <TextInput
          label={i18n.get('DisplayName')}
          name="displayName"
          {...userProfileForm.getInputProps('displayName')}
        />
        <Input.Wrapper label={i18n.get('Avatar')}>
          <Dropzone
            name="avatar"
            maxSize={2 * 1024 ** 2}
            accept={IMAGE_MIME_TYPE}
            multiple={false}
            styles={{ inner: { pointerEvents: 'all' } }}
            onDrop={(files: FileWithPath[]) => {
              const reader = new FileReader()
              reader.onload = () => {
                const result = reader.result as string
                if (result) {
                  setAvatar(result)
                  userProfileForm.setFieldValue('avatar', result)
                }
              }
              if (files[0]) {
                reader.readAsDataURL(files[0])
              }
            }}
          >
            <Group justify="center">
              <Dropzone.Accept>
                <IconPhotoUp />
              </Dropzone.Accept>
              <Dropzone.Reject>
                <IconPhotoX />
              </Dropzone.Reject>
              <Dropzone.Idle>
                <div onClick={(e) => { e.stopPropagation() }}>
                  <HoverCard disabled={avatar === ''}>
                    <HoverCard.Target>
                      <Avatar radius="xl" size="lg" src={avatar !== '' ? avatar : null}>
                        {avatar === '' && <IconUser size={24} />}
                      </Avatar>
                    </HoverCard.Target>
                    <HoverCard.Dropdown>
                      <Button
                        onClick={() => {
                          const newavatar = (user.avatar && avatar !== user.avatar) ? user.avatar : ''
                          setAvatar(newavatar)
                          userProfileForm.setFieldValue('avatar', newavatar)
                        }}
                      >
                        Delete
                      </Button>
                    </HoverCard.Dropdown>
                  </HoverCard>
                </div>
              </Dropzone.Idle>
              <div>
                <Text inline>
                  Drag image here or click to select file
                </Text>
                <Text size="sm" c="dimmed" inline mt={7}>
                  File should not exceed 2mb
                </Text>
              </div>
            </Group>
          </Dropzone>
        </Input.Wrapper>
        {/* removed until root user choice is implemented
        <Input.Wrapper label={i18n.get('PinCode')}>
          <PinInput
            name='pincode'
            type='number'
            oneTimeCode
            {...userProfileForm.getInputProps('pinCode')}
          />
        </Input.Wrapper>
        <Tooltip label={allowHtml(i18n.get('HideUserChoiceLibrary'])} {...defaultTooltipSettings}>
          <Checkbox
            mt='xl'
            label={i18n.get('HideUserLibrary')}
            {...userProfileForm.getInputProps('library_hidden', { type: 'checkbox' })}
          />
        </Tooltip>
        */}
        {userProfileForm.isDirty() && (
          <Group justify="flex-end" mt="md">
            <Button type="submit">
              {i18n.get('Apply')}
            </Button>
          </Group>
        )}
      </form>
    )
  }

  const UserGroupForm = ({ user }: { user: UmsUser }) => {
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

  const UserDeleteForm = ({ user }: { user: UmsUser }) => {
    const userDeleteForm = useForm({ initialValues: { id: user.id } })
    const handleUserDeleteSubmit = () => {
      const data = { operation: 'deleteuser', userid: user.id }
      postAccountAction(data, i18n.get('UserDeletion'), i18n.get('UserDeleting'), i18n.get('UserDeleted'), i18n.get('UserNotDeleted'))
    }
    const [opened, setOpened] = useState(false)
    return (
      <form onSubmit={userDeleteForm.onSubmit(handleUserDeleteSubmit)}>
        <Divider my="sm" />
        {opened
          ? (
              <Group justify="flex-end" mt="md">
                <Text c="red">{i18n.get('WarningUserWillBeDeleted')}</Text>
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
                <Text c="red">{i18n.get('DeleteUser')}</Text>
                <Button onClick={() => setOpened(true)} color="red" leftSection={<IconX />}>
                  {i18n.get('Delete')}
                </Button>
              </Group>
            )}
      </form>
    )
  }

  const NewUserAccordion = ({ i18n }: { i18n: I18nInterface }) => {
    const user = { id: 0, username: i18n.get('NewUser') } as UmsUser
    const userGroup = { id: 0, displayName: '' } as UmsGroup
    return canManageUsers
      ? (
          <Accordion.Item value={user.id.toString()}>
            <Accordion.Control><UserAccordionLabel user={user} group={userGroup} /></Accordion.Control>
            <Accordion.Panel><NewUserForm i18n={i18n} /></Accordion.Panel>
          </Accordion.Item>
        )
      : null
  }

  const UserAccordion = ({ user }: { user: UmsUser }) => {
    const userGroup = getUserGroup(user, accounts)
    return (
      <Accordion.Item value={user.id.toString()}>
        <Accordion.Control><UserAccordionLabel user={user} group={userGroup} /></Accordion.Control>
        <Accordion.Panel>
          <UserIdentityForm user={user} />
          <UserProfileForm user={user} />
          <UserGroupForm user={user} />
          <UserDeleteForm user={user} />
        </Accordion.Panel>
      </Accordion.Item>
    )
  }

  const UserAccordions = accounts.users.map((user) => {
    return <UserAccordion user={user} key={user.id} />
  })

  return (
    <Accordion value={userOpened} onChange={setUserOpened}>
      <NewUserAccordion i18n={i18n} />
      {UserAccordions}
    </Accordion>
  )
}
