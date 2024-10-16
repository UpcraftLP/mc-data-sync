#!/bin/sh

npm run prisma-deploy

exec node /app/build "$@"