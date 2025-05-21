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
import { CSSProperties, Title, useDirection } from '@mantine/core'
import { useRef } from 'react'

import { I18nInterface } from '../../services/i18n-service'
import { BaseMedia, PlayerInterface } from '../../services/player-service'
import { SessionInterface } from '../../services/session-service'
import MediaCard from './MediaCard'

export default function MediaGrid({ i18n, session, player, mediaArray, title, fixed }: { i18n: I18nInterface, session: SessionInterface, player: PlayerInterface, mediaArray: BaseMedia[], title?: string, fixed?: boolean }) {
  const { dir } = useDirection()
  const ref = useRef<HTMLDivElement>(null)

  function getPreviousIndex(current: number, max: number) {
    return (current > 0) ? current - 1 : max
  }

  function getNextIndex(current: number, max: number) {
    return (current < max) ? current + 1 : 0
  }

  function getPreviousRowIndex(current: number, max: number, columns: number) {
    const index = current - columns
    if (index < 0) {
      const col = current % columns
      const row = Math.floor(max / columns)
      const reverseIndex = (columns * row) + col
      return reverseIndex > max ? max : reverseIndex
    }
    return index
  }

  function getNextRowIndex(current: number, max: number, columns: number) {
    const index = current + columns
    if (index > max) {
      return current % columns
    }
    return index
  }

  function getColumnCount(elements: HTMLButtonElement[]) {
    if (elements.length === 0) {
      return 0
    }
    const firstTop = elements[0].getBoundingClientRect().top
    const elemIndex = elements.findLastIndex((element) => {
      return element.getBoundingClientRect().top == firstTop
    })
    return elemIndex + 1
  }

  function setFocusOn(element: HTMLButtonElement) {
    element.focus()
    element.scrollIntoView({ block: 'center', inline: 'center' })
  }

  function getCurrentElements() {
    return Array.from(
      ref.current?.querySelectorAll<HTMLButtonElement>('.thumbnail-container') || [],
    ).filter(node => !node.disabled)
  }

  function setFocusOnHorizontal(event: React.KeyboardEvent<HTMLButtonElement>, up: boolean) {
    event.stopPropagation()
    event.preventDefault()
    const elements = getCurrentElements()
    if (elements.length < 2) {
      return
    }
    const current = elements.findIndex(el => event.currentTarget === el)
    const rtl = dir === 'rtl'
    const next = up === rtl
    const nextIndex = next ? getNextIndex(current, elements.length - 1) : getPreviousIndex(current, elements.length - 1)
    setFocusOn(elements[nextIndex])
  }

  function setFocusOnNext(event: React.KeyboardEvent<HTMLButtonElement>) {
    setFocusOnHorizontal(event, true)
  }

  function setFocusOnPrevious(event: React.KeyboardEvent<HTMLButtonElement>) {
    setFocusOnHorizontal(event, false)
  }

  function setFocusOnVertical(event: React.KeyboardEvent<HTMLButtonElement>, up: boolean) {
    event.stopPropagation()
    event.preventDefault()
    const elements = getCurrentElements()
    if (elements.length < 2) {
      return
    }
    const columnCount = getColumnCount(elements)
    if (columnCount < 2) {
      return
    }
    const current = elements.findIndex(el => event.currentTarget === el)
    const nextIndex = up ? getNextRowIndex(current, elements.length - 1, columnCount) : getPreviousRowIndex(current, elements.length - 1, columnCount)
    setFocusOn(elements[nextIndex])
  }

  function setFocusOnNextRow(event: React.KeyboardEvent<HTMLButtonElement>) {
    setFocusOnVertical(event, true)
  }

  function setFocusOnPreviousRow(event: React.KeyboardEvent<HTMLButtonElement>) {
    setFocusOnVertical(event, false)
  }

  function setFocusOnStartEnd(event: React.KeyboardEvent<HTMLButtonElement>, start: boolean) {
    event.stopPropagation()
    event.preventDefault()
    const elements = getCurrentElements()
    if (elements.length < 2) {
      return
    }
    setFocusOn(start ? elements[0] : elements[elements.length - 1])
  }

  function onMediaCardKeyDown(event: React.KeyboardEvent<HTMLButtonElement>) {
    switch (event.key) {
      case 'ArrowRight': {
        setFocusOnPrevious(event)
        break
      }
      case 'ArrowLeft': {
        setFocusOnNext(event)
        break
      }
      case 'ArrowUp': {
        setFocusOnPreviousRow(event)
        break
      }
      case 'ArrowDown': {
        setFocusOnNextRow(event)
        break
      }
      case 'Home': {
        setFocusOnStartEnd(event, true)
        break
      }
      case 'End': {
        setFocusOnStartEnd(event, false)
        break
      }
    }
  }

  const MediaCards = ({ i18n, player, mediaArray }: { i18n: I18nInterface, player: PlayerInterface, mediaArray: BaseMedia[] }) => {
    return mediaArray?.map((media) => {
      return (
        <MediaCard key={media.id} i18n={i18n} session={session} player={player} media={media} onMediaCardKeyDown={onMediaCardKeyDown} />
      )
    })
  }
  const style = fixed ? { height: '240px', overflowY: 'hidden' } as CSSProperties : {}
  const className = fixed ? 'media-grid front-page-grid' : 'media-grid'

  return (mediaArray && mediaArray.length)
    ? title
      ? (
          <>
            <Title mb="md" size="h4" fw={400}>{i18n.get(title)}</Title>
            <div ref={ref} className={className} style={style}><MediaCards i18n={i18n} player={player} mediaArray={mediaArray} /></div>
          </>
        )
      : <div ref={ref} className={className}><MediaCards i18n={i18n} player={player} mediaArray={mediaArray} /></div>
    : undefined
}
