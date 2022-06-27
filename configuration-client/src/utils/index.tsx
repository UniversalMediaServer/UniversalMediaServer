export function getToolTipContent(content: string) {
  return (<span dangerouslySetInnerHTML={{__html: content}}></span>)
}
