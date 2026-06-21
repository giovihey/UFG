# Makefile for the Project Management report (Quarkdown).
# Each target delegates to a bash script under scripts/.

SHELL := /usr/bin/env bash
SCRIPTS := scripts

.DEFAULT_GOAL := help

.PHONY: help build preview pdf clean

help: ## Show this help
	@echo "Available targets:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) \
		| awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-10s\033[0m %s\n", $$1, $$2}'

build: ## Compile in strict mode to verify (artifacts in /tmp)
	@$(SCRIPTS)/build.sh

preview: ## Launch live preview server (watch + reload)
	@$(SCRIPTS)/preview.sh

pdf: ## Export the report to PDF (slower)
	@$(SCRIPTS)/pdf.sh

clean: ## Remove generated build artifacts
	@$(SCRIPTS)/clean.sh
