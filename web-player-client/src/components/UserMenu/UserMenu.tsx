import { Menu, ActionIcon } from '@mantine/core';
import { useContext } from 'react';
import { Logout, Menu2 } from 'tabler-icons-react';

import I18nContext from '../../contexts/i18n-context';
import SessionContext from '../../contexts/session-context';
import { redirectToLogin } from '../../services/auth-service';

function UserMenu() {
  const i18n = useContext(I18nContext);
  const session = useContext(SessionContext);

  return (
    <Menu>
      <Menu.Target>
        <ActionIcon variant="default" size={30}>
          <Menu2 size={16} />
        </ActionIcon>
      </Menu.Target>
      <Menu.Dropdown>
      {session.account && session.account?.user.id !== 2147483647 && (
        <Menu.Item
          color="red"
          icon={<Logout size={14} />}
          onClick={() => {
            redirectToLogin();
          }}
        >
          {i18n.get['LogOut']}
        </Menu.Item>
      )}
      </Menu.Dropdown>
    </Menu>
  );
}
export default UserMenu;