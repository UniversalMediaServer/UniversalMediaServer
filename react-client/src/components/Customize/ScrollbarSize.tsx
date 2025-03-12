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
import { Box, Paper, ScrollArea, Slider } from '@mantine/core'
import { useLocalStorage } from '@mantine/hooks'

export default function ScrollbarSize() {
  const [scrollbarSize, setScrollbarSize] = useLocalStorage<number>({
    key: 'mantine-scrollbar-size',
    defaultValue: 10,
  })

  return (
    <Paper shadow="xs" py="xs" px="xl" m="10">
      <Slider
        value={scrollbarSize}
        min={1}
        max={20}
        mb="xl"
        onChange={setScrollbarSize}
      />
      <Paper shadow="xs" withBorder>
        <ScrollArea h={100} type="always" scrollbars="y">
          <Box h={600} c="blue"></Box>
        </ScrollArea>
      </Paper>
    </Paper>
  )
}
