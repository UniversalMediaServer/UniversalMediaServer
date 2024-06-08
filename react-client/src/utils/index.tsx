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
export function allowHtml(content: string) {
  return (<span dangerouslySetInnerHTML={{ __html: content }}></span>)
}

export const openGitHubNewIssue = () => {
  window.open('https://github.com/UniversalMediaServer/UniversalMediaServer/issues/new', '_blank');
}

export const defaultTooltipSettings = {
  width: 350,
  color: 'blue',
  multiline: true,
  withArrow: true,
}

export const aboutApiUrl = '/v1/api/about/';
export const actionsApiUrl = '/v1/api/actions/';
export const accountApiUrl = '/v1/api/account/';
export const authApiUrl = '/v1/api/auth/';
export const i18nApiUrl = '/v1/api/i18n/';
export const logsApiUrl = '/v1/api/logs/';
export const playerApiUrl = '/v1/api/player/';
export const renderersApiUrl = '/v1/api/renderers/';
export const settingsApiUrl = '/v1/api/settings/';
export const sharedApiUrl = '/v1/api/shared/';
export const sseApiUrl = '/v1/api/sse/';
