import { TextInput, Button, Group, Box, Text, Space } from '@mantine/core';
import { useForm } from '@mantine/form';
import { showNotification } from '@mantine/notifications';
import { useContext } from 'react';
import { User, Lock } from 'tabler-icons-react';

import I18nContext from '../../contexts/i18n-context';
import { create, login } from '../../services/auth.service';
import SessionContext from '../../contexts/session-context';

const Login = () => {
  const i18n = useContext(I18nContext);
  const session = useContext(SessionContext);

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
          title: i18n.get['Dialog.Error'],
          message: i18n.get['WebGui.LoginError'],
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
          title: i18n.get['Dialog.Error'],
          message: i18n.get['WebGui.AccountsUserCreationError'],
          autoClose: 3000,
        });
      }
    );
  };

  return (
    <Box sx={{ maxWidth: 300 }} mx='auto'>
      <form onSubmit={form.onSubmit(session.noAdminFound ? handleUserCreation : handleLogin)}>
          <Text size="xl">Universal Media Server</Text>
          <Text size="lg">{ session.noAdminFound ? i18n.get['WebGui.LoginCreateFirstAdmin'] : i18n.get['WebGui.Login'] }</Text>
        <Space h="md" />
        <TextInput
          required
          label={i18n.get['WebGui.AccountsUsername']}
          icon={<User size={14} />}
          {...form.getInputProps('username')}
        />
        <TextInput
          required
          label={i18n.get['WebGui.AccountsPassword']}
          type='password'
          icon={<Lock size={14} />}
          {...form.getInputProps('password')}
        />
        <Group position='right' mt='md'>
          <Button type='submit'>{session.noAdminFound ? i18n.get['WebGui.ButtonCreate'] : i18n.get['WebGui.ButtonLogin']}</Button>
        </Group>
      </form>
    </Box>
  );
};

export default Login;
