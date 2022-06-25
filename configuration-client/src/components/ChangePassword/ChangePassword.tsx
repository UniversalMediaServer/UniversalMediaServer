import { showNotification } from '@mantine/notifications';
import { changePassword } from '../../services/auth.service';
import { TextInput, Button, Group, Box, Text, Space } from '@mantine/core';
import { Lock, LockOpen } from 'tabler-icons-react';
import { useForm } from '@mantine/form';

const ChangePassword = () => {
  const form = useForm({
      initialValues: {
          password: '',
          confirmpassword: '',
          newpassword: '',
      },
    });

  const handleChangePassword = (values: typeof form.values) => {
    const { password, confirmpassword, newpassword } = values;
    if (newpassword !== confirmpassword) {
        showNotification({
            id: 'pwd-error',
            color: 'red',
            title: 'Error',
            message: 'New passwords do not match, try again',
            autoClose: 3000,
          });
          return;
    }
    changePassword(password, confirmpassword).then(
      (data) => {
        if (data.error) {
          showNotification({
            id: 'data-loading',
            color: 'red',
            title: 'Error',
            message: data.error,
            autoClose: 3000,
          });
          return;
        }
        showNotification({
            id: 'data-loading',
            color: 'teal',
            title: 'Success',
            message: 'Password updated successfully',
            autoClose: 3000,
          });
          setTimeout(()=> {
            window.location.href = '/';
          }, 3000);
      },
      () => {
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
        <Text size="xl">Change password</Text>
        <Space h="md" />
        <TextInput
          required
          label='Current password'
          type='password'
          icon={<LockOpen size={14} />}
          {...form.getInputProps('password')}
        />
        <TextInput
          required
          label='New password'
          type='password'
          icon={<Lock size={14} />}
          {...form.getInputProps('newpassword')}
        />
        <TextInput
          required
          label='Confirm new password'
          type='password'
          icon={<Lock size={14} />}
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
