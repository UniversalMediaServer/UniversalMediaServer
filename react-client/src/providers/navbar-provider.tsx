import { ReactNode, useState } from 'react';

import NavbarContext from '../contexts/navbar-context';

interface Props {
  children?: ReactNode
}

export const NavbarProvider = ({ children, ...props }: Props) => {
  const [value, setValue] = useState(undefined);
  const [opened, setOpened] = useState<boolean>(false);
  const { Provider } = NavbarContext;
  return(
    <Provider value={{
      value: value,
      setValue : setValue,
      opened : opened,
      setOpened : setOpened,
    }}>{children}</Provider>
  )
}