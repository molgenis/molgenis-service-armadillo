export type StringObject = { [key: string]: string | Array<string> };

export type ObjectWithStringKey =  { [key: string]: string | StringArray | boolean };

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

export type StringArray = string[];