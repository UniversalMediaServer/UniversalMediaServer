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
import { Box, Button, Group, Text } from '@mantine/core'
import { useForm } from '@mantine/form'
import axios from 'axios'
import _ from 'lodash'
import { useEffect, useState } from 'react'

import { I18nInterface } from '../../services/i18n-service'
import { ServerEventInterface } from '../../services/server-event-service'
import { SessionInterface, UmsPermission } from '../../services/session-service'
import { sharedApiUrl } from '../../utils'
import SharedContentSettings from './SharedContentSettings'
import { showError, showInfo } from '../../utils/notifications'

export default function SharedContent({ i18n, session, sse }: { i18n: I18nInterface, session: SessionInterface, sse: ServerEventInterface }) {
  const [isLoading, setLoading] = useState(true)
  const [configuration, setConfiguration] = useState({} as any)

  const form = useForm({ initialValues: {} as Record<string, unknown> })
  const formSetValues = form.setValues

  const canModify = session.havePermission(UmsPermission.settings_modify)
  const canView = canModify || session.havePermission(UmsPermission.settings_view)

  useEffect(() => {
    session.useSseAs(SharedContent.name)
    session.stopPlayerSse()
    session.setDocumentI18nTitle('SharedContent')
    session.setNavbarManage(SharedContent.name)
  }, [])

  useEffect(() => {
    if (sse.userConfiguration === null) {
      return
    }

    const currentConfiguration = _.cloneDeep(configuration)
    // set a fresh state for shared_content
    if (sse.userConfiguration['shared_content']) {
      delete currentConfiguration['shared_content']
    }

    const newConfiguration = _.merge({}, currentConfiguration, sse.userConfiguration)
    sse.setUserConfiguration(null)
    setConfiguration(newConfiguration)
    formSetValues(newConfiguration)
  }, [configuration, sse, formSetValues])

  useEffect(() => {
    if (canView) {
      axios.get(sharedApiUrl)
        .then(function (response: any) {
          const sharedResponse = response.data
          setConfiguration(sharedResponse)
          formSetValues(sharedResponse)
        })
        .catch(function () {
          showError({
            id: 'data-loading',
            title: i18n.get('Error'),
            message: i18n.get('ConfigurationNotReceived'),
            message2: i18n.getReportLink(),
          })
        })
        .then(function () {
          setLoading(false)
        })
    }
  }, [canView, formSetValues])

  const handleSubmit = async (values: typeof form.values) => {
    setLoading(true)
    try {
      const changedValues: Record<string, any> = {}

      // construct an object of only changed values to send
      for (const key in values) {
        if (!_.isEqual(configuration[key], values[key])) {
          changedValues[key] = values[key] ? values[key] : null
        }
      }

      if (_.isEmpty(changedValues)) {
        showInfo({
          title: i18n.get('Saved'),
          message: i18n.get('ConfigurationHasNoChanges'),
        })
      }
      else {
        await axios.post(sharedApiUrl, changedValues)
        setConfiguration(values)
        setLoading(false)
        showInfo({
          title: i18n.get('Saved'),
          message: i18n.get('ConfigurationSaved'),
        })
      }
    }
    catch (err) {
      showError({
        title: i18n.get('Error'),
        message: i18n.get('ConfigurationNotSaved'),
        message2: i18n.getReportLink(),
      })
    }
    setLoading(false)
  }

  return canView
    ? (
        <Box style={{ maxWidth: 1024 }} mx="auto">
          <form onSubmit={form.onSubmit(handleSubmit)}>
            <Text size="lg" mb="md">{i18n.get('SharedContent')}</Text>
            <SharedContentSettings i18n={i18n} session={session} sse={sse} form={form} configuration={configuration} />
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
