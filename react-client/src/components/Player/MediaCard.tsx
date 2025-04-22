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
import { Card, Center, Image, Text } from '@mantine/core'
import { KeyboardEventHandler, useRef } from 'react'

import { I18nInterface } from '../../services/i18n-service'
import { BaseMedia, getMediaIcon, PlayerInterface } from '../../services/player-service'
import { SessionInterface } from '../../services/session-service'
import { playerApiUrl } from '../../utils'

export default function MediaCard({ i18n, session, player, media, onMediaCardKeyDown }: { i18n: I18nInterface, session: SessionInterface, player: PlayerInterface, media: BaseMedia, onMediaCardKeyDown: KeyboardEventHandler<HTMLButtonElement> | undefined }) {
  const clickCount = useRef(0)
  const onInfoPress = session.playerDirectPlay && media.goal === 'show'
    ? () => {
        player.askReqId(media.id, 'show')
      }
    : undefined
  const onAskReqId = () => player.askReqId(media.id, media.goal ? (media.goal === 'show' && session.playerDirectPlay) ? 'play' : media.goal : 'browse')
  const onClick = () => {
    if (onInfoPress === undefined) {
      onAskReqId()
      return
    }
    clickCount.current++
    if (clickCount.current > 1) {
      clickCount.current = 0
      onInfoPress()
    }
    setTimeout(() => {
      if (clickCount.current === 1) {
        onAskReqId()
      }
      clickCount.current = 0
    }, 650)
  }

  const MediaImage = ({ i18n, player, media }: { i18n: I18nInterface, player: PlayerInterface, media: BaseMedia }) => {
    const icon = getMediaIcon(media, i18n, 185)
    if (icon) {
      return <Center>{icon}</Center>
    }
    const updateId = media.updateId ? '?update=' + media.updateId : ''
    return (
      <Image
        src={playerApiUrl + 'thumbnail/' + player.uuid + '/' + media.id + updateId}
        alt={media.name}
        maw="auto"
        fit="contain"
        h="100%"
      />
    )
  }

  return (
    <Card
      component="button"
      className="thumbnail-container color-bright-hover"
      shadow="sm"
      padding="lg"
      radius="md"
      ta="center"
      onClick={onClick}
      onKeyDown={onMediaCardKeyDown}
      withBorder
    >
      <Card.Section h={185}>
        <MediaImage i18n={i18n} player={player} media={media} />
      </Card.Section>
      <Card.Section h={30} px={5} pt={5}>
        <Center>
          <Text size="sm" truncate>
            {i18n.getLocalizedName(media.name)}
          </Text>
        </Center>
      </Card.Section>
    </Card>
  )
}
