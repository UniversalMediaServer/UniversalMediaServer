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
import { Badge, Box, Breadcrumbs, Button, Card, Center, Grid, Group, Image, List, LoadingOverlay, MantineTheme, Menu, Paper, Rating, ScrollArea, Stack, Text, Title, Tooltip, useComputedColorScheme } from '@mantine/core';
import { showNotification } from '@mantine/notifications';
import axios from 'axios';
import { createElement, useContext, useEffect, useState } from 'react';
import ReactCountryFlag from 'react-country-flag';
import { useParams } from 'react-router-dom';
import { ArrowBigLeft, ArrowBigRight, Cast, Download, Edit, Folder, Home, LanguageOff, Movie, Music, Photo, PlayerPlay, PlaylistAdd, QuestionMark, RecordMail, RecordMailOff, Tag } from 'tabler-icons-react';

import I18nContext from '../../contexts/i18n-context';
import MainContext from '../../contexts/main-context';
import PlayerEventContext from '../../contexts/player-server-event-context';
import SessionContext from '../../contexts/session-context';
import { havePermission, Permissions } from '../../services/accounts-service';
import { playerApiUrl } from '../../utils';
import { VideoJsPlayer } from './VideoJsPlayer';
import VideoMetadataEditModal from './VideoMetadataEditModal';

