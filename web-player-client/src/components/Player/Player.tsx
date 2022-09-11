import { Badge, Box, Breadcrumbs, Button, Card, Center, Grid, Group, Image, List, LoadingOverlay, Paper, ScrollArea, Stack, Text } from '@mantine/core';
import { useLocalStorage } from '@mantine/hooks';
import { showNotification } from '@mantine/notifications';
import axios from 'axios';
import { createElement, useContext, useEffect, useState } from 'react';
import { ArrowBigLeft, ArrowBigRight, Folder, Home, Movie, Music, Photo, QuestionMark } from 'tabler-icons-react';

import I18nContext from '../../contexts/i18n-context';
import SessionContext from '../../contexts/session-context';
import { havePermission, Permissions } from '../../services/accounts-service';
import { AudioPlayer } from './AudioPlayer';
import { VideoPlayer } from './VideoPlayer';

export const Player = () => {
  const baseUrl = '/v1/api/player/';
  const [token, setToken] = useState('');
  const [data, setData] = useState({goal:'',folders:[],breadcrumbs:[],medias:[],useWebControl:false} as BaseBrowse);
  const [browseId, setBrowseId] = useState('0');
  const [playId, setPlayId] = useState('');
  const [loading, setLoading] = useState(false);
  const i18n = useContext(I18nContext);
  const session = useContext(SessionContext);

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
        {...{media:media , baseUrl:baseUrl, token:token, askPlayId:askPlayId}}
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
    if (selection && selection.length > 0) {
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

  const getMetadataBaseMediaList = (title: string, mediaList?: BaseMedia[]) => {
    if (mediaList && mediaList.length > 0) {
      return (<Group spacing="xs" mt="sm" sx={(theme) => ({color: theme.colorScheme === 'dark' ? 'white' : 'black',})}>
			<Text weight={700}>{i18n.get[title]}: </Text>
        { mediaList.map((media: BaseMedia) => {
          return (
            <Badge
              sx={(theme) => ({
                cursor: 'pointer',
                color: theme.colorScheme === 'dark' ? 'white' : 'black',
				backgroundColor: theme.colorScheme === 'dark' ? theme.colors.dark[5] : theme.colors.gray[5],
                '&:hover': {
                  backgroundColor:
                  theme.colorScheme === 'dark' ? theme.colors.dark[9] : theme.colors.gray[0],
                },
              })}
              onClick={() => askBrowseId(media.id)}
            >
              {media.name}
            </Badge>);
        })}
      </Group>);
    }
  }

  const getMetadataString = (title:string, mediaString?:string) => {
    if (mediaString) {
      return (<Group mt="sm" sx={(theme) => ({color: theme.colorScheme === 'dark' ? 'white' : 'black',})}><Text weight={700}>{i18n.get[title]}: </Text><Text>{mediaString}</Text></Group>);
    }
  }

  const getMetadataRatingList = (ratingsList?: MediaRating[]) => {
    if (ratingsList && ratingsList.length > 0) {
      return (<><Group mt="sm" sx={(theme) => ({color: theme.colorScheme === 'dark' ? 'white' : 'black',})}>
	  <Text weight={700}>{i18n.get['Ratings']}: </Text></Group>
        <List withPadding>
          { ratingsList.map((media: MediaRating) => {
            return (<List.Item>{media.source}: {media.value}</List.Item>);
          })}
        </List>
      </>);
    }
  }

  function getMetadataImages() {
    let background, logo, poster;
    if (data.metadata && data.metadata.images && data.metadata.images.length > 0) {
      const iso639 = i18n.language.substring(0,2);
      let apiImagesList = data.metadata.images[0];
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
          var randomBackground = Math.floor(Math.random() * (backgrounds.length));
          background = data.metadata.imageBaseURL + 'original' + backgrounds[randomBackground].file_path;
        }
      }
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
          let betterLogo = logos.reduce((previousValue, currentValue) => {
            return (currentValue.vote_average > previousValue.vote_average) ? currentValue : previousValue;
          });
          logo = (
		  <div>
			<img src={data.metadata.imageBaseURL + 'w500' + betterLogo.file_path} style={{ maxHeight: '150px', maxWidth: 'calc(100% - 61px)' }} alt={data.metadata.originalTitle}></img>
		  </div>
		  );
        }
      }
      if (!logo) {
        logo = (<Text>{data.metadata.originalTitle}</Text>);
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
          let betterPoster = posters.reduce((previousValue, currentValue) => {
            return (currentValue.vote_average > previousValue.vote_average) ? currentValue : previousValue;
          });
          poster = (<Image style={{ maxHeight: 500 }} radius="md" fit="contain" src={data.metadata.imageBaseURL + 'w500' + betterPoster.file_path} ></Image>);
        }
      }
      if (!poster && data.metadata.poster) {
        poster = (<Image style={{ maxHeight: 500 }} radius="md" fit="contain" src={data.metadata.poster} />);
      }
    }
    return {background:background, logo:logo, poster:poster};
  }

  const images = getMetadataImages();
  const getBrowseMetadata = () => {
	  if (data.goal === 'browse' && data.metadata) {
          
          return (<>
            <Grid.Col span={12}>
                <Grid columns={20} justify="center">
                  <Grid.Col span={6}>
                    { images.poster }
                  </Grid.Col>
                  <Grid.Col span={12}  >
                    <Card shadow="sm" p="lg" radius="md"  sx={(theme) => ({backgroundColor: theme.colorScheme === 'dark' ? 'rgba(0, 0, 0, 0.6)' : 'rgba(255, 255, 255, 0.6)',})}>
                      { images.logo }
                      { getMetadataBaseMediaList('Actors', data.metadata.actors) }
                      { getMetadataString('Awards', data.metadata.awards) }
                      { getMetadataBaseMediaList('Country', data.metadata.countries) }
                      { getMetadataBaseMediaList('Director', data.metadata.directors) }
                      { getMetadataBaseMediaList('Genres', data.metadata.genres) }
                      { getMetadataString('Plot', data.metadata.plot) }
                      { getMetadataRatingList(data.metadata.ratings) }
                    </Card>
                  </Grid.Col>
                </Grid>
            </Grid.Col>
          </>);
      }
  }

  useEffect(() => {
    ((!session.authenticate || havePermission(session, Permissions.web_player_browse) && getToken()))
  }, [session]);

  useEffect(() => {
    if (token && browseId) {
      setLoading(true);
      axios.post(baseUrl + 'browse', {token:token,id:browseId})
      .then(function (response: any) {
        setData(response.data);
        window.scrollTo(0,0);
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
  }, [token, browseId]);

  useEffect(() => {
    if (token && playId) {
      setLoading(true);
      axios.post(baseUrl + 'play', {token:token,id:playId})
      .then(function (response: any) {
        setData(response.data);
        window.scrollTo(0,0);
      })
      .catch(function () {
        showNotification({
          id: 'player-data-loading',
          color: 'red',
          title: 'Error',
          message: 'Your play data was not received from the server.',
          autoClose: 3000,
        });
      })
      .then(function () {
        setLoading(false);
      });
    }
  }, [token, playId]);

  return (!session.authenticate || havePermission(session, Permissions.web_player_browse)) ? (
    <Box mx="auto" style={{ backgroundImage:images.background?'url(' + images.background + ')':'none'}}>
      <LoadingOverlay visible={loading} />
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
				{getBrowseMetadata()}
                {getMedias()}
              </Grid>
            )}
          </ScrollArea>
        </Grid.Col>
      </Grid>
    </Box>
  ) : (
    <Box sx={{ maxWidth: 1024 }} mx="auto">
      <Text color="red">{i18n.get['YouNotHaveAccessArea']}</Text>
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
  awards: string,
  countries?: BaseMedia[],
  createdBy?: string,
  credits?: string,
  directors?: BaseMedia[],
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
  ratings?: MediaRating[],
  seasons?: string,
  seriesType?: string,
  spokenLanguages?: string,
  startYear?: string,
  status?: string,
  tagline?: string,
  totalSeasons?: number,
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
