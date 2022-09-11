export function allowHtml(content: string) {
  return (<span dangerouslySetInnerHTML={{__html: content}}></span>)
}

export const openGitHubNewIssue = () => {
  window.open('https://github.com/UniversalMediaServer/UniversalMediaServer/issues/new','_blank');
}

export const authApiUrl = '/v1/api/auth/';
export const i18nApiUrl = '/v1/api/i18n/';
export const sseApiUrl = '/v1/api/sse/';