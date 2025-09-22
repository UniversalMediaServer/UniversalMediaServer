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
import { Badge, Group, MantineBreakpoint, Paper, Slider, Text } from '@mantine/core'
import { useLocalStorage } from '@mantine/hooks'
import { useEffect, useState } from 'react'

export default function NavbarWidth({
  title,
  storageKey,
  defaultValue,
  visibleFrom,
  hiddenFrom,
}: {
  title: string
  storageKey: string
  defaultValue: number
  visibleFrom?: MantineBreakpoint
  hiddenFrom?: MantineBreakpoint
}) {
  const [navbarWidthShow, setNavbarWidthShow] = useState<number>(defaultValue)
  const [navbarWidth, setNavbarWidth] = useLocalStorage<number>({
    key: storageKey,
    defaultValue: defaultValue,
  })

  useEffect(() => {
    setNavbarWidthShow(navbarWidth)
  }, [navbarWidth])

  return (
    <Paper shadow="xs" py="xs" px="xl" m="10">
      <Group>
        <Badge visibleFrom={visibleFrom} hiddenFrom={hiddenFrom}>Current</Badge>
        <Text>{title}</Text>
        <Text>
          {navbarWidth}
          px
        </Text>
      </Group>
      <Slider
        value={navbarWidthShow}
        min={100}
        max={500}
        step={10}
        mb="xl"
        onChange={setNavbarWidthShow}
        onChangeEnd={setNavbarWidth}
      />
    </Paper>
  )
}
