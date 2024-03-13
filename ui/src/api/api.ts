import { ApiError } from "@/helpers/errors";
import { sanitizeObject } from "@/helpers/utils";
import {
  Principal,
  Profile,
  Project,
  User,
  Auth,
  RemoteFileInfo,
  RemoteFileDetail,
} from "@/types/api";
import { ObjectWithStringKey, StringArray } from "@/types/types";
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
  if (response.status === 204 || response.status === 500) {
    return outcome;
  } else if (response.status === 403 || response.status === 401) {
    return outcome;
  } else {
    return response.json();
  }
}

export async function put(url: string, body: ObjectWithStringKey) {
  const requestOptions = {
    method: "PUT",
    headers: APISettings.headers,
    body: JSON.stringify(sanitizeObject(body)),
  };
  const response = await fetch(url, requestOptions);
  return handleResponse(response);
}

export async function post(url: string) {
  const requestOptions = {
    method: "POST",
    headers: APISettings.headers,
  };
  const response = await fetch(url, requestOptions);
  return handleResponse(response);
}

export async function postFormData(url: string, formData: FormData) {
  const requestOptions = {
    method: "POST",
    body: formData,
  };
  const response = await fetch(url, requestOptions);
  return handleResponse(response);
}

export async function delete_(url: string, item: string) {
  const response = await fetch(`${url}/${item.trim()}`, { method: "DELETE" });
  return handleResponse(response);
}

export async function handleResponse(response: Response) {
  let error = new ApiError("", response.status);
  if (!response.ok) {
    if (response.status === 500) {
      error.message = response.statusText;
    } else if (response.status === 403 || response.status === 401) {
      error.message =
        "You don't have correct permissions. Please contact the administrator";
    } else {
      const json = await response.json();

      if (json.message) {
        error.message = json.message;
      } else {
        error.message = response.statusText;
      }
    }
    throw error;
  } else {
    return response;
  }
}

export async function getActuator() {
  let result = await get("/actuator");
  return result;
}

export async function getActuatorItem(item: string) {
  let result = await get(`/actuator/${item}`);
  return result;
}

export async function getVersion() {
  let result = await get("/actuator/info");
  return result.build.version;
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

export async function getFiles(): Promise<RemoteFileInfo[]> {
  return get("/insight/files");
}

export async function getFileDetail(
  file_id: string,
  page_num: number,
  page_size: number,
  direction: string
): Promise<RemoteFileDetail> {
  return get(
    `/insight/files/${file_id}?page_num=${page_num}&page_size=${page_size}&direction=${direction}`
  );
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

export async function getFileDetails(project: string, object: string) {
  return get(`/storage/projects/${project}/objects/${object}/info`);
}
