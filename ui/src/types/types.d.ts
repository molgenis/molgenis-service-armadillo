export type StringObject = { [key: string]: string | Array<string> };

export type ObjectWithStringKeyAndStringArrayValue = {
  [key: string]: StringArray;
};

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

export type StringArray = string[];
