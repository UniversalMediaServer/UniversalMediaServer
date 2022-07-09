import { Accordion, Avatar, Box, Button, Checkbox, CheckboxGroup, Divider, Group, Modal, PasswordInput, Select, Tabs, Text, TextInput } from '@mantine/core';
import { useForm } from '@mantine/form';
import React, { useContext, useState } from 'react';
import { ExclamationMark, Folder, FolderPlus, User, UserPlus, X } from 'tabler-icons-react';

import AccountsContext from '../../contexts/accounts-context';
import I18nContext from '../../contexts/i18n-context';
import SessionContext, { UmsGroup, UmsUser } from '../../contexts/session-context';
import { getUserGroup, getUserGroupsSelection, havePermission, postAccountAction, postAccountAuthAction } from '../../services/accounts-service';
import { allowHtml } from '../../utils';

const Accounts = () => {
  const [activeTab, setActiveTab] = useState(0);
  const i18n = useContext(I18nContext);
  const session = useContext(SessionContext);
  const accounts = useContext(AccountsContext);
  const groupSelectionDatas = getUserGroupsSelection(accounts, i18n.get['None']);
  const canModifySettings = havePermission(session, "settings_modify");
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
      postAccountAction(data, i18n.get['UserCreation'], i18n.get['NewUserCreated'], i18n.get['NewUserNotCreated']);
    }
    return (
      <form onSubmit={newUserForm.onSubmit(handleNewUserSubmit)}>
        <TextInput
          required
          label={i18n.get['Username']}
          name="username"
          {...newUserForm.getInputProps('username')}
        />
        <PasswordInput
          required
          label={i18n.get['Password']}
          name="username"
          type="password"
          {...newUserForm.getInputProps('password')}
        />
        <TextInput
          required
          label={i18n.get['DisplayName']}
          name="displayname"
          {...newUserForm.getInputProps('displayname')}
        />
        <Select
          required
          label={i18n.get['Group']}
          name="groupId"
          data={groupSelectionDatas}
          {...newUserForm.getInputProps('groupid')}
        />
        <Group position="right" mt="md">
          <Button type="submit">
            {i18n.get['Create']}
          </Button>
        </Group>
      </form>
    )
  };

  function UserIdentityForm(user: UmsUser) {
    const userIdentityForm = useForm({ initialValues: {id:user.id,username:user.username,password:''} });
    const handleUserIdentitySubmit = (values: typeof userIdentityForm.values) => {
      const data = {operation:'changelogin', userid:user.id, username:values.username, password:values.password};
      postAccountAction(data, i18n.get['CredentialsUpdate'], i18n.get['CredentialsUpdated'], i18n.get['CredentialsNotUpdated']);
    }
    return (
      <form onSubmit={userIdentityForm.onSubmit(handleUserIdentitySubmit)}>
        <Divider my="sm" label={i18n.get['Credentials']} />
        <TextInput
          required
          label={i18n.get['Username']}
          name="username"
          {...userIdentityForm.getInputProps('username')}
        />
        <PasswordInput
          required
          label={i18n.get['Password']}
          name="username"
          type="password"
          {...userIdentityForm.getInputProps('password')}
        />
        <Group position="right" mt="md">
          <Button type="submit">
            {i18n.get['Apply']}
          </Button>
        </Group>
      </form>
    )
  };

  function UserDisplayNameForm(user: UmsUser) {
    const userDisplayNameForm = useForm({ initialValues: {id:user.id,displayName:user.displayName} });
    const handleUserDisplayNameSubmit = (values: typeof userDisplayNameForm.values) => {
      const data = {operation:'modifyuser', userid:user.id, name:values.displayName};
      postAccountAction(data, i18n.get['DisplayNameUpdate'], i18n.get['DisplayNameUpdated'], i18n.get['DisplayNameNotUpdated']);
    }
    return (
      <form onSubmit={userDisplayNameForm.onSubmit(handleUserDisplayNameSubmit)}>
        <Divider my="sm" label={i18n.get['DisplayName']} />
        <TextInput
          label={i18n.get['DisplayName']}
          name="displayName"
          {...userDisplayNameForm.getInputProps('displayName')}
        />
        <Group position="right" mt="md">
          <Button type="submit">
            {i18n.get['Apply']}
          </Button>
        </Group>
      </form>
    )
  };

  function UserGroupForm(user: UmsUser) {
    const userGroupForm = useForm({ initialValues: {id:user.id,groupId:user.groupId.toString()} });
    const handleUserGroupSubmit = (values: typeof userGroupForm.values) => {
      const data = {operation:'modifyuser', userid:user.id, groupid:values.groupId};
      postAccountAction(data, i18n.get['UserGroupChange'], i18n.get['UserGroupChanged'], i18n.get['UserGroupNotChanged']);
    };
    return (
      <form onSubmit={userGroupForm.onSubmit(handleUserGroupSubmit)}>
        <Divider my="sm" label={i18n.get['Group']} />
        <Select
          label={i18n.get['Group']}
          name="groupId"
          disabled={!canManageGroups}
          data={groupSelectionDatas}
          {...userGroupForm.getInputProps('groupId')}
        />
        {canManageGroups && (
          <Group position="right" mt="md">
            <Button type="submit">
              {i18n.get['Apply']}
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
      postAccountAction(data, i18n.get['UserDeletion'], i18n.get['UserDeleted'], i18n.get['UserNotDeleted']);
    }
    const [opened, setOpened] = useState(false);
    return (
      <form onSubmit={userDeleteForm.onSubmit(handleUserDeleteSubmit)}>
        <Divider my="sm" />
        { opened ? (
          <Group position="right" mt="md">
            <Text color="red">{i18n.get['UserWillBeDeleted']}</Text>
            <Button onClick={() => setOpened(false)}>
              {i18n.get['Cancel']}
            </Button>
            <Button type="submit" color="red" leftIcon={<ExclamationMark />} rightIcon={<ExclamationMark />}>
              {i18n.get['Confirm']}
            </Button>
          </Group>
        ) : (
          <Group position="right" mt="md">
            <Text color="red">{i18n.get['DeleteUser']}</Text>
            <Button onClick={() => setOpened(true)} color="red" leftIcon={<X />}>
              {i18n.get['Delete']}
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
    const user = {id:0,username:i18n.get['NewUser']} as UmsUser;
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
      postAccountAction(data, i18n.get['DisplayNameUpdate'], i18n.get['DisplayNameUpdated'], i18n.get['DisplayNameNotUpdated']);
    }
    return (
      <form onSubmit={groupDisplayNameForm.onSubmit(handleGroupDisplayNameSubmit)}>
        <Divider my="sm" label={i18n.get['DisplayName']} />
        <TextInput
          label={i18n.get['DisplayName']}
          name="displayName"
          {...groupDisplayNameForm.getInputProps('displayName')}
        />
        <Group position="right" mt="md">
          <Button type="submit">
            {i18n.get['Update']}
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
      postAccountAction(data, i18n.get['PermissionsUpdate'], i18n.get['PermissionsUpdated'], i18n.get['PermissionsNotUpdated']);
    }
    return (
      <form onSubmit={groupPermissionsForm.onSubmit(handleGroupPermissionsSubmit)}>
        <Divider my="sm" label={i18n.get['Permissions']} />
        <CheckboxGroup
          value={permissions}
          onChange={setPermissions}
          orientation="vertical"
        >
          <Checkbox value="*" label={i18n.get['AllPermissions']} />
          <Checkbox value="server_restart" label={i18n.get['RestartServer']} />
          <Checkbox value="users_manage" label={i18n.get['ManageUsers']} />
          <Checkbox value="groups_manage" label={i18n.get['ManageGroups']} />
          <Checkbox value="settings_view" label={i18n.get['ViewSettings']} />
          <Checkbox value="settings_modify" label={i18n.get['ModifySettings']} />
        </CheckboxGroup>
        <Group position="right" mt="md">
          <Button type="submit">
            {i18n.get['Update']}
          </Button>
        </Group>
      </form>
    )
  };

  function GroupDeleteForm(group: UmsGroup) {
    const groupDeleteForm = useForm({ initialValues: {id:group.id} });
    const handleGroupDeleteSubmit = () => {
      const data = {operation:'deletegroup', groupid:group.id};
      postAccountAction(data, i18n.get['GroupDeletion'], i18n.get['GroupDeleted'], i18n.get['GroupNotDeleted']);
    }
    const [opened, setOpened] = useState(false);
    return group.id > 1 ? (
      <form onSubmit={groupDeleteForm.onSubmit(handleGroupDeleteSubmit)}>
        <Divider my="sm" />
        { opened ? (
          <Group position="right" mt="md">
            <Text color="red">{i18n.get['GroupWillBeDeleted']}</Text>
            <Button onClick={() => setOpened(false)}>
              {i18n.get['Cancel']}
            </Button>
            <Button type="submit" color="red" leftIcon={<ExclamationMark />} rightIcon={<ExclamationMark />}>
              {i18n.get['Confirm']}
            </Button>
          </Group>
        ) : (
          <Group position="right" mt="md">
            <Text color="red">{i18n.get['DeleteGroup']}</Text>
            <Button onClick={() => setOpened(true)} color="red" leftIcon={<X />}>
              {i18n.get['Delete']}
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
      postAccountAction(data, i18n.get['GroupCreation'], i18n.get['NewGroupCreated'], i18n.get['NewGroupNotCreated']);
    }
    return (
      <form onSubmit={newGroupForm.onSubmit(handleNewGroupSubmit)}>
        <TextInput
          required
          label={i18n.get['DisplayName']}
          name="displayName"
          {...newGroupForm.getInputProps('displayName')}
        />
        <Group position="right" mt="md">
          <Button type="submit">
            {i18n.get['Create']}
          </Button>
        </Group>
      </form>
    )
  };

  function NewGroupAccordion() {
    const group = {id:0,displayName:i18n.get['NewGroup']} as UmsGroup;
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

  function AuthenticationServiceButton() {
    const [authOpened, setAuthOpened] = useState(false);
    const handleAuthenticationToggle = () => {
      const data = {operation:'authentication', enabled:!accounts.enabled};
      postAccountAuthAction(data, accounts.enabled?i18n.get['AuthenticationServiceNotDisabled']:i18n.get['AuthenticationServiceNotEnabled']);
    }
    return accounts.enabled ? (<>
      <Group position='left' mt='md'>
        <Modal
          centered
          opened={authOpened}
          onClose={() => setAuthOpened(false)}
          title={i18n.get['Warning']}
        >
          <Text>{allowHtml(i18n.get['DisablingAuthenticationReduces'])}</Text>
          <Group position='right' mt='md'>
            <Button onClick={() => setAuthOpened(false)}>{i18n.get['Cancel']}</Button>
            <Button color="red" onClick={() => handleAuthenticationToggle()}>{i18n.get['Confirm']}</Button>
          </Group>
        </Modal>
        <Button onClick={() => setAuthOpened(true)}>{i18n.get['Disable']}</Button>
        <Text>{i18n.get['AuthenticationServiceEnabled']}</Text>
      </Group>
      <AuthenticateLocalhostAdmin />
    </>) : (
      <Group position='left' mt='md'>
        <Button onClick={() => handleAuthenticationToggle()}>{i18n.get['Enable']}</Button>
        <Text>{i18n.get['AuthenticationServiceDisabled']}</Text>
      </Group>
    );
  };

  function AuthenticateLocalhostAdmin() {
    const [localhostOpened, setLocalhostOpened] = useState(false);
    const handleAuthenticateLocalhostToggle = () => {
      const data = {operation:'localhost', enabled:!accounts.localhost};
      postAccountAuthAction(data, i18n.get['AuthenticationServiceNotToggled']);
    }
    return accounts.localhost ? (
      <Group position='left' mt='md'>
        <Button onClick={() => handleAuthenticateLocalhostToggle()}>{i18n.get['Disable']}</Button>
        <Text>{i18n.get['AuthenticateLocalhostAdminEnabled']}</Text>
      </Group>
    ) : (<>
      <Group position='left' mt='md'>
        <Modal
          centered
          opened={localhostOpened}
          onClose={() => setLocalhostOpened(false)}
          title={i18n.get['Warning']}
        >
          <Text>{i18n.get['EnablingAuthenticateLocalhost']}</Text>
          <Group position='right' mt='md'>
            <Button onClick={() => setLocalhostOpened(false)}>{i18n.get['Cancel']}</Button>
            <Button color="red" onClick={() => handleAuthenticateLocalhostToggle()}>{i18n.get['Confirm']}</Button>
          </Group>
        </Modal>
        <Button onClick={() => setLocalhostOpened(true)}>{i18n.get['Enable']}</Button>
        <Text>{i18n.get['AuthenticateLocalhostAdminDisabled']}</Text>
      </Group>
    </>)
  };

  return (
      <Box sx={{ maxWidth: 700 }} mx="auto">
          {canManageGroups ? (
            <Tabs active={activeTab} onTabChange={setActiveTab}>
              {session.authenticate && (
                <Tabs.Tab label={i18n.get['Users']}>
                  <UsersAccordions />
                </Tabs.Tab>
              )}
              {session.authenticate && (
                <Tabs.Tab label={i18n.get['Groups']}>
                  <GroupsAccordions />
                </Tabs.Tab>
              )}
              {canModifySettings && (
                <Tabs.Tab label={i18n.get['Settings']}>
                   <AuthenticationServiceButton />
                </Tabs.Tab>
              )}
            </Tabs>
          ) : session.authenticate ? (
            <UsersAccordions />
          ) : (
            <Box sx={{ maxWidth: 700 }} mx="auto">
              <Text color="red">{i18n.get['YouNotHaveAccessArea']}</Text>
            </Box>
          )}
      </Box>
  );
};

export default Accounts;
