# Builds the backend + the Python ML scripts into one image, for hosting
# (e.g. Render). The backend starts ml/*.py as child processes, so the
# image needs Node AND Python with the ML libraries.
FROM node:22-slim

RUN apt-get update \
    && apt-get install -y --no-install-recommends python3 python3-venv \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Python libraries first (changes rarely, so Docker can cache this layer).
# The venv goes at ml/.venv because backend/src/utils/mlRunner.js looks for
# exactly that path before falling back to a bare "python3".
COPY ml/requirements.txt ml/requirements.txt
RUN python3 -m venv ml/.venv \
    && ml/.venv/bin/pip install --no-cache-dir -r ml/requirements.txt

# Node dependencies (production only).
COPY backend/package.json backend/package-lock.json backend/
RUN cd backend && npm ci --omit=dev

COPY backend backend
COPY ml ml
# The forecast script trains on this CSV (path: ../data from ml/).
COPY data/SMSMS_monthly_sales_history.csv data/

WORKDIR /app/backend
ENV NODE_ENV=production
# The host (Render) provides PORT; server.js reads it.
CMD ["node", "src/server.js"]
