import { TextInput, Button, Group, Box, Text, Space } from '@mantine/core';
import { useForm } from '@mantine/form';
import { showNotification } from '@mantine/notifications';
import { useContext } from 'react';
import { User, Lock } from 'tabler-icons-react';

import I18nContext from '../../contexts/i18n-context';
import { login } from '../../services/auth-service';

const Login = () => {
  const i18n = useContext(I18nContext);
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
      () => {
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

  return (
    <Box sx={{ maxWidth: 300 }} mx='auto'>
      <form onSubmit={form.onSubmit(handleLogin)}>
          <Text size="xl">Universal Media Server</Text>
          <Text size="lg">{ i18n.get['LogIn'] }</Text>
        <Space h="md" />
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
          <Button type='submit'>{i18n.get['LogIn']}</Button>
        </Group>
      </form>
    </Box>
  );
};

export default Login;
