FROM node:22-alpine AS build
WORKDIR /workspace
COPY apps/storefront ./apps/storefront
WORKDIR /workspace/apps/storefront
RUN npm install
RUN npm run build

FROM node:22-alpine
WORKDIR /app
ENV NODE_ENV=production
ENV HOST=0.0.0.0
ENV PORT=3000
COPY --from=build /workspace/apps/storefront/.output ./.output
EXPOSE 3000
CMD ["node", ".output/server/index.mjs"]