export function allowHtml(content: string) {
  return (<span dangerouslySetInnerHTML={{__html: content}}></span>)
}

export const openGitHubNewIssue = () => {
  window.open('https://github.com/UniversalMediaServer/UniversalMediaServer/issues/new','_blank');
}

export const aboutApiUrl = '/v1/api/about/';
export const actionsApiUrl = '/v1/api/actions/';
export const accountApiUrl = '/v1/api/account/';
export const authApiUrl = '/v1/api/auth/';
export const i18nApiUrl = '/v1/api/i18n/';
export const settingsApiUrl = '/v1/api/settings/';
export const sseApiUrl = '/v1/api/sse/';