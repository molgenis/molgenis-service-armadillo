import { ConnectionError } from "@/helpers/errors";
import { Principal, Profile, Project, User, Auth } from "@/types/api";
import { StringArray } from "@/types/types";
import { APISettings } from "./config";

export async function get(url: string, auth: Auth | undefined = undefined) {
  let headers = APISettings.headers;
  if (auth) {
    headers.set("Authorization", `Basic ${btoa(auth.user + ":" + auth.pwd)}`);
  }
  const response = await fetch(url, {
    method: "GET",
    headers: headers,
  });
  const outcome = handleResponse(response);
  if (response.status === 204) {
    return outcome;
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
  return handleResponse(response);
}

export async function post(url: string) {
  // PUT request using fetch with async/await
  const requestOptions = {
    method: "POST",
    headers: APISettings.headers,
  };
  const response = await fetch(url, requestOptions);
  return handleResponse(response);
}

export async function postFormData(url: string, formData: FormData) {
  // PUT request using fetch with async/await
  const requestOptions = {
    method: "POST",
    body: formData,
  };
  const response = await fetch(url, requestOptions);
  return handleResponse(response);
}

export async function delete_(url: string, item: string) {
  const response = await fetch(`${url}/${item}`, { method: "DELETE" });
  return handleResponse(response);
}

export async function handleResponse(response: Response) {
  if (!response.ok) {
    const json = await response.json();
    let error = new ConnectionError("", response.status);
    if (json.message) {
      error.message = json.message;
    } else {
      error.message = response.statusText;
    }
    throw error;
  } else {
    return response;
  }
}

export async function deleteUser(email: string) {
  return delete_("/access/users", email);
}

export async function putUser(userJson: User) {
  return put("/access/users", userJson);
}

export async function deleteProject(name: string) {
  return delete_("/access/projects", name);
}

export async function putProject(projectJson: Project) {
  return put("/access/projects", projectJson);
}

export async function getUsers(): Promise<User[]> {
  return get("/access/users");
}

export async function getProjects(): Promise<Project[]> {
  return get("/access/projects");
}

export async function getPrincipal(): Promise<Principal> {
  return get("/my/principal");
}

export async function getProject(projectId: string): Promise<StringArray> {
  const project = get(`/storage/projects/${projectId}/objects`);
  return project;
}

export async function deleteObject(project: string, name: string) {
  return delete_("/storage/projects/" + project + "/objects", name);
}

export async function getProfiles(): Promise<Profile[]> {
  return get("/ds-profiles");
}

export async function deleteProfile(name: string) {
  return delete_("/ds-profiles", name);
}

export async function putProfile(profileJson: Profile) {
  return put("/ds-profiles", profileJson);
}

export async function startProfile(name: string) {
  return post(`/ds-profiles/${name}/start`);
}

export async function stopProfile(name: string) {
  return post(`/ds-profiles/${name}/stop`);
}

export async function uploadIntoProject(
  fileToUpload: File,
  object: string,
  project: string
) {
  let formData = new FormData();
  formData.append("file", fileToUpload);
  formData.append("object", `${object}/${fileToUpload.name}`);
  return postFormData(`/storage/projects/${project}/objects`, formData);
}

export async function previewObject(projectId: string, object: string) {
  return get(`/storage/projects/${projectId}/objects/${object}/preview`);
}

export async function logout() {
  const auth = { user: "logout", pwd: new Date().getTime().toString() };
  return get("/logout").then(() => {
    authenticate(auth);
  });
}

export async function authenticate(auth: Auth) {
  const response = await fetch("/basic-login", {
    method: "GET",
    headers: {
      Authorization: `Basic ${btoa(auth.user + ":" + auth.pwd)}`,
    },
  });
  return handleResponse(response);
}
