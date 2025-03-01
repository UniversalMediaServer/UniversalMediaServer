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
export interface BaseMedia {
  goal?: string
  icon?: string
  id: string
  name: string
  fullyplayed?: boolean
  updateId?: string
}

export interface PlayMedia extends BaseMedia {
  autoContinue: boolean
  isDownload: boolean
  isDynamicPls: boolean
  mediaType: string
  surroundMedias: SurroundMedias
}

export interface VideoMedia extends PlayMedia {
  height: number
  isVideoWithChapters: boolean
  metadata?: VideoMetadata
  mime: string
  resumePosition?: number
  width: number
}

export interface SurroundMedias {
  prev?: BaseMedia
  next?: BaseMedia
}

export interface AudioMedia extends PlayMedia {
  isNativeAudio: boolean
  mime: string
  width: number
  height: number
}

export interface MediasSelections {
  recentlyAdded: BaseMedia[]
  recentlyPlayed: BaseMedia[]
  inProgress: BaseMedia[]
  mostPlayed: BaseMedia[]
}

export interface BaseBrowse {
  breadcrumbs: BaseMedia[]
  folders: BaseMedia[]
  goal: string
  medias: BaseMedia[]
  mediaLibraryFolders?: BaseMedia[]
  mediasSelections?: MediasSelections
  metadata?: VideoMetadata
  useWebControl: boolean
}

export interface MediaRating {
  source: string
  value: string
}

export interface VideoMetadata {
  actors?: BaseMedia[]
  awards?: string
  countries?: BaseMedia[]
  createdBy?: string
  credits?: string
  directors?: BaseMedia[]
  endYear?: string
  externalIDs?: string
  firstAirDate?: string
  genres?: BaseMedia[]
  homepage?: string
  images?: VideoMetadataImages[]
  imageBaseURL: string
  imdbID?: string
  inProduction?: boolean
  languages?: string
  lastAirDate?: string
  mediaType?: string
  networks?: string
  numberOfEpisodes?: number
  numberOfSeasons?: string
  originCountry?: string
  originalLanguage?: string
  originalTitle?: string
  overview?: string
  poster?: string
  productionCompanies?: string
  productionCountries?: string
  rated?: BaseMedia
  rating?: number
  ratings?: MediaRating[]
  seasons?: string
  seriesType?: string
  spokenLanguages?: string
  startYear?: string
  status?: string
  tagline?: string
  title?: string
  tmdbID?: number
  tmdbTvID?: number
  tvEpisode?: string
  tvSeason?: string
  totalSeasons?: number
  votes?: string
  isEditable: boolean
}

export interface VideoMetadataImages {
  backdrops?: VideoMetadataImage[]
  logos?: VideoMetadataImage[]
  posters?: VideoMetadataImage[]
}

export interface VideoMetadataImage {
  aspect_ratio?: number
  height?: number
  iso_639_1?: string
  file_path?: string
  vote_average: number
  vote_count?: number
  width?: number
}

export interface ImageMedia extends PlayMedia {
  delay?: number
}
