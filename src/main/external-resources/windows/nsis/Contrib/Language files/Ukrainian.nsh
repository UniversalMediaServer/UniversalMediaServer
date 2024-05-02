;Language: Ukrainian (1058)
;By Yuri Holubow, Nash-Soft.com
;Corrections by Osidach Vitaly (Vit_Os2) and others

!insertmacro LANGFILE "Ukrainian" = "Українська" "Ukrayins'ka"

!ifdef MUI_WELCOMEPAGE
  ${LangFileString} MUI_TEXT_WELCOME_INFO_TITLE "Ласкаво просимо до встановлення $(^NameDA)"
  ${LangFileString} MUI_TEXT_WELCOME_INFO_TEXT "Ця програма допоможе вам встановити $(^NameDA).$\r$\n$\r$\nРадимо закрити всі інші програми, перш ніж почати встановлення. Завдяки цьому будуть оновлені відповідні системні файли без потреби перезапускати комп’ютер.$\r$\n$\r$\n$_CLICK"
!endif

!ifdef MUI_UNWELCOMEPAGE
  ${LangFileString} MUI_UNTEXT_WELCOME_INFO_TITLE "Ласкаво просимо до видалення $(^NameDA)"
  ${LangFileString} MUI_UNTEXT_WELCOME_INFO_TEXT "Ця програма допоможе вам видалити $(^NameDA).$\r$\n$\r$\nПеред початком видалення обов’язково закрийте $(^NameDA).$\r$\n$\r$\n$_CLICK"
!endif

!ifdef MUI_LICENSEPAGE
  ${LangFileString} MUI_TEXT_LICENSE_TITLE "Ліцензійна угода"
  ${LangFileString} MUI_TEXT_LICENSE_SUBTITLE "Ознайомтеся з умовами ліцензійної угоди перед встановленням $(^NameDA)."
  ${LangFileString} MUI_INNERTEXT_LICENSE_BOTTOM "Якщо ви згодні з умовами угоди, натисніть кнопку «Погоджуюся», щоб продовжити. Ви маєте погодитися з угодою для встановлення $(^NameDA)."
  ${LangFileString} MUI_INNERTEXT_LICENSE_BOTTOM_CHECKBOX "Якщо ви згодні з умовами угоди, встановіть позначку нижче. Ви маєте погодитися з угодою для встановлення $(^NameDA). $_CLICK"
  ${LangFileString} MUI_INNERTEXT_LICENSE_BOTTOM_RADIOBUTTONS "Якщо ви згодні з умовами угоди, виберіть перший варіант нижче. Ви маєте погодитися з угодою для встановлення $(^NameDA). $_CLICK"
!endif

!ifdef MUI_UNLICENSEPAGE
  ${LangFileString} MUI_UNTEXT_LICENSE_TITLE "Ліцензійна угода"
  ${LangFileString} MUI_UNTEXT_LICENSE_SUBTITLE "Ознайомтеся з умовами ліцензійної угоди перед видаленням $(^NameDA)."
  ${LangFileString} MUI_UNINNERTEXT_LICENSE_BOTTOM "Якщо ви згодні з умовами угоди, натисніть «Погоджуюся» для продовження. Ви маєте погодитися з угодою для видалення $(^NameDA)."
  ${LangFileString} MUI_UNINNERTEXT_LICENSE_BOTTOM_CHECKBOX "Якщо ви згодні з умовами угоди, встановіть позначку нижче. Ви маєте погодитися з угодою для видалення $(^NameDA). $_CLICK"
  ${LangFileString} MUI_UNINNERTEXT_LICENSE_BOTTOM_RADIOBUTTONS "Якщо ви згодні з умовами угоди, виберіть перший варіант нижче. Ви маєте погодитися з угодою для видалення $(^NameDA). $_CLICK"
!endif

!ifdef MUI_LICENSEPAGE | MUI_UNLICENSEPAGE
  ${LangFileString} MUI_INNERTEXT_LICENSE_TOP "Натисніть клавішу PageDown, щоб переглянути угоду далі."
!endif

!ifdef MUI_COMPONENTSPAGE
  ${LangFileString} MUI_TEXT_COMPONENTS_TITLE "Оберіть компоненти"
  ${LangFileString} MUI_TEXT_COMPONENTS_SUBTITLE "Оберіть, які компоненти $(^NameDA) ви хочете встановити."
!endif

!ifdef MUI_UNCOMPONENTSPAGE
  ${LangFileString} MUI_UNTEXT_COMPONENTS_TITLE "Оберіть компоненти"
  ${LangFileString} MUI_UNTEXT_COMPONENTS_SUBTITLE "Оберіть, які компоненти $(^NameDA) ви хочете видалити."
!endif

!ifdef MUI_COMPONENTSPAGE | MUI_UNCOMPONENTSPAGE
  ${LangFileString} MUI_INNERTEXT_COMPONENTS_DESCRIPTION_TITLE "Опис"
  !ifndef NSIS_CONFIG_COMPONENTPAGE_ALTERNATIVE
    ${LangFileString} MUI_INNERTEXT_COMPONENTS_DESCRIPTION_INFO "Наведіть мишу на компонент, щоб побачити його опис."
  !else
    ${LangFileString} MUI_INNERTEXT_COMPONENTS_DESCRIPTION_INFO "Оберіть компонент, щоб побачити його опис."
  !endif
!endif

!ifdef MUI_DIRECTORYPAGE
  ${LangFileString} MUI_TEXT_DIRECTORY_TITLE "Оберіть теку встановлення"
  ${LangFileString} MUI_TEXT_DIRECTORY_SUBTITLE "Оберіть теку, в яку потрібно встановити $(^NameDA)."
!endif

