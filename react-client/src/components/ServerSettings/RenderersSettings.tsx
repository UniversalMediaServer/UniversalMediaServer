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
import { Accordion, Checkbox, MultiSelect, Select, Stack, Tooltip } from '@mantine/core'

import { havePermission, Permissions } from '../../services/accounts-service'
import { I18nInterface } from '../../services/i18n-service'
import { SessionInterface } from '../../services/session-service'
import { allowHtml, defaultTooltipSettings } from '../../utils'

export default function RenderersSettings({
  i18n,
  session,
  form,
  selectionSettings,
}: {
  i18n: I18nInterface
  session: SessionInterface
  form: any
  selectionSettings: any
}) {
  const canModify = havePermission(session, Permissions.settings_modify)

  return (
    <Accordion>
      <Accordion.Item value="Renderers">
        <Accordion.Control>{i18n.get('Renderers')}</Accordion.Control>
        <Accordion.Panel>
          <Stack>
            <MultiSelect
              disabled={!canModify}
              data={i18n.getValueLabelData(selectionSettings.allRendererNames)}
              label={i18n.get('EnabledRenderers')}
              {...form.getInputProps('selected_renderers')}
            />
            <Select
              disabled={!canModify}
              style={{ flex: 1 }}
              label={i18n.get('DefaultRendererWhenAutoFails')}
              data={i18n.getValueLabelData(selectionSettings.enabledRendererNames)}
              {...form.getInputProps('renderer_default')}
              searchable
            />
            <Tooltip label={allowHtml(i18n.get('DisablesAutomaticDetection'))} {...defaultTooltipSettings}>
              <Checkbox
                disabled={!canModify}
                label={i18n.get('ForceDefaultRenderer')}
                {...form.getInputProps('renderer_force_default', { type: 'checkbox' })}
              />
            </Tooltip>
          </Stack>
        </Accordion.Panel>
      </Accordion.Item>
    </Accordion>
  )
}
