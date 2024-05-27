import videojs from 'video.js';
import Player from 'video.js/dist/types/player';

const MenuComponent = videojs.getComponent('Menu') as any;
const MenuButtonComponent = videojs.getComponent('MenuButton') as any;

/**
 * Extend vjs button class for quality button.
 */
export default class ConcreteButton extends MenuButtonComponent {
  items: any;
  options_: any;

  /**
   * Button constructor.
   *
   * @param {Player} player - videojs player instance
   */
  constructor(player: Player) {
    super(player, {
      title: player.localize('Quality'),
      name: 'QualityButton'
    });
  }
  /**
   * Creates button items.
   *
   * @return {Array} - Button items
   */
  createItems(): Array<any> {
    return [];
  }

  /**
   * Create the menu and add all items to it.
   *
   * @return {Menu}
   *         The constructed menu
   */
  createMenu(): typeof MenuComponent {
    const menu = new MenuComponent(this.player_, { menuButton: this });
    this.items = this.createItems();

    if (this.items) {
      // Add menu items to the menu
      for (let i = 0; i < this.items.length; i++) {
        menu.addItem(this.items[i]);
      }
    }
    return menu;
  }

}