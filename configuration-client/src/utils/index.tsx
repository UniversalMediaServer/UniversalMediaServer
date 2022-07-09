export function getToolTipContent(content: string) {
  return (<span dangerouslySetInnerHTML={{__html: content}}></span>)
}

export const openGitHubNewIssue = () => {
  window.open('https://github.com/UniversalMediaServer/UniversalMediaServer/issues/new','_blank');
}
