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
import { ActionIcon, Card, Center, Image, Text } from '@mantine/core'
import { IconInfoSmall } from '@tabler/icons-react'

import { I18nInterface } from '../../services/i18n-service'
import { PlayerEventInterface } from '../../services/player-server-event-service'
import { BaseMedia, getMediaIcon } from '../../services/player-service'
import { SessionInterface } from '../../services/session-service'
import { playerApiUrl } from '../../utils'

export default function MediaCard({ i18n, session, sse, media }: { i18n: I18nInterface, session: SessionInterface, sse: PlayerEventInterface, media: BaseMedia }) {
  const onInfoPress = session.playerDirectPlay && media.goal === 'show'
    ? (e: any) => {
        sse.askReqId(media.id, 'show')
        e.stopPropagation()
      }
    : undefined
  const onClick = () => sse.askReqId(media.id, media.goal ? (media.goal === 'show' && session.playerDirectPlay) ? 'play' : media.goal : 'browse')

  const MediaImage = ({ i18n, sse, media }: { i18n: I18nInterface, sse: PlayerEventInterface, media: BaseMedia }) => {
    const icon = getMediaIcon(media, i18n, 185)
    if (icon) {
      return <Center>{icon}</Center>
    }
    const updateId = media.updateId ? '?update=' + media.updateId : ''
    return (
      <Image
        src={playerApiUrl + 'thumbnail/' + sse.uuid + '/' + media.id + updateId}
        alt={media.name}
        maw="auto"
        fit="contain"
        h="100%"
      />
    )
  }

  return (
    <Card
      className="thumbnail-container color-bright-hover"
      shadow="sm"
      padding="lg"
      radius="md"
      ta="center"
      onClick={onClick}
      withBorder
    >
      <Card.Section h={185}>
        <MediaImage i18n={i18n} sse={sse} media={media} />
      </Card.Section>
      {onInfoPress && (
        <Card.Section h={0} mt={-185} mb={185} ms={50} ta="end">
          <ActionIcon
            variant="default"
            radius="md"
            color="grey"
            size="xl"
            className="color-bright-hover"
            onClick={onInfoPress}
          >
            <IconInfoSmall size={70} />
          </ActionIcon>
        </Card.Section>
      )}
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
