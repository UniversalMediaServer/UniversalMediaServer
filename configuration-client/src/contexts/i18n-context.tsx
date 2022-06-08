import { Context, createContext } from "react";

export const i18nContext: Context<{[key: string]: string}> = createContext({});

export default i18nContext;