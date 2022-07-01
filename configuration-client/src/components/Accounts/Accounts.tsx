import { Accordion, Avatar, Box, Button, Checkbox, CheckboxGroup, Divider, Group, PasswordInput, Select, Tabs, Text, TextInput } from '@mantine/core';
import { useForm } from '@mantine/form';
import { useContext, useState } from 'react';
import { ExclamationMark, Folder, FolderPlus, User, UserPlus, X } from 'tabler-icons-react';

import AccountsContext from '../../contexts/accounts-context';
import I18nContext from '../../contexts/i18n-context';
import SessionContext, { UmsGroup, UmsUser } from '../../contexts/session-context';
import { getUserGroup, getUserGroupsSelection, havePermission, postAccountAction } from '../../services/accounts-service';

const Accounts = () => {
  const [activeTab, setActiveTab] = useState(0);
  const i18n = useContext(I18nContext);
  const session = useContext(SessionContext);
  const accounts = useContext(AccountsContext);
  const groupSelectionDatas = getUserGroupsSelection(accounts, i18n.get['Generic.None']);
  const canManageUsers = havePermission(session, "users_manage");
  const canManageGroups = havePermission(session, "groups_manage");

  function UserAccordionLabel(user: UmsUser, group: UmsGroup) {
    const showAsUsername = (user.displayName == null || user.displayName.length === 0 || user.displayName === user.username);
    const groupDisplayName = group.displayName.length !== 0 ? ' ('.concat(group.displayName).concat(')') : '';
    const displayName = showAsUsername ? user.username : user.displayName;
    return (
      <Group noWrap>
        <Avatar radius="xl" size="lg">
          {user.id === 0 ? (<UserPlus size={24} />) : (<User size={24} />)}
        </Avatar>
        <div>
          <Text>{displayName}{groupDisplayName}</Text>
          { !showAsUsername && (
            <Text size="sm" color="dimmed" weight={400}>
              {user.username}
            </Text>
          )}
        </div>
      </Group>
    );
  };

  function NewUserForm() {
    const newUserForm = useForm({ initialValues: {username:null,password:null,groupid:"0",displayname:null} });
    const handleNewUserSubmit = (values: typeof newUserForm.values) => {
      const data = {operation:'createuser', username:values.username, password:values.password, groupid:values.groupid, displayname:values.displayname};
      postAccountAction(data, i18n.get['WebGui.AccountsUserCreationTitle'], i18n.get['WebGui.AccountsUserCreationSuccess'], i18n.get['WebGui.AccountsUserCreationError']);
    }
    return (
      <form onSubmit={newUserForm.onSubmit(handleNewUserSubmit)}>
        <TextInput
          required
          label={i18n.get['WebGui.AccountsUsername']}
          name="username"
          {...newUserForm.getInputProps('username')}
        />
        <PasswordInput
          required
          label={i18n.get['WebGui.AccountsPassword']}
          name="username"
          type="password"
          {...newUserForm.getInputProps('password')}
        />
        <TextInput
          required
          label={i18n.get['WebGui.AccountsDisplayName']}
          name="displayname"
          {...newUserForm.getInputProps('displayname')}
        />
        <Select
          required
          label={i18n.get['WebGui.AccountsGroup']}
          name="groupId"
          data={groupSelectionDatas}
          {...newUserForm.getInputProps('groupid')}
        />
        <Group position="right" mt="md">
          <Button type="submit">
            {i18n.get['WebGui.ButtonCreate']}
          </Button>
        </Group>
      </form>
    )
  };

  function UserIdentityForm(user: UmsUser) {
    const userIdentityForm = useForm({ initialValues: {id:user.id,username:user.username,password:''} });
    const handleUserIdentitySubmit = (values: typeof userIdentityForm.values) => {
      const data = {operation:'changelogin', userid:user.id, username:values.username, password:values.password};
      postAccountAction(data, i18n.get['WebGui.AccountsCredentialsUpdateTitle'], i18n.get['WebGui.AccountsCredentialsUpdateSuccess'], i18n.get['WebGui.AccountsCredentialsUpdateError']);
    }
    return (
      <form onSubmit={userIdentityForm.onSubmit(handleUserIdentitySubmit)}>
        <Divider my="sm" label={i18n.get['WebGui.AccountsCredentials']} />
        <TextInput
          required
          label={i18n.get['WebGui.AccountsUsername']}
          name="username"
          {...userIdentityForm.getInputProps('username')}
        />
        <PasswordInput
          required
          label={i18n.get['WebGui.AccountsPassword']}
          name="username"
          type="password"
          {...userIdentityForm.getInputProps('password')}
        />
        <Group position="right" mt="md">
          <Button type="submit">
            {i18n.get['WebGui.ButtonUpdate']}
          </Button>
        </Group>
      </form>
    )
  };

  function UserDisplayNameForm(user: UmsUser) {
    const userDisplayNameForm = useForm({ initialValues: {id:user.id,displayName:user.displayName} });
    const handleUserDisplayNameSubmit = (values: typeof userDisplayNameForm.values) => {
      const data = {operation:'modifyuser', userid:user.id, name:values.displayName};
      postAccountAction(data, i18n.get['WebGui.AccountsDisplayNameUpdateTitle'], i18n.get['WebGui.AccountsDisplayNameUpdateSuccess'], i18n.get['WebGui.AccountsDisplayNameUpdateError']);
    }
    return (
      <form onSubmit={userDisplayNameForm.onSubmit(handleUserDisplayNameSubmit)}>
        <Divider my="sm" label={i18n.get['WebGui.AccountsDisplayName']} />
        <TextInput
          label={i18n.get['WebGui.AccountsDisplayName']}
          name="displayName"
          {...userDisplayNameForm.getInputProps('displayName')}
        />
        <Group position="right" mt="md">
          <Button type="submit">
            {i18n.get['WebGui.ButtonUpdate']}
          </Button>
        </Group>
      </form>
    )
  };

  function UserGroupForm(user: UmsUser) {
    const userGroupForm = useForm({ initialValues: {id:user.id,groupId:user.groupId.toString()} });
    const handleUserGroupSubmit = (values: typeof userGroupForm.values) => {
      const data = {operation:'modifyuser', userid:user.id, groupid:values.groupId};
      postAccountAction(data, i18n.get['WebGui.AccountsUserGroupChangeTitle'], i18n.get['WebGui.AccountsUserGroupChangeSuccess'], i18n.get['WebGui.AccountsUserGroupChangeError']);
    };
    return (
      <form onSubmit={userGroupForm.onSubmit(handleUserGroupSubmit)}>
        <Divider my="sm" label={i18n.get['WebGui.AccountsGroup']} />
        <Select
          label={i18n.get['WebGui.AccountsGroup']}
          name="groupId"
          disabled={!canManageGroups}
          data={groupSelectionDatas}
          {...userGroupForm.getInputProps('groupId')}
        />
        {canManageGroups && (
          <Group position="right" mt="md">
            <Button type="submit">
              {i18n.get['WebGui.ButtonUpdate']}
            </Button>
          </Group>
        )}
      </form>
    )
  };

  function UserDeleteForm(user: UmsUser) {
    const userDeleteForm = useForm({ initialValues: {id:user.id} });
    const handleUserDeleteSubmit = () => {
      const data = {operation:'deleteuser', userid:user.id};
      postAccountAction(data, i18n.get['WebGui.AccountsUserDeleteTitle'], i18n.get['WebGui.AccountsUserDeleteSuccess'], i18n.get['WebGui.AccountsUserDeleteError']);
    }
    const [opened, setOpened] = useState(false);
    return (
      <form onSubmit={userDeleteForm.onSubmit(handleUserDeleteSubmit)}>
        <Divider my="sm" />
        { opened ? (
          <Group position="right" mt="md">
            <Text color="red">{i18n.get['WebGui.AccountsUserDeleteWarning']}</Text>
            <Button onClick={() => setOpened(false)}>
              {i18n.get['WebGui.ButtonCancel']}
            </Button>
            <Button type="submit" color="red" leftIcon={<ExclamationMark />} rightIcon={<ExclamationMark />}>
              {i18n.get['WebGui.ButtonConfirm']}
            </Button>
          </Group>
        ) : (
          <Group position="right" mt="md">
            <Text color="red">{i18n.get['WebGui.AccountsUserDelete']}</Text>
            <Button onClick={() => setOpened(true)} color="red" leftIcon={<X />}>
              {i18n.get['WebGui.ButtonDelete']}
            </Button>
          </Group>
        )}
      </form>
    ); 
  };

  function UserAccordion(user: UmsUser) {
    const userGroup = getUserGroup(user, accounts);
    const userAccordionLabel = UserAccordionLabel(user, userGroup);
    const userIdentityForm = UserIdentityForm(user);
    const userDisplayNameForm = UserDisplayNameForm(user);
    const userGroupForm = UserGroupForm(user);
    const userDeleteForm = UserDeleteForm(user);
    return (
      <Accordion.Item label={userAccordionLabel} key={user.id}>
        {userIdentityForm}
        {userDisplayNameForm}
        {userGroupForm}
        {userDeleteForm}
      </Accordion.Item>
    )
  };

  function NewUserAccordion() {
    const user = {id:0,username:i18n.get['WebGui.AccountsNewUser']} as UmsUser;
    const userGroup = {id:0,displayName:''} as UmsGroup;
    const userAccordionLabel = UserAccordionLabel(user, userGroup);
    const newUserForm = NewUserForm();
    return canManageUsers ? (
        <Accordion.Item label={userAccordionLabel} key={user.id}>
          {newUserForm}
        </Accordion.Item>
    ) : null;
  };

  function UsersAccordions() {
    const newUserAccordion = canManageUsers ? NewUserAccordion() : null;
    const usersAccordions = accounts.users.map((user) => {
      return UserAccordion(user);
    });
    return (
      <Accordion initialItem={-1} iconPosition="right">
        {newUserAccordion}
        {usersAccordions}
      </Accordion>
    );
  };

  function GroupAccordionLabel(group: UmsGroup) {
    return (
      <Group noWrap>
        <Avatar radius="xl" size="lg">
          {group.id === 0 ? (<FolderPlus size={24} />) : (<Folder size={24} />)}
        </Avatar>
        <Text>{group.displayName}</Text>
      </Group>
    );
  };

  function GroupDisplayNameForm(group: UmsGroup) {
    const groupDisplayNameForm = useForm({ initialValues: {id:group.id,displayName:group.displayName} });
    const handleGroupDisplayNameSubmit = (values: typeof groupDisplayNameForm.values) => {
      const data = {operation:'modifygroup', groupid:group.id, name:values.displayName};
      postAccountAction(data, i18n.get['WebGui.AccountsDisplayNameUpdateTitle'], i18n.get['WebGui.AccountsDisplayNameUpdateSuccess'], i18n.get['WebGui.AccountsDisplayNameUpdateError']);
    }
    return (
      <form onSubmit={groupDisplayNameForm.onSubmit(handleGroupDisplayNameSubmit)}>
        <Divider my="sm" label={i18n.get['WebGui.AccountsDisplayName']} />
        <TextInput
          label={i18n.get['WebGui.AccountsDisplayName']}
          name="displayName"
          {...groupDisplayNameForm.getInputProps('displayName')}
        />
        <Group position="right" mt="md">
          <Button type="submit">
            {i18n.get['WebGui.ButtonUpdate']}
          </Button>
        </Group>
      </form>
    )
  };

  function GroupPermissionsForm(group: UmsGroup) {
    const [permissions, setPermissions] = useState<string[]>(group.permissions);
    const groupPermissionsForm = useForm({ initialValues: {id:group.id} });
    const handleGroupPermissionsSubmit = (values: typeof groupPermissionsForm.values) => {
      const data = {operation:'updatepermission', groupid:group.id, permissions:permissions};
      postAccountAction(data, i18n.get['WebGui.AccountsPermissionsUpdateTitle'], i18n.get['WebGui.AccountsPermissionsUpdateSuccess'], i18n.get['WebGui.AccountsPermissionsUpdateError']);
    }
    return (
      <form onSubmit={groupPermissionsForm.onSubmit(handleGroupPermissionsSubmit)}>
        <Divider my="sm" label={i18n.get['WebGui.AccountsPermissions']} />
        <CheckboxGroup
          value={permissions}
          onChange={setPermissions}
          orientation="vertical"
        >
          <Checkbox value="*" label={i18n.get['WebGui.AccountsPermissionAllPermissions']} />
          <Checkbox value="server_restart" label={i18n.get['WebGui.AccountsPermissionServerRestart']} />
          <Checkbox value="users_manage" label={i18n.get['WebGui.AccountsPermissionUsersManage']} />
          <Checkbox value="groups_manage" label={i18n.get['WebGui.AccountsPermissionGroupsManage']} />
          <Checkbox value="settings_view" label={i18n.get['WebGui.AccountsPermissionSettingsView']} />
          <Checkbox value="settings_modify" label={i18n.get['WebGui.AccountsPermissionSettingsModify']} />
        </CheckboxGroup>
        <Group position="right" mt="md">
          <Button type="submit">
            {i18n.get['WebGui.ButtonUpdate']}
          </Button>
        </Group>
      </form>
    )
  };

  function GroupDeleteForm(group: UmsGroup) {
    const groupDeleteForm = useForm({ initialValues: {id:group.id} });
    const handleGroupDeleteSubmit = () => {
      const data = {operation:'deletegroup', groupid:group.id};
      postAccountAction(data, i18n.get['WebGui.AccountsGroupDeleteTitle'], i18n.get['WebGui.AccountsGroupDeleteSuccess'], i18n.get['WebGui.AccountsGroupDeleteError']);
    }
    const [opened, setOpened] = useState(false);
    return group.id > 1 ? (
      <form onSubmit={groupDeleteForm.onSubmit(handleGroupDeleteSubmit)}>
        <Divider my="sm" />
        { opened ? (
          <Group position="right" mt="md">
            <Text color="red">{i18n.get['WebGui.AccountsGroupDeleteWarning']}</Text>
            <Button onClick={() => setOpened(false)}>
              {i18n.get['WebGui.ButtonCancel']}
            </Button>
            <Button type="submit" color="red" leftIcon={<ExclamationMark />} rightIcon={<ExclamationMark />}>
              {i18n.get['WebGui.ButtonConfirm']}
            </Button>
          </Group>
        ) : (
          <Group position="right" mt="md">
            <Text color="red">{i18n.get['WebGui.AccountsGroupDelete']}</Text>
            <Button onClick={() => setOpened(true)} color="red" leftIcon={<X />}>
              {i18n.get['WebGui.ButtonDelete']}
            </Button>
          </Group>
        )}
      </form>
    ) : null;
  };

  function GroupAccordion(group: UmsGroup) {
    const groupAccordionLabel = GroupAccordionLabel(group);
    const groupDisplayNameForm = GroupDisplayNameForm(group);
    const groupPermissionsForm = GroupPermissionsForm(group);
    const groupDeleteForm = GroupDeleteForm(group);
    //perms
    return group.id > 0 ? (
      <Accordion.Item label={groupAccordionLabel} key={group.id}>
        {groupDisplayNameForm}
        {groupPermissionsForm}
        {groupDeleteForm}
      </Accordion.Item>
    ) : null;
  };

  function NewGroupForm() {
    const newGroupForm = useForm({ initialValues: {displayName:''} });
    const handleNewGroupSubmit = (values: typeof newGroupForm.values) => {
      const data = {operation:'creategroup', name:values.displayName};
      postAccountAction(data, i18n.get['WebGui.AccountsGroupCreationTitle'], i18n.get['WebGui.AccountsGroupCreationSuccess'], i18n.get['WebGui.AccountsGroupCreationError']);
    }
    return (
      <form onSubmit={newGroupForm.onSubmit(handleNewGroupSubmit)}>
        <TextInput
          required
          label={i18n.get['WebGui.AccountsDisplayName']}
          name="displayName"
          {...newGroupForm.getInputProps('displayName')}
        />
        <Group position="right" mt="md">
          <Button type="submit">
            {i18n.get['WebGui.ButtonCreate']}
          </Button>
        </Group>
      </form>
    )
  };

  function NewGroupAccordion() {
    const group = {id:0,displayName:i18n.get['WebGui.AccountsNewGroup']} as UmsGroup;
    const groupAccordionLabel = GroupAccordionLabel(group);
    const newGroupForm = NewGroupForm();
    return (
      <Accordion.Item label={groupAccordionLabel} key={group.id}>
        {newGroupForm}
      </Accordion.Item>
    );
  };

  function GroupsAccordions() {
    const newGroupAccordion = NewGroupAccordion();
    const groupsAccordions = accounts.groups.map((group) => {
      return GroupAccordion(group);
    });
    return (
      <Accordion initialItem={-1} iconPosition="right">
        {newGroupAccordion}
        {groupsAccordions}
      </Accordion>
    );
  };

  return (
      <Box sx={{ maxWidth: 700 }} mx="auto">
          {canManageGroups ? (
            <Tabs active={activeTab} onTabChange={setActiveTab}>
	          <Tabs.Tab label={i18n.get['WebGui.AccountsUsers']}>
	            <UsersAccordions />
              </Tabs.Tab>
	          <Tabs.Tab label={i18n.get['WebGui.AccountsGroups']}>
	            <GroupsAccordions />
              </Tabs.Tab>
            </Tabs>
          ) : (
            <UsersAccordions />
          )}
      </Box>
  );
};

export default Accounts;
