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
import { CSSProperties, Title } from '@mantine/core'

import { I18nInterface } from '../../services/i18n-service'
import { BaseMedia, PlayerInterface } from '../../services/player-service'
import { SessionInterface } from '../../services/session-service'
import MediaCard from './MediaCard'

export default function MediaGrid({ i18n, session, player, mediaArray, title, fixed }: { i18n: I18nInterface, session: SessionInterface, player: PlayerInterface, mediaArray: BaseMedia[], title?: string, fixed?: boolean }) {
  const MediaCards = ({ i18n, player, mediaArray }: { i18n: I18nInterface, player: PlayerInterface, mediaArray: BaseMedia[] }) => {
    return mediaArray?.map((media) => {
      return (
        <MediaCard key={media.id} i18n={i18n} session={session} player={player} media={media} />
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
            <div className={className} style={style}><MediaCards i18n={i18n} player={player} mediaArray={mediaArray} /></div>
          </>
        )
      : <div className={className}><MediaCards i18n={i18n} player={player} mediaArray={mediaArray} /></div>
    : undefined
}
