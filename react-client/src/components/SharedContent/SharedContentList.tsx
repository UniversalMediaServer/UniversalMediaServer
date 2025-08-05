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
import { Stack } from '@mantine/core'
import _ from 'lodash'
import { CSSProperties } from 'react'
import { arrayMove, List } from 'react-movable'

import { I18nInterface } from '../../services/i18n-service'
import { SharedContentInterface } from '../../services/shared-service'
import SharedContentItem from './SharedContentItem'
import { UmsGroup } from '../../services/session-service'

export default function SharedContentList({
  i18n,
  sharedContents,
  setSharedContents,
  openEditModal,
  groups,
  canModify,
}: {
  i18n: I18nInterface
  sharedContents: SharedContentInterface[]
  setSharedContents: (sharedContents: SharedContentInterface[]) => void
  openEditModal: (index: number) => void
  groups: UmsGroup[]
  canModify: boolean
}) {
  const moveSharedContentItem = (oldIndex: number, newIndex: number) => {
    let sharedContentsTemp = _.cloneDeep(sharedContents)
    sharedContentsTemp = arrayMove(sharedContentsTemp, oldIndex, newIndex)
    setSharedContents(sharedContentsTemp)
  }

  return (
    canModify
    && sharedContents.length > 1
  )
    ? (
        <List
          lockVertically
          values={sharedContents}
          onChange={({ oldIndex, newIndex }) => {
            if (canModify) {
              moveSharedContentItem(oldIndex, newIndex)
            }
          }}
          renderList={
            ({ children, props }) => {
              return (
                <Stack gap="xs" {...props}>
                  {children}
                </Stack>
              )
            }
          }
          renderItem={
            ({ value, props, isDragged, isSelected }) => {
            // react-movable has a bug, hack this until it's solved
              props.style = props.style ? { ...props.style, zIndex: isSelected ? 5000 : 'auto' } as CSSProperties : {} as CSSProperties
              const usekey = 'shared' + props.key?.toString()
              props.key = undefined
              const isFirst = sharedContents.indexOf(value) === 0
              const isLast = sharedContents.indexOf(value) === sharedContents.length - 1
              return (
                <SharedContentItem
                  i18n={i18n}
                  value={value}
                  sharedContents={sharedContents}
                  setSharedContents={setSharedContents}
                  openEditModal={openEditModal}
                  groups={groups}
                  canModify={canModify}
                  usekey={usekey}
                  isDragged={isDragged}
                  isSelected={isSelected}
                  isFirst={isFirst}
                  isLast={isLast}
                  props={props}
                />
              )
            }
          }
        />
      )
    : (
        <Stack gap="xs">
          {sharedContents.map((value: SharedContentInterface, index) => {
            return (
              <SharedContentItem
                i18n={i18n}
                value={value}
                sharedContents={sharedContents}
                setSharedContents={setSharedContents}
                openEditModal={openEditModal}
                groups={groups}
                canModify={canModify}
                key={index}
              />
            )
          })}
        </Stack>
      )
}
