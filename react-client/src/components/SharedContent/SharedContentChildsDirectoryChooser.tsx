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
import { I18nInterface } from '../../services/i18n-service'
import { Folder } from '../../services/shared-service'
import DirectoryChooser from '../DirectoryChooser/DirectoryChooser'

export default function SharedContentText({
  i18n,
  childs,
  canModify,
  setSharedContentChild,
}: {
  i18n: I18nInterface
  childs?: Folder[]
  canModify: boolean
  setSharedContentChild: (file: string, index: number) => void
}) {
  return childs?.map((child: Folder, index) => (
    <DirectoryChooser
      i18n={i18n}
      disabled={!canModify}
      size="xs"
      path={child.file}
      callback={(directory: string) => setSharedContentChild(directory, index)}
    >
    </DirectoryChooser>
  ))
}
