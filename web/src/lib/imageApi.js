const API_URL = import.meta.env.VITE_IMAGE_API_URL;

function buildMockImageUrl(prompt, aspectRatio) {
  const seed = encodeURIComponent(`${prompt}-${aspectRatio}`.trim() || "sample-image");
  const dimensions = aspectRatio === "9:16" ? "720/1280" : aspectRatio === "1:1" ? "1024/1024" : "1280/720";
  return `https://picsum.photos/seed/${seed}/${dimensions}`;
}

function normalizeImageUrl(payload) {
  if (!payload) {
    return null;
  }

  if (typeof payload === "string") {
    return payload;
  }

  if (typeof payload.imageUrl === "string") {
    return payload.imageUrl;
  }

  if (typeof payload.url === "string") {
    return payload.url;
  }

  if (Array.isArray(payload.data) && payload.data.length > 0) {
    const first = payload.data[0];
    if (typeof first === "string") {
      return first;
    }
    if (first && typeof first.url === "string") {
      return first.url;
    }
    if (first && typeof first.imageUrl === "string") {
      return first.imageUrl;
    }
  }

  return null;
}

export async function generateImage(request) {
  if (!API_URL) {
    await new Promise((resolve) => window.setTimeout(resolve, 1200));

    return {
      imageUrl: buildMockImageUrl(request.prompt, request.aspectRatio),
      requestId: `mock-${Date.now()}`
    };
  }

  const response = await fetch(API_URL, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(request)
  });

  if (!response.ok) {
    throw new Error(`生成失败: ${response.status} ${response.statusText}`);
  }

  const payload = await response.json();
  const imageUrl = normalizeImageUrl(payload);

  if (!imageUrl) {
    throw new Error("接口已返回响应，但没有可用的图片 URL。");
  }

  return {
    imageUrl,
    requestId: payload.requestId || payload.id || `remote-${Date.now()}`
  };
}
