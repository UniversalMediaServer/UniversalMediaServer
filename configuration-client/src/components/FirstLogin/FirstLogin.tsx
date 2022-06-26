import { TextInput, Button, Group, Box, Text, Space } from '@mantine/core';
import { useForm } from '@mantine/form';
import { showNotification } from '@mantine/notifications';
import { useContext } from 'react';
import { User, Lock } from 'tabler-icons-react';

import { create } from '../../services/auth.service';
import I18nContext from '../../contexts/i18n-context';

const FirstLogin = () => {
  const i18n = useContext(I18nContext);

  const form = useForm({
    initialValues: {
      username: '',
      password: '',
    },
  });

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

  return (
    <Box sx={{ maxWidth: 300 }} mx='auto'>
      <form onSubmit={form.onSubmit(handleUserCreation)}>
        <Text size="xl">Universal Media Server</Text>
        <Text size="lg">Create your first admin user</Text>
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
          <Button type="submit">
            {i18n.get['LooksFrame.9']}
          </Button>
        </Group>
      </form>
    </Box>
  );
};

export default FirstLogin;
