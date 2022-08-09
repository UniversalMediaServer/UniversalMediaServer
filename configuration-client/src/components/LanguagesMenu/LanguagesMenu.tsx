import { ActionIcon, Group, Menu, ScrollArea, Text } from '@mantine/core';
import React, { useContext } from 'react';
import ReactCountryFlag from 'react-country-flag';
import { Language } from 'tabler-icons-react';

import I18nContext, { LanguageValue } from '../../contexts/i18n-context';

function LanguagesMenu() {
  const i18n = useContext(I18nContext);
  const LanguageMenu = (language: LanguageValue) => {
    return (
      <Menu.Item
        onClick={() => { i18n.setLanguage(language.id); }}
        key={language.id}
      >
        <Group spacing='xs'>
          <ReactCountryFlag countryCode={language.country} style={{fontSize: '1.5em'}}/>
          <Text>{language.name}</Text>
          {language.name!==language.defaultname && (
            <Text>{'('+language.defaultname+')'}</Text>
          )}
        </Group>
      </Menu.Item>
    );
  }

  const languagesMenus = i18n.languages.map((language) => {
    return LanguageMenu(language);
  });

  return (
    <Menu>
      <Menu.Target>
         <ActionIcon variant="default" size={30}>
          <Language size={16} />
        </ActionIcon>
      </Menu.Target>
      <Menu.Dropdown>
        <Menu.Label>{i18n.get['Language']}</Menu.Label>
        <ScrollArea style={{ height: 250 }}>
          {languagesMenus}
        </ScrollArea>
      </Menu.Dropdown>
    </Menu>
  );
}
export default LanguagesMenu;