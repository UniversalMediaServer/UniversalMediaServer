import { Context, createContext } from "react";

export const i18nContext: Context<{get:{[key: string]: string};languages:LanguageValue[];updateLanguage: (language: string) => void;}> = createContext({get:{},languages:[] as LanguageValue[],updateLanguage: (language: string) => {}});

export default i18nContext;

export interface LanguageValue {
  id : string,
  name : string,
  country : string,
}