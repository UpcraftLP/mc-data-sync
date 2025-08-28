ARG NODE_VERSION=20
FROM node:${NODE_VERSION}-alpine AS base

RUN ulimit -c unlimited

# prisma needs OpenSSL
RUN apk add --no-cache openssl

FROM base AS build

WORKDIR /app

COPY package*.json .

RUN npm ci

COPY . .

RUN npm run prisma-generate
RUN npm run build
RUN npm prune --omit=dev

FROM base AS deploy
ARG NODE_ENV=production
ENV NODE_ENV=${NODE_ENV}

WORKDIR /app

COPY docker/entrypoint.sh ./entrypoint.sh
RUN chmod +x ./entrypoint.sh

COPY --from=build /app/build ./build
COPY --from=build /app/package.json ./package.json
COPY --from=build /app/node_modules ./node_modules
COPY --from=build /app/prisma ./prisma

EXPOSE 3000

CMD ["./entrypoint.sh"]
