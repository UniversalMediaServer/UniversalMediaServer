import { showNotification } from '@mantine/notifications';
import { login } from '../../services/auth.service';
import { TextInput, Button, Group, Box } from '@mantine/core';
import { useForm } from '@mantine/form';

const Login = () => {
    const form = useForm({
        initialValues: {
          username: '',
          password: '',
        },
      });

      const handleLogin = (values: typeof form.values) => {
        const { username, password } = values;
        login(username, password).then(
          ({firstLogin}) => {
            if (firstLogin === "true") {
              return window.location.href = '/changepassword'
            }
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
          <form onSubmit={form.onSubmit(handleLogin)}>
            <h1>Log In</h1>
            <TextInput
              required
              label='Username'
              {...form.getInputProps('username')}
            />
            <TextInput
              required
              label='Password'
              type='password'
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
