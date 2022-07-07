import { Box, Breadcrumbs, Button, Card, Grid, Group, Image, Paper, ScrollArea, Stack, Text } from '@mantine/core';
import { useLocalStorage } from '@mantine/hooks';
import { showNotification } from '@mantine/notifications';
import axios from 'axios';
import { useEffect, useState } from 'react';
import { ArrowBigLeft, ArrowBigRight, Folder, Home } from 'tabler-icons-react';
import ReactPlayer from 'react-player';

const Player = () => {
  const baseUrl = '/v1/api/player/';
  const [token, setToken] = useState('');
  const [data, setData] = useState({goal:'',folders:[],breadcrumbs:[],medias:[]});
  const [browseId, setBrowseId] = useState('0');
  const [playId, setPlayId] = useState('');

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

  const getFolders = () => {
    return data.folders.map((folder: BaseMedia) => {
      return (
        <Button
          onClick={() => askBrowseId(folder.id)}
          color='gray'
          variant="subtle"
          compact
          styles={{inner: {justifyContent: 'normal'}, root:{fontWeight: 400, '&:hover':{fontWeight: 600}}}}
          leftIcon = {getFolderIcon(folder.name, rtl)}
        >
          {folder.name}
        </Button>
     );
	});
  }

  const getFolderIcon = (name:string, rtl:boolean) => {
    return name === '..' ? rtl ? <ArrowBigRight size={20}/> : <ArrowBigLeft size={20}/> : <Folder size={20}/>;
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
      <ReactPlayer
        url={baseUrl + "raw/" + token + "/"  + media.id}
        controls={true}
        light={baseUrl + "thumb/" + token + "/"  + media.id}
      />
    </Paper>);
  }

  const getAudioMediaPlayer = (media: AudioMedia) => {
    return (<Paper>
      <ReactPlayer
        url={baseUrl + "raw/" + token + "/"  + media.id}
        controls={true}
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

  const getMedias = () => {
    return data.medias.map((media: GoalMedia) => {
      return (
	  <Grid.Col span={3}>
        <Card shadow="sm" p="lg"
          onClick={() => media.goal==="play" ? askPlayId(media.id) : askBrowseId(media.id)}
          style={{cursor:'pointer'}}
		>
          <Card.Section>
            <Image src={baseUrl + "thumb/" + token + "/"  + media.id} fit="contain" height={160} alt={media.name} />
          </Card.Section>
          <Text align="center" size="sm">
            {media.name}
          </Text>
        </Card>
		</Grid.Col>
     )
	});
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
  id: string,
  name: string,
}

interface GoalMedia extends BaseMedia {
  goal: string,
}

interface SurroundMedias {
  prev?: BaseMedia,
  next?: BaseMedia,
}

interface PlayMedia extends BaseMedia {
  mediaType: string,
  autoContinue: boolean,
  isDynamicPls: boolean,
  surroundMedias: SurroundMedias,
  useWebControl: boolean,
}

interface VideoMedia extends PlayMedia {
  isVideoWithChapters: boolean,
  mime: string,
  width: number,
  height: number,
  metadatas?: string,
  resumePosition?: number,
  sub?: string,
}

interface AudioMedia extends PlayMedia {
  isNativeAudio:boolean,
  mime:string,
  width:number,
  height:number,
}

interface ImageMedia extends PlayMedia {
  delay?:number,
}
