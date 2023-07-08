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
import { Group, Progress, Text } from '@mantine/core';
import { useContext } from 'react';

import I18nContext from '../../contexts/i18n-context';
import ServerEventContext from '../../contexts/server-event-context';

export const MemoryBar = ({ decorate }: { decorate?: boolean }) => {
  const sse = useContext(ServerEventContext);
  const i18n = useContext(I18nContext);
  const MaxMemLabel = () => { return sse.memory.max.toString(); }
  const UsedMemPercent = () => { return Math.ceil((sse.memory.used / sse.memory.max) * 100); }
  const UsedMemLabel = () => { return UsedMemPercent().toString() + ' %'; }
  const BufferMemPercent = () => { return Math.floor((sse.memory.buffer / sse.memory.max) * 100) };
  const MemoryBarProgress =
    <Progress
      size='xl'
      radius='xl'
      sections={[
        { value: UsedMemPercent(), color: 'pink', label: UsedMemPercent() > 10 ? UsedMemLabel() : '' },
        { value: BufferMemPercent(), color: 'grape' },
      ]}
      style={{ marginTop: '4px' }}
    />
    ;

  return decorate ? (
    <Group position='center' spacing='xs' grow>
      <Text>{i18n.get['MemoryUsage']}</Text>
      {MemoryBarProgress}
      <Text>{MaxMemLabel() + ' ' + i18n.get['Mb']}</Text>
    </Group>
  ) : MemoryBarProgress;
};

export default MemoryBar;