import { showNotification } from '@mantine/notifications';
import { create } from '../../services/auth.service';
import { TextInput, Button, Group, Box, Text, Space } from '@mantine/core';
import { User, Lock } from 'tabler-icons-react';
import { useForm } from '@mantine/form';

const FirstLogin = () => {
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
          <Text size="xl">Welcome to Universal Media Server</Text>
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
            <Button type='submit'>Submit</Button>
          </Group>
        </form>
      </Box>
    );
};

export default FirstLogin;
