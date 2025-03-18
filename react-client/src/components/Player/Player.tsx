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
import { Box, Breadcrumbs, Button, Group, Image, LoadingOverlay, Paper, ScrollArea, Text } from '@mantine/core'
import { IconHome } from '@tabler/icons-react'
import axios from 'axios'
import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'

import { I18nInterface } from '../../services/i18n-service'
import { PlayerEventInterface } from '../../services/player-server-event-service'
import { SessionInterface, UmsPermission } from '../../services/session-service'
import { AudioMedia, BaseBrowse, BaseMedia, ImageMedia, PlayMedia, VideoMedia, VideoMetadata } from '../../services/player-service'
import { playerApiUrl } from '../../utils'
import VideoJsPlayer from './VideoJsPlayer'
import { showError } from '../../utils/notifications'
import MediaFolders from './MediaFolders'
import MediaSelections from './MediaSelections'
import PlayerNavbar from './PlayerNavbar'
import MediaGrid from './MediaGrid'
import MediaPanel from './MediaPanel'

const Player = ({ i18n, session, sse }: { i18n: I18nInterface, session: SessionInterface, sse: PlayerEventInterface }) => {
  const [data, setData] = useState({ goal: '', folders: [], breadcrumbs: [], medias: [], useWebControl: false } as BaseBrowse)
  const [loading, setLoading] = useState(false)
  const { req, id } = useParams()

  useEffect(() => {
    session.stopSse()
    session.startPlayerSse()
  }, [])

  // set the document title to last breadCrumbs Name else default
  useEffect(() => {
    if (hasBreadcrumbs()) {
      const lastBreadCrumb = data.breadcrumbs[data.breadcrumbs.length - 1]
      const documentTitle = i18n.getLocalizedName(lastBreadCrumb.name)
      session.setDocumentTitle(documentTitle)
    }
    else {
      session.setDocumentTitle('')
    }
  }, [data.breadcrumbs])

  const refreshPage = () => {
    if (sse.uuid && sse.reqType) {
      setLoading(true)
      axios.post(playerApiUrl + sse.reqType, { uuid: sse.uuid, id: sse.reqId, lang: i18n.language }, { headers: { Player: sse.uuid } })
        .then(function (response: any) {
          setData(response.data)
          const mediaTemp = response.data.goal === 'show' ? response.data.medias[0] : response.data.breadcrumbs[response.data.breadcrumbs.length - 1]
          setMetadataBackground(
            response.data.goal === 'show' ? (mediaTemp as any).metadata as VideoMetadata : response.data.metadata,
          )
          window.scrollTo(0, 0)
          const url = '/player/' + sse.reqType + '/' + sse.reqId
          if (url !== history.state) {
            window.history.pushState(url, '', url)
          }
        })
        .catch(function () {
          showError({
            id: 'player-data-loading',
            title: i18n.get('Error'),
            message: 'Your browse data was not received from the server.',
          })
        })
        .then(function () {
          setLoading(false)
        })
    }
  }

  const hasBreadcrumbs = () => {
    return data.breadcrumbs.length > 1
  }

  const PlayerBreadcrumbs = () => {
    return hasBreadcrumbs()
      ? (
          <Paper
            mb="xs"
            shadow="xs"
            p="sm"
            bg="transparentBg"
          >
            <Group>
              <Breadcrumbs
                separatorMargin={0}
                styles={{ root: { flexWrap: 'wrap' } }}
              >
                {data.breadcrumbs.map((breadcrumb: BaseMedia) => breadcrumb.id
                  ? (
                      <Button
                        key={breadcrumb.id}
                        style={breadcrumb.id ? { fontWeight: 400 } : { cursor: 'default' }}
                        onClick={breadcrumb.id ? () => sse.askBrowseId(breadcrumb.id) : undefined}
                        color="gray"
                        variant="subtle"
                        size="compact-md"
                      >
                        {breadcrumb.name === 'root' ? (<IconHome />) : (i18n.getLocalizedName(breadcrumb.name))}
                      </Button>
                    )
                  : (
                      <LastBreadcrumbButton key={-1} breadcrumb={breadcrumb} />
                    ),
                )}
              </Breadcrumbs>
            </Group>
          </Paper>
        )
      : undefined
  }

  const LastBreadcrumbButton = ({ breadcrumb }: { breadcrumb: BaseMedia }) => (
    <Button
      style={{ cursor: 'default' }}
      color="gray"
      variant="subtle"
      size="compact-md"
    >
      {i18n.getLocalizedName(breadcrumb.name)}
    </Button>
  )

  const VideoJsMediaPlayer = ({ sse, media }: { sse: PlayerEventInterface, media: VideoMedia | AudioMedia }) => {
    return (
      <Paper>
        <VideoJsPlayer
          {...{ media: media, uuid: sse.uuid, askPlayId: sse.askPlayId }}
        />
      </Paper>
    )
  }

  const ImageMediaPlayer = ({ sse, media }: { sse: PlayerEventInterface, media: ImageMedia }) => {
    if (media.delay && media.surroundMedias.next) {
      setTimeout(() => {
        if (media.surroundMedias.next) {
          sse.askPlayId(media.surroundMedias.next.id)
        }
      }, media.delay)
    }
    return (
      <Paper>
        <Image
          radius="md"
          src={playerApiUrl + 'image/' + sse.uuid + '/' + media.id}
          alt={media.name}
        />
      </Paper>
    )
  }

  const MediaPlayer = ({ sse, data }: { sse: PlayerEventInterface, data: BaseBrowse }) => {
    if (data.medias.length === 1) {
      switch ((data.medias[0] as PlayMedia).mediaType) {
        case 'video':
          return <VideoJsMediaPlayer media={data.medias[0] as VideoMedia} sse={sse} />
        case 'audio':
          return <VideoJsMediaPlayer media={data.medias[0] as AudioMedia} sse={sse} />
        case 'image':
          return <ImageMediaPlayer media={data.medias[0] as ImageMedia} sse={sse} />
      }
    }
    return undefined
  }

  /**
   * Sorts through the metadata to select any relevant background
   * image, preload it and fade it in, or if there is no metadata
   * it fades out any previous one and unsets it.
   */
  function setMetadataBackground(metadata: VideoMetadata) {
    let background = ''
    if (metadata && metadata.images && metadata.images.length > 0) {
      const iso639 = i18n.language.substring(0, 2)
      const apiImagesList = metadata.images[0]
      // Set the page background and color scheme
      if (apiImagesList && apiImagesList.backdrops && apiImagesList.backdrops.length > 0) {
        let backgrounds = apiImagesList.backdrops.filter(backdrop => !backdrop.iso_639_1)
        if (backgrounds.length === 0) {
          // TODO: Support i18n for backgrounds
          backgrounds = apiImagesList.backdrops.filter(backdrop => backdrop.iso_639_1 === iso639)
        }
        if (backgrounds.length === 0) {
          backgrounds = apiImagesList.backdrops.filter(backdrop => backdrop.iso_639_1 === 'en')
        }
        if (backgrounds.length > 0) {
          const randomBackground = Math.floor(Math.random() * (backgrounds.length))
          background = metadata.imageBaseURL + 'original' + backgrounds[randomBackground].file_path

          const backgroundImagePreCreation = new (window as any).Image() as HTMLImageElement
          // @ts-expect-error doesn't think crossorigin exists, using crossOrigin breaks it
          backgroundImagePreCreation.crossorigin = ''
          backgroundImagePreCreation.id = 'backgroundPreload'
          backgroundImagePreCreation.onload = function () {
            document.body.style.backgroundImage = 'url(' + background + ')'

            const mantineAppShellMain = document.getElementsByClassName('mantine-AppShell-main')[0] as HTMLElement
            mantineAppShellMain.style.backgroundColor = 'unset'

            const bodyBackgroundImageScreens = document.getElementsByClassName('bodyBackgroundImageScreen') as HTMLCollectionOf<HTMLElement>
            if (bodyBackgroundImageScreens && bodyBackgroundImageScreens[0]) {
              const bodyBackgroundImageScreen = bodyBackgroundImageScreens[0]
              // Set the background "screen" to invisible, to give a fade-in effect for the background image
              bodyBackgroundImageScreen.style.backgroundColor = 'rgba(20, 21, 23, 0)'
            }
          }
          setTimeout(function () {
            backgroundImagePreCreation.src = background
            const backgroundPreloadContainer = document.getElementsByClassName('backgroundPreloadContainer')[0] as HTMLElement
            // TypeScript doesn't like assigning a non-String with innerHtml even though that's valid so we do the "unknown" trick
            backgroundPreloadContainer.innerHTML = backgroundImagePreCreation as unknown as string
          })
        }
      }
    }
    else {
      // reset background image state
      const bodyBackgroundImageScreens = document.getElementsByClassName('bodyBackgroundImageScreen') as HTMLCollectionOf<HTMLElement>
      if (bodyBackgroundImageScreens && bodyBackgroundImageScreens[0]) {
        const bodyBackgroundImageScreen = bodyBackgroundImageScreens[0]
        // Set the background "screen" to visible, to give a fade-out effect for the background image
        bodyBackgroundImageScreen.style.backgroundColor = 'rgba(20, 21, 23, 0.99)'
      }

      // After the fade out is finished, clear the background image
      setTimeout(() => {
        document.body.style.backgroundImage = 'none'
      }, 500)
    }
  }

  useEffect(() => {
    if (id && req) {
      sse.askReqId(id, req)
    }
  }, [req, id])

  useEffect(() => {
    refreshPage()
  }, [sse.uuid, sse.reqType, sse.reqId, i18n.language])

  useEffect(() => {
    session.setNavbarValue(session.playerNavbar ? <PlayerNavbar data={data} i18n={i18n} sse={sse} /> : undefined)
  }, [data, i18n.get, session.playerNavbar])

  return (!session.authenticate || session.havePermission(UmsPermission.web_player_browse))
    ? (
        <Box>
          <LoadingOverlay visible={loading} overlayProps={{ fixed: true }} loaderProps={{ style: { position: 'fixed' } }} />
          <PlayerBreadcrumbs />
          <ScrollArea>
            {
              data.goal === 'play'
                ? (
                    <Paper>
                      <MediaPlayer sse={sse} data={data} />
                    </Paper>
                  )
                : data.goal === 'show'
                  ? (
                      <MediaPanel i18n={i18n} sse={sse} data={data} refreshPage={refreshPage} setLoading={setLoading} />
                    )
                  : (
                      <span>
                        <MediaSelections i18n={i18n} session={session} sse={sse} data={data} />
                        <MediaPanel i18n={i18n} sse={sse} data={data} refreshPage={refreshPage} setLoading={setLoading} />
                        <MediaFolders i18n={i18n} session={session} sse={sse} data={data} />
                        <MediaGrid i18n={i18n} session={session} sse={sse} mediaArray={data.medias} />
                      </span>
                    )
            }
          </ScrollArea>
          <div className="backgroundPreloadContainer">
            <img id="backgroundPreload" crossOrigin="" />
          </div>
        </Box>
      )
    : (
        <Box style={{ maxWidth: 1024 }} mx="auto">
          <Text c="red">{i18n.get('YouDontHaveAccessArea')}</Text>
        </Box>
      )
}

export default Player
