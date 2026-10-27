export default defineEventHandler((event) => {
  const config = useRuntimeConfig()
  const path = getRouterParam(event, 'path') ?? ''
  const query = getRequestURL(event).search ?? ''
  return proxyRequest(event, `${config.apiInternalBase}/api/${path}${query}`)
})
