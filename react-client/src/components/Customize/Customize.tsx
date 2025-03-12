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
import { Accordion, Box } from '@mantine/core'
import { IconBoxAlignLeft, IconClockExclamation, IconDimensions } from '@tabler/icons-react'
import { useEffect } from 'react'

import { I18nInterface } from '../../services/i18n-service'
import { MainInterface } from '../../services/main-service'
import { SessionInterface } from '../../services/session-service'
import NotificationDuration from './NotificationDuration'
import { getErrorDuration, getInfoDuration, getWarningDuration, setErrorDuration, setInfoDuration, setWarningDuration, showError, showInfo, showWarning } from '../../utils/notifications'
import NavbarWidth from './NavbarWidth'
import ScrollbarSize from './ScrollbarSize'

const Customize = ({ i18n, main, session }: { i18n: I18nInterface, main: MainInterface, session: SessionInterface }) => {
  useEffect(() => {
    session.setDocumentTitle('Customize')
    session.stopSse()
    session.stopPlayerSse()
    main.setNavbarItem(i18n, session, Customize.name)
  }, [])

  return (
    <Box style={{ maxWidth: 1024 }} mx="auto">
      <Accordion defaultValue="NavbarWidth">
        <Accordion.Item value="NotificationDuration">
          <Accordion.Control icon={<IconClockExclamation />}>Notification duration</Accordion.Control>
          <Accordion.Panel>
            <NotificationDuration
              i18n={i18n}
              title="Info auto close"
              duration={getInfoDuration()}
              setDuration={setInfoDuration}
              test={showInfo}
            />
            <NotificationDuration
              i18n={i18n}
              title="Warning auto close"
              duration={getWarningDuration()}
              setDuration={setWarningDuration}
              test={showWarning}
            />
            <NotificationDuration
              i18n={i18n}
              title="Error auto close"
              duration={getErrorDuration()}
              setDuration={setErrorDuration}
              test={showError}
              color="red"
            />
          </Accordion.Panel>
        </Accordion.Item>
        <Accordion.Item value="ScrollbarSize">
          <Accordion.Control icon={<IconDimensions />}>Scrollbar size</Accordion.Control>
          <Accordion.Panel>
            <ScrollbarSize />
          </Accordion.Panel>
        </Accordion.Item>
        <Accordion.Item value="NavbarWidth">
          <Accordion.Control icon={<IconBoxAlignLeft />}>Navbar width</Accordion.Control>
          <Accordion.Panel>
            <NavbarWidth
              title="Large viewport"
              visibleFrom="lg"
              storageKey="mantine-navbar-width-lg"
              defaultValue={400}
            />
            <NavbarWidth
              title="Medium viewport"
              visibleFrom="md"
              hiddenFrom="lg"
              storageKey="mantine-navbar-width-md"
              defaultValue={300}
            />
            <NavbarWidth
              title="Small viewport"
              visibleFrom="sm"
              hiddenFrom="md"
              storageKey="mantine-navbar-width-sm"
              defaultValue={250}
            />
          </Accordion.Panel>
        </Accordion.Item>
      </Accordion>
    </Box>
  )
}

export default Customize
