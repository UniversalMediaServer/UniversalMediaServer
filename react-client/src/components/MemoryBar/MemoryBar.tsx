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

import { I18nInterface } from '../../contexts/i18n-context';
import { ServerEventInterface } from '../../contexts/server-event-context';

export const MemoryBar = ({ decorate, sse, i18n }: { decorate?: boolean, sse: ServerEventInterface, i18n: I18nInterface}) => {
  const MaxMemLabel = () => { return sse.memory.max.toString(); }
  const UsedMem = () => { return Math.max(0, sse.memory.used - sse.memory.dbcache - sse.memory.buffer); }
  const UsedMemPercent = () => { return Math.ceil((UsedMem() / sse.memory.max) * 100); }
  const UsedMemLabel = () => { return UsedMemPercent().toString() + ' %'; }
  const DbCacheMemPercent = () => { return Math.floor((sse.memory.dbcache / sse.memory.max) * 100) };
  const DbCacheMemLabel = () => { return DbCacheMemPercent().toString() + ' %'; }
  const BufferMemPercent = () => { return Math.floor((sse.memory.buffer / sse.memory.max) * 100) };
  const MemoryBarProgress =
    <Progress
      size='xl'
      radius='xl'
      sections={[
        { value: UsedMemPercent(), color: 'pink', label: UsedMemPercent() > 10 ? UsedMemLabel() : '', tooltip: 'UMS: ' + UsedMem() + ' ' + i18n.get['Mb'] },
        { value: DbCacheMemPercent(), color: 'grape', label: DbCacheMemPercent() > 10 ? DbCacheMemLabel() : '', tooltip: i18n.get['DatabaseCache'] + ' ' + sse.memory.dbcache + ' ' + i18n.get['Mb'] },
        { value: BufferMemPercent(), color: 'orange', tooltip: sse.memory.buffer + ' ' + i18n.get['Mb'] },
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