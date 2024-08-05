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
import { Accordion, Avatar, Box, Button, Checkbox, Divider, Group, HoverCard, Input, Modal, PasswordInput, Select, Stack, Tabs, Text, TextInput } from '@mantine/core';
import { Dropzone, FileWithPath, IMAGE_MIME_TYPE } from '@mantine/dropzone';
import { useForm } from '@mantine/form';
import { useContext, useState } from 'react';
import { ExclamationMark, Folder, FolderPlus, PhotoUp, PhotoX, User, UserPlus, X } from 'tabler-icons-react';

import AccountsContext from '../../contexts/accounts-context';
import I18nContext from '../../contexts/i18n-context';
import SessionContext, { UmsGroup, UmsUser } from '../../contexts/session-context';
import { getUserGroup, getUserGroupsSelection, havePermission, Permissions, postAccountAction, postAccountAuthAction } from '../../services/accounts-service';
import { allowHtml } from '../../utils';

const Accounts = () => {
  const i18n = useContext(I18nContext);
  const session = useContext(SessionContext);
  const accounts = useContext(AccountsContext);
  const groupSelectionDatas = getUserGroupsSelection(accounts.groups, i18n.get('None'));
  const canModifySettings = havePermission(session, Permissions.settings_modify);
  const canManageUsers = havePermission(session, Permissions.users_manage);
  const canManageGroups = havePermission(session, Permissions.groups_manage);
  const [localhostOpened, setLocalhostOpened] = useState(false);
  const [authOpened, setAuthOpened] = useState(false);
  const [userOpened, setUserOpened] = useState<string | null>(null);
  const [groupOpened, setGroupOpened] = useState<string | null>(null);

  const UserAccordionLabel = (user: UmsUser, group: UmsGroup) => {
    const showAsUsername = (user.displayName == null || user.displayName.length === 0 || user.displayName === user.username);
    const groupDisplayName = group.displayName.length !== 0 ? ' ('.concat(group.displayName).concat(')') : '';
    const displayName = showAsUsername ? user.username : user.displayName;
    return (
      <Group wrap='nowrap'>
        <Avatar radius='xl' size='lg' src={user.avatar}>
          {!user.avatar && (user.id === 0 ? (<UserPlus size={24} />) : (<User size={24} />))}
        </Avatar>
        <div>
          <Text>{displayName}{groupDisplayName}</Text>
          {!showAsUsername && (
            <Text size='sm' c='dimmed' fw={400}>
              {user.username}
            </Text>
          )}
        </div>
      </Group>
    );
  }

  const NewUserForm = () => {
    const newUserForm = useForm({ initialValues: { username: null, password: null, groupid: '0', displayname: null } });
    const handleNewUserSubmit = (values: typeof newUserForm.values) => {
      const data = { operation: 'createuser', username: values.username, password: values.password, groupid: values.groupid, displayname: values.displayname };
      postAccountAction(data, i18n.get('UserCreation'), i18n.get('NewUserCreating'), i18n.get('NewUserCreated'), i18n.get('NewUserNotCreated'));
    }
    return (
      <form onSubmit={newUserForm.onSubmit(handleNewUserSubmit)}>
        <TextInput
          required
          label={i18n.get('Username')}
          name='username'
          {...newUserForm.getInputProps('username')}
        />
        <PasswordInput
          required
          label={i18n.get('Password')}
          name='username'
          type='password'
          {...newUserForm.getInputProps('password')}
        />
        <TextInput
          required
          label={i18n.get('DisplayName')}
          name='displayname'
          {...newUserForm.getInputProps('displayname')}
        />
        <Select
          required
          label={i18n.get('Group')}
          name='groupId'
          data={groupSelectionDatas}
          {...newUserForm.getInputProps('groupid')}
        />
        {newUserForm.isValid() && (
          <Group justify='flex-end' mt='md'>
            <Button type='submit'>
              {i18n.get('Create')}
            </Button>
          </Group>
        )}
      </form>
    )
  }

  const UserIdentityForm = (user: UmsUser) => {
    const userIdentityForm = useForm({ initialValues: { id: user.id, username: user.username, password: '' } });
    const handleUserIdentitySubmit = (values: typeof userIdentityForm.values) => {
      const data = { operation: 'changelogin', userid: user.id, username: values.username, password: values.password };
      postAccountAction(data, i18n.get('CredentialsUpdate'), i18n.get('CredentialsUpdating'), i18n.get('CredentialsUpdated'), i18n.get('CredentialsNotUpdated'));
    }
    return (
      <form onSubmit={userIdentityForm.onSubmit(handleUserIdentitySubmit)}>
        <Divider my='sm' label={i18n.get('Credentials')} fz='md' c={'var(--mantine-color-text)'} />
        <TextInput
          required
          label={i18n.get('Username')}
          name='username'
          {...userIdentityForm.getInputProps('username')}
        />
        <PasswordInput
          required
          label={i18n.get('Password')}
          name='username'
          type='password'
          {...userIdentityForm.getInputProps('password')}
        />
        <Group justify='flex-end' mt='md'>
          <Button type='submit'>
            {i18n.get('Apply')}
          </Button>
        </Group>
      </form>
    )
  }

  const UserProfileForm = (user: UmsUser) => {
    const [avatar, setAvatar] = useState<string>(user.avatar ? user.avatar : '');
    const userProfileForm = useForm({ initialValues: { id: user.id, displayName: user.displayName, avatar: user.avatar ? user.avatar : '', pinCode: user.pinCode ? user.pinCode : '', libraryHidden: user.libraryHidden } });
    const handleUserProfileSubmit = (values: typeof userProfileForm.values) => {
      const data = { operation: 'modifyuser', userid: user.id, name: values.displayName } as any;
      if (userProfileForm.isDirty('displayName')) data.name = values.displayName;
      if (userProfileForm.isDirty('avatar')) data.avatar = values.avatar;
      if (userProfileForm.isDirty('pinCode')) data.pincode = values.pinCode;
      if (userProfileForm.isDirty('libraryHidden')) data.library_hidden = values.libraryHidden;
      postAccountAction(data, i18n.get('UserProfileUpdate'), i18n.get('UserProfileUpdating'), i18n.get('UserProfileUpdated'), i18n.get('UserProfileNotUpdated'));
    }
    return (
      <form onSubmit={userProfileForm.onSubmit(handleUserProfileSubmit)}>
        <Divider my='sm' label={i18n.get('Profile')} fz='md' c={'var(--mantine-color-text)'} />
        <TextInput
          label={i18n.get('DisplayName')}
          name='displayName'
          {...userProfileForm.getInputProps('displayName')}
        />
        <Input.Wrapper label={i18n.get('Avatar')}>
          <Dropzone
            name='avatar'
            maxSize={2 * 1024 ** 2}
            accept={IMAGE_MIME_TYPE}
            multiple={false}
            styles={{ inner: { pointerEvents: 'all' } }}
            onDrop={(files: FileWithPath[]) => {
              const reader = new FileReader();
              reader.onload = () => {
                const result = reader.result as string;
                if (result) {
                  setAvatar(result);
                  userProfileForm.setFieldValue('avatar', result);
                }
              }
              if (files[0]) {
                reader.readAsDataURL(files[0]);
              }
            }}
          >
            <Group justify='center'>
              <Dropzone.Accept>
                <PhotoUp />
              </Dropzone.Accept>
              <Dropzone.Reject>
                <PhotoX />
              </Dropzone.Reject>
              <Dropzone.Idle>
                <div onClick={e => { e.stopPropagation() }}>
                  <HoverCard disabled={avatar === ''}>
                    <HoverCard.Target>
                      <Avatar radius='xl' size='lg' src={avatar !== '' ? avatar : null}>
                        {avatar === '' && <User size={24} />}
                      </Avatar>
                    </HoverCard.Target>
                    <HoverCard.Dropdown>
                      <Button
                        onClick={() => {
                          const newavatar = (user.avatar && avatar !== user.avatar) ? user.avatar : '';
                          setAvatar(newavatar);
                          userProfileForm.setFieldValue('avatar', newavatar);
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
                <Text size='sm' c='dimmed' inline mt={7}>
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
          <Group justify='flex-end' mt='md'>
            <Button type='submit'>
              {i18n.get('Apply')}
            </Button>
          </Group>
        )}
      </form>
    )
  }

  const UserGroupForm = (user: UmsUser) => {
    const userGroupForm = useForm({ initialValues: { id: user.id, groupId: user.groupId.toString() } });
    const handleUserGroupSubmit = (values: typeof userGroupForm.values) => {
      const data = { operation: 'modifyuser', userid: user.id, groupid: values.groupId };
      postAccountAction(data, i18n.get('UserGroupChange'), i18n.get('UserGroupChanging'), i18n.get('UserGroupChanged'), i18n.get('UserGroupNotChanged'));
    };
    return (
      <form onSubmit={userGroupForm.onSubmit(handleUserGroupSubmit)}>
        <Divider my='sm' label={i18n.get('Group')} fz='md' c={'var(--mantine-color-text)'} />
        <Select
          label={i18n.get('Group')}
          name='groupId'
          disabled={!canManageGroups}
          data={groupSelectionDatas}
          {...userGroupForm.getInputProps('groupId')}
        />
        {canManageGroups && userGroupForm.isDirty() && (
          <Group justify='flex-end' mt='md'>
            <Button type='submit'>
              {i18n.get('Apply')}
            </Button>
          </Group>
        )}
      </form>
    )
  }

  const UserDeleteForm = (user: UmsUser) => {
    const userDeleteForm = useForm({ initialValues: { id: user.id } });
    const handleUserDeleteSubmit = () => {
      const data = { operation: 'deleteuser', userid: user.id };
      postAccountAction(data, i18n.get('UserDeletion'), i18n.get('UserDeleting'), i18n.get('UserDeleted'), i18n.get('UserNotDeleted'));
    }
    const [opened, setOpened] = useState(false);
    return (
      <form onSubmit={userDeleteForm.onSubmit(handleUserDeleteSubmit)}>
        <Divider my='sm' />
        {opened ? (
          <Group justify='flex-end' mt='md'>
            <Text c='red'>{i18n.get('WarningUserWillBeDeleted')}</Text>
            <Button onClick={() => setOpened(false)}>
              {i18n.get('Cancel')}
            </Button>
            <Button type='submit' color='red' leftSection={<ExclamationMark />} rightSection={<ExclamationMark />}>
              {i18n.get('Confirm')}
            </Button>
          </Group>
        ) : (
          <Group justify='flex-end' mt='md'>
            <Text c='red'>{i18n.get('DeleteUser')}</Text>
            <Button onClick={() => setOpened(true)} color='red' leftSection={<X />}>
              {i18n.get('Delete')}
            </Button>
          </Group>
        )}
      </form>
    );
  }

  const UserAccordion = (user: UmsUser) => {
    const userGroup = getUserGroup(user, accounts);
    const userAccordionLabel = UserAccordionLabel(user, userGroup);
    const userIdentityForm = UserIdentityForm(user);
    const userDisplayNameForm = UserProfileForm(user);
    const userGroupForm = UserGroupForm(user);
    const userDeleteForm = UserDeleteForm(user);
    return (
      <Accordion.Item value={user.id.toString()} key={user.id}>
        <Accordion.Control>{userAccordionLabel}</Accordion.Control>
        <Accordion.Panel>
          {userIdentityForm}
          {userDisplayNameForm}
          {userGroupForm}
          {userDeleteForm}
        </Accordion.Panel>
      </Accordion.Item>
    )
  }

  const NewUserAccordion = () => {
    const user = { id: 0, username: i18n.get('NewUser') } as UmsUser;
    const userGroup = { id: 0, displayName: '' } as UmsGroup;
    const userAccordionLabel = UserAccordionLabel(user, userGroup);
    const newUserForm = NewUserForm();
    return canManageUsers ? (
      <Accordion.Item value={user.id.toString()} key={user.id}>
        <Accordion.Control>{userAccordionLabel}</Accordion.Control>
        <Accordion.Panel>{newUserForm}</Accordion.Panel>
      </Accordion.Item>
    ) : null;
  }

  const UsersAccordions = () => {
    const newUserAccordion = canManageUsers ? NewUserAccordion() : null;
    const usersAccordions = accounts.users.map((user) => {
      return UserAccordion(user);
    });
    return (
      <Accordion value={userOpened} onChange={setUserOpened}>
        {newUserAccordion}
        {usersAccordions}
      </Accordion>
    );
  }

  const GroupAccordionLabel = (group: UmsGroup) => {
    return (
      <Group wrap='nowrap'>
        <Avatar radius='xl' size='lg'>
          {group.id === 0 ? (<FolderPlus size={24} />) : (<Folder size={24} />)}
        </Avatar>
        <Text>{group.displayName}</Text>
      </Group>
    );
  }

  const GroupDisplayNameForm = (group: UmsGroup) => {
    const groupDisplayNameForm = useForm({ initialValues: { id: group.id, displayName: group.displayName } });
    const handleGroupDisplayNameSubmit = (values: typeof groupDisplayNameForm.values) => {
      const data = { operation: 'modifygroup', groupid: group.id, name: values.displayName };
      postAccountAction(data, i18n.get('DisplayNameUpdate'), i18n.get('DisplayNameUpdating'), i18n.get('DisplayNameUpdated'), i18n.get('DisplayNameNotUpdated'));
    }
    return (
      <form onSubmit={groupDisplayNameForm.onSubmit(handleGroupDisplayNameSubmit)}>
        <Divider my='sm' label={i18n.get('DisplayName')} fz='md' c={'var(--mantine-color-text)'} />
        <TextInput
          label={i18n.get('DisplayName')}
          name='displayName'
          {...groupDisplayNameForm.getInputProps('displayName')}
        />
        <Group justify='flex-end' mt='md'>
          <Button type='submit'>
            {i18n.get('Update_verb')}
          </Button>
        </Group>
      </form>
    )
  }

  const GroupPermissionsForm = (group: UmsGroup) => {
    const [permissions, setPermissions] = useState<number>(group.permissions ? group.permissions.value : 0);
    const addPermission = (permission: number) => {
      setPermissions(permissions | permission);
    }
    const removePermission = (permission: number) => {
      setPermissions(permissions & ~permission);
    }
    const groupPermissionsForm = useForm({ initialValues: { id: group.id } });
    const handleGroupPermissionsSubmit = () => {
      const data = { operation: 'updatepermission', groupid: group.id, permissions: permissions };
      postAccountAction(data, i18n.get('PermissionsUpdate'), i18n.get('PermissionsUpdating'), i18n.get('PermissionsUpdated'), i18n.get('PermissionsNotUpdated'));
    }
    return (
      <form onSubmit={groupPermissionsForm.onSubmit(handleGroupPermissionsSubmit)}>
        <Divider my='sm' label={i18n.get('Permissions')} fz='md' c={'var(--mantine-color-text)'} />
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
          <Group justify='flex-end' mt='md'>
            <Button type='submit'>
              {i18n.get('Update_verb')}
            </Button>
          </Group>
        )}
      </form>
    )
  }

  const GroupDeleteForm = (group: UmsGroup) => {
    const groupDeleteForm = useForm({ initialValues: { id: group.id } });
    const handleGroupDeleteSubmit = () => {
      const data = { operation: 'deletegroup', groupid: group.id };
      postAccountAction(data, i18n.get('GroupDeletion'), i18n.get('GroupDeleting'), i18n.get('GroupDeleted'), i18n.get('GroupNotDeleted'));
    }
    const [opened, setOpened] = useState(false);
    return group.id > 1 ? (
      <form onSubmit={groupDeleteForm.onSubmit(handleGroupDeleteSubmit)}>
        <Divider my='sm' />
        {opened ? (
          <Group justify='flex-end' mt='md'>
            <Text c='red'>{i18n.get('WarningGroupWillBeDeleted')}</Text>
            <Button onClick={() => setOpened(false)}>
              {i18n.get('Cancel')}
            </Button>
            <Button type='submit' color='red' leftSection={<ExclamationMark />} rightSection={<ExclamationMark />}>
              {i18n.get('Confirm')}
            </Button>
          </Group>
        ) : (
          <Group justify='flex-end' mt='md'>
            <Text c='red'>{i18n.get('DeleteGroup')}</Text>
            <Button onClick={() => setOpened(true)} color='red' leftSection={<X />}>
              {i18n.get('Delete')}
            </Button>
          </Group>
        )}
      </form>
    ) : null;
  }

  const GroupAccordion = (group: UmsGroup) => {
    const groupAccordionLabel = GroupAccordionLabel(group);
    const groupDisplayNameForm = GroupDisplayNameForm(group);
    const groupPermissionsForm = GroupPermissionsForm(group);
    const groupDeleteForm = GroupDeleteForm(group);
    //perms
    return group.id > 0 ? (
      <Accordion.Item value={group.id.toString()} key={group.id}>
        <Accordion.Control>{groupAccordionLabel}</Accordion.Control>
        <Accordion.Panel>
          {groupDisplayNameForm}
          {groupPermissionsForm}
          {groupDeleteForm}
        </Accordion.Panel>
      </Accordion.Item>
    ) : null;
  }

  const NewGroupForm = () => {
    const newGroupForm = useForm({ initialValues: { displayName: '' } });
    const handleNewGroupSubmit = (values: typeof newGroupForm.values) => {
      const data = { operation: 'creategroup', name: values.displayName };
      postAccountAction(data, i18n.get('GroupCreation'), i18n.get('NewGroupCreating'), i18n.get('NewGroupCreated'), i18n.get('NewGroupNotCreated'));
    }
    return (
      <form onSubmit={newGroupForm.onSubmit(handleNewGroupSubmit)}>
        <TextInput
          required
          label={i18n.get('DisplayName')}
          name='displayName'
          {...newGroupForm.getInputProps('displayName')}
        />
        <Group justify='flex-end' mt='md'>
          <Button type='submit'>
            {i18n.get('Create')}
          </Button>
        </Group>
      </form>
    )
  }

  const NewGroupAccordion = () => {
    const group = { id: 0, displayName: i18n.get('NewGroup') } as UmsGroup;
    const groupAccordionLabel = GroupAccordionLabel(group);
    const newGroupForm = NewGroupForm();
    return (
      <Accordion.Item value={group.id.toString()} key={group.id}>
        <Accordion.Control>{groupAccordionLabel}</Accordion.Control>
        <Accordion.Panel>{newGroupForm}</Accordion.Panel>
      </Accordion.Item>
    );
  }

  const GroupsAccordions = () => {
    const newGroupAccordion = NewGroupAccordion();
    const groupsAccordions = accounts.groups.map((group) => {
      return GroupAccordion(group);
    });
    return (
      <Accordion value={groupOpened} onChange={setGroupOpened}>
        {newGroupAccordion}
        {groupsAccordions}
      </Accordion>
    );
  }

  const handleAuthenticateLocalhostToggle = () => {
    const data = { operation: 'localhost', enabled: !accounts.localhost };
    postAccountAuthAction(data, i18n.get('AuthenticationServiceNotToggled'));
  }

  const AuthenticateLocalhostAdmin = () => {
    return accounts.localhost ? (
      <Group justify='flex-start' mt='md'>
        <Button onClick={() => handleAuthenticateLocalhostToggle()}>{i18n.get('Disable')}</Button>
        <Text>{i18n.get('AuthenticateLocalhostAdminEnabled')}</Text>
      </Group>
    ) : (
      <Group justify='flex-start' mt='md'>
        <Modal
          centered
          opened={localhostOpened}
          onClose={() => setLocalhostOpened(false)}
          title={i18n.get('Warning')}
        >
          <Text>{i18n.get('EnablingAuthenticateLocalhost')}</Text>
          <Group justify='flex-end' mt='md'>
            <Button onClick={() => setLocalhostOpened(false)}>{i18n.get('Cancel')}</Button>
            <Button color='red' onClick={() => handleAuthenticateLocalhostToggle()}>{i18n.get('Confirm')}</Button>
          </Group>
        </Modal>
        <Button onClick={() => setLocalhostOpened(true)}>{i18n.get('Enable')}</Button>
        <Text>{i18n.get('AuthenticateLocalhostAdminDisabled')}</Text>
      </Group>
    )
  }

    const handleAuthenticationToggle = () => {
      const data = { operation: 'authentication', enabled: !accounts.enabled };
      postAccountAuthAction(data, accounts.enabled ? i18n.get('AuthenticationServiceNotDisabled') : i18n.get('AuthenticationServiceNotEnabled'));
    }

  const AuthenticationServiceButton = () => {
    return accounts.enabled ? (
      <Group justify='flex-start' mt='md'>
        <Modal
          centered
          opened={authOpened}
          onClose={() => setAuthOpened(false)}
          title={i18n.get('Warning')}
        >
          <Text>{allowHtml(i18n.get('DisablingAuthenticationReduces'))}</Text>
          <Group justify='flex-end' mt='md'>
            <Button onClick={() => setAuthOpened(false)}>{i18n.get('Cancel')}</Button>
            <Button color='red' onClick={() => handleAuthenticationToggle()}>{i18n.get('Confirm')}</Button>
          </Group>
        </Modal>
        <Button onClick={() => setAuthOpened(true)}>{i18n.get('Disable')}</Button>
        <Text>{i18n.get('AuthenticationServiceEnabled')}</Text>
      </Group>
    ) : (
      <Group justify='flex-start' mt='md'>
        <Button onClick={() => handleAuthenticationToggle()}>{i18n.get('Enable')}</Button>
        <Text>{i18n.get('AuthenticationServiceDisabled')}</Text>
      </Group>
    );
  }

  const AuthenticationSettings = () => {
    return accounts.enabled ? (<>
      <AuthenticationServiceButton />
      <AuthenticateLocalhostAdmin />
    </>) : (
      <AuthenticationServiceButton />
    );
  }

  return (
    <Box style={{ maxWidth: 1024 }} mx='auto'>
      {canManageGroups ? (
        <Tabs defaultValue={accounts.enabled ? 'users' : 'settings'}>
          <Tabs.List>
            {session.authenticate && (
              <Tabs.Tab value='users'>
                {i18n.get('Users')}
              </Tabs.Tab>
            )}
            {session.authenticate && (
              <Tabs.Tab value='groups'>
                {i18n.get('Groups')}
              </Tabs.Tab>
            )}
            {canModifySettings && (
              <Tabs.Tab value='settings'>
                {i18n.get('Settings')}
              </Tabs.Tab>
            )}
          </Tabs.List>
          {session.authenticate && (
            <Tabs.Panel value='users'>
              <UsersAccordions />
            </Tabs.Panel>
          )}
          {session.authenticate && (
            <Tabs.Panel value='groups'>
              <GroupsAccordions />
            </Tabs.Panel>
          )}
          {canModifySettings && (
            <Tabs.Panel value='settings'>
              <AuthenticationSettings />
            </Tabs.Panel>
          )}
        </Tabs>
      ) : session.authenticate ? (
        <UsersAccordions />
      ) : (
        <Box style={{ maxWidth: 1024 }} mx='auto'>
          <Text c='red'>{i18n.get('YouDontHaveAccessArea')}</Text>
        </Box>
      )}
    </Box>
  );
};

export default Accounts;
