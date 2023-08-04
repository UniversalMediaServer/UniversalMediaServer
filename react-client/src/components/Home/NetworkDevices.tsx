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
import { ActionIcon, Card, Grid, Group, Menu, Modal, Text, TextInput } from '@mantine/core';
import _ from 'lodash';
import { CodePlus, DevicesPc, DevicesPcOff, Dots, Refresh } from 'tabler-icons-react';

import { NetworkDevice, NetworkDevicesFilter } from './Home';
import { useDisclosure } from '@mantine/hooks';
import { useState } from 'react';

const NetworkDevices = (
  { blockedByDefault, canModify, currentTime, i18n, networkDeviceFilters, refreshData, setAllowed }:
    {
      blockedByDefault: boolean,
      canModify: boolean,
      currentTime: number,
      i18n: { get: { [key: string]: string } },
      networkDeviceFilters: NetworkDevicesFilter[],
      refreshData: () => void,
      setAllowed: (rule: string, isAllowed: boolean) => void
    }
) => {
  const [showCustomFilter, { open, close }] = useDisclosure(false);
  const [customFilter, setCustomFilter] = useState('');
  const [customFilterValid, setCustomFilterValid] = useState(false);

  const getFormatedInterval = (time: number) => {
    let interval = (currentTime - time) / 1000;
    const seconds = Math.floor(interval % 60);
    const secondsAsString = seconds < 10 ? '0' + seconds : seconds;
    interval = Math.floor(interval / 60);
    const minutes = interval % 60;
    const minutesAsString = minutes < 10 ? '0' + minutes : minutes;
    interval = Math.floor(interval / 60);
    const hours = interval % 24;
    const hoursAsString = hours < 10 ? '0' + hours : hours;
    // Convert time difference from hours to days
    interval = Math.floor(interval / 24);
    return interval + ' ' + (interval > 1 ? i18n.get['Days'] : i18n.get['Day']) + ' ' + hoursAsString + ':' + minutesAsString + ':' + secondsAsString;
  };

  const setCustomFilterValue = (value: string) => {
    setCustomFilter(value);
    setCustomFilterValid(validateCustomFilter(value));
  }

  const validateCustomFilter = (value: string) => {
    const ipMatch = /(\d{0,3}-?\d{0,3}|\*)\.(\d{0,3}-?\d{0,3}|\*)\.(\d{0,3}-?\d{0,3}|\*)\.(\d{0,3}-?\d{0,3}|\*)/gm.exec(value)
    console.log(JSON.stringify(ipMatch));
    const hostnameMatch = /[\w.-]*/gm.exec(value)
    console.log(JSON.stringify(hostnameMatch));
    return (ipMatch != null && ipMatch[0] === value) || (hostnameMatch != null && hostnameMatch[0] === value);
  }

  const setCustomFilterAllowed = () => {
    if (validateCustomFilter(customFilter)) {
      setAllowed(customFilter, blockedByDefault);
    }
    setCustomFilter('');
    setCustomFilterValid(false);
    close();
  }

  const networkDevicesHeader = (
    <Card shadow='sm' p='lg' radius='md' mb='lg' withBorder>
      <Card.Section withBorder inheritPadding py='xs'>
        <Group position='apart'>
          <Text weight={500} color={blockedByDefault ? 'red' : 'green'}>{blockedByDefault ? i18n.get['NetworkDevicesBlockedByDefault'] : i18n.get['NetworkDevicesAllowedByDefault']}</Text>
          {canModify && (
            <Menu withinPortal position='bottom-end' shadow='sm'>
              <Menu.Target>
                <ActionIcon>
                  <Dots size={16} />
                </ActionIcon>
              </Menu.Target>
              <Menu.Dropdown>
                <>
                  {blockedByDefault ? (
                    <Menu.Item icon={<DevicesPc size={14} />} onClick={() => setAllowed('DEFAULT', true)} color='green'>{i18n.get['AllowByDefault']}</Menu.Item>
                  ) : (
                    <Menu.Item icon={<DevicesPcOff size={14} />} onClick={() => setAllowed('DEFAULT', false)} color='red'>{i18n.get['BlockByDefault']}</Menu.Item>
                  )}
                  <Menu.Item icon={<Refresh size={14} />} onClick={() => refreshData()}>{i18n.get['Refresh']}</Menu.Item>
                  <Menu.Item icon={<CodePlus size={14} />} onClick={() => open()}>{i18n.get['AddCustomFilter']}</Menu.Item>
                </>
              </Menu.Dropdown>
            </Menu>
          )}
        </Group>
      </Card.Section>
    </Card>
  );

  const networkDevicesCustomFilter = canModify && (
    <Modal opened={showCustomFilter} onClose={close} title={i18n.get['AddCustomFilter']}>
      <TextInput
        value={customFilter}
        onChange={(event) => setCustomFilterValue(event.currentTarget.value)}
        rightSection={customFilterValid && (
          <ActionIcon variant='outline' onClick={() => setCustomFilterAllowed()}>
            <CodePlus size={16} />
          </ActionIcon>
        )}
      />
    </Modal>
  );

  const networkDevicesCards = networkDeviceFilters && networkDeviceFilters.map((deviceFilter: NetworkDevicesFilter) => (
    <Grid.Col span={12} xs={6} key={deviceFilter.name}>
      <Card shadow='sm' p='lg' radius='md' withBorder>
        <Card.Section withBorder inheritPadding py='xs'>
          <Group position='apart'>
            <Text weight={500} color={deviceFilter.isAllowed ? 'green' : 'red'}>{deviceFilter.name}</Text>
            {canModify && !deviceFilter.isDefault && (
              <Menu withinPortal position='bottom-end' shadow='sm'>
                <Menu.Target>
                  <ActionIcon>
                    <Dots size={16} />
                  </ActionIcon>
                </Menu.Target>
                <Menu.Dropdown>
                  <>
                    {!deviceFilter.isAllowed && (
                      <Menu.Item icon={<DevicesPc size={14} />} onClick={() => setAllowed(deviceFilter.name, true)} color='green'>{i18n.get['Allow']}</Menu.Item>
                    )}
                    {deviceFilter.isAllowed && (
                      <Menu.Item icon={<DevicesPcOff size={14} />} onClick={() => setAllowed(deviceFilter.name, false)} color='red'>{i18n.get['Block']}</Menu.Item>
                    )}
                  </>
                </Menu.Dropdown>
              </Menu>
            )}
          </Group>
        </Card.Section>
        {deviceFilter.devices && deviceFilter.devices.map((device: NetworkDevice, index) => (
          <Card.Section key={'devices' + index}>
            <Text align='center' size='sm'>
              {device.hostName}
            </Text>
            {device.ipAddress != device.hostName && (
              <Text align='center' size='sm' color='dimmed'>
                {device.ipAddress}
              </Text>
            )}
            {device.lastSeen && (
              <Text align='center' size='sm' color='dimmed'>
                {getFormatedInterval(device.lastSeen)}
              </Text>
            )}
          </Card.Section>
        ))}
      </Card>
    </Grid.Col>
  ));

  return (
    <>
      {networkDevicesHeader}
      {networkDevicesCustomFilter}
      <Grid>
        {networkDevicesCards}
      </Grid>
    </>
  );
};

export default NetworkDevices;
