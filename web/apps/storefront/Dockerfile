FROM node:22-alpine AS build
WORKDIR /workspace
# 依赖锁定：COPY 根 lockfile + npm ci，避免每次构建解析出不同版本（含 heic2any 这类范围依赖）。
# 旧写法只 COPY apps/storefront 再 npm install，版本不可复现，也命中不了层缓存。
COPY package.json package-lock.json ./
COPY packages ./packages
COPY apps/storefront ./apps/storefront
RUN npm ci --workspace apps/storefront
WORKDIR /workspace/apps/storefront
RUN npm run build

FROM node:22-alpine
WORKDIR /app
ENV NODE_ENV=production
ENV HOST=0.0.0.0
ENV PORT=3000
# 前台是 Nuxt SSR（不是 nginx 静态托管），运行时必须有 Node——已在本镜像内，客户无需安装。
COPY --from=build /workspace/apps/storefront/.output ./.output
EXPOSE 3000
CMD ["node", ".output/server/index.mjs"]
