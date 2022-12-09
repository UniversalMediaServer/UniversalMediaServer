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
import { Badge, Box, Breadcrumbs, Button, Card, Center, Container, Grid, Group, Image, List, LoadingOverlay, Paper, ScrollArea, Text, Title, Tooltip } from '@mantine/core';
import { useLocalStorage } from '@mantine/hooks';
import { showNotification } from '@mantine/notifications';
import axios from 'axios';
import { createElement, useContext, useEffect, useRef, useState } from 'react';
import { useParams } from "react-router-dom";
import { ArrowBigLeft, ArrowBigRight, Cast, Download, Folder, Home, Movie, Music, Photo, PlayerPlay, PlaylistAdd, QuestionMark } from 'tabler-icons-react';

import I18nContext from '../../contexts/i18n-context';
import NavbarContext from '../../contexts/navbar-context';
import PlayerEventContext from '../../contexts/player-server-event-context';
import SessionContext from '../../contexts/session-context';
import { havePermission, Permissions } from '../../services/accounts-service';
import { playerApiUrl } from '../../utils';
import { AudioPlayer } from './AudioPlayer';
import { VideoPlayer } from './VideoPlayer';

export const Player = () => {
  const [uuid, setUuid] = useState('');
  const [data, setData] = useState({goal:'',folders:[],breadcrumbs:[],medias:[],useWebControl:false} as BaseBrowse);
  const [loading, setLoading] = useState(false);
  const mainScroll = useRef<HTMLDivElement>(null);
  const i18n = useContext(I18nContext);
  const navbar = useContext(NavbarContext);
  const session = useContext(SessionContext);
  const sse = useContext(PlayerEventContext);
  const { req, id } = useParams();

  const [rtl] = useLocalStorage<boolean>({
    key: 'mantine-rtl',
    defaultValue: false,
  });

  const getUuid = () => {
    if (sessionStorage.getItem('player')) {
      setUuid(sessionStorage.getItem('player') as string);
    } else {
      axios.get(playerApiUrl)
      .then(function (response: any) {
        if (response.data.uuid) {
          sessionStorage.setItem('player', response.data.uuid);
          setUuid(response.data.uuid);
        }
      })
      .catch(function () {
        showNotification({
          id: 'player-data-loading',
          color: 'red',
          title: 'Error',
          message: 'Your player session was not received from the server.',
          autoClose: 3000,
        });
      });
	}
  };

  const hasBreadcrumbs = () => {
    return data.breadcrumbs.length > 1;
  }

  const getBreadcrumbs = () => {
    return hasBreadcrumbs() ? (
      <Paper
        mb="xs"
        shadow="xs"
        p="sm"
        sx={(theme) => ({backgroundColor: theme.colorScheme === 'dark' ? theme.colors.darkTransparent[8] : theme.colors.lightTransparent[0],})}
      >
        <Group>
          <Breadcrumbs
            styles={{separator: {margin: '0'}}}
          >
            {data.breadcrumbs.map((breadcrumb: BaseMedia) => (
              <Button
                key={breadcrumb.id ? breadcrumb.id : breadcrumb.name}
                style={breadcrumb.id ? {fontWeight: 400} : {cursor:'default'}}
                onClick={breadcrumb.id ? () => sse.askBrowseId(breadcrumb.id) : undefined}
                color='gray'
                variant='subtle'
                compact
              >
                {breadcrumb.name === "root" ? (<Home />) : (breadcrumb.name) }
              </Button>)
            )}
          </Breadcrumbs>
        </Group>
      </Paper>
    ) : null;
  }

  const getVideoMediaPlayer = (media: VideoMedia) => {
    return (<Paper>
      <VideoPlayer
        {...{media:media, uuid:uuid, askPlayId:sse.askPlayId}}
      />
    </Paper>);
  }

  const getAudioMediaPlayer = (media: AudioMedia) => {
    return (<Paper>
      <AudioPlayer
        {...{media:media, uuid:uuid}}
      />
    </Paper>);
  }

  const getImageMediaPlayer = (media: ImageMedia) => {
    if (media.delay && media.surroundMedias.next) {
      setTimeout(() => { if (media.surroundMedias.next) sse.askPlayId(media.surroundMedias.next.id); }, media.delay);
    }
    return (
      <Paper>
        <Image
          radius='md'
          src={playerApiUrl + 'image/' + uuid + '/'  + media.id}
          alt={media.name}
        />
      </Paper>);
  }

  const getMediaPlayer = () => {
    if (data.medias.length === 1) {
      switch((data.medias[0] as PlayMedia).mediaType) {
        case 'video':
          return getVideoMediaPlayer(data.medias[0] as VideoMedia);
        case 'audio':
          return getAudioMediaPlayer(data.medias[0] as AudioMedia);
        case 'image':
          return getImageMediaPlayer(data.medias[0] as ImageMedia);
      }
    }
	return null;
  }

  const getMediaIcon = (media: BaseMedia, rtl:boolean) => {
    if (media.icon) {
        switch(media.icon) {
          case 'back':
            return rtl ? ArrowBigRight : ArrowBigLeft;
          case 'video':
            return Movie;
          case 'audio':
            return Music;
          case 'image':
            return Photo;
          case 'folder':
            return Folder;
          default:
            return QuestionMark;
        }
    }
    return null;
  }

  const getMedia = (media: BaseMedia) => {
    let image;
    const icon = getMediaIcon(media, rtl);
    if (icon) {
      image = <Center>{createElement(icon, {size:60})}</Center>;
    } else {
      image = <img src={playerApiUrl + "thumb/" + uuid + "/"  + media.id} alt={media.name} className='thumbnail-image' />;
    }
    return (
      <div
        className='thumbnail-container'
        onClick={() => sse.askReqId(media.id, media.goal ? media.goal : 'browse' )}
        key={media.id}
      >
        {image}
        <div className='thumbnail-text-wrapper'>
          <Text align='left' size='sm' lineClamp={1} className='thumbnail-text'>
            {media.name}
          </Text>
        </div>
      </div>
    )
  }

  const getMedias = () => {
    if (data.goal === 'browse') {
      const mediaList = data.medias.map((media: BaseMedia) => {
        return getMedia(media);
      })
      return (<><div className="media-grid">{mediaList}</div></>);
    }
  }

  const getMediasSelection = (selection: BaseMedia[], title: string) => {
    if (selection && selection.length > 0) {
      const medias = selection.map((media: BaseMedia) => {
        return getMedia(media);
      })
      return (<><Title order={2} mb='md' size='h4' weight={400}>{i18n.get[title]}</Title><div className="front-page-grid">{medias}</div></>);
    }
  }

  const getMediaSelections = () => {
    if (data.mediasSelections) {
      return <>
        {getMediasSelection(data.mediasSelections.recentlyAdded, 'RecentlyAddedVideos')}
        {getMediasSelection(data.mediasSelections.inProgress, 'InProgressVideos')}
        {getMediasSelection(data.mediasSelections.recentlyPlayed, 'RecentlyPlayedVideos')}
        {getMediasSelection(data.mediasSelections.mostPlayed, 'MostPlayedVideos')}
      </>;
    }
  }

  const getMetadataBaseMediaList = (title: string, mediaList?: BaseMedia[]) => {
    if (mediaList && mediaList.length > 0) {
      return (<Group spacing="xs" mt="sm" sx={(theme) => ({color: theme.colorScheme === 'dark' ? 'white' : 'black',})}>
			<Text weight={700}>{i18n.get[title]}: </Text>
        { mediaList.map((media: BaseMedia) => {
          return (
            <Badge
              key={media.id}
              sx={(theme) => ({
                cursor: 'pointer',
                color: theme.colorScheme === 'dark' ? 'white' : 'black',
                backgroundColor: theme.colorScheme === 'dark' ? theme.colors.dark[5] : theme.colors.gray[5],
                '&:hover': {
                  backgroundColor:
                  theme.colorScheme === 'dark' ? theme.colors.dark[9] : theme.colors.gray[0],
                },
              })}
              onClick={() => sse.askBrowseId(media.id)}
            >
              {media.name}
            </Badge>);
        })}
      </Group>);
    }
  }
 
  const getMetadataBaseMedia = (title: string, media?: BaseMedia) => {
    if (media) {
      return getMetadataBaseMediaList(title, [media]);
    }
  }

  const getMetadataString = (title:string, mediaString?:string) => {
    if (mediaString) {
      return (
        <Group mt='sm' sx={(theme) => ({color: theme.colorScheme === 'dark' ? 'white' : 'black',})}>
          <Text weight={700}>{i18n.get[title]}: </Text><Text>{mediaString}</Text>
        </Group>);
    }
  }

  const getMetadataRatingList = (ratingsList?: MediaRating[]) => {
    if (ratingsList && ratingsList.length > 0) {
      return (<>
        <Group mt='sm' sx={(theme) => ({color: theme.colorScheme === 'dark' ? 'white' : 'black',})}>
          <Text weight={700}>{i18n.get['Ratings']}: </Text>
        </Group>
        <List withPadding>
          { ratingsList.map((media: MediaRating) => {
            return (<List.Item key={media.source}>{media.source}: {media.value}</List.Item>);
          })}
        </List>
      </>);
    }
  }

  const media = data.goal === 'show' ? data.medias[0] : data.breadcrumbs[data.breadcrumbs.length - 1];
  const metadata = data.goal === 'show' ? (media as any).metadata : data.metadata;

  function getMetadataImages(metadata?: VideoMetadata, media?: BaseMedia) {
    let logo, poster, imdb;
    if (metadata && metadata.imdbID) {
      imdb = (<a href={'https://www.imdb.com/title/' + metadata.imdbID} rel="noopener noreferrer" target="_blank"><svg aria-hidden="true" width="1.5em" role="img" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 448 512" color="#e0ac00"><path fill="currentColor" d="M350.5 288.7c0 5.4 1.6 14.4-6.2 14.4-1.6 0-3-.8-3.8-2.4-2.2-5.1-1.1-44.1-1.1-44.7 0-3.8-1.1-12.7 4.9-12.7 7.3 0 6.2 7.3 6.2 12.7v32.7zM265 229.9c0-9.7 1.6-16-10.3-16v83.7c12.2.3 10.3-8.7 10.3-18.4v-49.3zM448 80v352c0 26.5-21.5 48-48 48H48c-26.5 0-48-21.5-48-48V80c0-26.5 21.5-48 48-48h352c26.5 0 48 21.5 48 48zM21.3 228.8c-.1.1-.2.3-.3.4h.3v-.4zM97 192H64v127.8h33V192zm113.3 0h-43.1l-7.6 59.9c-2.7-20-5.4-40.1-8.7-59.9h-42.8v127.8h29v-84.5l12.2 84.5h20.6l11.6-86.4v86.4h28.7V192zm86.3 45.3c0-8.1.3-16.8-1.4-24.4-4.3-22.5-31.4-20.9-49-20.9h-24.6v127.8c86.1.1 75 6 75-82.5zm85.9 17.3c0-17.3-.8-30.1-22.2-30.1-8.9 0-14.9 2.7-20.9 9.2V192h-31.7v127.8h29.8l1.9-8.1c5.7 6.8 11.9 9.8 20.9 9.8 19.8 0 22.2-15.2 22.2-30.9v-36z"></path></svg></a>);
    }
    if (metadata && metadata.images && metadata.images.length > 0 && metadata.imageBaseURL) {
      const iso639 = i18n.language.substring(0,2);
      const apiImagesList = metadata.images[0];

      // Set a logo as the heading
      if (apiImagesList && apiImagesList.logos && apiImagesList.logos.length > 0) {
        let logos = apiImagesList.logos.filter(logo => logo.iso_639_1 === iso639);
        if (logos.length === 0) {
          logos = apiImagesList.logos.filter(logo => !logo.iso_639_1);
        }
        if (logos.length === 0) {
          logos = apiImagesList.logos.filter(logo => logo.iso_639_1 === 'en');
        }
        if (logos.length > 0) {
          const betterLogo = logos.reduce((previousValue, currentValue) => {
            return (currentValue.vote_average > previousValue.vote_average) ? currentValue : previousValue;
          });
          logo = (
            <Group pb='xs'>
              <img src={metadata.imageBaseURL + 'w500' + betterLogo.file_path} style={{ maxHeight: '150px', maxWidth: 'calc(100% - 61px)' }} alt={metadata.originalTitle}></img>
              {imdb}
            </Group>
          );
        }
      }
      // Set a poster
      if (apiImagesList && apiImagesList.posters && apiImagesList.posters.length > 0) {
        let posters = apiImagesList.posters.filter(poster => poster.iso_639_1 === iso639);
        if (posters.length === 0) {
          posters = apiImagesList.posters.filter(poster => !poster.iso_639_1);
        }
        if (posters.length === 0) {
          posters = apiImagesList.posters.filter(poster => poster.iso_639_1 === 'en');
        }
        if (posters.length > 0) {
          const betterPoster = posters.reduce((previousValue, currentValue) => {
            return (currentValue.vote_average > previousValue.vote_average) ? currentValue : previousValue;
          });
          poster = (<img className="poster" src={metadata.imageBaseURL + 'w500' + betterPoster.file_path} alt="Poster" />);
        }
      }
    }
    if (!logo && metadata && metadata.originalTitle) {
      logo = (
        <Group pb='xs'>
          <Text pb='xs'>{metadata.originalTitle}</Text>
          {imdb}
        </Group>
      );
    }
    if (!logo && media && media.name) {
      logo = (
        <Group pb='xs'>
          <Text pb='xs'>{media.name}</Text>
          {imdb}
        </Group>
      );
    }
    if (!poster && metadata && metadata.poster) {
      poster = (<img className="poster" src={metadata.poster} />);
    }
    if (!poster && media && media.id) {
      poster = (<img className="poster" src={playerApiUrl + "thumb/" + uuid + "/"  + media.id} />);
    }
    return { logo, poster };
  }

  const images = getMetadataImages(metadata, media);

  /**
   * Sorts through the metadata to select any relevant background
   * image, preload it and fade it in, or if there is no metadata
   * it fades out any previous one and unsets it.
   */
  function setMetadataBackground(metadata: VideoMetadata) {
    let background = '';
    if (metadata && metadata.images && metadata.images.length > 0) {
      const iso639 = i18n.language.substring(0,2);
      const apiImagesList = metadata.images[0];
      // Set the page background and color scheme
      if (apiImagesList && apiImagesList.backdrops && apiImagesList.backdrops.length > 0) {
        let backgrounds = apiImagesList.backdrops.filter(backdrop => !backdrop.iso_639_1);
        if (backgrounds.length === 0) {
          // TODO: Support i18n for backgrounds
          backgrounds = apiImagesList.backdrops.filter(backdrop => backdrop.iso_639_1 === iso639);
        }
        if (backgrounds.length === 0) {
          backgrounds = apiImagesList.backdrops.filter(backdrop => backdrop.iso_639_1 === 'en');
        }
        if (backgrounds.length > 0) {
          const randomBackground = Math.floor(Math.random() * (backgrounds.length));
          background = metadata.imageBaseURL + 'original' + backgrounds[randomBackground].file_path;

          const backgroundImagePreCreation = new (window as any).Image() as HTMLImageElement;
          // @ts-expect-error doesn't think crossorigin exists, using crossOrigin breaks it
          backgroundImagePreCreation.crossorigin = '';
          backgroundImagePreCreation.id = 'backgroundPreload';
          backgroundImagePreCreation.onload = function() {
            document.body.style.backgroundImage='url(' + background + ')';

            const mantineAppShellMain = document.getElementsByClassName('mantine-AppShell-main')[0] as HTMLElement;
            mantineAppShellMain.style.backgroundColor='unset';

            const bodyBackgroundImageScreens = document.getElementsByClassName('bodyBackgroundImageScreen') as HTMLCollectionOf<HTMLElement>;
            if (bodyBackgroundImageScreens && bodyBackgroundImageScreens[0]) {
              const bodyBackgroundImageScreen = bodyBackgroundImageScreens[0];
              // Set the background "screen" to invisible, to give a fade-in effect for the background image
              bodyBackgroundImageScreen.style.backgroundColor = 'rgba(20, 21, 23, 0)';
            }
          }
          setTimeout(function() {
            backgroundImagePreCreation.src = background;
            const backgroundPreloadContainer = document.getElementsByClassName('backgroundPreloadContainer')[0] as HTMLElement;
            // TypeScript doesn't like assigning a non-String with innerHtml even though that's valid so we do the "unknown" trick
            backgroundPreloadContainer.innerHTML = backgroundImagePreCreation as unknown as string;
          });
        }
      }
    } else {
      // reset background image state
      const bodyBackgroundImageScreens = document.getElementsByClassName('bodyBackgroundImageScreen') as HTMLCollectionOf<HTMLElement>;
      if (bodyBackgroundImageScreens && bodyBackgroundImageScreens[0]) {
        const bodyBackgroundImageScreen = bodyBackgroundImageScreens[0];
        // Set the background "screen" to visible, to give a fade-out effect for the background image
        bodyBackgroundImageScreen.style.backgroundColor = 'rgba(20, 21, 23, 0.99)';
      }

      // After the fade out is finished, clear the background image
      setTimeout(() => {
        document.body.style.backgroundImage = 'none';
      }, 500);
    }
  }

  const getPlayControls = () => {
    if (data.goal === 'show') {
      return (
        <Button.Group>
          <Button variant='default' compact leftIcon={<PlayerPlay size={14} />} onClick={() => sse.askPlayId(data.medias[0].id)}>{i18n.get['Play']}</Button>
          {data.useWebControl && (
            <Tooltip withinPortal label={i18n.get['PlayOnAnotherRenderer']}>
              <Button variant='default' disabled compact onClick={() => {}}><Cast size={14} /></Button>
            </Tooltip>
          )}
          <Tooltip withinPortal label={i18n.get['AddToPlaylist']}>
            <Button variant='default' disabled compact onClick={() => {}}><PlaylistAdd size={14} /></Button>
          </Tooltip>
          {((data.medias[0]) as PlayMedia).isDownload && (
            <Tooltip withinPortal label={i18n.get['Download']}>
              <Button variant='default' compact onClick={() => window.open(playerApiUrl + 'download/' + uuid + '/' + data.medias[0].id ,'_blank')}><Download size={14} /></Button>
            </Tooltip>
          )}
        </Button.Group>
      )
    }
  }

  const getMetadataGridCol = () => {
    if (metadata) {
      return (<>
        <Grid mb="md">
          <Grid.Col span={12}>
            <Grid columns={20} justify="center">
              <Grid.Col span={6}>
                { images.poster }
              </Grid.Col>
              <Grid.Col span={12}>
                <Card shadow="sm" p="lg" radius="md" sx={(theme) => ({backgroundColor: theme.colorScheme === 'dark' ? theme.colors.darkTransparent[8] : theme.colors.lightTransparent[0],})}>
                  { images.logo }
                  { getPlayControls() }
                  { getMetadataBaseMediaList('Actors', metadata.actors) }
                  { getMetadataString('Awards', metadata.awards) }
                  { getMetadataBaseMediaList('Country', metadata.countries) }
                  { getMetadataBaseMediaList('Director', metadata.directors) }
                  { getMetadataBaseMediaList('Genres', metadata.genres) }
                  { getMetadataString('Plot', metadata.plot) }
                  { getMetadataBaseMedia('Rated', metadata.rated) }
                  { getMetadataRatingList(metadata.ratings) }
                  { getMetadataString('YearStarted', metadata.startYear) }
                  { getMetadataString('TotalSeasons', metadata.totalSeasons) }
                </Card>
              </Grid.Col>
            </Grid>
          </Grid.Col>
        </Grid>
      </>);
    }
  }

  const getMediaGridCol = () => {
    if (media) {
      return (<>
        <Grid mb="md">
          <Grid.Col span={12}>
            <Grid columns={20} justify='center'>
              <Grid.Col span={6}>
                <Image style={{ maxHeight: 500 }} radius='md' fit='contain' src={playerApiUrl + "thumb/" + uuid + "/"  + media.id} />
              </Grid.Col>
              <Grid.Col span={12}  >
                <Card shadow='sm' p='lg' radius='md' sx={(theme) => ({backgroundColor: theme.colorScheme === 'dark' ? theme.colors.darkTransparent[8] : theme.colors.lightTransparent[0],})}>
                  <Text pb='xs'>{media.name}</Text>
                  { getPlayControls() }
                </Card>
              </Grid.Col>
            </Grid>
          </Grid.Col>
        </Grid>
      </>);
    }
  }

  const getShowMetadata = () => {
    if (data.goal === 'show') {
      return metadata ? getMetadataGridCol() : getMediaGridCol();
    }
  }

  const getBrowseMetadata = () => {
    if ((data.goal === 'browse') && metadata) {
      return getMetadataGridCol();
    }
  }

  useEffect(() => {
    if (id && req) {
      sse.askReqId(id, req);
    }
  }, [req, id]);

  useEffect(() => {
    ((!session.authenticate || havePermission(session, Permissions.web_player_browse)) && getUuid())
  }, [session]);

  useEffect(() => {
    if (uuid && sse.reqType) {
      setLoading(true);
      axios.post(playerApiUrl + sse.reqType, { uuid: uuid, id: sse.reqId })
        .then(function (response: any) {
          setData(response.data);
          const mediaTemp = response.data.goal === 'show' ? response.data.medias[0] : response.data.breadcrumbs[response.data.breadcrumbs.length - 1];
          setMetadataBackground(
            response.data.goal === 'show' ? (mediaTemp as any).metadata as VideoMetadata : response.data.metadata,
          );
          window.scrollTo(0,0);
          const url = '/player/' + sse.reqType + '/' + sse.reqId;
          if (url !== history.state) {
            window.history.pushState(url, '', url);
          }
        })
        .catch(function () {
          showNotification({
            id: 'player-data-loading',
            color: 'red',
            title: 'Error',
            message: 'Your browse data was not received from the server.',
            autoClose: 3000,
          });
        })
        .then(function () {
          setLoading(false);
        });
    }
  }, [uuid, sse.reqType, sse.reqId]);

  useEffect(() => {
    const getFolderIcon = (folder:BaseMedia, rtl:boolean) => {
      const icon = getMediaIcon(folder, rtl);
      if (icon) {
        return createElement(icon, {size:20});
      }
      return <Folder size={20} />
    }
    const getFoldersButtons = () => {
      return data.folders.map((folder) => {
        return (
          <Button
            key={folder.id}
            onClick={() => sse.askBrowseId(folder.id)}
            color='gray'
            variant='subtle'
            compact
            styles={{inner: {justifyContent: 'normal'}, root:{fontWeight: 400, '&:hover':{fontWeight: 600}}}}
            leftIcon = {getFolderIcon(folder, rtl)}
          >
            {folder.name}
          </Button>
        );
      });
    }
    const getMediaLibraryFolders = () => {
      return data.mediaLibraryFolders?.map((folder) => {
        return (
          <Button
            key={folder.id}
            onClick={() => sse.askBrowseId(folder.id)}
            color='gray'
            variant='subtle'
            compact
            styles={{inner: {justifyContent: 'normal'}, root:{fontWeight: 400, '&:hover':{fontWeight: 600}}}}
            leftIcon = {getFolderIcon(folder, rtl)}
          >
            {folder.name}
          </Button>
       );
      });
    }
    const getNavFolders = () => {
      if (data.mediaLibraryFolders && data.mediaLibraryFolders.length > 0) {
        return (<><div>{i18n.get['MediaLibrary']}</div>{getMediaLibraryFolders()}<div>{i18n.get['YourFolders']}</div>{getFoldersButtons()}</>);
      } else {
        return getFoldersButtons();
      }
    }
    navbar.setValue(getNavFolders());
    // eslint-disable-next-line
  }, [data, i18n.get, navbar.setValue]);

  return (!session.authenticate || havePermission(session, Permissions.web_player_browse)) ? (
    <Box>
      <LoadingOverlay visible={loading} />
      {getBreadcrumbs()}
      <ScrollArea offsetScrollbars viewportRef={mainScroll}>
        {
          data.goal === 'play' ?
            <Paper>
              {getMediaPlayer()}
            </Paper>
          : data.goal === 'show' ? (
            getShowMetadata()
          ) : (
            <span>
              {getMediaSelections()}
              {getBrowseMetadata()}
              {getMedias()}
            </span>
          )
        }
      </ScrollArea>
      <div className="backgroundPreloadContainer">
        <img id="backgroundPreload" crossOrigin="" />
      </div>
    </Box>
  ) : (
    <Box sx={{ maxWidth: 1024 }} mx="auto">
      <Text color="red">{i18n.get['YouDontHaveAccessArea']}</Text>
    </Box>
  );
};

export default Player;

export interface BaseMedia {
  goal?: string,
  icon?: string,
  id: string,
  name: string,
}

interface MediasSelections {
  recentlyAdded: BaseMedia[],
  recentlyPlayed: BaseMedia[],
  inProgress: BaseMedia[],
  mostPlayed: BaseMedia[],
}

interface BaseBrowse {
  breadcrumbs: BaseMedia[],
  folders: BaseMedia[],
  goal: string,
  medias: BaseMedia[],
  mediaLibraryFolders?: BaseMedia[],
  mediasSelections?: MediasSelections,
  metadata?: VideoMetadata,
  useWebControl: boolean,
}

interface SurroundMedias {
  prev?: BaseMedia,
  next?: BaseMedia,
}

interface PlayMedia extends BaseMedia {
  autoContinue: boolean,
  isDownload: boolean,
  isDynamicPls: boolean,
  mediaType: string,
  surroundMedias: SurroundMedias,
}

interface MediaRating {
  source: string,
  value: string,
}

interface VideoMetadata {
  actors?: BaseMedia[],
  awards?: string,
  countries?: BaseMedia[],
  createdBy?: string,
  credits?: string,
  directors?: BaseMedia[],
  endYear?: string,
  externalIDs?: string,
  firstAirDate?: string,
  genres?: BaseMedia[],
  homepage?: string,
  images?: VideoMetadataImages[],
  imageBaseURL: string,
  imdbID?: string,
  inProduction?: boolean,
  languages?: string,
  lastAirDate?: string,
  networks?: string,
  numberOfEpisodes?: number,
  numberOfSeasons?: string,
  originCountry?: string,
  originalLanguage?: string,
  originalTitle?: string,
  plot?: string,
  poster?: string,
  productionCompanies?: string,
  productionCountries?: string,
  rated?: BaseMedia,
  rating?: string,
  ratings?: MediaRating[],
  seasons?: string,
  seriesType?: string,
  spokenLanguages?: string,
  startYear?: string,
  status?: string,
  tagline?: string,
  totalSeasons?: number,
  votes?: string,
}

interface VideoMetadataImages {
	backdrops?: VideoMetadataImage[],
	logos?: VideoMetadataImage[],
	posters?: VideoMetadataImage[],
}

interface VideoMetadataImage {
  aspect_ratio?: number,
  height?: number,
  iso_639_1?: string,
  file_path?: string,
  vote_average: number,
  vote_count?: number,
  width?: number,
}

export interface VideoMedia extends PlayMedia {
  height: number,
  isVideoWithChapters: boolean,
  metadata?: VideoMetadata,
  mime: string,
  resumePosition?: number,
  sub?: string,
  width: number,
}

export interface AudioMedia extends PlayMedia {
  isNativeAudio:boolean,
  mime:string,
  width:number,
  height:number,
}

interface ImageMedia extends PlayMedia {
  delay?:number,
}
