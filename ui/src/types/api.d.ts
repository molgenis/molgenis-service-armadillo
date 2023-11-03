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

export type UserStringKey = "email" | "firstName" | "lastName" | "institution";

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
  principal: {
    authorities: Array<Object>;
    attributes: {
      applicationId: string;
      email: string;
      email_verified: boolean;
      family_name: string;
      given_name: string;
      roles: StringArray;
      sub: string;
    };
    name: string;
  } | null;
};

export type Profile = {
  name: string;
  image: string;
  host: string;
  port: number;
  packageWhitelist: StringArray;
  functionBlacklist: StringArray;
  datashieldSeed: string;
  options: {
    "datashield.seed": string;
  };
  container: {
    tags: StringArray;
    status: string;
  };
};

export type Auth = { user: string; pwd: string };
