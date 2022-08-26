import { APISettings } from "./config.js";

export async function get(url) {
  const response = await fetch(url, {
    method: "GET",
    headers: APISettings.headers,
  });
  if (response.status != 200) {
    const error =
      (response.json && response.json.message) || response.statusText;
    throw error;
  } else {
    return response.json();
  }
}

export async function put(url, body) {
  // PUT request using fetch with async/await
  const requestOptions = {
    method: "PUT",
    headers: APISettings.headers,
    body: JSON.stringify(body),
  };
  const response = await fetch(url, requestOptions);
  if (response.status != 200) {
    const error =
      (response.json && response.json.message) || response.statusText;
    throw error;
  } else {
    return response;
  }
}

export async function delete_(url, item) {
  const response = await fetch(`${url}/${item}`, { method: "DELETE" });
  if (response.status != 200) {
    const error =
    (response.json && response.json.message) || response.statusText;
    throw error;
  } else {
    return response;
  }
}

export async function deleteUser(email) {
  return delete_("/metadata/users", email);
}

export async function putUser(userJson) {
  return put("/metadata/users", userJson);
}

export async function deleteProject(name) {
  return delete_("/metadata/projects", name);
}

export async function putProject(projectJson) {
  return put("/metadata/projects", projectJson);
}

export async function getUsers() {
  return get("/metadata/users");
}

export async function getProjects() {
  return get("/metadata/projects");
}

export async function getPrincipal() {
  const principal = get("/my/principal");
  return principal;
}
