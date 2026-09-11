import type { NextConfig } from "next";

// Same-site API proxy: browser calls /api/* (first-party cookies), Next.js
// forwards to the backend preserving the /api prefix its controllers require.
// Local dev defaults to localhost:8080; production sets BACKEND_INTERNAL_URL
// (e.g. https://dance7-api-production.up.railway.app). No trailing slash.
const backendOrigin = (process.env.BACKEND_INTERNAL_URL ?? "http://localhost:8080").replace(/\/$/, "");

const nextConfig: NextConfig = {
  reactStrictMode: true,
  async rewrites() {
    return [{ source: "/api/:path*", destination: `${backendOrigin}/api/:path*` }];
  },
};

export default nextConfig;
