# syntax=docker/dockerfile:1

# ----------------------------------------------------------------------
# Build stage — compile the Angular app with the production config
# (defaultConfiguration in angular.json, which bakes environment.ts:
#  apiBaseUrl = http://localhost:8082/ — the browser talks to the
#  already-dockerized backend directly, so no proxy is needed here).
# ----------------------------------------------------------------------
FROM node:24-alpine AS build
WORKDIR /app

# Install against the lockfile first so this layer caches across source edits
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# ----------------------------------------------------------------------
# Runtime stage — static bundle served by a rootless nginx
# ----------------------------------------------------------------------
FROM nginxinc/nginx-unprivileged:1.30-alpine AS runtime

# SPA-aware site config (client-side routing fallback + asset caching)
COPY nginx.conf /etc/nginx/conf.d/default.conf

# @angular/build:application emits the browser files under dist/<project>/browser
COPY --from=build /app/dist/rf-inventario-panel/browser /usr/share/nginx/html

EXPOSE 8080

# 127.0.0.1 and not "localhost": in Alpine localhost resolves to ::1 first,
# and nginx only listens on IPv4 here, so the check would always fail.
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget -q -O /dev/null http://127.0.0.1:8080/ || exit 1
