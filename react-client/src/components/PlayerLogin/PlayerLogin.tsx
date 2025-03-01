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
import { TextInput, Button, Group, Box, Text, Space } from '@mantine/core';
import { useForm } from '@mantine/form';
import { IconUser, IconLock } from '@tabler/icons-react';
import { useEffect } from 'react';

import { login } from '../../services/auth-service';
import { I18nInterface } from '../../services/i18n-service';
import { MainInterface } from '../../services/main-service';
import { SessionInterface } from '../../services/session-service';
import { showError } from '../../utils/notifications';

const Login = ({ i18n, main, session }: { i18n:I18nInterface, main:MainInterface, session:SessionInterface }) => {

  //set the document Title to Login
  useEffect(() => {
    document.title="Universal Media Server - Login";
    session.stopSse()
    session.stopPlayerSse()
    main.setNavbarValue(undefined)
  }, []);

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
        session.refresh();
      },
      () => {
        showError({
          id: 'pwd-error',
          title: i18n.get('Error'),
          message: i18n.get('ErrorLoggingIn'),
        });
      }
    );
  };

  return (
    <Box style={{ maxWidth: 300 }} mx='auto'>
      <form onSubmit={form.onSubmit(handleLogin)}>
        <Text size='xl'>Universal Media Server</Text>
        <Text size='lg'>{i18n.get('LogIn')}</Text>
        <Space h='md' />
        <TextInput
          required
          label={i18n.get('Username')}
          leftSection={<IconUser size={14} />}
          {...form.getInputProps('username')}
        />
        <TextInput
          required
          label={i18n.get('Password')}
          type='password'
          leftSection={<IconLock size={14} />}
          {...form.getInputProps('password')}
        />
        <Group justify='flex-end' mt='md'>
          <Button type='submit'>{i18n.get('LogIn')}</Button>
        </Group>
      </form>
    </Box>
  );
};

export default Login;
