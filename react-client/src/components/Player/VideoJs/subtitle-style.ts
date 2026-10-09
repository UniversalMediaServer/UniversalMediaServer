import { SubtitleStyle } from '../../../services/player-service'
import './subtitle-style.css'

export function applySubtitleStyle(player: HTMLElement, style: SubtitleStyle) {
  player.classList.add('ums-subtitle-color', 'ums-subtitle-font', 'ums-subtitle-size')
  player.style.setProperty('--ums-subtitle-color', style.color)
  // A quoted CSS string preserves font names containing spaces or punctuation.
  player.style.setProperty('--ums-subtitle-font', style.fontFamily ? JSON.stringify(style.fontFamily) : 'sans-serif')
  const display = player.querySelector<HTMLElement>('.vjs-text-track-display')
  const resize = () => {
    const height = display?.clientHeight || player.clientHeight
    player.style.setProperty('--ums-subtitle-size', (height * style.fontHeightPercent / 100) + 'px')
  }
  const observer = new ResizeObserver(resize)
  observer.observe(display || player)
  resize()
  // Explicit choices in the player's caption dialog take priority over UMS defaults.
  const onChange = (event: Event) => {
    const target = event.target
    if (!(target instanceof HTMLSelectElement)) {
      return
    }
    if (target.matches('.vjs-text-color > select, .vjs-text-opacity > select')) {
      player.classList.remove('ums-subtitle-color')
    }
    if (target.matches('.vjs-font-family > select')) {
      player.classList.remove('ums-subtitle-font')
    }
    if (target.matches('.vjs-font-percent > select')) {
      player.classList.remove('ums-subtitle-size')
    }
  }
  player.addEventListener('change', onChange)
  return () => {
    observer.disconnect()
    player.removeEventListener('change', onChange)
  }
}
