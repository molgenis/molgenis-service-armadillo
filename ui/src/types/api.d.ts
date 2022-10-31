import { StringArray } from "@/types/types";

export type Project = {
  name: string;
  users: string[];
};

export type User = {
  email: string;
  firstName: string;
  lastName: string;
  institution: string;
  admin: boolean;
  projects: string[];
};

export type UserStringKey =
  | "email"
  | "firstName"
  | "lastName"
  | "institution";

export type Principal = {
  authorities: [
    {
      authority: string;
    }
  ];
  details: Object | null;
  authenticated: boolean;
  name: string;
  credentials: Object | null;
  principal: Object | null;
};

export type Profile = {
  name: string;
  image: string;
  host: string;
  port: number;
  whitelist: StringArray;
  options: Object;
  container: {
    tags: StringArray;
    status: string;
  };
};
