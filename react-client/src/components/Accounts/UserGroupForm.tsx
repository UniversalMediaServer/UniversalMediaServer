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
import { Button, Divider, Group, Select } from '@mantine/core'
import { useForm } from '@mantine/form'
import { useEffect, useRef } from 'react'
import { I18nInterface, ValueLabelData } from '../../services/i18n-service'
import { UmsUser } from '../../services/session-service'

export default function UserGroupForm({
  i18n,
  user,
  canManageGroups,
  groupSelectionDatas,
  postAccountAction,
}:
{
  i18n: I18nInterface
  canManageGroups: boolean
  groupSelectionDatas: ValueLabelData[]
  user: UmsUser
  postAccountAction: (data: Record<string, unknown>, title: string, message: string, successmessage: string, errormessage: string) => void
}) {
  const userGroupForm = useForm({ initialValues: { id: user.id, groupId: user.groupId.toString() } })
  const selectRef = useRef<HTMLInputElement>(null)
  const scrollPositionRef = useRef<number>(0)

  const handleUserGroupSubmit = (values: typeof userGroupForm.values) => {
    const data = { operation: 'modifyuser', userid: user.id, groupid: values.groupId }
    postAccountAction(data, i18n.get('UserGroupChange'), i18n.get('UserGroupChanging'), i18n.get('UserGroupChanged'), i18n.get('UserGroupNotChanged'))
  }

  // Prevent mobile scroll jump on focus
  useEffect(() => {
    const selectElement = selectRef.current
    if (!selectElement) return

    const handleFocus = (e: FocusEvent) => {
      // Save current scroll position
      scrollPositionRef.current = window.scrollY
      
      // Prevent default scroll behavior
      e.preventDefault()
      
      // Restore scroll position after browser tries to scroll
      requestAnimationFrame(() => {
        window.scrollTo(0, scrollPositionRef.current)
      })
    }

    const handleClick = (e: MouseEvent) => {
      // Save scroll position on click
      scrollPositionRef.current = window.scrollY
      
      // Ensure dropdown stays in view
      requestAnimationFrame(() => {
        const rect = selectElement.getBoundingClientRect()
        const isOutOfView = rect.top < 0 || rect.bottom > window.innerHeight
        
        if (isOutOfView) {
          selectElement.scrollIntoView({ 
            behavior: 'smooth', 
            block: 'center',
            inline: 'nearest' 
          })
        }
      })
    }

    selectElement.addEventListener('focus', handleFocus as EventListener)
    selectElement.addEventListener('click', handleClick)

    return () => {
      selectElement.removeEventListener('focus', handleFocus as EventListener)
      selectElement.removeEventListener('click', handleClick)
    }
  }, [])

  return (
    <form onSubmit={userGroupForm.onSubmit(handleUserGroupSubmit)}>
      <Divider my="sm" label={i18n.get('Group')} fz="md" c="var(--mantine-color-text)" />
      <Select
        ref={selectRef}
        label={i18n.get('Group')}
        name="groupId"
        disabled={!canManageGroups}
        data={groupSelectionDatas}
        {...userGroupForm.getInputProps('groupId')}
        styles={{
          input: {
            fontSize: '16px', // Prevents iOS zoom on focus
          },
          dropdown: {
            position: 'absolute',
            zIndex: 9999,
          }
        }}
        dropdownPosition="bottom"
        withinPortal={true}
      />
      {canManageGroups && userGroupForm.isDirty() && (
        <Group justify="flex-end" mt="md">
          <Button type="submit">
            {i18n.get('Apply')}
          </Button>
        </Group>
      )}
    </form>
  )
}
