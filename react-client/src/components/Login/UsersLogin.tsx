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
import { Carousel } from '@mantine/carousel'
import { Button, PinInput, Stack, TextInput, VisuallyHidden } from '@mantine/core'
import { useForm } from '@mantine/form'
import { IconLock } from '@tabler/icons-react'
import { useState } from 'react'

import { login, loginPin } from '../../services/auth-service'
import { I18nInterface } from '../../services/i18n-service'
import { SessionInterface, UmsUserLogin } from '../../services/session-service'
import { showError } from '../../utils/notifications'
import LoginUserCard from './LoginUserCard'

export default function UsersLogin({ i18n, session }: { i18n: I18nInterface, session: SessionInterface }) {
  const [selected, setSelected] = useState<UmsUserLogin | undefined>((session.users && session.users.length > 0) ? session.users[0] : undefined)

  const UserSlides = session.users?.map((user) => {
    return <Carousel.Slide><LoginUserCard user={user} /></Carousel.Slide>
  })

  const onSlideChange = (index: number) => {
    if (session.users && session.users.length > index) {
      setSelected(session.users[index])
    }
  }
  const handlePinLogin = (id: number, pin: string) => {
    loginPin(id, pin).then(
      () => {
        session.refresh()
      },
      () => {
        showError({
          id: 'pwd-error',
          title: i18n.get('Error'),
          message: i18n.get('ErrorLoggingIn'),
        })
      },
    )
  }

  const NoneLogin = () => {
    return (selected && selected.login === 'none')
      ? (
          <Button
            variant="default"
            onClick={() => handlePinLogin(selected.id, '')}
          >
            {i18n.get('LogIn')}
          </Button>
        )
      : undefined
  }

  const PinLogin = () => {
    return (selected && selected.login === 'pin')
      ? (
          <PinInput
            oneTimeCode
            mask
            onComplete={(value: string) => {
              handlePinLogin(selected.id, value)
            }}
          />
        )
      : undefined
  }

  const PassLogin = () => {
    return (selected && selected.login === 'pass')
      ? (
          <PassLoginForm user={selected} />
        )
      : undefined
  }

  function PassLoginForm({ user }: { user: UmsUserLogin }) {
    const form = useForm({
      initialValues: {
        username: user.username,
        password: '',
      },
    })
    const handleLogin = (values: typeof form.values) => {
      const { username, password } = values
      login(username, password).then(
        () => {
          session.refresh()
        },
        () => {
          showError({
            id: 'pwd-error',
            title: i18n.get('Error'),
            message: i18n.get('ErrorLoggingIn'),
          })
        },
      )
    }
    return (
      <form onSubmit={form.onSubmit(handleLogin)}>
        <Stack>
          <VisuallyHidden>
            <TextInput
              name="username"
              value={user.username}
            />
          </VisuallyHidden>
          <TextInput
            required
            label={i18n.get('Password')}
            type="password"
            leftSection={<IconLock size={14} />}
            {...form.getInputProps('password')}
          />
          <Button variant="default" type="submit">{i18n.get('LogIn')}</Button>
        </Stack>
      </form>
    )
  }

  return (
    <Stack justify="center" align="center" h="100%">
      <Carousel slideSize={180} slideGap={20} onSlideChange={onSlideChange}>
        {UserSlides}
      </Carousel>
      <PassLogin />
      <PinLogin />
      <NoneLogin />
    </Stack>
  )
}
