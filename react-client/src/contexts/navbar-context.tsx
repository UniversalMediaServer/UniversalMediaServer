import { Context, createContext } from 'react';

export const NavbarContext: Context<{
  value: any;
  setValue: (navbar: any) => void;
  opened: boolean;
  setOpened: (opened: any) => void;
}> = createContext({
  value : undefined,
  setValue : (navbar) => {},
  opened : false as boolean,
  setOpened : (opened) => {},
});

export default NavbarContext;
