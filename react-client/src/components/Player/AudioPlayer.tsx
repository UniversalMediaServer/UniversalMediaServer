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
import { useEffect, useRef } from 'react';
import videojs, { VideoJsPlayerOptions } from 'video.js';
import 'videojs-contrib-quality-levels';

import 'video.js/dist/video-js.min.css';

import { playerApiUrl } from '../../utils';
import { AudioMedia } from './Player';

export const AudioPlayer = (apOptions: AudioPlayerOption) => {
  const videoRef = useRef<HTMLVideoElement>(null);

  useEffect(() => {
    if (!videoRef.current || !document.body.contains(videoRef.current)) {return}
    const options = {} as VideoJsPlayerOptions;
    options.liveui = true;
    options.controls = true;
    options.sources=[{src:playerApiUrl + "media/" + apOptions.token + "/"  + apOptions.media.id, type: apOptions.media.mime}];
    options.poster = playerApiUrl + "thumb/" + apOptions.token + "/"  + apOptions.media.id;
    const videoPlayer = videojs(videoRef.current, options);
    return () => videoPlayer.dispose();
  }, [apOptions, videoRef]);

  return (
    <div>
      <audio id='player' ref={videoRef} className="video-js vjs-default-skin vjs-fluid vjs-big-play-centered full-card card" />
    </div>
  );
};

interface AudioPlayerOption {
  media:AudioMedia,
  token:string,
}
