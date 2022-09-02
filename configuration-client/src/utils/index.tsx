export function allowHtml(content: string) {
  return (<span dangerouslySetInnerHTML={{__html: content}}></span>)
}

export const openGitHubNewIssue = () => {
  window.open('https://github.com/UniversalMediaServer/UniversalMediaServer/issues/new','_blank');
}

export const defaultTooltipSettings = {
  width: 350,
  color: 'blue',
  multiline: true,
  withArrow: true,
}
