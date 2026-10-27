export default defineEventHandler((event) => {
  const config = useRuntimeConfig()
  const path = getRouterParam(event, 'path') ?? ''
  const target = `${config.apiInternalBase}/assets/${path}`
  return proxyRequest(event, target)
})
