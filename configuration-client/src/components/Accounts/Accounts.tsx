import { Accordion, Avatar, Box, Group, Text } from '@mantine/core';
import { UmsUser } from '../../contexts/session-context';
import AccountsContext, { UmsAccounts } from '../../contexts/accounts-context';
import { AccountsProvider } from '../../providers/accounts-provider';

const Accounts = () => {

  function UserAccordionLabel(user: UmsUser) {
    var showAsUsername = (user.displayName === null || user.displayName.length === 0 || user.displayName === user.username);
    var displayName = showAsUsername ? user.username : user.displayName;
    return (
      <Group noWrap>
        <Avatar radius="xl" size="lg" />
        <div>
          <Text>{displayName}</Text>
          {!showAsUsername && (
            <Text size="sm" color="dimmed" weight={400}>
              {user.username}
            </Text>
          )}
        </div>
      </Group>
    );
  };

  function UserAccordion(user: UmsUser) {
    return (
        <Accordion.Item label={<UserAccordionLabel {...user} />} key={user.id}>
          <Text size="sm">{user.displayName}</Text>
        </Accordion.Item>
    );
  };

  function AccountsAccordion(accounts: UmsAccounts) {
    const usersAccordions = accounts.users.map((user) => (
        <UserAccordion {...user} key={user.id} />
    ));
    return (
      <Accordion initialItem={-1} iconPosition="right">
        {usersAccordions}
      </Accordion>
    );
  };

  return (
    <Box sx={{ maxWidth: 700 }} mx="auto">
      <AccountsProvider>
        <Text size="xl">Accounts</Text>
        <AccountsContext.Consumer>
	      { accounts => (
	        <AccountsAccordion {...accounts} />
          )}
        </AccountsContext.Consumer>
      </AccountsProvider>
    </Box>
  );
};

export default Accounts;
