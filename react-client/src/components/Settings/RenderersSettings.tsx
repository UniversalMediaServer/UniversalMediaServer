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
import { Accordion, Checkbox, MultiSelect, Select, Stack, Tooltip } from '@mantine/core';
import { useContext } from 'react';

import I18nContext from '../../contexts/i18n-context';
import SessionContext from '../../contexts/session-context';
import { havePermission, Permissions } from '../../services/accounts-service';
import { allowHtml, defaultTooltipSettings } from '../../utils';
import { mantineSelectData } from './Settings';

export default function RenderersSettings(
  form: any,
  selectionSettings: any,
) {
  const i18n = useContext(I18nContext);
  const session = useContext(SessionContext);
  const canModify = havePermission(session, Permissions.settings_modify);

  const getI18nSelectData = (values: mantineSelectData[]) => {
    return values.map((value: mantineSelectData) => {
      return { value: value.value, label: i18n.getI18nString(value.label) };
    });
  }

  return (
    <Accordion>
      <Accordion.Item value='Renderers'>
        <Accordion.Control>{i18n.get['Renderers']}</Accordion.Control>
        <Accordion.Panel>
          <Stack>
            <MultiSelect
              disabled={!canModify}
              data={getI18nSelectData(selectionSettings.allRendererNames)}
              label={i18n.get['EnabledRenderers']}
              {...form.getInputProps('selected_renderers')}
            />
            <Select
              disabled={!canModify}
              sx={{ flex: 1 }}
              label={i18n.get['DefaultRendererWhenAutoFails']}
              data={getI18nSelectData(selectionSettings.enabledRendererNames)}
              {...form.getInputProps('renderer_default')}
              searchable
            />
            <Tooltip label={allowHtml(i18n.get['DisablesAutomaticDetection'])} {...defaultTooltipSettings}>
              <Checkbox
                disabled={!canModify}
                label={i18n.get['ForceDefaultRenderer']}
                {...form.getInputProps('renderer_force_default', { type: 'checkbox' })}
              />
            </Tooltip>
          </Stack>
        </Accordion.Panel>
      </Accordion.Item>
    </Accordion>
  );
}
