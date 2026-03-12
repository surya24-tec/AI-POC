# Stage 1: Build the Angular app
FROM node:20-alpine AS build

WORKDIR /app

# Copy dependency files first for better layer caching
COPY package.json package-lock.json ./
RUN npm ci

# Copy the rest of the source code
COPY . .

# Build the production bundle
RUN npm run build

# Stage 2: Serve with Nginx
FROM nginx:alpine

# Remove default Nginx config
RUN rm /etc/nginx/conf.d/default.conf

# Copy custom Nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy the Angular build output to Nginx html directory
COPY --from=build /app/dist/ai-interviewer-frontend/browser /usr/share/nginx/html

# Render uses port 10000 by default
EXPOSE 10000

CMD ["nginx", "-g", "daemon off;"]
