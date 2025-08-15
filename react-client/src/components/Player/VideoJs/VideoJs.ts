import Player from 'video.js/dist/types/player'
import LiveTracker from 'video.js/dist/types/live-tracker'
import ControlBar from 'video.js/dist/types/control-bar/control-bar'

import QualityLevelList from 'videojs-contrib-quality-levels/dist/types/quality-level-list'
import { SourceObject } from 'video.js/dist/types/tech/tech'

export interface HlsQualitySelectorPluginOptions {
  placementIndex?: number
  displayCurrentQuality?: boolean
  vjsIconClass?: string
}

export interface VideoJsPlayer extends Player {
  liveTracker?: LiveTracker
  qualityLevels?: () => QualityLevelList
  controlBar: ControlBar
}

export interface VideoJsPlayerOptions {
  liveui?: boolean
  controls?: boolean
  qualityLevels?: boolean
  sources?: SourceObject[]
  poster?: string
  audioPosterMode?: boolean
  tracks?: Track[]
  userActions?: UserActions
}

interface UserActions {
  hotkeys?: (this: VideoJsPlayer, event: KeyboardEvent) => void
}

interface Track {
  kind: string
  label?: string
  src?: string
  default?: boolean
}
