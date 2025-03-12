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
import { ActionIcon, Group, Menu, ScrollArea, Text } from '@mantine/core'
import { IconLanguage } from '@tabler/icons-react'
import ReactCountryFlag from 'react-country-flag'

import { I18nInterface, LanguageValue } from '../../services/i18n-service'

export default function LanguagesMenu({ i18n }: { i18n: I18nInterface }) {
  const LanguageMenu = (language: LanguageValue) => {
    return (
      <Menu.Item
        onClick={() => { i18n.setLanguage(language.id) }}
        key={language.id}
      >
        <Group gap="xs">
          <ReactCountryFlag countryCode={language.country} style={{ fontSize: '1.5em' }} />
          <Text>{language.name}</Text>
          {language.name !== language.defaultname && (
            <Text>{'(' + language.defaultname + ')'}</Text>
          )}
        </Group>
      </Menu.Item>
    )
  }

  const languagesMenus = i18n.languages.map((language) => {
    return LanguageMenu(language)
  })

  return (
    <Menu>
      <Menu.Target>
        <ActionIcon variant="default" size="input-xs">
          <IconLanguage size={16} />
        </ActionIcon>
      </Menu.Target>
      <Menu.Dropdown>
        <Menu.Label>{i18n.get('Language')}</Menu.Label>
        <ScrollArea h={250}>
          {languagesMenus}
        </ScrollArea>
      </Menu.Dropdown>
    </Menu>
  )
}
