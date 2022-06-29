import { ActionIcon, Menu } from '@mantine/core';
import React, { useContext } from 'react';
import { Language } from 'tabler-icons-react';
import ReactCountryFlag from "react-country-flag";

import I18nContext, { LanguageValue } from '../../contexts/i18n-context';

function LanguagesMenu() {
  const i18n = useContext(I18nContext);
  const LanguageMenu = (language : LanguageValue) => {
    const padding = i18n.rtl ? '0 0 0.17em 0.3em' : '0 0.3em 0.17em 0';
    return (
      <Menu.Item
        onClick={() => { setLanguage(language.id); }}
      >
        <ReactCountryFlag countryCode={language.country} style={{fontSize: '1.5em',padding: padding}}/>{language.name}
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
      <Menu.Label>{i18n.get['GeneralTab.14']}</Menu.Label>
      {languagesMenus}
    </Menu>
  );
}
export default LanguagesMenu;