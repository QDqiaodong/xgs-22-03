FROM ${DOCKER_REGISTRY}node:20-alpine AS builder

WORKDIR /app

RUN npm config set registry https://mirrors.ustc.edu.cn/npm/

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM ${DOCKER_REGISTRY}nginx:alpine

COPY --from=builder /app/dist /usr/share/nginx/html

COPY nginx.conf /etc/nginx/nginx.conf

EXPOSE 8117

CMD ["nginx", "-g", "daemon off;"]