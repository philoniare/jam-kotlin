#!/bin/bash
set -e

# Build and push jam-forge Docker image to GitHub Container Registry
# Requires: GH_USER and GH_DOCKER_TOKEN environment variables

IMAGE_NAME="jam-forge"
REGISTRY="ghcr.io"

# Check required environment variables
if [ -z "$GH_USER" ]; then
    echo "Error: GH_USER environment variable is not set"
    exit 1
fi

if [ -z "$GH_DOCKER_TOKEN" ]; then
    echo "Error: GH_DOCKER_TOKEN environment variable is not set"
    exit 1
fi

FULL_IMAGE="${REGISTRY}/${GH_USER}/${IMAGE_NAME}:latest"

echo "=== Building ${IMAGE_NAME} for linux/amd64 ==="
echo "Target: ${FULL_IMAGE}"

# Navigate to project root (where Dockerfile is)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_ROOT"

echo ""
echo "=== Step 1: Building Docker image ==="
docker build --platform linux/amd64 -t "${IMAGE_NAME}:latest" .

echo ""
echo "=== Step 2: Logging into GHCR ==="
echo "$GH_DOCKER_TOKEN" | docker login "$REGISTRY" -u "$GH_USER" --password-stdin

echo ""
echo "=== Step 3: Tagging image ==="
docker tag "${IMAGE_NAME}:latest" "$FULL_IMAGE"

echo ""
echo "=== Step 4: Pushing to GHCR ==="
docker push "$FULL_IMAGE"

echo ""
echo "=== Done! ==="
echo "Image pushed to: ${FULL_IMAGE}"