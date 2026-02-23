import { Project, User, Workspace } from "./api";

export type StringObject = Record<string, string | Array<string>>;

export type ObjectWithStringKey = Record<
  string,
  string | StringArray | boolean | number | Object
>;

export type ListOfObjectsWithStringKey = ObjectWithStringKey[];

export type BootstrapType =
  | "primary"
  | "secondary"
  | "success"
  | "warning"
  | "info"
  | "danger"
  | "light"
  | "dark";

export type TypeString =
  | "string"
  | "number"
  | "boolean"
  | "array"
  | "date"
  | "object";
export type StringArray = string[];

interface Dictionary<T> {
  [key: string]: T;
}

// Maybe later expand with float/int/enum/character
export type TypeObject = Record<string, TypeString>;
export type ProjectsExplorerData = {
  editView: boolean;
  fileToDelete: string;
  folderToDeleteFrom: string;
  projectToEdit: string;
  projectToEditIndex: number;
  loading: boolean;
  successMessage: string;
  filePreview: Array<any>;
  fileMetaData: Object;
  fileInfo: {
    fileSize: string;
    dataSizeRows: number;
    dataSizeColumns: number;
    sourceLink: string;
    variables: Array<string>;
  };
  createNewFolder: boolean;
  loading_preview: boolean;
  loading_metadata: boolean;
  projectContent: Record<string, string[]>;
  selectedFile: string;
  selectedFolder: string;
  createLinkFromTarget: boolean;
  createLinkFromSrc: boolean;
  currentTab: string;
  columnNames: Array<string>;
};

export type ProjectsData = {
  availableUsers: StringArray;
  userToAdd: string;
  recordToDelete: string;
  projectToHighlightIndex: number;
  addRow: boolean;
  newProject: Project;
  projectsDataStructure: TypeObject;
  projectToEdit: Project;
  projectToEditIndex: number;
  loading: boolean;
  successMessage: string;
  searchString: string;
};

export type UsersData = {
  availableProjects: StringArray;
  projectToAdd: string;
  recordToDelete: string;
  updatedUserIndex: number;
  userDataStructure: TypeObject;
  editMode: {
    addProjectToRow: boolean;
    userToEdit: string;
    projects: StringArray;
    userToEditIndex: number;
  };
  addMode: {
    addProjectToRow: boolean;
    newUser: User;
  };
  addRow: boolean;
  loading: boolean;
  successMessage: string;
  searchString: string;
};

type StatusMappingType = {
  status: "ONLINE" | "OFFLINE" | "ERROR";
  text: "Stop" | "Start" | "Error";
  color: BootstrapType;
  icon: string;
};

export type ContainerType = "ds" | "flwr-node" | "flwr-app" | "default";

export type ContainersData = {
  addContainer: boolean;
  recordToDelete: string;
  loading: boolean;
  loadingContainer: string;
  successMessage: string;
  profileToEditIndex: number;
  profileToEdit: string;
  statusMapping: {
    NOT_FOUND: StatusMappingType;
    NOT_RUNNING: StatusMappingType;
    RUNNING: StatusMappingType;
    DOCKER_OFFLINE: StatusMappingType;
  };
};

export type ViewEditorData = {
  projectData: Record<string, StringArray>;
  vwTable: string;
  vwProject: string;
  vwFolder: string;
  srcTable: string;
  srcProject: string;
  srcFolder: string;
  formValidated: boolean;
};

export type Workspaces = Record<string, Workspace[]>;

export type FormattedWorkspace = {
  user?: string;
  name: string;
  size: string;
  lastModified: Date;
};

export type FormattedWorkspaces = Record<string, FormattedWorkspace[]>;

export type EndpointObject = {
  key: string;
  href: string;
  templated: boolean;
};
