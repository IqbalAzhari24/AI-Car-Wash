# Build stage
FROM node:20-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm ci --production=false

COPY . .
RUN npm run build

# Serve stage
FROM nginx:alpine
RUN addgroup -g 1001 -S appgroup && adduser -S appuser -u 1001 -G appgroup

COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

RUN chown -R appuser:appgroup /usr/share/nginx/html \
  && chown -R appuser:appgroup /var/cache/nginx \
  && chown -R appuser:appgroup /var/log/nginx \
  && touch /var/run/nginx.pid \
  && chown -R appuser:appgroup /var/run/nginx.pid

USER appuser
EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -qO- http://localhost:80/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
