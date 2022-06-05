import { showNotification } from '@mantine/notifications';
import { changePassword } from '../../services/auth.service';
import { TextInput, Checkbox, Button, Group, Box } from '@mantine/core';
import { useForm } from '@mantine/form';
import { isReturnStatement } from 'typescript';

const ChangePassword = ({ }) => {
    const form = useForm({
        initialValues: {
            password: '',
            confirmpassword: '',
        },
      });

      const handleChangePassword = (values: typeof form.values) => {
        const { password, confirmpassword } = values;
        if (password != confirmpassword) {
            showNotification({
                id: 'pwd-error',
                color: 'red',
                title: 'Error',
                message: 'Passwords do not match, try again',
                autoClose: 3000,
              });
              return;
        }
        changePassword(password).then(
          () => {
            showNotification({
                id: 'data-loading',
                color: 'teal',
                title: 'Success',
                message: 'Password updated successfully',
                autoClose: 3000,
              });
              window.location.href = '/';
          },
          (error) => {
            showNotification({
                id: 'pwd-error',
                color: 'red',
                title: 'Error',
                message: 'Error updating password',
                autoClose: 3000,
              });
          }
        );
      };
    return (
        <Box sx={{ maxWidth: 300 }} mx='auto'>
          <form onSubmit={form.onSubmit(handleChangePassword)}>
            <h1>Change password</h1>
            <TextInput
              required
              label='Password'
              type='password'
              {...form.getInputProps('password')}
            />
            <TextInput
              required
              label='Confirm Password'
              type='password'
              {...form.getInputProps('confirmpassword')}
            />
            <Group position='right' mt='md'>
              <Button type='submit'>Submit</Button>
            </Group>
          </form>
        </Box>
      );
};

export default ChangePassword;
