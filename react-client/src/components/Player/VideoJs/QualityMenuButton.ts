import videojs from 'video.js'
import Menu from 'video.js/dist/types/menu/menu'
import QualityMenuItem from './QualityMenuItem'
import { VideoJsPlayer } from './VideoJs'
import Component from 'video.js/dist/types/component'

const VideoJsButtonClass = videojs.getComponent('MenuButton')
const VideoJsMenuClass = videojs.getComponent('Menu') as typeof Menu
const VideoJsComponent = videojs.getComponent('Component') as typeof Component
const Dom = videojs.dom

function toTitleCase(value: string): string {
  if (typeof value !== 'string') {
    return value
  }

  return value.charAt(0).toUpperCase() + value.slice(1)
}

export default class QualityMenuButton extends VideoJsButtonClass {
  private hideThreshold_: number
  items: QualityMenuItem[]

  constructor(player: VideoJsPlayer) {
    super(player)
    this.options_.title = player.localize('Quality')
    this.name_ = 'QualityButton'
    this.hideThreshold_ = 0
    this.items = []
  }

  createItems(): QualityMenuItem[] {
    return []
  }

  createMenu(): Menu {
    const menu = new VideoJsMenuClass(this.player_, { menuButton: this })

    menu.addClass('hls-quality-button')

    if (this.options_.title) {
      const titleEl = Dom.createEl('li', {
        className: 'vjs-menu-title',
        innerHTML: toTitleCase(this.options_.title),
        tabIndex: -1,
      })
      const titleComponent = new VideoJsComponent(this.player_, {})
      titleComponent.el_ = titleEl
      this.hideThreshold_ += 1

      menu.addItem(titleComponent)
    }

    this.items = this.createItems()

    if (this.items) {
      for (let i: number = 0; i < this.items.length; i++) {
        menu.addItem(this.items[i])
      }
    }

    return menu
  }
}
