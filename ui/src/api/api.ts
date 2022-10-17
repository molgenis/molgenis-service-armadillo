import { Principal, Project, User } from "@/types/api";
import { StringArray } from "@/types/types";
import { APISettings } from "./config";

export async function get(url: string) {
  const response = await fetch(url, {
    method: "GET",
    headers: APISettings.headers,
  });
  if (response.status != 200) {
    const error =
      (response.json && 'message' in response.json) || response.statusText;
    throw error;
  } else {
    return response.json();
  }
}

export async function put(url: string, body: Object) {
  // PUT request using fetch with async/await
  const requestOptions = {
    method: "PUT",
    headers: APISettings.headers,
    body: JSON.stringify(body),
  };
  const response = await fetch(url, requestOptions);
  if (response.status != 204) {
    const error =
      (response.json && 'message' in response.json) || response.statusText;
    throw error;
  } else {
    return response;
  }
}

export async function delete_(url: string, item: string) {
  const response = await fetch(`${url}/${item}`, { method: "DELETE" });
  if (response.status != 204) {
    const error =
    (response.json && 'message' in response.json) || response.statusText;
    throw error;
  } else {
    return response;
  }
}

export async function deleteUser(email: string) {
  return delete_("/admin/users", email);
}

export async function putUser(userJson: User) {
  return put("/admin/users", userJson);
}

export async function deleteProject(name: string) {
  return delete_("/admin/projects", name);
}

export async function putProject(projectJson: Project) {
  return put("/admin/projects", projectJson);
}

export async function getUsers(): Promise<User[]> {
  return get("/admin/users");
}

export async function getProjects(): Promise<Project[]>  {
  return get("/admin/projects");
}

export async function getPrincipal(): Promise<Principal> {
  return get("/my/principal");
}

export async function getProject(projectId: string): Promise<StringArray> {
  return get(`/storage/projects/${projectId}/objects`);
}
