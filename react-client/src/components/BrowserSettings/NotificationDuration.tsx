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
import { Button, Group, MantineColor, Paper, Slider, Text } from '@mantine/core'
import { I18nInterface } from '../../services/i18n-service'

export default function NotificationDuration({
  i18n,
  title,
  color,
  duration,
  setDuration,
  test,
}: {
  i18n: I18nInterface
  title: string
  color?: MantineColor
  duration: number
  setDuration: (duration: number) => void
  test?: ({ message, message2, title }: { message: React.ReactNode, message2?: React.ReactNode, title?: React.ReactNode }) => void
}) {
  const never = i18n.get('Never')
  const marks = [
    { value: 0, label: never },
    { value: 10000, label: '10s' },
  ]

  const label = (value: number) => {
    if (value === 0) {
      return test ? never : ''
    }
    else {
      return (value / 1000) + 's'
    }
  }
  return (
    <Paper shadow="xs" py="xs" px="xl" m="10">
      <Group justify="space-between">
        <Text size="sm">{title}</Text>
        <Button
          radius="xl"
          size="compact-xs"
          onClick={() => test && test({ message: title, message2: 'Test message' })}
        >
          Try
        </Button>
      </Group>
      <Slider
        defaultValue={duration}
        label={label}
        marks={marks}
        min={0}
        max={10000}
        step={1000}
        mb="xl"
        color={color}
        onChangeEnd={setDuration}
      />
    </Paper>
  )
}
