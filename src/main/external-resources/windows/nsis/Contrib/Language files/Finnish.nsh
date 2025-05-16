;Compatible with Modern UI 1.86
;Language: Finnish (1035)
;By Eclipser (Jonne Lehtinen) <Eclipser at pilvikaupunki dot com>
;Updated by Puuhis (puuhis@puuhis.net)
;Updated 11/08 by WTLib Team
;Updated 01/24 by olavinto (Oskari Lavinto) (l18npub@olavinto.simplelogin.com)

!insertmacro LANGFILE "Finnish" = "Suomi" =

!ifdef MUI_WELCOMEPAGE
  ${LangFileString} MUI_TEXT_WELCOME_INFO_TITLE "Tervetuloa $(^NameDA) -asennukseen"
  ${LangFileString} MUI_TEXT_WELCOME_INFO_TEXT "Näet tarpeellisia ohjeita $(^NameDA) -asennuksen edistyessä.$\r$\n$\r$\nKaikki muut sovellukset kannattaa sulkea ennen asennuksen aloitusta, jotta asennus voi päivittää järjestelmätiedostoja käynnistämättä tietokonetta uudelleen.$\r$\n$\r$\n$_CLICK"
!endif

!ifdef MUI_UNWELCOMEPAGE
  ${LangFileString} MUI_UNTEXT_WELCOME_INFO_TITLE "Tervetuloa $(^NameDA) -asennuksen poistoon"
  ${LangFileString} MUI_UNTEXT_WELCOME_INFO_TEXT "Saat tarvittavia ohjeita sitä mukaa kuin $(^NameDA) -asennuksen poisto edistyy.$\r$\n$\r$\nVarmista ennen asennuksen poiston aloitusta, ettei $(^NameDA) ole käynnissä.$\r$\n$\r$\n$_CLICK"
!endif

!ifdef MUI_LICENSEPAGE
  ${LangFileString} MUI_TEXT_LICENSE_TITLE "Käyttöoikeussopimus"
  ${LangFileString} MUI_TEXT_LICENSE_SUBTITLE "Lue käyttöehdot huolellisesti ennen $(^NameDA) -asennusta."
  ${LangFileString} MUI_INNERTEXT_LICENSE_BOTTOM "Jos hyväksyt ehdot, valitse Hyväksyn. Asennus edellyttää käyttöehtojen hyväksyntää."
  ${LangFileString} MUI_INNERTEXT_LICENSE_BOTTOM_CHECKBOX "Jos hyväksyt ehdot, laita rasti ruutuun. Asennus edellyttää käyttöehtojen hyväksyntää. $_CLICK"
  ${LangFileString} MUI_INNERTEXT_LICENSE_BOTTOM_RADIOBUTTONS "Jos hyväksyt ehdot, valitse alta ensimmäinen valinta. Asennus edellyttää käyttöehtojen hyväksyntää. $_CLICK"
!endif

!ifdef MUI_UNLICENSEPAGE
  ${LangFileString} MUI_UNTEXT_LICENSE_TITLE "Käyttöoikeussopimus"
  ${LangFileString} MUI_UNTEXT_LICENSE_SUBTITLE "Lue käyttöehdot huolellisesti ennen $(^NameDA) -asennuksen poistoa."
  ${LangFileString} MUI_UNINNERTEXT_LICENSE_BOTTOM "Jos hyväksyt ehdot, valitse Hyväksyn. Asennuksen poisto edellyttää käyttöehtojen hyväksyntää."
  ${LangFileString} MUI_UNINNERTEXT_LICENSE_BOTTOM_CHECKBOX "Jos hyväksyt ehdot, laita rasti ruutuun. Asennuksen poisto edellyttää käyttöehtojen hyväksyntää. $_CLICK"
  ${LangFileString} MUI_UNINNERTEXT_LICENSE_BOTTOM_RADIOBUTTONS "Jos hyväksyt ehdot, valitse alta ensimmäinen valinta. Asennuksen poisto edellyttää käyttöehtojen hyväksyntää. $_CLICK"
!endif

!ifdef MUI_LICENSEPAGE | MUI_UNLICENSEPAGE
  ${LangFileString} MUI_INNERTEXT_LICENSE_TOP "Vieritä ja näytä sopimusta lisää painamalla Page Down -näppäintä."
!endif

!ifdef MUI_COMPONENTSPAGE
  ${LangFileString} MUI_TEXT_COMPONENTS_TITLE "Valitse komponentit"
  ${LangFileString} MUI_TEXT_COMPONENTS_SUBTITLE "Valitse asennettavat $(^NameDA) -ominaisuudet."
!endif

!ifdef MUI_UNCOMPONENTSPAGE
  ${LangFileString} MUI_UNTEXT_COMPONENTS_TITLE "Valitse komponentit"
  ${LangFileString} MUI_UNTEXT_COMPONENTS_SUBTITLE "Valitse $(^NameDA) -ominaisuudet, jotka haluat poistaa."
!endif

!ifdef MUI_COMPONENTSPAGE | MUI_UNCOMPONENTSPAGE
  ${LangFileString} MUI_INNERTEXT_COMPONENTS_DESCRIPTION_TITLE "Kuvaus"
  !ifndef NSIS_CONFIG_COMPONENTPAGE_ALTERNATIVE
    ${LangFileString} MUI_INNERTEXT_COMPONENTS_DESCRIPTION_INFO "Näytä komponentin kuvaus osoittamalla sitä hiirellä."
  !else
    ${LangFileString} MUI_INNERTEXT_COMPONENTS_DESCRIPTION_INFO "Näytä komponentin kuvaus valitsemalla se."
  !endif
