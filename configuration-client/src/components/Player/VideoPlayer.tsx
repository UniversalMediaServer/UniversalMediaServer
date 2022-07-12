import { useEffect, useRef } from 'react';
import videojs, { VideoJsPlayerOptions } from 'video.js';
import hlsQualitySelector from 'videojs-hls-quality-selector';
import 'videojs-contrib-quality-levels';

import 'video.js/dist/video-js.min.css';
import 'videojs-hls-quality-selector/dist/videojs-hls-quality-selector.css';

import { VideoMedia } from './Player';

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
    options.sources=[{src:vpOptions.baseUrl + "media/" + vpOptions.token + "/"  + vpOptions.media.id, type: vpOptions.media.mime}];
    options.poster = vpOptions.baseUrl + "thumb/" + vpOptions.token + "/"  + vpOptions.media.id;
    if (vpOptions.media.sub) {
      if (!options.tracks) { options.tracks = [] }
      let sub = {kind:'captions', src:'/files/' + vpOptions.media.sub, default:true} as videojs.TextTrackOptions;
      options.tracks.push(sub);
    }
    const videoPlayer = videojs(videoRef.current, options);
    videoPlayer.hlsQualitySelector();
    return () => videoPlayer.dispose();
  }, [vpOptions, videoRef]);

  return (
    <div>
      <video ref={videoRef} className="video-js vjs-default-skin vjs-fluid vjs-big-play-centered full-card card" />
    </div>
  );
};

interface VideoPlayerOption {
  media:VideoMedia,
  baseUrl:string,
  token:string,
}
