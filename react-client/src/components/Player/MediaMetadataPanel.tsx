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
import { Badge, Card, Grid, Group, List, Rating, Stack, Text, Tooltip } from '@mantine/core'
import { IconLanguageOff, IconTag } from '@tabler/icons-react'
import { ReactNode } from 'react'
import ReactCountryFlag from 'react-country-flag'

import { I18nInterface } from '../../services/i18n-service'
import { PlayerEventInterface } from '../../services/player-server-event-service'
import { BaseMedia, MediaRating, VideoMetadata } from '../../services/player-service'
import { playerApiUrl } from '../../utils'

export default function MediaMetadataPanel({ i18n, sse, media, metadata, children }: { i18n: I18nInterface, sse: PlayerEventInterface, media: BaseMedia, metadata: VideoMetadata, children?: ReactNode }) {
  const imdbSvg = <svg aria-hidden="true" width="1.5em" role="img" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 448 512" color="#e0ac00"><path fill="currentColor" d="M350.5 288.7c0 5.4 1.6 14.4-6.2 14.4-1.6 0-3-.8-3.8-2.4-2.2-5.1-1.1-44.1-1.1-44.7 0-3.8-1.1-12.7 4.9-12.7 7.3 0 6.2 7.3 6.2 12.7v32.7zM265 229.9c0-9.7 1.6-16-10.3-16v83.7c12.2.3 10.3-8.7 10.3-18.4v-49.3zM448 80v352c0 26.5-21.5 48-48 48H48c-26.5 0-48-21.5-48-48V80c0-26.5 21.5-48 48-48h352c26.5 0 48 21.5 48 48zM21.3 228.8c-.1.1-.2.3-.3.4h.3v-.4zM97 192H64v127.8h33V192zm113.3 0h-43.1l-7.6 59.9c-2.7-20-5.4-40.1-8.7-59.9h-42.8v127.8h29v-84.5l12.2 84.5h20.6l11.6-86.4v86.4h28.7V192zm86.3 45.3c0-8.1.3-16.8-1.4-24.4-4.3-22.5-31.4-20.9-49-20.9h-24.6v127.8c86.1.1 75 6 75-82.5zm85.9 17.3c0-17.3-.8-30.1-22.2-30.1-8.9 0-14.9 2.7-20.9 9.2V192h-31.7v127.8h29.8l1.9-8.1c5.7 6.8 11.9 9.8 20.9 9.8 19.8 0 22.2-15.2 22.2-30.9v-36z"></path></svg>
  const tmdbSvg = (
    <svg xmlns="http://www.w3.org/2000/svg" width="1.5em" viewBox="0 0 185.04 133.4">
      <defs>
        <linearGradient id="linear-gradient" y1="66.7" x2="185.04" y2="66.7" gradientUnits="userSpaceOnUse">
          <stop offset="0" stopColor="#90cea1" />
          <stop offset="0.56" stopColor="#3cbec9" />
          <stop offset="1" stopColor="#00b3e5" />
        </linearGradient>
      </defs>
      <path xmlns="http://www.w3.org/2000/svg" fill="url(#linear-gradient)" d="M51.06,66.7h0A17.67,17.67,0,0,1,68.73,49h-.1A17.67,17.67,0,0,1,86.3,66.7h0A17.67,17.67,0,0,1,68.63,84.37h.1A17.67,17.67,0,0,1,51.06,66.7Zm82.67-31.33h32.9A17.67,17.67,0,0,0,184.3,17.7h0A17.67,17.67,0,0,0,166.63,0h-32.9A17.67,17.67,0,0,0,116.06,17.7h0A17.67,17.67,0,0,0,133.73,35.37Zm-113,98h63.9A17.67,17.67,0,0,0,102.3,115.7h0A17.67,17.67,0,0,0,84.63,98H20.73A17.67,17.67,0,0,0,3.06,115.7h0A17.67,17.67,0,0,0,20.73,133.37Zm83.92-49h6.25L125.5,49h-8.35l-8.9,23.2h-.1L99.4,49H90.5Zm32.45,0h7.8V49h-7.8Zm22.2,0h24.95V77.2H167.1V70h15.35V62.8H167.1V56.2h16.25V49h-24ZM10.1,35.4h7.8V6.9H28V0H0V6.9H10.1ZM39,35.4h7.8V20.1H61.9V35.4h7.8V0H61.9V13.2H46.75V0H39Zm41.25,0h25V28.2H88V21h15.35V13.8H88V7.2h16.25V0h-24Zm-79,49H9V57.25h.1l9,27.15H24l9.3-27.15h.1V84.4h7.8V49H29.45l-8.2,23.1h-.1L13,49H1.2Zm112.09,49H126a24.59,24.59,0,0,0,7.56-1.15,19.52,19.52,0,0,0,6.35-3.37,16.37,16.37,0,0,0,4.37-5.5A16.91,16.91,0,0,0,146,115.8a18.5,18.5,0,0,0-1.68-8.25,15.1,15.1,0,0,0-4.52-5.53A18.55,18.55,0,0,0,133.07,99,33.54,33.54,0,0,0,125,98H113.29Zm7.81-28.2h4.6a17.43,17.43,0,0,1,4.67.62,11.68,11.68,0,0,1,3.88,1.88,9,9,0,0,1,2.62,3.18,9.87,9.87,0,0,1,1,4.52,11.92,11.92,0,0,1-1,5.08,8.69,8.69,0,0,1-2.67,3.34,10.87,10.87,0,0,1-4,1.83,21.57,21.57,0,0,1-5,.55H121.1Zm36.14,28.2h14.5a23.11,23.11,0,0,0,4.73-.5,13.38,13.38,0,0,0,4.27-1.65,9.42,9.42,0,0,0,3.1-3,8.52,8.52,0,0,0,1.2-4.68,9.16,9.16,0,0,0-.55-3.2,7.79,7.79,0,0,0-1.57-2.62,8.38,8.38,0,0,0-2.45-1.85,10,10,0,0,0-3.18-1v-.1a9.28,9.28,0,0,0,4.43-2.82,7.42,7.42,0,0,0,1.67-5,8.34,8.34,0,0,0-1.15-4.65,7.88,7.88,0,0,0-3-2.73,12.9,12.9,0,0,0-4.17-1.3,34.42,34.42,0,0,0-4.63-.32h-13.2Zm7.8-28.8h5.3a10.79,10.79,0,0,1,1.85.17,5.77,5.77,0,0,1,1.7.58,3.33,3.33,0,0,1,1.23,1.13,3.22,3.22,0,0,1,.47,1.82,3.63,3.63,0,0,1-.42,1.8,3.34,3.34,0,0,1-1.13,1.2,4.78,4.78,0,0,1-1.57.65,8.16,8.16,0,0,1-1.78.2H165Zm0,14.15h5.9a15.12,15.12,0,0,1,2.05.15,7.83,7.83,0,0,1,2,.55,4,4,0,0,1,1.58,1.17,3.13,3.13,0,0,1,.62,2,3.71,3.71,0,0,1-.47,1.95,4,4,0,0,1-1.23,1.3,4.78,4.78,0,0,1-1.67.7,8.91,8.91,0,0,1-1.83.2h-7Z" />
    </svg>
  )

  function getMetadataImages(metadata?: VideoMetadata, media?: BaseMedia) {
    let logo, poster, imdb, tmdb
    if (metadata && metadata.imdbID) {
      imdb = (<a href={'https://www.imdb.com/title/' + metadata.imdbID} rel="noopener noreferrer" target="_blank">{imdbSvg}</a>)
    }
    if (metadata && metadata.mediaType) {
      if (metadata.mediaType === 'movie' && metadata.tmdbID) {
        tmdb = (<a href={'https://www.themoviedb.org/movie/' + metadata.tmdbID} rel="noopener noreferrer" target="_blank">{tmdbSvg}</a>)
      }
      else if (metadata.mediaType === 'tv' && metadata.tmdbID) {
        tmdb = (<a href={'https://www.themoviedb.org/tv/' + metadata.tmdbID} rel="noopener noreferrer" target="_blank">{tmdbSvg}</a>)
      }
      else if (metadata.mediaType === 'tv_season' && metadata.tmdbTvID && metadata.tvSeason) {
        tmdb = (<a href={'https://www.themoviedb.org/tv/' + metadata.tmdbTvID + '/season/' + metadata.tvSeason} rel="noopener noreferrer" target="_blank">{tmdbSvg}</a>)
      }
      else if (metadata.mediaType === 'tv_episode' && metadata.tmdbTvID && metadata.tvSeason && metadata.tvEpisode) {
        tmdb = (<a href={'https://www.themoviedb.org/tv/' + metadata.tmdbTvID + '/season/' + metadata.tvSeason + '/episode/' + metadata.tvEpisode} rel="noopener noreferrer" target="_blank">{tmdbSvg}</a>)
      }
    }
    if (metadata && metadata.images && metadata.images.length > 0 && metadata.imageBaseURL) {
      const iso639 = i18n.language.substring(0, 2)
      const apiImagesList = metadata.images[0]

      // Set a logo as the heading
      if (apiImagesList && apiImagesList.logos && apiImagesList.logos.length > 0) {
        let logos = apiImagesList.logos.filter(logo => logo.iso_639_1 === iso639)
        if (logos.length === 0) {
          logos = apiImagesList.logos.filter(logo => !logo.iso_639_1)
        }
        if (logos.length === 0) {
          logos = apiImagesList.logos.filter(logo => logo.iso_639_1 === 'en')
        }
        if (logos.length > 0) {
          const betterLogo = logos.reduce((previousValue, currentValue) => {
            return (currentValue.vote_average > previousValue.vote_average) ? currentValue : previousValue
          })
          logo = (
            <Group pb="xs">
              <img src={metadata.imageBaseURL + 'w500' + betterLogo.file_path} style={{ maxHeight: '150px', maxWidth: 'calc(100% - 61px)' }} alt={metadata.originalTitle}></img>
              <Stack gap={0}>
                {imdb}
                {tmdb}
              </Stack>
            </Group>
          )
        }
      }
      // Set a poster
      if (apiImagesList && apiImagesList.posters && apiImagesList.posters.length > 0) {
        let posters = apiImagesList.posters.filter(poster => poster.iso_639_1 === iso639)
        if (posters.length === 0) {
          posters = apiImagesList.posters.filter(poster => !poster.iso_639_1)
        }
        if (posters.length === 0) {
          posters = apiImagesList.posters.filter(poster => poster.iso_639_1 === 'en')
        }
        if (posters.length > 0) {
          const betterPoster = posters.reduce((previousValue, currentValue) => {
            return (currentValue.vote_average > previousValue.vote_average) ? currentValue : previousValue
          })
          poster = (<img style={{ maxHeight: '100%', maxWidth: '100%' }} src={metadata.imageBaseURL + 'w500' + betterPoster.file_path} alt="Poster" />)
        }
      }
    }
    if (!logo) {
      const title = metadata?.title || metadata?.originalTitle || i18n.getLocalizedName(media?.name)
      if (title) {
        logo = (
          <Group pb="xs">
            <Text pb="xs">{title}</Text>
            <Stack gap={0}>
              {imdb}
              {tmdb}
            </Stack>
          </Group>
        )
      }
    }
    if (!poster && metadata && metadata.poster) {
      poster = (<img style={{ maxHeight: '100%', maxWidth: '100%' }} src={metadata.poster} />)
    }
    if (!poster && media && media.id) {
      const updateId = media.updateId ? '?update=' + media.updateId : ''
      poster = (<img style={{ maxHeight: '100%', maxWidth: '100%' }} src={playerApiUrl + 'thumbnail/' + sse.uuid + '/' + media.id + updateId} />)
    }
    return { logo, poster }
  }

  const images = getMetadataImages(metadata, media)

  const MetadataString = ({ i18n, title, mediaString }: { i18n: I18nInterface, title: string, mediaString?: string }) => {
    if (mediaString) {
      return (
        <Group mt="sm" style={{ color: 'var(--mantine-color-bright)' }}>
          <Text fw={700}>
            {i18n.get(title)}
            :
            {' '}
          </Text>
          <Text>{mediaString}</Text>
        </Group>
      )
    }
  }

  const MetadataBaseMediaList = ({ i18n, sse, title, mediaList }: { i18n: I18nInterface, sse: PlayerEventInterface, title: string, mediaList?: BaseMedia[] }) => {
    if (mediaList && mediaList.length > 0) {
      return (
        <Group gap="xs" mt="sm" style={{ color: 'var(--mantine-color-bright)' }}>
          <Text fw={700}>
            {i18n.get(title)}
            :
            {' '}
          </Text>
          {mediaList.map((media: BaseMedia) => {
            return (
              <Badge
                key={title + media.id}
                style={{
                  'cursor': 'pointer',
                  'color': 'var(--mantine-color-bright)',
                  'backgroundColor': 'var(--mantine-color-default-border)',
                  '&:hover': {
                    backgroundColor: 'var(--mantine-color-default-hover)',
                  },
                }}
                onClick={() => {
                  if (media.id) {
                    sse.askBrowseId(media.id)
                  }
                }}
              >
                {i18n.getLocalizedName(media.name)}
              </Badge>
            )
          })}
        </Group>
      )
    }
  }

  const MetadataBaseMedia = ({ i18n, sse, title, media }: { i18n: I18nInterface, sse: PlayerEventInterface, title: string, media?: BaseMedia }) => {
    return media
      ? <MetadataBaseMediaList i18n={i18n} sse={sse} title={title} mediaList={[media]} />
      : undefined
  }

  function MetadataCountryList({ i18n, sse, mediaList }: { i18n: I18nInterface, sse: PlayerEventInterface, mediaList?: BaseMedia[] }) {
    return (mediaList && mediaList.length > 0)
      ? (
          <Group gap="xs" mt="sm" style={{ color: 'var(--mantine-color-bright)' }}>
            <Text fw={700}>
              {i18n.get('Country')}
              :
              {' '}
            </Text>
            {mediaList.map((media: BaseMedia) => {
              return (
                <ReactCountryFlag
                  key={media.id}
                  countryCode={media.name}
                  style={{
                    fontSize: '1.5em',
                    cursor: 'pointer',
                  }}
                  onClick={() => {
                    if (media.id) {
                      sse.askBrowseId(media.id)
                    }
                  }}
                />
              )
            })}
          </Group>
        )
      : undefined
  }

  const MetadataOriginalTitle = ({ title, language }: { title?: string, language?: string }) => {
    if (title) {
      return (
        <Group mt="sm" style={{ color: 'var(--mantine-color-bright)' }}>
          <IconLanguageOff />
          <Text fs="italic">{title + (language ? ' (' + language + ')' : '')}</Text>
        </Group>
      )
    }
  }

  const MetadataTagLine = ({ mediaString }: { mediaString?: string }) => {
    if (mediaString) {
      return (
        <Group mt="sm" style={{ color: 'var(--mantine-color-bright)' }}>
          <IconTag />
          <Text fs="italic">{mediaString}</Text>
        </Group>
      )
    }
  }

  const MetadataRating = ({ rating }: { rating?: number }) => {
    if (rating) {
      return (
        <Group mt="sm" style={{ color: 'var(--mantine-color-bright)' }}>
          <Text fw={700}>
            {i18n.get('Rating')}
            :
            {' '}
          </Text>
          <Tooltip label={rating}><Rating value={rating / 2} fractions={4} readOnly /></Tooltip>
        </Group>
      )
    }
  }

  const MetadataRatingList = ({ ratingsList, rating }: { ratingsList?: MediaRating[], rating?: number }) => {
    if (ratingsList && ratingsList.length > 0) {
      return (
        <>
          <Group mt="sm" style={{ color: 'var(--mantine-color-bright)' }}>
            <Text fw={700}>
              {i18n.get('Ratings')}
              :
              {' '}
            </Text>
          </Group>
          <List withPadding>
            {ratingsList.map((media: MediaRating) => {
              return (
                <List.Item>
                  {media.source}
                  :
                  {' '}
                  {media.value}
                </List.Item>
              )
            })}
          </List>
        </>
      )
    }
    else {
      return <MetadataRating rating={rating} />
    }
  }

  return (
    <Grid mb="md">
      <Grid.Col span={12}>
        <Grid columns={20} justify="center">
          <Grid.Col span={6}>
            {images.poster}
          </Grid.Col>
          <Grid.Col span={12}>
            <Card shadow="sm" p="lg" radius="md" bg="transparentBg">
              {images.logo}
              {children}
              <MetadataTagLine mediaString={metadata.tagline} />
              <MetadataOriginalTitle title={metadata.originalTitle} language={metadata.originalLanguage} />
              <MetadataBaseMediaList i18n={i18n} sse={sse} title="Actors" mediaList={metadata.actors} />
              <MetadataString i18n={i18n} title="Awards" mediaString={metadata.awards} />
              <MetadataCountryList i18n={i18n} sse={sse} mediaList={metadata.countries} />
              <MetadataBaseMediaList i18n={i18n} sse={sse} title="Director" mediaList={metadata.directors} />
              <MetadataBaseMediaList i18n={i18n} sse={sse} title="Genres" mediaList={metadata.genres} />
              <MetadataString i18n={i18n} title="Plot" mediaString={metadata.overview} />
              <MetadataBaseMedia i18n={i18n} sse={sse} title="Rated" media={metadata.rated} />
              <MetadataRatingList ratingsList={metadata.ratings} rating={metadata.rating} />
              <MetadataString i18n={i18n} title="YearStarted" mediaString={metadata.startYear} />
              <MetadataString i18n={i18n} title="TotalSeasons" mediaString={metadata.totalSeasons ? metadata.totalSeasons.toString() : undefined} />
            </Card>
          </Grid.Col>
        </Grid>
      </Grid.Col>
    </Grid>
  )
}
