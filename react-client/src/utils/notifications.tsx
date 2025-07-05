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
import { Box, Stack } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { MouseEventHandler } from 'react'

const setDuration = (key: string, defaultDuration: number, duration: number) => {
  if (duration == defaultDuration) {
    localStorage.removeItem(key)
  }
  else if (!isNaN(duration)) {
    localStorage.setItem(key, duration.toString())
  }
}
const getDuration = (key: string, defaultDuration: number) => {
  if (!localStorage.getItem(key)) {
    return defaultDuration
  }
  const duration = Number(localStorage.getItem(key))
  return isNaN(duration) ? defaultDuration : duration
}

const keyInfoDuration = 'infoDuration'
const defaultInfoDuration = 4000
export const setInfoDuration = (duration: number) => {
  setDuration(keyInfoDuration, defaultInfoDuration, duration)
}
export const getInfoDuration = () => {
  return getDuration(keyInfoDuration, defaultInfoDuration)
}

const keyWarningDuration = 'warningDuration'
const defaultWarningDuration = 8000
export const setWarningDuration = (duration: number) => {
  setDuration(keyWarningDuration, defaultWarningDuration, duration)
}
export const getWarningDuration = () => {
  return getDuration(keyWarningDuration, defaultWarningDuration)
}

const keyErrorDuration = 'errorDuration'
const defaultErrorDuration = 0
export const setErrorDuration = (duration: number) => {
  setDuration(keyErrorDuration, defaultErrorDuration, duration)
}
export const getErrorDuration = () => {
  return getDuration(keyErrorDuration, defaultErrorDuration)
}

const getMessage = (message: React.ReactNode, message2?: React.ReactNode) => {
  return message2
    ? (
        <Stack gap="xs">
          <Box>{message}</Box>
          <Box>{message2}</Box>
        </Stack>
      )
    : message
}
export const showLoading = ({ id, message, message2, title }: { id: string, message: React.ReactNode, message2?: React.ReactNode, title?: React.ReactNode }) => {
  notifications.show({
    id: id,
    loading: true,
    title: title,
    message: getMessage(message, message2),
    autoClose: false,
    withCloseButton: false,
  })
}

export const showInfo = ({ id, message, message2, title }: { id?: string, message: React.ReactNode, message2?: React.ReactNode, title?: React.ReactNode }) => {
  const duration = getInfoDuration()
  notifications.show({
    id: id,
    title: title,
    message: getMessage(message, message2),
    autoClose: duration === 0 ? false : duration,
    withCloseButton: duration === 0 || duration > 5000,
  })
}

export const showSuccess = ({ id, message, message2, title }: { id?: string, message: React.ReactNode, message2?: React.ReactNode, title?: React.ReactNode }) => {
  const duration = getInfoDuration()
  notifications.show({
    id: id,
    color: 'teal',
    title: title,
    message: getMessage(message, message2),
    autoClose: duration === 0 ? false : duration,
    withCloseButton: duration === 0 || duration > 5000,
  })
}

export const showWarning = ({ id, message, message2, title, onClick }: { id?: string, message: React.ReactNode, message2?: React.ReactNode, title?: React.ReactNode, onClick?: MouseEventHandler | undefined }) => {
  const duration = getWarningDuration()
  notifications.show({
    id: id,
    color: 'orange',
    title: title,
    message: getMessage(message, message2),
    autoClose: duration === 0 ? false : duration,
    onClick: onClick,
    withCloseButton: duration === 0 || duration > 5000,
  })
}

export const showError = ({ id, message, message2, title, icon, onClick }: { id?: string, message: React.ReactNode, message2?: React.ReactNode, title?: React.ReactNode, icon?: React.ReactNode, onClick?: MouseEventHandler | undefined }) => {
  const duration = getErrorDuration()
  notifications.show({
    id: id,
    color: 'red',
    title: title,
    message: getMessage(message, message2),
    icon: icon,
    autoClose: duration === 0 ? false : duration,
    withCloseButton: duration === 0 || duration > 5000,
    onClick: onClick,
  })
}

export const updateSuccess = ({ id, message, message2, title, icon }: { id: string, message: React.ReactNode, message2?: React.ReactNode, title?: React.ReactNode, icon?: React.ReactNode }) => {
  const duration = getInfoDuration()
  notifications.update({
    id: id,
    loading: false,
    color: 'teal',
    title: title,
    message: getMessage(message, message2),
    icon: icon,
    autoClose: duration === 0 ? false : duration,
    withCloseButton: duration === 0 || duration > 5000,
  })
}

export const updateInfo = ({ id, message, message2, title, icon }: { id: string, message: React.ReactNode, message2?: React.ReactNode, title?: React.ReactNode, icon?: React.ReactNode }) => {
  const duration = getInfoDuration()
  notifications.update({
    id: id,
    loading: false,
    color: undefined,
    title: title,
    message: getMessage(message, message2),
    icon: icon,
    autoClose: duration === 0 ? false : duration,
    withCloseButton: duration === 0 || duration > 5000,
  })
}

export const updateError = ({ id, message, message2, title, icon, onClick }: { id: string, message: React.ReactNode, message2?: React.ReactNode, title?: React.ReactNode, icon?: React.ReactNode, onClick?: MouseEventHandler | undefined }) => {
  const duration = getErrorDuration()
  notifications.update({
    id: id,
    loading: false,
    color: 'red',
    title: title,
    message: getMessage(message, message2),
    icon: icon,
    autoClose: duration === 0 ? false : duration,
    withCloseButton: duration === 0 || duration > 5000,
    onClick: onClick,
  })
}
