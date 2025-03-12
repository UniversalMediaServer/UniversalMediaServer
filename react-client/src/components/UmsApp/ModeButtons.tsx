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
import { Box, Button, useMatches } from '@mantine/core'
import { IconPlayerPlay, IconSettings } from '@tabler/icons-react'
import { useNavigate } from 'react-router-dom'

import { havePermission, Permissions } from '../../services/accounts-service'
import { I18nInterface } from '../../services/i18n-service'
import { SessionInterface } from '../../services/session-service'

export default function ModeButtons({ i18n, session }: { i18n: I18nInterface, session: SessionInterface }) {
  const navigate = useNavigate()
  const canPlay = havePermission(session, Permissions.web_player_browse)
  const canManage = !session.player && havePermission(session, Permissions.settings_view)
  const inPlayer = location.pathname.startsWith('/player')
  const isNeeded = !session.player && canPlay && canManage
  const playerSection = useMatches({
    sm: <IconPlayerPlay size={16} />,
  })
  const manageSection = useMatches({
    sm: <IconSettings size={16} />,
  })

  return isNeeded
    ? !session.player
        ? (
            <Button.Group>
              <Button
                size="compact-md"
                leftSection={playerSection}
                variant={!inPlayer ? 'default' : 'filled'}
                onClick={() => { navigate('/player') }}
              >
                <Box visibleFrom="sm">{i18n.get('Play')}</Box>
                <Box hiddenFrom="sm"><IconPlayerPlay size={16} /></Box>
              </Button>
              <Button
                size="compact-md"
                rightSection={manageSection}
                variant={inPlayer ? 'default' : 'filled'}
                onClick={() => { navigate('/') }}
              >
                <Box visibleFrom="sm">{i18n.get('Manage')}</Box>
                <Box hiddenFrom="sm"><IconSettings size={16} /></Box>
              </Button>
            </Button.Group>
          )
        : (
            <Button.Group>
              <Button
                size="compact-md"
                variant={!inPlayer ? 'default' : 'filled'}
                onClick={() => { navigate('/player') }}
              >
                <IconPlayerPlay size={16} />
              </Button>
              <Button
                size="compact-md"
                variant={inPlayer ? 'default' : 'filled'}
                onClick={() => { navigate('/customize') }}
              >
                <IconSettings size={16} />
              </Button>
            </Button.Group>
          )
    : undefined
}
