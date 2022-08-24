import { useEffect, useRef } from 'react';
import videojs, { VideoJsPlayerOptions } from 'video.js';
import 'videojs-contrib-quality-levels';

import 'video.js/dist/video-js.min.css';

import { AudioMedia } from './Player';

export const AudioPlayer = (apOptions: AudioPlayerOption) => {
  const videoRef = useRef<HTMLVideoElement>(null);

  useEffect(() => {
    if (!videoRef.current || !document.body.contains(videoRef.current)) {return}
    let options = {} as VideoJsPlayerOptions;
    options.liveui = true;
    options.controls = true;
    options.sources=[{src:apOptions.baseUrl + "media/" + apOptions.token + "/"  + apOptions.media.id, type: apOptions.media.mime}];
    options.poster = apOptions.baseUrl + "thumb/" + apOptions.token + "/"  + apOptions.media.id;
    const videoPlayer = videojs(videoRef.current, options);
    return () => videoPlayer.dispose();
  }, [apOptions, videoRef]);

  return (
    <div>
      <audio ref={videoRef} className="video-js vjs-default-skin vjs-fluid vjs-big-play-centered full-card card" />
    </div>
  );
};

interface AudioPlayerOption {
  media:AudioMedia,
  baseUrl:string,
  token:string,
}
