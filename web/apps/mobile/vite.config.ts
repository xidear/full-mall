import { defineConfig } from 'vite'
import uniPlugin from '@dcloudio/vite-plugin-uni'

const uni = (uniPlugin as unknown as { default?: typeof uniPlugin }).default ?? uniPlugin

export default defineConfig({
  plugins: [uni()]
})