import { Group, Progress, Text } from '@mantine/core';
import React, { useContext } from 'react';

import I18nContext from '../../contexts/i18n-context';
import ServerEventContext from '../../contexts/server-event-context';

export const MemoryBar = ({ decorate }: { decorate?: boolean }) => {
  const sse = useContext(ServerEventContext);
  const i18n = useContext(I18nContext);
  const MaxMemLabel = () => { return sse.memory.max.toString(); }
  const UsedMemPercent = () => { return Math.ceil((sse.memory.used / sse.memory.max) * 100); }
  const UsedMemLabel = () => { return UsedMemPercent().toString() + " %"; }
  const BufferMemPercent = () => { return Math.floor((sse.memory.buffer / sse.memory.max) * 100)};
  const MemoryBarProgress =
    <Progress
      mt="md"
      size="xl"
      radius="xl"
      sections={[
        { value: UsedMemPercent(), color: 'pink', label: UsedMemPercent() > 10 ? UsedMemLabel() : '' },
        { value: BufferMemPercent(), color: 'grape'},
      ]}
      style={{marginTop: '4px'}}
    />
  ;

  return decorate ? (
    <Group position="center" spacing="xs" grow>
      <Text>{i18n.get['MemoryUsage']}</Text>
      { MemoryBarProgress }
      <Text>{MaxMemLabel() + ' ' + i18n.get['Mb']}</Text>
    </Group>
  ) : MemoryBarProgress;
};

export default MemoryBar;