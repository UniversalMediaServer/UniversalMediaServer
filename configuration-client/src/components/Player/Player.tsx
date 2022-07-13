import { Box, Breadcrumbs, Button, Card, Center, Grid, Group, Image, Paper, ScrollArea, Stack, Text } from '@mantine/core';
import { useLocalStorage } from '@mantine/hooks';
import { showNotification } from '@mantine/notifications';
import axios from 'axios';
import { createElement, useContext, useEffect, useState } from 'react';
import { ArrowBigLeft, ArrowBigRight, Folder, Home, Movie, Music, Photo, QuestionMark } from 'tabler-icons-react';

import { AudioPlayer } from './AudioPlayer';
import { VideoPlayer } from './VideoPlayer';
import I18nContext from '../../contexts/i18n-context';

const Player = () => {
  const baseUrl = '/v1/api/player/';
  const [token, setToken] = useState('');
  const [data, setData] = useState({goal:'',folders:[],breadcrumbs:[],medias:[]} as BaseBrowse);
  const [browseId, setBrowseId] = useState('0');
  const [playId, setPlayId] = useState('');
  const i18n = useContext(I18nContext);

  const [rtl] = useLocalStorage<boolean>({
    key: 'mantine-rtl',
    defaultValue: false,
  });

  const getToken = () => {
    if (sessionStorage.getItem('player')) {
      setToken(sessionStorage.getItem('player') as string);
    } else {
      axios.get(baseUrl)
      .then(function (response: any) {
        sessionStorage.setItem('player', response.data.token);
        setToken(response.data.token);
      })
      .catch(function (error: Error) {
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

  const askBrowseId = (id:string) => {
	  setPlayId('');
	  setBrowseId(id);
  }

  const askPlayId = (id:string) => {
	  setBrowseId('');
	  setPlayId(id);
  }
  const getMediaLibraryFolders = () => {
    return data.mediaLibraryFolders?.map((folder) => {
      return (
        <Button
          onClick={() => askBrowseId(folder.id)}
          color='gray'
          variant="subtle"
          compact
          styles={{inner: {justifyContent: 'normal'}, root:{fontWeight: 400, '&:hover':{fontWeight: 600}}}}
          leftIcon = {getFolderIcon(folder, rtl)}
        >
          {folder.name}
        </Button>
     );
	});
  }
  const getFolders = () => {
    if (data.mediaLibraryFolders && data.mediaLibraryFolders.length > 0) {
		return (<><span>{i18n.get['MediaLibrary']}</span>{getMediaLibraryFolders()}<span>{i18n.get['YourFolders']}</span>{getFoldersButtons()}</>);
    } else {
		return getFoldersButtons();
	}
  }
  const getFoldersButtons = () => {
    return data.folders.map((folder) => {
      return (
        <Button
          onClick={() => askBrowseId(folder.id)}
          color='gray'
          variant="subtle"
          compact
          styles={{inner: {justifyContent: 'normal'}, root:{fontWeight: 400, '&:hover':{fontWeight: 600}}}}
          leftIcon = {getFolderIcon(folder, rtl)}
        >
          {folder.name}
        </Button>
     );
	});
  }

  const getFolderIcon = (folder:BaseMedia, rtl:boolean) => {
    let icon = getMediaIcon(folder, rtl);
    if (icon) {
      return createElement(icon, {size:20});
    }
	return <Folder size={20} />
  }

  const hasBreadcrumbs = () => {
    return data.breadcrumbs.length > 1;
  }

  const getBreadcrumbs = () => {
    return hasBreadcrumbs() ? (
      <Paper mb='xs'>
        <Group>
          <Breadcrumbs
            styles={{separator: {margin: '0'}}}
          >
            {data.breadcrumbs.map((breadcrumb: BaseMedia) => (
			  <Button
                style={breadcrumb.id ? {fontWeight: 400} : {cursor:'default'}}
                onClick={breadcrumb.id ? () => askBrowseId(breadcrumb.id) : undefined}
                color='gray'
                variant="subtle"
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
        {...{media:media , baseUrl:baseUrl, token:token}}
      />
    </Paper>);
  }

  const getAudioMediaPlayer = (media: AudioMedia) => {
    return (<Paper>
      <AudioPlayer
        {...{media:media , baseUrl:baseUrl, token:token}}
      />
    </Paper>);
  }

  const getImageMediaPlayer = (media: ImageMedia) => {
    if (media.delay && media.surroundMedias.next) {
      setTimeout(() => { if (media.surroundMedias.next) askPlayId(media.surroundMedias.next.id); }, media.delay);
    }
    return (
      <Paper>
        <Image
          radius="md"
          src={baseUrl + "image/" + token + "/"  + media.id}
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
    let isPlay = media.goal==="play";
	let image;
    let icon = getMediaIcon(media, rtl);
    if (icon) {
      image = <Center>{createElement(icon, {size:60})}</Center>;
    } else {
      image = <Image src={baseUrl + "thumb/" + token + "/"  + media.id} fit="contain" height={160} alt={media.name} />;
    }
    return (
      <Grid.Col span={3}>
        <Card shadow="sm" p="lg"
          onClick={() => isPlay ? askPlayId(media.id) : askBrowseId(media.id)}
          style={{cursor:'pointer'}}
        >
          <Card.Section>
            {image}
          </Card.Section>
          <Text align="center" size="sm">
            {media.name}
          </Text>
        </Card>
      </Grid.Col>
    )
  }

  const getMedias = () => {
    return data.medias.map((media: BaseMedia) => {
      return getMedia(media);
	})
  }

  const getMediasSelection = (selection:BaseMedia[], title:string) => {
    if (selection.length > 0) {
      let medias = selection.map((media: BaseMedia) => {
        return getMedia(media);
      })
      return (<><Grid.Col span={12}><Card><Text align="center" size="lg">{i18n.get[title]}</Text></Card></Grid.Col>{medias}</>);
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

  useEffect(() => {
    getToken();
  }, []);

  useEffect(() => {
    if (token && browseId) {
      axios.post(baseUrl + 'browse', {token:token,id:browseId})
      .then(function (response: any) {
        setData(response.data);
      })
      .catch(function (error: Error) {
        showNotification({
          id: 'player-data-loading',
          color: 'red',
          title: 'Error',
          message: 'Your browse data was not received from the server.',
          autoClose: 3000,
        });
      });
    }
  }, [token, browseId]);

  useEffect(() => {
    if (token && playId) {
      axios.post(baseUrl + 'play', {token:token,id:playId})
      .then(function (response: any) {
        setData(response.data);
      })
      .catch(function (error: Error) {
        showNotification({
          id: 'player-data-loading',
          color: 'red',
          title: 'Error',
          message: 'Your play data was not received from the server.',
          autoClose: 3000,
        });
      });
    }
  }, [token, playId]);

  return (
    <Box mx="auto">
      <Grid>
        <Grid.Col md={3} style={{height: 'calc(100vh - 60px)'}}>
		<Paper>
          <ScrollArea style={{height: 'calc(100vh - 60px)'}}>
            <Stack justify="flex-start" spacing={0}>
              {getFolders()}
            </Stack>
          </ScrollArea>
		  </Paper>
        </Grid.Col>
        <Grid.Col md={9} style={{height: 'calc(100vh - 60px)'}}>
          {getBreadcrumbs()}
          <ScrollArea offsetScrollbars style={{height: hasBreadcrumbs() ? 'calc(100vh - 100px)' : 'calc(100vh - 60px)'}}>
            {data.goal === 'play' ?
              <Paper>
                {getMediaPlayer()}
			  </Paper>
             : (
              <Grid>
                {getMediaSelections()}
                {getMedias()}
              </Grid>
            )}
          </ScrollArea>
        </Grid.Col>
      </Grid>
    </Box>
  );
};

export default Player;

interface BaseMedia {
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
}

interface SurroundMedias {
  prev?: BaseMedia,
  next?: BaseMedia,
}

interface PlayMedia extends BaseMedia {
  autoContinue: boolean,
  isDynamicPls: boolean,
  mediaType: string,
  surroundMedias: SurroundMedias,
  useWebControl: boolean,
}

interface MediaRating {
  source: string,
  value: string,
}

interface VideoMetadatas {
  actors?: BaseMedia[],
  awards: string,
  country?: BaseMedia,
  createdBy?: string,
  credits?: string,
  director?: BaseMedia,
  externalIDs?: string,
  firstAirDate?: string,
  genres?: BaseMedia[],
  homepage?: string,
  images?: string,
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
  ratings?: MediaRating[],
  seasons?: string,
  seriesType?: string,
  spokenLanguages?: string,
  startYear?: string,
  status?: string,
  tagline?: string,
  totalSeasons?: number,
}

export interface VideoMedia extends PlayMedia {
  height: number,
  isVideoWithChapters: boolean,
  metadatas?: VideoMetadatas,
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
