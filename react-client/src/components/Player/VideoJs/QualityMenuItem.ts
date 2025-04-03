import videojs from 'video.js'
import MenuItem from 'video.js/dist/types/menu/menu-item'
import QualityMenuButton from './QualityMenuButton'
import HlsQualitySelector, { QualityItem } from './HlsQualitySelector'
import { VideoJsPlayer } from './VideoJs'

const VideoJsMenuItemComponent = videojs.getComponent('MenuItem') as typeof MenuItem

export default class QualityMenuItem extends VideoJsMenuItemComponent {
  item: QualityItem
  private qualityButton: QualityMenuButton
  private selector: HlsQualitySelector

  constructor(player: VideoJsPlayer, item: QualityItem, qualityButton: QualityMenuButton, selector: HlsQualitySelector) {
    super(player, {
      label: item.label,
      selectable: true,
      selected: item.selected || false,
    })
    this.item = item
    this.qualityButton = qualityButton
    this.selector = selector
  }

  handleClick(): void {
    for (let i = 0; i < this.qualityButton.items.length; ++i) {
      this.qualityButton.items[i].selected(false)
    }

    this.selector.setQuality(this.item.value)
    this.selected(true)
  }
}
