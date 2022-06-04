import React, { useState } from "react";
import { login } from "../../services/auth.service";
import { TextInput, Checkbox, Button, Group, Box } from '@mantine/core';
import { useForm } from '@mantine/form';

const Login = ({ }) => {
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
            window.location.href = '/';
          },
          (error) => {
            const resMessage =
              (error.response &&
                error.response.data &&
                error.response.data.message) ||
              error.message ||
              error.toString();
          }
        );
      };
    return (
        <Box sx={{ maxWidth: 300 }} mx="auto">
          <form onSubmit={form.onSubmit(handleLogin)}>
            <TextInput
              required
              label="Username"
              {...form.getInputProps('username')}
            />
            <TextInput
              required
              label="Password"
              type="password"
              {...form.getInputProps('password')}
            />
            <Group position="right" mt="md">
              <Button type="submit">Submit</Button>
            </Group>
          </form>
        </Box>
      );
};
export default Login;
