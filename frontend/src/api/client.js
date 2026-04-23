const API_BASE_URL = `${import.meta.env.VITE_API_BASE_URL || "http://localhost:8082"}/api`;

export async function apiRequest(path, options = {}, token) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(options.headers || {})
    },
    ...options
  });

  const text = await response.text();
  const data = text ? JSON.parse(text) : null;

  if (!response.ok) {
    throw new Error(data?.message || "Request failed");
  }

  return data;
}

export { API_BASE_URL };
