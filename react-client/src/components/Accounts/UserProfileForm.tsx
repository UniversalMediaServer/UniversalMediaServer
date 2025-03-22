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
import { Avatar, Button, Checkbox, Divider, Group, HoverCard, Input, PinInput, Text, TextInput, Tooltip } from '@mantine/core'
import { Dropzone, FileWithPath, IMAGE_MIME_TYPE } from '@mantine/dropzone'
import { useForm } from '@mantine/form'
import { IconPhotoUp, IconPhotoX, IconUser } from '@tabler/icons-react'
import { useState } from 'react'

import { I18nInterface } from '../../services/i18n-service'
import { UmsUser } from '../../services/session-service'
import { allowHtml } from '../../utils'

export default function UserProfileForm({
  i18n,
  user,
  postAccountAction,
}:
{
  i18n: I18nInterface
  user: UmsUser
  postAccountAction: (data: any, title: string, message: string, successmessage: string, errormessage: string) => void
}) {
  const [avatar, setAvatar] = useState<string>(user.avatar ? user.avatar : '')
  const userProfileForm = useForm({ initialValues: { id: user.id, displayName: user.displayName, avatar: user.avatar ? user.avatar : '', pinCode: user.pinCode ? user.pinCode : '', libraryHidden: user.libraryHidden } })
  const handleUserProfileSubmit = (values: typeof userProfileForm.values) => {
    const data = { operation: 'modifyuser', userid: user.id, name: values.displayName } as any
    if (userProfileForm.isDirty('displayName')) data.name = values.displayName
    if (userProfileForm.isDirty('avatar')) data.avatar = values.avatar
    if (userProfileForm.isDirty('pinCode')) data.pincode = values.pinCode
    if (userProfileForm.isDirty('libraryHidden')) data.library_hidden = values.libraryHidden
    postAccountAction(data, i18n.get('UserProfileUpdate'), i18n.get('UserProfileUpdating'), i18n.get('UserProfileUpdated'), i18n.get('UserProfileNotUpdated'))
  }
  return (
    <form onSubmit={userProfileForm.onSubmit(handleUserProfileSubmit)}>
      <Divider my="sm" label={i18n.get('Profile')} fz="md" c="var(--mantine-color-text)" />
      <TextInput
        label={i18n.get('DisplayName')}
        name="displayName"
        {...userProfileForm.getInputProps('displayName')}
      />
      <Input.Wrapper label={i18n.get('Avatar')}>
        <Dropzone
          name="avatar"
          maxSize={2 * 1024 ** 2}
          accept={IMAGE_MIME_TYPE}
          multiple={false}
          styles={{ inner: { pointerEvents: 'all' } }}
          onDrop={(files: FileWithPath[]) => {
            const reader = new FileReader()
            reader.onload = () => {
              const result = reader.result as string
              if (result) {
                setAvatar(result)
                userProfileForm.setFieldValue('avatar', result)
              }
            }
            if (files[0]) {
              reader.readAsDataURL(files[0])
            }
          }}
        >
          <Group justify="center">
            <Dropzone.Accept>
              <IconPhotoUp />
            </Dropzone.Accept>
            <Dropzone.Reject>
              <IconPhotoX />
            </Dropzone.Reject>
            <Dropzone.Idle>
              <div onClick={(e) => { e.stopPropagation() }}>
                <HoverCard disabled={avatar === ''}>
                  <HoverCard.Target>
                    <Avatar radius="xl" size="lg" src={avatar !== '' ? avatar : null}>
                      {avatar === '' && <IconUser size={24} />}
                    </Avatar>
                  </HoverCard.Target>
                  <HoverCard.Dropdown>
                    <Button
                      onClick={() => {
                        const newavatar = (user.avatar && avatar !== user.avatar) ? user.avatar : ''
                        setAvatar(newavatar)
                        userProfileForm.setFieldValue('avatar', newavatar)
                      }}
                    >
                      Delete
                    </Button>
                  </HoverCard.Dropdown>
                </HoverCard>
              </div>
            </Dropzone.Idle>
            <div>
              <Text inline>
                Drag image here or click to select file
              </Text>
              <Text size="sm" c="dimmed" inline mt={7}>
                File should not exceed 2mb
              </Text>
            </div>
          </Group>
        </Dropzone>
      </Input.Wrapper>
      <Input.Wrapper label={i18n.get('PinCode')}>
        <PinInput
          name="pincode"
          type="number"
          oneTimeCode
          {...userProfileForm.getInputProps('pinCode')}
        />
      </Input.Wrapper>
      <Tooltip label={allowHtml(i18n.get('HideUserChoiceLibrary'))}>
        <Checkbox
          mt="xl"
          label={i18n.get('HideUserLibrary')}
          {...userProfileForm.getInputProps('library_hidden', { type: 'checkbox' })}
        />
      </Tooltip>
      {userProfileForm.isDirty() && (
        <Group justify="flex-end" mt="md">
          <Button type="submit">
            {i18n.get('Apply')}
          </Button>
        </Group>
      )}
    </form>
  )
}
