import { showNotification } from '@mantine/notifications';
import { TextInput, Button, Group, Box, Text, Space } from '@mantine/core';
import { User, Lock } from 'tabler-icons-react';
import { useForm } from '@mantine/form';
import { useContext } from 'react';

import { create, login } from '../../services/auth.service';
import sessionContext from '../../contexts/session-context';

const Login = () => {
  const form = useForm({
    initialValues: {
      username: '',
      password: '',
    },
  });

  const session = useContext(sessionContext)
  console.log('session in login is', session);

  const handleFormSubmit = (values: typeof form.values) => {
    return sessionStorage.firstlogin === true ? handleUserCreation(values) : handleLogin(values);
  }

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
          title: 'Error',
          message: 'Error on user creation',
          autoClose: 3000,
        });
      }
    );
  };

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
          title: 'Error',
          message: 'Error logging in',
          autoClose: 3000,
        });
      }
    );
  };
  return (
    <Box sx={{ maxWidth: 300 }} mx='auto'>
      <form onSubmit={form.onSubmit(handleFormSubmit)}>
        <Text size="xl">{session.firstLogin === true ? 'Create admin user' : 'Log in' }</Text>
        <Space h="md" />
        <TextInput
          required
          label='Username'
          icon={<User size={14} />}
          {...form.getInputProps('username')}
        />
        <TextInput
          required
          label='Password'
          type='password'
          icon={<Lock size={14} />}
          {...form.getInputProps('password')}
        />
        <Group position='right' mt='md'>
          <Button type='submit'>Submit</Button>
        </Group>
      </form>
    </Box>
  );
};

export default Login;
