import { Context, createContext } from "react";

export const i18nContext: Context<{
  get:{[key: string]: string};
  getI18nString: (value: string) => string;
  language:string;
  rtl:boolean;
  languages:LanguageValue[];
  updateLanguage: (newlang: string) => void;
}> = createContext({
  get:{},
  getI18nString: (value: string) => {return value},
  language:"en-US",
  rtl:false as boolean,
  languages:[] as LanguageValue[],
  updateLanguage: (newlang: string) => {}
});

export default i18nContext;

export interface LanguageValue {
  id : string,
  name : string,
  country : string,
}