!ifdef MUI_UNDIRECTORYPAGE
  ${LangFileString} MUI_UNTEXT_DIRECTORY_TITLE "Оберіть теку видалення"
  ${LangFileString} MUI_UNTEXT_DIRECTORY_SUBTITLE "Оберіть теку, з якої потрібно видалити $(^NameDA)."
!endif

!ifdef MUI_INSTFILESPAGE
  ${LangFileString} MUI_TEXT_INSTALLING_TITLE "Встановлення"
  ${LangFileString} MUI_TEXT_INSTALLING_SUBTITLE "Будь ласка, зачекайте поки триває встановлення $(^NameDA)."
  ${LangFileString} MUI_TEXT_FINISH_TITLE "Встановлення завершено"
  ${LangFileString} MUI_TEXT_FINISH_SUBTITLE "Встановлення успішно завершено."
  ${LangFileString} MUI_TEXT_ABORT_TITLE "Встановлення перервано"
  ${LangFileString} MUI_TEXT_ABORT_SUBTITLE "Встановлення не було завершено."
!endif

!ifdef MUI_UNINSTFILESPAGE
  ${LangFileString} MUI_UNTEXT_UNINSTALLING_TITLE "Видалення"
  ${LangFileString} MUI_UNTEXT_UNINSTALLING_SUBTITLE "Будь ласка, зачекайте поки триває видалення $(^NameDA)."
  ${LangFileString} MUI_UNTEXT_FINISH_TITLE "Видалення завершено"
  ${LangFileString} MUI_UNTEXT_FINISH_SUBTITLE "Видалення успішно завершено."
  ${LangFileString} MUI_UNTEXT_ABORT_TITLE "Видалення перервано"
  ${LangFileString} MUI_UNTEXT_ABORT_SUBTITLE "Видалення не було завершено."
!endif

!ifdef MUI_FINISHPAGE
  ${LangFileString} MUI_TEXT_FINISH_INFO_TITLE "Завершення встановлення $(^NameDA)"
  ${LangFileString} MUI_TEXT_FINISH_INFO_TEXT "$(^NameDA) встановлено на ваш комп’ютер.$\r$\n$\r$\nНатисніть «Завершити» для виходу."
  ${LangFileString} MUI_TEXT_FINISH_INFO_REBOOT "Щоб завершити встановлення $(^NameDA), потрібно перезапустити комп’ютер. Хочете перезапустити зараз?"
!endif

!ifdef MUI_UNFINISHPAGE
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_TITLE "Завершення видалення $(^NameDA)"
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_TEXT "$(^NameDA) видалено з вашого комп’ютера.$\r$\n$\r$\nНатисніть «Завершити» для виходу."
  ${LangFileString} MUI_UNTEXT_FINISH_INFO_REBOOT "Щоб завершити видалення $(^NameDA), потрібно перезапустити комп’ютер. Хочете перезапустити зараз?"
!endif

!ifdef MUI_FINISHPAGE | MUI_UNFINISHPAGE
  ${LangFileString} MUI_TEXT_FINISH_REBOOTNOW "Перезапустити"
  ${LangFileString} MUI_TEXT_FINISH_REBOOTLATER "Я хочу перезапустити власноруч згодом"
  ${LangFileString} MUI_TEXT_FINISH_RUN "&Запустити $(^NameDA)"
  ${LangFileString} MUI_TEXT_FINISH_SHOWREADME "&Показати інформацію про програму"
  ${LangFileString} MUI_BUTTONTEXT_FINISH "&Завершити"  
!endif

!ifdef MUI_STARTMENUPAGE
  ${LangFileString} MUI_TEXT_STARTMENU_TITLE "Тека в меню Пуск"
  ${LangFileString} MUI_TEXT_STARTMENU_SUBTITLE "Оберіть «Тека в меню Пуск» для ярликів програми $(^NameDA)."
  ${LangFileString} MUI_INNERTEXT_STARTMENU_TOP "Оберіть теку в меню Пуск, в якій ви бажаєте створити ярлики для встановленої програми. Можна також ввести назву для створення нової теки."
  ${LangFileString} MUI_INNERTEXT_STARTMENU_CHECKBOX "Не створювати ярлики"
!endif

!ifdef MUI_UNCONFIRMPAGE
  ${LangFileString} MUI_UNTEXT_CONFIRM_TITLE "Видалення $(^NameDA)"
  ${LangFileString} MUI_UNTEXT_CONFIRM_SUBTITLE "Видалення $(^NameDA) з вашого комп’ютера."
!endif

!ifdef MUI_ABORTWARNING
  ${LangFileString} MUI_TEXT_ABORTWARNING "Ви дійсно хочете вийти з встановлення $(^Name)?"
!endif

!ifdef MUI_UNABORTWARNING
  ${LangFileString} MUI_UNTEXT_ABORTWARNING "Ви дійсно хочете вийти з видалення $(^Name)?"
!endif

!ifdef MULTIUSER_INSTALLMODEPAGE
  ${LangFileString} MULTIUSER_TEXT_INSTALLMODE_TITLE "Оберіть користувачів"
  ${LangFileString} MULTIUSER_TEXT_INSTALLMODE_SUBTITLE "Оберіть користувачів, для яких потрібно встановити $(^NameDA)."
  ${LangFileString} MULTIUSER_INNERTEXT_INSTALLMODE_TOP "Оберіть, чи хочете ви встановити $(^NameDA) лише для себе, чи для всіх користувачів цього комп’ютера. $(^ClickNext)"
  ${LangFileString} MULTIUSER_INNERTEXT_INSTALLMODE_ALLUSERS "Встановити для всіх користувачів цього комп’ютера"
  ${LangFileString} MULTIUSER_INNERTEXT_INSTALLMODE_CURRENTUSER "Встановити лише для мене"
!endif