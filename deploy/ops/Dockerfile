FROM node:22-alpine
RUN apk add --no-cache bash openssl curl ca-certificates tzdata git
ARG ACME_SH_VERSION=3.1.6
# acme.sh 的获取地址可覆盖：内网/无外网构建时指向内部 git 镜像，
#   docker build --build-arg ACME_SH_SOURCE=https://git.internal/acme.sh.git ...
# 或者（更推荐）在有网机器上预构建本镜像并推私有仓库，部署机只拉取。
# 不能把"构建机能访问 GitHub"当成交付前提——这正是依赖矩阵里登记的那条约束。
ARG ACME_SH_SOURCE=https://github.com/acmesh-official/acme.sh.git
RUN git clone --depth 1 --branch "${ACME_SH_VERSION}" "${ACME_SH_SOURCE}" /opt/acme.sh \
    || { echo "[ops] acme.sh 拉取失败：${ACME_SH_SOURCE}（无外网时请用 --build-arg ACME_SH_SOURCE=<内部镜像> 或改用预构建镜像）" >&2; exit 1; }
RUN rm -rf /opt/acme.sh/.git
WORKDIR /app
COPY server.mjs /app/server.mjs
EXPOSE 41287
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD node -e "fetch('http://127.0.0.1:'+(process.env.PORT||41287)+'/health').then(r=>process.exit(r.ok?0:1)).catch(()=>process.exit(1))"
CMD ["node", "/app/server.mjs"]
