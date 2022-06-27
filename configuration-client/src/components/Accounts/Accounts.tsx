import { Accordion, Avatar, Box, Button, Checkbox, CheckboxGroup, Divider, Group, PasswordInput, Select, Tabs, Text, TextInput } from '@mantine/core';
import { useForm } from '@mantine/form';
import { useContext, useState } from 'react';
import { ExclamationMark, Folder, FolderPlus, User, UserPlus, X } from 'tabler-icons-react';

import { UmsGroup, UmsUser } from '../../contexts/session-context';
import AccountsContext, { UmsAccounts } from '../../contexts/accounts-context';
import { AccountsProvider } from '../../providers/accounts-provider';
import { getUserGroup, getUserGroupsSelection, postAccountAction } from '../../services/accounts-service';
import I18nContext from '../../contexts/i18n-context';

const Accounts = () => {
  const [activeTab, setActiveTab] = useState(0);
  const i18n = useContext(I18nContext);

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

  function NewUserForm(accounts: UmsAccounts) {
    const newUserForm = useForm({ initialValues: {username:null,password:null,groupid:"0",displayname:null} });
    const handleNewUserSubmit = (values: typeof newUserForm.values) => {
      const data = {operation:'createuser', username:values.username, password:values.password, groupid:values.groupid, displayname:values.displayname};
      postAccountAction(data, i18n['WebGui.AccountsUserCreationTitle'], i18n['WebGui.AccountsUserCreationSuccess'], i18n['WebGui.AccountsUserCreationError']);
    }
    const groupDatas2 = getUserGroupsSelection(accounts);
    return (
      <form onSubmit={newUserForm.onSubmit(handleNewUserSubmit)}>
        <TextInput
          required
          label={i18n['WebGui.AccountsUsername']}
          name="username"
          {...newUserForm.getInputProps('username')}
        />
        <PasswordInput
          required
          label={i18n['WebGui.AccountsPassword']}
          name="username"
          type="password"
          {...newUserForm.getInputProps('password')}
        />
        <TextInput
          required
          label={i18n['WebGui.AccountsDisplayName']}
          name="displayname"
          {...newUserForm.getInputProps('displayname')}
        />
        <Select
          required
          label={i18n['WebGui.AccountsGroup']}
          name="groupId"
          data={groupDatas2}
          {...newUserForm.getInputProps('groupid')}
        />
        <Group position="right" mt="md">
          <Button type="submit">
            {i18n['WebGui.ButtonCreate']}
          </Button>
        </Group>
      </form>
    )
  };

  function UserIdentityForm(user: UmsUser) {
    const userIdentityForm = useForm({ initialValues: {id:user.id,username:user.username,password:''} });
    const handleUserIdentitySubmit = (values: typeof userIdentityForm.values) => {
      const data = {operation:'changelogin', userid:user.id, username:values.username, password:values.password};
      postAccountAction(data, i18n['WebGui.AccountsCredentialsUpdateTitle'], i18n['WebGui.AccountsCredentialsUpdateSuccess'], i18n['WebGui.AccountsCredentialsUpdateError']);
    }
    return (
      <form onSubmit={userIdentityForm.onSubmit(handleUserIdentitySubmit)}>
        <Divider my="sm" label={i18n['WebGui.AccountsCredentials']} />
        <TextInput
          required
          label={i18n['WebGui.AccountsUsername']}
          name="username"
          {...userIdentityForm.getInputProps('username')}
        />
        <PasswordInput
          required
          label={i18n['WebGui.AccountsPassword']}
          name="username"
          type="password"
          {...userIdentityForm.getInputProps('password')}
        />
        <Group position="right" mt="md">
          <Button type="submit">
            {i18n['WebGui.ButtonUpdate']}
          </Button>
        </Group>
      </form>
    )
  };

  function UserDisplayNameForm(user: UmsUser) {
    const userDisplayNameForm = useForm({ initialValues: {id:user.id,displayName:user.displayName} });
    const handleUserDisplayNameSubmit = (values: typeof userDisplayNameForm.values) => {
      const data = {operation:'modifyuser', userid:user.id, name:values.displayName};
      postAccountAction(data, i18n['WebGui.AccountsDisplayNameUpdateTitle'], i18n['WebGui.AccountsDisplayNameUpdateSuccess'], i18n['WebGui.AccountsDisplayNameUpdateError']);
    }
    return (
      <form onSubmit={userDisplayNameForm.onSubmit(handleUserDisplayNameSubmit)}>
        <Divider my="sm" label={i18n['WebGui.AccountsDisplayName']} />
        <TextInput
          label={i18n['WebGui.AccountsDisplayName']}
          name="displayName"
          {...userDisplayNameForm.getInputProps('displayName')}
        />
        <Group position="right" mt="md">
          <Button type="submit">
            {i18n['WebGui.ButtonUpdate']}
          </Button>
        </Group>
      </form>
    )
  };

  function UserGroupForm(user: UmsUser, accounts: UmsAccounts) {
    const userGroupForm = useForm({ initialValues: {id:user.id,groupId:user.groupId.toString()} });
    const handleUserGroupSubmit = (values: typeof userGroupForm.values) => {
      const data = {operation:'modifyuser', userid:user.id, groupid:values.groupId};
      postAccountAction(data, i18n['WebGui.AccountsUserGroupChangeTitle'], i18n['WebGui.AccountsUserGroupChangeSuccess'], i18n['WebGui.AccountsUserGroupChangeError']);
    };
    const groupDatas = getUserGroupsSelection(accounts);
    return (
      <form onSubmit={userGroupForm.onSubmit(handleUserGroupSubmit)}>
        <Divider my="sm" label={i18n['WebGui.AccountsGroup']} />
        <Select
          label={i18n['WebGui.AccountsGroup']}
          name="groupId"
          disabled={!accounts.groupsManage}
          data={groupDatas}
          {...userGroupForm.getInputProps('groupId')}
        />
        {accounts.groupsManage && (
          <Group position="right" mt="md">
            <Button type="submit">
              {i18n['WebGui.ButtonUpdate']}
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
      postAccountAction(data, i18n['WebGui.AccountsUserDeleteTitle'], i18n['WebGui.AccountsUserDeleteSuccess'], i18n['WebGui.AccountsUserDeleteError']);
    }
    const [opened, setOpened] = useState(false);
    return (
      <form onSubmit={userDeleteForm.onSubmit(handleUserDeleteSubmit)}>
        <Divider my="sm" />
        { opened ? (
          <Group position="right" mt="md">
            <Text color="red">{i18n['WebGui.AccountsUserDeleteWarning']}</Text>
            <Button onClick={() => setOpened(false)}>
              {i18n['WebGui.ButtonCancel']}
            </Button>
            <Button type="submit" color="red" leftIcon={<ExclamationMark />} rightIcon={<ExclamationMark />}>
              {i18n['WebGui.ButtonConfirm']}
            </Button>
          </Group>
        ) : (
          <Group position="right" mt="md">
            <Text color="red">{i18n['WebGui.AccountsUserDelete']}</Text>
            <Button onClick={() => setOpened(true)} color="red" leftIcon={<X />}>
              {i18n['WebGui.ButtonDelete']}
            </Button>
          </Group>
        )}
      </form>
    ); 
  };

  function UserAccordion(user: UmsUser, accounts: UmsAccounts) {
    const userGroup = getUserGroup(user, accounts);
    const userAccordionLabel = UserAccordionLabel(user, userGroup);
    const userIdentityForm = UserIdentityForm(user);
    const userDisplayNameForm = UserDisplayNameForm(user);
    const userGroupForm = UserGroupForm(user, accounts);
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

  function NewUserAccordion(accounts: UmsAccounts) {
    const user = {id:0,username:i18n['WebGui.AccountsNewUser']} as UmsUser;
    const userGroup = {id:0,displayName:''} as UmsGroup;
    const userAccordionLabel = UserAccordionLabel(user, userGroup);
    const newUserForm = NewUserForm(accounts);
    return accounts.usersManage ? (
        <Accordion.Item label={userAccordionLabel} key={user.id}>
          {newUserForm}
        </Accordion.Item>
    ) : null;
  };

  function UsersAccordions(accounts: UmsAccounts) {
    const newUserAccordion = accounts.usersManage ? NewUserAccordion(accounts) : null;
    const usersAccordions = accounts.users.map((user) => {
      return UserAccordion(user, accounts);
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
      postAccountAction(data, i18n['WebGui.AccountsDisplayNameUpdateTitle'], i18n['WebGui.AccountsDisplayNameUpdateSuccess'], i18n['WebGui.AccountsDisplayNameUpdateError']);
    }
    return (
      <form onSubmit={groupDisplayNameForm.onSubmit(handleGroupDisplayNameSubmit)}>
        <Divider my="sm" label={i18n['WebGui.AccountsDisplayName']} />
        <TextInput
          label={i18n['WebGui.AccountsDisplayName']}
          name="displayName"
          {...groupDisplayNameForm.getInputProps('displayName')}
        />
        <Group position="right" mt="md">
          <Button type="submit">
            {i18n['WebGui.ButtonUpdate']}
          </Button>
        </Group>
      </form>
    )
  };

  function GroupPermissionsForm(group: UmsGroup, accounts: UmsAccounts) {
    const [permissions, setPermissions] = useState<string[]>(group.permissions);
    const groupPermissionsForm = useForm({ initialValues: {id:group.id} });
    const handleGroupPermissionsSubmit = (values: typeof groupPermissionsForm.values) => {
      const data = {operation:'updatepermission', groupid:group.id, permissions:permissions};
      postAccountAction(data, i18n['WebGui.AccountsPermissionsUpdateTitle'], i18n['WebGui.AccountsPermissionsUpdateSuccess'], i18n['WebGui.AccountsPermissionsUpdateError']);
    }
    return (
      <form onSubmit={groupPermissionsForm.onSubmit(handleGroupPermissionsSubmit)}>
        <Divider my="sm" label={i18n['WebGui.AccountsPermissions']} />
        <CheckboxGroup
          value={permissions}
          onChange={setPermissions}
          orientation="vertical"
        >
          <Checkbox value="*" label={i18n['WebGui.AccountsPermissionAllPermissions']} />
          <Checkbox value="server_restart" label={i18n['WebGui.AccountsPermissionServerRestart']} />
          <Checkbox value="users_manage" label={i18n['WebGui.AccountsPermissionUsersManage']} />
          <Checkbox value="groups_manage" label={i18n['WebGui.AccountsPermissionGroupsManage']} />
          <Checkbox value="settings_view" label={i18n['WebGui.AccountsPermissionSettingsView']} />
          <Checkbox value="settings_modify" label={i18n['WebGui.AccountsPermissionSettingsModify']} />
        </CheckboxGroup>
        <Group position="right" mt="md">
          <Button type="submit">
            {i18n['WebGui.ButtonUpdate']}
          </Button>
        </Group>
      </form>
    )
  };

  function GroupDeleteForm(group: UmsGroup) {
    const groupDeleteForm = useForm({ initialValues: {id:group.id} });
    const handleGroupDeleteSubmit = () => {
      const data = {operation:'deletegroup', groupid:group.id};
      postAccountAction(data, i18n['WebGui.AccountsGroupDeleteTitle'], i18n['WebGui.AccountsGroupDeleteSuccess'], i18n['WebGui.AccountsGroupDeleteError']);
    }
    const [opened, setOpened] = useState(false);
    return group.id > 1 ? (
      <form onSubmit={groupDeleteForm.onSubmit(handleGroupDeleteSubmit)}>
        <Divider my="sm" />
        { opened ? (
          <Group position="right" mt="md">
            <Text color="red">{i18n['WebGui.AccountsGroupDeleteWarning']}</Text>
            <Button onClick={() => setOpened(false)}>
              {i18n['WebGui.ButtonCancel']}
            </Button>
            <Button type="submit" color="red" leftIcon={<ExclamationMark />} rightIcon={<ExclamationMark />}>
              {i18n['WebGui.ButtonConfirm']}
            </Button>
          </Group>
        ) : (
          <Group position="right" mt="md">
            <Text color="red">{i18n['WebGui.AccountsGroupDelete']}</Text>
            <Button onClick={() => setOpened(true)} color="red" leftIcon={<X />}>
              {i18n['WebGui.ButtonDelete']}
            </Button>
          </Group>
        )}
      </form>
    ) : null;
  };

  function GroupAccordion(group: UmsGroup, accounts: UmsAccounts) {
    const groupAccordionLabel = GroupAccordionLabel(group);
    const groupDisplayNameForm = GroupDisplayNameForm(group);
    const groupPermissionsForm = GroupPermissionsForm(group, accounts);
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
      postAccountAction(data, i18n['WebGui.AccountsGroupCreationTitle'], i18n['WebGui.AccountsGroupCreationSuccess'], i18n['WebGui.AccountsGroupCreationError']);
    }
    return (
      <form onSubmit={newGroupForm.onSubmit(handleNewGroupSubmit)}>
        <TextInput
          required
          label={i18n['WebGui.AccountsDisplayName']}
          name="displayName"
          {...newGroupForm.getInputProps('displayName')}
        />
        <Group position="right" mt="md">
          <Button type="submit">
            {i18n['WebGui.ButtonCreate']}
          </Button>
        </Group>
      </form>
    )
  };

  function NewGroupAccordion() {
    const group = {id:0,displayName:i18n['WebGui.AccountsNewGroup']} as UmsGroup;
    const groupAccordionLabel = GroupAccordionLabel(group);
    const newGroupForm = NewGroupForm();
    return (
      <Accordion.Item label={groupAccordionLabel} key={group.id}>
        {newGroupForm}
      </Accordion.Item>
    );
  };

  function GroupsAccordions(accounts: UmsAccounts) {
    const newGroupAccordion = NewGroupAccordion();
    const groupsAccordions = accounts.groups.map((group) => {
      return GroupAccordion(group, accounts);
    });
    return (
      <Accordion initialItem={-1} iconPosition="right">
        {newGroupAccordion}
        {groupsAccordions}
      </Accordion>
    );
  };

  return (
    <AccountsProvider>
      <Box sx={{ maxWidth: 700 }} mx="auto">
        <AccountsContext.Consumer>
	    { accounts => (
          {...accounts.groupsManage ? (
            <Tabs active={activeTab} onTabChange={setActiveTab}>
	          <Tabs.Tab label={i18n['WebGui.AccountsUsers']}>
	            <UsersAccordions {...accounts} />
              </Tabs.Tab>
	          <Tabs.Tab label={i18n['WebGui.AccountsGroups']}>
	            <GroupsAccordions {...accounts} />
              </Tabs.Tab>
            </Tabs>
          ) : (
            <UsersAccordions {...accounts} />
          )}
        )}
        </AccountsContext.Consumer>
      </Box>
    </AccountsProvider>
  );
};

export default Accounts;
