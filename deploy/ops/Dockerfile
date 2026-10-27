FROM node:22-alpine
RUN apk add --no-cache bash openssl curl ca-certificates tzdata git
ARG ACME_SH_VERSION=3.1.6
RUN git clone --depth 1 --branch "${ACME_SH_VERSION}" https://github.com/acmesh-official/acme.sh.git /opt/acme.sh \
    && rm -rf /opt/acme.sh/.git
WORKDIR /app
COPY server.mjs /app/server.mjs
EXPOSE 41287
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD node -e "fetch('http://127.0.0.1:'+(process.env.PORT||41287)+'/health').then(r=>process.exit(r.ok?0:1)).catch(()=>process.exit(1))"
CMD ["node", "/app/server.mjs"]
