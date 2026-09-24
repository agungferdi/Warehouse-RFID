export interface DeviceAuthResult {
  ok: boolean;
  status: number;
  body?: { success: false; error: { code: string; message: string } };
}

export function validateDeviceApiKey(headers: Headers): DeviceAuthResult {
  const expected = process.env.DEVICE_API_KEY;
  if (!expected) {
    return {
      ok: false,
      status: 500,
      body: { success: false, error: { code: "SERVER_MISCONFIGURED", message: "DEVICE_API_KEY is not configured." } },
    };
  }

  const provided = headers.get("x-api-key");
  if (!provided || provided !== expected) {
    return {
      ok: false,
      status: 401,
      body: { success: false, error: { code: "UNAUTHORIZED", message: "Missing or invalid X-API-Key header." } },
    };
  }

  return { ok: true, status: 200 };
}
