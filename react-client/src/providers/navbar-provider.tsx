/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
import { ReactNode, useState } from 'react';

import NavbarContext from '../contexts/navbar-context';

interface Props {
  children?: ReactNode
}

export const NavbarProvider = ({ children, ...props }: Props) => {
  const [value, setValue] = useState(undefined);
  const [opened, setOpened] = useState<boolean>(false);
  const { Provider } = NavbarContext;
  return (
    <Provider value={{
      value: value,
      setValue: setValue,
      opened: opened,
      setOpened: setOpened,
    }}>{children}</Provider>
  )
}