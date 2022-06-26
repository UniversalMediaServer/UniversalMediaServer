import { Menu, ActionIcon } from '@mantine/core';
import React, { useContext } from 'react';
import { Language } from 'tabler-icons-react';
import ReactCountryFlag from "react-country-flag";

import I18nContext, { LanguageValue } from '../../contexts/i18n-context';

function LanguagesMenu() {
  const i18n = useContext(I18nContext);
  const LanguageMenu = (language : LanguageValue) => { return (
        <Menu.Item
          onClick={() => { setLanguage(language.id); }}
        >
          <ReactCountryFlag countryCode={language.country} /> {language.name}
        </Menu.Item>
    );
  }

  const setLanguage = (language : string) => {
    i18n.updateLanguage(language);
  };

  const languagesMenus = i18n.languages.map((language) => {
    return LanguageMenu(language);
  });

  return (
    <Menu
      control={
        <ActionIcon variant="default" size={30}>
          <Language size={16} />
        </ActionIcon>
      }
    >
      <Menu.Label>{i18n.get['LanguageSelection.Language']}</Menu.Label>
      {languagesMenus}
    </Menu>
  );
}
export default LanguagesMenu;