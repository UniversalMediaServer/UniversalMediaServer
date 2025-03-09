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
import { Paper } from '@mantine/core'
import { useInterval } from '@mantine/hooks'
import { useState } from 'react'

export default function MediaCard({ children, onClick, onLongPress }: { children: React.ReactNode, onClick?: () => void, onLongPress?: () => void }) {
  const [progressValue, setProgressValue] = useState(0)
  const interval = useInterval(() => {
    setProgressValue((current) => {
      return current + 50
    })
    if (progressValue >= 500 && onLongPress) {
      onLongPress()
    }
  }, 50)

  const startLongPress = (_event: React.MouseEvent<HTMLDivElement> | React.TouchEvent<HTMLDivElement>) => {
    setProgressValue(0)
    interval.start()
  }

  const stopLongPress = (_event: React.MouseEvent<HTMLDivElement> | React.TouchEvent<HTMLDivElement>) => {
    interval.stop()
    if (progressValue >= 500 && onLongPress) {
      onLongPress()
    }
    cancelLongPress()
  }

  const cancelLongPress = (_event?: React.MouseEvent<HTMLDivElement> | React.TouchEvent<HTMLDivElement>) => {
    interval.stop()
    setProgressValue(0)
  }

  const resetLongPress = (_event?: React.MouseEvent<HTMLDivElement> | React.TouchEvent<HTMLDivElement>) => {
    setProgressValue(0)
  }

  return (
    <Paper
      className="thumbnail-container"
      styles={{ root: { backgroundColor: '' } }}
      onMouseDown={onLongPress ? startLongPress : undefined}
      onMouseUp={onLongPress ? stopLongPress : undefined}
      onMouseLeave={onLongPress ? stopLongPress : undefined}
      onTouchStart={onLongPress ? startLongPress : undefined}
      onTouchEnd={onLongPress ? stopLongPress : undefined}
      onTouchCancel={onLongPress ? cancelLongPress : undefined}
      onTouchMove={onLongPress ? resetLongPress : undefined}
      onDrag={onLongPress ? cancelLongPress : undefined}
      onClick={onClick ? onClick : undefined}
    >
      {children}
    </Paper>
  )
}