export const Player = () => {
  const [data, setData] = useState({ goal: '', folders: [], breadcrumbs: [], medias: [], useWebControl: false } as BaseBrowse);
  const [loading, setLoading] = useState(false);
  const i18n = useContext(I18nContext);
  const main = useContext(MainContext);
  const session = useContext(SessionContext);
  const sse = useContext(PlayerEventContext);
  const { req, id } = useParams();
  const [showVideoMetadataEdit, setShowVideoMetadataEdit] = useState(false);
  const computedColorScheme = useComputedColorScheme('dark', { getInitialValueInEffect: true });

  const getI18nName = (name: string | undefined) => {
    const nameData = name ? name.split('|') : [''];
    if (nameData.length > 1) {
      return i18n.getI18nFormat(nameData);
    } else {
      return i18n.getI18nString(nameData[0]);
    }
  }

  const refreshPage = () => {
    if (sse.uuid && sse.reqType) {
      setLoading(true);
      axios.post(playerApiUrl + sse.reqType, { uuid: sse.uuid, id: sse.reqId, lang: i18n.language }, { headers: { 'Player': sse.uuid } })
        .then(function(response: any) {
          setData(response.data);
          const mediaTemp = response.data.goal === 'show' ? response.data.medias[0] : response.data.breadcrumbs[response.data.breadcrumbs.length - 1];
          setMetadataBackground(
            response.data.goal === 'show' ? (mediaTemp as any).metadata as VideoMetadata : response.data.metadata,
          );
          window.scrollTo(0, 0);
          const url = '/player/' + sse.reqType + '/' + sse.reqId;
          if (url !== history.state) {
            window.history.pushState(url, '', url);
          }
        })
        .catch(function() {
          showNotification({
            id: 'player-data-loading',
            color: 'red',
            title: 'Error',
            message: 'Your browse data was not received from the server.',
            autoClose: 3000,
          });
        })
        .then(function() {
          setLoading(false);
        });
    }
  }

  const setFullyPlayed = (id: string, fullyPlayed: boolean) => {
    setLoading(true);
    axios.post(playerApiUrl + 'setFullyPlayed', { uuid: sse.uuid, id, fullyPlayed }, { headers: { 'Player': sse.uuid } })
      .then(function() {
        refreshPage();
      })
      .catch(function() {
        showNotification({
          id: 'player-fully-played',
          color: 'red',
          title: 'Error',
          message: 'Your request was not handled by the server.',
          autoClose: 3000,
        });
      })
      .then(function() {
        setLoading(false);
      });
  }

  const hasBreadcrumbs = () => {
    return data.breadcrumbs.length > 1;
  }

  const getBreadcrumbs = () => {
    return hasBreadcrumbs() ? (
      <Paper
        mb='xs'
        shadow='xs'
        p='sm'
        style={(theme: MantineTheme) => ({ backgroundColor: computedColorScheme === 'dark' ? theme.colors.darkTransparent[8] : theme.colors.lightTransparent[0], })}
      >
        <Group>
          <Breadcrumbs
            separatorMargin={0}
            styles={{ root: { flexWrap: 'wrap' } }}
          >
            {data.breadcrumbs.map((breadcrumb: BaseMedia) => breadcrumb.id ?
              (
                <Button
                  key={breadcrumb.id ? breadcrumb.id : breadcrumb.name}
                  style={breadcrumb.id ? { fontWeight: 400 } : { cursor: 'default' }}
                  onClick={breadcrumb.id ? () => sse.askBrowseId(breadcrumb.id) : undefined}
                  color='gray'
                  variant='subtle'
                  size='compact-md'
                >
                  {breadcrumb.name === 'root' ? (<Home />) : (getI18nName(breadcrumb.name))}
                </Button>
              ) : (
                getBreadcrumbsMenu(breadcrumb)
              )
            )}
          </Breadcrumbs>
        </Group>
      </Paper>
    ) : null;
  }

  const getLastBreadcrumbButton = (breadcrumb: BaseMedia) => (
    <Button
      key={breadcrumb.name}
      style={{ cursor: 'default' }}
      color='gray'
      variant='subtle'
      size='compact-md'
    >
      {getI18nName(breadcrumb.name)}
    </Button>
  )

  const getVideoMetadataEditModal = () => {
    if (isVideoMetadataEditable()) {
      return <VideoMetadataEditModal uuid={sse.uuid} id={sse.reqId} start={showVideoMetadataEdit} started={() => setShowVideoMetadataEdit(false)} callback={() => refreshPage()} />
    }
    return null;
  }

  const getBreadcrumbsMenu = (breadcrumb: BaseMedia) => {
    return hasBreadcrumbsMenu(breadcrumb) ?
      (
        <Menu shadow="md" width={200}>
          <Menu.Target>
            {getLastBreadcrumbButton(breadcrumb)}
          </Menu.Target>
          <Menu.Dropdown>
            {breadcrumb.fullyplayed === false &&
              <Menu.Item
                color='blue'
                leftSection=<RecordMail />
                onClick={() => setFullyPlayed(sse.reqId, true)}
              >
                {i18n.get('MarkContentsFullyPlayed')}
              </Menu.Item>
            }
            {breadcrumb.fullyplayed === true &&
              <Menu.Item
                color='green'
                leftSection=<RecordMailOff />
                onClick={() => setFullyPlayed(sse.reqId, false)}
              >
                {i18n.get('MarkContentsUnplayed')}
              </Menu.Item>
            }
            {isVideoMetadataEditable() &&
              <Menu.Item
                leftSection=<Edit />
                onClick={() => setShowVideoMetadataEdit(true)}
              >
                {i18n.get('Edit')}
              </Menu.Item>
            }
          </Menu.Dropdown>
        </Menu>
      ) : (
        getLastBreadcrumbButton(breadcrumb)
      )
  }

  const hasBreadcrumbsMenu = (breadcrumb: BaseMedia) => {
    return breadcrumb.fullyplayed != null || isVideoMetadataEditable();
  }

  const isVideoMetadataEditable = () => {
    return (data.goal === 'browse' && data.metadata?.isEditable) || (data.goal === 'show' && ((data.medias[0]) as PlayMedia).mediaType === 'video' && ((data.medias[0]) as VideoMedia).metadata?.isEditable);
  }

  const getVideoJsMediaPlayer = (media: VideoMedia | AudioMedia) => {
    return (<Paper>
      <VideoJsPlayer
        {...{ media: media, uuid: sse.uuid, askPlayId: sse.askPlayId }}
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
          src={playerApiUrl + 'image/' + sse.uuid + '/' + media.id}
          alt={media.name}
        />
      </Paper>);
  }

  const getMediaPlayer = () => {
    if (data.medias.length === 1) {
      switch ((data.medias[0] as PlayMedia).mediaType) {
        case 'video':
          return getVideoJsMediaPlayer(data.medias[0] as VideoMedia);
        case 'audio':
          return getVideoJsMediaPlayer(data.medias[0] as AudioMedia);
        case 'image':
          return getImageMediaPlayer(data.medias[0] as ImageMedia);
      }
    }
    return null;
  }

  const getMediaIcon = (media: BaseMedia) => {
    if (media.icon) {
      switch (media.icon) {
        case 'back':
          return i18n.dir === 'rtl' ? ArrowBigRight : ArrowBigLeft;
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
    const icon = getMediaIcon(media);
    if (icon) {
      image = <Center>{createElement(icon, { size: 60 })}</Center>;
    } else {
      const updateId = media.updateId ? '?update=' + media.updateId : '';
      image = <img src={playerApiUrl + 'thumbnail/' + sse.uuid + '/' + media.id + updateId} alt={media.name}
        style={{ objectFit: 'contain', maxWidth: '100%', height: 'calc(100% - 2.4rem)' }} />;
    }
    return (
      <Paper
        className='thumbnail-container'
        styles={{ root: { backgroundColor: '' } }}
        onClick={() => sse.askReqId(media.id, media.goal ? media.goal : 'browse')}
        key={media.id}
      >
        {image}
        <Text mx='xs' size='sm' lineClamp={1}>
          {getI18nName(media.name)}
        </Text>
      </Paper>
    )
  }

  const getMedias = () => {
    if (data.goal === 'browse') {
      const mediaList = data.medias.map((media: BaseMedia) => {
        return getMedia(media);
      })
      return (<><div className='media-grid'>{mediaList}</div></>);
    }
  }

  const getMediasSelection = (selection: BaseMedia[], title: string) => {
    if (selection && selection.length > 0) {
      const medias = selection.map((media: BaseMedia) => {
        return getMedia(media);
      })
      return (<><Title order={2} mb='md' size='h4' fw={400}>{i18n.get(title)}</Title><div style={{ height: '240px', overflowY: 'hidden' }} className='media-grid front-page-grid'>{medias}</div></>);
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
      return (<Group gap='xs' mt='sm' style={() => ({ color: computedColorScheme === 'dark' ? 'white' : 'black', })}>
        <Text fw={700}>{i18n.get(title)}: </Text>
        {mediaList.map((media: BaseMedia) => {
          return (
            <Badge
              key={media.id}
              style={(theme: MantineTheme) => ({
                cursor: 'pointer',
                color: computedColorScheme === 'dark' ? 'white' : 'black',
                backgroundColor: computedColorScheme === 'dark' ? theme.colors.dark[5] : theme.colors.gray[5],
                '&:hover': {
                  backgroundColor:
                    computedColorScheme === 'dark' ? theme.colors.dark[9] : theme.colors.gray[0],
                },
              })}
              onClick={() => { if (media.id) { sse.askBrowseId(media.id) } }}
            >
              {getI18nName(media.name)}
            </Badge>);
        })}
      </Group>);
    }
  }

  const getMetadataCountryList = (title: string, mediaList?: BaseMedia[]) => {
    if (mediaList && mediaList.length > 0) {
      return (<Group gap='xs' mt='sm' style={() => ({ color: computedColorScheme === 'dark' ? 'white' : 'black', })}>
        <Text fw={700}>{i18n.get(title)}: </Text>
        {mediaList.map((media: BaseMedia) => {
          return (
            <ReactCountryFlag
              key={media.id}
              countryCode={media.name}
              style={{
                fontSize: '1.5em',
                cursor: 'pointer',
              }}
              onClick={() => { if (media.id) { sse.askBrowseId(media.id) } }}
            />
          );
        })}
      </Group>);
    }
  }

  const getMetadataBaseMedia = (title: string, media?: BaseMedia) => {
    if (media) {
      return getMetadataBaseMediaList(title, [media]);
    }
  }

  const getMetadataString = (title: string, mediaString?: string) => {
    if (mediaString) {
      return (
        <Group mt='sm' style={() => ({ color: computedColorScheme === 'dark' ? 'white' : 'black', })}>
          <Text fw={700}>{i18n.get(title)}: </Text><Text>{mediaString}</Text>
        </Group>);
    }
  }

  const getMetadataOriginalTitle = (title?: string, language?: string) => {
    if (title) {
      return (
        <Group mt='sm' style={() => ({ color: computedColorScheme === 'dark' ? 'white' : 'black', })}>
          <LanguageOff /><Text fs='italic'>{title + (language ? ' (' + language + ')' : '')}</Text>
        </Group>);
    }
  }

  const getMetadataTagLine = (mediaString?: string) => {
    if (mediaString) {
      return (
        <Group mt='sm' style={() => ({ color: computedColorScheme === 'dark' ? 'white' : 'black', })}>
          <Tag /><Text fs='italic'>{mediaString}</Text>
        </Group>);
    }
  }

  const getMetadataRating = (rating?: number) => {
    if (rating) {
      return (
        <Group mt='sm' style={() => ({ color: computedColorScheme === 'dark' ? 'white' : 'black', })}>
          <Text fw={700}>{i18n.get('Rating')}: </Text><Tooltip label={rating}><Rating value={rating / 2} fractions={4} readOnly /></Tooltip>
        </Group>);
    }
  }

  const getMetadataRatingList = (ratingsList?: MediaRating[], rating?: number) => {
    if (ratingsList && ratingsList.length > 0) {
      return (<>
        <Group mt='sm' style={() => ({ color: computedColorScheme === 'dark' ? 'white' : 'black', })}>
          <Text fw={700}>{i18n.get('Ratings')}: </Text>
        </Group>
        <List withPadding>
          {ratingsList.map((media: MediaRating) => {
            return (<List.Item key={media.source}>{media.source}: {media.value}</List.Item>);
          })}
        </List>
      </>);
    } else {
      return getMetadataRating(rating);
    }
  }

  const media = data.goal === 'show' ? data.medias[0] : data.breadcrumbs[data.breadcrumbs.length - 1];
  const metadata = data.goal === 'show' ? (media as any).metadata : data.metadata;

  const imdbSvg = <svg aria-hidden='true' width='1.5em' role='img' xmlns='http://www.w3.org/2000/svg' viewBox='0 0 448 512' color='#e0ac00'><path fill='currentColor' d='M350.5 288.7c0 5.4 1.6 14.4-6.2 14.4-1.6 0-3-.8-3.8-2.4-2.2-5.1-1.1-44.1-1.1-44.7 0-3.8-1.1-12.7 4.9-12.7 7.3 0 6.2 7.3 6.2 12.7v32.7zM265 229.9c0-9.7 1.6-16-10.3-16v83.7c12.2.3 10.3-8.7 10.3-18.4v-49.3zM448 80v352c0 26.5-21.5 48-48 48H48c-26.5 0-48-21.5-48-48V80c0-26.5 21.5-48 48-48h352c26.5 0 48 21.5 48 48zM21.3 228.8c-.1.1-.2.3-.3.4h.3v-.4zM97 192H64v127.8h33V192zm113.3 0h-43.1l-7.6 59.9c-2.7-20-5.4-40.1-8.7-59.9h-42.8v127.8h29v-84.5l12.2 84.5h20.6l11.6-86.4v86.4h28.7V192zm86.3 45.3c0-8.1.3-16.8-1.4-24.4-4.3-22.5-31.4-20.9-49-20.9h-24.6v127.8c86.1.1 75 6 75-82.5zm85.9 17.3c0-17.3-.8-30.1-22.2-30.1-8.9 0-14.9 2.7-20.9 9.2V192h-31.7v127.8h29.8l1.9-8.1c5.7 6.8 11.9 9.8 20.9 9.8 19.8 0 22.2-15.2 22.2-30.9v-36z'></path></svg>;
  const tmdbSvg = <svg xmlns='http://www.w3.org/2000/svg' width='1.5em' viewBox='0 0 185.04 133.4'><defs><linearGradient id='linear-gradient' y1='66.7' x2='185.04' y2='66.7' gradientUnits='userSpaceOnUse'><stop offset='0' stopColor='#90cea1' /><stop offset='0.56' stopColor='#3cbec9' /><stop offset='1' stopColor='#00b3e5' /></linearGradient></defs><path xmlns='http://www.w3.org/2000/svg' fill='url(#linear-gradient)' d='M51.06,66.7h0A17.67,17.67,0,0,1,68.73,49h-.1A17.67,17.67,0,0,1,86.3,66.7h0A17.67,17.67,0,0,1,68.63,84.37h.1A17.67,17.67,0,0,1,51.06,66.7Zm82.67-31.33h32.9A17.67,17.67,0,0,0,184.3,17.7h0A17.67,17.67,0,0,0,166.63,0h-32.9A17.67,17.67,0,0,0,116.06,17.7h0A17.67,17.67,0,0,0,133.73,35.37Zm-113,98h63.9A17.67,17.67,0,0,0,102.3,115.7h0A17.67,17.67,0,0,0,84.63,98H20.73A17.67,17.67,0,0,0,3.06,115.7h0A17.67,17.67,0,0,0,20.73,133.37Zm83.92-49h6.25L125.5,49h-8.35l-8.9,23.2h-.1L99.4,49H90.5Zm32.45,0h7.8V49h-7.8Zm22.2,0h24.95V77.2H167.1V70h15.35V62.8H167.1V56.2h16.25V49h-24ZM10.1,35.4h7.8V6.9H28V0H0V6.9H10.1ZM39,35.4h7.8V20.1H61.9V35.4h7.8V0H61.9V13.2H46.75V0H39Zm41.25,0h25V28.2H88V21h15.35V13.8H88V7.2h16.25V0h-24Zm-79,49H9V57.25h.1l9,27.15H24l9.3-27.15h.1V84.4h7.8V49H29.45l-8.2,23.1h-.1L13,49H1.2Zm112.09,49H126a24.59,24.59,0,0,0,7.56-1.15,19.52,19.52,0,0,0,6.35-3.37,16.37,16.37,0,0,0,4.37-5.5A16.91,16.91,0,0,0,146,115.8a18.5,18.5,0,0,0-1.68-8.25,15.1,15.1,0,0,0-4.52-5.53A18.55,18.55,0,0,0,133.07,99,33.54,33.54,0,0,0,125,98H113.29Zm7.81-28.2h4.6a17.43,17.43,0,0,1,4.67.62,11.68,11.68,0,0,1,3.88,1.88,9,9,0,0,1,2.62,3.18,9.87,9.87,0,0,1,1,4.52,11.92,11.92,0,0,1-1,5.08,8.69,8.69,0,0,1-2.67,3.34,10.87,10.87,0,0,1-4,1.83,21.57,21.57,0,0,1-5,.55H121.1Zm36.14,28.2h14.5a23.11,23.11,0,0,0,4.73-.5,13.38,13.38,0,0,0,4.27-1.65,9.42,9.42,0,0,0,3.1-3,8.52,8.52,0,0,0,1.2-4.68,9.16,9.16,0,0,0-.55-3.2,7.79,7.79,0,0,0-1.57-2.62,8.38,8.38,0,0,0-2.45-1.85,10,10,0,0,0-3.18-1v-.1a9.28,9.28,0,0,0,4.43-2.82,7.42,7.42,0,0,0,1.67-5,8.34,8.34,0,0,0-1.15-4.65,7.88,7.88,0,0,0-3-2.73,12.9,12.9,0,0,0-4.17-1.3,34.42,34.42,0,0,0-4.63-.32h-13.2Zm7.8-28.8h5.3a10.79,10.79,0,0,1,1.85.17,5.77,5.77,0,0,1,1.7.58,3.33,3.33,0,0,1,1.23,1.13,3.22,3.22,0,0,1,.47,1.82,3.63,3.63,0,0,1-.42,1.8,3.34,3.34,0,0,1-1.13,1.2,4.78,4.78,0,0,1-1.57.65,8.16,8.16,0,0,1-1.78.2H165Zm0,14.15h5.9a15.12,15.12,0,0,1,2.05.15,7.83,7.83,0,0,1,2,.55,4,4,0,0,1,1.58,1.17,3.13,3.13,0,0,1,.62,2,3.71,3.71,0,0,1-.47,1.95,4,4,0,0,1-1.23,1.3,4.78,4.78,0,0,1-1.67.7,8.91,8.91,0,0,1-1.83.2h-7Z' /></svg>;

  function getMetadataImages(metadata?: VideoMetadata, media?: BaseMedia) {
    let logo, poster, imdb, tmdb;
    if (metadata && metadata.imdbID) {
      imdb = (<a href={'https://www.imdb.com/title/' + metadata.imdbID} rel='noopener noreferrer' target='_blank'>{imdbSvg}</a>);
    }
    if (metadata && metadata.mediaType) {
      if (metadata.mediaType === 'movie' && metadata.tmdbID) {
        tmdb = (<a href={'https://www.themoviedb.org/movie/' + metadata.tmdbID} rel='noopener noreferrer' target='_blank'>{tmdbSvg}</a>);
      } else if (metadata.mediaType === 'tv' && metadata.tmdbID) {
        tmdb = (<a href={'https://www.themoviedb.org/tv/' + metadata.tmdbID} rel='noopener noreferrer' target='_blank'>{tmdbSvg}</a>);
      } else if (metadata.mediaType === 'tv_season' && metadata.tmdbTvID && metadata.tvSeason) {
        tmdb = (<a href={'https://www.themoviedb.org/tv/' + metadata.tmdbTvID + '/season/' + metadata.tvSeason} rel='noopener noreferrer' target='_blank'>{tmdbSvg}</a>);
      } else if (metadata.mediaType === 'tv_episode' && metadata.tmdbTvID && metadata.tvSeason && metadata.tvEpisode) {
        tmdb = (<a href={'https://www.themoviedb.org/tv/' + metadata.tmdbTvID + '/season/' + metadata.tvSeason + '/episode/' + metadata.tvEpisode} rel='noopener noreferrer' target='_blank'>{tmdbSvg}</a>);
      }
    }
    if (metadata && metadata.images && metadata.images.length > 0 && metadata.imageBaseURL) {
      const iso639 = i18n.language.substring(0, 2);
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
              <Stack gap={0}>{imdb}{tmdb}</Stack>
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
          poster = (<img style={{ maxHeight: '100%', maxWidth: '100%' }} src={metadata.imageBaseURL + 'w500' + betterPoster.file_path} alt='Poster' />);
        }
      }
    }
    if (!logo) {
      const title = metadata?.title || metadata?.originalTitle || getI18nName(media?.name);
      if (title) {
        logo = (
          <Group pb='xs'>
            <Text pb='xs'>{title}</Text>
            <Stack gap={0}>{imdb}{tmdb}</Stack>
          </Group>
        );
      }
    }
    if (!poster && metadata && metadata.poster) {
      poster = (<img style={{ maxHeight: '100%', maxWidth: '100%' }} src={metadata.poster} />);
    }
    if (!poster && media && media.id) {
      const updateId = media.updateId ? '?update=' + media.updateId : '';
      poster = (<img style={{ maxHeight: '100%', maxWidth: '100%' }} src={playerApiUrl + 'thumbnail/' + sse.uuid + '/' + media.id + updateId} />);
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
      const iso639 = i18n.language.substring(0, 2);
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
            document.body.style.backgroundImage = 'url(' + background + ')';

            const mantineAppShellMain = document.getElementsByClassName('mantine-AppShell-main')[0] as HTMLElement;
            mantineAppShellMain.style.backgroundColor = 'unset';

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
          <Button variant='default' size='compact-md' leftSection={<PlayerPlay size={14} />} onClick={() => sse.askPlayId(data.medias[0].id)}>{i18n.get('Play')}</Button>
          {data.useWebControl && (
            <Tooltip withinPortal label={i18n.get('PlayOnAnotherRenderer')}>
              <Button variant='default' disabled size='compact-md' onClick={() => { }}><Cast size={14} /></Button>
            </Tooltip>
          )}
          <Tooltip withinPortal label={i18n.get('AddToPlaylist')}>
            <Button variant='default' disabled size='compact-md' onClick={() => { }}><PlaylistAdd size={14} /></Button>
          </Tooltip>
          {((data.medias[0]) as PlayMedia).isDownload && (
            <Tooltip withinPortal label={i18n.get('Download')}>
              <Button variant='default' size='compact-md' onClick={() => window.open(playerApiUrl + 'download/' + sse.uuid + '/' + data.medias[0].id, '_blank')}><Download size={14} /></Button>
            </Tooltip>
          )}
        </Button.Group>
      )
    }
  }

  const getMetadataGridCol = () => {
    if (metadata) {
      return (<>
        <Grid mb='md'>
          <Grid.Col span={12}>
            <Grid columns={20} justify='center'>
              <Grid.Col span={6}>
                {images.poster}
              </Grid.Col>
              <Grid.Col span={12}>
                <Card shadow='sm' p='lg' radius='md' style={(theme: MantineTheme) => ({ backgroundColor: computedColorScheme === 'dark' ? theme.colors.darkTransparent[8] : theme.colors.lightTransparent[0], })}>
                  {images.logo}
                  {getPlayControls()}
                  {getMetadataTagLine(metadata.tagline)}
                  {getMetadataOriginalTitle(metadata.originalTitle, metadata.originalLanguage)}
                  {getMetadataBaseMediaList('Actors', metadata.actors)}
                  {getMetadataString('Awards', metadata.awards)}
                  {getMetadataCountryList('Country', metadata.countries)}
                  {getMetadataBaseMediaList('Director', metadata.directors)}
                  {getMetadataBaseMediaList('Genres', metadata.genres)}
                  {getMetadataString('Plot', metadata.overview)}
                  {getMetadataBaseMedia('Rated', metadata.rated)}
                  {getMetadataRatingList(metadata.ratings, metadata.rating)}
                  {getMetadataString('YearStarted', metadata.startYear)}
                  {getMetadataString('TotalSeasons', metadata.totalSeasons)}
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
      const updateId = media.updateId ? '?update=' + media.updateId : '';
      return (<>
        <Grid mb='md'>
          <Grid.Col span={12}>
            <Grid columns={20} justify='center'>
              <Grid.Col span={6}>
                <Image style={{ maxHeight: 500 }} radius='md' fit='contain' src={playerApiUrl + 'thumbnail/' + sse.uuid + '/' + media.id + updateId} />
              </Grid.Col>
              <Grid.Col span={12}  >
                <Card shadow='sm' p='lg' radius='md' style={(theme: MantineTheme) => ({ backgroundColor: computedColorScheme === 'dark' ? theme.colors.darkTransparent[8] : theme.colors.lightTransparent[0], })}>
                  <Text pb='xs'>{getI18nName(media.name)}</Text>
                  {getPlayControls()}
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
    refreshPage();
  }, [sse.uuid, sse.reqType, sse.reqId, i18n.language]);

  useEffect(() => {
    const getFolderIcon = (folder: BaseMedia) => {
      const icon = getMediaIcon(folder);
      if (icon) {
        return createElement(icon, { size: 20 });
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
            size='compact-md'
            leftSection={getFolderIcon(folder)}
          >
            {getI18nName(folder.name)}
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
            size='compact-md'
            leftSection={getFolderIcon(folder)}
          >
            {getI18nName(folder.name)}
          </Button>
        );
      });
    }
    const getNavFolders = () => {
      if (data.mediaLibraryFolders && data.mediaLibraryFolders.length > 0) {
        return (<><div>{i18n.get('MediaLibrary')}</div>{getMediaLibraryFolders()}<div>{i18n.get('YourFolders')}</div>{getFoldersButtons()}</>);
      } else {
        return getFoldersButtons();
      }
    }
    main.setNavbarValue(getNavFolders());
  }, [data, i18n.get, main.setNavbarValue]);

  return (!session.authenticate || havePermission(session, Permissions.web_player_browse)) ? (
    <Box>
      <LoadingOverlay visible={loading} overlayProps={{ fixed: true }} />
      {getVideoMetadataEditModal()}
      {getBreadcrumbs()}
      <ScrollArea offsetScrollbars>
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
      <div className='backgroundPreloadContainer'>
        <img id='backgroundPreload' crossOrigin='' />
      </div>
    </Box>
  ) : (
    <Box style={{ maxWidth: 1024 }} mx='auto'>
      <Text c='red'>{i18n.get('YouDontHaveAccessArea')}</Text>
    </Box>
  );
};

export default Player;

export interface BaseMedia {
  goal?: string,
  icon?: string,
  id: string,
  name: string,
  fullyplayed?: boolean,
  updateId?: string,
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
  mediaType?: string,
  networks?: string,
  numberOfEpisodes?: number,
  numberOfSeasons?: string,
  originCountry?: string,
  originalLanguage?: string,
  originalTitle?: string,
  overview?: string,
  poster?: string,
  productionCompanies?: string,
  productionCountries?: string,
  rated?: BaseMedia,
  rating?: number,
  ratings?: MediaRating[],
  seasons?: string,
  seriesType?: string,
  spokenLanguages?: string,
  startYear?: string,
  status?: string,
  tagline?: string,
  title?: string,
  tmdbID?: number,
  tmdbTvID?: number,
  tvEpisode?: string,
  tvSeason?: string,
  totalSeasons?: number,
  votes?: string,
  isEditable: boolean,
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
  width: number,
}

export interface AudioMedia extends PlayMedia {
  isNativeAudio: boolean,
  mime: string,
  width: number,
  height: number,
}

interface ImageMedia extends PlayMedia {
  delay?: number,
}
