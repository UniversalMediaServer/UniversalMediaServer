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
import axios from 'axios';
import { useEffect } from 'react';
import videojs, { VideoJsPlayerOptions } from 'video.js';
import 'videojs-contrib-quality-levels';
import hlsQualitySelector from 'videojs-hls-quality-selector';

import 'video.js/dist/video-js.min.css';
import 'videojs-hls-quality-selector/dist/videojs-hls-quality-selector.css';

import { playerApiUrl } from '../../utils';
import { BaseMedia, VideoMedia, AudioMedia } from './Player';

declare module 'video.js' {
  interface VideoJsPlayer {
    hlsQualitySelector: typeof hlsQualitySelector;
  }
}

if (!videojs.getPlugin('hlsQualitySelector')) {
  videojs.registerPlugin('hlsQualitySelector', hlsQualitySelector);
}

export const VideoJsPlayer = (vpOptions: VideoPlayerOption) => {
  useEffect(() => {
    const videoElem = document.createElement('video');
    videoElem.id='player';
    videoElem.classList.add('video-js','vjs-default-skin','vjs-fluid','vjs-big-play-centered','full-card','card');
    document.getElementById('videodiv')?.appendChild(videoElem);

    const videoMedia = (vpOptions.media.mediaType === 'video') ? (vpOptions.media as VideoMedia) : null;
    const options = {} as VideoJsPlayerOptions;
    options.liveui = true;
    options.controls = true;
    options.sources=[{src:playerApiUrl + 'media/' + vpOptions.uuid + '/'  + vpOptions.media.id, type: vpOptions.media.mime}];
    options.poster = playerApiUrl + 'thumb/' + vpOptions.uuid + '/'  + vpOptions.media.id;
    if (vpOptions.media.mediaType === 'audio') {
      options.audioPosterMode = true;
    }
    if (videoMedia?.sub) {
      if (!options.tracks) { options.tracks = [] }
      const sub = {kind:'captions', src:'/files/' + videoMedia.sub, default:true} as videojs.TextTrackOptions;
      options.tracks.push(sub);
    }
    if (videoMedia?.isVideoWithChapters) {
      if (!options.tracks) { options.tracks = [] }
      const sub = {kind:'chapters', src:playerApiUrl + 'media/' + vpOptions.uuid + '/'  + vpOptions.media.id + '/chapters.vtt', default:true} as videojs.TextTrackOptions;
      options.tracks.push(sub);
    }
    const status = {'uuid':vpOptions.uuid,'id':vpOptions.media.id} as {[key: string]: string};
    const setStatus = (key:string, value:any, wait:boolean) => {
      if (status[key] !== value) {
        status[key] = value;
        if (! wait) {
          axios.post(playerApiUrl + 'status', status);
        }
      }
    }
    const onready  = (player: videojs.Player ) => {
      const volumeStatus = () => {
        setStatus('mute', videoPlayer.muted() ? '1' : '0', true);
        setStatus('volume', (videoPlayer.volume() * 100).toFixed(0), false);
      }
      videoPlayer.on(['play','playing'], () => {setStatus('playback', 'PLAYING', false)});
      videoPlayer.on('pause', () => {setStatus('playback', 'PAUSED', false)});
      videoPlayer.on(['dispose','abort','ended','error','beforeunload'], () => {setStatus('playback', 'STOPPED', false)});
      videoPlayer.on('timeupdate', () => {setStatus('position', videoPlayer.currentTime().toFixed(0), false)});
      videoPlayer.on('volumechange', () => {volumeStatus()});
      if (videoMedia?.resumePosition) {
        videoPlayer.on('loadedmetadata', () => {videoPlayer.currentTime(videoMedia.resumePosition as number)});
        videoPlayer.one('canplaythrough', () => {videoPlayer.currentTime(videoMedia.resumePosition as number)});
      }
      volumeStatus();
      if (vpOptions.media.isDownload) {
        const indexopt = videoPlayer.controlBar.children().findIndex((e) => e.hasClass('vjs-remaining-time')) + 1;
        const nextButton = videoPlayer.controlBar.addChild('button',
          {'controlText':'Download', 'className':'vjs-menu-button', 'clickHandler':() => {window.open(playerApiUrl + 'download/' + vpOptions.uuid + '/' + vpOptions.media.id ,'_blank'); }}
          , indexopt);
        const placeholder = nextButton.el().getElementsByClassName('vjs-icon-placeholder').item(0);
        if (placeholder) {
			placeholder.innerHTML=('<svg xmlns="http://www.w3.org/2000/svg" width="1rem" height="1rem" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"><path stroke="none" d="M0 0h24v24H0z" fill="none"></path><path d="M4 17v2a2 2 0 0 0 2 2h12a2 2 0 0 0 2 -2v-2"></path><polyline points="7 11 12 16 17 11"></polyline><line x1="12" y1="4" x2="12" y2="16"></line></svg>');
        }
      }
      if (vpOptions.media.surroundMedias.next !== undefined) {
        const next = vpOptions.media.surroundMedias.next as BaseMedia;
        const indexopt = videoPlayer.controlBar.children().findIndex((e) => e.hasClass('vjs-remaining-time')) + 1;
        const nextButton = videoPlayer.controlBar.addChild('button',
          {'controlText':next.name, 'className':'vjs-menu-button', 'clickHandler':() => {vpOptions.askPlayId(next.id)}}
          , indexopt);
        const placeholder = nextButton.el().getElementsByClassName('vjs-icon-placeholder').item(0);
        if (placeholder) {
          placeholder.className = 'vjs-icon-placeholder vjs-icon-next-item';
        }
        if (vpOptions.media.autoContinue) {
          videoPlayer.on('ended', () => {vpOptions.askPlayId(next.id)});
        }
      }
      if (vpOptions.media.surroundMedias.prev !== undefined) {
        const prev = vpOptions.media.surroundMedias.prev as BaseMedia;
        const indexopt = videoPlayer.controlBar.children().findIndex((e) => e.hasClass('vjs-remaining-time')) + 1;
        const prevButton = videoPlayer.controlBar.addChild('Button',
          {'controlText':prev.name, 'className':'vjs-menu-button', 'clickHandler':() => {vpOptions.askPlayId(prev.id)}}
          , indexopt);
        const placeholder = prevButton.el().getElementsByClassName('vjs-icon-placeholder').item(0);
        if (placeholder) {
          placeholder.className = 'vjs-icon-placeholder vjs-icon-previous-item';
        }
      }
      if (vpOptions.media.mime === 'application/x-mpegURL') {
        videoPlayer.hlsQualitySelector();
      }
    };

    const videoPlayer = videojs(videoElem, options, onready as videojs.ReadyCallback);

    return () => {
      if (!videoPlayer.isDisposed()) {
        videoPlayer.dispose()
      }
    };
  }, [vpOptions]);

  return (
    <div id='videodiv'>
    </div>
  );
};

export interface VideoPlayerOption {
  media:VideoMedia|AudioMedia,
  uuid:string,
  askPlayId: (id:string) => void;
}
