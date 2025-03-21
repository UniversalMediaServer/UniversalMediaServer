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
import { Box, Button, Group, Tabs, Text } from '@mantine/core'
import { useForm } from '@mantine/form'
import { useLocalStorage } from '@mantine/hooks'
import { hideNotification } from '@mantine/notifications'
import axios, { AxiosError } from 'axios'
import _ from 'lodash'
import { useEffect, useState } from 'react'
import { IconCheck } from '@tabler/icons-react'

import { I18nInterface } from '../../services/i18n-service'
import { ServerEventInterface } from '../../services/server-event-service'
import { SessionInterface, UmsPermission } from '../../services/session-service'
import { SelectionSettingsData } from '../../services/settings-service'
import { settingsApiUrl } from '../../utils'
import { showError, showLoading, updateError, updateInfo, updateSuccess } from '../../utils/notifications'
import GeneralSettings from './GeneralSettings'
import NavigationSettings from './NavigationSettings'
import RenderersSettings from './RenderersSettings'
import TranscodingSettings from './TranscodingSettings'

export default function ServerSettings({ i18n, session, sse }: { i18n: I18nInterface, session: SessionInterface, sse: ServerEventInterface }) {
  const [advancedSettings] = useLocalStorage<boolean>({
    key: 'mantine-advanced-settings',
    defaultValue: false,
  })
  const [isLoading, setLoading] = useState(true)
  const [defaultConfiguration, setDefaultConfiguration] = useState<Record<string, unknown>>({})
  const [configuration, setConfiguration] = useState<Record<string, unknown>>({})
  // key/value pairs for dropdowns
  const [selectionSettings, setSelectionSettings] = useState<SelectionSettingsData>()

  const canModify = session.havePermission(UmsPermission.settings_modify)
  const canView = canModify || session.havePermission(UmsPermission.settings_view)

  const form = useForm({
    enhanceGetInputProps: (payload) => {
      if (!payload.form.initialized) {
        return { disabled: true }
      }
      if (!canModify) {
        return { disabled: true }
      }
      return {}
    },
  })
  const formSetValues = form.setValues

  useEffect(() => {
    session.useSseAs(ServerSettings.name)
    session.stopPlayerSse()
    session.setDocumentI18nTitle('ServerSettings')
    session.setNavbarManage(ServerSettings.name)
  }, [])

  useEffect(() => {
    if (sse.userConfiguration === null) {
      return
    }
    const userConfig = _.merge({}, configuration, sse.userConfiguration)
    sse.setUserConfiguration(null)
    setConfiguration(userConfig)
    formSetValues(userConfig)
  }, [configuration, sse, formSetValues])

  // Code here will run just like componentDidMount
  useEffect(() => {
    if (canView) {
      axios.get(settingsApiUrl)
        .then(function (response: any) {
          const settingsResponse = response.data
          setSelectionSettings(settingsResponse.selectionSettings)
          setDefaultConfiguration(settingsResponse.userSettingsDefaults)

          // merge defaults with what we receive, which might only be non-default values
          const userConfig = _.merge({}, settingsResponse.userSettingsDefaults, settingsResponse.userSettings)

          setConfiguration(userConfig)
          form.initialize(userConfig)
        })
        .catch(function (error: AxiosError) {
          if (!error.response && error.request) {
            i18n.showServerUnreachable()
          }
          else {
            showError({
              id: 'data-loading',
              title: i18n.get('Error'),
              message: i18n.get('ConfigurationNotReceived'),
              message2: i18n.getReportLink(),
            })
          }
        })
        .then(function () {
          setLoading(false)
        })
    }
  }, [canView])

  const handleSubmit = async (values: typeof form.values) => {
    setLoading(true)
    showLoading({
      id: 'settings-save',
      title: i18n.get('Save'),
      message: i18n.get('SavingConfiguration'),
    })
    const changedValues: Record<string, any> = {}

    // construct an object of only changed values to send
    for (const key in values) {
      if (!_.isEqual(configuration[key], values[key])) {
        changedValues[key] = values[key] !== undefined && values[key] !== null ? values[key] : null
      }
    }

    if (_.isEmpty(changedValues)) {
      updateInfo({
        id: 'settings-save',
        title: i18n.get('Saved'),
        message: i18n.get('ConfigurationHasNoChanges'),
      })
    }
    else {
      await axios.post(settingsApiUrl, changedValues)
        .then(function () {
          setConfiguration(values)
          setLoading(false)
          updateSuccess({
            id: 'settings-save',
            title: i18n.get('Saved'),
            message: i18n.get('ConfigurationSaved'),
            icon: <IconCheck size="1rem" />,
          })
        })
        .catch(function (error: AxiosError) {
          if (!error.response && error.request) {
            hideNotification('settings-save')
            i18n.showServerUnreachable()
          }
          else {
            updateError({
              id: 'settings-save',
              title: i18n.get('Error'),
              message: i18n.get('ConfigurationNotSaved'),
              message2: i18n.getReportLink(),
            })
          }
        })
    }
    setLoading(false)
  }

  return canView
    ? (
        <Box style={{ maxWidth: 1024 }} mx="auto">
          <form onSubmit={form.onSubmit(handleSubmit)}>
            <Tabs defaultValue="GeneralSettings">
              <Tabs.List>
                <Tabs.Tab value="GeneralSettings">{i18n.get('GeneralSettings')}</Tabs.Tab>
                {advancedSettings
                  && <Tabs.Tab value="RenderersSettings">{i18n.get('RenderersSettings')}</Tabs.Tab>}
                {advancedSettings
                  && <Tabs.Tab value="NavigationSettings">{i18n.get('NavigationSettings')}</Tabs.Tab>}
                <Tabs.Tab value="TranscodingSettings">{i18n.get('TranscodingSettings')}</Tabs.Tab>
              </Tabs.List>
              <Tabs.Panel value="GeneralSettings">
                <GeneralSettings i18n={i18n} form={form} defaultConfiguration={defaultConfiguration} selectionSettings={selectionSettings} />
              </Tabs.Panel>
              {advancedSettings
                && (
                  <Tabs.Panel value="RenderersSettings">
                    <RenderersSettings i18n={i18n} form={form} selectionSettings={selectionSettings} />
                  </Tabs.Panel>
                )}
              {advancedSettings
                && (
                  <Tabs.Panel value="NavigationSettings">
                    <NavigationSettings i18n={i18n} canModify={canModify} form={form} defaultConfiguration={defaultConfiguration} selectionSettings={selectionSettings} />
                  </Tabs.Panel>
                )}
              <Tabs.Panel value="TranscodingSettings">
                <TranscodingSettings i18n={i18n} canModify={canModify} form={form} defaultConfiguration={defaultConfiguration} selectionSettings={selectionSettings} />
              </Tabs.Panel>
            </Tabs>
            {canModify && (
              <Group justify="flex-end" mt="md">
                <Button type="submit" loading={isLoading}>
                  {i18n.get('Save')}
                </Button>
              </Group>
            )}
          </form>
        </Box>
      )
    : (
        <Box style={{ maxWidth: 1024 }} mx="auto">
          <Text c="red">{i18n.get('YouDontHaveAccessArea')}</Text>
        </Box>
      )
}
