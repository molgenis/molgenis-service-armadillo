import { APISettings } from "./config.js";

export async function get(url) {
  const response = await fetch(url, {
    method: "GET",
    headers: APISettings.headers,
  });
  if (response.status != 200) {
    throw response.status;
  } else {
    return response.json();
  }
}

export async function put(url, body) {
  // PUT request using fetch with async/await
  const requestOptions = {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: body
  };
  const response = await fetch(url, requestOptions);
  if (response.status != 200) {
    throw response.status;
  } else {
    return response.json();
    //updatedAt
  }
}

export async function putUser(userJson) {
  return put("/metadata/users", userJson);
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

export default { get };
