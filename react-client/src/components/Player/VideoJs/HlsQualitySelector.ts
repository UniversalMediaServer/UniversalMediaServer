import MenuButton from 'video.js/dist/types/menu/menu-button'

import QualityMenuButton from './QualityMenuButton'
import QualityMenuItem from './QualityMenuItem'
import { HlsQualitySelectorPluginOptions, VideoJsPlayer } from './VideoJs'

export interface QualityItem {
  label: string
  selected?: boolean
  value: number
}

export default class HlsQualitySelector {
  private player: VideoJsPlayer
  private config: HlsQualitySelectorPluginOptions
  private _qualityButton: QualityMenuButton
  private _menuButton: MenuButton
  private _currentQuality: number | undefined

  constructor(player: VideoJsPlayer, options: HlsQualitySelectorPluginOptions) {
    this.player = player
    this.config = options
    this._qualityButton = new QualityMenuButton(player)
    this._menuButton = this._qualityButton as unknown as MenuButton
    this.addQualityButton()
    this.bindPlayerEvents()
  }

  bindPlayerEvents() {
    if (this.player.qualityLevels) {
      this.player
        .qualityLevels()
        .on('addqualitylevel', this.onAddQualityLevel.bind(this))
    }
  }

  addQualityButton() {
    const placementIndex = this.player.controlBar.children().length - 2
    const concreteButtonInstance = this.player.controlBar.addChild(
      this._qualityButton,
      { componentClass: 'qualitySelector' },
      this.config.placementIndex || placementIndex,
    ) as QualityMenuButton

    concreteButtonInstance.addClass('vjs-quality-selector')

    if (!this.config.displayCurrentQuality) {
      const icon = ` ${this.config.vjsIconClass || 'vjs-icon-hd'}`
      const iconPlaceholder = concreteButtonInstance.$('.vjs-icon-placeholder')
      if (iconPlaceholder) {
        iconPlaceholder.className += icon
      }
    }
    else {
      this.setButtonInnerText(this.player.localize('Auto'))
    }
    concreteButtonInstance.removeClass('vjs-hidden')
  }

  setButtonInnerText(text: string) {
    const iconPlaceholder = this._qualityButton.$('.vjs-icon-placeholder')
    if (iconPlaceholder) {
      iconPlaceholder.innerHTML = text
    }
  }

  getQualityMenuItem(item: QualityItem) {
    const player = this.player

    return new QualityMenuItem(player, item, this._qualityButton, this)
  }

  onAddQualityLevel() {
    if (!this.player.qualityLevels) {
      return
    }
    const qualityList = this.player.qualityLevels()
    const levels = qualityList.levels_ || []

    const levelItems: Array<QualityMenuItem> = []

    for (let i = 0; i < levels.length; ++i) {
      const { width, height } = levels[i]
      const pixels = width > height ? height : width

      if (!pixels) continue

      if (!levelItems.filter(_existingItem => _existingItem.item && _existingItem.item.value === pixels).length) {
        const levelItem = this.getQualityMenuItem({
          label: `${pixels}p`,
          value: pixels,
        })

        levelItems.push(levelItem)
      }
    }

    levelItems.sort((current, next) => current.item.value - next.item.value)

    levelItems.push(this.getQualityMenuItem({
      label: this.player.localize('Auto'),
      value: -1,
      selected: true,
    }))

    if (this._qualityButton) {
      this._qualityButton.createItems = function () {
        return levelItems
      }
      this._menuButton.update()
    }
  }

  setQuality(quality: number) {
    if (!this.player.qualityLevels) {
      return
    }
    const qualityList = this.player.qualityLevels()

    // Set quality on plugin
    this._currentQuality = quality

    if (this.config.displayCurrentQuality) {
      this.setButtonInnerText(quality === -1
        ? this.player.localize('Auto')
        : `${quality}p`)
    }

    for (let i = 0; i < qualityList.levels_.length; ++i) {
      const { width, height } = qualityList.levels_[i]
      const pixels = width > height ? height : width

      qualityList.levels_[i].enabled = pixels === quality || quality === -1
    }

    this._menuButton.unpressButton()
  }

  getCurrentQuality(): number {
    return this._currentQuality || -1
  }
}
