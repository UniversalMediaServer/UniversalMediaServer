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
import { Card, Grid, Image, Text } from '@mantine/core'
import { ReactNode } from 'react'

import { I18nInterface } from '../../services/i18n-service'
import { BaseMedia, PlayerInterface } from '../../services/player-service'
import { playerApiUrl } from '../../utils'

export default function MediaShowPanel({ i18n, player, media, children }: { i18n: I18nInterface, player: PlayerInterface, media: BaseMedia, children?: ReactNode }) {
  const updateId = media.updateId ? '?update=' + media.updateId : ''
  return (
    <Grid mb="md">
      <Grid.Col span={12}>
        <Grid columns={20} justify="center">
          <Grid.Col span={{ base: 0, xs: 6 }}>
            <Image style={{ maxHeight: 500 }} radius="md" fit="contain" src={playerApiUrl + 'thumbnail/' + player.uuid + '/' + media.id + updateId} />
          </Grid.Col>
          <Grid.Col span={{ base: 20, xs: 12 }}>
            <Card shadow="sm" p="lg" radius="md" bg="transparentBg">
              <Text pb="xs">{i18n.getLocalizedName(media.name)}</Text>
              {children}
            </Card>
          </Grid.Col>
        </Grid>
      </Grid.Col>
    </Grid>
  )
}
