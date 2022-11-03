export type StringObject = { [key: string]: string | Array<string> };

export type ObjectWithStringKeyAndStringArrayValue = {
  [key: string]: Array<string>;
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

// Maybe later expand with float/int/enum/character
export type TypeObject = {[key: string]: "string" | "number" | "boolean" | "array" | "date" | "object"}