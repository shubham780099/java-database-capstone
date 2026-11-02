// config.js

/**
 * Global front-end settings.
 *
 * API_BASE_URL is the base URL for every API request made from the browser.
 * The pages are served by the same Spring Boot application as the REST API, so the page's own
 * origin is the right value everywhere: http://localhost:8080 locally, the lab's application URL
 * in the Cloud IDE, or wherever the Docker container is published. Nothing to edit per environment.
 *
 * Example usage:
 *   fetch(`${API_BASE_URL}/doctor`)
 */
export const API_BASE_URL = window.location.origin;
