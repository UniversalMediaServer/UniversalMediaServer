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
import { notifications } from '@mantine/notifications'
import { MouseEventHandler } from 'react'

export const getErrorDuration = () => {
  if (!localStorage.getItem('errorDuration')) {
    return 0
  }
  return Number(localStorage.getItem('errorDuration'))
}

export const getWarningDuration = () => {
  if (!localStorage.getItem('warningDuration')) {
    return 8000
  }
  return Number(localStorage.getItem('warningDuration'))
}

export const getInfoDuration = () => {
  if (!localStorage.getItem('infoDuration')) {
    return 4000
  }
  return Number(localStorage.getItem('infoDuration'))
}

export const showLoading = ({ id, message, title }: { id: string, message: React.ReactNode, title?: React.ReactNode }) => {
  notifications.show({
    id: id,
    loading: true,
    title: title,
    message: message,
    autoClose: false,
    withCloseButton: false,
  })
}

export const showInfo = ({ id, message, title }: { id?: string, message: React.ReactNode, title?: React.ReactNode }) => {
  const duration = getInfoDuration()
  notifications.show({
    id: id,
    title: title,
    message: message,
    autoClose: duration === 0 ? false : duration,
    withCloseButton: duration === 0 || duration > 5000,
  })
}

export const showSuccess = ({ id, message, title }: { id?: string, message: React.ReactNode, title?: React.ReactNode }) => {
  const duration = getInfoDuration()
  notifications.show({
    id: id,
    color: 'teal',
    title: title,
    message: message,
    autoClose: duration === 0 ? false : duration,
    withCloseButton: duration === 0 || duration > 5000,
  })
}

export const showWarning = ({ id, message, title, onClick }: { id?: string, message: React.ReactNode, title?: React.ReactNode, onClick?: MouseEventHandler | undefined }) => {
  const duration = getWarningDuration()
  notifications.show({
    id: id,
    color: 'orange',
    title: title,
    message: message,
    autoClose: duration === 0 ? false : duration,
    onClick: onClick,
    withCloseButton: duration === 0 || duration > 5000,
  })
}

export const showError = ({ id, message, title, icon, onClick }: { id?: string, message: React.ReactNode, title?: React.ReactNode, icon?: React.ReactNode, onClick?: MouseEventHandler | undefined }) => {
  const duration = getErrorDuration()
  notifications.show({
    id: id,
    color: 'red',
    title: title,
    message: message,
    icon: icon,
    autoClose: duration === 0 ? false : duration,
    withCloseButton: duration === 0 || duration > 5000,
    onClick: onClick,
  })
}

export const updateSuccess = ({ id, message, title, icon }: { id: string, message: React.ReactNode, title?: React.ReactNode, icon?: React.ReactNode }) => {
  const duration = getInfoDuration()
  notifications.update({
    id: id,
    loading: false,
    color: 'teal',
    title: title,
    message: message,
    icon: icon,
    autoClose: duration === 0 ? false : duration,
    withCloseButton: duration === 0 || duration > 5000,
  })
}

export const updateInfo = ({ id, message, title, icon }: { id: string, message: React.ReactNode, title?: React.ReactNode, icon?: React.ReactNode }) => {
  const duration = getInfoDuration()
  notifications.update({
    id: id,
    loading: false,
    color: undefined,
    title: title,
    message: message,
    icon: icon,
    autoClose: duration === 0 ? false : duration,
    withCloseButton: duration === 0 || duration > 5000,
  })
}

export const updateError = ({ id, message, title, icon, onClick }: { id: string, message: React.ReactNode, title?: React.ReactNode, icon?: React.ReactNode, onClick?: MouseEventHandler | undefined }) => {
  const duration = getErrorDuration()
  notifications.update({
    id: id,
    loading: false,
    color: 'red',
    title: title,
    message: message,
    icon: icon,
    autoClose: duration === 0 ? false : duration,
    withCloseButton: duration === 0 || duration > 5000,
    onClick: onClick,
  })
}
