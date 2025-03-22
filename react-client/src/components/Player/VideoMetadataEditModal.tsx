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
import { Badge, Button, Card, Divider, Flex, Group, Image, Modal, NumberInput, ScrollArea, Spoiler, Stack, Text, TextInput } from '@mantine/core'
import { useForm } from '@mantine/form'
import axios, { AxiosError } from 'axios'
import { useEffect, useState } from 'react'
import { IconListSearch } from '@tabler/icons-react'

import { I18nInterface } from '../../services/i18n-service'
import { playerApiUrl } from '../../utils'
import { showError } from '../../utils/notifications'

export default function VideoMetadataEditModal({
  i18n,
  uuid,
  id,
  start,
  started,
  callback,
}: {
  i18n: I18nInterface
  uuid: string
  id: string
  start: boolean
  started: () => void
  callback: () => void
}) {
  const [isLoading, setLoading] = useState(true)
  const [opened, setOpened] = useState(false)
  const [editResults, setEditResults] = useState<TmdbResult[]>([])
  const [editData, setEditData] = useState<BaseEdit | null>(null)
  const searchForm = useForm()

  const getEditData = () => {
    setLoading(true)
    axios.post(playerApiUrl + 'edit', { uuid: uuid, id: id })
      .then(function (response: any) {
        setEditData(response.data)
        searchForm.setValues(response.data)
        setOpened(true)
      })
      .catch(function (error: AxiosError) {
        if (!error.response && error.request) {
          i18n.showServerUnreachable()
        }
        else {
          showError({
            id: 'data-loading',
            title: i18n.get('Error'),
            message: 'Your edit data was not received from the server.',
          })
        }
      })
      .then(function () {
        setLoading(false)
      })
  }

  const getMetadataResults = (media_type: string, search: string, year: string) => {
    setLoading(true)
    axios.post(playerApiUrl + 'findMetadata', { uuid: uuid, id: id, media_type: media_type, search: search, year: year, lang: i18n.language })
      .then(function (response: any) {
        setEditResults(response.data)
      })
      .catch(function (error: AxiosError) {
        if (!error.response && error.request) {
          i18n.showServerUnreachable()
        }
        else {
          showError({
            id: 'data-loading',
            title: i18n.get('Error'),
            message: 'Your search data was not received from the server.',
          })
        }
      })
      .then(function () {
        setLoading(false)
      })
  }

  const setMetadataChange = (tmdb_id: number) => {
    setLoading(true)
    axios.post(playerApiUrl + 'setMetadata', { uuid: uuid, id: id, media_type: editData?.media_type, tmdb_id })
      .then(function () {
        setOpened(false)
        callback()
      })
      .catch(function (error: AxiosError) {
        if (!error.response && error.request) {
          i18n.showServerUnreachable()
        }
        else {
          showError({
            id: 'data-loading',
            title: i18n.get('Error'),
            message: 'Your data sent was not received from the server.',
          })
        }
      })
      .then(function () {
        setLoading(false)
      })
  }

  const handleSearchForm = (values: any) => {
    getMetadataResults(values.media_type, values.search, values.year)
  }

  const MetadataResultCard = ({ i18n, tmdbResult, editData }: { i18n: I18nInterface, tmdbResult: TmdbResult, editData?: BaseEdit | null }) => {
    const buttonText = tmdbResult.selected ? i18n.get('SelectedMedia') : editData?.media_type == 'tv_episode' ? i18n.get('UpdateEpisode') : editData?.media_type == 'tv' ? i18n.get('UpdateTvSeries') : i18n.get('UpdateMovie')
    return (
      <Card shadow="sm" padding="lg" radius="md" withBorder>
        <Flex
          mih={150}
          gap="md"
          justify="flex-start"
          align="flex-start"
          direction="row"
          wrap="nowrap"
        >
          <Image
            radius="md"
            h={150}
            w="auto"
            fit="contain"
            src={tmdbResult.poster}
          />
          <Flex
            justify="flex-start"
            align="flex-start"
            direction="column"
            wrap="wrap"
          >
            <Group gap="0">
              <Text size="md">{tmdbResult.title}</Text>
              {tmdbResult.year && (
                <Badge variant="default" mx="sm">{tmdbResult.year.substring(0, 4)}</Badge>
              )}
            </Group>
            {tmdbResult.title !== tmdbResult.original_title && (
              <Group gap="0">
                <Text size="sm" fs="italic">{tmdbResult.original_title}</Text>
                {tmdbResult.original_language && (
                  <Badge variant="default" mx="sm" size="sm" fs="italic">{tmdbResult.original_language}</Badge>)}
              </Group>
            )}
            <Spoiler maxHeight={120} showLabel="..." hideLabel="↥">
              <Text size="sm" c="dimmed">{tmdbResult.overview}</Text>
            </Spoiler>
          </Flex>
        </Flex>
        <Button
          fullWidth
          mt="md"
          radius="md"
          loading={isLoading}
          size="compact-md"
          color={tmdbResult.selected ? 'teal' : undefined}
          onClick={() => setMetadataChange(tmdbResult.id)}
        >
          {buttonText}
        </Button>
      </Card>
    )
  }

  const MetadataResultsForm = ({ i18n, editResults, editData }: { i18n: I18nInterface, editResults: TmdbResult[], editData?: BaseEdit | null }) => {
    return editResults && editResults.length > 0
      ? (
          <Stack>
            <Divider size="md" my="xs" label="↧" labelPosition="center" fz="md" c="var(--mantine-color-text)" />
            {editResults.map((tmdbResult) => {
              return (<MetadataResultCard key={tmdbResult.id} i18n={i18n} tmdbResult={tmdbResult} editData={editData} />)
            })}
          </Stack>
        )
      : undefined
  }

  useEffect(() => {
    if (start) {
      getEditData()
      started()
    }
  }, [start])

  return (
    <Modal
      opened={opened}
      onClose={() => setOpened(false)}
      title={i18n.get('EditMetadata')}
      scrollAreaComponent={ScrollArea.Autosize}
      size="lg"
    >
      <form onSubmit={searchForm.onSubmit(handleSearchForm)}>
        {editData?.folder && (
          <Group>
            <Text size="xs" fw={500} c="dimmed">{i18n.get('Folder')}</Text>
            <Text size="xs">{editData.folder}</Text>
          </Group>
        )}
        {editData?.filename && (
          <Group>
            <Text size="xs" fw={500} c="dimmed">{i18n.get('File')}</Text>
            <Text size="xs">{editData.filename}</Text>
          </Group>
        )}
        <TextInput
          required
          label={i18n.get('Search')}
          name="search"
          {...searchForm.getInputProps('search')}
        />
        <NumberInput
          label={i18n.get('Year')}
          {...searchForm.getInputProps('year')}
        />
        <Button
          fullWidth
          mt="md"
          type="submit"
          leftSection={<IconListSearch size={18} />}
          loading={isLoading}
          size="compact-md"
        >
          {i18n.get('Search')}
        </Button>
      </form>
      <MetadataResultsForm i18n={i18n} editResults={editResults} editData={editData} />
    </Modal>
  )
}

interface BaseEdit {
  filename?: string
  folder?: string
  search?: string
  year?: string
  media_type: string
  episode?: number
  season?: number
}

interface TmdbResult {
  id: number
  title: string
  poster: string
  overview: string
  year: string
  original_language: string
  original_title: string
  selected: boolean
}
