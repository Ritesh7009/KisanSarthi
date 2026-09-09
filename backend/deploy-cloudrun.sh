#!/usr/bin/env bash
# ==============================================================================
# KisanSarthi MP - Google Cloud Run Deployment Script
# Deploys the Spring Boot backend to Google Cloud Run
# ==============================================================================

set -euo pipefail

# Configuration Defaults
SERVICE_NAME="${SERVICE_NAME:-kisansarthi-backend}"
REGION="${REGION:-asia-south1}" # Default to Mumbai (closest to Madhya Pradesh)
PROJECT_ID="${GCP_PROJECT_ID:-$(gcloud config get-value project 2>/dev/null || echo '')}"

if [ -z "$PROJECT_ID" ]; then
  echo "Error: Google Cloud Project ID is not set. Run 'gcloud config set project <PROJECT_ID>' or set GCP_PROJECT_ID."
  exit 1
fi

IMAGE_TAG="gcr.io/${PROJECT_ID}/${SERVICE_NAME}:latest"

echo "=================================================="
echo " Deploying ${SERVICE_NAME} to Google Cloud Run"
echo " Project: ${PROJECT_ID}"
echo " Region:  ${REGION}"
echo " Image:   ${IMAGE_TAG}"
echo "=================================================="

# 1. Build and push container using Google Cloud Build (no local Docker required)
echo "Step 1: Submitting build to Google Cloud Build..."
gcloud builds submit backend \
  --tag "${IMAGE_TAG}" \
  --project "${PROJECT_ID}"

# 2. Deploy to Cloud Run
echo "Step 2: Deploying container image to Cloud Run..."
gcloud run deploy "${SERVICE_NAME}" \
  --image "${IMAGE_TAG}" \
  --platform managed \
  --region "${REGION}" \
  --allow-unauthenticated \
  --memory 1024Mi \
  --cpu 1 \
  --timeout 300s \
  --min-instances 0 \
  --max-instances 10 \
  --set-env-vars "SPRING_PROFILES_ACTIVE=prod,kisansarthi.sms.provider=twilio" \
  --project "${PROJECT_ID}"

echo ""
echo "Deployment completed successfully!"
echo "Service URL:"
gcloud run services describe "${SERVICE_NAME}" --platform managed --region "${REGION}" --format 'value(status.url)'
