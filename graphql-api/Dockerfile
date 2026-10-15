FROM node:22-bookworm-slim
WORKDIR /app/graphql-api
COPY graphql-api/package*.json ./
RUN npm ci --omit=dev
COPY graphql-api/ ./
COPY database/ /app/database/
CMD ["node","--max-old-space-size=160","server.cjs"]
