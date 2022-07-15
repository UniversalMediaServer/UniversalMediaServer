import axios from 'axios';
import { useEffect, useRef } from 'react';
import videojs, { VideoJsPlayerOptions } from 'video.js';
import hlsQualitySelector from 'videojs-hls-quality-selector';
import 'videojs-contrib-quality-levels';

import 'video.js/dist/video-js.min.css';
import 'videojs-hls-quality-selector/dist/videojs-hls-quality-selector.css';

import { BaseMedia, VideoMedia } from './Player';

export const VideoPlayer = (vpOptions: VideoPlayerOption) => {
  const videoRef = useRef<HTMLVideoElement>(null);

  useEffect(() => {
    if (!videoRef.current || !document.body.contains(videoRef.current)) {return}
    if (!videojs.getPlugin('hlsQualitySelector')) {
      videojs.registerPlugin('hlsQualitySelector', hlsQualitySelector);
    }
    let options = {} as VideoJsPlayerOptions;
    options.liveui = true;
    options.controls = true;
    options.sources=[{src:vpOptions.baseUrl + 'media/' + vpOptions.token + '/'  + vpOptions.media.id, type: vpOptions.media.mime}];
    options.poster = vpOptions.baseUrl + 'thumb/' + vpOptions.token + '/'  + vpOptions.media.id;
    if (vpOptions.media.sub) {
      if (!options.tracks) { options.tracks = [] }
      let sub = {kind:'captions', src:'/files/' + vpOptions.media.sub, default:true} as videojs.TextTrackOptions;
      options.tracks.push(sub);
    }
    if (vpOptions.media.isVideoWithChapters) {
      if (!options.tracks) { options.tracks = [] }
      let sub = {kind:'chapters', src:vpOptions.baseUrl + 'media/' + vpOptions.token + '/'  + vpOptions.media.id + '/chapters.vtt', default:true} as videojs.TextTrackOptions;
      options.tracks.push(sub);
    }

    const onready  = (player: videojs.Player ) => {
      if (vpOptions.media.isDownload) {
        let indexopt = videoPlayer.controlBar.children().findIndex((e) => e.hasClass('vjs-remaining-time')) + 1;
        let nextButton = videoPlayer.controlBar.addChild('button',
          {'controlText':'Download', 'className':'vjs-menu-button', 'clickHandler':() => {window.open(vpOptions.baseUrl + 'download/' + vpOptions.token + '/' + vpOptions.media.id ,'_blank'); }}
          , indexopt);
        let placeholder = nextButton.el().getElementsByClassName('vjs-icon-placeholder').item(0);
        if (placeholder) {
          placeholder.className = 'vjs-icon-placeholder fa fa-cog';
        }
      }
      if (vpOptions.media.surroundMedias.next !== undefined) {
        let next = vpOptions.media.surroundMedias.next as BaseMedia;
        let indexopt = videoPlayer.controlBar.children().findIndex((e) => e.hasClass('vjs-remaining-time')) + 1;
        let nextButton = videoPlayer.controlBar.addChild('button',
          {'controlText':next.name, 'className':'vjs-menu-button', 'clickHandler':() => {askPlayId(next.id)}}
          , indexopt);
        let placeholder = nextButton.el().getElementsByClassName('vjs-icon-placeholder').item(0);
        if (placeholder) {
          placeholder.className = 'vjs-icon-placeholder vjs-icon-next-item';
        }
        if (vpOptions.media.autoContinue) {
          videoPlayer.on('ended', () => {vpOptions.askPlayId(next.id)});
        }
      }
      if (vpOptions.media.surroundMedias.prev !== undefined) {
        let prev = vpOptions.media.surroundMedias.prev as BaseMedia;
        let indexopt = videoPlayer.controlBar.children().findIndex((e) => e.hasClass('vjs-remaining-time')) + 1;
        let prevButton = videoPlayer.controlBar.addChild('Button',
          {'controlText':prev.name, 'className':'vjs-menu-button', 'clickHandler':() => {askPlayId(prev.id)}}
          , indexopt);
        let placeholder = prevButton.el().getElementsByClassName('vjs-icon-placeholder').item(0);
        if (placeholder) {
          placeholder.className = 'vjs-icon-placeholder vjs-icon-previous-item';
        }
      }
	  if (vpOptions.media.mime === 'application/x-mpegURL') {
        videoPlayer.hlsQualitySelector();
	  }
    };
    const videoPlayer = videojs(videoRef.current, options, onready as videojs.ReadyCallback);
    const state = {} as {[key: string]: string};
    const askPlayId = (id:string) => {
      videoPlayer.dispose();
      vpOptions.askPlayId(id);
    }
    const setStatus = (k:string, v:any, wait:boolean) => {
      if (state[k] !== v) {
        state[k] = v;
        if (! wait) {
          axios.post(vpOptions.baseUrl + 'status', JSON.stringify(state));
        }
      }
    }
    setStatus('token', vpOptions.token, true);
    const volumeStatus = () => {
      setStatus('mute', videoPlayer.muted() ? '1' : '0', true);
      setStatus('volume', (videoPlayer.volume() * 100).toFixed(0), false);
    }
    videoPlayer.on(['play','playing'], () => {setStatus('playback', 'PLAYING', false)});
    videoPlayer.on('pause', () => {setStatus('playback', 'PAUSED', false)});
    videoPlayer.on(['dispose','abort','ended','error','beforeunload'], () => {setStatus('playback', 'STOPPED', false)});
    videoPlayer.on('timeupdate', () => {setStatus('position', videoPlayer.currentTime().toFixed(0), false)});
    videoPlayer.on('volumechange', () => {volumeStatus()});
    if (vpOptions.media.resumePosition) {
      videoPlayer.on('loadedmetadata', () => {videoPlayer.currentTime(vpOptions.media.resumePosition as number)});
      videoPlayer.one('canplaythrough', () => {videoPlayer.currentTime(vpOptions.media.resumePosition as number)});
    }
    volumeStatus();
    return () => videoPlayer.dispose();
  }, [vpOptions, videoRef]);

  return (
    <div>
      <video ref={videoRef} className='video-js vjs-default-skin vjs-fluid vjs-big-play-centered full-card card' />
    </div>
  );
};

interface VideoPlayerOption {
  media:VideoMedia,
  baseUrl:string,
  token:string,
  askPlayId: (id:string) => void;
}
