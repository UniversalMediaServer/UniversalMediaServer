import videojs from 'video.js'
import ConcreteButton from './ConcreteButton'
import { HlsQualitySelectorClass } from './HlsQualitySelector'

const MenuItemComponent = videojs.getComponent('MenuItem') as any

/**
 * Extend vjs menu item class.
 */
export default class ConcreteMenuItem extends MenuItemComponent {
  item: any
  qualityButton: ConcreteButton
  plugin: HlsQualitySelectorClass

  /**
   * Menu item constructor.
   *
   * @param {Player} player - vjs player
   * @param {Object} item - Item object
   * @param {ConcreteButton} qualityButton - The containing button.
   * @param {HlsQualitySelectorPlugin} plugin - This plugin instance.
   */
  constructor(player: any, item: any, qualityButton: ConcreteButton, plugin: HlsQualitySelectorClass) {
    super(player, {
      label: item.label,
      selectable: true,
      selected: item.selected || false,
    })
    this.item = item
    this.qualityButton = qualityButton
    this.plugin = plugin
  }

  /**
   * Click event for menu item.
   */
  handleClick() {
    // Reset other menu items selected status.
    for (let i = 0; i < this.qualityButton.items.length; ++i) {
      this.qualityButton.items[i].selected(false)
    }

    // Set this menu item to selected, and set quality.
    this.plugin.setQuality(this.item.value)
    this.selected(true)
  }
}