!endif

!ifdef MUI_DIRECTORYPAGE
  ${LangFileString} MUI_TEXT_DIRECTORY_TITLE "Valitse asennuskansio"
  ${LangFileString} MUI_TEXT_DIRECTORY_SUBTITLE "Valitse kansio, johon $(^NameDA) asennetaan."
!endif

!ifdef MUI_UNDIRECTORYPAGE
  ${LangFileString} MUI_UNTEXT_DIRECTORY_TITLE "Valitse poistokansio"
  ${LangFileString} MUI_UNTEXT_DIRECTORY_SUBTITLE "Valitse kansio, josta $(^NameDA) poistetaan."
!endif

!ifdef MUI_INSTFILESPAGE
  ${LangFileString} MUI_TEXT_INSTALLING_TITLE "Asennetaan"
  ${LangFileString} MUI_TEXT_INSTALLING_SUBTITLE "Odota... $(^NameDA) asennetaan..."
  ${LangFileString} MUI_TEXT_FINISH_TITLE "Asennus on valmis"
  ${LangFileString} MUI_TEXT_FINISH_SUBTITLE "Asennus onnistui."
  ${LangFileString} MUI_TEXT_ABORT_TITLE "Asennus keskeytettiin"
  ${LangFileString} MUI_TEXT_ABORT_SUBTITLE "Asennus epäonnistui."
!endif

!ifdef MUI_UNINSTFILESPAGE
  ${LangFileString} MUI_UNTEXT_UNINSTALLING_TITLE "Asennusta poistetaan"
  ${LangFileString} MUI_UNTEXT_UNINSTALLING_SUBTITLE "Odota... $(^NameDA) poistetaan."
  ${LangFileString} MUI_UNTEXT_FINISH_TITLE "Asennuksen poisto on valmis"
  ${LangFileString} MUI_UNTEXT_FINISH_SUBTITLE "Asennuksen poisto onnistui."
  ${LangFileString} MUI_UNTEXT_ABORT_TITLE "Asennuksen poisto keskeytettiin"
  ${LangFileString} MUI_UNTEXT_ABORT_SUBTITLE "Asennuksen poisto epäonnistui."
!endif

!ifdef MUI_FINISHPAGE
  ${LangFileString} MUI_TEXT_FINISH_INFO_TITLE "$(^NameDA) on asennettu"
  ${LangFileString} MUI_TEXT_FINISH_INFO_TEXT "$(^NameDA) on asennettu tietokoneelle.$\r$\n$\r$\nSulje asentaja valitsemalla Valmis."
  ${LangFileString} MUI_TEXT_FINISH_INFO_REBOOT "$(^NameDA) -asennuksen viimeisteleminen edellyttää tietokoneen uudelleenkäynnistämistä. Haluatko käynnistää tietokoneen uudelleen nyt?"
!endif

!ifdef MUI_UNFINISHPAGE
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_TITLE "$(^NameDA) on poistettu"
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_TEXT "$(^NameDA) on poistettu tietokoneelta.$\r$\n$\r$\nSulje asentaja valitsemalla Lopeta."
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_REBOOT "$(^NameDA) -asennuksen poiston viimeisteleminen edellyttää tietokoneen uudelleenkäynnistystä. Haluatko käynnistää tietokoneen uudelleen nyt?"
!endif

!ifdef MUI_FINISHPAGE | MUI_UNFINISHPAGE
  ${LangFileString} MUI_TEXT_FINISH_REBOOTNOW "Käynnistä uudelleen nyt"
  ${LangFileString} MUI_TEXT_FINISH_REBOOTLATER "Käynnistän uudelleen myöhemmin"
  ${LangFileString} MUI_TEXT_FINISH_RUN "Käynnistä $(^NameDA)"
  ${LangFileString} MUI_TEXT_FINISH_SHOWREADME "Näytä Lueminut-tiedosto"
  ${LangFileString} MUI_BUTTONTEXT_FINISH "&Valmis"  
!endif

!ifdef MUI_STARTMENUPAGE
  ${LangFileString} MUI_TEXT_STARTMENU_TITLE "Valitse Käynnistä-valikon kansio"
  ${LangFileString} MUI_TEXT_STARTMENU_SUBTITLE "Valitse Käynnistä-valikon kansio, johon pikakuvakkeet asennetaan."
  ${LangFileString} MUI_INNERTEXT_STARTMENU_TOP "Valitse Käynnistä-valikon kansio, johon pikakuvakkeet asennetaan. Voit luoda myös uuden kansion kirjoittamalla sen nimen."
  ${LangFileString} MUI_INNERTEXT_STARTMENU_CHECKBOX "Älä luo pikakuvakkeita."
!endif

!ifdef MUI_UNCONFIRMPAGE
  ${LangFileString} MUI_UNTEXT_CONFIRM_TITLE "Poista $(^NameDA)"
  ${LangFileString} MUI_UNTEXT_CONFIRM_SUBTITLE "Poista $(^NameDA) tietokoneesta."
!endif

!ifdef MUI_ABORTWARNING
  ${LangFileString} MUI_TEXT_ABORTWARNING "Haluatko varmasti keskeyttää $(^Name) -asennuksen?"
!endif

!ifdef MUI_UNABORTWARNING
  ${LangFileString} MUI_UNTEXT_ABORTWARNING "Haluatko varmasti keskeyttää $(^Name) -asennuksen poiston?"
!endif
