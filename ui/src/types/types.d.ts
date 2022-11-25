import { User } from "./api";

export type StringObject = { [key: string]: string | Array<string> };

export type ObjectWithStringKey = {
  [key: string]: string | StringArray | boolean | number | Object;
};

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

// Maybe later expand with float/int/enum/character
export type TypeObject = {
  [key: string]: TypeString;
};

export type ProjectsExplorerData = {
  triggerFileUpload: boolean;
  projectToEdit: string;
  projectToEditIndex: number;
  loading: boolean;
  successMessage: string;
  filePreview: Array<any>;
  createNewFolder: boolean;
  loading_preview: boolean;
  newFolder: string;
  projectContent: Record<string, string[]>;
};

export type ProjectsData = {
  updatedProjectIndex: number;
  projectsDataStructure: TypeObject;
  projectToEdit: string;
  projectToEditIndex: number;
  loading: boolean;
  successMessage: string;
  searchString: string;
};

export type UsersData = {
  updatedUserIndex: number;
  userDataStructure: TypeObject;
  editMode: {
    addProjectToRow: boolean;
    project: string;
    userToEdit: string;
    userToEditIndex: number;
  };
  addMode: {
    addProjectToRow: boolean;
    newUser: User;
    project: string;
  };
  addRow: boolean;
  loading: boolean;
  successMessage: string;
  searchString: string;
};

export type ProfilesData = {
  profilesDataStructure: TypeObject;
  loading: boolean;
  successMessage: string;
  profileToEditIndex: number;
  profileToEdit: string;
};
