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
import { Affix, Box, Button, Text } from '@mantine/core'
import axios, { AxiosError, AxiosResponse } from 'axios'
import _ from 'lodash'
import { useEffect, useState } from 'react'

import { I18nInterface } from '../../services/i18n-service'
import { ServerEventInterface } from '../../services/server-event-service'
import { SessionInterface, UmsPermission } from '../../services/session-service'
import { SharedContentConfiguration, SharedContentInterface } from '../../services/shared-service'
import { sharedApiUrl } from '../../utils'
import { showError, showInfo } from '../../utils/notifications'
import SharedContentSettings from './SharedContentSettings'

export default function SharedContent({ i18n, session, sse }: { i18n: I18nInterface, session: SessionInterface, sse: ServerEventInterface }) {
  const [isLoading, setLoading] = useState(true)
  const [configuration, setConfiguration] = useState<SharedContentConfiguration>()
  const [sharedContents, setSharedContents] = useState<SharedContentInterface[]>()
  const [modified, setModified] = useState(false)
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
    if (sse.userConfiguration['shared_content'] && currentConfiguration !== undefined) {
      currentConfiguration.shared_content = []
    }

    const newConfiguration = _.merge({}, currentConfiguration, sse.userConfiguration)
    sse.setUserConfiguration(null)
    setConfiguration(newConfiguration)
    setSharedContents(newConfiguration.shared_content)
  }, [configuration, sse])

  useEffect(() => {
    if (canView) {
      axios.get(sharedApiUrl)
        .then(function (response: AxiosResponse) {
          const sharedResponse = response.data as SharedContentConfiguration
          setConfiguration(sharedResponse)
          setSharedContents(sharedResponse.shared_content)
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

  useEffect(() => {
    setModified(!_.isEqual(configuration?.shared_content, sharedContents))
  }, [configuration, sharedContents])

  const submit = async () => {
    setLoading(true)
    try {
      const changedValues: Record<string, unknown> = {}

      // construct an object of only changed values to send
      if (!_.isEqual(configuration?.shared_content, sharedContents)) {
        changedValues['shared_content'] = sharedContents
      }

      if (_.isEmpty(changedValues)) {
        showInfo({
          title: i18n.get('Saved'),
          message: i18n.get('ConfigurationHasNoChanges'),
        })
      }
      else {
        await axios.post(sharedApiUrl, changedValues)
        setLoading(false)
        showInfo({
          title: i18n.get('Saved'),
          message: i18n.get('ConfigurationSaved'),
        })
      }
    }
    catch {
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
          <Text size="lg" mb="md">{i18n.get('SharedContent')}</Text>
          {sharedContents && configuration && (
            <SharedContentSettings i18n={i18n} canModify={canModify} sse={sse} sharedContents={sharedContents} setSharedContents={setSharedContents} configuration={configuration} />
          )}
          {canModify && modified && (
            <Box h={50}>
              <Affix withinPortal={false} position={{ bottom: 20, right: 20 }}>
                <Button
                  loading={isLoading}
                  onClick={() => submit()}
                >
                  {i18n.get('Save')}
                </Button>
              </Affix>
            </Box>
          )}
        </Box>
      )
    : (
        <Box style={{ maxWidth: 1024 }} mx="auto">
          <Text c="red">{i18n.get('YouDontHaveAccessArea')}</Text>
        </Box>
      )
}
