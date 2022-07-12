import { Context, createContext } from "react";

export const i18nContext: Context<{
  get:{[key: string]: string};
  getI18nString: (value: string) => string;
  getI18nFormat: (value: string[]) => string;
  language:string;
  rtl:boolean;
  languages:LanguageValue[];
  setLanguage: (language: string) => void;
}> = createContext({
  get:{},
  getI18nString: (value: string) => {return value},
  getI18nFormat: (value: string[]) => {return value.length ? value[0] : ""},
  language:"en-US",
  rtl:false as boolean,
  languages:[] as LanguageValue[],
  setLanguage: (language: string) => {}
});

export default i18nContext;

export interface LanguageValue {
  id : string,
  name : string,
  defaultname : string,
  country : string,
  coverage : number,
}