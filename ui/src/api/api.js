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

export async function getUsers() {
  return get("/metadata/users");
}

export async function getProjects() {
  return get("/metadata/projects");
}

export async function getPrincipal() {
  const principal = get("/my/principal");
  console.log(principal);
  return principal;
}

export default { get };
