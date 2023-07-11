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
import { TextInput, Button, Group, Box, Text, Space, Divider, Modal } from '@mantine/core';
import { useForm } from '@mantine/form';
import { showNotification } from '@mantine/notifications';
import { useContext, useState } from 'react';
import { User, Lock } from 'tabler-icons-react';

import I18nContext from '../../contexts/i18n-context';
import SessionContext from '../../contexts/session-context';
import { clearJwt, create, disable, login } from '../../services/auth-service';
import { allowHtml } from '../../utils';

const Login = () => {
  const i18n = useContext(I18nContext);
  const session = useContext(SessionContext);
  const [opened, setOpened] = useState(false);
  const form = useForm({
    initialValues: {
      username: '',
      password: '',
    },
  });

  const handleLogin = (values: typeof form.values) => {
    const { username, password } = values;
    login(username, password).then(
      () => {
        window.location.reload();
      },
      (error) => {
        showNotification({
          id: 'pwd-error',
          color: 'red',
          title: i18n.get['Error'],
          message: i18n.get['ErrorLoggingIn'],
          autoClose: 3000,
        });
      }
    );
  };

  const handleUserCreation = (values: typeof form.values) => {
    const { username, password } = values;
    create(username, password).then(
      () => {
        window.location.reload();
      },
      (error) => {
        showNotification({
          id: 'user-creation-error',
          color: 'red',
          title: i18n.get['Error'],
          message: i18n.get['NewUserNotCreated'],
          autoClose: 3000,
        });
      }
    );
  };

  const handleAuthDisable = () => {
    disable().then(
      () => {
        clearJwt();
        window.location.reload();
      },
      (error) => {
        showNotification({
          id: 'auth-disable-error',
          color: 'red',
          title: i18n.get['Error'],
          message: i18n.get['AuthenticationServiceNotDisabled'],
          autoClose: 3000,
        });
      }
    );
  };

  return (
    <Box sx={{ maxWidth: 300 }} mx='auto'>
      <form onSubmit={form.onSubmit(session.noAdminFound ? handleUserCreation : handleLogin)}>
        <Text size='xl'>Universal Media Server</Text>
        <Text size='lg'>{session.noAdminFound ? i18n.get['CreateFirstAdmin'] : i18n.get['LogIn']}</Text>
        <Space h='md' />
        <TextInput
          required
          label={i18n.get['Username']}
          icon={<User size={14} />}
          {...form.getInputProps('username')}
        />
        <TextInput
          required
          label={i18n.get['Password']}
          type='password'
          icon={<Lock size={14} />}
          {...form.getInputProps('password')}
        />
        <Group position='right' mt='md'>
          <Button type='submit'>{session.noAdminFound ? i18n.get['Create'] : i18n.get['LogIn']}</Button>
        </Group>
        {session.noAdminFound && session.authenticate && (
          <>
            <Divider my='lg' label={i18n.get['Or']} labelPosition='center' />
            <Modal
              centered
              opened={opened}
              onClose={() => setOpened(false)}
              title={i18n.get['Warning']}
            >
              <Text>{allowHtml(i18n.get['DisablingAuthenticationReduces'])}</Text>
              <Group position='right' mt='md'>
                <Button onClick={() => setOpened(false)}>{i18n.get['Cancel']}</Button>
                <Button color='red' onClick={() => handleAuthDisable()}>{i18n.get['Confirm']}</Button>
              </Group>
            </Modal>
            <Group position='center' mt='md'>
              <Button color='red' onClick={() => setOpened(true)}>{i18n.get['DisableAuthentication']}</Button>
            </Group>
          </>
        )}
      </form>
    </Box>
  );
};

export default Login;
