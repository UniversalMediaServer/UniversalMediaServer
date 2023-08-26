/**
 * based on videojs-hls-quality-selector from Chris Boustead (chris@forgemotion.com)
 */
import videojs from 'video.js';
import { HlsQualitySelectorClass } from './HlsQualitySelector';
import './HlsQualitySelector.css';

const VERSION = "1.0.0";

/**
 * A video.js plugin.
 *
 * In the plugin function, the value of `this` is a video.js `Player`
 * instance. You cannot rely on the player being in a "ready" state here,
 * depending on how the plugin is invoked. This may or may not be important
 * to you; if not, remove the wait for "ready"!
 *
 * @param {Object} options Plugin options object
 * @return {HlsQualitySelectorClass} a HlsQualitySelector
 */
const hlsQualitySelector = function(this: any, options: any): HlsQualitySelectorClass {
  return initPlugin(this, videojs.obj.merge({}, options));
};

/**
 * Initialization function for the hlsQualitySelector plugin. Sets up the QualityLevelList and
 * event handlers.
 *
 * @param {Player} player Player object.
 * @param {Object} options Plugin options object.
 * @return {HlsQualitySelectorClass} a HlsQualitySelector
 */
const initPlugin = function(player: any, options: any): HlsQualitySelectorClass {
  const hlsQualitySelector = new HlsQualitySelectorClass(player, options);
  player.hlsQualitySelector = () => hlsQualitySelector;
  player.hlsQualitySelector.VERSION = VERSION;
  return hlsQualitySelector;
};

// Register the plugin with video.js.
videojs.registerPlugin('hlsQualitySelector', hlsQualitySelector);

export default hlsQualitySelector;