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
import { Group, Progress, Text, Tooltip } from '@mantine/core';

import { I18nInterface } from '../../contexts/i18n-context';
import { UmsMemory } from '../../contexts/server-event-context';

export const MemoryBar = ({ decorate, memory, i18n }: { decorate?: boolean, memory: UmsMemory, i18n: I18nInterface }) => {
  const MaxMemLabel = () => { return memory.max.toString(); }
  const UsedMem = () => { return Math.max(0, memory.used - memory.dbcache - memory.buffer); }
  const UsedMemPercent = () => { return Math.ceil((UsedMem() / memory.max) * 100); }
  const UsedMemLabel = () => { return UsedMemPercent().toString() + ' %'; }
  const DbCacheMemPercent = () => { return Math.floor((memory.dbcache / memory.max) * 100) };
  const DbCacheMemLabel = () => { return DbCacheMemPercent().toString() + ' %'; }
  const BufferMemPercent = () => { return Math.floor((memory.buffer / memory.max) * 100) };
  const MemoryBarProgress =
    <Progress.Root size='xl' mt='4px'>
      <Tooltip label={'UMS: ' + UsedMem() + ' ' + i18n.get('Mb')}>
        <Progress.Section value={UsedMemPercent()} color='pink'>
          <Progress.Label>{UsedMemPercent() > 10 ? UsedMemLabel() : ''}</Progress.Label>
        </Progress.Section>
      </Tooltip>
      <Tooltip label={i18n.get('DatabaseCache') + ' ' + memory.dbcache + ' ' + i18n.get('Mb')}>
        <Progress.Section value={DbCacheMemPercent()} color='grape'>
          <Progress.Label>{DbCacheMemPercent() > 10 ? DbCacheMemLabel() : ''}</Progress.Label>
        </Progress.Section>
      </Tooltip>
      <Tooltip label={memory.buffer + ' ' + i18n.get('Mb')}>
        <Progress.Section value={BufferMemPercent()} color='orange' />
      </Tooltip>
    </Progress.Root>
    ;

  return decorate ? (
    <Group justify='center' gap='xs' grow>
      <Text>{i18n.get('MemoryUsage')}</Text>
      {MemoryBarProgress}
      <Text>{MaxMemLabel() + ' ' + i18n.get('Mb')}</Text>
    </Group>
  ) : MemoryBarProgress;
};

export default MemoryBar;