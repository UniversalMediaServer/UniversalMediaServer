import { useEffect, useRef } from 'react';
import videojs, { VideoJsPlayerOptions } from 'video.js';
import hlsQualitySelector from 'videojs-hls-quality-selector';
import 'videojs-contrib-quality-levels';

import 'video.js/dist/video-js.min.css';
import 'videojs-hls-quality-selector/dist/videojs-hls-quality-selector.css';

export const VideoPlayer = (options: VideoJsPlayerOptions) => {
  const videoRef = useRef<HTMLVideoElement>(null);

  useEffect(() => {
    if (!videoRef.current || !document.body.contains(videoRef.current)) {return}
    if (!videojs.getPlugin('hlsQualitySelector')) {
      videojs.registerPlugin('hlsQualitySelector', hlsQualitySelector);
    }
    const videoPlayer = videojs(videoRef.current, options);
	videoPlayer.hlsQualitySelector();
    return () => videoPlayer.dispose();
  }, [options, videoRef]);

  return (
    <div>
      <video ref={videoRef} className="video-js vjs-default-skin vjs-fluid vjs-big-play-centered full-card card" />
    </div>
  );
};